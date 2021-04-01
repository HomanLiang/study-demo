[toc]

# Kafka API

## 生产者

### 生产者API流程

Kafka 的 Producer 发送消息采用的是异步发送的方式。在消息发送的过程中，涉及到了两个线程——main 线程和 Sender 线程，以及一个线程共享变量——RecordAccumulator。 main 线程将消息发送给 RecordAccumulator， Sender 线程不断从 RecordAccumulator 中拉取消息发送到 Kafka broker。

![19](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317220549.png)

相关参数：

- **batch.size**： 只有数据积累到 batch.size 之后， sender 才会发送数据。
- **linger.ms**： 如果数据迟迟未达到 batch.size， sender 等待 linger.time 之后就会发送数据。



### 异步发送API-普通生产者

**导入依赖**

pom.xml

```xml
<dependency>
	<groupId>org.apache.kafka</groupId>
	<artifactId>kafka-clients</artifactId>
	<version>0.11.0.0</version>
</dependency>
```

**编写代码**

需要用到的类：

- KafkaProducer：需要创建一个生产者对象，用来发送数据
- ProducerConfig：获取所需的一系列配置参数
- ProducerRecord：每条数据都要封装成一个 ProducerRecord 对象

CustomProducer.java

```java
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class CustomProducer {

	public static void main(String[] args) {
		Properties props = new Properties();
		// kafka 集群， broker-list
		
		props.put("bootstrap.servers", "127.0.0.1:9092");
		
		//可用ProducerConfig.ACKS_CONFIG 代替 "acks"
		//props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put("acks", "all");
		// 重试次数
		props.put("retries", 1);
		// 批次大小
		props.put("batch.size", 16384);
		// 等待时间
		props.put("linger.ms", 1);
		// RecordAccumulator 缓冲区大小
		props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		Producer<String, String> producer = new KafkaProducer<>(props);
		for (int i = 0; i < 100; i++) {
			producer.send(new ProducerRecord<String, String>("test", "test-" + Integer.toString(i),
					"test-" + Integer.toString(i)));
		}
		producer.close();
	}
}
```



### 异步发送API-带回调函数的生产者

回调函数会在 producer 收到 ack 时调用，为异步调用， 该方法有两个参数，分别是 RecordMetadata 和 Exception，如果 Exception 为 null，说明消息发送成功，如果Exception 不为 null，说明消息发送失败。

**注意**：消息发送失败会自动重试，不需要我们在回调函数中手动重试。

CallBackProducer.java

```java
import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class CallBackProducer {
	public static void main(String[] args) {
		Properties props = new Properties();
		props.put("bootstrap.servers", "127.0.0.1:9092");//kafka 集群， broker-list
		props.put("acks", "all");
		props.put("retries", 1);//重试次数
		props.put("batch.size", 16384);//批次大小
		props.put("linger.ms", 1);//等待时间
		props.put("buffer.memory", 33554432);//RecordAccumulator 缓冲区大小
		props.put("key.serializer",
		"org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer",
		"org.apache.kafka.common.serialization.StringSerializer");
		
		Producer<String, String> producer = new KafkaProducer<>(props);
		for (int i = 0; i < 100; i++) {
			producer.send(new ProducerRecord<String, String>("test",
				"test" + Integer.toString(i)), new Callback() {
			
				//回调函数， 该方法会在 Producer 收到 ack 时调用，为异步调用
				@Override
				public void onCompletion(RecordMetadata metadata, Exception exception) {
					if (exception == null) {
						System.out.println(metadata.partition() + " - " + metadata.offset());
					} else {
						exception.printStackTrace();
					}
				}
			});
		}
		
		producer.close();
	}
}
```



### 自定义分区器

MyPartitioner.java

```java
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

public class MyPartitioner implements Partitioner {

	@Override
	public void configure(Map<String, ?> configs) {
		// TODO Auto-generated method stub

	}

	@Override
	public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
```

具体内容填写可参考默认分区器`org.apache.kafka.clients.producer.internals.DefaultPartitioner`

然后Producer配置中注册使用

```java
Properties props = new Properties();
...
props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, MyPartitioner.class);
...
Producer<String, String> producer = new KafkaProducer<>(props);
```



### 同步发送API

同步发送的意思就是，一条消息发送之后，会阻塞当前线程， 直至返回 ack。

由于 send 方法返回的是一个 Future 对象，根据 Futrue 对象的特点，我们也可以实现同步发送的效果，只需在调用 Future 对象的 get 方发即可。

SyncProducer.java

```java
Producer<String, String> producer = new KafkaProducer<>(props);
for (int i = 0; i < 100; i++) {
	producer.send(new ProducerRecord<String, String>("test",  "test - 1"), new Callback() {
		@Override
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			...
		}
	}).get();//<----------------------
}
```



### 拦截器

#### 拦截器原理

Producer 拦截器(interceptor)是在 Kafka 0.10 版本被引入的，主要用于实现 clients 端的定制化控制逻辑。

对于 producer 而言， interceptor 使得用户在消息发送前以及 producer 回调逻辑前有机会对消息做一些定制化需求，比如`修改消息`等。同时， producer 允许用户指定多个 interceptor按序作用于同一条消息从而形成一个拦截链(interceptor chain)。 Intercetpor 的实现接口是`org.apache.kafka.clients.producer.ProducerInterceptor`，其定义的方法包括：

- `configure(configs)`：获取配置信息和初始化数据时调用。
- `onSend(ProducerRecord)`：该方法封装进 KafkaProducer.send 方法中，即它运行在用户主线程中。 Producer 确保**在消息被序列化以及计算分区前**调用该方法。 用户可以在该方法中对消息做任何操作，但最好保证不要修改消息所属的 topic 和分区， 否则会影响目标分区的计算。
- `onAcknowledgement(RecordMetadata, Exception)`：**该方法会在消息从 RecordAccumulator 成功发送到 Kafka Broker 之后，或者在发送过程中失败时调用**。 并且通常都是在 producer 回调逻辑触发之前。 onAcknowledgement 运行在producer 的 IO 线程中，因此不要在该方法中放入很重的逻辑，否则会拖慢 producer 的消息发送效率。
- `close()`：关闭 interceptor，主要用于执行一些**资源清理**工作

