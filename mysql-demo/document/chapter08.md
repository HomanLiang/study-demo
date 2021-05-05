[toc]

# MySQL 索引

> 声明：如果没有说明具体的数据库和存储引擎，默认指的是MySQL中的InnoDB存储引擎

## 1.数据页

### 1.1.Mysql的基本存储结构是页(记录都存在页里边)

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

### 1.2.数据页长啥样？

数据页长下面这样：

![image-20210313005635837](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210313005635.png)

### 1.3.什么是数据区？

在MySQL的设定中，同一个表空间内的一组连续的数据页为一个extent（区），默认区的大小为1MB，页的大小为16KB。16*64=1024，也就是说一个区里面会有64个连续的数据页。连续的256个数据区为一组数据区。

于是我们可以画出这张图：

![image-20210313005700047](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210313005700.png)

从直观上看，其实不用纳闷为啥MySQL按照这样的方式组织存储在磁盘上的数据。

这就好比你搞了个Java的封装类描述一类东西，然后再相应的给它加上一些功能方法，或者用golang封装struct去描述一类对象。最终的目的都是为了方便、管理、控制。

约定好了数据的组织方式，那MySQL的作用不就是：按照约定数据规则将数据文件中的数据加载进内存，然后展示给用户看，以及提供其他能力吗？

### 1.4.数据页分裂问题

假设你现在已经有两个数据页了。并且你正在往第二个数据页中写数据。

关于B+Tree，你肯定知道B+Tree中的叶子结点之间是通过双向链表关联起来的。

在InnoDB索引的设定中，要求主键索引是递增的，这样在构建索引树的时候才更加方便。你可以脑补一下。如果按1、2、3...递增的顺序给你这些数。是不是很方便的构建一棵树。然后你可以自由自在的在这棵树上玩二分查找。

那假设你自定义了主键索引，而且你自定义的这个主键索引并不一定是自增的。

那就有可能出现下面这种情况 如下图：

![image-20210313005725929](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210313005726.png)

> 假设上图中的id就是你自定义的不会自增的主键

然后随着你将数据写入。就导致后一个数据页中的所有行并不一定比前一个数据页中的行的id大。

这时就会触发页分裂的逻辑。

页分裂的目的就是保证：后一个数据页中的所有行主键值比前一个数据页中主键值大。

经过分裂调整，可以得到下面的这张图。

![image-20210313005751602](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210313005751.png)



## 2.B树和B+树

### 2.1.B树

B-Tree，即B树或者B-树。
![image-20210310211359381](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312211453.png)
**一棵 m 阶的 B 树，需要满足下列条件：**

1. 定义任意非叶子结点最多只有M个儿子，且M>2；
2. 根结点的儿子数为[2, M]；
3. 除根结点以外的非叶子结点的儿子数为[M/2, M]，向上取整；
4. 非叶子结点的关键字个数=儿子数-1；
5. 所有叶子结点位于同一层；
6. k个关键字把节点拆成k+1段，分别指向k+1个儿子，同时满足查找树的大小关系。

**B树的一些特点：**

1. 关键字集合分布在整颗树中；
2. 任何一个关键字出现且只出现在一个结点中；
3. 搜索有可能在非叶子结点结束；
4. 其搜索性能等价于在关键字全集内做一次二分查找；

从上图可以看出，key 为 50 的节点就在第一层，B-树只需要一次磁盘 IO 即可完成查找。所以说B-树的查询最好时间复杂度是 O(1)。

### 2.2.B+树

![image-20210310211614907](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310211614907.png)

**m阶的b+树的特征：**

1. 有n棵子树的非叶子结点中含有n个关键字（b树是n-1个），这些关键字不保存数据，只用来索引，所有数据都保存在叶子节点（b树是每个关键字都保存数据）;
2. 所有的叶子结点中包含了全部关键字的信息，及指向含这些关键字记录的指针，且叶子结点本身依关键字的大小自小而大顺序链接;
3. 所有的非叶子结点可以看成是索引部分，结点中仅含其子树中的最大（或最小）关键字;
4. 通常在b+树上有两个头指针，一个指向根结点，一个指向关键字最小的叶子结点;
5. 同一个数字会在不同节点中重复出现，根节点的最大元素就是b+树的最大元素。

由于B+树所有的 data 域都在根节点，所以查询 key 为 50的节点必须从根节点索引到叶节点，时间复杂度固定为 O(log n)。

**B+树的基本结构：**

这里不对B+树做精确定义，直接给出一个B+树的示意图并做一些解释说明。

![img](https://img2020.cnblogs.com/blog/1128201/202010/1128201-20201008182000370-274030447.png)

B+树是一颗`多路平衡查找树`，所有节点称为`页`，页就是一个数据块，里面可以放数据，页是固定大小的，在InnoDB中是16kb。页里边的数据是一些key值，n个key可以划分为n+1个区间，每个区间有一个指向下级节点的指针，每个页之间以双向链表的方式连接，一层中的key是`有序`的。以磁盘块1这个页为例，他有两个key，17,35，划分了三个区间（-无穷,17) p1,[17, 35) p2, [35, +无穷] p3三个区间，也称扇出为3. p1指向的下级节点里边的key都是比17小的；p2指向的下级节点里边的key大于等于17，小于35；p3指向的下级节点里边的key都大于等于35。

**在B+树查找数据的流程：**

例如要在上边这棵树查找28，首先定位到磁盘1，通过`二分`的方式找到他属于哪个区间，发现是p2，从而定位到磁盘块3，在磁盘块3的key里边做二分查找，找到p2, 定位到磁盘块8，然后二分找到28这个key。对于数据库来说，查找一个key最终一定会定位到叶子节点，因为只有叶子节点才包含行记录或者主键key。

**插入节点与删除节点：**

这里不对其详细流程做介绍，给大家安利一个工具：https://www.cs.usfca.edu/~galles/visualization/BPlusTree.html， 这个工具可以以动画方式演示B+树插入和删除的过程，非常直观，大家可以去动手试试看。如图所示：

![image-20210310215814742](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310215814742.png)

值得注意的是，插入节点时，可能存在`页分裂`的情况，删除节点时可能存在`页合并`的情况。页的分裂就是指当一个页容纳不了新的key时，分为多个页的过程。页合并是指当删除一个节点使得页中的key的数量少到一定程度时与相邻的页合在一起成为新的页。并非一个页满插入就会发生页分裂，会优先通过类似`旋转`的方式进行调整，这样可以避免浪费空间。

下图演示一种最简单的页分裂情况，假设一页只能放3个key，插入efg时，叶子页放了了，所以分裂为了两个页，并且增加了一层。

![image-20210310215835919](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310215835919.png)

数据库索引的B+树的显著特点是`高扇出`，也就是说一个页存放的数据多，这样的好处是树的`高度小`，大概在2到4层，`高度越小，查找的IO次数越少`。

### 2.3.总结

![image-20210310211700352](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310211700352.png)
通过以上的叙述和上面这张图，基本可以知道B树和B+树的区别：

- B+ 树中的节点不存储数据，只是索引，而 B 树中的节点存储数据；
- B 树中的叶子节点并不需要链表来串联。

从定义上来说，B+树叶节点两两相连可大大增加区间访问性，可使用在范围查询等，而B-树每个节点 key 和 data 在一起，无法区间查找。

事实上，例如oracle、MongoDB这样使用B树的数据，肯定是可以范围查询的，因为他们使用的B树也是在叶子节点存储行的位置信息，数据在逻辑上是连续的。其实，B+树就是 B树的改进版。


## 3.索引有哪些类型

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210312125233.png)

**数据结构维度**

- B+树索引：所有数据存储在叶子节点，复杂度为O(logn)，适合范围查询。
- 哈希索引：适合等值查询，检索效率高，一次到位。
- 全文索引：MyISAM和InnoDB中都支持使用全文索引，一般在文本类型char,text,varchar类型上创建。
- R-Tree索引：用来对GIS数据类型创建SPATIAL索引

