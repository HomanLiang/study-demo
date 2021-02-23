[toc]



# ElasticSearch DSL 查询











## 按查询条件数量维度

### 无条件搜索

```
GET /index/_search?pretty
```



### 单条件搜索

#### term 精确搜索

类似于关系型数据库的等于，但搜索词**不会分词**

```
GET /index/_search
{
    "query" : {
        "term" : { "待查询字段" : "搜索词" }
    }
}
```



#### match 模糊搜索

类似于关系型数据库的等于，但搜索词**会分词**

```
GET /index/_search
{
    "query" : {
        "match" : { "待查询字段" : "搜索词" }
    }
}
```



#### fuzzy 更智能的模糊搜索

fuzzy 也是一个模糊查询，它看起来更加智能。它类似于搜狗输入法中允许语法错误，但仍能搜出你想要的结果。

```
GET /index/_search
{
    "query" : {
        "fuzzy" : { "name" : "kevon" }
    }
}
```

ES 返回结果包括name="kevin",name="kevin yu"



### 多条件搜索

当搜索需要多个条件时，条件与条件之间的关系有“与”、“或”、“非”，正如关系型数据库中的“and”、“or"、”not“

在ES中表示”与“关系的关键字`must`，表示”或“关系的是关键字`should`，还有表示”非“的关键字`must_not`

示例1：待查询字段1=搜索词1 或者 待查询字段2=搜索词2

```
GET /index/_search
{
    "query" : {
    	"bool": {
            "should" : [
                {
                    "term" : { "待查询字段1" : "搜索词1" }
                },
                {
                    "term" : { "待查询字段2" : "搜索词2" }
                }
            ]
    	}
    }
}
```

示例2：待查询字段1=搜索词1 并且 !(待查询字段2=搜索词2)

```
GET /index/_search
{
    "query" : {
    	"bool": {
            "must" : [
                {
                    "term" : { "待查询字段1" : "搜索词1" }
                }
            ],
            "must_not" : [
                {
                    "term" : { "待查询字段2" : "搜索词2" }
                }
            ]
    	}
    }
}
```





## 按等值、范围查询维度

### 范围查询

示例：待查询字段>=0 and 待查询字段<100

```
GET /index/_search
{
    "query" : {
        "range" : { 
        	"待查询字段" : {
        		"gte": 0,
        		"lt": 100
        	}
        }
    }
}
```





### 存在查询

示例：返回在原始字段中至少有一个非空值的文档：

```
GET /index/_search
{
    "query" : {
        "exists" : { 
        	"field" : "name"
        }
    }
}
```



### 不存在查询

示例：此查询返回在 user 字段中没有值的文档。

```
GET /index/_search
{
    "query" : {
    	"bool": {
    		"must_not": {
                "exists" : { 
                    "field" : "user"
                }
    		}
    	}
    }
}
```



### 分页搜索

```
GET /index/_search
{
    "query" : {
        "term" : { "待查询字段" : "搜索词" }
    },
    "from": 0,
    "size": 10
}
```



### 排序

```
GET /index/_search
{
    "query" : {
        "term" : { "待查询字段" : "搜索词" }
    },
    "from": 0,
    "size": 10,
    "sort": {
    	"age": {
    		"order": "desc"
    	}
    }
}
```

ES默认升序排序，如果不指定排序字段的排序，则sort字段可直接写为 "sort":"age"



















## 短语查询

搜索的结果需要完全匹配分词后的词项，且位置对应

“this is a test” ==>‘this is a new test“

```
GET /_search
{
  "query": {
    "match_phrase": {
      "message": "this is a test"
    }
  }
}
```





## 短语前缀查询

类似MySQL中的 like "新希望%"，也是需要满足文档数据和搜索关键字在词项和位置上保持一致。

```
GET /_search
{
  "query": {
    "match_phrase_prefix": {
      "message": {
        "query": "新希望"
      }
    }
  }
}
```



