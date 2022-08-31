[TOC]



# Spring Boot 常见问题

## 1.Spring Boot 定义接口的方法是否可以声明为 private？

我们在 `Controller` 中定义接口的时候，一般都是像下面这样：

```
@GetMapping("/01")
public String hello(Map<String,Object> map) {
    map.put("name", "javaboy");
    return "forward:/index";
}
```

估计很少有人会把接口方法定义成 `private` 的吧？那我们不禁要问，如果非要定义成 `private` 的方法，那能运行起来吗？

带着这个疑问，我们开始今天的源码解读～

在我们使用 `Spring Boot` 的时候，经常会看到 `HandlerMethod` 这个类型，例如我们在定义拦截器的时候，如果拦截目标是一个方法，则 `preHandle` 的第三个参数就是 `HandlerMethod`

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

我们在阅读 `SpringMVC` 源码的时候，也会反复看到这个 `HandlerMethod`，那么它到底是什么意思？今天我想和小伙伴们捋一捋这个问题，把这个问题搞清楚了，前面的问题大家也就懂了。

### 1.1.概览

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504101246.webp)

可以看到，`HandlerMethod` 体系下的类并不多：

`HandlerMethod`：封装 `Handler` 和具体处理请求的 `Method`。

`InvocableHandlerMethod`：在 `HandlerMethod` 的基础上增加了调用的功能。

`ServletInvocableHandlerMethod`：在 `InvocableHandlerMethod` 的基础上增了对 `@ResponseStatus` 注解的支持、增加了对返回值的处理。

`ConcurrentResultHandlerMethod`：在 `ServletInvocableHandlerMethod` 的基础上，增加了对异步结果的处理。

基本上就是这四个，接下来松哥就来详细说一说这四个组件。

### 1.2.HandlerMethod

#### 1.2.1 bridgedMethod

在正式开始介绍 `HandlerMethod` 之前，想先和大家聊聊 `bridgedMethod`，因为在 `HandlerMethod` 中将会涉及到这个东西，而有的小伙伴可能还没听说过 `bridgedMethod`，因此松哥在这里做一个简单介绍。

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

首先我们定义了一个 `Animal` 接口，里边定义了一个 `eat` 方法，同时声明了一个泛型。`Cat`  实现了 `Animal` 接口，将泛型也定义为了 `String`。当我调用的时候，声明类型是 `Animal`，实际类型是 `Cat`，这个时候调 `eat` 方法传入了 `Object` 对象大家猜猜会怎么样？如果调用 `eat` 方法时传入的是 `String` 类型那就肯定没问题，但如果不是 `String` 呢？

松哥先说结论：编译没问题，运行报错。

如果小伙伴们在自己电脑上写出上面这段代码，你会发现这样一个问题，开发工具中提示的参数类型竟然是 `Object`，以松哥的 `IDEA` 为例，如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504101356.webp)

大家看到，在我写代码的时候，开发工具会给我提示，这个参数类型是 `Object`，有的小伙伴会觉得奇怪，明明是泛型，怎么变成 `Object` 了？

我们可以通过反射查看 `Cat` 类中到底有哪些方法，代码如下：

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

可以看到，在实际运行过程中，竟然有两个 `eat` 方法，一个的参数为 `String` 类型，另一个参数为 `Object` 类型，这是怎么回事呢？

这个参数类型为 `Object` 的方法其实是 `Java` 虚拟机在运行时创建出来的，这个方法就是我们所说的 `bridge method`。本节的小标题叫做 `bridgedMethod`，这是 `HandlerMethod` 源码中的变量名，`bridge` 结尾多了一个 `d`，含义变成了被 `bridge` 的方法，也就是参数为 String 的原方法，大家在接下来的源码中看到了 `bridgedMethod` 就知道这表示参数类型不变的原方法。

#### 1.2.2.HandlerMethod 介绍

接下来我们来简单看下 `HandlerMethod`。

在我们前面分析 `HandlerMapping` 的时候（参见：），里边有涉及到 `HandlerMethod`，创建 `HandlerMethod` 的入口方法是 `createWithResolvedBean`，因此这里我们就从该方法开始看起：

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

这个方法主要是确认了一下 `handler` 的类型，如果 `handler` 是 `String` 类型，则根据 `beanName` 从 `Spring` 容器中重新查找到 `handler` 对象，然后构建 `HandlerMethod`：

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

这里的参数都比较简单，没啥好说的，唯一值得介绍的地方有两个：`parameters` 和 `responseStatus`。

##### 1.2.2.1.parameters

`parameters` 实际上就是方法参数，对应的类型是 `MethodParameter`，这个类的源码我这里就不贴出来了，主要和大家说一下封装的内容包括：参数的序号（`parameterIndex`），参数嵌套级别（`nestingLevel`），参数类型（`parameterType`），参数的注解（`parameterAnnotations`），参数名称查找器（`parameterNameDiscoverer`），参数名称（`parameterName`）等。

`HandlerMethod` 中还提供了两个内部类来封装 `MethodParameter`，分别是：

- `HandlerMethodParameter`：这个封装方法调用的参数。
- `ReturnValueMethodParameter`：这个继承自 `HandlerMethodParameter`，它封装了方法的返回值，返回值里边的 `parameterIndex` 是 -1。

注意，这两者中的 `method` 都是 `bridgedMethod`。

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

在 `handlerMethod` 中，在调用其构造方法的时候，都会调用 `evaluateResponseStatus` 方法处理 `@ResponseStatus` 注解，如下：

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

这下小伙伴们应该明白了 `HandlerMethod` 大概是个怎么回事。

### 1.3.InvocableHandlerMethod

看名字就知道，`InvocableHandlerMethod` 可以调用 `HandlerMethod` 中的具体方法，也就是 `bridgedMethod`。我们先来看下 `InvocableHandlerMethod` 中声明的属性：

