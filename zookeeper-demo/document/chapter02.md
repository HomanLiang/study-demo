[toc]



# Zookeeper环境搭建

Zookeeper搭建均以Linux环境为例

## 单机环境搭建

1. **下载安装包**

   首先下载稳定版本的zookeeper http://zookeeper.apache.org/releases.html,如下使用的版本为3.4.14》》zookeeper-3.4.14.tar.gz

2. **上传**

   将zookeeper安装压缩包zookeeper-3.4.14.tar.gz上传至服务器系统

3. 解压缩安装包至/usr/local

   ```
   cd /usr/local
   tar -zxvf zookeeper-3.4.14.tar.gz
   ```

4. 创建data文件夹

   ```
   cd zookeeper-3.4.14
   mkdir data
   ```

5. 修改配置文件名称

   ```
   cd conf
   mv zoo_sample.cfg zoo.cfg
   ```

6. 修改zoo.cfg中的data属性

   ```
   dataDir=/usr/local/zookeeper-3.4.14/data
   ```

7. 启动服务

   ```
   ./zkServer.sh start
   ```

8. 查看

   输出以下内容表示启动成功

   ![image-20210313132212772](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313132212.png)

   关闭服务输入命令

   ```
   ./zkServer.sh stop
   ```

   输出以下提是信息表示关闭成功


   ![image-20210313132317335](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313132317.png)

   查看状态输入命令

   ```
   ./zkServer.sh status
   ```



## 集群模式搭建

Zookeeper不但可以在单机上运行单机模式，而且可以在单机模拟集群模式。以下为单机情况下模拟集群模式的搭建。我们将不同的实例运行在一台机器，用端口区分。

我们在一台机器上部署了3个server，也就是说单台机器上运行多个Zookeeper实例，这种情况下我们要保证每个不同的zookeeper实例的端口号是不能冲突的，除了clientPort不同之外，dataDir也不同。另外，还要再dataDir所对应的目录中创建myid文件再指定对应的zookeeper服务器实例

- **clientPort端口**

  如果在1台机器上部署多个zookeeper实例，那么每个实例需要不同的clientPort

- **dataDir和dataLogDir**

  如果在1台机器上部署多个zookeeper实例，dataDir和dataLogDir也需要区分

- **server.X和myid**

  server.X这个数字就是对应data/myid中的数字。在3个server的myid文件中分别写入了1,2,3，那么每个server中的zoo.cfg都配置sever.1,server.2,server.3。在同一台机器上部署的情况下，后面连着的2个端接口都不能是一样的，否则会端口冲突



**安装步骤：**

1. 下载安装包

   首先下载稳定版本的zookeeper http://zookeeper.apache.org/releases.html,如下使用的版本为3.4.14》》zookeeper-3.4.14.tar.gz

2. 上传

   将zookeeper安装压缩包zookeeper-3.4.14.tar.gz上传至服务器系统

3. 解压缩安装包至/usr/local中创建的新目录zkcluster中

   ```
   cd /usr/local
   mkdir zkcluster
   tar -zxvf zookeeper-3.4.14.tar.gz -C /zkcluster
   ```

4. 改变名称

   ```
   mv zookeeper-3.4.14 zookeeper01
   ```

5. 复制多分模拟多个实例并改名

   ```
   cp -r zookeeper01/ zookeeper02
   cp -r zookeeper01/ zookeeper03
   ```

6. 分别在这三个实例文件夹中创建data及logs目录

   ```
   cd /usr/local/zkcluster/zookeeper01
   mkdir data
   cd data
   mkdir logs
   
   cd /usr/local/zkcluster/zookeeper02
   mkdir data
   cd data
   mkdir logs
   
   cd /usr/local/zkcluster/zookeeper03
   mkdir data
   cd data
   mkdir logs
   ```

7. 修改配置文件名称（三个实例需要都修改）

   ```
   cd /usr/local/zkcluster/zookeeper01/conf
   mv zoo_sample.cfg zoo.cfg
   ```

8. 配置每一个Zookeeper的dataDir

   ```
   clientPort=2181
   dataDir=/usr/local/zkcluster/zookeeper01/data
   dataLogDir=/usr/local/zkcluster/zookeeper01/data/logs
   
   clientPort=2182
   dataDir=/usr/local/zkcluster/zookeeper02/data
   dataLogDir=/usr/local/zkcluster/zookeeper02/data/logs
   
   clientPort=2183
   dataDir=/usr/local/zkcluster/zookeeper03/data
   dataLogDir=/usr/local/zkcluster/zookeeper03/data/logs
   ```

9. 配置集群

   在每个Zookeeper的data目录下创建一个myid文件，内容分别是1,2,3。这个文件就是记录每个服务器的ID

   ```
   touch myid
   ```

   在每个zookeeper的zoo.cfg配置客户端访问端口和集群服务器IP列表

   ```
   server.1=服务器IP:2881:3881
   server.2=服务器IP:2882:3882
   server.3=服务器IP:2883:3883
   #server.服务器ID=服务器IP地址：服务器之间通信端口：服务器之间头皮选举端口
   ```

10. 启动集群
    依次启动三个zookeeper实例

    

