## query_string

和match_phrase区别的是，**query_string查询text类型字段，不需要连续，顺序还可以调换。**

```
GET /_search
{
  "query": {
    "query_string": {
      "message": {
        "query": "新希望",
        "field": "name"
      }
    }
  }
}
```





## 最低匹配度

```
GET /_search
{ 
  "bool": { 
    "should": [ 
      { "term": { "body": "how"}}, 
      { "term": { "body": "not"}}, 
      { "term": { "body": "to"}}, 
      { "term": { "body": "be"}} , 
      { "term": { "body": "be1"}} 
    ], 
    "minimum_should_match": 80% 
  } 
} 
```







## 聚合查询

### ES聚合分析是什么

- 聚合分析是数据库中重要的功能特性，完成对⼀个查询的数据集中数据的聚合计算，如：找出某字段（或计算表达式的结果）的最⼤值、最⼩值，计算和、平均值等。ES作为搜索引擎兼数据库，同样提供了强⼤的聚合分析能⼒。
- 对⼀个数据集求最大、最小、和平均值等指标的聚合，在ES中称为指标聚合
- 而关系型数据库中除了有聚合函数外，还可以对查询出的数据进⾏分组group by，再在组上进⾏指标聚合。在ES中称为桶聚合



### 指标聚合

#### max求最大值/min求最小值/sum求合/avg求平均值

求出⽕箭队球员的平均年龄

aggs 跟query、sort一样，代表聚合

avgAge 别名，代表聚合结果的字段名称

avg 聚合类型，求平均值

field 聚合字段

size 查询结果，查询的记录条数为0，不返回记录详情，也就是，只返回聚合结果

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "avgAge": {
            "avg": {
                "field": "age"
            }
        }
    },
    "size": 0
}
```



#### value_count 统计⾮空字段的⽂档数，跟mysql中的count()功能一样

写法1：求出⽕箭队中球员打球时间不为空的数量

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "countPlayerYear": {
            "value_count": {
                "field": "playYear"
            }
        }
    },
    "size": 0
}
```

写法2：查出⽕箭队有多少名球员

```
POST nba/_count
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    }
}
```



#### Cardinality 值去重计数，跟mysql中的distinct去重一样

查出⽕箭队中年龄不同的数量

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "counAget": {
            "cardinality": {
                "field": "age"
            }
        }
    },
    "size": 0
}
```



#### stats 统计count max min avg sum 5个值

查出⽕箭队球员的年龄stats

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "statsAge": {
            "stats": {
                "field": "age"
            }
        }
    },
    "size": 0
}
```



#### Extended stats ⽐stats多4个统计结果： 平⽅和、⽅差、标准差、平均值加/减两个标准差的区间

查出⽕箭队球员的年龄Extend stats

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "extendStatsAge": {
            "extended_stats": {
                "field": "age"
            }
        }
    },
    "size": 0
}
```



#### Percentiles 占⽐百分位对应的值统计，默认返回[ 1, 5, 25, 50, 75, 95, 99 ]分位上的值

1%的人，最大年龄是多少

5%的人，最大年龄是多少

25%的人，最大年龄是多少

50%的人，最大年龄是多少

75%的人，最大年龄是多少

95%的人，最大年龄是多少

99%的人，最大年龄是多少

查出⽕箭的球员的年龄占⽐

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "pecentAge": {
            "percentiles": {
                "field": "age"
            }
        }
    },
    "size": 0
}
```

查出⽕箭的球员的年龄占⽐(指定分位值)

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "percentAge": {
            "percentiles": {
                "field": "age",
                "percents": [
                    20,
                    50,
                    75
                ]
            }
        }
    },
    "size": 0
}
```



### 桶聚合-------mysql中group by

#### Terms Aggregation 根据字段项分组聚合

⽕箭队根据年龄进⾏分组

field 分组字段，group by age

size 返回的分组数，如下，分组结果一共分成了100个组，size:10，则返回10个分组的数据

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "aggsAge": {
            "terms": {
                "field": "age",
                "size": 10
            }
        }
    },
    "size": 0
}
```



