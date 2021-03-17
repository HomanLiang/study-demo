[toc]



# Kafka 监控

## Kafka Eagle

在Kafka的监控系统中有很多优秀的开源监控系统。比如Kafka-manager，open-faclcon，zabbix等主流监控工具均可直接监控kafka。Kafka集群性能监控可以从消息网络传输，消息传输流量，请求次数等指标来衡量集群性能。这些指标数据可以通过访问kafka集群的JMX接口获取。

**由于Kafka Eagle监控系统核心模块采用java编程语言实现，因此启动Kafka Eagle 监控系统需要依赖Java运行环境（JDK），建议使用java 7以上版本，推荐使用JDK1.8。关于jdk的安装我就不废话了，大家自行安装一下。**

**一.kafka Eagle简介**

**1>.kafka Eagle监控系统**

　　kafka Eagle监控系统的目标是做一个部署简单，开发容易，使用方便的kafka消息监控系统。

**2>.Kafka Eagle最新版本的**

　　下载地址：http://download.kafka-eagle.org/

**3>.Kafka Eagle 源代码**

　　下载地址：https://github.com/smartloli/kafka-eagle

**4>.Kafka Eagle 各个版本**

　　下载地址：http://www.kafka-eagle.org/articles/docs/changelog/changelog.html

 

**二.安装Kafka Eagle**

**1>.使用wget工具下载kafka Eagle 软件安装包**

```
[root@node105 ~]# 
[root@node105 ~]# mkdir  -pv /yinzhengjie/kafka-eagle && cd /yinzhengjie/kafka-eagle
mkdir: created directory ‘/yinzhengjie/kafka-eagle’
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# wget https://github.com/smartloli/kafka-eagle-bin/archive/v1.2.0.tar.gz
--2018-11-14 15:21:20--  https://github.com/smartloli/kafka-eagle-bin/archive/v1.2.0.tar.gz
Resolving github.com (github.com)... 52.74.223.119, 13.250.177.223, 13.229.188.59
Connecting to github.com (github.com)|52.74.223.119|:443... connected.
HTTP request sent, awaiting response... 302 Found
Location: https://codeload.github.com/smartloli/kafka-eagle-bin/tar.gz/v1.2.0 [following]
--2018-11-14 15:21:21--  https://codeload.github.com/smartloli/kafka-eagle-bin/tar.gz/v1.2.0
Resolving codeload.github.com (codeload.github.com)... 13.250.162.133, 54.251.140.56, 13.229.189.0
Connecting to codeload.github.com (codeload.github.com)|13.250.162.133|:443... connected.
HTTP request sent, awaiting response... 200 OK
Length: unspecified [application/x-gzip]
Saving to: ‘v1.2.0.tar.gz’

    [                                                                                                               <=>                        ] 57,443,692  1.73MB/s   in 31s    

2018-11-14 15:21:53 (1.79 MB/s) - ‘v1.2.0.tar.gz’ saved [57443692]

[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# ll
total 56100
-rw-r--r--. 1 root root 57443692 Nov 14 15:32 v1.2.0.tar.gz
[root@node105 kafka-eagle]# 

[root@node105 kafka-eagle]# wget https://github.com/smartloli/kafka-eagle-bin/archive/v1.2.0.tar.gz
```

**2>.解压安装包并创建软连接** 

```
[root@node105 kafka-eagle]# ll
total 56100
-rw-r--r--. 1 root root 57443692 Nov 14 15:32 v1.2.0.tar.gz
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# tar -zxf v1.2.0.tar.gz 
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# ll
total 56100
drwxrwxr-x. 2 root root       46 Jan 23  2018 kafka-eagle-bin-1.2.0
-rw-r--r--. 1 root root 57443692 Nov 14 15:32 v1.2.0.tar.gz
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# tar -zxf kafka-eagle-bin-1.2.0/kafka-eagle-web-1.2.0-bin.tar.gz -C /soft/
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# ln -s /soft/kafka-eagle-web-1.2.0/ /soft/kafka-eagle
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# ll /soft/ 
total 0
lrwxrwxrwx. 1 root root  19 Oct 26 20:10 jdk -> /soft/jdk1.8.0_131/
drwxr-xr-x. 8 root root 255 Oct 26 13:20 jdk1.8.0_131
drwxr-xr-x. 7 root root 101 Oct 26 13:20 kafka
lrwxrwxrwx. 1 root root  28 Nov 14 15:41 kafka-eagle -> /soft/kafka-eagle-web-1.2.0/
drwxr-xr-x. 8 root root  75 Nov 14 15:39 kafka-eagle-web-1.2.0
drwxr-xr-x. 7 root root 126 Oct 26 13:20 kafka-manager
[root@node105 kafka-eagle]# 
```

