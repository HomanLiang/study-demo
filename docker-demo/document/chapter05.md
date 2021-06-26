[toc]



#  Docker 常用命令

## 1.帮助命令
### 1.1.查询docker版本
```
docker version
```
### 1.2.查询docker信息
```
docker info
```
### 1.3.docker帮助命令
```
docker --help
```
## 2.镜像命令
### 2.1.列出本地主机上的镜像
命令：`docker images`
```
[root@localhost ~]# docker images
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
redis               latest              857c4ab5f029        7 hours ago         98.2MB
hello-world         latest              fce289e99eb9        7 months ago        1.84kB
```
各个选项说明:
- `REPOSITORY`：表示镜像的仓库源
- `TAG`：镜像的标签
- `IMAGE ID`：镜像ID
- `CREATED`：镜像创建时间
- `SIZE`：镜像大小

同一仓库源可以有多个 `TAG`，代表这个仓库源的不同个版本，我们使用 `REPOSITORY:TAG` 来定义不同的镜像。

如果你不指定一个镜像的版本标签，例如你只使用 `ubuntu`，`docker` 将默认使用 `ubuntu:latest`  镜像( `latest` 最新的镜像)

`OPTIONS` 说明

- `-a `:列出本地所有的镜像（含中间映像层）

- `-q` :只显示镜像ID。

- `--digests`:显示镜像的摘要信息（64位表示）
```
[root@localhost ~]# docker images --digests 
REPOSITORY          TAG                 DIGEST                                                                    IMAGE ID            CREATED             SIZE
redis               latest              sha256:854715f5cd1b64d2f62ec219a7b7baceae149453e4d29a8f72cecbb5ac51c4ad   857c4ab5f029        7 hours ago         98.2MB
hello-world         latest              sha256:6540fc08ee6e6b7b63468dc3317e3303aae178cb8a45ed3123180328bcc1d20f   fce289e99eb9        7 months ago        1.84kB
```
- `--no-trunc` :显示完整的镜像信息
用法如：`docker images --no-trunc`

### 2.2.搜索镜像
命令：`docker search [option] keyword`

运行上面的命令实质是去：https://hub.docker.com 去搜索了

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412230633.png)

option说明：

```
[root@localhost ~]# docker search redis --
--filter    --format    --help      --limit     --no-trunc  
```
具体有以下参数：
- --filter 过滤输出内容
- --format 格式化输出内容；
- --help
- --limit 限制输出结果个数， 默认为 25 个
- --no-trunc 不截断输出结果 ;默认的话描述可能过长会截断，如果使用该参数就不会截断

### 2.3.获取镜像(下载镜像)
命令:`docker [image] pull NAME [ :TAG]`

`TAG`:默认为 `latest`

一般来说， 镜像的 `latest` 标签意味着该镜像的内容会跟踪最新版本的变更而变化， 内容是不稳定的。因此，从稳定性上考虑，不要在生产环境中忽略镜像的标签信息或使用默认的 `latest` 标记的镜像。

