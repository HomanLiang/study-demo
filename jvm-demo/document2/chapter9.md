[toc]



# Java 应用故障诊断

## 1. 故障定位思路

Java 应用出现线上故障，如何进行诊断？

我们在定位线上问题时要有一个整体的思路，顺藤摸瓜，才能较快的找到问题原因。

一般来说，服务器故障诊断的整体思路如下：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303330393138313634352e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122834.png)

应用故障诊断思路：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303330393138313833312e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122847.png)



## 2. CPU 问题

- **CPU 使用率过高**：往往是由于程序逻辑问题导致的。常见导致 CPU 飙升的问题场景如：死循环，无限递归、频繁 GC、线程上下文切换过多。

- **CPU 始终升不上去**：往往是由于程序中存在大量 IO 操作并且时间很长（数据库读写、日志等）。

### 2.1.查找 CPU 占用率较高的进程、线程

线上环境的 Java 应用可能有多个进程、线程，所以，要先找到 CPU 占用率较高的进程、线程。

（1）使用 `ps` 命令查看 xxx 应用的进程 ID（PID）

```
ps -ef | grep xxx
```

也可以使用 `jps` 命令来查看。

（2）如果应用有多个进程，可以用 `top` 命令查看哪个占用 CPU 较高。

（3）用 `top -Hp pid` 来找到 CPU 使用率比较高的一些线程。

（4）将占用 CPU 最高的 PID 转换为 16 进制，使用 `printf '%x\n' pid` 得到 `nid`

（5）使用 `jstack pic | grep 'nid' -C5` 命令，查看堆栈信息：

```
$ jstack 7129 | grep '0x1c23' -C5
        at java.lang.Object.wait(Object.java:502)
        at java.lang.ref.Reference.tryHandlePending(Reference.java:191)
        - locked <0x00000000b5383ff0> (a java.lang.ref.Reference$Lock)
        at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:153)

"main" #1 prio=5 os_prio=0 tid=0x00007f4df400a800 nid=0x1c23 in Object.wait() [0x00007f4dfdec8000]
   java.lang.Thread.State: WAITING (on object monitor)
        at java.lang.Object.wait(Native Method)
        - waiting on <0x00000000b5384018> (a org.apache.felix.framework.util.ThreadGate)
        at org.apache.felix.framework.util.ThreadGate.await(ThreadGate.java:79)
        - locked <0x00000000b5384018> (a org.apache.felix.framework.util.ThreadGate)
```

（6）更常见的操作是用 `jstack` 生成堆栈快照，然后基于快照文件进行分析。生成快照命令：

```
jstack -F -l pid >> threaddump.log
```

（7）分析堆栈信息

一般来说，状态为 `WAITING`、`TIMED_WAITING` 、`BLOCKED` 的线程更可能出现问题。可以执行以下命令查看线程状态统计：

```
cat threaddump.log | grep "java.lang.Thread.State" | sort -nr | uniq -c
```

如果存在大量 `WAITING`、`TIMED_WAITING` 、`BLOCKED` ，那么多半是有问题啦。

### 2.2.是否存在频繁 GC

如果应用频繁 GC，也可能导致 CPU 飙升。为何频繁 GC 可以使用 `jstack` 来分析问题（分析和解决频繁 GC 问题，在后续讲解）。

那么，如何判断 Java 进程 GC 是否频繁？

可以使用 `jstat -gc pid 1000` 命令来观察 GC 状态。

```
$ jstat -gc 29527 200 5
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       MC     MU    CCSC   CCSU   YGC     YGCT    FGC    FGCT     GCT
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
22528.0 22016.0  0.0   21388.2 4106752.0 921244.7 5592576.0  2086826.5  110716.0 103441.1 12416.0 11167.7   3189   90.057  10      2.140   92.197
```

### 2.3.是否存在频繁上下文切换

针对频繁上下文切换问题，可以使用 `vmstat pid` 命令来进行查看。

```
$ vmstat 7129
procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
 r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
 1  0   6836 737532   1588 3504956    0    0     1     4    5    3  0  0 100  0  0
```

其中，`cs` 一列代表了上下文切换的次数。

【解决方法】

如果，线程上下文切换很频繁，可以考虑在应用中针对线程进行优化，方法有：

- **无锁并发**：多线程竞争时，会引起上下文切换，所以多线程处理数据时，可以用一些办法来避免使用锁，如将数据的 ID 按照 Hash 取模分段，不同的线程处理不同段的数据；
- **CAS 算法**：Java 的 Atomic 包使用 CAS 算法来更新数据，而不需要加锁；
- **最少线程**：避免创建不需要的线程，比如任务很少，但是创建了很多线程来处理，这样会造成大量线程都处于等待状态；
- **使用协程**：在单线程里实现多任务的调度，并在单线程里维持多个任务间的切换；

## 3.内存问题

内存问题诊断起来相对比 CPU 麻烦一些，场景也比较多。主要包括 OOM、GC 问题和堆外内存。一般来讲，我们会先用 `free` 命令先来检查一发内存的各种情况。

诊断内存问题，一般首先会用 `free` 命令查看一下机器的物理内存使用情况。

```
$ free
              total        used        free      shared  buff/cache   available
Mem:        8011164     3767900      735364        8804     3507900     3898568
Swap:       5242876        6836     5236040
```

