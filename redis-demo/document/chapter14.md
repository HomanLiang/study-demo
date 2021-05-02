[toc]



# Redis 应用场景





## 1.Redis分布式锁

### 1.1.相关文章

[Redis高级项目实战，都0202年了，还不会Redis？](https://www.cnblogs.com/chenyanbin/p/13506946.html)

### 1..Redisson 是如何实现分布式锁的？

#### 1.2.1.Maven配置

```
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>2.2.12</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-annotations</artifactId>
    <version>2.6.0</version>
</dependency>
```

#### 1.2.2.RedissonLock简单示例

redission支持4种连接redis方式，分别为单机、主从、Sentinel、Cluster 集群，项目中使用的连接方式是Sentinel。

redis服务器不在本地的同学请注意权限问题。

**Sentinel配置**

```
Config config = new Config();
config.useSentinelServers().addSentinelAddress("127.0.0.1:6479", "127.0.0.1:6489").setMasterName("master").setPassword("password").setDatabase(0);
RedissonClient redisson = Redisson.create(config);
```

**简单使用**

```
RLock lock = redisson.getLock("test_lock");
try{
    boolean isLock=lock.tryLock();
    if(isLock){
        doBusiness();
    }
}catch(exception e){
}finally{
    lock.unlock();
}
```

#### 1.2.3.源码中使用到的Redis命令

分布式锁主要需要以下redis命令，这里列举一下。在源码分析部分可以继续参照命令的操作含义。

1. EXISTS key :当 key 存在，返回1；若给定的 key 不存在，返回0。
2. GETSET key value:将给定 key 的值设为 value ，并返回 key 的旧值 (old value)，当 key 存在但不是字符串类型时，返回一个错误，当key不存在时，返回nil。
3. GET key:返回 key 所关联的字符串值，如果 key 不存在那么返回 nil。
4. DEL key [KEY …]:删除给定的一个或多个 key ,不存在的 key 会被忽略,返回实际删除的key的个数（integer）。
5. HSET key field value：给一个key 设置一个{field=value}的组合值，如果key没有就直接赋值并返回1，如果field已有，那么就更新value的值，并返回0.
6. HEXISTS key field:当key中存储着field的时候返回1，如果key或者field至少有一个不存在返回0。
7. HINCRBY key field increment:将存储在key中的哈希（Hash）对象中的指定字段field的值加上增量increment。如果键key不存在，一个保存了哈希对象的新建将被创建。如果字段field不存在，在进行当前操作前，其将被创建，且对应的值被置为0，返回值是增量之后的值
8. PEXPIRE key milliseconds：设置存活时间，单位是毫秒。expire操作单位是秒。
9. PUBLISH channel message:向channel post一个message内容的消息，返回接收消息的客户端数。

#### 1.2.4.源码中使用到的lua脚本语义

Redisson源码中，执行redis命令的是lua脚本，其中主要用到如下几个概念。

- redis.call() 是执行redis命令.
- KEYS[1] 是指脚本中第1个参数
- ARGV[1] 是指脚本中第一个参数的值
- 返回值中nil与false同一个意思。

需要注意的是，在redis执行lua脚本时，相当于一个redis级别的锁，不能执行其他操作，类似于原子操作，也是redisson实现的一个关键点。

另外，如果lua脚本执行过程中出现了异常或者redis服务器直接宕掉了，执行redis的根据日志回复的命令，会将脚本中已经执行的命令在日志中删除。

#### 1.2.5.源码分析

##### 1.2.5.1.RLOCK结构

```java
public interface RLock extends Lock, RExpirable {
    void lockInterruptibly(long leaseTime, TimeUnit unit) throws InterruptedException;
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;
    void lock(long leaseTime, TimeUnit unit);
    void forceUnlock();
    boolean isLocked();
    boolean isHeldByCurrentThread();
    int getHoldCount();
    Future<Void> unlockAsync();
    Future<Boolean> tryLockAsync();
    Future<Void> lockAsync();
    Future<Void> lockAsync(long leaseTime, TimeUnit unit);
    Future<Boolean> tryLockAsync(long waitTime, TimeUnit unit);
    Future<Boolean> tryLockAsync(long waitTime, long leaseTime, TimeUnit unit);
}
```

该接口主要继承了Lock接口, 并扩展了部分方法, 比如:boolean tryLock(long waitTime, long leaseTime, TimeUnit unit)新加入的leaseTime主要是用来设置锁的过期时间, 如果超过leaseTime还没有解锁的话, redis就强制解锁. leaseTime的默认时间是30s

##### 1.2.5.2.RedissonLock获取锁 tryLock源码

```java
Future<Long> tryLockInnerAsync(long leaseTime, TimeUnit unit, long threadId) {
       internalLockLeaseTime = unit.toMillis(leaseTime);
       return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_LONG,
                 "if (redis.call('exists', KEYS[1]) == 0) then " +
                     "redis.call('hset', KEYS[1], ARGV[2], 1); " +
                     "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                     "return nil; " +
                 "end; " +
                 "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then " +
                     "redis.call('hincrby', KEYS[1], ARGV[2], 1); " +
                     "redis.call('pexpire', KEYS[1], ARGV[1]); " +
                     "return nil; " +
                 "end; " +
                 "return redis.call('pttl', KEYS[1]);",
                   Collections.<Object>singletonList(getName()), internalLockLeaseTime, getLockName(threadId));
   }
```

其中：

- KEYS[1] 表示的是 getName() ，代表的是锁名 test_lock
- ARGV[1] 表示的是 internalLockLeaseTime 默认值是30s
- ARGV[2] 表示的是 getLockName(threadId) 代表的是 id:threadId 用锁对象id+线程id， 表示当前访问线程，用于区分不同服务器上的线程。

逐句分析：

```
if (redis.call('exists', KEYS[1]) == 0) then
         redis.call('hset', KEYS[1], ARGV[2], 1);
         redis.call('pexpire', KEYS[1], ARGV[1]);
         return nil;
         end;
```

`if (redis.call(‘exists’, KEYS[1]) == 0)` 如果锁名称不存在

`then redis.call(‘hset’, KEYS[1], ARGV[2],1)` 则向redis中添加一个key为test_lock的set，并且向set中添加一个field为线程id，值=1的键值对，表示此线程的重入次数为1

`redis.call(‘pexpire’, KEYS[1], ARGV[1])` 设置set的过期时间，防止当前服务器出问题后导致死锁，`return nil; end;` 返回 `nil`  结束

```lua
if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then
         redis.call('hincrby', KEYS[1], ARGV[2], 1);
         redis.call('pexpire', KEYS[1], ARGV[1]);
         return nil;
         end;
```

`if (redis.call(‘hexists’, KEYS[1], ARGV[2]) == 1)` 如果锁是存在的，检测是否是当前线程持有锁，如果是当前线程持有锁

`then redis.call(‘hincrby’, KEYS[1], ARGV[2], 1)`则将该线程重入的次数++

`redis.call(‘pexpire’, KEYS[1], ARGV[1])` 并且重新设置该锁的有效时间

`return nil; end;`返回nil，结束

```lua
return redis.call('pttl', KEYS[1]);
```

锁存在, 但不是当前线程加的锁，则返回锁的过期时间。

##### 1.2.5.3.RedissonLock解锁 unlock源码

```java
	@Override
    public void unlock() {
        Boolean opStatus = commandExecutor.evalWrite(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                        "if (redis.call('exists', KEYS[1]) == 0) then " +
                            "redis.call('publish', KEYS[2], ARGV[1]); " +
                            "return 1; " +
                        "end;" +
                        "if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then " +
                            "return nil;" +
                        "end; " +
                        "local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1); " +
                        "if (counter > 0) then " +
                            "redis.call('pexpire', KEYS[1], ARGV[2]); " +
                            "return 0; " +
                        "else " +
                            "redis.call('del', KEYS[1]); " +
                            "redis.call('publish', KEYS[2], ARGV[1]); " +
                            "return 1; "+
                        "end; " +
                        "return nil;",
                        Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage, internalLockLeaseTime, getLockName(Thread.currentThread().getId()));
        if (opStatus == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                    + id + " thread-id: " + Thread.currentThread().getId());
        }
        if (opStatus) {
            cancelExpirationRenewal();
        }
    }
```

其中：

- KEYS[1] 表示的是getName() 代表锁名test_lock
- KEYS[2] 表示getChanelName() 表示的是发布订阅过程中使用的Chanel
- ARGV[1] 表示的是LockPubSub.unLockMessage 是解锁消息，实际代表的是数字 0，代表解锁消息
- ARGV[2] 表示的是internalLockLeaseTime 默认的有效时间 30s
- ARGV[3] 表示的是getLockName(thread.currentThread().getId())，是当前锁id+线程id

语义分析:

```
if (redis.call('exists', KEYS[1]) == 0) then
         redis.call('publish', KEYS[2], ARGV[1]);
         return 1;
         end;
```

`if (redis.call(‘exists’, KEYS[1]) == 0)` 如果锁已经不存在(可能是因为过期导致不存在，也可能是因为已经解锁)

`then redis.call(‘publish’, KEYS[2], ARGV[1])` 则发布锁解除的消息

`return 1; end` 返回1结束

```
if (redis.call('hexists', KEYS[1], ARGV[3]) == 0) then
         return nil;
         end;
```

`if (redis.call(‘hexists’, KEYS[1], ARGV[3]) == 0)` 如果锁存在，但是若果当前线程不是加锁的线

`then return nil;end`则直接返回nil 结束

```
local counter = redis.call('hincrby', KEYS[1], ARGV[3], -1);
if (counter > 0) then
         redis.call('pexpire', KEYS[1], ARGV[2]);
         return 0;
else
         redis.call('del', KEYS[1]);
         redis.call('publish', KEYS[2], ARGV[1]);
         return 1;
end;
```

`local counter = redis.call(‘hincrby’, KEYS[1], ARGV[3], -1)` 如果是锁是当前线程所添加，定义变量counter，表示当前线程的重入次数-1,即直接将重入次数-1

`if (counter > 0)`如果重入次数大于0，表示该线程还有其他任务需要执行

`then redis.call(‘pexpire’, KEYS[1], ARGV[2])` 则重新设置该锁的有效时间

`return 0` 返回0结束

`else redis.call(‘del’, KEYS[1])`否则表示该线程执行结束，删除该锁

`redis.call(‘publish’, KEYS[2], ARGV[1])`并且发布该锁解除的消息

`return 1; end;`返回1结束

```
return nil;
```

其他情况返回nil并结束

```
if (opStatus == null) {
            throw new IllegalMonitorStateException("attempt to unlock lock, not locked by current thread by node id: "
                    + id + " thread-id: " + Thread.currentThread().getId());
        }
```

脚本执行结束之后，如果返回值不是0或1，即当前线程去解锁其他线程的加锁时，抛出异常。

##### 1.2.5.4.RedissonLock强制解锁源码

```java
	@Override
    public void forceUnlock() {
        get(forceUnlockAsync());
    }
    Future<Boolean> forceUnlockAsync() {
        cancelExpirationRenewal();
        return commandExecutor.evalWriteAsync(getName(), LongCodec.INSTANCE, RedisCommands.EVAL_BOOLEAN,
                "if (redis.call('del', KEYS[1]) == 1) then "
                + "redis.call('publish', KEYS[2], ARGV[1]); "
                + "return 1 "
                + "else "
                + "return 0 "
                + "end",
                Arrays.<Object>asList(getName(), getChannelName()), LockPubSub.unlockMessage);
    }
```

以上是强制解锁的源码,在源码中并没有找到forceUnlock()被调用的痕迹(也有可能是我没有找对),但是forceUnlockAsync()方法被调用的地方很多，大多都是在清理资源时删除锁。此部分比较简单粗暴，删除锁成功则并发布锁被删除的消息，返回1结束，否则返回0结束。

#### 1.2.6.总结

这里只是简单的一个redisson分布式锁的测试用例，并分析了执行lua脚本这部分，如果要继续分析执行结束之后的操作，需要进行netty源码分析 ，redisson使用了netty完成异步和同步的处理。





## 2.Redis分布式限流器

### 2.1.什么是限流？为什么要限流？

不知道大家有没有坐过帝都的地铁，就是进地铁站都要排队的那种，为什么要这样摆长龙转圈圈？答案就是为了限流！因为一趟地铁的运力是有限的，一下挤进去太多人会造成站台的拥挤、列车的超载，存在一定的安全隐患。同理，我们的程序也是一样，它处理请求的能力也是有限的，一旦请求多到超出它的处理极限就会崩溃。为了不出现最坏的崩溃情况，只能耽误一下大家进站的时间。

限流是保证系统高可用的重要手段！

由于互联网公司的流量巨大，系统上线会做一个流量峰值的评估，尤其是像各种秒杀促销活动，为了保证系统不被巨大的流量压垮，会在系统流量到达一定阈值时，拒绝掉一部分流量。

限流会导致用户在短时间内（这个时间段是毫秒级的）系统不可用，一般我们衡量系统处理能力的指标是每秒的QPS或者TPS，假设系统每秒的流量阈值是1000，理论上一秒内有第1001个请求进来时，那么这个请求就会被限流。

### 2.2.限流方案

#### 2.2.1.计数器

Java内部也可以通过原子类计数器AtomicInteger、Semaphore信号量来做简单的限流。

```
// 限流的个数
private int maxCount = 10;
// 指定的时间内
private long interval = 60;
// 原子类计数器
private AtomicInteger atomicInteger = new AtomicInteger(0);
// 起始时间
private long startTime = System.currentTimeMillis();

public boolean limit(int maxCount, int interval) {
    atomicInteger.addAndGet(1);
    if (atomicInteger.get() == 1) {
        startTime = System.currentTimeMillis();
        atomicInteger.addAndGet(1);
        return true;
    }
    // 超过了间隔时间，直接重新开始计数
    if (System.currentTimeMillis() - startTime > interval * 1000) {
        startTime = System.currentTimeMillis();
        atomicInteger.set(1);
        return true;
    }
    // 还在间隔时间内,check有没有超过限流的个数
    if (atomicInteger.get() > maxCount) {
        return false;
    }
    return true;
} 
```



#### 2.2.2.漏桶算法

漏桶算法思路很简单，我们把水比作是请求，漏桶比作是系统处理能力极限，水先进入到漏桶里，漏桶里的水按一定速率流出，当流出的速率小于流入的速率时，由于漏桶容量有限，后续进入的水直接溢出（拒绝请求），以此实现限流。

![image-20210304222955627](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304222955627.png)



#### 2.2.3.令牌桶算法

令牌桶算法的原理也比较简单，我们可以理解成医院的挂号看病，只有拿到号以后才可以进行诊病。

系统会维护一个令牌（token）桶，以一个恒定的速度往桶里放入令牌（token），这时如果有请求进来想要被处理，则需要先从桶里获取一个令牌（token），当桶里没有令牌（token）可取时，则该请求将被拒绝服务。令牌桶算法通过控制桶的容量、发放令牌的速率，来达到对请求的限制。

![image-20210304223047064](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304223047064.png)



#### 2.2.4.Redis + Lua

很多同学不知道Lua是啥？个人理解，Lua脚本和 MySQL数据库的存储过程比较相似，他们执行一组命令，所有命令的执行要么全部成功或者失败，以此达到原子性。也可以把Lua脚本理解为，一段具有业务逻辑的代码块。

而Lua本身就是一种编程语言，虽然redis 官方没有直接提供限流相应的API，但却支持了 Lua 脚本的功能，可以使用它实现复杂的令牌桶或漏桶算法，也是分布式系统中实现限流的主要方式之一。

相比Redis事务，Lua脚本的优点：

- 减少网络开销：使用Lua脚本，无需向Redis发送多次请求，执行一次即可，减少网络传输
- 原子操作：Redis将整个Lua脚本作为一个命令执行，原子，无需担心并发
- 复用：Lua脚本一旦执行，会永久保存 Redis 中，其他客户端可复用


Lua脚本大致逻辑如下：

```
-- 获取调用脚本时传入的第一个key值（用作限流的 key）
local key = KEYS[1]
-- 获取调用脚本时传入的第一个参数值（限流大小）
local limit = tonumber(ARGV[1])
-- 获取当前流量大小
local curentLimit = tonumber(redis.call('get', key) or "0")
-- 是否超出限流
if curentLimit + 1 > limit then
	-- 返回(拒绝)
	return 0
else
	-- 没有超出 value + 1
	redis.call("INCRBY", key, 1)
	-- 设置过期时间
	redis.call("EXPIRE", key, 2)
	-- 返回(放行)
	return 1
end
```

- 通过KEYS[1] 获取传入的key参数
- 通过ARGV[1]获取传入的limit参数
- redis.call方法，从缓存中get和key相关的值，如果为null那么就返回0
- 接着判断缓存中记录的数值是否会大于限制大小，如果超出表示该被限流，返回0
- 如果未超过，那么该key的缓存值+1，并设置过期时间为1秒钟以后，并返回缓存值+1


这种方式是推荐的方案，具体实现会在后边做细说。

#### 2.2.5.网关层限流

限流常在网关这一层做，比如Nginx、Openresty、Kong、Zuul、Spring Cloud Gateway等，而像spring cloud - gateway网关限流底层实现原理，就是基于Redis + Lua，通过内置Lua限流脚本的方式。

![image-20210304223228015](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304223228015.png)



### 2.3.Redis + Lua限流实现

#### 2.3.1.引入依赖包

pom文件中添加如下依赖包，比较关键的就是 `spring-boot-starter-data-redis` 和 `spring-boot-starter-aop`

```
<dependencies>
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
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
        <version>21.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.junit.vintage</groupId>
                <artifactId>junit-vintage-engine</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```

#### 2.3.2.配置application.properties

在application.properties文件中配置提前搭建好的Redis服务地址和端口。

```
spring.redis.host=127.0.0.1
spring.redis.port=6379
```

#### 2.3.3.配置RedisTemplate实例

```java
@Configuration
public class RedisLimiterHelper {
	@Bean
	public RedisTemplate<String, Serializable> limitRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
    	RedisTemplate<String, Serializable> template = new RedisTemplate<>();
    	template.setKeySerializer(new StringRedisSerializer());
    	template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    	template.setConnectionFactory(redisConnectionFactory);
    	return template;
	}
} 
```


限流类型枚举类：	

```java
/**
* @author fu
* @description 限流类型
* @date 2020/4/8 13:47
*/
public enum LimitType {

	/**
	 * 自定义key
	 */
	CUSTOMER,

	/**
	 * 请求者IP
	 */
	IP;
} 
```

#### 2.3.4.自定义注解

我们自定义个`@Limit`注解，注解类型为`ElementType.METHOD`即作用于方法上。

`period`表示请求限制时间段，`count`表示在`period`这个时间段内允许放行请求的次数。`limitType`代表限流的类型，可以根据请求的`IP`、自定义`key`，如果不传`limitType`属性则默认用方法名作为默认`key`。

```java
/**
* @author fu
* @description 自定义限流注解
* @date 2020/4/8 13:15
*/
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Limit {

	/**
	 * 名字
	 */
	String name() default "";

	/**
	 * key
	 */
	String key() default "";

	/**
	 * Key的前缀
	 */
	String prefix() default "";

	/**
	 * 给定的时间范围 单位(秒)
	 */
	int period();

	/**
	 * 一定时间内最多访问次数
	 */
	int count();

	/**
	 * 限流的类型(用户自定义key或者请求ip)
	 */
	LimitType limitType() default LimitType.CUSTOMER;
} 
```

#### 2.3.5.切面代码实现

```java
/**
* @author fu
* @description 限流切面实现
* @date 2020/4/8 13:04
*/
@Aspect
@Configuration
public class LimitInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(LimitInterceptor.class);

	private static final String UNKNOWN = "unknown";

	private final RedisTemplate<String, Serializable> limitRedisTemplate;

	@Autowired
	public LimitInterceptor(RedisTemplate<String, Serializable> limitRedisTemplate) {
		this.limitRedisTemplate = limitRedisTemplate;
	}

	/**
	 * @param pjp
	 * @author fu
	 * @description 切面
	 * @date 2020/4/8 13:04
	 */
	@Around("execution(public * *(..)) && @annotation(com.xiaofu.limit.api.Limit)")
	public Object interceptor(ProceedingJoinPoint pjp) {
		MethodSignature signature = (MethodSignature) pjp.getSignature();
		Method method = signature.getMethod();
		Limit limitAnnotation = method.getAnnotation(Limit.class);
		LimitType limitType = limitAnnotation.limitType();
		String name = limitAnnotation.name();
		String key;
		int limitPeriod = limitAnnotation.period();
		int limitCount = limitAnnotation.count();

		/**
		 * 根据限流类型获取不同的key ,如果不传我们会以方法名作为key
		 */
		switch (limitType) {
			case IP:
				key = getIpAddress();
				break;
			case CUSTOMER:
				key = limitAnnotation.key();
				break;
			default:
				key = StringUtils.upperCase(method.getName());
		}

		ImmutableList<String> keys = ImmutableList.of(StringUtils.join(limitAnnotation.prefix(), key));
		try {
			String luaScript = buildLuaScript();
			RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);
			Number count = limitRedisTemplate.execute(redisScript, keys, limitCount, limitPeriod);
			logger.info("Access try count is {} for name={} and key = {}", count, name, key);
			if (count != null && count.intValue() <= limitCount) {
				return pjp.proceed();
			} else {
				throw new RuntimeException("You have been dragged into the blacklist");
			}
		} catch (Throwable e) {
			if (e instanceof RuntimeException) {
				throw new RuntimeException(e.getLocalizedMessage());
			}
			throw new RuntimeException("server exception");
		}
	}

	/**
	 * @author fu
	 * @description 编写Redis Lua限流脚本
	 * @date 2020/4/8 13:24
	 */
	public String buildLuaScript() {
		StringBuilder lua = new StringBuilder();
		lua.append("local c");
		lua.append("\nc = redis.call('get',KEYS[1])");
		// 调用不超过最大值，则直接返回
		lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");
		lua.append("\nreturn c;");
		lua.append("\nend");
		// 执行计算器自加
		lua.append("\nc = redis.call('incr',KEYS[1])");
		lua.append("\nif tonumber(c) == 1 then");
		// 从第一次调用开始限流，设置对应键值的过期
		lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");
		lua.append("\nend");
		lua.append("\nreturn c;");
		return lua.toString();
	}


	/**
	 * @author fu
	 * @description 获取id地址
	 * @date 2020/4/8 13:24
	 */
	public String getIpAddress() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}
} 
```

#### 2.3.6.控制层实现

我们将@Limit注解作用在需要进行限流的接口方法上，下边我们给方法设置@Limit注解，在10秒内只允许放行3个请求，这里为直观一点用AtomicInteger计数。

```java
/**
* @Author: fu
* @Description:
*/
@RestController
public class LimiterController {

	private static final AtomicInteger ATOMIC_INTEGER_1 = new AtomicInteger();
	private static final AtomicInteger ATOMIC_INTEGER_2 = new AtomicInteger();
	private static final AtomicInteger ATOMIC_INTEGER_3 = new AtomicInteger();

	/**
	 * @author fu
	 * @description
	 * @date 2020/4/8 13:42
	 */
	@Limit(key = "limitTest", period = 10, count = 3)
	@GetMapping("/limitTest1")
	public int testLimiter1() {

		return ATOMIC_INTEGER_1.incrementAndGet();
	}

	/**
	 * @author fu
	 * @description
	 * @date 2020/4/8 13:42
	 */
	@Limit(key = "customer_limit_test", period = 10, count = 3, limitType = LimitType.CUSTOMER)
	@GetMapping("/limitTest2")
	public int testLimiter2() {

		return ATOMIC_INTEGER_2.incrementAndGet();
	}

	/**
	 * @author fu
	 * @description 
	 * @date 2020/4/8 13:42
	 */
	@Limit(key = "ip_limit_test", period = 10, count = 3, limitType = LimitType.IP)
	@GetMapping("/limitTest3")
	public int testLimiter3() {

		return ATOMIC_INTEGER_3.incrementAndGet();
	}
} 
```

#### 2.3.7.测试

测试「预期」：连续请求3次均可以成功，第4次请求被拒绝。接下来看一下是不是我们预期的效果，请求地址：http://127.0.0.1:8080/limitTest1，用postman进行测试，有没有postman url直接贴浏览器也是一样。

![0c94a6dae1550544e8fbe43dbeca963b](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/0c94a6dae1550544e8fbe43dbeca963b.png)

可以看到第四次请求时，应用直接拒绝了请求，说明我们的Spring Boot + aop + Lua限流方案搭建成功。

![8bcc2d9ffd916630e6a560459f70e8fe](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/8bcc2d9ffd916630e6a560459f70e8fe.png)



## 3.计数器（string）
如知乎每个问题的被浏览器次数
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212201.png)

