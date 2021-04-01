[TOC]



# Spring Boot Starter

## 1.自定义starter

### 1.1.码前必备知识

#### 1.1.1.SpringBoot starter机制

SpringBoot中的starter是一种非常重要的机制，能够抛弃以前繁杂的配置，将其统一集成进starter，应用者只需要在maven中引入starter依赖，SpringBoot就能自动扫描到要加载的信息并启动相应的默认配置。starter让我们摆脱了各种依赖库的处理，需要配置各种信息的困扰。SpringBoot会自动通过classpath路径下的类发现需要的Bean，并注册进IOC容器。SpringBoot提供了针对日常企业应用研发各种场景的spring-boot-starter依赖模块。所有这些依赖模块都遵循着约定成俗的默认配置，并允许我们调整这些配置，即遵循“约定大于配置”的理念。

#### 1.1.2.为什么要自定义starter

在我们的日常开发工作中，经常会有一些独立于业务之外的配置模块，我们经常将其放到一个特定的包下，然后如果另一个工程需要复用这块功能的时候，需要将代码硬拷贝到另一个工程，重新集成一遍，麻烦至极。如果我们将这些可独立于业务代码之外的功配置模块封装成一个个starter，复用的时候只需要将其在pom中引用依赖即可，SpringBoot为我们完成自动装配，简直不要太爽。

#### 1.1.3.自定义starter的案例

以下案例由笔者工作中遇到的部分场景

-  动态数据源。
- 登录模块。
- 基于AOP技术实现日志切面。

。。。。。。

#### 1.1.4.自定义starter的命名规则

SpringBoot提供的starter以`spring-boot-starter-xxx`的方式命名的。官方建议自定义的starter使用`xxx-spring-boot-starter`命名规则。以区分SpringBoot生态提供的starter。

### 1.2.starter的实现方法

#### 1.2.1.新建一个工程

命名为demo-spring-boot-starter

下图为工程目录结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401232750.png)

#### 1.2.2.pom依赖

```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.4.RELEASE</version>
    </parent>
    <groupId>com.demo</groupId>
    <artifactId>demo-spring-boot-starter</artifactId>
    <version>0.0.1-RELEASE</version>
    <name>demo-spring-boot-starter</name>
    <description>Demo project for Spring Boot</description>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### 1.2.3.定义一个实体类映射配置信息

@ConfigurationProperties(prefix = "demo") 它可以把相同前缀的配置信息通过配置项名称映射成实体类，比如我们这里指定 prefix = "demo" 这样，我们就能将以demo为前缀的配置项拿到了。

ps：其实这个注解很强大，它不但能映射成String或基本类型的变量。还可以映射为List，Map等数据结构。

```
package com.demo.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 描述：配置信息 实体
 *
 * @Author shf
 * @Date 2019/5/7 22:08
 * @Version V1.0
 **/
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {
    private String sayWhat;
    private String toWho;

    public String getSayWhat() {
        return sayWhat;
    }

    public void setSayWhat(String sayWhat) {
        this.sayWhat = sayWhat;
    }

    public String getToWho() {
        return toWho;
    }

    public void setToWho(String toWho) {
        this.toWho = toWho;
    }
}
```

#### 1.2.4.定义一个Service

```
package com.demo.starter.service;

/**
 * 描述：随便定义一个Service
 *
 * @Author shf
 * @Date 2019/5/7 21:59
 * @Version V1.0
 **/
public class DemoService {
    public String sayWhat;
    public String toWho;
    public DemoService(String sayWhat, String toWho){
        this.sayWhat = sayWhat;
        this.toWho = toWho;
    }
    public String say(){
        return this.sayWhat + "!  " + toWho;
    }
}
```

#### 1.2.5.定义一个配置类

这里，我们将DemoService类定义为一个Bean，交给Ioc容器。

- @Configuration 注解就不多说了。

- @EnableConfigurationProperties 注解。该注解是用来开启对3步骤中 @ConfigurationProperties 注解配置Bean的支持。也就是@EnableConfigurationProperties注解告诉Spring Boot 能支持@ConfigurationProperties。

  当然了，也可以在 @ConfigurationProperties 注解的类上添加 @Configuration 或者 @Component 注解

- @ConditionalOnProperty 注解控制 @Configuration 是否生效。简单来说也就是我们可以通过在yml配置文件中控制 @Configuration 注解的配置类是否生效。

```
package com.demo.starter.config;

import com.demo.starter.properties.DemoProperties;
import com.demo.starter.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 描述：配置类
 *
 * @Author shf
 * @Date 2019/5/7 21:50
 * @Version V1.0
 **/
@Configuration
@EnableConfigurationProperties(DemoProperties.class)
@ConditionalOnProperty(
        prefix = "demo",
        name = "isopen",
        havingValue = "true"
)
public class DemoConfig {
    @Autowired
    private DemoProperties demoProperties;

    @Bean(name = "demo")
    public DemoService demoService(){
        return new DemoService(demoProperties.getSayWhat(), demoProperties.getToWho());
    }
}
```

#### 1.2.6.最重要的来了

如图，新建META-INF文件夹，然后创建spring.factories文件，

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401233058.png)

 在该文件中加入如下配置，该配置指定上步骤中定义的配置类为自动装配的配置。（笔者努力最近把自动装配的博客写出来）

```
#-------starter自动装配---------
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.demo.starter.config.DemoConfig
```

#### 1.2.7.测试

在demo-spring-boot-starter工程中执行 `mvn clean install` 一个自定义的starter新鲜出炉。

**新建测试工程**

**引入starter依赖**

```
<dependency>
    <groupId>com.demo</groupId>
    <artifactId>demo-spring-boot-starter</artifactId>
    <version>0.0.1-RELEASE</version>
</dependency>
```

**配置文件**

```
demo.isopen=true
demo.say-what=hello
demo.to-who=shf
```

然后写个测试类。

```
package com.example.test.controller;

import com.demo.starter.service.DemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 描述：
 *
 * @Author shf
 * @Description TODO
 * @Date 2019/5/13 15:52
 * @Version V1.0
 **/
@RestController
public class DemoController {
    @Resource(name = "demo")
    private DemoService demoService;

    @GetMapping("/say")
    public String sayWhat(){
        return demoService.say();
    }

}
```

 **浏览器**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401233246.png)



