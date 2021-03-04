[toc]



# Redis 面试题



## 你知道Redis的字符串是怎么实现的吗？

Redis是C语言开发的，C语言自己就有字符类型，但是Redis却没直接采用C语言的字符串类型，而是自己构建了`动态字符串（SDS）`的抽象类型。

![image-20210228181856863](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228181856863.png)

就好比这样的一个命令，其实我是在Redis创建了两个SDS，一个是名为`aobing`的Key SDS，另一个是名为`cool`的Value SDS，就算是字符类型的List，也是由很多的SDS构成的Key和Value罢了。

SDS在Redis中除了用作字符串，还用作缓冲区（buffer），那到这里大家都还是有点疑惑的，C语言的字符串不好么为啥用SDS？SDS长啥样？有什么优点呢?

为此我去找到了Redis的源码，可以看到SDS值的结果大概是这样的，源码的在GitHub上是开源的大家一搜就有了。

```c
struct sdshdr{
 int len;
 int free;
 char buf[];
}
```

![image-20210228182032831](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182032831.png)

回到最初的问题，为什么Redis用了自己新开发的SDS，而不用C语言的字符串？那好我们去看看他们的区别。



### SDS与C字符串的区别

1. **计数方式不同**

   C语言对字符串长度的统计，就完全来自遍历，从头遍历到末尾，直到发现空字符就停止，以此统计出字符串的长度，这样获取长度的时间复杂度来说是0（n），大概就像下面这样：

   ![](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210228182201.gif)

   但是这样的计数方式会留下隐患，所以Redis没有采用C的字符串，我后面会提到。

   而Redis我在上面已经给大家看过结构了，他自己本身就保存了长度的信息，所以我们获取长度的时间复杂度为0（1），是不是发现了Redis快的一点小细节了？还没完，不止这些。

2. **杜绝缓冲区溢出**

   字符串拼接是我们经常做的操作，在C和Redis中一样，也是很常见的操作，但是问题就来了，C是不记录字符串长度的，一旦我们调用了拼接的函数，如果没有提前计算好内存，是会产生缓存区溢出的。

   比如本来字符串长这样：

   ![image-20210228182336698](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182336698.png)

   你现在需要在后面拼接  ，但是你没计算好内存，结果就可能这样了：

   ![image-20210228182425120](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182425120.png)

   这是你要的结果么？很显然，不是，你的结果意外的被修改了，这要是放在线上的系统，这不是完了？那Redis是怎么避免这样的情况的？

   我们都知道，他结构存储了当前长度，还有free未使用的长度，那简单呀，你现在做了拼接操作，我去判断一些是否可以放得下，如果长度够就直接执行，如果不够，那我就进行扩容。

   这些大家在Redis源码里面都是可以看到对应的API的，后面我就不一一贴源码了，有兴趣的可以自己去看一波，需要一点C语言的基础。

   ![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210228182501.webp)

3. 减少修改字符串时带来的内存重分配次数

   C语言字符串底层也是一个数组，每次创建的时候就创建一个N+1长度的字符，多的那个1，就是为了保存空字符的，这个空字符也是个坑，但是不是这个环节探讨的内容。

   Redis是个高速缓存数据库，如果我们需要对字符串进行频繁的拼接和截断操作，如果我们写代码忘记了重新分配内存，就可能造成缓冲区溢出，以及内存泄露。

   内存分配算法很耗时，且不说你会不会忘记重新分配内存，就算你全部记得，对于一个高速缓存数据库来说，这样的开销也是我们应该要避免的。

   Redis为了避免C字符串这样的缺陷，就分别采用了两种解决方案，去达到性能最大化，空间利用最大化：

   - 空间预分配：当我们对SDS进行扩展操作的时候，Redis会为SDS分配好内存，并且根据特定的公式，分配多余的free空间，还有多余的1byte空间（这1byte也是为了存空字符），这样就可以避免我们连续执行字符串添加所带来的内存分配消耗。

     比如现在有这样的一个字符：

     ![image-20210228182720875](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182720875.png)

     我们调用了拼接函数，字符串边长了，Redis还会根据算法计算出一个free值给他备用：

     ![image-20210228182737393](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182737393.png)

     我们再继续拼接，你会发现，备用的free用上了，省去了这次的内存重分配：

     ![image-20210228182753421](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182753421.png)

   - 惰性空间释放：刚才提到了会预分配多余的空间，很多小伙伴会担心带来内存的泄露或者浪费，别担心，Redis大佬一样帮我们想到了，当我们执行完一个字符串缩减的操作，redis并不会马上收回我们的空间，因为可以预防你继续添加的操作，这样可以减少分配空间带来的消耗，但是当你再次操作还是没用到多余空间的时候，Redis也还是会收回对于的空间，防止内存的浪费的。

     还是一样的字符串：

     ![image-20210228182847778](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182847778.png)

     当我们调用了删减的函数，并不会马上释放掉free空间：

     ![image-20210228182905523](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182905523.png)

     如果我们需要继续添加这个空间就能用上了，减少了内存的重分配，如果空间不需要了，调用函数删掉就好了：

     ![image-20210228182924321](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228182924321.png)

