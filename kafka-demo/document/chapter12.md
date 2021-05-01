[toc]



# Kafka 实际问题解决

## 1.记一次线上Kafka消息堆积踩坑总结
**线上问题**

系统平稳运行两个多月，基本上没有问题，知道最近几天，突然出现Kafka手动提交失败，堆栈信息如下：

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224214.png)

通过堆栈信息可以看出，有两个重要参数： `session.timeout`  和 `max.poll.records`

`session.timeout.ms`：在使用Kafka的团队管理设施时，用于检测消费者失败的超时时间。消费者定期发送心跳来向经纪人表明其活跃度。如果代理在该会话超时到期之前没有收到心跳，那么代理将从该组中删除该消费者并启动重新平衡。

`max.poll.records`：在一次调用 `poll()`中返回的最大记录数。

根据堆栈的提示，他让增加 `session.timeout.ms` 时间 或者 减少 `max.poll.records`。



## 2.Kafka下线 broker 的操作

背景：主动下线是指broker运行正常，因为机器需要运维（升级操作系统，添加磁盘等）而主动停止broker

分两种情况处理：
- 所有的topic的 `replica >= 2`

	此时，直接停止一个broker，会自动触发 `leader election` 操作，不过目前 `leader election` 是逐个partition进行，等待所有partition完成 `leader election` 耗时较长，这样不可服务的时间就比较长。为了缩短不可服务时间窗口，可以主动触发停止broker操作，这样可以逐个partition转移，直到所有partition完成转移，再停止broker。

    ```
    root@lizhitao:/data/kafka_2.10-0.8.1# bin/kafka-run-class.sh kafka.admin.ShutdownBroker --zookeeper 192.168.2.225:2183/config/mobile/mq/mafka02 --broker #brokerId# --num.retries 3 --retry.interval.ms 60
    ```

	然后shutdown broker

    ```
    root@lizhitao:/data/kafka_2.10-0.8.1# bin/kafka-server-stop.sh\
    ```

- 存在topic的 `replica=1`

  当存在topic的副本数小于2，只能手工把当前broker上这些topic对应的partition转移到其他broker上。当此broker上剩余的topic的replica > 2时，参照上面的处理方法继续处理。



## 3.kafka的consumer初始化时获取不到消息

发现一个问题，如果使用的是一个高级的kafka接口 那么默认的情况下如果某个topic没有变化 则consumer消费不到消息 比如某个消息生产了2w条，此时producer不再生产消息，然后另外一个consumer启动，此时拿不到消息.

原因解释：

`auto.offset.reset`：如果zookeeper没有offset值或offset值超出范围。那么就给个初始的offset。有smallest、largest、anything可选，分别表示给当前最小的offset、当前最大的offset、抛异常。默认largest

### 3.1.解决过程
然后我琢磨，上线两个月都没有问题，为什么最近突然出现问题了。我想肯定是业务系统有什么动作，我就去问了一个下，果然头一天风控系统kafka挂掉了，并进行了数据重推，导致了数据阻塞。但是我又想即使阻塞了也会慢慢消费掉的，不应该报错呀。后来我看了一下kafka官网上的参数介绍，发现 `max.poll.records` 默认是2147483647 （0.10.0.1版本），也就是kafka里面有多少poll多少，如果消费者拿到的这些数据在制定时间内消费不完，就会手动提交失败，数据就会回滚到kafka中，会发生重复消费的情况。如此循环，数据就会越堆越多。后来咨询了公司的kafka大神，他说我的kafka版本跟他的集群版本不一样让我升级kafka版本。于是我就升级到了0.10.2.1，查阅官网发现这个版本的 `max.poll.records` 默认是500，可能kafka开发团队也意识到了这个问题。并且这个版本多了一个 `max.poll.interval.ms` 这个参数，默认是300s。这个参数的大概意思就是kafka消费者在一次poll内，业务处理时间不能超过这个时间。后来升级了kafka版本，把max.poll.records改成了50个之后，上了一次线，准备观察一下。上完线已经晚上9点了，于是就打卡回家了，明天看结果。第二天早起满心欢喜准备看结果，以为会解决这个问题，谁曾想还是堆积。我的天，思来想去，也想不出哪里有问题。于是就把处理各个业务的代码前后执行时间打印出来看一下，添加代码，提交上线。然后观察结果，发现大部分时间都用在数据库IO上了，并且执行时间很慢，大部分都是2s。于是想可能刚上线的时候数据量比较小，查询比较快，现在数据量大了，就比较慢了。当时脑子里第一想法就是看了一下常用查询字段有没有添加索引，一看没有，然后马上添加索引。加完索引观察了一下，处理速度提高了好几倍。虽然单条业务处理的快乐， 但是堆积还存在，后来发现，业务系统大概1s推送3、4条数据，但是我kafka现在是单线程消费，速度大概也是这么多。再加上之前的堆积，所以消费还是很慢。于是业务改成多线程消费，利用线程池，开启了10个线程，上线观察。几分钟就消费完了。大功告成，此时此刻，心里舒坦了好多。不容易呀！

