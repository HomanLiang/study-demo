[toc]



# 实战：ELK日志分析系统

## 1.为什么要用ELK

`ELK` 实际上是三个工具，`Elastricsearch + LogStash + Kibana`，通过 `ELK`，用来收集日志还有进行日志分析，最后通过可视化UI进行展示。一开始业务量比较小的时候，通过简单的 `SLF4J+Logger` 在服务器打印日志，通过 `grep` 进行简单查询，但是随着业务量增加，数据量也会不断增加，所以使用 `ELK` 可以进行大数量的日志收集和分析

## 2.简单画了一下架构图

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234547.png)

**在环境配置中，主要介绍Mac和linux配置，windows系统大致相同，当然，前提是大家都安装了JDK1.8及以上版本~**

```
[root@VM_234_23_centos ~]# java -version
java version "1.8.0_161"
Java(TM) SE Runtime Environment (build 1.8.0_161-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.161-b12, mixed mode)
```

> 高版本的ELK同样需要高版本的JDK支持，本文配置的ELK版本是6.0+，所以需要的JDK版本不小于1.8

------

## 3.ElasticSearch

> `Elasticsearch` 是一个分布式的 `RESTful` 风格的搜索和数据分析引擎，能够解决不断涌现出的各种用例。作为 `Elastic Stack` 的核心，它集中存储您的数据，帮助您发现意料之中以及意料之外的情况。

Mac安装和运行

```
安装：brew install elasticsearch
运行：elasticsearch
```

linux: 从Elasticsearch官方地址下载（也可以下载完，通过ftp之类的工具传上去），gz文件的话通过tar进行解压缩，然后进入bin目录下运行软件

```
[root@VM_234_23_centos app]# curl -L -O https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.2.4.tar.gz
[root@VM_234_23_centos app]# tar -zxvf elasticsearch-6.2.4.tar.gz
[root@VM_234_23_centos app]# cd elasticsearch-6.2.4
[root@VM_234_23_centos elasticsearch-6.2.4]# ./bin/elasticsearch
```

> 在Linux机器上，运行elasticsearch需要一个新的用户组，文章最后有Elastic在linux安装的踩坑记录

------

## 4.Logstash

> Logstash 是开源的服务器端数据处理管道，能够同时从多个来源采集数据，转换数据，然后将数据发送到您最喜欢的 “存储库” 中。（我们的存储库当然是 Elasticsearch。）-官方卖萌

**4.1. 软件安装**

Mac安装：

```
brew install logstash
```

linux安装：

```
[root@VM_234_23_centos app]# curl -L -O https://artifacts.elastic.co/downloads/logstash/logstash-6.3.2.tar.gz
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  137M  100  137M    0     0  5849k      0  0:00:24  0:00:24 --:--:-- 6597k
[root@VM_234_23_centos app]# tar -zxvf logstash-6.3.2.tar.gz
```

**4.2. 修改配置文件**

```
vim /etc/logstash.conf
```

**conf文件，指定要使用的插件，和配置对应的elasticsearch的hosts**

```
input { stdin { } }
output {
  elasticsearch { hosts => ["localhost:9200"] }
  stdout { codec => rubydebug }
}
```

**4.3. 运行**

```
bin/logstash -f logstash.conf
```

**4.4. 访问http://localhost:9600/**

```
{
 "host": "=-=",
 "version": "6.2.4",
 "http_address": "127.0.0.1:9600",
 "id": "5b47e81f-bdf8-48fc-9537-400107a13bd2",
 "name": "=-=",
 "build_date": "2018-04-12T22:29:17Z",
 "build_sha": "a425a422e03087ac34ad6949f7c95ec6d27faf14",
 "build_snapshot": false
}
```

**在elasticsearch日志中，也能看到logstash正常加入的日志**

```
[2018-08-16T14:08:36,436][INFO ][o.e.c.m.MetaDataIndexTemplateService] [f2s1SD8] adding template [logstash] for index patterns [logstash-*]
```

看到这种返回值，表示已经成功安装和启动

