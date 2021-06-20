[toc]



# Spring Boot 关键注解

## 1.PropertySource

### 1.1.PropertySource 简介

**org.springframework.context.annotation.PropertySource** 是一个注解，可以标记在类上、接口上、枚举上，在运行时起作用。而`@Repeatable(value = PropertySources.class)` 表示在 `PropertySources` 中此注解时可以重复使用的。如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401212907.png)

### 1.2.@PropertySource与Environment读取配置文件

此注解 `@PropertySource` 为 `Spring` 中的 `Environment` 提供方便和声明机制，通常与 `Configuration` 一起搭配使用。

- 新建一个 `maven` 项目，添加 `pom.xml` 依赖：

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>

        <groupId>com.spring.propertysource</groupId>
        <artifactId>spring-propertysource</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <name>spring-propertysource</name>
        <description>Demo project for Spring Boot</description>

        <properties>
            <java.version>1.8</java.version>
            <spring.version>4.3.13.RELEASE</spring.version>
        </properties>

        <dependencies>

            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-core</artifactId>
                <version>${spring.version}</version>
            </dependency>

            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-context</artifactId>
                <version>${spring.version}</version>
            </dependency>

        </dependencies>

        <build>
            <pluginManagement>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.2</version>
                        <configuration>
                            <source>1.6</source>
                            <target>1.6</target>
                        </configuration>
                    </plugin>
                </plugins>
            </pluginManagement>
        </build>

    </project>
    ```

	> 一般把版本名称统一定义在 标签中，便于统一管理，如上可以通过`${…}` 来获取指定版本。

- 定义一个 `application.properties` 来写入如下配置

    ```properties
    com.spring.name=liuXuan
    com.spring.age=18
    ```

- 新建一个 `TestBean`，定义几个属性

    ```java
    public class TestBean {

        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "TestBean{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }
    }
    ```

- 新建一个 `main class`，用来演示 `@PropertySource` 的使用

    ```java
      @Configuration
      @PropertySource(value = "classpath:application.properties",ignoreResourceNotFound = false)
      public class SpringPropertysourceApplication {

        @Resource
        Environment environment;

        @Bean
        public TestBean testBean(){
          TestBean testBean = new TestBean();
          // 读取application.properties中的name
          testBean.setName(environment.getProperty("com.spring.name"));
          // 读取application.properties中的age
          testBean.setAge(Integer.valueOf(environment.getProperty("com.spring.age")));
          System.out.println("testBean = " + testBean);
          return testBean;
        }

        public static void main(String[] args) {
          ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringPropertysourceApplication.class);
          TestBean testBean = (TestBean)applicationContext.getBean("testBean");

        }
      }
    ```
    
    输出：
    
    ```
    testBean = TestBean{name='liuXuan', age=18}
    Refreshing the spring context
    ```

	> **@Configuration : 相当于 标签，注意不是，一个配置类可以有多个bean，但是只能有一个**
	>
	> **@PropertySource: 用于引入外部属性配置，和Environment 配合一起使用。其中ignoreResourceNotFound 表示没有找到文件是否会报错，默认为false，就是会报错，一般开发情况应该使用默认值，设置为true相当于生吞异常，增加排查问题的复杂性.**
	>
	> 引入PropertySource，注入Environment，然后就能用environment 获取配置文件中的value值。

### 1.3.@PropertySource与@Value读取配置文件

#### 1.3.1.@Value 基本使用

我们以DB的配置文件为例，来看一下如何使用 `@Value` 读取配置文件

- 首先新建一个**DBConnection**，具体代码如下：

    ```java
      // 组件bean
      @Component
      @PropertySource("classpath:db.properties")
      public class DBConnection {

        @Value("${DB_DRIVER_CLASS}")
        private String driverClass;

        @Value("${DB_URL}")
        private String dbUrl;

        @Value("${DB_USERNAME}")
        private String userName;

        @Value("${DB_PASSWORD}")
        private String password;

        public DBConnection(){}

        public void printDBConfigs(){
          System.out.println("Db Driver Class = " + driverClass);
          System.out.println("Db url = " + dbUrl);
          System.out.println("Db username = " + userName);
          System.out.println("Db password = " + password);
        }
      }
    ```

	> 类上加入 `@Component` 表示这是一个组件 `bean`，需要被 `spring` 进行管理，`@PropertySource`  用于获取类路径下的`db.properties` 配置文件，`@Value` 用于获取 `properties` 中的 `key` 对应的 `value` 值，`printDBConfigs` 方法打印出来对应的值。

- 新建一个**db.properties**，具体文件如下

    ```properties
    #MYSQL Database Configurations
    DB_DRIVER_CLASS=com.mysql.jdbc.Driver
    DB_URL=jdbc:mysql://localhost:3306/test
    DB_USERNAME=cxuan
    DB_PASSWORD=111111
    APP_NAME=PropertySourceExample
    ```

	> 这是一个MYSQL连接数据库驱动的配置文件。

- 新建一个**SpringMainClass**，用于测试 `DBConection` 中是否能够获取到 `@Value` 的值

    ```java
            public class SpringMainClass {

            public static void main(String[] args) {
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                // 注解扫描，和@ComponentScan 和 基于XML的配置<context:component-scan base-package>相同
                context.scan("com.spring.propertysource.app");

                // 刷新上下文环境
                context.refresh();
                System.out.println("Refreshing the spring context");

                // 获取DBConnection这个Bean，调用其中的方法
                DBConnection dbConnection = context.getBean(DBConnection.class);
                dbConnection.printDBConfigs();

                // 关闭容器(可以省略，容器可以自动关闭)
                context.close();
            }
        }
    ```

	输出：
	
	```
	Refreshing the spring context
	Db Driver Class = com.mysql.jdbc.Driver
	Db url = jdbc:mysql://localhost:3306/test
	Db username = cxuan
	Db password = 111111
	```

#### 1.3.2.@Value 高级用法

​	在实现了上述的例子之后，我们再来看一下@Value 的高级用法：

- @Value 可以直接为字段赋值，例如:

    ```java
    @Value("cxuan")
    String name;

    @Value(10)
    Integer age;

    @Value("${APP_NAME_NOT_FOUND:Default}")
    private String defaultAppName;
    ```

- @Value 可以直接获取系统属性，例如：

    ```java
    @Value("${java.home}")
    // @Value("#{systemProperties['java.home']}") SPEL 表达式
    String javaHome;

    @Value("${HOME}")
    String dir;
    ```

- @Value 可以注解在方法和参数上

    ```java
    @Value("Test") // 可以直接使用Test 进行单元测试
    public void printValues(String s, @Value("another variable") String v) {
        ... 
    }
    ```

	修改**DBConnection**后的代码如下：

    ```java
    public class DBConnection {

        @Value("${DB_DRIVER_CLASS}")
        private String driverClass;

        @Value("${DB_URL}")
        private String dbUrl;

        @Value("${DB_USERNAME}")
        private String userName;

        @Value("${DB_PASSWORD}")
        private String password;

        public DBConnection(){}

        public void printDBConfigs(){
            System.out.println("Db Driver Class = " + driverClass);
            System.out.println("Db url = " + dbUrl);
            System.out.println("Db username = " + userName);
            System.out.println("Db password = " + password);
        }
    }
    ```

	在 `com.spring.propertysource.app` 下 新增 `DBConfiguration`，作用是配置管理类，管理 `DBConnection`，并读取配置文件，代码如下：

    ```java
    @Configuration
    @PropertySources({
            @PropertySource("classpath:db.properties"),
            @PropertySource(value = "classpath:root.properties", ignoreResourceNotFound = true)
    })
    public class DBConfiguration {

        @Value("Default DBConfiguration")
        private String defaultName;

        @Value("true")
        private boolean defaultBoolean;

        @Value("10")
        private int defaultInt;

        @Value("${APP_NAME_NOT_FOUND:Default}")
        private String defaultAppName;

         @Value("#{systemProperties['java.home']}")
    //    @Value("${java.home}")
        private String javaHome;

        @Value("${HOME}")
        private String homeDir;

        @Bean
        public DBConnection getDBConnection() {
            DBConnection dbConnection = new DBConnection();
            return dbConnection;
        }

        @Value("Test") // 开启测试
        public void printValues(String s, @Value("another variable") String v) {
            System.out.println("Input Argument 1 = " + s);
            System.out.println("Input Argument 2 = " + v);

            System.out.println("Home Directory = " + homeDir);
            System.out.println("Default Configuration Name = " + defaultName);
            System.out.println("Default App Name = " + defaultAppName);
            System.out.println("Java Home = " + javaHome);
            System.out.println("Home dir = " + homeDir);
            System.out.println("Boolean = " + defaultBoolean);
            System.out.println("Int = " + defaultInt);

        }

    }
    ```
    
    使用**SpringMainClass** 进行测试，测试结果如下：
    
    ```
    Input Argument 1 = Test
    Input Argument 2 = another variable
    Home Directory = /Users/mr.l
    Default Configuration Name = Default DBConfiguration
    Default App Name = Default
    Java Home = /Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/jre
    Home dir = /Users/mr.l
    Boolean = true
    Int = 10
    Refreshing the spring context
    
    Db Driver Class = com.mysql.jdbc.Driver
    Db url = jdbc:mysql://localhost:3306/test
    Db username = cxuan
    Db password = 111111
    ```

	> 可以看到上述代码并没有显示调用printValues 方法，默认是以单元测试的方式进行的。

### 1.4.@PropertySource 与 @Import

`@Import` 可以用来导入 `@PropertySource` 标注的类，具体代码如下：

- 新建一个**PropertySourceReadApplication** 类，用于读取配置文件并测试，具体代码如下：

    ```java
    // 导入BasicPropertyWithJavaConfig类
    @Import(BasicPropertyWithJavaConfig.class)
    public class PropertySourceReadApplication {

        @Resource
        private Environment env;

        @Value("${com.spring.name}")
        private String name;

        @Bean("context")
        public PropertySourceReadApplication contextLoadInitialized(){
            // 用environment 读取配置文件
            System.out.println(env.getProperty("com.spring.age"));
            // 用@Value 读取配置文件
            System.out.println("name = " + name);
            return null;
        }

        public static void main(String[] args) {
            // AnnotationConnfigApplicationContext 内部会注册Bean
            new AnnotationConfigApplicationContext(PropertySourceReadApplication.class);
        }
    }
    ```

- 新建一个**BasicPropertyWithJavaConfig** 类，用于配置类并加载配置文件

    ```java
    @Configuration
    @PropertySource(value = "classpath:application.properties")
    public class BasicPropertyWithJavaConfig {

        public BasicPropertyWithJavaConfig(){
            super();
        }

    }
    ```

	启动 `PropertySourceReadApplication` ，`console` 能够发现读取到配置文件中的 `value` 值
	
	```
	18
	name = cxuan
	```



## 2.@SpringBootConfiguration

`@SpringBootConfiguration` 继承自 `@Configuration`，二者功能也一致，标注当前类是配置类，并会将当前类内声明的一个或多个以 `@Bean` 注解标记的方法的实例纳入到 `spring` 容器中，并且实例名就是方法名。

如下所示：

我定义了一个配置类

```
package com.lhkj.pluto.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class Config {
    @Bean
    public Map createMap(){
        Map map = new HashMap();
        map.put("username","gxz");
        map.put("age",27);
        return map;
    }
}
```

在main方法中，可以直接这样使用：

```
package com.lhkj.pluto;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lhkj.pluto.user.entity.User;



