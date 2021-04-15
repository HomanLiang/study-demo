[toc]



# Docker DockerFile

## 1.是什么
Dockerfle 是一个文本格式的配置文件， 用户可以使用 Dockerfle 来快速创建自定义的镜像。

Dockerfile是用来构建Docker镜像的构建文件，是由一系列命令和参数构成的脚本。

在Docker中创建镜像最常用的方式，就是使用Dockerfile。Dockerfile是一个Docker镜像的描述文件，我们可以理解成火箭发射的A、B、C、D…的步骤。Dockerfile其内部**包含了一条条的指令**，**每一条指令构建一层，因此每一条指令的内容，就是描述该层应当如何构建**。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413002616.png)

**三步骤**

- 编写Dockerfile文件
- docker build
- docker run
## 2.DockerFile构建过程解析
### 2.1.Dockerfile内容基础知识
1. 每条保留字指令都必须为大写字母且后面要跟随至少一个参数
2. 指令按照从上到下，顺序执行
3. #表示注释
4. 每条指令都会创建一个新的镜像层，并对镜像进行提交
### 2.2.Docker执行Dockerfile的大致流程
1. docker从基础镜像运行一个容器
1. 执行一条指令并对容器作出修改
1. 执行类似docker commit的操作提交一个新的镜像层
1. docker再基于刚提交的镜像运行一个新容器
1. 执行dockerfile中的下一条指令直到所有指令都执行完成
### 2.3.小总结
从应用软件的角度来看，Dockerfile、Docker镜像与Docker容器分别代表软件的三个不同阶段
1. Dockerfile是软件的原材料
1. Docker镜像是软件的交付品
1. Docker容器则可以认为是软件的运行态。

Dockerfile面向开发，Docker镜像成为交付标准，Docker容器则涉及部署与运维，三者缺一不可，合力充当Docker体系的基石。

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413002243.png)

1. **Dockerfile**，需要定义一个Dockerfile，Dockerfile定义了进程需要的一切东西。Dockerfile涉及的内容包括执行代码或者是文件、环境变量、依赖包、运行时环境、动态链接库、操作系统的发行版、服务进程和内核进程(当应用进程需要和系统服务和内核进程打交道，这时需要考虑如何设计namespace的权限控制)等等;
2. **Docker镜像**，在用Dockerfile定义一个文件之后，docker build时会产生一个Docker镜像，当运行 Docker镜像时，会真正开始提供服务;
3. **Docker容器**，容器是直接提供服务的。
## 3.DockerFile体系结构(保留字指令)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413002545.png)

### 3.1.FROM
基础镜像，当前新镜像是基于哪个镜像的

每个Dockerfile的第一挑指令必须是FROM，FROM指令指定一个已存在的镜像，后续指令都继续该镜像进行，这个镜像也称为基础镜像

### 3.2.MAINTAINER
MAINTAINER指令没有具体的格式，建议一般使用姓名和邮箱

### 3.3.RUN
RUN 指令是用来执行命令行命令的。由于命令行的强大能力，RUN 指令在定制镜像时是最常用的指令之一。其格式有两种：

**shell 格式**：RUN <命令>，就像直接在命令行中输入的命令一样。如下：

```
RUN echo '<h1>Hello, Docker!</h1>' > /usr/share/nginx/html/index.html
```
**exec 格式**：RUN [“可执行文件”, “参数1”, “参数2”]，这更像是函数调用中的格式。

Dockerfile 中每一个指令都会建立一层，RUN 也不例外。每一个 RUN 的行为，都会新建立一层，在其上执行这些命令，执行结束后，commit 这一层的修改，构成新的镜像。

**Dockerfile 不推荐写法：**
```
FROM debian:stretch

RUN apt-get update
RUN apt-get install -y gcc libc6-dev make wget
RUN wget -O redis.tar.gz "http://download.redis.io/releases/redis-5.0.3.tar.gz"
RUN mkdir -p /usr/src/redis
RUN tar -xzf redis.tar.gz -C /usr/src/redis --strip-components=1
RUN make -C /usr/src/redis
RUN make -C /usr/src/redis install
RUN rm redis.tar.gz
```
上面的这种写法，创建了 8 层镜像。这是完全没有意义的，而且很多运行时不需要的东西，都被装进了镜像里，比如编译环境、更新的软件包等等。最后一行即使删除了软件包，那也只是当前层的删除；虽然我们看不见这个包了，但软件包却早已存在于镜像中并一直跟随着镜像，没有真正的删除。

