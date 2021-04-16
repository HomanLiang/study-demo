[toc]



# Docker 应用

## 1.Docker redis5.0 集群（cluster）搭建
### 1.1.在Docker库获取镜像
```
docker pull redis
// 5.0以下需要ruby环境
docker pull ruby
```
查看已经安装的镜像
```
docker images
```
### 1.2.创建redis容器
我的是多台服务器，在另外那台同样执行以下操作，单台的话下面出现的9001-9003端口改成9001-9006
1. **创建redis配置文件（redis-cluster.tmpl）**

	在系统根目录/mnt目录下创建文件夹redis-cluster，并创建一个文件redis-cluster.tmpl，然后把以下内容复制进去。

    ```
    port ${PORT}
    protected-mode no
    cluster-enabled yes
    cluster-config-file nodes.conf
    cluster-node-timeout 15000
    cluster-announce-ip XX.XXX.XX.XX //自己服务器内网IP
    cluster-announce-port ${PORT}
    cluster-announce-bus-port 1${PORT}
    appendonly yes
    ```
   
2. **创建自定义network**

    ```
    docker network create redis-net
    ```

3. **进入/mnt/redis-cluster目录，生成conf、data文件夹和配置信息**

    ```
    for port in `seq 9001 9003`; do \
      mkdir -p ./${port}/conf \
      && PORT=${port} envsubst < ./redis-cluster.tmpl > ./${port}/conf/redis.conf \
      && mkdir -p ./${port}/data; \
    done
    ```

	共生成3个文件夹，从9001到9003，每个文件夹下包含data和conf文件夹，同时conf里面有redis.conf配置文件
	
4. **创建redis容器**

    ```
    for port in `seq 9001 9003`; do \
      docker run -d -ti -p ${port}:${port} -p 1${port}:1${port} \
      -v /mnt/redis-cluster/${port}/conf/redis.conf:/usr/local/etc/redis/redis.conf:rw \
      -v /mnt/redis-cluster/${port}/data:/data:rw \
      --restart always --name redis-${port} --net redis-net \
      --sysctl net.core.somaxconn=1024 redis redis-server /usr/local/etc/redis/redis.conf; \
    done
    ```

	至此，容器生成成功，通过命令docker ps可查看刚刚生成容器信息，两台服务器总共6个容器。

5. **启动集群**

    ```
    // 5.0.2(最新版好像不能直接exec -it,找了半天，最后在官网找到下面命令进行的链接。真的服。。。。)
    docker run -it --link redis-9001:redis --net redis-net --rm redis redis-cli --cluster create XX.XXX.XX.XX:9001 XX.XXX.XX.XX:9002 XX.XXX.XX.XX:9003 XX.XXX.XX.XX:9004 XX.XXX.XX.XX:9005 XX.XXX.XX.XX:9006 --cluster-replicas 1// 5.0
    docker exec -it redis容器ID redis-cli --cluster create XX.XXX.XX.XX:9001 XX.XXX.XX.XX:9002 XX.XXX.XX.XX:9003 XX.XXX.XX.XX:9004 XX.XXX.XX.XX:9005 XX.XXX.XX.XX:9006 --cluster-replicas 1// 5.0以下echo yes | docker run -i --rm --net redis-net ruby sh -c '\
      gem install redis \
      && wget http://download.redis.io/redis-stable/src/redis-trib.rb \
      && ruby redis-trib.rb create --replicas 1 XX.XXX.XX.XX:9001 XX.XXX.XX.XX:9002 XX.XXX.XX.XX:9003 XX.XXX.XX.XX:9001 XX.XXX.XX.XX:9002 XX.XXX.XX.XX:9003';
    ```

	至此，集群搭建成功。
	
	如果一直 `Waiting for the cluster to join ......`
	
	大部分是端口问题，除了开放集群的端口还要开放集群端口+10000端口来保证集群之间的通信
	
### 1.3.集群密码
```
cd /mnt/redis-cluster;
chmod 777 9001/conf/redis.conf; //配置文件授权...
chmod 777 9003/conf/redis.conf; //两台配置文件都需要授权

//链接redis
docker ps -a //查看容器ID

//5.0.2
docker run -it --link redis-9001:redis --net redis-net --rm redis redis-cli -h XX.XXX.XX.XX -c -p 9001//5.0（这里设置的是单台redis的密码，所以需要每台redis都要进入执行以下操作设置密码）
docker exec -it redis容器ID redis-cli -h XX.XXX.XX.XX -c -p 9001

//设置密码
config set masterauth 123456
config set requirepass 123456
auth 123456
config rewrite
```

## 2.Docker构建SpringBoot应用
### 2.1.准备工作
将SpringBoot项目通过maven打成jar包

mvn clean package #使用maven打包项目
### 2.2.使用Dockerfile构建镜像
**step1 在存放jar所在目录下创建Dockerfile文件**

