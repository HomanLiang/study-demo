[toc]



# Spring AOP

## 1.AOP概述

`AOP`（`Aspect Oriented Programing`）即面向切面编程，适用于那些具有横切逻辑的应用场合，是 `OOP` 的重要补充。`AOP` 有几个重要的概念：

1. 连接点（`Joinpoint`）：一段代码中一些具有边界性质的点，如类开始初始化前、类初始化后、方法调用前后、方法抛出异常。`Spring` 仅支持方法的连接点。

2. 切点（`Pointcut`）：`AOP` 通过切点定位连接点，相当于是连接点的定位条件。在 `Spring` 中，切点通过`org.springframework.aop.Pointcut` 接口进行描述。

3. 增强（`Advice`）：增强是指在目标连接点上织入一段程序代码。

4. 目标类（`Target`）：织入增强逻辑的目标类。

5. 引介（`Introduction`）： 引介是一种特殊的增强，它为类添加一些属性和方法。这样，即使一个业务类原本没有实现某个接口，通过AOP的引介功能，我们可以动态地为该业务类添加接口的实现逻辑，让业务类成为这个接口的实现类。

6. 织入（`Weaving`）：是将增强添加对目标类具体连接点上的过程。根据不同的实现技术，`AOP` 有三种织入的方式（`Spring` 采用动态代理织入，而 `AspectJ` 采用编译期织入和类装载期织入）：

   a. 编译期织入，这要求使用特殊的Java编译器。

   b. 类装载期织入，这要求使用特殊的类装载器。

   c. 动态代理织入，在运行期为目标类添加增强生成子类的方式。

7. 代理（`Proxy`）：一个类被 `AOP` 织入增强后，就产出了一个结果类，它是融合了原类和增强逻辑的代理类。根据不同的代理方式，代理类既可能是和原类具有相同接口的类，也可能就是原类的子类，所以我们可以采用调用原类相同的方式调用代理类。

8. 切面（`Aspect`）：切面由切点和增强（引介）组成，它既包括了横切逻辑的定义，也包括了连接点的定义。`Spring AOP` 就是负责实施切面的框架，它将切面所定义的横切逻辑织入到切面所指定的连接点中。

## 2.Java中的代理

`Spring AOP` 使用动态代理技术在运行期织入增强的代码，包括**基于JDK的动态代理和基于CGLib的动态代理**。之所以需要两种代理机制，很大程度上是因为JDK本身只提供接口的代理，而不支持类的代理。

### 2.1. JDK动态代理

#### 2.1.1.不使用代理的代码

下面的例子定义了一个 `Barber` 接口定义了理发师应该有的洗剪吹方法，我们需要对每个理发师的洗剪吹方法进行时间统计，于是在`BarberTony` 托尼老师的每个方法中都加入了时间统计的代码。这样的实现方式造成了代码的臃肿，几乎同样的代码多次重复。

```java
public class Main {
    public static void main(String[] args) {
        Barber barber = new BarberTony();
        barber.wash();
        barber.cut();
        barber.blow();
    }
}

/**
 * 理发师的抽象接口,包含洗剪吹三个方法
 */
interface Barber {
    void wash();
    void cut();
    void blow();
}

class BarberTony implements Barber {
    @Override
    public void wash() {
        // (1)此处代码臃肿需要移除
        Monitor.begin(this.getClass().getSimpleName(), "洗发时间:");
        try {
            // 模拟方法执行时间
            Thread.sleep(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // (2)此处代码臃肿需要移除
        Monitor.end(this.getClass().getSimpleName(), "洗发时间:");
    }

    @Override
    public void cut() {
        Monitor.begin(this.getClass().getSimpleName(), "剪发时间:");
        try {
            // 模拟方法执行时间
            Thread.sleep(200);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Monitor.end(this.getClass().getSimpleName(), "剪发时间:");
    }

    @Override
    public void blow() {
        Monitor.begin(this.getClass().getSimpleName(), "吹头发时间:");
        try {
            // 模拟方法执行时间
            Thread.sleep(200);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Monitor.end(this.getClass().getSimpleName(), "吹头发时间:");
    }
}

class Monitor {
    private static Map<String, Long> record = new HashMap<>();

    public static void begin(String barber, String method) {
        record.put(barber + method, System.currentTimeMillis());
    }

    public static void end(String barber, String method) {
        System.out.println(barber + method + (System.currentTimeMillis() - record.get(barber + method)));
    }
}
```

#### 2.1.2.使用动态代理

`JDK` 的动态代理主要涉及到 `java.lang.reflect` 中的两个类：`Proxy` 和 `InvocationHandler`。其中 `InvocationHandler` 是一个接口，可以通过实现该接口定义横切逻辑，并通过反射机制调用目标类的代码，动态将横切逻辑和业务逻辑编织在一起。而 `Proxy` 利用`invocationHandler` 动态创建一个符合某一接口的实例，生成目标类的代理对象。

```java
public class Main {
    public static void main(String[] args) {
        Barber barber = new BarberTony();

        MonitorHandler monitorHandler = new MonitorHandler(barber);
        
        // barberProxy是最终的代理类
        Barber barberProxy = (Barber)Proxy.newProxyInstance(barber.getClass().getClassLoader(), barber.getClass().getInterfaces(), monitorHandler);

        barberProxy.wash();
        barberProxy.cut();
        barberProxy.blow();
    }
}

class MonitorHandler implements InvocationHandler {
    // barber为要代理的目标类
    private Barber barber;

    public MonitorHandler(Barber barber) {
        this.barber = barber;
    }

    /**
     * @param proxy 是最终生成的代理实例，一般不会用到
     * @param method 传入的方法
     * @param args 方法的参数
     * @return method.invoke执行后的结果
     * @throws Throwable
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Monitor.begin(barber.getClass().getSimpleName(), ":" + method.getName() + ":");
        Object obj = method.invoke(barber, args);
        Monitor.end(barber.getClass().getSimpleName(), ":" + method.getName() +  ":");
        return obj;
    }
}

class Monitor {
    private static Map<String, Long> record = new HashMap<>();

    public static void begin(String barber, String method) {
        record.put(barber + method, System.currentTimeMillis());
    }

    public static void end(String barber, String method) {
        System.out.println(barber + method + (System.currentTimeMillis() - record.get(barber + method)));
    }
}
```

