[toc]



# JVM 性能调优

## 1. JVM 调优概述

### 1.1. GC 性能指标

对于 JVM 调优来说，需要先明确调优的目标。 从性能的角度看，通常关注三个指标：

- `吞吐量（throughput）` - 指不考虑 GC 引起的停顿时间或内存消耗，垃圾收集器能支撑应用达到的最高性能指标。
- `停顿时间（latency）` - 其度量标准是缩短由于垃圾啊收集引起的停顿时间或者完全消除因垃圾收集所引起的停顿，避免应用运行时发生抖动。
- `垃圾回收频率` - 久发生一次指垃圾回收呢？通常垃圾回收的频率越低越好，增大堆内存空间可以有效降低垃圾回收发生的频率，但同时也意味着堆积的回收对象越多，最终也会增加回收时的停顿时间。所以我们只要适当地增大堆内存空间，保证正常的垃圾回收频率即可。

大多数情况下调优会侧重于其中一个或者两个方面的目标，很少有情况可以兼顾三个不同的角度。

### 1.2. 调优原则

GC 优化的两个目标：

- **降低 Full GC 的频率**
- **减少 Full GC 的执行时间**

GC 优化的基本原则是：将不同的 GC 参数应用到两个及以上的服务器上然后比较它们的性能，然后将那些被证明可以提高性能或减少 GC 执行时间的参数应用于最终的工作服务器上。

#### 1.2.1.降低 Minor GC 频率

如果新生代空间较小，Eden 区很快被填满，就会导致频繁 Minor GC，因此我们可以通过增大新生代空间来降低 Minor GC 的频率。

可能你会有这样的疑问，扩容 Eden 区虽然可以减少 Minor GC 的次数，但不会增加单次 Minor GC 的时间吗？如果单次 Minor GC 的时间增加，那也很难达到我们期待的优化效果呀。

我们知道，单次 Minor GC 时间是由两部分组成：T1（扫描新生代）和 T2（复制存活对象）。假设一个对象在 Eden 区的存活时间为 500ms，Minor GC 的时间间隔是 300ms，那么正常情况下，Minor GC 的时间为 ：T1+T2。

当我们增大新生代空间，Minor GC 的时间间隔可能会扩大到 600ms，此时一个存活 500ms 的对象就会在 Eden 区中被回收掉，此时就不存在复制存活对象了，所以再发生 Minor GC 的时间为：两次扫描新生代，即 2T1。

可见，扩容后，Minor GC 时增加了 T1，但省去了 T2 的时间。通常在虚拟机中，复制对象的成本要远高于扫描成本。

如果在堆内存中存在较多的长期存活的对象，此时增加年轻代空间，反而会增加 Minor GC 的时间。如果堆中的短期对象很多，那么扩容新生代，单次 Minor GC 时间不会显著增加。因此，单次 Minor GC 时间更多取决于 GC 后存活对象的数量，而非 Eden 区的大小。

#### 1.2.2.降低 Full GC 的频率

Full GC 相对来说会比 Minor GC 更耗时。减少进入老年代的对象数量可以显著降低 Full GC 的频率。

**减少创建大对象：\**如果\**对象占用内存过大，在 Eden 区被创建后会直接被传入老年代**。在平常的业务场景中，我们习惯一次性从数据库中查询出一个大对象用于 web 端显示。例如，我之前碰到过一个一次性查询出 60 个字段的业务操作，这种大对象如果超过年轻代最大对象阈值，会被直接创建在老年代；即使被创建在了年轻代，由于年轻代的内存空间有限，通过 Minor GC 之后也会进入到老年代。这种大对象很容易产生较多的 Full GC。

我们可以将这种大对象拆解出来，首次只查询一些比较重要的字段，如果还需要其它字段辅助查看，再通过第二次查询显示剩余的字段。

**增大堆内存空间：**在堆内存不足的情况下，增大堆内存空间，且设置初始化堆内存为最大堆内存，也可以降低 Full GC 的频率。

#### 1.2.3.降低 Full GC 的时间

Full GC 的执行时间比 Minor GC 要长很多，因此，如果在 Full GC 上花费过多的时间（超过 1s），将可能出现超时错误。

- 如果**通过减小老年代内存来减少 Full GC 时间**，可能会引起 `OutOfMemoryError` 或者导致 Full GC 的频率升高。
- 另外，如果**通过增加老年代内存来降低 Full GC 的频率**，Full GC 的时间可能因此增加。

因此，你**需要把老年代的大小设置成一个“合适”的值**。



### 1.3. GC 优化的过程

GC 优化的过程大致可分为以下步骤：

#### （1）监控 GC 状态

你需要监控 GC 从而检查系统中运行的 GC 的各种状态。

#### （2）分析 GC 日志

在检查 GC 状态后，你需要分析监控结构并决定是否需要进行 GC 优化。如果分析结果显示运行 GC 的时间只有 0.1-0.3 秒，那么就不需要把时间浪费在 GC 优化上，但如果运行 GC 的时间达到 1-3 秒，甚至大于 10 秒，那么 GC 优化将是很有必要的。

但是，如果你已经分配了大约 10GB 内存给 Java，并且这些内存无法省下，那么就无法进行 GC 优化了。在进行 GC 优化之前，你需要考虑为什么你需要分配这么大的内存空间，如果你分配了 1GB 或 2GB 大小的内存并且出现了`OutOfMemoryError`，那你就应该执行**堆快照（heap dump）**来消除导致异常的原因。

> 🔔 注意：

> **堆快照（heap dump）**是一个用来检查 Java 内存中的对象和数据的内存文件。该文件可以通过执行 JDK 中的`jmap`命令来创建。在创建文件的过程中，所有 Java 程序都将暂停，因此，不要在系统执行过程中创建该文件。

