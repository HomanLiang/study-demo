[toc]



# Docker Compose

## 1.容器之间通信
### 1.1.单向通信
#### 1.1.1.什么意思
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413211425.png)

mysql和tomcat是两个独立的容器，但是tomcat需要和mysql通信，而mysql完全不用和tomcat通信，这就叫容器之间的单向通信。

#### 1.1.2.怎么通信
要谈通信，就需要谈一下ip，因为不知道ip是无法通信的。最简单的例子你jdbc要连接mysql数据库，你也需要配置mysql的ip地址。容器之间也不例外，都是靠虚拟ip来完成的。

何为虚拟ip？
虚拟ip：容器创建完成后都会生成一个唯一的ip，这个ip外界不能直接访问，他只用于容器之间进行通信交互用。这就是虚拟ip。

容器之间的虚拟ip是互通的。

通信是什么意思、靠什么通信我们都知道了，那还不抓紧实战一把？

#### 1.1.3.实战演示
1. 创建tomcat容器

    ```
    docker run -d --name mytomcat tomcat
    # --name指定的名称再docker ps里是可以看到的，最后一列Name
    docker ps
    ```
    
    知识点出现了！！！--name是神马鬼？先看如下一个场景
    
    在公司或者你直接买的阿里云数据库/redis等服务为什么给你个数据库域名而不是推荐用ip？因为ip的话可变，比如你业务系统写死了ip，这时候人家那边内网ip变了，你这所有用这个数据库的业务系统都要跟着改。用域名的话一劳永逸，底层ip变了后再映射到新域名上就行，不影响业务系统。
    
    --name就是给docker配置名称来与虚拟ip做映射，因为ip老变化，每次变化的时候其他容器都需要跟着改动才行。配个名称一劳永逸。创建容器的时候通过 --name xxx 即可指定。
    
2. 创建mysql容器

	我也没拉取mysql镜像，就用centos模拟一下数据库吧，主要是看能不能ping 通，能ping 通就代表能通信。

    ```
    docker run -d --name database -it centos /bin/bash
    ```

3. 小试一把

      我们需要进入mytomcat的容器然后去ping database的ip看看是否通，那么容器的虚拟ip怎么查呢？

    ```
    # 这又是一个知识点
    docker inspect 容器id
    # 比如：（9bf58b4014dd是我们database的容器id）
    docker inspect 9bf58b4014dd
    ```
    
    ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413211702.png)
    
    现在知道数据库的ip了，那赶紧进入我们的mytomcat的容器去ping一波

    ```
    docker exec -it mytomcat /bin/bash
    ping 172.17.0.6
    ```
    
    ![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413211731.png)
    
    完美！
    
    等等，貌似不是很完美，我们给数据库指定了名称database，那我们赶紧ping database 试一下。结果啪啪啪打脸，完全不通，那是因为相当于你就起了个名字，并没有做映射。那怎么映射呢？mytomcat启动容器的时候指定一个--link参数即可。

    ```
    # 强制删除老的
    docker rm -f mytomcat
    # 创建新的容器，用--link指定我们想连的配置的数据库“域名”
    docker run -d --name mytomcat --link database tomcat
    # 进入mytomcat容器
    docker exec -it mytomcat /bin/bash
    # ping
    ping database
    ```
    
    ![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413211802.png)
    
    这次是真的完美~！

#### 1.1.4.总结
1. 容器简单虚拟ip是互通的

1. 用--name 和 --link可以完成自定义“域名”来取代可变化的ip

### 1.2.双向通信
方式有很多，一般都采取桥接方式。由于篇幅过长，自行Google即可。重点搞懂了容器间的通信是什么意思，大概怎么做即可。比如上面的--link也是其一做法。

## 2.容器间数据共享
### 2.1.场景
需要宿主机和容器之间共享数据的业务场景。比如Mysql的

比如：集群部署的时候，我们的应用程序需要部署到10个docker容器里，那么比如要想改动一个文件的内容，就要重新打包然后部署10次。我们可以将我们需要部署的应用程序挂载到宿主机上，这样改一处就行了。比如静态html文件，再一个宿主机上启动了10个容器，这时候需求需要改文案（修改html），我们需要修改10个容器里的html，过于麻烦，所以可以把这个html挂载到宿主机，容器直接使用挂载到宿主机的文件即可。

