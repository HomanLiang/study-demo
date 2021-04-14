[toc]



# Spring AOP - 基于AspectJ的AOP

## 1.Spring对AOP的支持

Spring对AOP功能进行了很重要的增强：

1. 新增了基于Schema的配置支持，为AOP专门提供了aop命名空间
2. 新增了对AspectJ切点表达式语言的支持。`@AspectJ` 允许开发者在POJO中定义切面。Spring使用和 `@AspectJ` 相同风格的注解，并通过AspectJ提供的注解库和解析库处理切点。由于Spring只支持方法级的切点，仅对 `@AspectJ` 提供了有限的支持
3. 可以无缝地集成AspectJ

## 2.使用@Aspect

### 2.1.Maven依赖

1. Spring在处理@Aspect注解表达式时，需要将Spring的asm模块添加到类路径中。asm是轻量级的字节码处理框架，因为Java的反射机制无法获取入参名，Spring就利用asm处理@Aspect中所描述的方法入参名。
2. Spring采用AspectJ提供的@Aspect注解类库及相应的解析类库，需要在pom.xml文件中添加aspectj.weaver和aspectj.tools类包的依赖。

### 2.2 使用编码的方式

```java
public class Main {
    public static void main(String[] args) {
        AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory();
        // 添加要代理的增强类
        aspectJProxyFactory.setTarget(new Waitress());
        // 添加增强类
        aspectJProxyFactory.addAspect(WaitressPreAspect.class);

        Waitress waitress = aspectJProxyFactory.getProxy();
        waitress.sayHello("zzx");
    }
}

public class Waitress {
    public void sayHello(String name) {
        System.out.println("hello " + name + "!");
    }
    public void sayGoodbye(String name) {
        System.out.println("goodbye " + name + "!");
    }
}


@Aspect
public class WaitressPreAspect {
    @Before("execution(* sayHello(..))")
    public void beforeSayHello() {
        System.out.println("Advice sayHello");
    }
}
```

1. 在WaitressPreAspect类定义处，标注了一个@Aspect注解，第三方处理程序就可以通过类是否拥有@Aspect注解判断其是否是一个切面。
2. 在beforeSayHello()方法标签处，标注了@Before注解，并为该注解提供了成员值`execution(* sayHello(..))`，此注解提供了两个信息：@Before注解表示该增强是前置增强；而成员值通过@Apsect切点表达式语法定义切点。
3. beforeSayHello()方法是增强的横切逻辑，该横切逻辑在目标方法前调用。

WaitressPreAspect类通过注解和代码，将切点、增强类型和增强的横切逻辑揉合到一个类中，使切面的定义浑然天成。如果在低版本Spring AOP中，你必须同时创建增强类，切点类以及切面类，并使三者联合表达相同的信息。

> 添加@Aspect类的类必须是public的，否则会抛出异常`Caused by: java.lang.IllegalAccessException: Class org.springframework.aop.aspectj.annotation.AspectJProxyFactory can not access a member of class com.ankeetc.spring.WaitressPreAspect with modifiers ""`。

### 2.3.使用注解的方式

```java
public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext("com.ankeetc.spring");
        context.getBean(Waitress.class).sayHello("zzx");
    }
}

@Configuration
public class Config {
    @Bean
    public AnnotationAwareAspectJAutoProxyCreator annotationAwareAspectJAutoProxyCreator() {
        return new AnnotationAwareAspectJAutoProxyCreator();
    }
}

@Component
public class Waitress {
    public void sayHello(String name) {
        System.out.println("hello " + name + "!");
    }
    public void sayGoodbye(String name) {
        System.out.println("goodbye " + name + "!");
    }
}

@Aspect
@Component
 class WaitressPreAspect {
    @Before("execution(* sayHello(..))")
    public void beforeSayHello() {
        System.out.println("Advice sayHello");
    }
}
```

### 2.4.通过XML配置

```xml
<bean class="com.ankeetc.spring.Waitress"/>
<bean class="com.ankeetc.spring.WaitressPreAspect"/>
<!--自动为Spring容器中那些匹配@Aspect切面的Bean创建代理，完成切面织入-->
<bean class="org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator"/>
```

