[toc]



# Zookeeper简介和基本概念

## Zookeeper简介

### 定义

ZooKeeper 官网是这么介绍的：”**Apache ZooKeeper 致力于开发和维护一个支持高度可靠的分布式协调的开源服务器**“

### ZooKeeper是个啥

ZooKeeper 是 Apache 软件基金会的一个软件项目，它为大型「**分布式计算**」提供开源的分布式配置服务、同步服务和命名注册。

Zookeeper 最早起源于雅虎研究院的一个研究小组。在当时，研究人员发现，在雅虎内部很多大型系统基本都需要依赖一个类似的系统来进行分布式协调，但是这些系统往往都存在分布式单点问题。所以，雅虎的开发人员就试图开发一个通用的无单点问题的**分布式协调框架**，以便让开发人员将精力集中在处理业务逻辑上，Zookeeper 就这样诞生了。后来捐赠给了 `Apache` ，现已成为 `Apache` 顶级项目。

> 关于“ZooKeeper”这个项目的名字，其实也有一段趣闻。在立项初期，考虑到之前内部很多项目都是使用动物的名字来命名的（例如著名的Pig项目)，雅虎的工程师希望给这个项目也取一个动物的名字。时任研究院的首席科学家 RaghuRamakrishnan 开玩笑地说：“再这样下去，我们这儿就变成动物园了！”此话一出，大家纷纷表示就叫动物园管理员吧一一一因为各个以动物命名的分布式组件放在一起，雅虎的整个分布式系统看上去就像一个大型的动物园了，而 Zookeeper 正好要用来进行分布式环境的协调一一于是，Zookeeper 的名字也就由此诞生了。

ZooKeeper 是用于维护配置信息，命名，提供分布式同步和提供组服务的集中式服务。所有这些类型的服务都以某种形式被分布式应用程序使用。每次实施它们时，都会进行很多工作来修复不可避免的 bug 和竞争条件。由于难以实现这类服务，因此应用程序最初通常会跳过它们，这会使它们在存在更改的情况下变得脆弱并且难以管理。即使部署正确，这些服务的不同实现也会导致管理复杂。

ZooKeeper 的目标是将这些不同服务的精华提炼为一个非常简单的接口，用于集中协调服务。服务本身是分布式的，并且高度可靠。服务将实现共识，组管理和状态协议，因此应用程序不需要自己实现它们。

### 特性

![image-20210313155801209](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313155801.png)

1. ZooKeeper：一个领导者（leader），多个跟随者（follower）组成的集群。
2. Leader 负责进行投票的发起和决议，更新系统状态。
3. Follower 用于接收客户请求并向客户端返回结果，在选举 Leader 过程中参与投票。
4. 集群中只要有半数以上节点存活，Zookeeper 集群就能正常服务。
5. **全局数据一致**（单一视图）：每个 Server 保存一份相同的数据副本，Client 无论连接到哪个 Server，数据都是一致的。
6. **顺序一致性：** 从同一客户端发起的事务请求，最终将会严格地按照顺序被应用到 ZooKeeper 中去。
7. **原子性：** 所有事务请求的处理结果在整个集群中所有机器上的应用情况是一致的，也就是说，要么整个集群中所有的机器都成功应用了某一个事务，要么都没有应用。
8. **实时性**，在一定时间范围内，client 能读到最新数据。
9. **可靠性：** 一旦一次更改请求被应用，更改的结果就会被持久化，直到被下一次更改覆盖。

### 设计目标

- **简单的数据结构** ：Zookeeper 使得分布式程序能够通过一个共享的树形结构的名字空间来进行相互协调，即Zookeeper 服务器内存中的数据模型由一系列被称为`ZNode`的数据节点组成，**Zookeeper 将全量的数据存储在内存中，以此来提高服务器吞吐、减少延迟的目的**。
- **可以构建集群** ： Zookeeper 集群通常由一组机器构成，组成 Zookeeper 集群的每台机器都会在内存中维护当前服务器状态，并且每台机器之间都相互通信。
- **顺序访问** ： 对于来自客户端的每个更新请求，Zookeeper 都会**分配一个全局唯一的递增编号**，这个编号反映了所有事务操作的先后顺序。
- **高性能** ：Zookeeper 和 Redis 一样全量数据存储在内存中，100% 读请求压测 QPS 12-13W



