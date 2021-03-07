[toc]

# MySQL 常见问题



## Mysql中的排序规则utf8_unicode_ci、utf8_general_ci的区别
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307135301.png)
utf8_unicode_ci和utf8_general_ci对中、英文来说没有实质的差别。
utf8_general_ci 校对速度快，但准确度稍差。
utf8_unicode_ci 准确度高，但校对速度稍慢。

如果你的应用有德语、法语或者俄语，请一定使用utf8_unicode_ci。一般用utf8_general_ci就够了。

附：
ci是 case insensitive, 即 "大小写不敏感", a 和 A 会在字符判断中会被当做一样的；
bin 是二进制, a 和 A 会别区别对待。
例如你运行：
SELECT * FROM table WHERE txt = 'a'
那么在utf8_bin中你就找不到 txt = 'A' 的那一行， 而 utf8_general_ci 则可以。
utf8_general_ci 不区分大小写，这个你在注册用户名和邮箱的时候就要使用。
utf8_general_cs 区分大小写，如果用户名和邮箱用这个 就会照成不良后果
utf8_bin:字符串每个字符串用二进制数据编译存储。 区分大小写，而且可以存二进制的内容



## 为什么建议使用自增主键
我们都知道表的主键一般都要使用自增 id，不建议使用业务 id ，是因为使用自增 id 可以避免页分裂。这个其实可以相当于一个结论，你都可以直接记住这个结论就可以了。
我这里也稍微解释一下页分裂，mysql （注意本文讲的 mysql 默认为InnoDB 引擎）底层数据结构是 B+ 树，所谓的索引其实就是一颗 B+ 树，一个表有多少个索引就会有多少颗 B+ 树，mysql 中的数据都是按顺序保存在 B+ 树上的（所以说索引本身是有序的）。
然后 mysql 在底层又是以数据页为单位来存储数据的，一个数据页大小默认为 16k，当然你也可以自定义大小，也就是说如果一个数据页存满了，mysql 就会去申请一个新的数据页来存储数据。
如果主键为自增 id 的话，mysql 在写满一个数据页的时候，直接申请另一个新数据页接着写就可以了。
如果主键是非自增 id，为了确保索引有序，mysql 就需要将每次插入的数据都放到合适的位置上。
当往一个快满或已满的数据页中插入数据时，新插入的数据会将数据页写满，mysql 就需要申请新的数据页，并且把上个数据页中的部分数据挪到新的数据页上。
这就造成了页分裂，这个大量移动数据的过程是会严重影响插入效率的。
其实对主键 id 还有一个小小的要求，在满足业务需求的情况下，尽量使用占空间更小的主键 id，因为普通索引的叶子节点上保存的是主键 id 的值，如果主键 id 占空间较大的话，那将会成倍增加 mysql 空间占用大小。



## replace 与insert on duplicate效率分析

我们在向数据库里批量插入数据的时候，会遇到要将原有主键或者unique索引所在记录更新的情况，而如果没有主键或者unique索引冲突的时候，直接执行插入操作。
这种情况下，有三种方式执行：

**直接**

直接每条select, 判断，　然后insert，毫无疑问，这是最笨的方法了，不断的查询判断，有主键或索引冲突，执行update,否则执行insert. 数据量稍微大一点这种方式就不行了。

**replace**

这是mysql自身的一个语法，使用 replace 的时候。其语法为：
```
replace into tablename (f1, f2, f3) values(vf1, vf2, vf3),(vvf1, vvf2, vvf3)
```
这中语法会自动查询主键或索引冲突，如有冲突，他会先删除原有的数据记录，然后执行插入新的数据。

**insert on duplicate key**

这也是一种方式，mysql的insert操作中也给了一种方式，语法如下：
```
INSERT INTO table (a,b,c) VALUES (1,2,3)
  ON DUPLICATE KEY UPDATE c=c+1;
```
在insert时判断是否已有主键或索引重复，如果有，一句update后面的表达式执行更新，否则，执行插入。

**分析**

