[toc]



# Arthas

## 1.相关网站

[官方文档](https://arthas.aliyun.com/doc/)

## 2.简介
Arthas是Alibaba开源的Java诊断工具，深受开发者喜爱。它采用命令行交互模式，同时提供丰富的 Tab 自动补全功能，进一步方便进行问题的定位和诊断。

## 3.安装
> 为了还原一个真实的线上环境，我们将通过Arthas来对Docker容器中的Java程序进行诊断。

1. 使用arthas-boot，下载对应jar包，下载地址：https://alibaba.github.io/arthas/arthas-boot.jar

2. 将我们的Spring Boot应用mall-tiny-arthas使用Docker容器的方式启动起来，打包和运行脚本在项目的src\main\docker目录下；

1. 将arthas-boot.jar拷贝到我们应用容器的\目录下；

    ```
    docker container cp arthas-boot.jar mall-tiny-arthas:/
    ```

4. 进入容器并启动arthas-boot，直接当做jar包启动即可；

    ```
    docker exec -it mall-tiny-arthas /bin/bash
    java -jar arthas-boot.jar
    ```

5. 启动成功后，选择当前需要诊断的Java程序的序列号，这里是1，就可以开始诊断了；

  ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182319.png)

6. 期间会下载一些所需的文件，完成后控制台打印信息如下，至此Arthas就安装启动完成了。

  ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182336.png)

## 4.常用命令
> 我们先来介绍一些Arthas的常用命令，会结合实际应用来讲解，带大家了解下Arthas的使用。

### dashboard
使用dashboard命令可以显示当前系统的实时数据面板，包括线程信息、JVM内存信息及JVM运行时参数。

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182353.png)

### thread
查看当前线程信息，查看线程的堆栈，可以找出当前最占CPU的线程。

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182405.png)

常用命令：
```
# 打印当前最忙的3个线程的堆栈信息
thread -n 3
# 查看ID为1都线程的堆栈信息
thread 1
# 找出当前阻塞其他线程的线程
thread -b
# 查看指定状态的线程
thread -state WAITING
```

### sysprop
查看当前JVM的系统属性，比如当容器时区与宿主机不一致时，可以使用如下命令查看时区信息。
```
sysprop |grep timezone
```
```
user.timezone                  Asia/Shanghai
```

### sysenv
查看JVM的环境属性，比如查看下我们当前启用的是什么环境的Spring Boot配置。

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182415.png)

### logger
使用logger命令可以查看日志信息，并改变日志级别，这个命令非常有用。

比如我们在生产环境上一般是不会打印DEBUG级别的日志的，当我们在线上排查问题时可以临时开启DEBUG级别的日志，帮助我们排查问题，下面介绍下如何操作。
- 我们的应用默认使用的是INFO级别的日志，使用logger命令可以查看；

  ![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182426.png)

- 使用如下命令改变日志级别为DEBUG，需要使用-c参数指定类加载器的HASH值；

    ```
    logger -c 21b8d17c --name ROOT --level debug
    ```

- 再使用logger命令查看，发现ROOT级别日志已经更改；

  ![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182443.png)

- 使用docker logs -f mall-tiny-arthas命令查看容器日志，发现已经打印了DEBUG级别的日志；

  ![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182452.png)

- 查看完日志以后记得要把日志级别再调回INFO级别。

    ```
    logger -c 21b8d17c --name ROOT --level info
    ```

### sc
查看JVM已加载的类信息，Search-Class的简写，搜索出所有已经加载到 JVM 中的类信息。
- 搜索com.macro.mall包下所有的类；

    ```
    sc com.macro.mall.*
    ```

	![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182744.png)

- 打印类的详细信息，加入-d参数并指定全限定类名；

    ```
    sc -d com.macro.mall.tiny.common.api.CommonResult
    ```

	![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182805.png)

- 打印出类的Field信息，使用-f参数。

    ```
    sc -d -f com.macro.mall.tiny.common.api.CommonResult
    ```

	![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182828.png)

### sm
查看已加载类的方法信息，Search-Method的简写，搜索出所有已经加载的类的方法信息。
- 查看类中的所有方法；

    ```
    sm com.macro.mall.tiny.common.api.CommonResult
    ```

	![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182933.png)

- 查看指定方法信息，使用-d参数并指定方法名称；

    ```
    sm -d com.macro.mall.tiny.common.api.CommonResult getCode
    ```

	![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424182957.png)

### jad
反编译已加载类的源码，觉得线上代码和预期不一致，可以反编译看看。
- 查看启动类的相关信息，默认会带有ClassLoader信息；

    ```
    jad com.macro.mall.tiny.MallTinyApplication
    ```

	![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183023.png)

- 使用--source-only参数可以只打印类信息。

    ```
    jad --source-only com.macro.mall.tiny.MallTinyApplication
    ```

	![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183045.png)

### mc
内存编译器，Memory Compiler的缩写，编译.java文件生成.class。

### redefine
加载外部的.class文件，覆盖掉 JVM中已经加载的类。

### monitor
实时监控方法执行信息，可以查看方法执行成功此时、失败次数、平均耗时等信息。
```
monitor -c 5 com.macro.mall.tiny.controller.PmsBrandController listBrand
```
![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183056.png)

### watch
方法执行数据观测，可以观察方法执行过程中的参数和返回值。

使用如下命令观察方法执行参数和返回值，-x表示结果属性遍历深度。
```
watch com.macro.mall.tiny.service.impl.PmsBrandServiceImpl listBrand "{params,returnObj}" -x 2
```
![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183106.png)

## 5.热更新
> 尽管在线上环境热更代码并不是一个很好的行为，但有的时候我们真的很需要热更代码。下面介绍下如何使用jad/mc/redefine来热更新代码。

