[toc]

# MySQL 面试题 

## 数据库理论概念

### 一、数据库的三范式是什么？

- 第一范式：强调的是列的原子性，即数据库表的每一列都是不可分割的原子数据项。
- 第二范式：要求实体的属性完全依赖于主关键字。所谓完全依赖是指不能存在仅依赖主关键字一部分的属性。
- 第三范式：任何非主属性不依赖于其它非主属性。

### 二、普通查询、流式查询和游标查询的理解

**普通查询**

普通查询，将查询后的结果集，全部塞给客户端；

量大的话，就可能报OOM 内存溢出。

**流式查询**

流式查询获取数据的方法与普通查询其实是一样的（ this.io.nextRow），不同之处在与普通查询时先获取所有数据，然后交给应用处理（next方法其实都是从内存数组遍历），而流式查询时逐条获取，待应用处理完再去拿下一条数据。

我个人理解：

> 流式查询的结果集一直存放服务端，并且客户端要一直和服务端保持连接，等到客户端把这些结果集都消耗完了，才释放掉。

后来看到这篇文章，深入了解MySQL的流式查询机制时，算是肯定了想法，并且还了解到，当前的数据库连接还不能公用；

如果使用了流式查询，一个MySQL数据库连接同一时间只能为一个ResultSet对象服务，并且如果该ResultSet对象没有关闭，势必会影响其他查询对数据库连接的使用！

**游标查询**

这种方式就和我了解的Mongodb类似，一次查询指定fetchSize的数据，直到把数据全部处理完。

但是游标查询也有缺点：

> 应用指定每次查询获取的条数fetchSize，MySQL服务器每次只查询指定条数的数据，因此单次查询相比与前面两种方式占用MySQL时间较短。但由于MySQL方不知道客户端什么时候将数据消费完，MySQL需要建立一个临时空间来存放每次查询出的数据，大数据量时MySQL服务器IOPS、磁盘占用都会飙升，而且需要与服务器进行更多次的网络通讯，因此最终查询效率是不如流式查询的。

mongodb用的时内存映射的方式来查询，但是量大的话，其实也有上述的问题。所以在写mongodb代码时，游标记得一定要及时关闭。

**总结**

1. 普通查询

   优点：应用代码简单，数据量较小时操作速度快。

   缺点：数据量大时会出现OOM问题。

2. 流式查询

   优点：大数据量时不会有OOM问题。

   缺点：占用数据库时间更长，导致网络拥塞的可能性较大。

3. 游标查询

   优点：大数据量时不会有OOM问题，相比流式查询对数据库单次占用时间较短。

   缺点：相比流式查询，对服务端资源消耗更大，响应时间更长。

## Schema、表、字段、数据类型

### 一、char 和 varchar 的区别是什么？

char和varchar都是用来存储字符串的，但是他们保持和检索的方式不同。

char是属于固定长度的字符类型，而varchar是属于可变长度的字符类型。

由于char是固定长度的所以它的处理速度比varchar快很多。但是缺点是浪费存储空间，读取char类型数据时候时如果尾部有空格会丢失空格，所以对于那种长度变化不大的并且对查询速度有较高要求的数据可以考虑使用char类型来存储。

另外随着MySQL版本的不断升级，varchar数据类型的性能也在不断改进并提高，所以在许多的应用中，varchar类型被更多的使用

不同的存储引擎对char和varchar的使用原则有所不同：

- MyISAM存储引擎：建议使用固定长度的数据列代替可变长度的数据列。

- MEMORY存储引擎：目前都使用固定长度的数据行存储，因此无论使用CHAR或VARCHAR列都没有关系。两者都是作为CHAR类型处理。

- InnoDB存储引擎：建议使用VARCHAR类型。对于InnoDB数据表，内部的行存储格式没有区分固定长度和可变长度列（所有数据行都使用指向数据列值的头指针），因此在本质上，使用固定长度的CHAR列不一定比使用可变长度VARCHAR列性能要好。因而，主要的性能因素是数据行使用的存储总量。由于CHAR平均占用的空间多于VARCHAR，因此使用VARCHAR来最小化需要处理的数据行的存储总量和磁盘I/O是比较好的。

### 二、Mysql中的排序规则utf8_unicode_ci、utf8_general_ci的区别

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307135301.png)

`utf8_unicode_ci` 和 `utf8_general_ci` 对中、英文来说没有实质的差别。

`utf8_general_ci` 校对速度快，但准确度稍差。

`utf8_unicode_ci` 准确度高，但校对速度稍慢。

如果你的应用有德语、法语或者俄语，请一定使用 `utf8_unicode_ci`。一般用 `utf8_general_ci` 就够了。

附：

`ci` 是 `case insensitive`, 即 "大小写不敏感", a 和 A 会在字符判断中会被当做一样的；

`bin` 是二进制, a 和 A 会别区别对待。

例如你运行：`SELECT * FROM table WHERE txt = 'a'`

那么在 `utf8_bin` 中你就找不到 `txt = 'A'` 的那一行， 而 `utf8_general_ci` 则可以。

`utf8_general_ci` 不区分大小写，这个你在注册用户名和邮箱的时候就要使用。

`utf8_general_cs` 区分大小写，如果用户名和邮箱用这个 就会照成不良后果

`utf8_bin` 字符串每个字符串用二进制数据编译存储。 区分大小写，而且可以存二进制的内容

### 三、选择合适的 MySQL 日期时间类型来存储你的时间

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

### 四、一千个不用 Null 的理由！

1、NULL 为什么这么多人用？

NULL是创建数据表时默认的，初级或不知情的或怕麻烦的程序员不会注意这点。
很多人员都以为not null 需要更多空间，其实这不是重点。

重点是很多程序员觉得NULL在开发中不用去判断插入数据，写sql语句的时候更方便快捷。

**2、是不是以讹传讹？**

MySQL 官网文档：

> NULL columns require additional space in the rowto record whether their values are NULL. For MyISAM tables, each NULL columntakes one bit extra, rounded up to the nearest byte.

Mysql难以优化引用可空列查询，它会使索引、索引统计和值更加复杂。可空列需要更多的存储空间，还需要mysql内部进行特殊处理。可空列被索引后，每条记录都需要一个额外的字节，还能导致 `MYIsam` 中固定大小的索引变成可变大小的索引。

—— 出自《高性能mysql第二版》

照此分析，还真不是以讹传讹，这是有理论依据和出处的。

**3、给我一个不用 Null 的理由？**

1. 所有使用NULL值的情况，都可以通过一个有意义的值的表示，这样有利于代码的可读性和可维护性，并能从约束上增强业务数据的规范性。

   > NULL值到非NULL的更新无法做到原地更新，更容易发生索引分裂，从而影响性能。

2. 注意：但把NULL列改为NOT NULL带来的性能提示很小，除非确定它带来了问题，否则不要把它当成优先的优化措施，最重要的是使用的列的类型的适当性。

3. NULL值在 `timestamp` 类型下容易出问题，特别是没有启用参数 `explicit_defaults_for_timestamp`

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

   key_len 62 == 20*3（utf8 3字节） + 2 （存储 varchar 变长字符长度 2字节，定长字段无需额外的字节）

   key_len 83 == 20*4（utf8mb4 4字节） + 1 (是否为 Null 的标识) + 2 （存储 varchar 变长字符长度 2字节，定长字段无需额外的字节）

6. distinct 数据丢失

   当使用 `count(distinct col1, col2)` 查询时，如果其中一列为 `NULL`，那么即使另一列有不同的值，那么查询的结果也会将数据丢失

7. count 数据丢失
8. 导致空指针异常
9. 增加了查询难度

所以说索引字段最好不要为NULL，因为NULL会使索引、索引统计和值更加复杂，并且需要额外一个字节的存储空间。基于以上这些理由和原因，我想咱们不用 Null 的理由应该是够了

**阿里巴巴《Java开发手册》推荐我们使用 `ISNULL(cloumn)` 来判断 `NULL` 值**，原因是在 SQL 语句中，如果在 null 前换行，影响可读性；而 `ISNULL(column)` 是一个整体，简洁易懂。从性能数据上分析 `ISNULL(column)` 执行效率也更快一些。



## 常用SQL

### 一、where和having的区别

**1、用的地方不一样**

where可以用于select、update、delete和insert into values(select * from table where ..)语句中。
having只能用于select语句中

**2、执行的顺序不一样**

where的搜索条件是在执行语句进行分组之前应用
having的搜索条件是在分组条件后执行的
即如果where和having一起用时，where会先执行，having后执行

**3、子句有区别**

where子句中的条件表达式having都可以跟，而having子句中的有些表达式where不可以跟；having子句可以用集合函数（sum、count、avg、max和min），而where子句不可以。

**4、总结**

