[toc]



# Spring 核心容器 - Bean

## 1.Bean的基础知识

1. 在xml配置文件中，bean的标识（id 和 name）

    `id`:指定在benafactory中管理该bean的唯一的标识。name可用来唯一标识bean 或给bean起别名。

   ```
    <bean id="helloWorld" class="com.name.HelloWorld">
   ........
    </bean>
   ```

2. bean的class

   class属性指定了bean的来源，即bean的实际路径。注意要指定全路径，而不可只写类名。

3. Singleton的使用

   在spring中，bean可被定义为2中部署模式中的一种。singleton和prototype模式。

   `singleton`:只有一个共享的实例存在，所有对这个bean的请求都会返回这个唯一实例。

   `prototype`:对这个bean的每次请求都会都会创建一个新的bean实例。根据已经存在的bean而clone出来的bean。默认为singleton模式。

    改写成prototype模式写法如下：

   ```
   <bean id="student3" class="com.mucfc.beanfactory.Student" scope="prototype">
   	.......
   </bean>
   ```

4. bean的属性

   spring中，bean的属性值有2种注入方式。setter注入和构造函数注入。

   setter注入是在调用无参的构造函数或无参的静态工厂方法实例化配置文档中定义的bean之后，通过调用bean上的setter方法实现的。

   构造函数的依赖注入是通过调用带有很多参数的构造方法实现的，每个参数表示一个对象或者属性。

5. 对属性null值的处理

    ```
    <bean id="student5" class="com.mucfc.beanfactory.Student">
        <property name="std_name">
            <value></value>
        </property>
        <property name="std_id">
            <value>2005</value>
        </property>
    </bean>
    ```
    
    或者

    ```
    <bean id="student5" class="com.mucfc.beanfactory.Student">
        <property name="std_name">
            <value/>
        </property>
        <property name="std_id">
            <value>2005</value>
        </property>
    </bean>
    ```
    
    以上等同于this.std_name=null

6. 使用依赖depends-on

   此属性可在使用该bean之前，强制初始化一个或多个bean的初始化。例如

    ```
    <bean id="school" class="com.mucfc.beanfactory.School"
        depends-on="student6">
        <property name="student" ref="student6" />
    </bean>
    ```
   
   其中student6如下：

    ```
    <bean id="student6" class="com.mucfc.beanfactory.Student">
        <property name="std_name" value="水水" />
        <property name="std_id" value="3009" />
    </bean>
    ```

7. 指定bean引用

    ```
    <property name=" xxx" ref="yyyy "/>
    ```

	或者

    ```
    <property name="xxxx">
        <ref bean="yyt"/>
    <property/>
    ```

8. ref local指定同一个xml文件中的引用 

## 2.Bean的生命周期

**首先简单说一下**

1、实例化一个Bean－－也就是我们常说的new；

2、按照Spring上下文对实例化的Bean进行配置－－也就是IOC注入；

3、如果这个Bean已经实现了BeanNameAware接口，会调用它实现的 `setBeanName(String)` 方法，此处传递的就是Spring配置文件中Bean的id值

4、如果这个Bean已经实现了BeanFactoryAware接口，会调用它实现的`setBeanFactory(BeanFactory)`传递的是Spring工厂自身（可以用这个方式来获取其它Bean，只需在Spring配置文件中配置一个普通的Bean就可以）；

5、如果这个Bean已经实现了ApplicationContextAware接口，会调用`setApplicationContext(ApplicationContext)`方法，传入Spring上下文（同样这个方式也可以实现步骤4的内容，但比4更好，因为ApplicationContext是BeanFactory的子接口，有更多的实现方法）；

6、如果这个Bean关联了BeanPostProcessor接口，将会调用 `postProcessBeforeInitialization(Object obj, String s)` 方法，BeanPostProcessor经常被用作是Bean内容的更改，并且由于这个是在Bean初始化结束时调用那个的方法，也可以被应用于内存或缓存技术；

7、如果Bean在Spring配置文件中配置了init-method属性会自动调用其配置的初始化方法。

8、如果这个Bean关联了BeanPostProcessor接口，将会调用 `postProcessAfterInitialization(Object obj, String s)` 方法；

> 注：以上工作完成以后就可以应用这个Bean了，那这个Bean是一个Singleton的，所以一般情况下我们调用同一个id的Bean会是在内容地址相同的实例，当然在Spring配置文件中也可以配置非Singleton，这里我们不做赘述。

9、当Bean不再需要时，会经过清理阶段，如果Bean实现了DisposableBean这个接口，会调用那个其实现的destroy()方法；

10、最后，如果这个Bean的Spring配置中配置了destroy-method属性，会自动调用其配置的销毁方法。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329212437.jpeg)

**结合代码理解一下**

### 2.1.Bean的定义

Spring通常通过配置文件定义Bean。如：

```
<?xml version=”1.0″ encoding=”UTF-8″?>

<beans xmlns=”http://www.springframework.org/schema/beans”
xmlns:xsi=”http://www.w3.org/2001/XMLSchema-instance”
xsi:schemaLocation=”http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.0.xsd”>

<bean id=”HelloWorld” class=”com.pqf.beans.HelloWorld”>
    <property name=”msg”>
       <value>HelloWorld</value>
    </property>
</bean>

</beans>
```

