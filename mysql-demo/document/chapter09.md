[toc]

# MySQL 性能优化



## 1.性能优化原则和分类

**性能优化一般可以分为：**

- 主动优化

  所谓的主动优化是指不需要外力的推动而自发进行的一种行为，比如当服务没有明显的卡顿、宕机或者硬件指标异常的情况下，自我出发去优化的行为，就可以称之为主动优化。

- 被动优化

  而被动优化刚好与主动优化相反，它是指在发现了服务器卡顿、服务异常或者物理指标异常的情况下，才去优化的这种行为。

**性能优化原则**

无论是主动优化还是被动优化都要符合以下性能优化的原则：

1. 优化不能改变服务运行的逻辑，要保证服务的**正确性**；
2. 优化的过程和结果都要保证服务的**安全性**；
3. 要保证服务的**稳定性**，不能为了追求性能牺牲程序的稳定性。比如不能为了提高 Redis 的运行速度，而关闭持久化的功能，因为这样在 Redis 服务器重启或者掉电之后会丢失存储的数据。

以上原则看似都是些废话，但却给了我们一个启发，那就是我们**性能优化手段应该是：预防性能问题为主 + 被动优化为辅**。

也就是说，我们应该**以预防性能问题为主**，在开发阶段尽可能的规避性能问题，而**在正常情况下，应尽量避免主动优化，以防止未知的风险**（除非是为了 KPI，或者是闲的没事），尤其对生产环境而言更是如此，最后才是考虑**被动优化**。

> PS：当遇到性能缓慢下降、或硬件指标缓慢增加的情况，如今天内存的占用率是 50%，明天是 70%，后天是 90% ，并且丝毫没有收回的迹象时，我们应该提早发现并处理此类问题（这种情况也属于被动优化的一种）。



## 2.什么影响了数据库查询速度
### 2.1.影响数据库查询速度的四个因素
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307113301.png)
### 2.2.风险分析
> QPS： QueriesPerSecond意思是“每秒查询率”，是一台服务器每秒能够相应的查询次数，是对一个特定的查询服务器在规定时间内所处理流量多少的衡量标准。
>
> TPS： 是 TransactionsPerSecond的缩写，也就是事务数/秒。它是软件测试结果的测量单位。客户机在发送请求时开始计时，收到服务器响应后结束计时，以此来计算使用的时间和完成的事务个数。

Tips： 最好不要在主库上数据库备份，大型活动前取消这样的计划。

1. 效率低下的 sql：超低的 QPS与 TPS。

2. 大量的并发：数据连接数被占满（ `max_connection` 默认 100，一般把连接数设置得大一些）。 

   并发量:同一时刻数据库服务器处理的请求数量

3. 超高的 CPU使用率： CPU资源耗尽出现宕机。

4. 磁盘 IO：磁盘 IO性能突然下降、大量消耗磁盘性能的计划任务。解决：更快磁盘设备、调整计划任务、做好磁盘维护。
### 2.3.网卡流量：如何避免无法连接数据库的情况
- 减少从服务器的数量（从服务器会从主服务器复制日志）

- 进行分级缓存（避免前端大量缓存失效）

- 避免使用 select* 进行查询

- 分离业务网络和服务器网络

### 2.4.大表带来的问题
**大表的特点**

- 记录行数巨大，单表超千万
- 表数据文件巨大，超过 10个 G

**大表的危害**

- 慢查询：很难在短时间内过滤出需要的数据

  查询字段区分度低 -> 要在大数据量的表中筛选出来其中一部分数据会产生大量的磁盘 io -> 降低磁盘效率

- 对 DDL影响：
  - 建立索引需要很长时间：
  MySQL-v<5.5 建立索引会锁表
  MySQL-v>=5.5 建立索引会造成主从延迟（ mysql建立索引，先在组上执行，再在库上执行）
  - 修改表结构需要长时间的锁表：会造成长时间的主从延迟('480秒延迟')

**如何处理数据库上的大表**

> 分库分表把一张大表分成多个小表

难点：
1. 分表主键的选择
2. 分表后跨分区数据的查询和统计

### 2.5.大事务带来的问题
**什么是事务**

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307113302.png)

**大事务**

> 运行时间长，操作数据比较多的事务；

风险：锁定数据太多，回滚时间长，执行时间长。
- 锁定太多数据，造成大量阻塞和锁超时；
- 回滚时所需时间比较长，且数据仍然会处于锁定；
- 如果执行时间长，将造成主从延迟，因为只有当主服务器全部执行完写入日志时，从服务器才会开始进行同步，造成延迟。

解决思路：
- 避免一次处理太多数据，可以分批次处理；
- 移出不必要的SELECT操作，保证事务中只有必要的写操作。



## 3.什么影响了MySQL性能

### 3.1.影响性能的几个方面
1. 服务器硬件。
2. 服务器系统（系统参数优化）。
3. 存储引擎。 MyISAM： 不支持事务，表级锁。 InnoDB: 支持事务，支持行级锁，事务 ACID。
4. 数据库参数配置。
5. 数据库结构设计和SQL语句。（重点优化）

### 3.2.MySQL体系结构
分三层：客户端->服务层->存储引擎  

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307113403.png)



1. MySQL是插件式的存储引擎，其中存储引擎分很多种。只要实现符合mysql存储引擎的接口，可以开发自己的存储引擎!
2. 所有跨存储引擎的功能都是在服务层实现的。
3. MySQL的存储引擎是针对表的，不是针对库的。也就是说在一个数据库中可以使用不同的存储引擎。但是不建议这样做。
### 3.3.如何选择正确的存储引擎
参考条件：
1. 事务
2. 备份( Innobd免费在线备份)
3. 崩溃恢复
4. 存储引擎的特有特性

总结: Innodb 大法好。

注意: 尽量别使用混合存储引擎，比如回滚会出问题在线热备问题。

### 3.4.配置参数
**内存配置相关参数**

- 确定可以使用的内存上限。
- 内存的使用上限不能超过物理内存，否则容易造成内存溢出；（对于32位操作系统，MySQL只能试用3G以下的内存。）
- 确定MySQL的每个连接单独使用的内存。

```
sort_buffer_size #定义了每个线程排序缓存区的大小，MySQL在有查询、需要做排序操作时才会为每个缓冲区分配内存（直接分配该参数的全部内存）；
join_buffer_size #定义了每个线程所使用的连接缓冲区的大小，如果一个查询关联了多张表，MySQL会为每张表分配一个连接缓冲，导致一个查询产生了多个连接缓冲；
read_buffer_size #定义了当对一张MyISAM进行全表扫描时所分配读缓冲池大小，MySQL有查询需要时会为其分配内存，其必须是4k的倍数；
read_rnd_buffer_size #索引缓冲区大小，MySQL有查询需要时会为其分配内存，只会分配需要的大小。
```
注意： 以上四个参数是为一个线程分配的，如果有100个连接，那么需要×100。

MySQL数据库实例：

- MySQL是 单进程多线程（而oracle是多进程），也就是说 MySQL实例在系统上表现就是一个服务进程，即进程；
- MySQL实例是线程和内存组成，实例才是真正用于操作数据库文件的；

> 一般情况下一个实例操作一个或多个数据库；集群情况下多个实例操作一个或多个数据库。

**如何为缓存池分配内存：**

- `Innodb_buffer_pool_size`，定义了Innodb所使用缓存池的大小，对其性能十分重要，必须足够大，但是过大时，使得Innodb 关闭时候需要更多时间把脏页从缓冲池中刷新到磁盘中；

    ```
    总内存-（每个线程所需要的内存*连接数）-系统保留内存
    ```

- `key_buffer_size`，定义了MyISAM所使用的缓存池的大小，由于数据是依赖存储操作系统缓存的，所以要为操作系统预留更大的内存空间；

    ```
    select sum(index_length) from information_schema.talbes where engine='myisam'
    ```
    
    注意： 即使开发使用的表全部是Innodb表，也要为MyISAM预留内存，因为MySQL系统使用的表仍然是MyISAM表。

- `max_connections` 控制允许的最大连接数， 一般2000更大。

### 3.5.性能优化顺序
从上到下：
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307113404.png)



## 4.待优化SQL定位步骤

### 4.1. 查看SQL语句的执行次数

在MySQL中可以通过命令查看服务器该表状态信息

```mysql
show status like 'Com_______';
```

![image-20210311221938276](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311221938.png)

如果想查看整个数据库信息

```mysql
show global status like 'Com_______';
```

下面这些对于所有存储引擎的表操作都会进行累计

- Com_select：执行 select 操作的次数，一次查询只累加 1。
- Com_insert：执行 INSERT 操作的次数，对于批量插入的 INSERT 操作，只累加一次。
- Com_update：执行 UPDATE 操作的次数。
- Com_delete：执行 DELETE 操作的次数。

有专门针对Innodb统计的，其中 `rows_read`代表的是读取的行数。

```mysql
show status like 'Innodb_rows_%';
```

![image-20210311222000785](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311222000.png)