/*
 * 发现@SpringBootApplication是一个复合注解，
 * 包括@ComponentScan，和@SpringBootConfiguration，@EnableAutoConfiguration
 * 
 */
@RestController
@SpringBootApplication
public class App 
{   
    
    @RequestMapping(value="/hello")
    public String Hello(){
        return "hello";
    }
    
    @Bean
    public Runnable createRunnable() {
        return () -> System.out.println("spring boot is running");
    }

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        ConfigurableApplicationContext context = SpringApplication.run(App.class, args);
        context.getBean(Runnable.class).run();
        System.out.println(context.getBean(User.class));
        Map map = (Map) context.getBean("createMap");   //注意这里直接获取到这个方法bean
        int age = (int) map.get("age");
        System.out.println("age=="+age);
    }
    
    
    @Bean
    public EmbeddedServletContainerFactory servletFactory(){
        TomcatEmbeddedServletContainerFactory tomcatFactory = 
                new TomcatEmbeddedServletContainerFactory();
        //tomcatFactory.setPort(8011);
        tomcatFactory.setSessionTimeout(10,TimeUnit.SECONDS);
        return tomcatFactory;
        
    }
}
```



## 3.@EnableAutoConfiguration

### 3.1.demo

自己定义一个外部项目，`core-bean`，依赖如下

```xml
<artifactId>core-bean</artifactId>
<packaging>jar</packaging>

