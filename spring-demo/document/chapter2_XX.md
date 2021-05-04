[TOC]



# Spring Boot 常见问题

## 1.Spring Boot 定义接口的方法是否可以声明为 private？

我们在 Controller 中定义接口的时候，一般都是像下面这样：

```
@GetMapping("/01")
public String hello(Map<String,Object> map) {
    map.put("name", "javaboy");
    return "forward:/index";
}
```

估计很少有人会把接口方法定义成 private 的吧？那我们不禁要问，如果非要定义成 private 的方法，那能运行起来吗？

带着这个疑问，我们开始今天的源码解读～

在我们使用 Spring Boot 的时候，经常会看到 HandlerMethod 这个类型，例如我们在定义拦截器的时候，如果拦截目标是一个方法，则 preHandle 的第三个参数就是 HandlerMethod

```
@Component
public class IdempotentInterceptor implements HandlerInterceptor {
    @Autowired
    TokenService tokenService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        //省略...
        return true;
    }
    //...
}
```

我们在阅读 SpringMVC 源码的时候，也会反复看到这个 HandlerMethod，那么它到底是什么意思？今天我想和小伙伴们捋一捋这个问题，把这个问题搞清楚了，前面的问题大家也就懂了。

### 1.1.概览

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504101246.webp)

可以看到，HandlerMethod 体系下的类并不多：

`HandlerMethod`：封装 Handler 和具体处理请求的 Method。

`InvocableHandlerMethod`：在 HandlerMethod 的基础上增加了调用的功能。

`ServletInvocableHandlerMethod`：在 InvocableHandlerMethod 的基础上增了对 `@ResponseStatus` 注解的支持、增加了对返回值的处理。

`ConcurrentResultHandlerMethod`：在 ServletInvocableHandlerMethod 的基础上，增加了对异步结果的处理。

基本上就是这四个，接下来松哥就来详细说一说这四个组件。

### 1.2.HandlerMethod

#### 1.2.1 bridgedMethod

在正式开始介绍 HandlerMethod 之前，想先和大家聊聊 bridgedMethod，因为在 HandlerMethod 中将会涉及到这个东西，而有的小伙伴可能还没听说过 bridgedMethod，因此松哥在这里做一个简单介绍。

首先考考大家，下面这段代码编译会报错吗？

```
public interface Animal<T> {
    void eat(T t);
}
public class Cat implements Animal<String> {
    @Override
    public void eat(String s) {
        System.out.println("cat eat " + s);
    }
}
public class Demo01 {
    public static void main(String[] args) {
        Animal animal = new Cat();
        animal.eat(new Object());
    }
}
```

首先我们定义了一个 Animal 接口，里边定义了一个 eat 方法，同时声明了一个泛型。Cat 实现了 Animal 接口，将泛型也定义为了 String。当我调用的时候，声明类型是 Animal，实际类型是 Cat，这个时候调 eat 方法传入了 Object 对象大家猜猜会怎么样？如果调用 eat 方法时传入的是 String 类型那就肯定没问题，但如果不是 String 呢？

松哥先说结论：编译没问题，运行报错。

如果小伙伴们在自己电脑上写出上面这段代码，你会发现这样一个问题，开发工具中提示的参数类型竟然是 Object，以松哥的 IDEA 为例，如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504101356.webp)

大家看到，在我写代码的时候，开发工具会给我提示，这个参数类型是 Object，有的小伙伴会觉得奇怪，明明是泛型，怎么变成 Object 了？

我们可以通过反射查看 Cat 类中到底有哪些方法，代码如下：

```
public class Demo01 {
    public static void main(String[] args) {
        Method[] methods = Cat.class.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            System.out.println(name+"("+ Arrays.toString(parameterTypes) +")");
        }
    }
}
```

运行结果如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504101433.webp)

可以看到，在实际运行过程中，竟然有两个 eat 方法，一个的参数为 String 类型，另一个参数为 Object 类型，这是怎么回事呢？

这个参数类型为 Object 的方法其实是 Java 虚拟机在运行时创建出来的，这个方法就是我们所说的 bridge method。本节的小标题叫做 `bridgedMethod`，这是 HandlerMethod 源码中的变量名，bridge 结尾多了一个 d，含义变成了被 bridge 的方法，也就是参数为 String 的原方法，大家在接下来的源码中看到了 bridgedMethod 就知道这表示参数类型不变的原方法。