对于事务型的应用，通过 Com_commit 和 Com_rollback 可以了解事务提交和回滚的情况， 对于回滚操作非常频繁的数据库，可能意味着应用编写存在问题。

------

### 4.2.定位执行效率较低的SQL语句

- 通过慢查询日志定位那些执行效率较低的 SQL 语句，用 `--log-slow-queries[=file_name]` 选 项启动时，mysqld 写一个包含所有执行时间超过 `long_query_time` 秒的 SQL 语句的日志 文件。

- 慢查询日志在查询结束以后才纪录，所以在应用反映执行效率出现问题的时候查询慢查 询日志并不能定位问题，可以使用show processlist命令查看当前MySQL在进行的线程， 包括线程的状态、是否锁表等，可以实时地查看 SQL 的执行情况，同时对一些锁表操 作进行优化。

  通过下面命令可以查看MySQL进程

  ![image-20210311224108052](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311224108.png)

  - **Id**：数据库连接id
  - **User**：显示当前用户
  - **Host**：从哪个ip的哪个端口上发的
  - **db**：数据库
  - **Command**：连接的状态，休眠（sleep），查询（query），连接（connect）

  - **Time**：秒
  - **State**：SQL语句执行状态，可能需要经过 `copying to tmp table`、`sorting result`、`sending data` 等状态才可以完成

  - **Info**：SQL语句

------

### 4.3.通过 EXPLAIN 分析低效SQL的执行计划

找到相应的SQL语句之后，可以EXPLALIN获取MySQL的执行信息。

![image-20210311224813933](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311224814.png)

其中每个列的解释：

- **id**：id相同表示加载表的执行顺序从上到下，id越大加载的优先级越高

- **select_type**：表示 SELECT 的类型，常见的取值有
- SIMPLE（简单表，即不使用表连接 或者子查询）
	- PRIMARY（主查询，即外层的查询）
	- UNION（UNION 中的第二个或 者后面的查询语句）
	- SUBQUERY（子查询中的第一个 SELECT）
	
- **table**：输出结果集的表

- **type**：表示表的连接类型，性能好到坏的结果

	- system(表中仅有一行，即常量表)
	- const（单表中最多有一个匹配行，只能查询出来一条）
	- eq_ref（对于前面的每一行，在此表中只有一条查询数据，类似于主键和唯一索引）
	- ref（与eq_ref类式，区别是不使用主键和唯一索引）
	- ref_ir_null（与ref类似，区别在于对NULL的查询）
	- index_merge（索引合并优化）
	- unique_subquery（in 的后面是一个查询主键字段的子查询）
	- index_subquery（与 unique_subquery 类似， 区别在于 in 的后面是查询非唯一索引字段的子查询）
	- range（单表中的范围查询）
	- index（对于前面的每一行，都通过查询索引来得到数据）
	- all（对于前面的每一行， 207 都通过全表扫描来得到数据）

- **possible_keys**：表示查询时，可能使用的索引。

- **key：**表示实际使用的索引

- **key_len：**索引字段的长度

- **rows：**扫描行的数量

- **Extra：**执行情况的说明和描述

根据以上内容创建 `Teacher`、 `Student`表，通过ClassID关联

```mysql
create table Teacher
(
   teacherId int not NULL AUTO_INCREMENT,
	 teacherName VARCHAR(50),
	 ClassID int,
	 primary key (teacherId)
) ENGINE =innodb DEFAULT charset=utf8;

create table Student
(
   StudentID int not NULL AUTO_INCREMENT,
	 ClassId int,
	 StudentName varchar(50),
	 primary key (StudentID)
) ENGINE = INNODB DEFAULT charset=utf8;

INSERT into Teacher(teacherName,ClassID) values("小李",204),("小刘",205),("小杨",206);

INSERT into Student(ClassId,StudentName) VALUES(204,"张三"),(205,"李四"),(206,"王五");
```

------

#### 4.3.1.explain-id

（1）、**Id相同表示执行顺序从上到下**

```mysql
EXPLAIN select * from Teacher t,Student s where t.ClassID=s.ClassID;
```

![image-20210311225923555](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311225923.png)

（2）、**Id不同表示，Id越大越先执行**

```mysql
explain  select *from Teacher where ClassId =( select ClassId from Student where StudentName='张三');
```

![image-20210311225944965](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311225945.png)

（3）、**Id有相同的也有不同的，先执行Id大的，再从上到下执行**。

------

#### 4.3.2.explain select_type

（1）、**SIMLPLE简单的select查询，不包含子查询或者UNION**

```mysql
explain select * from Teacher;
```

![image-20210311230004181](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230004.png)

（2）、**PRIMARY查询当中包含了子查询，最外层就是改查询的标记**

（3）、**SUBQUERY在select或者Where中包含了子查询**

```mysql
explain select *from Teacher where ClassId=(select ClassId from Student where StudentId=1);
```

![image-20210311230034638](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230034.png)

（4）、**DERIVED在form列表包含子查询**

```mysql
explain  select * from (select * from Student where Student.StudentID>2  )   a where a.ClassID=204;
```

![image-20210311230056203](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230056.png)

如果查询显示都是SIMLPLE是因为mysql5.7对 ***derived_merge*** 参数默认设置为on，也就是开启状态，我们在mysql5.7中把它关闭 shut downn 使用如下命令就可以了

```
set session optimizer_switch=`derived_merge=off`;
set global optimizer_switch=`derived_merge=off`;
```

（5）、**UNION 、UNION RESULT**

```mysql
explain select * from Student where StudentID=1  union select * from Student where StudentID=2;
```

![image-20210311230115238](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230115.png)

UNION指的是后面那个Select，UNION RESULT 将前面的select语句和后面的select联合起来。

------

#### 4.3.3.explain-type

（1）、**NULL直接返回结果，不访问任何表索引**

```mysql
select NOW();
```

![image-20210311230206699](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230206.png)

（2）、**system查询结果只有一条的数据,const类型的特例**

```
explain select * from (select * from Student where StudentID=1) a;
```

![image-20210311230227347](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230227.png)

（3）、**const根据主键或者唯一索引进行查询，表示一次就找到了**

```mysql
EXPLAIN select * from Student where StudentID=1;
```

![image-20210311230248320](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230248.png)

（4）、**eq_ref 索引是主键或者唯一索引，使用多表关联查询查询出来的数据只有一条**

```mysql
explain select * from Student s,Teacher t where  s.StudentID=t.teacherId 
```

![image-20210311230305331](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230305.png)

（5）、**ref 根据非唯一性的索引查询，返回的记录有多条，比如给某个字段添加索引**

```mysql
explain select * from Student s WHERE  StudentName='张三1';
```

![image-20210311230322516](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230322.png)

（6）、**range 范围查询 between <> in等操作，前提是用索引，要自己设定索引字段；**

```mysql
explain select * from Student where StudentID in (2,3);
```

![image-20210311230336544](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230336.png)

（7）、**index 遍历整个索引树,相当于查询了整张表的索引**

```mysql
explain select  StudentID from Student;
```

![image-20210311230349110](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230349.png)

（8）、**ALL 遍历所有数据文件**

```mysql
explain select  * from Student;
```

![image-20210311230400882](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230400.png)

通过这个Type就可以判断当前查询返回了多少行，有没有走索引还是走全表扫描

结果从最好到最坏

```
NULL > system > const > eq_ref > ref > fulltext > ref_or_null > index_merge > unique_subquery > index_subquery > range > index > ALL


system > const > eq_ref > ref > range > index > ALL
```

#### 4.3.4.explain-key

![image-20210311230422244](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230422.png)

（1）possible_keys：可能用到的索引

（2）key：实际用到的索引

（3）key_len：key的长度，越短越好

#### 4.3.5.explain-rows

sql语句执行扫描的行数

#### 4.3.6.explain-extra

（1）**using filesort** ：会对进行文件排序即内容，而不是按索引排序，效率慢

```mysql
EXPLAIN select *from Student order by StudentName;
```

![image-20210311230440071](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230440.png)

如果要优化的话可以对该字段建索引

（2）**using index** 根据根据索引直接查，避免访问表的数据行

```mysql
explain select StudentID from Student order by StudentID ;
```

![image-20210311230453902](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230453.png)

（3）**using temporary** 使用临时表保存结果，在没有索引的情况下，需要进行优化

```mysql
EXPLAIN select * from Teacher t GROUP BY teacherName;
```

![image-20210311230509139](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230509.png)

报错：Expression #1 of SELECT list is not in GROUP BY clause and contains nonaggregated column 'demo_01.Teacher.teacherName' which is not functionally dependent on columns in GROUP BY clause; this is incompatible with sql_mode=only_full_group_by

解决办法：
1、找到mysql的配置文件 my.ini (一般在mysql根目录)

2、在my.cn中将以下内容添加到 [mysqld]下

我的是：etc/my.cnf

sql_mode=NO_ENGINE_SUBSTITUTION,STRICT_TRANS_TABLES

### 4.4.show profile分析SQL

show profile可以分析sql运行的时间，通过 `have_profiling`可以查看MySQL是否支持profile

![image-20210311230547435](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230547.png)

