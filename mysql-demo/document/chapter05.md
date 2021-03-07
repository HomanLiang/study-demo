[toc]

# MySQL 日志











## 慢查询日志
### 概念
MySQL的慢查询日志是MySQL提供的一种日志记录，它用来记录在MySQL中响应时间超过阀值的语句，具体指运行时间超过`long_query_time`值的SQL，则会被记录到慢查询日志中。`long_query_time`的默认值为10，意思是运行10S以上的语句。默认情况下，Mysql数据库并不启动慢查询日志，需要我们手动来设置这个参数，当然，如果不是调优需要的话，一般不建议启动该参数，因为开启慢查询日志会或多或少带来一定的性能影响。慢查询日志支持将日志记录写入文件，也支持将日志记录写入数据库表。
### 相关参数
MySQL 慢查询的相关参数解释：

- `slow_query_log`：是否开启慢查询日志，1表示开启，0表示关闭。
- l`og-slow-queries`：旧版（5.6以下版本）MySQL数据库慢查询日志存储路径。可以不设置该参数，系统则会默认给一个缺省的文件`host_name-slow.log`
- `slow-query-log-file`：新版（5.6及以上版本）MySQL数据库慢查询日志存储路径。可以不设置该参数，系统则会默认给一个缺省的文件`host_name-slow.log`
- `long_query_time`：慢查询阈值，当查询时间多于设定的阈值时，记录日志。
- `log_queries_not_using_indexes`：未使用索引的查询也被记录到慢查询日志中（可选项）。
- `log_output`：日志存储方式。log_output='FILE'表示将日志存入文件，默认值是`'FILE'`。`log_output='TABLE'`表示将日志存入数据库，这样日志信息就会被写入到`mysql.slow_log`表中。MySQL数据库支持同时两种日志存储方式，配置的时候以逗号隔开即可，如：`log_output='FILE,TABLE'`。日志记录到系统的专用日志表中，要比记录到文件耗费更多的系统资源，因此对于需要启用慢查询日志，又需要能够获得更高的系统性能，那么建议优先记录到文件。
### 慢查询日志配置
默认情况下`slow_query_log`的值为`OFF`，表示慢查询日志是禁用的，可以通过设置`slow_query_log`的值来开启，如下所示：
```
mysql> show variables  like '%slow_query_log%';
+---------------------+-----------------------------------------------+
| Variable_name       | Value                                         |
+---------------------+-----------------------------------------------+
| slow_query_log      | OFF                                           |
| slow_query_log_file | /home/WDPM/MysqlData/mysql/DB-Server-slow.log |
+---------------------+-----------------------------------------------+
2 rows in set (0.00 sec)
 
mysql> set global slow_query_log=1;
Query OK, 0 rows affected (0.09 sec)
 
mysql> show variables like '%slow_query_log%';
+---------------------+-----------------------------------------------+
| Variable_name       | Value                                         |
+---------------------+-----------------------------------------------+
| slow_query_log      | ON                                            |
| slow_query_log_file | /home/WDPM/MysqlData/mysql/DB-Server-slow.log |
+---------------------+-----------------------------------------------+
2 rows in set (0.00 sec)
 
mysql> 
```
使用`set global slow_query_log=1`开启了慢查询日志只对当前数据库生效，如果`MySQL`重启后则会失效。如果要永久生效，就必须修改配置文件`my.cnf`（其它系统变量也是如此）。例如如下所示：
```
mysql> show variables like 'slow_query%';
+---------------------+-----------------------------------------------+
| Variable_name       | Value                                         |
+---------------------+-----------------------------------------------+
| slow_query_log      | OFF                                           |
| slow_query_log_file | /home/WDPM/MysqlData/mysql/DB-Server-slow.log |
+---------------------+-----------------------------------------------+
2 rows in set (0.01 sec)
 
mysql> 
```
修改`my.cnf`文件，增加或修改参数slow_query_log 和slow_query_log_file后，然后重启MySQL服务器，如下所示
```
slow_query_log =1

slow_query_log_file=/tmp/mysql_slow.log
```
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307104601.png)
关于慢查询的参数slow_query_log_file ，它指定慢查询日志文件的存放路径，系统默认会给一个缺省的文件`host_name-slow.log`（如果没有指定参数`slow_query_log_file`的话）