在最终实践结果中,得到接过如下：
在数据库数据量很少的时候，　这两种方式都很快，无论是直接的插入还是有冲突时的更新，都不错，但在数据库表的内容数量比较大(如百万级)的时候，两种方式就不太一样了，
首先是直接的插入操作，两种的插入效率都略低，　比如直接向表里插入1000条数据(百万级的表(innodb引擎))，二者都差不多需要5，6甚至十几秒。究其原因，我的主机性能是一方面，但在向大数据表批量插入数据的时候，每次的插入都要维护索引的，　索引固然可以提高查询的效率，但在更新表尤其是大表的时候，索引就成了一个不得不考虑的问题了。
其次是更新表，这里的更新的时候是带主键值的(因为我是从另一个表获取数据再插入，要求主键不能变)　同样直接更新1000条数据，　replace的操作要比insert on duplicate的操作低太多太多，　当insert瞬间完成(感觉)的时候，replace要7,8S,　replace慢的原因我是知道的,在更新数据的时候，要先删除旧的，然后插入新的，在这个过程中，还要重新维护索引，所以速度慢,但为何insert　on duplicate的更新却那么快呢。　在向老大请教后，终于知道，insert on duplicate 的更新操作虽然也会更新数据，但其对主键的索引却不会有改变，也就是说，insert　on duplicate　更新对主键索引没有影响.因此对索引的维护成本就低了一些(如果更新的字段不包括主键，那就要另说了)。



## 选择合适的 MySQL 日期时间类型来存储你的时间

构建数据库写程序避免不了使用日期和时间，对于数据库来说，有多种日期时间字段可供选择，如 timestamp 和 datetime 以及使用 int 来存储 unix timestamp。
经常会有人用字符串存储日期型的数据（不正确的做法）
- 无法用日期函数进行计算和比较
- 用字符串存储日期要占用更多的空间

不仅新手，包括一些有经验的程序员还是比较迷茫，究竟我该用哪种类型来存储日期时间呢？

那我们就一步一步来分析他们的特点，这样我们根据自己的需求选择合适的字段类型来存储 (优点和缺点是比较出来的, 跟父母从小喜欢拿邻居小孩子跟自己比一样的)

**datetime 和 timestamp**

- datetime 更像日历上面的时间和你手表的时间的结合，就是指具体某个时间。
- timestamp 更适合来记录时间，比如我在东八区时间现在是 2016-08-02 10:35:52， 你在日本（东九区此时时间为 2016-08-02 11:35:52），我和你在聊天，数据库记录了时间，取出来之后，对于我来说时间是 2016-08-02 10:35:52，对于日本的你来说就是 2016-08-02 11:35:52。所以就不用考虑时区的计算了。
- 时间范围是 timestamp 硬伤（1970-2038），当然 datetime （1000-9999）也记录不了刘备什么时候出生（161 年）。

**timestamp 和 UNIX timestamp**

- 显示直观，出问题了便于排错，比好多很长的 int 数字好看多了
- int 是从 1970 年开始累加的，但是 int 支持的范围是 1901-12-13 到 2038-01-19 03:14:07，如果需要更大的范围需要设置为 bigInt。但是这个时间不包含毫秒，如果需要毫秒，还需要定义为浮点数。datetime 和 timestamp 原生自带 6 位的微秒。
- timestamp 是自带时区转换的，同上面的第 2 项。
- 用户前端输入的时间一般都是日期类型，如果存储 int 还需要存前取后处理

**总结**

- timestamp 记录经常变化的更新 / 创建 / 发布 / 日志时间 / 购买时间 / 登录时间 / 注册时间等，并且是近来的时间，够用，时区自动处理，比如说做海外购或者业务可能拓展到海外
- datetime 记录固定时间如服务器执行计划任务时间 / 健身锻炼计划时间等，在任何时区都是需要一个固定的时间要做某个事情。超出 timestamp 的时间，如果需要时区必须记得时区处理
- UNIX timestamps 使用起来并不是很方便，至于说比较取范围什么的，timestamp 和 datetime 都能干。
- 如果你不考虑时区，或者有自己一套的时区方案，随意了，喜欢哪个上哪个了
- laravel 是国际化设计的框架，为了程序员方便、符合数据库设计标准，所以 created_at updated_at 使用了 timestamp 是无可厚非的。
- 有没有一个时间类型即解决了范围、时区的问题？这是不可能的，不是还有 tinyInt BigInt 吗？取自己所需，并且 MySQL 是允许数据库字段变更的。
- 生日可以使用多个字段来存储，比如 year/month/day，这样就可以很方便的找到某天过生日的用户 (User::where(['month' => 8, 'day' => 12])->get())

构建项目的时候需要认真思考一下，自己的业务场景究竟用哪种更适合。选哪个？需求来定。