### 2.2. CGLib动态代理

使用 `JDK` 创建代理有一个限制，即它只能为接口创建代理实例，因为 `Proxy` 的接口方法 `newProxyInstance` 的入参只能接受`interfaces`。`CGLib` 采用底层的字节码技术，可以为一个类创建子类，在子类中采用方法拦截技术拦截所有父类方法的调用并织入横切逻辑。

```java
public class Main {
    public static void main(String[] args) {
        BarberTony tonyProxy = (BarberTony)new BarberTonyProxy().getProxy(BarberTony.class);
        tonyProxy.wash();
        tonyProxy.cut();
        tonyProxy.blow();
    }
}

// 不再需要Barber接口
class BarberTony {
    public void wash() {
        try {
            // 模拟方法执行时间
            Thread.sleep(100);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void cut() {
        try {
            // 模拟方法执行时间
            Thread.sleep(200);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void blow() {
        try {
            // 模拟方法执行时间
            Thread.sleep(200);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

//  继承MethodInterceptor并在intercept中实现增强逻辑
class BarberTonyProxy implements MethodInterceptor {
    private Enhancer enhancer = new Enhancer();

    public Object getProxy(Class clz) {
        // 设置要被代理的类
        enhancer.setSuperclass(clz);
        enhancer.setCallback(this);
        // 通过字节码技术动态创建子类实例
        return enhancer.create();
    }

    /**
     * 此方法拦截父类所有的方法调用
     * @param o 由CGLib动态生成的代理类实例
     * @param method 上文中实体类所调用的被代理的方法引用
     * @param objects 参数值列表
     * @param methodProxy 生成的代理类对方法的代理引用
     * @return 从代理实例的方法调用返回的值
     * @throws Throwable
     */
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        String barber = o.getClass().getSimpleName().split("\\$")[0];

        Monitor.begin(barber, method.getName());
        // 调用代理类调用父类中的方法
        Object obj = methodProxy.invokeSuper(o, objects);
        Monitor.end(barber, method.getName());
        return obj;
    }
}
```

### 2.3. 总结

#### 2.3.1.JDK或CGLib的动态代理不足之处

1. 目标类的所有方法都添加了横切逻辑，但这并不是我们所期望的，我们可能只希望对业务类中的某些特定的方法添加横切逻辑；
2. 我们通过硬编码的方式制定了织入横切逻辑的织入点，即在目标业务方法的开始和结束前织入代码；
3. 我们手工编写代理实例的创建过程，为不同类创建代理时，需要分别编写相应的创建代码，无法做到通用。

#### 2.3.2.JDK与CGLib比较

- `CGLib` 所创建的动态代理对象的性能比 `JDK` 的高大概10倍
- `CGLib` 在创建代理对象的时间比 `JDK` 大概多8倍
- 对于 `singleton` 的代理对象或者具有实例池的代理，因为无需重复的创建代理对象，所以比较适合 `CGLib` 动态代理技术，反之选择 `JDK` 代理

## 3. 创建增强类

### 3.1. Spring支持的增强类型

![14623831-b4e1c2bfb455fe9d](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329222026.webp)

AOP联盟为增强定义了 `org.aopalliance.aop.Advice` 接口，Spring支持5种类型的增强：

1. 前置增强：`org.springframework.aop.BeforeAdvice` 代表前置增强，因为Spring 只支持方法级的增强，所以`MethodBeforeAdvice` 是目前可用的前置增强，表示在目标方法执行前实施增强，而 `BeforeAdvice` 是为了将来版本扩展需要而定义的；
2. 后置增强：`org.springframework.aop.AfterReturningAdvice` 代表后增强，表示在目标方法执行后实施增强；
3. 环绕增强：`org.aopalliance.intercept.MethodInterceptor` 代表环绕增强，表示在目标方法执行前后实施增强；
4. 异常抛出增强：`org.springframework.aop.ThrowsAdvice` 代表抛出异常增强，表示在目标方法抛出异常后实施增强；
5. 引介增强：`org.springframework.aop.IntroductionInterceptor` 代表引介增强，表示在目标类中添加一些新的方法和属性。

> AOP联盟规范了一套用于规范AOP实现的底层API，通过这些统一的底层API，可以使得各个AOP实现及工具产品之间实现相互移植。这些API主要以标准接口的形式提供，是AOP编程思想所要解决的横切交叉关注点问题各部件的最高抽象。Spring的AOP框架中也直接以这些API为基础所构建。
>
> AOP联盟的API主要包括四个部分：
>
> - 第一个是aop包，定义了一个表示通知Advice的标识接口，各种各样的通知都继承或实现了该接口；aop包中还包括了一个用于描述AOP系统框架错误的运行时异常AspectException。 
> - 第二个部分是intercept包，也就是拦截器包，这个包中规范了AOP核心概念中的连接点及通知(Advice)类型。 
> - 第三及第四部分是instrument及reflect包。这两个包中的API主要包括AOP框架或产品为了实现把横切关注点的模块与核心应用模块组合集成，所需要使用的设施、技术及底层实现规范等。

