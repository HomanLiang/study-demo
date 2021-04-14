[toc]



# Spring 核心容器 - 上下文

## 1. context 是什么

我们经常在编程中见到 context 这个单词，当然每个人有每个人的理解，它被理解为：上下文、容器等等。我想说的是，context 理解为上下文最为合适。为什么呢？我以一个在计算机系统的例子来解释一下。

在计算机系统中，进程执行时有进程上下文，如果进程在执行的过程中遇到了中断，CPU 会从用户态切换为内核态（当然这个过程用户进程是感知不到的，由硬件来实现的），此时进程处于的进程上下文会被切换到中断上下文中，从而可以根据中断号去执行相应的中断程序。

通过上面这个例子我们可以发现，进程在执行程序（不管是用户程序，还是内核中的中断程序）时，都会依赖一个上下文，这个上下文由多种数据结构组成，可以提供我们运行时需要的一些数据和保存运行时的一些数据。那其实 context 就可以理解对一个程序运行时所需要的一些数据结构的抽象表达呗。

抽象是个好东西，可以更方便的表达一些东西，更好的设计系统，但大家要想进步也不能停留在抽象层面，要去探索它的真正含义，真正对应的实体。有时间和大家聊一聊抽象应该怎么去理解。

## 2. spring context 是什么

回到 spring 中，spring 的 ioc 容器也是程序呀，那它的执行也肯定需要依赖一个上下文。所以大家应该理解 spring context 的意思了吧。那 spring context 既然是 spring 的上下文，按照我们上面的说法上下文会对应数据结构，那 spring context 的数据结构是什么呢？换句话说，spring context 究竟包括什么？接下来我就把这个抽象的概念给大家对应到实打实的数据结构上。

## 3. spring context 包括什么

主要包括：

- **DefaultListableBeanFactory**

  这就是大家常说的 ioc 容器，它里面有很多 map、list。spring 帮我们创建的 singleton 类型的 bean 就存放在其中一个 map 中。我们定义的监听器（ApplicationListener）也被放到一个 Set 集合中。

- **BeanDefinitionRegistry**

  把一个 BeanDefinition 放到 beanDefinitionMap。

- **AnnotatedBeanDefinitionReader**

  针对 AnnotationConfigApplicationContext 而言。一个 BeanDefinition 读取器。

- **扩展点集合**

  存放 spring 扩展点（主要是 BeanFactoryPostProcessor、BeanPostProcessor）接口的 list 集合。

## 4. spring context 的生命周期

下面大家可以结合代码这段代码去理解 spring context 的生命周期。

```
 public static void main(String[] args) {
     // 初始化和启动
     AnnotationConfigApplicationContext acaContext = new AnnotationConfigApplicationContext(AppConfig.class);
     // 运行
     acaContext.getBean(ServiceA.class);
     // 关闭/销毁
     acaContext.close();
 }
```

### 4.1 初始化和启动

我们平时常说的spring 启动其实就是调用 `AbstractApplicationContext#refresh` 完成 spring context 的初始化和启动过程。spring context 初始化从开始到最后结束以及启动，这整个过程都在 refresh 这个方法中。refresh 方法刚开始做的是一些 spring context 的准备工作，也就是 spring context 的初始化，比如：创建 BeanFactory、注册 BeanFactoryPostProcessor 等，只有等这些准备工作做好以后才去开始 spring context 的启动。

与现实生活联系一下，你可以把初始化理解为准备原料（对应到编程中就是创建好一些数据结构，并为这些数据结构填充点数据进去），等准备了你才能去真正造玩偶、造东西呀（对应到编程中就是执行算法）。在编程中数据结构与算法是分不开的也是这个道理呀，它们相互依赖并没有严格的界限划分。

### 4.2 运行

spring context 启动后可以提供它的服务的这段时间。

### 4.3 关闭/销毁

不需要用 spring context ，关闭它时，其实对应到代码上就是  `acaContext.close();`

## 5.应用场景

对于上下文抽象接口，Spring也为我们提供了多种类型的容器实现，供我们在不同的应用场景选择

- **AnnotationConfigApplicationContext**：从一个或多个基于java的配置类中加载上下文定义，适用于java注解的方式；

- **ClassPathXmlApplicationContext**：从类路径下的一个或多个xml配置文件中加载上下文定义，适用于xml配置的方式；

- **FileSystemXmlApplicationContext**：从文件系统下的一个或多个xml配置文件中加载上下文定义，也就是说系统盘符中加载xml配置文件；

- **AnnotationConfigWebApplicationContext**：专门为web应用准备的，适用于注解方式；

- **XmlWebApplicationContext**：从web应用下的一个或多个xml配置文件加载上下文定义，适用于xml配置方式。

有了以上理解，问题就很好办了。你只要将你需要IOC容器替你管理的对象基于xml也罢，java注解也好，总之你要将需要管理的对象（Spring中我们都称之问bean）、bean之间的协作关系配置好，然后利用应用上下文对象加载进我们的Spring容器，容器就能为你的程序提供你想要的对象管理服务了。下面，还是贴一下简单的应用上下文的应用实例：

- 我们先采用xml配置的方式配置bean和建立bean之间的协作关系：

  ```
  <?xml version="1.0" encoding="UTF-8"?>
  <beans xmlns="http://www.springframework.org/schema/beans"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://www.springframework.org/schema/beans
             http://www.springframework.org/schema/beans/spring-beans-3.2.xsd">
      <bean id="man" class="spring.chapter1.domain.Man">
          <constructor-arg ref="qqCar" />
      </bean>
      <bean  id="qqCar" class="spring.chapter1.domain.QQCar"/>
  </beans>
  ```

  然后通过**应用上下文**将配置加载到IOC容器，让Spring替我们管理对象，待我们需要使用对象的时候，再从容器中获取bean就ok了：

  ```
  public class Test {
      public static void main(String[] args) {
          //加载项目中的spring配置文件到容器
  //        ApplicationContext context = new ClassPathXmlApplicationContext("resouces/applicationContext.xml");
          //加载系统盘中的配置文件到容器
          ApplicationContext context = new FileSystemXmlApplicationContext("E:/Spring/applicationContext.xml");
          //从容器中获取对象实例
          Man man = context.getBean(Man.class);
          man.driveCar();
      }
  }
  ```

- 以上测试中，我将配置文件applicationContext.xml分别放在项目中和任意的系统盘符下，我只需要使用相应的上下文对象去加载配置文件，最后的结果是完全一样的。当然，现在项目中越来越多的使用java注解，所以注解的方式必不可少：

  ```
  //同xml一样描述bean以及bean之间的依赖关系
  @Configuration
  public class ManConfig {
      @Bean
      public Man man() {
          return new Man(car());
      }
      @Bean
      public Car car() {
          return new QQCar();
      }
  }
  ```

  ```
  public class Test {
      public static void main(String[] args) {
          //从java注解的配置中加载配置到容器
          ApplicationContext context = new AnnotationConfigApplicationContext(ManConfig.class);
          //从容器中获取对象实例
          Man man = context.getBean(Man.class);
          man.driveCar();
      }
  }
  ```

  