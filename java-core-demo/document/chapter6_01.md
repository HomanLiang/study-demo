[toc]



# Tomcat

## 1.写一个迷你版的Tomcat
Tomcat是非常流行的Web Server，它还是一个满足Servlet规范的容器。那么想一想，Tomcat和我们的Web应用是什么关系？

从感性上来说，我们一般需要把Web应用打成WAR包部署到Tomcat中，在我们的Web应用中，我们要指明URL被哪个类的哪个方法所处理（不论是原始的Servlet开发，还是现在流行的Spring MVC都必须指明）。

由于我们的Web应用是运行在Tomcat中，那么显然，请求必定是先到达Tomcat的。Tomcat对于请求实际上会进行下面的处理：

1. 提供Socket服务

   Tomcat的启动，必然是Socket服务，只不过它支持HTTP协议而已！

   这里其实可以扩展思考下，Tomcat既然是基于Socket，那么是基于BIO or NIO or AIO呢？

2. 进行请求的分发

   要知道一个Tomcat可以为多个Web应用提供服务，那么很显然，Tomcat可以把URL下发到不同的Web应用。

3. 需要把请求和响应封装成request/response

   我们在Web应用这一层，可从来没有封装过request/response的，我们都是直接使用的，这就是因为Tomcat已经为你做好了！

话不多说，先来看一眼工程截图：

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213627.png)

**MyRequest**

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213637.png)

这里，你可以清楚的看到，我们通过输入流，对HTTP协议进行解析，拿到了HTTP请求头的方法以及URL。

**MyResponse**

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213644.png)

基于HTTP协议的格式进行输出写入。

**MyServlet**

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213652.png)

前文说Tomcat是满足Servlet规范的容器，那么自然Tomcat需要提供API。这里你看到了Servlet常见的doGet/doPost/service方法。

**FindGirlServlet和HelloWorldServlet**

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213657.png)

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213702.png)

提供这2个具体的Servlet实现，只是为了后续的测试！

**ServletMapping和ServletMappingConfig**

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213707.png)

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213712.png)

你应该有些感觉了吧？

我们在servlet开发中，会在web.xml中通过 `<servlet></servlet>` 和 `<servlet-mapping></servlet-mapping>` 来进行指定哪个URL交给哪个servlet进行处理。

**MyTomcat**

![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213717.png)

**start方法**

![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213723.png)

![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213727.png)

这里，你能够看到Tomcat的处理流程：把URL对应处理的Servlet关系形成，解析HTTP协议，封装请求/响应对象，利用反射实例化具体的Servlet进行处理即可。

**Test MyTomcat**

![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213733.png)

## 2.详解Tomcat 配置文件server.xml
`server.xml` 位于 `$TOMCAT_HOME/conf` 目录下；下面是一个 `server.xml` 实例。后文中将结合该实例讲解 `server.xml` 中，各个元素的含义和作用；在阅读后续章节过程中，可以对照该xml文档便于理解。

```
<Server port="8005" shutdown="SHUTDOWN">
  <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
  <Listener className="org.apache.catalina.core.JasperListener" />
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />

  <GlobalNamingResources>
    <Resource name="UserDatabase" auth="Container"
              type="org.apache.catalina.UserDatabase"
              description="User database that can be updated and saved"
              factory="org.apache.catalina.users.MemoryUserDatabaseFactory"
              pathname="conf/tomcat-users.xml" />
  </GlobalNamingResources>
 
  <Service name="Catalina">
    <Connector port="8080" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
    <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
    <Engine name="Catalina" defaultHost="localhost">
      <Realm className="org.apache.catalina.realm.LockOutRealm">
        <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
               resourceName="UserDatabase"/>
      </Realm>
 
      <Host name="localhost"  appBase="webapps"
            unpackWARs="true" autoDeploy="true">
        <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs"
               prefix="localhost_access_log." suffix=".txt"
               pattern="%h %l %u %t &quot;%r&quot; %s %b" />
      </Host>
    </Engine>
  </Service>
</Server>
```
### 2.1.server.xml文档的元素分类和整体结构
#### 2.1.1.整体结构
server.xml的整体结构如下：
```
<Server>
    <Service>
        <Connector />
        <Connector />
        <Engine>
            <Host>
                <Context /><!-- 现在常常使用自动部署，不推荐配置Context元素，Context小节有详细说明 -->
            </Host>
        </Engine>
    </Service>
</Server>
```
该结构中只给出了Tomcat的核心组件，除了核心组件外，Tomcat还有一些其他组件，下面介绍一下组件的分类。
#### 2.1.2.元素分类
server.xml文件中的元素可以分为以下4类：
1. 顶层元素：`<Server>` 和 `<Service>`

    `<Server>` 元素是整个配置文件的根元素，`<Service>` 元素则代表一个Engine元素以及一组与之相连的Connector元素。

2. 连接器：`<Connector>`

   `<Connector>`代表了外部客户端发送请求到特定Service的接口；同时也是外部客户端从特定Service接收响应的接口。

3. 容器：`<Engine><Host><Context>`

   容器的功能是处理Connector接收进来的请求，并产生相应的响应。Engine、Host和Context都是容器，但它们不是平行的关系，而是父子关系：Engine包含Host，Host包含Context。一个Engine组件可以处理Service中的所有请求，一个Host组件可以处理发向一个特定虚拟主机的所有请求，一个Context组件可以处理一个特定Web应用的所有请求。

4. 内嵌组件：可以内嵌到容器中的组件。实际上，Server、Service、Connector、Engine、Host和Context是最重要的最核心的Tomcat组件，其他组件都可以归为内嵌组件。

下面将详细介绍Tomcat中各个核心组件的作用，以及相互之间的关系。
### 2.2.核心组件
本部分将分别介绍各个核心组件的作用、特点以及配置方式等。
#### 2.2.1.Server
Server元素在最顶层，代表整个Tomcat容器，因此它必须是server.xml中唯一一个最外层的元素。一个Server元素中可以有一个或多个Service元素。

在第一部分的例子中，在最外层有一个 `<Server>` 元素，shutdown属性表示关闭Server的指令；port属性表示Server接收shutdown指令的端口号，设为-1可以禁掉该端口。

Server的主要任务，就是提供一个接口让客户端能够访问到这个Service集合，同时维护它所包含的所有的Service的声明周期，包括如何初始化、如何结束服务、如何找到客户端要访问的Service。

#### 2.2.2.Service
Service的作用，是在Connector和Engine外面包了一层，把它们组装在一起，对外提供服务。一个Service可以包含多个Connector，但是只能包含一个Engine；其中Connector的作用是从客户端接收请求，Engine的作用是处理接收进来的请求。

在第一部分的例子中，Server中包含一个名称为“Catalina”的Service。实际上，Tomcat可以提供多个Service，不同的Service监听不同的端口，后文会有介绍。