```
mysql> show variables like 'slow_query_log_file';
+---------------------+-----------------------------------------------+
| Variable_name       | Value                                         |
+---------------------+-----------------------------------------------+
| slow_query_log_file | /home/WDPM/MysqlData/mysql/DB-Server-slow.log |
+---------------------+-----------------------------------------------+
1 row in set (0.00 sec)
```
那么开启了慢查询日志后，什么样的SQL才会记录到慢查询日志里面呢？ 这个是由参数`long_query_time`控制，默认情况下`long_query_time`的值为10秒，可以使用命令修改，也可以在`my.cnf`参数里面修改。关于运行时间正好等于`long_query_time`的情况，并不会被记录下来。也就是说，在mysql源码里是判断大于`long_query_time`，而非大于等于。从MySQL 5.1开始，`long_query_time`开始以微秒记录SQL语句运行时间，之前仅用秒为单位记录。如果记录到表里面，只会记录整数部分，不会记录微秒部分。
```
mysql> show variables like 'long_query_time%';
+-----------------+-----------+
| Variable_name   | Value     |
+-----------------+-----------+
| long_query_time | 10.000000 |
+-----------------+-----------+
1 row in set (0.00 sec)
 
mysql> set global long_query_time=4;
Query OK, 0 rows affected (0.00 sec)
 
mysql> show variables like 'long_query_time';
+-----------------+-----------+
| Variable_name   | Value     |
+-----------------+-----------+
| long_query_time | 10.000000 |
+-----------------+-----------+
1 row in set (0.00 sec)
```
如上所示，我修改了变量`long_query_time`，但是查询变量`long_query_time`的值还是10，难道没有修改到呢？注意：使用命令 `set global long_query_time=4`修改后，需要重新连接或新开一个会话才能看到修改值。你用`show variables like 'long_query_time'`查看是当前会话的变量值，你也可以不用重新连接会话，而是用`show global variables like 'long_query_time'; `如下所示：
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307104802.png)
在MySQL里面执行下面SQL语句，然后我们去检查对应的慢查询日志，就会发现类似下面这样的信息。

```
mysql> select sleep(3);
+----------+
| sleep(3) |
+----------+
|        0 |
+----------+
1 row in set (3.00 sec)
 
[root@DB-Server ~]# more /tmp/mysql_slow.log
/usr/sbin/mysqld, Version: 5.6.20-enterprise-commercial-advanced-log (MySQL Enterprise Server - Advanced Edition (Commercial)). started with:
Tcp port: 0  Unix socket: (null)
Time                 Id Command    Argument
/usr/sbin/mysqld, Version: 5.6.20-enterprise-commercial-advanced-log (MySQL Enterprise Server - Advanced Edition (Commercial)). started with:
Tcp port: 0  Unix socket: (null)
Time                 Id Command    Argument
# Time: 160616 17:24:35
# User@Host: root[root] @ localhost []  Id:     5
# Query_time: 3.002615  Lock_time: 0.000000 Rows_sent: 1  Rows_examined: 0
SET timestamp=1466069075;
select sleep(3);
```
`log_output` 参数是指定日志的存储方式。`log_output='FILE'`表示将日志存入文件，默认值是`'FILE'`。`log_output='TABLE'`表示将日志存入数据库，这样日志信息就会被写入到`mysql.slow_log`表中。MySQL数据库支持同时两种日志存储方式，配置的时候以逗号隔开即可，如：`log_output='FILE,TABLE'`。日志记录到系统的专用日志表中，要比记录到文件耗费更多的系统资源，因此对于需要启用慢查询日志，又需要能够获得更高的系统性能，那么建议优先记录到文件。