如下：下载 `tomcat` 镜像
```
[root@localhost ~]# docker pull  tomcat
Using default tag: latest
latest: Pulling from library/tomcat
a4d8138d0f6b: Pull complete 
dbdc36973392: Pull complete 
f59d6d019dd5: Pull complete 
aaef3e026258: Pull complete 
5e86b04a4500: Pull complete 
1a6643a2873a: Pull complete 
2ad1e30fc17c: Pull complete 
16f4e6ee0ca6: Pull complete 
928f4d662d23: Pull complete 
b8d24294d525: Pull complete 
Digest: sha256:2785fac92d1bcd69d98f2461c6799390555a41fd50d3f847b544368d594c637b
Status: Downloaded newer image for tomcat:latest
docker.io/library/tomcat:latest
```
>下载过程中可以看出 ，镜像文件 一般由若干层(layer)组成 ，a4d8138d0f6b这样的串是层的唯一id(实际上完整的id包括256比特，64个十六进制字符组成）。使用docker pull命令下载中会获取并输出镜像的各层信息。当不同的镜像包括相同的层时，本地仅存储了层的 一份内容，减小了存储空间

### 2.4.删除和清理镜像
#### 2.4.1.使用标签删除
命令：`docker rmi IMGE [IMAGE ... ]`, 其中 IMAGE 可以为标签或 ID

option:

- `-f, -force`: 强制删除镜像， 即使有容器依赖它；
- `-no-prune`: 不要清理未带标签的父镜像。

如删除 `tomcat` 镜像：

`docker rmi tomcat` (不加标签默认删除 `latet docker rmi tomcat:latest`)

```
[root@localhost ~]# docker rmi tomcat:latest 
Untagged: tomcat:latest
Untagged: tomcat@sha256:2785fac92d1bcd69d98f2461c6799390555a41fd50d3f847b544368d594c637b
Deleted: sha256:238e6d7313e368610d3c01f01b650e33a9945e511e4f36b5eeedefb7a29971ef
Deleted: sha256:67eea8132df6b0fa354a682fd4f2eb49b4b3a63c66039b24d86bc3a596cc549a
Deleted: sha256:a59b696ed1e28ad7b06c2e2a48fcef4b5180c35b239459189dbf0f5899c87881
Deleted: sha256:e1e64dfe774547c0d75b14f3ba38625e3879d55ea99e0c9957e657e6451e63b3
Deleted: sha256:fae8323f0f9c854e64592959cc474f8ab1641f6650c05ba7bd51a6328015c4cb
Deleted: sha256:b23cfa351628eed4bed4a5d461a81ed32af66c3daebdb45763c110f0a4c2892e
Deleted: sha256:ddee1f79c299d9818245b4f6446237105c6687af1915ae97bb283f604fff182b
Deleted: sha256:3ed2a85fd2cbb4d32f9be22c9f9d57d7691f64f6275e1067c850c446d5ab47d9
Deleted: sha256:a7fb515b82afb13dc97ba73d90f6543bfc149d9b2060f5b52e15300b26b5e0f1
Deleted: sha256:2588c3b123d0790c6e569fdce63f8d93bd1387973ac74a3b438f738121b4e2e7
```
通过执行 `docker rmi` 命令来删除只有一个标签的镜像， 可以看出会删除这个镜像文件的所有文件层
#### 2.4.2.使用ID删除
命令：`docker rmi ID`

如下删除redis镜像
```
[root@localhost ~]# docker images 
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
redis               latest              857c4ab5f029        22 hours ago        98.2MB
hello-world         latest              fce289e99eb9        7 months ago        1.84kB
[root@localhost ~]# docker rmi 857c4ab5f029
Error response from daemon: conflict: unable to delete 857c4ab5f029 (must be forced) - image is being used by stopped container a7459959783f
[root@localhost ~]# docker rmi -f  857c4ab5f029
Untagged: redis:latest
Untagged: redis@sha256:854715f5cd1b64d2f62ec219a7b7baceae149453e4d29a8f72cecbb5ac51c4ad
Deleted: sha256:857c4ab5f0291ecbb4de238be9d5f9676e63dcc9608f70c8acc3748fe9689911
```
`Error response from daemon: conflict: unable to delete 857c4ab5f029 (must be forced) - image is being used by stopped container a7459959783f`

上面删除过程报错了：意思就是当前删除的容器在被其他容器依赖，如果想强制删除加 -f 参数

注意， 通常并不推荐使用-f参数来强制删除一个存在容器依赖的镜像。 正确的做法是，先删除依赖该镜像的所有容器， 再来删除镜像。

### 2.5.清理镜像
使用 `Docker` 一段时间后， 系统中可能会遗留一些临时的镜像文件， 以及用的镜像， 可以通过 `docker image prune` 命令来进行清理。
支待选项包括：

- `-a, -all`: 删除所有无用镜像， 不光是临时镜像；
- `-filter filter`: 只清理符合给定过滤器的镜像
- `-f, -force`: 强制删除镜像， 而不进行提示确认。

### 2.6.导出镜像
```
[root@docker01 ~]# docker image list
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
centos              latest              ff426288ea90        3 weeks ago         207MB
nginx               latest              3f8a4339aadd        5 weeks ago         108MB
# 导出
[root@docker01 ~]# docker image save centos > docker-centos.tar.gz
```
### 2.7.导入镜像
```
[root@docker01 ~]# docker image load -i docker-centos.tar.gz
e15afa4858b6: Loading layer  215.8MB/215.8MB
Loaded image: centos:latest
[root@docker01 ~]# docker image list
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
centos              latest              ff426288ea90        3 weeks ago         207MB
nginx               latest              3f8a4339aadd        5 weeks ago         108MB
```
### 2.8.查看镜像的详细信息
```
[root@docker01 ~]# docker image inspect centos
```


## 3.容器命令
### 3.1.新建并启动容器
命令：`docker run [OPTIONS] IMAGE [COMMAND] [ARG...]`

`docker [container］ run`，等价于先执行 `docker [container] create` 命令，再执行 `docker [container] start` 命令。

`OPTIONS` 说明（常用）：有些是一个减号，有些是两个减号

- `--name="容器新名字"`: 为容器指定一个名称；(没有名字默认就是 `ID`)
- `-d`: 后台运行容器，并返回容器 `ID`，也即启动守护式容器；
- `-i`：以交互模式运行容器，通常与 -t 同时使用；
- `-t`：为容器重新分配一个伪输入终端，通常与 -i 同时使用；
- `-P`: 随机端口映射；
- `-p`: 指定端口映射，有以下四种格式
	- ip:hostPort(主机端口/对外暴露端口):containerPort(docker容器端口)
	- ip::containerPort
	- hostPort:containerPort
	- containerPort

启动交互式容器,下面命令启动一个 `bash` 终端允许用户交互
```
[root@localhost ~]# docker run -it  centos /bin/bash
[root@97a4aeea579f /]# ps
   PID TTY          TIME CMD
     1 pts/0    00:00:00 bash
    14 pts/0    00:00:00 ps
```
在交互模式下，用户可以通过所创建的终端来输人命令 例如 `pwd ls`
```
[root@97a4aeea579f /]# pwd
/
[root@97a4aeea579f /]# ls
anaconda-post.log  bin  dev  etc  home  lib  lib64  media  mnt  opt  proc  root  run  sbin  srv  sys  tmp  usr  var
```
用户可以按 `Ctrl+d` 或输入 `exit` 命令来退出容器：
```
[root@97a4aeea579f /]# exit
exit
[root@localhost ~]# 
```
对于所创建的 `bash` 容器，当用户使用 `exit` 命令退出 `bash` 进程之后，容器也会自动退出 。 这是因为对于容器来说，当其中的应用退出后，容器的使命完成，也就没有继续运行的必要了 。

### 3.2.列出当前所有正在运行的容器
命令：`docker ps [OPTIONS]` -> 默认是当前正在运行的容器

`OPTIONS` 说明（常用）：

- `-a`:列出当前所有正在运行的容器+历史上运行过的
- `-l`:显示最近创建的容器。
- `-n`：显示最近n个创建的容器。
- `-q` :静默模式，只显示容器编号。
- `--no-trunc` :不截断输出。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412230922.png)