```
private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();
private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
@Nullable
private WebDataBinderFactory dataBinderFactory;
```

主要就是这三个属性：

- `resolvers`：参数解析器
- `parameterNameDiscoverer`：这个用来获取参数名称，在 `MethodParameter` 中会用到。
- `dataBinderFactory`：这个用来创建 `WebDataBinder`，在参数解析器中会用到。

具体的请求调用方法是 `invokeForRequest`，我们一起来看下：

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

首先调用 `getMethodArgumentValues` 方法按顺序获取到所有参数的值，这些参数值组成一个数组，然后调用 `doInvoke` 方法执行，在 `doInvoke` 方法中，首先获取到 `bridgedMethod`，并设置其可见（意味着我们在 `Controller` 中定义的接口方法也可以是 `private `的），然后直接通过反射调用即可。当我们没看 `SpringMVC` 源码的时候，我们就知道接口方法最终肯定是通过反射调用的，现在，经过层层分析之后，终于在这里找到了反射调用代码。

最后松哥再来说一下负责参数解析的 `getMethodArgumentValues` 方法：

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

1. 首先调用 `getMethodParameters` 方法获取到方法的所有参数。
2. 创建 `args` 数组用来保存参数的值。
3. 接下来一堆初始化配置。
4. 如果 `providedArgs` 中提供了参数值，则直接赋值。
5. 查看是否有参数解析器支持当前参数类型，如果没有，直接抛出异常。
6. 调用参数解析器对参数进行解析，解析完成后，赋值。

### 1.4.ServletInvocableHandlerMethod

`ServletInvocableHandlerMethod` 则是在 `InvocableHandlerMethod` 的基础上，又增加了两个功能：

- 对 `@ResponseStatus` 注解的处理
- 对返回值的处理

`Servlet` 容器下 `Controller` 在查找适配器时发起调用的最终就是 `ServletInvocableHandlerMethod`。

这里的处理核心方法是 `invokeAndHandle`，如下：

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

1. 首先调用父类的 `invokeForRequest` 方法对请求进行执行，拿到请求结果。
2. 调用 `setResponseStatus` 方法处理 `@ResponseStatus` 注解，具体的处理逻辑是这样：如果没有添加 `@ResponseStatus` 注解，则什么都不做；如果添加了该注解，并且 `reason` 属性不为空，则直接输出错误，否则设置响应状态码。这里需要注意一点，如果响应状态码是 `200`，就不要设置 `reason`，否则会按照 `error` 处理。
3. 接下来就是对返回值的处理了，`returnValueHandlers#handleReturnValue` 方法

事实上，`ServletInvocableHandlerMethod` 还有一个子类 `ConcurrentResultHandlerMethod`，这个支持异步调用结果处理，因为使用场景较少，这里就不做介绍啦。

## 2.SprignBoot是如何访问工程目录下的静态资源？

### 2.1.牛刀小试

#### 2.1.1.图片静态资源的访问

先看官方怎么说，点击链接，打开 [SpringBoot官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-web-applications.spring-mvc.static-content) 

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627111414.png)

文档中明确指出：**`/static` (or `/public` or `/resources` or `/META-INF/resources`)** ，这几个目录是SpringBoot放置静态资源的目录，只要把静态资源放到这几个目录下，就能直接访问到。

新建 Spingboot web项目试下，新项目只有 /static 目录 ,手动创建其他几个静态资源文件夹，每个目录添加1张图片

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627111446.png)

启动项目，分别访问这四张图片：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627111510.png)

发现图片均可访问，

文档说的对，果然没骗人，

由此我们认定 SpringBoot 访问静态资源 ：当前项目根路径 + / + 静态资源名

#### 2.1.2.为静态资源添加访问前缀

```
By default, resources are mapped on /**, but you can tune that with the spring.mvc.static-path-pattern property. For instance, relocating all resources to /resources/** can be achieved as follows:

PropertiesYaml
spring.mvc.static-path-pattern=/resources/**
```

文档又解释了一下，说，默认情况下 `SpringBoot` 是帮你映射的路径是 `/**`，