## 4.磁盘问题

### 4.1.查看磁盘空间使用率

可以使用 `df -hl` 命令查看磁盘空间使用率。

```
$ df -hl
Filesystem      Size  Used Avail Use% Mounted on
devtmpfs        494M     0  494M   0% /dev
tmpfs           504M     0  504M   0% /dev/shm
tmpfs           504M   58M  447M  12% /run
tmpfs           504M     0  504M   0% /sys/fs/cgroup
/dev/sda2        20G  5.7G   13G  31% /
/dev/sda1       380M  142M  218M  40% /boot
tmpfs           101M     0  101M   0% /run/user/0
```

### 4.2.查看磁盘读写性能

可以使用 `iostat` 命令查看磁盘读写性能。

```
iostat -d -k -x
Linux 3.10.0-327.el7.x86_64 (elk-server)        03/07/2020      _x86_64_        (4 CPU)

Device:         rrqm/s   wrqm/s     r/s     w/s    rkB/s    wkB/s avgrq-sz avgqu-sz   await r_await w_await  svctm  %util
sda               0.00     0.14    0.01    1.63     0.42   157.56   193.02     0.00    2.52   11.43    2.48   0.60   0.10
scd0              0.00     0.00    0.00    0.00     0.00     0.00     8.00     0.00    0.27    0.27    0.00   0.27   0.00
dm-0              0.00     0.00    0.01    1.78     0.41   157.56   177.19     0.00    2.46   12.09    2.42   0.59   0.10
dm-1              0.00     0.00    0.00    0.00     0.00     0.00    16.95     0.00    1.04    1.04    0.00   1.02   0.00
```

### 4.3.查看具体的文件读写情况

可以使用 `lsof -p pid` 命令

## 5.网络问题

### 5.1.无法连接

可以通过 `ping` 命令，查看是否能连通。

通过 `netstat -nlp | grep <port>` 命令，查看服务端口是否在工作。

### 5.2.网络超时

网络超时问题大部分出在应用层面。超时大体可以分为连接超时和读写超时，某些使用连接池的客户端框架还会存在获取连接超时和空闲连接清理超时。

- 读写超时。readTimeout/writeTimeout，有些框架叫做 so_timeout 或者 socketTimeout，均指的是数据读写超时。注意这边的超时大部分是指逻辑上的超时。soa 的超时指的也是读超时。读写超时一般都只针对客户端设置。
- 连接超时。connectionTimeout，客户端通常指与服务端建立连接的最大时间。服务端这边 connectionTimeout 就有些五花八门了，jetty 中表示空闲连接清理时间，tomcat 则表示连接维持的最大时间。
- 其他。包括连接获取超时 connectionAcquireTimeout 和空闲连接清理超时 idleConnectionTimeout。多用于使用连接池或队列的客户端或服务端框架。

我们在设置各种超时时间中，需要确认的是尽量保持客户端的超时小于服务端的超时，以保证连接正常结束。

在实际开发中，我们关心最多的应该是接口的读写超时了。

如何设置合理的接口超时是一个问题。如果接口超时设置的过长，那么有可能会过多地占用服务端的 tcp 连接。而如果接口设置的过短，那么接口超时就会非常频繁。

服务端接口明明 rt 降低，但客户端仍然一直超时又是另一个问题。这个问题其实很简单，客户端到服务端的链路包括网络传输、排队以及服务处理等，每一个环节都可能是耗时的原因。

### 5.3.TCP 队列溢出

tcp 队列溢出是个相对底层的错误，它可能会造成超时、rst 等更表层的错误。因此错误也更隐蔽，所以我们单独说一说。

![68747470733a2f2f66726564616c2d626c6f672e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f323031392d31312d30342d3038333832372e6a7067](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122903.jpg)

