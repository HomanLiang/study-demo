[toc]



# ElasticSearch 搜索模板与建议

[Search APIs](https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html) 用于**搜索**和**聚合**存储在 `ES` 中的数据。



## 1.搜索模板 Template

[Search 模板](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-template.html)使用 **mustache** 语言来呈现搜索请求，使得搜索请求**参数化**，达到开发人员与搜索工程师解耦的效果。

示例：

```shell
# 定义一个搜索模板
POST _scripts/template_name # 模板名称为 template_name 
{
  "script": {           # 固定写法
    "lang": "mustache", # 固定写法
    "source": {         # 固定写法
      "_source": ["title", "overview"], # 返回哪些字段
      "size": 20,
      "query": {       
        "multi_match": {
          "query": "{{q}}",     # 搜索参数，参数名为 q
          "fields": ["title", "overview"]
        }
      }
    }
  }
}

# 使用模板
POST template_name/_search/template
{
    "id":"template_name",
    "params": {  # 填写参数 q
        "q": "basketball with cartoon aliens"
    }
}
```



## 2.搜索建议 Suggesters

[搜索建议](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-suggesters.html)帮助用户在输入搜索的过程中，进行自动补全或者纠错，这种功能在 `ES` 中通过 **Suggesters** 来完成。

**Suggesters** 会将输入的文本分解为 `token`，然后在索引的字典里查找相似的 `Term` 并返回。

`ES` 提供了 4 种类别的 **Suggesters**：

- **Term suggester**
- **Phrase Suggester**
- **Completion Suggester**
- **Context Suggester**

**Suggester Mode** 用于指定在什么情况下给出搜索建议：

- **missing**：搜索不到内容时，给出搜索建议
- **popular**：推荐出现频率更高的词
- **always**：无论是否存在，都给出搜索建议



### 2.1.Term suggester

示例：

```shell
DELETE articles

# 插入一些文档
POST articles/_bulk
{ "index" : { } }
{ "title_completion": "lucene is very cool"}
{ "index" : { } }
{ "title_completion": "Elasticsearch builds on top of lucene"}
{ "index" : { } }
{ "title_completion": "Elasticsearch rocks"}
{ "index" : { } }
{ "title_completion": "elastic is the company behind ELK stack"}
{ "index" : { } }
{ "title_completion": "Elk stack rocks"}
{ "index" : {} }
```

**term-suggestion** 搜索：

```shell
POST /articles/_search
{
  "size": 1,
  "query": {
    "match": {
      "body": "lucen rock"   # 查询字符串，拼写错误
    }
  },
  "suggest": {               # 搜索建议
    "term-suggestion": {     # suggester 名称，自定义
      "text": "lucen rock",  # 这里也是查询字符串
      "term": {              # term-suggestion
        "suggest_mode": "missing",  # 建议模式，在没有搜索到时，到 body 字段搜索
        "field": "body"             # 建议字段
      }
    }
  }
}
```

一些参数：

- **sort**：排序方法，默认按照评分排序，也可以按照 **frequency** 排序。
- **prefix_length**：默认情况下，首字母不一致就不会给出建议词。如果将其设置为 0，就会为 `hock` 建议 `rock`。

示例：

```shell
POST /articles/_search
{

  "suggest": {
    "term-suggestion": {
      "text": "lucen hocks",
      "term": {
        "suggest_mode": "always",
        "field": "body",
        "prefix_length":0,
        "sort": "frequency"
      }
    }
  }
}
```



### 2.2.Phrase Suggester

`Phrase Suggester` 在 `Term Suggester` 的基础上增加了一些额外的逻辑。

一些参数：

- `max_errors`：最多可以拼错的 Terms 数
- `confidence`：限制返回结果数，默认为 1。值为 0 的话，表示返回前 N 个结果。

示例：