1. WHERE 子句用来筛选 FROM 子句中指定的操作所产生的行。
2. GROUP BY 子句用来分组 WHERE 子句的输出。
3. HAVING 子句用来从分组的结果中筛选行

### 二、删除重复记录

```
delete p
from people p
join people d on p.email = d.email and p.id < d.id;
```

```
delete
from people
where id not in (
      select max(id)
      from people
      group by email
     );
```

### 三、replace 与insert on duplicate效率分析

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

在数据库数据量很少的时候，这两种方式都很快，无论是直接的插入还是有冲突时的更新，都不错，但在数据库表的内容数量比较大(如百万级)的时候，两种方式就不太一样了。

首先是直接的插入操作，两种的插入效率都略低，　比如直接向表里插入1000条数据(百万级的表(innodb引擎))，二者都差不多需要5，6甚至十几秒。究其原因，我的主机性能是一方面，但在向大数据表批量插入数据的时候，每次的插入都要维护索引的，索引固然可以提高查询的效率，但在更新表尤其是大表的时候，索引就成了一个不得不考虑的问题了。

其次是更新表，这里的更新的时候是带主键值的(因为我是从另一个表获取数据再插入，要求主键不能变)　同样直接更新1000条数据，`replace` 的操作要比 `insert on duplicate` 的操作低太多太多，当 `insert` 瞬间完成(感觉)的时候，`replace` 要7,8S, `replace` 慢的原因我是知道的,在更新数据的时候，要先删除旧的，然后插入新的，在这个过程中，还要重新维护索引，所以速度慢,但为何 `insert　on duplicate` 的更新却那么快呢。在向老大请教后，终于知道，`insert on duplicate` 的更新操作虽然也会更新数据，但其对主键的索引却不会有改变，也就是说，`insert on duplicate` 更新对主键索引没有影响。因此对索引的维护成本就低了一些(如果更新的字段不包括主键，那就要另说了)。

### 四、Mysql分页order by数据错乱重复

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

### 五、需要MySQL查询中每一行的序列号

```
SELECT (@row:=@row+1) AS ROW, ID  
FROM TableA ,(SELECT @row := 0) r   
ORDER BY ID DESC
```

### 六、如何以最高的效率从MySQL中随机查询一条记录？

**面试题目**

如何从MySQL一个数据表中查询一条随机的记录，同时要保证效率最高。

从这个题目来看，其实包含了两个要求，第一个要求就是：从MySQL数据表中查询一条随机的记录。第二个要求就是要保证效率最高。

接下来，我们就来尝试使用各种方式来从MySQL数据表中查询数据。

**方法一**

这是最原始最直观的语法，如下：

```sql
SELECT * FROM foo ORDER BY RAND() LIMIT 1
```

当数据表中数据量较小时，此方法可行。但当数据量到达一定程度，比如100万数据或以上，就有很大的性能问题。如果你通过EXPLAIN来分析这个 语句，会发现虽然MySQL通过建立一张临时表来排序，但由于ORDER BY和LIMIT本身的特性，在排序未完成之前，我们还是无法通过LIMIT来获取需要的记录。亦即，你的记录有多少条，就必须首先对这些数据进行排序。

**方法二**

看来对于大数据量的随机数据抽取，性能的症结出在ORDER BY上，那么如何避免？方法二提供了一个方案。

首先，获取数据表的所有记录数：

```sql
SELECT count(*) AS num_rows FROM foo
```

然后，通过对应的后台程序记录下此记录总数（假定为num_rows）。

然后执行：

```sql
SELECT * FROM foo LIMIT [0到num_rows之间的一个随机数],1
```

上面这个随机数的获得可以通过后台程序来完成。此方法的前提是表的ID是连续的或者自增长的。

这个方法已经成功避免了ORDER BY的产生。

**方法三**

有没有可能不用ORDER BY，用一个SQL语句实现方法二？可以，那就是用JOIN。

```sql
SELECT * FROM Bar B JOIN (SELECT CEIL(MAX(ID)*RAND()) AS ID FROM Bar) AS m ON B.ID >= m.ID LIMIT 1;
```

此方法实现了我们的目的，同时，在数据量大的情况下，也避免了ORDER BY所造成的所有记录的排序过程，因为通过JOIN里面的SELECT语句实际上只执行了一次，而不是N次（N等于方法二中的num_rows）。而且， 我们可以在筛选语句上加上“大于”符号，还可以避免因为ID好不连续所产生的记录为空的现象。

在MySQL中查询5条不重复的数据，使用以下：

```sql
SELECT * FROM `table` ORDER BY RAND() LIMIT 5
```

就可以了。但是真正测试一下才发现这样效率非常低。一个15万余条的库，查询5条数据，居然要8秒以上

搜索Google，网上基本上都是查询 `max(id) * rand()` 来随机获取数据。

```sql
SELECT * 
FROM `table` AS t1 JOIN (SELECT ROUND(RAND() * (SELECT MAX(id) FROM `table`)) AS id) AS t2 
WHERE t1.id >= t2.id 
ORDER BY t1.id ASC LIMIT 5;
```

但是这样会产生连续的5条记录。解决办法只能是每次查询一条，查询5次。即便如此也值得，因为15万条的表，查询只需要0.01秒不到。

上面的语句采用的是JOIN，mysql的论坛上有人使用

```sql
SELECT * 
FROM `table` 
WHERE id >= (SELECT FLOOR( MAX(id) * RAND()) FROM `table` ) 
ORDER BY id LIMIT 1;
```

我测试了一下，需要0.5秒，速度也不错，但是跟上面的语句还是有很大差距。总觉有什么地方不正常。

于是我把语句改写了一下。

```sql
SELECT * FROM `table` 
WHERE id >= (SELECT floor(RAND() * (SELECT MAX(id) FROM `table`))) 
ORDER BY id LIMIT 1;
```

这下，效率又提高了，查询时间只有0.01秒

最后，再把语句完善一下，加上MIN(id)的判断。我在最开始测试的时候，就是因为没有加上MIN(id)的判断，结果有一半的时间总是查询到表中的前面几行。

完整查询语句是：

```sql
SELECT * FROM `table` 
WHERE id >= (SELECT floor( RAND() * ((SELECT MAX(id) FROM  `table`)-(SELECT MIN(id) FROM `table`)) + (SELECT MIN(id) FROM  `table`))) 
ORDER BY id LIMIT 1;

SELECT * 
 FROM  `table` AS t1 JOIN (SELECT ROUND(RAND() * ((SELECT MAX(id) FROM  `table`)-(SELECT MIN(id) FROM `table`))+(SELECT MIN(id) FROM `table`))  AS id) AS t2 
WHERE t1.id >= t2.id 
ORDER BY t1.id LIMIT 1;
```

最后对这两个语句进行分别查询10次，

前者花费时间 0.147433 秒
后者花费时间 0.015130 秒

看来采用JOIN的语法比直接在WHERE中使用函数效率还要高很多。



## 事务与锁

### 一、事务的四大特性

Spring事务的本质其实就是数据库对事务的支持，没有数据库的事务支持，spring是无法提供事务功能的

1. **原子性（A）**：是指事务要么都成功，要么都失败。成功就影响数据库，失败就对数据库不影响，保持原样。

2. **一致性（C）**：是指应用层系统从一种正确的状态，在事务成功后，达成另一种正确的状态。比如：A、B账面共计100W，A向B转账，加上事务控制，转成功后，他们账户总额应还是100W，事务应保持这种应用逻辑正确一致。还有，转账（事务成功）前后，数据库内部的数据结构--比如账户表的主键、外键、列必须大于0、Btree、双向链表等约束需要是正确的，和原来一致的。

3. **隔离性（I）**：隔离是指当多个事务提交时，让它们按顺序串行提交，每个时刻只有一个事务提交。但隔离处理并发事务，效率很差。

   所以SQL标准制作者妥协了，提出了4种事务隔离等级

   - `read-uncommited` 未提交就读，可能产生脏读 
   - `read-commited` 提交后读  可能产生不可重复读
   - `repeatable-read` 可重复读  可能产生幻读
   - `serializable` 序列化，最高级别，按顺序串行提交

4. **持久性（D）**：是指事务一旦提交后，对数据库中的数据改变是永久性的。



以上ACID是数据库的特性，C是应用层特性，AID是为C服务的，目的就是要保证逻辑能正确的执行。

- **脏读**：就是A事务在读取数据时，B事务对同一个数据修改了，但B未提交，A再读取时，读到了B修改后的数据，但是B事务提交失败，回滚，A后读到的数据就是B修改后的脏数据，此为脏读。

- **不可重复读**：就是A事务读取数据，B事务改了这个数据，也提交成功了，A再读取就是B修改后的数据，再也不能重复读到最开始的那个数据值了，此为不可重复读

