[toc]



# Dubbo 超时配置

## 1.RPC场景

本文所有问题均以下图做为业务场景，一个 `web api` 做为前端请求，`product service` 是产品服务，其中调用 `comment service` (评论服务)获取产品相关评论，`comment service` 从持久层中加载数据。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410170111.png)

## 2.超时是针对消费端还是服务端？

- 如果是针对消费端，那么当消费端发起一次请求后，如果在规定时间内未得到服务端的响应则直接返回超时异常，但服务端的代码依然在执行。
- 如果是针对服务端，那么当消费端发起一次请求后，一直等待服务端的响应，服务端在方法执行到指定时间后如果未执行完，此时返回一个超时异常给到消费端。

`dubbo` 的超时是针对客户端的，由于是一种 `NIO` 模式，消费端发起请求后得到一个 `ResponseFuture`，然后消费端一直轮询这个`ResponseFuture` 直至超时或者收到服务端的返回结果。虽然超时了，但仅仅是消费端不再等待服务端的反馈并不代表此时服务端也停止了执行。

> 按上图的业务场景，看看生成的日志：

`product service`:报超时错误，因为 `comment service` 加载数据需要 `5S`，但接口只等 `1S`。

```java
Caused by: com.alibaba.dubbo.remoting.TimeoutException: Waiting server-side response timeout. start time: 2017-08-05 18:14:52.751, end time: 2017-08-05 18:14:53.764, client elapsed: 6 ms, server elapsed: 1006 ms, timeout: 1000 ms, request: Request [id=0, version=2.0.0, twoway=true, event=false, broken=false, data=RpcInvocation [methodName=getCommentsByProductId, parameterTypes=[class java.lang.Long], arguments=[1], attachments={traceId=6299543007105572864, spanId=6299543007105572864, input=259, path=com.jim.framework.dubbo.core.service.CommentService, interface=com.jim.framework.dubbo.core.service.CommentService, version=0.0.0}]], channel: /192.168.10.222:53204 -> /192.168.10.222:7777
    at com.alibaba.dubbo.remoting.exchange.support.DefaultFuture.get(DefaultFuture.java:107) ~[dubbo-2.5.3.jar:2.5.3]
    at com.alibaba.dubbo.remoting.exchange.support.DefaultFuture.get(DefaultFuture.java:84) ~[dubbo-2.5.3.jar:2.5.3]
    at com.alibaba.dubbo.rpc.protocol.dubbo.DubboInvoker.doInvoke(DubboInvoker.java:96) ~[dubbo-2.5.3.jar:2.5.3]
    ... 42 common frames omitted
```

`comment service`: 并没有异常，而是慢慢悠悠的执行自己的逻辑：

```java
2017-08-05 18:14:52.760  INFO 846 --- [2:7777-thread-5] c.j.f.d.p.service.CommentServiceImpl     : getComments start:Sat Aug 05 18:14:52 CST 2017
2017-08-05 18:14:57.760  INFO 846 --- [2:7777-thread-5] c.j.f.d.p.service.CommentServiceImpl     : getComments end:Sat Aug 05 18:14:57 CST 2017
```

> 从日志来看，超时影响的是消费端，与服务端没有直接关系。

## 3.超时在哪设置？

**消费端**

- 全局控制

    ```markup
    <dubbo:consumer timeout="1000"></dubbo:consumer>
    ```

- 接口控制
- 方法控制

**服务端**

- 全局控制

    ```markup
    <dubbo:provider timeout="1000"></dubbo:provider>
    ```

- 接口控制
- 方法控制

可以看到 `dubbo` 针对超时做了比较精细化的支持，无论是消费端还是服务端，无论是接口级别还是方法级别都有支持。

## 4.超时设置的优先级是什么？

> 上面有提到dubbo支持多种场景下设置超时时间，也说过超时是针对消费端的。那么既然超时是针对消费端，为什么服务端也可以设置超时呢？

这其实是一种策略，其实服务端的超时配置是消费端的缺省配置，即如果服务端设置了超时，任务消费端可以不设置超时时间，简化了配置。

另外针对控制的粒度，`dubbo`支持了接口级别也支持方法级别，可以根据不同的实际情况精确控制每个方法的超时时间。所以最终的优先顺序为：`客户端方法级>服务端方法级>客户端接口级>服务端接口级>客户端全局>服务端全局`

## 5.超时的实现原理是什么？

之前有简单提到过, `dubbo` 默认采用了 `netty` 做为网络组件，它属于一种 `NIO` 的模式。消费端发起远程请求后，线程不会阻塞等待服务端的返回，而是马上得到一个 `ResponseFuture`，消费端通过不断的轮询机制判断结果是否有返回。因为是通过轮询，轮询有个需要特别注要的就是避免死循环，所以为了解决这个问题就引入了超时机制，只在一定时间范围内做轮询，如果超时时间就返回超时异常。

**ResponseFuture接口定义**

```
public interface ResponseFuture {

    /**
     * get result.
     * 
     * @return result.
     */
    Object get() throws RemotingException;

    /**
     * get result with the specified timeout.
     * 
     * @param timeoutInMillis timeout.
     * @return result.
     */
    Object get(int timeoutInMillis) throws RemotingException;

    /**
     * set callback.
     * 
     * @param callback
     */
    void setCallback(ResponseCallback callback);

    /**
     * check is done.
     * 
     * @return done or not.
     */
    boolean isDone();

}
```

**ReponseFuture的实现类：DefaultFuture**

只看它的 `get` 方法，可以清楚看到轮询的机制。