<dependencies>
   <dependency>
       <groupId>org.springframework</groupId>
       <artifactId>spring-context</artifactId>
       <version>4.3.9.RELEASE</version>
   </dependency>
</dependencies>
```

然后定义一个 `Cat` 类

```cpp
public class Cat {
}
```

```css
package core.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfig {

    @Bean
    public Cat cat(){
        return new Cat();
    }
}
```

我们知道这样就将 `Cat` 类装配到 `Spring` 容器了。

再定义一个 `springboot` 项目，加入 `core-bean` 依赖，依赖如下：

```xml
<artifactId>springboot-enableAutoConfiguration</artifactId>
<packaging>jar</packaging>

<dependencies>
     <dependency>
            <groupId>com.zhihao.miao</groupId>
            <artifactId>core-bean</artifactId>
            <version>1.0-SNAPSHOT</version>
      </dependency>
      <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
     </dependency>
</dependencies>
```

启动类启动：

```java
@EnableAutoConfiguration
@ComponentScan
public class Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =SpringApplication.run(Application.class,args);
        Cat cat = context.getBean(Cat.class);
        System.out.println(cat);
    }
}
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214414.webp)

发现 `Cat` 类并没有纳入到 `springboot-enableAutoConfiguration` 项目中。

**解决方案**

在 `core-bean` 项目 `resource` 下新建文件夹 `META-INF`，在文件夹下面新建 `spring.factories` 文件，文件中配置，`key` 为自定配置类 `EnableAutoConfiguration` 的全路径，`value` 是配置类的全路径

```undefined
org.springframework.boot.autoconfigure.EnableAutoConfiguration=core.bean.MyConfig
```

启动 `springboot-enableAutoConfiguration` 项目，打印结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214448.webp)

### 3.2.原理分析

进入 `EnableAutoConfiguration` 注解源码，发现是导入 `EnableAutoConfigurationImportSelector` 类，

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214459.webp)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214511.webp)

跟到最后发现继承了 `ImportSelector` 接口，之前我们讲过[Springboot @Enable*注解的工作原理](https://www.jianshu.com/p/1241c1079dd6) `ImportSelector`接口的`selectImports` 返回的数组（类的全类名）都会被纳入到 `spring` 容器中。

其在 `AutoConfigurationImportSelector` 类中的 `selectImports` 实现，进入`org.springframework.boot.autoconfigure.AutoConfigurationImportSelector`类，

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214538.webp)

进入 `getCandidateConfigurations` 方法

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214552.webp)

`getCandidateConfigurations` 会到 `classpath` 下的读取 `META-INF/spring.factories` 文件的配置，并返回一个字符串数组。

调试的时候读取到了 `core.bean.MyConfig`，也读到了一些其他的配置，下面会讲。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214603.webp)

具体的就不细说了，有兴趣的朋友可以自己调试一下。

`META-INF/spring.factories` 还可以配置多个配置类。

比如我们在 `core-bean` 下在定义二个类，

```java
package core.bean;

public class Dog {
}
```

```java
package core.bean;

public class People {
}
```

```css
package core.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Myconfig2 {

    @Bean
    public Dog dog(){
        return new Dog();
    }
}
```

修改 `META-INF/spring.factories` 下的配置

```undefined
org.springframework.boot.autoconfigure.EnableAutoConfiguration=core.bean.MyConfig,core.bean.Myconfig2,core.bean.People
```

修改 `springboot-enableAutoConfiguration` 项目的启动类：

```kotlin
package com.zhihao.miao;

import core.bean.Cat;
import core.bean.Dog;
import core.bean.People;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@EnableAutoConfiguration
@ComponentScan
public class Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext context =SpringApplication.run(Application.class,args);
        Cat cat = context.getBean(Cat.class);
        System.out.println(cat);
        Dog dog = context.getBean(Dog.class);
        System.out.println(dog);
        People people = context.getBean(People.class);
        System.out.println(people);
    }
}
```

打印结果如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214639.webp)

发现都纳入到 `spring` 容器中了。

可以配置`spring.boot.enableautoconfiguration=false`禁用自动配置，这样不会启动自动配置了，默认是true。还可以排出一些自动配置类，可以在 `EnableAutoConfiguration` 注解加入参数，这边不做过多解释。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214700.webp)

总结 `@EnableAutoConfiguration` 作用