**踩坑**

在运行的那一步，有可能遇到内存分配错误

```
Java HotSpot(TM) 64-Bit Server VM warning: INFO: os::commit_memory(0x00000000c5330000, 986513408, 0) failed; error=’Cannot allocate memory’ (errno=12)
```

这个错误很明显就是内存不足，由于个人购买的是腾讯云1G内存的服务器（如果是壕，请随意购买更高的配置=-=），已经运行了elasticsearch，导致logstash分配不到足够的内存，所以最后要修改一下jvm配置。

```
[root@VM_234_23_centos logstash-6.3.2]# cd config/
[root@VM_234_23_centos config]# ll
total 28
-rw-r--r-- 1 root root 1846 Jul 20 14:19 jvm.options
-rw-r--r-- 1 root root 4466 Jul 20 14:19 log4j2.properties
-rw-r--r-- 1 root root 8097 Jul 20 14:19 logstash.yml
-rw-r--r-- 1 root root 3244 Jul 20 14:19 pipelines.yml
-rw-r--r-- 1 root root 1696 Jul 20 14:19 startup.options
[root@VM_234_23_centos config]# vim jvm.options
```

**将-Xms1g -Xmx1g修改为**

```
-Xms256m  
-Xmx256m
```

**然后就能正常启动了~~**

## 5.Kibana

**5.1. 软件安装**

> Kibana 让您能够可视化 Elasticsearch 中的数据并操作 Elastic Stack，因此您可以在这里解开任何疑问：例如，为何会在凌晨 2:00 被传呼，雨水会对季度数据造成怎样的影响。（而且展示的图标十分酷炫）

Mac安装

```
brew install kibana
```

linux安装，官方下载地址

```
[root@VM_234_23_centos app]# curl -L -O https://artifacts.elastic.co/downloads/kibana/kibana-6.3.2-linux-x86_64.tar.gz
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0  195M    0  271k    0     0  19235      0  2:57:54  0:00:14  2:57:40 26393
```

**在这一步，有可能下载速度奇慢，所以我本地下载好之后，通过rz命令传输到服务器**

```
[root@VM_234_23_centos app]# rz
rz waiting to receive.
Starting zmodem transfer.  Press Ctrl+C to cancel.
Transferring kibana-6.3.2-linux-x86_64.tar.gz...
  100%  200519 KB     751 KB/sec    00:04:27       0 Errors  

[root@VM_234_23_centos app]# tar -zxvf kibana-6.3.2-linux-x86_64.tar.gz
```

**5.2. 修改配置**

> 修改 config/kibana.yml 配置文件，设置 elasticsearch.url 指向 Elasticsearch 实例。
>
> 如果跟我一样使用默认的配置，可以不需要修改该文件

**5.3. 启动**

```
[root@VM_234_23_centos kibana]# ./bin/kibana
```

**5.4. 访问 http://localhost:5601/app/kibana#/home?_g=()**

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234759.png)

**界面显示了这么多功能，下面通过整合SLF4J+LogBack**

------

## 6.整合Spring+Logstash

**6.1. 修改logstash.conf后，重新启动logstash**

```
input { 
  # stdin { }
  tcp { 
    # host:port就是上面appender中的 destination，
 # 这里其实把logstash作为服务，开启9250端口接收logback发出的消息 
    host => "127.0.0.1" port => 9250 mode => "server" tags => ["tags"] codec => json_lines 
  }
}
output {
  elasticsearch { hosts => ["localhost:9200"] }
  stdout { codec => rubydebug }
}
```

**6.2. 在Java应用中引用依赖**

```
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>5.2</version>
</dependency>
```

Spring Boot 基础教程看这里，太全了：https://github.com/javastacks/spring-boot-best-practice

**6.3. 在Logback.xml中配置日志输出**

