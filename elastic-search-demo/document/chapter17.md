[toc]



# ElasticSearch 面试题



## REST API在 Elasticsearch 方面有哪些优势？

REST API是使用超文本传输协议的系统之间的通信，该协议以 XML 和 JSON格式传输数据请求。

REST 协议是无状态的，并且与带有服务器和存储数据的用户界面分开，从而增强了用户界面与任何类型平台的可移植性。它还提高了可伸缩性，允许独立实现组件，因此应用程序变得更加灵活。

REST API与平台和语言无关，只是用于数据交换的语言是XML或JSON。

借助：REST API 查看集群信息或者排查问题都非常方便。



## Elasticsearch的倒排索引是什么？

面试官：想了解你对基础概念的认知。

解答：通俗解释一下就可以。

倒排索引是搜索引擎的核心。搜索引擎的主要目标是在查找发生搜索条件的文档时提供快速搜索。倒排索引是一种像数据结构一样的散列图，可将用户从单词导向文档或网页。它是搜索引擎的核心。其主要目标是快速搜索从数百万文件中查找数据。

传统的我们的检索是通过文章，逐个遍历找到对应关键词的位置。

而倒排索引，是通过分词策略，形成了词和文章的映射关系表，这种词典+映射表即为倒排索引。

有了倒排索引，就能实现o（1）时间复杂度的效率检索文章了，极大的提高了检索效率。

![image-20210227222928353](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210227222928353.png)

学术的解答方式：

倒排索引，相反于一篇文章包含了哪些词，它从词出发，记载了这个词在哪些文档中出现过，由两部分组成——词典和倒排表。

**加分项**：倒排索引的底层实现是基于：FST（Finite State Transducer）数据结构。

lucene从4+版本后开始大量使用的数据结构是FST。FST有两个优点：

1. 空间占用小。通过对词典中单词前缀和后缀的重复利用，压缩了存储空间；
2. 查询速度快。O(len(str))的查询时间复杂度。

 

## 解释 Elasticsearch 中的相关性和得分？

当你在互联网上搜索有关 Apple 的信息时。它可以显示有关水果或苹果公司名称的搜索结果。

- 你可能要在线购买水果，检查水果中的食谱或食用水果，苹果对健康的好处。
- 你也可能要检查Apple.com，以查找该公司提供的最新产品范围，检查评估公司的股价以及最近6个月，1或5年内该公司在纳斯达克的表现。

同样，当我们从 Elasticsearch 中搜索文档（记录）时，你会对获取所需的相关信息感兴趣。基于相关性，通过Lucene评分算法计算获得相关信息的概率。

ES 会将相关的内容都返回给你，只是：计算得出的评分高的排在前面，评分低的排在后面。

计算评分相关的两个核心因素是：词频和逆向文档频率（文档的稀缺性）。

大体可以解释为：单篇文档词频越高、得分越高；多篇文档某词越稀缺，得分越高。



## 请解释有关 Elasticsearch的 NRT？

从文档索引（写入）到可搜索到之间的延迟默认一秒钟，因此Elasticsearch是近实时（NRT）搜索平台。

也就是说：文档写入，最快一秒钟被索引到，不能再快了。

写入调优的时候，我们通常会动态调整：refresh_interval = 30s 或者更达值，以使得写入数据更晚一点时间被搜索到。



## elasticsearch 是如何实现 master 选举的

面试官：想了解 ES 集群的底层原理，不再只关注业务层面了。

前置前提：

1. 只有候选主节点（master：true）的节点才能成为主节点。
2. 最小主节点数（min_master_nodes）的目的是防止脑裂。

Elasticsearch 的选主是 ZenDiscovery 模块负责的，主要包含 Ping（节点之间通过这个RPC来发现彼此）和 Unicast（单播模块包含一个主机列表以控制哪些节点需要 ping 通）这两部分；
获取主节点的核心入口为 findMaster，选择主节点成功返回对应 Master，否则返回 null。

选举流程大致描述如下：
第一步：确认候选主节点数达标，elasticsearch.yml 设置的值 discovery.zen.minimum_master_nodes;
第二步：对所有候选主节点根据nodeId字典排序，每次选举每个节点都把自己所知道节点排一次序，然后选出第一个（第0位）节点，暂且认为它是master节点。
第三步：如果对某个节点的投票数达到一定的值（候选主节点数n/2+1）并且该节点自己也选举自己，那这个节点就是master。否则重新选举一直到满足上述条件。

- 补充：
  - 这里的 id 为 string 类型。
  - master 节点的职责主要包括集群、节点和索引的管理，不负责文档级别的管理；data 节点可以关闭 http 功能



## 如何解决ES集群的脑裂问题

所谓集群脑裂，是指 Elasticsearch 集群中的节点（比如共 20 个），其中的 10 个选了一个 master，另外 10 个选了另一个 master 的情况。

1. 当集群 master 候选数量不小于 3 个时，可以通过设置最少投票通过数量（discovery.zen.minimum_master_nodes）超过所有候选节点一半以上来解决脑裂问题；

2. 当候选数量为两个时，只能修改为唯一的一个 master 候选，其他作为 data 节点，避免脑裂问题。



