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



### 表锁的加锁规则如下：

- 对于读锁
  - 持有读锁的会话可以读表，但不能写表；
  - 允许多个会话同时持有读锁；
  - 其他会话就算没有给表加读锁，也是可以读表的，但是不能写表；
  - 其他会话申请该表写锁时会阻塞，直到锁释放。
- 对于写锁
  - 持有写锁的会话既可以读表，也可以写表；
  - 只有持有写锁的会话才可以访问该表，其他会话访问该表会被阻塞，直到锁释放；
  - 其他会话无论申请该表的读锁或写锁，都会阻塞，直到锁释放。



### 表锁的释放规则如下：

- 使用 UNLOCK TABLES 语句可以显示释放表锁；
- 如果会话在持有表锁的情况下执行 LOCK TABLES 语句，将会释放该会话之前持有的锁；
- 如果会话在持有表锁的情况下执行 START TRANSACTION 或 BEGIN 开启一个事务，将会释放该会话之前持有的锁；
- 如果会话连接断开，将会释放该会话所有的锁。





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

### 六、锁问题

MySQL锁会带来如下几种问题，如果能解决他们，就可以保证并发情况下不会出现问题；

| 锁问题     | 锁问题描述                                                   | 会出现锁问题的隔离级别                            | 解决办法                                                     |
| ---------- | ------------------------------------------------------------ | ------------------------------------------------- | ------------------------------------------------------------ |
| 脏读       | 一个事务中会读到其他并发事务未提交的数据，违反了事务的隔离性； | Read Uncommitted                                  | 提高事务隔离级别至Read Committed及以上；                     |
| 不可重复读 | 一个事务会读到其他并发事务已提交的数据，违反了数据库的一致性要求；可能出现的问题为幻读，幻读是指在同一事务下，连续执行两次同样的SQL语句可能导致不同的结果，第二次的SQL语句可能返回之前不存在的行记录； | Read Uncommitted、Read Committed                  | 默认的RR隔离级别下 ，解决办法分为两种情况：1、当前读：Next-Key Lock机制对相关索引记录及索引间隙加锁，防止并发事务修改数据或插入新数据到间隙；（详情参见第六章节『锁算法』）2、版本读：MVCC，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；提高事务隔离级别至Serializable； |
| 丢失更新   | 见章节一中描述；                                             | Read Uncommitted、Read Committed、Repeatable Read | 默认的RR隔离级别下 ，解决办法分为两种情况：1、乐观锁：数据表增加version字段，读取数据时记录原始version，更新数据时，比对version是否为原始version，如不等，则证明有并发事务已更新过此行数据，则可回滚事务后重试直至无并发竞争；2、悲观锁：读加排他锁，保证整个事务执行过程中，其他并发事务无法读取相关记录，直至当前事务提交或回滚释放锁； |

> 注：其实InnoDB默认的RR事务隔离级别已经为我们做了大多数的事，业务中更多需要关心『丢失更新』这种问题，通常使用乐观锁方式解决；我们在读操作时一般不会使用加锁读，但MVCC并不能完全解读幻读问题，其他并发事务是可以插入符合当前事务查询条件的数据，只是当前事务因为读快照数据无法查看到，这种情况下应该使用唯一索引等方式保证不会重复插入重复的业务数据，在此不再赘述~





## MySQL 锁种类

MySQL InnoDB存储引擎提供了如下几种锁：

1. **共享/排他锁（S/X锁）**

   共享锁（S Lock）：允许事务读取一行数据，多个事务可以拿到一把S锁（即读读并行）；

   排他锁（X Lock）：允许事务删除或更新一行数据，多个事务有且只有一个事务可以拿到X锁（即写写/写读互斥）；

2. **意向锁（Intention Lock）** 
   意向锁是一种表级别的锁，意味着事务在更细的粒度上进行加锁。

   意向共享锁（IS Lock）：事务想要获得一张表中某几行的共享锁；

   意向排他锁（IX Lock）：事务想要获得一张表中某几行的排他锁；

   举个例子，事务1在表1上加了S锁后，事务2想要更改某行记录，需要添加IX锁，由于不兼容，所以需要等待S锁释放；如果事务1在表1上加了IS锁，事务2添加的IX锁与IS锁兼容，就可以操作，这就实现了更细粒度的加锁。

3. **插入意向锁（Insert Intention Lock）** 
   插入意向锁是间隙锁的一种，专门针对insert操作的。即多个事务在同一个索引、同一个范围区间内插入记录时，如果插入的位置不冲突，则不会阻塞彼此； 
   举个例子：在可重复读隔离级别下，对PK ID为10-20的数据进行操作： 
   事务1在10-20的记录中插入了一行： 
   insert into table value(11, xx) 
   事务2在10-20的记录中插入了一行： 
   insert into table value(12, xx) 
   由于两条插入的记录不冲突，所以会使用插入意向锁，且事务2不会被阻塞。

