[toc]



# ElasticSearch 配置同义词

## 同义词文档格式

- 单向同义词

  ```autohotkey
  ipod, i-pod, i pod => ipod
  ```

- 双向同义词

  ```undefined
  马铃薯, 土豆, potato
  ```

## 试验步骤

### 添加同义词文件

- 在 Elasticsearch 的 config 目录下新建 `analysis` 目录，在 `analysis` 下添加同义词文件 `synonym.txt`（`/etc/elasticsearch/analysis/synonym.txt`）
- 在检索时使用同义词，不需要重启 Elasticsearch，也不需要重建索引

### 创建索引

```puppet
PUT my_index
{
  "settings": {
    "analysis": {
      "filter": {
        "word_syn": {
          "type": "synonym_graph",
          "synonyms_path": "analysis/synonym.txt",
          "updateable": true   # 允许热更新
        }
      },
      "analyzer": {
        "ik_smart_syn": {
          "filter": [          # token filter
            "stemmer",
            "word_syn"
          ],
          "type": "custom",
          "tokenizer": "ik_smart"
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "ik_max_word",
        "search_analyzer": "ik_smart"
      },
      "author": {
        "type": "keyword"
      }
    }
  }
}
```

- 热更新重载分词器：[https://www.elastic.co/guide/...](https://link.segmentfault.com/?enc=jxD3zod6%2BjU44n%2BTG%2FHefw%3D%3D.199aTwiTVD51boiHNr1fUmfqRFo5HNHee615eZB%2FCyBTeZHbveZ3P9EkL2NW94Ks%2BJ5MoNrF2AGlR403A7XLVZ8nr4vXeDsXNSDKKhBwfbqMKYcgT4iGxFx8OmA%2B%2Bskc)

  ```bash
  POST my_index/_reload_search_analyzers
  ```

### 直接测试分词器

- 查询语句

  ```routeros
  GET my_index/_analyze
  {
  "analyzer": "ik_smart_syn",
  "text": "马铃薯"
  }
  ```

- 输出

  ```ada
  {
  "tokens" : [
    {
      "token" : "马铃薯",
      "start_offset" : 0,
      "end_offset" : 3,
      "type" : "CN_WORD",
      "position" : 0
    },
    {
      "token" : "土豆",
      "start_offset" : 0,
      "end_offset" : 3,
      "type" : "SYNONYM",
      "position" : 0
    },
    {
      "token" : "potato",
      "start_offset" : 0,
      "end_offset" : 3,
      "type" : "SYNONYM",
      "position" : 0
    }
  ]
  }
  ```

### 添加测试数据

- 添加数据

  ```awk
  POST my_index/_doc/1
  {
    "title": "马铃薯",
    "author": "土豆"
  }
  ```

- 查看某个文档某个字段的分词结果

  ```routeros
  GET my_index/_termvectors/1?fields=title
  ```

### 检索测试

- 查询语句

  ```routeros
  GET my_index/_search
  {
  "query": {
    "query_string": {
      "analyzer": "ik_smart_syn", 
      "query": "title:potato AND author:potato"
    }
  }
  }
  ```

- 结果输出

  ```ada
  {
  "took" : 38,
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
    "max_score" : 0.5753642,
    "hits" : [
      {
        "_index" : "my_index",
        "_type" : "_doc",
        "_id" : "1",
        "_score" : 0.5753642,
        "_source" : {
          "title" : "马铃薯",
          "author" : "土豆"
        }
      }
    ]
  }
  }
  ```

## 相关文档

- CSDN blog： [Elasticsearch：使用同义词 synonyms 来提高搜索效率](https://link.segmentfault.com/?enc=xLhj9KQBdyu%2BrN9ZvCrGsQ%3D%3D.ImUdX2lWkdVxpD8B4AjwqRqrXVHnpf5SsurYCC1jbdAbKJ2OhqVD8BtGRhPwmseEKbVZSqSMlLNfUtiaZiaA2Q%3D%3D)
- 官方 blog： [一样，却又不同：借助同义词让 Elasticsearch 更加强大](https://link.segmentfault.com/?enc=utxCa3nfjeTZ4z8hhc2ovA%3D%3D.lwuEUxO8LiALd%2FI3RFkoTlsSNPWT4p1%2B3aqZ94MR1wBRUnA6WDS6Bi3og%2Frdl0AtNXlxcxme7r9VxaNmnN5JWDBRBq8AEjCGLjwB%2BZiV2F5DgPD073w%2F5qcAqsyK4ud0)
- 同义词过滤器： [Synonym token filter](https://link.segmentfault.com/?enc=gjao2DysfBXLVlXuoTSDZA%3D%3D.66tcuPi6E8WnGjo2mSsDxUSCB%2FfNCZVGPifRPX7dSjDuykgL2EoUhQZrHgV3DZcqbCcyoq4Hv5XZchrFpScC3ptDXOk0NtkMgOg%2BeLQX5bNk6jkp8jmPthlZMu9Ph2n2)、[Synonym graph token filter](https://link.segmentfault.com/?enc=yMCvh%2FIxn5xStsyyGlHD7Q%3D%3D.81tTCl0EcX6AZ1nIgGYzg62YASbUvhsiSuDeNaepfU10t1S9MYb23KFTrF2gwy0XfCJt2I3Bz0cyBpUzgcdf5YvVupkGK9yyK9XNy5yZ3EpXKUjPJpYiCaSjMGNOkFkmlDVivMmEMSob9n55ixLPlQ%3D%3D)