[toc]



# Nginx 应用

## 1.Windows下使用Nginx+Tomcat做负载均衡

### 1.1.配置文件介绍

现在我们有了Nginx的环境，接下来我们的目标是通过Nginx将用户的请求反向代理到Tomcat上，那么我们首先启动一台Tomcat服务器，默认配置即可。

然后我们打开 `nginx.conf` 文件，王子给大家简单介绍一下里面的一些配置的含义。

```
listen       81;
server_name  localhost;
```

- `listen`：刚才我们已经改过了，它就是代表Nginx的监听端口，这个没什么可说的

- `server_name`：表示监听到之后请求转到哪里，默认直接转到本地。

```
location / {
    root   html;
    index  index.html index.htm;
}
```

- `location`：表示匹配的路径，这时配置了 `/` 表示所有请求都被匹配到这里

- `root`：里面配置了root这时表示当匹配这个请求的路径时，将会在 `html` 这个文件夹内寻找相应的文件。

- `index`：当没有指定主页时，默认会选择这个指定的文件，它可以有多个，并按顺序来加载，如果第一个不存在，则找第二个，依此类推。

**除了这些配置，我们再补充一个配置**

`proxy_pass`，它表示代理路径，相当于转发，而不像之前说的root必须指定一个文件夹。

那么现在我们修改一下配置文件，如下：

```
location / {  
    proxy_pass http://localhost:8080;
}  
```

然后我们让Nginx重新加载配置文件，回到Nginx根目录执行 `nginx -s reload` 命令就可以了。

然后我们再重新打开Nginx的页面，小伙伴们，是不是发现它已经打开了Tomcat页面呢。

### 1.2.实现负载均衡的配置

刚刚我们已经实现了请求的反向代理，从 `Nginx` 转发到了 `Tomcat` 上，那么如何配置可以实现一个 `Tomcat` 的负载均衡集群呢，其实也是很容易的。

配置如下：

```
upstream localtomcat {  
    server localhost:8080;  
}  
  
server{  
        location / {  
           proxy_pass http://localtomcat;  
        }  
        #......其他省略  
}  
```

小伙伴们，划重点了，这里一定要注意。`upstream` 后的名字一定不要带下划线，`Nginx` 是不认下划线的，会导致转发异常。

那么如何添加新的 `tomcat` 实现负载均衡呢？

我们修改端口，新打开一个 `tomcat` 服务器，端口为 `8081`，然后增加配置如下：

```
upstream localtomcat {  
    server localhost:8080;  
    server localhost:8081;  
}  
```

再重新加载 `Nginx` 的配置文件，你会发现，负载均衡已经实现了，现在会向两台 `tomcat` 转发请求了。

而且我们可以设置 `weight=数字` 来指定每个 `tomcat` 的权重，数字越大，表明请求到的机会越大。

配置如下：

```
upstream localtomcat {  
    server localhost:8080 weight=1;  
    server localhost:8081 weight=5;  
}  
```



## 2.Nginx的高可用负载均衡

### 2.1.Keepalived 简要介绍

`Keepalived` 是一种高性能的服务器高可用或热备解决方案，`Keepalived` 可以用来防止服务器单点故障的发生，通过配合 `Nginx` 可以实现 web 前端服务的高可用。

`Keepalived` 以 `VRRP` 协议为实现基础，用 `VRRP` 协议来实现高可用性(`HA`)。 `VRRP`(`Virtual RouterRedundancy Protocol`)协议是用于实现路由器冗余的协议， `VRRP` 协议将两台或多台路由器设备虚拟成一个设备，对外提供虚拟路由器 IP(一个或多个)，而在路由器组内部，如果实际拥有这个对外 IP 的路由器如果工作正常的话就是 `MASTER`，或者是通过算法选举产生， `MASTER` 实现针对虚拟路由器 `IP` 的各种网络功能，如 `ARP` 请求， `ICMP`，以及数据的转发等；其他设备不拥有该虚拟 `IP`，状态是 `BACKUP`，除了接收 `MASTER` 的`VRRP` 状态通告信息外，不执行对外的网络功能。

当主机失效时， `BACKUP` 将接管原先 `MASTER` 的网络功能。`VRRP` 协议使用多播数据来传输 `VRRP` 数据， `VRRP` 数据使用特殊的虚拟源 `MAC` 地址发送数据而不是自身网卡的 `MAC` 地址， `VRRP` 运行时只有 `MASTER` 路由器定时发送 `VRRP` 通告信息，表示 `MASTER` 工作正常以及虚拟路由器 `IP`(组)，`BACKUP` 只接收 `VRRP` 数据，不发送数据，如果一定时间内没有接收到 `MASTER` 的通告信息，各 `BACKUP` 将宣告自己成为 `MASTER`，发送通告信息，重新进行 `MASTER` 选举状态。