## 在并发情况下，ES如果保证读写一致？

1. 可以通过版本号使用乐观并发控制，以确保新版本不会被旧版本覆盖，由应用层来处理具体的冲突；
2. 另外对于写操作，一致性级别支持quorum/one/all，默认为quorum，即只有当大多数分片可用时才允许写操作。但即使大多数可用，也可能存在因为网络等原因导致写入副本失败，这样该副本被认为故障，分片将会在一个不同的节点上重建。
3. 对于读操作，可以设置replication为sync(默认)，这使得操作在主分片和副本分片都完成后才会返回；如果设置replication为async时，也可以通过设置搜索请求参数_preference为primary来查询主分片，确保文档是最新版本。



### 详细解释

1. **当我们在说一致性，我们在说什么？**

   在分布式环境下，一致性指的是多个数据副本是否能保持一致的特性。

   在一致性的条件下，系统在执行数据更新操作之后能够从一致性状态转移到另一个一致性状态。

   对系统的一个数据更新成功之后，如果所有用户都能够读取到最新的值，该系统就被认为具有强一致性。

   <img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/1.webp" alt="1" style="zoom:50%;" />

2. **ES 的数据一致性问题**

   ##### es选举Master: [ES选举-类Bully算法](https://www.jianshu.com/p/9454ac19921d)

   ##### Master选举：[程序员小灰拜占庭将军问题和Raft算法](https://links.jianshu.com/go?to=https%3A%2F%2Fblog.csdn.net%2Fbjweimengshu%2Farticle%2Fdetails%2F80222416)

   ES 数据并发冲突控制是基于的乐观锁和版本号的机制

   一个document第一次创建的时候，它的_version内部版本号就是1；以后，每次对这个document执行修改或者删除操作，都会对这个_version版本号自动加1；哪怕是删除，也会对这条数据的版本号加1(假删除)。

   客户端对es数据做更新的时候，如果带上了版本号，那带的版本号与es中文档的版本号一致才能修改成功，否则抛出异常。如果客户端没有带上版本号，首先会读取最新版本号才做更新尝试，这个尝试类似于CAS操作，可能需要尝试很多次才能成功。乐观锁的好处是不需要互斥锁的参与。

   es节点更新之后会向副本节点同步更新数据(同步写入)，直到所有副本都更新了才返回成功。

   

   **分片向副本同步数据：**

   <img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/2.webp" alt="2" style="zoom:67%;" />

   **es节点之间强连通：**

   <img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/3.webp" alt="3" style="zoom:67%;" />

   **Elasticsearch Master 节点的职责**

	> There are two fault detection processes running. The first is by the master, to ping all the other nodes in the cluster and verify that they are alive. And on the other end, each node pings to master to verify if its still alive or an election process needs to be initiated.

	1. 由主节点负责ping 所有其他节点，判断是否有节点已经挂掉
	2. 创建或删除索引
	3. 决定分片在节点之间的分配

	稳定的主节点对集群的健康是非常重要的。虽然主节点也可以协调节点，路由搜索和从客户端新增数据到数据节点，但最好不要使用这些专用的主节点。一个重要的原则是，尽可能做尽量少的工作。
	 对于大型的生产集群来说，推荐使用一个专门的主节点来控制集群，该节点将不处理任何用户请求。



3. **ElasticSearch 的数据实时性**

   ElasticSearch 是通过怎样的手段做到数据的近实时搜索的？
   
   <img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/4.webp" alt="4" style="zoom:67%;" />
   
   一个Index由若干段组成,搜索的时候按段搜索,我们索引一条段后，每个段会通过fsync 操作持久化到磁盘，而fsync 操作比较耗时,如果每索引一条数据都做这个full commit(rsync)操作,提交和查询的时延都非常之大,所以在这种情况下做不到实时的一个搜索。
   
   
   
   **FileSystem Cache  与 refresh**
   
   针对这个问题的解决是在Elasticsearch和磁盘之间引入一层称为FileSystem Cache的系统缓存，正是由于这层cache的存在才使得es能够拥有更快搜索响应能力。
   
   **新的文档数据写入缓存区：**
   
   <img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/5.webp" alt="5" style="zoom:80%;" />
   
   **缓存区的数据写入一个新段：**
   
   <img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/6.webp" alt="6" style="zoom:80%;" />
   
   es中新增的document会被收集到indexing buffer区后被重写成一个segment然在es中新增的document会被收集到indexing buffer区后被重写成一个segment然后直接写入filesystem cache中，这个操作是非常轻量级的，相对耗时较少，之后经过一定的间隔或外部触发后才会被flush到磁盘上，这个操作非常耗时。但只要sengment文件被写入cache后，这个segment就可以打开和查询，从而确保在短时间内就可以搜到，而不用执行一个full commit也就是fsync操作，这是一个非常轻量级的处理方式而且是可以高频次的被执行，而不会破坏es的性能。
   
   在elasticsearch里面，这个轻量级的写入和打开一个cache中的segment的操作叫做refresh，默认情况下，es集群中的每个shard会每隔1秒自动refresh一次，这就是我们为什么说es是近实时的搜索引擎而不是实时的，也就是说给索引插入一条数据后，我们需要等待1秒才能被搜到这条数据，这是es对写入和查询一个平衡的设置方式，这样设置既提升了es的索引写入效率同时也使得es能够近实时检索数据。



