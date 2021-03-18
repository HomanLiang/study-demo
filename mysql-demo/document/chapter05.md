[toc]

# MySQL 日志

日志是 `mysql` 数据库的重要组成部分，记录着数据库运行期间各种状态信息。`mysql`日志主要包括错误日志、查询日志、慢查询日志、事务日志、二进制日志几大类。



## Error Log

记录Mysql运行过程中的Error、Warning、Note等信息，系统出错或者某条记录出问题可以查看Error日志。

- Mysql的错误日志默认以hostname.err存放在Mysql的日志目录，可以通过以下语句查看：

  ```ruby
mysql> show variables like "log_error";
  +---------------+----------------+
  | Variable_name | Value          |
  +---------------+----------------+
  | log_error     | /tmp/mysql.log |
  +---------------+---------------
  ```
  
- 修改错误日志的地址可以在/etc/my.cnf中添加--log-error = [filename]来开启mysql错误日志。我的是：

  ```cpp
 log_error = /tmp/mysql.log
  ```
  
- 先来查看一下：tail -f /tmp/mysql.log

  ```dart
bash-3.2# tail -f /tmp/mysql.log 
  2015-12-23T02:22:41.467311Z 0 [Note] IPv6 is available.
  2015-12-23T02:22:41.467324Z 0 [Note]   - '::' resolves to '::';
  2015-12-23T02:22:41.467350Z 0 [Note] Server socket created on IP: '::'.
  2015-12-23T02:22:41.584287Z 0 [Note] Event Scheduler: Loaded 0 events
  2015-12-23T02:22:41.584390Z 0 [Note] /usr/local/Cellar/mysql/5.7.9/bin/mysqld: ready for connections.
  Version: '5.7.9'  socket: '/tmp/mysql.sock'  port: 3306  Homebrew
  2015-12-23T02:22:42.540786Z 0 [Note] InnoDB: Buffer pool(s) load completed at 151223 10:22:42
  151223 10:22:51 mysqld_safe A mysqld process already exists
  2015-12-23T02:25:30.984395Z 2 [ERROR] Could not use /tmp/mysql_query.log for logging (error 13 - Permission denied). Turning logging off for the server process. To turn it on again: fix the cause, then either restart the query logging by using "SET GLOBAL GENERAL_LOG=ON" or restart the MySQL server.
  2015-12-23T07:28:03.923562Z 0 [Note] InnoDB: page_cleaner: 1000ms intended loop took 61473ms. The settings might not be optimal. (flushed=0 and evicted=0, during the time.)
  ```

信息量比较大，暂不分析了。。。。当然 如果mysql配置或连接出错时， 仍然可以通过tail -f 来跟踪日志的



## General Query Log

记录mysql的日常日志，包括查询、修改、更新等的每条sql。

- 先查看mysql是否启用了查询日志: **show global variables like "%genera%"**

  ```ruby
mysql> show global variables like "%genera%";
  +----------------------------------------+----------------------+
  | Variable_name                          | Value                |
  +----------------------------------------+----------------------+
  | auto_generate_certs                    | ON                   |
  | general_log                            | OFF                  |
  | general_log_file                       | /tmp/mysql_query.log |
  | sha256_password_auto_generate_rsa_keys | ON                   |
  +----------------------------------------+----------------------+
  4 rows in set (0.00 sec)
  ```
  
  我这里是配置了日志输出文件：/tmp/mysql_query.log，并且日志功能关闭

- 查询日志的输出文件可以在**/etc/my.cnf** 中添加**general-log-file = [filename]**

- Mysql打开general log日志后，所有的查询语句都可以在general log文件中输出，如果打开，文件会非常大，建议调试的时候打开，平时关闭

  ```csharp
mysql> set global general_log = on;
  Query OK, 0 rows affected (0.01 sec)
  
  mysql> set global general_log = off;
  Query OK, 0 rows affected (0.01 sec)
  ```
  
- **注意：**

  如果打开了日志功能，但是没有写入日志，那就有可能是mysql对日志文件的权限不够，所以需要指定权限，我的日志文件是 /tmp/mysql_query.log , 则：

  ```cpp
chown mysql:mysql /tmp/mysql_query.log
  ```



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



## binlog

`binlog` 用于记录数据库执行的写入性操作(不包括查询)信息，以二进制的形式保存在磁盘中。`binlog` 是 `mysql`的逻辑日志，并且由 `Server` 层进行记录，使用任何存储引擎的 `mysql` 数据库都会记录 `binlog` 日志。

