[toc]



# Dubbo 服务发现（SPI）机制

## 前言
用到微服务就不得不来谈谈服务发现的话题。通俗的来说，就是在提供服务方把服务注册到注册中心，并且告诉服务消费方现在已经存在了这个服务。那么里面的细节到底是怎么通过代码实现的呢，现在我们来看看Dubbo中的SPI机制
## SPI简介
SPI 全称为 Service Provider Interface，是一种服务发现机制。SPI本质是将接口实现类的全限定名配置在文件中，并由服务加载器读取配置文件，加载实现类，这样运行时可以动态的为接口替换实现类
## Dubbo中的SPI
Dubbo与上面的普通的Java方式实现SPI不同，在Dubbo中重新实现了一套功能更强的SPI机制，即通过键值对的方式进行配置及缓存。其中也使用ConcurrentHashMap与synchronize防止并发问题出现。主要逻辑封装在ExtensionLoader中。下面我们看看源码。
## ExtensionLoader源码解析
由于内部的方法实在太多，我们只挑选与实现SPI的重要逻辑部分拿出来讲解。　　

### getExtensionLoader(Class<T> type)
```
public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type == null");
        } else if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an interface!");
        } else if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type (" + type + ") is not an extension, because it is NOT annotated with @" + SPI.class.getSimpleName() + "!");
        } else {
            ExtensionLoader<T> loader = (ExtensionLoader)EXTENSION_LOADERS.get(type);
            if (loader == null) {
                EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader(type));
                loader = (ExtensionLoader)EXTENSION_LOADERS.get(type);
            }

            return loader;
        }
    }
```
这个是可以将对应的接口转换为ExtensionLoader 实例。相当于告诉Dubbo这是个服务接口，里面有对应的服务提供者

先是逻辑判断传进来的类不能为空，必须是接口且被@SPI注解注释过。这三个条件都满足就会创建ExtensionLoader 实例。同样的，如果当前类已经被创建过ExtensionLoader 实例，那么直接拿取。否则新建一个。这里使用的是键值对的存储类型，如下图：

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408002316.png)

使用ConcurrentHashMap防止在并发时出现问题，并且效率高HashTable不少，所以我们日常项目并发场景中也应该多用ConcurrentHashMap进行存储。

### getExtension(String name)
```
public T getExtension(String name) {
    if (name == null || name.length() == 0)
        throw new IllegalArgumentException("Extension name == null");
    if ("true".equals(name)) {
        // 获取默认的拓展实现类
        return getDefaultExtension();
    }
    // Holder，顾名思义，用于持有目标对象
    Holder<Object> holder = cachedInstances.get(name);
    if (holder == null) {
        cachedInstances.putIfAbsent(name, new Holder<Object>());
        holder = cachedInstances.get(name);
    }
    Object instance = holder.get();
    // 双重检查
    if (instance == null) {
        synchronized (holder) {
            instance = holder.get();
            if (instance == null) {
                // 创建拓展实例
                instance = createExtension(name);
                // 设置实例到 holder 中
                holder.set(instance);
            }
        }
    }
    return  instance;
}
```
这个方法主要是相当于得到具体的服务，上述我们已经对服务的接口进行加载，现在我们需要调用服务接口下的某一个具体服务实现类。就用这个方法。上述方法可以看出是会进入getOrCreateHolder中，这个方法顾名思义是获取或者创建Holder。进入到下面方法中：
```
private Holder<Object> getOrCreateHolder(String name) {
        //检查缓存中是否存在
        Holder<Object> holder = (Holder)this.cachedInstances.get(name);
        if (holder == null) {
        //缓存中不存在就去创建一个新的Holder
            this.cachedInstances.putIfAbsent(name, new Holder());
            holder = (Holder)this.cachedInstances.get(name);
        }

        return holder;
    }
```
同样，缓存池也是以ConcurrentHashMap为存储结构

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408002330.png)

### createExtension(String name)
实际上getExtension方法不一定每次都能拿到，当服务实现类是第一次进行加载的时候就需要当前的方法
```
private T createExtension(String name) {
        Class<?> clazz = (Class)this.getExtensionClasses().get(name);
        if (clazz == null) {
            throw this.findException(name);
        } else {
            try {
                T instance = EXTENSION_INSTANCES.get(clazz);
                if (instance == null) {
                    EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                    instance = EXTENSION_INSTANCES.get(clazz);
                }

                this.injectExtension(instance);
                Set<Class<?>> wrapperClasses = this.cachedWrapperClasses;
                Class wrapperClass;
                if (CollectionUtils.isNotEmpty(wrapperClasses)) {
                    for(Iterator var5 = wrapperClasses.iterator(); var5.hasNext(); instance = this.injectExtension(wrapperClass.getConstructor(this.type).newInstance(instance))) {
                        wrapperClass = (Class)var5.next();
                    }
                }

                return instance;
            } catch (Throwable var7) {
                throw new IllegalStateException("Extension instance (name: " + name + ", class: " + this.type + ") couldn't be instantiated: " + var7.getMessage(), var7);
            }
        }
    }
```
可以看出createExtension实际上是一个私有方法，也就是由上面的getExtension自动触发。内部逻辑大致为：
1. 通过 getExtensionClasses 获取所有的拓展类
1. 通过反射创建拓展对象
1. 向拓展对象中注入依赖（这里Dubbo有单独的IOC后面会介绍）
1. 将拓展对象包裹在相应的 Wrapper 对象中

