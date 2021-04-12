[toc]



# Dubbo 应用

## 启动时检查

我们检查依赖的服务是否启动，可利用下面三个属性，优先级从左到右逐渐降低。

如果服务不是强依赖，或者说服务之间可能存在死循环依赖，我们应该将 check 置为 false。

检查判断优先级：`dubbo.reference.check` > `dubbo.consumer.check` > `dubbo.registry.check`

## 只订阅

一般在开发环境时，我们新开发的接口都还不能发布出去，所以我们本地的服务不能往注册中心注册；

而且，如果我们将本地服务注册上去，会影响别的服务的联调，所以我们会利用下面属性，将服务设置为只订阅，不往注册中心注册。

```properties
dubbo.registry.register=false
dubbo.registry.subscribe=true
```

## 集群容错

集群容错模式可利用 `dubbo.reference.cluster`、`dubbo.consumer.cluster` 和 `dubbo.service.cluster`、`dubbo.provider.cluster` 等属性配置。

**集群容错中主要有几个角色：**

**Invoker**：Provider 的一个可调用 Service 的抽象，Invoker 封装了 Provider 地址以及 Service 接口信息。

**Directory**：代表多个 Invoker ，可以把它看成是 List，但是与 List 不同的是，它的值可能是动态变化的，比如注册中心推送变更。

**Cluster**：将 Directory 中的多个 Invoker 伪装成一个 Invoker，对上层透明，伪装过程包含了容错逻辑，调用失败后，重试另一个。

**Router**：负责从多个 Invoker 中按路由规则选出子集，比如读写分离，应用隔离等。

**LoadBalance**：负责从多个 Invoker 中选出具体的一个用于本次调用，选的过程包含了负载均衡算法，调用失败后，需要重选

**dubbo 提供的集群容错模式：**

**Failover Cluster**：失败自动切换，当出现失败，重试其它服务器。通常用于读操作，但重试会带来更长延迟。可通过 retries="2" 来设置重试次数(不含第一次)。

**Failfast Cluster**：快速失败，即只发起一次调用，失败立即报错。通常用于非幂等性的写操作。

**Failsafe Cluster**：失败安全，出现异常时直接忽略掉。通常用于写入审计日志等不重要的操作。

**Failback Cluster**：失败自动恢复，后台记录失败请求，定时重发。通常用于消息通知操作。

**Forking Cluster**：并行调用多个服务器，只要一个成功就返回。通常用于实时性要求较高的读操作，但需要浪费更多服务资源。可通过 forks="2" 来设置最大并行数。

**Broadcast Cluster**：广播调用所有提供者，逐个调用，任意一台报错则报错。通常用于通知所有提供者更新缓存或日志等本地资源信息。

## 负载均衡

在集群负载均衡时，Dubbo 提供了多种均衡策略，缺省为 random 随机调用。

**Random LoadBalance：**

- 随机，按权重设置随机概率。
- 在一个截面上碰撞的概率高，但调用量越大分布越均匀，而且按概率使用权重后也比较均匀，有利于动态调整提供者权重。

**RoundRobin LoadBalance：**

- 轮询，按公约后的权重设置轮询比率。
- 存在慢的提供者累积请求的问题，比如：第二台机器很慢，但没挂，当请求调到第二台时就卡在那，久而久之，所有请求都卡在调到第二台上。

**LeastActive LoadBalance：**

- 最少活跃调用数，相同活跃数的随机，活跃数指调用前后计数差。
- 使慢的提供者收到更少请求，因为越慢的提供者的调用前后计数差会越大。

**ConsistentHash LoadBalance：**

- 一致性 Hash，相同参数的请求总是发到同一提供者。
- 当某一台提供者挂时，原本发往该提供者的请求，基于虚拟节点，平摊到其它提供者，不会引起剧烈变动。
- 算法参见：http://en.wikipedia.org/wiki/Consistent_hashing
- 缺省只对第一个参数 Hash，如果要修改，请配置 <dubbo:parameter key="hash.arguments" value="0,1" />
  缺省用 160 份虚拟节点，如果要修改，请配置 <dubbo:parameter key="hash.nodes" value="320" />

可在服务端服务级别、客户端服务级别、服务端方法级别和客户端方法级别设置。

**服务端服务级别&客户端服务级别配置：**

```properties
dubbo.provider.loadbalance=leastactive
dubbo.consumer.loadbalance=leastactive
```

**服务端方法级别和客户端方法级别配置:**

```java
@DubboService(interfaceClass = DubboServiceOne.class,loadbalance = "random",
                methods = {
                    @Method(name="sayHello",loadbalance = "leastActive")
                })
public class DubboServiceOneImpl implements DubboServiceOne {}

@DubboReference(loadbalance = "random",
                    methods = {
                        @Method(name="sayHello",loadbalance = "leastactive")
                    })
private DubboServiceOne DubboServiceOne;
```

