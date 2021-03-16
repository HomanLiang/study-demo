[toc]



# Redis 应用场景





## 一、Redis分布式锁

### 相关文章

[Redis高级项目实战，都0202年了，还不会Redis？](https://www.cnblogs.com/chenyanbin/p/13506946.html)



### Redisson 是如何实现分布式锁的？

#### Maven配置

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



#### RedissonLock简单示例

redission支持4种连接redis方式，分别为单机、主从、Sentinel、Cluster 集群，项目中使用的连接方式是Sentinel。

redis服务器不在本地的同学请注意权限问题。

##### Sentinel配置

```
Config config = new Config();
config.useSentinelServers().addSentinelAddress("127.0.0.1:6479", "127.0.0.1:6489").setMasterName("master").setPassword("password").setDatabase(0);
RedissonClient redisson = Redisson.create(config);
```

##### 简单使用

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



#### 源码中使用到的Redis命令

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



#### 源码中使用到的lua脚本语义

Redisson源码中，执行redis命令的是lua脚本，其中主要用到如下几个概念。

- redis.call() 是执行redis命令.
- KEYS[1] 是指脚本中第1个参数
- ARGV[1] 是指脚本中第一个参数的值
- 返回值中nil与false同一个意思。

需要注意的是，在redis执行lua脚本时，相当于一个redis级别的锁，不能执行其他操作，类似于原子操作，也是redisson实现的一个关键点。

另外，如果lua脚本执行过程中出现了异常或者redis服务器直接宕掉了，执行redis的根据日志回复的命令，会将脚本中已经执行的命令在日志中删除。



#### 源码分析

##### RLOCK结构

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

##### RedissonLock获取锁 tryLock源码

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

##### RedissonLock解锁 unlock源码

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

##### RedissonLock强制解锁源码

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



#### 总结

这里只是简单的一个redisson分布式锁的测试用例，并分析了执行lua脚本这部分，如果要继续分析执行结束之后的操作，需要进行netty源码分析 ，redisson使用了netty完成异步和同步的处理。





## 二、Redis分布式限流器

### 什么是限流？为什么要限流？

不知道大家有没有坐过帝都的地铁，就是进地铁站都要排队的那种，为什么要这样摆长龙转圈圈？答案就是为了限流！因为一趟地铁的运力是有限的，一下挤进去太多人会造成站台的拥挤、列车的超载，存在一定的安全隐患。同理，我们的程序也是一样，它处理请求的能力也是有限的，一旦请求多到超出它的处理极限就会崩溃。为了不出现最坏的崩溃情况，只能耽误一下大家进站的时间。

限流是保证系统高可用的重要手段！

由于互联网公司的流量巨大，系统上线会做一个流量峰值的评估，尤其是像各种秒杀促销活动，为了保证系统不被巨大的流量压垮，会在系统流量到达一定阈值时，拒绝掉一部分流量。

限流会导致用户在短时间内（这个时间段是毫秒级的）系统不可用，一般我们衡量系统处理能力的指标是每秒的QPS或者TPS，假设系统每秒的流量阈值是1000，理论上一秒内有第1001个请求进来时，那么这个请求就会被限流。

### 限流方案

#### 计数器

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



#### 漏桶算法

漏桶算法思路很简单，我们把水比作是请求，漏桶比作是系统处理能力极限，水先进入到漏桶里，漏桶里的水按一定速率流出，当流出的速率小于流入的速率时，由于漏桶容量有限，后续进入的水直接溢出（拒绝请求），以此实现限流。

![image-20210304222955627](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304222955627.png)



#### 令牌桶算法

令牌桶算法的原理也比较简单，我们可以理解成医院的挂号看病，只有拿到号以后才可以进行诊病。

系统会维护一个令牌（token）桶，以一个恒定的速度往桶里放入令牌（token），这时如果有请求进来想要被处理，则需要先从桶里获取一个令牌（token），当桶里没有令牌（token）可取时，则该请求将被拒绝服务。令牌桶算法通过控制桶的容量、发放令牌的速率，来达到对请求的限制。

![image-20210304223047064](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304223047064.png)



#### Redis + Lua

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



#### 网关层限流

限流常在网关这一层做，比如Nginx、Openresty、Kong、Zuul、Spring Cloud Gateway等，而像spring cloud - gateway网关限流底层实现原理，就是基于Redis + Lua，通过内置Lua限流脚本的方式。

![image-20210304223228015](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210304223228015.png)



### Redis + Lua限流实现

#### 引入依赖包

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



#### 配置application.properties

在application.properties文件中配置提前搭建好的Redis服务地址和端口。

```
spring.redis.host=127.0.0.1
spring.redis.port=6379
```



#### 配置RedisTemplate实例

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



#### 自定义注解

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



#### 切面代码实现

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



#### 控制层实现

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



#### 测试

测试「预期」：连续请求3次均可以成功，第4次请求被拒绝。接下来看一下是不是我们预期的效果，请求地址：http://127.0.0.1:8080/limitTest1，用postman进行测试，有没有postman url直接贴浏览器也是一样。

![0c94a6dae1550544e8fbe43dbeca963b](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/0c94a6dae1550544e8fbe43dbeca963b.png)

可以看到第四次请求时，应用直接拒绝了请求，说明我们的Spring Boot + aop + Lua限流方案搭建成功。

![8bcc2d9ffd916630e6a560459f70e8fe](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/8bcc2d9ffd916630e6a560459f70e8fe.png)





## 三、计数器（string）
如知乎每个问题的被浏览器次数
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212201.png)

```
set key 0
incr key // incr readcount::{帖子id} 每阅读一次
get key // get readcount::{帖子id} 获取阅读量
```



## 四、分布式全局唯一id（string）

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



## 五、消息队列（list）

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



## 六、新浪/Twitter用户消息列表（list）

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



## 七、抽奖活动（set）

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



## 八、实现点赞，签到，like等功能(set)

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



## 九、实现关注模型，可能认识的人（set）

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



## 十、电商商品筛选（set）

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



## 十一、排行版（zset）

redis的zset天生是用来做排行榜的、好友列表, 去重, 历史记录等业务需求
![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/20210305212601.png)
```
# user1的用户分数为 10
zadd ranking 10 user1
zadd ranking 20 user2

# 取分数最高的3个用户
zrevrange ranking 0 2 withscores
```