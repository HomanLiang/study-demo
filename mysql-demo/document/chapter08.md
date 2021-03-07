[toc]

# MySQL 索引

> 声明：如果没有说明具体的数据库和存储引擎，默认指的是MySQL中的InnoDB存储引擎

## 基础知识
### Mysql的基本存储结构是页(记录都存在页里边)
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111301.png)
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111305.png)

- 各个数据页可以组成一个双向链表

- 而每个数据页中的记录又可以组成一个单向链表
    - 每个数据页都会为存储在它里边儿的记录生成一个页目录，在通过主键查找某条记录的时候可以在页目录中使用二分法快速定位到对应的槽，然后再遍历该槽对应分组中的记录即可快速找到指定的记录
    - 以其他列(非主键)作为搜索条件：只能从最小记录开始依次遍历单链表中的每条记录。

所以说，如果我们写 `select * from user where username='Java3y'` 这样没有进行任何优化的sql语句，默认会这样做：
- 定位到记录所在的页
    - 需要遍历双向链表，找到所在的页

- 从所在的页内中查找相应的记录
    - 由于不是根据主键查询，只能遍历所在页的单链表了

很明显，在数据量很大的情况下这样查找会很慢！



## 索引提高检索速度

其实就是将无序的数据变成有序(相对)：
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111501.png)
要找到id为8的记录简要步骤：
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111602.png)
很明显的是：没有用索引我们是需要遍历双向链表来定位对应的页，现在通过“目录”就可以很快地定位到对应的页上了！

其实底层结构就是B+树，B+树作为树的一种实现，能够让我们很快地查找出对应的记录。



## 索引降低增删改的速度

B+树是平衡树的一种。
> 平衡树：它是一棵空树或它的左右两个子树的高度差的绝对值不超过1，并且左右两个子树都是一棵平衡二叉树。

如果一棵普通的树在极端的情况下，是能退化成链表的(树的优点就不复存在了)
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111701.png)
B+树是平衡树的一种，是不会退化成链表的，树的高度都是相对比较低的(基本符合矮矮胖胖(均衡)的结构)【这样一来我们检索的时间复杂度就是O(logn)】！从上一节的图我们也可以看见，建立索引实际上就是建立一颗B+树。

- B+树是一颗平衡树，如果我们对这颗树增删改的话，那肯定会破坏它的原有结构。
- 要维持平衡树，就必须做额外的工作。正因为这些额外的工作开销，导致索引会降低增删改的速度



## 哈希索引

除了B+树之外，还有一种常见的是哈希索引。

哈希索引就是采用一定的哈希算法，把键值换算成新的哈希值，检索时不需要类似B+树那样从根节点到叶子节点逐级查找，只需一次哈希算法即可立刻定位到相应的位置，速度非常快。
- 本质上就是把键值换算成新的哈希值，根据这个哈希值来定位
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111703.png)
看起来哈希索引很牛逼啊，但其实哈希索引有好几个局限(根据他本质的原理可得)：
- 哈希索引也没办法利用索引完成排序
- 不支持最左匹配原则
- 在有大量重复键值情况下，哈希索引的效率也是极低的---->哈希碰撞问题。
- 不支持范围查询



## InnoDB支持哈希索引吗？

主流的还是使用B+树索引比较多，对于哈希索引，InnoDB是自适应哈希索引的（hash索引的创建由InnoDB存储引擎引擎自动优化创建，我们干预不了）！
![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111801.png)



## 聚集和非聚集索引

简单概括：
- 聚集索引就是以主键创建的索引
- 非聚集索引就是以非主键创建的索引

区别：
- 聚集索引在叶子节点存储的是表中的数据
- 非聚集索引在叶子节点存储的是主键和索引列
- 使用非聚集索引查询出数据时，拿到叶子上的主键再去查到想要查找的数据。(拿到主键再查找这个过程叫做回表)

非聚集索引也叫做二级索引，不用纠结那么多名词，将其等价就行了~