```
touch Dockerfile
```
**step2 编辑Dockerfile增加以下内容**

```
FROM java:8
MAINTAINER  niugang<863263957@qq.com>
RUN mkdir -p /opt/springapp
ADD   demo-0.0.1.jar  /opt/springapp
EXPOSE 8088
ENTRYPOINT ["java","-jar","/opt/springapp/demo-0.0.1.jar"]
```
**step3 构建镜像**

```
docker build -t springbootdemo:1.0 .
```
**step4 启动容器**

```
docker run -d -p 8088:8088 --name sb springbootdemo:1.0
```
在启动容器是可以添加数据卷，程序日志映射到宿主机，方便后期排查问题

注意：在启动过程中，一直起不起来，然后通过前台启动查看了日志。

报了如下错误：
> WARNING: IPv4 forwarding is disabled. Networking will not work.

解决：

```
#需要做如下配置
vim/etc/sysctl.conf
net.ipv4.ip_forward=1 #添加这段代码

#重启network服务
systemctl restart network && systemctl restart docker

#查看是否修改成功 （备注：返回1，就是成功）
[root@docker-node2 ~]# sysctl net.ipv4.ip_forward
net.ipv4.ip_forward = 1
```
**step5 调用你的springboot应用，验证其是否正确**

## 3.docker中save与load的使用及注意事项
对于没有私有仓库来说，将本地镜像放到其它服务器上执行时，可以使用save和load方法，前者用来把镜像保存一个tar文件，后台从一个tar文件恢复成一个镜像，这个功能对于开发者来说还是很方便的！下面就带大家来实现上面的过程。
### 3.1.docker images  查看一下本地镜像
```
[root@Dimage ~]# docker images
REPOSITORY                                                    TAG                 IMAGE ID            CREATED             SIZE
sonarqube                                                     7.1                 7a39fc50869a        8 months ago        803MB
gitlab/gitlab-ce                                              latest              80305d568e28        9 months ago        1.47GB
postgres                                                      10.4                978b82dc00dc        11 months ago       236MB
```
### 3.2.这里以sonarqube、postgres两个镜像为例
```
[root@Dimage Templates]# docker save sonarqube -o /root/Templates/sonarqube.tar
[root@Dimage Templates]# docker save postgres -o /root/Templates/postgres.tar
```
将上面的sonarqube、postgres两个镜像保存成一个tar文件，注意如果目录没有，需要提前建立一下，docker不会帮你建立目录的。

使用xtfp、FileZilla等工具把文件下载，复制到对应的服务器上

在外测服务器上，去load你的tar文件，把这恢复到docker列表里
```
[root@jenkins ~]# docker load < /home/sonarqube.tar
f715ed19c28b: Loading layer [==================================================>]  105.5MB/105.5MB
8bb25f9cdc41: Loading layer [==================================================>]  23.99MB/23.99MB
08a01612ffca: Loading layer [==================================================>]  7.994MB/7.994MB
1191b3f5862a: Loading layer [==================================================>]  146.4MB/146.4MB
097524d80f54: Loading layer [==================================================>]  2.332MB/2.332MB
685f72a7cd4f: Loading layer [==================================================>]  3.584kB/3.584kB
9c147c576d67: Loading layer [==================================================>]  1.536kB/1.536kB
4fbf445e0074: Loading layer [==================================================>]  356.3MB/356.3MB
f8d2b3161911: Loading layer [==================================================>]  362.5kB/362.5kB
23125fec8240: Loading layer [==================================================>]  338.4kB/338.4kB
1e09c232b1a9: Loading layer [==================================================>]  1.292MB/1.292MB
8fb1d730c37c: Loading layer [==================================================>]  177.8MB/177.8MB
195b3d541b37: Loading layer [==================================================>]  3.584kB/3.584kB
Loaded image: sonarqube:7.1
[root@180348-jenkins ~]# docker load < /home/postgres.tar
cdb3f9544e4c: Loading layer [==================================================>]  58.44MB/58.44MB
add4404d0b51: Loading layer [==================================================>]  10.43MB/10.43MB
0fae9a7d0574: Loading layer [==================================================>]  338.4kB/338.4kB
df9515382700: Loading layer [==================================================>]  3.059MB/3.059MB
998e6abcfae7: Loading layer [==================================================>]   17.1MB/17.1MB
c6fcee3b341c: Loading layer [==================================================>]  1.102MB/1.102MB
7c050956ab95: Loading layer [==================================================>]  1.536kB/1.536kB
ed4da41a79a9: Loading layer [==================================================>]  8.192kB/8.192kB
dd2083da8cd1: Loading layer [==================================================>]    154MB/154MB
fa8311b04439: Loading layer [==================================================>]  27.14kB/27.14kB
82360595589e: Loading layer [==================================================>]  2.048kB/2.048kB
20cbebd1cd5c: Loading layer [==================================================>]  3.072kB/3.072kB
b607040b9b5b: Loading layer [==================================================>]  8.704kB/8.704kB
Loaded image: postgres:10.4
```
然后使用docker images就可以看到自己加载的新的镜像了
```
[root@jenkins ~]# docker ps
CONTAINER ID        IMAGE                                                   COMMAND                  CREATED             STATUS              PORTS                    NAMES
b4cf326fbf10        sonarqube:7.1                                           "./bin/run.sh"           4 seconds ago       Up 3 seconds        0.0.0.0:9000->9000/tcp   sonarqube
a9ccfaf9a91e        postgres:10.4                                           "docker-entrypoint.s…"   16 minutes ago      Up 16 minutes       0.0.0.0:5432->5432/tcp   postgresql
```
（注意：docker save postgres -o /root/Templates/sonarqube.tar，这里最好是用镜像名，不要使用镜像ID，不然load出来的镜像显示如下REPOSITORY、TAG均显示none,不便于后面进一步操作）
```
[root@jenkins ~]# docker images
REPOSITORY                                      TAG                 IMAGE ID            CREATED             SIZE
sonarqube                                       7.1                 7a39fc50869a        8 months ago        803MB
<none>                                          <none>              978b82dc00dc        11 months ago       236MB
```