### 2.2.方案规划

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414221710.png)

**操作系统与安装软件如下：**

- `CentOS 6.8 x64`
- `keepalived-1.2.18.tar.gz`
- `nginx-1.19.1.tar.gz`

**2.2.1.安装依赖环境**

```
yum -y install wget gcc-c++ ncurses ncurses-devel cmake make perl bison openssl openssl-devel gcc* libxml2 libxml2-devel curl-devel libjpeg* libpng* freetype* autoconf automake zlib* fiex* libxml* libmcrypt* libtool-ltdl-devel* libaio libaio-devel  bzr libtool
```

**2.2.2.安装openssl**

```bash
wget https://www.openssl.org/source/openssl-1.0.2s.tar.gz
tar -zxvf openssl-1.0.2s.tar.gz
cd /usr/local/src/openssl-1.0.2s
./config --prefix=/usr/local/openssl-1.0.2s
make
make install
```

**2.2.3.安装pcre**

```bash
wget https://ftp.pcre.org/pub/pcre/pcre-8.43.tar.gz
tar -zxvf pcre-8.43.tar.gz
cd /usr/local/src/pcre-8.43
./configure --prefix=/usr/local/pcre-8.43
make
make install
```

**2.2.4.安装zlib**

```bash
wget https://sourceforge.net/projects/libpng/files/zlib/1.2.11/zlib-1.2.11.tar.gz
tar -zxvf zlib-1.2.11.tar.gz
cd /usr/local/src/zlib-1.2.11
./configure --prefix=/usr/local/zlib-1.2.11
make
make
```

**2.2.5.下载nginx-rtmp-module**

nginx-rtmp-module的官方github地址：https://github.com/arut/nginx-rtmp-module

使用命令：

```bash
git clone https://github.com/arut/nginx-rtmp-module.git  
```

**2.2.6.安装Nginx**

```bash
wget http://nginx.org/download/nginx-1.19.1.tar.gz
tar -zxvf nginx-1.19.1.tar.gz
cd /usr/local/src/nginx-1.19.1
./configure --prefix=/usr/local/nginx-1.19.1 --with-openssl=/usr/local/src/openssl-1.0.2s --with-pcre=/usr/local/src/pcre-8.43 --with-zlib=/usr/local/src/zlib-1.2.11 --add-module=/usr/local/src/nginx-rtmp-module --with-http_ssl_module
make
make install
```

**这里需要注意的是：安装Nginx时，指定的是openssl、pcre和zlib的源码解压目录，安装完成后Nginx配置文件的完整路径为：`/usr/local/nginx-1.19.1/conf/nginx.conf`。**

### 2.3.配置Nginx

在命令行输入如下命令编辑 `Nginx` 的 `nginx.conf` 文件，如下所示。

```bash
# vim /usr/local/nginx-1.19.1/conf/nginx.conf
```

编辑后的文件内容如下所示。

```bash
user root;
worker_processes 1;
#error_log logs/error.log;
#error_log logs/error.log notice;
#error_log logs/error.log info;
#pid logs/nginx.pid;
events {
	worker_connections 1024;
}
http {
	include mime.types;
	default_type application/octet-stream;
	#log_format main '$remote_addr - $remote_user [$time_local] "$request" '
	# '$status $body_bytes_sent "$http_referer" '
	# '"$http_user_agent" "$http_x_forwarded_for"';
	#access_log logs/access.log main;
	sendfile on;
	#tcp_nopush on;
	#keepalive_timeout 0;
	keepalive_timeout 65;
	#gzip on;
	server {
		listen 88;
		server_name localhost;
		#charset koi8-r;
		#access_log logs/host.access.log main;
		location / {
			root html;
			index index.html index.htm;
		}
		#error_page 404 /404.html;
		# redirect server error pages to the static page /50x.html
		error_page 500 502 503 504 /50x.html;
		location = /50x.html {
			root html;
		}
	}
}
```

修改 Nginx 欢迎首页内容（用于后面测试， 用于区分两个节点的 Nginx）：

在binghe133服务器上执行如下操作。

```bash
# vim /usr/local/nginx-1.19.1/html/index.html
```

在文件title节点下添加如下代码。

```html
<h1>Welcome to nginx! 1</h1>
```

在binghe134服务器上执行如下操作。

```bash
# vim /usr/local/nginx-1.19.1/html/index.html
```

在文件title节点下添加如下代码。

```html
<h1>Welcome to nginx! 2</h1>
```

### 2.4.开放端口