## ES对于大数据量（上亿量级）的聚合如何实现？

Elasticsearch 提供的首个近似聚合是cardinality 度量。它提供一个字段的基数，即该字段的distinct或者unique值的数目。它是基于HLL算法的。HLL 会先对我们的输入作哈希运算，然后根据哈希运算的结果中的 bits 做概率估算从而得到基数。

其特点是：可配置的精度，用来控制内存的使用（更精确 ＝ 更多内存）；

小的数据集精度是非常高的；我们可以通过配置参数，来设置去重需要的固定内存使用量。无论数千还是数十亿的唯一值，内存使用量只与你配置的精确度相关



## 对于GC方面，在使用ES时要注意什么？

1. 倒排词典的索引需要常驻内存，无法GC，需要监控data node上segment memory增长趋势。
1. 各类缓存，field cache, filter cache, indexing cache, bulk queue等等，要设置合理的大小，并且要应该根据最坏的情况来看heap是否够用，也就是各类缓存全部占满的时候，还有heap空间可以分配给其他任务吗？避免采用clear cache等“自欺欺人”的方式来释放内存。
1. 避免返回大量结果集的搜索与聚合。确实需要大量拉取数据的场景，可以采用scan & scroll api来实现。
1. cluster stats驻留内存并无法水平扩展，超大规模集群可以考虑分拆成多个集群通过tribe node连接。
1. 想知道heap够不够，必须结合实际应用场景，并对集群的heap使用情况做持续的监控。



## Elasticsearch作为解决方案需要注意什么？

本文以15年国外经典博客的框架为线索，剔除过时的技术体系、技术栈内容，结合近千万级业务场景和最新Elastic技术洞察重新梳理出：Elasticsearch方案选型必须了解的10件事。

1. 集群规模

   Elasticsearch的优点在于它是非常容易扩展。但，索引和查询时间可能因许多因素而异。在集群规模层面一方面要考虑数据量，另一方面比较重要的衡量因素是项目/产品的指标要求。

   要想达到吞吐量和CPU利用率的指标要求，建议进行一定量的测试，以确认集群承担的负载和性能瓶颈问题。

   测试工具推荐：`Apache Jmeter`。

   网上会有很多的一线互联网公司等的“他山之石”，但，方案仅供参考，需要自己结合业务场景、硬件资源进行`反复测试`验证。

2. 节点职责

   Elasticsearch节点可以是主节点（Master），数据节点（Data），客户端/路由节点（Client）或某种组合。 大多数人大规模集群选择专用主节点（至少3个），然后选择一些数据和客户端节点。

   建议：`职责分离`，并您针对特定工作负载优化每种类型的节点的分配。

   例如，通过分离客户端和数据节点提升性能。 客户端节点处理传入的HTTP请求，这使得数据节点为查询提供服务。

   这并不是绝对的，有大量网友在社区反馈，分离客户端节点并没有提升性能，因实际场景而异，大规模数据增量的业务场景，职责分离必然是大势所趋。

