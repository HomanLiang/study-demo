[toc]



# ElasticSearch 分词器

## 参考文档

- [官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.4/analysis.html)



## 精确值与全文本

ES 中有 **精确值**（Exact Values）与 **全文本**（Full Text）之分：

- 精确值：包括数字，日期，一个具体字符串（例如"Hello World"）。
  - 在 ES 中用 [keyword](https://www.elastic.co/guide/en/elasticsearch/reference/current/keyword.html) 数据类型表示。
  - 精确值不需要做分词处理。
- 全文本：非结构化的文本数据
  - 在 ES 中用 [text](https://www.elastic.co/guide/en/elasticsearch/reference/current/text.html) 数据类型表示。
  - 全文本需要做分词处理。

示例：

![image-20210223225131176](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210223225131176.png)



## 分词过程

搜索引擎需要建立单词（Term / Token）与倒排索引项的对应关系，那么首先就需要将文档拆分为单词，这个过程叫做分词。

比如将 hello world 拆分为 hello 和 world，这就是分词过程。



## 分词器

ES 使用分词器（Analyzer）对文档进行分词，ES 中内置了很多分词器供我们使用，我们也可以定制自己的分词器。

一个分词器有 3 个组成部分，分词过程会依次经过这些部分：

1. Character Filters：字符过滤，用于删去某些字符。该组件可以有 0 或多个。
1. Tokenizer：分词过程，按照某个规则将文档切分为单词，比如用空格来切分。该组件有且只能有一个。
1. Token Filter：对切分好的单词进一步加工，比如大小写转换，删除停用词等。该组件可以有 0 或多个。



## ES 中的分词器

ES 有下面这些[内置的分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html)：

- Standard Analyzer：默认分词器，按词切分，转小写处理，也可以过滤停用词（默认关闭）。
  - 在 ES 中的名称为 `standard`

- Simple Analyzer：按照非字母切分，非字母会被去除，转小写处理。
  - 在 ES 中的名称为 `simple`
- Stop Analyzer：按照非字母切分，非字母会被去除，转小写处理，停用词过滤(the、a、is 等)。
  - 在 ES 中的名称为 `stop`
- Whitespace Analyzer：按照空格切分，不转小写。
  - 在 ES 中的名称为 `whitespace`
- Keyword Analyzer：不做任何的分词处理，直接将输入当作输出。
  - 在 ES 中的名称为 keyword
- Pattern Analyzer：通过正则表达式进行分词，默认为\W+非字符分隔，然后会进行转小写处理。
  - 在 ES 中的名称为 `pattern`
- Language Analyzers：提供了30多种常见语言的分词器，比如：
  - `english`：英语分词器，会对英文单词进行归一化处理，去掉停用词等。
    - 归一化处理：比如 `running` 变为 `run`，`goods` 变为 `good` 等。



## 测试分词器

我们可以通过下面的 API 来测试分词器：

```
GET _analyze
{
  "analyzer": "AnalyzerName",
  "text": "内容"
}
```



## 自定义分词器

当 ES 中的内置分词器不能满足需求时，我们可以[定制自己的分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-custom-analyzer.html)。

在上文中已经介绍过一个分词器由 3 部分组成：

- Character Filters：字符过滤，用于删去某些字符。
  - 该组件可以有 0 或多个。
- Tokenizer：分词过程，按照某个规则将文档切分为单词，比如用空格来切分。
  - 该组件有且只能有一个。
- Token Filter：对切分好的单词进一步加工，比如大小写转换，删除停用词等。
  - 该组件可以有 0 或多个。



### 内置分词器组件

ES 对这 3 部分都有内置：

- 内置 Character Filters
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



### 自定义分词器

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



## 安装分词器插件

我们还可以通过**安装插件**的方式，来安装其它分词器。

比如安装 **analysis-icu** 分词器，它是一个不错的中文分词器：

```
elasticsearch-plugin install analysis-icu
```

**analysis-icu** 在 ES 中的名称为 `icu_analyzer`。

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



## IK分词器

### IK分词器的安装

**下载地址：**https://github.com/medcl/elasticsearch-analysis-ik/releases在这上面有elasticsearch所对应版本的IK分词器以编译的包，可以在上面找到对应版本进行下载使用

1. 下载：

   ```
   wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.5.1/elasticsearch-analysis-ik-7.5.1.zip
   ```

   

2. 在解压到elasticsearch/plugin/ik/文件夹中，ik文件夹需要自己建

   ```
   unzip elasticsearch-analysis-ik-7.5.1.zip -d $ES_HOME/plugins/ik/
   ```

   把$ES_HOME替换成你elasticsearch所在目录即可

3. 重启elasticsearch后就可以用了



### 使用ik分词器

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

IK分词器示例：

1. ik_smart:会做最粗粒度的拆分

   ```
   POST _analyze
   {
     "analyzer": "ik_smart",
     "text": ["我是中国人"]
   }
   ```

   ![image-20210221190812724](C:\Users\hmliang\AppData\Roaming\Typora\typora-user-images\image-20210221190812724.png)

2. ik_max_word:会将文本做最细粒度的拆分

   ```
   POST _analyze
   {
     "analyzer": "ik_max_word",
     "text": ["我是中国人"]
   }
   ```

   ![image-20210221190902406](C:\Users\hmliang\AppData\Roaming\Typora\typora-user-images\image-20210221190902406.png)



### 扩展词典

在`$ES_HOME/plugins/ik/elasticsearch-analysis-ik-7.5.1/config/IKAnalyzer.cfg.xml` 配置文件下

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

其中custom.dic为自定义的扩展字典， 内容可以根据自己需要，自定义分词的文字。

保存后重启elasticsearch即可看到自己想要的分词效果。









**参考文章：**

[ElasticSearch 分词器](https://www.cnblogs.com/codeshell/p/14389403.html)