### 3.2.前置增强

`BeforeAdvice` 是前置增强的接口，`MethodBeforeAdvice` 是其子类。

```java
public class Main {
    public static void main(String[] args) {
        ProxyFactory proxyFactory = new ProxyFactory();
        // 设置代理的目标类
        proxyFactory.setTarget(new BarberTony());
        // 为目标类添加增强
        proxyFactory.addAdvice(new BarberTonyBeforeAdvice());

        BarberTony barberTonyProxy = (BarberTony) proxyFactory.getProxy();
        barberTonyProxy.cut();
    }
}

class BarberTonyBeforeAdvice implements MethodBeforeAdvice {
    /**
     * 在此实现前置增强的逻辑
     * @param method 目标类的方法
     * @param objects 目标类方法的入参
     * @param o 目标类实例
     * @throws Throwable 该方法抛出异常将会组织目标类的方法执行
     */
    public void before(Method method, Object[] objects, Object o) throws Throwable {
        // doSomething()
    }
}

// 洗剪吹的Tony老师
class BarberTony {
    public void wash() {
        System.out.println("washing hair");
    }

    public void cut() {
        System.out.println("cutting hair");
    }

    public void dry() {
        System.out.println("drying hair");
    }
}
```

#### 3.2.1.代理工厂类ProxyFactory

使用代理工厂类 `ProxyFactory` 将增强织入到目标类中，这个 `JDK` 的 `Proxy` 和 `InvocationHandler` 如出一辙，事实上 `ProxyFactory` 内部就是使用 ` JDK` 或者 `CGLib` 动态代理技术将增强应用到目标类的。

`Spring` 定义了`org.springframework.aop.framework.AopProxy`接口，并提供了两个包访问权限的实现类：

1. `JdkDynamicAopProxy`：使用 `JDK` 动态代理技术创建代理，如果通过 `ProxyFactory` 的`setInterfaces(Class[] interfaces)`指定目标接口进行代理，`ProxyFactory` 就使用 `JdkDynamicAopProxy`。
2. `CglibAopProxy`：使用 `CGLib` 动态代理技术创建代理；如果是通过类的代理则使用 `Cglib2AopProxy`；另外也可以通过`ProxyFactory` 的`setOptimize(true)`方法，让 `ProxyFactory` 启动优化代理模式，这样针对接口的代理也会使用`CglibAopProxy`。

`ProxyFactory` 可以增加多个增强，他们的调用顺序和添加顺序一致。

#### 3.2.2.以配置的方式实现增强

`ProxyFactoryBean` 是 `FactoryBean` 接口的实现类，负责为其他 `Bean` 创建代理实例，在方法内部使用 `ProxyFactory` 完成该功能，有如下参数：

- `target`：代理的目标对象；
- `proxyInterfaces`：代理索要实现的接口，可以是多个接口。
- `interfaces`：`proxyInterfaces` 的别名属性。
- `interceptorNames`：需要织入目标对象的 `Bean` 列表，必须是实现了 `MethodInterceptor` 或者 `Advisor` 的 `Bean`，配置的顺序对应调用顺序。`interceptorNames` 接受的是 `Bean` 的名称而非 `Bean` 的实例，多个可以用`,`分割。
- `singleton`：返回的代理是否单实例，默认为单实例。
- `optimize`：当设置为true的时候，强制使用CGLib代理。
- `proxyTargetClass`：是否对类进行代理（而不是针对接口进行代理），设置为 `true` 后表示使用 `CGLib` 代理，此时无须设置`proxyInterfaces` 属性，即使设置了也会忽略。

```xml
    <bean id="barberTony" class="com.ankeetc.spring.BarberTony"/>
    <bean id="barberTonyBeforeAdvice" class="com.ankeetc.spring.BarberTonyBeforeAdvice"/>
    
    <bean id="barberTonyProxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <property name="target" ref="barberTony"/>
        <property name="interceptorNames" value="barberTonyBeforeAdvice"/>
    </bean>
```

### 3.3.后置增强

通过实现 `AfterReturningAdvice` 来实现后置增强。假如在后置增强中抛出异常，如果该异常是目标方法声明的异常，则该异常 归并到目标方法中；如果不是目标方法所声明的异常，则 `spring` 将其转为运行期异常抛出。

```java
class BarberTonyAfterAdvice implements AfterReturningAdvice {
    /**
     * @param returnValue 目标类方法的返回值
     * @param method 目标类方法
     * @param args 目标类方法的入参
     * @param target 目标类实例
     */
    public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        System.out.println();
    }
}
```

### 3.4.环绕增强

`Spring` 直接使用 `AOP` 联盟定义的 `MethodInterceptor` 作为环绕增强的接口。该接口拥有唯一的接口方法`Object invoke(MethodInvocation invocation)throws Throwable`。 `MethodInvocation` 不但封装了目标方法及其入参数组，还封装了目标方法所在的实例对象，`MethodInvocation` 主要方法如下：

- `getMethod()`：获取目标类的调用方法。
- `getArguments()`：该方法可以获取目标方法的入参数组。
- `proceed()`：该方法可以反射调用目标实例相应的方法。

### 3.5.异常抛出增强

异常抛出增强最适合的应用场景是事务管理，当参与事务的某个 `DAO` 发生异常时，事务管理器就必须回滚事务。通过实现 `ThrowsAdvice` 实现异常抛出增强，`ThrowsAdvice` 是一个没有任何方法的标记接口，`Spring` 通过反射机制自行判断，必须采用以下形式定义异常的增强方法：

