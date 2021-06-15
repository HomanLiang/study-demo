[toc]



# Redis 基本命令

## 1.全局命令

### 1.1.查询键

`keys *` 查询所有的键，会遍历所有的键值，复杂度 `O(n)`

### 1.2.键总数

`dbsize` 查询键总数，直接获取redis内置的键总数变量，复杂度 `O(1)`

### 1.3.检查键是否存在

`exists key` 存在返回1，不存在返回0

### 1.4.删除键O(k)

`del key [key...]` 返回结果为成功删除键的个数

### 1.5.键过期

`expire key seconds` 当超过过期时间，会自动删除，key在seconds秒后过期

`expireat key timestamp` 键在秒级时间戳timestamp后过期

`pexpire key milliseconds` 当超过过期时间，会自动删除，key在milliseconds毫秒后过期

`pexpireat key milliseconds-timestamp key`在豪秒级时间戳timestamp后过期

`ttl` 命令可以查看键hello的剩余过期时间，单位：秒（>0剩余过期时间；-1没设置过期时间；-2键不存在）

`pttl` 是毫秒

```
192.168.225.129:6379> expire k2 100
(integer) 1
192.168.225.129:6379> ttl k2
(integer) 91
192.168.225.129:6379> ttl ma
(integer) -1
192.168.225.129:6379> 
```

### 1.6.键的数据结构类型

`type key` 如果键hello是字符串类型，则返回string；如果键不存在，则返回none

### 1.7.键重命名

`rename key newkey `

`renamenx key newkey` 只有newkey不存在时才会被覆盖

#### 1.7.1.随机返回一个键

`randomkey`

#### 1.7.2.迁移键

1. `move key db` （不建议再生产环境中使用）把指定的键从源数据库移动到目标数据库

2. `dump+restore`

   `dump key` 

   `Restore key ttl value`

   `Dump+restore` 可以实现在不同的redis实例之间进行数据迁移的功能，整个迁移的过程分为两步;

   - 在源redis上，dump命令会将键值序列化，格式采用的是RDB格式
   - 在目标redis上，restore命令将上面序列化的值进行复原，其中ttl参数代表过期时间，ttl=0代表没有过期时间
     例子:

    ```
    源redis
    192.168.225.129:6379> get redis
    "world"
    192.168.225.129:6379> dump redis
    "\x00\x05world\a\x00\xe6\xe3\xe9w\xd8c\xa7\xd8"
    目标redis
    192.168.225.128:6379> get redis
    (nil)
    192.168.225.128:6379> restore redis 0 "\x00\x05world\a\x00\xe6\xe3\xe9w\xd8c\xa7\xd8"
    OK
    192.168.225.128:6379> get redis
    "world"
    ```

3. `migrate`

   `migrate`实际上是把`dump`、`restore`、`del` 3个命令进行组合，从而简化了操作步骤。
   
   `Migrate host port key [ key ......] destination-db timeout [replace]`
   
   源redis中执行
   
   `192.168.225.129:6379> migrate 192.168.225.128 6379 flower 0 1000 replace`
   
   （将键flower迁移至目标192.168.225.128:6379的库0中，超时时间为1000毫秒，replace表示目标库如果存在键flower，则覆盖）

### 1.8.遍历键

1. **全量遍历键**

   keys pattern

   例如：keys h *, keys [r,l]edis ,keys* 等等

2. **渐进式遍历**

   scan 它可以有效的解决keys命令存在的阻塞问题，scan每次的额复杂度是O(1)

### 1.9.数据库管理

1. 切换数据库

   `select dbIndex`
   默认16个数据库：0-15，进入redis后默认是0库。不建议使用多个数据库

2. `flushdb` / `flushall`

   用于清除数据库，flushdb只清除当前数据库，flushall清除所有数据库。



## 2.针对key的操作

### 2.1.设置值 O(1)

```
set key value [ex]  [px]  [nx|xx]
ex为键值设置秒级过期时间
px为键值设置毫秒级过期时间
nx键必须不存在，才可以设置成功，用于添加
xx与nx相反，键必须存在，才可以设置成功，用于更新
setnx、setex 与上面的nx、ex作用相同
```

### 2.2.获取值O(1)

`get key` 不存在则返回nil

### 2.3.批量设置值O(k)

```
mset key value [key value ......]
mset a 1 b 2 c 3 d 4
```

### 2.4.批量获取值O(k)，k是键的个数

```
mget key [key ......]
```

### 2.5.计数O(1)

```
incr key
decr key /inceby key increment /decrby key increment
```


返回结果分为3中情况：

- 值不是整数，返回错误；
- 值是整数，返回自增后的结果；
- 键不存在，按照值为0自增，返回结果为1。

### 2.6.追加值O(1)

`append key value` 可以向字符串尾部追加值

### 2.7.字符串长度O(1)

`strlen key`

每个汉字占用3个字字节

### 2.8.设置并返回原值O(1)

`getset key value`

### 2.9.设置指定位置的字符O(n),n是字符串长度

```
setrange key offeset value
192.168.225.129:6379> get liming
"class4"
192.168.225.129:6379> setrange liming 0 m
(integer) 6
192.168.225.129:6379> get liming
"mlass4"
192.168.225.129:6379> 
```

