[toc]



# MySQL 执行原理



## 1.MySQL 逻辑架构

MySQL是一个开放源代码的关系数据库管理系统。原开发者为瑞典的MySQL AB公司，最早是在2001年MySQL3.23进入到管理员的视野并在之后获得广泛的应用。

当MySQL启动（MySQL服务器就是一个进程），等待客户端连接，每一个客户端连接请求，服务器都会新建一个线程处理（如果是线程池的话，则是分配一个空的线程），每个线程独立，拥有各自的内存处理空间。

MySQL总体上可分为**Server层**和**存储引擎层**。

Server层包括**连接器**、**查询器**、**分析器**、**优化器**、**执行器**等，涵盖 MySQL 的大多数核心服务功能，以及所有的内置函数（如日期、时间、数学和加密函数等），所有跨存储引擎的功能都在这一层实现，比如存储过程、触发器、视图等。

存储引擎层负责数据的存储和提取。其架构模式是插件式的，支持 InnoDB、MyISAM、Memory 等多个存储引擎。

如果能在头脑中构建一幅MySQL各组件之间如何协同工作的架构图，有助于深入理解MySQL服务器。下图展示了MySQL的逻辑架构图。

![y250mu1ncx](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307191701.png)

MySQL 整体上可以分为 Server 层和存储引擎层两部分。详细的分层如下：

1. **客户端层**： 连接处理、授权认证、安全等功能均在这一层处理。包含本地sock通信和大多数基于客户端/服务端工具实现的类似于tcp/ip的通信。主要完成一些类似于连接处理、授权认证、及相关的安全方案。在该层上引入了线程池的概念，为通过认证安全接入的客户端提供线程。同样在该层上可以实现基于SSL的安全链接。服务器也会为安全接入的每个客户端验证它所具有的操作权限。

2. **核心服务层**：查询解析、分析、优化、缓存、内置函数(比如：时间、数学、加密等函数)等。该层架构主要完成核心服务功能，如SQL接口，并完成缓存的查询，SQL的分析和优化及部分内置函数的执行。所有跨存储引擎的功能也在这一层实现，如过程、函数等。在该层，服务器会解析查询并创建相应的内部解析树，并对其完成相应的优化如确定查询表的顺序，是否利用索引等，最后生成相应的执行操作。如果是select语句，服务器还会查询内部的缓存。如果缓存空间足够大，这样在解决大量读操作的环境中能够很好的提升系统的性能。

3. **存储引擎层**：存储过程、触发器、视图等。存储引擎真正的负责了MySQL中数据的存储和提取，服务器通过API与存储引擎进行通信。不同的存储引擎具有的功能不同，这样我们可以根据自己的实际需要进行选取。

4. **数据存储层**：主要是将数据存储在运行于裸设备的文件系统之上，并完成与存储引擎的交互。

最下层为存储引擎，其负责MySQL中的数据存储和提取。和Linux下的文件系统类似，每种存储引擎都有其优势和劣势。中间的服务层通过API与存储引擎通信，这些API接口屏蔽了不同存储引擎间的差异。

![image-20210307215458247](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210307215458247.png)



## 2.一条 sql 的执行过程详解

### 2.1.写操作执行过程

如果这条sql是写操作(insert、update、delete)，那么大致的过程如下，其中引擎层是属于 InnoDB 存储引擎的，因为InnoDB 是默认的存储引擎，也是主流的，所以这里只说明 InnoDB 的引擎层过程。由于写操作较查询操作更为复杂，所以先看一下写操作的执行图。方便后面解析。

![2012006-20201203220840727-213780904](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307184101.png)



### 2.2.组件介绍 

#### 2.2.1.Server层

**2.2.1.1.连接器**

- 负责与客户端的通信，是半双工模式，这就意味着某一固定时刻只能由客户端向服务器请求或者服务器向客户端发送数据，而不能同时进行。