```java
public Object get(int timeout) throws RemotingException {
        if (timeout <= 0) {
            timeout = Constants.DEFAULT_TIMEOUT;
        }
        if (! isDone()) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                while (! isDone()) {
                    done.await(timeout, TimeUnit.MILLISECONDS);
                    if (isDone() || System.currentTimeMillis() - start > timeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            if (! isDone()) {
                throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
            }
        }
        return returnFromResponse();
    }
```

## 6.超时解决的是什么问题？

设置超时主要是解决什么问题？如果没有超时机制会怎么样？

> 回答上面的问题，首先要了解 `dubbo` 这类 `rpc` 产品的线程模型。下图是我之前个人 `RPC` 学习产品的示例图，与 `dubbo` 的线程模型大致是相同的

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171016.jpeg)

我们从 `dubbo` 的源码看下这下线程模型是怎么用的：

**netty boss**

主要是负责socket连接之类的工作。

**netty wokers**

将一个请求分给后端的某个 `handle` 去处理，比如心跳 `handle` ,执行业务请求的 `handle` 等。

> `Netty Server` 中可以看到上述两个线程池是如何初始化的：

首选是 `open` 方法，可以看到一个 `boss` 一个 `worker` 线程池。

```java
protected void doOpen() throws Throwable {
        NettyHelper.setNettyLoggerFactory();
        ExecutorService boss = Executors.newCachedThreadPool(new NamedThreadFactory("NettyServerBoss", true));
        ExecutorService worker = Executors.newCachedThreadPool(new NamedThreadFactory("NettyServerWorker", true));
        ChannelFactory channelFactory = new NioServerSocketChannelFactory(boss, worker, getUrl().getPositiveParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS));
        bootstrap = new ServerBootstrap(channelFactory);
        // ......
}
```

再看 `ChannelFactory` 的构造函数：

```java
public NioServerSocketChannelFactory(Executor bossExecutor, Executor workerExecutor, int workerCount) {
    this(bossExecutor, 1, workerExecutor, workerCount);
}
```

可以看出，`boss` 线程池的大小为1，`worker` 线程池的大小也是可以配置的，默认大小是当前系统的核心数 `+1`，也称为 `IO` 线程。

**busines（业务线程池）**

> 为什么会有业务线程池，这里不多解释，可以参考我上面的文章。

缺省是采用固定大小的线程池，`dubbo` 提供了三种不同类型的线程池供用户选择。我们看看这个类：`AllChannelHandler`，它是其中一种 `handle`，处理所有请求，它的一个作用就是调用业务线程池去执行业务代码，其中有获取线程池的方法：

```java
 private ExecutorService getExecutorService() {
        ExecutorService cexecutor = executor;
        if (cexecutor == null || cexecutor.isShutdown()) { 
            cexecutor = SHARED_EXECUTOR;
        }
        return cexecutor;
    }
```

上面代码中的变量 `executor` 来自于 `AllChannelHandler` 的父类 `WrappedChannelHandler`，看下它的构造函数：

```java
public WrappedChannelHandler(ChannelHandler handler, URL url) {
       //......
        executor = (ExecutorService) ExtensionLoader.getExtensionLoader(ThreadPool.class).getAdaptiveExtension().getExecutor(url);

        //......
}
```

获取线程池来自于 `SPI` 技术,从代码中可以看出线程池的缺省配置就是上面提到的固定大小线程池。

```java
@SPI("fixed")
public interface ThreadPool {
    
    /**
     * 线程池
     * 
     * @param url 线程参数
     * @return 线程池
     */
    @Adaptive({Constants.THREADPOOL_KEY})
    Executor getExecutor(URL url);

}
```

最后看下是如何将请求丢给线程池去执行的，在 `AllChannelHandler` 中有这样的方法：

```java
public void received(Channel channel, Object message) throws RemotingException {
        ExecutorService cexecutor = getExecutorService();
        try {
            cexecutor.execute(new ChannelEventRunnable(channel, handler, ChannelState.RECEIVED, message));
        } catch (Throwable t) {
            throw new ExecutionException(message, channel, getClass() + " error when process received event .", t);
        }
    }
```

> 典型问题：拒绝服务

如果上面提到的 `dubbo` 线程池模型理解了，那么也就容易理解一个问题，当前端大量请求并发出现时，很有可以将业务线程池中的线程消费完，因为默认缺省的线程池是固定大小（我现在版本缺省线程池大小为200），此时会出现服务无法按预期响应的结果，当然由于是固定大小的线程池，当核心线程滿了后也有队列可排,但默认是不排队的，需要排队需要单独配置，我们可以从线程池的具体实现中看：

```java
public class FixedThreadPool implements ThreadPool {

    public Executor getExecutor(URL url) {
        String name = url.getParameter(Constants.THREAD_NAME_KEY, Constants.DEFAULT_THREAD_NAME);
        int threads = url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS);
        int queues = url.getParameter(Constants.QUEUES_KEY, Constants.DEFAULT_QUEUES);
        return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, 
                queues == 0 ? new SynchronousQueue<Runnable>() : 
                    (queues < 0 ? new LinkedBlockingQueue<Runnable>() 
                            : new LinkedBlockingQueue<Runnable>(queues)),
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name, url));
    }

}
```

上面代码的结论是：

- 默认线程池大小为 `200`（不同的 `dubbo` 版本可能此值不同）
- 默认线程池不排队，如果需要排队，需要指定队列的大小

当业务线程用完后，服务端会报如下的错误：