- **逻辑日志**：可以简单理解为记录的就是sql语句 。
- **物理日志**：`mysql` 数据最终是保存在数据页中的，物理日志记录的就是数据页变更 。

`binlog` 是通过追加的方式进行写入的，可以通过`max_binlog_size` 参数设置每个 `binlog`文件的大小，当文件大小达到给定值之后，会生成新的文件来保存日志。



**Binlog 包括两类文件：**

- 二进制日志索引文件(.index)：记录所有的二进制文件。
- 二进制日志文件(.00000*)：记录所有 DDL 和 DML 语句事件。



### 启用binlog功能和查看binlog

Binlog 日志功能默认是开启的，线上情况下 Binlog 日志的增长速度是很快的，在 MySQL 的配置文件 `my.cnf` 中提供一些参数来对 Binlog 进行设置。

```mysql
Copy设置此参数表示启用binlog功能，并制定二进制日志的存储目录
log-bin=/home/mysql/binlog/

#mysql-bin.*日志文件最大字节（单位：字节）
#设置最大100MB
max_binlog_size=104857600

#设置了只保留7天BINLOG（单位：天）
expire_logs_days = 7

#binlog日志只记录指定库的更新
#binlog-do-db=db_name

#binlog日志不记录指定库的更新
#binlog-ignore-db=db_name

#写缓冲多少次，刷一次磁盘，默认0
sync_binlog=0
```

需要注意的是：

**max_binlog_size** ：Binlog 最大和默认值是 1G，该设置并不能严格控制 Binlog 的大小，尤其是 Binlog 比较靠近最大值而又遇到一个比较大事务时，为了保证事务的完整性不可能做切换日志的动作，只能将该事务的所有 SQL 都记录进当前日志直到事务结束。所以真实文件有时候会大于 max_binlog_size 设定值。
**expire_logs_days** ：Binlog 过期删除不是服务定时执行，是需要借助事件触发才执行，事件包括：

- 服务器重启
- 服务器被更新
- 日志达到了最大日志长度 `max_binlog_size`
- 日志被刷新

二进制日志由配置文件的 `log-bin` 选项负责启用，MySQL 服务器将在数据根目录创建两个新文件`mysql-bin.000001` 和 `mysql-bin.index`，若配置选项没有给出文件名，MySQL 将使用主机名称命名这两个文件，其中 `.index` 文件包含一份全体日志文件的清单。

**sync_binlog**：这个参数决定了 Binlog 日志的更新频率。默认 0 ，表示该操作由操作系统根据自身负载自行决定多久写一次磁盘。

sync_binlog = 1 表示每一条事务提交都会立刻写盘。sync_binlog=n 表示 n 个事务提交才会写盘。

根据 MySQL 文档，写 Binlog 的时机是：SQL transaction 执行完，但任何相关的 Locks 还未释放或事务还未最终 commit 前。这样保证了 Binlog 记录的操作时序与数据库实际的数据变更顺序一致。

检查 Binlog 文件是否已开启：

```mysql
Copymysql> show variables like '%log_bin%';
+---------------------------------+------------------------------------+
| Variable_name                   | Value                              |
+---------------------------------+------------------------------------+
| log_bin                         | ON                                 |
| log_bin_basename                | /usr/local/mysql/data/binlog       |
| log_bin_index                   | /usr/local/mysql/data/binlog.index |
| log_bin_trust_function_creators | OFF                                |
| log_bin_use_v1_row_events       | OFF                                |
| sql_log_bin                     | ON                                 |
+---------------------------------+------------------------------------+
6 rows in set (0.00 sec)
```

Binlog 文件是二进制文件，强行打开看到的必然是乱码，MySQL 提供了命令行的方式来展示 Binlog 日志：

```shell
Copymysqlbinlog mysql-bin.000002 | more
```

`mysqlbinlog` 命令即可查看。

![image-20210308222009403](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308222009403.png)

看起来凌乱其实也有迹可循。Binlog 通过事件的方式来管理日志信息，可以通过 `show binlog events in` 的语法来查看当前 Binlog 文件对应的详细事件信息。

```mysql
Copymysql> show binlog events in 'mysql-bin.000001';
+------------------+-----+----------------+-----------+-------------+-----------------------------------+
| Log_name         | Pos | Event_type     | Server_id | End_log_pos | Info                              |
+------------------+-----+----------------+-----------+-------------+-----------------------------------+
| mysql-bin.000001 |   4 | Format_desc    |         1 |         125 | Server ver: 8.0.21, Binlog ver: 4 |
| mysql-bin.000001 | 125 | Previous_gtids |         1 |         156 |                                   |
| mysql-bin.000001 | 156 | Stop           |         1 |         179 |                                   |
+------------------+-----+----------------+-----------+-------------+-----------------------------------+
3 rows in set (0.01 sec)
```