再比如：Mysql的数据目录可配置文件（一些高级配置或者优化配置啥的肯定要用一份），这也可以用此场景。

### 2.2.语法
```
# 语法
docker run -v 宿主机路径:容器内挂载路径 镜像名
# 比如如下：他会把/home/main/programe下面的所有目录都挂载到容器的/usr/local/tomcat/webapps下
docker run -v /home/main/programe:/usr/local/tomcat/webapps tomcat
```

### 2.3.实战
1. 准备

	在如下目录里创建如下文件，并写上Hello Volumn~

    ```
   /home/main/docker/webapps/volumn-test/index.html
    ```

2. 操作

	很简单，按照上面的语法来就成了，如下就是将 `/home/main/docker/webapps` 下的目录挂载到容器部 `/usr/local/tomcat/webapps` 的目录下

    ```
   docker run --name t2 -d -p 8200:8080 -v /home/main/docker/webapps:/usr/local/tomcat/webapps tomcat
    ```

3. 验证

	我们先进入容器

    ```
   docker exec -it t2 /bin/bash
    ```

	然后查看/usr/local/tomcat/webapps下是否有我们挂载的目录以及文件

    ```
   root@4be396ff443b:/usr/local/tomcat/webapps# ls -R volumn-test/
   volumn-test/:
   index.html
    ```

	最后我们访问下看看效果

    ```
   [root@izm5 volumn-test]# curl 'localhost:8200/volumn-test/index.html'
   Hello Volumn~~
    ```

	我们修改下宿主机上的 `index.html` 的内容，修改为 `Hello Volumn~~ How are you?` 然后再次访问看效果：

	这里修改的是宿主机 `/home/main/docker/webapps/volumn-test` 下的index.html，为不是容器内部的。

    ```
    [root@izm5 volumn-test]# curl 'localhost:8200/volumn-test/index.html'
    Hello Volumn~~ How are you?
    ```

	很完美，容器无感知的就生效了。

4. 好处

	我这是启动了一个容器举例，如果多启动几个呢？然后产品要修改文案，那么你登录每个容器里去修改？或者重新打包然后重新启动所有容器？有点小题大做呀，利用-v命令进行挂载实现宿主机和容器的数据共享她不香吗？

### 2.4.新的问题
如果容器过多，那么每次启动容器都要-v xxx:xxx，这也很容易写错啊，写错一个字母都不行，还有，如果宿主机换地址了，这也需要批量更换容器的docker run -v的参数，机器太多不利于维护。

### 2.5.解决问题
共享容器诞生了！
#### 2.5.1.共享容器概念
如果容器太多，每一次都要写-v xxx:xxx，过于复杂，也容易出错，这时候可以通过docker create创建共享容器，然后启动的时候通过--volumes-from指定创建的共享容器名称即可，也就是说可以把上面-v xxx:xxx这一串统一放到一个地方去管理，容器启动的时候直接引用这个统一配置即可，方便统一管理。
#### 2.5.2.语法
```
# 创建共享容器语法，只是创建不是启动。最后的/bin/true 就是一个占位符，没啥乱用。
docker create --name 共享容器名称 -v 宿主机路径:容器内挂载路径 镜像名称 /bin/true
# 启动容器的时候通过--volumes-from 共享容器名称来使用共享容器挂载点
docker run --volumes-from 共享容器名称 --name xxx -d 镜像名称
```
#### 2.5.3.实战
```
# 创建共享容器
docker create --name webpage -v /home/main/docker/webapps:/usr/local/tomcat/webapps tomcat /bin/true
# 采取共享容器的配置来启动容器
docker run -p 8300:8080 --volumes-from webpage --name t3 -d tomcat
# 在启动个
docker run -p 8400:8080 --volumes-from webpage --name t4 -d tomcat
```
#### 2.5.4.验证&&好处
验证跟第一种-v的方式一样，修改内容，容器无感知。

相对于第一种方式的好处是：
- 不用每次都写-v xxx:xxx这一长串不仅令人厌恶还容易出现错误的英文字母。
- 更改路径，只修改一处即可。