**物理存储维度**

- 聚集索引：聚集索引就是以主键创建的索引，在叶子节点存储的是表中的数据。
- 非聚集索引：非聚集索引就是以非主键创建的索引，在叶子节点存储的是主键和索引列。

**逻辑维度**

- 主键索引：一种特殊的唯一索引，不允许有空值。
- 普通索引：MySQL中基本索引类型，允许空值和重复值。
- 联合索引：多个字段创建的索引，使用时遵循最左前缀原则。
- 唯一索引：索引列中的值必须是唯一的，但是允许为空值。
- 空间索引：MySQL5.7之后支持空间索引，在空间索引这方面遵循OpenGIS几何数据模型规则。



## 4.B+树索引

### 4.1.MyISAM引擎索引实现

在MyISAM中，主索引和辅助索引（Secondary key）在结构上没有任何区别，只是主索引要求key是唯一的，而辅助索引的key可以重复。

#### 4.1.1.主键索引

![image-20210310212046061](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310212046061.png)
MyISAM引擎使用B+树作为索引结构，叶节点的data域存放的是数据记录的地址。

#### 4.1.2.辅助索引

![image-20210310212224244](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310212224244.png)

同样也是一棵B+树，data域保存数据记录的地址。因此，MyISAM中索引检索的算法为首先按照B+树搜索算法搜索索引，如果指定的Key存在，则取出其data域的值，然后以data域的值为地址，再回表查询需要的数据。

MyISAM的索引方式也叫做“非聚集”的，之所以这么称呼是为了与InnoDB的聚集索引区分（可以发现和Oracle的B树类似）

### 4.2.InnoDB引擎索引实现

Innodb是索引组织表。在InnoDB中，表数据文件本身就是按B+树组织的一个索引结构（就是索引组织表），这棵树的叶节点data域保存了完整的数据记录。

聚簇索引的每一个叶子节点都包含了主键值、事务ID、用于事务和MVCC的回滚指针以及**所有的剩余列**。假设我们以col1为主键，则下图是一个InnoDB表的聚簇索引（主键索引）（Primary key）示意。

#### 4.2.1.主键索引

![image-20210310212253528](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310212253528.png)
因为InnoDB的数据文件本身要按主键聚集（聚集索引），所以InnoDB要求表必须有主键（MyISAM可以没有），如果没有显式指定，则MySQL会自动选择一个可以唯一标识数据记录的列作为主键，如果不存在这种列，则MySQL自动为InnoDB表生成一个隐含字段作为主键，这个字段长度为6个字节，类型为长整型。

#### 4.2.2.辅助索引

![image-20210310212317844](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310212317844.png)

首先检索辅助索引获得主键，然后用主键到主索引中检索获得记录。

与MyISAM不同的是，InnoDB的二级索引和聚簇索引很不相同。**InnoDB的二级索引的叶子节点存储的不是行号（行指针），而是主键列**。这种策略的缺点是二级索引需要两次索引查找，第一次在二级索引中查找主键，第二次在聚簇索引中通过主键查找需要的数据行。

画外音：可以通过我们前面提到过的**索引覆盖**来避免回表查询，这样就只需要一次回表查询，对于InnoDB而言，就是只需要一次索引查找就可以查询到需要的数据记录，因为需要的数据记录已经被索引到二级索引中，直接就可以找到。

好处是InnoDB在移动行时无需更新一级索引中的这个”指针“，因为主键是不会改变的，但是行指针却会改变。



### 4.3.为什么要用B+树

1. **为什么不用有序数组**

   有序数组可以通过二分的方法查找，查找时间复杂度为O(logn). 他的缺点是`插入和删除操作代价太高`，例如删除0位置，那么1到n-1位置的数据都要往前移动，代价O(n)

2. **为什么不用Hash表**

   存储引擎内部是有用到Hash表的，这里说的不用Hash表是我们自己建索引时通常不会去建立Hash索引（InnoDB也是不支持的）

   Hash表是一种查找效率很高的结构，例如我们Java中的HashMap，基本可以认为他的插入、查询、删除都是O(1)的。

   Hash表的底层是一个`数组`，插入数据时对数据的hashCode对数组长度`取模`，确定他在数组中的位置，放到数组里边。当然这里可能存在你要放的位置被占用了，这个叫`碰撞`，或者Hash冲突，此时可以用拉链法解决，具体就是在冲突的位置建一个链表。如下图所示，BCD三个数据在1位置发生冲突，因此在这里形成了链表。Hash表中的查找也很容易，先按插入的方式找到待查找数据在的位置，然后看这个位置有没有，有就找到了。

   ![image-20210310215431005](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310215431005.png)

   Hash表的一个缺点在于`对范围查询的支持不友好`，比如要查[F，K]之间数据，那么就需要将F到K之间的所有值枚举出来计算hashcode，一个一个去hash表查。而且他是无序的，`对于order by不友好`。因此除非你的查询就只有等值查询，否则不可能使用Hash表做索引。

3. **为什么不用搜索二叉树**

   不管是不经调整的搜索二叉树，还是AVL树、红黑树都是搜索二叉树，他的特点是，对于任意一个节点，他的左孩子（如果有）小于自己，右孩子（如果有）大于自己。

   搜索二叉树的缺点在于，他的`高度会随着节点数增加而增加`。我们知道，数据库索引是很大的，不可能直接装进内存，根节点可能是直接在内存的，其他节点存放在磁盘上，查找的时候`每往下找一层就需要读一次磁盘`。读磁盘的`效率是比较低的`，因此需要减少读磁盘的次数，那么也就需要减少树的高度。搜索二叉树当数据很多时，高度就会很高，那么磁盘IO次数就会很多，效率低下。

   另外，数据库是以页的形式存储的，InnoDB存储引擎默认一页16K，一页可以看成一个节点 ，二叉树一个结点只能存储一个一个数据.假如索引字段为int 也就是一个4字节的数字要占16k的空间，极大的`浪费了空间`。

4. **B+树有什么特点**

   - `高扇出`，高扇出使得一个节点可以存放更多的数据，整棵树会更加`矮胖`。InnoDB中一棵树的高度在2-4层，这意味着一次查询只需要1-3次磁盘IO
   - 非叶子节点只存放key值（也就是列值），这使得一页可以存更多的数据，这是高扇出的保证


## 5.哈希索引

除了B+树之外，还有一种常见的是哈希索引。

哈希索引就是采用一定的哈希算法，把键值换算成新的哈希值，检索时不需要类似B+树那样从根节点到叶子节点逐级查找，只需一次哈希算法即可立刻定位到相应的位置，速度非常快。

本质上就是把键值换算成新的哈希值，根据这个哈希值来定位
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111703.png)
看起来哈希索引很牛逼啊，但其实哈希索引有好几个局限(根据他本质的原理可得)：

- 哈希索引也没办法利用索引完成排序
- 不支持最左匹配原则
- 在有大量重复键值情况下，哈希索引的效率也是极低的---->哈希碰撞问题。
- 不支持范围查询

### 5.1.InnoDB支持哈希索引吗？

主流的还是使用B+树索引比较多，对于哈希索引，InnoDB是自适应哈希索引的（hash索引的创建由InnoDB存储引擎引擎自动优化创建，我们干预不了）！
![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111801.png)


## 6.聚集索引和非聚集索引

### 6.1.聚集索引

`聚集索引（Clustered index)` 也叫聚簇索引、主键索引。他的显著特点是`其叶子节点包含行数据（表中的一行）`，没错，InnoDB存储引擎表数据存在索引中，表是`索引组织表`。显然表数据不可能有多份，但是必须有一份，所以聚集索引在一张表有且仅有一个。

**什么样的列会建立聚集索引？**

`主键列`，也就是你指定一个表的主键就会创建聚集索引。InnoDB中的表必有主键列，如果没有指定主键，那么会选择一个非空唯一列作为主键，，否则隐式创建一个列作为主键。