- **幻读**：可重复读就是A事务读取数据，B事务改了这个数据（update），也提交成功了，A再读这个数据，SQL机制强行让A仍然读之前读到的数据值，这就是可重复读，这种机制对Insert操作无效，A事务在可重复读的机制下，读取数据，B事务insert一条数据，提交成功，A再读这个数据，会显示B插入的数据，此为幻读。

隔离级别越高，越能保证数据的完整性和一致性，但是对并发性能的影响也越大。

大多数的数据库默认隔离级别为 `Read Commited`，比如 `SqlServer`、`Oracle`

少数数据库默认隔离级别为：`Repeatable Read` 比如： `MySQL InnoDB`

### 二、MySQL 事务隔离级别？默认是什么级别？

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307153201.png)

mysql默认的事务隔离级别为repeatable-read

### 三、乐观锁与悲观锁的区别

**一、悲观锁**

总是假设最坏的情况，每次去拿数据的时候都认为别人会修改，所以每次在拿数据的时候都会上锁，这样别人想拿这个数据就会阻塞直到它拿到锁（共享资源每次只给一个线程使用，其它线程阻塞，用完后再把资源转让给其它线程）。传统的关系型数据库里边就用到了很多这种锁机制，比如行锁，表锁等，读锁，写锁等，都是在做操作之前先上锁。Java中 `synchronized` 和 `ReentrantLock` 等独占锁就是悲观锁思想的实现。

**二、乐观锁**

总是假设最好的情况，每次去拿数据的时候都认为别人不会修改，所以不会上锁，但是在更新的时候会判断一下在此期间别人有没有去更新这个数据，可以使用版本号机制和CAS算法实现。乐观锁适用于多读的应用类型，这样可以提高吞吐量，像数据库提供的类似于write_condition机制，其实都是提供的乐观锁。在Java中 `java.util.concurrent.atomic` 包下面的原子变量类就是使用了乐观锁的一种实现方式CAS实现的。

**1、乐观锁常见的两种实现方式**

**1.1、版本号机制**

一般是在数据表中加上一个数据版本号 `version` 字段，表示数据被修改的次数，当数据被修改时，`version` 值会加一。当线程A要更新数据值时，在读取数据的同时也会读取version值，在提交更新时，若刚才读取到的version值为当前数据库中的version值相等时才更新，否则重试更新操作，直到更新成功。

举一个简单的例子：

假设数据库中帐户信息表中有一个 version 字段，当前值为 1 ；而当前帐户余额字段（ balance ）为 $100 。当需要对账户信息表进行更新的时候，需要首先读取version字段。

1. 操作员 A 此时将其读出（ version=1 ），并从其帐户余额中扣除 $50（ $100-$50 ）。
1. 在操作员 A 操作的过程中，操作员B 也读入此用户信息（ version=1 ），并从其帐户余额中扣除 $20 （ $100-$20 ）。
1. 操作员 A 完成了修改工作，提交更新之前会先看数据库的版本和自己读取到的版本是否一致，一致的话，就会将数据版本号加1（ version=2 ），连同帐户扣除后余额（ balance=$50 ），提交至数据库更新，此时由于提交数据版本大于数据库记录当前版本，数据被更新，数据库记录 version 更新为 2 。
1. 操作员 B 完成了操作，提交更新之前会先看数据库的版本和自己读取到的版本是否一致，但此时比对数据库记录版本时发现，操作员 B 提交的数据版本号为 2 ，而自己读取到的版本号为1 ，不满足 “ 当前最后更新的version与操作员第一次读取的版本号相等 “ 的乐观锁策略，因此，操作员 B 的提交被驳回。

这样，就避免了操作员 B 用基于 version=1 的旧数据修改的结果覆盖操作员A 的操作结果的可能。

**1.2、CAS算法**

即compare and swap（比较与交换），是一种有名的无锁算法。无锁编程，即不使用锁的情况下实现多线程之间的变量同步，也就是在没有线程被阻塞的情况下实现变量的同步，所以也叫非阻塞同步（Non-blocking Synchronization）。CAS算法涉及到三个操作数
- 需要读写的内存值 V
- 进行比较的值 A
- 拟写入的新值 B

当且仅当 V 的值等于 A时，CAS通过原子方式用新值B来更新V的值，否则不会执行任何操作（比较和替换是一个原子操作）。一般情况下是一个自旋操作，即不断的重试。

**2、乐观锁的缺点**

**2.1、ABA 问题**

如果一个变量V初次读取的时候是A值，并且在准备赋值的时候检查到它仍然是A值，那我们就能说明它的值没有被其他线程修改过了吗？很明显是不能的，因为在这段时间它的值可能被改为其他值，然后又改回A，那CAS操作就会误认为它从来没有被修改过。这个问题被称为CAS操作的 "ABA"问题。

JDK 1.5 以后的 `AtomicStampedReference` 类就提供了此种能力，其中的 `compareAndSet` 方法就是首先检查当前引用是否等于预期引用，并且当前标志是否等于预期标志，如果全部相等，则以原子方式将该引用和该标志的值设置为给定的更新值。

**2.2、循环时间长开销大**

自旋CAS（也就是不成功就一直循环执行直到成功）如果长时间不成功，会给CPU带来非常大的执行开销。 如果JVM能支持处理器提供的pause指令那么效率会有一定的提升，pause指令有两个作用，第一它可以延迟流水线执行指令（de-pipeline）,使CPU不会消耗过多的执行资源，延迟的时间取决于具体实现的版本，在一些处理器上延迟时间是零。第二它可以避免在退出循环的时候因内存顺序冲突（memory order violation）而引起CPU流水线被清空（CPU pipeline flush），从而提高CPU的执行效率。

**2.3、只能保证一个共享变量的原子操作**

CAS 只对单个共享变量有效，当操作涉及跨多个共享变量时 CAS 无效。但是从 JDK 1.5开始，提供了 `AtomicReference` 类来保证引用对象之间的原子性，你可以把多个变量放在一个对象里来进行 CAS 操作.所以我们可以使用锁或者利用 `AtomicReference` 类把多个共享变量合并成一个共享变量来操作。

**三、两种锁的使用场景**

从上面对两种锁的介绍，我们知道两种锁各有优缺点，不可认为一种好于另一种，像乐观锁适用于写比较少的情况下（多读场景），即冲突真的很少发生的时候，这样可以省去了锁的开销，加大了系统的整个吞吐量。但如果是多写的情况，一般会经常产生冲突，这就会导致上层应用会不断的进行retry，这样反倒是降低了性能，所以一般多写的场景下用悲观锁就比较合适。

### 四、说一下 mysql 的行锁和表锁？

相对其他数据库而言，MySQL的锁机制比较简单，其最显著的特点是不同的存储引擎支持不同的锁机制。
MySQL大致可归纳为以下3种锁：
- 表级锁：开销小，加锁快；不会出现死锁；锁定粒度大，发生锁冲突的概率最高，并发度最低。
- 行级锁：开销大，加锁慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低，并发度也最高。
- 页面锁：开销和加锁时间界于表锁和行锁之间；会出现死锁；锁定粒度界于表锁和行锁之间，并发度一般

**一、MySQL表级锁的锁模式（MyISAM)**

MySQL表级锁有两种模式：表共享锁（Table Read Lock）和表独占写锁（Table Write Lock）。
- 对MyISAM的读操作，不会阻塞其他用户对同一表请求，但会阻塞对同一表的写请求；
- 对MyISAM的写操作，则会阻塞其他用户对同一表的读和写操作；
- MyISAM表的读操作和写操作之间，以及写操作之间是串行的。

当一个线程获得对一个表的写锁后，只有持有锁线程可以对表进行更新操作。其他线程的读、写操作都会等待，直到锁被释放为止。

**1、MySQL表级锁的锁模式**

ＭySQL的表锁有两种模式：表共享读锁（Table Read Lock）和表独占写锁（Table Write Lock）。锁模式的兼容如下表
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20200307153902.png)

可见，对ＭyISAM表的读操作，不会阻塞其他用户对同一表的读请求，但会阻塞对同一表的写请求；对ＭyISAM表的写操作，则会阻塞其他用户对同一表的读和写请求；ＭyISAM表的读和写操作之间，以及写和写操作之间是串行的！（当一线程获得对一个表的写锁后，只有持有锁的线程可以对表进行更新操作。其他线程的读、写操作都会等待，直到锁被释放为止。）

**2、如何加表锁**

MyISAM在执行查询语句（SELECT）前，会自动给涉及的所有表加读锁，在执行更新操作（UPDATE、DELETE、INSERT等）前，会自动给涉及的表加写锁，这个过程并不需要用户干预，因此用户一般不需要直接用LOCK TABLE命令给MyISAM表显式加锁。在本书的示例中，显式加锁基本上都是为了方便而已，并非必须如此。
给MyISAM表显示加锁，一般是为了一定程度模拟事务操作，实现对某一时间点多个表的一致性读取。例如，有一个订单表orders，其中记录有订单的总金额total，同时还有一个订单明细表order_detail，其中记录有订单每一产品的金额小计subtotal，假设我们需要检查这两个表的金额合计是否相等，可能就需要执行如下两条SQL：

