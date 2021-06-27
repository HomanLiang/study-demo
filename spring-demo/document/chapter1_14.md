[toc]



# Spring 应用

## 1.Assert类

### 1.1.Assert类目的

`Spring Assert` 类帮助我们校验参数。通过使用 `Assert` 类方法，我们可以写出我们认为是正确的假设，反之，会抛出运行时异常。

每个 `Assert` 的方法可以与 `java assert` 表达式进行比较。`java assert` 表达式在运行时如果条件校验失败，则抛出 `Error`，有趣的是，这些断言可以被禁用。

`Spring Assert` 的方法有一些特点： 

- 都是 `static` 方法 
- 抛出 `IllegalArgumentException` 或 `IllegalStateException` 异常 
- 第一个参数通常是需验证的对象或逻辑条件 
- 最后参数通常是异常消息，用于验证失败时显示 
- 消息可以作为 `String` 参数或 `Supplier` 参数传输

尽管 `Spring Assert` 与其他框架的名称类似，如 `JUnit` 或其他框架，但其实没有任何共同之处。`Spring Assert` 不是为了测试，而是为了调试。

### 1.2.使用示例

让我们定义 `Car` 类，并有 `public` 方法 `drive()`:

    public class Car {
        private String state = "stop";
    
        public void drive(int speed) {
            Assert.isTrue(speed > 0, "speed must be positive");
            this.state = "drive";
            // ...
        }
    }

我们看到 `speed` 必须是正数，上面一行简短的代码用于检测条件，如果失败抛出异常：

    if (!(speed > 0)) {
        throw new IllegalArgumentException("speed must be positive");
    }

每个Assert的方法包含大概类似上面的条件代码块，校验失败抛出运行时异常，应用程序不期望恢复。 

如果我们尝试带负数参数调用 `drive` 方法，会抛出 `IllegalArgumentException` 异常：

    Exception in thread "main" java.lang.IllegalArgumentException: speed must be positive

**逻辑断言**

- `isTrue()`

  上面已经看到示例，其接受布尔条件，如果条件为假抛出 `IllegalArgumentException` 异常。

- `state()`

  该方法与 `isTrue` 一样，但抛出 `IllegalStateException` 异常。

  如名称所示，通常用在因对象的非法状态时，方法不能继续执行。假设骑车运行是不能加油，我们可以使用 `state` 方法断言：

  ```
  public void fuel() {
      Assert.state(this.state.equals("stop"), "car must be stopped");
      // ...
  }
  ```

 当然，我们能使用逻辑断言验证所有场景。但为了更好的可读性，我们可以使用其他的断言，使代码表达性更好。

**对象和类型断言**

- `notNull()`

  通过 `notNull()` 方法可以假设对象不 `null`：

  ```
  public void сhangeOil(String oil) {
      Assert.notNull(oil, "oil mustn't be null");
      // ...
  }
  ```

- `isNull()`

  另外一方面，我们能使用 `isNull()` 方法检查对象为 `null`:

  ```
  public void replaceBattery(CarBattery carBattery) {
      Assert.isNull(
          carBattery.getCharge(), 
          "to replace battery the charge must be null");
      // ...
  }
  ```

- `isInstanceOf()`

  使用 `isInstanceOf()` 方法检查对象必须为另一个特定类型的实例：

  ```
  public void сhangeEngine(Engine engine) {
      Assert.isInstanceOf(ToyotaEngine.class, engine);
      // ...
  }
  ```

  示例中，`ToyotaEngine` 是类 `Engine` 的子类，所以检查通过.

- `isAssignable()`

  使用 `Assert.isAssignable()` 方法检查类型：

  ```
  public void repairEngine(Engine engine) {
      Assert.isAssignable(Engine.class, ToyotaEngine.class);
      // ...
  }
  ```

  这两个断言代表 `is-a` 关系.

**文本断言**

通常用来检查字符串参数。

- `hasLength()`

  如果检查字符串不是空符串，意味着至少包含一个空白，可以使用 `hasLength()` 方法：

  ```
  public void startWithHasLength(String key) {
      Assert.hasLength(key, "key must not be null and must not the empty");
      // ...
  }
  ```

- `hasText()`

  我们能增强检查条件，字符串至少包含一个非空白字符，可以使用 `hasText()` 方法：

  ```
  public void startWithHasText(String key) {
      Assert.hasText(
      key, 
      "key must not be null and must contain at least one non-whitespace  character");
      // ...
  }
  ```

- `doesNotContain()`

  我们能通过 `doesNotContain()` 方法检查参数不包含特定子串：

  ```
  public void startWithNotContain(String key) {
      Assert.doesNotContain(key, "123", "key mustn't contain 123");
      // ...
  }
  ```

**Collection和map断言**

- `Collection` 应用 `notEmpty()`

  如其名称所示，`notEmpty()` 方法断言 `collection` 不空，意味着不是 `null` 并包含至少一个元素：

  ```
  public void repair(Collection<String> repairParts) {
      Assert.notEmpty(
      repairParts, 
      "collection of repairParts mustn't be empty");
      // ...
  }
  ```

- `map` 应用 `notEmpty()`

  同样的方法重载用于 `map`，检查 `map` 不 `null`，并至少包含一个 `entry`（`key，value`键值对）：

  ```
  public void repair(Map<String, String> repairParts) {
      Assert.notEmpty(
      repairParts, 
      "map of repairParts mustn't be empty");
      // ...
  }
  ```

**数组断言**

- `notEmpty()`

  `notEmpty()` 方法可以检查数组不 `null`，且至少包括一个元素：

  ```
  public void repair(String[] repairParts) {
      Assert.notEmpty(
      repairParts, 
      "array of repairParts mustn't be empty");
      // ...
  }
  ```

- `noNullElements()`

  `noNullElements()` 方法确保数组不包含 `null` 元素：

  ```
  public void repairWithNoNull(String[] repairParts) {
      Assert.noNullElements(
      repairParts, 
      "array of repairParts mustn't contain null elements");
      // ...
  }
  ```

注意，如果数组为空检查可以通过，只要没有 `null` 元素。

### 1.3.总结

我们浏览 `Assert` 类，在 `spring` 框架中应用广泛，充分利用它可以很容易写出强壮的代码。



## 2.自定义注解

### 2.1.基本知识

在Java中，注解分为两种，元注解和自定义注解。

很多人误以为自定义注解就是开发者自己定义的，而其它框架提供的不算，但是其实上面我们提到的那几个注解其实都是自定义注解。

关于"元"这个描述，在编程世界里面有都很多，比如"元注解"、"元数据"、"元类"、"元表"等等，这里的"元"其实都是从meta翻译过来的。

一般我们把**元注解理解为描述注解的注解**，**元数据理解为描述数据的数据**，**元类理解为描述类的类**...

所以，在Java中，除了有限的几个固定的"描述注解的注解"以外，所有的注解都是自定义注解。

在JDK中提供了4个标准的用来对注解类型进行注解的注解类（元注解），他们分别是:

```
@Target
@Retention
@Documented
@Inherited
```

除了以上这四个，所有的其他注解全部都是自定义注解。

这里不准备深入介绍以上四个元注解的作用，大家可以自行学习。

本文即将提到的几个例子，都是作者在日常工作中真实使用到的场景，这例子有一个共同点，那就是都用到了Spring的AOP技术。

什么是AOP以及他的用法相信很多人都知道，这里也就不展开介绍了。

### 2.2.使用自定义注解做日志记录

不知道大家有没有遇到过类似的诉求，就是希望在一个方法的入口处或者出口处做统一的日志处理，比如记录一下入参、出参、记录下方法执行的时间等。

如果在每一个方法中自己写这样的代码的话，一方面会有很多代码重复，另外也容易被遗漏。

这种场景，就可以使用自定义注解+切面实现这个功能。

假设我们想要在一些web请求的方法上，记录下本次操作具体做了什么事情，比如新增了一条记录或者删除了一条记录等。

首先我们自定义一个注解：

```
/**
 * Operate Log 的自定义注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLog {

    /**
     * 业务类型，如新增、删除、修改
     *
     * @return
     */
    public OpType opType();

    /**
     * 业务对象名称，如订单、库存、价格
     *
     * @return
     */
    public String opItem();

    /**
     * 业务对象编号表达式，描述了如何获取订单号的表达式
     *
     * @return
     */
    public String opItemIdExpression();
}
```