- 首先我们有一个商品详情的接口，当我们传入id<=0时，会抛出IllegalArgumentException；

    ```
    /**
     * 品牌管理Controller
     * Created by macro on 2019/4/19.
     */
    @Api(tags = "PmsBrandController", description = "商品品牌管理")
    @Controller
    @RequestMapping("/brand")
    public class PmsBrandController {
        @Autowired
        private PmsBrandService brandService;

        private static final Logger LOGGER = LoggerFactory.getLogger(PmsBrandController.class);

        @ApiOperation("获取指定id的品牌详情")
        @RequestMapping(value = "/{id}", method = RequestMethod.GET)
        @ResponseBody
        public CommonResult<PmsBrand> brand(@PathVariable("id") Long id) {
            if(id<=0){
                throw new IllegalArgumentException("id not excepted id:"+id);
            }
            return CommonResult.success(brandService.getBrand(id));
        }
    }
    ```

- 调用接口会返回如下信息，调用地址：http://192.168.5.94:8088/brand/0

    ```
    {
      "timestamp": "2020-06-12T06:20:20.951+0000",
      "status": 500,
      "error": "Internal Server Error",
      "message": "id not excepted id:0",
      "path": "/brand/0"
    }
    ```

- 我们想对该问题进行修复，如果传入id<=0时，直接返回空数据的CommonResult，代码修改内容如下；

    ```
    /**
     * 品牌管理Controller
     * Created by macro on 2019/4/19.
     */
    @Api(tags = "PmsBrandController", description = "商品品牌管理")
    @Controller
    @RequestMapping("/brand")
    public class PmsBrandController {
        @Autowired
        private PmsBrandService brandService;

        private static final Logger LOGGER = LoggerFactory.getLogger(PmsBrandController.class);

        @ApiOperation("获取指定id的品牌详情")
        @RequestMapping(value = "/{id}", method = RequestMethod.GET)
        @ResponseBody
        public CommonResult<PmsBrand> brand(@PathVariable("id") Long id) {
            if(id<=0){
    //            throw new IllegalArgumentException("id not excepted id:"+id);
                return CommonResult.success(null);
            }
            return CommonResult.success(brandService.getBrand(id));
        }
    }
    ```

- 首先我们需要对PmsBrandController类代码进行修改，接着上传到服务器，然后使用如下命令将java文件拷贝到容器的/tmp目录下；

    ```
    docker container cp /tmp/PmsBrandController.java mall-tiny-arthas:/tmp/
    ```

- 之后我们需要查看该类的类加载器的Hash值；

    ```
    sc -d *PmsBrandController | grep classLoaderHash
    ```

	![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183319.png)

- 之后使用内存编译器把改.java文件编译成.class文件，注意需要使用-c指定类加载器；

    ```
    mc -c 21b8d17c /tmp/PmsBrandController.java -d /tmp
    ```

	![Image [19]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183329.png)

- 最后使用redefine命令加载.class文件，将原来加载的类覆盖掉；

    ```
    redefine -c 21b8d17c /tmp/com/macro/mall/tiny/controller/PmsBrandController.class
    ```

	![Image [20]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183341.png)

- 我们再次调用接口进行测试，发现已经返回了预期的结果，调用地址：http://192.168.3.101:8088/brand/0

    ```
    {
      "code": 200,
      "message": "操作成功",
      "data": null
    }
    ```

## 6.实际案例

### 6.1.如何使用 Arthas 定位 Spring Boot 接口超时

#### 6.1.1.背景

公司有个渠道系统，专门对接三方渠道使用，没有什么业务逻辑，主要是转换报文和参数校验之类的工作，起着一个承上启下的作用。

最近在优化接口的响应时间，优化了代码之后，但是时间还是达不到要求；有一个诡异的100ms左右的耗时问题，在接口中打印了请求处理时间后，和调用方的响应时间还有差了100ms左右。比如程序里记录150ms，但是调用方等待时间却为250ms左右。

下面记录下当时详细的定位&解决流程（其实解决很简单，关键在于怎么定位并找到解决问题的方法）

#### 6.1.2.定位过程

##### 6.1.2.1.分析代码

渠道系统是一个常见的spring-boot web工程，使用了集成的tomcat。分析了代码之后，发现并没有特殊的地方，没有特殊的过滤器或者拦截器，所以初步排除是业务代码问题

##### 6.1.2.2.分析调用流程

出现这个问题之后，首先确认了下接口的调用流程。由于是内部测试，所以调用流程较少。关注公众号码猿技术专栏获取更多面试资源。

```
Nginx -反向代理-> 渠道系统
```

公司是云服务器，网络走的也是云的内网。由于不明确问题的原因，所以用排除法，首先确认服务器网络是否有问题。

先确认发送端到Nginx Host是否有问题：

```
[jboss@VM_0_139_centos ~]$ ping 10.0.0.139
PING 10.0.0.139 (10.0.0.139) 56(84) bytes of data.
64 bytes from 10.0.0.139: icmp_seq=1 ttl=64 time=0.029 ms
64 bytes from 10.0.0.139: icmp_seq=2 ttl=64 time=0.041 ms
64 bytes from 10.0.0.139: icmp_seq=3 ttl=64 time=0.040 ms
64 bytes from 10.0.0.139: icmp_seq=4 ttl=64 time=0.040 ms
```

从ping结果上看，发送端到Nginx主机的延迟是无问题的，接下来查看Nginx到渠道系统的网络。

```
# 由于日志是没问题的，这里直接复制上面日志了
[jboss@VM_0_139_centos ~]$ ping 10.0.0.139
PING 10.0.0.139 (10.0.0.139) 56(84) bytes of data.
64 bytes from 10.0.0.139: icmp_seq=1 ttl=64 time=0.029 ms
64 bytes from 10.0.0.139: icmp_seq=2 ttl=64 time=0.041 ms
64 bytes from 10.0.0.139: icmp_seq=3 ttl=64 time=0.040 ms
64 bytes from 10.0.0.139: icmp_seq=4 ttl=64 time=0.040 ms
```