```java
// 入参必须是 Throwable 及其子类
public void afterThrowing(Exception ex) {}
// 前三个入参 Method method, Object[] args, Object target 要么都提供，要么都不提供
public void afterThrowing(Method method, Object[] args, Object target, Exception ex) {}
```

可以在同一个异常抛出增强中定义多个 `afterThrowing` 方法，抛出异常时 `Spring` 会自动选择匹配度最高的方法。

### 3.6.引介增强

引介增强为目标类创建新的方法和属性，所以引介增强的连接点是类级别的，而非方法级别的。通过引介增强，可以为目标类添加一个接口的实现，即目标类原来没有实现某个接口，引介增强后可以为目标类创建实现某接口的代理。

`Spring` 定义了引介增强接口 `IntroductionInterceptor`，该接口没有定义任何方法。一般通过扩展其实现类`DelegatingIntroductionInterceptor` 来定义自己的增强类。

```java
public class Main {
    public static void main(String[] args) {
        ProxyFactory proxyFactory = new ProxyFactory();
        // 必须添加引介增强需要实现的接口
        proxyFactory.addInterface(MonitorStatus.class);
        // 引介增强的目标类
        proxyFactory.setTarget(new Service());
        // 引介增强的类
        proxyFactory.addAdvice(new ServiceMonitor());
        // 由于引介增强一定要通过创建子类来生成代理，所以必须需要强制使用CGLib
        proxyFactory.setProxyTargetClass(true);

        Service serviceProxy = (Service) proxyFactory.getProxy();
        // 虽然Service并没有直接实现MonitorStatus接口，但是其代理类动态添加了该接口
        ((MonitorStatus) serviceProxy).setMonitorStatus(true);
        serviceProxy.doSomething();
    }
}

class ServiceMonitor extends DelegatingIntroductionInterceptor implements MonitorStatus {
    // 因为这个控制状态使代理类变成了非线程安全的实例，需要每一个线程有单独的状态
    ThreadLocal<Boolean> status = new ThreadLocal<Boolean>();

    // 覆盖率父类的invoke方法用于增强代码
    public Object invoke(MethodInvocation mi) throws Throwable {
        // 需要判断是否是需要增强的方法
        if ("doSomething".equals(mi.getMethod().getName()) && status.get() != null && status.get()) {
            System.out.println("method enhance");
        }
        // 直接调用super.invoke(mi)来实现调用原来方法
        return super.invoke(mi);
    }

    public void setMonitorStatus(boolean status) {
        this.status.set(status);
    }
}

// 为目标类引介增强的接口
interface MonitorStatus {
    void setMonitorStatus(boolean status);
}

// 需要增强的目标类
class Service {
    public void doSomething() {
        System.out.println("Service do something!");
    }
}
```

```xml
    <!-- 也可以使用xml定义 -->
    <bean id="service" class="com.ankeetc.spring.Service"/>
    <bean id="serviceMonitor" class="com.ankeetc.spring.ServiceMonitor"/>
    
    <bean id="serviceProxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <property name="target" ref="service"/>
        <property name="interfaces" value="com.ankeetc.spring.MonitorStatus"/>
        <property name="interceptorNames" value="serviceMonitor"/>
        <property name="proxyTargetClass" value="true"/>
    </bean>
```

**为什么需要ThreadLocal**

如果没有对 `ServiceMonitor` 进行线程安全的处理，就必须将 `singleton` 属性设置为 `false`，让 `ProxyFactoryBean` 产生 `prototype` 的作用域类型的代理。 这里就带来了一个严重的性能问题，因为 `CGLib` 动态创建代理的性能很低，而每次 `getBean` 方法从容器中获取作用域为 `prototype` 的Bean时都将返回一个新的代理实例，所以这种影响是巨大的，这就是为什么需要通过 `ThreadLocal` 对 `ServiceMonitor` 的开关进行线程安全化处理的原因。通过线程安全处理后，就可以使用默认的 `singleton` 作用域，这样创建代理的动作仅发生一次。

## 4.创建切面

我们希望有选择地织入目标类的某些特定方法中，就需要使用切点进行目标连接点的定位。`Spring` 通过`org.springframework.aop.Pointcut`接口描述切点，`Pointcut`由`ClassFilter`和`MethodMatcher`构成。`ClassFilter` 定位特定的类，`MethodMatcher` 定位特定的方法。

**静态方法匹配器与动态方法匹配器**

Spring支持两种方法匹配器——静态方法匹配器和动态方法匹配器：所谓静态方法匹配器，它仅对方法签名（包括方法名和入参类型、顺序）进行匹配； 静态匹配仅会判别一次。动态匹配器，会在运行期检查方法入参的值。动态匹配因为每次调用方法的入参可能都不一样，所以每次调用方法都会判断，因此动态匹配对性能的影响很大，一般情况下，动态匹配不常用。方法匹配器的类型由 `isRuntime()` 返回值决定，返回 `false` 表示静态方法匹配器，反之则是动态方法匹配器。

### 4.1.切点类型

1. 静态方法切点：`org.springframework.aop.support.StaticMethodMatcherPointcut`是静态方法切点的抽象基类，默认情况下匹配所有的类。最常用的两个子类`NameMatchMethodPointcut`和 `AbstractRegexpMethodPointcut`，前者提供简单字符串匹配方法签名，后者使用正则表达式匹配方法签名。
2. 动态方法切点：`org.springframework.aop.support.DynamicMethodMatcherPointcut`是动态方法切点的抽象基类，默认情况下匹配所有的类。
3. 注解切点：`org.springframework.aop.support.annotation.AnnotationMatchingPointcut`实现类表示注解切点。
4. 表达式切点：`org.springframework.aop.support.ExpressionPointcut`提供了对 `AspectJ` 切点表达式语法的支持。
5. 流程切点：`org.springframework.aop.support.ControlFlowPointcut`该切点是一个比较特殊的节点，它根据程序执行的堆栈信息查看目标方法是否由某一个方法直接或间接发起调用，一次来判断是否为匹配的链接点。
6. 复合切点：`org.springframework.aop.support.ComposablePointcut`该类是为实现创建多个切点而提供的操作类。

