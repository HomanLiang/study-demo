[toc]



# Spring 核心容器  - 初始化

## 1.Spring容器初始化过程

### 1.0.IOC类图结构

![](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328230747.png)

**各个接口的作用**

| 接口名                  | 说明                                                         |
| ----------------------- | ------------------------------------------------------------ |
| BeanFactory             | 定义了基本IOC容器的规范，包含像 `getBean()` 这样的IOC容器的基本方法 |
| HierarchicalBeanFactory | 增加了getParentBeanFactory()的接口功能，使BeanFactory具备了双亲IOC接口的关联功能 |
| ConfigurableBeanFactory | 主要定义了对BeanFactory的配置功能，比如通过setParentBeanFactory()设置双亲IOC容器，通过addBeanPostProcessor()配置Bean后置处理器，等等。 |
|AutowireCapableFactory|	这个工厂接口继承自BeanFacotory，它扩展了自动装配的功能，根据类定义BeanDefinition装配Bean、执行前、后处理器等|
|ListableBeanFactory|	可以列出工厂可以生产的所有实例。当然，工厂并没有直接提供返回所有实例的方法|
|MessageSource|	提供国际化的消息访问|
|ResourceLoader|	资源加载的功能|
|ApplicationContext|	核心接口|
|ApplicationEventPublisher|	功能就是发布事件，也就是把某个事件告诉的所有与这个事件相关的监听器|
|ConfigurableApplicationContext|	提供设置活动和默认配置文件以及操作底层属性源的工具|
|WebApplicationContext|	提供了Web环境的支持|
|ThemeSource|	主题资源|

**BeanFactory和ApplicationContext的区别**

- BeanFactory是spring中比较原始的Factory。如XMLBeanFactory就是一种典型的BeanFactory。原始的BeanFactory无法支持spring的许多插件，如AOP功能、Web应用等。

- ApplicationContext接口是由BeanFactory接口派生而来，因而具有BeanFactory所有的功能。ApplicationContext以一种更向面向框架的方式工作以及对上下文进行分层和实现继承，ApplicationContext包还提供了以下的功能
  - MessageSource, 提供国际化的消息访问

  - 资源访问，如URL和文件
  - 事件传播
  - 载入多个（有继承关系）上下文 ，使得每一个上下文都专注于一个特定的层次，比如应用的web层

![](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328231220.png)

### 1.1.Spring 容器高层视图

Spring 启动时读取应用程序提供的Bean配置信息，并在Spring容器中生成一份相应的Bean配置注册表，然后根据这张注册表实例化Bean，装配号Bean之间的依赖关系，为上层应用提供准备就绪的运行环境。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328223909.jpeg)

### 1.2.内部工作机制

该图描述了Spring容器从加载配置文件到创建出一个完整Bean的作业流程：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328223927.jpeg)

1. ResourceLoader从存储介质中加载Spring配置信息，并使用Resource表示这个配置文件的资源；
2. BeanDefinitionReader读取Resource所指向的配置文件资源，然后解析配置文件。配置文件中每一个`<bean>`解析成一个BeanDefinition对象，并保存到BeanDefinitionRegistry中；
3. 容器扫描BeanDefinitionRegistry中的BeanDefinition，使用Java的反射机制自动识别出Bean工厂后处理后器（实现BeanFactoryPostProcessor接口）的Bean，然后调用这些Bean工厂后处理器对BeanDefinitionRegistry中的BeanDefinition进行加工处理。主要完成以下两项工作：

	- 对使用到占位符的 `<bean>` 元素标签进行解析，得到最终的配置值，这意味对一些半成品式的BeanDefinition对象进行加工处理并得到成品的BeanDefinition对象；

	- 对BeanDefinitionRegistry中的BeanDefinition进行扫描，通过Java反射机制找出所有属性编辑器的Bean（实现java.beans.PropertyEditor接口的Bean），并自动将它们注册到Spring容器的属性编辑器注册表中（PropertyEditorRegistry）；

4. Spring容器从BeanDefinitionRegistry中取出加工后的BeanDefinition，并调用InstantiationStrategy着手进行Bean实例化的工作；
5. 在实例化Bean时，Spring容器使用BeanWrapper对Bean进行封装，BeanWrapper提供了很多以Java反射机制操作Bean的方法，它将结合该Bean的BeanDefinition以及容器中属性编辑器，完成Bean属性的设置工作；
6. 利用容器中注册的Bean后处理器（实现BeanPostProcessor接口的Bean）对已经完成属性设置工作的Bean进行后续加工，直接装配出一个准备就绪的Bean。

Spring容器确实堪称一部设计精密的机器，其内部拥有众多的组件和装置。Spring的高明之处在于，它使用众多接口描绘出了所有装置的蓝图，构建好Spring的骨架，继而通过继承体系层层推演，不断丰富，最终让Spring成为有血有肉的完整的框架。所以查看Spring框架的源码时，有两条清晰可见的脉络：

- 接口层描述了容器的重要组件及组件间的协作关系；

- 继承体系逐步实现组件的各项功能。

接口层清晰地勾勒出Spring框架的高层功能，框架脉络呼之欲出。有了接口层抽象的描述后，不但Spring自己可以提供具体的实现，任何第三方组织也可以提供不同实现， 可以说Spring完善的接口层使框架的扩展性得到了很好的保证。纵向继承体系的逐步扩展，分步骤地实现框架的功能，这种实现方案保证了框架功能不会堆积在某些类的身上，造成过重的代码逻辑负载，框架的复杂度被完美地分解开了。

Spring组件按其所承担的角色可以划分为两类：

- 物料组件：Resource、BeanDefinition、PropertyEditor以及最终的Bean等，它们是加工流程中被加工、被消费的组件，就像流水线上被加工的物料；

- 加工设备组件：ResourceLoader、BeanDefinitionReader、BeanFactoryPostProcessor、InstantiationStrategy以及BeanWrapper等组件像是流水线上不同环节的加工设备，对物料组件进行加工处理。

### 1.3.Spring容器-ApplicationContext的启动过程

ApplicationContext内部封装了一个BeanFactory对象，来实现对容器的操作，初始化完成之后，BeanFactory封装了bean的信息，而ApplicationContext通过访问这个对象获取bean的对象信息（BeanDefinition/Bean对象，都是由BeanFactory实际创建并管理的），为了实现接口的统一，ApplicationContext也实现了一系列的BeanFactory接口(可以说ApplicationContext对BeanFactory对象实现一种代理)。ApplicationContext建立在BeanFactory的基础之上，对配置对象的管理最终还是交于一个DefaultListableBeanFactory来完成（装配地址/访问等），而ApplicationContext在应用这个DefaultListableBeanFactory对象的基础上，**不仅实现了BeanFactory接口提供的功能方法，并且黏合了一些面向应用的功能，如资源/国际化支持/框架事件支持等，并且将一些原先需要手动设置到BeanFactory的属性通过配置文件中配置的形式代替（如工厂后处理器BeanPostProcessor/InstantiationAwareBeanPostProcessor）**

同样，因为对于BeanDefinition和bean对象的管理是由上下文持有的beanfactory对象完成的，用户不需要拥有这样的接口，因此，ApplicationContext的接口体系中并没有BeanDefinitionRegistry，SingletonBeanRegistry以及AutowireCapableBeanFactory接口(ApplicationContext可以访问一些接口方法在上述接口中也定义，但这些方法提供者为BeanFactory体系中的其他接口，BeanFactory接口体系中的接口之间有重复定义方法的)。

