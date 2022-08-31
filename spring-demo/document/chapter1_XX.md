[toc]



# Spring 面试题

## 1.Spring 用了哪些设计模式？

### 1.1.简单工厂(非23种设计模式中的一种)

**1.1.1.实现方式**：

`BeanFactory`。`Spring` 中的 `BeanFactory` 就是简单工厂模式的体现，根据传入一个唯一的标识来获得 `Bean` 对象，但是否是在传入参数后创建还是传入参数前创建这个要根据具体情况来定。

**1.1.2.实质：**

由一个工厂类根据传入的参数，动态决定应该创建哪一个产品类。

**1.1.3.实现原理：**

**bean容器的启动阶段：**

- 读取 `bean` 的 `xml` 配置文件,将 `bean` 元素分别转换成一个 `BeanDefinition` 对象。

- 然后通过 `BeanDefinitionRegistry` 将这些 `bean` 注册到 `beanFactory` 中，保存在它的一个 `ConcurrentHashMap` 中。

- 将 `BeanDefinition` 注册到了 `beanFactory` 之后，在这里 `Spring` 为我们提供了一个扩展的切口，允许我们通过实现接口`BeanFactoryPostProcessor`  在此处来插入我们定义的代码。

  典型的例子就是：`PropertyPlaceholderConfigurer`，我们一般在配置数据库的 `dataSource` 时使用到的占位符的值，就是它注入进去的。

**容器中bean的实例化阶段：**

实例化阶段主要是通过反射或者 `CGLIB` 对 `bean` 进行实例化，在这个阶段 `Spring` 又给我们暴露了很多的扩展点：

- **各种的Aware接口**，比如 `BeanFactoryAware`，对于实现了这些 `Aware` 接口的 `bean` ，在实例化 `bean` 时 `Spring` 会帮我们注入对应的 `BeanFactory` 的实例。
- **BeanPostProcessor接口**，实现了 `BeanPostProcessor` 接口的 `bean` ，在实例化`bean` 时 `Spring` 会帮我们调用接口中的方法。
- **InitializingBean接口**，实现了 `InitializingBean` 接口的 `bean`，在实例化 `bean` 时 `Spring` 会帮我们调用接口中的方法。
- **DisposableBean接口**，实现了 `BeanPostProcessor` 接口的 `bean`，在该 `bean` 死亡时 `Spring` 会帮我们调用接口中的方法。

**1.1.4.设计意义：**

**松耦合。**可以将原来硬编码的依赖，通过 `Spring` 这个 `beanFactory` 这个工厂来注入依赖，也就是说原来只有依赖方和被依赖方，现在我们引入了第三方—— `spring` 这个 `beanFactory`，由它来解决 `bean` 之间的依赖问题，达到了松耦合的效果.

**bean的额外处理。**通过 `Spring` 接口的暴露，在实例化 `bean` 的阶段我们可以进行一些额外的处理，这些额外的处理只需要让 `bean` 实现对应的接口即可，那么 `spring` 就会在 `bean` 的生命周期调用我们实现的接口来处理该 `bean`。`[非常重要]`

### 1.2.工厂方法

**1.2.1.实现方式：**

`FactoryBean` 接口。

**1.2.2.实现原理：**

实现了 `FactoryBean` 接口的 `bean` 是一类叫做 `factory` 的 `bean`。其特点是，`spring` 会在使用 `getBean()` 调用获得该 `bean` 时，会自动调用该 `bean` 的 `getObject()` 方法，所以返回的不是 `factory` 这个 `bean`，而是这个 `bean.getOjbect()` 方法的返回值。

**1.2.3.例子：**

典型的例子有 `spring` 与 `mybatis` 的结合。

**代码示例：**

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331224653.webp)

**1.2.4.说明：**

我们看上面该 `bean`，因为实现了 `FactoryBean` 接口，所以返回的不是 `SqlSessionFactoryBean` 的实例，而是它的 `SqlSessionFactoryBean.getObject()` 的返回值。

### 1.3.单例模式

`Spring`依赖注入 `Bean` 实例默认是单例的。

`Spring` 的依赖注入（包括 `lazy-init` 方式）都是发生在 `AbstractBeanFactory` 的 `getBean` 里。`getBean` 的 `doGetBean` 方法调用`getSingleton` 进行 `bean` 的创建。

分析 `getSingleton()` 方法

```
public Object getSingleton(String beanName){
    //参数true设置标识允许早期依赖
    return getSingleton(beanName,true);
}
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    //检查缓存中是否存在实例
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        //如果为空，则锁定全局变量并进行处理。
        synchronized (this.singletonObjects) {
            //如果此bean正在加载，则不处理
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {  
                //当某些方法需要提前初始化的时候则会调用addSingleFactory 方法将对应的ObjectFactory初始化策略存储在singletonFactories
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    //调用预先设定的getObject方法
                    singletonObject = singletonFactory.getObject();
                    //记录在缓存中，earlysingletonObjects和singletonFactories互斥
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return (singletonObject != NULL_OBJECT ? singletonObject : null);
}
```

**getSingleton()过程图**

ps：spring依赖注入时，使用了 双重判断加锁 的单例模式

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331224237.webp)

**总结**

**单例模式定义：**保证一个类仅有一个实例，并提供一个访问它的全局访问点。

**spring对单例的实现：**`spring` 中的单例模式完成了后半句话，即提供了全局的访问点 `BeanFactory`。但没有从构造器级别去控制单例，这是因为 `spring` 管理的是任意的 `java` 对象。

### 1.4.适配器模式

**1.4.1.实现方式：**

`SpringMVC` 中的适配器 `HandlerAdatper`。

**1.4.2.实现原理：**

`HandlerAdatper` 根据 `Handler` 规则执行不同的 `Handler`。

**1.4.3.实现过程：**

`DispatcherServlet` 根据 `HandlerMapping` 返回的 `handler`，向 `HandlerAdatper` 发起请求，处理 `Handler`。

`HandlerAdapter` 根据规则找到对应的 `Handler` 并让其执行，执行完毕后 `Handler` 会向 `HandlerAdapter` 返回一个 `ModelAndView`，最后由 `HandlerAdapter` 向 `DispatchServelet` 返回一个 `ModelAndView`。

**1.4.4.实现意义：**

`HandlerAdatper` 使得 `Handler` 的扩展变得容易，只需要增加一个新的 `Handler` 和一个对应的 `HandlerAdapter` 即可。

因此 `Spring` 定义了一个适配接口，使得每一种 `Controller` 有一种对应的适配器实现类，让适配器代替 `controller` 执行相应的方法。这样在扩展 `Controller` 时，只需要增加一个适配器类就完成了 `SpringMVC` 的扩展了。

### 1.5.装饰器模式

**1.5.1.实现方式：**

`Spring` 中用到的包装器模式在类名上有两种表现：一种是类名中含有 `Wrapper`，另一种是类名中含有 `Decorator`。

**1.5.2.实质：**

动态地给一个对象添加一些额外的职责。

就增加功能来说，`Decorator` 模式相比生成子类更为灵活。

### 1.6.代理模式

**1.6.1.实现方式：**

`AOP` 底层，就是动态代理模式的实现。

**1.6.2.动态代理：**

在内存中构建的，不需要手动编写代理类

**1.6.3.静态代理：**

需要手工编写代理类，代理类引用被代理对象。

**1.6.4.实现原理：**

切面在应用运行的时刻被织入。一般情况下，在织入切面时，AOP容器会为目标对象创建动态的创建一个代理对象。SpringAOP就是以这种方式织入切面的。

织入：把切面应用到目标对象并创建新的代理对象的过程。

### 1.7.观察者模式

**1.7.1.实现方式：**

`spring` 的事件驱动模型使用的是 观察者模式 ，`Spring` 中 `Observer` 模式常用的地方是 `listener` 的实现。

**1.7.2.具体实现：**

事件机制的实现需要三个部分,事件源,事件,事件监听器

`ApplicationEvent` 抽象类`[事件]`

继承自 `jdk` 的 `EventObject`,所有的事件都需要继承 `ApplicationEvent`,并且通过构造器参数 `source` 得到事件源.

该类的实现类 `ApplicationContextEvent` 表示 `ApplicaitonContext` 的容器事件.

代码：

```
public abstract class ApplicationEvent extends EventObject {
    private static final long serialVersionUID = 7099057708183571937L;
    private final long timestamp;
    public ApplicationEvent(Object source) {
    super(source);
    this.timestamp = System.currentTimeMillis();
    }
    public final long getTimestamp() {
        return this.timestamp;
    }
}
```

`ApplicationListener` 接口`[事件监听器]`

继承自 `jdk` 的 `EventListener`,所有的监听器都要实现这个接口。

这个接口只有一个 `onApplicationEvent()` 方法,该方法接受一个 `ApplicationEvent` 或其子类对象作为参数,在方法体中,可以通过不同对Event类的判断来进行相应的处理。

当事件触发时所有的监听器都会收到消息。

代码：

```
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {
     void onApplicationEvent(E event);
} 
```

`ApplicationContext` 接口`[事件源]`

`ApplicationContext` 是 `spring` 中的全局容器，翻译过来是”应用上下文”。

实现了 `ApplicationEventPublisher` 接口。

**1.7.3.职责：**

