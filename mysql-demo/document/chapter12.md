[toc]

# MySQL 常见问题



## 一、同事给我埋了个坑：Insert into select语句把生产服务器炸了

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

使用 `insert into tableA select * from tableB` 语句时，一定要确保tableB后面的where，order或者其他条件，都需要有对应的索引，来避免出现tableB全部记录被锁定的情况。



## 二、MySQL 服务占用cpu 100%，如何排查问题?

### 1、引子

对于互联网公司，线上CPU飙升的问题很常见（例如某个活动开始，流量突然飙升时），按照本文的步骤排查，基本1分钟即可搞定！特此整理排查方法一篇，供大家参考讨论提高。

### 2、问题复现

线上系统突然运行缓慢，CPU飙升，甚至到100%，以及Full GC次数过多，接着就是各种报警：例如接口超时报警等。此时急需快速线上排查问题。

### 3、问题排查

不管什么问题，既然是CPU飙升，肯定是查一下耗CPU的线程，然后看看GC。

#### 3.1 核心排查步骤

1. 执行`top`命令：查看所有进程占系统CPU的排序。极大可能排第一个的就是咱们的java进程（COMMAND列）。PID那一列就是进程号。
2. 执行`top -Hp 进程号`命令：查看java进程下的所有线程占CPU的情况。
3. 执行`printf "%x\n 10`命令 ：后续查看线程堆栈信息展示的都是十六进制，为了找到咱们的线程堆栈信息，咱们需要把线程号转成16进制。例如,printf "%x\n 10-》打印：a，那么在jstack中线程号就是0xa.
4. 执行 `jstack 进程号 | grep 线程ID` 查找某进程下-》线程ID（jstack堆栈信息中的nid）=0xa的线程状态。如果`"VM Thread" os_prio=0 tid=0x00007f871806e000 nid=0xa runnable`，第一个双引号圈起来的就是线程名，如果是“VM Thread”这就是虚拟机GC回收线程了
5. 执行`jstat -gcutil 进程号 统计间隔毫秒 统计次数（缺省代表一致统计）`，查看某进程GC持续变化情况，如果发现返回中FGC很大且一直增大-》确认Full GC! 也可以使用`jmap -heap 进程ID`查看一下进程的堆内从是不是要溢出了，特别是老年代内从使用情况一般是达到阈值(具体看垃圾回收器和启动时配置的阈值)就会进程Full GC。
6. 执行`jmap -dump:format=b,file=filename 进程ID`，导出某进程下内存heap输出到文件中。可以通过eclipse的mat工具查看内存中有哪些对象比较多。

#### 3.2 原因分析

**3.2.1.内存消耗过大，导致Full GC次数过多**

执行步骤1-5：

- 多个线程的CPU都超过了100%，通过jstack命令可以看到这些线程主要是垃圾回收线程-》上一节步骤2
- 通过jstat命令监控GC情况，可以看到Full GC次数非常多，并且次数在不断增加。--》上一节步骤5

确定是Full GC,接下来找到具体原因：

- 生成大量的对象，导致内存溢出-》执行步骤6，查看具体内存对象占用情况。
- 内存占用不高，但是Full GC次数还是比较多，此时可能是代码中手动调用 System.gc()导致GC次数过多，这可以通过添加 -XX:+DisableExplicitGC来禁用JVM对显示GC的响应。

**3.2.2.代码中有大量消耗CPU的操作，导致CPU过高，系统运行缓慢；**

执行步骤1-4：在步骤4jstack，可直接定位到代码行。例如某些复杂算法，甚至算法BUG，无限循环递归等等。

**3.2.3.由于锁使用不当，导致死锁。**

执行步骤1-4：如果有死锁，会直接提示。关键字：deadlock.步骤四，会打印出业务死锁的位置。

造成死锁的原因：最典型的就是2个线程互相等待对方持有的锁。

**3.2.4.随机出现大量线程访问接口缓慢。**

代码某个位置有阻塞性的操作，导致该功能调用整体比较耗时，但出现是比较随机的；平时消耗的CPU不多，而且占用的内存也不高。

**思路：**

首先找到该接口，通过压测工具不断加大访问力度，大量线程将阻塞于该阻塞点。

执行步骤1-4：

```
"http-nio-8080-exec-4" #31 daemon prio=5 os_prio=31 tid=0x00007fd08d0fa000 nid=0x6403 waiting on condition [0x00007000033db000]

   java.lang.Thread.State: TIMED_WAITING (sleeping)-》期限等待

    at java.lang.Thread.sleep(Native Method)

    at java.lang.Thread.sleep(Thread.java:340)

    at java.util.concurrent.TimeUnit.sleep(TimeUnit.java:386)

    at com.*.user.controller.UserController.detail(UserController.java:18)-》业务代码阻塞点
```