```java
Caused by: java.util.concurrent.RejectedExecutionException: Thread pool is EXHAUSTED! Thread Name: DubboServerHandler-192.168.10.222:9999, Pool Size: 1 (active: 1, core: 1, max: 1, largest: 1), Task: 8 (completed: 7), Executor status:(isShutdown:false, isTerminated:false, isTerminating:false), in dubbo://192.168.10.222:9999!
    at com.alibaba.dubbo.common.threadpool.support.AbortPolicyWithReport.rejectedExecution(AbortPolicyWithReport.java:53) ~[dubbo-2.5.3.jar:2.5.3]
    at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:823) [na:1.8.0_121]
    at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1369) [na:1.8.0_121]
    at com.alibaba.dubbo.remoting.transport.dispatcher.all.AllChannelHandler.caught(AllChannelHandler.java:65) ~[dubbo-2.5.3.jar:2.5.3]
    ... 17 common frames omitted
```

通过上面的分析，对调用的服务设置超时时间，是为了避免因为某种原因导致线程被长时间占用，最终出现线程池用完返回拒绝服务的异常。

## 7.超时与服务降级

按我们文章之前的场景，`web api` 请求产品明细时调用 `product service`，为了查询产品评论 `product service` 调用 `comment service`。如果此时由于 `comment service` 异常，响应时间增大到 `10S`（远大于上游服务设置的超时时间），会发生超时异常，进而导致整个获取产品明细的接口异常，这也就是平常说的强依赖。这类强依赖是超时不能解决的，解决方案一般是两种：

- 调用 `comment service` 时做异常捕获，返回空值或者返回具体的错误码，消费端根据不同的错误码做不同的处理。
- 调用 `coment service` 做服务降级，比如发生异常时返回一个 `mock` 的数据，`dubbo` 默认支持 `mock`。

只有通过做异常捕获或者服务降级才能确保某些不重要的依赖出问题时不影响主服务的稳定性。而超时就可以与服务降级结合起来，当消费端发生超时时自动触发服务降级， 这样即使我们的评论服务一直慢，但不影响获取产品明细的主体功能，只不过会牺牲部分体验，用户看到的评论不是真实的，但评论相对是个边缘功能，相比看不到产品信息要轻的多，某种程度上是可以舍弃的。



## 8.Dubbo超时机制导致的雪崩连接

**Bug** **影响：** `Dubbo` 服务提供者出现无法获取 `Dubbo` 服务处理线程异常，后端 `DB` 爆出拿不到数据库连接池，导致前端响应时间异常飙高，系统处理能力下降，核心基础服务无法提供正常服务。

**Bug** **发现过程：**

线上，对于高并发的服务化接口应用，时常会出现 `Dubbo`连接池爆满情况，通常，我们理所应当的认为，这是客户端并发连接过高所致，一方面调整连接池大小， 一方面考虑去增加服务接口的机器，当然也会考虑去优化服务接口的应用。很自然的，当我们在线上压测一个营销页面（为大促服务，具备高并发）时，我们遇到了 这种情况。而通过不断的深入研究，我发现了一个特别的情况。

场景描述：

![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171244.png)

压力从 `Jmeter` 压至前端 `web` 应用 `marketingfront`，场景是批量获取30个产品的信息。`wsproductreadserver` 有一个批量接口，会循环从 `tair` 中获取产品信息，若缓存不存在，则命中 `db`。

**压测后有两个现象：**

1. `Dubbo` 的服务端爆出大量连接拿不到的异常，还伴随着无法获取数据库连接池的情况

   ![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171314.png)

2. `Dubbo Consumer` 端有大量的 `Dubbo` 超时和重试的异常，且重试 `3` 次后，均失败。

   `Dubbo Consumer` 端的最大并发时 `91` 个

   ![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171329.jpeg)

   `Dubbo Provider` 端的最大并发却是 `600` 个，而服务端配置的 `dubbo` 最大线程数即为 `600`。

   ![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171400.png)

这个时候，出于性能测试的警觉性，发现这两个并发数极为不妥。

按照正常的请求模式，`DubboConsumer` 和 `DubboProvider` 展示出来的并发应该是一致的。此处为何会出现服务端的并发数被放大 `6` 倍，甚至有可能不止 `6` 倍，因为服务端的 `dubbo` 连接数限制就是 `600`。

此处开始发挥性能测试各种大胆猜想：

1. 是否是因为服务端再 `dubboServerHandle` 处理请求时，开启了多线程，而这块儿的多线程会累计到 `Dubbo` 的连接上，`dragoon` 采集的这个数据可以真实的反应目前应用活动的线程对系统的压力情况；
2. 压测环境不纯洁？我的小伙伴们在偷偷和我一起压测？（这个被我生生排除了，性能测试基本环境还是要保持独立性）
3. 是否是因为超时所致？这里超时会重试3次，那么顺其自然的想，并发有可能最多会被放大到3倍，`3*91=273<<600`....还是不止3倍？

有了猜想，就得小心求证！

首先通过和 `dubbo` 开发人员 【草谷】分析，`Dubbo` 连接数爆满的原因，猜想1被否决，`Dubbo` 服务端连接池是计数`DubboServerHandle` 个数的业务是否采用多线程无关。

通过在压测时，`Dump provider` 端的线程数，也证明了这个。

![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171416.jpeg)

那么，可能还是和超时有很大关系。

再观察 `wsproductreadserver` 接口的处理时间分布情况：

![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171426.png)

从 RT 的分布来看 。基本上 78.5% 的响应时间是超过 1s 的。那么这个接口方法的 `dubbo` 超时时间是 `500ms`，此时 `dubbo` 的重试机制会带来怎样的 **雪崩效应** 呢？

![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171436.png)