从 `classpath` 中搜索所有 `META-INF/spring.factories` 配置文件然后，将其中`org.springframework.boot.autoconfigure.EnableAutoConfiguration key`对应的配置项加载到 `spring` 容器。只有`spring.boot.enableautoconfiguration`为 `true`（默认为true）的时候，才启用自动配置
`@EnableAutoConfiguration` 还可以进行排除，排除方式有2中，一是根据class来排除（`exclude`），二是根据class name（`excludeName`）来排除
其内部实现的关键点有

- `ImportSelector` 该接口的方法的返回值都会被纳入到 `spring` 容器管理中
- `SpringFactoriesLoader` 该类可以从 `classpath` 中搜索所有 `META-INF/spring.factories` 配置文件，并读取配置

### 3.3.springboot内部如何使用@EnableAutoConfiguration注解

我们点进去 `spring-boot-autoconfigure` 中的 `META-INF` 下的 `spring.factories` 文件，发现 `spring.factories` 文件中配置了好多的配置类，在将这些依赖依赖到自己的项目中会将其都纳入到 `spring` 容器中，不过这些类好多都是配合 `@Conditional***` 等注解一起工作的。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214737.webp)

举个例子：

 在 `springboot-enableAutoConfiguration` 加入 `Gson` 依赖：

```xml
<dependency>
      <groupId>com.google.code.gson</groupId>
       <artifactId>gson</artifactId>
</dependency>
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214750.webp)

如果我们不在项目中配置，`spring-boot-autoconfigure` 会自动帮我们装配一个对象实例名为 `gson` 的 `Gson` 实例。如果自己装配那么就使用自己装配的 `Gson` 实例。

启动测试类：

```java
package com.zhihao.miao;

import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =SpringApplication.run(Application.class,args);
        System.out.println(context.getBeansOfType(Gson.class));
    }
}
```

此时自己没有去配置Gson对象

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214812.webp)

如果自己配置了，测试代码如下，启动：

```java
package com.zhihao.miao;

import com.google.gson.Gson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    @Bean
    public Gson createGson(){
        return new Gson();
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context =SpringApplication.run(Application.class,args);
        System.out.println(context.getBeansOfType(Gson.class));
    }
}
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401214824.webp)



## 4.@ComponentScan

**4.1.@ComponentScan注解是什么**

其实很简单，`@ComponentScan` 主要就是定义**扫描的路径**从中找出标识了**需要装配**的类自动装配到 `spring` 的 `bean` 容器中

**4.2.@ComponentScan注解的详细使用**

做过 `web` 开发的同学一定都有用过 `@Controller`，`@Service`，`@Repository` 注解，查看其源码你会发现，他们中有一个**共同的注解`@Component`**，没错 `@ComponentScan` 注解默认就会装配标识了`@Controller`，`@Service`，`@Repository`，`@Component` 注解的类到`spring` 容器中，好下面咱们就先来简单演示一下这个例子

在包 `com.zhang.controller` 下新建一个 `UserController` 带 `@Controller` 注解如下：

```
package com.zhang.controller;
import org.springframework.stereotype.Controller;
@Controller
public class UserController {
}
```

在包 `com.zhang.service` 下新建一个 `UserService` 带 `@Service` 注解如下：

```
package com.zhang.service;
import org.springframework.stereotype.Service;
@Service
public class UserService {
}
```

在包 `com.zhang.dao` 下新建一个 `UserDao` 带 `@Repository` 注解如下：

```
package com.zhang.dao;
import org.springframework.stereotype.Repository;
@Repository
public class UserDao {
}
```

新建一个配置类如下：

```
/**
 * 主配置类  包扫描com.zhang
 *
 * @author zhangqh
 * @date 2018年5月12日
 */
@ComponentScan(value="com.zhang")
@Configuration
public class MainScanConfig {
}
```

新建测试方法如下：

```
		AnnotationConfigApplicationContext applicationContext2 = new 			AnnotationConfigApplicationContext(MainScanConfig.class);
        String[] definitionNames = applicationContext2.getBeanDefinitionNames();
        for (String name : definitionNames) {
            System.out.println(name);
		}
```

运行结果如下：

```
mainScanConfig
userController
userDao
userService
```

怎么样，包扫描的方式比以前介绍的通过 `@Bean` 注解的方式是不是方便很多，这也就是为什么 `web` 开发的同学经常使用此方式的原因了

上面只是简单的介绍了 `@ComponentScan` 注解检测包含指定注解的自动装配，接下来让我们来看看**`@ComponentScan` 注解的更加详细的配置**，在演示详细的配置之前，让我们先看看 `@ComponentScan` 的源代码如下：

```
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Repeatable(ComponentScans.class)
public @interface ComponentScan {
    /**
     * 对应的包扫描路径 可以是单个路径，也可以是扫描的路径数组
     * @return
     */
    @AliasFor("basePackages")
    String[] value() default {};
    /**
     * 和value一样是对应的包扫描路径 可以是单个路径，也可以是扫描的路径数组
     * @return
     */
    @AliasFor("value")
    String[] basePackages() default {};
    /**
     * 指定具体的扫描的类
     * @return
     */
    Class<?>[] basePackageClasses() default {};
    /**
     * 对应的bean名称的生成器 默认的是BeanNameGenerator
     * @return
     */
    Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;
    /**
     * 处理检测到的bean的scope范围
     */
    Class<? extends ScopeMetadataResolver> scopeResolver() default AnnotationScopeMetadataResolver.class;
    /**
     * 是否为检测到的组件生成代理
     * Indicates whether proxies should be generated for detected components, which may be
     * necessary when using scopes in a proxy-style fashion.
     * <p>The default is defer to the default behavior of the component scanner used to
     * execute the actual scan.
     * <p>Note that setting this attribute overrides any value set for {@link #scopeResolver}.
     * @see ClassPathBeanDefinitionScanner#setScopedProxyMode(ScopedProxyMode)
     */
    ScopedProxyMode scopedProxy() default ScopedProxyMode.DEFAULT;
    /**
     * 控制符合组件检测条件的类文件   默认是包扫描下的  **/*.class
     * @return
     */
    String resourcePattern() default ClassPathScanningCandidateComponentProvider.DEFAULT_RESOURCE_PATTERN;
    /**
     * 是否对带有@Component @Repository @Service @Controller注解的类开启检测,默认是开启的
     * @return
     */
    boolean useDefaultFilters() default true;
    /**
     * 指定某些定义Filter满足条件的组件 FilterType有5种类型如：
     *                                  ANNOTATION, 注解类型 默认
                                        ASSIGNABLE_TYPE,指定固定类
                                        ASPECTJ， ASPECTJ类型
                                        REGEX,正则表达式
                                        CUSTOM,自定义类型
     * @return
     */
    Filter[] includeFilters() default {};
    /**
     * 排除某些过来器扫描到的类
     * @return
     */
    Filter[] excludeFilters() default {};
    /**
     * 扫描到的类是都开启懒加载 ，默认是不开启的
     * @return
     */
    boolean lazyInit() default false;
}
```