```
set key 0
incr key // incr readcount::{帖子id} 每阅读一次
get key // get readcount::{帖子id} 获取阅读量
```



## 4.分布式全局唯一id（string）

分布式全局唯一id的实现方式有很多，这里只介绍用redis实现

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212302.png)

每次获取userId的时候，对userId加1再获取，可以改进为如下形式

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212303.png)

直接获取一段userId的最大值，缓存到本地慢慢累加，快到了userId的最大值时，再去获取一段，一个用户服务宕机了，也顶多一小段userId没有用到

```
set userId 0
incr usrId //返回1
incrby userId 1000 //返回10001
```



## 5.消息队列（list）

在list里面一边进，一边出即可
```
# 实现方式一
# 一直往list左边放
lpush key value 
# key这个list有元素时，直接弹出，没有元素被阻塞，直到等待超时或发现可弹出元素为止，上面例子超时时间为10s
brpop key value 10 

# 实现方式二
rpush key value
blpop key value 10
```
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212401.png)



## 6.新浪/Twitter用户消息列表（list）

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212402.png)
假如说小编li关注了2个微博a和b，a发了一条微博（编号为100）就执行如下命令

```
lpush msg::li 100
```
b发了一条微博（编号为200）就执行如下命令：
```
lpush msg::li 200
```
假如想拿最近的10条消息就可以执行如下命令（最新的消息一定在list的最左边）：
```
# 下标从0开始，[start,stop]是闭区间，都包含
lrange msg::li 0 9 
```