#### 1.2.2.HandlerMethod 介绍

接下来我们来简单看下 HandlerMethod。

在我们前面分析 HandlerMapping 的时候（参见：），里边有涉及到 HandlerMethod，创建 HandlerMethod 的入口方法是 createWithResolvedBean，因此这里我们就从该方法开始看起：

```
public HandlerMethod createWithResolvedBean() {
 	Object handler = this.bean;
 	if (this.bean instanceof String) {
  		String beanName = (String) this.bean;
  		handler = this.beanFactory.getBean(beanName);
 	}
 	return new HandlerMethod(this, handler);
}
```

这个方法主要是确认了一下 handler 的类型，如果 handler 是 String 类型，则根据 beanName 从 Spring 容器中重新查找到 handler 对象，然后构建 HandlerMethod：

```
private HandlerMethod(HandlerMethod handlerMethod, Object handler) {
 	this.bean = handler;
 	this.beanFactory = handlerMethod.beanFactory;
 	this.beanType = handlerMethod.beanType;
 	this.method = handlerMethod.method;
 	this.bridgedMethod = handlerMethod.bridgedMethod;
 	this.parameters = handlerMethod.parameters;
 	this.responseStatus = handlerMethod.responseStatus;
 	this.responseStatusReason = handlerMethod.responseStatusReason;
 	this.resolvedFromHandlerMethod = handlerMethod;
 	this.description = handlerMethod.description;
}
```

这里的参数都比较简单，没啥好说的，唯一值得介绍的地方有两个：parameters 和 responseStatus。

##### 1.2.2.1.parameters

parameters 实际上就是方法参数，对应的类型是 MethodParameter，这个类的源码我这里就不贴出来了，主要和大家说一下封装的内容包括：参数的序号（parameterIndex），参数嵌套级别（nestingLevel），参数类型（parameterType），参数的注解（parameterAnnotations），参数名称查找器（parameterNameDiscoverer），参数名称（parameterName）等。

HandlerMethod 中还提供了两个内部类来封装 MethodParameter，分别是：

- HandlerMethodParameter：这个封装方法调用的参数。
- ReturnValueMethodParameter：这个继承自 HandlerMethodParameter，它封装了方法的返回值，返回值里边的 parameterIndex 是 -1。

注意，这两者中的 method 都是 bridgedMethod。

##### 1.2.2.2.responseStatus

这个主要是处理方法的 `@ResponseStatus` 注解，这个注解用来描述方法的响应状态码，使用方式像下面这样：

```
@GetMapping("/04")
@ResponseBody
@ResponseStatus(code = HttpStatus.OK)
public void hello4(@SessionAttribute("name") String name) {
    System.out.println("name = " + name);
}
```

从这段代码中大家可以看到，其实 `@ResponseStatus` 注解灵活性很差，不实用，当我们定义一个接口的时候，很难预知到该接口的响应状态码是 200。

在 handlerMethod 中，在调用其构造方法的时候，都会调用 evaluateResponseStatus 方法处理 `@ResponseStatus` 注解，如下：

```
private void evaluateResponseStatus() {
 	ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
 	if (annotation == null) {
  	annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
 	}
 	if (annotation != null) {
  		this.responseStatus = annotation.code();
  		this.responseStatusReason = annotation.reason();
 	}
}
```

可以看到，这段代码也比较简单，找到注解，把里边的值解析出来，赋值给相应的变量。

这下小伙伴们应该明白了 HandlerMethod 大概是个怎么回事。

### 1.3.InvocableHandlerMethod

看名字就知道，InvocableHandlerMethod 可以调用 HandlerMethod 中的具体方法，也就是 bridgedMethod。我们先来看下 InvocableHandlerMethod 中声明的属性：

```
private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();
private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
@Nullable
private WebDataBinderFactory dataBinderFactory;
```

主要就是这三个属性：

- resolvers：这个不用说，参数解析器，前面的文章中松哥已经和大家聊过这个问题了。
- parameterNameDiscoverer：这个用来获取参数名称，在 MethodParameter 中会用到。
- dataBinderFactory：这个用来创建 WebDataBinder，在参数解析器中会用到。

