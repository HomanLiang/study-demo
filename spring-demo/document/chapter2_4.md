[toc]



# Spring Boot 配置文件

## 1.多环境配置

### 1.1.前言

日常开发中至少有三个环境，分别是开发环境（`dev`），测试环境（`test`），生产环境（`prod`）。

不同的环境的各种配置都不相同，比如数据库，端口，`IP`地址等信息。

那么这么多环境如何区分，如何打包呢？

本篇文章就来介绍一下`Spring Boot` 中多环境如何配置，如何打包。

### 1.2.Spring Boot 自带的多环境配置

Spring Boot 对多环境整合已经有了很好的支持，能够在打包，运行间自由切换环境。

那么如何配置呢？下面将会逐步介绍。

#### 1.2.1.创建不同环境的配置文件

既然每个环境的配置都不相同，索性将不同环境的配置放在不同的配置文件中，因此需要创建三个不同的配置文件，分别是`application-dev.properties`、`application-test.properties`、`application-prod.properties`。

**「注意」**：配置文件的名称一定要是`application-name.properties`或者`application-name.yml`格式。这个`name`可以自定义，主要用于区分。

> 此时整个项目中就有四个配置文件，加上`application.properties`。

#### 1.2.2.指定运行的环境

虽然你创建了各个环境的配置文件，但是`Spring Boot` 仍然不知道你要运行哪个环境，有以下两种方式指定：

**1.2.2.1.配置文件中指定**

在`application.properties`或者`application.yml`文件中指定，内容如下：

```
# 指定运行环境为测试环境
spring.profiles.active=test
```

以上配置有什么作用呢？

如果没有指定运行的环境，`Spring Boot` 默认会加载`application.properties`文件，而这个的文件又告诉`Spring Boot` 去找`test`环境的配置文件。

**1.2.2.2.运行 jar 的时候指定**

`Spring Boot` 内置的环境切换能够在运行`Jar`包的时候指定环境，命令如下：

```
java -jar xxx.jar --spring.profiles.active=test
```

以上命令指定了运行的环境是`test`，是不是很方便呢？

### 1.3.Maven 的多环境配置

`Maven`本身也提供了对多环境的支持，不仅仅支持`Spring Boot`项目，只要是基于`Maven`的项目都可以配置。

`Maven`对于多环境的支持在功能方面更加强大，支持`JDK版本`、`资源文件`、`操作系统`等等因素来选择环境。

如何配置呢？下面逐一介绍。

#### 1.3.1.创建多环境配置文件

创建不同环境的配置文件，分别是`application-dev.properties`、`application-test.properties`、`application-prod.properties`。

加上默认的配置文件`application.properties`同样是四个配置文件。

#### 1.3.2.定义激活的变量

需要将`Maven`激活的环境作用于`Spring Boot`，实际还是利用了`spring.profiles.active`这个属性，只是现在这个属性的取值将是取值于`Maven`。配置如下：

```
spring.profiles.active=@profile.active@
```

> `profile.active`实际上就是一个变量，在`maven`打包的时候指定的`-P test`传入的就是值。

#### 1.3.3.pom 文件中定义 profiles

需要在`maven`的`pom.xml`文件中定义不同环境的`profile`，如下：

```
<!--定义三种开发环境-->
    <profiles>
        <profile>
            <!--不同环境的唯一id-->
            <id>dev</id>
            <activation>
                <!--默认激活开发环境-->
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <!--profile.active对应application.yml中的@profile.active@-->
                <profile.active>dev</profile.active>
            </properties>
        </profile>

        <!--测试环境-->
        <profile>
            <id>test</id>
            <properties>
                <profile.active>test</profile.active>
            </properties>
        </profile>

        <!--生产环境-->
        <profile>
            <id>prod</id>
            <properties>
                <profile.active>prod</profile.active>
            </properties>
        </profile>
    </profiles>
```

标签`<profile.active>`正是对应着配置文件中的`@profile.active@`。

`<activeByDefault>`标签指定了默认激活的环境，则是打包的时候不指定`-P`选项默认选择的环境。

以上配置完成后，将会在IDEA的右侧`Maven`选项卡中出现以下内容：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401231222.png)1

可以选择打包的环境，然后点击`package`即可。

或者在项目的根目录下用命令打包，不过需要使用`-P`指定环境，如下：

```
mvn clean package package -P test
```

