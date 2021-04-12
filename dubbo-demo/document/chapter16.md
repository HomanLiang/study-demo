[toc]

# Dubbo 面试题

## 说说Dubbo的分层？

从大的范围来说，dubbo分为三层，business业务逻辑层由我们自己来提供接口和实现还有一些配置信息，RPC层就是真正的RPC调用的核心层，封装整个RPC的调用过程、负载均衡、集群容错、代理，remoting则是对网络传输协议和数据转换的封装。

划分到更细的层面，就是图中的10层模式，整个分层依赖由上至下，除开business业务逻辑之外，其他的几层都是SPI机制。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410202903.jpeg)

## 能说下Dubbo的工作原理吗？

1. 服务启动的时候，provider和consumer根据配置信息，连接到注册中心register，分别向注册中心注册和订阅服务
2. register根据服务订阅关系，返回provider信息到consumer，同时consumer会把provider信息缓存到本地。如果信息有变更，consumer会收到来自register的推送
3. consumer生成代理对象，同时根据负载均衡策略，选择一台provider，同时定时向monitor记录接口的调用次数和时间信息
4. 拿到代理对象之后，consumer通过代理对象发起接口调用
5. provider收到请求后对数据进行反序列化，然后通过代理调用具体的接口实现

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410202918.jpeg)

## 为什么要通过代理对象通信？

主要是为了实现接口的透明代理，封装调用细节，让用户可以像调用本地方法一样调用远程方法，同时还可以通过代理实现一些其他的策略，比如：

1、调用的负载均衡策略

2、调用失败、超时、降级和容错机制

3、做一些过滤操作，比如加入缓存、mock数据

4、接口调用数据统计

## 说说服务暴露的流程？

1. 在容器启动的时候，通过ServiceConfig解析标签，创建dubbo标签解析器来解析dubbo的标签，容器创建完成之后，触发ContextRefreshEvent事件回调开始暴露服务
2. 通过ProxyFactory获取到invoker，invoker包含了需要执行的方法的对象信息和具体的URL地址
3. 再通过DubboProtocol的实现把包装后的invoker转换成exporter，然后启动服务器server，监听端口
4. 最后RegistryProtocol保存URL地址和invoker的映射关系，同时注册到服务中心

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410202929.jpeg)

## 说说服务引用的流程？

服务暴露之后，客户端就要引用服务，然后才是调用的过程。

1. 首先客户端根据配置文件信息从注册中心订阅服务

2. 之后DubboProtocol根据订阅的得到provider地址和接口信息连接到服务端server，开启客户端client，然后创建invoker

3. invoker创建完成之后，通过invoker为服务接口生成代理对象，这个代理对象用于远程调用provider，服务的引用就完成了

   

## 有哪些负载均衡策略？

1. 加权随机：假设我们有一组服务器 servers = [A, B, C]，他们对应的权重为 weights = [5, 3, 2]，权重总和为10。现在把这些权重值平铺在一维坐标值上，[0, 5) 区间属于服务器 A，[5, 8) 区间属于服务器 B，[8, 10) 区间属于服务器 C。接下来通过随机数生成器生成一个范围在 [0, 10) 之间的随机数，然后计算这个随机数会落到哪个区间上就可以了。
2. 最小活跃数：每个服务提供者对应一个活跃数 active，初始情况下，所有服务提供者活跃数均为0。每收到一个请求，活跃数加1，完成请求后则将活跃数减1。在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，因此活跃数下降的也越快，此时这样的服务提供者能够优先获取到新的服务请求。
3. 一致性hash：通过hash算法，把provider的invoke和随机节点生成hash，并将这个 hash 投射到 [0, 2^32 - 1] 的圆环上，查询的时候根据key进行md5然后进行hash，得到第一个节点的值大于等于当前hash的invoker。

