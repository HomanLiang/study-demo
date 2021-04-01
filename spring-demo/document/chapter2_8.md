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