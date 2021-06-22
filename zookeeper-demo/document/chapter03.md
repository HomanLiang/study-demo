[toc]



# Zookeeper系统模型

## 1.数据节点（Znode）

在 `Zookeeper` 中，数据信息被保存在一个个数据节点上，这些节点被称为 `ZNode`。`ZNode` 是 `zookeeper` 中的最小数据单位，在`ZNode` 下面又可以再挂 `ZNode`，一层层形成一个层次化命名空间的 `ZNode` 树，我们称之为 `ZNode Tree`，它采取了类似文件系统的层级树状结构进行管理。

`ZooKeeper` 拥有一个层次的命名空间，这个和标准的文件系统非常相似，如下图

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313133744.png)

从图中我们可以看出 `ZooKeeper` 的数据模型，在结构上和标准文件系统的非常相似，都是采用这种树形层次结构，`ZooKeeper` 树中的每个节点被称为— `Znode`。和文件系统的目录树一样，`ZooKeeper` 树中的每个节点可以拥有子节点。但也有不同之处：

1. **引用方式**

   `Zonde` 通过路径引用，如同 `Unix` 中的文件路径。路径必须是绝对的，因此他们必须由斜杠字符来开头。除此以外，他们必须是唯一的，也就是说每一个路径只有一个表示，因此这些路径不能改变。在 `ZooKeeper` 中，路径由 `Unicode` 字符串组成，并且有一些限制。字符串 `/zookeeper` 用以保存管理信息，比如关键配额信息。

2. **Znode结构**

   `ZooKeeper` 命名空间中的 `Znode`，兼具文件和目录两种特点。既像文件一样维护着数据、元信息、ACL、时间戳等数据结构，又像目录一样可以作为路径标识的一部分。图中的每个节点称为一个 `Znode`。 每个 `Znode` 由3部分组成:

   - `stat`：此为状态信息, 描述该 `Znode` 的版本, 权限等信息
   - `data`：与该 `Znode` 关联的数据
   - `children`：该 `Znode` 下的子节点

   `ZooKeeper` 虽然可以关联一些数据，但并没有被设计为常规的数据库或者大数据存储，相反的是，它用来管理调度数据，比如分布式应用中的配置文件信息、状态信息、汇集位置等等。这些数据的共同特性就是它们都是很小的数据，通常以 `KB` 为大小单位。

   `ZooKeeper` 的服务器和客户端都被设计为严格检查并限制每个 `Znode` 的数据大小至多 `1M`，但常规使用中应该远小于此值。

3. **数据访问**

    `ZooKeeper` 中的每个节点存储的数据要被原子性的操作。也就是说读操作将获取与节点相关的所有数据，写操作也将替换掉节点的所有数据。另外，每一个节点都拥有自己的 `ACL` (访问控制列表)，这个列表规定了用户的权限，即限定了特定用户对目标节点可以执行的操作。

4. **节点类型**

    `ZooKeeper` 中的节点有两种，分别为临时节点和永久节点。节点的类型在创建时即被确定，并且不能改变。

    - 临时节点：该节点的生命周期依赖于创建它们的会话。一旦会话(`Session`)结束，临时节点将被自动删除，当然可以也可以手动删除。虽然每个临时的 `Znode` 都会绑定到一个客户端会话，但他们对所有的客户端还是可见的。另外，`ZooKeeper` 的临时节点不允许拥有子节点。
    - 永久节点：该节点的生命周期不依赖于会话，并且只有在客户端显示执行删除操作的时候，他们才能被删除。

    - 顺序节点：当创建 `Znode` 的时候，用户可以请求在 `ZooKeeper` 的路径结尾添加一个递增的计数。这个计数对于此节点的父节点来说是唯一的，它的格式为 `%10d` (10位数字，没有数值的数位用0补充，例如"0000000001")。当计数值大于232-1时，计数器将溢出。

5. **观察**

   客户端可以在节点上设置 `watch`，我们称之为监视器。当节点状态发生改变时(`Znode` 的增、删、改)将会触发 `watch` 所对应的操作。当 `watch` 被触发时，`ZooKeeper` 将会向客户端发送且仅发送一条通知，因为 `watch` 只能被触发一次，这样可以减少网络流量。



## 2.ZooKeeper中的时间

`ZooKeeper` 有多种记录时间的形式，其中包含以下几个主要属性：

1. **Zxid**
	
	致使 `ZooKeeper` 节点状态改变的每一个操作都将使节点接收到一个 `Zxid` 格式的时间戳，并且这个时间戳全局有序。也就是说，也就是说，每个对节点的改变都将产生一个唯一的 `Zxid`。如果 `Zxid1` 的值小于 `Zxid2` 的值，那么 `Zxid1` 所对应的事件发生在 `Zxid2` 所对应的事件之前。实际上，`ZooKeeper` 的每个节点维护者三个 `Zxid` 值，为别为：`cZxid`、`mZxid`、`pZxid`。

	- `cZxid`： 是节点的创建时间所对应的 `Zxid` 格式时间戳。

	- `mZxid`：是节点的修改时间所对应的 `Zxid` 格式时间戳。
	

