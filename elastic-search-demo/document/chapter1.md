[toc]



# 搜索引擎相关概念

## 搜索引擎是什么

所谓搜索引擎，就是根据用户需求与一定算法，运用特定策略从互联网检索出制定信息反馈给用户的一门检索技术。搜索引擎依托于多种技术，如网络爬虫技术、检索排序技术、网页处理技术、大数据处理技术、自然语言处理技术等，为信息检索用户提供快速、高相关性的信息服务。搜索引擎技术的核心模块一般包括爬虫、索引、检索和排序等，同时可添加其他一系列辅助模块，以为用户创造更好的网络使用环境。



## 搜索相关性

搜索相关性用于描述文档与搜索字符串的匹配程度（ES 会计算出一个评分），目的是为文档进行排序，从而将最符合用户需求的文档排在前面。



## 索引

### 正排索引

拿 MySQL Innodb 的聚簇索引来说，如下图所示，一个极简版（无页属性）的 B+ 树索引结构大概是这样，叶子节点存放完整数据，非叶子节点存放建立对应聚簇索引对应的字段（主键），一条可以使用到聚簇索引的 SQL，会依次从上到下进行 B+ 树的查找直到字段一致；

```
CREATE TABLE user_info (
	id int,
	name varchar(16),
	hobby varchar(256)
);
```

![image-20210218223058014](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218223058014.png)



而对应非聚簇索引只是叶子节点的内容存放的是该表的主键信息，查询的顺序则是先通过非聚簇索引的字段找到叶子节点中一致的单个或者多个主键id，再使用这些主键id进行回表，最终获得对应的完整实体数据。如果我们看上面在 MySQL 中表的 hobby 爱好字段，如果我们有业务需求：根据用户爱好关键字如“篮球”去查询对应用户列表，我们怎么做，只能是写个字符串的 like SQL，全表扫描的逻辑。

```
SELECT *
FROM user_info
WHERE hobby LIKE '%篮球%';
```

 即使我们对 hobby 字段创建了普通索引，在 Innodb 引擎下，在查询中想使用字符串类型的索引也只能走最左前缀索引的逻辑，即 LIKE '篮球%'。幸好 Innodb 在 5.6 版本后支持了全文索引 Full text，在创建完全文索引后，查询中使用MATCH、AGAINST就能够使用全文索引了，比全表扫B+树效率会高很多，但是对应全文索引会占据相当的磁盘空间。全文索引与我们要说的倒排索引就是一个意思了。

```
SELECT *
FROM user_info
WHERE MATCH (hobby) AGAINST ('篮球');
```



### 倒排索引

相比 B+ 树的正排索引，如果我们对 hobby 字段建立了索引，他的倒排索引极简的数据格式如下。创建倒排索引的 field，会通过分词器根据语义将字段中的 field 分成一个一个对应的词索引（term index），构成该类型数据的全部词索引集合（term dictionary），如“喜欢篮球、唱歌”会被分成 “篮球”和“唱歌”两个 term index ；第二列是含有这些term index对应的文档 Id（documentId），这个数据可以帮助我们最终溯源到完整实体数据；第三列则是对应 term index 在该文档字段中的位置，0 表示在开头的位置，这个可以帮助标注检索出来数据的高亮信息。

<img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218223127901.png" alt="image-20210218223127901" style="zoom:80%;" />



### 倒排索引与分词

如何输入一个文档数据后创建对应的倒排索引，如 {"id":1,"name":"张三","hobby":"喜欢篮球、唱歌"}。拿ES来说，可以预先设置字段为 String 及对应的分词器，会针对 hobby 这个字段进行预处理，一整句话在经过下面的三个分词步骤，会被分成多个对应的词索引（term index），每个term index对应的位置、文档id也都会生成，添加到上述的数据结构中。

1. **Character Filters**：针对原始文本进行处理，比如去除 HTML 标签
1. **Tokenizer**：将原始文本按照一定规则切分为单词
1. **Token Filters**：针对 Tokenizer 处理的单词进行再加工，比如转小写、删除或增新等处理

