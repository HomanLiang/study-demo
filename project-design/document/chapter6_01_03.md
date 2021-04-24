[toc]



# Jenkins

## 1.如何安装Jenkins？

`Jenkins`是Java开发的一套工具，可以直接下载`war`包部署在`Tomcat`上，但是今天作者用最方便、最流行的`Docker`安装。

### 1.1.环境准备

在开始安装之前需要准备以下环境和工具：

1. 一台服务器，当然没有的话可以用自己的电脑，作者的服务器型号是`Ubuntu`。
2. `JDK`环境安装，作者的版本是`1.8`，至于如何安装，网上很多教程。
3. 准备`maven`环境，官网下载一个安装包，放在指定的目录下即可。
4. `Git`环境安装，网上教程很多。
5. 代码托管平台，比如`Github`、`GitLab`等。

### 1.2.开始安装Jenkins

`Docker`安装`Jenkins`非常方便，只要跟着作者的步骤一步步操作，一定能够安装成功。

#### 1.2.1.Docker环境安装

每个型号服务器安装的方式各不相同，读者可以根据自己的型号安装，网上教程很多。

#### 1.2.2.拉取镜像

我这里安装的版本是`jenkins/jenkins:2.222.3-centos`，可以去这里获取你需要的版本: `https://hub.docker.com/_/jenkins?tab=tags`。执行如下命令安装：

```
docker pull jenkins/jenkins:2.222.3-centos
```

#### 1.2.3.创建本地数据卷

在本地创建一个数据卷挂载docker容器中的数据卷，我创建的是`/data/jenkins_home/`，命令如下：

```
 mkdir -p /data/jenkins_home/
```

需要修改下目录权限，因为当映射本地数据卷时，`/data/jenkins_home/`目录的拥有者为`root`用户，而容器中`jenkins`用户的 `uid` 为 `1000`。

```
chown -R 1000:1000 /data/jenkins_home/
```

#### 1.2.4.创建容器

除了需要挂载上面创建的`/data/jenkins_home/`以外，还需要挂载`maven`、`jdk`的根目录。启动命令如下：

```
docker run -d --name jenkins -p 8040:8080 -p 50000:50000 -v /data/jenkins_home:/var/jenkins_home -v /usr/local/jdk:/usr/local/jdk -v /usr/local/maven:/usr/local/maven jenkins/jenkins:2.222.3-centos
```

以上命令解析如下：

1. `-d`：后台运行容器
2. `--name`：指定容器启动的名称
3. `-p`：指定映射的端口，这里是将服务器的`8040`端口映射到容器的`8080`以及`50000`映射到容器的`50000`。**「注意：」** `8040`和`50000`一定要是开放的且未被占用，如果用的是云服务器，还需要在管理平台开放对应的规则。
4. `-v`：挂载本地的数据卷到`docker`容器中，**「注意：」** 需要将`JDK`和`maven`的所在的目录挂载。

## 2.初始化配置

容器启动成功，则需要配置`Jenkins`，安装一些插件、配置远程推送等等。

### 2.1.访问首页

容器创建成功，访问`http://ip:8040`，如果出现以下页面表示安装成功：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164238.png)

### 2.2.输入管理员密码

启动成功，则会要求输入密码，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164243.webp)

这里要求输入的是管理的密码，提示是在`/var/jenkins_home/secrets/initialAdminPassword`，但是我们已经将`/var/jenkins_home`这个文件夹挂载到本地目录了，因此只需要去挂载的目录`/data/jenkins_home/secrets/initialAdminPassword`文件中找。

输入密码，点击继续。

### 2.3.安装插件

初始化安装只需要安装社区推荐的一些插件即可，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164250.png)

这里选择`安装推荐的插件`，然后 `Jenkins` 会自动开始安装。

**「注意：」** 如果出现想插件安装很慢的问题，找到`/data/jenkins_home/updates/default.json`文件，替换的内容如下：

1. 将 `updates.jenkins-ci.org/download` 替换为`mirrors.tuna.tsinghua.edu.cn/jenkins`
2. 将 `www.google.com` 替换为`www.baidu.com`。

