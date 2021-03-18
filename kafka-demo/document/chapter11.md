[toc]



# Kafka 性能调优

## 核心配置
### producer核心配置
1. acks ：发送应答（默认值：1）
生产者在考虑完成请求之前要求leader收到的确认的数量。这控制了发送的记录的持久性。允许以下设置:
    - acks=0：设置为0，则生产者将完全不等待来自服务器的任何确认。记录将立即添加到socket缓冲区，并被认为已发送。在这种情况下，不能保证服务器已经收到记录，重试配置将不会生效(因为客户机通常不会知道任何失败)。每个记录返回的偏移量总是-1。
    - acks=1:leader会将记录写到本地日志中，但不会等待所有follower的完全确认。在这种情况下，如果leader在记录失败后立即失败，但在追随者复制记录之前失败，那么记录就会丢失。
    - acks=all / -1:leader将等待完整的同步副本来确认记录。这保证了只要至少有一个同步副本仍然存在，记录就不会丢失。这是最有力的保证。这相当于acks=-1设置。

2. batch.size：批量发送大小（默认：16384，16K）
缓存到本地内存，批量发送大小，意思每次发送16K到broke。当多个记录被发送到同一个分区时，生产者将尝试将记录批处理成更少的请求。这有助于客户机和服务器上的性能。此配置以字节为单位控制默认批处理大小。

3. bootstrap.servers：服务器地址
broke服务器地址，多个用逗号割开。

4. buffer.memory：生产者最大可用缓存 (默认：33554432，32M)
生产者可以用来缓冲等待发送到服务器的记录的总内存字节。如果记录被发送的速度超过了它们可以被发送到服务器的速度，那么生产者将阻塞max.block。然后它会抛出一个异常。
该设置应该大致与生成器将使用的总内存相对应，但不是硬绑定，因为生成器使用的并非所有内存都用于缓冲。一些额外的内存将用于压缩(如果启用了压缩)以及维护飞行中的请求。
生产者产生的消息缓存到本地，每次批量发送batch.size大小到服务器。

5. client.id：生产者ID(默认“”)
请求时传递给服务器的id字符串。这样做的目的是通过允许在服务器端请求日志中包含逻辑应用程序名称，从而能够跟踪ip/端口之外的请求源。

6. compression.type：压缩类型（默认值：producer）
指定给定主题的最终压缩类型。此配置接受标准压缩编解码器(“gzip”、“snappy”、“lz4”、“zstd”)。它还接受“未压缩”，相当于没有压缩;以及“生产者”，即保留生产者设置的原始压缩编解码器。
“gzip”：压缩效率高，适合高内存、CPU
“snappy”：适合带宽敏感性，压缩力度大

7. retries:失败重试次数（默认：2147483647）
异常是RetriableException类型或者TransactionManager允许重试；
transactionManager.canRetry()后面会分析；先看看哪些异常是RetriableException类型异常。
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317225133.png)
允许重试，但不需要设置max.in.flight.requests.per.connection（单个连接上发送的未确认请求的最大数量）。连接到1可能会改变记录的顺序，因为如果将两个批发送到单个分区，第一个批处理失败并重试，但是第二个批处理成功，那么第二个批处理中的记录可能先出现。
通过delivery.timeout.ms也可以控制重试次数，如果重试次数没有用尽，传输超时也会停止。
retry.backoff.ms：重试阻塞时间（默认：100）
这避免了在某些失败场景下以紧密循环的方式重复发送请求。

8. delivery.timeout.ms：传输时间（默认：120000，2分钟）
生产者发送完请求接受服务器ACk的时间，该时间允许重试 ，该配置应该大于request.timeout.ms + linger.ms。

9. connections.max.idle.ms：关闭空闲连接时间（默认：540000）
 在此配置指定的毫秒数之后关闭空闲连接。

10. enable.idempotence：开启幂等（默认:false）
当设置为“true”时，生产者将确保在流中准确地写入每个消息的副本。如果“false”，则由于代理失败而导致生产者重试，等等，可能会在流中写入重试消息的副本。请注意，启用幂等需要使用max.in.flight.requests.per.connection,连接小于或等于5，重试大于0且ack必须为“all”。如果用户没有显式地设置这些值，将选择合适的值。如果设置了不兼容的值，就会抛出ConfigException。

