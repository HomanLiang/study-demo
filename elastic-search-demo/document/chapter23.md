[toc]



# Lunece

## 1.简介
Lucene是apache软件基金会4 jakarta项目组的一个子项目，是一个开放源代码的全文检索引擎工具包，但它不是一个完整的全文检索引擎，而是一个全文检索引擎的架构，提供了完整的查询引擎和索引引擎，部分文本分析引擎（英文与德文两种西方语言）。Lucene的目的是为软件开发人员提供一个简单易用的工具包，以方便的在目标系统中实现全文检索的功能，或者是以此为基础建立起完整的全文检索引擎。Lucene是一套用于全文检索和搜寻的开源程式库，由Apache软件基金会支持和提供。Lucene提供了一个简单却强大的应用程式接口，能够做全文索引和搜寻。在Java开发环境里Lucene是一个成熟的免费开源工具。就其本身而言，Lucene是当前以及最近几年最受欢迎的免费Java信息检索程序库。人们经常提到信息检索程序库，虽然与搜索引擎有关，但不应该将信息检索程序库与搜索引擎相混淆。
### 1.1.那么先来说一说什么是全文搜索
说之前先说一说数据的分类：
- 结构化数据：指具有固定格式或有限长度的数据，如数据库，元数据等。
- 非结构化数据：指不定长或无固定格式的数据，如邮件，word文档等磁盘上的文件
### 1.2.结构化数据查询方法
**数据库搜索**

数据库中的搜索很容易实现，通常都是使用sql语句进行查询，而且能很快的得到查询结果。

为什么数据库搜索很容易？

因为数据库中的数据存储是有规律的，有行有列而且数据格式、数据长度都是固定的。

### 1.3.非结构化数据查询方法
**顺序扫描法(Serial Scanning)**

所谓顺序扫描，比如要找内容包含某一个字符串的文件，就是一个文档一个文档的看，对于每一个文档，从头看到尾，如果此文档包含此字符串，则此文档为我们要找的文件，接着看下一个文件，直到扫描完所有的文件。如利用windows的搜索也可以搜索文件内容，只是相当的慢。
**全文检索(Full-text Search)**

将非结构化数据中的一部分信息提取出来，重新组织，使其变得有一定结构，然后对此有一定结构的数据进行搜索，从而达到搜索相对较快的目的。这部分从非结构化数据中提取出的然后重新组织的信息，我们称之索引。

例如：字典。字典的拼音表和部首检字表就相当于字典的索引，对每一个字的解释是非结构化的，如果字典没有音节表和部首检字表，在茫茫辞海中找一个字只能顺序扫描。然而字的某些信息可以提取出来进行结构化处理，比如读音，就比较结构化，分声母和韵母，分别只有几种可以一一列举，于是将读音拿出来按一定的顺序排列，每一项读音都指向此字的详细解释的页数。我们搜索时按结构化的拼音搜到读音，然后按其指向的页数，便可找到我们的非结构化数据——也即对字的解释。

这种先建立索引，再对索引进行搜索的过程就叫全文检索(Full-text Search)。

虽然创建索引的过程也是非常耗时的，但是索引一旦创建就可以多次使用，全文检索主要处理的是查询，所以耗时间创建索引是值得的。

**全文检索的应用场景**

对于数据量大、数据结构不固定的数据可采用全文检索方式搜索，比如百度、Google等搜索引擎、论坛站内搜索、电商网站站内搜索等。

## 2.核心模块
Lucene 的写流程和读流程如下图所示：

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233504.png)

其中，虚线箭头（a、b、c、d）表示写索引的主要过程，实线箭头（1-9）表示查询的主要过程。

Lucene 中的主要模块及模块说明如下：
- analysis：主要负责词法分析及语言处理，也就是我们常说的分词，通过该模块可最终形成存储或者搜索的最小单元 Term。
- index 模块：主要负责索引的创建工作。
- store 模块：主要负责索引的读写，主要是对文件的一些操作，其主要目的是抽象出和平台文件系统无关的存储。
- queryParser 模块：主要负责语法分析，把我们的查询语句生成 Lucene 底层可以识别的条件。
- search 模块：主要负责对索引的搜索工作。
- similarity 模块：主要负责相关性打分和排序的实现。

## 3.核心术语
下面介绍 Lucene 中的核心术语：
- Term：是索引里最小的存储和查询单元，对于英文来说一般是指一个单词，对于中文来说一般是指一个分词后的词。
- 词典（Term Dictionary，也叫作字典）：是 Term 的集合。词典的数据结构可以有很多种，每种都有自己的优缺点。
比如：排序数组通过二分查找来检索数据：HashMap（哈希表）比排序数组的检索速度更快，但是会浪费存储空间。
FST(finite-state transducer)有更高的数据压缩率和查询效率，因为词典是常驻内存的，而 FST 有很好的压缩率，所以 FST 在 Lucene 的最新版本中有非常多的使用场景，也是默认的词典数据结构。
- 倒排序（Posting List）：一篇文章通常由多个词组成，倒排表记录的是某个词在哪些文章中出现过。
- 正向信息：原始的文档信息，可以用来做排序、聚合、展示等。
- 段（Segment）：索引中最小的独立存储单元。一个索引文件由一个或者多个段组成。在 Luence 中的段有不变性，也就是说段一旦生成，在其上只能有读操作，不能有写操作。

