[toc]



# Redis Java API

## Jedis
1. 引用Jedis.jar
2. 连接到 redis 服务
```java
import redis.clients.jedis.Jedis;
 
public class RedisJava {
    public static void main(String[] args) {
        //连接本地的 Redis 服务
        Jedis jedis = new Jedis("localhost");
        System.out.println("连接成功");
        //查看服务是否运行
        System.out.println("服务正在运行: "+jedis.ping());
    }
}
```
3. Redis Java String(字符串) 实例
```java
import redis.clients.jedis.Jedis;
 
public class RedisStringJava {
    public static void main(String[] args) {
        //连接本地的 Redis 服务
        Jedis jedis = new Jedis("localhost");
        System.out.println("连接成功");
        //设置 redis 字符串数据
        jedis.set("runoobkey", "www.runoob.com");
        // 获取存储的数据并输出
        System.out.println("redis 存储的字符串为: "+ jedis.get("runoobkey"));
    }
}
```
4. Redis Java List(列表) 实例
```java
import java.util.List;
import redis.clients.jedis.Jedis;
 
public class RedisListJava {
    public static void main(String[] args) {
        //连接本地的 Redis 服务
        Jedis jedis = new Jedis("localhost");
        System.out.println("连接成功");
        //存储数据到列表中
        jedis.lpush("site-list", "Runoob");
        jedis.lpush("site-list", "Google");
        jedis.lpush("site-list", "Taobao");
        // 获取存储的数据并输出
        List<String> list = jedis.lrange("site-list", 0 ,2);
        for(int i=0; i<list.size(); i++) {
            System.out.println("列表项为: "+list.get(i));
        }
    }
}
```
5. Redis Java Keys 实例
```java
import java.util.Iterator;
import java.util.Set;
import redis.clients.jedis.Jedis;
 
public class RedisKeyJava {
    public static void main(String[] args) {
        //连接本地的 Redis 服务
        Jedis jedis = new Jedis("localhost");
        System.out.println("连接成功");
 
        // 获取数据并输出
        Set<String> keys = jedis.keys("*"); 
        Iterator<String> it=keys.iterator() ;   
        while(it.hasNext()){   
            String key = it.next();   
            System.out.println(key);   
        }
    }
}
```



## Jedis vs Lettuce

redis官方提供的java client有如图所示几种：
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305211001.png)
比较突出的是 Lettuce 和 jedis。Lettuce 和 jedis 的都是连接 Redis Server的客户端，Jedis 在实现上是直连 redis server，多线程环境下非线程安全，除非使用连接池，为每个 redis实例增加 物理连接。

Lettuce 是 一种可伸缩，线程安全，完全非阻塞的Redis客户端，多个线程可以共享一个RedisConnection,它利用Netty NIO 框架来高效地管理多个连接，从而提供了异步和同步数据访问方式，用于构建非阻塞的反应性应用程序。

在 springboot 1.5.x版本的默认的Redis客户端是 Jedis实现的，springboot 2.x版本中默认客户端是用 lettuce实现的。

下面介绍 springboot 2.0分别使用 jedis和 lettuce集成 redis服务

### springboot 2.0 通过 lettuce集成Redis服务
#### 导入依赖
```
<dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```
#### application.properties配置文件
```
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=root
# 连接池最大连接数(使用负值表示没有限制) 默认为8
spring.redis.lettuce.pool.max-active=8
# 连接池最大阻塞等待时间(使用负值表示没有限制) 默认为-1
spring.redis.lettuce.pool.max-wait=-1ms
# 连接池中的最大空闲连接 默认为8
spring.redis.lettuce.pool.max-idle=8
# 连接池中的最小空闲连接 默认为 0
spring.redis.lettuce.pool.min-idle=0
```
#### 自定义 RedisTemplate
默认情况下的模板只能支持 RedisTemplate<String,String>，只能存入字符串，很多时候，我们需要自定义 RedisTemplate ，设置序列化器，这样我们可以很方便的操作实例对象。如下所示：
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Serializable> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(connectionFactory);
        return redisTemplate;
    }
}
```
#### 定义测试实体类
```java
public class User implements Serializable {
    private static final long serialVersionUID = 4220515347228129741L;
    private Integer id;
    private String username;
    private Integer age;

    public User(Integer id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }

    public User() {
    }
    //getter/setter 省略
}
```
#### 测试
```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisTest {
    private Logger logger = LoggerFactory.getLogger(RedisTest.class);
    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    @Test
    public void test() {
        String key = "user:1";
        redisTemplate.opsForValue().set(key, new User(1,"pjmike",20));
        User user = (User) redisTemplate.opsForValue().get(key);
        logger.info("uesr: "+user.toString());
    }
}
```
### springboot 2.0 通过 jedis 集成Redis服务
#### 导入依赖
因为 springboot2.0中默认是使用 Lettuce来集成Redis服务，spring-boot-starter-data-redis默认只引入了 Lettuce包，并没有引入 jedis包支持。所以在我们需要手动引入 jedis的包，并排除掉 lettuce的包，pom.xml配置如下:
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <exclusions>
        <exclusion>
            <groupId>io.lettuce</groupId>
            <artifactId>lettuce-core</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
</dependency>
```
#### application.properties配置
使用jedis的连接池
```
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=root
spring.redis.jedis.pool.max-idle=8
spring.redis.jedis.pool.max-wait=-1ms
spring.redis.jedis.pool.min-idle=0
spring.redis.jedis.pool.max-active=8
```
#### 配置 JedisConnectionFactory
因为在 springoot 2.x版本中，默认采用的是 Lettuce实现的，所以无法初始化出 Jedis的连接对象 JedisConnectionFactory，所以我们需要手动配置并注入：
```java
public class RedisConfig {
    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        JedisConnectionFactory factory = new JedisConnectionFactory();
        return factory;
    }
```
但是启动项目后发现报出了如下的异常：
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305211101.png)
redis连接失败，springboot2.x通过以上方式集成Redis并不会读取配置文件中的 spring.redis.host等这样的配置，需要手动配置,如下：

```java
@Configuration
public class RedisConfig2 {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password}")
    private String password;
    @Bean
    public RedisTemplate<String, Serializable> redisTemplate(JedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Serializable> redisTemplate = new RedisTemplate<>();
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        return redisTemplate;
    }
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(RedisPassword.of(password));
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(config);
        return connectionFactory;
    }
}
```
通过以上方式就可以连接上 redis了，不过这里要提醒的一点就是，在springboot 2.x版本中 JedisConnectionFactory设置连接的方法已过时，如图所示：
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305211201.png)
在 springboot 2.x版本中推荐使用 RedisStandaloneConfiguration类来设置连接的端口，地址等属性

然后是单元测试，与上面 Lettuce的例子代码一样，并且测试通过。



## JedisPool
### 连接池 JedisPool，为什么要用 JedisPool
首先我们如果每次使用缓存都生成一个 Jedis 对象的话，这样意味着会建立很多 socket 连接，造成系统资源被不可控调用，甚至会导致奇怪错误的发生。

如果使用单例模式，在线程安全模式下适应不了高并发的需求，非线程安全模式又可能会出现与时间相关的错误。

因此，为了避免这些问题，引入了池的概念 JedisPool。JedissPool 是一个线程安全的网络连接池，我们可以通过 JedisPool 创建和管理 Jedis 实例，这样可以有效的解决以上问题以实现系统的高性能。

我们可以理解成项目中的数据库连接池，例如：阿里巴巴的 druid~

#### 直连和使用连接池的对比
|        | 优点                                                      | 缺点                                                         |
| :----- | :-------------------------------------------------------- | :----------------------------------------------------------- |
| 直连   | 简单方便适用于少量长期连接的场景                          | 存在每次新建/关闭TCP开销，资源无法控制，存在连接泄露的可能，Jedis对象线程不安全 |
| 连接池 | Jedis预先生成，降低开销，连接池的形式保护和控制资源的使用 | 相对于直连，使用相对麻烦，尤其在资源管理上需要很多参数来保证，一旦规划不合理也会出现问题。 |

### 如何创建 JedisPool 实例和 Jedis 实例对象
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305211501.png)
```java
private static JedisPool pool = null;

if( pool == null ){
    JedisPoolConfig config = new JedisPoolConfig();
    控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
    如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
    config.setMaxTotal(50); 
    控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
    config.setMaxIdle(5);
    表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；单位毫秒
    小于零:阻塞不确定的时间,  默认-1
    config.setMaxWaitMillis(1000*100);
    在borrow(引入)一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    config.setTestOnBorrow(true);
    return 一个jedis实例给pool时，是否检查连接可用性（ping()）
    config.setTestOnReturn(true);
    connectionTimeout 连接超时（默认2000ms）
    soTimeout 响应超时（默认2000ms）
}

// 获取实例
public static Jedis getJedis() {
    return pool.getResource();
}

// 释放 redis
public static void returnResource(Jedis jedis) {
    if(jedis != null) {
        jedis.close();
    }
}
```
### JedisPool 属性配置（JedisPoolConfig）
JedisPool的配置参数大部分是由JedisPoolConfig的对应项来赋值的。
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305211601.png)