但是，如果你想**加一个前缀也可以，比如 /res/**

技术圈有句话：**先有业务才有技术**，`SpringBoot` 官方考虑到某些网站添加了登录验证，一般需要登录后才能访问项目中的资源，为了登录页样式也能正常显示，方便放行静态资源，直接给所有静态资源添加一个前缀，既可统一拦截，又可统一放开

操作：在配置文件 `application.properties` 中添加

```
spring.mvc.static-path-pattern=/res/**
```

添加完再去访问原来的dog图片链接：http://localhost:8080/dog.jpeg

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627111712.png)

但是访问：[http://localhost:8080/res/dog.jpeg](http://localhost:8080/dog.jpeg) 发现这才可以

#### 2.1.3.WelCome Page 的奇妙跳转

```
7.1.6. Welcome Page
Spring Boot supports both static and templated welcome pages. It first looks for an index.html file in the configured static content locations. If one is not found, it then looks for an index template. If either is found, it is automatically used as the welcome page of the application.
```

文档说把一个名称叫 index.html 的文件放到任意的静态目录下，访问 [http://localhost:8080](http://localhost:8080/dog.jpeg) 即可到达，**意思就是给你一个首页跳转的快捷方式**（注意：需把1.2 的配置路径去掉，否则会导致welcome page功能失效，后面源码分析会说到）

新建html，放到 /static 下，访问：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627111802.png)

### 2.2.那么，SpringBoot是如何做到的呢？

接下来看源码探究 SpringBoot 静态资源配置原理  》》》》 gogogo

源码位置在：spring-boot-autoconfigure-2.5.1.jar 这个jar里面，具体的目录如下：

```
/spring-boot-autoconfigure-2.5.1.jar!/org/springframework/boot/autoconfigure/web/servlet/WebMvcAutoConfiguration.class
```

**WebMvcAutoConfiguration** 类里面找到 **addResourceHandlers** 方法，顾名思义 **添加资源处理器**

```
		@Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            // 相当于一个开关去控制静态资源处理器的加载，默认为true，设置为false就会禁止所有规则
            if (!this.resourceProperties.isAddMappings()) {
                logger.debug("Default resource handling disabled");
                return;
            }
            //第一个就配置webjars的访问规则，规定在类路径的/META-INF/resources/webjars/路径下，感兴趣的同学可以点进方法去，里面还配置了webjars的浏览器端缓存时间，是在application。propertities中的一个配置项 spring.web.resources.cache.period  
            addResourceHandler(registry, "/webjars/**", "classpath:/META-INF/resources/webjars/");
            //这里配置了静态资源的四个访问路径
            addResourceHandler(registry, this.mvcProperties.getStaticPathPattern(), (registration) -> {
                registration.addResourceLocations(this.resourceProperties.getStaticLocations());
                if (this.servletContext != null) {
                    ServletContextResource resource = new ServletContextResource(this.servletContext, SERVLET_LOCATION);
                    registration.addResourceLocations(resource);
                }
            });
        }
```

第一个if判断 this.resourceProperties.isAddMappings() 去配置文件获取

spring.resources 这个属性，默认是 true , 如果设置为false 那么就等于禁用掉所有的静态资源映射功能，不行就试一下

```
#springapplication.propertities中配置
spring.web.resources.add-mappings=false
```

重启项目，发现首页无法访问了...

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627111938.png)

改回 true ，首页就又可以访问了

不要停留，继续看第二个 addResourceHandler 方法，打断点看看这个方法添加了什么规则 

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627112033.png)

没错，第二个addResourceHandler 方法就表明 **/ \** 下的所有请求，都在这四个默认的位置去找静态资源映射** ，这四个目录在官方文档中提到过。

另外，**访问路径前缀**是在 **this.mvcProperties.getStaticPathPattern()** 获取的，配置上：

```
spring.mvc.static-path-pattern=/res/**
```

打断点如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627112053.png)

注意📢： 所有的请求先去controller控制器找映射，找不到，再来静态资源映射器。

到这里解决了静态资源目录的问题。

马不停蹄，**探究 Welcome Page 的事情** 》》》》》

还是在 WebMvcAutoConfiguration 这个类：搜索 “**WelcomePage**” ：

```
		@Bean
        public WelcomePageHandlerMapping welcomePageHandlerMapping(ApplicationContext applicationContext,
                FormattingConversionService mvcConversionService, ResourceUrlProvider mvcResourceUrlProvider) {
            WelcomePageHandlerMapping welcomePageHandlerMapping = new WelcomePageHandlerMapping(
                    new TemplateAvailabilityProviders(applicationContext), applicationContext, getWelcomePage(),
                    this.mvcProperties.getStaticPathPattern());
            welcomePageHandlerMapping.setInterceptors(getInterceptors(mvcConversionService, mvcResourceUrlProvider));
            welcomePageHandlerMapping.setCorsConfigurations(getCorsConfigurations());
            return welcomePageHandlerMapping;
        }
```

把 WelcomePageHandlerMapping 的有参构造也拿来

```
WelcomePageHandlerMapping(TemplateAvailabilityProviders templateAvailabilityProviders,
            ApplicationContext applicationContext, Resource welcomePage, String staticPathPattern) {
        if (welcomePage != null && "/**".equals(staticPathPattern)) {
            logger.info("Adding welcome page: " + welcomePage);
            setRootViewName("forward:index.html");
        }
        else if (welcomeTemplateExists(templateAvailabilityProviders, applicationContext)) {
            logger.info("Adding welcome page template: index");
            setRootViewName("index");
        }
    }
```

