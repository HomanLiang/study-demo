# 直接内存

## 直接内存概述

- 不是虚拟机运行时数据区的一部分，也不是《Java 虚拟机规范》中定义的内存区域。
- 直接内存是在 Java 对外的、直接向系统申请的内存区间
- 来源于 NIO，通过存在堆中的 DirectByteBuffer 操作 Native 内存
- 通常，访问直接内存的速度会优于 Java 堆。即读写性能高。
  - 因此出于性能考虑，读写频繁的场合可能会考虑使用直接内存。
  - Java 的 NIO 库允许 Java 程序使用直接内存，用于数据缓存区



- 读写文件，需要与磁盘交互，需要由用户态切换到内核态。在内核态时，需要内存如右图的操作。

  使用 IO，见右图。这里需要两份内存存储重复数据，效率低。

![image-20210131141246385](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210131141246385.png)



- 使用 NIO 时，如下图。操作系统划出的直接缓存区可以被 Java 代码直接访问，只有一份。NIO 适合对大文件的读写操作。

![image-20210131140750187](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210131140750187.png)

- 也可能导致 `OutOfMemoryError` 异常
- 由于直接内存在 Java 对外，因此它的大小直接受限于 `-Xmx` 指定的最大堆大小，但是系统内存是有限的，Java 堆和直接内存的总和依然受限于操作系统能给出最大内存。
- 缺点：
  - 分配回收成本较高
  - 不受 JVM 内存回收管理
- 直接内存大小可以通过 `MaxDirectMemorySize` 设置
- 如果不指定，默认与堆的最大值 `-Xmx` 参数值一致

![image-20210131141955903](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210131141955903.png)

![image-20210131142024094](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210131142024094.png)









