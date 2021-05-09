[toc]



# JVM GUI 工具

## 1. jconsole

> jconsole 是 JDK 自带的 GUI 工具。**jconsole(Java Monitoring and Management Console) 是一种基于 JMX 的可视化监视与管理工具**。
>
> jconsole 的管理功能是针对 JMX MBean 进行管理，由于 MBean 可以使用代码、中间件服务器的管理控制台或所有符合 JMX 规范的软件进行访问。

注意：使用 jconsole 的前提是 Java 应用开启 JMX。

### 1.1. 开启 JMX

Java 应用开启 JMX 后，可以使用 `jconsole` 或 `jvisualvm` 进行监控 Java 程序的基本信息和运行情况。

开启方法是，在 java 指令后，添加以下参数：

```
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Djava.rmi.server.hostname=127.0.0.1
-Dcom.sun.management.jmxremote.port=18888
```

- `-Djava.rmi.server.hostname` - 指定 Java 程序运行的服务器
- `-Dcom.sun.management.jmxremote.port` - 指定 JMX 服务监听端口

### 1.2. 连接 jconsole

如果是本地 Java 进程，jconsole 可以直接绑定连接。

如果是远程 Java 进程，需要连接 Java 进程的 JMX 端口。

![68747470733a2f2f646f63732e6f7261636c652e636f6d2f6a61766173652f382f646f63732f746563686e6f7465732f6775696465732f6d616e6167656d656e742f666967757265732f636f6e6e6563746164762e676966](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122351.gif)

### 1.3. jconsole 界面

进入 jconsole 应用后，可以看到以下 tab 页面。

- `概述` - 显示有关 Java VM 和监视值的概述信息。
- `内存` - 显示有关内存使用的信息。内存页相当于可视化的 `jstat` 命令。
- `线程` - 显示有关线程使用的信息。
- `类` - 显示有关类加载的信息。
- `VM 摘要` - 显示有关 Java VM 的信息。
- `MBean` - 显示有关 MBean 的信息。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733303135313432322e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122402.png)

## 2. jvisualvm

> jvisualvm 是 JDK 自带的 GUI 工具。**jvisualvm(All-In-One Java Troubleshooting Tool) 是多合一故障处理工具**。它支持运行监视、故障处理、性能分析等功能。

个人觉得 jvisualvm 比 jconsole 好用。

### 2.1. jvisualvm 概述页面

jvisualvm 概述页面可以查看当前 Java 进程的基本信息，如：JDK 版本、Java 进程、JVM 参数等。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733303135303134372e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122422.png)

### 2.2. jvisualvm 监控页面

在 jvisualvm 监控页面，可以看到 Java 进程的 CPU、内存、类加载、线程的实时变化。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733303135303235342e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122434.png)

### 2.3. jvisualvm 线程页面

jvisualvm 线程页面展示了当前的线程状态。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733303135303431362e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122445.png)

jvisualvm 还可以生成线程 Dump 文件，帮助进一步分析线程栈信息。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733303135303833302e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122457.png)

### 2.4. jvisualvm 抽样器页面

jvisualvm 可以对 CPU、内存进行抽样，帮助我们进行性能分析。



## 3. MAT

[MAT](https://www.eclipse.org/mat/) 即 Eclipse Memory Analyzer Tool 的缩写。

MAT 本身也能够获取堆的二进制快照。该功能将借助 `jps` 列出当前正在运行的 Java 进程，以供选择并获取快照。由于 `jps` 会将自己列入其中，因此你会在列表中发现一个已经结束运行的 `jps` 进程。

MAT 可以独立安装（[官方下载地址](http://www.eclipse.org/mat/downloads.php)），也可以作为 Eclipse IDE 的插件安装。

### 3.1. MAT 配置

MAT 解压后，安装目录下有个 `MemoryAnalyzer.ini` 文件。

```
MemoryAnalyzer.ini 中有个重要的参数 Xmx 表示最大内存，默认为：-vmargs -Xmx1024m
```

如果试图用 MAT 导入的 dump 文件超过 1024 M，会报错：

```
An internal error occurred during: "Parsing heap dump from XXX"
```

此时，可以适当调整 `Xmx` 大小。如果设置的 `Xmx` 数值过大，本机内存不足以支撑，启动 MAT 会报错：

```
Failed to create the Java Virtual Machine
```

### 3.2. MAT 分析

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303330383039323734362e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122522.png)

点击 Leak Suspects 可以进入内存泄漏页面。

（1）首先，可以查看饼图了解内存的整体消耗情况

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303330383135303535362e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122538.png)

（2）缩小范围，寻找问题疑似点

![68747470733a2f2f696d672d626c6f672e6373646e2e6e65742f3230313630323233323032313534383138](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122546.png)

可以点击进入详情页面，在详情页面 Shortest Paths To the Accumulation Point 表示 GC root 到内存消耗聚集点的最短路径，如果某个内存消耗聚集点有路径到达 GC root，则该内存消耗聚集点不会被当做垃圾被回收。

为了找到内存泄露，我获取了两个堆转储文件，两个文件获取时间间隔是一天（因为内存只是小幅度增长，短时间很难发现问题）。对比两个文件的对象，通过对比后的结果可以很方便定位内存泄露。

MAT 同时打开两个堆转储文件，分别打开 Histogram，如下图。在下图中方框 1 按钮用于对比两个 Histogram，对比后在方框 2 处选择 Group By package，然后对比各对象的变化。不难发现 heap3.hprof 比 heap6.hprof 少了 64 个 eventInfo 对象，如果对代码比较熟悉的话想必这样一个结果是能够给程序员一定的启示的。而我也是根据这个启示差找到了最终内存泄露的位置。