## 3.Docker Compose
### 3.1.有什么用
比如我们要部署一个javaweb应用，那一般情况都需要三个容器：nginx容器、tomcat容器、mysql容器。这是最基本的，可能更复杂。那运维人员每次都需要单独启动这三个容器来支撑我们的web应用吗？有点复杂了。

docker-compose就是为了简化这个过程的，相当于是个脚本，把这三个容器用脚本统一来管理和启动。节省运维时间和避免出错率。也就是说多应用互相协同才能完成一件事的时候，是很好用的，否则直接Dockerfile就完了。
### 3.2.安装Docker Compose
> 基于Linux的安装。
> 参考的官方安装文档：https://docs.docker.com/compose/install/

执行如下两个命令就完事了:
```
sudo curl -L "https://github.com/docker/compose/releases/download/1.25.5/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

sudo chmod +x /usr/local/bin/docker-compose
```
如果第一个命令特别慢的话可以用如下命令代替，毕竟是国外网站
```
> curl -L https://get.daocloud.io/docker/compose/releases/download/1.25.5/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
```
安装完进行验证：
```
docker-compose --version
```
### 3.3.实战感受一下
先来感受一下docker-compose的威力，部署下WordPress来玩玩。不知道WordPress是啥的自己百度，就是一个开源博客。

为什么是部署WordPress，因为官方也是WordPress….面向官方文档学习…

官方文档安装WordPress的教程：https://docs.docker.com/compose/wordpress/

1. 建立如下目录

    ```
    /home/main/docker/WordPress
    ```

2. 建立docker-compose.yml文件

    ```
    cd /home/main/docker/WordPress
    vi docker-compose.yml
    ```

3. 在docker-compose.yml里写上如下内容

    ```
    version: '3.3'

    services:
       db:
         image: mysql:5.7
         volumes:
           - db_data:/var/lib/mysql
         restart: always
         environment:
           MYSQL_ROOT_PASSWORD: somewordpress
           MYSQL_DATABASE: wordpress
           MYSQL_USER: wordpress
           MYSQL_PASSWORD: wordpress

       wordpress:
         depends_on:
           - db
         image: wordpress:latest
         ports:
           - "8000:80"
         restart: always
         environment:
           WORDPRESS_DB_HOST: db:3306
           WORDPRESS_DB_USER: wordpress
           WORDPRESS_DB_PASSWORD: wordpress
           WORDPRESS_DB_NAME: wordpress
    volumes:
        db_data: {}
    ```

> 看不懂？正常，也没关系。这就是大名鼎鼎的docker-compose，就是一个一.yml为后缀结尾的脚本文件。他能自动帮我们部署我们配置的容器，比如上述有mysql容器和wordpress容器。我们还能看到端口是8000，这就够了。开干！

4. 执行脚本

    ```
    # 先进入你的docker-compose.yml所在的目录
    cd /home/main/docker/WordPress
    # 执行脚本
    docker-compose up -d
    ```

5. 结果分析

    ```
    [root@izm5e3qug7oee4q1y4opibz WordPress]# docker-compose up -d
    Creating network "wordpress_default" with the default driver
    Creating volume "wordpress_db_data" with default driver
    Pulling db (mysql:5.7)...
    5.7: Pulling from library/mysql
    afb6ec6fdc1c: Pull complete
    ....
    0bdc5971ba40: Pull complete
    Digest: sha256:d16d9ef7a4ecb29efcd1ba46d5a82bda3c28bd18c0f1e3b86ba54816211e1ac4
    Status: Downloaded newer image for mysql:5.7
    Pulling wordpress (wordpress:latest)...
    latest: Pulling from library/wordpress
    afb6ec6fdc1c: Already exists
    3d895574014b: Pull complete
    ...
    Digest: sha256:0b452b7b45fa770f12e864720abb01bef506f4abe273669402434e94323c97d7
    Status: Downloaded newer image for wordpress:latest
    Creating wordpress_db_1 ... done
    Creating wordpress_wordpress_1 ... done
    ```