### JedisPool优化
#### maxTotal：最大连接数
实际上这个是一个很难回答的问题，考虑的因素比较多：
- 业务希望Redis并发量
- 客户端执行命令时间
- Redis资源：例如 nodes(例如应用个数) * maxTotal 是不能超过redis的最大连接数。
- 资源开销：例如虽然希望控制空闲连接，但是不希望因为连接池的频繁释放创建连接造成不必靠开销。

以一个例子说明，假设:
- 一次命令时间（borrow|return resource + Jedis执行命令(含网络) ）的平均耗时约为1ms，一个连接的QPS大约是1000
- 业务期望的QPS是50000

那么理论上需要的资源池大小是50000 / 1000 = 50个。但事实上这是个理论值，还要考虑到要比理论值预留一些资源，通常来讲maxTotal可以比理论值大一些。

但这个值不是越大越好，一方面连接太多占用客户端和服务端资源，另一方面对于Redis这种高QPS的服务器，一个大命令的阻塞即使设置再大资源池仍然会无济于事。

#### maxIdle minIdle
maxIdle实际上才是业务需要的最大连接数，maxTotal是为了给出余量，所以maxIdle不要设置过小，否则会有new Jedis(新连接)开销，而minIdle是为了控制空闲资源监测。

连接池的最佳性能是maxTotal = maxIdle ,这样就避免连接池伸缩带来的性能干扰。但是如果并发量不大或者maxTotal设置过高，会导致不必要的连接资源浪费。
可以根据实际总OPS和调用redis客户端的规模整体评估每个节点所使用的连接池。