结果就是产生非常臃肿、非常多层的镜像，不仅仅增加了构建部署的时间，也很容易出错。 这是很多初学 Docker 的人常犯的一个错误。

另外：Union FS 是有最大层数限制的，比如 AUFS，曾经是最大不得超过 42 层，现在是不得超过 127 层。

**Dockerfile 正确写法：**
```
FROM debian:stretch

RUN buildDeps='gcc libc6-dev make wget' \
    && apt-get update \
    && apt-get install -y $buildDeps \
    && wget -O redis.tar.gz "http://download.redis.io/releases/redis-5.0.3.tar.gz" \
    && mkdir -p /usr/src/redis \
    && tar -xzf redis.tar.gz -C /usr/src/redis --strip-components=1 \
    && make -C /usr/src/redis \
    && make -C /usr/src/redis install \
    && rm -rf /var/lib/apt/lists/* \
    && rm redis.tar.gz \
    && rm -r /usr/src/redis \
    && apt-get purge -y --auto-remove $buildDeps
```
这里没有使用很多个 RUN 对应不同的命令，而是仅仅使用一个 RUN 指令，并使用 && 将各个所需命令串联起来。将之前的 8 层，简化为了 1 层，且后面删除了不需要的包和目录。在撰写 Dockerfile 的时候，要经常提醒自己，这并不是在写 Shell 脚本，而是在定义每一层该如何构建。因此镜像构建时，一定要确保每一层只添加真正需要添加的东西，任何无关的东西都应该清理掉。

很多人初学 Docker 制作出了很臃肿的镜像的原因之一，就是忘记了每一层构建的最后一定要清理掉无关文件。

### 3.4.EXPOSE
当前容器对外暴露出的端口

### 3.5.WORKDIR
指定在创建容器后，终端默认登陆的进来工作目录，一个落脚点

可以用过-w标志在运行时覆盖工作目录

### 3.6.ENV
用来在构建镜像过程中设置环境变量

```
ENV MY_PATH /usr/mytest
```

这个环境变量可以在后续的任何RUN指令中使用，这就如同在命令前面指定了环境变量前缀一样；
也可以在其它指令中直接使用这些环境变量，

比如：`WORKDIR $MY_PATH`

### 3.7.ARG 构建参数
```
格式：ARG <参数名>[=<默认值>]
```
构建参数和 ENV 的效果一样，都是设置环境变量。所不同的是，ARG 所设置的构建环境的环境变量，在将来容器运行时是不会存在这些环境变量的。但是不要因此就使用 ARG 保存密码之类的信息，因为 docker history 还是可以看到所有值的。

Dockerfile 中的 ARG 指令是定义参数名称，以及定义其默认值。该默认值可以在构建命令 `docker build` 中用 `--build-arg <参数名>=<值>` 来覆盖。

在 1.13 之前的版本，要求 `--build-arg` 中的参数名，必须在 Dockerfile 中用 ARG 定义过了，换句话说，就是 --build-arg 指定的参数，必须在 Dockerfile 中使用了。如果对应参数没有被使用，则会报错退出构建。

从 1.13 开始，这种严格的限制被放开，不再报错退出，而是显示警告信息，并继续构建。这对于使用 CI 系统，用同样的构建流程构建不同的 Dockerfile 的时候比较有帮助，避免构建命令必须根据每个 Dockerfile 的内容修改。

### 3.8.ADD
将宿主机目录下的文件拷贝进镜像且ADD命令会自动处理URL和解压tar压缩包

在ADD文件时，Docker通过目的地址参数末尾的字符来判断文件源是目录还是文件。如果目标地址以/结尾那么Docker就认为源位置指向的是一个目录。如果目的地址不是以/结尾，那么Docker就认为原文件指向的是文件。

```
ADD jdk-8u91-linux-x64.tar.gz /opt
```