**演示basePackageClasses参数，如我们把配置文件改成如下：**

```
@ComponentScan(value="com.zhang.dao",useDefaultFilters=true,basePackageClasses=UserService.class)
@Configuration
public class MainScanConfig {
}
```

测试结果如下：

```
mainScanConfig
userDao
userService
```

只有 `userDao` 外加 `basePackageClasses` 指定的 `userService` 加入到了 `spring` 容器中

**演示includeFilters参数的使用如下：**

在 `com.zhang.service` 包下新建一个 `UserService2` 类如下：注意没有带 `@Service` 注解

```
package com.zhang.service;
public class UserService2 {
}
```

配置类改成：

```
@ComponentScan(value="com.zhang",useDefaultFilters=true,
    includeFilters={
        @Filter(type=FilterType.ANNOTATION,classes={Controller.class}),
        @Filter(type=FilterType.ASSIGNABLE_TYPE,classes={UserService2.class})
    })
@Configuration
public class MainScanConfig {
}
```

运行结果如下：

```
mainScanConfig
userController
userDao
userService
userService2
```

`userService2` 同样被加入到了 `spring` 容器

新增一个自定义的实现了 `TypeFilter` 的 `MyTypeFilter` 类如下：

```
/**
 * 自定义过滤
 *
 * @author zhangqh
 * @date 2018年5月12日
 */
public class MyTypeFilter implements TypeFilter {
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
            throws IOException {
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        Resource resource = metadataReader.getResource();
        String className = classMetadata.getClassName();
        System.out.println("--->"+className);
        // 检测名字包含Service的bean
        if(className.contains("Service")){
            return true;
        }
        return false;
    }
}
```

修改主配置如下：

```
@ComponentScan(value="com.zhang",useDefaultFilters=true,
    includeFilters={
        @Filter(type=FilterType.ANNOTATION,classes={Controller.class}),
        @Filter(type=FilterType.CUSTOM,classes={MyTypeFilter.class})
    })
@Configuration
public class MainScanConfig {
}
```

运行结果如下：

```
mainScanConfig
userController
userDao
userService
userService2
```

可以发现同样 `userService2` 被加入到了 `spring` 容器中 

好了 `includeFilters` 参数就演示到这，另外一个参数 `excludeFilters` 和 `includeFilters` 用户一摸一样，只是他是过滤出不加入`spring` 容器中，感兴趣的同学可以自己试试，我这边就不演示了

**4.3.总结一下@ComponentScan的常用方式如下**

- 自定扫描路径下边带有 `@Controller`，`@Service`，`@Repository`，`@Component` 注解加入 `spring` 容器
- 通过 `includeFilters` 加入扫描路径下没有以上注解的类加入 `spring` 容器
- 通过 `excludeFilters` 过滤出不用加入 `spring` 容器的类
- 自定义增加了 `@Component` 注解的注解方式



## 5.@Import

### 5.1.@Import注解须知

- **@Import只能用在类上** ，`@Import` 通过快速导入的方式实现把实例加入 `spring` 的 `IOC` 容器中

- 加入 `IOC` 容器的方式有很多种，`@Import` 注解就相对很牛皮了，**@Import注解可以用于导入第三方包** ，当然 `@Bean` 注解也可以，但是 `@Import` 注解快速导入的方式更加便捷

- `@Import` 注解有三种用法

### 5.2.@Import的三种用法

`@Import` 的三种用法主要包括：

- 直接填 `class` 数组方式

- `ImportSelector` 方式【重点】

- `ImportBeanDefinitionRegistrar` 方式

#### 5.2.1.第一种用法：直接填class数组

**直接填对应的class数组，class数组可以有0到多个。**

语法如下：

```
@Import({ 类名.class , 类名.class... })
public class TestDemo {

}
```

对应的 `import` 的 `bean` 都将加入到 `spring` 容器中，这些在容器中 `bean` 名称是该类的**全类名** ，比如 `com.yc.类名`

#### 5.2.2.第二种用法：ImportSelector方式【重点】

这种方式的前提就是一个类要实现 `ImportSelector` 接口，假如我要用这种方法，目标对象是 `Myclass` 这个类，分析具体如下：

创建 `Myclass` 类并实现 `ImportSelector` 接口

```
public class Myclass implements ImportSelector {
	//既然是接口肯定要实现这个接口的方法
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[0];
    }
}
```

分析实现接口的 `selectImports` 方法中的：

- 1、返回值： 就是我们实际上要导入到容器中的组件全类名【**重点** 】
- 2、参数： `AnnotationMetadata` 表示当前被 `@Import` 注解给标注的所有注解信息【不是重点】