针对不同的文本内容，我们可以使用不同的分词器甚至是自定义分词器，如ES的分词器：Standard Analyzer（默认分词器，按词切分，处理标点）、Simple Analyzer（不是子母的字符会被忽略切为切分点）、Whitespace Analyzer（基于空格的分词器）、IKAnalyzer（比较流行的中文分词器）；

MySQL 的全文索引也有对应的中文分词器 ngram。



### 两个索引查询顺序

通过上面的描述，我们可以知道在使用正排索引、倒排索引时的一个大概的查询顺序

<img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218223551937.png" alt="image-20210218223551937" style="zoom:80%;" />



### 倒排索引思想的应用

之前有接过一个需求描述：我们要对不同城市、年级、学期、设备的用户设定不同的内容展现规则，这几个属性是可以设置为空的，最后如果一个用户的属性匹配到了多个规则，那么就需要根据这几个属性的权重来打分，取分最高的规则，如我们配置规则如下：那么一个上海小学一年级春季的用户过来就匹配到了两个规则，然后根据这两个规则的属性进行权重值打分计算即可，最后选择显示A或B。

<img src="https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218223739218.png" alt="image-20210218223739218" style="zoom: 80%;" />

上面在进行数据查询时，SQL 如下，在这个or的过程的思想就类似于“倒排索引中分词+查找所有含有该词索引的文档数据”（只是这个词索引是早已确定好的），然后在搜索到多条记录后进行权重值计算（类似ES中的检索关联程度打分）。

```
SELECT *
FROM tb_rules
WHERE (city = '上海' OR grade = '小学一年级' OR term = '春季')
```



## 一个搜索过程

![image-20210223223704513](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210223223704513.png)

当用户向搜索引擎发送一个搜索请求的时候，搜索引擎经过了以下步骤：
1. 分词器对搜索字符串进行分词处理。
1. 在倒排索引表中查到匹配的文档。
1. 对每个匹配的文档进行相关性评分。
1. 根据相关性评分对文档进行排序。
1. 将排好序的文档返回给用户。



## 分词

### 中文分词

**为什么要进行中文分词？**

词是最小的能够独立活动的有意义的语言成分，英文单词之间是以空格作为自然分界符的，而汉语是以字为基本的书写单位，词语之间没有明显的区分标记，因此，中文词语分析是中文信息处理的基础与关键。
Lucene中对中文的处理是基于自动切分的单字切分，或者二元切分。除此之外，还有最大切分（包括向前、向后、以及前后相结合）、最少切分、全切分等等。


### 常用中文分词器

- IK分词

  [ElasticSearch IK中文分词使用详解](https://blog.csdn.net/xsdxs/article/details/72853288)

  [ElasticSearch中文分词器-IK分词器的使用](https://www.cnblogs.com/haixiang/p/11810799.html)

- ansj分词

  [ansj分词史上最详细教程](https://www.cnblogs.com/a-du/p/9667785.html)

- jieba分词

  [自然语言处理之jieba分词](https://www.cnblogs.com/chenhuabin/p/13521253.html)



## 检索质量评价标准

### 精确率和召回率

![image-20210218224549797](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218224549797.png)

#### 召回率（recall）

![image-20210218224639341](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218224639341.png)

#### 准确率（Precision）

![image-20210218224719883](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210218224719883.png)

### P@10指标

评估在搜索结果中排名最靠前的头10个文档中有多大比例是相关的。



### MAP指标

MAP（Mean Average Precision）多次查询的平均准确率衡量标准。

AP（Average Precision）衡量单次查询的检索质量。用户查询出的某个文档在理想搜索系统中应该排在第1位，结果排在了第2位。则AP值位1/2 = 0.5。

MAP就是计算多个文档的AP值，并求平均数。也就是说最理想的搜索引擎其MAP等于1。































**参考文章：**

[对正排索引与倒排索引的理解](https://juejin.cn/post/6850418110424416270)

[检索质量评价标准](https://www.yuque.com/backend/gc31mc/naaye2)

[中文分词技术(中文分词原理)](https://www.cnblogs.com/flish/archive/2011/08/08/2131031.html)



