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

 