**内部工作机制(Spring容器ApplicationContext的初始化）**                                        

首先来看创建ApplicationContext ，以ClassPathXmlApplicationContext为例：

ApplicationContext = new ClassPathXmlApplicationContext(xmlPath); 

源码如下：

```
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {

public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
                  this(new String[] {configLocation});
       }

public ClassPathXmlApplicationContext(String[] configLocations) throws BeansException {
                  this(configLocations, (ApplicationContext) null);
       }

//。。。。。。省略几个重载的构造函数
public ClassPathXmlApplicationContext(String[] configLocations, ApplicationContext parent)                                                                                                            throws BeansException {
          super(parent);
                 this.configLocations = configLocations;
          //IoC容器的初始化过程，其初始化过程的大致步骤由AbstractApplicationContext来定义   
                 refresh();
       }
```

关键之处在于refresh方法，此方法继承于ClassPathXmlApplicationContext的间接父类：

```
public abstract class AbstractApplicationContext extends DefaultResourceLoader
                            implements ConfigurableApplicationContext, DisposableBean {
```

Spring的AbstractApplicationContext是ApplicationContext抽象实现类，该抽象类的refresh()方法定义了Spring容器在加载配置文件后的各项处理过程，这些处理过程清晰刻画了Spring容器启动时所执行的各项操作（创建Spring容器如ClassPathXmlApplicationContext）。下面，我们来看一下refresh()内部定义了哪些执行逻辑：

```
public void refresh() throws BeansException, IllegalStateException {
              synchronized (this.startupShutdownMonitor) {
                    // Prepare this context for refreshing.
                    prepareRefresh();

             // Tell the subclass to refresh the internal bean factory       

            ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();--------（1）

             // Prepare the bean factory for use in this context.
                    prepareBeanFactory(beanFactory);

             try {
                          // Allows post-processing of the bean factory in context subclasses.
                          postProcessBeanFactory(beanFactory);

                   // Invoke factory processors registered as beans in the context.
                          invokeBeanFactoryPostProcessors(beanFactory);-------------------------------------（2）

                   // Register bean processors that intercept bean creation
                          registerBeanPostProcessors(beanFactory);---------------------------------------------（3）

                   // Initialize message source for this context.
                          initMessageSource();-------------------------------------------------------------------------（4）

                   // Initialize event multicaster for this context.
                          initApplicationEventMulticaster();-----------------------------------------------------------（5）

                   // Initialize other special beans in specific context subclasses.
                          onRefresh();------------------------------------------------------------------------------------（6）

                   // Check for listener beans and register them.
                          registerListeners();----------------------------------------------------------------------------（7）

                   // Instantiate singletons this late to allow them to access the message source.
                          beanFactory.preInstantiateSingletons();--------------------------------------------------（8）

                   // Last step: publish corresponding event.
                          publishEvent(new ContextRefreshedEvent(this));---------------------------------------（9）
                    } catch (BeansException ex) {
                          // Destroy already created singletons to avoid dangling resources.
                          beanFactory.destroySingletons();
                          throw ex;
                   }
             }
       }

protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
             refreshBeanFactory();
             ConfigurableListableBeanFactory beanFactory = getBeanFactory();

      return beanFactory;
       } 

protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;
```

1.3.1.**初始化BeanFactory**：根据配置文件实例化BeanFactory，getBeanFactory()方法由具体子类实现。在这一步里，Spring将配置文件的信息解析成为一个个的BeanDefinition对象并装入到容器的Bean定义注册表（BeanDefinitionRegistry）中，但此时Bean还未初始化；obtainFreshBeanFactory()会调用自身的refreshBeanFactory(),而refreshBeanFactory()方法由子类AbstractRefreshableApplicationContext实现，该方法返回了一个创建的DefaultListableBeanFactory对象，这个对象就是由ApplicationContext管理的BeanFactory容器对象。

这一步的操作相当于，如果我们在自己的应用代码中不用ApplicationContext而直接用BeanFactory时创建BeanFactory对象的操作

核心代码如下：

```
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext { }

 /** 该ApplicationContext管理的BeanFactory容器对象*/
       private DefaultListableBeanFactory beanFactory;

       protected final void refreshBeanFactory() throws BeansException {
              // Shut down previous bean factory, if any.
              ConfigurableListableBeanFactory oldBeanFactory = null;
              synchronized (this.beanFactoryMonitor) {
                      oldBeanFactory = this.beanFactory;
              }
              if (oldBeanFactory != null) {
                      oldBeanFactory.destroySingletons();
                      synchronized (this.beanFactoryMonitor) {
                             this.beanFactory = null;
                      }
              }

       // Initialize fresh bean factory.
              try {

               // 创建容器对象
                      DefaultListableBeanFactory beanFactory = createBeanFactory();

               // Customize the internal bean factory used by this context
                      customizeBeanFactory(beanFactory);

               // 装载配置文件，并传入相关联的BeanFactory对象，作为BeanDefinition的容器
                      loadBeanDefinitions(beanFactory);
                      synchronized (this.beanFactoryMonitor) {
                            this.beanFactory = beanFactory;
                      }
              }  catch (IOException ex) {
                      throw new ApplicationContextException(
                                  "I/O error parsing XML document for application context [" + getDisplayName() + "]", ex);
              }
       }

// 创建Spring默认的容器对象
protected DefaultListableBeanFactory createBeanFactory() {
       return new DefaultListableBeanFactory(getInternalParentBeanFactory());
}

// 该方法为一个钩子方法，子类可以覆盖它对当前上下文管理的BeanFactory提供客户化操作，也可以忽略
protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
       }

// 装载配置文件的方法，需要子类实现
protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
                                                                                                     throws IOException, BeansException;
```

对于上面装载配置文件的方法，由其子类扩展实现：

```
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableApplicationContext  {}

protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
             // 使用XMLBeanDefinitionReader来载入bean定义信息的XML文件，传入关联的BeanFactory

      XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

      // 这里配置reader的环境，其中ResourceLoader是我们用来定位bean定义信息资源位置的
             // 因为上下文本身实现了ResourceLoader接口，所以可以直接把上下文作为ResourceLoader传递入

      beanDefinitionReader.setResourceLoader(this);
             beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

      // Allow a subclass to provide custom initialization of the reader,
             // then proceed with actually loading the bean definitions.
             initBeanDefinitionReader(beanDefinitionReader);

      // 这里转到定义好的XmlBeanDefinitionReader中对载入bean信息进行处理 
             loadBeanDefinitions(beanDefinitionReader);
       } 

protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
             Resource[] configResources = getConfigResources();
             if (configResources != null) {
                     reader.loadBeanDefinitions(configResources);
             }
             String[] configLocations = getConfigLocations();
             if (configLocations != null) {
                     reader.loadBeanDefinitions(configLocations);
             }
       }

reader.loadBeanDefinitions(configLocations);涉及到XmlBeanDefinitionReader 工具类的使用（以后整理）
```

1.3.2.**调用工厂后处理器**：根据反射机制从BeanDefinitionRegistry中找出所有BeanFactoryPostProcessor类型的Bean，并调用其postProcessBeanFactory()接口方法；

经过第一步加载配置文件，已经把配置文件中定义的所有bean装载到BeanDefinitionRegistry这个Beanfactory中，对于ApplicationContext应用来说这个BeanDefinitionRegistry类型的BeanFactory就是Spring默认的DefaultListableBeanFactory

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
                              implements ConfigurableListableBeanFactory, BeanDefinitionRegistry

在这些被装载的bean中，若有类型为BeanFactoryPostProcessor的bean（配置文件中配置的），则将对应的BeanDefinition生成BeanFactoryPostProcessor对象

容器扫描BeanDefinitionRegistry中的BeanDefinition，使用java反射自动识别出Bean工厂后处理器（实现BeanFactoryPostProcessor接口）的bean，然后调用这些bean工厂后处理器对BeanDefinitionRegistry中的BeanDefinition进行加工处理，可以完成以下两项工作(当然也可以有其他的操作，用户自己定义)：

- 对使用到占位符的 `<bean>` 元素标签进行解析，得到最终的配置值，这意味着对一些半成品式的BeanDefinition对象进行加工处理并取得成品的BeanDefinition对象。
-  对BeanDefinitionRegistry中的BeanDefinition进行扫描，通过Java反射机制找出所有属性编辑器的Bean（实现java.beans.PropertyEditor接口的Bean），并自动将它们注册到Spring容器的属性编辑器注册表中（PropertyEditorRegistry），这个Spring提供了实现：CustomEditorConfigurer，它实现了BeanFactoryPostProcessor，用它来在此注册自定义属性编辑器；

AbstractApplicationContext中的代码如下：

```
protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
             // Invoke factory processors registered with the context instance.
             for (Iterator it = getBeanFactoryPostProcessors().iterator(); it.hasNext();) {
                   BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor) it.next();
                   factoryProcessor.postProcessBeanFactory(beanFactory);
             }

      // Do not initialize FactoryBeans here: We need to leave all regular beans
             // 通过ApplicatinContext管理的beanfactory获取已经注册的BeanFactoryPostProcessor类型的bean的名字

             String[] factoryProcessorNames =
                              beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

      // Separate between BeanFactoryPostProcessors that implement the Ordered
             // interface and those that do not.
             List orderedFactoryProcessors = new ArrayList();
             List nonOrderedFactoryProcessorNames = new ArrayList();
             for (int i = 0; i < factoryProcessorNames.length; i++) {
                   if (isTypeMatch(factoryProcessorNames[i], Ordered.class)) {

                   // 调用beanfactory的getBean取得所有的BeanFactoryPostProcessor对象
                         orderedFactoryProcessors.add(beanFactory.getBean(factoryProcessorNames[i]));
                   }
                   else {
                         nonOrderedFactoryProcessorNames.add(factoryProcessorNames[i]);
                   }
             }

      // First, invoke the BeanFactoryPostProcessors that implement Ordered.
             Collections.sort(orderedFactoryProcessors, new OrderComparator());
             for (Iterator it = orderedFactoryProcessors.iterator(); it.hasNext();) {
                  BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor) it.next();

           // 执行BeanFactoryPostProcessor的方法，传入当前持有的beanfactory对象，以获取要操作的 

           // BeanDefinition
                  factoryProcessor.postProcessBeanFactory(beanFactory);
             }
             // Second, invoke all other BeanFactoryPostProcessors, one by one.
             for (Iterator it = nonOrderedFactoryProcessorNames.iterator(); it.hasNext();) {
                  String factoryProcessorName = (String) it.next();
                  ((BeanFactoryPostProcessor) getBean(factoryProcessorName)).

                                                                      postProcessBeanFactory(beanFactory);
             }
       }
```

BeanFactoryPostProcessor接口代码如下，实际的操作由用户扩展并配置--扩展点

参考：[spring扩展点之一：BeanFactoryPostProcessor和BeanPostProcessor](http://www.cnblogs.com/duanxz/p/3750725.html)

1.3.3.**注册Bean后处理器**：根据反射机制从BeanDefinitionRegistry中找出所有BeanPostProcessor类型的Bean，并将它们注册到容器Bean后处理器的注册表中；

AbstractApplicatinContext中对应代码如下：

```
protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
              String[] processorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

         // Register BeanPostProcessorChecker that logs an info message when
                int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 +  processorNames.length;
                beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory,  beanProcessorTargetCount));
                List orderedProcessors = new ArrayList();
                List nonOrderedProcessorNames = new ArrayList();  

         for (int i = 0; i < processorNames.length; i++) {
                      if (isTypeMatch(processorNames[i], Ordered.class)) {
                           orderedProcessors.add(getBean(processorNames[i]));
                      }
                      else {
                           nonOrderedProcessorNames.add(processorNames[i]);
                      }  
                }

         // First, register the BeanPostProcessors that implement Ordered.
                Collections.sort(orderedProcessors, new OrderComparator());
                for (Iterator it = orderedProcessors.iterator(); it.hasNext();) {

               // 注册bean后处理器，该方法定义于ConfigurableBeanFactory接口
                      beanFactory.addBeanPostProcessor((BeanPostProcessor) it.next());
                }
                // Second, register all other BeanPostProcessors, one by one.
                for (Iterator it = nonOrderedProcessorNames.iterator(); it.hasNext();) {
                      String processorName = (String) it.next();
                      beanFactory.addBeanPostProcessor((BeanPostProcessor) getBean(processorName));
                }
       }
```

整段代码类似于第三步的调用工厂后处理器，区别之处在于，工厂后处理器在获取后立即调用，而Bean后处理器在获取后注册到上下文持有的beanfactory中，供以后操作调用（在用户获取bean的过程中，对已经完成属性设置工作的Bean进行后续加工，他加工的是bean，而工厂后处理器加工的是BeanDefinition）

BeanPostProcessor 接口代码如下，实际的操作由用户扩展并配置--**扩展点**

**参考：[spring扩展点之一：BeanFactoryPostProcessor和BeanPostProcessor](http://www.cnblogs.com/duanxz/p/3750725.html)**

1.3.4.**初始化消息源**：初始化容器的国际化信息资源；

源代码如下：

```
protected void initMessageSource() {
        // **补充**
}
```

1.3.5.**初始化应用上下文事件广播器**；（观察者模式中的具体主题角色，持有观察者角色的集合，称为注册表）

AbstractApplciationContext拥有一个applicationEventMulticaster 成员变量，applicationEventMulticaster 提供了容器监听器的注册表，成其为事件广播器。在第七步中将会将事件监听器装入其中

AbstractApplicationContext中的代码如下：

```
private ApplicationEventMulticaster applicationEventMulticaster;

protected void initApplicationEventMulticaster() {

        // "applicationEventMulticaster"，先看配置文件中有无配置该类型类（用户扩展 扩展点，如何扩展）
               if (containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
                    this.applicationEventMulticaster = (ApplicationEventMulticaster)
                    getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
               }
               else {

              // 若没有，则应用Spring框架提供的事件广播器实例
                     this.applicationEventMulticaster = new SimpleApplicationEventMulticaster();
               }
       }
       public boolean containsLocalBean(String name) {
               return getBeanFactory().containsLocalBean(name);
       }

public Object getBean(String name, Class requiredType) throws BeansException {
              return getBeanFactory().getBean(name, requiredType);
       }
```

Spring初始化事件广播器，用户可以在配置文件中为容器定义一个自定义的事件广播器（bean的名称要为"applicationEventMulticaster"），只要实现ApplicationEventMulticaster就可以了，Spring在此会根据beanfactory自动获取。如果没有找到外部配置的事件广播器，Spring使用SimpleApplicationEventMulticaster作为事件广播器。

 1.3.6.**初始化其他特殊的Bean**：这是一个钩子方法，子类可以借助这个钩子方法执行一些特殊的操作：如AbstractRefreshableWebApplicationContext就使用该钩子方法执行初始化ThemeSource的操作；

```
protected void onRefresh() throws BeansException {//--扩展点

package org.springframework.context;
      // For subclasses: do nothing by default.
}
```

1.3.7.**注册事件监听器**；（观察者模式中的观察者角色）

Spring根据上下文持有的beanfactory对象，从它的BeanDefinitionRegistry中找出所有实现org.springfamework.context.ApplicationListener的bean，将BeanDefinition对象生成bean，注册为容器的事件监听器，实际的操作就是将其添加到事件广播器所提供的监听器注册表中

AbstractApplicationContext中的代码如下：

```
/** Statically specified listeners */

private List applicationListeners = new ArrayList();

public List getApplicationListeners() {
              return this.applicationListeners;
       }

protected void registerListeners() {
              // Register statically specified listeners first.
              for (Iterator it = getApplicationListeners().iterator(); it.hasNext();) {
                     addListener((ApplicationListener) it.next());
              }
              // 获取ApplicationListener类型的所有bean，即事件监听器
              // uninitialized to let post-processors apply to them!
              Collection listenerBeans = getBeansOfType(ApplicationListener.class, true, false).values();
              for (Iterator it = listenerBeans.iterator(); it.hasNext();) {

              // 将事件监听器装入第五步初始化的事件广播器
                     addListener((ApplicationListener) it.next());
              }
       }

public Map getBeansOfType(Class type, boolean includePrototypes, boolean allowEagerInit) throws BeansException {
         return getBeanFactory().getBeansOfType(type, includePrototypes, allowEagerInit);
       }

protected void addListener(ApplicationListener listener) {     getApplicationEventMulticaster().addApplicationListener(listener);
}
```

ApplicationListener 的源代码如下：

```
package org.springframework.context;

import java.util.EventListener;

/**
        * Interface to be implemented by application event listeners.
        * @see org.springframework.context.event.ApplicationEventMulticaster
       */
       public interface ApplicationListener extends EventListener {
                void onApplicationEvent(ApplicationEvent event);

}
```

--**扩展点** 参考：[spring扩展点之三：Spring 的监听事件 ApplicationListener 和 ApplicationEvent 用法，在spring启动后做些事情](http://www.cnblogs.com/duanxz/p/3772654.html)

1.3.8.**初始化singleton的Bean**：实例化所有singleton的Bean，并将它们放入Spring容器的缓存中；这就是和直接在应用中使用BeanFactory的区别之处，在创建ApplicationContext对象时，不仅创建了一个BeanFactory对象，并且还应用它实例化所有单实例的bean。

AbstractApplicationContext中的代码如下：

beanFactory.preInstantiateSingletons();

**关于BeanFactory体系的代码参照。。。。。。**

1.3.9.**发布上下文刷新事件**：在此处时容器已经启动完成，发布容器refresh事件（ContextRefreshedEvent）

创建上下文刷新事件，事件广播器负责将些事件广播到每个注册的事件监听器中。

```
publishEvent(new ContextRefreshedEvent(this));

public void publishEvent(ApplicationEvent event) {
              Assert.notNull(event, "Event must not be null");

       // 在此获取事件广播器，并调用其方法发布事件：调用所有注册的监听器的方法
              getApplicationEventMulticaster().multicastEvent(event);
              if (this.parent != null) {
                    this.parent.publishEvent(event);
              }
       } 
```

至此，ApplicationContext对象就完成了初始化工作：创建BeanFactory来装配BeanDefiniton，加工处理BeanDefiniton，注册了bean后处理器，初始化了消息资源，初始化了应用上下文事件广播器，注册了事件监听器，初始化了所有singleton的bean，最后发布上下文刷新事件

### 1.4.BeanFactory和ApplicationContext之间的关系

- BeanFactory和ApplicationContext是Spring的两大核心接口，而其中ApplicationContext是BeanFactory的子接口。它们都可以当做Spring的容器，Spring容器是生成Bean实例的工厂，并管理容器中的Bean。在基于Spring的Java EE应用中，所有的组件都被当成Bean处理，包括数据源，Hibernate的SessionFactory、事务管理器等。

- 生活中我们一般会把生产产品的地方称为工厂，而在这里bean对象的地方官方取名为BeanFactory，直译Bean工厂（com.springframework.beans.factory.BeanFactory），我们一般称BeanFactory为IoC容器，而称ApplicationContext为应用上下文。

- Spring的核心是容器，而容器并不唯一，框架本身就提供了很多个容器的实现，大概分为两种类型：
   一种是不常用的BeanFactory，这是最简单的容器，只能提供基本的DI功能；

   一种就是继承了BeanFactory后派生而来的ApplicationContext(应用上下文)，它能提供更多企业级的服务，例如解析配置文本信息等等，这也是ApplicationContext实例对象最常见的应用场景。

#### 1.4.1.BeanFactory详情介绍

Spring容器最基本的接口就是BeanFactory。BeanFactory负责配置、创建、管理Bean，它有一个子接口ApplicationContext，也被称为Spring上下文，容器同时还管理着Bean和Bean之间的依赖关系。

Spring Ioc容器的实现，从根源上是BeanFactory，但真正可以作为一个可以独立使用的ioc容器还是DefaultListableBeanFactory，因此可以这么说，DefaultListableBeanFactory 是整个spring ioc的始祖。

![12234310-6bf928fc2231465a](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328231913.webp)

**接口介绍：**

1. BeanFactory接口是Spring bean容器的根接口，提供获取bean，是否包含bean,是否单例与原型，获取bean类型，bean 别名的方法 。它最主要的方法就是getBean(String beanName)。

2. BeanFactory的三个子接口：

   - HierarchicalBeanFactory：提供父容器的访问功能

   - ListableBeanFactory：提供了批量获取Bean的方法

   - AutowireCapableBeanFactory：在BeanFactory基础上实现对已存在实例的管理

3. ConfigurableBeanFactory：主要单例bean的注册，生成实例，以及统计单例bean

4. ConfigurableListableBeanFactory：继承了上述的所有接口，增加了其他功能：比如类加载器,类型转化,属性编辑器,BeanPostProcessor,作用域,bean定义,处理bean依赖关系, bean如何销毁…

5. 实现类DefaultListableBeanFactory：实现了ConfigurableListableBeanFactory，实现上述BeanFactory所有功能。它还可以注册BeanDefinition

#### 1.4.2.ApplicationContext介绍

如果说BeanFactory是Sping的心脏，那么ApplicationContext就是完整的身躯了。

![12234310-a14ad5a594b524fb](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328232423.webp)

ApplicationContext结构图

![12234310-be1edded652cea7b](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328232436.webp)

ApplicationContext类结构树

|     ApplicationContext常用实现类      |                             作用                             |
| :-----------------------------------: | :----------------------------------------------------------: |
|  AnnotationConfigApplicationContext   | 从一个或多个基于java的配置类中加载上下文定义，适用于java注解的方式。 |
|    ClassPathXmlApplicationContext     | 从类路径下的一个或多个xml配置文件中加载上下文定义，适用于xml配置的方式。 |
|    FileSystemXmlApplicationContext    | 从文件系统下的一个或多个xml配置文件中加载上下文定义，也就是说系统盘符中加载xml配置文件。 |
| AnnotationConfigWebApplicationContext |            专门为web应用准备的，适用于注解方式。             |
|       XmlWebApplicationContext        | 从web应用下的一个或多个xml配置文件加载上下文定义，适用于xml配置方式。 |

Spring具有非常大的灵活性，它提供了三种主要的装配机制：

- 在XMl中进行显示配置
- 在Java中进行显示配置
- 隐式的bean发现机制和自动装配
  - 组件扫描（component scanning）：Spring会自动发现应用上下文中所创建的bean。
  - 自动装配（autowiring）：Spring自动满足bean之间的依赖。

（使用的优先性: 3>2>1）尽可能地使用自动配置的机制，显示配置越少越好。当必须使用显示配置bean的时候（如：有些源码不是由你来维护的，而当你需要为这些代码配置bean的时候），推荐使用类型安全比XML更加强大的JavaConfig。最后只有当你想要使用便利的XML命名空间，并且在JavaConfig中没有同样的实现时，才使用XML。

代码示例：

1. 通过xml文件将配置加载到IOC容器中

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">
     <!--若没写id，则默认为com.test.Man#0,#0为一个计数形式-->
    <bean id="man" class="com.test.Man"></bean>
</beans>
```

```java
public class Test {
    public static void main(String[] args) {
        //加载项目中的spring配置文件到容器
        //ApplicationContext context = new ClassPathXmlApplicationContext("resouces/applicationContext.xml");
        //加载系统盘中的配置文件到容器
        ApplicationContext context = new FileSystemXmlApplicationContext("E:/Spring/applicationContext.xml");
        //从容器中获取对象实例
        Man man = context.getBean(Man.class);
        man.driveCar();
    }
}
```

2. 通过java注解的方式将配置加载到IOC容器

```java
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

```java
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

3. 隐式的bean发现机制和自动装配

```java
/**
 * 这是一个游戏光盘的实现
 */
//这个简单的注解表明该类回作为组件类，并告知Spring要为这个创建bean。
@Component
public class GameDisc implements Disc{
    @Override
    public void play() {
        System.out.println("我是马里奥游戏光盘。");
    }
}
```

不过，组件扫描默认是不启用的。我们还需要显示配置一下Spring，从而命令它去寻找@Component注解的类，并为其创建bean。

```java
@Configuration
@ComponentScan
public class DiscConfig {
}
```

我们在DiscConfig上加了一个@ComponentScan注解表示在Spring中开启了组件扫描，默认扫描与配置类相同的包，就可以扫描到这个GameDisc的Bean了。这就是Spring的自动装配机制。

------

**除了提供BeanFactory所支持的所有功能外ApplicationContext还有额外的功能**

- 默认初始化所有的Singleton，也可以通过配置取消预初始化。
- 继承MessageSource，因此支持国际化。
- 资源访问，比如访问URL和文件。
- 事件机制。
- 同时加载多个配置文件。
- 以声明式方式启动并创建Spring容器。

> 注：由于ApplicationContext会预先初始化所有的Singleton Bean，于是在系统创建前期会有较大的系统开销，但一旦ApplicationContext初始化完成，程序后面获取Singleton Bean实例时候将有较好的性能。也可以为bean设置lazy-init属性为true，即Spring容器将不会预先初始化该bean。

### 1.5 BeanFactory和FactoryBean的区别

**BeanFacotry是spring中比较原始的Factory。**如XMLBeanFactory就是一种典型的BeanFactory。原始的BeanFactory无法支持spring的许多插件，如AOP功能、Web应用等。

**ApplicationContext接口，它由BeanFactory接口派生而来。**ApplicationContext包含BeanFactory的所有功能，通常建议比BeanFactory优先。

BeanFactory是接口，提供了OC容器最基本的形式，给具体的IOC容器的实现提供了规范，

FactoryBean也是接口，为IOC容器中Bean的实现提供了更加灵活的方式，FactoryBean在IOC容器的基础上给Bean的实现加上了一个简单工厂模式和装饰模式(如果想了解装饰模式参考：修饰者模式(装饰者模式，Decoration) 我们可以在getObject()方法中灵活配置。其实在Spring源码中有很多FactoryBean的实现类.

**区别：**BeanFactory是个Factory，也就是IOC容器或对象工厂，FactoryBean是个Bean。在Spring中，**所有的Bean都是由BeanFactory(也就是IOC容器)来进行管理的。**

但**对FactoryBean而言，这个Bean不是简单的Bean，而是一个能生产或者修饰对象生成的工厂Bean,它的实现与设计模式中的工厂模式和修饰器模式类似**

#### 1.5.1.BeanFactory

1.4有详细介绍

#### 1.5.2.FactoryBean

一般情况下，Spring通过反射机制利用`<bean>`的class属性指定实现类实例化Bean，在某些情况下，实例化Bean过程比较复杂，如果按照传统的方式，则需要在`<bean>`中提供大量的配置信息。配置方式的灵活性是受限的，这时采用编码的方式可能会得到一个简单的方案。

Spring为此提供了一个org.springframework.bean.factory.FactoryBean的工厂类接口，用户可以通过实现该接口定制实例化Bean的逻辑。FactoryBean接口对于Spring框架来说占用重要的地位，Spring自身就提供了70多个FactoryBean的实现。它们隐藏了实例化一些复杂Bean的细节，给上层应用带来了便利。从Spring3.0开始，FactoryBean开始支持泛型，即接口声明改为`FactoryBean<T>`的形式

以Bean结尾，表示它是一个Bean，不同于普通Bean的是：它是实现了`FactoryBean<T>`接口的Bean，根据该Bean的ID从BeanFactory中获取的实际上是FactoryBean的getObject()返回的对象，而不是FactoryBean本身，如果要获取FactoryBean对象，请在id前面加一个&符号来获取。

例如自己实现一个FactoryBean，功能：用来代理一个对象，对该对象的所有方法做一个拦截，在调用前后都输出一行LOG，模仿ProxyFactoryBean的功能。

```
/**
 * my factory bean<p>
 * 代理一个类，拦截该类的所有方法，在方法的调用前后进行日志的输出
 * @author daniel.zhao
 *
 */
public class MyFactoryBean implements FactoryBean<Object>, InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(MyFactoryBean.class);    
    private String interfaceName;    
    private Object target;    
    private Object proxyObj;    
    @Override
    public void destroy() throws Exception {
        logger.debug("destroy......");
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        proxyObj = Proxy.newProxyInstance(
                this.getClass().getClassLoader(), 
                new Class[] { Class.forName(interfaceName) }, 
                new InvocationHandler() {                    
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                logger.debug("invoke method......" + method.getName());
                logger.debug("invoke method before......" + System.currentTimeMillis());
                Object result = method.invoke(target, args);
                logger.debug("invoke method after......" + System.currentTimeMillis());
                return result;            }            
        });
        logger.debug("afterPropertiesSet......");
    }

    @Override
    public Object getObject() throws Exception {
        logger.debug("getObject......");
        return proxyObj;
    }

    @Override
    public Class<?> getObjectType() {
        return proxyObj == null ? Object.class : proxyObj.getClass();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public Object getTarget() {
        return target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public Object getProxyObj() {
        return proxyObj;
    }

    public void setProxyObj(Object proxyObj) {
        this.proxyObj = proxyObj;
    }

}
```

XML-Bean配置如下

```
<bean id="fbHelloWorldService" class="com.ebao.xxx.MyFactoryBean">
   <property name="interfaceName" value="com.ebao.xxx.HelloWorldService" />
   <property name="target" ref="helloWorldService" />
</bean>
```

Junit Test class

```
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(classes = { MyFactoryBeanConfig.class })
public class MyFactoryBeanTest {
    @Autowired
    private ApplicationContext context;    
    /**
     * 测试验证FactoryBean原理，代理一个servcie在调用其方法的前后，打印日志亦可作其他处理
     * 从ApplicationContext中获取自定义的FactoryBean
     * context.getBean(String beanName) ---> 最终获取到的Object是FactoryBean.getObejct(), 
     * 使用Proxy.newInstance生成service的代理类
     */
    @Test
    public void testFactoryBean() {
        HelloWorldService helloWorldService = (HelloWorldService) context.getBean("fbHelloWorldService");
        helloWorldService.getBeanName();
        helloWorldService.sayHello();
    }
}
```

FactoryBean是一个接口，当在IOC容器中的Bean实现了FactoryBean后，通过getBean(String BeanName)获取到的Bean对象并不是FactoryBean的实现类对象，而是这个实现类中的getObject()方法返回的对象。要想获取FactoryBean的实现类，就要getBean(&BeanName)，在BeanName之前加上&。

Java代码

```
package org.springframework.beans.factory;  
public interface FactoryBean<T> {  
    T getObject() throws Exception;  
    Class<?> getObjectType();  
    boolean isSingleton();  
}
```

在该接口中还定义了以下3个方法：

- **TgetObject()：**返回由FactoryBean创建的Bean实例，如果isSingleton()返回true，则该实例会放到Spring容器中单实例缓存池中；
- **booleanisSingleton()：**返回由FactoryBean创建的Bean实例的作用域是singleton还是prototype；
- **Class<T>getObjectType()：**返回FactoryBean创建的Bean类型。

当配置文件中`<bean>`的class属性配置的实现类是FactoryBean时，通过getBean()方法返回的不是FactoryBean本身，而是FactoryBean#getObject()方法所返回的对象，相当于FactoryBean#getObject()代理了getBean()方法。

例：如果使用传统方式配置下面Car的`<bean>`时，Car的每个属性分别对应一个`<property>`元素标签。

```
package  com.baobaotao.factorybean;  
    public class Car  {  
        private   int maxSpeed ;  
        private  String brand ;  
        private   double price ;  
        public   int  getMaxSpeed ()   {  
            return   this . maxSpeed ;  
        }  
        public   void  setMaxSpeed ( int  maxSpeed )   {  
            this . maxSpeed  = maxSpeed;  
        }  
        public  String getBrand ()   {  
            return   this . brand ;  
        }  
        public   void  setBrand ( String brand )   {  
            this . brand  = brand;  
        }  
        public   double  getPrice ()   {  
            return   this . price ;  
        }  
        public   void  setPrice ( double  price )   {  
            this . price  = price;  
       }  
}
```

如果用FactoryBean的方式实现就灵活点，下例通过逗号分割符的方式一次性的为Car的所有属性指定配置值：

```
package  com.baobaotao.factorybean;  
import  org.springframework.beans.factory.FactoryBean;  
public   class  CarFactoryBean  implements  FactoryBean<Car>  {  
    private  String carInfo ;  
    public  Car getObject ()   throws  Exception  {  
        Car car =  new  Car () ;  
        String []  infos =  carInfo .split ( "," ) ;  
        car.setBrand ( infos [ 0 ]) ;  
        car.setMaxSpeed ( Integer. valueOf ( infos [ 1 ])) ;  
        car.setPrice ( Double. valueOf ( infos [ 2 ])) ;  
        return  car;  
    }  
    public  Class<Car> getObjectType ()   {  
        return  Car. class ;  
    }  
    public   boolean  isSingleton ()   {  
        return   false ;  
    }  
    public  String getCarInfo ()   {  
        return   this . carInfo ;  
    }  

    // 接受逗号分割符设置属性信息  
    public   void  setCarInfo ( String carInfo )   {  
        this . carInfo  = carInfo;  
    }  
}
```

有了这个CarFactoryBean后，就可以在配置文件中使用下面这种自定义的配置方式配置CarBean了：

```
<bean d="car"class="com.baobaotao.factorybean.CarFactoryBean"
P:carInfo="法拉利,400,2000000"/>
```

当调用getBean("car")时，Spring通过反射机制发现CarFactoryBean实现了FactoryBean的接口，这时Spring容器就调用接口方法CarFactoryBean#getObject()方法返回。如果希望获取CarFactoryBean的实例，则需要在使用getBean(beanName)方法时在beanName前显示的加上"&"前缀：如getBean("&car");

下面是一个应用FactoryBean的例子

```
<beans xmlns="http://www.springframework.org/schema/beans"  
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
             xmlns:context="http://www.springframework.org/schema/context"  
             xmlns:aop="http://www.springframework.org/schema/aop"  
             xmlns:tx="http://www.springframework.org/schema/tx"  
             xsi:schemaLocation="http://www.springframework.org/schema/beans  
                     http://www.springframework.org/schema/beans/spring-beans-3.0.xsd  
                     http://www.springframework.org/schema/context  
                     http://www.springframework.org/schema/context/spring-context-3.0.xsd  
                     http://www.springframework.org/schema/aop  
                     http://www.springframework.org/schema/aop/spring-aop-3.0.xsd  
                     http://www.springframework.org/schema/tx  
                     http://www.springframework.org/schema/tx/spring-tx-3.0.xsd">  

 <bean id="student" class="com.spring.bean.Student">    
  <property name="name" value="zhangsan" />    
 </bean>    

 <bean id="school" class="com.spring.bean.School">    
 </bean>   

 <bean id="factoryBeanPojo" class="com.spring.bean.FactoryBeanPojo">    
    <property name="type" value="student" />  
 </bean>   
</beans>
```

FactoryBean的实现类

```
import org.springframework.beans.factory.FactoryBean;  

/**  
 * @author  作者 wangbiao 
 * @parameter  
 * @return  
 */  
public class FactoryBeanPojo implements FactoryBean{  
    private String type;  

    @Override  
    public Object getObject() throws Exception {  
        if("student".equals(type)){  
            return new Student();             
        }else{  
            return new School();  
        }  

    }  

    @Override  
    public Class getObjectType() {  
        return School.class;  
    }  

    @Override  
    public boolean isSingleton() {  
        return true;  
    }  

    public String getType() {  
        return type;  
    }  

    public void setType(String type) {  
        this.type = type;  
    }  

}
```

普通的bean

```
/**  
 * @author  作者 wangbiao 
 * @parameter  
 * @return  
 */  
public class School {  
    private String schoolName;  
    private String address;  
    private int studentNumber;  
    public String getSchoolName() {  
        return schoolName;  
    }  
    public void setSchoolName(String schoolName) {  
        this.schoolName = schoolName;  
    }  
    public String getAddress() {  
        return address;  
    }  
    public void setAddress(String address) {  
        this.address = address;  
    }  
    public int getStudentNumber() {  
        return studentNumber;  
    }  
    public void setStudentNumber(int studentNumber) {  
        this.studentNumber = studentNumber;  
    }  
    @Override  
    public String toString() {  
        return "School [schoolName=" + schoolName + ", address=" + address  
                + ", studentNumber=" + studentNumber + "]";  
    }  
}
```

测试类

```
import org.springframework.context.support.ClassPathXmlApplicationContext;  

import com.spring.bean.FactoryBeanPojo;  

/**  
 * @author  作者 wangbiao 
 * @parameter  
 * @return  
 */  
public class FactoryBeanTest {  
    public static void main(String[] args){  
        String url = "com/spring/config/BeanConfig.xml";  
        ClassPathXmlApplicationContext cpxa = new ClassPathXmlApplicationContext(url);  
        Object school=  cpxa.getBean("factoryBeanPojo");  
        FactoryBeanPojo factoryBeanPojo= (FactoryBeanPojo) cpxa.getBean("&factoryBeanPojo");  
        System.out.println(school.getClass().getName());  
        System.out.println(factoryBeanPojo.getClass().getName());  
    }  
}
```

输出的结果：

```
十一月 16, 2016 10:28:24 上午 org.springframework.context.support.AbstractApplicationContext prepareRefresh  
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@1e8ee5c0: startup date [Wed Nov 16 10:28:24 CST 2016]; root of context hierarchy  
十一月 16, 2016 10:28:24 上午 org.springframework.beans.factory.xml.XmlBeanDefinitionReader loadBeanDefinitions  
INFO: Loading XML bean definitions from class path resource [com/spring/config/BeanConfig.xml]  
十一月 16, 2016 10:28:24 上午 org.springframework.beans.factory.support.DefaultListableBeanFactory preInstantiateSingletons  
INFO: Pre-instantiating singletons in org.springframework.beans.factory.support.DefaultListableBeanFactory@35b793ee: defining beans [student,school,factoryBeanPojo]; root of factory hierarchy  
com.spring.bean.Student  
com.spring.bean.FactoryBeanPojo
```

从结果上可以看到当从IOC容器中获取FactoryBeanPojo对象的时候，用getBean(String BeanName)获取的确是Student对象，可以看到在FactoryBeanPojo中的type属性设置为student的时候，会在getObject()方法中返回Student对象。

所以说从IOC容器获取实现了FactoryBean的实现类时，返回的却是实现类中的getObject方法返回的对象，要想获取FactoryBean的实现类，得在getBean(String BeanName)中的BeanName之前加上&,写成getBean(String &BeanName)。



## 初始化过程源码

### 初始化过程

我们以 ClassPathXmlApplicationContext 作为切入点研究 ApplicationContext 的初始化过程。

```java
public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
    this(new String[] {configLocation}, true, null);
}

public ClassPathXmlApplicationContext(String[] configLocations, boolean refresh, ApplicationContext parent)
            throws BeansException {
    // 1. 设置父容器
    super(parent);
    // 2. 设置配置文件路径，调用 getEnvironment().resolveRequiredPlaceholders(path) 方法解析路径中的占位符
    setConfigLocations(configLocations);
    // 3. 刷新容器，即重新初始化 bean 工厂
    if (refresh) {
        refresh();
    }
}
```

#### refresh

下面我们重点关注 refresh() 方法都作了那些事。

```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 1. 刷新上下文环境
        prepareRefresh();

        // 2. 初始化 beanFactory，对配置文件进行解读
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // 3. 对 beanFactory 进行功能扩展
        prepareBeanFactory(beanFactory);

        try {
            // 4. 专门留给子类作扩展用，这是一个空的方法
            postProcessBeanFactory(beanFactory);

            // 5. 注册并执行 BeanFactoryPostProcessor 后置处理器
            invokeBeanFactoryPostProcessors(beanFactory);

            // 6. 注册 BeanPostProcessor 后置处理器，在 getBean() 创建 bean 时调用
            registerBeanPostProcessors(beanFactory);

            // 7. 初始化 Message 源，即不同语言消息体，国际化处理
            initMessageSource();

            // 8. 注册多播器，事件监听器的管理者
            initApplicationEventMulticaster();

            // 9. 专门留给子类初始化其它 bean 用，这是一个空的方法
            onRefresh();

            // 10. 注册监听器
            registerListeners();

            // 11. 初始化剩余的 bean (部分在 invokeBeanFactoryPostProcessors 已经初始化)
            finishBeanFactoryInitialization(beanFactory);

            // 12. 完成刷新，通知生命周期处理器 LifecycleProcessor 刷新过程，同时发布 ContextRefreshedEvent 通知别人
            finishRefresh();
        } catch (BeansException ex) {
            destroyBeans();
            cancelRefresh(ex);
            throw ex;
        } finally {
            resetCommonCaches();
        }
    }
}
```

下面概括一下 ClassPathXmlApplicationContext 初始化的步骤，并从中解释一下它为我们提供的功能。

1. 初始化前的准备工作，例如对系统属性或者环境变量进行准备及验证。

   在某种情况下项目的使用需要读取某些系统变量，而这个变量的设置很可能会影响着系统的正确性，那么 ClassPathXmlApplicationContext 为我们提供的这个准备函数就显得非常必要，它可以在 Spring 启动的时候提前对必须的变量进行存在性验证

2. 初始化 BeanFactory，并进行 XML 文件读取。

   之前有提到 ClassPathXmlApplicationContext 包含着 BeanFactory 所提供的一切特征，那么在这一步骤中将会复用 BeanFactory 中的配置文件读取解析及其他功能，这一步之后，ClassPathXmlApplicationContext 实际上就已经包含了 BeanFactory 所提提供的功能，也就是可以进行 Bean 的提取等基础操作了。

3. 对 BeanFactory 进行各种功能填充。

   @Qualifier 与 @Autowired 应该是大家非常熟悉的注解，那么这两个注解正是在这一步骤中增加的支持。

4. 子类覆盖方法做额外的处理。

   Spring 之所以强大，为世人所推崇，除了它功能上为大家提供了便例外，还有一方面是它的完美架构，开放式的架构让使用它的程序员很容易根据业务需要扩展已经存在的功能。这种开放式的设计在 Spring 中随处可见，例如在本例中就提供了一个空的函数实现 postProcessBeanFactory 来方便程序员在业务上做进一步扩展。

5. 激活各种 BeanFactory 处理器。

6. 注册拦截 bean 创建的 bean 处理器，这里只是注册，真正的调用是在 getbean 时候。

7. 为上下文初始化 Message 源，即对不同语言的消息体进行国际化处理。

8. 初始化应用消息广播器，并放人 "applicationeventMulticaster" bean中。

9. 留给子类来初始化其他的 bean。

10. 在所有注册的 bean 中查找 listener bean，注册到消息广播器中。

11. 初始化剩下的单实例(非惰性的)。

12. 完成刷新过程，通知生命周期处理器 lifecycleProcessor 刷新过程，同时发出 ContextRefreshEvent 通知别人。

### 环境准备

本节介绍容器初始化的第一步：环境准备工作。

prepareRefresh 函数主要是做些准备工作，例如对系统属性及环境变量的初始化及验证。

```java
protected void prepareRefresh() {
    this.startupDate = System.currentTimeMillis();
    this.closed.set(false);
    this.active.set(true);

    // 1. 留给子类覆盖，如添加要验证的属性
    initPropertySources();

    // 2. 验证需要的属性文件是否都已经放入环境中
    getEnvironment().validateRequiredProperties();

    // 3. 如果多播器还未初始化完成，就将早期发布的事件统一放到集合中，等多播器初始化完成后再发布事件
    this.earlyApplicationEvents = new LinkedHashSet<ApplicationEvent>();
}
```

网上有人说其实这个函数没什么用，因为最后两句代码才是最为关键的，但是却没有什么逻辑处理，initPropertySources 是空的，没有任何逻辑，而 getEnvironment().validateRequiredProperties() 也因为没有需要验证的属性而没有做任何处理。其实这这都是因为没有彻底理解才会这么说，这个函数如果用好了作用还是挺大的。那么，该怎么用呢？我们先探索下各个函数的作用。

1. initPropertySources 正符合 Spring 的开放式结构设计，给用户最大扩展 Spring 的能力。用户可以根据自身的需要重写 initPropertySources 方法，并在方法中进行个性化的属性处理及设置。

2. validateRequiredProperties 则是对属性进行验证，那么如何验证呢？我们举个融合两句代码的小例子来帮助大家理解。

假如现在有这样一个需求，工程在运行过程中用到的某个设置(例如 VAR)是从系统环境变量中取得的，而如果用户没有在系统环境变量中配置这个参数，那么工程可能不会工作。这一要求可能会有各种各样的解决办法，当然，在 Spring 中可以这样做，你可以直接修改 Spring 的源码，例如修改 ClassPathXmlApplicationContext。当然，最好的办法还是对源码进行扩展，我们可以自定义类：

```java
public class MyClassPathXmlApplicationContext extends ClassPathXmlApplicationContext {

    public MyClassPathXmlApplicationContext(String configLocations) throws BeansException {
        super(configLocations);
    }

    public MyClassPathXmlApplicationContext(String path, Class<?> clazz) throws BeansException {
        super(path, clazz);
    }

    @Override
    protected void initPropertySources() {
        // 添加验证要求
        getEnvironment().setRequiredProperties("VAR");
    }
}
```

我们自定义了继承自 ClassPathXmlApplicationContext 的 MyClassPathXmlApplicationContext，并重写了 initPropertySources 方法，在方法中添加了我们的个性化需求，那么在验证的时候也就是程序走到 getEnvironment().validateRequiredProperties() 代码的时候，如果系统并没有檢测到对应 VAR 的环境变量，那么将抛出异常。当然我们还需要在使用的时候替换掉原有的

```java
public static void main(String[] args) {
    ApplicationContext context = new MyClassPathXmlApplicationContext(
            "spring-context-test.xml", Main.class);
    MyTestBean myTestBean = (MyTestBean) context.getBean("myTestBean");
}
```

### BeanFactory 初始化

上节我们提到容器初始化的第一步首先进行了属性的检验，下面就要开始第二步：进行 beanFactory 的初始化工作了。

ApplicationContext 是对 BeanFactory 的功能上的扩展，不但包含了 BeanFactory 的全部功能更在其基础上添加了大量的扩展应用，那么 obtainFreshBeanFactory 正是实现 BeanFactory ，并对配置文件进行解析。

源代码【AbstractApplicationContext】

```java
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
    // 初始化 BeanFactory，并进行 XML 文件读取，并将得到的 BeanFactory 记录在当前实体的属性中
    refreshBeanFactory();
    // 返回当前实体的 BeanFactory 属性
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    return beanFactory;
}
```

很明显这段代码没有做什么事，将其主要的工作都委托给了 refreshBeanFactory() 方法，这个方法由其子类实现。

源代码【AbstractRefreshableApplicationContext】

```java
protected final void refreshBeanFactory() throws BeansException {
    if (hasBeanFactory()) {
        destroyBeans();
        closeBeanFactory();
    }
    try {
        // 1. 创建 DefaultListableBeanFactory
        DefaultListableBeanFactory beanFactory = createBeanFactory();

        // 为了序列化指定id，如果需要的话，让这个 beanFactory 从 id 反序列化到 BeanFactory 对象
        beanFactory.setSerializationId(getId());
        // 2. 定制 beanFactory，如设置：①是否允许同名覆盖、②是否允许循环依赖
        customizeBeanFactory(beanFactory);
        // 3. 初始化 DocumentReader，并进行 XML 文件读取及解析
        loadBeanDefinitions(beanFactory);
        synchronized (this.beanFactoryMonitor) {
            this.beanFactory = beanFactory;
        }
    }
    catch (IOException ex) {
        throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
    }
}
```

在 refreshBeanFactory() 方法中主要做了三件事：

1. 创建 BeanFactory。创建前需要先销毁以前的 beanFactory。

2. 定制 BeanFactory。设置了是否允许同名覆盖、是否允许循环依赖两个属性。你可能会奇怪，这两个属性本来就是 null，没有值，这是在干什么？还是那名话，需要子类来覆盖。

3. 加载 BeanFactory 配置文件。解析 xml 文件，加载 BeanDefinition。

#### 创建 BeanFactory

```java
protected DefaultListableBeanFactory createBeanFactory() {
    return new DefaultListableBeanFactory(getInternalParentBeanFactory());
}
```

#### 定制 BeanFactory

```java
protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    if (this.allowBeanDefinitionOverriding != null) {
        beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
    }
    if (this.allowCircularReferences != null) {
        beanFactory.setAllowCircularReferences(this.allowCircularReferences);
    }
}
```

#### 加载 BeanDefinition

源代码【AbstractXmlApplicationContext】

```java
@Override
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
    // 1. 创建 XmlBeanDefinitionReader
    XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

    // 2. 对 beanDefinitionReader 进行环境变量的设置
    beanDefinitionReader.setEnvironment(this.getEnvironment());
    beanDefinitionReader.setResourceLoader(this);
    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

    // 3. 对 beanDefinitionReader 进行设置，默认可以覆盖
    initBeanDefinitionReader(beanDefinitionReader);
    loadBeanDefinitions(beanDefinitionReader);
}

protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
    reader.setValidating(this.validating);
}
```

在初始化了 DefaultListableBeanfactory 和 XmlBeanDefinitionReader，后就可以进行配置文件的读取了。

```java
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) 
        throws BeansException, IOException {
    Resource[] configResources = getConfigResources();
    if (configResources != null) {
        reader.loadBeanDefinitions(configResources);
    }
    String[] configLocations = getConfigLocations();
    if (configLocations != null) {
        reader.loadBeanDefinitions(configLocations);
    }
}
```

使用 XmlBeanDefinitionReader 的 loadBeanDefinitions 方法进行配置文件的加载机注册相信大家已经不陌生，这完全就是开始 BeanFactory 的套路。因为在 XmlBeanDefinitionReader 中已经将之前初始化的 DefaultlistableBeanfactory 注册进去了，所以 XmlBeanDefinitionReader 所读取的 BeanDefinitionHolder 都会注册到 DefaultListableBeanfactory 中。

此时的 BeanFactory 已经解析了所有的 BeanDefinition，可以进行 bean 的加载了，不过在加载前 Spring 还做了一些其它的工作。

### BeanFactory 功能扩展

上节我们提到容器刷新的第二步初始化 BeanFactory 工厂并解析配制文件，但此时 BeanFactory 的功能还很简单，需要对其进行扩展。这就涉及到下面第三步：BeanFactory 功能扩展。

那 Spring 究竟进行了那些功能扩展呢？

源代码【AbstractApplicationContext】

```java
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // 1. 设置 beanFactory 的 classLoader 为当前 context 的 classLoader
    beanFactory.setBeanClassLoader(getClassLoader());

    // 2. 设置 beanFactory 的表达式语言处理器，Spring3 增加了表达式语言的支持
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));

    // 3. 为 beanFactory 增加一个默认的 propertyEditor，这个主要是对 bean 的属性等设置管理的一个工具
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    // 4. 添加 BeanPostProcessor
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

    // 5. 设置了几个忽略自动装配的接口
    beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
    beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);

    // 6. 设置了几个自动装配的特殊规则
    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ResourceLoader.class, this);
    beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);

    // 7. 增加对 AspectJ 的支持
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        // Set a temporary ClassLoader for type matching.
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // 8. 添加默认的系统环境 bean
    if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
    }
}
```

上面函数中主要进行了几个方面的扩展。

(1) 增加对 SPEL 语言的支持

(2) 增加对属性编辑器的支持

(3) 增加对一些内置类，比如 Environmentaware、 Messagesourceaware 的信息注入。

(4) 设置了依赖功能可忽略的接口

(5) 注册一些固定依的属性

(6) 增加 Aspect的支持(会在第7章中进行详细的讲解)

(7) 将相关环境变量及属性注册以单例模式注册

可能读者不是很理解每个步骤的具体含义，接下来我们会对各个步骤进行详细地分析。

#### 增加对 SPEL 语言的支持

SpEL使用 #{...} 作为定界符，所有在大框号中的字符都将被认为是 SpEL，使用格式如下：

```java
@Value("#{19 - 1}")
private int age3;
```

在源码中通过代码 beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader())) 注册语言解析器，就可以对 SPEL 进行解析了，那么在注册解析器后 Spring 又是在什么时候调用这个解析器进行解析呢？详见 [《SPEL语言执行过程》](https://www.cnblogs.com/binarylei/p/10423549.html)

#### 增加对属性编辑器的支持

##### 属性编辑器的基本用法

在 Spring DI 注入的时候可以把普通属性注入进来，但是像 Date 类型就无法被识別。例如：

```java
public class UserManager {
    private Date data;
    // 省略get/set
}
```

上面代码中，需要对日期型属性进行注入：

```xml
<bean id="userManager" class="UserManager">
    <property name="data" value="2018-08-01 00:00:00"/>