Lucene 的底层存储格式如下图所示，由词典和倒排序两部分组成，其中的词典就是 Term 的集合：

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233521.png)

词典中的 Term 指向的文档链表的集合，叫做倒排表。词典和倒排表是 Lucene 中很重要的两种数据结构，是实现快速检索的重要基石。

词典和倒排表是分两部分存储的，在倒排序中不但存储了文档编号，还存储了词频等信息。

在上图所示的词典部分包含三个词条（Term）：Elasticsearch、Lucene 和 Solr。词典数据是查询的入口，所以这部分数据是以 FST 的形式存储在内存中的。

在倒排表中，“Lucene”指向有序链表 3，7，15，30，35，67，表示字符串“Lucene”在文档编号为3、7、15、30、35、67的文章中出现过，Elasticsearch 和 Solr 同理。

## 4.检索方式
在 Lucene 的查询过程中的主要检索方式有以下四种：
### 4.1.单个词查询
指对一个 Term 进行查询。比如，若要查找包含字符串“Lucene”的文档，则只需在词典中找到 Term“Lucene”，再获得在倒排表中对应的文档链表即可。
### 4.2.AND
指对多个集合求交集。比如，若要查找既包含字符串“Lucene”又包含字符串“Solr”的文档，则查找步骤如下：

在词典中找到 Term “Lucene”，得到“Lucene”对应的文档链表。

在词典中找到 Term “Solr”，得到“Solr”对应的文档链表。

合并链表，对两个文档链表做交集运算，合并后的结果既包含“Lucene”也包含“Solr”。
### 4.3.OR
指多个集合求并集。比如，若要查找包含字符串“Luence”或者包含字符串“Solr”的文档，则查找步骤如下：

在词典中找到 Term “Lucene”，得到“Lucene”对应的文档链表。

在词典中找到 Term “Solr”，得到“Solr”对应的文档链表。

合并链表，对两个文档链表做并集运算，合并后的结果包含“Lucene”或者包含“Solr”。
### 4.4.NOT
指对多个集合求差集。比如，若要查找包含字符串“Solr”但不包含字符串“Lucene”的文档，则查找步骤如下：
- 在词典中找到 Term “Lucene”，得到“Lucene”对应的文档链表。
- 在词典中找到 Term “Solr”，得到“Solr”对应的文档链表。
- 合并链表，对两个文档链表做差集运算，用包含“Solr”的文档集减去包含“Lucene”的文档集，运算后的结果就是包含“Solr”但不包含“Lucene”。

通过上述四种查询方式，我们不难发现，由于 Lucene 是以倒排表的形式存储的。

所以在 Lucene 的查找过程中只需在词典中找到这些 Term，根据 Term 获得文档链表，然后根据具体的查询条件对链表进行交、并、差等操作，就可以准确地查到我们想要的结果。

相对于在关系型数据库中的“Like”查找要做全表扫描来说，这种思路是非常高效的。

虽然在索引创建时要做很多工作，但这种一次生成、多次使用的思路也是非常高明的。

## 5.分段存储
在早期的全文检索中为整个文档集合建立了一个很大的倒排索引，并将其写入磁盘中，如果索引有更新，就需要重新全量创建一个索引来替换原来的索引。

这种方式在数据量很大时效率很低，并且由于创建一次索引的成本很高，所以对数据的更新不能过于频繁，也就不能保证实效性。

现在，在搜索中引入了段的概念（将一个索引文件拆分为多个子文件，则每个子文件叫做段），每个段都是一个独立的可被搜索的数据集，并且段具有不变性，一旦索引的数据被写入硬盘，就不可修改。

在分段的思想下，对数据写操作的过程如下：
- 新增：当有新的数据需要创建索引时，由于段段不变性，所以选择新建一个段来存储新增的数据。

- 删除：当需要删除数据时，由于数据所在的段只可读，不可写，所以 Lucene 在索引文件新增一个 .del 的文件，用来专门存储被删除的数据 id。
当查询时，被删除的数据还是可以被查到的，只是在进行文档链表合并时，才把已经删除的数据过滤掉。被删除的数据在进行段合并时才会被真正被移除。

- 更新：更新的操作其实就是删除和新增的组合，先在.del文件中记录旧数据，再在新段中添加一条更新后的数据。

段不可变性的优点如下：
- 不需要锁：因为数据不会更新，所以不用考虑多线程下的读写不一致情况。
可以常驻内存：段在被加载到内存后，由于具有不变性，所以只要内存的空间足够大，就可以长时间驻存，大部分查询请求会直接访问内存，而不需要访问磁盘，使得查询的性能有很大的提升。

- 缓存友好：在段的声明周期内始终有效，不需要在每次数据更新时被重建。

- 增量创建：分段可以做到增量创建索引，可以轻量级地对数据进行更新，由于每次创建的成本很低，所以可以频繁地更新数据，使系统接近实时更新。

段不可变性的缺点如下：
- 删除：当对数据进行删除时，旧数据不会被马上删除，而是在 .del 文件中被标记为删除。而旧数据只能等到段更新时才能真正地被移除，这样会有大量的空间浪费。
- 更新：更新数据由删除和新增这两个动作组成。若有一条数据频繁更新，则会有大量的空间浪费。
- 新增：由于索引具有不变性，所以每次新增数据时，都需要新增一个段来存储数据。当段段数量太多时，对服务器的资源（如文件句柄）的消耗会非常大，查询的性能也会受到影响。
- 过滤：在查询后需要对已经删除的旧数据进行过滤，这增加了查询的负担。

