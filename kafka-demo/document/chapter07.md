[toc]



# Kafka 常用命令

## 1.Topic管理
### 1.1.创建topic
```
kafka-topics.sh --zookeeper 47.52.199.52:2181 --create --topic test-15 --replication-factor 1 --partitions 3
```
### 1.2.新增partition
```
kafka-topics.sh --zookeeper zk.server --alter --topic test --replication-factor 1 --partitions 3
```
注：topic一旦创建，partition只能增加，不能减少
### 1.3.删除topic
```
kafka-topics.sh --zookeeper zk.server --delete --topic test 
```
- 删除kafka存储目录（`server.properties` 文件 `log.dirs` 配置，默认为 `/tmp/kafka-logs`）相关topic目录

- 如果配置了 `delete.topic.enable=true` 直接通过命令删除，如果命令删除不掉，直接通过 `zookeeper-client` 删除掉 `/brokers/topics/`目录下相关topic节点。

  注意: 如果你要删除一个topic并且重建，那么必须重新启动kafka，否则新建的topic在zookeeper的 `/brokers/topics/test-topic/` 目录下没有partitions这个目录，也就是没有分区信息。
### 1.4.查看topic列表
```
kafka-topics.sh --zookeeper zk.server --list
```
### 1.5.查看topic详细信息
```
kafka-topics.sh --zookeeper zk.server --topic test --describe
```
### 1.6.查看某个topic的message数量
```
./bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list 47.52.199.51:9092 --topic consumer-send
```
## 2.consumer管理
### 2.1.查看consumer Group列表
```
./bin/kafka-consumer-groups.sh  --list  --bootstrap-server 192.168.88.108:9092
```
### 2.2.查看指定group.id的消费情况
```
./bin/kafka-consumer-groups.sh --bootstrap-server 47.52.199.51:9092 --group test-1 --describe
```
结果：

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317230203.png)

`CURRENT-OFFSET`：当前消费偏移量

`LOG-END-OFFSET`：末尾偏移量

### 2.3.删除group
```
./bin/kafka-consumer-groups.sh --bootstrap-server 47.52.199.51:9092 --group test-1 --delete
```
### 2.4.重置offset
1. 要求修改的group不能active,查看是否active

    ```
    [root@izj6c46svwddzpu0evy0vbz kafka_2.11-2.0.1]# ./bin/kafka-consumer-groups.sh --bootstrap-server 47.52.199.51:9092 --group test_4 --describe
    OpenJDK 64-Bit Server VM warning: If the number of processors is expected to increase from one, then you should configure the number of parallel GC threads appropriately using -XX:ParallelGCThreads=N
    Consumer group 'test_4' has no active members.

    TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
    consumer-send   0          5697            5697            0               -               -               -
    producer-syn    0          4125            4125            0               -               -               -
    ```

2. 重置命令

    ```
    ./bin/kafka-consumer-groups.sh --bootstrap-server 47.52.199.51:9092 --group test_4 --reset-offsets -to-offset 100 --topic consumer-send --execute
    ```

3. 导出offset

    ```
    ./bin/kafka-consumer-groups.sh --bootstrap-server 47.52.199.51:9092 --group test_4 --reset-offsets -to-offset 100 --topic consumer-send --export > 1.txt
    ```

## 3.动态配置
### 3.1.再平衡
```
./bin/kafka-preferred-replica-election.sh --zookeeper 47.52.199.51:2181/chroot
```
## 4.生产消费者
### 4.1.启动kafka
```
nohup bin/kafka-server-start.sh config/server.properties > /dev/null 2>&1 &
```
### 4.2.创建消费者
```
bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic middleware --from-beginning
```
### 4.3.创建生产者
```
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
```

