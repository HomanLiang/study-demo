[toc]



# Spring Boot 自动装配原理

## 1.自动装配原理

先看看 `SpringBoot` 的主配置类：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235735.png)

里面有一个 `main` 方法运行了一个 `run()` 方法，在 `run` 方法中必须要传入一个被 `@SpringBootApplication` 注解的类。

`SpringBoot` 应用标注在某个类上说明这个类是 `SpringBoot` 的主配置类，`SpringBoot` 就会运行这个类的 `main` 方法来启动`SpringBoot` 项目。

那 `@SpringBootApplication` 注解到底是什么呢，点进去看看：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235824.png)

发现 `@SpringBootApplication` 是一个组合注解。

先看看 `@SpringBootConfiguration` 注解：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235844.png)

这个注解很简单，表明该类是一个 `Spring` 的配置类。

再进去看看 `@Configuration`：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235855.png)

说明 `Spring` 的配置类也是 `Spring` 的一个组件。

`@EnableAutoConfiguration` 这个注解是开启自动配置的功能。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235920.png)

先看看 `@AutoConfigurationPackage` 注解：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235932.png)

这个注解是自动配置包，主要是使用的 `@Import` 来给 `Spring` 容器中导入一个组件 ，这里导入的是 `Registrar.class`。

来看下这个 `Registrar`：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235941.png)

就是通过这个方法获取扫描的包路径，可以 `debug` 看看：

在这行代码上打了一个断点：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235952.png)

启动项目：

进入断点处：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000003.png)

看看能否获取扫描的包路径：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000013.png)

已经获取到了包路径：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000027.png)

那那个 `metadata` 是什么呢：

可以看到是标注在 `@SpringBootApplication` 注解上的 `DemosbApplication`，也就是我们的主配置类：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000038.png)

**说白了就是将主配置类（即@SpringBootApplication标注的类）的所在包及子包里面所有组件扫描加载到Spring容器。所以包名一定要注意。**

现在包扫描路径获取到了，那具体加载哪些组件呢，看看下面这个注解。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000050.png)

`@Import` 注解就是给 `Spring` 容器中导入一些组件，这里传入了一个组件的选择器: `AutoConfigurationImportSelector`。

里面有一个 `selectImports` 方法，将所有需要导入的组件以全类名的方式返回；这些组件就会被添加到容器中。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000101.png)

debug运行看看：

会给容器中导入非常多的自动配置类（xxxAutoConfiguration）；就是给容器中导入这个场景需要的所有组件，并配置好这些组件:

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000113.png)

有了自动配置类，免去了我们手动编写配置注入功能组件等的工作。

那他是如何获取到这些配置类的呢，看看上面这个方法：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000129.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000139.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000152.png)

会从 `META-INF/spring.factories` 中获取资源，然后通过 `Properties` 加载资源：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000213.png)

`Spring Boot` 在启动的时候从类路径下的 `META-INF/spring.factories` 中获取 `EnableAutoConfiguration` 指定的值，将这些值作为自动配置类导入到容器中，自动配置类就生效，帮我们进行自动配置工作。以前我们需要自己配置的东西，自动配置类都帮我们完成了。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000227.png)

`J2EE` 的整体整合解决方案和自动配置都在 `spring-boot-autoconfigure-2.0.3.RELEASE.jar`：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000240.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000248.png)

比如看看 `WebMvcAutoConfiguration`：

都已经帮我们配置好了，我们不用再单独配置了：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210402000300.png)



## 2.在SpringBoot初始化时搞点事情

### 2.1.容器刷新完成扩展点

#### 2.1.1.监听容器刷新完成扩展点`ApplicationListener<ContextRefreshedEvent>`

##### 2.1.1.1.基本用法

熟悉`Spring`的同学一定知道，容器刷新成功意味着所有的`Bean`初始化已经完成，当容器刷新之后`Spring`将会调用容器内所有实现了`ApplicationListener<ContextRefreshedEvent>`的`Bean`的`onApplicationEvent`方法，应用程序可以以此达到监听容器初始化完成事件的目的。

```
@Component
public class StartupApplicationListenerExample implements 
  ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG 
      = Logger.getLogger(StartupApplicationListenerExample.class);

    public static int counter;

    @Override public void onApplicationEvent(ContextRefreshedEvent event) {
        LOG.info("Increment counter");
        counter++;
    }
}
```

##### 2.1.1.2.易错的点

这个扩展点用在`web`容器中的时候需要额外注意，在web 项目中（例如`spring mvc`），系统会存在两个容器，一个是`root application context`,另一个就是我们自己的`context`（作为`root application context`的子容器）。如果按照上面这种写法，就会造成`onApplicationEvent`方法被执行两次。解决此问题的方法如下：

```
@Component
public class StartupApplicationListenerExample implements 
  ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG 
      = Logger.getLogger(StartupApplicationListenerExample.class);

    public static int counter;

    @Override public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            // root application context 没有parent
            LOG.info("Increment counter");
            counter++;
        }
    }
}
```

##### 2.1.1.3.高阶玩法

当然这个扩展还可以有更高阶的玩法：**自定义事件**，可以借助`Spring`以最小成本实现一个观察者模式：

- 先自定义一个事件：

    ```
    public class NotifyEvent extends ApplicationEvent {
        private String email;
        private String content;
        public NotifyEvent(Object source) {
            super(source);
        }
        public NotifyEvent(Object source, String email, String content) {
            super(source);
            this.email = email;
            this.content = content;
        }
        // 省略getter/setter方法
    }
    ```

- 注册一个事件监听器

    ```
    @Component
    public class NotifyListener implements ApplicationListener<NotifyEvent> {

        @Override
        public void onApplicationEvent(NotifyEvent event) {
            System.out.println("邮件地址：" + event.getEmail());
            System.out.println("邮件内容：" + event.getContent());
        }
    }
    ```