假设有如下一张表，a为主键，假设一页只能放三个数据

| 编号 | a    | b    | c    |
| ---- | ---- | ---- | ---- |
| 1    | 1    | a    | 11   |
| 2    | 2    | b    | 12   |
| 3    | 3    | c    | 13   |
| 4    | 4    | d    | 14   |

我们看一看他的聚集索引大概是张什么样的

![image-20210310222100891](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222100891.png)

其中r1到r4分别表示编号从1到4的行

使用聚集索引的好处：

1. `查询快`，等值和范围查询都快，使用索引必然查询效率会高，使用聚集索引比非聚集索引查询更快，因为他能直接在叶子节点找到数据，而不需要回表（后文说明）
2. 基于主键（聚集索引）的`排序快`，数据本身就是根据主键排序的

下面我们创建一个表看一下

建表语句和初始化数据如下：

```sql
-- a为主键
create table t (
    a int not null,
    b varchar(600),
    c int not null,
    primary key(a)
) engine=INNODB;

insert into t values 
(1,'a',11),
(2, 'b', 12),
(3, 'c', 13),
(4, 'd', 14);
```

![image-20210310222158357](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222158357.png)

第一个查询我们在a列上做等值查询

第二个在c上做等值查询。从key列可以看到，第一个查询用到了聚集索引，第二个由于c没有索引，所以全表扫描

第三个查询对a做排序,第四个查询对c列做排序。发现对主键的排序不会用filesort.



### 6.2.非聚集索引

`非聚集索引（Secondary Index)`也叫辅助索引、二级索引、非主键索引。非主键列创建的索引就是这种索引。他的显著特点是`叶子节点不包括完整的行数据`（如果包括，这是一件多么恐怖的事啊！），而是包含行记录对应的`主键key`。

还是以上边的表为例，我们在b列创建一个索引。

![image-20210310222251939](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222251939.png)

注意我们只用了b的前10个字符创建索引，所以你能看到Sub_part这列显示的为10。

此时，idx_b这个索引对应B+树类似下边这种形式

![image-20210310222306270](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222306270.png)

可以看到叶子节点中的1,2,3,4其实是主键里边的值

**在非聚集索引的查找过程是：**

先在非聚集索引树找到指定key，同时能得到主键key，拿着主键key到聚集索引里找到对应的行。

拿着主键key到聚集索引找行的过程称为`回表`，回表有可能避免，详见后文的覆盖索引。

使用非聚集索引的好处：

1. `占用的空间相比聚集索引小`，因为他的叶子节点并不包含完整的行数据，只包含主键key
2. `查询快`，这和聚集索引是类似的，但是效率可能比聚集索引低，因为存在回表过程

**缺点：**

回表问题，就是要查两棵索引树才能找到数据，当然后面会提到并不是所有用非聚集索引查询都有回表过程。

**下边来看几个查询计划**

![image-20210310222357121](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222357121.png)

第一个 key为idx_b, 表明用到了非聚集索引，extra是mysql5.6后做的一个优化，Index Push Down优化，简言之就是在使用索引查询时直接通过where条件过滤掉了不符合条件的数据。

第二个演示了按非聚集索引的列做排序的情况，发现会用到filesort，因为没法直接根据索引排序了，需要回表。

第三个和第二个类似，但是他只选择了b这个列，发现没有用filesort.因为不用回表，这个其实就是用到了覆盖索引。



### 6.3.聚集和非聚集索引比较

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



## 7.联合索引

联合索引就是索引`包含多个列`的情况，此时的B+树每个key包含了几个部分，而不是单一值。

继续上边的例子，我们建立b，c列上的联合索引。

![image-20210310222434636](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222434636.png)

这个索引树可能的形式如下：

![image-20210310222452843](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222452843.png)

这个图画的不太好，其实第二个列在一页里边也可以是无序的

每个key有两个列值组成，叶子节点也是包含了主键key，可见这个联合索引是非聚集索引。当然主键索引也可以包含多个列，自然也可以是联合索引。

**联合索引的作用：**

1. 对左边的列做查询排序都可以用到这个索引（最左原则）

    ```sql
    -- 这里可以假设没有idx_b这个索引
    select * from t where b='a';
    select * from t where b='a' and c=11;
    ```

2. 左边的列做等值查询，对后边的列做排序友好，因为后边的已经是排序的

    ```sql
    -- 这里可以假设没有idx_b这个索引
    select * from t where b='a' order by  c;
    ```

3. 让索引包含更多数据，走覆盖索引，一旦放到一个列被索引，那么索引树必包含这个列的数据

	对于字符串类型的列，也是满足最左前缀原则，like '%a' 不能命中索引，like 'a%'就可以。

	注意下边这个语句用不到索引

    ```sql
    select * from t where c=11;
    ```

**下面看几个查询计划：**

先来看一看索引情况

![image-20210310222638468](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222638468.png)

可以看到我们在b，c两列建立了idx_b_c的联合索引

![image-20210310222654942](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222654942.png)

1号查询，条件包含最左列，b列，命中索引

2号查询，条件不包含最左列，key列显示为NULL，未命中索引，type为ALL，是全表扫描

3号查询，对最左列做等值，然后右列做排序，命中了索引

4号查询，没有命中索引，用到了filesort

通过这四个查询我们能够了解到联合索引的最左原则是怎么回事了，结合前面提到的联合索引的树结构，这个原则是理所当然的。



## 8.覆盖索引

覆盖的意思就是`包含`的意思，覆盖索引就是说`索引里包含了你需要的数据`。

聚集索引直接包含了行数据，因此是覆盖索引，但是一般不这么说。非聚集索引索引数据里边有索引列的列值（这不完全对，后面有说明）。覆盖索引不是一种新的索引结构，只是`恰好你要查的数据就在索引树里有`，这样就`不用回表查询`了（非聚集索引叶子节点只有主键key，和索引列值，如果需要其他列值，就需要在通过聚集索引查一次，也就是要走回表）。`如果使用了覆盖索引，那么查询计划的Extra列为Using index`.

**看几个具体的例子：**

目前的索引情况如下

![image-20210310222735861](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222735861.png)

一些执行计划

![image-20210310222756497](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222756497.png)

c的索引包含c列和主键列的值，所以第一第二个查询不需要回表，使用了覆盖索引。

c的索引不包含b列，所以当c列索引查b列时就需要回表了

第四个查询，b列上有索引，索引里边有b列的值，要查的也是b列，索引覆盖了要查询的列，所以也使用了覆盖索引。

需要注意的是，不要忘记了`主键列在所有索引都可以被覆盖到`。

测试发现一个奇怪的现象，这里分享给大伙儿，一个列的varchar给超过767的长度，然后在上边建索引，会有一个自动的截取。如图所示：

![image-20210310222859904](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310222859904.png)

大家可以思考一下，如果你的索引key只是列的一部分，比如，有一个字段为varchar(100), 你的索引只包含前50个字符，这个时候能不能走覆盖索引？



## 9.索引的优缺点

### 9.1.索引提高检索速度

其实就是将无序的数据变成有序(相对)：
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111501.png)

要找到id为8的记录简要步骤：

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111602.png)

很明显的是：没有用索引我们是需要遍历双向链表来定位对应的页，现在通过“目录”就可以很快地定位到对应的页上了！

其实底层结构就是B+树，B+树作为树的一种实现，能够让我们很快地查找出对应的记录。



### 9.2.索引降低增删改的速度

B+树是平衡树的一种。
> 平衡树：它是一棵空树或它的左右两个子树的高度差的绝对值不超过1，并且左右两个子树都是一棵平衡二叉树。

如果一棵普通的树在极端的情况下，是能退化成链表的(树的优点就不复存在了)
![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307111701.png)
B+树是平衡树的一种，是不会退化成链表的，树的高度都是相对比较低的(基本符合矮矮胖胖(均衡)的结构)【这样一来我们检索的时间复杂度就是O(logn)】！从上一节的图我们也可以看见，建立索引实际上就是建立一颗B+树。