负责读取 `bean` 的配置文档,管理 `bean` 的加载,维护 `bean` 之间的依赖关系,可以说是负责 `bean` 的整个生命周期,再通俗一点就是我们平时所说的 `IOC` 容器。

代码：

```
public interface ApplicationEventPublisher {
        void publishEvent(ApplicationEvent event);
}   

public void publishEvent(ApplicationEvent event) {
    Assert.notNull(event, "Event must not be null");
    if (logger.isTraceEnabled()) {
         logger.trace("Publishing event in " + getDisplayName() + ": " + event);
    }
    getApplicationEventMulticaster().multicastEvent(event);
    if (this.parent != null) {
    this.parent.publishEvent(event);
    }
}
```

`ApplicationEventMulticaster` 抽象类`[事件源中publishEvent方法需要调用其方法getApplicationEventMulticaster]`

属于事件广播器,它的作用是把 `Applicationcontext` 发布的 `Event`广播给所有的监听器.

代码：

```
public abstract class AbstractApplicationContext extends DefaultResourceLoader
    implements ConfigurableApplicationContext, DisposableBean {  
    private ApplicationEventMulticaster applicationEventMulticaster;  
    protected void registerListeners() {  
    // Register statically specified listeners first.  
    for (ApplicationListener<?> listener : getApplicationListeners()) {  
    getApplicationEventMulticaster().addApplicationListener(listener);  
    }  
    // Do not initialize FactoryBeans here: We need to leave all regular beans  
    // uninitialized to let post-processors apply to them!  
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);  
    for (String lisName : listenerBeanNames) {  
    getApplicationEventMulticaster().addApplicationListenerBean(lisName);  
    }  
  }  
}
```

### 1.8.策略模式

**1.8.1.实现方式：**

`Spring` 框架的资源访问 `Resource` 接口。该接口提供了更强的资源访问能力，`Spring` 框架本身大量使用了 `Resource` 接口来访问底层资源。

**1.8.2.Resource 接口介绍**

`source` 接口是具体资源访问策略的抽象，也是所有资源访问类所实现的接口。

`Resource` 接口主要提供了如下几个方法:

- **getInputStream()：**定位并打开资源，返回资源对应的输入流。每次调用都返回新的输入流。调用者必须负责关闭输入流。
- **exists()：**返回 Resource 所指向的资源是否存在。
- **isOpen()：**返回资源文件是否打开，如果资源文件不能多次读取，每次读取结束应该显式关闭，以防止资源泄漏。
- **getDescription()：**返回资源的描述信息，通常用于资源处理出错时输出该信息，通常是全限定文件名或实际 URL。
- **getFile：**返回资源对应的 File 对象。
- **getURL：**返回资源对应的 URL 对象。

最后两个方法通常无须使用，仅在通过简单方式访问无法实现时，`Resource` 提供传统的资源访问的功能。

`Resource` 接口本身没有提供访问任何底层资源的实现逻辑，**针对不同的底层资源，Spring 将会提供不同的 Resource 实现类，不同的实现类负责不同的资源访问逻辑。**

`Spring` 为 `Resource` 接口提供了如下实现类：

- **UrlResource：**访问网络资源的实现类。
- **ClassPathResource：**访问类加载路径里资源的实现类。
- **FileSystemResource：**访问文件系统里资源的实现类。
- **ServletContextResource：**访问相对于 ServletContext 路径里的资源的实现类.
- **InputStreamResource：**访问输入流资源的实现类。
- **ByteArrayResource：**访问字节数组资源的实现类。

这些 `Resource` 实现类，针对不同的的底层资源，提供了相应的资源访问逻辑，并提供便捷的包装，以利于客户端程序的资源访问。

### 1.9.模版方法模式

**1.9.1.经典模板方法定义：**

父类定义了骨架（调用哪些方法及顺序），某些特定方法由子类实现。

最大的好处：代码复用，减少重复代码。除了子类要实现的特定方法，其他方法及方法调用顺序都在父类中预先写好了。

**所以父类模板方法中有两类方法：**

**共同的方法：**所有子类都会用到的代码

**不同的方法：**子类要覆盖的方法，分为两种：

- 抽象方法：父类中的是抽象方法，子类必须覆盖
- 钩子方法：父类中是一个空方法，子类继承了默认也是空的

注：为什么叫钩子，子类可以通过这个钩子（方法），控制父类，因为这个钩子实际是父类的方法（空方法）！

**1.9.2.Spring模板方法模式实质：**

是模板方法模式和回调模式的结合，是 `Template Method` 不需要继承的另一种实现方式。`Spring` 几乎所有的外接扩展都采用这种模式。

**1.9.3.具体实现：**

`JDBC` 的抽象和对 `Hibernate` 的集成，都采用了一种理念或者处理方式，那就是模板方法模式与相应的 `Callback` 接口相结合。

采用模板方法模式是为了以一种统一而集中的方式来处理资源的获取和释放，以 `JdbcTempalte` 为例:

```
public abstract class JdbcTemplate {  
     public final Object execute（String sql）{  
        Connection con=null;  
        Statement stmt=null;  
        try{  
            con=getConnection（）;  
            stmt=con.createStatement（）;  
            Object retValue=executeWithStatement（stmt,sql）;  
            return retValue;  
        }catch（SQLException e）{  
             ...  
        }finally{  
            closeStatement（stmt）;  
            releaseConnection（con）;  
        }  
    }   
    protected abstract Object executeWithStatement（Statement   stmt, String sql）;  
}  
```

**1.9.4.引入回调原因：**

`JdbcTemplate` 是抽象类，不能够独立使用，我们每次进行数据访问的时候都要给出一个相应的子类实现,这样肯定不方便，所以就引入了回调。

回调代码

```
public interface StatementCallback{  
    Object doWithStatement（Statement stmt）;  
}   
```

利用回调方法重写JdbcTemplate方法

```
public class JdbcTemplate {  
    public final Object execute（StatementCallback callback）{  
        Connection con=null;  
        Statement stmt=null;  
        try{  
            con=getConnection（）;  
            stmt=con.createStatement（）;  
            Object retValue=callback.doWithStatement（stmt）;  
            return retValue;  
        }catch（SQLException e）{  
            ...  
        }finally{  
            closeStatement（stmt）;  
            releaseConnection（con）;  
        }  
    }  

    ...//其它方法定义  
}   
```

Jdbc使用方法如下：

```
JdbcTemplate jdbcTemplate=...;  
    final String sql=...;  
    StatementCallback callback=new StatementCallback(){  
    public Object=doWithStatement(Statement stmt){  
        return ...;  
    }  
}    
jdbcTemplate.execute(callback);  
```

**1.9.5.为什么JdbcTemplate没有使用继承？**

因为这个类的方法太多，但是我们还是想用到 `JdbcTemplate` 已有的稳定的、公用的数据库连接，那么我们怎么办呢？

我们可以把变化的东西抽出来作为一个参数传入 `JdbcTemplate` 的方法中。但是变化的东西是一段代码，而且这段代码会用到`JdbcTemplate` 中的变量。怎么办？

那我们就用回调对象吧。在这个回调对象中定义一个操纵 `JdbcTemplate` 中变量的方法，我们去实现这个方法，就把变化的东西集中到这里了。然后我们再传入这个回调对象到 `JdbcTemplate`，从而完成了调用。



## 2.Spring中的循环依赖

### 2.1.前言

循环依赖问题，算是一道烂大街的面试题了，解毒之前，我们先来回顾两个知识点：

初学 `Spring` 的时候，我们就知道 `IOC`，控制反转么，它将原本在程序中手动创建对象的控制权，交由 `Spring` 框架来管理，不需要我们手动去各种 `new XXX`。

尽管是 `Spring` 管理，不也得创建对象吗， Java 对象的创建步骤很多，可以 `new XXX`、序列化、`clone()` 等等， 只是 `Spring` 是通过反射 + 工厂的方式创建对象并放在容器的，创建好的对象我们一般还会对对象属性进行赋值，才去使用，可以理解是分了两个步骤。

好了，对这两个步骤有个印象就行，接着我们进入循环依赖，先说下循环依赖的概念

#### 2.1.1.什么是循环依赖

所谓的循环依赖是指，A 依赖 B，B 又依赖 A，它们之间形成了循环依赖。或者是 A 依赖 B，B 依赖 C，C 又依赖 A，形成了循环依赖。更或者是自己依赖自己。它们之间的依赖关系如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331231728.png)

这里以两个类直接相互依赖为例，他们的实现代码可能如下：

```java
public class BeanB {
    private BeanA beanA;
    public void setBeanA(BeanA beanA) {
		this.beanA = beanA;
	}
}

public class BeanA {
    private BeanB beanB;
    public void setBeanB(BeanB beanB) {
        this.beanB = beanB;
	}
}
```

配置信息如下（用注解方式注入同理，只是为了方便理解，用了配置文件）：

```java
<bean id="beanA" class="priv.starfish.BeanA">
  <property name="beanB" ref="beanB"/>
</bean>

<bean id="beanB" class="priv.starfish.BeanB">
  <property name="beanA" ref="beanA"/>
</bean>
```

`Spring` 启动后，读取如上的配置文件，会按顺序先实例化 A，但是创建的时候又发现它依赖了 B，接着就去实例化 B ，同样又发现它依赖了 A ，这尼玛咋整？无限循环呀