11. max.in.flight.requests.per.connection：单个连接上发送的未确认请求的最大数量（默认：5）
阻塞前客户端在单个连接上发送的未确认请求的最大数量。请注意，如果该设置设置为大于1，并且发送失败，则有由于重试(即，如果启用重试)。

12. interceptor.classes：拦截器（默认：无）
用作拦截器的类的列表。实现接口：org.apache.kafka.clients.producer。ProducerInterceptor接口允许将生产者接收到的记录发布到Kafka集群之前拦截它们(可能还会发生突变)。默认情况下，没有拦截器。

13. key.serializer:key序列化器（默认无）
实现org.apache.kafka.common. serialize .Serializer接口的key的序列化器类。String可配置：class org.apache.kafka.common.serialization.StringSerializer。

14. value.serializer:value序列化器（默认无）
序列化器类的值，该值实现org.apache.kafka.common. serialize .Serializer接口。String可配置：class org.apache.kafka.common.serialization.StringSerializer

15. linger.ms：发送延迟时间（默认：0）
为减少负载和客户端的请求数量，生产者不会一条一条发送，而是会逗留一段时间批量发送。batch.size和linger.ms满足任何一个条件都会发送。

16. max.block.ms：阻塞时间（默认：60000，一分钟）
配置控制KafkaProducer.send()和KafkaProducer.partitionsFor()阻塞的时间。由于缓冲区已满或元数据不可用，也会阻塞。用户提供的序列化器或分区程序中的阻塞将不计入此超时。

17. max.request.size：最大请求字节大小（默认：1048576，1M）
请求的最大字节大小。此设置将限制生产者在单个请求中发送记录批的数量，以避免发送大量请求。这也有效地限制了最大记录批大小。注意，服务器对记录批处理大小有自己的上限，这可能与此不同。

18. metric.reporters：自定义指标报告器
用作指标报告器的类的列表。metricsreporter接口实现了org.apache.kafka.common.metrics.MetricsReporter接口，该接口允许插入将在创建新度量时得到通知的类。JmxReporter始终包含在注册JMX统计信息中。

19. partitioner.class：自定义分区策略
实现接口 org.apache.kafka.clients.producer.Partitioner，默认值：org.apache.kafka.clients.producer.internals.DefaultPartitioner

20. request.timeout.ms：请求超时时间（默认：30000）
配置控制客户机等待请求响应的最长时间。如果在超时超时之前没有收到响应，客户端将在需要时重新发送请求，或者在重试耗尽时失败请求。这个应该大于replica.lag.time.max。ms(代理配置)，以减少由于不必要的生产者重试而导致消息重复的可能性。

21. receive.buffer.bytes（默认：32768，32K）
读取数据时使用的TCP接收缓冲区(SO_RCVBUF)的大小。如果值是-1，将使用OS默认值。

22. send.buffer.bytes（默认：131072,128K）
发送数据时使用的TCP发送缓冲区(SO_SNDBUF)的大小。如果值是-1，将使用OS默认值。

23. retry.backoff.ms：重试阻塞时间（默认：100）
这避免了在某些失败场景下以紧密循环的方式重复发送请求。

### consumer核心配置
1. enable.auto.commit：开启自动提交（默认:true）
如果为true，consumer的偏移量将在后台定期提交。

2. auto.commit.interval.ms：自动提交频率（默认：5000）
如果enable.auto.commit设置为true，则使用者偏移量自动提交到Kafka的频率(毫秒)。

3. client.id：客户ID
便于跟踪日志。

4. check.crcs：是否开启数据校验（默认：true）
自动检查消耗的记录的CRC32。这确保不会发生对消息的在线或磁盘损坏。此检查增加了一些开销，因此在寻求极端性能的情况下可能禁用此检查。

5. bootstrap.servers：服务器配置
多个用都好隔开。

6. connections.max.idle.ms：关闭空间连接时间（默认：540000）
在此配置指定的毫秒数之后关闭空闲连接。

7. group.id：群组（默认：“”）
唯一标识用户群组，同一个group每个partition只会分配到一个consumer。

8. max.poll.records：拉起最大记录（默认：500）
单次轮询()调用中返回的记录的最大数量。

9. max.poll.interval.ms：拉取记录间隔（默认：300000，5分钟）
使用消费者组管理时轮询()调用之间的最大延迟。这为使用者在获取更多记录之前空闲的时间设置了上限。如果在此超时过期之前没有调用poll()，则认为使用者失败，组将重新平衡，以便将分区重新分配给另一个成员。

