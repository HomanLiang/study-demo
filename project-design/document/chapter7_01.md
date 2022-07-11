[toc]



# PinPoint

## 1.序章

pinpoint是开源在github上的一款APM监控工具，它是用Java编写的，用于大规模分布式系统监控。它对性能的影响最小（只增加约3％资源利用率），安装agent是无侵入式的，只需要在被测试的Tomcat中加上3句话，打下探针，就可以监控整套程序了。这篇Blog主要是想记录一下它安装的过程，方便日后查阅。

我安装它用到的2台 CentOS6.8 虚拟机，一台主要部署pinpoint的主程序，一台模拟测试环境。配置如下：

| IP              | 操作系统   | 安装项         | 描述                                           |
| :-------------- | :--------- | :------------- | :--------------------------------------------- |
| 192.168.245.136 | CentOS 6.8 | pinpoint       | pinpoint的web展示端，逻辑控制机，以及Hbase存储 |
| 192.168.245.135 | CentOS 6.8 | pinpoint-agent | 主要用来采集数据，发送给pinpoint处理           |

java 1.7 http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html

pinpoint https://github.com/naver/pinpoint

我将需要的资源都整合起来了，上传至百度网盘

百度网盘: < 链接：https://pan.baidu.com/s/1WC3VhyhgicqMMmTgbxMziA 密码：vdp6 >

下面是官方的一些截图，很帅，很直观

![demo0](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710193231.png)

![demo1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710193236.png)

![demo2](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710193327.png)

![demo3](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710193339.png)

## 2.环境配置

### 2.1.获取需要的依赖包

进入home目录，创建一个"pp_res"的资源目录，用来存放需要安装的包

```bash
mkdir /home/pp_res
cd /home/pp_res/
```

