[toc]



# Spring MVC

## 1 概述

### 1.1 Spring MVC特点

- 注解驱动：Spring MVC通过一套MVC注解，让POJO成为处理请求的控制器，无需实现任何接口
- REST风格：Spring MVC支持REST风格的URL请求

### 1.2 核心内容

Spring MVC框架围绕DispatcherServlet这个核心展开，它负责截获请求并将其分派给相应的处理器处理。Spring MVC框架包括注解驱动控制器、请求及响应的信息处理、视图解析、本地化解析、上传文件解析、异常处理以及表单标签绑定等内容。

### 1.3 体系结构

Spring MVC是基于Model 2实现的技术框架。Spring MVC通过一个DispatcherServlet接收所有请求，并将具体工作委托给其他组件进行处理。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223649.webp)

1. 客户端发出一个HTTP请求，Web应用服务器接收到这个请求，如果匹配DispatcherServlet的请求映射路径(在web.xml中指定)，Web容器将该请求转交给DispatcherServlet处理。
2. DispatcherServlet接收到这个请求后，将根据请求的信息（包括URL、HTTP方法、请求报文头、请求参数、Cookie等）及HandlerMapping的配置找到处理请求的处理器（Handler）。可将HandlerMapping看成路由控制器，将Handler看成目标主机。值得注意的是：Spring MVC中并没有定义一个Handler接口，实际上任何一个Object都可以成为请求处理器。
3. 当DispatcherServlet根据HandlerMapping得到对应当前请求的Handler后，通过HandlerAdapter对Handler进行封装，再以统一的适配器接口调用Handler。 HandlerAdapter是Spring MVC的框架级接口，顾名思义HandlerAdapter是一个适配器，它用统一的接口对各种Handler方法进行调用。
4. 处理器完成业务逻辑的处理后将运回一个ModelAndView给DispatcherServlet，ModelAndView包含了视图逻辑名和模型数据信息。
5. ModelAndView中包含的是“逻辑视图名”而非真正的视图对象，DispatcherServlet借由ViewResolver完成逻辑视图名到真实视图对象的解析工作。
6. 当得到真实的视图对象View后，DispatcherServlet就使用这个View对象对ModelAndView中的模型数据进行视图渲染。
7. 最终客户端得到的响应消息，可能是一个普通的HTML页而，也可能是一个XML或JSON串，   甚至是一张图片或一个PDF文档等不同的媒体形式。

### 1.4 配置DispatcherServlet

可以在web.xml中配置一个Servlet，并通过 `<servlet-mapping>` 指定其处理的URL。

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

1. 在(1)处，通过contextConfigLocation参数指定业务层Spring容器的配置文件（多个配置文件用`,`分割）。
2. 在(2)处，ContextLoaderListener是一个ServletLoaderListener，它通过contextConfigLocation指定的Spring配置文件启动业务层的Spring容器。
3. 在(3)处，配置了名为web的DispatcherServlet，它默认加载/WEB-INF/web-servlet.xml（`<servlet-name>-servlet.xml`）的Spring配置文件，启动Web层的Spring容器。Web层容器将作为业务层容器的子容器，Web层容器可以访问业务层容器的Bean，而业务层容器访问不了Web层容器的Bean。
4. 在(4)处，通过 `<servlet-mapping>` 指定DispatcherServlet处理`/*`全部的HTTP请求。一个web.xml可以配置多个DispatcherServlet，通过其对应的 `<servlet-mapping>` 配置，让每个DispatcherServlet处理不同的请求。

**DispatcherServlet 的配置参数**

可以通过 `<servlet>` 的 `<init-param>` 属性指定配置参数：

1. namespace参数：DispatcherServlet对应的命名空间，默认是`WEB-INF/<servlet-name>-servlet.xml`。在显式配置该参数后，新的配置文件对应的路径是`WEB-INF/<namespace>.xml`，例如如果将namespace设置为sample，则对应的Spring配置文件为WEB-INF/sample.xml。
2. contextConfigLocation：如果DispatcherServlet上下文对应的Spring配置文件有多个，则可以使用该属性按照Spring资源路径的方式指定，如`classpath:sample1.xml,classpath:sample2.xml`。
3. publishContext：默认为true。DispatcherServlet根据该属性决定是否将WebApplicationContext发布到ServletContext的属性列表中，方便调用者可借由ServletContext找到WebApplicationContext实例，对应的属性名为`DispatcherServlet#getServletContextAttributeName()`的返回值。
4. publishEvents：默认为true。当DispatcherServlet处理完一个请求后，是否需要向容器发布一个ServletRequestHandleEvent事件。

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