或者通过基于Schema的aop命名空间。`<aop:aspectj-autoproxy/>`有一个proxy-target-class属性，默认为false，表示使用JDK动态代理织入增强；当配置为true时，表示使用CGLib动态代理技术织入增强。不过即使proxy-target-class设置为false，如果目标类没有声明接口，则Spring将自动使用CGLib动态代理。

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-4.0.xsd
       http://www.springframework.org/schema/aop
       http://www.springframework.org/schema/aop/spring-aop-4.0.xsd">

    <bean class="com.ankeetc.spring.Waitress"/>
    <bean class="com.ankeetc.spring.WaitressPreAspect"/>
    <aop:aspectj-autoproxy/>
</beans>
```

## 3.语法基础

AspectJ使用JDK 5.0注解和正规的AspectJ 5的切点表达式语言描述切面，由于Spring只支持方法的连接点，所以Spring仅支持部分AspectJ的切点语言

### 3.1.切点表达式函数

AspectJ 5的切点表达式由关键字和操作参数组成，如`execution(* func(..))`的切点表达式，`execute`为关键字（函数），而`* func(..)`为操作参数（入参），两者联合起来所表示的切点匹配目标类greetTo()方法的连接点。Spring支持9个切点表达式函数，它们用不同的方式描述目标类的连接点，根据描述对象的不同，可以将它们大致分为4种类型：

- 方法切点函数：通过描述目标类方法信息定义连接点；
- 方法入参切点函数：通过描述目标类方法入参的信息定义连接点；
- 目标类切点函数：通过描述目标类类型信息定义连接点；
- 代理类切点函数：通过描述目标类的代理类的信息定义连接点；

#### 3.1.1.方法切点函数

| 函数          | 入参           | 说明                                                         |
| ------------- | -------------- | ------------------------------------------------------------ |
| execution()   | 方法匹配模式串 | 表示满足某一匹配模式的所有目标类方法连接点。如`execution(* greetTo(..))`表示所有目标类中的greetTo()方法。 |
| @annotation() | 方法注解类名   | 表示标注了特定注解的目标方法连接点。如`@annotation(com.ankeetc.anno.NeedTest)`表示任何标注了@NeedTest注解的目标类方法。 |

#### 3.1.2.方法入参切点函数

| 函数    | 入参         | 说明                                                         |
| ------- | ------------ | ------------------------------------------------------------ |
| args()  | 类名         | 通过判别目标类方法运行时入参对象的类型定义指定连接点。如`args(com.ankeetc.Waiter)`表示所有有且仅有一个按类型匹配于Waiter的入参的方法。 |
| @args() | 类型注解类名 | 通过判别目标方法的运行时入参对象的类是否标注特定注解来指定连接点。如`@args(com.ankeetc.Monitorable)`表示任何这样的一个目标方法：它有一个入参且入参对象的类标注@Monitorable注解。 |

### 3.1.3.目标类切点函数

| 函数      | 入参         | 说明                                                         |
| --------- | ------------ | ------------------------------------------------------------ |
| within()  | 类名匹配串   | 表示特定域下的所有连接点。如`within(com.ankeetc.service.*)`表示`com.ankeetc.service`包中的所有连接点，也即包中所有类的所有方法，而`within(com.ankeetc.service.*Service)`表示在`com.ankeetc.service`包中，所有以Service结尾的类的所有连接点。 |
| target()  | 类名         | 假如目标类按类型匹配于指定类，则目标类的所有连接点匹配这个切点。如通过`target(com.ankeetc.Waiter)`定义的切点，Waiter、以及Waiter实现类NaiveWaiter中所有连接点都匹配该切点。 |
| @within() | 类型注解类名 | 假如目标类按类型匹配于某个类A，且类A标注了特定注解，则目标类的所有连接点匹配这个切点。如`@within(com.ankeetc.Monitorable)`定义的切点，假如Waiter类标注了@Monitorable注解，则Waiter以及Waiter实现类NaiveWaiter类的所有连接点都匹配。 |
| @target() | 类型注解类名 | 目标类标注了特定注解，则目标类所有连接点匹配该切点。如`@target(com.ankeetc.Monitorable)`，假如NaiveWaiter标注了@Monitorable，则NaiveWaiter所有连接点匹配切点。 |

#### 3.1.4.代理类切点函数

| 函数   | 入参 | 说明                                                         |
| ------ | ---- | ------------------------------------------------------------ |
| this() | 类名 | 代理类按类型匹配于指定类，则被代理的目标类所有连接点匹配切点。这个函数比较难理解，这里暂不举例，留待后面详解。 |

#### 3.1.5.不支持的函数

AspectJ除上表中所列的函数外，还有call()、initialization()、 preinitialization()、 staticinitialization()、 get()、 set()、handler()、 adviceexecution()、 withincode()、 cflow()、 cflowbelow()、 if()、 @this()以及@withincode()等函数，这些函数在Spring中不能使用，否则会抛出IllegalArgumentException异常。

### 3.2.函数入参通配符

#### 3.2.1.三种通配符

有些函数的入参可以接受通配符，AsppectJ支持三种通配符：

- `*`：匹配任意字符，但它只能匹配上下文中的一个元素；
- `..`：匹配任意字符，可以匹配上下文中的多个元素，但在表示类时，必须和*联合使用，而在表示入参时则单独使用；
- `+`：表示按类型匹配指定类的所有类，仅能跟在类名后面。

#### 3.2.2.函数及其支持的通配符

- 支持所有通配符：`execution()`、`within()`，如`within(com.ankeetc.*)`、`within(com.ankeetc.service..*.*Service+)`等；
- 仅支持+通配符：args()、this()、target()，如`args(com.ankeetc.Waiter+)`、 `target(java.util.List+)`等。虽然这三个函数可以支持+通配符，但其意义不大，因为对于这些函数来说使用和不使用+都是一样的，如`target(com.ankeetc.Waiter+)`和`target(com.ankeetc.aspectj.Waiter)`是等价的。
- 不支持通配符：@args()、@within()、@target()和@annotation()，如`@args(com.ankeetc.anno.NeedTest)`，`@within(com.ankeetc.anno.NeedTest)`。

此外，args()、this()、target()、@args()、@within()、@target()和@annotation()这7个函数除了可以指定类名外，也可以指定变量名，并将目标对象中变量绑定到增强的方法中。

### 3.3.逻辑运算符

切点函数之间还可以进行逻辑运算组成复合切点，Spring支持以下的切点运算符：

- `&&`与操作符：相当于切点的交集运算（and是等效的操作符）。如`within(com.ankeetc..*) && args(String)`表示在`com.ankeetc`包下所有类（当前包以及子孙包）拥有一个String入参的方法。
- `||`或操作符：相当于切点的并集运算（or是等效的操作符）。如`within(com.ankeetc..*) || args(String)`表示在`com.ankeetc`包下所有类的方法，或者所有拥有一个String入参的方法。
- `!`非操作符：相当于切点的反集运算（not是等效的操作符）。如`!within(com.ankeetc.*)`表示所有不在`com.ankeetc`包下的方法。

标准的AspectJ中并不提供and、or和not操作符，它们是Spring为了在XML配置文件中定义切点表达式而特意添加的等价操作符。在Spring中使用and、or和not时，允许不在前后添加空格， 如：`within(com.ankeetc..*)andnotargs(String)`和`within(com.ankeetc..*) and not args(String)`拥有相同的效果。虽然Spring接受这种表示方式，但为了保证程序的可读性，最好还是采用传统习惯，在操作符的前后添加空格。

> 如果not位于切点表达式的开头，则必须在开头添加一个空格，否则将产生解析错误。如`not within(com.ankeetc..*)`将产生解析错误，这应该是Spring解析的一个Bug，在表达式开头添加空格后则可以通过解析：`" not within(com.ankeetc..*)`。

