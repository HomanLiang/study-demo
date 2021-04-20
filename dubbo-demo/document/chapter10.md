[toc]



# Dubbo 熔断、限流和降级

当我们遇到高并发下的流量激增问题时，常常提到`降级`、`熔断`和`限流`的概念。我先简单讲解一下这三个概念的定义。

## 1.相关概念 

### 1.1.降级

降级也就是服务降级，当我们的服务器压力剧增时，为了保证核心功能的可用性，而选择性的降低一些功能的可用性，或者直接关闭该功能。例如在双十一凌晨抢购的时候，流量压力是很大的，为了保证订单的正常支付，在凌晨1-2点左右的订单修改功能是关闭的。  

### 1.2.熔断  

降级一般是指我们自身的系统出现了故障而降级。而熔断一般是指依赖的外部接口出现故障的情况断绝和外部接口的关系。  

例如你的A服务里的一个功能依赖B服务，这时B服务出现问题了，返回的很慢。而越是庞大的系统，上下游的调用链就会越长，而如果在一个很长的调用链中，某一个服务由于某种原因导致响应时间过长，或者完全无响应，那么就可能把整个分布式系统都拖垮。这种情况下可能会因为这个功能而拖慢A服务里面的所有功能，严重的时候会导致**服务雪崩**现象。  

在分布式系统中，为了保证整体服务的可用性和一致性，很多系统会采用**重试机制**，在一些因为网络抖动原因发生失败的可以采用这种措施。  

但是有些情况，并不适合用**重试机制**，反而进一步损害了系统的性能。比如说下游系统因为请求量太大，导致CPU已经被打满，或者数据库连接池被占满，这时候上游系统调用下游系统获取不导信息就会不断重试，这种情况下的重试很造成下游系统的崩溃。  

在分布式系统中，大多数的服务雪崩也是因为不断重试导致的，这种重试有可能是框架级别的自动重试、有可能是代码级别的重试逻辑、还有可能是用户的主动重试。  

那我们可以利用**熔断器模式**来解决这一现象。一个典型的熔断器有三种状态：  

- **关闭** 

  熔断器默认情况是关闭的。熔断器本身带有计数功能，每当错误发生一次，计数器就会进行累加，到了一定的次数熔断器就会开启，同时开启一个计时器，一旦时间到了就会切换成`半开启`的状态。  

- **开启**  

  在开启的状态下任何请求都会直接被拒绝并且抛出异常信息。   

- **半开启**

  熔断器会允许部分的请求，如果这些请求都成功通过，那么就说明错误已经不存在了，则会被切换到关闭状态并重置计数。倘若请求中有任何一个发生失败，就会恢复到`开启`状态，并且重置计时，给予系统一段休息时间。  

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410141232.webp)

### 1.3.限流

这个很容易理解，就是通过对系统的评估，只允许一定数量的请求进入，剩余的请求拒绝执行。 比如在秒杀系统中，要秒杀100份商品，我只允许前2000个请求进入，剩余的直接拦截拒接。  



## 2.Dubbo服务降级 

Dubbo可以通过服务降级功能临时屏蔽某个出错的非关键性服务，并定义降级后的返回策略。 我们可以向注册中心写入动态配置覆盖规则：  

```
RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
Registry registry = registryFactory.getRegistry(URL.valueOf("zookeeper://10.20.153.10:2181"));
registry.register(URL.valueOf("override://0.0.0.0/com.foo.BarService?category=configurators&dynamic=false&application=foo&mock=force:return+null"));
```

也可以通过配置文件进行定义。  

```
<dubbo:reference id="iUser" interface="com.dubbosample.iface.IUser"  timeout="1000" check="false" mock="return null">
```

也有这几种形式：  

```
<dubbo:reference mock="true"  .../>
<dubbo:reference mock="com.xxxx" .../>
<dubbo:reference mock="return null" .../>
<dubbo:reference mock="throw xxx" .../>
<dubbo:reference mock="force:return .." .../>
<dubbo:reference mock="force:throw ..." .../>
```

其中，最主要的两种形式是： 

- `mock='force:return+null'`表示消费对该服务的方法调用都直接返回null值，不发起远程调用。用来屏蔽不重要服务不可用时对调用方的影响。
-  还可以改为`mock=fail:return+null`表示消费方对该服务的方法调用在失败后，再返回null。用来容忍不重要服务不稳定时对调用方的影响。

### 2.1.具体代码 