## 7.抽奖活动（set）

```
# 参加抽奖活动
sadd key {userId} 

# 获取所有抽奖用户，大轮盘转起来
smembers key 

# 抽取count名中奖者，并从抽奖活动中移除
spop key count 

# 抽取count名中奖者，不从抽奖活动中移除
srandmember key count
```



## 8.实现点赞，签到，like等功能(set)

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212503.png)
```
# 1001用户给8001帖子点赞
sadd like::8001 1001

# 取消点赞
srem like::8001 1001

# 检查用户是否点过赞
sismember like::8001 1001 

# 获取点赞的用户列表
smembers like::8001 

# 获取点赞用户数
scard like::8001 
```



## 9.实现关注模型，可能认识的人（set）

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212506.png)
```
seven关注的人
sevenSub -> {qing, mic, james}
青山关注的人
qingSub->{seven,jack,mic,james}
Mic关注的人
MicSub->{seven,james,qing,jack,tom}
```
```
# 返回sevenSub和qingSub的交集，即seven和青山的共同关注
sinter sevenSub qingSub -> {mic,james}

# 我关注的人也关注他,下面例子中我是seven
# qing在micSub中返回1，否则返回0
sismember micSub qing
sismember jamesSub qing

# 我可能认识的人,下面例子中我是seven
# 求qingSub和sevenSub的差集，并存在sevenMayKnow集合中
sdiffstore sevenMayKnow qingSub sevenSub -> {seven,jack}
```