> 需要注意的是 `selectImports` 方法可以返回空数组但是不能返回 `null`，否则会报空指针异常！

以上分析完毕之后，具体用法步骤如下：

第一步：创建 `Myclass` 类并实现 `ImportSelector` 接口，这里用于演示就添加一个全类名给其返回值

```
public class Myclass implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{"com.yc.Test.TestDemo3"};
    }
}
```

第二步：编写 `TestDemo` 类，并标注上使用 `ImportSelector` 方式的 `Myclass` 类

```
@Import({TestDemo2.class,Myclass.class})
public class TestDemo {
        @Bean
        public AccountDao2 accountDao2(){
            return new AccountDao2();
        }

}
```

可以看出，宜春故意挑了个龙套角色 `@Bean` 注解，若对 `@Bean` 注解不是很清晰的童鞋可以参考[大白话讲解Spring的@bean注解](https://blog.csdn.net/qq_44543508/article/details/103718958)

第三步：编写打印容器中的组件测试类

```
/**
 * 打印容器中的组件测试
 */
public class AnnotationTestDemo {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext applicationContext=new AnnotationConfigApplicationContext(TestDemo.class);  //这里的参数代表要做操作的类

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String name : beanDefinitionNames){
            System.out.println(name);
        }

    }
}
```

第四步：运行结果

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401220741.png)

#### 5.2.3.第三种用法：ImportBeanDefinitionRegistrar方式

同样是一个接口，类似于第二种 `ImportSelector` 用法，相似度80%，只不过这种用法比较自定义化注册，具体如下：

第一步：创建 `Myclass2` 类并实现 `ImportBeanDefinitionRegistrar` 接口

```
public class Myclass2 implements ImportBeanDefinitionRegistrar {
//该实现方法默认为空
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
      
    }
}
```

参数分析：

- 第一个参数：`annotationMetadata` 和之前的 `ImportSelector` 参数一样都是表示当前被 `@Import` 注解给标注的所有注解信息
- 第二个参数表示用于注册定义一个 `bean`

第二步：编写代码，自定义注册 `bean`

```
public class Myclass2 implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        //指定bean定义信息（包括bean的类型、作用域...）
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(TestDemo4.class);
        //注册一个bean指定bean名字（id）
        beanDefinitionRegistry.registerBeanDefinition("TestDemo4444",rootBeanDefinition);
    }
}
```

第三步：编写 `TestDemo` 类，并标注上使用 `ImportBeanDefinitionRegistrar` 方式的 `Myclass2` 类

```
@Import({TestDemo2.class,Myclass.class,Myclass2.class})
public class TestDemo {

        @Bean
        public AccountDao2 accountDao222(){
            return new AccountDao2();
        }

}
```

第四步：运行结果

![å¨è¿éæå¥å¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401220801.png)

### 5.3.@Import注解的三种使用方式总结

- 第一种用法：`@Import`（{ 要导入的容器中的组件 } ）：容器会自动注册这个组件，**id默认是全类名**

- 第二种用法：`ImportSelector`：返回需要导入的组件的全类名数组，`springboot` 底层用的特别多【**重点** 】

- 第三种用法：`ImportBeanDefinitionRegistrar`：手动注册 `bean` 到容器

**以上三种用法方式皆可混合在一个@Import中使用，特别注意第一种和第二种都是以全类名的方式注册，而第三中可自定义方式。**

`@Import` 注解本身在 `springboot` 中用的很多，特别是其中的第二种用法 `ImportSelector` 方式在 `springboot` 中使用的特别多，尤其要掌握！



## 6.@Conditionalxxx

### 6.1前言

不知道大家在使用Spring Boot开发的日常中有没有用过`@Conditionalxxx`注解，比如`@ConditionalOnMissingBean`。相信看过 `Spring Boot` 源码的朋友一定不陌生。

`@Conditionalxxx` 这类注解表示某种判断条件成立时才会执行相关操作。掌握该类注解，有助于日常开发，框架的搭建。

### 6.2.Spring Boot 版本

本文基于的Spring Boot的版本是`2.3.4.RELEASE`。

### 6.3.@Conditional

`@Conditional`注解是从`Spring4.0`才有的，可以用在任何类型或者方法上面，通过`@Conditional`注解可以配置一些条件判断，当所有条件都满足的时候，被`@Conditional`标注的目标才会被`Spring容器`处理。

`@Conditional`的使用很广，比如控制某个`Bean`是否需要注册，在Spring Boot中的变形很多，比如`@ConditionalOnMissingBean`、`@ConditionalOnBean`等等，如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401221750.png)

该注解的源码其实很简单，只有一个属性`value`，表示判断的条件（一个或者多个），是`org.springframework.context.annotation.Condition`类型，源码如下：

```
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

 /**
  * All {@link Condition} classes that must {@linkplain Condition#matches match}
  * in order for the component to be registered.
  */
 Class<? extends Condition>[] value();
}
```

`@Conditional`注解实现的原理很简单，就是通过`org.springframework.context.annotation.Condition`这个接口判断是否应该执行操作。

### 6.4.Condition接口

`@Conditional`注解判断条件与否取决于`value`属性指定的`Condition`实现，其中有一个`matches()`方法，返回`true`表示条件成立，反之不成立，接口如下：

```
@FunctionalInterface
public interface Condition {
 boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
}
```

**`matches`中的两个参数如下：**

1. `context`：条件上下文，`ConditionContext`接口类型的，可以用来获取容器中上下文信息。
2. `metadata`：用来获取被`@Conditional`标注的对象上的所有注解信息

#### 6.4.1.ConditionContext接口

这个接口很重要，能够从中获取Spring上下文的很多信息，比如`ConfigurableListableBeanFactory`，源码如下：

