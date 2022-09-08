[toc]



# Spring MVC

## 1 概述

### 1.1 Spring MVC特点

- 注解驱动：`Spring MVC` 通过一套 `MVC` 注解，让 `POJO` 成为处理请求的控制器，无需实现任何接口
- `REST` 风格：`Spring MVC` 支持 `REST` 风格的 `URL` 请求

### 1.2 核心内容

`Spring MVC` 框架围绕 `DispatcherServlet` 这个核心展开，它负责截获请求并将其分派给相应的处理器处理。`Spring MVC` 框架包括注解驱动控制器、请求及响应的信息处理、视图解析、本地化解析、上传文件解析、异常处理以及表单标签绑定等内容。

### 1.3 体系结构

`Spring MVC` 是基于 `Model 2` 实现的技术框架。`Spring MVC` 通过一个 `DispatcherServlet` 接收所有请求，并将具体工作委托给其他组件进行处理。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223649.webp)

1. 客户端发出一个 `HTTP` 请求，`Web` 应用服务器接收到这个请求，如果匹配 `DispatcherServlet` 的请求映射路径(在 `web.xml` 中指定)，`Web` 容器将该请求转交给 `DispatcherServlet` 处理。
2. `DispatcherServlet` 接收到这个请求后，将根据请求的信息（包括 `URL`、`HTTP` 方法、请求报文头、请求参数、`Cookie` 等）及`HandlerMapping` 的配置找到处理请求的处理器（`Handler`）。可将 `HandlerMapping` 看成路由控制器，将 `Handler` 看成目标主机。值得注意的是：`Spring MVC` 中并没有定义一个 `Handler` 接口，实际上任何一个 `Object` 都可以成为请求处理器。
3. 当 `DispatcherServlet` 根据 `HandlerMapping` 得到对应当前请求的 `Handler` 后，通过 `HandlerAdapter` 对 `Handler` 进行封装，再以统一的适配器接口调用 `Handler`。`HandlerAdapter` 是 `Spring MVC` 的框架级接口，顾名思义 `HandlerAdapter` 是一个适配器，它用统一的接口对各种 `Handler` 方法进行调用。
4. 处理器完成业务逻辑的处理后将运回一个 `ModelAndView` 给 `DispatcherServlet`，`ModelAndView` 包含了视图逻辑名和模型数据信息。
5. `ModelAndView` 中包含的是“逻辑视图名”而非真正的视图对象，`DispatcherServlet` 借由 `ViewResolver`完成逻辑视图名到真实视图对象的解析工作。
6. 当得到真实的视图对象 `View` 后，`DispatcherServlet`就使用这个 `View` 对象对 `ModelAndView`中的模型数据进行视图渲染。
7. 最终客户端得到的响应消息，可能是一个普通的 `HTML` 页而，也可能是一个 `XML` 或 `JSON` 串，   甚至是一张图片或一个 `PDF` 文档等不同的媒体形式。

### 1.4 配置DispatcherServlet

可以在 `web.xml` 中配置一个 `Servlet`，并通过 `<servlet-mapping>` 指定其处理的 `URL`。

```xml
<web-app xmlns="http://java.sun.com/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
         http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd" version="3.0">

    <!-- (1)从类路径下加载Spring配置文件-->
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:application-context.xml</param-value>
    </context-param>

    <!-- (2)负责启动 Spring 容器的监听器，它将引用(1)处的上下文参数获得Spring配置文件的地址 -->
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>

    <!-- (3)配置DispatcherServlet -->
    <servlet>
        <servlet-name>web</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    
    <!-- (4)指定处理的URL路径 -->
    <servlet-mapping>
        <servlet-name>web</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

</web-app>
```

1. 在(1)处，通过 `contextConfigLocation` 参数指定业务层 `Spring` 容器的配置文件（多个配置文件用`,`分割）。
2. 在(2)处，`ContextLoaderListener` 是一个 `ServletLoaderListener`，它通过 `contextConfigLocation` 指定的 `Spring` 配置文件启动业务层的 `Spring` 容器。
3. 在(3)处，配置了名为 `web` 的 `DispatcherServlet`，它默认加载 `/WEB-INF/web-servlet.xml`（`<servlet-name>-servlet.xml`）的 `Spring` 配置文件，启动 `Web` 层的 `Spring` 容器。`Web` 层容器将作为业务层容器的子容器，`Web` 层容器可以访问业务层容器的 `Bean`，而业务层容器访问不了 `Web` 层容器的 `Bean`。
4. 在(4)处，通过 `<servlet-mapping>` 指定 `DispatcherServlet` 处理 `/*` 全部的 `HTTP` 请求。一个 `web.xml` 可以配置多个`DispatcherServlet` ，通过其对应的 `<servlet-mapping>` 配置，让每个 `DispatcherServlet` 处理不同的请求。

**DispatcherServlet 的配置参数**

可以通过 `<servlet>` 的 `<init-param>` 属性指定配置参数：

1. `namespace` 参数：`DispatcherServlet` 对应的命名空间，默认是`WEB-INF/<servlet-name>-servlet.xml`。在显式配置该参数后，新的配置文件对应的路径是`WEB-INF/<namespace>.xml`，例如如果将 `namespace` 设置为 `sample`，则对应的 `Spring` 配置文件为 `WEB-INF/sample.xml`。
2. `contextConfigLocation`：如果 `DispatcherServlet` 上下文对应的 `Spring` 配置文件有多个，则可以使用该属性按照 `Spring` 资源路径的方式指定，如`classpath:sample1.xml`,`classpath:sample2.xml`。
3. `publishContext`：默认为 `true`。`DispatcherServlet` 根据该属性决定是否将 `WebApplicationContext` 发布到 `ServletContext` 的属性列表中，方便调用者可借由 `ServletContext` 找到 `WebApplicationContext` 实例，对应的属性名为`DispatcherServlet#getServletContextAttributeName()`的返回值。
4. `publishEvents`：默认为 `true`。当 `DispatcherServlet` 处理完一个请求后，是否需要向容器发布一个`ServletRequestHandleEvent`事件。

**Spring容器配置**

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans-4.0.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context-4.0.xsd
       http://www.springframework.org/schema/mvc
       http://www.springframework.org/schema/mvc/spring-mvc-4.0.xsd">

    <context:component-scan base-package="com.ankeetc.web"/>
    <!-- 会自动注册RequestMappingHandlerMapping与RequestMappingHandlerAdapter两个Bean，这是SpringMVC为@Controllers分发请求所必需的 -->
    <!-- 并提供了数据绑定支持、@NumberFormatannotation支持、 @DateTimeFormat支持、@Valid支持、读写XML的支持和读写JSON的支持等功能。 -->
    <mvc:annotation-driven />

</beans>
```

### 1.5 基于编程的配置

`Spring 4.0` 已经全面支持 `Servlet 3.0`，可以使用编程的方式配置 `Servlet` 容器。在 `Servlet 3.0` 环境中，容器会在类路径中查找实现 `javax.servlet.ServletContainerInitializer` 接口的类，如果发现实现类，就会用它来配置 `Servlet` 容器。`Spring` 提供了这个接口的实现，名为 `SpringServletContainerInitializer`，这个类反过来又查找实现 `WebApplicationInitializer` 的类并将配置的任务交给它们来完成。Spring还提供了一个 `WebApplicationInitializer` 基础实现类`AbstractAnnotationConfigDispatcherServletInitializer`，使得它在注册 `DispatcherServlet` 时只需要简单地指定它的 `Servlet` 映射即可。

```java
public class WebApplicationInitilalizer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        ServletRegistration.Dynamic registration = servletContext.addServlet("web", new DispatcherServlet());
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
```

### 1.6 DispatcherServlet的内部逻辑

```java
    protected void initStrategies(ApplicationContext context) {
        initMultipartResolver(context);
        initLocaleResolver(context);
        initThemeResolver(context);
        initHandlerMappings(context);
        initHandlerAdapters(context);
        initHandlerExceptionResolvers(context);
        initRequestToViewNameTranslator(context);
        initViewResolvers(context);
        initFlashMapManager(context);
    }
```

`DispatcherServlet#initStrategies()` 方法将在 `WebApplicationContext` 初始化后执行，此时 `Spring` 上下文中的 `Bean` 已经初始化完毕，该方法通过反射查找并装配 `Spring` 容器中用户自定义的 `Bean`，如果找不到就装配默认的组件实例。

**默认组件**

在 `DispatcherServlet.properties` 配置文件里边，指定了 `DispatcherServlet` 所使用的默认组件。如果用户希望采用非默认的组件，只需在 `Spring` 配置文件中配置自定义的组件 `Bean` 即可。

