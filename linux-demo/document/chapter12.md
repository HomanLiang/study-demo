[toc]



# Linux 内存使用情况分析

## 1.什么是内存

内存(Memory)是计算机的重要部件之一，也称内存储器和主存储器，它**用于暂时存放CPU中的运算数据，与硬盘等外部存储器交换的数据**。

它是外存与CPU进行沟通的桥梁，计算机中所有程序的运行都在内存中进行，内存性能的强弱影响计算机整体发挥的水平。

只要计算机开始运行，操作系统就会把需要运算的数据从内存调到CPU中进行运算，当运算完成，CPU将结果传送出来。

内存的运行也决定计算机整体运行快慢的程度。

### 1.2.Linux内存回收机制

为啥要回收：

- 内核需要为任何时刻突发到来的内存申请提供足够的内存，以便cache的使用和其他相关内存的使用不至于让系统的剩余内存长期处于很少的状态。
- 当真的有大于空闲内存的申请到来的时候，会触发强制内存回收。

内存回收针对的目标有两种，一种是针对zone的，另一种是针对一个memcg的，把针对zone的内存回收方式分为三种，分别是快速内存回收、直接内存回收、kswapd内存回收。

## 2.查看Linux内存情况

### 2.1.查看`/proc/meminfo`

```
[root@test ~]# cat /proc/meminfo
MemTotal:       16166688 kB
MemFree:        14051412 kB
MemAvailable:   14772588 kB
Buffers:            2116 kB
Cached:          1073260 kB
SwapCached:            0 kB
Active:           770384 kB
Inactive:         698264 kB
Active(anon):     450156 kB
Inactive(anon):    76748 kB
Active(file):     320228 kB
Inactive(file):   621516 kB
Unevictable:           0 kB
Mlocked:               0 kB
SwapTotal:      33554428 kB
SwapFree:       33554428 kB
Dirty:               476 kB
Writeback:             0 kB
AnonPages:        393328 kB
Mapped:           153828 kB
Shmem:            133628 kB
Slab:             246448 kB
SReclaimable:     133892 kB
SUnreclaim:       112556 kB
KernelStack:       13472 kB
PageTables:        30496 kB
NFS_Unstable:          0 kB
Bounce:                0 kB
WritebackTmp:          0 kB
CommitLimit:    41637772 kB
Committed_AS:    4257776 kB
VmallocTotal:   34359738367 kB
VmallocUsed:      320696 kB
VmallocChunk:   34350426108 kB
HardwareCorrupted:     0 kB
AnonHugePages:    155648 kB
CmaTotal:              0 kB
CmaFree:               0 kB
HugePages_Total:       0
HugePages_Free:        0
HugePages_Rsvd:        0
HugePages_Surp:        0
Hugepagesize:       2048 kB
DirectMap4k:      279276 kB
DirectMap2M:     6965248 kB
DirectMap1G:    11534336 kB
```

### 2.2.使用`free`命令查看

```
[root@test ~]# free -h
              total        used        free      shared  buff/cache   available
Mem:            15G        874M         13G        130M        1.2G         14G
Swap:           31G          0B         31G
```

参数说明：

- total：总内存大小。
- used：已经使用的内存大小（这里面包含cached和buffers和shared部分）。
- free：空闲的内存大小。
- shared：进程间共享内存（一般不会用，可以忽略）。
- buffers：**内存中写完的东西缓存起来**，这样快速响应请求，后面数据再定期刷到磁盘上。
- cached：**内存中读完缓存起来内容占的大小**（这部分是为了下次查询时快速返回）。
- available：还可以被**应用程序**使用的物理内存大小，和free的区别是，free是真正未被使用的内存，available是包括buffers、cached的。
- Swap：硬盘上交换分区的使用大小。

### 2.3.Buffer和Cache

Cache（缓存），为了调高CPU和内存之间数据交换而设计，Buffer（缓冲）为了提高内存和硬盘（或其他I/O设备的数据交换而设计）。