### 4.2.切面类型

切面类继承关系图：

![14623831-5ac26a72fd08bae9](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329222307.webp)

Spring使用`org.springframework.aop.Advisor`接口表示切面的概念，一个切面同时包含横切代码和连接点信息。

1. `Advisor`：代表一般切面，仅包含一个 `Advice`，因为 `Advice` 包含了横切代码和连接点信息，所以 `Advice` 本身一个简单的切面，只不过它代表的横切的连接点是所有目标类的所有方法，因为这个横切面太宽泛，所以一般不会直接使用。
2. `PointcutAdvisor`：代表具有切点的切面，包括 `Advice` 和 `Pointcut` 两个类，这样就可以通过类、方法名以及方位等信息灵活的定义切面的连接点，提供更具实用性的切面。`PointcutAdvisor`主要有6个具体的实现类：
3. `IntroductionAdvisor`：代表引介切面， 引介切面是对应引介增强的特殊的切面，它应用于类层上面，所以引介切点使用ClassFilter进行定义。

**PointcutAdvisor的实现类**

![14623831-c397aff64dfc5c2a](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329222352.webp)

- `DefaultPointcutAdvisor`：最常用的切面类型，它可以通过任意 `Pointcut` 和 `Advice` 定义一个切面，唯一不支持的就是引介的切面类型，一般可以通过扩展该类实现自定义的切面。
- `NameMatchMethodPointcutAdvisor`：通过该类可以定义按方法名定义切点的切面。
- `RegexpMethodPointcutAdvisor`：对于按正则表达式匹配方法名进行切点的切面，可以通过扩展该实现类进行操作。`RegexpMethodPointcutAdvisor` 允许用户以正则表达式模式串定义方法匹配的切点，其内部通过 `JdkRegexpMethodPointcut` 构造出正则表达式方法名切点。
- `StaticMethodMatcherPointcutAdvisor`：静态方法匹配器切点定义的切面，默认情况下匹配所有的目标类。
- `AspectJExpressionPointcutAdvisor`：用于AspectJ切点表达式定义切点的切面。
- `AspectJPointcutAdvisor`：用于AspectJ语法定义切点的切面。

> Advisor都实现了org.springframework.core.Ordered接口，Spring 将根据Advisor定义的顺序决定织入切面的顺序。

### 4.3.静态普通方法名匹配切面

1. 定义切面，继承 `StaticMethodMatcherPointcutAdvisor` 并实现其 `matches()` 方法
2. 定义增强，实现 `Advice` 或者其子类并实现相关方法
3. 为切面设置增强
4. 通过 `ProxyFactory` 生成代理类

```java
public class Main {
    public static void main(String[] args) throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        // 设置目标类
        proxyFactory.setTarget(new Waitress());

        // 在切面上添加增强
        WaitressAdvisor waitressAdvisor = new WaitressAdvisor();
        waitressAdvisor.setAdvice(new WaitressAdvice());

        // 增加切面
        proxyFactory.addAdvisor(waitressAdvisor);

        Waitress waitress = (Waitress)proxyFactory.getProxy();
        waitress.sayHello("zzx");
        waitress.order("cola");
    }
}

/**
 * 目标类
 */
class Waitress {
    public void sayHello(String name) {
        System.out.println("hello " + name + "!");
    }

    public void order(String food) {
        System.out.println("order " + food + "!");
    }
}

/**
 * 定义切面
 * StaticMethodMatcherPointcutAdvisor唯一需要定义的是matches()方法
 * 默认匹配所有类，可以通过getClassFilter()方法让它仅匹配指定类
 */
class WaitressAdvisor extends StaticMethodMatcherPointcutAdvisor {
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return "sayHello".equals(method.getName());
    }
}

/**
 * 定义增强，此处定义了一个方法前置增强
 */
class WaitressAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("Morning " + args[0] + "!");
    }
}
```

**通过XML的配置**

可以通过 `ProxyFactoryBean` 类来生成代理类。

```xml
    <bean id="waitressAdvisor" class="com.ankeetc.spring.WaitressAdvisor">
        <!-- 将advice增强装配到advisor切面中 -->
        <!-- 还可以配置classFilter类匹配过滤器和配置order切面织入时的顺序 -->
        <property name="advice" >
            <bean class="com.ankeetc.spring.WaitressAdvice"/>
        </property>
    </bean>

    <bean id="proxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <property name="target">
            <bean class="com.ankeetc.spring.Waitress"/>
        </property>
        <property name="interceptorNames" value="waitressAdvisor"/>
        <property name="proxyTargetClass" value="true"/>
    </bean>
```

### 4.4.静态增则表达式方法匹配切面

在 `StaticMethodMatcherPointcutAdvisor` 中，仅能通过方法名定义切点，这种方式不够灵活。`RegexpMethodPointcutAdvisor` 是正则表达式方法匹配的切面实现类，一般不需要扩展。

```xml
    <bean id="advisor" class="org.springframework.aop.support.RegexpMethodPointcutAdvisor">
        <property name="advice" >
            <bean class="com.ankeetc.spring.WaitressAdvice"/>
        </property>
        <!-- 配置正则表达式的列表 -->
        <property name="patterns">
            <list>
                <value>.*sayHello.*</value>
            </list>
        </property>
    </bean>

    <bean id="proxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <property name="target">
            <bean class="com.ankeetc.spring.Waitress"/>
        </property>
        <property name="interceptorNames" value="advisor"/>
        <property name="proxyTargetClass" value="true"/>
    </bean>
```