根据有参构造可以看出来，只有 欢迎页这个资源存在，并且 静态资源访问路径是 /** ，才能重定向到indes.html ，否则就会去找 Controller 处理。

这就解释了，上面为什么配置了静态资源访问路径 为/res/** 后导致首页无法访问到 的问题

## 3.SpringBoot 中实现跨域的5种方式

### 3.1.为什么会出现跨域问题

出于浏览器的同源策略限制。同源策略（Sameoriginpolicy）是一种约定，它是浏览器最核心也最基本的安全功能，如果缺少了同源策略，则浏览器的正常功能可能都会受到影响。可以说Web是构建在同源策略基础之上的，浏览器只是针对同源策略的一种实现。

同源策略会阻止一个域的javascript脚本和另外一个域的内容进行交互。所谓同源（即指在同一个域）就是两个页面具有相同的协议（protocol），主机（host）和端口号（port）

### 3.2.什么是跨域

当一个请求url的协议、域名、端口三者之间任意一个与当前页面url不同即为跨域

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210627140414.webp)

### 3.3.非同源限制

【1】无法读取非同源网页的 Cookie、LocalStorage 和 IndexedDB

【2】无法接触非同源网页的 DOM

【3】无法向非同源地址发送 AJAX 请求

### 3.4.java 后端 实现 CORS 跨域请求的方式

对于 CORS的跨域请求，主要有以下几种方式可供选择：

1. 返回新的CorsFilter
2. 重写 WebMvcConfigurer
3. 使用注解 @CrossOrigin
4. 手动设置响应头 (HttpServletResponse)
5. 自定web filter 实现跨域

注意:

- CorFilter / WebMvConfigurer / @CrossOrigin 需要 SpringMVC 4.2以上版本才支持，对应springBoot 1.3版本以上

- 上面前两种方式属于全局 CORS 配置，后两种属于局部 CORS配置。如果使用了局部跨域是会覆盖全局跨域的规则，所以可以通过 @CrossOrigin 注解来进行细粒度更高的跨域资源控制。

- 其实无论哪种方案，最终目的都是修改响应头，向响应头中添加浏览器所要求的数据，进而实现跨域

  。

#### 3.4.1.返回新的 CorsFilter(全局跨域)

在任意配置类，返回一个 新的 CorsFIlter Bean ，并添加映射路径和具体的CORS配置路径。

```
@Configuration
public class GlobalCorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        //1. 添加 CORS配置信息
        CorsConfiguration config = new CorsConfiguration();
        //放行哪些原始域
        config.addAllowedOrigin("*");
        //是否发送 Cookie
        config.setAllowCredentials(true);
        //放行哪些请求方式
        config.addAllowedMethod("*");
        //放行哪些原始请求头部信息
        config.addAllowedHeader("*");
        //暴露哪些头部信息
        config.addExposedHeader("*");
        //2. 添加映射路径
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();
        corsConfigurationSource.registerCorsConfiguration("/**",config);
        //3. 返回新的CorsFilter
        return new CorsFilter(corsConfigurationSource);
    }
}
```

#### 3.4.2. 重写 WebMvcConfigurer(全局跨域)

```
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                //是否发送Cookie
                .allowCredentials(true)
                //放行哪些原始域
                .allowedOrigins("*")
                .allowedMethods(new String[]{"GET", "POST", "PUT", "DELETE"})
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}
```

#### 3.4.3. 使用注解 (局部跨域)

在控制器(类上)上使用注解 @CrossOrigin:，表示该类的所有方法允许跨域。

```
@RestController
@CrossOrigin(origins = "*")
public class HelloController {
    @RequestMapping("/hello")
    public String hello() {
        return "hello world";
    }
}
```

在方法上使用注解 @CrossOrigin:

```
@RequestMapping("/hello")
    @CrossOrigin(origins = "*")
     //@CrossOrigin(value = "http://localhost:8081") //指定具体ip允许跨域
    public String hello() {
        return "hello world";
    }
```

#### 3.4.4. 手动设置响应头(局部跨域)

使用 HttpServletResponse 对象添加响应头(Access-Control-Allow-Origin)来授权原始域，这里 Origin的值也可以设置为 “*”,表示全部放行。推荐：[150道常见的Java面试题分解汇总](http://mp.weixin.qq.com/s?__biz=MzU2MTI4MjI0MQ==&mid=2247493168&idx=3&sn=4a2eb3c0ad574dd58bda8664746aae00&chksm=fc798b9ecb0e0288697af194f241f3c3da73381f69116f72db3a0fdf92b6aa4e19dd3c5c285d&scene=21#wechat_redirect)

```
@RequestMapping("/index")
public String index(HttpServletResponse response) {
    response.addHeader("Access-Allow-Control-Origin","*");
    return "index";
}
```

#### 3.4.5. 使用自定义filter实现跨域

首先编写一个过滤器，可以起名字为MyCorsFilter.java

```
package com.mesnac.aop;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
@Component
public class MyCorsFilter implements Filter {
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) res;
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader("Access-Control-Allow-Headers", "x-requested-with,content-type");
    chain.doFilter(req, res);
  }
  public void init(FilterConfig filterConfig) {}
  public void destroy() {}
}
```

在web.xml中配置这个过滤器，使其生效

```
<!-- 跨域访问 START-->
<filter>
 <filter-name>CorsFilter</filter-name>
 <filter-class>com.mesnac.aop.MyCorsFilter</filter-class>
</filter>
<filter-mapping>
 <filter-name>CorsFilter</filter-name>
 <url-pattern>/*</url-pattern>
</filter-mapping>
<!-- 跨域访问 END  -->
```





## 4.为什么 SpringBoot 的 jar 可以直接运行？

SpringBoot提供了一个插件spring-boot-maven-plugin用于把程序打包成一个可执行的jar包。在pom文件里加入这个插件即可：

```
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

打包完生成的executable-jar-1.0-SNAPSHOT.jar内部的结构如下：

```
├── META-INF
│   ├── MANIFEST.MF
│   └── maven
│       └── spring.study
│           └── executable-jar
│               ├── pom.properties
│               └── pom.xml
├── lib
│   ├── aopalliance-1.0.jar
│   ├── classmate-1.1.0.jar
│   ├── spring-boot-1.3.5.RELEASE.jar
│   ├── spring-boot-autoconfigure-1.3.5.RELEASE.jar
│   ├── ...
├── org
│   └── springframework
│       └── boot
│           └── loader
│               ├── ExecutableArchiveLauncher$1.class
│               ├── ...
└── spring
    └── study
        └── executablejar
            └── ExecutableJarApplication.class
```

然后可以直接执行jar包就能启动程序了：

```
java -jar executable-jar-1.0-SNAPSHOT.jar
```

打包出来fat jar内部有4种文件类型：

- META-INF文件夹：程序入口，其中MANIFEST.MF用于描述jar包的信息
- lib目录：放置第三方依赖的jar包，比如springboot的一些jar包
- spring boot loader相关的代码
- 模块自身的代码

MANIFEST.MF文件的内容：

```
Manifest-Version: 1.0
Implementation-Title: executable-jar
Implementation-Version: 1.0-SNAPSHOT
Archiver-Version: Plexus Archiver
Built-By: Format
Start-Class: spring.study.executablejar.ExecutableJarApplication
Implementation-Vendor-Id: spring.study
Spring-Boot-Version: 1.3.5.RELEASE
Created-By: Apache Maven 3.2.3
Build-Jdk: 1.8.0_20
Implementation-Vendor: Pivotal Software, Inc.
Main-Class: org.springframework.boot.loader.JarLauncher
```

我们看到，它的Main-Class是org.springframework.boot.loader.JarLauncher，当我们使用java -jar执行jar包的时候会调用JarLauncher的main方法，而不是我们编写的SpringApplication。

那么JarLauncher这个类是的作用是什么的？

它是SpringBoot内部提供的工具Spring Boot Loader提供的一个用于执行Application类的工具类(fat jar内部有spring loader相关的代码就是因为这里用到了)。相当于Spring Boot Loader提供了一套标准用于执行SpringBoot打包出来的jar

### 4.1.Spring Boot Loader抽象的一些类

抽象类Launcher：各种Launcher的基础抽象类，用于启动应用程序；跟Archive配合使用；目前有3种实现，分别是JarLauncher、WarLauncher以及PropertiesLauncher

Archive：归档文件的基础抽象类。JarFileArchive就是jar包文件的抽象。它提供了一些方法比如getUrl会返回这个Archive对应的URL；getManifest方法会获得Manifest数据等。ExplodedArchive是文件目录的抽象

JarFile：对jar包的封装，每个JarFileArchive都会对应一个JarFile。JarFile被构造的时候会解析内部结构，去获取jar包里的各个文件或文件夹，这些文件或文件夹会被封装到Entry中，也存储在JarFileArchive中。如果Entry是个jar，会解析成JarFileArchive。

比如一个JarFileArchive对应的URL为：

```
jar:file:/Users/format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/
```

它对应的JarFile为：

```
/Users/format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar
```

这个JarFile有很多Entry，比如：

```
META-INF/
META-INF/MANIFEST.MF
spring/
spring/study/
....
spring/study/executablejar/ExecutableJarApplication.class
lib/spring-boot-starter-1.3.5.RELEASE.jar
lib/spring-boot-1.3.5.RELEASE.jar
...
```

JarFileArchive内部的一些依赖jar对应的URL(SpringBoot使用org.springframework.boot.loader.jar.Handler处理器来处理这些URL)：

```
jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-starter-web-1.3.5.RELEASE.jar!/

jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-loader-1.3.5.RELEASE.jar!/org/springframework/boot/loader/JarLauncher.class
```

我们看到如果有jar包中包含jar，或者jar包中包含jar包里面的class文件，那么会使 用 !/ 分隔开，这种方式只有org.springframework.boot.loader.jar.Handler能处 理，它是SpringBoot内部扩展出来的一种URL协议。

### 4.2.JarLauncher的执行过程

JarLauncher的main方法：

```
public static void main(String[] args) {
    // 构造JarLauncher，然后调用它的launch方法。参数是控制台传递的
    new JarLauncher().launch(args);
}  
```

JarLauncher被构造的时候会调用父类ExecutableArchiveLauncher的构造方法。

ExecutableArchiveLauncher的构造方法内部会去构造Archive，这里构造了JarFileArchive。构造JarFileArchive的过程中还会构造很多东西，比如JarFile，Entry …

```
JarLauncher的launch方法：
protected void launch(String[] args) {
  try {
    // 在系统属性中设置注册了自定义的URL处理器：org.springframework.boot.loader.jar.Handler。如果URL中没有指定处理器，会去系统属性中查询
    JarFile.registerUrlProtocolHandler();
    // getClassPathArchives方法在会去找lib目录下对应的第三方依赖JarFileArchive，同时也会项目自身的JarFileArchive
    // 根据getClassPathArchives得到的JarFileArchive集合去创建类加载器ClassLoader。这里会构造一个LaunchedURLClassLoader类加载器，这个类加载器继承URLClassLoader，并使用这些JarFileArchive集合的URL构造成URLClassPath
    // LaunchedURLClassLoader类加载器的父类加载器是当前执行类JarLauncher的类加载器
    ClassLoader classLoader = createClassLoader(getClassPathArchives());
    // getMainClass方法会去项目自身的Archive中的Manifest中找出key为Start-Class的类
    // 调用重载方法launch
    launch(args, getMainClass(), classLoader);
  }
  catch (Exception ex) {
    ex.printStackTrace();
    System.exit(1);
  }
}

// Archive的getMainClass方法
// 这里会找出spring.study.executablejar.ExecutableJarApplication这个类
public String getMainClass() throws Exception {
  Manifest manifest = getManifest();
  String mainClass = null;
  if (manifest != null) {
    mainClass = manifest.getMainAttributes().getValue("Start-Class");
  }
  if (mainClass == null) {
    throw new IllegalStateException(
        "No 'Start-Class' manifest entry specified in " + this);
  }
  return mainClass;
}

// launch重载方法
protected void launch(String[] args, String mainClass, ClassLoader classLoader)
    throws Exception {
      // 创建一个MainMethodRunner，并把args和Start-Class传递给它
  Runnable runner = createMainMethodRunner(mainClass, args, classLoader);
      // 构造新线程
  Thread runnerThread = new Thread(runner);
      // 线程设置类加载器以及名字，然后启动
  runnerThread.setContextClassLoader(classLoader);
  runnerThread.setName(Thread.currentThread().getName());
  runnerThread.start();
}
```

MainMethodRunner的run方法：

```
@Override
public void run() {
  try {
    // 根据Start-Class进行实例化
    Class<?> mainClass = Thread.currentThread().getContextClassLoader()
        .loadClass(this.mainClassName);
    // 找出main方法
    Method mainMethod = mainClass.getDeclaredMethod("main", String[].class);
    // 如果main方法不存在，抛出异常
    if (mainMethod == null) {
      throw new IllegalStateException(
          this.mainClassName + " does not have a main method");
    }
    // 调用
    mainMethod.invoke(null, new Object[] { this.args });
  }
  catch (Exception ex) {
    UncaughtExceptionHandler handler = Thread.currentThread()
        .getUncaughtExceptionHandler();
    if (handler != null) {
      handler.uncaughtException(Thread.currentThread(), ex);
    }
    throw new RuntimeException(ex);
  }
}
```

Start-Class的main方法调用之后，内部会构造Spring容器，启动内置Servlet容器等过程。这些过程我们都已经分析过了。

### 4.3.关于自定义的类加载器LaunchedURLClassLoader

LaunchedURLClassLoader重写了loadClass方法，也就是说它修改了默认的类加载方式(先看该类是否已加载这部分不变，后面真正去加载类的规则改变了，不再是直接从父类加载器中去加载)。LaunchedURLClassLoader定义了自己的类加载规则：

```
private Class<?> doLoadClass(String name) throws ClassNotFoundException {

  // 1) Try the root class loader
  try {
    if (this.rootClassLoader != null) {
      return this.rootClassLoader.loadClass(name);
    }
  }
  catch (Exception ex) {
    // Ignore and continue
  }

  // 2) Try to find locally
  try {
    findPackage(name);
    Class<?> cls = findClass(name);
    return cls;
  }
  catch (Exception ex) {
    // Ignore and continue
  }

  // 3) Use standard loading
  return super.loadClass(name, false);
}
```

加载规则：

- 如果根类加载器存在，调用它的加载方法。这里是根类加载是ExtClassLoader
- 调用LaunchedURLClassLoader自身的findClass方法，也就是URLClassLoader的findClass方法
- 调用父类的loadClass方法，也就是执行默认的类加载顺序(从BootstrapClassLoader开始从下往下寻找)

LaunchedURLClassLoader自身的findClass方法：

```
protected Class<?> findClass(final String name)
     throws ClassNotFoundException
{
    try {
        return AccessController.doPrivileged(
            new PrivilegedExceptionAction<Class<?>>() {
                public Class<?> run() throws ClassNotFoundException {
                    // 把类名解析成路径并加上.class后缀
                    String path = name.replace('.', '/').concat(".class");
                    // 基于之前得到的第三方jar包依赖以及自己的jar包得到URL数组，进行遍历找出对应类名的资源
                    // 比如path是org/springframework/boot/loader/JarLauncher.class，它在jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-loader-1.3.5.RELEASE.jar!/中被找出
                    // 那么找出的资源对应的URL为jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-loader-1.3.5.RELEASE.jar!/org/springframework/boot/loader/JarLauncher.class
                    Resource res = ucp.getResource(path, false);
                    if (res != null) { // 找到了资源
                        try {
                            return defineClass(name, res);
                        } catch (IOException e) {
                            throw new ClassNotFoundException(name, e);
                        }
                    } else { // 找不到资源的话直接抛出ClassNotFoundException异常
                        throw new ClassNotFoundException(name);
                    }
                }
            }, acc);
    } catch (java.security.PrivilegedActionException pae) {
        throw (ClassNotFoundException) pae.getException();
    }
}
```

下面是LaunchedURLClassLoader的一个测试：

```
// 注册org.springframework.boot.loader.jar.Handler URL协议处理器
JarFile.registerUrlProtocolHandler();
// 构造LaunchedURLClassLoader类加载器，这里使用了2个URL，分别对应jar包中依赖包spring-boot-loader和spring-boot，使用 "!/" 分开，需要org.springframework.boot.loader.jar.Handler处理器处理
LaunchedURLClassLoader classLoader = new LaunchedURLClassLoader(
        new URL[] {
                new URL("jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-loader-1.3.5.RELEASE.jar!/")
                , new URL("jar:file:/Users/Format/Develop/gitrepository/springboot-analysis/springboot-executable-jar/target/executable-jar-1.0-SNAPSHOT.jar!/lib/spring-boot-1.3.5.RELEASE.jar!/")
        },
        LaunchedURLClassLoaderTest.class.getClassLoader());

// 加载类
// 这2个类都会在第二步本地查找中被找出(URLClassLoader的findClass方法)
classLoader.loadClass("org.springframework.boot.loader.JarLauncher");
classLoader.loadClass("org.springframework.boot.SpringApplication");
// 在第三步使用默认的加载顺序在ApplicationClassLoader中被找出
classLoader.loadClass("org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration");
```

### 4.4.Spring Boot Loader的作用

SpringBoot在可执行jar包中定义了自己的一套规则，比如第三方依赖jar包在/lib目录下，jar包的URL路径使用自定义的规则并且这个规则需要使用org.springframework.boot.loader.jar.Handler处理器处理。它的Main-Class使用JarLauncher，如果是war包，使用WarLauncher执行。这些Launcher内部都会另起一个线程启动自定义的SpringApplication类。

这些特性通过spring-boot-maven-plugin插件打包完成。



## 5.如何让Spring Boot 的配置动起来？

看下`Config`的源码，代码关键部分在`org.springframework.cloud.context.refresh.ContextRefresher#refresh()`方法中，如下图：

![image-20220825213432213](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220825213432.png)

因此只需要在修改属性之后调用下`ContextRefresher#refresh()`（异步，避免一直阻塞等待）方法即可。

为了方便测试，我们自己手动写一个refresh接口，如下：

```java
@GetMapping("/show/refresh")
    public String refresh(){
        //修改配置文件中属性
        HashMap<String, Object> map = new HashMap<>();
        map.put("config.version",99);
        map.put("config.app.name","appName");
        map.put("config.platform","ORACLE");
        MapPropertySource propertySource=new MapPropertySource("dynamic",map);
        //将修改后的配置设置到environment中
        environment.getPropertySources().addFirst(propertySource);
        //异步调用refresh方法，避免阻塞一直等待无响应
        new Thread(() -> contextRefresher.refresh()).start();
        return "success";
    }
```

> 上述代码中作者只是手动设置了配置文件中的值，实际项目中可以通过持久化的方式从数据库中读取配置刷新。

下面我们测试看看，启动项目，访问`http://localhost:8080/show/version`，发现是之前配置在`application.properties`中的值，如下图：

![image-20220825213534492](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220825213534.png)

调用`refresh`接口：`http://localhost:8080/show/refresh`重新设置属性值；

再次调用`http://localhost:8080/show/version`查看下配置是否修改了，如下图：

![image-20220825213549429](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20220825213549.png)

从上图可以发现，配置果然修改了，达到了动态刷新的效果。



## 6.SpringBoot静态获取 bean的三种方式

**方式一  注解@PostConstruct**

```
import com.example.javautilsproject.service.AutoMethodDemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
 
import javax.annotation.PostConstruct;
 
/**
 * springboot静态方法获取 bean 的三种方式(一)
 * @author: clx
 * @version: 1.1.0
 */
