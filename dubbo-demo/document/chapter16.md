[toc]



# Dubbo 调优

## 1.限流策略

### 1.1.前情导读

高并发环境下若生产者不能及时处理请求造成大量请求线程积压，最终会演变为大面积服务崩溃现象产生。根据服务特点设定合理的请求拒绝策略，保证服务正常运行是本文重点。当然必须区别于`负载均衡只能分配流量而不能限制流量`

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904172657.webp)



### 1.2.消费端actives

仅针对消费者端生效，只能在`<dubbo:reference>`亦或是其子标签`<dubbo:method>`或者是`<dubbo:consumer>`中配置。

#### 1.2.1 配置示例

- [dubbo:consumer](https://link.juejin.cn?target=)中配置针对所有服务所有方法生效

- [dubbo:consumer](https://link.juejin.cn?target=)中配置针对该服务所有方法生效

- dubbo:method

  中配置针对该方法生效

  ![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173118.webp)

#### 1.2.2 参数详解

| 描述     | 备注                                                 |
| -------- | ---------------------------------------------------- |
| 作用     | 消费者最大并发数量限制，超过限制将会抛出异常         |
| 实现     | 过滤器Filter，具体实现子类为ActiveLimitFilter        |
| 默认值   | 0表示没有限制                                        |
| 配置地点 | <dubbo:consumer>、<dubbo:reference> 、<dubbo:method> |

#### 1.2.3 源码导读

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173155.webp)

1. **处理请求参数**：URL为Dubbo封装的一个请求对象类，包含Map<String, String>类型属性numbers，该属性中含有actives配置
2. **请求过滤判断**：RpcStatus类封装生产者调用状态，AtomicInteger原子类型active属性存储当前调用数量。通过其与URL中获取到的对应参数属性值比较判断
3. **请求返回结果**：如果允许则进行下一步RPC调用，不允许则会暂停等待线程timeout参数时长，若唤醒还未有空余线程则抛出异常

### 1.3.消费端connections

大家熟悉的HTTP协议就属于短连接，每次请求的时候都会多次验证握手建立连接。默认的Dubbo协议属于长连接，采用NIO异步传输，每消费者与生产者之间默认采用单一长连接方式通信。换个简单说法就是每个消费者与生产者之间长连接默认就创建一个，所有请求共用

connections参数针对上述长连接与短连接具备不同作用效果：

- 短连接因为是多连接所以限制其个数
- 长连接因为是单一连接所以是指定其创建数量

#### 1.3.1 配置示例

connections参数生效的位置在消费端，图一表示消费端的配置，图二表示在生产者的配置。根据自身测试以及github验证，生产端的配置确实会通过注册中心传递给消费端生效

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173238.webp)

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173302.webp)

#### 1.3.2 参数详解

| 描述     | 备注                                                         |
| -------- | ------------------------------------------------------------ |
| 作用     | 限制消费者短连接数量，长连接创建数量                         |
| 实现     | 初始化连接时根据参数控制                                     |
| 默认值   | 长连接默认表示使用JVM共享长连接，线上一般都是多生产多消费，这个参数不建议更改 |
| 配置地点 | <dubbo:consumer>、<dubbo:reference> 、<dubbo:provider>、<dubbo:service> |

#### 1.3.3 源码导读

首先项目初始化的时候会根据connections参数初始化连接，过程在DubboProtocol类的getClients()方法中，下图是debug跟进的初始化结果。可以看到用于储存连接的数组最后返回的是两个连接实例

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173315.webp)

连接使用发生在类DubboInvoker中，该类的方法doInvoke()用于执行调用逻辑。使用的连接就是在DubboProtocol类中getClients()初始化出来并在方法refer()中放入DubboInvoker对象的连接。如下图所示是DubboInvoker中doIncoke()使用连接的关键代码

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173343.webp)

### 1.4.生产端accepts

消费者可以通过connections参数设置连接的数量，但是如果生产者不进行自我保护，采用默认的无限制连接策略。高并发情况下生产者可能就会因为连接数量巨大崩溃，这时可以通过参数accepts限制生产者可接受最大连接数量

#### 1.4.1 配置示例

accepts用于生产者限制最大连接数量保护自身服务可用性，可以在标签`<dubbo:protocol>`中进行配置。这时候在`<dubbo:reference>`中设置connections超过accepts值，用于方便后续的源码跟进

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173532.webp)

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173559.webp)4.2 参数详解

