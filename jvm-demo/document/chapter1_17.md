[toc]



# 垃圾回收器

## GC 分类与性能指标

### 垃圾回收器概述

- 垃圾收集器没有在规范中进行过多的规定，可以由不同的厂商、不同版本的 JVM 来实现。
- 由于 JDK 的版本处于高速迭代过程中，因此 Java 发展至今已经衍生了众多的GC版本。
- 从不同角度分析垃圾收集器，可以将GC分为不同类型。



### 垃圾回收器分类

- 按线程数分，可以分为串行垃圾回收器和并行垃圾回收器。

  ![image-20210213154827109](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213154827109.png)

  - 串行回收指的是在同一时间段内只允许有一个 CPU 用于执行垃圾回收操作，此时工作线程被暂停，知道垃圾收集工作结束。
    - 在诸如单 CPU 处理器或者较小的应用内存等硬件平台不是特别优越的场合，串行回收器的性能表现可以超过并行回收器和并发回收器。所以，串行回收默认被应用在客户端的 Client 模式下的 JVM 中
  - 在并发能力比较强的 CPU 上，并发回收器产生的停顿时间要短于串行回收器
  - 和串行回收相反，并行收集可以运用多个 CPU 同时执行垃圾回收，因此提升了应用的吞吐量，不过并行回收仍然与串行回收一样，采用独占式，使用了“Stop-the-world”机制。

- 按照工作模式分，可以分为并发式垃圾回收器和独占式垃圾回收器。
  - 并发式垃圾回收器与应用程序线程交替工作，以尽可能减少应用程序的停顿时间。
  - 独占式垃圾回收器（Stop the world）一旦运行，就停止应用程序中所有用户线程，直到垃圾回收过程完全结束。

![image-20210213161409256](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213161409256.png)

- 按碎片处理方式分，可分为压缩式垃圾回收器和非压缩式垃圾回收器。
  - 压缩式垃圾回收器会在回收完成后，对存活对象进行压缩整理，消除回收后的碎片。
  - 非压缩式的垃圾回收器不进行这步操作。

- 按工作的内存区间分，又可分为年轻代垃圾回收器和老年代垃圾回收器。



### 评估 GC 的性能指标

- 吞吐量：运行用户代码的时间占总运行时间的比例
  - （总运行时间：程序的运行时间 + 内存回收的时间）
- 垃圾收集开销：吞吐量的补数，垃圾收集所有时间与总运行时间的比例。
- 暂停时间：执行垃圾收集时，程序的工作线程被暂停的时间。
- 收集频率：相对于应用程序的执行，收集操作发生的频率。
- 内存占用：Java 堆区所占的内存大小。
- 快速：一个对象从诞生到被回收所经历的时间。

- 这三者共同构成一个“不可能三角”。三者总体的表现会随着技术进步而越来越好。一款优秀的收集器通常最多同时满足其中的两项。（内存、吞吐量、暂停时间）
- 这三项里，暂停时间的重要性日益凸显。因为随着硬件发展，内存占用多些越来越容忍，硬件性能的提升也有助于降低收集器运行时对应用程序的影响，即提高了吞吐量。而内存的扩大，对延迟反而带来负面效果。
- 简单来说，主要抓住两点：
  - 吞吐量
  - 暂停时间

### 评估 GC 的性能指标：吞吐量（throughput）

- 吞吐量就是 CPU 用于运行用户代码的时间与 CPU 总消耗时间的比值，即吞吐量 = 运行用户代码时间 / (运行用户代码时间 + 垃圾收集时间)
  - 比如：虚拟机总共运行了 100 分钟，其中垃圾收集花掉了 1分钟，那吞吐量就是99%
- 这种情况下，应用程序能容忍较高的暂停时间，因此高吞吐量的应用程序有更长的时间基准，快速响应是不必考虑的。
- 吞吐量优先，意味着在单位时间内，STW 的时间最短：0.2 + 0.2 = 0.4

![image-20210213163442623](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213163442623.png)

### 评估 GC 的性能指标：暂停时间（pause time）

- “暂停时间”是指一个时间段内应用程序线程暂停，让 GC 线程执行的状态
  - 例如：GC 期间 100 毫秒的暂停时间意味着在这100毫米期间内没有应用程序线程是活动的。
- 暂停时间优先，意味着尽可能让单次 STW 的时间最短：0.1 + 0.1 + 0.1 + 0.1 + 0.1 = 0.5

![image-20210213170651624](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213170651624.png)

### 评估 GC 的性能指标：吞吐量 vs 暂停时间

- 高吞吐量较好因为这会让应用程序的最终用户感觉只有应用程序线程在做“生产性”工作。直觉上，吞吐量越高程序运行越快。
- 低暂停时间（低延迟）较好因为从最终用户的角度来看不管是 GC 还是其他原因导致一个应用被挂起始终是不好的。这取决于应用程序的类型，有时候甚至短暂的200毫秒暂停都可能打断终端用户体验。因此，具有低的较大暂停时间是非常重要的，特别是对于一个交互式应用程序。
- 不幸的是“高吞吐量”和“低暂停时间”是一对相互竞争的目标（矛盾）。
  - 因为如果选择以吞吐量优先，那么必然需要降低内存回收的执行频率，但是这样会导致 GC 需要更长的暂停时间来执行内存回收。
  - 相反的，如果选择以低延迟优先为原则，那么为了降低每次执行内存回收时暂停时间，也只能频繁地执行内存回收，但这又引起了年轻代内存的缩减和导致程序吞吐量的下降。

在设计（或使用）GC 算法时，我们必须确定我们的目标：一个 GC 算法只可能针对两个目标之一（即只专注于较大吞吐量或最小暂停时间），或尝试找到一个二者的折衷。

现在标准：在最大吞吐量优先的情况下，降低停顿时间。



## 不同的垃圾回收器概述

垃圾收集机制是 Java 的招牌能力，极大地提高了开发效率。这当然也是面试的热点。

那么，Java 常见的垃圾收集器有哪些？

### 垃圾收集器发展史

![image-20210213233030973](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213233030973.png)



### 7款经典的垃圾收集器

- 串行回收器：Serial、Serial Old
- 并行回收器：ParNew、Parallel Scavenge、Parallel Old
- 并发回收器：CMS、G1



### 经典的垃圾收集器