### 3.4.不同增强类型

在低版本的Spring AOP中，你可以通过实现不同的增强接口定义各种类型的增强类。AspectJ也为各种类型的增强提供了不同的注解类，它们位于org.aspectj.lang.annotation.*包中。这些注解的存留期限都是RetentionPolicy.RUNTIME，标注目标都是ElementType.METHOD。

#### 3.4.1.@Before

前置增强，相当于BeforeAdvice的功能，Before注解类拥有2个成员：

- value：该成员用于定义切点；
- argNames：由于无法通过Java反射机制获取方法入参名，所以如果在Java编译时未启用调试信息或者需要在运行期解析切点，就必须通过这个成员指定注解所标注增强方法的参数名（注意两者名字必须完全相同），多个参数名用逗号分隔。

#### 3.4.2.@AfterReturning

后置增强，相当于AfterReturningAdvice，AfterReturning注解类拥有4个成员：

- value：该成员用于定义切点；
- pointcut：表示切点的信息，如果显式指定pointcut值，它将覆盖value的设置值，可以将pointcut成员看成是value的同义词；
- returning：将目标对象方法的返回值绑定给增强的方法；
- argNames：如前所述。

#### 3.4.3.@Around

环绕增强，相当于MethodInterceptor，Around注解类拥有2个成员：