因为我们不仅要在日志中记录本次操作了什么，还需要知道被操作的对象的具体的唯一性标识，如订单号信息。

但是每一个接口方法的参数类型肯定是不一样的，很难有一个统一的标准，那么我们就可以借助Spel表达式，即在表达式中指明如何获取对应的对象的唯一性标识。

有了上面的注解，接下来就可以写切面了。主要代码如下：

```
/**
 * OpLog的切面处理类，用于通过注解获取日志信息，进行日志记录
 *
 * @author Hollis
 */
@Aspect
@Component
public class OpLogAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpLogAspect.class);

    @Autowired
    HttpServletRequest request;

    @Around("@annotation(com.hollis.annotation.OpLog)")
    public Object log(ProceedingJoinPoint pjp) throws Exception {

        Method method = ((MethodSignature)pjp.getSignature()).getMethod();
        OpLog opLog = method.getAnnotation(OpLog.class);

        Object response = null;

        try {
            // 目标方法执行
            response = pjp.proceed();
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        } 

        if (StringUtils.isNotEmpty(opLog.opItemIdExpression())) {
            SpelExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(opLog.opItemIdExpression());

            EvaluationContext context = new StandardEvaluationContext();
            // 获取参数值
            Object[] args = pjp.getArgs();

            // 获取运行时参数的名称
            LocalVariableTableParameterNameDiscoverer discoverer
                = new LocalVariableTableParameterNameDiscoverer();
            String[] parameterNames = discoverer.getParameterNames(method);

            // 将参数绑定到context中
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 将方法的resp当做变量放到context中，变量名称为该类名转化为小写字母开头的驼峰形式
            if (response != null) {
                context.setVariable(
                    CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, response.getClass().getSimpleName()),
                    response);
            }

            // 解析表达式，获取结果
            String itemId = String.valueOf(expression.getValue(context));

            // 执行日志记录
            handle(opLog.opType(), opLog.opItem(), itemId);
        }

        return response;
    }


    private void handle(OpType opType,  String opItem, String opItemId) {
      // 通过日志打印输出
      LOGGER.info("opType = " + opType.name() +",opItem = " +opItem + ",opItemId = " +opItemId);
    }
}
```

以上切面中，有几个点需要大家注意的：

1、使用 `@Around` 注解来指定对标注了 `OpLog` 的方法设置切面。

 2、使用 `Spel` 的相关方法，通过指定的表示，从对应的参数中获取到目标对象的唯一性标识。 

3、再方法执行成功后，输出日志。

有了以上的切面及注解后，我们只需要在对应的方法上增加注解标注即可，如：

```
@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
@OpLog(opType = OpType.QUERY, opItem = "order", opItemIdExpression = "#id")
public @ResponseBody
HashMap view(@RequestParam(name = "id") String id)
    throws Exception {
}
```

上面这种是入参的参数列表中已经有了被操作的对象的唯一性标识，直接使用`#id`指定即可。

如果被操作的对象的唯一性标识不在入参列表中，那么可能是入参的对象中的某一个属性，用法如下：

```
@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
@OpLog(opType = OpType.QUERY, opItem = "order", opItemIdExpression = "#orderVo.id")
public @ResponseBody
HashMap update(OrderVO orderVo)
    throws Exception {
}
```

以上，即可从入参的 `OrderVO` 对象的 `id` 属性的值获取。

如果我们要记录的唯一性标识，在入参中没有的话，应该怎么办呢？最典型的就是插入方法，插入成功之前，根本不知道主键ID是什么，这种怎么办呢？

我们上面的切面中，做了一件事情，就是我们把方法的返回值也会使用表达式进行一次解析，如果可以解析得到具体的值，可以是可以。如以下写法：

```
 @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
@OpLog(opType = OpType.QUERY, opItem = "order", opItemIdExpression = "#insertResult.id")
public @ResponseBody
InsertResult insert(OrderVO orderVo)
    throws Exception {

    return orderDao.insert(orderVo);
}
```

以上，就是一个简单的使用自定义注解+切面进行日志记录的场景。下面我们再来看一个如何使用注解做方法参数的校验。

### 2.3.使用自定义注解做前置检查

当我们对外部提供接口的时候，会对其中的部分参数有一定的要求，比如某些参数值不能为空等。大多数情况下我们都需要自己主动进行校验，判断对方传入的值是否合理。

这里推荐一个使用 `HibernateValidator` + 自定义注解 + AOP实现参数校验的方式。

首先我们会有一个具体的入参类，定义如下：

```
public class User {
    private String idempotentNo;
    @NotNull(
        message = "userName can't be null"
    )
    private String userName;
}
```

以上，对userName参数注明不能为null。

然后再使用hibernate validator定义一个工具类，用于做参数校验。

```
/**
 * 参数校验工具
 *
 * @author Hollis
 */
public class BeanValidator {

    private static Validator validator = Validation.byProvider(HibernateValidator.class).configure().failFast(true)
        .buildValidatorFactory().getValidator();

    /**
     * @param object object
     * @param groups groups
     */
    public static void validateObject(Object object, Class<?>... groups) throws ValidationException {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (constraintViolations.stream().findFirst().isPresent()) {
            throw new ValidationException(constraintViolations.stream().findFirst().get().getMessage());
        }
    }
}
```

以上代码，会对一个 `bean` 进行校验，一旦失败，就会抛出 `ValidationException`。

接下来定义一个注解：

```
/**
 * facade接口注解， 用于统一对facade进行参数校验及异常捕获
 * <pre>
 *      注意，使用该注解需要注意，该方法的返回值必须是BaseResponse的子类
 * </pre>
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Facade {

}
```

这个注解里面没有任何参数，只用于标注那些方法要进行参数校验。

接下来定义切面：

```
/**
 * Facade的切面处理类，统一统计进行参数校验及异常捕获
 *
 * @author Hollis
 */
@Aspect
@Component
public class FacadeAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacadeAspect.class);

    @Autowired
    HttpServletRequest request;

    @Around("@annotation(com.hollis.annotation.Facade)")
    public Object facade(ProceedingJoinPoint pjp) throws Exception {

        Method method = ((MethodSignature)pjp.getSignature()).getMethod();
        Object[] args = pjp.getArgs();

        Class returnType = ((MethodSignature)pjp.getSignature()).getMethod().getReturnType();

        //循环遍历所有参数，进行参数校验
        for (Object parameter : args) {
            try {
                BeanValidator.validateObject(parameter);
            } catch (ValidationException e) {
                return getFailedResponse(returnType, e);
            }
        }

        try {
            // 目标方法执行
            Object response = pjp.proceed();
            return response;
        } catch (Throwable throwable) {
            return getFailedResponse(returnType, throwable);
        }
    }

    /**
     * 定义并返回一个通用的失败响应
     */
    private Object getFailedResponse(Class returnType, Throwable throwable)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        //如果返回值的类型为BaseResponse 的子类，则创建一个通用的失败响应
        if (returnType.getDeclaredConstructor().newInstance() instanceof BaseResponse) {
            BaseResponse response = (BaseResponse)returnType.getDeclaredConstructor().newInstance();
            response.setSuccess(false);
            response.setResponseMessage(throwable.toString());
            response.setResponseCode(GlobalConstant.BIZ_ERROR);
            return response;
        }

        LOGGER.error(
            "failed to getFailedResponse , returnType (" + returnType + ") is not instanceof BaseResponse");
        return null;
    }
}
```

以上代码，和前面的切面有点类似，主要是定义了一个切面，会对所有标注 `@Facade` 的方法进行统一处理，即在开始方法调用前进行参数校验，一旦校验失败，则返回一个固定的失败的 `Response`，特别需要注意的是，这里之所以可以返回一个固定的 `BaseResponse`，是因为我们会要求我们的所有对外提供的接口的 `response` 必须继承 `BaseResponse` 类，这个类里面会定义一些默认的参数，如错误码等。

之后，只需要对需要参数校验的方法增加对应注解即可：

```
@Facade
public TestResponse query(User user) {

}
```