从ping结果上看，Nginx到渠道系统服务器网络延迟也是没问题的

既然网络看似没问题，那么可以继续排除法，砍掉Nginx，客户端直接再渠道系统的服务器上，通过回环地址（localhost）直连，避免经过网卡/dns，缩小问题范围看看能否复现（这个应用和地址是我后期模拟的，测试的是一个空接口）：

```
[jboss@VM_10_91_centos tmp]$ curl -w "@curl-time.txt" http://127.0.0.1:7744/send
success
              http: 200
               dns: 0.001s
          redirect: 0.000s
      time_connect: 0.001s
   time_appconnect: 0.000s
  time_pretransfer: 0.001s
time_starttransfer: 0.073s
     size_download: 7bytes
    speed_download: 95.000B/s
                  ----------
        time_total: 0.073s 请求总耗时
```

从curl日志上看，通过回环地址调用一个空接口耗时也有73ms。这就奇怪了，跳过了中间所有调用节点（包括过滤器&拦截器之类），直接请求应用一个空接口，都有73ms的耗时，再请求一次看看：

```
[jboss@VM_10_91_centos tmp]$ curl -w "@curl-time.txt" http://127.0.0.1:7744/send
success
              http: 200
               dns: 0.001s
          redirect: 0.000s
      time_connect: 0.001s
   time_appconnect: 0.000s
  time_pretransfer: 0.001s
time_starttransfer: 0.003s
     size_download: 7bytes
    speed_download: 2611.000B/s
                  ----------
        time_total: 0.003s
```

更奇怪的是，第二次请求耗时就正常了，变成了3ms。经查阅资料，linux curl是默认开启http keep-alive的。就算不开启keep-alive，每次重新handshake，也不至于需要70ms。关注公众号码猿技术专栏获取更多面试资源。

经过不断分析测试发现，连续请求的话时间就会很短，每次请求只需要几毫秒，但是如果隔一段时间再请求，就会花费70ms以上。

从这个现象猜想，可能是某些缓存机制导致的，连续请求因为有缓存，所以速度快，时间长缓存失效后导致时间长。

那么这个问题点到底在哪一层呢？tomcat层还是spring-webmvc呢？

光猜想定位不了问题，还是得实际测试一下，把渠道系统的代码放到本地ide里启动测试能否复现

但是导入本地Ide后，在Ide中启动后并不能复现问题，并没有70+ms的延迟问题。这下头疼了，本地无法复现，不能Debug，由于问题点不在业务代码，也不能通过加日志的方式来Debug

这时候可以祭出神器Arthas了

#### 6.1.3.Arthas分析问题

Arthas 是Alibaba开源的Java诊断工具，深受开发者喜爱。当你遇到以下类似问题而束手无策时，Arthas可以帮助你解决：

- 这个类从哪个 jar 包加载的？为什么会报各种类相关的 Exception？
- 我改的代码为什么没有执行到？难道是我没 commit？分支搞错了？
- 遇到问题无法在线上 debug，难道只能通过加日志再重新发布吗？
- 线上遇到某个用户的数据处理有问题，但线上同样无法 debug，线下无法重现！
- 是否有一个全局视角来查看系统的运行状况？
- 有什么办法可以监控到JVM的实时运行状态？

上面是Arthas的官方简介，这次我只需要用他的一个小功能trace。动态计算方法调用路径和时间，这样我就可以定位时间在哪个地方被消耗了。

- trace 方法内部调用路径，并输出方法路径上的每个节点上耗时
- trace 命令能主动搜索 class-pattern／method-pattern
- 对应的方法调用路径，渲染和统计整个调用链路上的所有性能开销和追踪调用链路。

有了神器，那么该追踪什么方法呢？由于我对Tomcat源码不是很熟，所以只能从spring mvc下手，先来trace一下spring mvc的入口：

