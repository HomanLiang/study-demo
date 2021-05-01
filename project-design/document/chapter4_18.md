[toc]



# 分布式限流

## 1.前言

相信很多在中小型企业或者TO B企业的小伙伴们都未曾接触过限流。举个例子，小伙伴们就会发现，原来软件限流就在身边。相信很多小伙伴们都有12306买票回家的体验吧。如下图大家应该非常熟悉。

![12306登录](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210501150646.png)

没错，这就是坑死人不偿命的验证码，尤其在抢票的时候（当然今年疫情期间抢票容易很多），不管怎么选，总是被告知选错了。要么就给你一堆鬼都看不出什么东西的图，有时候真的让人怀疑人生。其实这就是一种限流措施，这种故意刁难用户的手段，光明正大地限制了流量的访问，大大降低了系统的访问压力。

## 2.基本概念

通过上述例子，老猫觉得小伙伴们至少心里对限流有了个定数，那么我们再来细看一下限流的维度。

其实对于一般限流场景来说，会有两个维度的信息：

1. **时间**：限流基于某个时间段或者时间点，即“时间窗口”，对每分钟甚至每秒做限定。
2. **资源**：基于现有可用资源的限制，比方说最大访问次数或者最高可用连接数。

基于上述的两个维度，我们基本可以给限流下个简单的定义，限流就是某个时间窗口对资源访问做限制。打个比方，每秒最多100个请求。但是在真正的场景中，我们不会仅仅只设置一种限流规则，而是多种规则共同作用。

![限流手段](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210501150702.png)

### 2.1.QPS以及连接数控制

针对上图中的连接数以及访问频次（QPS）限流来说，我们可以设定IP维度的限流。也可以基于当个服务器的限流。在实战的时候通常会设置多个维度限流规则，举个例子，访问同一个IP每秒访问频率小于10连接小于5，在设定每台机器QPS最高1000，连接数最大保持200。更进一步的，我们可以把整个服务器组以及机房当做一个整体，然后设置更高级别的限流规则。关于这个场景，老猫会在本文的后面篇幅给出具体的实现的demo代码。

### 2.2.传输速率

对于传输速率，大家应该不陌生，例如某盘如果不是会员给你几KB的下载，充完会员给你几十M的下载速率。这就是基于会员用户的限流逻辑。其实在Nginx中我们就可以限制传输速率，demo看本文后面篇幅。

### 2.3.黑白名单

黑白名单是很多企业的常见限流以及放行手段，而且黑白名单往往是动态变化的。举个例子，如果某个IP在一段时间中访问次数过于频繁，别系统识别为机器人或者流量攻击，那么IP就会被加入黑名单，从而限制了对系统资源的访问，这就是封IP，还是说到抢火车票，大家会用到第三方的软件，在进行刷票的时候，不晓得大家有没有留意有的时候会关进小黑屋，然后果断时间又被释放了。其实这就是基于黑名单的动态变化。

那关于白名单的话就更加不用解释了，就相当于通行证一样可以自由穿行在各个限流规则中。

### 2.4.分布式限流

现在很多系统都是分布式系统，老猫在之前和大家分享了分布式锁机制。那么什么叫做分布式限流呢？其实也很简单，就是区别于单机限流场景，把整个分布式环境中所有的服务器当做一个整体去考量。举个例子，比方说针对IP的限流，我们限制一个IP每秒最多100个访问，不管落到哪台机器上，只要是访问了集群中的服务节点，就会受到限流约束。

因此分布式的限流方案一般有这两种：

- 网关层限流：流量规则放在流量入口。
- 中间件限流：利用中间件，例如Redis缓存，每个组件都可以从这里获取当前时刻的流量统计，从而决定放行还是拒绝。

## 3.解决方案

### 3.1.基于GUAVA实现限流

相信很多铁子比较熟悉guava，它其实是谷歌出品的工具包，我们经常用它做一些集合操作或者做一些内存缓存操作。但是除了这些基本的用法之外，其实Guava在其他的领域涉及也很广，其中包括反射工具、函数式编程、数学运算等等。当然在限流的领域guava也做了贡献。主要的是在多线程的模块中提供了RateLimiter为首的几个限流支持类。Guava是一个客户端组件，就是说它作用范围仅限于当前的服务器，不能对集群以内的其他服务器加以流量控制。简单示例图如下

![guava使用简单架构](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210501150825.png)

### 3.2.网关层面实现限流

咱们直接看个图，准确地来说应该是个漏斗模型，具体如下：

![简单漏斗模型](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210501150830.png)

从上图中我们可以发现这基本是我们日常请求的一个正常的请求流程：

1. 用户流量从网关层到后台服务
2. 后台服务承接流量，调用缓存获取数据
3. 缓存中无数据的情况则回源查询数据库