在服务器的防火墙中开放88端口，如下所示。

```bash
vim /etc/sysconfig/iptables
```

添加如下配置。

```bash
-A INPUT -m state --state NEW -m tcp -p tcp --dport 88 -j ACCEPT
```

接下来，输入如下命令重启防火墙。

```bash
service iptables restart
```

### 2.5.测试Nginx

**测试Nginx是否安装成功**

```bash
# /usr/local/nginx-1.19.1/sbin/nginx -t
nginx: the configuration file /usr/local/nginx-1.19.1/conf/nginx.conf syntax is ok
nginx: configuration file /usr/local/nginx-1.19.1/conf/nginx.conf test is successful
```

**启动Nginx**

```bash
# /usr/local/nginx-1.19.1/sbin/nginx
```

**重启 Nginx**

```bash
# /usr/local/nginx-1.19.1/sbin/nginx -s reload
```

**设置Nginx开机自启动**

```bash
# vim /etc/rc.local
```

加入如下一行配置。

```bash
/usr/local/nginx-1.19.1/sbin/nginx
```

接下来，分别访问两台服务器上Nginx，如下所示。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414221849.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414221858.png)

### 2.6.安装 Keepalived

官方下载链接为：http://www.keepalived.org/download.html 。

#### 2.6.1.上传或下载 keepalived

上传或下载 `keepalived`（`keepalived-1.2.18.tar.gz`） 到 `/usr/local/src` 目录

#### 2.6.2.解压安装

```bash
# cd /usr/local/src
# tar -zxvf keepalived-1.2.18.tar.gz
# cd keepalived-1.2.18
# ./configure --prefix=/usr/local/keepalived
# make && make install
```

#### 2.6.3.将 keepalived 安装成 Linux 系统服务

因为没有使用 `keepalived` 的默认路径安装（默认是 `/usr/local`） ,安装完成之后，需要做一些工作复制默认配置文件到默认路径

```bash
# mkdir /etc/keepalived
# cp /usr/local/keepalived/etc/keepalived/keepalived.conf /etc/keepalived/
```

复制 keepalived 服务脚本到默认的地址

```bash
# cp /usr/local/keepalived/etc/rc.d/init.d/keepalived /etc/init.d/
# cp /usr/local/keepalived/etc/sysconfig/keepalived /etc/sysconfig/
# ln -s /usr/local/sbin/keepalived /usr/sbin/
# ln -s /usr/local/keepalived/sbin/keepalived /sbin/
```

设置 keepalived 服务开机启动。

```bash
# chkconfig keepalived on
```

#### 2.6.4.修改 Keepalived 配置文件

**MASTER 节点配置文件（`192.168.50.133`）**

```bash
# vim /etc/keepalived/keepalived.conf

! Configuration File for keepalived
global_defs {
	## keepalived 自带的邮件提醒需要开启 sendmail 服务。 建议用独立的监控或第三方 SMTP
	router_id binghe133 ## 标识本节点的字条串，通常为 hostname
} 
## keepalived 会定时执行脚本并对脚本执行的结果进行分析，动态调整 vrrp_instance 的优先级。如果脚本执行结果为 0，并且 weight 配置的值大于 0，则优先级相应的增加。如果脚本执行结果非 0，并且 weight配置的值小于 0，则优先级相应的减少。其他情况，维持原本配置的优先级，即配置文件中 priority 对应的值。
vrrp_script chk_nginx {
	script "/etc/keepalived/nginx_check.sh" ## 检测 nginx 状态的脚本路径
	interval 2 ## 检测时间间隔
	weight -20 ## 如果条件成立，权重-20
}
## 定义虚拟路由， VI_1 为虚拟路由的标示符，自己定义名称
vrrp_instance VI_1 {
	state MASTER ## 主节点为 MASTER， 对应的备份节点为 BACKUP
	interface eth0 ## 绑定虚拟 IP 的网络接口，与本机 IP 地址所在的网络接口相同， 我的是 eth0
	virtual_router_id 33 ## 虚拟路由的 ID 号， 两个节点设置必须一样， 可选 IP 最后一段使用, 相同的 VRID 为一个组，他将决定多播的 MAC 地址
	mcast_src_ip 192.168.50.133 ## 本机 IP 地址
	priority 100 ## 节点优先级， 值范围 0-254， MASTER 要比 BACKUP 高
	nopreempt ## 优先级高的设置 nopreempt 解决异常恢复后再次抢占的问题
	advert_int 1 ## 组播信息发送间隔，两个节点设置必须一样， 默认 1s
	## 设置验证信息，两个节点必须一致
	authentication {
		auth_type PASS
		auth_pass 1111 ## 真实生产，按需求对应该过来
	}
	## 将 track_script 块加入 instance 配置块
	track_script {
		chk_nginx ## 执行 Nginx 监控的服务
	} #
	# 虚拟 IP 池, 两个节点设置必须一样
	virtual_ipaddress {
		192.168.50.130 ## 虚拟 ip，可以定义多个
	}
}
```