这样，有了以上注解和切面，我们就可以对所有的对外方法做统一的控制了。

其实，以上这个 `facadeAspect` 我省略了很多东西，我们真正使用的那个切面，不仅仅做了参数检查，还可以做很多其他事情。比如异常的统一处理、错误码的统一转换、记录方法执行时长、记录方法的入参出参等等。

总之，使用切面+自定义注解，我们可以统一做很多事情。除了以上的这几个场景，我们还有很多相似的用法，比如：

统一的缓存处理。如某些操作需要在操作前查缓存、操作后更新缓存。这种就可以通过自定义注解+切面的方式统一处理。

代码其实都差不多，思路也比较简单，就是通过自定义注解来标注需要被切面处理的累或者方法，然后在切面中对方法的执行过程进行干预，比如在执行前或者执行后做一些特殊的操作。

使用这种方式可以大大减少重复代码，大大提升代码的优雅性，方便我们使用。

但是同时也不能过度使用，因为注解看似简单，但是其实内部有很多逻辑是容易被忽略的。但是快快在你的项目中用起来吧。

## 3.数据校验

### 3.1.什么是 JSR-303？

`JSR-303` 是 `JAVA EE 6` 中的一项子规范，叫做 `Bean Validation`。

`Bean Validation` 为 `JavaBean` 验证定义了相应的`元数据模型`和`API`。缺省的元数据是`Java Annotations`，通过使用 `XML` 可以对原有的元数据信息进行覆盖和扩展。在应用程序中，通过使用`Bean Validation` 或是你自己定义的 `constraint`，例如 `@NotNull`, `@Max`, `@ZipCode` ， 就可以确保数据模型（`JavaBean`）的正确性。`constraint` 可以附加到字段，`getter` 方法，类或者接口上面。对于一些特定的需求，用户可以很容易的开发定制化的 `constraint`。`Bean Validation` 是一个运行时的数据验证框架，在验证之后验证的错误信息会被马上返回。

### 3.2.添加依赖

`Spring Boot` 整合 `JSR-303` 只需要添加一个`starter`即可，如下：

```
<dependency>
    <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 3.3.内嵌的注解有哪些？

`Bean Validation` 内嵌的注解很多，基本实际开发中已经够用了，注解如下：

| 注解                        | 详细信息                                                 |
| :-------------------------- | :------------------------------------------------------- |
| @Null                       | 被注释的元素必须为 null                                  |
| @NotNull                    | 被注释的元素必须不为 null                                |
| @AssertTrue                 | 被注释的元素必须为 true                                  |
| @AssertFalse                | 被注释的元素必须为 false                                 |
| @Min(value)                 | 被注释的元素必须是一个数字，其值必须大于等于指定的最小值 |
| @Max(value)                 | 被注释的元素必须是一个数字，其值必须小于等于指定的最大值 |
| @DecimalMin(value)          | 被注释的元素必须是一个数字，其值必须大于等于指定的最小值 |
| @DecimalMax(value)          | 被注释的元素必须是一个数字，其值必须小于等于指定的最大值 |
| @Size(max, min)             | 被注释的元素的大小必须在指定的范围内                     |
| @Digits (integer, fraction) | 被注释的元素必须是一个数字，其值必须在可接受的范围内     |
| @Past                       | 被注释的元素必须是一个过去的日期                         |
| @Future                     | 被注释的元素必须是一个将来的日期                         |
| @Pattern(value)             | 被注释的元素必须符合指定的正则表达式                     |

> 以上是`Bean Validation`的内嵌的注解，但是`Hibernate Validator`在原有的基础上也内嵌了几个注解，如下。

| 注解      | 详细信息                               |
| :-------- | :------------------------------------- |
| @Email    | 被注释的元素必须是电子邮箱地址         |
| @Length   | 被注释的字符串的大小必须在指定的范围内 |
| @NotEmpty | 被注释的字符串的必须非空               |
| @Range    | 被注释的元素必须在合适的范围内         |

### 3.4.如何使用？

参数校验分为**简单校验**、**嵌套校验**、**分组校验**。

#### 3.4.1.简单校验

简单的校验即是没有嵌套属性，直接在需要的元素上标注约束注解即可。如下：

```
@Data
public class ArticleDTO {

    @NotNull(message = "文章id不能为空")
    @Min(value = 1,message = "文章ID不能为负数")
    private Integer id;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    @NotBlank(message = "作者Id不能为空")
    private String authorId;

    @Future(message = "提交时间不能为过去时间")
    private Date submitTime;
}
```

> 同一个属性可以指定多个约束，比如`@NotNull`和`@MAX`,其中的`message`属性指定了约束条件不满足时的提示信息。

以上约束标记完成之后，要想完成校验，需要在`controller`层的接口标注`@Valid`注解以及声明一个`BindingResult`类型的参数来接收校验的结果。

下面简单的演示下添加文章的接口，如下：

```
/**
     * 添加文章
     */
    @PostMapping("/add")
    public String add(@Valid @RequestBody ArticleDTO articleDTO, BindingResult bindingResult) throws JsonProcessingException {
        //如果有错误提示信息
        if (bindingResult.hasErrors()) {
            Map<String , String> map = new HashMap<>();
            bindingResult.getFieldErrors().forEach( (item) -> {
                String message = item.getDefaultMessage();
                String field = item.getField();
                map.put( field , message );
            } );
            //返回提示信息
            return objectMapper.writeValueAsString(map);
        }
        return "success";
    }
```

> 仅仅在属性上添加了约束注解还不行，还需在接口参数上标注`@Valid`注解并且声明一个`BindingResult`类型的参数来接收校验结果。

#### 3.4.2.分组校验

举个栗子：上传文章不需要传文章`ID`，但是修改文章需要上传文章`ID`，并且用的都是同一个`DTO`接收参数，此时的约束条件该如何写呢？

此时就需要对这个文章`ID`进行分组校验，上传文章接口是一个分组，不需要执行`@NotNull`校验，修改文章的接口是一个分组，需要执行`@NotNull`的校验。

> 所有的校验注解都有一个`groups`属性用来指定分组，`Class<?>[]`类型，没有实际意义，因此只需要定义一个或者多个接口用来区分即可。

```
@Data
public class ArticleDTO {

    /**
     * 文章ID只在修改的时候需要检验，因此指定groups为修改的分组
     */
    @NotNull(message = "文章id不能为空",groups = UpdateArticleDTO.class )
    @Min(value = 1,message = "文章ID不能为负数",groups = UpdateArticleDTO.class)
    private Integer id;

    /**
     * 文章内容添加和修改都是必须校验的，groups需要指定两个分组
     */
    @NotBlank(message = "文章内容不能为空",groups = {AddArticleDTO.class,UpdateArticleDTO.class})
    private String content;

    @NotBlank(message = "作者Id不能为空",groups = AddArticleDTO.class)
    private String authorId;

    /**
     * 提交时间是添加和修改都需要校验的，因此指定groups两个
     */
    @Future(message = "提交时间不能为过去时间",groups = {AddArticleDTO.class,UpdateArticleDTO.class})
    private Date submitTime;
    
    //修改文章的分组
    public interface UpdateArticleDTO{}

    //添加文章的分组
    public interface AddArticleDTO{}

}
```

> JSR303本身的`@Valid`并不支持分组校验，但是Spring在其基础提供了一个注解`@Validated`支持分组校验。`@Validated`这个注解`value`属性指定需要校验的分组。

```
/**
     * 添加文章
     * @Validated：这个注解指定校验的分组信息
     */
    @PostMapping("/add")
    public String add(@Validated(value = ArticleDTO.AddArticleDTO.class) @RequestBody ArticleDTO articleDTO, BindingResult bindingResult) throws JsonProcessingException {
        //如果有错误提示信息
        if (bindingResult.hasErrors()) {
            Map<String , String> map = new HashMap<>();
            bindingResult.getFieldErrors().forEach( (item) -> {
                String message = item.getDefaultMessage();
                String field = item.getField();
                map.put( field , message );
            } );
            //返回提示信息
            return objectMapper.writeValueAsString(map);
        }
        return "success";
    }