## 直连提供者

在开发及测试环境下，经常需要绕过注册中心，只测试指定服务提供者，这时候可能需要点对点直连，点对点直连方式，将以服务接口为单位，忽略注册中心的提供者列表，A 接口配置点对点，不影响 B 接口从注册中心获取列表。

在 JVM 启动参数中加入 -D 参数映射服务地址：

```
java -D com.alibaba.xxx.XxxService=dubbo://localhost:20890
```

在`<dubbo.reference>`标签或者`@DubboResource注解`中增加 url 属性。

```xml
<debbo.reference url="dubbo://localhost:20890" interfaceClass=""/>
@DubboReference(url="dubbo://localhost:20890")
private DubboServiceOne DubboServiceOne;
```

## 本地调用

本地调用使用了 injvm 协议，是一个伪协议，它不开启端口，不发起远程调用，只在 JVM 内直接关联，但执行 Dubbo 的 Filter 链。

protocol、provider、consumer、service、reference 都可以设置。

## 本地存根

远程服务后，客户端通常只剩下接口，而实现全在服务器端，但提供方有些时候想在客户端也执行部分逻辑，比如：做 ThreadLocal 缓存，提前验证参数，调用失败后伪造容错数据等等，此时就需要在 API 中带上 Stub，客户端生成 Proxy 实例，会把 Proxy 通过构造函数传给 Stub ，然后把 Stub 暴露给用户，Stub 可以决定要不要去调 Proxy。

Sub 利用 `dubbo.service.sub` 属性设置。

**例子：**

假设服务提供者提供了一个接口，DubboServiceOne，然后服务消费者要做本地存根，只需要在自己的项目中，增加一个 DubboServiceOne 接口的实现类，然后在 `@DubboReference` 注解或者 `<dubbo:reference>` 标签增加 stub 属性即可。

**实现类：**

```java
/**
 * DubboServiceOne 本地存根
 * @author winfun
 * @date 2021/2/1 9:59 上午
 **/
@Slf4j
public class DubboServiceOneStub implements DubboServiceOne {

    private final DubboServiceOne dubboServiceOne;

    public DubboServiceOneStub(DubboServiceOne dubboServiceOne){
        this.dubboServiceOne = dubboServiceOne;
    }

    /***
     *  say hello
     * @author winfun
     * @param name name
     * @return {@link ApiResult <String> }
     **/
    @Override
    public ApiResult<String> sayHello(String name) {
        try {
            ApiResult<String> result = this.dubboServiceOne.sayHello(name);
            if (ApiContants.SUCCESS.equals(result.getCode())){
                return ApiResult.fail(ApiContants.FAIL,"业务异常",result.getData());
            }
        }catch (Exception e){
            log.error("call DubboServiceOne throw exception!message is {}",e.getMessage());
            return ApiResult.fail("调用失败");
        }
        return null;
    }
}
```

**使用：**

```java
/**
 * 测试本地存根
 * @author winfun
 * @date 2021/2/1 10:26 上午
 **/
@RestController
public class TestStubController {

    @DubboReference(lazy = true,check = false,stub = "com.winfun.demo.stub.DubboServiceOneStub")
    private DubboServiceOne dubboServiceOne;

    @GetMapping("/stub/{name}")
    public ApiResult<String> testStub(@PathVariable("name") String name){
        return this.dubboServiceOne.sayHello(name);
    }
}
```

## 本地伪装

本地伪装通常用于服务降级，比如某验权服务，当服务提供方全部挂掉后，客户端不抛出异常，而是通过 mock 数据返回授权失败。

mock 是 sub 的一个子集，mock是发生了错误，也就是抛出 RpcException 异常时会触发；而使用 sub，那么就需要在程序捕获异常，然后进行处理。

mock 机制可利用`<dubbo.reference>`标签或者`@DubboReference`的mock属性设置。

详细可看我自己写的文章：https://blog.csdn.net/Howinfun/article/details/113439208

## 服务延迟暴露

Dubbo 2.6.5 之后，所有服务都将在 Spring 初始化完成后进行暴露，如果你不需要延迟暴露服务，无需配置 delay。

可使用 `dubbo.service.delay` 属性来设置延迟多少秒暴露服务。delay 的时间单位为毫秒。

## 并发控制

并发控制都是在服务提供者端设置的。

首先，可通过 `dubbo.service.executes` 属性限制服务端每个方法的并发量（占用线程池线程数）、通过 `dubbo.method.executes`（服务端） 属性直接将并发限制具体到接口的某个方法。

还可以通过 `dubbo.service.actives`/`dubbo.reference.actives` 属性控制接口每个方法每个客户端的并发执行数；
通过 `dubbo.method.actives`（两端） 属性直接将并发执行控制到接口的某个方法。