```
SELECT SUM(total) FROM orders;
SELECT SUM(subtotal) FROM order_detail;
```
这时，如果不先给这两个表加锁，就可能产生错误的结果，因为第一条语句执行过程中，order_detail表可能已经发生了改变。因此，正确的方法应该是：
```
LOCK tables orders read local,order_detail read local;
SELECT SUM(total) FROM orders;
SELECT SUM(subtotal) FROM order_detail;
Unlock tables;
```
要特别说明以下两点内容：
- 上面的例子在LOCK TABLES时加了‘local’选项，其作用就是在满足MyISAM表并发插入条件的情况下，允许其他用户在表尾插入记录
- 在用 `LOCK TABLES` 给表显式加表锁是时，必须同时取得所有涉及表的锁，并且MySQL支持锁升级。也就是说，在执行 `LOCK TABLES` 后，只能访问显式加锁的这些表，不能访问未加锁的表；同时，如果加的是读锁，那么只能执行查询操作，而不能执行更新操作。其实，在自动加锁的情况下也基本如此，MySQL问题一次获得SQL语句所需要的全部锁。这也正是MyISAM表不会出现死锁（Deadlock Free）的原因

一个session使用 `LOCK TABLE` 命令给表 `film_text` 加了读锁，这个session可以查询锁定表中的记录，但更新或访问其他表都会提示错误；同时，另外一个session可以查询表中的记录，但更新就会出现锁等待。
当使用 `LOCK TABLE` 时，不仅需要一次锁定用到的所有表，而且，同一个表在SQL语句中出现多少次，就要通过与SQL语句中相同的别名锁多少次，否则也会出错！

**3、并发锁**

 在一定条件下，MyISAM也支持查询和操作的并发进行。

MyISAM存储引擎有一个系统变量 `concurrent_insert`，专门用以控制其并发插入的行为，其值分别可以为0、1或2。
- 当 `concurrent_insert` 设置为0时，不允许并发插入。
- 当 `concurrent_insert` 设置为1时，如果MyISAM允许在一个读表的同时，另一个进程从表尾插入记录。这也是MySQL的默认设置。
- 当 `concurrent_insert` 设置为2时，无论MyISAM表中有没有空洞，都允许在表尾插入记录，都允许在表尾并发插入记录。

可以利用MyISAM存储引擎的并发插入特性，来解决应用中对同一表查询和插入锁争用。例如，将`concurrent_insert` 系统变量为2，总是允许并发插入；同时，通过定期在系统空闲时段执行 `OPTIONMIZE TABLE` 语句来整理空间碎片，收到因删除记录而产生的中间空洞。

**4、MyISAM的锁调度**

前面讲过，MyISAM存储引擎的读和写锁是互斥，读操作是串行的。那么，一个进程请求某个MyISAM表的读锁，同时另一个进程也请求同一表的写锁，MySQL如何处理呢？答案是写进程先获得锁。不仅如此，即使读进程先请求先到锁等待队列，写请求后到，写锁也会插到读请求之前！这是因为MySQL认为写请求一般比读请求重要。这也正是MyISAM表不太适合于有大量更新操作和查询操作应用的原因，因为，大量的更新操作会造成查询操作很难获得读锁，从而可能永远阻塞。这种情况有时可能会变得非常糟糕！幸好我们可以通过一些设置来调节MyISAM的调度行为。
- 通过指定启动参数 `low-priority-updates`，使MyISAM引擎默认给予读请求以优先的权利。
- 通过执行命令 `SET LOW_PRIORITY_UPDATES=1`，使该连接发出的更新请求优先级降低。
- 通过指定INSERT、UPDATE、DELETE语句的LOW_PRIORITY属性，降低该语句的优先级。

虽然上面3种方法都是要么更新优先，要么查询优先的方法，但还是可以用其来解决查询相对重要的应用（如用户登录系统）中，读锁等待严重的问题。

另外，MySQL也提供了一种折中的办法来调节读写冲突，即给系统参数 `max_write_lock_count` 设置一个合适的值，当一个表的读锁达到这个值后，MySQL变暂时将写请求的优先级降低，给读进程一定获得锁的机会。

上面已经讨论了写优先调度机制和解决办法。这里还要强调一点：一些需要长时间运行的查询操作，也会使写进程“饿死”！因此，应用中应尽量避免出现长时间运行的查询操作，不要总想用一条SELECT语句来解决问题。因为这种看似巧妙的SQL语句，往往比较复杂，执行时间较长，在可能的情况下可以通过使用中间表等措施对SQL语句做一定的“分解”，使每一步查询都能在较短时间完成，从而减少锁冲突。如果复杂查询不可避免，应尽量安排在数据库空闲时段执行，比如一些定期统计可以安排在夜间执行。

**二、InnoDB锁问题**

InnoDB与MyISAM的最大不同有两点：一是支持事务（TRANSACTION）；二是采用了行级锁。

**1、InnoDB的行锁模式及加锁方法**

InnoDB实现了以下两种类型的行锁。
- 共享锁（s）：允许一个事务去读一行，阻止其他事务获得相同数据集的排他锁。
- 排他锁（Ｘ）：允许获取排他锁的事务更新数据，阻止其他事务取得相同的数据集共享读锁和排他写锁。

另外，为了允许行锁和表锁共存，实现多粒度锁机制，InnoDB还有两种内部使用的意向锁（Intention Locks），这两种意向锁都是表锁。

意向共享锁（IS）：事务打算给数据行共享锁，事务在给一个数据行加共享锁前必须先取得该表的IS锁。

意向排他锁（IX）：事务打算给数据行加排他锁，事务在给一个数据行加排他锁前必须先取得该表的IX锁。

**InnoDB行锁模式兼容性列表**
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307153903.png)
如果一个事务请求的锁模式与当前的锁兼容，InnoDB就请求的锁授予该事务；反之，如果两者两者不兼容，该事务就要等待锁释放。

意向锁是InnoDB自动加的，不需用户干预。对于UPDATE、DELETE和INSERT语句，InnoDB会自动给涉及及数据集加排他锁（Ｘ）；对于普通SELECT语句，InnoDB会自动给涉及数据集加排他锁（Ｘ）；对于普通SELECT语句，InnoDB不会任何锁；事务可以通过以下语句显示给记录集加共享锁或排锁。

共享锁（Ｓ）：`SELECT * FROM table_name WHERE ... LOCK IN SHARE MODE`

排他锁（X）：`SELECT * FROM table_name WHERE ... FOR UPDATE`

用 `SELECT .. IN SHARE MODE` 获得共享锁，主要用在需要数据依存关系时确认某行记录是否存在，并确保没有人对这个记录进行 `UPDATE` 或者 `DELETE` 操作。但是如果当前事务也需要对该记录进行更新操作，则很有可能造成死锁，对于锁定行记录后需要进行更新操作的应用，应该使用 `SELECT ... FOR UPDATE` 方式获取排他锁。

**2、InnoDB行锁实现方式**

InnoDB行锁是通过索引上的索引项来实现的，这一点ＭySQL与Oracle不同，后者是通过在数据中对相应数据行加锁来实现的。InnoDB这种行锁实现特点意味者：只有通过索引条件检索数据，InnoDB才会使用行级锁，否则，InnoDB将使用表锁！

在实际应用中，要特别注意InnoDB行锁的这一特性，不然的话，可能导致大量的锁冲突，从而影响并发性能。

**3、间隙锁（Next-Key锁）**

当我们用范围条件而不是相等条件检索数据，并请求共享或排他锁时，InnoDB会给符合条件的已有数据的索引项加锁；对于键值在条件范围内但并不存在的记录，叫做“间隙(GAP)”，InnoDB也会对这个“间隙”加锁，这种锁机制不是所谓的间隙锁（Next-Key锁）。

举例来说，假如emp表中只有101条记录，其empid的值分别是1,2,...,100,101，下面的SQL：
```
SELECT * FROM emp WHERE empid > 100 FOR UPDATE
```
是一个范围条件的检索，InnoDB不仅会对符合条件的empid值为101的记录加锁，也会对empid大于101（这些记录并不存在）的“间隙”加锁。

InnoDB使用间隙锁的目的，一方面是为了防止幻读，以满足相关隔离级别的要求，对于上面的例子，要是不使用间隙锁，如果其他事务插入了empid大于100的任何记录，那么本事务如果再次执行上述语句，就会发生幻读；另一方面，是为了满足其恢复和复制的需要。有关其恢复和复制对机制的影响，以及不同隔离级别下InnoDB使用间隙锁的情况。

