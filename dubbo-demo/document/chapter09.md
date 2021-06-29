[toc]



# Dubbo 容错机制

## 1.相关概念

### 1.1.前置动作

集群容错真正发生在消费端。当消费端发起调用时，会先从**服务目录**查询满足需求的服务提供者信息，在此基础上进行**路由**，路由后的结果才会真正进行容错处理。所以，就会有如下的活动图：

![éç¾¤å®¹éåç½®å¨ä½](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410115618.png)

### 1.2.invoker 是什么？

在 `Dubbo` 中 `invoker` 其实就是一个具有调用功能的对象，在服务暴露端封装的就是真实的服务实现，把真实的服务实现封装一下变成一个 `invoker`。

在服务引入端就是从注册中心得到服务提供者的配置信息，然后一条配置信息对应封装成一个 `invoker`，这个 `invoker` 就具备远程调用能力，当然要是走的是 `injvm` 协议那真实走的还是本地的调用。

然后还有个 `ClusterInvoker`，它也是个 `invoker`，它封装了服务引入生成的 `invoker` 们，赋予其集群容错等能力，这个 `invoker` 就是暴露给消费者调用的 `invoker`。

所以说 `Dubbo` 就是搞了个统一模型，将能**调用的服务的对象都封装成 invoker**。

我们今天主要讲的是服务消费者这边的事情，因为**集群容错是消费者端实现的**。

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114252.webp)

### 1.3.服务目录到底是什么？

服务目录到底是个什么东西呢，看名字好像就是服务的目录，通过这个目录来查找远程服务？

对了一半！可以通过服务目录来查找远程服务，但是它不是"目录"，实际上它是一堆 `invoker` 的集合，

前面说到服务的提供者都会集群部署，所有同样的服务会有多个提供者，因此就搞个服务目录来聚集它们，到时候要选择的时候就去服务目录里挑。

而服务提供者们也不是一成不变的，比如集群中加了一台服务提供者，那么相应的服务目录就需要添加一个 `invoker`，下线了一台服务提供者，目录里面也需要删除对应的 `invoker`，修改了配置也一样得更新。

所以这个服务目录其实还实现了监听注册中心的功能（指的是 `RegistryDirectory`）。

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114313.webp)

这个 `Node` 就不管了，主要是看 `Directory`，正常操作搞一个抽象类来实现 `Directory` 接口，抽象类会实现一些公共方法，并且定义好逻辑，然后具体的实现由子类来完成，可以看到有两个子类，分别是 `StaticDirectory` 和 `RegistryDirectory`。

### 1.4.RegistryDirectory

我们先来看下 `RegistryDirectory`，它是一个动态目录，我们来看一下具体的结构。

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114324.png)

从截图可以看到 `RegistryDirectory` 内部存储了 `DemoService` 的两个服务提供者 `url` 和对应的 `invoker`。

而且从上面的继承结构也可以看出，它实现了 `NotifyListener` 接口，所以它可以监听注册中心的变化，当服务中心的配置发生变化之后，`RegistryDirectory` 就可以收到变更通知，然后根据配置刷新其 `Invoker` 列表。

所以说 `RegistryDirectory` 一共有三大作用：

1. 获取 `invoker` 列表
2. 监听注册中心的变化
3. 刷新 `invokers`。

**1.获取 invoker 列表**，`RegistryDirectory` 实现的父类抽象方法 `doList`，其目的就是得到 `invoker` 列表，而其内部的实现主要是做了层方法名的过滤，通过方法名找到对应的 `invokers`。

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114413.webp)

**2.监听注册中心的变化**，通过实现 `NotifyListener` 接口能感知到注册中心的数据变更，这其实是在服务引入的时候就订阅的。

```
    public void subscribe(URL url) {
        setConsumerUrl(url);
        registry.subscribe(url, this); //订阅
    }
```

`RegistryDirectory` 定义了三种集合，分别是 `invokerUrls`、`routerUrls`、`configuratorUrls` 分别处理相应的配置变化，然后对应转化成对象。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114123.webp)

**3.刷新 Invoker 列表**，其实就是根据监听变更的 `invokerUrls` 做一波操作，`refreshInvoker(invokerUrls)`, 根据配置更新 invokers。

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114541.webp)