```
[arthas@24851]$ trace org.springframework.web.servlet.DispatcherServlet *
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:44) cost in 508 ms.
`---ts=2019-09-14 21:07:44;thread_name=http-nio-7744-exec-2;id=11;is_daemon=true;priority=5;TCCL=org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader@7c136917
    `---[2.952142ms] org.springframework.web.servlet.DispatcherServlet:buildLocaleContext()

`---ts=2019-09-14 21:07:44;thread_name=http-nio-7744-exec-2;id=11;is_daemon=true;priority=5;TCCL=org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader@7c136917
    `---[18.08903ms] org.springframework.web.servlet.DispatcherServlet:doService()
        +---[0.041346ms] org.apache.commons.logging.Log:isDebugEnabled() #889
        +---[0.022398ms] org.springframework.web.util.WebUtils:isIncludeRequest() #898
        +---[0.014904ms] org.springframework.web.servlet.DispatcherServlet:getWebApplicationContext() #910
        +---[1.071879ms] javax.servlet.http.HttpServletRequest:setAttribute() #910
        +---[0.020977ms] javax.servlet.http.HttpServletRequest:setAttribute() #911
        +---[0.017073ms] javax.servlet.http.HttpServletRequest:setAttribute() #912
        +---[0.218277ms] org.springframework.web.servlet.DispatcherServlet:getThemeSource() #913
        |   `---[0.137568ms] org.springframework.web.servlet.DispatcherServlet:getThemeSource()
        |       `---[min=0.00783ms,max=0.014251ms,total=0.022081ms,count=2] org.springframework.web.servlet.DispatcherServlet:getWebApplicationContext() #782
        +---[0.019363ms] javax.servlet.http.HttpServletRequest:setAttribute() #913
        +---[0.070694ms] org.springframework.web.servlet.FlashMapManager:retrieveAndUpdate() #916
        +---[0.01839ms] org.springframework.web.servlet.FlashMap:<init>() #920
        +---[0.016943ms] javax.servlet.http.HttpServletRequest:setAttribute() #920
        +---[0.015268ms] javax.servlet.http.HttpServletRequest:setAttribute() #921
        +---[15.050124ms] org.springframework.web.servlet.DispatcherServlet:doDispatch() #925
        |   `---[14.943477ms] org.springframework.web.servlet.DispatcherServlet:doDispatch()
        |       +---[0.019135ms] org.springframework.web.context.request.async.WebAsyncUtils:getAsyncManager() #953
        |       +---[2.108373ms] org.springframework.web.servlet.DispatcherServlet:checkMultipart() #960
        |       |   `---[2.004436ms] org.springframework.web.servlet.DispatcherServlet:checkMultipart()
        |       |       `---[1.890845ms] org.springframework.web.multipart.MultipartResolver:isMultipart() #1117
        |       +---[2.054361ms] org.springframework.web.servlet.DispatcherServlet:getHandler() #964
        |       |   `---[1.961963ms] org.springframework.web.servlet.DispatcherServlet:getHandler()
        |       |       +---[0.02051ms] java.util.List:iterator() #1183
        |       |       +---[min=0.003805ms,max=0.009641ms,total=0.013446ms,count=2] java.util.Iterator:hasNext() #1183
        |       |       +---[min=0.003181ms,max=0.009751ms,total=0.012932ms,count=2] java.util.Iterator:next() #1183
        |       |       +---[min=0.005841ms,max=0.015308ms,total=0.021149ms,count=2] org.apache.commons.logging.Log:isTraceEnabled() #1184
        |       |       `---[min=0.474739ms,max=1.19145ms,total=1.666189ms,count=2] org.springframework.web.servlet.HandlerMapping:getHandler() #1188
        |       +---[0.013071ms] org.springframework.web.servlet.HandlerExecutionChain:getHandler() #971
        |       +---[0.372236ms] org.springframework.web.servlet.DispatcherServlet:getHandlerAdapter() #971
        |       |   `---[0.280073ms] org.springframework.web.servlet.DispatcherServlet:getHandlerAdapter()
        |       |       +---[0.004804ms] java.util.List:iterator() #1224
        |       |       +---[0.003668ms] java.util.Iterator:hasNext() #1224
        |       |       +---[0.003038ms] java.util.Iterator:next() #1224
        |       |       +---[0.006451ms] org.apache.commons.logging.Log:isTraceEnabled() #1225
        |       |       `---[0.012683ms] org.springframework.web.servlet.HandlerAdapter:supports() #1228
        |       +---[0.012848ms] javax.servlet.http.HttpServletRequest:getMethod() #974
        |       +---[0.013132ms] java.lang.String:equals() #975
        |       +---[0.003025ms] org.springframework.web.servlet.HandlerExecutionChain:getHandler() #977
        |       +---[0.008095ms] org.springframework.web.servlet.HandlerAdapter:getLastModified() #977
        |       +---[0.006596ms] org.apache.commons.logging.Log:isDebugEnabled() #978
        |       +---[0.018024ms] org.springframework.web.context.request.ServletWebRequest:<init>() #981
        |       +---[0.017869ms] org.springframework.web.context.request.ServletWebRequest:checkNotModified() #981
        |       +---[0.038542ms] org.springframework.web.servlet.HandlerExecutionChain:applyPreHandle() #986
        |       +---[0.00431ms] org.springframework.web.servlet.HandlerExecutionChain:getHandler() #991
        |       +---[4.248493ms] org.springframework.web.servlet.HandlerAdapter:handle() #991
        |       +---[0.014805ms] org.springframework.web.context.request.async.WebAsyncManager:isConcurrentHandlingStarted() #993
        |       +---[1.444994ms] org.springframework.web.servlet.DispatcherServlet:applyDefaultViewName() #997
        |       |   `---[0.067631ms] org.springframework.web.servlet.DispatcherServlet:applyDefaultViewName()
        |       +---[0.012027ms] org.springframework.web.servlet.HandlerExecutionChain:applyPostHandle() #998
        |       +---[0.373997ms] org.springframework.web.servlet.DispatcherServlet:processDispatchResult() #1008
        |       |   `---[0.197004ms] org.springframework.web.servlet.DispatcherServlet:processDispatchResult()
        |       |       +---[0.007074ms] org.apache.commons.logging.Log:isDebugEnabled() #1075
        |       |       +---[0.005467ms] org.springframework.web.context.request.async.WebAsyncUtils:getAsyncManager() #1081
        |       |       +---[0.004054ms] org.springframework.web.context.request.async.WebAsyncManager:isConcurrentHandlingStarted() #1081
        |       |       `---[0.011988ms] org.springframework.web.servlet.HandlerExecutionChain:triggerAfterCompletion() #1087
        |       `---[0.004015ms] org.springframework.web.context.request.async.WebAsyncManager:isConcurrentHandlingStarted() #1018
        +---[0.005055ms] org.springframework.web.context.request.async.WebAsyncUtils:getAsyncManager() #928
        `---[0.003422ms] org.springframework.web.context.request.async.WebAsyncManager:isConcurrentHandlingStarted() #928
```

~

```
[jboss@VM_10_91_centos tmp]$ curl -w "@curl-time.txt" http://127.0.0.1:7744/send
success
              http: 200
               dns: 0.001s
          redirect: 0.000s
      time_connect: 0.001s
   time_appconnect: 0.000s
  time_pretransfer: 0.001s
time_starttransfer: 0.115s
     size_download: 7bytes
    speed_download: 60.000B/s
                  ----------
        time_total: 0.115s
```

本次调用，调用端时间花费115ms，但是从arthas trace上看，spring mvc只消耗了18ms，那么剩下的97ms去哪了呢？

本地测试后已经可以排除spring mvc的问题了，最后也是唯一可能出问题的点就是tomcat