是将宿主机当前目录下的 `jdk-8u91-linux-x64.tar.gz` 拷贝到容器/opt目录下 ，容器的目标路径必须的绝对路径。

一般将Dockerfile与需要添加到容器中的文件放在同一目录下，有助于编写来源路径

### 3.9.COPY
格式：
```
COPY [--chown=<user>:<group>] <源路径>... <目标路径>
COPY [--chown=<user>:<group>] ["<源路径1>",... "<目标路径>"]
```

COPY 指令将从构建上下文目录中 <源路径> 的文件/目录复制到新的一层的镜像内的 <目标路径> 位置。如：
```
COPY package.json /usr/src/app/
```

<源路径> 可以是多个，甚至可以是通配符，其通配符规则要满足 Go 的 filepath.Match 规则，如：
```
COPY hom* /mydir/
COPY hom?.txt /mydir/
```
<目标路径> 可以是容器内的绝对路径，也可以是相对于工作目录的相对路径（工作目录可以用 WORKDIR 指令来指定）。目标路径不需要事先创建，如果目录不存在会在复制文件前先行创建缺失目录。

此外，还需要注意一点，使用 COPY 指令，源文件的各种元数据都会保留。比如读、写、执行权限、文件变更时间等。这个特性对于镜像定制很有用。特别是构建相关文件都在使用 Git 进行管理的时候。

在使用该指令的时候还可以加上 --chown=<user>:<group> 选项来改变文件的所属用户及所属组。
```
COPY --chown=55:mygroup files* /mydir/
COPY --chown=bin files* /mydir/
COPY --chown=1 files* /mydir/
COPY --chown=10:11 files* /mydir/
```

### 3.10.VOLUME
容器数据卷，用于数据保存和持久化工作

### 3.11.CMD
指定一个容器启动时要运行的命令

ENTRYPOINT 的目的和 CMD 一样，都是在指定容器启动程序及参数

CMD指令用于执行容器提供默认值。每个Dockerfile只有一个CMD命令，如果指定了多个CMD命令，那么只有最后一个会被执行。

Docker run 命令可以覆盖CMD指令。如果在Dockerfile里指定了CMD指令，而同时在docker run命令行中也指定的要运行的命令，命令行中指定的命令会覆盖Dockerfile中的CMD指令。

如 `CMD java app.jar` 容器启动时启动app.jar应用

### 3.12.ENTRYPOINT
指定一个容器启动时要运行的命令

ENTRYPOINT 的目的和 CMD 一样，都是在指定容器启动程序及参数

**区别：**

- 可以在docker run 命令中覆盖CMD命令。

- ENTRYPOINT指令提供的命令则不容易在启动容器时被覆盖。实际上docker run命令中指定的任何参数会被当做参数再次传递给ENTRYPOINT指令中指定的命令。

### 3.13.ONBUILD
当构建一个被继承的Dockerfile时运行命令，父镜像在被子继承后父镜像的onbuild被触发

### 3.14.USER
指定该镜像会以什么用户运行

### 3.15.小结
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413002333.png)

## 4.案例
### 4.1.使用Dockerfile构建Java镜像入门
**step 1 新建Dockerfile文件**

```
touch Dokcerfile
```
**step 2 编写Dockerfile脚本**

```
FROM centos:latest
MAINTAINER "NIU GANG"<863263957@qq.com>
# 在opt目录下新建apps目录
RUN  mkdir -p /opt/apps
# 在当前目录下及和Dockerfile在一个目录下,将jdk安装包增加到镜像的opt/apps目录下
# ADD目录会自动将 .tar.gz包进行解压
ADD jdk-8u211-linux-x64.tar.gz /opt/apps
# 在启动容器是执行命令
CMD /opt/apps/jdk1.8.0_211/bin/java -version
```
**step 3 构建镜像**