### getExtensionClasses()
```
private Map<String, Class<?>> getExtensionClasses() {
    // 从缓存中获取已加载的拓展类
    Map<String, Class<?>> classes = cachedClasses.get();
    // 双重检查
    if (classes == null) {
        synchronized (cachedClasses) {
            classes = cachedClasses.get();
            if (classes == null) {
                // 加载拓展类
                classes = loadExtensionClasses();
                cachedClasses.set(classes);
            }
        }
    }
    return classes;
}

//进入到loadExtensionClasses中

private Map<String, Class<?>> loadExtensionClasses() {
    // 获取 SPI 注解，这里的 type 变量是在调用 getExtensionLoader 方法时传入的
    final SPI defaultAnnotation = type.getAnnotation(SPI.class);
    if (defaultAnnotation != null) {
        String value = defaultAnnotation.value();
        if ((value = value.trim()).length() > 0) {
            // 对 SPI 注解内容进行切分
            String[] names = NAME_SEPARATOR.split(value);
            // 检测 SPI 注解内容是否合法，不合法则抛出异常
            if (names.length > 1) {
                throw new IllegalStateException("more than 1 default extension name on extension...");
            }

            // 设置默认名称，参考 getDefaultExtension 方法
            if (names.length == 1) {
                cachedDefaultName = names[0];
            }
        }
    }

    Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
    // 加载指定文件夹下的配置文件
    loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY);
    loadDirectory(extensionClasses, DUBBO_DIRECTORY);
    loadDirectory(extensionClasses, SERVICES_DIRECTORY);
    return extensionClasses;
}

//进入到loadDirectory中

private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir) {
    // fileName = 文件夹路径 + type 全限定名 
    String fileName = dir + type.getName();
    try {
        Enumeration<java.net.URL> urls;
        ClassLoader classLoader = findClassLoader();
        // 根据文件名加载所有的同名文件
        if (classLoader != null) {
            urls = classLoader.getResources(fileName);
        } else {
            urls = ClassLoader.getSystemResources(fileName);
        }
        if (urls != null) {
            while (urls.hasMoreElements()) {
                java.net.URL resourceURL = urls.nextElement();
                // 加载资源
                loadResource(extensionClasses, classLoader, resourceURL);
            }
        }
    } catch (Throwable t) {
        logger.error("...");
    }
}

//进入到loadResource中

private void loadResource(Map<String, Class<?>> extensionClasses, 
    ClassLoader classLoader, java.net.URL resourceURL) {
    try {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(resourceURL.openStream(), "utf-8"));
        try {
            String line;
            // 按行读取配置内容
            while ((line = reader.readLine()) != null) {
                // 定位 # 字符
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // 截取 # 之前的字符串，# 之后的内容为注释，需要忽略
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        String name = null;
                        int i = line.indexOf('=');
                        if (i > 0) {
                            // 以等于号 = 为界，截取键与值
                            name = line.substring(0, i).trim();
                            line = line.substring(i + 1).trim();
                        }
                        if (line.length() > 0) {
                            // 加载类，并通过 loadClass 方法对类进行缓存
                            loadClass(extensionClasses, resourceURL, 
                                      Class.forName(line, true, classLoader), name);
                        }
                    } catch (Throwable t) {
                        IllegalStateException e = new IllegalStateException("Failed to load extension class...");
                    }
                }
            }
        } finally {
            reader.close();
        }
    } catch (Throwable t) {
        logger.error("Exception when load extension class...");
    }
}

//进入到loadClass中

private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, 
    Class<?> clazz, String name) throws NoSuchMethodException {
    
    if (!type.isAssignableFrom(clazz)) {
        throw new IllegalStateException("...");
    }

    // 检测目标类上是否有 Adaptive 注解
    if (clazz.isAnnotationPresent(Adaptive.class)) {
        if (cachedAdaptiveClass == null) {
            // 设置 cachedAdaptiveClass缓存
            cachedAdaptiveClass = clazz;
        } else if (!cachedAdaptiveClass.equals(clazz)) {
            throw new IllegalStateException("...");
        }
        
    // 检测 clazz 是否是 Wrapper 类型
    } else if (isWrapperClass(clazz)) {
        Set<Class<?>> wrappers = cachedWrapperClasses;
        if (wrappers == null) {
            cachedWrapperClasses = new ConcurrentHashSet<Class<?>>();
            wrappers = cachedWrapperClasses;
        }
        // 存储 clazz 到 cachedWrapperClasses 缓存中
        wrappers.add(clazz);
        
    // 程序进入此分支，表明 clazz 是一个普通的拓展类
    } else {
        // 检测 clazz 是否有默认的构造方法，如果没有，则抛出异常
        clazz.getConstructor();
        if (name == null || name.length() == 0) {
            // 如果 name 为空，则尝试从 Extension 注解中获取 name，或使用小写的类名作为 name
            name = findAnnotationName(clazz);
            if (name.length() == 0) {
                throw new IllegalStateException("...");
            }
        }
        // 切分 name
        String[] names = NAME_SEPARATOR.split(name);
        if (names != null && names.length > 0) {
            Activate activate = clazz.getAnnotation(Activate.class);
            if (activate != null) {
                // 如果类上有 Activate 注解，则使用 names 数组的第一个元素作为键，
                // 存储 name 到 Activate 注解对象的映射关系
                cachedActivates.put(names[0], activate);
            }
            for (String n : names) {
                if (!cachedNames.containsKey(clazz)) {
                    // 存储 Class 到名称的映射关系
                    cachedNames.put(clazz, n);
                }
                Class<?> c = extensionClasses.get(n);
                if (c == null) {
                    // 存储名称到 Class 的映射关系
                    extensionClasses.put(n, clazz);
                } else if (c != clazz) {
                    throw new IllegalStateException("...");
                }
            }
        }
    }
}
```
上面的方法较多，理一下逻辑：
1. getExtensionClasses()：先检查缓存，若缓存未命中，则通过 synchronized 加锁。加锁后再次检查缓存，并判断是否为空。此时如果 classes 仍为 null，则通过 loadExtensionClasses 加载拓展类。
2. loadExtensionClasses()：对 SPI 注解的接口进行解析，而后调用 loadDirectory 方法加载指定文件夹配置文件。
3. loadDirectory()：方法先通过 classLoader 获取所有资源链接，然后再通过 loadResource 方法加载资源。
4. loadResource()：用于读取和解析配置文件，并通过反射加载类，最后调用 loadClass 方法进行其他操作。loadClass 方法用于主要用于操作缓存。

