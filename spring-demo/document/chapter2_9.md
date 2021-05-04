[toc]



# Sprint Boot 常用工具

## 1.官方热部署神器-`spring-boot-devtools`

### 1.1.`spring-boot-devtools`简介

SpringBoot官方开发工具，如果你的应用集成了它，即可实现热部署和远程调试。

### 1.2.实现原理

使用该工具应用为什么启动更快了？主要是因为它使用了两种不同的类加载器。基础类加载器用于加载不会改变的类（比如第三方库中的类），重启类加载器用于加载你应用程序中的类。当应用程序启动时，重启类加载器中的类将会被替换掉，这就意味着重启将比冷启动更快！

### 1.3.热部署

> 接下来我们将集成devtools，来演示下热部署功能。

- 首先需要在项目的`pom.xml`文件中，添加devtools的依赖；

    ```
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <optional>true</optional>
    </dependency>
    ```

- 为了方便测试，我们在项目中添加了如下测试接口；

    ```
    /**
     * Created by macro on 2021/3/25.
     */
    @Api(tags = "TestController", description = "SpringBoot Dev Tools测试")
    @Controller
    @RequestMapping("/test")
    public class TestController {

        @ApiOperation("测试修改")
        @RequestMapping(value = "/first", method = RequestMethod.GET)
        @ResponseBody
        public CommonResult first() {
            String message = "返回消息";
            return CommonResult.success(null,message);
        }
    }
    ```

- 然后启动项目，启动成功后通过Swagger访问接口，返回结果如下，访问地址：http://localhost:8088/swagger-ui.html

    ```
    {
      "code": 200,
      "message": "返回消息",
      "data": null
    }
    ```

- 由于在项目构建时，devtools才会自动重启项目，而IDEA默认并没有使用自动构建，此时我们可以修改应用启动配置，设置当IDEA失去焦点时自动构建项目；

    ![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504102516.png)

- 修改Controller中的代码，只要修改下`message`变量即可；

    ```
    /**
     * Created by macro on 2021/3/25.
     */
    @Api(tags = "TestController", description = "SpringBoot Dev Tools测试")
    @Controller
    @RequestMapping("/test")
    public class TestController {

        @ApiOperation("测试修改")
        @RequestMapping(value = "/first", method = RequestMethod.GET)
        @ResponseBody
        public CommonResult first() {
            String message = "返回消息（已修改）";
            return CommonResult.success(null,message);
        }
    }
    ```

- 失去焦点后，等待项目自动构建，此时访问接口出现404问题；

    ```
    {
      "timestamp": "2021-03-29T07:09:05.415+00:00",
      "status": 404,
      "error": "Not Found",
      "message": "No message available",
      "path": "/test/first"
    }
    ```

- 由于devtools检测时间和IDEA的编译所需时间存在差异，当IDEA还没编译完成，devtools就已经重启应用了，导致了这个问题，修改`application.yml`配置文件，添加如下配置即可；

    ```
    spring:
      devtools:
        restart:
          poll-interval: 2s
          quiet-period: 1s
    ```

- 此时再次访问测试接口，显示内容如下，修改后的代码已经被自动应用了。

    ```
    {
      "code": 200,
      "message": "返回消息（已修改）",
      "data": null
    }
    ```

### 1.4.远程调试

> devtools除了支持热部署之外，还支持远程调试，接下来我们把应用部署到Docker容器中，然后试试远程调试！

- 由于SpringBoot默认打包不会包含devtools，所以我们需要先修改下`pom.xml`；

    ```
    <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
            <!--打包时不排除Devtools-->
            <excludeDevtools>false</excludeDevtools>
        </configuration>
    </plugin>
    ```

- 接下来需要`application.yml`文件，添加devtools的远程访问密码；

    ```
    spring:
      devtools:
        remote:
          secret: macro666
    ```

- 接下来把项目打包成Docker镜像，然后使用如下命令运行起来；

    ```
    docker run -p 8088:8088 --name mall-tiny-devtools \
    --link mysql:db \
    -v /etc/localtime:/etc/localtime \
    -v /mydata/app/mall-tiny/logs:/var/logs \
    -d mall-tiny/mall-tiny-devtools:1.0-SNAPSHOT
    ```

- 添加一个启动配置，修改启动类为`org.springframework.boot.devtools.RemoteSpringApplication`，配置信息具体如下；

    ![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504102526.png)

- 启动该配置，控制台输出如下结果表示远程连接成功；

    ```
    2021-03-29 15:49:50.991  INFO 7848 --- [           main] o.s.b.devtools.RemoteSpringApplication   : Starting RemoteSpringApplication v2.3.0.RELEASE on DESKTOP-5NIMJ19 with PID 7848
    2021-03-29 15:49:51.003  INFO 7848 --- [           main] o.s.b.devtools.RemoteSpringApplication   : No active profile set, falling back to default profiles: default
    2021-03-29 15:49:51.664  WARN 7848 --- [           main] o.s.b.d.r.c.RemoteClientConfiguration    : The connection to http://192.168.5.78:8088 is insecure. You should use a URL starting with 'https://'.
    2021-03-29 15:49:52.024  INFO 7848 --- [           main] o.s.b.d.a.OptionalLiveReloadServer       : LiveReload server is running on port 35729
    2021-03-29 15:49:52.055  INFO 7848 --- [           main] o.s.b.devtools.RemoteSpringApplication   : Started RemoteSpringApplication in 2.52 seconds (JVM running for 4.236)
    ```

- 接下来我们再次修改下Controller中的测试代码，只要修改下`message`变量即可；

    ```
    /**
     * Created by macro on 2021/3/25.
     */
    @Api(tags = "TestController", description = "SpringBoot Dev Tools测试")
    @Controller
    @RequestMapping("/test")
    public class TestController {

        @ApiOperation("测试修改")
        @RequestMapping(value = "/first", method = RequestMethod.GET)
        @ResponseBody
        public CommonResult first() {
            String message = "返回消息（远程调试）";
            return CommonResult.success(null,message);
        }
    }
    ```

- 远程调试如果自动构建的话会导致远程服务频繁重启，此时我们可以使用IDEA手动构建，在项目的右键菜单中可以找到构建按钮；

	![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504102540.png)

- 构建成功后可以发现远程服务会自动重启，并应用修改后的代码，访问测试接口返回如下信息；

    ```
    {
      "code": 200,
      "message": "返回消息（远程调试）",
      "data": null
    }
    ```

### 1.5.总结

虽说使用SpringBoot官方的devtools可以进行热部署，但是这种方式更像是热重启，如果你想要更快的热部署体验的话可以使用JRebel。