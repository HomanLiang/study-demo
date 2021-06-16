[toc]



# Redis 实际问题解决

## 1.Redis的模糊查询在生产环境出现严重的性能问题
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





## 2.Spring Redis开启事务支持错误用法导致服务不可用
### 2.1.事故背景
在APP访问服务器接口时需要从redis中获取token进行校验，服务器上线后发现一开始可以正常访问，但只要短时间内请求量增长服务则无法响应
### 2.2.排查流程
 1. 使用top指令查看CPU资源占用还远远达不到瓶颈，排查因为CPU资源不足导致服务不可用的可能

1. 查看tomcat线程池配置，默认最大线程数为200，理论上可以支持目前服务器的访问量

1. 使用jmap指令保存堆栈信息，jmap -dump:format=b,file=dump.log pid，pid为进程号

1. 使用Java visualVM打开保存的堆栈日志dump.log，发现绝大部分的线程都阻塞在从redis连接池中获取连接的代码，如下图所示

 ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/Image.png)
### 2.3.原理分析
根据堆栈日志所显示得知线程访问redis前需要从连接池队列中推出一个连接，当连接池没有连接时，则会阻塞等待，阻塞等待的时间可以自行设置MAA_WAIT参数，默认是-1表示不限时等待，目前项目使用默认配置，所以所有的线程都会一直阻塞在获取连接的步骤，如果设置了最大等待时间，当超过最大等待时间会报出Could not get a resource from the pool的异常

1. 在spring配置文件中的stringRedisTemplate对象配置参数中打开了事务支持，而redis的事务支持是用MUTI和EXEC指令来支持

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

### 2.4.如何修改代码
1. 确认实际需求是否需要事务支持，如果需要则在对应方法上加上@Transaction注解

2. 如果不需要事务支持则将enableTransactionSupport设置为false

 



## 3.线上Redis高并发连接失败问题排查

### 3.1.项目背景

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

### 3.2.问题排查

**3.2.1. 连接池泄露？**

从上面的现象基本可以排除连接池泄露的可能，如果连接未被释放，那么一旦开始报错，后面的 Redis 请求基本上都会失败。而不是有90%都可正常执行。

但 Jedis 客户端据说有高并发下连接池泄露的问题，所以为了排除一切可能，还是升级了 Jedis 版本，发布上线，发现没什么用。

**3.2.2.硬件原因？**

排查 Redis 客户端服务器性能指标，CPU利用率10%，内存利用率75%，磁盘利用率10%，网络I/O上行 1.12M/s，下行 2.07M/s。接口单实例QPS均值300左右，峰值600左右。

Redis 服务端连接总数徘徊在2000+，CPU利用率5.8%，内存使用率49%，QPS1500-2500。

硬件指标似乎也没什么问题。

**3.2.3.Redis参数配置问题？**

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

**3.2.4.连接数统计**

在 Redis Master 库上执行命令：client list。打印出当前所有连接到服务器的客户端IP，并过滤出当前服务的IP地址的连接。

发现均未达到最大连接数，确实排除了连接泄露的可能。

![image-20210306131448698](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210306131448698.png)

**3.2.5.最大连接数调优和压测**

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

**3.2.6.插曲 - Redis锁**

在优化此服务的同时，把同事使用的另一个 Redis 客户端一起优化了，结果同事的接口过了一天开始大面积报错，接口响应时间达到8个小时。

排查发现，同事的接口仅使用 Redis 作为分布式锁。而这个 RedisLock 类是从其他服务拿过来直接用的，自旋时间设置过长，这个接口又是超高并发。

最大连接数设为50后，锁资源竞争激烈，直接导致大部分线程自旋把连接池耗尽了。于是又紧急把最大连接池恢复到200，问题得以解决。

由此可见，在分布式锁的场景下，配置不能完全参考读写 Redis 操作的配置。

**3.2.7.排查服务端持久化**

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

### 3.3.小结

