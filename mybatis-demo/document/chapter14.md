[toc]



# MyBatis 面试题

## MyBatis 定义的接口，怎么找到实现的？
一共五步:

1. Mapper 接口在初始SqlSessionFactory 注册的。
1. Mapper 接口注册在了名为 MapperRegistry 类的 HashMap中， key = Mapper class value = 创建当前Mapper的工厂。
1. Mapper 注册之后，可以从SqlSession中get
1. SqlSession.getMapper 运用了 JDK动态代理，产生了目标Mapper接口的代理对象。
1. 动态代理的 代理类是 MapperProxy ，这里边最终完成了增删改查方法的调用。

## mybatis 有几种分页方式？
## RowBounds 是一次性查询全部结果吗？为什么？
## mybatis 逻辑分页和物理分页的区别是什么？
## mybatis 是否支持延迟加载？延迟加载的原理是什么？
## 说一下 mybatis 的一级缓存和二级缓存？
## mybatis 和 hibernate 的区别有哪些？
## mybatis 有哪些执行器（Executor）？
## mybatis 分页插件的实现原理是什么？
## mybatis 如何编写一个自定义插件？