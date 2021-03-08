[toc]

# MySQL 锁

## 为什么要加锁

> 锁机制用于管理对共享资源的并发访问。

当多个用户并发地存取数据时，在数据库中就可能会产生多个事务同时操作同一行数据的情况，若对并发操作不加控制就可能会读取和存储不正确的数据，破坏数据的一致性。

一种典型的并发问题——丢失更新（其他锁问题及解决方法会在后面说到）：

> 注：RR默认隔离级别下，为更清晰体现时间先后，暂时忽略锁等待，不影响最终效果~

| 时间点 | 事务A                                                        | 事务B                                                        |
| ------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1      | 开启事务A                                                    |                                                              |
| 2      |                                                              | 开启事务B                                                    |
| 3      | 查询当前商品S库存为100                                       |                                                              |
| 4      |                                                              | 查询当前商品S库存为100                                       |
| 5      | 业务逻辑处理，确定要将商品S库存增加10，故更新库存为110（update stock set amount=110 where sku_id=S;） |                                                              |
| 6      |                                                              | 业务逻辑处理，确定要将商品S库存增加20，故更新库存为120（update stock set amount=120 where sku_id=S;） |
| 7      | 提交事务A                                                    |                                                              |
| 8      |                                                              | 提交事务B                                                    |

**异常结果：**商品S库存更新为120，但实际上针对商品S进行了两次入库操作，最终商品S库存应为100+10+20=130，但实际结果为120，首先提交的事务A的更新『丢失了』！！！所以就需要锁机制来保证这种情况不会发生。



## Mysql锁的概念与特性

在Mysql数据库系统中,不同的存储引擎支持不同的锁机制。比如MyISAM和MEMORY存储引擎采用的表级锁（table-level locking），BDB采用的是页面锁（page-level locking），也支持表级锁，InnoDB存储引擎既支持行级锁（row-level locking），也支持表级锁，默认情况下采用行级锁。

MySQL这3种锁的特性可大致归纳如下:

| 模式   | 开锁、加锁速度、死锁、粒度、并发性能                         |
| ------ | ------------------------------------------------------------ |
| 表级锁 | 开销小，加锁快；不会出现死锁；锁定粒度大，发生锁冲突的概率最高,并发度最低。 |
| 行级锁 | 开销大，加锁慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低,并发度也最高。 |
| 页面锁 | 开销和加锁时间界于表锁和行锁之间；会出现死锁；锁定粒度界于表锁和行锁之间，并发度一般。 |

从上述特点可见，很难笼统地说哪种锁更好，只能就具体应用的特点来说哪种锁更合适！仅从锁的角度来说：表级锁更适合于以查询为主，只有少量按索引条件更新数据的应用，如Web应用；而行级锁则更适合于有大量按索引条件并发更新少量不同数据，同时又有 并发查询的应用，如一些在线事务处理（OLTP）系统。



## MyISAM 锁

### 一、MyISAM表锁模式

![image-20210308232003088](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308232003088.png)

MySQL的表级锁有两种模式：表共享读锁（Table Read Lock）和表独占写锁（Table Write Lock）。

#### 1、MyISAM Lock Read(共享读)

共享读:MyISAM表的读操作，不会阻塞其他用户对同一个表的读请求，但会阻塞对同一个表的写请求.

**操作命令:**

```
//加锁
lock table 表名 read
//解锁
unlock tables
```

**实战场景:**

```
clientA: 
    lock table roles read; //读锁
    select * from roles where id = 1; //查询成功
clientB: 
    select * from roles where id = 1; //查询成功
    update roles set name = 'root'; //卡住,等待锁释放
ClientA:
    unlock tables; //解锁
clientB: 
    update roles set name = 'root2'; //更新成功
```

![image-20210308232121636](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308232121636.png)

#### 2、MyISAM Lock Write(读占写)

独占写:MyISAM表的写操作，会阻塞其他用户对同一个表的读和写操作。

操作命令:

```
//加锁
lock table 表名 write
//解锁
unlock tables
```

实战场景:

```
clientA: 
    lock table roles write; //写锁
    select * from roles where id = 1; //查询成功
    update roles set name = 'admin' where id = 1; //更新成功
clientB: 
    select * from roles where id = 1; //卡住,等待锁释放
ClientA:
    unlock tables; //解锁
clientB: 
    select * from roles where id = 1; //查询成功
```

![image-20210308232212140](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308232212140.png)

### 二、表级锁争用情况分析

通过检查table_locks_waited(表锁等待,无法立即获得数据)和table_locks_immediate(立即获得锁地查询数目)状态变量分析系统上表锁争夺情况

```
show status like '%table_lock%'
```

![image-20210308232240473](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308232240473.png)
分析:如果Table_locks_waited 数值比较高,就说明存在着较严重的表级锁争用情况 ,性能有问题，并发高,需要优化.

### 三、关于MyISAM 锁调度