`maven`中的`profile`的激活条件还可以根据`jdk`、`操作系统`、`文件存在或者缺失`来激活。这些内容都是在`<activation>`标签中配置，如下：

```
<!--activation用来指定激活方式，可以根据jdk环境，环境变量，文件的存在或缺失-->
  <activation>
       <!--配置默认激活-->
      <activeByDefault>true</activeByDefault>
                
      <!--通过jdk版本-->
      <!--当jdk环境版本为1.8时，此profile被激活-->
      <jdk>1.8</jdk>
      <!--当jdk环境版本1.8或以上时，此profile被激活-->
      <jdk>[1.8,)</jdk>

      <!--根据当前操作系统-->
      <os>
        <name>Windows XP</name>
        <family>Windows</family>
        <arch>x86</arch>
        <version>5.1.2600</version>
      </os>
  </activation>
```

#### 1.3.4.资源过滤

如果你不配置这一步，将会在任何环境下打包都会带上全部的配置文件，但是我们可以配置只保留对应环境下的配置文件，这样安全性更高。

这一步配置很简单，只需要在`pom.xml`文件中指定`<resource>`过滤的条件即可，如下：

```
<build>
  <resources>
  <!--排除配置文件-->
    <resource>
      <directory>src/main/resources</directory>
      <!--先排除所有的配置文件-->
        <excludes>
          <!--使用通配符，当然可以定义多个exclude标签进行排除-->
          <exclude>application*.properties</exclude>
        </excludes>
    </resource>

    <!--根据激活条件引入打包所需的配置和文件-->
    <resource>
      <directory>src/main/resources</directory>
      <!--引入所需环境的配置文件-->
      <filtering>true</filtering>
      <includes>
        <include>application.yml</include>
          <!--根据maven选择环境导入配置文件-->
        <include>application-${profile.active}.yml</include>
      </includes>
    </resource>
  </resources>
</build>
```

上述配置主要分为两个方面，第一是先排除所有配置文件，第二是根据`profile.active`动态的引入配置文件。

#### 1.3.5.总结

至此，`Maven`的多环境打包已经配置完成，相对来说挺简单，既可以在`IDEA`中选择环境打包，也同样支持命令`-P`指定环境打包。

### 1.4.配置文件加载位置

`springboot` 启动会扫描以下位置的 `application.properties` 或者 `application.yml` 文件作为 `Spring boot` 的默认配置文件

- `–file:./config/`

- `–file:./`

- `–classpath:/config/`

- `–classpath:/`

优先级由高到底，高优先级的配置会覆盖低优先级的配置；

`SpringBoot` 会从这四个位置全部加载主配置文件；**互补配置**；

我们还可以通过 `spring.config.location` 来改变默认的配置文件位置

**项目打包好以后，我们可以使用命令行参数的形式，启动项目的时候来指定配置文件的新位置；指定配置文件和默认加载的这些配置文件共同起作用形成互补配置；**

`java -jar spring-boot-02-config-02-0.0.1-SNAPSHOT.jar --spring.config.location=G:/application.properties`

### 1.5.外部配置加载顺序

**SpringBoot也可以从以下位置加载配置； 优先级从高到低；高优先级的配置覆盖低优先级的配置，所有的配置会形成互补配置**

1. 命令行参数

   所有的配置都可以在命令行上进行指定

    ```
   java -jar spring-boot-02-config-02-0.0.1-SNAPSHOT.jar --server.port=8087 --server.context-path=/abc
    ```

	多个配置用空格分开； --配置项=值

2. 来自`java:comp/env`的JNDI属性

3. Java系统属性（`System.getProperties()`）

4. 操作系统环境变量

5. `RandomValuePropertySource`配置的`random.*`属性值

   **由jar包外向jar包内进行寻找；**

   **优先加载带profile**

6. `jar` 包外部的 `application-{profile}.properties` 或 `application.yml` (带 `spring.profile`)配置文件

7. `jar` 包内部的 `application-{profile}.properties` 或 `application.yml` (带 `spring.profile`)配置文件

   **再来加载不带profile**

8. `jar` 包外部的 `application.properties`或 `application.yml`(不带 `spring.profile`)配置文件

9. `jar` 包内部的 `application.properties` 或 `application.yml` (不带 `spring.profile`)配置文件

10. `@Configuration` 注解类上的 `@PropertySource`

11. 通过 `SpringApplication.setDefaultProperties` 指定的默认属性

    所有支持的配置加载来源；