如上图所示，这里有两个队列：syns queue(半连接队列）、accept queue（全连接队列）。三次握手，在 server 收到 client 的 syn 后，把消息放到 syns queue，回复 syn+ack 给 client，server 收到 client 的 ack，如果这时 accept queue 没满，那就从 syns queue 拿出暂存的信息放入 accept queue 中，否则按 tcp_abort_on_overflow 指示的执行。

tcp_abort_on_overflow 0 表示如果三次握手第三步的时候 accept queue 满了那么 server 扔掉 client 发过来的 ack。tcp_abort_on_overflow 1 则表示第三步的时候如果全连接队列满了，server 发送一个 rst 包给 client，表示废掉这个握手过程和这个连接，意味着日志里可能会有很多`connection reset / connection reset by peer`。

那么在实际开发中，我们怎么能快速定位到 tcp 队列溢出呢？

**netstat 命令，执行 netstat -s | egrep "listen|LISTEN"** 

![68747470733a2f2f66726564616c2d626c6f672e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f323031392d31312d30342d38333832382e6a7067](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122913.jpg)

如上图所示，overflowed 表示全连接队列溢出的次数，sockets dropped 表示半连接队列溢出的次数。

**ss 命令，执行 ss -lnt** 

![68747470733a2f2f66726564616c2d626c6f672e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f323031392d31312d30342d3038333832382e6a7067](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122925.jpg)

上面看到 Send-Q 表示第三列的 listen 端口上的全连接队列最大为 5，第一列 Recv-Q 为全连接队列当前使用了多少。

接着我们看看怎么设置全连接、半连接队列大小吧：

全连接队列的大小取决于 min(backlog, somaxconn)。backlog 是在 socket 创建的时候传入的，somaxconn 是一个 os 级别的系统参数。而半连接队列的大小取决于 max(64, /proc/sys/net/ipv4/tcp_max_syn_backlog)。

在日常开发中，我们往往使用 servlet 容器作为服务端，所以我们有时候也需要关注容器的连接队列大小。在 tomcat 中 backlog 叫做`acceptCount`，在 jetty 里面则是`acceptQueueSize`。

### 5.4.RST 异常

RST 包表示连接重置，用于关闭一些无用的连接，通常表示异常关闭，区别于四次挥手。

在实际开发中，我们往往会看到`connection reset / connection reset by peer`错误，这种情况就是 RST 包导致的。

**端口不存在**

如果像不存在的端口发出建立连接 SYN 请求，那么服务端发现自己并没有这个端口则会直接返回一个 RST 报文，用于中断连接。

**主动代替 FIN 终止连接**

一般来说，正常的连接关闭都是需要通过 FIN 报文实现，然而我们也可以用 RST 报文来代替 FIN，表示直接终止连接。实际开发中，可设置 SO_LINGER 数值来控制，这种往往是故意的，来跳过 TIMED_WAIT，提供交互效率，不闲就慎用。

**客户端或服务端有一边发生了异常，该方向对端发送 RST 以告知关闭连接**

我们上面讲的 tcp 队列溢出发送 RST 包其实也是属于这一种。这种往往是由于某些原因，一方无法再能正常处理请求连接了(比如程序崩了，队列满了)，从而告知另一方关闭连接。

**接收到的 TCP 报文不在已知的 TCP 连接内**

比如，一方机器由于网络实在太差 TCP 报文失踪了，另一方关闭了该连接，然后过了许久收到了之前失踪的 TCP 报文，但由于对应的 TCP 连接已不存在，那么会直接发一个 RST 包以便开启新的连接。

**一方长期未收到另一方的确认报文，在一定时间或重传次数后发出 RST 报文**

这种大多也和网络环境相关了，网络环境差可能会导致更多的 RST 报文。

之前说过 RST 报文多会导致程序报错，在一个已关闭的连接上读操作会报`connection reset`，而在一个已关闭的连接上写操作则会报`connection reset by peer`。通常我们可能还会看到`broken pipe`错误，这是管道层面的错误，表示对已关闭的管道进行读写，往往是在收到 RST，报出`connection reset`错后继续读写数据报的错，这个在 glibc 源码注释中也有介绍。

我们在诊断故障时候怎么确定有 RST 包的存在呢？当然是使用 tcpdump 命令进行抓包，并使用 wireshark 进行简单分析了。`tcpdump -i en0 tcp -w xxx.cap`，en0 表示监听的网卡。

![68747470733a2f2f66726564616c2d626c6f672e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f323031392d31312d30342d3038333832392e6a7067](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122937.jpg)

接下来我们通过 wireshark 打开抓到的包，可能就能看到如下图所示，红色的就表示 RST 包了。

![68747470733a2f2f66726564616c2d626c6f672e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f323031392d31312d30342d3038333833302e6a7067](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122946.jpg)

### 5.5.TIME_WAIT 和 CLOSE_WAIT

TIME_WAIT 和 CLOSE_WAIT 是啥意思相信大家都知道。 在线上时，我们可以直接用命令`netstat -n | awk '/^tcp/ {++S[$NF]} END {for(a in S) print a, S[a]}'`来查看 time-wait 和 close_wait 的数量

用 ss 命令会更快`ss -ant | awk '{++S[$1]} END {for(a in S) print a, S[a]}'`

![68747470733a2f2f66726564616c2d626c6f672e6f73732d636e2d68616e677a686f752e616c6979756e63732e636f6d2f323031392d31312d30342d3038333833302e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327122954.png)

**TIME_WAIT**

time_wait 的存在一是为了丢失的数据包被后面连接复用，二是为了在 2MSL 的时间范围内正常关闭连接。它的存在其实会大大减少 RST 包的出现。

过多的 time_wait 在短连接频繁的场景比较容易出现。这种情况可以在服务端做一些内核参数调优:

```
#表示开启重用。允许将TIME-WAIT sockets重新用于新的TCP连接，默认为0，表示关闭
net.ipv4.tcp_tw_reuse = 1
#表示开启TCP连接中TIME-WAIT sockets的快速回收，默认为0，表示关闭
net.ipv4.tcp_tw_recycle = 1
```

当然我们不要忘记在 NAT 环境下因为时间戳错乱导致数据包被拒绝的坑了，另外的办法就是改小`tcp_max_tw_buckets`，超过这个数的 time_wait 都会被干掉，不过这也会导致报`time wait bucket table overflow`的错。

**CLOSE_WAIT**

close_wait 往往都是因为应用程序写的有问题，没有在 ACK 后再次发起 FIN 报文。close_wait 出现的概率甚至比 time_wait 要更高，后果也更严重。往往是由于某个地方阻塞住了，没有正常关闭连接，从而渐渐地消耗完所有的线程。

想要定位这类问题，最好是通过 jstack 来分析线程堆栈来诊断问题，具体可参考上述章节。这里仅举一个例子。

开发同学说应用上线后 CLOSE_WAIT 就一直增多，直到挂掉为止，jstack 后找到比较可疑的堆栈是大部分线程都卡在了`countdownlatch.await`方法，找开发同学了解后得知使用了多线程但是确没有 catch 异常，修改后发现异常仅仅是最简单的升级 sdk 后常出现的`class not found`。

## 6.GC 问题

GC 问题除了影响 CPU 也会影响内存，诊断思路也是一致的。

（1）通常，先使用 `jstat` 来查看分代变化情况，比如 **minor gc** 或 **full gc** 次数是不是太频繁、耗时太久。

线程量太大，且不被及时 GC 也会引发 OOM，大部分就是之前说的 `unable to create new native thread`。除了 jstack 细细分析 dump 文件外，我们一般先会看下总体线程。

可以执行以下命令中任意一个，没来查看当前进程创建的总线程数。

```
pstreee -p pid | wc -l
ls -l /proc/pid/task | wc -l
```

堆内内存泄漏总是和 GC 异常相伴。不过 GC 问题不只是和内存问题相关，还有可能引起 CPU 负载、网络问题等系列并发症，只是相对来说和内存联系紧密些，所以我们在此单独总结一下 GC 相关问题。

我们在 cpu 章介绍了使用 jstat 来获取当前 GC 分代变化信息。而更多时候，我们是通过 GC 日志来诊断问题的，在启动参数中加上`-verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps`来开启 GC 日志。 常见的 Minor GC、Full GC 日志含义在此就不做赘述了。

针对 gc 日志，我们就能大致推断出 Minor GC 与 fullGC 是否过于频繁或者耗时过长，从而对症下药。我们下面将对 G1 垃圾收集器来做分析，这边也建议大家使用 G1`-XX:+UseG1GC`。

### 6.1.OOM

查看 GC 日志，如果有明显提示 OOM 问题，那就可以根据提示信息，较为快速的定位问题。

### 6.2.Minor GC

**Minor GC 过频**

Minor GC 频繁一般是短周期的 Java 小对象较多。

（1）先考虑是不是 Eden 区/新生代设置的太小了，看能否通过调整 `-Xmn、-XX:SurvivorRatio` 等参数设置来解决问题。

（2）如果参数正常，但是 Minor GC 频率还是太高，就需要使用 `jmap` 和 `MAT` 对 dump 文件进行进一步诊断了。

**Minor GC 耗时过长**

Minor GC 耗时过长问题就要看 GC 日志里耗时耗在哪一块了。

以 G1 GC 日志为例，可以关注 Root Scanning、Object Copy、Ref Proc 等阶段。Ref Proc 耗时长，就要注意引用相关的对象。Root Scanning 耗时长，就要注意线程数、跨代引用。Object Copy 则需要关注对象生存周期。而且耗时分析它需要横向比较，就是和其他项目或者正常时间段的耗时比较。

### 6.3.Full GC 过频

G1 中更多的还是 mixedGC，但 mixedGC 可以和 Minor GC 思路一样去诊断。触发 fullGC 了一般都会有问题，G1 会退化使用 Serial 收集器来完成垃圾的清理工作，暂停时长达到秒级别，可以说是半跪了。

fullGC 的原因可能包括以下这些，以及参数调整方面的一些思路：

- 并发阶段失败：在并发标记阶段，MixGC 之前老年代就被填满了，那么这时候 G1 就会放弃标记周期。这种情况，可能就需要增加堆大小，或者调整并发标记线程数`-XX:ConcGCThreads`。
- 晋升失败：在 GC 的时候没有足够的内存供存活/晋升对象使用，所以触发了 Full GC。这时候可以通过`-XX:G1ReservePercent`来增加预留内存百分比，减少`-XX:InitiatingHeapOccupancyPercent`来提前启动标记，`-XX:ConcGCThreads`来增加标记线程数也是可以的。
- 大对象分配失败：大对象找不到合适的 region 空间进行分配，就会进行 fullGC，这种情况下可以增大内存或者增大`-XX:G1HeapRegionSize`。
- 程序主动执行 System.gc()：不要随便写就对了。

另外，我们可以在启动参数中配置`-XX:HeapDumpPath=/xxx/dump.hprof`来 dump fullGC 相关的文件，并通过 jinfo 来进行 gc 前后的 dump

```
jinfo -flag +HeapDumpBeforeFullGC pid
jinfo -flag +HeapDumpAfterFullGC pid
```

这样得到 2 份 dump 文件，对比后主要关注被 gc 掉的问题对象来定位问题。

## 7.常用 Linux 命令

在故障排查时，有一些 Linux 命令十分有用，建议掌握。

### 7.1.top

top 命令可以实时动态地查看系统的整体运行情况，是一个综合了多方信息监测系统性能和运行信息的实用工具。

通常，会使用 `top -Hp pid` 查看具体线程使用系统资源情况。

> 命令详情参考：http://man.linuxde.net/top

### 7.2.vmstat

vmstat 是一款指定采样周期和次数的功能性监测工具，我们可以看到，它不仅可以统计内存的使用情况，还可以观测到 CPU 的使用率、swap 的使用情况。但 vmstat 一般很少用来查看内存的使用情况，而是经常被用来观察进程的上下文切换。

- r：等待运行的进程数；
- b：处于非中断睡眠状态的进程数；
- swpd：虚拟内存使用情况；
- free：空闲的内存；
- buff：用来作为缓冲的内存数；
- si：从磁盘交换到内存的交换页数量；
- so：从内存交换到磁盘的交换页数量；
- bi：发送到块设备的块数；
- bo：从块设备接收到的块数；
- in：每秒中断数；
- cs：每秒上下文切换次数；
- us：用户 CPU 使用时间；
- sy：内核 CPU 系统使用时间；
- id：空闲时间；
- wa：等待 I/O 时间；
- st：运行虚拟机窃取的时间。





## 案例

### 1.一次 Java 内存泄漏的排查

#### 1.1.问题

**网络问题？**

晚上七点多开始，我就开始不停地收到报警邮件，邮件显示探测的几个接口有超时情况。 多数执行栈都在：

```
java.io.BufferedReader.readLine(BufferedReader.java:371)
java.io.BufferedReader.readLine(BufferReader.java:389)
java_io_BufferedReader$readLine.call(Unknown Source)
com.domain.detect.http.HttpClient.getResponse(HttpClient.groovy:122)
com.domain.detect.http.HttpClient.this$2$getResponse(HttpClient.groovy)
```

这个线程栈的报错我见得多了，我们设置的 HTTP DNS 超时是 1s， connect 超时是 2s， read 超时是 3s，这种报错都是探测服务正常发送了 HTTP 请求，服务器也在收到请求正常处理后正常响应了，但数据包在网络层层转发中丢失了，所以请求线程的执行栈会停留在获取接口响应的地方。这种情况的典型特征就是能在服务器上查找到对应的日志记录。而且日志会显示服务器响应完全正常。 与它相对的还有线程栈停留在 Socket connect 处的，这是在建连时就失败了，服务端完全无感知。

我注意到其中一个接口报错更频繁一些，这个接口需要上传一个 4M 的文件到服务器，然后经过一连串的业务逻辑处理，再返回 2M 的文本数据，而其他的接口则是简单的业务逻辑，我猜测可能是需要上传下载的数据太多，所以超时导致丢包的概率也更大吧。

根据这个猜想，群登上服务器，使用请求的 request_id 在近期服务日志中搜索一下，果不其然，就是网络丢包问题导致的接口超时了。

当然这样 leader 是不会满意的，这个结论还得有人接锅才行。于是赶紧联系运维和网络组，向他们确认一下当时的网络状态。网络组同学回复说是我们探测服务所在机房的交换机老旧，存在未知的转发瓶颈，正在优化，这让我更放心了，于是在部门群里简单交待一下，算是完成任务。

**问题爆发**

本以为这次值班就起这么一个小波浪，结果在晚上八点多，各种接口的报警邮件蜂拥而至，打得准备收拾东西过周日单休的我措手不及。

这次几乎所有的接口都在超时，而我们那个大量网络 I/O 的接口则是每次探测必超时，难道是整个机房故障了么。

我再次通过服务器和监控看到各个接口的指标都很正常，自己测试了下接口也完全 OK，既然不影响线上服务，我准备先通过探测服务的接口把探测任务停掉再慢慢排查。

结果给暂停探测任务的接口发请求好久也没有响应，这时候我才知道没这么简单。

#### 1.2.解决

**内存泄漏**

于是赶快登陆探测服务器，首先是 `top free df` 三连，结果还真发现了些异常。

![detect_cpu_exception](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327220418.png)

我们的探测进程 CPU 占用率特别高，达到了 900%。

我们的 Java 进程，并不做大量 CPU 运算，正常情况下，CPU 应该在 100~200% 之间，出现这种 CPU 飙升的情况，要么走到了死循环，要么就是在做大量的 GC。

使用 `jstat -gc pid [interval]` 命令查看了 java 进程的 GC 状态，果然，FULL GC 达到了每秒一次。

![jstat](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327220447.png)

这么多的 FULL GC，应该是内存泄漏没跑了，于是 使用 `jstack pid > jstack.log` 保存了线程栈的现场，使用 `jmap -dump:format=b,file=heap.log pid` 保存了堆现场，然后重启了探测服务，报警邮件终于停止了。

**jstat**

jstat 是一个非常强大的 JVM 监控工具，一般用法是： `jstat [-options] pid interval`

它支持的查看项有：

- -class 查看类加载信息
- -compile 编译统计信息
- -gc 垃圾回收信息
- -gcXXX 各区域 GC 的详细信息 如 -gcold

使用它，对定位 JVM 的内存问题很有帮助。

#### 1.3.排查

问题虽然解决了，但为了防止它再次发生，还是要把根源揪出来。

**分析栈**

栈的分析很简单，看一下线程数是不是过多，多数栈都在干嘛。

```
> grep 'java.lang.Thread.State' jstack.log  | wc -l
> 464
```

才四百多线程，并无异常。

```
> grep -A 1 'java.lang.Thread.State' jstack.log  | grep -v 'java.lang.Thread.State' | sort | uniq -c |sort -n

     10 	at java.lang.Class.forName0(Native Method)
     10 	at java.lang.Object.wait(Native Method)
     16 	at java.lang.ClassLoader.loadClass(ClassLoader.java:404)
     44 	at sun.nio.ch.EPollArrayWrapper.epollWait(Native Method)
    344 	at sun.misc.Unsafe.park(Native Method)
```

线程状态好像也无异常，接下来分析堆文件。

**下载堆 dump 文件**

堆文件都是一些二进制数据，在命令行查看非常麻烦，Java 为我们提供的工具都是可视化的，Linux 服务器上又没法查看，那么首先要把文件下载到本地。

由于我们设置的堆内存为 4G，所以 dump 出来的堆文件也很大，下载它确实非常费事，不过我们可以先对它进行一次压缩。

`gzip` 是个功能很强大的压缩命令，特别是我们可以设置 `-1 ~ -9` 来指定它的压缩级别，数据越大压缩比率越大，耗时也就越长，推荐使用 -6~7， -9 实在是太慢了，且收益不大，有这个压缩的时间，多出来的文件也下载好了。

**使用 MAT 分析 jvm heap**

MAT 是分析 Java 堆内存的利器，使用它打开我们的堆文件（将文件后缀改为 `.hprof`）, 它会提示我们要分析的种类，对于这次分析，果断选择 `memory leak suspect`。

![heap_pie](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327220554.png)

从上面的饼图中可以看出，绝大多数堆内存都被同一个内存占用了，再查看堆内存详情，向上层追溯，很快就发现了罪魁祸首。

![heap_object](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327220601.png)

**分析代码**

找到内存泄漏的对象了，在项目里全局搜索对象名，它是一个 Bean 对象，然后定位到它的一个类型为 Map 的属性。

这个 Map 根据类型用 ArrayList 存储了每次探测接口响应的结果，每次探测完都塞到 ArrayList 里去分析，由于 Bean 对象不会被回收，这个属性又没有清除逻辑，所以在服务十来天没有上线重启的情况下，这个 Map 越来越大，直至将内存占满。

内存满了之后，无法再给 HTTP 响应结果分配内存了，所以一直卡在 readLine 那。而我们那个大量 I/O 的接口报警次数特别多，估计跟响应太大需要更多内存有关。

给代码 owner 提了 PR，问题圆满解决。

#### 1.4.小结

------

其实还是要反省一下自己的，一开始报警邮件里还有这样的线程栈：

```
groovy.json.internal.JsonParserCharArray.decodeValueInternal(JsonParserCharArray.java:166)
groovy.json.internal.JsonParserCharArray.decodeJsonObject(JsonParserCharArray.java:132)
groovy.json.internal.JsonParserCharArray.decodeValueInternal(JsonParserCharArray.java:186)
groovy.json.internal.JsonParserCharArray.decodeJsonObject(JsonParserCharArray.java:132)
groovy.json.internal.JsonParserCharArray.decodeValueInternal(JsonParserCharArray.java:186)
```

看到这种报错线程栈却没有细想，要知道 TCP 是能保证消息完整性的，况且消息没有接收完也不会把值赋给变量，这种很明显的是内部错误，如果留意后细查是能提前查出问题所在的，查问题真是差了哪一环都不行啊。



### 2.一次频繁Full GC的排查过程

**问题描述**

最近公司的线上监控系统给我推送了一些kafka lag持续增长的消息，我上生产环境去看了相应的consumer的情况，发现几台机器虽然还在处理消息，但是速度明显慢了很多。

**问题猜测与验证**

我猜测是JVM频繁做Full GC，导致进程也跟着频繁卡顿，处理消息的速度自然就慢了。为了验证这个想法，先用jstat看看内存使用情况：

```
jstat -gcutil 1 1000 #1是进程号
```

![20171003135116485](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003410.jpg)

结果如我所料，几乎1秒钟就要做一次FGC，能安安静静的做个正常的consumer才有鬼了。

赶紧留了一台consumer拿来做分析，把别的几台consumer都重启。不管怎样，先恢复消费能力再说！

**内存泄露root cause排查**

1秒一次FGC，那肯定是发生内存泄露了。

二话不说，把堆dump下来先！

> jmap -F -dump:format=b,file=heapDump 1 #1是进程号

生成的heapDump文件有将近2个G的大小，这么大个文件，为了不影响生产环境的机器，还是scp到本地进行分析吧！

jhat了一下，直接卡在那里不动了。没办法，祭出VisualVM来帮忙。导入文件之后，发现有一大堆HashMap的Node在那占着：

![20171003140911997](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003521.png)

然而并不知道这是个啥，点进去看看内容，发现有一大堆node的key类型是X509CertImpl：

![20171003141144019](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003539.jpg)

这时候我意识到，问题可能出在网络连接上面。但是还是没法定位到具体的代码。

没办法，接着向上找线索。不断地通过OQL查询Referrers:

![20171003145051366](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003601.png)

接着查询：

![20171003145103379](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003621.png)

接着查询：

![20171003145135289](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003656.png)

这时候看到了连接池的踪迹，感觉离真相不远了！ 

![20171003145225339](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003713.png)

![20171003145251284](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003754.png)

到了这里，我心里大概知道了答案：问题一定出在阿里云OSS身上。再结合这张图：

![20171003145916056](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003817.jpg)

就可以猜出是因为使用了OSS的客户端，但是没有正确的释放资源，导致client被回收时，它所创建的资源因为还有别的referrer, 却没有被回收。

再去oss github上的sample一看，果然有这么一段：

![20171003150711600](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003830.png)

而这个shutdown方法做的正是释放Idle资源的事儿：

```
public void shutdown() {
        IdleConnectionReaper.removeConnectionManager(this.connectionManager);
        this.connectionManager.shutdown();
 }
```

**问题修复**

知道了原因，修复也是很轻松的事儿。 在创建client的缓存里加个removeListener，用来主动调用client.shutdown()， 美滋滋：

![20171003151517089](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328003915.jpg)

### 3.排查FGC问题的实践指南

**3.1. 清楚从程序角度，有哪些原因导致FGC？** 

- 大对象：系统一次性加载了过多数据到内存中（比如SQL查询未做分页），导致大对象进入了老年代。
- 内存泄漏：频繁创建了大量对象，但是无法被回收（比如IO对象使用完后未调用close方法释放资源），先引发FGC，最后导致OOM.
- 程序频繁生成一些长生命周期的对象，当这些对象的存活年龄超过分代年龄时便会进入老年代，最后引发FGC. （即本文中的案例）
- 程序BUG导致动态生成了很多新类，使得 Metaspace 不断被占用，先引发FGC，最后导致OOM.
- 代码中显式调用了gc方法，包括自己的代码甚至框架中的代码。
- JVM参数设置问题：包括总内存大小、新生代和老年代的大小、Eden区和S区的大小、元空间大小、垃圾回收算法等等。

**3.2. 清楚排查问题时能使用哪些工具**

- 公司的监控系统：大部分公司都会有，可全方位监控JVM的各项指标。

- JDK的自带工具，包括jmap、jstat等常用命令：

  ```
  # 查看堆内存各区域的使用率以及GC情况
  jstat -gcutil -h20 pid 1000
  
  # 查看堆内存中的存活对象，并按空间排序
  jmap -histo pid | head -n20
  
  # dump堆内存文件
  jmap -dump:format=b,file=heap pid
  ```

- 可视化的堆内存分析工具：JVisualVM、MAT等

**3.3. 排查指南**

- 查看监控，以了解出现问题的时间点以及当前FGC的频率（可对比正常情况看频率是否正常）
- 了解该时间点之前有没有程序上线、基础组件升级等情况。
- 了解JVM的参数设置，包括：堆空间各个区域的大小设置，新生代和老年代分别采用了哪些垃圾收集器，然后分析JVM参数设置是否合理。
- 再对步骤1中列出的可能原因做排除法，其中元空间被打满、内存泄漏、代码显式调用gc方法比较容易排查。
- 针对大对象或者长生命周期对象导致的FGC，可通过 jmap -histo 命令并结合dump堆内存文件作进一步分析，需要先定位到可疑对象。
- 通过可疑对象定位到具体代码再次分析，这时候要结合GC原理和JVM参数设置，弄清楚可疑对象是否满足了进入到老年代的条件才能下结论。



### 4.JVM 常见线上问题 → CPU 100%、内存泄露 问题排查

**4.1.前言**

后文会从 Windows、Linux 两个系统来做示例展示，有人会有疑问了：为什么要说 Windows 版的 ？ 目前市面上还是有很多 Windows 服务器的，应用于传统行业、政府结构、医疗行业 等等；两个系统下的情况都演示下，有备无患

后文中用到了两个工具：[Processor Explorer](https://docs.microsoft.com/en-us/sysinternals/downloads/process-explorer)、[MAT](https://www.eclipse.org/mat/downloads.php)，它们是什么，有什么用，怎么用，本文不做介绍，不知道的小伙伴最好先去做下功课

**4.2.cpu 100%**

下面的示例中， cpu 的占有率没到 100%，只是比较高，但是排查方式是一样的，希望大家不要钻牛角尖

**4.2.1.Windows**

1、找到 cpu 占有率最高的 java 进程号

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328163611.png)

PID： 20260 

2、根据进程号找到 cpu 占有率最高的线程号

双击刚刚找到的 java 进程

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328163627.gif)