```prop
# 本地化解析器
org.springframework.web.servlet.LocaleResolver=org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver

# 主题解析器
org.springframework.web.servlet.ThemeResolver=org.springframework.web.servlet.theme.FixedThemeResolver

# 处理器解析器
org.springframework.web.servlet.HandlerMapping=org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping,\
    org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping

# 处理器适配器
org.springframework.web.servlet.HandlerAdapter=org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter,\
    org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter,\
    org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter

# 异常处理器
org.springframework.web.servlet.HandlerExceptionResolver=org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver,\
    org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver,\
    org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver

# 视图名称处理器
org.springframework.web.servlet.RequestToViewNameTranslator=org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator

# 视图解析器
org.springframework.web.servlet.ViewResolver=org.springframework.web.servlet.view.InternalResourceViewResolver

org.springframework.web.servlet.FlashMapManager=org.springframework.web.servlet.support.SessionFlashMapManager
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223832.webp)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223853.webp)

## 2.注解驱动的控制器

### 2.1.@RequestMapping映射请求

1. 在 `POJO` 类上标注 `@Controller`，再通过 `<context:component-scan>` 扫描到该类，可以是 `POJO` 成为一个能处理 `HTTP` 请求的控制器。
2. 在控制器的类定义和方法定义处都可以使用 `@RequestMapping` 映射对应的处理方法。
3. `@RequestMapping` 不但支持标准的 `URL`，还支持 `Ant` 风格和 `{XXX}` 占位符的 `URL`。
4. `@RequestMapping` 和 `value`、`method`、`params` 及 `headers` 分别表示请求路径、请求方法、请求参数及报文头的映射条件。

### 2.2.获取请求内容

1. 可以通过 `@RequestParam`、`@RequestHeader`、`@PathVariable` 获取HTTP请求信息。
2. 可以使用 `@CookieValue` 让方法入参绑定某个 `Cookie` 值
3. 可以使用 `@MatrixVariable` 注解将请求中的矩阵变量绑定到处理器的方法参数中。
4. 可以使用命令/表单对象（就是一个 `POJO`）绑定请求参数值，Spring会按照请求参数名和对象属性名匹配的方式，自动为该对象填充属性。
5. 可以使用 `Servlet API` 的类作为处理方法的入参，如 `HttpServletRequest`、`HttpServletResponse`、`HttpSession`；如果使用 `HttpServletResponse` 返回相应，则处理方法返回着设置成 `void` 即可；在`org.springframework.web.context.request`定义了若干个可代理 `Servlet` 原生 `API` 类的接口，如 `WebRequest` 和 `NativeWebRequest`。
6. 可以使用 `java.io` 中的 `InputStream`、`Reader`、`OutputStream`、`Writer` 作为方法的入参。
7. 还可以使用 `java.util.Locale`、`java.security.Principal`作为入参。

### 2.3.使用HttpMessageConverter

`HttpMessageConverter` 接口可以将请求信息转换为一个对象（类型为T），并将对象（类型为T）绑定到请求方法的参数中或输出为响应信息。`DispatcherServlet` 默认已经安装了 `RequestMethodHandlerAdapter` 作为 `HandlerAdapter` 组件的实现类，`HttpMessageConverter` 即由 `RequestMethodHandlerAdapter` 使用，将请求信息转换为对象，或将对象转换为响应信息。

**2.3.1.HttpMessageConverter的实现类**

`Spring` 为 `HttpMessageConverter` 提供了众多的实现类：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223942.webp)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223959.webp)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330224014.webp)

**2.3.2.默认的HttpMessageConverter**

`RequestMappingHandlerAdapter` 已经默认装配了以下的 `HttpMessageConverter`：

- `StringHttpMessageConverter`
- `ByteArrayHttpMessageConverter`
- `SourceHttpMessageConverter`
- `AllEncompassingFormHttpMessageConverter`

**2.3.3.装配其他类型的HttpMessageConverter**

如果需要装配其他类型的 `HttpMessageConverter`，可以在 `Spring` 的 `Web` 容器上下文中自行定义一个`RequestMappingHandlerAdapter`，注册若干 `HttpMessageConverter`。如果在 `Spring web` 容器中显式定义了一个`RequestMappingHandlerAdapter`，则 `Spring MVC` 将使用它**覆盖**默认的 `RequestMappingHandlerAdapter`。

**2.3.4.使用HttpMessageConverter**

1. 可以使用`@RequestBody`、`@ResponseBody`对处理方法进行标注
2. 可以使用`HttpEntity<T>`、`ResponseEntity<T>`作为处理方法的入参或返回值

> `RestTemplate` 是 `Spring` 的模板类，可以使用该类调用 `Web` 服务端的服务，它支持 `Rest` 风格的 `URL`。

**2.3.5.结论**

1. 当控制器处理方法使用到`@RequestBody`、`@ResponseBody` 或 `HttpEntity<T>`、`ResponseEntity<T>` 时，`Spring MVC` 才会使用注册的 `HttpMessageConvertor` 对请求、相应消息进行处理。
2. 当控制器处理方法使用到`@RequestBody`、`@ResponseBody` 或 `HttpEntity<T>`、`ResponseEntity<T>`时，`Spring` 首先根据请求头或响应的 `Accept` 属性选择匹配的 `HttpMessageConverter`，进而根据参数类型或泛型类型的过滤得到匹配的 `HttpMessageConverter`，若找不到可用的 `HttpMessageConverter` 将报错。
3. `@RequestBody`、`@ResponseBody` 不需要成对出现。

**2.3.7.处理XML和JSON**

`Spring MVC` 提供了几个处理 `XML` 和 `JSON` 格式的请求、响应消息的 `HttpMessageConverter`：

- `MarshallingHttpMessageConverter`：处理XML
- `Jaxb2RootElementHttpMessageConverter`：处理XML，底层使用JAXB
- `MappingJackson2HttpMessageConverter`：处理JSON格式

只要在 `Spring Web` 容器中为 `RequestMappingHandlerAdapter` 装配好相应的 `HttpMessageConverter`，并在交互中通过请求的`Accept` 指定 `MIME` 类型，`Spring MVC` 就可以是服务器段的处理方法和客户端透明的通过 `XML` 或 `JSON` 格式进行通信。

```xml
    <bean class="org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter">
        <property name="messageConverters">
            <list>
                <bean class="org.springframework.http.converter.StringHttpMessageConverter"/>
                <bean class="org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter"/>
                <bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"/>
            </list>
        </property>
    </bean>
```

### 2.4 使用@RestController

`@RestController` 已经标注了 `@ResponseBody` 和 `@Controller`，可以直接在控制器上标注该注解，就不用在每个`@RequestMapping` 方法上添加 `@ResponseBody` 了。

### 2.5 AsyncRestTemplate

`Spring 4.0` 提供了 `AsyncRestTemplate` 用于以异步无阻塞的方式进行服务访问。

```java
public class WebApplicationInitilalizer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        ServletRegistration.Dynamic registration = servletContext.addServlet("web", new DispatcherServlet());
        registration.setLoadOnStartup(1);
        // 此处要设置为true
        registration.setAsyncSupported(true);
        registration.addMapping("/");
    }
}

@RestController
public class AsyncController {

    @RequestMapping(value = "/async", method = RequestMethod.GET)
    public Callable<String> async() {
        System.out.println("hello!");
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                TimeUnit.SECONDS.sleep(5);
                return "ASYNC";
            }
        };
    }
}
public class Main {
    public static void main(String[] args) {
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();

        ListenableFuture<ResponseEntity<String>> future = asyncRestTemplate.getForEntity("http://localhost:8080/async", String.class);

        System.out.println("return");
        future.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Failure");
            }

