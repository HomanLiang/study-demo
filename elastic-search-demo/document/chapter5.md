[toc]



# 分词

## 参考文档

- [官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.4/analysis.html)



## 分词简介

ES 作为一个开源的搜索引擎，其核心自然在于搜索，而搜索不同于我们在 MySQL 中的 select 查询语句，无论我们在百度搜索一个关键字，或者在京东搜索一个商品时，常常无法很准确的给出一个关键字，例如我们在百度希望搜索“Java教程”，我们希望结果是“Java教程”、“Java”、“Java基础教程”，甚至是“教程Java”。MySQL虽然能满足前三种查询结果，但无法满足最后一种搜索结果。

虽然我们很难做到对于百度或者京东的搜索（这甚至需要了解Lucene和搜索的底层原理），但我们能借助ES做出一款不错的搜索产品。

ES的搜索中，分词是非常重要的概念。掌握分词原理，对待一个不甚满意的搜索结果我们能定位是哪里出了问题，从而做出相应的调整。

ES中，只对字符串进行分词，在ElasticSearch2.x版本中，字符串类型只有string，ElasticSearch5.x版本后字符串类型分为了text和keyword类型，需要明确的分词只有text类型。

ES的默认分词器是standard，对于英文搜索它没有问题，但对于中文搜索它会将所有的中文字符串挨个拆分，也就是它会将“中国”拆分为“中”和“国”两个单词，这带来的问题会是搜索关键字为“中国”时，将不会有任何结果，ES会将搜索字段进行拆分后搜索。当然，你可以指定让搜索的字段不进行分词，例如设置为keyword字段。



## ES常用内置分词器

### standard

支持中英文，中文会议单个字切割。他会将词汇单元转换成小写形式，并去除停用词和标点符号

示例：

```
POST _analyze
{
  "analyzer": "standard",
  "text": ["my name is 张某某 X"]
}
```

返回结果：

![image-20210221181543854](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210221181543854.png)



### simple

首先会通过非字母字符来分割文本信息，然后将词汇单元统一为小写形式。该分析器会去掉数字类型的字符。中文原样输出



示例：

```
POST _analyze
{
  "analyzer": "simple",
  "text": ["my name is 张某某 X"]
}
```

返回结果：

![image-20210221181822018](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210221181822018.png)



### whitespace 



示例：

```
POST _analyze
{
  "analyzer": "whitespace",
  "text": ["my name is 张某某 X"]
}
```

返回结果：

仅仅是去除空格，对字符没有lowcase化并且不对生成的词汇单元进行其他的规范化处理。

![image-20210221181913326](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210221181913326.png)



### stop

小写处理，停用词过滤(the,a,is)



### keyword 

不分词，直接将输入当作输出



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