Cache主要是针对**读操作**设计的，不过Cache概念可能容易混淆，我理解为CPU本身就有Cache，包括一级缓存、二级缓存、三级缓存，我们知道CPU所有的指令操作对接的都是内存，而CPU的处理能力远高于内存速度，所以为了不让CPU资源闲置，Intel等公司在CPU内部集成了一些Cache，但毕竟不能放太多电路在里面，所以这部分Cache并不是很大，主要是用来存放一些常用的指令和常用数据，真正大部分Cache的数据应该是占用内存的空间来缓存请求过的数据，即上面的Cached部分（这部分纯属个人理解，正确与否有待考证）。

Buffer主要是针对**写操作**设计的，更细的说是针对内存和硬盘之间的写操作来设计的，目的是将写的操作集中起来进行，减少磁盘碎片和硬盘反复寻址过程，提高性能。

在Linux系统内部有一个守护进程会定期清空Buffer中的内容，将其写入硬盘内，当手动执行sync命令时也会触发上述操作。

### 2.4.Swap

虽然现在的内存已经变得非常廉价，但是swap仍然有很大的使用价值，合理的规划和使用swap分区，对系统稳定运行至关重要。

Linux下可以使用文件系统中的一个常规文件或者一个独立分区作为交换空间使用。同时linux允许使用多个交换分区或者交换文件。

## 3.内存泄漏和内存溢出

内存溢出（OOM，out of memory），是指程序在申请内存时，没有足够的内存空间供其使用，出现out of memory；比如申请了一个integer，但给它存了long才能存下的数，那就是内存溢出。

内存泄露（memory leak），是指程序在申请内存后，无法释放已申请的内存空间，一次内存泄露危害可以忽略，但内存泄露堆积后果很严重，无论多少内存，迟早会被占光。

### 3.1.如何判断内存泄露

用 `jstat -gcutil PID`，观察Old这个参数，如果每次执行完FULLGC之后Old区的值一次比一次升高，就可以判断为发生了内存泄漏。

### 3.2.如何判断内存溢出

Heap Dump（堆转储文件）它是一个Java进程在某个时间点上的内存快照。Heap Dump是有着多种类型的。不过总体上heap dump在触发快照的时候都保存了java对象和类的信息。通常在写heap dump文件前会触发一次FullGC，所以heap dump文件中保存的是FullGC后留下的对象信息。

通过设置如下的JVM参数，可以在发生OutOfMemoryError后获取到一份HPROF二进制Heap Dump文件：

```
-XX:+HeapDumpOnOutOfMemoryError
```

生成的文件会直接写入到工作目录。

注意：该方法需要JDK5以上版本。

转存堆内存信息后，需要对文件进行分析，从而找到OOM的原因。可以使用以下方式：

mat：eclipse memory analyzer, 基于eclipse RCP的内存分析工具。具体使用参考：http://www.eclipse.org/mat/

jhat：JDK自带的java heap analyze tool，可以将堆中的对象以html的形式显示出来，包括对象的数量，大小等等，并支持对象查询语言OQL，分析相关的应用后，可以通过http://localhost:7000来访问分析结果。不推荐使用。

#### 3.2.1.OOM常见原因及解决方案

可参考[高手总结的9种 OOM 常见原因及解决方案](https://zhuanlan.zhihu.com/p/79355050)

## 4.释放内存

在Linux系统下，我们一般不需要去释放内存，因为系统已经将内存管理的很好。但是凡事也有例外，有的时候内存会被缓存占用掉，导致系统使用SWAP空间影响性能，例如当你在linux下频繁存取文件后,物理内存会很快被用光,当程序结束后，内存不会被正常释放，而是一直作为caching。此时就需 要执行释放内存（清理缓存）的操作了。

释放内存操作：

```
sync  # 强制将内存中的缓存写入磁盘

echo 数字 > /proc/sys/vm/drop_caches #数字可以是0-3的整数
```

数字含义：

- 0：不释放（系统默认值）
- 1：释放页缓存
- 2：释放dentries和inodes
- 3：释放所有缓存