`Spring` “肯定”不会让这种事情发生的，如前言我们说的 `Spring` 实例化对象分两步，第一步会先创建一个原始对象，只是没有设置属性，可以理解为"半成品"—— 官方叫 A 对象的早期引用（`EarlyBeanReference`），所以当实例化 B 的时候发现依赖了 A， B 就会把这个“半成品”设置进去先完成实例化，既然 B 完成了实例化，所以 A 就可以获得 B 的引用，也完成实例化了，这其实就是 Spring 解决循环依赖的思想。

不理解没关系，先有个大概的印象，然后我们从源码来看下 Spring 具体是怎么解决的。

### 2.2.源码解毒

> 代码版本：5.0.16.RELEASE

在 `Spring IOC` 容器读取 `Bean` 配置创建 `Bean` 实例之前, 必须对它进行实例化。只有在容器实例化后，才可以从 `IOC` 容器里获取 `Bean` 实例并使用，循环依赖问题也就是发生在实例化 Bean 的过程中的，所以我们先回顾下获取 `Bean` 的过程。

#### 2.2.1.获取 Bean 流程

`Spring IOC` 容器中获取 `bean` 实例的简化版流程如下（排除了各种包装和检查的过程）

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331231947.png)

大概的流程顺序（可以结合着源码看下，我就不贴了，贴太多的话，呕~呕呕，想吐）：

1. 流程从 `getBean` 方法开始，`getBean` 是个空壳方法，所有逻辑直接到 `doGetBean` 方法中
2. `transformedBeanName` 将 `name` 转换为真正的 `beanName`（`name` 可能是 `FactoryBean` 以 `&` 字符开头或者有别名的情况，所以需要转化下）
3. 然后通过 `getSingleton(beanName)` 方法尝试从缓存中查找是不是有该实例 `sharedInstance`（单例在 `Spring` 的同一容器只会被创建一次，后续再获取 `bean`，就直接从缓存获取即可）
4. 如果有的话，`sharedInstance` 可能是完全实例化好的 `bean`，也可能是一个原始的 `bean`，所以再经 `getObjectForBeanInstance` 处理即可返回
5. 当然 `sharedInstance` 也可能是 `null`，这时候就会执行创建 `bean` 的逻辑，将结果返回

第三步的时候我们提到了一个缓存的概念，这个就是 Spring 为了解决单例的循环依赖问题而设计的 **三级缓存**

```java
/** Cache of singleton objects: bean name --> bean instance */
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

/** Cache of singleton factories: bean name --> ObjectFactory */
private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>(16);

/** Cache of early singleton objects: bean name --> bean instance */
private final Map<String, Object> earlySingletonObjects = new HashMap<>(16);
```

这三级缓存的作用分别是：

- `singletonObjects`：完成初始化的单例对象的 `cache`，这里的 `bean` 经历过 `实例化->属性填充->初始化` 以及各种后置处理（一级缓存）
- `earlySingletonObjects`：存放原始的 `bean` 对象（**完成实例化但是尚未填充属性和初始化**），仅仅能作为指针提前曝光，被其他 `bean` 所引用，用于解决循环依赖的 （二级缓存）
- `singletonFactories`：在 `bean` 实例化完之后，属性填充以及初始化之前，如果允许提前曝光，`Spring` 会将实例化后的 `bean` 提前曝光，也就是把该 `bean` 转换成 `beanFactory` 并加入到 `singletonFactories`（三级缓存）

我们首先从缓存中试着获取 `bean`，就是从这三级缓存中查找

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 从 singletonObjects 获取实例，singletonObjects 中的实例都是准备好的 bean 实例，可以直接使用
    Object singletonObject = this.singletonObjects.get(beanName);
    //isSingletonCurrentlyInCreation() 判断当前单例bean是否正在创建中
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            // 一级缓存没有，就去二级缓存找
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                // 二级缓存也没有，就去三级缓存找
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 三级缓存有的话，就把他移动到二级缓存,.getObject() 后续会讲到
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}
```

如果缓存没有的话，我们就要创建了，接着我们以单例对象为例，再看下创建 `bean` 的逻辑（大括号表示内部类调用方法）：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331232526.png)

1. 创建 `bean` 从以下代码开始，一个匿名内部类方法参数（总觉得 `Lambda` 的方式可读性不如内部类好理解）

   ```java
   if (mbd.isSingleton()) {
       sharedInstance = getSingleton(beanName, () -> {
           try {
               return createBean(beanName, mbd, args);
           }
           catch (BeansException ex) {
               destroySingleton(beanName);
               throw ex;
           }
       });
       bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
   }
   ```

   `getSingleton()` 方法内部主要有两个方法

   ```java
   public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
       // 创建 singletonObject
   	singletonObject = singletonFactory.getObject();
       // 将 singletonObject 放入缓存
       addSingleton(beanName, singletonObject);
   }
   ```

2. `getObject()` 匿名内部类的实现真正调用的又是 `createBean(beanName, mbd, args)`

3. 往里走，主要的实现逻辑在 `doCreateBean`方法，先通过 `createBeanInstance` 创建一个原始 bean 对象

4. 接着 `addSingletonFactory` 添加 `bean` 工厂对象到  `singletonFactories` 缓存（三级缓存）

5. 通过 `populateBean` 方法向原始 bean 对象中填充属性，并解析依赖，假设这时候创建 A 之后填充属性时发现依赖 B，然后创建依赖对象 B 的时候又发现依赖 A，还是同样的流程，又去 `getBean(A)`，这个时候三级缓存已经有了 beanA 的“半成品”，这时就可以把 A 对象的原始引用注入 B 对象（并将其移动到二级缓存）来解决循环依赖问题。这时候 `getObject()` 方法就算执行结束了，返回完全实例化的 bean

6. 最后调用 `addSingleton` 把完全实例化好的 bean 对象放入 singletonObjects 缓存（一级缓存）中，打完收工

#### 2.2.2.Spring 解决循环依赖

建议搭配着“源码”看下边的逻辑图，更好下饭

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331232553.png)

流程其实上边都已经说过了，结合着上图我们再看下具体细节，用大白话再捋一捋：

1. `Spring` 创建 `bean` 主要分为两个步骤，创建原始 `bean` 对象，接着去填充对象属性和初始化
2. 每次创建 `bean` 之前，我们都会从缓存中查下有没有该 `bean`，因为是单例，只能有一个
3. 当我们创建 `beanA` 的原始对象后，并把它放到三级缓存中，接下来就该填充对象属性了，这时候发现依赖了 `beanB`，接着就又去创建 `beanB`，同样的流程，创建完 `beanB` 填充属性时又发现它依赖了 `beanA`，又是同样的流程，不同的是，这时候可以在三级缓存中查到刚放进去的原始对象 `beanA`，所以不需要继续创建，用它注入 `beanB`，完成 `beanB` 的创建
4. 既然 `beanB` 创建好了，所以 `beanA` 就可以完成填充属性的步骤了，接着执行剩下的逻辑，闭环完成

这就是单例模式下 `Spring` 解决循环依赖的流程了。

但是这个地方，不管是谁看源码都会有个小疑惑，为什么需要三级缓存呢，我赶脚二级他也够了呀

革命尚未成功，同志仍需努力

跟源码的时候，发现在创建 `beanB` 需要引用 `beanA` 这个“半成品”的时候，就会触发"前期引用"，即如下代码：

```java
ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
if (singletonFactory != null) {
    // 三级缓存有的话，就把他移动到二级缓存
    singletonObject = singletonFactory.getObject();
    this.earlySingletonObjects.put(beanName, singletonObject);
    this.singletonFactories.remove(beanName);
}
```

`singletonFactory.getObject()` 是一个接口方法，这里具体的实现方法在

```java
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                // 这么一大段就这句话是核心，也就是当bean要进行提前曝光时，
                // 给一个机会，通过重写后置处理器的getEarlyBeanReference方法，来自定义操作bean
                // 值得注意的是，如果提前曝光了，但是没有被提前引用，则该后置处理器并不生效!!!
                // 这也正式三级缓存存在的意义，否则二级缓存就可以解决循环依赖的问题
                exposedObject = ibp.getEarlyBeanReference(exposedObject, beanName);
            }
        }
    }
    return exposedObject;
}
```

这个方法就是 `Spring` 为什么使用三级缓存，而不是二级缓存的原因，它的目的是为了后置处理，如果没有 `AOP` 后置处理，就不会走进 `if` 语句，直接返回了 `exposedObject` ，相当于啥都没干，二级缓存就够用了。

所以又得出结论，这个三级缓存应该和 `AOP` 有关系，继续。

在 Spring 的源码中`getEarlyBeanReference` 是 `SmartInstantiationAwareBeanPostProcessor` 接口的默认方法，真正实现这个方法的只有**`AbstractAutoProxyCreator`** 这个类，用于提前曝光的 `AOP` 代理。

```java
@Override
public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
   Object cacheKey = getCacheKey(bean.getClass(), beanName);
   this.earlyProxyReferences.put(cacheKey, bean);
   // 对bean进行提前Spring AOP代理
   return wrapIfNecessary(bean, beanName, cacheKey);
}
```

这么说有点干，来个小 `demo` 吧，我们都知道 **Spring AOP、事务**等都是通过代理对象来实现的，而**事务**的代理对象是由自动代理创建器来自动完成的。也就是说 `Spring` 最终给我们放进容器里面的是一个代理对象，**而非原始对象**，假设我们有如下一段业务代码：

```java
@Service
public class HelloServiceImpl implements HelloService {
   @Autowired
   private HelloService helloService;