#### 2.2.3.Connector
Connector的主要功能，是接收连接请求，创建Request和Response对象用于和请求端交换数据；然后分配线程让Engine来处理这个请求，并把产生的Request和Response对象传给Engine。

通过配置Connector，可以控制请求Service的协议及端口号。在第一部分的例子中，Service包含两个Connector：

```
<Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
<Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
```

1. 通过配置第1个Connector，客户端可以通过8080端口号使用http协议访问Tomcat。其中，protocol属性规定了请求的协议，port规定了请求的端口号，redirectPort表示当强制要求https而请求是http时，重定向至端口号为8443的Connector，connectionTimeout表示连接的超时时间。

   在这个例子中，Tomcat监听HTTP请求，使用的是8080端口，而不是正式的80端口；实际上，在正式的生产环境中，Tomcat也常常监听8080端口，而不是80端口。这是因为在生产环境中，很少将Tomcat直接对外开放接收请求，而是在Tomcat和客户端之间加一层代理服务器(如nginx)，用于请求的转发、负载均衡、处理静态文件等；通过代理服务器访问Tomcat时，是在局域网中，因此一般仍使用8080端口。

2. 通过配置第2个Connector，客户端可以通过8009端口号使用AJP协议访问Tomcat。AJP协议负责和其他的HTTP服务器(如Apache)建立连接；在把Tomcat与其他HTTP服务器集成时，就需要用到这个连接器。之所以使用Tomcat和其他服务器集成，是因为Tomcat可以用作Servlet/JSP容器，但是对静态资源的处理速度较慢，不如Apache和IIS等HTTP服务器；因此常常将Tomcat与Apache等集成，前者作Servlet容器，后者处理静态资源，而AJP协议便负责Tomcat和Apache的连接。

   ![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506213745.png)

#### 2.2.4.Engine
Engine组件在Service组件中有且只有一个；Engine是Service组件中的请求处理组件。Engine组件从一个或多个Connector中接收请求并处理，并将完成的响应返回给Connector，最终传递给客户端。

前面已经提到过，Engine、Host和Context都是容器，但它们不是平行的关系，而是父子关系：Engine包含Host，Host包含Context。
在第一部分的例子中，Engine的配置语句如下：

```
<Engine name="Catalina" defaultHost="localhost">
```


其中，name属性用于日志和错误信息，在整个Server中应该唯一。defaultHost属性指定了默认的host名称，当发往本机的请求指定的host名称不存在时，一律使用defaultHost指定的host进行处理；因此，defaultHost的值，必须与Engine中的一个Host组件的name属性值匹配。

#### 2.2.5.Host
1. Engine与Host

   Host是Engine的子容器。Engine组件中可以内嵌1个或多个Host组件，每个Host组件代表Engine中的一个虚拟主机。Host组件至少有一个，且其中一个的name必须与Engine组件的defaultHost属性相匹配。

2. Host的作用

   Host虚拟主机的作用，是运行多个Web应用（一个Context代表一个Web应用），并负责安装、展开、启动和结束每个Web应用。

   Host组件代表的虚拟主机，对应了服务器中一个网络名实体(如”www.test.com”，或IP地址”116.25.25.25”)；为了使用户可以通过网络名连接Tomcat服务器，这个名字应该在DNS服务器上注册。

   客户端通常使用主机名来标识它们希望连接的服务器；该主机名也会包含在HTTP请求头中。Tomcat从HTTP头中提取出主机名，寻找名称匹配的主机。如果没有匹配，请求将发送至默认主机。因此默认主机不需要是在DNS服务器中注册的网络名，因为任何与所有Host名称不匹配的请求，都会路由至默认主机。

3. Host的配置

    在第一部分的例子中，Host的配置如下：

    ```
    <Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true">
    ```

    下面对其中配置的属性进行说明：

    name属性指定虚拟主机的主机名，一个Engine中有且仅有一个Host组件的name属性与Engine组件的defaultHost属性相匹配；一般情况下，主机名需要是在DNS服务器中注册的网络名，但是Engine指定的defaultHost不需要，原因在前面已经说明。

    unpackWARs指定了是否将代表Web应用的WAR文件解压；如果为true，通过解压后的文件结构运行该Web应用，如果为false，直接使用WAR文件运行Web应用。

    Host的autoDeploy和appBase属性，与Host内Web应用的自动部署有关；此外，本例中没有出现的xmlBase和deployOnStartup属性，也与Web应用的自动部署有关；将在下一节(Context)中介绍。

#### 2.2.6.Context
1. **Context的作用**

   Context元素代表在特定虚拟主机上运行的一个Web应用。在后文中，提到Context、应用或Web应用，它们指代的都是Web应用。每个Web应用基于WAR文件，或WAR文件解压后对应的目录（这里称为应用目录）。

   Context是Host的子容器，每个Host中可以定义任意多的Context元素。

   在第一部分的例子中，可以看到server.xml配置文件中并没有出现Context元素的配置。这是因为，Tomcat开启了自动部署，Web应用没有在server.xml中配置静态部署，而是由Tomcat通过特定的规则自动部署。下面介绍一下Tomcat自动部署Web应用的机制。

