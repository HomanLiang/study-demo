[TOC]



# Spring Boot 应用

## 1.全局异常处理

在开发中，我们经常会使用`try/catch块`来捕获异常进行处理，如果有些代码中忘记捕获异常或者不可见的一些异常出现，就会响应给前端一些不友好的提示，这时候我们可以使用全局异常处理。这样就不用在代码中写那些烦人的`try/catch块了`，代码的可读性也会提高。

SpringBoot提供的的注解`@ControllerAdvice`表示开启全局异常捕获，在自定义的异常方法上使用`ExceptionHandler`来进行统一处理。

下面一起看看如何优雅的处理全局异常！

### 1.1.定义响应状态码及信息的枚举类

```java
@Getter
public enum CodeEnum {
    SUCCESS(0,"请求成功"),
    ERROR(500,"未知异常"),
    ERROR_EMPTY_RESULT(1001,"查询结果为空"),
    ERROR_INCOMPLETE_RESULT(1002,"请求参数不全");
    
    private int code;
    private String message;
    CodeEnum(int code,String message){
        this.code = code;
        this.message = message;
    }
}
```

### 1.2.定义响应数据的实体类

```java
@Slf4j
@Data
public class R<T> implements Serializable {

    private static final long serialVersionUID = 572235155491705152L;
    /**
     * 响应的状态码
     */
    private int code;
    /***
     * 响应的信息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;

    /**
     * 放入响应码并返回
     * @param code
     * @param msg
     * @return
     */
    public R fillCode(int code,String msg){
        this.code = code;
        this.message = msg;
        return this;
    }

    /**
     * 放入响应码并返回
     * @param codeEnum
     * @return
     */
    public R fillCode(CodeEnum codeEnum){
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMessage();
        return this;
    }

    /**
     * 放入数据并响应成功状态
     * @param data
     * @return
     */
    public R fillData(T data){
        this.code = CodeEnum.SUCCESS.getCode();
        this.message = CodeEnum.SUCCESS.getMessage();
        this.data = data;
        return this;
    }
}
```

### 1.3.自定义两个异常

根据业务需求自定义异常，在本文中我定义了两个异常，分别用作响应结果为空时处理和请求参数错误时处理。

```java
@Data
public class EmptyResutlException extends RuntimeException {

    private static final long serialVersionUID = -8839210969758687047L;
    private int code;
    private String message;

    public EmptyResutlException(CodeEnum codeEnum){
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMessage();
    }
}
@Data
public class RequestParamException extends RuntimeException {

    private static final long serialVersionUID = 4748844811214637041L;
    private int code;
    private String message;

    public RequestParamException(CodeEnum codeEnum){
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMessage();
    }
}
```

### 1.4.定义全局异常处理类