![image-20210213233538601](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213233538601.png)



### 7款经典收集器与垃圾分代之间的关系

![image-20210213233655199](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213233655199.png)



### 垃圾收集器的组合关系

![image-20210213233738684](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210213233738684.png)

1. 两个收集器间有连线，表明它们可以搭配使用：

   Serial/Serial Old、Serial/CMS、ParNew/Serial Old、ParNew/CMS、Parallel Scavenge/Serial Old、Parallel Scavenge/Parallel Old、G1

2. 其中 Serial Old 作为 CMS 出现”Concurrent Mode Failure“失败的后备预案。

3. （红色虚线）由于维护和兼容性测试的成本，在 JDK 8时将 Serial +CMS、ParNew + Serial Old 这两个组合声明为废弃（JEP 173）并在 JDK 9 中完全取消了这些组合的支持（JEP214)，即：移除。

4. （绿色虚线）JDK 14中：弃用 Parallel Scavenge 和 SerialOld GC 组合（JEP 366）

5. （青色虚线）JDK 14中：删除 CMS 垃圾回收器（JEP 363）



### 不同的垃圾回收器概述

- 为什么要有很多收集器，一个不够吗？因为 Java 的使用场景很多，移动端，服务器等。所以就需要针对不同的场景，提供不同的垃圾收集器，提高垃圾收集的性能。
- 虽然我们会对各个收集器进行比较，但并非为了挑选一个最好的收集器出来。没有一种放之四海皆准、任何场景下都适用的完美收集器存在，更加没有万能的收集器。所以我们选择的只是对具体应用最合适的收集器。

### 如何查看默认的垃圾收集器

- `-XX:+PrintCommandLineFlags`：查看命令行相关参数（包含使用的垃圾收集器）
- 使用命令行指令：`jinfo -flag 相关垃圾回收器参数 进程ID` 





## Serial 回收器：串行回收

- Serial 收集器是最基本、历史最悠久的垃圾收集器了。JDK 1.3 之前回收新生代唯一的选择。
- Serial 收集器作为 HotSpot 中 Client 模式下的默认新生代垃圾收集器。
- Serial 收集器采用复制算法、串行回收和“Stop-the-World”机制的方式执行内存回收。
- 除了年轻代之外，Serial 收集器还提供用于执行老年代垃圾收集的 Serial Old 收集器。Serial Old 收集器同样也采用了串行回收和“Stop-the-World”机制，只不过内存回收算法使用的是标记-压缩算法。
  - Serial Old 是运行在 Client 模式下默认的老年代的垃圾回收器
  - Serial Old 在 Server 模式下主要有两个用途：
    - 与新生代的 Parallel Scavenge 配合使用
    - 作为老年代 CMS 收集器的后备垃圾收集方案

![image-20210214104430470](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210214104430470.png)

这个收集器是一个单线程的收集器，但它的“单线程”的意义并不仅仅说明它只会使用一个 CPU 或一条收集线程去完成垃圾收集工作，更重要的是在它进行垃圾收集时，必须暂停其他所有的工作线程，直到它收集结束（Stop The World）。

- 优势：简单而高效（与其他收集器的单线程比），对于限定单个 CPU 的环境来说，Serial 收集器由于没有线程交互的开销，专心做垃圾收集自然可以获取最高的单线程收集效率。
  - 运行在 Client 模式下的虚拟机是个不错的选择。
- 在用户的桌面应用场景中，可用内存一般不大（几十 MB 至一两百MB），可以在较短时间内完成垃圾收集（几十ms至一百多ms），只要不频繁发生，使用串行回收器也是可以接受的。
- 在 HotSpot 虚拟机中，使用 `-XX:+UseSerialGC` 参数可以指定年轻代和老年代都使用串行收集器。
  - 等价于新生代用 Serial GC，且老年代用 Serial Old GC



### 总结

这种垃圾收集器大家了解，现在已经不用串行的了。而且在限定单核 CPU 才可以用。现在都不是单核的了。

对于交互较强的应用而言，这种垃圾收集器是不能接受的。一般在 Java web 应用程序中是不会采用串行垃圾收集器的。



## ParNew 回收器：并行回收

- 如果说 Serial GC 是年轻代中的单线程垃圾收集器，那么 ParNew 收集器则是 Serial 收集器的多线程版本。
  - Par 是 Parallel 的缩写，New：只能处理的是新生代
- ParNew 收集器除了采用并行回收的方式执行内存回收外，两款垃圾回收器之间几乎没有任何区别。ParNew 收集器在年轻代中同样也是采用复制算法、“Stop-the-World"机制。
- ParNew 是很多 JVM 运行在 Server 模式下新生代的默认垃圾收集器。

![image-20210214214314263](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210214214314263.png)

- 对于新生代，回收次数频繁，使用并行方式高效。
- 对于老年代，回收次数少，使用串行方式节省资源。（CPU 并行需要切换线程，串行可以省去切换线程的资源）

- 由于 ParNew 收集器是基于并行回收，那么是否可以断定 ParNew 收集器的回收率在任何场景下都会比 Serial 收集器更高效？
  - ParNew 收集器运行在多 CPU 的环境下，由于可以充分利用 CPU、多核心等物理硬件资源优势，可以更快速地完成垃圾收集，提升程序的吞吐量。
  - 但是在单个 CPU 的环境下，ParNew 收集器不比 Serial 收集器更高效。虽然 Serial 收集器是基于串行回收，但是由于 CPU 不需要频繁地做任务切换，因此可以有效避免多线程交互过程中产生的一些额外开销。
- 因为除 Serial 外，目前只有 ParNew GC 能与 CMS 收集器配合工作。
- 在程序中，开发人员可以通过选项”`-XX:+UseParNewGC`“手动指定使用 ParNew 收集器执行内存回收任务。它表示年轻代使用并行收集器，不影响老年代。
- `-XX:ParallelGCThreads` 限制线程数量，默认开启和 CPU 数据相同的线程数。



## Parallel 回收器：吞吐量优先

- HotSpot 的年轻代中除了拥有 ParNew 收集器是基于并行回收的以外，Parallel Scavenge 收集器同样也采用了复制算法、并行回收和“Stop-the-world”机制。
- 那么 Parallel 收集器的出现是否多此一举？
  - 和 ParNew 收集器不同，Parallel Scavenge 收集器的目标是达到一个可控制的吞吐量（Throughput），它也被称为吞吐量优先的垃圾收集器。
  - 自适应调节策略也是 Parallel Scavenge 与 ParNew 一个重要区别。

