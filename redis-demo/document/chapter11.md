[toc]



# Redis 监控

本文重点讲述Redis的哪些metrics需要重要监控（篇幅有限，不能涵盖所有），以及我们如何获取这些metrics数据。从而确保对我们应用至关重要的Redis是否健康运行，以及当出现问题时能及时通知我们。
## 吞吐量
吞吐量包括Redis实例历史总吞吐量，以及每秒钟的吞吐量。可以通过命令 `info stats` 得到我们要监控的吞吐量：
```
# 从Rdis上一次启动以来总计处理的命令数
total_commands_processed:2255
# 当前Redis实例的OPS
instantaneous_ops_per_sec:12
# 网络总入量
total_net_input_bytes:34312
# 网络总出量
total_net_output_bytes:78215
# 每秒输入量，单位是kb/s
instantaneous_input_kbps:1.20
# 每秒输出量，单位是kb/s
instantaneous_output_kbps:2.62
```


## 内存利用率

Redis高性能保障的一个重要资源就是足够的内存。`Used memory` 表示Redis已经分配的总内存大小。我们可以通过 `info memory` 命令获取所有内存利用了相关数据，其结果如下：
```
127.0.0.1:6379> info memory
# Memory
used_memory:1007888
used_memory_human:984.27K
used_memory_rss:581632
used_memory_rss_human:568.00K
used_memory_peak:1026064
used_memory_peak_human:1002.02K
total_system_memory:8589934592
total_system_memory_human:8.00G
used_memory_lua:37888
used_memory_lua_human:37.00K
maxmemory:0
maxmemory_human:0B
maxmemory_policy:noeviction
mem_fragmentation_ratio:0.58
mem_allocator:libc
```
需要注意的是，如果我们没有配置 `maxmemory`（可以通过 `config get/set maxmemory` 查询并在不重启Redis实例的前提下设置），那么Redis可能会耗尽服务器所有可用内存，从而可能导致swap甚至被系统kill掉。
所以建议方案是配置 `maxmemory`，并且配置 `maxmemory-policy`（不要是默认的 `noviction`）。即使这样还不够，因为如果并发比较大的话，缓存逐除策略可能会忙不过来，从而依然会有无法操作Redis的错误。所以强烈建议：在配置 `maxmemory-policy` 和    `maxmemory` 双策略的前提下，对 `used_memory` 进行监控，建议是 `maxmemory` 的90%。例如 `maxmemory` 为10G，那么当 `used_memory` 达到9G的时候，进行相关预警，从而准备扩容。



## 缓存命中率

缓存命中率表示缓存的使用效率，很明显，它通过公式：`HitRate = keyspace_hits / (keyspace_hits + keyspace_misses)` 计算得到。在`info stats`中恰好有这些数据：
```
keyspace_hits:17
keyspace_misses:1
```
缓存命中率建议不需要低于90%，越高越好。这个命中率越低，表示越多对缓存中没有的KEY进行了访问。可能是这些KEY已经过期、已经被删除、已经被evict、或者压根儿不存在的KEY非法访问等原因。
缓存命中率越低，或导致越多的请求穿透Redis从MySQL（或者其他速度远比Redis慢的存储服务）获取数据，从而导致越多的请求有更大的延迟，导致API耗时增加，影响用户体验。
如果是内存不足，那么需要扩容。例如`info stats`中的`evicted_keys`不为0，或者`used_memory`达到了内存上限。如果是用法问题，那么需要优化代码。客户端连接数



## 客户端连接数

这个值可以通过`info clients`中的字段`connected_clients`获取，它会受到操作系统`ulimit`和redis的`maxclients`配置的限制。如果Rdis客户端中报出获取不到连接数的错误（异常信息：`ERR max number of clients reached`），需要排查这两个地方是否限制了客户端连接数。当然，也可能还有其他其他原因，比如客户端BUG导致连接没有释放等。



## 慢日志

Redis和其他关系型数据库一样，也有命令执行的慢日志。慢日志收集的阈值可通过`config set slowlog-log-slower-than`配置，单位是微妙。默认是10000微秒，即10ms，笔者认为这个默认值设置的太大，建议将其调整到1ms。因为这个慢日志统计的时间只是命令执行的时间，不包括客户端到服务端的时间，以及命令在服务端队列中的等待时间。以Redis的性能来说，正常的执行时间一般在10微秒级别（单实例OPS可以达到10W）。所以，设置`slowlog-log-slower-than`为`1000`，即1毫秒已经绰绰有余：
```
redis> slowlog get 
1) 1) (integer) 21          # Unique ID
   2) (integer) 1439419285  # Unix timestamp
   3) (integer) 19125       # Execution time in microseconds
   4) 1) "keys"             # Command
... ...
```
另外，可以通过命令`slowlog reset`清理掉所有保存的慢日志。
说明：Redis4.0或者更高的版本多了两个额外的字段：客户端IP端口以及客户端名称。客户端名称可以通过命令：`client setname` 进行自定义设置。