### 3.2.总结：
1. 使用Kafka时，消费者每次poll的数据业务处理时间不能超过kafka的 `max.poll.interval.ms`，该参数在kafka0.10.2.1中的默认值是300s,所以要综合业务处理时间和每次poll的数据数量。
2. Java线程池大小的选择

   对于CPU密集型应用，也就是计算密集型，线程池大小应该设置为CPU核数+1；

   对于IO密集型应用 ，线程池大小设置为    2*CPU核数+1.    



## 4.生产者阶段重复场景

### 4.1.根本原因
生产发送的消息没有收到正确的broke响应，导致producer重试。

producer发出一条消息，broke落盘以后因为网络等种种原因发送端得到一个发送失败的响应或者网络中断，然后producer收到一个可恢复的Exception重试消息导致消息重复。  

### 4.2.说明

1. `new KafkaProducer()` 后创建一个后台线程KafkaThread扫描RecordAccumulator中是否有消息； 
2. 调用 `KafkaProducer.send()` 发送消息，实际上只是把消息保存到RecordAccumulator中； 
3. 后台线程KafkaThread扫描到RecordAccumulator中有消息后，将消息发送到kafka集群； 
4. 如果发送成功，那么返回成功； 
5. 如果发送失败，那么判断是否允许重试。如果不允许重试，那么返回失败的结果；如果允许重试，把消息再保存到RecordAccumulator中，等待后台线程KafkaThread扫描再次发送；
### 4.3.生产者发送重复解决方案
#### 4.3.1.启动kafka的幂等性
要启动kafka的幂等性，无需修改代码，默认为关闭，需要修改配置文件: `enable.idempotence=true` 同时要求 `ack=all` 且 `retries>1`。

幂等原理：

每个producer有一个producer id，服务端会通过这个id关联记录每个producer的状态，每个producer的每条消息会带上一个递增的sequence，服务端会记录每个producer对应的当前最大sequence，`producerId + sequence` ，如果新的消息带上的sequence不大于当前的最大sequence就拒绝这条消息，如果消息落盘会同时更新最大sequence，这个时候重发的消息会被服务端拒掉从而避免消息重复。该配置同样应用于kafka事务中。

#### 4.3.2.ack=0，不重试。
可能会丢消息，适用于吞吐量指标重要性高于数据丢失，例如：日志收集。



## 5.消费者数据重复场景及解决方案

### 5.1.根本原因
数据消费完没有及时提交offset到broke。

### 5.2.场景
消息消费端在消费过程中挂掉没有及时提交offset到broke，另一个消费端启动拿之前记录的offset开始消费，由于offset的滞后性可能会导致新启动的客户端有少量重复消费。

### 5.3.解决方案
- 取消自动自动提交

  每次消费完或者程序退出时手动提交。这可能也没法保证一条重复。

- 下游做幂等

  一般的解决方案是让下游做幂等或者尽量每消费一条消息都记录offset，对于少数严格的场景可能需要把offset或唯一ID,例如订单ID和下游状态更新放在同一个数据库里面做事务来保证精确的一次更新或者在下游数据表里面同时记录消费offset，然后更新下游数据的时候用消费位点做乐观锁拒绝掉旧位点的数据更新。