为了提升写的性能，Lucene 并没有每新增一条数据就增加一个段，而是采用延迟写的策略，每当有新增的数据时，就将其先写入内存中，然后批量写入磁盘中。

若有一个段被写到硬盘，就会生成一个提交点，提交点就是一个用来记录所有提交后的段信息的文件。

一个段一旦拥有了提交点，就说明这个段只有读的权限，失去了写的权限；相反，当段在内存中时，就只有写数据的权限，而不具备读数据的权限，所以也就不能被检索了。

从严格意义上来说，Lucene 或者 Elasticsearch 并不能被称为实时的搜索引擎，只能被称为准实时的搜索引擎。

写索引的流程如下：
- 新数据被写入时，并没有被直接写到硬盘中，而是被暂时写到内存中。Lucene 默认是一秒钟，或者当内存中数据量达到一定阶段时，再批量提交到磁盘中。
当然，默认的时间和数据量的大小是可以通过参数控制的。通过延时写的策略，可以减少数据往磁盘上写的次数，从而提升整体的写入性能，如图 3。

- 在达到出触发条件以后，会将内存中缓存的数据一次性写入磁盘中，并生成提交点。

- 清空内存，等待新的数据写入，如下图所示。

  ![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233540.png)

从上述流程可以看出，数据先被暂时缓存在内存中，在达到一定的条件再被一次性写入硬盘中，这种做法可以大大提升数据写入的速度。

但是数据先被暂时存放在内存中，并没有真正持久化到磁盘中，所以如果这时出现断电等不可控的情况，就会丢失数据，为此，Elasticsearch 添加了事务日志，来保证数据的安全。

## 6.段合并策略
虽然分段比每次都全量创建索引有更高的效率，但是由于在每次新增数据时都会新增一个段，所以经过长时间的的积累，会导致在索引中存在大量的段。

当索引中段的数量太多时，不仅会严重消耗服务器的资源，还会影响检索的性能。

因为索引检索的过程是：查询所有段中满足查询条件的数据，然后对每个段里查询的结果集进行合并，所以为了控制索引里段的数量，我们必须定期进行段合并操作。

但是如果每次合并全部的段，则会造成很大的资源浪费，特别是“大段”的合并。

所以 Lucene 现在的段合并思路是：根据段的大小将段进行分组，再将属于同一组的段进行合并。

但是由于对于超级大的段的合并需要消耗更多的资源，所以 Lucene 会在段的大小达到一定规模，或者段里面的数据量达到一定条数时，不会再进行合并。

所以 Lucene 的段合并主要集中在对中小段的合并上，这样既可以避免对大段进行合并时消耗过多的服务器资源，也可以很好地控制索引中段的数量。

段合并的主要参数如下：
- mergeFactor：每次合并时参与合并的最少数量，当同一组的段的数量达到此值时开始合并，如果小于此值则不合并，这样做可以减少段合并的频率，其默认值为 10。
- SegmentSize：指段的实际大小，单位为字节。
- minMergeSize：小于这个值的段会被分到一组，这样可以加速小片段的合并。
- maxMergeSize：若有一段的文本数量大于此值，就不再参与合并，因为大段合并会消耗更多的资源。

段合并相关的动作主要有以下两个：
- 对索引中的段进行分组，把大小相近的段分到一组，主要由 LogMergePolicy1 类来处理。
- 将属于同一分组的段合并成一个更大的段。

在段合并前对段的大小进行了标准化处理，通过 logMergeFactorSegmentSize 计算得出。

其中 MergeFactor 表示一次合并的段的数量，Lucene 默认该数量为 10；SegmentSize 表示段的实际大小。通过上面的公式计算后，段的大小更加紧凑，对后续的分组更加友好。

段分组的步骤如下：

1. 根据段生成的时间对段进行排序，然后根据上述标准化公式计算每个段的大小并且存放到段信息中，后面用到的描述段大小的值都是标准化后的值，如图 4 所示：

  ![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233555.png)

2. 在数组中找到最大的段，然后生成一个由最大段的标准化值作为上限，减去 LEVEL_LOG_SPAN（默认值为 0.75）后的值作为下限的区间，小于等于上限并且大于下限的段，都被认为是属于同一组的段，可以合并。

3. 在确定一个分组的上下限值后，就需要查找属于这个分组的段了，具体过程是：创建两个指针（在这里使用指针的概念是为了更好地理解）start 和 end。

  start 指向数组的第 1 个段，end 指向第 start+MergeFactor 个段，然后从 end 逐个向前查找落在区间的段。

  当找到第 1 个满足条件的段时，则停止，并把当前段到 start 之间的段统一分到一个组，无论段的大小是否满足当前分组的条件。

  如图 5 所示，第 2 个段明显小于该分组的下限，但还是被分到了这一组。

  ![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233616.png)

  这样做的好处如下：
- 增加段合并的概率，避免由于段的大小参差不齐导致段难以合并。
- 简化了查找的逻辑，使代码的运行效率更高。

4. 在分组找到后，需要排除不参加合并的“超大”段，然后判断剩余的段是否满足合并的条件。

  如图 5 所示，mergeFactor=5，而找到的满足合并条件的段的个数为 4，所以不满足合并的条件，暂时不进行合并，继续找寻下一个组的上下限。

