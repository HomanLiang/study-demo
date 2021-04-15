[toc]



# Docker Machine

## 1.Docker Machine 是什么？

[Docker Machine](https://docs.docker.com/machine/overview/) 是 Docker 官方提供的一个工具，它可以帮助我们在远程的机器上安装 Docker，或者在虚拟机 host 上直接安装虚拟机并在虚拟机中安装 Docker。我们还可以通过 docker-machine 命令来管理这些虚拟机和 Docker。下面是来自 Docker Machine 官方文档的一张图，很形象哦！

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210526.png)

本文将通过一系列 demo 来展示 Docker Machine 的主要使用场景。

## 2.安装 Docker Machine

安装 Docker Machine 前请先在本地安装 [Docker](https://docs.docker.com/engine/installation/)。

Docker Machine 的安装十分简单，在 Ubuntu 中直接把可执行文件下载到本地就可以了。

```
$ curl -L https://github.com/docker/machine/releases/download/v0.12.0/docker-machine-`uname -s`-`uname -m` > /tmp/docker-machine
$ chmod +x /tmp/docker-machine
$ sudo mv /tmp/docker-machine /usr/local/bin/docker-machine
```

其中 v0.12.0 是最新的版本。当然 [Docker Machine](https://github.com/docker/machine) 是个开源项目，你可以选择安装不同的版本，或者是自行编译。下图为笔者安装之后显示的版本信息：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210538.png)

## 3.在远程主机上安装 Docker

如果我们有多台 Ubuntu 主机都需要安装 Docker，怎么办呢？是不是一个个登录上去通过 apt-get 命令安装呢？当然不需要，通过 docker-machine 命令我们可以轻松的在远程主机上安装 Docker。

### 前提条件

在使用 docker-machine 进行远程安装前我们需要做一些准备工作：

1. 在目标主机上创建一个用户并加入sudo 组
2. 为该用户设置 sudo 操作不需要输入密码
3. 把本地用户的 ssh public key 添加到目标主机上

比如我们要在远程主机上添加一个名为 nick 的用户并加入 sudo 组：

```
$ sudo adduser nick
$ sudo usermod -a -G sudo nick
```

然后设置 sudo 操作不需要输入密码：

```
$ sudo visudo
```

把下面一行内容添加到文档的最后并保存文件：

```
nick   ALL=(ALL:ALL) NOPASSWD: ALL
```

最后把本地用户的 ssh public key 添加到目标主机上：

```
$ ssh-copy-id -i ~/.ssh/id_rsa.pub nick@xxx.xxx.xxx.xxx
```

这几步操作的主要目的是获得足够的权限可以远程的操作目标主机。

### 3.1.安装命令

在本地运行下面的命令：

```
$ docker-machine create -d generic \
    --generic-ip-address=xxx.xxx.xxx.xxx \
    --generic-ssh-user=nick \
    --generic-ssh-key ~/.ssh/id_rsa \
    krdevdb
```

注意，create 命令本是要创建虚拟主机并安装 Docker，因为本例中的目标主机已经存在，所以仅安装 Docker。-d 是 --driver 的简写形式，主要用来指定使用什么驱动程序来创建目标主机。Docker Machine 支持在云服务器上创建主机，就是靠使用不同的驱动来实现了。本例中使用 generic 就可以了。接下来以 --generic 开头的三个参数主要是指定操作的目标主机和使用的账户。最后一个参数 krdevdb 是虚拟机的名称，Docker Machine 会用它来设置目标主机的名称。

好了，就这么简单！经过简短的等待 Docker 就在目标机器上安装成功了：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210551.png)

### 3.2.检查安装结果

我们可以通过 Docker Machine 的 ls 命令查看当前可管理的主机列表：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210558.png)

其中的 krdevdb 主机就是刚才我们安装了 Docker 的主机，最后一列显示了安装的 Docker 版本：v17.05.0-ce。

然后执行 `eval $(docker-machine env krdevdb) ` 命令，就可以通过本地的客户端操作远程主机上的 Docker daemon 了。执行 docker version 命令看看：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210603.png)

请注意上图中的 Client 和 Server 版本不一样，这也说明了我们正在使用本地的 Client 连接远程的 Server。

## 4.在本地主机上安装带有 Docker 的虚机

在实际使用中我们一般会在物理机上安装 vSphere 等虚拟机管理软件，并称之为虚拟机 host。然后通过 vSphere 工具安装虚拟机进行使用。接下来我们将介绍如何在本地的一台安装了 vSphere 的虚拟机 host 上安装带有 Docker 的虚拟机。直接上命令：