            @Override
            public void onSuccess(ResponseEntity<String> result) {
                System.out.println("Success");
            }
        });
    }
}
```

### 2.6 处理模型数据

Spring MVC提供了多种途径输出模型数据：

1. `ModelAndView`：当处理方法返回值类型为 `ModelAndView` 时，方法体即可通过该对象添加模型数据；
2. `@ModelAttribute`：在方法入参标注该注解后，入参的对象就会放到数据模型中；
3. `Map` 和 `Model`：如果方法入参为 `org.framework.ui.Model`、`org.framework.ui.ModelMap`、`java.util.Map`，当处理方法返回时，`Map` 中的数据会自动添加到模型中；
4. `@SessionAttributes`：将模型中的某个属性暂存到 `HttpSession` 中，以便多个请求之间可以共享这个属性。

## 3 处理方法的数据绑定

`Spring` 会根据请求方法签名的不同，将请求中的信息以一定方式转换并绑定到请求方法的入参中，还会进行数据转换、数据格式化及数据校验等。

### 3.1 数据绑定流程

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330224125.webp)

`Spring MVC` 通过反射对目标签名进行分析，将请求消息绑定到处理方法的入参中。数据绑定的核心部件是 `DataBinder`。`Spring MVC`主框架将 `ServletRequest` 对象及处理方法的入参对象实例传递给 `DataBinder`，`DataBinder` 首先调用装配在 `Spring Web` 上下文中的 `ConversionService` 组件进行数据类型转换、数据格式化等工作，将 `ServletRequest` 中的消息填充到入参对象中， 然后调用`Validator` 组件对己经绑定了请求消息数据的入参对象进行数据合法性校验，最 终生成数据绑定结果 `BindingResult` 对象。`BindingResult` 包含了已完成数据绑定的入参 对象，还包含相应的校验错误对象。`Spring MVC` 抽取 `BindingResult` 中的入参对象及校验错误对象，将它们赋给处理方法的相应入参。

### 3.2.数据转换

类型转换模块位于`org.framework.core.convert`包中，同时由于历史原因，`Spring` 还支持JDK的 `PropertyEditor`。

**3.2.1.ConversionService简介**

`ConversionService` 是 Spring 类型转换体系的核心接口，它定义了以下4个方法：

- `boolean canConvert(Class<?> sourceType, Class<?> targetType)`：判断是否可以将一个Java类转换为另一个Java类。
- `Boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType)`：需转换的类将以成员变量的方式出现在宿主类中。`TypeDescriptor` 不但描述了需转换类的信息，还描述了从宿主类的上下文信息，如成员变量上的注解，成员变量是否以数组、集合或Map的方式呈现等。类型转换逻辑可以利用这些信息做出 各种灵活的控制。
- `<T> T convert(Object source, Class<T> targetType)`：将原类型对象转换为目标类型对象。
- `Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType)`：
   将对象从原类型对象转换为目标类型对象，此时往往会用到所在宿主类的上下 文信息。

第一个和第三个接口方法类似于`PmpertyEditor`，它们不关注类型对象所在的上下文 信息，只简单地完成两个类型对象的转换，唯一的区别在于这两个方法支持任意两个类型的转换。而第二个和第四个接口方法会参考类型对象所在宿主类的上下文信息，并利用这些信息进行类型转换。

**3.2.2.使用ConversionService**

可以利用 `org.springframework.context.support.ConversionServiceFactoryBean` 在 `Spring` 的 上下文中定义一个`ConversionService`。`Spring` 将自动识别出上下文中的 `ConversionService`， 并在 `Bean` 属性配置及 `Spring MVC` 处理方法入参绑定等场合使用它进行数据转换。该 `FactoryBean` 创建 `ConversionService` 内建了很多转换器，可完成大多数 `Java` 类型的转换工作。除了包括将 `String` 对象转换为各种基础类型的对象外，还包括 `String`、 `Number`、`Array`、`Collection`、`Map`、`Properties` 及 `Object` 之间的转换器。可通过 `ConversionServiceFactoryBean` 的 `converters` 属性注册自定义的类型转换器：

```xml
    <bean class="org.springframework.context.support.ConversionServiceFactoryBean">
        <property name="converters">
            <list>
                <bean class="com.ankeetc.MyConverter"/>
            </list>
        </property>
    </bean>
```

**3.2.3.Spring支持的转换器**

`Spring` 在 `org.springframework.core.convert.converter` 包中定义了3种类型的转换器接口，实现任意一个转换器接口都可以作为自定义转换器注册到 `ConversionServiceFactoryBean` 中。这3种类型的转换器接口分别为：

- `Converter<S, T>`：将S类型的对象转换为T类型的对象
- `GenericConverter`：根据源类对象及目标类对象所在的宿主类的上下文信息进行类型转换工作。该类还有一个子接口`ConditionalGenericConverter`，它添加了一个接口方法根据源类型及目标类型所在宿主类的上下文信息决定是否要进行类型转换。
- `ConverterFactory`：`ConversionServiceFactoryBean` 的 `converters` 属性可接受 `Converter`、`ConverterFactory`、 `GenericConverter` 或 `ConditionalGenericConverter` 接口的实现类，并把这些转换器的转换逻辑统一封装到一个 `ConversionService` 实例对象中（`GenericConversionService`)。`Spring` 在 `Bean`属性配置及 `Spring MVC` 请求消息绑定时将利用这个 `ConversionService` 实例完成类型转换工作。

**3.2.4.在Spring中使用@lnitBinder 和 WebBindingInitializer装配自定义编辑器**

`Spring` 也支持 `JavaBeans` 的 `PropertyEditor`。可以在控制器中使用 `@InitBinder` 添加自定义的编辑器，也可以通过 `WebBindingInitializer` 装配在全局范围内使用的编辑器。

```java
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(User.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                User user = new User();
                user.setName(text);

                this.setValue(user);
            }
        });
    }
```

如果希望在全局范围内使用，则可实现 `WebBindingInitializer` 接口并在该实现类中注册。

1. 实现 `WebBindingInitializer` 接口并在 `initBinder` 接口方法中注册了自定义的编辑器。
2. 在 `Spring` 上下文中通过 `RequestMappingHandlerAdapter` 装配自定义的 `Initializer`。

**3.2.5.顺序**

对于同一个类型对象来说，如果既在 `ConversionService` 中装配了自定义转换器，又通过 `WebBindinglnitializer` 装配了自定义编辑器，同时还在控制器中通过 `@InitBinder` 装配了自定义编辑器，那么 `Spring MVC` 将按以下优先顺序查找对应类型的编辑器：

1. 查询通过 `@InitBinder` 装配的自定义编辑器。
2. 查询通过 `ConversionService`装配的自定义转换器。
3. 查询通过 `WebBindingInitializer` 装配的自定义编辑器。

### 3.3 数据格式化

`Spring` 的转换器并不提供输入及输出信息格式化的工作，一般需要转换的源类型数据（一般是字符串）都是具有一定格式的，在不同的本地化环境中， 同一类型的数据还会相应地呈现不同的显示格式。`Spring` 引入了一个新的格式化框架，这个框架位于`org.springframework.format` 类包中。

**最重要的 `Formatter<T>`接口**

**注解驱动格式化AnnotationFormatterFactory**

为了让注解和格式化的属性类型关联起来，`Spring` 在 `Formatter<T>` 所在的包中还提供了一个 `AnnotationFormatterFactory<A extends Annotation>` 接口。

**启用注解驱动格式化功能**

对属性对象的输入/输出进行格式化，从本质上讲依然属于“类型转换”的范畴。 `Spring` 就是基于对象转换框架植入“格式化”功能的。`Spring` 在格式化模块中定义了一个实现 `ConversionService` 接口的 `FormattingConversionService` 实现类，该实现类扩展了 `GenericConversionService`，因此它既具有类型转换功能，又具有格式化功能。

`FormattingConversionService` 也拥有一个对应的 `FormattingConversionServiceFactoryBean` 工厂类，后者用于在 `Spring`上下文中构造一个`FormattingConversionService`。通过这个工厂类，既可以注册自定义的转换器，还可以注册自定义的注解驱动逻辑。由于 `FormattingConversionServiceFactoryBean` 在内部会自动注册 `NumberFormatAnnotationFormatterFactory` 和 `JodaDateTimeFormatAnnotationFormatterFactory`，因此装配了 `FormattingConversionServiceFactoryBean` 后，就可以在 `Spring MVC` 入参绑定及模型数据输出时使用注解驱动的格式化功能。

 值得注意的是，`<mvc:annotation-driven/>` 标签内部默认创建的 `ConversionService` 实例就是一个 `FormattingConversionServiceFactoryBean`。

### 3.4 数据校验

`Spring` 拥有自己独立的数据校验框架，同时支持 `JSR-303` 标准的校验框架。`Spring` 的 `DataBinder` 在进行数据绑定时，可同时调用校验框架完成数据校验工作。在 `Spring MVC` 中，则可直接通过注解驱动的方式进行数据校验。

`LocalValidatorFactoryBean` 既实现了 `Spring` 的 `Validator` 接口，又实现了 `JSR-303` 的 `Validator` 接口。只要在 `Spring` 容器中定义了一个 `LocalValidatorFactoryBean`，即可将其注入需要数据校验的 `Bean` 中。值得注意的是，`Spring` 本身没有提供 `JSR-303` 的实现，所以必须将 `JSR-303` 的实现 者（如 `Hibernate Validator`)的 `JAR` 文件放到类路径下，`Spring` 将自动加载并装配好 `JSR-303` 的实现者。

 `<mvc:annotation-driven/>` 会默认装配一个 `LocalValidatorFactoryBean`，通过在处理方法的入参上标注 `@Valid` 注解，即可让`Spring MVC` 在完成数据绑定后执行数据校验工作。

### 3.5.Spring MVC处理请求、响应JSON数据

`Spring MVC` 默认使用 `MappingJackson2HttpMessageConverter` 转换 `JSON` 格式的数据，使用 `Jackson` 可以比较容易的 `json`、`java` 对象之间相互转换，因此在使用时需要引入相关 `jackson` 的jar包：`jackson-core-asl.jar`、`jackson-mapper-asl.jar`

**发送json示例：**

**3.5.1.pojo类，User.java**

```
public class User implements Serializable {
	private static final long serialVersionUID = -1684248767747396092L;
	
