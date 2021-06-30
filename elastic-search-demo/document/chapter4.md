[toc]



# ElasticSearch 分词器

## 1.参考文档

- [官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.4/analysis.html)



## 2.精确值与全文本

`ES` 中有 **精确值**（`Exact Values`）与 **全文本**（`Full Text`）之分：

- **精确值**：包括数字，日期，一个具体字符串（例如 `Hello World`）。
  - 在 ES 中用 [keyword](https://www.elastic.co/guide/en/elasticsearch/reference/current/keyword.html) 数据类型表示。
  - 精确值不需要做分词处理。
- **全文本**：非结构化的文本数据
  - 在 ES 中用 [text](https://www.elastic.co/guide/en/elasticsearch/reference/current/text.html) 数据类型表示。
  - 全文本需要做分词处理。

示例：

![image-20210223225131176](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210223225131176.png)



## 3.分词过程

搜索引擎需要建立单词（`Term / Token`）与倒排索引项的对应关系，那么首先就需要将文档拆分为单词，这个过程叫做分词。

比如将 `hello world` 拆分为 `hello` 和 `world`，这就是分词过程。



## 4.分词器

`ES` 使用分词器（`Analyzer`）对文档进行分词，`ES` 中内置了很多分词器供我们使用，我们也可以定制自己的分词器。

一个分词器有 3 个组成部分，分词过程会依次经过这些部分：

1. **Character Filters**：字符过滤，用于删去某些字符。该组件可以有 0 或多个。
1. **Tokenizer**：分词过程，按照某个规则将文档切分为单词，比如用空格来切分。该组件有且只能有一个。
1. **Token Filter**：对切分好的单词进一步加工，比如大小写转换，删除停用词等。该组件可以有 0 或多个。



## 5.ES 中的分词器

ES 有下面这些[内置的分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html)：

- `Standard Analyzer`：默认分词器，按词切分，转小写处理，也可以过滤停用词（默认关闭）。
  - 在 `ES` 中的名称为 `standard`

- `Simple Analyzer`：按照非字母切分，非字母会被去除，转小写处理。
  - 在 `ES` 中的名称为 `simple`
- `Stop Analyzer`：按照非字母切分，非字母会被去除，转小写处理，停用词过滤(`the`、`a`、`is` 等)。
  - 在 `ES` 中的名称为 `stop`
- `Whitespace Analyzer`：按照空格切分，不转小写。
  - 在 `ES` 中的名称为 `whitespace`
- `Keyword Analyzer`：不做任何的分词处理，直接将输入当作输出。
  - 在 `ES` 中的名称为 keyword
- `Pattern Analyzer`：通过正则表达式进行分词，默认为 `\W+` 非字符分隔，然后会进行转小写处理。
  - 在 `ES` 中的名称为 `pattern`
- `Language Analyzers`：提供了 `30` 多种常见语言的分词器，比如：
  - `english`：英语分词器，会对英文单词进行归一化处理，去掉停用词等。
    - 归一化处理：比如 `running` 变为 `run`，`goods` 变为 `good` 等。



## 6.测试分词器

我们可以通过下面的 API 来测试分词器：

```
GET _analyze
{
  "analyzer": "AnalyzerName",
  "text": "内容"
}
```



## 7.自定义分词器

当 `ES` 中的内置分词器不能满足需求时，我们可以[定制自己的分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-custom-analyzer.html)。

在上文中已经介绍过一个分词器由 3 部分组成：

- `Character Filters`：字符过滤，用于删去某些字符。
  - 该组件可以有 0 或多个。
- `Tokenizer`：分词过程，按照某个规则将文档切分为单词，比如用空格来切分。
  - 该组件有且只能有一个。
- `Token Filter`：对切分好的单词进一步加工，比如大小写转换，删除停用词等。
  - 该组件可以有 0 或多个。



### 7.1.内置分词器组件

`ES` 对这 3 部分都有内置：

- 内置 `Character Filters`
  - [HTML Strip](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-htmlstrip-charfilter.html)：去除 HTML 标签。
  - [Mapping](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-mapping-charfilter.html)：字符串替换。
  - [Pattern Replace](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-pattern-replace-charfilter.html)：正则匹配替换。
- [内置 Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenizers.html)：有 15 种。
- [内置 Token Filter](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenfilters.html)：有将近 50 种。

**Character Filters** 示例：

```
# 使用 html_strip
POST _analyze
{
  "tokenizer":"keyword",
  "char_filter":["html_strip"],
  "text": "<b>hello world</b>"
}

# 使用 mapping
POST _analyze
{
  "tokenizer": "standard",
  "char_filter": [
      {
        "type" : "mapping",
        "mappings" : [ "- => _"]
      }
    ],
  "text": "123-456, I-test! test-990 650-555-1234"
}

# 正则匹配替换
POST _analyze
{
  "tokenizer": "standard",
  "char_filter": [
      {
        "type" : "pattern_replace",
        "pattern" : "http://(.*)",
        "replacement" : "$1"
      }
    ],
    "text" : "http://www.elastic.co"
}
```

**Token Filter** 示例：

```
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": ["stop"],
  "text": ["The gilrs in China are playing this game!"]
}

# 先 lowercase 再 stop
POST _analyze
{
  "tokenizer": "whitespace",
  "filter": ["lowercase","stop"],
  "text": ["The gilrs in China are playing this game!"]
}
```



### 7.2.自定义分词器

[自定义分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-custom-analyzer.html)需要使用 **settings** 配置，示例：

```
PUT index_name
{
  "settings": {        # 固定写法
    "analysis": {      # 固定写法
      "analyzer": {    # 固定写法
        "my_custom_analyzer": {     # 自定义分词器的名称
          "type": "custom",         # 固定写法
          "tokenizer": "standard",  # 定义 tokenizer
          "char_filter": [          # 定义 char_filter
            "html_strip"
          ],
          "filter": [               # 定义 token filter
            "lowercase",
            "asciifolding"
          ]
        }
      }
    }
  }
}

# 更复杂的一个示例
PUT index_name
{
  "settings": {         # 固定写法
    "analysis": {       # 固定写法
      "analyzer": {     # 固定写法
        "my_custom_analyzer": {         # 自定义分词器的名称
          "type": "custom",             # 固定写法
          "char_filter": [              # 定义 char_filter
            "emoticons"                 # 在下面定义
          ],
          "tokenizer": "punctuation",   # 定义 tokenizer
          "filter": [                   # 定义 token filter
            "lowercase",
            "english_stop"              # 在下面定义
          ]
        }
      },
      
      "tokenizer": {                   # 自定义 tokenizer
        "punctuation": {               # 自定义的 tokenizer 名称
          "type": "pattern",           # type
          "pattern": "[ .,!?]"         # 分词规则
        }
      },
      
      "char_filter": {                 # 自定义 char_filter
        "emoticons": {                 # 自定义的 char_filter 名称
          "type": "mapping",           # type
          "mappings": [                # 规则
            ":) => _happy_",
            ":( => _sad_"
          ]
        }
      },
      
      "filter": {                      # 自定义 token filter
        "english_stop": {              # 自定义 token filter 名称
          "type": "stop",              # type
          "stopwords": "_english_"     
        }
      }
    }
  }
}

# 使用自定义分词器
POST index_name/_analyze
{
  "analyzer": "my_custom_analyzer",
  "text": "I'm a :) person, and you?"
}
```



## 8.安装分词器插件

我们还可以通过**安装插件**的方式，来安装其它分词器。

比如安装 **analysis-icu** 分词器，它是一个不错的中文分词器：

```
elasticsearch-plugin install analysis-icu
```

**analysis-icu** 在 `ES` 中的名称为 `icu_analyzer`。

还有一些其它的中文分词器：

- [IK 分词器](https://github.com/medcl/elasticsearch-analysis-ik)
- [THULAC](https://github.com/microbun/elasticsearch-thulac-plugin/)：是由清华大学自然语言处理与社会人文计算实验室研制的一套中文分词器。
- [hanlp 分词器](https://github.com/KennFalcon/elasticsearch-analysis-hanlp)：基于 [HanLP](https://www.hanlp.com/)。
- [Pinyin 分词器](https://github.com/medcl/elasticsearch-analysis-pinyin)

其它分词器：

- [中科院计算所 NLPIR](http://ictclas.nlpir.org/nlpir/)
- [ansj分词器](https://github.com/NLPchina/ansj_seg)
- [哈工大的LTP](https://github.com/HIT-SCIR/ltp)
- [清华大学THULAC](https://github.com/thunlp/THULAC)
- [斯坦福分词器](https://nlp.stanford.edu/software/segmenter.shtml)
- [Hanlp分词器](https://github.com/hankcs/HanLP)
- [结巴分词](https://github.com/yanyiwu/cppjieba)
- [KCWS分词器](https://github.com/koth/kcws)
- [ZPar](https://github.com/frcchang/zpar/releases)
- [IKAnalyzer](https://github.com/wks/ik-analyzer)



## 9.IK分词器

### 9.1.IK分词器的安装

**下载地址：**https://github.com/medcl/elasticsearch-analysis-ik/releases在这上面有 `elasticsearch` 所对应版本的IK分词器以编译的包，可以在上面找到对应版本进行下载使用

1. 下载：

   ```
   wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.5.1/elasticsearch-analysis-ik-7.5.1.zip
   ```

2. 在解压到 `elasticsearch/plugin/ik/` 文件夹中，`ik` 文件夹需要自己建

   ```
   unzip elasticsearch-analysis-ik-7.5.1.zip -d $ES_HOME/plugins/ik/
   ```

   把 `$ES_HOME` 替换成你 `elasticsearch` 所在目录即可

3. 重启 `elasticsearch` 后就可以用了



### 9.2.使用ik分词器

创建索引时指定分词器：

```
PUT my_index
{
    "settings": {
        "analysis": {
            "analyzer": {
                "ik": {
                    "tokenizer": "ik_max_word"
                }
            }
        }
    },
    "mappings": {
        "properties": {
            "title": {
                "type": "text"
            },
            "content": {
                "type": "text",
                "analyzer": "ik_max_word"
            }
        }
    }
}
```

`IK` 分词器示例：

1. `ik_smart`:会做最粗粒度的拆分

   ```
   POST _analyze
   {
     "analyzer": "ik_smart",
     "text": ["我是中国人"]
   }
   ```

   ![image-20210221190812724](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210312211148.png)

2. `ik_max_word`:会将文本做最细粒度的拆分

   ```
   POST _analyze
   {
     "analyzer": "ik_max_word",
     "text": ["我是中国人"]
   }
   ```

   ![image-20210221190902406](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210312211158.png)



### 9.3.扩展词典

在 `$ES_HOME/plugins/ik/elasticsearch-analysis-ik-7.5.1/config/IKAnalyzer.cfg.xml`  配置文件下

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
	<comment>IK Analyzer 扩展配置</comment>
	<!--用户可以在这里配置自己的扩展字典 -->
	<entry key="ext_dict">custom.dic</entry>
	 <!--用户可以在这里配置自己的扩展停止词字典-->
	<entry key="ext_stopwords"></entry>
	<!--用户可以在这里配置远程扩展字典 -->
	<!-- <entry key="remote_ext_dict">words_location</entry> -->
	<!--用户可以在这里配置远程扩展停止词字典-->
	<!-- <entry key="remote_ext_stopwords">words_location</entry> -->
</properties>
```

其中 `custom.dic` 为自定义的扩展字典， 内容可以根据自己需要，自定义分词的文字。

保存后重启 `elasticsearch` 即可看到自己想要的分词效果。



## 10.ES 实现实时从Mysql数据库中读取热词,停用词

`IK` 分词器虽然自带词库

![image-20210306213327744](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306213327744.png)

但是在实际开发应用中对于词库的灵活度的要求是远远不够的，`IK` 分词器虽然配置文件中能添加扩展词库，但是需要重启 `ES`

其实 `IK` 本身是支持热更新词库的,但是需要我感觉不是很好

词库热更新方案:

1. `IK` 原生的热更新方案,部署一个 `WEB` 服务器,提供一个 `Http` 接口，通过 `Modified` 和 `tag` 两个 `Http` 响应头，来完成词库的热更新

2. 通过修改 `IK` 源码支持 `Mysql` 定时更新数据

注意:推荐使用第二种方案，也是比较常用的方式，虽然第一种是官方提供的，但是官方也不建议使用

 

### 10.1.方案一:IK原生方案

1. 外挂词库，就是在 `IK` 配置文件中添加扩展词库文件多个之间使用分号分割

   优点：编辑指定词库文件，部署比较方便

   缺点：每次编辑更新后都需要重启 `ES`

2. 远程词库，就是在 `IK` 配置文件中配置一个 `Http` 请求，可以是 `.dic` 文件,也可以是接口，同样多个之间使用分号分割

   优点：指定静态文件，或者接口设置词库实现热更新词库，不用重启 `ES`，是 `IK` 原生自带的

   缺点：需要通过 `Modified` 和 `tag` 两个 `Http` 响应头，来提供词库的热更新，有时候会不生效

具体使用就不说了，在这里具体说第二种方案



### 10.2.方案二:通过定时读取Mysql完成词库的热更新

首先要下载 `IK` 分词器的源码

网址:https://github.com/medcl/elasticsearch-analysis-ik

下载的时候一定要选对版本，保持和 `ES` 的版本一致，否则会启动的时候报错,版本不一致

接着把源码导入 `IDEA` 中，并在 `POM.xml` 中添加 `Mysql` 的依赖，根据自己的 `Mysql` 版本需要添加

我的 `Mysql` 是 `5.6.1` 所以添加 `5` 的驱动包

```
<!-- https://mvnrepository.com/artifact/mysql/mysql-connector-java -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.49</version>
        </dependency>
```

然后再 `config` 目录下创建一个新的 `.properties` 配置文件

![image-20210306213653363](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306213653363.png)

 在里面配置 `Mysql` 的一些配置,以及我们需要的配置

```
jdbc.url=jdbc:mysql://192.168.43.154:3306/es?characterEncoding=UTF-8&serverTimezone=GMT&nullCatalogMeansCurrent=true
jdbc.user=root
jdbc.password=root
# 更新词库
jdbc.reload.sql=select word from hot_words
# 更新停用词词库
jdbc.reload.stopword.sql=select stopword as word from hot_stopwords
# 重新拉取时间间隔
jdbc.reload.interval=5000
```

创建一个新的线程，用于调用 `Dictionary` 得 `reLoadMainDict()` 方法重新加载词库

![image-20210306213729281](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306213729281.png)

```java
package org.wltea.analyzer.dic;

import org.wltea.analyzer.help.ESPluginLoggerFactory;

public class HotDicReloadThread implements Runnable{

    private static final org.apache.logging.log4j.Logger logger = ESPluginLoggerFactory.getLogger(Dictionary.class.getName());

    @Override
    public void run() {

        while (true){
            logger.info("-------重新加载mysql词典--------");

            Dictionary.getSingleton().reLoadMainDict();
        }

    }
}
```

修改 `org.wltea.analyzer.dic` 文件夹下的 `Dictionary`

在 `Dictionary` 类中加载 `mysql` 驱动类

```java
private static Properties prop = new Properties();

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("error", e);
        }
    }
```

接着,创建重 `Mysql` 中加载词典的方法

```java
	/**
     * 从mysql中加载热更新词典
     */
    private void loadMySqlExtDict(){
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            Path file = PathUtils.get(getDictRoot(),"jdbc-reload.properties");
            prop.load(new FileInputStream(file.toFile()));

            logger.info("-------jdbc-reload.properties-------");
            for (Object key : prop.keySet()) {
                logger.info("key:{}", prop.getProperty(String.valueOf(key)));
            }

            logger.info("------- 查询词典, sql:{}-------", prop.getProperty("jdbc.reload.sql"));

            // 建立mysql连接
            connection = DriverManager.getConnection(
                    prop.getProperty("jdbc.url"),
                    prop.getProperty("jdbc.user"),
                    prop.getProperty("jdbc.password")
            );

            // 执行查询
            statement = connection.createStatement();
            resultSet = statement.executeQuery(prop.getProperty("jdbc.reload.sql"));

            // 循环输出查询啊结果,添加到Main.dict中去
            while (resultSet.next()) {
                String theWord = resultSet.getString("word");
                logger.info("------热更新词典:{}------", theWord);

                // 加到mainDict里面
                _MainDict.fillSegment(theWord.trim().toCharArray());
            }
        } catch (Exception e) {
            logger.error("error:{}", e);
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e){
                logger.error("error", e);
            }
        }
    }
```

接着，创建加载停用词词典方法

```java
	/**
     * 从mysql中加载停用词
     */
    private void loadMySqlStopwordDict(){
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            Path file = PathUtils.get(getDictRoot(), "jdbc-reload.properties");
            prop.load(new FileInputStream(file.toFile()));

            logger.info("-------jdbc-reload.properties-------");
            for(Object key : prop.keySet()) {
                logger.info("-------key:{}", prop.getProperty(String.valueOf(key)));
            }

            logger.info("-------查询停用词, sql:{}",prop.getProperty("jdbc.reload.stopword.sql"));

            conn = DriverManager.getConnection(
                    prop.getProperty("jdbc.url"),
                    prop.getProperty("jdbc.user"),
                    prop.getProperty("jdbc.password"));
            stmt = conn.createStatement();
            rs = stmt.executeQuery(prop.getProperty("jdbc.reload.stopword.sql"));

            while(rs.next()) {
                String theWord = rs.getString("word");
                logger.info("------- 加载停用词 : {}", theWord);
                _StopWords.fillSegment(theWord.trim().toCharArray());
            }

            Thread.sleep(Integer.valueOf(String.valueOf(prop.get("jdbc.reload.interval"))));
        } catch (Exception e) {
            logger.error("error", e);
        } finally {
            try {
                if(rs != null) {
                    rs.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
                if(conn != null) {
                    conn.close();
                }
            } catch (SQLException e){
                logger.error("error:{}", e);
            }

        }
    }
```

接下来,分别在 `loadMainDict()` 方法和 `loadStopWordDict()` 方法结尾处调用

```java
	/**
     * 加载主词典及扩展词典
     */
    private void loadMainDict() {
        // 建立一个主词典实例
        _MainDict = new DictSegment((char) 0);

        // 读取主词典文件
        Path file = PathUtils.get(getDictRoot(), Dictionary.PATH_DIC_MAIN);
        loadDictFile(_MainDict, file, false, "Main Dict");
        // 加载扩展词典
        this.loadExtDict();
        // 加载远程自定义词库
        this.loadRemoteExtDict();
        // 加载Mysql外挂词库
        this.loadMySqlExtDict();
    }
```



```java
	/**
     * 加载用户扩展的停止词词典
     */
    private void loadStopWordDict() {
        // 建立主词典实例
        _StopWords = new DictSegment((char) 0);

        // 读取主词典文件
        Path file = PathUtils.get(getDictRoot(), Dictionary.PATH_DIC_STOP);
        loadDictFile(_StopWords, file, false, "Main Stopwords");

        // 加载扩展停止词典
        List<String> extStopWordDictFiles = getExtStopWordDictionarys();
        if (extStopWordDictFiles != null) {
            for (String extStopWordDictName : extStopWordDictFiles) {
                logger.info("[Dict Loading] " + extStopWordDictName);

                // 读取扩展词典文件
                file = PathUtils.get(extStopWordDictName);
                loadDictFile(_StopWords, file, false, "Extra Stopwords");
            }
        }

        // 加载远程停用词典
        List<String> remoteExtStopWordDictFiles = getRemoteExtStopWordDictionarys();
        for (String location : remoteExtStopWordDictFiles) {
            logger.info("[Dict Loading] " + location);
            List<String> lists = getRemoteWords(location);
            // 如果找不到扩展的字典，则忽略
            if (lists == null) {
                logger.error("[Dict Loading] " + location + " load failed");
                continue;
            }
            for (String theWord : lists) {
                if (theWord != null && !"".equals(theWord.trim())) {
                    // 加载远程词典数据到主内存中
                    logger.info(theWord);
                    _StopWords.fillSegment(theWord.trim().toLowerCase().toCharArray());
                }
            }
        }

        // 加载Mysql停用词词库
        this.loadMySqlStopwordDict();

    }
```

最后在 `initial()` 方法中启动更新线程

```java
	/**
     * 词典初始化 由于IK Analyzer的词典采用Dictionary类的静态方法进行词典初始化
     * 只有当Dictionary类被实际调用时，才会开始载入词典， 这将延长首次分词操作的时间 该方法提供了一个在应用加载阶段就初始化字典的手段
     *
     * @return Dictionary
     */
    public static synchronized void initial(Configuration cfg) {
        if (singleton == null) {
            synchronized (Dictionary.class) {
                if (singleton == null) {

                    singleton = new Dictionary(cfg);
                    singleton.loadMainDict();
                    singleton.loadSurnameDict();
                    singleton.loadQuantifierDict();
                    singleton.loadSuffixDict();
                    singleton.loadPrepDict();
                    singleton.loadStopWordDict();

                    // 执行更新mysql词库的线程
                    new Thread(new HotDicReloadThread()).start();

                    if(cfg.isEnableRemoteDict()){
                        // 建立监控线程
                        for (String location : singleton.getRemoteExtDictionarys()) {
                            // 10 秒是初始延迟可以修改的 60是间隔时间 单位秒
                            pool.scheduleAtFixedRate(new Monitor(location), 10, 60, TimeUnit.SECONDS);
                        }
                        for (String location : singleton.getRemoteExtStopWordDictionarys()) {
                            pool.scheduleAtFixedRate(new Monitor(location), 10, 60, TimeUnit.SECONDS);
                        }
                    }

                }
            }
        }
    }
```

然后，修改 `src/main/assemblies/plugin.xml` 文件中，加入`Mysql`

```
		<dependencySet>
            <outputDirectory>/</outputDirectory>
            <useProjectArtifact>true</useProjectArtifact>
            <useTransitiveFiltering>true</useTransitiveFiltering>
            <includes>
                <include>mysql:mysql-connector-java</include>
            </includes>
        </dependencySet>
```

源码至此修改完成，在自己的数据库中创建两张新的表

建表 `SQL`

```
CREATE TABLE hot_words (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	word varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '词语',
	PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE TABLE hot_stopwords (
	id bigint(20) NOT NULL AUTO_INCREMENT,
	stopword varchar(50) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '停用词',
	PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
```

接下来对源码进行打包:

打包之前检查自己的 `POM.xml` 中的 `elasticsearch.version` 的版本，记得和自己的 `ES` 的版本对应，否则到时候会报错

![image-20210306214035983](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214035983.png)

检查完毕后,点击 `IDEA` 右侧的 `package` 进行项目打包,如果版本不对,修改版本并点击 `IDEA` 右侧的刷新同步,进行版本的更换,然后打包

![image-20210306214056152](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214056152.png)

 打包完成后在左侧项目中会出现 `target` 目录,会看到一个 `zip` ,我的是因为解压了,所以有文件夹

![image-20210306214118479](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214118479.png)

点击右键在文件夹中展示,然后使用解压工具解压

![image-20210306214139342](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214139342.png)

解压完成后,双击进入

![image-20210306214158884](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214158884.png)

先把原来 `ES` 下的 `plugins` 下的 `IK` 文件夹中的东西删除,可以先备份,然后把自己打包解压后里面的东西全部拷贝到 `ES` 下的 `plugins` 下的 `IK` 文件夹中

![image-20210306214221905](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214221905.png)

接下来进入 `bin` 目录下启动就可以了

当然按照惯例,我的启动时不会那么简单的,很高兴,我的报错了,所有的坑都踩了一遍,之前的版本不对就踩了两次

第一次是源码下载的版本不对

第二次的 `ES` 依赖版本不对

好了说报错:报错只贴主要内容

第三次报错:

```
Caused by: java.security.AccessControlException: access denied ("java.lang.RuntimePermission" "setContextClassLoader")
```

这个是 `JRE` 的类的创建设值权限不对

在 `jre/lib/security` 文件夹中有一个 `java.policy` 文件,在其 `grant{}` 中加入授权即可

```
permission java.lang.RuntimePermission "createClassLoader"; 
permission java.lang.RuntimePermission "getClassLoader"; 
permission java.lang.RuntimePermission "accessDeclaredMembers";
permission java.lang.RuntimePermission "setContextClassLoader";
```

第四次报错:

```
Caused by: java.security.AccessControlException: access denied ("java.net.SocketPermission" "192.168.43.154:3306" "connect,resolve")
```

这个是通信链接等权限不对

也是,在 `jre/lib/security` 文件夹中有一个 `java.policy` 文件,在其 `grant{}` 中加入授权即可

```
permission java.net.SocketPermission "192.168.43.154:3306","accept";
permission java.net.SocketPermission "192.168.43.154:3306","listen";
permission java.net.SocketPermission "192.168.43.154:3306","resolve";
permission java.net.SocketPermission "192.168.43.154:3306","connect";
```

到此之后启动无异常

最后就是测试了,启动我的 `head` 插件和 `kibana`,这两个没有或者不会的可以看我之前写的,也可以百度

执行分词

![image-20210306214331740](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214331740.png)

但是我想要 天青色

在 `Mysql` 中添加记录

```
insert into hot_words(word) value("天青色");
```

重新执行

![image-20210306214356859](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214356859.png)

也比如我想要这就是一个词 天青色等烟雨

在 `Mysql` 中添加记录

```
insert into hot_words(word) value("天青色等烟雨");
```

再次执行

![image-20210306214429920](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306214429920.png)

到此实现了 `ES` 定时从 `mysql` 中读取热词,停用词这个一般用的比较少,有兴趣自己测测,在使用的时候,通过业务系统往数据库热词表和停用词表添加记录就可以了





















**参考文章：**

[ElasticSearch 分词器](https://www.cnblogs.com/codeshell/p/14389403.html)