> 你可以在互联网上搜索 heap dump 的详细说明。

#### （3）选择合适 GC 回收器

如果你决定要进行 GC 优化，那么你需要选择一个 GC 回收器，并且为它设置合理 JVM 参数。此时如果你有多个服务器，请如上文提到的那样，在每台机器上设置不同的 GC 参数并分析它们的区别。

#### （4）分析结果

在设置完 GC 参数后就可以开始收集数据，请在收集至少 24 小时后再进行结果分析。如果你足够幸运，你可能会找到系统的最佳 GC 参数。如若不然，你还需要分析输出日志并检查分配的内存，然后需要通过不断调整 GC 类型/内存大小来找到系统的最佳参数。

#### （5）应用优化配置

如果 GC 优化的结果令人满意，就可以将参数应用到所有服务器上，并停止 GC 优化。

在下面的章节中，你将会看到上述每一步所做的具体工作。

## 2. GC 日志

### 2.1. 获取 GC 日志

获取 GC 日志有两种方式：

- 使用 `jstat` 命令动态查看
- 在容器中设置相关参数打印 GC 日志

#### jstat 命令查看 GC

`jstat -gc` 统计垃圾回收堆的行为：

```
jstat -gc 1262
 S0C    S1C     S0U     S1U   EC       EU        OC         OU        PC       PU         YGC    YGCT    FGC    FGCT     GCT
26112.0 24064.0 6562.5  0.0   564224.0 76274.5   434176.0   388518.3  524288.0 42724.7    320    6.417   1      0.398    6.815
```

也可以设置间隔固定时间来打印：

```
jstat -gc 1262 2000 20
```

这个命令意思就是每隔 2000ms 输出 1262 的 gc 情况，一共输出 20 次

#### 打印 GC 的参数

通过 JVM 参数预先设置 GC 日志，通常有以下几种 JVM 参数设置：

```
-XX:+PrintGC 输出 GC 日志
-XX:+PrintGCDetails 输出 GC 的详细日志
-XX:+PrintGCTimeStamps 输出 GC 的时间戳（以基准时间的形式）
-XX:+PrintGCDateStamps 输出 GC 的时间戳（以日期的形式，如 2013-05-04T21:53:59.234+0800）
-XX:+PrintHeapAtGC 在进行 GC 的前后打印出堆的信息
-verbose:gc -Xloggc:../logs/gc.log 日志文件的输出路径
```

如果是长时间的 GC 日志，我们很难通过文本形式去查看整体的 GC 性能。此时，我们可以通过[GCView](https://sourceforge.net/projects/gcviewer/)工具打开日志文件，图形化界面查看整体的 GC 性能。

【示例】Tomcat 设置示例

```
JAVA_OPTS="-server -Xms2000m -Xmx2000m -Xmn800m -XX:PermSize=64m -XX:MaxPermSize=256m -XX:SurvivorRatio=4
-verbose:gc -Xloggc:$CATALINA_HOME/logs/gc.log
-Djava.awt.headless=true
-XX:+PrintGCTimeStamps -XX:+PrintGCDetails
-Dsun.rmi.dgc.server.gcInterval=600000 -Dsun.rmi.dgc.client.gcInterval=600000
-XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=15"
```

- `-Xms2000m -Xmx2000m -Xmn800m -XX:PermSize=64m -XX:MaxPermSize=256m` Xms，即为 jvm 启动时得 JVM 初始堆大小,Xmx 为 jvm 的最大堆大小，xmn 为新生代的大小，permsize 为永久代的初始大小，MaxPermSize 为永久代的最大空间。
- `-XX:SurvivorRatio=4` SurvivorRatio 为新生代空间中的 Eden 区和救助空间 Survivor 区的大小比值，默认是 8，则两个 Survivor 区与一个 Eden 区的比值为 2:8,一个 Survivor 区占整个年轻代的 1/10。调小这个参数将增大 survivor 区，让对象尽量在 survitor 区呆长一点，减少进入年老代的对象。去掉救助空间的想法是让大部分不能马上回收的数据尽快进入年老代，加快年老代的回收频率，减少年老代暴涨的可能性，这个是通过将-XX:SurvivorRatio 设置成比较大的值（比如 65536)来做到。
- `-verbose:gc -Xloggc:$CATALINA_HOME/logs/gc.log` 将虚拟机每次垃圾回收的信息写到日志文件中，文件名由 file 指定，文件格式是平文件，内容和-verbose:gc 输出内容相同。
- `-Djava.awt.headless=true` Headless 模式是系统的一种配置模式。在该模式下，系统缺少了显示设备、键盘或鼠标。
- `-XX:+PrintGCTimeStamps -XX:+PrintGCDetails` 设置 gc 日志的格式
- `-Dsun.rmi.dgc.server.gcInterval=600000 -Dsun.rmi.dgc.client.gcInterval=600000` 指定 rmi 调用时 gc 的时间间隔
- `-XX:+UseConcMarkSweepGC -XX:MaxTenuringThreshold=15` 采用并发 gc 方式，经过 15 次 minor gc 后进入年老代

### 2.2. 分析 GC 日志

Young GC 回收日志:

```
2016-07-05T10:43:18.093+0800: 25.395: [GC [PSYoungGen: 274931K->10738K(274944K)] 371093K->147186K(450048K), 0.0668480 secs] [Times: user=0.17 sys=0.08, real=0.07 secs]
```

Full GC 回收日志:

```
2016-07-05T10:43:18.160+0800: 25.462: [Full GC [PSYoungGen: 10738K->0K(274944K)] [ParOldGen: 136447K->140379K(302592K)] 147186K->140379K(577536K) [PSPermGen: 85411K->85376K(171008K)], 0.6763541 secs] [Times: user=1.75 sys=0.02, real=0.68 secs]
```