2. **Web应用自动部署**

   **Host的配置**

   要开启Web应用的自动部署，需要配置所在的虚拟主机；配置的方式就是前面提到的Host元素的deployOnStartup和autoDeploy属性。如果deployOnStartup和autoDeploy设置为true，则tomcat启动自动部署：当检测到新的Web应用或Web应用的更新时，会触发应用的部署(或重新部署)。二者的主要区别在于，deployOnStartup为true时，Tomcat在启动时检查Web应用，且检测到的所有Web应用视作新应用；autoDeploy为true时，Tomcat在运行时定期检查新的Web应用或Web应用的更新。除此之外，二者的处理相似。

   通过配置deployOnStartup和autoDeploy可以开启虚拟主机自动部署Web应用；实际上，自动部署依赖于检查是否有新的或更改过的Web应用，而Host元素的appBase和xmlBase设置了检查Web应用更新的目录。

   其中，appBase属性指定Web应用所在的目录，默认值是webapps，这是一个相对路径，代表Tomcat根目录下webapps文件夹。
     xmlBase属性指定Web应用的XML配置文件所在的目录，默认值为`conf/<engine_name>/<host_name>`，例如第一部分的例子中，主机localhost的xmlBase的默认值是 `$TOMCAT_HOME/conf/Catalina/localhost`。

   **检查Web应用更新**

   一个Web应用可能包括以下文件：XML配置文件，WAR包，以及一个应用目录(该目录包含Web应用的文件结构)；其中XML配置文件位于xmlBase指定的目录，WAR包和应用目录位于appBase指定的目录。

   Tomcat按照如下的顺序进行扫描，来检查应用更新：

   - 扫描虚拟主机指定的xmlBase下的XML配置文件
   - 扫描虚拟主机指定的appBase下的WAR文件
   - 扫描虚拟主机指定的appBase下的应用目录

   **`<Context>` 元素的配置**

   Context元素最重要的属性是docBase和path，此外reloadable属性也比较常用。

   docBase指定了该Web应用使用的WAR包路径，或应用目录。需要注意的是，在自动部署场景下，docBase不在appBase目录中，才需要指定；如果docBase指定的WAR包或应用目录就在appBase中，则不需要指定，因为Tomcat会自动扫描appBase中的WAR包和应用目录，指定了反而会造成问题。

   path指定了访问该Web应用的上下文路径，当请求到来时，Tomcat根据Web应用的 path属性与URI的匹配程度来选择Web应用处理相应请求。例如，Web应用app1的path属性是”/app1”，Web应用app2的path属性是”/app2”，那么请求/app1/index.html会交由app1来处理；而请求/app2/index.html会交由app2来处理。如果一个Context元素的path属性为””，那么这个Context是虚拟主机的默认Web应用；当请求的uri与所有的path都不匹配时，使用该默认Web应用来处理。

   但是，需要注意的是，在自动部署场景下，不能指定path属性，path属性由配置文件的文件名、WAR文件的文件名或应用目录的名称自动推导出来。如扫描Web应用时，发现了xmlBase目录下的app1.xml，或appBase目录下的app1.WAR或app1应用目录，则该Web应用的path属性是”app1”。如果名称不是app1而是ROOT，则该Web应用是虚拟主机默认的Web应用，此时path属性推导为””。

   reloadable属性指示tomcat是否在运行时监控在WEB-INF/classes和WEB-INF/lib目录下class文件的改动。如果值为true，那么当class文件改动时，会触发Web应用的重新加载。在开发环境下，reloadable设置为true便于调试；但是在生产环境中设置为true会给服务器带来性能压力，因此reloadable参数的默认值为false。

   下面来看自动部署时，xmlBase下的XML配置文件app1.xml的例子：

   ```
   <Context docBase="D:\Program Files\app1.war" reloadable="true"/>  
   ```

   在该例子中，docBase位于Host的appBase目录之外；path属性没有指定，而是根据app1.xml自动推导为”app1”；由于是在开发环境下，因此reloadable设置为true，便于开发调试。

    **自动部署举例**

   最典型的自动部署，就是当我们安装完Tomcat后，`$TOMCAT_HOME/webapps` 目录下有如下文件夹：

     ![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214001.png)

    当我们启动Tomcat后，可以使用 `http://localhost:8080/` 来访问Tomcat，其实访问的就是ROOT对应的Web应用；我们也可以通过 `http://localhost:8080/docs` 来访问docs应用，同理我们可以访问 `examples/host-manager/manager` 这几个Web应用。

3. **server.xml中静态部署Web应用**

   除了自动部署，我们也可以在server.xml中通过 `<context>` 元素静态部署Web应用。静态部署与自动部署是可以共存的。在实际应用中，并不推荐使用静态部署，因为server.xml 是不可动态重加载的资源，服务器一旦启动了以后，要修改这个文件，就得重启服务器才能重新加载。而自动部署可以在Tomcat运行时通过定期的扫描来实现，不需要重启服务器。

    `server.xml` 中使用Context元素配置Web应用，Context元素应该位于Host元素中。举例如下：

   ```
   <Context path="/" docBase="D:\Program Files \app1.war" reloadable="true"/>
   ```

   docBase：静态部署时，docBase可以在appBase目录下，也可以不在；本例中，docBase不在appBase目录下。

   path：静态部署时，可以显式指定path属性，但是仍然受到了严格的限制：只有当自动部署完全关闭(deployOnStartup和autoDeploy都为false)或docBase不在appBase中时，才可以设置path属性。在本例中，docBase不在appBase中，因此path属性可以设置。

   reloadable属性的用法与自动部署时相同。

### 2.3.核心组件的关联
#### 2.3.1.整体关系
核心组件之间的整体关系，在上一部分有所介绍，这里总结一下：

Server元素在最顶层，代表整个Tomcat容器；一个Server元素中可以有一个或多个Service元素。

Service在Connector和Engine外面包了一层，把它们组装在一起，对外提供服务。一个Service可以包含多个Connector，但是只能包含一个Engine；Connector接收请求，Engine处理请求。

Engine、Host和Context都是容器，且 Engine包含Host，Host包含Context。每个Host组件代表Engine中的一个虚拟主机；每个Context组件代表在特定Host上运行的一个Web应用。

#### 2.3.2.如何确定请求由谁处理？
当请求被发送到Tomcat所在的主机时，如何确定最终哪个Web应用来处理该请求呢？
1. 根据协议和端口号选定Service和Engine

   Service中的Connector组件可以接收特定端口的请求，因此，当Tomcat启动时，Service组件就会监听特定的端口。在第一部分的例子中，Catalina这个Service监听了8080端口（基于HTTP协议）和8009端口（基于AJP协议）。当请求进来时，Tomcat便可以根据协议和端口号选定处理请求的Service；Service一旦选定，Engine也就确定。

   通过在Server中配置多个Service，可以实现通过不同的端口号来访问同一台机器上部署的不同应用。

2. 根据域名或IP地址选定Host

   Service确定后，Tomcat在Service中寻找名称与域名/IP地址匹配的Host处理该请求。如果没有找到，则使用Engine中指定的defaultHost来处理该请求。在第一部分的例子中，由于只有一个Host（name属性为localhost），因此该Service/Engine的所有请求都交给该Host处理。

3. 根据URI选定Context/Web应用

   这一点在Context一节有详细的说明：Tomcat根据应用的 path属性与URI的匹配程度来选择Web应用处理相应请求，这里不再赘述。

4. 举例

   以请求http://localhost:8080/app1/index.html为例，首先通过协议和端口号（http和8080）选定Service；然后通过主机名（localhost）选定Host；然后通过uri（/app1/index.html）选定Web应用。

#### 2.3.3.如何配置多个服务
通过在Server中配置多个Service服务，可以实现通过不同的端口号来访问同一台机器上部署的不同Web应用。
在server.xml中配置多服务的方法非常简单，分为以下几步：
1. 复制 `<Service>` 元素，放在当前 `<Service>` 后面。

2. 修改端口号：根据需要监听的端口号修改 `<Connector>` 元素的port属性；必须确保该端口没有被其他进程占用，否则Tomcat启动时会报错，而无法通过该端口访问Web应用。

   以Win7为例，可以用如下方法找出某个端口是否被其他进程占用：`netstat -aon|findstr "8081"` 发现8081端口被PID为2064的进程占用，tasklist |findstr "2064"发现该进程为FrameworkService.exe(这是McAfee杀毒软件的进程)。

   ![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214056.png)

