[toc]



# Docker Swarm

## 1.前言
Docker Swarm 和 Docker Compose 一样，都是 Docker 官方容器编排项目，但不同的是，Docker Compose 是一个在单个服务器或主机上创建多个容器的工具，而 Docker Swarm 则可以在多个服务器或主机上创建容器集群服务，对于微服务的部署，显然 Docker Swarm 会更加适合。

从 Docker 1.12.0 版本开始，Docker Swarm 已经包含在 Docker 引擎中（docker swarm），并且已经内置了服务发现工具，我们就不需要像之前一样，再配置 Etcd 或者 Consul 来进行服务发现配置了。

Docker Swarm集群中有三个角色：manager（管理者）；worker（实际工作者）以及service（服务）。

在上面的三个角色中，其本质上与我们公司的组织架构类似，有领导（manager），有搬砖的（worker），而领导下发给搬砖者的任务，就是Docker Swarm中的service（服务）。

需要注意的是，在一个Docker Swarm群集中，每台docker服务器的角色可以都是manager，但是，不可以都是worker，也就是说，不可以群龙无首，并且，参与群集的所有主机名，千万不可以冲突。

> Swarm 是 Docker 公司在2014年12月初发布的一套用来管理 Docker 集群的较为简单的工具，由于 Swarm 使用标准的Docker API接口作为其前端访问入口，所以各种形式的Docker Client(dockerclient in go, docker_py, docker等)都可以直接与Swarm通信。老的 Docker Swarm 使用独立的外部KV存储（比如Consul、etcd、zookeeper），搭建独立运行的Docker主机集群，用户像操作单台Docker 机器一样操作整个集群，Docker Swarm 把多台 Docker 主机当做一台 Docker 主机来管理。新的 Swarm mode 是在docker 1.12版本中集成到 Docker 引擎中的，引入服务的概念，提供了众多的新特性，比如：具有容错能力的去中心化设计、内置服务发现、负载均衡、路由网格、动态伸缩、滚动更新、安全传输等。使得 Docker 原生的 Swarm mode 集群具备与 Mesos、Kubernetes 叫板的实力。

这里通过一个案例来展示Docker Swarm集群的配置。

## 2.环境准备
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413220235.png)

在上述主机中，将指定主机docker01为manager的角色，其他主机的角色为worker。


## 3.配置主机docker01
以下操作，将初始化一个Swarm群集，并指定docker01的角色为manager。
```
#由于需要在三台主机间复制一些配置文件，所以在docker01上配置免密登录
[root@docker01 ~]# ssh-keygen     #生成密钥对，一路按回车即可生成
[root@docker01 ~]# tail -3 /etc/hosts   #配置/etc/hosts文件
#三台主机之间要互相解析（Swarm群集也需要此配置）
192.168.20.6 docker01
192.168.20.7 docker02
192.168.20.8 docker03
[root@docker01 ~]# ssh-copy-id docker02   #将生成的秘钥发送到docker02
root@docker02 s password:        #要输入docker02的root密码
[root@docker01 ~]# ssh-copy-id docker03   #将秘钥发送到docker03，同样需要输入docker03的root密码
[root@docker01 ~]# scp /etc/hosts docker02:/etc/   #将hosts文件发送到docker02
[root@docker01 ~]# scp /etc/hosts docker03:/etc/   #将hosts文件发送到docker03
[root@docker01 ~]# docker swarm init --advertise-addr 192.168.20.6   #初始化一个集群，并指定自己为manager
```
当执行上述操作，指定自己为manager初始化一个群组后，则会随着命令的执行成功而返回一系列的提示信息，这些提示信息给出的是，如果其他节点需要加入此节点，需要执行的命令，直接对其进行复制，然后，在需要加入此群集的主机上执行，即可成功加入群集。

返回的提示信息如下：

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221403.png)

在上述中，既然给出了相应的命令，那么，现在开始配置需要加入群集的docker服务器。


## 4.配置docker02及docker03加入Swarm群集
```
#docker02执行以下命令：
[root@docker02 ~]# docker swarm join --token SWMTKN-1-5ofgk6fh1vey2k7qwsk4gb9yohkxja6hy8les7plecgih1xiw1-3vpemis38suwyxg3efryv5nyu 192.168.20.6:2377
#docker03也执行以下命令
[root@docker03 ~]# docker swarm join --token SWMTKN-1-5ofgk6fh1vey2k7qwsk4gb9yohkxja6hy8les7plecgih1xiw1-3vpemis38suwyxg3efryv5nyu 192.168.20.6:2377
[root@docker01 ~]# docker node promote docker02    #将docker02从worker升级为manager。
```
至此，docker02及03便以worker的角色加入到了群集当中。

