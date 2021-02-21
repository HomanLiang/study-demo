[toc]



# REST APIs

ES 提供了多种操作数据的方式，其中较为常见的方式就是RESTful风格的API。

简单的体验：利用Postman发起HTTP请求（当然也可以在命令行中使用curl命令）。

[官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.4/rest-apis.html)



## Index APIs

### 创建索引

#### 简单创建索引

```
PUT /twitter
{
    "settings" : {
        "number_of_shards" : 3,
        "number_of_replicas" : 2
    }
}
```

number_of_shards：分片数，默认1

number_of_replicas：备份数，默认1

或者

```
PUT /twitter
```



#### 创建索引并指定索引的映射 Mapping

```
PUT /test
{
    "settings" : {
        "number_of_shards" : 1
    },
    "mappings" : {
        "properties" : {
            "field1" : { "type" : "text" }
        }
    }
}
```



#### 创建索引并给索引指定别名 Aliases

```
PUT /test
{
    "aliases" : {
        "alias_1" : {},
        "alias_2" : {
            "filter" : {
                "term" : {"user" : "kimchy" }
            },
            "routing" : "kimchy"
        }
    }
}
```



#### 创建成功返回结果

```
{
    "acknowledged": true,
    "shards_acknowledged": true,
    "index": "test"
}
```



### 修改索引

#### 修改索引设置 settings

```
PUT /twitter/_settings
{
    "index" : {
        "number_of_replicas" : 2
    }
}
```

#### 修改索引别名 Aliases

##### 增加别名

```console
POST /_aliases
{
    "actions" : [
        { "add" : { "index" : "twitter", "alias" : "alias1" } }
    ]
}
```

##### 移除别名

```
POST /_aliases
{
    "actions" : [
        { "remove" : { "index" : "test1", "alias" : "alias1" } }
    ]
}
```

##### 重命名别名

```
POST /_aliases
{
    "actions" : [
        { "remove" : { "index" : "test1", "alias" : "alias1" } },
        { "add" : { "index" : "test1", "alias" : "alias2" } }
    ]
}
```





### 查看指定索引

#### 查询索引

Request:

```
GET /<alias>
```

查询所有索引

Request:

```
GET /_all
```



#### 查询索引别名

```
GET /_alias

GET /_alias/<alias>

GET /<index>/_alias/<alias>
```



#### 查询索引设置

Request:

```
GET /<index>/_settings

GET /<index>/_settings/<setting>
GET /log_2013_-*/_settings/index.number_*
```



#### 查询映射

```
GET /_mapping

GET /<index>/_mapping
```



### 删除索引

#### 删除索引

Request:

```
DELETE /<index>
```

#### 删除别名

Request:

```
DELETE /<index>/_alias/<alias>

DELETE /<index>/_aliases/<alias>
```



### 分词 Analyze

#### 查询使用分词器

```
GET /_analyze

POST /_analyze

GET /<index>/_analyze

POST /<index>/_analyze
```

#### 测试分词器结果

```
GET /_analyze
{
  "analyzer" : "standard",
  "text" : "this is a test"
}
```

```
GET /_analyze
{
  "analyzer" : "standard",
  "text" : ["this is a test", "the second text"]
}
```





## Document APIs

### 插入文档

- Create document IDs automatically

```
POST twitter/_doc/
{
    "user" : "kimchy",
    "post_date" : "2009-11-15T14:12:12",
    "message" : "trying out Elasticsearch"
}
```

​	返回结果：

```
{
    "_shards" : {
        "total" : 2,
        "failed" : 0,
        "successful" : 2
    },
    "_index" : "twitter",
    "_type" : "_doc",
    "_id" : "W0tpsmIBdwcYyG50zbta",
    "_version" : 1,
    "_seq_no" : 0,
    "_primary_term" : 1,
    "result": "created"
}
```

- Insert a JSON document into the `twitter` index with an `_id` of 1:

```
PUT twitter/_doc/1
{
    "user" : "kimchy",
    "post_date" : "2009-11-15T14:12:12",
    "message" : "trying out Elasticsearch"
}
```

​	The API returns the following result:

```
{
    "_shards" : {
        "total" : 2,
        "failed" : 0,
        "successful" : 2
    },
    "_index" : "twitter",
    "_type" : "_doc",
    "_id" : "1",
    "_version" : 1,
    "_seq_no" : 0,
    "_primary_term" : 1,
    "result" : "created"
}
```

- Use the `_create` resource to index a document into the `twitter` index if no document with that ID exists:

```console
PUT twitter/_create/1
{
    "user" : "kimchy",
    "post_date" : "2009-11-15T14:12:12",
    "message" : "trying out Elasticsearch"
}
```

