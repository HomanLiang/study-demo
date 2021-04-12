[toc]



# Docker 数据卷

## 1.数据卷

### 1.1.为什么需要数据卷？

这得从 docker 容器的文件系统说起。出于效率等一系列原因，docker 容器的文件系统在宿主机上存在的方式很复杂，这会带来下面几个问题：

- 不能在宿主机上很方便地访问容器中的文件。
- 无法在多个容器之间共享数据。
- 当容器删除时，容器中产生的数据将丢失。

为了解决这些问题，docker 引入了数据卷(volume) 机制。数据卷是存在于一个或多个容器中的特定文件或文件夹，这个文件或文件夹以独立于 docker 文件系统的形式存在于宿主机中。数据卷的最大特定是：**其生存周期独立于容器的生存周期**。

### 1.2.使用数据卷的最佳场景

- 在多个容器之间共享数据，多个容器可以同时以只读或者读写的方式挂载同一个数据卷，从而共享数据卷中的数据。
- 当宿主机不能保证一定存在某个目录或一些固定路径的文件时，使用数据卷可以规避这种限制带来的问题。
- 当你想把容器中的数据存储在宿主机之外的地方时，比如远程主机上或云存储上。
- 当你需要把容器数据在不同的宿主机之间备份、恢复或迁移时，数据卷是很好的选择。

### 1.3.数据卷原理

下图描述了 docker 容器挂载数据的三种方式：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412233259.png)

数据卷是完全被 docker 管理的，就像上图中的黄色区域描述的一样，docker 在宿主机的文件系统中找了个文件管理数据卷相关的数据。因此你可能根本不需要知道数据卷文件在宿主机上的存储位置(事实上抱着刨根问底的精神我们还是很想搞清楚它背后的工作原理！)。

docker 数据卷的本质是容器中的一个特殊目录。在容器创建的过程中，docker 会将宿主机上的指定目录(一个以数据卷 ID 为名称的目录)挂载到容器中指定的目录上。这里使用的挂载方式为绑定挂载(bind mount)，所以挂载完成后的宿主机目录和容器内的目标目录表现一致。
比如我们执行下面的命令创建数据卷 hello，并挂载到容器 testcon 的 /world 目录：

```
$ docker volume create hello
$ docker run -id --name testcon --mount type=volume,source=hello,target=/world ubuntu /bin/bash
```

实际上在容器的创建过程中，类似于在容器中执行了下面的代码：

```
// 将数据卷 hello 在宿主机上的目录绑定挂载到 rootfs 中指定的挂载点 /world 上
mount("/var/lib/docker/volumes/hello/_data", "rootfs/world", "none", MS_BIND, NULL)
```

在处理完所有的 mount 操作之后(真正需要 docker 容器挂载的除了数据卷目录还包括 rootfs，init-layer 里的内容，/proc 设备等)，docker 只需要通过 chdir 和 pivot_root 切换进程的根目录到 rootfs 中，这样容器内部进程就只能看见以 rootfs 为根的文件系统以及被 mount 到 rootfs 之下的各项目录了。例如我们启动的 testcon 中的文件系统为：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412233243.png)

下面我们介绍几个数据卷在使用中比较常见的问题。

### 1.4.数据的覆盖问题

- 如果挂载一个空的数据卷到容器中的一个非空目录中，那么这个目录下的文件会被复制到数据卷中。
- 如果挂载一个非空的数据卷到容器中的一个目录中，那么容器中的目录中会显示数据卷中的数据。如果原来容器中的目录中有数据，那么这些原始数据会被隐藏掉。

这两个规则都非常重要，灵活利用第一个规则可以帮助我们初始化数据卷中的内容。掌握第二个规则可以保证挂载数据卷后的数据总是你期望的结果。

### 1.5.容器内添加-直接命令添加(使用 mount 语法挂载数据卷)

#### 1.5.1.命令
命令： `docker run -it -v /宿主机绝对路径目录:/容器内目录 镜像名`
```
docker run -it -v /dataVolume:/dataVloumeContainer centos /bin/bash
```
查看容器内时候创建成功

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232600.png)

查看宿主机是否创建成功

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232612.png)

#### 1.5.2.查看数据卷是否挂载成功
命令：`docker inspect 容器ID`

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232646.png)

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232655.png)

上面的“RW”:true 标识具有读写权限

#### 1.5.3.容器和宿主机之间数据共享
主要是在dataVolume 和dataVolumeContainer 两个挂载的目录下演示数据是否共享

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232707.png)

上图主要演示在centos docker容器和宿主机中往test.txt文件中写数据，看双方数据是否一致。

结果在centos docker容器中编辑的test.txt文件能同步到宿主机中的test.txt文件中。

在宿主机中编辑的test.txt文件也能同步到centos docker 容器的test.txt中。
#### 1.5.4.容器停止退出后，主机修改后数据是否同步
演示步骤：
- 退出centos docker容器(exit 命令，退出并停止容器)
- 在宿主机的dataVolume上执行 echo "java world">>test02.txt
- docker ps -a 查询刚才停止退出的容器id
- docker start 容器id
- docker attach 容器id
- 查看dataVolumeContainer目录下是否有新建的test02.txt和之前的text.txt

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232723.png)

#### 1.5.5.命令(带权限)
命令： `docker run -it -v /宿主机绝对路径目录:/容器内目录:ro 镜像名`