**BACKUP 节点配置文件（`192.168.50.134`）**

```bash
# vim /etc/keepalived/keepalived.conf

! Configuration File for keepalived
global_defs {
	router_id binghe134
}
vrrp_script chk_nginx {
	script "/etc/keepalived/nginx_check.sh"
	interval 2
	weight -20
}
vrrp_instance VI_1 {
	state BACKUP
	interface eth1
	virtual_router_id 33
	mcast_src_ip 192.168.50.134
	priority 90
	advert_int 1
	authentication {
		auth_type PASS
		auth_pass 1111
	}
	track_script {
		chk_nginx
	}
	virtual_ipaddress {
		192.168.50.130
	}
}
```

#### 2.6.5.编写 Nginx 状态检测脚本

编写 Nginx 状态检测脚本 `/etc/keepalived/nginx_check.sh` (已在 `keepalived.conf` 中配置)脚本要求：如果 `nginx` 停止运行，尝试启动，如果无法启动则杀死本机的 `keepalived` 进程，`keepalied` 将虚拟 `ip` 绑定到 `BACKUP` 机器上。 内容如下。

```bash
# vim /etc/keepalived/nginx_check.sh

#!/bin/bash
A=`ps -C nginx –no-header |wc -l`
if [ $A -eq 0 ];then
/usr/local/nginx/sbin/nginx
sleep 2
if [ `ps -C nginx --no-header |wc -l` -eq 0 ];then
	killall keepalived
fi
fi
```

保存后，给脚本赋执行权限：

```bash
# chmod +x /etc/keepalived/nginx_check.sh
```

#### 2.6.6.启动 Keepalived

```bash
# service keepalived start
Starting keepalived: [ OK ]
```

### 2.7.Keepalived+Nginx 的高可用测试

同时启动 `192.168.50.133` 和 `192.168.50.134` 上的 `Nginx` 和 `Keepalived` ，我们通过`VIP` (`192.168.50.130`)来访问 `Nginx`，如下所示。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414221919.png)

我们关闭 `192.168.50.133` 上的 `Keepalived` 和 `Nginx`，在 `192.168.50.133` 执行如下命令。

```bash
service keepalived stop
/usr/local/nginx-1.19.1/sbin/nginx -s stop
```

此时，再通过`VIP` (`192.168.50.130`)来访问 `Nginx`，如下所示。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414221926.png)

我们再开启 `192.168.50.133` 上的 `Keepalived` 和 `Nginx`，在 `192.168.50.133` 执行如下命令：

```bash
/usr/local/nginx-1.19.1/sbin/nginx
service keepalived start
```

或者只执行

```bash
service keepalived start
```

因为我们写了脚本 `nginx_check.sh`，这个脚本会为我们自动自动 `Nginx`。

此时，我们再通过`VIP`(`192.168.50.130`)来访问 `Nginx`，如下所示。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414221944.png)

## 3.Nginx实现MySQL负载均衡

### 3.1.前提条件

**注意：使用Nginx实现MySQL数据库的负载均衡，前提是要搭建MySQL的主主复制环境，关于MySQL主主复制环境的搭建，后续会在MySQL专题为大家详细阐述。这里，我们假设已经搭建好MySQL的主主复制环境，MySQL服务器的IP和端口分别如下所示。**

- `192.168.1.101 3306`
- `192.168.1.102 3306`

通过Nginx访问MySQL的IP和端口如下所示。

- `192.168.1.100 3306`

### 3.2.Nginx实现MySQL负载均衡