#### order 分组聚合排序

⽕箭队根据年龄进⾏分组，分组信息通过年龄从⼤到⼩排序 (通过指定字段)

解析：根据组内最大的年龄，对所有分组进行排序

![image-20210222223446768](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210222231634445.png)

```
POST /nba/_search
{
    "query": {
        "term": {
            "teamNameEn": {
                "value": "Rockets"
            }
        }
    },
    "aggs": {
        "aggsAge": {
            "terms": {
                "field": "age",
                "size": 10,
                "order": {
                    "_key": "desc"
                }
            }
        }
    },
    "size": 0
}
```

每⽀球队按该队所有球员的平均年龄进⾏分组排序 (通过分组指标值)

```
POST /nba/_search
{
    "aggs": {
        "aggsTeamName": {
            "terms": {
                "field": "teamNameEn",
                "size": 30,
                "order": {
                    "avgAge": "desc"
                }
            },
            "aggs": {
                "avgAge": {
                    "avg": {
                        "field": "age"
                    }
                }
            }
        }
    },
    "size": 0
}
```



#### 筛选分组聚合

湖⼈和⽕箭队按球队平均年龄进⾏分组排序 (指定值列表)

```
POST /nba/_search
{
    "aggs": {
        "aggsTeamName": {
            "terms": {
                "field": "teamNameEn",
                "include": [
                    "Lakers",
                    "Rockets",
                    "Warriors"
                ],
                "exclude": [
                    "Warriors"
                ],
                "size": 30,
                "order": {
                    "avgAge": "desc"
                }
            },
            "aggs": {
                "avgAge": {
                    "avg": {
                        "field": "age"
                    }
                }
            }
        }
    },
    "size": 0
}
```

湖⼈和⽕箭队按球队平均年龄进⾏分组排序 (正则表达式匹配值)

```
POST /nba/_search
{
    "aggs": {
        "aggsTeamName": {
            "terms": {
                "field": "teamNameEn",
                "include": "Lakers|Ro.*|Warriors.*",
                "exclude": "Warriors",
                "size": 30,
                "order": {
                    "avgAge": "desc"
                }
            },
            "aggs": {
                "avgAge": {
                    "avg": {
                        "field": "age"
                    }
                }
            }
        }
    },
    "size": 0
}
```



#### Range Aggregation 范围分组聚合

NBA球员年龄按20,20-35,35这样分组

```
POST /nba/_search
{
    "aggs": {
        "ageRange": {
            "range": {
                "field": "age",
                "ranges": [
                    {
                        "to": 20
                    },
                    {
                        "from": 20,
                        "to": 35
                    },
                    {
                        "from": 35
                    }
                ]
            }
        }
    },
    "size": 0
}
```

NBA球员年龄按20,20-35,35这样分组 (起别名)

```
POST /nba/_search
{
    "aggs": {
        "ageRange": {
            "range": {
                "field": "age",
                "ranges": [
                    {
                        "to": 20,
                        "key": "A"
                    },
                    {
                        "from": 20,
                        "to": 35,
                        "key": "B"
                    },
                    {
                        "from": 35,
                        "key": "C"
                    }
                ]
            }
        }
    },
    "size": 0
}
```



#### Date Range Aggregation 时间范围分组聚合

NBA球员按出⽣年⽉分组

```
POST /nba/_search
{
    "aggs": {
        "birthDayRange": {
            "date_range": {
                "field": "birthDay",
                "format": "MM-yyy",
                "ranges": [
                    {
                        "to": "01-1989"
                    },
                    {
                        "from": "01-1989",
                        "to": "01-1999"
                    },
                    {
                        "from": "01-1999",
                        "to": "01-2009"
                    },
                    {
                        "from": "01-2009"
                    }
                ]
            }
        }
    },
    "size": 0
}
```