## 6.如何保证Kafka不丢失消息

### 6.1.生产者丢失消息的情况
生产者(Producer) 调用send方法发送消息之后，消息可能因为网络问题并没有发送过去。

所以，我们不能默认在调用send方法发送消息之后，消息就发送成功了。为了确定消息是发送成功，我们要判断消息发送的结果。但是要注意的是  Kafka 生产者(Producer) 使用  send 方法发送消息实际上是异步的操作，我们可以通过 get()方法获取调用结果，但是这样也让它变为了同步操作，示例代码如下：
```
SendResult<String, Object> sendResult = kafkaTemplate.send(topic, o).get();
if (sendResult.getRecordMetadata() != null) {
  logger.info("生产者成功发送消息到" + sendResult.getProducerRecord().topic() + "-> " + sendResult.getProducerRecord().value().toString());
}
```
但是一般不推荐这么做！可以采用为其添加回调函数的形式，示例代码如下：
```
ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, o);
future.addCallback(result -> logger.info("生产者成功发送消息到topic:{} partition:{}的消息", result.getRecordMetadata().topic(), result.getRecordMetadata().partition()),
		ex -> logger.error("生产者发送消失败，原因：{}", ex.getMessage()));
```
如果消息发送失败的话，我们检查失败的原因之后重新发送即可！

另外这里推荐为 Producer 的retries（重试次数）设置一个比较合理的值，一般是 3 ，但是为了保证消息不丢失的话一般会设置比较大一点。设置完成之后，当出现网络问题之后能够自动重试消息发送，避免消息丢失。另外，建议还要设置重试间隔，因为间隔太小的话重试的效果就不明显了，网络波动一次你3次一下子就重试完了

### 6.2.消费者丢失消息的情况
我们知道消息在被追加到 Partition(分区)的时候都会分配一个特定的偏移量（offset）。偏移量（offset)表示 Consumer 当前消费到的 Partition(分区)的所在的位置。Kafka 通过偏移量（offset）可以保证消息在分区内的顺序性。

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224002.png)

当消费者拉取到了分区的某个消息之后，消费者会自动提交了 offset。自动提交的话会有一个问题，试想一下，当消费者刚拿到这个消息准备进行真正消费的时候，突然挂掉了，消息实际上并没有被消费，但是 offset 却被自动提交了。

解决办法也比较粗暴，我们手动关闭自动提交 offset，每次在真正消费完消息之后之后再自己手动提交 offset 。 但是，细心的朋友一定会发现，这样会带来消息被重新消费的问题。比如你刚刚消费完消息之后，还没提交 offset，结果自己挂掉了，那么这个消息理论上就会被消费两次。

### 6.3.Kafka 弄丢了消息
我们知道 Kafka 为分区（Partition）引入了多副本（Replica）机制。分区（Partition）中的多个副本之间会有一个叫做 leader 的家伙，其他副本称为 follower。我们发送的消息会被发送到 leader 副本，然后 follower 副本才能从 leader 副本中拉取消息进行同步。生产者和消费者只与 leader 副本交互。你可以理解为其他副本只是 leader 副本的拷贝，它们的存在只是为了保证消息存储的安全性。

试想一种情况：假如 leader 副本所在的 broker 突然挂掉，那么就要从 follower 副本重新选出一个 leader ，但是 leader 的数据还有一些没有被 follower 副本的同步的话，就会造成消息丢失。

**6.3.1.设置 acks = all**

解决办法就是我们设置  acks = all。acks 是 Kafka 生产者(Producer)  很重要的一个参数。

acks 的默认值即为1，代表我们的消息被leader副本接收之后就算被成功发送。当我们配置 acks = all 代表则所有副本都要接收到该消息之后该消息才算真正成功被发送。

**6.3.2.设置 replication.factor >= 3**

为了保证 leader 副本能有 follower 副本能同步消息，我们一般会为 topic 设置 replication.factor >= 3。这样就可以保证每个 分区(partition) 至少有 3 个副本。虽然造成了数据冗余，但是带来了数据的安全性。