	private String username;//用户名
	private String pwd;//密码
	
	get/set略...
}
```

**3.5.2.springmvc.xml配置**

定义 `JSON` 转换器，在 `Controller` 使用 `@ResponseBody` 表示返回值转换成 `JSON` 文本；注意，对于 `spring4.x`,使用`MappingJackson2HttpMessageConverter`，`spring3.x` 使用 `MappingJacksonHttpMessageConverter`。若是配置文件中使用了`<mvc:annotation-driven />`，则会自动提供读取 `JSON` 的功能，下面的配置则可省略。

```
<bean class="org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter">
	<property name="messageConverters">
		<list>
			<bean class="org.springframework.http.converter.json.MappingJackson2HttpMessageConverter"/>
		</list>
	</property>
</bean>
```

**3.5.3.请求的js以及处理方法**

**3.5.3.1方式一**
使用默认的 `contentType`，默认值为 `application/x-www-form-urlencoded`，发送 `js` 数据（`key:value` 形式）；后台可以通过`@RequestParam`、`@ModelAttribute`、`@RequestBody` 等注解处理

**3.5.3.1.1请求的js**

```
$("#_btn_send1").click(function(){
	$.post("${pageContext.request.contextPath}/json/receiveJson.do", {username:"小明",pwd:"123456"}, 
		function(data){
			console.log(data);	
		}, "json");
});
```

**3.5.3.1.2对应的处理方法**

```
@RequestMapping("/receiveJson")
@ResponseBody
public Map<String, Object> receiveJson(User user) throws Exception {
	Map<String, Object> dataMap = new HashMap<String,Object>();
	dataMap.put("status", "success");
		
	if(user != null){
		System.out.println("用户名：" + user.getUsername() + " 密码：" + user.getPwd());
	}else {
		System.out.println("未获取到用户");
	}
	return dataMap;
}
```

**3.5.3.2方式二**
指定请求的 `contentType:'application/json'`，此时发送数据需为 `json` 字符串，否则提示 `400` 错误；对于设置 `contentType` 为`application/json`、`application/xml` 后，后台需要通过 `@RequestBody` 注解接收，否则值都为 `null`

**3.5.3.2.1请求的js**

```
$("#_btn_send2").click(function(){
	$.ajax({
		url : "${pageContext.request.contextPath}/json/receiveJson2.do",
		type : "post",
		data: JSON.stringify({username:"小明",pwd:"123456"}),
		contentType : "application/json",
		dataType : "json",
		success : function(data){
			console.log(data);
		}
	});
});
```

**3.5.3.2.2对应的处理方法**

```
@RequestMapping("/receiveJson2")
@ResponseBody
public Map<String, Object> receiveJson2(@RequestBody User user) throws Exception {
	Map<String, Object> dataMap = new HashMap<String,Object>();
	dataMap.put("status", "success");
		
	if(user != null){
		System.out.println("用户名：" + user.getUsername() + " 密码：" + user.getPwd());
	}else {
		System.out.println("未获取到用户");
	}
	return dataMap;
}
```

**3.5.3.注：**

- 注意 `jackson` 相关 `jar` 的引入，以及 `springmvc.xml` 文件中定义 `JSON` 转换器的配置，`spring3.x/4.x` 用的两个转换器名称上略有不同，当时坑了我好久才发现
- 若 `springmvc.xml` 配置文件中使用了 `<mvc:annotation-driven />`，则会自动提供读取 `JSON` 的功能，此时步骤2可省略；
- 对于 `GET/POST` 请求，请求头的 `contentType` 不同分为以下几种情况：
  - `application/x-www-urlencoded`（默认），可以使用 `@RequestParam`、`@RequestBody`、`@ModelAttribute` 处理；
  - `multipart/form-data`，可使用 `@RequestParam`，对于文件则可以使用 `MultipartFile` 类接收处理；`@RequestBody` 不能处理；
  - `application/json`、`application/xml`等格式，须使用 `@RequestBody` 处理

## 4.视图和视图解析器

## 5.本地化

Spring提供了以下4个本地化解析器。

- `AcceptHeaderLocaleResolver`:根据 HTTP 报文头的 Accept-Language 参数确定本 地化类型。如果没有显式定义本地化解析器，则Spring MVC默认采用 AcceptHeaderLocaleResolver。
- `CookieLocaleResolver`:根据指定的Cookie值确定本地化类型。
- `SessionLocaleResolver`:根据Session中特定的属性值确定本地化类型。
- `LocaleChangeInterceptor`:从请求参数中获取本次请求对应的本地化类型。

## 6 文件上传

`Spring MVC` 为文件上传提供了直接支持，这种支持是通过即插即用的 `MultipartResolver` 实现的。`Spring` 使用 `Jakarta Commons FileUpload` 技术实现了一个 `MultipartResolver` 实现 类：`CommonsMultipartResolver`。

在 `Spring MVC`上下文中默认没有装配 `MultipartResolver`，因此默认情况下不能 处理文件的上传工作。如果想使用 `Spring` 的文件上传功能，则需要先在上下文中配置 `MultipartResolver`。

## 7.WebSocket

## 8.静态资源处理

1. `<mvc:default-servlet-handler/>`：在 `smart-servlet.xml` 中配置 `<mvc:default-servlet-handler/>` 后，会在 `Spring MVC` 上下文中定义一个 `org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler`，它将充当一个检查员的角色，对进入`DispatcherServlet` 的 `URL` 进行筛查。如果发现是静态资源的请求，就将该请求转由 `Web` 应用服务器默认的`Servlet` 处理；如果不是静态资源 的请求，则由 `DispatcherServlet` 继续处理。
2. `<mvc:resources/>`：`<mvc:default-servlet-handler/>`将静态资源的处理经由 `Spring MVC` 框架交回 `Web` 应用服务器。而`<mvc:resources/>`更进一步，由 `SpringMVC` 框架自己处理静态资源，并添 加一些有用的附加功能。

## 9 拦截器

当收到请求时，`DispatcherServlet` 将请求交给处理器映射（`HandlerMapping`)，让它找出对应该请求的 `HandlerExecutionChain` 对象。在讲解 `HandlerMapping` 之前，有必要 认识一下这个 `HandlerExecutionChain` 对象。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330224339.webp)
`HandlerExecutionChain` 负责处理请求并返回 `ModelAndView` 的处理执行链，它包含一个处理该请求的处理器 (`Handler`)，同时包括若干个对该请求实施拦截的拦截器（`HandlerInterceptor`)。当 `HandlerMapping` 返回 `HandlerExecutionChain` 后，`DispatcherServlet` 将请求交给定义在 `HandlerExecutionChain` 中的拦截器和处理器一并处理。

 位于处理器链末端的是一个 `Handler`，`DispatcherServlet` 通过 `Handler Adapter` 适配器对 `Handler` 进行封装，并按统一的适配器接口对 `Handler` 处理方法进行调用。可以在 `web-servlet.xml`中配置多个拦截器,每个拦截器都可以指定一个匹配的映射路径，以限制拦截器的作用范围。

## 10 异常处理

`Spring MVC` 通过 `HandlerExceptionResolver` 处理程序的异常，包括处理器映射、数据绑定及处理器执行时发生的异常。 `HandlerExceptionResolver` 仅有一个接口方法：`Modelandview resolveException(HttpServletRequest request HttpServletResponse response Object handler, Exception ex)`。当发生异常时，`Spring MVC` 将调用 `resolveException` 方法，并转到 `ModelAndView` 对应的视图中，作为一个异常报告页面反馈给用户。

**实现类**

`HandlerExceptionResolver` 拥有4个实现类

1. `DefaultHandlerExceptionResolver`：默认装配了该类，将对应异常转换为错误码
2. `SimpleMappingExceptionResolver`：对所有异常进行统一处理
3. `AnnotationMethodHandlerExceptionResolver`：默认注册了该类，允许通过@ExceptionHandler注解指定处理特定的异常
4. `ResponseStatusExceptionResolver`



## 11.常用注解

### 11.1.先说扫描注解

```
<context:component-scan base-package = "" />
```

`component-scan` 默认扫描的注解类型是 `@Component`，不过，在 `@Component` 语义基础上细化后的 `@Repository`, `@Service` 和 `@Controller` 也同样可以获得 `component-scan` 的青睐 

有了 `<context:component-scan>`，另一个 `<context:annotation-config/>` 标签根本可以移除掉，因为已经被包含进去了 
另外 `<context:annotation-config/>` 还提供了两个子标签 

1. `<context:include-filter> `//指定扫描的路径 

2. `<context:exclude-filter>` //排除扫描的路径 

`<context:component-scan>` 有一个 `use-default-filters` 属性，属性默认为 `true`,表示会扫描指定包下的全部的标有`@Component` 的类，并注册成 `bean`。也就是 `@Component` 的子注解 `@Service`,`@Reposity` 等。 

这种扫描的粒度有点太大，如果你只想扫描指定包下面的 `Controller` 或其他内容则设置 `use-default-filters` 属性为 `false`，表示不再按照 `scan` 指定的包扫描，而是按照 `<context:include-filter>` 指定的包扫描，示例：

```
<context:component-scan base-package="com.tan" use-default-filters="false">
        <context:include-filter type="regex" expression="com.tan.*"/>//注意后面要写.*