4. **自增锁（Auto-inc Locks）** 
   自增锁是一种特殊的表级别锁，专门针对事务插入AUTO-INCREMENT类型的列。 
   即一个事务正在往表中插入记录时，其他事务的插入必须等待，以便第1个事务插入的行得到的主键值是连续的。 
   举个例子：在可重复读隔离级别下，PK ID为自增主键 
   表中已有主键ID为1、2、3的3条记录。 
   事务1插入了一行： 
   insert into table value('aa') 
   得到一条（4,’aa’）的记录，未提交；

   此时 
   事务2中插入了一行： 
   insert into table value('bb') 
   这时会被阻塞，即用到了插入意向锁的概念。

5. **记录锁（Record Locks）- locks rec but not gap** 
   记录锁是的单个行记录上的锁，会阻塞其他事务对其插入、更新、删除；

6. **间隙锁(Gap Lock)** 
   间隙锁锁定记录的一个间隔，但不包含记录本身。 
   举个例子： 
   假如数据库已有ID为1、6两条记录， 
   现在想要在ID in （4，10）之间更新数据的时候，会加上间隙锁，锁住[4,5] [7,10] ,(不包含已有记录ID=5本身) 
   那么在更新ID=5的记录（只有一条记录）符合条件； 
   如果不加间隙锁，事务2有可能会在4、10之间插入一条数据，这个时候事务1再去更新，发现在(4,10)这个区间内多出了一条“幻影”数据。
   间隙锁就是防止其他事务在间隔中插入数据，以导致“不可重复读”。

7. **临键锁（Next-Key Lock）= Gap Lock + Record Lock** 
   临建锁是记录锁与间隙锁的组合，即：既包含索引记录，又包含索引区间，主要是为了解决幻读。



## 案例分析

###  一、记一次神奇的Mysql死锁排查

#### 问题初现

在某天下午，突然系统报警，抛出个异常:

![image-20210309214413908](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309214413908.png)

仔细一看好像是事务回滚异常，写着的是因为死锁回滚，原来是个死锁问题，由于我对Mysql锁还是有一定了解的，于是开始主动排查这个问题。

首先在数据库中查找Innodb Status，在Innodb Status中会记录上一次死锁的信息,输入下面命令:

```
SHOW ENGINE INNODB STATUS
```

死锁信息如下，SQL 信息进行了简单处理:

```
------------------------
LATEST DETECTED DEADLOCK
------------------------
2019-02-22 15:10:56 0x7eec2f468700
*** (1) TRANSACTION:
TRANSACTION 2660206487, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 2 lock struct(s), heap size 1136, 1 row lock(s)
MySQL thread id 31261312, OS thread handle 139554322093824, query id 11624975750 10.23.134.92 erp_crm__6f73 updating
/*id:3637ba36*/UPDATE tenant_config SET
       open_card_point =  0
       where tenant_id = 123
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 1322 page no 534 n bits 960 index uidx_tenant of table `erp_crm_member_plan`.`tenant_config` trx id 2660206487 lock_mode X locks rec but not gap waiting

*** (2) TRANSACTION:
TRANSACTION 2660206486, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
3 lock struct(s), heap size 1136, 2 row lock(s)
MySQL thread id 31261311, OS thread handle 139552870532864, query id 11624975758 10.23.134.92 erp_crm__6f73 updating
/*id:3637ba36*/UPDATE tenant_config SET
       open_card_point =  0
       where tenant_id = 123
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 1322 page no 534 n bits 960 index uidx_tenant of table `erp_crm_member_plan`.`tenant_config` trx id 2660206486 lock mode S
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 1322 page no 534 n bits 960 index uidx_tenant of table `erp_crm_member_plan`.`tenant_config` trx id 2660206486 lock_mode X locks rec but not gap waiting
*** WE ROLL BACK TRANSACTION (1)
------------
```

给大家简单的分析解释一下这段死锁日志:

事务1执行Update语句的时候需要获取uidx_tenant这个索引再where条件上的X锁(行锁)

事务2执行同样的Update语句，也在uidx_tenant上面想要获取X锁(行锁)，然后就出现了死锁，回滚了事务1。

当时我就很懵逼，回想了一下死锁产生的必要条件:

1. 互斥。
2. 请求与保持条件。
3. 不剥夺条件。
4. 循环等待。 从日志上来看事务1和事务2都是取争夺同一行的行锁，和以往的互相循环争夺锁有点不同，怎么看都无法满足循环等待条件。

经过同事提醒，既然从死锁日志中不能进行排查，那么就只能从业务代码和业务日志从排查。这段代码的逻辑如下:

```
public int saveTenantConfig(PoiContext poiContext, TenantConfigDO tenantConfig) { 
	try { 
		return tenantConfigMapper.saveTenantConfig(poiContext.getTenantId(), poiContext.getPoiId(), tenantConfig); 
	} catch (DuplicateKeyException e) { 
		LOGGER.warn("[saveTenantConfig] 主键冲突，更新该记录。context:{}, config:{}", poiContext, tenantConfig); 
		return tenantConfigMapper.updateTenantConfig(poiContext.getTenantId(), tenantConfig); 
	} 
}
```