```

#### 3.4.3.嵌套校验

嵌套校验简单的解释就是一个实体中包含另外一个实体，并且这两个或者多个实体都需要校验。

举个栗子：文章可以有一个或者多个分类，作者在提交文章的时候必须指定文章分类，而分类是单独一个实体，有`分类ID`、`名称`等等。大致的结构如下：

```
public class ArticleDTO{
  ...文章的一些属性.....
  
  //分类的信息
  private CategoryDTO categoryDTO;
}
```

此时文章和分类的属性都需要校验，这种就叫做嵌套校验。

> 嵌套校验很简单，只需要在嵌套的实体属性标注`@Valid`注解，则其中的属性也将会得到校验，否则不会校验。

如下**文章分类实体类校验**：

```
/**
 * 文章分类
 */
@Data
public class CategoryDTO {
    @NotNull(message = "分类ID不能为空")
    @Min(value = 1,message = "分类ID不能为负数")
    private Integer id;

    @NotBlank(message = "分类名称不能为空")
    private String name;
}
```

文章的实体类中有个嵌套的文章分类`CategoryDTO`属性，需要使用`@Valid`标注才能嵌套校验，如下：

```
@Data
public class ArticleDTO {

    @NotBlank(message = "文章内容不能为空")
    private String content;

    @NotBlank(message = "作者Id不能为空")
    private String authorId;

    @Future(message = "提交时间不能为过去时间")
    private Date submitTime;

    /**
     * @Valid这个注解指定CategoryDTO中的属性也需要校验
     */
    @Valid
    @NotNull(message = "分类不能为空")
    private CategoryDTO categoryDTO;
  }
```

`Controller`层的添加文章的接口同上，需要使用`@Valid`或者`@Validated`标注入参，同时需要定义一个`BindingResult`的参数接收校验结果。

> 嵌套校验针对**分组查询**仍然生效，如果嵌套的实体类（比如`CategoryDTO`）中的校验的属性和接口中`@Validated`注解指定的分组不同，则不会校验。

`JSR-303`针对`集合`的嵌套校验也是可行的，比如`List`的嵌套校验，同样需要在属性上标注一个`@Valid`注解才会生效，如下：

```
@Data
public class ArticleDTO {
    /**
     * @Valid这个注解标注在集合上，将会针对集合中每个元素进行校验
     */
    @Valid
    @Size(min = 1,message = "至少一个分类")
    @NotNull(message = "分类不能为空")
    private List<CategoryDTO> categoryDTOS;
  }
```

> 总结：嵌套校验只需要在需要校验的元素（单个或者集合）上添加`@Valid`注解，接口层需要使用`@Valid`或者`@Validated`注解标注入参。

### 3.5.如何接收校验结果？

接收校验的结果的方式很多，不过实际开发中最好选择一个优雅的方式，下面介绍常见的两种方式。

#### 3.5.1.BindingResult 接收

这种方式需要在 `Controller` 层的每个接口方法参数中指定，`Validator` 会将校验的信息自动封装到其中。这也是上面例子中一直用的方式。如下：

```
@PostMapping("/add")
public String add(@Valid @RequestBody ArticleDTO articleDTO, BindingResult bindingResult){}
```

这种方式的弊端很明显，每个接口方法参数都要声明，同时每个方法都要处理校验信息，显然不现实，舍弃。

> 此种方式还有一个优化的方案：使用`AOP`，在`Controller`接口方法执行之前处理`BindingResult`的消息提示，不过这种方案仍然**不推荐使用**。

#### 3.5.2.全局异常捕捉

参数在校验失败的时候会抛出的`MethodArgumentNotValidException`或者`BindException`两种异常，可以在全局的异常处理器中捕捉到这两种异常，将提示信息或者自定义信息返回给客户端。

作者这里就不再详细的贴出其他的异常捕获了，仅仅贴一下参数校验的异常捕获（**仅仅举个例子，具体的返回信息需要自己封装**），如下：

```
@RestControllerAdvice
public class ExceptionRsHandler {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 参数校验异常步骤
     */
    @ExceptionHandler(value= {MethodArgumentNotValidException.class , BindException.class})
    public String onException(Exception e) throws JsonProcessingException {
        BindingResult bindingResult = null;
        if (e instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException)e).getBindingResult();
        } else if (e instanceof BindException) {
            bindingResult = ((BindException)e).getBindingResult();
        }
        Map<String,String> errorMap = new HashMap<>(16);
        bindingResult.getFieldErrors().forEach((fieldError)->
                errorMap.put(fieldError.getField(),fieldError.getDefaultMessage())
        );
        return objectMapper.writeValueAsString(errorMap);
    }

}
```

### 3.6.spring-boot-starter-validation做了什么？

这个启动器的自动配置类是`ValidationAutoConfiguration`，最重要的代码就是注入了一个`Validator`（校验器）的实现类，代码如下：

```
@Bean
 @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
 @ConditionalOnMissingBean(Validator.class)
 public static LocalValidatorFactoryBean defaultValidator() {
  LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
  MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory();
  factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
  return factoryBean;
 }
```

这个有什么用呢？`Validator`这个接口定义了校验的方法，如下：

```
<T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups);


<T> Set<ConstraintViolation<T>> validateProperty(T object,
              String propertyName,
              Class<?>... groups);
                           
<T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType,
              String propertyName,
              Object value,
              Class<?>... groups);
......
```

> 这个`Validator`可以用来自定义实现自己的校验逻辑，有些大公司完全不用JSR-303提供的`@Valid`注解，而是有一套自己的实现，其实本质就是利用`Validator`这个接口的实现。

### 3.7.如何自定义校验？

虽说在日常的开发中内置的约束注解已经够用了，但是仍然有些时候不能满足需求，需要自定义一些校验约束。

**举个栗子：有这样一个例子，传入的数字要在列举的值范围中，否则校验失败。**

#### 3.7.1.自定义校验注解

首先需要自定义一个校验注解，如下：

```
@Documented
@Constraint(validatedBy = { EnumValuesConstraintValidator.class})
@Target({ METHOD, FIELD, ANNOTATION_TYPE })
@Retention(RUNTIME)
@NotNull(message = "不能为空")
public @interface EnumValues {
    /**
     * 提示消息
     */
    String message() default "传入的值不在范围内";

    /**
     * 分组
     * @return
     */
    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    /**
     * 可以传入的值
     * @return
     */
    int[] values() default { };
}
```

根据`Bean Validation API` 规范的要求有如下三个属性是必须的：

1. `message`：定义消息模板，校验失败时输出
2. `groups`：用于校验分组
3. `payload`：`Bean Validation API` 的使用者可以通过此属性来给约束条件指定严重级别. 这个属性并不被API自身所使用。

除了以上三个必须要的属性，添加了一个`values`属性用来接收限制的范围。

该校验注解头上标注的如下一行代码：

```
@Constraint(validatedBy = { EnumValuesConstraintValidator.class})
```

这个`@Constraint`注解指定了通过哪个校验器去校验。

> 自定义校验注解可以复用内嵌的注解，比如`@EnumValues`注解头上标注了一个`@NotNull`注解，这样`@EnumValues`就兼具了`@NotNull`的功能。

#### 3.7.2.自定义校验器

`@Constraint`注解指定了校验器为`EnumValuesConstraintValidator`，因此需要自定义一个。

自定义校验器需要实现`ConstraintValidator<A extends Annotation, T>`这个接口，第一个泛型是`校验注解`，第二个是`参数类型`。代码如下：

```
/**
 * 校验器
 */
public class EnumValuesConstraintValidator implements ConstraintValidator<EnumValues,Integer> {
    /**
     * 存储枚举的值
     */
    private  Set<Integer> ints=new HashSet<>();

    /**
     * 初始化方法
     * @param enumValues 校验的注解
     */
    @Override
    public void initialize(EnumValues enumValues) {
        for (int value : enumValues.values()) {
            ints.add(value);
        }
    }

