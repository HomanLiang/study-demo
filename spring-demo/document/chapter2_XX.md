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