默认profiling是关闭的，可以通过语句打开

```mysql
set profiling=1;//打开
```

![image-20210311230607503](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230607.png)

执行SQL语句之后乐意通过show profiles指令，来查看语句的耗时

```mysql
 show profiles;
```

![image-20210311230628619](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230628.png)

可以通过Show profile for query Query_id查看每个阶段的耗时

```
Show  profile for query 2;
```

![image-20210311230654140](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230654.png)

> 其中Sending data表示来说访问数据库并把结果返回给数据库的过程，MySQL需要做大量的磁盘读取操作，因此是最耗时的。

在知道最消耗时间的状态后，可以选择all、cpu、block to、context switch、page fault等明细查看在什么资源上浪费了时间

```
show profile cpu for query 2;
```

![image-20210311230714594](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230714.png)

### 4.5.trace分析优化器执行计划

Mysql有一个优化器按照规则对SQL进行优化处理，trace就是用来分析优化器的执行计划

首先开启trace开关，然后设置trace文件占用的内存空间

```mysql
set optimizer_trace="enabled=on",end_markers_in_json=on;
set optimizer_trace_max_mem_size=1000000;
```

执行SQL语句之后检查系统表就可以知道如何执行的SQL

```sql
select * from information_schema.optimizer_trace\G;
*************************** 1. row ***************************
                            QUERY: select * from Student where StudentId<1
                            TRACE: {
  "steps": [
    {
      "join_preparation": {
        "select#": 1,
        "steps": [
          {
            "expanded_query": "/* select#1 */ select `Student`.`StudentID` AS `StudentID`,`Student`.`ClassId` AS `ClassId`,`Student`.`StudentName` AS `StudentName` from `Student` where (`Student`.`StudentID` < 1)" //把*查询的都解析出来了
          }
        ] /* steps */
      } /* join_preparation */
    },
    {
      "join_optimization": {
        "select#": 1,
        "steps": [
          {
            "condition_processing": {
              "condition": "WHERE",
              "original_condition": "(`Student`.`StudentID` < 1)",
              "steps": [
                {
                  "transformation": "equality_propagation",
                  "resulting_condition": "(`Student`.`StudentID` < 1)"
                },
                {
                  "transformation": "constant_propagation",
                  "resulting_condition": "(`Student`.`StudentID` < 1)"
                },
                {
                  "transformation": "trivial_condition_removal",
                  "resulting_condition": "(`Student`.`StudentID` < 1)"
                }
              ] /* steps */
            } /* condition_processing */
          },
          {
            "substitute_generated_columns": {
            } /* substitute_generated_columns */
          },
          {
            "table_dependencies": [
              {
                "table": "`Student`",
                "row_may_be_null": false,
                "map_bit": 0,
                "depends_on_map_bits": [
                ] /* depends_on_map_bits */
              }
            ] /* table_dependencies */
          },
          {
            "ref_optimizer_key_uses": [
            ] /* ref_optimizer_key_uses */
          },
          {
            "rows_estimation": [
              {
                "table": "`Student`",
                "range_analysis": {
                  "table_scan": {
                    "rows": 4,
                    "cost": 3.9
                  } /* table_scan */,
                  "potential_range_indexes": [
                    {
                      "index": "PRIMARY",
                      "usable": true,
                      "key_parts": [
                        "StudentID"
                      ] /* key_parts */
                    },
                    {
                      "index": "index_id_Student",
                      "usable": true,
                      "key_parts": [
                        "StudentID"
                      ] /* key_parts */
                    },
                    {
                      "index": "index_Name_Student",
                      "usable": false,
                      "cause": "not_applicable"
                    }
                  ] /* potential_range_indexes */,
                  "setup_range_conditions": [
                  ] /* setup_range_conditions */,
                  "group_index_range": {
                    "chosen": false,
                    "cause": "not_group_by_or_distinct"
                  } /* group_index_range */,
                  "analyzing_range_alternatives": {
                    "range_scan_alternatives": [
                      {
                        "index": "PRIMARY",
                        "ranges": [
                          "StudentID < 1"
                        ] /* ranges */,
                        "index_dives_for_eq_ranges": true,
                        "rowid_ordered": true,
                        "using_mrr": false,
                        "index_only": false,
                        "rows": 1,
                        "cost": 1.21,
                        "chosen": true
                      },
                      {
                        "index": "index_id_Student",
                        "ranges": [
                          "StudentID < 1"
                        ] /* ranges */,
                        "index_dives_for_eq_ranges": true,
                        "rowid_ordered": false,
                        "using_mrr": false,
                        "index_only": false,
                        "rows": 1,
                        "cost": 2.21,
                        "chosen": false,
                        "cause": "cost"
                      }
                    ] /* range_scan_alternatives */,
                    "analyzing_roworder_intersect": {
                      "usable": false,
                      "cause": "too_few_roworder_scans"
                    } /* analyzing_roworder_intersect */
                  } /* analyzing_range_alternatives */,
                  "chosen_range_access_summary": {
                    "range_access_plan": {
                      "type": "range_scan",
                      "index": "PRIMARY",
                      "rows": 1,
                      "ranges": [
                        "StudentID < 1"
                      ] /* ranges */
                    } /* range_access_plan */,
                    "rows_for_plan": 1,
                    "cost_for_plan": 1.21,
                    "chosen": true
                  } /* chosen_range_access_summary */
                } /* range_analysis */
              }
            ] /* rows_estimation */
          },
          {
            "considered_execution_plans": [
              {
                "plan_prefix": [
                ] /* plan_prefix */,
                "table": "`Student`",
                "best_access_path": {
                  "considered_access_paths": [
                    {
                      "rows_to_scan": 1,
                      "access_type": "range",
                      "range_details": {
                        "used_index": "PRIMARY"
                      } /* range_details */,
                      "resulting_rows": 1,
                      "cost": 1.41,
                      "chosen": true
                    }
                  ] /* considered_access_paths */
                } /* best_access_path */,
                "condition_filtering_pct": 100,
                "rows_for_plan": 1,
                "cost_for_plan": 1.41,
                "chosen": true
              }
            ] /* considered_execution_plans */
          },
          {
            "attaching_conditions_to_tables": {
              "original_condition": "(`Student`.`StudentID` < 1)",
              "attached_conditions_computation": [
              ] /* attached_conditions_computation */,
              "attached_conditions_summary": [
                {
                  "table": "`Student`",
                  "attached": "(`Student`.`StudentID` < 1)"
                }
              ] /* attached_conditions_summary */
            } /* attaching_conditions_to_tables */
          },
          {
            "refine_plan": [
              {
                "table": "`Student`"
              }
            ] /* refine_plan */
          }
        ] /* steps */
      } /* join_optimization */
    },
    {
      "join_execution": {
        "select#": 1,
        "steps": [
        ] /* steps */
      } /* join_execution */
    }
  ] /* steps */
}
```



## 5.性能优化方法

### 5.1.单表优化

除非单表数据未来会一直不断上涨，否则不要一开始就考虑拆分，拆分会带来逻辑、部署、运维的各种复杂度，一般以整型值为主的表在千万级以下，字符串为主的表在五百万以下是没有太大问题的。而事实上很多时候MySQL单表的性能依然有不少优化空间，甚至能正常支撑千万级以上的数据量：

#### 引擎

目前广泛使用的是MyISAM和InnoDB两种引擎：

**MyISAM**

MyISAM引擎是MySQL 5.1及之前版本的默认引擎，它的特点是：

- 不支持行锁，读取时对需要读到的所有表加锁，写入时则对表加排它锁
- 不支持事务
- 不支持外键
- 不支持崩溃后的安全恢复
- 在表有读取查询的同时，支持往表中插入新纪录
- 支持BLOB和TEXT的前500个字符索引，支持全文索引
- 支持延迟更新索引，极大提升写入性能
- 对于不会进行修改的表，支持压缩表，极大减少磁盘空间占用

**InnoDB**

InnoDB在MySQL 5.5后成为默认索引，它的特点是：

- 支持行锁，采用MVCC来支持高并发
- 支持事务
- 支持外键
- 支持崩溃后的安全恢复
- 不支持全文索引

ps: 据说innodb已经在mysql 5.6.4支持全文索引了

总体来讲，MyISAM适合SELECT密集型的表，而InnoDB适合INSERT和UPDATE密集型的表

#### Schema与数据类型优化

1. 整数通常是标识列最好的选择，因为它们很快并且可以使用 `AUTO_INCREMENT`
1. 完全“随机”的字符串（如：`MD5()`、`SHA1()` 或者 `UUID()` 等产生的字符串）会任意分布在很大的空间内，会导致INSERT以及一些SELECT语句变的很慢
1. 如果希望查询执行得快速且并发性好，单个查询最好不要做太多的关联查询（互联网公司非常忌讳关联查询），利用程序来完成关联操作
1. 如果需要对一张比较大的表做表结构变更（ALTER TABLE操作增加一列），建议先拷贝一张与原表结构一样的表，再将数据复制进去，最后通过重命名将新表的表名称修改为原表的表名称。因为在变更表结构的时候很有可能会锁住整个表，并且可能会有长时间的不可用
1. 避免多表关联的时候可以适当考虑一些反范式的建表方案，增加一些冗余字段
1. 不用外键，由程序保证约束
1. 尽量不用UNIQUE，由程序保证约束