线程号： 15900 ，转成十六进制： 3e1c 

3、利用 jstack 生成虚拟机中所有线程的快照

命令： jstack -l {pid} > {path} 　

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328163710.gif)

文件路径： D:\20260.stack 

4、线程快照分析

我们先浏览下快照内容

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328163726.gif)

内容还算比较简洁，线程快照格式都是统一的，我们以一个线程快照简单说明下

```
"main" #1 prio=5 os_prio=0 tid=0x0000000002792800 nid=0x3e1c runnable [0x00000000025cf000] 
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328163802.png)

我们前面找到占 cpu 最高的线程号： 15900 ，十六进制： 3e1c ，用 3e1c 去快照文件里面搜一下

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210328163349.png)

自此，找到问题

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201004110311029-1529704197.png)

**4.2.2.Linux**

排查方式与 Windows 版一样，只是命令有些区别

1、找到 cpu 占有率最高的 java 进程号

使用命令： top -c 显示运行中的进程列表信息， shift + p 使列表按 cpu 使用率排序显示

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005155118246-1241601173.png)

PID = 2227 的进程，cpu 使用率最高

2、根据进程号找到 cpu 占有率最高的线程号

使用命令： top -Hp {pid} ，同样 shift + p 可按 cpu 使用率对线程列表进行排序

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005155250572-2089729235.png)

PID = 2228 的线程消耗 cpu 最高，十进制的 2228 转成十六进制 8b4 

3、利用 jstack 生成虚拟机中所有线程的快照

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005160052511-1486937402.gif)

4、线程快照分析

分析方式与 Windows 版一致，我们可以把 2227.stack 下载到本地进行分析，也可直接在 Linux 上分析

在 Linux 上分析，命令： cat 2227.stack |grep '8b4' -C 5 

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005160857697-1644470640.png)

至此定位到问题

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005161225327-456396209.png)

不管是在 Windows 下，还是在 Linux 下，排查套路都是一样的

　　　　![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005162319476-934354285.png)

**4.3.内存泄露**

同样的，Windows、Linux 各展示一个示例

**4.3.1.Windows**

1、找到内存占有率最高的进程号 PID

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003101550651-1105475493.gif)

第一眼看上去， idea 内存占有率最高，因为我是以 idea 启动的 java 进程；idea 进程我们无需关注，我们找到内存占有率最高的 java 的 PID： 10824 

2、利用 jmap 生成堆转储快照

命令： jmap -dump:format=b,file={path} {pid} 

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003103335905-1453172109.gif)

dump 文件路径： D:\heapdump_108244.hprof 

3、利用 MAT 分析 dump 文件

MAT：Memory Analyzer Tool，是针对 java 的内存分析工具；下载地址：

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003104829032-1371035071.png)

选择对应的版本，下载后直接解压；默认情况下，mat 最大内存是 1024m ，而我们的 dump 文件往往大于 1024m，所以我们需要调整，在 mat 的 home 目录下找到 MemoryAnalyzer.ini ，将 -Xmx1024m 修改成大于 dump 大小的空间， 我把它改成了 -Xmx4096m 

接着我们就可以将 dump 文件导入 mat 中，开始 dump 文件的解析

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003111554500-1696060756.gif)

解析是个比较漫长的过程，我们需要耐心等待

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003111641027-1672077826.gif)

解析完成后，我们可以看到如下概况界面

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003115057791-1520985127.png)

各个窗口的各个细节就不做详细介绍了，有兴趣的可自行去查阅资料；我们来看看几个图：饼状图、直方图、支配树、可疑的内存泄露报告

**饼状图**

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003122524138-1998051348.png)

可以看出， com.lee.schedule.Schedule 对象持有 1G 内存，肯定有问题

**直方图**　

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003123244326-919065365.png)

我们看下 Person 定义

```
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    private String name;
    private Integer age;

}
```

可想而知，上图标记的几项都与 Person 有关

**支配树**

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003131327910-1367025773.png)

这就非常直观了，Schedule 中的 ArrayList 占了 99.04% 的大小

可疑的内存泄露报告

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003132003332-1101477624.png)

通过这些数据，相信大家也能找到问题所在了

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201003132333862-1463075551.png)

**4.3.2.Linux**

排查方式与 Windows 一样，只是有稍许的命令区别

1、找到内存占有率最高的进程号

使用命令： top -c 显示运行中的进程列表信息， shift + m 按内存使用率进行排序

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005173640395-1393526073.png)

进程号： 2527 

2、利用 jmap 生成堆转储快照

命令： jmap -dump:format=b,file={path} {pid} 

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005174153301-1652576050.png)

堆转储快照文件路径： /opt/heapdump_2527.hprof 

3、利用 MAT 分析堆转储快照

将 heapdump_2448.phrof 下载到本地，利用 MAT 进行分析；分析过程与 Windows 版完全一致

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005175048982-1567905237.gif)

自此，定位到问题

Windows 下与 Linux 下，排查流程是一样的

![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201005175603239-1802273563.png)

**4.4.总结**

**4.4.1.JVM 常用命令**

jps：列出正在运行的虚拟机进程

jstat：监视虚拟机各种运行状态信息，可以显示虚拟机进程中的类装载、内存、垃圾收集、JIT编译等运行数据

jinfo：实时查看和调整虚拟机各项参数

jmap：生成堆转储快照，也可以查询 finalize 执行队列、Java 堆和永久代的详细信息

jstack：生成虚拟机当前时刻的线程快照

jhat：虚拟机堆转储快照分析工具

与 jmap 搭配使用，分析 jmap 生成的堆转储快照，与 MAT 的作用类似

**4.4.2.排查步骤**

1、先找到对应的进程： PID 

2、生成线程快照 stack （或堆转储快照： hprof ）

3、分析快照（或堆转储快照），定位问题

**4.4.3.内存泄露、内存溢出和 CPU 100% 关系**

 ![img](https://img2020.cnblogs.com/blog/747662/202010/747662-20201004114724788-615783599.png)

**4.4.4常用 JVM 性能检测工具**

Eclipse Memory Analyer、JProfile、JProbe Profiler、JVisualVM、JConsole、Plumbr