## 2.自定义配置

SpringBoot免除了项目中大部分手动配置，可以说，几乎所有的配置都可以写在全局配置文件application.peroperties中，SpringBoot会自动加载全局配置文件从而免除我们手动加载的烦恼。但是，如果我们自定义了配置文件，那么SpringBoot是无法识别这些配置文件的，此时需要我们手动加载。

接下来，将针对SpringBoot的自定义配置文件及其加载方式进行讲解。

### 2.1.使用@PropertySource加载配置文件

我们可以使用@PropertySource注解结合@Configuration注解配置类的方式来加载自定义配置文件，@PropertySource注解用于指定自定义配置文件的具体位置和名称。同时，为了保证SpringBoot能够扫描该注解，还需要在类上添加@Configuration注解将实体类作为自定义配置类。

如果需要将自定义配置文件中的属性值注入到对应类的属性中，可以使用@ConfigurationProperties注解或者@Value注解进行属性值注入。

操作步骤：

2.1.1.在SpringBoot项目的resources目录下新建一个名为test.properties的自定义配置文件，在该配置文件中编写需要设置的配置属性：

```
# 对实体类对象MyProperties进行属性配置
test.id=1
test.name=test
```

2.1.2.在com.hardy.springboot_demo.pojo包下新建一个配置类MyProperties，提供test.properties自定义配置文件中对应的属性，并根据@PropertySource注解的使用进行相关的配置：

```
package com.hardy.springboot_demo.pojo;

/**
 * @Author: HardyYao
 * @Date: 2021/5/31
 */

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration // 自定义配置类
@PropertySource("classpath:test.properties") // 指定自定义配置文件的文件位置和文件名称
@EnableConfigurationProperties(MyProperties.class) // 开启对应配置类的属性注入功能
@ConfigurationProperties(prefix = "test") // 指定配置文件注入属性前缀
public class MyProperties {

    private int id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "MyProperties{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
```

这里主要是新建一个自定义配置类，通过相关注解引入了自定义的配置文件，并完成了自定义属性值的注入。

针对上述的几个注解，进行逐一讲解：

- @Configuration注解表示当前类是一个自定义配置类，并添加为Spring容器的组件，这里也可以使用传统的@Component注解实现相同的功能；
- @PropertySource("classpath:test.properties")注解指定了定义配置文件的文件位置和文件名称，此处表示自定义配置文件为classpath类路径下的test.properties文件；
- @ConfigurationProperties(prefix = "test")将上述自定义配置文件test.properties中以test开头的属性值注入到该配置类属性中；
- 如果配置类上使用的是@Component注解而非@Configuration注解，那么@EnableConfigurationProperties注解还可以省略。

2.1.3.编写测试方法进行测试：

```
@Autowired
private MyProperties myProperties;
@Test
void myPropertiesTest() {
    System.out.println(myProperties);
}
```

测试结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627121056.png)

### 2.2.使用@Configuration编写自定义配置类

在SpringBoot框架中，推荐使用配置类的方式向容器中添加和配置组件。

在SpringBoot框架中，通常使用@Configuration注解定义一个配置类，SpringBoot会自动扫描和识别配置类，从而替换传统Spring框架中的XML配置文件。

当自定义一个配置类后，还需要在类中的方法上加上@Bean注解进行组件配置，将方法的返回对象注入到Spring容器中，并且组件名称默认使用的是方法名，当然也可以使用@Bean注解的name或value属性自定义组件的名称。

操作步骤：

2.2.1.在com.hardy.springboot_demo包下新建一个config包，并在该包下新建一个MyService类，该类中不需要写任何代码：

```
package com.hardy.springboot_demo.config;

/**
 * @Author: HardyYao
 * @Date: 2021/5/31
 */
public class MyService {
}
```

由于该类目前没有任何配置和注解，因此还无法正常被SpringBoot扫描和识别。

2.2.2.在config包下，新建一个MyConfig类，并使用@Configuration注解将该类声明为一个配置类，该类的内容如下：

```
package com.hardy.springboot_demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: HardyYao
 * @Date: 2021/5/31
 */
@Configuration // 定义该类为一个配置类
public class MyConfig {

    /**
     * 将返回值对象作为组件添加到Spring容器中，该组件id默认为方法名
     * @return
     */
    @Bean
    public MyService myService(){
        return new MyService();
    }

}
```