#### 字段

- 尽量使用TINYINT、SMALLINT、MEDIUM_INT作为整数类型而非INT，如果非负则加上UNSIGNED
- VARCHAR的长度只分配真正需要的空间
- 使用枚举或整数代替字符串类型
- 尽量使用TIMESTAMP而非DATETIME，
- 单表不要有太多字段，建议在20以内
- 避免使用NULL字段，很难查询优化且占用额外索引空间
- 用整型来存IP

#### 索引

- 索引并不是越多越好，要根据查询有针对性的创建，考虑在 `WHERE` 和 `ORDER BY` 命令上涉及的列建立索引，可根据EXPLAIN来查看是否用了索引还是全表扫描
- 值分布很稀少的字段不适合建索引，例如"性别"这种只有两三个值的字段--数据越不集中，并且回表数据多，速度就慢
- 字符字段只建前缀索引
- 字符字段最好不要做主键
- 使用多列索引时主意顺序和查询条件保持一致，同时删除不必要的单列索引


- 在范围查询的字段后面索引失效

    ```
    explain select *from Student where 索引1= and 字段>2 and 索引2=
    ```

	因此索引2将会失效，用不到该索引

- 如果对某一个列进行了计算操作，索引失效

    ```mysql
    explain select * from Student where ClassId  BETWEEN 20771 and 20111
    ```

	![image-20210311230924946](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230925.png)

- 如果字符串不加单引号，索引会失效。

- 使用覆盖索引（只访问索引的查询），避免使用select *

	在查询的时候将 `*` 号改成需要查询的字段或者索引，减少不必要的开销，使用索引查询，`using index condition` 会将需要的字段查询出来

    ```
  using index ：使用覆盖索引的时候就会出现
  using where：在查找使用索引的情况下，需要回表去查询所需的数据
  using index condition：查找使用了索引，但是需要回表查询数据
  using index ; using where：查找使用了索引，但是需要的数据都在索引列中能找到，所以不需要回表查询数据
    ```

- 如果有 `or`后面的字段没有索引，则整个索引失效

    ```
    explain select * from Teacher where  teacherId=2;
    ```
    
    原本主键索引
    
    ![image-20210311230942403](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230942.png)
    
    加上or之后，索引失效

    ```
    explain select * from Teacher where   ClassId=204  or teacherId=2;
    ```
    
    ![image-20210311230954888](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311230954.png)
    
- 以like '%XX'开头不走索引

    正常走索引

    ```mysql
    explain select * from Student where StudentName LIKE '货物9000号%';
    ```
    
    ![image-20210311231008741](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311231008.png)
    
    在like前加上%号

    ```mysql
    explain select * from Student where StudentName LIKE '%货物9000号%' ;
    ```
    
    ![image-20210311231022629](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311231022.png)
    
    不走索引解决办法：使用覆盖索引，将*号改成有索引的列，再通过索引查询

    ```mysql
    explain select StudentID from Student where StudentName LIKE '%货物9000号%' 
    ```

	![image-20210311231041677](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311231041.png)

- 如果再一张表中，一个字段数据基本全是1，只有为2。这时候给该字段建立索引，查询1的时候mysql认为走全表速度更快就不会走索引，如果查询2就会走索引。

- IS NUL、IS NOT NULL有时走索引

	如果一个字段中所有数据都不为空，那么查询该字段时会走索引，是少量的就会走索引，大多数不会走索引。

    ```mysql
  EXPLAIN  select * from Student  where StudentName is NULL;
  EXPLAIN  select * from Student  where StudentName is NOT NULL;
    ```

	![image-20210311231053721](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311231053.png)

- in走索引、not in 不走索引，但也不是绝对的

- 对于联合索引来说，要遵守最左前缀法则
    
举列来说索引含有字段id、name、school，可以直接用id字段，也可以id、name这样的顺序，但是name;school都无法使用这个索引。所以在创建联合索引的时候一定要注意索引字段顺序，常用的查询字段放在最前面。
    
如果不是按照索引的最左列开始查找，则无法使用索引
    
- 所有的非聚簇索引都需要先通过索引定位到对应的主键，然后在到聚簇索引查找数据，所以在定义主键索引的时候一定要谨慎

- 只有当索引的列顺序和ORDER BY子句的顺序完全一致，并且所有列的排序方向（倒序或者正序）都一样时，MySQL才能够使用索引来对结果做排序。有一种情况下ORDER BY子句可以不满足索引的最左前缀的要求，就是前导列为常量的时候。

- 哈希索引是基于哈希表实现的，只有精确匹配索引所有列的查询才有效，也不遵循索引的最左匹配原则

- 当服务器需要对多个索引做联合操作时（通常有多个OR条件），建议修改成UNION的方式，这样方便命中索引

- 对于如何选择索引的列顺序有一个经验法则：将选择性最高的列放到索引最前列

- 尽可能多的使用覆盖索引（如果一个索引包含或者说覆盖所有需要查询的字段的值，我们就称之为覆盖索引），通过 `EXPLAIN` 的 `Extra` 列可以看到 `Using index` 信息

- 当ID为主键时，创建索引(A)，相当于创建了(A)和(A, ID)两个索引

- 表中的索引越多对SELECT、UPDATE和DELETE操作速度变慢，同时占用的内存也会比较多

- InnoDB在二级索引上使用共享锁，但是访问主键索引需要排他锁

- 尽可能的使用WHERE IN和WHERE BETWEEN AND的方式来进行范围查询

- 编写查询语句时应该避免单行查找、尽可能的使用数据原生顺序从而避免额外的排序操作，并尽可能使用索引覆盖查询

- 不做列运算：SELECT id WHERE age + 1 = 10，任何对列的操作都将导致表扫描，它包括数据库教程函数、计算表达式等等，查询时要尽可能将操作移至等号右边

- 关联查询的时候要确保关联的字段上有索引

- 如果定了的索引列和分区列不匹配，会导致查询无法进行分区过滤

- SQL提示

    （1）USE index，在有多个索引的情况下，希望Mysql使用该索引，但不是一定会用。

    ```mysql
    explain select * from sales2 use index (ind_sales2_id) where id = 3
    ```

    （2）ignore index可以忽略使用该索引，使用其他索引

    （3）在数据量很多的情况下，查询数据占很大比重，即使使用了索引，数据库也不会用，这时候使用force index强制指定索引。



#### SQL优化

- 可通过开启慢查询日志来找出较慢的SQL

- sql语句尽可能简单：一条sql只能在一个cpu运算；大语句拆小语句，减少锁时间；一条大sql可以堵死整个库

- 不用SELECT *

- OR改写成IN：OR的效率是n级别，IN的效率是log(n)级别，in的个数建议控制在200以内

- 不用函数和触发器，在应用程序实现

- 少用JOIN

- 对于连续数值，使用BETWEEN不用IN：SELECT id FROM t WHERE num BETWEEN 1 AND 5

- 列表数据不要拿全表，要使用LIMIT来分页，每页数量也不要太大

- SQL语句中IN包含的值不应过多
  
  MySQL对于IN做了相应的优化，即将IN中的常量全部存储在一个数组里面，而且这个数组是排好序的。但是如果数值较多，产生的消耗也是比较大的。再例如：select id from t where num in(1,2,3) 对于连续的数值，能用between就不要用in了；再或者使用连接来替换。
  
- 当只需要一条数据的时候，使用limit 1
  
  这是为了使EXPLAIN中type列达到const类型五、如果排序字段没有用到索引
  
- 如果排序字段没有用到索引，就尽量少排序

- 尽量用union all代替union
  
  union和union all的差异主要是前者需要将结果集合并后再进行唯一性过滤操作，这就会涉及到排序，增加大量的CPU运算，加大资源消耗及延迟。当然，union all的前提条件是两个结果集没有重复数据。
  
- 不使用ORDER BY RAND()

    ```
    select id from `dynamic` order by rand() limit 1000;
    ```

	上面的SQL语句，可优化为：

	```
    select id from `dynamic` t1 join (select rand() * (select max(id) from `dynamic`) as nid) t2 on t1.id > t2.nidlimit 1000;
    ```

- 使用合理的分页方式以提高分页的效率

    ```
    select id,name from product limit 866613, 20
    ```

	使用上述SQL语句做分页的时候，可能有人会发现，随着表数据量的增加，直接使用limit分页查询会越来越慢。
优化的方法如下：可以取前一页的最大行数的id，然后根据这个最大的id来限制下一页的起点。比如此列中，上一页最大的id是866612。SQL可以采用如下的写法：

    ```
    select id,name from product where id> 866612 limit 20
    ```

- 避免在where子句中对字段进行表达式操作

- 避免隐式类型转换，使用同类型进行比较，比如用'123'和'123'比，123和123比