5. 由于在第 4 步并没有找到满足段合并的段的数量，所以这一分组的段不满足合并的条件，继续进行下一分组段的查找。

  具体过程是：将 start 指向 end，在剩下的段（从 end 指向的元素开始到数组的最后一个元素）中寻找最大的段，在找到最大的值后再减去 LEVEL_LOG_SPAN 的值，再生成一下分组的区间值。

  然后把 end 指向数组的第 start+MergeFactor 个段，逐个向前查找第 1 个满足条件的段：重复第 3 步和第 4 步。

6. 如果一直没有找到满足合并条件的段，则一直重复第 5 步，直到遍历完整个数组，如图 6 所示：

  ![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233645.png)

7. 在找到满足条件的 mergeFactor 个段时，就需要开始合并了。但是在满足合并条件的段大于 mergeFactor 时，就需要进行多次合并。

  也就是说每次依然选择 mergeFactor 个段进行合并，直到该分组的所有段合并完成，再进行下一分组的查找合并操作。

8. 通过上述几步，如果找到了满足合并要求的段，则将会进行段的合并操作。

  因为索引里面包含了正向信息和反向信息，所以段合并的操作分为两部分：

  - 一个是正向信息合并，例如存储域、词向量、标准化因子等。
  - 一个是反向信息的合并，例如词典、倒排表等。

  在段合并时，除了需要对索引数据进行合并，还需要移除段中已经删除的数据。

## 7.Lucene 相似度打分
我们在前面了解到，Lucene 的查询过程是：首先在词典中查找每个 Term，根据 Term 获得每个 Term 所在的文档链表；然后根据查询条件对链表做交、并、差等操作，链表合并后的结果集就是我们要查找的数据。

这样做可以完全避免对关系型数据库进行全表扫描，可以大大提升查询效率。

但是，当我们一次查询出很多数据时，这些数据和我们的查询条件又有多大关系呢？其文本相似度是多少？

本节会回答这个问题，并介绍 Lucene 最经典的两个文本相似度算法：基于向量空间模型的算法和基于概率的算法（BM25）。

如果对此算法不太感兴趣，那么只需了解对文本相似度有影响的因子有哪些，哪些是正向的，哪些是逆向的即可，不需要理解每个算法的推理过程。但是这两个文本相似度算法有很好的借鉴意义。


## 8.Lucene实现全文检索的流程
索引和搜索流程图

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233741.png)

- 绿色表示索引过程，对要搜索的原始内容进行索引构建一个索引库，索引过程包括：
确定原始内容即要搜索的内容→采集文档→创建文档→分析文档→索引文档
- 红色表示搜索过程，从索引库中搜索内容，搜索过程包括：
用户通过搜索界面→创建查询→执行搜索，从索引库搜索→渲染搜索结果

接下来详细讲解一下这张图片：　
### 8.1.创建索引
对文档索引的过程，将用户要搜索的文档内容进行索引，索引存储在索引库（index）中。
这里我们要搜索的文档是磁盘上的文本文件，根据案例描述：凡是文件名或文件内容包括关键字的文件都要找出来，这里要对文件名和文件内容创建索引。

- **获得原始文档**

  原始文档是指要索引和搜索的内容。原始内容包括互联网上的网页、数据库中的数据、磁盘上的文件等。

  从互联网上、数据库、文件系统中等获取需要搜索的原始信息，这个过程就是信息采集，信息采集的目的是为了对原始内容进行索引。在Internet上采集信息的软件通常称为爬虫或蜘蛛，也称为网络机器人，爬虫访问互联网上的每一个网页，将获取到的网页内容存储起来。

- **创建文档对象**

  获取原始内容的目的是为了索引，在索引前需要将原始内容创建成文档（Document），文档中包括一个一个的域（Field），域中存储内容。

  这里我们可以将磁盘上的一个文件当成一个document，Document中包括一些Field（file_name文件名称、file_path文件路径、file_size文件大小、file_content文件内容），如下图：

  ![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505233933.png)

  注意：

  （1）每个Document可以有多个Field

  （2）不同的Document可以有不同的Field

  （3）同一个Document可以有相同的Field（域名和域值都相同）

  （4）每个文档都有一个唯一的编号，就是文档id。

- **分析文档**
  将原始内容创建为包含域（Field）的文档（document），需要再对域中的内容进行分析，分析的过程是经过对原始文档提取单词、将字母转为小写、去除标点符号、去除停用词等过程生成最终的语汇单元，可以将语汇单元理解为一个一个的单词。

  比如下边的文档经过分析如下：

  原文档内容：Lucene is a Java full-text search engine.  

  分析后得到的语汇单元：lucene、java、full、search、engine

  每个单词叫做一个Term，不同的域中拆分出来的相同的单词是不同的term。term中包含两部分一部分是文档的域名，另一部分是单词的内容。

  例如：文件名中包含apache和文件内容中包含的apache是不同的term。

