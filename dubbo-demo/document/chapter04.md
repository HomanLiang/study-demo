[toc]



# Dubbo 服务注册

## 前言
前面有说到Dubbo的服务发现机制，也就是SPI，那既然Dubbo内部实现了更加强大的服务发现机制，现在我们就来一起看看Dubbo在发现服务后需要做什么才能将服务注册到注册中心中。
## Dubbo服务注册简介
首先需要明白的是Dubbo是依赖于Spring容器的（至于为什么在上篇博客中有介绍），Dubbo服务注册过程也是始于Spring容器发布刷新事件。而后Dubbo在接收到事件后，就会进行服务注册，整个逻辑大致分为三个部分：
1. 检查参数，组装URL：服务消费方是通过URL拿到服务提供者的，所以我们需要为服务提供者配置好对应的URL。
2. 导出服务到本地和远程：这里的本地指的是JVM，远程指的是实现invoke，使得服务消费方能够通过invoke调用到服务。
3. 向注册中心注册服务：能够让服务消费方知道服务提供方提供了那个服务。
## 接收Spring容器刷新事件
在简介中我们提到Dubbo服务注册是始于Spring容器发布刷新事件，那么Dubbo是如何接收该事件的呢？

在我们平常编写provider的接口实现类时，都会打上@Service注解，从而这个标注这个类属于ServiceBean。在ServiceBean中有这样一个方法onApplicationEvent。该方法会在收到 Spring 上下文刷新事件后执行服务注册操作
```
public void onApplicationEvent(ContextRefreshedEvent event) {
        //是否已导出 && 是不是已被取消导出
        if (!this.isExported() && !this.isUnexported()) {
            if (logger.isInfoEnabled()) {
                logger.info("The service ready on spring started. service: " + this.getInterface());
            }

            this.export();
        }

    }
```
注意这里是2.7.3的Dubbo，接收Spring上下文刷新事件已经不需要设置延迟导出，而是在导出的时候检查配置再决定是否需要延时，所以只有两个判断。而在2.6.x版本的Dubbo存在着isDelay的判断。这个是判断服务是否延时导出。这里说个题外话2.6.x的版本是com.alibaba.dubbo的，而2.7.x是org.apache.dubbo的，而2.7.0也开始代表dubbo从Apache里毕业了。