@Component
public class StaticMethodGetBean_1 {
 
    @Autowired
    private AutoMethodDemoService autoMethodDemoService;
 
    @Autowired
    private static AutoMethodDemoService staticAutoMethodDemoService;
 
    @PostConstruct
    public void init() {
        staticAutoMethodDemoService = autoMethodDemoService;
    }
 
    public static String getAuthorizer() {
        return staticAutoMethodDemoService.test();
    }
}
```

注解@PostConstruct说明

PostConstruct 注释用于在依赖关系注入完成之后需要执行的方法上，以执行任何初始化。此方法必须在将类放入服务之前调用。支持依赖关系注入的所有类都必须支持此注释。即使类没有请求注入任何资源，用 PostConstruct 注释的方法也必须被调用。只有一个方法可以用此注释进行注释。

应用 PostConstruct 注释的方法必须遵守以下所有标准：

- 该方法不得有任何参数，除非是在 EJB 拦截器 (interceptor) 的情况下，根据 EJB 规范的定义，在这种情况下它将带有一个 InvocationContext 对象 ；
- 该方法的返回类型必须为 void；
- 该方法不得抛出已检查异常；
- 应用 PostConstruct 的方法可以是 public、protected、package private 或 private；
- 除了应用程序客户端之外，该方法不能是 static；
- 该方法可以是 final；
- 如果该方法抛出未检查异常，那么不得将类放入服务中，除非是能够处理异常并可从中恢复的 EJB。

**方式二  启动类ApplicationContext**

实现方式：在springboot的启动类中，定义static变量ApplicationContext，利用容器的getBean方法获得依赖对象

```
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
/**
 * @author: clx
 * @version: 1.1.0
 */