## 10.电商商品筛选（set）

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212507.png)
```
每个商品入库的时候即会建立他的静态标签列表如，品牌，尺寸，处理器，内存
# 将拯救者y700P-001和ThinkPad-T480这两个元素放到集合brand::lenovo
sadd brand::lenovo 拯救者y700P-001 ThinkPad-T480
sadd screenSize::15.6 拯救者y700P-001 机械革命Z2AIR
sadd processor::i7 拯救者y700P-001 机械革命X8TIPlus

# 获取品牌为联想，屏幕尺寸为15.6，并且处理器为i7的电脑品牌(sinter为获取集合的交集)
sinter brand::lenovo screenSize::15.6 processor::i7 -> 拯救者y700P-001
```



## 11.排行版（zset）

redis的zset天生是用来做排行榜的、好友列表, 去重, 历史记录等业务需求

![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212601.png)

```
# user1的用户分数为 10
zadd ranking 10 user1
zadd ranking 20 user2

# 取分数最高的3个用户
zrevrange ranking 0 2 withscores
```

## 12.基于Bitmap实现用户签到功能

很多应用上都有用户签到的功能，尤其是配合积分系统一起使用。现在有以下需求：

1. 签到1天得1积分，连续签到2天得2积分，3天得3积分，3天以上均得3积分等。
2. 如果连续签到中断，则重置计数，每月重置计数。
3. 显示用户某月的签到次数和首次签到时间。
4. 在日历控件上展示用户每月签到，可以切换年月显示。
5. ...

