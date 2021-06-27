[toc]



# Docker 资源设置

## 1.Docker 容器磁盘限制

**如何看docker磁盘模式**

```
docker info|more
```

### 1.1.磁盘驱动模式为devicemapper

`Docker` 从 `1.13` 版本开始默认磁盘驱动模式：`overlay2`，可以修改为 `Devicemapper` 模式，修改方法：

- `vim /etc/sysconfig/docker-storage`

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502223742.webp)

- `vim /etc/sysconfig/docker-storage-setup`

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502223814.webp)

  这里也要修改改成 `devicemapper`

- 保存重启 `docker` 服务（这下引擎变了，数据也就都丢失了，镜像和容器都没了，所以用什么模式要规划好不要随便改磁盘模式）

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502223834.webp)

#### 1.1.1.新建容器磁盘资源限制

可以指定默认容器的大小（在启动容器的时候指定），可以在 `docker` 配置文件里通过 `dm.basesize` 参数指定，指定 `Docker` 容器`rootfs` 容量大小为 `20G`：

```
vim /etc/sysconfig/docker-storage
修改为如下代码：
DOCKER_STORAGE_OPTIONS="--storage-driver devicemapper --storage-opt dm.basesize=20G"
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502223909.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502223912.png)

重启 `docker` 服务。

启动一个容器后查看磁盘。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502223917.png)

**注意**：`devicemapper` 模式下扩容不支持 `xfs`，`overlay2` 可以支持 `xfs`

#### 1.1.2.容器启动后给在线扩容

基于现有容器在线扩容，宿主机文件系统类型支持：`ext2`、`ext3`、`ext4`、不支持 `XFS`。

- 查看原容器的磁盘空间大小：

- 查看 `mapper` 设备：

- 查看卷信息表：

- 根据要扩展的大小，计算需要多少扇区：

  第二个数字是设备的大小，表示有多少个512－bytes 的扇区. 这个值略高于 10GB 的大小。

  我们来计算一下一个 15GB 的卷需要多少扇区，

  `$ echo $((15*1024*1024*1024/512)) 31457280`

  修改卷信息表--激活--并且验证（红色3个部分）

- 修改文件系统大小：

- 最后验证磁盘大小：

### 1.2.磁盘驱动模式为overlay2

#### 1.2.1.新容器生成时指定默认容器大小

修改 `docker` 配置文件 `/etc/sysconfig/docker-storage` 中，`OPTIONS` 参数后面添加如下代码，指定 `docker` 容器 `rootfs` 容量大小为 `10G`

 `OPTIONS=‘--storage-opt  over lay2.size=10G’`，修改完重启docker

#### 1.2.2.给正在运行的容器指定大小

1. 首先添加一块磁盘 `sdb 10G`

   ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502224533.webp)

2. 格式化硬盘为 `xfs` 格式

    ```undefined
    mkfs.xfs -f /dev/sdb
    ```

3. 创建 `data` 目录，把 `sdb` 挂载，开启配额功能（默认 `xfs` 支持配额功能）

    ```kotlin
    mkfs.xfs -f /dev/sdb
    mount -o uquota,prjquota /dev/sdb /data/
    ```
    
    挂载配额类型：
    
    - 根据用户：（uquota/usrquota/quota）
    - 根据组：(qguota|grpquota)
    - 根据目录：（pguota/pr jquota）(不能与grpquota同时设定)

4. 查看配额配置详情，命令：

    ```kotlin
    xfs_quota -x -c 'report'  /data/
    ```
    
    ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502224705.webp)

5. 可以通过命令行 `xfs_quota` 设置来为用户和目录分配配额，也可以通过命令来查看配额信息

    ```kotlin
    例如给用户fp1限制磁盘配为10M
    xfs_quota -x -c 'limit  bsoft=10M bhard=10M fp1'  /data
    ```
    
    ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502224733.webp)
    
    测试：发现 `fp1` 在 `data` 目录下确实最多只能写 `10M`
    
    ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502224752.webp)

6. 将 `docker` 引擎默认数据存储目录 `/var/lib/docker` 重命名，并将 `/data/docker` 目录软链接到 `/var/lib` 下即可

## 2.清理 Docker 占用的磁盘空间

`Docker` 很占用空间，每当我们运行容器、拉取镜像、部署应用、构建自己的镜像时，我们的磁盘空间会被大量占用。

如果你也被这个问题所困扰，咱们就一起看一下 `Docker` 是如何使用磁盘空间的，以及如何回收。

`docker` 占用的空间可以通过下面的命令查看：

```javascript
$ docker system df
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231316.webp)