**6.3.3.设置 min.insync.replicas > 1**

一般情况下我们还需要设置 min.insync.replicas> 1 ，这样配置代表消息至少要被写入到 2 个副本才算是被成功发送。min.insync.replicas 的默认值为 1 ，在实际生产中应尽量避免默认值 1。

但是，为了保证整个 Kafka 服务的高可用性，你需要确保 replication.factor > min.insync.replicas 。为什么呢？设想一下加入两者相等的话，只要是有一个副本挂掉，整个分区就无法正常工作了。这明显违反高可用性！一般推荐设置成 replication.factor = min.insync.replicas + 1。

**6.3.4.设置 unclean.leader.election.enable = false**

> Kafka 0.11.0.0版本开始 unclean.leader.election.enable 参数的默认值由原来的true 改为false

我们最开始也说了我们发送的消息会被发送到 leader 副本，然后 follower 副本才能从 leader 副本中拉取消息进行同步。多个 follower 副本之间的消息同步情况不一样，当我们配置了 unclean.leader.election.enable = false  的话，当 leader 副本发生故障时就不会从  follower 副本中和 leader 同步程度达不到要求的副本中选择出  leader ，这样降低了消息丢失的可能性。



## 7.kafka 消息堆积慢消费问题

### 7.1.问题描述

我们的物料筛选排序系统之前经常会出现操作效果延迟的情况。在筛选排序数据不对时，我们一般会查看ES里存储的数据和DB里存储的数据是否一致，发现在一段时间内确实会存在数据不一致。我们怀疑是数据流下发存在延迟，于是立刻查看了kafka里消息增量数量的监控，发现出问题时kafka消息增量一般都堆积比较严重，而且看kafka的消息数变化曲线，消息堆积被消费后下降的速度要明显慢于平时下降的速度。因为在凤巢消息堆积和增量变多有时候是业务上的流量变大导致的，比如某些特殊的活动导致广告主调用API大量编辑物料，但是消息堆积后消费者的速度不但跟不上了，反而变得更慢了，这个问题是急需解决的。

### 7.2.排查过程

我对消息堆积后消息被消费掉的速度变慢产生了好奇。首选我在思考是不是最近消费者模块有代码升级，导致消费者本身对业务的处理变慢了。但是看了git的提交之后,发现提交时间是在几个月前，要是是代码本身的问题，应该早就应该暴露了。

因为我们消费者模块主要的操作就是去读取kafka下发的增量然后解析增量的内容同步修改ES，然后怀疑是增量的什么原因，导致消息堆积后消息下降变慢。然后我在遇上线的环境上线了打印解析消息内容的日志，然后让QA同学用压测工具回放出现故障时的线上流量，然后我观察日志分析出问题的原因。结果发现了不但复现了消息堆积的情况，还发现消息堆积时还出现了大量的重复消息。于是我想到可能是由于消息堆积触发了kafka对消息回滚的策略，导致了大量的消息重发，因此存在这边消费者在消费消息，另一边却在不断重发消息，因此消息的下降速度会变慢。并且查看了错误日志，有大量以下报错

```
08-09 11:01:11 131 pool-7-thread-3 ERROR [] - 
commit failed 
org.apache.kafka.clients.consumer.CommitFailedException: Commit cannot be completed since the group has already rebalanced and assigned the partitions to another member. This means that the time between subsequent calls to poll() was longer than the configured max.poll.interval.ms, which typically implies that the poll loop is spending too much time message processing. You can address this either by increasing the session timeout or by reducing the maximum size of batches returned in poll() with max.poll.records.
        at org.apache.kafka.clients.consumer.internals.ConsumerCoordinator.sendOffsetCommitRequest(ConsumerCoordinator.java:713) ~[MsgAgent-jar-with-dependencies.jar:na]
        at org.apache.kafka.clients.consumer.internals.ConsumerCoordinator.commitOffsetsSync(ConsumerCoordinator.java:596) ~[MsgAgent-jar-with-dependencies.jar:na]
        at org.apache.kafka.clients.consumer.KafkaConsumer.commitSync(KafkaConsumer.java:1218) ~[MsgAgent-jar-with-dependencies.jar:na]
        at com.today.eventbus.common.MsgConsumer.run(MsgConsumer.java:121) ~[MsgAgent-jar-with-dependencies.jar:na]
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149) [na:1.8.0_161]
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624) [na:1.8.0_161]
        at java.lang.Thread.run(Thread.java:748) [na:1.8.0_161]
```