   @Override
   @Transactional
   public Object hello() {
      return "Hello JavaKeeper";
   }
}
```

此 `Service` 类使用到了事务，所以最终会生成一个 JDK 动态代理对象 `Proxy`。刚好它又存在**自己引用自己**的循环依赖，完美符合我们的场景需求。

我们再自定义一个后置处理，来看下效果：

```java
@Component
public class HelloProcessor implements SmartInstantiationAwareBeanPostProcessor {

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		System.out.println("提前曝光了："+beanName);
		return bean;
	}
}
```

可以看到，调用方法栈中有我们自己实现的 `HelloProcessor`，说明这个 `bean` 会通过 `AOP` 代理处理。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331232633.png)

再从源码看下这个自己循环自己的 `bean` 的创建流程：

```java
protected Object doCreateBean( ... ){
	...
	
	boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName));
    // 需要提前暴露（支持循环依赖），就注册一个ObjectFactory到三级缓存
	if (earlySingletonExposure) { 
        // 添加 bean 工厂对象到 singletonFactories 缓存中，并获取原始对象的早期引用
		//匿名内部方法 getEarlyBeanReference 就是后置处理器	
		// SmartInstantiationAwareBeanPostProcessor 的一个方法，
		// 它的功效为：保证自己被循环依赖的时候，即使被别的Bean @Autowire进去的也是代理对象
		addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
	}

	// 此处注意：如果此处自己被循环依赖了  那它会走上面的getEarlyBeanReference，从而创建一个代理对象从		三级缓存转移到二级缓存里
	// 注意此时候对象还在二级缓存里，并没有在一级缓存。并且此时后续的这两步操作还是用的 exposedObject，它仍旧是原始对象~~~
	populateBean(beanName, mbd, instanceWrapper);
	exposedObject = initializeBean(beanName, exposedObject, mbd);

	// 因为事务的AOP自动代理创建器在getEarlyBeanReference 创建代理后，initializeBean 就不会再重复创建了，二选一的）
    	
	// 所以经过这两大步后，exposedObject 还是原始对象，通过 getEarlyBeanReference 创建的代理对象还在三级缓存呢
	
	...
	
	// 循环依赖校验
	if (earlySingletonExposure) {
        // 注意此处第二个参数传的false，表示不去三级缓存里再去调用一次getObject()方法了~~~，此时代理对象还在二级缓存，所以这里拿出来的就是个 代理对象
		// 最后赋值给exposedObject  然后return出去，进而最终被addSingleton()添加进一级缓存里面去  
		// 这样就保证了我们容器里 最终实际上是代理对象，而非原始对象~~~~~
		Object earlySingletonReference = getSingleton(beanName, false);
		if (earlySingletonReference != null) {
			if (exposedObject == bean) { 
				exposedObject = earlySingletonReference;
			}
		}
		...
	}
	
}
```

**自我解惑：**

**问：还是不太懂，为什么这么设计呢，即使有代理，在二级缓存代理也可以吧 | 为什么要使用三级缓存呢？**

我们再来看下相关代码，假设我们现在是二级缓存架构，创建 A 的时候，我们不知道有没有循环依赖，所以放入二级缓存提前暴露，接着创建 B，也是放入二级缓存，这时候发现又循环依赖了 A，就去二级缓存找，是有，但是如果此时还有 AOP 代理呢，我们要的是代理对象可不是原始对象，这怎么办，只能改逻辑，在第一步的时候，不管3721，所有 Bean 统统去完成 AOP 代理，如果是这样的话，就不需要三级缓存了，但是这样不仅没有必要，而且违背了 Spring 在结合 `AOP` 跟 Bean 的生命周期的设计。

所以 Spring “多此一举”的将实例先封装到 ObjectFactory 中（三级缓存），主要关键点在 `getObject()` 方法并非直接返回实例，而是对实例又使用 `SmartInstantiationAwareBeanPostProcessor` 的 `getEarlyBeanReference` 方法对 bean 进行处理，也就是说，当 Spring 中存在该后置处理器，所有的单例 bean 在实例化后都会被进行提前曝光到三级缓存中，但是并不是所有的 bean 都存在循环依赖，也就是三级缓存到二级缓存的步骤不一定都会被执行，有可能曝光后直接创建完成，没被提前引用过，就直接被加入到一级缓存中。因此可以确保只有提前曝光且被引用的 bean 才会进行该后置处理。

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
             // 三级缓存获取，key=beanName value=objectFactory，objectFactory中存储					//getObject()方法用于获取提前曝光的实例
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 三级缓存有的话，就把他移动到二级缓存
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}


boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
      isSingletonCurrentlyInCreation(beanName));
if (earlySingletonExposure) {
   if (logger.isDebugEnabled()) {
      logger.debug("Eagerly caching bean '" + beanName +
            "' to allow for resolving potential circular references");
   }
   // 添加 bean 工厂对象到 singletonFactories 缓存中，并获取原始对象的早期引用
   //匿名内部方法 getEarlyBeanReference 就是后置处理器
   // SmartInstantiationAwareBeanPostProcessor 的一个方法，
   // 它的功效为：保证自己被循环依赖的时候，即使被别的Bean @Autowire进去的也是代理对象~~~~  AOP自动代理创建器此方法里会创建的代理对象~~~
   addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
}
```

**再问：AOP 代理对象提前放入了三级缓存，没有经过属性填充和初始化，这个代理又是如何保证依赖属性的注入的呢？**

这个又涉及到了 `Spring` 中动态代理的实现，不管是`cglib`代理还是`jdk`动态代理生成的代理类，代理时，会将目标对象 `target` 保存在最后生成的代理 `$proxy` 中，当调用 `$proxy` 方法时会回调 `h.invoke`，而 `h.invoke` 又会回调目标对象 `target` 的原始方法。所有，其实在 `AOP` 动态代理时，原始 `bean` 已经被保存在 **提前曝光代理**中了，之后 `原始 bean` 继续完成`属性填充`和`初始化`操作。因为 `AOP` 代理`$proxy`中保存着 `traget` 也就是是 `原始bean` 的引用，因此后续 `原始bean` 的完善，也就相当于 `Spring AOP` 中的 `target` 的完善，这样就保证了 `AOP` 的`属性填充`与`初始化`了！

#### 2.2.3.非单例循环依赖

看完了单例模式的循环依赖，我们再看下非单例的情况，假设我们的配置文件是这样的：

```xml
<bean id="beanA" class="priv.starfish.BeanA" scope="prototype">
   <property name="beanB" ref="beanB"/>
</bean>

<bean id="beanB" class="priv.starfish.BeanB" scope="prototype">
   <property name="beanA" ref="beanA"/>
</bean>
```

启动 Spring，结果如下：

```java
Error creating bean with name 'beanA' defined in class path resource [applicationContext.xml]: Cannot resolve reference to bean 'beanB' while setting bean property 'beanB';

Error creating bean with name 'beanB' defined in class path resource [applicationContext.xml]: Cannot resolve reference to bean 'beanA' while setting bean property 'beanA';

Caused by: org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name 'beanA': Requested bean is currently in creation: Is there an unresolvable circular reference?
```

对于 `prototype` 作用域的 `bean`，`Spring` 容器无法完成依赖注入，因为 `Spring` 容器不进行缓存 `prototype` 作用域的 `bean` ，因此无法提前暴露一个创建中的 `bean`。

原因也挺好理解的，原型模式每次请求都会创建一个实例对象，即使加了缓存，循环引用太多的话，就比较麻烦了就，所以 `Spring` 不支持这种方式，直接抛出异常：

```java
if (isPrototypeCurrentlyInCreation(beanName)) {
   throw new BeanCurrentlyInCreationException(beanName);
}
```

#### 2.2.4.构造器循环依赖

上文我们讲的是通过 `Setter` 方法注入的单例 `bean` 的循环依赖问题，用 `Spring` 的小伙伴也都知道，依赖注入的方式还有**构造器注入**、工厂方法注入的方式（很少使用），那如果构造器注入方式也有循环依赖，可以搞不？

我们再改下代码和配置文件

```java
public class BeanA {
   private BeanB beanB;
   public BeanA(BeanB beanB) {
      this.beanB = beanB;
   }
}

public class BeanB {
	private BeanA beanA;
	public BeanB(BeanA beanA) {
		this.beanA = beanA;
	}
}
<bean id="beanA" class="priv.starfish.BeanA">
<constructor-arg ref="beanB"/>
</bean>

<bean id="beanB" class="priv.starfish.BeanB">
<constructor-arg ref="beanA"/>
</bean>
```

执行结果，又是异常

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331232813.png)

看看官方给出的说法

> Circular dependencies
>
> If you use predominantly constructor injection, it is possible to create an unresolvable circular dependency scenario.
>
> For example: Class A requires an instance of class B through constructor injection, and class B requires an instance of class A through constructor injection. If you configure beans for classes A and B to be injected into each other, the Spring IoC container detects this circular reference at runtime, and throws a `BeanCurrentlyInCreationException`.
>
> One possible solution is to edit the source code of some classes to be configured by setters rather than constructors. Alternatively, avoid constructor injection and use setter injection only. In other words, although it is not recommended, you can configure circular dependencies with setter injection.
>
> Unlike the typical case (with no circular dependencies), a circular dependency between bean A and bean B forces one of the beans to be injected into the other prior to being fully initialized itself (a classic chicken-and-egg scenario).