在这里就是Dubbo服务导出到注册中心过程的起点。需要我们在服务接口实现类上打上@Service。ServiceBean是Dubbo与Spring 框架进行整合的关键，可以看做是两个框架之间的桥梁。具有同样作用的类还有ReferenceBean。
## 检查配置参数以及URL装配
### 检查配置
在这一阶段Dubbo需要检查用户的配置是否合理，或者为用户补充缺省配置。就是从刷新事件开始，进入export()方法，源码解析如下：
```
public void export() {
        super.export();
        this.publishExportEvent();
    }

//进入到ServiceConfig.class中的export。

public synchronized void export() {
        //检查并且更新配置
        this.checkAndUpdateSubConfigs();
        //是否需要导出
        if (this.shouldExport()) {
            //是否需要延时
            if (this.shouldDelay()) {
                DELAY_EXPORT_EXECUTOR.schedule(this::doExport, (long)this.getDelay(), TimeUnit.MILLISECONDS);
            } else {
                //立刻导出
                this.doExport();
            }

        }
    }

//进入checkAndUpdateSubConfigs。

public void checkAndUpdateSubConfigs() {
        //检查配置项包括provider是否存在，导出端口是否可用，注册中心是否可以连接等等
        this.completeCompoundConfigs();
        this.startConfigCenter();
        this.checkDefault();
        this.checkProtocol();
        this.checkApplication();
        if (!this.isOnlyInJvm()) {
            this.checkRegistry();
        }
        //检查接口内部方法是否不为空
        this.refresh();
        this.checkMetadataReport();
        if (StringUtils.isEmpty(this.interfaceName)) {
            throw new IllegalStateException("<dubbo:service interface=\"\" /> interface not allow null!");
        } else {
            if (this.ref instanceof GenericService) {
                this.interfaceClass = GenericService.class;
                if (StringUtils.isEmpty(this.generic)) {
                    this.generic = Boolean.TRUE.toString();
                }
            } else {
                try {
                    this.interfaceClass = Class.forName(this.interfaceName, true, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException var5) {
                    throw new IllegalStateException(var5.getMessage(), var5);
                }

                this.checkInterfaceAndMethods(this.interfaceClass, this.methods);
                this.checkRef();
                this.generic = Boolean.FALSE.toString();
            }
            //是否需要导出服务或者只是在本地运行测试
            Class stubClass;
            if (this.local != null) {
                if ("true".equals(this.local)) {
                    this.local = this.interfaceName + "Local";
                }

                try {
                    stubClass = ClassUtils.forNameWithThreadContextClassLoader(this.local);
                } catch (ClassNotFoundException var4) {
                    throw new IllegalStateException(var4.getMessage(), var4);
                }

                if (!this.interfaceClass.isAssignableFrom(stubClass)) {
                    throw new IllegalStateException("The local implementation class " + stubClass.getName() + " not implement interface " + this.interfaceName);
                }
            }

            if (this.stub != null) {
                if ("true".equals(this.stub)) {
                    this.stub = this.interfaceName + "Stub";
                }

                try {
                    stubClass = ClassUtils.forNameWithThreadContextClassLoader(this.stub);
                } catch (ClassNotFoundException var3) {
                    throw new IllegalStateException(var3.getMessage(), var3);
                }

                if (!this.interfaceClass.isAssignableFrom(stubClass)) {
                    throw new IllegalStateException("The stub implementation class " + stubClass.getName() + " not implement interface " + this.interfaceName);
                }
            }

            this.checkStubAndLocal(this.interfaceClass);
            this.checkMock(this.interfaceClass);
        }
    }
```
上面的源码分析可看出。export方法主要检查的配置项有@Service标签的类是否属性合法。服务提供者是否存在，是否有对应的Application启动，端口是否能连接，是否有对应的注册中心等等一些配置，在检查完这些配置后Dubbo会识别我们此次启动服务是想在本地启动进行一些调试，还是将服务暴露给别人。不想暴露出去可以进行配置
```
<dubbo:provider export="false" />
```
### URL装配
在Dubbo中的URL一般包括以下字段：protocol，host，port，path,parameters。在检查配置后会进入到doExport中。
protocol：就是URL最前面的字段，表示的是协议，一般是：dubbo thrift http zk
host.port：就是对应的IP地址和端口
path：接口名称
parameters：参数键值对
```
protected synchronized void doExport() {
        if (this.unexported) {
            throw new IllegalStateException("The service " + this.interfaceClass.getName() + " has already unexported!");
        } else if (!this.exported) {
            this.exported = true;
            if (StringUtils.isEmpty(this.path)) {
                this.path = this.interfaceName;
            }

            this.doExportUrls();
        }
    }

//进入到doExportUrls
private void doExportUrls() {
        //加载注册中心链接
        List<URL> registryURLs = this.loadRegistries(true);
        //使用遍历器遍历protocols，并在每个协议下导出服务
        Iterator var2 = this.protocols.iterator();

        while(var2.hasNext()) {
            ProtocolConfig protocolConfig = (ProtocolConfig)var2.next();
            String pathKey = URL.buildKey((String)this.getContextPath(protocolConfig).map((p) -> {
                return p + "/" + this.path;
            }).orElse(this.path), this.group, this.version);
            ProviderModel providerModel = new ProviderModel(pathKey, this.ref, this.interfaceClass);
            ApplicationModel.initProviderModel(pathKey, providerModel);
            this.doExportUrlsFor1Protocol(protocolConfig, registryURLs);
        }

    }

//进入到加载注册中心链接的方法

protected List<URL> loadRegistries(boolean provider) {
        List<URL> registryList = new ArrayList();
        if (CollectionUtils.isNotEmpty(this.registries)) {
            Iterator var3 = this.registries.iterator();
            //循环的从注册链表中拿取地址及配置
            label47:
            while(true) {
                RegistryConfig config;
                String address;
                do {
                    if (!var3.hasNext()) {
                        return registryList;
                    }

                    config = (RegistryConfig)var3.next();
                    address = config.getAddress();
                    //address为空就默认为0.0.0.0
                    if (StringUtils.isEmpty(address)) {
                        address = "0.0.0.0";
                    }
                } while("N/A".equalsIgnoreCase(address));

                Map<String, String> map = new HashMap();
                // 添加 ApplicationConfig 中的字段信息到 map 中
                appendParameters(map, this.application);
                // 添加 RegistryConfig 字段信息到 map 中
                appendParameters(map, config);
                // 添加 path，protocol 等信息到 map 中
                map.put("path", RegistryService.class.getName());
                appendRuntimeParameters(map);
                if (!map.containsKey("protocol")) {
                    map.put("protocol", "dubbo");
                }
                // 解析得到 URL 列表，address 可能包含多个注册中心 ip，
                // 因此解析得到的是一个 URL 列表
                List<URL> urls = UrlUtils.parseURLs(address, map);
                Iterator var8 = urls.iterator();

                while(true) {
                    URL url;
                    do {
                        if (!var8.hasNext()) {
                            continue label47;
                        }

                        url = (URL)var8.next();
                        //// 将 URL 协议头设置为 registry
                        url = URLBuilder.from(url).addParameter("registry", url.getProtocol()).setProtocol("registry").build();
                    // 通过判断条件，决定是否添加 url 到 registryList 中，条件如下：
                    // (服务提供者 && register = true 或 null) || (非服务提供者 && subscribe = true 或 null)
                    } while((!provider || !url.getParameter("register", true)) && (provider || !url.getParameter("subscribe", true)));

                    //添加url到registryList中
                    registryList.add(url);
                }
            }
        } else {
            return registryList;
        }
    }
```
loadRegistries方法主要包含如下的逻辑：
1. 构建参数映射集合，也就是 map
2. 构建注册中心链接列表
3. 遍历链接列表，并根据条件决定是否将其添加到 registryList 中