### 2.10.获取部分字符串

`getrange key start end` start和end分别为开始和结束的偏移量，偏移量从0开始



## 3.Hash操作

### 3.1.设置值

`hset key field value`

还提供了`hsetnx`命令

Eg：`hset user:1 name tom`

### 3.2.获取值

`hget key field`

```
192.168.225.129:6379> hset user:1 name Tom
(integer) 1
192.168.225.129:6379> hget user:1 name
"Tom"
192.168.225.129:6379> hget user:1 age
(nil)
```

### 3.3.删除field

`hdel key field [field ......] `会删除一个或多个field，返回结果为成功删除fiel的个数

### 3.4.计算field的个数

`hlen key`

### 3.5.批量设置或获取field-value

```
Hmget key field [field ......]
Hmset key field value [field value]
```

### 3.6.判断field是否存在

`hexists key field`

### 3.7.获取所有field

`hkeys key`

```
192.168.225.129:6379> hkeys user:1
1) "name"
2) "age"
3) "grand"
4) "city"
```

### 3.8.获取所有value

`hvals key`

```
192.168.225.129:6379> hvals user:1
1) "Tom"
2) "20"
3) "3"
4) "beijing"
```

### 3.9.获取所有的field、value

`hgetall key`

### 3.10.hincrby hincrbyfloat 作用域是field

```
hincrby key field
hincrbyfloat key field
```

### 3.11.计算value字符串的长度

hstrlen key field



## 4.列表List操作

列表类型原来存储多个有序的字符串，可以重复

| 列表的4中操作类型 |                           |
| ----------------- | ------------------------- |
| 操作类型          | 操作                      |
| 添加              | rpush 、lpush、linsert    |
| 查                | lrange、lindex、llen      |
| 删除              | lpop 、rpop、 lrem、ltrim |
| 修改              | lset                      |
| 阻塞操作          | blpop、brpop              |

### 4.1.添加

1. 从右边插入元素

   `rpush key value [value......]`

2. 从左边插入元素

   `lpush key value [value......]`

3. 向某个元素前或者后插入元素

   `linsert key before|after pivot value`
   
   `linsert`命令会从列表中找到等于`pivot`的元素，在其前或者后插入一个新的元素value

```
192.168.225.129:6379> rpush mylist a b c d e f b a 
(integer) 8
192.168.225.129:6379> linsert mylist after f g
(integer) 9
192.168.225.129:6379> lrange mylist 0 -1
1) "a"
2) "b"
3) "c"
4) "d"
5) "e"
6) "f"
7) "g"
8) "b"
9) "a"
```

### 4.2.查找

1. 获取指定范围内的元素列表

   `lrange key start end` 索引下标从左到右分别是0到N-1，从右到左分别是-1到-N；end选项包含了自身

   `lrange key 0 -1` 可以从左到右获取列表的所有元素

   `lrange mylist 1 3` 获取列表中第2个到第4个元素

2. 获取列表指定下标的元素

   `lindex key index`

3. 获取列表长度

   `llen key`

### 4.3.删除

1. 从列表右侧弹出元素

   `rpop key`

2. 从列表左侧弹出元素

   `lpop key`

3. 删除指定元素

   `lrem key count value`
   
Lrem命令会从列表中找到=value的元素进行删除，根据count的不同分为3中情况
   
   ```
    Count>0,从左到有，删除最多count个元素
    Count<0,从右到左，删除最多count绝对值个元素
    Count=0,删除所有    
```
   
    ```
    列表listaaa为a a a a java php b a b
    192.168.225.129:6379> lrem listaaa 5 a
    (integer) 5
    192.168.225.129:6379> lrange listaaa 0 -1
    1) "java"
    2) "php"
    3) "b"
    4) "b"
    192.168.225.129:6379> lrem listaaa 3 php
    (integer) 1
    192.168.225.129:6379> lrange listaaa 0 -1
    1) "java"
    2) "b"
    3) "b"
    ```

### 4.4.修改

`lset key index newValue` 修改指定索引下标的元素

Eg：`lset listaaa 1 python` 输出为java python b

### 4.5.阻塞操作

`blpop key [key ...] timeout`

`brpop key [key ...] timeout`

blpop和brpop是lpop、rpop的阻塞版本，除了弹出方式不同，使用方法基本相同，timeout阻塞时间

1. 列表为空

   `brpop list:test 3` 3秒后返回

   `brpop list:test 0` 一直处于阻塞中

2. 列表不为空

   `brpop mylist 0` 立刻返回

    ```
    192.168.225.129:6379> brpop mylist 0
    1) "mylist"
    2) "a"
    ```



## 5.Set操作（不可重复）

### 5.1.集合内的操作

1. **添加元素**

   `sadd key element [element .....]` 返回结果为添加成功的元素个数

2. **删除元素**

   `srem key element [element .....]` 返回结果为删除成功的元素个数

3. **计算元素个数**

   `Scard key scard`的时间复杂度为O(1),直接用redis内部的变量

