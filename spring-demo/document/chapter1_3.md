[toc]



# Spring 核心容器

## Spring 核心容器理解

spring容器可以理解为生产对象（OBJECT）的地方，在这里容器不只是帮我们创建了对象那么简单，它负责了对象的整个生命周期--创建、装配、销毁。而这里对象的创建管理的控制权都交给了Spring容器，所以这是一种控制权的反转，称为IOC容器，而这里IOC容器不只是Spring才有，很多框架也都有该技术。



## IOC

### 依赖倒置

假设我们设计一辆汽车：先设计轮子，然后根据轮子大小设计底盘，接着根据底盘设计车身，最后根据车身设计好整个汽车。这里就出现了一个“依赖”关系：汽车依赖车身，车身依赖底盘，底盘依赖轮子。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329205726.webp)

这样的设计看起来没问题，但是可维护性却很低。

假设设计完工之后，上司却突然说根据市场需求的变动，要我们把车子的轮子设计都改大一码。这下我们就蛋疼了：因为我们是根据轮子的尺寸设计的底盘，轮子的尺寸一改，底盘的设计就得修改；同样因为我们是根据底盘设计的车身，那么车身也得改，同理汽车设计也得改——整个设计几乎都得改！

我们现在换一种思路。我们先设计汽车的大概样子，然后根据汽车的样子来设计车身，根据车身来设计底盘，最后根据底盘来设计轮子。这时候，依赖关系就倒置过来了：轮子依赖底盘， 底盘依赖车身， 车身依赖汽车。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329205847.webp)

这时候，上司再说要改动轮子的设计，我们就只需要改动轮子的设计，而不需要动底盘，车身，汽车的设计了。这就是依赖倒置原则——把原本的高层建筑依赖底层建筑“倒置”过来，变成底层建筑依赖高层建筑。**高层建筑决定需要什么，底层去实现这样的需求，但是高层并不用管底层是怎么实现的。**这样就不会出现前面的“牵一发动全身”的情况。

### 控制反转（Inversion of Control）

就是依赖倒置原则的一种代码设计的思路。具体采用的方法就是所谓的依赖注入（Dependency Injection）。其实这些概念初次接触都会感到云里雾里的。说穿了，这几种概念的关系大概如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329205927.webp)

为了理解这几个概念，我们还是用上面汽车的例子。只不过这次换成代码。我们先定义四个Class，车，车身，底盘，轮胎。然后初始化这辆车，最后跑这辆车。代码结构如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329205952.webp)

这样，就相当于上面第一个例子，上层建筑依赖下层建筑——每一个类的构造函数都直接调用了底层代码的构造函数。假设我们需要改动一下轮胎（Tire）类，把它的尺寸变成动态的，而不是一直都是30。我们需要这样改：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210032.webp)

由于我们修改了轮胎的定义，为了让整个程序正常运行，我们需要做以下改动：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210038.webp)

由此我们可以看到，仅仅是为了修改轮胎的构造函数，这种设计却需要修改整个上层所有类的构造函数！在软件工程中，这样的设计几乎是不可维护的——在实际工程项目中，有的类可能会是几千个类的底层，如果每次修改这个类，我们都要修改所有以它作为依赖的类，那软件的维护成本就太高了。

所以我们需要进行控制反转（IoC），即上层控制下层，而不是下层控制着上层。我们用依赖注入（Dependency Injection）这种方式来实现控制反转。所谓依赖注入，就是把底层类作为参数传入上层类，实现上层类对下层类的“控制”。这里我们用构造方法传递的依赖注入方式重新写车类的定义：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210047.webp)

这里我们再把轮胎尺寸变成动态的，同样为了让整个系统顺利运行，我们需要做如下修改：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210118.webp)

看到没？这里我只需要修改轮胎类就行了，不用修改其他任何上层类。这显然是更容易维护的代码。不仅如此，在实际的工程中，这种设计模式还有利于不同组的协同合作和单元测试：比如开发这四个类的分别是四个不同的组，那么只要定义好了接口，四个不同的组可以同时进行开发而不相互受限制；而对于单元测试，如果我们要写Car类的单元测试，就只需要Mock一下Framework类传入Car就行了，而不用把Framework, Bottom, Tire全部new一遍再来构造Car。

这里我们是采用的构造函数传入的方式进行的依赖注入。其实还有另外两种方法：Setter传递和接口传递。这里就不多讲了，核心思路都是一样的，都是为了实现控制反转。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210130.webp)

### 控制反转容器(IoC Container)

其实上面的例子中，对车类进行初始化的那段代码发生的地方，就是控制反转容器。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210141.webp)

显然你也应该观察到了，因为采用了依赖注入，在初始化的过程中就不可避免的会写大量的new。这里IoC容器就解决了这个问题。这个容器可以自动对你的代码进行初始化，你只需要维护一个Configuration（可以是xml可以是一段代码），而不用每次初始化一辆车都要亲手去写那一大段初始化的代码。