## 延迟监控

任何环境都会存在延迟，关键是看延迟是否在我们能接受的范围内。一些影响会比较大的高延迟，可能会有很多的原因，例如：网络原因、计算密集型命令、时间复杂度为O(n)的命令、系统内存不够发生SWAP等。
Redis提供了非常多的工具来定位这些延迟问题。
- `slowlog`
即慢日志，前面已经有详细的说明，这是非常重要的监控项。Redis是单线程处理命令，所以如果有执行时间比较长的命令，就会导致其他命令阻塞。
- `latency monitor`
latency monitoring是从Redis2.8.13开始引入的新特性，用来帮组定位延迟问题，它能够记录Redis产生延迟问题的可能原因。需要通过如下命令来开启这个特性，当然，也可以在redis.conf中配置：
```
config set latency-monitor-threshold ms
```
接下来可以通过如下命令检查是否开启成功：
```
redis> latency latest
1) 1) "command"             # Event name
   2) (integer) 1539479413  # Unix timestamp
   3) (integer) 381         # Latency of latest event
   4) (integer) 6802        # All time maximum latency

# 还可以查看引起延迟的历史命令：
redis> latency history command

# 延迟诊断
redis> latency  doctor
```
- `intrinsic latency`
Redis服务内部延迟。通过执行命令：`src/redis-cli  --intrinsic-latency sec` 得到延迟统计数据，它的结果可以用来衡量Redis服务内部延迟时间。这个命令的总运行时间由最后一个参数`sec`决定。通过这个命令，我们能判定搭建的Redis服务性能是否正常。命令使用参考：
```
afeideMBP:redis-3.2.11 litian$ src/redis-cli  --intrinsic-latency 5
Max latency so far: 1 microseconds.
Max latency so far: 4 microseconds.
Max latency so far: 11 microseconds.
Max latency so far: 17 microseconds.
Max latency so far: 115 microseconds.
Max latency so far: 648 microseconds.

99087235 total runs (avg latency: 0.0505 microseconds / 50.46 nanoseconds per run).
Worst run took 12842x longer than the average latency.
```
- `network latency`
前面使用 `--intrinsic-latency` 可以检查Redis内部延迟情况，但是因为Redis是远程缓存服务，命令执行时从客户端到服务端的时间延迟并没有得到统计。而且相比起内部延迟，Redis客户端到服务端的网络延迟影响更大，不确定因素也更多，比如网络抖动等。Redis也提供了相关命令来统计网络延迟情况，这个命令的本质就是通过ping你的Redis服务端来衡量响应时间。使用方法如下：
```
afeideMBP:redis-3.2.11 litian$ src/redis-cli  --latency -h 127.0.1.168 -p 6379
min: 0, max: 1, avg: 0.18 (174 samples)
```
注意：这个命令会一直运行下去，除非你主动终止它。
- `cachecloud`
通过上文我们可知，大部分的metrics都可以通过info命令得到，毫不夸张的说，info命令是窥探Redis最好的方法。所以，要监控要Redis，我们一定要熟悉info结果中每个字段的含义，然后结合自己的业务有针对性的定制化最适合我们业务的监控方案。但是info命令只是一个单机版的命令，而一般我们的生产环境是redis集群。那么我们需要一个专业的监控服务来将整个redis集群的metric聚合起来方便我们查看，笔者在这里强烈推荐`cachecloud`。



## 命令统计

由于 Redis 没有非常详细的日志，要想知道在 Redis 实例内部都做了些什么是非常困难的。幸运的是 Redis 提供了一个下面这样的命令统计工具：
```
127.0.0.1:6379> INFO commandstats
# Commandstats
cmdstat_get:calls=78,usec=608,usec_per_call=7.79
cmdstat_setex:calls=5,usec=71,usec_per_call=14.20
cmdstat_keys:calls=2,usec=42,usec_per_call=21.00
cmdstat_info:calls=10,usec=1931,usec_per_call=193.10
```
通过这个工具可以查看所有命令统计的快照，比如命令执行了多少次，执行命令所耗费的毫秒数(每个命令的总时间和平均时间)
只需要简单地执行 `CONFIG RESETSTAT` 命令就可以重置，这样你就可以得到一个全新的统计结果。