### 小结
我们稍微捋一下Dubbo是如何进行SPI的即发现接口的实现类。先是需要实例化扩展类加载器。这里为了更好的和微服务贴合起来，我们就把它称作服务加载器。在服务加载器中用的是ConcurrentHashMap的缓存结构。在我们需要寻找服务的过程中，Dubbo先通过反射加载类，而后将有@SPI表示的接口（即服务接口）的实现类（即服务提供方）进行配置对应的文件夹及文件。将配置文件以键值对的方式存到缓存中key就是当前服务接口下类的名字，value就是Dubbo生成的对应的类配置文件。方便我们下次调用。其中为了防止并发问题产生，使用ConcurrentHashMap，并且使用synchronize关键字对存在并发问题的节点进行双重检查。

## Dubbo中的IOC
在createExtension中有提到过将拓展对象注入依赖。这里使用的是injectExtension(T instance)：
```
private T injectExtension(T instance) {
    try {
        if (objectFactory != null) {
            // 遍历目标类的所有方法
            for (Method method : instance.getClass().getMethods()) {
                // 检测方法是否以 set 开头，且方法仅有一个参数，且方法访问级别为 public
                if (method.getName().startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())) {
                    // 获取 setter 方法参数类型
                    Class<?> pt = method.getParameterTypes()[0];
                    try {
                        // 获取属性名，比如 setName 方法对应属性名 name
                        String property = method.getName().length() > 3 ? 
                            method.getName().substring(3, 4).toLowerCase() + 
                                method.getName().substring(4) : "";
                        // 从 ObjectFactory 中获取依赖对象
                        Object object = objectFactory.getExtension(pt, property);
                        if (object != null) {
                            // 通过反射调用 setter 方法设置依赖
                            method.invoke(instance, object);
                        }
                    } catch (Exception e) {
                        logger.error("fail to inject via method...");
                    }
                }
            }
        }
    } catch (Exception e) {
        logger.error(e.getMessage(), e);
    }
    return instance;
}
```
在上面代码中，objectFactory 变量的类型为 AdaptiveExtensionFactory，AdaptiveExtensionFactory 内部维护了一个ExtensionFactory 列表，用于存储其他类型的 ExtensionFactory。Dubbo 目前提供了两种 ExtensionFactory，分别是 SpiExtensionFactory 和SpringExtensionFactory。前者用于创建自适应的拓展，后者是用于从 Spring 的 IOC 容器中获取所需的拓展。这就是我们常说的Dubbo为什么能够与Spring无缝连接，因为Dubbo底层就是依赖Spring的，对于Spring的IOC容器可直接拿来用。