1. 如果简单的在网上搜索，**Could not get a resource from the pool ，** 基本都是些连接未释放的问题。然而很多原因可能导致 Jedis 报这个错，这条信息并不是异常堆栈的最顶层。
2. Redis其实只适合作为缓存，而不是数据库或是存储。它的持久化方式适用于救救急啥的，不太适合当作一个普通功能来用。
3. 还是建议任何数据都设置过期时间，哪怕设1年呢。不然老的项目可能已经都废弃了，残留在 Redis 里的 key，其他人也不敢删。
4. 不要存放垃圾数据到 Redis 中，及时清理无用数据。业务下线了，就把相关数据清理掉。

## 4.巧用 Redis pipeline 命令，解决真实的生产问题

### 4.1.为什么多次调用 Redis 命令比较慢

Redis 客户端执行一个命令需要经历流程如下图所示：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183317.webp)

总共需要经过四个流程：

1. 客户端发送命令
2. Redis 服务收到命令等待处理
3. Redis 服务端处理命令
4. Redis 服务返回执行结果

Redis 的客户端与服务可能部署在不同的机器上，这里我们假设 Redis 客户端部署在北京，而 Redis 服务端在广州，两地的网络延时为 50ms。

一次 Redis 命令，1 与 4 这两个流程就需要耗费 100ms, 而 2 与 3 在由于是在 Redis 服务端执行，执行速度会很快，可以忽略不计。

此时客户端如果需要执行 N 次 Redis 命令，我们就需要耗费 `2N*100ms` 时间，执行命令越多，耗时越长。

这就是文章开头 Redis 删除多个命令比较慢的主要原因。

### 4.2.Redis pipeline 流水线执行命令

那如何解决这类问题了？

解决办法有三种：

- 第一种利用多线程机制，并行执行命令，提高执行速度。

- 第二种，调用 `mget` 这类命令，这类命令可以一次操作多个键，Redis 服务端收到命令之后，将会批量执行。

  但是 `mget`这类批量命令毕竟是少数，很多情况下我们没办法直接使用，就像我们上面的例子。

- 这样的话，只能使用最后一种办法，使用 Redis `pipeline`命令。

  开启 Redis `pipeline` 之后,再执行 Redis 的其他命令，命令将不会发送给服务端，而是先暂存在客户端，左后等到所有命令都执行完，然后再统一发送给服务端。

  服务端会根据发送过来的命令的顺序，依次运行计算。

  然后同样先将结果暂存服务端，等到命令都执行完毕之后，统一返回给客户端。

  通过这种方式，减少多个命令之间网络交互，有效的提高多个命令执行的速度。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183424.webp)

如上图所示，开启 Redis Pipeline 之后，客户端运行的 5 个命令将会一起发送到服务端。服务依次运行命令，然后统一返回。

介绍完原理，我们来看下如何使用  Redis Pipeline ，下面代码以 Jedis 为例。

```
JedisPoolConfig poolConfig = new JedisPoolConfig();
poolConfig.setMaxIdle(100);
poolConfig.setTestOnBorrow(false);
poolConfig.setTestOnReturn(false);


JedisPool jedisPool = new JedisPool(poolConfig, "127.0.0.1", Integer.parseInt("6379"), 60*1000, "1234qwer");

Jedis jedis = jedisPool.getResource();

Pipeline pipelined = jedis.pipelined();

for (int i = 0; i < 100; i++) {
    pipelined.set("key" + i, "value" + i);
}
pipelined.sync();
```

`Jedis#pipelined` 将会开启 Redis `Pipeline`，而`Pipeline` 这个类提供所有 Redis 可以使用的命令：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183435.webp)

当执行完所有的命令之后，调用 `Pipelined#sync` 命令，所有命令数据将会统一发送到到 Redis 中。

上面的例子中，`Pipelined#sync` 方法调用之后不会返回任何结果。

