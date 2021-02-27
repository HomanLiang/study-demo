[toc]

# ElasticSearch 数据建模



通常在使用 ES 构建数据模型时，需要考虑以下几点：

- 字段类型
- 是否需要搜索与分词
- 是否需要聚合与排序
- 是否需要额外的存储



## 字段类型

对于不同类型的数据，主要考虑下面几点：

- 对于Text 类型：用于全文本字段，数据会被分词器分词。
  - 默认不支持聚合分析及排序，需要设置 [fielddata](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/fielddata.html) 为 `true`。
- 对于Keyword 类型：用于不需要分词处理的文本，例如手机号，email 地址，性别等。
  - 适用于精确匹配，支持聚合与排序。
- 对于多字段类型：默认情况下，ES 会为将文本设置为text 类型，并添加一个keyword子字段。
  - 在处理人类语言时，可以通过增加“英文”，“拼音”和“标准”分词器，来满足搜索需求。
- 对于**数值类型**：尽量选择贴近的类型。比如 **byte** 类型能满足需求，就不要用 **long**。



## 搜索需求

对于搜索需求，主要考虑以下几点：

- 如果不需要检索，排序和聚合，可将 [enabled](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/enabled.html) 设置成 `false`，以减少不必要的处理（磁盘开销），来提高性能。
- 如果不需要检索，但需要排序与聚合，可将 [index](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/mapping-index.html) 设置成 `false`。



## 聚合与排序

对于聚合与排序，主要考虑以下几点：

- 如果不需要检索，排序和聚合，可将 [enabled](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/enabled.html) 设置成 `false`。
- 如果需要检索，但不需要排序与聚合，可将 [doc_values](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/doc-values.html) 和 [fielddata](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/fielddata.html) 设置成 `false`。
- 对于**keyword** 类型的字段，如果更新与聚合比较频繁，推荐将 [eager_global_ordinals](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/eager-global-ordinals.html) 设置为 `true`（可以达到利用缓冲的目的，提高性能）。



## 额外存储

将 [store](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/mapping-store.html) 设置为 `true`（默认为 `false`），可以存储字段的原始内容；一般在 [_source](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/mapping-source-field.html) 的 [enabled](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/enabled.html) 为 `false` 时使用。



## 示例

如果需要对一些图书信息进行建模，需求如下：

- 书名：支持全文本及精确匹配
- 简介：支持全文本
- 作者：支持精确匹配
- 出版日期：日期类型
- 图书封面：不需要支持搜索

示例数据如下：

```shell
{
  "title":"Mastering ElasticSearch 5.0",
  "description":"Master the searching, indexing, and aggregation features in ElasticSearch Improve users’ search experience with Elasticsearch’s functionalities and develop your own Elasticsearch plugins",
  "author":"Bharvi Dixit",
  "public_date":"2017",
  "cover_url":"https://images-na.ssl-images-amazon.com/images/I/51OeaMFxcML.jpg"
}
```

如果不手动设置 mapping，那么每个字段将被 ES 设置为如下类型：

```shell
{
  "type" : "text",   # text 类型
  "fields" : {       # 并添加一个 keyword 子字段
    "keyword" : {
      "type" : "keyword",
      "ignore_above" : 256
    }
  }
}
```



### 手动设置 mapping

下面根据需求，手动设置 mapping：

```shell
PUT books
{
	"mappings": {
		"properties": {
			"author": {
				"type": "keyword"
			},
			"cover_url": {
				"type": "keyword",
				"index": false      # 不需要支持搜索
			},
			"description": {
				"type": "text"
			},
			"public_date": {
				"type": "date"
			},
			"title": {
				"type": "text",
				"fields": {
					"keyword": {
						"type": "keyword",
						"ignore_above": 100
					}
				}
			}
		}
	}
}
```



### 增加需求

如果现在需要添加一个字段 `content`，用于存储图书的内容，因此该字段的**信息量将非常大**，这将**导致 _source 的内容过大**，导致过大的网络开销。

为了优化，可以将 **_source** 的 **enabled** 设置为 `false`，然后将每个字段的 **store** 设置为 `true`（打开**额外存储**）。

如下：

```shell
PUT books
{
	"mappings": {
		"_source": {
			"enabled": false    # enabled 为 false
		},
		"properties": {
			"author": {
				"type": "keyword",
				"store": true   # store 为 true
			},
			"cover_url": {
				"type": "keyword",
				"index": false,
				"store": true  # store 为 true
			},
			"description": {
				"type": "text",
				"store": true  # store 为 true
			},
			"content": {
				"type": "text",
				"store": true  # store 为 true
			},
			"public_date": {
				"type": "date",
				"store": true # store 为 true
			},
			"title": {
				"type": "text",
				"fields": {
					"keyword": {
						"type": "keyword",
						"ignore_above": 100
					}
				},
				"store": true # store 为 true
			}
		}
	}
}
```

将 **_source** 禁止掉之后，查询的结果中就没有了 _source 字段；如果需要哪些字段的内容，则需要设置 **stored_fields**，如下：

```shell
POST books/_search
{
  "stored_fields": ["title","author","public_date"],
  "query": {
    "match": {
      "content": "searching"
    }
  }
}
```





参考文章：

[ElasticSearch 数据建模](https://www.cnblogs.com/codeshell/p/14445450.html)