很显然，在使用范围条件检索并锁定记录时，InnoDB这种加锁机制会阻塞符合条件范围内键值的并发插入，这往往会造成严重的锁等待。因此，在实际开发中，尤其是并发插入比较多的应用，我们要尽量优化业务逻辑，尽量使用相等条件来访问更新数据，避免使用范围条件。

**4、什么时候使用表锁**

对于InnoDB表，在绝大部分情况下都应该使用行级锁，因为事务和行锁往往是我们之所以选择InnoDB表的理由。但在个另特殊事务中，也可以考虑使用表级锁。
- 第一种情况是：事务需要更新大部分或全部数据，表又比较大，如果使用默认的行锁，不仅这个事务执行效率低，而且可能造成其他事务长时间锁等待和锁冲突，这种情况下可以考虑使用表锁来提高该事务的执行速度。
- 第二种情况是：事务涉及多个表，比较复杂，很可能引起死锁，造成大量事务回滚。这种情况也可以考虑一次性锁定事务涉及的表，从而避免死锁、减少数据库因事务回滚带来的开销。

当然，应用中这两种事务不能太多，否则，就应该考虑使用ＭyISAＭ表。

在InnoDB下 ，使用表锁要注意以下两点：
1. 使用 `LOCK TALBES` 虽然可以给 `InnoDB` 加表级锁，但必须说明的是，表锁不是由 `InnoDB` 存储引擎层管理的，而是由其上一层 `ＭySQL Server` 负责的，仅当 `autocommit=0`、`innodb_table_lock=1`（默认设置）时，InnoDB层才能知道MySQL加的表锁，ＭySQL Server才能感知InnoDB加的行锁，这种情况下，InnoDB才能自动识别涉及表级锁的死锁；否则，InnoDB将无法自动检测并处理这种死锁。
1. 在用 `LOCAK TABLES` 对InnoDB锁时要注意，要将 `AUTOCOMMIT` 设为0，否则ＭySQL不会给表加锁；事务结束前，不要用 `UNLOCAK TABLES` 释放表锁，因为 `UNLOCK TABLES` 会隐含地提交事务；`COMMIT` 或`ROLLBACK` 产不能释放用 `LOCAK TABLES` 加的表级锁，必须用 `UNLOCK TABLES` 释放表锁，正确的方式见如下语句。

例如，如果需要写表t1并从表t读，可以按如下做：
```
SET AUTOCOMMIT=0;
LOCAK TABLES t1 WRITE, t2 READ, ...;
[do something with tables t1 and here];
COMMIT;
UNLOCK TABLES;
```

**三、关于死锁**

ＭyISAM表锁是 `deadlock free` 的，这是因为ＭyISAM总是一次性获得所需的全部锁，要么全部满足，要么等待，因此不会出现死锁。但是在InnoDB中，除单个SQL组成的事务外，锁是逐步获得的，这就决定了InnoDB发生死锁是可能的。

发生死锁后，InnoDB一般都能自动检测到，并使一个事务释放锁并退回，另一个事务获得锁，继续完成事务。但在涉及外部锁，或涉及锁的情况下，InnoDB并不能完全自动检测到死锁，这需要通过设置锁等待超时参数`innodb_lock_wait_timeout` 来解决。需要说明的是，这个参数并不是只用来解决死锁问题，在并发访问比较高的情况下，如果大量事务因无法立即获取所需的锁而挂起，会占用大量计算机资源，造成严重性能问题，甚至拖垮数据库。我们通过设置合适的锁等待超时阈值，可以避免这种情况发生。

通常来说，死锁都是应用设计的问题，通过调整业务流程、数据库对象设计、事务大小、以及访问数据库的SQL语句，绝大部分都可以避免。下面就通过实例来介绍几种死锁的常用方法：
1. 在应用中，如果不同的程序会并发存取多个表，应尽量约定以相同的顺序为访问表，这样可以大大降低产生死锁的机会。如果两个session访问两个表的顺序不同，发生死锁的机会就非常高！但如果以相同的顺序来访问，死锁就可能避免。
1. 在程序以批量方式处理数据的时候，如果事先对数据排序，保证每个线程按固定的顺序来处理记录，也可以大大降低死锁的可能。
1. 在事务中，如果要更新记录，应该直接申请足够级别的锁，即排他锁，而不应该先申请共享锁，更新时再申请排他锁，甚至死锁。
1. 在 `REPEATEABLE-READ` 隔离级别下，如果两个线程同时对相同条件记录用 `SELECT...ROR UPDATE` 加排他锁，在没有符合该记录情况下，两个线程都会加锁成功。程序发现记录尚不存在，就试图插入一条新记录，如果两个线程都这么做，就会出现死锁。这种情况下，将隔离级别改成 `READ COMMITTED`，就可以避免问题。
1. 当隔离级别为 `READ COMMITED` 时，如果两个线程都先执行 `SELECT...FOR UPDATE` ，判断是否存在符合条件的记录，如果没有，就插入记录。此时，只有一个线程能插入成功，另一个线程会出现锁等待，当第１个线程提交后，第２个线程会因主键重出错，但虽然这个线程出错了，却会获得一个排他锁！这时如果有第３个线程又来申请排他锁，也会出现死锁。对于这种情况，可以直接做插入操作，然后再捕获主键重异常，或者在遇到主键重错误时，总是执行ROLLBACK释放获得的排他锁。

尽管通过上面的设计和优化等措施，可以大减少死锁，但死锁很难完全避免。因此，在程序设计中总是捕获并处理死锁异常是一个很好的编程习惯。

如果出现死锁，可以用 `SHOW INNODB STATUS` 命令来确定最后一个死锁产生的原因和改进措施。

**四、总结**

对于ＭyISAM的表锁，主要有以下几点
1. 共享读锁（S）之间是兼容的，但共享读锁（S）和排他写锁（X）之间，以及排他写锁之间（X）是互斥的，也就是说读和写是串行的。
1. 在一定条件下，ＭyISAM允许查询和插入并发执行，我们可以利用这一点来解决应用中对同一表和插入的锁争用问题。
1. ＭyISAM默认的锁调度机制是写优先，这并不一定适合所有应用，用户可以通过设置 `LOW_PRIPORITY_UPDATES` 参数，或在INSERT、UPDATE、DELETE语句中指定 `LOW_PRIORITY` 选项来调节读写锁的争用。
1. 由于表锁的锁定粒度大，读写之间又是串行的，因此，如果更新操作较多，ＭyISAM表可能会出现严重的锁等待，可以考虑采用InnoDB表来减少锁冲突。

对于InnoDB表，主要有以下几点
1. InnoDB的行销是基于索引实现的，如果不通过索引访问数据，InnoDB会使用表锁。
1. InnoDB间隙锁机制，以及InnoDB使用间隙锁的原因。
1. 在不同的隔离级别下，InnoDB的锁机制和一致性读策略不同。
1. ＭySQL的恢复和复制对InnoDB锁机制和一致性读策略也有较大影响。
1. 锁冲突甚至死锁很难完全避免。

在了解InnoDB的锁特性后，用户可以通过设计和SQL调整等措施减少锁冲突和死锁，包括：
- 尽量使用较低的隔离级别
- 精心设计索引，并尽量使用索引访问数据，使加锁更精确，从而减少锁冲突的机会。
- 选择合理的事务大小，小事务发生锁冲突的几率也更小。
- 给记录集显示加锁时，最好一次性请求足够级别的锁。比如要修改数据的话，最好直接申请排他锁，而不是先申请共享锁，修改时再请求排他锁，这样容易产生死锁。
- 不同的程序访问一组表时，应尽量约定以相同的顺序访问各表，对一个表而言，尽可能以固定的顺序存取表中的行。这样可以大减少死锁的机会。
- 尽量用相等条件访问数据，这样可以避免间隙锁对并发插入的影响。
- 不要申请超过实际需要的锁级别；除非必须，查询时不要显示加锁。
- 对于一些特定的事务，可以使用表锁来提高处理速度或减少死锁的可能



## 存储引擎

### 一、数据库两种存储引擎的区别

**一、InnoDB存储引擎**

InnoDB是事务型数据库的首选引擎，支持事务安全表（ACID），支持行锁定和外键，上图也看到了，InnoDB是默认的MySQL引擎。InnoDB主要特性有：
1. InnoDB给MySQL提供了具有提交. 回滚和崩溃恢复能力的事物安全（ACID兼容）存储引擎。InnoDB锁定在行级并且也在SELECT语句中提供一个类似Oracle的非锁定读。这些功能增加了多用户部署和性能。在SQL查询中，可以自由地将InnoDB类型的表和其他MySQL的表类型混合起来，甚至在同一个查询中也可以混合