Spring 4.0已经全面支持Servlet 3.0，可以使用编程的方式配置Servlet容器。在Servlet 3.0环境中，容器会在类路径中查找实现`javax.servlet.ServletContainerInitializer`接口的类，如果发现实现类，就会用它来配置Servlet容器。Spring提供了这个接口的实现，名为SpringServletContainerInitializer，这个类反过来又查找实现WebApplicationInitializer的类并将配置的任务交给它们来完成。Spring还提供了一个WebApplicationInitializer基础实现类AbstractAnnotationConfigDispatcherServletInitializer，使得它在注册DispatcherServlet时只需要简单地指定它的Servlet映射即可。

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

DispatcherServlet#initStrategies()方法将在WebApplicationContext初始化后执行，此时Spring上下文中的Bean已经初始化完毕，该方法通过反射查找并装配Spring容器中用户自定义的Bean，如果找不到就装配默认的组件实例。

**默认组件**

在DispatcherServlet.properties配置文件里边，指定了DispatcherServlet所使用的默认组件。如果用户希望采用非默认的组件，只需在Spring配置文件中配置自定义的组件Bean即可。

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

## 2 注解驱动的控制器

### 2.1 @RequestMapping映射请求

1. 在POJO类上标注@Controller，再通过 `<context:component-scan>` 扫描到该类，可以是POJO成为一个能处理HTTP请求的控制器。
2. 在控制器的类定义和方法定义处都可以使用@RequestMapping映射对应的处理方法。
3. @RequestMapping不但支持标准的URL，还支持Ant风格和{XXX}占位符的URL。
4. @RequestMapping和value、method、params及headers分别表示请求路径、请求方法、请求参数及报文头的映射条件。

### 2.2 获取请求内容

1. 可以通过@RequestParam、@RequestHeader、@PathVariable获取HTTP请求信息。
2. 可以使用@CookieValue让方法入参绑定某个Cookie值
3. 可以使用@MatrixVariable注解将请求中的矩阵变量绑定到处理器的方法参数中。
4. 可以使用命令/表单对象（就是一个POJO）绑定请求参数值，Spring会按照请求参数名和对象属性名匹配的方式，自动为该对象填充属性。
5. 可以使用Servlet API的类作为处理方法的入参，如HttpServletRequest、HttpServletResponse、HttpSession；如果使用HttpServletResponse返回相应，则处理方法返回着设置成void即可；在`org.springframework.web.context.request`定义了若干个可代理Servlet原生API类的接口，如WebRequest和NativeWebRequest。
6. 可以使用java.io中的InputStream、Reader、OutputStream、Writer作为方法的入参。
7. 还可以使用java.util.Locale、java.security.Principal作为入参。

### 2.3 使用HttpMessageConverter

HttpMessageConverter接口可以将请求信息转换为一个对象（类型为T），并将对象（类型为T）绑定到请求方法的参数中或输出为响应信息。DispatcherServlet默认已经安装了RequestMethodHandlerAdapter作为HandlerAdapter组件的实现类，HttpMessageConverter即由RequestMethodHandlerAdapter使用，将请求信息转换为对象，或将对象转换为响应信息。

**HttpMessageConverter的实现类**

Spring为HttpMessageConverter提供了众多的实现类：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223942.webp)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330223959.webp)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330224014.webp)

**默认的HttpMessageConverter**

RequestMappingHandlerAdapter已经默认装配了以下的HttpMessageConverter：

- StringHttpMessageConverter
- ByteArrayHttpMessageConverter
- SourceHttpMessageConverter
- AllEncompassingFormHttpMessageConverter

**装配其他类型的HttpMessageConverter**

如果需要装配其他类型的HttpMessageConverter，可以在Spring的Web容器上下文中自行定义一个RequestMappingHandlerAdapter，注册若干HttpMessageConverter。如果在Spring web容器中显式定义了一个RequestMappingHandlerAdapter，则Spring MVC将使用它**覆盖**默认的RequestMappingHandlerAdapter。

**使用HttpMessageConverter**

1. 可以使用@RequestBody、@ResponseBody对处理方法进行标注
2. 可以使用HttpEntity<T>、ResponseEntity<T>作为处理方法的入参或返回值

> RestTemplate是Spring的模板类，可以使用该类调用Web服务端的服务，它支持Rest风格的URL。

**结论**

1. 当控制器处理方法使用到@RequestBody、@ResponseBody 或 HttpEntity<T>、ResponseEntity<T> 时，Spring MVC才会使用注册的HttpMessageConvertor对请求、相应消息进行处理。
2. 当控制器处理方法使用到@RequestBody、@ResponseBody 或 HttpEntity<T>、ResponseEntity<T>时，Spring 首先根据请求头或响应的Accept属性选择匹配的 HttpMessageConverter，进而根据参数类型或泛型类型的过滤得到匹配的 HttpMessageConverter，若找不到可用的 HttpMessageConverter 将报错。
3. @RequestBody、@ResponseBody不需要成对出现。