- Set the `op_type` parameter to *create* to index a document into the `twitter` index if no document with that ID exists:

```console
PUT twitter/_doc/1?op_type=create
{
    "user" : "kimchy",
    "post_date" : "2009-11-15T14:12:12",
    "message" : "trying out Elasticsearch"
}
```



### 查询文档

#### 查询单个文档

Request:

```
GET <index>/_doc/<_id>

HEAD <index>/_doc/<_id>

GET <index>/_source/<_id>

HEAD <index>/_source/<_id>
```
示例：

```
GET twitter/_doc/0
```

返回结果

```console-result
{
    "_index" : "twitter",
    "_type" : "_doc",
    "_id" : "0",
    "_version" : 1,
    "_seq_no" : 10,
    "_primary_term" : 1,
    "found": true,
    "_source" : {
        "user" : "kimchy",
        "date" : "2009-11-15T14:12:12",
        "likes": 0,
        "message" : "trying out Elasticsearch"
    }
}
```

#### Multi get API

Request:

```
GET /_mget

GET /<index>/_mget
```

示例：

```console
GET /test/_doc/_mget
{
    "docs" : [
        {
            "_id" : "1"
        },
        {
            "_id" : "2"
        }
    ]
}
```

或者

```console
GET /twitter/_mget
{
    "ids" : ["1", "2"]
}
```



### 修改文档

#### 单个文档更新

Request:

```
POST /<index>/_update/<_id>
```

示例：

```console
POST test/_update/1
{
    "doc" : {
        "name" : "new_name"
    }
}
```

返回结果

```console
{
   "_shards": {
        "total": 0,
        "successful": 0,
        "failed": 0
   },
   "_index": "test",
   "_type": "_doc",
   "_id": "1",
   "_version": 7,
   "_primary_term": 1,
   "_seq_no": 6,
   "result": "noop"
}
```



#### 单个文档Upsert

If the document does not already exist, the contents of the `upsert` element are inserted as a new document. If the document exists, the `script` is executed:

```console
POST test/_update/1
{
    "script" : {
        "source": "ctx._source.counter += params.count",
        "lang": "painless",
        "params" : {
            "count" : 4
        }
    },
    "upsert" : {
        "counter" : 1
    }
}
```

#### 单个文档 Scripted Upsert

To run the script whether or not the document exists, set `scripted_upsert` to `true`:

```console
POST sessions/_update/dh3sgudg8gsrgl
{
    "scripted_upsert":true,
    "script" : {
        "id": "my_web_session_summariser",
        "params" : {
            "pageViewEvent" : {
                "url":"foo.com/bar",
                "response":404,
                "time":"2014-01-01 12:32"
            }
        }
    },
    "upsert" : {}
}
```



#### 单个文档 Doc as Upsert

Instead of sending a partial `doc` plus an `upsert` doc, you can set `doc_as_upsert` to `true` to use the contents of `doc` as the `upsert` value:

```console
POST test/_update/1
{
    "doc" : {
        "name" : "new_name"
    },
    "doc_as_upsert" : true
}
```

#### 通过查询更新

```console
POST twitter/_update_by_query
{
  "script": {
    "source": "ctx._source.likes++",
    "lang": "painless"
  },
  "query": {
    "term": {
      "user": "kimchy"
    }
  }
}
```





### 删除文档

#### 单个文档删除

Request:

```
DELETE /<index>/_doc/<_id>
```

示例：

```console
DELETE /twitter/_doc/1
```

返回结果:

```console-result
{
    "_shards" : {
        "total" : 2,
        "failed" : 0,
        "successful" : 2
    },
    "_index" : "twitter",
    "_type" : "_doc",
    "_id" : "1",
    "_version" : 2,
    "_primary_term": 1,
    "_seq_no": 5,
    "result": "deleted"
}
```



#### 通过查询删除

Request:

```
POST /<index>/_delete_by_query
```

示例：

```console
POST /twitter/_delete_by_query
{
  "query": {
    "match": {
      "message": "some message"
    }
  }
}
```

返回结果

```console_result
{
  "took" : 147,
  "timed_out": false,
  "total": 119,
  "deleted": 119,
  "batches": 1,
  "version_conflicts": 0,
  "noops": 0,
  "retries": {
    "bulk": 0,
    "search": 0
  },
  "throttled_millis": 0,
  "requests_per_second": -1.0,
  "throttled_until_millis": 0,
  "failures" : [ ]
}
```



### Bulk API

Performs multiple indexing or delete operations in a single API call. This reduces overhead and can greatly increase indexing speed.

Request:

```
POST /_bulk

POST /<index>/_bulk
```