由于这里我想要响应的结果为实体类对象，因此我直接用`@RestControllerAdvice`来代替了`@ControllerAdvice`，这两个注解的差别跟`@Controller`和`@RestController`一样，rest的响应体为json格式的数据。

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 查询结果为空时处理
     * @param e
     * @return
     */
    @ExceptionHandler(EmptyResutlException.class)
    public R emptyResultExceptionHandler(EmptyResutlException e){
        log.error("查询结果为空：{}",e.getMessage());
        R result = new R();
        result.fillCode(e.getCode(),e.getMessage());
        return result;
    }

    /**
     * 请求参数错误时处理
     * @param e
     * @return
     */
    @ExceptionHandler(RequestParamException.class)
    public R requestParamExceptionHandler(RequestParamException e){
        log.error("请求参数不合法：{}",e.getMessage());
        R result = new R();
        result.fillCode(e.getCode(),e.getMessage());
        return result;
    }

    /**
     * 处理其他异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public R exceptionHandler(Exception e){
        log.error("未知异常：{}",e.getMessage());
        R result = new R();
        result.fillCode(CodeEnum.ERROR);
        return result;
    }
}
```

### 1.5.自定义接口测试异常

```java
@RestController
public class TestController {

    @GetMapping("getString")
    public R getString(String name){

        if(StringUtils.isEmpty(name)){
            throw new RequestParamException(1002,"请求参数name为空");
        }else if ("Java旅途".equals(name)) {
            // 这里没有查询操作，当请求参数是Java旅途的时候，模拟成查询结果为空
            throw new EmptyResutlException(1001,"查询结果为空");
        }
        // 这里模拟一下除自定义异常外的其他两种异常
        int i = 0;
        i = 5/i;
        return new R().fillData(name);
    }
}
```

在实际开发中可以自定义响应状态码的枚举类和自定义异常以满足需求。



## 2.Springboot集成JUnit5优雅进行单元测试

### 2.1.为什么使用JUnit5

- JUnit4被广泛使用，但是许多场景下使用起来语法较为繁琐，JUnit5中支持lambda表达式，语法简单且代码不冗余。
- JUnit5易扩展，包容性强，可以接入其他的测试引擎。
- 功能更强大提供了新的断言机制、参数化测试、重复性测试等新功能。
- ps：开发人员为什么还要测试，单测写这么规范有必要吗？其实单测是开发人员必备技能，只不过很多开发人员开发任务太重导致调试完就不管了，没有系统化得单元测试，单元测试在系统重构时能发挥巨大的作用，可以在重构后快速测试新的接口是否与重构前有出入。

### 2.2.简介

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235116.png)

如图，JUnit5结构如下：

- **JUnit Platform**： 这是Junit提供的平台功能模块，通过它，其它的测试引擎都可以接入Junit实现接口和执行。
- **JUnit JUpiter**：这是JUnit5的核心，是一个基于JUnit Platform的引擎实现，它包含许多丰富的新特性来使得自动化测试更加方便和强大。
- **JUnit Vintage**：这个模块是兼容JUnit3、JUnit4版本的测试引擎，使得旧版本的自动化测试也可以在JUnit5下正常运行。

### 2.3.依赖引入

我们以`SpringBoot2.3.1`为例，引入如下依赖，防止使用旧的junit4相关接口我们将其依赖排除。

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.junit.vintage</groupId>
                    <artifactId>junit-vintage-engine</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```

### 2.4.常用注解

- @BeforeEach：在每个单元测试方法执行前都执行一遍
- @BeforeAll：在每个单元测试方法执行前执行一遍（只执行一次）
- @DisplayName("商品入库测试")：用于指定单元测试的名称
- @Disabled：当前单元测试置为无效，即单元测试时跳过该测试
- @RepeatedTest(n)：重复性测试，即执行n次
- @ParameterizedTest：参数化测试，
- @ValueSource(ints = {1, 2, 3})：参数化测试提供数据

### 2.5.断言

JUnit Jupiter提供了强大的断言方法用以验证结果，在使用时需要借助java8的新特性lambda表达式，均是来自`org.junit.jupiter.api.Assertions`包的`static`方法。

assertTrue`与`assertFalse`用来判断条件是否为`true`或`false

```
	@Test
    @DisplayName("测试断言equals")
    void testEquals() {
        assertTrue(3 < 4);
    }    
```

assertNull`与`assertNotNull`用来判断条件是否为·`null

```
@Test
@DisplayName("测试断言NotNull")
void testNotNull() {
	assertNotNull(new Object());
}
```

`assertThrows`用来判断执行抛出的异常是否符合预期，并可以使用异常类型接收返回值进行其他操作

```java
    @Test
    @DisplayName("测试断言抛异常")
    void testThrows() {
        ArithmeticException arithExcep = assertThrows(ArithmeticException.class, () -> {
            int m = 5/0;
        });
        assertEquals("/ by zero", arithExcep.getMessage());
    }
```

`assertTimeout`用来判断执行过程是否超时

```java
    @Test
    @DisplayName("测试断言超时")
    void testTimeOut() {
        String actualResult = assertTimeout(ofSeconds(2), () -> {
            Thread.sleep(1000);
            return "a result";
        });
        System.out.println(actualResult);
    }
```

`assertAll`是组合断言，当它内部所有断言正确执行完才算通过

```java
    @Test
    @DisplayName("测试组合断言")
    void testAll() {
        assertAll("测试item商品下单",
                () -> {
                    //模拟用户余额扣减
                    assertTrue(1 < 2, "余额不足");
                },
                () -> {
                    //模拟item数据库扣减库存
                    assertTrue(3 < 4);
                },
                () -> {
                    //模拟交易流水落库
                    assertNotNull(new Object());
                }
        );
    }
```

### 2.6.重复性测试

在许多场景中我们需要对同一个接口方法进行重复测试，例如对幂等性接口的测试。

JUnit Jupiter通过使用`@RepeatedTest(n)`指定需要重复的次数

```java
    @RepeatedTest(3)
    @DisplayName("重复测试")
    void repeatedTest() {
        System.out.println("调用");
    }
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401235109.png)

### 2.7.参数化测试

参数化测试可以按照多个参数分别运行多次单元测试这里有点类似于重复性测试，只不过每次运行传入的参数不用。需要使用到`@ParameterizedTest`，同时也需要`@ValueSource`提供一组数据，它支持八种基本类型以及`String`和自定义对象类型，使用极其方便。

```java
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    @DisplayName("参数化测试")
    void paramTest(int a) {
        assertTrue(a > 0 && a < 4);
    }
```

### 2.8.内嵌测试

JUnit5提供了嵌套单元测试的功能，可以更好展示测试类之间的业务逻辑关系，我们通常是一个业务对应一个测试类，有业务关系的类其实可以写在一起。这样有利于进行测试。而且内联的写法可以大大减少不必要的类，精简项目，防止类爆炸等一系列问题。

```java
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Junit5单元测试")
public class MockTest {
    //....
    @Nested
    @DisplayName("内嵌订单测试")
    class OrderTestClas {
        @Test
        @DisplayName("取消订单")
        void cancelOrder() {
            int status = -1;
            System.out.println("取消订单成功,订单状态为:"+status);
        }
    }
}
```

## 3.自定义注解+拦截器优雅的实现敏感数据的加解密

在实际生产项目中，经常需要对如身份证信息、手机号、真实姓名等的敏感数据进行加密数据库存储，但在业务代码中对敏感信息进行手动加解密则十分不优雅，甚至会存在错加密、漏加密、业务人员需要知道实际的加密规则等的情况。

本文将介绍使用springboot+mybatis拦截器+自定义注解的形式对敏感数据进行存储前拦截加密的详细过程。

### 3.1.什么是Mybatis Plugin

在mybatis官方文档中，对于Mybatis plugin的的介绍是这样的：

MyBatis 允许你在已映射语句执行过程中的某一点进行拦截调用。默认情况下，MyBatis 允许使用插件来拦截的方法调用包括：

```
//语句执行拦截
Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)

// 参数获取、设置时进行拦截
ParameterHandler (getParameterObject, setParameters)

// 对返回结果进行拦截
ResultSetHandler (handleResultSets, handleOutputParameters)

//sql语句拦截
StatementHandler (prepare, parameterize, batch, update, query)
```

简而言之，即在执行sql的整个周期中，我们可以任意切入到某一点对sql的参数、sql执行结果集、sql语句本身等进行切面处理。基于这个特性，我们便可以使用其对我们需要进行加密的数据进行切面统一加密处理了（分页插件 pageHelper 就是这样实现数据库分页查询的）。

### 3.2.实现基于注解的敏感信息加解密拦截器

#### 3.2.1.实现思路

对于数据的加密与解密，应当存在两个拦截器对数据进行拦截操作

参照官方文档，因此此处我们应当使用ParameterHandler拦截器对入参进行加密

使用ResultSetHandler拦截器对出参进行解密操作。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504100033.webp)

目标需要加密、解密的字段可能需要灵活变更，此时我们定义一个注解，对需要加密的字段进行注解，那么便可以配合拦截器对需要的数据进行加密与解密操作了。

mybatis的interceptor接口有以下方法需要实现。

```
public interface Interceptor {
 
  //主要参数拦截方法
  Object intercept(Invocation invocation) throws Throwable;
 
  //mybatis插件链
  default Object plugin(Object target) {return Plugin.wrap(target, this);}
 
  //自定义插件配置文件方法
  default void setProperties(Properties properties) {}
 
}
```

#### 3.2.2.定义需要加密解密的敏感信息注解

定义注解敏感信息类（如实体类POJO\PO）的注解

```
/**
 * 注解敏感信息类的注解
 */