3. 修改Service和Engine的name属性

4. 修改Host的appBase属性（如webapps2）

5. Web应用仍然使用自动部署

6. 将要部署的Web应用(WAR包或应用目录)拷贝到新的appBase下。

   以第一部分的server.xml为例，多个Service的配置如下：

   ```
   <?xml version='1.0' encoding='utf-8'?>
   <Server port="8005" shutdown="SHUTDOWN">
     <Listener className="org.apache.catalina.startup.VersionLoggerListener" />
     <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
     <Listener className="org.apache.catalina.core.JasperListener" />
     <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
     <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
     <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
   
     <GlobalNamingResources>
       <Resource name="UserDatabase" auth="Container" type="org.apache.catalina.UserDatabase" description="User database that can be updated and saved" factory="org.apache.catalina.users.MemoryUserDatabaseFactory" pathname="conf/tomcat-users.xml" />
     </GlobalNamingResources>
   
     <Service name="Catalina">
       <Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
       <Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
       <Engine name="Catalina" defaultHost="localhost">
         <Realm className="org.apache.catalina.realm.LockOutRealm">
           <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
                  resourceName="UserDatabase"/>
         </Realm>
   
         <Host name="localhost"  appBase="/opt/project/webapps" unpackWARs="true" autoDeploy="true">
           <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs" prefix="localhost_access_log." suffix=".txt" pattern="%h %l %u %t &quot;%r&quot; %s %b" />
         </Host>
       </Engine>
     </Service>
   
     <Service name="Catalina2">
       <Connector port="8084" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
       <Connector port="8010" protocol="AJP/1.3" redirectPort="8443" />
       <Engine name="Catalina2" defaultHost="localhost">
         <Realm className="org.apache.catalina.realm.LockOutRealm">
           <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
                  resourceName="UserDatabase"/>
         </Realm>
   
         <Host name="localhost"  appBase="/opt/project/webapps2" unpackWARs="true" autoDeploy="true">
           <Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs" prefix="localhost_access_log." suffix=".txt" pattern="%h %l %u %t &quot;%r&quot; %s %b" />
         </Host>
       </Engine>
     </Service>
   </Server>
   ```

   再将原webapps下的docs目录拷贝到webapps2中，则通过如下两个接口都可以访问docs应用：

   http://localhost:8080/docs/

   http://localhost:8084/docs/

### 2.4.其他组件
#### 2.4.1.Listener
```
<Listener className="org.apache.catalina.startup.VersionLoggerListener" />
  <Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
  <Listener className="org.apache.catalina.core.JasperListener" />
  <Listener className="org.apache.catalina.core.JreMemoryLeakPreventionListener" />
  <Listener className="org.apache.catalina.mbeans.GlobalResourcesLifecycleListener" />
  <Listener className="org.apache.catalina.core.ThreadLocalLeakPreventionListener" />
```
Listener(即监听器)定义的组件，可以在特定事件发生时执行特定的操作；被监听的事件通常是Tomcat的启动和停止。

监听器可以在Server、Engine、Host或Context中，本例中的监听器都是在Server中。实际上，本例中定义的6个监听器，都只能存在于Server组件中。监听器不允许内嵌其他组件。

监听器需要配置的最重要的属性是className，该属性规定了监听器的具体实现类，该类必须实现了org.apache.catalina.LifecycleListener接口。

下面依次介绍例子中配置的监听器：

VersionLoggerListener：当Tomcat启动时，该监听器记录Tomcat、Java和操作系统的信息。该监听器必须是配置的第一个监听器。
AprLifecycleListener：Tomcat启动时，检查APR库，如果存在则加载。APR，即Apache Portable Runtime，是Apache可移植运行库，可以实现高可扩展性、高性能，以及与本地服务器技术更好的集成。

JasperListener：在Web应用启动之前初始化Jasper，Jasper是JSP引擎，把JVM不认识的JSP文件解析成java文件，然后编译成class文件供JVM使用。

JreMemoryLeakPreventionListener：与类加载器导致的内存泄露有关。

GlobalResourcesLifecycleListener：通过该监听器，初始化< GlobalNamingResources>标签中定义的全局JNDI资源；如果没有该监听器，任何全局资源都不能使用。< GlobalNamingResources>将在后文介绍。

ThreadLocalLeakPreventionListener：当Web应用因thread-local导致的内存泄露而要停止时，该监听器会触发线程池中线程的更新。当线程执行完任务被收回线程池时，活跃线程会一个一个的更新。只有当Web应用(即Context元素)的renewThreadsWhenStoppingContext属性设置为true时，该监听器才有效。

#### 2.4.2.GlobalNamingResources与Realm
第一部分的例子中，Engine组件下定义了Realm组件：
```
<Realm className="org.apache.catalina.realm.LockOutRealm">
  <Realm className="org.apache.catalina.realm.UserDatabaseRealm"
         resourceName="UserDatabase"/>
</Realm>
```
Realm，可以把它理解成“域”；Realm提供了一种用户密码与web应用的映射关系，从而达到角色安全管理的作用。在本例中，Realm的配置使用name为UserDatabase的资源实现。而该资源在Server元素中使用GlobalNamingResources配置：
```
<GlobalNamingResources>
  <Resource name="UserDatabase" auth="Container" type="org.apache.catalina.UserDatabase" description="User database that can be updated and saved" factory="org.apache.catalina.users.MemoryUserDatabaseFactory" pathname="conf/tomcat-users.xml" />
</GlobalNamingResources>
```
GlobalNamingResources元素定义了全局资源，通过配置可以看出，该配置是通过读取$TOMCAT_HOME/ conf/tomcat-users.xml实现的。
#### 2.4.3.Valve
在第一部分的例子中，Host元素内定义了Valve组件：
```
<Valve className="org.apache.catalina.valves.AccessLogValve" directory="logs" prefix="localhost_access_log." suffix=".txt" pattern="%h %l %u %t &quot;%r&quot; %s %b" />
```
单词Valve的意思是“阀门”，在Tomcat中代表了请求处理流水线上的一个组件；Valve可以与Tomcat的容器(Engine、Host或Context)关联。

不同的Valve有不同的特性，下面介绍一下本例中出现的AccessLogValve。

AccessLogValve的作用是通过日志记录其所在的容器中处理的所有请求，在本例中，Valve放在Host下，便可以记录该Host处理的所有请求。AccessLogValve记录的日志就是访问日志，每天的请求会写到一个日志文件里。AccessLogValve可以与Engine、Host或Context关联；在本例中，只有一个Engine，Engine下只有一个Host，Host下只有一个Context，因此AccessLogValve放在三个容器下的作用其实是类似的。

本例的AccessLogValve属性的配置，使用的是默认的配置；下面介绍AccessLogValve中各个属性的作用：