示例：

```
POST _bulk
{ "index" : { "_index" : "test", "_id" : "1" } }
{ "field1" : "value1" }
{ "delete" : { "_index" : "test", "_id" : "2" } }
{ "create" : { "_index" : "test", "_id" : "3" } }
{ "field1" : "value3" }
{ "update" : {"_id" : "1", "_index" : "test"} }
{ "doc" : {"field2" : "value2"} }
```

返回结果

```js
{
   "took": 30,
   "errors": false,
   "items": [
      {
         "index": {
            "_index": "test",
            "_type": "_doc",
            "_id": "1",
            "_version": 1,
            "result": "created",
            "_shards": {
               "total": 2,
               "successful": 1,
               "failed": 0
            },
            "status": 201,
            "_seq_no" : 0,
            "_primary_term": 1
         }
      },
      {
         "delete": {
            "_index": "test",
            "_type": "_doc",
            "_id": "2",
            "_version": 1,
            "result": "not_found",
            "_shards": {
               "total": 2,
               "successful": 1,
               "failed": 0
            },
            "status": 404,
            "_seq_no" : 1,
            "_primary_term" : 2
         }
      },
      {
         "create": {
            "_index": "test",
            "_type": "_doc",
            "_id": "3",
            "_version": 1,
            "result": "created",
            "_shards": {
               "total": 2,
               "successful": 1,
               "failed": 0
            },
            "status": 201,
            "_seq_no" : 2,
            "_primary_term" : 3
         }
      },
      {
         "update": {
            "_index": "test",
            "_type": "_doc",
            "_id": "1",
            "_version": 2,
            "result": "updated",
            "_shards": {
                "total": 2,
                "successful": 1,
                "failed": 0
            },
            "status": 200,
            "_seq_no" : 3,
            "_primary_term" : 4
         }
      }
   ]
}
```



## Search APIs

### Search

Request:

```
GET /<index>/_search

POST /<index>/_search

GET /_search

POST /_search
```

- 示例1：查询指定索引

```console
GET /twitter/_search?q=user:kimchy
```

​	返回结果：

```console-result
{
  "took" : 5,
  "timed_out" : false,
  "_shards" : {
    "total" : 1,
    "successful" : 1,
    "skipped" : 0,
    "failed" : 0
  },
  "hits" : {
    "total" : {
      "value" : 1,
      "relation" : "eq"
    },
    "max_score" : 1.3862944,
    "hits" : [
      {
        "_index" : "twitter",
        "_type" : "_doc",
        "_id" : "0",
        "_score" : 1.3862944,
        "_source" : {
          "date" : "2009-11-15T14:12:12",
          "likes" : 0,
          "message" : "trying out Elasticsearch",
          "user" : "kimchy"
        }
      }
    ]
  }
}
```

- 示例2：查询多个索引

  ```console
  GET /kimchy,elasticsearch/_search?q=user:kimchy
  ```



- 示例3：查询全部索引

  ```console
  GET /_search?q=user:kimchy
  
  GET /_all/_search?q=user:kimchy
  
  GET /*/_search?q=user:kimchy
  ```



### URI Search

完全通过URI查询，包括查询条件。

```console
GET twitter/_search?q=user:kimchy
```



### Request Body Search

通过方法体请求搜索（**后面会详细讲**）

Request:

```
GET /<index>/_search
{
  "query": {<parameters>}
}
```

示例：

```console
GET /twitter/_search
{
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}
```

返回结果：

```console
{
    "took": 1,
    "timed_out": false,
    "_shards":{
        "total" : 1,
        "successful" : 1,
        "skipped" : 0,
        "failed" : 0
    },
    "hits":{
        "total" : {
            "value": 1,
            "relation": "eq"
        },
        "max_score": 1.3862944,
        "hits" : [
            {
                "_index" : "twitter",
                "_type" : "_doc",
                "_id" : "0",
                "_score": 1.3862944,
                "_source" : {
                    "user" : "kimchy",
                    "message": "trying out Elasticsearch",
                    "date" : "2009-11-15T14:12:12",
                    "likes" : 0
                }
            }
        ]
    }
}
```





### Count API

获取匹配查询的文档数量

Request:

```
GET /<index>/_count
```

示例：

```console
PUT /twitter/_doc/1?refresh
{
    "user": "kimchy"
}

GET /twitter/_count?q=user:kimchy

GET /twitter/_count
{
    "query" : {
        "term" : { "user" : "kimchy" }
    }
}
```

返回结果：

```console
{
    "count" : 1,
    "_shards" : {
        "total" : 1,
        "successful" : 1,
        "skipped" : 0,
        "failed" : 0
    }
}
```











