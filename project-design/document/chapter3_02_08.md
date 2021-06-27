[toc]



# 电商 - 订单超时自动取消

## 1.背景

系统中用户下单，对于系统下单一般是分布式事务的操作，想要实现订单超时自动取消，我们可以基于MQ的延迟队列和死信队列实现。整体的实现思路分三种情况要考虑，第一种是订单的创建和投递到MQ，第二种是正常订单消息的消费，另外则是超时后消息的消费。



## 2.实现思路

对于订单的创建，只要生产者将消息成功投递到MQ，则认为订单创建成功。MQ返回ack表明消息投递成功，此时向延迟队列发送一条消息，而延迟队列挂载死信队列。这样做目的是：如果延迟队列中的消息达到阈值还没消费，则会进入死信队列，此时死信队列的监听器则会获取到过期的订单信息，可以做取消操作，反之，则走正常订单消费的流程。

整体实现思路大体如下：

![å¨è¿éæå¥å¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210627202426.png)



## 3.具体代码

本文基于RabbitMQ实现，借助于RabbitMQ的延迟队列TTL和死信队列。

配置文件：

```
server.port=8080

# 设计rabbitmq连接
spring.rabbitmq.host=127.0.0.1
spring.rabbitmq.port=5672
spring.rabbitmq.username=root
spring.rabbitmq.password=123456
# 设置虚拟主机
spring.rabbitmq.virtual-host=keduw-order

# 设置发布者确认机制
# correlated发布消息成功到交换器后会触发回调方法，默认是none
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true

# 消息为手动确认
spring.rabbitmq.listener.direct.acknowledge-mode=manual
```

增加RabbitMQ的配置类，创建对应的队列、转换器、监听器以及队列信息绑定，备注很详细，这里就不太赘述。

```
**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 使用DirectMessageListenerContainer，您需要确保ConnectionFactory配置了一个任务执行器，
     * 该执行器在使用该ConnectionFactory的所有侦听器容器中具有足够的线程来支持所需的并发性。
     * 默认连接池大小仅为5。
     *
     * 并发性基于配置的队列和consumersPerQueue。每个队列的每个使用者使用一个单独的通道，
     * 并发性由rabbit客户端库控制;默认情况下，它使用5个线程池;
     * 可以配置taskExecutor来提供所需的最大并发性。
     *
     * @param connectionFactory
     * @return
     */
    @Bean(name = "rabbitMessageListenerContainer")
    public DirectMessageListenerContainer listenerContainer(ConnectionFactory connectionFactory){
        // 写的时候，默认使用DEFAULT_NUM_THREADS = Runtime.getRuntime().availableProcessors() * 2个线程
        DirectMessageListenerContainer container = new DirectMessageListenerContainer(connectionFactory);
        // 设置确认消息的模式
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setPrefetchCount(5);
        container.setConsumersPerQueue(5);
        container.setMessagesPerAck(1);

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(10);
        taskExecutor.setMaxPoolSize(20);
        //设置该属性，灵活设置并发 ,多线程运行。
        container.setTaskExecutor(taskExecutor);

        return container;
    }

    /**
     * 设置消息转换器，用于将对象转换成JSON数据
     * 可以通过converterAndSend将对象发送消息队列
     * 监听器也可以通过该工具将接受对象反序列化成java对象
     *
     * @return Jackson转换器
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 订单消息队列
     * @return
     */
    @Bean
    public Queue orderQueue(){
        return QueueBuilder.durable("q.order").build();
    }

    /**
     * 延迟消息队列
     * @return
     */
    @Bean
    public Queue ttlQueue(){
        Map<String,Object> args = new HashMap<>();
        // 该队列的消息10s到期
        args.put("x-message-ttl", 10000);
        // 设置死信队列交换器,（当队列消息TTL到期后依然没有消费，则加入死信队列）
        args.put("x-dead-letter-exchange","x.dlx");
        // 设置私信队列路由键,设置该队列所关联的死信交换器的routingKey，如果没有特殊指定，使用原队列的routingKey
        args.put("x-dead-letter-routing-key","k.dlx");
        Queue queue = new Queue("q.ttl",true,false,false, args);
        return queue;
    }

    /**
     * 死信队列，用于取消用户订单
     * 当10s还没有付款的订单则进入死信队列，消费死信队列，取消用户订单
     *
     * @return
     */
    @Bean
    public Queue dlxQueue(){
        Map<String,Object> args = new HashMap<>();
        Queue dlq = new Queue("q.dlx",true,false,false, args);

        return dlq;
    }

    /**
     * 订单交换器
     * @return
     */
    @Bean
    public Exchange orderExchange(){
        Map<String, Object> args = new HashMap<>();
        DirectExchange exchange = new DirectExchange("x.order", true, false, args);

        return exchange;
    }

    /**
     * 延迟队列交换器
     * @return
     */
    @Bean
    public Exchange ttlExchange(){
        Map<String, Object> args = new HashMap<>();
        return new DirectExchange("x.ttl", true, false, args);
    }

    /**
     * 死信队列交换器
     * @return
     */
    @Bean
    public Exchange dlxExchange(){
        Map<String, Object> args = new HashMap<>();
        DirectExchange exchange = new DirectExchange("x.dlx", true, false, args);
        return exchange;
    }

    /**
     * 用于发送下单，做分布式事务的MQ
     * @return
     */
    @Bean
    public Binding orderBinding(){
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("k.order")
                .noargs();
    }

    /**
     * 用于等待用户支付的延迟队列绑定
     * @return
     */
    @Bean
    public Binding ttlBinding(){
        return BindingBuilder.bind(ttlQueue())
                .to(ttlExchange())
                .with("k.ttl")
                .noargs();
    }

    /**
     * 用于支付超时取消用户订单的死信队列绑定
     * @return
     */
    @Bean
    public Binding dlxBinding(){
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with("k.dlx")
                .noargs();
    }

}
```