- 高吞吐量则可以高效率地利用 CPU 时间，尽快完成程序的运算任务，主要适合在后台运算而不需要太多交互的任务。因此，常见在服务器环境中使用。例如，那些执行批量处理、订单处理、工资支付、科学计算的应用程序。
- Parallel 收集器在 JDK 1.6 时提供了用于执行老年代垃圾收集器的 Parallel Old 收集器，用来代替老年代的 Serial Old 收集器。
- Parallel Old 收集器采用了标记-压缩算法，但同样也是基于并行回收和“Stop-the-World”机制

![image-20210214222042207](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210214222042207.png)

- 在程序吞吐量优先的应用场景中，Parallel 收集器和 Parallel Old 收集器的组合，在Server模式下的内存回收性能很不错。
- 在 Java 8中，默认是此垃圾收集器。

- 参数配置：
  - `-XX:+UseParallelGC` 手动指定年轻代使用 Parallel 并行收集器执行内存回收任务
  - `-XX:+UserParallelOldGC` 手动指定老年代都是使用并行回收收集器。
    - 分别适用于新生代和老年代。默认JDK8开启的
    - 上面两个参数，默认开启一个，另一个也会被开启（互相激活）
  - `-XX:ParallelGCThreads` 设置年轻代并行收集器的线程数。一般地，最好与CPU数量相等，以避免过多的线程数影响垃圾收集性能。
    - 在默认情况下，当CPU数量小于8个，ParallelGCThreads 的值等于 CPU 数量。
    - 当CPU数量大于8个，ParallelGCThreads 的值等于 3 + [5 * CPU_Count] /8。
  - `-XX:MaxGCPauseMillis` 设置垃圾收集器最大停顿时间（即 STW 的时间）。单位是毫秒。
    - 为了尽可能地把停顿时间控制在 MaxGCPauseMills以内，收集器在工作时会调整 Java 堆大小或者其他一些参数。
    - 对于用户来讲，停顿时间越短体验越好。但是在服务器端，我们注重高并发，整体的吞吐量。所以服务器端适合 Parallel，进行控制。
    - 该参数使用需谨慎。
  - `-XX:GCTimeRatio` 垃圾收集时间占总时间的比例（= 1 / (N + 1))。
    - 取值范围（0,100）。默认值99，也就是垃圾回收时间不超过1%
    - 与前一个 `-XX:MaxGCPauseMillis` 参数有一定矛盾性。暂停时间越长，Radio 参数就容易超过设定的比例。
  - `-XX:+UseAdaptiveSizePolicy` 设置 Parallel Scavenge 收集器具有自适应调节策略
    - 在这种模式下，年轻代的大小、Eden和Survivor的比例、晋升老年代的对象年龄等参数会被自动调整，已达到在堆大小、吞吐量和停顿时间之间的平衡点。
    - 在手动调优比较困难的场合，可以直接使用这种自适应的方式，仅指定虚拟机的最大堆、目标的吞吐量（GCTimeRatio）和停顿时间（MaxGCPauseMills），让虚拟机自己完成调优工作。



## CMS 回收器：低延迟

### CMS 回收器：低延迟

- 在 JDK 1.5 时期，HotSpot 推出一款在强交互应用中几乎可认为有划时代意义的垃圾收集器：CMS（Concurrent-Mark-Sweep）收集器，这款收集器是 HotSpot 虚拟机中第一款真正意义上的并发收集器，它第一次实现了让垃圾收集线程与用户线程同时工作。
- CMS 收集器的关注点是尽可能压缩垃圾收集时用户线程的停顿时间。停顿时间越短（低延迟）就越适合与用户交互的程序，良好的响应速度能提升用户体验。
  - 目前很大一部分的 Java 应用集中在互联网站或者B/S系统的服务器端，这类应用尤其重视服务的响应速度，希望系统停顿时间最短，以给用户带来较好的体验。CMS 收集器就非常符合这类应用的需求。
- CMS 的垃圾收集算法采用标记-清除算法，并且也会“Stop-the-world”

不幸的是，CMS 作为老年代的收集器，却无法与 JDK 1.4.0 中已经存在的新生代收集器 Parallel Scavenge 配合工作，所以在 JDK 1.5中使用 CMS 来收集老年代的时候，新生代只能选择 ParNew 或者 Serial 收集器中的一个。

在 G1 出现之前，CMS使用还是非常广泛的。直到今天，仍然有很多系统使用 CMS GC。



### CMS 的工作原理

![image-20210215125113223](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210215125113223.png)

CMS 整个过程比之前的收集器要复杂，整个过程分为4个主要阶段，即初始化标记阶段、并发标记阶段、重新标记阶段和并发清除阶段。

- 初始标记（Initial-Mark）阶段：在这个阶段中，程序中所有的工作线程都会因为“Stop-the-World”机制而出现短暂的暂停，这个阶段的主要任务仅仅只是标记出 GC Roots 能直接关联到的对象。一旦标记完成之后就会恢复之前被暂停的所有应用线程。由于直接关联对象比较小，所以这里的速度非常快。
- 并发标记（Concurrent-Mark）阶段：从 GC Roots 的直接关联对象开始遍历整个对象图的过程，这个过程耗时较长但是不需要停顿用户线程，可以与垃圾收集线程一起并发运行。
- 重新标记（Remark）阶段：由于在并发标记阶段中，程序的工作线程会和垃圾收集线程同时运行或者交叉运行，因此为了修正并发标记期间，因用户程序继续运作而导致标记产生变动的那一部分对象的标记记录，这个阶段的停顿时间通常会比初始化标记阶段稍长一些，但也远比并发标记阶段的时间短。
- 并发清除（Concurrent-Sweep）阶段：此阶段清理删除掉标记阶段判断的已经死亡的对象，释放内存空间。由于不需要移动存活对象，所以这个阶段也是可以与用户线程同时并发的

尽管 CMS 收集器采用的是并发回收（非独占式），但是在其初始化标记和再次标记这两个阶段中仍然需要执行“Stop-the-World”机制暂停程序中的工作线程，不过暂停时间并不会太长，因此可以说明目前所有垃圾收集器都做不到完全不需要“Stop-the-World”，只是尽可能地缩短暂停时间。