如果此时需要处理 Redis 的返回值，那么我们需要调用 `Pipelined#syncAndReturnAll` 方法，这个方法返回值将会是一个集合，返回结果按照 Redis 命令的顺序排序。

### 4.3.解密 pipeline 实现原理

Redis `pipeline` 命令的实现，其实需要客户端与服务端同时支持，并且实际执行过程中，Redis `pipeline` 会根据需要发送命令数据量大小进行拆分，拆分成多个数据包进行发送。

这么做主要原因是因为，如果一次组装 `pipeline` 数据量过大，一方面会增加客户端的等待时间，而另一方面会造成一定的网络阻塞。

不同 Redis 客户端 `pipeline` 发送的最大字节数不太相同，比如 jedis-pipeline 每次最大发送字节数为8192。

下面我们从源码侧，看下 jedis `pipeline` 实现机制。

Pipeline 所有命令方法，底层最终将会调用 `Protocol#sendCommand`方法，这个方法主要就是向 `RedisOutputStream` 输出流中写入数据。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183447.webp)

`RedisOutputStream#write`方法如下图所示：

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183507.webp)

这个方法内，一旦缓冲的数据大小超过指定大小，目前为 8192，就会立刻将数据全部写入到真正输出流中。

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183516.webp)

`pipeline` 多个命令实际发送流程图如下所示：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502183524.webp)

一旦 Redis 客户端将部分 `pipeline` 中执行命令的发送给 Redis 服务端，服务端就会立即运行这些命令，然后返回给客户端。

但是此时客户端并不会去读取，所以返回的响应数据将会暂存在客户端的 Socket 接收缓冲区中。

如果响应数据比较大，填满缓冲区，此时客户端会通过 TCP 流量控制机制，ACK 返回 WIN=0（接收窗口）来控制服务端不要再发送数据。

这时这些响应数据将会一直暂存在 Redis 服务端输出缓存中，如果数据比较多，将会占用很多内存。

所以使用 Redis Pipeline 机制一定注意返回的数据量，如果数据很多，建议将包含大量命令的 `pipeline` 拆分成多次较小的 `pipeline` 来完成。

### 4.4.总结

Redis 的 `pipeline` 命令可以批量执行多个 redis 命令，它通过减少网络的调用次数，从而有效提高的多个命令执行的速度。

不过我们使用过程，一定注意执行数据的大小，如果数据过大，可以考虑将一个 `pipeline` 拆分成多个小 `pipeline`执行。

## 5.乱码问题处理

### 5.1.前言

Redis 作为当前最流行的 NoSQL 之一，想必很多人都用过。

Redis 有五种常见的数据类型：string、list、hash、set、zset。讲真，我以前只用过 Redis 的 string 类型。

由于业务需求，用到了 Redis 的集合 set。这不，一上来就踩到坑了。

前几天有个需求提测，测试小哥提了个 bug，并给了我一个日志截图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194649.webp)

### 5.2.问题排查

从堆栈信息定位到了项目的代码，大致如下：

```
public class CityService
  private void setStatus(CityRequest request) {
    // 根据城市码查询城市信息
    Set<String> cityList = cityService.findByCityCode(request.getCityCode());
    if (CollectionUtils.isEmpty(cityList)) {
      return;
    }

    // 遍历，做一些操作（报错就在这这一行）
    for (String city : cityList) {
      // ...
    }
  }

  // 一些无关的代码...
}
```

报错的代码就在 for 循环那一行。

这一行看起来似乎没什么错误，跟 HashSet 和 String 转换有什么关系呢？往前翻一翻 cityList 是怎么来的。

cityList 会根据城市码查询城市信息，这个方法有如下三步：

1. 从本地缓存查询，若存在则直接返回；否则进行第二步。
2. 从 Redis 查询，若存在，存入本地缓存并返回；否则进行第三步。
3. 从 MySQL 查询，若存在，存入本地缓存和 Redis（set 类型）并返回；若不存在返回空。