## 基本概念

### 集群角色

通常在分布式系统中，构成一个集群的每一台机器都有自己的角色，最典型的就是Master/Slave的主备模式。能够处理写操作的机器成为Master机器，把所有通过异步复制方式获取最新数据，并提供服务的机器为Slave机器。

在Zookeeper中，它没有引用Master/Slave概念，而是引入Leader、Follower、Observer三种角色。Zookeeper集群中的所有机器通过Leader选举来选定一台呗成为Leader的机器，Leader服务器为客户端提供读和写的服务，除了Leader机器外的其他机器，包括Follower和Observer都能提供读服务，但是Observer机器不参与Leader选举过程，不参与写操作的过半写成功策略，因此Observer可以在不影响写性能的情况下提升集群的性能。

### 会话（Session）

Session指的是客户端会话，一个客户端连接是指客户端和服务端之间的一个TCP长连接，Zookeeper对外的服务端口默认为2181，客户端启动的时候，首先会与服务器建立一个TCP长连接，从第一次连接建立开始，客户端会话的生命周期也就开始了，通过这个连接，客户端能够使用心跳检测机制与服务器保持有效的会话，也能够向Zookeeper服务器发送请求并接受响应，同时还能通过该连接接受来自服务器的Watch事件通知。

Session 指的是 ZooKeeper 服务器与客户端会话。

Session 作为会话实体，用来代表客户端会话，其包括 4 个属性：

- **SessionID**，用来全局唯一识别会话；
- **TimeOut**，会话超时事件。客户端在创造 Session 实例的时候，会设置一个会话超时的时间。当由于服务器压力太大、网络故障或是客户端主动断开连接等各种原因导致客户端连接断开时，只要在 sessionTimeout 规定的时间内能够重新连接上集群中任意一台服务器，那么之前创建的会话仍然有效；
- **TickTime**，下次会话超时时间点；
- **isClosing**，当服务端如果检测到会话超时失效了，会通过设置这个属性将会话关闭。



### 数据节点（ZNode）

通常‘节点’指的是组成集群的每一台机器，然而在Zookeeper中，“节点”分为两类，第一类同样指的是组成集群的机器，我们称之为机器节点；第二类则指的是数据模型中数据单元，我们称之为数据节点–ZNode。在Zookeeper中会将所有的数据存储在内存中，数据模型是一棵树也成为ZNode Tree，又斜杠（/）进行分割的路径，就是一个ZNode，例如/znode/path1。每个ZNode上都会保存自己的数据内容以及一系列看ii的属性信息。

### 版本

对于每个ZNode，Zookeeper都会为其为维护一个叫做Stat的数据结构，Stat记录了这个ZNode的三个数据版本，分别是version（当前ZNode的版本）、cversion（当前ZNode子节点的版本）、aversion（当前ZNode的ACL版本）。

### Watcher（事件监听器）

Watcher事件监听器是在Zookeeper中很重要的特性，Zookeeper允许用户在指定节点上注册一些Watcher事件监听器，并且在特定时间触发的时候，Zookeeper服务端会将时间通知到感兴趣的客户端。

### ACL

Zookeeper采用ACL（Access Control Lists）策略来进行权限控制，其中定义了五种权限

- CREATE
  创建子节点的权限
- READ
  获取节点数据和子节点列表的权限
- WRITE
  更新节点数据的权限
- DELETE
  删除子节点的权限
- ADMIN
  设置节点的ACL权限的权限



### 集群角色

最典型集群模式：Master/Slave 模式（主备模式）。在这种模式中，通常 Master 服务器作为主服务器提供写服务，其他的 Slave 从服务器通过异步复制的方式获取 Master 服务器最新的数据提供读服务。

但是，在 ZooKeeper 中没有选择传统的 Master/Slave 概念，而是引入了**Leader**、**Follower** 和 **Observer** 三种角色。

- Leader： 为客户端提供**读和写**的服务，负责投票的发起和决议，更新系统状态
- Follower： 为客户端提供读服务，如果是写服务则转发给 Leader。在选举过程中参与投票
- Observer： 为客户端提供读服务器，如果是写服务则转发给 Leader。不参与选举过程中的投票，也不参与“过半写成功”策略。在不影响写性能的情况下提升集群的读性能。此角色是在 zookeeper3.3 系列新增的角色。