使用xshell等类似的工具，将需要的文件上传到Linux虚拟机中，主要要传的文件都在[百度网盘](http://pan.baidu.com/s/1eRU5RW2)中

1. jdk7 --- Java运行环境
2. hbase-1.0 --- 数据库，用来存储监控信息
3. tomcat8.0 --- Web服务器
4. pinpoint-collector.war --- pp的控制器
5. pinpoint-web.war --- pp展示页面
6. pp-collector.init --- 用来快速启动pp-col，不要也可以
7. pp-web.init --- 用来快速启动pp-web，不要也可以

![xshell](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710193457.png)

使用ll命令，查看一下是否上传成功

```bash
[root@localhost pp_res]# ll
total 367992
-rw-r--r--. 1 root root   9277365 Nov 15 00:07 apache-tomcat-8.0.36.tar.gz
-rw-r--r--. 1 root root 103847513 Nov 15 00:07 hbase-1.0.3-bin.tar.gz
-rw-r--r--. 1 root root 153512879 Nov 15 00:07 jdk-7u79-linux-x64.tar.gz
-rw-r--r--. 1 root root   6621915 Nov 15 00:07 pinpoint-agent-1.5.2.tar.gz
-rw-r--r--. 1 root root  31339914 Nov 15 00:07 pinpoint-collector-1.5.2.war
-rw-r--r--. 1 root root  54505168 Nov 15 00:07 pinpoint-web-1.5.2.war
-rw-r--r--. 1 root root      3084 Nov 15 00:07 pp-collector.init
-rw-r--r--. 1 root root      3072 Nov 15 00:07 pp-web.init
-rw-r--r--. 1 root root  17699306 Nov 15 00:07 zookeeper-3.4.6.tar.gz
```

### 2.2 配置jdk1.7

```bash
# 这套APM系统主要是用jdk1.7来进行部署的，首先要配置jdk的环境变量

cd /home/pp_res/
tar -zxvf jdk-7u79-linux-x64.tar.gz
mkdir /usr/java
mv jdk1.7.0_79/ /usr/java/jdk17

# 配置java环境变量
vi /etc/profile

# 将下列复制到profile的最后一行中
export JAVA_HOME=/usr/java/jdk17
export PATH=$PATH:$JAVA_HOME/bin

# 让环境变量生效
source /etc/profile

# 测试java的环境变量是否配置好了
[root@localhost pp_res]# java -version
java version "1.7.0_79"
Java(TM) SE Runtime Environment (build 1.7.0_79-b15)
Java HotSpot(TM) 64-Bit Server VM (build 24.79-b02, mixed mode)
```

## 3. 安装Hbase

pinpoint收集来的测试数据，主要是存在Hbase数据库的。所以它可以收集大量的数据，可以进行更加详细的分析。 

### 3.1 将Hbase解压，并且放入指定目录

```shell
cd /home/pp_res/
tar -zxvf hbase-1.0.3-bin.tar.gz
mkdir -p /data/service
mv hbase-1.0.3/ /data/service/hbase
cd /data/service/hbase/conf/
vi hbase-env.sh

# 在27行左右的位置，修改如下
export JAVA_HOME=/usr/java/jdk17/
```

### 3.2. 修改Hbase的配置信息

```shell
vi hbase-site.xml

# 在结尾修改成如下，这里我们指定Hbase本地来存储数据，生产环境将数据建议存入HDFS中。

  
    hbase.rootdir
    file:///data/hbase
```

### 3.3.启动hbase

```shell
cd /data/service/hbase/bin
./start-hbase.sh

# 查看Hbase是否启动成功，如果启动成功的会看到"HMaster"的进程
[root@localhost bin]# jps
12075 Jps
11784 HMaster
```

### 3.4.初始化Hbase的pinpoint库

```shell
# 执行pinpoint提供的Hbase初始化语句，这时会初始化一会。
./hbase shell /home/pp_res/hbase-create.hbase

# 执行完了以后，进入Hbase
./hbase shell

# 进入后可以看到Hbase的版本，还有一些相关的信息
2016-11-15 01:55:44,861 WARN  [main] util.NativeCodeLoader: Unable to load native-hadoop library for your platform... using built
in-java classes where applicableHBase Shell; enter 'help' for list of supported commands.
Type "exit" to leave the HBase Shell
Version 1.0.3, rf1e1312f9790a7c40f6a4b5a1bab2ea1dd559890, Tue Jan 19 19:26:53 PST 2016
 
hbase(main):001:0>

# 输入"status 'detailed'"可以查看刚才初始化的表，是否存在
hbase(main):001:0> status 'detailed'
version 1.0.3
0 regionsInTransition
master coprocessors: []
1 live servers
    localhost:50887 1478538574709
        requestsPerSecond=0.0, numberOfOnlineRegions=498, usedHeapMB=24, maxHeapMB=237, numberOfStores=626, numberOfStorefiles=0, storefileUncom
pressedSizeMB=0, storefileSizeMB=0, memstoreSizeMB=0, storefileIndexSizeMB=0, readRequestsCount=7714, writeRequestsCount=996, rootIndexSizeKB=0, totalStaticIndexSizeKB=0, totalStaticBloomSizeKB=0, totalCompactingKVs=0, currentCompactedKVs=0, compactionProgressPct=NaN, coprocessors=[MultiRowMutationEndpoint]        "AgentEvent,,1478539104778.aa1b3b14d0b48d83cbf4705b75cb35b7."
            numberOfStores=1, numberOfStorefiles=0, storefileUncompressedSizeMB=0, storefileSizeMB=0, memstoreSizeMB=0, storefileIndexSizeMB=0,
readRequestsCount=0, writeRequestsCount=0, rootIndexSizeKB=0, totalStaticIndexSizeKB=0, totalStaticBloomSizeKB=0, totalCompactingKVs=0, currentCompactedKVs=0, compactionProgressPct=NaN, completeSequenceId=-1, dataLocality=0.0
...
```

也可以登录web，来查看HBase的数据是否初始化成功

HbaseWeb : http://192.168.245.134:16010/master-status

![HbaseWeb](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710193654.png)



## 4.安装pinpoint-collector

## 4.1 部署war包

```shell
# 解压Tomcat，将Tomcat重命名移动到指定位置
cd /home/pp_res/
tar -zxvf apache-tomcat-8.0.36.tar.gz
mv apache-tomcat-8.0.36/ /data/service/pp-col
 

# 修改pp-col的Tomcat的配置，主要修改端口，避免与pp-web的Tomcat的端口冲突。我在原本默认的端口前都加了1，下面是替换的shell命令。
#【注意】最后一条是将tomcat的私有ip开放，需要将localhost替换成本机的ip，我本机的网卡是默认的，如果你本机的网卡不是eth0，需要进行相关的修改。或者直接用"vi"进去，修改localhost
cd /data/service/pp-col/conf/
sed -i 's/port="8005"/port="18005"/g' server.xml
sed -i 's/port="8080"/port="18080"/g' server.xml
sed -i 's/port="8443"/port="18443"/g' server.xml
sed -i 's/port="8009"/port="18009"/g' server.xml
sed -i 's/redirectPort="8443"/redirectPort="18443"/g' server.xml
sed -i "s/localhost/`ifconfig eth0 | grep 'inet addr' | awk '{print $2}' | awk -F: '{print $2}'`/g" server.xml
 

# 部署pinpoint-collector.war包
#【注意：如果没有unzip命令，可以 "yum install unzip" 】
cd /home/pp_res/
rm -rf /data/service/pp-col/webapps/*
unzip pinpoint-collector-1.5.2.war -d /data/service/pp-col/webapps/ROOT
 

# 启动Tomcat
cd /data/service/pp-col/bin/
./startup.sh
 

# 查看日志，是否成功启动
tail -f ../logs/catalina.out
```



## 4.2 配置快速启动

```shell
# 配置快速启动需要修改pp-collector.init的路径( pp-collector在网盘里面有 )，可以"vi"进去，大概在18，24，27行处，修改相关的路径。我这边为了方便，直接就用替换的shell做了，如果路径与我的不一致，需要将路径修改成自己的路径。
cd /home/pp_res
sed -i "s/JAVA_HOME=\/usr\/java\/default\//JAVA_HOME=\/usr\/java\/jdk17\//g" pp-collector.init
sed -i "s/CATALINA_HOME=\/data\/service\/pinpoint-collector\//CATALINA_HOME=\/data\/service\/pp-col\//g" pp-collector.init
sed -i "s/CATALINA_BASE=\/data\/service\/pinpoint-collector\//CATALINA_BASE=\/data\/service\/pp-col\//g" pp-collector.init
 

# 将文件赋予"执行"的权限，把它放到"init.d"中去。以后就可以restart快速重启了。
chmod 711 pp-collector.init
mv pp-collector.init /etc/init.d/pp-col
 
 
# 测试一下restart
[root@localhost pp_res]# /etc/init.d/pp-col restart
Stoping Tomcat
Using CATALINA_BASE:   /data/service/pp-col/
Using CATALINA_HOME:   /data/service/pp-col/
Using CATALINA_TMPDIR: /data/service/pp-col//temp
Using JRE_HOME:        /usr/java/jdk17/
Using CLASSPATH:       /data/service/pp-col//bin/bootstrap.jar:/data/service/pp-col//bin/tomcat-juli.jar
 
waiting for processes to exitStarting tomcat
Using CATALINA_BASE:   /data/service/pp-col/
Using CATALINA_HOME:   /data/service/pp-col/
Using CATALINA_TMPDIR: /data/service/pp-col//temp
Using JRE_HOME:        /usr/java/jdk17/
Using CLASSPATH:       /data/service/pp-col//bin/bootstrap.jar:/data/service/pp-col//bin/tomcat-juli.jar
Tomcat started.
Tomcat is running with pid: 22824
```

# 5. 安装pinpoint-web

## 5.1 部署war包

```shell
# 解压Tomcat，将Tomcat重命名移动到指定位置
cd /home/pp_res/
tar -zxvf apache-tomcat-8.0.36.tar.gz
mv apache-tomcat-8.0.36/ /data/service/pp-web
 

# 修改pp-web的Tomcat的配置，主要修改端口，避免与pp-col的Tomcat的端口冲突。我在原本默认的端口前都加了2，下面是替换的shell命令
#【注意】最后一条是将tomcat的私有ip开放，需要将localhost替换成本机的ip，我本机的网卡是默认的，如果你本机的网卡不是eth0，需要进行相关的修改。或者直接用"vi"进去，修改localhost
cd /data/service/pp-web/conf/
sed -i 's/port="8005"/port="28005"/g' server.xml
sed -i 's/port="8080"/port="28080"/g' server.xml
sed -i 's/port="8443"/port="28443"/g' server.xml
sed -i 's/port="8009"/port="28009"/g' server.xml
sed -i 's/redirectPort="8443"/redirectPort="28443"/g' server.xml
sed -i "s/localhost/`ifconfig eth0 | grep 'inet addr' | awk '{print $2}' | awk -F: '{print $2}'`/g" server.xml
 

# 部署pinpoint-collector.war包
#【注意：如果没有unzip命令，可以 "yum install unzip" 】
cd /home/pp_res/
rm -rf /data/service/pp-web/webapps/*
unzip pinpoint-web-1.5.2.war -d /data/service/pp-web/webapps/ROOT
 

# 查看war包是否解压成功
[root@localhost conf]# ll /data/service/pp-web/webapps/ROOT/WEB-INF/classes/
total 88
-rw-rw-r--. 1 root root 2164 Apr  7  2016 applicationContext-cache.xml # 这些 *.xml 文件在后续的调优工作中会用到。
-rw-rw-r--. 1 root root 3649 Apr  7  2016 applicationContext-dao-config.xml
-rw-rw-r--. 1 root root 1490 Apr  7  2016 applicationContext-datasource.xml
-rw-rw-r--. 1 root root 6680 Apr  7  2016 applicationContext-hbase.xml
-rw-rw-r--. 1 root root 1610 Apr  7  2016 applicationContext-websocket.xml
-rw-rw-r--. 1 root root 6576 Apr  7  2016 applicationContext-web.xml
drwxrwxr-x. 2 root root 4096 Apr  7  2016 batch
-rw-rw-r--. 1 root root  106 Apr  7  2016 batch.properties
drwxrwxr-x. 3 root root 4096 Apr  7  2016 com
-rw-rw-r--. 1 root root  682 Apr  7  2016 ehcache.xml
-rw-rw-r--. 1 root root 1001 Apr  7  2016 hbase.properties # 配置我们pp-web从哪个数据源获取采集数据，这里我们只指定Hbase的zookeeper地址。
-rw-rw-r--. 1 root root  153 Apr  7  2016 jdbc.properties # 连接自身Mysql数据库的连接认证配置。
-rw-rw-r--. 1 root root 3338 Apr  7  2016 log4j.xml
drwxrwxr-x. 2 root root 4096 Apr  7  2016 mapper
-rw-rw-r--. 1 root root 1420 Apr  7  2016 mybatis-config.xml
drwxrwxr-x. 3 root root 4096 Apr  7  2016 org
-rw-rw-r--. 1 root root  630 Apr  7  2016 pinpoint-web.properties # 这里pp-web集群的配置文件，如果你需要pp-web集群的话。
-rw-rw-r--. 1 root root  141 Apr  7  2016 project.properties
-rw-rw-r--. 1 root root 3872 Apr  7  2016 servlet-context.xml
drwxrwxr-x. 2 root root 4096 Apr  7  2016 sql # sql目录 pp-web本身有些数据需要存放在MySQL数据库中，这里需要初始化一下表结构。


# 启动Tomcat
cd /data/service/pp-web/bin/
./startup.sh
 

# 查看日志，Tocmat是否启动成功
tail -f ../logs/catalina.out
 

# 日志中出现下面这句话，说明已经启动成功了
org.apache.catalina.startup.Catalina.start Server startup in 79531 ms


# 这时候我们可以访问一下这个地址，在浏览器中输入"http://192.168.245.136:28080"，就会出现主页面了
# 如果访问不了的话，关闭防火墙
[root@localhost conf]# /etc/init.d/iptables stop
iptables: Setting chains to policy ACCEPT: filter          [  OK  ]
iptables: Flushing firewall rules:                         [  OK  ]
iptables: Unloading modules:                               [  OK  ]
```

![pp-web](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200245.png)

### 5.2 配置快速启动

```shell
# 需要修改"pp-web.init"，与上面的步骤一致
cd /home/pp_res
sed -i "s/JAVA_HOME=\/usr\/java\/default\//JAVA_HOME=\/usr\/java\/jdk17\//g" pp-web.init
sed -i "s/CATALINA_HOME=\/data\/service\/pinpoint-web\//CATALINA_HOME=\/data\/service\/pp-web\//g" pp-web.init
sed -i "s/CATALINA_BASE=\/data\/service\/pinpoint-web\//CATALINA_BASE=\/data\/service\/pp-web\//g" pp-web.init
 

# 将文件赋予"执行"的权限，把让放到"init.d"中去。以后就可以restart快速重启了。
chmod 711 pp-web.init
mv pp-web.init /etc/init.d/pp-web
 
 
# 测试一下restart
[root@localhost pp_res]# /etc/init.d/pp-web restart
Stoping Tomcat
Using CATALINA_BASE:   /data/service/pp-web/
Using CATALINA_HOME:   /data/service/pp-web/
Using CATALINA_TMPDIR: /data/service/pp-web//temp
Using JRE_HOME:        /usr/java/jdk17/
Using CLASSPATH:       /data/service/pp-web//bin/bootstrap.jar:/data/service/pp-web//bin/tomcat-juli.jar
 
waiting for processes to exitStarting tomcat
Using CATALINA_BASE:   /data/service/pp-web/
Using CATALINA_HOME:   /data/service/pp-web/
Using CATALINA_TMPDIR: /data/service/pp-web//temp
Using JRE_HOME:        /usr/java/jdk17/
Using CLASSPATH:       /data/service/pp-web//bin/bootstrap.jar:/data/service/pp-web//bin/tomcat-juli.jar
Tomcat started.
Tomcat is running with pid: 22703
```



## 6. 部署pp-agent采集监控数据

### 6.1 在测试系统中，部署pp-agent采集监控数据

部署采集器就很简单了，只需要加3句话就好了。我这边做一个测试的Tomcat，来模拟部署。

首先，先建立一个文件夹，放测试需要的包

```shell
mkdir /home/pp_test
cd /home/test
```

将测试需要的pp-agent拉到服务器上

![pp-test](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200429.png)

查看包是否上传成功

```shell
[root@localhost pp_test]# ll
total 16820
-rw-r--r--. 1 root root 9277365 Nov  9 02:25 apache-tomcat-8.0.36.tar.gz
-rw-r--r--. 1 root root 6621915 Nov  9 02:25 pinpoint-agent-1.5.2.tar.gz
-rw-r--r--. 1 root root 1320206 Nov  9 02:25 test.war
```

## 6.2 配置模拟的Tomcat测试环境

```shell
# 为了方便观察，配置一个假的系统，解压Tomcat到指定目录
cd /home/pp_test
mkdir /data
tar -zxvf apache-tomcat-8.0.36.tar.gz
 

# 配置localhost让外部可以访问
cd /data/pp-test/conf/
sed -i "s/localhost/`ifconfig eth0 | grep 'inet addr' | awk '{print $2}' | awk -F: '{print $2}'`/g" server.xml
 

# 解压测试用的war包
cd /home/pp_test/
rm -rf /data/pp-test/webapps/*
unzip test.war -d /data/pp-test/webapps/ROOT
```



## 6.3 配置pp-agent采集器

```shell
# 解压pp-agent
cd /home/pp_test
tar -zxvf pinpoint-agent-1.5.2.tar.gz
mv pinpoint-agent-1.5.2 /data/pp-agent
 

# 编辑配置文件
cd /data/pp-agent/
vi pinpoint.config
 

# 主要修改IP，只需要指定到安装pp-col的IP就行了，安装pp-col启动后，自动就开启了9994，9995，9996的端口了。这里就不需要操心了，如果有端口需求，要去pp-col的配置文件("pp-col/webapps/ROOT/WEB-INF/classes/pinpoint-collector.properties")中，修改这些端口
profiler.collector.ip=192.168.245.136
 

# 修改测试项目下的tomcat启动文件"catalina.sh"，修改这个只要是为了监控测试环境的Tomcat，增加探针
cd /data/pp-test/bin
vi catalina.sh
 

# 在20行增加如下字段
# 第一行是pp-agent的jar包位置
# 第二行是agent的ID，这个ID是唯一的，我是用pp + 今天的日期命名的，只要与其他的项目的ID不重复就好了
# 第三行是采集项目的名字，这个名字可以随便取，只要各个项目不重复就好了
CATALINA_OPTS="$CATALINA_OPTS -javaagent:/data/pp-agent/pinpoint-bootstrap-1.5.2.jar"
CATALINA_OPTS="$CATALINA_OPTS -Dpinpoint.agentId=pp20161122"
CATALINA_OPTS="$CATALINA_OPTS -Dpinpoint.applicationName=MyTestPP
```



## 6.4 监控Tomcat

```shell
# 配置好了。就可以开始监控了，我们启动测试用的Tomcat的服务器
cd /data/pp-test/bin/
./startup.sh
 

# 查看启动日志，确实Tomcat启动
tail -f ../logs/catalina.out
```

启动了，我们就可以访问测试环境了

![test](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200457.png)

![test1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200527.png)

这时候我们在访问pp-web，可以发现它的下拉框中，多了一个app

![pp-testApp](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200551.png)

![pp-testView](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200600.png)

因为我访问了两次，所以他显示有两条请求记录，可以在右上角的框查看详情。

【注意】鼠标点击右上角箭头位置，鼠标左键按住不动，拉框查看。我被这个坑，坑懵逼了，特此写清楚。

![pp-detail](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200618.png)

这时候就弹出了新页面，可以看到，我访问了一次主页，访问了一次test的servlet。而且详细信息都记录在下表中。

![pp-code](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710200630.png)

## 7.总结

到这里，整个部署过程就完了。值得要注意的地方：

1. 如果Hbase不是与pp-web, pp-col装在一台机器上，需要安装zookeeper，只要安装就好，确实2181端口启动就好。
2. 如果zookeeper安装在独立机器上，这里需要修改一下pp-colletor 和 pp-web的配置文件pinpoint-collector.properties，pinpoint-web.properties，不然会导致俩个模块启动失败。
3. 发现pinpoint还是有些缺陷，异步的操作监控不到，比如我写了个多线程来发送HttpClient4的请求，但是pinpoint监控不到。但是它介绍又说可以监控到Httpclient4的请求。现在都是分布式系统，异步拿数据再常见不过来，如果监控不到异步的操作，就很鸡肋了。看pp1.6会不会修复这个问题
4. 在pp1.6部署，Hbase中的默认字段有增加，如果没有加上默认字段，取得的数据就会变得相当少了。

 