### 12.1.功能分析

对于用户签到数据，如果直接采用数据库存储，当出现高并发访问时，对数据库压力会很大，例如双十一签到活动。这时候应该采用缓存，以减轻数据库的压力，Redis是高性能的内存数据库，适用于这样的场景。

如果采用String类型保存，当用户数量大时，内存开销就非常大。

如果采用集合类型保存，例如Set、Hash，查询用户某个范围的数据时，查询效率又不高。

Redis提供的数据类型BitMap（位图），每个bit位对应0和1两个状态。虽然内部还是采用String类型存储，但Redis提供了一些指令用于直接操作BitMap，可以把它看作一个bit数组，数组的下标就是偏移量。

它的优点是**内存开销小，效率高且操作简单**，很适合用于签到这类场景。缺点在于位计算和位表示数值的局限。如果要用位来做业务数据记录，就不要在意value的值。

Redis提供了以下几个指令用于操作BitMap：

| 命令                                                 | 说明                                                         | 可用版本 | 时间复杂度 |
| ---------------------------------------------------- | ------------------------------------------------------------ | -------- | ---------- |
| [SETBIT](http://redisdoc.com/bitmap/setbit.html)     | 对 `key` 所储存的字符串值，设置或清除指定偏移量上的位(bit)。 | >= 2.2.0 | O(1)       |
| [GETBIT](http://redisdoc.com/bitmap/getbit.html)     | 对 `key` 所储存的字符串值，获取指定偏移量上的位(bit)。       | >= 2.2.0 | O(1)       |
| [BITCOUNT](http://redisdoc.com/bitmap/bitcount.html) | 计算给定字符串中，被设置为 1 的比特位的数量。                | >= 2.6.0 | O(N)       |
| [BITPOS](http://redisdoc.com/bitmap/bitpos.html)     | 返回位图中第一个值为 bit 的二进制位的位置。                  | >= 2.8.7 | O(N)       |
| [BITOP](http://redisdoc.com/bitmap/bitop.html)       | 对一个或多个保存二进制位的字符串 `key` 进行位元操作。        | >= 2.6.0 | O(N)       |
| [BITFIELD](http://redisdoc.com/bitmap/bitfield.html) | `BITFIELD` 命令可以在一次调用中同时对多个位范围进行操作。    | >= 3.2.0 | O(1)       |

考虑到每月要重置连续签到次数，最简单的方式是按用户每月存一条签到数据。Key的格式为 `u:sign:{uid}:{yyyMM}`，而Value则采用长度为4个字节的（32位）的BitMap（最大月份只有31天）。BitMap的每一位代表一天的签到，1表示已签，0表示未签。

例如 `u:sign:1225:202101` 表示ID=1225的用户在2021年1月的签到记录

```
# 用户1月6号签到
SETBIT u:sign:1225:202101 5 1 # 偏移量是从0开始，所以要把6减1

# 检查1月6号是否签到
GETBIT u:sign:1225:202101 5 # 偏移量是从0开始，所以要把6减1

# 统计1月份的签到次数
BITCOUNT u:sign:1225:202101

# 获取1月份前31天的签到数据
BITFIELD u:sign:1225:202101 get u31 0

# 获取1月份首次签到的日期
BITPOS u:sign:1225:202101 1 # 返回的首次签到的偏移量，加上1即为当月的某一天
```

### 12.2.示例代码

```csharp
using StackExchange.Redis;
using System;
using System.Collections.Generic;
using System.Linq;

/**
* 基于Redis Bitmap的用户签到功能实现类
* 
* 实现功能：
* 1. 用户签到
* 2. 检查用户是否签到
* 3. 获取当月签到次数
* 4. 获取当月连续签到次数
* 5. 获取当月首次签到日期
* 6. 获取当月签到情况
*/
public class UserSignDemo
{
    private IDatabase _db;

    public UserSignDemo(IDatabase db)
    {
        _db = db;
    }

    /**
     * 用户签到
     *
     * @param uid  用户ID
     * @param date 日期
     * @return 之前的签到状态
     */
    public bool DoSign(int uid, DateTime date)
    {
        int offset = date.Day - 1;
        return _db.StringSetBit(BuildSignKey(uid, date), offset, true);
    }

    /**
     * 检查用户是否签到
     *
     * @param uid  用户ID
     * @param date 日期
     * @return 当前的签到状态
     */
    public bool CheckSign(int uid, DateTime date)
    {
        int offset = date.Day - 1;
        return _db.StringGetBit(BuildSignKey(uid, date), offset);
    }

    /**
     * 获取用户签到次数
     *
     * @param uid  用户ID
     * @param date 日期
     * @return 当前的签到次数
     */
    public long GetSignCount(int uid, DateTime date)
    {
        return _db.StringBitCount(BuildSignKey(uid, date));
    }

    /**
     * 获取当月连续签到次数
     *
     * @param uid  用户ID
     * @param date 日期
     * @return 当月连续签到次数
     */
    public long GetContinuousSignCount(int uid, DateTime date)
    {
        int signCount = 0;
        string type = $"u{date.Day}";   // 取1号到当天的签到状态

        RedisResult result = _db.Execute("BITFIELD", (RedisKey)BuildSignKey(uid, date), "GET", type, 0);
        if (!result.IsNull)
        {
            var list = (long[])result;
            if (list.Length > 0)
            {
                // 取低位连续不为0的个数即为连续签到次数，需考虑当天尚未签到的情况
                long v = list[0];
                for (int i = 0; i < date.Day; i++)
                {
                    if (v >> 1 << 1 == v)
                    {
                        // 低位为0且非当天说明连续签到中断了
                        if (i > 0) break;
                    }
                    else
                    {
                        signCount += 1;
                    }
                    v >>= 1;
                }
            }
        }
        return signCount;
    }

    /**
     * 获取当月首次签到日期
     *
     * @param uid  用户ID
     * @param date 日期
     * @return 首次签到日期
     */
    public DateTime? GetFirstSignDate(int uid, DateTime date)
    {
        long pos = _db.StringBitPosition(BuildSignKey(uid, date), true);
        return pos < 0 ? null : date.AddDays(date.Day - (int)(pos + 1));
    }

    /**
     * 获取当月签到情况
     *
     * @param uid  用户ID
     * @param date 日期
     * @return Key为签到日期，Value为签到状态的Map
     */
    public Dictionary<string, bool> GetSignInfo(int uid, DateTime date)
    {
        Dictionary<string, bool> signMap = new Dictionary<string, bool>(date.Day);
        string type = $"u{GetDayOfMonth(date)}";
        RedisResult result = _db.Execute("BITFIELD", (RedisKey)BuildSignKey(uid, date), "GET", type, 0);
        if (!result.IsNull)
        {
            var list = (long[])result;
            if (list.Length > 0)
            {
                // 由低位到高位，为0表示未签，为1表示已签
                long v = list[0];
                for (int i = GetDayOfMonth(date); i > 0; i--)
                {
                    DateTime d = date.AddDays(i - date.Day);
                    signMap.Add(FormatDate(d, "yyyy-MM-dd"), v >> 1 << 1 != v);
                    v >>= 1;
                }
            }
        }
        return signMap;
    }

    private static string FormatDate(DateTime date)
    {
        return FormatDate(date, "yyyyMM");
    }

    private static string FormatDate(DateTime date, string pattern)
    {
        return date.ToString(pattern);
    }

    /**
     * 构建签到Key
     *
     * @param uid  用户ID
     * @param date 日期
     * @return 签到Key
     */
    private static string BuildSignKey(int uid, DateTime date)
    {
        return $"u:sign:{uid}:{FormatDate(date)}";
    }

    /**
     * 获取月份天数
     *
     * @param date 日期
     * @return 天数
     */
    private static int GetDayOfMonth(DateTime date)
    {
        if (date.Month == 2)
        {
            return 28;
        }
        if (new int[] { 1, 3, 5, 7, 8, 10, 12 }.Contains(date.Month))
        {
            return 31;
        }
        return 30;
    }

    static void Main(string[] args)
    {
        ConnectionMultiplexer connection = ConnectionMultiplexer.Connect("192.168.0.104:7001,password=123456");

        UserSignDemo demo = new UserSignDemo(connection.GetDatabase());
        DateTime today = DateTime.Now;
        int uid = 1225;

        { // doSign
            bool signed = demo.DoSign(uid, today);
            if (signed)
            {
                Console.WriteLine("您已签到：" + FormatDate(today, "yyyy-MM-dd"));
            }
            else
            {
                Console.WriteLine("签到完成：" + FormatDate(today, "yyyy-MM-dd"));
            }
        }

        { // checkSign
            bool signed = demo.CheckSign(uid, today);
            if (signed)
            {
                Console.WriteLine("您已签到：" + FormatDate(today, "yyyy-MM-dd"));
            }
            else
            {
                Console.WriteLine("尚未签到：" + FormatDate(today, "yyyy-MM-dd"));
            }
        }

        { // getSignCount
            long count = demo.GetSignCount(uid, today);
            Console.WriteLine("本月签到次数：" + count);
        }

        { // getContinuousSignCount
            long count = demo.GetContinuousSignCount(uid, today);
            Console.WriteLine("连续签到次数：" + count);
        }

        { // getFirstSignDate
            DateTime? date = demo.GetFirstSignDate(uid, today);
            if (date.HasValue)
            {
                Console.WriteLine("本月首次签到：" + FormatDate(date.Value, "yyyy-MM-dd"));
            }
            else
            {
                Console.WriteLine("本月首次签到：无");
            }
        }

        { // getSignInfo
            Console.WriteLine("当月签到情况：");
            Dictionary<string, bool> signInfo = new Dictionary<string, bool>(demo.GetSignInfo(uid, today));
            foreach (var entry in signInfo)
            {
                Console.WriteLine(entry.Key + ": " + (entry.Value ? "√" : "-"));
            }
        }
    }
}
```

运行结果

![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502193803.png)

### 12.3.更多应用场景

- 统计活跃用户：把日期作为Key，把用户ID作为offset，1表示当日活跃，0表示当日不活跃。还能使用位计算得到日活、月活、留存率等数据。
- 用户在线状态：跟统计活跃用户一样。

### 12.4.总结

- 位图优点是**内存开销小，效率高且操作简单**；缺点是**位计算和位表示数值的局限**。
- 位图适合二元状态的场景，例如用户签到、在线状态等场景。
- String类型最大长度为512M。 注意SETBIT时的偏移量，当偏移量很大时，可能会有较大耗时。 位图不是绝对的好，有时可能更浪费空间。
- 如果位图很大，建议分拆键。如果要使用BITOP，建议读取到客户端再进行位计算。

## 13.布隆过滤器 Bloom Filter

### 13.1.前言

假如有一个15亿用户的系统，每天有几亿用户访问系统，要如何快速判断是否为系统中的用户呢？

- 方法一，将15亿用户存储在数据库中，每次用户访问系统，都到数据库进行查询判断，准确性高，但是查询速度会比较慢。
- 方法二，将15亿用户缓存在Redis内存中，每次用户访问系统，都到Redis中进行查询判断，准确性高，查询速度也快，但是占用内存极大。即使只存储用户ID，一个用户ID一个字符，则15亿*8字节=12GB，对于一些内存空间有限的服务器来说相对浪费。

还有对于网站爬虫的项目，我们都知道世界上的网站数量及其之多，每当我们爬一个新的网站url时，如何快速判断是否爬虫过了呢？还有垃圾邮箱的过滤，广告电话的过滤等等。如果还是用上面2种方法，显然不是最好的解决方案。

再者，查询是一个系统最高频的操作，当查询一个数据，首先会先到缓存查询（例如Redis），如果缓存没命中，于是到持久层数据库（mongo，mysql等）查询，发现也没有此数据，于是本此查询失败。如果用户很多的时候，并且缓存都没命中，进而全部请求了持久层数据库，这就给数据库带来很大压力，严重可能拖垮数据库。俗称`缓存穿透`。

可能大家也听到另一个词叫`缓存击穿`，它是指一个热点key，不停着扛着高并发，突然这个key失效了，在失效的瞬间，大量的请求缓存就没命中，全部请求到数据库。

对于以上这些以及类似的场景，如何高效的解决呢？针对此，布隆过滤器应运而生了。

### 13.2.布隆过滤器

布隆过滤器（Bloom Filter）是1970年由布隆提出的。它实际上是一个很长的二进制向量和一系列随机映射函数。布隆过滤器可以用于检索一个元素是否在一个集合中。它的优点是空间效率和查询时间都比一般的算法要好的多，缺点是有一定的误识别率和删除困难。

二进制向量，简单理解就是一个二进制数组。这个数组里面存放的值要么是0，要么是1。

映射函数，它可以将一个元素映射成一个位阵列（Bit array）中的一个点。所以通过这个点，就能判断集合中是否有此元素。

**基本思想**

- 当一个元素被加入集合时，通过K个散列函数将这个元素映射到一个位数组中的K个点，把它们置为1。
- 检索某个元素时，再通过这K个散列函数将这个元素映射，看看这些位置是不是都是1就能知道集合中这个元素存不存在。如果这些位置有任何一个0，则该元素`一定`不存在；如果都是1，则被检元素`很可能`存在。

Bloom Filter跟单个哈希函数映射不同，Bloom Filter使用了k个哈希函数，每个元素跟k个bit对应。从而降低了冲突的概率。

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194146.png)

**优点**

1. 二进制组成的数组，内存占用空间少，并且插入和查询速度很快，常数级别。
2. Hash函数相互之间没有必然联系，方便由硬件并行实现。
3. 只存储0和1，不需要存储元素本身，在某些对保密要求非常严格的场合有优势。

**缺点**

1. 存在误差率。随着存入的元素数量增加，误算率随之增加。（比如现实中你是否遇到正常邮件也被放入垃圾邮件目录，正常短信被拦截）可以增加一个小的白名单，存储那些可能被误判的元素。
2. 删除困难。一个元素映射到bit数组的k个位置上是1，删除的时候不能简单的直接置为0，可能会影响其他元素的判断。因为其他元素的映射也有可能在相同的位置置为1。可以采用`Counting Bloom Filter`解决。

### 13.3.Redis实现

在Redis中，有一种数据结构叫位图，即`bitmap`。以下是一些常用的操作命令。

在Redis命令中，`SETBIT key offset value`，此命令表示将key对应的值的二进制数组，从左向右起，offset下标的二进制数字设置为value。

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194216.png)

键k1对应的值为keke，对应ASCII码为107 101 107 101，对应的二进制为 0110 1**0**11，0110 0101，0110 1011，0110 0101。将下标5的位置设置为1，所以变成 0110 1**1**11，0110 0101，0110 1011，0110 0101。即 oeke。

`GETBIT key offset`命令，它用来获取指定下标的值。

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194234.png)

还有一个比较常用的命令，`BITCOUNT key [start end]`，用来获取位图中指定范围值为1的个数。注意，start和end指定的是字节的个数，而不是位数组下标。

![å¨è¿éæå¥å¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194240.png)

`Redisson`是用于在Java程序中操作Redis的库，利用Redisson我们可以在程序中轻松地使用Redis。Redisson这个客户端工具实现了布隆过滤器，其底层就是通过bitmap这种数据结构来实现的。

Redis 4.0提供了插件功能之后，Redis就提供了布隆过滤器功能。布隆过滤器作为一个插件加载到了Redis Server之中，给Redis提供了强大的布隆去重功能。此文就不细讲了，大家感兴趣地可到官方查看详细文档介绍。它又如下常用命令：

1. bf.add：添加元素
2. bf.madd：批量添加元素
3. bf.exists：检索元素是否存在
4. bf.mexists：检索多个元素是否存在
5. bf.reserve：自定义布隆过滤器，设置key，error_rate和initial_size

下面演示是在本地单节点Redis实现的，如果数据量很大，并且误差率又很低的情况下，那单节点内存可能会不足。当然，在集群Redis中，也是可以通过Redisson实现分布式布隆过滤器的。

**引入依赖**

```xml
<!-- https://mvnrepository.com/artifact/org.redisson/redisson -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.13.6</version>
</dependency>
```

**代码测试**

```java
package com.nobody;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * @Description
 * @Author Mr.nobody
 * @Date 2021/3/6
 * @Version 1.0
 */
public class RedissonDemo {

    public static void main(String[] args) {

        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        // config.useSingleServer().setPassword("123456");

        RedissonClient redissonClient = Redisson.create(config);
        // 获取一个redis key为users的布隆过滤器
        RBloomFilter<Integer> bloomFilter = redissonClient.getBloomFilter("users");

        // 假设元素个数为10万
        int size = 100000;

        // 进行初始化，预计元素为10万，误差率为1%
        bloomFilter.tryInit(size, 0.01);

        // 将1至100000这十万个数映射到布隆过滤器中
        for (int i = 1; i <= size; i++) {
            bloomFilter.add(i);
        }

        // 检查已在过滤器中的值，是否有匹配不上的
        for (int i = 1; i <= size; i++) {
            if (!bloomFilter.contains(i)) {
                System.out.println("存在不匹配的值：" + i);
            }
        }

        // 检查不在过滤器中的1000个值，是否有匹配上的
        int matchCount = 0;
        for (int i = size + 1; i <= size + 1000; i++) {
            if (bloomFilter.contains(i)) {
                matchCount++;
            }
        }
        System.out.println("误判个数：" + matchCount);
    }
}
```

结果存在的10万个元素都匹配上了；不存在布隆过滤器中的1千个元素，有23个误判。

```bash
误判个数：23
```

### 13.4.Guava实现

布隆过滤器有许多实现与优化，Guava中就提供了一种实现。Google Guava提供的布隆过滤器的位数组是存储在JVM内存中，故是单机版的，并且最大位长为int类型的最大值。

- 使用布隆过滤器时，重要关注点是预估数据量n以及期望的误判率fpp。
- 实现布隆过滤器时，重要关注点是hash函数的选取以及bit数组的大小。

**Bit数组大小选择**

根据预估数据量n以及误判率fpp，bit数组大小的m的计算方式：

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194302.png)

Guava中源码实现如下：

```java
@VisibleForTesting
static long optimalNumOfBits(long n, double p) {
  if (p == 0) {
    p = Double.MIN_VALUE;
  }
  return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
}
```

**哈希函数选择**

哈希函数的个数的选择也是挺讲究的，哈希函数的选择影响着性能的好坏，而且一个好的哈希函数能近似等概率的将元素映射到各个Bit。如何选择构造k个函数呢，一种简单的方法是选择一个哈希函数，然后送入k个不同的参数。

哈希函数的个数k，可以根据预估数据量n和bit数组长度m计算而来：

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194320.png)

Guava中源码实现如下：

```java
	@VisibleForTesting
  	static int optimalNumOfHashFunctions(long n, long m) {
        // (m / n) * log(2), but avoid truncation due to division!
    	return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
 	}
```

**引入依赖**

```bash
<!-- https://mvnrepository.com/artifact/com.google.guava/guava -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>28.2-jre</version>
</dependency>
```

**代码测试**

```java
package com.nobody;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

/**
 * @Description
 * @Author Mr.nobody
 * @Date 2021/3/6
 * @Version 1.0
 */
public class GuavaDemo {

    public static void main(String[] args) {

        // 假设元素个数为10万
        int size = 100000;

        // 预计元素为10万，误差率为1%
        BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), size, 0.01);

        // 将1至100000这十万个数映射到布隆过滤器中
        for (int i = 1; i <= size; i++) {
            bloomFilter.put(i);
        }

        // 检查已在过滤器中的值，是否有匹配不上的
        for (int i = 1; i <= size; i++) {
            if (!bloomFilter.mightContain(i)) {
                System.out.println("存在不匹配的值：" + i);
            }
        }

        // 检查不在过滤器中的1000个值，是否有匹配上的
        int matchCount = 0;
        for (int i = size + 1; i <= size + 1000; i++) {
            if (bloomFilter.mightContain(i)) {
                matchCount++;
            }
        }
        System.out.println("误判个数：" + matchCount);

    }
}
```

结果存在的10万个元素都匹配上了；不存在布隆过滤器中的1千个元素，有10个误判。

```bash
误判个数：10
```

当fpp的值改为为0.001，即降低误差率时，误判个数为0个。

```bash
误判个数：0
```

分析结果可知，误判率确实跟我们传入的容错率差不多，而且在布隆过滤器中的元素都匹配到了。

**源码分析**

通过debug创建布隆过滤器的方法，当预计元素为10万个，fpp的值为0.01时，需要位数958505个，hash函数个数为7个。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194353.png)