@SpringBootApplication
public class Application {
    public static ConfigurableApplicationContext ac;
    public static void main(String[] args) {
       ac = SpringApplication.run(Application.class, args);
    }
 
}
```

调用方式

```
/**
 * @author: clx
 * @version: 1.1.0
 */
@RestController
public class TestController {
    /**
     * 方式二
     */
    @GetMapping("test2")
    public void method_2() {
        AutoMethodDemoService methodDemoService = Application.ac.getBean(AutoMethodDemoService.class);
        String test2 = methodDemoService.test2();
        System.out.println(test2);
    }
}
```

**方式三 手动注入ApplicationContext**

手动注入ApplicationContext

```
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
 
 
/**
 * springboot静态方法获取 bean 的三种方式(三)
 * @author: clx
 * @version: 1.1.0
 */
@Component
public class StaticMethodGetBean_3<T> implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StaticMethodGetBean_3.applicationContext = applicationContext;
    }
 
    public static <T> T  getBean(Class<T> clazz) {
        return applicationContext != null?applicationContext.getBean(clazz):null;
    }
}
```

调用方式

```
    /**
     * 方式三
     */
    @Test
    public void method_3() {
        AutoMethodDemoService autoMethodDemoService = StaticMethodGetBean_3.getBean(AutoMethodDemoService.class);
        String test3 = autoMethodDemoService.test3();
        System.out.println(test3);
    }