非聚集索引在建立的时候也未必是单列的，可以多个列来创建索引。
-  此时就涉及到了哪个列会走索引，哪个列不走索引的问题了(最左匹配原则-->后面有说)
-  创建多个单列(非聚集)索引的时候，会生成多个索引树(所以过多创建索引会占用磁盘空间)
![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111902.png)

在创建多列索引中也涉及到了一种特殊的索引-->覆盖索引
- 我们前面知道了，如果不是聚集索引，叶子节点存储的是主键+列值
- 最终还是要“回表”，也就是要通过主键再查找一次。这样就会比较慢
- 覆盖索引就是把要查询出的列和索引是对应的，不做回表操作！

比如说：
- 现在我创建了索引 (username,age)，在查询数据的时候： select username, age from user where username='Java3y' and age=20。
- 很明显地知道，我们上边的查询是走索引的，并且，要查询出的列在叶子节点都存在！所以，就不用回表了~
- 所以，能使用覆盖索引就尽量使用吧~



## 索引最左匹配原则

最左匹配原则：
- 索引可以简单如一个列 (a)，也可以复杂如多个列 (a,b,c,d)，即联合索引。
- 如果是联合索引，那么key也由多个列组成，同时，索引只能用于查找key是否存在（相等），遇到范围查询 (>、<、between、like左匹配)等就不能进一步匹配了，后续退化为线性查找。
- 因此，列的排列顺序决定了可命中索引的列数。

例子：
- 如有索引 (a,b,c,d)，查询条件 a=1 and b=2 and c >3 and d=4，则会在每个节点依次命中a、b、c，无法命中d。(c已经是范围查询了，d肯定是排不了序了)

为什么能命中c？
举个简单例子： `select * from user where age>30; `如果在age列创建索引，那你说会走索引吗？



## =、in自动优化顺序

不需要考虑=、in等的顺序，mysql会自动优化这些条件的顺序，以匹配尽可能多的索引列。

例子：
如有索引 (a,b,c,d)，查询条件 `c>3 and b=2 and a=1 and d<4`与 `a=1 and c>3 and b=2 and d<4`等顺序都是可以的，MySQL会自动优化为 `a=1 and b=2 and c>3 and d<4`，依次命中a、b、c。



## 索引总结

索引在数据库中是一个非常重要的知识点！上面谈的其实就是索引最基本的东西，要创建出好的索引要顾及到很多的方面：
- 最左前缀匹配原则。这是非常重要、非常重要、非常重要（重要的事情说三遍）的原则，MySQL会一直向右匹配直到遇到范围查询 `（>,<,BETWEEN,LIKE）`就停止匹配。
- 尽量选择区分度高的列作为索引，区分度的公式是` COUNT(DISTINCT col)/COUNT(*)`。表示字段不重复的比率，比率越大我们扫描的记录数就越少。
- 索引列不能参与计算，尽量保持列“干净”。比如， `FROM_UNIXTIME(create_time)='2016-06-06' `就不能使用索引，原因很简单，B+树中存储的都是数据表中的字段值，但是进行检索时，需要把所有元素都应用函数才能比较，显然这样的代价太大。所以语句要写成 ： `create_time=UNIX_TIMESTAMP('2016-06-06')`。
- 尽可能的扩展索引，不要新建立索引。比如表中已经有了a的索引，现在要加（a,b）的索引，那么只需要修改原来的索引即可。
- 单个多列组合索引和多个单列索引的检索查询效果不同，因为在执行SQL时，MySQL只能使用一个索引，会从多个单列索引中选择一个限制最为严格的索引(经指正，在MySQL5.0以后的版本中，有“合并索引”的策略，翻看了《高性能MySQL 第三版》，书作者认为：还是应该建立起比较好的索引，而不应该依赖于“合并索引”这么一个策略)。
- “合并索引”策略简单来讲，就是使用多个单列索引，然后将这些结果用“union或者and”来合并起来



## 问题

### SELECT COUNT(*) 会造成全表扫描？
**前言**