- B+树是一颗平衡树，如果我们对这颗树增删改的话，那肯定会破坏它的原有结构。
- 要维持平衡树，就必须做额外的工作。正因为这些额外的工作开销，导致索引会降低增删改的速度



## 10.索引下推

### 10.1.低版本操作

其实在 Mysql 5.6 版本之前是没有索引下推这个功能的，从 5.6 版本后才加上了这个优化项。所以在引出索引下推前还是先回顾下没有这个功能时是怎样一种处理方式。

我们以一个真实例子来进行讲解。

在这里有张用户表 user，记录着用户的姓名，性别，身高，年龄等信息。表中 id 是自增主键，(name,sex) 是联合索引。在这里用 1 表示男，2 表示女。现在需要查找所有姓王的男性信息。

SQL 实现起来很简单：

![image-20210310234726372](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310234726372.png)

但是它的实现原理是什么呢？

根据联合索引最左前缀原则，我们在非主键索引树上找到第一个满足条件的值时，通过叶子节点记录的主键值再回到主键索引树上查找到对应的行数据，再对比是否为当前所要查找的性别。

整个原理可以用下边的图进行表示。

![image-20210310234744953](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310234744953.png)

看到了吧，低版本中需要每条数据都进行回表，增加了树的搜索次数。如果遇到所要查找的数据量很大的话，性能必然有所缺失。

### 10.2.高版本操作

知道了痛点，那么怎么解决。很简单，只有符合条件了再进行回表。结合我们的例子来说就是当满足了性别 sex = 1 了，再回表查找。这样原本可能需要进行回表查找 4 次，现在可能只需要 2 次就可以了。

![image-20210310234829556](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310234829556.png)

所以本质来说，索引下推就是只有符合条件再进行回表，对索引中包含的字段先进行判断，不符合条件的跳过。减少了不必要的回表操作。

### 10.3.总结

**回表操作**

- 当所要查找的字段不在非主键索引树上时，需要通过叶子节点的主键值去主键索引上获取对应的行数据，这个过程称为回表操作。

**索引下推**

- 索引下推主要是减少了不必要的回表操作。对于查找出来的数据，先过滤掉不符合条件的，其余的再去主键索引树上查找。



## 11.建索引的几大原则

1. **选择唯一性索引**

   唯一性索引的值是唯一的，可以更快速的通过该索引来确定某条记录。例如，学生表中学号是具有唯一性的字段。为该字段建立唯一性索引可以很快的确定某个学生的信息。如果使用姓名的话，可能存在同名现象，从而降低查询速度。

2. **为经常需要排序、分组和联合操作的字段建立索引**

   经常需要ORDER BY、GROUP BY、DISTINCT和UNION等操作的字段，排序操作会浪费很多时间。如果为其建立索引，可以有效地避免排序操作。

3. **为常作为查询条件的字段建立索引**

   如果某个字段经常用来做查询条件，那么该字段的查询速度会影响整个表的查询速度。因此，为这样的字段建立索引，可以提高整个表的查询速度。

4. **限制索引的数目**

   索引的数目不是越多越好。每个索引都需要占用磁盘空间，索引越多，需要的磁盘空间就越大。修改表时，对索引的重构和更新很麻烦。越多的索引，会使更新表变得很浪费时间。

5. **尽量使用数据量少的索引**

   如果索引的值很长，那么查询的速度会受到影响。例如，对一个CHAR(100)类型的字段进行全文检索需要的时间肯定要比对CHAR(10)类型的字段需要的时间要多。

6. **尽量使用前缀来索引**

   如果索引字段的值很长，最好使用值的前缀来索引。例如，TEXT和BLOG类型的字段，进行全文检索会很浪费时间。如果只检索字段的前面的若干个字符，这样可以提高检索速度。

7. **删除不再使用或者很少使用的索引**

   表中的数据被大量更新，或者数据的使用方式被改变后，原有的一些索引可能不再需要。数据库管理员应当定期找出这些索引，将它们删除，从而减少索引对更新操作的影响。

8. **最左前缀匹配原则，非常重要的原则**

   mysql会一直向右匹配直到遇到范围查询(>、<、between、like)就停止匹配，比如a 1=”” and=”” b=”2” c=”“> 3 and d = 4 如果建立(a,b,c,d)顺序的索引，d是用不到索引的，如果建立(a,b,d,c)的索引则都可以用到，a,b,d的顺序可以任意调整。

9. **=和in可以乱序**

   比如a = 1 and b = 2 and c = 3 建立(a,b,c)索引可以任意顺序，mysql的查询优化器会帮你优化成索引可以识别的形式

10. **尽量选择区分度高的列作为索引**

    区分度的公式是count(distinct col)/count(*)，表示字段不重复的比例，比例越大我们扫描的记录数越少，唯一键的区分度是1，而一些状态、性别字段可能在大数据面前区分度就 是0，那可能有人会问，这个比例有什么经验值吗？使用场景不同，这个值也很难确定，一般需要join的字段我们都要求是0.1以上，即平均1条扫描10条 记录

11. **索引列不能参与计算，保持列“干净”**

    比如from_unixtime(create_time) = ’2014-05-29’就不能使用到索引，原因很简单，b+树中存的都是数据表中的字段值，但进行检索时，需要把所有元素都应用函数才能比较，显然成本 太大。所以语句应该写成create_time = unix_timestamp(’2014-05-29’);

12. **尽量的扩展索引，不要新建索引**

    比如表中已经有a的索引，现在要加(a,b)的索引，那么只需要修改原来的索引即可

注意：选择索引的最终目的是为了使查询的速度变快。上面给出的原则是最基本的准则，但不能拘泥于上面的准则。读者要在以后的学习和工作中进行不断的实践。根据应用的实际情况进行分析和判断，选择最合适的索引方式。



## 12.Cardinality

使用 `show index from 表名` 时， 可以看到有一个Cardinality列，这个列是衡量我们`索引有效性`的方式。他的含义是索引列中不重复的行数，Cardinality除以表行数称为`索引的选择性`，`选择性越高越好`，选择性小于30%通常认为这个索引建的不好。

Cardinality是一个`采样估计值`，会随机选择若干页计算平均不同记录的个数，然后乘上页数量。所以可能你每次查到的值不一样，即使你的表没有更新。

这个值并不是每一次表更新都会计算的，他会有自己的一个计算策略。

执行如下语句会导致这个值的重新计算, 当然也可以配置为不进行计算：

1. analyze table
2. show table status
3. show index

## 13.全文索引

 MySQL 从 5.7.6 版本开始，MySQL就内置了ngram全文解析器，用来支持中文、日文、韩文分词。在 MySQL 5.7.6 版本之前，全文索引只支持英文全文索引，不支持中文全文索引，需要利用分词器把中文段落预处理拆分成单词，然后存入数据库。本篇文章测试的时候，采用的 Mysql 5.7.6 ，InnoDB数据库引擎。

### 13.1.全文解析器ngram

ngram就是一段文字里面连续的n个字的序列。ngram全文解析器能够对文本进行分词，每个单词是连续的n个字的序列。 例如，用ngram全文解析器对“你好世界”进行分词:

```javascript
n=1: '你', '好', '世', '界' 
n=2: '你好', '好世', '世界' 
n=3: '你好世', '好世界' 
n=4: '你好世界'
```

MySQL 中使用全局变量 ngram_token_size 来配置 ngram 中 n 的大小，它的取值范围是1到10，默认值是 2。通常ngram_token_size设置为要查询的单词的最小字数。如果需要搜索单字，就要把ngram_token_size设置为 1。在默认值是 2 的情况下，搜索单字是得不到任何结果的。因为中文单词最少是两个汉字，推荐使用默认值 2。

咱们看一下Mysql默认的ngram_token_size大小：

```javascript
show variables like 'ngram_token_size'
```

