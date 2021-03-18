[toc]



# Kafka工作流程和文件存储机制

## Kafka 工作流程

![05](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317211709.png)

Kafka 中消息是以 topic 进行分类的， producer生产消息，consumer消费消息，都是面向 topic的。(从命令行操作看出)

```bat
bin\windows\kafka-console-producer.bat --broker-list localhost:9092 --topic test

bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test --from-beginning
```

topic 是逻辑上的概念，而 partition 是物理上的概念，每个 partition 对应于一个 log 文件，该 log 文件中存储的就是 producer 生产的数据。（topic = N partition，partition = log）

Producer 生产的数据会被不断追加到该log 文件末端，且每条数据都有自己的 offset。 consumer组中的每个consumer， 都会实时记录自己消费到了哪个 offset，以便出错恢复时，从上次的位置继续消费。（producer -> log with offset -> consumer(s)）



## Kafka 文件存储

### 1、目录结构

在数据存储中，每个主题又划分了多个分区（partition），主题+分区会在每个broker以文件夹的形式存在

![1722808db5532731](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318223216.jpg)


可以看到，首先创建了一个名为 mytopic 的主题，并且分配了3个分区，最终在broker的日志存储目录中创建了3个文件夹，名称分别为“<主题名>+<分区序号>”。每个文件夹内包含了日志索引文件（“.index”和“.timeindex”）和日志数据文件（“.log”）两部分。

### 2、数据文件存储结构

在每个partition中，包含了很多个LogSegment，LogSegment是由一个**日志文件**和**两个索引文件**组成。每个LogSegment的大小相等（大小可以在**config/server.properties**中通过 `og.segment.bytes` 属性配置，默认为1073741824字节，即1GB）。
LogSegment命名规则：第一个segment从0开始，后续每个segment文件名为上一个segment文件最后一条消息的offset, offset的数值最大为64位（long类型），20位数字字符长度，没有数字用0填充。
下面这张图很直观的表明了每个partition中的数据存储方式（[图片出处](https://www.jianshu.com/p/3e54a5a39683)）：

![1722808db5b26de6](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318223259.jpg)

### 3、索引文件

kafka的.log数据文件大小1G，如果没有索引，每次读取数据时都需要从文件头开始读，这样未免效率太低了。所以出现了以 .index 为后缀名的索引文件。每个索引条目由offset和position组成，offset表示每条消息的逻辑偏移量，postition标识消息在.log数据文件中实际的物理偏移量。这样，每个索引条目就可以唯一确定在各个分区数据文件的一条消息。其中，Kafka采用稀疏索引存储的方式，默认是日志写入大小达到4KB时，才会在.index中增加一个索引项。可以通过 `log.index.interval.bytes` 来设置这个间隔大小。

![1722808db77066ac](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318223324.jpg)
.timeindex 索引是在 kafka 0.10.1.0 版本中新增的基于时间的索引文件，他包含两个字段 **时间戳**和 **消息偏移量**，它的作用则是为了解决根据时间戳快速定位消息所在位置。

我们可以通过如下命令将二进制分段的索引和日志数据文件内容转换为字符型文件：

- **.log文件**

```
./bin/kafka-run-class.sh kafka.tools.DumpLogSegments --files /tmp/kafka-logs/mytopic-0/000000000000000000.log --print-data-log > ~/000000000000000000_txt.log
```

![1722808db7faa7cd](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318223413.jpg)

- **.index文件**

```
./bin/kafka-run-class.sh kafka.tools.DumpLogSegments --files /tmp/kafka-logs/mytopic-0/000000000000000000.index --print-data-log > ~/000000000000000000_txt.index
```

![1722808dba8161c4](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318223439.jpg)

- **.timeindex文件**

```
./bin/kafka-run-class.sh kafka.tools.DumpLogSegments --files /tmp/kafka-logs/mytopic-0/000000000000000000.timeindex --print-data-log > ~/000000000000000000_txt.timeindex
```

![1722808db8509fa3](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318223458.jpg)

### 4、消息结构

kafka的消息结构经过了多次版本的调整，关于这部分最好还是参考[官方文档](https://kafka.apache.org/documentation/#messageformat)，这里只简单说一下，列一些关键属性。
为了保证传输效率，生产者会将多条消息压缩到一起批量发送到broker，在broker中存储的消息也是压缩后的，最终消息会在消费端解压消费。最新版本的kafka中，被压缩在一起的一批消息被称为 Record Batch ，在 Record Batch中包含了多条 Record，大概的组织方式就像上图我们dump出来的.log文件一样。

这里简要的列举一下关键属性：

| **消息属性** | **属性说明**                           |
| ------------ | -------------------------------------- |
| baseOffset   | 这一批消息中**起始**消息的offset       |
| lastOffset   | 这一批消息中**结束**消息的offset       |
| count        | 这一批消息中的消息总数                 |
| size         | 这一批消息的总大小                     |
| magic        | 消息格式的版本号                       |
| crc32        | crc32校验值。校验范围为crc32之后字节。 |
| offset       | 当前消息在partition的逻辑偏移量        |
| CreateTime   | 时间戳                                 |
| keysize      | 当前消息key大小                        |
| valuesize    | 当前消息value大小                      |
| key          | 消息的key                              |
| payload      | 消息数据                               |



## 问题

### 一、kafka日志段如何读写

**Kafka的存储结构**

总所周知，Kafka的Topic可以有多个分区，分区其实就是最小的读取和存储结构，即Consumer看似订阅的是Topic，实则是从Topic下的某个分区获得消息，Producer也是发送消息也是如此。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318221737.png)

上图是总体逻辑上的关系，映射到实际代码中在磁盘上的关系则是如下图所示：

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318221753.png)