1. className：规定了Valve的类型，是最重要的属性；本例中，通过该属性规定了这是一个AccessLogValve。

2. directory：指定日志存储的位置，本例中，日志存储在$TOMCAT_HOME/logs目录下。

3. prefix：指定了日志文件的前缀。

4. suffix：指定了日志文件的后缀。通过directory、prefix和suffix的配置，在$TOMCAT_HOME/logs目录下，可以看到如下所示的日志文件。

   ![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214142.png)

5. pattern：指定记录日志的格式，本例中各项的含义如下：

    - %h：远程主机名或IP地址；如果有nginx等反向代理服务器进行请求分发，该主机名/IP地址代表的是nginx，否则代表的是客户端。后面远程的含义与之类似，不再解释。

    - %l：远程逻辑用户名，一律是”-”，可以忽略。

    - %u：授权的远程用户名，如果没有，则是”-”。

    - %t：访问的时间。

    - %r：请求的第一行，即请求方法(get/post等)、uri、及协议。

    - %s：响应状态，200,404等等。

    - %b：响应的数据量，不包括请求头，如果为0，则是””-。

      例如，下面是访问日志中的一条记录

      ![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214155.png)

      pattern的配置中，除了上述各项，还有一个非常常用的选项是%D，含义是请求处理的时间(单位是毫秒)，对于统计分析请求的处理速度帮助很大。

      开发人员可以充分利用访问日志，来分析问题、优化应用。例如，分析访问日志中各个接口被访问的比例，不仅可以为需求和运营人员提供数据支持，还可以使自己的优化有的放矢；分析访问日志中各个请求的响应状态码，可以知道服务器请求的成功率，并找出有问题的请求；分析访问日志中各个请求的响应时间，可以找出慢请求，并根据需要进行响应时间的优化。

## 3.详解tomcat的连接数与线程池
### 3.1.前言
在使用tomcat时，经常会遇到连接数、线程数之类的配置问题，要真正理解这些概念，必须先了解Tomcat的连接器（Connector）。
在前面的文章 详解Tomcat配置文件 `server.xml` 中写到过：Connector的主要功能，是接收连接请求，创建Request和Response对象用于和请求端交换数据；然后分配线程让Engine（也就是Servlet容器）来处理这个请求，并把产生的Request和Response对象传给Engine。当Engine处理完请求后，也会通过Connector将响应返回给客户端。

可以说，Servlet容器处理请求，是需要Connector进行调度和控制的，Connector是Tomcat处理请求的主干，因此Connector的配置和使用对Tomcat的性能有着重要的影响。这篇文章将从Connector入手，讨论一些与Connector有关的重要问题，包括NIO/BIO模式、线程池、连接数等。

根据协议的不同，Connector可以分为HTTP Connector、AJP Connector等，本文只讨论HTTP Connector。

### 3.2.Nio、Bio、APR
#### 3.2.1.Connector的protocol
Connector在处理HTTP请求时，会使用不同的protocol。不同的Tomcat版本支持的protocol不同，其中最典型的protocol包括BIO、NIO和APR（Tomcat7中支持这3种，Tomcat8增加了对NIO2的支持，而到了Tomcat8.5和Tomcat9.0，则去掉了对BIO的支持）。

BIO是Blocking IO，顾名思义是阻塞的IO；NIO是Non-blocking IO，则是非阻塞的IO。而APR是Apache Portable Runtime，是Apache可移植运行库，利用本地库可以实现高可扩展性、高性能；Apr是在Tomcat上运行高并发应用的首选模式，但是需要安装apr、apr-utils、tomcat-native等包。

#### 3.2.2.如何指定protocol
Connector使用哪种protocol，可以通过 `<connector>` 元素中的protocol属性进行指定，也可以使用默认值。指定的protocol取值及对应的协议如下：
- `HTTP/1.1`：默认值，使用的协议与Tomcat版本有关
- `org.apache.coyote.http11.Http11Protocol`：BIO
- `org.apache.coyote.http11.Http11NioProtocol`：NIO
- `org.apache.coyote.http11.Http11Nio2Protocol`：NIO2
- `org.apache.coyote.http11.Http11AprProtocol`：APR

如果没有指定protocol，则使用默认值HTTP/1.1，其含义如下：在Tomcat7中，自动选取使用BIO或APR（如果找到APR需要的本地库，则使用APR，否则使用BIO）；在Tomcat8中，自动选取使用NIO或APR（如果找到APR需要的本地库，则使用APR，否则使用NIO）。
#### 3.2.3.BIO/NIO有何不同
无论是BIO，还是NIO，Connector处理请求的大致流程是一样的：

在accept队列中接收连接（当客户端向服务器发送请求时，如果客户端与OS完成三次握手建立了连接，则OS将该连接放入accept队列）；在连接中获取请求的数据，生成request；调用servlet容器处理请求；返回response。为了便于后面的说明，首先明确一下连接与请求的关系：连接是TCP层面的（传输层），对应socket；请求是HTTP层面的（应用层），必须依赖于TCP的连接实现；一个TCP连接中可能传输多个HTTP请求。

在BIO实现的Connector中，处理请求的主要实体是JIoEndpoint对象。JIoEndpoint维护了Acceptor和Worker：Acceptor接收socket，然后从Worker线程池中找出空闲的线程处理socket，如果worker线程池没有空闲线程，则Acceptor将阻塞。其中Worker是Tomcat自带的线程池，如果通过 `<Executor>` 配置了其他线程池，原理与Worker类似。

在NIO实现的Connector中，处理请求的主要实体是NIoEndpoint对象。NIoEndpoint中除了包含Acceptor和Worker外，还使用了Poller，处理流程如下图所示

![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214213.png)

Acceptor接收socket后，不是直接使用Worker中的线程处理请求，而是先将请求发送给了Poller，而Poller是实现NIO的关键。Acceptor向Poller发送请求通过队列实现，使用了典型的生产者-消费者模式。在Poller中，维护了一个Selector对象；当Poller从队列中取出socket后，注册到该Selector中；然后通过遍历Selector，找出其中可读的socket，并使用Worker中的线程处理相应请求。与BIO类似，Worker也可以被自定义的线程池代替。

通过上述过程可以看出，在NIoEndpoint处理请求的过程中，无论是Acceptor接收socket，还是线程处理请求，使用的仍然是阻塞方式；但在“读取socket并交给Worker中的线程”的这个过程中，使用非阻塞的NIO实现，这是NIO模式与BIO模式的最主要区别（其他区别对性能影响较小，暂时略去不提）。而这个区别，在并发量较大的情形下可以带来Tomcat效率的显著提升：