3. 安全

   近期，未加任何安全防护措施的Elastic`安全事件`频发。建议在应用程序API和Elasticsearch层之间以及Elasticsearch层和内部网络之间保护您的Elasticsearch集群。

   1. 6.3+版本之后，xpack插件已经集成到Elastic产品线。（收费）
   2. 加一层Nginx代理，能防止未经授权的访问。
   3. 其他选型推荐：search-guard，readonlyRest等。

   “裸奔的风险非常大”，进阶阅读：[你的Elasticsearch在裸奔吗？](http://mp.weixin.qq.com/s?__biz=MzI2NDY1MTA3OQ==&mid=2247484309&idx=1&sn=0f3921611ea97715cf616d2c13ba85a2&chksm=eaa82bbddddfa2aba7a54203a2a6a2a1041ef1ba3ed19dd24d953b526cab4b21539a04e4159e&scene=21#wechat_redirect)



## 数据建模

1. **使用别名**

   业务层面使用`别名`进行检索、聚合操作。

   别名的好处：

   - 将应用和索引名称隔离；

   - 可以方便的实现跨索引检索。

2. **数据类型选型**

   若不指定数据类型的动态映射机制，比如：字符串类型会默认存储为text和keyword两种类型，势必会`增加存储成本`。
   建议：针对业务场景需求，静态的手动指定好每个字段的数据类型。

   考虑因素包含但不限于：

   - 是否需要索引；
   - 是否需要存储；
   - 是否需要分词；
   - 是否需要聚合；
   - 是否需要多表关联（nested类型、join或者是宽表存储）；
   - 是否需要快速响应（keyword和long类型选型）
     ……
     此处的`设计时间不能省`。

3. **检索选型**

   Elasticsearch查询DSL非常庞大。如果业务场景不需要计算评分，推荐使用过滤器`filter`。因为基于缓存，更高效。
   查询相关的API包含但不限于：

    - match/multi_match
    - match_phrase/match_phrase_prefix
    - term/terms
    - wildcard/regexp
    - query_string
   
   选型前，建议通过Demo验证一下是否符合预期。
   
   了解如何编写高效查询是一回事，但让它们返回最终用户期望的结果是另一回事。
   
   业务实战中，建议`花一些时间`调整分析器、分词和评分，以便ES返回期望的正确的命中。

4. **监控和警报**

   请务必考虑一个完全独立的“监视”集群机制，该机制仅用于捕获有关群集运行状况的统计信息，并在出现问题时提醒您。

   **监控作用**：能通过可视化的方式，直观的看到内存、JVM、CPU、负载、磁盘等的使用情况，以对可能的突发情况及早做出应对方案。

   **警报作用**：异常实时预警。

   ES6.X xpack已经集成watcher工具。它会监视某些条件，并在满足这些条件时提醒您。

   举例：当某些状态（例如JVM堆）达到阈值时，您可以采取一些操作（发送电子邮件，调用Web钩子等）。

   如果你的业务场景是：几乎实时地将数据写入Elasticsearch并希望在数据与某些`模式匹配`时收到警报，则推荐使用`ElastAlert`。

   https://github.com/Yelp/elastalert

5. **节点配置和配置管理**

    一旦拥有多个节点，就每个节点在软件版本、配置等方面`保持同步`变得具有挑战性。

    有许多开源工具可以帮助解决这个问题。推荐：`Chef`和`Ansible`帮助管理Elasticsearch集群。

    `Ansible`可以自动执行升级和配置传播，而无需在任何Elasticsearch节点上安装任何其他软件。

    当前可能看不到对自动化的巨大需求，如果要从小规模开始发展，并且希望能够快速发展的话，一个使用Ansible编写的常见任务库可以使你在几分钟内从裸服务器转到完全配置的Elasticsearch节点，`无需人工干预`。

    增量索引的管理推荐：rollover + curator + crontab，6.6版本的新特性：`Index Lifecycle Management(索引生命周期管理）`，推荐尝鲜使用。

6. **备份和恢复**

   经常被问到的问题1“ES中误删除的数据（delete或者delete_by_query）能恢复吗？”
   ——答案：如果做了备份，是可以的。如果没有，不可以。

   问题2：“迁移节点，直接data路径原封不动拷贝可以吗？”
   ——答案：不可以，不推荐。推荐使用reindex或其他工具实现。

   对于高可用性的业务系统，数据的`备份`功能非常重要。 由于数据的存储可能会涉及多个节点，依赖OS级文件系统备份可能会很冒险。

   推荐使用Elasticsearch内置的“`快照`”功能，可以备份您的索引。

7. **API选型**

   Elastic`官方`支持API，包含：JAVA、Java Script、.net、PHP、python、Ruby。
   Elastic民间API（社区贡献）非常庞大：C++、Go等20多种。

   API选型推荐使用：`官方API`。

   原因：

   - 版本更新及时、
   - 新特性支持适配更新及时。

   DSL开发推荐使用的Kibana的`Dev-tool`，非常高效、方便。

8. **数据接入**

   将数据索引到Elasticsearch很容易。 根据数据源和其他因素，您可以自己编写，也可以使用Elastic中的`Logstash`工具。

   Logstash可以查看日志文件或其他输入，然后有效地将数据索引到集群中。

   其他大数据组件或开源项目也有类似的功能，举例：

   `kafka-connector`，`flume`，`canal`等。

   选型中，`不一棵树上吊死`，综合对比性能和稳定性，找适合自己业务场景的最为重要。

9. **小结**

   安装和运行开箱即用的Elasticsearch集群非常简单。 使其适用于你的实际业务场景并满足你的性能指标非常不容易。



## es分布式架构原理

首先需要明白es是如何存储数据的，es把对应的数据转换为index。

基于倒排索引的方式，每个index上存储了多个type类型，每个type对应一个document。而一个index会被分成多个shard(默认是5个)。 

在分布式部署时，每个shard会被复制，即一个shard有primary和replica 每个es进程存储的是不同shard的primary和replica。

es集群多个节点，会自动选举一个节点为master节点，这个master节点其实就是干一些管理的工作的，比如维护索引元数据，负责切换primary shard和replica shard身份。 

![image-20210227231935464](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210227231935464.png)



## es的数据写入与读取

### es数据的写入

1. **es数据的写入过程**

   注意，客户端是可以在任意节点进行写入数据的，与Kakfa不同。 

   - 客户端选择一个node发送请求过去，这个node就是coordinating node（协调节点）

   - coordinating node，对document进行路由得到对应应该存储到哪个shard，将请求转发给对应的node（有primary shard） 

   - 实际的node上的primary shard处理请求，然后将数据同步到replica node 

   - coordinating node，如果发现primary node和所有replica node都搞定之后，就返回响应结果给客户端 

   ![image-20210227232231301](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210227232231301.png)

2. **es数据的写入原理**

   es数据写入原理主要可以分为4个操作：

   - refresh

   - commit

   - flush

   - merge

	|             |                         操作触发条件                         | 操作过程                                                     |
| ----------- | :----------------------------------------------------------: | ------------------------------------------------------------ |
| **refresh** | 1. 每隔1s进行一次refresh操作 2. buffer已满，则进行一次refresh操作 | 1. buffer将数据写入segment file 2. 清空buffer                |
| **commit**  |      1. 每隔30分钟执行一次translog 2. translog日志已满       | 1. 会主动进行一次refresh操作，把buffer中的数据写入到segment file 2. 生成一个 commit point 文件标识此次操作一件把buffer数据执行到了哪一个segment文件 3. 执行flush操作 |
| **flush**   |                         commit操作中                         | 1. 把file system上的文件全部强制fsync（持久化）到磁盘 2. 清空translog文件 3. 生成一个新的translog文件 |
| **merge**   |                           后台检查                           | 1. 将多个segment文件合并为一个文件，并把.del文件删除 2. commit log 更新标识目前的segment 3. 打开segmentfile 到file cache 以供快速搜索 4. 删除旧的segment file |

<img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/up-93210c6ea1f83b2d638ee38eb5b1af2c35c.png" alt="up-93210c6ea1f83b2d638ee38eb5b1af2c35c"  />



### es数据的读取

1. **读取数据**

   使用RestFul API向对应的node发送查询请求，根据did来判断在哪个shard上，返回的是primary和replica的node节点集合 这样会负载均衡地把查询发送到对应节点，之后对应节点接收到请求，将document数据返回协调节点，协调节点把document返回给客户端 

![up-d7f4d579ea5edca6214b27b8b84e3ae2526](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/up-d7f4d579ea5edca6214b27b8b84e3ae2526.png)

2. **全文检索**
   - 客户端使用RestFul API向对应的node发送查询请求
   - 协调节点将请求转发到所有节点（primary或者replica）所有节点将对应的数据查询之后返回对应的doc id 返回给协调节点
   - 协调节点将doc进行排序聚合
   - 协调节点再根据doc id 把查询请求发送到对应shard的node，返回document



## 详细描述一下 Elasticsearch 写入索引文档的过程

面试官：想了解 ES 的底层原理，不再只关注业务层面了。

解答：

这里的索引文档应该理解为文档写入 ES，创建索引的过程。

文档写入包含：单文档写入和批量 bulk 写入，这里只解释一下：单文档写入流程。

记住官方文档中的这个图。

![image-20210228001106769](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210228001106769.png)

第一步：客户写集群某节点写入数据，发送请求。（如果没有指定路由/协调节点，请求的节点扮演路由节点的角色。）

第二步：节点 1 接受到请求后，使用文档_id 来确定文档属于分片 0。请求会被转到另外的节点，假定节点 3。因此分片 0 的主分片分配到节点 3 上。

第三步：节点 3 在主分片上执行写操作，如果成功，则将请求并行转发到节点 1和节点 2 的副本分片上，等待结果返回。所有的副本分片都报告成功，节点 3 将向协调节点（节点 1）报告成功，节点 1 向请求客户端报告写入成功。

如果面试官再问：第二步中的文档获取分片的过程？

回答：借助路由算法获取，路由算法就是根据路由和文档 id 计算目标的分片 id 的过程。

```bash
1shard = hash(_routing) % (num_of_primary_shards)
```



## elasticsearch 了解多少，说说你们公司 es 的集群架构，索引数据大小，分片有多少，以及一些调优手段 。

面试官：想了解应聘者之前公司接触的 ES 使用场景、规模，有没有做过比较大规模的索引设计、规划、调优。

解答：如实结合自己的实践场景回答即可。

### 架构情况

**情况1：**

生产环境部署情况 :

- es生产集群我们部署了5台机器，每台机器是6核64G的，集群总内存是320G

- 我们es集群的日增量数据大概是2000万条，每天日增量数据大概是500MB， 每月增量数据大概是6亿，15G。目前系统已经运行了几个月，现在es集群里数据总量大概是100G左右。

- 目前线上有5个索引（这个结合你们自己业务来，看看自己有哪些数据可以放es的）， 每个索引的数据量大概是20G，所以这个数据量之内，我们每个索引分配的是8个shard，比默认的5个shard多了3个shard。

**情况2：**

比如：ES 集群架构 13 个节点，索引根据通道不同共 20+索引，根据日期，每日递增 20+，索引：10 分片，每日递增 1 亿+数据，每个通道每天索引大小控制：150GB 之内。

**情况3：**

我司有多个ES集群，下面列举其中一个。该集群有20个节点，根据数据类型和日期分库，每个索引根据数据量分片，比如日均1亿+数据的，控制单索引大小在200GB以内。　



### 调优手段

1. **设计阶段调优**
   - 根据业务增量需求，采取基于日期模板创建索引，通过 roll over API 滚动索引；

   - 使用别名进行索引管理；

   - 每天凌晨定时对索引做 force_merge 操作，以释放空间；

   - 采取冷热分离机制，热数据存储到 SSD，提高检索效率；冷数据定期进行 shrink操作，以缩减存储；

   - 采取 curator 进行索引的生命周期管理；

   - 仅针对需要分词的字段，合理的设置分词器；

   - Mapping 阶段充分结合各个字段的属性，是否需要检索、是否需要存储等。……..

2. **写入调优**

   - 写入前副本数设置为 0；

   - 写入前关闭 refresh_interval 设置为-1，禁用刷新机制；

   - 写入过程中：采取 bulk 批量写入；

   - 写入后恢复副本数和刷新间隔；

   - 尽量使用自动生成的 id。

3. **查询调优**

   - 禁用 wildcard；

   - 禁用批量 terms（成百上千的场景）；

   - 充分利用倒排索引机制，能 keyword 类型尽量 keyword；

   - 数据量大时候，可以先基于时间敲定索引再检索；

   - 设置合理的路由机制。

4. **其他调优**

   部署调优，业务调优等。

   上面的提及一部分，面试者就基本对你之前的实践或者运维经验有所评估了。



### elasticsearch 索引数据多了怎么办，如何调优，部署

面试官：想了解大数据量的运维能力。

解答：索引数据的规划，应在前期做好规划，正所谓“设计先行，编码在后”，这样才能有效的避免突如其来的数据激增导致集群处理能力不足引发的线上客户检索或者其他业务受到影响。

如何调优，正如问题 1 所说，这里细化一下：

1. **索引层面：**
   - 使用批量请求并调整其大小：每次批量数据 5–15 MB 大是个不错的起始点。
   - 段合并：Elasticsearch默认值是20MB/s，对机械磁盘应该是个不错的设置。如果你用的是SSD，可以考虑提高到100-200MB/s。如果你在做批量导入，完全不在意搜索，你可以彻底关掉合并限流。另外还可以增加 index.translog.flush_threshold_size 设置，从默认的512MB到更大一些的值，比如1GB，这可以在一次清空触发的时候在事务日志里积累出更大的段。
   - 如果你的搜索结果不需要近实时的准确度，考虑把每个索引的index.refresh_interval 改到30s。
   - 如果你在做大批量导入，考虑通过设置index.number_of_replicas: 0 关闭副本。
   - 需要大量拉取数据的场景，可以采用scan & scroll api来实现，而不是from/size一个大范围。

2. **动态索引层面**

   基于模板+时间+rollover api 滚动创建索引，举例：设计阶段定义：blog 索引的模板格式为：blog_index_时间戳的形式，每天递增数据。这样做的好处：不至于数据量激增导致单个索引数据量非常大，接近于上线 2 的32 次幂-1，索引存储达到了 TB+甚至更大。

   一旦单个索引很大，存储等各种风险也随之而来，所以要提前考虑+及早避免。

3. **存储层面**

   冷热数据分离存储，热数据（比如最近 3 天或者一周的数据），其余为冷数据。

   对于冷数据不会再写入新数据，可以考虑定期 force_merge 加 shrink 压缩操作，节省存储空间和检索效率。

   - 基于数据+时间滚动创建索引，每天递增数据。控制单个索引的量，一旦单个索引很大，存储等各种风险也随之而来，所以要提前考虑+及早避免。
   - 冷热数据分离存储，热数据（比如最近3天或者一周的数据），其余为冷数据。对于冷数据不会再写入新数据，可以考虑定期force_merge加shrink压缩操作，节省存储空间和检索效率

4. **部署层面**

   一旦之前没有规划，这里就属于应急策略。

   结合 ES 自身的支持动态扩展的特点，动态新增机器的方式可以缓解集群压力，注意：如果之前主节点等规划合理，不需要重启集群也能完成动态新增的。

   - 最好是64GB内存的物理机器，但实际上32GB和16GB机器用的比较多，但绝对不能少于8G，除非数据量特别少，这点需要和客户方面沟通并合理说服对方。
   - 多个内核提供的额外并发远胜过稍微快一点点的时钟频率。
   - 尽量使用SSD，因为查询和索引性能将会得到显著提升。
   - 避免集群跨越大的地理距离，一般一个集群的所有节点位于一个数据中心中。
   - 设置堆内存：节点内存/2，不要超过32GB。一般来说设置export ES_HEAP_SIZE=32g环境变量，比直接写-Xmx32g -Xms32g更好一点。
   - 关闭缓存swap。内存交换到磁盘对服务器性能来说是致命的。如果内存交换到磁盘上，一个100微秒的操作可能变成10毫秒。 再想想那么多10微秒的操作时延累加起来。不难看出swapping对于性能是多么可怕。
   - 增加文件描述符，设置一个很大的值，如65535。Lucene使用了大量的文件，同时，Elasticsearch在节点和HTTP客户端之间进行通信也使用了大量的套接字。所有这一切都需要足够的文件描述符。
   - 不要随意修改垃圾回收器（CMS）和各个线程池的大小。
   - 通过设置gateway.recover_after_nodes、gateway.expected_nodes、gateway.recover_after_time可以在集群重启的时候避免过多的分片交换，这可能会让数据恢复从数个小时缩短为几秒钟。



## Elasticsearch 在部署时，对 Linux 的设置有哪些优化方法

面试官：想了解对 ES 集群的运维能力。

解答：

1. 关闭缓存 swap;
2. 堆内存设置为：Min（节点内存/2, 32GB）;
3. 设置最大文件句柄数；
4. 线程池+队列大小根据业务需要做调整；
5. 磁盘存储 raid 方式——存储有条件使用 RAID10，增加单节点性能以及避免单节点存储故障。



## 拼写纠错是如何实现的？





## 如何监控Elasticsearch集群状态？

1. 查看集群的健康状态

   ```
   http://ip:9200/_cat/health?v
   ```

   ![20200619141926979](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20200619141926979.png)

   **URL说明：**

   - _cat表示查看信息
   - health表明返回的信息为集群健康信息
   - ?v表示返回的信息加上头信息，跟返回JSON信息加上
   - ?pretty同理，就是为了获得更直观的信息，当然，你也可以不加，不要头信息，特别是通过代码获取返回信息进行解释，头信息有时候不需要，写shell脚本也一样，经常要去除一些多余的信息。

   **通过这个链接会返回下面的信息，下面的信息包括：**

   - 集群的状态（status）：red红表示集群不可用，有故障。yellow黄表示集群不可靠但可用，一般单节点时就是此状态。green正常状态，表示集群一切正常。
   - 节点数（node.total）：节点数，这里是3，表示该集群有三个节点。
   - 数据节点数（node.data）：存储数据的节点数，这里是3。
   - 分片数（shards）：这是52，表示我们把数据分成多少块存储。
   - 主分片数（pri）：primary shards，这里是26，实际上是分片数的两倍，因为有一个副本，如果有两个副本，这里的数量应该是分片数的三倍，这个会跟后面的索引分片数对应起来，这里只是个总数。
   - 激活的分片百分比（active_shards_percent）：这里可以理解为加载的数据分片数，只有加载所有的分片数，集群才算正常启动，在启动的过程中，如果我们不断刷新这个页面，我们会发现这个百分比会不断加大。

   

2. 查看集群的索引数

   ```
   http://ip:9200/_cat/indices?v
   ```

   ![20200619142157410](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20200619142157410.png)

   通过该连接返回了集群中的所有索引，其中.kibana是kibana连接后在es建的索引，http-log-*是我自己添加的。
   **这些信息，包括：**

   - 索引健康（health），green为正常，yellow表示索引不可靠（单节点），red索引不可用。与集群健康状态一致。
   - 状态（status），表明索引是否打开。
   - 索引名称（index）。
   - uuid，索引内部随机分配的名称，表示唯一标识这个索引。
   - 主分片（pri），.kibana为1，`http-log-*`为5，加起来主分片数为6，这个就是集群的主分片数。
   - 文档数（docs.count），`http-log-*`添加了三条记录，所以这里的文档数为3。
   - 已删除文档数（docs.deleted），这里统计了被删除文档的数量。
   - 索引存储的总容量（store.size），这里`http-log-*`索引的总容量为66.4kb，是主分片总容量的两倍，因为存在一个副本。
   - 主分片的总容量（pri.store.size），这里`http-log-*`的主分片容量是33.2kb，是索引总容量的一半。

   

3. 查看集群所在磁盘的分配状况

   ```
   http://ip:9200/_cat/allocation?v
   ```

   ![20200619142621636](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20200619142621636.png)

   通过该连接返回了集群中的各节点所在磁盘的磁盘状况
   **返回的信息包括：**

   - 分片数（shards），集群中各节点的分片数相同。
   - 索引所占空间（disk.indices），该节点中所有索引在该磁盘所点的空间
   - 磁盘使用容量（disk.used）
   - 磁盘可用容量（disk.avail）
   - 磁盘总容量（disk.total）
   - 磁盘使用率（disk.percent）

   

4. 查看集群的节点

   ```
   http://ip:9200/_cat/nodes?v
   ```

   ![20200619142835812](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20200619142835812.png)

   通过该连接返回了集群中各节点的情况。这些信息中比较重要的是master列，带`*`星号表明该节点是主节点。带-表明该节点是从节点。
   另外还是heap.percent堆内存使用情况，ram.percent运行内存使用情况，cpu使用情况。

   

5. 查看集群的其它信息

   ```
   http://ip:9200/_cat/
   ```

   查看集群信息的目录

   ![20200619142936945](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20200619142936945.png)



## translog日志文件作用是什么？

**Translog实现机制的理解**

ES作为一个[NoSQL](https://www.yisu.com/mongodb)，典型的应用场景就是存储数据。即用户可以通过api添加数据到es中。由于Lucene内部的实现， 每次添加的数据并不是实时落盘的。而是在内存中维护着索引信息，直到缓冲区满了或者显式的commit, 数据才会落盘，形成一个segement，保存在文件中。

那么假如由于某种原因，ES的进程突然挂了，那些在内存中的数据就会丢失。而实际上，用户调用api, 返回结果确认用户数据已经添加到索引中。这种数据丢失是无法被接受的。怎么解决这个问题呢？

ES实现了Translog， 即数据索引前，会先写入到日志文件中。假如节点挂了，重启节点时就会重放日志，这样相当于把用户的操作模拟了一遍。保证了数据的不丢失。

通过ES的源码，了解一下实现的细节。 首先关注`Translog`类。

`Translog`类是一个索引分片层级的组件，即每个`index shard`一个`Translog`类。它的作用是: 将没有提交的索引操作以持久化的方式记录起来(其实就是写到文件中)。

`InternalEngine` 在`commit metadata`中记录了当前最新的translog generation。 通过这个 generation，可以关联到所有没有commit的操作记录。

每个`Translog`实例在任何时候都只会有一个处于open状态的translog file. 这个translog file跟translog generation ID是一一映射的关系。

出于性能的考虑，灾后重建并不是回放所有的translog, 而是最新没有提交索引的那一部分。所以必须有一个checkpoint, 即translog.ckp文件。

综上，从文件的视角看待translog机制其实是两个文件:

```
$ tree translog
translog
├── translog-11.tlog
└── translog.ckp
```

translog记录日志的格式如下:`|记录size|操作的唯一id|操作的内容|checksum|` 每次add操作返回的location会记录到versionMap中，这样就能实现realtime get的功能了。

了解了这一点，在配置es的时候，有两种途径可以提升ES索引的性能。

```
a. 将translog日子和索引配置到不同的盘片。
b. 将translog的flush间隔设置长一些。比如如下的参数:
index.translog.sync_interval : 30s 
index.translog.durability : “async” 
index.translog.flush_threshold_size: 4g 
index.translog.flush_threshold_ops: 50000
```

了解了translog的机制，会发现，即使是translog机制，也并不能完全能避免数据的丢失。在性能和数据丢失容忍度上，还是需要做一些平衡。



## Elasticsearch如何保证数据不丢失？

### 如何保证数据写入过程中不丢

数据写入请求达到时，以需要的数据格式组织并写入磁盘的过程叫做数据提交，对应es就是创建倒排索引，维护segment文件
如果我们同步的方式，来处理上述过程，那么系统的吞吐量将很低
如果我们以异步的方式，先写入内存，然后再异步提交到磁盘，则有可能因为机器故障而而丢失还未写入到磁盘中的数据

为了解决这个问题，一般的存储系统都会设计transag log (事务日志)或这write ahead log(预写式日志)。它的作用是，将最近的写入数据或操作以日志的形式直接落盘，从而使得即便系统崩溃后，依然可以基于这些磁盘日志进行数据恢复。

Mysql有redo undo log ，而HBASE、LevelDB，RockDB等采用的LSM tree则提供了write ahead log 这样的设计，来保证数据的不丢失

#### 直接落盘的 translog 为什么不怕降低写入吞吐量？

上述论述中，数据以同步方式落盘会有性能问题，为什么将translog和wal直接落盘不影响性能？原因如下：

- 写的日志不需要维护复杂的数据结构，它仅用于记录还未真正提交的业务数据。所以体量小
- 并且以顺序方式写盘，速度快

es默认是每个请求都会同步落盘translog ，即配置`index.translog.durability` 为`request`。当然对于一些可以丢数据的场景，我们可以将`index.translog.durability`配置为`async` 来提升写入translog的性能，该配置会异步写入translog到磁盘。具体多长时间写一次磁盘，则通过`index.translog.sync_interval`来控制

前面说了，为了保证translog足够小，所以translog不能无限扩张，需要在一定量后，将其对应的真实业务数据以其最终数据结构(es是倒排索引)提交到磁盘，这个动作称为flush ，它会实际的对底层Lucene 进行一次commit。我们可以通过`index.translog.flush_threshold_size` 来配置translog多大时，触发一次flush。每一次flush后，原translog将被删除，重新创建一个新的translog

elasticsearch本身也提供了flush api来触发上述commit动作，但无特殊需求，尽量不要手动触发

### 如何保证已写数据在集群中不丢

对每个shard采用副本机制。保证写入每个shard的数据不丢

### in-memory buffer

前述translog只是保证数据不丢，为了其记录的高效性，其本身并不维护复杂的数据结构。 实际的业务数据的会先写入到in-memory buffer中，当调用refresh后，该buffer中的数据会被清空，转而reopen一个segment,使得其数据对查询可见。但这个segment本身也还是在内存中，如果系统宕机，数据依然会丢失。需要通过translog进行恢复

其实这跟lsm tree非常相似，新写入内存的业务数据存放在内存的MemTable（对应es的in-memory buffer），它对应热数据的写入，当达到一定量并维护好数据结构后，将其转成内存中的ImmutableMemTable(对应es的内存segment)，它变得可查询。

### 总结

- refresh 用于将写入内存in-memory buffer数据，转为查询可见的segment
  ![image-20210306215721985](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306215721985.png)
- 每次一次写入除了写入内存外in-memory buffer，还会默认的落盘translog
  ![image-20210306215748639](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306215748639.png)
- translog 达到一定量后，触发in-memory buffer落盘，并清空自己，这个动作叫做flush
  ![image-20210306215812266](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306215812266.png)
- 如遇当前写入的shard宕机，则可以通过磁盘中的translog进行数据恢复