</bean>
```

如果直接这样使用，程序则会报异常，类型转换不成功。因为在 Usermanager 中的 data Value 属性是 Date 类型型的，而在 XML中配置的却是 String 类型的，所以当然会报异常。

Spring 针对此问题提供了两种解决办法。

###### 使用用自定义属性编辑器

使用自定义属性编辑器，通过继承 PropertyEditorSupport，重写 setastext 方法，具体步骤如下。

(1) 编写自定义的属性编辑器。

```java
public class DatePropertyEdit extends PropertyEditorSupport implements PropertyEditor {
    private String format = "yyyy-MM-dd HH:mm:ss";
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(text);
            this.setValue(date);
        } catch (ParseException e) {
        }
    }
}
```

(2) 将自定义属性编辑器注册到 Spring 中。

```xml
<bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
    <property name="customEditors">
        <map>
            <entry key="java.util.Date"
                   value="com.github.binarylei.spring01.day0728.propertyedit.DataPropertyEdit">
            </entry>
        </map>
    </property>
</bean>
```

在配置文件中引入类型为 CustomEditorConfigurer 的 bean，并在属性 customeditors 中加入自定义的属性编辑器，其中 key 为属性编辑器所对应的类型。通过这样的配置，当 Spring 在注入 bean 的属性时一旦遇到了 java.uti.Date 类型的属性会自动调用自定义的 DatepropertyEditor 解解析器进行解析，并用解析结果代替配置属性进行注入。

###### 注册 Spring 自带的属性编辑器 CustomDateEditor

通过注册 Spring 自带的属性编辑器 CustomDateEditor，具体步骤如下。

(1) 定义属性编辑器

```java
public class DatePropertyEditorRegistar implements PropertyEditorRegistrar {
    @Override
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registry.registerCustomEditor(Date.class, 
                new CustomDateEditor(new SimpleDateFormat("yyyy-MM-dd"),true));
    }
}
```

(2) 注册到 Spring

```xml
<bean class="org.springframework.beans.factory.config.CustomEditorConfigurer">
    <property name="propertyEditorRegistrars">
        <list>
            <bean class="spring.context.spring_di.DatePropertyEditorRegistar"/>
        </list>
    </property>
