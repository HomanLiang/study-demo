[toc]



# ElasticSearch 集群的规划部署与运维

## 1.常见的集群部署方式

ES 有以下不同类型的节点：

- **Master(eligible)节点**：只有 `Master eligible` 节点可以成为 `Master` 节点。
  - `Master` 节点用于维护索引信息和集群状态。
- **Data** 节点：负责数据存储。
- **Ingest** 节点：数据预处理。
- **Coordinating** 节点：处理用户请求。
- **ML** 节点：机器学习相关功能。

在开发环境中，一个节点可以承担多种角色。

但是在生产环境，建议一个节点只负责单一角色，以达到高可用性及高性能。同时根据业务需求和硬件资源来合理分配节点。

### 1.1.节点配置参数

在默认情况下，一个节点会同时扮演 `Master eligible Node`，`Data Node` 和 `Ingest Node`。

各类型的节点配置参数如下：

| 节点类型        | 配置参数    | 默认值                     |
| --------------- | ----------- | -------------------------- |
| Master eligible | node.master | true                       |
| Data Node       | node.data   | true                       |
| Ingest Node     | node.ingest | true                       |
| Coordinating    | 无          | -                          |
| ML              | node.ml     | true（需要 enable x-pack） |

默认情况下，每个节点都是一个 `Coordinating` 节点，可以将 `node.master`，`node.data` 和 `node.ingest` 同时设置为 `false`，让一个节点**只负责** `Coordinating` 节点的角色。

### 1.2.配置单一角色

默认情况下，一个节点会承担多个角色，可以通过配置让一个节点只负责单一角色。

单一职责节点配置：

- `Master` 节点：从高可用和避免脑裂的角度考虑，生产环境可配置 3 个 `Master` 节点。
  - `node.master`：`true`
  - `node.ingest`：`false`
  - `node.data`：`false`
- `Data` 节点
  - `node.master`：`false`
  - `node.ingest`：`false`
  - `node.data`：`true`
- `Ingest` 节点
  - `node.master`：`false`
  - `node.ingest`：`true`
  - `node.data`：`false`
- `Coordinating` 节点
  - `node.master`：`false`
  - `node.ingest`：`false`
  - `node.data`：`false`

### 1.3.水平扩展架构

集群的水平扩展：

- 当需要更多的磁盘容量和读写能力时，可以增加 `Data Node`；
- 当系统有大量的复杂查询和聚合分析时，可以增加 `Coordinating Node`。

![image-20210306172946095](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306172946095.png)

### 1.4.读写分离架构

使用 **Ingest** 节点对数据预处理。

![image-20210306173004961](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306173004961.png)



## 2.分片设计与管理

ES 中的文档存储在索引中，索引的最小存储单位是分片，不同的索引存储在不同的分片中。

> 当讨论分片时，一般是**基于某个索引**的，不同索引之间的分片互不干扰。

分片分为**主分片**和**副本分片**两种；副本分片是主分片的拷贝，主要用于备份数据。

关于[主副分片数的设置](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/index-modules.html)：

- 主分片数：主分片数在索引创建时确定，之后不能修改。
  - 在 `ES 7.0` 以后，一个索引**默认**有一个主分片。
  - 一个索引的主分片数不能超过 **1024**。
- 副本分片数：副本分片数在索引创建之后可以动态修改。
  - 副本分片数默认为 1。

关于每个节点上的分片数的设置，可参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/allocation-total-shards.html)。

### 2.1.主分片的设计

如果某个索引只有**一个主分片**：

- 优点：查询算分和聚合不精准的问题都可避免。
- 缺点：集群无法实现水平扩展。
  - 因为索引（不管该索引的数据量达到了多大）只能存储在一个主分片上（一个分片不能跨节点存储/处理）；
  - 对于单个主分片的索引来说，即使有再多的数据节点，它也无法利用。

如果某个索引有**多个主分片**：

- 优点：集群可以实现水平扩展。
  - 对于拥有多个主分片的索引，该索引的数据可以分布在多个主分片上，不同的主分片可以分布在不同的数据节点中；这样，该索引就可以利用多个节点的读写能力，从而处理更多的数据。
  - 如果当前的**数据节点数**小于**主分片数**，当有新的数据节点加入集群后，这些主分片就会自动被分配到新的数据节点上，从而实现水平扩容。