- 验证用户名和密码是否正确（数据库mysql的user表中进行验证），如果错误返回错误通知（`deAcess nied for user 'root'@'localhost'（using password：YES）`），如果正确，则会去 mysql 的权限表（mysql中的 user、db、columns_priv、Host 表，分别存储的是全局级别、数据库级别、表级别、列级别、配合 db 的数据库级别）查询当前用户的权限。

 

**2.2.1.2.缓存（Cache）**

也称为查询缓存，存储的数据是以键值对的形式进行存储，如果开启了缓存，那么在一条查询sql语句进来时会先判断缓存中是否包含当前的sql语句键值对，如果存在直接将其对应的结果返回，如果不存在再执行后面一系列操作。如果没有开启则直接跳过。

**相关操作**：

查看缓存配置：`show variables like 'have_query_cache';`

查看是否开启：`show variables like 'query_cache_type';`

查看缓存占用大小：`show variables like 'query_cache_size';`

查看缓存状态信息：`show status like 'Qcache%';`

![2012006-20201126164635848-1359264994](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307184402.png)

相关参数的含义：

![2012006-20201126164721379-1600241288](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307184503.png)

 **缓存失效场景**：

- 查询语句不一致。前后两条查询SQL必须完全一致。
- 查询语句中含有一些不确定的值时，则不会缓存。比如 `now()`、`current_date()`、`curdate()`、`curtime()`、`rand()`、`uuid()`等。
- 不使用任何表查询。如 `select 'A';`
- 查询 `mysql`、`information_schema` 或 `performance_schema` 数据库中的表时，不会走查询缓存。
- 在存储的函数，触发器或事件的主体内执行的查询。
- **如果表更改，则使用该表的所有高速缓存查询都变为无效并从缓存中删除，这包括使用 MERGE 映射到已更改表的表的查询。一个表可以被许多类型的语句改变，如 insert、update、delete、truncate rable、alter table、drop table、drop database。**

 通过上面的失效场景可以看出缓存是很容易失效的，所以如果不是查询次数远大于修改次数的话，使用缓存不仅不能提升查询效率还会拉低效率（每次读取后需要向缓存中保存一份，而缓存又容易被清除）。所以在 MYSQL5.6默认是关闭缓存的，并且在 8.0 直接被移除了。当然，如果场景需要用到，还是可以使用的。

**开启**

在配置文件(linux下是安装目录的cnf文件，windows是安装目录下的ini文件)中，增加配置：`query_cache_type = 1`

关于 `query_type_type` 参数的说明：

![2012006-20201126171630000-652492053](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307184604.png)

指定 SQL_NO_CACHE：`select SQL_NO_CACHE * from student where age >20; `， SQL_CACHE 同理。

 

**2.2.1.3.分析器**

对客户端传来的 sql 进行分析，这将包括预处理与解析过程，并进行关键词的提取、解析，并组成一个解析树。具体的解析词包括但不局限于 `select/update/delete/or/in/where/group by/having/count/limit` 等，如果分析到语法错误，会直接抛给客户端异常：`ERROR:You have an error in your SQL syntax.`

比如：`select * from user where userId =1234;`

在分析器中就通过语义规则器将 `select from where` 这些关键词提取和匹配出来,mysql会自动判断关键词和非关键词，将用户的匹配字段和自定义语句识别出来。这个阶段也会做一些校验:比如校验当前数据库是否存在user表，同时假如User表中不存在userId这个字段同样会报错：`unknown column in field list.`

 

**2.2.1.4.优化器**

进入优化器说明sql语句是符合标准语义规则并且可以执行。优化器会根据执行计划选择最优的选择，匹配合适的索引，选择最佳的方案。比如一个典型的例子是这样的：

表T,对A、B、C列建立联合索引(A,B,C)，在进行查询的时候，当sql查询条件是: `select xx where  B=x and A=x and C=x`. 很多人会以为是用不到索引的，但其实会用到,虽然索引必须符合最左原则才能使用,但是本质上,优化器会自动将这条sql优化为: `where A=x and B=x and C=X`,这种优化会为了底层能够匹配到索引，同时在这个阶段是自动按照执行计划进行预处理,mysql会计算各个执行方法的最佳时间,最终确定一条执行的sql交给最后的执行器。