如前所述， interceptor 可能被运行在多个线程中，因此在具体实现时用户需要自行确保线程安全。另外**倘若指定了多个 interceptor，则 producer 将按照指定顺序调用它们**，并仅仅是捕获每个 interceptor 可能抛出的异常记录到错误日志中而非在向上传递。这在使用过程中要特别留意。

#### 自定义拦截器（代码实现）

##### 需求

实现一个简单的双 interceptor 组成的拦截链。

- 第一个 interceptor 会在消息发送前将时间戳信息加到消息 value 的最前部；
- 第二个 interceptor 会在消息发送后更新成功发送消息数或失败发送消息数。

##### 案例实操

**增加时间戳拦截器**

TimeInterceptor.java

```java
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class TimeInterceptor implements ProducerInterceptor<String, String> {
	@Override
	public void configure(Map<String, ?> configs) {
	}

	@Override
	public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
		// 创建一个新的 record，把时间戳写入消息体的最前部
		return new ProducerRecord(record.topic(), record.partition(), record.timestamp(), record.key(),
				"TimeInterceptor: " + System.currentTimeMillis() + "," + record.value().toString());
	}

	@Override
	public void close() {
	}

	@Override
	public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
		// TODO Auto-generated method stub
		
	}
}
```

**增加计数器拦截器**

统计发送消息成功和发送失败消息数，并在 producer 关闭时打印这两个计数器

CounterInterceptor.java

```java
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class CounterInterceptor implements ProducerInterceptor<String, String>{

	private int errorCounter = 0;
	private int successCounter = 0;
	
	@Override
	public void configure(Map<String, ?> configs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
		return record;
	}

	@Override
	public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
		// 统计成功和失败的次数
		if (exception == null) {
			successCounter++;
		} else {
			errorCounter++;
		}
	}

	@Override
	public void close() {
		// 保存结果
		System.out.println("Successful sent: " + successCounter);
		System.out.println("Failed sent: " + errorCounter);
		
	}

}
```

**producer 主程序**

**InterceptorProducer.java**

```java
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

public class InterceptorProducer {
	public static void main(String[] args) {
		// 1 设置配置信息
		Properties props = new Properties();
		props.put("bootstrap.servers", "127.0.0.1:9092");
		props.put("acks", "all");
		props.put("retries", 3);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);
		props.put("key.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		//<--------------------------------------------
		// 2 构建拦截链
		List<String> interceptors = new ArrayList<>();
		interceptors.add("com.lun.kafka.interceptor.TimeInterceptor");
		interceptors.add("com.lun.kafka.interceptor.CounterInterceptor");
		
		props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, interceptors);
		
		String topic = "test";
		Producer<String, String> producer = new KafkaProducer<>(props);
		// 3 发送消息
		for (int i = 0; i < 10; i++) {
			ProducerRecord<String, String> record = new ProducerRecord<>(topic, "message" + i);
			producer.send(record);
		}
		
		// 4 一定要关闭 producer，这样才会调用 interceptor 的 close 方法
		producer.close();
		
	}
}
```

**运行结果**

