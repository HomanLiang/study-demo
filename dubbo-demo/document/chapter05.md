[toc]



# Dubbo 服务引用

## 1.服务引用大致流程

我们已经得知 `Provider`将自己的服务暴露出来，注册到注册中心，而 `Consumer`无非就是通过一波操作从注册中心得知 Provider 的信息，然后自己封装一个调用类和 Provider 进行深入地交流。

而之前的文章我都已经提到在 `Dubbo`中一个可执行体就是 `Invoker`，所有调用都要向 Invoker 靠拢，因此可以推断出应该要先生成一个 Invoker，然后又因为框架需要往不侵入业务代码的方向发展，那我们的 Consumer 需要无感知的调用远程接口，因此需要搞个代理类，包装一下屏蔽底层的细节。

整体大致流程如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233609.png)

## 2.服务引入的时机

服务的引入和服务的暴露一样，也是通过 spring 自定义标签机制解析生成对应的 Bean，**Provider Service 对应解析的是 ServiceBean 而 Consumer Reference 对应的是 ReferenceBean**。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233634.png)

前面服务暴露的时机我们上篇文章分析过了，在 Spring 容器刷新完成之后开始暴露，而服务的引入时机有两种，第一种是饿汉式，第二种是懒汉式。

饿汉式是通过实现 Spring 的`InitializingBean`接口中的 `afterPropertiesSet`方法，容器通过调用 `ReferenceBean`的 `afterPropertiesSet`方法时引入服务。

懒汉式是只有当这个服务被注入到其他类中时启动引入流程，也就是说用到了才会开始服务引入。

**默认情况下，Dubbo 使用懒汉式引入服务**，如果需要使用饿汉式，可通过配置 `dubbo:reference` 的 init 属性开启。

我们可以看到 `ReferenceBean`还实现了`FactoryBean`接口，这里有个关于 Spring 的面试点我带大家分析一波。

## 3.BeanFactory 、FactoryBean、ObjectFactory

就是这三个玩意，我单独拿出来说一下，从字面上来看其实可以得知`BeanFactory`、`ObjectFactory`是个工厂而`FactoryBean`是个 Bean。

`BeanFactory` 其实就是 IOC 容器，有多种实现类我就不分析了，简单的说就是 Spring 里面的 Bean 都归它管，而`FactoryBean`也是 Bean 所以说也是归 BeanFactory 管理的。

那 `FactoryBean` 到底是个什么 Bean 呢？它其实就是把你真实想要的 Bean 封装了一层，在真正要获取这个 Bean 的时候容器会调用 FactoryBean#getObject() 方法，而在这个方法里面你可以进行一些复杂的组装操作。

这个方法就封装了真实想要的对象**复杂的创建过程**。

到这里其实就很清楚了，就是在真实想要的 Bean 创建比较复杂的情况下，或者是一些第三方 Bean 难以修改的情形，使用 FactoryBean 封装了一层，屏蔽了底层创建的细节，便于 Bean 的使用。

而 ObjectFactory 这个是用于延迟查找的场景，它就是一个普通工厂，当得到 ObjectFactory 对象时，相当于 Bean 没有被创建，只有当 getObject() 方法时，才会触发 Bean 实例化等生命周期。

主要用于暂时性地获取某个 Bean Holder 对象，如果过早的加载，可能会引起一些意外的情况，比如当 Bean A 依赖 Bean B 时，如果过早地初始化 A，那么 B 里面的状态可能是中间状态，这时候使用 A 容易导致一些错误。

总结的说 **BeanFactory 就是 IOC 容器，FactoryBean 是特殊的 Bean, 用来封装创建比较复杂的对象，而 ObjectFactory 主要用于延迟查找的场景，延迟实例化对象**。

## 4.服务引入的三种方式

服务的引入又分为了三种，第一种是本地引入、第二种是直接连接引入远程服务、第三种是通过注册中心引入远程服务。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233731.png)

- **本地引入**不知道大家是否还有印象，之前服务暴露的流程每个服务都会通过搞一个本地暴露，走 injvm 协议（当然你要是 scope = remote 就没本地引用了），因为**存在一个服务端既是 Provider 又是 Consumer 的情况，然后有可能自己会调用自己的服务**，因此就弄了一个本地引入，这样就避免了远程网络调用的开销。

  所以**服务引入会先去本地缓存找找看有没有本地服务**。

- **直连远程引入服务**，这个其实就是平日测试的情况下用用，不需要启动注册中心，由 Consumer 直接配置写死 Provider 的地址，然后直连即可。

- **注册中心引入远程服务**，这个就是重点了，Consumer 通过注册中心得知 Provider 的相关信息，然后进行服务的引入，这里还包括多注册中心，同一个服务多个提供者的情况，如何抉择如何封装，如何进行负载均衡、容错并且让使用者无感知，这就是个技术活。

**本文用的就是单注册中心引入远程服务**，让我们来看看 Dubbo 是如何做的吧。

## 5.服务引入流程解析

默认是懒汉式的，所以服务引入的入口就是 ReferenceBean 的 getObject 方法。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233801.png)

可以看到很简单，就是调用 get 方法，如果当前还没有这个引用那么就执行 init 方法。

## 6.源码分析