执行以下两条命令：

```
sed -i 's/www.google.com/www.baidu.com/g' default.json

sed -i 's/updates.jenkins-ci.org\/download/mirrors.tuna.tsinghua.edu.cn\/jenkins/g' default.json
```

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164255.webp)

全部安装完成，继续下一步。

### 2.4.创建管理员

随便创建一个管理员，按要求填写信息，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164301.png)

### 2.5.实例配置

配置自己的服务器`IP`和`端口`，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164306.webp)

### 2.6.配置完成

按照以上步骤，配置完成后自动跳转到如下界面：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164310.png)

## 3.构建Spring Boot 项目

在构建之前还需要配置一些开发环境，比如`JDK`，`Maven`等环境。

### 3.1.配置JDK、maven、Git环境

`Jenkins`集成需要用到`maven`、`JDK`、`Git`环境，下面介绍如何配置。

首先打开`系统管理`->`全局工具配置`，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164315.webp)

分别配置`JDK`，`Git`，`Maven`的路径，根据你的实际路径来填写。

**「注意」**：这里的`JDK`、`Git`、`Maven`环境一定要挂载到`docker`容器中，否则会出现以下提示：

```
 xxxx is not a directory on the Jenkins master (but perhaps it exists on some agents)
```

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164320.png)

![图片](https://mmbiz.qpic.cn/mmbiz_png/19cc2hfD2rBVwQwU5fZH6BgqIJJ95DWj6PhW2BEM2YzgcDrXPcqC3N3cOxya6D3FichQa4Bz0YLEXiaPccSccuibA/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

![图片](https://mmbiz.qpic.cn/mmbiz_png/19cc2hfD2rBVwQwU5fZH6BgqIJJ95DWjGv7tqCeBeA74iclYSdjDCJWoDian4jEKia1DacKk6tJ9KczCPkIaMazBg/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

配置成功后，点击保存。

### 3.2.安装插件

除了初始化配置中安装的插件外，还需要安装如下几个插件：

1. `Maven Integration`
2. `Publish Over SSH`

打开`系统管理` -> `插件管理`，选择`可选插件`，勾选中 `Maven Integration` 和 `Publish Over SSH`，点击`直接安装`。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164325.webp)

在安装界面勾选上安装完成后重启 `Jenkins`。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164329.png)

### 3.3.添加 SSH Server

`SSH Server` 是用来连接部署服务器的，用于在项目构建完成后将你的应用推送到服务器中并执行相应的脚本。

打开 `系统管理` -> `系统配置`，找到 `Publish Over SSH` 部分，选择`新增`

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164334.png)

点击 `高级` 展开配置

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164338.webp)

最终配置如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164342.png)

配置完成后可点击 `Test Configuration` 测试连接，出现 `success` 则连接成功。

### 3.4.添加凭据

凭据 是用来从 `Git` 仓库拉取代码的，打开 `凭据` -> `系统` -> `全局凭据` -> `添加凭据`

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164348.png)

这里配置的是`Github`，直接使用`用户名`和`密码`，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164352.png)

创建成功，点击保存。

### 3.5.新建Maven项目

以上配置完成后即可开始构建了，首先需要新建一个`Maven`项目，步骤如下。

#### 3.5.1.创建任务

首页点击`新建任务`->`构建一个maven项目`，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164401.webp)

#### 3.5.2.源码管理

在源码管理中，选择`Git`，填写`仓库地址`，选择之前添加的`凭证`。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164407.png)

#### 3.5.3.构建环境

勾选 `Add timestamps to the Console Output`，代码构建的过程中会将日志打印出来。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164412.png)

#### 3.5.4.构建命令

在`Build`中，填写 `Root POM` 和 `Goals and options`，也就是你构建项目的命令。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164416.png)

#### 3.5.5.Post Steps

选择`Run only if build succeeds`，添加 `Post` 步骤，选择 `Send files or execute commands over SSH`。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164420.webp)

上图各个选项解析如下：