- 优化批量插入

  - 大批量插入数据时，需要将主键按顺序插入会快很多

  - 如果插入过程中有唯一索引，可以先关闭索引检查，防止每插入一条时对索引进行筛查

    ```mysql
    set unique_checks=1;//1为打开 0为关闭
    ```

  - 手动提交事务，关闭自动提交事务

    ```mysql
    set autocommit=1;//1为打开 0为关闭
    ```

- 优化insert语句

  （1）将多条insert语句改为一条

  （2）手动开启事务，全部插入之后，再提交

  （3）尽量按主键顺序插入

- 优化Order by语句

  （1）如果按照多字段排序，要么统一升序要么统一降序

  （2）order 不用后面的字段需要和索引的顺序保持一致

  （3）如果Extra列还出现Using filesort，表示进行了额外的一次排序，考虑使用联合索引

- 优化Group by语句

  （1）使用Group by如果Extra列出现Using filesort，表示Group by语句默认进行了排序，可以使用Order by null取消排序

  （2）使用Group by如果Extra列出现Using Temporary，可以给字段建立索引提高效率

- 优化嵌套查询

  （1）把多表连接查询替换为子查询

- 优化OR查询

  （1）如果需要用到索引，则每个列需要单独创建索引，不能用复合索引

  （2）使用Union替换Or

- 优化分页查询

  （1）根据主键进行排序分页操作，得到主键再回原表进行查询

  （2）主键自增时，可以直接根据ID查询，数据没删除的情况下

- 尽量使用数字型字段，若只含数值信息的字段尽量不要设计为字符型，这会降低查询和连接的性能，并会增加存储开销。这是因为引擎在处理查询和连接时会逐个比较字符串中每一个字符，而对于数字型而言只需要比较一次就够了。

- 尽可能的使用 varchar 代替 char ，因为首先变长字段存储空间小，可以节省存储空间， 其次对于查询来说，在一个相对较小的字段内搜索效率显然要高些。

- 对于低效的查询，通常从两个方面来分析：
  - 确认应用程序是否在检索大量超过需要的数据。这通常意味着访问了太多的行，但有时候可能是访问了太多的列
  - 确认MySQL服务器层是否在分析大量超过需要的数据行
  
- 一般MySQL能够使用以下三种方式应用WHERE条件，从好到坏依次为：
  - 在索引中使用WHERE条件俩过滤不匹配的记录
  - 使用索引覆盖扫描来返回记录
  - 从数据表中返回数据，然后过滤不满足条件的记录
  
- MySQL从设计上让连接和断开连接都很轻量级，在返回一个小的查询结果方面很高效。在一个通用服务器上，也能够运行每秒超过10万的查询，一个千兆网卡也能轻松满足每秒超过2000次的查询，MySQL内部每秒能够扫描内存中上百万行数据

- 在删除大量数据时，建议每次删除一小批量数据后，暂停一会儿再做下一次的删除

- 无论如何排序都是一个成本很高的操作，所以从性能角度考虑，应尽可能避免排序或者尽可能避免对大量数据进行排序

- COUNT()函数有两种不同的作用：它可以统计某个列值的数量，也可以统计行数。最简单的就是通过`COUNT(*)`来统计行数

- 在数据量很大并且历史数据需要定期删除的情况下，可以考虑使用分区表

- 触发器、存储过程、自定义函数等最好不要使用

- LIMIT的偏移量越大性能越慢



#### 系统调优参数

可以使用下面几个工具来做基准测试：

- sysbench：一个模块化，跨平台以及多线程的性能测试工具
- iibench-mysql：基于 Java 的 MySQL/Percona/MariaDB 索引进行插入性能测试工具
- tpcc-mysql：Percona开发的TPC-C测试工具

具体的调优参数内容较多，具体可参考官方文档，这里介绍一些比较重要的参数：

- back_log：back_log值指出在MySQL暂时停止回答新请求之前的短时间内多少个请求可以被存在堆栈中。也就是说，如果MySql的连接数据达到max_connections时，新来的请求将会被存在堆栈中，以等待某一连接释放资源，该堆栈的数量即back_log，如果等待连接的数量超过back_log，将不被授予连接资源。可以从默认的50升至500
- wait_timeout：数据库连接闲置时间，闲置连接会占用内存资源。可以从默认的8小时减到半小时
- max_user_connection: 最大连接数，默认为0无上限，最好设一个合理上限
- thread_concurrency：并发线程数，设为CPU核数的两倍
- skip_name_resolve：禁止对外部连接进行DNS解析，消除DNS解析时间，但需要所有远程主机用IP访问
- key_buffer_size：索引块的缓存大小，增加会提升索引处理速度，对MyISAM表性能影响最大。对于内存4G左右，可设为256M或384M，通过查询show status like 'key_read%'，保证key_reads / key_read_requests在0.1%以下最好
- innodb_buffer_pool_size：缓存数据块和索引块，对InnoDB表性能影响最大。通过查询show status like 'Innodb_buffer_pool_read%'，保证 (Innodb_buffer_pool_read_requests – Innodb_buffer_pool_reads) / Innodb_buffer_pool_read_requests越高越好
- innodb_additional_mem_pool_size：InnoDB存储引擎用来存放数据字典信息以及一些内部数据结构的内存空间大小，当数据库对象非常多的时候，适当调整该参数的大小以确保所有数据都能存放在内存中提高访问效率，当过小的时候，MySQL会记录Warning信息到数据库的错误日志中，这时就需要该调整这个参数大小
- innodb_log_buffer_size：InnoDB存储引擎的事务日志所使用的缓冲区，一般来说不建议超过32MB
- query_cache_size：缓存MySQL中的ResultSet，也就是一条SQL语句执行的结果集，所以仅仅只能针对select语句。当某个表的数据有任何任何变化，都会导致所有引用了该表的select语句在Query Cache中的缓存数据失效。所以，当我们的数据变化非常频繁的情况下，使用Query Cache可能会得不偿失。根据命中率(Qcache_hits/(Qcache_hits+Qcache_inserts)*100))进行调整，一般不建议太大，256MB可能已经差不多了，大型的配置型静态数据可适当调大.
- 可以通过命令show status like 'Qcache_%'查看目前系统Query catch使用大小
- read_buffer_size：MySql读入缓冲区大小。对表进行顺序扫描的请求将分配一个读入缓冲区，MySql会为它分配一段内存缓冲区。如果对表的顺序扫描请求非常频繁，可以通过增加该变量值以及内存缓冲区大小提高其性能
- sort_buffer_size：MySql执行排序使用的缓冲大小。如果想要增加ORDER BY的速度，首先看是否可以让MySQL使用索引而不是额外的排序阶段。如果不能，可以尝试增加sort_buffer_size变量的大小
- read_rnd_buffer_size：MySql的随机读缓冲区大小。当按任意顺序读取行时(例如，按照排序顺序)，将分配一个随机读缓存区。进行排序查询时，MySql会首先扫描一遍该缓冲，以避免磁盘搜索，提高查询速度，如果需要排序大量数据，可适当调高该值。但MySql会为每个客户连接发放该缓冲空间，所以应尽量适当设置该值，以避免内存开销过大。
- record_buffer：每个进行一个顺序扫描的线程为其扫描的每张表分配这个大小的一个缓冲区。如果你做很多顺序扫描，可能想要增加该值
- thread_cache_size：保存当前没有与连接关联但是准备为后面新的连接服务的线程，可以快速响应连接的线程请求而无需创建新的
- table_cache：类似于thread_cache_size，但用来缓存表文件，对InnoDB效果不大，主要用于MyISAM

#### 升级硬件

Scale up，这个不多说了，根据MySQL是CPU密集型还是I/O密集型，通过提升CPU和内存、使用SSD，都能显著提升MySQL性能



## 6.案例分析

### 6.1.为啥阿里巴巴不建议MySQL使用Text类型？

众所周知，MySQL广泛应用于互联网的OLTP（联机事务处理过程）业务系统中，在大厂开发规范中，经常会看到一条"不建议使用text大字段类型”。

下面就从text类型的存储结构，引发的问题解释下为什么不建议使用text类型，以及Text改造的建议方法。

#### 背景

写log表导致DML慢

- **问题描述**

  某歪有一个业务系统，使用RDS for MySQL 5.7的高可用版本，配置long_query_time=1s，添加慢查询告警，我第一反应就是某歪又乱点了。

  我通过监控看CPU， QPS，TPS等指标不是很高，最近刚好双十一全站都在做营销活动，用户量稍微有所增加。某歪反馈有些原本不慢的接口变的很慢，影响了正常的业务，需要做一下troubleshooting。

