[toc]



# ElasticSearch 索引文档及操作

`ES` 提供了多种操作数据的方式，其中较为常见的方式就是 `RESTful` 风格的 `API`。

简单的体验：利用 `Postman` 发起 `HTTP` 请求（当然也可以在命令行中使用 `curl` 命令）。

[官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/7.4/rest-apis.html)



## 1.ES 中的索引

`ES` 中的文档都会存储在某个**索引**（`Index`）中，索引是文档的容器，是一类文档的集合，相当于关系型数据库中的表的概念。

`ES` 中可以创建很多不同的索引，表示不同的文档集合。

每个索引都可以定义自己的 **Mappings** 和 **Settings**：

- `Mappings`：用于设置文档字段的类型。
- `Settings`：用于设置不同的数据分布。

对于索引的一些参数设置，有些参数可以动态修改，有些参数在索引创建后不能修改，可参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/index-modules.html)。

***ES 与传统数据库类比***

如果将 `ES` 中的基本概念类比到传统数据库中，它们的对应关系如下：

| ES        | 传统数据库 |
| --------- | ---------- |
| 索引      | 表         |
| 文档      | 行         |
| 字段      | 列         |
| `Mapping` | 表定义     |
| `DSL`     | `SQL` 语句 |



## 2.Index APIs

### 2.1.创建索引

#### 2.1.1.简单创建索引

```
PUT /twitter
{
    "settings" : {
        "number_of_shards" : 3,
        "number_of_replicas" : 2
    }
}
```

`number_of_shards`：分片数，默认1

`number_of_replicas`：备份数，默认1

或者

```
PUT /twitter
```



#### 2.1.2.创建索引并指定索引的映射 Mapping

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



#### 2.1.3.创建索引并给索引指定别名 Aliases

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



#### 2.1.4.创建成功返回结果

```
{
    "acknowledged": true,
    "shards_acknowledged": true,
    "index": "test"
}
```



### 2.2.修改索引

#### 2.2.1.修改索引设置 settings

```
PUT /twitter/_settings
{
    "index" : {
        "number_of_replicas" : 2
    }
}
```

#### 2.2.2.修改索引别名 Aliases

##### 2.2.2.1.增加别名

```console
POST /_aliases
{
    "actions" : [
        { "add" : { "index" : "twitter", "alias" : "alias1" } }
    ]
}
```

##### 2.2.2.2.移除别名

```
POST /_aliases
{
    "actions" : [
        { "remove" : { "index" : "test1", "alias" : "alias1" } }
    ]
}
```

##### 2.2.2.3.重命名别名

```
POST /_aliases
{
    "actions" : [
        { "remove" : { "index" : "test1", "alias" : "alias1" } },
        { "add" : { "index" : "test1", "alias" : "alias2" } }
    ]
}
```



### 2.3.查看指定索引

#### 2.3.1.查询索引

`Request`:

```
GET /<alias>
```

查询所有索引

`Request`:

```
GET /_all
```



#### 2.3.2.查询索引别名

```
GET /_alias

GET /_alias/<alias>

GET /<index>/_alias/<alias>
```



#### 2.3.3.查询索引设置

`Request`:

```
GET /<index>/_settings

GET /<index>/_settings/<setting>
GET /log_2013_-*/_settings/index.number_*
```



#### 2.3.4.查询映射

```
GET /_mapping

GET /<index>/_mapping
```



### 2.4.删除索引

#### 2.4.1.删除索引

`Request`:

```
DELETE /<index>
```

#### 2.4.2.删除别名

`Request`:

```
DELETE /<index>/_alias/<alias>

DELETE /<index>/_aliases/<alias>
```



### 2.5.分词 Analyze

#### 2.5.1.查询使用分词器

```
GET /_analyze

POST /_analyze

GET /<index>/_analyze

POST /<index>/_analyze
```

#### 2.5.2.测试分词器结果

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



## 3.ES 中的文档

在 `ES` 中，**文档**（`Document`）是可搜索数据的最小存储单位，相当于关系数据库中的一条记录。

文档以 **Json** 数据格式保存在 `ES` 中，`Json` 中保存着多个**键值对**，它可以保存不同类型的数据，比如：

- 字符串类型
- 数字类型
- 布尔类型
- 数组类型
- 日期类型
- 二进制类型
- 范围类型