2. InnoDB是为处理巨大数据量的最大性能设计。它的CPU效率可能是任何其他基于磁盘的关系型数据库引擎锁不能匹敌的

3. InnoDB存储引擎完全与MySQL服务器整合，InnoDB存储引擎为在主内存中缓存数据和索引而维持它自己的缓冲池。InnoDB将它的表和索引在一个逻辑表空间中，表空间可以包含数个文件（或原始磁盘文件）。这与MyISAM表不同，比如在MyISAM表中每个表被存放在分离的文件中。InnoDB表可以是任何尺寸，即使在文件尺寸被限制为2GB的操作系统上

4. InnoDB支持外键完整性约束，存储表中的数据时，每张表的存储都按主键顺序存放，如果没有显示在表定义时指定主键，InnoDB会为每一行生成一个6字节的ROWID，并以此作为主键

5. InnoDB被用在众多需要高性能的大型数据库站点上

InnoDB不创建目录，使用InnoDB时，MySQL将在MySQL数据目录下创建一个名为ibdata1的10MB大小的自动扩展数据文件，以及两个名为ib_logfile0和ib_logfile1的5MB大小的日志文件

**二、MyISAM存储引擎**

MyISAM基于ISAM存储引擎，并对其进行扩展。它是在Web、数据仓储和其他应用环境下最常使用的存储引擎之一。MyISAM拥有较高的插入、查询速度，但不支持事物。MyISAM主要特性有：
1. 大文件（达到63位文件长度）在支持大文件的文件系统和操作系统上被支持

2. 当把删除和更新及插入操作混合使用的时候，动态尺寸的行产生更少碎片。这要通过合并相邻被删除的块，以及若下一个块被删除，就扩展到下一块自动完成

3. 每个MyISAM表最大索引数是64，这可以通过重新编译来改变。每个索引最大的列数是16

4. 最大的键长度是1000字节，这也可以通过编译来改变，对于键长度超过250字节的情况，一个超过1024字节的键将被用上

5. BLOB和TEXT列可以被索引

6. NULL被允许在索引的列中，这个值占每个键的0~1个字节

7. 所有数字键值以高字节优先被存储以允许一个更高的索引压缩

8. 每个MyISAM类型的表都有一个AUTO_INCREMENT的内部列，当INSERT和UPDATE操作的时候该列被更新，同时AUTO_INCREMENT列将被刷新。所以说，MyISAM类型表的AUTO_INCREMENT列更新比InnoDB类型的AUTO_INCREMENT更快

9. 可以把数据文件和索引文件放在不同目录

10. 每个字符列可以有不同的字符集

11. 有VARCHAR的表可以固定或动态记录长度

12. VARCHAR和CHAR列可以多达64KB

使用MyISAM引擎创建数据库，将产生3个文件。文件的名字以表名字开始，扩展名之处文件类型：frm文件存储表定义、数据文件的扩展名为.MYD（MYData）、索引文件的扩展名时.MYI（MYIndex）

**三、区别**

1. InnoDB支持事务，MyISAM不支持，这一点是非常之重要。事务是一种高级的处理方式，如在一些列增删改中只要哪个出错还可以回滚还原，而MyISAM就不可以了。
1. MyISAM适合查询以及插入为主的应用，InnoDB适合频繁修改以及涉及到安全性较高的应用
1. InnoDB支持外键，MyISAM不支持
1. 从MySQL5.5.5以后，InnoDB是默认引擎
1. InnoDB不支持FULLTEXT类型的索引
1. InnoDB中不保存表的行数，如select count() from table时，InnoDB需要扫描一遍整个表来计算有多少行，但是MyISAM只要简单的读出保存好的行数即可。注意的是，当count()语句包含where条件时MyISAM也需要扫描整个表。
1. 对于自增长的字段，InnoDB中必须包含只有该字段的索引，但是在MyISAM表中可以和其他字段一起建立联合索引。
1. 清空整个表时，InnoDB是一行一行的删除，效率非常慢。MyISAM则会重建表。
1. InnoDB支持行锁（某些情况下还是锁整表，如 update table set a=1 where user like ‘%lee%’

### 二、一张自增表里面总共有 7 条数据，删除了最后 2 条数据，重启 mysql 数据库，又插入了一条数据，此时 id 是几？

- 数据库引擎如果是 MyISAM ，那 id 就是 8。
- 数据库引擎如果是 InnoDB，那 id 就是 6。

InnoDB 表只会把自增主键的最大 id 记录在内存中，所以重启之后会导致最大 id 丢失。

### 三、明明已经删除了数据，可是表文件大小依然没变

对于运行很长时间的数据库来说，往往会出现表占用存储空间过大的问题，可是将许多没用的表删除之后，表文件的大小并没有改变，想解决这个问题，就需要了解 InnoDB 如何回收表空间的。

对于一张表来说，占用空间重要分为两部分，表结构和表数据。通常来说，表结构定义占用的空间很小。所以空间的问题主要和表数据有关。

在 MySQL 8.0 前，表结构存储在以 .frm 为后缀的文件里。在 8.0，允许将表结构定义在系统数据表中。

**1、关于表数据的存放**

可以将表数据存在共享表空间，或者单独的文件中，通过 `innodb_file_per_table` 来控制。

- 如果为 OFF ，表示存在系统共享表空间中，和数据字典一起
- 如果为 ON，每个 InnoDB 表结构存储在 .idb 为后缀的文件中

在 5.6.6 以后，默认值为 ON.

> 建议将该参数设置为 ON，这样在不需要时，通过 drop table 命令，系统就会直接删除该文件。
> 但在共享表空间中，即使表删掉，空间也不会回收。

```
truncate = drop + create 
```

**2、数据删除流程**

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

**3、插入操作也会造成空隙**

在插入数据时，如果数据按照索引递增顺序插入，索引的结构会是紧凑的。但如果是随机插入的，很可能造成索引数据**页分裂**。

比如给已满的 Page A 插入数据。
![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307141109.png)
由于 Page A 满了，所以要申请 Page B，调整 Page A 的过程到 Page B，这也称为页分裂。

结束后 Page A 就有了空隙。

另外对于更新操作也是，先删除再插入，也会造成空隙。

进而对于大量进行增删改的表，都有可能存在空洞。如果把空洞去掉，自然空间就被释放了。

**4、 使用重建表**

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

**5、Online DDL**

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

**6、 Online 和 inplace**

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

**7、 拓展**

说一下 optimize，analyze，alter table 三种重建表之间的区别：

1. `alter table t engine = InnoDB`（也就是 recreate）默认的是 Oline DDL 过程。
1. `analyze table t` 不是重建表，仅仅是对表的索引信息做重新统计，没有修改数据，期间加 MDL 读锁。
1. `optimize table t` 等于上两步的操作。

> 在事务里面使用 alter table 默认会自动提交事务，保持事务一致性

如果有时，在重建某张表后，空间不仅没有变小，甚至还变大了一点点。这时因为，重建的这张表本身没有空隙，在 DDL 期间，刚好有一些 DML 执行，引入了一些新的空隙。

而且 InnoDB 不会把整张表填满，每个页留下 1/16 给后续的更新用，所以可能远离是紧凑的，但重建后变成的稍有空隙。

**8、页合并与页分裂**

**页合并**：既然产生了数据空洞，那么数据文件将会变得越来越大，这样是很不利的，所以 MySQL 会在数据空洞达到一定比例后出触发 "页合并"，触发的页会找最靠近的可以合并的页进行合并来优化空间（只会将数据页使用权腾出来，并不会减小表文件大小），防止后续的数据插入使用更多的数据页造成文件更大。

**页分裂**：页分裂是在插入操作时操作的记录主键 ID 在原本的记录之间时产生的，因为记录存储在数据页中，如果该数据页没有合适的位置来存储这条记录，那么就会将该条记录以及后面的记录另开要一个数据页来存储。

![image-20210312231147736](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312231147.png)

**优化**：因为页合并和页分裂都需要消耗额外的性能。所以我们在插入数据时应当按主键递增顺序插入（主键可以使用自增ID 或 雪花算法，但如果业务字段有唯一字段且没有其他索引，那么可以使用其作为主键来避免每次查询都需要回表），删除数据时按主键顺序删除。

**9、如何减小表文件**

- 自动触发的页合并。

- 手动触发清理大部分的数据空洞--上面有介绍

**10、总结**

现在我们知道，在使用 delete 删除数据时，其实对应的数据行并不是真正的删除，InnoDB 仅仅是将其标记成可复用的状态，所以表空间不会变小。

通常来说，在标记复用空间时分为两种，一种是仅将某些数据页中的位置标记为删除状态，但这样的位置只会在一定范围内使用，会出现空隙的情况。