由于最耗费时间的并发标记与并发清除阶段都不需要暂停工作，所以整体的回收是低停顿的。

另外，由于在垃圾收集阶段用户线程没有中断，所以在 CMS 回收过程中，还应该确保应用程序用户线程有足够的内存可用。因此，CMS 收集器不能像其他收集器那样等到老年代几乎完全被填满了再进行收集，而是当堆内存使用率达到某一阈值时，便开始进行回收，以确保应用程序在 CMS 工作过程中依然有足够的空间支持应用程序运行。要是 CMS 运行期间预留的内存无法满足程序需要，就会出现一次“Concurrent Mode Failure”失败，这时虚拟机将启动后备预案：临时启动 Serial Old 收集器来重新进行老年代的垃圾收集，这样停顿时间就很长了。

CMS 收集器的垃圾收集算法采用的是标记-清除算法，这意味着每次执行完内存回收后，由于被执行内存回收的无用对象所占用的内存空间极有可能是不连续的一些内存块，不可避免地将会产生一些内存碎片。那么CMS在为新对象分配内存空间时，将无法使用指针碰撞（Bump the Pointer）技术，而只能够选择空闲列表（Free List）执行内存分配。

![image-20210215172427308](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210215172427308.png)

有人会觉得既然 Mark-Sweep 会造成内存碎片，那么为什么不把算法换成 Mark-Compact 呢？

答案其实很简单，因为当并发清除的时候，用 Compact 整理内存的话，原来的用户线程使用的内存还怎么用呢？要保证用户线程能继续执行，前提的它运行的资源不受影响嘛。Mark-Compact 更适合 “Stop-the-World”这种场景下使用

- CMS 的优点：
  - 并发收集
  - 低延迟
- CMS 的弊端：
  - 会产生内存碎片，导致并发清除后，用户线程可用的空间不足。在无法分配大对象的情况下，不得不提前触发 Full GC。
  - CMS 收集器对 CPU 资源非常敏感。在并发阶段，它虽然不会导致用户停顿，但是会因为占用了一部分线程而导致应用程序变慢，总吞吐量会降低。
  - CMS 收集器无法处理浮动垃圾。可能出现“Concurrent Mode Failure”失败而导致另一次 Full GC 的产生。在并发标记阶段由于程序的工作线程和垃圾收集线程是同时运行或者交叉运行的，那么在并发标记阶段如果产生新的垃圾对象，CMS 将无法对这些垃圾对象进行标记，最终会导致这些新产生的垃圾对象没有被及时回收，从而只能在下一次执行GC时释放这些之前未被回收的内存空间。

### CMS 收集器可以设置的参数

- `-XX:+UseConcMarkSweepGC` 手动指定使用CMS收集器执行内存回收任务。
  
- 开启该参数后会自动将 `-XX:UseParNewGC` 打开。即：ParNew(Young 区用) + CMS(Old 区用) + Serial Old 的组合
  
- `-XX:CMSInitiatingOccupancyFraction` 设置对内存使用率的阈值，一旦达到该阈值，便开始进行回收。
  - JDK 5 及以前版本的默认值为 68，即当老年代的空间使用率达到 68%时，会执行一次 CMS 回收。JDK 6及以上版本默认值为 92%
  - 如果内存增长缓慢，则可以设置一个稍大的值，大的阈值可以有效降低CMS的触发效率，减少老年代回收的次数可以较为明显地改善应用程序性能。反之，如果应用程序内存使用率增长很快，则应该降低这个阀值，以避免频繁触发老年代串行收集器。因此通过该选项便可以有效降低 Full GC 的执行次数。

- `-XX:+UseCMSCompactAtFullCollection` 用于指定在执行完 Full GC 后对内存空间进行压缩整理，以此避免内存碎片的产生。不过由于内存压缩整理过程中无法并发执行，所带来的问题就是停顿时间变得更长了。
- `-XX:CMSFullGCsBeforeCompaction` 设置在执行多少次 Full GC 后对内存空间进行压缩整理
- `-XX:ParallelCMSThreads` 设置 CMS 的线程数量
  
- CMS 默认启动的线程数是（ParallelGCThreads + 3）/ 4，ParallelGCThreads 是年轻代并行收集器的线程数。当CPU资源比较紧张时，受到CMS收集器线程的影响，应用程序的性能在垃圾回收阶段可能会非常糟糕。
  
- 小结：

  HotSpot 有这么多的垃圾回收器，那么如果有人问，Serial GC、Parallel GC、Concurrent Mark Sweep GC这三个GC有什么不同呢？

  请记住下面口令：

  - 如果你想要最小化地使用内存和并行开销，请选 Serial GC；
  - 如果你想要最大化应用程序的吞吐量，请选Parallel GC;
  - 如果你想要最小化GC的中断或停顿时间，请选CMS GC；



### JDK 后续版本中 CMS 的变化

- JDK9新特性：CMS被标记为 Deprecate了（JEP291)
  - 如果对 JDK9 及以上版本的 HotSpot 虚拟机使用参数 `-XX:+UseConcMarkSweepGC` 来开启 CMS 收集器的话，用户会收到一个警告信息，提示未来将会被废弃
- JDK14 新特性：删除 CMS 垃圾回收器（JEP 363)
  - 移除了 CMS 垃圾收集器，如果在 JDK14中使用 `-XX:+UseConcMarkSweepGC` 的话，JVM不会报错，只是给出一个warning，但是不会exit。JVM会自动回退以默认GC方式启动JVM

![image-20210215184905349](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210215184905349.png)



## G1 回收器：区域化分代

### G1 回收器：区域化分代式

- 既然我们已经有了前面几个强大的GC，为什么还要发布 Garbage First（G1）GC？

  原因就在于应用程序所应对的业务越来越庞大、复杂，用户越来越多，没有GC就不能保证应用程序正常进行，而经常造成STW的GC又跟不上实际的需求，所以才会不断地尝试对GC进行优化。G1（Garbage-First）垃圾回收器是在 Java7 Update 4 之后引入的一个新的垃圾回收器，是当今收集器技术发展的最前沿成果之一。

  与此同时，为了适应现在不断扩大的内存和不断增加的处理器数量，进一步降低暂停时间（pause time），同时兼顾良好的吞吐量。

  官方给G1设定的目标是在延迟可控的情况下获得尽可能高的吞吐量，所以才担当起“全功能收集器”的重任与期望。