- value：该成员用于定义切点；
- argNames：如前所述。

#### 3.4.4.@AfterThrowing

抛出增强，相当于ThrowsAdvice，AfterThrowing注解类拥有4个成员：

- value：该成员用于定义切点；
- pointcut：表示切点的信息，如果显式指定pointcut值，它将覆盖value的设置值，可以将pointcut成员看成是value的同义词；
- throwing：将抛出的异常绑定到增强方法中；
- argNames：如前所述。

#### 3.4.5.@After

Final 增强，不管是抛出异常或者是正常退出，该增强都会得到执行，该增强没有对应的增强接口，可以把它看成ThrowsAdvice和 AfterReturningAdvice的混合物，一般用于释放资源，相当于try{}finally{}的控制流。After注解类拥有2个成员：

- value：该成员用于定义切点；
- argNames：如前所述。

#### 3.4.6.@DeclareParents

引介增强，相当于IntroductionInterceptor，DeclareParents注解类拥有2个成员：

- value：该成员用于定义切点，它表示在哪个目标类上添加引介增强；
- defaultImpl：默认的接口实现类。

```java
interface Coach {
    void changePlayers(String player1, String player2);
}

public class DefaultCoach implements Coach {
    public void changePlayers(String player1, String player2) {
        System.out.println(player1 + " on ," + player2 + " off!");
    }
}

interface Player {
}

class LeBronJames implements Player {
}

// 我们通过Aspect为LeBronJames添加上教练接口
@Aspect
public class PlayerAspect {
    @DeclareParents(value = "com.ankeetc.spring.Player", defaultImpl = DefaultCoach.class)
    public Coach coach;
}
```

```xml
    <aop:aspectj-autoproxy/>
    <bean class="com.ankeetc.spring.DefaultCoach"/>
    <bean class="com.ankeetc.spring.Player"/>
```

## 4.切点函数详解

类继承关系图：

![14623831-2ba415b2d29aa410](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329225613.webp)

### 4.1.@annotation()

@annotation表示标注了某个注解的所有方法。

```java
public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        context.getBean(Player.class).run();
        // Player is running!
        // Player runs faster!
    }
}

class Player {
    @MyAnno
    public void run() {
        System.out.println("Player is running!");
    }
}

// 为所有标注了该注解的方法进行后置增强
@Aspect
class RunAdvice {
    @AfterReturning("@annotation(com.ankeetc.spring.MyAnno)")
    public void runFaster() {
        System.out.println("Player runs faster!");
    }
}

// 自定义注解
@interface MyAnno {}
```

```xml
<aop:aspectj-autoproxy/>
<bean class="com.ankeetc.spring.Player"/>
<bean class="com.ankeetc.spring.RunAdvice"/>
```

### 4.2.execution()

execution()是最常用的切点函数，除了返回类型模式、方法名模式和参数模式外，其它项都是可选的，其语法如下所示：`execution(<修饰符模式>? <返回类型模式> <方法名模式>(<参数模式>) <异常模式>?)`

#### 4.2.1.通过方法签名定义切点：

1. `execution(public * *(..))`：匹配所有目标类的public方法。第一个`*`代表返回类型，第二个`*`代表方法名，而`..`代表任意入参的方法；
2. `execution(* *To(..))`：匹配目标类所有以To为后缀的方法。第一个`*`代表返回类型，而`*To`代表任意以To为后缀的方法；

#### 4.2.2.通过类定义切点