</bean>
```

通过在配置文件中将自定义的 DatePropertyEditorRegistar 注册进人 org.springframework.beans.factory.config.CustomEditorConfigurer 的 propertyEditorRegistrars 属性中，可以具有与方法 1 同样的效果我们了解了自定义属性编辑器的使用。

但是，似乎这与本节中围绕的核心代码 beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment())) 并无联系，因为在注册自定义属性编辑器的时候使用的是 PropertyEditorRegistrar 的 registerCustomEditors 方法，而这里使用的是 ConfigurableListableBeanFactory 的 addPropertyEditorRegistrar 方法。我们不妨深入探索下 ResourceEditorRegistrar 的内部实现，在 ResourceEditorRegistrar 中，我们最关心的方法是 registerCustomEditor 方法。

##### 属性编辑器的内部原理

```
beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()))
```

源代码【AbstractBeanFactory】

```java
public void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar) {
    Assert.notNull(registrar, "PropertyEditorRegistrar must not be null");
    this.propertyEditorRegistrars.add(registrar);
}
```

源代码【ResourceEditorRegistrar】

```java
// 1. PropertyEditorRegistrar 将常用的资源编辑器注册到 PropertyEditorRegistry 上，相当于一个工具类
// 2. PropertyEditorRegistry 实际持有编辑器
public void registerCustomEditors(PropertyEditorRegistry registry) {
    ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
    doRegisterEditor(registry, Resource.class, baseEditor);
    doRegisterEditor(registry, ContextResource.class, baseEditor);
    doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
    doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
    doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
    doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
    doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

    ClassLoader classLoader = this.resourceLoader.getClassLoader();
    doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
    doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
    doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

    if (this.resourceLoader instanceof ResourcePatternResolver) {
        doRegisterEditor(registry, Resource[].class,
                new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
    }
}

/**
 * 注册对应的属性编辑器
 */
private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
    if (registry instanceof PropertyEditorRegistrySupport) {
        ((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
    }
    else {
        registry.registerCustomEditor(requiredType, editor);
    }
}
```

在 doRegisterEditor 函数中，可以看到在之前提到的自定义属性中使用的关键代码 registry.registerCustomEditor(requiredType, editor)，回过头来看 ResourceEditorRegistrar 类的 registerCustomEditors 方法的核心功能，其实无非是注册了一系列的常用类型的属性编辑器，例如，代码 doRegisterEditor(registry, Class.class, new ClassEditor(classLoader)) 实现的功能就是注册 Class 类对应的属性编辑器。那么，注册后，一且某个实体 bean 中存在一些 Class 类型的属性，那么 Spring 会调用 Classeditor 将配置中定义的 String 类型转换为 Class 类型并进行赋值。

分析到这里，我们不禁有个疑问，虽说 ResourceEditorRegistrar 类的 registerCustomEditors 方法实现了批量注册的功能，但是 beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment())) 仅仅是注册了 ResourceEditorRegistrar 实例，却并没有调用 ResourceEditorRegistrar 的 registerCustomEditors 方法进行注册，那么到底是在什么时候进行注册的呢？

源代码【Abstractbeanfactory】

```java
// 如此，BeanWrapper 就注册了各种属性编辑器，可以在 populate() 时解析各种 String 类型的字段进行注入
protected void initBeanWrapper(BeanWrapper bw) {
    bw.setConversionService(getConversionService());
    registerCustomEditors(bw);
}

// 调用 propertyEditorRegistrars 为 registry 注入属性编辑器
protected void registerCustomEditors(PropertyEditorRegistry registry) {
    PropertyEditorRegistrySupport registrySupport =
            (registry instanceof PropertyEditorRegistrySupport ? (PropertyEditorRegistrySupport) registry : null);
    if (registrySupport != null) {
        registrySupport.useConfigValueEditors();
    }
    if (!this.propertyEditorRegistrars.isEmpty()) {
        for (PropertyEditorRegistrar registrar : this.propertyEditorRegistrars) {
            registrar.registerCustomEditors(registry);
        }
    }
    if (!this.customEditors.isEmpty()) {
        for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : this.customEditors.entrySet()) {
            Class<?> requiredType = entry.getKey();
            Class<? extends PropertyEditor> editorClass = entry.getValue();
            registry.registerCustomEditor(requiredType, BeanUtils.instantiateClass(editorClass));
        }
    }
}
```

既然提到了 BeanWrapper，这里也有必要强调下， Spring 中用于封装 bean 的是 BeanWrapper 类型，而它又间接继承了 PropertyEditorRegistry 类型，也就是我们之前反复看到的方法参数 PropertyEditorRegistry registry，其实大部分情况下都是 BeanWrapper，对于 BeanWrapper 在 Spring 中的默认实现是 BeanWrapperlmpl，而 BeanWrapperlmpl 除了实现 BeanWrapper 接口外还继承了 PropertyEditorRegistrySupport，在 PropertyEditorRegistrySupport 中有这样一个方法

源代码【PropertyEditorRegistrySupport】

```java
public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {
    // 在 PropertyEditorRegistrySupport 中注册了一系列默认的属性编辑器
    private void createDefaultEditors() {
        this.defaultEditors = new HashMap<Class<?>, PropertyEditor>(64);

        // Simple editors, without parameterization capabilities.
        // The JDK does not contain a default editor for any of these target types.
        this.defaultEditors.put(Charset.class, new CharsetEditor());
        this.defaultEditors.put(Class.class, new ClassEditor());
        this.defaultEditors.put(Class[].class, new ClassArrayEditor());
        this.defaultEditors.put(Currency.class, new CurrencyEditor());
        this.defaultEditors.put(File.class, new FileEditor());
        this.defaultEditors.put(InputStream.class, new InputStreamEditor());
        this.defaultEditors.put(InputSource.class, new InputSourceEditor());
        this.defaultEditors.put(Locale.class, new LocaleEditor());
        this.defaultEditors.put(Pattern.class, new PatternEditor());
        this.defaultEditors.put(Properties.class, new PropertiesEditor());
        this.defaultEditors.put(Reader.class, new ReaderEditor());
        this.defaultEditors.put(Resource[].class, new ResourceArrayPropertyEditor());
        this.defaultEditors.put(TimeZone.class, new TimeZoneEditor());
        this.defaultEditors.put(URI.class, new URIEditor());
        this.defaultEditors.put(URL.class, new URLEditor());
        this.defaultEditors.put(UUID.class, new UUIDEditor());
        if (zoneIdClass != null) {
            this.defaultEditors.put(zoneIdClass, new ZoneIdEditor());
        }

        // Default instances of collection editors.
        // Can be overridden by registering custom instances of those as custom editors.
        this.defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
        this.defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
        this.defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
        this.defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
        this.defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

        // Default editors for primitive arrays.
        this.defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
        this.defaultEditors.put(char[].class, new CharArrayPropertyEditor());

        // The JDK does not contain a default editor for char!
        this.defaultEditors.put(char.class, new CharacterEditor(false));
        this.defaultEditors.put(Character.class, new CharacterEditor(true));

        // Spring's CustomBooleanEditor accepts more flag values than the JDK's default editor.
        this.defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
        this.defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

        // The JDK does not contain default editors for number wrapper types!
        // Override JDK primitive number editors with our own CustomNumberEditor.
        this.defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
        this.defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
        this.defaultEditors.put(short.class, new CustomNumberEditor(Short.class, false));
        this.defaultEditors.put(Short.class, new CustomNumberEditor(Short.class, true));
        this.defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
        this.defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
        this.defaultEditors.put(long.class, new CustomNumberEditor(Long.class, false));
        this.defaultEditors.put(Long.class, new CustomNumberEditor(Long.class, true));
        this.defaultEditors.put(float.class, new CustomNumberEditor(Float.class, false));
        this.defaultEditors.put(Float.class, new CustomNumberEditor(Float.class, true));
        this.defaultEditors.put(double.class, new CustomNumberEditor(Double.class, false));
        this.defaultEditors.put(Double.class, new CustomNumberEditor(Double.class, true));
        this.defaultEditors.put(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
        this.defaultEditors.put(BigInteger.class, new CustomNumberEditor(BigInteger.class, true));

        // Only register config value editors if explicitly requested.
        if (this.configValueEditorsActive) {
            StringArrayPropertyEditor sae = new StringArrayPropertyEditor();
            this.defaultEditors.put(String[].class, sae);
            this.defaultEditors.put(short[].class, sae);
            this.defaultEditors.put(int[].class, sae);
            this.defaultEditors.put(long[].class, sae);
        }
    }
}
```

##### 添加 ApplicationContextAwareProcessor 处理器

在 Spring 中可以注入一些底层的组件，怎么实现的呢？这就是 beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this)) 的作用了。首先回顾一下 ApplicationContextAware 的使用方法：

```java
// 可以使用 ApplicationContext 组件
@Service
public class Bean implements ApplicationContextAware {
    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
```

很显然 context 是在 bean 实例化完成，属性注入后准备进行 init-method 方法前完成的。我们看一下代码：

源代码【PropertyEditorRegistrySupport】

```java
// 抛开各种异常处理 Spring 作了如下的处理，这里我们重点关注 applyBeanPostProcessorsBeforeInitialization
protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
    
    // 1. 注入 Spring 组件
    invokeAwareMethods(beanName, bean);

    // 2. 执行 init-method 前的回调，ApplicationContextAware 就是在这一步注入属性
    wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);

    // 3. 执行 init-method 方法
    invokeInitMethods(beanName, wrappedBean, mbd);
    
    // 4. 执行 init-method 后的回调
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    return wrappedBean;
}

// 注入 BeanNameAware、BeanClassLoaderAware、BeanFactoryAware
private void invokeAwareMethods(final String beanName, final Object bean) {
    if (bean instanceof Aware) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof BeanClassLoaderAware) {
            ((BeanClassLoaderAware) bean).setBeanClassLoader(getBeanClassLoader());
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(AbstractAutowireCapableBeanFactory.this);
        }
    }
}