### 4.5.动态切面

可以使用 `DefaultPointcutAdvisor` 和 `DynamicMethodMatcherPointcut` 来创建动态切面。`DynamicMethodMatcherPointcut` 是一个抽象类，通过将 `isRuntime` 标识为 `final` 且返回为 `true`，是的其子类就一定是一个动态切点。

```java
public class Main {
    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        Waitress waitress = context.getBean("proxy", Waitress.class);

        waitress.sayHello("zzx");
        waitress.sayHello("zzx");

        waitress.sayGoodbye("lucas");
        waitress.sayGoodbye("lucas");
    }
}

/**
 * 目标类
 */
class Waitress {
    public void sayHello(String name) {
        System.out.println("hello " + name + "!");
    }

    public void sayGoodbye(String name) {
        System.out.println("goodbye " + name + "!");
    }
}

class WaitressPointcut extends DynamicMethodMatcherPointcut {
    // 对方法进行静态检查
    public boolean matches(Method method, Class<?> targetClass) {
        System.out.println("静态检查: class=" + targetClass.getSimpleName() + ", method=" +  method.getName());
        return "sayHello".equals(method.getName());
    }

    // 对方法进行动态检查
    public boolean matches(Method method, Class<?> targetClass, Object... args) {
        System.out.println("动态检查: class=" + targetClass.getSimpleName() + ", method=" +  method.getName());
        return true;
    }
}

/**
 * 定义增强，此处定义了一个方法前置增强
 */
class WaitressAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("Morning " + args[0] + "!");
    }
}
```

```yaml
# 织入切面前Spring对目标类中所有方法进行静态切点检查
静态检查: class=Waitress, method=sayHello
静态检查: class=Waitress, method=sayGoodbye
静态检查: class=Waitress, method=toString
静态检查: class=Waitress, method=clone

# 调用waitress.sayHello("zzx")进行静态和动态检查
静态检查: class=Waitress, method=sayHello
动态检查: class=Waitress, method=sayHello
Morning zzx!
hello zzx!

# 第二次调用waitress.sayHello("zzx")只进行动态检查
动态检查: class=Waitress, method=sayHello
Morning zzx!
hello zzx!

# 调用waitress.sayGoodbye("lucas");进行静态检查，检查false不进行动态检查
静态检查: class=Waitress, method=sayGoodbye
goodbye lucas!

# 第二次调用waitress.sayGoodbye("lucas");不进行检查
goodbye lucas!
```

- Spring在创建代理织入切面时，对目标类中的所有方法进行静态切点检查
- 在生成织入切面的代理对象后，第一次调用任何方法都会对该方法进行静态切点检查
- 如果仅通过静态切点检查就可以知道连接点是**不匹配的**，则运行时不再进行动态检查；否则在运行时每次都进行动态检查。
- 动态切点检查会对性能造成很大的影响，所以在动态切点类中定义静态切点检查方法可以避免不必要的动态检查。

### 4.6.流程切面

`Spring` 的流程切面由 `DefaultPointcutAdvisor` 和 `ControlFlowPointcut` 实现。流程切点代表某个方法**直接或间接发起调用的其他方法**。

```java
class Waitress {
    public void sayHello(String name) {
        System.out.println("hello " + name + "!");
    }

    public void sayGoodbye(String name) {
        System.out.println("goodbye " + name + "!");
    }
}

class SuperWaitress {
    Waitress waitress;

    public SuperWaitress(Waitress waitress) {
        this.waitress = waitress;
    }

    // waitress的方法通过该方法调用，对该方法的调用的其他所有方法都织入增强
    public void say(String name) {
        waitress.sayHello(name);
        waitress.sayGoodbye(name);
    }
}
```

#### XML配置

`ControlFlowPointcut` 切点有两个构造方法：

1. `ControlFlowPointcut(Class<?> clazz)` 指定一个类作为流程切点
2. `ControlFlowPointcut(Class<?> clazz, String methodName)` 指定一个类和一个方法作为流程切点。

```xml
    <bean id="advisor" class="org.springframework.aop.support.DefaultPointcutAdvisor">
        <!-- 配置增强类 -->
        <property name="advice" >
            <bean class="com.ankeetc.spring.WaitressAdvice"/>
        </property>
        <!--配置ControlFlowPointcut切点，表示通过SuperWaitress#say()方法直接或间接发起的调用匹配切点-->
        <property name="pointcut">
            <bean class="org.springframework.aop.support.ControlFlowPointcut">
                <constructor-arg type="java.lang.Class" value="com.ankeetc.spring.SuperWaitress"/>
                <constructor-arg type="java.lang.String" value="say"/>
            </bean>
        </property>
    </bean>

    <bean id="proxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <property name="target">
            <bean class="com.ankeetc.spring.Waitress"/>
        </property>
        <property name="interceptorNames" value="advisor"/>
        <property name="proxyTargetClass" value="true"/>
    </bean>
```

```java
public class Main {
    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        Waitress waitress = context.getBean("proxy", Waitress.class);

        waitress.sayHello("zzx");

        SuperWaitress superWaitress = new SuperWaitress(waitress);
        superWaitress.say("lucas");
    }
}
```

```prop
# 通过waitress.sayHello("zzx");没有增强效果
hello zzx!

# 通过superWaitress.say("lucas");两个方法都织入了增强效果。
Advice lucas!
hello lucas!
Advice lucas!
goodbye lucas!
```