</context:component-scan>
```

当没有设置 `use-default-filters` 属性或者属性为 `true` 时，表示基于 `base-packge` 包下指定扫描的具体路径

```
<context:component-scan base-package="com.tan" >
        <context:include-filter type="regex" expression=".controller.*"/>
        <context:include-filter type="regex" expression=".service.*"/>
        <context:include-filter type="regex" expression=".dao.*"/>
</context:component-scan>
```

效果相当于：

```
<context:component-scan base-package="com.tan" >
        <context:exclude-filter type="regex" expression=".model.*"/>
</context:component-scan>
```


注意：无论哪种情况 `<context:include-filter>` 和 `<context:exclude-filter>` 都不能同时存在

### 11.2.@Controller

在 `SpringMVC` 中，控制器 `Controller` 负责处理由 `DispatcherServlet` 分发的请求，它把用户请求的数据经过业务处理层处理之后封装成一个 `Model`，然后再把该 `Model` 返回给对应的 `View` 进行展示。在 `SpringMVC` 中提供了一个非常简便的定义 `Controller` 的方法，你无需继承特定的类或实现特定的接口，只需使用 `@Controller` 标记一个类是 `Controller`，然后使用 `@RequestMapping` 和`@RequestParam` 等一些注解用以定义 `URL` 请求和 `Controller` 方法之间的映射，这样的 `Controller` 就能被外界访问到。此外`Controller` 不会直接依赖于 `HttpServletRequest` 和 `HttpServletResponse` 等 `HttpServlet` 对象，它们可以通过 `Controller` 的方法参数灵活的获取到

```
@Controller
public class SpringMVCController {
    @RequestMapping("/index")
    public ModelAndView index(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("index");
        modelAndView.addObject("sunwukong");
        return modelAndView;
    }
}
```

`ModelAdnView` 的 `addObject` 底层其实就是往 `HashMap` 里 `put` 值，我们可以看下源码实现

```
public ModelAndView addObject(Object attributeValue)
{
    getModelMap().addAttribute(attributeValue);
    return this;
}
```

在进一步看下 `addAttribute` 的实现

```
 public ModelMap addAttribute(String attributeName, Object attributeValue)
    {
        Assert.notNull(attributeName, "Model attribute name must not be null");
        put(attributeName, attributeValue);
        return this;
    }
// put方法就是HashMap的put
public V put(K key, V value) {
        if (table == EMPTY_TABLE) {
            inflateTable(threshold);
        }
        if (key == null)
            return putForNullKey(value);
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {
            Object k;
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
            }
        }
        modCount++;
        addEntry(hash, key, value, i);
        return null;
    }
```

`@Controller` 用于标记在一个类上，使用它标记的类就是一个 `SpringMVC Controller` 对象。分发处理器将会扫描使用了该注解的类的方法，并检测该方法是否使用了 `@RequestMapping`  注解。`@Controller` 只是定义了一个控制器类，而使用 `@RequestMapping` 注解的方法才是真正处理请求的处理器 。单单使用 `@Controller` 标记在一个类上还不能真正意义上的说它就是 `SpringMVC` 的一个控制器类，因为这个时候 `Spring`  还不认识它。把这个控制器类交给 `Spring` 来管理的方法有两种如下 

1. 在 `Spring MVC` 的配置文件中定义 `Spring MVC Controller` 的 `bean` 对象

   ```
   <bean class="com.nyonline.sp2p.controller.SpringMVCController"/>
   ```

2. 在 `Spring MVC` 的配置文件中让 `Spring` 去扫描获取

   ```
   < context:component-scan base-package = "com.nyonline.sp2p.controller" >
          < context:exclude-filter type = "annotation"
              expression = "org.springframework.stereotype.Service" /> <!-- 排除@service -->
   </ context:component-scan> 
   ```

   

### 11.3.@RequestMapping

`RequestMapping` 是一个用来处理请求地址映射的注解，可用于类或方法上。用于类上，表示类中的所有响应请求的方法都是以该地址作为父路径。`RequestMapping` 注解有六个属性，下面我们把她分成三类进行说明 

**11.3.1.value与Method**

```
	/**
     * value:指定请求的实际地址，指定的地址可以是URI Template 模式
     * method:指定请求的method类型， GET、POST、PUT、DELETE等吗，更接近rest模式
     * @return
     */
    @RequestMapping (value= "testMethod" , method={RequestMethod.GET ,RequestMethod.DELETE})
    public String testMethod() {
       return "method" ;
    }  
```

**11.3.2.consumes与produces** 

`consumes`：指定处理请求的提交内容类型（`Content-Type`），例如 `application/json`, `text/html`; 

`produces`: 指定返回的内容类型，仅当 `request` 请求头中的( `Accept`)类型中包含该指定类型才返回

```
	/**
     * 方法仅处理request Content-Type为“application/json”类型的请求. 
     * produces标识==>处理request请求中Accept头中包含了"application/json"的请求，
     * 同时暗示了返回的内容类型为application/json
     * @return
     */
    @RequestMapping (value= "testConsumes" ,consumes = "application/json",produces = "application/json")
    public String testConsumes() {
       return "consumes" ;
    }
