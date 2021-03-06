[toc]



# Redis 实际问题解决

## Redis的模糊查询在生产环境出现严重的性能问题
Redis是一个高性能高效率的key-value存储的nosql数据库，由于数据是存储在内存中，因此访问速度非常快，由于项目涉及到数据库的查询非常多，而数据变大并不是非常频繁，所以在项目中采用Redis分担大部分MySQL的压力。
在项目中实际使用我用的Redis提供的客户端连接工具包jedis，在项目中引入jedis.Jar即可

```java
public static Set<String> searchLike(String like_key) {
    //线上环境模糊查询带来严重的性能问题，杜绝使用
    if(!Config.IS_BUG){
        return null;
    }
    Jedis jedis = RedisApi.getJedis();
    boolean is_ok = true;
    try {
        if (jedis == null) {
            return null;
        }
        return jedis.keys(like_key);
    } catch (Exception e) {
        // TODO: handle exception
        is_ok = false;
        return null;
    } finally {
        close(jedis, is_ok);
    }
}
```
每当用户登录成功之后，都会生成一个cookie，分别存在客户端和Redis数据库，cookie的key由cookie值+用户ID组成：cookie字符串+"_"+用户ID，

例如用户cookie为“d9fb0ea5955fcf0a2183c5076”，用户ID为 19092,

那Redis中存储的key就是 d9fb0ea5955fcf0a2183c5076_19092,最终的key-vlaue就是：
```
{"d9fb0ea5955fcf0a2183c5076_19092":d9fb0ea5955fcf0a2183c5076}
```
而在用户不断的登录成功，就不断地产生这样的记录，久而久之，会积累出非常多的无用的key，浪费redis的空间，也加重了redis查询的负担，因此想到使用Redis的模糊查询来清掉无用的cookie的key

而Redis的客户端jedis操作是通过jedis.keys(keys)来完成的，keys可以使用通配符来匹配Redis中的key

通配符说明：
```
*: 0到任意多个字符 eg： searchLike("test*")
?: 1个字符
```
比如现在需要清除某个用户所有的无用的cookie的key，，则可以写成“ *_19092 ”
```
String key_like = "*_19092";
Set<String> keys = RedisApi.searchLike(key_like);
```

这样就可以查出所有这个用户的keys，调用jedis提供的批量删除key的方法即可达到目的。
```
String key_like = "*_19092";
Set<String> keys = RedisApi.searchLike(key_like);
```

到这里从需求到逻辑到编码一气呵成，简单测试没什么问题后，就发布到线上，由于平时网站的流量不算非常高，所以运行了几天也没发生什么异常，直到今天早上，拥有几十万粉丝的公众号发推文，推文的内容直接链接到网站，因此说瞬间流量是非常高
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305213801.png)

运行了大约十分钟之后，运营突然发疯的过来说网站访问非常慢，甚至出现错误码，心里一慌，赶紧上去看日志，我了个乖乖啊，简直是吓人，error日志想流水一样蹦出来，但五一不例外都是下面图示的错误：从Redis池中获取不大连接数，马上上redis服务器查看，发现CPU已经到达了100%以上
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305213806.png)

能让Redis的CPU到100%的，我想出了一下几个可能：

- 连接数过多，占用连接的时间过长
- 存储的值过大，存取均很占用CPU和内存
- 慢查询，事其它操作等待时间超时
- redis阻塞，某个操作把Redis阻塞，导致CPU飙升

由于项目上线时间已经很久，前三个可能基本都在平时查看Redis服务器性能的过程中排除掉，因此很大概率是第四个，突然想起前几天做的功能，有个模糊查询，该不是这个问题吧？我到网站输入“Redis 模糊查询 性能”，出来非常多关于redis模糊查询性能急剧下降的的情况，而且建议生产环境下禁用redis的模糊查询，于是我把模糊查询这块业务直接注释掉，重新上线，运行了半天，再没出现这个问题，因此可以断定就是模糊查询搞的鬼。