![g13vsz91iu](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505103820.png)

ngram_token_size 变量的两种设置方式：

- 启动mysqld命令时指定 

    ```javascript
    mysqld --ngram_token_size=2
    ```

- 修改mysql配置文件 

    ```javascript
    [mysqld] 
    ngram_token_size=2
    ```

### 13.2.全文索引

以某文书数据为例，新建数据表 t_wenshu ，并且针对文书内容字段创建全文索引，导入10w条测试数据。

![nqtcojorh6](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505103920.png)

- 建表时创建全文索引

    ```javascript
    CREATE TABLE `t_wenshu` (
      `province` varchar(255) DEFAULT NULL,
      `caseclass` varchar(255) DEFAULT NULL,
      `casenumber` varchar(255) DEFAULT NULL,
      `caseid` varchar(255) DEFAULT NULL,
      `types` varchar(255) DEFAULT NULL,
      `title` varchar(255) DEFAULT NULL,
      `content` longtext,
      `updatetime` varchar(255) DEFAULT NULL,
      FULLTEXT KEY `content` (`content`) WITH PARSER `ngram`
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
    ```

- 通过 alter table 方式

    ```javascript
    ALTER TABLE t_wenshu ADD FULLTEXT INDEX content_index (content) WITH PARSER ngram;
    ```

- 通过 create index 方式

    ```javascript
    CREATE FULLTEXT INDEX content_index ON t_wenshu (content) WITH PARSER ngram;
    ```

### 13.3.检索模式

**自然语言检索**

（IN NATURAL LANGUAGE MODE）自然语言模式是 MySQL 默认的全文检索模式。自然语言模式不能使用操作符，不能指定关键词必须出现或者必须不能出现等复杂查询。

**布尔检索**

（IN BOOLEAN MODE）剔除一半匹配行以上都有的词，例如，每行都有this这个词的话，那用this去查时，会找不到任何结果，这在记录条数特别多时很有用，原因是数据库认为把所有行都找出来是没有意义的，这时，this几乎被当作是stopword(中断词)；布尔检索模式可以使用操作符，可以支持指定关键词必须出现或者必须不能出现或者关键词的权重高还是低等复杂查询。

```javascript
   ● IN BOOLEAN MODE的特色： 
      ·不剔除50%以上符合的row。 
      ·不自动以相关性反向排序。 
      ·可以对没有FULLTEXT index的字段进行搜寻，但会非常慢。 
      ·限制最长与最短的字符串。 
      ·套用Stopwords。

   ● 搜索语法规则：
     +   一定要有(不含有该关键词的数据条均被忽略)。 
     -   不可以有(排除指定关键词，含有该关键词的均被忽略)。 
     >   提高该条匹配数据的权重值。 
     <   降低该条匹配数据的权重值。
     ~   将其相关性由正转负，表示拥有该字会降低相关性(但不像-将之排除)，只是排在较后面权重值降低。 
     *   万用字，不像其他语法放在前面，这个要接在字符串后面。 
     " " 用双引号将一段句子包起来表示要完全相符，不可拆字。
```

**查询扩展检索**

注释：（WITH QUERY EXPANSION）由于查询扩展可能带来许多非相关性的查询，谨慎使用！

### 13.4.检索查询

- 查询 content 中包含“盗窃罪”的记录，查询语句如下

    ```javascript
    select caseid,content, MATCH ( content) AGAINST ('盗窃罪') as score from t_wenshu where MATCH ( content) AGAINST ('盗窃罪' IN NATURAL LANGUAGE MODE)
    ```
    
    ![run88a8651](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505104150.png)

- 查询 content 中包含“寻衅滋事”的记录，查询语句如下

    ```javascript
    select caseid,content, MATCH ( content) AGAINST ('寻衅滋事') as score from t_wenshu where MATCH ( content) AGAINST ('寻衅滋事' IN NATURAL LANGUAGE MODE) ;
    ```
    
    ![xnnc1ahu7p](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505104329.png)
    
- 单个汉字，查询 content 中包含“我”的记录，查询语句如下

    ```javascript
    select caseid,content, MATCH ( content) AGAINST ('我') as score from t_wenshu where MATCH ( content) AGAINST ('我' IN NATURAL LANGUAGE MODE) ;
    ```

    ![nukd7jmr2s](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505104343.png)

    备注：因为设置的全局变量 ngram_token_size 的值为 2。如果想查询单个汉字，需要在配置文件 my.ini 中修改 ngram_token_size = 1 ，并重启 mysqld 服务，此处不做尝试了。

- 查询字段 content 中包含 “危险驾驶”和“寻衅滋事”的语句如下：

    ```javascript
    select caseid,content, MATCH (content) AGAINST ('+危险驾驶 +寻衅滋事') as score from t_wenshu where MATCH (content) AGAINST ('+危险驾驶 +寻衅滋事' IN BOOLEAN MODE);
    ```

    ![p6g7ldw9g4](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505104403.png)

- 查询字段 content 中包含 “危险驾驶”，但不包含“寻衅滋事”的语句如下：

    ```javascript
    select caseid,content, MATCH (content) AGAINST ('+危险驾驶 -寻衅滋事') as score from t_wenshu where MATCH (content) AGAINST ('+危险驾驶 -寻衅滋事' IN BOOLEAN MODE);
    ```

    ![7qtjzyexpj](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505104419.png)

- 查询字段 conent 中包含“危险驾驶”或者“寻衅滋事”的语句如下：

    ```javascript
    select caseid,content, MATCH (content) AGAINST ('危险驾驶 寻衅滋事') as score from t_wenshu where MATCH (content) AGAINST ('危险驾驶 寻衅滋事' IN BOOLEAN MODE);
    ```

    ![ngmw9e1x3g](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505104432.png)

### 13.5.总结

- 使用 Mysql 全文索引之前，搞清楚各版本支持情况；
- 全文索引比 like + % 快 N 倍，但是可能存在精度问题；
- 如果需要全文索引的是大量数据，建议先添加数据，再创建索引； 
- 对于中文，可以使用 MySQL 5.7.6 之后的版本，或者 Sphinx、Lucene 等第三方的插件；
- MATCH()函数使用的字段名，必须要与创建全文索引时指定的字段名一致，且只能是同一个表的字段不能跨表； 



## X.常见问题

### X.1.SELECT COUNT(*) 会造成全表扫描？
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

如图所示: 发现确实此条语句在此例中用到的并不是主键索引，而是辅助索引，实际上在此例中我试验了，不管是 COUNT(1)，还是 `COUNT(*)`，MySQL 都会用成本最小的辅助索引查询方式来计数，也就是使用 COUNT(*) 由于 MySQL 的优化已经保证了它的查询性能是最好的！随带提一句，COUNT(*)是 SQL92 定义的标准统计行数的语法，并且效率高，所以请直接使用 `COUNT(*)` 查询表的行数！

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



### X.2.为什么我使用了索引，查询还是慢？

#### X.2.1.案例剖析

言归正传，为了实验，我创建了如下表：

```
CREATE TABLE `T`(
	`id` int(11) NOT NULL,
	`a` int(11) DEFAUT NULL,
	PRIMARY KEY(`id`),
	KEY `a`(`a`)
) ENGINE=InnoDB;
```

该表有三个字段，其中用id是主键索引，a是普通索引。

首先SQL判断一个语句是不是慢查询语句，用的是语句的执行时间。他把语句执行时间跟 `long_query_time` 这个系统参数作比较，如果语句执行时间比它还大，就会把这个语句记录到慢查询日志里面，这个参数的默认值是10秒。当然在生产上，我们不会设置这么大，一般会设置1秒，对于一些比较敏感的业务，可能会设置一个比1秒还小的值。

语句执行过程中有没有用到表的索引，可以通过explain一个语句的输出结果来看KEY的值不是NULL。

我们看下 `explain select * from t;` 的KEY结果是NULL

![image-20210310235217295](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235217295.png)