```

**11.3.3.params与headers** 

`params`：指定 `request` 中必须包含某些参数值是，才让该方法处理。 

`headers`：指定 `request` 中必须包含某些指定的 `header` 值，才能让该方法处理请求。

```
/**
* headers 属性的用法和功能与params 属性相似。在上面的代码中当请求/testHeaders.do的时候只有当请求头包含Accept信息，
* 且请求的host 为localhost和name=sunwukong的时候才能正确的访问到testHeaders方法。
* @return
*/
@RequestMapping (value= "testHeaders" , headers={ "host=localhost" , "Accept"},params={"name=sunwukong"})
public String testHeaders() {
	return "headers" ;
}
```

### 11.4.@Resource和@Autowired

**@Autowired**

| 特性 | 说明                         |
| ---- | ---------------------------- |
| 原理 | 根据类型来自动注入（ByType） |
| 注入类型	| 既可以注入一个接口，也可以直接注入一个实例|
| 限制	| 1.当注入一个接口时，这个接口只能有一个实现类，如果存在一个以上的实现类，那么Spring会抛出异常，因为两个同样的接口实现类，它不知道该选择哪一个来注入。2.当注入一个实例时，跟接口类似，如果这个实例在XML配置文件中声明了两个不同的Bean,那么Spring也会抛出异常。|
| 解决办法	| @Autowired配合@Qualifier来使用，通过@Qualifier来指明要注入Bean的name。|

**@Resource**

| 特性     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 原理     | 如果指定了name属性, 那么就按name属性的名称装配;如果没有指定name属性, 那就按照要注入对象的字段名查找依赖对象;如果按默认名称查找不到依赖对象, 那么就按照类型查找。 |
| 注入类型 | 既可以注入一个接口，也可以直接注入一个实例                   |

`@Resource` 和 `@Autowired` 都是做 `bean` 的注入时使用，其实 `@Resource` 并不是 `Spring` 的注解，它的包是`javax.annotation.Resource`，需要导入，但是 `Spring` 支持该注解的注入。 

- 共同点 

  两者都可以写在字段和 `setter` 方法上。两者如果都写在字段上，那么就不需要再写 `setter` 方法 

- 不同点 

	`@Autowired` 为 `Spring` 提供的注解，需要导入包 `org.springframework.beans.factory.annotation.Autowired;` 只按照`byType` 注入。

    ```
    public class TestServiceImpl {
        // 下面两种@Autowired只要使用一种即可
        @Autowired
        private UserDao userDao; // 用于字段上

        @Autowired
        public void setUserDao(UserDao userDao) { // 用于属性的方法上
            this.userDao = userDao;
        }
    }
    ```

	`@Autowired` 注解是按照类型（`byType`）装配依赖对象，默认情况下它要求依赖对象必须存在，如果允许 `null` 值，可以设置它的`required` 属性为 `false`。如果我们想使用按照名称（`byName`）来装配，可以结合 `@Qualifier` 注解一起使用。如下：

    ```
    public class TestServiceImpl {
        @Autowired
        @Qualifier("userDao")
        private UserDao userDao; 
    }
    ```
  
  `@Resource` 默认按照 `ByName` 自动注入，由 `J2EE` 提供，需要导入包 `javax.annotation.Resource`。`@Resource` 有两个重要的属性：`name` 和 `type`，而 `Spring` 将 `@Resource` 注解的 `name` 属性解析为 `bean` 的名字，而 `type` 属性则解析为 `bean` 的类型。所以，如果使用 `name` 属性，则使用 `byName` 的自动注入策略，而使用 `type` 属性时则使用 `byType` 自动注入策略。如果既不制定`name` 也不制定 `type` 属性，这时将通过反射机制使用 `byName` 自动注入策略。

    ```
    @Resource(name="ad")
    private AdBo ad;

    @Resource(type=BidBo.class)
    private BidBo bid;
    ```
  
  - 如果同时指定了 `name` 和 `type`，则从 `Spring` 上下文中找到唯一匹配的 `bean` 进行装配，找不到则抛出异常。
  - 如果指定了 `name`，则从上下文中查找名称（id）匹配的 `bean`进行装配，找不到则抛出异常。 
  
  - 如果指定了 `type`，则从上下文中找到类似匹配的唯一bean进行装配，找不到或是找到多个，都会抛出异常。 
  
  - 如果既没有指定 `name`，又没有指定 `type`，则自动按照 `byName` 方式进行装配；如果没有匹配，则回退为一个原始类型进行匹配，如果匹配则自动装配。 
  
  - `@Resource` 的作用相当于 `@Autowired`，只不过 `@Autowired` 按照 `byType` 自动注入。

### 11.5.@ModelAttribute

代表的是：该 `Controller` 的所有方法在调用前，先执行此 `@ModelAttribute` 方法，可用于注解和方法参数中，可以把这个`@ModelAttribute` 特性，应用在 `BaseController` 当中，所有的 `Controller` 继承 `BaseController`，即可实现在调用 `Controller`时，先执行 `@ModelAttribute` 方法。 

`@modelAttribute` 可以在两种地方使用，参数和方法体。

先介绍下方法体的用法

```
	@ModelAttribute("user")
    public User getUser() {
        User user = new User();
        user.setId(1234567L);
        user.setName("sunwukong");
        user.setPassword("admin");
        return user;
    }
	@RequestMapping(value = "testUser", method = {RequestMethod.GET, RequestMethod.POST})
    public String testUser(Map<String, Object> map) {
        System.out.println(map.get("user"));
        return "success";
   }
```

当我们发出 `/testUser.do` 这个请求时，`SpringMvc` 在执行该请求前会先逐个调用在方法级上标注了 
`@ModelAttribute` 的方法，然后将该模型参数放入`testUser()` 函数的 `Map` 参数中 

**执行结果：** 

```
User{id=123456,name=”sunwukong”,password=”admin”} 
```

**作用在参数上** 

`SpringMVC` 先从模型数据中获取对象，再将请求参数绑定到对象中，再传入形参，并且数据模型中的对象会被覆盖

```
	@RequestMapping(value = "test1")
    public ModelAndView test1(@ModelAttribute("user1") User user, ModelAndView modelAndView) {
        System.out.println(user + ":test1");
        modelAndView.setViewName("redirect:/test2.do");
        return modelAndView;
    }
    @RequestMapping(value = "test2", method = { RequestMethod.GET, RequestMethod.POST })
    public String test2(Map<String, Object> map) {
        System.out.println(map.get("user1"));
        return "success";
    }
    @ModelAttribute("user1")
    public User getUser() {
        User user = new User();
        user.setId(123456L);
        user.setName("sunwukong");
        user.setPassword("admin");
        return user;
    }
```

访问 `http://localhost/test1.do?name=qitiandasheng`，输出结果： 

```
User{id=123456,name=”齐天大圣”,password=”admin”} 
```

我们传入了 `name` 此时 `user` 的 `name` 被传入的值覆盖，如果请求参数中有的参数已经绑定到了 `user` 中，那么请求参数会覆盖掉 `user` 中已存在的值，并且 `user` 对象会被放入数据模型中覆盖掉原来的 `user1` 对象。也就是模型数据中的 `user1` 的优先级低于请求参数

### 11.6.@SessionAttributes

该注解用来绑定 `HttpSession` 中的 `attribute` 对象的值，便于在方法中的参数里使用。

```
@Controller
@SessionAttributes(value = {"user1"})
public class SpringMVCController {

    @RequestMapping(value = "test4", method = {RequestMethod.GET, RequestMethod.POST})
    public String test4(Map<String, Object> map, HttpSession session) {
        System.out.println(map.get("user1"));
        System.out.println("session:" + session.getAttribute("user1"));
        return "success";
    }
    @ModelAttribute("user1")
    public User getUser() {
        User user = new User();
        user.setId(123456L);
        user.setName("sunwukong");
        user.setPassword("admin");
        return user;
    }
}
```

当我第一次执行 `test4.do` 输出结果： 

```
User [id=123456, name=sunwukong, password=admin] 
session:null 
```

第二次执行 

```
User [id=123456, name=sunwukong, password=admin] 
session:User [id=123456, name=sunwukong, password=admin] 
```

只是在第一次访问 `test4.do` 的时候 `@SessionAttributes` 定义了需要存放到 `session` 中的属性，而且这个模型中也有对应的属性，但是这个时候还没有加到 `session` 中，所以 `session` 中不会有任何属性，等处理器方法执行完成后 `Spring` 才会把模型中对应的属性添加到 `session` 中。

### 11.7.@PathVariable和@RequestParam

请求路径上有个id的变量值，可以通过@PathVariable来获取 ` @RequestMapping(value = "/page/{id}", method = RequestMethod.GET) `
@RequestParam用来获得静态的URL请求入参,spring注解时action里用到。它有三个常用参数：`defaultValue = “0”, required = false, value = “pageNo”；` `defaultValue `表示设置默认值，`required` 通过 `boolean` 设置是否是必须要传入的参数，`value` 值表示接受的传入的参数类型。 

例如： 

地址① 

`http://localhost:8989/test/index?pageNo=2 `

地址② 

`http://localhost:8989/test/index/7 `

如果想获取地址①中的 `pageNo` 的值 ‘2’ ，则使用 `@RequestParam` ， 

如果想获取地址②中的 `emp/7` 中的 ‘7 ’ 则使用 `@PathVariable`

```
@RequestMapping("/index")  
public String list(@RequestParam(value="pageNo",required=false,  
        defaultValue="1")String pageNoStr,Map<String, Object>map){  

    int pageNo = 1;  

    try {  
        //对pageNo 的校验   
        pageNo = Integer.parseInt(pageNoStr);  
        if(pageNo<1){  
            pageNo = 1;  
        }  
    } catch (Exception e) {}  

    Page<Employee> page = employeeService.getPage(pageNo, 5);  
    map.put("page",page);  

    return "index/list";  
}  

@RequestMapping(value="/index/{id}",method=RequestMethod.GET)  
public String edit(@PathVariable("id")Integer id,Map<String , Object>map){  
    Employee employee = employeeService.getEmployee(id);  
    List<Department> departments = departmentService.getAll();  
    map.put("employee", employee);  
    map.put("departments", departments);  
    return "index/input";  
}  
```