10. request.timeout.ms：请求超时时间（默认：30000 ，30S）
配置控制客户机等待请求响应的最长时间。如果在超时超时之前没有收到响应，客户端将在需要时重新发送请求，或者在重试耗尽时失败请求。

11. session.timeout.ms：consumer session超时
用于检测worker程序失败的超时。worker定期发送心跳，以向代理表明其活性。如果在此会话超时过期之前代理没有接收到心跳，则代理将从组中删除。请注意，该值必须位于group.min.session.timeout在broker配置中配置的允许范围内group.max.session.timeout.ms。

12. auto.offset.reset:初始偏移量 (默认：latest)
如果Kafka中没有初始偏移量，或者服务器上不再存在当前偏移量(例如，因为该数据已被删除)，该怎么办:
earliest:自动重置偏移到最早的偏移
latest:自动将偏移量重置为最新偏移量
none:如果没有为使用者的组找到以前的偏移量，则向使用者抛出exception
anything else:向使用者抛出异常

13. key.deserializer
用于实现org.apache.kafka.common. serialize .Deserializer接口的key的反序列化类，class org.apache.kafka.common.serialization.StringDeserializer

14. value.deserializer
用于实现org.apache.kafka.common. serialize .Deserializer接口的value的反序列化类，class org.apache.kafka.common.serialization.StringDeserializer

15. max.partition.fetch.bytes
每个分区服务器将返回的最大数据量。记录由consumer成批提取。如果fetch的第一个非空分区中的第一个记录批处理大于这个限制，那么仍然会返回批处理，以确保使用者能够取得进展。broker接受的最大记录批处理大小是通过message.max定义的。字节(broker配置)或max.message。字节(topic配置)。看fetch.max.bytes用于限制consumer请求大小的字节。

16. partition.assignment.strategy：consumer订阅分区策略
（默认：class org.apache.kafka.clients.consumer.RangeAssignor）

当使用组管理时，客户端将使用分区分配策略的类名在使用者实例之间分配分区所有权。

17. fetch.max.bytes：拉取最大字节（默认：52428800，50M）
服务器应该为获取请求返回的最大数据量。记录由使用者成批地获取，并且如果获取的第一个非空分区中的第一个记录批处理大于这个值，仍然会返回记录批处理，以确保使用者能够取得进展。因此，这不是一个绝对最大值。代理接受的最大记录批处理大小是通过message.max定义的。字节(代理配置)或max.message。字节(主题配置)。请注意，使用者并行执行多个获取。

18. heartbeat.interval.ms：心跳时间（默认：3000, 3S）
使用Kafka的组管理工具时，从心跳到消费者协调器的预期时间。心跳被用来确保消费者的会话保持活跃，并在新消费者加入或离开组时促进再平衡。该值必须设置为小于session.timeout.ms的1/3。它可以调整甚至更低，以控制正常再平衡的预期时间。

19. fetch.max.wait.ms：拉取阻塞时间（默认：500）
如果没有足够的数据立即满足fetch.min.bytes提供的要求，服务器在响应fetch请求之前将阻塞的最长时间。

20. fetch.min.bytes：拉取最小字节数（默认：1）
服务器应该为获取请求返回的最小数据量。如果没有足够的数据可用，请求将等待那么多数据累积后再响应请求。默认的1字节设置意味着，只要数据的一个字节可用，或者获取请求超时等待数据到达，就会响应获取请求。将此设置为大于1的值将导致服务器等待更大数量的数据累积，这可以稍微提高服务器吞吐量，但代价是增加一些延迟。

21. exclude.internal.topics:公开内部topic(默认：true)
是否应该将来自内部主题(如偏移量)的记录公开给使用者，consumer共享offset。如果设置为true，从内部主题接收记录的唯一方法是订阅它。