## 4.如何提交对 Docker 镜像的更改
我将使用官方NGINX图像演示该过程，并将自定义来自Docker Hub的镜像。我假设您已经在您选择的平台上启动并运行Docker，现在准备开始。
### 4.1.拉取官方镜像
这个过程的第一步是从Docker Hub下载官方镜像。要拉取此镜像，执行以下命令：
```
docker pull nginx
```
图像应该很快拉下来（图A）

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413231226.png)

<center>图A：从Docker Hub拉取官方NGINX图像</center>

下载镜像后，我们现在可以部署容器，以便可以自定义容器以满足我们的特定需求。

### 4.2.部署容器
我们要做的是以这样一种方式部署我们的新容器，即我们可以访问关联的bash提示符（这样我们就可以在容器内工作）。为此，我们使用以下命令进行部署：
```
docker run --name nginx-template -p8080:80-e TERM=xterm -d nginx
```
上面的命令分解如下：
- docker run 指示Docker 我们要运行一个新容器。
- -name nginx-template 指示Docker命名新容器nginx-template
- -p8080:80指示Docker将内部容器端口80暴露给网络端口8080。
- -e TERM = xterm 定义我们的终端变量。
- -d 在后台启动容器。
- nginx 是要用于容器的图像的名称。

### 4.3.访问修改容器
我们的下一步是访问容器。当您执行上述命令时，Docker将返回容器的ID（图B）。此ID是您用于访问容器的ID。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413231258.png)

<center>图B：我们新部署的容器ID</center>

我们需要使用的是容器ID的前四位数字。所以在我们的例子中，我们使用b1d5。
```
docker  exec  -it  b1d5  bash
```
注意：运行该命令时，您将获得完全不同的ID。

此时，您将发现自己处于NGINX容器的bash提示符下（ 图C ）。

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413231329.png)

<center>图C：我们新部署的容器的bash提示符</center>

### 4.4.安装工具
下一步是安装必要的工具。请记住，我们将安装build-essential，PHP，MySQL和nano。在尝试安装任何内容之前，首先需要使用命令更新apt：
```
apt-get update
```
完成该命令后，使用以下命令安装必要的软件：
```
apt-get  install  nano
apt-get  install  build-essential
apt-get  install  php  php-mysql
apt-get  install  mysql-server
```
完成上述命令后，使用exit命令退出NGINX bash提示符。

### 4.5.提交改变
现在该基于我们的添加提交您的更改去创建新图像。为此，我们需要再次使用容器ID（在我们的示例中前四个字符，b1d5）。当我们提交这些更改时，我们会有效地创建一个新镜像，其中包含对原始镜像的所有添加。执行此操作的命令是：
```
docker commit b1d5 nginx-template
```
此命令将在30秒内完成。完成后，执行命令docker images，可以看到新创建的NGINX镜像，其中包含MySQL，PHP，build-essential和nano（图D ）。

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413231356.png)

<center>图D：列出了我们新创建的图像</center>

恭喜，您现在拥有一个基于容器的自定义镜像。您可以使用以下命令使用此镜像部署新容器：
```
docker run --name nginx-dev -p8080:80-e TERM=xterm -d nginx-template
```
部署容器后，访问它的bash提示符（与之前相同的方式），您将看到所有添加软件都在那里，随时可以使用。

### 4.6.简单的容器模板
这只是简化容器部署的一条途径。如果您倾向于推出容器，并且发现自己不得不经常向镜像添加相同的基础软件，那么您可能需要考虑使用此方法，使该过程显着提高效率。





