    /**
     *
     * @param value  入参传的值
     * @param context
     * @return
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        //判断是否包含这个值
        return ints.contains(value);
    }
}
```

> 如果约束注解需要对其他数据类型进行校验，则可以的自定义对应数据类型的校验器，然后在约束注解头上的`@Constraint`注解中指定其他的校验器。

#### 3.7.3.演示

校验注解和校验器自定义成功之后即可使用，如下：

```
@Data
public class AuthorDTO {
    @EnumValues(values = {1,2},message = "性别只能传入1或者2")
    private Integer gender;
}
```

### 3.8.总结

数据校验作为客户端和服务端的一道屏障，有着重要的作用，通过这篇文章希望能够对`JSR-303`数据校验有着全面的认识。



## 4.Spring 中的重试机制

### 4.1.概要

`Spring` 实现了一套重试机制，功能简单实用。`Spring Retry` 是从 `Spring Batch` 独立出来的一个功能，已经广泛应用于 `Spring Batch`,`Spring Integration`, `Spring for Apache Hadoop` 等 `Spring` 项目。本文将讲述如何使用 `Spring Retry` 及其实现原理。

### 4.2.背景

重试，其实我们其实很多时候都需要的，为了保证容错性，可用性，一致性等。一般用来应对外部系统的一些不可预料的返回、异常等，特别是网络延迟，中断等情况。还有在现在流行的微服务治理框架中，通常都有自己的重试与超时配置，比如 `dubbo` 可以设置`retries=1，timeout=500` 调用失败只重试1次，超过 `500ms` 调用仍未返回则调用失败。如果我们要做重试，要为特定的某个操作做重试功能，则要硬编码，大概逻辑基本都是写个循环，根据返回或异常，计数失败次数，然后设定退出条件。这样做，且不说每个操作都要写这种类似的代码，而且重试逻辑和业务逻辑混在一起，给维护和扩展带来了麻烦。从面向对象的角度来看，我们应该把重试的代码独立出来。

### 4.3.使用介绍

#### 4.3.1.基本使用

先举个例子：

```
@Configuration
@EnableRetry
public class Application {

    @Bean
    public RetryService retryService(){
        return new RetryService();
    }

    public static void main(String[] args) throws Exception{
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("springretry");
        RetryService service1 = applicationContext.getBean("service", RetryService.class);
        service1.service();
    }
}

@Service("service")
public class RetryService {

    @Retryable(value = IllegalAccessException.class, maxAttempts = 5,
            backoff= @Backoff(value = 1500, maxDelay = 100000, multiplier = 1.2))
    public void service() throws IllegalAccessException {
        System.out.println("service method...");
        throw new IllegalAccessException("manual exception");
    }

    @Recover
    public void recover(IllegalAccessException e){
        System.out.println("service retry after Recover => " + e.getMessage());
    }

}
```

- `@EnableRetry` - 表示开启重试机制

- `@Retryable` - 表示这个方法需要重试，它有很丰富的参数，可以满足你对重试的需求

- `@Backoff` - 表示重试中的退避策略 @Recover - 兜底方法，即多次重试后还是失败就会执行这个方法

`Spring-Retry` 的功能丰富在于其重试策略和退避策略，还有兜底，监听器等操作。

然后每个注解里面的参数，都是很简单的，大家看一下就知道是什么意思，怎么用了，我就不多讲了。

#### 4.3.2.重试策略

看一下 `Spring Retry` 自带的一些重试策略，主要是用来判断当方法调用异常时是否需要重试。（下文原理部分会深入分析实现）

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504232333.webp)

- `SimpleRetryPolicy` 默认最多重试3次
- `TimeoutRetryPolicy` 默认在1秒内失败都会重试
- `ExpressionRetryPolicy` 符合表达式就会重试
- `CircuitBreakerRetryPolicy` 增加了熔断的机制，如果不在熔断状态，则允许重试
- `CompositeRetryPolicy` 可以组合多个重试策略
- `NeverRetryPolicy 从不重试`（也是一种重试策略哈）
- `AlwaysRetryPolicy` 总是重试

….等等

#### 4.3.3.退避策略

看一下退避策略，退避是指怎么去做下一次的重试，在这里其实就是等待多长时间。（下文原理部分会深入分析实现）

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504232338.webp)

- `FixedBackOffPolicy` 默认固定延迟1秒后执行下一次重试
- `ExponentialBackOffPolicy` 指数递增延迟执行重试，默认初始0.1秒，系数是2，那么下次延迟0.2秒，再下次就是延迟0.4秒，如此类推，最大30秒。
- `ExponentialRandomBackOffPolicy` 在上面那个策略上增加随机性
- `UniformRandomBackOffPolicy` 这个跟上面的区别就是，上面的延迟会不停递增，这个只会在固定的区间随机
- `StatelessBackOffPolicy` 这个说明是无状态的，所谓无状态就是对上次的退避无感知，从它下面的子类也能看出来

### 4.4.原理

原理部分我想分开两部分来讲，一是重试机制的切入点，即它是如何使得你的代码实现重试功能的；二是重试机制的详细，包括重试的逻辑以及重试策略和退避策略的实现。

#### 4.4.1.切入点

##### 4.4.1.1.@EnableRetry

```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Import(RetryConfiguration.class)
@Documented
public @interface EnableRetry {

    /**
    * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
    * to standard Java interface-based proxies. The default is {@code false}.
    *
    * @return whether to proxy or not to proxy the class
    */
    boolean proxyTargetClass() default false;

}
```

我们可以看到`@EnableAspectJAutoProxy(proxyTargetClass = false)`这个并不陌生，就是打开Spring AOP功能。重点看看`@Import(RetryConfiguration.class)`@Import相当于注册这个Bean

我们看看这个`RetryConfiguration`是个什么东西

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504232344.webp)

它是一个 `AbstractPointcutAdvisor`，它有一个 `pointcut` 和一个 `advice`。我们知道，在 `IOC` 过程中会根据 `PointcutAdvisor` 类来对 `Bean` 进行 `Pointcut` 的过滤，然后生成对应的 `AOP` 代理类，用 `advice` 来加强处理。看看 `RetryConfiguration` 的初始化:

```
@PostConstruct
public void init() {
    Set<Class<? extends Annotation>> retryableAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(1);
    retryableAnnotationTypes.add(Retryable.class);
    //创建pointcut
    this.pointcut = buildPointcut(retryableAnnotationTypes);
    //创建advice
    this.advice = buildAdvice();
    if (this.advice instanceof BeanFactoryAware) {
        ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
    }
}
protected Pointcut buildPointcut(Set<Class<? extends Annotation>> retryAnnotationTypes) {
    ComposablePointcut result = null;
    for (Class<? extends Annotation> retryAnnotationType : retryAnnotationTypes) {
        Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
        if (result == null) {
        	result = new ComposablePointcut(filter);
        }
        else {
        	result.union(filter);
        }
    }
    return result;
}
```

上面代码用到了 `AnnotationClassOrMethodPointcut`，其实它最终还是用到了 `AnnotationMethodMatcher` 来根据注解进行切入点的过滤。这里就是 `@Retryable` 注解了。

```
//创建advice对象，即拦截器
protected Advice buildAdvice() {
    //下面关注这个对象
    AnnotationAwareRetryOperationsInterceptor interceptor = new AnnotationAwareRetryOperationsInterceptor();
    if (retryContextCache != null) {
    	interceptor.setRetryContextCache(retryContextCache);
    }
    if (retryListeners != null) {
    	interceptor.setListeners(retryListeners);
    }
    if (methodArgumentsKeyGenerator != null) {
    	interceptor.setKeyGenerator(methodArgumentsKeyGenerator);
    }
    if (newMethodArgumentsIdentifier != null) {
    	interceptor.setNewItemIdentifier(newMethodArgumentsIdentifier);
    }
    if (sleeper != null) {
        interceptor.setSleeper(sleeper);
    }
    return interceptor;
}
```

##### 4.4.1.2.AnnotationAwareRetryOperationsInterceptor

 可以看出AnnotationAwareRetryOperationsInterceptor是一个MethodInterceptor，在创建AOP代理过程中如果目标方法符合pointcut的规则，它就会加到interceptor列表中，然后做增强，我们看看invoke方法做了什么增强。

```
@Override
 public Object invoke(MethodInvocation invocation) throws Throwable {
  MethodInterceptor delegate = getDelegate(invocation.getThis(), invocation.getMethod());
  if (delegate != null) {
   return delegate.invoke(invocation);
  }
  else {
   return invocation.proceed();
  }
 }