优化器会根据扫描行数、是否使用临时表、是否排序等来判断是否使用某个索引，其中扫描行数的计算可以通过统计信息来估算得出，而统计信息可以看作是索引唯一数的数量，可以使用部分采样来估算，具体就是选择 N 个数据页，统计这些页上数据的不同值，得到一个平均值，然后乘以这个索引的页面数，就得到了。但是因为索引数据会变化，所以索引的统计信息也会变化。当变更的数据行数超过 1/M 的时候，就会重新计算一次统计信息。

**关于统计信息可以选择是否持久化：**

通过 `innodb_stats_persistent` ：设置为 on 的时候，表示统计信息会持久化存储。这时，默认的 N 是 20，M 是 10。设置为 off 的时候，表示统计信息只存储在内存中。这时，默认的 N 是 8，M 是 16。

**没有使用最优索引如何优化：**

- 虽然会自动更新统计信息，但是但是不能保证统计信息是最新值，这就可能导致优化器选择了不同的索引导致执行变慢，所以可以通过 `analyze table 表名` 来重新计算索引的统计信息。
- 在表名后面添加 `force index(索引名) ` 语句来强制使用索引
- 将 sql 进行修改成优化器可以选最优索引的实现方式。
- 新建一个最优索引或者删除优化器误用的索引。

 

**2.2.1.5.执行器**

执行器会调用对应的存储引擎执行 sql。主流的是MyISAM 和 Innodb。

![2012006-20201126175323253-2016884073](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307184805.png)



#### 2.2.2.存储引擎（InnoDB）层

**2.2.2.1.undo log 与 MVCC**

undo log是 Innodb 引擎专属的日志，是**记录每行数据事务执行前的数据**。主要作用是用于实现MVCC版本控制，保证事务隔离级别的读已提交和读未提交级别。

 

**2.2.2.2.redo log 与 Buffer Pool**

InnoDB 内部维护了一个缓冲池，用于减少对磁盘数据的直接IO操作，并配合 redo log、内部的 change buffer 来实现异步的落盘，保证程序的高效执行。redo log 大小固定，采用循环写

![2012006-20201202094232901-1051915905](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307185006.png)

write pos 表示当前正在记录的位置，会向后记录， checkpoint 表示数据落盘的边界，也就是 checkpoint 与 write pos中间是已记录的，当 write pos写完 id_logfile_3后，会回到id_logfile_0循环写，而追上 checkpomnit 后则需要先等数据进行落盘，等待 checkponit向后面移动一段距离再写。**redo log存储的内容是对数据页的修改逻辑以及 change buffer 的变更。**

 

**2.2.2.3.bin log(Server 层)**

redo log 因为大小固定，所以不能存储过多的数据，它只能用于未更新的数据落盘，而数据操作的备份恢复、以及主从复制是靠 bin log（如果数据库误删需要还原，那么需要某个时间点的数据备份以及bin log）。**5.7默认记录的是操作语句涉及的每一行修改前后的行记录。**

在更新到数据页缓存或者 Change Buffer 后，**首先进行 redo log 的编写，编写完成后将 redo log 设为 prepare 状态，随后再进行 binlog 的编写，等到 binlog 也编写完成后再将 redo log 设置为 commit 状态**。这是为了防止数据库宕机导致 binlog 没有将修改记录写入，后面数据恢复、主从复制时数据不一致。在断电重启后先检查 redo log 记录的事务操作是否为 commit 状态：

- 如果是 commit 状态说明没有数据丢失，判断下一个。

- 如果是 prepare 状态，检查 binlog 记录的对应事务操作（redo log 与 binlog 记录的事务操作有一个共同字段 XID，redo log 就是通过这个字段找到 binlog 中对应的事务的）是否完整（这点在前面 binlog 三种格式分析过，每种格式记录的事务结尾都有特定的标识），如果完整就将 redo log 设为 commit 状态，然后结束；不完整就回滚 redo log 的事务，结束。