实现中 `Zxid` 是一个 `64` 为的数字，它高 `32` 位是 `epoch` 用来标识 `leader` 关系是否改变，每次一个 `leader` 被选出来，它都会有一个 新的 `epoch`。低 `32` 位是个递增计数。 
	
2. **版本号**

   对节点的每一个操作都将致使这个节点的版本号增加。每个节点维护着三个版本号，他们分别为：

   - `version`：节点数据版本号
   - `cversion`：子节点版本号
   - `aversion`：节点所拥有的ACL版本号



## 3.事务ID

在 `Zookeeper` 中，事务是指能够改变 `Zookeeper` 服务器状态的操作，我们也称之为事务操作或更新操作，一般包括数据节点的创建与删除、数据节点内容更新等操作。对于每一个事务请求，`Zookeeper` 都会为其分配一个全局唯一的事务 `ID`，用 `ZXID` 来表示，通常是一个 `64` 位的数字。每一个 `ZXID` 对于一次更新操作。



## 4.ZNode的状态信息

![image-20210313140020758](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313140020.png)

整个 `ZNode` 节点内容包括两个部分：

1. 节点数据内容：上图 `quota` 是数据内容
2. 节点状态信息：除 `quota` 意外全是状态信息

| 属性 | 全称 | 含义 |
| ---- | ---- | ---- |
|cZxid	|Create ZXID	|节点被创建时的事务ID|
|ctime	|Create Time	|节点创建时间|
|mZxid	|Modified ZXID	|节点最后一次被修改时的事务ID|
|mtime	|Modified Time	|节点最后一次被修改的时间|
|pZxid	|/	|节点的子节点列表最后一次被修改时的事务ID。只有子节点列表变更才会更新，子节点内容变更时不会更新|
|version	|/	| 节点被修改的版本号                                           |
|cversion	|/	|子节点版本号|
|dataVersion	|/	|内容版本号|
|aclVersion	|/	|ACL版本|
|ephemeralOwner	|/	|创建该临时节点时的花花sessionID，如果时持节性节点那么值为0|
|dataLength	|/	|数据长度|
|numChildren	|/	|直系子节点数|



##  5.事件监听器（Watcher）

**5.1.watch概述**

`ZooKeeper` 可以为所有的读操作设置 `watch`，这些读操作包括：`exists()`、`getChildren()` 及 `getData()`。`watch` 事件是一次性的触发器，当 `watch` 的对象状态发生改变时，将会触发此对象上 `watch` 所对应的事件。`watch` 事件将被异步地发送给客户端，并且`ZooKeeper` 为 `watch` 机制提供了有序的一致性保证。理论上，客户端接收 `watch` 事件的时间要快于其看到 `watch` 对象状态变化的时间。

**5.2.watch类型**

`ZooKeeper` 所管理的 `watch` 可以分为两类：

- 数据 `watch` (`data watches`)：`getData` 和 `exists` 负责设置数据 `watch`

- 孩子 `watch`(`child watches`)：`getChildren` 负责设置孩子 `watch`

我们可以通过操作返回的数据来设置不同的 `watch`：

- `getData` 和 `exists`：返回关于节点的数据信息

- `getChildren`：返回孩子列表

因此

- 一个成功的 `setData` 操作将触发 `Znode` 的数据 `watch`

- 一个成功的 `create` 操作将触发 `Znode` 的数据 `watch` 以及孩子 `watch`

- 一个成功的 `delete` 操作将触发 `Znode` 的数据 `watch` 以及孩子 `watch`

**5.3.watch注册与处触发**

`watch` 设置操作及相应的触发器如图下图所示：

![47f3f49183b527543ec15e89768b34de.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313141707.png)

`exists` 操作上的 `watch`，在被监视的 `Znode` 创建、删除或数据更新时被触发。

`getData` 操作上的 `watch`，在被监视的 `Znode` 删除或数据更新时被触发。在被创建时不能被触发，因为只有 `Znode` 一定存在，`getData` 操作才会成功。

`getChildren` 操作上的 `watch`，在被监视的 `Znode` 的子节点创建或删除，或是这个 `Znode` 自身被删除时被触发。可以通过查看 `watch` 事件类型来区分是 `Znode`，还是他的子节点被删除：`NodeDelete` 表示 `Znode` 被删除，`NodeDeletedChanged` 表示子节点被删除。