1. `execution(* com.ankeetc.Waiter.*(..))`：匹配Waiter接口的所有方法。第一个`*`代表返回任意类型，`com.ankeetc.Waiter.*`代表Waiter接口中的所有方法；
2. `execution(* com.ankeetc.Waiter+.*(..))`：匹配Waiter接口及其所有实现类的方法，它不但匹配在Waiter接口定义的方法，同时还匹配不在Waiter接口中定义的方法。

#### 4.2.3.通过类包定义切点

在类名模式串中，`.*`表示包下的所有类，而`..*`表示包、子孙包下的所有类。

1. `execution(* com.ankeetc.*(..))`：匹配com.ankeetc包下所有类的所有方法；
2. `execution(* com.ankeetc..*(..))`：匹 配com.ankeetc包、子孙包下所有类的所有方法，如`com.ankeetc.dao`，`com.ankeetc.server`以及`com.ankeetc.dao.user`包下的所有类的所有方法都匹配。`..`出现在类名中时，后面必须跟`*`，表示包、子孙包下的所有类；
3. `execution(* com..*.*Dao.find*(..))`：匹配包名前缀为com的任何包下类名后缀为Dao的方法，方法名必须以find为前缀。如`com.ankeetc.UserDao#findByUserId()`、`com.ankeetc.dao.ForumDao#findById()`的方法都匹配切点。

#### 4.2.4.通过方法入参定义切点

切点表达式中方法入参部分比较复杂，可以使用`*`和`..`通配符，其中`*`表示任意类型的参数，而`..`表示任意类型参数且参数个数不限。

1. `execution(* joke(String,int)))`：匹配joke(String,int)方法，且joke()方法的第一个入参是String，第二个入参是int。如果方法中的入参类型是java.lang包下的类，可以直接使用类名，否则必须使 用全限定类名，如joke(java.util.List,int)；
2. `execution(* joke(String,*)))`：匹配目标类中的joke()方法，该方法第一个入参为String，第二个入参可以是任意类型，如`joke(String s1,String s2)`和`joke(String s1,double d2)`都匹配，但`joke(String s1,double d2,String s3)`则不匹配；
3. `execution(* joke(String,..)))`：匹配目标类中的joke()方法，该方法第 一个入参为String，后面可以有任意个入参且入参类型不限，如`joke(String s1)`、`joke(String s1,String s2)`和`joke(String s1,double d2,String s3)`都匹配。
4. `execution(* joke(Object+)))`：匹配目标类中的joke()方法，方法拥有一个入参，且入参是Object类型或该类的子类。 它匹配`joke(String s1)`和`joke(Client c)`。如果我们定义的切点是`execution(* joke(Object))`，则只匹配`joke(Object object)`而不匹配`joke(String cc)`或`joke(Client c)`。

**args()和@args()**
 args()函数的入参是类名，@args()函数的入参必须是注解类的类名。虽然args()允许在类名后使用+通配符后缀，但该通配符在此处没有意义：添加和不添加效果都一样。

### 4.3 args()

该函数接受一个类名，表示目标类方法入参对象按类型匹配于指定类时切点匹配。如下面的例子：`args(com.ankeetc.Waiter)`表示运行时入参是Waiter类型的方法，它和`execution(* *(com.ankeetc.Waiter))`区别在于后者是针对类方法的签名而言的，而前者则针对运行时的入参类型而言。如`args(com.ankeetc.Waiter)`既匹配于`addWaiter(Waiter waiter)`，也匹配于`addNaiveWaiter(NaiveWaiter naiveWaiter)`，而`execution(* *(com.ankeetc.Waiter))`只匹配`addWaiter(Waiter waiter)`方法；实际上，`args(com.ankeetc.Waiter)`等价于`execution(* *(com.ankeetc.Waiter+))`，当然也等价于`args(com.ankeetc.Waiter+)`。

### 4.4 @args()

该函数接受一个注解类的类名，当方法的运行时入参对象标注发指定的注解时匹配切点。这个切点函数的匹配规则不太容易理解，我们通过以下示意图对此进行详细讲解：

![14623831-006f6092b530c62c](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329225725.webp)