## 如果频繁的修改一个表的数据，那么这么表会被锁死。造成假死现象。

MySQL如果频繁的修改一个表的数据，那么这么表会被锁死。造成假死现象。比如用Navicat等连接工具操作，Navicat会直接未响应，只能强制关闭软件，但是重启后依然无效。解决办法：
首先执行：
```
show full processlist;  //列出当前的操作process，一般会看到很多waiting的process，说明已经有卡住的proces了，我们要杀死这些process！！
```
再执行：
```
kill processid;  //processid表示process的id，比如kill 3301，就会将id为3301的process杀死。
```
使用 kill 将所有的 id 杀死。然后重启MySQL，一般就会解决了。如果还不行，那应该是不可能的吧。。。
重启MySQL：
```
net stop mysql  //停止MySQL
net start mysql  //启动MySQL
```



## mysql 索引过长1071-max key length is 767 byte

问题：create table: Specified key was too long; max key length is 767 bytes
原因：数据库表采用utf8编码，其中varchar(255)的column进行了唯一键索引，而mysql默认情况下单个列的索引不能超过767位(不同版本可能存在差异)，于是utf8字符编码下，255*3 byte 超过限制
解决：
1. 使用innodb引擎；
2. 启用innodb_large_prefix选项，将约束项扩展至3072byte；
3. 重新创建数据库；
my.cnf配置：
```
default-storage-engine=INNODB
innodb_large_prefix=on
```
一般情况下不建议使用这么长的索引，对性能有一定影响；



## Mysql分页order by数据错乱重复

作久项目代码优化，公司用的是Mybatis，发现分页和排序时直接传递参数占位符用的都是 $，由于$有SQL注入风险，要改为#，但是封装page类又麻烦，所以直接使用了 pageHelper 插件了，方便快捷，但是测试时发现数据有问题：
```
//第二页
SELECT id, createtime, idnumber, mac FROM `tblmacwhitelist`  
ORDER BY idnumber DESC  
LIMIT    5 , 5;
 
//第三页
SELECT id, createtime, idnumber, mac FROM `tblmacwhitelist`  
ORDER BY idnumber DESC  
LIMIT    10 , 5
 
//第四页
SELECT id, createtime, idnumber, mac FROM `tblmacwhitelist`  
ORDER BY idnumber DESC  
LIMIT    15 , 5
```
分页数量正常，但这3条SQL的结果集是一样的，第二第三第四页的数据，一模一样，我一脸懵逼，后来查了mysql官方文档返现：
```
If multiple rows have identical values in the ORDER BY columns, the server is free to return those rows in any order, and may do so differently depending on the overall execution plan. In other words, the sort order of those rows is nondeterministic with respect to the nonordered columns.

One factor that affects the execution plan is LIMIT, so an ORDER BY query with and without LIMIT may return rows in different orders.
```
大概意思是 ：一旦 order by 的 colunm 有多个相同的值的话，结果集是非常不稳定

那怎么解决呢，其实很简单，就是order by 加上唯一不重复的列即可，即在后面加上一个唯一索引就可以了，ORDER BY idnumber DESC , id DESC
```
//第二页
SELECT id, createtime, idnumber, mac FROM `tblmacwhitelist`  
ORDER BY idnumber DESC ,
id DESC
LIMIT    5 , 5;
 
//第三页
SELECT id, createtime, idnumber, mac FROM `tblmacwhitelist`  
ORDER BY idnumber DESC ,
id DESC
LIMIT    10 , 5
 
//第四页
SELECT id, createtime, idnumber, mac FROM `tblmacwhitelist`  
ORDER BY idnumber DESC  ,
id DESC
LIMIT    15 , 5
```



## 同事给我埋了个坑：Insert into select语句把生产服务器炸了

**1. 前言**

Insert into select请慎用。这天xxx接到一个需求，需要将表A的数据迁移到表B中去做一个备份。本想通过程序先查询查出来然后批量插入。但xxx觉得这样有点慢，需要耗费大量的网络I/O，决定采取别的方法进行实现。通过在Baidu的海洋里遨游，他发现了可以使用insert into select实现，这样就可以避免使用网络I/O，直接使用SQL依靠数据库I/O完成，这样简直不要太棒了。然后他就被开除了。

**2. 事故发生的经过**

