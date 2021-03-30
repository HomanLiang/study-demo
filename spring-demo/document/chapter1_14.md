[toc]



# Spring 应用

## Assert类

### Assert类目的

Spring Assert类帮助我们校验参数。通过使用Assert类方法，我们可以写出我们认为是正确的假设，反之，会抛出运行时异常。

每个Assert的方法可以与java assert表达式进行比较。java assert表达式在运行时如果条件校验失败，则抛出Error，有趣的是，这些断言可以被禁用。

Spring Assert的方法有一些特点： 

- 都是static方法 
- 抛出IllegalArgumentException 或 IllegalStateException异常 
- 第一个参数通常是需验证的对象或逻辑条件 
- 最后参数通常是异常消息，用于验证失败时显示 
- 消息可以作为String参数或Supplier 参数传输

尽管Spring Assert与其他框架的名称类似，如JUnit或其他框架，但其实没有任何共同之处。Spring Assert不是为了测试，而是为了调试。

### 使用示例

让我们定义Car类，并有public方法drive():

    public class Car {
        private String state = "stop";
    
        public void drive(int speed) {
            Assert.isTrue(speed > 0, "speed must be positive");
            this.state = "drive";
            // ...
        }
    }

我们看到speed必须是正数，上面一行简短的代码用于检测条件，如果失败抛出异常：

    if (!(speed > 0)) {
        throw new IllegalArgumentException("speed must be positive");
    }

每个Assert的方法包含大概类似上面的条件代码块，校验失败抛出运行时异常，应用程序不期望恢复。 
如果我们尝试带负数参数调用drive方法，会抛出IllegalArgumentException异常：

    Exception in thread "main" java.lang.IllegalArgumentException: speed must be positive

**逻辑断言**

- isTrue()

  上面已经看到示例，其接受布尔条件，如果条件为假抛出IllegalArgumentException 异常。

- state()

  该方法与isTrue一样，但抛出IllegalStateException异常。

  如名称所示，通常用在因对象的非法状态时，方法不能继续执行。假设骑车运行是不能加油，我们可以使用state方法断言：

  ```
  public void fuel() {
      Assert.state(this.state.equals("stop"), "car must be stopped");
      // ...
  }
  ```

 当然，我们能使用逻辑断言验证所有场景。但为了更好的可读性，我们可以使用其他的断言，使代码表达性更好。

**对象和类型断言**

- notNull()

  通过notNull()方法可以假设对象不null：

  ```
  public void сhangeOil(String oil) {
      Assert.notNull(oil, "oil mustn't be null");
      // ...
  }
  ```

- isNull()

  另外一方面，我们能使用isNull()方法检查对象为null:

  ```
  public void replaceBattery(CarBattery carBattery) {
      Assert.isNull(
          carBattery.getCharge(), 
          "to replace battery the charge must be null");
      // ...
  }
  ```

- isInstanceOf()

  使用isInstanceOf()方法检查对象必须为另一个特定类型的实例：

  ```
  public void сhangeEngine(Engine engine) {
      Assert.isInstanceOf(ToyotaEngine.class, engine);
      // ...
  }
  ```

  示例中，ToyotaEngine 是类 Engine的子类，所以检查通过.

- isAssignable()

  使用Assert.isAssignable()方法检查类型：

  ```
  public void repairEngine(Engine engine) {
      Assert.isAssignable(Engine.class, ToyotaEngine.class);
      // ...
  }
  ```

  这两个断言代表 is-a 关系.

**文本断言**

通常用来检查字符串参数。

- hasLength()

  如果检查字符串不是空符串，意味着至少包含一个空白，可以使用hasLength()方法：

  ```
  public void startWithHasLength(String key) {
      Assert.hasLength(key, "key must not be null and must not the empty");
      // ...
  }
  ```

- hasText()

  我们能增强检查条件，字符串至少包含一个非空白字符，可以使用hasText()方法：

  ```
  public void startWithHasText(String key) {
      Assert.hasText(
      key, 
      "key must not be null and must contain at least one non-whitespace  character");
      // ...
  }
  ```