大概意思是：

如果您主要使用构造器注入，循环依赖场景是无法解决的。建议你用 setter 注入方式代替构造器注入

其实也不是说只要是构造器注入就会有循环依赖问题，`Spring` 在创建 `Bean` 的时候默认是**按照自然排序来进行创建的**，我们暂且把先创建的 `bean` 叫主 `bean`，上文的 `A` 即主 `bean`，**只要主 bean 注入依赖 bean 的方式是 setter 方式，依赖 bean 的注入方式无所谓，都可以解决，反之亦然**

所以上文我们 `AB` 循环依赖问题，只要 `A` 的注入方式是 `setter` ，就不会有循环依赖问题。

面试官问：为什么呢？

**Spring 解决循环依赖依靠的是 Bean 的“中间态”这个概念，而这个中间态指的是已经实例化，但还没初始化的状态。实例化的过程又是通过构造器创建的，如果 A 还没创建好出来，怎么可能提前曝光，所以构造器的循环依赖无法解决，我一直认为应该先有鸡才能有蛋**。

### 2.3.小总结 | 面试这么答

**B 中提前注入了一个没有经过初始化的 A 类型对象不会有问题吗？**

虽然在创建 B 时会提前给 B 注入了一个还未初始化的 A 对象，但是在创建 A 的流程中一直使用的是注入到 B 中的 A 对象的引用，之后会根据这个引用对 A 进行初始化，所以这是没有问题的。

**Spring 是如何解决的循环依赖？**

Spring 为了解决单例的循环依赖问题，使用了三级缓存。其中一级缓存为单例池（`singletonObjects`），二级缓存为提前曝光对象（`earlySingletonObjects`），三级缓存为提前曝光对象工厂（`singletonFactories`）。

假设A、B循环引用，实例化 A 的时候就将其放入三级缓存中，接着填充属性的时候，发现依赖了 B，同样的流程也是实例化后放入三级缓存，接着去填充属性时又发现自己依赖 A，这时候从缓存中查找到早期暴露的 A，没有 AOP 代理的话，直接将 A 的原始对象注入 B，完成 B 的初始化后，进行属性填充和初始化，这时候 B 完成后，就去完成剩下的 A 的步骤，如果有 AOP 代理，就进行 AOP 处理获取代理后的对象 A，注入 B，走剩下的流程。

**为什么要使用三级缓存呢？二级缓存能解决循环依赖吗？**

如果没有 AOP 代理，二级缓存可以解决问题，但是有 AOP 代理的情况下，只用二级缓存就意味着所有 Bean 在实例化后就要完成 AOP 代理，这样违背了 Spring 设计的原则，Spring 在设计之初就是通过 `AnnotationAwareAspectJAutoProxyCreator` 这个后置处理器来在 Bean 生命周期的最后一步来完成 AOP 代理，而不是在实例化后就立马进行 AOP 代理。

## 3.面试官：什么是AOP？Spring AOP和AspectJ的区别是什么？

`AOP`（`Aspect Orient Programming`），它是面向对象编程的一种补充，主要应用于处理一些具有横切性质的系统级服务，如日志收集、事务管理、安全检查、缓存、对象池管理等。

`AOP` 实现的关键就在于 `AOP` 框架自动创建的 `AOP` 代理，`AOP` 代理则可分为静态代理和动态代理两大类，其中静态代理是指使用 `AOP` 框架提供的命令进行编译，从而在编译阶段就可生成 `AOP` 代理类，因此也称为编译时增强；而动态代理则在运行时借助于JDK动态代理、`CGLIB` 等在内存中“临时”生成 `AOP` 动态代理类，因此也被称为运行时增强。

面向切面的编程（`AOP`） 是一种编程范式，旨在通过允许横切关注点的分离，提高模块化。`AOP` 提供切面来将跨越对象关注点模块化。

`AOP` 要实现的是在我们写的代码的基础上进行一定的包装，如在方法执行前、或执行后、或是在执行中出现异常后这些地方进行拦截处理或叫做增强处理

### 3.1.Aop的概念

**Pointcut**：是一个（组）基于正则表达式的表达式，有点绕，就是说他本身是一个表达式，但是他是基于正则语法的。通常一个pointcut，会选取程序中的某些我们感兴趣的执行点，或者说是程序执行点的集合。

**JoinPoint**：通过pointcut选取出来的集合中的具体的一个执行点，我们就叫 `JoinPoint`

**Advice**：在选取出来的JoinPoint上要执行的操作、逻辑。关于５种类型，我不多说，不懂的同学自己补基础。

**Aspect**：就是我们关注点的模块化。这个关注点可能会横切多个对象和模块，事务管理是横切关注点的很好的例子。它是一个抽象的概念，从软件的角度来说是指在应用程序不同模块中的某一个领域或方面。又 `pointcut` 和  `advice` 组成。

**Weaving**：把切面应用到目标对象来创建新的 `advised` 对象的过程。

### 3.2.AspectJ是什么？能做什么？

`AspectJ` 是一个易用的功能强大的 `AOP` 框架

`AspectJ` 全称是 `Eclipse AspectJ`， 其官网地址是：`http://www.eclipse.org/aspectj/`，目前最新版本为：`1.9.0`

引用官网描述：

- a seamless aspect-oriented extension to the Javatm programming language（一种基于Java平台的面向切面编程的语言）
- Java platform compatible（兼容Java平台，可以无缝扩展）
- easy to learn and use（易学易用）

可以单独使用，也可以整合到其它框架中。

单独使用 `AspectJ` 时需要使用专门的编译器 `ajc`。

`java` 的编译器是 `javac`，`AspectJ` 的编译器是 `ajc`，`aj` 是首字母缩写，`c` 即 `compiler`。

### 3.3.AspectJ和Spring AOP的区别？

相信作为 `Java` 开发者我们都很熟悉 `Spring` 这个框架，在 `spring` 框架中有一个主要的功能就是 `AOP`，提到 `AOP` 就往往会想到`AspectJ` ，下面我对 `AspectJ` 和 `Spring AOP` 作一个简单的比较：

#### 3.3.1.Spring AOP

- 基于动态代理来实现，默认如果使用接口的，用 `JDK` 提供的动态代理实现，如果是方法则使用CGLIB实现

- `Spring AOP` 需要依赖 `IOC` 容器来管理，并且只能作用于 `Spring` 容器，使用纯Java代码实现

- 在性能上，由于 `Spring AOP` 是基于动态代理来实现的，在容器启动时需要生成代理实例，在方法调用上也会增加栈的深度，使得`Spring AOP` 的性能不如 `AspectJ` 的那么好

#### 3.3.2.AspectJ

- `AspectJ` 来自于 `Eclipse` 基金会
- `AspectJ` 属于静态织入，通过修改代码来实现，有如下几个织入的时机：
  - 编译期织入（`Compile-time weaving`）： 如类 A 使用 `AspectJ` 添加了一个属性，类 `B` 引用了它，这个场景就需要编译期的时候就进行织入，否则没法编译类 B。
  - 编译后织入（`Post-compile weaving`）： 也就是已经生成了 `.class` 文件，或已经打成 `jar` 包了，这种情况我们需要增强处理的话，就要用到编译后织入。
  - 类加载后织入（`Load-time weaving`）： 指的是在加载类的时候进行织入，要实现这个时期的织入，有几种常见的方法。
    - 自定义类加载器来干这个，这个应该是最容易想到的办法，在被织入类加载到 `JVM` 前去对它进行加载，这样就可以在加载的时候定义行为了。
    - 在 JVM 启动的时候指定 AspectJ 提供的 agent：`-javaagent:xxx/xxx/aspectjweaver.jar`。

- `AspectJ` 可以做 `Spring AOP` 干不了的事情，它是 `AOP` 编程的完全解决方案，`Spring AOP` 则致力于解决企业级开发中最普遍的`AOP`（方法织入）。而不是成为像 `AspectJ` 一样的 `AOP` 方案
- 因为 `AspectJ` 在实际运行之前就完成了织入，所以说它生成的类是没有额外运行时开销的

#### 3.3.3.对比总结

下表总结了 `Spring AOP` 和 `AspectJ` 之间的关键区别:

| Spring AOP                                       | AspectJ                                                      |
| ------------------------------------------------ | ------------------------------------------------------------ |
| 在纯 Java 中实现                                 | 使用 Java 编程语言的扩展实现                                 |
| 不需要单独的编译过程                             | 除非设置 LTW，否则需要 AspectJ 编译器 (ajc)                  |
| 只能使用运行时织入                               | 运行时织入不可用。支持编译时、编译后和加载时织入             |
| 功能不强-仅支持方法级编织                        | 更强大 - 可以编织字段、方法、构造函数、静态初始值设定项、最终类/方法等......。 |
| 只能在由 Spring 容器管理的 bean 上实现           | 可以在所有域对象上实现                                       |
| 仅支持方法执行切入点                             | 支持所有切入点                                               |
| 代理是由目标对象创建的, 并且切面应用在这些代理上 | 在执行应用程序之前 (在运行时) 前, 各方面直接在代码中进行织入 |
| 比 AspectJ 慢多了                                | 更好的性能                                                   |
| 易于学习和应用                                   | 相对于 Spring AOP 来说更复杂                                 |