目前大多数HTTP请求使用的是长连接（HTTP/1.1默认keep-alive为true），而长连接意味着，一个TCP的socket在当前请求结束后，如果没有新的请求到来，socket不会立马释放，而是等timeout后再释放。如果使用BIO，“读取socket并交给Worker中的线程”这个过程是阻塞的，也就意味着在socket等待下一个请求或等待释放的过程中，处理这个socket的工作线程会一直被占用，无法释放；因此Tomcat可以同时处理的socket数目不能超过最大线程数，性能受到了极大限制。而使用NIO，“读取socket并交给Worker中的线程”这个过程是非阻塞的，当socket在等待下一个请求或等待释放时，并不会占用工作线程，因此Tomcat可以同时处理的socket数目远大于最大线程数，并发性能大大提高。

### 3.3.  3个参数：acceptCount、maxConnections、maxThreads
再回顾一下Tomcat处理请求的过程：在accept队列中接收连接（当客户端向服务器发送请求时，如果客户端与OS完成三次握手建立了连接，则OS将该连接放入accept队列）；在连接中获取请求的数据，生成request；调用servlet容器处理请求；返回response。
相对应的，Connector中的几个参数功能如下：
#### 3.3.1.acceptCount
accept队列的长度；当accept队列中连接的个数达到acceptCount时，队列满，进来的请求一律被拒绝。默认值是100。
#### 3.3.2.maxConnections
Tomcat在任意时刻接收和处理的最大连接数。当Tomcat接收的连接数达到maxConnections时，Acceptor线程不会读取accept队列中的连接；这时accept队列中的线程会一直阻塞着，直到Tomcat接收的连接数小于maxConnections。如果设置为-1，则连接数不受限制。
默认值与连接器使用的协议有关：NIO的默认值是10000，APR/native的默认值是8192，而BIO的默认值为maxThreads（如果配置了Executor，则默认值是Executor的maxThreads）。

在windows下，APR/native的maxConnections值会自动调整为设置值以下最大的1024的整数倍；如设置为2000，则最大值实际是1024。

#### 3.3.3.maxThreads
请求处理线程的最大数量。默认值是200（Tomcat7和8都是的）。如果该Connector绑定了Executor，这个值会被忽略，因为该Connector将使用绑定的Executor，而不是内置的线程池来执行任务。

maxThreads规定的是最大的线程数目，并不是实际running的CPU数量；实际上，maxThreads的大小比CPU核心数量要大得多。这是因为，处理请求的线程真正用于计算的时间可能很少，大多数时间可能在阻塞，如等待数据库返回数据、等待硬盘读写数据等。因此，在某一时刻，只有少数的线程真正的在使用物理CPU，大多数线程都在等待；因此线程数远大于物理核心数才是合理的。

换句话说，Tomcat通过使用比CPU核心数量多得多的线程数，可以使CPU忙碌起来，大大提高CPU的利用率。

#### 3.3.4.参数设置
1. maxThreads的设置既与应用的特点有关，也与服务器的CPU核心数量有关。通过前面介绍可以知道，maxThreads数量应该远大于CPU核心数量；而且CPU核心数越大，maxThreads应该越大；应用中CPU越不密集（IO越密集），maxThreads应该越大，以便能够充分利用CPU。当然，maxThreads的值并不是越大越好，如果maxThreads过大，那么CPU会花费大量的时间用于线程的切换，整体效率会降低。
2. maxConnections的设置与Tomcat的运行模式有关。如果tomcat使用的是BIO，那么maxConnections的值应该与maxThreads一致；如果tomcat使用的是NIO，maxConnections值应该远大于maxThreads。
3. 通过前面的介绍可以知道，虽然tomcat同时可以处理的连接数目是maxConnections，但服务器中可以同时接收的连接数为maxConnections+acceptCount 。acceptCount的设置，与应用在连接过高情况下希望做出什么反应有关系。如果设置过大，后面进入的请求等待时间会很长；如果设置过小，后面进入的请求立马返回connection refused。
### 3.4.线程池Executor
Executor元素代表Tomcat中的线程池，可以由其他组件共享使用；要使用该线程池，组件需要通过executor属性指定该线程池。
```
<Executor name="tomcatThreadPool" namePrefix ="catalina-exec-" maxThreads="150" minSpareThreads="4" />
<Connector executor="tomcatThreadPool" port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" acceptCount="1000" />\
```
Executor的主要属性包括：
- name：该线程池的标记
- maxThreads：线程池中最大活跃线程数，默认值200（Tomcat7和8都是）
- minSpareThreads：线程池中保持的最小线程数，最小值是25
- maxIdleTime：线程空闲的最大时间，当空闲超过该值时关闭线程（除非线程数小于minSpareThreads），单位是ms，默认值60000（1分钟）
- daemon：是否后台线程，默认值true
- threadPriority：线程优先级，默认值5
- namePrefix：线程名字的前缀，线程池中线程名字为：namePrefix+线程编号
### 3.5.查看当前状态
#### 3.5.1.连接数
假设Tomcat接收http请求的端口是8083，则可以使用如下语句查看连接情况：
```
netstat –nat | grep 8083
```
结果如下所示：

![Image [19]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214224.png)

可以看出，有一个连接处于listen状态，监听请求；除此之外，还有4个已经建立的连接（ESTABLISHED）和2个等待关闭的连接（CLOSE_WAIT）。

#### 3.5.2.线程
ps命令可以查看进程状态，如执行如下命令：
```
ps –e | grep java
```
结果如下图：

![Image [20]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214233.png)

可以看到，只打印了一个进程的信息；27989是进程id，java是指执行的java命令。这是因为启动一个tomcat，内部所有的工作都在这一个进程里完成，包括主线程、垃圾回收线程、Acceptor线程、请求处理线程等等。

通过如下命令，可以看到该进程内有多少个线程；其中， `nlwp` 含义是 `number of light-weight process`。

```
ps –o nlwp 27989
```
![Image [21]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214239.png)

可以看到，该进程内部有73个线程；但是73并没有排除处于idle状态的线程。要想获得真正在running的线程数量，可以通过以下语句完成：

```
ps -eLo pid ,stat | grep 27989 | grep running | wc -l
```
其中 `ps -eLo pid ,stat` 可以找出所有线程，并打印其所在的进程号和线程当前的状态；两个grep命令分别筛选进程号和线程状态；wc统计个数。其中，`ps -eLo pid ,stat | grep 27989` 输出的结果如下：

![Image [22]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214246.png)

图中只截图了部分结果；Sl表示大多数线程都处于空闲状态。