如果按照上图，虽然客户端只有1个并发在做操作，但是由于服务端执行十分耗时，每个请求的执行RT远远超过了超时时间500ms，此时服务端的最大并发会有多少呢?

和服务端处理的响应时间有特比特别大的关系。服务端处理时间变长，但是如果超时，客户端的阻塞时间却只有可怜的500ms，超过500ms，新一轮压力又将发起。

上图可直接看到的并发是8个，如果服务端RT再长些，那么并发可能还会再大些！

这也是为什么从 `marketingfront consumer` 的 `dragoon` 监控来看，只有 `90` 个并发。但是到服务端，却导致 `dubbo` 连接池爆掉的直接原因。

查看了 `wsproductreadserver` 的堆栈，`600` 个 `dubboServerHandle` 大部分都在做数据库的读取和数据库连接获取以及 `tair` 的操作。

![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171516.jpeg)

所以，为什么 `Dubbo` 服务端的连接池会爆掉？很有可能就是因为你的服务接口，在高并发下的大部分 `RT` 分布已经超过了你的 `Dubbo` 设置的超时时间！这将直接导致 `Dubbo` 的重试机制会不断放大你的服务端请求并发。

所 以如果，你在线上曾经遇到过类似场景，您可以采取去除 `Dubbo` 的重试机器，并且合理的设置 `Dubbo` 的超时时间。目前国际站的服务中心，已经开始去除 `Dubbo` 的重试机制。当然 `Dubbo` 的重试机制其实是非常好的 `QOS` 保证，它的路由机制，是会帮你把超时的请求路由到其他机器上，而不是本机尝试，所以 `dubbo` 的重试机器也能一定程度的保证服务的质量。但是请一定要综合线上的访问情况，给出综合的评估。

------------ **等等等，别着急，我们似乎又忽略了一些细节，元芳，你怎么看？** ------------------------

我们重新回顾刚才的业务流程架构，`wsproductReadserver` 层有 `DB` 和 `tair` 两级存储。那么对于同样接口为什么服务化的接口RT如此之差，按照前面提到的架构，包含 `tair` 缓存，怎么还会有数据库连接获取不到的情况？

接续深入追踪，将问题暴露和开发讨论，他们拿出 `tair`

可以看到，客户端提交批量查询 `30` 个产品的产品信息。在服务端，有一个缓存模块，缓存的 `key` 是产品的 `ID`。当产品命中 `tair` 时，则直接返回，若不命中，那么回去 `db` 中取数，再放入缓存中。

这里可以发现一个潜在的性能问题：

客 户端提交 `30` 个产品的查询请求，而服务端，则通过 `for` 循环和 `tair` 交互，所以这个接口在通常情况下的性能估计也得超过 `60-100ms`。如果不是 `30` 个产品，而是 `50` 或者 `100`，那么这个接口的性能将会衰减的非常厉害！（这纯属性能测试的yy，当然这个暂时还不是我们本次关注的主要原因）

那么如此的架构，请求打在 `db` 上的可能性是比较小的， 由缓存命中率来保证。从线上真实的监控数据来看，tair的命中率在70%，应该说还不错，为什么在我们的压测场景，`DB` 的压力确是如此凶残，甚至导致 `db` 的连接池无法获取呢？

所以性能验证场景就呼之欲出了：

**场景：** 准备 `30` 个产品 `ID`，保持不变，这样最多只会第一次会去访问 `DB`，并将数据存入缓存，后面将会直接命中缓存，`db` 就在后面喝喝茶好了！

![alt](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410171526.jpeg)

于是开始检查这30个产品到底有哪几个没有存入缓存。

通 过开发 `Debug` 预发布环境代码，最终发现，这两个产品竟然已经被用户移到垃圾箱了。而通过和李浩和跃波沟通 `SellerCoponList` 的业务来 看，DA推送过来的产品是存在被用户移除的可能性。因而，每次这两个数据的查询，由于数据库查询不到记录，tair也没有存储相关记录，导致这些查询都将 经过数据库。数据库压力原因也找到了。

**但是问题还没有结束，这似乎只像是冰山表面，我们希望能够鸟瞰整个冰山！**

**细细品味这个问题的最终性能表象** **，** 这是一种变向击穿缓存的做法啊！也就是具备一定的通用性。如果接口始终传入数据库和缓存都不可能存在的数据，那么每次的访问都就落到 `db` 上，导致缓存变相击穿，这个现象很有意思！

目前有一种解决方案，就是 `Null Object Pattern`，将数据库不存在的记录也记录到缓存中，但是 `value` 为 `NULL`，使得缓存可以有效的拦截。由于数据的超时时间是 `10min`，所以如果数据有所改动，也可以接受。

我相信这只是一种方案，可能还会有其他方案，但是这种变向的缓存击穿却让我很兴奋。回过头来，如果让我自己去实现这样的缓存机制，数据库和缓存都不存在的数据场景很容易被忽略，并且这个对于业务确实也不会有影响。在线上存在大量热点数据情况下，这样的机制，往往并不会暴露性能问题。巧合的是，特定的场景， 性能却会出现很大的偏差，这考验的既是性能测试工程师的功力，也考验的是架构的功力！

**Bug** **解决办法：**

其实这过程中不仅仅有一些方法论，也有一些是性能测试经验的功底，更重要的是产出了一些通用性的性能问题解决方案，以及部分参数和技术方案的设计对系统架构的影响。

1. 对于核心的服务中心，去除 `dubbo` 超时重试机制，并重新评估设置超时时间。

2. 对于存在tair或者其他中间件缓存产品，对NULL数据进行缓存，防止出现缓存的变相击穿问题



## 9.Dubbo超时和重连机制