**流程切面对性能影响**

流程切面和动态切面从某种程度上说可以算是一类切面，因为二者都需要在运行期判断动态环境。对于流程切面来说，代理对象在每次调用目标类方法时，都需要判断方法调用堆栈中是否有满足流程切点要求的方法。因此，和动态切面一样，流程切面对性能的影响也很大。

### 4.7.复合切点切面

有的时候，一个切点可能难以描述目标连接点的信息。如4.6中如果我们希望由 `SuperWaitress.say()` 方法发起调用并且被调用的方法是 `waitress.sayHello()` 时才织入增强，那么这个切点就是复合切点，因为它有两个单独的切点共同确定。
 当然，我们可以只通过一个切点来描述同时满足上述两个匹配条件的连接点，但是更好的方式是使用 `Spring` 提供的`ComposalbePointcut` 把两个切点组合起来，通过切点的符合运行算表示。 `ComposalbePointcut` 可以将多个切点以并集或者交集的方式组合起来，提供切点之间复合运算的功能。

#### 4.7.1.ComposablePointcut构造函数

- `public ComposablePointcut()`：构造一个匹配所有类所有方法的复合切点
- `public ComposablePointcut(Pointcut pointcut)`：构造出一个匹配特定切点的复合切点
- `public ComposablePointcut(ClassFilter classFilter)`：构造一个匹配特定类所有方法的复合切点
- `public ComposablePointcut(MethodMatcher methodMatcher)`：构造出一个匹配所有类特定方法的复合切点
- `public ComposablePointcut(ClassFilter classFilter, MethodMatcher methodMatcher)`：构造出一个匹配特定类特定方法的复合切点

#### 4.7.2.ComposablePointcut交集运算的方法

- `public ComposablePointcut intersection(ClassFilter other)`：将复合切点和一个ClassFilter对象进行交集运算，得到一个结果复合切点
- `public ComposablePointcut intersection(MethodMatcher other)`：将复合切点和一个MethodMatcher对象进行交集运算，得到一个结果复合切点
- `public ComposablePointcut intersection(Pointcut other)`：将复合切点和一个切点对象进行交集运算，得到一个结果复合切点

#### 4.7.3.ComposablePointcut并集运算的方法

- `public ComposablePointcut union(ClassFilter other)`：将复合切点和一个ClassFilter对象进行并集运算，得到一个结果复合切点
- `public ComposablePointcut union(MethodMatcher other)`：将复合切点和一个MethodMatcher对象进行并集运算，得到一个结果复合切点
- `public ComposablePointcut union(Pointcut other)`：将复合切点和一个切点对象进行并集运算，得到一个结果复合切点

#### 4.7.4.多个切点之间的交集并集运算

`ComposablePointcut` 没有提供直接对两个切点机型并集交集的运算的方法，如果需要对连个切点进行叫交集并集运算，可以使用`Spring` 提供的 `org.springframework.aop.support.Pointcuts`工具类。

- `public static Pointcut union(Pointcut pc1, Pointcut pc2)`：对两个切点进行交集运算，返回一个结果切点，该切点即`ComposablePointcut` 对象的实例
- `public static Pointcut intersection(Pointcut pc1, Pointcut pc2)`：对两个切点进行并集运算，返回一个结果切点，该切点即 `ComposablePointcut` 对象的实例

#### 4.7.5.实例

```xml
    <bean id="waitressComposable" class="com.ankeetc.spring.WaitressComposable"/>

    <bean id="advisor" class="org.springframework.aop.support.DefaultPointcutAdvisor">
        <property name="advice" >
            <bean class="com.ankeetc.spring.WaitressAdvice"/>
        </property>
        <property name="pointcut" value="#{waitressComposable.pointcut}"/>
    </bean>

    <bean id="proxy" class="org.springframework.aop.framework.ProxyFactoryBean">
        <property name="target">
            <bean class="com.ankeetc.spring.Waitress"/>
        </property>
        <property name="interceptorNames" value="advisor"/>
        <property name="proxyTargetClass" value="true"/>
    </bean>
```

```java
public class Main {
    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        Waitress waitress = context.getBean("proxy", Waitress.class);

        waitress.sayHello("zzx");

        SuperWaitress superWaitress = new SuperWaitress(waitress);
        superWaitress.say("lucas");
    }
}

class WaitressComposable {
    public Pointcut getPointcut() {
        ComposablePointcut cp = new ComposablePointcut();

        // 创建一个流程切点
        Pointcut pt1 = new ControlFlowPointcut(SuperWaitress.class, "say");
        // 创建一个方法名切点
        Pointcut pt2 = new NameMatchMethodPointcut();
        ((NameMatchMethodPointcut) pt2).addMethodName("sayHello");

        // 两个切点取交集
        return cp.intersection(pt1).intersection(pt2);
    }
}
```

```prop
# waitress.sayHello("zzx"); 未增强
hello zzx!

# superWaitress.say("lucas");只增强了sayHello()方法
Advice lucas!
hello lucas!
goodbye lucas!
```

### 4.8.引介切面

引介切面是引介增强的封装器，通过引介切面可以很容易的为现有对象添加任何接口的实现。`IntroductionAdvisor` 和 `PointcutAdvisor` 不同，`IntroductionAdvisor` 仅有一个类过滤器 `ClassFilter` 而没有 `MethodMatcher`，因为引介切面是类级别的，而 `Poincut` 的切点是方法级别的。

引介切面类图：

![14623831-194221ae1c1b497b](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329222617.webp)

#### 4.8.1.IntroductionAdvisor接口的两个实现类