1. `name`:选择前面添加的`SSH Server`
2. `Source files`:要推送的文件
3. `Remove prefix`:文件路径中要去掉的前缀，
4. `Remote directory`:要推送到目标服务器上的哪个目录下
5. `Exec command`:目标服务器上要执行的脚本

`Exec command`指定了需要执行的脚本，如下：

```
# jdk环境，如果全局配置了，可以省略
export JAVA_HOME=/xx/xx/jdk
export JRE_HOME=/xx/xx/jdk/jre
export CLASSPATH=/xx/xx/jdk/lib
export PATH=$JAVA_HOME/bin:$JRE_HOME/bin:$PATH
 
# jenkins编译之后的jar包位置，在挂载docker的目录下
JAR_PATH=/data/jenkins_home/workspace/test/target
# 自定义的jar包位置
DIR=/data/test

## jar包的名称
JARFILE=swagger-demo-0.0.1-SNAPSHOT.jar

if [ ! -d $DIR/backup ];then
   mkdir -p $DIR/backup
fi

ps -ef | grep $JARFILE | grep -v grep | awk '{print $2}' | xargs kill -9

if [ -f $DIR/backup/$JARFILE ]; then
 rm -f $DIR/backup/$JARFILE
fi

mv $JAR_PATH/$JARFILE $DIR/backup/$JARFILE


java -jar $DIR/backup/$JARFILE > out.log &
if [ $? = 0 ];then
        sleep 30
        tail -n 50 out.log
fi

cd $DIR/backup/
ls -lt|awk 'NR>5{print $NF}'|xargs rm -rf
```

以上脚本大致的意思就是将`kill`原有的进程，启动新构建`jar`包。

> 脚本可以自己定制，比如备份`Jar`等操作。

## 4.构建任务

项目新建完成之后，一切都已准备就绪，点击`立即构建`可以开始构建任务，控制台可以看到`log`输出，如果构建失败，在`log`中会输出原因。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164425.png)

任务构建过程会执行脚本启动项目。

## 5.如何构建托管在GitLab的项目？

上文介绍的例子是构建`Github`仓库的项目，但是企业中一般都是私服的`GitLab`，那么又该如何配置呢？

其实原理是一样的，只是在构建任务的时候选择的是`GitLab`的凭据，下面将详细介绍。

### 5.1.安装插件

在`系统管理`->`插件管理`->`可选插件`中搜索`GitLab Plugin`并安装。

### 5.2.添加GitLab API token

首先打开 `凭据` -> `系统` -> `全局凭据` -> `添加凭据`，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164430.png)

上图中的`API token`如何获取呢？

打开`GitLab`（例如公司内网的`GitLab`网站），点击个人设置菜单下的`setting`，再点击`Account`，复制`Private token`，如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164434.png)

上图的`Private token`则是`API token`，填上即可。

### 5.3.配置GitLab插件

打开`系统管理`->`系统配置`->`GitLab`，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164438.png)

配置成功后，点击`Test Connection`，如果提示`Success`则配置成功。

### 5.4.新建任务

新建一个Maven任务，配置的步骤和上文相同，唯一区别则是配置`Git`仓库地址的地方，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164442.png)

仓库地址和凭据需要填写`Gitlab`相对应的。

### 5.5.后续操作

后续一些操作，比如构建项目，控制台输出等操作，都是和`GitHub`操作相同，不再赘述了。

## 6.多模块项目如何构建？

如果你的多模块不是通过私服仓库依赖的，那么在构建打包是有先后顺序的，在新建任务的时候需要配置`Build`的`maven`命令，如下图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424164446.png)