```
mysql> show variables like '%log_output%';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| log_output    | FILE  |
+---------------+-------+
1 row in set (0.00 sec)
 
mysql> set global log_output='TABLE';
Query OK, 0 rows affected (0.00 sec)
 
mysql> show variables like '%log_output%';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| log_output    | TABLE |
+---------------+-------+
1 row in set (0.00 sec)
 
mysql> select sleep(5) ;
+----------+
| sleep(5) |
+----------+
|        0 |
+----------+
1 row in set (5.00 sec)
 
mysql> 
 
mysql> select * from mysql.slow_log;
+---------------------+---------------------------+------------+-----------+-----------+---------------+----+----------------+-----------+-----------+-----------------+-----------+
| start_time          | user_host                 | query_time | lock_time | rows_sent | rows_examined | db | last_insert_id | insert_id | server_id | sql_text        | thread_id |
+---------------------+---------------------------+------------+-----------+-----------+---------------+----+----------------+-----------+-----------+-----------------+-----------+
| 2016-06-16 17:37:53 | root[root] @ localhost [] | 00:00:03   | 00:00:00  |         1 |             0 |    |              0 |         0 |         1 | select sleep(3) |         5 |
| 2016-06-16 21:45:23 | root[root] @ localhost [] | 00:00:05   | 00:00:00  |         1 |             0 |    |              0 |         0 |         1 | select sleep(5) |         2 |
+---------------------+---------------------------+------------+-----------+-----------+---------------+----+----------------+-----------+-----------+-----------------+-----------+
2 rows in set (0.00 sec)
 
mysql> 
```
系统变量`log-queries-not-using-indexes`：未使用索引的查询也被记录到慢查询日志中（可选项）。如果调优的话，建议开启这个选项。另外，开启了这个参数，其实使用`full index scan`的`sql`也会被记录到慢查询日志。
```
mysql> show variables like 'log_queries_not_using_indexes';
+-------------------------------+-------+
| Variable_name                 | Value |
+-------------------------------+-------+
| log_queries_not_using_indexes | OFF   |
+-------------------------------+-------+
1 row in set (0.00 sec)
 
mysql> set global log_queries_not_using_indexes=1;
Query OK, 0 rows affected (0.00 sec)
 
mysql> show variables like 'log_queries_not_using_indexes';
+-------------------------------+-------+
| Variable_name                 | Value |
+-------------------------------+-------+
| log_queries_not_using_indexes | ON    |
+-------------------------------+-------+
1 row in set (0.00 sec)
 
mysql> 
```
系统变量`log_slow_admin_statements`表示是否将慢管理语句例如`ANALYZE TABLE`和`ALTER TABLE`等记入慢查询日志
```
mysql> show variables like 'log_slow_admin_statements';
+---------------------------+-------+
| Variable_name             | Value |
+---------------------------+-------+
| log_slow_admin_statements | OFF   |
+---------------------------+-------+
1 row in set (0.00 sec)
 
mysql> 
```
另外，如果你想查询有多少条慢查询记录，可以使用系统变量。
```
mysql> show global status like '%Slow_queries%';
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| Slow_queries  | 2104  |
+---------------+-------+
1 row in set (0.00 sec)
 
mysql> 
```
### 日志分析工具mysqldumpslow
在生产环境中，如果要手工分析日志，查找、分析SQL，显然是个体力活，MySQL提供了日志分析工具mysqldumpslow

查看mysqldumpslow的帮助信息：
```
[root@DB-Server ~]# mysqldumpslow --help
Usage: mysqldumpslow [ OPTS... ] [ LOGS... ]
 
Parse and summarize the MySQL slow query log. Options are
 
  --verbose    verbose
  --debug      debug
  --help       write this text to standard output
 
  -v           verbose
  -d           debug
  -s ORDER     what to sort by (al, at, ar, c, l, r, t), 'at' is default
                al: average lock time
                ar: average rows sent
                at: average query time
                 c: count
                 l: lock time
                 r: rows sent
                 t: query time  
  -r           reverse the sort order (largest last instead of first)
  -t NUM       just show the top n queries
  -a           don't abstract all numbers to N and strings to 'S'
  -n NUM       abstract numbers with at least n digits within names
  -g PATTERN   grep: only consider stmts that include this string
  -h HOSTNAME  hostname of db server for *-slow.log filename (can be wildcard),
               default is '*', i.e. match all
  -i NAME      name of server instance (if using mysql.server startup script)
  -l           don't subtract lock time from total time
```