```



## 7.SpringBoot的自动配置

在介绍`SpringBoot`的自动配置之前，先了解下注解`@Import`的使用，`SpringBoot`的`@Enable*`开头的注解底层依赖于`@Import`注解导入一些类，使用`@Import`导入的类会被`Spring`加载到`IOC`容器中，而`@Import`提供了以下4中用法：

- 直接导入`Bean`
- 通过配置类导入`Bean`
- 导入`ImportSelector`实现类,一般用于加载配置文件的类
- 导入`ImportBeanDefinitionRegistrar`实现类

下面来分别介绍这几种用法。

- 直接导入Bean就比较简单了，新建一个`User`类

```none
public class User{
    private String name;
    private String address;
}
```

然后在启动类上使用`@Import`注解导入即可

```none
@SpringBootApplication
@Import(User.class)
public class Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class,args);
        System.out.println(context.getBean(User.class));
    }
}
```

> 这里需要注意的是，通过上下文获取Bean时，需要使用Bean的class，因为通过Bean的方式导入，Spring存入IOC容器，是用类的全类名存储的。可以使用上下文的`getBeansOfType`方法查看，返回的是Map对象。

```none
{com.tenghu.sbc.entity.User=User(name=null, age=0)}
```

从返回的结果可以看出，`key`就是存的`User`的全类名。

- 通过配置类导入`Bean`，创建一个配置类;

```none
public class UserConfig {
    @Bean(name = "user")
    public User user(){
        return new User();
    }
}
```

然后通过`@Import`导入这个配置类

```none
@SpringBootApplication
@Import(UserConfig.class)
public class Application {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class,args);
        System.out.println(context.getBean(User.class));
    }
}
```

通过配置类的方式可以在配置类里面定义多个`Bean`，当导入配置类时，配置类下定义的`Bean`都会被导入。

- 导入`ImportSelector`实现类

```none
public class MyImportSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{User.class.getName()};
    }
}
```

实现`ImportSelector`类，必须实现`selectImports`，然后返回需要导入的`Bean`。与上面一样使用`@Import`导入这个实现类。

```none
@Import(MyImportSelector.class)
```

- 导入`ImportBeanDefinitionRegistrar`实现类

```none
public class MyImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(User.class).getBeanDefinition();
        registry.registerBeanDefinition("user",beanDefinition);
    }
}
```

使用方式一样，通过`@Import`导入

```none
@Import(MyImportBeanDefinitionRegistrar.class)
```

了解完`@Import`的使用，接下来可以来看下`SpringBoot`的自动配置是怎么处理的。从上面的启动类，使用`SpringBoot`就用了一个注解`@SpringBootApplication`，可以打开这个注解的源码看下：

```none
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
    excludeFilters = {@Filter(
    type = FilterType.CUSTOM,
    classes = {TypeExcludeFilter.class}
), @Filter(
    type = FilterType.CUSTOM,
    classes = {AutoConfigurationExcludeFilter.class}
)}
)
public @interface SpringBootApplication
```

用到这样一个注解`@EnableAutoConfiguration`注解。底层使用`@Import`导入上面第三种方式`AutoConfigurationImportSelector`。

```none
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@Import({AutoConfigurationImportSelector.class})
public @interface EnableAutoConfiguration {
    String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";

