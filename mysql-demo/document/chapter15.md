[toc]



# MySQL 应用

## 1.高并发下的数据字段变更

### 1.1.背景

经常会遇到这种情况，我们的业务已经稳定地运行一段时间了，并且流量渐渐已经上去了。这时候，却因为某些原因（比如功能调整或者业务扩展），你需要对数据表进行调整，加字段 or 修改表结构。

可能很多人说 alter table add column ... / alter table modify ...，轻轻松松就解决了。 这样其实是有风险的，对于复杂度比较高、数据量比较大的表。调整表结构、创建或删除索引、触发器，都可能引起锁表，而锁表的时长依你的数据表实际情况而定。 本人有过惨痛的教训，在一次业务上线过程中没有评估好数据规模，导致长时间业务数据写入不进来。

那么有什么办法对数据库的业务表进行无缝升级，让该表对用户透明无感呢？下面我们一个个来讨论。

### 1.2.新增关联表

最简单的一种办法，把新增的字段存储在另外一张辅表上，用外键关联到主表的主键。达到动态扩展的目标。后续功能上线之后，新增的数据会存储到辅表中，主表无需调整，透明、无损。
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180459.png)
存在的问题：

- 读取数据时，联表查询效率低下，数据量越大，数据越复杂，劣势越明显。
- 并没有彻底的解决问题，之后有新增字段，照样面临是新增表还是修改原表的问题。即使后续新增的字段都加在辅表上，同样面临锁表的问题。
- 辅表的作用仅仅是解决字段新增的问题，并未解决字段更新的问题（如修改字段名、数据类型等）。

### 1.3.新增通用列

假设我们原有表结构如下，为了保障业务的持续发展，后续不间断的会有字段扩展。这时候就需要考虑增加一个可自动扩缩的通用字段。
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180534.png)
以MySQL为例子，5.7版本版本之后提供了Json字段类型，方便我们存储复杂的Json对象数据。

```sql
use test;
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE "t_user" (
  "id" bigint(20) NOT NULL AUTO_INCREMENT,
  "name" varchar(20) NOT NULL,
  "age" int(11) DEFAULT NULL,
  "address" varchar(255) DEFAULT NULL,
  "sex" int(11) DEFAULT '1',
  "ext_data" json DEFAULT NULL COMMENT 'json字符串',
  PRIMARY KEY ("id")
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES ('1', 'brand', '21', 'fuzhou', '1', '{"tel": "13212345678", "name": "brand", "address": "fuzhou"}');
```

代码中 ext_data 采用Json数据类型，是一种可扩展的对象载体，存放被查询数据的信息补充。
同样的，MySQL提供的这种数据类型，也提供了很强大的Json函数进行操作。

```sql
SELECT id,`name`,age,address FROM `t_user` WHERE json_extract(ext_data,'$.tel') = '13212345678';
```

结果如下：
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180555.png)

之前写MySQL系列的时候，博客园的一位读者留言要我归纳一下MySQL Json 的用法，一直没时间，大家可以看一下[官网的文档](https://dev.mysql.com/doc/refman/8.0/en/json-functions.html)，还是比较清晰的。

Json结构一般来说是向下兼容的，所以你在设计字段扩展的时候，一般建议往前增，不建议删除旧属性。但是这也有个问题，就是业务越复杂，Json复杂度也越高，冗余属性也越多。

比如上文中我们的json包含三个属性，tel、name、address，之后的业务调整中，发现tel没用了，加了个age属性，那tel要不要删除？

有一种比较好的办法，是给表加上version属性，每个时期的业务对应一个version，每个version对应的Json数据结构也不一样。
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180610.png)

**优点：**

- 可以随时动态扩展属性
- 新旧两种数据可以同时存在
- 迁移数据方便，写个程序将旧版本ext的改为新版本的ext，并修改version

**不足：**

- ext_data里的字段无法建立索引
- ext_data里的key会有大量空间占用，建议key简短一些
- 从json中去统计某个字段数据之类的很麻烦，而且效率低。
- 查询相对效率较低，操作复杂。
- 更新Json中的某个字段效率较低，不适合存储业务逻辑复杂的数据。
- 统计数据复杂，建议需要做报表的数据不要存json。

**改进：**

- 如果ext里的属性有索引之类的需求，可能NoSql（如MongoDB）会更适合

### 1.4.新表+数据迁移

#### 1.4.1.利用触发器进行数据迁移

![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180624.png)
整个步骤如下：

- 新建一个表t_user_v1 (id, name, age, address, sex, ext_column)，包含了扩展字段 ext_column
- 在原有表上添加触发器，原表的DML操作（主要INSERT、UPDATE、DELETE），都会触发操作，把数据转存到新表t_user_v1中
- 对于旧表中原有的数据，逐步的迁移直至完成
- 删掉触发器，把原表移走（默认是drop掉）
- 把新表t_user_v1重命名（rename）成原表t_user
  通过上述步骤，逐渐的将数据迁移到新表，并替换旧表，整个操作无需停服维护，对用业务无损

#### 1.4.2.利用Binlog 进行数据迁移

如果是MySQL数据库，可以通过复制binlog的操作进行数据迁移的，效果一样，比起触发器，更稳定一点。
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180703.png)

#### 1.4.3.存在的问题

- 操作繁琐，效率低下
- 数据迁移和数据表切换之间存在操作间隙，对于高并发、高频操作的数据表，还是有风险的，会引起短暂连接失效 和 数据不一致。
- 对于大数据表，同步时间长

### 1.5.字段预留

预留字段 和 字段与表格名称映射的办法。
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180744.png)

#### 1.5.1.存在的问题

- 同样的，查询效率低
- 预设存在未知数，可能存在预设的字段不够，也可能存在空间冗余
- 冗余过多的空子字段，对存储空间的占用和性能的提升存在阻碍。
- 该方法还是比较笨的，不适合程序员思维

### 1.6.多主模式和分级更新

如果业务流量比较小，可以直接在表上进行字段新增或者修改，短暂的写锁是可以承受的。但如果是高并发、集群化、分布式的系统，则从数据层面上就应该进行主从或者分库分表治理。
以下是典型的的多主要模式下，进行数据库表结构升级的过程。
![image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20220831180810.png)

1. 正常两主模式下，主主同步，可以使用DBproxy、Fabric 等数据中间件做负载均衡，也可以自己定义一些负载策略，比如 Range、Hash。
2. 修改配置，让流量都切到其中一台上，然后对另外一台进行数据表升级（比如切DB1，只使用DB2）。切记在业务低峰期进行，避免流量过大导致另外一个数据库实例负载过大而挂起。
3. 轮流这个操作，但是这时候不需要再升级DB2了，因为是主主同步。DB instance 1 已经是新的表结构了，这时候会连同架构包括数据一起更新到 DB2 上。
4. 等两个数据库实例都一致了，修改配置，重设两个数据库实例的负载，恢复到之前的状态。