具体的请求调用方法是 invokeForRequest，我们一起来看下：

```
@Nullable
public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
  	Object... providedArgs) throws Exception {
 	Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
 	return doInvoke(args);
}
@Nullable
protected Object doInvoke(Object... args) throws Exception {
 	Method method = getBridgedMethod();
 	ReflectionUtils.makeAccessible(method);
 	try {
  	if (KotlinDetector.isSuspendingFunction(method)) {
   		return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);
  	}
  	return method.invoke(getBean(), args);
 }
 catch (InvocationTargetException ex) {
  	// 省略 ...
 }
}
```

首先调用 getMethodArgumentValues 方法按顺序获取到所有参数的值，这些参数值组成一个数组，然后调用 doInvoke 方法执行，在 doInvoke 方法中，首先获取到 bridgedMethod，并设置其可见（意味着我们在 Controller 中定义的接口方法也可以是 private 的），然后直接通过反射调用即可。当我们没看 SpringMVC 源码的时候，我们就知道接口方法最终肯定是通过反射调用的，现在，经过层层分析之后，终于在这里找到了反射调用代码。

最后松哥再来说一下负责参数解析的 getMethodArgumentValues 方法：

```
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,
  	Object... providedArgs) throws Exception {
 	MethodParameter[] parameters = getMethodParameters();
 	if (ObjectUtils.isEmpty(parameters)) {
  		return EMPTY_ARGS;
 	}
 	Object[] args = new Object[parameters.length];
 	for (int i = 0; i < parameters.length; i++) {
  		MethodParameter parameter = parameters[i];
  		parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
  		args[i] = findProvidedArgument(parameter, providedArgs);
  		if (args[i] != null) {
   			continue;
  		}
  		if (!this.resolvers.supportsParameter(parameter)) {
   			throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
      	}
  		try {
   			args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
  		}
  		catch (Exception ex) {
   			// 省略...
 	 	}
 	}
 	return args;
}
```

1. 首先调用 getMethodParameters 方法获取到方法的所有参数。
2. 创建 args 数组用来保存参数的值。
3. 接下来一堆初始化配置。
4. 如果 providedArgs 中提供了参数值，则直接赋值。
5. 查看是否有参数解析器支持当前参数类型，如果没有，直接抛出异常。
6. 调用参数解析器对参数进行解析，解析完成后，赋值。

### 1.4.ServletInvocableHandlerMethod

ServletInvocableHandlerMethod 则是在 InvocableHandlerMethod 的基础上，又增加了两个功能：

- 对 `@ResponseStatus` 注解的处理
- 对返回值的处理

Servlet 容器下 Controller 在查找适配器时发起调用的最终就是 ServletInvocableHandlerMethod。

这里的处理核心方法是 invokeAndHandle，如下：

```
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,
  Object... providedArgs) throws Exception {
 	Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
 	setResponseStatus(webRequest);
 	if (returnValue == null) {
  		if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {
   			disableContentCachingIfNecessary(webRequest);
   			mavContainer.setRequestHandled(true);
   			return;
  		}
 	}
 	else if (StringUtils.hasText(getResponseStatusReason())) {
  		mavContainer.setRequestHandled(true);
  		return;
 	}
 	mavContainer.setRequestHandled(false);
 	try {
  		this.returnValueHandlers.handleReturnValue(
    	returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
 	}
 	catch (Exception ex) {
  		throw ex;
 	}
}
```

1. 首先调用父类的 invokeForRequest 方法对请求进行执行，拿到请求结果。
2. 调用 setResponseStatus 方法处理 `@ResponseStatus` 注解，具体的处理逻辑是这样：如果没有添加 `@ResponseStatus` 注解，则什么都不做；如果添加了该注解，并且 reason 属性不为空，则直接输出错误，否则设置响应状态码。这里需要注意一点，如果响应状态码是 200，就不要设置 reason，否则会按照 error 处理。
3. 接下来就是对返回值的处理了，returnValueHandlers#handleReturnValue 方法

事实上，ServletInvocableHandlerMethod 还有一个子类 ConcurrentResultHandlerMethod，这个支持异步调用结果处理，因为使用场景较少，这里就不做介绍啦。