init 方法很长，不过大部分就是检查配置然后将配置构建成 map ，这一大段我就不分析了，我们直接看一下构建完的 map 长什么样。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233923.png)

然后就进入重点方法 createProxy，从名字可以得到就是要创建的一个代理，因为代码很长，我就**一段一段的分析**。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233934.png)

如果是走本地的话，那么直接构建个走本地协议的 URL 然后进行服务的引入，即 refprotocol.refer，这个方法之后会做分析，本地的引入就不深入了，就是去之前服务暴露的 exporterMap 拿到服务。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233946.png)

如果不是本地，那肯定是远程了，接下来就是判断是点对点直连 provider 还是通过注册中心拿到 provider 信息再连接 provider 了，我们分析一下配置了 url 的情况，如果配置了 url 那么不是直连的地址，就是注册中心的地址。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408233957.png)

然后就是没配置 url 的情况，到这里肯定走的就是注册中心引入远程服务了。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234007.png)

最终拼接出来的 URL 长这样。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234018.png)

可以看到这一部分其实就是根据各种参数来组装 URL ，因为我们的自适应扩展都需要根据 URL 的参数来进行的。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234027.png)

至此我先画个图，给大家先捋一下。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234043.png)

这其实就是整个流程了，简述一下就是先检查配置，通过配置构建一个 map ，然后利用 map 来构建 URL ，再通过 URL 上的协议利用自适应扩展机制调用对应的 protocol.refer 得到相应的 invoker 。

在有多个 URL 的时候，先遍历构建出 invoker 然后再由 StaticDirectory 封装一下，然后通过 cluster 进行合并，只暴露出一个 invoker 。

然后再构建代理，封装 invoker 返回服务引用，之后 Comsumer 调用的就是这个代理类。

相信通过图和上面总结性的简述已经知道大致的服务引入流程了，不过还是有很多细节，比如如何从注册中心得到 Provider 的地址，invoker 里面到底是怎么样的？别急，我们继续看。

从前面的截图我们可以看到此时的协议是 registry 因此走的是 RegistryProtocol#refer，我们来看一下这个方法。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234054.png)

主要就是获取注册中心实例，然后调用 doRefer 进行真正的 refer。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234107.png)

这个方法很关键，可以看到生成了`RegistryDirectory` 这个 directory 塞了注册中心实例，它自身也实现了`NotifyListener` 接口，因此**注册中心的监听其实是靠这家伙来处理的**。

然后向注册中心注册自身的信息，并且向注册中心订阅了 providers 节点、 configurators 节点 和 routers 节点，**订阅了之后 RegistryDirectory 会收到这几个节点下的信息，就会触发 DubboInvoker 的生成了，即用于远程调用的 Invoker**。

然后通过 cluster 再包装一下得到 Invoker，因此一个服务可能有多个提供者，最终在 ProviderConsumerRegTable 中记录这些信息，然后返回 Invoker。

所以我们知道`Conusmer` 是在 RegistryProtocol#refer 中向注册中心注册自己的信息，并且订阅 Provider 和配置的一些相关信息，我们看看订阅返回的信息是怎样的。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234123.png)

拿到了`Provider`的信息之后就可以通过监听触发 DubboProtocol# refer 了（具体调用哪个 protocol 还是得看 URL的协议的，我们这里是 dubbo 协议），整个触发流程我就不一一跟一下了，看下调用栈就清楚了。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234133.png)

终于我们从注册中心拿到远程`Provider` 的信息了，然后进行服务的引入。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234143.png)

这里的重点在 `getClients`，因为终究是要跟远程服务进行网络调用的，而 getClients 就是用于获取客户端实例，实例类型为 ExchangeClient，底层依赖 Netty 来进行网络通信，并且可以看到默认是共享连接。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234157.png)

`getSharedClient` 我就不分析了，就是通过远程地址找 client ，这个 client 还有引用计数的功能，如果该远程地址还没有 client 则调用 initClient，我们就来看一下 initClient 方法。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234208.png)

而这个`connect`最终返回 `HeaderExchangeClient`里面封装的是 `NettyClient` 。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234221.png)

然后最终得到的 `Invoker`就是这个样子，可以看到记录的很多信息，基本上该有的都有了，我这里走的是对应的服务只有一个 url 的情况，多个 url 无非也是利用 `directory` 和 `cluster`再封装一层。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234232.png)

最终将调用 `return (T) proxyFactory.getProxy(invoker);` 返回一个代理对象，这个就不做分析了。

到这里，整个流程就是分析完了，不知道大家清晰了没？我再补充前面的图，来一个完整的流程给大家再过一遍。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408234248.png)

## 7.小结

相信分析下来整个流程不难的，总结地说无非就是通过配置组成 URL ，然后通过自适应得到对于的实现类进行服务引入，如果是注册中心那么会向注册中心注册自己的信息，然后订阅注册中心相关信息，得到远程 `provider`的 ip 等信息，再通过`netty`客户端进行连接。

并且通过`directory` 和 `cluster` 进行底层多个服务提供者的屏蔽、容错和负载均衡等，这个之后文章会详细分析，最终得到封装好的 `invoker`再通过动态代理封装得到代理类，让接口调用者无感知的调用方法。