![img](https://gitee.com/jallenkwong/LearnKafka/raw/master/image/21.png)





## 消费者

### 简单消费者API

- **KafkaConsumer**： 需要创建一个消费者对象，用来消费数据
- **ConsumerConfig**： 获取所需的一系列配置参数
- **ConsuemrRecord**： 每条数据都要封装成一个 ConsumerRecord 对象

**为了使我们能够专注于自己的业务逻辑， Kafka 提供了自动提交 offset 的功能**。

自动提交 offset 的相关参数：

- **enable.auto.commit**：是否开启自动提交 offset 功能
- **auto.commit.interval.ms**：自动提交 offset 的时间间隔

CustomConsumer.java

```
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class CustomConsumer {
	public static void main(String[] args) {
		
		Properties props = new Properties();
		
		props.put("bootstrap.servers", "127.0.0.1:9092");
		props.put("group.id", "abc");
		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer",
				"org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer",
				"org.apache.kafka.common.serialization.StringDeserializer");
		
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		
		consumer.subscribe(Arrays.asList("test"));
		
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(100);
			for (ConsumerRecord<String, String> record : records) {
				System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
			}
		}
	}
}
```



### 重置offset

Consumer 消费数据时的可靠性是很容易保证的，因为数据在 Kafka 中是持久化的，故不用担心数据丢失问题。

由于 consumer 在消费过程中可能会出现断电宕机等故障， consumer 恢复后，需要从故障前的位置的继续消费，所以 consumer 需要实时记录自己消费到了哪个 offset，以便故障恢复后继续消费。

**所以 offset 的维护是 Consumer 消费数据是必须考虑的问题**。

```java
public static final String AUTO_OFFSET_RESET_CONFIG = "auto.offset.reset";
Properties props = new Properties();
...
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
props.put("group.id", "abcd");//组id需另设，否则看不出上面一句的配置效果
...
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
```

从结果看，`props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");`与命令行中`bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test --from-beginning`的`--from-beginning`拥有相同的作用。



### 自动提交offset

```java
props.put("enable.auto.commit", "true");
```

PS.我将Offset提交类比成数据库事务的提交。



### 手动提交offset

虽然自动提交 offset 十分便利，但由于其是基于时间提交的， 开发人员难以把握offset 提交的时机。因此 **Kafka 还提供了手动提交 offset 的 API**。

手动提交 offset 的方法有两种：

1. commitSync（同步提交）
2. commitAsync（异步提交）

两者的**相同点**是，都会将本次 poll 的一批数据最高的偏移量提交；

**不同点**是，commitSync 阻塞当前线程，一直到提交成功，并且会自动失败重试（由不可控因素导致，也会出现提交失败）；而 commitAsync 则没有失败重试机制，故有可能提交失败。

#### 同步提交offset

由于同步提交 offset 有失败重试机制，故更加可靠，以下为同步提交 offset 的示例。

SyncCommitOffset.java

```java
public class SyncCommitOffset {
	public static void main(String[] args) {
		Properties props = new Properties();
		...
		//<-----------------
		//关闭自动提交 offset
		props.put("enable.auto.commit", "false");
		...
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Arrays.asList("first"));//消费者订阅主题
		while (true) {
			//消费者拉取数据
			ConsumerRecords<String, String> records =
			consumer.poll(100);
			for (ConsumerRecord<String, String> record : records) {
				System.out.printf("offset = %d, key = %s, value= %s%n", record.offset(), record.key(), record.value());
			}
			//<---------------------------------------
			//同步提交，当前线程会阻塞直到 offset 提交成功
			consumer.commitSync();
		}
	}
}
```

#### 异步提交offset

虽然同步提交 offset 更可靠一些，但是由于其会阻塞当前线程，直到提交成功。因此吞吐量会收到很大的影响。因此更多的情况下，会选用异步提交 offset 的方式。

AsyncCommitOffset.java

```java
public class AsyncCommitOffset {
	public static void main(String[] args) {
		Properties props = new Properties();
		...
		//<--------------------------------------
		//关闭自动提交
		props.put("enable.auto.commit", "false");
		...
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Arrays.asList("first"));// 消费者订阅主题
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(100);// 消费者拉取数据
			for (ConsumerRecord<String, String> record : records) {
				System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
			}
			//<----------------------------------------------
			// 异步提交
			consumer.commitAsync(new OffsetCommitCallback() {
				@Override
				public void onComplete(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) {
					if (exception != null) {
						System.err.println("Commit failed for" + offsets);
					}
				}
			});
		}
	}
}
```

#### 数据漏消费和重复消费分析

无论是同步提交还是异步提交 offset，都有可能会造成数据的漏消费或者重复消费。先提交 offset 后消费，有可能造成数据的漏消费；而先消费后提交 offset，有可能会造成数据的重复消费。

#### 自定义存储 offset

Kafka 0.9 版本之前， offset 存储在 zookeeper， 0.9 版本及之后，默认将 offset 存储在 Kafka的一个内置的 topic 中。除此之外， Kafka 还可以选择自定义存储 offset。

offset 的维护是相当繁琐的， 因为需要考虑到消费者的 Rebalace。

**当有新的消费者加入消费者组、 已有的消费者推出消费者组或者所订阅的主题的分区发生变化，就会触发到分区的重新分配，重新分配的过程叫做 Rebalance**。

消费者发生 Rebalance 之后，每个消费者消费的分区就会发生变化。**因此消费者要首先获取到自己被重新分配到的分区，并且定位到每个分区最近提交的 offset 位置继续消费**。

要实现自定义存储 offset，需要借助 `ConsumerRebalanceListener`， 以下为示例代码，其中提交和获取 offset 的方法，需要根据所选的 offset 存储系统自行实现。(可将offset存入MySQL数据库)

CustomSaveOffset.java

```java
public class CustomSaveOffset {
	private static Map<TopicPartition, Long> currentOffset = new HashMap<>();

	public static void main(String[] args) {
		// 创建配置信息
		Properties props = new Properties();
		...
		//<--------------------------------------
		// 关闭自动提交 offset
		props.put("enable.auto.commit", "false");
		...
		// 创建一个消费者
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		// 消费者订阅主题
		consumer.subscribe(Arrays.asList("first"), 
			//<-------------------------------------
			new ConsumerRebalanceListener() {
			// 该方法会在 Rebalance 之前调用
			@Override
			public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
				commitOffset(currentOffset);
			}

			// 该方法会在 Rebalance 之后调用
			@Override
			public void onPartitionsAssigned(Collection<TopicPartition> partitions) {

				currentOffset.clear();
				for (TopicPartition partition : partitions) {
					consumer.seek(partition, getOffset(partition));// 定位到最近提交的 offset 位置继续消费
				}
			}
		});
		
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(100);// 消费者拉取数据
			for (ConsumerRecord<String, String> record : records) {
				System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
				currentOffset.put(new TopicPartition(record.topic(), record.partition()), record.offset());
			}
			commitOffset(currentOffset);// 异步提交
		}
	}

	// 获取某分区的最新 offset
	private static long getOffset(TopicPartition partition) {
		return 0;
	}

	// 提交该消费者所有分区的 offset
	private static void commitOffset(Map<TopicPartition, Long> currentOffset) {
	}
}
```





## 原生
### Maven
```
dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.2.0</version>
</dependency>
```
### Producer
#### 配置项
|                 名称                  |                             说明                             |         默认值          |                有效值                |  重要性  |
| :-----------------------------------: | :----------------------------------------------------------: | :---------------------: | :----------------------------------: | :------: |
|           bootstrap.servers           |   kafka集群的broker-list，如：hadoop01:9092,hadoop02:9092    |           无            |                                      |   必选   |
|                 acks                  | 确保生产者可靠性设置，有三个选项：acks=0:不等待成功返回；acks=1:等Leader写成功返回；acks=all:等Leader和所有ISR中的Follower写成功返回,all也可以用-1代替 |           -1            |              0,1,-1,all              |          |
|            key.serializer             |                        key的序列化器                         |                         | ByteArraySerializer StringSerializer |   必选   |
|           value.serializer            |                       value的序列化器                        |                         | ByteArraySerializer StringSerializer |   必选   |
|             buffer.memory             |                     Producer总体内存大小                     |        33554432         |  不要超过物理内存，根据实际情况调整  | 建议必选 |
|           compression.type            | 压缩类型：压缩最好用于批量处理，批量处理消息越多，压缩性能越好 |           无            |          none、gzip、snappy          |          |
|                retries                |                     发送失败尝试重发次数                     |            0            |                                      |          |
|              batch.size               |                每个partition的未发送消息大小                 |          16384          |           根据实际情况调整           | 建议必选 |
|               client.id               |   附着在每个请求的后面，用于标识请求是从什么地方发送过来的   |                         |                                      |          |
|        connections.max.idle.ms        |           连接空闲时间超过过久自动关闭（单位毫秒）           |         540000          |                                      |          |
|               linger.ms               | 数据在缓冲区中保留的时长,0表示立即发送；为了减少网络耗时，需要设置这个值；太大可能容易导致缓冲区满，阻塞消费者；太小容易频繁请求服务端 |            0            |                                      |          |
|             max.block.ms              |                         最大阻塞时长                         |          60000          |                                      |          |
|           max.request.size            | 请求的最大字节数，该值要比batch.size大；不建议去更改这个值，如果设置不好会导致程序不报错，但消息又没有发送成功 |         1048576         |                                      |          |
|           partitioner.class           |        分区类，可以自定义分区类，实现partitioner接口         | 默认是哈希值%partitions |                                      |          |
|         receive.buffer.bytes          |          socket的接收缓存空间大小,当阅读数据时使用           |          32768          |                                      |          |
|          request.timeout.ms           |  等待请求响应的最大时间,超时则重发请求,超过重试次数将抛异常  |          3000           |                                      |          |
|           send.buffer.bytes           |                   发送数据时的缓存空间大小                   |         131072          |                                      |          |
|              timeout.ms               |         控制server等待来自followers的确认的最大时间          |          30000          |                                      |          |
| max.in.flight.requests.per.connection | kafka可以在一个connection中发送多个请求，叫作一个flight,这样可以减少开销，但是如果产生错误，可能会造成数据的发送顺序改变。 |            5            |                                      |          |
|       metadata.fetch.timeout.ms       |    从ZK中获取元数据超时时间<br>比如topic\host\partitions     |          60000          |                                      |          |
|          metadata.max.age.ms          | 即使没有任何partition leader 改变，强制更新metadata的时间间隔 |         300000          |                                      |          |
|           metric.reporters            | 类的列表，用于衡量指标。实现MetricReporter接口，将允许增加一些类，这些类在新的衡量指标产生时就会改变。JmxReporter总会包含用于注册JMX统计 |          none           |                                      |          |
|          metrics.num.samples          |                   用于维护metrics的样本数                    |            2            |                                      |          |
|       metrics.sample.window.ms        | metrics系统维护可配置的样本数量，在一个可修正的window size。这项配置配置了窗口大小，例如。我们可能在30s的期间维护两个样本。当一个窗口推出后，我们会擦除并重写最老的窗口 |          30000          |                                      |          |
|         reconnect.backoff.ms          | 连接失败时，当我们重新连接时的等待时间。这避免了客户端反复重连 |           10            |                                      |          |
|           retry.backoff.ms            | 在试图重试失败的produce请求之前的等待时间。避免陷入发送-失败的死循环中 |           100           |                                      |          |

#### Producer简单使用
```
Properties properties = new Properties();
properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"k8s-n1:9092");
properties.put(ProducerConfig.ACKS_CONFIG,"1");
properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
for (int i = 0; i < 100; i++)
	producer.send(new ProducerRecord<String, String>("mytest", Integer.toString(i), Integer.toString(i)));
producer.close();
```
#### 带回调函数的生产者
```
        Properties properties = new Properties();
        //设置kafka集群
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"k8s-n1:9092");
        //设置brokeACK应答机制
        properties.put(ProducerConfig.ACKS_CONFIG,"1");
        //设置key序列化
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        //设置value序列化
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
        //设置批量大小
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG,"6238");
        //设置提交延时
        properties.put(ProducerConfig.LINGER_MS_CONFIG,"1");
        //设置producer缓存
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG,Long.MAX_VALUE);

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        for ( int i = 0; i < 12; i++) {
            final int finalI = i;
            producer.send(new ProducerRecord<String, String>("mytest", Integer.toString(i), Integer.toString(i)), new Callback() {

                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if(exception==null){
                        System.out.println("发送成功: " + finalI +","+metadata.partition()+","+ metadata.offset());
                    }
                }
            });
        }
        producer.close();
```
结果：
```
发送成功: 0,0,170
发送成功: 2,0,171
发送成功: 11,0,172
发送成功: 4,1,101
发送成功: 5,2,116
发送成功: 6,2,117
发送成功: 10,2,118
发送成功: 1,3,175
发送成功: 3,3,176
发送成功: 7,3,177
发送成功: 8,3,178
发送成功: 9,3,179
```
数据不均等的分配到0-3 号分区上
#### 事务模式
事务模式要求数据发送必须包含在事务中，在事务中可以向多个topic发送数据，消费者端最好也使用事务模式读，保证一次能将整个事务的数据全部读取过来。当然消费者也可以不设置为事务读的模式。
```
@Test
public void transactional(){
	Properties props = new Properties();
	props.put("bootstrap.servers", "hadoop01:9092,hadoop02:9092,hadoop03:9092");
	props.put("transactional.id", "my_transactional_id");
	Producer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());

	producer.initTransactions();

	try {
		//数据发送必须在beginTransaction()和commitTransaction()中间，否则会报状态不对的异常
		producer.beginTransaction();
		for (int i = 0; i < 100; i++)
			producer.send(new ProducerRecord<>("mytopic1", Integer.toString(i), Integer.toString(i)));
		producer.commitTransaction();
	} catch (ProducerFencedException | OutOfOrderSequenceException | AuthorizationException e) {
		// 这些异常不能被恢复，因此必须要关闭并退出Producer
		producer.close();
	} catch (KafkaException e) {
		// 出现其它异常，终止事务
		producer.abortTransaction();
	}
	producer.close();
}
```
#### 自定义分区发送
```
public class CustomProducer implements Partitioner {
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        return 0;
    }

    public void close() {

    }

    public void configure(Map<String, ?> configs) {
    }
}
```
设置分区
```
Properties properties = new Properties();
//设置kafka集群
properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"k8s-n1:9092");
//设置brokeACK应答机制
properties.put(ProducerConfig.ACKS_CONFIG,"1");
//设置key序列化
properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
//设置value序列化
properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringSerializer");
//设置批量大小
properties.put(ProducerConfig.BATCH_SIZE_CONFIG,"6238");
//设置提交延时
properties.put(ProducerConfig.LINGER_MS_CONFIG,"1");
//设置producer缓存
properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG,Long.MAX_VALUE);
//设置partition
properties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,"com.sonly.kafka.CustomProducer");
KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
for ( int i = 0; i < 12; i++) {
	final int finalI = i;
	producer.send(new ProducerRecord<String, String>("mytest", Integer.toString(i), Integer.toString(i)), new Callback() {

		public void onCompletion(RecordMetadata metadata, Exception exception) {
			if(exception==null){
				System.out.println("发送成功: " + finalI +","+metadata.partition()+","+ metadata.offset());
			}
		}
	});
}
producer.close();
```

#### producer 拦截器(interceptor)
##### 拦截器原理
Producer 拦截器(interceptor)是在 Kafka 0.10 版本被引入的，主要用于实现 clients 端的定
制化控制逻辑。

对于 producer 而言，interceptor 使得用户在消息发送前以及 producer 回调逻辑前有机会
对消息做一些定制化需求，比如修改消息等。同时，producer 允许用户指定多个 interceptor
按序作用于同一条消息从而形成一个拦截链(interceptor chain)。Intercetpor 的实现接口是
org.apache.kafka.clients.producer.ProducerInterceptor，其定义的方法包括：

1. configure(configs)
获取配置信息和初始化数据时调用。
2. onSend(ProducerRecord)：
该方法封装进 KafkaProducer.send 方法中，即它运行在用户主线程中。Producer 确保在
消息被序列化以及计算分区前调用该方法。用户可以在该方法中对消息做任何操作，但最好
保证不要修改消息所属的 topic 和分区，否则会影响目标分区的计算
3. onAcknowledgement(RecordMetadata, Exception)：
该方法会在消息被应答或消息发送失败时调用，并且通常都是在 producer 回调逻辑触
发之前。onAcknowledgement 运行在 producer 的 IO 线程中，因此不要在该方法中放入很重
的逻辑，否则会拖慢 producer 的消息发送效率
4. close
关闭 interceptor，主要用于执行一些资源清理工作

如前所述，interceptor 可能被运行在多个线程中，因此在具体实现时用户需要自行确保
线程安全。另外倘若指定了多个 interceptor，则 producer 将按照指定顺序调用它们，并仅仅
是捕获每个 interceptor 可能抛出的异常记录到错误日志中而非在向上传递。这在使用过程中
要特别留意。

##### 拦截器案例
###### 需求
实现一个简单的双 interceptor 组成的拦截链。第一个 interceptor 会在消息发送前将时间
戳信息加到消息 value 的最前部；第二个 interceptor 会在消息发送后更新成功发送消息数或
失败发送消息数。

Kafka拦截器
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317225736.png)