阅读过源码的知道Dubbo的远程调用是从一个代理Proxy开始的，首先将运行时参数存储在数组中，然后调用`InvocationHandler`接口来实现类的`invoke`方法。下面是一个动态生成的一个代理类例子。 

```
public class proxy0 implements ClassGenerator.DC, EchoService, DemoService {
    // 方法数组
    public static Method[] methods;
    private InvocationHandler handler;

    public proxy0(InvocationHandler invocationHandler) {
        this.handler = invocationHandler;
    }

    public proxy0() {
    }

    public String sayHello(String string) {
        // 将参数存储到 Object 数组中
        Object[] arrobject = new Object[]{string};
        // 调用 InvocationHandler 实现类的 invoke 方法得到调用结果
        Object object = this.handler.invoke(this, methods[0], arrobject);
        // 返回调用结果
        return (String)object;
    }
    public Object $echo(Object object) {
        Object[] arrobject = new Object[]{object};
        Object object2 = this.handler.invoke(this, methods[1], arrobject);
        return object2;
    }
}
```

`InvokerInvocationHandler`中的`invoker`成员变量为`MockClusterInvoker`，它来处理服务降级的逻辑。

```
public class InvokerInvocationHandler implements InvocationHandler {

    private final Invoker<?> invoker;

    public InvokerInvocationHandler(Invoker<?> handler) {
        this.invoker = handler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        // 拦截定义在 Object 类中的方法（未被子类重写），比如 wait/notify
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(invoker, args);
        }

        // 如果 toString、hashCode 和 equals 等方法被子类重写了，这里也直接调用
        if ("toString".equals(methodName) && parameterTypes.length == 0) {
            return invoker.toString();
        }
        if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
            return invoker.hashCode();
        }
        if ("equals".equals(methodName) && parameterTypes.length == 1) {
            return invoker.equals(args[0]);
        }

        // 将 method 和 args 封装到 RpcInvocation 中，并执行后续的调用
        return invoker.invoke(new RpcInvocation(method, args)).recreate();
    }
}
```

在`MockClusterInvoker`中，从`no mock(正常情况)`,`force:direct mock(屏蔽)`,`fail-mock(容错)`三种情况我们也可以看出,普通情况是直接调用,容错的情况是调用失败后,返回一个设置的值.而屏蔽就很暴力了,直接连调用都不调用,就直接返回一个之前设置的值.
从下面的注释中可以看出，如果没有降级，会执行`this.invoker.invoke(invocation)`方法进行远程调动，默认类是`FailoverClusterInvoker`，它会执行集群模块的逻辑，主要是调用`Directory#list`方法获取所有该服务提供者的地址列表，然后将多个服务提供者聚合成一个`Invoker`， 并调用 Router 的 route 方法进行路由，过滤掉不符合路由规则的 Invoker。当 FailoverClusterInvoker 拿到 Directory 返回的 Invoker 列表后，它会通过 LoadBalance 从 Invoker 列表中选择一个 Invoker。最后 FailoverClusterInvoker 会将参数传给 LoadBalance 选择出的 Invoker 实例的 invoke 方法，进行真正的远程调用。

```
public class MockClusterInvoker<T> implements Invoker<T> {

    private final Invoker<T> invoker;

    public Result invoke(Invocation invocation) throws RpcException {
        Result result = null;

        // 获取 mock 配置值
        String value = directory.getUrl().getMethodParameter(invocation.getMethodName(), Constants.MOCK_KEY, Boolean.FALSE.toString()).trim();
        if (value.length() == 0 || value.equalsIgnoreCase("false")) {
            // 无 mock 逻辑，直接调用其他 Invoker 对象的 invoke 方法，
            // 比如 FailoverClusterInvoker
            result = this.invoker.invoke(invocation);
        } else if (value.startsWith("force")) {
            // force:xxx 直接执行 mock 逻辑，不发起远程调用
            result = doMockInvoke(invocation, null);
        } else {
            // fail:xxx 表示消费方对调用服务失败后，再执行 mock 逻辑，不抛出异常
            try {
                // 调用其他 Invoker 对象的 invoke 方法
                result = this.invoker.invoke(invocation);
            } catch (RpcException e) {
                if (e.isBiz()) {
                    throw e;
                } else {
                    // 调用失败，执行 mock 逻辑
                    result = doMockInvoke(invocation, e);
                }
            }
        }
        return result;
    }

    // 省略其他方法
}
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210410141420.webp)