**处理XML和JSON**

Spring MVC提供了几个处理XML和JSON格式的请求、响应消息的HttpMessageConverter：

- MarshallingHttpMessageConverter：处理XML
- Jaxb2RootElementHttpMessageConverter：处理XML，底层使用JAXB
- MappingJackson2HttpMessageConverter：处理JSON格式

只要在Spring Web容器中为RequestMappingHandlerAdapter装配好相应的HttpMessageConverter，并在交互中通过请求的Accept指定MIME类型，Spring MVC就可以是服务器段的处理方法和客户端透明的通过XML或JSON格式进行通信。

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

@RestController已经标注了@ResponseBody和@Controller，可以直接在控制器上标注该注解，就不用在每个@RequestMapping方法上添加@ResponseBody了。

### 2.5 AsyncRestTemplate

Spring 4.0提供了AsyncRestTemplate用于以异步无阻塞的方式进行服务访问。

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

1. ModelAndView：当处理方法返回值类型为ModelAndView时，方法体即可通过该对象添加模型数据；
2. @ModelAttribute：在方法入参标注该注解后，入参的对象就会放到数据模型中；
3. Map和Model：如果方法入参为org.framework.ui.Model、org.framework.ui.ModelMap、java.util.Map，当处理方法返回时，Map中的数据会自动添加到模型中；
4. @SessionAttributes：将模型中的某个属性暂存到HttpSession中，以便多个请求之间可以共享这个属性。

## 3 处理方法的数据绑定

Spring会根据请求方法签名的不同，将请求中的信息以一定方式转换并绑定到请求方法的入参中，还会进行数据转换、数据格式化及数据校验等。

### 3.1 数据绑定流程

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330224125.webp)

Spring MVC通过反射对目标签名进行分析，将请求消息绑定到处理方法的入参中。数据绑定的核心部件是DataBinder。Spring MVC主框架将ServletRequest对象及处理方法的入参对象实例传递给 DataBinder，DataBinder 首先调用装配在 Spring Web 上下文中的 ConversionService 组件进行数据类型转换、数据格式化等工作，将ServletRequest中的消息填充到入参对象中， 然后调用Validator组件对己经绑定了请求消息数据的入参对象进行数据合法性校验，最 终生成数据绑定结果BindingResult对象。BindingResult包含了已完成数据绑定的入参 对象，还包含相应的校验错误对象。Spring MVC抽取BindingResult中的入参对象及校验错误对象，将它们赋给处理方法的相应入参。

### 3.2 数据转换

类型转换模块位于org.framework.core.convert包中，同时由于历史原因，Spring还支持JDK的PropertyEditor。

**ConversionService简介**

ConversionService 是 Spring 类型转换体系的核心接口，它定义了以下4个方法：

- `boolean canConvert(Class<?> sourceType, Class<?> targetType)`：判断是否可以将一个Java类转换为另一个Java类。
- `Boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType)`：需转换的类将以成员变量的方式出现在宿主类中。TypeDescriptor不但描述了需转换类的信息，还描述了从宿主类的上下文信息，如成员变量上的注解，成员变量是否以数组、集合或Map的方式呈现等。类型转换逻辑可以利用这些信息做出 各种灵活的控制。
- `<T> T convert(Object source, Class<T> targetType)`：将原类型对象转换为目标类型对象。
- `Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType)`：
   将对象从原类型对象转换为目标类型对象，此时往往会用到所在宿主类的上下 文信息。

第一个和第三个接口方法类似于PmpertyEditor，它们不关注类型对象所在的上下文 信息，只简单地完成两个类型对象的转换，唯一的区别在于这两个方法支持任意两个类型的转换。而第二个和第四个接口方法会参考类型对象所在宿主类的上下文信息，并利用这些信息进行类型转换。

**使用ConversionService**

可以利用 `org.springframework.context.support.ConversionServiceFactoryBean` 在 Spring 的 上下文中定义一个ConversionService。Spring将自动识别出上下文中的ConversionService， 并在Bean属性配置及Spring MVC处理方法入参绑定等场合使用它进行数据转换。该FactoryBean创建ConversionService内建了很多转换器，可完成大多数Java类型的转换工作。除了包括将String对象转换为各种基础类型的对象外，还包括String、 Number、Array、Collection、Map、Properties 及 Object 之间的转换器。可通过ConversionServiceFactoryBean的converters属性注册自定义的类型转换器：

```xml
    <bean class="org.springframework.context.support.ConversionServiceFactoryBean">
        <property name="converters">
            <list>
                <bean class="com.ankeetc.MyConverter"/>
            </list>
        </property>
    </bean>
```