`dubbo` 启动时默认有重试机制和超时机制。

超时机制的规则是如果在一定的时间内，`provider` 没有返回，则认为本次调用失败，重试机制在出现调用失败时，会再次调用。如果在配置的调用次数内都失败，则认为此次请求异常，抛出异常。

如果出现超时，通常是业务处理太慢，可在服务提供方执行：`jstack PID > jstack.log` 分析线程都卡在哪个方法调用上，这里就是慢的原因。

如果不能调优性能，请将 `timeout` 设大。

**某些业务场景下，如果不注意配置超时和重试，可能会引起一些异常。**

### 9.1.超时机制

`Dubbo` 是阿里开源的分布式远程调用方案(`RPC`)，由于网络或服务端不可靠，会导致调用出现一种不确定的中间状态（超时）。为了避免超时导致客户端资源（线程）挂起耗尽，必须设置超时时间。

`Provider` 可以配置的 `Consumer` 端主要属性有 `timeout`、`retries`、`loadbalance`、`actives` 和 `cluster`。`Provider`上应尽量多配置些 `Consumer` 端的属性，让 `Provider` 实现者一开始就思考 `Provider` 的服务特点与服务质量。配置之间存在着覆盖，具体规则如下： 

1. 方法级配置别优于接口级别，即小 `Scope` 优先 
2. `Consumer` 端配置优于 `Provider` 配置，优于全局配置 
3. `Dubbo Hard Code` 的配置值（默认）

根据规则2，纵使消费端配置优于服务端配置，但消费端配置超时时间不能随心所欲，需要根据业务实际情况来设定。如果超时时间设置得太短，复杂业务本来就需要很长时间完成，服务端无法在设定的超时时间内完成业务处理；如果超时时间设置太长，会由于服务端或者网络问题导致客户端资源大量线程挂起。

### 9.2.超时设置

`DUBBO` 消费端设置超时时间需要根据业务实际情况来设定，如果设置的时间太短，一些复杂业务需要很长时间完成，导致在设定的超时时间内无法完成正常的业务处理。

这样消费端达到超时时间，那么 `dubbo` 会进行重试机制，不合理的重试在一些特殊的业务场景下可能会引发很多问题，需要合理设置接口超时时间。

比如发送邮件，可能就会发出多份重复邮件，执行注册请求时，就会插入多条重复的注册数据。

- **合理配置超时和重连的思路**
  - 对于核心的服务中心，去除 `dubbo` 超时重试机制，并重新评估设置超时时间。
  - 业务处理代码必须放在服务端，客户端只做参数验证和服务调用，不涉及业务流程处理

- **Dubbo超时和重连配置示例**

  - `Dubbo` 消费端 

    全局超时配置

    ```
    <dubbo:consumer timeout="5000" />
    ```

    指定接口以及特定方法超时配置

    ```
    <dubbo:reference interface="com.foo.BarService" timeout="2000">
        <dubbo:method name="sayHello" timeout="3000" />
    </dubbo:reference>
    ```

  - `Dubbo` 服务端 

    全局超时配置

    ```
    <dubbo:provider timeout="5000" />
    ```

    指定接口以及特定方法超时配置

    ```
    <dubbo:provider interface="com.foo.BarService" timeout="2000">
        <dubbo:method name="sayHello" timeout="3000" />
    </dubbo:provider>
    ```

    


### 9.3.重连机制

`dubbo` 在调用服务不成功时，默认会重试2次。

`Dubbo` 的路由机制，会把超时的请求路由到其他机器上，而不是本机尝试，所以 `dubbo` 的重试机制也能一定程度的保证服务的质量。

但是如果不合理的配置重试次数，当失败时会进行重试多次，这样在某个时间点出现性能问题，调用方再连续重复调用，系统请求变为正常值的retries倍，系统压力会大增，容易引起服务雪崩，需要根据业务情况规划好如何进行异常处理，何时进行重试。

### 9.4.Dubbo协议超时实现

`Dubbo` 协议超时实现使用了 `Future` 模式，主要涉及类 `DubboInvoker`，`ResponseFuture`, `DefaultFuture`。 

`ResponseFuture.get()` 在请求还未处理完或未到超时前一直是 `wait` 状态；响应达到后，设置请求状态，并进行 `notify` 唤醒。

```
    public Object get() throws RemotingException {
        return get(timeout);
    }

    public Object get(int timeout) throws RemotingException {
        if (timeout <= 0) {
            timeout = Constants.DEFAULT_TIMEOUT;
        }
        if (! isDone()) {
            long start = System.currentTimeMillis();
            lock.lock();
            try {
                while (! isDone()) {
                    done.await(timeout, TimeUnit.MILLISECONDS);
                    if (isDone() || System.currentTimeMillis() - start > timeout) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            if (! isDone()) {
                throw new TimeoutException(sent > 0, channel, getTimeoutMessage(false));
            }
        }
        return returnFromResponse();
    }
```

```
    public static void received(Channel channel, Response response) {
        try {
            DefaultFuture future = FUTURES.remove(response.getId());
            if (future != null) {
                future.doReceived(response);
            } else {
                logger.warn("The timeout response finally returned at " 
                            + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())) 
                            + ", response " + response 
                            + (channel == null ? "" : ", channel: " + channel.getLocalAddress() 
                                + " -> " + channel.getRemoteAddress()));
            }
        } finally {
            CHANNELS.remove(response.getId());
        }
    }

    private void doReceived(Response res) {
        lock.lock();
        try {
            response = res;
            if (done != null) {
                done.signal();
            }
        } finally {
            lock.unlock();
        }
        if (callback != null) {
            invokeCallback(callback);
        }
    }
```



