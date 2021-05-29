[toc]



# 异常监控方式

## 1.最小成本化

如果是刚成立的创业团队，可以用最小的实现成本来对系统的异常进行实时监控。所谓最小的实现成本，就是可以不用依赖任何三方的框架就可以实现。

可以采用手动埋点的方式将异常进行告警，这种方式最好是在全局异常处理的地方进行告警，才能统一管理。

如代码所示：

```plain
@ExceptionHandler(value = Exception.class)
@ResponseBody
public ResponseData<Object> defaultErrorHandler(HttpServletRequest req, Exception e) {
   // 记录异常
   // 钉钉或者短信告警
}
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424002244.png)

当我们的项目中有了全局异常处理，当底层报错的时候，异常都会进入到 ExceptionHandler 进行处理，在 ExceptionHandler 中我们可以通过 HttpServletRequest 来获取响应的请求信息和异常信息，然后进行告警。

### 1.1.异常告警信息

异常告警信息一定要详细，当线上出现异常后，第一时间要去修复这个问题。如果没有详细的信息根本就无法复现这个问题，就不好去定位和解决了。

告警信息需要有下面的内容：

```plain
告警服务：mobile-gateway
负责人：yinjihuan
请求地址：http://xxx.com/xxx/xxx?id=xxx
请求体：{ "name": "xxx" }
请求头：key=value
异常码：500
异常类型：RuntimeException
异常堆栈：java.lang.RuntimeException: com.xxx.exception.ApplicationException: 获取XXX信息失败！
```

最重要的就是请求参数了，有了参数才能复现错误。需要注意的是通过 HttpServletRequest 获取请求体的时候会报错，因为流只能读取一次。

等到了全局异常处理类的时候已经被读取过了，所以我们需要特殊处理一下，写个过滤器将请求体的值缓存起来，可以 `org.springframework.web.util.ContentCachingRequestWrapper` 对 `HttpServletRequest` 进行装饰，然后通过 `ContentCachingRequestWrapper` 获取请求体。

## 2.最小成本化+兼顾性能

手动埋点的方式对异常进行实时告警，然后直接发送短信等告警信息，这个过程是同步的，或多或少会加大响应的时间，不过请求进入到异常处理这里的话就证明这个请求已经失败了，影响不大。

虽然影响不大，但还是可以稍微优化一下。最常见的优化方式就是将同步转成异步操作，比如丢到单独的线程池中进行告警，丢到内存队列中，单独用一个线程去获取进行告警。

本地异步可能出现丢失的情况，对于这类监控的信息丢失几条问题也不大，如果不想丢失，可以使用外部的消息队列来存储告警信息，有单独的消费者进行消费，告警操作。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424002343.png)

## 3.统一日志监控

最小化成本的方式，只需要稍微写几十行代码就可以搞定。不好的点在于每个项目中都要有这样一份代码，告警的逻辑也是耦合在了代码中。。

什么 EFK，ELK 相信大家都听过，将日志统一进行收集，集中管理。每个系统中在出错的时候需要往本地日志中写入异常信息即可，不需要单独对异常进行告警，告警的动作可以由单独的告警系统来做，告警系统根据收集过来的日志进行判断，是否需要告警，告警频次等。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424002403.png)

统一日志监控需要搭建日志平台，成本相对来说高一点。当然也可以用开源的方案，也有商业的方案。

商业的可以用云服务，使用简单，快速接入，支持各种维度的告警规则，就是有点费钱。

如果只是想对异常进行监控，我推荐一款开源的错误追踪系统，Sentry 是一个开源的实时错误追踪系统，可以帮助开发者实时监控并修复异常问题，当然 Sentry 也有商业版。

## 4.APM 监控

`apm` (`Application Performance Management`) 除了对服务的调用链，性能进行详情的监控，同时对异常信息也有较好的监控。

常见的 `apm` 有 `skywalking`，`pinpoint`，`cat` 等，以 `cat` 来举例，`problem` 报表中展示的就是应用的错误信息，而且在 `cat` 的首页大盘中会按分钟展示各个应用的错误情况，如果有大量错误，大盘的颜色的就是红色，当你看到一片飘红的时候，那就是异常太多了。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424002511.png)