通过上面日志分析得出，PSYoungGen、ParOldGen、PSPermGen 属于 Parallel 收集器。其中 PSYoungGen 表示 gc 回收前后年轻代的内存变化；ParOldGen 表示 gc 回收前后老年代的内存变化；PSPermGen 表示 gc 回收前后永久区的内存变化。young gc 主要是针对年轻代进行内存回收比较频繁，耗时短；full gc 会对整个堆内存进行回城，耗时长，因此一般尽量减少 full gc 的次数



#### CPU 过高

定位步骤：

（1）执行 top -c 命令，找到 cpu 最高的进程的 id

（2）jstack PID 导出 Java 应用程序的线程堆栈信息。

示例：

```
jstack 6795

"Low Memory Detector" daemon prio=10 tid=0x081465f8 nid=0x7 runnable [0x00000000..0x00000000]
        "CompilerThread0" daemon prio=10 tid=0x08143c58 nid=0x6 waiting on condition [0x00000000..0xfb5fd798]
        "Signal Dispatcher" daemon prio=10 tid=0x08142f08 nid=0x5 waiting on condition [0x00000000..0x00000000]
        "Finalizer" daemon prio=10 tid=0x08137ca0 nid=0x4 in Object.wait() [0xfbeed000..0xfbeeddb8]

        at java.lang.Object.wait(Native Method)

        - waiting on <0xef600848> (a java.lang.ref.ReferenceQueue$Lock)

        at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:116)

        - locked <0xef600848> (a java.lang.ref.ReferenceQueue$Lock)

        at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:132)

        at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:159)

        "Reference Handler" daemon prio=10 tid=0x081370f0 nid=0x3 in Object.wait() [0xfbf4a000..0xfbf4aa38]

        at java.lang.Object.wait(Native Method)

        - waiting on <0xef600758> (a java.lang.ref.Reference$Lock)

        at java.lang.Object.wait(Object.java:474)

        at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:116)

        - locked <0xef600758> (a java.lang.ref.Reference$Lock)

        "VM Thread" prio=10 tid=0x08134878 nid=0x2 runnable

        "VM Periodic Task Thread" prio=10 tid=0x08147768 nid=0x8 waiting on condition
```

在打印的堆栈日志文件中，tid 和 nid 的含义：

```
nid : 对应的 Linux 操作系统下的 tid 线程号，也就是前面转化的 16 进制数字
tid: 这个应该是 jvm 的 jmm 内存规范中的唯一地址定位
```

在 CPU 过高的情况下，查找响应的线程，一般定位都是用 nid 来定位的。而如果发生死锁之类的问题，一般用 tid 来定位。

（3）定位 CPU 高的线程打印其 nid

查看线程下具体进程信息的命令如下：

top -H -p 6735

```
top - 14:20:09 up 611 days,  2:56,  1 user,  load average: 13.19, 7.76, 7.82
Threads: 6991 total,  17 running, 6974 sleeping,   0 stopped,   0 zombie
%Cpu(s): 90.4 us,  2.1 sy,  0.0 ni,  7.0 id,  0.0 wa,  0.0 hi,  0.4 si,  0.0 st
KiB Mem:  32783044 total, 32505008 used,   278036 free,   120304 buffers
KiB Swap:        0 total,        0 used,        0 free. 4497428 cached Mem

  PID USER      PR  NI    VIRT    RES    SHR S %CPU %MEM     TIME+ COMMAND
 6800 root      20   0 27.299g 0.021t   7172 S 54.7 70.1 187:55.61 java
 6803 root      20   0 27.299g 0.021t   7172 S 54.4 70.1 187:52.59 java
 6798 root      20   0 27.299g 0.021t   7172 S 53.7 70.1 187:55.08 java
 6801 root      20   0 27.299g 0.021t   7172 S 53.7 70.1 187:55.25 java
 6797 root      20   0 27.299g 0.021t   7172 S 53.1 70.1 187:52.78 java
 6804 root      20   0 27.299g 0.021t   7172 S 53.1 70.1 187:55.76 java
 6802 root      20   0 27.299g 0.021t   7172 S 52.1 70.1 187:54.79 java
 6799 root      20   0 27.299g 0.021t   7172 S 51.8 70.1 187:53.36 java
 6807 root      20   0 27.299g 0.021t   7172 S 13.6 70.1  48:58.60 java
11014 root      20   0 27.299g 0.021t   7172 R  8.4 70.1   8:00.32 java
10642 root      20   0 27.299g 0.021t   7172 R  6.5 70.1   6:32.06 java
 6808 root      20   0 27.299g 0.021t   7172 S  6.1 70.1 159:08.40 java
11315 root      20   0 27.299g 0.021t   7172 S  3.9 70.1   5:54.10 java
12545 root      20   0 27.299g 0.021t   7172 S  3.9 70.1   6:55.48 java
23353 root      20   0 27.299g 0.021t   7172 S  3.9 70.1   2:20.55 java
24868 root      20   0 27.299g 0.021t   7172 S  3.9 70.1   2:12.46 java
 9146 root      20   0 27.299g 0.021t   7172 S  3.6 70.1   7:42.72 java
```

由此可以看出占用 CPU 较高的线程，但是这些还不高，无法直接定位到具体的类。nid 是 16 进制的，所以我们要获取线程的 16 进制 ID：

```
printf "%x\n" 6800
输出结果:45cd
```

然后根据输出结果到 jstack 打印的堆栈日志中查定位：

