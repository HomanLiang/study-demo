[toc]



# Spring 核心容器

## Spring 核心容器理解

spring容器可以理解为生产对象（OBJECT）的地方，在这里容器不只是帮我们创建了对象那么简单，它负责了对象的整个生命周期--创建、装配、销毁。而这里对象的创建管理的控制权都交给了Spring容器，所以这是一种控制权的反转，称为IOC容器，而这里IOC容器不只是Spring才有，很多框架也都有该技术。



## Spring容器创建对象的三种方式

```javascript
/**
 * spring容器做的事情：
 *    解析spring的配置文件，利用Java的反射机制创建对象
 *
 */
public class testHelloWorld {
    @Test
    public void testHelloWorld(){
        //启动sping容器
        ApplicationContext context=new ClassPathXmlApplicationContext("applicationContext.xml");
        //从spring容器中把对象提取出来
        HelloWorld helloWorld=(HelloWorld)context.getBean("helloWorld");
        helloWorld.sayHello();
        HelloWorld alias=(HelloWorld)context.getBean("三毛");
        alias.sayHello();
    }
    /**
     * 静态工厂
     * 在spring 内部，调用了HelloWorldFactory 内部的   getInstance 内部方法
     * 而该方法的内容，就是创建对象的过程，是由程序员来完成的
     * 这就是静态工厂
     * */
    @Test
    public  void testHelloWorldFactory(){
        ApplicationContext context=new ClassPathXmlApplicationContext("applicationContext.xml");
        HelloWorld helloWorld=(HelloWorld)context.getBean("helloWorldFactory");
        helloWorld.sayHello();
    }
    /**
     * 实例工厂
     *   1.spring容器（beans）创建了一个实例工厂的bean
     *   2.该bean 调用了工厂方法的getInstance 方法产生对象
     * */
    @Test
    public void testHelloWorldFactory2(){
        ApplicationContext context=new ClassPathXmlApplicationContext("applicationContext.xml");
        HelloWorld helloWorld=(HelloWorld)context.getBean("helloWorld3");
        helloWorld.sayHello();
    }
```

```javascript
public class HelloWorld {
    public  HelloWorld(){
        System.out.println("spring 在默认的情况下，使用默认的构造函数");
    }
    public void sayHello(){
        System.out.println("hello");
    }
}
```

```javascript
public class HelloWorldFactory {
    public static  HelloWorld getInstance(){
        return new HelloWorld();
    }
}
```

```javascript
public class HelloWorldFactory2 {
      public  HelloWorld getInstance(){
          return new HelloWorld();
      }
}
```

```javascript
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
           http://www.springframework.org/schema/beans/spring-beans-2.5.xsd">
    <!-- 
        beans  存放了很多个类
            把一个类放入到spring容器中，该类就是bean
     -->
     <!-- 
        一个bean就是描述一个类
          id就是标示符
                命名规范：类的第一个字母变成小写，其他的字母保持不变
          class为类的全名
      -->
    <bean id="helloWorld" class="com.sanmao.spring.ioc.HelloWorld"></bean>
    <!--alias  别名-->
    <alias name="helloWorld" alias="三毛"></alias>


    <!--静态工厂-->
    <bean id="helloWorldFactory" class="com.sanmao.spring.ioc.HelloWorldFactory"
                 factory-method="getInstance"></bean>


    <!--实例工厂-->
    <bean id="helloWorldFactory2" class="com.sanmao.spring.ioc.HelloWorldFactory2">
    </bean>
    <!--factory-bean  指向实例工厂的bean-->
    <!--factory-bean 实例工厂对象的方法-->
    <bean  id="helloWorld3" factory-bean="helloWorldFactory2" factory-method="getInstance"></bean>

</beans>
```



## Spring父子容器问题

这个问题老早就存在了，只是今天组长让我看AOP不生效的时候，才真实遇到这个问题，之前都是用的Spring Boot开发，不会存在这个问题。

### 问题描述

如果使用传统的方式来开发Spring项目，要部署在Tomcat上面，一般会依赖Spring与Spring MVC，在Tomcat的web.xml中会配置一个加载service的配置文件，这个在Tomcat启动的时候会进行加载，会生成一个Spring的容器。

默认情况下，Tomcat会在资源目录下加载配置servlet名称的另外一个xml配置文件，比如servlet名称为test，那么会加载test-servlet.xml配置文件，如果使用Spring MVC的话，会解析这个配置文件并且再生成一个Spring的容器，同时设置Tomcat启动时创建的那个容器为父容器。

![image-20190329172613784](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328234140.webp)



容器分离的影响：

- 一般在父容器中会加载service,dao之类的东西，不加载controller
- 在mvc容器中只加载controller

两个容器负责不同的bean，但是如果在父容器中配置了一些AOP想要处理controller的内容，因为容器的隔离，AOP就不会生效。

因为在解析bean的时候，它会获取BeanPostProcesser(BPP)，不同容器获取的BPP也是不相同的，理所当然在子容器解析Controller的bean时候，获取不到父容器配置的BPP，也就无法生成代理对象。

### 解决方案

如果明白为什么会出现父子容器问题，那么可以想到如下方案

- 如果只需要处理service的bean，那么只在父容器的配置文件中操作
- 如果只处理controller的bean，那么在mvc的配置文件中修改
- 如果同时要处理service，还要处理controller，那么在两个配置文件中都进行修改

个人觉得，根据自己的业务处理，尝试重新ApplicationContext的getBeanFactory方法，交给其父容器获得，如果父容器为空，再由自己处理，参考Classloader的双亲委派机制，当然这只是我的一种设想。

```
@Override
public final ConfigurableListableBeanFactory getBeanFactory() {
   if(getParent() != null){
       getParent().getBeanFactory();
   }
   synchronized (this.beanFactoryMonitor) {
      if (this.beanFactory == null) {
         throw new IllegalStateException("BeanFactory not initialized or already closed - " +
               "call 'refresh' before accessing beans via the ApplicationContext");
      }
      return this.beanFactory;
   }
}
```