由于数据数据库中order_today数据量过大，当时好像有700W了并且每天在以30W的速度增加。所以上司命令xxx将order_today内的部分数据迁移到order_record中，并将order_today中的数据删除。这样来降低order_today表中的数据量。

由于考虑到会占用数据库I/O，为了不影响业务，计划是9:00以后开始迁移，但是xxx在8:00的时候，尝试迁移了少部分数据(1000条)，觉得没啥问题，就开始考虑大批量迁移。

在迁移的过程中，应急群是先反应有小部分用户出现支付失败，随后反应大批用户出现支付失败的情况，以及初始化订单失败的情况，同时腾讯也开始报警。

然后xxx就慌了，立即停止了迁移。

本以为停止迁移就就可以恢复了，但是并没有。后面发生的你们可以脑补一下。

**3. 事故还原**

在本地建立一个精简版的数据库，并生成了100w的数据。模拟线上发生的情况。

**4. 建立表结构**

**订单表**

```
CREATE TABLE `order_today` (
  `id` varchar(32) NOT NULL COMMENT '主键',
  `merchant_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '商户编号',
  `amount` decimal(15,2) NOT NULL COMMENT '订单金额',
  `pay_success_time` datetime NOT NULL COMMENT '支付成功时间',
  `order_status` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '支付状态  S：支付成功、F：订单支付失败',
  `remark` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间 -- 修改时自动更新',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_merchant_id` (`merchant_id`) USING BTREE COMMENT '商户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
**订单记录表**

```
CREATE TABLE order_record like order_today;
```
**今日订单表数据**
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307135901.png)

**5. 模拟迁移**

把8号之前的数据都迁移到order_record表中去。
```
INSERT INTO order_record SELECT
    * 
FROM
    order_today 
WHERE
    pay_success_time < '2020-03-08 00:00:00';
```
在navicat中运行迁移的sql,同时开另个一个窗口插入数据，模拟下单。
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307135902.png)
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307140003.png)
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307140005.png)
从上面可以发现一开始能正常插入，但是后面突然就卡住了，并且耗费了23s才成功，然后才能继续插入。这个时候已经迁移成功了，所以能正常插入了。

**6. 出现的原因**

在默认的事务隔离级别下：insert into order_record select * from order_today 加锁规则是：order_record表锁，order_today逐步锁（扫描一个锁一个）。

分析执行过程。
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307140006.png)
通过观察迁移sql的执行情况你会发现order_today是全表扫描，也就意味着在执行insert into select from 语句时，mysql会从上到下扫描order_today内的记录并且加锁，这样一来不就和直接锁表是一样了。

这也就可以解释，为什么一开始只有少量用户出现支付失败，后续大量用户出现支付失败，初始化订单失败等情况，因为一开始只锁定了少部分数据，没有被锁定的数据还是可以正常被修改为正常状态。由于锁定的数据越来越多，就导致出现了大量支付失败。最后全部锁住，导致无法插入订单，而出现初始化订单失败。

**7. 解决方案**

由于查询条件会导致order_today全表扫描，什么能避免全表扫描呢，很简单嘛，给pay_success_time字段添加一个idx_pay_suc_time索引就可以了，由于走索引查询，就不会出现扫描全表的情况而锁表了，只会锁定符合条件的记录。

**8. 最终的sql**

```
INSERT INTO order_record 
SELECT * 
FROM
    order_today FORCE INDEX (idx_pay_suc_time)
WHERE
    pay_success_time <= '2020-03-08 00:00:00';
```
执行过程
![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307140107.png)

**总结**

使用 `insert into tablA select * from tableB` 语句时，一定要确保tableB后面的where，order或者其他条件，都需要有对应的索引，来避免出现tableB全部记录被锁定的情况。



## 需要MySQL查询中每一行的序列号

```
SELECT (@row:=@row+1) AS ROW, ID  
FROM TableA ,(SELECT @row := 0) r   
ORDER BY ID DESC
```


## 并发insert操作导致的dead lock

**1. 说明**

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
**2. 初步分析**

1. 122和120 在同一时刻发起了相同的insert 操作  数据一模一样 而 a,b,c 刚好是uniq key

