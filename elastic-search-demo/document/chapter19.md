[toc]



# ElasticSearch 第三方组件

## canal

`canal` 主要用途是对 `MySQL` 数据库增量日志进行解析，提供增量数据的订阅和消费，简单说就是可以对 `MySQL` 的增量数据进行实时同步，支持同步到 `MySQ`L、`Elasticsearch`、`HBase`等数据存储中去。

[MySQL如何实时同步数据到ES？试试这款阿里开源的神器](https://mp.weixin.qq.com/s?__biz=MzU1Nzg4NjgyMw==&mid=2247487292&idx=1&sn=3d9c08bd622aac48eb4834d854c707b8&chksm=fc2fb334cb583a224364dce115e1b81ad238b441e8cb785f4dcac54ce4a99ec593c04bf0e217&scene=126&sessionid=1604278963&key=252344e591cd8433714ccf004e3d4e9af3c7412a3f5fed190e145d2d867431826d2a2f77636346ade76752be950e48617150d11b8eda56e3649366c3093a5c2a059bc369670b1e6207ba7fd833bd0b601e0588f6306b662abca0f8ba56d64e09c2bc9a252468b12c70caba4a23d77bbf665049b17ec06249cd419b37c8a71719&ascene=14&uin=MTU1NTA3NjAyMQ%3D%3D&devicetype=Windows+10+x64&version=6300002f&lang=zh_CN&exportkey=A9ek6sXeNL%2F3v2pfCQBS1e8%3D&pass_ticket=8OFq31mB6QrmkMu%2FWYhOUsk6slQuVJ4l7feMCP7YI6u5ETSR5lFN1%2FNnTEE1T2gQ&wx_header=0&fontgear=2)

https://github.com/alibaba/canal/wiki