```

这里用到了委托，主要是需要根据配置委托给具体“有状态”的 `interceptor` 还是“无状态”的 `interceptor`。

```
private MethodInterceptor getDelegate(Object target, Method method) {
    if (!this.delegates.containsKey(target) || !this.delegates.get(target).containsKey(method)) {
        synchronized (this.delegates) {
            if (!this.delegates.containsKey(target)) {
                this.delegates.put(target, new HashMap<Method, MethodInterceptor>());
            }
            Map<Method, MethodInterceptor> delegatesForTarget = this.delegates.get(target);
            if (!delegatesForTarget.containsKey(method)) {
                Retryable retryable = AnnotationUtils.findAnnotation(method, Retryable.class);
                if (retryable == null) {
                    retryable = AnnotationUtils.findAnnotation(method.getDeclaringClass(), Retryable.class);
                }
                if (retryable == null) {
                    retryable = findAnnotationOnTarget(target, method);
                }
                if (retryable == null) {
                    return delegatesForTarget.put(method, null);
                }
                MethodInterceptor delegate;
                //支持自定义MethodInterceptor，而且优先级最高
                if (StringUtils.hasText(retryable.interceptor())) {
                    delegate = this.beanFactory.getBean(retryable.interceptor(), MethodInterceptor.class);
                }
                else if (retryable.stateful()) {
                    //得到“有状态”的interceptor
                    delegate = getStatefulInterceptor(target, method, retryable);
                }
                else {
                    //得到“无状态”的interceptor
                    delegate = getStatelessInterceptor(target, method, retryable);
                }
                delegatesForTarget.put(method, delegate);
            }
        }
    }
    return this.delegates.get(target).get(method);
}
```

`getStatefulInterceptor` 和 `getStatelessInterceptor` 都是差不多，我们先看看比较简单的 `getStatelessInterceptor`。

```
private MethodInterceptor getStatelessInterceptor(Object target, Method method, Retryable retryable) {
    //生成一个RetryTemplate
    RetryTemplate template = createTemplate(retryable.listeners());
    //生成retryPolicy
    template.setRetryPolicy(getRetryPolicy(retryable));
    //生成backoffPolicy
    template.setBackOffPolicy(getBackoffPolicy(retryable.backoff()));
    return RetryInterceptorBuilder.stateless()
        .retryOperations(template)
        .label(retryable.label())
        .recoverer(getRecoverer(target, method))
        .build();
}
```

具体生成 `retryPolicy` 和 `backoffPolicy` 的规则，我们等下再回头来看。`RetryInterceptorBuilder` 其实就是为了生成`RetryOperationsInterceptor`。`RetryOperationsInterceptor` 也是一个 `MethodInterceptor`，我们来看看它的`invoke`方法。

```
public Object invoke(final MethodInvocation invocation) throws Throwable {

    String name;
    if (StringUtils.hasText(label)) {
        name = label;
    } else {
        name = invocation.getMethod().toGenericString();
    }
    final String label = name;

    //定义了一个RetryCallback，其实看它的doWithRetry方法，调用了invocation的proceed()方法，是不是有点眼熟，这就是AOP的拦截链调用，如果没有拦截链，那就是对原来方法的调用。
    RetryCallback<Object, Throwable> retryCallback = new RetryCallback<Object, Throwable>() {

        public Object doWithRetry(RetryContext context) throws Exception {

            context.setAttribute(RetryContext.NAME, label);

            /*
            * If we don't copy the invocation carefully it won't keep a reference to
            * the other interceptors in the chain. We don't have a choice here but to
            * specialise to ReflectiveMethodInvocation (but how often would another
            * implementation come along?).
            */
            if (invocation instanceof ProxyMethodInvocation) {
                try {
                    return ((ProxyMethodInvocation) invocation).invocableClone().proceed();
                }
                catch (Exception e) {
                    throw e;
                }
                catch (Error e) {
                    throw e;
                }
                catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
            else {
                throw new IllegalStateException(
                    "MethodInvocation of the wrong type detected - this should not happen with Spring AOP, " +
                    "so please raise an issue if you see this exception");
            }
        }

    };

    if (recoverer != null) {
        ItemRecovererCallback recoveryCallback = new ItemRecovererCallback(
            invocation.getArguments(), recoverer);
        return this.retryOperations.execute(retryCallback, recoveryCallback);
    }
    //最终还是进入到retryOperations的execute方法，这个retryOperations就是在之前的builder set进来的RetryTemplate。
    return this.retryOperations.execute(retryCallback);

}
```

无论是`RetryOperationsInterceptor`还是`StatefulRetryOperationsInterceptor`，最终的拦截处理逻辑还是调用到 `RetryTemplate` 的 `execute` 方法，从名字也看出来，`RetryTemplate` 作为一个模板类，里面包含了重试统一逻辑。不过，我看这个`RetryTemplate` 并不是很“模板”，因为它没有很多可以扩展的地方。

#### 4.4.2.重试逻辑及策略实现

上面介绍了 `Spring Retry` 利用了AOP代理使重试机制对业务代码进行“入侵”。下面我们继续看看重试的逻辑做了什么。`RetryTemplate` 的 `doExecute` 方法。

```
protected <T, E extends Throwable> T doExecute(RetryCallback<T, E> retryCallback,
RecoveryCallback<T> recoveryCallback, RetryState state)
throws E, ExhaustedRetryException {

    RetryPolicy retryPolicy = this.retryPolicy;
    BackOffPolicy backOffPolicy = this.backOffPolicy;

    //新建一个RetryContext来保存本轮重试的上下文
    RetryContext context = open(retryPolicy, state);
    if (this.logger.isTraceEnabled()) {
        this.logger.trace("RetryContext retrieved: " + context);
    }

    // Make sure the context is available globally for clients who need
    // it...
    RetrySynchronizationManager.register(context);

    Throwable lastException = null;

    boolean exhausted = false;
    try {

        //如果有注册RetryListener，则会调用它的open方法，给调用者一个通知。
        boolean running = doOpenInterceptors(retryCallback, context);

        if (!running) {
            throw new TerminatedRetryException(
            "Retry terminated abnormally by interceptor before first attempt");
        }

        // Get or Start the backoff context...
        BackOffContext backOffContext = null;
        Object resource = context.getAttribute("backOffContext");

        if (resource instanceof BackOffContext) {
            backOffContext = (BackOffContext) resource;
        }

        if (backOffContext == null) {
            backOffContext = backOffPolicy.start(context);
            if (backOffContext != null) {
                context.setAttribute("backOffContext", backOffContext);
            }
        }

        //判断能否重试，就是调用RetryPolicy的canRetry方法来判断。
        //这个循环会直到原方法不抛出异常，或不需要再重试
        while (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {

        try {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Retry: count=" + context.getRetryCount());
            }
            //清除上次记录的异常
            lastException = null;
            //doWithRetry方法，一般来说就是原方法
            return retryCallback.doWithRetry(context);
        }
        catch (Throwable e) {
            //原方法抛出了异常
            lastException = e;

            try {
                //记录异常信息
                registerThrowable(retryPolicy, state, context, e);
            }
            catch (Exception ex) {
                throw new TerminatedRetryException("Could not register throwable",ex);
            }
            finally {
                //调用RetryListener的onError方法
                doOnErrorInterceptors(retryCallback, context, e);
            }
            //再次判断能否重试
            if (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {
                try {
                    //如果可以重试则走退避策略
                    backOffPolicy.backOff(backOffContext);
                }
                catch (BackOffInterruptedException ex) {
                    lastException = e;
                    // back off was prevented by another thread - fail the retry
                    if (this.logger.isDebugEnabled()) {
                        this.logger
                        .debug("Abort retry because interrupted: count="
                        + context.getRetryCount());
                    }
                    throw ex;
                }
            }

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Checking for rethrow: count=" + context.getRetryCount());
            }

            if (shouldRethrow(retryPolicy, context, state)) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Rethrow in retry for policy: count=" + context.getRetryCount());
                }
                throw RetryTemplate.<E>wrapIfNecessary(e);
            }

        }

        /*
        * A stateful attempt that can retry may rethrow the exception before now,
        * but if we get this far in a stateful retry there's a reason for it,
        * like a circuit breaker or a rollback classifier.
        */
        if (state != null && context.hasAttribute(GLOBAL_STATE)) {
        break;
        }
        }

        if (state == null && this.logger.isDebugEnabled()) {
        this.logger.debug(
        "Retry failed last attempt: count=" + context.getRetryCount());
        }

        exhausted = true;
        //重试结束后如果有兜底Recovery方法则执行，否则抛异常
        return handleRetryExhausted(recoveryCallback, context, state);

    } catch (Throwable e) {
        throw RetryTemplate.<E>wrapIfNecessary(e);
    } finally {
        //处理一些关闭逻辑
        close(retryPolicy, context, state, lastException == null || exhausted);
        //调用RetryListener的close方法
        doCloseInterceptors(retryCallback, context, lastException);
        RetrySynchronizationManager.clear();
    }
}
```

主要核心重试逻辑就是上面的代码了，看上去还是挺简单的。在上面，我们漏掉了 `RetryPolicy` 的 `canRetry` 方法和 `BackOffPolicy` 的 `backOff` 方法，以及这两个 `Policy` 是怎么来的。我们回头看看`getStatelessInterceptor`方法中的`getRetryPolicy`和`getRetryPolicy`方法。

```
private RetryPolicy getRetryPolicy(Annotation retryable) {
    Map<String, Object> attrs = AnnotationUtils.getAnnotationAttributes(retryable);
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] includes = (Class<? extends Throwable>[]) attrs.get("value");
    String exceptionExpression = (String) attrs.get("exceptionExpression");
    boolean hasExpression = StringUtils.hasText(exceptionExpression);
    if (includes.length == 0) {
        @SuppressWarnings("unchecked")
        Class<? extends Throwable>[] value = (Class<? extends Throwable>[]) attrs.get("include");
        includes = value;
    }
    @SuppressWarnings("unchecked")
    Class<? extends Throwable>[] excludes = (Class<? extends Throwable>[]) attrs.get("exclude");
    Integer maxAttempts = (Integer) attrs.get("maxAttempts");
    String maxAttemptsExpression = (String) attrs.get("maxAttemptsExpression");
    if (StringUtils.hasText(maxAttemptsExpression)) {
    	maxAttempts = PARSER.parseExpression(resolve(maxAttemptsExpression), PARSER_CONTEXT).getValue(this.evaluationContext, Integer.class);
    }
    if (includes.length == 0 && excludes.length == 0) {
    	SimpleRetryPolicy simple = hasExpression ? new ExpressionRetryPolicy(resolve(exceptionExpression)).withBeanFactory(this.beanFactory) : new SimpleRetryPolicy();
    	simple.setMaxAttempts(maxAttempts);
    	return simple;
    }
    Map<Class<? extends Throwable>, Boolean> policyMap = new HashMap<Class<? extends Throwable>, Boolean>();
    for (Class<? extends Throwable> type : includes) {
    	policyMap.put(type, true);
    }
    for (Class<? extends Throwable> type : excludes) {
    	policyMap.put(type, false);
    }
    boolean retryNotExcluded = includes.length == 0;
    if (hasExpression) {
    	return new ExpressionRetryPolicy(maxAttempts, policyMap, true, exceptionExpression, retryNotExcluded).withBeanFactory(this.beanFactory);
    }
    else {
    	return new SimpleRetryPolicy(maxAttempts, policyMap, true, retryNotExcluded);
    }
}
```

嗯～，代码不难，这里简单做一下总结好了。就是通过 `@Retryable` 注解中的参数，来判断具体使用文章开头说到的哪个重试策略，是`SimpleRetryPolicy` 还是 `ExpressionRetryPolicy` 等。

```
private BackOffPolicy getBackoffPolicy(Backoff backoff) {
    long min = backoff.delay() == 0 ? backoff.value() : backoff.delay();
    if (StringUtils.hasText(backoff.delayExpression())) {
        min = PARSER.parseExpression(resolve(backoff.delayExpression()), PARSER_CONTEXT).getValue(this.evaluationContext, Long.class);
    }
    long max = backoff.maxDelay();
    if (StringUtils.hasText(backoff.maxDelayExpression())) {
   		max = PARSER.parseExpression(resolve(backoff.maxDelayExpression()), PARSER_CONTEXT).getValue(this.evaluationContext, Long.class);
    }
    double multiplier = backoff.multiplier();
    if (StringUtils.hasText(backoff.multiplierExpression())) {
    	multiplier = PARSER.parseExpression(resolve(backoff.multiplierExpression()), PARSER_CONTEXT).getValue(this.evaluationContext, Double.class);
    }
    if (multiplier > 0) {
    	ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        if (backoff.random()) {
            policy = new ExponentialRandomBackOffPolicy();
        }
        policy.setInitialInterval(min);
        policy.setMultiplier(multiplier);
        policy.setMaxInterval(max > min ? max : ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL);
        if (this.sleeper != null) {
            policy.setSleeper(this.sleeper);
        }
        return policy;
    }
    if (max > min) {
        UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
        policy.setMinBackOffPeriod(min);
        policy.setMaxBackOffPeriod(max);
        if (this.sleeper != null) {
            policy.setSleeper(this.sleeper);
        }
        return policy;
    }
    FixedBackOffPolicy policy = new FixedBackOffPolicy();
    policy.setBackOffPeriod(min);
    if (this.sleeper != null) {
    	policy.setSleeper(this.sleeper);
    }
    return policy;
}
```

嗯～，一样的味道。就是通过 `@Backoff` 注解中的参数，来判断具体使用文章开头说到的哪个退避策略，是 `FixedBackOffPolicy` 还是`UniformRandomBackOffPolicy` 等。

那么每个 `RetryPolicy` 都会重写 `canRetry` 方法，然后在 `RetryTemplate` 判断是否需要重试。我们看看 `SimpleRetryPolicy` 的

```
@Override
public boolean canRetry(RetryContext context) {
    Throwable t = context.getLastThrowable();
    //判断抛出的异常是否符合重试的异常
    //还有，是否超过了重试的次数
    return (t == null || retryForException(t)) && context.getRetryCount() < maxAttempts;
}
```

同样，我们看看 `FixedBackOffPolicy` 的退避方法。

```
protected void doBackOff() throws BackOffInterruptedException {
    try {
        //就是sleep固定的时间
        sleeper.sleep(backOffPeriod);
    } catch (InterruptedException e) {
        throw new BackOffInterruptedException("Thread interrupted while sleeping", e);
    }
}
```

至此，重试的主要原理以及逻辑大概就是这样了。

#### 4.4.3.RetryContext

我觉得有必要说说 `RetryContext`，先看看它的继承关系。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504232358.webp)

可以看出对每一个策略都有对应的 `Context`。

在 `Spring Retry` 里，其实每一个策略都是单例来的。我刚开始直觉是对每一个需要重试的方法都会 `new` 一个策略，这样重试策略之间才不会产生冲突，但是一想就知道这样就可能多出了很多策略对象出来，增加了使用者的负担，这不是一个好的设计。`Spring Retry` 采用了一个更加轻量级的做法，就是针对每一个需要重试的方法只 `new` 一个上下文 `Context` 对象，然后在重试时，把这个 `Context` 传到策略里，策略再根据这个 `Context` 做重试，而且 `Spring Retry` 还对这个 `Context` 做了 `cache`。这样就相当于对重试的上下文做了优化。

### 4.5.总结

`Spring Retry` 通过 `AOP` 机制来实现对业务代码的重试”入侵“，`RetryTemplate` 中包含了核心的重试逻辑，还提供了丰富的重试策略和退避策略。

## 5.轻松自定义类型转换

spring目前支持3中类型转换器：

- Converter<S,T>：将 S 类型对象转为 T 类型对象
- ConverterFactory<S, R>：将 S 类型对象转为 R 类型及子类对象
- GenericConverter：它支持多个source和目标类型的转化，同时还提供了source和目标类型的上下文，这个上下文能让你实现基于属性上的注解或信息来进行类型转换。

这3种类型转换器使用的场景不一样，我们以`Converter<S,T>`为例。假如：接口中接收参数的实体对象中，有个字段的类型是Date，但是实际传参的是字符串类型：2021-01-03 10:20:15，要如何处理呢？

第一步，定义一个实体User：

```
@Data
public class User {
    private Long id;
    private String name;
    private Date registerDate;
}
```

第二步，实现Converter接口：

```
public class DateConverter implements Converter<String, Date> {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Date convert(String source) {
        if (source != null && !"".equals(source)) {
            try {
                simpleDateFormat.parse(source);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
```

第三步，将新定义的类型转换器注入到spring容器中：

```
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new DateConverter());
    }
}
```

第四步，调用接口

```
@RequestMapping("/user")
@RestController
public class UserController {