4. **判断元素是否在集合中**

   `Sismember key element` 在集合中则返回1，否则返回0

5. **随机从集合返回指定个数元素**

   `Srandmember key [count] count`可不写，默认为1

6. **从集合随机弹出元素**

   `Spop key [count]` 3.2版本开始支持[count]

7. **获取所有元素**

   `Smembers key `它和`lrange`、`hgetall`都属于比较重的命令，有时候可以使用sscan来完成



### 5.2.集合间的操作

1. 求多个集合的交集 `sinter key [ key ......]`

2. 求多个集合的并集 `sunion key [key ......]`

3. 求多个集合的差集 `sdiff key [key ......]` 第一个`key`里面有的，第二个`key`里面没有的

4. 将交集、并集、差集的结果保存

```
sinterstore destination key [ key ......]
sunionstore destination key [ key ......]
sdiffstore destination key [ key ......]
```

例如：sinterstore user:1_2:inter user:1 user:2 user:1_2:incr也是集合类型



## 6.ZADD操作（有序集合）

### 6.1.集合内

1. 添加成员 时间复杂度O(log(n)), sadd为O(1)

   `zadd key score member[score member .....]` 返回结果为添加成功的元素个数

2. 计算成员个数

   `zcard key scard`的时间复杂度为O(1),直接用redis内部的变量

3. 计算某个成员分数

   `zsore key member`

4. 计算成员的排名

   `zrank key member`

5. 删除成员

   `zrem key member [member .......]`

6. 增加成员的分数

   `zincrby key increment member`

7. 返回指定排名范围的成员

   `zrange key start end [withscores] `从低分到高分

   `zrevrange key start end [withscores]` 从高分到低分

8. 返回指定分数范围的成员

   `zrange key min max [withscores] [limit offset count ]` 按照分数从低分到高分

   `zrevrange key max min [withscores] [limit offset count ]` 按照分数从高分到低分

9. 返回指定分数范围的成员个数

   `zcount key min max`

10. 删除指定排名内的升序元素

    `zremrangebyrank key start end`

11. 删除指定分数范围的成员

    `zremrangebystore key min max`



### 6.2.集合间的操作

1. 交集

2. 并集

3. 差集

4. 将交集、并集、差集的结果保存



## 7.pub/sub(发布、订阅)

```
1、publish channel message           发布消息    eg：publish channel:sports 'I want to go eatting'
2、subscribe channel [channel .....]     订阅消息    eg: subscribe channel:sports
3、unsubscribe channel [channel .....]   取消订阅
4、psubscribe pattern [pattern ......]    按照模式订阅
5、unpsubscribe pattern [pattern ......]  按照模式取消订阅
6、查询订阅
 pubsub channels                    查看活跃的频道
   192.168.225.128:6379> pubsub channels
   1) "channel:sports"
   2) "__sentinel__:hello"
  pubsub numsub [channel ......]        查看频道订阅数  pubsub numsub channel:sports
  pubsub numpat                    查看模式订阅数
7、说明：
   客户端在执行订阅命令之后进入了订阅状态，只能接收四个命令：subscribe、psubscribe、unsubscribe、punsubscribe；
   新开启的订阅客户端，无法收到该频道之前的消息，因为redis不会对发布的消息进行持久化。
```



## 8.Transaction（事务）

```
discard                             , 取消执行事务块内的所有命令
exec                                , 执行事务块内的命令
multi                               , 标记一个事务块的开始
unwatch                             , 取消watch命令对所有key的监视
watch key [key ...]                 , 监视一个或者多个key，如果事务执行之前，这个kye被其它命令所动，则事务被打断
```



## 9.Connection（连接）

```
auth password                       , 登录redis时输入密码
echo message               , 打印一个特定的信息message，测试时使用
ping                                , 测试与服务器的连接，如果正常则返回pong
quit                                , 请求服务器关闭与当前客户端的连接
select index                        , 切换到指定的数据库
```



## 10.Server(服务器)

```
bgsave                             , 后台异步保存数据到硬盘
client setname/client getname      , 为连接设置、获取名字
client kill ip:port                , 关闭地址为 ip:port的客户端
client list                        , 以人类可读的方式，返回所有的连接客户端信息和统计数据
config get parameter               , 取得运行redis服务器的配置参数
config set parameter value         , 设置redis服务器的配置参数
config resetstat                   , 重置info命令的某些统计数据
dbsize                             , 返回当前数据库中key的数量
flushall                           , 清空整个redis服务器的数据（删除所有数据库的所有 key）
flushdb                           , 清空当前数据库中的所有key
info [section]                    , 返回redis服务器的各种信息和统计数据
lastsave                          , 返回最近一次redis成功将数据保存到磁盘时的时间
monitor                           , 实时打印出redis服务器接收到的指令
save                              , 将当前 Redis 实例的所有数据快照(snapshot)以 RDB 文件的形式保存到硬盘
slaveof host port                 , 将当前服务器转变为指定服务器的从属服务器
slowlog subcommand [argument]     , Redis 用来记录查询执行时间的日志系统
```





参考文章：

[【Redis】Redis常用命令](https://segmentfault.com/a/1190000010999677)