- 为什么名字叫做 Garbage First（G1）呢？

  因为 G1 是一个并行回收器，它把堆内存分割为很多不相关的区域（Region）（物理上不连续的）。使用不同的Region 来表示 Eden、幸存者0区、幸存者1区、老年代等。

  G1 GC 有计划地避免在整个 Java 堆中进行全区域的垃圾收集。G1 跟踪各个 Region 里面的垃圾堆积的价值大小（回收所获得的空间大小以及回收所需时间的经验值），在后台维护一个优先列表，每次根据允许的收集时间，优先回收价值最大的Region。

  由于这种方式的侧重点在于回收垃圾最大量的区间（Region），所以我们给 G1 一个名字：垃圾优先（Garbage First）

- G1(Garbage-First)是一款面向服务端应用的垃圾收集器，主要针对配备多核 CPU 及大容量内存的机器，以极高概率满足 GC 停顿时间的同时，还兼具高吞吐量的性能特征。
- 在 JDK1.7 版本正式启用，移除了 Experimental 的标识，是 JDK 9以后的默认垃圾回收器，取代了 CMS 回收器以及 Parallel + Parallel Old 组合。被 Oracle 官方称为 “全功能的垃圾收集器”。
- 与此同时，CMS 已经在 JDK 9中被标记为废弃（Deprecated）。在JDK8中还不是默认的垃圾回收器，需要使用 `-XX:+UseG1GC` 来启用。

### G1 回收器的特点（优势）

与其他 GC 收集器相比，G1 使用了全新的分区算法，其特点如下所示：

- 并行和并发
  - 并行性：G1 在回收期间，可以有多个 GC 线程同时工作，有效利用多核计算能力。此时用户线程 STW
  - 并发性：G1 拥有与应用程序交替执行的能力，部分工作可以和应用程序同时执行，因此，一般来说，不会整个回收阶段发生完全阻塞应用程序的情况。
- 分代收集
  - 从分代上看，G1依然属于分代型垃圾回收器，它会区分年轻代和老年代，年轻代依然有Eden区和Survivor区。但从堆的结构上看，它不要求整个Eden区、年轻代或者老年代都是连续的，也不再坚持固定大小和固定数量。
  - 将堆空间分为若干区域（Region），这些区域中包含了逻辑上的年轻代和老年代。
  - 和之前的各类回收器不同，它同时兼顾年轻代和老年代。对比其他回收器，或者工作在年轻代，或者工作在老年代；

![image-20210215225854414](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210215225854414.png)

![image-20210215225905214](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210215225905214.png)

- 空间整合
  - CMS：“标记-清除“算法、内存碎片、若干次GC后进行一次碎片整理
  - G1 将内存划分为一个个的Region。内存回收是以Region作为基本单位的。Region之间是复制算法，但整体上实际可看作是标记-压缩（Mark-Compact）算法，两种算法都可以避免内存碎片。这种特性有利于程序长时间运行，分配大对象时不会因为无法找到连续内存空间而提前触发下一次 GC。尤其是当 Java 堆非常大的时候，G1的优势更加明显。

- 可预测的停顿时间模型（即：软实时soft real-time）

  这是 G1 相对于 CMS 的另一大优势，G1 除了追求低停顿外，还能建立可预测的停顿时间模型，能让使用者明确指定在一个长度为 M 毫秒的时间片段内，消耗在垃圾收集上的时间不得超过 N 毫秒。

  - 由于分区的原因，G1 可以只选取部分区域进行内存回收，这样缩小了回收的范围，因此对于全局停顿情况的发生也能得到较好的控制。
  - G1 跟踪各个 Region 里面的垃圾堆积的价值大小（回收所获得的空间大小以及回收所需时间的经验值），在后台维护一个优先列表，每次根据允许的收集时间，优先回收价值最大的Region。保证了 G1 收集器在有限的时间内可以获取尽可能高的收集效率。
  - 相比于 CMS GC，G1未必能做到CMS在最好的情况下的延时停顿，但是最差的情况要好很多。

### G1回收器的缺点

相较于CMS，G1还不具备全方位、压倒性优势。比如在用户程序运行过程中，G1 无论是为了垃圾收集产生的内存占用（Footprint）还是程序运行时的额外执行负载（Overload）都要比CMS要高。

从经验上来说，在小内存应用上CMS的表现大概率会优于G1，而G1在大内存应用上则发挥其优势。平衡点在6-8GB之间。

### G1回收器的参数设置

- `-XX:+UseG1GC` 手动指定使用G1收集器执行内存回收任务
- `-XX:G1HeapRegionSize` 设置每个Region的大小。值是2的幂，范围是1MB到32MB之间，目标是根据最小的Java堆大小划分出约2048个区域。默认是堆内存的1/2000。
- `-XX:MaxGCPauseMillis` 设置期待达到的最大GC停顿时间指标（JVM会尽力实现，但不保证达到）。默认值是200ms

- `-XX:ParallelGCThread` 设置 STW 工作线程数的值。最多设置为8
- `-XX:ConcGCThreads` 设置并发标记的线程数。将 n 设置为并行垃圾回收线程数（ParallelGCThreads）的1/4左右。
- `-XX:InitiatingHeapOccupancyPercent` 设置触发并发GC周期的Java堆占用率阈值。超过此值，就触发GC。默认值是45.

### G1回收器的常见操作步骤

G1的设计原则就是简化 JVM 性能调优，开发人员只需要简单的三步即可完成调优：

1. 开启G1垃圾收集器
2. 设置堆的最大内存
3. 设置最大的停顿时间

G1中提供了三种垃圾回收模式：YoungGC、Mixed GC 和 Full GC，在不同的条件下被触发。



### G1 回收器的适用场景

- 面向服务器端应用，针对具有大内存、多处理器的机器（在普通大小的堆里表现并不惊喜）

- 最主要的应用是需要低GC延迟，并具有大堆的应用程序提供解决方案；

- 如：在堆大小约6GB或更大时，可预测的暂停时间可以低于0.5秒：（G1通过每次只清理一部分而不是全部的Region的增量式清理来保证每次GC停顿时间不会过长）。