实际上因为Dubbo现如今支持很多注册中心，所以对于一些注册中心的URL也要进行遍历构建。这里是生成注册中心的URL。还未生成Dubbo服务的URL。比如说使用的是Zookeeper注册中心，可能从loadRegistries中拿到的就是：
```
registry://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-provider&dubbo=2.7.3&pid=1528&qos.port=22222&registry=zookeeper&timestamp=1530743640901
```
这种类型的URL，表示这是一个注册协议，现在可以根据这个URL定位到注册中心去了。服务接口是RegistryService，registry的类型为zookeeper。可是我们还未生成Dubbo服务提供方的URL所以接着看下面代码

然后进行到doExportUrlsFor1Protocol（装配Dubbo服务的URL并且实行发布）
```
private void doExportUrlsFor1Protocol(ProtocolConfig protocolConfig, List<URL> registryURLs) {
        //首先是将一些信息，比如版本、时间戳、方法名以及各种配置对象的字段信息放入到 map 中
        //map 中的内容将作为 URL 的查询字符串。构建好 map 后，紧接着是获取上下文路径、主机名以及端口号等信息。
       //最后将 map 和主机名等数据传给 URL 构造方法创建 URL 对象。需要注意的是，这里出现的 URL 并非 java.net.URL，而是 com.alibaba.dubbo.common.URL。
        String name = protocolConfig.getName();
        // 如果协议名为空，或空串，则将协议名变量设置为 dubbo
        if (StringUtils.isEmpty(name)) {
            name = "dubbo";
        }

        Map<String, String> map = new HashMap();
        // 添加 side、版本、时间戳以及进程号等信息到 map 中
        map.put("side", "provider");
        appendRuntimeParameters(map);
        // 通过反射将对象的字段信息添加到 map 中
        appendParameters(map, this.metrics);
        appendParameters(map, this.application);
        appendParameters(map, this.module);
        appendParameters(map, this.provider);
        appendParameters(map, protocolConfig);
        appendParameters(map, this);
        String scope;
        Iterator metadataReportService;
        // methods 为 MethodConfig 集合，MethodConfig 中存储了 <dubbo:method> 标签的配置信息
        if (CollectionUtils.isNotEmpty(this.methods)) {
            Iterator var5 = this.methods.iterator();
            //检测 <dubbo:method> 标签中的配置信息，并将相关配置添加到 map 中
            label166:
            while(true) {
                MethodConfig method;
                List arguments;
                do {
                    if (!var5.hasNext()) {
                        break label166;
                    }

                    method = (MethodConfig)var5.next();
                    appendParameters(map, method, method.getName());
                    String retryKey = method.getName() + ".retry";
                    if (map.containsKey(retryKey)) {
                        scope = (String)map.remove(retryKey);
                        if ("false".equals(scope)) {
                            map.put(method.getName() + ".retries", "0");
                        }
                    }

                    arguments = method.getArguments();
                } while(!CollectionUtils.isNotEmpty(arguments));

                metadataReportService = arguments.iterator();

                while(true) {
                    ArgumentConfig argument;
                    Method[] methods;
                    do {
                        do {
                            while(true) {
                                if (!metadataReportService.hasNext()) {
                                    continue label166;
                                }

                                argument = (ArgumentConfig)metadataReportService.next();
                                if (argument.getType() != null && argument.getType().length() > 0) {
                                    methods = this.interfaceClass.getMethods();
                                    break;
                                }

                                if (argument.getIndex() == -1) {
                                    throw new IllegalArgumentException("Argument config must set index or type attribute.eg: <dubbo:argument index='0' .../> or <dubbo:argument type=xxx .../>");
                                }

                                appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                            }
                        } while(methods == null);
                    } while(methods.length <= 0);

                    for(int i = 0; i < methods.length; ++i) {
                        String methodName = methods[i].getName();
                        if (methodName.equals(method.getName())) {
                            Class<?>[] argtypes = methods[i].getParameterTypes();
                            if (argument.getIndex() != -1) {
                                if (!argtypes[argument.getIndex()].getName().equals(argument.getType())) {
                                    throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                }

                                appendParameters(map, argument, method.getName() + "." + argument.getIndex());
                            } else {
                                for(int j = 0; j < argtypes.length; ++j) {
                                    Class<?> argclazz = argtypes[j];
                                    if (argclazz.getName().equals(argument.getType())) {
                                        appendParameters(map, argument, method.getName() + "." + j);
                                        if (argument.getIndex() != -1 && argument.getIndex() != j) {
                                            throw new IllegalArgumentException("Argument config error : the index attribute and type attribute not match :index :" + argument.getIndex() + ", type:" + argument.getType());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        String host;
         // 检测 generic 是否为 "true"，并根据检测结果向 map 中添加不同的信息
        if (ProtocolUtils.isGeneric(this.generic)) {
            map.put("generic", this.generic);
            map.put("methods", "*");
        } else {
            host = Version.getVersion(this.interfaceClass, this.version);
            if (host != null && host.length() > 0) {
                map.put("revision", host);
            }
             // 为接口生成包裹类 Wrapper，Wrapper 中包含了接口的详细信息，比如接口方法名数组，字段信息等
            String[] methods = Wrapper.getWrapper(this.interfaceClass).getMethodNames();
            if (methods.length == 0) {
                logger.warn("No method found in service interface " + this.interfaceClass.getName());
                map.put("methods", "*");
            } else {
                // 将逗号作为分隔符连接方法名，并将连接后的字符串放入 map 中
                map.put("methods", StringUtils.join(new HashSet(Arrays.asList(methods)), ","));
            }
        }
        // 添加 token 到 map 中
        if (!ConfigUtils.isEmpty(this.token)) {
            if (ConfigUtils.isDefault(this.token)) {
                map.put("token", UUID.randomUUID().toString());
            } else {
                map.put("token", this.token);
            }
        }
        //获取host和port
        host = this.findConfigedHosts(protocolConfig, registryURLs, map);
        Integer port = this.findConfigedPorts(protocolConfig, name, map);
        // 获取上下文路径并且组装URL
        URL url = new URL(name, host, port, (String)this.getContextPath(protocolConfig).map((p) -> {
            return p + "/" + this.path;
        }).orElse(this.path), map);
        if (ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).hasExtension(url.getProtocol())) {
            // 加载 ConfiguratorFactory，并生成 Configurator 实例，然后通过实例配置 url，使用了前面提到的SPI机制
            url = ((ConfiguratorFactory)ExtensionLoader.getExtensionLoader(ConfiguratorFactory.class).getExtension(url.getProtocol())).getConfigurator(url).configure(url);
        }
        //下面逻辑主要分三步
        // 如果 scope = none，则什么都不做
        // scope != remote，导出到本地
        // scope != local，导出到远程
        scope = url.getParameter("scope");
        if (!"none".equalsIgnoreCase(scope)) {
            if (!"remote".equalsIgnoreCase(scope)) {
                this.exportLocal(url);
            }

            if (!"local".equalsIgnoreCase(scope)) {
                if (!this.isOnlyInJvm() && logger.isInfoEnabled()) {
                    logger.info("Export dubbo service " + this.interfaceClass.getName() + " to url " + url);
                }

                if (CollectionUtils.isNotEmpty(registryURLs)) {
                    metadataReportService = registryURLs.iterator();

                    while(metadataReportService.hasNext()) {
                        URL registryURL = (URL)metadataReportService.next();
                        if (!"injvm".equalsIgnoreCase(url.getProtocol())) {
                            url = url.addParameterIfAbsent("dynamic", registryURL.getParameter("dynamic"));
                            URL monitorUrl = this.loadMonitor(registryURL);
                            if (monitorUrl != null) {
                                url = url.addParameterAndEncoded("monitor", monitorUrl.toFullString());
                            }

                            if (logger.isInfoEnabled()) {
                                logger.info("Register dubbo service " + this.interfaceClass.getName() + " url " + url + " to registry " + registryURL);
                            }

                            String proxy = url.getParameter("proxy");
                            if (StringUtils.isNotEmpty(proxy)) {
                                registryURL = registryURL.addParameter("proxy", proxy);
                            }
                            // 为服务提供类(ref)生成 Invoker
                            Invoker<?> invoker = PROXY_FACTORY.getInvoker(this.ref, this.interfaceClass, registryURL.addParameterAndEncoded("export", url.toFullString()));
                            DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
                            // 导出服务，并生成 Exporter
                            Exporter<?> exporter = protocol.export(wrapperInvoker);
                            this.exporters.add(exporter);
                        }
                    }
                // 不存在注册中心，仅导出服务
                } else {
                    Invoker<?> invoker = PROXY_FACTORY.getInvoker(this.ref, this.interfaceClass, url);
                    DelegateProviderMetaDataInvoker wrapperInvoker = new DelegateProviderMetaDataInvoker(invoker, this);
                    Exporter<?> exporter = protocol.export(wrapperInvoker);
                    this.exporters.add(exporter);
                }

                metadataReportService = null;
                MetadataReportService metadataReportService;
                if ((metadataReportService = this.getMetadataReportService()) != null) {
                    metadataReportService.publishProvider(url);
                }
            }
        }

        this.urls.add(url);
    }
```
上面的源码前半段是进行URL装配，这个URL就是Dubbo服务的URL，大致如下：
```
dubbo://192.168.1.6:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=demo-provider&bind.ip=192.168.1.6&bind.port=20880&dubbo=2.7.3&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=5744&qos.port=22222&side=provider&timestamp=1530746052546
```
这个URL表示它是一个dubbo协议(DubboProtocol)，地址是当前服务器的ip，端口是要暴露的服务的端口号，可以从dubbo:protocol配置，服务接口为dubbo:service配置发布的接口。