```
<!--日志导出的到 Logstash-->
<appender name="stash"
              class="net.logstash.logback.appender.LogstashTcpSocketAppender">
   <destination>localhost:9250</destination>
   <!-- encoder必须配置,有多种可选 -->
   <encoder charset="UTF-8"
            class="net.logstash.logback.encoder.LogstashEncoder" >
       <!-- "appname":"ye_test" 的作用是指定创建索引的名字时用，并且在生成的文档中会多了这个字段  -->
       <customFields>{"appname":"ye_test"}</customFields>
   </encoder>
</appender>  
    
<root level="INFO">
    <appender-ref ref="stash"/>
</root>
```

**由于我在第一步骤中，没有指定对应的index，所以在服务启动的时候，日志采集器Logstash帮我自动创建了logstash-timestamp的index。**

**6.4. 在kibana中添加index索引**

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234810.webp)

**6.5. 在左边discover中查看索引信息**

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234814.png)

**6.6. 添加可视化图表Visualize**

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210503234817.png)

**还有更多功能还在探索中，首先环境搭起来才会用动力继续去学习~**

------

## 7.踩坑记录

### 7.1.启动报错

> uncaught exception in thread [main] org.elasticsearch.bootstrap.StartupException: java.lang.RuntimeException: can not run elasticsearch as root

原因：不能使用Root权限登录

解决方案：切换用户

```
[root@VM_234_23_centos ~]# groupadd es
[root@VM_234_23_centos ~]# useradd es -g es -p es
[root@VM_234_23_centos ~]# chown es:es /home/app/elasticsearch/
# 切换用户，记得su - ，这样才能获得环境变量
[root@VM_234_23_centos ~]# sudo su - es
```

> Exception in thread “main” java.nio.file.AccessDeniedException:

错误原因：使用非 root用户启动ES，而该用户的文件权限不足而被拒绝执行。

解决方法：chown -R 用户名:用户名 文件（目录）名

例如：chown -R abc:abc searchengine 再启动ES就正常了

另外，常见的 Linux 面试题和答案我都整理好了，关注公众号Java技术栈，在后台回复：面试，可以获取，非常齐全。

------

### 7.2.elasticsearch启动后报Killed

```
[2018-07-13T10:19:44,775][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [aggs-matrix-stats]
[2018-07-13T10:19:44,779][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [analysis-common]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [ingest-common]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [lang-expression]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [lang-mustache]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [lang-painless]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [mapper-extras]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [parent-join]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [percolator]
[2018-07-13T10:19:44,780][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [rank-eval]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [reindex]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [repository-url]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [transport-netty4]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [tribe]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-core]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-deprecation]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-graph]
[2018-07-13T10:19:44,781][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-logstash]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-ml]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-monitoring]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-rollup]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-security]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-sql]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-upgrade]
[2018-07-13T10:19:44,782][INFO ][o.e.p.PluginsService     ] [f2s1SD8] loaded module [x-pack-watcher]
[2018-07-13T10:19:44,783][INFO ][o.e.p.PluginsService     ] [f2s1SD8] no plugins loaded
Killed
```

修改config目录下的jvm.options，将堆的大小设置小一点

```
# Xms represents the initial size of total heap space
# Xmx represents the maximum size of total heap space

-Xms512m
-Xmx512m
```

------

### 7.3.虚拟内存不足

> max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]

```
[2018-07-13T14:02:06,749][DEBUG][o.e.a.ActionModule       ] Using REST wrapper from plugin org.elasticsearch.xpack.security.Security
[2018-07-13T14:02:07,249][INFO ][o.e.d.DiscoveryModule    ] [f2s1SD8] using discovery type [zen]
[2018-07-13T14:02:09,173][INFO ][o.e.n.Node               ] [f2s1SD8] initialized
[2018-07-13T14:02:09,174][INFO ][o.e.n.Node               ] [f2s1SD8] starting ...
[2018-07-13T14:02:09,539][INFO ][o.e.t.TransportService   ] [f2s1SD8] publish_address {10.105.234.23:9300}, bound_addresses {0.0.0.0:9300}
[2018-07-13T14:02:09,575][INFO ][o.e.b.BootstrapChecks    ] [f2s1SD8] bound or publishing to a non-loopback address, enforcing bootstrap checks
ERROR: [1] bootstrap checks failed
[1]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
[2018-07-13T14:02:09,621][INFO ][o.e.n.Node               ] [f2s1SD8] stopping ...
[2018-07-13T14:02:09,726][INFO ][o.e.n.Node               ] [f2s1SD8] stopped
[2018-07-13T14:02:09,726][INFO ][o.e.n.Node               ] [f2s1SD8] closing ...
[2018-07-13T14:02:09,744][INFO ][o.e.n.Node               ] [f2s1SD8] closed
```