22. isolation.level(隔离级别：默认：read_uncommitted）
控制如何以事务方式读取写入的消息。如果设置为read_committed, consumer.poll()将只返回已提交的事务消息。如果设置为read_uncommitted'(默认)，consumer.poll()将返回所有消息，甚至是已经中止的事务消息。在任何一种模式下，非事务性消息都将无条件返回。

### broker配置--server.properties配置文件
1. zookeeper.connect：zk地址
多个用逗号隔开。

2. advertised.host.name（默认：null）
不赞成使用:
在server.properties 里还有另一个参数是解决这个问题的， advertised.host.name参数用来配置返回的host.name值，把这个参数配置为外网IP地址即可。
这个参数默认没有启用，默认是返回的java.net.InetAddress.getCanonicalHostName的值，在我的mac上这个值并不等于hostname的值而是返回IP，但在linux上这个值就是hostname的值。

3. advertised.listeners
hostname和端口注册到zk给生产者和消费者使用的，如果没有设置，将会使用listeners的配置，如果listeners也没有配置，将使用java.net.InetAddress.getCanonicalHostName()来获取这个hostname和port，对于ipv4，基本就是localhost了。

4. auto.create.topics.enable（自动创建topic，默认：true）
第一次发动消息时，自动创建topic。

5. auto.leader.rebalance.enable：自动rebalance(默认：true)
支持自动领导平衡。如果需要，后台线程定期检查并触发leader balance。

6. background.threads：处理线程（默认：10）
用于各种后台处理任务的线程数。

7. broker.id 默认：-1
此服务器的broke id。如果未设置，将生成唯一的代理id。为了避免zookeeper生成的broke id和用户配置的broke id之间的冲突，生成的代理id从reserve .broker.max开始id + 1。

8. compression.type：压缩类型，默认：producer
指定给定主题的最终压缩类型。此配置接受标准压缩编解码器(“gzip”、“snappy”、“lz4”、“zstd”)。它还接受“未压缩”，相当于没有压缩;以及“producer”，即保留producer设置的原始压缩编解码器。

9. delete.topic.enable 删除topic(默认：true)
允许删除主题。如果关闭此配置，则通过管理工具删除主题将无效。

10. leader.imbalance.check.interval.seconds（rebalance检测频率，默认：300）
控制器触发分区rebalance检查的频率。

11. leader.imbalance.per.broker.percentage（触发rebalance比率，默认：10，10%）
每个broke允许的lead不平衡比率。如果控制器超过每个broke的这个值，控制器将触发一个leader balance。该值以百分比指定。

12. log.dir（日志目录，默认：/tmp/kafka-logs）
保存日志数据的目录。

13. log.dirs
保存日志数据的目录。如果未设置，则为日志中的值。使用dir。

14. log.flush.interval.messages（默认：9223372036854775807）
在将消息刷新到磁盘之前，日志分区上累积的消息数量

15. log.flush.interval.ms（默认：null）
任何主题中的消息在刷新到磁盘之前保存在内存中的最长时间。如果没有设置，则使用log.flush.scheduler.interval.ms中的值。

16. log.flush.offset.checkpoint.interval.ms（默认：60000）
作为日志恢复点的上次刷新的持久记录的更新频率。

17. log.retention.bytes 保存日志文件的最大值（默认：-1）
删除前日志的最大大小。

18. log.retention.hours日志文件最大保存时间（小时）默认：168，一周
日志文件最大保存时间。

19. log.retention.minutes日志文件最大保存时间（分钟）默认：null
20. log.retention.ms日志文件最大保存时间（毫秒）默认：null
21. log.roll.hours:新segment产生时间，默认：168，一周
即使文件没有到达log.segment.bytes，只要文件创建时间到达此属性，就会创建新文件。

22. log.roll.ms :新segment产生时间
滚出新日志段之前的最大时间(以毫秒为单位)。如果未设置，则为log.roll中的值使用。

23. log.segment.bytes：segment文件最大值，默认：1073741824（1G）
24. log.segment.delete.delay.ms：segment删除等待时间， 默认：60000
从文件系统中删除文件之前等待的时间量。

25. message.max.bytes 最大batch size 默认：1000012，0.9M
Kafka允许的最大记录batch size。如果增加了这个值，并且存在大于0.10.2的使用者，那么还必须增加consumer的fetch大小，以便他们能够获取这么大的记录批。在最新的消息格式版本中，记录总是按批进行分组，以提高效率。在以前的消息格式版本中，未压缩记录没有分组成批，这种限制只适用于单个记录。可以使用主题级别max.message设置每个主题。字节的配置。

26. min.insync.replicas（insync中最小副本值）
当producer将acks设置为“all”(或“-1”)时，min.insync。副本指定必须确认写操作成功的最小副本数量。如果不能满足这个最小值，则生产者将引发一个异常(要么是NotEnoughReplicas，要么是NotEnoughReplicasAfterAppend)。
当一起使用时，min.insync.replicas和ack允许您执行更大的持久性保证。一个典型的场景是创建一个复制因子为3的主题，设置min.insync复制到2个，用“all”配置发送。将确保如果大多数副本没有收到写操作，则生产者将引发异常。

27. num.io.threads，默认：8
服务器用于处理请求的线程数，其中可能包括磁盘I/O。

28. num.network.threads，默认：3
服务器用于接收来自网络的请求和向网络发送响应的线程数。

29. num.recovery.threads.per.data.dir 默认：1
每个数据目录在启动时用于日志恢复和在关闭时用于刷新的线程数。

30. num.replica.alter.log.dirs.threads 默认：null
可以在日志目录(可能包括磁盘I/O)之间移动副本的线程数。

31. num.replica.fetchers
从leader复制数据到follower的线程数。

32. offset.metadata.max.bytes 默认：4096
与offset提交关联的metadata的最大大小。

33. offsets.commit.timeout.ms 默认：5000
偏移量提交将被延迟，直到偏移量主题的所有副本收到提交或达到此超时。这类似于生产者请求超时。

34. offsets.topic.num.partitions 默认：50
偏移量提交主题的分区数量(部署后不应更改)。

35. offsets.topic.replication.factor 副本大小,默认：3
低于上述值，主题将创建失败。

36. offsets.topic.segment.bytes 默认104857600 ,100M
segment映射文件（index）文件大小，以便加快日志压缩和缓存负载。

37. queued.max.requests 默认：500
阻塞网络线程之前允许排队的请求数。

38. replica.fetch.min.bytes默认：1
每个fetch响应所需的最小字节。如果字节不够，则等待replicaMaxWaitTimeMs。

39. replica.lag.time.max.ms 默认：10000
如果follower 没有发送任何获取请求，或者至少在这段时间没有消耗到leader日志的结束偏移量，那么leader将从isr中删除follower 。

40. transaction.max.timeout.ms 默认：900000
事务执行最长时间，超时则抛出异常。

41. unclean.leader.election.enable 默认：false
是否选举ISR以外的副本作为leader,会导致数据丢失。

42. zookeeper.connection.timeout.ms
客户端等待与zookeeper建立连接的最长时间。如果未设置，则用zookeeper.session.timeout中的值。

43. zookeeper.max.in.flight.requests
阻塞之前consumer将发送给Zookeeper的未确认请求的最大数量。

44. group.max.session.timeout.ms
注册使用者允许的最大会话超时。超时时间越长，消费者在心跳之间处理消息的时间就越多，而检测故障的时间就越长。

45. group.min.session.timeout.ms
注册使用者允许的最小会话超时。更短的超时导致更快的故障检测，但代价是更频繁的用户心跳，这可能会压倒broke资源。

46. num.partitions 默认：1
每个主题的默认日志分区数量。









## 示例

```
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092"); 
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("buffer.memory", 67108864); 
props.put("batch.size", 131072); 
props.put("linger.ms", 100); 
props.put("max.request.size", 10485760); 
props.put("acks", "1"); 
props.put("retries", 10); 
props.put("retry.backoff.ms", 500);

KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);
```
## 内存缓冲的大小
Kafka的客户端发送数据到服务器，一般都是要经过缓冲的，也就是说，你通过KafkaProducer发送出去的消息都是先进入到客户端本地的内存缓冲里，然后把很多消息收集成一个一个的Batch，再发送到Broker上去的。
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224603.png)
所以这个“buffer.memory”的本质就是用来约束KafkaProducer能够使用的内存缓冲的大小的，他的默认值是32MB。