## 4.过滤器、拦截器和AOP的分析与对比

### 4.1.Filter过滤器

过滤器可以**拦截到方法的请求和响应**(ServletRequest request, ServletResponse response),并对**请求响应**做出过滤操作。

> 过滤器**依赖于servlet容器**。在实现上，基于函数回调，它可以对几乎所有请求进行过滤，一个过滤器实例只能在**容器初始化时调用一次。**

使用过滤器的目的是用来**做一些过滤操作**，获取我们想要获取的数据，比如：在过滤器中修改字符编码；在**过滤器中修改HttpServletRequest的一些参数**，包括：过滤低俗文字、危险字符等。话不多说，先上代码。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627122902.jpeg)

再定义两个Controller，一个UserController，一个OrderController

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627122916.jpeg)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627122921.png)

虽然Filter过滤器和Controller请求都已经定义了，但现在过滤器是不起作用的。需要把Filter配置一下，有两个方案**第一个方案在Filter上面加上@Component**。

```
@Component
public  class  TimeFilter  implements  Filter
```

**第二个方案配置化注册过滤器**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627122956.png)

第二个方案的特点就是可以**细化到过滤哪些规则的URL**我们来**启动应用时，过滤器被初始化了，init函数被回调**。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627123013.png)

**请求**http://localhost:9000/order/1

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627123027.png)

控制台日志输出

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627123046.png)

停止应用后，控制台输出

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627123051.png)

**Filter随web应用的启动而启动**，只初始化一次，随web应用的停止而销毁。

> 1.启动服务器时加载过滤器的实例，并**调用init()方法**来初始化实例；
>
> 2.每一次请求时都**只调用方法doFilter()进行处理**；
>
> 3.停止服务器时**调用destroy()方法**，销毁实例。

我们再来看看doFilter方法

> **doFilter**(ServletRequest request, ServletResponse response, FilterChain chain)

从参数我们看到，filter里面是能够获取到**请求的参数和响应的数据**；但此方法是无法知道是哪一个Controller类中的哪个方法被执行。还有一点需要注意的是，filter中是没法使用注入的bean的，也就是无法使用@Autowired

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627123105.png)

上面**代码注入的值为null。这是为什么呢**？

> 其实Spring中，web应用启动的顺序是：**listener->filter->servlet**，先初始化listener，然后再来就filter的初始化，**再接着才到我们的dispathServlet的初始化**，因此，当我们需要在filter里注入一个注解的bean时，就会注入失败，**因为filter初始化时，注解的bean还没初始化，没法注入。**

### 4.2.Interceptor拦截器

依赖于web框架，在SpringMVC中就是依赖于SpringMVC框架。在实现上,**基于Java的反射机制，属于面向切面编程（AOP）的一种运用**，就是在一个方法前，调用一个方法，或者在方法后，调用一个方法。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627130051.jpeg)

在WebMvcConfigurationSupport配置一下

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627130103.png)

执行结果

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627130111.png)

我们发现拦截器中可以获取到Controller对象

```
preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
```

object handler就是controller方法对象

```
HandlerMethod handlerMethod = (HandlerMethod)handler;

handlerMethod.getBean().getClass().getName(); //获取类名

handlerMethod.getMethod().getName(); //获取方法名
```

但我们发现获取不到方法的参数值，这个是为什么呢？在**DispatcherServlet类**中，方法 doDispatch(HttpServletRequest request, HttpServletResponse response)。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627130144.png)

**applyPreHandle**这个方法执行，就是执行的拦截器的preHandler方法，但这个过程中，controller方法没有从request中获取请求参数，组装方法参数；**而是在ha.handle这个方法的时候，才会组装参数**。

> 虽然没法得到方法的参数，但是可以获得IOC的bean哦。

再说明一点的是**postHandler方法**。

> postHandler方法的执行，当controller内部有异常，posthandler方法是不会执行的。

**afterCompletion方法**，不管controller内部是否有异常，都会执行此方法；此方法还会有个Exception ex这个参数；**如果有异常，ex会有异常值；没有异常 此值为null**。

> 注意点如果controller内部有异常，但异常被@ControllerAdvice 异常统一捕获的话，ex也会为null

### 4.3.Aspect切片

AOP操作可以对操作进行横向的拦截,最大的优势在于他可以**获取执行方法的参数**,对方法进行统一的处理。常见**使用日志,事务,请求参数安全验证**等。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627130222.jpeg)

上面的代码中，我们是可以获取方法的参数的

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627130234.png)

虽然切面aop可以拿到方法参数，但拿不到response，request对象。

### 4.4.对比

**执行顺序**

> filter -> interceptor -> ControllerAdvice -> aspect -> controller

**返回顺序**

> controller -> aspect -> controllerAdvice -> Interceptor -> Filter

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627131045.jpeg)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627131054.jpeg)

#### 4.4.1.过滤器（Filter）

- 应用场景
  - 自动登录
  - 统一设置编码格式
  - 访问权限控制
  - 敏感字符过滤等

#### 4.4.2.拦截器（Interceptor）

- 应用场景
  - 日志记录：记录请求信息的日志
  - 权限检查：如登录检查
  - 性能检测：检测方法的执行时间

#### 4.4.3.面向切面编程（AOP）

- 应用场景
  - 事务控制
  - 异常处理
  - 打印日志等

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627131243.png) 

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627131226.png)

## 5.ExceptionHandler的执行顺序

在项目开发中经常会遇到统一异常处理的问题，在springMVC中有一种解决方式，使用ExceptionHandler。举个例子，

```
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseBody
    public Result handleIllegalArgumentException(IllegalArgumentException e) {
        logger.error(e.getLocalizedMessage(), e);
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler({RuntimeException.class})
    @ResponseBody
    public Result handleRuntimeException(RuntimeException e) {
        logger.error(e.getLocalizedMessage(), e);
        return Result.failure();
    }
}

```

在这段代码中，我们可以看到存在两个异常处理的函数分别处理IllegalArgumentException和RuntimeException，但是转念一想，就会想到一个问题，IllegalArgumentException是RuntimeException的子类，那么对IllegalArgumentException这个异常又会由谁来处理呢？起初在网上看到一些答案，可以通过Order设置，但是经过简单的测试，发现Order并不起任何作用。虽然心中已有猜测，但还是希望能够找到真正可以证明想法的证据，于是便尝试找到这一块的源码。

### 5.1.源码解读

**调用栈**

排出掉缓存的情况，主动触发一个IllegalArgumentException异常，经过一步步调试，发现调用栈如下:

![image-20190326180205336](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627132107.webp)

**核心代码**

决定最终选择哪个ExceptionHandler的核心代码为ExceptionHandlerMethodResolver的getMappedMethod方法。代码如下:

```
private Method getMappedMethod(Class<? extends Throwable> exceptionType) {
    List<Class<? extends Throwable>> matches = new ArrayList<Class<? extends Throwable>>();
    for (Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
        if (mappedException.isAssignableFrom(exceptionType)) {
            matches.add(mappedException);
        }
    }
    if (!matches.isEmpty()) {
        Collections.sort(matches, new ExceptionDepthComparator(exceptionType));
        return this.mappedMethods.get(matches.get(0));
    }
    else {
    	return null;
    }
}

```

这个首先找到可以匹配异常的所有ExceptionHandler，然后对其进行排序，取深度最小的那个(即匹配度最高的那个)。

至于深度比较器的算法如下图，就是做了一个简单的递归，不停地判断父异常是否为目标异常来取得最终的深度。

![image-20190327224336509](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627132157.webp)

### 5.2.结论

源码不长，我们也可以很容易地就找到我们想要的答案——ExceptionHandler的处理顺序是由异常匹配度来决定的，且我们也无法通过其他途径指定顺序(其实也没有必要)。

## 6.@RestControllerAdvice与@ControllerAdvice的区别

简单地说，@RestControllerAdvice与@ControllerAdvice的区别就和@RestController与@Controller的区别类似，@RestControllerAdvice注解包含了@ControllerAdvice注解和@ResponseBody注解。

**当自定义类加@ControllerAdvice注解时，方法需要返回json数据时，每个方法还需要添加@ResponseBody注解：**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627133914.png)

**当自定义类加@RestControllerAdvice注解时，方法自动返回json数据，每个方法无需再添加@ResponseBody注解：**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627133924.png)

## 7.spring中定义bean的方法有哪些?

### 7.1. xml文件配置bean

我们先从`xml配置bean`开始，它是spring最早支持的方式。后来，随着`springboot`越来越受欢迎，该方法目前已经用得很少了，但我建议我们还是有必要了解一下。

#### 7.1.1 构造器

如果你之前有在bean.xml文件中配置过bean的经历，那么对如下的配置肯定不会陌生：

```java
<bean id="personService" class="com.sue.cache.service.test7.PersonService">
</bean>
```

这种方式是以前使用最多的方式，它默认使用了无参构造器创建bean。

当然我们还可以使用有参的构造器，通过`<constructor-arg>`标签来完成配置。

```java
<bean id="personService" class="com.sue.cache.service.test7.PersonService">
   <constructor-arg index="0" value="susan"></constructor-arg>
   <constructor-arg index="1" ref="baseInfo"></constructor-arg>
</bean>
```

