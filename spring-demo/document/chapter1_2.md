[toc]



# Spring 模块结构

Spring是**一个轻量级Java开发框架**，致力于简化Java开发。

Spring 总共大约有 20 个模块， 由 1300 多个不同的文件构成。 而这些组件被分别整合在`核心容器（Core Container）` 、 `AOP（Aspect Oriented Programming）和设备支持（Instrmentation）` 、`数据访问与集成（Data Access/Integeration）` 、 `Web`、 `消息（Messaging）` 、 `Test`等 6 个模块中。 以下是 Spring 5 的模块结构图：

![x6gs4mgaq3](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328192848.png)

组成 Spring 框架的每个模块集合或者模块都可以单独存在， 也可以一个或多个模块联合实现。 每个模 块的组成和功能如下：

## 1.核心容器

Spring的核心容器是其他模块建立的基础，有spring-core、spring-beans、spring-context、spring-context-support和spring-expression（Spring表达式语言）等模块组成。

- **spring-core 模块**：提供了框架的基本组成部分，包括控制反转（Inversion of Control，IOC）和依赖注入（Dependency Injection，DI）功能。

- **spring-beans 模块**：提供了BeanFactory，是工厂模式的一个经典实现，Spring将管理对象称为Bean。

- **spring-context 模块**：建立在Core和Beans模块的基础之上，提供一个框架式的对象访问方式，是访问定义和配置的任何对象的媒介。ApplicationContext接口是Context模块的焦点。

- **spring-context-support 模块**：支持整合第三方库到Spring应用程序上下文，特别是用于高速缓存（EhCache、JCache）和任务调度（CommonJ、Quartz）的支持。

- **Spring-expression 模块**：提供了强大的表达式语言去支持运行时查询和操作对象图。这是对JSP2.1规范中规定的统一表达式语言（Unified EL）的扩展。该语言支持设置和获取属性值、属性分配、方法调用、访问数组、集合和索引器的内容、逻辑和算术运算、变量命名以及从Spring的IOC容器中以名称检索对象。它还支持列表投影、选择以及常用的列表聚合。

## 2.AOP 和设备支持

由spring-aop、 spring-aspects 和 spring-instrument等 3 个模块组成。

- **spring-aop 模块**：是 Spring 的另一个核心模块，提供了一个符合 AOP 要求的面向切面的编程实现。 作为继 OOP（面向对象编程） 后， 对程序员影响最大的编程思想之一， AOP 极大地开拓了人们对于编程的思路。 在 Spring 中， 以动态代理技术为基础，允许定义方法拦截器和切入点，将代码按照功能进行分离，以便干净地解耦。

- **spring-aspects 模块**：提供了与AspectJ的集成功能，AspectJ是一个功能强大且成熟的AOP框架。

- **spring-instrument 模块**：是 AOP 的一个支援模块， 提供了类植入（Instrumentation）支持和类加载器的实现，可以在特定的应用服务器中使用。主要作用是在 JVM 启用时， 生成一个代理类， 程序员通过代理类在运行时修改类的字节， 从而改变一个类的功能， 实现 AOP 的功能。

## 3.数据访问与集成

由 spring-jdbc、spring-orm、spring-oxm、spring-jms 和 spring-tx 等 5 个模块组成。

- **spring-jdbc 模块**：提供了一个JDBC的抽象层，消除了烦琐的JDBC编码和数据库厂商特有的错误代码解析， 用于简化JDBC。主要是提供 JDBC 模板方式、 关系数据库对象化方式、 SimpleJdbc 方式、 事务管理来简化 JDBC 编程， 主要实现类是 JdbcTemplate、 SimpleJdbcTemplate 以及 NamedParameterJdbcTemplate。

- **spring-orm 模块**：是 ORM 框架支持模块， 主要集成 Hibernate， Java Persistence API (JPA) 和Java Data Objects (JDO) 用于资源管理、 数据访问对象(DAO)的实现和事务策略。

- **spring-oxm 模块**：主要提供一个抽象层以支撑 OXM（OXM 是 Object-to-XML-Mapping 的缩写， 它是一个 O/M-mapper， 将 java 对象映射成 XML 数据， 或者将 XML 数据映射成 java 对象） ， 例如： JAXB，Castor，XMLBeans，JiBX 和 XStream 等。

- **spring-jms模块**（Java Messaging Service）：指Java消息传递服务，包含用于生产和使用消息的功能。自Spring4.1以后，提供了与spring-messaging模块的集成。

- **spring-tx 模块**：事务模块，支持用于实现特殊接口和所有POJO（普通Java对象）类的编程和声明式事务管理。

## 4.Web

由spring-websocket、spring-webmvc、spring-web、portlet和spring-webflux模块等 5 个模块组成。

- **spring-websocket 模块**：Spring4.0以后新增的模块，实现双工异步通讯协议，实现了WebSocket和SocketJS，提供Socket通信和web端的推送功能。

- **spring-webmvc 模块**：也称为Web-Servlet模块，包含用于web应用程序的Spring MVC和REST Web Services实现。Spring MVC框架提供了领域模型代码和Web表单之间的清晰分离，并与Spring Framework的所有其他功能集成。

- **spring-web 模块**：提供了基本的Web开发集成功能，包括使用Servlet监听器初始化一个IOC容器以及Web应用上下文，自动载入WebApplicationContext特性的类，Struts集成类、文件上传的支持类、Filter类和大量辅助工具类。

- **portlet 模块**：实现web模块功能的聚合，类似于Servlet模块的功能，提供了Portlet环境下的MVC实现。

- **spring-webflux 模块**：是一个新的非堵塞函数式 Reactive Web 框架， 可以用来建立异步的， 非阻塞，事件驱动的服务， 并且扩展性非常好。

## 5.消息(Messaging)

即 spring-messaging 模块。

spring-messaging 是从 Spring4 开始新加入的一个模块， 该模块提供了对消息传递体系结构和协议的支持。

## 6.Test

即 spring-test 模块。

spring-test 模块主要为测试提供支持的，支持使用JUnit或TestNG对Spring组件进行单元测试和集成测试。

## 7.Spring各模块(jar包)之间的依赖关系

该图是 Spring5 的包结构， 可以从中清楚看出 Spring 各个模块(jar包)之间的依赖关系。

![zdtawzuwgj](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328192856.png)