2. 咱们是RC 级别  出现了 GAP lock 这个有点疑问？查阅了下文档 
```
Gap locking can be disabled explicitly. This occurs if you change the transaction isolation level to READ COMMITTED or enable theinnodb_locks_unsafe_for_binlog system variable (which is now deprecated). Under these circumstances, gap locking is disabled for searches and index scans and is used only for foreign-key constraint checking and duplicate-key checking.
```
设置innodb_locks_unsafe_for_binlog或者RC级别来关闭gap  

后面部分 可以理解为 RC级别下的 外键和重复检查的时候也会产生GAP呢

**3. 重现此deadlock**

```
5.5.19-55-log Percona Server (GPL), Release rel24.0, Revision 204

tx_isolation=READ-COMMITTED 

innodb_locks_unsafe_for_binlog=OFF
```

**4. 创建实验表**

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
**5. 事务T1**

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
**6. 事务T2**

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
**7. 事务T3**

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

**8. 事务T1进行rollback**

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

**9. DEADLOCK信息**

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

如上，只能看到事务T2和事务T3最终导致了deadlock；T2等待获取unq_b_c_a唯一key对应的记录锁（X lock）,T3在`unq_b_c_a`对应的记录上持有S锁，并且T3也在等待获取对应的X锁。最终T3被ROLL BACK了，并且发回了DEAD LOCK的提示信息

**10. 综上**

1. SHOW ENGINE INNODB STATUS\G 看到的DEADLOCK相关信息，只会返回最后的2个事务的信息，而其实有可能有更多的事务才最终导致的死锁
2. 当有3个（或以上）事务对相同的表进行insert操作，如果insert对应的字段上有uniq key约束并且第一个事务rollback了，那其中一个将返回死锁错误信息。
3. 死锁的原因
    - T1 获得 X 锁并 insert 成功
    - T2 试图 insert, 检查重复键需要获得 S 锁, 但试图获得 S 锁失败, 加入等待队列, 等待 T1
    - T3 试图 insert, 检查重复键需要获得 S 锁, 但试图获得 S 锁失败, 加入等待队列, 等待 T1
    - T1 rollback, T1 释放锁, 此后 T2, T3 获得 S 锁成功, 检查 duplicate-key, 之后 INSERT 试图获得 X 锁, 但 T2, T3 都已经获得 S 锁, 导致 T2, T3 死锁
4. 避免此DEADLOCK；我们都知道死锁的问题通常都是业务处理的逻辑造成的，既然是uniq key，同时多台不同服务器上的相同程序对其insert一模一样的value，这本身逻辑就不太完美。故解决此问题：
    - 保证业务程序别再同一时间点并发的插入相同的值到相同的uniq key的表中
    - 上述实验可知，是由于第一个事务rollback了才产生的deadlock，查明rollback的原因
    - 尽量减少完成事务的时间

**11. 最终结论**

当有3个（或以上）事务对相同的表进行insert操作，如果insert对应的字段上有uniq key约束并且第一个事务rollback了，那其中一个将返回死锁错误信息。



## 明明已经删除了数据，可是表文件大小依然没变

对于运行很长时间的数据库来说，往往会出现表占用存储空间过大的问题，可是将许多没用的表删除之后，表文件的大小并没有改变，想解决这个问题，就需要了解 InnoDB 如何回收表空间的。

对于一张表来说，占用空间重要分为两部分，表结构和表数据。通常来说，表结构定义占用的空间很小。所以空间的问题主要和表数据有关。

在 MySQL 8.0 前，表结构存储在以 .frm 为后缀的文件里。在 8.0，允许将表结构定义在系统数据表中。

**1. 关于表数据的存放**

可以将表数据存在共享表空间，或者单独的文件中，通过 innodb_file_per_table 来控制。
- 如果为 OFF ，表示存在系统共享表空间中，和数据字典一起
- 如果为 ON，每个 InnoDB 表结构存储在 .idb 为后缀的文件中

在 5.6.6 以后，默认值为 ON.

> 建议将该参数设置为 ON，这样在不需要时，通过 drop table 命令，系统就会直接删除该文件。
> 但在共享表空间中，即使表删掉，空间也不会回收。

```
truncate = drop + create 
```

**2. 数据删除流程**

但有时使用 delete 删除数据时，仅仅删除的是某些行，但这可能就会出现表空间没有被回收的情况。

我们知道，MySQL InnoDB 中采用了 B+ 树作为存储数据的结构，也就是常说的索引组织表，并且数据时按照页来存储的。

在删除数据时，会有两种情况：
- 删除数据页中的某些记录
- 删除整个数据页的内容