> `Python` 语言中的**字典**类型，就是 `Json` 数据格式。

文档中的数据类型可以指定，也可以由 `ES` 自动推断。

每个文档中都有一个 **Unique ID**，用于唯一标识一个文档。`Unique ID` 可以由用户指定，也可以由 `ES` 自动生成。

> **Unique ID** 实际上是一个**字符串**。

比如下面的 `Json` 就是一个文档：

```python
{
  "name" : "XiaoMing",
  "age" : 19,
  "gender" : "male"
}
```

### 3.1.文档元数据

将上面那个 `Json` 数据存储到 `ES` 后，会像下面这样：

```python
{
    "_index": "person", 
    "_type": "_doc", 
    "_id": "2344563",
    "_version": 1, 
    "_source": {
        "name": "XiaoMing", 
        "age": 19, 
        "gender": "male"
    }
}
```

其中以下划线开头的字段就是元数据：

- `_index`：文档所属的索引。
- `_type`：文档的类型。`ES 7.0` 开始，一个索引只能有一种 `_type`。
- `_id`：文档的唯一 `ID`。
- `_source`：文档的原始 `Json` 数据。
- `_version`：文档更新的次数。

你可以查看[这里](https://www.elastic.co/cn/blog/moving-from-types-to-typeless-apis-in-elasticsearch-7-0)，了解“**为什么单个Index下，不再支持多个Types？**”。

更多关于元数据的信息，可以参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/mapping-fields.html)。



### 3.2.文档的删除与更新

`ES` 中文档的**删除操作不会马上将其删除**，而是会将其标记到 **del** 文件中，在后期合适的时候（比如 `Merge` 阶段）会真正的删除。

**ES 中的文档是不可变更的**，**更新操作**会将旧的文档标记为删除，同时增加一个新的字段，并且文档的 `version` 加 1。



### 3.3.文档中的字段数

