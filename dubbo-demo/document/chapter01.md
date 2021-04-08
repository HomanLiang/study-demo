[toc]



# Dubbo 简介

## 相关文档
 [官方文档](http://dubbo.apache.org/)
[dubbo可视化测试工具](https://gitee.com/IdeaHome_admin/dubbo-proxy-tools)

## Dubbo 简介

Apache Dubbo (incubating) |ˈdʌbəʊ| 是一款高性能、轻量级的开源Java RPC框架，它提供了三大核心能力：**面向接口的远程方法调用，智能容错和负载均衡，以及服务自动注册和发现。**

![å¨è¿éæå¥å¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210409000219.png)

Dubbo的服务治理：

![å¨è¿éæå¥å¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210409000235.png)

Dubbo原理图片，图片来自Dubbo官网：

![å¨è¿éæå¥å¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/dubbo-demo/20210409000251.png)

**Dubbo角色：**

- Provider：暴露服务的服务提供者
- Container：服务运行的容器
- Consumer：调用远程服务的消费者
- Registry：服务注册和发现的注册中心
- Minitor：统计服务调用次数和时间的监控中心

**调用过程：**

下面根据我的理解说明一下

- 0：服务器容器负责启动、加载、运行服务提供者
- 1：服务提供者在启动后就可以向注册中心暴露服务
- 2：服务消费者在启动后就可以向注册中心订阅想要的服务
- 3：注册中心向服务消费者返回服务调用列表
- 4：服务消费者基于软负载均衡算法调用服务提供者的服务，这个服务提供者有可能是一个服务提供者列表，调用那个服务提供者就是根据负载均衡来调用了
- 5：服务提供者和服务消费者定时将保存在内存中的服务调用次数和服务调用时间推送给监控中心