比如想要删除 R4 这条记录：
![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141108.png)
InnoDB 直接将 R4 这条记录标记为删除，称为可复用的位置。如果之后要插入 ID 在 300 到 700 间的记录时，就会复用该位置。由此可见，磁盘文件的大小并不会减少。

而且记录的复用，只限于符合范围条件的数据。之后要插入 ID 为 800 的记录，R4 的位置就不能被复用了。

再比如要是删除了整个数据页的内容，假设删除 R3 R4 R5，为 Page A 数据页。

这时 InnoDB 就会将整个 Page A 标记为删除状态，之后整个数据都可以被复用，没有范围的限制。比如要插入 ID=50 的内容就可以直接复用。

并且如果两个相邻的数据页利用率都很小，就会把两个页中的数据合到其中一个页上，另一个页标记为可复用。

综上，无论是数据行的删除还是数据页的删除，都是将其标记为删除的状态，用于复用，所以文件并不会减小。对应到具体的操作就是使用 delete 命令.

而且，我们还可以发现，对于第一种删除记录的情况，由于复用时会有范围的限制，所以就会出现很多空隙的情况，比如删除 R4，插入的却是 ID=800.

**3. 插入操作也会造成空隙**

在插入数据时，如果数据按照索引递增顺序插入，索引的结构会是紧凑的。但如果是随机插入的，很可能造成索引数据页分裂。

比如给已满的 Page A 插入数据。
![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141109.png)
由于 Page A 满了，所以要申请 Page B，调整 Page A 的过程到 Page B，这也称为页分裂。

结束后 Page A 就有了空隙。

另外对于更新操作也是，先删除再插入，也会造成空隙。

进而对于大量进行增删改的表，都有可能存在空洞。如果把空洞去掉，自然空间就被释放了。

**4. 使用重建表**

为了把表中的空隙去掉，这时就可以采用重新建一个与表 A 结构相同的表 B，然后按照主键 ID 递增的顺序，把数据依次插入到 B 表中。

由于是顺序插入，自然 B 表的空隙不存在，数据页的利用率也更高。之后用表 B 代替表 A，好像起到了收缩表 A 空间的作用。

具体通过:
```
alter table A engine=InnoDB
```
在 5.5 版本后，该命令和上面提到的流程差不多，而且 MySQL 会自己完成数据，交换表名，删除旧表的操作。
![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141210.png)
但这就有一个问题，在 DDL 中，表 A 不能有更新，此时有数据写入表 A 的话，就会造成数据丢失。

在 5.6 版本后引入了 Online DDL。

**5. Online DDL**

Online DDL 在其基础上做了如下的更新：
![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141211.png)
重建表的过程如下：

1. 建立一个临时文件，扫描表 A 主键的所有数据页。
1. 用生成的数据页生成 B+ 树，存储到临时文件中。
1. 生成临时文件时，如果有对 A 的操作，将其记录在日志文件中，对应图中 state 2 的状态。
1. 临时文件生成后，将日志文件应用到临时文件中，得到与 A 表相同的数据文件，对应 state 3 状态。
1. 用临时文件替换 A 表的数据文件。
1. 由于 row log 日志文件存在，可以在重建表示，对表 A 进行 DML 操作。

需要注意的是，在 alter 语句执行前，会先申请 MDL 写锁，但在拷贝数据前会退化成 MDL 读锁，从而支持 DML 操作。

至于为什么不大 MDL 去掉，是防止其他线程对这个表同时做 DDL 操作。

对于大表来说，该操作很耗 IO 和 CPU 资源，所以在线上操作时，要控制操作时间。如果为了保证安全，推荐使用 gh-ost 来迁移。

**6. Online 和 inplace**

首先说一下 inplace 和 copy 的区别：

在 Online DDL 中，表 A 重建后的数据放在 tmp_file 中，这个临时文件是在 InnoDB 内部创建出来的。整个 DDL 在 InnoDB 内部完成。进而对于 Server 层来说，并没有数据移动到临时表中，是一个 "原地" 操作，所以叫 "inplace" .

而在之前普通的 DDL 中，创建后的表 A 是在 tmp_table 是 Server 创建的，所以叫 "copy"