- **创建索引**
  对所有文档分析得出的语汇单元进行索引，索引的目的是为了搜索，最终要实现只搜索被索引的语汇单元从而找到Document（文档）。

  ![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505234042.png)

  ![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505234048.png)

  注意：

  （1）创建索引是对语汇单元索引，通过词语找文档，这种索引的结构叫倒排索引结构。

  （2）传统方法是根据文件找到该文件的内容，在文件内容中匹配搜索关键字，这种方法是顺序扫描方法，数据量大、搜索慢。　　        

  （3）倒排索引结构是根据内容（词语）找文档，如下图：

  ![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505234146.png)

  倒排索引结构也叫反向索引结构，包括索引和文档两部分，索引即词汇表，它的规模较小，而文档集合较大。

  创建索引代码实例：

  新建一个Java工程，导入相关的jar包

  ![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505234155.png)

    ```
    //创建索引
        public void testCreateIndex() throws IOException{
            //指定索引库的存放位置Directory对象
            Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
            //索引库还可以存放到内存中
            //Directory directory = new RAMDirectory();

            //指定一个标准分析器，对文档内容进行分析
            Analyzer analyzer = new StandardAnalyzer();

            //创建indexwriterCofig对象
            //第一个参数： Lucene的版本信息，可以选择对应的lucene版本也可以使用LATEST
            //第二根参数：分析器对象
            IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);

            //创建一个indexwriter对象
            IndexWriter indexWriter = new IndexWriter(directory, config);

            //原始文档的路径
            File file = new File("E:\\programme\\searchsource");
            File[] fileList = file.listFiles();
            for (File file2 : fileList) {
                //创建document对象
                Document document = new Document();

                //创建field对象，将field添加到document对象中

                //文件名称
                String fileName = file2.getName();
                //创建文件名域
                //第一个参数：域的名称
                //第二个参数：域的内容
                //第三个参数：是否存储
                Field fileNameField = new TextField("fileName", fileName, Store.YES);

                //文件的大小
                long fileSize  = FileUtils.sizeOf(file2);
                //文件大小域
                Field fileSizeField = new LongField("fileSize", fileSize, Store.YES);

                //文件路径
                String filePath = file2.getPath();
                //文件路径域（不分析、不索引、只存储）
                Field filePathField = new StoredField("filePath", filePath);

                //文件内容
                String fileContent = FileUtils.readFileToString(file2);
                //String fileContent = FileUtils.readFileToString(file2, "utf-8");
                //文件内容域
                Field fileContentField = new TextField("fileContent", fileContent, Store.YES);

                document.add(fileNameField);
                document.add(fileSizeField);
                document.add(filePathField);
                document.add(fileContentField);
                //使用indexwriter对象将document对象写入索引库，此过程进行索引创建。并将索引和document对象写入索引库。
                indexWriter.addDocument(document);
            }
            //关闭IndexWriter对象。
            indexWriter.close();
        }
    ```

**Field域的属性概述**

**是否分析**：是否对域的内容进行分词处理。前提是我们要对域的内容进行查询。

**是否索引**：将Field分析后的词或整个Field值进行索引，只有索引方可搜索到。

比如：商品名称、商品简介分析后进行索引，订单号、身份证号不用分析但也要索引，这些将来都要作为查询条件。

**是否存储**：将Field值存储在文档中，存储在文档中的Field才可以从Document中获取

比如：商品名称、订单号，凡是将来要从Document中获取的Field都要存储。

**是否存储的标准**：是否要将内容展示给用户

|                           Field类                            |        数据类型        | Analyzed 是否分析 | Indexed 是否索引 | Stored 是否存储 |                             说明                             |
| :----------------------------------------------------------: | :--------------------: | :---------------: | :--------------: | :-------------: | :----------------------------------------------------------: |
|        StringField(FieldName, FieldValue,Store.YES))         |         字符串         |         N         |        Y         |      Y或N       | 这个Field用来构建一个字符串Field，但是不会进行分析，会将整个串存储在索引中，比如(订单号,姓名等)，是否存储在文档中用Store.YES或Store.NO决定 |
|          LongField(FieldName, FieldValue,Store.YES)          |         Long型         |         Y         |        Y         |      Y或N       | 这个Field用来构建一个Long数字型Field，进行分析和索引，比如(价格)，是否存储在文档中用Store.YES或Store.NO决定 |
|              StoredField(FieldName, FieldValue)              | 重载方法，支持多种类型 |         N         |        N         |        Y        | 这个Field用来构建不同类型Field，不分析，不索引，但要Field存储在文档中 |
| TextField(FieldName, FieldValue, Store.NO) 或者 TextField(FieldName, reader) |     字符串 或者 流     |         Y         |        Y         |      Y或N       | 如果是一个Reader, lucene猜测内容比较多,会采用Unstored的策略  |

### 8.2.查询索引
查询索引也是搜索的过程。搜索就是用户输入关键字，从索引（index）中进行搜索的过程。根据关键字搜索索引，根据索引找到对应的文档，从而找到要搜索的内容（这里指磁盘上的文件）。

 对要搜索的信息创建Query查询对象，Lucene会根据Query查询对象生成最终的查询语法，类似关系数据库Sql语法一样Lucene也有自己的查询语法，比如：“name:lucene”表示查询Field的name为“lucene”的文档信息。

- **用户查询接口**

  全文检索系统提供用户搜索的界面供用户提交搜索的关键字，搜索完成展示搜索结果。

  比如： 百度搜索

  Lucene不提供制作用户搜索界面的功能，需要根据自己的需求开发搜索界面。

- **创建查询**

  用户输入查询关键字执行搜索之前需要先构建一个查询对象，查询对象中可以指定查询要搜索的Field文档域、查询关键字等，查询对象会生成具体的查询语法

  例如： 语法 “fileName:lucene”表示要搜索Field域的内容为“lucene”的文档