如上，找到业务代码阻塞点，这里业务代码使用了TimeUnit.sleep()方法，使线程进入了TIMED_WAITING(期限等待)状态。

**3.2.5.某个线程由于某种原因而进入WAITING状态，此时该功能整体不可用，但是无法复现；**

执行步骤1-4：jstack多查询几次，每次间隔30秒，对比一直停留在parking 导致的WAITING状态的线程。

例如CountDownLatch倒计时器，使得相关线程等待->AQS->LockSupport.park()。

```
"Thread-0" #11 prio=5 os_prio=31 tid=0x00007f9de08c7000 nid=0x5603 waiting on condition [0x0000700001f89000]   
java.lang.Thread.State: WAITING (parking) ->无期限等待
at sun.misc.Unsafe.park(Native Method)    
at java.util.concurrent.locks.LockSupport.park(LockSupport.java:304)    
at com.*.SyncTask.lambda$main$0(SyncTask.java:8)-》业务代码阻塞点
at com.*.SyncTask$$Lambda$1/1791741888.run(Unknown Source)    
at java.lang.Thread.run(Thread.java:748)
```

### 4、总结

按照3.1节的6个步骤走下来，基本都能找到问题所在。



## 三、排查Mysql突然变慢的一次过程

> 上周客户说系统突然变得很慢，而且时不时的蹦出一个 `404` 和 `500`，弄得真的是很没面子，而恰巧出问题的时候正在深圳出差，所以一直没有时间
> 看问题，一直到今天，才算是把问题原因找到。

------

### 定位问题

刚开始得到是系统慢的反馈，没有将问题点定位到数据库上，查了半天服务是否正常（因为之前有一次Dubbo内存泄漏）。

在将应用服务日志查看了一遍后，没有发现任何异常，只是打了几个警告的日志。

于是又查看了业务运行时的日志，看到日志都提示了一个 `Lock wait timeout exceeded; try restarting transaction` 的异常。

这时还是没有将重心放到数据库上，认为是代码的原因，导致事务一直没有提交。

重新将代码审阅了一遍，觉得应该不是代码逻辑的问题，而这个时候， `Lock wait timeout exceeded; try restarting transaction` 这个异常的日志越来越多。

认为是数据库层面出了问题，开始排查数据库。

------

### 寻找原因

由于我们的数据库不是用的 `云RDS版本`，是在一台8核32G的AWS上的安装版本。

使用 `top` 命令，查看到 Mysql 占用的 CPU 使用率高达 90% 左右。

心里一慌，感觉不妙，这样子高负载的CPU使用率，搞不好服务器都要宕掉。

于是拿出了仅有的一点Mysql基本知识，基本上这次只使用到了下面几个语句：

- 查看当前Mysql所有的进程

```
show processlist;
```

- 查看Mysql的最大缓存

```
show global variables like "global max_allowed_packet"
```

- 查看当前正在进行的事务

```
select * from information_schema.INNODB_TRX
```

- 查看当前Mysql的连接数

```
show status like 'thread%'
```

------

### 解决

按照上面的几个语句，一步一步跟踪定位下来。

> `show processlist;` 下来，我们就可以查看出当前所有的进程，并且得到最耗时的进程。

在当前数据库中，看到处于 `Sleep` 状态的SQL非常多，而这也是占用CPU过高的重大原因，休眠线程太多，于是配置了一个 `wait_time_out` 为 600 秒的一个解决方案。

为什么配置600秒，因为我们应用超时时间配置的最大时间就是 600秒，10分钟，这里的配置需要根据业务来具体配置。

> ```
> select * from information_schema.INNODB_TRX
> ```

执行这个语句，看到Mysql中大部分处于 `Lock` 的SQL是一条 update 的SQL，而且还有一个单条件的SQL，查询居然耗时4分钟，很是惊讶。

于是查看了这张表。

刚一打开结构，差点没忍住口吐芬芳，居然一个索引都没有，数据量超过300W，没有索引查询基本上都要4分钟往上走。

于是准备加上索引，在一阵漫长的等待中，索引终于加上去了。

> ```
> show status like 'thread%'
> ```

索引加上去了之后，查看了一下当前Mysql的连接数，似乎没有之前那么高了，估计是挤压的太多。

然后又查看了下服务器的CPU占用率，这次好了一点，从1%到80%来回跳动，没有出现90&那么高的频率。

------

### 总结

Mysql作为使用频率非常高的数据库，对于它的SQL调优真的是一门技术活，而且项目中的一些SQL看的也是想吐，这种调优起来真的难上加难。

其实 `information_schema` 这个数据库，里面的Mysql日志看起来比业务日志顺眼的很多。



## 四、MySQL ERROR 1040: Too many connections

如题，本章主要讲下当服务器出现` ERROR 1040: Too many connections`错误时的一些处理心得。

### max_connections查看

