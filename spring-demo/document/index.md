# Spring 全家桶学习笔记

## Spring Framework

也就是我们经常说的spring框架，包括了ioc依赖注入，Context上下文、bean管理、springmvc等众多功能模块，其它spring项目比如spring boot也会依赖spring框架。

1. [Spring 概述](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_1.md)
2. [Spring 模块结构](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_2.md)
3. [Spring 核心容器](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_3.md)
4. [Spring 核心容器 - Bean](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_4.md)
5. [Spring 核心容器 - 上下文](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_5.md)
6. [Spring 核心容器  - 初始化](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_6.md)
7. [Spring AOP](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_7.md)
8. [Spring AOP - 基于AspectJ的AOP](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_8.md)
9. [Spring 事务](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_9.md)
10. [Spring DAO](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_10.md)
11. -[Spring ORM](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_11.md)
12. Spring Web
13. [Spring MVC](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_13.md)
14. [Spring 应用](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_14.md)
15. [Spring 面试题](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter1_XXmd)



## Spring Boot

它的目标是简化Spring应用和服务的创建、开发与部署，简化了配置文件，使用嵌入式web服务器，含有诸多开箱即用的微服务功能，可以和spring cloud联合部署。

Spring Boot的核心思想是约定大于配置，应用只需要很少的配置即可，简化了应用开发模式。

1. [Spring Boot 概述](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_1.md)
2. [Spring Boot 项目构建](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_2.md)
3. [Spring Boot 关键注解](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_3.md)
4. [Spring Boot 配置文件](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_4.md)
5. [Spring Boot Starter](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_5.md)
6. [Spring Boot 自动装配原理](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_6.md)
7. [Spring Boot 部署](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_7.md)
8. [Spring Boot 应用](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_8.md)
9. [Sprint Boot 常用工具](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_9.md)
10. [Spring Boot 集成](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_10.md)
11. [Spring Boot 常见问题](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter2_XX.md)



## Spring Data

是一个数据访问及操作的工具集，封装了多种数据源的操作能力，包括：jdbc、Redis、MongoDB等。



## Spring Cloud

是一套完整的微服务解决方案，是一系列不同功能的微服务框架的集合。Spring Cloud基于Spring Boot，简化了分布式系统的开发，集成了服务发现、配置管理、消息总线、负载均衡、断路器、数据监控等各种服务治理能力。比如sleuth提供了全链路追踪能力，Netflix套件提供了hystrix熔断器、zuul网关等众多的治理组件。config组件提供了动态配置能力，bus组件支持使用RabbitMQ、kafka、Activemq等消息队列，实现分布式服务之间的事件通信。



## Spring Security

主要用于快速构建安全的应用程序和服务，在Spring Boot和Spring Security OAuth2的基础上，可以快速实现常见安全模型，如单点登录，令牌中继和令牌交换。你可以了解一下oauth2授权机制和jwt认证方式。oauth2是一种授权机制，规定了完备的授权、认证流程。JWT全称是JSON Web Token，是一种把认证信息包含在token中的认证实现，oauth2授权机制中就可以应用jwt来作为认证的具体实现方法。

1. [Spring Security 和 Shiro 该如何选择？](https://github.com/HomanLiang/study-demo/blob/main/spring-demo/document/chapter5_01.md)