这段代码的意思是保存一个配置文件，如果发生了唯一索引冲突那么就会进行更新，当然这里可能写得不是很规范，其实可以用

```
insert into ... 
on duplicate key update 
```

也可以达到同样的效果，但是就算用这个其实也会发生死锁。看了代码之后同事又给我发了当时业务日志,

![image-20210309214935834](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309214935834.png)

可以看见这里有三条同时发生的日志，说明都发生了唯一索引冲突进入了更新的语句，然后发生的死锁。到这里答案终于稍微有点眉目了。

这个时候再看我们的表结构如下(做了简化处理):

```mysql
CREATE TABLE `tenant_config` (
  `id` bigint(21) NOT NULL AUTO_INCREMENT,
  `tenant_id` int(11) NOT NULL,
  `open_card_point` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uidx_tenant` (`tenant_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT
```

我们的tenant_id是用来做唯一索引，我们的插入和更新的where条件都是基于唯一索引来操作的。

```mysql
UPDATE tenant_config SET
open_card_point =  0
where tenant_id = 123
```

到了这里感觉插入的时候对唯一索引加锁有关系，接下来我们进行下一步的深入剖析。

#### 深入剖析

上面我们说有三个事务进入update语句，为了简化说明这里我们只需要两个事务同时进入update语句即可，下面的表格展示了我们整个的发生过程:

| 时间线 | 事务1          | 事务2                                           | 事务3                                 |
| ------ | -------------- | ----------------------------------------------- | ------------------------------------- |
| 1      | insert into xx | insert into xx                                  | insert into xx                        |
| 2      | 获取当前行X锁  |                                                 |                                       |
| 3      |                | 需要检测唯一索引是否冲突获取S锁，阻塞           | 需要检测唯一索引是否冲突获取S锁，阻塞 |
| 4      | commit;        | 获取到到S锁                                     | 获取到S锁                             |
| 5      |                | 发现唯一索引冲突，执行Update语句(此时S锁未释放) | 发现唯一索引冲突，执行Update语句      |
| 6      |                | 获取该行的X锁，被事务3的S锁阻塞                 | 获取该行的X锁，被事务2的S锁阻塞       |
| 7      |                | 发现死锁，回滚该事务                            | update成功,commit;                    |

> 小提示:S锁是共享锁，X锁是互斥锁。一般来说X锁和S，X锁都互斥，S锁和S锁不互斥。

我们从上面的流程中看见发生这个死锁的关键需要获取S锁，为什么我们再插入的时候需要获取S锁呢？因为我们需要检测唯一索引？在RR隔离级别下如果要读取那么就是当前读,那么其实就需要加上S锁。这里发现唯一键已经存在，这个时候执行update就会被两个事务的S锁互相阻塞，从而形成上面的循环等待条件。

![image-20210309215416285](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309215416285.png)

> 小提示: 在MVCC中，当前读和快照读的区别:当前读每次需要加锁（可以使共享锁或者互斥锁）获取到最新的数据，而快照读是读取的是这个事务开始的时候那个快照，这个是通过undo log去进行实现的。

#### 解决方案

这里的核心问题是需要把S锁给干掉，这里有三个可供参考的解决方案:

- 将RR隔离级别，降低成RC隔离级别。这里RC隔离级别会用快照读，从而不会加S锁。
- 再插入的时候使用select * for update,加X锁，从而不会加S锁。
- 可以提前加上分布式锁，可以利用Redis,或者ZK等

第一种方法不太现实，毕竟隔离级别不能轻易的修改。第三种方法又比较麻烦。所以第二种方法是我们最后确定的。

#### 总结

说了这么多，最后做一个小小的总结吧。排查死锁这种问题的时候有时候光看死锁日志有时候会解决不了问题，需要结合整个的业务日志，代码以及表结构来进行分析，才能得到正确的结果。



### 二、mysql死锁问题分析

线上某服务时不时报出如下异常（大约一天二十多次）：“Deadlock found when trying to get lock;”。

Oh, My God! 是死锁问题。尽管报错不多，对性能目前看来也无太大影响，但还是需要解决，保不齐哪天成为性能瓶颈。

为了更系统的分析问题，本文将从死锁检测、索引隔离级别与锁的关系、死锁成因、问题定位这五个方面来展开讨论。

![image-20210309220607043](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309220607043.png)

 **图1 应用日志**

#### 1 死锁是怎么被发现的？

##### 1.1 死锁成因&&检测方法

左图那两辆车造成死锁了吗？不是！右图四辆车造成死锁了吗？是！

![image-20210309221113569](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221113569.png)

**图2 死锁描述**

我们mysql用的存储引擎是innodb，从日志来看，innodb主动探知到死锁，并回滚了某一苦苦等待的事务。问题来了，innodb是怎么探知死锁的？

直观方法是在两个事务相互等待时，当一个等待时间超过设置的某一阀值时，对其中一个事务进行回滚，另一个事务就能继续执行。这种方法简单有效，在innodb中，参数innodb_lock_wait_timeout用来设置超时时间。

仅用上述方法来检测死锁太过被动，innodb还提供了wait-for graph算法来主动进行死锁检测，每当加锁请求无法立即满足需要并进入等待时，wait-for graph算法都会被触发。

##### 1.2 wait-for graph原理

我们怎么知道上图中四辆车是死锁的？他们相互等待对方的资源，而且形成环路！我们将每辆车看为一个节点，当节点1需要等待节点2的资源时，就生成一条有向边指向节点2，最后形成一个有向图。我们只要检测这个有向图是否出现环路即可，出现环路就是死锁！这就是wait-for graph算法。
![image-20210309221217857](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221217857.png)                                              

**图3 wait for graph**

innodb将各个事务看为一个个节点，资源就是各个事务占用的锁，当事务1需要等待事务2的锁时，就生成一条有向边从1指向2，最后行成一个有向图。

#### 2 innodb隔离级别、索引与锁 

死锁检测是死锁发生时innodb给我们的救命稻草，我们需要它，但我们更需要的是避免死锁发生的能力，如何尽可能避免？这需要了解innodb中的锁。

##### 2.1 锁与索引的关系

假设我们有一张消息表（msg），里面有3个字段。假设id是主键，token是非唯一索引，message没有索引。

| id: bigint | token: varchar(30) | message: varchar(4096) |
| ---------- | ------------------ | ---------------------- |
|            |                    |                        |

   innodb对于主键使用了聚簇索引，这是一种数据存储方式，表数据是和主键一起存储，主键索引的叶结点存储行数据。对于普通索引，其叶子节点存储的是主键值。
![image-20210309221351414](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221351414.png)

**图4 聚簇索引和二级索引**

下面分析下索引和锁的关系。

1）delete from msg where id=2；

由于id是主键，因此直接锁住整行记录即可。

![image-20210309221525678](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221525678.png)                                        

2）delete from msg where token=’ cvs’;

由于token是二级索引，因此首先锁住二级索引（两行），接着会锁住相应主键所对应的记录；

![image-20210309221602592](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221602592.png)                                    

3）delete from msg where message=订单号是多少’；

message没有索引，所以走的是全表扫描过滤。这时表上的各个记录都将添加上X锁。

![image-20210309221643052](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221643052.png)                                    

##### 2.2 锁与隔离级别的关系

大学数据库原理都学过，为了保证并发操作数据的正确性，数据库都会有事务隔离级别的概念：

1）未提交读（Read uncommitted）；

2）已提交读（Read committed（RC））；

3）可重复读（Repeatable read（RR））；

4）可串行化（Serializable）。我们较常使用的是RC和RR。

提交读(RC)：只能读取到已经提交的数据。

可重复读(RR)：在同一个事务内的查询都是事务开始时刻一致的，InnoDB默认级别。

我们在2.1节谈论的其实是RC隔离级别下的锁，它可以防止不同事务版本的数据修改提交时造成数据冲突的情况，但当别的事务插入数据时可能会出现问题。

如下图所示，事务A在第一次查询时得到1条记录，在第二次执行相同查询时却得到两条记录。从事务A角度上看是见鬼了！这就是幻读，RC级别下尽管加了行锁，但还是避免不了幻读。

![166dde181aaa60227c726e653bf6d6d91e1594c5](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/166dde181aaa60227c726e653bf6d6d91e1594c5.png)                                   

innodb的RR隔离级别可以避免幻读发生，怎么实现？当然需要借助于锁了！

为了解决幻读问题，innodb引入了gap锁。

在事务A执行：`update msg set message=‘订单’ where token=‘asd’;`

innodb首先会和RC级别一样，给索引上的记录添加上X锁，此外，还在非唯一索引’asd’与相邻两个索引的区间加上锁。

这样，当事务B在执行 `insert into msg values (null,‘asd',’hello’); commit;` 时，会首先检查这个区间是否被锁上，如果被锁上，则不能立即执行，需要等待该gap锁被释放。这样就能避免幻读问题。
![image-20210309222041969](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222041969.png)                                      

#### 3 死锁成因

了解了innodb锁的基本原理后，下面分析下死锁的成因。如前面所说，死锁一般是事务相互等待对方资源，最后形成环路造成的。下面简单讲下造成相互等待最后形成环路的例子。

##### 3.1不同表相同记录行锁冲突

这种情况很好理解，事务A和事务B操作两张表，但出现循环等待锁情况。
![image-20210309222100709](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222100709.png)

**3.2 相同表记录行锁冲突**

这种情况比较常见，之前遇到两个job在执行数据批量更新时，jobA处理的的id列表为[1,2,3,4]，而job处理的id列表为[8,9,10,4,2]，这样就造成了死锁。

![image-20210309222154206](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222154206.png)

##### 3.3 不同索引锁冲突

这种情况比较隐晦，事务A在执行时，除了在二级索引加锁外，还会在聚簇索引上加锁，在聚簇索引上加锁的顺序是[1,4,2,3,5]，而事务B执行时，只在聚簇索引上加锁，加锁顺序是[1,2,3,4,5]，这样就造成了死锁的可能性。

![image-20210309222243979](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222243979.png)                                  

##### 3.4 gap锁冲突

innodb在RR级别下，如下的情况也会产生死锁，比较隐晦。不清楚的同学可以自行根据上节的gap锁原理分析下。
![image-20210309222343808](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222343808.png)                                        

#### 4 如何尽可能避免死锁

- 以固定的顺序访问表和行。比如对第2节两个job批量更新的情形，简单方法是对id列表先排序，后执行，这样就避免了交叉等待锁的情形；又比如对于3.1节的情形，将两个事务的sql顺序调整为一致，也能避免死锁。

- 大事务拆小。大事务更倾向于死锁，如果业务允许，将大事务拆小。
- 在同一个事务中，尽可能做到一次锁定所需要的所有资源，减少死锁概率。
- 降低隔离级别。如果业务允许，将隔离级别调低也是较好的选择，比如将隔离级别从RR调整为RC，可以避免掉很多因为gap锁造成的死锁。
- 为表添加合理的索引。可以看到如果不走索引将会为表的每一行记录添加上锁，死锁的概率大大增大。
- 设置锁等待超时参数：`innodb_lock_wait_timeout`，这个参数并不是只用来解决死锁问题，在并发访问比较高的情况下，如果大量事务因无法立即获得所需的锁而挂起，会占用大量计算机资源，造成严重性能问题，甚至拖跨数据库。我们通过设置合适的锁等待超时阈值，可以避免这种情况发生。

#### 5 如何定位死锁成因

下面以本文开头的死锁案例为例，讲下如何排查死锁成因。

1）通过应用业务日志定位到问题代码，找到相应的事务对应的sql；

因为死锁被检测到后会回滚，这些信息都会以异常反应在应用的业务日志中，通过这些日志我们可以定位到相应的代码，并把事务的sql给梳理出来。

```
start tran
1 deleteHeartCheckDOByToken
2 updateSessionUser
...
commit
```

此外，我们根据日志回滚的信息发现在检测出死锁时这个事务被回滚。

2）确定数据库隔离级别。

执行 `select @@global.tx_isolation`，可以确定数据库的隔离级别，我们数据库的隔离级别是RC，这样可以很大概率排除gap锁造成死锁的嫌疑;

3）找DBA执行下`show InnoDB STATUS`看看最近死锁的日志。

这个步骤非常关键。通过DBA的帮忙，我们可以有更为详细的死锁信息。通过此详细日志一看就能发现，与之前事务相冲突的事务结构如下：

```
start tran
1 updateSessionUser
2 deleteHeartCheckDOByToken
...
commit
```



### 三、解决死锁之路（终结篇） - 再见死锁

#### 开启锁监控

在遇到线上死锁问题时，我们应该第一时间获取相关的死锁日志。我们可以通过 `show engine innodb status` 命令来获取死锁信息，但是它有个限制，只能拿到最近一次的死锁日志。MySQL 提供了一套 InnoDb 的监控机制，用于周期性（每隔 15 秒）输出 InnoDb 的运行状态到 mysqld 服务的标准错误输出（stderr）。默认情况下监控是关闭的，只有当需要分析问题时再开启，并且在分析问题之后，建议将监控关闭，因为它对数据库的性能有一定影响，另外每 15 秒输出一次日志，会使日志文件变得特别大。

InnoDb 的监控主要分为四种：标准监控（Standard InnoDB Monitor）、锁监控（InnoDB Lock Monitor）、表空间监控（InnoDB Tablespace Monitor）和表监控（InnoDB Table Monitor）。后两种监控已经基本上废弃了，关于各种监控的作用可以参考 MySQL 的官方文档 [Enabling InnoDB Monitors](https://dev.mysql.com/doc/refman/5.6/en/innodb-enabling-monitors.html) 或者 [这篇文章](http://yeshaoting.cn/article/database/开启InnoDB监控/)。

要获取死锁日志，我们需要开启 InnoDb 的标准监控，我推荐将锁监控也打开，它可以提供一些额外的锁信息，在分析死锁问题时会很有用。开启监控的方法有两种：

##### 1. 基于系统表

MySQL 使用了几个特殊的表名来作为监控的开关，比如在数据库中创建一个表名为 `innodb_monitor` 的表开启标准监控，创建一个表名为 `innodb_lock_monitor` 的表开启锁监控。MySQL 通过检测是否存在这个表名来决定是否开启监控，至于表的结构和表里的内容无所谓。相反的，如果要关闭监控，则将这两个表删除即可。这种方法有点奇怪，在 5.6.16 版本之后，推荐使用系统参数的形式开启监控。

```
-- 开启标准监控
CREATE TABLE innodb_monitor (a INT) ENGINE=INNODB;
 
-- 关闭标准监控
DROP TABLE innodb_monitor;
 
-- 开启锁监控
CREATE TABLE innodb_lock_monitor (a INT) ENGINE=INNODB;
 
-- 关闭锁监控
DROP TABLE innodb_lock_monitor;
```

##### 2. 基于系统参数

在 MySQL 5.6.16 之后，可以通过设置系统参数来开启锁监控，如下：

```
-- 开启标准监控
set GLOBAL innodb_status_output=ON;
 
-- 关闭标准监控
set GLOBAL innodb_status_output=OFF;
 
-- 开启锁监控
set GLOBAL innodb_status_output_locks=ON;
 
-- 关闭锁监控
set GLOBAL innodb_status_output_locks=OFF;
```

另外，MySQL 提供了一个系统参数 `innodb_print_all_deadlocks` 专门用于记录死锁日志，当发生死锁时，死锁日志会记录到 MySQL 的错误日志文件中。

```
set GLOBAL innodb_print_all_deadlocks=ON;
```

除了 MySQL 自带的监控机制，还有一些有趣的监控工具也很有用，比如 [Innotop](http://yeshaoting.cn/article/database/命令行监控工具Innotop/) 和 Percona Toolkit 里的小工具 [pt-deadlock-logger](https://www.linuxidc.com/Linux/2014-03/97830.htm)。

#### 读懂死锁日志

一切准备就绪之后，我们从 DBA 那里拿到了死锁日志（其中的SQL语句做了省略）：

```
------------------------
LATEST DETECTED DEADLOCK
------------------------
2017-09-06 11:58:16 7ff35f5dd700
*** (1) TRANSACTION:
TRANSACTION 182335752, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 11 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 15
MySQL thread id 12032077, OS thread handle 0x7ff35ebf6700, query id 196418265 10.40.191.57 RW_bok_db update
INSERT INTO bok_task
                 ( order_id ...
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` 
    trx id 182335752 lock_mode X insert intention waiting
*** (2) TRANSACTION:
TRANSACTION 182335756, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
11 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 15
MySQL thread id 12032049, OS thread handle 0x7ff35f5dd700, query id 196418268 10.40.189.132 RW_bok_db update
INSERT INTO bok_task
                 ( order_id ...
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` 
    trx id 182335756 lock_mode X
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` 
    trx id 182335756 lock_mode X insert intention waiting
*** WE ROLL BACK TRANSACTION (2)
```

日志中列出了死锁发生的时间，以及导致死锁的事务信息（只显示两个事务，如果由多个事务导致的死锁也只显示两个），并显示出每个事务正在执行的 SQL 语句、等待的锁以及持有的锁信息等。下面我们就来研究下这份死锁日志，看看从这份死锁日志中能不能发现死锁的原因？

首先看事务一的信息：

> *** (1) TRANSACTION:
> TRANSACTION 182335752, ACTIVE 0 sec inserting

ACTIVE 0 sec 表示事务活动时间，inserting 为事务当前正在运行的状态，可能的事务状态有：fetching rows，updating，deleting，inserting 等。

> mysql tables in use 1, locked 1
> LOCK WAIT 11 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 15

tables in use 1 表示有一个表被使用，locked 1 表示有一个表锁。LOCK WAIT 表示事务正在等待锁，11 lock struct(s) 表示该事务的锁链表的长度为 11，每个链表节点代表该事务持有的一个锁结构，包括表锁，记录锁以及 autoinc 锁等。heap size 1184 为事务分配的锁堆内存大小。
2 row lock(s) 表示当前事务持有的行锁个数，通过遍历上面提到的 11 个锁结构，找出其中类型为 LOCK_REC 的记录数。undo log entries 15 表示当前事务有 15 个 undo log 记录，因为二级索引不记 undo log，说明该事务已经更新了 15 条聚集索引记录。

> MySQL thread id 12032077, OS thread handle 0x7ff35ebf6700, query id 196418265 10.40.191.57 RW_bok_db update

事务的线程信息，以及数据库 IP 地址和数据库名，对我们分析死锁用处不大。

> INSERT INTO bok_task
>
> ```
> ( order_id ...
> ```

这里显示的是正在等待锁的 SQL 语句，死锁日志里每个事务都只显示一条 SQL 语句，这对我们分析死锁很不方便，我们必须要结合应用程序去具体分析这个 SQL 之前还执行了哪些其他的 SQL 语句，或者根据 binlog 也可以大致找到一个事务执行的 SQL 语句。

> *** (1) WAITING FOR THIS LOCK TO BE GRANTED:

RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` trx id 182335752 lock_mode X insert intention waiting

这里显示的是事务正在等待什么锁。RECORD LOCKS 表示记录锁（并且可以看出要加锁的索引为 order_id_un），space id 为 300，page no 为 5480，n bits 552 表示这个记录锁结构上留有 552 个 bit 位（该 page 上的记录数 + 64）。
lock_mode X 表示该记录锁为排他锁，insert intention waiting 表示要加的锁为插入意向锁，并处于锁等待状态。

在上面有提到 `innodb_status_output_locks` 这个系统变量可以开启 InnoDb 的锁监控，如果开启了，这个地方还会显示出锁的一些额外信息，包括索引记录的 info bits 和数据信息等：

```
Record lock, heap no 2 PHYSICAL RECORD: n_fields 2; compact format; info bits 0`` ``0: len 4; hex 80000002; asc   ;;`` ``1: len 4; hex 80000001; asc   ;;
```

在 [《了解常见的锁类型》](https://www.aneasystone.com/archives/2017/11/solving-dead-locks-two.html) 中我们说过，一共有四种类型的行锁：记录锁，间隙锁，Next-key 锁和插入意向锁。这四种锁对应的死锁日志各不相同，如下：

- 记录锁（LOCK_REC_NOT_GAP）: lock_mode X locks rec but not gap
- 间隙锁（LOCK_GAP）: lock_mode X locks gap before rec
- Next-key 锁（LOCK_ORNIDARY）: lock_mode X
- 插入意向锁（LOCK_INSERT_INTENTION）: lock_mode X locks gap before rec insert intention

这里有一点要注意的是，并不是在日志里看到 lock_mode X 就认为这是 Next-key 锁，因为还有一个例外：如果在 supremum record 上加锁，`locks gap before rec` 会省略掉，间隙锁会显示成 `lock_mode X`，插入意向锁会显示成 `lock_mode X insert intention`。譬如下面这个：

```
RECORD LOCKS space id 0 page no 307 n bits 72 index `PRIMARY` of table `test`.`test` trx id 50F lock_mode X``Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
```

看起来像是 Next-key 锁，但是看下面的 `heap no 1` 表示这个记录是 supremum record（另外，infimum record 的 heap no 为 0），所以这个锁应该看作是一个间隙锁。

看完第一个事务，再来看看第二个事务：

> *** (2) TRANSACTION:

TRANSACTION 182335756, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
11 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 15
MySQL thread id 12032049, OS thread handle 0x7ff35f5dd700, query id 196418268 10.40.189.132 RW_bok_db update
INSERT INTO bok_task

```
( order_id ...
```

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` trx id 182335756 lock_mode X
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` trx id 182335756 lock_mode X insert intention waiting

事务二和事务一的日志基本类似，不过它多了一部分 HOLDS THE LOCK(S)，表示事务二持有什么锁，这个锁往往就是事务一处于锁等待的原因。这里可以看到事务二正在等待索引 order_id_un 上的插入意向锁，并且它已经持有了一个 X 锁（Next-key 锁，也有可能是 supremum 上的间隙锁）。

到这里为止，我们得到了很多关键信息，此时我们可以逆推出死锁发生的原因吗？这可能也是每个开发人员和 DBA 最关心的问题，如何通过死锁日志来诊断死锁的成因？实际上这是非常困难的。

如果每个事务都只有一条 SQL 语句，这种情况的死锁成因还算比较好分析，因为我们可以从死锁日志里找到每个事务执行的 SQL 语句，只要对这两条 SQL 语句的加锁过程有一定的了解，死锁原因一般不难定位。但也有可能死锁的成因非常隐蔽，这时需要我们对这两条 SQL 语句的加锁流程做非常深入的研究才有可能分析出死锁的根源。

不过大多数情况下，每个事务都不止一条 SQL 语句，譬如上面的死锁日志里显示的 `undo log entries 15`，说明执行 INSERT 语句之前肯定还执行了其他的 SQL 语句，但是具体是什么，我们不得而知，我们只能根据 HOLDS THE LOCK(S) 部分知道有某个 SQL 语句对 order_id_un 索引加了 Next-key 锁（或间隙锁）。另外事务二在 WAITING FOR 插入意向锁，至于它和事务一的哪个锁冲突也不得而知，因为事务一的死锁日志里并没有 HOLDS THE LOCK(S) 部分。

所以，对死锁的诊断不能仅仅靠死锁日志，还应该结合应用程序的代码来进行分析，如果实在接触不到应用代码，还可以通过数据库的 binlog 来分析（只要你的死锁不是 100% 必现，那么 binlog 日志里肯定能找到一份完整的事务一和事务二的 SQL 语句）。通过应用代码或 binlog 理出每个事务的 SQL 执行顺序，这样分析死锁时就会容易很多。

#### 常见死锁分析

尽管上面说通过死锁日志来推断死锁原因非常困难，但我想也不是完全不可能。我在 Github 上新建了一个项目 [mysql-deadlocks](https://github.com/aneasystone/mysql-deadlocks)，这个项目收集了一些常见的 MySQL 死锁案例，大多数案例都来源于网络，并对它们进行分类汇总，试图通过死锁日志分析出每种死锁的原因，还原出死锁现场。这虽然有点痴人说梦的感觉，但还是希望能给后面的开发人员在定位死锁问题时带来一些便利。

我将这些死锁按事务执行的语句和正在等待或已持有的锁进行分类汇总（目前已经收集了十余种死锁场景）：

![image-20210309231753281](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231753281.png)

表中的语句虽然只列出了 delete 和 insert，但实际上绝大多数的 delete 语句和 update 或 select ... for update 加锁机制是一样的，所以为了避免重复，对于 update 语句就不在一起汇总了（当然也有例外，譬如使用 update 对索引进行更新时加锁机制和 delete 是有区别的，这种情况我会单独列出）。

对每一个死锁场景，我都会定义一个死锁名称（实际上就是事务等待和持有的锁），每一篇分析，我都分成了 死锁特征、死锁日志、表结构、重现步骤、分析和参考 这几个部分。

下面我们介绍几种常见的死锁场景，还是以前面提到的 students 表为例：

![image-20210309231823404](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231823404.png)

其中，id 为主键，no（学号）为二级唯一索引，name（姓名）和 age（年龄）为二级非唯一索引，score（学分）无索引。数据库隔离级别为 RR。

##### 3.1 死锁案例一

![image-20210309231842684](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231842684.png)

死锁的根本原因是有两个或多个事务之间加锁顺序的不一致导致的，这个死锁案例其实是最经典的死锁场景。

首先，事务 A 获取 id = 20 的锁（lock_mode X locks rec but not gap），事务 B 获取 id = 30 的锁；然后，事务 A 试图获取 id = 30 的锁，而该锁已经被事务 B 持有，所以事务 A 等待事务 B 释放该锁，然后事务 B 又试图获取 id = 20 的锁，这个锁被事务 A 占有，于是两个事务之间相互等待，导致死锁。

##### 3.2 死锁案例二

![image-20210309231910096](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231910096.png)

首先事务 A 和事务 B 执行了两条 UPDATE 语句，但是由于 id = 25 和 id = 26 记录都不存在，事务 A 和 事务 B 并没有更新任何记录，但是由于数据库隔离级别为 RR，所以会在 (20, 30) 之间加上间隙锁（lock_mode X locks gap before rec），间隙锁和间隙锁并不冲突。之后事务 A 和事务 B 分别执行 INSERT 语句要插入记录 id = 25 和 id = 26，需要在 (20, 30) 之间加插入意向锁（lock_mode X locks gap before rec insert intention），插入意向锁和间隙锁冲突，所以两个事务互相等待，最后形成死锁。

要解决这个死锁很简单，显然，前面两条 UPDATE 语句是无效的，将其删除即可。另外也可以将数据库隔离级别改成 RC，这样在 UPDATE 的时候就不会有间隙锁了。这个案例正是文章开头提到的死锁日志中的死锁场景，别看这个 UPDATE 语句是无效的，看起来很傻，但是确实是真实的场景，因为在真实的项目中代码会非常复杂，比如采用了 ORM 框架，应用层和数据层代码分离，一般开发人员写代码时都不知道会生成什么样的 SQL 语句，我也是从 DBA 那里拿到了 binlog，然后从里面找到了事务执行的所有 SQL 语句，发现其中竟然有一行无效的 UPDATE 语句，最后追本溯源，找到对应的应用代码，将其删除，从而修复了这个死锁。

##### 3.3 死锁案例三

![image-20210309231939825](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231939825.png)

别看这个案例里每个事务都只有一条 SQL 语句，但是却实实在在可能会导致死锁问题，其实说起来，这个死锁和案例一并没有什么区别，只不过理解起来要更深入一点。要知道在范围查询时，加锁是一条记录一条记录挨个加锁的，所以虽然只有一条 SQL 语句，如果两条 SQL 语句的加锁顺序不一样，也会导致死锁。

在案例一中，事务 A 的加锁顺序为： id = 20 -> 30，事务 B 的加锁顺序为：id = 30 -> 20，正好相反，所以会导致死锁。这里的情景也是一样，事务 A 的范围条件为 id < 30，加锁顺序为：id = 15 -> 18 -> 20，事务 B 走的是二级索引 age，加锁顺序为：(age, id) = (24, 18) -> (24, 20) -> (25, 15) -> (25, 49)，其中，对 id 的加锁顺序为 id = 18 -> 20 -> 15 -> 49。可以看到事务 A 先锁 15，再锁 18，而事务 B 先锁 18，再锁 15，从而形成死锁。