```shell
POST /articles/_search
{
  "suggest": {
    "my-suggestion": {  # 一个 Suggester，自定义名称
      "text": "lucne and elasticsear rock hello world ", # 搜索字符串
      "phrase": {           # phrase-suggestion
        "field": "body",
        "max_errors":2,
        "confidence":0,
        "direct_generator":[{
          "field":"body",
          "suggest_mode":"always"
        }],
        "highlight": {
          "pre_tag": "<em>",
          "post_tag": "</em>"
        }
      }
    }
  }
}
```



### 2.3.Completion Suggester

`Completion Suggester` 用于自动补全，用户每输入一个字符，就需要即时发送一个查询请求到后端查找匹配项。

自动补全功能对性能要求比较高，`ES` 采用了不同的数据结构，而非通过倒排索引来完成。

自动补全功能将分词数据编码成 `FST`，与索引放在一起。`FST` 会被加载到内存中，以加快速度。`FSF` 的缺点是只能用于**前缀查找**。

示例：

首先，字段的类型必须是 [completion](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/search-suggesters.html#completion-suggester)，该数据类型用于自动补全功能。

```shell
DELETE articles
PUT articles
{
  "mappings": {
    "properties": {
      "title_completion":{
        "type": "completion"
      }
    }
  }
}
```

定义好数据类型之后才能插入数据：

```shell
POST articles/_bulk
{ "index" : { } }
{ "title_completion": "lucene is very cool"}
{ "index" : { } }
{ "title_completion": "Elasticsearch builds on top of lucene"}
{ "index" : { } }
{ "title_completion": "Elasticsearch rocks"}
{ "index" : { } }
{ "title_completion": "elastic is the company behind ELK stack"}
{ "index" : { } }
{ "title_completion": "Elk stack rocks"}
{ "index" : {} }
```

查询数据：

```shell
POST articles/_search?pretty
{
  "size": 0,
  "suggest": {
    "article-suggester": { # 自定义名称
      "prefix": "elk ",    # 查询前缀，基于该前缀进行自动补全
      "completion": {      # completion 
        "field": "title_completion"  # 字段
      }
    }
  }
}
```



### 2.4.Context Suggester

`Context Suggester` 是对 `Completion Suggester` 的扩展，称为**上下文感知推荐**，可以在搜索时提供更多的上下文信息。

例如，输入 "star"：

- 咖啡相关：建议 "starbucks"
- 电影相关：建议 "start wars"

可以定义两种类型的上下文：

- `Category`：任意字符串
- `Geo`：地理位置信息

实现 `Context Suggester` 的步骤：

1. 定义一个 `Mapping`
2. 索引数据，并且为每个文档加入 `Context` 信息
3. 查询

示例：

```shell
# 设置 Mapping
PUT comments
PUT comments/_mapping
{
  "properties": {
    "comment_autocomplete":{      # 字段名称
      "type": "completion",       # 字段类型
      "contexts":[{               # Context Suggester，是一个数组
        "type":"category",        # 上下文类型
        "name":"comment_category" # 名称
      }]
    }
  }
}

# 写入数据
POST comments/_doc
{
  "comment":"I love the star war movies",
  "comment_autocomplete":{
    "input":["star wars"],        # 如果请求 movies 类型的数据，就返回 "star wars"
    "contexts":{
      "comment_category":"movies" # 自定义 movies 类型
    }
  }
}

POST comments/_doc
{
  "comment":"Where can I find a Starbucks",
  "comment_autocomplete":{
    "input":["starbucks"],        # 如果请求 coffee 类型的数据，就返回 "starbucks"
    "contexts":{
      "comment_category":"coffee" # 自定义 coffee 类型
      }
    }
  }
}

# 查询
POST comments/_search
{
  "suggest": {
    "MY_SUGGESTION": {     # 自定义名称
      "prefix": "sta",     # 搜索字符串前缀
      "completion":{       # 自动补全，固定写法
        "field":"comment_autocomplete",  # 字段名称
        "contexts":{      
          "comment_category":"coffee"    # 请求 coffee 类型的数据
        }
      }
    }
  }
}
```



### 2.5.几种建议的指标比较

比较：

- 精准度： `Completion > Phrase > Term`
- 召回率：`Term > Phrase > Completion`
- 性能：`Completion > Phrase > Term`