// 执行后置处理器，ApplicationContextAware 在这里注入了部分其它的 Aware
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
        throws BeansException {

    Object result = existingBean;
    for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
        result = beanProcessor.postProcessBeforeInitialization(result, beanName);
        if (result == null) {
            return result;
        }
    }
    return result;
}
```

下面我们看一下 ApplicationContextAwareProcessor 都做了那些事情，postProcessBeforeInitialization 实际上事情委托给了 invokeAwareInterfaces()

```java
class ApplicationContextAwareProcessor implements BeanPostProcessor {
    public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
           
        invokeAwareInterfaces(bean);

        return bean;
    }

    private void invokeAwareInterfaces(Object bean) {
        if (bean instanceof Aware) {
            if (bean instanceof EnvironmentAware) {
                ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
            }
            if (bean instanceof EmbeddedValueResolverAware) {
                ((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(
                        new EmbeddedValueResolver(this.applicationContext.getBeanFactory()));
            }
            if (bean instanceof ResourceLoaderAware) {
                ((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
            }
            if (bean instanceof ApplicationEventPublisherAware) {
                ((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
            }
            if (bean instanceof MessageSourceAware) {
                ((MessageSourceAware) bean).setMessageSource(this.applicationContext);
            }
            if (bean instanceof ApplicationContextAware) {
                ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
            }
        }
    }
}
```

##### 设置忽略依赖

```java
beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
```

很明显 ApplicationContextAware 不能再注入到 bean 中：

```java
public class Bean {
    @Autowired
    private ApplicationContextAware applicationContextAware;
}
```

##### 注册依赖

```java
beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
beanFactory.registerResolvableDependency(ResourceLoader.class, this);
beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
beanFactory.registerResolvableDependency(ApplicationContext.class, this);
```

Spring 还简化了底层组件的注入问题：

```java
public class Bean {
    @Autowired
    private ApplicationContext context;
}
```

### BeanPostProcessor

产生回顾一下 ApplicationContext 初始化的几个步骤：第一步是刷新环境变量；第二步是刷新 beanFactory 并加载 BeanDefinition；第三步是对 beanFactory 进行功能扩展，如增加 SPEL 支持和属性编辑器；第四步是留给子类实现的。

上一节中向 Spring 中注册将执行了 BeanFactoryPostProcessor，本节则继续探讨一下 BeanPostProcessor 的注册及调用时机。

```java
public interface BeanPostProcessor {
    // 在初始化之前调用
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;
    // 在初始化之后调用
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
}
```

1. BeanPostProcessor 什么时候注册？

   BeanFactory 和 ApplicationContext 容器的注册方式不大一样：若使用 BeanFactory，则必须要显示的调用其 addBeanPostProcessor() 方法进行注册；如果是使用 ApplicationContext，那么容器会在配置文件在中自动寻找实现了 BeanPostProcessor 接口的 Bean，然后自动注册。

2. BeanPostProcessor 如何确保调用顺序？

   假如我们使用了多个的 BeanPostProcessor 的实现类，那么如何确定处理顺序呢？其实只要实现 Ordered 接口，设置 order 属性就可以很轻松的确定不同实现类的处理顺序了；
   接口中的两个方法都要将传入的 bean 返回，而不能返回 null，如果返回的是 null 那么我们通过 getBean() 方法将得不到目标。

#### BeanPostProcessor 的注册

源代码【AbstractApplicationContext】

```java
@Override
public void refresh() throws BeansException, IllegalStateException {
    // 6. 注册 BeanPostProcessor 后置处理器，在 getBean() 创建 bean 时调用
    registerBeanPostProcessors(beanFactory);

    /**
    * 1. 实例化剩余的所有非延迟加载单例对象
    * 2. 为什么说是剩余的？因为在上面的 registerBeanPostProcessors 中已经把所有 BeanPostProcessors 所有对象都已经实例化过了;
    * 3. 这加载的时候会判断 bean 是不是 FactoryBean 类型的
    *   3.1 如果是 FactoryBean 类型，则 getBean(&beanName)，这里是把 FactoryBean 本身的对象给实例化了，而没有调它的 getObject 方法；
    *   3.2 如果不是 FactoryBean 类型，直接 getBean() 就行了；
    * 4. 还要判断是不是 SmartInitializingSingleton 接口，这个接口有个 afterSingletonsInstantiated 方法；
    */
    finishBeanFactoryInitialization(beanFactory);
}

protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
    PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
}
```

源代码【PostProcessorRegistrationDelegate】

```java
public static void registerBeanPostProcessors(
            ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
    // 1. 此时 BeanDefinition 已经加载，只是 bean 还没有被实例化
    String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

    // 2. 记录日志用
    // 可能已经注册了部分 BeanFactoryPostProcessors 接口
    int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
    beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

    // 3. 按 PriorityOrdered internal Ordered nonOrdered 四个级别
    // 3.1 优先级最高的 BeanPostProcessors，这类最先调用；需要实现 PriorityOrdered 接口
    List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
    // 3.2 内部 BeanPostProcessors
    List<BeanPostProcessor> internalPostProcessors = new ArrayList<BeanPostProcessor>();
    // 3.3 继承了 Ordered 接口，优先级比上面低一点
    List<String> orderedPostProcessorNames = new ArrayList<String>();
    // 3.4 这就是普通的了，优先级最低
    List<String> nonOrderedPostProcessorNames = new ArrayList<String>();
    for (String ppName : postProcessorNames) {
        if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            priorityOrderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
            orderedPostProcessorNames.add(ppName);
        }
        else {
            nonOrderedPostProcessorNames.add(ppName);
        }
    }

    // 4. PriorityOrdered internal Ordered nonOrdered 分别排序、初始化、注册
    // 4.1 PriorityOrdered BeanPostProcessors
    sortPostProcessors(beanFactory, priorityOrderedPostProcessors);
    registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

    // 4.2 Ordered BeanPostProcessors
    List<BeanPostProcessor> orderedPostProcessors = new ArrayList<BeanPostProcessor>();
    for (String ppName : orderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        orderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    sortPostProcessors(beanFactory, orderedPostProcessors);
    registerBeanPostProcessors(beanFactory, orderedPostProcessors);

    // 4.3 nonOrdered BeanPostProcessors
    List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<BeanPostProcessor>();
    for (String ppName : nonOrderedPostProcessorNames) {
        BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
        nonOrderedPostProcessors.add(pp);
        if (pp instanceof MergedBeanDefinitionPostProcessor) {
            internalPostProcessors.add(pp);
        }
    }
    registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

    // 4.4 internal BeanPostProcessors
    //     注意重复注册会先删除先注册的元素加添加到集合最后面，影响执行顺序
    sortPostProcessors(beanFactory, internalPostProcessors);
    registerBeanPostProcessors(beanFactory, internalPostProcessors);

    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
}

private static void registerBeanPostProcessors(
        ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

    for (BeanPostProcessor postProcessor : postProcessors) {
        beanFactory.addBeanPostProcessor(postProcessor);
    }
}
```

源代码【AbstractBeanFactory】

```java
private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

@Override
public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
    Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
    // 如果已经注册则删除后重新注册，影响其执行顺序，如 internal 中的 MergedBeanDefinitionPostProcessor
    this.beanPostProcessors.remove(beanPostProcessor);
    this.beanPostProcessors.add(beanPostProcessor);
    if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
        this.hasInstantiationAwareBeanPostProcessors = true;
    }
    if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
        this.hasDestructionAwareBeanPostProcessors = true;
    }
}
```

总结：

(1) 最后 BeanPostProcessor 的注册顺序为 PriorityOrdered、Ordered、nonOrdered、internal，其中 internal 又分为 PriorityOrdered、Ordered、nonOrdered 三种顺序。

#### BeanPostProcessor 实战

```java
class PriorityOrderTest implements BeanPostProcessor, PriorityOrdered {}
class OrderTest implements BeanPostProcessor, Ordered {}
class NoneTest implements BeanPostProcessor {}
```

#### BeanPostProcessor 调用时机

源代码【AbstractAutowireCapableBeanFactory】

```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args) {
    //省略...
    populateBean(beanName, mbd, instanceWrapper);
    if (exposedObject != null) {
        // BeanPostProcessors 两个方法都在这里面
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
}

protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
    // 省略 ...
    invokeAwareMethods(beanName, bean);

    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
        // 初始化前
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }

    try {
        // 初始化
        invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
        throw new BeanCreationException(ex);
    }

    if (mbd == null || !mbd.isSynthetic()) {
        // 初始化后
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }
    return wrappedBean;
}