比如:

得到返回记录集最多的10个SQL。

```
mysqldumpslow -s r -t 10 /database/mysql/mysql06_slow.log
```

得到访问次数最多的10个SQL

```
mysqldumpslow -s c -t 10 /database/mysql/mysql06_slow.log
```

得到按照时间排序的前10条里面含有左连接的查询语句。

```
mysqldumpslow -s t -t 10 -g “left join” /database/mysql/mysql06_slow.log
```

另外建议在使用这些命令时结合 | 和more 使用 ，否则有可能出现刷屏的情况。

```
mysqldumpslow -s r -t 20 /mysqldata/mysql/mysql06-slow.log | more
```




### 问题

#### 1. 为什么在慢查询日志里面出现Query_time小于long_query_time阀值的SQL语句呢？
例如，如下截图，`long_query_time=5`， 但是`Query_time`小于1秒的SQL都记录到慢查询日志当中了。
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307104901.png)
相信有些人遇到这个问题的时候觉得很奇怪，其实这个不是bug，而是你设置了系统变量`log_queries_not_using_indexes` ,这个系统变量开启后，会将那些未使用索引的SQL也被记录到慢查询日志中，另外，`full index scan`的SQL也会被记录到慢查询日志。所以，当满足这些条件的SQL，即使`Query_time`时间小于`long_query_time`的值，也会被记录到慢查询日志。

#### 2. 使用日志分析工具mysqldumpslow分析有些日志非常慢，如何加快？
```
mysqldumpslow -s t -t 10 /var/lib/mysql/MyDB-slow.log
```

1. 出现这种情况是因为慢查询日志变得很大（个人遇到的案例，慢查询日志就有2G多了），所以，需要每天或每周切分慢查询日志。设置一个Crontab作业即可。
/var/lib/mysql/DB-Server-slow.log.20181112
/var/lib/mysql/DB-Server-slow.log.20181113
/var/lib/mysql/DB-Server-slow.log.20181114
/var/lib/mysql/DB-Server-slow.log.20181115

2. 开启了系统变量`log_queries_not_using_indexes`后，如果系统设计糟糕，未使用索引的SQL很多，那么这一类的日志可能会有很多，所以还有个特别的开关`log_throttle_queries_not_using_indexes`用于限制每分钟输出未使用索引的日志数量。

#### 3. mysqldumpslow的生成报告中的Count、 Time、 Lock、Rows代表具体意思。
`mysqldumpslow -s c -t 10 /var/lib/mysql/MyDB-slow.log` 使用mysqldumpslow分析慢查询日志分析获取访问次数最多的10个SQL。
Count:表示这个SQL总共执行了195674次（慢查询日志中出现的次数）
Time:表示执行时间，后面括号里面的38s 表示这个SQL语句累计的执行耗费时间为38秒。其实就是单次执行的时间和总共执行消耗的时间的区别。
Lock:表示锁定时间，后面括号里面表示这些SQL累计的锁定时间为48s
Rows:表示返回的记录数，括号里面表示所有SQL语句累计返回记录数
![Image [3] - 副本](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307105002.png)
然后我们看看慢查询日志的相关信息：
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307105103.png)

```
# Time: 2018-11-15T01:43:51.338167Z  
```
这个表示日志记录的时间，确切的说是SQL执行完的时间点。注意这个时间有可能跟系统当前时间不一致，它可能是UTC时间。这个要看系统变量log_timestamps是UTC还是system。 
```
mysql> show variables like 'log_timestamps';
+----------------+-------+
| Variable_name  | Value |
+----------------+-------+
| log_timestamps | UTC   |
+----------------+-------+
1 row in set (0.01 sec)
 
mysql> set global log_timestamps=system;
Query OK, 0 rows affected (0.00 sec)
```
```
# User@Host: xxx[xxx] @  [xxx.xxx.xxx.xxx]  Id: 23781
客户端的账户信息，两个用户名（第一个是授权账户，第二个为登录账户），客户端IP地址，还有mysqld的线程ID。

# Query_time: 16.480118  Lock_time: 0.000239 Rows_sent: 1  Rows_examined: 348011
查询执行的信息，包括查询时长，锁持有时长，返回客户端的行数，优化器扫描行数。通常需要优化的就是最后一个内容，尽量减少SQL语句扫描的数据行数

#use xxx;

#SET timestamp=1542246231;
这个是时间戳，你可以将其转换为时间格式（注意时区），如下所示：

[root@mylnx02 ~]# date -d @1542246231
Thu Nov 15 09:43:51 CST 2018

[root@DB-Server ~]# date -d @1542246231
Wed Nov 14 20:43:51 EST 2018
```
#### 4. 如何分析慢查询日志一段时间内的数据呢？
mysqldumpslow这款工具没有提供相关参数分析某个日期范围内的慢查询日志，也就是说没法提供精细的搜索、分析。如果要分析某段时间内的慢查询日志可以使用工具pt-query-digest 