`TYPE` 列出了 `docker` 使用磁盘的 4 种类型：

- **Images**：所有镜像占用的空间，包括拉取下来的镜像，和本地构建的。
- **Containers**：运行的容器占用的空间，表示每个容器的读写层的空间。
- **Local Volumes**：容器挂载本地数据卷的空间。
- **Build Cache**：镜像构建过程中产生的缓存空间（只有在使用 BuildKit 时才有，Docker 18.09 以后可用）。

最后的 `RECLAIMABLE` 是可回收大小。

**可以进一步通过-v参数查看空间占用细节**

```
[root@dockercon ~]# docker system df -v
#镜像空间使用情况
Images space usage:

REPOSITORY                    TAG                 IMAGE ID            CREATED ago         SIZE                SHARED SIZE         UNIQUE SiZE         CONTAINERS
kalilinux/kali-linux-docker   latest              c927a54ec8a4        8 days ago ago      1.884GB             0B                  1.884GB             0
nginx                         latest              3f8a4339aadd        9 days ago ago      108.5MB             0B                  108.5MB             0
busybox                       latest              6ad733544a63        2 months ago ago    1.129MB             0B                  1.129MB             0

#容器空间使用情况
Containers space usage:

CONTAINER ID        IMAGE               COMMAND             LOCAL VOLUMES       SIZE                CREATED ago         STATUS              NAMES

#本地卷使用情况
Local Volumes space usage:

VOLUME NAME         LINKS               SIZE

Build cache usage: 0B
```



下面就分别了解一下这几个类型。

### 2.1.容器的磁盘占用

每次创建一个容器时，都会有一些文件和目录被创建，例如：

- `/var/lib/docker/containers/ID`目录，如果容器使用了默认的日志模式，他的所有日志都会以 `JSON` 形式保存到此目录下。
- `/var/lib/docker/overlay2` 目录下含有容器的读写层，如果容器使用自己的文件系统保存了数据，那么就会写到此目录下。

现在我们从一个完全干净的系统开始，假设 `docker` 刚刚安装：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231330.webp)

首先，我们启动一个 `NGINX` 容器：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231339.webp)

现在运行 `df` 命令后，就会看到：

- 一个镜像，126MB
- 一个容器

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231348.webp)

此时没有可回收空间，因为容器在运行，镜像正被使用。

现在，我们在容器内创建一个 `100MB` 的空文件：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231357.webp)

```javascript
$ docker exec -ti www \
  dd if=/dev/zero of=test.img bs=1024 count=0 seek=$[1024*100]
```

再次查看空间：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231403.webp)

可以看到容器占用的空间增加了，这个文件保存在本机哪里呢？

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231413.webp)

和上面说的一样，是保存在容器的读写层。

当停止容器后，容器占用的空间就会变为可回收的：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231420.webp)

如何回收呢？删除容器时会删除其关联的读写层占用的空间。

也可以一键删除所有已经停止的容器：

```javascript
$ docker container prune
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231429.webp)

删除容器后，镜像也可以回收了：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231437.webp)

上面的 `docker container prune` 命令是删除停止的容器，如果想删除所有容器（包括停止的、正在运行的），可以使用下面这2个命令：

```javascript
$ docker rm -f $(docker ps -aq)

$ docker container rm -f $(docker container ls -aq)
```

### 2.2.镜像的磁盘占用

有一些镜像是隐形的：

- 子镜像，就是被其他镜像引用的中间镜像，不能被删除。
- 悬挂状态的镜像，就是不会再被使用的镜像，可以被删除。

下面的命令列出所有悬挂状态的镜像：

```javascript
$ docker image ls -f dangling=true
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231457.webp)