其中：

- `index`表示下标，从0开始。
- `value`表示常量值
- `ref`表示引用另一个bean

#### 7.1.2 setter方法

除此之外，spring还提供了另外一种思路：通过setter方法设置bean所需参数，这种方式耦合性相对较低，比有参构造器使用更为广泛。

先定义Person实体：

```java
@Data
public class Person {
    private String name;
    private int age;
}
```

它里面包含：成员变量name和age，getter/setter方法。

然后在bean.xml文件中配置bean时，加上`<property>`标签设置bean所需参数。

```java
<bean id="person" class="com.sue.cache.service.test7.Person">
   <property name="name" value="susan"></constructor-arg>
   <property name="age" value="18"></constructor-arg>
</bean>
```

#### 7.1.3 静态工厂

这种方式的关键是需要定义一个工厂类，它里面包含一个创建bean的静态方法。例如：

```java
public class SusanBeanFactory {
    public static Person createPerson(String name, int age) {
        return new Person(name, age);
    }
}
```

接下来定义Person类如下：

```java
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Person {
    private String name;
    private int age;
}
```

它里面包含：成员变量name和age，getter/setter方法，无参构造器和全参构造器。

然后在bean.xml文件中配置bean时，通过`factory-method`参数指定静态工厂方法，同时通过`<constructor-arg>`设置相关参数。

```java
<bean class="com.sue.cache.service.test7.SusanBeanFactory" factory-method="createPerson">
   <constructor-arg index="0" value="susan"></constructor-arg>
   <constructor-arg index="1" value="18"></constructor-arg>
</bean>
```

#### 7.1.4 实例工厂方法

这种方式也需要定义一个工厂类，但里面包含非静态的创建bean的方法。

```java
public class SusanBeanFactory {
    public Person createPerson(String name, int age) {
        return new Person(name, age);
    }
}
```

Person类跟上面一样，就不多说了。

然后bean.xml文件中配置bean时，需要先配置工厂bean。然后在配置实例bean时，通过`factory-bean`参数指定该工厂bean的引用。

```java
<bean id="susanBeanFactory" class="com.sue.cache.service.test7.SusanBeanFactory">
</bean>
<bean factory-bean="susanBeanFactory" factory-method="createPerson">
   <constructor-arg index="0" value="susan"></constructor-arg>
   <constructor-arg index="1" value="18"></constructor-arg>
</bean>
```

#### 7.1.5 FactoryBean

不知道大家有没有发现，上面的实例工厂方法每次都需要创建一个工厂类，不方面统一管理。

这时我们可以使用`FactoryBean`接口。

```java
public class UserFactoryBean implements FactoryBean<User> {
    @Override
    public User getObject() throws Exception {
        return new User();
    }

    @Override
    public Class<?> getObjectType() {
        return User.class;
    }
}
```

在它的`getObject`方法中可以实现我们自己的逻辑创建对象，并且在`getObjectType`方法中我们可以定义对象的类型。

然后在bean.xml文件中配置bean时，只需像普通的bean一样配置即可。

```java
<bean id="userFactoryBean" class="com.sue.async.service.UserFactoryBean">
</bean>
```

轻松搞定，so easy。

> 注意：getBean("userFactoryBean");获取的是getObject方法中返回的对象。而getBean("&userFactoryBean");获取的才是真正的UserFactoryBean对象。

我们通过上面五种方式，在bean.xml文件中把bean配置好之后，spring就会自动扫描和解析相应的标签，并且帮我们创建和实例化bean，然后放入spring容器中。

虽说基于xml文件的方式配置bean，简单而且非常灵活，比较适合一些小项目。但如果遇到比较复杂的项目，则需要配置大量的bean，而且bean之间的关系错综复杂，这样久而久之会导致xml文件迅速膨胀，非常不利于bean的管理。

### 7.2. Component注解

为了解决bean太多时，xml文件过大，从而导致膨胀不好维护的问题。在spring2.5中开始支持：`@Component`、`@Repository`、`@Service`、`@Controller`等注解定义bean。

如果你有看过这些注解的源码的话，就会惊奇得发现：其实后三种注解也是`@Component`。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172134.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172516.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172524.png)

`@Component`系列注解的出现，给我们带来了极大的便利。我们不需要像以前那样在bean.xml文件中配置bean了，现在只用在类上加Component、Repository、Service、Controller，这四种注解中的任意一种，就能轻松完成bean的定义。

```java
@Service
public class PersonService {
    public String get() {
        return "data";
    }
}
```

其实，这四种注解在功能上没有特别的区别，不过在业界有个不成文的约定：

- Controller 一般用在控制层
- Service 一般用在业务层
- Repository 一般用在数据层
- Component 一般用在公共组件上

太棒了，简直一下子解放了我们的双手。

不过，需要特别注意的是，通过这种`@Component`扫描注解的方式定义bean的前提是：**需要先配置扫描路径**。

目前常用的配置扫描路径的方式如下：

1. 在applicationContext.xml文件中使用`<context:component-scan>`标签。例如：

```java
<context:component-scan base-package="com.sue.cache" />
```

2. 在springboot的启动类上加上`@ComponentScan`注解，例如：

```java
@ComponentScan(basePackages = "com.sue.cache")
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

3. 直接在`SpringBootApplication`注解上加，它支持ComponentScan功能：

```java
@SpringBootApplication(scanBasePackages = "com.sue.cache")
public class Application {
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

当然，如果你需要扫描的类跟springboot的入口类，在同一级或者子级的包下面，无需指定`scanBasePackages`参数，spring默认会从入口类的同一级或者子级的包去找。

```java
@SpringBootApplication
public class Application {
    
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

此外，除了上述四种`@Component`注解之外，springboot还增加了`@RestController`注解，它是一种特殊的`@Controller`注解，所以也是`@Component`注解。

`@RestController`还支持`@ResponseBody`注解的功能，即将接口响应数据的格式自动转换成json。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172545.png)

`@Component`系列注解已经让我们爱不释手了，它目前是我们日常工作中最多的定义bean的方式。

### 7.3. JavaConfig

`@Component`系列注解虽说使用起来非常方便，但是bean的创建过程完全交给spring容器来完成，我们没办法自己控制。

spring从3.0以后，开始支持JavaConfig的方式定义bean。它可以看做spring的配置文件，但并非真正的配置文件，我们需要通过编码java代码的方式创建bean。例如：

```java
@Configuration
public class MyConfiguration {

    @Bean
    public Person person() {
        return new Person();
    }
}
```

在JavaConfig类上加`@Configuration`注解，相当于配置了`<beans>`标签。而在方法上加`@Bean`注解，相当于配置了`<bean>`标签。

此外，springboot还引入了一些列的`@Conditional`注解，用来控制bean的创建。

```java
@Configuration
public class MyConfiguration {

    @ConditionalOnClass(Country.class)
    @Bean
    public Person person() {
        return new Person();
    }
}
```

`@ConditionalOnClass`注解的功能是当项目中存在Country类时，才实例化Person类。换句话说就是，如果项目中不存在Country类，就不实例化Person类。

这个功能非常有用，相当于一个开关控制着Person类，只有满足一定条件才能实例化。

spring中使用比较多的Conditional还有：

- ConditionalOnBean
- ConditionalOnProperty
- ConditionalOnMissingClass
- ConditionalOnMissingBean
- ConditionalOnWebApplication

如果你对这些功能比较感兴趣，可以看看《》，这是我之前写的一篇文章，里面做了更详细的介绍。

下面用一张图整体认识一下@Conditional家族:
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172612.png)

nice，有了这些功能，我们终于可以告别麻烦的xml时代了。

### 7.4. Import注解

通过前面介绍的@Configuration和@Bean相结合的方式，我们可以通过代码定义bean。但这种方式有一定的局限性，它只能创建该类中定义的bean实例，不能创建其他类的bean实例，如果我们想创建其他类的bean实例该怎么办呢？

这时可以使用`@Import`注解导入。

#### 7.4.1 普通类

spring4.2之后`@Import`注解可以实例化普通类的bean实例。例如：

先定义了Role类：

```java
@Data
public class Role {
    private Long id;
    private String name;
}
```

接下来使用@Import注解导入Role类：

```java
@Import(Role.class)
@Configuration
public class MyConfig {
}
```

然后在调用的地方通过`@Autowired`注解注入所需的bean。

```java
@RequestMapping("/")
@RestController
public class TestController {

    @Autowired
    private Role role;

    @GetMapping("/test")
    public String test() {
        System.out.println(role);
        return "test";
    }
}
```

聪明的你可能会发现，我没有在任何地方定义过Role的bean，但spring却能自动创建该类的bean实例，这是为什么呢？

这也许正是`@Import`注解的强大之处。

此时，有些朋友可能会问：`@Import`注解能定义单个类的bean，但如果有多个类需要定义bean该怎么办呢？

恭喜你，这是个好问题，因为`@Import`注解也支持。

```java
@Import({Role.class, User.class})
@Configuration
public class MyConfig {
}
```

甚至，如果你想偷懒，不想写这种`MyConfig`类，springboot也欢迎。

```cpp
@Import({Role.class, User.class})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class})
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(WebApplicationType.SERVLET).run(args);
    }
}
```

可以将@Import加到springboot的启动类上。

这样也能生效？

springboot的启动类一般都会加@SpringBootApplication注解，该注解上加了@SpringBootConfiguration注解。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172624.png)

而@SpringBootConfiguration注解，上面又加了@Configuration注解
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172634.png)

所以，springboot启动类本身带有@Configuration注解的功能。

意不意外？惊不惊喜？

#### 7.4.2 Configuration类

上面介绍了@Import注解导入普通类的方法，它同时也支持导入Configuration类。

先定义一个Configuration类：

```java
@Configuration
public class MyConfig2 {