- 缺点：但是主分片数也不能过多，因为对于分片的管理也需要额外的资源开销。主要会带来以下问题：
  - 每次搜索/聚合数据时需要从多个分片上获取数据，并汇总；除了会带来精准度问题，还会有性能问题。
  - 分片的 Meta 信息由 Master 节点维护管理，过多的分片，会增加 Master 节点的负担。

对于**分片的设计建议**：

- 从分片的存储量考虑：
  - 对于日志类应用，单个分片不要大于 `50G`；
  - 对于搜索类应用，单个分片不要大于 `20G`；
- 从分片数量考虑：
  - **一个 ES 集群的分片**（包括主分片和副本分片）**总数不超过 10 W**。

### 2.2.副本分片的设计

副本分片是主分片的备份：

- 优点：
  - 可防止数据丢失，提高系统的可用性；
  - 可以分担主分片的查询压力，提高系统的**查询**性能。
- 缺点：
  - 与主分片一样，需要占用系统资源，有多少个副本，就会增加多少倍的存储消耗。
  - 会降低系统的写入速度。



## 3.集群容量规划

容量规划指的是，在一个实际项目中：

- 一个集群需要多少节点，以及节点类型分配。
- 一个索引需要几个主分片，几个副本分片。

### 3.1.要考虑的因素

做容量规划时要考虑的因素：

- 机器的软硬件配置
- 数据量：
  - 单条文档的尺寸
  - 文档的总数量
  - 索引的总数量
- 业务需求：
  - 文档的复杂度、数据格式
  - 写入需求
  - 查询需求
  - 聚合需求

### 3.2.硬件配置

对系统整体性能要求高的，建议使用 `SSD`，内存与硬盘的比例可为 `1：10`。

对系统整体性能要求一般的，可使用机械硬盘，内存与硬盘的比例可为 `1：50`。

`JVM` 配置为机器内存的一半，建议 `JVM` 内存配置不超过 `32 G`。

单个节点的数据建议控制在 `2TB` 以内，最大不超过 `5 TB`。

### 3.3.常见应用场景

有如下常见应用场景：

- 搜索类应用：
  - 总体数据集大小基本固定，数据量增长较慢。
- 日志类应用：
  - 每日新增数据量比较稳定，数据量持续增长，可预期。

**3.3.1.处理时间序列数据**