    @RequestMapping("/save")
    public String save(@RequestBody User user) {
        return "success";
    }
}
```

请求接口时User对象中registerDate字段会被自动转换成Date类型。

## 6.Enable开关真香

不知道你有没有用过`Enable`开头的注解，比如：EnableAsync、EnableCaching、EnableAspectJAutoProxy等，这类注解就像开关一样，只要在@Configuration定义的配置类上加上这类注解，就能开启相关的功能。

让我们一起实现一个自己的开关：

第一步，定义一个LogFilter：

```
public class LogFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("记录请求日志");
        chain.doFilter(request, response);
        System.out.println("记录响应日志");
    }

    @Override
    public void destroy() {
        
    }
}
```

第二步，注册LogFilter：

```
@ConditionalOnWebApplication
public class LogFilterWebConfig {

    @Bean
    public LogFilter timeFilter() {
        return new LogFilter();
    }
}
```

注意，这里用了`@ConditionalOnWebApplication`注解，没有直接使用`@Configuration`注解。

第三步，定义开关@EnableLog注解：

```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LogFilterWebConfig.class)
public @interface EnableLog {

}
```

第四步，只需在springboot启动类加上@EnableLog注解即可开启LogFilter记录请求和响应日志的功能。

## 7.RestTemplate拦截器

我们使用`RestTemplate`调用远程接口时，有时需要在header中传递信息，比如：traceId，source等，便于在查询日志时能够串联一次完整的请求链路，快速定位问题。

这种业务场景就能通过`ClientHttpRequestInterceptor`接口实现，具体做法如下：

第一步，实现ClientHttpRequestInterceptor接口：

```
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("traceId", MdcUtil.get());
        return execution.execute(request, body);
    }
}
```

第二步，定义配置类：

```
@Configuration
public class RestTemplateConfiguration {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(restTemplateInterceptor()));
        return restTemplate;
    }

    @Bean
    public RestTemplateInterceptor restTemplateInterceptor() {
        return new RestTemplateInterceptor();
    }
}
```

其中MdcUtil其实是利用`MDC`工具在`ThreadLocal`中存储和获取traceId

```
public class MdcUtil {