创建订单监听器，用于监听订单正常的支付提交和超时取消。

```
/**
 * 订单正常支付流程监听
 */
@Component
public class OrderNormalListener {

    @RabbitListener(queues = "q.order",ackMode = "MANUAL")
    public void onMessage(Order order , Channel channel , Message message) throws IOException {
        System.out.println("写入数据库");
        System.out.println(order);

        for (OrderDetail detail : order.getDetails()){
            System.out.println(detail);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag() , false);
    }

}
```

创建订单超时自动取消监听器，监听的是死信队列。

```
/**
 * 订单超时自动取消监听
 */
@Component
public class OrderCancelListener implements ChannelAwareMessageListener {

    @Override
    @RabbitListener(queues = "q.dlx" , ackMode = "MANUAL")
    public void onMessage(Message message, Channel channel) throws Exception {
        String orderId = new String(message.getBody());
        System.out.println("取消订单：" + orderId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
```

对于订单提交，正常提交后同时投递多一份到延迟队列里面去，用作延迟取消。

```
// 构建订单信息
Order order = new Order();
order.setUserId(IdUtils.generateUserId());
order.setOrderId(IdUtils.generateOrderId());
// 设置状态为待支付
order.setStatus(OrderStatus.TO_BE_PAYED.toString());
order.setDetails(details);

// 投递消息
CorrelationData correlationData = new CorrelationData();
rabbitTemplate.convertAndSend("x.order","k.order", order, correlationData);
// 同步等待，可以设置为异步回调
CorrelationData.Confirm confirm = correlationData.getFuture().get();
// 判断发送的消息是否得到broker的确认
boolean confirmAck = confirm.isAck();
if (confirmAck){
    // 发送延迟等待消息
    rabbitTemplate.convertAndSend("x.ttl","k.ttl" , order.getOrderId());
}
```

## 4.总结

到这里，基本就实现了整个订单延迟自动取消的思路，但事实上还有问题。

投递订单消息到MQ后要投递多一份到延迟队列，可能存在第一次投递成功但投递到延迟队列失败的情况，这里则需要依赖分布式锁或者增加补偿机制；还有编码上的问题，MQ队列名称这些最好抽离出来，当然这里只是demo，就没有那么规范，如果是产品开发，这些都需要最好规定，方便后期维护。