可以看到pulling db、pulling wordpress、done。大概了解到为我们创建了wordpress和wordpress所需要的mysql数据库。访问8000端口，大功告成！

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413212422.png)

### 3.4.和Dockerfile区别
Dockerfile容器间通信需要--link或者桥接方式进行，而DockerCompose全自动的呀。也就是说单容器的话肯定Dockerfile了，但是多容器之间需要交互、有依赖关系，那用DockerCompose来统一管理那些零散的Dockerfile来达到自动构建部署的一体化脚本。

### 3.5.实战
#### 3.5.1.需求描述
实战一个spring boot的项目，一个springboot的jar包依赖mysql数据库。我们用docker-compose完成自动化部署。
#### 3.5.2.准备工作
1. 准备如下文件

    ```
    [root@izm5e3qug7oee4q1y4opibz docker-compose-app]# pwd
    /home/main/docker/docker-compose-app
    [root@izm5e3qug7oee4q1y4opibz docker-compose-app]# ls -l
    total 12
    drwxr-xr-x 2 root root 4096 May 24 12:20 app
    drwxr-xr-x 2 root root 4096 May 24 12:20 db
    -rw-r--r-- 1 root root  335 May 24 12:20 docker-compose.yml
    ```

2. app
	> 里面就是我们的springboot的jar包和制作镜像的Dockerfile文件。

    ```
    [root@izm5e3qug7oee4q1y4opibz docker-compose-app]# ll app/
    total 23492
    -rw-r--r-- 1 root root     1071 May 24 12:19 application-dev.yml
    -rw-r--r-- 1 root root     1457 May 24 12:19 application.yml
    -rw-r--r-- 1 root root 24042957 May 24 12:20 bsbdj.jar
    -rw-r--r-- 1 root root      154 May 24 12:20 Dockerfile
    ```

	看下application-dev.yml的配置，主要看数据库配置：

    ```
    spring:
      datasource:
        driver-class-name: com.mysql.jdbc.Driver
        url: jdbc:mysql://db:3306/bsbdj?useUnicode=true
        username: root
        password: root
        tomcat:
          init-s-q-l: SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci
    server:
      port: 80
    ```
   
	> 这里我们发现猫腻了，数据库配置是db:3306，这不是平常的ip/域名神马的，这个是Mysql容器名称。所以这个服务依赖mysql服务，所以我们的主要目的就是利用DockerCompose来自动化操作多容器的Dockerfile文件来自动部署和初始化SQL的操作。

3. db
	> jar包所需要的数据库的sql文件和制作镜像的Dockerfile文件。

    ```
    [root@izm5e3qug7oee4q1y4opibz docker-compose-app]# ll db/
    total 35612
    -rw-r--r-- 1 root root       69 May 24 12:20 Dockerfile
    -rw-r--r-- 1 root root 36460577 May 24 12:20 init-db.sql
    ```

#### 3.5.3.开始实战
1. app的Dockerfile

    ```
    FROM openjdk:8u222-jre
    WORKDIR /usr/local/bsbdj
    ADD bsbdj.jar .
    ADD application.yml .
    ADD application-dev.yml .
    EXPOSE 80
    CMD ["java","-jar","bsbdj.jar"]
    ```

2. db的Dockerfile

    ```
    FROM mysql:5.7
    WORKDIR /docker-entrypoint-initdb.d
    ADD init-db.sql .
    ```

	这里有个细节：为什么是进入docker-entrypoint-initdb.d这目录在ADD sql？因为这个目录是个后门，这个目录下的sql文件会自动执行。我咋知道的？官方告诉我的：https://hub.docker.com/_/mysql

	![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/docker-demo/20210413212925.png)

3. 最终Boss：docker-compose.yml
	现在我们应用程序的Dockerfile和应用程序所依赖的数据库Dockerfile都已就绪。还剩下最后一个终极yml配置文件

    ```
    # 目前最稳定版本：3.3，所以3.3就行。
    version: '3.3'
    services:
      ## 服务名称叫 db，还记得我们application-dev.yml的配置吗？数据库配置的是db，对应的就是这里了。
      db:
        # Dockerfile文件所属的目录。
        build: ./db/
        # always：宕机自动重启。牛逼的很。
        restart: always
        # 环境变量，类似于-e参数
        environment:
          MYSQL_ROOT_PASSWORD: root
      # 服务名称叫 app    
      app:
        # Dockerfile文件所属的目录。若app依赖db，则需要把db服务配置放到app的前面。
        build: ./app/
        # 依赖上面的db service
        depends_on:
          - db
        # 宿主机和容器的端口均为80。上面app的Dockerfile暴露的是80端口，所以这里容器是80  
        ports:
          - "80:80"
        restart: always
    ```