## 10.浅谈dubbo的ExceptionFilter异常处理

### 10.1.背景

我们的项目使用了 `dubbo` 进行不同系统之间的调用。每个项目都有一个全局的异常处理，对于业务异常，我们会抛出自定义的业务异常（继承 `RuntimeException`）。全局的异常处理会根据不同的异常类型进行不同的处理。

最近我们发现，某个系统调用 `dubbo` 请求，`provider` 端（服务提供方）抛出了自定义的业务异常，但 `consumer` 端（服务消费方）拿到的并不是自定义的业务异常。

这是为什么呢？还需要从 `dubbo` 的 `ExceptionFilter` 说起。

### 10.2.ExceptionFilter

如果 `Dubbo` 的 `provider` 端 抛出异常（`Throwable`），则会被 `provider` 端 的 `ExceptionFilter` 拦截到，执行以下 `invoke` 方法：

```
/* 
 * Copyright 1999-2011 Alibaba Group. 
 *   
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *   
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */  
package com.alibaba.dubbo.rpc.filter;  
  
import java.lang.reflect.Method;  
  
import com.alibaba.dubbo.common.Constants;  
import com.alibaba.dubbo.common.extension.Activate;  
import com.alibaba.dubbo.common.logger.Logger;  
import com.alibaba.dubbo.common.logger.LoggerFactory;  
import com.alibaba.dubbo.common.utils.ReflectUtils;  
import com.alibaba.dubbo.common.utils.StringUtils;  
import com.alibaba.dubbo.rpc.Filter;  
import com.alibaba.dubbo.rpc.Invocation;  
import com.alibaba.dubbo.rpc.Invoker;  
import com.alibaba.dubbo.rpc.Result;  
import com.alibaba.dubbo.rpc.RpcContext;  
import com.alibaba.dubbo.rpc.RpcException;  
import com.alibaba.dubbo.rpc.RpcResult;  
import com.alibaba.dubbo.rpc.service.GenericService;  
  
/** 
 * ExceptionInvokerFilter 
 * <p> 
 * 功能： 
 * <ol> 
 * <li>不期望的异常打ERROR日志（Provider端）<br> 
 *     不期望的日志即是，没有的接口上声明的Unchecked异常。 
 * <li>异常不在API包中，则Wrap一层RuntimeException。<br> 
 *     RPC对于第一层异常会直接序列化传输(Cause异常会String化)，避免异常在Client出不能反序列化问题。 
 * </ol> 
 *  
 * @author william.liangf 
 * @author ding.lid 
 */  
@Activate(group = Constants.PROVIDER)  
public class ExceptionFilter implements Filter {  
  
    private final Logger logger;  
      
    public ExceptionFilter() {  
        this(LoggerFactory.getLogger(ExceptionFilter.class));  
    }  
      
    public ExceptionFilter(Logger logger) {  
        this.logger = logger;  
    }  
      
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {  
        try {  
            Result result = invoker.invoke(invocation);  
            if (result.hasException() && GenericService.class != invoker.getInterface()) {  
                try {  
                    Throwable exception = result.getException();  
  
                    // 如果是checked异常，直接抛出  
                    if (! (exception instanceof RuntimeException) && (exception instanceof Exception)) {  
                        return result;  
                    }  
                    // 在方法签名上有声明，直接抛出  
                    try {  
                        Method method = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());  
                        Class<?>[] exceptionClassses = method.getExceptionTypes();  
                        for (Class<?> exceptionClass : exceptionClassses) {  
                            if (exception.getClass().equals(exceptionClass)) {  
                                return result;  
                            }  
                        }  
                    } catch (NoSuchMethodException e) {  
                        return result;  
                    }  
  
                    // 未在方法签名上定义的异常，在服务器端打印ERROR日志  
                    logger.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()  
                            + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()  
                            + ", exception: " + exception.getClass().getName() + ": " + exception.getMessage(), exception);  
  
                    // 异常类和接口类在同一jar包里，直接抛出  
                    String serviceFile = ReflectUtils.getCodeBase(invoker.getInterface());  
                    String exceptionFile = ReflectUtils.getCodeBase(exception.getClass());  
                    if (serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile)){  
                        return result;  
                    }  
                    // 是JDK自带的异常，直接抛出  
                    String className = exception.getClass().getName();  
                    if (className.startsWith("java.") || className.startsWith("javax.")) {  
                        return result;  
                    }  
                    // 是Dubbo本身的异常，直接抛出  
                    if (exception instanceof RpcException) {  
                        return result;  
                    }  
  
                    // 否则，包装成RuntimeException抛给客户端  
                    return new RpcResult(new RuntimeException(StringUtils.toString(exception)));  
                } catch (Throwable e) {  
                    logger.warn("Fail to ExceptionFilter when called by " + RpcContext.getContext().getRemoteHost()  
                            + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()  
                            + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);  
                    return result;  
                }  
            }  
            return result;  
        } catch (RuntimeException e) {  
            logger.error("Got unchecked and undeclared exception which called by " + RpcContext.getContext().getRemoteHost()  
                    + ". service: " + invoker.getInterface().getName() + ", method: " + invocation.getMethodName()  
                    + ", exception: " + e.getClass().getName() + ": " + e.getMessage(), e);  
            throw e;  
        }  
    }  
  
}  
```

#### 10.2.1.代码分析

按逻辑顺序进行分析，满足其中一个即返回，不再继续执行判断。

**逻辑0**

```
if (result.hasException() && GenericService.class != invoker.getInterface()) {  
    //...  
}  
return result; 
```