删除这类镜像：

```javascript
$ docker image rm $(docker image ls -f dangling=true -q)
```

或者：

```javascript
$ docker image prune
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231508.webp)

如果想删除所有镜像，可以使用下面的命令：

```javascript
$ docker image rm $(docker image ls -q)
```

注意，正在被容器使用的镜像是不能被删除的。

### 2.3.数据卷的磁盘占用

数据卷是容器自身文件体统之外的数据存储。

例如容器中的应用有上传图片的功能，上传之后肯定不能保存在容器内部，因为容器内部的数据会随着容器的死掉而被删除，所以，这些图片要保存在容器之外，也就是数据卷。

比如我们运行了一个 `MongoDB` 容器做测试，导入了很多测试数据，这些数据就不是在容器内部的，是在数据卷中，因为 `MongoDB` 的 `Dockerfile` 中使用了数据卷。

测试完成后，删除了这个 `MongoDB` 容器，但测试数据还在，没被删除。

删除不再使用的数据卷：

```javascript
$ docker volume rm $(docker volume ls -q)
```

或者：

```javascript
$ docker volume prune
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231536.webp)

### 2.4.Build Cache 的磁盘占用

`Docker 18.09` 引入了 **BuildKit**，提升了构建过程的性能、安全、存储管理等能力。

删除 `build cache` 可以使用命令：

```javascript
$ docker builder prune
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231549.webp)

### 2.5.一键清理

通过上面的说明，我们知道了像容器、镜像、数据卷都提供了 `prune`这个子命令，帮助我们回收空间。

其实，docker 系统层面也有 `prune` 这个子命令，可以一键清理没用的空间：

```javascript
$ docker system prune
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502231558.webp)

定期执行这个命令是个好习惯。

**docker system prune 自动清理说明：**

- 该指令默认会清除所有如下资源：
  - 已停止的容器（container）
  - 未被任何容器所使用的卷（volume）
  - 未被任何容器所关联的网络（network）
  - 所有悬空镜像（image）。
- 该指令默认只会清除悬空镜像，未被使用的镜像不会被删除。添加 `-a` 或 `--all` 参数后，可以一并清除所有未使用的镜像和悬空镜像。
- 可以添加 `-f` 或 `--force` 参数用以忽略相关告警确认信息。