| 描述     | 备注                                                         |
| -------- | ------------------------------------------------------------ |
| 作用     | 限制生产者最大可接受连接数量，用于保护生产者自身             |
| 实现     | 消费者初始化创建连接时会打开创建链接，这时候就会根据限制参数判断 |
| 默认值   | 0表示没有限制，比较危险的配置                                |
| 配置地点 | <dubbo:protocol>                                             |

#### 1.4.3 源码导读

生产者启动初始化过程中可以看到开启连接的时候获取了参数accepts的设置，过程在AbstractServer类构造函数中可以看到

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173615.webp)

消费端初始化的时候当超过生产者限制连接数量后，在AbstractClient类中可以看到，构造函数中调用方法connect()创建连接。这时候会抛出异常，因为异常原因是等待创建连接超时3000ms。验证参数accepts效果


![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173735.webp)



### 1.5.生产端线程池

多线程并发操作一定离不开线程池，Dubbo自身提供了支持了四种线程池类型支持。生产者`<dubbo:protocol>`标签中可配置线程池关键参数，线程池类型、阻塞队列大小、核心线程数量等

#### 1.5.1 iothreads、threads

- **iothreads**：限制的是io线程池大小，该线程池线程用于处理Dubbo框架自身业务逻辑。默认值为`CPU+1`，不建议更改设置
- **threads**：用于指定下面讲到的业务线程池线程数量，这个才是业务需要关心的线程数量。默认大小`200`

#### 1.5.2 threadpool

参数threadpool指定使用线程池类型，Dubbo中自身实现提供了如下表所示四种线程池。默认使用固定大小线程池FixedThreadPool

| 类型名称          | 队列类型                                                     | 特性备注                                                     |
| ----------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| FixedThreadPool   | `queues`属性为0创建无容量阻塞队列`SynchronousQueue`，若 `queues`小于0则创建Integer.MAX_VALUE容量`LinkedBlockingQueue`阻塞队列，大于0则创建 `queues`参数限定容量`LinkedBlockingQueue`阻塞队列 | 核心线程数量与最大线程数量一致采用参数`threads`值、线程空闲存活时间0 |
| CachedThreadPool  | 队列创建类型规则与`FixedThreadPool`一致                      | 相对于固定容量大小FixedThreadPool线程池多了参数`corethreads`设置核心线程数量支持默认0，线程空闲存活时间暂时未提供参数设置，默认1分钟 |
| LimitedThreadPool | 队列创建类型规则与`FixedThreadPool`一致                      | 相对于CachedThradPool而言最大的变化在于线程存活时间修改为Long.MAX_VALUE |
| EagerThreadPool   | 队列为Dubbo设计实现的`TaskQueue`队列，该队列继承自`LinkedBlockingQueue`。当`queues`参数小于等于0则其容量为1，若大于0则容量为`queues`参数值 | 后面会有专门文章研究这个线程池实现                           |

#### 1.5.3 注意

Dubbo官网文档只描述了fixed/cached，四种线程池默认支持的是fixed

### 1.6.生产端executes

一个只能在生产者即[dubbo:service](https://link.juejin.cn?target=)亦或是其子标签[dubbo:method](https://link.juejin.cn?target=)中配置的属性，消费者中配置不会生效。这个参数主要目的是在生产者端限制应用线程使用数量

#### 1.6.1 配置示例

限制该服务每个方法并发不超过10，其中dubboProtocolGetMethod方法并发不超过2。方法级别的配置优先级高于服务配置

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904173956.webp)

#### 1.6.2 参数详解

| 配置地点 | 生产者[dubbo:service](https://link.juejin.cn?target=)标签或其子标签[dubbo:method](https://link.juejin.cn?target=)中 |
| -------- | ------------------------------------------------------------ |
| 默认值   | 0表示没有限制                                                |
| 作用     | 服务提供者每个方法只能占用线程池中配置数量线程，超出则抛出异常 |
| 实现     | 过滤器Filter                                                 |

#### 1.6.3 源码导读

- 主要涉及类：ExecuteLimitFilter，关注相关类RpcStatus、URL

- 主要方法：getMethodParameter()、beginCount()、getStatus()

  ![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904174107.webp)

1. 处理请求参数，URL为Dubbo封装的一个请求对象类，包含Map<String, String>类型属性numbers，该属性中含有executes配置
2. 提取executes参数值，numbers -- paramters -- 默认值顺序返回
3. 比较executes值数量，RpcStatus类封装生产者调用状态，AtomicInteger原子类型active属性存储当前调用数量





## 2.启动检查

dubbo的启动检查是在启动服务消费者的时候，是否进行检查消费者中引用的服务接口是否在注册中心中是否存在。默认检查开启。检查只存在与dubbo消费者工程中，只要全局级别和类级别上。

### 2.1.使用xml方式配置开启关闭检查

与 timeout、retries 一样 全局配置与 <dubbo:consumer > 全局、<dubbo:reference >引用接口上。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904222039.png)