nginx在版本1.9.0以后支持tcp的负载均衡，具体可以参照官网关于模块[ngx_stream_core_module](http://nginx.org/en/docs/stream/ngx_stream_core_module.html#tcp_nodelay)的叙述，链接地址为：http://nginx.org/en/docs/stream/ngx_stream_core_module.html#tcp_nodelay。

`nginx` 从 `1.9.0` 后引入模块 `ngx_stream_core_module`，模块是没有编译的，需要用到编译，编译时需添加 `--with-stream` 配置参数，`stream` 负载均衡官方配置样例如下所示。

```bash
worker_processes auto;
error_log /var/log/nginx/error.log info;

events {
    worker_connections  1024;
}

stream {
    upstream backend {
        hash $remote_addr consistent;

        server backend1.example.com:12345 weight=5;
        server 127.0.0.1:12345            max_fails=3 fail_timeout=30s;
        server unix:/tmp/backend3;
    }

    upstream dns {
       server 192.168.0.1:53535;
       server dns.example.com:53;
    }

    server {
        listen 12345;
        proxy_connect_timeout 1s;
        proxy_timeout 3s;
        proxy_pass backend;
    }

    server {
        listen 127.0.0.1:53 udp;
        proxy_responses 1;
        proxy_timeout 20s;
        proxy_pass dns;
    }
    
    server {
        listen [::1]:12345;
        proxy_pass unix:/tmp/stream.socket;
    }
}
```

说到这里，使用Nginx实现MySQL的负载均衡就比较简单了。我们可以参照上面官方的配置示例来配置MySQL的负载均衡。这里，我们可以将Nginx配置成如下所示。

```bash
user  nginx;
#user root;
worker_processes  1;
error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;
events {
    worker_connections  1024;
}
http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';
    access_log  /var/log/nginx/access.log  main;
    sendfile        on;
    #tcp_nopush     on;
    keepalive_timeout  65;
    #gzip  on;
    include /etc/nginx/conf.d/*.conf;
}

stream{
	upstream mysql{
		server 192.168.1.101:3306 weight=1;
		server 192.168.1.102:3306 weight=1;
	}
        
	server{
		listen 3306;
		server_name 192.168.1.100;
		proxy_pass mysql;
	}
}
```

配置完成后，我们就可以通过如下方式来访问MySQL数据库。

```bash
jdbc:mysql://192.168.1.100:3306/数据库名称
```

此时，`Nginx` 会将访问 `MySQL` 的请求路由到IP地址为 `192.168.1.101` 和 `192.168.1.102` 的 `MySQL`上。

## 4.禁用IP和IP段

### 4.1.禁用IP和IP段

`Nginx` 的 `ngx_http_access_module` 模块可以封配置内的ip或者ip段，语法如下：

```bash
deny IP;
deny subnet;
allow IP;
allow subnet;
# block all ips
deny    all;
# allow all ips
allow    all;
```

如果规则之间有冲突，会以最前面匹配的规则为准。

### 4.2.配置禁用ip和ip段

下面说明假定 `nginx` 的目录在 `/usr/local/nginx/`。

首先要建一个封 `ip` 的配置文件 `blockips.conf`，然后 `vi blockips.conf` 编辑此文件，在文件中输入要封的ip。

```bash
deny 1.2.3.4;
deny 91.212.45.0/24;
deny 91.212.65.0/24;
```

然后保存此文件，并且打开 `nginx.conf` 文件，在http配置节内添加下面一行配置：

```bash
include blockips.conf;
```

保存 `nginx.conf` 文件，然后测试现在的 `nginx` 配置文件是否是合法的：

```bash
/usr/local/nginx/sbin/nginx -t
```

如果配置没有问题，就会输出：

```bash
the configuration file /usr/local/nginx/conf/nginx.conf syntax is ok
configuration file /usr/local/nginx/conf/nginx.conf test is successful
```

如果配置有问题就需要检查下哪儿有语法问题，如果没有问题，需要执行下面命令，让nginx重新载入配置文件。

```bash
/usr/local/nginx/sbin/nginx -s reload
```

### 4.3.仅允许内网ip

如何禁止所有外网ip，仅允许内网ip呢？

如下配置文件

```bash
location / {
  # block one workstation
  deny    192.168.1.1;
  # allow anyone in 192.168.1.0/24
  allow   192.168.1.0/24;
  # drop rest of the world
  deny    all;
}
```

上面配置中禁止了192.168.1.1，允许其他内网网段，然后deny all禁止其他所有ip。

### 4.4.格式化nginx的403页面

如何格式化nginx的403页面呢？

首先执行下面的命令：

```bash
cd /usr/local/nginx/html
vi error403.html
```

然后输入403的文件内容，例如：

```html
<html>
<head><title>Error 403 - IP Address Blocked</title></head>
<body>
Your IP Address is blocked. If you this an error, please contact binghe with your IP at test@binghe.com
</body>
</html>
```

如果启用了SSI，可以在403中显示被封的客户端ip，如下：

```bash
Your IP Address is <!--#echo var="REMOTE_ADDR" --> blocked.
```

保存error403文件，然后打开nginx的配置文件vi nginx.conf,在server配置节内添加下面内容。

```bash
# redirect server error pages to the static page
 error_page   403  /error403.html;
 location = /error403.html {
         root   html;
 }
```

然后保存配置文件，通过nginx -t命令测试配置文件是否正确，若正确通过nginx -s reload载入配置。

## 5.使用Nginx解决跨域问题

### 5.1.为何会跨域？

出于浏览器的同源策略限制。同源策略（Sameoriginpolicy）是一种约定，它是浏览器最核心也最基本的安全功能，如果缺少了同源策略，则浏览器的正常功能可能都会受到影响。可以说Web是构建在同源策略基础之上的，浏览器只是针对同源策略的一种实现。同源策略会阻止一个域的javascript脚本和另外一个域的内容进行交互。所谓同源（即指在同一个域）就是两个页面具有相同的协议（`protocol`），主机（`host`）和端口号（`port`）。

### 5.2.Nginx如何解决跨域？

这里，我们利用Nginx的反向代理功能解决跨域问题，至于，什么是Nginx的反向代理，大家就请自行百度或者谷歌吧。

Nginx作为反向代理服务器，就是把http请求转发到另一个或者一些服务器上。通过把本地一个url前缀映射到要跨域访问的web服务器上，就可以实现跨域访问。对于浏览器来说，访问的就是同源服务器上的一个url。而Nginx通过检测url前缀，把http请求转发到后面真实的物理服务器。并通过rewrite命令把前缀再去掉。这样真实的服务器就可以正确处理请求，并且并不知道这个请求是来自代理服务器的。

### 5.3.Nginx解决跨域案例

使用Nginx解决跨域问题时，我们可以编译 `Nginx` 的 `nginx.conf` 配置文件，例如，将 `nginx.conf` 文件的 `server` 节点的内容编辑成如下所示。

```bash
server {
        location / {
            root   html;
            index  index.html index.htm;
            //允许cros跨域访问
            add_header 'Access-Control-Allow-Origin' '*';

        }
        //自定义本地路径
        location /apis {
            rewrite  ^.+apis/?(.*)$ /$1 break;
            include  uwsgi_params;
            proxy_pass   http://www.binghe.com;
       }
}
```

然后我把项目部署在 `nginx` 的 `html` 根目录下，在 `ajax` 调用时设置url从 `http://www.binghe.com/apistest/test` 变为 `http://www.binghe.com/apis/apistest/test` 然后成功解决。

假设，之前我在页面上发起的Ajax请求如下所示。

```html
$.ajax({
        type:"post",
        dataType: "json",
        data:{'parameter':JSON.stringify(data)},
        url:"http://www.binghe.com/apistest/test",
        async: flag,
        beforeSend: function (xhr) {
 
            xhr.setRequestHeader("Content-Type", submitType.Content_Type);
            xhr.setRequestHeader("user-id", submitType.user_id);
            xhr.setRequestHeader("role-type", submitType.role_type);
            xhr.setRequestHeader("access-token", getAccessToken().token);
        },
        success:function(result, status, xhr){
     	
        }
        ,error:function (e) {
            layerMsg('请求失败，请稍后再试')
        }
    });
```

修改成如下的请求即可解决跨域问题。

```bash
$.ajax({
        type:"post",
        dataType: "json",
        data:{'parameter':JSON.stringify(data)},
        url:"http://www.binghe.com/apis/apistest/test",
        async: flag,
        beforeSend: function (xhr) {
 
            xhr.setRequestHeader("Content-Type", submitType.Content_Type);
            xhr.setRequestHeader("user-id", submitType.user_id);
            xhr.setRequestHeader("role-type", submitType.role_type);
            xhr.setRequestHeader("access-token", getAccessToken().token);
        },
        success:function(result, status, xhr){
     	
        }
        ,error:function (e) {
            layerMsg('请求失败，请稍后再试')
        }
    });
```

## 6.使用Nginx实现限流

### 6.1.限流措施

**网上很多的文章和帖子中在介绍秒杀系统时，说是在下单时使用异步削峰来进行一些限流操作，那都是在扯淡！因为下单操作在整个秒杀系统的流程中属于比较靠后的操作了，限流操作一定要前置处理，在秒杀业务后面的流程中做限流操作是没啥卵用的。**

Nginx作为一款高性能的Web代理和负载均衡服务器，往往会部署在一些互联网应用比较前置的位置。此时，我们就可以在Nginx上进行设置，对访问的IP地址和并发数进行相应的限制。

### 6.2.Nginx官方的限流模块

Nginx官方版本限制IP的连接和并发分别有两个模块：

- `limit_req_zone` 用来限制单位时间内的请求数，即速率限制,采用的漏桶算法 "leaky bucket"。
- `limit_req_conn` 用来限制同一时间连接数，即并发限制。

### 6.3.limit_req_zone 参数配置

#### 6.3.1.limit_req_zone参数说明

```bash
Syntax: limit_req zone=name [burst=number] [nodelay];
Default:    —
Context:    http, server, location
limit_req_zone $binary_remote_addr zone=one:10m rate=1r/s;
```

- 第一个参数：`$binary_remote_addr` 表示通过 `remote_addr` 这个标识来做限制，`binary_` 的目的是缩写内存占用量，是限制同一客户端ip地址。
- 第二个参数：`zone=one:10m` 表示生成一个大小为 `10M`，名字为 `one` 的内存区域，用来存储访问的频次信息。
- 第三个参数：`rate=1r/s` 表示允许相同标识的客户端的访问频次，这里限制的是每秒1次，还可以有比如 `30r/m` 的。

```bash
limit_req zone=one burst=5 nodelay;
```

- 第一个参数：`zone=one` 设置使用哪个配置区域来做限制，与上面 `limit_req_zone`  里的 `name` 对应。
- 第二个参数：`burst=5`，重点说明一下这个配置，`burst` 爆发的意思，这个配置的意思是设置一个大小为5的缓冲区当有大量请求（爆发）过来时，超过了访问频次限制的请求可以先放到这个缓冲区内。
- 第三个参数：`nodelay`，如果设置，超过访问频次而且缓冲区也满了的时候就会直接返回503，如果没有设置，则所有请求会等待排队。

#### 6.3.2.limit_req_zone示例

```bash
http {
    limit_req_zone $binary_remote_addr zone=one:10m rate=1r/s;
    server {
        location /search/ {
            limit_req zone=one burst=5 nodelay;
        }
}
```

下面配置可以限制特定UA（比如搜索引擎）的访问：

```bash
limit_req_zone  $anti_spider  zone=one:10m   rate=10r/s;
limit_req zone=one burst=100 nodelay;
if ($http_user_agent ~* "googlebot|bingbot|Feedfetcher-Google") {
    set $anti_spider $http_user_agent;
}
```

其他参数

```bash
Syntax: limit_req_log_level info | notice | warn | error;
Default:    
limit_req_log_level error;
Context:    http, server, location
```

当服务器由于limit被限速或缓存时，配置写入日志。延迟的记录比拒绝的记录低一个级别。例子：`limit_req_log_level notice`延迟的的基本是info。

```bash
Syntax: limit_req_status code;
Default:    
limit_req_status 503;
Context:    http, server, location
```

设置拒绝请求的返回值。值只能设置 400 到 599 之间。

### 6.4.ngx_http_limit_conn_module 参数配置

#### 6.4.1.ngx_http_limit_conn_module 参数说明

这个模块用来限制单个IP的请求数。并非所有的连接都被计数。只有在服务器处理了请求并且已经读取了整个请求头时，连接才被计数。

```bash
Syntax: limit_conn zone number;
Default:    —
Context:    http, server, location
limit_conn_zone $binary_remote_addr zone=addr:10m;
 
server {
    location /download/ {
        limit_conn addr 1;
    }
```

一次只允许每个IP地址一个连接。

```bash
limit_conn_zone $binary_remote_addr zone=perip:10m;
limit_conn_zone $server_name zone=perserver:10m;
 
server {
    ...
    limit_conn perip 10;
    limit_conn perserver 100;
}
```

可以配置多个 `limit_conn` 指令。例如，以上配置将限制每个客户端IP连接到服务器的数量，同时限制连接到虚拟服务器的总数。

```bash
Syntax: limit_conn_zone key zone=name:size;
Default:    —
Context:    http
limit_conn_zone $binary_remote_addr zone=addr:10m;
```

在这里，客户端IP地址作为关键。请注意，不是 `$ remote_addr`，而是使用 `$ binary_remote_addr` 变量。` $ remote_addr` 变量的大小可以从7到15个字节不等。存储的状态在32位平台上占用32或64字节的内存，在64位平台上总是占用64字节。对于 `IPv4` 地址，`$ binary_remote_addr` 变量的大小始终为4个字节，对于 `IPv6` 地址则为16个字节。存储状态在32位平台上始终占用32或64个字节，在64位平台上占用64个字节。一个兆字节的区域可以保持大约32000个32字节的状态或大约16000个64字节的状态。如果区域存储耗尽，服务器会将错误返回给所有其他请求。

```bash
Syntax: limit_conn_log_level info | notice | warn | error;
Default:    
limit_conn_log_level error;
Context:    http, server, location
```

当服务器限制连接数时，设置所需的日志记录级别。

```bash
Syntax: limit_conn_status code;
Default:    
limit_conn_status 503;
Context:    http, server, location
```

设置拒绝请求的返回值。

### 6.5.Nginx限流实战

#### 6.5.1.限制访问速率

```bash
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=2r/s;
server { 
    location / { 
        limit_req zone=mylimit;
    }
}
```

上述规则限制了每个IP访问的速度为2r/s，并将该规则作用于根目录。如果单个IP在非常短的时间内并发发送多个请求，结果会怎样呢？

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414230337.png)