MyISAM存储引擎的读和写锁是互斥，读操作是串行的。

那么，一个进程请求某个MyISAM表的读锁，同时另一个进程也请求同一表的写锁，MySQL如何处理呢？

答案是写进程先获得锁。不仅如此，即使读进程先请求先到锁等待队列，写请求后到，写锁也会插到读请求之前！这是因为MySQL认为写请求一般比读请求重要。这也正是MyISAM表不太适合于有大量更新操作和查询操作应用的原因，因为，大量的更新操作会造成查询操作很难获得读锁，从而可能永远阻塞。这种情况有时可能会变得非常糟糕！幸好我们可以通过一些设置来调节MyISAM的调度行为。

- 通过指定启动参数low-priority-updates，使MyISAM引擎默认给予读请求以优先的权利。
- 通过执行命令SET LOW_PRIORITY_UPDATES=1，使该连接发出的更新请求优先级降低。
- 通过指定INSERT、UPDATE、DELETE语句的LOW_PRIORITY属性，降低该语句的优先级。

虽然上面3种方法都是要么更新优先，要么查询优先的方法，但还是可以用其来解决查询相对重要的应用（如用户登录系统）中，读锁等待严重的问题。

另外，MySQL也提供了一种折中的办法来调节读写冲突，即给系统参数max_write_lock_count设置一个合适的值，当一个表的读锁达到这个值后，MySQL变暂时将写请求的优先级降低，给读进程一定获得锁的机会。



## InnoDB 锁

### 一、InnoDB锁类型概述

![image-20210308230318517](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230318517.png)

简介（后面会分别详细说到）：

1. 乐观锁与悲观锁是两种并发控制的思想，可用于解决丢失更新问题：

   乐观锁会“乐观地”假定大概率不会发生并发更新冲突，访问、处理数据过程中不加锁，只在更新数据时再根据版本号或时间戳判断是否有冲突，有则处理，无则提交事务；

   悲观锁会“悲观地”假定大概率会发生并发更新冲突，访问、处理数据前就加排他锁，在整个数据处理过程中锁定数据，事务提交或回滚后才释放锁；