**3>.为Kafka Eagle配置环境变量（“/etc/profile”）** 

```
[root@node105 kafka-eagle]# tail /etc/profile
export GOROOT=/usr/lib/golang
export GOPATH=/home/yinzhengjie/golang

#ADD open-falcon path by yinzhengjie
export FALCON_HOME=/yinzhengjie/open-falcon/workspace
export WORKSPACE=/open-falcon

#ADD kafka-Eagle path by yinzhengjie
export KE_HOME=/soft/kafka-eagle
export PATH=$PATH:$KE_HOME/bin
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# chmod +x /soft/kafka-eagle/bin/ -R   　　#这个目录下有2个脚本，当你启动服务时，会多处一个ke.pid的文件，用于存放进程pid的，这一步必须得做，没有权限的话，你没法启动服务哟！
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# source  /etc/profile
[root@node105 kafka-eagle]#
```

**4>.编辑配置文件（/soft/kafka-eagle/conf/system-config.properties）**

 ```
[root@node101.yinzhengjie.org.cn ~]# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 2
Server version: 5.7.25-log MySQL Community Server (GPL)

Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> 
mysql> CREATE DATABASE kafka DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
Query OK, 1 row affected (0.00 sec)

mysql> 
mysql> CREATE USER kafka@'172.30.1.10%' IDENTIFIED WITH mysql_native_password BY 'yinzhengjie';
Query OK, 0 rows affected (0.00 sec)

mysql> 
mysql> GRANT ALL PRIVILEGES ON kafka.* TO kafka@'172.30.1.10%';
Query OK, 0 rows affected (0.00 sec)

mysql> 
mysql> 
mysql> quit
Bye
[root@node101.yinzhengjie.org.cn ~]# 
[root@node101.yinzhengjie.org.cn ~]# 
[root@node101.yinzhengjie.org.cn ~]# mysql -ukafka -pyinzhengjie
mysql: [Warning] Using a password on the command line interface can be insecure.
ERROR 1045 (28000): Access denied for user 'kafka'@'localhost' (using password: YES)
[root@node101.yinzhengjie.org.cn ~]# 
[root@node101.yinzhengjie.org.cn ~]# 
[root@node101.yinzhengjie.org.cn ~]# mysql -ukafka -pyinzhengjie -h node101.yinzhengjie.org.cn
mysql: [Warning] Using a password on the command line interface can be insecure.
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 4
Server version: 5.7.25-log MySQL Community Server (GPL)

Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| kafka              |
+--------------------+
2 rows in set (0.00 sec)

mysql> 
mysql> quit
Bye
[root@node101.yinzhengjie.org.cn ~]# 

[root@node101.yinzhengjie.org.cn ~]# mysql -uroot -p　　　　　　　　#对用户进行授权操作
 ```

```
[root@node105 kafka-eagle]# cat /soft/kafka-eagle/conf/system-config.properties 
######################################
# 多集群模式配置，包含多个kafka和zookeeper。
######################################
#kafka.eagle.zk.cluster.alias=cluster1,cluster2
#cluster1.zk.list=tdn1:2181,tdn2:2181,tdn3:2181
#cluster2.zk.list=xdn10:2181,xdn11:2181,xdn12:2181

kafka.eagle.zk.cluster.alias=yinzhengjie-kafka
yinzhengjie-kafka.zk.list=10.1.2.102:2181,10.1.2.103:2181,10.1.2.104:2181
######################################
# zookeeper客户端连接数限制
######################################
kafka.zk.limit.size=25

######################################
# kafka eagle webui port
######################################
kafka.eagle.webui.port=8048

######################################
# kafka 消费信息存储位置，用来兼容kafka低版本
######################################
kafka.eagle.offset.storage=kafka

######################################
# kafka eagle 设置告警邮件服务器 
######################################
kafka.eagle.mail.enable=true
kafka.eagle.mail.sa=alert_sa
kafka.eagle.mail.username=alert_sa@163.com
kafka.eagle.mail.password=mqslimczkdqabbbg
kafka.eagle.mail.server.host=smtp.163.com
kafka.eagle.mail.server.port=25

######################################
# 管理员删除kafka中topic的口令
######################################
kafka.eagle.topic.token=keadmin

######################################
# kafka 集群是否开启了认证模式
######################################
kafka.eagle.sasl.enable=false
kafka.eagle.sasl.protocol=SASL_PLAINTEXT
kafka.eagle.sasl.mechanism=PLAIN
kafka.eagle.sasl.client=/hadoop/kafka-eagle/conf/kafka_client_jaas.conf

######################################
# kafka eagle  存储监控数据的数据库地址
######################################
kafka.eagle.driver=com.mysql.jdbc.Driver
kafka.eagle.url=jdbc:mysql://127.0.0.1:3306/kafka?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull
kafka.eagle.username=kafka
kafka.eagle.password=yinzhengjie

[root@node105 kafka-eagle]# 
```