@Override
public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
        throws BeansException {

    Object result = existingBean;
    for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
        result = beanProcessor.postProcessBeforeInitialization(result, beanName);
        if (result == null) {
            return result;
        }
    }
    return result;
}
```

### 事件监听机制

本节则重点关注的是 Spring 的事件监听机制，主要是第 8 步：多播器注册；第 10 步：事件注册。

```java
public void refresh() throws BeansException, IllegalStateException {
    // 8. 注册多播器，事件监听器的管理者
    initApplicationEventMulticaster();
    // 9. 专门留给子类初始化其它 bean 用，这是一个空的方法
    onRefresh();
    // 10. 注册监听器
    registerListeners();
}
```

事件定义如下，实现了 JDK 的规范 EventListener

```java
public interface ApplicationListener<E extends ApplicationEvent> extends EventListener {
    // 监听 event 事件
    void onApplicationEvent(E event);
}
```

#### ApplicationListener 实战

```java
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationListener implements ApplicationListener<ApplicationEvent> {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("接收的事件：" + event);
    }
}
```

运行后结果如下：

```text
接收的事件：org.springframework.context.event.ContextRefreshedEvent[source=org.springframework.context.support.ClassPathXmlApplicationContext@16eabae: startup date [Sun Jul 29 12:41:42 CST 2018]; root of context hierarchy]
```

#### ApplicationEventMulticaster 多播器的初始化

ApplicationContext 中 refresh() 第 8 步 initApplicationEventMulticaster() 进行多播器的初始化工作

源代码【AbstractApplicationContext】

```java
protected void initApplicationEventMulticaster() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
        this.applicationEventMulticaster =
                beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
    }
    else {
        this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
        beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
    }
}
```

#### ApplicationListener 注册

ApplicationContext 中 refresh() 第 10 步 registerListeners() 进行事件监听者的注册工作。

源代码【AbstractApplicationContext】

```java
protected void registerListeners() {
    // 1. 注册静态指定的侦听器
    for (ApplicationListener<?> listener : getApplicationListeners()) {
        getApplicationEventMulticaster().addApplicationListener(listener);
    }

    // 2. 注册 ApplicationListener
    String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
    for (String listenerBeanName : listenerBeanNames) {
        getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
    }

    // 3. Publish early application events
    Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
    this.earlyApplicationEvents = null;
    if (earlyEventsToProcess != null) {
        for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
            getApplicationEventMulticaster().multicastEvent(earlyEvent);
        }
    }
}
```

#### ApplicationEvent 事件发布

源代码【AbstractApplicationContext】

```java
public void publishEvent(ApplicationEvent event) {
    publishEvent(event, null);
}

