[toc]



# ZooKeeper工作机制

`ZooKeeper` 从设计模式角度来理解：就是一个基于**观察者模式**设计的分布式服务管理框架，它负责存储和管理大家都关心的数据，然后接受观察者的注册，一旦这些数据的状态发生变化，`ZK` 就将负责通知已经在 `ZK` 上注册的那些观察者做出相应的反应，从而实现集群中类似 `Master/Slave` 管理模式。

![image-20210313155609629](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313155609.png)

- `Zookeeper` 的核心是原子广播，这个机制保证了各个 `server` 之间的同步。实现这个机制的协议叫做 `Zab` 协议。`Zab` 协议有两种模式，它们分别是恢复模式和广播模式。
- 当服务启动或者在领导者崩溃后，`Zab` 就进入了恢复模式，当领导者被选举出来，且大多数 `server` 的完成了和 `leader` 的状态同步以后，恢复模式就结束了。状态同步保证了 `leader` 和 `server` 具有相同的系统状态
- 一旦 `leader` 已经和多数的 `follower` 进行了状态同步后，他就可以开始广播消息了，即进入广播状态。这时候当一个 `server` 加入 `zookeeper` 服务中，它会在恢复模式下启动，现 `leader`，并和 `leader` 进行状态同步。待到同步结束，它也参与消息广播。`Zookeeper` 服务一直维持在 `Broadcast` 状态，直到 `leader` 崩溃了或者 `leader` 失去了大部分的 `followers` 支持。
- 广播模式需要保证 `proposal` 被按顺序处理，因此zk采用了递增的事务 `id` 号(`zxid`)来保证。所有的提议(`proposal`)都在被提出的时候加上了 `zxid`。实现中 `zxid` 是一个 `64` 为的数字，它高 `32` 位是 `epoch` 用来标识 `leader` 关系是否改变，每次一个 `leader` 被选出来，它都会有一个新的 `epoch`。低 `32`位是个递增计数。
- 当 `leader` 崩溃或者 `leader` 失去大多数的 `follower`，这时候zk进入恢复模式，恢复模式需要重新选举出一个新的 `leader`，让所有的 `server` 都恢复到一个正确的状态。　
-  每个 `Server` 启动以后都询问其它的 `Server` 它要投票给谁。
- 对于其他 `server` 的询问，`server` 每次根据自己的状态都回复自己推荐的 `leader` 的 `id` 和上一次处理事务的 `zxid`（系统启动时每个 `server` 都会推荐自己）
- 收到所有 `Server` 回复以后，就计算出 `zxid` 最大的哪个 `Server`，并将这个 `Server` 相关信息设置成下一次要投票的 `Server`。
- 计算这过程中获得票数最多的的 `sever` 为获胜者，如果获胜者的票数超过半数，则改 `server` 被选为 `leader`。否则，继续这个过程，直到 `leader` 被选举出来　　
- `leader` 就会开始等待 `server` 连接
- `Follower` 连接 `leader`，将最大的 `zxid` 发送给 `leader`
- `Leader` 根据 `follower` 的 `zxid` 确定同步点
- 完成同步后通知 `follower` 已经成为 `uptodate` 状态
- `Follower` 收到 `uptodate` 消息后，又可以重新接受 `client` 的请求进行服务了