![68747470733a2f2f696d672d626c6f672e6373646e2e6e65742f3230313630323233323033323236333632](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122557.png)

## 4. JProfile

[JProfiler](https://www.ej-technologies.com/products/jprofiler/overview.html) 是一款性能分析工具。

由于它是收费的，所以我本人使用较少。但是，它确实功能强大，且方便使用，还可以和 Intellij Idea 集成。



## 5. Arthas

[Arthas](https://github.com/alibaba/arthas) 是 Alibaba 开源的 Java 诊断工具，深受开发者喜爱。在线排查问题，无需重启；动态跟踪 Java 代码；实时监控 JVM 状态。

Arthas 支持 JDK 6+，支持 Linux/Mac/Windows，采用命令行交互模式，同时提供丰富的 `Tab` 自动补全功能，进一步方便进行问题的定位和诊断。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733303134353033302e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122610.png)

### 5.1. Arthas 基础命令

- help——查看命令帮助信息
- [cat](https://alibaba.github.io/arthas/cat.html)——打印文件内容，和 linux 里的 cat 命令类似
- [echo](https://alibaba.github.io/arthas/echo.html)–打印参数，和 linux 里的 echo 命令类似
- [grep](https://alibaba.github.io/arthas/grep.html)——匹配查找，和 linux 里的 grep 命令类似
- [tee](https://alibaba.github.io/arthas/tee.html)——复制标准输入到标准输出和指定的文件，和 linux 里的 tee 命令类似
- [pwd](https://alibaba.github.io/arthas/pwd.html)——返回当前的工作目录，和 linux 命令类似
- cls——清空当前屏幕区域
- session——查看当前会话的信息
- [reset](https://alibaba.github.io/arthas/reset.html)——重置增强类，将被 Arthas 增强过的类全部还原，Arthas 服务端关闭时会重置所有增强过的类
- version——输出当前目标 Java 进程所加载的 Arthas 版本号
- history——打印命令历史
- quit——退出当前 Arthas 客户端，其他 Arthas 客户端不受影响
- stop——关闭 Arthas 服务端，所有 Arthas 客户端全部退出
- [keymap](https://alibaba.github.io/arthas/keymap.html)——Arthas 快捷键列表及自定义快捷键

### 5.2. Arthas jvm 相关命令

- [dashboard](https://alibaba.github.io/arthas/dashboard.html)——当前系统的实时数据面板
- [thread](https://alibaba.github.io/arthas/thread.html)——查看当前 JVM 的线程堆栈信息
- [jvm](https://alibaba.github.io/arthas/jvm.html)——查看当前 JVM 的信息
- [sysprop](https://alibaba.github.io/arthas/sysprop.html)——查看和修改 JVM 的系统属性
- [sysenv](https://alibaba.github.io/arthas/sysenv.html)——查看 JVM 的环境变量
- [vmoption](https://alibaba.github.io/arthas/vmoption.html)——查看和修改 JVM 里诊断相关的 option
- [perfcounter](https://alibaba.github.io/arthas/perfcounter.html)——查看当前 JVM 的 Perf Counter 信息
- [logger](https://alibaba.github.io/arthas/logger.html)——查看和修改 logger
- [getstatic](https://alibaba.github.io/arthas/getstatic.html)——查看类的静态属性
- [ognl](https://alibaba.github.io/arthas/ognl.html)——执行 ognl 表达式
- [mbean](https://alibaba.github.io/arthas/mbean.html)——查看 Mbean 的信息
- [heapdump](https://alibaba.github.io/arthas/heapdump.html)——dump java heap, 类似 jmap 命令的 heap dump 功能

### 5.3. Arthas class/classloader 相关命令

- [sc](https://alibaba.github.io/arthas/sc.html)——查看 JVM 已加载的类信息
- [sm](https://alibaba.github.io/arthas/sm.html)——查看已加载类的方法信息
- [jad](https://alibaba.github.io/arthas/jad.html)——反编译指定已加载类的源码
- [mc](https://alibaba.github.io/arthas/mc.html)——内存编译器，内存编译`.java`文件为`.class`文件
- [redefine](https://alibaba.github.io/arthas/redefine.html)——加载外部的`.class`文件，redefine 到 JVM 里
- [dump](https://alibaba.github.io/arthas/dump.html)——dump 已加载类的 byte code 到特定目录
- [classloader](https://alibaba.github.io/arthas/classloader.html)——查看 classloader 的继承树，urls，类加载信息，使用 classloader 去 getResource

### 5.4. Arthas monitor/watch/trace 相关命令

> 请注意，这些命令，都通过字节码增强技术来实现的，会在指定类的方法中插入一些切面来实现数据统计和观测，因此在线上、预发使用时，请尽量明确需要观测的类、方法以及条件，诊断结束要执行 `stop` 或将增强过的类执行 `reset` 命令。

- [monitor](https://alibaba.github.io/arthas/monitor.html)——方法执行监控
- [watch](https://alibaba.github.io/arthas/watch.html)——方法执行数据观测
- [trace](https://alibaba.github.io/arthas/trace.html)——方法内部调用路径，并输出方法路径上的每个节点上耗时
- [stack](https://alibaba.github.io/arthas/stack.html)——输出当前方法被调用的调用路径
- [tt](https://alibaba.github.io/arthas/tt.html)——方法执行数据的时空隧道，记录下指定方法每次调用的入参和返回信息，并能对这些不同的时间下调用进行观测