```
[root@localhost tools]# docker build -t niugang/java .
Sending build context to Docker daemon  197.8MB
Step 1/5 : FROM centos:latest
 ---> 9f38484d220f
Step 2/5 : MAINTAINER "NIU GANG"<863263957@qq.com>
 ---> Running in e4a4a0c1ffa9
Removing intermediate container e4a4a0c1ffa9
 ---> a7f400aae1a7
Step 3/5 : RUN  mkdir -p /opt/apps
 ---> Running in 49f2c1f076c0
Removing intermediate container 49f2c1f076c0
 ---> 287c44707477
Step 4/5 : ADD jdk-8u211-linux-x64.tar.gz /opt/apps
 ---> cf2dfbb373c8
Step 5/5 : CMD /opt/apps/jdk1.8.0_211/bin/java -version
 ---> Running in 83d41700285a
Removing intermediate container 83d41700285a
 ---> 79bf24642eb4
Successfully built 79bf24642eb4
Successfully tagged niugang/java:latest
```
对于 `docker build -t niugang/java` . 这个命令-t 选项指定镜像名称 并读取当前（即.）目录中的Dockerfile文件

**step 4 验证**

```
[root@localhost tools]# docker run --rm niugang/java
java version "1.8.0_211"
Java(TM) SE Runtime Environment (build 1.8.0_211-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.211-b12, mixed mode)
```
--rm 是启动并删除容器

注意：

最开始在启动 `docker run --rm niugang/java` 会报 `/bin/sh: /opt/apps/jdk-8u211/bin/java: No such file or directory`。是因为当初认为ADD 增加 jdk-8u211-linux-x64.tar.gz 后解压的名称叫 jdk-8u211(感觉犯例了一个常识性的错误)，但其实解压完成后的文件夹名称为1.8.0_211

### 4.2.自定义tomcat9镜像
**step1 在根目录下新建mytomcat9文件夹**

```
mkdir mytomcat9
```
**step2 上传jdk tomcat安装包**

**step3 新建Dockerfile文件**

```
touch Dockerfile
```
**step4 编写Dockerfile 内容如下**

```
FROM         centos
MAINTAINER    niugang<863263957@qq.com>
#把宿主机当前上下文的test.txt拷贝到容器/usr/local/路径下并重命名为test1.txt
COPY test.txt /usr/local/test1.txt
#把java与tomcat添加到容器中
ADD jdk-8u211-linux-x64.tar.gz /usr/local/
ADD apache-tomcat-9.0.27.tar.gz /usr/local/
#安装vim编辑器
RUN yum -y install vim
#设置工作访问时候的WORKDIR路径，登录落脚点
ENV MYPATH /usr/local
WORKDIR $MYPATH
#配置java与tomcat环境变量
ENV JAVA_HOME /usr/local/jdk1.8.0_211
ENV CLASSPATH $JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
ENV CATALINA_HOME /usr/local/apache-tomcat-9.0.27
ENV CATALINA_BASE /usr/local/apache-tomcat-9.0.27
ENV PATH $PATH:$JAVA_HOME/bin:$CATALINA_HOME/lib:$CATALINA_HOME/bin
#容器运行时监听的端口
EXPOSE  8080
#启动时运行tomcat
# ENTRYPOINT ["/usr/local/apache-tomcat-9.0.27/bin/startup.sh" ]
# CMD ["/usr/local/apache-tomcat-9.0.27/bin/catalina.sh","run"]
CMD /usr/local/apache-tomcat-9.0.27/bin/startup.sh && tail -F /usr/local/apache-tomcat-9.0.27/bin/logs/catalina.out
```
**step5 构建镜像**

```
docker build -t niugang/tomcat9 .
```
**step6 运行容器**

```
docker run -d -p 9080:8080 --name myt9  -v /mytomcat9/mywebapp/:/usr/local/apache-tomcat-9.0.27/webapps   -v /mytomcat9/tomcat9logs/:/usr/local/apache-tomcat-9.0.27/logs niugang/tomcat9 --privileged=true
```
上述运行容器创建两个数据卷
- 第一个是将容器中的tomcat日志映射到宿主机上
- 第二个是 将tomcat中的webapps目录映射到宿主机上方便日后发布应用
也可以将容器中tomcat的配置conf文件夹映射到宿主机

**step7 验证应用发布**

在映射的/mytomcat9/mywebapp/ 新建test目录，test目录下新建index.html,写入helle world。
```
[root@localhost test]# pwd
/mytomcat9/mywebapp/test
[root@localhost test]# ll
总用量 4
-rw-r--r--. 1 root root 12 10月 23 21:30 index.html
```
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413002454.png)