添加了ro之后，只允许宿主机进行单项的操作，容器内目录只有读的权限

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232739.png)



### 1.6.容器内添加-直接命令添加(使用 volume driver 把数据存储到其它地方)

除了默认的把数据卷中的数据存储在宿主机，docker 还允许我们通过指定 volume driver 的方式把数据卷中的数据存储在其它的地方，比如 Azrue Storge 或 AWS 的 S3。
简单起见，我们接下来的 demo 演示如何通过 vieux/sshfs 驱动把数据卷的存储在其它的主机上。
docker 默认是不安装 vieux/sshfs 插件的，我们可以通过下面的命令进行安装：

```
$ docker plugin install --grant-all-permissions vieux/sshfs
```

然后通过 vieux/sshfs 驱动创建数据卷，并指定远程主机的登录用户名、密码和数据存放目录：

```
docker volume create --driver vieux/sshfs \
    -o sshcmd=nick@10.32.2.134:/home/nick/sshvolume \
    -o password=yourpassword \
    mysshvolume
```

注意，请确保你指定的远程主机上的挂载点目录是存在的(demo 中是 /home/nick/sshvolume 目录)，否则在启动容器时会报错。
最后在启动容器时指定挂载这个数据卷：

```
docker run -id \
    --name testcon \
    --mount type=volume,volume-driver=vieux/sshfs,source=mysshvolume,target=/world \
    ubuntu /bin/bash
```

这就搞定了，你在容器中 /world 目录下操作的文件都存储在远程主机的 /home/nick/sshvolume 目录中。进入容器 testcon 然后在 /world 目录中创建一个文件，然后打开远程主机的 /home/nick/sshvolume 目录进行查看，你新建的文件是不是已经出现在那里了！

### 1.7.容器内添加-DockerFile添加

Java EE Hello.java---->Hello.class

Docker images---->DockerFile(dokcer编程)

基于 Dockerfile 创建镜像是常见的方式。 Dockerfile 是一个文本文件，利用给定的指令描述基于某个父镜像创建新镜像的过程 。

**step1**

根目录下新建mydocker文件夹并进入

**step2**

可在Dockerfile中使用VOLUME指令来给镜像添加一个或多个数据卷

```
VOLUME["/dataVolumeContainer","/dataVolumeContainer2","/dataVolumeContainer3"]
```

说明：
出于可移植和分享的考虑，用-v 主机目录:容器目录这种方法不能够直接在Dockerfile中实现。

由于宿主机目录是依赖于特定宿主机的，并不能够保证在所有的宿主机上都存在这样的特定目录。

**step3**

File构建

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232806.png)

```
# volume test
FROM centos
VOLUME ["/dataVolumeContainer1","/dataVolumeContainer2"]
CMD echo "finished,--------success1"
CMD /bin/bash
```
**step4**

build后生成镜像

获得一个新镜像ng/centos
```
docker build -f /mydocker/dockerfile -t ng/centos .
```
![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232823.png)

**step5**

run容器

![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232837.png)

**step6**

通过上述步骤，容器内的卷目录地址已经知道对应的主机目录地址哪??

docker会默认生成一个，通过docker inspet 可以查看具体路径

![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232848.png)

**step7**

验证容器和宿主机之间是否数据共享

**step8**

备注

Docker挂载主机目录Docker访问出现cannot open directory .: Permission denied
解决办法：在挂载目录后多加一个--privileged=true参数即可

## 2.数据卷容器
### 2.1.是什么
命名的容器挂载数据卷，其它容器通过挂载这个(父容器)实现数据共享，挂载数据卷的容器，称之为数据卷容器

如果用户需要在多个容器之间共享一些持续更新的数据，最简单的方式是使用数据卷容器 。 数据卷容器也是一个容器，但是它的目的是专门提供数据卷给其他容器挂载。
### 2.2.总体介绍
![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232906.png)

![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232916.png)

以上一步新建的镜像ng/centos为模板并运行容器dc01/dc02/dc03

它们已经具有容器卷
- /dataVolumeContainer1
- /dataVolumeContainer2

### 2.3.容器间传递共享(--volumes-from)
**step 1**

先启动一个父容器dc01
```
docker run -it --name dc01 ng/centos /bin/bash
```
![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232931.png)

在dataVolumeContainer2新增内容

**step 2**

dc02/dc03继承自dc01 (主要使用--volumes-from)
```
docker run -it --name dc02 --volumes-from dc01 ng/centos
docker run -it --name dc03 --volumes-from dc01 ng/centos
```
![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232940.png)

![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412232946.png)

dc02 dc03容器继承与dc01容器，并在dc2容器的dataVolumeContainer2文件夹下，新增dc02_add.txt文件。

此时可以看到，dc01 dc03容器也有新增加的文件。

并在dc03容器的dataVolumeContainer2文件夹下，新增dc03_add.txt文件。此时可以看到，dc01 dc02容器也有新增加的文件。

**step 3**

删除dc01，dc02修改后dc03可否访问------->是可以访问的

删除dc01容器
```
docker container rm -f dc01
```
**step 4**

删除dc02后dc03可否访问---->是可以访问的

**step 5**

新建dc04继承dc03后再删除dc03
```
docker run -it --name dc04 --volumes-from dc03 ng/centos
docker container rm -f dc03
```

结论：容器之间配置信息的传递，数据卷的生命周期一直持续到没有容器使用它为止