这是一份没有任何写入数据的 Binlog 日志文件。

Binlog 的版本是V4，可以看到日志的结束时间为 Stop。出现 Stop event 有两种情况：

1. 是 master shut down 的时候会在 Binlog 文件结尾出现
2. 是备机在关闭的时候会写入 relay log 结尾，或者执行 RESET SLAVE 命令执行

本文出现的原因是我有手动停止过 MySQL 服务。

一般来说一份正常的 Binlog 日志文件会以 **Rotate event** 结束。当 Binlog 文件超过指定大小，Rotate event 会写在文件最后，指向下一个 Binlog 文件。

我们来看看有过数据操作的 Binlog 日志文件是什么样子的。

```mysql
Copymysql> show binlog events in 'mysql-bin.000002';
+------------------+-----+----------------+-----------+-------------+-----------------------------------+
| Log_name         | Pos | Event_type     | Server_id | End_log_pos | Info                              |
+------------------+-----+----------------+-----------+-------------+-----------------------------------+
| mysql-bin.000002 |   4 | Format_desc    |         1 |         125 | Server ver: 8.0.21, Binlog ver: 4 |
| mysql-bin.000002 | 125 | Previous_gtids |         1 |         156 |                                   |
+------------------+-----+----------------+-----------+-------------+-----------------------------------+
2 rows in set (0.00 sec)
```

上面是没有任何数据操作且没有被截断的 Binlog。接下来我们插入一条数据，再看看 Binlog 事件。

```mysql
Copymysql> show binlog events in 'mysql-bin.000002';
+------------------+-----+----------------+-----------+-------------+-------------------------------------------------------------------------+
| Log_name         | Pos | Event_type     | Server_id | End_log_pos | Info                                                                    |
+------------------+-----+----------------+-----------+-------------+-------------------------------------------------------------------------+
| mysql-bin.000002 |   4 | Format_desc    |         1 |         125 | Server ver: 8.0.21, Binlog ver: 4                                       |
| mysql-bin.000002 | 125 | Previous_gtids |         1 |         156 |                                                                         |
| mysql-bin.000002 | 156 | Anonymous_Gtid |         1 |         235 | SET @@SESSION.GTID_NEXT= 'ANONYMOUS'                                    |
| mysql-bin.000002 | 235 | Query          |         1 |         323 | BEGIN                                                                   |
| mysql-bin.000002 | 323 | Intvar         |         1 |         355 | INSERT_ID=13                                                            |
| mysql-bin.000002 | 355 | Query          |         1 |         494 | use `test_db`; INSERT INTO `test_db`.`test_db`(`name`) VALUES ('xdfdf') |
| mysql-bin.000002 | 494 | Xid            |         1 |         525 | COMMIT /* xid=192 */                                                    |
+------------------+-----+----------------+-----------+-------------+-------------------------------------------------------------------------+
7 rows in set (0.00 sec)
```

这是加入一条数据之后的 Binlog 事件。

我们对 event 查询的数据行关键字段来解释一下：

- **Pos**：当前事件的开始位置，每个事件都占用固定的字节大小，结束位置(End_log_position)减去Pos，就是这个事件占用的字节数。

  上面的日志中我们能看到，第一个事件位置并不是从 0 开始，而是从 4。MySQL 通过文件中的前 4 个字节，来判断这是不是一个 Binlog 文件。这种方式很常见，很多格式的文件，如 pdf、doc、jpg等，都会通常前几个特定字符判断是否是合法文件。

- **Event_type**：表示事件的类型

- **Server_id**：表示产生这个事件的 MySQL server_id，通过设置 my.cnf 中的 **server-id** 选项进行配置

- **End_log_position**：下一个事件的开始位置

- **Info**：包含事件的具体信息



### binlog使用场景

在实际应用中， `binlog` 的主要使用场景有两个，分别是 **主从复制** 和 **数据恢复** 。

1. **主从复制** ：在 `Master` 端开启 `binlog` ，然后将 `binlog`发送到各个 `Slave` 端， `Slave` 端重放 `binlog` 从而达到主从数据一致。
2. **数据恢复** ：通过使用 `mysqlbinlog` 工具来恢复数据。

### binlog刷盘时机