上篇 SQL 进阶技巧（下) 中提到使用以下 sql 会导致慢查询
```
SELECT COUNT(*) FROM SomeTable
SELECT COUNT(1) FROM SomeTable
```
原因是会造成全表扫描，有位读者说这种说法是有问题的，实际上针对无 where_clause 的 COUNT(*)，MySQL 是有优化的，优化器会选择成本最小的辅助索引查询计数，其实反而性能最高，这位读者的说法对不对呢

针对这个疑问，我首先去生产上找了一个千万级别的表使用  EXPLAIN 来查询了一下执行计划
```
EXPLAIN SELECT COUNT(*) FROM SomeTable
```
结果如下
![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307112005.png)
如图所示: 发现确实此条语句在此例中用到的并不是主键索引，而是辅助索引，实际上在此例中我试验了，不管是 COUNT(1)，还是 COUNT(*)，MySQL 都会用成本最小的辅助索引查询方式来计数，也就是使用 COUNT(*) 由于 MySQL 的优化已经保证了它的查询性能是最好的！随带提一句，COUNT(*)是 SQL92 定义的标准统计行数的语法，并且效率高，所以请直接使用COUNT(*)查询表的行数！

所以这位读者的说法确实是对的。但有个前提，在 MySQL 5.6 之后的版本中才有这种优化。

那么这个成本最小该怎么定义呢，有时候在 WHERE 中指定了多个条件，为啥最终 MySQL 执行的时候却选择了另一个索引，甚至不选索引？

本文将会给你答案，本文将会从以下两方面来分析
- SQL 选用索引的执行成本如何计算
- 实例说明

**SQL 选用索引的执行成本如何计算**

就如前文所述，在有多个索引的情况下， 在查询数据前，MySQL 会选择成本最小原则来选择使用对应的索引，这里的成本主要包含两个方面。

- IO 成本: 即从磁盘把数据加载到内存的成本，默认情况下，读取数据页的 IO 成本是 1，MySQL 是以页的形式读取数据的，即当用到某个数据时，并不会只读取这个数据，而会把这个数据相邻的数据也一起读到内存中，这就是有名的程序局部性原理，所以 MySQL 每次会读取一整页，一页的成本就是 1。所以 IO 的成本主要和页的大小有关
- CPU 成本：将数据读入内存后，还要检测数据是否满足条件和排序等 CPU 操作的成本，显然它与行数有关，默认情况下，检测记录的成本是 0.2。

**实例说明**

为了根据以上两个成本来算出使用索引的最终成本，我们先准备一个表（以下操作基于 MySQL 5.7.18）
```
CREATE TABLE `person` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `score` int(11) NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `name_score` (`name`(191),`score`),
  KEY `create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
这个表除了主键索引之外，还有另外两个索引, name_score 及 create_time。然后我们在此表中插入 10 w 行数据，只要写一个存储过程调用即可，如下:
```
CREATE PROCEDURE insert_person()
begin
    declare c_id integer default 1;
    while c_id<=100000 do
    insert into person values(c_id, concat('name',c_id), c_id+100, date_sub(NOW(), interval c_id second));
    set c_id=c_id+1;
    end while;
end
```
插入之后我们现在使用 EXPLAIN 来计算下统计总行数到底使用的是哪个索引
```
EXPLAIN SELECT COUNT(*) FROM person
```
![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307112101.png)
从结果上看它选择了 create_time 辅助索引，显然 MySQL 认为使用此索引进行查询成本最小，这也是符合我们的预期，使用辅助索引来查询确实是性能最高的！

我们再来看以下 SQL 会使用哪个索引
```
SELECT * FROM person WHERE NAME >'name84059' AND create_time>'2020-05-23 14:39:18' 
```
![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307112202.png)
用了全表扫描！理论上应该用 name_score 或者 create_time 索引才对，从 WHERE 的查询条件来看确实都能命中索引，那是否是使用 SELECT * 造成的回表代价太大所致呢，我们改成覆盖索引的形式试一下