可是本人并不熟悉tomcat中的源码，就连请求入口都不清楚，tomcat里需要trace的类都不好找。。。

不过没关系，有神器Arthas，可以通过stack命令来反向查找调用路径，以`org.springframework.web.servlet.DispatcherServlet`作为参数：

> stack 输出当前方法被调用的调用路径
>
> 很多时候我们都知道一个方法被执行，但这个方法被执行的路径非常多，或者你根本就不知道这个方法是从那里被执行了，此时你需要的是 stack 命令。

```
[arthas@24851]$ stack org.springframework.web.servlet.DispatcherServlet *
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:44) cost in 495 ms.
ts=2019-09-14 21:15:19;thread_name=http-nio-7744-exec-5;id=14;is_daemon=true;priority=5;TCCL=org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader@7c136917
    @org.springframework.web.servlet.FrameworkServlet.processRequest()
        at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:866)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:635)
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:851)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:742)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.HttpPutFormContentFilter.doFilterInternal(HttpPutFormContentFilter.java:109)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:81)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:198)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:496)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:140)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:81)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:87)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:342)
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:803)
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:790)
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1468)
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:748)

ts=2019-09-14 21:15:19;thread_name=http-nio-7744-exec-5;id=14;is_daemon=true;priority=5;TCCL=org.springframework.boot.web.embedded.tomcat.TomcatEmbeddedWebappClassLoader@7c136917
    @org.springframework.web.servlet.DispatcherServlet.doService()
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:974)
        at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:866)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:635)
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:851)
        at javax.servlet.http.HttpServlet.service(HttpServlet.java:742)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:231)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:99)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.HttpPutFormContentFilter.doFilterInternal(HttpPutFormContentFilter.java:109)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.HiddenHttpMethodFilter.doFilterInternal(HiddenHttpMethodFilter.java:81)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:193)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:166)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:198)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:96)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:496)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:140)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:81)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:87)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:342)
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:803)
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:66)
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:790)
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1468)
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:49)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:748)
```

从stack日志上可以很直观的看出DispatchServlet的调用栈，那么这么长的路径，该trace哪个类呢（这里跳过spring mvc中的过滤器的trace过程，实际排查的时候也trace了一遍，但这诡异的时间消耗不是由这里过滤器产生的）？

有一定经验的老司机从名字上大概也能猜出来从哪里下手比较好，那就是`org.apache.coyote.http11.Http11Processor.service`，从名字上看，http1.1处理器，这可能是一个比较好的切入点。下面来trace一下：

```
[arthas@24851]$ trace org.apache.coyote.http11.Http11Processor service
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 269 ms.
`---ts=2019-09-14 21:22:51;thread_name=http-nio-7744-exec-8;id=17;is_daemon=true;priority=5;TCCL=org.springframework.boot.loader.LaunchedURLClassLoader@20ad9418
    `---[131.650285ms] org.apache.coyote.http11.Http11Processor:service()
        +---[0.036851ms] org.apache.coyote.Request:getRequestProcessor() #667
        +---[0.009986ms] org.apache.coyote.RequestInfo:setStage() #668
        +---[0.008928ms] org.apache.coyote.http11.Http11Processor:setSocketWrapper() #671
        +---[0.013236ms] org.apache.coyote.http11.Http11InputBuffer:init() #672
        +---[0.00981ms] org.apache.coyote.http11.Http11OutputBuffer:init() #673
        +---[min=0.00213ms,max=0.007317ms,total=0.009447ms,count=2] org.apache.coyote.http11.Http11Processor:getErrorState() #683
        +---[min=0.002098ms,max=0.008888ms,total=0.010986ms,count=2] org.apache.coyote.ErrorState:isError() #683
        +---[min=0.002448ms,max=0.007149ms,total=0.009597ms,count=2] org.apache.coyote.http11.Http11Processor:isAsync() #683
        +---[min=0.002399ms,max=0.00852ms,total=0.010919ms,count=2] org.apache.tomcat.util.net.AbstractEndpoint:isPaused() #683
        +---[min=0.033587ms,max=0.11832ms,total=0.151907ms,count=2] org.apache.coyote.http11.Http11InputBuffer:parseRequestLine() #687
        +---[0.005384ms] org.apache.tomcat.util.net.AbstractEndpoint:isPaused() #695
        +---[0.007924ms] org.apache.coyote.Request:getMimeHeaders() #702
        +---[0.006744ms] org.apache.tomcat.util.net.AbstractEndpoint:getMaxHeaderCount() #702
        +---[0.012574ms] org.apache.tomcat.util.http.MimeHeaders:setLimit() #702
        +---[0.14319ms] org.apache.coyote.http11.Http11InputBuffer:parseHeaders() #703
        +---[0.003997ms] org.apache.coyote.Request:getMimeHeaders() #743
        +---[0.026561ms] org.apache.tomcat.util.http.MimeHeaders:values() #743
        +---[min=0.002869ms,max=0.01203ms,total=0.014899ms,count=2] java.util.Enumeration:hasMoreElements() #745
        +---[0.070114ms] java.util.Enumeration:nextElement() #746
        +---[0.010921ms] java.lang.String:toLowerCase() #746
        +---[0.008453ms] java.lang.String:contains() #746
        +---[0.002698ms] org.apache.coyote.http11.Http11Processor:getErrorState() #775
        +---[0.00307ms] org.apache.coyote.ErrorState:isError() #775
        +---[0.002708ms] org.apache.coyote.RequestInfo:setStage() #777
        +---[0.171139ms] org.apache.coyote.http11.Http11Processor:prepareRequest() #779
        +---[0.009349ms] org.apache.tomcat.util.net.SocketWrapperBase:decrementKeepAlive() #794
        +---[0.002574ms] org.apache.coyote.http11.Http11Processor:getErrorState() #800
        +---[0.002696ms] org.apache.coyote.ErrorState:isError() #800
        +---[0.002499ms] org.apache.coyote.RequestInfo:setStage() #802
        +---[0.005641ms] org.apache.coyote.http11.Http11Processor:getAdapter() #803
        +---[129.868916ms] org.apache.coyote.Adapter:service() #803
        +---[0.003859ms] org.apache.coyote.http11.Http11Processor:getErrorState() #809
        +---[0.002365ms] org.apache.coyote.ErrorState:isError() #809
        +---[0.003844ms] org.apache.coyote.http11.Http11Processor:isAsync() #809
        +---[0.002382ms] org.apache.coyote.Response:getStatus() #809
        +---[0.002476ms] org.apache.coyote.http11.Http11Processor:statusDropsConnection() #809
        +---[0.002284ms] org.apache.coyote.RequestInfo:setStage() #838
        +---[0.00222ms] org.apache.coyote.http11.Http11Processor:isAsync() #839
        +---[0.037873ms] org.apache.coyote.http11.Http11Processor:endRequest() #843
        +---[0.002188ms] org.apache.coyote.RequestInfo:setStage() #845
        +---[0.002112ms] org.apache.coyote.http11.Http11Processor:getErrorState() #849
        +---[0.002063ms] org.apache.coyote.ErrorState:isError() #849
        +---[0.002504ms] org.apache.coyote.http11.Http11Processor:isAsync() #853
        +---[0.009808ms] org.apache.coyote.Request:updateCounters() #854
        +---[0.002008ms] org.apache.coyote.http11.Http11Processor:getErrorState() #855
        +---[0.002192ms] org.apache.coyote.ErrorState:isIoAllowed() #855
        +---[0.01968ms] org.apache.coyote.http11.Http11InputBuffer:nextRequest() #856
        +---[0.010065ms] org.apache.coyote.http11.Http11OutputBuffer:nextRequest() #857
        +---[0.002576ms] org.apache.coyote.RequestInfo:setStage() #870
        +---[0.016599ms] org.apache.coyote.http11.Http11Processor:processSendfile() #872
        +---[0.008182ms] org.apache.coyote.http11.Http11InputBuffer:getParsingRequestLinePhase() #688
        +---[0.0075ms] org.apache.coyote.http11.Http11Processor:handleIncompleteRequestLineRead() #690
        +---[0.001979ms] org.apache.coyote.RequestInfo:setStage() #875
        +---[0.001981ms] org.apache.coyote.http11.Http11Processor:getErrorState() #877
        +---[0.001934ms] org.apache.coyote.ErrorState:isError() #877
        +---[0.001995ms] org.apache.tomcat.util.net.AbstractEndpoint:isPaused() #877
        +---[0.002403ms] org.apache.coyote.http11.Http11Processor:isAsync() #879
        `---[0.006176ms] org.apache.coyote.http11.Http11Processor:isUpgrade() #881