- 用来替换掉 JDK1.5 中的CMS收集器；

  在下面的情况时，使用G1可能比CMS好：

  - 超过 50% 的 Java 堆被活动数据占用；
  - 对象分配频率或年代提升频率变化很大；
  - GC停顿时间过长（长于0.5至1秒）

- HotSpot 垃圾收集器里，除了 G1 以外，其他的垃圾收集器使用内置的JVM线程执行GC的多线程操作，而G1 GC可以采用应用线程承担后台运行的GC工作，即当JVM的GC线程处理速度慢时，系统会调用应用程序线程帮助加速垃圾回收过程。



### 分区Region：化零为整

使用 G1 收集器时，它将整个 Java 堆划分成约2048个大小相同的独立Region块，每个Region块大小根据堆空间的实际大小而定，整体被控制在1MB到32MB之间，且为2的N次幂，即1MB，2MB，4MB，8MB，16MB，32MB。可以通过 `-XX:G1HeapRegionSize` 设定。所有的Region大小相同，且在 JVM 生命周期内不会被改变。

虽然还保留有新生代和老年代的概念，但新生代和老年代不再是物理隔离的了，它们都是一部分Region（不需要连续）的集合。通过Region的动态分配方式实现逻辑上的连续。

![image-20210216104239744](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210216104239744.png)

- 一个 Region 有可能属于 Eden，Survivor 或者 Old/Tenured 内存区域。但是一个Region只可能属于一个角色。图中的E表示该Region属于Eden内存区域，S表示属于Survivor内存区域，O表示属于Old内存区域。图中空白的表示未使用的内存空间。
- G1 垃圾收集器还增加了一种新的内存区域，叫做 Humongous 内存区域，如图中的 H块。主要用于存储大对象，如果超过1.5个Region，就放到H

- 设置H的原因：

  对于堆中的大对象，默认直接会被分配到老年代，但是如果它是一个短期存在的大对象，就会对垃圾收集器造成负面影响。为了解决这个问题，G1划分了一个 Humongous 区，它用来专门存放大对象。如果一个 H 区装不下一个大对象，那么G1会寻找连续的H区来存储。为了能找到连续的H区，有时候不得不启动 Full GC。G1的大多数行为都把H区作为老年代的一部分来看待。

![image-20210216105000770](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210216105000770.png)



### G1回收器垃圾回收过程

G1 GC 的垃圾回收过程主要包括如下三个环节：

- 年轻代GC（Young GC）
- 老年代并发标记过程（Concurrent Marking）
- 混合回收（Mixed GC）
- 如果需要，单线程、独占式、高强度的Full GC还是继续存在的。它针对GC的评估失败提供了一种失败保护机制，即强力回收。

![image-20210216212433549](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210216212433549.png)

顺时针，Young GC --> Young GC + Concurrent Mark --> Mixed GC 顺序，进行垃圾回收。

应用程序分配内存，当年轻代的 Eden 区用尽时开始年轻代回收过程；G1 的年轻代收集阶段是一个并行的独占式收集器。在年轻代回收期，G1 GC暂停所有应用程序线程，启动多线程执行年轻代回收。然后从年轻代区间移动存活对象到Survivor区间或者老年区间，也有可能是两个区间都会涉及。

当堆内存使用达到一定值（默认45%）时，开始老年代并发标记过程。

标记完成马上开始混合回收过程。对于一个混合回收期，G1 GC 从老年区间移动存活对象到空闲区间，这些空闲区间也就成为了老年代的一部分。和年轻代不同，老年代的G1回收器和其他GC不同，G1的老年代回收器不需要整个老年代被回收，一次只需要扫描/回收一小部分老年代的Region就可以了。同时，这个老年代Region是和年轻代一起被回收的。

举个例子：一个Web服务器，Java 进程最大堆内存为4G，每分钟响应1500个请求，每45秒钟会新分配大约2G的内存。G1会每45秒钟进行一次年轻代回收，每31个小时整个堆的使用率会达到45%，会开始老年代并发标记过程，标记完成后开始四到五次的混合回收。



### G1回收器垃圾回收过程：Remembered Set

问题：

- 一个对象被不同区域引用的问题
- 一个Region不可能是孤立的，一个Region中的对象可能被其他任意Region中对象引用，判断对象存活时，是否需要扫描整个Java堆才能保证准确？
- 在其他的分代收集器，也存在这样的问题（而G1更突出）
- 回收新生代也不得不同时扫描老年代？
- 这样的话会降低Minor GC的效率；

解决方法：

- 无论G1还是其他分代收集器，JVM都是使用 Remembered Set 来避免全局扫描
- 每个Region都一个对应的Remembered Set
- 每次Reference类型数据写操作时，都会产生一个Write Barrier暂时中断操作
- 然后检查将要写入的引用指向的对象是否和该Reference类型数据在不同的Region（其他收集器：检查老年代对象是否引用了新生代对象）
- 如果不同，通过CardTable把相关引用信息记录到引用指向对象的所在Region对应的Remembered Set中
- 当进行垃圾收集时，在GC根节点的枚举范围加入Remembered Set；就可以保证不进行全局扫描，也不会有遗漏。

![image-20210216215735135](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210216215735135.png)

### G1 回收过程一：年轻代GC

JVM 启动时，G1先准备好Eden区，程序在运行过程中不断创建对象到Eden区，当Eden空间耗尽时，G1会启动一次年轻代垃圾回收过程。

年轻代垃圾回收只会回收Eden区和Survivor区。

首先G1停止应用程序的执行（Stop-The-World），G1创建回收集（Collection Set），回收集是指需要被回收的内存分段的集合，年轻代回收过程的回收集包含年轻代Eden区和Survivor区所有的内存分段。

![image-20210216215838247](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210216215838247.png)

然后开始如下回收过程：

- 第一阶段，扫描根

  根是指static变量指向的对象，正在执行的方法调用链条上的局部变量等。根引用连同RSet记录的外部引用作为扫描存活对象的入口

- 第二阶段，更新RSet

  处理 dirty card queue 中的 card，更新RSet。此阶段完成后，RSet可以准确的反映老年代对所在的内存分段中对象的引用。

- 第三阶段，处理RSet

  识别被老年代对象指向的Eden中的对象，这些被指向的Eden中对象被认为是存活的对象。

