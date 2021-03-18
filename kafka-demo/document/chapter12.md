[toc]



# Kafka 实际问题解决

## 一、记一次线上Kafka消息堆积踩坑总结
### 线上问题
系统平稳运行两个多月，基本上没有问题，知道最近几天，突然出现Kafka手动提交失败，堆栈信息如下：
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224214.png)

通过堆栈信息可以看出，有两个重要参数： session.timeout  和 max.poll.records

session.timeout.ms : 在使用Kafka的团队管理设施时，用于检测消费者失败的超时时间。消费者定期发送心跳来向经纪人表明其活跃度。如果代理在该会话超时到期之前没有收到心跳，那么代理将从该组中删除该消费者并启动重新平衡。

max.poll.records : 在一次调用poll（）中返回的最大记录数。

根据堆栈的提示，他让增加 session.timeout.ms 时间 或者 减少 max.poll.records。



## 二、Kafka下线 broker 的操作

背景：主动下线是指broker运行正常，因为机器需要运维（升级操作系统，添加磁盘等）而主动停止broker

分两种情况处理：
- 所有的topic的replica >= 2
此时，直接停止一个broker，会自动触发leader election操作，不过目前leader election是逐个partition进行，等待所有partition完成leader election耗时较长，这样不可服务的时间就比较长。为了缩短不可服务时间窗口，可以主动触发停止broker操作，这样可以逐个partition转移，直到所有partition完成转移，再停止broker。
```
root@lizhitao:/data/kafka_2.10-0.8.1# bin/kafka-run-class.sh kafka.admin.ShutdownBroker --zookeeper 192.168.2.225:2183/config/mobile/mq/mafka02 --broker #brokerId# --num.retries 3 --retry.interval.ms 60
```
然后shutdown broker
```
root@lizhitao:/data/kafka_2.10-0.8.1# bin/kafka-server-stop.sh\
```
- 存在topic的replica=1
当存在topic的副本数小于2，只能手工把当前broker上这些topic对应的partition转移到其他broker上。当此broker上剩余的topic的replica > 2时，参照上面的处理方法继续处理。



## 三、kafka的consumer初始化时获取不到消息

发现一个问题，如果使用的是一个高级的kafka接口 那么默认的情况下如果某个topic没有变化 则consumer消费不到消息 比如某个消息生产了2w条，此时producer不再生产消息，然后另外一个consumer启动，此时拿不到消息.

原因解释：
auto.offset.reset：如果zookeeper没有offset值或offset值超出范围。那么就给个初始的offset。有smallest、largest、anything可选，分别表示给当前最小的offset、当前最大的offset、抛异常。默认largest

### 解决过程
然后我琢磨，上线两个月都没有问题，为什么最近突然出现问题了。我想肯定是业务系统有什么动作，我就去问了一个下，果然头一天风控系统kafka挂掉了，并进行了数据重推，导致了数据阻塞。但是我又想即使阻塞了也会慢慢消费掉牙，不应该报错呀。后来我看了一下kafka官网上的参数介绍，发现max.poll.records默认是2147483647 （0.10.0.1版本），也就是kafka里面有多少poll多少，如果消费者拿到的这些数据在制定时间内消费不完，就会手动提交失败，数据就会回滚到kafka中，会发生重复消费的情况。如此循环，数据就会越堆越多。后来咨询了公司的kafka大神，他说我的kafka版本跟他的集群版本不一样让我升级kafka版本。于是我就升级到了0.10.2.1，查阅官网发现这个版本的max.poll.records默认是500，可能kafka开发团队也意识到了这个问题。并且这个版本多了一个max.poll.interval.ms这个参数，默认是300s。这个参数的大概意思就是kafka消费者在一次poll内，业务处理时间不能超过这个时间。后来升级了kafka版本，把max.poll.records改成了50个之后，上了一次线，准备观察一下。上完线已经晚上9点了，于是就打卡回家了，明天看结果。第二天早起满心欢喜准备看结果，以为会解决这个问题，谁曾想还是堆积。我的天，思来想去，也想不出哪里有问题。于是就把处理各个业务的代码前后执行时间打印出来看一下，添加代码，提交上线。然后观察结果，发现大部分时间都用在数据库IO上了，并且执行时间很慢，大部分都是2s。于是想可能刚上线的时候数据量比较小，查询比较快，现在数据量大了，就比较慢了。当时脑子里第一想法就是看了一下常用查询字段有没有添加索引，一看没有，然后马上添加索引。加完索引观察了一下，处理速度提高了好几倍。虽然单条业务处理的快乐， 但是堆积还存在，后来发现，业务系统大概1s推送3、4条数据，但是我kafka现在是单线程消费，速度大概也是这么多。再加上之前的堆积，所以消费还是很慢。于是业务改成多线程消费，利用线程池，开启了10个线程，上线观察。几分钟就消费完了。大功告成，此时此刻，心里舒坦了好多。不容易呀！