调用结果有异常且未实现 `GenericService` 接口，进入后续判断逻辑，否则直接返回结果。

```
/** 
 * 通用服务接口 
 *  
 * @author william.liangf 
 * @export 
 */  
public interface GenericService {  
  
    /** 
     * 泛化调用 
     *  
     * @param method 方法名，如：findPerson，如果有重载方法，需带上参数列表，如：findPerson(java.lang.String) 
     * @param parameterTypes 参数类型 
     * @param args 参数列表 
     * @return 返回值 
     * @throws Throwable 方法抛出的异常 
     */  
    Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;  
  
}  
```

泛接口实现方式主要用于服务器端没有 `API` 接口及模型类元的情况，参数及返回值中的所有 `POJO` 均用 `Map` 表示，通常用于框架集成，比如：实现一个通用的远程服务 `Mock` 框架，可通过实现 `GenericService` 接口处理所有服务请求。

不适用于此场景，不在此处探讨。

**逻辑1**

```
// 如果是checked异常，直接抛出  
if (! (exception instanceof RuntimeException) && (exception instanceof Exception)) {  
    return result;  
}  
```

不是 `RuntimeException` 类型的异常，并且是受检异常（继承 `Exception`），直接抛出。

`provider` 端想抛出受检异常，必须在api上明确写明抛出受检异常；consumer端如果要处理受检异常，也必须使用明确写明抛出受检异常的api。

`provider` 端 `api` 新增 自定义的 受检异常， 所有的 `consumer` 端 `api` 都必须升级，同时修改代码，否则无法处理这个特定异常。

`consumer` 端 `DecodeableRpcResult` 的 `decode` 方法会对异常进行处理

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410172623.png)

此处会抛出 `IOException`，上层 `catch` 后会做 `toString` 处理，放到 `mErrorMsg` 属性中：

```
try {  
    decode(channel, inputStream);  
} catch (Throwable e) {  
    if (log.isWarnEnabled()) {  
        log.warn("Decode rpc result failed: " + e.getMessage(), e);  
    }  
    response.setStatus(Response.CLIENT_ERROR);  
    response.setErrorMessage(StringUtils.toString(e));  
} finally {  
    hasDecoded = true;  
} 
```

`DefaultFuture` 判断请求返回的结果，最后抛出 `RemotingException`：

```
private Object returnFromResponse() throws RemotingException {  
    Response res = response;  
    if (res == null) {  
        throw new IllegalStateException("response cannot be null");  
    }  
    if (res.getStatus() == Response.OK) {  
        return res.getResult();  
    }  
    if (res.getStatus() == Response.CLIENT_TIMEOUT || res.getStatus() == Response.SERVER_TIMEOUT) {  
        throw new TimeoutException(res.getStatus() == Response.SERVER_TIMEOUT, channel, res.getErrorMessage());  
    }  
    throw new RemotingException(channel, res.getErrorMessage());  
}
```

`DubboInvoker` 捕获 `RemotingException`，抛出 `RpcException`：

```
try {  
    boolean isAsync = RpcUtils.isAsync(getUrl(), invocation);  
    boolean isOneway = RpcUtils.isOneway(getUrl(), invocation);  
    int timeout = getUrl().getMethodParameter(methodName, Constants.TIMEOUT_KEY,Constants.DEFAULT_TIMEOUT);  
    if (isOneway) {  
        boolean isSent = getUrl().getMethodParameter(methodName, Constants.SENT_KEY, false);  
        currentClient.send(inv, isSent);  
        RpcContext.getContext().setFuture(null);  
        return new RpcResult();  
    } else if (isAsync) {  
        ResponseFuture future = currentClient.request(inv, timeout) ;  
        RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));  
        return new RpcResult();  
    } else {  
        RpcContext.getContext().setFuture(null);  
        return (Result) currentClient.request(inv, timeout).get();  
    }  
} catch (TimeoutException e) {  
    throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "Invoke remote method timeout. method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);  
} catch (RemotingException e) {  
    throw new RpcException(RpcException.NETWORK_EXCEPTION, "Failed to invoke remote method: " + invocation.getMethodName() + ", provider: " + getUrl() + ", cause: " + e.getMessage(), e);  
}  
```

调用栈：

`FailOverClusterInvoker.doInvoke -...-> DubboInvoker.doInvoke -> ReferenceCountExchangeClient.request -> HeaderExchangeClient.request -> HeaderExchangeChannel.request -> AbstractPeer.send -> NettyChannel.send -> AbstractChannel.write -> Channels.write --back_to--> DubboInvoker.doInvoke -> DefaultFuture.get -> DefaultFuture.returnFromResponse -> throw new RemotingException`

异常示例：

```
com.alibaba.dubbo.rpc.RpcException: Failed to invoke the method triggerCheckedException in the service com.xxx.api.DemoService. Tried 1 times of the providers [192.168.1.101:20880] (1/1) from the registry 127.0.0.1:2181 on the consumer 192.168.1.101 using the dubbo version 3.1.9. Last error is: Failed to invoke remote method: triggerCheckedException, provider: dubbo://192.168.1.101:20880/com.xxx.api.DemoService?xxx, cause: java.io.IOException: Response data error, expect Throwable, but get {cause=(this Map), detailMessage=null, suppressedExceptions=[], stackTrace=[Ljava.lang.StackTraceElement;@23b84919}  
java.io.IOException: Response data error, expect Throwable, but get {cause=(this Map), detailMessage=null, suppressedExceptions=[], stackTrace=[Ljava.lang.StackTraceElement;@23b84919}  
    at com.alibaba.dubbo.rpc.protocol.dubbo.DecodeableRpcResult.decode(DecodeableRpcResult.java:94)  
```