- doesNotContain()

  我们能通过doesNotContain()方法检查参数不包含特定子串：

  ```
  public void startWithNotContain(String key) {
      Assert.doesNotContain(key, "123", "key mustn't contain 123");
      // ...
  }
  ```

**Collection和map断言**

- Collection应用notEmpty()

  如其名称所示，notEmpty()方法断言collection不空，意味着不是null并包含至少一个元素：

  ```
  public void repair(Collection<String> repairParts) {
      Assert.notEmpty(
      repairParts, 
      "collection of repairParts mustn't be empty");
      // ...
  }
  ```

- map应用notEmpty()

  同样的方法重载用于map，检查map不null，并至少包含一个entry（key，value键值对）：

  ```
  public void repair(Map<String, String> repairParts) {
      Assert.notEmpty(
      repairParts, 
      "map of repairParts mustn't be empty");
      // ...
  }
  ```

**数组断言**

- notEmpty()

  notEmpty()方法可以检查数组不null，且至少包括一个元素：

  ```
  public void repair(String[] repairParts) {
      Assert.notEmpty(
      repairParts, 
      "array of repairParts mustn't be empty");
      // ...
  }
  ```

- noNullElements()

  noNullElements()方法确保数组不包含null元素：

  ```
  public void repairWithNoNull(String[] repairParts) {
      Assert.noNullElements(
      repairParts, 
      "array of repairParts mustn't contain null elements");
      // ...
  }
  ```

注意，如果数组为空检查可以通过，只要没有null元素。

### 总结

我们浏览Assert类，在spring框架中应用广泛，充分利用它可以很容易写出强壮的代码。



## 自定义注解

### 基本知识

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

### 使用自定义注解做日志记录

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

1、使用@Around注解来指定对标注了OpLog的方法设置切面。

 2、使用Spel的相关方法，通过指定的表示，从对应的参数中获取到目标对象的唯一性标识。 

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

以上，即可从入参的OrderVO对象的id属性的值获取。

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

### 使用自定义注解做前置检查

当我们对外部提供接口的时候，会对其中的部分参数有一定的要求，比如某些参数值不能为空等。大多数情况下我们都需要自己主动进行校验，判断对方传入的值是否合理。

这里推荐一个使用HibernateValidator + 自定义注解 + AOP实现参数校验的方式。

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

以上代码，会对一个bean进行校验，一旦失败，就会抛出ValidationException。

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

以上代码，和前面的切面有点类似，主要是定义了一个切面，会对所有标注@Facade的方法进行统一处理，即在开始方法调用前进行参数校验，一旦校验失败，则返回一个固定的失败的Response，特别需要注意的是，这里之所以可以返回一个固定的BaseResponse，是因为我们会要求我们的所有对外提供的接口的response必须继承BaseResponse类，这个类里面会定义一些默认的参数，如错误码等。

之后，只需要对需要参数校验的方法增加对应注解即可：

```
@Facade
public TestResponse query(User user) {

}
```

这样，有了以上注解和切面，我们就可以对所有的对外方法做统一的控制了。

其实，以上这个facadeAspect我省略了很多东西，我们真正使用的那个切面，不仅仅做了参数检查，还可以做很多其他事情。比如异常的统一处理、错误码的统一转换、记录方法执行时长、记录方法的入参出参等等。

总之，使用切面+自定义注解，我们可以统一做很多事情。除了以上的这几个场景，我们还有很多相似的用法，比如：

统一的缓存处理。如某些操作需要在操作前查缓存、操作后更新缓存。这种就可以通过自定义注解+切面的方式统一处理。

代码其实都差不多，思路也比较简单，就是通过自定义注解来标注需要被切面处理的累或者方法，然后在切面中对方法的执行过程进行干预，比如在执行前或者执行后做一些特殊的操作。

使用这种方式可以大大减少重复代码，大大提升代码的优雅性，方便我们使用。

但是同时也不能过度使用，因为注解看似简单，但是其实内部有很多逻辑是容易被忽略的。但是快快在你的项目中用起来吧。