对于 `InnoDB` 存储引擎而言，只有在事务提交时才会记录`biglog` ，此时记录还在内存中，那么 `biglog`是什么时候刷到磁盘中的呢？

`mysql` 通过 `sync_binlog` 参数控制 `biglog` 的刷盘时机，取值范围是 `0-N`：

- 0：不去强制要求，由系统自行判断何时写入磁盘；
- 1：每次 `commit` 的时候都要将 `binlog` 写入磁盘；
- N：每N个事务，才会将 `binlog` 写入磁盘。

从上面可以看出， `sync_binlog` 最安全的是设置是 `1` ，这也是`MySQL 5.7.7`之后版本的默认值。但是设置一个大一些的值可以提升数据库性能，因此实际情况下也可以将值适当调大，牺牲一定的一致性来获取更好的性能。

### binlog日志格式

`binlog` 日志有三种格式，分别为 `STATMENT` 、 `ROW` 和 `MIXED`。

> 在 `MySQL 5.7.7` 之前，默认的格式是 `STATEMENT` ， `MySQL 5.7.7` 之后，默认值是 `ROW`。日志格式通过 `binlog-format` 指定。

- `STATMENT`：基于`SQL` 语句的复制( `statement-based replication, SBR` )，每一条会修改数据的sql语句会记录到`binlog` 中  。

	- 优点：不需要记录每一行的变化，减少了 binlog 日志量，节约了 IO  , 从而提高了性能；
	- 缺点：在某些情况下会导致主从数据不一致，比如执行sysdate() 、  slepp()  等 。

- `ROW`：基于行的复制(`row-based replication, RBR` )，不记录每条sql语句的上下文信息，仅需记录哪条数据被修改了 。

	- 优点：不会出现某些特定情况下的存储过程、或function、或trigger的调用和触发无法被正确复制的问题 ；
	- 缺点：会产生大量的日志，尤其是` alter table ` 的时候会让日志暴涨

- `MIXED`：基于`STATMENT` 和 `ROW` 两种模式的混合复制(`mixed-based replication, MBR` )，一般的复制使用`STATEMENT` 模式保存 `binlog` ，对于 `STATEMENT` 模式无法复制的操作使用 `ROW` 模式保存 `binlog`

  ![image-20210308221010353](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308221010353.png)

### 如何通过 `mysqlbinlog` 命令手动恢复数据

上面说过每一条 event 都有位点信息，如果我们当前的 MySQL 库被无操作或者误删除了，那么该如何通过 Binlog 来恢复到删除之前的数据状态呢？

首先发现误操作之后，先停止 MySQL 服务，防止继续更新。

接着通过 `mysqlbinlog`命令对二进制文件进行分析，查看误操作之前的位点信息在哪里。

接下来肯定就是恢复数据，当前数据库的数据已经是错的，那么就从开始位置到误操作之前位点的数据肯定的都是正确的；如果误操作之后也有正常的数据进来，这一段时间的位点数据也要备份。

比如说：

误操作的位点开始值为 501，误操作结束的位置为705，之后到800的位点都是正确数据。

那么从 0 - 500 ，706 - 800 都是有效数据，接着我们就可以进行数据恢复了。

先将数据库备份并清空。

接着使用 `mysqlbinlog` 来恢复数据：

0 - 500 的数据：

```mysql
Copymysqlbinlog --start-position=0  --stop-position=500  bin-log.000003 > /root/back.sql;
```

上面命令的作用就是将 0 -500 位点的数据恢复到自定义的 SQL 文件中。同理 706 - 800 的数据也是一样操作。之后我们执行这两个 SQL 文件就行了。

### Binlog 事件类型

上面我们说到了 Binlog 日志中的事件，不同的操作会对应着不同的事件类型，且不同的 Binlog 日志模式同一个操作的事件类型也不同，下面我们一起看看常见的事件类型。

首先我们看看源码中的事件类型定义：

源码位置：/libbinlogevents/include/binlog_event.h