![图片来自dubbo官方](https://tva1.sinaimg.cn/large/007S8ZIlgy1gjtjbirenpj31920kiq51.jpg)图片来自dubbo官方

1. 加权轮询：比如服务器 A、B、C 权重比为 5:2:1，那么在8次请求中，服务器 A 将收到其中的5次请求，服务器 B 会收到其中的2次请求，服务器 C 则收到其中的1次请求。

## 集群容错方式有哪些？

1. Failover Cluster失败自动切换：dubbo的默认容错方案，当调用失败时自动切换到其他可用的节点，具体的重试次数和间隔时间可用通过引用服务的时候配置，默认重试次数为1也就是只调用一次。
2. Failback Cluster快速失败：在调用失败，记录日志和调用信息，然后返回空结果给consumer，并且通过定时任务每隔5秒对失败的调用进行重试
3. Failfast Cluster失败自动恢复：只会调用一次，失败后立刻抛出异常
4. Failsafe Cluster失败安全：调用出现异常，记录日志不抛出，返回空结果
5. Forking Cluster并行调用多个服务提供者：通过线程池创建多个线程，并发调用多个provider，结果保存到阻塞队列，只要有一个provider成功返回了结果，就会立刻返回结果
6. Broadcast Cluster广播模式：逐个调用每个provider，如果其中一台报错，在循环调用结束后，抛出异常。

## 了解Dubbo SPI机制吗？

SPI 全称为 Service Provider Interface，是一种服务发现机制，本质是将接口实现类的全限定名配置在文件中，并由服务加载器读取配置文件，加载实现类，这样可以在运行时，动态为接口替换实现类。

Dubbo也正是通过SPI机制实现了众多的扩展功能，而且dubbo没有使用java原生的SPI机制，而是对齐进行了增强和改进。

SPI在dubbo应用很多，包括协议扩展、集群扩展、路由扩展、序列化扩展等等。

使用方式可以在META-INF/dubbo目录下配置：

```
key=com.xxx.value
```

然后通过dubbo的ExtensionLoader按照指定的key加载对应的实现类，这样做的好处就是可以按需加载，性能上得到优化。

## 如果让你实现一个RPC框架怎么设计？

1. 首先需要一个服务注册中心，这样consumer和provider才能去注册和订阅服务
2. 需要负载均衡的机制来决定consumer如何调用客户端，这其中还当然要包含容错和重试的机制
3. 需要通信协议和工具框架，比如通过http或者rmi的协议通信，然后再根据协议选择使用什么框架和工具来进行通信，当然，数据的传输序列化要考虑
4. 除了基本的要素之外，像一些监控、配置管理页面、日志是额外的优化考虑因素。

那么，本质上，只要熟悉一两个RPC框架，就很容易想明白我们自己要怎么实现一个RPC框架。

## Dubbo中zookeeper做注册中心，如果注册中心集群全都挂掉，发布者和订阅者之间还能通信么？

- 【提供者】在 【启动】 时，向注册中心zk 【注册】 自己提供的服务。

- 【消费者】在【启动】时，向注册中心zk 【订阅】自己所需的服务。

可以的，消费者在启动时，消费者会从zk拉取注册的生产者的地址接口等数据，缓存在本地。每次调用时，按照本地存储的地址进行调用 。消费者本地有一个生产者的列表，他会按照列表继续工作，倒是无法从注册中心去同步最新的服务列表，短期的注册中心挂掉是不要紧的，但一定要尽快修复

挂掉是不要紧的，但前提是你没有增加新的服务，如果你要调用新的服务，则是不能办到的

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410192214.png)





## Netty 在 Dubbo 中是如何应用的？

### dubbo 的 Consumer 消费者如何使用 Netty
注意：此次代码使用了从 github 上 clone 的 dubbo 源码中的 dubbo-demo 例子。
代码如下：
```
   System.setProperty("java.net.preferIPv4Stack", "true");
   ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
   context.start();
    // @1
   DemoService demoService = (DemoService) context.getBean("demoService"); // get remote service proxy
   int a = 0;
   while (true) {
       try {
           Thread.sleep(1000);
           System.err.println( ++ a + " ");

           String hello = demoService.sayHello("world"); // call remote method
           System.out.println(hello); // get result

       } catch (Throwable throwable) {
           throwable.printStackTrace();
       }
   }
```
当代码执行到 @1 的时候，会调用 Spring 容器的 getBean 方法，而 dubbo 扩展了 FactoryBean，所以，会调用 getObject 方法，该方法会创建代理对象。

这个过程中会调用 DubboProtocol 实例的 getClients（URL url） 方法，当这个给定的 URL 的 client 没有初始化则创建，然后放入缓存，代码如下：

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408004555.png)

这个 initClient 方法就是创建 Netty 的 client 的。

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408004605.png)

最终调用的就是抽象父类 AbstractClient 的构造方法，构造方法中包含了创建 Socket 客户端，连接客户端等行为。