```
SELECT create_time FROM person WHERE NAME >'name84059' AND create_time > '2020-05-23 14:39:18' 
```
结果 MySQL 依然选择了全表扫描！这就比较有意思了，理论上采用了覆盖索引的方式进行查找性能肯定是比全表扫描更好的，为啥 MySQL 选择了全表扫描呢，既然它认为全表扫描比使用覆盖索引的形式性能更好，那我们分别用这两者执行来比较下查询时间吧
```
-- 全表扫描执行时间: 4.0 ms
SELECT create_time FROM person WHERE NAME >'name84059' AND create_time>'2020-05-23 14:39:18' 

-- 使用覆盖索引执行时间: 2.0 ms
SELECT create_time FROM person force index(create_time) WHERE NAME >'name84059' AND create_time>'2020-05-23 14:39:18' 
```
从实际执行的效果看使用覆盖索引查询比使用全表扫描执行的时间快了一倍！说明 MySQL 在查询前做的成本估算不准！我们先来看看 MySQL 做全表扫描的成本有多少。

前面我们说了成本主要 IO 成本和 CPU 成本有关，对于全表扫描来说也就是分别和聚簇索引占用的页面数和表中的记录数。执行以下命令
```
SHOW TABLE STATUS LIKE 'person'
```
![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307112205.png)
可以发现

1. 行数是 100264，我们不是插入了 10 w 行的数据了吗，怎么算出的数据反而多了，其实这里的计算是估算，也有可能这里的行数统计出来比 10 w 少了，估算方式有兴趣大家去网上查找，这里不是本文重点，就不展开了。得知行数，那我们知道 CPU 成本是 100264 * 0.2 = 20052.8。
1. 数据长度是 5783552，InnoDB 每个页面的大小是 16 KB，可以算出页面数量是 353。

也就是说全表扫描的成本是 20052.8 + 353 =  20406。

这个结果对不对呢，我们可以用一个工具验证一下。在 MySQL 5.6 及之后的版本中，我们可以用 optimizer trace 功能来查看优化器生成计划的整个过程 ，它列出了选择每个索引的执行计划成本以及最终的选择结果，我们可以依赖这些信息来进一步优化我们的 SQL。

optimizer_trace 功能使用如下
```
SET optimizer_trace="enabled=on";
SELECT create_time FROM person WHERE NAME >'name84059' AND create_time > '2020-05-23 14:39:18';
SELECT * FROM information_schema.OPTIMIZER_TRACE;
SET optimizer_trace="enabled=off";
```
执行之后我们主要观察使用 name_score，create_time 索引及全表扫描的成本。

先来看下使用 name_score 索引执行的的预估执行成本:
```
{
    "index": "name_score",
    "ranges": [
      "name84059 <= name"
    ],
    "index_dives_for_eq_ranges": true,
    "rows": 25372,
    "cost": 30447
}
```
可以看到执行成本为 30447，高于我们之前算出来的全表扫描成本：20406。所以没选择此索引执行

注意：这里的 30447 是查询二级索引的 IO 成本和 CPU 成本之和，再加上回表查询聚簇索引的 IO 成本和 CPU 成本之和。

再来看下使用 create_time 索引执行的的预估执行成本:
```
{
    "index": "create_time",
    "ranges": [
      "0x5ec8c516 < create_time"
    ],
    "index_dives_for_eq_ranges": true,
    "rows": 50132,
    "cost": 60159,
    "cause": "cost"
}
```
可以看到成本是 60159,远大于全表扫描成本 20406，自然也没选择此索引。

再来看计算出的全表扫描成本：
```
{
    "considered_execution_plans": [
      {
        "plan_prefix": [
        ],
        "table": "`person`",
        "best_access_path": {
          "considered_access_paths": [
            {
              "rows_to_scan": 100264,
              "access_type": "scan",
              "resulting_rows": 100264,
              "cost": 20406,
              "chosen": true
            }
          ]
        },
        "condition_filtering_pct": 100,
        "rows_for_plan": 100264,
        "cost_for_plan": 20406,
        "chosen": true
      }
    ]
}
```