```ruby
[root@dockercon ~]# docker system prune --help

Usage:  docker system prune [OPTIONS]

Remove unused data

Options:
  -a, --all             Remove all unused images not just dangling ones
      --filter filter   Provide filter values (e.g. 'label=<key>=<value>')
  -f, --force           Do not prompt for confirmation
      --volumes         Prune volumes
[root@dockercon ~]# docker system prune --all
WARNING! This will remove:
        - all stopped containers
        - all networks not used by at least one container
        - all images without at least one container associated to them
        - all build cache
Are you sure you want to continue? [y/N] y
Deleted Containers:
f095899e7343e160d5b32d0688a6561a1a7f6af91c42ffe966649240b58ca23f

Deleted Images:
untagged: busybox:latest
untagged: busybox@sha256:e3789c406237e25d6139035a17981be5f1ccdae9c392d1623a02d31621a12bcc
deleted: sha256:6ad733544a6317992a6fac4eb19fe1df577d4dec7529efec28a5bd0edad0fd30
deleted: sha256:0271b8eebde3fa9a6126b1f2335e170f902731ab4942f9f1914e77016540c7bb
untagged: kalilinux/kali-linux-docker:latest
untagged: kalilinux/kali-linux-docker@sha256:28ff9e4bf40f7399e0570394a2d3d388a7b60c748be1b0a180c14c87afad1968
deleted: sha256:c927a54ec8a46164d7046b2a6dc09b2fce52b3066317d50cf73d14fa9778ca48
deleted: sha256:244c1920ef0442167cdbd095e5d29813cb5be0b70cc116faf8d7e50074f6c446
deleted: sha256:7748477cf079d6b0c13925ca90a5a1c7e93b8b508853f0cdff506c18caee14bd
deleted: sha256:dd9acc2ebbb7901b407d4270d4fd065d9bee10d11f2df13a256d892cc6e892f9
deleted: sha256:46c7843e50429fcafe2d3b6c676ac1a25e00851420ba2b1d52c69307f68ab3e5
deleted: sha256:f0944ddbb9bb11fb68f7edbde8e849233f7562d8087248c944e8c2fc7fe9fc0b
deleted: sha256:146e723c1713625c00cc736d74c9f6a16bd24464c42b33a8a234ec6e4c8b61ef
deleted: sha256:bca8a24862472a44c7ab1e3bdf2d5e4008e35d6c50b94f2547d3d595d86abef1
deleted: sha256:749be9d8a5ebb09cbc58d50c4b7244a10accdedc2a01c1d65d07d25322caacad
deleted: sha256:2d9e7ebb987a4cfb3142ce1612640248085d05b264012cb0885b3062105dfcb4
deleted: sha256:0655dca90e7c9c62d48128343ce89e016ae9f9df75c9dd6ad66c281e04e2b431
deleted: sha256:e78aa5d90040550584961eaccec1d047b755e97148fe753186e221c5ac40e330
deleted: sha256:598719dc4ba2de8d1be6564ca1f43846497608188cd20476712f7449755fea21
deleted: sha256:b084b4800972b561c21d804fab08c1fff0b9a9bcbf95a5394c0d4292c145c6d0
deleted: sha256:2e1b87f8f95e635c8ff4cbde28be38df39e8f3614576e09d7fb69c20421d1727
deleted: sha256:4a4a13e39112faa3b7ef0cb307bbf926fd1e46f3fbb9bc803cb9f4ab2f7694b0
untagged: alpine:latest
untagged: alpine@sha256:ccba511b1d6b5f1d83825a94f9d5b05528db456d9cf14a1ea1db892c939cda64
untagged: alpine-io:latest
deleted: sha256:3a043b0342a4907a1dfc95e2ea5e4df6a8e92d29dfe5d5910282bdfff27045d4
deleted: sha256:ddfb1d0e7629fd459b04f6efa89109ea0f7458aec76760e31888464d3074ae56
deleted: sha256:b6a7ea2197b744efab03320eda59d036ac3458ab7a0c5ada355faff0dd936af0
deleted: sha256:c96ab19b9ede349cb84e510a76a93d2b155aad54416f1591d7128cdeef228efc
deleted: sha256:43e7d32baaf31ab6bd4210ff3df54d1dec57cc761eab88c5eaef2973d6bed770
deleted: sha256:11a9226e2c0aeaa12408501b274575c8ee471a785b332af3c776e23dfd2eb629
deleted: sha256:bd9f490e64a2ceccdeb936f43047c0757635b4bc88159ba5b191285ef41f535c
deleted: sha256:e21c333399e0aeedfd70e8827c9fba3f8e9b170ef8a48a29945eb7702bf6aa5f
deleted: sha256:04a094fe844e055828cb2d64ead6bd3eb4257e7c7b5d1e2af0da89fa20472cf4
untagged: nginx:latest
untagged: nginx@sha256:cf8d5726fc897486a4f628d3b93483e3f391a76ea4897de0500ef1f9abcd69a1
deleted: sha256:3f8a4339aadda5897b744682f5f774dc69991a81af8d715d37a616bb4c99edf5
deleted: sha256:bb528503f6f01b70cd8de94372e1e3196fad3b28da2f69b105e95934263b0487
deleted: sha256:410204d28a96d436e31842a740ad0c827f845d22e06f3b1ff19c3b22706c3ed4
deleted: sha256:2ec5c0a4cb57c0af7c16ceda0b0a87a54f01f027ed33836a5669ca266cafe97a

Total reclaimed space: 5.219GB
```

### 2.6手动清除

对于悬空镜像和未使用镜像可以使用手动进行个别删除：

- 删除所有悬空镜像，不删除未使用镜像：

  ```
  docker rmi $(docker images -f "dangling=true" -q)
  ```

