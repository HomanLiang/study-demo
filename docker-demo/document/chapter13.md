[toc]



# Docker 常见问题

## 1.我把数据库部署在Docker容器内，你觉得如何？
近2年 `Docker` 非常的火热，各位开发者恨不得把所有的应用、软件都部署在 `Docker` 容器中，但是您确定也要把数据库也部署的容器中吗？

这个问题不是子虚乌有，因为在网上能够找到很多各种操作手册和视频教程，小编整理了一些数据库不适合容器化的原因供大家参考，同时也希望大家在使用时能够谨慎一点。

目前为止将数据库容器化是非常不合理的，但是容器化的优点相信各位开发者都尝到了甜头，希望随着技术的发展能够更加完美的解决方案出现。

### 1.1.Docker不适合部署数据库的7大原因
#### 1.1.1.数据安全问题
不要将数据储存在容器中，这也是 `Docker` 官方容器使用技巧中的一条。容器随时可以停止、或者删除。当容器被 `rm` 掉，容器里的数据将会丢失。为了避免数据丢失，用户可以使用数据卷挂载来存储数据。但是容器的 `Volumes` 设计是围绕 `Union FS` 镜像层提供持久存储，数据安全缺乏保证。如果容器突然崩溃，数据库未正常关闭，可能会损坏数据。另外，容器里共享数据卷组，对物理机硬件损伤也比较大。即使你要把 `Docker` 数据放在主机来存储 ，它依然不能保证不丢数据。`Docker volumes` 的设计围绕 `Union FS` 镜像层提供持久存储，但它仍然缺乏保证。使用当前的存储驱动程序，`Docker` 仍然存在不可靠的风险。如果容器崩溃并数据库未正确关闭，则可能会损坏数据。
#### 1.1.2.性能问题
大家都知道，`MySQL` 属于关系型数据库，对 `IO` 要求较高。当一台物理机跑多个时，`IO` 就会累加，导致 `IO` 瓶颈，大大降低 `MySQL` 的读写性能。

在一次 `Docker` 应用的十大难点专场上，某国有银行的一位架构师也曾提出过：“数据库的性能瓶颈一般出现在 `IO`上面，如果按 `Docker` 的思路，那么多个 `docker` 最终 `IO` 请求又会出现在存储上面。现在互联网的数据库多是 `share nothing` 的架构，可能这也是不考虑迁移到 `Docker` 的一个因素吧”。

针对性能问题有些同学可能也有相对应的方案来解决：

**数据库程序与数据分离**

如果使用 `Docker` 跑 `MySQL`，数据库程序与数据需要进行分离，将数据存放到共享存储，程序放到容器里。如果容器有异常或 `MySQL` 服务异常，自动启动一个全新的容器。另外，建议不要把数据存放到宿主机里，宿主机和容器共享卷组，对宿主机损坏的影响比较大。

**跑轻量级或分布式数据库**

`Docker` 里部署轻量级或分布式数据库，`Docker` 本身就推荐服务挂掉，自动启动新容器，而不是继续重启容器服务。

**合理布局应用**

对于 `IO` 要求比较高的应用或者服务，将数据库部署在物理机或者 `KVM` 中比较合适。目前 `TX` 云的 `TDSQL` 和阿里的 `Oceanbase` 都是直接部署在物理机器，而非 `Docker `。

#### 1.1.3.网络问题
要理解 `Docker` 网络，您必须对网络虚拟化有深入的了解。也必须准备应付好意外情况。你可能需要在没有支持或没有额外工具的情况下，进行 `bug` 修复。

我们知道：数据库需要专用的和持久的吞吐量，以实现更高的负载。我们还知道容器是虚拟机管理程序和主机虚拟机背后的一个隔离层。然而网络对于数据库复制是至关重要的，其中需要主从数据库间 24/7 的稳定连接。未解决的 Docker 网络问题在1.9版本依然没有得到解决。

把这些问题放在一起，容器化使数据库容器很难管理。我知道你是一个顶级的工程师，什么问题都可以得到解决。但是，你需要花多少时间解决 Docker 网络问题？将数据库放在专用环境不会更好吗？节省时间来专注于真正重要的业务目标。

#### 1.1.4.状态
在 `Docker` 中打包无状态服务是很酷的，可以实现编排容器并解决单点故障问题。但是数据库呢？将数据库放在同一个环境中，它将会是有状态的，并使系统故障的范围更大。下次您的应用程序实例或应用程序崩溃，可能会影响数据库。

知识点在 `Docker` 中水平伸缩只能用于无状态计算服务，而不是数据库。

`Docker` 快速扩展的一个重要特征就是无状态，具有数据状态的都不适合直接放在 `Docker` 里面，如果 `Docker` 中安装数据库，存储服务需要单独提供。

目前，`TX` 云的 `TDSQL` (金融分布式数据库)和阿里云的 `Oceanbase` (分布式数据库系统)都直接运行中在物理机器上，并非使用便于管理的 `Docker` 上。

#### 1.1.5.资源隔离
资源隔离方面，`Docker` 确实不如虚拟机 `KVM`，`Docker` 是利用 `Cgroup` 实现资源限制的，只能限制资源消耗的最大值，而不能隔绝其他程序占用自己的资源。如果其他应用过渡占用物理机资源，将会影响容器里 `MySQL` 的读写效率。

需要的隔离级别越多，获得的资源开销就越多。相比专用环境而言，容易水平伸缩是 `Docker` 的一大优势。然而在 `Docker` 中水平伸缩只能用于无状态计算服务，数据库并不适用。

我们没有看到任何针对数据库的隔离功能，那为什么我们应该把它放在容器中呢？

#### 1.1.6.云平台的不适用性
大部分人通过共有云开始项目。云简化了虚拟机操作和替换的复杂性，因此不需要在夜间或周末没有人工作时间来测试新的硬件环境。当我们可以迅速启动一个实例的时候，为什么我们需要担心这个实例运行的环境？

这就是为什么我们向云提供商支付很多费用的原因。当我们为实例放置数据库容器时，上面说的这些便利性就不存在了。因为数据不匹配，新实例不会与现有的实例兼容，如果要限制实例使用单机服务，应该让 `DB` 使用非容器化环境，我们仅仅需要为计算服务层保留弹性扩展的能力。

#### 1.1.7.运行数据库的环境需求
常看到 `DBMS` 容器和其他服务运行在同一主机上。然而这些服务对硬件要求是非常不同的。

数据库（特别是关系型数据库）对 `IO` 的要求较高。一般数据库引擎为了避免并发资源竞争而使用专用环境。如果将你的数据库放在容器中，那么将浪费你的项目的资源。因为你需要为该实例配置大量额外的资源。在公有云，当你需要 34G 内存时，你启动的实例却必须开 64G 内存。在实践中，这些资源并未完全使用。

怎么解决？您可以分层设计，并使用固定资源来启动不同层次的多个实例。水平伸缩总是比垂直伸缩更好。

### 1.2.总结
针对上面问题是不是说数据库一定不要部署在容器里吗？

答案是：并不是

我们可以把数据丢失不敏感的业务（搜索、埋点）就可以数据化，利用数据库分片来来增加实例数，从而增加吞吐量。

docker适合跑轻量级或分布式数据库，当docker服务挂掉，会自动启动新容器，而不是继续重启容器服务。

数据库利用中间件和容器化系统能够自动伸缩、容灾、切换、自带多个节点，也是可以进行容器化的。