## 4.调优
### 4.1.tomcat 启动慢
#### 4.1.1.tomcat 获取随机值阻塞
tomcat的启动需要产生 `session id`，这个产生需要通过 `java.security.SecureRandom` 生成随机数来实现，随机数算法使用的是”SHA1PRNG”，但这个算法依赖于操作系统的提供的随机数据，在linux系统中，这个值又依赖于 `/dev/random` 和 `/dev/urandom`
```
/dev/random :阻塞型，读取它就会产生随机数据，但该数据取决于熵池噪声，当熵池空了，对/dev/random 的读操作也将会被阻塞。
/dev/urandom: 非阻塞的随机数产生器，它会重复使用熵池中的数据以产生伪随机数据。这表示对/dev/urandom的读取操作不会产生阻塞，但其输出的熵可能小于/dev/random的。它可以作为生成较低强度密码的伪随机数生成器，不建议用于生成高强度长期密码。
```
总结 tomcat 启动慢的原因是随机数产生遭到阻塞，遭到阻塞的原因是 熵池大小 。

解决方法：
1. **更换产生随机数的源**

    因为 `/dev/urandom` 是非阻塞的随机数产生器，所以我们可以从这边获取，但是生产的随机数的随机性比较低。我们可以在 我们的tomcat启动脚本(catalina.sh)里面添加

    ```
    JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
    ```

    或者是更改java的 `java.security` 文件，将 `securerandom.source=file:/dev/random`

    ```
    securerandom.source=file:/dev/./urandom
    ```

    注意一下，为什么我们这里使用的路径是 `/dev/./urandom` ,而不是  `/dev/urandom` ,是因为在java 8之前的版本设置了 `/dev/urandom` ，但是实际还是使用 `/dev/random`，设置为 `/dev/./urandom` 才能正常使用 `/dev/urandom` ， 这个bug在java8版本已经修复了，如果你是java7版本的话，需要按照上面设置，java8的话可以不用加 `./`。

2. **增大熵池 的值**

    要增大熵池 的值首先得你的cpu支持DRNG 特性， 如何查看我们的服务器的是否支持DRNG特性？

    ```
    cat /proc/cpuinfo | grep rdrand
    ```

    如果不支持的话，那么就只能通过上面的第一种方法来解决了

    **安装rngd服务**

    ```
    yum -y install rng-tools
    systemctl enable   rngd
    systemctl start  rngd
    ```

    然后我们进行查看我们的熵池 的值,会发现变大了

    ```
    cat /proc/sys/kernel/random/entropy_avail
    ```

    然后我们启动tomcat 会发现启动速度快很多。

#### 4.1.2.tomcat 需要部署的web应用程序太多
有的时候，我们tomcat启动比较慢是因为它需要部署的web应用程序太多，但是其中有些应用程序是我们不需要的，比如在webapps下的 doc 、example、ROOT 等等，我们可以将我们不需要的webapps删除，然后再进行发布，这些不需要的web，不仅会占用我们的资源，还有可能是入侵者的入侵对象。如果我们想并行启动多个web应用程序，我们可以Host 的属性 startStopThreads 值设置大于1 ，但这也取决于我们的服务器是不是多核的。如果是多核的建议调大 startStopThreads 的值，但不超过内核数。
#### 4.1.3.tomcat启动内存不足
 如果是项目比较大的话，我们使用默认的参数去启动的tomcat是很有可能内存不足的，我们需要设置JVM，将内存调整，JVM 的最大值和最小值建议是不要相差太大(最好一致.)
在启动脚本catalina.sh加上：
```
JAVA_OPTS='-server -Xms1024m -Xmx1024m'
```
### 4.2.Connector 调优
#### 4.2.1.使用arp 连接器
tomcat 可以使用Apache Portable Runtime来提供更高的性能服务。Apache Portable Runtime是一个高度可移植的库，是Apache HTTP Server 2.x的核心。APR有许多用途，包含高级的io功能(sendfile,epoll,Openssl),系统级别功能(产生随机数，系统状态)和进程处理(共享内存，NT管道，unix套接字)，它可以让tomcat成为通用的web服务器，让java应用作为一个完整的web服务器更加可行，而不是仅仅作为后端的技术。

总结：从系统级别来解决异步io的问题，提升性能。

apr 连接器需要自己手动安装，需要以下组件

- APR library (需要手动下载安装) tomcat 8.5 需要 APR 1.2+
- OpenSSL libraries (需要安装)
- JNI wrappers for APR used by Tomcat (libtcnative) （tomcat 安装包已经提供）
#### 4.2.2.Connector 其它属性调优
|         属性         |                             描述                             | 建议设置的值 |
| :------------------: | :----------------------------------------------------------: | :----------: |
|      maxThreads      |       tomcat能创建来处理请求的最大线程数，默认值为200        |     500      |
|    minProcessors     |               启动时创建的线程数（最小线程数）               |              |
|     acceptCount      | 指定当所有可以使用的处理请求的线程数都被使用时，可以放到队列中的请求数，，超过这个数的请求将拒绝连接 默认值为100 |     500      |
| compressibleMimeType | 该值用来指定哪些文件类型的文件可以进行压缩，默认值为：text/html,text/xml,text/plain,text/css,text/javascript,application/javascript |              |
|     compression      | 开启gzip 压缩，可以接受的值是 "off"(禁用压缩),"on"(开启压缩),"force(强制压缩)"，"1-9"(等效于开启压缩，并且设定压缩等级),开启了压缩，也就意味着要占用更多的cpu资源 |      on      |
|   keepAliveTimeout   | 指connector两个HTTP请求直接的等待时间，超过该时间没有接收到第二个HTTP请求就关闭连接，默认是使用connectionTimeout 的值，单位为毫秒 |    30000     |
|    processorCache    | 进程缓冲器，默认值是maxThreads的值,使用好该值可以提升并发请求。 |     500      |
#### 4.2.3.Host 属性调优
|       属性       |                             描述                             | 建议设置的值 |
| :--------------: | :----------------------------------------------------------: | :----------: |
| startStopThreads | 指Host用于启动Context的线程数，默认值为1，如果多核建议配置为核心数-1 |      2       |
|    unpackWARs    | 默认为true,如果设置为true 表示将web应用程序war包解压，false表示直接从war文件运行。设置为false对性能有一定的影响 |     true     |
如果我们想并行启动多个web应用程序，我们可以Host 的属性 startStopThreads 值设置大于1 ，但这也取决于我们的服务器是不是多核的。如果是多核的建议调大 startStopThreads 的值，但不超过内核数。
#### 4.2.4.tomcat线程关闭不掉调优(代码层)
如果我们使用的是自动发布的形式，也就是替换war包的形式，在tomcat安装文章中介绍了与jenkins 集成实现的，那么容易出现的一个问题就是 上一次的线程没有关闭掉，就启动了新的版本，那么上一个版本的线程还存在，也还在占用着资源，这个问题的原因有可能是代码的问题，我们可以尝试直接使用 catalina.sh 脚本stop ，你会发现stop 掉后，该tomcat 的线程还是存在的，那么出现这种情况很大一部分原因就是 在java代码中有非守护线程，也就是java代码未将线程设置为守护线程，导致了tomcat 进行stop 不掉该线程的原因。那么这种情况有可能会导致两个web应用(新老版本)都在使用，那这这个应用的定时任务可能就会执行两次，就容易导致生产事故。