首先要明确一点，那就是在内存缓冲里大量的消息会缓冲在里面，形成一个一个的Batch，每个Batch里包含多条消息。
然后KafkaProducer有一个Sender线程会把多个Batch打包成一个Request发送到Kafka服务器上去。
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224625.png)
那么如果要是内存设置的太小，可能导致一个问题：消息快速的写入内存缓冲里面，但是Sender线程来不及把Request发送到Kafka服务器。
这样是不是会造成内存缓冲很快就被写满？一旦被写满，就会阻塞用户线程，不让继续往Kafka写消息了。
所以对于“buffer.memory”这个参数应该结合自己的实际情况来进行压测，你需要测算一下在生产环境，你的用户线程会以每秒多少消息的频率来写入内存缓冲。
比如说每秒300条消息，那么你就需要压测一下，假设内存缓冲就32MB，每秒写300条消息到内存缓冲，是否会经常把内存缓冲写满？经过这样的压测，你可以调试出来一个合理的内存大小。

## 多少数据打包为一个Batch合适？
这个东西是决定了你的每个Batch要存放多少数据就可以发送出去了。

比如说你要是给一个Batch设置成是16KB的大小，那么里面凑够16KB的数据就可以发送了。

这个参数的默认值是16KB，一般可以尝试把这个参数调节大一些，然后利用自己的生产环境发消息的负载来测试一下。