```c++
Copyenum Log_event_type
{
  /**
    Every time you update this enum (when you add a type), you have to
    fix Format_description_event::Format_description_event().
  */
  UNKNOWN_EVENT= 0,
  START_EVENT_V3= 1,
  QUERY_EVENT= 2,
  STOP_EVENT= 3,
  ROTATE_EVENT= 4,
  INTVAR_EVENT= 5,
  LOAD_EVENT= 6,
  SLAVE_EVENT= 7,
  CREATE_FILE_EVENT= 8,
  APPEND_BLOCK_EVENT= 9,
  EXEC_LOAD_EVENT= 10,
  DELETE_FILE_EVENT= 11,
  /**
    NEW_LOAD_EVENT is like LOAD_EVENT except that it has a longer
    sql_ex, allowing multibyte TERMINATED BY etc; both types share the
    same class (Load_event)
  */
  NEW_LOAD_EVENT= 12,
  RAND_EVENT= 13,
  USER_VAR_EVENT= 14,
  FORMAT_DESCRIPTION_EVENT= 15,
  XID_EVENT= 16,
  BEGIN_LOAD_QUERY_EVENT= 17,
  EXECUTE_LOAD_QUERY_EVENT= 18,

  TABLE_MAP_EVENT = 19,

  /**
    The PRE_GA event numbers were used for 5.1.0 to 5.1.15 and are
    therefore obsolete.
   */
  PRE_GA_WRITE_ROWS_EVENT = 20,
  PRE_GA_UPDATE_ROWS_EVENT = 21,
  PRE_GA_DELETE_ROWS_EVENT = 22,

  /**
    The V1 event numbers are used from 5.1.16 until mysql-trunk-xx
  */
  WRITE_ROWS_EVENT_V1 = 23,
  UPDATE_ROWS_EVENT_V1 = 24,
  DELETE_ROWS_EVENT_V1 = 25,

  /**
    Something out of the ordinary happened on the master
   */
  INCIDENT_EVENT= 26,

  /**
    Heartbeat event to be send by master at its idle time
    to ensure master's online status to slave
  */
  HEARTBEAT_LOG_EVENT= 27,

  /**
    In some situations, it is necessary to send over ignorable
    data to the slave: data that a slave can handle in case there
    is code for handling it, but which can be ignored if it is not
    recognized.
  */
  IGNORABLE_LOG_EVENT= 28,
  ROWS_QUERY_LOG_EVENT= 29,

  /** Version 2 of the Row events */
  WRITE_ROWS_EVENT = 30,
  UPDATE_ROWS_EVENT = 31,
  DELETE_ROWS_EVENT = 32,

  GTID_LOG_EVENT= 33,
  ANONYMOUS_GTID_LOG_EVENT= 34,

  PREVIOUS_GTIDS_LOG_EVENT= 35,

  TRANSACTION_CONTEXT_EVENT= 36,

  VIEW_CHANGE_EVENT= 37,

  /* Prepared XA transaction terminal event similar to Xid */
  XA_PREPARE_LOG_EVENT= 38,
  /**
    Add new events here - right above this comment!
    Existing events (except ENUM_END_EVENT) should never change their numbers
  */
  ENUM_END_EVENT /* end marker */
};
```

这么多的事件类型我们就不一一介绍，挑出来一些常用的来看看。

**FORMAT_DESCRIPTION_EVENT**

FORMAT_DESCRIPTION_EVENT 是 Binlog V4 中为了取代之前版本中的 START_EVENT_V3 事件而引入的。它是 Binlog 文件中的第一个事件，而且，该事件只会在 Binlog 中出现一次。MySQL 根据 FORMAT_DESCRIPTION_EVENT 的定义来解析其它事件。

它通常指定了 MySQL 的版本，Binlog 的版本，该 Binlog 文件的创建时间。

**QUERY_EVENT**

QUERY_EVENT 类型的事件通常在以下几种情况下使用：

- 事务开始时，执行的 BEGIN 操作
- STATEMENT 格式中的 DML 操作
- ROW 格式中的 DDL 操作

比如上文我们插入一条数据之后的 Binlog 日志：

```mysql
Copymysql> show binlog events in 'mysql-bin.000002';
+------------------+-----+----------------+-----------+-------------+-------------------------------------------------------------------------+
| Log_name         | Pos | Event_type     | Server_id | End_log_pos | Info                                                                    |
+------------------+-----+----------------+-----------+-------------+-------------------------------------------------------------------------+
| mysql-bin.000002 |   4 | Format_desc    |         1 |         125 | Server ver: 8.0.21, Binlog ver: 4                                       |
| mysql-bin.000002 | 125 | Previous_gtids |         1 |         156 |                                                                         |
| mysql-bin.000002 | 156 | Anonymous_Gtid |         1 |         235 | SET @@SESSION.GTID_NEXT= 'ANONYMOUS'                                    |
| mysql-bin.000002 | 235 | Query          |         1 |         323 | BEGIN                                                                   |
| mysql-bin.000002 | 323 | Intvar         |         1 |         355 | INSERT_ID=13                                                            |
| mysql-bin.000002 | 355 | Query          |         1 |         494 | use `test_db`; INSERT INTO `test_db`.`test_db`(`name`) VALUES ('xdfdf') |
| mysql-bin.000002 | 494 | Xid            |         1 |         525 | COMMIT /* xid=192 */                                                    |
+------------------+-----+----------------+-----------+-------------+-------------------------------------------------------------------------+
7 rows in set (0.00 sec)
```