##### 案例实操
```
package com.atlxl.producer;

import org.apache.kafka.clients.Metadata;
import org.apache.kafka.clients.producer.*;

import java.util.ArrayList;
import java.util.Properties;

public class CustomerProducer {

    public static void main(String[] args) {

        //配置信息
        Properties props = new Properties();
        // Kafka 服务端的主机名和端口号
        props.put("bootstrap.servers", "hadoop102:9092");
        // 等待所有副本节点的应答
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        // 消息发送最大尝试次数（应答级别 ）
        props.put("retries", 0);
        // 一批消息处理大小
        props.put("batch.size", 16384);
        // 请求延时
        props.put("linger.ms", 1);
        // 发送缓存区内存大小
        props.put("buffer.memory", 33554432);
        // key 序列化
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // value 序列化
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");


        ArrayList<String> list = new ArrayList<>();
        list.add("com.atlxl.intercetor.TimeIntercetor");
        list.add("com.atlxl.intercetor.CountIntercetor");

        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, list);

//        props.put("partitioner.class", "com.atlxl.producer.CustomerPartitioner");


        KafkaProducer<String, String> producer = new KafkaProducer(props);

        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<String, String>("second", String.valueOf(i)), new Callback() {
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if (e == null) {
                        System.out.println(recordMetadata.partition() + "--" + recordMetadata.offset());
                    }else {
                        System.out.println("发送失败！");
                    }
                }
            });
        }

        //关闭资源
        producer.close();


    }
}
```
```
package com.atlxl.intercetor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

public class TimeIntercetor implements ProducerInterceptor<String,String> {


    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        return new ProducerRecord<>(record.topic(),record.key(),System.currentTimeMillis() + "," + record.value());
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {

    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
```
```
package com.atlxl.intercetor;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;

public class CountIntercetor implements ProducerInterceptor<String,String> {

    private int successCount = 0;
    private int errorCount = 0;

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {

        if (exception==null) {
            successCount++;
        } else {
            errorCount++;
        }

    }

    @Override
    public void close() {
        System.out.println("发送成功：" + successCount + "条数据！");
        System.out.println("发送失败：" + errorCount + "条数据！");

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
```
```
[lxl@hadoop102 kafka]$ bin/kafka-console-consumer.sh --zookeeper hadoop102:2181 --topic second
```
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317225811.png)
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317225818.png)