**【替代方案】**
有问题肯定是要解决的，既然模糊查询行不通的，那就得想别的办法达到目的，想到Redis有Set这这种存储结构，因此可以把用户的所有cookie key都放到一个用户专属的Set中，每次用户登录成功之后，都把之前Set里的cookie key清除，然后再把最新的key放进去，这样就可以达到同样的目的了。
```
String setKey = "prefix_customer_cookie_list_10920";
String token="ss2ssssss";
//取出所有的用户的cookie key
Set<String> list = RedisApi.getSet(setKey);
if (list != null && list.size() > 0) {
    //删除用户所有的cookie key
    RedisApi.removeFromSet(setKey, list.toArray(new String[0]));
}
//把最新的cookie key加入到Set中
RedisApi.addSet(setKey, token);
```





## Spring Redis开启事务支持错误用法导致服务不可用
### 事故背景
在APP访问服务器接口时需要从redis中获取token进行校验，服务器上线后发现一开始可以正常访问，但只要短时间内请求量增长服务则无法响应
### 排查流程
 1. 使用top指令查看CPU资源占用还远远达不到瓶颈，排查因为CPU资源不足导致服务不可用的可能
1. 查看tomcat线程池配置，默认最大线程数为200，理论上可以支持目前服务器的访问量
1. 使用jmap指令保存堆栈信息，jmap -dump:format=b,file=dump.log pid，pid为进程号
1. 使用Java visualVM打开保存的堆栈日志dump.log，发现绝大部分的线程都阻塞在从redis连接池中获取连接的代码，如下图所示
 ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/Image.png)
### 原理分析
根据堆栈日志所显示得知线程访问redis前需要从连接池队列中推出一个连接，当连接池没有连接时，则会阻塞等待，阻塞等待的时间可以自行设置MAA_WAIT参数，默认是-1表示不限时等待，目前项目使用默认配置，所以所有的线程都会一直阻塞在获取连接的步骤，如果设置了最大等待时间，当超过最大等待时间会报出Could not get a resource from the pool的异常

1. 在spring配置文件中的stringRedisTemplate对象配置参数中打开了事务支持，而redis的事务支持是用MUTI和EXEC指令来支持，以下事务实例截图来自菜鸟教程 https://www.runoob.com/redis/redis-transactions.html
 ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305214109.png)
 ![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305214101.png)

2. 如果要保证在事务能正常执行，那么在一个方法中多次操作redis必须是同一条连接，这样才能保证事务能正常执行，所以在stringRedisTemplate会将连接绑定在当前线程，当第二次访问redis时直接从当前线程中获取连接，绑定连接源码如下：
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305214203.png)

3. 按照流程，先绑定连接，最后在finally代码块中释放连接，看起来并没有问题，但跳进去releaseConnection方法的代码发现连接需要在事务提交后才能释放，也就是说service方法上必须使用@Transation注解修饰，但因为业务方法上少写了@Transation注解导致连接将一直绑定第一次获取他的线程上，当线程池的线程被获取完之后，其他线程就会就如阻塞等待状态，导致服务不可用
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305214207.png)

4. 如果加上@Transation注解，那么方法执行完之后将会执行TransactionSynchronizationUtils.invokeAfterCompletion这个方法，mysql事务也是在这个方法执行commit操作，如下图所示方法的第一个参数是List<TransactionSynchronization> synchronizations，代表可以有多个事务，redis，mysql等，都会此进行事务提交操作，这里使用多态，根据对象的具体类型执行不同的方法，redis则执行redis的事务提交操作，mysql则执行mysql的事务提交操作
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305214204.png)

5. 以下为redis事务提交的代码，也跟我们上面提到的一样，发送exec指令提交事务
![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305214301.png)

### 如何修改代码
1. 确认实际需求是否需要事务支持，如果需要则在对应方法上加上@Transaction注解

2. 如果不需要事务支持则将enableTransactionSupport设置为false

 



## 线上Redis高并发连接失败问题排查

### 项目背景

最近，做一个按优先级和时间先后排队的需求。用 Redis 的 sorted set 做排队队列。

主要使用的 Redis 命令有， zadd, zcount, zscore, zrange 等。

测试完毕后，发到线上，发现有大量接口请求返回超时熔断（超时时间为3s）。

Error日志打印的异常堆栈为：