@Inherited
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveData {
}
```

定义注解敏感信息类中敏感字段的注解

```
/**
 * 注解敏感信息类中敏感字段的注解
 */
@Inherited
@Target({ ElementType.Field })
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveField {
}
```

#### 3.2.3.定义加密接口及其实现类

定义加密接口，方便以后拓展加密方法（如AES加密算法拓展支持PBE算法，只需要注入时指定一下便可）

```
public interface EncryptUtil {
    
    /**
     * 加密
     *
     * @param declaredFields paramsObject所声明的字段
     * @param paramsObject   mapper中paramsType的实例
     * @return T
     * @throws IllegalAccessException 字段不可访问异常
     */
     <T> T encrypt(Field[] declaredFields, T paramsObject) throws IllegalAccessException;
}
```

EncryptUtil 的AES加密实现类，此处AESUtil为自封装的AES加密工具，需要的小伙伴可以自行封装，本文不提供。（搜索公众号Java知音，回复“2021”，送你一份Java面试题宝典）

```
@Component
public class AESEncrypt implements EncryptUtil {
    
    @Autowired
    AESUtil aesUtil;
 
    /**
     * 加密
     *
     * @param declaredFields paramsObject所声明的字段
     * @param paramsObject   mapper中paramsType的实例
     * @return T
     * @throws IllegalAccessException 字段不可访问异常
     */
    @Override
    public <T> T encrypt(Field[] declaredFields, T paramsObject) throws IllegalAccessException {
        for (Field field : declaredFields) {
            //取出所有被EncryptDecryptField注解的字段
            SensitiveField sensitiveField = field.getAnnotation(SensitiveField.class);
            if (!Objects.isNull(sensitiveField)) {
                field.setAccessible(true);
                Object object = field.get(paramsObject);
                //暂时只实现String类型的加密
                if (object instanceof String) {
                    String value = (String) object;
                    //加密  这里我使用自定义的AES加密工具
                    field.set(paramsObject, aesUtil.encrypt(value));
                }
            }
        }
        return paramsObject;
    }
}
```

#### 3.2.4.实现入参加密拦截器

Myabtis包中的org.apache.ibatis.plugin.Interceptor拦截器接口要求我们实现以下三个方法

```
public interface Interceptor {
 
