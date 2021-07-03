[toc]

# MySQL 锁

## 1.为什么要加锁

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
| 5      | 业务逻辑处理，确定要将商品S库存增加10，故更新库存为110（`update stock set amount=110 where sku_id=S;`） |                                                              |
| 6      |                                                              | 业务逻辑处理，确定要将商品S库存增加20，故更新库存为120（`update stock set amount=120 where sku_id=S;`） |
| 7      | 提交事务A                                                    |                                                              |
| 8      |                                                              | 提交事务B                                                    |

**异常结果：**商品S库存更新为120，但实际上针对商品S进行了两次入库操作，最终商品S库存应为 `100+10+20=130`，但实际结果为120，首先提交的事务A的更新『丢失了』！！！所以就需要锁机制来保证这种情况不会发生。



## 2.Mysql锁的概念与特性

在Mysql数据库系统中,不同的存储引擎支持不同的锁机制。比如 `MyISAM` 和 `MEMORY` 存储引擎采用的表级锁（`table-level locking`），BDB采用的是页面锁（ `page-level locking` ），也支持表级锁，InnoDB存储引擎既支持行级锁（ `row-level locking`），也支持表级锁，默认情况下采用行级锁。

MySQL这3种锁的特性可大致归纳如下:

| 模式   | 开锁、加锁速度、死锁、粒度、并发性能                         |
| ------ | ------------------------------------------------------------ |
| 表级锁 | 开销小，加锁快；不会出现死锁；锁定粒度大，发生锁冲突的概率最高，并发度最低。 |
| 行级锁 | 开销大，加锁慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低，并发度也最高。 |
| 页面锁 | 开销和加锁时间界于表锁和行锁之间；会出现死锁；锁定粒度界于表锁和行锁之间，并发度一般。 |

从上述特点可见，很难笼统地说哪种锁更好，只能就具体应用的特点来说哪种锁更合适！仅从锁的角度来说：表级锁更适合于以查询为主，只有少量按索引条件更新数据的应用，如Web应用；而行级锁则更适合于有大量按索引条件并发更新少量不同数据，同时又有 并发查询的应用，如一些在线事务处理（OLTP）系统。



## 3.MyISAM 锁

### 3.1.MyISAM表锁模式

![image-20210308232003088](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308232003088.png)

MySQL的表级锁有两种模式：表共享读锁（`Table Read Lock`）和表独占写锁（`Table Write Lock`）。

#### 3.1.1.MyISAM Lock Read(共享读)

MyISAM表的读操作，不会阻塞其他用户对同一个表的读请求，但会阻塞对同一个表的写请求.

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

#### 3.1.2.MyISAM Lock Write(读占写)

MyISAM表的写操作，会阻塞其他用户对同一个表的读和写操作。

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

### 3.2.表级锁争用情况分析

通过检查 `table_locks_waited` (表锁等待,无法立即获得数据)和 `table_locks_immediate` (立即获得锁地查询数目)状态变量分析系统上表锁争夺情况

```
show status like '%table_lock%'
```

![image-20210308232240473](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308232240473.png)

分析:如果 `Table_locks_waited` 数值比较高,就说明存在着较严重的表级锁争用情况 ,性能有问题，并发高,需要优化.

### 3.3.关于MyISAM 锁调度

MyISAM存储引擎的读和写锁是互斥，读操作是串行的。

那么，一个进程请求某个MyISAM表的读锁，同时另一个进程也请求同一表的写锁，MySQL如何处理呢？

答案是写进程先获得锁。不仅如此，即使读进程先请求先到锁等待队列，写请求后到，写锁也会插到读请求之前！这是因为MySQL认为写请求一般比读请求重要。这也正是MyISAM表不太适合于有大量更新操作和查询操作应用的原因，因为，大量的更新操作会造成查询操作很难获得读锁，从而可能永远阻塞。这种情况有时可能会变得非常糟糕！幸好我们可以通过一些设置来调节MyISAM的调度行为。

- 通过指定启动参数 `low-priority-updates`，使MyISAM引擎默认给予读请求以优先的权利。
- 通过执行命令 `SET LOW_PRIORITY_UPDATES=1`，使该连接发出的更新请求优先级降低。
- 通过指定 `INSERT`、`UPDATE`、`DELETE` 语句的 `LOW_PRIORITY` 属性，降低该语句的优先级。

虽然上面3种方法都是要么更新优先，要么查询优先的方法，但还是可以用其来解决查询相对重要的应用（如用户登录系统）中，读锁等待严重的问题。

另外，MySQL也提供了一种折中的办法来调节读写冲突，即给系统参数 `max_write_lock_count` 设置一个合适的值，当一个表的读锁达到这个值后，MySQL会暂时将写请求的优先级降低，给读进程一定获得锁的机会。



### 3.4.表锁的加锁规则

- 对于读锁
  - 持有读锁的会话可以读表，但不能写表；
  - 允许多个会话同时持有读锁；
  - 其他会话就算没有给表加读锁，也是可以读表的，但是不能写表；
  - 其他会话申请该表写锁时会阻塞，直到锁释放。
- 对于写锁
  - 持有写锁的会话既可以读表，也可以写表；
  - 只有持有写锁的会话才可以访问该表，其他会话访问该表会被阻塞，直到锁释放；
  - 其他会话无论申请该表的读锁或写锁，都会阻塞，直到锁释放。



### 3.5.表锁的释放规则

- 使用 `UNLOCK TABLES` 语句可以显示释放表锁；
- 如果会话在持有表锁的情况下执行 `LOCK TABLES` 语句，将会释放该会话之前持有的锁；
- 如果会话在持有表锁的情况下执行 `START TRANSACTION` 或 `BEGIN` 开启一个事务，将会释放该会话之前持有的锁；
- 如果会话连接断开，将会释放该会话所有的锁。





## 4.InnoDB 锁

### 4.1.InnoDB锁类型概述

![image-20210308230318517](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230318517.png)

简介（后面会分别详细说到）：

1. 乐观锁与悲观锁是两种并发控制的思想，可用于解决丢失更新问题：

   **乐观锁**会“乐观地”假定大概率不会发生并发更新冲突，访问、处理数据过程中不加锁，只在更新数据时再根据版本号或时间戳判断是否有冲突，有则处理，无则提交事务；

   **悲观锁**会“悲观地”假定大概率会发生并发更新冲突，访问、处理数据前就加排他锁，在整个数据处理过程中锁定数据，事务提交或回滚后才释放锁；

2. InnoDB支持多种锁粒度，默认使用行锁，锁粒度最小，锁冲突发生的概率最低，支持的并发度也最高，但系统消耗成本也相对较高；
3. 共享锁与排他锁是InnoDB实现的两种标准的行锁；
4. InnoDB有三种锁算法——记录锁、gap间隙锁、还有结合了记录锁与间隙锁的next-key锁，InnoDB对于行的查询加锁是使用的是`next-key locking` 这种算法，一定程度上解决了幻读问题；
5. 意向锁是为了支持多种粒度锁同时存在；

### 4.2.行锁详解

InnoDB默认使用行锁，实现了两种标准的行锁——共享锁与排他锁；

![image-20210308230528011](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230528011.png)

**注意：**

- 除了显式加锁的情况，其他情况下的加锁与解锁都无需人工干预。
- InnoDB所有的行锁算法都是基于索引实现的，锁定的也都是索引或索引区间（这一点会在`锁算法`中详细说到）；

**共享锁与排它锁兼容性示例（使用默认的RR隔离级别，图中数字从小到大标识操作执行先后顺序）：**

![image-20210308230636211](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230636211.png)

### 4.3.当前读与快照读

**4.3.1.当前读：**即加锁读，读取记录的最新版本，会加锁保证其他并发事务不能修改当前记录，直至获取锁的事务释放锁；