`Watch` 由客户端所连接的 `ZooKeeper` 服务器在本地维护，因此 `watch` 可以非常容易地设置、管理和分派。当客户端连接到一个新的服务器时，任何的会话事件都将可能触发 `watch`。另外，当从服务器断开连接的时候，`watch` 将不会被接收。但是，当一个客户端重新建立连接的时候，任何先前注册过的 `watch` 都会被重新注册。

**5.4.需要注意的几点**

`Zookeeper` 的 `watch` 实际上要处理两类事件：

- 连接状态事件(`type=None, path=null`) 

  这类事件不需要注册，也不需要我们连续触发，我们只要处理就行了。

- 节点事件

  节点的建立，删除，数据的修改。它是 `one time trigger`，我们需要不停的注册触发，还可能发生事件丢失的情况。

上面2类事件都在 `Watch` 中处理，也就是重载的 `process`(`Event event`)

节点事件的触发，通过函数 `exists`，`getData` 或 `getChildren` 来处理这类函数，有双重作用：

- 注册触发事件

- 函数本身的功能

函数的本身的功能又可以用异步的回调函数来实现,重载 `processResult()` 过程中处理函数本身的的功能。

**5.5.Watcher机制**

`Zookeeper` 使用 `Watcher` 机制实现分布式数据的发布/订阅功能

`Zookeeper` 允许客户端向服务端注册一个 `Watcher` 监听，当服务端的一些指定事件触发了这个

`Watcher`，那么就会向指定客户端发送一个时间通知来实现分布式的通知功能。

![image-20210313140515831](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313140515.png)

`Zookeeper` 的 `Watcher` 机制主要包括 **客户端线程**、**客户端WatcherManager**、**Zookeeper服务器三部分**。

具体工作流程如下：

1. 客户端在向 `Zookeeper` 服务器注册的同时，会将 `Watcher` 对象存储在客户端的 `WatcherManager` 当中
2. 当 `Zookeeper` 服务器触发 `Watcher` 事件后，向客户端发送通知
3. 客户端线程从 `WatcherManager` 中取出对应的 `Watcher` 对象来执行回调逻辑



## 6.ACL—保障数据的安全

`Zookeeper` 作为一个分布式协调框架，其内部存储了分布式系统运行时状态的元数据，这些元数据会直接影响基于Zookeeper进行构造的分布式系统的运行状态，一次 `Zookeeper` 中提供了一套完善的 `ACL`（`Access Control List`）权限控制机制来保障数据的安全。

### 6.1.权限模式：Scheme

权限模式用来确定权限验证过程中使用的检验策略

1. **IP**

   IP模式就是通过IP地址粒度来进行权限控制，如 `ip:192.168.0.110` 表示针对 `192.168.0.110` 这个 `IP` 地址，同时也支持按网段方式进行配置，如 `ip:192.168.0.1/24` 表示针对 `192.168.0.*` 这个网段进行权限控制。

2. **Digest**

   `Digest` 是最常用的权限控制模式，它使用 `username:password` 形式的权限标识来进行权限配置，便于区分不同应用来进行权限空直，当我们通过 `username:password` 形式来妹纸之后，`Zookeeper` 会先对其进行 `SHA-1` 加密和 `BASE64` 编码。

3. **World**

   `World` 是一种最开放的权限控制模式，这种权限控制方式几乎没有任何作用，数据节点的访问权限是对全部用户进行开放的，同时`World` 模式也是一种特殊的 `Digest` 模式，即 `world:anyone`

4. **Super**

   超级用户模式，也是一种特殊的 `Digest` 模式，在 `Super` 模式下，超级用户可以对任意 `Zookeeper`上的数据节点进行任何操作

### 6.2.授权对象:ID

授权对象是指权限赋予的用户或一个指定实体，例如IP地址或者机器等。

|    权限模式  |   授权对象   |
| ---- | ---- |
|   IP   |    通常是一个IP地址或地址段，如:192.168.0.110或192.168.0.1/24  |
|Digest	|自定义，通常是username:BASE64(SHA-1(username:password)),例如：zm:sdfndsllndlksfn7c=|
|World	|只有一个ID:anyone|
|Super	|超级用户|

### 6.3.权限

权限就是指哪些通过权限检查后被允许执行的操作，`Zookeeper` 中又五大类权限

|标识符|	含义	|描述|
| ---- | ---- | ---- |
|C	|CREATE|	数据节的创建权限，允许授权对象在该数据节点下创建子节点|
|D	|DELETE	|子节点的删除权限，允许授权对象删除该数据节点的子节点|
|R	|READ|	数据节点的读取权限，允许授权对象访问该节点并读取其数据内容和子节点列表等|
|W	|WRITE	|数据节点的更新权限，允许授权对象访问该节点进行更新操作|
|A|	ADMIN|	数据节点的管理权限，允许授权对象对该数据节点进行ACL相关的设置操作|