#### Date Histogram Aggregation 时间柱状图聚合

按天、⽉、年等进⾏聚合统计。可按 year (1y), quarter (1q), month (1M), week (1w), day(1d), hour (1h), minute (1m), second (1s) 间隔聚合

NBA球员按出⽣年分组

```
POST /nba/_search
{
    "aggs": {
        "birthday_aggs": {
            "date_histogram": {
                "field": "birthDay",
                "format": "yyyy",
                "interval": "year"
            }
        }
    },
    "size": 0
}
```





## 深分页

### from/size方案
这是ES分页最常用的一种方案，跟mysql类似，from指定查询的起始位置，size表示从起始位置开始的文档数量。看个例子。
```
GET /kibana_sample_data_ecommerce/_search
{
  "from": 0, 
  "size" : 10,
  "query": {
    "bool": {
      "must": [
        {"match": {
          "customer_first_name": "Diane"
        }}
      ],
      "filter": {
        "range": {
          "order_date": {
            "gte": "2020-01-03"
          }
        }
      }
    }
  }, 
  "sort": [
    {
      "order_date": {
        "order": "asc"
      }
    }
  ]
}
```
这个例子是查询客户名字带有diane，并且订单时间大于2020-01-03的订单信息，并且查询的结果按照时间升序。

使用起来很简单，不过ES默认的分页深度是10000，也就是说from+size超过10000就会报错，我们可以试下，会报下面的错误：
```
{
  "error": {
    "root_cause": [
      {
        "type": "illegal_argument_exception",
        "reason": "Result window is too large, from + size must be less than or equal to: [10000] but was [10009]. See the scroll api for a more efficient way to request large data sets. This limit can be set by changing the [index.max_result_window] index level setting."
      }
    ],
    "type": "search_phase_execution_exception",
    "reason": "all shards failed",
    "phase": "query",
    "grouped": true,
```
其实很多时候，业务场景很少遇到这种深度分页的情况，一般通过页面查询，不会有人会翻到这么深的页数。

不过，如果我们的业务场景确实需要超过10000条记录的分页，有办法解决吗？当然有。ES内部是通过index.max_result_window这个参数控制分页深度的，我们可以针对特定的索引来修改这个值。
```
curl -XPUT IP:PORT/index_name/_settings -d '{ "index.max_result_window" :"100000"}'
```
这里是把深度分页的限制改成了10万。

事实上，ES之所以有这个限制，是因为在分布式环境下深度分页的查询效率会非常低。比如我们现在查询第from=990，size=10这样的条件，这个在业务层就是查询第990页，每页展示10条数据。

但是在ES处理的时候，会分别从每个分片上拿到1000条数据，然后在coordinating的节点上根据查询条件聚合出1000条记录，最后返回其中的10条。所以分页越深，ES处理的开销就大，占用内存就越大。
![image-20210222231530287](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210222231634445.png)

### search after方案
有时候我们会遇到一些业务场景，需要进行很深度的分页，但是可以不指定页数翻页，只要可以实时请求下一页就行。比如一些实时滚动的场景。

ES为这种场景提供了一种解决方案：search after。

search after利用实时有游标来帮我们解决实时滚动的问题，简单来说前一次查询的结果会返回一个唯一的字符串，下次查询带上这个字符串，进行下一页的查询。看个例子：
```
GET /kibana_sample_data_ecommerce/_search
{
  "size" : 2,
  "query": {
    "bool": {
      "must": [
        {"match": {
          "customer_first_name": "Diane"
        }}
      ],
      "filter": {
        "range": {
          "order_date": {
            "gte": "2020-01-03"
          }
        }
      }
    }
  }, 
  "sort": [
    {
      "order_date": "desc",
      "_id": "asc"
      
    }
  ]
}
```
首先查询第一页数据，我这里指定取回2条，条件跟上一节一样。唯一的区别在于sort部分我多加了id，这个是为了在order_date字段一样的情况下告诉ES一个可选的排序方案。因为search after的游标是基于排序产生的。