### Consumer
kafka的消费者API提供从kafka服务端拉取消息的能力，kafka引入了消费者组的概念，不同消费者组之间互不影响，独自拥有一份数据，而同一个消费者组内的消费者则有如下规律：
- 分区数=消费者数：一个消费者拉取一个分区的数据
- 分区数>消费者数：同一个消费者可能拉取不同分区的数据
- 分区数<消费者数：一个消费者拉取一个分区的数据，多余的消费者不参与工作，当正在工作的消费者挂了之 后，这些闲着的消费者会顶替它干活，但会出现重复消费数据的情况


kafka Consumer提供两套Java API：高级Consumer API、和低级Consumer API。
高级 API 
- 优点：
    - 高级API写起来简单，易用。
    - 不需要自行去管理offset，API已经封装好了offset这块的东西，会通过zookeeper自行管理
    - 不需要管理分区，副本等情况，系统自动管理
    - 消费者断线后会自动根据上次记录在zookeeper中的offset接着消费消息。

- 缺点：
    - 不能自行控制offset。
    - 不能自行管理分区，副本，zk等相关信息。

低级API
- 优点：
    - 能够让开发者自己维护offset.想从哪里消费就从哪里消费
    - 自行控制连接分区，对分区自定义负载均衡
    - 对zookeeper的依赖性降低（如 offset 不一定要用zk来存储，可以存在缓存里或者内存中）