**5>.启动Kafka监控系统（此处需要对ke.sh这个脚本进行修改， ）**

```
[root@node105 kafka-eagle]# ke.sh start
Starting : KE Service Check ...
  created: META-INF/
 inflated: META-INF/MANIFEST.MF
  created: media/
  created: media/css/
  created: media/css/fonts/
  created: media/css/img/
  created: media/css/plugins/
  created: media/css/plugins/datatimepicker/
  created: media/css/plugins/select2/
  created: media/css/public/
  created: media/css/public/account/
  created: media/css/public/images/
  created: media/img/
  created: media/js/
  created: media/js/main/
  created: media/js/main/account/
  created: media/js/main/alarm/
  created: media/js/main/cluster/
  created: media/js/main/consumer/
  created: media/js/main/error/
  created: media/js/main/metrics/
  created: media/js/main/system/
  created: media/js/main/topic/
  created: media/js/plugins/
  created: media/js/plugins/codemirror/
  created: media/js/plugins/d3/
  created: media/js/plugins/datatables/
  created: media/js/plugins/datatimepicker/
  created: media/js/plugins/magicsuggest/
  created: media/js/plugins/select2/
  created: media/js/plugins/terminal/
  created: media/js/plugins/tokenfield/
  created: media/js/public/
  created: WEB-INF/
  created: WEB-INF/classes/
  created: WEB-INF/classes/org/
  created: WEB-INF/classes/org/smartloli/
  created: WEB-INF/classes/org/smartloli/kafka/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/pojo/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/quartz/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/filter/
  created: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/pojo/
  created: WEB-INF/lib/
  created: WEB-INF/views/
  created: WEB-INF/views/account/
  created: WEB-INF/views/alarm/
  created: WEB-INF/views/cluster/
  created: WEB-INF/views/consumers/
  created: WEB-INF/views/error/
  created: WEB-INF/views/main/
  created: WEB-INF/views/metrics/
  created: WEB-INF/views/public/
  created: WEB-INF/views/system/
  created: WEB-INF/views/topic/
 inflated: media/css/fonts/fontawesome-webfont.ttf
 inflated: media/css/fonts/fontawesome-webfont.woff
 inflated: media/css/fonts/glyphicons-halflings-regular.ttf
 inflated: media/css/fonts/glyphicons-halflings-regular.woff
 inflated: media/css/fonts/glyphicons-halflings-regular.woff2
 inflated: media/css/img/glyphicons-halflings.png
 inflated: media/css/plugins/datatimepicker/daterangepicker.css
 inflated: media/css/plugins/select2/select2.min.css
 inflated: media/css/public/account/hfc.ttf
 inflated: media/css/public/account/hfd.ttf
 inflated: media/css/public/account/main.css
 inflated: media/css/public/bootstrap-tokenfield.css
 inflated: media/css/public/bootstrap-treeview.min.css
 inflated: media/css/public/bootstrap.min.css
 inflated: media/css/public/codemirror.css
 inflated: media/css/public/dataTables.bootstrap.min.css
 inflated: media/css/public/font-awesome.min.css
 inflated: media/css/public/images/ui-bg_glass_75_e6e6e6_1x400.png
 inflated: media/css/public/images/ui-bg_glass_75_ffffff_1x400.png
 inflated: media/css/public/images/ui-bg_highlight-soft_75_cccccc_1x100.png
 inflated: media/css/public/images/ui-icons_222222_256x240.png
 inflated: media/css/public/images/ui-icons_454545_256x240.png
 inflated: media/css/public/images/ui-icons_888888_256x240.png
 inflated: media/css/public/jquery.terminal.min.css
 inflated: media/css/public/magicsuggest.css
 inflated: media/css/public/morris.css
 inflated: media/css/public/sb-admin.css
 inflated: media/css/public/show-hint.css
 inflated: media/css/public/tokenfield-typeahead.css
 inflated: media/img/favicon.ico
 inflated: media/img/ke_login.png
 inflated: media/js/main/account/signin.js
 inflated: media/js/main/alarm/add.js
 inflated: media/js/main/alarm/modify.js
 inflated: media/js/main/cluster/cluster.js
 inflated: media/js/main/cluster/multicluster.js
 inflated: media/js/main/cluster/zkcli.js
 inflated: media/js/main/consumer/consumers.js
 inflated: media/js/main/consumer/offset.consumer.js
 inflated: media/js/main/consumer/offset.realtime.js
 inflated: media/js/main/error/error.js
 inflated: media/js/main/index.js
 inflated: media/js/main/metrics/brokers.js
 inflated: media/js/main/metrics/trend.js
 inflated: media/js/main/system/notice.js
 inflated: media/js/main/system/resource.js
 inflated: media/js/main/system/role.js
 inflated: media/js/main/system/user.js
 inflated: media/js/main/topic/create.js
 inflated: media/js/main/topic/list.js
 inflated: media/js/main/topic/mock.js
 inflated: media/js/main/topic/msg.js
 inflated: media/js/main/topic/topic.meta.js
 inflated: media/js/plugins/codemirror/codemirror.js
 inflated: media/js/plugins/codemirror/show-hint.js
 inflated: media/js/plugins/codemirror/sql-hint.js
 inflated: media/js/plugins/codemirror/sql.js
 inflated: media/js/plugins/d3/d3.js
 inflated: media/js/plugins/d3/d3.layout.js
 inflated: media/js/plugins/datatables/dataTables.bootstrap.min.js
 inflated: media/js/plugins/datatables/jquery.dataTables.min.js
 inflated: media/js/plugins/datatimepicker/daterangepicker.js
 inflated: media/js/plugins/datatimepicker/moment.min.js
 inflated: media/js/plugins/magicsuggest/magicsuggest.js
 inflated: media/js/plugins/select2/select2.min.js
 inflated: media/js/plugins/select2/select2.min.js.bak
 inflated: media/js/plugins/terminal/jquery.terminal.min.js
 inflated: media/js/plugins/tokenfield/bootstrap-tokenfield.js
 inflated: media/js/public/bootstrap-treeview.min.js
 inflated: media/js/public/bootstrap.min.js
 inflated: media/js/public/jquery.js
 inflated: media/js/public/morris.min.js
 inflated: media/js/public/navbar.js
 inflated: media/js/public/raphael.min.js
 inflated: WEB-INF/classes/mbean-quartz.xml
 inflated: WEB-INF/classes/offsets-quartz.xml
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/AccountController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/AlarmController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/BaseController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/ClusterController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/ConsumersController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/DashboardController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/ErrorPageController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/MetricsController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/OffsetController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/ResourcesController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/RoleController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/StartupListener$ContextSchema.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/StartupListener$RunTask.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/StartupListener.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/controller/TopicController.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/MBeanDao.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/MBeanDao.xml
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/ResourcesDao.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/ResourcesDao.xml
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/RoleDao.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/RoleDao.xml
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/UserDao.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/dao/UserDao.xml
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/pojo/Role.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/pojo/RoleResource.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/pojo/Signiner.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/pojo/UserRole.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/quartz/MBeanQuartz.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/quartz/OffsetsQuartz.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/AccountService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/AlarmService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/ClusterService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/ConsumerService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/DashboardService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/AccountServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/AlarmServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/ClusterServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/ConsumerServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/DashboardServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/MetricsServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/OffsetServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/ResourceServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/RoleServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/impl/TopicServiceImpl.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/MetricsService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/OffsetService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/ResourceService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/RoleService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/service/TopicService.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/filter/SSOFilter.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/filter/SSORealm.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/pojo/Resources.class
 inflated: WEB-INF/classes/org/smartloli/kafka/eagle/web/sso/pojo/SSOAuthenticationToken.class
 inflated: WEB-INF/classes/shiro-ehcache.xml
 inflated: WEB-INF/classes/spring-mvc.xml
 inflated: WEB-INF/classes/spring-mybatis.xml
 inflated: WEB-INF/classes/spring-shiro.xml
 inflated: WEB-INF/lib/activation-1.1.jar
 inflated: WEB-INF/lib/aopalliance-1.0.jar
 inflated: WEB-INF/lib/aspectjrt-1.8.10.jar
 inflated: WEB-INF/lib/aspectjweaver-1.8.10.jar
 inflated: WEB-INF/lib/c3p0-0.9.1.1.jar
 inflated: WEB-INF/lib/calcite-core-1.6.0.jar
 inflated: WEB-INF/lib/calcite-linq4j-1.6.0.jar
 inflated: WEB-INF/lib/commons-beanutils-1.8.3.jar
 inflated: WEB-INF/lib/commons-codec-1.2.jar
 inflated: WEB-INF/lib/commons-compiler-3.0.6.jar
 inflated: WEB-INF/lib/commons-dbcp-1.2.2.jar
 inflated: WEB-INF/lib/commons-httpclient-3.0.jar
 inflated: WEB-INF/lib/commons-io-2.4.jar
 inflated: WEB-INF/lib/commons-lang-2.6.jar
 inflated: WEB-INF/lib/commons-lang3-3.5.jar
 inflated: WEB-INF/lib/commons-logging-1.1.2.jar
 inflated: WEB-INF/lib/commons-pool-1.3.jar
 inflated: WEB-INF/lib/dom4j-1.6.1.jar
 inflated: WEB-INF/lib/druid-1.0.31.jar
 inflated: WEB-INF/lib/ehcache-core-2.5.3.jar
 inflated: WEB-INF/lib/eigenbase-properties-1.1.5.jar
 inflated: WEB-INF/lib/fastjson-1.2.7.jar
 inflated: WEB-INF/lib/gson-2.2.4.jar
 inflated: WEB-INF/lib/guava-19.0.jar
 inflated: WEB-INF/lib/jackson-annotations-2.8.0.jar
 inflated: WEB-INF/lib/jackson-core-2.8.7.jar
 inflated: WEB-INF/lib/jackson-core-asl-1.9.13.jar
 inflated: WEB-INF/lib/jackson-databind-2.8.7.jar
 inflated: WEB-INF/lib/jackson-mapper-asl-1.9.13.jar
 inflated: WEB-INF/lib/janino-3.0.6.jar
 inflated: WEB-INF/lib/jline-0.9.94.jar
 inflated: WEB-INF/lib/jopt-simple-5.0.3.jar
 inflated: WEB-INF/lib/jsr305-1.3.9.jar
 inflated: WEB-INF/lib/jstl-1.2.jar
 inflated: WEB-INF/lib/junit-3.8.1.jar
 inflated: WEB-INF/lib/kafka-clients-0.10.2.0.jar
 inflated: WEB-INF/lib/kafka-eagle-api-1.2.0.jar
 inflated: WEB-INF/lib/kafka-eagle-common-1.2.0.jar
 inflated: WEB-INF/lib/kafka-eagle-core-1.2.0.jar
 inflated: WEB-INF/lib/kafka-eagle-plugin-1.2.0.jar
 inflated: WEB-INF/lib/kafka_2.11-0.10.2.0.jar
 inflated: WEB-INF/lib/log4j-1.2.17.jar
 inflated: WEB-INF/lib/lz4-1.3.0.jar
 inflated: WEB-INF/lib/mail-1.4.7.jar
 inflated: WEB-INF/lib/metrics-core-2.2.0.jar
 inflated: WEB-INF/lib/mybatis-3.2.6.jar
 inflated: WEB-INF/lib/mybatis-spring-1.2.2.jar
 inflated: WEB-INF/lib/mysql-connector-java-5.1.30.jar
 inflated: WEB-INF/lib/netty-3.7.0.Final.jar
 inflated: WEB-INF/lib/pentaho-aggdesigner-algorithm-5.1.5-jhyde.jar
 inflated: WEB-INF/lib/quartz-2.2.1.jar
 inflated: WEB-INF/lib/scala-library-2.11.8.jar
 inflated: WEB-INF/lib/scala-parser-combinators_2.11-1.0.4.jar
 inflated: WEB-INF/lib/servlet-api-2.5.jar
 inflated: WEB-INF/lib/shiro-core-1.3.2.jar
 inflated: WEB-INF/lib/shiro-ehcache-1.3.2.jar
 inflated: WEB-INF/lib/shiro-spring-1.3.2.jar
 inflated: WEB-INF/lib/shiro-web-1.3.2.jar
 inflated: WEB-INF/lib/slf4j-api-1.6.6.jar
 inflated: WEB-INF/lib/slf4j-log4j12-1.7.5.jar
 inflated: WEB-INF/lib/snappy-java-1.1.2.6.jar
 inflated: WEB-INF/lib/spring-aop-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-beans-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-context-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-context-support-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-core-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-expression-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-jdbc-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-oxm-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-test-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-tx-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-web-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/spring-webmvc-4.1.6.RELEASE.jar
 inflated: WEB-INF/lib/sqlite-jdbc-3.20.0.jar
 inflated: WEB-INF/lib/xml-apis-1.0.b2.jar
 inflated: WEB-INF/lib/zkclient-0.9.jar
 inflated: WEB-INF/lib/zookeeper-3.4.8.jar
 inflated: WEB-INF/views/account/signin.jsp
 inflated: WEB-INF/views/alarm/add.jsp
 inflated: WEB-INF/views/alarm/add_failed.jsp
 inflated: WEB-INF/views/alarm/add_success.jsp
 inflated: WEB-INF/views/alarm/modify.jsp
 inflated: WEB-INF/views/cluster/cluster.jsp
 inflated: WEB-INF/views/cluster/multicluster.jsp
 inflated: WEB-INF/views/cluster/zkcli.jsp
 inflated: WEB-INF/views/consumers/consumers.jsp
 inflated: WEB-INF/views/consumers/offset_consumers.jsp
 inflated: WEB-INF/views/consumers/offset_realtime.jsp
 inflated: WEB-INF/views/error/403.jsp
 inflated: WEB-INF/views/error/404.jsp
 inflated: WEB-INF/views/error/405.jsp
 inflated: WEB-INF/views/error/500.jsp
 inflated: WEB-INF/views/error/503.jsp
 inflated: WEB-INF/views/main/index.jsp
 inflated: WEB-INF/views/metrics/brokers.jsp
 inflated: WEB-INF/views/metrics/trend.jsp
 inflated: WEB-INF/views/public/css.jsp
 inflated: WEB-INF/views/public/kindeditor.jsp
 inflated: WEB-INF/views/public/navbar.jsp
 inflated: WEB-INF/views/public/script.jsp
 inflated: WEB-INF/views/public/tagcss.jsp
 inflated: WEB-INF/views/public/tcss.jsp
 inflated: WEB-INF/views/public/tscript.jsp
 inflated: WEB-INF/views/system/notice.jsp
 inflated: WEB-INF/views/system/resource.jsp
 inflated: WEB-INF/views/system/role.jsp
 inflated: WEB-INF/views/system/user.jsp
 inflated: WEB-INF/views/topic/add_failed.jsp
 inflated: WEB-INF/views/topic/add_success.jsp
 inflated: WEB-INF/views/topic/create.jsp
 inflated: WEB-INF/views/topic/list.jsp
 inflated: WEB-INF/views/topic/mock.jsp
 inflated: WEB-INF/views/topic/msg.jsp
 inflated: WEB-INF/views/topic/topic_meta.jsp
 inflated: WEB-INF/web.xml
  created: META-INF/maven/
  created: META-INF/maven/org.smartloli.kafka.eagle/
  created: META-INF/maven/org.smartloli.kafka.eagle/kafka-eagle-web/
 inflated: META-INF/maven/org.smartloli.kafka.eagle/kafka-eagle-web/pom.xml
 inflated: META-INF/maven/org.smartloli.kafka.eagle/kafka-eagle-web/pom.properties
*******************************************************************
* Kafka Eagle system monitor port successful... *
*******************************************************************
Status Code[0]
[Job done!]
Welcome to
    __ __    ___     ____    __ __    ___            ______    ___    ______    __     ______
   / //_/   /   |   / __/   / //_/   /   |          / ____/   /   |  / ____/   / /    / ____/
  / ,<     / /| |  / /_    / ,<     / /| |         / __/     / /| | / / __    / /    / __/   
 / /| |   / ___ | / __/   / /| |   / ___ |        / /___    / ___ |/ /_/ /   / /___ / /___   
/_/ |_|  /_/  |_|/_/     /_/ |_|  /_/  |_|       /_____/   /_/  |_|\____/   /_____//_____/   
                                                                                             

Version 1.2.0
*******************************************************************
* Kafka Eagle Service has started success! *
* Welcome, Now you can visit 'http://<your_host_or_ip>:port/ke' *
* Account:admin ,Password:123456                          *
*******************************************************************
* <Usage> ke.sh [start|status|stop|restart|stats] </Usage> *
* <Usage> http://ke.smartloli.org/ </Usage> *
*******************************************************************
[root@node105 kafka-eagle]# 

[root@node105 kafka-eagle]# ke.sh start　　　　　　　　#启动Kafka Eagle系统
```