如果实在需要使用mysqldumpslow分析某段时间内的慢查询SQL，可以借助awk命令的帮助。如下样例所示
```
#取出一天时间的慢查询日志

# awk '/# Time: 2018-11-14/,/# Time: 2018-11-15/' DB-Server-slow.log > DB-Server-slow.log.20181114

#取出2018-11-14号4点到6点之间两个小时的数据

#awk '/# Time: 2018-11-14T04/,/# Time: 2018-11-14T06/' DB-Server-slow.log > slow_04_06.log
```
#### 5. 关于慢查询日志中query_time和lock_time的关系。
只有当一个SQL的执行时间（不包括锁等待的时间 lock_time）>long_query_time的时候，才会判定为慢查询SQL；但是判定为慢查询SQL之后，输出的Query_time包括了（执行时间+锁等待时间），并且也会输出Lock_time时间。当一个SQL的执行时间（排除lock_time）小于long_query_time的时候（即使他锁等待超过了很久），也不会记录到慢查询日志当中的。
#### 6. mysqldumpslow相关参数的详细信息
```
#  mysqldumpslow --help
Usage: mysqldumpslow [ OPTS... ] [ LOGS... ]
 
Parse and summarize the MySQL slow query log. Options are
 
  --verbose    verbose  #显示详细信息
  --debug      debug    #调试模式下运行。
  --help       write this text to standard output
 
  -v           verbose #显示详细信息
  -d           debug   #调试模式下运行。
  -s ORDER     what to sort by (al, at, ar, c, l, r, t), 'at' is default  排序方式，at是默认方式
                al: average lock time  #平均锁定时间排序
                ar: average rows sent  #平均发送行数排序
 
                at: average query time #平均查询时间排序
 
                 c: count              #执行次数排序
 
                 l: lock time          #锁定时间排序
 
                 r: rows sent          #总结果行数排序
 
                 t: query time         #总查询时间排序
  -r           reverse the sort order (largest last instead of first) 
               #倒序信息排序
  -t NUM       just show the top n queries  
               #只显示前n个查询
  -a           don't abstract all numbers to N and strings to 'S'
  -n NUM       abstract numbers with at least n digits within names
  -g PATTERN   grep: only consider stmts that include this string
               #根据字符串筛选慢查询日志
  -h HOSTNAME  hostname of db server for *-slow.log filename (can be wildcard),
               default is '*', i.e. match all
               #根据服务器名称选择慢查询日志
  -i NAME      name of server instance (if using mysql.server startup script)
               #根据服务器MySQL实例名称选择慢查询日志。
  -l           don't subtract lock time from total time  
               #不要从总时间减去锁定时间
```
#### 7. 系统变量Slow_queries会统计慢查询出现的次数。
```
mysql>  show global status like '%slow%'; 
+---------------------+--------+
| Variable_name       | Value  |
+---------------------+--------+
| Slow_launch_threads | 0      |
| Slow_queries        | 120    |
+---------------------+--------+
```
#### 8. 系统变量slow_launch_time 是什么？ 跟慢查询日志有关系吗？
如果创建线程需要的时间比slow_launch_time多，服务器会增加Slow_launch_threads的状态变量的数量。其实这个状态变量跟慢查询没有什么关系。之所以放到这里，是有人问过这个问题！