**XID_EVENT**

在事务提交时，不管是 STATEMENT 还 是ROW 格式的 Binlog，都会在末尾添加一个 XID_EVENT 事件代表事务的结束。该事件记录了该事务的 ID，在 MySQL 进行崩溃恢复时，根据事务在 Binlog 中的提交情况来决定是否提交存储引擎中状态为 prepared 的事务。

**ROWS_EVENT**

对于 ROW 格式的 Binlog，所有的 DML 语句都是记录在 ROWS_EVENT 中。

ROWS_EVENT分为三种：

- WRITE_ROWS_EVENT
- UPDATE_ROWS_EVENT
- DELETE_ROWS_EVENT

分别对应 insert，update 和 delete 操作。

对于 insert 操作，WRITE_ROWS_EVENT 包含了要插入的数据。

对于 update 操作，UPDATE_ROWS_EVENT 不仅包含了修改后的数据，还包含了修改前的值。

对于 delete 操作，仅仅需要指定删除的主键（在没有主键的情况下，会给定所有列）。

**对比 QUERY_EVENT 事件，是以文本形式记录 DML 操作的。而对于 ROWS_EVENT 事件，并不是文本形式，所以在通过 mysqlbinlog 查看基于 ROW 格式的 Binlog 时，需要指定 `-vv --base64-output=decode-rows`。**

我们来测试一下，首先将日志格式改为 Rows：

```mysql
Copymysql> set binlog_format=row;
Query OK, 0 rows affected (0.00 sec)

mysql>
mysql> flush logs;
Query OK, 0 rows affected (0.01 sec)
```

然后刷新一下日志文件，重新开始一个 Binlog 日志。我们插入一条数据之后看一下日志：

```mysql
Copymysql> show binlog events in 'binlog.000008';
+---------------+-----+----------------+-----------+-------------+--------------------------------------+
| Log_name      | Pos | Event_type     | Server_id | End_log_pos | Info                                 |
+---------------+-----+----------------+-----------+-------------+--------------------------------------+
| binlog.000008 |   4 | Format_desc    |         1 |         125 | Server ver: 8.0.21, Binlog ver: 4    |
| binlog.000008 | 125 | Previous_gtids |         1 |         156 |                                      |
| binlog.000008 | 156 | Anonymous_Gtid |         1 |         235 | SET @@SESSION.GTID_NEXT= 'ANONYMOUS' |
| binlog.000008 | 235 | Query          |         1 |         313 | BEGIN                                |
| binlog.000008 | 313 | Table_map      |         1 |         377 | table_id: 85 (test_db.test_db)       |
| binlog.000008 | 377 | Write_rows     |         1 |         423 | table_id: 85 flags: STMT_END_F       |
| binlog.000008 | 423 | Xid            |         1 |         454 | COMMIT /* xid=44 */                  |
+---------------+-----+----------------+-----------+-------------+--------------------------------------+
7 rows in set (0.01 sec)
```



## redo log

### 为什么需要redo log

我们都知道，事务的四大特性里面有一个是 **持久性** ，具体来说就是**只要事务提交成功，那么对数据库做的修改就被永久保存下来了，不可能因为任何原因再回到原来的状态** 。

那么 `mysql`是如何保证一致性的呢？

最简单的做法是在每次事务提交的时候，将该事务涉及修改的数据页全部刷新到磁盘中。但是这么做会有严重的性能问题，主要体现在两个方面：

1. 因为 `Innodb` 是以 `页` 为单位进行磁盘交互的，而一个事务很可能只修改一个数据页里面的几个字节，这个时候将完整的数据页刷到磁盘的话，太浪费资源了！
2. 一个事务可能涉及修改多个数据页，并且这些数据页在物理上并不连续，使用随机IO写入性能太差！

因此 `mysql` 设计了 `redo log` ， **具体来说就是只记录事务对数据页做了哪些修改**，这样就能完美地解决性能问题了(相对而言文件更小并且是顺序IO)。

### redo log基本概念

`redo log` 包括两部分：一个是内存中的日志缓冲( `redo log buffer` )，另一个是磁盘上的日志文件( `redo logfile`)。