![795254-20181114171756333-579309297](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317234652.png)

**6>.启动成功的标志**

![795254-20181114172739189-1609507279](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317234744.png)

**7>.遇到的一系列坑**

**启动时报错如下：**

```
[root@node105 kafka-eagle]# ke.sh start
Starting : KE Service Check ...
Error: The JAVA_HOME environment variable is not defined correctly.
Error: This environment variable is needed to run this program.
[root@node105 kafka-eagle]#
```

![795254-20181114163140155-563976930](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317234816.png) 

**查看启动脚本（vi /soft/kafka-eagle/bin/ke.sh ）** 

![795254-20181114171037413-210831510](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317234837.png)

发现我的java环境已经配置好啦～可以直接查看相应的环境变量～

```
[root@node105 kafka-eagle]# java -version
java version "1.8.0_131"
Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# echo $JAVA_HOME
/soft/jdk
[root@node105 kafka-eagle]# 
```

**解决方案如下：**

![795254-20181114171446276-58697554](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317234933.png)

**8>.查看端口是否存在**

```
[root@node105 kafka-eagle]# hostname
node105.yinzhengjie.org.cn
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# 
[root@node105 kafka-eagle]# netstat -untalp | grep 8048
tcp6       0      0 :::8048                 :::*                    LISTEN      5184/java           
[root@node105 kafka-eagle]# 
```