对应到语句其实就是：
```
# alter table t engine=InnoDB 默认为下面
alter table t engine=innodb,ALGORITHM=inplace;

# 走的就是 server 拷贝的过程
alter table t engine=innodb,ALGORITHM=copy;
```
需要注意的是 inplace 和 Online 并不是对应关系：
1. DDL 过程是 Online，则一定是 inplace
1. 如果是 inplace 的 DDL 不应当是 Online，如在 <= 8.0, 添加全文索引和空间索引就属于这种情况。

**7. 拓展**

说一下 optimize，analyze，alter table 三种重建表之间的区别：
1. alter table t engine = InnoDB（也就是 recreate）默认的是 Oline DDL 过程。
1. analyze table t 不是重建表，仅仅是对表的索引信息做重新统计，没有修改数据，期间加 MDL 读锁。
1. optimize table t 等于上两步的操作。

> 在事务里面使用 alter table 默认会自动提交事务，保持事务一致性

如果有时，在重建某张表后，空间不仅没有变小，甚至还变大了一点点。这时因为，重建的这张表本身没有空隙，在 DDL 期间，刚好有一些 DML 执行，引入了一些新的空隙。

而且 InnoDB 不会把整张表填满，每个页留下 1/16 给后续的更新用，所以可能远离是紧凑的，但重建后变成的稍有空隙。

**8. 总结**

现在我们知道，在使用 delete 删除数据时，其实对应的数据行并不是真正的删除，InnoDB 仅仅是将其标记成可复用的状态，所以表空间不会变小。

通常来说，在标记复用空间时分为两种，一种是仅将某些数据页中的位置标记为删除状态，但这样的位置只会在一定范围内使用，会出现空隙的情况。

另一种是将整个数据页标记成可复用的状态，这样的数据页没有限制，可直接复用。

为了解决这个问题，我们可以采用重建表的方式，其中在 5.6 版本后，创建表已经支持 Online 的操作，但最后是在业务低峰时使用



## 一千个不用 Null 的理由！

**一、NULL 为什么这么多人用？**

NULL是创建数据表时默认的，初级或不知情的或怕麻烦的程序员不会注意这点。
很多人员都以为not null 需要更多空间，其实这不是重点。

重点是很多程序员觉得NULL在开发中不用去判断插入数据，写sql语句的时候更方便快捷。

**二、是不是以讹传讹？**

MySQL 官网文档：
> NULL columns require additional space in the rowto record whether their values are NULL. For MyISAM tables, each NULL columntakes one bit extra, rounded up to the nearest byte.

Mysql难以优化引用可空列查询，它会使索引、索引统计和值更加复杂。可空列需要更多的存储空间，还需要mysql内部进行特殊处理。可空列被索引后，每条记录都需要一个额外的字节，还能导致MYisam 中固定大小的索引变成可变大小的索引。
—— 出自《高性能mysql第二版》

照此分析，还真不是以讹传讹，这是有理论依据和出处的。

**三、给我一个不用 Null 的理由？**

1. 所有使用NULL值的情况，都可以通过一个有意义的值的表示，这样有利于代码的可读性和可维护性，并能从约束上增强业务数据的规范性。
	
	> NULL值到非NULL的更新无法做到原地更新，更容易发生索引分裂，从而影响性能。
2. 注意：但把NULL列改为NOT NULL带来的性能提示很小，除非确定它带来了问题，否则不要把它当成优先的优化措施，最重要的是使用的列的类型的适当性。
3. NULL值在timestamp类型下容易出问题，特别是没有启用参数explicit_defaults_for_timestamp
4. NOT IN、!= 等负向条件查询在有 NULL 值的情况下返回永远为空结果，查询容易出错