使用当前读的操作主要包括：显式加锁的读操作与插入/更新/删除等写操作，如下所示：

```
select * from table where ? lock in share mode;
select * from table where ? for update;
insert into table values (…);
update table set ? where ?;
delete from table where ?;
```

> 注：当 `Update SQL` 被发给MySQL后，MySQL Server会根据where条件，读取第一条满足条件的记录，然后InnoDB引擎会将第一条记录返回，并加锁，待MySQL Server收到这条加锁的记录之后，会再发起一个Update请求，更新这条记录。一条记录操作完成，再读取下一条记录，直至没有满足条件的记录为止。因此，Update操作内部，就包含了当前读。同理，Delete操作也一样。Insert操作会稍微有些不同，简单来说，就是Insert操作可能会触发 `Unique Key` 的冲突检查，也会进行一个当前读。

**4.4.2.快照读**：即不加锁读，读取记录的快照版本而非最新版本，通过MVCC实现；

InnoDB默认的RR事务隔离级别下，不显式加 `lock in share mode` 与 `for update` 的 `select` 操作都属于快照读，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；

### 4.4.MVCC

> MVCC『多版本并发控制』，与之对应的是『基于锁的并发控制』；

MVCC的最大好处：读不加任何锁，读写不冲突，对于读操作多于写操作的应用，极大的增加了系统的并发性能；

InnoDB默认的RR事务隔离级别下，不显式加 `lock in share mode` 与 `for update` 的 `select` 操作都属于快照读，使用MVCC，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；

> 关于InnoDB MVCC的实现原理，在《高性能Mysql》一书中有一些说明，网络上也大多沿用这一套理论，但这套理论与InnoDB的实际实现还是有一定差距的，但不妨我们通过它初步理解MVCC的实现机制，所以我在此贴上此书中的说明；

![4132383459-5ac1c82d92d75_articlex](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210308230801.png)

### 4.5.锁算法

InnoDB主要实现了三种行锁算法：

![image-20210308230856792](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230856792.png)

InnoDB所有的行锁算法都是基于索引实现的，锁定的也都是索引或索引区间；

不同的事务隔离级别、不同的索引类型、是否为等值查询，使用的行锁算法也会有所不同；下面仅以InnoDB默认的RR隔离级别、等值查询为例，介绍几种行锁算法：

![image-20210308230917442](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230917442.png)

**4.5.1.等值查询使用聚簇索引**
![image-20210308230938553](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230938553.png)

> 注： InnoDB表是索引组织表，根据主键索引构造一棵B+树，叶子节点存放的是整张表的行记录数据，且按主键顺序存放；我这里做了一个表格模拟主键索引的叶子节点，使用主键索引查询，就会锁住相关主键索引，锁住了索引也就锁住了行记录，其他并发事务就无法修改此行数据，直至提交事务释放锁，保证了并发情况下数据的一致性；