-  删除所有未使用镜像和悬空镜像

  ```
  docker rmi $(docker images -q)
  ```

-  清理卷

-  如果卷占用空间过高，可以清除一些不使用的卷，包括一些未被任何容器调用的卷（-v 详细信息中若显示 LINKS = 0，则是未被调用）：

  删除所有未被容器引用的卷：

  ```
  docker volume rm $(docker volume ls -qf dangling=true)
  ```

- 容器清理

  如果发现是容器占用过高的空间，可以手动删除一些：

  删除所有已退出的容器：

  ```
  docker rm -v $(docker ps -aq -f status=exited)
  ```

  删除所有状态为dead的容器

   ```
  docker rm -v $(docker ps -aq -f status=dead)
   ```

  

## X.资源设置案例

### X.1.Docker容器：磁盘&内存&CPU资源限制实战

#### X.1.1.首先先看看怎样查看这三项指标

#### X.1.1.1.先进入docker容器里

```
docker exec -it b6bac438271d /bin/bash
```

##### X.1.1.2.查看磁盘

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502225859.png)

##### X.1.1.3.查看内存

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502225903.png)

##### X.1.1.4.查看cpu

物理 `cpu`：主板上实际插入的 `cpu` 数量，可以数不重复的 `physical id` 有几个（`physical id`）

cpu核数：单块CPU上面能处理数据的芯片组的数量

逻辑cpu：一般情况下，`逻辑cpu=物理CPU个数×每颗核数`

```
1.物理cpu数：[root@server ~]# grep 'physical id' /proc/cpuinfo|sort|uniq|wc -l

2.cpu核数：[root@server ~]# grep 'cpu cores' /proc/cpuinfo|uniq|awk -F ':' '{print $2}'
```

注：top命令输入后，出现如下界面，再按1，可以看逻辑cpu，及每个核的cpu使用情况。

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502225906.png)

注：上面这样子看的和在宿主机上通过这些命令看，你会发现docker容器和宿主机都是一样的，这是为什么呢？

首先我们要知道，`docker` 默认容器和宿主机时共享所有的 `cpu`、内存、磁盘资源的，所以这样看都是一样的。为了不让个别容器因为受到攻击，大肆占用资源，造成其他容器也崩溃，我们需要对每个容器的资源多少进行限制。那么就有这两个问题：怎样看准确的呢？怎样对容器使用的这些资源进行限制呢？

#### X.1.2.怎样准确查看每个容器的资源消耗情况呢？

查看磁盘：只有上面这样子看！

查看每个容器的内存、cpu消耗情况：

```
#宿主机上输入命令
docker stats #这是实时查看cpu、内存消耗情况
docker stats --no-stream #查看瞬间的cpu、内存消耗情况
```

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502225917.png)

#### X.1.3.怎样对每个容器使用的这些资源进行限制呢？

如果我们买过云主机，那么每个云主机的磁盘、cpu、内存都有明确的配置，比如我买的阿里云主机就是40G的硬盘、1核cpu、2G内存。我们接下来就是需要对容器进行这样的限制。

##### X.1.3.1.cpu、内存限制

```
docker run -itd --cpuset-cpus=0-0 -m 4MB docker.io/jdeathe/centos-ssh /bin/bash
--cpuset-cpus:设置cpu的核数，0-0、1-1、2-2...(这种是绑定cpu,把本虚拟机绑定在一个逻辑cpu上);0-1、0-2、0-3和0,1、0,2、0,3（这两种形式都是指定多个逻辑cpu，每次随机使用一个逻辑cpu,相当于是共享cpu）
#注意：一个docker容器绑定一个逻辑cpu便于监控容器占用cpu的情况;而共享cpu可以更好利用cpu资源，而且要选好cpu调度算法！
-m:设置内存的大小
```

我设置两核cpu，它报错了：

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502225924.png)

报错的意思是：守护进程的错误响应：请求的CPU不可用-请求的0-1，可用：0。

说明咱们的 `vmware` 虚拟机创建时只给了1核，所以这里不能给两核！

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230000.png)

查看cpu、内存是否限制成功：

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230002.png)

