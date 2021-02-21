[toc]



# REST APIs--简单搜索

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





