若docker02或者docker03要脱离这个群集，那么需要以下配置（这里以docker03为例）：
```
#将docker03脱离这个群集
[root@docker03 ~]# docker swarm leave        #在docker03上执行此命令
[root@docker01 ~]# docker node rm docker03         #然后在manager角色的服务器上移除docker03
[root@docker01 ~]# docker swarm leave -f  #若是最后一个manager上进行删除群集，则需要加“-f”选项
#最后一个删除后，这个群集也就不存在了
```

## 5.搭建registry私有仓库
在docker Swarm群集中，私有仓库并不影响其群集的正常运行，只是公司的生产环境多数都是自己的私有仓库，所以这里模拟一下。
```
[root@docker01 ~]# docker run -d --name registry --restart always -p 5000:5000 registry  #运行一个registry仓库容器
[root@docker01 ~]# vim /usr/lib/systemd/system/docker.service    #修改docker配置文件，以便指定私有仓库
ExecStart=/usr/bin/dockerd -H unix:// --insecure-registry 192.168.20.6:5000    #定位到改行，指定私有仓库IP及端口
#编辑完成后，保存退出即可
[root@docker01 ~]# systemctl daemon-reload       #重新加载配置文件
[root@docker01 ~]# systemctl restart docker         #重启docker服务
#docker02及docker03也需要指定私有仓库的位置，所以执行下面的命令将更改后的docker配置文件复制过去
[root@docker01 ~]# scp /usr/lib/systemd/system/docker.service docker02:/usr/lib/systemd/system/         
[root@docker01 ~]# scp /usr/lib/systemd/system/docker.service docker03:/usr/lib/systemd/system/
#将docker的配置文件复制过去以后，需要重启docker02及03的docker服务
#下面的命令需要在docker02及03的服务器上分别运行一次：
[root@docker02 ~]# systemctl daemon-reload
[root@docker02 ~]# systemctl restart docker
```
在私有仓库完成后，最好测试一下是否可以正常使用，如下：
```
#docker01将httpd镜像上传到私有仓库
[root@docker01 ~]# docker tag httpd:latest 192.168.20.6:5000/lvjianzhao:latest
[root@docker01 ~]# docker push 192.168.20.6:5000/lvjianzhao:latest 
#在dokcer02上进行下载，测试是否可以正常下载
[root@docker02 ~]# docker pull 192.168.20.6:5000/lvjianzhao:latest
#可以正常下载，说明私有仓库可用
```
在上面搭建私有仓库的过程，并没有实现数据的持久化，若需要基于数据持久化搭建私有仓库，可以参考博文：[Docker之Registry私有仓库+Harbor私有仓库的搭建](https://blog.51cto.com/14154700/2443431)。


## 6.docker01部署docker Swarm群集的web UI界面
```
[root@docker01 ~]# docker run -d -p 8000:8080 -e HOST=172.16.20.6 -e PORT=8080 -v /var/run/docker.sock:/var/run/docker.sock  --name visualizer  dockersamples/visualizer
#执行上述命令后，即可客户端访问其8000访问，可以看到群集内的节点信息
#若节点发生故障，则会立即检测到
```
访问docker01的8000端口，即可看到以下界面（该界面只能看，不能进行任何配置）：

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221423.png)

配置至此，docker Swarm的群集基本完善了，接下来，开始展示该群集，究竟可以做些什么？也就是到了配置其service服务阶段。

## 6.docker Swarm群集的service服务配置
在docker01（必须在manager角色的主机）上，发布一个任务，使用刚刚测试时上传的httpd镜像，运行六个容器，命令如下：
```
[root@docker01 ~]# docker service create --replicas 6 --name lvjianzhao -p 80 192.168.20.6:5000/lvjianzhao:latest 
#上述命令中，“--replicas”选项就是用来指定要运行的容器数量
```
当运行六个容器副本后，可以查看群集的web UI界面，显示如下：

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221436.png)

注意：docker03并没有下载相应的镜像，但是也会运行httpd服务，那么就可以得出一个结论：若docker主机没有指定的镜像，那么它将会自动去下载相应的镜像。