当预计元素为10万个，fpp的值为0.001时，需要位数1437758个，hash函数个数为10个。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210502194357.png)

**得出结论**

- 容错率越大，所需空间和时间越小，容错率越小，所需空间和时间越大。
- 理论上存10万个数，一个int是4字节，即32位，需要320万位。如果使用HashMap存储，按HashMap50%的存储效率，需要640万位。而布隆过滤器即使容错率fpp为0.001，也才需要1437758位，可以看出BloomFilter的存储空间很小。

### 13.5.扩展知识点

假如有一台服务器，内存只有4GB，磁盘上有2个大文件，文件A存储100亿个URL，文件B存储100亿个URL。请问如何`模糊`找出两个文件的URL交集？如何`精致`找出两个文件的URL交集。

**模糊交集：**

借助布隆过滤器思想，先将一个文件的URL通过hash函数映射到bit数组中，这样大大减少了内存存储，再读取另一个文件URL，去bit数组中进行匹配。

**精致交集：**

对大文件进行hash拆分成小文件，例如拆分成1000个小文件（如果服务器内存更小，则可以拆分更多个更小的文件），比如文件A拆分为A1，A2，A3...An，文件B拆分为B1，B2，B3...Bn。而且通过相同的hash函数，相同的URL一定被映射到相同下标的小文件中，例如A文件的www.baidu.com被映射到A1中，那B文件的www.baidu.com也一定被映射到B1文件中。最后再通过求相同下标的小文件（例如A1和B1）（A2和B2）的交集即可。





