另一种是将整个数据页标记成可复用的状态，这样的数据页没有限制，可直接复用。

为了解决这个问题，我们可以采用重建表的方式，其中在 5.6 版本后，创建表已经支持 Online 的操作，但最后是在业务低峰时使用


## 索引

### 一、面试官考点之索引是什么？

![20210312125203](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125203.png)

- 索引是一种能提高数据库查询效率的数据结构。它可以比作一本字典的目录，可以帮你快速找到对应的记录。
- 索引一般存储在磁盘的文件中，它是占用物理空间的。
- 正所谓水能载舟，也能覆舟。适当的索引能提高查询效率，过多的索引会影响数据库表的插入和更新功能。

### 二、索引有哪些类型类型

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125233.png)

**数据结构维度**

- B+树索引：所有数据存储在叶子节点，复杂度为O(logn)，适合范围查询。
- 哈希索引: 适合等值查询，检索效率高，一次到位。
- 全文索引：`MyISAM` 和 `InnoDB` 中都支持使用全文索引，一般在文本类型 `char` ,`text`,`varchar` 类型上创建。
- R-Tree索引: 用来对 `GIS` 数据类型创建 `SPATIAL` 索引

**物理存储维度**

- 聚集索引：聚集索引就是以主键创建的索引，在叶子节点存储的是表中的数据。
- 非聚集索引：非聚集索引就是以非主键创建的索引，在叶子节点存储的是主键和索引列。

**逻辑维度**

- 主键索引：一种特殊的唯一索引，不允许有空值。
- 普通索引：`MySQL` 中基本索引类型，允许空值和重复值。
- 联合索引：多个字段创建的索引，使用时遵循最左前缀原则。
- 唯一索引：索引列中的值必须是唯一的，但是允许为空值。
- 空间索引：`MySQL5.7` 之后支持空间索引，在空间索引这方面遵循 `OpenGIS` 几何数据模型规则。

### 三、面试官考点之为什么选择B+树作为索引结构

可以从这几个维度去看这个问题，查询是否够快，效率是否稳定，存储数据多少，以及查找磁盘次数等等。为什么不是哈希结构？为什么不是二叉树，为什么不是平衡二叉树，为什么不是B树，而偏偏是B+树呢？

我们写业务SQL查询时，大多数情况下，都是范围查询的，如下SQL

```
select * from employee where age between 18 and 28;
```

**为什么不使用哈希结构？**

我们知道哈希结构，类似k-v结构，也就是，key和value是一对一关系。它用于**等值查询**还可以，但是范围查询它是无能为力的哦。

**为什么不使用二叉树呢？**

先回忆下二叉树相关知识啦~ 所谓**二叉树，特点如下：**

- 每个结点最多两个子树，分别称为左子树和右子树。
- 左子节点的值小于当前节点的值，当前节点值小于右子节点值
- 顶端的节点称为根节点，没有子节点的节点值称为叶子节点。

如果二叉树特殊化为一个链表，相当于全表扫描。那么还要索引干嘛呀？因此，一般二叉树不适合作为索引结构。

我们脑海中，很容易就浮现出这种二叉树结构图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125321.webp)

但是呢，有些特殊二叉树，它可能这样的哦：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125338.png)

如果二叉树特殊化为一个链表，相当于全表扫描。那么还要索引干嘛呀？因此，一般二叉树不适合作为索引结构。



#### 为什么不使用平衡二叉树呢？

平衡二叉树特点：它也是一颗二叉查找树，任何节点的两个子树高度最大差为1。所以就不会出现特殊化一个链表的情况啦。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125356.webp)

但是呢：

- 平衡二叉树插入或者更新是，需要左旋右旋维持平衡，维护代价大
- 如果数量多的话，树的高度会很高。因为数据是存在磁盘的，以它作为索引结构，每次从磁盘读取一个节点，操作IO的次数就多啦。

#### 为什么不使用B树呢？

数据量大的话，平衡二叉树的高度会很高，会增加IO嘛。那为什么不选择同样数据量，**高度更矮的B树**呢？

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125418.png)

B树相对于平衡二叉树，就可以存储更多的数据，高度更低。但是最后为甚选择B+树呢？因为B+树是B树的升级版：

> - B+树非叶子节点上是不存储数据的，仅存储键值，而B树节点中不仅存储键值，也会存储数据。innodb中页的默认大小是16KB，如果不存储数据，那么就会存储更多的键值，相应的树的阶数（节点的子节点树）就会更大，树就会更矮更胖，如此一来我们查找数据进行磁盘的IO次数有会再次减少，数据查询的效率也会更快。
> - B+树索引的所有数据均存储在叶子节点，而且数据是按照顺序排列的，链表连着的。那么B+树使得范围查找，排序查找，分组查找以及去重查找变得异常简单。

### 四、面试官考点之一次B+树索引搜索过程

**面试官：** 假设有以下表结构，并且有这几条数据