上图中的`Goals and options`中的命令就是构建`api`这个模块的命令，至于这个命令是什么意思，前面有单独一篇文章介绍过，请看[一次打包引发的思考，原来maven还能这么玩~](https://mp.weixin.qq.com/s?__biz=MzU3MDAzNDg1MA==&mid=2247485752&idx=1&sn=615f97bd9d161a87f309261c665397b4&scene=21#wechat_redirect)。

## 7.Jenkins 环境变量

### 7.1.认识 Jenkins 环境变量

> Jenkins 环境变量就是通过 `env` 关键字暴露出来的**全局变量**，可以在 Jenkins 文件的**任何位置使用**

其实和你使用的编程语言中的全局变量没有实质差别

#### 7.1.1.查看 Jenkins 系统内置环境变量

Jenkins 在系统内置了很多环境变量方便我们快速使用，查看起来有两种方式：

**方式一：**

直接在浏览器中访问 `${YOUR_JENKINS_HOST}/env-vars.html` 页面就可以，比如 `http://localhost:8080/env-vars.html` ，每个变量的用途写的都很清楚

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165722.png)

**方式二**

通过执行 `printenv` shell 命令来获取：

```groovy
pipeline {
    agent any

    stages {
        stage("Env Variables") {
            steps {
                sh "printenv"
            }
        }
    }
}
```

直接 Save - Build, 在终端 log 中你会看到相应的环境变量，并且可以快速看到他们当前的值

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165715.png)

通常这两种方式可以结合使用

#### 7.1.2.读取环境变量

上面我们说了 `env` 是环境变量的关键字，但是读取 Jenkins 内置的这些环境变量，`env` 关键字是可有可无, 但不能没了底裤，都要使用 `${xxx}` 包围起来。以 `BUILD_NUMBER` 这个内置环境变量举例来说明就是这样滴：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165711.png)

如果你在 Jenkins 文件中使用 shell 命令，使用这些内置环境变量甚至可以不用 `{}`， 来看一下：

```groovy
pipeline {
    agent any

    stages {
        stage("Read Env Variables") {
            steps {
                echo "带 env 的读取方式：${env.BUILD_NUMBER}"
                echo "不带 env 的读取方式：${BUILD_NUMBER}"
                sh 'echo "shell 中读取方式 $BUILD_NUMBER"'
            }
        }
    }
}
```

可以看到结果是一样一样滴，**不管有几种，记住第一种最稳妥**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165706.png)

内置的环境变量虽好，但也不能完全满足我们自定义的 pipeline 的执行逻辑，所以我们也得知道如何定义以及使用自定义环境变量

### 7.2.自定义 Jenkins 环境变量

Jenkins pipeline 分声明式（Declarative）和 脚本式（imperative）写法，相应的环境变量定义方式也略有不同，归纳起来有三种方式：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165701.png)

还是看个实际例子吧：

```groovy
pipeline {
    agent any

    environment {
        FOO = "bar"
    }

    stages {
        stage("Custom Env Variables") {
            environment {
                NAME = "RGYB"
            }

            steps {
                echo "FOO = ${env.FOO}"
                echo "NAME = ${env.NAME}"

                script {
                    env.SCRIPT_VARIABLE = "Thumb Up"
                }

                echo "SCRIPT_VARIABLE = ${env.SCRIPT_VARIABLE}"

                withEnv(["WITH_ENV_VAR=Come On"]) {
                    echo "WITH_ENV_VAR = ${env.WITH_ENV_VAR}"
                }
            }
        }
    }
}
```

来看运行结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165656.png)

> 注意：`withEnv(["WITH_ENV_VAR=Come On"]) {}` **这里的 = 号两侧不能有空格**，必须是 `key=value` 的形式

一个完整的 pipeline 通常会有很多个 stage，环境变量在不同的 stage 有不同的值是很常见的，知道如何设置以及读取环境变量后，我们还得知道如何重写环境变量

### 7.3.重写 Jenkins 环境变量

Jenkins 让人相对困惑最多的地方就是重写环境变量，但是只要记住下面这三条规则，就可以搞定一切了

1. `withEnv(["WITH_ENV_VAR=Come On"]) {}` 内置函数的这种写法，可以重写任意环境变量
2. 定义在 `environment {}` 的环境变量不能被脚本式定义的环境变量（`env.key="value"`）重写
3. 脚本式环境变量只能重写脚本式环境变量

这三点是硬规则，没涵盖在这 3 点规则之内的也就是被允许的了

三条规则就有点让人头大了，农夫选豆种，举例为证吧