  //核心拦截逻辑
  Object intercept(Invocation invocation) throws Throwable;
  
  //拦截器链
  default Object plugin(Object target) {return Plugin.wrap(target, this);}
 
  //自定义配置文件操作
  default void setProperties(Properties properties) { }
 
}
```

因此，参考官方文档的示例，我们自定义一个入参加密拦截器。

@Intercepts 注解开启拦截器，@Signature 注解定义拦截器的实际类型。

**@Signature中**

- type 属性指定当前拦截器使用StatementHandler 、ResultSetHandler、ParameterHandler，Executor的一种
- method 属性指定使用以上四种类型的具体方法（可进入class内部查看其方法）。
- args 属性指定预编译语句

此处我们使用了 ParameterHandler.setParamters()方法，拦截mapper.xml中paramsType的实例（即在每个含有paramsType属性mapper语句中，都执行该拦截器，对paramsType的实例进行拦截处理）

```
/**
 * 加密拦截器
 * 注意@Component注解一定要加上
 *
 * @author : tanzj
 * @date : 2020/1/19.
 */
@Slf4j
@Component
@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = PreparedStatement.class),
})
public class EncryptInterceptor implements Interceptor {
 
    private final EncryptDecryptUtil encryptUtil;
 
    @Autowired
    public EncryptInterceptor(EncryptDecryptUtil encryptUtil) {
        this.encryptUtil = encryptUtil;
    }
 
    @Override
   
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //@Signature 指定了 type= parameterHandler 后，这里的 invocation.getTarget() 便是parameterHandler 
        //若指定ResultSetHandler ，这里则能强转为ResultSetHandler
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
        // 获取参数对像，即 mapper 中 paramsType 的实例
        Field parameterField = parameterHandler.getClass().getDeclaredField("parameterObject");
        parameterField.setAccessible(true);
        //取出实例
        Object parameterObject = parameterField.get(parameterHandler);
        if (parameterObject != null) {
            Class<?> parameterObjectClass = parameterObject.getClass();
            //校验该实例的类是否被@SensitiveData所注解
            SensitiveData sensitiveData = AnnotationUtils.findAnnotation(parameterObjectClass, SensitiveData.class);
            if (Objects.nonNull(sensitiveData)) {
                //取出当前当前类所有字段，传入加密方法
                Field[] declaredFields = parameterObjectClass.getDeclaredFields();
                encryptUtil.encrypt(declaredFields, parameterObject);
            }
        }
        return invocation.proceed();
    }
 
    /**
     * 切记配置，否则当前拦截器不会加入拦截器链
     */
    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }
 
    //自定义配置写入，没有自定义配置的可以直接置空此方法
    @Override
    public void setProperties(Properties properties) {
    }
}
```

至此完成自定义加密拦截加密。

#### 3.2.5.定义解密接口及其实现类

解密接口，其中result为mapper.xml中resultType的实例。

```
public interface DecryptUtil {
 
    /**
     * 解密
     *
     * @param result resultType的实例
     * @return T
     * @throws IllegalAccessException 字段不可访问异常
     */
     <T> T decrypt(T result) throws IllegalAccessException;
    
}
```

解密接口AES工具解密实现类

```
public class AESDecrypt implements DecryptUtil {
    
    @Autowired
    AESUtil aesUtil;
    