```
redis.clients.jedis.exceptions.JedisConnectionException: Could not get a resource from the pool

Caused by: redis.clients.jedis.exceptions.JedisConnectionException: java.net.ConnectException: Connection timed out (Connection timed out)

Caused by: java.net.ConnectException: Connection timed out (Connection timed out)
```

且有一个怪异的现象，只有写库的逻辑报错，即 zadd 操作。像 zcount, zscore 这些操作全部能正常执行。

还有就是报错和正常执行交错持续。即假设每分钟有1000个 Redis 操作，其中900个正常，100个报错。而不是报错后，Redis 就不能正常使用了。

### 问题排查

**1. 连接池泄露？**

从上面的现象基本可以排除连接池泄露的可能，如果连接未被释放，那么一旦开始报错，后面的 Redis 请求基本上都会失败。而不是有90%都可正常执行。

但 Jedis 客户端据说有高并发下连接池泄露的问题，所以为了排除一切可能，还是升级了 Jedis 版本，发布上线，发现没什么用。



**2.硬件原因？**

排查 Redis 客户端服务器性能指标，CPU利用率10%，内存利用率75%，磁盘利用率10%，网络I/O上行 1.12M/s，下行 2.07M/s。接口单实例QPS均值300左右，峰值600左右。

Redis 服务端连接总数徘徊在2000+，CPU利用率5.8%，内存使用率49%，QPS1500-2500。

硬件指标似乎也没什么问题。



**3.Redis参数配置问题？**

```
 1 JedisPoolConfig config = new JedisPoolConfig();
 2 config.setMaxTotal (200);        // 最大连接数
 3 config.setMinIdle (5);           // 最小空闲连接数
 4 config.setMaxIdle (50);          // 最大空闲连接数
 5 config.setMaxWaitMillis (1000 * 1);    // 最长等待时间
 6 config.setTestOnReturn (false);
 7 config.setTestOnBorrow (false);
 8 config.setTestWhileIdle (true);
 9 config.setTimeBetweenEvictionRunsMillis (30 * 1000);
10 config.setNumTestsPerEvictionRun (50);
```

基本上大部分公司的配置包括网上博客提供的配置其实都和上面差不多，看不出有什么问题。

这里我尝试把最大连接数调整到500，发布到线上，并没什么卵用，报错数反而变多了。



**4.连接数统计**

在 Redis Master 库上执行命令：client list。打印出当前所有连接到服务器的客户端IP，并过滤出当前服务的IP地址的连接。

发现均未达到最大连接数，确实排除了连接泄露的可能。

![image-20210306131448698](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131448698.png)

 

**5.最大连接数调优和压测**

既然连接远未打满，说明不需要设置那么大的连接数。而 Redis 服务端又是单线程读写。客户端创建过多连接，只会耗费资源，反而拖累性能。

![image-20210306131528347](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131528347.png)

使用以上代码，在本机使用 JMeter 压测300个线程，连续请求30秒。

**首先把最大连接数设为500**，成功率：99.61%

请求成功：82004次，TP90耗时目测在50-80ms左右。

请求失败322次，全部为请求服务器超时：socket read timeout，耗时2s后，由 Jedis 自行熔断。

（这种情况造成数据不一致，实际上服务端已执行了命令，只是客户端读取返回结果超时）。

![image-20210306131617920](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131617920.png)

![image-20210306131633377](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131633377.png)

**再把最大连接数设为20，**成功率：98.62%（有一定几率100%成功）

请求成功：85788次，TP90耗时在10ms左右。

请求失败：1200次，全部为等待客户端连接超时：`Caused by: java.util.NoSuchElementException: Timeout waiting for idle object`，熔断时间为1秒。

![image-20210306131719526](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131719526.png)

![image-20210306131743317](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131743317.png)

 **再将最大连接数调整为50，**成功率：100%

 请求成功：85788次， TP90耗时10ms。

请求失败：0次。

![image-20210306131822318](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131822318.png)

![image-20210306131834870](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131834870.png)

**综上，Redis 服务端单线程读写，连接数太多并没卵用，反而会消耗更多资源。最大连接数配置太小，不能满足并发需求，线程会因为拿不到空闲连接而超时退出。**

**在满足并发的前提下，maxTotal连接数越小越好。在300线程并发下，最大连接数设为50，可以稳定运行。**