```
public interface ConditionContext {

    /**
     * 返回bean定义注册器，可以通过注册器获取bean定义的各种配置信息
     */
    BeanDefinitionRegistry getRegistry();

    /**
     * 返回ConfigurableListableBeanFactory类型的bean工厂，相当于一个ioc容器对象
     */
    @Nullable
    ConfigurableListableBeanFactory getBeanFactory();

    /**
     * 返回当前spring容器的环境配置信息对象
     */
    Environment getEnvironment();

    /**
     * 返回资源加载器
     */
    ResourceLoader getResourceLoader();

    /**
     * 返回类加载器
     */
    @Nullable
    ClassLoader getClassLoader();
}
```

### 6.5.如何自定义Condition？

举个栗子：**假设有这样一个需求，需要根据运行环境注入不同的`Bean`，`Windows`环境和`Linux`环境注入不同的`Bean`。**

实现很简单，分别定义不同环境的判断条件，实现`org.springframework.context.annotation.Condition`即可。

**windows环境的判断条件源码如下**：

```
/**
 * 操作系统的匹配条件，如果是windows系统，则返回true
 */
public class WindowsCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
        //获取当前环境信息
        Environment environment = conditionContext.getEnvironment();
        //获得当前系统名
        String property = environment.getProperty("os.name");
        //包含Windows则说明是windows系统，返回true
        if (property.contains("Windows")){
            return true;
        }
        return false;

    }
}
```

**Linux环境判断源码如下**：

```
/**
 * 操作系统的匹配条件，如果是windows系统，则返回true
 */
public class LinuxCondition implements Condition {
    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
        Environment environment = conditionContext.getEnvironment();

        String property = environment.getProperty("os.name");
        if (property.contains("Linux")){
            return true;
        }
        return false;

    }
}
```

**配置类中结合`@Bean`注入不同的Bean，如下**：

```
@Configuration
public class CustomConfig {

    /**
     * 在Windows环境下注入的Bean为winP
     * @return
     */
    @Bean("winP")
    @Conditional(value = {WindowsCondition.class})
    public Person personWin(){
        return new Person();
    }

    /**
     * 在Linux环境下注入的Bean为LinuxP
     * @return
     */
    @Bean("LinuxP")
    @Conditional(value = {LinuxCondition.class})
    public Person personLinux(){
        return new Person();
    }
```

**简单的测试一下，如下**：

```
@SpringBootTest
class SpringbootInterceptApplicationTests {

    @Autowired(required = false)
    @Qualifier(value = "winP")
    private Person winP;

    @Autowired(required = false)
    @Qualifier(value = "LinuxP")
    private Person linP;

    @Test
    void contextLoads() {
        System.out.println(winP);
        System.out.println(linP);
    }
}
```

**Windows环境下执行单元测试，输出如下**：

```
com.example.springbootintercept.domain.Person@885e7ff
null
```

很显然，判断生效了，Windows环境下只注入了`WINP`。

### 6.6.条件判断在什么时候执行？

条件判断的执行分为两个阶段，如下：

1. **配置类解析阶段(`ConfigurationPhase.PARSE_CONFIGURATION`)**：在这个阶段会得到一批配置类的信息和一些需要注册的`Bean`。
2. **Bean注册阶段(`ConfigurationPhase.REGISTER_BEAN`)**：将配置类解析阶段得到的配置类和需要注册的 `Bean` 注入到容器中。

默认都是配置解析阶段，其实也就够用了，但是在 `Spring Boot` 中使用了`ConfigurationCondition`，这个接口可以自定义执行阶段，比如`@ConditionalOnMissingBean`都是在Bean注册阶段执行，因为需要从容器中判断Bean。

> **这个两个阶段有什么不同呢？**：其实很简单的，配置类解析阶段只是将需要加载配置类和一些Bean（被`@Conditional`注解过滤掉之后）收集起来，而Bean注册阶段是将的收集来的Bean和配置类注入到容器中，**如果在配置类解析阶段执行`Condition`接口的`matches()`接口去判断某些Bean是否存在IOC容器中，这个显然是不行的，因为这些Bean还未注册到容器中**。

> **什么是配置类，有哪些？**：类上被`@Component`、 `@ComponentScan`、`@Import`、`@ImportResource`、`@Configuration`标注的以及类中方法有`@Bean`的方法。如何判断配置类，在源码中有单独的方法：`org.springframework.context.annotation.ConfigurationClassUtils#isConfigurationCandidate`。

### 6.7.ConfigurationCondition接口

这个接口相比于`@Condition`接口就多了一个`getConfigurationPhase()`方法，可以自定义执行阶段。源码如下：

```
public interface ConfigurationCondition extends Condition {

    /**
     * 条件判断的阶段，是在解析配置类的时候过滤还是在创建bean的时候过滤
     */
    ConfigurationPhase getConfigurationPhase();


    /**
     * 表示阶段的枚举：2个值
     */
    enum ConfigurationPhase {

        /**
         * 配置类解析阶段，如果条件为false，配置类将不会被解析
         */
        PARSE_CONFIGURATION,

        /**
         * bean注册阶段，如果为false，bean将不会被注册
         */
        REGISTER_BEAN
    }
}
```

这个接口在需要指定执行阶段的时候可以实现，比如需要根据某个Bean是否在IOC容器中来注入指定的Bean，则需要指定执行阶段为**Bean的注册阶段**（`ConfigurationPhase.REGISTER_BEAN`）。

### 6.8.多个Condition的执行顺序

`@Conditional`中的`Condition`判断条件可以指定多个，默认是按照先后顺序执行，如下：

```
class Condition1 implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        System.out.println(this.getClass().getName());
        return true;
    }
}

class Condition2 implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        System.out.println(this.getClass().getName());
        return true;
    }
}

class Condition3 implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        System.out.println(this.getClass().getName());
        return true;
    }
}

@Configuration
@Conditional({Condition1.class, Condition2.class, Condition3.class})
public class MainConfig5 {
}
```