    /**
     * 解密
     *
     * @param result resultType的实例
     * @return T
     * @throws IllegalAccessException 字段不可访问异常
     */
    @Override
    public <T> T decrypt(T result) throws IllegalAccessException {
        //取出resultType的类
        Class<?> resultClass = result.getClass();
        Field[] declaredFields = resultClass.getDeclaredFields();
        for (Field field : declaredFields) {
            //取出所有被EncryptDecryptField注解的字段
            SensitiveField sensitiveField = field.getAnnotation(SensitiveField.class);
            if (!Objects.isNull(sensitiveField)) {
                field.setAccessible(true);
                Object object = field.get(result);
                //只支持String的解密
                if (object instanceof String) {
                    String value = (String) object;
                    //对注解的字段进行逐一解密
                    field.set(result, aesUtil.decrypt(value));
                }
            }
        }
        return result;
    }
}
```

#### 3.2.6.定义出参解密拦截器

```
@Slf4j
@Component
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class DecryptInterceptor implements Interceptor {
 
    @Autowired
    DecryptUtil aesDecrypt;
 
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        //取出查询的结果
        Object resultObject = invocation.proceed();
        if (Objects.isNull(resultObject)) {
            return null;
        }
        //基于selectList
        if (resultObject instanceof ArrayList) {
            ArrayList resultList = (ArrayList) resultObject;
            if (!CollectionUtils.isEmpty(resultList) && needToDecrypt(resultList.get(0))) {
                for (Object result : resultList) {
                    //逐一解密
                    aesDecrypt.decrypt(result);
                }
            }
        //基于selectOne
        } else {
            if (needToDecrypt(resultObject)) {
                aesDecrypt.decrypt(resultObject);
            }
        }
        return resultObject;
    }
 
    private boolean needToDecrypt(Object object) {
        Class<?> objectClass = object.getClass();
        SensitiveData sensitiveData = AnnotationUtils.findAnnotation(objectClass, SensitiveData.class);
        return Objects.nonNull(sensitiveData);
    }
 
 
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
 
    @Override
    public void setProperties(Properties properties) {
 
    }
}
```

至此完成解密拦截器的配置工作。

### 3.注解实体类中需要加解密的字段

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504100211.webp)

此时在mapper中，指定paramType=User resultType=User 便可实现脱离业务层，基于mybatis拦截器的加解密操作。

## 4.接口的优雅写法

### 4.1.前言

一个后端接口大致分为四个部分组成：接口地址（url）、接口请求方式（get、post等）、请求数据（request）、响应数据（response）。如何构建这几个部分每个公司要求都不同，没有什么“一定是最好的”标准，但一个优秀的后端接口和一个糟糕的后端接口对比起来差异还是蛮大的，其中最重要的关键点就是看**是否规范!** 本文就一步一步演示如何构建起一个优秀的后端接口体系，体系构建好了自然就有了规范，同时再构建新的后端接口也会十分轻松。

### 4.2.所需依赖包

这里用的是SpringBoot配置项目，本文讲解的重点是后端接口，所以只需要导入一个spring-boot-starter-web包就可以了：

```
<!--web依赖包，web应用必备-->
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

本文还用了swagger来生成API文档，lombok来简化类，不过这两者不是必须的，可用可不用。

### 4.3.参数校验

一个接口一般对参数（请求数据）都会进行安全校验，参数校验的重要性自然不必多说，那么如何对参数进行校验就有讲究了。

#### 4.3.1.业务层校验

首先我们来看一下最常见的做法，就是在业务层进行参数校验：

```
public String addUser(User user) {
     if (user == null || user.getId() == null || user.getAccount() == null || user.getPassword() == null || user.getEmail() == null) {
         return "对象或者对象字段不能为空";
     }
     if (StringUtils.isEmpty(user.getAccount()) || StringUtils.isEmpty(user.getPassword()) || StringUtils.isEmpty(user.getEmail())) {
         return "不能输入空字符串";
     }
     if (user.getAccount().length() < 6 || user.getAccount().length() > 11) {
         return "账号长度必须是6-11个字符";
     }
     if (user.getPassword().length() < 6 || user.getPassword().length() > 16) {
         return "密码长度必须是6-16个字符";
     }
     if (!Pattern.matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", user.getEmail())) {
         return "邮箱格式不正确";
     }
     // 参数校验完毕后这里就写上业务逻辑
     return "success";
 }
```

这样做当然是没有什么错的，而且格式排版整齐也一目了然，不过这样太繁琐了，这还没有进行业务操作呢光是一个参数校验就已经这么多行代码，实在不够优雅。我们来改进一下，使用Spring Validator和Hibernate Validator这两套Validator来进行方便的参数校验！这两套Validator依赖包已经包含在前面所说的web依赖包里了，所以可以直接使用。

#### 4.3.2.Validator + BindResult进行校验

Validator可以非常方便的制定校验规则，并自动帮你完成校验。首先在入参里需要校验的字段加上注解,每个注解对应不同的校验规则，并可制定校验失败后的信息：

```
@Data
public class User {
    @NotNull(message = "用户id不能为空")
    private Long id;

    @NotNull(message = "用户账号不能为空")
    @Size(min = 6, max = 11, message = "账号长度必须是6-11个字符")
    private String account;

    @NotNull(message = "用户密码不能为空")
    @Size(min = 6, max = 11, message = "密码长度必须是6-16个字符")
    private String password;

    @NotNull(message = "用户邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

校验规则和错误提示信息配置完毕后，接下来只需要在接口需要校验的参数上加上@Valid注解，并添加BindResult参数即可方便完成验证：

```
@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    private UserService userService;
    
    @PostMapping("/addUser")
    public String addUser(@RequestBody @Valid User user, BindingResult bindingResult) {
        // 如果有参数校验失败，会将错误信息封装成对象组装在BindingResult里
        for (ObjectError error : bindingResult.getAllErrors()) {
            return error.getDefaultMessage();
        }
        return userService.addUser(user);
    }
}
```

这样当请求数据传递到接口的时候Validator就自动完成校验了，校验的结果就会封装到BindingResult中去，如果有错误信息我们就直接返回给前端，业务逻辑代码也根本没有执行下去。此时，业务层里的校验代码就已经不需要了：

```
public String addUser(User user) {
     // 直接编写业务逻辑
     return "success";
 }