### 11.8.@ResponseBody

该注解用于将 `Controller` 的方法返回的对象，通过适当的 `HttpMessageConverter` 转换为指定格式后，写入到 `Response` 对象的`body` 数据区。 

使用时机：返回的数据不是 `html` 标签的页面，而是其他某种格式的数据时（如 `json`、`xml`等）使用；

```
/** 
 * 返回处理结果到前端 
 * @param msg: Success or fail。 
 * @throws IOException 
 */  
public void sendToCFT(String msg) throws IOException {  
    String strHtml = msg;  
    PrintWriter out = this.getHttpServletResponse().getWriter();  
    out.println(strHtml);  
    out.flush();  
    out.close();  

}  
```

如果用 `SpringMVC` 注解

```
    @ResponseBody  
    @RequestMapping("/pay/tenpay")  
    public String tenpayReturnUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {  
        unpackCookie(request, response);  
        payReturnUrl.payReturnUrl(request, response);  
        return "pay/success";  
    }  
```

### 11.9.@Component

泛指组件，当组件不要好归类时，可以使用这个注解进行标注

### 11.10.@Repository

用于注解 `dao` 层，在 `daoImpl` 类上面注解。 

`@Repository(value=”userDao”)` 注解是告诉 `Spring`，让 `Spring` 创建一个名字叫 `userDao` 的 `UserDaoImpl` 实例 

### 11.11.@RequestBody 

`@RequestBody` 接收的是一个 `Json` 对象的字符串，而不是一个 `Json` 对象。然而在 `ajax` 请求往往传的都是 `Json` 对象，后来发现用 `JSON.stringify(data)`的方式就能将对象变成字符串。同时 `ajax` 请求的时候也要指定 `dataType: “json”,contentType:”application/json”` 这样就可以轻易的将一个对象或者 `List` 传到 `Java` 端，使用 `@RequestBody` 即可绑定对象或者 `List`

```
<script type="text/javascript">  
    $(document).ready(function(){  
        var saveDataAry=[];  
        var data1={"userName":"test","address":"gz"};  
        var data2={"userName":"ququ","address":"gr"};  
        saveDataAry.push(data1);  
        saveDataAry.push(data2);         
        $.ajax({ 
            type:"POST", 
            url:"user/saveUser", 
            dataType:"json",      
            contentType:"application/json",               
            data:JSON.stringify(saveData), 
            success:function(data){ 

            } 
         }); 
    });  
</script> 
```

Java代码：

```
	@RequestMapping(value = "saveUser", method = {RequestMethod.POST }}) 
    @ResponseBody  
    public void saveUser(@RequestBody List<User> users) { 
         userService.batchSave(users); 
    } 
```

## 12.拦截器

**SpringMVC 拦截器的原理图**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330232809.png)

**springMVC拦截器的实现一般有两种方式**

- 第一种方式是要定义的 `Interceptor` 类要实现了 `Spring` 的 `HandlerInterceptor` 接口

- 第二种方式是继承实现了 `HandlerInterceptor` 接口的类，比如 `Spring` 已经提供的实现了 `HandlerInterceptor` 接口的抽象类`HandlerInterceptorAdapter`



`HandlerInterceptor` 接口中定义了三个方法，我们就是通过这三个方法来对用户的请求进行拦截处理的。

- **preHandle()**： 这个方法在业务处理器处理请求之前被调用，`SpringMVC` 中的 `Interceptor` 是链式的调用的，在一个应用中或者说是在一个请求中可以同时存在多个 `Interceptor` 。每个 `Interceptor` 的调用会依据它的声明顺序依次执行，而且最先执行的都是 `Interceptor` 中的 `preHandle` 方法，所以可以在这个方法中进行一些前置初始化操作或者是对当前请求的一个预处理，也可以在这个方法中进行一些判断来决定请求是否要继续进行下去。该方法的返回值是布尔值 `Boolean` 类型的，当它返回为 `false` 时，表示请求结束，后续的 `Interceptor` 和 `Controller` 都不会再执行；当返回值为 `true` 时就会继续调用下一个 `Interceptor` 的`preHandle` 方法，如果已经是最后一个 `Interceptor` 的时候就会是调用当前请求的 `Controller` 方法。

- **postHandle()**：这个方法在当前请求进行处理之后，也就是 `Controller` 方法调用之后执行，但是它会在 `DispatcherServlet` 进行视图返回渲染之前被调用，所以我们可以在这个方法中对 `Controller` 处理之后的 `ModelAndView` 对象进行操作。`postHandle` 方法被调用的方向跟 `preHandle` 是相反的，也就是说先声明的 `Interceptor` 的 `postHandle` 方法反而会后执行。

- **afterCompletion()**：该方法也是需要当前对应的 `Interceptor` 的 `preHandle` 方法的返回值为 `true` 时才会执行。顾名思义，该方法将在整个请求结束之后，也就是在 `DispatcherServlet` 渲染了对应的视图之后执行。这个方法的主要作用是用于进行资源清理工作的。

下面来看我们的Interceptor类

```
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class CommonInterceptor  extends HandlerInterceptorAdapter{

        private final Logger log = LoggerFactory.getLogger(CommonInterceptor.class);

        public  static  final  String  LAST_PAGE = "lastPage";
        /** 
         * 在业务处理器处理请求之前被调用 
         * 如果返回false 
         *     从当前的拦截器往回执行所有拦截器的afterCompletion(),再退出拦截器链
         *     
         * 如果返回true 
         *    执行下一个拦截器,直到所有的拦截器都执行完毕 
         *    再执行被拦截的Controller 
         *    然后进入拦截器链, 
         *    从最后一个拦截器往回执行所有的postHandle() 
         *    接着再从最后一个拦截器往回执行所有的afterCompletion() 
         */  
        @Override  
        public boolean preHandle(HttpServletRequest request,  
                HttpServletResponse response, Object handler) throws Exception {            
            if ("GET".equalsIgnoreCase(request.getMethod())) {
                    RequestUtil.saveRequest();
            }
            log.info("==============执行顺序: 1、preHandle================");  
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String url = requestUri.substring(contextPath.length());         if ("/userController/login".equals(url)) {                  
                    return true;
            }else {               
                    String username =  (String)request.getSession().getAttribute("user"); 
                    if(username == null){
                            log.info("Interceptor：跳转到login页面！");
                            request.getRequestDispatcher("/page/index.jsp").forward(request, response);
                            return false;
                    }else
                            return true;   
           }
            
        }        
        /**
         * 在业务处理器处理请求执行完成后,生成视图之前执行的动作   
         * 可在modelAndView中加入数据，比如当前时间
         */
        @Override  
        public void postHandle(HttpServletRequest request,  
                HttpServletResponse response, Object handler,  
                ModelAndView modelAndView) throws Exception {   
            log.info("==============执行顺序: 2、postHandle================");  
            if(modelAndView != null){  //加入当前时间  
                modelAndView.addObject("haha", "测试postHandle");  
            }  
        }        
        /** 
         * 在DispatcherServlet完全处理完请求后被调用,可用于清理资源等    
         * 当有拦截器抛出异常时,会从当前拦截器往回执行所有的拦截器的afterCompletion() 
         */  
        @Override  
        public void afterCompletion(HttpServletRequest request,  
                HttpServletResponse response, Object handler, Exception ex)  
                throws Exception {  
            log.info("==============执行顺序: 3、afterCompletion================");  
        }  
}
```

spring-MVC.xml的相关配置

```
    <!-- 对静态资源文件的访问-->
    <!-- <mvc:resources mapping="/images/**"  location="/images/"/> 
    <mvc:resources mapping="/css/**"  location="/css/" />
    <mvc:resources mapping="/js/**"  location="/js/" /> 
    <mvc:resources mapping="/favicon.ico"  location="favicon.ico" /> --> 
    <!--配置拦截器, 多个拦截器,顺序执行 -->
    <mvc:interceptors> 
           <mvc:interceptor>
                   <!--  
                       /**的意思是所有文件夹及里面的子文件夹 
                       /*是所有文件夹，不含子文件夹 
                       /是web项目的根目录
                     --> 
                   <mvc:mapping path="/**" /> 
                   <!-- 需排除拦截的地址 -->  
                   <!--  <mvc:exclude-mapping path="/userController/login"/>  -->
                   <bean id="commonInterceptor" class="org.shop.interceptor.CommonInterceptor"></bean> <!--这个类就是我们自定义的Interceptor -->
          </mvc:interceptor> 
          <!-- 当设置多个拦截器时，先按顺序调用preHandle方法，然后逆序调用每个拦截器的postHandle和afterCompletion方法  -->
    </mvc:interceptors>
```