后半段主要是判断scope变量来决定是否将服务导出远程或者本地，导出到本地实际上很简单只需要生成Invoker。当导出到远程就需要添加监视器还要生成invoker。监视器能让Dubbo定时查看注册中心挂了没。会抛出指定异常，而invoker使得服务消费方能够远程调用到服务。并且还会进行注册到注册中心下面我们接着来看看服务的发布。因为Invoker比较重要在消费者和提供者中都有，所以这个后面会单独拿出来进行探讨。
## 服务发布本地与远程
### 服务发布到本地
```
private void exportLocal(URL url) {
        //进行本地URL的构建
        URL local = URLBuilder.from(url).setProtocol("injvm").setHost("127.0.0.1").setPort(0).build();
        //根据本地的URL来实现对应的Invoker
        Exporter<?> exporter = protocol.export(PROXY_FACTORY.getInvoker(this.ref, this.interfaceClass, local));
        this.exporters.add(exporter);
        logger.info("Export dubbo service " + this.interfaceClass.getName() + " to local registry url : " + local);
    }
```
可见发布到本地是重新构建了protocol，injvm就是代表在本地的JVM里，host与port都统一默认127.0.0.1:0。
### 服务发布到远程
```
public <T> Exporter<T> export(Invoker<T> originInvoker) throws RpcException {
        //获取注册中心的URL，比如：zookeeper://127.0.0.1:2181/......
        URL registryUrl = this.getRegistryUrl(originInvoker);
        //获取所有服务提供者的URL，比如：dubbo://192.168.1.6:20880/.......
        URL providerUrl = this.getProviderUrl(originInvoker);
        //获取订阅URL，比如：provider://192.168.1.6:20880/......
        URL overrideSubscribeUrl = this.getSubscribedOverrideUrl(providerUrl);
        //创建监听器
        RegistryProtocol.OverrideListener overrideSubscribeListener = new RegistryProtocol.OverrideListener(overrideSubscribeUrl, originInvoker);
        //向订阅中心推送监听器
        this.overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);
        providerUrl = this.overrideUrlWithConfig(providerUrl, overrideSubscribeListener);
        //导出服务
        RegistryProtocol.ExporterChangeableWrapper<T> exporter = this.doLocalExport(originInvoker, providerUrl);
        Registry registry = this.getRegistry(originInvoker);
        //获取已注册的服务提供者的URL，比如dubbo://192.168.1.6:20880/.......
        URL registeredProviderUrl = this.getRegisteredProviderUrl(providerUrl, registryUrl);
        // 向服务提供者与消费者注册表中注册服务提供者
        ProviderInvokerWrapper<T> providerInvokerWrapper = ProviderConsumerRegTable.registerProvider(originInvoker, registryUrl, registeredProviderUrl);
        // 获取 register 参数
        boolean register = registeredProviderUrl.getParameter("register", true);
        // 根据 register 的值决定是否注册服务
        if (register) {
            this.register(registryUrl, registeredProviderUrl);
            providerInvokerWrapper.setReg(true);
        }
        // 向注册中心进行订阅 override 数据
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);
        exporter.setRegisterUrl(registeredProviderUrl);
        exporter.setSubscribeUrl(overrideSubscribeUrl);
        // 创建并返回 DestroyableExporter
        return new RegistryProtocol.DestroyableExporter(exporter);
    }
```
上面的源码主要是根据前面生成的URL进行服务的发布和注册（注册在下一节展开源码）。当执行到doLocalExport也就是发布本地服务到远程时候会调用 DubboProtocol 的 export 方法大致会经历下面一些步骤来导出服务
- 从Invoker获取providerUrl，构建serviceKey(group/service:version:port)，构建DubboExporter并以serviceKey为key放入本地map缓存
- 处理url携带的本地存根和callback回调
- 根据url打开服务器端口，暴露本地服务。先以url.getAddress为key查询本地缓存serverMap获取ExchangeServer，如果不存在，则通过createServer创建。
- createServer方法，设置心跳时间，判断url中的传输方式(key=server,对应Transporter服务)是否支持，设置codec=dubbo，最后根据url和ExchangeHandler对象绑定server返回，这里的ExchangeHandler非常重要，它就是消费方调用时，底层通信层回调的Handler，从而获取包含实际Service实现的Invoker执行器，它是定义在DubboProtocol类中的ExchangeHandlerAdapter内部类。
- 返回DubboExporter对象