**4.5.2.等值查询使用唯一索引**
![这里写图片描述](https://segmentfault.com/img/remote/1460000014133589)

> 注：辅助索引的叶子节点除了存放辅助索引值，也存放了对应主键索引值；锁定时会锁定辅助索引与主键索引；

**4.5.3.等值查询使用辅助索引**
![image-20210308230954750](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308230954750.png)

> 注：Gap锁，锁定的是索引记录之间的间隙，是防止幻读的关键；如果没有上图中绿色标识的Gap Lock，其他并发事务在间隙中插入了一条记录如：`insert into stock (id,sku_id) values(2,103);` 并提交，那么在此事务中重复执行上图中SQL，就会查询出并发事务新插入的记录，即出现幻读；（幻读是指在同一事务下，连续执行两次同样的SQL语句可能导致不同的结果，第二次的SQL语句可能返回之前不存在的行记录）加上Gap Lock后，并发事务插入新数据前会先检测间隙中是否已被加锁，防止幻读的出现；

### 4.6.锁问题

MySQL锁会带来如下几种问题，如果能解决他们，就可以保证并发情况下不会出现问题；

| 锁问题     | 锁问题描述                                                   | 会出现锁问题的隔离级别                                  | 解决办法                                                     |
| ---------- | ------------------------------------------------------------ | ------------------------------------------------------- | ------------------------------------------------------------ |
| 脏读       | 一个事务中会读到其他并发事务未提交的数据，违反了事务的隔离性； | `Read Uncommitted`                                      | 提高事务隔离级别至 `Read Committed` 及以上；                 |
| 不可重复读 | 一个事务会读到其他并发事务已提交的数据，违反了数据库的一致性要求；可能出现的问题为幻读，幻读是指在同一事务下，连续执行两次同样的SQL语句可能导致不同的结果，第二次的SQL语句可能返回之前不存在的行记录； | `Read Uncommitted`、`Read Committed`                    | 默认的RR隔离级别下 ，解决办法分为两种情况：1、当前读：`Next-Key Lock` 机制对相关索引记录及索引间隙加锁，防止并发事务修改数据或插入新数据到间隙；2、版本读：MVCC，保证事务执行过程中只有第一次读之前提交的修改和自己的修改可见，其他的均不可见；提高事务隔离级别至Serializable； |
| 丢失更新   |                                                              | `Read Uncommitted`、`Read Committed`、`Repeatable Read` | 默认的RR隔离级别下 ，解决办法分为两种情况：1、乐观锁：数据表增加version字段，读取数据时记录原始version，更新数据时，比对version是否为原始version，如不等，则证明有并发事务已更新过此行数据，则可回滚事务后重试直至无并发竞争；2、悲观锁：读加排他锁，保证整个事务执行过程中，其他并发事务无法读取相关记录，直至当前事务提交或回滚释放锁； |

> 注：其实InnoDB默认的RR事务隔离级别已经为我们做了大多数的事，业务中更多需要关心『丢失更新』这种问题，通常使用乐观锁方式解决；我们在读操作时一般不会使用加锁读，但MVCC并不能完全解读幻读问题，其他并发事务是可以插入符合当前事务查询条件的数据，只是当前事务因为读快照数据无法查看到，这种情况下应该使用唯一索引等方式保证不会重复插入重复的业务数据，在此不再赘述~





## 5.MySQL 锁种类

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
   
   ```
   insert into table value(11, xx) 
   ```
   
   事务2在10-20的记录中插入了一行： 
   
   ```
   insert into table value(12, xx) 
   ```
   
   由于两条插入的记录不冲突，所以会使用插入意向锁，且事务2不会被阻塞。

4. **自增锁（Auto-inc Locks）** 

   自增锁是一种特殊的表级别锁，专门针对事务插入 `AUTO-INCREMENT` 类型的列。 

   即一个事务正在往表中插入记录时，其他事务的插入必须等待，以便第1个事务插入的行得到的主键值是连续的。 

   举个例子：在可重复读隔离级别下，PK ID为自增主键 

   表中已有主键ID为1、2、3的3条记录。 

   事务1插入了一行： 

   ```
   insert into table value('aa') 
   ```

   得到一条`（4,’aa’）`的记录，未提交；

   此时，事务2中插入了一行： 

   ```
   insert into table value('bb') 
   ```

   这时会被阻塞，即用到了插入意向锁的概念。

5. **记录锁（Record Locks）- locks rec but not gap** 

   记录锁是的单个行记录上的锁，会阻塞其他事务对其插入、更新、删除；

6. **间隙锁(Gap Lock)** 

   间隙锁锁定记录的一个间隔，但不包含记录本身。 

   举个例子： 

   假如数据库已有ID为1、6两条记录， 现在想要在 `ID in （4，10）`之间更新数据的时候，会加上间隙锁，锁住 `[4,5] [7,10]` ,(不包含已有记录 `ID=5` 本身) 。那么在更新 `ID=5` 的记录（只有一条记录）符合条件； 如果不加间隙锁，事务2有可能会在4、10之间插入一条数据，这个时候事务1再去更新，发现在`(4,10)`这个区间内多出了一条“幻影”数据。

   间隙锁就是防止其他事务在间隔中插入数据，以导致“不可重复读”。

7. **临键锁（Next-Key Lock）= Gap Lock + Record Lock** 

   临建锁是记录锁与间隙锁的组合，即：既包含索引记录，又包含索引区间，主要是为了解决幻读。



## X.案例分析

###  X.1.记一次神奇的Mysql死锁排查

#### X.1.1.问题初现

在某天下午，突然系统报警，抛出个异常:

![image-20210309214413908](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309214413908.png)

仔细一看好像是事务回滚异常，写着的是因为死锁回滚，原来是个死锁问题，由于我对Mysql锁还是有一定了解的，于是开始主动排查这个问题。

首先在数据库中查找 `Innodb Status`，在 `Innodb Status` 中会记录上一次死锁的信息,输入下面命令:

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

事务1执行Update语句的时候需要获取 `uidx_tenant` 这个索引再where条件上的X锁(行锁)

事务2执行同样的Update语句，也在 `uidx_tenant` 上面想要获取X锁(行锁)，然后就出现了死锁，回滚了事务1。

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

#### X.1.2.深入剖析

上面我们说有三个事务进入update语句，为了简化说明这里我们只需要两个事务同时进入update语句即可，下面的表格展示了我们整个的发生过程:

| 时间线 | 事务1            | 事务2                                           | 事务3                                 |
| ------ | ---------------- | ----------------------------------------------- | ------------------------------------- |
| 1      | `insert into xx` | `insert into xx`                                | `insert into xx`                      |
| 2      | 获取当前行X锁    |                                                 |                                       |
| 3      |                  | 需要检测唯一索引是否冲突获取S锁，阻塞           | 需要检测唯一索引是否冲突获取S锁，阻塞 |
| 4      | commit;          | 获取到到S锁                                     | 获取到S锁                             |
| 5      |                  | 发现唯一索引冲突，执行Update语句(此时S锁未释放) | 发现唯一索引冲突，执行Update语句      |
| 6      |                  | 获取该行的X锁，被事务3的S锁阻塞                 | 获取该行的X锁，被事务2的S锁阻塞       |
| 7      |                  | 发现死锁，回滚该事务                            | update成功,commit;                    |

> 小提示:S锁是共享锁，X锁是互斥锁。一般来说X锁和S，X锁都互斥，S锁和S锁不互斥。

我们从上面的流程中看见发生这个死锁的关键需要获取S锁，为什么我们再插入的时候需要获取S锁呢？因为我们需要检测唯一索引？在RR隔离级别下如果要读取那么就是当前读,那么其实就需要加上S锁。这里发现唯一键已经存在，这个时候执行update就会被两个事务的S锁互相阻塞，从而形成上面的循环等待条件。

![image-20210309215416285](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309215416285.png)

> 小提示: 在MVCC中，当前读和快照读的区别:当前读每次需要加锁（可以使共享锁或者互斥锁）获取到最新的数据，而快照读是读取的是这个事务开始的时候那个快照，这个是通过 `undo log` 去进行实现的。

#### X.1.3.解决方案

这里的核心问题是需要把S锁给干掉，这里有三个可供参考的解决方案:

- 将RR隔离级别，降低成RC隔离级别。这里RC隔离级别会用快照读，从而不会加S锁。
- 再插入的时候使用 `select * for update` ,加X锁，从而不会加S锁。
- 可以提前加上分布式锁，可以利用Redis,或者ZK等

第一种方法不太现实，毕竟隔离级别不能轻易的修改。第三种方法又比较麻烦。所以第二种方法是我们最后确定的。

#### X.1.4.总结

说了这么多，最后做一个小小的总结吧。排查死锁这种问题的时候有时候光看死锁日志有时候会解决不了问题，需要结合整个的业务日志，代码以及表结构来进行分析，才能得到正确的结果。



### X.2.mysql死锁问题分析

线上某服务时不时报出如下异常（大约一天二十多次）：“Deadlock found when trying to get lock;”。

Oh, My God! 是死锁问题。尽管报错不多，对性能目前看来也无太大影响，但还是需要解决，保不齐哪天成为性能瓶颈。

为了更系统的分析问题，本文将从死锁检测、索引隔离级别与锁的关系、死锁成因、问题定位这五个方面来展开讨论。

![image-20210309220607043](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309220607043.png)

<center>图1 应用日志</center>

#### X.2.1.死锁是怎么被发现的？

##### X.2.1.1.死锁成因&&检测方法

左图那两辆车造成死锁了吗？不是！右图四辆车造成死锁了吗？是！

![image-20210309221113569](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221113569.png)

<center>图2 死锁描述</center>

我们mysql用的存储引擎是innodb，从日志来看，innodb主动探知到死锁，并回滚了某一苦苦等待的事务。问题来了，innodb是怎么探知死锁的？

直观方法是在两个事务相互等待时，当一个等待时间超过设置的某一阀值时，对其中一个事务进行回滚，另一个事务就能继续执行。这种方法简单有效，在innodb中，参数 `innodb_lock_wait_timeout` 用来设置超时时间。

仅用上述方法来检测死锁太过被动，innodb还提供了 `wait-for graph` 算法来主动进行死锁检测，每当加锁请求无法立即满足需要并进入等待时，`wait-for graph` 算法都会被触发。

##### X.2.1.2.wait-for graph原理

我们怎么知道上图中四辆车是死锁的？他们相互等待对方的资源，而且形成环路！我们将每辆车看为一个节点，当节点1需要等待节点2的资源时，就生成一条有向边指向节点2，最后形成一个有向图。我们只要检测这个有向图是否出现环路即可，出现环路就是死锁！这就是`wait-for graph` 算法。
![image-20210309221217857](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221217857.png)                                              

<center>图3 wait for graph</center>

innodb将各个事务看为一个个节点，资源就是各个事务占用的锁，当事务1需要等待事务2的锁时，就生成一条有向边从1指向2，最后行成一个有向图。

#### X.2.2. innodb隔离级别、索引与锁 

死锁检测是死锁发生时innodb给我们的救命稻草，我们需要它，但我们更需要的是避免死锁发生的能力，如何尽可能避免？这需要了解innodb中的锁。

##### X.2.2.1.锁与索引的关系

假设我们有一张消息表（msg），里面有3个字段。假设id是主键，token是非唯一索引，message没有索引。

| id: bigint | token: varchar(30) | message: varchar(4096) |
| ---------- | ------------------ | ---------------------- |
|            |                    |                        |

innodb对于主键使用了聚簇索引，这是一种数据存储方式，表数据是和主键一起存储，主键索引的叶结点存储行数据。对于普通索引，其叶子节点存储的是主键值。
![image-20210309221351414](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221351414.png)

<center>图4 聚簇索引和二级索引</center>

下面分析下索引和锁的关系。

1）`delete from msg where id=2;`

由于id是主键，因此直接锁住整行记录即可。

![image-20210309221525678](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221525678.png)                                        

2）`delete from msg where token=’ cvs’;`

由于token是二级索引，因此首先锁住二级索引（两行），接着会锁住相应主键所对应的记录；

![image-20210309221602592](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221602592.png)                                    

3）`delete from msg where message=订单号是多少’;`

message没有索引，所以走的是全表扫描过滤。这时表上的各个记录都将添加上X锁。

![image-20210309221643052](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309221643052.png)                                    

##### X.2.2.2.锁与隔离级别的关系

大学数据库原理都学过，为了保证并发操作数据的正确性，数据库都会有事务隔离级别的概念：

- 未提交读（Read uncommitted）
- 已提交读（Read committed（RC））
- 可重复读（Repeatable read（RR）
- 可串行化（Serializable）。我们较常使用的是RC和RR。

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

#### X.2.3.死锁成因

了解了innodb锁的基本原理后，下面分析下死锁的成因。如前面所说，死锁一般是事务相互等待对方资源，最后形成环路造成的。下面简单讲下造成相互等待最后形成环路的例子。

##### X.2.3.1.不同表相同记录行锁冲突

这种情况很好理解，事务A和事务B操作两张表，但出现循环等待锁情况。
![image-20210309222100709](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222100709.png)

##### X.2.3.2.相同表记录行锁冲突

这种情况比较常见，之前遇到两个job在执行数据批量更新时，jobA处理的的id列表为[1,2,3,4]，而job处理的id列表为[8,9,10,4,2]，这样就造成了死锁。

![image-20210309222154206](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222154206.png)

##### X.2.3.3.不同索引锁冲突

这种情况比较隐晦，事务A在执行时，除了在二级索引加锁外，还会在聚簇索引上加锁，在聚簇索引上加锁的顺序是[1,4,2,3,5]，而事务B执行时，只在聚簇索引上加锁，加锁顺序是[1,2,3,4,5]，这样就造成了死锁的可能性。

![image-20210309222243979](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222243979.png)                                  

##### X.2.3.4.gap锁冲突

innodb在RR级别下，如下的情况也会产生死锁，比较隐晦。不清楚的同学可以自行根据上节的gap锁原理分析下。
![image-20210309222343808](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309222343808.png)                                        

#### X.2.4.如何尽可能避免死锁

- 以固定的顺序访问表和行。比如对第2节两个job批量更新的情形，简单方法是对id列表先排序，后执行，这样就避免了交叉等待锁的情形；又比如对于3.1节的情形，将两个事务的sql顺序调整为一致，也能避免死锁。

- 大事务拆小。大事务更倾向于死锁，如果业务允许，将大事务拆小。
- 在同一个事务中，尽可能做到一次锁定所需要的所有资源，减少死锁概率。
- 降低隔离级别。如果业务允许，将隔离级别调低也是较好的选择，比如将隔离级别从RR调整为RC，可以避免掉很多因为gap锁造成的死锁。
- 为表添加合理的索引。可以看到如果不走索引将会为表的每一行记录添加上锁，死锁的概率大大增大。
- 设置锁等待超时参数：`innodb_lock_wait_timeout`，这个参数并不是只用来解决死锁问题，在并发访问比较高的情况下，如果大量事务因无法立即获得所需的锁而挂起，会占用大量计算机资源，造成严重性能问题，甚至拖跨数据库。我们通过设置合适的锁等待超时阈值，可以避免这种情况发生。

#### X.2.5.如何定位死锁成因

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



### X.3.解决死锁之路（终结篇） - 再见死锁

#### X.3.1.开启锁监控

在遇到线上死锁问题时，我们应该第一时间获取相关的死锁日志。我们可以通过 `show engine innodb status` 命令来获取死锁信息，但是它有个限制，只能拿到最近一次的死锁日志。MySQL 提供了一套 InnoDb 的监控机制，用于周期性（每隔 15 秒）输出 InnoDb 的运行状态到 mysqld 服务的标准错误输出（stderr）。默认情况下监控是关闭的，只有当需要分析问题时再开启，并且在分析问题之后，建议将监控关闭，因为它对数据库的性能有一定影响，另外每 15 秒输出一次日志，会使日志文件变得特别大。

InnoDb 的监控主要分为四种：标准监控（`Standard InnoDB Monitor`）、锁监控（`InnoDB Lock Monitor`）、表空间监控（`InnoDB Tablespace Monitor`）和表监控（`InnoDB Table Monitor`）。后两种监控已经基本上废弃了，关于各种监控的作用可以参考 MySQL 的官方文档 [Enabling InnoDB Monitors](https://dev.mysql.com/doc/refman/5.6/en/innodb-enabling-monitors.html) 。

要获取死锁日志，我们需要开启 InnoDb 的标准监控，我推荐将锁监控也打开，它可以提供一些额外的锁信息，在分析死锁问题时会很有用。开启监控的方法有两种：

##### X.3.1.1. 基于系统表

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

##### X.3.1.2. 基于系统参数

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

除了 MySQL 自带的监控机制，还有一些有趣的监控工具也很有用，比如 Innotop 和 Percona Toolkit 里的小工具 [pt-deadlock-logger](https://www.linuxidc.com/Linux/2014-03/97830.htm)。

#### X.3.2.读懂死锁日志

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

ACTIVE 0 sec 表示事务活动时间，inserting 为事务当前正在运行的状态，可能的事务状态有：`fetching rows`，`updating`，`deleting`，`inserting` 等。

> mysql tables in use 1, locked 1
> LOCK WAIT 11 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 15

`tables in use 1` 表示有一个表被使用，`locked 1` 表示有一个表锁。`LOCK WAIT` 表示事务正在等待锁，`11 lock struct(s)` 表示该事务的锁链表的长度为 11，每个链表节点代表该事务持有的一个锁结构，包括表锁，记录锁以及 `autoinc` 锁等。`heap size 1184` 为事务分配的锁堆内存大小。

`2 row lock(s)` 表示当前事务持有的行锁个数，通过遍历上面提到的 11 个锁结构，找出其中类型为 `LOCK_REC` 的记录数。`undo log entries 15` 表示当前事务有 15 个 undo log 记录，因为二级索引不记 undo log，说明该事务已经更新了 15 条聚集索引记录。

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
RECORD LOCKS space id 0 page no 307 n bits 72 index `PRIMARY` of table `test`.`test` trx id 50F lock_mode X` `Record lock, heap no 1 PHYSICAL RECORD: n_fields 1; compact format; info bits 0
```

看起来像是 Next-key 锁，但是看下面的 `heap no 1` 表示这个记录是 supremum record（另外，infimum record 的 heap no 为 0），所以这个锁应该看作是一个间隙锁。

看完第一个事务，再来看看第二个事务：

> *** (2) TRANSACTION:



```
TRANSACTION 182335756, ACTIVE 0 sec inserting
mysql tables in use 1, locked 1
11 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 15
MySQL thread id 12032049, OS thread handle 0x7ff35f5dd700, query id 196418268 10.40.189.132 RW_bok_db update
INSERT INTO bok_task
( order_id ...
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` trx id 182335756 lock_mode X
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 300 page no 5480 n bits 552 index `order_id_un` of table `bok_db`.`bok_task` trx id 182335756 lock_mode X insert intention waiting
```

事务二和事务一的日志基本类似，不过它多了一部分 `HOLDS THE LOCK(S)`，表示事务二持有什么锁，这个锁往往就是事务一处于锁等待的原因。这里可以看到事务二正在等待索引 `order_id_un` 上的插入意向锁，并且它已经持有了一个 X 锁（`Next-key` 锁，也有可能是 `supremum` 上的间隙锁）。

到这里为止，我们得到了很多关键信息，此时我们可以逆推出死锁发生的原因吗？这可能也是每个开发人员和 DBA 最关心的问题，如何通过死锁日志来诊断死锁的成因？实际上这是非常困难的。

如果每个事务都只有一条 SQL 语句，这种情况的死锁成因还算比较好分析，因为我们可以从死锁日志里找到每个事务执行的 SQL 语句，只要对这两条 SQL 语句的加锁过程有一定的了解，死锁原因一般不难定位。但也有可能死锁的成因非常隐蔽，这时需要我们对这两条 SQL 语句的加锁流程做非常深入的研究才有可能分析出死锁的根源。

不过大多数情况下，每个事务都不止一条 SQL 语句，譬如上面的死锁日志里显示的 `undo log entries 15`，说明执行 INSERT 语句之前肯定还执行了其他的 SQL 语句，但是具体是什么，我们不得而知，我们只能根据 HOLDS THE LOCK(S) 部分知道有某个 SQL 语句对 `order_id_un` 索引加了 `Next-key` 锁（或间隙锁）。另外事务二在 `WAITING FOR` 插入意向锁，至于它和事务一的哪个锁冲突也不得而知，因为事务一的死锁日志里并没有 `HOLDS THE LOCK(S) ` 部分。

所以，对死锁的诊断不能仅仅靠死锁日志，还应该结合应用程序的代码来进行分析，如果实在接触不到应用代码，还可以通过数据库的 binlog 来分析（只要你的死锁不是 100% 必现，那么 binlog 日志里肯定能找到一份完整的事务一和事务二的 SQL 语句）。通过应用代码或 binlog 理出每个事务的 SQL 执行顺序，这样分析死锁时就会容易很多。

#### X.3.3.常见死锁分析

尽管上面说通过死锁日志来推断死锁原因非常困难，但我想也不是完全不可能。我在 Github 上新建了一个项目 [mysql-deadlocks](https://github.com/aneasystone/mysql-deadlocks)，这个项目收集了一些常见的 MySQL 死锁案例，大多数案例都来源于网络，并对它们进行分类汇总，试图通过死锁日志分析出每种死锁的原因，还原出死锁现场。这虽然有点痴人说梦的感觉，但还是希望能给后面的开发人员在定位死锁问题时带来一些便利。

我将这些死锁按事务执行的语句和正在等待或已持有的锁进行分类汇总（目前已经收集了十余种死锁场景）：

![image-20210309231753281](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231753281.png)

表中的语句虽然只列出了 delete 和 insert，但实际上绝大多数的 delete 语句和 update 或 `select ... for update` 加锁机制是一样的，所以为了避免重复，对于 update 语句就不在一起汇总了（当然也有例外，譬如使用 update 对索引进行更新时加锁机制和 delete 是有区别的，这种情况我会单独列出）。

对每一个死锁场景，我都会定义一个死锁名称（实际上就是事务等待和持有的锁），每一篇分析，我都分成了 死锁特征、死锁日志、表结构、重现步骤、分析和参考 这几个部分。

下面我们介绍几种常见的死锁场景，还是以前面提到的 students 表为例：

![image-20210309231823404](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231823404.png)

其中，id 为主键，no（学号）为二级唯一索引，name（姓名）和 age（年龄）为二级非唯一索引，score（学分）无索引。数据库隔离级别为 RR。

##### X.3.3.1 死锁案例一

![image-20210309231842684](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231842684.png)

死锁的根本原因是有两个或多个事务之间加锁顺序的不一致导致的，这个死锁案例其实是最经典的死锁场景。

首先，事务 A 获取 id = 20 的锁（`lock_mode X locks rec but not gap`），事务 B 获取 id = 30 的锁；然后，事务 A 试图获取 id = 30 的锁，而该锁已经被事务 B 持有，所以事务 A 等待事务 B 释放该锁，然后事务 B 又试图获取 id = 20 的锁，这个锁被事务 A 占有，于是两个事务之间相互等待，导致死锁。

##### X.3.3.2 死锁案例二

![image-20210309231910096](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231910096.png)

首先事务 A 和事务 B 执行了两条 UPDATE 语句，但是由于 id = 25 和 id = 26 记录都不存在，事务 A 和 事务 B 并没有更新任何记录，但是由于数据库隔离级别为 RR，所以会在 (20, 30) 之间加上间隙锁（`lock_mode X locks gap before rec`），间隙锁和间隙锁并不冲突。之后事务 A 和事务 B 分别执行 INSERT 语句要插入记录 id = 25 和 id = 26，需要在 (20, 30) 之间加插入意向锁（`lock_mode X locks gap before rec insert intention`），插入意向锁和间隙锁冲突，所以两个事务互相等待，最后形成死锁。

要解决这个死锁很简单，显然，前面两条 UPDATE 语句是无效的，将其删除即可。另外也可以将数据库隔离级别改成 RC，这样在 UPDATE 的时候就不会有间隙锁了。这个案例正是文章开头提到的死锁日志中的死锁场景，别看这个 UPDATE 语句是无效的，看起来很傻，但是确实是真实的场景，因为在真实的项目中代码会非常复杂，比如采用了 ORM 框架，应用层和数据层代码分离，一般开发人员写代码时都不知道会生成什么样的 SQL 语句，我也是从 DBA 那里拿到了 binlog，然后从里面找到了事务执行的所有 SQL 语句，发现其中竟然有一行无效的 UPDATE 语句，最后追本溯源，找到对应的应用代码，将其删除，从而修复了这个死锁。

##### X.3.3.3 死锁案例三

![image-20210309231939825](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210309231939825.png)

别看这个案例里每个事务都只有一条 SQL 语句，但是却实实在在可能会导致死锁问题，其实说起来，这个死锁和案例一并没有什么区别，只不过理解起来要更深入一点。要知道在范围查询时，加锁是一条记录一条记录挨个加锁的，所以虽然只有一条 SQL 语句，如果两条 SQL 语句的加锁顺序不一样，也会导致死锁。

在案例一中，事务 A 的加锁顺序为：`id = 20 -> 30`，事务 B 的加锁顺序为：`id = 30 -> 20`，正好相反，所以会导致死锁。这里的情景也是一样，事务 A 的范围条件为 `id < 30`，加锁顺序为：`id = 15 -> 18 -> 20`，事务 B 走的是二级索引 age，加锁顺序为：`(age, id) = (24, 18) -> (24, 20) -> (25, 15) -> (25, 49)`，其中，对 id 的加锁顺序为 `id = 18 -> 20 -> 15 -> 49`。可以看到事务 A 先锁 15，再锁 18，而事务 B 先锁 18，再锁 15，从而形成死锁。



### X.4.并发insert操作导致的dead lock

**X.4.1. 说明**

线上某业务最近经常会出现dead lock，相关信息如下：

```
2016-06-15 20:28:25 7f72c0043700InnoDB: transactions deadlock detected, dumping detailed information.

2016-06-15 20:28:25 7f72c0043700
*** (1) TRANSACTION:
TRANSACTION 151506716, ACTIVE 30 sec inserting
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 1
MySQL thread id 1467337, OS thread handle 0x7f72a84d6700, query id 308125831 IP地址1 fold-sys update
insert into t ( a,b,c, addtime )
        values
         (63, 27451092,120609109,now())
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 46 page no 693076 n bits 664 index `unq_fk_key` of table `dbname`.`t` trx id 151506716 lock_mode X locks gap before rec insert intention waiting
*** (2) TRANSACTION:
TRANSACTION 151506715, ACTIVE 30 sec inserting, thread declared inside InnoDB 1
mysql tables in use 1, locked 1
4 lock struct(s), heap size 1184, 2 row lock(s), undo log entries 1
MySQL thread id 1477334, OS thread handle 0x7f72c0043700, query id 308125813 IP地址2 fold-sys update
insert into t ( a,b,c, addtime )
        values
         (63, 27451092,120609109,now())
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 46 page no 693076 n bits 664 index `unq_fk_folder_fk_video_seq` of table `folder`.`t_mapping_folder_video` trx id 151506715 lock mode S locks gap before rec
*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 46 page no 693076 n bits 664 index`unq_fk_key` of table `dbname`.`t` trx  id 151506715 lock_mode X locks gap before rec insert intention waiting
*** WE ROLL BACK TRANSACTION (2)
```

**X.4.2. 初步分析**

1. 122和120 在同一时刻发起了相同的insert 操作  数据一模一样 而 a,b,c 刚好是uniq key

2. 咱们是RC 级别  出现了 GAP lock 这个有点疑问？查阅了下文档 

```
Gap locking can be disabled explicitly. This occurs if you change the transaction isolation level to READ COMMITTED or enable theinnodb_locks_unsafe_for_binlog system variable (which is now deprecated). Under these circumstances, gap locking is disabled for searches and index scans and is used only for foreign-key constraint checking and duplicate-key checking.
```

设置 `innodb_locks_unsafe_for_binlog` 或者RC级别来关闭gap  

后面部分 可以理解为 RC级别下的 外键和重复检查的时候也会产生GAP呢

**X.4.3. 重现此deadlock**

```
5.5.19-55-log Percona Server (GPL), Release rel24.0, Revision 204

tx_isolation=READ-COMMITTED 

innodb_locks_unsafe_for_binlog=OFF
```

**X.4.4. 创建实验表**

```
CREATE TABLE `deadlock` (

  `id` bigint(20) NOT NULL AUTO_INCREMENT,

  `a` smallint(5) unsigned NOT NULL DEFAULT '0',

  `b` int(11) NOT NULL DEFAULT '0',

  `c` int(11) NOT NULL DEFAULT '0',

  `d` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',

  PRIMARY KEY (`id`),

  UNIQUE KEY `unq_b_c_a` (`b`,`c`,`a`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

**X.4.5. 事务T1**

```
mysql> begin;

Query OK, 0 rows affected (0.00 sec)

 

mysql>insert into deadlock(a,b,c) values(1,2,3);

Query OK, 1 row affected (0.00 sec)

Records: 1  Duplicates: 0  Warnings: 0
```

事务和锁
#此时表deadlock上被加了一把意向排它锁（IX）

```
---TRANSACTION 4F23D, ACTIVE 20 sec

1 lock struct(s), heap size 376, 0 row lock(s), undo log entries 1

MySQL thread id 10, OS thread handle 0x41441940, query id 237 localhost root

TABLE LOCK table `yujx`.`deadlock` trx id 4F236 lock mode IX
```

**X.4.6. 事务T2**

```
mysql> begin;

Query OK, 0 rows affected (0.00 sec)

mysql> insert into deadlock(a,b,c) select 1,2,3;

#此处会处于等待
```

事务和锁

```
---TRANSACTION 4F23E, ACTIVE 3 sec inserting

mysql tables in use 1, locked 1

LOCK WAIT 2 lock struct(s), heap size 376, 1 row lock(s), undo log entries 1

MySQL thread id 7, OS thread handle 0x408d8940, query id 243 localhost root update

insert into deadlock(a,b,c) values(1,2,3)

#事务T2对表deadlock加了一把意向排它锁（IX），而对unq_b_c_a唯一约束检查时需要获取对应的共享锁，但是对应记录被T1加了X锁，此处等待获取S锁（#注意，insert进行的是当前读，所以读会被X锁阻塞。如果是快照读的话，不需要等待X锁）

------- TRX HAS BEEN WAITING 3 SEC FOR THIS LOCK TO BE GRANTED:

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F237 lock mode S waiting

------------------

TABLE LOCK table `yujx`.`deadlock` trx id 4F237 lock mode IX

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F237 lock mode S waiting

---TRANSACTION 4F23D, ACTIVE 37 sec

#事务T1对表deadlock加了一把意向排它锁（IX）和记录锁（X）

2 lock struct(s), heap size 376, 1 row lock(s), undo log entries 1

MySQL thread id 10, OS thread handle 0x41441940, query id 237 localhost root

TABLE LOCK table `yujx`.`deadlock` trx id 4F236 lock mode IX

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F236 lock_mode X locks rec but not gap

----------------------------
```

**X.4.7. 事务T3**

```
mysql> begin;

Query OK, 0 rows affected (0.00 sec)

 

mysql> insert into deadlock(a,b,c) values(1,2,3);

#此处会处于等待
```

事务和锁

```
---TRANSACTION 4F23F, ACTIVE 3 sec inserting

mysql tables in use 1, locked 1

LOCK WAIT 2 lock struct(s), heap size 376, 1 row lock(s), undo log entries 1

MySQL thread id 8, OS thread handle 0x41976940, query id 245 localhost root update

insert into deadlock(a,b,c) values(1,2,3)

------- TRX HAS BEEN WAITING 3 SEC FOR THIS LOCK TO BE GRANTED:

#同样，事务T3与上面的事务T2的事务和锁等待一样，事务T1造成了T2和T3的等待

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F238 lock mode S waiting

------------------

TABLE LOCK table `yujx`.`deadlock` trx id 4F238 lock mode IX

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F238 lock mode S waiting

---TRANSACTION 4F23E, ACTIVE 31 sec inserting

mysql tables in use 1, locked 1

LOCK WAIT 2 lock struct(s), heap size 376, 1 row lock(s), undo log entries 1

MySQL thread id 7, OS thread handle 0x408d8940, query id 243 localhost root update

insert into deadlock(a,b,c) values(1,2,3)

------- TRX HAS BEEN WAITING 31 SEC FOR THIS LOCK TO BE GRANTED:

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F237 lock mode S waiting

------------------

TABLE LOCK table `yujx`.`deadlock` trx id 4F237 lock mode IX

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F237 lock mode S waiting

---TRANSACTION 4F23D, ACTIVE 65 sec

2 lock struct(s), heap size 376, 1 row lock(s), undo log entries 1

MySQL thread id 10, OS thread handle 0x41441940, query id 237 localhost root

TABLE LOCK table `yujx`.`deadlock` trx id 4F236 lock mode IX

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F236 lock_mode X locks rec but not gap
```

**X.4.8. 事务T1进行rollback**

```
#事务T1进行rollback;

mysql> rollback;

Query OK, 0 rows affected (0.00 sec)

#事务T2的insert成功

mysql> insert into deadlock(a,b,c) values(1,2,3);

Query OK, 1 row affected (10.30 sec)

#事务T3返回deadlock错误

mysql> insert into deadlock(a,b,c) values(1,2,3);

ERROR 1213 (40001): Deadlock found when trying to get lock; try restarting transaction
```

**X.4.9. DEADLOCK信息**

```
------------------------

LATEST DETECTED DEADLOCK

------------------------

160620 11:38:14

*** (1) TRANSACTION:

TRANSACTION 4F23E, ACTIVE 48 sec inserting

mysql tables in use 1, locked 1

LOCK WAIT 4 lock struct(s), heap size 1248, 2 row lock(s), undo log entries 1

MySQL thread id 7, OS thread handle 0x408d8940, query id 297 localhost root update

insert into deadlock(a,b,c) values(1,2,3)

*** (1) WAITING FOR THIS LOCK TO BE GRANTED:

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F23E lock_mode X insert intention waiting

*** (2) TRANSACTION:

TRANSACTION 4F23F, ACTIVE 30 sec inserting

mysql tables in use 1, locked 1

4 lock struct(s), heap size 1248, 2 row lock(s), undo log entries 1

MySQL thread id 8, OS thread handle 0x41976940, query id 300 localhost root update

insert into deadlock(a,b,c) values(1,2,3)

*** (2) HOLDS THE LOCK(S):

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F23F lock mode S

*** (2) WAITING FOR THIS LOCK TO BE GRANTED:

RECORD LOCKS space id 0 page no 101724 n bits 72 index `unq_b_c_a` of table `yujx`.`deadlock` trx id 4F23F lock_mode X insert intention waiting

*** WE ROLL BACK TRANSACTION (2)
```

如上，只能看到事务T2和事务T3最终导致了 `deadlock`；T2等待获取 `unq_b_c_a` 唯一key对应的记录锁（`X lock`）,T3在`unq_b_c_a`对应的记录上持有S锁，并且T3也在等待获取对应的X锁。最终T3被 `ROLL BACK` 了，并且发回了 `DEAD LOCK` 的提示信息

**X.4.10. 综上**

1. `SHOW ENGINE INNODB STATUS` 看到的 `DEADLOCK` 相关信息，只会返回最后的2个事务的信息，而其实有可能有更多的事务才最终导致的死锁
2. 当有3个（或以上）事务对相同的表进行insert操作，如果insert对应的字段上有 `uniq key`约束并且第一个事务rollback了，那其中一个将返回死锁错误信息。
3. 死锁的原因
   - T1 获得 X 锁并 insert 成功
   - T2 试图 insert, 检查重复键需要获得 S 锁, 但试图获得 S 锁失败, 加入等待队列, 等待 T1
   - T3 试图 insert, 检查重复键需要获得 S 锁, 但试图获得 S 锁失败, 加入等待队列, 等待 T1
   - T1 rollback, T1 释放锁, 此后 T2, T3 获得 S 锁成功, 检查 duplicate-key, 之后 INSERT 试图获得 X 锁, 但 T2, T3 都已经获得 S 锁, 导致 T2, T3 死锁
4. 避免此DEADLOCK；我们都知道死锁的问题通常都是业务处理的逻辑造成的，既然是uniq key，同时多台不同服务器上的相同程序对其insert一模一样的value，这本身逻辑就不太完美。故解决此问题：
   - 保证业务程序别再同一时间点并发的插入相同的值到相同的uniq key的表中
   - 上述实验可知，是由于第一个事务rollback了才产生的deadlock，查明rollback的原因
   - 尽量减少完成事务的时间

**X.4.11. 最终结论**

当有3个（或以上）事务对相同的表进行insert操作，如果insert对应的字段上有uniq key约束并且第一个事务rollback了，那其中一个将返回死锁错误信息。



### X.5.一次MySQL死锁的排查记录

前几天线上收到一条告警邮件，生产环境MySQL操作发生了死锁，邮件告警的提炼出来的SQL大致如下。

```
update pe_order_product_info_test
        set  end_time = '2021-04-30 23:59:59'
        where order_no = '111111111'
        and product_id = 123456
        and status in (1,2);
update pe_order_product_info_test
        set  end_time = '2021-04-30 23:59:59'
        where order_no = '222222222'
        and product_id = 123456
        and status in (1,2);      
```

是一条Update语句，定位了它的调用情况，发现Update的调用方只有一处，并且在Cat中看到一个小时的调用次数只有700多次，这个调用量基本与并发Update引起死锁无关了。

当时猜测了几种情况，这里Update进行操作时有其他业务方调用Select相关的接口，但是排查了那个时间点发生死锁应用的调用链，发现好像并没有其他会影响到Update的调用。

看了死锁日志，看到了问题要害——`index_merge`索引合并。

#### X.5.1. 什么是索引合并

这是MySQL在5.1引入的优化技术，再此之前，一个表仅仅只能使用一个索引，但索引合并的引入，可以对同一张表使用多个索引分别进行条件扫描。

如果要拿索引合并index_merge与只使用一个索引做比较，那么拿上面那个update语句来做演示。

```
update pe_order_product_info_test
        set end_time = '2021-04-30 23:59:59'
        where order_no = '111111111'
        and product_id = 123456
        and status in (1,2);
```

只是用一个索引时，MySQL会选择一个最优的索引来使用，比如使用 `index_order_no`，拿它来找出所有order_no为111111111的索引记录，从该索引上找到它的`PRIMARY`索引的`id`，然后回表找到对应的行数据，最后在内存中根据剩下的product_id和status条件来进行过滤。

但如果MySQL优化器觉得你如果只是用一个索引，拿出大量记录，然后再在内存中使用product_id和status过滤（并且符合该条件的记录值很少），这个第二步效率可能不高时，他就会使用索引合并进行优化。

如果使用索引合并去判断where条件时，那么它就会先通过 `index_order_no` 索引去找到`PRIMARY`索引的`id`，再通过 `index_product_id` 索引去找到`PRIMARY`索引的`id`，最后将两个id集合求交集，再回表找到行数据。(索引合并使用索引的顺序是不确定的)

#### X.5.2. 场景复现

在MySQL的Bug反馈文档中也有记录一个**Bug #77209**的记录，标注了索引合并引发死锁的情况。但是我按照它给出的repeat并不能重现索引合并的场景，在它的实例中早了600万随机数，我猜测可能是MySQL调高了索引合并的条件，将数据量增加到了1000万。

先来带大家复现一下当时的情况。

环境：MySQL 5.6.24

1. 创建一张测试表

   ```
   CREATE TABLE `a` (
     `ID` int  AUTO_INCREMENT PRIMARY KEY,
     `NAME` varchar(21),
     `STATUS` int,
     KEY `NAME` (`NAME`),
     KEY `STATUS` (`STATUS`)
   ) engine = innodb;
   ```

2. 导入数据，为了方便导入一些随机数据，需要先开启一个兼容性配置。

   ```SQL
   set global show_compatibility_56=on;  
   ```

   开始导入随机数据。

   ```
   set @N=0;
   insert into a(ID,NAME,STATUS)
   select
   	@N:=@N+1,
   	@N%1600000, 
   	floor(rand()*4)
    from information_schema.global_variables a, information_schema.global_variables b, information_schema.global_variables c 
   LIMIT 10000000;
   ```

3. 测试

   ```
   update a set status=5 where rand() < 0.005 limit 1;
   explain UPDATE a SET STATUS = 2 WHERE NAME =  '1000000' AND STATUS = 5;
   ```

![image-20210313003040573](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210313003040.png)



#### X.5.3. 为什么发生了死锁

直接上一副图，以及两个update事务的加锁流程。

![image-20210313003103060](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210313003103.png)

可以看到在订单与产品这个模型中，Update事务一和Update事物二在product_id索引和primary索引上都存在交叉重合，这就导致了死锁的发生。

| 步数 | 事务一                                                       | 事务二                                                       |
| ---- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 1    | 锁住`index_order_no`索引树上order_no为2222的索引项           |                                                              |
| 2    |                                                              | 锁住`index_order_no`索引树上order_no为3333的索引项           |
| 3    | 回表锁住 `PRIMARY` 索引中 id 为 11 的索引项                  |                                                              |
| 4    |                                                              | 回表锁住 `PRIMARY` 索引中 id 为 12 的索引项                  |
| 5    | 锁住`index_product_id`索引树上product_id为2000的四个索引项   |                                                              |
| 6    |                                                              | 尝试去锁住`index_product_id`索引树上product_id为2000的四个索引项，但是已经被事务一锁住，`等待事务一释放`在`index_product_id`上的锁 |
| 7    | 试图回表锁住 `PRIMARY` 索引中 id 为10，11，12，13的索引项，发现id为12的索引项在`第4步`已经被事务二锁住，`等待事务二释放`在 |                                                              |

这就是本次死锁发生的原因所在了，解决方案有很多种，可以根据具体场景选择。

1. 删除某一个索引，这当然不是一个好办法
2. 关闭index_merge优化
3. 为查询条件增加联合索引，在本例中是product_id和order_no。

#### X.5.4. 最后

当然最后这些都是我个人的分析，DBA老师给的建议是直接上联合索引

### X.6.Lock wait timeout exceeded该如何处理？

这个问题我相信大家对它并不陌生，但是有很多人对它产生的原因以及处理吃的不是特别透，很多情况都是交给DBA去定位和处理问题，接下来我们就针对这个问题来展开讨论。

Mysql造成锁的情况有很多，下面我们就列举一些情况：

1. 执行DML操作没有commit，再执行删除操作就会锁表。
2. 在同一事务内先后对同一条数据进行插入和更新操作。
3. 表索引设计不当，导致数据库出现死锁。
4. 长事物，阻塞DDL，继而阻塞所有同表的后续操作。

但是要区分的是`Lock wait timeout exceeded`与`Dead Lock`是不一样。

- `Lock wait timeout exceeded`：后提交的事务等待前面处理的事务释放锁，但是在等待的时候超过了mysql的锁等待时间，就会引发这个异常。
- `Dead Lock`：两个事务互相等待对方释放相同资源的锁，从而造成的死循环，就会引发这个异常。

还有一个要注意的是`innodb_lock_wait_timeout`与`lock_wait_timeout`也是不一样的。

- `innodb_lock_wait_timeout`：innodb的dml操作的行级锁的等待时间
- `lock_wait_timeout`：数据结构ddl操作的锁的等待时间

如何查看 `innodb_lock_wait_timeout` 的具体值？

```
SHOW VARIABLES LIKE 'innodb_lock_wait_timeout'
```

如何修改 `innode lock wait timeout` 的值？

参数修改的范围有 `Session` 和 `Global`，并且支持动态修改，可以有两种方法修改：

方法一：

通过下面语句修改

```
set innodb_lock_wait_timeout=100;
set global innodb_lock_wait_timeout=100;
```

*ps. 注意 `global` 的修改对当前线程是不生效的，只有建立新的连接才生效。*

方法二：

修改参数文件`/etc/my.cnf` `innodb_lock_wait_timeout = 50`

*ps. `innodb_lock_wait_timeout`指的是事务等待获取资源等待的最长时间，超过这个时间还未分配到资源则会返回应用失败； 当锁等待超过设置时间的时候，就会报如下的错误；`ERROR 1205 (HY000): Lock wait timeout exceeded; try restarting transaction`。其参数的时间单位是秒，最小可设置为1s(一般不会设置得这么小)，最大可设置1073741824秒，默认安装时这个值是50s(默认参数设置)。*

下面介绍在遇到这类问题该如何处理

#### X.6.1.问题现象

- 数据更新或新增后数据经常自动回滚。
- 表操作总报 `Lock wait timeout exceeded` 并长时间无反应

#### X.6.2.解决方法

- 应急方法：`show full processlist;` `kill`掉出现问题的进程。 *ps.有的时候通过processlist是看不出哪里有锁等待的，当两个事务都在commit阶段是无法体现在processlist上*
- 根治方法：`select * from innodb_trx;`查看有是哪些事务占据了表资源。 *ps.通过这个办法就需要对innodb有一些了解才好处理*

说起来很简单找到它杀掉它就搞定了，但是实际上并没有想象的这么简单，当问题出现要分析问题的原因，通过原因定位业务代码可能某些地方实现的有问题，从而来避免今后遇到同样的问题。

#### X.6.3.innodb_*表的解释

`Mysql` 的 `InnoDB` 存储引擎是支持事务的，事务开启后没有被主动 `Commit`。导致该资源被长期占用，其他事务在抢占该资源时，因上一个事务的锁而导致抢占失败！因此出现 `Lock wait timeout exceeded`

下面几张表是 `innodb` 的事务和锁的信息表，理解这些表就能很好的定位问题。

`innodb_trx` ## 当前运行的所有事务 `innodb_locks` ## 当前出现的锁 `innodb_lock_waits` ## 锁等待的对应关系

下面对 `innodb_trx` 表的每个字段进行解释：

```
trx_id：事务ID。
trx_state：事务状态，有以下几种状态：RUNNING、LOCK WAIT、ROLLING BACK 和 COMMITTING。
trx_started：事务开始时间。
trx_requested_lock_id：事务当前正在等待锁的标识，可以和 INNODB_LOCKS 表 JOIN 以得到更多详细信息。
trx_wait_started：事务开始等待的时间。
trx_weight：事务的权重。
trx_mysql_thread_id：事务线程 ID，可以和 PROCESSLIST 表 JOIN。
trx_query：事务正在执行的 SQL 语句。
trx_operation_state：事务当前操作状态。
trx_tables_in_use：当前事务执行的 SQL 中使用的表的个数。
trx_tables_locked：当前执行 SQL 的行锁数量。
trx_lock_structs：事务保留的锁数量。
trx_lock_memory_bytes：事务锁住的内存大小，单位为 BYTES。
trx_rows_locked：事务锁住的记录数。包含标记为 DELETED，并且已经保存到磁盘但对事务不可见的行。
trx_rows_modified：事务更改的行数。
trx_concurrency_tickets：事务并发票数。
trx_isolation_level：当前事务的隔离级别。
trx_unique_checks：是否打开唯一性检查的标识。
trx_foreign_key_checks：是否打开外键检查的标识。
trx_last_foreign_key_error：最后一次的外键错误信息。
trx_adaptive_hash_latched：自适应散列索引是否被当前事务锁住的标识。
trx_adaptive_hash_timeout：是否立刻放弃为自适应散列索引搜索 LATCH 的标识。
```

下面对 `innodb_locks` 表的每个字段进行解释：

```
lock_id：锁 ID。
lock_trx_id：拥有锁的事务 ID。可以和 INNODB_TRX 表 JOIN 得到事务的详细信息。
lock_mode：锁的模式。有如下锁类型：行级锁包括：S、X、IS、IX，分别代表：共享锁、排它锁、意向共享锁、意向排它锁。表级锁包括：S_GAP、X_GAP、IS_GAP、IX_GAP 和 AUTO_INC，分别代表共享间隙锁、排它间隙锁、意向共享间隙锁、意向排它间隙锁和自动递增锁。
lock_type：锁的类型。RECORD 代表行级锁，TABLE 代表表级锁。
lock_table：被锁定的或者包含锁定记录的表的名称。
lock_index：当 LOCK_TYPE=’RECORD’ 时，表示索引的名称；否则为 NULL。
lock_space：当 LOCK_TYPE=’RECORD’ 时，表示锁定行的表空间 ID；否则为 NULL。
lock_page：当 LOCK_TYPE=’RECORD’ 时，表示锁定行的页号；否则为 NULL。
lock_rec：当 LOCK_TYPE=’RECORD’ 时，表示一堆页面中锁定行的数量，亦即被锁定的记录号；否则为 NULL。
lock_data：当 LOCK_TYPE=’RECORD’ 时，表示锁定行的主键；否则为NULL。
```

下面对 `innodb_lock_waits` 表的每个字段进行解释：

```
requesting_trx_id：请求事务的 ID。
requested_lock_id：事务所等待的锁定的 ID。可以和 INNODB_LOCKS 表 JOIN。
blocking_trx_id：阻塞事务的 ID。
blocking_lock_id：某一事务的锁的 ID，该事务阻塞了另一事务的运行。可以和 INNODB_LOCKS 表 JOIN。
```

#### X.6.4.锁等待的处理步骤

- 直接查看 `innodb_lock_waits` 表

  ```
  SELECT * FROM innodb_lock_waits;
  ```

- `innodb_locks` 表和 `innodb_lock_waits` 表结合：

  ```
  SELECT * FROM innodb_locks WHERE lock_trx_id IN (SELECT blocking_trx_id FROM innodb_lock_waits);
  ```

- `innodb_locks` 表 `JOIN` `innodb_lock_waits` 表:

  ```
  SELECT innodb_locks.* FROM innodb_locks JOIN innodb_lock_waits ON (innodb_locks.lock_trx_id = innodb_lock_waits.blocking_trx_id);
  ```

- 查询 `innodb_trx` 表:

  ```
  SELECT trx_id, trx_requested_lock_id, trx_mysql_thread_id, trx_query FROM innodb_trx WHERE trx_state = 'LOCK WAIT';
  ```

- `trx_mysql_thread_id` 即 `kill` 掉事务线程 ID

  ```
  SHOW ENGINE INNODB STATUS ;
  SHOW PROCESSLIST ;
  ```

从上述方法中得到了相关信息，我们可以得到发生锁等待的线程 `ID`，然后将其 `KILL` 掉。`KILL` 掉发生锁等待的线程。

```
kill ID;
```