ES 中提供了 [Date Math 索引名](https://www.elastic.co/guide/en/elasticsearch/reference/current/date-math-index-names.html)用于写入时间序列的数据。

示例：

![image-20210306173130572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306173130572.png)

请求 URI 要经过 URL 编码：

```shell
# PUT /<my-index-{now/d}>
# 经过 URL 编码后
PUT /%3Cmy-index-%7Bnow%2Fd%7D%3E
```

查询示例：

```shell
# POST /<logs-{now/d}/_search
POST /%3Clogs-%7Bnow%2Fd%7D%3E/_search

# POST /<logs-{now/w}/_search
POST /%3Clogs-%7Bnow%2Fw%7D%3E/_search
```



## 4.ES 开发模式与生产模式

从 `ES 5` 开始，`ES` 支持开发模式与生产模式，`ES` 可通过配置自动选择不同的模式去运行：

- 开发模式配置：
  - `http.host：localhost`
  - `transport.bind_host：localhost`
- 生产模式配置：
  - `http.host：真实 IP 地址`
  - `transport.bind_host：真实 IP 地址`

### 4.1.Booststrap 检测

在生产模式启动 `ES` 集群时，会进行 [Booststrap 检测](https://www.elastic.co/guide/en/elasticsearch/reference/current/bootstrap-checks.html)（只有检测通过才能启动成功），它包括：

- `JVM` 检测
- `Linux` 检测：只在 `Linux` 环境进行

### 4.2.JVM 配置

JVM 通过 `config` 目录下的 [jvm.options](https://www.elastic.co/guide/en/elasticsearch/reference/current/jvm-options.html) 文件进行配置，需要注意以下几点：

- 将 `Xms` 和 `Xmx` 设置成一样；
- `Xmx` 不要超过物理内存的 `50%`，最大内存建议不超过 `32G`；
- `JVM` 有 `Server` 和 `Client` 两种模式，在 `ES` 的生产模式必须使用 `Server` 模式；
- 需要关闭 `JVM Swapping`

### 4.3.更多的 ES 配置

更多的关于 **ES 的配置**可参考其官方文档，包括：

- [Configuring Elasticsearch](https://www.elastic.co/guide/en/elasticsearch/reference/current/settings.html)
- [Important Elasticsearch configuration](https://www.elastic.co/guide/en/elasticsearch/reference/current/important-settings.html)
- [Important System Configuration](https://www.elastic.co/guide/en/elasticsearch/reference/current/system-config.html)



## 5.监控集群状态

集群状态为 **Green** 只能代表分片正常分配，不能代表没有其它问题。

`ES` 提供了很多监控相关的 `API`：

- [_cluster/health](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html)：集群健康状态。
- [_cluster/state](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-state.html)：集群状态。
- [_cluster/stats](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-stats.html)：集群指标统计。
- [_cluster/pending_tasks](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-pending.html)：集群中正在执行的任务。
- [_tasks](https://www.elastic.co/guide/en/elasticsearch/reference/current/tasks.html)：集群任务。
- [_cluster/allocation/explain](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-allocation-explain.html)：查看集群分片的分配情况，用于查找原因。
- [_nodes/stats](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-nodes-stats.html)：节点指标统计。
- [_nodes/info](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-nodes-info.html)：节点信息。
- [_index/stats](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html)：索引指标统计。
- 一些 [cat](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat.html) API。

### 5.1.Slow log

ES 的 [Slow log](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules-slowlog.html) 可以设置一些阈值，当写入时间或者查询时间超过这些阈值后，会将相关操作记录日志。

### 5.2.集群诊断

需要监控的指标：

![20210128102700148](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210128102700148.png)

一个集群诊断工具 [Support Diagnostics](https://github.com/elastic/support-diagnostics)。









## 6.Elasticsearch 运维实战常用命令

### 6.1.集群状态非绿排查清单

#### 6.1.1.集群状态的含义

- 红色：至少一个主分片未分配成功；
- 黄色：至少一个副本分片未分配成功；
- 绿色：全部主&副本都分配成功。

#### 6.1.2.排查实战

**6.1.2.1.查看集群状态**

```
GET _cluster/health
```

返回状态举例："status" : "red", 红色，至少一个主分片未分配成功。

**6.1.2.2.到底哪个节点出现了红色或者黄色问题呢？**

```
GET _cluster/health?level=indices
```

如下的方式，更明快直接

```
GET /_cat/indices?v&health=yellow
GET /_cat/indices?v&health=red
```

找到对应的索引。

**6.1.2.3.到底索引的哪个分片出现了红色或者黄色问题呢？**

```
GET _cluster/health?level=shards
```

**6.1.2.4.到底什么原因导致了集群变成红色或者黄色呢？**

```
GET _cluster/allocation/explain
```

返回核心信息解读举例：

```
	"current_state" : "unassigned",——未分配
  	"unassigned_info" : {
    "reason" : "INDEX_CREATED",——原因，索引创建阶段
    "at" : "2020-01-29T07:32:39.041Z",
    "last_allocation_status" : "no"
  },
  "explanation" : """node does not match index setting [index.routing.allocation.require] filters [box_type:"hot"]"""
}
```

根本原因，shard分片与节点过滤类型不一致 到此，找到了根本原因，也就知道了对应解决方案。

#### 6.1.3.扩展思考：类似 "current_state" : "unassigned",——未分配 还有哪些？

实战：

GET _cat/shards?h=index,shard,prirep,state,unassigned.reason

官网：https://www.elastic.co/guide/en/elasticsearch/reference/7.2/cat-shards.html

未分配状态及原因解读：

- `INDEX_CREATED`：Unassigned as a result of an API creation of an index.
- `CLUSTER_RECOVERED`：Unassigned as a result of a full cluster recovery.
- `INDEX_REOPENED`：Unassigned as a result of opening a closed index.
- `DANGLING_INDEX_IMPORTED`：Unassigned as a result of importing a dangling index.
- `NEW_INDEX_RESTORED`：Unassigned as a result of restoring into a new index.
- `EXISTING_INDEX_RESTORED`：Unassigned as a result of restoring into a closed index.
- `REPLICA_ADDED：Unassigned as a result of explicit addition of a replica.
- `ALLOCATION_FAILED`：Unassigned as a result of a failed allocation of the shard.
- `NODE_LEFT`：Unassigned as a result of the node hosting it leaving the cluster.
- `REROUTE_CANCELLED`：Unassigned as a result of explicit cancel reroute command.
- `REINITIALIZED`：When a shard moves from started back to initializing, for example, with shadow replicas.
- `REALLOCATED_REPLICA`：A better replica location is identified and causes the existing replica allocation to be cancelled.



### 6.2.节点间分片移动

适用场景：手动移动分配分片。将启动的分片从一个节点移动到另一节点。

```
POST /_cluster/reroute
{
  "commands": [
    {
      "move": {
        "index": "indexname",
        "shard": 1,
        "from_node": "nodename",
        "to_node": "nodename"
      }
    }
  ]
}
```



### 6.3.集群节点优雅下线

适用场景：保证集群颜色绿色的前提下，将某个节点优雅下线。

```
PUT /_cluster/settings
{
  "transient": {
    "cluster.routing.allocation.exclude._ip": "122.5.3.55"
  }
}
```



### 6.4.强制刷新

适用场景：刷新索引是确保当前仅存储在事务日志中的所有数据也永久存储在 `Lucene` 索引中。

```
POST /_flush
```

注意：这和 7.6 版本之前的同步刷新（未来8版本+会废弃同步刷新）一致。

```
POST /_flush/synced
```



### 6.5.更改并发分片的数量以平衡集群

适用场景：

控制在集群范围内允许多少并发分片重新平衡。默认值为2。

```
PUT /_cluster/settings
{
  "transient": {
    "cluster.routing.allocation.cluster_concurrent_rebalance": 2
  }
}
```



### 6.6.更改每个节点同时恢复的分片数量

适用场景：

如果节点已从集群断开连接，则其所有分片将都变为未分配状态。经过一定的延迟后，分片将分配到其他位置。每个节点要恢复的并发分片数由该设置确定。

```
PUT /_cluster/settings
{
  "transient": {
    "cluster.routing.allocation.node_concurrent_recoveries": 6
  }
}
```



### 6.7.调整恢复速度

适用场景：

为了避免集群过载，`Elasticsearch` 限制了分配给恢复的速度。你可以仔细更改该设置，以使其恢复更快。

如果此值调的太高，则正在进行的恢复可能会消耗过多的带宽和其他资源，这可能会使集群不稳定。

```
PUT /_cluster/settings
{
  "transient": {
    "indices.recovery.max_bytes_per_sec": "80mb"
  }
}
```



### 6.8.清除节点上的缓存

适用场景：如果节点达到较高的 `JVM` 值，则可以在节点级别上调用该 `API` 以使 `Elasticsearch` 清理缓存。

这会降低性能，但可以使你摆脱 `OOM`（内存不足）的困扰。

```
POST /_cache/clear
```



### 6.9.调整断路器

适用场景：为了避免在 `Elasticsearch` 中进入 `OOM`，可以调整断路器上的设置。这将限制搜索内存，并丢弃所有估计消耗比所需级别更多的内存的搜索。

注意：这是一个非常精密的设置，你需要仔细校准。

```
PUT /_cluster/settings
{
  "persistent": {
    "indices.breaker.total.limit": "40%"
  }
}
```



### 16.10.集群迁移

适用场景：集群数据迁移、索引数据迁移等。

**方案一、 针对索引部分或者全部数据，reindex**

POST _reindex
{
  "source": {
    "index": "my-index-000001"
  },
  "dest": {
    "index": "my-new-index-000001"
  }
}
**方案二：借助第三方工具迁移索引或者集群**

- `elasticdump`

- `elasticsearch-migration`

工具本质：`scroll + bulk` 实现。



### 6.11.集群数据备份和恢复

适用场景：高可用业务场景，定期增量、全量数据备份，以备应急不时之需。

```
PUT /_snapshot/my_backup/snapshot_hamlet_index?wait_for_completion=true
{
  "indices": "hamlet_*",
  "ignore_unavailable": true,
  "include_global_state": false,
  "metadata": {
    "taken_by": "mingyi",
    "taken_because": "backup before upgrading"
  }
}

POST /_snapshot/my_backup/snapshot_hamlet_index/_restore
```



### 6.12.小结

文章开头的几个运维问题已经解决，其他性能相关的问题，后面会有另外的博文做梳理。

运维工作包罗万象，文章内容只是抛砖引玉，开了个头。

牛逼的集群运维需要结合可视化工具（如：`kibana`，`cerebro`，`elastic-hd`，`Prometheus + grafana`，结合业务自研工具如 阿里云 `Eyou` 等）能极大提高效率。













参考文档：

[ElasticSearch 集群的规划部署与运维](https://www.cnblogs.com/codeshell/p/14472651.html)