**Spring支持的转换器**

Spring 在 org.springframework.core.convert.converter 包中定义了3种类型的转换器接口，实现任意一个转换器接口都可以作为自定义转换器注册到ConversionServiceFactoryBean中。这3种类型的转换器接口分别为：

- Converter<S, T>：将S类型的对象转换为T类型的对象
- GenericConverter：根据源类对象及目标类对象所在的宿主类的上下文信息进行类型转换工作。该类还有一个子接口ConditionalGenericConverter，它添加了一个接口方法根据源类型及目标类型所在宿主类的上下文信息决定是否要进行类型转换。
- ConverterFactory：

ConversionServiceFactoryBean 的 converters 属性可接受 Converter、ConverterFactory、 GenericConverter或ConditionalGenericConverter接口的实现类，并把这些转换器的转换逻辑统一封装到一个 ConversionService 实例对象中（GenericConversionService)。Spring 在Bean属性配置及Spring MVC请求消息绑定时将利用这个ConversionService实例完成类型转换工作。

**在Spring中使用@lnitBinder 和 WebBindingInitializer装配自定义编辑器**

Spring也支持JavaBeans的PropertyEditor。可以在控制器中使用@InitBinder添加自定义的编辑器，也可以通过 WebBindingInitializer 装配在全局范围内使用的编辑器。

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

如果希望在全局范围内使用，则可实现WebBindingInitializer接口并在该实现类中注册。

1. 实现WebBindingInitializer接口并在initBinder接口方法中注册了自定义的编辑器。
2. 在 Spring 上下文中通过 RequestMappingHandlerAdapter 装配自定义的Initializer。

**顺序**

对于同一个类型对象来说，如果既在ConversionService中装配了自定义转换器，又通过WebBindinglnitializer装配了自定义编辑器，同时还在控制器中通过@InitBinder装 配了自定义编辑器，那么Spring MVC将按以下优先顺序查找对应类型的编辑器：

1. 查询通过@InitBinder装配的自定义编辑器。
2. 查询通过ConversionService装配的自定义转换器。
3. 查询通过WebBindingInitializer装配的自定义编辑器。

### 3.3 数据格式化

Spring的转换器并不提供输入及输出信息格式化的工作，一般需要转换的源类型数据（一般是字符串）都是具有一定格式的，在不同的本地化环境中， 同一类型的数据还会相应地呈现不同的显示格式。Spring引入了一个新的格式化框架，这个框架位于org.springframework.format类包中。

**最重要的 `Formatter<T>`接口**

**注解驱动格式化AnnotationFormatterFactory**

为了让注解和格式化的属性类型关联起来，Spring在Formatter<T>所在的包中还提供了一个 AnnotationFormatterFactory<A extends Annotation>接口。

**启用注解驱动格式化功能**

对属性对象的输入/输出进行格式化，从本质上讲依然属于“类型转换”的范畴。 Spring就是基于对象转换框架植入“格式化”功能的。Spring 在格式化模块中定义了一个实现 ConversionService 接口的 FormattingConversionService实现类，该实现类扩展了 GenericConversionService，因此它既具有类型转换功能，又具有格式化功能。
 FormattingConversionService 也拥有一个对应的 **FormattingConversionServiceFactoryBean** 工厂类，后者用于在Spring上下文中构造一个FormattingConversionService。通过这个工厂类，既可以注册自定义的转换器，还可以注册自定义的注解驱动逻辑。由于 FormattingConversionServiceFactoryBean 在内部会自动注册 NumberFormatAnnotationFormatterFactory 和 JodaDateTimeFormatAnnotationFormatterFactory，因此装配了 FormattingConversionServiceFactoryBean 后，就可以在 Spring MVC 入参绑定及模型数据输出时使用注解驱动的格式化功能。
 值得注意的是，`<mvc:annotation-driven/>`标签内部默认创建的ConversionService实例就是一个 FormattingConversionServiceFactoryBean。

### 3.4 数据校验

Spring拥有自己独立的数据校验框架，同时支持JSR-303标准的校验框架。Spring 的DataBinder在进行数据绑定时，可同时调用校验框架完成数据校验工作。在Spring MVC中，则可直接通过注解驱动的方式进行数据校验。
 LocalValidatorFactoryBean 既实现了 Spring 的 Validator 接口，又实现了 JSR-303 的 Validator 接口。只要在 Spring 容器中定义了一个 LocalValidatorFactoryBean，即可将其注入需要数据校验的Bean中。值得注意的是，Spring本身没有提供JSR-303的实现，所以必须将JSR-303的实现 者（如Hibernate Validator)的JAR文件放到类路径下，Spring将自动加载并装配好 JSR-303的实现者。
 `<mvc:annotation-driven/>` 会默认装配一个 LocalValidatorFactoryBean，通过在处理方法的入参上标注@Valid注解，即可让Spring MVC在完成数据绑定后执行数据校验工作。