联系报错信息，再看这几步的代码，1、3 可能性较小；第二步因为之前没有直接用过 set 这种数据结构，嫌疑较大。

于是想先通过 Redis 客户端看下缓存信息。

这一看不当紧，更疑惑了：Redis 的 key/value 前面有类似`\xAC\xED\x00\x05t\x00\x1B` 的字符串（可能略有不同），而且还有乱码。如图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194716.webp)

### 5.3.乱码问题处理

网上查了一番，原来是 spring-data-redis 的 RedisTemplate 序列化的问题。

RedisTemplate 的默认配置如下：

```
public class RedisAutoConfiguration {

 @Bean
 @ConditionalOnMissingBean(name = "redisTemplate")
 public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory)
   throws UnknownHostException {
  RedisTemplate<Object, Object> template = new RedisTemplate<>();
  template.setConnectionFactory(redisConnectionFactory);
  return template;
 }
}
```

RedisTemplate 在操作 Redis 时默认使用 JdkSerializationRedisSerializer 来进行序列化的。

对于这个问题，修改下配置就可以了，示例代码如下：

```
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisConfig {
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory);

    // 使用 Jackson2JsonRedisSerialize 替换默认序列化
    Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

    // 设置 key/value 的序列化规则
    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);

    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer);
    redisTemplate.afterPropertiesSet();

    return redisTemplate;
  }
}
```

这个配置改过之后，乱码的情况就没了。

### 5.4.类型转换问题

继续跟进前面的类型转换问题。

通过客户端查看 Redis 的值，如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194751.webp)



这是什么鬼？明显不对劲儿啊！

我们想存储的是 set 类型，正常应该是三条数据，这里怎么只有一条？

想了想应该是向 Redis 存储值的时候有什么问题，于是翻到代码看了看怎么存的：

```
public class CityService {
  public Set<String> findCityByCode(String cityCode) {
    // ...

    // 查询MySQL
    List<CityDO> cityDoList = cityRepository.findByCityCode(cityCode);

    // 封装数据
    Set<String> cityList = new HashSet<>();
    cityDoList.forEach(record -> {
      String city = String.format("%s-%s", record.getType(), record.getCity());
      cityList.add(city);
    });

    // 【问题出在这里】
    redisService.add2Set(cacheKey, cityList);
    return cityList;
  }
}
```

RedisService#add2Set 方法：

```
public class RedisService {
  // ...
  public <T> void add2Set(String key, T... values) {
    redisTemplate.opsForSet().add(key, values);
  }
}
```

乍一看好像没什么问题。

但是再一看，RedisService#add2Set 方法中，values 是可变长度类型的参数，如果把整个 cityList（java.util.Set 类型）作为一个参数传给可变长度类型的参数会怎么样呢？

> PS: 可变长度类型参数是 Java 中的一种语法糖，其实它本质上是一个数组。

打个断点看下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194808.webp)



可以看到这里的 Set 类型，也就是传入的 cityList 被当成了数组中的一个元素，怪不得会报错。

那这种情况该怎么处理呢？

其实也很简单，把 cityList 转成数组就可以了：

```
public class CityService {
  public Set<String> findCityByCode(String cityCode) {
    // ...

    // 【问题出在这里】转成数组，即 toArray 方法
    redisService.add2Set(cacheKey, cityList.toArray());
    return cityList;
  }
}
```

这样入参就按照想要的方式来了：

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194814.webp)

再观察 Redis 的缓存值，可以看到也是想要的结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194823.webp)



到这里，问题算是搞定了。

### 5.5.结语

本文主要复盘了 Redis 使用过程中遇到的两个问题：

1. Redis key/value 乱码问题。原因是 RedisTemplate 的序列化问题，注意配置。
2. HashSet 和 String 类型转换问题。主要是在操作 Redis 的 set 时（其他类型亦然），注意 API 的参数细节，不能想当然。