**三种格式：**

- Row（5.7默认）。记录操作语句对具体行的操作以及操作前的整行信息。缺点是占空间大。优点是能保证数据安全，不会发生遗漏。

- Statement。记录修改的 sql。缺点是在 mysql 集群时可能会导致操作不一致从而使得数据不一致（比如在操作中加入了Now()函数，主从数据库操作的时间不同结果也不同）。优点是占空间小，执行快。

- Mixed。会针对于操作的 sql 选择使用Row 还是 Statement。缺点是还是可能发生主从不一致的情况。

 

#### 2.2.3.三个日志的比较（undo、redo、bin）

1. undo log是用于事务的回滚、保证事务隔离级别读已提交、可重复读实现的。redo log是用于对暂不更新到磁盘上的操作进行记录，使得其可以延迟落盘，保证程序的效率。bin log是对数据操作进行备份恢复（并不能依靠 bin log 直接完成数据恢复）。
2. undo log 与 redo log 是存储引擎层的日志，只能在 InnoDB 下使用；而bin log 是 Server 层的日志，可以在任何引擎下使用。
3. redo log 大小有限，超过后会循环写；另外两个大小不会。
4. undo log 记录的是行记录变化前的数据；redo log 记录的是 sql 的数据页修改逻辑以及 change buffer 的变更；bin log记录操作语句对具体行的操作以及操作前的整行信息（5.7默认）或者sql语句。
5. 单独的 binlog 没有 crash-safe 能力，也就是在异常断电后，之前已经提交但未更新的事务操作到磁盘的操作会丢失，也就是主从复制的一致性无法保障，而 redo log 有 crash-safe 能力，通过与 redo log 的配合实现 "三步提交"，就可以让主从库的数据也能保证一致性。
6. redo log 是物理日志，它记录的是数据页修改逻辑以及 change buffer 的变更，只能在当前存储引擎下使用，而 binlog 是逻辑日志，它记录的是操作语句涉及的每一行修改前后的值，在任何存储引擎下都可以使用。

**MySQL 是 WAL（Write-Ahead Logging）机制，也就是写操作会先存入日志，然后再写入磁盘，这样可以避开高峰，提高数据库的可用性。**



#### 2.2.4.两阶段提交

redo log 的写入拆成了两个步骤：prepare 和 commit，这就是"两阶段提交"。

**为什么必须有“两阶段提交”呢？**

这是为了让两份日志之间的逻辑一致。

由于 redo log 和 binlog 是两个独立的逻辑，如果不用两阶段提交，要么就是先写完 redo log 再写 binlog，或者采用反过来的顺序。我们看看这两种方式会有什么问题。

仍然用前面的 update 语句来做例子。假设当前 ID=2 的行，字段 c 的值是 0，再假设执行 update语句过程中在写完第一个日志后，第二个日志还没有写完期间发生了 crash，会出现什么情况呢？

**情况 1 ：先写 redo log 后写 binlog。**

假设在 redo log 写完，binlog 还没有写完的时候，MySQL 进程异常重启。由于我们前面说过的，redo log 写完之后，系统即使崩溃，仍然能够把数据恢复回来，所以恢复后这一行 c 的值是 1。但是由于 binlog 没写完就 crash 了，这时候 binlog 里面就没有记录这个语句。因此，之后备份日志的时候，存起来的 binlog 里面就没有这条语句。然后你会发现，如果需要用这个 binlog 来恢复临时库的话，由于这个语句的 binlog 丢失，这个临时库就会少了这一次更新，恢复出来的这一行 c 的值就是 0，与原库的值不同。

**情况 2：先写 binlog 后写 redo log。**