简单的说就是先根据 `invokerUrls` 数量和协议头是否是 `empty`，来决定是否禁用所有 `invokers`，如果不禁用，则将 `url` 转成 `Invoker`，得到 `<url, Invoker>` 的映射关系。

然后再进行转换，得到 `<方法名, Invoker 列表>` 映射关系，再将同一个组的 `Invoker` 进行合并，并将合并结果赋值给 `methodInvokerMap`，这个 `methodInvokerMap` 就是上面 `doList` 中使用的那个 `Map`。

所以是在 `refreshInvoker` 的时候构造 `methodInvokerMap`，然后在调用的时候再读 `methodInvokerMap`，最后再销毁无用的 `invoker`。

### 1.5.StaticDirectory

`StaticDirectory`，这个是**用在多注册中心的时候，它是一个静态目录，即固定的不会增减的**，所有 `Invoker` 是通过构造器来传入。

可以简单的理解成在**单注册中心**下我们配置的一条 `reference` 可能对应有多个 `provider`，然后生成多个 `invoker`，我们将它们存入 `RegistryDirectory` 中进行管理，为了便于调用再对外只暴露出一个 `invoker` 来封装内部的多 `invoker` 情况。

那多个注册中心就会有多个已经封装好了的 `invoker`，这又面临了选择了，于是我们用 `StaticDirectory` 再来存入这些 `invoker` 进行管理，也再封装起来对外只暴露出一个 `invoker` 便于调用。

之所以是静态的是因为多注册中心是写在配置里面的，不像服务可以动态变更。

`StaticDirectory` 的内部逻辑非常的简单，就是一个 `list` 存储了这些 `invokers`，然后实现父类的方法也就单纯的返回这个 `list` 不做任何操作。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114557.webp)

### 1.6.什么是服务路由？

服务路由其实就是路由规则，它规定了服务消费者可以调用哪些服务提供者，`Dubbo` 一共有三种路由分别是：条件路由 `ConditionRouter`、脚本路由 `ScriptRouter` 和标签路由 `TagRouter`。

最常用的就是条件路由，我们就分析下条件路由。

条件路由是两个条件组成的，是这么个格式 `[服务消费者匹配条件] => [服务提供者匹配条件]`，举个例子官网的例子就是 `host = 10.20.153.10 => host = 10.20.153.11`。

该条规则表示 `IP` 为 `10.20.153.10` 的服务消费者只可调用 `IP` 为 `10.20.153.11` 机器上的服务，不可调用其他机器上的服务。

这就叫路由了。

路由的配置一样是通过 `RegistryDirectory` 的 `notify` 更新和构造的，然后路由的调用在是刷新 `invoker` 的时候，具体是在调用 `toMethodInvokers` 的时候会进行服务级别的路由和方法级别的路由。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114623.png)

具体的路由匹配和表达式解析就不深入了，有兴趣的同学自行了解，其实知道这个功能是干嘛的差不多了，反正经过路由的过滤之后消费者拿到的都是能调用的远程服务。

### 1.7.Dubbo 的 Cluster 有什么用？

前面我们已经说了有服务目录，并且目录还经过了路由规则的过滤，此时我们手上还是有一堆 `invokers`，那对于消费者来说就需要进行抉择，那到底选哪个 `invoker` 进行调用呢？

假设选择的那个 `invoker` 调用出错了怎么办？前面我们已经提到了，这时候就是 `cluster` 登场的时候了，它会把这一堆 `invoker` 封装成 `clusterInovker`，给到消费者调用的就只有一个 `invoker` 了，

然后在这个 `clusterInovker` 内部还能做各种操作，比如选择一个 `invoker`，调用出错了可以换一个等等。

这些细节都被封装了，消费者感受不到这个复杂度，所以 `cluster` 就是一个中间层，为消费者屏蔽了服务提供者的情况，简化了消费者的使用。

并且也更加方便的替换各种集群容错措施。

`Dubbo` 默认的 `cluster` 实现有很多，主要有以下几种：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114631.webp)

每个 `Cluster` 内部其实返回的都是 `XXXClusterInvoker`，我就举一下 `FailoverCluster` 这个例子。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410114638.png)

就下来我们就每个 `Cluster` 过一遍。



## 2.集群容错策略

### 2.1.FailoverClusterInvoker