错误的意思是消费者在处理完一批poll的消息后，在同步提交偏移量给broker时出错。初步分析日志是由于当前消费者线程消费的分区已经被broker给回收了，因为kafka认为这个消费者死了，那么为什么呢？

### 7.3.原理分析

如下是我们消费者处理逻辑(省略业务相关代码)

```
while (isRunning) {
      ConsumerRecords<KEY, VALUE> records = consumer.poll(100);
      if (records != null && records.count() > 0) {
	      for (ConsumerRecord<KEY, VALUE> record : records) {
	          dealMessage(bizConsumer, record.value());
	          try {
	                //records记录全部完成后，才提交
	                consumer.commitSync();
	          } catch (CommitFailedException e) {
	                logger.error("commit failed,will break this for loop", e);
	                break;
	         }
	     }
	 }
 }
```

poll()方法该方法轮询返回消息集，调用一次可以获取一批消息。kafkaConsumer调用一次轮询方法只是拉取一次消息。客户端为了不断拉取消息，会用一个外部循环不断调用消费者的轮询方法。每次轮询到消息，在处理完这一批消息后，才会继续下一次轮询。

kafka的偏移量(offset)是由消费者进行管理的，偏移量有两种，拉取偏移量(position)与提交偏移量(committed)。拉取偏移量代表当前消费者分区消费进度。每次消息消费后，需要提交偏移量。在提交偏移量时，kafka会使用拉取偏移量的值作为分区的提交偏移量发送给协调者。如果没有提交偏移量，下一次消费者重新与broker连接后，会从当前消费者group已提交到broker的偏移量处开始消费。

我在网上查阅了消息堆积和消息重复的一些原因，发现问题可能出现在kafka的poll()设置上。

查阅kafka官网发现我用的那个版本的kafka主要有以下几个比较关键的指标:

- `max.poll.records`：一次poll返回的最大记录数默认是500

- `max.poll.interval.ms`：两次poll方法最大时间间隔这个参数，默认是300s

这次问题出现的原因为由于业务上下方的消息增量变多，导致堆积的消息过多，每一批poll()的处理都能达到500条消息，导致poll之后消费的时间过长。 服务端约定了和客户端 `max.poll.interval.ms`，两次poll最大间隔。如果客户端处理一批消息花费的时间超过了这个限制时间，broker可能就会把消费者客户端移除掉，提交偏移量又会报错。所以拉取偏移量没有提交到broker，分区又rebalance，下一次重新分配分区时，消费者会从最新的已提交偏移量处开始消费，这里就出现了重复消费的问题。而服务注册中心zookeeper以为客户端失效进行rebalance，因此连接到另外一台消费服务器，然而另外一台服务器也出现poll()超时，又进行rebalance…如此循环，才出现了一直重发消息，导致消息数量被消费后下降很慢。

### 7.4.Rebalance介绍

consumer订阅topic中的一个或者多个partition中的消息，一个consumer group下可以有多个consumer，一条消息只能被group中的一个consumer消费。consumer和consumer group的关系是动态维护的，并不固定，当某个consumer卡住或者挂掉时，该consumer订阅的partition会被重新分配给该group下其它consumer，用于保证服务的可用性。为维护consumer和group之间的关系，consumer会定期向服务端的coordinator(一个负责维持客户端与服务端关系的协调者)发送心跳heartbeat，当consumer因为某种原因如死机无法在session.timeout.ms配置的时间间隔内发送heartbeat时，coordinator会认为该consumer已死，它所订阅的partition会被重新分配给同一group的其它consumer，该过程叫：rebalanced。