可以看到，在进行上述配置后，群集中的三台服务器基于httpd镜像运行了两个容器。共六个：
```
[root@docker01 ~]# docker service ls   #查看service的状态
ID                  NAME                MODE                REPLICAS            IMAGE                                 PORTS
13zjbf5s02f8        lvjianzhao          replicated          6/6                 192.168.20.6:5000/lvjianzhao:latest   *:30000->80/tcp
```


## 7.实现docker容器的扩容及缩容
何为扩容？何为缩容？无非就是在容器无法承担当前负载压力的情况下，扩增几个一样的容器，缩容呢？也就是在大量容器资源闲置的情况下，减少几个一样的容器而已。
### 7.1.下面是针对上述创建的6个httpd服务的容器的扩容及缩容：
**容器的扩容：**

```
[root@docker01 ~]# docker service scale lvjianzhao=9    #将运行的httpd容器扩容到9个
```
扩容后，其web UI界面显示如下：

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221451.png)

**容器的缩容**

```
[root@docker01 ~]# docker service scale lvjianzhao=3
将9个httpd服务的容器缩减到3个
```
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221501.png)
### 7.2.设置某个docker服务器不运行容器
在上述的配置中，若运行指定数量的容器，那么将是群集中的所有docker主机进行轮询的方式运行，直到运行够指定的容器数量，那么，如果不想让docker01这个manager角色运行容器呢？（公司领导也不会去一线搬砖的嘛），可以进行以下配置：
```
[root@docker01 ~]# docker node update --availability drain docker01
#设置主机docker01以后不运行容器，但已经运行的容器并不会停止
# “--availability”选项后面共有三个选项可配置，如下：
# “active”：工作；“pause”：暂时不工作；“drain”：永久性的不工作
```

## 8.附加——docker Swarm群集常用命令
```
[root@docker01 ~]# docker node ls    #查看群集的信息（只可以在manager角色的主机上查看）
[root@docker01 ~]# docker swarm join-token worker      #如果后期需要加入worker端，可以执行此命令查看令牌（也就是加入时需要执行的命令）
[root@docker01 ~]# docker swarm join-token manager         #同上，若要加入manager端，则可以执行这条命令查看令牌。
[root@docker01 ~]# docker service scale web05=6    #容器的动态扩容及缩容
[root@docker01 ~]# docker service ps web01       #查看创建的容器运行在哪些节点
[root@docker01 ~]# docker service ls        #查看创建的服务#将docker03脱离这个群集
[root@docker03 ~]# docker swarm leave        #docker03脱离这个群集
[root@docker01 ~]# docker node rm docker03         #然后在manager角色的服务器上移除docker03
[root@docker01 ~]# docker node promote docker02    #将docker02从worker升级为manager。#升级后docker02状态会为Reachable
[root@docker01 ~]# docker node demote docker02    #将docker02从manager角色降级为worker
[root@docker01 ~]# docker node update --availability drain docker01#设置主机docker01以后不运行容器，但已经运行的容器并不会停止
[root@docker01 ~]# docker node update --label-add mem=max docker03#更改docker03主机的标签为mem=max
[root@docker01 ~]# docker service update --replicas 8 --image 192.168.20.6:5000/lvjianzhao:v2.0 --container-label-add 'node.labels.mem == max' lvjianzhao05#将服务升级为8个容器，并且指定在mem=max标签的主机上运行
```


## 9.docker Swarm总结
在我对docker Swarm群集进行一定了解后，得出的结论如下：

- 参与群集的主机名一定不能冲突，并且可以互相解析对方的主机名；
- 集群内的所有节点可以都是manager角色，但是不可以都是worker角色；
- 当指定运行的镜像时，如果群集中的节点本地没有该镜像，那么它将会自动下载对应的镜像；
- 当群集正常工作时，若一个运行着容器的docker服务器发生宕机，那么，其所运行的所有容器，都将转移到其他正常运行的节点之上，而且，就算发生宕机的服务器恢复正常运行，也不会再接管之前运行的容器；

**Swarm 的不足**

- 功能简单有限

- 当集群中某台机器的资源 ( CPU、内存等 ) 不足时，**Swarm** 在部署服务的时候还是会傻傻地平均分配容器到这台机器上。

## 10.Docker Swarm网络管理
Swarm群集会产生两种不同类型的流量：