![image-20210313161313687](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313161313.png)

![image-20210313161329266](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313161329.png)

**server 状态**

- LOOKING：寻找Leader状态
- LEADING：领导者状态，表明当前服务器角色是 Leader
- FOLLOWING：跟随者状态，表明当前服务器角色是 Follower
- OBSERVING：观察者状态，表明当前服务器角色是 Observer

Zookeeper的核心是原子广播，这个机制保证了各个Server之间的同步。实现这个机制的协议叫做Zab协议。Zab协议有两种模式，它们分别是恢复模式（选主）和广播模式（同步）。当服务启动或者在领导者崩溃后，Zab就进入了恢复模式，当领导者被选举出来，且大多数Server完成了和leader的状态同步以后，恢复模式就结束了。状态同步保证了leader和Server具有相同的系统状态。

为了保证事务的顺序一致性，zookeeper采用了递增的事务id号（zxid）来标识事务。所有的提议（proposal）都在被提出的时候加上了zxid。实现中zxid是一个64位的数字，它高32位是epoch用来标识leader关系是否改变，每次一个leader被选出来，它都会有一个新的epoch，标识当前属于那个leader的统治时期。低32位用于递增计数。



### 选举机制

半数通过

- 3台机器 挂一台 2>3/2
- 4台机器 挂2台 2！>4/2

![image-20210313160955856](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313160955.png)

1. 服务器1启动，此时只有它一台服务器启动了，它发出去的报文没有任何响应，所以它的选举状态一直是LOOKING 状态。
2. 服务器2启动，它与最开始启动的服务器1进行通信，互相交换自己的选举结果，由于两者都没有历史数据，所以 id 值较大的服务器2胜出，但是由于没有达到超过半数以上的服务器都同意选举它(这个例子中的半数以上是3)，所以服务器1、2还是继续保持 LOOKING 状态。
3. 服务器3启动，根据前面的理论分析，服务器3成为服务器1、2、3中的老大，而与上面不同的是，此时有三台服务器选举了它，所以它成为了这次选举的Leader。
4. 服务器4启动，根据前面的分析，理论上服务器4应该是服务器1、2、3、4中最大的，但是由于前面已经有半数以上的服务器选举了服务器3，所以它只能接受当小弟的命了。
5. 服务器5启动，同4一样当小弟。



### Zookeeper 的读写机制

- Zookeeper是一个由多个server组成的集群
- 一个leader，多个follower
-  每个server保存一份数据副本
- 全局数据一致
- 分布式读写
- 更新请求转发，由leader实施



### Zookeeper 的保证

- 更新请求顺序进行，来自同一个client的更新请求按其发送顺序依次执行
- 数据更新原子性，一次数据更新要么成功，要么失败
- 全局唯一数据视图，client无论连接到哪个server，数据视图都是一致的
- 实时性，在一定事件范围内，client能读到最新数据



### Zookeeper节点数据操作流程

![image-20210313161726099](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313161726.png)

流程说明：

1. 在Client向Follwer发出一个写的请求

2. Follwer把请求发送给Leader

3. Leader接收到以后开始发起投票并通知Follwer进行投票

4. Follwer把投票结果发送给Leader

5. Leader将结果汇总后如果需要写入，则开始写入同时把写入操作通知给Leader，然后commit;

6. Follwer把请求结果返回给Client

Follower主要有四个功能：

1. 向Leader发送请求（PING消息、REQUEST消息、ACK消息、REVALIDATE消息）；
2. 接收Leader消息并进行处理；
3. 接收Client的请求，如果为写请求，发送给Leader进行投票；
4. 返回Client结果。


Follower的消息循环处理如下几种来自Leader的消息：

1. PING消息： 心跳消息；
2. PROPOSAL消息：Leader发起的提案，要求Follower投票；
3. COMMIT消息：服务器端最新一次提案的信息；
4. UPTODATE消息：表明同步完成；
5. REVALIDATE消息：根据Leader的REVALIDATE结果，关闭待revalidate的session还是允许其接受消息；
6. SYNC消息：返回SYNC结果到客户端，这个消息最初由客户端发起，用来强制得到最新的更新。