2. InnoDB支持多种锁粒度，默认使用行锁，锁粒度最小，锁冲突发生的概率最低，支持的并发度也最高，但系统消耗成本也相对较高；
3. 共享锁与排他锁是InnoDB实现的两种标准的行锁；
4. InnoDB有三种锁算法——记录锁、gap间隙锁、还有结合了记录锁与间隙锁的next-key锁，InnoDB对于行的查询加锁是使用的是next-key locking这种算法，一定程度上解决了幻读问题；
5. 意向锁是为了支持多种粒度锁同时存在；（1.0版本不重点介绍，如有兴趣可参看知乎推荐回答[https://www.zhihu.com/questio...](https://www.zhihu.com/question/51513268)）

### 二、行锁详解

InnoDB默认使用行锁，实现了两种标准的行锁——共享锁与排他锁；
![image-20210308230528011](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230528011.png)

**注意：**
1、除了显式加锁的情况，其他情况下的加锁与解锁都无需人工干预。
2、InnoDB所有的行锁算法都是基于索引实现的，锁定的也都是索引或索引区间（这一点会在`锁算法`中详细说到）；

**共享锁与排它锁兼容性示例（使用默认的RR隔离级别，图中数字从小到大标识操作执行先后顺序）：**

![image-20210308230636211](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230636211.png)

### 三、当前读与快照读

**1、当前读：**即加锁读，读取记录的最新版本，会加锁保证其他并发事务不能修改当前记录，直至获取锁的事务释放锁；

使用当前读的操作主要包括：显式加锁的读操作与插入/更新/删除等写操作，如下所示：

```
select * from table where ? lock in share mode;
select * from table where ? for update;
insert into table values (…);
update table set ? where ?;
delete from table where ?;
```

> 注：当Update SQL被发给MySQL后，MySQL Server会根据where条件，读取第一条满足条件的记录，然后InnoDB引擎会将第一条记录返回，并加锁，待MySQL Server收到这条加锁的记录之后，会再发起一个Update请求，更新这条记录。一条记录操作完成，再读取下一条记录，直至没有满足条件的记录为止。因此，Update操作内部，就包含了当前读。同理，Delete操作也一样。Insert操作会稍微有些不同，简单来说，就是Insert操作可能会触发Unique Key的冲突检查，也会进行一个当前读。

**2、快照读**：即不加锁读，读取记录的快照版本而非最新版本，通过MVCC实现；

InnoDB默认的RR事务隔离级别下，不显式加『lock in share mode』与『for update』的『select』操作都属于快照读，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；

### 四、MVCC

> MVCC『多版本并发控制』，与之对应的是『基于锁的并发控制』；

MVCC的最大好处：读不加任何锁，读写不冲突，对于读操作多于写操作的应用，极大的增加了系统的并发性能；

InnoDB默认的RR事务隔离级别下，不显式加『lock in share mode』与『for update』的『select』操作都属于快照读，使用MVCC，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；

> 关于InnoDB MVCC的实现原理，在《高性能Mysql》一书中有一些说明，网络上也大多沿用这一套理论，但这套理论与InnoDB的实际实现还是有一定差距的，但不妨我们通过它初步理解MVCC的实现机制，所以我在此贴上此书中的说明；

![4132383459-5ac1c82d92d75_articlex](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210308230801.png)

### 五、锁算法

InnoDB主要实现了三种行锁算法：
![image-20210308230856792](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230856792.png)

InnoDB所有的行锁算法都是基于索引实现的，锁定的也都是索引或索引区间；

不同的事务隔离级别、不同的索引类型、是否为等值查询，使用的行锁算法也会有所不同；下面仅以InnoDB默认的RR隔离级别、等值查询为例，介绍几种行锁算法：
![image-20210308230917442](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230917442.png)

**1、等值查询使用聚簇索引**
![image-20210308230938553](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230938553.png)

> 注： InnoDB表是索引组织表，根据主键索引构造一棵B+树，叶子节点存放的是整张表的行记录数据，且按主键顺序存放；我这里做了一个表格模拟主键索引的叶子节点，使用主键索引查询，就会锁住相关主键索引，锁住了索引也就锁住了行记录，其他并发事务就无法修改此行数据，直至提交事务释放锁，保证了并发情况下数据的一致性；

**2、等值查询使用唯一索引**
![这里写图片描述](https://segmentfault.com/img/remote/1460000014133589)

> 注：辅助索引的叶子节点除了存放辅助索引值，也存放了对应主键索引值；锁定时会锁定辅助索引与主键索引；

**3、等值查询使用辅助索引**
![image-20210308230954750](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230954750.png)

> 注：Gap锁，锁定的是索引记录之间的间隙，是防止幻读的关键；如果没有上图中绿色标识的Gap Lock，其他并发事务在间隙中插入了一条记录如：『insert into stock (id,sku_id) values(2,103);』并提交，那么在此事务中重复执行上图中SQL，就会查询出并发事务新插入的记录，即出现幻读；（幻读是指在同一事务下，连续执行两次同样的SQL语句可能导致不同的结果，第二次的SQL语句可能返回之前不存在的行记录）加上Gap Lock后，并发事务插入新数据前会先检测间隙中是否已被加锁，防止幻读的出现；

更多锁示例可参看博客：[https://yq.aliyun.com/article...](https://yq.aliyun.com/articles/108095?t=t1)
更多锁算法详解可参看何博士博客：http://hedengcheng.com/?p=771

### 六、锁问题

MySQL锁会带来如下几种问题，如果能解决他们，就可以保证并发情况下不会出现问题；

| 锁问题     | 锁问题描述                                                   | 会出现锁问题的隔离级别                            | 解决办法                                                     |
| ---------- | ------------------------------------------------------------ | ------------------------------------------------- | ------------------------------------------------------------ |
| 脏读       | 一个事务中会读到其他并发事务未提交的数据，违反了事务的隔离性； | Read Uncommitted                                  | 提高事务隔离级别至Read Committed及以上；                     |
| 不可重复读 | 一个事务会读到其他并发事务已提交的数据，违反了数据库的一致性要求；可能出现的问题为幻读，幻读是指在同一事务下，连续执行两次同样的SQL语句可能导致不同的结果，第二次的SQL语句可能返回之前不存在的行记录； | Read Uncommitted、Read Committed                  | 默认的RR隔离级别下 ，解决办法分为两种情况：1、当前读：Next-Key Lock机制对相关索引记录及索引间隙加锁，防止并发事务修改数据或插入新数据到间隙；（详情参见第六章节『锁算法』）2、版本读：MVCC，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；提高事务隔离级别至Serializable； |
| 丢失更新   | 见章节一中描述；                                             | Read Uncommitted、Read Committed、Repeatable Read | 默认的RR隔离级别下 ，解决办法分为两种情况：1、乐观锁：数据表增加version字段，读取数据时记录原始version，更新数据时，比对version是否为原始version，如不等，则证明有并发事务已更新过此行数据，则可回滚事务后重试直至无并发竞争；2、悲观锁：读加排他锁，保证整个事务执行过程中，其他并发事务无法读取相关记录，直至当前事务提交或回滚释放锁； |

> 注：其实InnoDB默认的RR事务隔离级别已经为我们做了大多数的事，业务中更多需要关心『丢失更新』这种问题，通常使用乐观锁方式解决；我们在读操作时一般不会使用加锁读，但MVCC并不能完全解读幻读问题，其他并发事务是可以插入符合当前事务查询条件的数据，只是当前事务因为读快照数据无法查看到，这种情况下应该使用唯一索引等方式保证不会重复插入重复的业务数据，在此不再赘述~