- **问题分析**

  我从慢查询告警，可以看到有一些insert和update语句比较慢，同时告警时段的监控，发现IOPS很高，达到了70MB/s左右，由于RDS的CloundDBA功能不可用，又没有audit log功能，troubleshooting比较困难，硬着头皮只能分析binlog了。

  配置了max_binlog_size =512MB，在IOPS高的时段里，看下binlog的生成情况。

  ![image-20210311234505214](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210311234505.png)

  需要分析为什么binlog写这么快，最有可能原因就是insert into request_log表上有text类型，request_log表结构如下（demo）

    ```
    CREATE TABLE request_log (`
     `id bigint(20) NOT NULL AUTO_INCREMENT,`
     `log text,`    
     `created_at datetime NOT NULL,`
     `status tinyint(4) NOT NULL,`
     `method varchar(10) DEFAULT NULL,`
     `url varchar(50) DEFAULT NULL,`
     `update_at datetime DEFAULT NULL,`
     `running_time tinyint(4) DEFAULT '0',`
     `user_id bigint(20) DEFAULT NULL,`
     `type varchar(50) DEFAULT NULL,`
     `PRIMARY KEY (id)`
    `) ENGINE=InnoDB AUTO_INCREMENT=4229611 DEFAULT CHARSET=utf8`
    ```

	分析binlog：

    ```
    $ mysqlbinlog --no-defaults -v -v --base64-output=DECODE-ROWS mysql-bin.000539|egrep "insert into request_log"
    ```
  
  满屏幕都是看不清的内容，翻了半天没翻完。
  
  基本上已经确定是写入request_log的log字段引起的，导致binlog_cache频繁的flush，以及binlog过度切换，导致IOPS过高，影响了其他正常的DML操作。

- **问题解决**

  跟开发同学沟通后，计划在下一个版本修复这个问题，不再将request信息写入表中，写入到本地日志文件，通过filebeat抽取到es进行查询，如果只是为了查看日志也可以接入grayLog等日志工具，没必要写入数据库。

#### MySQL中的Text

1、**Text类型**

text是一个能够存储大量的数据的大对象，有四种类型：TINYTEXT, TEXT, MEDIUMTEXT,LONGTEXT，不同类型存储的值范围不同，如下所示

| Data Type  | Storage Required             |
| :--------- | :--------------------------- |
| TINYTEXT   | L + 1 bytes, where L < 2**8  |
| TEXT       | L + 2 bytes, where L < 2**16 |
| MEDIUMTEXT | L + 3 bytes, where L < 2**24 |
| LONGTEXT   | L + 4 bytes, where L < 2**32 |

其中L表是text类型中存储的实际长度的字节数。可以计算出TEXT类型最大存储长度2**16-1 = 65535 Bytes。

2、**InnoDB数据页**

Innodb数据页由以下7个部分组成：

| 内容                        | 占用大小 | 说明                                                         |
| :-------------------------- | :------- | :----------------------------------------------------------- |
| File Header                 | 38Bytes  | 数据文件头                                                   |
| Page Header                 | 56 Bytes | 数据页头                                                     |
| Infimun 和 Supermum Records |          | 伪记录                                                       |
| User Records                |          | 用户数据                                                     |
| Free Space                  |          | 空闲空间：内部是链表结构，记录被delete后，会加入到free_lru链表 |
| Page  Dictionary            |          | 页数据字典：存储记录的相对位置记录，也称为Slot，内部是一个稀疏目录 |
| File Trailer                | 8Bytes   | 文件尾部：为了检测页是否已经完整个的写入磁盘                 |

说明：File Trailer只有一个FiL_Page_end_lsn部分，占用8字节，前4字节代表该页的checksum值，最后4字节和File Header中的FIL_PAGE_LSN，一个页是否发生了Corrupt，是通过File Trailer部分进行检测，而该部分的检测会有一定的开销，用户可以通过参数innodb_checksums开启或关闭这个页完整性的检测。

从MySQL 5.6开始默认的表存储引擎是InnoDB，它是面向ROW存储的，每个page(default page size = 16KB)，存储的行记录也是有规定的，最多允许存储16K/2 - 200 = 7992行。

3、**InnoDB的行格式**

Innodb支持四种行格式：

| 行格式     | Compact存储特性 | 增强的变长列存储 | 支持大前缀索引 | 支持压缩 | 支持表空间类型                  |
| :--------- | :-------------- | :--------------- | :------------- | :------- | :------------------------------ |
| REDUNDANT  | No              | No               | No             | No       | system, file-per-table, general |
| COMPACT    | Yes             | No               | No             | No       | system, file-per-table, general |
| DYNAMIC    | Yes             | Yes              | Yes            | No       | system, file-per-table, general |
| COMPRESSED | Yes             | Yes              | Yes            | Yes      | file-per-table, general         |

由于Dynamic是Compact变异而来，结构大同而已，现在默认都是Dynamic格式；COMPRESSED主要是对表和索引数据进行压缩，一般适用于使用率低的归档，备份类的需求，主要介绍下REDUNDANT和COMPACT行格式。

3.1、**Redundant行格式**

这种格式为了兼容旧版本MySQL。

**行记录格式：**

| Variable-length offset list | record_header        | col1_value | col2_value | …….  | text_value     |
| :-------------------------- | :------------------- | :--------- | :--------- | :--- | :------------- |
| 字段长度偏移列表            | 记录头信息，占48字节 | 列1数据    | 列2数据    | …….  | Text列指针数据 |

**具有以下特点：**

- 存储变长列的前768 Bytes在索引记录中，剩余的存储在overflow page中，对于固定长度且超过768 Bytes会被当做变长字段存储在off-page中。
- 索引页中的每条记录包含一个6 Bytes的头部，用于链接记录用于行锁。
- 聚簇索引的记录包含用户定义的所有列。另外还有一个6字节的事务ID（DB_TRX_ID）和一个7字节长度的回滚段指针(Roll pointer)列。
- 如果创建表没有显示指定主键，每个聚簇索引行还包括一个6字节的行ID（row ID）字段。
- 每个二级索引记录包含了所有定义的主键索引列。
- 一条记录包含一个指针来指向这条记录的每个列，如果一条记录的列的总长度小于128字节，这个指针占用1个字节，否则2个字节。这个指针数组称为记录目录（record directory）。指针指向的区域是这条记录的数据部分。
- 固定长度的字符字段比如CHAR(10)通过固定长度的格式存储，尾部填充空格。
- 固定长度字段长度大于或者等于768字节将被编码成变长的字段，存储在off-page中。
- 一个SQL的NULL值存储一个字节或者两个字节在记录目录（record dirictoty）。对于变长字段null值在数据区域占0个字节。对于固定长度的字段，依然存储固定长度在数据部分，为null值保留固定长度空间允许列从null值更新为非空值而不会引起索引的分裂。
- 对varchar类型，Redundant行记录格式同样不占用任何存储空间，而CHAR类型的NULL值需要占用空间。

其中变长类型是通过长度 + 数据的方式存储，不同类型长度是从1到4个字节（L+1 到 L + 4），对于TEXT类型的值需要L Bytes存储value，同时需要2个字节存储value的长度。同时Innodb最大行长度规定为65535 Bytes，对于Text类型，只保存9到12字节的指针，数据单独存在overflow page中。

3.2、**Compact行格式**

这种行格式比redundant格式减少了存储空间作为代价，但是会增加某些操作的CPU开销。如果系统workload是受缓存命中率和磁盘速度限制，compact行格式可能更快。如果你的工作负载受CPU速度限制，compact行格式可能更慢，Compact 行格式被所有file format所支持。

**行记录格式：**

| Variable-length field length list | NULL标志位 | record_header | col1_value | col2_value | …….  | text_value     |
| :-------------------------------- | :--------- | :------------ | :--------- | :--------- | :--- | :------------- |
| 变长字段长度列表                  |            | 记录头信息-   | 列1数据    | 列2数据    | …….  | Text列指针数据 |

Compact首部是一个非NULL变长字段长度的列表，并且是按列的顺序逆序放置的，若列的长度小于255字节，用1字节表示；若大于255个字节，用2字节表示。变长字段最大不可以超过2字节，这是因为MySQL数据库中varchar类型最大长度限制为65535，变长字段之后的第二个部分是NULL标志位，表示该行数据是否有NULL值。有则用1表示，该部分所占的字节应该为1字节。

所以在创建表的时候，尽量使用NOT NULL DEFAULT ''，如果表中列存储大量的NULL值，一方面占用空间，另一个方面影响索引列的稳定性。

**具有以下特点：**

- 索引的每条记录包含一个5个字节的头部，头部前面可以有一个可变长度的头部。这个头部用来将相关连的记录链接在一起，也用于行锁。
- 记录头部的变长部分包含了一个表示null 值的位向量(bit vector)。如果索引中可以为null的字段数量为N，这个位向量包含 N/8 向上取整的字节数。比例如果有9-16个字段可以为NULL值，这个位向量使用两个字节。为NULL的列不占用空间，只占用这个位向量中的位。头部的变长部分还包含了变长字段的长度。每个长度占用一个或者2个字节，这取决了字段的最大长度。如果所有列都可以为null 并且制定了固定长度，记录头部就没有变长部分。
- 对每个不为NULL的变长字段，记录头包含了一个字节或者两个字节的字段长度。只有当字段存储在外部的溢出区域或者字段最大长度超过255字节并且实际长度超过127个字节的时候会使用2个字节的记录头部。对应外部存储的字段，两个字节的长度指明内部存储部分的长度加上指向外部存储部分的20个字节的指针。内部部分是768字节，因此这个长度值为 768+20， 20个字节的指针存储了这个字段的真实长度。
- NULL不占该部分任何空间，即NULL除了占用NULL标志位，实际存储不占任何空间。
- 记录头部跟着非空字段的数据部分。
- 聚簇索引的记录包含了所以用户定于的字段。另外还有一个6字节的事务ID列和一个7字节的回滚段指针。
- 如果没有定于主键索引，则聚簇索引还包括一个6字节的Row ID列。
- 每个辅助索引记录包含为群集索引键定义的不在辅助索引中的所有主键列。如果任何一个主键列是可变长度的，那么每个辅助索引的记录头都有一个可变长度的部分来记录它们的长度，即使辅助索引是在固定长度的列上定义的。
- 固定长度的字符字段比如CHAR(10)通过固定长度的格式存储，尾部填充空格。
- 对于变长的字符集，比如uft8mb3和utf8mb4， InnoDB试图用N字节来存储 CHAR(N)。如果CHAR(N)列的值的长度超过N字节，列后面的空格减少到最小值。CHAR(N)列值的最大长度是最大字符编码数 x N。比如utf8mb4字符集的最长编码为4，则列的最长字节数是 4*N。

#### Text类型引发的问题

1、**插入text字段导致报错**

1.1、**创建测试表**

```
[root@barret] [test]>create table user(id bigint not null primary key auto_increment, 
  -> name varchar(20) not null default '' comment '姓名', 
  -> age tinyint not null default 0 comment 'age', 
  -> gender char(1) not null default 'M' comment '性别',
  -> info text not null comment '用户信息',
  -> create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  -> update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
  -> );