这是引入IoC Container的第一个好处。IoC Container的第二个好处是：我们在创建实例的时候不需要了解其中的细节。在上面的例子中，我们自己手动创建一个车instance时候，是从底层往上层new的：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210220.webp)

这个过程中，我们需要了解整个Car/Framework/Bottom/Tire类构造函数是怎么定义的，才能一步一步new/注入。而IoC Container在进行这个工作的时候是反过来的，它先从最上层开始往下找依赖关系，到达最底层之后再往上一步一步new（有点像深度优先遍历）：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210225.webp)

IoC Container可以直接隐藏具体的创建实例的细节。



## DI（依赖注入）

依赖注入（Dependency Injection，DI）和控制反转含义相同，它们是从两个角度描述的同一个概念。

当某个 Java实例需要另一个 Java 实例时，传统的方法是由调用者创建被调用者的实例（例如，使用 new 关键字获得被调用者实例），而使用 Spring 框架后，被调用者的实例不再由调用者创建，而是由 Spring 容器创建，这称为控制反转。

Spring 容器在创建被调用者的实例时，会自动将调用者需要的对象实例注入给调用者，这样，调用者通过 Spring 容器获得被调用者实例，这称为依赖注入。

依赖注入主要有两种实现方式，分别是属性 setter 注入和构造方法注入。具体介绍如下。

- **属性 setter 注入**

  指 IoC 容器使用 setter 方法注入被依赖的实例。通过调用无参构造器或无参 static 工厂方法实例化 bean 后，调用该 bean 的 setter 方法，即可实现基于 setter 的 DI。

- **构造方法注入**

  指 IoC 容器使用构造方法注入被依赖的实例。基于构造器的 DI 通过调用带参数的构造方法实现，每个参数代表一个依赖。

下面通过属性 setter 注入的案例演示 Spring 容器是如何实现依赖注入的。具体步骤如下。

1. **创建 PersonService 接口**

   在 springDemo01 项目的 com.mengma.ioc 包下创建一个名为 PersonService 的接口，该接口中包含一个 addPerson() 方法，如下所示。

    ```
    package com.mengma.ioc;

    public interface PersonService {
        public void addPerson();
    }
    ```

2. **创建接口实现类 PersonServiceImpl**

   在 com.mengma.ioc 包下创建一个名为 PersonServiceImpl 的类，该类实现了 PersonService 接口，如下所示。

    ```
   package com.mengma.ioc;
   
   public class PersonServiceImpl implements PersonService {
   
       // 定义接口声明
       private PersonDao personDao;
   
       // 提供set()方法，用于依赖注入
       public void setPersonDao(PersonDao personDao) {
           this.personDao = personDao;
       }
   
       // 实现PersonService接口的方法
       @Override
       public void addPerson() {
           personDao.add(); // 调用PersonDao中的add()方法
           System.out.println("addPerson()执行了...");
       }
   
   }
    ```
   
   上述代码中，首先声明了 personDao 对象，并为其添加 setter 方法，用于依赖注入，然后实现了 PersonDao 接口的 addPerson() 方法，并在方法中调用 save() 方法和输出一条语句。

3. **在 applicationContext.xml 中添加配置信息**

   在 applicationContext.xml 配置文件中添加一个 `<bean>` 元素，用于实例化 PersonServiceImpl 类，并将 personDao 的实例注入到 personService 中，其实现代码如下所示：

    ```
   <bean id="personService" class="com.mengma.ioc.PersonServiceImpl">
       <!-- 将personDao实例注入personService实例中 -->
       <property name="personDao" ref="personDao"/>
   </bean>
    ```

4. **编写测试方法**

   在 FirstTest 类中创建一个名为 test2() 的方法，编辑后如下所示：

    ```
   @Test
   public void test2() {
       // 定义Spring配置文件的路径
       String xmlPath = "applicationContext.xml";
       // 初始化Spring容器，加载配置文件
       ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
               xmlPath);
       // 通过容器获取personService实例
       PersonService personService = (PersonService) applicationContext
               .getBean("personService");
       // 调用personService的addPerson()方法
       personService.addPerson();
   }
    ```

5. **运行项目并查看结果**

   使用 JUnit 测试运行 test2() 方法，运行成功后，控制台的输出结果如下图所示。

   ![运行结果](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329210321.png)
   
   
   从上图的输出结果中可以看出，使用 Spring 容器获取 userService 的实例后，调用了该实例的 addPerson() 方法，在该方法中又调用了 PersonDao 实现类中的 add() 方法，并输出了结果。这就是 Spring 容器属性 setter 注入的方式，也是实际开发中较为常用的一种方式。



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

### 总结

**Spring容器和SpringMVC容器虽然是父容器与子容器的关系，但二者之间具有一定的独立性。具体来说，两个容器基于各自的配置文件分别进行初始化，只有在子容器找不到对应的Bean时，才回去父容器中去找并加载。**