```
"catalina-exec-5692" daemon prio=10 tid=0x00007f3b05013800 nid=0x45cd waiting on condition [0x00007f3ae08e3000]
   java.lang.Thread.State: TIMED_WAITING (parking)
        at sun.misc.Unsafe.park(Native Method)
        - parking to wait for  <0x00000006a7800598> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
        at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:226)
        at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.awaitNanos(AbstractQueuedSynchronizer.java:2082)
        at java.util.concurrent.LinkedBlockingQueue.poll(LinkedBlockingQueue.java:467)
        at org.apache.tomcat.util.threads.TaskQueue.poll(TaskQueue.java:86)
        at org.apache.tomcat.util.threads.TaskQueue.poll(TaskQueue.java:32)
        at java.util.concurrent.ThreadPoolExecutor.getTask(ThreadPoolExecutor.java:1068)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1130)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)
        at java.lang.Thread.run(Thread.java:745)
```

## 3. GC 配置

> 详细参数说明请参考官方文档：[JavaHotSpot VM Options](http://www.oracle.com/technetwork/java/javase/tech/vmoptions-jsp-140102.html)，这里仅列举常用参数。

### 3.1. 堆大小设置

**年轻代的设置很关键。**

JVM 中最大堆大小有三方面限制：

1. 相关操作系统的数据模型（32-bt 还是 64-bit）限制；
2. 系统的可用虚拟内存限制；
3. 系统的可用物理内存限制。

```
整个堆大小 = 年轻代大小 + 年老代大小 + 持久代大小
```

- 持久代一般固定大小为 `64m`。使用 `-XX:PermSize` 设置。
- 官方推荐年轻代占整个堆的 3/8。使用 `-Xmn` 设置。

### 3.2. JVM 内存配置

| 配置                | 描述                                                         |
| ------------------- | ------------------------------------------------------------ |
| `-Xss`              | 虚拟机栈大小。                                               |
| `-Xms`              | 堆空间初始值。                                               |
| `-Xmx`              | 堆空间最大值。                                               |
| `-Xmn`              | 新生代空间大小。                                             |
| `-XX:NewSize`       | 新生代空间初始值。                                           |
| `-XX:MaxNewSize`    | 新生代空间最大值。                                           |
| `-XX:NewRatio`      | 新生代与年老代的比例。默认为 2，意味着老年代是新生代的 2 倍。 |
| `-XX:SurvivorRatio` | 新生代中调整 eden 区与 survivor 区的比例，默认为 8。即 `eden` 区为 80% 的大小，两个 `survivor` 分别为 10% 的大小。 |
| `-XX:PermSize`      | 永久代空间的初始值。                                         |
| `-XX:MaxPermSize`   | 永久代空间的最大值。                                         |

### 3.3. GC 类型配置

一般而言， GC不应该成为影响系统性能的瓶颈，我们在评估 GC收集器的优劣时一般考虑以下几点：

- 吞吐量
- GC开销
- 暂停时间
- GC频率
- 堆空间
- 对象生命周期

所以针对不同的 GC收集器，我们要对应我们的应用场景来进行选择和调优，回顾 GC的历史，主要有 4种 GC收集器: Serial、 Parallel、 CMS和 G1。

| 配置                        | 描述                                                 |
| --------------------------- | ---------------------------------------------------- |
| `-XX:+UseSerialGC`          | 使用 Serial + Serial Old 垃圾回收器组合              |
| `-XX:+UseParallelGC`        | 使用 Parallel Scavenge + Parallel Old 垃圾回收器组合 |
| ~~`-XX:+UseParallelOldGC`~~ | ~~使用 Parallel Old 垃圾回收器（JDK5 后已无用）~~    |
| `-XX:+UseParNewGC`          | 使用 ParNew + Serial Old 垃圾回收器                  |
| `-XX:+UseConcMarkSweepGC`   | 使用 CMS + ParNew + Serial Old 垃圾回收器组合        |
| `-XX:+UseG1GC`              | 使用 G1 垃圾回收器                                   |
| `-XX:ParallelCMSThreads`    | 并发标记扫描垃圾回收器 = 为使用的线程数量            |

### 3.4. 垃圾回收器通用参数

| 配置                     | 描述                                                         |
| ------------------------ | ------------------------------------------------------------ |
| `PretenureSizeThreshold` | 晋升年老代的对象大小。默认为 0。比如设为 10M，则超过 10M 的对象将不在 eden 区分配，而直接进入年老代。 |
| `MaxTenuringThreshold`   | 晋升老年代的最大年龄。默认为 15。比如设为 10，则对象在 10 次普通 GC 后将会被放入年老代。 |
| `DisableExplicitGC`      | 禁用 `System.gc()`                                           |

### 3.5. JMX

开启 JMX 后，可以使用 `jconsole` 或 `jvisualvm` 进行监控 Java 程序的基本信息和运行情况。

```
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Djava.rmi.server.hostname=127.0.0.1
-Dcom.sun.management.jmxremote.port=18888
```

`-Djava.rmi.server.hostname` 指定 Java 程序运行的服务器，`-Dcom.sun.management.jmxremote.port` 指定服务监听端口。

### 3.6. 远程 DEBUG

如果开启 Java 应用的远程 Debug 功能，需要指定如下参数：

```
-Xdebug
-Xnoagent
-Djava.compiler=NONE
-Xrunjdwp:transport=dt_socket,address=28888,server=y,suspend=n
```

address 即为远程 debug 的监听端口。

### 3.7. HeapDump

```
-XX:-OmitStackTraceInFastThrow -XX:+HeapDumpOnOutOfMemoryError
```

### 3.8. 辅助配置

| 配置                              | 描述                     |
| --------------------------------- | ------------------------ |
| `-XX:+PrintGCDetails`             | 打印 GC 日志             |
| `-Xloggc:<filename>`              | 指定 GC 日志文件名       |
| `-XX:+HeapDumpOnOutOfMemoryError` | 内存溢出时输出堆快照文件 |



## 4.常用参数设置

### 4.1.虚拟机参数

| **参数名称**                | **含义**                                                   | **默认值**           | 解释说明                                                     |
| :-------------------------- | :--------------------------------------------------------- | :------------------- | :----------------------------------------------------------- |
| -Xms                        | 初始堆大小                                                 | 物理内存的1/64(<1GB) | 默认(MinHeapFreeRatio参数可以调整)空余堆内存小于40%时，JVM就会增大堆直到-Xmx的最大限制. |
| -Xmx                        | 最大堆大小                                                 | 物理内存的1/4(<1GB)  | 默认(MaxHeapFreeRatio参数可以调整)空余堆内存大于70%时，JVM会减少堆直到 -Xms的最小限制 |
| -Xmn                        | 年轻代大小(1.4or lator)                                    |                      | **注意**：此处的大小是（eden+ 2 survivor space).与jmap -heap中显示的New gen是不同的。 整个堆大小=年轻代大小 + 年老代大小 + 持久代大小. 增大年轻代后,将会减小年老代大小.此值对系统性能影响较大,Sun官方推荐配置为整个堆的3/8 |
| -XX:NewSize                 | 设置年轻代大小(for 1.3/1.4)                                |                      |                                                              |
| -XX:MaxNewSize              | 年轻代最大值(for 1.3/1.4)                                  |                      |                                                              |
| -XX:PermSize                | 设置持久代(perm gen)初始值                                 | 物理内存的1/64       |                                                              |
| -XX:MaxPermSize             | 设置持久代最大值                                           | 物理内存的1/4        |                                                              |
| -Xss                        | 每个线程的堆栈大小                                         |                      | JDK5.0以后每个线程堆栈大小为1M,以前每个线程堆栈大小为256K.更具应用的线程所需内存大小进行 调整.在相同物理内存下,减小这个值能生成更多的线程.但是操作系统对一个进程内的线程数还是有限制的,不能无限生成,经验值在3000~5000左右 一般小的应用， 如果栈不是很深， 应该是128k够用的 大的应用建议使用256k。这个选项对性能影响比较大，需要严格的测试。 和threadstacksize选项解释很类似,官方文档似乎没有解释,在论坛中有这样一句话:"” -Xss is translated in a VM flag named ThreadStackSize” 一般设置这个值就可以了。 |
| -*XX:ThreadStackSize*       | Thread Stack Size                                          |                      | (0 means use default stack size) [Sparc: 512; Solaris x86: 320 (was 256 prior in 5.0 and earlier); Sparc 64 bit: 1024; Linux amd64: 1024 (was 0 in 5.0 and earlier); all others 0.] |
| -XX:NewRatio                | 年轻代(包括Eden和两个Survivor区)与年老代的比值(除去持久代) |                      | -XX:NewRatio=4表示年轻代与年老代所占比值为1:4,年轻代占整个堆栈的1/5 Xms=Xmx并且设置了Xmn的情况下，该参数不需要进行设置。 |
| -XX:SurvivorRatio           | Eden区与Survivor区的大小比值                               |                      | 设置为8,则两个Survivor区与一个Eden区的比值为2:8,一个Survivor区占整个年轻代的1/10 |
| -XX:LargePageSizeInBytes    | 内存页的大小不可设置过大， 会影响Perm的大小                |                      | =128m                                                        |
| -XX:+UseFastAccessorMethods | 原始类型的快速优化                                         |                      |                                                              |
| -XX:+DisableExplicitGC      | 关闭System.gc()                                            |                      | 这个参数需要严格的测试                                       |
| -XX:MaxTenuringThreshold    | 垃圾最大年龄                                               |                      | 如果设置为0的话,则年轻代对象不经过Survivor区,直接进入年老代. 对于年老代比较多的应用,可以提高效率.如果将此值设置为一个较大值,则年轻代对象会在Survivor区进行多次复制,这样可以增加对象再年轻代的存活 时间,增加在年轻代即被回收的概率 该参数只有在串行GC时才有效. |
| -XX:+AggressiveOpts         | 加快编译                                                   |                      |                                                              |
| -XX:+UseBiasedLocking       | 锁机制的性能改善                                           |                      |                                                              |
| -Xnoclassgc                 | 禁用垃圾回收                                               |                      |                                                              |
| -XX:SoftRefLRUPolicyMSPerMB | 每兆堆空闲空间中SoftReference的存活时间                    | 1s                   | softly reachable objects will remain alive for some amount of time after the last time they were referenced. The default value is one second of lifetime per free megabyte in the heap |
| -XX:PretenureSizeThreshold  | 对象超过多大是直接在旧生代分配                             | 0                    | 单位字节 新生代采用Parallel Scavenge GC时无效 另一种直接在旧生代分配的情况是大的数组对象,且数组中无外部引用对象. |
| -XX:TLABWasteTargetPercent  | TLAB占eden区的百分比                                       | 1%                   |                                                              |
| -XX:+*CollectGen0First*     | FullGC时是否先YGC                                          | false                |                                                              |