```

日志里有一个129ms的耗时点（时间比没开arthas的时候更长是因为arthas本身带来的性能消耗，所以生产环境小心使用），这个就是要找的问题点。

打问题点找到了，那怎么定位是什么导致的问题呢，又如何解决呢？

继续trace吧，细化到具体的代码块或者内容。trace由于性能考虑，不会展示所有的调用路径，如果调用路径过深，只有手动深入trace，原则就是trace耗时长的那个方法：

```
[arthas@24851]$ trace org.apache.coyote.Adapter service
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 608 ms.
`---ts=2019-09-14 21:34:33;thread_name=http-nio-7744-exec-1;id=10;is_daemon=true;priority=5;TCCL=org.springframework.boot.loader.LaunchedURLClassLoader@20ad9418
    `---[81.70999ms] org.apache.catalina.connector.CoyoteAdapter:service()
        +---[0.032546ms] org.apache.coyote.Request:getNote() #302
        +---[0.007148ms] org.apache.coyote.Response:getNote() #303
        +---[0.007475ms] org.apache.catalina.connector.Connector:getXpoweredBy() #324
        +---[0.00447ms] org.apache.coyote.Request:getRequestProcessor() #331
        +---[0.007902ms] java.lang.ThreadLocal:get() #331
        +---[0.006522ms] org.apache.coyote.RequestInfo:setWorkerThreadName() #331
        +---[73.793798ms] org.apache.catalina.connector.CoyoteAdapter:postParseRequest() #336
        +---[0.001536ms] org.apache.catalina.connector.Connector:getService() #339
        +---[0.004469ms] org.apache.catalina.Service:getContainer() #339
        +---[0.007074ms] org.apache.catalina.Engine:getPipeline() #339
        +---[0.004334ms] org.apache.catalina.Pipeline:isAsyncSupported() #339
        +---[0.002466ms] org.apache.catalina.connector.Request:setAsyncSupported() #339
        +---[6.01E-4ms] org.apache.catalina.connector.Connector:getService() #342
        +---[0.001859ms] org.apache.catalina.Service:getContainer() #342
        +---[9.65E-4ms] org.apache.catalina.Engine:getPipeline() #342
        +---[0.005231ms] org.apache.catalina.Pipeline:getFirst() #342
        +---[7.239154ms] org.apache.catalina.Valve:invoke() #342
        +---[0.006904ms] org.apache.catalina.connector.Request:isAsync() #345
        +---[0.00509ms] org.apache.catalina.connector.Request:finishRequest() #372
        +---[0.051461ms] org.apache.catalina.connector.Response:finishResponse() #373
        +---[0.007244ms] java.util.concurrent.atomic.AtomicBoolean:<init>() #379
        +---[0.007314ms] org.apache.coyote.Response:action() #380
        +---[0.004518ms] org.apache.catalina.connector.Request:isAsyncCompleting() #382
        +---[0.001072ms] org.apache.catalina.connector.Request:getContext() #394
        +---[0.007166ms] java.lang.System:currentTimeMillis() #401
        +---[0.004367ms] org.apache.coyote.Request:getStartTime() #401
        +---[0.011483ms] org.apache.catalina.Context:logAccess() #401
        +---[0.0014ms] org.apache.coyote.Request:getRequestProcessor() #406
        +---[min=8.0E-4ms,max=9.22E-4ms,total=0.001722ms,count=2] java.lang.Integer:<init>() #406
        +---[0.001082ms] java.lang.reflect.Method:invoke() #406
        +---[0.001851ms] org.apache.coyote.RequestInfo:setWorkerThreadName() #406
        +---[0.035805ms] org.apache.catalina.connector.Request:recycle() #410
        `---[0.007849ms] org.apache.catalina.connector.Response:recycle() #411
```