MyConfig是@Configuration注解声明的配置类（类似于声明了一个XML配置文件），该配置类会被SpringBoot自动扫描识别。

使用@Bean注解的myService()方法，其返回值对象会作为组件添加到Spring容器中（类似于XML配置文件中的标签配置），并且该组件id默认为方法名myService。

2.2.3.编写测试方法进行测试

```
@Autowired
private ApplicationContext applicationContext;
    
@Test
void iocTest() {
    System.out.println(applicationContext.containsBean("myService"));
}
```

上述代码中，先通过@Autowired注解注入Spring容器示例ApplicationContext，然后在测试方法iocTest()中测试查看该容器中是否包含id为myService的组件。

执行测试方法iocTest()，运行结果如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627121208.png)

从测试结果可以看出，测试方法iocTest()运行成功，显示运行结果为true，表示Spirng的IOC容器中也包含了id为myService的实例对象组件，说明使用自定义配置类的形式完成了向Spring容器进行组件的添加和配置。

### 2.3.随机数设置及参数间引用

在SpringBoot配置文件中设置属性时，除了可以像前面示例中显示的配置属性值外，还可以使用**随机值**和**参数间引用**对属性值进行设置。下面，针对配置文件中这两种属性值的设置方式进行讲解。

#### 2.3.1.随机值设置

在SpringBoot配置文件中，随机值设置使用到了SpringBoot内嵌的RandomValuePropertySource类，对一些隐秘属性值或者测试用例属性值进行随机值注入。

随机值设置的语法格式为${random.xx}，xx表示需要制定生成的随机数类型和范围，它可以生成随机的整数、uuid或字符串，示例代码如下：

```
my.secret=${random.value} // 配置随机值
my.number=${random.int} // 配置随机整数
my.bignumber=${random.long} // 配置随机long类型数
my.uuid=${random.uuid} // 配置随机uuid类型数
my.number.less.than.ten=${random.int(10)} // 配置小于10的随机整数
my.number.in.range=${random.int[1024,65536]} // 配置范围在[1024,65536]之间的随机整数
```

上述代码中，使用RandomValuePropertySource类中random提供的随机数类型，分别展示了不同类型随机值的设置示例。

#### 2.3.2.参数间引用

在SpringBoot配置文件中，配置文件的属性值还可以进行参数间的引用，也就是在后一个配置的属性值中直接引用先前定义过的属性，这样就可以直接解析其中的属性值了。

使用参数间引用的好处就是，在多个具有相互关联的配置属性中，只需要对其中一处属性进行预先配置，那么其他地方都可以引用，省去了后续多处修改的麻烦。

参数间引用的语法格式为${xx}，xx表示先前在配置文件中已经配置过的属性名，示例代码如下：

```
app.name=MyApp
app.description=${app.name} is a Spring Boot application
```

上述参数间引用设置示例中，先设置了“app.name=MyApp”，将app.name属性的属性值设置为了MyApp；接着，在app.description属性配置中，使用${app.name}对前一个属性进行了引用。

接下来，通过一个案例来演示使用随机值设置以及参数间引用的方式进行属性设置的具体使用和效果，具体步骤如下：

2.3.2.1.打开全局配置文件application.properties，在该配置文件中分别通过随机值设置和参数间引用来配置两个属性，示例代码如下：

```
# 随机值设置以及参数间引用配置
hardy.age=${random.int[20,30]}
hardy.description=hardy的年龄可能是${hardy.age}
```

在上述application.properties配置文件中，先使用随机数设置了hardy.age的属性值，该属性值的取值范围在[10,20]之间，随后使用参数间引用配置了hardy.description属性。

2.3.2.2.在项目的测试类中添加description属性，并将配置文件中hardy.description的属性进行注入，然后新增一个测试方法进行测试，测试代码如下：

```
@Value("${hardy.description}")
private String description;

@Test
void placeholderTest() {
    System.out.println(description);
}
```

在上述代码中，通过@Value("${hardy.description}")注解将配置文件中的hardy.description属性值注入到了对应的description属性中，在测试方法placeholderTest()中对该属性值进行了输出打印。

执行测试方法后，控制台输出结果如下所示：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627121325.png)

可以看到，测试方法placeholderTest()成功打印出了description属性的注入内容（age的取值始终在[20,30]之间随机显示），该内容与配置文件中配置的属性值保持一致。