### 4.2.并行收集器相关参数

| **参数名称**                | **含义**                                          | **默认值** | 解释说明                                                     |
| :-------------------------- | :------------------------------------------------ | :--------- | :----------------------------------------------------------- |
| -XX:+UseParallelGC          | Full GC采用parallel MSC (此项待验证)              |            | 选择垃圾收集器为并行收集器.此配置仅对年轻代有效.即上述配置下,年轻代使用并发收集,而年老代仍旧使用串行收集.(此项待验证) |
| -XX:+UseParNewGC            | 设置年轻代为并行收集                              |            | 可与CMS收集同时使用 JDK5.0以上,JVM会根据系统配置自行设置,所以无需再设置此值 |
| -XX:ParallelGCThreads       | 并行收集器的线程数                                |            | 此值最好配置与处理器数目相等 同样适用于CMS                   |
| -XX:+UseParallelOldGC       | 年老代垃圾收集方式为并行收集(Parallel Compacting) |            | 这个是JAVA 6出现的参数选项                                   |
| -XX:MaxGCPauseMillis        | 每次年轻代垃圾回收的最长时间(最大暂停时间)        |            | 如果无法满足此时间,JVM会自动调整年轻代大小,以满足此值.       |
| -XX:+UseAdaptiveSizePolicy  | 自动选择年轻代区大小和相应的Survivor区比例        |            | 设置此选项后,并行收集器会自动选择年轻代区大小和相应的Survivor区比例,以达到目标系统规定的最低相应时间或者收集频率等,此值建议使用并行收集器时,一直打开. |
| -XX:GCTimeRatio             | 设置垃圾回收时间占程序运行时间的百分比            |            | 公式为1/(1+n)                                                |
| -XX:+*ScavengeBeforeFullGC* | Full GC前调用YGC                                  | true       | Do young generation GC prior to a full GC. (Introduced in 1.4.1.) |