基于以上结论，尝试调整 Redis 参数配置并发布上线，但以上实验只执行了 zadd 命令，仍未解决一个问题：为什么只有写库报错？

果然，发布上线后，接口超时次数有所减少，响应时间有所提升，但仍有报错，没能解决此问题。



**6.插曲 - Redis锁**

在优化此服务的同时，把同事使用的另一个 Redis 客户端一起优化了，结果同事的接口过了一天开始大面积报错，接口响应时间达到8个小时。

排查发现，同事的接口仅使用 Redis 作为分布式锁。而这个 RedisLock 类是从其他服务拿过来直接用的，自旋时间设置过长，这个接口又是超高并发。

最大连接数设为50后，锁资源竞争激烈，直接导致大部分线程自旋把连接池耗尽了。于是又紧急把最大连接池恢复到200，问题得以解决。

由此可见，在分布式锁的场景下，配置不能完全参考读写 Redis 操作的配置。



**7.排查服务端持久化**

在把客户端研究了好几遍之后，发现并没有什么可以优化的了，于是开始怀疑是服务端的问题。

持久化是一直没研究过的问题。在查阅了网上的一些博客，发现持久化确实有可能阻塞读写IO的。

- 对于没有持久化的方式，读写都在数据量达到800万的时候，性能下降几倍，此时正好是达到内存10G，Redis开始换出到磁盘的时候。并且从那以后再也没办法重新振作起来，性能比Mongodb还要差很多。
-  对于AOF持久化的方式，总体性能并不会比不带持久化方式差太多，都是在到了千万数据量，内存占满之后读的性能只有几百。
-  对于Dump持久化方式，读写性能波动都比较大，可能在那段时候正在Dump也有关系，并且在达到了1400万数据量之后，读写性能贴底了。在Dump的时候，不会进行换出，而且所有修改的数据还是创建的新页，内存占用比平时高不少，超过了15GB。而且Dump还会压缩，占用了大量的CPU。也就是说，在那个时候内存、磁盘和CPU的压力都接近极限，性能不差才怪。” ---- 引用自[lovecindywang](https://www.cnblogs.com/lovecindywang/) 的博客园博客

 

“**内存越大，触发持久化的操作阻塞主线程的时间越长**

- Redis是单线程的内存数据库，在redis需要执行耗时的操作时，会fork一个新进程来做，比如bgsave，bgrewriteaof。 Fork新进程时，虽然可共享的数据内容不需要复制，但会复制之前进程空间的内存页表，这个复制是主线程来做的，会阻塞所有的读写操作，并且随着内存使用量越大耗时越长。例如：内存20G的redis，bgsave复制内存页表耗时约为750ms，redis主线程也会因为它阻塞750ms。”    ---- 引用自CSDN博客

 

而我们的Redis实例内存配额20G，已使用了50%，keys数量达4000w。

主从集群，从库不做持久化，主库使用RDB持久化。rdb的save参数是默认值。（这也恰好能解释通为什么写库报错，读库正常）

且此 Redis 已使用了几年，里面可能存在大量的key已经不使用了，但未设置过期时间。

　　

然而，像 Redis、MySQL 这种都是由数据中台负责，我们并无权查看服务端日志，这个事情也不好推动，中台会说客户端使用的有问题，建议调整参数。

所以最佳解决方案可能是，重新申请 Redis 实例，逐步把项目中使用的 Redis 迁移到新实例，并注意设置过期时间。迁移完成后，把老的 Redis 实例废弃回收。



### 小结

1. 如果简单的在网上搜索，**Could not get a resource from the pool ，** 基本都是些连接未释放的问题。然而很多原因可能导致 Jedis 报这个错，这条信息并不是异常堆栈的最顶层。
2. Redis其实只适合作为缓存，而不是数据库或是存储。它的持久化方式适用于救救急啥的，不太适合当作一个普通功能来用。
3. 还是建议任何数据都设置过期时间，哪怕设1年呢。不然老的项目可能已经都废弃了，残留在 Redis 里的 key，其他人也不敢删。
4. 不要存放垃圾数据到 Redis 中，及时清理无用数据。业务下线了，就把相关数据清理掉。



