    private static final String TRACE_ID = "TRACE_ID";

    public static String get() {
        return MDC.get(TRACE_ID);
    }

    public static void add(String value) {
        MDC.put(TRACE_ID, value);
    }
}
```

当然，这个例子中没有演示MdcUtil类的add方法具体调的地方，我们可以在filter中执行接口方法之前，生成traceId，调用MdcUtil类的add方法添加到MDC中，然后在同一个请求的其他地方就能通过MdcUtil类的get方法获取到该traceId。

## 8.@ControllerAdvice与统一异常处理

### 8.1.@ControllerAdvice

Spring源码中有关`@ControllerAdvice`的注解如下：

> Specialization of {@link Component @Component} for classes that declare {@link ExceptionHandler @ExceptionHandler}, {@link InitBinder @InitBinder}, or {@link ModelAttribute @ModelAttribute} methods to be shared across multiple {@code @Controller} classes.

理解：

`@ControllerAdvice`是一个特殊的`@Component`，用于标识一个类，这个类中被以下三种注解标识的方法：`@ExceptionHandler`，`@InitBinder`，`@ModelAttribute`，将作用于所有的`@Controller`类的接口上。

那么，这个三个注解分别是什么意思，起到什么作用呢？

### 8.2.@InitBinder

> Annotation that identifies methods which initialize the {@link org.springframework.web.bind.WebDataBinder} which will be used for populating command and form object arguments of annotated handler methods. Such init-binder methods support all arguments that {@link RequestMapping} supports, except for command/form objects and corresponding validation result objects. Init-binder methods must not have a return value; they are usually declared as {@code void}.

作用：注册属性编辑器，对HTTP请求参数进行处理，再绑定到对应的接口，比如格式化的时间转换等。应用于单个@Controller类的方法上时，仅对该类里的接口有效。与@ControllerAdvice组合使用可全局生效。

示例：

```
@ControllerAdvice
public class ActionAdvice {
    
    @InitBinder
    public void handleException(WebDataBinder binder) {
        binder.addCustomFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));
    }
}
```

### 8.3.@ExceptionHandler

作用：统一异常处理，也可以指定要处理的异常类型

示例：

```
@ControllerAdvice
public class ActionAdvice {
    
    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Map handleException(Exception ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 400);
        map.put("msg", ex.toString());
        return map;
    }
}
```

### 8.4.@ModelAttribute

作用：绑定数据

示例：

```
@ControllerAdvice
public class ActionAdvice {
    
    @ModelAttribute
    public void handleException(Model model) {
        model.addAttribute("user", "zfh");
    }
}
```

在接口中获取前面绑定的参数：

```
@RestController
public class BasicController {
    
    @GetMapping(value = "index")
    public Map index(@ModelAttribute("user") String user) {
        //...
    }
}
```

完整示例代码：

```
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一异常处理
 * @author zfh
 * @version 1.0
 * @since 2019/1/4 15:23
 */
@ControllerAdvice
public class ControllerExceptionHandler {

    private Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @InitBinder
    public void initMyBinder(WebDataBinder binder) {
        // 添加对日期的统一处理
        //binder.addCustomFormatter(new DateFormatter("yyyy-MM-dd"));
        binder.addCustomFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));

        // 添加表单验证
        //binder.addValidators();
    }

    @ModelAttribute
    public void addMyAttribute(Model model) {
        model.addAttribute("user", "zfh"); // 在@RequestMapping的接口中使用@ModelAttribute("name") Object name获取
    }

    @ExceptionHandler(value = Exception.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody // 如果使用了@RestControllerAdvice，这里就不需要@ResponseBody了
    public Map handler(Exception ex) {
        logger.error("统一异常处理", ex);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 400);
        map.put("msg", ex);
        return map;
    }
}
```

测试接口：

```
@RestController
public class TestAction {

    @GetMapping(value = "testAdvice")
    public JsonResult testAdvice(@ModelAttribute("user") String user, Date date) throws Exception {
        System.out.println("user: " + user);
        System.out.println("date: " + date);
        throw new Exception("直接抛出异常");
    }
}
```

### 8.5.高阶应用--格式化时间转Date

使用`@ControllerAdvice` + `@InitBinder`，可将http请求参数中的时间自动转换成Date类型。

```
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        GenericConversionService genericConversionService = (GenericConversionService) binder.getConversionService();
        if (genericConversionService != null) {
            genericConversionService.addConverter(new DateConverter());
        }
    }
```

自定义的时间类型转换器：

```
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期转换类
 * 将标准日期、标准日期时间、时间戳转换成Date类型
 */
public class DateConverter implements Converter<String, Date> {
    private static final String dateFormat = "yyyy-MM-dd HH:mm:ss";
    private static final String shortDateFormat = "yyyy-MM-dd";
    private static final String timeStampFormat = "^\\d+$";

    @Override
    public Date convert(String value) {

        if(StringUtils.isEmpty(value)) {
            return null;
        }

        value = value.trim();

        try {
            if (value.contains("-")) {
                SimpleDateFormat formatter;
                if (value.contains(":")) {
                    formatter = new SimpleDateFormat(dateFormat);
                } else {
                    formatter = new SimpleDateFormat(shortDateFormat);
                }
                return formatter.parse(value);
            } else if (value.matches(timeStampFormat)) {
                Long lDate = new Long(value);
                return new Date(lDate);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("parser %s to Date fail", value));
        }
        throw new RuntimeException(String.format("parser %s to Date fail", value));
    }
}
```