```
public AbstractClient(URL url, ChannelHandler handler) throws RemotingException {
   doOpen();
   connect();
}
```
doOpent 方法用来创建 Netty 的 bootstrap ：
```
protected void doOpen() throws Throwable {
   NettyHelper.setNettyLoggerFactory();
   bootstrap = new ClientBootstrap(channelFactory);
   bootstrap.setOption("keepAlive", true);
   bootstrap.setOption("tcpNoDelay", true);
   bootstrap.setOption("connectTimeoutMillis", getTimeout());
   final NettyHandler nettyHandler = new NettyHandler(getUrl(), this);
   bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
       public ChannelPipeline getPipeline() {
           NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyClient.this);
           ChannelPipeline pipeline = Channels.pipeline();
           pipeline.addLast("decoder", adapter.getDecoder());
           pipeline.addLast("encoder", adapter.getEncoder());
           pipeline.addLast("handler", nettyHandler);
           return pipeline;
       }
   });
}
```
connect 方法用来连接提供者：
```
protected void doConnect() throws Throwable {
   long start = System.currentTimeMillis();
   ChannelFuture future = bootstrap.connect(getConnectAddress());
   boolean ret = future.awaitUninterruptibly(getConnectTimeout(), TimeUnit.MILLISECONDS);
   if (ret && future.isSuccess()) {
       Channel newChannel = future.getChannel();
       newChannel.setInterestOps(Channel.OP_READ_WRITE);
   }
}
```
上面的代码中，调用了 bootstrap 的 connect 方法，熟悉的 Netty 连接操作。当然这里使用的是  jboss 的 netty3，稍微有点区别。点击这篇：[教你用 Netty 实现一个简单的 RPC](https://mp.weixin.qq.com/s?__biz=MzI3ODcxMzQzMw==&mid=2247491548&idx=3&sn=cbb7e36f2d41f2e80feeec5d78b4de13&chksm=eb539aeadc2413fc5d82cb18bb552b84a7fa37764c728e89885d2ed7d0a983978bff29a1e5ab&scene=21#wechat_redirect)。当连接成功后，注册写事件，准备开始向提供者传递数据。

当 main 方法中调用 demoService.sayHello(“world”) 的时候，最终会调用 HeaderExchangeChannel 的 request 方法，通过 channel 进行请求。
```
public ResponseFuture request(Object request, int timeout) throws RemotingException {
   Request req = new Request();
   req.setVersion("2.0.0");
   req.setTwoWay(true);
   req.setData(request);
   DefaultFuture future = new DefaultFuture(channel, req, timeout);
   channel.send(req);
   return future;
}
```
send 方法中最后调用 jboss  Netty 中继承了  NioSocketChannel 的 NioClientSocketChannel 的 write 方法。完成了一次数据的传输。

### dubbo 的 Provider 提供者如何使用 Netty
Provider demo 代码：
```
System.setProperty("java.net.preferIPv4Stack", "true");
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-provider.xml"});
context.start();
System.in.read(); // press any key to exit
```
rovider 作为被访问方，肯定是一个 Server 模式的 Socket。如何启动的呢？

当 Spring 容器启动的时候，会调用一些扩展类的初始化方法，比如继承了 InitializingBean，ApplicationContextAware，ApplicationListener 。

而 dubbo 创建了 ServiceBean 继承了一个监听器。Spring 会调用他的 onApplicationEvent 方法，该类有一个 export 方法，用于打开 ServerSocket 。

然后执行了 DubboProtocol 的 createServer 方法，然后创建了一个NettyServer 对象。NettyServer 对象的 构造方法同样是  doOpen 方法和。

代码如下：
```
protected void doOpen() throws Throwable {
   NettyHelper.setNettyLoggerFactory();
   ExecutorService boss = Executors.newCachedThreadPool(new NamedThreadFactory("NettyServerBoss", true));
   ExecutorService worker = Executors.newCachedThreadPool(new NamedThreadFactory("NettyServerWorker", true));
   ChannelFactory channelFactory = new NioServerSocketChannelFactory(boss, worker, getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS));
   bootstrap = new ServerBootstrap(channelFactory);

   final NettyHandler nettyHandler = new NettyHandler(getUrl(), this);
   channels = nettyHandler.getChannels();
   bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
       public ChannelPipeline getPipeline() {
           NettyCodecAdapter adapter = new NettyCodecAdapter(getCodec(), getUrl(), NettyServer.this);
           ChannelPipeline pipeline = Channels.pipeline();
           pipeline.addLast("decoder", adapter.getDecoder());
           pipeline.addLast("encoder", adapter.getEncoder());
           pipeline.addLast("handler", nettyHandler);
           return pipeline;
       }
   });
   channel = bootstrap.bind(getBindAddress());
}
```
该方法中，看到了熟悉的 boss 线程，worker 线程，和 ServerBootstrap，在添加了编解码 handler  之后，添加一个 NettyHandler，最后调用 bind 方法，完成绑定端口的工作。和我们使用 Netty 是一摸一样。

### 总结
可以看到，dubbo 使用 Netty 还是挺简单的，消费者使用 NettyClient，提供者使用 NettyServer，Provider  启动的时候，会开启端口监听，使用我们平时启动 Netty 一样的方式。

而 Client 在 Spring getBean 的时候，会创建 Client，当调用远程方法的时候，将数据通过 dubbo 协议编码发送到 NettyServer，然后 NettServer 收到数据后解码，并调用本地方法，并返回数据，完成一次完美的 RPC 调用。