```

现在可以看一下参数校验效果。我们故意给这个接口传递一个不符合校验规则的参数，先传递一个错误数据给接口，故意将password这个字段不满足校验条件：

```
{
 "account": "12345678",
 "email": "123@qq.com",
 "id": 0,
 "password": "123"
}
```

再来看一下接口的响应数据：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104555.webp)

这样是不是方便很多？不难看出使用Validator校验有如下几个好处：

1. 简化代码，之前业务层那么一大段校验代码都被省略掉了。
2. 使用方便，那么多校验规则可以轻而易举的实现，比如邮箱格式验证，之前自己手写正则表达式要写那么一长串，还容易出错，用Validator直接一个注解搞定。（还有更多校验规则注解，可以自行去了解哦）
3. 减少耦合度，使用Validator能够让业务层只关注业务逻辑，从基本的参数校验逻辑中脱离出来。

使用Validator+ BindingResult已经是非常方便实用的参数校验方式了，在实际开发中也有很多项目就是这么做的，不过这样还是不太方便，因为你每写一个接口都要添加一个BindingResult参数，然后再提取错误信息返回给前端。这样有点麻烦，并且重复代码很多（尽管可以将这个重复代码封装成方法）。我们能否去掉BindingResult这一步呢？当然是可以的！

#### 4.3.3.Validator + 自动抛出异常

我们完全可以将BindingResult这一步给去掉：

```
@PostMapping("/addUser")
public String addUser(@RequestBody @Valid User user) {
    return userService.addUser(user);
}
```

去掉之后会发生什么事情呢？直接来试验一下，还是按照之前一样故意传递一个不符合校验规则的参数给接口。此时我们观察控制台可以发现接口已经引发`MethodArgumentNotValidException`异常了：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104602.webp)

其实这样就已经达到我们想要的效果了，参数校验不通过自然就不执行接下来的业务逻辑，去掉BindingResult后会自动引发异常，异常发生了自然而然就不会执行业务逻辑。也就是说，我们完全没必要添加相关BindingResult相关操作嘛。不过事情还没有完，异常是引发了，可我们并没有编写返回错误信息的代码呀，那参数校验失败了会响应什么数据给前端呢？我们来看一下刚才异常发生后接口响应的数据：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104611.webp)

没错，是直接将整个错误对象相关信息都响应给前端了！这样就很难受，不过解决这个问题也很简单，就是我们接下来要讲的全局异常处理！

### 4.4.全局异常处理

参数校验失败会自动引发异常，我们当然不可能再去手动捕捉异常进行处理，不然还不如用之前BindingResult方式呢。**又不想手动捕捉这个异常，又要对这个异常进行处理**，那正好使用SpringBoot全局异常处理来达到一劳永逸的效果！

#### 4.4.1.基本使用

首先，我们需要新建一个类，在这个类上加上`@ControllerAdvice`或`@RestControllerAdvice`注解，这个类就配置成全局处理类了。（这个根据你的Controller层用的是`@Controller`还是`@RestController`来决定） 然后在类中新建方法，在方法上加上`@ExceptionHandler`注解并指定你想处理的异常类型，接着在方法内编写对该异常的操作逻辑，就完成了对该异常的全局处理！我们现在就来演示一下对参数校验失败抛出的`MethodArgumentNotValidException`全局处理：

```
@RestControllerAdvice
public class ExceptionControllerAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
     // 从异常对象中拿到ObjectError对象
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        // 然后提取错误提示信息进行返回
        return objectError.getDefaultMessage();
    }
    
}
```

我们再来看下这次校验失败后的响应数据：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104618.webp)

没错，这次返回的就是我们制定的错误提示信息！我们通过全局异常处理优雅的实现了我们想要的功能！以后我们再想写接口参数校验，就只需要在入参的成员变量上加上Validator校验规则注解，然后在参数上加上`@Valid`注解即可完成校验，校验失败会自动返回错误提示信息，无需任何其他代码！

#### 4.4.2.自定义异常

全局处理当然不会只能处理一种异常，用途也不仅仅是对一个参数校验方式进行优化。在实际开发中，如何对异常处理其实是一个很麻烦的事情。传统处理异常一般有以下烦恼：

1. 是捕获异常(`try...catch`)还是抛出异常(`throws`)
2. 是在`controller`层做处理还是在`service`层处理又或是在`dao`层做处理
3. 处理异常的方式是啥也不做，还是返回特定数据，如果返回又返回什么数据
4. 不是所有异常我们都能预先进行捕捉，如果发生了没有捕捉到的异常该怎么办？

以上这些问题都可以用全局异常处理来解决，全局异常处理也叫统一异常处理，全局和统一处理代表什么？**代表规范！** 规范有了，很多问题就会迎刃而解！全局异常处理的基本使用方式大家都已经知道了，我们接下来更进一步的规范项目中的异常处理方式：自定义异常。在很多情况下，我们需要手动抛出异常，比如在业务层当有些条件并不符合业务逻辑，我这时候就可以手动抛出异常从而触发事务回滚。那手动抛出异常最简单的方式就是`throw new RuntimeException("异常信息")`了，不过使用自定义会更好一些：

1. 自定义异常可以携带更多的信息，不像这样只能携带一个字符串。
2. 项目开发中经常是很多人负责不同的模块，使用自定义异常可以统一了对外异常展示的方式。
3. 自定义异常语义更加清晰明了，一看就知道是项目中手动抛出的异常。

我们现在就来开始写一个自定义异常：

```
@Getter //只要getter方法，无需setter
public class APIException extends RuntimeException {
    private int code;
    private String msg;

