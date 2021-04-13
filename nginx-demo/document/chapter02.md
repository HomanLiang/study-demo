[toc]



# Nginx 安装

## 1.Linux 版本安装
### 1.1.安装 nginx 环境
```
yum install gcc-c++
yum install -y pcre pcre-devel
yum install -y zlib zlib-devel
yum install -y openssl openssl-devel
```
对于 gcc，因为安装nginx需要先将官网下载的源码进行编译，编译依赖gcc环境，如果没有gcc环境的话，需要安装gcc。

对于 pcre，prce(Perl Compatible Regular Expressions)是一个Perl库，包括 perl 兼容的正则表达式库。nginx的http模块使用pcre来解析正则表达式，所以需要在linux上安装pcre库。

对于 zlib，zlib库提供了很多种压缩和解压缩的方式，nginx使用zlib对http包的内容进行gzip，所以需要在linux上安装zlib库。

对于 openssl，OpenSSL 是一个强大的安全套接字层密码库，囊括主要的密码算法、常用的密钥和证书封装管理功能及SSL协议，并提供丰富的应用程序供测试或其它目的使用。nginx不仅支持http协议，还支持https（即在ssl协议上传输http），所以需要在linux安装openssl库。

### 1.2.编译安装
首先将下载的 nginx-1.14.0.tar.gz 文件复制到 Linux 系统中，然后解压：
```
tar -zxvf nginx-1.14.0.tar.gz
```
接着进入到解压之后的目录，进行编译安装。
```
./configure --prefix=/usr/local/nginx
make
make install
```
注意：指定 /usr/local/nginx 为nginx 服务安装的目录。
### 1.3.启动 nginx
进入到 /usr/local/nginx 目录，文件目录显示如下：

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414002101.png)

接着我们进入到 sbin 目录，通过如下命令启动 nginx：

```
./nginx
```
当然你也可以配置环境命令，这样在任意目录都能启动 nginx。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414002111.png)

Linux 没有消息就好消息，不提示任何信息说明启动成功。
或者也可以输入如下命令，查看 nginx 是否有服务正在运行：

```
ps -ef | grep nginx
```
然后我们在浏览器输入Linux系统的IP地址，出现windows安装成功的界面即可。

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414002117.png)

### 1.4.关闭 nginx
有两种方式：
- 快速停止

    ```
    cd /usr/local/nginx/sbin
    ./nginx -s stop
    ```

	此方式相当于先查出nginx进程id再使用kill命令强制杀掉进程。不太友好。
	
- 平缓停止

    ```
    cd /usr/local/nginx/sbin
    ./nginx -s quit
    ```

	此方式是指允许 nginx 服务将当前正在处理的网络请求处理完成，但不在接收新的请求，之后关闭连接，停止工作。

### 1.5.重启 nginx
- 先停止再启动

    ```
    ./nginx -s quit
    ./nginx
    ```

	相当于先执行停止命令再执行启动命令。

- 重新加载配置文件

    ```
    ./nginx -s reload
    ```

	通常我们使用nginx修改最多的便是其配置文件 nginx.conf。修改之后想要让配置文件生效而不用重启 nginx，便可以使用此命令。

### 1.6.检测配置文件语法是否正确
- 通过如下命令，指定需要检查的配置文件

    ```
    nginx -t -c  /usr/local/nginx/conf/nginx.conf
    ```

- 通过如下命令，不加 -c 参数，默认检测nginx.conf 配置文件。

    ```
    nginx -t 
    ```

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414002246.png)