**上述例子会依次按照`Condition1`、`Condition2`、`Condition3`执行。**

默认按照先后顺序执行，但是当我们需要指定顺序呢？很简单，有如下三种方式：

1. 实现`PriorityOrdered`接口，指定优先级
2. 实现`Ordered`接口接口，指定优先级
3. 使用`@Order`注解来指定优先级

例子如下：

```
@Order(1) 
class Condition1 implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        System.out.println(this.getClass().getName());
        return true;
    }
}

class Condition2 implements Condition, Ordered { 
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        System.out.println(this.getClass().getName());
        return true;
    }

    @Override
    public int getOrder() { 
        return 0;
    }
}

class Condition3 implements Condition, PriorityOrdered {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        System.out.println(this.getClass().getName());
        return true;
    }

    @Override
    public int getOrder() {
        return 1000;
    }
}

@Configuration
@Conditional({Condition1.class, Condition2.class, Condition3.class})
public class MainConfig6 {
}
```

根据排序的规则，`PriorityOrdered`的会排在前面，然后会再按照`order`升序，最后可以顺序是：`Condtion3->Condtion2->Condtion1`

### 6.9.Spring Boot中常用的一些注解

Spring Boot中大量使用了这些注解，常见的注解如下：

1. `@ConditionalOnBean`：当容器中有指定Bean的条件下进行实例化。
2. `@ConditionalOnMissingBean`：当容器里没有指定Bean的条件下进行实例化。
3. `@ConditionalOnClass`：当classpath类路径下有指定类的条件下进行实例化。
4. `@ConditionalOnMissingClass`：当类路径下没有指定类的条件下进行实例化。
5. `@ConditionalOnWebApplication`：当项目是一个Web项目时进行实例化。
6. `@ConditionalOnNotWebApplication`：当项目不是一个Web项目时进行实例化。
7. `@ConditionalOnProperty`：当指定的属性有指定的值时进行实例化。
8. `@ConditionalOnExpression`：基于SpEL表达式的条件判断。
9. `@ConditionalOnJava`：当JVM版本为指定的版本范围时触发实例化。
10. `@ConditionalOnResource`：当类路径下有指定的资源时触发实例化。
11. `@ConditionalOnJndi`：在JNDI存在的条件下触发实例化。
12. `@ConditionalOnSingleCandidate`：当指定的Bean在容器中只有一个，或者有多个但是指定了首选的Bean时触发实例化。

比如在`WEB`模块的自动配置类`WebMvcAutoConfiguration`下有这样一段代码：

```
    @Bean
  @ConditionalOnMissingBean
  public InternalResourceViewResolver defaultViewResolver() {
   InternalResourceViewResolver resolver = new InternalResourceViewResolver();
   resolver.setPrefix(this.mvcProperties.getView().getPrefix());
   resolver.setSuffix(this.mvcProperties.getView().getSuffix());
   return resolver;
  }
```

常见的`@Bean`和`@ConditionalOnMissingBean`注解结合使用，意思是当容器中没有`InternalResourceViewResolver`这种类型的Bean才会注入。这样写有什么好处呢？好处很明显，可以让开发者自定义需要的视图解析器，如果没有自定义，则使用默认的，这就是Spring Boot为自定义配置提供的便利。

### 6.10.总结

`@Conditional`注解在Spring Boot中演变的注解很多，需要着重了解，特别是后期框架整合的时候会大量涉及。

## 7.@ConfigurationProperties

读取默认配置文件（`application.properties`、`application.yml`）

实现方式一 `@ConfigurationProperties` + `@Component` 作用于类上

```java
@ConfigurationProperties(prefix="person")
@Componment
@Data     // lombok，用于自动生成getter、setter
public class Person {
    private String name；
}

@RestController
@RequestMapping("/db")
public class TestController {
    @Autowired
    private Person person;

    @GetMapping("/person")
    public String parsePerson() {
        return person.getName();
    }
}
```

实现方式二 `@ConfigurationProperties` + `@Bean` 作用在配置类的 `bean` 方法上

```java
@Data
public class Person {
    private String name；
}

@Configuration
public class PersonConf{
    @Bean
    @ConfigurationProperties(prefix="person")
    public Person person(){
        return new Person();
    }  
} 

@RestController
@RequestMapping("/db")
public class TestController {
    @Autowired
    private Person person;
    @GetMapping("/person")
    public String parsePerson() {
        return person.getName();
    }
}
```

实现方式三 `@ConfigurationProperties` 注解到普通类、 `@EnableConfigurationProperties` 注解定义为 `bean`

```java
@ConfigurationProperties(prefix="person")
@Data
public class Person {
    private String name；
}
// 说明： @EnableConfigurationProperties可以直接注到启动类上，也可以放在自定义配置类，自定义配置类使用@Configuration标注
@SpringBootApplication
@EnableConfigurationProperties(Person.class)
public class DbApplication {
    public static void main(String[] args) {
        SpringApplication.run(DbApplication.class, args);
    }
}

@RestController
@RequestMapping("/db")
public class TestController {
    @Autowired
    private Person person;

    @GetMapping("/person")
    public String parsePerson() {
        return person.getName();
    }
}
```

实现方式四 `@Value` 作用属性上

```java
@RestController
@RequestMapping("/db")
public class TestController {
    @Value("${person.name}")
    private String name;

    @GetMapping("/person")
    public String parsePerson() {
        return name;
    }
}
```

实现方式五 使用自带的 `Environment` 对象

```java
@RestController
@RequestMapping("/db")
public class TestController {
    @Autowired
    private Environment environment;

    @GetMapping("/person")
    public String parsePerson() {
        return environment.getProperty("person.name");
    }
}
```









