需要修改虚拟内存的大小（在root权限下）

```
[root@VM_234_23_centos elasticsearch]# vim /etc/sysctl.conf
# 插入下列代码后保存退出
vm.max_map_count=655360
[root@VM_234_23_centos elasticsearch]# sysctl -p
# 最后重启elastricsearch
```

## 8.使用Filebeat

### 8.1.认识Beats

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183253.png)

Beats是用于单用途数据托运人的平台。它们以轻量级代理的形式安装，并将来自成百上千台机器的数据发送到Logstash或Elasticsearch。

（画外音：通俗地理解，就是**采集数据**，并**上报**到Logstash或Elasticsearch）

Beats对于收集数据非常有用。它们位于你的服务器上，将数据集中在Elasticsearch中，Beats也可以发送到Logstash来进行转换和解析。

为了捕捉（捕获）数据，Elastic提供了各种Beats：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183315.png)

Beats可以直接（或者通过Logstash）将数据发送到Elasticsearch，在那里你可以进一步处理和增强数据，然后在Kibana中将其可视化。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183452.png)

### 8.2.Filebeat

**第1步：安装Filebeat**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183509.png)

**第2步：配置Filebeat**

配置文件：filebeat.yml

为了配置Filebeat：

**1. 定义日志文件路径**

对于最基本的Filebeat配置，你可以使用单个路径。例如：

```
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/*.log
```

在这个例子中，获取在/var/log/*.log路径下的所有文件作为输入，这就意味着Filebeat将获取/var/log目录下所有以.log结尾的文件。

为了从预定义的子目录级别下抓取所有文件，可以使用以下模式：/var/log/*/*.log。这将抓取/var/log的子文件夹下所有的以.log结尾的文件。它不会从/var/log文件夹本身抓取。目前，不可能递归地抓取这个目录下的所有子目录下的所有.log文件。

（画外音：