我们使用单个IP在10ms内发并发送了6个请求，只有1个成功，剩下的5个都被拒绝。我们设置的速度是2r/s，为什么只有1个成功呢，是不是Nginx限制错了？当然不是，是因为Nginx的限流统计是基于毫秒的，我们设置的速度是2r/s，转换一下就是500ms内单个IP只允许通过1个请求，从501ms开始才允许通过第二个请求。

#### 6.5.2.burst缓存处理

我们看到，我们短时间内发送了大量请求，Nginx按照毫秒级精度统计，超出限制的请求直接拒绝。这在实际场景中未免过于苛刻，真实网络环境中请求到来不是匀速的，很可能有请求“突发”的情况，也就是“一股子一股子”的。Nginx考虑到了这种情况，可以通过burst关键字开启对突发请求的缓存处理，而不是直接拒绝。

来看我们的配置：

```bash
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=2r/s;
server { 
    location / { 
        limit_req zone=mylimit burst=4;
    }
}
```

我们加入了 `burst=4`，意思是每个key(此处是每个IP)最多允许4个突发请求的到来。如果单个IP在10ms内发送6个请求，结果会怎样呢？

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414230350.png)

相比实例一成功数增加了4个，这个我们设置的burst数目是一致的。具体处理流程是：1个请求被立即处理，4个请求被放到burst队列里，另外一个请求被拒绝。通过burst参数，我们使得Nginx限流具备了缓存处理突发流量的能力。