`explain select * from t where id=2;`的KEY结果是PRIMARY，就是我们常说的使用了主键索引

![image-20210310235241876](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235241876.png)

`explain select a from t;`的KEY结果是a，表示使用了a这个索引。

![image-20210310235257544](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235257544.png)

虽然后两个查询的KEY都不是NULL，但是最后一个实际上扫描了整个索引树a。

假设这个表的数据量有100万行，图二的语句还是可以执行很快，但是图三就肯定很慢了。如果是更极端的情况，比如，这个数据库上CPU压力非常的高，那么可能第2个语句的执行时间也会超过 `long_query_time` ，会进入到慢查询日志里面。

所以我们可以得出一个结论：**是否使用索引和是否进入慢查询之间并没有必然的联系。使用索引只是表示了一个SQL语句的执行过程，而是否进入到慢查询是由它的执行时间决定的，而这个执行时间，可能会受各种外部因素的影响。换句话来说，使用了索引你的语句可能依然会很慢。**

#### X.2.2.全索引扫描的不足

那如果我们在更深层次的看这个问题，其实他还潜藏了一个问题需要澄清，就是什么叫做使用了索引。

我们都知道，InnoDB是索引组织表，所有的数据都是存储在索引树上面的。比如上面的表t，这个表包含了两个索引，一个主键索引和一个普通索引。在InnoDB里，数据是放在主键索引里的。如图所示：

![image-20210310235402806](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235402806.png)

可以看到数据都放在主键索引上，如果从逻辑上说，所有的InnoDB表上的查询，都至少用了一个索引，所以现在我问你一个问题，如果你执行`select from t where id>0`，你觉得这个语句有用上索引吗？

![image-20210310235632205](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235632205.png)

我们看上面这个语句的explain的输出结果显示的是PRIMARY。其实从数据上你是知道的，这个语句一定是做了全面扫描。但是优化器认为，这个语句的执行过程中，需要根据主键索引，定位到第1个满足ID>0的值，也算用到了索引。

所以即使explain的结果里写的KEY不是NULL，实际上也可能是全表扫描的，因此InnoDB里面只有一种情况叫做没有使用索引，那就是从主键索引的最左边的叶节点开始，向右扫描整个索引树。

也就是说，没有使用索引并不是一个准确的描述。

- 你可以用全表扫描来表示一个查询遍历了整个主键索引树；
- 也可以用全索引扫描，来说明像 `select a from t;` 这样的查询，他扫描了整个普通索引树；
- 而 `select * from t where id=2` 这样的语句，才是我们平时说的使用了索引。他表示的意思是，我们使用了索引的快速搜索功能，并且有效的减少了扫描行数。

#### X.2.3.索引的过滤性要足够好

根据以上解剖，我们知道全索引扫描会让查询变慢，接下来就要来谈谈索引的过滤性。

假设你现在维护了一个表，这个表记录了中国14亿人的基本信息，现在要查出所有年龄在10~15岁之间的姓名和基本信息，那么你的语句会这么写，`select * from t_people where age between 10 and 15`。

你一看这个语句一定要在age字段上开始建立索引了，否则就是个全面扫描，但是你会发现，在你建立索引以后，这个语句还是执行慢，因为满足这个条件的数据可能有超过1亿行。

我们来看看建立索引以后，这个表的组织结构图：

![image-20210310235647871](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235647871.png)

这个语句的执行流程是这样的:

- 从索引上用树搜索，取到第1个age等于10的记录，得到它的主键id的值，根据id的值去主键索引取整行的信息，作为结果集的一部分返回；
- 在索引age上向右扫描，取下一个id的值，到主键索引上取整行信息，作为结果集的一部分返回；
- 重复上面的步骤，直到碰到第1个age大于15的记录；

你看这个语句，虽然他用了索引，但是他扫描超过了1亿行。所以你现在知道了，当我们在讨论有没有使用索引的时候，其实我们关心的是扫描行数。

**对于一个大表，不止要有索引，索引的过滤性还要足够好。**

像刚才这个例子的age，它的过滤性就不够好，在设计表结构的时候，我们要让所有的过滤性足够好，也就是区分度足够高。

#### X.2.4.回表的代价

那么过滤性好了，是不是表示查询的扫描行数就一定少呢？

**我们再来看一个例子：**

如果你的执行语句是 `select * from t_people where name='张三' and age=8`

t_people表上有一个索引是姓名和年龄的联合索引，那这个联合索引的过滤性应该不错，可以在联合索引上快速找到第1个姓名是张三，并且年龄是8的小朋友，当然这样的小朋友应该不多，因此向右扫描的行数很少，查询效率就很高。

但是查询的过滤性和索引的过滤性可不一定是一样的，如果现在你的需求是查出所有名字的第1个字是张，并且年龄是8岁的所有小朋友，你的语句会怎么写呢？

你的语句要怎么写？很显然你会这么写：`select * from t_people where name like '张%' and age=8;`

在MySQL5.5和之前的版本中，这个语句的执行流程是这样的:
![image-20210310235702238](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235702238.png)

- 首先从联合索引上找到第1个年龄字段是张开头的记录，取出主键id，然后到主键索引树上，根据id取出整行的值；
- 判断年龄字段是否等于8，如果是就作为结果集的一行返回，如果不是就丢弃。
- 在联合索引上向右遍历，并重复做回表和判断的逻辑，直到碰到联合索引树上名字的第1个字不是张的记录为止。

我们把根据id到主键索引上查找整行数据这个动作，称为回表。你可以看到这个执行过程里面，最耗费时间的步骤就是回表，假设全国名字第1个字是张的人有8000万，那么这个过程就要回表8000万次，在定位第一行记录的时候，只能使用索引和联合索引的最左前缀，最称为最左前缀原则。

你可以看到这个执行过程，它的回表次数特别多，性能不够好，有没有优化的方法呢？

在MySQL5.6版本，引入了 `index condition pushdown` 的优化。我们来看看这个优化的执行流程：

![image-20210310235724015](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210310235724015.png)

- 首先从联合索引树上，找到第1个年龄字段是张开头的记录，判断这个索引记录里面，年龄的值是不是8，如果是就回表，取出整行数据，作为结果集的一部分返回，如果不是就丢弃；
- 在联合索引树上，向右遍历，并判断年龄字段后，根据需要做回表，直到碰到联合索引树上名字的第1个字不是张的记录为止；

这个过程跟上面的差别，是在遍历联合索引的过程中，将年龄等于8的条件下推到所有遍历的过程中，减少了回表的次数，假设全国名字第1个字是张的人里面，有100万个是8岁的小朋友，那么这个查询过程中在联合索引里要遍历8000万次，而回表只需要100万次。

#### X.2.5.虚拟列

可以看到这个优化的效果还是很不错的，但是这个优化还是没有绕开最左前缀原则的限制，因此在联合索引你还是要扫描8000万行，那有没有更进一步的优化方法呢？

我们可以考虑把名字的第一个字和age来做一个联合索引。这里可以使用MySQL5.7引入的虚拟列来实现。对应的修改表结构的SQL语句:

```
alter table t_people add name_first varchar(2) generated (left(name,1)),add index(name_first,age);
```

我们来看这个SQL语句的执行效果:

```
CREATE TABLE `t_people`(
`id` int(11) DEFAULT NULL,
`name` varchar(20) DEFAUT NULL,
`name_first` varchar(2) GENERATED ALWAYS AS (left(`name`,1)) VIRTUAL,KEY `name_first`(`name_first`,'age')
) ENGINE=InnoDB DEFAULT CHARSET=utf8; 
```

首先他在people上创建一个字段叫name_first的虚拟列，然后给name_first和age上创建一个联合索引，并且，让这个虚拟列的值总是等于name字段的前两个字节，虚拟列在插入数据的时候不能指定值，在更新的时候也不能主动修改，它的值会根据定义自动生成，在name字段修改的时候也会自动修改。