- T0、T1、T2、T3具有如图所示的继承关系，假设目标类方法的签名为fun(T1 t)，它的入参为T1，而切面的切点定义为@args(M)，T2类标注了@M。当fun(T1 t)传入对象是T2或T3时，则方法匹配@args(M)所声明定义的切点；
- 假设方法签名是fun(T1 t)，入参为T1，而标注@M的类是T0，当funt(T1 t)传入T1、T2、T3的实例时，均不匹配切点@args(M)。
- 在类的继承树中，①处为方法签名中入参类型在类继承树中的位置，我们称之为入参类型点，而②处为标注了@M注解的类在类继承树中位置，我们称之为注解点。判断方法在运行时是否匹配@agrs(M)切点，可以根据①点和②点在类继承树中的相对位置来判别：
- 如果在类继承树中注解点②高于入参类型点①，则该目标方法不可能匹配切点@args(M)，如图 5所示；
- 如果在类继承树中注解点②低于入参类型点①，则注解点所在类及其子孙类作为方法入参时，该方法匹配@args(M)切点，如示意图1所示。

![14623831-d10aef1793e81552](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329225740.webp)

下面举一个具体的例子，假设我们定义这样的切点：`@args(com.ankeetc.Monitorable)` ，如果NaiveWaiter标注了@Monitorable，则对于WaiterManager#addWaiter(Waiter w)方法来说，如果入参是NaiveWaiter或其子类对象，该方法匹配切点，如果入参是NaughtyWaiter对象，不匹配切点。如果 Waiter标注了@Monitorable，但NaiveWaiter未标注@Monitorable，则 WaiterManager#addNaiveWaiter(NaiveWaiter w)却不匹配切点，这是因为注解点（在Waiter）高于入参类型点（NaiveWaiter）。

### 4.5.within()

通过类匹配模式串声明切点，within()函数定义的连接点是针对目标类而言，而非针对运行期对象的类型而言，这一点和execution()是相同 的。但和execution()函数不同的是，within()所指定的连接点最小范围只能是类，而execution()所指定的连接点，可以大到包， 小到方法入参。所以从某种意义上说，execution()函数的功能涵盖了within()函数的功能。within()函数的语法如下所示：`within(<类匹配模式>)`。形如`within(com.ankeetc.NaiveWaiter)`是within()函数所能表达的最小粒度，如果试图用within()匹配方法级别的连接点，如`within(com.ankeetc.NaiveWaiter.greet*)`将会产生解析错误。

1. `within(com.ankeetc.NaiveWaiter)`：匹配目标类NaiveWaiter的所有方法。如果切点调整为`within(com.ankeetc.Waiter)`，则NaiveWaiter和 NaughtyWaiter中的所有方法都不匹配，而Waiter本身是接口不可能实例化，所以`within(com.ankeetc.Waiter)`的声明是无意义的；
2. `within(com.ankeetc.*)`：匹配com.ankeetc包中的所有类，但不包括子孙包，所以com.ankeetc.service包中类的方法不匹配这个切点；
3. `within(com.ankeetc..*)`：匹配com.ankeetc包及子孙包中的类，所以com.ankeetc.service、com.ankeetc.dao以及com.ankeetc.service.fourm等包中所有类的方法都匹配这个切点。

### 4.6.@within()和@target()

除@annotation()和@args()外，还有另外两个用于注解的切点函数，它们分别是@target()和@within()。和 @annotation()及@args()函数一样，它们也只接受注解类名作为入参。其中@target(M)匹配任意标注了@M的目标类，而 @within(M)匹配标注了@M的类及子孙类。

@target(M)切点的匹配规则如图所示：

![14623831-616c191e135aae6b](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329225811.webp)
假设NaiveWaiter标注了@Monitorable，则其子类CuteNaiveWaiter没有标注@Monitorable，则 @target(com.ankeetc.Monitorable)匹配NaiveWaiter类的所有方法，但不匹配 CuteNaiveWaiter类的方法。

@within(M)切点的匹配规则如图所示：

![14623831-98e23beeb9be527c](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329225858.webp)
 假设NaiveWaiter标注了@Monitorable，而其子类CuteNaiveWaiter没有标注@Monitorable，则 `@within(com.ankeetc.Monitorable)`不但匹配NaiveWaiter类中的所有方法也匹配 CuteNaiveWaiter类中的所有方法。
 但有一个特别值得注意地方是，如果标注@M注解的是一个接口，则所有实现该接口的类并不匹配@within(M)。假设Waiter标注了@Monitorable注解，但NaiveWaiter、NaughtyWaiter及 CuteNaiveWaiter这些接口实现类都没有标注@Monitorable，则 @within(com.ankeetc.Monitorable)和@target(com.ankeetc.Monitorable)都不匹配NaiveWaiter、NaughtyWaiter及CuteNaiveWaiter。这是因为@within()、@target()以及 @annotation()都是针对目标类而言，而非针对运行时的引用类型而言，这点区别需要在开发中特别注意。