- 缺点：
    - 过于复杂，需要自行控制offset，连接哪个分区，找分区leader等。

#### 消费者高级API
```
Properties properties = new Properties();
//设置kafka集群
properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"k8s-n1:9092");
//设置brokeACK应答机制
properties.put(ConsumerConfig.GROUP_ID_CONFIG,"teste3432");
//设置key反序列化
properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
//设置value反序列化
properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.StringDeserializer");
//设置拿取大小
properties.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG,100*1024*1024);
//设置自动提交offset
properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,true);
//设置自动提交延时
properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,1000);
KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
consumer.subscribe(Arrays.asList("mytest","test"));
while (true){
	ConsumerRecords<String, String> records = consumer.poll(10);
	for (ConsumerRecord<String, String> record : records) {
		System.out.println(record.topic()+"--"+record.partition()+"--"+record.value());
	}
}
```
#### 消费者低级API
##### 消费者使用低级API的主要步骤
1. 根据指定分区从topic元数据中找到leader
2. 获取分区最新的消费进度
3. 从主副本中拉取分区消息
4. 识别主副本的变化，重试
##### 方法描述：
|      方法       |                         描述                         |
| :-------------: | :--------------------------------------------------: |
|  findLeader()   |  客户端向种子阶段发送主题元数据，将副本加入备用节点  |
| getLastOffset() |   消费者客户端发送偏移量请求，获取分区最近的偏移量   |
|      run()      |             消费者低级API拉取消息的方法              |
| findNewLeader() | 当分区主副本节点发生故障时，客户端将要找出新的主副本 |

##### 修改pom
```
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>1.1.1</version>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>1.1.1</version>
</dependency>
```
```
public class LowerConsumer {
    //保存offset
    private long offset;
    //保存分区副本
    private Map<Integer,List<BrokerEndPoint>> partitionsMap = new HashMap<Integer, List<BrokerEndPoint>>(1024);
    public static void main(String[] args) throws InterruptedException {
        List<String> brokers = Arrays.asList("k8s-n1", "k8s-n2","k8s-n3");
        int port = 9092;
        int partition = 1;
        long offset=2;
        LowerConsumer lowerConsumer = new LowerConsumer();
        while(true){
//            offset = lowerConsumer.getOffset();
            lowerConsumer.getData(brokers,port,"mytest",partition,offset);
            TimeUnit.SECONDS.sleep(1);
        }

    }

    public long getOffset() {
        return offset;
    }


    private BrokerEndPoint findLeader(Collection<String> brokers,int port,String topic,int partition){
        for (String broker : brokers) {
            //创建消费者对象操作每一台服务器
            SimpleConsumer getLeader = new SimpleConsumer(broker, port, 10000, 1024 * 24, "getLeader");
            //构造元数据请求
            TopicMetadataRequest topicMetadataRequest = new TopicMetadataRequest(Collections.singletonList(topic));
            //发送元数据请求
            TopicMetadataResponse response = getLeader.send(topicMetadataRequest);
            //解析元数据
            List<TopicMetadata> topicMetadatas = response.topicsMetadata();
            //遍历数据
            for (TopicMetadata topicMetadata : topicMetadatas) {
                //获取分区元数据信息
                List<PartitionMetadata> partitionMetadatas = topicMetadata.partitionsMetadata();
                //遍历分区元数据
                for (PartitionMetadata partitionMetadata : partitionMetadatas) {
                    if(partition == partitionMetadata.partitionId()){
                        //保存，分区对应的副本，如果需要主副本挂掉重新获取leader只需要遍历这个缓存即可
                        List<BrokerEndPoint> isr = partitionMetadata.isr();
                        this.partitionsMap.put(partition,isr);
                        return partitionMetadata.leader();
                    }
                }
            }
        }
        return null;
    }
    private void getData(Collection<String> brokers,int port,String topic,int partition,long offset){
        //获取leader
        BrokerEndPoint leader = findLeader(brokers, port, topic, partition);
        if(leader==null) return;
        String host = leader.host();
        //获取数据的消费者对象
        SimpleConsumer getData = new SimpleConsumer(host, port, 10000, 1024 * 10, "getData");
        //构造获取数据request 这里一次可以添加多个topic addFecth 添加即可
        FetchRequest fetchRequestBuilder = new FetchRequestBuilder().addFetch(topic, partition, offset, 1024 * 10).build();
        //发送获取数据请求
        FetchResponse fetchResponse = getData.fetch(fetchRequestBuilder);
        //解析元数据返回，这是message的一个set集合
        ByteBufferMessageSet messageAndOffsets = fetchResponse.messageSet(topic, partition);
        //遍历消息集合
        for (MessageAndOffset messageAndOffset : messageAndOffsets) {
            long offset1 = messageAndOffset.offset();
            this.setOffset(offset);
            ByteBuffer payload = messageAndOffset.message().payload();
            byte[] buffer = new byte[payload.limit()];
            payload.get(buffer);
            String message = new String(buffer);
            System.out.println("offset:"+ offset1 +"--message:"+ message);

        }
    }

    private void setOffset(long offset) {
        this.offset = offset;
    }
}
```
这个低级API在最新的kafka版本中已经不再提供了。