有了这个新的联合索引，我们在找名字的第1个字是张，并且年龄为8的小朋友的时候，这个SQL语句就可以这么写：`select * from t_people where name_first='张' and age=8`

这样这个语句的执行过程，就只需要扫描联合索引的100万行，并回表100万次，这个优化的本质是我们创建了一个更紧凑的索引，来加速了查询的过程。

#### X.2.6.总结

本文给你介绍了索引的基本结构和一些查询优化的基本思路，你现在知道了，使用索引的语句也有可能是慢查询，我们的查询优化的过程，往往就是减少扫描行数的过程。

慢查询归纳起来大概有这么几种情况：

- 全表扫描
- 全索引扫描
- 索引过滤性不好
- 频繁回表的开销





### X.3.MySQL索引使用策略及优化实例

MySQL的优化主要分为结构优化（Scheme optimization）和查询优化（Query optimization）。本章讨论的高性能索引策略主要属于`结构优化`范畴。

#### X.3.1.示例数据库

为了讨论索引策略，需要一个数据量不算小的数据库作为示例。本文选用MySQL官方文档中提供的示例数据库之一：employees。这个数据库关系复杂度适中，且数据量较大。下图是这个数据库的E-R关系图（引用自MySQL官方手册）：
![image-20210311000336928](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000336928.png)

MySQL官方文档中关于此数据库的页面为http://dev.mysql.com/doc/employee/en/employee.html (目前不可访问)。里面详细介绍了此数据库，并提供了下载地址和导入方法，如果有兴趣导入此数据库到自己的MySQL可以参考文中内容。

#### X.3.2.最左前缀原理与相关优化

高效使用索引的首要条件是知道什么样的查询会使用到索引，这个问题和B+Tree中的`“最左前缀原理”`有关，下面通过例子说明最左前缀原理。

这里先说一下联合索引的概念。在上文中，我们都是假设索引只引用了单个的列，实际上，`MySQL中的索引可以以一定顺序引用多个列，这种索引叫做联合索引`，一般的，一个联合索引是一个有序元组`<a1, a2, …, an>`，其中各个元素均为数据表的一列，实际上要严格定义索引需要用到关系代数，但是这里我不想讨论太多关系代数的话题，因为那样会显得很枯燥，所以这里就不再做严格定义。另外，单列索引可以看成联合索引元素数为1的特例。

以employees.titles表为例，下面先查看其上都有哪些索引：

![image-20210311000440987](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000440987.png)
从结果中可以到titles表的主索引为`<emp_no, title, from_date>`，还有一个辅助索引`<emp_no>`。为了避免多个索引使事情变复杂（MySQL的SQL优化器在多索引时行为比较复杂），这里我们将辅助索引drop掉：

```
ALTER TABLE employees.titles DROP INDEX emp_no;
```

这样就可以专心分析索引PRIMARY的行为了。

**① 全列匹配**

![image-20210311000511100](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000511100.png)

很明显，当按照索引中所有列进行精确匹配（这里精确匹配指“=”或“IN”匹配）时，索引可以被用到。这里有一点需要注意，理论上索引对顺序是敏感的，但是由于MySQL的查询优化器会自动调整where子句的条件顺序以使用适合的索引，例如我们将where中的条件顺序颠倒：

![image-20210311000534857](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000534857.png)

会发现效果是一样的。

------

**② 最左前缀匹配**

![image-20210311000553071](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000553071.png)

当查询条件精确匹配索引的左边连续一个或几个列时，如`<emp_no>或<emp_no, title>`，索引可以被用到，但是只能用到一部分，即条件所组成的最左前缀。上面的查询从分析结果看用到了PRIMARY索引，但是key_len为4，说明只用到了索引的第一列前缀。

**③ 查询条件用到了索引中列的精确匹配，但是中间某个条件未提供**

![image-20210311000638264](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000638264.png)

此时索引使用情况和情况二相同，因为title未提供，所以查询只用到了索引的第一列，而后面的`from_date`虽然也在索引中，但是由于title不存在而无法和左前缀连接，因此需要对结果进行扫描过滤`from_date`（这里由于emp_no唯一，所以不存在扫描）。如果想让`from_date`也使用索引而不是where过滤，可以增加一个辅助索引`<emp_no, from_date>`，此时上面的查询会使用这个索引。

除此之外，还可以使用一种称之为`“隔离列”`的优化方法，将`emp_no与from_date之间的“坑”`填上。首先我们看下title一共有几种不同的值：

![image-20210311000701410](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000701410.png)

只有7种。在这种成为“坑”的列值比较少的情况下，可以考虑用“IN”来填补这个“坑”从而形成最左前缀：

![image-20210311000724130](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000724130.png)

这次key_len为59，说明索引被用全了，但是从type和rows看出IN实际上执行了一个range查询，这里检查了7个key。看下两种查询的性能比较：

![image-20210311000743166](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000743166.png)

`“填坑”`后性能提升了一点。如果经过emp_no筛选后余下很多数据，则后者性能优势会更加明显。当然，如果title的值很多，用填坑就不合适了，必须建立辅助索引。

------

**④ 查询条件没有指定索引第一列**

![image-20210311000806381](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000806381.png)
由于不是最左前缀，索引这样的查询显然用不到索引。

**⑤ 匹配某列的前缀字符串**

![image-20210311000827187](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000827187.png)

如果通配符%不出现在开头，则可以用到索引，但根据具体情况不同可能只会用其中一个前缀。即只有`XXX%`会使用到索引，`%XXX%`或`%XXX`都不会使用到索引。

**⑥ 范围查询**

![image-20210311000856753](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000856753.png)

范围列可以用到索引（必须是最左前缀），但是范围列后面的列无法用到索引。同时，索引最多用于一个范围列，因此如果查询条件中有两个范围列则无法全用到索引。

![image-20210311000942157](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311000942157.png)

可以看到索引对第二个范围索引无能为力。这里特别要说明MySQL一个有意思的地方，那就是仅用explain可能无法区分范围索引和多值匹配，因为在type中这两者都显示为range。同时，用了“between”并不意味着就是范围查询，例如下面的查询：

![image-20210311001025873](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001025873.png)

看起来是用了两个范围查询，但作用于emp_no上的“BETWEEN”实际上相当于“IN”，也就是说emp_no实际是多值精确匹配。可以看到这个查询用到了索引全部三个列。因此在MySQL中要谨慎地区分多值匹配和范围匹配，否则会对MySQL的行为产生困惑。

------

**⑦ 查询条件中含有函数或表达式**

很不幸，如果查询条件中含有函数或表达式，则MySQL不会为这列使用索引（虽然某些在数学意义上可以使用）。例如：

![image-20210311001057019](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001057019.png)

虽然这个查询和情况五中功能相同，但是由于使用了函数left，则无法为title列应用索引，而情况五中用LIKE则可以。再如：

![image-20210311001113208](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001113208.png)

显然这个查询等价于查询emp_no为10001的函数，但是由于查询条件是一个表达式，MySQL无法为其使用索引。看来MySQL还没有智能到自动优化常量表达式的程度，因此在写查询语句时尽量避免表达式出现在查询中，而是先手工私下代数运算，转换为无表达式的查询语句。

------

#### X.3.3.索引选择性与前缀索引

既然索引可以加快查询速度，那么是不是只要是查询语句需要，就建上索引？答案是否定的。因为索引虽然加快了查询速度，但索引也是有代价的：索引文件本身要消耗存储空间，同时索引会加重插入、删除和修改记录时的负担。另外，MySQL在运行时也要消耗资源维护索引，因此索引并不是越多越好。

**一般两种情况下不建议建索引：**