这个配置文件就定义了一个标识为 HelloWorld 的Bean。在一个配置文档中可以定义多个Bean。

### 2.2.Bean的初始化

有两种方式初始化Bean。

**2.2.1.在配置文档中通过指定init-method 属性来完成**

在Bean的类中实现一个初始化Bean属性的方法，如 `init()`，如：

```
public class HelloWorld{
   public String msg=null;
   public Date date=null;

    public void init() {
      msg=”HelloWorld”;
      date=new Date();
    }
    …… 
}
```

然后，在配置文件中设置 `init-mothod` 属性：

**2.2.2.实现 org.springframwork.beans.factory.InitializingBean接口**

Bean实现InitializingBean接口，并且增加 afterPropertiesSet() 方法：

```
public class HelloWorld implement InitializingBean {
   public String msg=null;
   public Date date=null;

   public void afterPropertiesSet() {
       msg="向全世界问好！";
       date=new Date();
   }
    …… 
}
```

那么，当这个 `Bean` 的所有属性被 `Spring` 的 `BeanFactory` 设置完后，会自动调用 `afterPropertiesSet()` 方法对Bean进行初始化，于是，配置文件就不用指定 `init-method` 属性了。

### 2.3.Bean的调用

有三种方式可以得到Bean并进行调用：

**2.3.1.使用BeanWrapper**

```
HelloWorld hw=new HelloWorld();
BeanWrapper bw=new BeanWrapperImpl(hw);
bw.setPropertyvalue(”msg”,”HelloWorld”);
system.out.println(bw.getPropertyCalue(”msg”));
```

**2.3.2.使用BeanFactory**

```
InputStream is=new FileInputStream(”config.xml”);
XmlBeanFactory factory=new XmlBeanFactory(is);
HelloWorld hw=(HelloWorld) factory.getBean(”HelloWorld”);
system.out.println(hw.getMsg());
```

**2.3.3.使用ApplicationContext**

```
ApplicationContext actx=new FleSystemXmlApplicationContext(”config.xml”);
HelloWorld hw=(HelloWorld) actx.getBean(”HelloWorld”);
System.out.println(hw.getMsg());
```

### 2.4.Bean的销毁

**2.4.1.使用配置文件中的 destory-method 属性**

与初始化属性 `init-methods` 类似，在Bean的类中实现一个撤销Bean的方法，然后在配置文件中通过 `destory-method` 指定，那么当bean销毁时，Spring将自动调用指定的销毁方法。

**2.4.2.实现 org.springframwork.bean.factory.DisposebleBean接口**

如果实现了DisposebleBean接口，那么Spring将自动调用bean中的Destory方法进行销毁，所以，Bean中必须提供Destory方法。

**图解**

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329001917.jpeg)



## 3.Bean对象创建时机

- 默认 `lazy-init=default||false`：当spring容器实例化的时候，并把容器中对象全部完成实例化

    ```
    <bean id="bean" class="com.tarena.entity.Bean"  lazy-init="false"/>
    ```

- `lazy-init="true"`：当从spring容器中获取对象时候在对对象实例始化

- 设置全局 `default-lazy-init="true"`： 整个配置文件中对象都实例化延迟

  ```
  <beans  ... 
  	default-lazy-init="true">
  </beans>
  ```


  注意：在使用定时器的时候，不能使用 `lazy-init="true"`

## 4.Bean 的5种作用域介绍

Spring Bean 中所说的作用域，在配置文件中即是“scope”。在面向对象程序设计中作用域一般指对象或变量之间的可见范围。而在Spring容器中是指其创建的Bean对象相对于其他Bean对象的请求可见范围。在Spring 容器当中，一共提供了5种作用域类型，在配置文件中，通过属性scope来设置bean的作用域范围。

1. **singleton**

   ```
   <bean id="userInfo" class="cn.lovepi.UserInfo" scope="singleton"></bean>
   ```

   当Bean的作用域为singleton的时候,Spring容器中只会存在一个共享的Bean实例，所有对Bean的请求只要id与bean的定义相匹配，则只会返回bean的同一实例。单一实例会被存储在单例缓存中，为Spring的缺省作用域。

   **作用域**

   是指在Spring IoC容器中仅存在一个Bean的示例，Bean以单实例的方式存在，**单实例模式**是重要的设计模式之一，在Spring中对此实现了超越，可以对那些**非线程安全的对象**采用单实例模式。

   接下来看一个示例：

   ```
   <bean id="car" class="cn.lovepi.Car" scope="singleton"></bean>
   <bean id="boss1" class="cn.lovepi .Boss” p:car-ref=“car"></bean>
   <bean id="boss2" class="cn.lovepi .Boss” p:car-ref=“car"></bean>
   <bean id="boss3" class="cn.lovepi .Boss” p:car-ref=“car"></bean>
   ```

   在1中car这个Bean生命周期声明为了singleton模式，其他的bean如2，3，4引用了这个Bean。在容器中boss1、boss2、boss3的属性都指向同一个bean car。如下图所示： 

   ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328212421.png)

   不仅在容器中通过对象引入配置注入引用相同的car bean，通过容器的getBean()方法返回的实例也指向同一个bean。

   在默认的情况下Spring的ApplicationContext容器在启动的时候，自动实例化所有的singleton的bean并缓存于容器当中。

   虽然启动时会花费一定的时间，但是他却带来了两个**好处**:

   1. 对bean提前的实例化操作，会及早发现一些潜在的配置的问题。
   2. Bean以缓存的方式运行，当运行到需要使用该bean的时候，就不需要再去实例化了。加快了运行效率。