就这么简单SpringMVC拦截器写好了，**登陆的实现用上一篇filter的代码就可以进行测试了。**

注意：在我测试的时候我用 `<mvc:resources>`不拦截静态资源居然不管用，也不知道是怎么回事，希望有大神指正下应该怎么做

```
<!-- 对静态资源文件的访问--><mvc:resources mapping="/images/**"  location="/images/"/> 
<mvc:resources mapping="/css/**"  location="/css/" />
<mvc:resources mapping="/js/**"  location="/js/" /> 
<mvc:resources mapping="/favicon.ico"  location="favicon.ico" />
```

所以我只好在web.xml进行了对静态资源不拦截的配置

```
    <!-- 不拦截静态文件 -->
    <servlet-mapping>
        <servlet-name>default</servlet-name>
        <url-pattern>/js/*</url-pattern>
        <url-pattern>/css/*</url-pattern>
        <url-pattern>/images/*</url-pattern>
        <url-pattern>/fonts/*</url-pattern>
    </servlet-mapping>
```

## 13.SpringMVC常用接口之HandlerMethodArgumentResolver

在初学springmvc框架时，我就一直有一个疑问，为什么controller方法上竟然可以放这么多的参数，而且都能得到想要的对象，比如`HttpServletRequest`或`HttpServletResponse`，各种注解`@RequestParam`、`@RequestHeader`、`@RequestBody`、`@PathVariable`、`@ModelAttribute`等。相信很多初学者都曾经感慨过。

这篇文章就是讲解处理这方面内容的
 org.springframework.web.method.support.HandlerMethodArgumentResolver接口。

```java
ServletRequestMethodArgumentResolver和ServletResponseMethodArgumentResolver
处理了自动绑定HttpServletRequest和HttpServletResponse

RequestParamMapMethodArgumentResolver处理了@RequestParam
RequestHeaderMapMethodArgumentResolver处理@RequestHeader
PathVariableMapMethodArgumentResolver处理了@PathVariable
ModelAttributeMethodProcessor处理了@ModelAttribute
RequestResponseBodyMethodProcessor处理了@RequestBody
```

我们可以模仿springmvc的源码，实现一些我们自己的实现类，而方便我们的代码开发。

接口说明

```java
package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;

public interface HandlerMethodArgumentResolver {
    //用于判定是否需要处理该参数分解，返回true为需要，并会去调用下面的方法resolveArgument。
    boolean supportsParameter(MethodParameter parameter);
    //真正用于处理参数分解的方法，返回的Object就是controller方法上的形参对象。
    Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception;

}
```

示例1
 本示例显示如何 **优雅地**将传入的信息转化成自定义的实体传入controller方法。

post 数据:
 first_name = Bill
 last_name = Gates
 初学者一般喜欢类似下面的代码：

```java
package com.demo.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.demo.domain.Person;
import com.demo.mvc.annotation.MultiPerson;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("demo1")
public class HandlerMethodArgumentResolverDemoController {

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    public String addPerson(HttpServletRequest request) {
        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        Person person = new Person(firstName, lastName);
        log.info(person.toString());
        return person.toString();
    }
}
```

这样的代码强依赖了javax.servlet-api的HttpServletRequest对象，并且把初始化Person对象这“活儿”加塞给了controller。代码显得累赘不优雅。在controller里我只想使用person而不想组装person，想要类似下面的代码：

```java
@RequestMapping(method = RequestMethod.POST)
public String addPerson(Person person) {
  log.info(person.toString());
  return person.toString();
}
```

直接在形参列表中获得person。那么这该如实现呢？

我们需要定义如下的一个参数分解器：

```java
package com.demo.mvc.component;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.demo.domain.Person;

public class PersonArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Person.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        String firstName = webRequest.getParameter("first_name");
        String lastName = webRequest.getParameter("last_name");
        return new Person(firstName, lastName);
    }

}
```

在supportsParameter中判断是否需要启用分解功能，这里判断形参类型是否为Person类，也就是说当形参遇到Person类时始终会执行该分解流程resolveArgument。

在resolveArgument中处理person的初始化工作。

注册自定义分解器：

传统XML配置：

```xml
<mvc:annotation-driven>
      <mvc:argument-resolvers>
        <bean class="com.demo.mvc.component.PersonArgumentResolver"/>
      </mvc:argument-resolvers>
</mvc:annotation-driven>
```

或

```xml
<bean class="org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter">
    <property name="customArgumentResolvers">
          <bean class="com.demo.mvc.component.PersonArgumentResolver"/>
    </property>
</bean>
```

示例2
 加强版Person分解器，支持多个person对象。

post 数据:
 person1.first_name = Bill
 person1.last_name = Gates
 person2.first_name = Steve
 person2.last_name = Jobs
 用前缀区分属于哪个person对象。
 定义一个注解用于设定前缀：

```java
package com.demo.mvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiPerson {

    public String value();
}
```

参数分解器：

```java
package com.demo.mvc.component;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.demo.domain.Person;
import com.demo.mvc.annotation.MultiPerson;

public class MultiPersonArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MultiPerson.class) && parameter.getParameterType().equals(Person.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        MultiPerson annotation = parameter.getParameterAnnotation(MultiPerson.class);
        String firstName = webRequest.getParameter(annotation.value() + ".first_name");
        String lastName = webRequest.getParameter(annotation.value() + ".last_name");
        return new Person(firstName, lastName);
    }

}
```

controller：

```java
@ResponseBody
@RequestMapping(value = "multi", method = RequestMethod.POST)
public String addPerson(@MultiPerson("person1") Person person1, @MultiPerson("person2") Person person2) {
  log.info(person1.toString());
  log.info(person2.toString());
  return person1.toString() + "\n" + person2.toString();
}
```













## X.面试题

### X.1.SpringMVC的控制器是单例的吗?

**第一次：类是多例，一个普通属性和一个静态属性。**

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330233616.png)

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330233636.webp)

结果：

```
普通属性：0.............静态属性：0
普通属性：0.............静态属性：1
普通属性：0.............静态属性：2
普通属性：0.............静态属性：3
```

所以说：对于多例情况普通属性是不会共用的，不会产生影响，对于静态属性会去共用这个属性。

**第二次：类改为单例**

**![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330233705.webp)**

结果：

```
普通属性：0.............静态属性：0
普通属性：1.............静态属性：1
普通属性：2.............静态属性：2
普通属性：3.............静态属性：3
```

所以说：对于单例情况普通属性和静态属性都会被共用。

**第三次：类去掉@Scope注解**

**![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330233713.webp)**

结果：

```
普通属性：0.............静态属性：0
普通属性：1.............静态属性：1
普通属性：2.............静态属性：2
普通属性：3.............静态属性：3
```

所以说：springmvc默认是单例的。

另外在其他方法里面打印

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330233719.webp)

输出的结果是

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330233726.png)

跳到别的方法里面也并不会去取初始值，而是再去共用这个属性。

**总结**

尽量不要在 `controller` 里面去定义属性，如果在特殊情况需要定义属性的时候，那么就在类上面加上注解 `@Scope("prototype")` 改为多例的模式.

以前 `struts` 是基于类的属性进行发的，定义属性可以整个类通用，所以默认是多例，不然多线程访问肯定是共用类里面的属性值的，肯定是不安全的，但是 `springmvc` 是基于方法的开发，都是用形参接收值，一个方法结束参数就销毁了，多线程访问都会有一块内存空间产生，里面的参数也是不会共用的，所有 `springmvc` 默认使用了单例.

所以 `controller` 里面不适合在类里面定义属性，只要 `controller` 中不定义属性，那么单例完全是安全的。`springmvc` 这样设计主要的原因也是为了提高程序的性能和以后程序的维护只针对业务的维护就行，要是 `struts` 的属性定义多了，都不知道哪个方法用了这个属性，对以后程序的维护还是很麻烦的。

**怎么保证线程安全**

单例是不安全的，会导致属性重复使用。

- 不要在 `controller` 中定义成员变量。
- 万一必须要定义一个非静态成员变量时候，则通过注解 `@Scope(“prototype”)`，将其设置为多例模式。
- 在 `Controller` 中使用 `ThreadLocal` 变量

**为什么设计成单例设计模式?**

1. 性能(不用每次请求都创建对象)

2. 不需要多例(不要在控制器中定义成员变量)





