### 总结：
1. 使用Kafka时，消费者每次poll的数据业务处理时间不能超过kafka的max.poll.interval.ms，该参数在kafka0.10.2.1中的默认值是300s,所以要综合业务处理时间和每次poll的数据数量。
2. Java线程池大小的选择，

对于CPU密集型应用，也就是计算密集型，线程池大小应该设置为CPU核数+1；

对于IO密集型应用 ，线程池大小设置为    2*CPU核数+1.    



## 四、生产者阶段重复场景

### 根本原因
生产发送的消息没有收到正确的broke响应，导致producer重试。

producer发出一条消息，broke落盘以后因为网络等种种原因发送端得到一个发送失败的响应或者网络中断，然后producer收到一个可恢复的Exception重试消息导致消息重复。  
### 重试过程
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224230.png)
说明： 

1. new KafkaProducer()后创建一个后台线程KafkaThread扫描RecordAccumulator中是否有消息； 
2. 调用KafkaProducer.send()发送消息，实际上只是把消息保存到RecordAccumulator中； 
3. 后台线程KafkaThread扫描到RecordAccumulator中有消息后，将消息发送到kafka集群； 
4. 如果发送成功，那么返回成功； 
5. 如果发送失败，那么判断是否允许重试。如果不允许重试，那么返回失败的结果；如果允许重试，把消息再保存到RecordAccumulator中，等待后台线程KafkaThread扫描再次发送；
### 可恢复异常说明
异常是RetriableException类型或者TransactionManager允许重试；RetriableException类继承关系如下：
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224241.png)
### 记录顺序问题
如果设置max.in.flight.requests.per.connection大于1（默认5，单个连接上发送的未确认请求的最大数量，表示上一个发出的请求没有确认下一个请求又发出了）。大于1可能会改变记录的顺序，因为如果将两个batch发送到单个分区，第一个batch处理失败并重试，但是第二个batch处理成功，那么第二个batch处理中的记录可能先出现被消费。

设置max.in.flight.requests.per.connection为1，可能会影响吞吐量，可以解决单台producer发送顺序问题。如果多个producer，producer1先发送一个请求，producer2后发送请求，这是producer1返回可恢复异常，重试一定次数成功了。虽然时producer1先发送消息，但是producer2发送的消息会被先消费。

## 生产者发送重复解决方案
### 启动kafka的幂等性
要启动kafka的幂等性，无需修改代码，默认为关闭，需要修改配置文件:enable.idempotence=true 同时要求 ack=all 且 retries>1。

幂等原理：
每个producer有一个producer id，服务端会通过这个id关联记录每个producer的状态，每个producer的每条消息会带上一个递增的sequence，服务端会记录每个producer对应的当前最大sequence，producerId + sequence ，如果新的消息带上的sequence不大于当前的最大sequence就拒绝这条消息，如果消息落盘会同时更新最大sequence，这个时候重发的消息会被服务端拒掉从而避免消息重复。该配置同样应用于kafka事务中。

### ack=0，不重试。
可能会丢消息，适用于吞吐量指标重要性高于数据丢失，例如：日志收集。

### 消费者数据重复场景及解决方案
### 根本原因
数据消费完没有及时提交offset到broke。

### 场景
消息消费端在消费过程中挂掉没有及时提交offset到broke，另一个消费端启动拿之前记录的offset开始消费，由于offset的滞后性可能会导致新启动的客户端有少量重复消费。

### 解决方案
- 取消自动自动提交
每次消费完或者程序退出时手动提交。这可能也没法保证一条重复。
- 下游做幂等
一般的解决方案是让下游做幂等或者尽量每消费一条消息都记录offset，对于少数严格的场景可能需要把offset或唯一ID,例如订单ID和下游状态更新放在同一个数据库里面做事务来保证精确的一次更新或者在下游数据表里面同时记录消费offset，然后更新下游数据的时候用消费位点做乐观锁拒绝掉旧位点的数据更新。





