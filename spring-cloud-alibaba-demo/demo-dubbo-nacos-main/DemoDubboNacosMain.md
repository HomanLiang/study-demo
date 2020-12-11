# Dubbo RPC框架集成Nacos注册中心

注：[示例代码](https://github.com/HomanLiang/study-demo/tree/main/spring-cloud-alibaba-demo/demo-dubbo-nacos-main)




## 基于Nacos实现高可用服务注册中心部署

### 安装虚拟机

​	自行百度，下载，安装，破解

### 安装CentOS 7.x操作系统

​	参考下面步骤安装三台虚拟机

​	请参考：[Centos7.6 下载与安装](https://zhuanlan.zhihu.com/p/77198314)

### CentOS 集群配置

1. 首先进行网络配置，让主机能直接访问虚拟机

   注：网络配置请参考：[  如何使用Xshell连接VMware上的Linux虚拟机   ]( https://www.cnblogs.com/shireenlee4testing/p/9469650.html )

2. 配置机器Host

   ```
   vi /etc/hosts
   ```

   ```
   192.168.xxx.xxx test01
   192.168.xxx.xxx test02
   192.168.xxx.xxx test03
   ```

3. 配置多台CentOS为ssh免密码互相通信

   注：参考：[  配置多台CentOS为ssh免密码互相通信 ](  https://www.cnblogs.com/hbbbs/articles/8175897.html )

### Nacos集群搭建

1. 下载nacos-server-1.4版本的源码

   ```
   git clone https://github.com/alibaba/nacos.git
   ```

   

2. 编译源码

   ```
   cd nacos/
   mvn -Prelease-nacos -Dmaven.test.skip=true clean install -U
   ```

   

3. 上传压缩包文件到三台服务器虚拟机

   ```
   ls -al distribution/target/
   ```

   nacos-server-1.4.0-SNAPSHOT.tar.gz

4. 新建数据库，并执行下面脚本`\nacos\conf\nacos-mysql.sql`

5. 解压缩文件后，分别修改配置文件`application.properties`和`cluster.conf.example`

   application.properties

   ```
   spring.datasource.platform=mysql
   db.num=1
   db.url.0=xxx
   db.user=xx
   db.password=xxx
   ```

   cluster.conf.example-->cluster.conf

   ```
   #2020-10-28T15:17:44.296
   192.168.241.4:8848
   192.168.241.5:8848
   192.168.241.6:8848
   ```

6. `/bin/startup.sh`,启动Nacos.

7. 登录控制台:`http://192.168.241.5:8848/nacos/index.html`

   nacos/nacos

## Dubbo RPC框架集成Nacos注册中心示例
1. 下载本项目代码
```
https://github.com/HMLIANG/demo-dubbo-nacos.git
```

2. 项目结构：
```
demo-dubbo-nacos-api：服务接口
demo-dubbo-nacos-provider：服务提供者
demo-dubbo-nacos-consumer：服务消费之
```

3. 修改配置文件

demo-dubbo-nacos-provider：application.properties 文件
```
spring.application.name=demo-dubbo-nacos-provider
# 订阅的服务提供者
dubbo.scan.base-packages=com.zhss.demo.dubbo.nacos
dubbo.protocol.name=dubbo
dubbo.protocol.port=20880
dubbo.registry.address=spring-cloud://localhost
# nacos注册中心地址
spring.cloud.nacos.discovery.server-addr=192.168.241.4:8848,192.168.241.5:8848,192.168.241.6:8848,192.168.241.7:8848
```
demo-dubbo-nacos-consumer：application.properties 文件
```
spring.application.name=demo-dubbo-nacos-consumer
# 订阅的服务提供者
dubbo.cloud.subscribed-services=demo-dubbo-nacos-provider
# 待扫描的包
dubbo.scan.base-packages=com.zhss.demo.dubbo.nacos
# nacos注册中心地址
spring.cloud.nacos.discovery.server-addr=192.168.241.4:8848,192.168.241.5:8848,192.168.241.6:8848,192.168.241.7:8848
```

4. 启动项目

先启动`demo-dubbo-nacos-provider`，然后启动`demo-dubbo-nacos-consumer`，然后调用`http://localhost:8080/greet?name=homan`测试

5. 查看Nacos控制台的结果

![](https://raw.githubusercontent.com/HomanLiang/pictures/main/study-demo/demo-dubbo-nacos-main/nacos_provider.png)