- 控制和管理层面：包括 Swarm 消息管理等，例如请求加入或离开Swarm，这种类型的流量总是被加密的。(涉及到集群内部的hostname、ip-address、subnet、gateway等)；
- 应用数据层面：包括容器与客户端的通信等。（涉及到防火墙、端口映射、网口映射、VIP等）

在Swarm service中有三个重要的网络概念：

- overlay networks 管理Swarm中docker守护进程间的通信。可以将容器附加到一个或多个已存在的overlay网络上，使容器与容器之间能够通信；
- ingress network 是一个特殊的 overlay 网络，用于服务节点间的负载均衡。当任何 Swarm 节点在发布的端口上接收到请求时，它将该请求交给一个名为 IPVS 的模块。IPVS 跟踪参与该服务的所有IP地址，选择其中的一个，并通过 ingress 网络将请求路由到它；
- 初始化或加入 Swarm 集群时会自动创建 ingress 网络，大多数情况下，用户不需要自定义配置，但是 docker 17.05 和更高版本允许你自定义。
- docker_gwbridge是一种桥接网络，将 overlay 网络（包括 ingress 网络）连接到一个单独的 Docker 守护进程的物理网络。默认情况下，服务正在运行的每个容器都连接到本地 Docker 守护进程主机的 docker_gwbridge 网络。
docker_gwbridge 网络在初始化或加入 Swarm 时自动创建。大多数情况下，用户不需要自定义配置，但是 Docker 允许自定义。

查看docker01上面的默认网络，如下（注意其SCOPE列，确认其生效范围）：

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221520.png)

除了Swarm群集默认创建的两个网络以外，我们还可以自定义创建overlay网络，连接到此网络的容器，即可互相通信，但是需要注意，除了在docker01这个manager上可以查看创建的overlay网络外，其他节点在没有加入此网络前，执行“docker network ls”命令是查看不到的。

### 10.1.创建自定义overlay网络并验证
```
[root@docker01 ~]# docker network create -d overlay --subnet 192.168.22.0/24 --gateway 192.168.22.1 --attachable my_net1
# 创建一个overlay网络，名字为my_net1；
# “--subnet”：指定其网段（可以不指定）；“--gateway”：指定其网关（可以不指定）；
# 但是在docker  Swarm群集中创建overlay网络时，必须添加“--attachable”选项
# 否则，其他节点的容器运行时，无法使用此网络
```
创建完成后，在其他docker节点上是查看不到这个新创建的overlay网络的，但是，可以使用此网络（在运行容器时，直接指定即可，等容器运行后，便可以查看到此网络了）

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221532.png)

测试刚刚创建的overlay网络，是否可用，分别在docker01、docker02上基于创建的overlay网络运行一个容器，然后进行ping测试，确认可以ping通：

```
#docker01主机上基于overlay网络创建一个容器：
[root@docker01 ~]# docker run -tid --network my_net1 --name test1 busybox
#同docker01的操作，在docker02上也创建一个：
[root@docker02 ~]# docker run -tid --network my_net1 --name test2 busybox
```
在容器创建后，在docker02主机上，使用test2这个容器去ping容器test1，测试结果如下（由于是自定义网络，所以可以直接ping对端容器的容器名）：

![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221544.png)

## 11.Swarm的service管理及版本更新
### 11.1.指定某个service运行在同一台docker服务器上
在第一篇的博文中测试过，如果Swarm群集中的manager下发一个service任务，那么，下发的任务将随机分布在群集中的docker服务器之上运行， 如果说，由于需要将自己的生产环境配置的统一、规范一些，某一台docker服务器，我就只运行web服务，另一台docker主机，我就只运行PHP服务，那么，怎么解决呢？

解决方案一：
```
[root@docker01 ~]# docker service create --replicas 3 --constraint node.hostname==docker03 --name test nginx
#在docker03主机上，基于nginx镜像，运行3个名为test的容器
```
上述命令的执行后如下所示：
![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221601.png)

解决方案二：
```
[root@docker01 ~]# docker node update --label-add mem=max docker02
#以键值对的方式给docker02主机打上标签“mem=max”，等号两边的内容是可以自定义的
[root@docker01 ~]# docker service create --name test01 --replicas 3 --constraint 'node.labels.mem==max' nginx
#基于nginx镜像在标签为“mem==max”的主机上运行3个名为test01的服务
[root@docker01 ~]# docker node inspect docker02   #可以执行此命令查看dokcer02主机的标签
#标签相关的信息，在Spec{  }中
```
查看web UI界面进行确认：