### 3.3.退出容器
`exit(/ctrl+d)` 容器停止退出

`ctrl+P+Q` 容器不停止退出

### 3.4.启动容器
`docker start 容器ID或者容器名`

### 3.5.重启容器
`docker restart 容器ID或者容器名`

### 3.6.停止容器
`docker stop 容器ID或者容器名`

`docker [container] stop [-t I - -time [=10]] [CONTA工NER ... ］`

该命令会首先向容器发送 SIGTERM 信号，等待一段超时时间后（默认为 10 秒），再发送 SIGK工 LL 信号来终止容器,这个过程终端会进入阻塞状态
```
[root@localhost ~]# docker stop -t 10 84fca5fa15e3
84fca5fa15e3
```

### 3.7.强制停止容器
`docker kill 容器ID或者容器名`

### 3.8.删除已停止的容器
可以使用 `docker [container] rm` 命令来删除处于终止或退出状态的容器，命令格式为 `docker [container) rm [-f |-－ force] [-l| -－ link [-v |-－volumes] CONTAINER[CONTAINER ... ）` 。
主要支持的选项包括 ：

- － f, --force=false ： 是否强行终止并删除一个运行中的容器 ；
- － 1, --link=false ：删除容器的连接 ，但保留容器；
- － v, --volumes=false ：删除容器挂载的数据卷 。

