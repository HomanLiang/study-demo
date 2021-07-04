[toc]



# ElasticSearch 跨集群数据迁移

写这篇文章，主要是目前公司要把 `ES` 从 `2.4.1` 升级到最新版本 `7.8`，不过现在是 `7.9` 了，官方的文档：https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html

由于从 `2.4.1` 跨很大基本的升级，所以不能平滑的升级了，只能重新搭建集群进行数据迁移，所以迁移数据是第一步，但是呢，`2.4.1` 是可以支持多个 `type` 的，现在的新版已经不能支持多个 `type` 了，所以在迁移的过程中要将每一个 `type` 建立相应的索引，目前只是根据原先的 `type` 之间创建新的索引，还没考虑到业务的需求，这个可能需要重新设计索引。

本文主要针对在实际过程中遇到的几个问题进行阐述，第一步迁移的方案选择，第二怎么去迁移，第三对遇到的问题进行解决

## 1.迁移方案的选择

主要参考下这篇文章：https://www.jianshu.com/p/50ef4c9090f0

我个人觉得比较熟悉的方法选择三种：

一个是备份和还原，也就是说使用snapshot，官方的网址：https://www.elastic.co/guide/en/elasticsearch/reference/current/snapshot-restore.html

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234007.png)

 

然后发现只能跨一个版本升级，并不不符合我目前的需求，所以排除这个方案。

第二个是使用 `elk` 的方式，使用 `logstash`，我有另外一篇文章介绍这个的玩法：[从0到1学会logstash的玩法（ELK）](https://www.cnblogs.com/zsql/p/13143445.html)

`logstash` 的官方文档：https://www.elastic.co/guide/en/logstash/current/index.html

这个方法确实可以实现迁移，但是对于不熟悉这个 `elk` 的童鞋来说有点难，因为刚开始接触，也不是那么的友好，所以我在这里并没有优先选择。

第三种方案就是采用 `elasticsearch` 的一个接口 `_reindex`，这个方法很简单，官方文档也很详细，上手也比较快，所以选择了该方案，以前也没接触过，所以也要研究下吧，所以就有官方的文档：https://www.elastic.co/guide/en/elasticsearch/reference/7.9/docs-reindex.html，对英文不是很熟的童鞋完全可以搜索其他博客，很多博客都有详细的介绍，我也是看别人的博客，但是忘记网址了，然后再来看官网补充下，毕竟官网的是比较可信点。

## 2.迁移的语法

我们的需求是跨集群进行数据迁移，所以这里不说同集群的迁移，因为对于跨集群来说，同一个集群更简单。

第一步：配置新版 `es` 的参数，就是配置白名单，允许哪个集群可以 `reindex` 数据进来，在官网 `copy` 了一段，根据自己的实际情况进行修改就好，需要重启

```
reindex.remote.whitelist: "otherhost:9200, another:9200, 127.0.10.*:9200, localhost:*"
```

第二步：就是在新版的 `es` 上建立索引，设置好分片，副本（刚开始为0吧）等，因为自动创建的分片和副本都是1，所以自己先创建好索引比较好

第三步：直接配置啦，这里我就直接贴代码了，具体的介绍可以去看官网啦

```
curl -u user:password   -XPOST "http://ip_new:port/_reindex" -H 'Content-Type:application/json'  -d '  #这里的user，port，ip，password都是新集群的
{
  "conflicts": "proceed",
  "source": {
    "remote": { #这是2.4.1的配置
      "host": "http://ip:port/",  
      "username": "user",
      "password": "password"
    },
    "index": "index_name_source", #2.4.1的要迁移的索引
    "query": {
       "term": {
        "_type":"type_name"  #查询单个type的数据
}
    },
    "size": 6000  #这个参数可以根据实际情况调整
  },
  "dest": {
    "index": "index_name_dest",  #这里是新es的索引名字
  }
}'
```

好，很好，好了，那就跑起来，刚开始第一个小的索引跑起来没任何问题，刚开始以为就这么简单，后来索引大了，不合理的设计，就出现下面的问题了

## 3.遇到的问题和解决方案

我。。。这是神马设计，一个文档的 `id` 这么长 `id is too long, must be no longer than 512 bytes but was: 574`;

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234053.png)

 因为在 `elasticsearch7.8` 中的 `id` 最长为512啦，可以看看这个：https://github.com/elastic/elasticsearch/pull/16036/commits/99052c3fef16d6192af0839558ce3dbf23aa148d

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234108.png)

没啥不服气的吧，那怎么解决这个问题呢，`id` 好像不好改吧，网上都说要自己写代码去重新缩短这个 `id` 的长度。好吧，感觉有点复杂了，我又去看 `reindex` 这个文档的，官方文档啦，原来还支持脚本呢，但是这里这个支持的脚本有点强大，比那个 `update_by_query` 的脚本强大，可以改 `id` 哦

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234120.png)

 看到没，这些都可以修改，看到了希望有没有？接下我就修改迁移的代码啦：

```
curl -u user:password   -XPOST "http://ip_new:port/_reindex" -H 'Content-Type:application/json'  -d '  #这里的user，port，ip，password都是新集群的
{
  "conflicts": "proceed",
  "source": {
    "remote": { #这是2.4.1的配置
      "host": "http://ip:port/",  
      "username": "user",
      "password": "password"
    },
    "index": "index_name_source",
    "query": {
       "term": {
        "_type":"type_name"  #查询单个type的数据
　　}
    },
    "size": 6000  #这个参数可以根据实际情况调整
  },
  "dest": {
    "index": "index_name_dest",  #这里是新es的索引名字
  },
  "script": {
    "source": "if (ctx._id.length()>512) {ctx._id = ctx._id.substring(0,511)}",  #这里我简单粗暴，截断了，各位可以根据需求去改，这里和java的语法相似的
    "lang": "painless"
  }
}'
```

果然解决了问题，迁移数据没问题了，很好，非常好，但是总是不那么如意，当一个索引很大，3+个T的数据（吐槽下，什么鬼，原来的索引还只有6个分片，这个设计。。。）就会 `curl` 出问题了，出问题了，中途中断了，说是无法接收到网络数据：

`curl: (56) Failure when receiving data from the peer #`这个问题网上很多人都问，但是都没解决方案，有人说这是bug

就这么认命了吗，每次跑了一半多，白干活了，这个错误也是真的没啥详细说明呀，有点惨，我想升级这个 `curl` 的版本，但是升级失败了

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234223.png)

 算了，没劲，那就换一个 `centos7.x` 的机器看看 `curl` 的版本吧，果然比这个高一些，那就换机器继续试试啦。果然没让我失望，没在出现这个问题了。好吧顺利的去迁移数据去了，不过那个 `size` 参数是可以调整的，太大了会报错的，要根据`文档的大小*size`，这个值在 `5-15M` 之间速度会比较快一些，我这也还没对比测试。