这个 `cluster` 实现的是失败自动切换功能，简单的说一个远程调用失败，它就立马换另一个，当然是有重试次数的。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122142.webp)

可以看到 `doInvoke` 方法首先是获取重试次数，然后根据重试次数进行循环调用，会 `catch` 住异常，然后失败后进行重试。

每次循环会通过负载均衡选择一个 `Invoker`，然后通过这个 `Invoker` 进行远程调用，如果失败了会记录下异常，并进行重试。

这个 **select 实际上还进行了粘性处理**，也就是会记录上一次选择的 `invoker`，这样使得每次调用不会一直换 `invoker`，如果上一次没有 `invoker`，或者上一次的 `invoker` 下线了则会进行负载均衡选择。

### 2.2.FailfastClusterInvoker

这个 `cluster` 只会进行一次远程调用，如果失败后立即抛出异常，也就是快速失败，它适合于不支持幂等的一些调用。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122205.webp)

从代码可以看到，很简单还是通过负载均衡选择一个 `invoker`，然后发起调用，如果失败了就抛错。

![Dubboçå¿«éå¤±è´¥å®¹éæºå¶](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122407.png)

### 2.3.FailsafeClusterInvoker

这个 `cluster` 是一种失败安全的 `cluster`，也就是调用出错仅仅就日志记录一下，然后返回了一个空结果，适用于写入审计日志等操作。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122219.webp)

可以看到代码很简单，抛错就日志记录，返回空结果。

![Dubboçå¤±è´¥å®å¨å®¹éæºå¶](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122419.png)

### 2.4.FailbackClusterInvoker

这个 `cluster` 会在调用失败后，记录下来这次调用，然后返回一个空结果给服务消费者，并且会通过定时任务对失败的调用进行重调。

适合执行消息通知等最大努力场景。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122225.webp)

看起来好像代码很多，其实逻辑很简单。

当调用出错的时候就返回空结果，并且加入到 `failed` 中，并且会有一个定时任务会定时的去调用 `failed` 里面的调用，如果调用成功就从 `failed` 中移除这个调用。

![Dubboçå¤±è´¥èªå¨æ¢å¤å®¹éæºå¶](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122352.png)

### 2.5.ForkingClusterInvoker

这个 `cluster` 会在运行时把所有 `invoker` 都通过线程池进行并发调用，只要有一个服务提供者成功返回了结果，`doInvoke` 方法就会立即结束运行。

适合用在对实时性要求比较高读操作。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122236.webp)

![Dubboçå¹¶è¡è°ç¨å¤ä¸ªæå¡æä¾èå®¹éæºå¶](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122440.png)

### 2.6.BroadcastClusterInvoker

这个 `cluster` 会在运行时把所有 `invoker` 逐个调用，然后在最后判断如果有一个调用抛错的话，就抛出异常。

适合通知所有提供者更新缓存或日志等本地资源信息的场景。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122245.png)

![Dubboçå¹¿æ­æ¹å¼å®¹éæºå¶](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122451.png)

### 2.7.AbstractClusterInvoker

这其实是它们的父类，不过 `AvailableCluster` 内部就是返回 `AbstractClusterInvoker`，这个主要用在多注册中心的时候，比较简单，就是哪个能用就用那个。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410122253.png)

### 2.8.小结 Cluster

可以看到上面有很多种集群的实现，分别适用不同的场景，这其实就是很好的抽象，加了这个中间层向服务消费者屏蔽了集群调用的细节，并且可以在不同场景选择更合适的实现。

当然还能自定义实现，自己加以扩展来定制合适自己业务的链路调用方案。



## 3.串联容错机制和负载均衡

先来看下官网的这一张图，很是清晰，然后我再用语言来阐述一遍。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410123215.webp)

首先在服务引入的时候，将多个远程调用都塞入 `Directory` 中，然后通过 `Cluster` 来封装这个目录，封装的同时提供各种容错功能，比如 `FailOver`、`FailFast` 等等，最终暴露给消费者的就是一个 `invoker`。

然后消费者调用的时候会目录里面得到 `invoker` 列表，当然会经过路由的过滤，得到这些 `invokers` 之后再由 `loadBalance` 来进行负载均衡选择一个 `invoker`，最终发起调用。

这种过程其实是在 `Cluster` 的内部发起的，所以能在发起调用出错的情况下，用上容错的各种措施。