- 第四阶段，复制对象

  此阶段，对象树被遍历，Eden区内存段中存活的对象会被复制到Survivor区中空的内存分段，Survivor区内存段中存活的对象如果年龄未达阈值，年龄会加1，达到阈值会被复制到Old区中空的内存分段。如果Survivor空间不够，Eden空间的部分数据会直接晋升到老年代空间。

- 第五阶段，处理引用

  处理 Soft、Weak、Phantom、Final、JNI Weak等引用。最终 Eden 空间的数据为空，GC 停止工作，而目标内存中的对象都是连续存储的，没有碎片，所以复制过程可以达到内存整理的效果，减少碎片。

对于应用程序的引用赋值语句 object.field=object，JVM会在之前和之后执行特殊的操作以在 dirty card queue 中入队一个保存了对象引用信息的card。在年轻代回收的时候，G1会对Dirty Card Queue中所有的card进行处理，以更新RSet，保证RSet实时准确的反映引用关系。

那为什么不在引用赋值语句处直接更新RSet呢？这是为了性能的需要，RSet的处理需要线程同步，开销会很大，使用队列性能会好很多。

### G1 回收过程二：并发标记过程

1. 初始标记阶段：标记从根节点直接可达的对象。这个阶段是STW的，并且会触发一次年轻代GC。
2. 根区域扫描（Root Region Scanning）：G1 GC 扫描 Survivor 区直接可达的老年代区域对象，并标记被引用的对象。这一过程必须在Young GC之前完成。
3. 并发标记（Concurrent Marking）：在整个堆中进行并发标记（和应用程序并发执行），此过程可能被Young GC中断。在并发标记阶段，若发现区域对象中的所有对象都是垃圾，那这个区域会被立即回收。同时，并发标记过程中，会计算每个区域的对象活性（区域中存活对象的比例）
4. 再次标记（Remark）：由于应用程序持续进行，需要修改上一次的标记结果。是STW的。G1中采用了比CMS更快的初始快照算法：snapshot-at-the-beginning(SATB)
5. 独占清理（cleanup，STW）：计算各个区域的存活对象和GC回收比例，并进行排序，识别可以混合回收的区域。为下阶段做铺垫。是STW的。
   - 这个阶段并不会实际上去做垃圾的收集
6. 并发清理阶段：识别并清理完全空闲的区域。



### G1 回收过程三：混合回收

当越来越多的对象晋升到老年代Old Region时，为了避免堆内存被耗尽，虚拟机会触发一个混合的垃圾收集器，即Mixed GC，该算法并不是一个Old GC，除了回收整个Young Region，还会回收一部分的Old Region。这里需要注意：是一部分老年代，而不是全部老年代。可以选择哪些Old Region 进行收集，从而可以对垃圾回收的耗时时间进行控制。也要注意的是Mixed GC并不是Full GC。

![image-20210217000315382](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217000315382.png)

- 并发标记结束以后，老年代中百分百为垃圾的内存分段被回收了，部分为垃圾的内存分段被计算了出来。默认情况下，这些老年代的内存分段会分8次（可以通过`-XX:G1MixedGCCountTarget`设置）被回收
- 混合回收的回收集（Collection Set）包括八分之一的老年代内存分段，Eden区内存分段，Survivor区内存分段。混合回收的算法和年轻代回收的算法完全一样，只是回收集多了老年代的内存分段。具体过程参考上面的年轻代回收过程。
- 由于老年代中的内存分段默认分8次回收，G1会优先回收垃圾多的内存分段。垃圾占内存分段比例越高的，越会被先回收。并且有一个阈值会决定内存分段是否被回收，`-XX:G1MixedGCLiveThresholdPercent`，默认为65%，意思是垃圾占内存分段比例要达到65%才会被回收。如果垃圾占比太低，意味着存活的对象占比高，在复制的时候会花费更多的时间。
- 混合回收并一定要进行8次。有一个阈值`-XX:G1HeapWastePercent`，默认值为10%，意思是允许整个堆内存中有10%的空间被浪费，意味着如果发现可以回收的垃圾占堆内存的比例低于10%，则不再进行混合回收。因此GC会花费很多的时间但是回收到的内存却很少。



### G1 回收过程四：Full GC

G1的初衷就是要避免Full GC的出现。但是如果上述方式不能正常工作，G1会停止应用程序的执行（Stop-The-World），使用单线程的内存回收算法进行垃圾回收，性能会非常差，应用程序停顿时间会很长。

要避免Full GC的发生，一旦发生需要进行调整。什么时候会发生Full GC呢？比如堆内存太小，当G1在复制存活对象的时候没有空的内存分段可用，则会回退到Full GC，这种情况可以通过增大内存解决。

导致G1 Full GC的原因可能有两个：

1. Evacuation 的时候没有足够的to-space来存放晋升的对象
2. 并发处理过程完成之前空间耗尽。



### G1回收过程：补充

从Oracle官方透漏出来的信息可获知，回收阶段（Evacuation）其实本也有想过设计成与用户程序一起并发执行，但这件事做起来比较复杂，考虑到G1只是回收一部分Region，停顿时间是用户可控制的，所以并不迫切去实现，而选择把这个特性放到了G1之后出现的低延迟垃圾收集器（ZGC）中。另外，还考虑到G1不仅仅面向低延迟，停顿用户线程能够最大幅度提高垃圾收集效率，为了保证吞吐量所以才选择了完全暂停用户线程的实现方案。



### G1回收器优化建议

- 年轻代大小
  - 避免使用`-Xmn`或`-XX:NewRatio`等相关选项显示设置年轻代大小
  - 固定年轻代的大小会覆盖暂停时间目标
- 暂停时间目标不要太过严苛
  - G1 GC的吞吐量目标是90%的应用时间和10%的垃圾回收时间
  - 评估G1 GC的吞吐量时，暂停时间目标不要太严苛。目标太过严苛表示你愿意承受更多的垃圾回收开销，而这些会直接影响到吞吐量。



## 垃圾回收器总结

### 7种经典垃圾回收器总结

截止JDK 1.8，一共有7款不同的垃圾收集器。每一款不同的垃圾收集器都有不同的特点，在具体使用的时候，需要根据具体的情况选用不同的垃圾收集器。