## 如何保证Kafka不丢失消息

### 生产者丢失消息的情况
生产者(Producer) 调用send方法发送消息之后，消息可能因为网络问题并没有发送过去。

所以，我们不能默认在调用send方法发送消息之后，消息就发送成功了。为了确定消息是发送成功，我们要判断消息发送的结果。但是要注意的是  Kafka 生产者(Producer) 使用  send 方法发送消息实际上是异步的操作，我们可以通过 get()方法获取调用结果，但是这样也让它变为了同步操作，示例代码如下：
```
SendResult<String, Object> sendResult = kafkaTemplate.send(topic, o).get();
if (sendResult.getRecordMetadata() != null) {
  logger.info("生产者成功发送消息到" + sendResult.getProducerRecord().topic() + "-> " + sendRe
              sult.getProducerRecord().value().toString());
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

### 消费者丢失消息的情况
我们知道消息在被追加到 Partition(分区)的时候都会分配一个特定的偏移量（offset）。偏移量（offset)表示 Consumer 当前消费到的 Partition(分区)的所在的位置。Kafka 通过偏移量（offset）可以保证消息在分区内的顺序性。
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317224002.png)
当消费者拉取到了分区的某个消息之后，消费者会自动提交了 offset。自动提交的话会有一个问题，试想一下，当消费者刚拿到这个消息准备进行真正消费的时候，突然挂掉了，消息实际上并没有被消费，但是 offset 却被自动提交了。

解决办法也比较粗暴，我们手动关闭自动提交 offset，每次在真正消费完消息之后之后再自己手动提交 offset 。 但是，细心的朋友一定会发现，这样会带来消息被重新消费的问题。比如你刚刚消费完消息之后，还没提交 offset，结果自己挂掉了，那么这个消息理论上就会被消费两次。

### Kafka 弄丢了消息
我们知道 Kafka 为分区（Partition）引入了多副本（Replica）机制。分区（Partition）中的多个副本之间会有一个叫做 leader 的家伙，其他副本称为 follower。我们发送的消息会被发送到 leader 副本，然后 follower 副本才能从 leader 副本中拉取消息进行同步。生产者和消费者只与 leader 副本交互。你可以理解为其他副本只是 leader 副本的拷贝，它们的存在只是为了保证消息存储的安全性。

试想一种情况：假如 leader 副本所在的 broker 突然挂掉，那么就要从 follower 副本重新选出一个 leader ，但是 leader 的数据还有一些没有被 follower 副本的同步的话，就会造成消息丢失。

#### 设置 acks = all
解决办法就是我们设置  acks = all。acks 是 Kafka 生产者(Producer)  很重要的一个参数。

acks 的默认值即为1，代表我们的消息被leader副本接收之后就算被成功发送。当我们配置 acks = all 代表则所有副本都要接收到该消息之后该消息才算真正成功被发送。

#### 设置 replication.factor >= 3
为了保证 leader 副本能有 follower 副本能同步消息，我们一般会为 topic 设置 replication.factor >= 3。这样就可以保证每个 分区(partition) 至少有 3 个副本。虽然造成了数据冗余，但是带来了数据的安全性。

#### 设置 min.insync.replicas > 1
一般情况下我们还需要设置 min.insync.replicas> 1 ，这样配置代表消息至少要被写入到 2 个副本才算是被成功发送。min.insync.replicas 的默认值为 1 ，在实际生产中应尽量避免默认值 1。

但是，为了保证整个 Kafka 服务的高可用性，你需要确保 replication.factor > min.insync.replicas 。为什么呢？设想一下加入两者相等的话，只要是有一个副本挂掉，整个分区就无法正常工作了。这明显违反高可用性！一般推荐设置成 replication.factor = min.insync.replicas + 1。

#### 设置 unclean.leader.election.enable = false
> Kafka 0.11.0.0版本开始 unclean.leader.election.enable 参数的默认值由原来的true 改为false

我们最开始也说了我们发送的消息会被发送到 leader 副本，然后 follower 副本才能从 leader 副本中拉取消息进行同步。多个 follower 副本之间的消息同步情况不一样，当我们配置了 unclean.leader.election.enable = false  的话，当 leader 副本发生故障时就不会从  follower 副本中和 leader 同步程度达不到要求的副本中选择出  leader ，这样降低了消息丢失的可能性。