Query OK, 0 rows affected (0.04 sec)
```

1.2、**插入测试数据**

```
root@barret] [test]>insert into user(name,age,gender,info) values('moon', 34, 'M', repeat('a',1024*1024*3));
ERROR 1406 (22001): Data too long for column 'info' at row 1
[root@barret] [test]>insert into user(name,age,gender,info) values('sky', 35, 'M', repeat('b',1024*1024*5));
ERROR 1301 (HY000): Result of repeat() was larger than max_allowed_packet (4194304) - truncated
```

1.3、**错误分析**

```
[root@barret] [test]>select @@max_allowed_packet;
+----------------------+
| @@max_allowed_packet |
+----------------------+
|       4194304 |
+----------------------+
1 row in set (0.00 sec)
```

max_allowed_packet控制communication buffer最大尺寸，当发送的数据包大小超过该值就会报错，我们都知道，MySQL包括Server层和存储引擎，它们之间遵循2PC协议，Server层主要处理用户的请求：连接请求—>SQL语法分析—>语义检查—>生成执行计划—>执行计划—>fetch data；存储引擎层主要存储数据，提供数据读写接口。

```
max_allowed_packet=4M，当第一条insert repeat('a',1024*1024*3)，数据包Server执行SQL发送数据包到InnoDB层的时候，检查数据包大小没有超过限制4M，在InnoDB写数据时，发现超过了Text的限制导致报错。第二条insert的数据包大小超过限制4M，Server检测不通过报错。
```

**引用AWS RDS参数组中该参数的描述**

max_allowed_packet:  This value by default is small, to catch large (possibly incorrect) packets. Must be increased if using large TEXT columns or long strings. As big as largest BLOB.

增加该参数的大小可以缓解报错，但是不能彻底的解决问题。

2、**RDS实例被锁定**

2.1、**背景描述**

公司每个月都会做一些营销活动，有个服务apush活动推送，单独部署在高可用版的RDS for MySQL 5.7，配置是4C8G 150G磁盘，数据库里也就4张表，晚上22：00下班走的时候，rds实例数据使用了50G空间，第二天早晨9：30在地铁上收到钉钉告警短信，提示push服务rds实例由于disk is full被locked with —read-only，开发也反馈，应用日志报了一堆MySQL error。

2.2、**问题分析**

通过DMS登录到数据库，看一下那个表最大，发现有张表push_log占用了100G+，看了下表结构，里面有两个text字段。

```
request text default '' comment '请求信息',
response text default '' comment '响应信息'
mysql>show  table status like 'push_log'；
```

发现Avg_row_length基本都在150KB左右，Rows = 78w，表的大小约为780000*150KB/1024/1024 = 111.5G。

3、**通过主键update也很慢**

```
insert into user(name,age,gender,info) values('thooo', 35, 'M', repeat('c',65535);
insert into user(name,age,gender,info) values('thooo11', 35, 'M', repeat('d',65535);
insert into user(name,age,gender,info) select name,age,gender,info from user;
Query OK, 6144 rows affected (5.62 sec)
Records: 6144  Duplicates: 0  Warnings: 0                                        
[root@barret] [test]>select count(*) from user;
+----------+
| count(*) |
+----------+
|    24576 |
+----------+
1 row in set (0.05 sec)
```

做update操作并跟踪。

```
mysql> set profiling = 1;
Query OK, 0 rows affected, 1 warning (0.00 sec)

mysql> update user set info = repeat('f',65535) where id = 11;
Query OK, 1 row affected (0.28 sec)
Rows matched: 1  Changed: 1  Warnings: 0

mysql> show profiles;
+----------+------------+--------------------------------------------------------+
| Query_ID | Duration   | Query                                                  |
+----------+------------+--------------------------------------------------------+
|        1 | 0.27874125 | update user set info = repeat('f',65535) where id = 11 |
+----------+------------+--------------------------------------------------------+
1 row in set, 1 warning (0.00 sec)

mysql> show profile cpu,block io for query 1;  
+----------------------+----------+----------+------------+--------------+---------------+
| Status               | Duration | CPU_user | CPU_system | Block_ops_in | Block_ops_out |
+----------------------+----------+----------+------------+--------------+---------------+
| starting             | 0.000124 | 0.000088 |   0.000035 |            0 |             0 |
| checking permissions | 0.000021 | 0.000014 |   0.000006 |            0 |             0 |
| Opening tables       | 0.000038 | 0.000026 |   0.000011 |            0 |             0 |
| init                 | 0.000067 | 0.000049 |   0.000020 |            0 |             0 |
| System lock          | 0.000076 | 0.000054 |   0.000021 |            0 |             0 |
| updating             | 0.244906 | 0.000000 |   0.015382 |            0 |         16392 |
| end                  | 0.000036 | 0.000000 |   0.000034 |            0 |             0 |
| query end            | 0.033040 | 0.000000 |   0.000393 |            0 |           136 |
| closing tables       | 0.000046 | 0.000000 |   0.000043 |            0 |             0 |
| freeing items        | 0.000298 | 0.000000 |   0.000053 |            0 |             0 |
| cleaning up          | 0.000092 | 0.000000 |   0.000092 |            0 |             0 |
+----------------------+----------+----------+------------+--------------+---------------+
11 rows in set, 1 warning (0.00 sec)
```

可以看到主要耗时在updating这一步，IO输出次数16392次，在并发的表上通过id做update，也会变得很慢。

4、**group_concat也会导致查询报错**

在业务开发当中，经常有类似这样的需求，需要根据每个省份可以定点医保单位名称，通常实现如下：

```
select group_concat(dru_name) from t_drugstore group by province;
```

其中内置group_concat返回一个聚合的string，最大长度由参数group_concat_max_len（Maximum allowed result length in bytes for the GROUP_CONCAT()）决定，默认是1024，一般都太短了，开发要求改长一点，例如1024000。

当group_concat返回的结果集的大小超过max_allowed_packet限制的时候，程序会报错，这一点要额外注意。

5、**MySQL内置的log表**

MySQL中的日志表mysql.general_log和mysql.slow_log，如果开启审计audit功能，同时log_output=TABLE，就会有mysql.audit_log表，结构跟mysql.general_log大同小异。

分别看一下他们的表结构

```
CREATE TABLE `general_log` (
  `event_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `user_host` mediumtext NOT NULL,
  `thread_id` bigint(21) unsigned NOT NULL,
  `server_id` int(10) unsigned NOT NULL,
  `command_type` varchar(64) NOT NULL,
  `argument` mediumblob NOT NULL
) ENGINE=CSV DEFAULT CHARSET=utf8 COMMENT='General log'
CREATE TABLE `slow_log` (
  `start_time` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `user_host` mediumtext NOT NULL,
  `query_time` time(6) NOT NULL,
  `lock_time` time(6) NOT NULL,
  `rows_sent` int(11) NOT NULL,
  `rows_examined` int(11) NOT NULL,
  `db` varchar(512) NOT NULL,
  `last_insert_id` int(11) NOT NULL,
  `insert_id` int(11) NOT NULL,
  `server_id` int(10) unsigned NOT NULL,
  `sql_text` mediumblob NOT NULL,
  `thread_id` bigint(21) unsigned NOT NULL
) ENGINE=CSV DEFAULT CHARSET=utf8 COMMENT='Slow log'
```

mysql.general_log记录的是经过MySQL Server处理的所有的SQL，包括后端和用户的，insert比较频繁，同时`argument` mediumblob NOT NULL，对MySQL Server性能有影响的，一般我们在dev环境为了跟踪排查问题，可以开启general_log，Production环境禁止开启general_log，可以开启audit_log，它是在general_log的基础上做了一些filter，比如我只需要业务账号发起的所有的SQL，这个很有用的，很多时候需要分析某一段时间内哪个SQL的QPS，TPS比较高。

mysql.slow_log记录的是执行超过long_query_time的所有SQL，如果遵循MySQL开发规范，slow query不会太多，但是开启了log_queries_not_using_indexes=ON就会有好多full table scan的SQL被记录，这时slow_log表会很大，对于RDS来说，一般只保留一天的数据，在频繁insert into slow_log的时候，做truncate table slow_log去清理slow_log会导致MDL，影响MySQL稳定性。

建议将log_output=FILE，开启slow_log， audit_log，这样就会将slow_log，audit_log写入文件，通过Go API处理这些文件将数据写入分布式列式数据库clickhouse中做统计分析。

#### Text改造建议

1、**使用es存储**

在MySQL中，一般log表会存储text类型保存request或response类的数据，用于接口调用失败时去手动排查问题，使用频繁的很低。可以考虑写入本地log file，通过filebeat抽取到es中，按天索引，根据数据保留策略进行清理。

2、**使用对象存储**

有些业务场景表用到TEXT，BLOB类型，存储的一些图片信息，比如商品的图片，更新频率比较低，可以考虑使用对象存储，例如阿里云的OSS，AWS的S3都可以，能够方便且高效的实现这类需求。

#### 总结

由于MySQL是单进程多线程模型，一个SQL语句无法利用多个cpu core去执行，这也就决定了MySQL比较适合OLTP（特点：大量用户访问、逻辑读，索引扫描，返回少量数据，SQL简单）业务系统，同时要针对MySQL去制定一些建模规范和开发规范，尽量避免使用Text类型，它不但消耗大量的网络和IO带宽，同时在该表上的DML操作都会变得很慢。

另外建议将复杂的统计分析类的SQL，建议迁移到实时数仓OLAP中，例如目前使用比较多的clickhouse，里云的ADB，AWS的Redshift都可以，做到OLTP和OLAP类业务SQL分离，保证业务系统的稳定性。



### 6.2.MySQL分页查询优化

当需要从数据库查询的表有上万条记录的时候，一次性查询所有结果会变得很慢，特别是随着数据量的增加特别明显，这时需要使用分页查询。对于数据库分页查询，也有很多种方法和优化的点。下面简单说一下我知道的一些方法。

#### 准备工作

为了对下面列举的一些优化进行测试，下面针对已有的一张表进行说明。

- 表名：order_history
- 描述：某个业务的订单历史表
- 主要字段：unsigned int id，tinyint(4) int type
- 字段情况：该表一共37个字段，不包含text等大型数据，最大为varchar(500)，id字段为索引，且为递增。
- 数据量：5709294
- MySQL版本：5.7.16
  线下找一张百万级的测试表可不容易，如果需要自己测试的话，可以写shell脚本什么的插入数据进行测试。
  以下的 sql 所有语句执行的环境没有发生改变，下面是基本测试结果：

```sql
select count(*) from orders_history;
```

返回结果：5709294

三次查询时间分别为：

- 8903 ms
- 8323 ms
- 8401 ms

#### 一般分页查询

一般的分页查询使用简单的 limit 子句就可以实现。limit 子句声明如下：

```sql
SELECT * FROM table LIMIT [offset,] rows | rows OFFSET offset
```

LIMIT 子句可以被用于指定 SELECT 语句返回的记录数。需注意以下几点：

- 第一个参数指定第一个返回记录行的偏移量，注意从`0`开始
- 第二个参数指定返回记录行的最大数目
- 如果只给定一个参数：它表示返回最大的记录行数目
- 第二个参数为 -1 表示检索从某一个偏移量到记录集的结束所有的记录行
- 初始记录行的偏移量是 0(而不是 1)

下面是一个应用实例：

```sql
select * from orders_history where type=8 limit 1000,10;
```

该条语句将会从表 orders_history 中查询`offset: 1000`开始之后的10条数据，也就是第1001条到第1010条数据（`1001 <= id <= 1010`）。

数据表中的记录默认使用主键（一般为id）排序，上面的结果相当于：

```sql
select * from orders_history where type=8 order by id limit 10000,10;
```

三次查询时间分别为：

- 3040 ms
- 3063 ms
- 3018 ms

针对这种查询方式，下面测试查询记录量对时间的影响：

```sql
select * from orders_history where type=8 limit 10000,1;
select * from orders_history where type=8 limit 10000,10;
select * from orders_history where type=8 limit 10000,100;
select * from orders_history where type=8 limit 10000,1000;
select * from orders_history where type=8 limit 10000,10000;
```

三次查询时间如下：

- 查询1条记录：3072ms 3092ms 3002ms
- 查询10条记录：3081ms 3077ms 3032ms
- 查询100条记录：3118ms 3200ms 3128ms
- 查询1000条记录：3412ms 3468ms 3394ms
- 查询10000条记录：3749ms 3802ms 3696ms

另外我还做了十来次查询，从查询时间来看，基本可以确定，在查询记录量低于100时，查询时间基本没有差距，随着查询记录量越来越大，所花费的时间也会越来越多。

针对查询偏移量的测试：

```sql
select * from orders_history where type=8 limit 100,100;
select * from orders_history where type=8 limit 1000,100;
select * from orders_history where type=8 limit 10000,100;
select * from orders_history where type=8 limit 100000,100;
select * from orders_history where type=8 limit 1000000,100;
```

三次查询时间如下：

- 查询100偏移：25ms 24ms 24ms
- 查询1000偏移：78ms 76ms 77ms
- 查询10000偏移：3092ms 3212ms 3128ms
- 查询100000偏移：3878ms 3812ms 3798ms
- 查询1000000偏移：14608ms 14062ms 14700ms

随着查询偏移的增大，尤其查询偏移大于10万以后，查询时间急剧增加。

**这种分页查询方式会从数据库第一条记录开始扫描，所以越往后，查询速度越慢，而且查询的数据越多，也会拖慢总查询速度。**

#### 使用子查询优化

这种方式先定位偏移位置的 id，然后往后查询，这种方式适用于 id 递增的情况。

```sql
select * from orders_history where type=8 limit 100000,1;

select id from orders_history where type=8 limit 100000,1;

select * from orders_history where type=8 and 
id>=(select id from orders_history where type=8 limit 100000,1) 
limit 100;

select * from orders_history where type=8 limit 100000,100;
```

4条语句的查询时间如下：

- 第1条语句：3674ms
- 第2条语句：1315ms
- 第3条语句：1327ms
- 第4条语句：3710ms

针对上面的查询需要注意：

- 比较第1条语句和第2条语句：使用 select id 代替 select * 速度增加了3倍
- 比较第2条语句和第3条语句：速度相差几十毫秒
- 比较第3条语句和第4条语句：得益于 select id 速度增加，第3条语句查询速度增加了3倍

这种方式相较于原始一般的查询方法，将会增快数倍。

#### 使用 id 限定优化

这种方式假设数据表的id是**连续递增**的，则我们根据查询的页数和查询的记录数可以算出查询的id的范围，可以使用 id between and 来查询：

```sql
select * from orders_history where type=2 
and id between 1000000 and 1000100 limit 100;
```

查询时间：15ms 12ms 9ms

这种查询方式能够极大地优化查询速度，基本能够在几十毫秒之内完成。限制是只能使用于明确知道id的情况，不过一般建立表的时候，都会添加基本的id字段，这为分页查询带来很多便利。

还可以有另外一种写法：

```sql
select * from orders_history where id >= 1000001 limit 100;
```

当然还可以使用 in 的方式来进行查询，这种方式经常用在多表关联的时候进行查询，使用其他表查询的id集合，来进行查询：

```sql
select * from orders_history where id in
(select order_id from trade_2 where goods = 'pen')
limit 100;
```

这种 in 查询的方式要注意：某些 mysql 版本不支持在 in 子句中使用 limit。

#### 使用临时表优化

这种方式已经不属于查询优化，这儿附带提一下。

对于使用 id 限定优化中的问题，需要 id 是连续递增的，但是在一些场景下，比如使用历史表的时候，或者出现过数据缺失问题时，可以考虑使用临时存储的表来记录分页的id，使用分页的id来进行 in 查询。这样能够极大的提高传统的分页查询速度，尤其是数据量上千万的时候。

#### 关于数据表的id说明

一般情况下，在数据库中建立表的时候，强制为每一张表添加 id 递增字段，这样方便查询。

如果像是订单库等数据量非常庞大，一般会进行分库分表。这个时候不建议使用数据库的 id 作为唯一标识，而应该使用分布式的高并发唯一 id 生成器来生成，并在数据表中使用另外的字段来存储这个唯一标识。

使用先使用范围查询定位 id （或者索引），然后再使用索引进行定位数据，能够提高好几倍查询速度。即先 select id，然后再 select *；