![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221839.png)

### 11.2.更新某个service版本
#### 11.2.1.准备要使用的镜像，并基于此镜像运行service
```
[root@docker01 aa]# cat html/index.html    #准备网页文件
127.0.0.1
[root@docker01 aa]# cat Dockerfile      
#基于nginx容器，将当前目录下的html目录挂载为nginx的网页根目录
FROM nginx
ADD html /usr/share/nginx/html
[root@docker01 aa]# docker build -t 192.168.20.6:5000/testnginx:1.0 .     #生成一个镜像
[root@docker01 aa]# docker push 192.168.20.6:5000/testnginx:1.0
#将新生成的镜像上传至私有仓库
[root@docker01 aa]# docker service create --name newnginx -p 80:80 --replicas 3  192.168.20.6:5000/testnginx:1.0 
#基于上传到私有仓库的镜像，运行三个service，并映射到本地80端口
#当上面的命令执行成功后，只要docker主机上运行着那个service，就可以通过它的80端口访问到nginx服务
```
运行后，web UI界面显示如下：

![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221909.png)

可以看到，每个节点都运行了那个service，也就是说，访问哪个节点的80端口，都可以看到一样的页面，如下：

![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413221958.png)

![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413222010.png)

在docker01上查看service的详细信息，如下：

```
[root@docker01 aa]# docker service ps newnginx    #查看service的详细信息
```
命令执行的结果（需要注意的是其镜像标签，也就是说注意其是基于哪个镜像运行的）：

![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413222127.png)

#### 11.2.2.准备该镜像的2.0版本（模拟在线版本升级）：
```
[root@docker01 aa]# docker tag nginx:latest 192.168.20.6:5000/testnginx:2.0 
#准备2.0版本的镜像
[root@docker01 aa]# docker push 192.168.20.6:5000/testnginx:2.0 
#上传到私有仓库
[root@docker01 aa]# docker service update --image 192.168.20.6:5000/testnginx:2.0 newnginx 
#将newnginx服务的镜像升级到2.0
[root@docker01 aa]# docker service ps newnginx    #再次查看service的详细信息
```
命令执行的结果如下，发现基于1.0镜像运行的newnginx的service状态已经变成了shutdown，而基于2.0运行的service变为了running，如下：

![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413222140.png)

此时，若再次访问其web页面，就变为了nginx的默认首页（因为我们的2.0镜像只是更改了下nginx镜像的标签，并没有修改其文件），如下：

![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413222156.png)

其web UI界面可以查看到该service的最后一次升级的时间。

#### 11.2.3.升级2.0到3.0（升级时，对其进行精细的控制）
```
[root@docker01 aa]# docker tag nginx:latest 192.168.20.6:5000/testnginx:3.0 
#准备3.0版本的镜像
[root@docker01 aa]# docker push 192.168.20.6:5000/testnginx:3.0 
#上传到私有仓库
[root@docker01 ~]# docker service update --replicas 6 --image 192.168.20.6:5000/testnginx:3.0 --update-parallelism 3 --update-delay 1m newnginx
#上述选项的含义如下：
# “--replicas 6”：更新后的service数量为6个（原本是3个）
# “ --update-parallelism 2 ”：设置并行更新的副本数。
# “ --update-delay 1m ”：指定滚动更新的时间间隔为1分钟
[root@docker01 ~]# docker service ps newnginx    #自行对比newnginx服务的详细信息
```
#### 11.2.4.版本回滚操作
当我们升级到新的版本后，发现新版本的镜像有些问题，而不得不返回之前运行的版本，那么可以执行下面的操作：
```
[root@docker01 ~]# docker service update --rollback newnginx   #将newnginx的service回滚到前一个版本
[root@docker01 ~]# docker service ps newnginx   #自行查看
```
执行回滚命令后，回滚过程如下：

![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413222211.png)回滚成功后，我这里就从原来的3.0变回了2.0，虽然在升级3.0的时候，指定的service数量是6个，但是之前只有3个，所以在执行回滚操作后，service数量也将变回3个。

注意：当我们执行回滚操作的时候，默认是回滚到上一次操作的版本，并且不可以连续回滚。