- **执行查询**

  搜索索引过程：

  根据查询语法在倒排索引词典表中分别找出对应搜索词的索引，从而找到索引所链接的文档链表。

  比如搜索语法为“fileName:lucene”表示搜索出fileName域中包含Lucene的文档。

  搜索过程就是在索引上查找域为fileName，并且关键字为Lucene的term，并根据term找到文档id列表。

  可通过两种方法创建查询对象：

  - **使用Lucene提供Query子类**

    Query是一个抽象类，lucene提供了很多查询对象，比如TermQuery项精确查询，NumericRangeQuery数字范围查询等。

    如下代码：

    `Query query = new TermQuery(new Term("name", "lucene"));`

  - **使用QueryParse解析查询表达式**

    QueryParse会将用户输入的查询表达式解析成Query对象实例。

    如下代码：

    ```
    QueryParser queryParser = new QueryParser("name", new IKAnalyzer());
    Query query = queryParser.parse("name:lucene");
    ```

#### 8.2.1.MatchAllDocsQuery
使用MatchAllDocsQuery查询索引目录中的所有文档

```
@Test
    public void testMatchAllDocsQuery() throws Exception {
        //创建一个Directory对象，指定索引库存放的路径
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        //创建IndexReader对象，需要指定Directory对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建Indexsearcher对象，需要指定IndexReader对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //创建查询条件
        //使用MatchAllDocsQuery查询索引目录中的所有文档
        Query query = new MatchAllDocsQuery();
        //执行查询
        //第一个参数是查询对象，第二个参数是查询结果返回的最大值
        TopDocs topDocs = indexSearcher.search(query, 10);
        
        //查询结果的总条数
        System.out.println("查询结果的总条数："+ topDocs.totalHits);
        //遍历查询结果
        //topDocs.scoreDocs存储了document对象的id
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc属性就是document对象的id
            //int doc = scoreDoc.doc;
            //根据document的id找到document对象
            Document document = indexSearcher.doc(scoreDoc.doc);
            //文件名称
            System.out.println(document.get("fileName"));
            //文件内容
            System.out.println(document.get("fileContent"));
            //文件大小
            System.out.println(document.get("fileSize"));
            //文件路径
            System.out.println(document.get("filePath"));
            System.out.println("----------------------------------");
        }
        //关闭indexreader对象
        indexReader.close();
    }
```

#### 8.2.2.TermQuery（精准查询）
TermQuery，通过项查询，TermQuery不使用分析器所以建议匹配不分词的Field域查询，比如订单号、分类ID号等。指定要查询的域和要查询的关键词。
```
    //搜索索引
    @Test
    public void testSearchIndex() throws IOException{
        //创建一个Directory对象，指定索引库存放的路径
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        //创建IndexReader对象，需要指定Directory对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建Indexsearcher对象，需要指定IndexReader对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        //创建一个TermQuery（精准查询）对象，指定查询的域与查询的关键词
        //创建查询
        Query query = new TermQuery(new Term("fileName", "apache"));
        //执行查询
        //第一个参数是查询对象，第二个参数是查询结果返回的最大值
        TopDocs topDocs = indexSearcher.search(query, 10);
        //查询结果的总条数
        System.out.println("查询结果的总条数："+ topDocs.totalHits);
        //遍历查询结果
        //topDocs.scoreDocs存储了document对象的id
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc属性就是document对象的id
            //int doc = scoreDoc.doc;
            //根据document的id找到document对象
            Document document = indexSearcher.doc(scoreDoc.doc);
            //文件名称
            System.out.println(document.get("fileName"));
            //文件内容
            System.out.println(document.get("fileContent"));
            //文件大小
            System.out.println(document.get("fileSize"));
            //文件路径
            System.out.println(document.get("filePath"));
            System.out.println("----------------------------------");
        }
        //关闭indexreader对象
        indexReader.close();
    }
}
```

#### 8.2.3.NumericRangeQuery
可以根据数值范围查询。
```
//数值范围查询
    @Test
    public void testNumericRangeQuery() throws Exception {
        //创建一个Directory对象，指定索引库存放的路径
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        //创建IndexReader对象，需要指定Directory对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建Indexsearcher对象，需要指定IndexReader对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //创建查询
        //参数：
        //1.域名
        //2.最小值
        //3.最大值
        //4.是否包含最小值
        //5.是否包含最大值
        Query query = NumericRangeQuery.newLongRange("fileSize", 41L, 2055L, true, true);
        //执行查询

        //第一个参数是查询对象，第二个参数是查询结果返回的最大值
        TopDocs topDocs = indexSearcher.search(query, 10);
        
        //查询结果的总条数
        System.out.println("查询结果的总条数："+ topDocs.totalHits);
        //遍历查询结果
        //topDocs.scoreDocs存储了document对象的id
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc属性就是document对象的id
            //int doc = scoreDoc.doc;
            //根据document的id找到document对象
            Document document = indexSearcher.doc(scoreDoc.doc);
            //文件名称
            System.out.println(document.get("fileName"));
            //文件内容
            System.out.println(document.get("fileContent"));
            //文件大小
            System.out.println(document.get("fileSize"));
            //文件路径
            System.out.println(document.get("filePath"));
            System.out.println("----------------------------------");
        }
        //关闭indexreader对象
        indexReader.close();
    }
```
#### 8.2.4.BooleanQuery
可以组合查询条件。
```
//组合条件查询
    @Test
    public void testBooleanQuery() throws Exception {
        //创建一个Directory对象，指定索引库存放的路径
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        //创建IndexReader对象，需要指定Directory对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建Indexsearcher对象，需要指定IndexReader对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //创建一个布尔查询对象
        BooleanQuery query = new BooleanQuery();
        //创建第一个查询条件
        Query query1 = new TermQuery(new Term("fileName", "apache"));
        Query query2 = new TermQuery(new Term("fileName", "lucene"));
        //组合查询条件
        query.add(query1, Occur.MUST);
        query.add(query2, Occur.MUST);
        //执行查询

        //第一个参数是查询对象，第二个参数是查询结果返回的最大值
        TopDocs topDocs = indexSearcher.search(query, 10);
        
        //查询结果的总条数
        System.out.println("查询结果的总条数："+ topDocs.totalHits);
        //遍历查询结果
        //topDocs.scoreDocs存储了document对象的id
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc属性就是document对象的id
            //int doc = scoreDoc.doc;
            //根据document的id找到document对象
            Document document = indexSearcher.doc(scoreDoc.doc);
            //文件名称
            System.out.println(document.get("fileName"));
            //文件内容
            System.out.println(document.get("fileContent"));
            //文件大小
            System.out.println(document.get("fileSize"));
            //文件路径
            System.out.println(document.get("filePath"));
            System.out.println("----------------------------------");
        }
        //关闭indexreader对象
        indexReader.close();
    }
```
**Occur.MUST**：必须满足此条件，相当于and