2. **prototype**

   ```
   <bean id="userInfo" class="cn.lovepi.UserInfo" scope="prototype "></bean>
   ```

   每次对该Bean请求的时候，Spring IoC都会创建一个新的作用域。

   对于有状态的Bean应该使用prototype，对于无状态的Bean则使用singleton

   **作用域**

   是指每次从容器中调用Bean时，都返回一个新的实例，即每次调用getBean()时，相当于执行new Bean()的操作。在默认情况下，Spring容器在启动时不实例化prototype的Bean。

   接下来看一个示例：

   ```
   <bean id=“car" class="cn.lovepi.Car" scope=“prototype"></bean>
   <bean id=“boss1" class="cn.lovepi.Boss” p:car-ref=“car"></bean>
   <bean id=“boss2" class="cn.lovepi.Boss” p:car-ref=“car"></bean>
   <bean id=“boss3" class="cn.lovepi.Boss” p:car-ref=“car"></bean>
   ```

   通过以上的配置，Boss1、boss2、boss3所引用的都是一个新的car的实例，每次通过容器getBean()方法返回的也是一个新的car实例。如下图所示：

   ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210328212638.png)

   在默认情况下，Spring容器在启动时不实例化prototype这种bean。此外，Spring容器将prototype的实例交给调用者之后便不在管理他的生命周期了。

3. **request**

   ```
   <bean id="userInfo" class="cn.lovepi.UserInfo" scope="request "></bean>
   ```

   Request作用域针对的是每次的Http请求，Spring容器会根据相关的Bean的定义来创建一个全新的Bean实例。而且该Bean只在当前request内是有效的。

   **作用域**

   对应一个http请求和生命周期，当http请求调用作用域为request的bean的时候,Spring便会创建一个新的bean，在请求处理完成之后便及时销毁这个bean。

4. **session**

   ```
   <bean id="userInfo" class="cn.lovepi.UserInfo" scope="session "></bean>
   ```

   针对http session起作用，Spring容器会根据该Bean的定义来创建一个全新的Bean的实例。而且该Bean只在当前http session内是有效的。

   **作用域**

   Session中所有http请求共享同一个请求的bean实例。Session结束后就销毁bean。

5. **global session**

   ```
   <bean id="userInfo" class="cn.lovepi.UserInfo"scope=“globalSession"></bean>
   ```

   类似标准的http session作用域，不过仅仅在基于portlet的web应用当中才有意义。Portlet规范定义了全局的Session的概念。他被所有构成某个portlet外部应用中的各种不同的portlet所共享。在global session作用域中所定义的bean被限定于全局的portlet session的生命周期范围之内。

   **作用域**

   与session大体相同，但仅在**portlet**应用中使用。



## 5.Bean对象初始化和销毁

- 在spring配置文件定义销毁方法和初始化方法

  ```
  <bean init-method="init" destroy-method="destroy">
  ```

- 在Bean 对象中定义销毁方法和初始化方法

  ```
  public void init(){}
  public void destroy(){}
  ```

- spring容器自动调用销毁方法和初始化方法

注意：销毁方法在spring容器销毁才去调用 `AbstractApplicationContext` 提供销毁容器方法 `close();`//销毁容器

**Bean对象时多例(scope="prototype")不支持destroy(){}销毁**

测试类:

	public class Bean {
		
		public void show(){
			System.out.println("我是一个豆子");
		}
		
		public Bean() {
			System.out.println("我出生了");
		}
		
		//定义初始化方法
		public void init(){
			System.out.println("执行init方法");
		}
		
		public void destroy(){
			System.out.println("执行destroy");
		}
		
		public static void main(String[] args) {
			AbstractApplicationContext ac = new 
				FileSystemXmlApplicationContext("classpath:applicationContext.xml");
			Bean bean = (Bean)ac.getBean("bean");
			bean.show();
			ac.close();
	 
		}
	}
当配置文件中

```
<bean id="bean" class="com.tarena.entity.Bean" init-method="init" destroy-method="destroy" />
```

运行结果:

```
我出生了
执行init方法
我是一个豆子
执行destroy
```

当配置文件中

```
<bean id="bean" class="com.tarena.entity.Bean" init-method="init" destroy-method="destroy" scope="prototype"/>
```

运行结果

```
运行结果:
我出生了
执行init方法
我是一个豆子
```