### 4.3.CMS处理器参数设置

| **参数名称**                           | **含义**                                  | **默认值** | 解释说明                                                     |
| :------------------------------------- | :---------------------------------------- | :--------- | :----------------------------------------------------------- |
| -XX:+UseConcMarkSweepGC                | 使用CMS内存收集                           |            | 测试中配置这个以后,-XX:NewRatio=4的配置失效了,原因不明.所以,此时年轻代大小最好用-Xmn设置.??? |
| -XX:+AggressiveHeap                    |                                           |            | 试图是使用大量的物理内存 长时间大内存使用的优化，能检查计算资源（内存， 处理器数量） 至少需要256MB内存 大量的CPU／内存， （在1.4.1在4CPU的机器上已经显示有提升） |
| -XX:CMSFullGCsBeforeCompaction         | 多少次后进行内存压缩                      |            | 由于并发收集器不对内存空间进行压缩,整理,所以运行一段时间以后会产生"碎片",使得运行效率降低.此值设置运行多少次GC以后对内存空间进行压缩,整理. |
| -XX:+CMSParallelRemarkEnabled          | 降低标记停顿                              |            |                                                              |
| -XX+UseCMSCompactAtFullCollection      | 在FULL GC的时候， 对年老代的压缩          |            | CMS是不会移动内存的， 因此， 这个非常容易产生碎片， 导致内存不够用， 因此， 内存的压缩这个时候就会被启用。 增加这个参数是个好习惯。 可能会影响性能,但是可以消除碎片 |
| -XX:+UseCMSInitiatingOccupancyOnly     | 使用手动定义初始化定义开始CMS收集         |            | 禁止hostspot自行触发CMS GC                                   |
| -XX:CMSInitiatingOccupancyFraction=70  | 使用cms作为垃圾回收 使用70％后开始CMS收集 | 92         | 为了保证不出现promotion failed(见下面介绍)错误,该值的设置需要满足以下公式**[CMSInitiatingOccupancyFraction计算公式](http://www.cnblogs.com/redcreen/archive/2011/05/04/2037057.html#CMSInitiatingOccupancyFraction_value)** |
| -XX:CMSInitiatingPermOccupancyFraction | 设置Perm Gen使用到达多少比率时触发        | 92         |                                                              |
| -XX:+CMSIncrementalMode                | 设置为增量模式                            |            | 用于单CPU情况                                                |
| -XX:+CMSClassUnloadingEnabled          |                                           |            |                                                              |

### 4.4.JVM辅助信息参数设置

| **参数名称**                          | **含义**                                                 | **默认值** | 解释说明                                                     |
| :------------------------------------ | :------------------------------------------------------- | :--------- | :----------------------------------------------------------- |
| -XX:+PrintGC                          |                                                          |            | 输出形式:[GC 118250K->113543K(130112K), 0.0094143 secs] [Full GC 121376K->10414K(130112K), 0.0650971 secs] |
| -XX:+PrintGCDetails                   |                                                          |            | 输出形式:[GC [DefNew: 8614K->781K(9088K), 0.0123035 secs] 118250K->113543K(130112K), 0.0124633 secs] [GC [DefNew: 8614K->8614K(9088K), 0.0000665 secs][Tenured: 112761K->10414K(121024K), 0.0433488 secs] 121376K->10414K(130112K), 0.0436268 secs] |
| -XX:+PrintGCTimeStamps                |                                                          |            |                                                              |
| -XX:+PrintGC:PrintGCTimeStamps        |                                                          |            | 可与-XX:+PrintGC -XX:+PrintGCDetails混合使用 输出形式:11.851: [GC 98328K->93620K(130112K), 0.0082960 secs] |
| -XX:+PrintGCApplicationStoppedTime    | 打印垃圾回收期间程序暂停的时间.可与上面混合使用          |            | 输出形式:Total time for which application threads were stopped: 0.0468229 seconds |
| -XX:+PrintGCApplicationConcurrentTime | 打印每次垃圾回收前,程序未中断的执行时间.可与上面混合使用 |            | 输出形式:Application time: 0.5291524 seconds                 |
| -XX:+PrintHeapAtGC                    | 打印GC前后的详细堆栈信息                                 |            |                                                              |
| -Xloggc:filename                      | 把相关日志信息记录到文件以便分析. 与上面几个配合使用     |            |                                                              |
| -XX:+PrintClassHistogram              | garbage collects before printing the histogram.          |            |                                                              |
| -XX:+PrintTLAB                        | 查看TLAB空间的使用情况                                   |            |                                                              |
| XX:+PrintTenuringDistribution         | 查看每次minor GC后新的存活周期的阈值                     |            | Desired survivor size 1048576 bytes, new threshold 7 (max 15) new threshold 7即标识新的存活周期的阈值为7。 |

### 4.5.JVM GC垃圾回收器参数设置

JVM给出了3种选择：**串行收集器**、**并行收集器**、**并发收集器**。串行收集器只适用于小数据量的情况，所以生产环境的选择主要是并行收集器和并发收集器。默认情况下JDK5.0以前都是使用串行收集器，如果想使用其他收集器需要在启动时加入相应参数。JDK5.0以后，JVM会根据当前系统配置进行智能判断。

**串行收集器**
-XX:+UseSerialGC：设置串行收集器。

**并行收集器（吞吐量优先）**
-XX:+UseParallelGC：设置为并行收集器。此配置仅对年轻代有效。即年轻代使用并行收集，而年老代仍使用串行收集。

-XX:ParallelGCThreads=20：配置并行收集器的线程数，即：同时有多少个线程一起进行垃圾回收。此值建议配置与CPU数目相等。

-XX:+UseParallelOldGC：配置年老代垃圾收集方式为并行收集。JDK6.0开始支持对年老代并行收集。

-XX:MaxGCPauseMillis=100：设置每次年轻代垃圾回收的最长时间（单位毫秒）。如果无法满足此时间，JVM会自动调整年轻代大小，以满足此时间。

-XX:+UseAdaptiveSizePolicy：设置此选项后，并行收集器会自动调整年轻代Eden区大小和Survivor区大小的比例，以达成目标系统规定的最低响应时间或者收集频率等指标。此参数建议在使用并行收集器时，一直打开。
并发收集器（响应时间优先）

**并行收集器**

-XX:+UseConcMarkSweepGC：即CMS收集，设置年老代为并发收集。CMS收集是JDK1.4后期版本开始引入的新GC算法。它的主要适合场景是对响应时间的重要性需求大于对吞吐量的需求，能够承受垃圾回收线程和应用线程共享CPU资源，并且应用中存在比较多的长生命周期对象。CMS收集的目标是尽量减少应用的暂停时间，减少Full GC发生的几率，利用和应用程序线程并发的垃圾回收线程来标记清除年老代内存。

-XX:+UseParNewGC：设置年轻代为并发收集。可与CMS收集同时使用。JDK5.0以上，JVM会根据系统配置自行设置，所以无需再设置此参数。

-XX:CMSFullGCsBeforeCompaction=0：由于并发收集器不对内存空间进行压缩和整理，所以运行一段时间并行收集以后会产生内存碎片，内存使用效率降低。此参数设置运行0次Full GC后对内存空间进行压缩和整理，即每次Full GC后立刻开始压缩和整理内存。

-XX:+UseCMSCompactAtFullCollection：打开内存空间的压缩和整理，在Full GC后执行。可能会影响性能，但可以消除内存碎片。

-XX:+CMSIncrementalMode：设置为增量收集模式。一般适用于单CPU情况。

-XX:CMSInitiatingOccupancyFraction=70：表示年老代内存空间使用到70%时就开始执行CMS收集，以确保年老代有足够的空间接纳来自年轻代的对象，避免Full GC的发生。

**其它垃圾回收参数**

-XX:+ScavengeBeforeFullGC：年轻代GC优于Full GC执行。

-XX:-DisableExplicitGC：不响应 System.gc() 代码。

-XX:+UseThreadPriorities：启用本地线程优先级API。即使 java.lang.Thread.setPriority() 生效，不启用则无效。

-XX:SoftRefLRUPolicyMSPerMB=0：软引用对象在最后一次被访问后能存活0毫秒（JVM默认为1000毫秒）。

-XX:TargetSurvivorRatio=90：允许90%的Survivor区被占用（JVM默认为50%）。提高对于Survivor区的使用率。

### 4.6.JVM参数优先级

-Xmn，-XX:NewSize/-XX:MaxNewSize，-XX:NewRatio 3组参数都可以影响年轻代的大小，混合使用的情况下，优先级是什么？

答案如下：

> 高优先级：-XX:NewSize/-XX:MaxNewSize
> 中优先级：-Xmn（默认等效 -Xmn=-XX:NewSize=-XX:MaxNewSize=?）
> 低优先级：-XX:NewRatio
>
> 推荐使用-Xmn参数，原因是这个参数简洁，相当于一次设定 NewSize/MaxNewSIze，而且两者相等，适用于生产环境。-Xmn 配合 -Xms/-Xmx，即可将堆内存布局完成。
>
> -Xmn参数是在JDK 1.4 开始支持。

下面用一些小案例加深理解:

HelloGC是java代码编译后的一个class文件,代码:

```
public class T01_HelloGC {
    public static void main(String[] args) {

        for(int i=0; i<10000; i++) {
            byte[] b = new byte[1024 * 1024];
        }
    }
}
```

1.java -XX:+PrintCommandLineFlags HelloGC

```
[root@localhost courage]# java -XX:+PrintCommandLineFlags T01_HelloGC
-XX:InitialHeapSize=61780800 -XX:MaxHeapSize=988492800 -XX:+PrintCommandLineFlags -XX
:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseParallelGC 
```

2.

```
java -Xmn10M -Xms40M -Xmx60M -XX:+PrintCommandLineFlags -XX:+PrintGC  HelloGC
PrintGCDetails PrintGCTimeStamps PrintGCCauses
```

结果:

```
-XX:InitialHeapSize=41943040 -XX:MaxHeapSize=62914560 -XX:MaxNewSize=10485760 -XX:NewSize=10485760 -XX:+PrintCommandLineFlags -XX:+PrintGC -XX:+UseCompressedClassPointers -XX:+UseCompressedOops 
-XX:+UseParallelGC[GC (Allocation Failure)  7839K->392K(39936K), 0.0015452 secs]
[GC (Allocation Failure)  7720K->336K(39936K), 0.0005439 secs]
[GC (Allocation Failure)  7656K->336K(39936K), 0.0005749 secs]
[GC (Allocation Failure)  7659K->368K(39936K), 0.0005095 secs]
[GC (Allocation Failure)  7693K->336K(39936K), 0.0004385 secs]
[GC (Allocation Failure)  7662K->304K(40448K), 0.0028468 secs]
......
```

命令解释:

> java:表示使用java执行器执行
> -Xmn10M :表示设置年轻代值为10M
> -Xms40M :表示设置堆内存的最小Heap值为40M
> -Xmx60M :表示设置堆内存的最大Heap值为60M
> -XX:+PrintCommandLineFlags:打印显式隐式参数,就是结果前三行
> -XX:+PrintGC : 打印垃圾回收有关信息
> HelloGC :这是需要执行的启动类
> PrintGCDetails :打印GC详细信息
> PrintGCTimeStamps :打印GC时间戳
> PrintGCCauses	:打印GC产生的原因

结果解释:

[![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328155751.png)](https://img2020.cnblogs.com/blog/2002319/202102/2002319-20210208103357164-1086413610.png)

3.`java -XX:+UseConcMarkSweepGC -XX:+PrintCommandLineFlags HelloGC`

表示使用CMS垃圾收集器,同时打印参数
打印结果:

```
-XX:InitialHeapSize=61780800 
-XX:MaxHeapSize=988492800 
-XX:MaxNewSize=329252864 
-XX:MaxTenuringThreshold=6 
-XX:OldPLABSize=16 
-XX:+PrintCommandLineFlags 
-XX:+UseCompressedClassPointers 
-XX:+UseCompressedOops 
-XX:+UseConcMarkSweepGC 
-XX:+UseParNewGC
```

1. java -XX:+PrintFlagsInitial 默认参数值
2. java -XX:+PrintFlagsFinal 最终参数值
3. java -XX:+PrintFlagsFinal | grep xxx 找到对应的参数
4. java -XX:+PrintFlagsFinal -version |grep GC



## JVM调优流程

JVM调优,设计到三个大的方面,在服务器出现问题之前要先根据业务场景选择合适的垃圾处理器,设置不同的虚拟机参数,运行中观察GC日志,分析性能,分析问题定位问题,虚拟机排错等内容,如果服务器挂掉了,要及时生成日志文件便于找到问题所在。

### 调优前的基础概念

目前的垃圾处理器中,一类是以吞吐量优先,一类是以响应时间优先:

吞吐量=用户代码执行时间用户代码执行时间+垃圾回收执行时间吞吐量=用户代码执行时间用户代码执行时间+垃圾回收执行时间

响应时间：STW越短，响应时间越好

对吞吐量、响应时间、QPS、并发数相关概念可以参考:[吞吐量（TPS）、QPS、并发数、响应时间（RT）概念](https://www.cnblogs.com/Courage129/p/14386511.html)

所谓调优，首先确定追求什么,是吞吐量? 还是追求响应时间？还是在满足一定的响应时间的情况下，要求达到多大的吞吐量,等等。一般情况下追求吞吐量的有以下领域:科学计算、数据挖掘等。吞吐量优先的垃圾处理器组合一般为：Parallel Scavenge + Parallel Old （PS + PO）。

而追求响应时间的业务有：网站相关 （JDK 1.8之后 G1,之前可以ParNew + CMS + Serial Old）

### 什么是调优？

1. 根据需求进行JVM规划和预调优
2. 优化运行JVM运行环境（慢，卡顿）
3. 解决JVM运行过程中出现的各种问题(OOM)

### 调优之前的规划

- 调优，从业务场景开始，没有业务场景的调优都是耍流氓

- 无监控（压力测试，能看到结果），不调优

- 步骤：

  1. 熟悉业务场景（没有最好的垃圾回收器，只有最合适的垃圾回收器）

     1. 响应时间、停顿时间 [CMS G1 ZGC] （需要给用户作响应）
     2. 吞吐量 = 用户时间 /( 用户时间 + GC时间) [PS+PO]

  2. 选择回收器组合

  3. 计算内存需求（经验值 1.5G 16G）

  4. 选定CPU（越高越好）

  5. 设定年代大小、升级年龄

  6. 设定日志参数

     1. 

        ```
        -Xloggc:/opt/xxx/logs/xxx-xxx-gc-%t.log 
        -XX:+UseGCLogFileRotation 
        -XX:NumberOfGCLogFiles=5 
        -XX:GCLogFileSize=20M 
        -XX:+PrintGCDetails 
        -XX:+PrintGCDateStamps 
        -XX:+PrintGCCause
        ```

        日志参数解释说明:

        > /opt/xxx/logs/xxx-xxx-gc-%t.log 中XXX表示路径,%t表示时间戳,意思是给日志文件添加一个时间标记,如果不添加的话,也就意味着每次虚拟机启动都会使用原来的日志名,那么会被重写。
        >
        > Rotation中文意思是循环、轮流,意味着这个GC日志会循环写
        >
        > GCLogFileSize=20M 指定一个日志大小为20M,太大了不利于分析,太小又会产生过多的日志文件
        >
        > NumberOfGCLogFiles=5 : 指定生成的日志数目
        >
        > PrintGCDateStamps :PrintGCDateStamps会打印具体的时间，而PrintGCTimeStamps
        >
        > ​	主要打印针对JVM启动的时候的相对时间，相对来说前者更消耗内存。

     2. 或者每天产生一个日志文件

  7. 观察日志情况
     日志有分析工具,可视化分析工具有[GCeasy](https://gceasy.io/)和[GCViewer](https://github.com/chewiebug/GCViewer)。

     ### 