#### 3.8.1.删除多个容器
`docker rm -f $(docker ps -a -q)`

`docker ps -a -q | xargs docker rm`

### 3.9.容器重要部分
#### 3.9.1.启动守护式容器
命令：`docker run -d 容器名`

运行如下命令：

```
[root@localhost ~]# docker run -d centos
5eb5c39e1c15ad637ebf86418a829017f02d47cee1b4d22b6c71a44a65fdbec9
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND             CREATED             STATUS              PORTS               NAMES
```
发现通过 `docker run -d centos` 容器启动后会返回一个唯一的 `id`，可以通过 `docker ps`，容器运行状态，但是发现没有正在运行的容器。

**使用镜像centos:latest以后台模式启动一个容器**

`docker run -d centos`

问题：然后 `docker ps -a` 进行查看, 会发现容器已经退出

很重要的要说明的一点: `Docker` 容器后台运行,就必须有一个前台进程.

容器运行的命令如果不是那些一直挂起的命令（比如运行 `top`，`tail`），就是会自动退出的。

这个是 `docker` 的机制问题,比如你的 `web` 容器,我们以 `nginx` 为例，正常情况下,我们配置启动服务只需要启动响应的 `service` 即可。例如

`systemctl start nginx`

但是,这样做,`nginx` 为后台进程模式运行,就导致 `docker` 前台没有运行的应用,这样的容器后台启动后,会立即自杀因为他觉得他没事可做了.所以，最佳的解决方案是,将你要运行的程序以前台进程的形式运行

改造上面命令：`docker run -d centos /bin/sh -c "while true;do echo hello world;sleep 2;done"`

然后通过 `docker ps` 就可以查到运行状态是 `up`
```
[root@localhost ~]# docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS               NAMES
16a2c2f75e4b        centos              "/bin/sh -c 'while t…"   7 seconds ago       Up 6 seconds                            reverent_lichterman
```
#### 3.9.2.查看容器日志
`docker logs -f -t --tail 容器ID`

- -t 是加入时间戳
- -f 跟随最新的日志打印
- --tail 数字 显示最后多少条

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412231115.png)

#### 3.9.3.查看容器内运行的进程
命令：`docker top 容器ID`
#### 3.9.4.查看容器内部细节
命令：`docker inspect 容器ID`
#### 3.9.5.进入正在运行的容器并以命令行交互
在使用－ d 参数时，容器启动后会进入后台，用户无法看到容器中的信息，也无法进行操作。

官方推荐使用 `attach` 或 `exec`.

然而使用 `attach` 命令有时候并不方便 。 当多个窗口同时 `attach` 到同一个容器的时候，所有窗口都会同步显示；当某个窗口因命令阻塞时，其他窗口也无法执行操作了 。

命令：

`docker exec -it 容器ID /bash/shell`

`docker exec -it 容器名 /bin/bash`

`docker exec -it 容器名 /bin/sh`

可以看到会打开一个新的 `bash` 终端，在不影响容器内其他应用的前提下，用户可以与容器进行交互。

通过指定 `-it` 参数来保持标准输入打开，并且分配一个伪终端。 通过 `exec` 命令对容器执行操作是最为推荐的方式 。

直接在宿主机中执行进入容器的命令，这样就不用再重新打开一个新的终端。
```
[root@localhost ~]# docker exec -it 0d32c7830086    ls -l /tmp
total 4
-rwx------. 1 root root 836 Mar  5 17:36 ks-script-eC059Y
-rw-------. 1 root root   0 Mar  5 17:34 yum.log
```
命令：重新进入 `docker attach 容器ID`

区别：
- `attach` 直接进入容器启动命令的终端，不会启动新的进程
- `exec` 是在容器中打开新的终端，并且可以启动新的进程

#### 3.9.6.从容器内拷贝文件到主机上
docker cp 容器ID:容器内路径 目的主机路径
```
[root@localhost /]# docker ps
CONTAINER ID        IMAGE               COMMAND                  CREATED             STATUS              PORTS               NAMES
0d32c7830086        centos              "/bin/sh -c 'while t…"   16 minutes ago      Up 16 minutes                           peaceful_newton
[root@localhost /]# docker cp 0d32c7830086:/tmp/yum.log /home
```


## 4.小结
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412231159.png)