到这里大致的服务发布图如下：

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408003048.png)

## 服务注册
服务注册操作对于 Dubbo 来说不是必需的，通过服务直连的方式就可以绕过注册中心。但通常我们不会这么做，直连方式不利于服务治理，仅推荐在测试服务时使用。对于 Dubbo 来说，注册中心虽不是必需，但却是必要的。源码如下：
```
public void register(URL url) {
    super.register(url);
    failedRegistered.remove(url);
    failedUnregistered.remove(url);
    try {
        // 模板方法，由子类实现
        doRegister(url);
    } catch (Exception e) {
        Throwable t = e;

        // 获取 check 参数，若 check = true 将会直接抛出异常
        boolean check = getUrl().getParameter(Constants.CHECK_KEY, true)
                && url.getParameter(Constants.CHECK_KEY, true)
                && !Constants.CONSUMER_PROTOCOL.equals(url.getProtocol());
        boolean skipFailback = t instanceof SkipFailbackWrapperException;
        if (check || skipFailback) {
            if (skipFailback) {
                t = t.getCause();
            }
            throw new IllegalStateException("Failed to register");
        } else {
            logger.error("Failed to register");
        }

        // 记录注册失败的链接
        failedRegistered.add(url);
    }
}

//进入doRegister方法

protected void doRegister(URL url) {
    try {
        // 通过 Zookeeper 客户端创建节点，节点路径由 toUrlPath 方法生成，路径格式如下:
        //   /${group}/${serviceInterface}/providers/${url}
        // 比如
        //   /dubbo/org.apache.dubbo.DemoService/providers/dubbo%3A%2F%2F127.0.0.1......
        zkClient.create(toUrlPath(url), url.getParameter(Constants.DYNAMIC_KEY, true));
    } catch (Throwable e) {
        throw new RpcException("Failed to register...");
    }
}

//进入create方法

public void create(String path, boolean ephemeral) {
    if (!ephemeral) {
        // 如果要创建的节点类型非临时节点，那么这里要检测节点是否存在
        if (checkExists(path)) {
            return;
        }
    }
    int i = path.lastIndexOf('/');
    if (i > 0) {
        // 递归创建上一级路径
        create(path.substring(0, i), false);
    }
    
    // 根据 ephemeral 的值创建临时或持久节点
    if (ephemeral) {
        createEphemeral(path);
    } else {
        createPersistent(path);
    }
}

//进入createEphemeral

public void createEphemeral(String path) {
    try {
        // 通过 Curator 框架创建节点
        client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
    } catch (NodeExistsException e) {
    } catch (Exception e) {
        throw new IllegalStateException(e.getMessage(), e);
    }
}
```
根据上面的方法，可以将当前服务对应的配置信息（存储在URL中的）注册到注册中心/dubbo/org.apache.dubbo.demo.DemoService/providers/ 。里面直接使用了Curator进行创建节点（Curator是Netflix公司开源的一套zookeeper客户端框架）
## 总结
到这里Dubbo的服务注册流程终于是解释完。核心在于Dubbo使用规定好的URL+SPI进行寻找和发现服务，通过URL定位注册中心，再通过将服务的URL发布到注册中心从而使得消费者可以知道服务的有哪些，里面可以看见对于URL这种复杂的对象并且需要经常更改的，通常采用建造者模式。而2.7.3版本的Dubbo源码也使用了Java8以后的新特性Lambda表达式来构建隐式函数。而一整套流程下来可以在ZooInspector这个zk可视化客户端看见我们创建的节点，前提是注册中心为zk。