一段无聊的手动深入trace之后………………

```
[arthas@24851]$ trace org.apache.catalina.webresources.AbstractArchiveResourceSet getArchiveEntries
Press Q or Ctrl+C to abort.
Affect(class-cnt:4 , method-cnt:2) cost in 150 ms.
`---ts=2019-09-14 21:36:26;thread_name=http-nio-7744-exec-3;id=12;is_daemon=true;priority=5;TCCL=org.springframework.boot.loader.LaunchedURLClassLoader@20ad9418
    `---[75.743681ms] org.apache.catalina.webresources.JarWarResourceSet:getArchiveEntries()
        +---[0.025731ms] java.util.HashMap:<init>() #106
        +---[0.097729ms] org.apache.catalina.webresources.JarWarResourceSet:openJarFile() #109
        +---[0.091037ms] java.util.jar.JarFile:getJarEntry() #110
        +---[0.096325ms] java.util.jar.JarFile:getInputStream() #111
        +---[0.451916ms] org.apache.catalina.webresources.TomcatJarInputStream:<init>() #113
        +---[min=0.001175ms,max=0.001176ms,total=0.002351ms,count=2] java.lang.Integer:<init>() #114
        +---[0.00104ms] java.lang.reflect.Method:invoke() #114
        +---[0.045105ms] org.apache.catalina.webresources.TomcatJarInputStream:getNextJarEntry() #114
        +---[min=5.02E-4ms,max=0.008531ms,total=0.028864ms,count=31] java.util.jar.JarEntry:getName() #116
        +---[min=5.39E-4ms,max=0.022805ms,total=0.054647ms,count=31] java.util.HashMap:put() #116
        +---[min=0.004452ms,max=34.479307ms,total=74.206249ms,count=31] org.apache.catalina.webresources.TomcatJarInputStream:getNextJarEntry() #117
        +---[0.018358ms] org.apache.catalina.webresources.TomcatJarInputStream:getManifest() #119
        +---[0.006429ms] org.apache.catalina.webresources.JarWarResourceSet:setManifest() #120
        +---[0.010904ms] org.apache.tomcat.util.compat.JreCompat:isJre9Available() #121
        +---[0.003307ms] org.apache.catalina.webresources.TomcatJarInputStream:getMetaInfEntry() #133
        +---[5.5E-4ms] java.util.jar.JarEntry:getName() #135
        +---[6.42E-4ms] java.util.HashMap:put() #135
        +---[0.001981ms] org.apache.catalina.webresources.TomcatJarInputStream:getManifestEntry() #137
        +---[0.064484ms] org.apache.catalina.webresources.TomcatJarInputStream:close() #141
        +---[0.007961ms] org.apache.catalina.webresources.JarWarResourceSet:closeJarFile() #151
        `---[0.004643ms] java.io.InputStream:close() #155
```

发现了一个值得暂停思考的点：

```
+---[min=0.004452ms,max=34.479307ms,total=74.206249ms,count=31] org.apache.catalina.webresources.TomcatJarInputStream:getNextJarEntry() #117
```

这行代码加载了31次，一共耗时74ms；从名字上看，应该是tomcat加载jar包时的耗时，那么是加载了31个jar包的耗时，还是加载了jar包内的某些资源31次耗时呢？

TomcatJarInputStream这个类源码的注释写到：

```
The purpose of this sub-class is to obtain references to the JarEntry objects for META-INF/ and META-INF/MANIFEST.MF that are otherwise swallowed by the JarInputStream implementation.
```

大概意思也就是，获取jar包内META-INF/，META-INF/MANIFEST的资源，这是一个子类，更多的功能在父类JarInputStream里。

其实看到这里大概也能猜到问题了，tomcat加载jar包内META-INF/，META-INF/MANIFEST的资源导致的耗时，至于为什么连续请求不会耗时，应该是tomcat的缓存机制（下面介绍源码分析）

不着急定位问题，试着通过Arthas最终定位问题细节，继续手动深入trace

```
[arthas@24851]$ trace org.apache.catalina.webresources.TomcatJarInputStream *
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:4) cost in 44 ms.
`---ts=2019-09-14 21:37:47;thread_name=http-nio-7744-exec-5;id=14;is_daemon=true;priority=5;TCCL=org.springframework.boot.loader.LaunchedURLClassLoader@20ad9418
    `---[0.234952ms] org.apache.catalina.webresources.TomcatJarInputStream:createZipEntry()
        +---[0.039455ms] java.util.jar.JarInputStream:createZipEntry() #43
        `---[0.007827ms] java.lang.String:equals() #44

`---ts=2019-09-14 21:37:47;thread_name=http-nio-7744-exec-5;id=14;is_daemon=true;priority=5;TCCL=org.springframework.boot.loader.LaunchedURLClassLoader@20ad9418
    `---[0.050222ms] org.apache.catalina.webresources.TomcatJarInputStream:createZipEntry()
        +---[0.001889ms] java.util.jar.JarInputStream:createZipEntry() #43
        `---[0.001643ms] java.lang.String:equals() #46