```
## 查看最大连接数``SHOW VARIABLES ``LIKE` `"max_connections"``;``+``-----------------+-------+``| Variable_name  | Value |``+``-----------------+-------+``| max_connections | 512  |``+``-----------------+-------+` `## 查看已使用最大连接数``SHOW VARIABLES ``LIKE` `'Max_used_connections'``;``+``----------------------+-------+``| Variable_name    | Value |``+``----------------------+-------+``| Max_used_connections | 499  |``+``----------------------+-------+　
```

### 处理方案

这个问题一般有两种处理方案，解决方案非常容易，我们只需要增加`max_connections`连接数即可。

**1、增加当前会话的mysql最大连接数**

```
SET GLOBAL max_connections = 1000;
```

上面mysql连接值临时增加到1000，但仅适用于当前会话。一旦我们重新启动mysql服务或重新启动系统，该值将重置为默认值。

**2、永久增加mysql最大连接数**

为了永久增加mysql连接数，我们需要编辑mysql配置文件，即`/etc/my.cnf`。

```
sudo vim /etc/my.cnf
## 修改``max_connections = 1000
```

保存文件重启MySQL即可生效。

### 扩多少合适？

`Max_connextions`并不是越大越好的，那么如何配置？

**方式一**

对于提高MySQL的并发，很大程度取决于内存，官方提供了一个关于`innodb`的内存[计算方式](https://dev.mysql.com/doc/refman/8.0/en/innodb-init-startup-configuration.html)：

```
innodb_buffer_pool_size``+ key_buffer_size``+ max_connections * (sort_buffer_size + read_buffer_size + binlog_cache_size)``+ max_connections * 2MB
```

**方式二**

安装比例扩容：

```
max_used_connections / max_connections * 100% = [85, 90]%
```

`最大使用连接数/最大连接数`达到了80%～90%区间，就建议进行优化或者扩容了。

### 扩展

以下也涉及几种常见的影响MySQL性能的情况：

**1、线程**

```
SHOW STATUS ``LIKE` `'Threads%'``;``+``-------------------+--------+``| Variable_name   | Value |``+``-------------------+--------+``| Threads_cached  | 1   |``| Threads_connected | 217  |``| Threads_created  | 29   |``| Threads_running  | 88   |``+``-------------------+--------+` `SHOW VARIABLES ``LIKE` `'thread_cache_size'``;``+``-------------------+-------+``| Variable_name   | Value |``+``-------------------+-------+``| thread_cache_size | 10   |``+``-------------------+-------+
```

- Threads_cached 线程在缓存中的数量
- Threads_connected 当前打开的连接数
- Threads_created 创建用于处理连接的线程数。
- Threads_running 未休眠的线程数

如果Threads_created大，则可能要增加thread_cache_size值。缓存未命中率可以计算为Threads_created / Connections

**2、查看表锁情况**

```
SHOW ``GLOBAL` `STATUS ``LIKE` `'table_locks%'``;``+``-----------------------+-------+``| Variable_name     | Value |``+``-----------------------+-------+``| Table_locks_immediate | 90  |``| Table_locks_waited  | 0   |``+``-----------------------+-------+
```

- Table_locks_immediate 立即获得表锁请求的次数
- Table_locks_waited 无法立即获得对表锁的请求的次数，需要等待。这个值过高说明性能可能出现了问题，并影响连接的释放

**3、慢查询**

```
show variables ``like` `'%slow%'``;` `+``---------------------------+----------------------------------------------+``| Variable_name       | Value                    |``+``---------------------------+----------------------------------------------+``| slow_launch_time     | 2                      |``| slow_query_log      | ``On`                      `|``+``---------------------------+----------------------------------------------+
```

**4、线程详情**

```
## 查看每个线程的详细信息``SHOW PROCESSLIST;``+``--------+----------+------------------+--------------+---------+-------+-------------+------------------+``| Id   | ``User`   `| Host       | db      | Command | ``Time` `| State    | Info       |``+``--------+----------+------------------+--------------+---------+-------+-------------+------------------+``|   3 | xxxadmin | localhost    | ``NULL`     `| Sleep  |   1 | cleaning up | ``NULL`       `|``|   4 | xxxadmin | localhost    | ``NULL`     `| Sleep  |   0 | cleaning up | ``NULL`       `|``|   5 | xxxadmin | localhost    | ``NULL`     `| Sleep  |   6 | cleaning up | ``NULL`       `|``+``--------+----------+------------------+--------------+---------+-------+-------------+------------------+
```

### 总结

当然，以上只是一个大概的解决思路，无论使用哪一种方式，都需要结合实际业务场景去扩容。

另外，对于生产环境，设置恰当的告警阈值，也是很有必要的。

最后，在编程时，由于用MySQL语句调用数据库执行`SQL`，会分配一个线程操作MySQL，所以在结束调用后，需要回收连接，避免泄漏。