![image-20210217113030589](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217113030589.png)

GC发展阶段：Serial->Parallel（并行）->CMS（并发）->G1->ZGC



### 垃圾回收器组合

不同厂商、不同版本的虚拟机实现差别很大。HotSpot 虚拟机在 JDK7/8后所有收集器及组合（连线），如下图：

![image-20210217113531420](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217113531420.png)

1. 两个收集器间有连线，表明它们可以搭配使用：Serial/Serial Old、Serial/CMS、ParNew/Serial Old、ParNew/CMS、Parallel Scavenge/Serial Old、Parallel Scavenge/Parallel Old、G1;
2. 其中 Serial Old作为CMS出现“Concurrent Mode Failure”失败的后备预案。
3. （红色虚线）由于维护和兼容性测试的成本，在JDK 8时将Serial+CMS、ParNew+Serial Old这两个组合声明为Deprecated（JEP 173），并在JDK 9中完全取消了这些组合的支持（JEP214），即：移除。
4. （绿色虚线）JDK 14中：弃用Parallel Scavenge和Serial Old GC组合（JEP 366）
5. （青色虚线）JDK 14中：删除CMS垃圾回收器（JEP 363）



### 怎么选择垃圾回收器？

- Java 垃圾收集器的配置对于JVM优化来说是一个重要的选择，选择合适的垃圾收集器可以让JVM的性能有一个很大的提升。
- 怎么选择垃圾收集器？
  - 优先调整堆的大小让JVM自适应完成
  - 如果内存小于100M，使用串行收集器
  - 如果是单核、单机程序，并且没有停顿时间的要求，串行收集器
  - 如果是多CPU、需要高吞吐量、允许停顿时间超过1秒，选择并行或者JVM自己选择
  - 如果是多CPU、追求低停顿时间，需快速响应（比如延迟不能超过1秒，如互联网应用），使用并发收集器。官方推荐G1，性能高。现在互联网的项目，基本都是使用G1.
- 最后需要明确一个观点：
  - 没有最好的收集器，更没有万能的收集器；
  - 调优永远是针对特定场景、特定需求，不存在一劳永逸的收集器。

### 面试

- 对于垃圾收集，面试官可以循序渐进从理论、实践各种角度深入，也未必是要求面试者什么都懂。但如果你懂得原理，一定会成为面试中的加分项。这里较通用、基础性的部分如下：
  - 垃圾收集的算法有哪些？如果判断一个对象是否可以回收？
  - 垃圾收集器工作的基本流程
- 另外，大家需要多关注垃圾回收器这一章的各种常用的参数。



## GC日志分析

通过阅读GC日志，我们可以了解Java虚拟机内存分配与回收策略。

内存分配与垃圾回收的参数列表

`-XX:+PrintGC`：输出GC日志。类似：-verbose:gc

`-XX:+PrintGCDetails`：输出GC的详细日志

`-XX:+PrintGCTimeStamps`：输出GC的时间戳（以基准时间的形式）

`-XX:+PrintGCDateStamps`：输出GC的时间戳（以日期的形式，如2013-05-04T21:53:59.234+0800）

`-XX:PrintHeapAtGC`：在进行GC的前后打印出堆的信息

`-Xloggc:../logs/gc.log：`日志文件的输出路径



### GC日志分析--案例1

- 打开GC日志：

  ```
  -verbose:gc
  ```

  

- 这个只会显示总的GC堆的变化，如下：

![image-20210217123801917](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217123801917.png)

- 参数解析：

  ![image-20210217123856607](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217123856607.png)



### GC日志分析--案例2

- 打开GC日志：

  ```
  -verbose:gc -XX:+PrintGCDetails
  ```

  

- 输出信息如下：

  ![image-20210217124343421](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217124343421.png)

- 参数解析：

  ![image-20210217124418973](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217124418973.png)



### GC日志分析--案例3

- 打开GC日志：

  ```
  -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:PrintGCDateStamps
  ```

  

- 输出信息如下：（说明：带上了日期和时间）

  ![image-20210217124554060](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217124554060.png)

### GC日志分析--案例4

- 如果想把GC日志存到文件的话，是下面这个参数：

  ```
  -Xloggc:/path/to/gc.log
  ```

  

### 日志补充说明

- Allocation Failure

- [PSYoungGen: 5986K->696K(8704K)] 5986K->704K(9216K)

  中括号内：GC 回收前年轻代大小，回收后大小，（年轻代总大小）

  括号外：GC回收年轻代和老年代大小，回收后大小，（年轻代和老年代总大小）

- user 代表用户态回收耗时，sys 内核态回收耗时， real 实际耗时。由于多核的原因，时间总和可能会超过real时间

![image-20210217140148467](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217140148467.png)



### Minor GC 日志

![image-20210217140236599](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217140236599.png)



### Full GC日志

![image-20210217140329340](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217140329340.png)



### GC日志分析--案例5

![image-20210217140413506](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217140413506.png)

![image-20210217140426457](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217140426457.png)

![image-20210217140446150](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217140446150.png)



### GC日志分析工具

可以用一些工具去分析这些GC日志

常用的日志分析工具有：GCViewer、[GCEasy](https://gceasy.io/)、GCHisto、GCLogViewer、Hpjmeter、garbagecat等。





## 垃圾回收器的新发展

### 垃圾回收器的新发展

![image-20210217153724879](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217153724879.png)

### JDK 11 新特性

![image-20210217153739481](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217153739481.png)

### OpenJDK 12 的 Shenandoah GC

- 现在 G1 回收器已成为默认回收器好几年了。
- 我们还看到了引入了两个新的收集器：ZGC（JDK11出现）和Shennadoah(OpenJDK 12)
  - 主打特点：低停顿时间

![image-20210217154034278](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154034278.png)

![image-20210217154100501](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154100501.png)

![image-20210217154121723](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154121723.png)



### 令人震惊、革命性的ZGC

![image-20210217154200326](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154200326.png)

![image-20210217154213558](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154213558.png)

![image-20210217154224758](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154224758.png)

![image-20210217154237306](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154237306.png)

![image-20210217154253605](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154253605.png)

![image-20210217154305705](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154305705.png)

### 其它垃圾回收器：AliGC

![image-20210217154330772](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154330772.png)

![image-20210217154349162](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210217154349162.png)



