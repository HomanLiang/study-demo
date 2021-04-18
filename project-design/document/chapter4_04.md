[toc]



# 分布式事务 - 三阶段提交协议（3PC）

## 1.什么是三阶段提交协议？

三阶段提交（Three-phase commit），也叫三阶段提交协议（Three-phase commit protocol），是二阶段提交（2PC）的改进版本。

与两阶段提交不同的是，三阶段提交有两个改动点。

1、引入超时机制。同时在协调者和参与者中都引入超时机制。
2、在第一阶段和第二阶段中插入一个准备阶段。保证了在最后提交阶段之前各参与节点的状态是一致的。

也就是说，除了引入超时机制之外，3PC把2PC的准备阶段再次一分为二，这样三阶段提交就有`CanCommit`、`PreCommit`、`DoCommit`三个阶段。

## 2.三阶段提交协议交互过程描述

三阶段提交协议在协调者和参与者中都引入超时机制，并且把两阶段提交协议的第一个阶段拆分成了两步：询问，然后再锁资源，最后真正提交。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418204010.webp)

**2.1.三个阶段的执行**

1. **CanCommit阶段**

   3PC的CanCommit阶段其实和2PC的准备阶段很像。

   协调者向参与者发送commit请求，参与者如果可以提交就返回Yes响应，否则返回No响应。

2. **PreCommit阶段**

   Coordinator根据Cohort的反应情况来决定是否可以继续事务的PreCommit操作。

   根据响应情况，有以下两种可能。

   - 假如Coordinator从所有的Cohort获得的反馈都是Yes响应，那么就会进行事务的预执行：
     - 发送预提交请求。Coordinator向Cohort发送PreCommit请求，并进入Prepared阶段。
     - 事务预提交。Cohort接收到PreCommit请求后，会执行事务操作，并将undo和redo信息记录到事务日志中。
     - 响应反馈。如果Cohort成功的执行了事务操作，则返回ACK响应，同时开始等待最终指令。

   - 假如有任何一个Cohort向Coordinator发送了No响应，或者等待超时之后，Coordinator都没有接到Cohort的响应，那么就中断事务：
     - 发送中断请求。Coordinator向所有Cohort发送abort请求。
     - 中断事务。Cohort收到来自Coordinator的abort请求之后（或超时之后，仍未收到Cohort的请求），执行事务的中断。

3. **DoCommit阶段**

   该阶段进行真正的事务提交，也可以分为以下两种情况:

   - 执行提交
     - 发送提交请求。Coordinator接收到Cohort发送的ACK响应，那么他将从预提交状态进入到提交状态。并向所有Cohort发送doCommit请求。
     - 事务提交。Cohort接收到doCommit请求之后，执行正式的事务提交。并在完成事务提交之后释放所有事务资源。
     - 响应反馈。事务提交完之后，向Coordinator发送ACK响应。
     - 完成事务。Coordinator接收到所有Cohort的ACK响应之后，完成事务。

   - 中断事务
     - Coordinator没有接收到Cohort发送的ACK响应（可能是接受者发送的不是ACK响应，也可能响应超时），那么就会执行中断事务。

**2.2.三阶段提交协议和两阶段提交协议的不同**

对于协调者(Coordinator)和参与者(Cohort)都设置了超时机制（在2PC中，只有协调者拥有超时机制，即如果在一定时间内没有收到cohort的消息则默认失败）。

在2PC的准备阶段和提交阶段之间，插入预提交阶段，使3PC拥有CanCommit、PreCommit、DoCommit三个阶段。

PreCommit是一个缓冲，保证了在最后提交阶段之前各参与节点的状态是一致的。

**2.3.三阶段提交协议的缺点**

如果进入PreCommit后，Coordinator发出的是abort请求，假设只有一个Cohort收到并进行了abort操作，而其他对于系统状态未知的Cohort会根据3PC选择继续Commit，此时系统状态发生不一致性。