但是请注意：burst的作用是让多余的请求可以先放到队列里，慢慢处理。如果不加nodelay参数，队列里的请求不会立即处理，而是按照rate设置的速度，以毫秒级精确的速度慢慢处理。

#### 6.5.3.nodelay降低排队时间

在使用burst缓存处理中，我们看到，通过设置burst参数，我们可以允许Nginx缓存处理一定程度的突发，多余的请求可以先放到队列里，慢慢处理，这起到了平滑流量的作用。但是如果队列设置的比较大，请求排队的时间就会比较长，用户角度看来就是RT变长了，这对用户很不友好。有什么解决办法呢？nodelay参数允许请求在排队的时候就立即被处理，也就是说只要请求能够进入burst队列，就会立即被后台worker处理，请注意，这意味着burst设置了nodelay时，系统瞬间的QPS可能会超过rate设置的阈值。nodelay参数要跟burst一起使用才有作用。

延续burst缓存处理的配置，我们加入nodelay选项：

```bash
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=2r/s;
server { 
    location / { 
        limit_req zone=mylimit burst=4 nodelay;
    }
}
```

单个IP 10ms内并发发送6个请求，结果如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414230400.png)

跟burst缓存处理相比，请求成功率没变化，但是总体耗时变短了。这怎么解释呢？在burst缓存处理中，有4个请求被放到burst队列当中，工作进程每隔500ms(rate=2r/s)取一个请求进行处理，最后一个请求要排队2s才会被处理；这里，请求放入队列跟burst缓存处理是一样的，但不同的是，队列中的请求同时具有了被处理的资格，所以这里的5个请求可以说是同时开始被处理的，花费时间自然变短了。