那么我们为什么称呼它为漏斗模型？其实很简单，因为流量自上而下是递减的，在网关层聚集了最为密集的用户访问请求，其次才是后台服务，经过服务验证之后，刷掉一部分错误请求，剩下的请求落到缓存中，如果没有缓存的情况下才是最终的数据库层，所以数据库请求频次是最低的。之后老猫会将网关层Nginx的限流演示给大家看。

### 3.3.中间件限流

对于开发人员来说，网关层的限流需要寻找运维团队的配合才能实现，但是现在的年轻人控制欲都挺强的，于是大部分开发会决定在代码层面进行限流控制，那么此时，中间件Redis就成了不二之选。我们可以利用Redis的过期时间特性，请求设置限流的时间跨度（比如每秒是个请求，或者10秒10个请求）。同时Redis还有一个特殊的技能叫做脚本编程，我们可以将限流逻辑编写完成一段脚本植入到Redis中，这样就将限流的重任从服务层完全剥离出来，同时Redis强大的并发量特性以及高可用的集群架构也可以很好支持庞大集群的限流访问。

## 4.常用限流算法

### 4.1.令牌桶算法（Token bucket）

Token Bucket令牌桶算法是目前应用最为广泛的限流算法，顾名思义，它由以下两个角色构成：

- 令牌——获取到令牌的请求才会被处理，其他请求要么排队要么被丢弃。
- 桶——用来装令牌的地方，所有请求都是从桶中获取相关令牌。

简单令牌处理如下图：

![令牌桶算法模型](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210501150836.png)

**令牌生成**

该流程涉及令牌生成器以及令牌桶，对于令牌生成器来说，它会根据一个预定的速率向桶中添加令牌，例如每秒100个请求的速率发放令牌。这里的发放是匀速发放。令牌生成器类比水龙头，令牌桶类比水桶，当水盛满之后，接下来再往桶中灌水的话就会溢出，令牌发放性质是一样的，令牌桶的容量是有限的，当前若已经放满，那么此时新的令牌就会被丢弃掉。（大家可以尝试先思考一下令牌发放速率快慢有无坑点）。

**令牌获取**

每个请求到达之后，必须获取到一个令牌才能执行后面的逻辑。假如令牌的数量少，而访问请求较多的情况下，一部分请求自然无法获取到令牌，这个时候我们就可以设置一个缓冲队列来存储多余的令牌。缓冲队列也是一个可选项，并不是所有的令牌桶算法程序都会实现队列。当缓存队列存在的情况下，那些暂时没有获取到令牌的请求将被放到这个队列中排队，直到新的令牌产生后，再从队列头部拿出一个请求来匹配令牌。当队列满的情况下，这部分访问请求就被抛弃。其实在实际的应用中也可以设置队里的相关属性，例如设置队列中请求的存活时间，或者根据优先级排序等等。

### 4.2.漏桶算法（Leaky Bucket）

示意图如下：