kafka在0.10.1之后的版本，增加了另一个概念：`max.poll.interval.ms`，即最大的poll时间间隔。consumer是通过拉取的方式向服务端拉取数据，当超过指定时间间隔 `max.poll.interval.ms` 没有向服务端发送poll()请求，而心跳heartbeat线程仍然在继续，会认为该consumer锁死，就会将该consumer退出group，并进行再分配。

这是一个巧妙的设计，两个配置项对应2个线程，在0.10.0之前的版本中，是没有区分这2个线程的，即超过 `session.timeout.ms` 没有发送心跳就直接rebalance。`session.timeout.ms` 默认值是10秒，`max.poll.interval.ms` 默认值是300s，改进为2个线程的意义在于，heartbeat线程独立于consumer的消费能力，在后台运行，用于快速检查整个客户端服务是否可用（如发生宕机等情况），而poll线程与consumer消费能力挂勾，用于检查单个consumer是否可用，这样可以避免当某些consumer消费较久配置心跳时间很长的情况下，我们不必等到这么久才知道服务可能已经宕机了。

### 7.5.解决方案

使用Kafka时，消费者每次poll的数据业务处理时间不能超过kafka的 `max.poll.interval.ms`，可以考虑调大超时时间或者调小每次poll的数据量。

增加 `max.poll.interval.ms` 处理时长(默认间隔300s)

```
max.poll.interval.ms=300
```

修改分区拉取阈值(默认50s,建议压测评估调小)

```
max.poll.records = 50
```

可以考虑增强消费者的消费能力，使用线程池消费或者将消费者中耗时业务改成异步，并保证对消息是幂等处理

不但要有消息积压的监控，还可以考虑做消息消费速度的监控（前后两次offset比较）



## 8.Kafka集群消息积压问题及处理策略

### 8.1.Kafka消息积压的典型场景

**8.1.1. 实时/消费任务挂掉**

比如，我们写的实时应用因为某种原因挂掉了，并且这个任务没有被监控程序监控发现通知相关负责人，负责人又没有写自动拉起任务的脚本进行重启。

那么在我们重新启动这个实时应用进行消费之前，这段时间的消息就会被滞后处理，如果数据量很大，可就不是简单重启应用直接消费就能解决的。

**8.1.2. Kafka分区数设置的不合理（太少）和消费者"消费能力"不足**

Kafka单分区生产消息的速度qps通常很高，如果消费者因为某些原因（比如受业务逻辑复杂度影响，消费时间会有所不同），就会出现消费滞后的情况。

此外，Kafka分区数是Kafka并行度调优的最小单元，如果Kafka分区数设置的太少，会影响Kafka consumer消费的吞吐量。

**8.1.3. Kafka消息的key不均匀，导致分区间数据不均衡**

在使用Kafka producer消息时，可以为消息指定key，但是要求key要均匀，否则会出现Kafka分区间数据不均衡。

### 8.2.那么，针对上述的情况，有什么好的办法处理数据积压呢？

一般情况下，针对性的解决办法有以下几种：

**8.2.1. 实时/消费任务挂掉导致的消费滞后**

- 任务重新启动后直接消费最新的消息，对于"滞后"的历史数据采用离线程序进行"补漏"。

  此外，建议将任务纳入监控体系，当任务出现问题时，及时通知相关负责人处理。当然任务重启脚本也是要有的，还要求实时框架异常处理能力要强，避免数据不规范导致的不能重新拉起任务。

- 任务启动从上次提交offset处开始消费处理

  如果积压的数据量很大，需要增加任务的处理能力，比如增加资源，让任务能尽可能的快速消费处理，并赶上消费最新的消息

**8.2.2. Kafka分区少了**

如果数据量很大，合理的增加Kafka分区数是关键。如果利用的是Spark流和Kafka direct approach方式，也可以对KafkaRDD进行repartition重分区，增加并行度处理。

**8.2.3. 由于Kafka消息key设置的不合理，导致分区数据不均衡**

可以在Kafka producer处，给key加随机后缀，使其均衡。

















 	