- 第一种情况是表记录比较少，例如一两千条甚至只有几百条记录的表，没必要建索引，让查询做全表扫描就好了。至于多少条记录才算多，我个人的经验是以2000作为分界线，记录数不超过 2000可以考虑不建索引，超过2000条可以酌情考虑索引。
- 另一种不建议建索引的情况是索引的选择性较低。所谓索引的选择性(Selectivity)，是指不重复的索引值(也叫基数，Cardinality)与表记录数(#T)的比值：

```
Index Selectivity = Cardinality / #T
```

显然选择性的取值范围为(0, 1]，选择性越高的索引价值越大，这是由B+Tree的性质决定的。

例如，上文用到的employees.titles表，如果title字段经常被单独查询，是否需要建索引，我们看一下它的选择性：
![image-20210311001209607](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001209607.png)
title的选择性不足0.0001（精确值为0.00001579），所以实在没有什么必要为其单独建索引。

------

**前缀索引**

有一种与索引选择性有关的索引优化策略叫做前缀索引，就是用列的前缀代替整个列作为索引key，当前缀长度合适时，可以做到既使得前缀索引的选择性接近全列索引，同时因为索引key变短而减少了索引文件的大小和维护开销。

下面以employees.employees表为例介绍前缀索引的选择和使用。

从下图可以看到employees表只有一个索引`<emp_no>`，那么如果我们想按名字搜索一个人，就只能全表扫描了：

![image-20210311001252916](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001252916.png)

如果频繁按名字搜索员工，这样显然效率很低，因此我们可以考虑建索引。有两种选择，建`<first_name>或<first_name, last_name>`，看下两个索引的选择性：
![image-20210311001313213](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001313213.png)
`<first_name>`显然选择性太低，`<first_name, last_name>`选择性很好，但是`first_name和last_name`加起来长度为30，有没有兼顾长度和选择性的办法？可以考虑用`first_name和last_name的前几个字符`建立索引，例如`<first_name, left(last_name, 3)>`，看看其选择性：
![image-20210311001336057](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001336057.png)
选择性还不错，但离0.9313还是有点距离，那么把last_name前缀加到4：
![image-20210311001349792](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001349792.png)
这时选择性已经很理想了，而这个索引的长度只有18，比`<first_name, last_name>`短了接近一半，我们把这个前缀索引 建上：

```
ALTER TABLE employees.employees
ADD INDEX `first_name_last_name4` (first_name, last_name(4));
12
```

此时再执行一遍按名字查询，比较分析一下与建索引前的结果：
![image-20210311001406101](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001406101.png)
性能的提升是显著的，查询速度提高了120多倍。

前缀索引兼顾索引大小和查询速度，但是其缺点是不能用于ORDER BY和GROUP BY操作，也不能用于Covering index（即当索引本身包含查询所需全部数据时，不再访问数据文件本身）。

------

#### X.3.4.InnoDB的主键选择与插入优化

在使用InnoDB存储引擎时，如果没有特别的需要，请永远使用一个与业务无关的自增字段作为主键。

上文讨论过InnoDB的索引实现，InnoDB使用聚集索引，数据记录本身被存于主索引（一颗B+Tree）的叶子节点上。这就要求同一个叶子节点内（大小为一个内存页或磁盘页）的各条数据记录按主键顺序存放，因此每当有一条新的记录插入时，MySQL会根据其主键将其插入适当的节点和位置，如果页面达到装载因子（InnoDB默认为15/16），则开辟一个新的页（节点）。

如果表使用自增主键，那么每次插入新的记录，记录就会顺序添加到当前索引节点的后续位置，当一页写满，就会自动开辟一个新的页。如下图所示：

![image-20210311001455283](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001455283.png)

这样就会形成一个紧凑的索引结构，近似顺序填满。由于每次插入时也不需要移动已有数据，因此效率很高，也不会增加很多开销在维护索引上。

如果使用非自增主键（如果身份证号或学号等），由于每次插入主键的值近似于随机，因此每次新纪录都要被插到现有索引页得中间某个位置：

![image-20210311001514919](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210311001514919.png)

此时MySQL不得不为了将新记录插到合适位置而移动数据，甚至目标页面可能已经被回写到磁盘上而从缓存中清掉，此时又要从磁盘上读回来，这增加了很多开销，同时频繁的移动、分页操作造成了大量的碎片，得到了不够紧凑的索引结构，后续不得不通过OPTIMIZE TABLE来重建表并优化填充页面。

**因此，只要可以，请尽量在InnoDB上采用自增字段做主键。**

#### X.3.5.总结

**索引失效的条件**

- 不在索引列上做任何操作（计算、函数、（自动or手动）类型转换），会导致索引失效而转向全表扫描
- 存储引擎不能使用索引范围条件右边的列
- 尽量使用覆盖索引（只访问索引的查询（索引列和查询列一致）），减少select *
- mysql在使用不等于（！=或者<>）的时候无法使用索引会导致全表扫描
- is null,is not null也无法使用索引
- like以通配符开头（’%abc…’）mysql索引失效会变成全表扫描的操作。





### X.4.mysql 索引过长1071-max key length is 767 byte

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



### X.5.长字段的索引调优

`selelct * from  employees where first_name = ' Facello'`  假设 `first_name` 的字段长度很长，如大于200个字符，那么索引占用的空间也会很大，作用在超长字段的索引查询效率也不高。

**解决方法**： 额外创建个字段，比如`first_name_hash int default 0 not null`. first_name的hashcode 

```sql
insert into employees value (999999, now(), 'zhangsan...','zhang','M',now(), CRC32('zhangsan...'));
```

first_name_hash的值应该具备以下要求

- 字段的长度应该比较小，SHA1和MD5是不合适的

- 应当尽量避免hash冲突，就目前来说，流行使用CRC32(),或者FNV64()

修改后的SQL `selelct * from  employees where first_name_hash = CRC32(zhangsan...) and first_name = 'Facello' `

并且给 first_name_hash设置所有，并带上 `first_name = ' Facello'` 为了解决hash冲突也能返回正确的结果。

但是，`selelct * from  employees where first_name like ' Facello%' `，如果是like，就不能使用上面的调优方法。

**解决方法**： 前缀索引`alter table employees add key (first_name(5))`  **这里的5是如何确定的，能不能其它数字呢？**

索引选择性 = 不重复的索引值/数据表的总记录数

数值越大，表示选择性越高，性能越好。

`select count(distince first_name)/count(*) from employees;`  -- 返回的值为0。0043 完整列的选择性 0.0043 【这个字段的最大选择性】

```
select count(distinct left(first_name,5)) / count(*) from employees; -- 返回结果 0.0038
select count(distinct left(first_name,6)) / count(*) from employees; -- 返回结果 0.0041
select count(distinct left(first_name,7)) / count(*) from employees; -- 返回结果 0.0042
select count(distinct left(first_name,8)) / count(*) from employees; -- 返回结果 0.0042
select count(distinct left(first_name,9)) / count(*) from employees; -- 返回结果 0.0042
select count(distinct left(first_name,10)) / count(*) from employees; -- 返回结果 0.0042
select count(distinct left(first_name,11)) / count(*) from employees; -- 返回结果 0.0043，说明 为大于等于11时，返回 0.0043
select count(distinct left(first_name,``12``)) / count(*) from employees; -- 返回结果 0.0043
```

说明 为大于等于11时，返回 0.0043

**结论**： 前缀索引的长度设置为11 

`alter table employees add key (first_name(11)) `

**优点**： 前缀索引可以让索引更小，更加高效，而且对上层应用是透明的。应用不需要做任何改造，使用成本较低。

这是一种比较容易落地的优化方案。

**局限性**： 无法做order by、group by； 无法使用覆盖索引。

 

**使用场景**： 后缀索引，MySql是没有后缀索引的

额外创建一个字段，比如说first_name_reverse, 在存储的时候，把first_name的值翻转过来再存储。

比方Facello 变成 ollecaF存储到first_name_reverse



### X.6.亿级大表在线不锁表变更字段与索引

https://www.cnblogs.com/huaweiyun/p/14291566.html

[pt-osc官网](https://www.percona.com/doc/percona-toolkit/3.0/pt-online-schema-change.html)