![漏桶算法模型](https://img2020.cnblogs.com/blog/2200669/202102/2200669-20210222221645894-796760395.png)

漏桶算法判断逻辑和令牌桶有所类似，但是操作对象不同，令牌桶是将令牌放入桶内，而漏桶是将请求放到桶中。一样的是如果桶满了，那么后来的数据会被丢弃。漏桶算法只会以一个恒定的速度将数据包从桶内流出。

两种算法的联系和区别：

- 共同点：这两种算法都有一个“恒定”的速率和“不定”的速率。令牌桶是以恒定的速率创建令牌，但是访问请求获取令牌的速率是不定的，有多少令牌就发多少，令牌没了只能等着。漏桶算法是以很定的速率处理请求，但是流入桶内的请求的速度是不定的
- 不同点：漏桶天然决定它不会发生突发流量，就算每秒1000个请求，它后来服务输出的访问速率永远都是恒定的。然而令牌桶不同，其特性可以“预存”一定量的令牌，因此在应对突发流量的时候可以在短时间里消耗所有的令牌。突发流量的出率效率会比漏桶来的高，当然对应后台系统的压力也会大一些。

## 5.限流实战

### 5.1.基于guava的限流实战

pom依赖：

```xml
 <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>18.0</version>
  </dependency>
```

demo代码：

```java
   // 限流组件每秒允许发放两个令牌
    RateLimiter limiter = RateLimiter.create(2.0);
    //非阻塞限流
    @GetMapping("/tryAcquire")
    public String tryAcquire(Integer count){
        // 每次请求需要获取的令牌数量
        if (limiter.tryAcquire(count)){
            log.info("success, rate is {}",limiter.getRate());
            return "success";
        }else {
            log.info("fail ,rate is {}",limiter.getRate());
            return "fail";
        }
    }
    //限定时间的阻塞限流
    @GetMapping("tryAcquireWithTimeout")
    public String tryAcquireWithTimeout(Integer count, Integer timeout){
        if (limiter.tryAcquire(count,timeout, TimeUnit.SECONDS)){
            log.info("success, rate is {}",limiter.getRate());
            return "success";
        }else {
            log.info("fail ,rate is {}",limiter.getRate());
            return "fail";
        }
    }
    //同步阻塞限流
    @GetMapping("acquire")
    public String acquire(Integer count) {
        limiter.acquire(count);
        log.info("success, rate is {}",limiter.getRate());
        return "success";
    }
```

关于guava单机限流的演示，老猫简单地写了几个Demo。guava单机限流主要分为阻塞限流以及非阻塞限流。大家启动项目之后，可以调整相关的入参来观察一下日志的变更情况。

老猫举例分析一下其中一种请求的结果 localhost:10088/tryAcquire?count=2;请求之后输出的日志如下：

```tex
2021-02-18 23:41:48.615  INFO 5004 --- [io-10088-exec-9] com.ktdaddy.KTController:success,rate is2.0
2021-02-18 23:41:49.164  INFO 5004 --- [io-10088-exec-2] com.ktdaddy.KTController:success, rate is2.0
2021-02-18 23:41:49.815  INFO 5004 --- [o-10088-exec-10] com.ktdaddy.KTController:success, rate is2.0
2021-02-18 23:41:50.205  INFO 5004 --- [io-10088-exec-1] com.ktdaddy.KTController:fail ,rate is 2.0
2021-02-18 23:41:50.769  INFO 5004 --- [io-10088-exec-3] com.ktdaddy.KTController:success,rate is 2.0
2021-02-18 23:41:51.470  INFO 5004 --- [io-10088-exec-4] com.ktdaddy.KTController:fail ,rate is 2.0
```

从请求日志中我们神奇地发现前两次请求中间间隔不到一秒，但是消耗了令牌确都是成功的。这个是什么原因呢？这里面卖个关子，后面再和大家同步一下guava的流量预热模型。

### 5.2.基于Nginx限流实战

nginx.conf限流配置如下：

```nginx
# 根据 IP地址限制速度
# （1）第一个参数 $binary_remote_addr：可以理解成nginx内部系统的变量
#                binary_目的是缩写内存占用，remote_addr表示通过IP地址来限流
#
# （2）第二个参数 zone=iplimit:20m
#                 iplimit是一块内存区域，20m是指这块内存区域的大小（专门记录访问频率信息）  
# （3）第三个参数 rate=1r/s，标识访问的限流频率
#                 配置形式不止一种，还例如：100r/m
limit_req_zone $binary_remote_addr zone=iplimit:20m rate=10r/s;

# 根据服务器级别做限流
limit_req_zone $server_name zone=serverlimit:10m rate=1r/s;

# 基于IP连接数的配置
limit_conn_zone $binary_remote_addr zone=perip:20m;

# 根据服务器级别做限流
limit_conn_zone $server_name zone=perserver:20m;

# 普通的服务映射域名limit-rate.ktdaddy.com映射到http://127.0.0.1:10088/服务。
# 大家本地可以通过配置host映射去实现，很简单，不多赘述。
server {
        server_name  limit-rate.ktdaddy.com;
        location /access-limit/ {
            proxy_pass http://127.0.0.1:10088/;
            
            # 基于IP地址的限制
            # 1）第一个参数  zone=iplimit => 引用limit_req_zone的zone变量信息
            # 2）第二个参数  burst=2,设置一个大小为2的缓冲区域，当大量请求到来，请求数量超过限流频率的时候
            #             将其放入缓冲区域
            # 3）第三个参数  nodelay=> 缓冲区满了以后，直接返回503异常
            limit_req zone=iplimit burst=2 nodelay;
        
            # 基于服务器级别的限制
            # 通常情况下,server级别的限流速率大于IP限流的速率（大家可以思考一下，其实比较简单）
            limit_req zone=serverlimit burst=1 nodelay;
       
            # 每个server最多保持100个链接
            limit_conn perserver 100;
            # 每个Ip地址保持1个链接地址
            limit_conn perip 1;
            # 异常情况返回指定返回504而不是默认的503
            limit_req_status 504;
            limit_conn_status 504;
        }
        # 简单下载限速的配置，表示下载文件达到100m之后限制速度为256k的下载速度
        location /download/ {
              limit_rate_after 100m;
              limit_rate 256k;
        }
    }
```

上面配置里面其实结合了nginx限流四种配置方式，分别是基于ip的限流方式，基于每台服务的限流，基于IP连接数的限流，基于每台服务连接数的限流。当然最后还给大家提到了下载限速的配置。大家有兴趣可以模拟配置一下，体验一下基于nginx的限流。

### 5.3.基于Redis+Lua的限流组件

此处还是比较重要的，老猫决定单独作为一个知识点和大家分享，此处暂时省略。