4. 二进制安全

   仔细看的仔肯定看到上面我不止一次提到了空字符也就是’\0‘，C语言是判断空字符去判断一个字符的长度的，但是有很多数据结构经常会穿插空字符在中间，比如图片，音频，视频，压缩文件的二进制数据，就比如下面这个单词，他只能识别前面的 不能识别后面的字符，那对于我们开发者而言，这样的结果显然不是我们想要的对不对。

   ![image-20210228183010653](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228183010653.png)

   Redis就不存在这个问题了，他不是保存了字符串的长度嘛，他不判断空字符，他就判断长度对不对就好了，所以redis也经常被我们拿来保存各种二进制数据，我反正是用的很high，经常用来保存小文件的二进制。

   ![image-20210228183037288](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210228183037288.png)

### 总结

大家是不是发现，一个小小的SDS居然有这么多道理在这？

以前就知道Redis快，最多说个Redis是单线程的，说个多路IO复用，说个基于内存的操作就完了，现在是不是还可以展开说说了？





## Redis中是如何实现分布式锁的？

分布式锁常见的三种实现方式：

1. 数据库乐观锁；
2. 基于Redis的分布式锁；
3. 基于ZooKeeper的分布式锁。

本地面试考点是，你对Redis使用熟悉吗？Redis中是如何实现分布式锁的。

### 要点

Redis要实现分布式锁，以下条件应该得到满足

**互斥性**

- 在任意时刻，只有一个客户端能持有锁。

**不能死锁**

- 客户端在持有锁的期间崩溃而没有主动解锁，也能保证后续其他客户端能加锁。

**容错性**

- 只要大部分的Redis节点正常运行，客户端就可以加锁和解锁。



### 实现

可以直接通过 `set key value px milliseconds nx` 命令实现加锁， 通过Lua脚本实现解锁。

```
//获取锁（unique_value可以是UUID等）
SET resource_name unique_value NX PX  30000

//释放锁（lua脚本中，一定要比较value，防止误解锁）
if redis.call("get",KEYS[1]) == ARGV[1] then
    return redis.call("del",KEYS[1])
else
    return 0
end
```

**代码解释**

- set 命令要用 `set key value px milliseconds nx`，替代 `setnx + expire` 需要分两次执行命令的方式，保证了原子性，
- value 要具有唯一性，可以使用`UUID.randomUUID().toString()`方法生成，用来标识这把锁是属于哪个请求加的，在解锁的时候就可以有依据；
- 释放锁时要验证 value 值，防止误解锁；
- 通过 Lua 脚本来避免 Check And Set 模型的并发问题，因为在释放锁的时候因为涉及到多个Redis操作 （利用了eval命令执行Lua脚本的原子性）；

**加锁代码分析**

首先，`set()`加入了NX参数，可以保证如果已有`key`存在，则函数不会调用成功，也就是只有一个客户端能持有锁，满足互斥性。其次，由于我们对锁设置了过期时间，即使锁的持有者后续发生崩溃而没有解锁，锁也会因为到了过期时间而自动解锁（即key被删除），不会发生死锁。最后，因为我们将value赋值为requestId，用来标识这把锁是属于哪个请求加的，那么在客户端在解锁的时候就可以进行校验是否是同一个客户端。

**解锁代码分析**