**逻辑2**

```
// 在方法签名上有声明，直接抛出  
try {  
    Method method = invoker.getInterface().getMethod(invocation.getMethodName(), invocation.getParameterTypes());  
    Class<?>[] exceptionClassses = method.getExceptionTypes();  
    for (Class<?> exceptionClass : exceptionClassses) {  
        if (exception.getClass().equals(exceptionClass)) {  
            return result;  
        }  
    }  
} catch (NoSuchMethodException e) {  
    return result;  
}  
```

如果在 `provider` 端的 `api` 明确写明抛出运行时异常，则会直接被抛出。

如果抛出了这种异常，但是 `consumer` 端又没有这种异常，会发生什么呢？

答案是和上面一样，抛出 `RpcException`。

因此如果 `consumer` 端不 `care` 这种异常，则不需要任何处理；

`consumer` 端有这种异常（路径要完全一致，包名+类名），则不需要任何处理；

没有这种异常，又想进行处理，则需要引入这个异常进行处理（方法有多种，比如升级api，或引入/升级异常所在的包）。

**逻辑3**

```
// 异常类和接口类在同一jar包里，直接抛出  
String serviceFile = ReflectUtils.getCodeBase(invoker.getInterface());  
String exceptionFile = ReflectUtils.getCodeBase(exception.getClass());  
if (serviceFile == null || exceptionFile == null || serviceFile.equals(exceptionFile)){  
    return result;  
}  
```

如果异常类和接口类在同一个jar包中，直接抛出。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410172926.png)

**逻辑4**

```
// 是JDK自带的异常，直接抛出  
String className = exception.getClass().getName();  
if (className.startsWith("java.") || className.startsWith("javax.")) {  
    return result;  
}  
```

以 `java.`或 `javax.`开头的异常直接抛出。

**逻辑5**

```
// 是Dubbo本身的异常，直接抛出  
if (exception instanceof RpcException) {  
    return result;  
}  
```

`dubbo` 自身的异常，直接抛出。

**逻辑6**

```
// 否则，包装成RuntimeException抛给客户端  
return new RpcResult(new RuntimeException(StringUtils.toString(exception)));  
```

不满足上述条件，会做 `toString` 处理并被封装成 `RuntimeException` 抛出。

#### 10.2.2.核心思想

尽力避免反序列化时失败（只有在 `jdk` 版本或 `api` 版本不一致时才可能发生）。

### 10.3.如何正确捕获业务异常

了解了 `ExceptionFilter`，解决上面提到的问题就很简单了。

有多种方法可以解决这个问题，每种都有优缺点，这里不做详细分析，仅列出供参考：

1. 将该异常的包名以 `java.` 或者 `javax. ` 开头

2. 使用受检异常（继承 `Exception`）

3. 不用异常，使用错误码

4. 把异常放到 `provider-api` 的 `jar` 包中

5. 判断异常 `message` 是否以 `XxxException.class.getName()` 开头（其中 `XxxException` 是自定义的业务异常）

6. `provider` 实现 `GenericService` 接口

7. `provider` 的 `api` 明确写明 `throws XxxException`，发布 `provider`（其中 `XxxException` 是自定义的业务异常）

8. 实现 `dubbo` 的 `filter`，自定义 `provider` 的异常处理逻辑

 

#### 10.3.1.给dubbo接口添加白名单

`dubbo Filter` 的使用具体内容如下：

在开发中，有时候需要限制访问的权限，白名单就是一种方法。对于 `Java Web` 应用，`Spring` 的拦截器可以拦截 `Web` 接口的调用；而对于`dubbo` 接口，`Spring` 的拦截器就不管用了。

`dubbo` 提供了 `Filter` 扩展，可以通过自定义 `Filter` 来实现这个功能。本文通过一个事例来演示如何实现 `dubbo` 接口的 `IP` 白名单。

**扩展Filter**

实现 `com.alibaba.dubbo.rpc.Filter` 接口：

```
public class AuthorityFilter implements Filter {  
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityFilter.class);  
  
    private IpWhiteList ipWhiteList;  
  
    //dubbo通过setter方式自动注入  
    public void setIpWhiteList(IpWhiteList ipWhiteList) {  
        this.ipWhiteList = ipWhiteList;  
    }  
  
    @Override  
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {  
        if (!ipWhiteList.isEnabled()) {  
            LOGGER.debug("白名单禁用");  
            return invoker.invoke(invocation);  
        }  
  
        String clientIp = RpcContext.getContext().getRemoteHost();  
        LOGGER.debug("访问ip为{}", clientIp);  
        List<String> allowedIps = ipWhiteList.getAllowedIps();  
        if (allowedIps.contains(clientIp)) {  
            return invoker.invoke(invocation);  
        } else {  
            return new RpcResult();  
        }  
    }  
}  
```

注意：只能通过 `setter` 方式来注入其他的 `bean`，且不要标注注解！

`dubbo` 自己会对这些 `bean` 进行注入，不需要再标注 `@Resource` 让 `Spring`注入

**配置文件**

在 `resources` 目录下添加纯文本文件 `META-INF/dubbo/com.alibaba.dubbo.rpc.Filter`，内容如下：

```
xxxFilter=com.xxx.AuthorityFilter  
```

修改 `dubbo` 的 `provider` 配置文件，在 `dubbo:provider` 中添加配置的 `filter`，如下：

```
<dubbo:provider filter="xxxFilter" />  
```

这样就可以实现 `dubbo` 接口的 `IP` 白名单功能了。