注意看查询结果的最后，有个类似下面这样的东东：
```
"sort" : [
          1580597280000,
          "RZz1f28BdseAsPClqbyw"
        ]
```
在下一页的查询中，我们带上这个玩意，如下：
```
GET /kibana_sample_data_ecommerce/_search
{
  "size" : 2,
  "query": {
    "bool": {
      "must": [
        {"match": {
          "customer_first_name": "Diane"
        }}
      ],
      "filter": {
        "range": {
          "order_date": {
            "gte": "2020-01-03"
          }
        }
      }
    }
  }, 
  "search_after": 
      [
          1580597280000,
          "RZz1f28BdseAsPClqbyw"
        ],
  "sort": [
    {
      "order_date": "desc",
      "_id": "asc"
      
    }
  ]
}
```
就这样一直操作就可以实现不断的查看下一页了。

其实仔细想想这个操作原理并不复杂，以前笔者在mysql的场景下也用过类似的方案。我们来看看上一节讨论的那个问题，比如通过一直下一页，翻到了990页，当继续下页时，因为有了排序的唯一标识，ES只需从每个分片上拿到满足条件的10条文档，然后基于这30条文档最终聚合成10条结果返回即可。
![image-20210222231634445](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210222231634445.png)
很显然，开销小很多。

### scroll api方案
还有一种查询场景，我们需要一次性或者每次查询大量的文档，但是对实时性要求并不高。ES针对这种场景提供了scroll api的方案。这个方案牺牲了实时性，但是查询效率确实非常高。

先来看个示例：
```
POST /kibana_sample_data_ecommerce/_search?scroll=1m
{
    "size": 10,
    "query": {
        "match_all" : {
        }
    }
}
```
首先我们第一次查询，会生成一个当前查询条件结果的快照，后面的每次滚屏（或者叫翻页）都是基于这个快照的结果，也就是即使有新的数据进来也不会别查询到。

上面这个查询结果会返回一个scroll_id，拷贝过来，组成下一条查询语句，
```
POST /_search/scroll
{
    "scroll" : "1m",
  "scroll_id" : "DXF1ZXJ5QW5kRmV0Y2gBAAAAAAAAA5AWNGpKbFNMZnVSc3loXzQwb0tJZHBtZw=="
}
```
以此类推，后面每次滚屏都把前一个的scroll_id复制过来。注意到，后续请求时没有了index信息，size信息等，这些都在初始请求中，只需要使用scroll_id和scroll两个参数即可。

很多人对scroll这个参数容易混淆，误认为是查询的限制时间。这个理解是错误的。这个时间其实指的是es把本次快照的结果缓存起来的有效时间。

scroll 参数相当于告诉了 ES我们的search context要保持多久，后面每个 scroll 请求都会设置一个新的过期时间，以确保我们可以一直进行下一页操作。

我们继续讨论一个问题，scroll这种方式为什么会比较高效？

ES的检索分为查询（query）和获取（fetch）两个阶段，query阶段比较高效，只是查询满足条件的文档id汇总起来。fetch阶段则基于每个分片的结果在coordinating节点上进行全局排序，然后最终计算出结果。

scroll查询的时候，在query阶段把符合条件的文档id保存在前面提到的search context里。 后面每次scroll分批取回只是根据scroll_id定位到游标的位置，然后抓取size大小的结果集即可。

### 总结
from/size方案的优点是简单，缺点是在深度分页的场景下系统开销比较大，占用较多内存。

search after基于ES内部排序好的游标，可以实时高效的进行分页查询，但是它只能做下一页这样的查询场景，不能随机的指定页数查询。

scroll方案也很高效，但是它基于快照，不能用在实时性高的业务场景，建议用在类似报表导出，或者ES内部的reindex等场景。





