**9>.访问kafka eagle的webUI服务（用户名：admin，密码：123456）**

```
http://node105.yinzhengjie.org.cn:8048/ke/
```

![795254-20181114173650290-1062611043](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235003.png)

**10>.登陆成功的界面如下**

 ![795254-20181114174142745-1641661964](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235024.png)

 

**三.Kafka Eagle快速入门**

**都已经部署成功了，剩下给大家介绍界面的功能感觉是多余的，不过大家也别嫌我啰嗦，部署成功了，大家点点鼠标也就知道咋回事了～其实和Kafka manager差不多多少～只不过功能要比kafka manager更多一点而已啦！**

**1>.创建topic**

![795254-20181114175355031-2038701268](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235158.png)

**2>.查看topic**

 ![795254-20181114175846117-506327670](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235213.png)

**3>.kafka SQL查询界面（我没咋用过哈～有时间可以研究研究～）**

![795254-20181114180759278-2033671644](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235228.png)

**4>.发送消息到指定的topic**

 ![795254-20181114181059878-1663538645](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235237.png)

**别忘了启动一个消费者进行测试，查看是否能拿到数据，很显然，我是拿到数据啦：**

![795254-20181114181226182-792073933](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235300.png)

**5>.查看所有消费者的情况**

 ![795254-20181114181525130-961814087](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235308.png)

**6>.查看正在监控的集群**

![795254-20181114182522472-2100100279](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235321.png)

**7>.监控多个kafka集群**

![795254-20181114182212442-1318519051](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235331.png)

**8>.zookeeper客户端操作命令**

![795254-20181114182927889-295573900](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235341.png)

**9>.性能指标监控（broker总的流量监控）**

![795254-20181114183406334-284467433](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235358.png)

**10>.通过JMX获取数据，监控Kafka客户端，生产端，消息数量，请求数量，处理时间和其他数据，以可视化性能。**

![795254-20181114183618184-913387496](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235410.png)

**11>.kafka系统管理**

![795254-20181114183909070-1901115317](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235418.png)

**12>.关于kafka Eagle脚本的命令**

**详情请参考官网：http://www.kafka-eagle.org/articles/docs/quickstart/shell.html。说句实话，我一般不咋夸人，这家公司官网的图解做的很好，在这里，我本人希望Kafka Eagle功能越来越强，比如支持微信，集群压力测试等等。**

![795254-20181114184604981-153076461](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/kafka-demo/20210317235428.png)

 