在 `ES` 中，一个文档默认最多可以有 **1000** 个字段，可以通过 [index.mapping.total_fields.limit](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html#mapping-limit-settings) 进行设置。

注意在设计 `ES` 中的数据结构时，不要使文档的字段数过多，这样会使得 `mapping` 很大，增加集群的负担。





## 4.Document APIs

### 4.1.插入文档

- `Create document IDs automatically`

    ```
    POST twitter/_doc/
    {
        "user" : "kimchy",
        "post_date" : "2009-11-15T14:12:12",
        "message" : "trying out Elasticsearch"
    }
    ```

	返回结果：

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

	The API returns the following result:

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



### 4.2.查询文档

#### 4.2.1.查询单个文档

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

#### 4.2.2.Multi get API

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



### 4.3.修改文档

#### 4.3.1.单个文档更新

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



#### 4.3.2.单个文档Upsert

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

#### 4.3.3.单个文档 Scripted Upsert

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



#### 4.3.4.单个文档 Doc as Upsert

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

#### 4.3.5.通过查询更新

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





### 4.4.删除文档

#### 4.4.1.单个文档删除

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



#### 4.4.2.通过查询删除

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



### 4.5.Bulk API

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



## 5.Search APIs

### 5.1.Search

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



### 5.2.URI Search

完全通过URI查询，包括查询条件。

```console
GET twitter/_search?q=user:kimchy
```



### 5.3.Request Body Search

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





### 5.4.Count API

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



## 6.说明

### 6.1.GET 操作

**GET** 操作可以获取指定文档的内容。

`GET index_name/_count`：获取指定索引中的文档数。

`GET index_name/_doc/id`：获取指定索引中的指定文档。

`GET index_name/_doc`：**不允许**该操作。

`GET index_name`：获取指定索引的 `Mappings` 和 `Settings`。



### 6.2.POST / PUT 操作

**POST/PUT** 操作用于创建文档。

***按照 POST / PUT 方法来区分***

`POST index_name/_doc`：

- `POST index_name/_doc`：不指定 ID，总是会插入新的文档，文档数加 1。

- `POST/PUT index_name/_doc/id` ：指定 ID

  - 当 `id` 存在时，会覆盖之前的，并且 `version` 会加 1，文档数不增加。
  - 当 `id` 不存在时，会插入新的文档，文档数加 1。

`PUT index_name/_create`：

- `PUT index_name/_create`：不指定 ID，**不允许**该操作。
- `PUT index_name/_create/id`：指定 ID
- 当 id 存在时：**报错**，不会插入新文档。
  - 当 id 不存在时：会插入新的文档，文档数加 1。

`PUT index_name/_doc`：

- `PUT index_name/_doc`：不指定 ID，**不允许**该操作。
- `PUT/POST index_name/_doc/id`：指定 ID
  - 当 id 存在时，会覆盖之前的，并且 version 会加 1，文档数不增加。
  - 当 id 不存在时，会插入新的文档，文档数加 1。

- `PUT index_name/_doc/id?op_type=XXX`
  - `op_type=create`：
    - 当 id 存在时，**报错**，不会插入新文档。
    - 当 id 不存在时，会插入新的文档，文档数加 1。

  - `op_type=index`：
    - 当 id 存在时，会覆盖之前的，并且 version 会加 1，文档数不增加。
    - 当 id 不存在时，会插入新的文档，文档数加 1。

***按照是否指定 ID 来区分***

**指定 ID**：

- `POST/PUT index_name/_doc/id`：指定 ID，称为 Index 操作
  - 相当于 `PUT index_name/_doc/id?op_type=index`
  - 当 id 存在时，会覆盖之前的，并且 version 会加 1，文档数不增加。
  - 当 id 不存在时，会插入新的文档，文档数加 1。

- `PUT index_name/_doc/id?op_type=create`：指定 ID，称为Create操作
  - 相当于 `PUT index_name/_create/id`
  - 当 id 存在时，**报错**，不会插入新文档。
  - 当 id 不存在时，会插入新的文档，文档数加 1。

**不指定 ID**：

- `POST index_name/_doc`：不指定 ID，总是会插入新的文档，文档数加 1。
- `PUT index_name/_doc`：不指定 ID，**不允许**该操作。
- `PUT index_name/_create`：不指定 ID，**不允许**该操作。



### 6.3.Update 操作

Update 操作用于更新文档的内容。

`POST index_name/_update/id/`：更新指定文档的内容。更新的内容要放在 **doc** 字段中，否则会**报错**。

- 当 id 不存在时，报错，不更新任何内容。
- 当 id 存在时：
  - 如果更新的字段与**原来的相同**，则不做任何操作。
  - 如果更新的字段与**原来的不同**，则更新原有内容，并且 `version` 会加 1。

实际上 `ES` 中的文档是不可变更的，更新操作会将旧的文档标记为删除，同时增加一个新的字段，并且文档的 `version` 加 1。



### 6.4.Delete 操作

`Delete` 操作用于删除索引或文档。

`DELETE /index_name/_doc/id`：删除某个文档。

- 当删除的 id 存在时，会删除该文档。
- 当删除的 id 不存在时，ES 会返回 `not_found`。

`DELETE /index_name`：删除整个索引，**要谨慎使用**！

- 当删除的 `index_name` 存在时，会删除整个索引内容。
- 当删除的 `index_name` 不存在时，`ES` 会返回 `404` 错误。



### 6.5.Bulk 批量操作

批量操作指的是，在一次 API 调用中，对不同的索引进行多次操作。

每次操作互不影响，即使某个操作出错，也不影响其他操作。

返回的结果中包含了所有操作的执行结果。

Bulk 支持的操作有 `Index`，`Create`，`Update`，`Delete`。

Bulk 操作的格式如下：

```shell
POST _bulk
{ "index" : { "_index" : "test", "_id" : "1" } }
{ "field1" : "value1" }
{ "delete" : { "_index" : "test", "_id" : "2" } }
{ "create" : { "_index" : "test2", "_id" : "3" } }
{ "field1" : "value3" }
{ "update" : {"_id" : "1", "_index" : "test"} }
{ "doc" : {"field2" : "value2"} }
```

注意 `Bulk` 请求体的数据量不宜过大，建议在 `5~15M`。



### 6.6.Mget 批量读取

**Mget** 一次读取多个文档的内容，设计思想类似 `Bulk` 操作。

`Mget` 操作的格式如下：

```shell
GET _mget
{
    "docs" : [
        {"_index" : "index_name1", "_id" : "1"},
        {"_index" : "index_name2", "_id" : "2"}
    ]
}
```

也可以在 URI 中指定索引名称：

```shell
GET /index_name/_mget
{
    "docs" : [
        {"_id" : "1"},
        {"_id" : "2"}
    ]
}
```

还可以用 `_source` 字段来设置返回的内容：

```shell
GET _mget
{
    "docs" : [
        {"_index" : "index_name1", "_id" : "1"},
        {"_index" : "index_name2", "_id" : "2", "_source" : ["f1", "f2"]}
    ]
}
```



### 6.7.Msearch 批量查询

**Msearch** 操作用于批量查询，格式如下：

```shell
POST index_name1/_msearch
{} # 索引名称，不写的话就是 URI 中的索引
{"query" : {"match_all" : {}},"size":1}
{"index" : "index_name2"} # 改变了索引名称
{"query" : {"match_all" : {}},"size":2}
```

URI 中也可以不写索引名称，此时**请求体**里必须写索引名称：

```shell
POST _msearch
{"index" : "index_name1"} # 索引名称
{"query" : {"match_all" : {}},"size":1}
{"index" : "index_name2"} # 索引名称
{"query" : {"match_all" : {}},"size":2}
```

上文中介绍了 3 种批量操作，分别是 `Bulk`，`Mget`，`Msearch`。注意在使用批量操作时，数据量不宜过大，避免出现**性能问题**。



### 6.8.ES 常见错误码

当我们的请求发生错误的时候，ES 会返回相应的**错误码**，常见的错误码如下：

| 错误码 | 含义         |
| ------ | ------------ |
| 429    | 集群过于繁忙 |
| 4XX    | 请求格式错误 |
| 500    | 集群内部错误 |



### 6.9.Reindex 重建索引

有时候我们需要**重建索引**，比如以下情况：

- 索引的 `mappings` 发生改变：比如字段类型或者分词器等发生更改。
- 索引的 `settings` 发生改变：比如索引的主分片数发生更改。
- 集群内或集群间需要做**数据迁移**。

ES 中提供两种重建 API：

- [Update by query](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html)：在现有索引上重建索引。
- [Reindex](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)：在其它索引上重建索引。



#### 6.9.1.添加子字段

先在一个索引中插入数据：

```shell
DELETE blogs/

# 写入文档
PUT blogs/_doc/1
{
  "content":"Hadoop is cool",
  "keyword":"hadoop"
}

# 查看自动生成的 Mapping
GET blogs/_mapping

# 查询文档
POST blogs/_search
{
  "query": {
    "match": {
      "content": "Hadoop"
    }
  }
}

# 可以查到数据
```

现在修改 **mapping**（**添加子字段**是允许的），为 **content** 字段加入一个子字段：

```shell
# 修改 Mapping，增加子字段，使用英文分词器
PUT blogs/_mapping
{
  "properties" : {
    "content" : {      # content 字段
      "type" : "text",
      "fields" : {     # 加入一个子字段
        "english" : {  # 子字段名称
          "type" : "text",      # 子字段类型
          "analyzer":"english"  # 子字段分词器
        }
      }
    }
  }
}

# 查看新的 Mapping
GET blogs/_mapping
```

修改 **mapping** 之后再查询文档：

```shell
# 使用 english 子字段查询 Mapping 变更前写入的文档
# 查不到文档
POST blogs/_search
{
  "query": {
    "match": {
      "content.english": "Hadoop"
    }
  }
}

# 注意：不使用 english 子字段是可以查询到之前的文档的
POST blogs/_search
{
  "query": {
    "match": {
      "content": "Hadoop"
    }
  }
}
```

结果发现，使用 **english** 子字段是查不到之前的文档的。这时候就需要**重建索引**。



#### 6.9.2.Update by query

下面使用 `Update by query` 对索引进行重建：

```shell
# Update所有文档
POST blogs/_update_by_query
{

}
```

重建索引之后，不管是使用 **english** 子字段还是不使用，都可以查出文档。

`Update by query` 操作还可以设置一些条件：

- [uri-params](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html#docs-update-by-query-api-query-params)
- [request-body](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html#docs-update-by-query-api-request-body)：通过设置一个 **query** 条件，来指定对哪些数据进行重建。

**request-body** 示例：

```shell
POST tech_blogs/_update_by_query?pipeline=blog_pipeline
{
    "query": {      # 将 query 的查询结果进行重建
        "bool": {
            "must_not": {
                "exists": {"field": "views"}
            }
        }
    }
}
```



#### 6.9.3.修改字段类型

在原有 **mapping** 上，**修改字段类型**是不允许的：

```shell
# 会发生错误
PUT blogs/_mapping
{
  "properties" : {
    "content" : {
      "type" : "text",
      "fields" : {
        "english" : {
          "type" : "text",
          "analyzer" : "english"
        }
      }
    },
    "keyword" : {  # 修改 keyword 字段的类型
      "type" : "keyword"
    }
  }
}
```

这时候只能创建一个新的索引，设置正确的字段类型，然后再将原有索引中的数据，重建到新索引中。

建立一个新的索引 **blogs_new**：

```shell
# 创建新的索引并且设定新的Mapping
PUT blogs_new/
{
  "mappings": {
    "properties" : {
      "content" : {
        "type" : "text",
        "fields" : {
          "english" : {
            "type" : "text",
            "analyzer" : "english"
          }
        }
      },
      "keyword" : {
        "type" : "keyword"
      }
    }    
  }
}
```



#### 6.9.4.Reindex

下面使用 **Reindex** 将原来索引中的数据，导入到新的索引中：

```shell
# Reindx API
POST _reindex
{
  "source": { # 指定原有索引
    "index": "blogs"
  },
  "dest": {   # 指定目标索引
    "index": "blogs_new"
  }
}
```

**Reindex** API 中的 **source** 字段和 **dest** 字段还有很多参数可以设置，具体可参考[其官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html#docs-reindex-api-request-body)。

另外 **Reindex** 请求的 URI 中也可以设置参数，可以参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html#docs-reindex-api-query-params)。



### 6.10.ES 的并发控制

同一个资源在多并发处理的时候，会发生冲突的问题。

传统数据库（比如 `MySQL`）会采用**锁**的方式，在更新数据的时候对数据进行加锁，来防止冲突。

而 `ES` 并没有采用锁，而是将并发问题交给了用户处理。

在 `ES` 中可以采用两种方式：

- 内部版本控制（`ES` 自带的 `version`）：在 `URI` 中使用 `if_seq_no` 和 `if_primary_term`
- 外部版本控制（由用户指定 `version`）：在 `URI` 中使用 `version` 和 `version_type=external`

示例，首先插入数据：

```shell
DELETE products
PUT products/_doc/1
{
  "title":"iphone",
  "count":100
}

# 上面的插入操作会返回 4 个字段：
#{
#  "_id" : "1",
#  "_version" : 1,
#  "_seq_no" : 0,
#  "_primary_term" : 1
#}
```



#### 6.10.1.内部版本控制方式

使用内部版本控制的方式：

```shell
PUT products/_doc/1?if_seq_no=0&if_primary_term=1
{
  "title":"iphone",
  "count":100
}

# 上面的更新操作返回下面内容：
#{
#  "_id" : "1",
#  "_version" : 2,       # 加 1
#  "_seq_no" : 1,        # 加 1
#  "_primary_term" : 1   # 不变
#}
```

如果再次执行这句更新操作，则会出错，**出错之后由用户决定如何处理**，**这就达到了解决冲突的目的**。

```shell
# 再执行则会出错，因为 seq_no=0 且 primary_term=1 的数据已经不存在了
PUT products/_doc/1?if_seq_no=0&if_primary_term=1
```



#### 6.10.2.外部版本控制方式

先看下数据库中的数据：

```shell
GET products/_doc/1

# 返回：
{
  "_index" : "products",
  "_type" : "_doc",
  "_id" : "1",            # id
  "_version" : 2,         # version
  "_seq_no" : 1,
  "_primary_term" : 1,
  "found" : true,
  "_source" : {
    "title" : "iphone",
    "count" : 100
  }
}
```

使用外部版本控制的方式：

```shell
# 如果 URI 中的 version 值与 ES 中的 version 值相等，则出错
# 下面这句操作会出错，出错之后，由用户决定如何处理
PUT products/_doc/1?version=2&version_type=external
{
  "title":"iphone",
  "count":1000
}

# 如果 URI 中的 version 值与 ES 中的 version 值不相等，则成功
# 下面这句操作会成功
PUT products/_doc/1?version=3&version_type=external
{
  "title":"iphone",
  "count":1000
}
```







**参考文章：**

[ElasticSearch 文档及操作](https://www.cnblogs.com/codeshell/p/14429409.html)