```
CREATE TABLE `employee` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `age` int(11) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `sex` int(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_age` (`age`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into employee values(100,'小伦',43,'2021-01-20','0');
insert into employee values(200,'俊杰',48,'2021-01-21','0');
insert into employee values(300,'紫琪',36,'2020-01-21','1');
insert into employee values(400,'立红',32,'2020-01-21','0');
insert into employee values(500,'易迅',37,'2020-01-21','1');
insert into employee values(600,'小军',49,'2021-01-21','0');
insert into employee values(700,'小燕',28,'2021-01-21','1');
```

**面试官：** 如果执行以下的查询SQL，需要执行几次的树搜索操作？可以画下对应的索引结构图~

```
select * from Temployee where age=32;
```

**解析：** 其实这个，面试官就是考察候选人是否熟悉B+树索引结构图。可以像酱紫回答~

- 先画出`idx_age`索引的索引结构图，大概如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312130204.png)

- 再画出id主键索引，我们先画出聚族索引结构图，如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125509.png)

因此，这条 SQL 查询语句执行大概流程就是酱紫：

1. 搜索`idx_age`索引树，将磁盘块1加载到内存，由于32<37,搜索左路分支，到磁盘寻址磁盘块2。
   
2. 将磁盘块2加载到内存中，在内存继续遍历，找到age=32的记录，取得id = 400.
  
3. 拿到id=400后，回到id主键索引树。
  
4. 搜索`id主键`索引树，将磁盘块1加载内存，在内存遍历，找到了400，但是B+树索引非叶子节点是不保存数据的。索引会继续搜索400的右分支，到磁盘寻址磁盘块3.
   
5. 将磁盘块3加载内存，在内存遍历，找到id=400的记录，拿到R4这一行的数据，好的，大功告成。

因此，这个SQL查询，执行了几次树的搜索操作，是不是一步了然了呀。**「特别的」**，在`idx_age`二级索引树找到主键`id`后，回到id主键索引搜索的过程,就称为回表。

> 什么是回表？拿到主键再回到主键索引查询的过程，就叫做**「回表」**
>

### 五、面试官考点之覆盖索引

**面试官：** 如果不用`select *`, 而是使用`select id,age`，以上的题目执行了几次树搜索操作呢？

**解析：** 这个问题，主要考察候选人的覆盖索引知识点。回到`idx_age`索引树，你可以发现查询选项id和age都在叶子节点上了。因此，可以直接提供查询结果啦，根本就不需要再回表了~

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125642.png)

> 覆盖索引：在查询的数据列里面，不需要回表去查，直接从索引列就能取到想要的结果。换句话说，你SQL用到的索引列数据，覆盖了查询结果的列，就算上覆盖索引了。

所以，相对于上个问题，就是省去了回表的树搜索操作。

### 六、面试官考点之索引失效

**面试官：** 如果我现在给`name`字段加上普通索引，然后用个like模糊搜索，那会执行多少次查询呢？SQL如下：

```
select * from employee where name like '%杰伦%';
```

**解析：** 这里考察的知识点就是，like是否会导致不走索引，看先该SQL的explain执行计划吧。其实like 模糊搜索，会导致不走索引的，如下:

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125659.png)

因此，这条SQL最后就全表扫描啦~日常开发中，这几种骚操作都可能会导致索引失效，如下：

- 查询条件包含or，可能导致索引失效
- 如何字段类型是字符串，where时一定用引号括起来，否则索引失效
- like通配符可能导致索引失效。
- 联合索引，查询时的条件列不是联合索引中的第一个列，索引失效。
- 在索引列上使用mysql的内置函数，索引失效。
- 对索引列运算（如，+、-、*、/），索引失效。
- 索引字段上使用（！= 或者 < >，not in）时，可能会导致索引失效。
- 索引字段上使用is null， is not null，可能导致索引失效。
- 左连接查询或者右连接查询查询关联的字段编码格式不一样，可能导致索引失效。
- mysql估计使用全表扫描要比使用索引快,则不使用索引。

### 七、面试官考点联合索引之最左前缀原则

**面试官：** 如果我现在给name,age字段加上联合索引索引，以下SQL执行多少次树搜索呢？先画下索引树？

```
select * from employee where name like '小%' order by age desc;
```

**「解析：」** 这里考察联合索引的最左前缀原则以及like是否中索引的知识点。组合索引树示意图大概如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125719.png)

联合索引项是先按姓名name从小到大排序，如果名字name相同，则按年龄age从小到大排序。面试官要求查所有名字第一个字是“小”的人，SQL的like '小%'是可以用上`idx_name_age`联合索引的。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125758.png)

该查询会沿着idx_name_age索引树，找到第一个字是小的索引值，因此依次找到`小军、小伦、小燕、`，分别拿到Id=`600、100、700`，然后回三次表，去找对应的记录。这里面的最左前缀`小`，就是字符串索引的最左M个字符。实际上，

- 这个最左前缀可以是联合索引的最左N个字段。比如组合索引（a,b,c）可以相当于建了（a），（a,b）,(a,b,c)三个索引，大大提高了索引复用能力。
- 最左前缀也可以是字符串索引的最左M个字符。

### 八、面试官考点之索引下推

**面试官：** 我们还是居于组合索引 idx_name_age，以下这个SQL执行几次树搜索呢？

```
select * from employee where name like '小%' and age=28 and sex='0';
```

**「解析：」** 这里考察索引下推的知识点，如果是**「Mysql5.6之前」**，在idx_name_age索引树，找出所有名字第一个字是“小”的人，拿到它们的主键id，然后回表找出数据行，再去对比年龄和性别等其他字段。如图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125908.png)

有些朋友可能觉得奇怪，（name,age)不是联合索引嘛？为什么选出包含“小”字后，不再顺便看下年龄age再回表呢，不是更高效嘛？所以呀，MySQL 5.6 就引入了**「索引下推优化」**，可以在索引遍历过程中，对索引中包含的字段先做判断，直接过滤掉不满足条件的记录，减少回表次数。

因此，MySQL5.6版本之后，选出包含“小”字后，顺表过滤age=28，,所以就只需一次回表。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125915.png)

### 九、 面试官考点之大表添加索引

**面试官：** 如果一张表数据量级是千万级别以上的，那么，给这张表添加索引，你需要怎么做呢？

**解析：** 我们需要知道一点，给表添加索引的时候，是会对表加锁的。如果不谨慎操作，有可能出现生产事故的。可以参考以下方法：

1. 先创建一张跟原表A数据结构相同的新表B。

2. 在新表B添加需要加上的新索引。

3. 把原表A数据导到新表B

4. rename新表B为原表的表名A，原表A换别的表名；

### 十、聚集索引和非聚集索引的区别

1. 聚集索引一个表只能有一个，而非聚集索引一个表可以存在多个
1. 聚集索引存储记录是物理上连续存在，而非聚集索引是逻辑上的连续，物理存储并不连续
1. 聚集索引：物理存储按照索引排序；聚集索引是一种索引组织形式，索引的键值逻辑顺序决定了表数据行的物理存储顺序
1. 非聚集索引：物理存储不按照索引排序；非聚集索引则就是普通索引了，仅仅只是对数据列创建相应的索引，不影响整个表的物理存储顺序.
1. 索引是通过二叉树的数据结构来描述的，我们可以这么理解聚簇索引：索引的叶节点就是数据节点。而非聚簇索引的叶节点仍然是索引节点，只不过有一个指针指向对应的数据块。

### 十一、最左前缀匹配原则及它的原因

最左前缀匹配原则，是一个非常重要的原则，可以通过以下这几个特性来理解。

- 对于联合索引，MySQL 会一直向右匹配直到遇到范围查询（> ， < ，between，like）就停止匹配。比如 a = 3 and b = 4 and c > 5 and d = 6，如果建立的是（a,b,c,d）这种顺序的索引，那么 d 是用不到索引的，但是如果建立的是 （a,b,d,c）这种顺序的索引的话，那么就没问题，而且 a，b，d 的顺序可以随意调换。
- = 和 in 可以乱序，比如 a = 3 and b = 4 and c = 5 建立 （a，b，c）索引可以任意顺序。
- 如果建立的索引顺序是 （a，b）那么直接采用 where b = 5 这种查询条件是无法利用到索引的，这一条最能体现最左匹配的特性。

#### 最左匹配原则的成因

MySQL 建立联合索引的规则是这样的，它会首先根据联合索引中最左边的、也就是第一个字段进行排序，在第一个字段排序的基础上，再对联合索引中后面的第二个字段进行排序，依此类推。

综上，第一个字段是绝对有序的，从第二个字段开始是无序的，这就解释了为什么直接使用第二字段进行条件判断用不到索引了（从第二个字段开始，无序，无法走 B+ Tree 索引）！这也是 MySQL 在联合索引中强调最左前缀匹配原则的原因。

### 十二、什么时候不该使用索引？

1. 表的数据量特别小的时候。
   如果一张表，只有极少的几条数据，那么不使用索引，是直接全表扫描，速度也是极快的。
   但是如果使用索引，为什么反而慢了呢？
   因为才用索引去访问记录的话，首先要去访问索引表，然后再通过索引表访问数据表，一般情况下索引表与我们的数据表不在同一个数据块，这种情况下需要去往返两个数据块，两次，而不使用索引，一次就可以完成，所以在数据量小的时候，反而不使用索引更快。

2. 数据的差异性很小
   什么叫做数据的差异性很小呢，就是你一个字段，只有2个值，比方说，性别，这只有两个数据的字段，就算建了索引，那么数据库的索引二叉树级别也很少，大多都是平级的，这样的二叉树跟全表查询差别不大。

3. 频繁更新的字段
   如果一个字段频繁更新，还使用索引，会加大数据库的工作量，所以不建议使用。

4. 查询字段中含有IS NULL /IS NOT NULL/ like ‘%输入符% / <> 等条件
   如果查询条件中，有这些条件，就算该字段有索引，也不会使用索引，一定要注意。

5. 对于那些定义为text, image和bit数据类型的列不应该增加索引。这是因为，这些列的数据量要么相当大，要么取值很少。

### 十三、怎么验证 mysql 的索引是否满足需求？

EXPLAIN 

### 十四、为什么建议使用自增主键

我们都知道表的主键一般都要使用自增 id，不建议使用业务 id ，是因为使用自增 id 可以避免页分裂。这个其实可以相当于一个结论，你都可以直接记住这个结论就可以了。

我这里也稍微解释一下页分裂，mysql （注意本文讲的 mysql 默认为InnoDB 引擎）底层数据结构是 B+ 树，所谓的索引其实就是一颗 B+ 树，一个表有多少个索引就会有多少颗 B+ 树，mysql 中的数据都是按顺序保存在 B+ 树上的（所以说索引本身是有序的）。

然后 mysql 在底层又是以数据页为单位来存储数据的，一个数据页大小默认为 16k，当然你也可以自定义大小，也就是说如果一个数据页存满了，mysql 就会去申请一个新的数据页来存储数据。

如果主键为自增 id 的话，mysql 在写满一个数据页的时候，直接申请另一个新数据页接着写就可以了。

如果主键是非自增 id，为了确保索引有序，mysql 就需要将每次插入的数据都放到合适的位置上。

当往一个快满或已满的数据页中插入数据时，新插入的数据会将数据页写满，mysql 就需要申请新的数据页，并且把上个数据页中的部分数据挪到新的数据页上。

这就造成了页分裂，这个大量移动数据的过程是会严重影响插入效率的。

其实对主键 id 还有一个小小的要求，在满足业务需求的情况下，尽量使用占空间更小的主键 id，因为普通索引的叶子节点上保存的是主键 id 的值，如果主键 id 占空间较大的话，那将会成倍增加 mysql 空间占用大小。





## 性能调优

### 一、mysql 问题排查都有哪些手段？

使用 show processlist 命令查看当前所有连接信息。

使用 explain 命令查询 SQL 语句执行计划。

开启慢查询日志，查看慢查询的 SQL。



## 实际项目应用

### 一、项目 MySQL 的数据量和并发量有多大？

评注:此题为走向题，你的回答不同，后面问题走向就变了。

关于容量:单表行数超过 500 万行或者单表容量超过2GB，此时就要答分库分表的中间件了！那后面题目的走向就变为mycat、sharing-jdbc等分库分表中间件的底层原理了！

关于并发量:如果并发数过1200，此时就要答利用MQ或者redis等中间件，作为补偿措施，而不能直接操作数据库。那后面的题目走向就是redis、mq的原理了!

介于面试者还是一个应届生，我斗胆猜测面试者是这么答的

回答:数据量估计就三四百万吧，并发量就五六百左右！