`mysql` 每执行一条 `DML` 语句，先将记录写入 `redo log buffer`，后续某个时间点再一次性将多个操作记录写到 `redo log file`。这种 **先写日志，再写磁盘** 的技术就是 `MySQL`
里经常说到的 `WAL(Write-Ahead Logging)` 技术。

在计算机操作系统中，用户空间( `user space` )下的缓冲区数据一般情况下是无法直接写入磁盘的，中间必须经过操作系统内核空间( `kernel space` )缓冲区( `OS Buffer` )。

因此， `redo log buffer` 写入 `redo logfile` 实际上是先写入 `OS Buffer` ，然后再通过系统调用 `fsync()` 将其刷到 `redo log file`
中，过程如下：

![image-20210308220105394](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308220105394.png)

`mysql` 支持三种将 `redo log buffer` 写入 `redo log file` 的时机，可以通过 `innodb_flush_log_at_trx_commit` 参数配置，各参数值含义如下：

![image-20210308220142598](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308220142598.png)

![image-20210308220212824](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308220212824.png)



### redo log记录形式

前面说过， `redo log` 实际上记录数据页的变更，而这种变更记录是没必要全部保存，因此 `redo log`实现上采用了大小固定，循环写入的方式，当写到结尾时，会回到开头循环写日志。如下图：

![image-20210308220256532](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308220256532.png)

同时我们很容易得知， 在innodb中，既有`redo log` 需要刷盘，还有 `数据页` 也需要刷盘， `redo log`存在的意义主要就是降低对 `数据页` 刷盘的需求 。

在上图中， `write pos` 表示 `redo log` 当前记录的 `LSN` (逻辑序列号)位置， `check point` 表示 **数据页更改记录** 刷盘后对应 `redo log` 所处的 `LSN`(逻辑序列号)位置。

`write pos` 到 `check point` 之间的部分是 `redo log` 空着的部分，用于记录新的记录；`check point` 到 `write pos` 之间是 `redo log` 待落盘的数据页更改记录。当 `write pos`追上`check point` 时，会先推动 `check point` 向前移动，空出位置再记录新的日志。

启动 `innodb` 的时候，不管上次是正常关闭还是异常关闭，总是会进行恢复操作。因为 `redo log`记录的是数据页的物理变化，因此恢复的时候速度比逻辑日志(如 `binlog` )要快很多。

重启`innodb` 时，首先会检查磁盘中数据页的 `LSN` ，如果数据页的`LSN` 小于日志中的 `LSN` ，则会从 `checkpoint` 开始恢复。

还有一种情况，在宕机前正处于`checkpoint` 的刷盘过程，且数据页的刷盘进度超过了日志页的刷盘进度，此时会出现数据页中记录的 `LSN` 大于日志中的 `LSN`，这时超出日志进度的部分将不会重做，因为这本身就表示已经做过的事情，无需再重做。

### redo log与binlog区别

![image-20210308220401703](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308220401703.png)

由 `binlog` 和 `redo log` 的区别可知：`binlog` 日志只用于归档，只依靠 `binlog` 是没有 `crash-safe` 能力的。

但只有 `redo log` 也不行，因为 `redo log` 是 `InnoDB`特有的，且日志上的记录落盘后会被覆盖掉。因此需要 `binlog`和 `redo log`二者同时记录，才能保证当数据库发生宕机重启时，数据不会丢失。



## undo log

### 1、undo是啥

undo日志用于存放数据修改被修改前的值，假设修改 tba 表中 id=2的行数据，把Name='B' 修改为Name = 'B2' ，那么undo日志就会用来存放Name='B'的记录，如果这个修改出现异常，可以使用undo日志来实现回滚操作，保证事务的一致性。

对数据的变更操作，主要来自 INSERT UPDATE DELETE，而UNDO LOG中分为两种类型，一种是 INSERT_UNDO（INSERT操作），记录插入的唯一键值；一种是 UPDATE_UNDO（包含UPDATE及DELETE操作），记录修改的唯一键值以及old column记录。

| Id   | Name |
| ---- | ---- |
| 1    | A    |
| 2    | B    |
| 3    | C    |
| 4    | D    |

### 2、 undo参数

MySQL跟undo有关的参数设置有这些：