**Occur.SHOULD**：应该满足，但是不满足也可以，相当于or

**Occur.MUST_NOT**：必须不满足。相当于not

#### 8.2.5.使用queryparser查询
通过QueryParser也可以创建Query，QueryParser提供一个Parse方法，此方法可以直接根据查询语法来查询。Query对象执行的查询语法可通过System.out.println(query);查询。

这个操作需要使用到分析器。建议创建索引时使用的分析器和查询索引时使用的分析器要一致。

```
@Test
    public void testQueryParser() throws Exception {
        //创建一个Directory对象，指定索引库存放的路径
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        //创建IndexReader对象，需要指定Directory对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建Indexsearcher对象，需要指定IndexReader对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //创建queryparser对象
        //第一个参数默认搜索的域
        //第二个参数就是分析器对象
        QueryParser queryParser = new QueryParser("fileName", new IKAnalyzer());
        //使用默认的域,这里用的是语法，下面会详细讲解一下
        Query query = queryParser.parse("apache");
        //不使用默认的域，可以自己指定域
        //Query query = queryParser.parse("fileContent:apache");
        //执行查询


        //第一个参数是查询对象，第二个参数是查询结果返回的最大值
        TopDocs topDocs = indexSearcher.search(query, 10);
        
        //查询结果的总条数
        System.out.println("查询结果的总条数："+ topDocs.totalHits);
        //遍历查询结果
        //topDocs.scoreDocs存储了document对象的id
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc属性就是document对象的id
            //int doc = scoreDoc.doc;
            //根据document的id找到document对象
            Document document = indexSearcher.doc(scoreDoc.doc);
            //文件名称
            System.out.println(document.get("fileName"));
            //文件内容
            System.out.println(document.get("fileContent"));
            //文件大小
            System.out.println(document.get("fileSize"));
            //文件路径
            System.out.println(document.get("filePath"));
            System.out.println("----------------------------------");
        }
        //关闭indexreader对象
        indexReader.close();        
    }
```
**查询语法**
- 基础的查询语法，关键词查询：

  域名+“：”+搜索的关键字

  例如：content:java

- 范围查询

  域名+“:”+[最小值 TO 最大值]

  例如：size:[1 TO 1000]

  范围查询在lucene中支持数值类型，不支持字符串类型。在solr中支持字符串类型。

- 组合条件查询

  +条件1 +条件2：两个条件之间是并且的关系and

  例如：+filename:apache +content:apache

  +条件1 条件2：必须满足第一个条件，应该满足第二个条件

  例如：+filename:apache content:apache

  条件1 条件2：两个条件满足其一即可。

  例如：filename:apache content:apache

  -条件1 条件2：必须不满足条件1，要满足条件2

  例如：-filename:apache content:apache

#### 8.2.6.MultiFieldQueryParser
可以指定多个默认搜索域
```
@Test
    public void testMultiFiledQueryParser() throws Exception {
        //创建一个Directory对象，指定索引库存放的路径
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        //创建IndexReader对象，需要指定Directory对象
        IndexReader indexReader = DirectoryReader.open(directory);
        //创建Indexsearcher对象，需要指定IndexReader对象
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        
        //可以指定默认搜索的域是多个
        String[] fields = {"fileName", "fileContent"};
        //创建一个MulitFiledQueryParser对象
        MultiFieldQueryParser queryParser = new MultiFieldQueryParser(fields, new IKAnalyzer());
        Query query = queryParser.parse("apache");
        System.out.println(query);
        //执行查询


        //第一个参数是查询对象，第二个参数是查询结果返回的最大值
        TopDocs topDocs = indexSearcher.search(query, 10);
        
        //查询结果的总条数
        System.out.println("查询结果的总条数："+ topDocs.totalHits);
        //遍历查询结果
        //topDocs.scoreDocs存储了document对象的id
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc属性就是document对象的id
            //int doc = scoreDoc.doc;
            //根据document的id找到document对象
            Document document = indexSearcher.doc(scoreDoc.doc);
            //文件名称
            System.out.println(document.get("fileName"));
            //文件内容
            System.out.println(document.get("fileContent"));
            //文件大小
            System.out.println(document.get("fileSize"));
            //文件路径
            System.out.println(document.get("filePath"));
            System.out.println("----------------------------------");
        }
        //关闭indexreader对象
        indexReader.close();
    }
```
#### 8.2.7.IndexSearcher搜索方法
|                        方法                         |                             说明                             |
| :-------------------------------------------------: | :----------------------------------------------------------: |
|           indexSearcher.search(query, n)            |             根据Query搜索，返回评分最高的n条记录             |
|       indexSearcher.search(query, filter, n)        |      根据Query搜索，添加过滤策略，返回评分最高的n条记录      |
|        indexSearcher.search(query, n, sort)         |      根据Query搜索，添加排序策略，返回评分最高的n条记录      |
| indexSearcher.search(booleanQuery, filter, n, sort) | 根据Query搜索，添加过滤策略，添加排序策略，返回评分最高的n条记录 |