### 3.5.Spring MVC处理请求、响应JSON数据

Spring MVC默认使用MappingJackson2HttpMessageConverter转换JSON格式的数据，使用Jackson可以比较容易的json、java对象之间相互转换，因此在使用时需要引入相关jackson的jar包：jackson-core-asl.jar、jackson-mapper-asl.jar

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

定义JSON转换器，在Controller使用@ResponseBody表示返回值转换成JSON文本；注意，对于spring4.x,使用MappingJackson2HttpMessageConverter，spring3.x使用MappingJacksonHttpMessageConverter。若是配置文件中使用了<mvc:annotation-driven />，则会自动提供读取JSON的功能，下面的配置则可省略。

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
使用默认的contentType，默认值为application/x-www-form-urlencoded，发送js数据（key:value形式）；后台可以通过@RequestParam、@ModelAttribute、@RequestBody等注解处理

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
指定请求的contentType:'application/json'，此时发送数据需为json字符串，否则提示400错误；对于设置contentType为application/json、application/xml后，后台需要通过@RequestBody注解接收，否则值都为null

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

- 注意jackson相关jar的引入，以及springmvc.xml文件中定义JSON转换器的配置，spring3.x/4.x用的两个转换器名称上略有不同，当时坑了我好久才发现
- 若springmvc.xml配置文件中使用了<mvc:annotation-driven />，则会自动提供读取JSON的功能，此时步骤2可省略；
- 对于GET/POST请求，请求头的contentType不同分为以下几种情况：
  - application/x-www-urlencoded（默认），可以使用@RequestParam、@RequestBody、@ModelAttribute处理；
  - multipart/form-data，可使用@RequestParam，对于文件则可以使用MultipartFile类接收处理；@RequestBody不能处理；
  - application/json、application/xml等格式，须使用@RequestBody处理



## 4 视图和视图解析器

## 5 本地化

Spring提供了以下4个本地化解析器。

- AcceptHeaderLocaleResolver:根据 HTTP 报文头的 Accept-Language 参数确定本 地化类型。如果没有显式定义本地化解析器，则Spring MVC默认采用 AcceptHeaderLocaleResolver。
- CookieLocaleResolver:根据指定的Cookie值确定本地化类型。
- SessionLocaleResolver:根据Session中特定的属性值确定本地化类型。
- LocaleChangeInterceptor:从请求参数中获取本次请求对应的本地化类型。

## 6 文件上传

Spring MVC为文件上传提供了直接支持，这种支持是通过即插即用的MultipartResolver 实现的。Spring 使用 Jakarta Commons FileUpload 技术实现了一个 MultipartResolver 实现 类：CommonsMultipartResolver。
 在Spring MVC上下文中默认没有装配MultipartResolver,因此默认情况下不能 处理文件的上传工作。如果想使用Spring的文件上传功能，则需要先在上下文中配置 MultipartResolver。

## 7 WebSocket

## 8 静态资源处理

1. `<mvc:default-servlet-handler/>`：在 smart-servlet.xml 中配置 `<mvc:default-servlet-handler/>` 后，会在 Spring MVC 上下文中定义一个 org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler，它将充当一个检查员的角色，对进入DispatcherServlet的URL进行筛查。如果发现是静态资源的请求，就将该请求转由Web应用服务器默认的Servlet处理；如果不是静态资源 的请求，则由DispatcherServlet继续处理。
2. `<mvc:resources/>`：`<mvc:default-servlet-handler/>`将静态资源的处理经由Spring MVC框架交回Web应 用服务器。而`<mvc:resources/>`更进一步，由SpringMVC框架自己处理静态资源，并添 加一些有用的附加功能。

## 9 拦截器

当收到请求时，DispatcherServlet将请求交给处理器映射（HandlerMapping)，让它找出对应该请求的HandlerExecutionChain对象。在讲解HandlerMapping之前，有必要 认识一下这个 HandlerExecutionChain 对象。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330224339.webp)
 HandlerExecutionChain负责处理请求并返回ModelAndView的处理执行链，它包含一个处理该请求的处理器 (Handler)，同时包括若干个对该请求实施拦截的拦截器（HandlerInterceptor)。当 HandlerMapping 返回 HandlerExecutionChain 后，DispatcherServlet 将请求交给定义在 HandlerExecutionChain中的拦截器和处理器一并处理。
 位于处理器链末端的是一个 Handler，DispatcherServlet通过 Handler Adapter适配器对 Handler进行封装，并按统一的适配器接口对 Handler处理方法进行调用。可以在 `web-servlet.xml`中配置多个拦截器,每个拦截器都可以指定一个匹配的映射路径，以限制拦截器的作用范围。

