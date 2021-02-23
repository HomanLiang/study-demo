[toc]



# ElasticSearch 查询

ES 中的查询 API 有两种：

- **URI Search**：HTTP GET 请求的方式。
- **Request Body Search**：基于 Json 数据格式的 **DSL**（Query Domain Specific Language）。



## 指定查询范围

通过 URI 可以指定在哪些索引中进行查询，有下面几种格式：

- `/_search`：在所有的索引中进行搜索。

- `/index_name/_search`：在 `index_name` 索引中进行搜索。

- `/index1,index2/_search`：在 `index1` 和 `index2` 索引中进行搜索。

- `/index*/_search`：在所有的以 `index` 为前缀的索引中进行搜索。

  

## URI 查询

URI 查询使用 HTTP GET 请求的方式，使用 `q` 指定查询的内容，格式如下：

```shell
curl -XGET http://localhost:9200/index_name/_search?q=key:val
```

简写为：

```shell
GET /index_name/_search?q=key:val
```

其表示的含义是：在 `index_name` 索引中的所有文档中，查询 `key` 字段的值为 `val` 的内容。



## Request Body 查询

Request Body 查询可以使用 GET 或 POST 方式，格式如下：

```
curl -XGET/POST http://localhost:9200/index_name/_search -H 'Content-Type: application/json' -d'
{
 "query": {
 "match_all":{}
 }
}'
```

简写为：

```
POST index_name/_search
{
	"query": {
		"match_all": {}
	}
}
```



## ES 查询的响应内容

如果查询成功，会返回如下格式的内容：

![image-20210223231349880](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210223231349880.png)

返回的结果集会以 `_score` 评分进行排序，`_score` 评分指的是查询的相关性。



## 相关性指标

搜索的相关性有 3 种衡量指标：

- **查准率**：尽可能返回较少的无关文档。
- **查全率**：尽可能返回较多的相关文档。
- **结果排名**：查询结果排名是否准确。

在 ES 中可以通过调整查询的参数来改善搜素的查准率和查全率。







**参考文章：**

[ElasticSearch 查询](https://www.cnblogs.com/codeshell/p/14389415.html)