```
 1 mysql> show global variables like '%undo%';
 2 +--------------------------+------------+
 3 | Variable_name            | Value      |
 4 +--------------------------+------------+
 5 | innodb_max_undo_log_size | 1073741824 |
 6 | innodb_undo_directory    | ./         |
 7 | innodb_undo_log_truncate | OFF        |
 8 | innodb_undo_logs         | 128        |
 9 | innodb_undo_tablespaces  | 3          |
10 +--------------------------+------------+
11  
12 mysql> show global variables like '%truncate%';
13 +--------------------------------------+-------+
14 | Variable_name                        | Value |
15 +--------------------------------------+-------+
16 | innodb_purge_rseg_truncate_frequency | 128   |
17 | innodb_undo_log_truncate             | OFF   |
18 +--------------------------------------+-------+
```

- **innodb_max_undo_log_size**

  控制最大undo tablespace文件的大小，当启动了innodb_undo_log_truncate 时，undo tablespace 超过innodb_max_undo_log_size 阀值时才会去尝试truncate。该值默认大小为1G，truncate后的大小默认为10M。

- **innodb_undo_tablespaces** 

  设置undo独立表空间个数，范围为0-128， 默认为0，0表示表示不开启独立undo表空间 且 undo日志存储在ibdata文件中。该参数只能在最开始初始化MySQL实例的时候指定，如果实例已创建，这个参数是不能变动的，如果在数据库配置文 件 .cnf 中指定innodb_undo_tablespaces 的个数大于实例创建时的指定个数，则会启动失败，提示该参数设置有误。

  ![image-20210308224116476](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308224116476.png)

  如果设置了该参数为n（n>0），那么就会在undo目录下创建n个undo文件（undo001，undo002 ...... undo n），每个文件默认大小为10M.

  ![image-20210308224157869](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308224157869.png)
  
  **什么时候需要来设置这个参数呢？**
  
  当DB写压力较大时，可以设置独立UNDO表空间，把UNDO LOG从ibdata文件中分离开来，指定 innodb_undo_directory目录存放，可以制定到高速磁盘上，加快UNDO LOG 的读写性能。
  
- **innodb_undo_log_truncate**

  InnoDB的purge线程，根据innodb_undo_log_truncate设置开启或关闭、innodb_max_undo_log_size的参数值，以及truncate的频率来进行空间回收和 undo file 的重新初始化。

  **该参数生效的前提是，已设置独立表空间且独立表空间个数大于等于2个。**

  purge线程在truncate undo log file的过程中，需要检查该文件上是否还有活动事务，如果没有，需要把该undo log file标记为不可分配，这个时候，undo log 都会记录到其他文件上，所以至少需要2个独立表空间文件，才能进行truncate 操作，标注不可分配后，会创建一个独立的文件undo_<space_id>_trunc.log，记录现在正在truncate 某个undo log文件，然后开始初始化undo log file到10M，操作结束后，删除表示truncate动作的 undo_<space_id>_trunc.log 文件，这个文件保证了即使在truncate过程中发生了故障重启数据库服务，重启后，服务发现这个文件，也会继续完成truncate操作，删除文件结束后，标识该undo log file可分配。

- **innodb_purge_rseg_truncate_frequency**

  用于控制purge回滚段的频度，默认为128。假设设置为n，则说明，当Innodb Purge操作的协调线程 purge事务128次时，就会触发一次History purge，检查当前的undo log 表空间状态是否会触发truncate。

### 3、 undo空间管理

如果需要设置独立表空间，需要在初始化数据库实例的时候，指定独立表空间的数量。

UNDO内部由多个回滚段组成，即 Rollback segment，一共有128个，保存在ibdata系统表空间中，分别从resg slot0 - resg slot127，每一个resg slot，也就是每一个回滚段，内部由1024个undo segment 组成。

回滚段（rollback segment）分配如下：

- slot 0 ，预留给系统表空间；
- slot 1- 32，预留给临时表空间，每次数据库重启的时候，都会重建临时表空间；
- slot33-127，如果有独立表空间，则预留给UNDO独立表空间；如果没有，则预留给系统表空间；

回滚段中除去32个提供给临时表事务使用，剩下的 128-32=96个回滚段，可执行 96*1024 个并发事务操作，每个事务占用一个 undo segment slot，注意，如果事务中有临时表事务，还会在临时表空间中的 undo segment slot 再占用一个 undo segment slot，即占用2个undo segment slot。如果错误日志中有：`Cannot find a free slot for an undo log。`则说明并发的事务太多了，需要考虑下是否要分流业务。

回滚段（rollback segment ）采用 轮询调度的方式来分配使用，如果设置了独立表空间，那么就不会使用系统表空间回滚段中undo segment，而是使用独立表空间的，同时，如果回顾段正在 Truncate操作，则不分配。

![image-20210308223942673](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308223942673.png)