## 10 异常处理

Spring MVC通过 HandlerExceptionResolver处理程序的异常，包括处理器映射、数据绑定及处理器执行时发生的异常。 HandlerExceptionResolver仅有一个接口方法：`Modelandview resolveException(HttpServletRequest request HttpServletResponse response Object handler, Exception ex)`。当发生异常时，Spring MVC将调用 resolveException方法，并转到 ModelAndView 对应的视图中，作为一个异常报告页面反馈给用户。

**实现类**

HandlerExceptionResolver拥有4个实现类

1. DefaultHandlerExceptionResolver：默认装配了该类，将对应异常转换为错误码
2. SimpleMappingExceptionResolver：对所有异常进行统一处理
3. AnnotationMethodHandlerExceptionResolver：默认注册了该类，允许通过@ExceptionHandler注解指定处理特定的异常
4. ResponseStatusExceptionResolver



## 11.常用注解

### 11.1.先说扫描注解

```
<context:component-scan base-package = "" />
```

component-scan 默认扫描的注解类型是 @Component，不过，在 @Component 语义基础上细化后的 @Repository, @Service 和 @Controller 也同样可以获得 component-scan 的青睐 

有了 `<context:component-scan>`，另一个 `<context:annotation-config/>` 标签根本可以移除掉，因为已经被包含进去了 
另外 `<context:annotation-config/>` 还提供了两个子标签 

1. `<context:include-filter> `//指定扫描的路径 

2. `<context:exclude-filter>` //排除扫描的路径 

`<context:component-scan>` 有一个use-default-filters属性，属性默认为true,表示会扫描指定包下的全部的标有@Component的类，并注册成bean.也就是@Component的子注解@Service,@Reposity等。 

这种扫描的粒度有点太大，如果你只想扫描指定包下面的Controller或其他内容则设置use-default-filters属性为false，表示不再按照scan指定的包扫描，而是按照 `<context:include-filter>` 指定的包扫描，示例：

```
<context:component-scan base-package="com.tan" use-default-filters="false">
        <context:include-filter type="regex" expression="com.tan.*"/>//注意后面要写.*
</context:component-scan>
```

当没有设置use-default-filters属性或者属性为true时，表示基于base-packge包下指定扫描的具体路径

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

在SpringMVC 中，控制器Controller 负责处理由DispatcherServlet 分发的请求，它把用户请求的数据经过业务处理层处理之后封装成一个Model ，然后再把该Model 返回给对应的View 进行展示。在SpringMVC 中提供了一个非常简便的定义Controller 的方法，你无需继承特定的类或实现特定的接口，只需使用@Controller 标记一个类是Controller ，然后使用@RequestMapping 和@RequestParam 等一些注解用以定义URL 请求和Controller 方法之间的映射，这样的Controller 就能被外界访问到。此外Controller 不会直接依赖于HttpServletRequest 和HttpServletResponse 等HttpServlet 对象，它们可以通过Controller 的方法参数灵活的获取到

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

ModelAdnView的addObject底层其实就是往HashMap里put值，我们可以看下源码实现

```
public ModelAndView addObject(Object attributeValue)
{
    getModelMap().addAttribute(attributeValue);
    return this;
}
```

在进一步看下addAttribute的实现

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

@Controller 用于标记在一个类上，使用它标记的类就是一个SpringMVC Controller 对象。分发处理器将会扫描使用了该注解的类的方法，并检测该方法是否使用了@RequestMapping 注解。@Controller 只是定义了一个控制器类，而使用@RequestMapping 注解的方法才是真正处理请求的处理器 。单单使用@Controller 标记在一个类上还不能真正意义上的说它就是SpringMVC 的一个控制器类，因为这个时候Spring 还不认识它。把这个控制器类交给Spring 来管理的方法有两种如下 
1、在SpringMVC的配置文件中定义SpringMVCController的bean 对象

```
<bean class="com.nyonline.sp2p.controller.SpringMVCController"/>
```

2、在SpringMVC的配置文件中让Spring去扫描获取

```
< context:component-scan base-package = "com.nyonline.sp2p.controller" >
       < context:exclude-filter type = "annotation"
           expression = "org.springframework.stereotype.Service" /> <!-- 排除@service -->
</ context:component-scan> 
```

### 11.3.@RequestMapping

RequestMapping是一个用来处理请求地址映射的注解，可用于类或方法上。用于类上，表示类中的所有响应请求的方法都是以该地址作为父路径。RequestMapping注解有六个属性，下面我们把她分成三类进行说明 

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