- `DefaultIntroductionAdvisor`，引介切面最常用的实现类
- `DeclareParentsAdvisor`， 用于实现使用 `AspectJ` 语言的 `DeclareParent` 注解表示的引介切面。

#### 4.8.2.DefaultIntroductionAdvisor的构造函数

- `public DefaultIntroductionAdvisor(Advice advice)`：通过一个增强创建的引介切面，引介切面将为目标对象增强对象中所有接口的实现
- `public DefaultIntroductionAdvisor(DynamicIntroductionAdvice advice,
   Class<?> intf) `：通过一个增强和一个指定的接口类创建引介切面，仅为目标对象新增class接口的实现
- `public DefaultIntroductionAdvisor(Advice advice, IntroductionInfo
   introductionInfo) `：通过一个增强和一个 `IntroductionInfo` 创建引介切面，目标对象小实现哪些接口由 `introduction` 对象的 `getInterfaces()` 方法标识

## 5.自动创建代理

使用 `ProxyFactoryBean` 创建代理比较麻烦，`Spring` 通过 `BeanPostProcessor` 提供了自动代理机制，让容器自动生成代理。

### 5.1.实现类介绍

这些基于`BeanPostProcessor` 的自动代理创建器的实现类，将根据一些规则自动在容器实例化 `Bean` 时为匹配的 `Bean` 生成代理实例。

1. 基于Bean配置名规则的自动代理创建器：允许为一组特定配置名的Bean自动创建代理实例的代理创建器，实现类为`BeanNameAutoProxyCreator`；
2. 基于 `Advisor` 匹配机制的自动代理创建器：它会对容器中所有的 `Advisor` 进行扫描，自动将这些切面应用到匹配的Bean中（即为目标Bean创建代理实例），实现类为 `DefaultAdvisorAutoProxyCreator`；
3. 基于 `Bean` 中 `AspjectJ` 注解标签的自动代理创建器：为包含 `AspectJ` 注解的 `Bean` 自动创建代理实例，它的实现类是`AnnotationAwareAspectJAutoProxyCreator`，该类是 `Spring 2.0` 的新增类。

自动代理创建器类图：

![14623831-7cbcb44e7c3ab5ec](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329222723.webp)

### 5.2 BeanNameAutoProxyCreator

```xml
    <bean id="waitress" class="com.ankeetc.spring.Waitress"/>
    <bean id="advice" class="com.ankeetc.spring.WaitressAdvice"/>

    <bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator">
       <!-- 可以通过list子元素设定多个Bean名称，或这通过逗号、空格、分号设定多个 -->
        <property name="beanNames" value="waitress"/>
        <property name="interceptorNames" value="advice"/>
        <property name="optimize" value="true"/>
    </bean>
```

```java
public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        Waitress waitress = context.getBean("waitress", Waitress.class);
        waitress.sayHello("zzx");
    }
}

class Waitress {
    public void sayHello(String name) {
        System.out.println("hello " + name + "!");
    }

    public void sayGoodbye(String name) {
        System.out.println("goodbye " + name + "!");
    }
}

class WaitressAdvice implements MethodBeforeAdvice {
    @Override
    public void before(Method method, Object[] args, Object target) throws Throwable {
        System.out.println("Morning " + args[0] + "!");
    }
}
```

- `beanNames` 属性：`BeanNameAutoProxyCreator` 有一个 `beanNames` 属性，它允许用户指定一组需要自动代理的 `Bean` 名称，`Bean` 名称可以使用 `*` 通配符。
- `FactoryBean` 的 `Bean` ：一般不会为 `FactoryBean` 的 `Bean` 创建代理，如果刚好有这样一个需求，这需要在 `beanNames` 中指定添加 `$` 的 `Bean` 名称，如 `<property name="beanNames" value="$waitress">`
- `interceptorNames` 属性：`BeanNameAutoProxyCreator` 的 `interceptorNames` 属性指定一个或者多个 `Bean` 的名称。
- `optimize` 属性：如果将此属性设置为 `true`，则将强制使用 `CGLib` 动态代理技术。

### 5.3.DefaultAdvisorAutoProxyCreator

`Advisor` 是切点和增强的复合体，`Advisor` 本身已经包含了足够的信息：横切逻辑（要织入什么）以及连接点（织入到哪里）。`DefaultAdvisorAutoProxyCreator` 能够扫描容器中的 `Advisor`，并将 `Advisor` 自动织入到匹配的目标 `Bean` 中，即为匹配的目标`Bean` 自动创建代理。

```xml
    <bean id="waitress" class="com.ankeetc.spring.Waitress"/>

    <bean id="waitressAdvisor" class="org.springframework.aop.support.NameMatchMethodPointcutAdvisor">
        <property name="mappedNames" value="sayHello"/>
        <property name="advice" >
            <bean class="com.ankeetc.spring.WaitressAdvice"/>
        </property>
    </bean>

    <!-- 定义一个DefaultAdvisorAutoProxyCreator，它会将容器中的Advisor织入匹配的目标Bean中 -->
    <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator"/>
```

### 5.4.细节

1. 基于 `JDK` 动态代理，通过接口来实现方法拦截，所以必须要确保要拦截的目标方法在接口中有定义，否则将无法实现拦截
2. `GCLib` 动态代理，通过动态生成子类来实现方法拦截，必须确保要拦截的目标方法可被子类访问，即目标方法必须定义为非 `final`。且非私有实例方法。
3. 在方法内部之间调用的时候，不会使用被增强的代理类，而是直接使用未被增强原类的方法。想解决这个问题，就是在内部方法调用时，让其通过代理类调用其内部方法，即需要让原来的 `Waiter` 实现一个可注入自身代理类的接口 `BeanSelfProxyAware`。