举例：
```
create table table_2 (
     `id` INT (11) NOT NULL,
    user_name varchar(20) NOT NULL
)


create table table_3 (
     `id` INT (11) NOT NULL,
    user_name varchar(20)
)

insert into table_2 values (4,"zhaoliu_2_1"),(2,"lisi_2_1"),(3,"wangmazi_2_1"),(1,"zhangsan_2"),(2,"lisi_2_2"),(4,"zhaoliu_2_2"),(3,"wangmazi_2_2")

insert into table_3 values (1,"zhaoliu_2_1"),(2, null)

-- 1、NOT IN子查询在有NULL值的情况下返回永远为空结果，查询容易出错
select user_name from table_2 where user_name not in (select user_name from table_3 where id!=1)

mysql root@10.48.186.32:t_test_zz5431> select user_name from table_2 where user_name not
                                    -> in (select user_name from table_3 where id!=1);
+-------------+
| user_name   |
|-------------|
+-------------+
0 rows in set
Time: 0.008s
mysql root@10.48.186.32:t_test_zz5431>

-- 2、单列索引不存null值，复合索引不存全为null的值，如果列允许为null，可能会得到“不符合预期”的结果集
-- 如果name允许为null，索引不存储null值，结果集中不会包含这些记录。所以，请使用not null约束以及默认值。
select * from table_3 where name != 'zhaoliu_2_1'

-- 3、如果在两个字段进行拼接：比如题号+分数，首先要各字段进行非null判断，否则只要任意一个字段为空都会造成拼接的结果为null。
select CONCAT("1",null) from dual; -- 执行结果为null。

-- 4、如果有 Null column 存在的情况下，count(Null column)需要格外注意，null 值不会参与统计。
mysql root@10.48.186.32:t_test_zz5431> select * from table_3;
+------+-------------+
|   id | user_name   |
|------+-------------|
|    1 | zhaoliu_2_1 |
|    2 | <null>      |
|   21 | zhaoliu_2_1 |
|   22 | <null>      |
+------+-------------+
4 rows in set
Time: 0.007s
mysql root@10.48.186.32:t_test_zz5431> select count(user_name) from table_3;
+--------------------+
|   count(user_name) |
|--------------------|
|                  2 |
+--------------------+
1 row in set
Time: 0.007s

-- 5、注意 Null 字段的判断方式， = null 将会得到错误的结果。
mysql root@localhost:cygwin> create index IDX_test on table_3 (user_name);
Query OK, 0 rows affected
Time: 0.040s
mysql root@localhost:cygwin>  select * from table_3 where user_name is null\G
***************************[ 1. row ]***************************
id        | 2
user_name | None

1 row in set
Time: 0.002s
mysql root@localhost:cygwin> select * from table_3 where user_name = null\G

0 rows in set
Time: 0.002s
mysql root@localhost:cygwin> desc select * from table_3 where user_name = 'zhaoliu_2_1'\G
***************************[ 1. row ]***************************
id            | 1
select_type   | SIMPLE
table         | table_3
type          | ref
possible_keys | IDX_test
key           | IDX_test
key_len       | 23
ref           | const
rows          | 1
Extra         | Using where

1 row in set
Time: 0.006s
mysql root@localhost:cygwin> desc select * from table_3 where user_name = null\G
***************************[ 1. row ]***************************
id            | 1
select_type   | SIMPLE
table         | None
type          | None
possible_keys | None
key           | None
key_len       | None
ref           | None
rows          | None
Extra         | Impossible WHERE noticed after reading const tables

1 row in set
Time: 0.002s
mysql root@localhost:cygwin> desc select * from table_3 where user_name is null\G
***************************[ 1. row ]***************************
id            | 1
select_type   | SIMPLE
table         | table_3
type          | ref
possible_keys | IDX_test
key           | IDX_test
key_len       | 23
ref           | const
rows          | 1
Extra         | Using where

1 row in set
Time: 0.002s
mysql root@localhost:cygwin>
```
5. Null 列需要更多的存储空间：需要一个额外字节作为判断是否为 NULL 的标志位

举例：
```
alter table table_3 add index idx_user_name (user_name);
alter table table_2 add index idx_user_name (user_name);
explain select * from table_2 where user_name='zhaoliu_2_1';
explain select * from table_3 where user_name='zhaoliu_2_1';
```
![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141412.png)
可以看到同样的 varchar(20) 长度，table_2 要比 table_3 索引长度大，这是因为：

两张表的字符集不一样，且字段一个为 NULL 一个非 NULL。
![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141413.png)
key_len 的计算规则和三个因素有关：数据类型、字符编码、是否为 NULL

1. key_len 62 == 20*3（utf8 3字节） + 2 （存储 varchar 变长字符长度 2字节，定长字段无需额外的字节）
1. key_len 83 == 20*4（utf8mb4 4字节） + 1 (是否为 Null 的标识) + 2 （存储 varchar 变长字符长度 2字节，定长字段无需额外的字节）

所以说索引字段最好不要为NULL，因为NULL会使索引、索引统计和值更加复杂，并且需要额外一个字节的存储空间。基于以上这些理由和原因，我想咱们不用 Null 的理由应该是够了 :)