比如说发送消息的频率就是每秒300条，那么如果比如“batch.size”调节到了32KB，或者64KB，是否可以提升发送消息的整体吞吐量。

因为理论上来说，提升batch的大小，可以允许更多的数据缓冲在里面，那么一次Request发送出去的数据量就更多了，这样吞吐量可能会有所提升。

但是这个东西也不能无限的大，过于大了之后，要是数据老是缓冲在Batch里迟迟不发送出去，那么岂不是你发送消息的延迟就会很高。

比如说，一条消息进入了Batch，但是要等待5秒钟Batch才凑满了64KB，才能发送出去。那这条消息的延迟就是5秒钟。

所以需要在这里按照生产环境的发消息的速率，调节不同的Batch大小自己测试一下最终出去的吞吐量以及消息的 延迟，设置一个最合理的参数。

## 要是一个Batch迟迟无法凑满怎么办？
要是一个Batch迟迟无法凑满，此时就需要引入另外一个参数了，“linger.ms”

他的含义就是说一个Batch被创建之后，最多过多久，不管这个Batch有没有写满，都必须发送出去了。

给大家举个例子，比如说batch.size是16kb，但是现在某个低峰时间段，发送消息很慢。

这就导致可能Batch被创建之后，陆陆续续有消息进来，但是迟迟无法凑够16KB，难道此时就一直等着吗？

当然不是，假设你现在设置“linger.ms”是50ms，那么只要这个Batch从创建开始到现在已经过了50ms了，哪怕他还没满16KB，也要发送他出去了。

所以“linger.ms”决定了你的消息一旦写入一个Batch，最多等待这么多时间，他一定会跟着Batch一起发送出去。

避免一个Batch迟迟凑不满，导致消息一直积压在内存里发送不出去的情况。这是一个很关键的参数。

这个参数一般要非常慎重的来设置，要配合batch.size一起来设置。

举个例子，首先假设你的Batch是32KB，那么你得估算一下，正常情况下，一般多久会凑够一个Batch，比如正常来说可能20ms就会凑够一个Batch。

那么你的linger.ms就可以设置为25ms，也就是说，正常来说，大部分的Batch在20ms内都会凑满，但是你的linger.ms可以保证，哪怕遇到低峰时期，20ms凑不满一个Batch，还是会在25ms之后强制Batch发送出去。

如果要是你把linger.ms设置的太小了，比如说默认就是0ms，或者你设置个5ms，那可能导致你的Batch虽然设置了32KB，但是经常是还没凑够32KB的数据，5ms之后就直接强制Batch发送出去，这样也不太好其实，会导致你的Batch形同虚设，一直凑不满数据。

## 最大请求大小
“max.request.size”这个参数决定了每次发送给Kafka服务器请求的最大大小，同时也会限制你一条消息的最大大小也不能超过这个参数设置的值，这个其实可以根据你自己的消息的大小来灵活的调整。

给大家举个例子，你们公司发送的消息都是那种大的报文消息，每条消息都是很多的数据，一条消息可能都要20KB。

此时你的batch.size是不是就需要调节大一些？比如设置个512KB？然后你的buffer.memory是不是要给的大一些？比如设置个128MB？

只有这样，才能让你在大消息的场景下，还能使用Batch打包多条消息的机制。但是此时“max.request.size”是不是也得同步增加？

因为可能你的一个请求是很大的，默认他是1MB，你是不是可以适当调大一些，比如调节到5MB？

## 重试机制
“retries”和“retries.backoff.ms”决定了重试机制，也就是如果一个请求失败了可以重试几次，每次重试的间隔是多少毫秒。

这个大家适当设置几次重试的机会，给一定的重试间隔即可，比如给100ms的重试间隔。