```groovy
pipeline {
    agent any

    environment {
        FOO = "你当像鸟飞往你的山"
        NAME = "Tan"
    }

    stages {
        stage("Env Variables") {
            environment {
              	// 会重写第 6 行 变量
                NAME = "RGYB" 
              	// 会重写系统内置的环境变量 BUILD_NUMBER
                BUILD_NUMBER = "10" 
            }

            steps {
              	// 应该打印出 "FOO = 你当像鸟飞往你的山"
                echo "FOO = ${env.FOO}" 
              	// 应该打印出 "NAME = RGYB"
                echo "NAME = ${env.NAME}" 
              	// 应该打印出 "BUILD_NUMBER = 10"
                echo "BUILD_NUMBER =  ${env.BUILD_NUMBER}" 

                script {
                  	// 脚本式创建一个环境变量
                    env.SCRIPT_VARIABLE = "1" 
                }
            }
        }

        stage("Override Variables") {
            steps {
                script {
                  	// 这里的 FOO 不会被重写，违背 Rule No.2
                    env.FOO = "Tara"
                  	// SCRIPT_VARIABLE 变量会被重写，符合 Rule No.3
                    env.SCRIPT_VARIABLE = "2" 
                }

              	// FOO 在第 37 行重写失败，还会打印出 "FOO = 你当像鸟飞往你的山"
                echo "FOO = ${env.FOO}" 
              	// 会打印出 "SCRIPT_VARIABLE = 2"
                echo "SCRIPT_VARIABLE = ${env.SCRIPT_VARIABLE}" 

              	// FOO 会被重写，符合 Rule No.1
                withEnv(["FOO=Educated"]) { 
                  	// 应该打印 "FOO = Educated"
                    echo "FOO = ${env.FOO}" 
                }

              	// 道理同上
                withEnv(["BUILD_NUMBER=15"]) {
                  	// 应该打印出 "BUILD_NUMBER = 15"
                    echo "BUILD_NUMBER = ${env.BUILD_NUMBER}"
                }
            }
        }
    }
}
```

来验证一下结果吧

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165644.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165639.png)

看到这，基本的设置应该就没有什么问题了，相信你也发现了，Jenkins 设置环境变量和编程语言的那种设置环境变量还是略有不同的，后者可以将变量赋值为对象，但 Jenkins 就不行，因为**在 Jenkins 文件中，所有设置的值都会被当成 String**， 难道没办法应用 Boolean 值吗？

### 7.4.Jenkins 中使用 Boolean 值

如果设置一个变量为 `false` ，Jenkins 就会将其转换为 `"false"`, 如果想使用 Boolean 来做条件判断，必须要调用 `toBoolean()` 方法做转换

```groovy
pipeline {
    agent any

    environment {
        IS_BOOLEAN = false
    }

    stages {
        stage("Env Variables") {
            steps {
                script {
                  	// Hello 会被打印出来，因为非空字符串都会被认为是 Boolean.True
                    if (env.IS_BOOLEAN) {
                        echo "Hello"
                    }

                  	// 真正的 Boolean 比较
                    if (env.IS_BOOLEAN.toBoolean() == false) {
                        echo "日拱一兵"
                    }
                  
                  	// 真正的 Boolean 
                    if (!env.IS_BOOLEAN.toBoolean()) {
                        echo "RGYB"
                    }
                }
            }
        }
    }
}
```

来看运行结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165632.png)

如果你写过 Pipeline，你一定会知道，写 Pipeline 是离不开写 shell 的，有些时候，需要将 shell 的执行结果赋值给环境变量，Jenkins 也有方法支持

### 7.5.Shell 结果赋值给环境变量

实现这种方式很简单，只需要记住一个格式：`sh(script: 'cmd', returnStdout:true)`

```groovy
pipeline {
    agent any

    environment {
      	// 使用 trim() 去掉结果中的空格
        LS_RESULT = "${sh(script:'ls -lah', returnStdout: true).trim()}"
    }

    stages {
        stage("Env Variables") {
            steps {
                echo "LS_RESULT = ${env.LS_RESULT}"
            }
        }
    }
}
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424165625.png)







