内存、`cpu` 限制成功！这个查看容器的 `cpu` 核数的方式不知道应该怎样查看！我后面加了 `cpu`，然后试了下，`top` 命令和通过看`cpuinfo` 文件都不能查看出一个容器占用的 `cpu` 资源实际情况！

##### X.1.3.2.磁盘大小限制

**X.1.3.2.1.备份镜像、容器文件**

- 备份镜像

    ```
    docker save docker.io/centos >/root/centos-image.tar
    ```

- 备份docker容器

    ```
    先docker commit把容器提交成镜像，再备份镜像即可
    ```

**X.1.3.2.2.修改docker配置文件**

`docker` 配置文件：`/etc/sysconfig/docker`（注意不是 `docker-storage` 文件）中，`OPTIONS` 参数后面添加如下代码：

```
OPTIONS='--storage-opt overlay2.size=40G'
```

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230008.png)

重启 `docker`，报错：

```
systemctl status docker.service -l
```

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230015.png)

注：`project Quota`(目录配额)

解释：`Overlay2 Docker` 磁盘驱动模式，如果要调整其大小，需要让 `Linux` 文件系统设置为 `xfs` ，并且支持目录级别的磁盘配额功能；

**X.1.3.2.3.接下来我们就做支持目录级别的磁盘配额功能**

首先理解什么叫支持目录的磁盘配额？

答：就是支持在固定大小目录中分配磁盘大小。目录有大小怎么理解？将一个固定大小的硬盘挂载到此目录,这个目录的大小就是硬盘的大小。然后目录可分配指定大小的硬盘资源给其下的文件

- 添加新的硬盘

  ![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230028.png)

  注：`/dev/sdb` 就是我添加的磁盘！

- 格式化硬盘为xfs文件系统格式

    ```
    mkfs.xfs -f /dev/sdb
    ```

- 创建data目录，后续将作为docker数据目录；

    ```
    mkdir /data/ -p
    ```

- 挂载data目录，并且开启磁盘配额功能（默认xfs支持配额功能）

    ```
    mount -o uquota,prjquota /dev/sdb /data/
    #把/dev/sdb这块新建硬盘挂载到/data/，且让/data/目录分别支持用户级别和目录级别的配额！其实只需要prjquota就行了，不需要uquota。
    ```
    
    ![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230037.png)
    
    上图证明/data目录的大小已经被限制！！
    
    那么支持目录的磁盘目录配额就是，支持将20G的/data/目录可分配这20G磁盘大小！
    
- 查看配额-配置详情，命令如下：

    ```
    xfs_quota -x -c 'report' /data/
    ```

	![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230044.png)

	运行了xfs_quota这个命令后，显示如上，说明，/data/这个目录已经支持了目录配额功能！

**X.1.3.2.4.从/data/docker/作软链接到/var/lib下**

```
mkdir /data/docker
mv /var/lib/docker/* /data/docker/
rm -rf /var/lib/docker/
ln -s /data/docker /var/lib/
```

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230051.png)

这个样子，不支持目录级别的磁盘配额功能的 `/var/lib/docker/` 目录，就变成支持目录级别的磁盘配额功能软链接到 `/data/docker/` 目录下的 `/var/lib/docker/` 目录

**X.1.3.2.5.docker load加载备份的镜像，并运行容器，查看磁盘大小**

```
docker load < /root/centos-image.tar
docker run -itd --privileged --cpuset-cpus=0-1 -m 4048M docker.io/centos /bin/bash
```

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230059.png)

磁盘大小限制为10G成功！

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210502230106.png)

内存大小限制为4G成功！

##### X.1.3.3.最后注意点

无论是磁盘大小的限制、还是cpu、内存，它们都不能超出实际拥有的大小！

比如我这台vmware的内存是4G、cpu两核、硬盘20G（因为这里可配额的/data/目录就只有20G），因为centos系统运行还需要占部分内存，所以容器指定内存最好不要超过3G，cpu不能超过两核（即0-0、1-1；0-1都可以）、硬盘不能超过20G（最好在15G以下）