每个分区对应一个Log对象，在磁盘中就是一个子目录，子目录下面会有多组日志段即多Log Segment，每组日志段包含：消息日志文件(以log结尾)、位移索引文件(以index结尾)、时间戳索引文件(以timeindex结尾)。其实还有其它后缀的文件，例如.txnindex、.deleted等等。篇幅有限，暂不提起。

**日志段的定义**

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318221821.webp)



![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318221827.webp)

`indexIntervalBytes`可以理解为插了多少消息之后再建一个索引，由此可以看出**Kafka的索引其实是稀疏索引**，这样可以避免索引文件占用过多的内存，从而可以**在内存中保存更多的索引**。对应的就是Broker 端参数`log.index.interval.bytes` 值，默认4KB。

实际的**通过索引**查找消息过程是先通过offset找到索引所在的文件，然后**通过二分法**找到离目标最近的索引，再顺序遍历消息文件找到目标文件。这波操作时间复杂度为`O(log2n)+O(m)`,n是索引文件里索引的个数，m为稀疏程度。

**这就是空间和时间的互换，又经过数据结构与算法的平衡，妙啊！**

再说下`rollJitterMs`,这其实是个扰动值，对应的参数是`log.roll.jitter.ms`,这其实就要说到日志段的切分了，`log.segment.bytes`,这个参数控制着日志段文件的大小，默认是1G，即当文件存储超过1G之后就新起一个文件写入。这是以大小为维度的，还有一个参数是`log.segment.ms`,以时间为维度切分。

那配置了这个参数之后如果有很多很多分区，然后因为这个参数是全局的，因此同一时刻需要做很多文件的切分，这磁盘IO就顶不住了啊，因此需要设置个`rollJitterMs`，来岔开它们。

**怎么样有没有联想到redis缓存的过期时间？过期时间加个随机数，防止同一时刻大量缓存过期导致缓存击穿数据库。看看知识都是通的啊！**



**日志段的写入**

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318222021.png)

1、判断下当前日志段是否为空，空的话记录下时间，来作为之后日志段的切分依据

2、确保位移值合法，最终调用的是`AbstractIndex.toRelative(..)`方法，即使判断offset是否小于0，是否大于int最大值。

3、append消息，实际上就是通过`FileChannel`将消息写入，当然只是写入内存中及页缓存，是否刷盘看配置。

4、更新日志段最大时间戳和最大时间戳对应的位移值。这个时间戳其实用来作为定期删除日志的依据

5、更新索引项，如果需要的话`(bytesSinceLastIndexEntry > indexIntervalBytes)`

最后再来个流程图

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318222040.webp)



**日志段的读取**

![640 (3)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318222057.webp)

1、根据第一条消息的offset，通过`OffsetIndex`找到对应的消息所在的物理位置和大小。

2、获取`LogOffsetMetadata`,元数据包含消息的offset、消息所在segment的起始offset和物理位置

3、判断`minOneMessage`是否为`true`,若是则调整为必定返回一条消息大小，其实就是在单条消息大于`maxSize`的情况下得以返回，防止消费者饿死

4、再计算最大的`fetchSize`,即（最大物理位移-此消息起始物理位移）和`adjustedMaxSize`的最小值(这波我不是很懂，因为以上一波操作`adjustedMaxSize`已经最小为一条消息的大小了)

5、调用 `FileRecords` 的 `slice` 方法从指定位置读取指定大小的消息集合，并且构造`FetchDataInfo`返回

再来个流程图：

![640 (3)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210318222109.png)



**小结**

从哪里跌倒就从哪里爬起来对吧，这波操作下来咱也不怕下次遇到面试官问了。

区区源码不过尔尔，哈哈哈哈(首先得要有气势)

实际上这只是Kafka源码的冰山一角，长路漫漫。虽说Kafka Broker都是由Scala写的，不过语言不是问题，这不看下来也没什么难点，注释也很丰富。遇到不知道的语法小查一下搞定。

所以强烈建议大家入手源码，从源码上理解。今天说的 `append` 和 `read` 是很核心的功能，但一看也并不复杂，所以不要被源码这两个字吓到了。

看源码可以让我们深入的理解内部的设计原理，精进我们的代码功力（经常看着看着，我擦还能这么写）。当然还有系统架构能力。



