### 2.2 使用注解方式配置开启检查

作用与属性注解@Reference中。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20220904222104.png)

1. 默认开启检查时，正常启动时先启动服务提供者工程，在启动服务消费者工程，启动过程无问题；

2. 默认开启检查时，先启动服务消费者工程，则在启动过程中进行检查，当有引用的服务不存在时，则会抛出异常：

   ```
   org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'orderController': Unsatisfied dependency expressed through field 'orderService'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'orderServiceImpl': Injection of @Reference dependencies is failed; nested exception is java.lang.IllegalStateException: Failed to check the status of the service com.xiaohui.service.UserService. No provider available for the service com.xiaohui.service.UserService:2.0.0 from the url zookeeper://172.18.230.163:2181/org.apache.dubbo.registry.RegistryService?anyhost=true&application=order-service-consumer&bean.name=ServiceBean:com.xiaohui.service.UserService:2.0.0&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=com.xiaohui.service.UserService&lazy=false&methods=queryAllUserAddress&pid=11228&qos.enable=false&register=true&register.ip=10.4.41.51&release=2.7.3&remote.application=user-service-provider&retries=3&revision=2.0.0&side=consumer&sticky=false&timestamp=1599573920786&version=2.0.0 to the consumer 10.4.41.51 use dubbo version 2.7.3
   
   Caused by: org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'orderServiceImpl': Injection of @Reference dependencies is failed; nested exception is java.lang.IllegalStateException: Failed to check the status of the service com.xiaohui.service.UserService. No provider available for the service com.xiaohui.service.UserService:2.0.0 from the url zookeeper://172.18.230.163:2181/org.apache.dubbo.registry.RegistryService?anyhost=true&application=order-service-consumer&bean.name=ServiceBean:com.xiaohui.service.UserService:2.0.0&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=com.xiaohui.service.UserService&lazy=false&methods=queryAllUserAddress&pid=11228&qos.enable=false&register=true&register.ip=10.4.41.51&release=2.7.3&remote.application=user-service-provider&retries=3&revision=2.0.0&side=consumer&sticky=false&timestamp=1599573920786&version=2.0.0 to the consumer 10.4.41.51 use dubbo version 2.7.3
   Caused by: java.lang.IllegalStateException: Failed to check the status of the service com.xiaohui.service.UserService. No provider available for the service com.xiaohui.service.UserService:2.0.0 from the url zookeeper://172.18.230.163:2181/org.apache.dubbo.registry.RegistryService?anyhost=true&application=order-service-consumer&bean.name=ServiceBean:com.xiaohui.service.UserService:2.0.0&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=com.xiaohui.service.UserService&lazy=false&methods=queryAllUserAddress&pid=11228&qos.enable=false&register=true&register.ip=10.4.41.51&release=2.7.3&remote.application=user-service-provider&retries=3&revision=2.0.0&side=consumer&sticky=false&timestamp=1599573920786&version=2.0.0 to the consumer 10.4.41.51 use dubbo version 2.7.3
   ```

   

3. 当关闭检查后，先启动服务消费者工程，则正常启动不会报错。



## 3.Dubbo超时和重连机制

**简介：** dubbo启动时默认有重试机制和超时机制。 超时机制的规则是如果在一定的时间内，provider没有返回，则认为本次调用失败， 重试机制在出现调用失败时，会再次调用。如果在配置的调用次数内都失败，则认为此次请求异常，抛出异常。

dubbo启动时默认有重试机制和超时机制。
超时机制的规则是如果在一定的时间内，provider没有返回，则认为本次调用失败，
重试机制在出现调用失败时，会再次调用。如果在配置的调用次数内都失败，则认为此次请求异常，抛出异常。

如果出现超时，通常是业务处理太慢，可在服务提供方执行：jstack PID > jstack.log 分析线程都卡在哪个方法调用上，这里就是慢的原因。
如果不能调优性能，请将timeout设大。