### 管理 topic
```
package com.example.demo.topic;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.server.ConfigType;
import kafka.utils.ZkUtils;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.security.JaasUtils;
import scala.collection.JavaConversions;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KafkaTopic {
    public static void main(String[] args) {
        //createTopic();
        //deleteTopic();
        //listAllTopic();
        // getTopic();
        listTopicAllConfig();
    }

    /**
    * 创建主题
    * kafka-topics.sh --zookeeper localhost:2181 --create --topic kafka-action --replication-factor 2 --partitions 3
    */
    private static void createTopic() {
        ZkUtils zkUtils = ZkUtils.apply("47.52.199.51:2181", 30000, 30000, JaasUtils.isZkSecurityEnabled());
        // 创建一个单分区单副本名为t1的topic
        AdminUtils.createTopic(zkUtils, "topic-20", 3, 1, new Properties(), RackAwareMode.Enforced$.MODULE$);
        zkUtils.close();
    }

    /**
     * 除某主题
     * kafka-topics.sh --zookeeper localhost:2181 --topic kafka-action --delete
     */
    private static void deleteTopic() {
        ZkUtils zkUtils = ZkUtils.apply("47.52.199.51:2181", 30000, 30000, JaasUtils.isZkSecurityEnabled());
        // 删除topic 't1'
        AdminUtils.deleteTopic(zkUtils, "topic-19");
        zkUtils.close();
    }

    /**
     * 修改主题配置     kafka-config --zookeeper localhost:2181 --entity-type topics --entity-name kafka-action     
     *   --alter --add-config max.message.bytes=202480 --alter --delete-config flush.messages
     */
    private static void updateTopic() {
        ZkUtils zkUtils = ZkUtils.apply("47.52.199.51:2181", 30000, 30000, JaasUtils.isZkSecurityEnabled());
        Properties props = AdminUtils.fetchEntityConfig(zkUtils, ConfigType.Topic(), "topic-19");
        // 增加topic级别属性
        props.put("min.cleanable.dirty.ratio", "0.3");
        // 删除topic级别属性
        props.remove("max.message.bytes");
        // 修改topic 'test'的属性
        AdminUtils.changeTopicConfig(zkUtils, "test", props);
        zkUtils.close();
    }

    /**
     *
     * 查看所有主题    kafka-topics.sh --zookeeper localhost:2181 --list
     */
    public static void listAllTopic() {
        ZkUtils zkUtils = null;
        try {
            zkUtils = ZkUtils.apply("47.52.199.51:2181", 30000, 30000, JaasUtils.isZkSecurityEnabled());
            List<String> topics = JavaConversions.seqAsJavaList(zkUtils.getAllTopics());
            topics.forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zkUtils != null) {
                zkUtils.close();
            }
        }
    }

    /**
     * 得到所有topic的配置信息     kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --describe
     */
    public static void listTopicAllConfig() {
        ZkUtils zkUtils = null;
        try {
            zkUtils = ZkUtils.apply("47.52.199.51:2181", 30000, 30000, JaasUtils.isZkSecurityEnabled());
            Map<String, Properties> configs = JavaConversions.mapAsJavaMap(AdminUtils.fetchAllTopicConfigs(zkUtils));
            // 获取特定topic的元数据
            MetadataResponse.TopicMetadata topicMetadata = AdminUtils.fetchTopicMetadataFromZk("topic-19",zkUtils);
            // 获取特定topic的配置信息
            Properties properties = AdminUtils.fetchEntityConfig(zkUtils,"topics","kafka-test");
            for (Map.Entry<String, Properties> entry : configs.entrySet()) {
                System.out.println("key=" + entry.getKey() + " ;value= " + entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (zkUtils != null) {
                zkUtils.close();
            }
        }
    }
}
```




### kafka通过java api 获取当前消费组offset/logsize/lag信息，实现消费延迟监控
注意此篇是针对使用旧版消费的方案， 旧版（0.8之前）offset信息存在zk，新版（0.9以后）存在topic中。如果不知道自己是什么版本的

一般监控kafka消费情况我们可以使用现成的工具来查看，但如果发生大量延迟不能及时知道。所以问题就来了，怎么用java api 进行kafka的监控呢？

用过kafka都该知道 延迟量 lag = logSize(topic记录量) - offset(消费组消费进度) 