#这里一共31个trace日志，删减了剩下的
```

从方法名上看，还是加载资源之类的意思。都已经到jdk源码了，这时候来看一下`TomcatJarInputStream`这个类的源码:

```
/**
 * Creates a new <code>JarEntry</code> (<code>ZipEntry</code>) for the
 * specified JAR file entry name. The manifest attributes of
 * the specified JAR file entry name will be copied to the new
 * <CODE>JarEntry</CODE>.
 *
 * @param name the name of the JAR/ZIP file entry
 * @return the <code>JarEntry</code> object just created
 */
protected ZipEntry createZipEntry(String name) {
    JarEntry e = new JarEntry(name);
    if (man != null) {
        e.attr = man.getAttributes(name);
    }
    return e;
}
```

这个`createZipEntry`有个name参数，从注释上看，是jar/zip文件名，如果能得到文件名这种关键信息，就可以直接定位问题了；还是通过Arthas，使用watch命令，动态监测方法调用数据

**watch方法执行数据观测**

> 让你能方便的观察到指定方法的调用情况。能观察到的范围为：返回值、抛出异常、入参，通过编写 OGNL 表达式进行对应变量的查看。

watch 该方法的入参

```
[arthas@24851]$ watch  org.apache.catalina.webresources.TomcatJarInputStream createZipEntry "{params[0]}"
Press Q or Ctrl+C to abort.
Affect(class-cnt:1 , method-cnt:1) cost in 27 ms.
ts=2019-09-14 21:51:14; [cost=0.14547ms] result=@ArrayList[
    @String[META-INF/],
]
ts=2019-09-14 21:51:14; [cost=0.048028ms] result=@ArrayList[
    @String[META-INF/MANIFEST.MF],
]
ts=2019-09-14 21:51:14; [cost=0.046071ms] result=@ArrayList[
    @String[META-INF/resources/],
]
ts=2019-09-14 21:51:14; [cost=0.033855ms] result=@ArrayList[
    @String[META-INF/resources/swagger-ui.html],
]
ts=2019-09-14 21:51:14; [cost=0.039138ms] result=@ArrayList[
    @String[META-INF/resources/webjars/],
]
ts=2019-09-14 21:51:14; [cost=0.033701ms] result=@ArrayList[
    @String[META-INF/resources/webjars/springfox-swagger-ui/],
]
ts=2019-09-14 21:51:14; [cost=0.033644ms] result=@ArrayList[
    @String[META-INF/resources/webjars/springfox-swagger-ui/favicon-16x16.png],
]
ts=2019-09-14 21:51:14; [cost=0.033976ms] result=@ArrayList[
    @String[META-INF/resources/webjars/springfox-swagger-ui/springfox.css],
]
ts=2019-09-14 21:51:14; [cost=0.032818ms] result=@ArrayList[
    @String[META-INF/resources/webjars/springfox-swagger-ui/swagger-ui-standalone-preset.js.map],
]
ts=2019-09-14 21:51:14; [cost=0.04651ms] result=@ArrayList[
    @String[META-INF/resources/webjars/springfox-swagger-ui/swagger-ui.css],
]
ts=2019-09-14 21:51:14; [cost=0.034793ms] result=@ArrayList[
    @String[META-INF/resources/webjars/springfox-swagger-ui/swagger-ui.js.map],
```

这下直接看到了具体加载的资源名，这么熟悉的名字：swagger-ui，一个国外的rest接口文档工具，又有国内开发者基于swagger-ui做了一套spring mvc的集成工具，通过注解就可以自动生成swagger-ui需要的接口定义json文件，用起来还比较方便，就是侵入性较强。

删除swagger的jar包后问题，诡异的70+ms就消失了

```
<!--pom 里删除这两个引用，这两个包时国内开发者封装的，swagger-ui并没有提供java spring-mvc的支持包，swagger只是一个浏览器端的ui+editor -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.9.2</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.9.2</version>
</dependency>
```

那么为什么swagger会导致请求耗时呢，为什么每次请求偶读会加载swagger内部的静态资源呢？

其实这是tomcat-embed的一个bug吧，下面详细介绍一下该Bug

#### 6.1.4.Tomcat embed Bug分析&解决

源码分析过程实在太漫长，而且也不是本文的重点，所以就不介绍了， 下面直接介绍下分析结果

顺便贴一张tomcat处理请求的核心类图

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424183847.webp)图片

##### 6.1.4.1.为什么每次请求会加载Jar包内的静态资源

关键在于`org.apache.catalina.mapper.Mapper#internalMapWrapper`这个方法，该版本下处理请求的方式有问题，导致每次都校验静态资源。

##### 6.1.4.2.为什么连续请求不会出现问题

因为Tomcat对于这种静态资源的解析是有缓存的，优先从缓存查找，缓存过期后再重新解析。具体参考`org.apache.catalina.webresources.Cache`，默认过期时间ttl是5000ms。

##### 6.1.4.3.为什么本地不会复现

其实确切的说，是通过spring-boot打包插件后不能复现。由于启动方式的不同，tomcat使用了不同的类去处理静态资源，所以没问题

##### 6.1.4.4.如何解决

升级tomcat-embed版本即可

当前出现Bug的版本为：

spring-boot:2.0.2.RELEASE，内置的tomcat embed版本为8.5.31

升级tomcat embed版本至8.5.40+即可解决此问题，新版本已经修复了

**通过替换springboot pom properties方式**

如果项目是maven是继承的springboot，即parent配置为springboot的，或者dependencyManagement中import spring boot包的

```
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.0.2.RELEASE</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

pom中直接覆盖properties即可：

```
<properties>
    <tomcat.version>8.5.40</tomcat.version>
</properties>
```

##### 6.1.4.5.升级spring boot版本

springboot 2.1.0.RELEASE中的tomcat embed版本已经大于8.5.31了，所以直接将springboot升级至该版本及以上版本就可以解决此问题