- 发布事件

    ```
    @RunWith(SpringRunner.class)
    @SpringBootTest
    public class ListenerTest {
        @Autowired
        private WebApplicationContext webApplicationContext;

        @Test
        public void testListener() {
            NotifyEvent event = new NotifyEvent("object", "abc@qq.com", "This is the content");
            webApplicationContext.publishEvent(event);
        }
    }
    ```

- 执行单元测试可以看到邮件的地址和内容都被打印出来了

#### 2.1.2.`SpringBoot`的`CommandLineRunner`接口

当容器上下文初始化完成之后，`SpringBoot`也会调用所有实现了`CommandLineRunner`接口的`run`方法，下面这段代码可起到和上文同样的作用：

```
@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    private static final Logger LOG =
      LoggerFactory.getLogger(CommandLineAppStartupRunner.class);

    public static int counter;

    @Override
    public void run(String...args) throws Exception {
        LOG.info("Increment counter");
        counter++;
    }
}
```

对于这个扩展点的使用有额外两点需要注意：

- 多个实现了`CommandLineRunner`的`Bean`的执行顺序可以根据`Bean`上的`@Order`注解调整
- 其`run`方法可以接受从控制台输入的参数，跟`ApplicationListener<ContextRefreshedEvent>`这种扩展相比，更加灵活

```
// 从控制台输入参数示例
java -jar CommandLineAppStartupRunner.jar abc abcd
```

#### 2.1.3.`SpringBoot`的`ApplicationRunner`接口

这个扩展和`SpringBoot`的`CommandLineRunner`接口的扩展类似，只不过接受的参数是一个`ApplicationArguments`类，对控制台输入的参数提供了更好的封装，以`--`开头的被视为带选项的参数，否则是普通的参数

```
@Component
public class AppStartupRunner implements ApplicationRunner {
    private static final Logger LOG =
      LoggerFactory.getLogger(AppStartupRunner.class);

    public static int counter;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOG.info("Application started with option names : {}", 
          args.getOptionNames());
        LOG.info("Increment counter");
        counter++;
    }
}
```

比如：

```
java -jar CommandLineAppStartupRunner.jar abc abcd --autho=mark verbose
```

### 2.2.`Bean`初始化完成扩展点

前面的内容总结了针对容器初始化的扩展点，在有些场景，比如监听消息的时候，我们希望`Bean`初始化完成之后立刻注册监听器，而不是等到整个容器刷新完成，`Spring`针对这种场景同样留足了扩展点：

#### 2.2.1.`@PostConstruct`注解

`@PostConstruct`注解一般放在`Bean`的方法上，被`@PostConstruct`修饰的方法会在`Bean`初始化后马上调用：

```
@Component
public class PostConstructExampleBean {

    private static final Logger LOG 
      = Logger.getLogger(PostConstructExampleBean.class);

    @Autowired
    private Environment environment;

    @PostConstruct
    public void init() {
        LOG.info(Arrays.asList(environment.getDefaultProfiles()));
    }
}
```

#### 2.2.2.`InitializingBean`接口

`InitializingBean`的用法基本上与`@PostConstruct`一致，只不过相应的`Bean`需要实现`afterPropertiesSet`方法

```
@Component
public class InitializingBeanExampleBean implements InitializingBean {

    private static final Logger LOG 
      = Logger.getLogger(InitializingBeanExampleBean.class);

    @Autowired
    private Environment environment;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info(Arrays.asList(environment.getDefaultProfiles()));
    }
}
```

#### 2.2.3.`@Bean`注解的初始化方法

通过`@Bean`注入`Bean`的时候可以指定初始化方法：

**`Bean`的定义**

```
public class InitMethodExampleBean {

    private static final Logger LOG = Logger.getLogger(InitMethodExampleBean.class);

    @Autowired
    private Environment environment;

    public void init() {
        LOG.info(Arrays.asList(environment.getDefaultProfiles()));
    }
}
```

**`Bean`注入**

```
@Bean(initMethod="init")
public InitMethodExampleBean initMethodExampleBean() {
    return new InitMethodExampleBean();
}
```

#### 2.2.4.通过构造函数注入

`Spring`也支持通过构造函数注入，我们可以把搞事情的代码写在构造函数中，同样能达到目的

```
@Component 
public class LogicInConstructorExampleBean {

    private static final Logger LOG 
      = Logger.getLogger(LogicInConstructorExampleBean.class);

    private final Environment environment;

    @Autowired
    public LogicInConstructorExampleBean(Environment environment) {
        this.environment = environment;
        LOG.info(Arrays.asList(environment.getDefaultProfiles()));
    }
}
```

#### 2.2.5.`Bean`初始化完成扩展点执行顺序？

可以用一个简单的测试：

```
@Component
@Scope(value = "prototype")
public class AllStrategiesExampleBean implements InitializingBean {

    private static final Logger LOG 
      = Logger.getLogger(AllStrategiesExampleBean.class);

    public AllStrategiesExampleBean() {
        LOG.info("Constructor");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.info("InitializingBean");
    }

    @PostConstruct
    public void postConstruct() {
        LOG.info("PostConstruct");
    }

    public void init() {
        LOG.info("init-method");
    }
}
```

实例化这个`Bean`后输出：

```
[main] INFO o.b.startup.AllStrategiesExampleBean - Constructor
[main] INFO o.b.startup.AllStrategiesExampleBean - PostConstruct
[main] INFO o.b.startup.AllStrategiesExampleBean - InitializingBean
[main] INFO o.b.startup.AllStrategiesExampleBean - init-method
```