如果在 binlog 写完之后 crash，由于 redo log 还没写，崩溃恢复以后这个事务无效，所以这一行 c 的值是 0。但是 binlog 里面已经记录了“把 c 从 0 改成 1”这个日志。所以，在之后用 binlog 来恢复的时候就多了一个事务出来，恢复出来的这一行 c 的值就是 1，与原库的值不同。

可以看到，如果不使用“两阶段提交”，那么数据库的状态就有可能和用它的日志恢复出来的库的状态不一致。

 

### 2.3.执行过程

#### 2.3.1.写操作

 通过上面的分析，可以很容易地了解开始的更新执行图。这里就不过多阐述了。

 

#### 2.3.2.读操作

查询的过程和更新比较相似，但是有些不同，主要是来源于他们在查找筛选时的不同，更新因为在查找后会进行更新操作，所以查询这一行为至始至终都在缓冲池中（使用到索引且缓冲池中包含数据对应的数据页）。而查询则更复杂一些。



**2.3.2.1.Where 条件的提取**

在 MySQL 5.6开始，引入了一种索引优化策略——索引下推，其本质优化的就是 Where 条件的提取。Where 提取过程是怎样的？用一个例子来说明，首先进行建表，插入记录。

```
create table tbl_test (a int primary key, b int, c int, d int, e varchar(50));
create index idx_bcd on tbl_test(b, c, d);
insert into tbl_test values (4,3,1,1,'a');
insert into tbl_test values (1,1,1,2,'d');
insert into tbl_test values (8,8,7,8,'h');
insert into tbl_test values (2,2,1,2,'g');
insert into tbl_test values (5,2,2,5,'e');
insert into tbl_test values (3,3,2,1,'c');
insert into tbl_test values (7,4,0,5,'b');
insert into tbl_test values (6,5,2,4,'f');
```

那么执行 `select * from tbl_test where b >= 2 and b < 7 and c > 0 and d != 2 and e != 'a';` 在提取时，会将 Where 条件拆分为 **Index Key（First Key & Last Key）、Index Filter 与 Table Filter**。

**2.3.2.1.1.Index Key**

用于确定 SQL 查询在索引中的连续范围（起始点 + 终止点）的查询条件，被称之为Index Key；由于一个范围，至少包含一个起始条件与一个终止条件，因此 Index Key 也被拆分为 Index First Key 和 Index Last Key，分别用于定位索引查找的起始点以终止点

**Index First Key**

用于确定索引查询范围的起始点；提取规则：从索引的第一个键值开始，检查其在 where 条件中是否存在，若存在并且条件是 `=`、`>=`，则将对应的条件加入Index First Key之中，继续读取索引的下一个键值，使用同样的提取规则；若存在并且条件是 >，则将对应的条件加入 Index First Key 中，同时终止 Index First Key 的提取；若不存在，同样终止 Index First Key 的提取

针对 SQL：`select * from tbl_test where b >= 2 and b < 7 and c > 0 and d != 2 and e != 'a'`，应用这个提取规则，提取出来的 Index First Key 为 `b >= 2`, `c > 0`，由于 c 的条件为 `>`，提取结束

**Index Last Key**

用于确定索引查询范围的终止点，与 Index First Key 正好相反；提取规则：从索引的第一个键值开始，检查其在 where 条件中是否存在，若存在并且条件是 `=`、`<=`，则将对应条件加入到 Index Last Key 中，继续提取索引的下一个键值，使用同样的提取规则；若存在并且条件是 `<` ，则将条件加入到 Index Last Key 中，同时终止提取；若不存在，同样终止Index Last Key的提取

针对 SQL：`select * from tbl_test where b >= 2 and b < 7 and c > 0 and d != 2 and e != 'a'`，应用这个提取规则，提取出来的 Index Last Key为 `b < 7` ，由于是 `<` 符号，提取结束

 

**2.3.2.1.2.Index Filter**

在完成 Index Key 的提取之后，我们根据 where 条件固定了索引的查询范围，那么是不是在范围内的每一个索引项都满足 WHERE 条件了 ？ 很明显 **4**,**0**,**5** ， **2**,**1**,**2** 均属于范围中，但是又均不满足SQL 的查询条件