**某些业务场景下，如果不注意配置超时和重试，可能会引起一些异常。**

### 3.1.超时设置

DUBBO消费端设置超时时间需要根据业务实际情况来设定，
如果设置的时间太短，一些复杂业务需要很长时间完成，导致在设定的超时时间内无法完成正常的业务处理。
这样消费端达到超时时间，那么dubbo会进行重试机制，不合理的重试在一些特殊的业务场景下可能会引发很多问题，需要合理设置接口超时时间。
比如发送邮件，可能就会发出多份重复邮件，执行注册请求时，就会插入多条重复的注册数据。

**（1）合理配置超时和重连的思路**

1.对于核心的服务中心，去除dubbo超时重试机制，并重新评估设置超时时间。
2.业务处理代码必须放在服务端，客户端只做参数验证和服务调用，不涉及业务流程处理

**（2）Dubbo超时和重连配置示例**

```
<!-- 服务调用超时设置为5秒,超时不重试--> 
<dubbo:service interface="com.provider.service.DemoService" ref="demoService"  retries="0" timeout="5000"/>
```



### 3.2.重连机制

dubbo在调用服务不成功时，默认会重试2次。
Dubbo的路由机制，会把超时的请求路由到其他机器上，而不是本机尝试，所以 dubbo的重试机器也能一定程度的保证服务的质量。
但是如果不合理的配置重试次数，当失败时会进行重试多次，这样在某个时间点出现性能问题，调用方再连续重复调用，
系统请求变为正常值的retries倍，系统压力会大增，容易引起服务雪崩，需要根据业务情况规划好如何进行异常处理，何时进行重试。



## 4.参数优化

### 4.1.消费者端配置

1. `dubbo.consumer.timeout=3000`，控制消费者等待服务端返回消息的最大时间，默认1秒；【默认配置下，某些服务端又存在慢方法，很容易导致请求超时报错；】

### 4.2.服务提供端配置

1. `dubbo.protocol.threadpool=cached`，配置业务线程池类型；其他线程池类型如下：【若不配置，线程池默认使用`SynchronousQueue`队列（除eager 线程池），`SynchronousQueue`没有容量，是无缓冲等待队列，是一个不存储元素的阻塞队列，会直接将任务交给消费者，必须等队列中的添加元素被消费后才能继续添加新的元素】

   > 1. **fixed** 固定大小线程池，启动时建立线程，不关闭，一直持有；(**默认**)
   > 2. **cached** 缓存线程池，空闲一分钟自动删除，需要时重建；
   > 3. **limited** 可伸缩线程池，但池中的线程数只会增长不会收缩。只增长不收缩的目的是为了避免收缩时突然来了大流量引起的性能问题；
   > 4. **eager** 优先创建Worker线程池。在任务数量大于corePoolSize但是小于maximumPoolSize时，优先创建Worker来处理任务。当任务数量大于maximumPoolSize时，将任务放入阻塞队列中。阻塞队列充满时抛出RejectedExecutionException。(相比于cached，cached在任务数量超过maximumPoolSize时直接抛出异常而不是将任务放入阻塞队列)。

2. `dubbo.protocol.threads=10`，限制业务线程池最大线程数；等于并发访问量，超过线程数的请求直接触发拒绝策略；（默认fixed 线程池最大200个线程）
    

3. `dubbo.protocol.dispatcher=message`，配置dispatcher调度模式；【一般情况下建议配置调度模式为`message`】，其他调度模式如下：

   > 1. **all** 所有消息都派发到线程池，包括请求，响应，连接事件，断开事件，心跳等；
   > 2. **direct** 所有消息都不派发到线程池，全部在 IO 线程上直接执行；
   > 3. **message** 只有请求响应消息派发到线程池，其它连接断开事件，心跳等消息，直接在 IO 线程上执行；
   > 4. **execution** 只有请求消息派发到线程池，不含响应，响应和其它连接断开事件，心跳等消息，直接在 IO 线程上执行；
   > 5. **connection** 在 IO 线程上，将连接断开事件放入队列，有序逐个执行，其它消息派发到线程池。

4. `dubbo.provider.actives=10`，每个服务消费者，个方法最大并发调用数；从消费端控制，并发数是业务线程池大小的子集。
    

5. `dubbo.provider.executes=10`，每个服务提供者各方法最大可并行执行请求数；从提供端控制，并发数是业务线程池大小的子集（小于等于业务线程池大小）。

