这种问题，一方面会占用服务器资源。另外一方面还会容易导致生产事故，我们可以用jstack 分析下未停止的线程，并和开发区解决这个线程关闭不掉的问题。

#### 4.2.5.AJP 连接器禁用
AJP协议在tomcat中的作用就是将该服务与其它HTTP服务器集成，我们一般项目中，没有用到该连接器，所以我们可以禁用该连接器
### 4.3.JVM 设置









## X.问题
### X.1.Tomcat控制台输出中文乱码解决方法
#### X.1.1.Tomcat 7
对于Tomcat 7及以前的版本,使用的编码格式是：iso8859-1,所以不能显示中文.Tomcat 8及以后版本默认编码为UTF-8.对于Tomcat 7作出以下修改,使得Tomcat 7在处理get请求时使用UTF-8编码:

找到Tomcat的解压目录,例如: `D:\apache-tomcat-8.5.41\conf` ,打开其中的server.xml,找到如下代码:

![Image [23]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214258.png)

加上一句:`URLEncoding="UTF-8"`,保存退出

![Image [24]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214304.png)

而对于post请求,在代码中设置:request.setCharacterEncoding("UTF-8"),将编码设置为UTF-8

#### X.1.2.Tomcat 8
对于Tomcat 8的控制台乱码,如图所示:

![Image [25]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210506214308.png)

修改 `D:\apache-tomcat-8.5.41\conf\logging.properties` ,添加语句:

```
java.util.logging.ConsoleHandler.encoding = GBK
```
重启Tomcat即可.如图所示:






### X.2.解决catalina.out文件过大的问题
#### X.2.1.前言
有用Tomcat的人，绝对都会遇到这样一个问题：`catalina.out` 文件过大。

它是Tomcat默认生成的日志文件，会随着时间的推移，逐渐增大，不断的增长，甚至达到几G，几十G的大小。由于文件过大，不仅占系统的存储，我们还将无法使用过常规的编辑工具进行查看，严重影响系统的维护工作。

对此，出现了以下几种解决 `catalina.out` 文件过大的方案。

#### X.2.2.简洁型
##### X.2.2.1.手动版
每次监控到tomcat的硬盘空间变小达到阈值，手动登陆服务器，切换到tomcat的logs下，手动清空
```
echo " "  > catalina.out
```
##### X.2.2.2.脚本版
```
crontab -e 
0 24 * * *    sh /root/qin_catalina.out.sh

vim qin_catalina.out.sh
 #!/usr/bin/bash 
 echo " " > catalina.out
```
#### X.2.3.技术型
##### X.2.3.1.cronolog
使用cronolog日志切分工具切分Tomcat的 `catalina.out` 日志文件
1. 下载cronolog，并进行安装

    ```
    wget http://cronolog.org/download/cronolog-1.6.2.tar.gz   (中国服务器可能无法下载或下载缓慢，可先下载到境外服务器上)

    tar zxvf cronolog-1.6.2.tar.gz

    ./cronolog-1.6.2/configure

    make

    make install

    (默认安装在/usr/local/sbin下)
    ```

2. 配置

    在 `tomcat/bin/catalian.sh` 中

    ```
    org.apache.catalina.startup.Bootstrap "$@" start \ >> "$CATALINA_BASE"/logs/catalina.out 2&1 &

    改成:
    org.apache.catalina.startup.Bootstrap"$@" start \ |/usr/local/sbin/cronolog "$CATALINA_BASE"/logs/catalina.%Y-%m-%d.out >> /dev/null 2>&1 &
    或
    org.apache.catalina.startup.Bootstrap   "$@"  start  2>&1  \   |  /usr/local/sbin/cronolog "$CATALINA_BASE"/logs/catalina.%Y-%m-%d.out >> /dev/null & 

    并注释    touch "$CATALINA_OUT"  
    ```

3. 重启Tomcat

	Tomcat输出日志文件分割成功，输出log文件格式变为：catalina.2017-05-15.out

##### X.2.3.2.logrotate
CentOS6.5后自带logrotate程序，可以解决catalina.out的日志轮转问题
1. 在/etc/logrotate.d/目录下新建一个tomcat的文件

    ```
    cat >/etc/logrotate.d/tomcat 

    /usr/local/tomcat/logs/catalina.out{            要轮转的文件
        copytruncate                                创建新的catalina.out副本，截断源catalina.out文件
        daily                                       每天进行catalina.out文件的轮转
        rotate 7                                    至多保留7个副本
        missingok                                   文件丢失了，轮转不报错
        compress                                    使用压缩
        size 16M                                    当catalina.out文件大于16MB，就轮转
    }
    ```

2. 当执行以上操作时是自动执行的，也可手动切割

    ```
    logrotate /etc/logrotate.conf
    ```
    如果只轮转tomcat配置文件，要指定文件
    ```
    logrotate --force /etc/logrotate.d/tomcat
    ```

3. 删除要清理的日志

    手工查找需要清理的日志文件

    ```
    cd /usr/local/tomcat/logs
    rm -rf catalina.out.4.gz
    ```

##### X.2.3.3.日志切割脚本版
使用cron每天来定时备份当前的catalina.out，然后清空他的内容；
1. crontab -e

    ```
    30 * * * *  sh /root/qie_catalina.out.sh
    ```

2. cat qie_catalina.out.sh 参考脚本

    ```
     #!/bin/bash 
     y=`date "+%Y"`
     m=`date "+%m"`
     d=`date "+%d"`

    cp /etc/tomcat/logs/catalina.out  /etc/tomcat/logs/`catalina.out.$y_$m_$d`

    echo " " > catalina.out
    ```

### X.3.Tomcat 8005/8009/8080/8443端口的作用

- 8005--关闭tomcat进程所用。当执行shutdown.sh关闭tomcat时就是连接8005端口执行“SHUTDOWN”命令--由此，我们直接telnet8005端口执行“SHUTDOWN”（要大写，小写没用；不运只能telnet 127.0.0.1 8005其他地址telnet都不能连接）也可以成功关闭tomcat.

  同时反之如果8005端口未监听那么tomcat无法用shutdown.sh关闭。

- 8009--httpd等反向代理tomcat时就可以使用使用ajp协议反向代理到该端口。虽然我们经常都是使用http反向代理到8080端口，但由于ajp建立tcp连接后一般长时间保持，从而减少了http反复进行tcp连接和断开的开销，所以反向代理中ajp是比http高效的。

- 8080--默认的http监听端口。

- 8443--默认的https监听端口。默认未开启，如果要开启由于tomcat不自带证书所以除了取消注释之外，还要自己生成证书并在`<Connector>` 中指定方可。