### 4.7.target()和this()

target()切点函数通过判断目标类是否按类型匹配指定类决定连接点是否匹配，而this()则通过判断代理类是否按类型匹配指定类来决定是否和切点 匹配。两者都仅接受类名的入参，虽然类名可以带`+`通配符，但对于这两个函数来说，使用与不使用+通配符，效果完全相同。

#### 4.7.1.target()

target(M)表示如果目标类按类型匹配于M，则目标类所有方法匹配切点。

1. `target(com.ankeetc.Waiter)`：NaiveWaiter、NaughtyWaiter以及CuteNaiveWaiter的所有方法都匹配切点，包括那些未在Waiter接口中定义的方法，如NaiveWaiter#simle()和NaughtyWaiter#joke()方法。
2. `target(com.ankeetc.Waiter+)`：和target(com.ankeetc.Waiter)是等价的。

#### 4.7.2.this()

根据Spring的官方文档，this()函数判断代理对象的类是否按类型匹配于指定类，如果匹配，则代理对象的所有连接点匹配切点。但通过实验，我们发现实际情况和文档有出入，如我们声明一个this(com.ankeetc.NaiveWaiter)的切点，如果不使用CGLib代理，则生成的代理 对象是Waiter类型，而非NaiveWaiter类型，这一点可以简单地通过instanceof操作符进行判断。但是，我们发现 NaiveWaiter中所有的方法还是被织入了增强。

#### 4.7.3.区别与联系

在一般情况下，使用this()和target()通过定义切点，两者是等效的：

1. target(com.ankeetc.Waiter) 等价于this(com.ankeetc.Waiter)
2. target(com.ankeetc.NaiveWaiter) 等价于 this(com.ankeetc.NaiveWaiter)

两者区别体现在通过引介切面产生的代理对象时的具体表现，如果我们通过本文前面的方法为NaiveWaiter引介一个Seller接口的实现，则 this(com.ankeetc.Seller)匹配NaiveWaiter代理对象的所有方法，包括NaiverWaiter本身的greetTo()、serverTo()方法以及通过Seller接口引入的sell()方法。而 target(com.ankeetc.Seller)不匹配通过引介切面产生的NaiveWaiter代理对象。

## 5.AspectJ进阶

### 5.1.切点复合运算

可以使用切点复合运算符来组合多个切点。

### 5.2.命名切点

切点直接声明在增强方法处被称为匿名切点，匿名切点只能在声明处使用。如果希望在其他地方重用一个切点，我们可以通过@Pointcut注解以及切面类方法对切点进行命名。

```java
public class TestPointcut {
    // 命名切点：可以通过权限修饰符控制切点的访问权限
    @Pointcut("within(com.ankeetc.spring.*)")
    public void inPackage(){}
}

@Aspect
public class TestAspect {
    // 使用切点
    @Before("TestPointcut.inPackage()")
    public void advice() {}
}
```

### 5.3.增强织入顺序

一个连接点可以同时匹配多个切点，切点对应的增强在连接点上的织入顺序的安排主要有以下3种情况：

1. 如果增强在同一个切面类中声明，则依照增强在切面类中定义的顺序进行织入；
2. 如何增强位于不同的切面类中，且这些切面类都实现了org.springframework.core.Order接口，则由接口方法的顺序号决定（顺序号小的先织入）；
3. 如果增强位于不同的切面类中，且这些切面类没有实现org.springframework.core.Order接口，织入的顺序是不确定的。

### 5.4.访问连接点信息

AspectJ使用org.aspectj.lang.JoinPoint接口表示目标类连接点对象，如果是环绕增强时，使用org.aspectj.lang.ProceedingJoinPoint表示连接点对象，该类是JoinPoint的子接口，任何一个增强方法都可以通过将第一个入参声明为JoinPoint访问到连接点上下文的信息。