　　假设配置的输入路径是/var/log/*/*.log，假设目录结构是这样的：

　　![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183554.gif)

　　那么只会抓取到2.log和3.log，而不会抓到1.log和4.log。因为/var/log/aaa/ccc/1.log和/var/log/4.log不会被抓到。

）

**2. 如果你发送输出目录到Elasticsearch（并且不用Logstash），那么设置IP地址和端口以便能够找到Elasticsearch：**

```
output.elasticsearch:
    hosts: ["192.168.1.42:9200"]
```

**3. 如果你打算用Kibana仪表盘，可以这样配置Kibana端点：**

```
setup.kibana:
      host: "localhost:5601"
```

**4. 如果你的Elasticsearch和Kibana配置了安全策略，那么在你启动Filebeat之前需要在配置文件中指定访问凭据。例如：**

[![复制代码](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183554.gif)](javascript:void(0);)

```
output.elasticsearch:
      hosts: ["myEShost:9200"]
      username: "filebeat_internal"
      password: "{pwd}" 
setup.kibana:
      host: "mykibanahost:5601"
      username: "my_kibana_user"  
      password: "{pwd}"
```

[![复制代码](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183554.gif)](javascript:void(0);)

### 第3步：配置Filebeat以使用Logstash

如果你想使用Logstash对Filebeat收集的数据执行额外的处理，那么你需要将Filebeat配置为使用Logstash。

```
output.logstash:
      hosts: ["127.0.0.1:5044"]
```

### 第4步：在Elasticsearch中加载索引模板

在Elasticsearch中，索引模板用于定义设置和映射，以确定如何分析字段。（画外音：相当于定义索引文档的数据结构，因为要把采集的数据转成标准格式输出）

Filebeat包已经安装了推荐的索引模板。如果你接受filebeat.yml中的默认配置，那么Filebeat在成功连接到Elasticsearch以后会自动加载模板。如果模板已经存在，不会覆盖，除非你配置了必须这样做。

通过在Filebeat配置文件中配置模板加载选项，你可以禁用自动模板加载，或者自动加载你自己的目标。

**配置模板加载**

默认情况下，如果Elasticsearch输出是启用的，那么Filebeat会自动加载推荐的模板文件 ——— fields.yml。

- 加载不同的模板

- ```
  setup.template.name: "your_template_name"
  setup.template.fields: "path/to/fields.yml"
  ```

  覆盖一个已存在的模板

- ```
  setup.template.overwrite: true
  ```

  禁用自动加载模板

- ```
  setup.template.enabled: false
  ```

  修改索引名称

- [![复制代码](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183554.gif)](javascript:void(0);)

  ```
  # 默认情况下，Filebeat写事件到名为filebeat-6.3.2-yyyy.MM.dd的索引，其中yyyy.MM.dd是事件被索引的日期。为了用一个不同的名字，你可以在Elasticsearch输出中设置index选项。例如：
  output.elasticsearch.index: "customname-%{[beat.version]}-%{+yyyy.MM.dd}"
  setup.template.name: "customname"
  setup.template.pattern: "customname-*"
  setup.dashboards.index: "customname-*"
  ```

  [![复制代码](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183554.gif)](javascript:void(0);)

   

**手动加载模板**

```
./filebeat setup --template -E output.logstash.enabled=false -E 'output.elasticsearch.hosts=["localhost:9200"]'
```

### 第5步：设置Kibana dashboards

Filebeat附带了Kibana仪表盘、可视化示例。在你用dashboards之前，你需要创建索引模式，filebeat-*，并且加载dashboards到Kibana中。为此，你可以运行setup命令或者在filebeat.yml配置文件中配置dashboard加载。

```
./filebeat setup --dashboards
```

### 第6步：启动Filebeat

```
./filebeat -e -c filebeat.yml -d "publish"
```

### 第7步：查看Kibana仪表板示例

```
http://127.0.0.1:5601
```

## 完整的配置

[![复制代码](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828183554.gif)](javascript:void(0);)

```
#=========================== Filebeat inputs ==============
filebeat.inputs:

- type: log

  enabled: true

  paths:
    - /var/log/*.log

#============================== Dashboards ===============
setup.dashboards.enabled: false

#============================== Kibana ==================
setup.kibana:
	host: "192.168.101.5:5601"

#-------------------------- Elasticsearch output ---------
output.elasticsearch:
  	hosts: ["localhost:9200"]
```

**启动Elasticsearch**

　　/usr/local/programs/elasticsearch/elasticsearch-6.3.2/bin/elasticsearch

**启动Kibana**

　　/usr/local/programs/kibana/kibana-6.3.2-linux-x86_64/bin/kibana

**设置dashboard**

　　./filebeat setup --dashboards

**启动Filebeat**

　　./filebeat -e -c filebeat.yml -d "publish"

**浏览器访问** **http://192.168.101.5:5601**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20220828211731.png)

 **查看索引**

请求：

```
curl -X GET "localhost:9200/_cat/indices?v"
```

响应：

```
health status index                     uuid                   pri rep docs.count docs.deleted store.size pri.store.size
yellow open   bank                      59jD3B4FR8iifWWjrdMzUg   5   1       1000            0    475.1kb        475.1kb
green  open   .kibana                   DzGTSDo9SHSHcNH6rxYHHA   1   0        153           23    216.8kb        216.8kb
yellow open   filebeat-6.3.2-2018.08.08 otgYPvsgR3Ot-2GDcw_Upg   3   1        255            0     63.7kb         63.7kb
yellow open   customer                  DoM-O7QmRk-6f3Iuls7X6Q   5   1          1            0      4.5kb          4.5kb
```