4. 启动

    ```
    # -d代表后台启动
    docker-compose up -d
    ```

	启动结果：

    ```
    [root@izm5e3qug7oee4q1y4opibz docker-compose-app]# docker-compose up -d
    Creating network "docker-compose-app_default" with the default driver
    Building db
    Step 1/3 : FROM mysql:5.7
     ---> a4fdfd462add
    Step 2/3 : WORKDIR /docker-entrypoint-initdb.d
     ---> Running in d1ff6e4bb5a8
    Removing intermediate container d1ff6e4bb5a8
     ---> d29a05c5bfcb
    Step 3/3 : ADD init-db.sql .
     ---> 6ae6d9eb35ca

    Successfully built 6ae6d9eb35ca
    Successfully tagged docker-compose-app_db:latest
    WARNING: Image for service db was built because it did not already exist. To rebuild this image you must use `docker-compose build` or `docker-compose up --build`.
    Building app
    Step 1/7 : FROM openjdk:8u222-jre
    8u222-jre: Pulling from library/openjdk
    9a0b0ce99936: Pull complete
    db3b6004c61a: Pull complete
    f8f075920295: Pull complete
    4901756f2337: Pull complete
    9cfcf0e1f584: Pull complete
    d6307286bdcd: Pull complete
    Digest: sha256:3d3df6a0e485f9c38236eaa795fc4d2e8b8d0f9305051c1e4f7fbca71129b06a
    Status: Downloaded newer image for openjdk:8u222-jre
     ---> 25073ded58d2
    Step 2/7 : WORKDIR /usr/local/bsbdj
     ---> Running in df4a4c352e71
    Removing intermediate container df4a4c352e71
     ---> 0d88b2f13319
    Step 3/7 : ADD bsbdj.jar .
     ---> aabaa119855d
    Step 4/7 : ADD application.yml .
     ---> 7e1f7b4614cc
    Step 5/7 : ADD application-dev.yml .
     ---> a8d36115592f
    Step 6/7 : EXPOSE 80
     ---> Running in 26b44c9d57ef
    Removing intermediate container 26b44c9d57ef
     ---> fd36f3cdd115
    Step 7/7 : CMD ["java","-jar","bsbdj.jar"]
     ---> Running in 64bdeff2f1ce
    Removing intermediate container 64bdeff2f1ce
     ---> 77d18bae9bbc

    Successfully built 77d18bae9bbc
    Successfully tagged docker-compose-app_app:latest
    Creating docker-compose-app_db_1 ... done
    Creating docker-compose-app_app_1 ... done
    ```
    
	> 可以看到先为我们构建了mysql的镜像然后又构建了bsbdj.jar的镜像。最后执行了CMD ["java","-jar","bsbdj.jar"]，这些过程全自动化。

	查看容器

    ```
    docker-compose ps
    ```

	结果：

    ```
    [root@izm5e3qug7oee4q1y4opibz docker-compose-app]# docker-compose ps
              Name                       Command             State          Ports       
    ------------------------------------------------------------------------------------
    docker-compose-app_app_1   java -jar bsbdj.jar           Up      0.0.0.0:80->80/tcp 
    docker-compose-app_db_1    docker-entrypoint.sh mysqld   Up      3306/tcp, 33060/tcp
    ```
    
    然后访问 `http://ip:80` 即可看到效果。

#### 3.5.4.补充
docker-compose其他命令可以用docker-compose --help查看。再说下docker-compose和Dockerfile区别，可以粗糙理解成Dockerfile是针对单容器的脚本，docker-compose是针对多Dockerfile的自动化脚本，他帮我们处理容器之间的依赖关系和其他需要人为干涉的操作。