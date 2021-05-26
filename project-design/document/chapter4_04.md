[toc]



# 分布式事务 - 三阶段提交协议（3PC）

## 1.什么是三阶段提交协议？

三阶段提交（Three-phase commit），也叫三阶段提交协议（Three-phase commit protocol），是二阶段提交（2PC）的改进版本。

与两阶段提交不同的是，三阶段提交有两个改动点。

1. 引入超时机制。同时在协调者和参与者中都引入超时机制。

2. 在第一阶段和第二阶段中插入一个准备阶段。保证了在最后提交阶段之前各参与节点的状态是一致的。

也就是说，除了引入超时机制之外，3PC把2PC的准备阶段再次一分为二，这样三阶段提交就有`CanCommit`、`PreCommit`、`DoCommit`三个阶段。

## 2.三阶段提交协议交互过程描述

三阶段提交协议在协调者和参与者中都引入超时机制，并且把两阶段提交协议的第一个阶段拆分成了两步：询问，然后再锁资源，最后真正提交。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418204010.webp)

**2.1.三个阶段的执行**

1. **CanCommit阶段**

   `3PC` 的 `CanCommit` 阶段其实和 `2PC` 的准备阶段很像。

   协调者向参与者发送 `commit` 请求，参与者如果可以提交就返回 `Yes` 响应，否则返回 `No` 响应。

2. **PreCommit阶段**

   `Coordinator`  根据  `Cohort` 的反应情况来决定是否可以继续事务的 `PreCommit` 操作。

   根据响应情况，有以下两种可能。

   - 假如 `Coordinator` 从所有的 `Cohort` 获得的反馈都是 `Yes` 响应，那么就会进行事务的预执行：
     - 发送预提交请求。`Coordinator` 向 `Cohort` 发送 `PreCommit` 请求，并进入 `Prepared` 阶段。
     - 事务预提交。`Cohort` 接收到 `PreCommit` 请求后，会执行事务操作，并将 `undo` 和 `redo` 信息记录到事务日志中。
     - 响应反馈。如果 `Cohort` 成功的执行了事务操作，则返回 `ACK` 响应，同时开始等待最终指令。

   - 假如有任何一个 `Cohort` 向 `Coordinator` 发送了 `No` 响应，或者等待超时之后，`Coordinator` 都没有接到 `Cohort` 的响应，那么就中断事务：
     - 发送中断请求。`Coordinator` 向所有 `Cohort` 发送 `abort` 请求。
     - 中断事务。`Cohort` 收到来自 `Coordinator` 的 `abort` 请求之后（或超时之后，仍未收到 `Cohort` 的请求），执行事务的中断。

3. **DoCommit阶段**

   该阶段进行真正的事务提交，也可以分为以下两种情况:

   - 执行提交
     - 发送提交请求。`Coordinator` 接收到 `Cohort` 发送的 `ACK` 响应，那么他将从预提交状态进入到提交状态。并向所有 `Cohort` 发送 `doCommit` 请求。
     - 事务提交。`Cohort` 接收到 `doCommit` 请求之后，执行正式的事务提交。并在完成事务提交之后释放所有事务资源。
     - 响应反馈。事务提交完之后，向 `Coordinator` 发送 `ACK` 响应。
     - 完成事务。`Coordinator` 接收到所有 `Cohort` 的 `ACK` 响应之后，完成事务。

   - 中断事务
     - `Coordinator` 没有接收到 `Cohort` 发送的 `ACK` 响应（可能是接受者发送的不是 `ACK` 响应，也可能响应超时），那么就会执行中断事务。

**2.2.三阶段提交协议和两阶段提交协议的不同**

对于协调者(`Coordinator`)和参与者(`Cohort`)都设置了超时机制（在 `2PC` 中，只有协调者拥有超时机制，即如果在一定时间内没有收到`cohort` 的消息则默认失败）。

在 `2PC` 的准备阶段和提交阶段之间，插入预提交阶段，使 `3PC` 拥有 `CanCommit`、`PreCommit`、`DoCommit` 三个阶段。

`PreCommit` 是一个缓冲，保证了在最后提交阶段之前各参与节点的状态是一致的。

**2.3.三阶段提交协议的缺点**

如果进入 `PreCommit` 后，`Coordinator` 发出的是 `abort` 请求，假设只有一个 `Cohort` 收到并进行了 `abort` 操作，而其他对于系统状态未知的 `Cohort` 会根据 `3PC` 选择继续 `Commit`，此时系统状态发生不一致性。

