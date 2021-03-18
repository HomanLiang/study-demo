[toc]



# Kafka 消费者

## 消费者分区分配策略

### 消费方式

**consumer 采用 pull（拉） 模式从 broker 中读取数据**。

**push（推）模式很难适应消费速率不同的消费者，因为消息发送速率是由 broker 决定的**。它的目标是尽可能以最快速度传递消息，但是这样很容易造成 consumer 来不及处理消息，典型的表现就是拒绝服务以及网络拥塞。而 pull 模式则可以根据 consumer 的消费能力以适当的速率消费消息。

**pull 模式不足之处**是，如果 kafka 没有数据，消费者可能会陷入循环中， 一直返回空数据。 针对这一点， Kafka 的消费者在消费数据时会传入一个时长参数 timeout，如果当前没有数据可供消费， consumer 会等待一段时间之后再返回，这段时长即为 timeout。



### 分区分配策略

一个 consumer group 中有多个 consumer，一个 topic 有多个 partition，所以必然会涉及到 partition 的分配问题，即确定那个 partition 由哪个 consumer 来消费。

Kafka 有两种分配策略：

- round-robin循环
- range

#### Round Robin

关于Roudn Robin重分配策略，其主要采用的是一种轮询的方式分配所有的分区，该策略主要实现的步骤如下。这里我们首先假设有三个topic：t0、t1和t2，这三个topic拥有的分区数分别为1、2和3，那么总共有六个分区，这六个分区分别为：t0-0、t1-0、t1-1、t2-0、t2-1和t2-2。这里假设我们有三个consumer：C0、C1和C2，它们订阅情况为：C0订阅t0，C1订阅t0和t1，C2订阅t0、t1和t2。那么这些分区的分配步骤如下：

- 首先将所有的partition和consumer按照字典序进行排序，所谓的字典序，就是按照其名称的字符串顺序，那么上面的六个分区和三个consumer排序之后分别为：

  ![16](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317213655.png)

- 然后依次以按顺序轮询的方式将这六个分区分配给三个consumer，如果当前consumer没有订阅当前分区所在的topic，则轮询的判断下一个consumer：

- 尝试将t0-0分配给C0，由于C0订阅了t0，因而可以分配成功；

- 尝试将t1-0分配给C1，由于C1订阅了t1，因而可以分配成功；

- 尝试将t1-1分配给C2，由于C2订阅了t1，因而可以分配成功；

- 尝试将t2-0分配给C0，由于C0没有订阅t2，因而会轮询下一个consumer；

- 尝试将t2-0分配给C1，由于C1没有订阅t2，因而会轮询下一个consumer；

- 尝试将t2-0分配给C2，由于C2订阅了t2，因而可以分配成功；

- 同理由于t2-1和t2-2所在的topic都没有被C0和C1所订阅，因而都不会分配成功，最终都会分配给C2。

- 按照上述的步骤将所有的分区都分配完毕之后，最终分区的订阅情况如下：

  ![17](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317213712.png)

从上面的步骤分析可以看出，轮询的策略就是简单的将所有的partition和consumer按照字典序进行排序之后，然后依次将partition分配给各个consumer，如果当前的consumer没有订阅当前的partition，那么就会轮询下一个consumer，直至最终将所有的分区都分配完毕。但是从上面的分配结果可以看出，轮询的方式会导致每个consumer所承载的分区数量不一致，从而导致各个consumer压力不均一。

#### Range

所谓的Range重分配策略，就是首先会计算各个consumer将会承载的分区数量，然后将指定数量的分区分配给该consumer。这里我们假设有两个consumer：C0和C1，两个topic：t0和t1，这两个topic分别都有三个分区，那么总共的分区有六个：t0-0、t0-1、t0-2、t1-0、t1-1和t1-2。那么Range分配策略将会按照如下步骤进行分区的分配：

- 需要注意的是，Range策略是按照topic依次进行分配的，比如我们以t0进行讲解，其首先会获取t0的所有分区：t0-0、t0-1和t0-2，以及所有订阅了该topic的consumer：C0和C1，并且会将这些分区和consumer按照字典序进行排序；

- 然后按照平均分配的方式计算每个consumer会得到多少个分区，如果没有除尽，则会将多出来的分区依次计算到前面几个consumer。比如这里是三个分区和两个consumer，那么每个consumer至少会得到1个分区，而3除以2后还余1，那么就会将多余的部分依次算到前面几个consumer，也就是这里的1会分配给第一个consumer，总结来说，那么C0将会从第0个分区开始，分配2个分区，而C1将会从第2个分区开始，分配1个分区；

- 同理，按照上面的步骤依次进行后面的topic的分配。

- 最终上面六个分区的分配情况如下：

  ![18](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317213730.png)

可以看到，如果按照`Range`分区方式进行分配，其本质上是依次遍历每个topic，然后将这些topic的分区按照其所订阅的consumer数量进行平均的范围分配。这种方式从计算原理上就会导致排序在前面的consumer分配到更多的分区，从而导致各个consumer的压力不均衡。



## 消费者offset的存储

由于 consumer 在消费过程中可能会出现断电宕机等故障， consumer 恢复后，需要从故障前的位置的继续消费，所以 **consumer 需要实时记录自己消费到了哪个 offset**，以便故障恢复后继续消费。

![13](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317213742.png)

**Kafka 0.9 版本之前， consumer 默认将 offset 保存在 Zookeeper 中，从 0.9 版本开始，consumer 默认将 offset 保存在 Kafka 一个内置的 topic 中，该 topic 为__consumer_offsets**。

1. 修改配置文件 consumer.properties，`exclude.internal.topics=false`。
2. 读取 offset
   - 0.11.0.0 之前版本 - `bin/kafka-console-consumer.sh --topic __consumer_offsets --zookeeper hadoop102:2181 --formatter "kafka.coordinator.GroupMetadataManager\$OffsetsMessageFormatter" --consumer.config config/consumer.properties --from-beginning`
   - 0.11.0.0 及之后版本 - `bin/kafka-console-consumer.sh --topic __consumer_offsets --zookeeper hadoop102:2181 --formatter "kafka.coordinator.group.GroupMetadataManager\$OffsetsMessageFormatter" --consumer.config config/consumer.properties --from-beginning`



## 消费者组案例

### 需求

测试同一个消费者组中的消费者， **同一时刻只能有一个**消费者消费。

### 操作步骤

1.修改`%KAFKA_HOME\config\consumer.properties%`文件中的`group.id`属性。

```properties
group.id=shan-kou-zu
```

2.打开两个cmd，分别启动两个消费者。（以`%KAFKA_HOME\config\consumer.properties%`作配置参数）

```bat
bin\windows\kafka-console-consumer.bat --zookeeper 127.0.0.1:2181 --topic test --consumer.config config\consumer.properties
```

3.再打开一个cmd，启动一个生产者。

```bat
bin\windows\kafka-console-producer.bat --broker-list 127.0.0.1:9092 --topic test
```

4.在生产者窗口输入消息，观察两个消费者窗口。**会发现两个消费者窗口中，只有一个才会弹出消息**。