### 5.5.绑定连接点方法入参

args()、this()、target()、@args()、@within()、@target()和@annotation()这7个函数除了可以指定类名外，也可以指定变量名，并将目标对象中变量绑定到增强的方法中。其中，args()用于绑定连接点方法的入参；@annotation()用于绑定连接点方法的注解对象；而@args()用于绑定连接点方法入参的注解。

```java
@Aspect
public class TestAspect4 {
    @Before("target(com.ankeetc.spring.NaiveWaiter) && args(name,num,..)")
    public void bindJoinPointParams(int num, String name) {
        System.out.println("---bindJoinPointParams---");
        System.out.println("name:" + name);
        System.out.println("num:" + num);
        System.out.println("---bindJoinPointParams---");
    }
}
```

我们通过args(name,num,..)进行连接点参数的绑定，和前面我们所讲述的方式不一样，当args()函数入参为参数名时，共包括两方面的信息：

- 连接点匹配规则信息：连接点方法第一个入参是String类型，第二个入参是int类型；
- 连接点方法入参和增强方法入参的绑定信息：连接点方法的第一个入参绑定到增强方法的name参数上，第二个入参绑定到增强方法的num入参上。

**切点匹配和参数绑定的过程**

1. 首先args()根据参数名称在增强方法中查到名称相同的入参并获知对应的类型，这样就知道匹配连接点方法的入参类型。
2. 其次连接点方法入参类型所在的位置则由参数名在args()函数中声明的位置决定。
3. args(name,num)只匹配第一个入参是String第二个入参是int的目标类方法。

### 5.6.绑定被代理对象

使用this()或target()可绑定被代理对象实例，在通过类实例名绑定对象时，还依然具有原来连接点匹配的功能，只不过类名是通过增强方法中同名入参的类型间接决定罢了。

```java
@Aspect
public class TestAspect {
    @Before("this(waiter)")
    public void bindProxyObj(Waiter waiter){
        System.out.println("---bindProxyObj---");
        System.out.println(waiter.getClass().getName());
        System.out.println("---bindProxyObj---");
    }
}
```

切点表达式首先按类变量名查找增强方法的入参列表，进而获取类变量名对应的类为com.ankeetc.Waiter，这样就知道了切点的定义为this(com.ankeetc.Waiter)，即所有代理对象为Waiter类的所有方法匹配该切点。增强方法通过waiter入参绑定目标对象。

### 5.7.绑定类注解对象

@within()和@target()函数可以将目标类的注解对象绑定到增强方法中，我们通过@within()演示注解绑定的操作。

```java
@Aspect
public class TestAspect6 {
    @Before("@within(m)")
    public void bindTypeAnnoObject(Monitorable m) {
        System.out.println("---bindTypeAnnoObject---");
        System.out.println(m.getClass().getName());
        System.out.println("---bindTypeAnnoObject---");
    }
}
```

### 5.8.绑定返回值

在后置增强中，我们可以通过returning绑定连接点方法的返回值。

```java
@Aspect
public class TestAspect7 {
    @AfterReturning(value = "target(com.ankeetc.spring.SmartSeller)", returning = "retVal")
    public void bindReturnValue(int retVal) {
        System.out.println("---bindReturnValue---");
        System.out.println("returnValue:" + retVal);
        System.out.println("---bindReturnValue---");
    }
}
```

### 5.9.绑定方法抛出的异常

和通过切点函数绑定连接点信息不同，连接点抛出的异常必须使用AfterThrowing注解的throwing成员进行绑定。

```java
@Aspect
public class TestAspect8 {
    // 这个异常增强只在连接点抛出异常instanceof IllegalArgumentException才匹配
    @AfterThrowing(value = "target(com.ankeetc.spring.SmartSeller)", throwing = "iae")
    public void bindException(IllegalArgumentException iae) {
        System.out.println("---bindException---");
        System.out.println("exception:" + iae.getMessage());
        System.out.println("---bindException---");
    }
}
```

## 6.基于Schema配置切面

略

## 7.混合使用切面类型

Spring提供了多种定义切面的方式，但底层都是CGLib和JDK 动态代理，所以可以（但不建议）在一个项目中使用多种切面定义方式。

四种比较：

![14623831-846d3e1d71c53c83](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329230014.webp)