#### maxIdle和maxtotal参数
1. 我们一般连接redis都需要用到连接池，最常用的就是jedis连接池，连接池中有两个参数的设置对高性能有较大影响：maxIdle和maxTotal
2. maxIdle的意思是连接池中空闲连接的最大数量，maxTotal是连接池中总连接的最大数量
3. 之前我一般设置这两者的时候是没有设置成相等的值的，也就是比如设置maxIdle=10，然后maxTotal=30这样，但是基础架构的压测报告发现在高并发的情况下这样设置的后果竟然会产生大量的短连接，这样的结果令人非常意外，这些这么多的短链接是怎么产生的？
4. 回答3的问题：还是以maxIdle=10，maxTotal=30作为例子，假设时刻1，30条连接全部使用来进行redis操作，时刻2，有20条连接释放，那么将会有10条连接放回连接池中，另外的10条连接将会被close掉，成为短连接，此时其他线程再来获取比如20个连接的时候，将会需要再额外创建10条连接。这就是短连接的主要产生场景；至于创建的短连接的数量取决于cpu的调度，简单归结原因是： 连接放回连接池的速度要比等待线程从线程池中获取连接要快，这样每次释放连接的时候都有部分连接超过maxIdle数量而被物理close掉成为短链接。至于为何释放连接的速度要比获取连接的速度快，留个悬念



 ## Redission
 [github](https://github.com/redisson/redisson)
[官网](https://redisson.org/)
### 简介
 Redisson是架设在Redis基础上的一个Java驻内存数据网格（In-Memory Data Grid）。

Redisson在基于NIO的Netty框架上，充分的利用了Redis键值数据库提供的一系列优势，在Java实用工具包中常用接口的基础上，为使用者提供了一系列具有分布式特性的常用工具类。使得原本作为协调单机多线程并发程序的工具包获得了协调分布式多机多线程并发系统的能力，大大降低了设计和研发大规模分布式系统的难度。同时结合各富特色的分布式服务，更进一步简化了分布式环境中程序相互之间的协作。

redisson是redis官网推荐的java语言实现分布式锁的项目。当然，redisson远不止分布式锁，还包括其他一些分布式结构。

redission可以支持redis cluster、master-slave、redis哨兵和redis单机。

每个Redis服务实例都能管理多达1TB的内存。

能够完美的在云计算环境里使用，并且支持AWS ElastiCache主备版，AWS ElastiCache集群版，Azure Redis Cache和阿里云（Aliyun）的云数据库Redis版。
### 适用场景
 - 分布式应用
- 分布式缓存
- 分布式回话管理
- 分布式服务（任务，延迟任务，执行器）
- 分布式redis客户端

 ### Redisson功能
- 支持同步/异步/异步流/管道流方式连接
- 多样化数据序列化
- 集合数据分片
- 分布式对象
- 分布式集合
- 分布式锁和同步器
- 分布式服务
- 独立节点模式
- 三方框架整合

 ### 分布式锁
#### Maven依赖
```
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
</dependency>
或
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.10.7</version>
</dependency>
```

 #### Redission是怎么实现的？
##### 加锁机制
假设是一个redis cluster，客户端1会根据hash算法选择一个节点，发送lua脚本，之所以发送lua脚本，是因为要保证原子性。
 ```
"if(redis.call('exist'),KEYS[1]==0) then" +  // KEY[1]就是key，我们假设是mylock
    "redis.call('hset',KEYS[1],ARGV[2], 1);"+ // ARGV[2]就是客户端的ID
    "redis.call('pexpire', KEYS[1], ARGV[1]);"+ // ARGV[1]代表的就是锁key的默认生存时间，默认30秒
    "return nil;" + 
"end;" + 
"if (redis.call('hexists', KEYS[1], ARGV[2] == 1)) then" + 
    "redis.call('hincrby',KEYS[1],ARGV[2],1);" + 
    "reids.call('pexipre',KEYS[1], ARGV[1]);" +
    "return nil;" +"end" + 
"return redis.call('pttl', KEYS[1])"
 ```
客户端1成功加锁，客户端2来了，一看发现第一个if，发现mylock这个锁key已经存在了，就走第二个if，一看发现没有自己的客户端ID，所以客户端ID会获取到mylock这个key的剩余时间。之后客户端2会进入一个while循环，不停的尝试加锁。

##### watch dog自动延期
redission还提供了watch dog线程，客户端1加锁的key默认是30s，但是客户端1业务还没有执行完，时间就过了，客户端1还想持有锁的话，就会启动一个watch dog后台线程，不断的延长锁key的生存时间。
##### 可重入锁
同时，如果客户端1重复加锁，也是支持，无非就是hset +1，代表的加锁的次数+1，不过代码中记得要unlock()掉。
##### 释放锁
释放锁，其实就是将加锁次数-1，如果发现加锁次数是0，说明这个客户端已经不再持有锁，就会执行del mylock命令，从redis里把这个kv删掉，这样客户端2就可以加锁了。
##### 缺点
因为是redis cluster，这个kv会被异步复制给其他节点。但是在这过程中主节点挂了，还没来得及复制。虽然客户端1以为加锁成功了，其实这个key已经丢失。
主备切换后，客户端2也来加锁，也成功了，这样就导致了多个客户端对一个分布式锁完成了加锁，可能会造成脏数据。
#### 配置
##### 单机配置
```
@Configurationpublic class RedissonConfig {
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private String port;

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"+host+":"+port);
        return Redisson.create(config);
    }}
```

#### 分布式锁
##### 可重入锁（Reentrant Lock）
```
Redisson的分布式可重入锁RLock Java对象实现了java.util.concurrent.locks.Lock接口，同时还支持自动过期解锁。
 public void testReentrantLock(RedissonClient redisson){

        RLock lock = redisson.getLock("anyLock");
        try{
            // 1. 最常见的使用方法
            //lock.lock();

            // 2. 支持过期解锁功能,10秒钟以后自动解锁, 无需调用unlock方法手动解锁
            //lock.lock(10, TimeUnit.SECONDS);

            // 3. 尝试加锁，最多等待3秒，上锁以后10秒自动解锁
            boolean res = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if(res){    //成功
                // do your business

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }
```
Redisson同时还为分布式锁提供了异步执行的相关方法：

```
public void testAsyncReentrantLock(RedissonClient redisson){
        RLock lock = redisson.getLock("anyLock");
        try{
            lock.lockAsync();
            lock.lockAsync(10, TimeUnit.SECONDS);
            Future<Boolean> res = lock.tryLockAsync(3, 10, TimeUnit.SECONDS);

            if(res.get()){
                // do your business

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

    }
```
##### 公平锁（Fair Lock）
Redisson分布式可重入公平锁也是实现了java.util.concurrent.locks.Lock接口的一种RLock对象。在提供了自动过期解锁功能的同时，保证了当多个Redisson客户端线程同时请求加锁时，优先分配给先发出请求的线程。
```
public void testFairLock(RedissonClient redisson){

        RLock fairLock = redisson.getFairLock("anyLock");
        try{
            // 最常见的使用方法
            fairLock.lock();

            // 支持过期解锁功能, 10秒钟以后自动解锁,无需调用unlock方法手动解锁
            fairLock.lock(10, TimeUnit.SECONDS);

            // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
            boolean res = fairLock.tryLock(100, 10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            fairLock.unlock();
        }
    }
```
Redisson同时还为分布式可重入公平锁提供了异步执行的相关方法：
```
RLock fairLock = redisson.getFairLock("anyLock");
fairLock.lockAsync();
fairLock.lockAsync(10, TimeUnit.SECONDS);
Future<Boolean> res = fairLock.tryLockAsync(100, 10, TimeUnit.SECONDS);
```

##### 联锁（MultiLock）
Redisson的RedissonMultiLock对象可以将多个RLock对象关联为一个联锁，每个RLock对象实例可以来自于不同的Redisson实例。
```
public void testMultiLock(RedissonClient redisson1,
                              RedissonClient redisson2, RedissonClient redisson3){

        RLock lock1 = redisson1.getLock("lock1");
        RLock lock2 = redisson2.getLock("lock2");
        RLock lock3 = redisson3.getLock("lock3");

        RedissonMultiLock lock = new RedissonMultiLock(lock1, lock2, lock3);

        try {
            // 同时加锁：lock1 lock2 lock3, 所有的锁都上锁成功才算成功。
            lock.lock();

            // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
```
##### 红锁（RedLock）
 Redisson的RedissonRedLock对象实现了Redlock介绍的加锁算法。该对象也可以用来将多个RLock
对象关联为一个红锁，每个RLock对象实例可以来自于不同的Redisson实例。
```
    public void testRedLock(RedissonClient redisson1,
                              RedissonClient redisson2, RedissonClient redisson3){

        RLock lock1 = redisson1.getLock("lock1");
        RLock lock2 = redisson2.getLock("lock2");
        RLock lock3 = redisson3.getLock("lock3");

        RedissonRedLock lock = new RedissonRedLock(lock1, lock2, lock3);
      try {
            // 同时加锁：lock1 lock2 lock3, 红锁在大部分节点上加锁成功就算成功。
            lock.lock();

            // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
```
##### 读写锁（ReadWriteLock）
 Redisson的分布式可重入读写锁RReadWriteLock Java对象实现了java.util.concurrent.locks.ReadWriteLock接口。同时还支持自动过期解锁。该对象允许同时有多个读取锁，但是最多只能有一个写入锁。
```
RReadWriteLock rwlock = redisson.getLock("anyRWLock");
// 最常见的使用方法
rwlock.readLock().lock();
// 或
rwlock.writeLock().lock();

// 支持过期解锁功能
// 10秒钟以后自动解锁
// 无需调用unlock方法手动解锁
rwlock.readLock().lock(10, TimeUnit.SECONDS);
// 或
rwlock.writeLock().lock(10, TimeUnit.SECONDS);

// 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
boolean res = rwlock.readLock().tryLock(100, 10, TimeUnit.SECONDS);// 或
boolean res = rwlock.writeLock().tryLock(100, 10, TimeUnit.SECONDS);
...
lock.unlock();
```
##### 信号量（Semaphore）
Redisson的分布式信号量（Semaphore）Java对象RSemaphore采用了与java.util.concurrent.Semaphore相似的接口和用法。
```
RSemaphore semaphore = redisson.getSemaphore("semaphore");
semaphore.acquire();
//或
semaphore.acquireAsync();
semaphore.acquire(23);
semaphore.tryAcquire();
//或
semaphore.tryAcquireAsync();
semaphore.tryAcquire(23, TimeUnit.SECONDS);
//或
semaphore.tryAcquireAsync(23, TimeUnit.SECONDS);
semaphore.release(10);
semaphore.release();
//或
semaphore.releaseAsync();
```
##### 可过期性信号量（PermitExpirableSemaphore）
Redisson的可过期性信号量（PermitExpirableSemaphore）实在RSemaphore对象的基础上，为每个信号增加了一个过期时间。每个信号可以通过独立的ID来辨识，释放时只能通过提交这个ID才能释放。
```
RPermitExpirableSemaphore semaphore = redisson.getPermitExpirableSemaphore("mySemaphore");
String permitId = semaphore.acquire();
// 获取一个信号，有效期只有2秒钟。
String permitId = semaphore.acquire(2, TimeUnit.SECONDS);
// ...
semaphore.release(permitId);
```
##### 闭锁（CountDownLatch）
Redisson的分布式闭锁（CountDownLatch）Java对象RCountDownLatch采用了与java.util.concurrent.CountDownLatch相似的接口和用法。
```
RCountDownLatch latch = redisson.getCountDownLatch("anyCountDownLatch");
latch.trySetCount(1);
latch.await();

// 在其他线程或其他JVM里
RCountDownLatch latch = redisson.getCountDownLatch("anyCountDownLatch");
latch.countDown();
```

### 使用 RList 操作 Redis 列表
下面的代码简单演示了如何在 Redisson 中使用 RList 对象。RList 是 Java 的 List 集合的分布式并发实现。考虑以下代码：
```
 import org.redisson.Redisson;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

public class ListExamples {
    public static void main(String[] args) {
        // 默认连接上 127.0.0.1:6379
        RedissonClient client = Redisson.create();

        // RList 继承了 java.util.List 接口
        RList<String> nameList = client.getList("nameList");
        nameList.clear();
        nameList.add("bingo");
        nameList.add("yanglbme");
        nameList.add("https://github.com/yanglbme");
        nameList.remove(-1);

        boolean contains = nameList.contains("yanglbme");
        System.out.println("List size: " + nameList.size());
        System.out.println("Is list contains name 'yanglbme': " + contains);
        nameList.forEach(System.out::println);

        client.shutdown();
    }
}
```
运行上面的代码时，可以获得以下输出：
```
 List size: 2
Is list contains name 'yanglbme': true
bingo
yanglbme
```
### 使用 RMap 操作 Redis 哈希
Redisson 还包括 RMap，它是 Java Map 集合的分布式并发实现，考虑以下代码：
```
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

public class MapExamples {
    public static void main(String[] args) {
        // 默认连接上127.0.0.1:6379
        RedissonClient client = Redisson.create();
        // RMap 继承了 java.util.concurrent.ConcurrentMap 接口
        RMap<String, String> map = client.getMap("personalInfo");
        map.put("name", "yanglbme");
        map.put("address", "Shenzhen");
        map.put("link", "https://github.com/yanglbme");

        boolean contains = map.containsKey("link");
        System.out.println("Map size: " + map.size());
        System.out.println("Is map contains key 'link': " + contains);
        String value = map.get("name");
        System.out.println("Value mapped by key 'name': " + value);
        boolean added = map.putIfAbsent("link", "https://doocs.github.io") == null;
        System.out.println("Is value mapped by key 'link' added: " + added);
        client.shutdown();
    }
}
```
运行上面的代码时，将会看到以下输出：
```
Map size: 3
Is map contains key 'link': true
Value mapped by key 'name': yanglbme
Is value mapped by key 'link' added: false
```

### 使用 RAtomicLong 实现 Redis 原子操作
RAtomicLong 是 Java 中 AtomicLong 类的分布式“替代品”，用于在并发环境中保存长值。以下示例代码演示了 RAtomicLong 的用法：
```
import org.redisson.Redisson;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;

public class AtomicLongExamples {
    public static void main(String[] args) {
        // 默认连接上127.0.0.1:6379
        RedissonClient client = Redisson.create();
        RAtomicLong atomicLong = client.getAtomicLong("myLong");

        System.out.println("Init value: " + atomicLong.get());

        atomicLong.incrementAndGet();
        System.out.println("Current value: " + atomicLong.get());

        atomicLong.addAndGet(10L);
        System.out.println("Final value: " + atomicLong.get());

        client.shutdown();
    }
}
```
此代码的输出将是：
```
Init value: 0
Current value: 1
Final value: 11
```



## Redis 客户端 Jedis、lettuce 和 Redisson 对比

Redis 支持多种语言的客户端，下面列举了部分 Redis 支持的客户端语言，大家可以通过[官网](https://redis.io/clients)查看 Redis 支持的客户端详情。

- C语言
- C++
- C#
- Java
- [Python](https://redis.io/clients#python)
- Node.js
- PHP

Redis 是用单线程来处理多个客户端的访问，因此作为 Redis 的开发和运维人员需要了解 Redis 服务端和客户端的通信协议，以及主流编程语言的 Redis 客户端使用方法，同时还需要了解客户端管理的相应 API 以及开发运维中可能遇到的问题。

### Redis 客户端通信协议

Redis制定了RESP（Redis Serialization Protocol，Redis序列化协议）实现客户端与服务端的正常交互，这种协议简单高效，既能够被机器解析，又容易被人类识别。

`RESP`可以序列化不同的数据类型，如整型、字符串、数组还有一种特殊的`Error`类型。需要执行的`Redis`命令会封装为类似于**字符串数组**的请求然后通过`Redis`客户端发送到`Redis`服务端。`Redis`服务端会基于特定的命令类型选择对应的一种数据类型进行回复。

**1. RESP 发送命令格式**

在`RESP`中，发送的数据类型取决于数据报的第一个字节：

- 单行字符串的第一个字节为`+`。
- 错误消息的第一个字节为`-`。
- 整型数字的第一个字节为`:`。
- 定长字符串的第一个字节为`$`。
- `RESP`数组的第一个字节为`*`。

| 数据类型        | 本文翻译名称 | 基本特征                                                     | 例子                           |
| :-------------- | :----------- | :----------------------------------------------------------- | :----------------------------- |
| `Simple String` | 单行字符串   | 第一个字节是`+`，最后两个字节是`\r\n`，其他字节是字符串内容  | `+OK\r\n`                      |
| `Error`         | 错误消息     | 第一个字节是`-`，最后两个字节是`\r\n`，其他字节是异常消息的文本内容 | `-ERR\r\n`                     |
| `Integer`       | 整型数字     | 第一个字节是`:`，最后两个字节是`\r\n`，其他字节是数字的文本内容 | `:100\r\n`                     |
| `Bulk String`   | 定长字符串   | 第一个字节是`$`，紧接着的字节是`内容字符串长度\r\n`，最后两个字节是`\r\n`，其他字节是字符串内容 | `$4\r\ndoge\r\n`               |
| `Array`         | `RESP`数组   | 第一个字节是`*`，紧接着的字节是`元素个数\r\n`，最后两个字节是`\r\n`，其他字节是各个元素的内容，每个元素可以是任意一种数据类型 | `*2\r\n:100\r\n$4\r\ndoge\r\n` |

发送的命令格式如下，CRLF代表"\r\n":

```
*<参数数量> CRLF
$<参数1的字节数量> CRLF
<参数1> CRLF
...
$<参数N的字节数量> CRLF
<参数N> CRLF
```

以`set hello world`这个命令为例，发送的内容就是这样的：

```
*3
$3
SET
$5
hello
$5
world
```

第一行*3表示有3个参数，3表示接下来的一个参数有3个字节，接下来是参数，3表示接下来的一个参数有3个字节，接下来是参数，5表示下一个参数有5个字节，接下来是参数，$5表示下一个参数有5个字节，接下来是参数。

所以set hello world最终发送给redis服务器的命令是：

```
*3\r\n$3\r\nSET\r\n$5\r\nhello\r\n$5\r\nworld\r\n
```

**2. RESP 响应内容**

```
Redis的返回结果类型分为以下五种：
        正确回复：在RESP中第一个字节为"+"
        错误回复：在RESP中第一个字节为"-"
        整数回复：在RESP中第一个字节为":"
        字符串回复：在RESP中第一个字节为"$"
        多条字符串回复：在RESP中第一个字节为"*"

(+) 表示一个正确的状态信息，具体信息是当前行+后面的字符。
(-)  表示一个错误信息，具体信息是当前行－后面的字符。
(*) 表示消息体总共有多少行，不包括当前行,*后面是具体的行数。
($) 表示下一行数据长度，不包括换行符长度\r\n,$后面则是对应的长度的数据。
(:) 表示返回一个数值，：后面是相应的数字节符。
```

![image-20210305221406248](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210305221406248.png)

![image-20210305221432871](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210305221432871.png)

有了这个协议，我们就可以编写程序来和 Redis 服务端进行通信。由于 Redis 的流行，已经存在了很多流行的开源客户端。本文主要选择 Java 领域 Redis 官方推荐的客户端进行介绍。

### Redis 的 Java 客户端

Redis 官方推荐的 Java 客户端有Jedis、lettuce 和 Redisson。

**1. Jedis**

Jedis 是老牌的 Redis 的 Java 实现客户端，提供了比较全面的 Redis 命令的支持，其官方网址是：http://tool.oschina.net/uploads/apidocs/redis/clients/jedis/Jedis.html。

优点：

- 支持全面的 Redis 操作特性（可以理解为API比较全面）。

缺点：

- 使用阻塞的 I/O，且其方法调用都是同步的，程序流需要等到 sockets 处理完 I/O 才能执行，不支持异步；
- Jedis 客户端实例不是线程安全的，所以需要通过连接池来使用 Jedis。

**2. lettuce**

lettuce （[ˈletɪs]），是一种可扩展的线程安全的 Redis 客户端，支持异步模式。如果避免阻塞和事务操作，如BLPOP和MULTI/EXEC，多个线程就可以共享一个连接。lettuce 底层基于 Netty，支持高级的 Redis 特性，比如哨兵，集群，管道，自动重新连接和Redis数据模型。lettuce 的官网地址是：https://lettuce.io/

优点：

- 支持同步异步通信模式；
- Lettuce 的 API 是线程安全的，如果不是执行阻塞和事务操作，如BLPOP和MULTI/EXEC，多个线程就可以共享一个连接。

**3. Redisson**

Redisson 是一个在 Redis 的基础上实现的 Java 驻内存数据网格（In-Memory Data Grid）。它不仅提供了一系列的分布式的 Java 常用对象，还提供了许多分布式服务。其中包括( BitSet, Set, Multimap, SortedSet, Map, List, Queue, BlockingQueue, Deque, BlockingDeque, Semaphore, Lock, AtomicLong, CountDownLatch, Publish / Subscribe, Bloom filter, Remote service, Spring cache, Executor service, Live Object service, Scheduler service) Redisson 提供了使用Redis 的最简单和最便捷的方法。Redisson 的宗旨是促进使用者对Redis的关注分离（Separation of Concern），从而让使用者能够将精力更集中地放在处理业务逻辑上。Redisson的官方网址是：https://redisson.org/

优点：

- 使用者对 Redis 的关注分离，可以类比 Spring 框架，这些框架搭建了应用程序的基础框架和功能，提升开发效率，让开发者有更多的时间来关注业务逻辑；
- 提供很多分布式相关操作服务，例如，分布式锁，分布式集合，可通过Redis支持延迟队列等。

缺点：

- Redisson 对字符串的操作支持比较差。

**4. 使用建议**

结论：lettuce + Redisson

Jedis 和 lettuce 是比较纯粹的 Redis 客户端，几乎没提供什么高级功能。Jedis 的性能比较差，所以如果你不需要使用 Redis 的高级功能的话，优先推荐使用 lettuce。

Redisson 的优势是提供了很多开箱即用的 Redis 高级功能，如果你的应用中需要使用到 Redis 的高级功能，建议使用 Redisson。具体 Redisson 的高级功能可以参考：https://redisson.org/

## 参考

- RESP协议1：https://www.cnblogs.com/4a8a08f09d37b73795649038408b5f33/p/9998245.html
- RESP协议2：https://my.oschina.net/u/2474629/blog/913805
- RESP协议3：https://www.cnblogs.com/throwable/p/11644790.html
- Redis的三个框架：Jedis,Redisson,Lettuce：https://www.cnblogs.com/williamjie/p/11287292.html
- redis客户端选型-Jedis、lettuce、Redisson：https://blog.csdn.net/a5569449/article/details/106891111/

作者：程序员自由之路

出处：https://www.cnblogs.com/54chensongxia/p/13815761.html

版权：本作品采用「[署名-非商业性使用-相同方式共享 4.0 国际](https://creativecommons.org/licenses/by-nc-sa/4.0/)」许可协议进行许可。









## shiro-redis-sentinel

[springboot+shiro-redis 使用Redis sentinel（哨兵）主从实现](https://blog.51cto.com/1745012/2115011)
[SoftWindDay/shiro-redis-sentinel](https://github.com/SoftWindDay/shiro-redis-sentinel)

