    public APIException() {
        this(1001, "接口错误");
    }

    public APIException(String msg) {
        this(1001, msg);
    }

    public APIException(int code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
```

在刚才的全局异常处理类中记得添加对我们自定义异常的处理：

```
@ExceptionHandler(APIException.class)
public String APIExceptionHandler(APIException e) {
    return e.getMsg();
}
```

这样就对异常的处理就比较规范了，当然还可以添加对`Exception`的处理，这样无论发生什么异常我们都能屏蔽掉然后响应数据给前端，不过建议最后项目上线时这样做，能够屏蔽掉错误信息暴露给前端，在开发中为了方便调试还是不要这样做。现在全局异常处理和自定义异常已经弄好了，不知道大家有没有发现一个问题，就是当我们抛出自定义异常的时候全局异常处理只响应了异常中的错误信息msg给前端，并没有将错误代码code返回。这就要引申出我们接下来要讲的东西了：数据统一响应

### 4.5.数据统一响应

现在我们规范好了参数校验方式和异常处理方式，然而还没有规范响应数据！比如我要获取一个分页信息数据，获取成功了呢自然就返回的数据列表，获取失败了后台就会响应异常信息，即一个字符串，就是说前端开发者压根就不知道后端响应过来的数据会是啥样的！所以，统一响应数据是前后端规范中必须要做的！

#### 4.5.1.自定义统一响应体

统一数据响应第一步肯定要做的就是我们自己自定义一个响应体类，无论后台是运行正常还是发生异常，响应给前端的数据格式是不变的！那么如何定义响应体呢？可以参考我们自定义异常类，也来一个响应信息代码code和响应信息说明msg：

```
@Getter
public class ResultVO<T> {
    /**
     * 状态码，比如1000代表响应成功
     */
    private int code;
    /**
     * 响应信息，用来说明响应情况
     */
    private String msg;
    /**
     * 响应的具体数据
     */
    private T data;

    public ResultVO(T data) {
        this(1000, "success", data);
    }

    public ResultVO(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
```

然后我们修改一下全局异常处理那的返回值：

```
@ExceptionHandler(APIException.class)
public ResultVO<String> APIExceptionHandler(APIException e) {
    // 注意哦，这里返回类型是自定义响应体
    return new ResultVO<>(e.getCode(), "响应失败", e.getMsg());
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResultVO<String> MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
    ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
    // 注意哦，这里返回类型是自定义响应体
    return new ResultVO<>(1001, "参数校验失败", objectError.getDefaultMessage());
}
```

我们再来看一下此时如果发生异常了会响应什么数据给前端：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104628.webp)

OK，这个异常信息响应就非常好了，状态码和响应说明还有错误提示数据都返给了前端，并且是所有异常都会返回相同的格式！异常这里搞定了，别忘了我们到接口那也要修改返回类型，我们新增一个接口好来看看效果：

```
@GetMapping("/getUser")
public ResultVO<User> getUser() {
    User user = new User();
    user.setId(1L);
    user.setAccount("12345678");
    user.setPassword("12345678");
    user.setEmail("123@qq.com");
    
    return new ResultVO<>(user);
}
```

看一下如果响应正确返回的是什么效果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104633.webp)

这样无论是正确响应还是发生异常，响应数据的格式都是统一的，十分规范！

数据格式是规范了，不过响应码code和响应信息msg还没有规范呀！大家发现没有，无论是正确响应，还是异常响应，响应码和响应信息是想怎么设置就怎么设置，要是10个开发人员对同一个类型的响应写10个不同的响应码，那这个统一响应体的格式规范就毫无意义！所以，必须要将响应码和响应信息给规范起来。

#### 4.5.2.响应码枚举

要规范响应体中的响应码和响应信息用枚举简直再恰当不过了，我们现在就来创建一个响应码枚举类：

```
@Getter
public enum ResultCode {

    SUCCESS(1000, "操作成功"),

    FAILED(1001, "响应失败"),

    VALIDATE_FAILED(1002, "参数校验失败"),

    ERROR(5000, "未知错误");

    private int code;
    private String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
```

然后修改响应体的构造方法，让其只准接受响应码枚举来设置响应码和响应信息：

```
public ResultVO(T data) {
    this(ResultCode.SUCCESS, data);
}

public ResultVO(ResultCode resultCode, T data) {
    this.code = resultCode.getCode();
    this.msg = resultCode.getMsg();
    this.data = data;
}
```

然后同时修改全局异常处理的响应码设置方式：

```
@ExceptionHandler(APIException.class)
public ResultVO<String> APIExceptionHandler(APIException e) {
    // 注意哦，这里传递的响应码枚举
    return new ResultVO<>(ResultCode.FAILED, e.getMsg());
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResultVO<String> MethodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
    ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
    // 注意哦，这里传递的响应码枚举
    return new ResultVO<>(ResultCode.VALIDATE_FAILED, objectError.getDefaultMessage());
}
```

这样响应码和响应信息只能是枚举规定的那几个，就真正做到了响应数据格式、响应码和响应信息规范化、统一化！

#### 4.5.3.全局处理响应数据

接口返回统一响应体 + 异常也返回统一响应体，其实这样已经很好了，但还是有可以优化的地方。要知道一个项目下来定义的接口搞个几百个太正常不过了，要是每一个接口返回数据时都要用响应体来包装一下好像有点麻烦，有没有办法省去这个包装过程呢？当然是有滴，还是要用到全局处理。

首先，先创建一个类加上注解使其成为全局处理类。然后继承`ResponseBodyAdvice`接口重写其中的方法，即可对我们的`controller`进行增强操作，具体看代码和注释：

```
@RestControllerAdvice(basePackages = {"com.rudecrab.demo.controller"}) // 注意哦，这里要加上需要扫描的包
public class ResponseControllerAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> aClass) {
        // 如果接口返回的类型本身就是ResultVO那就没有必要进行额外的操作，返回false
        return !returnType.getParameterType().equals(ResultVO.class);
    }

    @Override
    public Object beforeBodyWrite(Object data, MethodParameter returnType, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest request, ServerHttpResponse response) {
        // String类型不能直接包装，所以要进行些特别的处理
        if (returnType.getGenericParameterType().equals(String.class)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // 将数据包装在ResultVO里后，再转换为json字符串响应给前端
                return objectMapper.writeValueAsString(new ResultVO<>(data));
            } catch (JsonProcessingException e) {
                throw new APIException("返回String类型错误");
            }
        }
        // 将原本的数据包装在ResultVO里
        return new ResultVO<>(data);
    }
}
```

重写的这两个方法是用来在`controller`将数据进行返回前进行增强操作，`supports`方法要返回为`true`才会执行`beforeBodyWrite`方法，所以如果有些情况不需要进行增强操作可以在`supports`方法里进行判断。对返回数据进行真正的操作还是在`beforeBodyWrite`方法中，我们可以直接在该方法里包装数据，这样就不需要每个接口都进行数据包装了，省去了很多麻烦。

我们可以现在去掉接口的数据包装来看下效果：

```
@GetMapping("/getUser")
public User getUser() {
    User user = new User();
    user.setId(1L);
    user.setAccount("12345678");
    user.setPassword("12345678");
    user.setEmail("123@qq.com");
    // 注意哦，这里是直接返回的User类型，并没有用ResultVO进行包装
    return user;
}
```

然后我们来看下响应数据：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504104639.webp)

成功对数据进行了包装！

> 注意：`beforeBodyWrite`方法里包装数据无法对String类型的数据直接进行强转，所以要进行特殊处理，这里不讲过多的细节，有兴趣可以自行深入了解。

### 4.6.总结

自此整个后端接口基本体系就构建完毕了

- 通过Validator + 自动抛出异常来完成了方便的参数校验
- 通过全局异常处理 + 自定义异常完成了异常操作的规范
- 通过数据统一响应完成了响应数据的规范
- 多个方面组装非常优雅的完成了后端接口的协调，让开发人员有更多的经历注重业务逻辑代码，轻松构建后端接口

再次强调，项目体系该怎么构建、后端接口该怎么写都没有一个绝对统一的标准，不是说一定要按照本文的来才是最好的，你怎样都可以，本文每一个环节你都可以按照自己的想法来进行编码，我只是提供了一个思路！