将`Lua`代码传到`jedis.eval()`方法里，并使参数`KEYS[1]`赋值为`lockKey`，`ARGV[1]`赋值为`requestId`。在执行的时候，首先会获取锁对应的`value`值，检查是否与`requestId`相等，如果相等则解锁（删除key）。

**存在的风险**

如果存储锁对应key的那个节点挂了的话，就可能存在丢失锁的风险，导致出现多个客户端持有锁的情况，这样就不能实现资源的独享了。

1. 客户端A从master获取到锁
2. 在master将锁同步到slave之前，master宕掉了（Redis的主从同步通常是异步的）。
   主从切换，slave节点被晋级为master节点
3. 客户端B取得了同一个资源被客户端A已经获取到的另外一个锁。导致存在同一时刻存不止一个线程获取到锁的情况。



### redlock算法出现

这个场景是假设有一个 redis cluster，有 5 个 redis master 实例。然后执行如下步骤获取一把锁：

1. 获取当前时间戳，单位是毫秒；
2. 跟上面类似，轮流尝试在每个 master 节点上创建锁，过期时间较短，一般就几十毫秒；
3. 尝试在大多数节点上建立一个锁，比如 5 个节点就要求是 3 个节点 n / 2 + 1；
4. 客户端计算建立好锁的时间，如果建立锁的时间小于超时时间，就算建立成功了；
5. 要是锁建立失败了，那么就依次之前建立过的锁删除；
6. 只要别人建立了一把分布式锁，你就得不断轮询去尝试获取锁。

 ![image-20210304231419219](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304231419219.png)

Redis 官方给出了以上两种基于 Redis 实现分布式锁的方法，详细说明可以查看：

> https://redis.io/topics/distlock 。



### Redisson实现

Redisson是一个在Redis的基础上实现的Java驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的Java常用对象，还实现了可重入锁（Reentrant Lock）、公平锁（Fair Lock、联锁（MultiLock）、 红锁（RedLock）、 读写锁（ReadWriteLock）等，还提供了许多分布式服务。

Redisson提供了使用Redis的最简单和最便捷的方法。Redisson的宗旨是促进使用者对Redis的关注分离（Separation of Concern），从而让使用者能够将精力更集中地放在处理业务逻辑上。

**Redisson 分布式重入锁用法**

Redisson 支持单点模式、主从模式、哨兵模式、集群模式，这里以单点模式为例：

```
// 1.构造redisson实现分布式锁必要的Config
Config config = new Config();
config.useSingleServer().setAddress("redis://127.0.0.1:5379").setPassword("123456").setDatabase(0);
// 2.构造RedissonClient
RedissonClient redissonClient = Redisson.create(config);
// 3.获取锁对象实例（无法保证是按线程的顺序获取到）
RLock rLock = redissonClient.getLock(lockKey);
try {
    /**
     * 4.尝试获取锁
     * waitTimeout 尝试获取锁的最大等待时间，超过这个值，则认为获取锁失败
     * leaseTime   锁的持有时间,超过这个时间锁会自动失效（值应设置为大于业务处理的时间，确保在锁有效期内业务能处理完）
     */
    boolean res = rLock.tryLock((long)waitTimeout, (long)leaseTime, TimeUnit.SECONDS);
    if (res) {
        //成功获得锁，在这里处理业务
    }
} catch (Exception e) {
    throw new RuntimeException("aquire lock fail");
}finally{
    //无论如何, 最后都要解锁
    rLock.unlock();
}
```

**加锁流程图**

![image-20210304231636699](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304231636699.png)

**解锁流程图**

![image-20210304231719590](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304231719590.png)

我们可以看到，RedissonLock是可重入的，并且考虑了失败重试，可以设置锁的最大等待时间， 在实现上也做了一些优化，减少了无效的锁申请，提升了资源的利用率。

需要特别注意的是，RedissonLock 同样没有解决 节点挂掉的时候，存在丢失锁的风险的问题。而现实情况是有一些场景无法容忍的，所以 Redisson 提供了实现了redlock算法的 RedissonRedLock，RedissonRedLock 真正解决了单点失败的问题，代价是需要额外的为 RedissonRedLock 搭建Redis环境。

所以，如果业务场景可以容忍这种小概率的错误，则推荐使用 RedissonLock， 如果无法容忍，则推荐使用 RedissonRedLock。







