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