```
$ docker-machine create \
    --driver vmwarevsphere \
    --vmwarevsphere-vcenter=xxx.xxx.xxx.xxx \
    --vmwarevsphere-username=root \
    --vmwarevsphere-password=12345678 \
    --vmwarevsphere-cpu-count=1 \
    --vmwarevsphere-memory-size=512 \
    --vmwarevsphere-disk-size=10240 \
    testvm
```

解释一下比较重要的参数：

`--driver vmwarevsphere`
我们的虚拟机 host 上安装的是 vmware 的产品 vSphere，因此需要给 Docker Machine 提供对应的驱动，这样才能够在上面安装新的虚拟机。

```
--vmwarevsphere-vcenter=xxx.xxx.xxx.xxx
--vmwarevsphere-username=root
--vmwarevsphere-password=12345678
```

上面三行分别指定了虚拟机 host 的 IP 地址、用户名和密码。

```
--vmwarevsphere-cpu-count=1
--vmwarevsphere-memory-size=512
--vmwarevsphere-disk-size=10240
```

上面三行则分别指定了新创建的虚拟机占用的 cpu、内存和磁盘资源。

**testvm**
最后一个参数则是新建虚拟机的名称。
很快虚拟机的创建就完成了。先在 vSphere 的客户端中看一下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210733.png)

名为 testvm 的虚拟机已经在运行了。
再执行 docker-machine ls 命令看看：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210739.png)

已经可以看到 testvm了，并且它的 DRIVER 显示为 vmwarevsphere。
这就搞定了吗？
好像哪里不对呀！是的，平时我们手动创建虚机时最重要的东西是什么？是安装虚拟机的镜像啊！但这里我们并没有指定相关的东西，那么docker-machine 究竟给我们安装了一个什么系统？在使用 vmwarevsphere 驱动安装虚机时，我们不能指定自己喜欢的虚机镜像(可能是 Docker Machine 还没有准备好)。默认使用一个叫做 boot2docker 的虚拟机镜像，这个东西非常小，只有几十兆，因此安装会很快。

## 5.管理远程的 Docker

### 5.1.客户端服务器模式

Docker 一直是以客户端和服务器的模式运行的，只不过起初的版本是通过同一个二进制文件 docker 来启动服务器端 daemon 和客户端的。在近期的版本中，服务端的可执行文件已经和客户端的可执行文件分离开了。查看 /usr/bin 目录下的可执行文件：

![952033-20170618183456993-180819644](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210831.png)

其中 dockerd 就是执行服务器端任务的可执行文件。而我们平时执行本机 docker 任务则主要通过 docker 这个客户端命令给本机的服务器端发送任务。

### 5.2.使用本地的客户端连接远程的服务器

那么本地的客户端可不可以连接并发送任务给远程的 Docker 服务器端呢？当然是可以的，只不过我们手动设置起来稍微麻烦一些。不过没关系，Docker Machine 都为我们做好了！下面就让我们看看如何通过本地的 Docker 客户端在 krdevdb 这台主机上运行容器：

```
$ docker-machine env krdevdb
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210837.png)

这个命令输出的内容可以作为命令来设置一些 Docker 客户端使用的环境变量，从而让本机的 Docker 客户端可以与远程的 Docker 服务器通信。按照上面的提示执行命令：

```
$ eval $( docker-machine env krdevdb)
```

好了，在当前的命令行终端中，接下来运行的 docker 命令操作的都是远程主机 krdevdb 上的 Docker daemon。为了区分本机的 Docker daemon 操作，我们重新启动一个新的命令行终端，然后分别执行 docker ps 命令：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210841.png)

从上图中可以明显的看出本地主机和远程主机上分别运行着不同的容器。

### 5.3.管理远程 Docker daemon

除了运行基本的 docker 命令，Docker Machine 还能够管理远程的 Docker 主机。比如我们可以通过 start, stop, restart 命令分别启动、关闭和重启远程的 Docker daemon。这里的情况稍微复杂一些，只有支持这些命令的驱动才能完成相关的操作。比如，我们分别关闭 krdevdb 和 testvm：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413210847.png)

前一个提示 generic 驱动不支持 stop 命令。而 testvm 是通过 vmwarevsphere 驱动安装的，所以成功的执行了 stop。

对于远程管理来说，SSH 的支持是必不可少的！Docker Machine 当然也尽职尽责的完成了任务：

```
$ docker-machine ssh krdevdb
```

执行上面的命令就可以了。注意，这个命令可不会提示你输入密码，当然更不会让你去配置 SSH 秘钥什么的，因为 Docker Machine 私下全把脏活累活干完了。

## 6.总结

Docker Machine 的目的是简化 Docker 的安装和远程管理。从本文的内容我们也可以看到，Docker Machine 确实为我们使用和管理 Docker 带来了很多的便利。至于有待提高的方面，现在 Docker Machine 会安装最新版本的 Docker，笔者觉得如果能够支持指定安装 Docker 的版本就好了！