public void publishEvent(Object event) {
    publishEvent(event, null);
}

protected void publishEvent(Object event, ResolvableType eventType) {
    Assert.notNull(event, "Event must not be null");
   
    // 1. 如果 event 不是 ApplicationEvent，则需要进行封装成 PayloadApplicationEvent
    ApplicationEvent applicationEvent;
    if (event instanceof ApplicationEvent) {
        applicationEvent = (ApplicationEvent) event;
    }
    else {
        applicationEvent = new PayloadApplicationEvent<Object>(this, event);
        if (eventType == null) {
            eventType = ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, event.getClass());
        }
    }

    // 2. 发布事件 event，如果多播器懒加载，还没有初始化则将该事件先放到 earlyApplicationEvents 容器中
    //    等待多播器创建好了再发布事件 ???
    if (this.earlyApplicationEvents != null) {
        this.earlyApplicationEvents.add(applicationEvent);
    }
    else {
        getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
    }

    // 3. 父容器中也需要发布该事件 event
    if (this.parent != null) {
        if (this.parent instanceof AbstractApplicationContext) {
            ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
        }
        else {
            this.parent.publishEvent(event);
        }
    }
}
```

### 初始化非延迟的 bean

此至，ApplicationContext 已经完成了全部的准备工作，开始初始化剩余的 bean 了(第 11 步)。

```java
public void refresh() throws BeansException, IllegalStateException {
    // 11. 初始化剩余的 bean (部分在 invokeBeanFactoryPostProcessors 已经初始化)
    finishBeanFactoryInitialization(beanFactory);
}
```

finishBeanFactoryInitialization 主要是实例化非懒加载的 bean。

```java
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
    // 1. 注册 ConversionService，注意 beanName 必须为 conversionService
    if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
            beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
        beanFactory.setConversionService(
                beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
    }

    // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
    String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
    for (String weaverAwareName : weaverAwareNames) {
        getBean(weaverAwareName);
    }

    // Stop using the temporary ClassLoader for type matching.
    beanFactory.setTempClassLoader(null);

    // 2. 冻结所有的 bean 定义，说明注册的 bean 定义将不被修改
    beanFactory.freezeConfiguration();

    // 3. 初始化剩下的 bean
    beanFactory.preInstantiateSingletons();
}
```

#### 注册 ConversionService

(1) String2DateConverter

```java
public class String2DateConverter implements Converter<String, Date> {
    @Override
    public Date convert(String source) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse(source);
        } catch (ParseException e) {
            ;
        }
        return null;
    }
}
```

(2) 注册

注意：beanName 必须为 conversionService

```xml
<bean id="conversionService" class="org.springframework.context.support.ConversionServiceFactoryBean">
    <property name="converters">
        <set>
            <bean class="com.github.binarylei.spring01.day0728.conversionservice.String2DateConverter"/>
        </set>
    </property>
</bean>
```

(3) 测试

```java
@Test
public void test() {
    DefaultConversionService service = new DefaultConversionService();
    service.addConverter(new String2DateConverter());

    Date value = service.convert("2018-08-02", Date.class);
    System.out.println(value);
}
```

#### 冻结配置

源代码【DefaultListableBeanFactory】

```java
public void freezeConfiguration() {
    this.configurationFrozen = true;
    this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
}
```

#### 初始化非延迟加载

```java
public void preInstantiateSingletons() throws BeansException {
    if (this.logger.isDebugEnabled()) {
        this.logger.debug("Pre-instantiating singletons in " + this);
    }

    // 1. 创建剩下的单实例 bean
    List<String> beanNames = new ArrayList<String>(this.beanDefinitionNames);

    // Trigger initialization of all non-lazy singleton beans...
    for (String beanName : beanNames) {
        RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
        if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
            if (isFactoryBean(beanName)) {
                final FactoryBean<?> factory = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                boolean isEagerInit;
                if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                    isEagerInit = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return ((SmartFactoryBean<?>) factory).isEagerInit();
                        }
                    }, getAccessControlContext());
                }
                else {
                    isEagerInit = (factory instanceof SmartFactoryBean &&
                            ((SmartFactoryBean<?>) factory).isEagerInit());
                }
                if (isEagerInit) {
                    getBean(beanName);
                }
            }
            else {
                getBean(beanName);
            }
        }
    }

    // 2. 筛选出实现了 SmartInitializingSingleton 接口的 bean，回调 afterSingletonsInstantiated() 方法
    //    @EventListener 注解的方式注册监听器就是在这一步完成的
    for (String beanName : beanNames) {
        Object singletonInstance = getSingleton(beanName);
        if (singletonInstance instanceof SmartInitializingSingleton) {
            final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    @Override
                    public Object run() {
                        smartSingleton.afterSingletonsInstantiated();
                        return null;
                    }
                }, getAccessControlContext());
            }
            else {
                smartSingleton.afterSingletonsInstantiated();
            }
        }
    }
}
```

### finishRefresh

经过以上 11 步，ApplicationContext 的刷新工作基本完成，就剩下最后一点收尾的工作。

在 Spring 中还提供了 Lifecycle 接口， Lifecycle 中包含 start/stop 方法，实现此接口后 Spring 会保证在启动的时候调用其 start 方法开始生命周期，并在 Spring 关闭的时候调用 stop 方法来结束生命周期，通常用来配置后台程序，在启动后一直运行(如对 MQ 进行轮询等)而 ApplicationContext 的初始化最后正是保证了这一功能的实现。

```java
protected void finishRefresh() {
    // Initialize lifecycle processor for this context.
    initLifecycleProcessor();

    // Propagate refresh to lifecycle processor first.
    getLifecycleProcessor().onRefresh();

    // Publish the final event.
    publishEvent(new ContextRefreshedEvent(this));

    // Participate in LiveBeansView MBean, if active.
    LiveBeansView.registerApplicationContext(this);
}
```

#### initLifecycleProcessor

当 ApplicationContext 启动或停止时，它会通过 initLifecycleProcessor 来与所有声明的 bean 的周期做状态更新，而在 LifecycleProcessor 的使用前首先需要初始化。

```java
protected void initLifecycleProcessor() {
    ConfigurableListableBeanFactory beanFactory = getBeanFactory();
    if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
        this.lifecycleProcessor =
                beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
    }
    else {
        DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
        defaultProcessor.setBeanFactory(beanFactory);
        this.lifecycleProcessor = defaultProcessor;
        beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
    }
}
```

#### onRefresh

启动所有实现了 Lifecycle 接口的 bean

```java
public void onRefresh() {
    startBeans(true);
    this.running = true;
}

// Spring 内部用
private void startBeans(boolean autoStartupOnly) {
    Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
    Map<Integer, LifecycleGroup> phases = new HashMap<Integer, LifecycleGroup>();
    for (Map.Entry<String, ? extends Lifecycle> entry : lifecycleBeans.entrySet()) {
        Lifecycle bean = entry.getValue();
        if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
            int phase = getPhase(bean);
            LifecycleGroup group = phases.get(phase);
            if (group == null) {
                group = new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
                phases.put(phase, group);
            }
            group.add(entry.getKey(), bean);
        }
    }
    if (phases.size() > 0) {
        List<Integer> keys = new ArrayList<Integer>(phases.keySet());
        Collections.sort(keys);
        for (Integer key : keys) {
            phases.get(key).start();
        }
    }
}
```

#### publishEvent

当完成 ApplicationContext 初始化的时候，要通过 Spring 中的事件发布机制来发出 ContextRefreshedEvent 事件，以保证对应的监听器可以做进一步的逻辑处理。

```java
protected void publishEvent(Object event, ResolvableType eventType) {
    Assert.notNull(event, "Event must not be null");

    // 1. 如果 event 不是 ApplicationEvent，则需要进行封装成 PayloadApplicationEvent
    ApplicationEvent applicationEvent;
    if (event instanceof ApplicationEvent) {
        applicationEvent = (ApplicationEvent) event;
    }
    else {
        applicationEvent = new PayloadApplicationEvent<Object>(this, event);
        if (eventType == null) {
            eventType = ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, event.getClass());
        }
    }

    // 2. 发布事件 event，如果多播器懒加载，还没有初始化则将该事件先放到 earlyApplicationEvents 容器中
    //    等待多播器创建好了再发布事件 ???
    if (this.earlyApplicationEvents != null) {
        this.earlyApplicationEvents.add(applicationEvent);
    }
    else {
        getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
    }

    // 3. 父容器中也需要发布该事件 event
    if (this.parent != null) {
        if (this.parent instanceof AbstractApplicationContext) {
            ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
        }
        else {
            this.parent.publishEvent(event);
        }
    }
}
```