    @Bean
    public User user() {
        return  new User();
    }

    @Bean
    public Role role() {
        return new Role();
    }
}
```

然后在另外一个Configuration类中引入前面的Configuration类：

```java
@Import({MyConfig2.class})
@Configuration
public class MyConfig {
}
```

这种方式，如果MyConfig2类已经在spring指定的扫描目录或者子目录下，则MyConfig类会显得有点多余。因为MyConfig2类本身就是一个配置类，它里面就能定义bean。

但如果MyConfig2类不在指定的spring扫描目录或者子目录下，则通过MyConfig类的导入功能，也能把MyConfig2类识别成配置类。这就有点厉害了喔。

**其实下面还有更高端的玩法**。

swagger作为一个优秀的文档生成框架，在spring项目中越来越受欢迎。接下来，我们以swagger2为例，介绍一下它是如何导入相关类的。

众所周知，我们引入swagger相关jar包之后，只需要在springboot的启动类上加上`@EnableSwagger2`注解，就能开启swagger的功能。

其中@EnableSwagger2注解中导入了Swagger2DocumentationConfiguration类。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172642.png)
该类是一个Configuration类，它又导入了另外两个类：

- SpringfoxWebMvcConfiguration
- SwaggerCommonConfiguration

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172651.png)

SpringfoxWebMvcConfiguration类又会导入新的Configuration类，并且通过@ComponentScan注解扫描了一些其他的路径。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172701.png)

SwaggerCommonConfiguration同样也通过@ComponentScan注解扫描了一些额外的路径。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172715.png)

如此一来，我们通过一个简单的`@EnableSwagger2`注解，就能轻松的导入swagger所需的一系列bean，并且拥有swagger的功能。

还有什么好说的，狂起点赞，简直完美。

#### 7.4.3 ImportSelector

上面提到的Configuration类，它的功能非常强大。但怎么说呢，它不太适合加复杂的判断条件，根据某些条件定义这些bean，根据另外的条件定义那些bean。

那么，这种需求该怎么实现呢？

这时就可以使用`ImportSelector`接口了。

首先定义一个类实现`ImportSelector`接口：

```java
public class DataImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        return new String[]{"com.sue.async.service.User", "com.sue.async.service.Role"};
    }
}
```

重写`selectImports`方法，在该方法中指定需要定义bean的类名，注意要包含完整路径，而非相对路径。

然后在MyConfig类上@Import导入这个类即可：

```java
@Import({DataImportSelector.class})
@Configuration
public class MyConfig {
}
```

朋友们是不是又发现了一个新大陆？

不过，这个注解还有更牛逼的用途。

@EnableAutoConfiguration注解中导入了AutoConfigurationImportSelector类，并且里面包含系统参数名称：`spring.boot.enableautoconfiguration`。![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172724.png)

AutoConfigurationImportSelector类实现了`ImportSelector`接口。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172903.png)

并且重写了`selectImports`方法，该方法会根据某些注解去找所有需要创建bean的类名，然后返回这些类名。其中在查找这些类名之前，先调用isEnabled方法，判断是否需要继续查找。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172913.png)

该方法会根据ENABLED_OVERRIDE_PROPERTY的值来作为判断条件。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172924.png)

而这个值就是`spring.boot.enableautoconfiguration`。

换句话说，这里能根据系统参数控制bean是否需要被实例化，优秀。

我个人认为实现ImportSelector接口的好处主要有以下两点：

1. 把某个功能的相关类，可以放到一起，方面管理和维护。
2. 重写selectImports方法时，能够根据条件判断某些类是否需要被实例化，或者某个条件实例化这些bean，其他的条件实例化那些bean等。我们能够非常灵活的定制化bean的实例化。

#### 7.4.4 ImportBeanDefinitionRegistrar

我们通过上面的这种方式，确实能够非常灵活的自定义bean。

但它的自定义能力，还是有限的，它没法自定义bean的名称和作用域等属性。

有需求，就有解决方案。

接下来，我们一起看看`ImportBeanDefinitionRegistrar`接口的神奇之处。

先定义CustomImportSelector类实现ImportBeanDefinitionRegistrar接口：

```java
public class CustomImportSelector implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        RootBeanDefinition roleBeanDefinition = new RootBeanDefinition(Role.class);
        registry.registerBeanDefinition("role", roleBeanDefinition);

        RootBeanDefinition userBeanDefinition = new RootBeanDefinition(User.class);
        userBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        registry.registerBeanDefinition("user", userBeanDefinition);
    }
}
```

重写`registerBeanDefinitions`方法，在该方法中我们可以获取`BeanDefinitionRegistry`对象，通过它去注册bean。不过在注册bean之前，我们先要创建BeanDefinition对象，它里面可以自定义bean的名称、作用域等很多参数。

然后在MyConfig类上导入上面的类：

```java
@Import({CustomImportSelector.class})
@Configuration
public class MyConfig {
}
```

我们所熟悉的fegin功能，就是使用ImportBeanDefinitionRegistrar接口实现的：
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172934.png)

具体细节就不多说了，有兴趣的朋友可以加我微信找我私聊。

### 7.5. PostProcessor

除此之外，spring还提供了专门注册bean的接口：`BeanDefinitionRegistryPostProcessor`。

该接口的方法postProcessBeanDefinitionRegistry上有这样一段描述：
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172949.png)

修改应用程序上下文的内部bean定义注册表标准初始化。所有常规bean定义都将被加载，但是还没有bean被实例化。这允许进一步添加在下一个后处理阶段开始之前定义bean。

如果用这个接口来定义bean，我们要做的事情就变得非常简单了。只需定义一个类实现`BeanDefinitionRegistryPostProcessor`接口。

```java
@Component
public class MyRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        RootBeanDefinition roleBeanDefinition = new RootBeanDefinition(Role.class);
        registry.registerBeanDefinition("role", roleBeanDefinition);

        RootBeanDefinition userBeanDefinition = new RootBeanDefinition(User.class);
        userBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        registry.registerBeanDefinition("user", userBeanDefinition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
```

重写`postProcessBeanDefinitionRegistry`方法，在该方法中能够获取`BeanDefinitionRegistry`对象，它负责bean的注册工作。

不过细心的朋友可能会发现，里面还多了一个`postProcessBeanFactory`方法，没有做任何实现。

这个方法其实是它的父接口：`BeanFactoryPostProcessor`里的方法。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824172959.png)

在应用程序上下文的标准bean工厂之后修改其内部bean工厂初始化。所有bean定义都已加载，但没有bean将被实例化。这允许重写或添加属性甚至可以初始化bean。

```java
@Component
public class MyPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory registry = (DefaultListableBeanFactory)beanFactory;
        RootBeanDefinition roleBeanDefinition = new RootBeanDefinition(Role.class);
        registry.registerBeanDefinition("role", roleBeanDefinition);

        RootBeanDefinition userBeanDefinition = new RootBeanDefinition(User.class);
        userBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        registry.registerBeanDefinition("user", userBeanDefinition);
    }
}
```

既然这两个接口都能注册bean，那么他们有什么区别？

- BeanDefinitionRegistryPostProcessor 更侧重于bean的注册
- BeanFactoryPostProcessor 更侧重于对已经注册的bean的属性进行修改，虽然也可以注册bean。

此时，有些朋友可能会问：既然拿到BeanDefinitionRegistry对象就能注册bean，那通过BeanFactoryAware的方式是不是也能注册bean呢？

从下面这张图能够看出DefaultListableBeanFactory就实现了BeanDefinitionRegistry接口。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824173008.png)

这样一来，我们如果能够获取DefaultListableBeanFactory对象的实例，然后调用它的注册方法，不就可以注册bean了？

说时迟那时快，定义一个类实现`BeanFactoryAware`接口：

```java
@Component
public class BeanFactoryRegistry implements BeanFactoryAware {
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory registry = (DefaultListableBeanFactory) beanFactory;
        RootBeanDefinition rootBeanDefinition = new RootBeanDefinition(User.class);
        registry.registerBeanDefinition("user", rootBeanDefinition);

        RootBeanDefinition userBeanDefinition = new RootBeanDefinition(User.class);
        userBeanDefinition.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);
        registry.registerBeanDefinition("user", userBeanDefinition);
    }
}
```

重写`setBeanFactory`方法，在该方法中能够获取BeanFactory对象，它能够强制转换成DefaultListableBeanFactory对象，然后通过该对象的实例注册bean。

当你满怀喜悦的运行项目时，发现竟然报错了：
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824173016.png)

为什么会报错？

spring中bean的创建过程顺序大致如下：
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220824173026.png)

`BeanFactoryAware`接口是在bean创建成功，并且完成依赖注入之后，在真正初始化之前才被调用的。在这个时候去注册bean意义不大，因为这个接口是给我们获取bean的，并不建议去注册bean，会引发很多问题。