    Class<?>[] exclude() default {};

    String[] excludeName() default {};
}
```

进入源码找到实现了`selectImports`方法

```none
public String[] selectImports(AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return NO_IMPORTS;
    } else {
        AutoConfigurationImportSelector.AutoConfigurationEntry autoConfigurationEntry = this.getAutoConfigurationEntry(annotationMetadata);
        return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
    }
}
```

通过调用方法`getAutoConfigurationEntry`

```none
protected AutoConfigurationImportSelector.AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
    if (!this.isEnabled(annotationMetadata)) {
        return EMPTY_ENTRY;
    } else {
        AnnotationAttributes attributes = this.getAttributes(annotationMetadata);
        List<String> configurations = this.getCandidateConfigurations(annotationMetadata, attributes);
        configurations = this.removeDuplicates(configurations);
        Set<String> exclusions = this.getExclusions(annotationMetadata, attributes);
        this.checkExcludedClasses(configurations, exclusions);
        configurations.removeAll(exclusions);
        configurations = this.getConfigurationClassFilter().filter(configurations);
        this.fireAutoConfigurationImportEvents(configurations, exclusions);
        return new AutoConfigurationImportSelector.AutoConfigurationEntry(configurations, exclusions);
    }
}
```

这里主要的看调用这个方法`getCandidateConfigurations`，返回的就是要自动加载的`Bean`

```none
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
    List<String> configurations = SpringFactoriesLoader.loadFactoryNames(this.getSpringFactoriesLoaderFactoryClass(), this.getBeanClassLoader());
    Assert.notEmpty(configurations, "No auto configuration classes found in META-INF/spring.factories. If you are using a custom packaging, make sure that file is correct.");
    return configurations;
}
```

通过`META-INF/spring.factories`配置文件里的`EnableAutoConfiguration`获取配置的`Bean`

```none
# Auto Configure
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.springframework.boot.autoconfigure.admin.SpringApplicationAdminJmxAutoConfiguration,\
org.springframework.boot.autoconfigure.aop.AopAutoConfiguration,\
org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,\
org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration,\
org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration,\
org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration,\
.....
```

太多了，有兴趣的可以查看`Spring`的`xxx-autoconfigure`包。将读取到的配置最终返回给`selectImports`，然后通过工具类`StringUtils.toStringArray`转换为字符串数组返回给`@Import`，从而实现自动配置。第三方包只要是`xxx-autoconfigure`结尾的包，`META-INF`都有`spring.factories`，这个名字是固定写法。都可以被`SpringBoot`识别并且进行自动配置，前提是需要配置到`org.springframework.boot.autoconfigure.EnableAutoConfiguration`下。
从以上总结来看，`SpringBoot`的自动配置原理如下：

- `@EnableAutoConfiguration`注解内部使用`Import(AutoConfigurationImportSelector.class)`来加载配置类
- 通过配置文件：`META-INF/spring.factories`，配置大量的配置类，`SpringBoot`启动时就会自动加载这些类并初始化的`Bean`。

这里需要说明一点，并不是所有配置到配置文件的`Bean`都会被初始化，需要符合配置类中使用`Condition`来加载满足条件的`Bean`。比如我们打开`RedisAutoConfiguration`的源码查看：

```none
@Configuration(
    proxyBeanMethods = false
)
@ConditionalOnClass({RedisOperations.class})
@EnableConfigurationProperties({RedisProperties.class})
@Import({LettuceConnectionConfiguration.class, JedisConnectionConfiguration.class})
public class RedisAutoConfiguration {
    public RedisAutoConfiguration() {
    }

    @Bean
    @ConditionalOnMissingBean(
        name = {"redisTemplate"}
    )
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
```

类上面有这么个注解`@ConditionalOnClass({RedisOperations.class})`，意思就是需要`RedisOperations`类存在的情况下，才自动加载；这还不算完，继续查看下面的方法上有个`@ConditionalOnMissingBean(name = {"redisTemplate"})`，这里的意思是，当其他地方没有`redisTemplate`实例化这个`Bean`时，才自动加载。符合这两个条件，`SpringBoot`才会进行自动加载并初始化。