consumes：指定处理请求的提交内容类型（Content-Type），例如application/json, text/html; 

produces: 指定返回的内容类型，仅当request请求头中的(Accept)类型中包含该指定类型才返回

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

params：指定request中必须包含某些参数值是，才让该方法处理。 

headers：指定request中必须包含某些指定的header值，才能让该方法处理请求。

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

@Resource和@Autowired都是做bean的注入时使用，其实@Resource并不是Spring的注解，它的包是javax.annotation.Resource，需要导入，但是Spring支持该注解的注入。 

- 共同点 

  两者都可以写在字段和setter方法上。两者如果都写在字段上，那么就不需要再写setter方法 

- 不同点 

  @Autowired为Spring提供的注解，需要导入包org.springframework.beans.factory.annotation.Autowired;只按照byType注入。

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

@Autowired注解是按照类型（byType）装配依赖对象，默认情况下它要求依赖对象必须存在，如果允许null值，可以设置它的required属性为false。如果我们想使用按照名称（byName）来装配，可以结合@Qualifier注解一起使用。如下：

```
public class TestServiceImpl {
    @Autowired
    @Qualifier("userDao")
    private UserDao userDao; 
}
```


@Resource默认按照ByName自动注入，由J2EE提供，需要导入包javax.annotation.Resource。@Resource有两个重要的属性：name和type，而Spring将@Resource注解的name属性解析为bean的名字，而type属性则解析为bean的类型。所以，如果使用name属性，则使用byName的自动注入策略，而使用type属性时则使用byType自动注入策略。如果既不制定name也不制定type属性，这时将通过反射机制使用byName自动注入策略。

    @Resource(name="ad")
    private AdBo ad;
    
    @Resource(type=BidBo.class)
    private BidBo bid;
- 如果同时指定了name和type，则从Spring上下文中找到唯一匹配的bean进行装配，找不到则抛出异常。
- 如果指定了name，则从上下文中查找名称（id）匹配的bean进行装配，找不到则抛出异常。 

- 如果指定了type，则从上下文中找到类似匹配的唯一bean进行装配，找不到或是找到多个，都会抛出异常。 

- 如果既没有指定name，又没有指定type，则自动按照byName方式进行装配；如果没有匹配，则回退为一个原始类型进行匹配，如果匹配则自动装配。 

- @Resource的作用相当于@Autowired，只不过@Autowired按照byType自动注入。

### 11.5.@ModelAttribute

代表的是：该Controller的所有方法在调用前，先执行此@ModelAttribute方法，可用于注解和方法参数中，可以把这个@ModelAttribute特性，应用在BaseController当中，所有的Controller继承BaseController，即可实现在调用Controller时，先执行@ModelAttribute方法。 

@modelAttribute可以在两种地方使用，参数和方法体。

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

当我们发出/testUser.do这个请求时，SpringMvc 在执行该请求前会先逐个调用在方法级上标注了 
@ModelAttribute 的方法，然后将该模型参数放入testUser()函数的Map参数中 

执行结果： 

User{id=123456,name=”sunwukong”,password=”admin”} 

作用在参数上 

SpringMVC先从模型数据中获取对象，再将请求参数绑定到对象中，再传入形参，并且数据模型中的对象会被覆盖

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

访问http://localhost/test1.do?name=qitiandasheng，输出结果： 

User{id=123456,name=”齐天大圣”,password=”admin”} 

我们传入了name此时user的name被传入的值覆盖，如果请求参数中有的参数已经绑定到了user中，那么请求参数会覆盖掉user中已存在的值，并且user对象会被放入数据模型中覆盖掉原来的user1对象。也就是模型数据中的user1的优先级低于请求参数

### 11.6.@SessionAttributes

该注解用来绑定HttpSession中的attribute对象的值，便于在方法中的参数里使用。

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

当我第一次执行test4.do输出结果： 

```
User [id=123456, name=sunwukong, password=admin] 
session:null 
```

第二次执行 

```
User [id=123456, name=sunwukong, password=admin] 
session:User [id=123456, name=sunwukong, password=admin] 
```

只是在第一次访问 test4.do 的时候 @SessionAttributes 定义了需要存放到 session 中的属性，而且这个模型中也有对应的属性，但是这个时候还没有加到 session 中，所以 session 中不会有任何属性，等处理器方法执行完成后 Spring 才会把模型中对应的属性添加到 session 中。

### 11.7.@PathVariable和@RequestParam

请求路径上有个id的变量值，可以通过@PathVariable来获取 ` @RequestMapping(value = "/page/{id}", method = RequestMethod.GET) `
@RequestParam用来获得静态的URL请求入参,spring注解时action里用到。它有三个常用参数：defaultValue = “0”, required = false, value = “pageNo”；defaultValue 表示设置默认值，required 通过boolean设置是否是必须要传入的参数，value 值表示接受的传入的参数类型。 