#### 8.2.8.TopDocs
Lucene搜索结果可通过TopDocs遍历，TopDocs类提供了少量的属性，如下：
| 方法或属性 |          说明          |
| :--------: | :--------------------: |
| totalHits  | 匹配搜索条件的总记录数 |
| scoreDocs  |      顶部匹配记录      |

#### 8.2.9.中文分词器 ：
首先，看一看Lucene自带的中文分词器
- **StandardAnalyzer**：（标准分词器，也是前面例子中使用的分词器）
  
  单字分词：就是按照中文一个字一个字地进行分词。
  

如：“我爱中国”，

  效果：“我”、“爱”、“中”、“国”。

- **CJKAnalyzer**
  
  二分法分词：按两个字进行切分。
  
  如：“我是中国人”，

  效果：“我是”、“是中”、“中国”“国人”。
  
  但上边两个分词器无法满足需求。
  
- **SmartChineseAnalyzer**
  
  对中文支持较好，但扩展性差，扩展词库，禁用词库和同义词库等不好处理
  

然后，看一看我们开发真正使用的第三方中文分词器：

  我们今天介绍IK-analyzer这款第三方中文分词器

- **IK-analyzer**： 最新版在https://code.google.com/p/ik-analyzer/上，支持Lucene 4.10从2006年12月推出1.0版开始，IKAnalyzer已经推出了4个大版本。最初，它是以开源项目Luence为应用主体的，结合词典分词和文法分析算法的中文分词组件。从3.0版本开 始，IK发展为面向Java的公用分词组件，独立于Lucene项目，同时提供了对Lucene的默认优化实现。在2012版本中，IK实现了简单的分词 歧义排除算法，标志着IK分词器从单纯的词典分词向模拟语义分词衍化。 但是也就是2012年12月后没有在更新。
  
	使用方法：
  

	第一步：把jar包添加到工程中

	第二步：把配置文件和扩展词词典和停用词词典添加到classpath下（停用词词典与扩展词词典名称可自行定义，只要在配置文件中配置好就可以了）

	![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505234354.png)

	注意：扩展词词典和停用词词典文件的格式为UTF-8，注意是无BOM 的UTF-8 编码。

	配置文件详情

    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">  
    <properties>  
        <comment>IK Analyzer 扩展配置</comment>
        <!--用户可以在这里配置自己的扩展字典 --> 
        <entry key="ext_dict">ext.dic;</entry> 
    
        <!--用户可以在这里配置自己的扩展停止词字典-->
        <entry key="ext_stopwords">stopword.dic;</entry> 
    </properties>
    ```

停用词词典与扩展词词典样例：

![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/team-manage/20210505234406.png)

这样，创建分析器时，用一下代码就好了

```
Analyzer analyzer = new IKAnalyzer();
```

注意：搜索使用的分析器要和索引使用的分析器一致，不然搜索出来结果可能会错乱。

### 8.3.删除索引
删除全部索引

说明：将索引目录的索引信息全部删除，直接彻底删除，无法恢复。此方法慎用！！

```
//删除全部索引
    @Test
    public void testDeleteAllIndex() throws Exception {
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        //删除全部索引
        indexWriter.deleteAll();
        //关闭indexwriter
        indexWriter.close();
    }
```
指定查询条件删除
```
//根据查询条件删除索引
    @Test
    public void deleteIndexByQuery() throws Exception {
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        //创建一个查询条件
        Query query = new TermQuery(new Term("fileContent", "apache"));
        //根据查询条件删除
        indexWriter.deleteDocuments(query);
        //关闭indexwriter
        indexWriter.close();
    }
```

### 8.4.索引库的修改
更新的原理就是先删除在添加
```
//修改索引库
    @Test
    public void updateIndex() throws Exception {
        Directory directory = FSDirectory.open(new File("E:\\programme\\test"));
        Analyzer analyzer = new IKAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);
        //创建一个Document对象
        Document document = new Document();
        //向document对象中添加域。
        //不同的document可以有不同的域，同一个document可以有相同的域。
        document.add(new TextField("fileXXX", "要更新的文档", Store.YES));
        document.add(new TextField("contentYYY", "简介 Lucene 是一个基于 Java 的全文信息检索工具包。", Store.YES));
        indexWriter.updateDocument(new Term("fileName", "apache"), document);
        //关闭indexWriter
        indexWriter.close();
    }
```