所以 Index Filter 用于索引范围确定后，确定 SQL 中还有哪些条件可以使用索引来过滤；提取规则：**从索引列的第一列开始，检查其在 where 条件中是否存在，若存在并且 where 条件仅为 =，则跳过第一列继续检查索引下一列，下一索引列采取与索引第一列同样的提取规则；若 where 条件为 >=、>、<、<= 其中的几种，则跳过索引第一列，将其余 where 条件中索引相关列全部加入到 Index Filter 之中；若索引第一列的 where 条件包含 =、>=、>、<、<= 之外的条件，则将此条件以及其余 where 条件中索引相关列全部加入到 Index Filter 之中；若第一列不包含查询条件，则将所有索引相关条件均加入到 Index Filter之中**

针对 SQL：`select * from tbl_test where b >= 2 and b < 7 and c > 0 and d != 2 and e != 'a'`，应用这个提取规则，提取出来的 Index Filter 为 `c > 0 and d != 2` ，因为索引第一列只包含 `>=`、`<` 两个条件，因此第一列跳过，将余下的 c、d 两列加入到 Index Filter 中，提取结束

 

**2.3.2.1.3.Table Filter**

这个就比较简单了，where 中不能被索引过滤的条件都归为此中；提取规则：所有不属于索引列的查询条件，均归为 Table Filter 之中

针对 SQL：`select * from tbl_test where b >= 2 and b < 7 and c > 0 and d != 2 and e != 'a'`，应用这个提取规则，那么 Table Filter 就为 `e != 'a' `

在5.6 之前，是不分 Table Filter 与 Index Filter 的，这两个条件都直接分配到 Server 层进行筛选。筛选过程是先根据 Index Key 的条件先在引擎层进行初步筛选，然后得到对应的主键值进行回表查询得到初筛的行记录，传入 Server 层进行后续的筛选，在 Server 层的筛选因为没有用到索引所以会进行全表扫描。而索引下推的优化就是将 Index Filter 的条件下推到引擎层，在使用 Index First Key 与 Index Last Key 进行筛选时，就带上 Index Filter 的条件再次筛选，以此来过滤掉不符合条件的记录对应的主键值，减少回表的次数，同时发给 Server 层的记录也会更少，全表扫描筛选的效率也会变高。下面是未使用索引下推和使用索引下推的示意图。

**未使用索引下推:**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307185407.png)

**使用索引下推:**

​				 ![2012006-20201202180024735-291134037](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307185508.png)

索引下推：

开启：`set optimizer_switch='index_condition_pushdown=on';`　　  

查看 ：`show variables like 'optimizer_switch';`

 

**从上面的分析来看，查询的流程图大致可以用下面这张图来概括**

 ![2012006-20201203220240808-1368483593](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307185709.png)

 这里要注意的是如果在一开始没有用到索引，会依次将磁盘上的数据页读取到缓冲池中进行查询。

 

#### 2.3.3.SQL执行顺序

最后需要注意的是 SQL 语句关键词的解析执行顺序：

![2012006-20210114202416943-1713966410](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307185810.png)

 



## X.常见问题

### X.1.为什么MySQL8.0直接把查询缓存的功能移除了呢？

一种说法是不建议使用查询缓存，因为查询缓存往往弊大于利。

查询缓存的失效非常频繁，只要有对一个表的更新，这个表上的所有的查询缓存都会被清空。

因此很可能你费劲地把结果存起来，还没使用呢，就被一个更新全清空了。对于更新压力大的数据库来说，查询缓存的命中率会非常低。除非你的业务有一张静态表，很长时间更新一次，比如系统配置表，那么这张表的查询才适合做查询缓存。

在我看来，大多数应用都把缓存做到了应用逻辑层，简单的如一个map的mybatis，复杂的可以用redis或者memcache，直接操作内存远远比走网络访问快，所以mysql直接抛弃了查询缓存？