但是请注意，虽然设置burst和nodelay能够降低突发请求的处理时间，但是长期来看并不会提高吞吐量的上限，长期吞吐量的上限是由rate决定的，因为nodelay只能保证burst的请求被立即处理，但Nginx会限制队列元素释放的速度，就像是限制了令牌桶中令牌产生的速度。

看到这里你可能会问，加入了nodelay参数之后的限速算法，到底算是哪一个“桶”，是漏桶算法还是令牌桶算法？当然还算是漏桶算法。考虑一种情况，令牌桶算法的token为耗尽时会怎么做呢？由于它有一个请求队列，所以会把接下来的请求缓存下来，缓存多少受限于队列大小。但此时缓存这些请求还有意义吗？如果server已经过载，缓存队列越来越长，RT越来越高，即使过了很久请求被处理了，对用户来说也没什么价值了。所以当token不够用时，最明智的做法就是直接拒绝用户的请求，这就成了漏桶算法。

#### 6.5.4.自定义返回值

```bash
limit_req_zone $binary_remote_addr zone=mylimit:10m rate=2r/s;
server { 
    location / { 
        limit_req zone=mylimit burst=4 nodelay;
        limit_req_status 598;
    }
}
```

默认情况下 没有配置 status 返回值的状态：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414230412.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414230422.png)





























