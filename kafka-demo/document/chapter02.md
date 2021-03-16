[toc]

# Kafka 安装部署

## Windows 版本安装

本次安装学习在Windows操作系统进行。（Linux版本的差别不大，运行脚本文件后缀从`bat`改为`sh`，配置路径改用Unix风格的）

### Step 1: Download the code

[下载代码](http://kafka.apache.org/downloads)并解压

下载[kafka 0.11.0.0](https://archive.apache.org/dist/kafka/0.11.0.0/kafka_2.11-0.11.0.0.tgz)版本，解压到`C:\Kafka\`路径下，Kafka主目录文件为`C:\Kafka\kafka_2.11-0.11.0.0`（下文用KAFKA_HOME表示）。



### Step 2: Start the server

Kafka 用到 ZooKeeper 功能，所以要预先运行ZooKeeper。

- 首先，修改`%KAFKA_HOME%\conf\zookeeper.properties`中的`dataDir=/tmp/zookeeper`，改为`dataDir=C:\\Kafka\\data\\zookeeper`。
- 创建新目录`C:\\Kafka\\data\\zookeeper`。
- 启动cmd，工作目录切换到`%KAFKA_HOME%`，执行命令行：

    ```bat
    start bin\windows\zookeeper-server-start.bat config\zookeeper.properties
    ```

- 修改`%KAFKA_HOME%\conf\server.properties`中的`log.dirs=/tmp/kafka-logs`，改为`log.dirs=C:\\Kafka\\data\\kafka-logs`。
- 创建新目录`C:\\Kafka\\data\\kafka-logs`。
- 另启动cmd，工作目录切换到`%KAFKA_HOME%`，执行命令行：

    ```bat
    start bin\windows\kafka-server-start.bat config\server.properties
    ```

- 可写一脚本，一键启动
- 关闭服务，`bin\windows\kafka-server-stop.bat`和`bin\windows\zookeeper-server-stop.bat`。

------

TODO:**一个问题**，通过`kafka-server-stop.bat`或右上角关闭按钮来关闭Kafka服务后，马上下次再启动Kafka，抛出异常，说某文件被占用，需清空`log.dirs`目录下文件，才能重启Kafka。

```
[2020-07-21 21:43:26,755] ERROR There was an error in one of the threads during logs loading: java.nio.file.FileSystemException: C:\Kafka\data\kafka-logs-0\my-replicated-topic-0\00000000000000000000.timeindex: 另一个程序正在使用此文件，进程无法访问。
 (kafka.log.LogManager)
...
```

参阅网络，这可能是在windows下的一个Bug，没有更好的解决方案，暂时写个py脚本用来对kafka的log文件进行删除。下次启动kafka，先运行这个删除脚本吧。

**好消息**，当你成功启动kafka，然后在对应的命令行窗口用`Ctrl + C`结束Kakfa，下次不用清理kafka日志，也能正常启动。



### Step 3: Create a topic

- 用单一partition和单一replica创建一个名为`test`的topic:

```bat
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
```

- 查看已创建的topic，也就刚才创建的名为`test`的topic：

```bat
bin\windows\kafka-topics.bat --list --zookeeper localhost:2181
```

或者，你可配置你的broker去自动创建未曾发布过的topic，代替手动创建topic



### Step 4: Send some messages

运行producer，然后输入几行文本，发至服务器：

```bat
bin\windows\kafka-console-producer.bat --broker-list localhost:9092 --topic test
>hello, kafka.
>what a nice day!
>to be or not to be. that' s a question.
```

请勿关闭窗口，下面步骤需要用到



### Step 5: Start a consumer

运行consumer，将[Step 4](https://my.oschina.net/jallenkwong/blog/4449224#)中输入的几行句子，标准输出。

```bat
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test --from-beginning
hello, kafka.
what a nice day!
to be or not to be. that' s a question.
```

若你另启cmd，执行命令行`bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test --from-beginning`来运行consumer，然后在[Step 4](https://my.oschina.net/jallenkwong/blog/4449224#)中producer窗口输入一行句子，如`I must admit, I can't help but feel a twinge of envy.`，两个consumer也会同时输出`I must admit, I can't help but feel a twinge of envy.`。



### Step 6: Setting up a multi-broker cluster

目前为止，我们仅作为一个单一broker，这不好玩。让我们弄个有三个节点的集群来玩玩。

- 首先，在`%KAFKA%\config\server.properties`的基础上创建两个副本`server-1.properties`和`server-2.properties`。

```bat
copy config\server.properties config\server-1.properties
copy config\server.properties config\server-2.properties
```

- 打开副本，编辑如下属性

```properties
#config/server-1.properties:
broker.id=1
listeners=PLAINTEXT://127.0.0.1:9093
log.dir=C:\\Kafka\\data\\kafka-logs-1
 
#config/server-2.properties:
broker.id=2
listeners=PLAINTEXT://127.0.0.1:9094
log.dir=C:\\Kafka\\data\\kafka-logs-2
```

这个`broker.id`属性是集群中每个节点的唯一永久的名称。

我们必须重写端口和日志目录，只是因为我们在同一台机器上运行它们，并且我们希望阻止brokers试图在同一个端口上注册或覆盖彼此的数据。

- 我们已经启动了Zookeeper和我们的单个节点，所以我们只需要启动两个新节点：

```bat
start bin\windows\kafka-server-start.bat config\server-1.properties
start bin\windows\kafka-server-start.bat config\server-2.properties
```

- 创建一个replication-factor为3的topic:

```bat
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 3 --partitions 1 --topic my-replicated-topic
```

- OK，现在我们有了一个集群，但是我们怎么知道哪个broker在做什么呢？那就运行`describe topics`命令：

```bat
bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic my-replicated-topic

Topic:my-replicated-topic       PartitionCount:1        ReplicationFactor:3
Configs:
		Topic: my-replicated-topic      Partition: 0    Leader: 0       
		Replicas: 0,1,2        Isr: 0,1,2
```

- 以下是输出的说明。第一行给出所有Partition的摘要，每一行提供有关一个Partition的信息。因为这个Topic只有一个Partition，所以只有一行。
  - "leader"是负责给定Partition的所有读写的节点。每个节点都可能成为Partition随机选择的leader。
  - "replicas"是复制此Partition日志的节点列表，无论它们是leader还是当前处于存活状态。
  - "isr"是一组 "in-sync" replicas。这是replicas列表的一个子集，它当前处于存活状态，并补充leader。

注意，**在我的示例中，node 0是Topic唯一Partition的leader**。（下面操作需要用到）

- 让我们为我们的新Topic发布一些信息：

```bat
bin\windows\kafka-console-producer.bat --broker-list localhost:9092 --topic my-replicated-topic

>There's sadness in your eyes, I don't want to say goodbye to you.
>Love is a big illusion, I should try to forget, but there's something left in m
y head.
>
```

- 让我们接收刚刚发布的信息吧！

```bat
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --from-beginning --topic my-replicated-topic

There's sadness in your eyes, I don't want to say goodbye to you.
Love is a big illusion, I should try to forget, but there's something left in my head.
```

- 让我们测试一下容错性，由上文可知，Broker 0 身为 leader，因此，让我们干掉它吧：
  - 先找出 Broker 0 的进程pid。
  - 杀掉 Broker 0 的进程。

```bat
wmic process where "caption='java.exe' and commandline like '%server.properties%'" get processid,caption
Caption   ProcessId
java.exe  7528

taskkill /pid 7528 /f
成功: 已终止 PID 为 7528 的进程。
```

- 原leader已被替换成它的flowers中的其中一个，并且 node 0 不在 in-sync replica 集合当中。

```bat
bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic my-replicated-topic

Topic:my-replicated-topic       PartitionCount:1        ReplicationFactor:3
Configs:
		Topic: my-replicated-topic      Partition: 0    Leader: 1       Replicas: 0,1,2 Isr: 1,2
```

- 尽管原leader已逝，当原来消息依然可以接收。（注意，参数`--bootstrap-server localhost:9093`，而不是`--bootstrap-server localhost:9092`）

```bat
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9093 --from-beginning --topic my-replicated-topic

There's sadness in your eyes, I don't want to say goodbye to you.
Love is a big illusion, I should try to forget, but there's something left in my head.
I don't forget the way your kissing, the feeling 's so strong which is lasting for so long.
```



### server.properties一瞥

```properties
#broker 的全局唯一编号，不能重复
broker.id=0
#删除 topic 功能使能
delete.topic.enable=true
#处理网络请求的线程数量
num.network.threads=3
#用来处理磁盘 IO 的现成数量
num.io.threads=8
#发送套接字的缓冲区大小
socket.send.buffer.bytes=102400
#接收套接字的缓冲区大小
socket.receive.buffer.bytes=102400
#请求套接字的缓冲区大小
socket.request.max.bytes=104857600
#kafka 运行日志存放的路径
log.dirs=/opt/module/kafka/logs
#topic 在当前 broker 上的分区个数
num.partitions=1
#用来恢复和清理 data 下数据的线程数量
num.recovery.threads.per.data.dir=1
#segment 文件保留的最长时间，超时将被删除
log.retention.hours=168
#配置连接 Zookeeper 集群地址
zookeeper.connect=hadoop102:2181,hadoop103:2181,hadoop104:2181
```