例如： 

地址① 

http://localhost:8989/test/index?pageNo=2 

地址② 

http://localhost:8989/test/index/7 

如果想获取地址①中的 pageNo的值 ‘2’ ，则使用 @RequestParam ， 

如果想获取地址②中的 emp/7 中的 ‘7 ’ 则使用 @PathVariable

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

该注解用于将Controller的方法返回的对象，通过适当的HttpMessageConverter转换为指定格式后，写入到Response对象的body数据区。 

使用时机：返回的数据不是html标签的页面，而是其他某种格式的数据时（如json、xml等）使用；

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

如果用SpringMVC注解

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

用于注解dao层，在daoImpl类上面注解。 

@Repository(value=”userDao”)注解是告诉Spring，让Spring创建一个名字叫“userDao”的UserDaoImpl实例 

@RequestBody 

@RequestBody接收的是一个Json对象的字符串，而不是一个Json对象。然而在ajax请求往往传的都是Json对象，后来发现用 `JSON.stringify(data)`的方式就能将对象变成字符串。同时ajax请求的时候也要指定 `dataType: “json”,contentType:”application/json”` 这样就可以轻易的将一个对象或者List传到Java端，使用@RequestBody即可绑定对象或者List

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

- 第一种方式是要定义的Interceptor类要实现了Spring的HandlerInterceptor 接口

- 第二种方式是继承实现了HandlerInterceptor接口的类，比如Spring已经提供的实现了HandlerInterceptor接口的抽象类HandlerInterceptorAdapter



HandlerInterceptor 接口中定义了三个方法，我们就是通过这三个方法来对用户的请求进行拦截处理的。

- **preHandle()**： 这个方法在业务处理器处理请求之前被调用，SpringMVC 中的Interceptor 是链式的调用的，在一个应用中或者说是在一个请求中可以同时存在多个Interceptor 。每个Interceptor 的调用会依据它的声明顺序依次执行，而且最先执行的都是Interceptor 中的preHandle 方法，所以可以在这个方法中进行一些前置初始化操作或者是对当前请求的一个预处理，也可以在这个方法中进行一些判断来决定请求是否要继续进行下去。该方法的返回值是布尔值Boolean 类型的，当它返回为false 时，表示请求结束，后续的Interceptor 和Controller 都不会再执行；当返回值为true 时就会继续调用下一个Interceptor 的preHandle 方法，如果已经是最后一个Interceptor 的时候就会是调用当前请求的Controller 方法。

- **postHandle()**：这个方法在当前请求进行处理之后，也就是Controller 方法调用之后执行，但是它会在DispatcherServlet 进行视图返回渲染之前被调用，所以我们可以在这个方法中对Controller 处理之后的ModelAndView 对象进行操作。postHandle 方法被调用的方向跟preHandle 是相反的，也就是说先声明的Interceptor 的postHandle 方法反而会后执行。

- **afterCompletion()**：该方法也是需要当前对应的Interceptor 的preHandle 方法的返回值为true 时才会执行。顾名思义，该方法将在整个请求结束之后，也就是在DispatcherServlet 渲染了对应的视图之后执行。这个方法的主要作用是用于进行资源清理工作的。

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





## 面试题

### 1.SpringMVC的控制器是单例的吗?

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

尽量不要在controller里面去定义属性，如果在特殊情况需要定义属性的时候，那么就在类上面加上注解@Scope("prototype")改为多例的模式.

以前struts是基于类的属性进行发的，定义属性可以整个类通用，所以默认是多例，不然多线程访问肯定是共用类里面的属性值的，肯定是不安全的，但是springmvc是基于方法的开发，都是用形参接收值，一个方法结束参数就销毁了，多线程访问都会有一块内存空间产生，里面的参数也是不会共用的，所有springmvc默认使用了单例.

所以controller里面不适合在类里面定义属性，只要controller中不定义属性，那么单例完全是安全的。springmvc这样设计主要的原因也是为了提高程序的性能和以后程序的维护只针对业务的维护就行，要是struts的属性定义多了，都不知道哪个方法用了这个属性，对以后程序的维护还是很麻烦的。

**怎么保证线程安全**

单例是不安全的，会导致属性重复使用。

- 不要在controller中定义成员变量。
- 万一必须要定义一个非静态成员变量时候，则通过注解@Scope(“prototype”)，将其设置为多例模式。
- 在Controller中使用ThreadLocal变量

**为什么设计成单例设计模式?**

1. 性能(不用每次请求都创建对象)

2. 不需要多例(不要在控制器中定义成员变量)





























