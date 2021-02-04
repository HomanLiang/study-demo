[toc]



# 垃圾回收概述

## 什么是垃圾

![image-20210203220822624](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210203220822624.png)

- 垃圾收集，不是 Java 语言的伴生产物。早在1960年，第一门开始使用内存动态分配和垃圾收集技术的 Lisp 语言诞生。
- 关于垃圾收集有三个经典问题：
  - 哪些内存需要回收？
  - 什么时候回收？
  - 如何回收？
- 垃圾收集机制是 Java 的招牌能力，极大地提高了开发效率。如今，垃圾收集几乎成为现代语言的标配，即使经过如何长时间的发展，Java 的垃圾收集机制仍然在不断的演进中，不同大小的设备、不同特征的应用场景，对垃圾收集提出了新的挑战，这当然也是面试的热点。

- 什么是垃圾（Garbage）呢？
  - 垃圾是指在运行程序中没有任何指针指向的对象，这个对象就是需要被回收的垃圾。
- 如果不及时对内存中的垃圾进行清理，那么这些垃圾对象所占的内存空间会一直保留到应用程序结束，被保留的空间无法被其他对象使用。甚至可能导致内存溢出。

### 磁盘碎片整理的日子

![image-20210203223803669](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210203223803669.png)

### 大厂面试题

- 蚂蚁金服：
  - 你知道哪几种垃圾回收器，各自的优缺点，重点讲一下 CMS 和 G1
  - 一面：JVM GC 算法有哪些，目前的 JDK 版本采用什么回收算法
  - 一面：G1 回收器讲下回收过程
  - GC 是什么？为什么要有GC?
  - 一面：GC 的两种判定方法？CMS收集器与G1收集器的特点
- 百度：
  - 说一下 GC 算法，分代回收说下
  - 垃圾收集策略和算法
- 天猫：
  - 一面：JVM GC 原理，JVM 怎么回收内存
  - 一面：CMS 特点，垃圾回收算法有哪些？各自的优缺点，他们共同的缺点是什么？
- 滴滴：
  - 一面：Java 的垃圾回收器都有那些，说下 G1 的应用场景，平时你是如何搭配使用

- 京东：
  - 你知道哪几种垃圾收集器，各自的优缺点，终点讲下CMS和G1，包括原理、流程、优缺点。垃圾回收算法的实现原理。
- 阿里
  - 讲一讲垃圾回收算法
  - 什么情况下触发垃圾回收
  - 如何选择合适的垃圾收集算法？
  - JVM 有那三种垃圾回收器？
- 字节跳动：
  - 常见的垃圾回收器算法有哪些，各有什么优劣？
  - system.gc() 和 runtime.gc() 会做什么事情？
  - 一面：Java GC机制？GC Roots 有哪些？
  - 二面：Java 对象的回收方式，回收算法。
  - CMS 和 G1 了解么，CMS 解决什么问题，说一下回收的过程。
  - CMS 回收停顿了几次，为什么要停顿两次。



## 为什么需要 GC

想要学习 GC，首先需要理解为什么需要GC？

- 对于高级语言来说，一个基本认知是如果不进行垃圾回收，内存迟早都会被消耗完，因为不断地分配内存空间而不进行回收，就好像不停地生产生活垃圾而从来不打扫一样。
- 除了释放没用的对象，垃圾回收也可以清除内存里的记录碎片。碎片整理将所占用的堆内存移到堆的一端，以便 JVM 将整理出的内存分配给新的对象。
- 随着应用程序所应付的业务越来越庞大、复杂，用户越来越多，没有GC就不能保证应用程序的正常使用。而经常造成STW的GC又跟不上实际的需求，所以才会不断地尝试对GC进行优化。





## 早期垃圾回收

- 在早期的C/C++时代，垃圾回收基本上是手工进行的。开发人员可以使用 new 关键字进行内存申请，并使用 delete 关键字进行内存释放。比如以下代码：

  ![image-20210203235154627](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210203235154627.png)

- 这种方式可以灵活控制内存释放的时间，但是会给开发人员带来频繁申请和释放内存的管理负担。倘若有一处内存区间由于程序员编码的问题忘记被回收，那么就会产生内存泄漏，垃圾对象永久无法被清除，随着系统运行时间的不断增长，垃圾对象所耗内存可能持续上升，知道出现内存溢出并造成应用程序崩溃。

- 在有了垃圾回收机制后，上述代码块有可能变成这样：

  ![image-20210203235246743](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210203235246743.png)

- 现在，除了 Java 以外，C#、Python、Ruby 等语言都使用了自动垃圾回收思想，也是未来发展趋势。可以说，这种自动化的内存分配和垃圾回收的方式已经成为现代开发语言必备的标准。





## Java 垃圾回收机制

- 自动内存管理，无需开发人员手动参与内存的分配和回收，这样降低内存泄漏和内存溢出的风险
  - 没有垃圾回收器，Java 也会和 cpp 一样，各种悬垂指针，野指针，泄漏问题让你头疼不已。
- 自动内存管理机制，将程序员从繁重的内存管理中释放出来，可以更专心地专注于业务开发

### 担忧

- 对于 Java 开发人员而言，自动内存管理就像是一个黑匣子，如果过度依赖于“自动”，那么这将是一场灾难，最严重的就会弱化 Java 开发人员在程序出现内存溢出时定位问题和解决问题的能力。
- 此时，了解 JVM 的自动内存分配和内存回收原理就显得非常重要，只有真正了解 JVM 是如何管理内存后，我们才能够在遇见 OutOfMemoryError 时，快速地根据错误异常日志定位问题和解决问题。
- 当需要排查各种内存溢出、内存泄漏问题时，当垃圾收集成为系统达到更高并发量的瓶颈时，我们就必须对这些“自动化”的技术实施必要的监控和调节。

### 应该关心哪些区域的回收？

![image-20210204000041386](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210204000041386.png)

- 垃圾回收器可以对年轻代回收，也可以对老年代回收，甚至是全堆和方法区的回收。
  - 其中，Java 堆是垃圾收集器的工作重点。
- 从次数上讲：
  - 频繁收集 Young 区
  - 较少收集 Old 区
  - 基本不动 Perm 区










