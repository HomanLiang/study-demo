[toc]



# ElasticSearch 聚合分析

## 

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

![image-20210222223446768](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210222231634445.png)

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







