[toc]



# Docker 镜像

## 1.镜像生命周期
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215651.png)
## 2.镜像是什么
镜像是一种轻量级、可执行的独立软件包，用来打包软件运行环境和基于运行环境开发的软件，它包含运行某个软件所需的所有内容，包括代码、运行时、库、环境变量和配置文件。
### 2.1.UnionFS（联合文件系统）
`UnionFS`（联合文件系统）：`Union` 文件系统（`UnionFS`）是一种分层、轻量级并且高性能的文件系统，它支持对文件系统的修改作为一次提交来一层层的叠加，同时可以将不同目录挂载到同一个虚拟文件系统下(`unite several directories into a single virtual filesystem`)。`Union` 文件系统是 `Docker` 镜像的基础。镜像可以通过分层来进行继承，基于基础镜像（没有父镜像），可以制作各种具体的应用镜像。

特性：一次同时加载多个文件系统，但从外面看起来，只能看到一个文件系统，联合加载会把各层文件系统叠加起来，这样最终的文件系统会包含所有底层的文件和目录
### 2.2.Docker镜像加载原理
`docker` 的镜像实际上由一层一层的文件系统组成，这种层级的文件系统 `UnionFS`。

`bootfs`(`boot file system`)主要包含 `bootloader` 和 `kernel` , `bootloader` 主要是引导加载 `kernel`, `Linux` 刚启动时会加载 `bootfs` 文件系统，在 `Docker` 镜像的最底层是 `bootfs` 。这一层与我们典型的 `Linux/Unix` 系统是一样的，包含 `boot` 加载器和内核。当 `boot` 加载完成之后整个内核就都在内存中了，此时内存的使用权已由 `bootfs` 转交给内核，此时系统也会卸载 `bootfs`。

`rootfs` (`root file system`) ，在 `bootfs` 之上。包含的就是典型 `Linux` 系统中的 `/dev`, `/proc`, `/bin`, `/etc` 等标准目录和文件。`rootfs` 就是各种不同的操作系统发行版，比如 `Ubuntu`，`Centos` 等等。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215705.png)

平时我们安装进虚拟机的 `CentOS` 都是好几个 `G`，为什么 `docker` 这里才 `200M`？？

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215720.png)

对于一个精简的 `OS`，`rootfs` 可以很小，只需要包括最基本的命令、工具和程序库就可以了，因为底层直接用 `Host` 的 `kernel`(使用宿主机的 `kernel`)，自己只需要提供 `rootfs` 就行了。由此可见对于不同的 `linux` 发行版, `bootfs` 基本是一致的, `rootfs` 会有差别, 因此不同的发行版可以公用 `bootfs`。

### 2.3.分层的镜像
以我们的 `pull tomcat` 为例，在下载的过程中我们可以看到 `docker` 的镜像好像是在一层一层的在下载(这个过程其实是在下 `tomcat` 运行所需要的环境)

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215733.png)

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215742.png)

## 3.特点
`Docker` 镜像都是只读的。当容器启动时，一个新的可写层被加载到镜像的顶部。这一层通常被称作“容器层”，“容器层”之下的都叫“镜像层”。

## 4.Docker镜像commit操作补充
### 4.1.提交命令
`docker commit` 提交容器副本使之成为一个新的镜像

命令：`docker commit -m=“提交的描述信息” -a=“作者” 容器ID 要创建的目标镜像名:[标签名]`
### 4.2.案列
#### 4.2.1.从Hub上下载tomcat镜像到本地并成功运行
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215831.png)
- 以指定端口启动tomcat容器

  `docker run -it -p 8888:8080 tomcat ---->以指定端口运行tomcat镜像`

  - -p 主机端口(对外暴露的端口):docker容器端口
  - -P 随机分配端口
  - -i:交互
  - -t:终端

  ![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412215940.png)

  tomcat镜像打印的日志

  ![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220003.png)

  访问进行验证

  ![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220020.png)

- 以随机端口启动tomcat容器
  `docker run -it -P tomcat`

  **查看进程**

  ![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220156.png)

  **浏览器访问**

  ![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220209.png)
#### 4.2.2.故意删除上一步镜像生产tomcat容器的文档
![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220223.png)
#### 4.2.3.commit镜像
命令：`docker commit -a="niugang" -m="tomcat without docs" 41bcfdd07e2b niugang/tomcat01:1.1`

注意：`niugang/` 是命名空间，可以理解为包名

![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220242.png)

#### 4.2.4.启动我们的新镜像并和原来的对比
运行刚才 `commit` 的 `niugang/tomcat01` 镜像

命令：`docker run -it -p 9999:8080 niugang/tomcat01:1.1`

这块必须加 `tag`，不加默认为 `latest`

![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220255.png)

运行从阿里云下载下来的镜像

命令：`docker run -it -p 8888:8080 tomcat`

![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210412220312.png)

#### 4.2.5.注意
注意以上 `docker run -it -p 8888:8080 tomcat` 都是前台启动，所谓的前台启动就是在启动 `tomcat` 的过程中，会打印 `tomcat` 日志。

还有另外一种就是后台启动。

```
[root@localhost ~]# docker run -d -p 8888:8080 tomcat
8e5498c1235294f74db95a21d6a41f407a2510a8297e2c4efcab291ca018125e
```