所以我们获取到logSize / offset 就可以了。
#### pom
```
<groupId>org.apache.kafka</groupId>
<artifactId>kafka_2.11</artifactId>
<version>0.10.1.1</version>
```
```
package com.fengjr.elk.web.write;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.ErrorMapping;
import kafka.common.OffsetMetadataAndError;
import kafka.common.TopicAndPartition;
import kafka.javaapi.*;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.network.BlockingChannel;

public class KafkaOffsetTools {

    public static void main(String[] args) {

        String topic = "app-log-all-beta";
        String broker = "10.255.73.160";
        int port = 9092;
        String group = "fengjr-elk-group-es";
        String clientId = "Client_app-log-all-beta_1";
        int correlationId = 0;
        BlockingChannel channel = new BlockingChannel(broker, port,
                BlockingChannel.UseDefaultBufferSize(),
                BlockingChannel.UseDefaultBufferSize(),
                5000 );
        channel.connect();

        List<String> seeds = new ArrayList<String>();
        seeds.add(broker);
        KafkaOffsetTools kot = new KafkaOffsetTools();

        TreeMap<Integer,PartitionMetadata> metadatas = kot.findLeader(seeds, port, topic);

        long sum = 0l;
        long sumOffset = 0l;
        long lag = 0l;
        List<TopicAndPartition> partitions = new ArrayList<TopicAndPartition>();
        for (Entry<Integer,PartitionMetadata> entry : metadatas.entrySet()) {
            int partition = entry.getKey();
            TopicAndPartition testPartition = new TopicAndPartition(topic, partition);
            partitions.add(testPartition);
        }
        OffsetFetchRequest fetchRequest = new OffsetFetchRequest(
                group,
                partitions,
                (short) 0,
                correlationId,
                clientId);
        for (Entry<Integer,PartitionMetadata> entry : metadatas.entrySet()) {
            int partition = entry.getKey();
            try {
                channel.send(fetchRequest.underlying());
                OffsetFetchResponse fetchResponse = OffsetFetchResponse.readFrom(channel.receive().payload());
                TopicAndPartition testPartition0 = new TopicAndPartition(topic, partition);
                OffsetMetadataAndError result = fetchResponse.offsets().get(testPartition0);
                short offsetFetchErrorCode = result.error();
                if (offsetFetchErrorCode == ErrorMapping.NotCoordinatorForConsumerCode()) {
                } else {
                    long retrievedOffset = result.offset();
                    sumOffset += retrievedOffset;
                }
                String leadBroker = entry.getValue().leader().host();
                String clientName = "Client_" + topic + "_" + partition;
                SimpleConsumer consumer = new SimpleConsumer(leadBroker, port, 100000,
                        64 * 1024, clientName);
                long readOffset = getLastOffset(consumer, topic, partition,
                        kafka.api.OffsetRequest.LatestTime(), clientName);
                sum += readOffset;
                System.out.println(partition+":"+readOffset);
                if(consumer!=null)consumer.close();
            } catch (Exception e) {
                channel.disconnect();
            }
        }

        System.out.println("logSize："+sum);
        System.out.println("offset："+sumOffset);

        lag = sum - sumOffset;
        System.out.println("lag:"+ lag);


    }

    public KafkaOffsetTools() {
    }


    public static long getLastOffset(SimpleConsumer consumer, String topic,
                                     int partition, long whichTime, String clientName) {
        TopicAndPartition topicAndPartition = new TopicAndPartition(topic,
                partition);
        Map<TopicAndPartition, PartitionOffsetRequestInfo> requestInfo = new HashMap<TopicAndPartition, PartitionOffsetRequestInfo>();
        requestInfo.put(topicAndPartition, new PartitionOffsetRequestInfo(
                whichTime, 1));
        kafka.javaapi.OffsetRequest request = new kafka.javaapi.OffsetRequest(
                requestInfo, kafka.api.OffsetRequest.CurrentVersion(),
                clientName);
        OffsetResponse response = consumer.getOffsetsBefore(request);
        if (response.hasError()) {
            System.out
                    .println("Error fetching data Offset Data the Broker. Reason: "
                            + response.errorCode(topic, partition));
            return 0;
        }
        long[] offsets = response.offsets(topic, partition);
        return offsets[0];
    }

    private TreeMap<Integer,PartitionMetadata> findLeader(List<String> a_seedBrokers,
                                                          int a_port, String a_topic) {
        TreeMap<Integer, PartitionMetadata> map = new TreeMap<Integer, PartitionMetadata>();
        loop: for (String seed : a_seedBrokers) {
            SimpleConsumer consumer = null;
            try {
                consumer = new SimpleConsumer(seed, a_port, 100000, 64 * 1024,
                        "leaderLookup"+new Date().getTime());
                List<String> topics = Collections.singletonList(a_topic);
                TopicMetadataRequest req = new TopicMetadataRequest(topics);
                kafka.javaapi.TopicMetadataResponse resp = consumer.send(req);

                List<TopicMetadata> metaData = resp.topicsMetadata();
                for (TopicMetadata item : metaData) {
                    for (PartitionMetadata part : item.partitionsMetadata()) {
                        map.put(part.partitionId(), part);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error communicating with Broker [" + seed
                        + "] to find Leader for [" + a_topic + ", ] Reason: " + e);
            } finally {
                if (consumer != null)
                    consumer.close();
            }
        }
        return map;
    }
}
```

## Spring boot 集成
[官方文档](https://docs.spring.io/spring-kafka/docs/2.2.0.RELEASE/reference/html/_reference.html)

### 手动提交
```
@Bean
public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> kafkaListenerContainerFactory(ConsumerFactory<String, String> consumerFactory) {
   ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
   factory.setConsumerFactory(consumerFactory);
   factory.getContainerProperties().setPollTimeout(1500);
   //配置手动提交offset
   factory.getContainerProperties().setAckMode((ContainerProperties.AckMode.MANUAL));
   return factory;
}
```
```
public class KonkaKafkaListener {
 
    private final static Logger LOGGER = LoggerFactory.getLogger(KonkaKafkaListener.class);
 
 
    @Autowired
    private RouterService routerService;
 
 
    @KafkaListener(containerFactory = "kafkaListenerContainerFactory", topics = "test")
    public void consumerListener(List<ConsumerRecord> consumerRecords, Acknowledgment ack) {
        ack.acknowledge();//直接提交offset
        if (consumerRecords.size() > 0) {
            PartitionCounter.addCounter(consumerRecords.get(0).partition(), consumerRecords.size());
        }
        Iterator<ConsumerRecord> iterator = consumerRecords.iterator();
        while (iterator.hasNext()) {
            ConsumerRecord consumerRecord = iterator.next();
            String key = consumerRecord.key().toString();
            KafkaLogMessage kafkaLogMessage = (KafkaLogMessage) consumerRecord.value();
            if (kafkaLogMessage == null) {
                continue;
            }
            routerService.handleKafkaMessage(key, kafkaLogMessage);
        }
    }
```