[toc]



# Nginx 常见问题

## 1.并发量太高，Nginx扛不住？这次我错怪Nginx了！！

最近，在服务器上搭建了一套压测环境，不为别的，就为压测下Nginx的性能，到底有没有传说中的那么牛逼！具体环境为：11台虚拟机，全部安装CentOS 6.8 64位操作系统，1台安装部署Nginx，其他10台作为客户端同时以压满CPU的线程向Nginx发送请求，对Nginx进行压测。没想到，出现问题了！！

### 1.1.Nginx报错

Nginx服务器访问量非常高，在Nginx的错误日志中不停的输出如下错误信息。

```bash
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
2020-07-23 02:53:49 [alert] 13576#0: accept() failed (24: Too many open files)
```

根据错误日志的输出信息，我们可以看出：是打开的文件句柄数太多了，导致Nginx报错了！那我们该如何解决这个问题呢？

### 1.2.问题分析

既然我们能够从Nginx的错误日志中基本能够确定导致问题的原因，那这到底是不是Nginx本身的问题呢？答案为：是，也不全是！

为啥呢？原因很简单：Nginx无法打开那么多的文件句柄，一方面是因为我没有配置Nginx能够打开的最大文件数；另一方面是因为CentOS 6.8操作系统本身对打开的最大文件句柄数有限制，我同样没有配置操作系统的最大文件句柄数。所以说，不全是Nginx的锅！在某种意义上说，我错怪Nginx了！

在CentOS 6.8服务器中，我们可以在命令行输入如下命令来查看服务器默认配置的最大文件句柄数。

```bash
[root@binghe150 ~]# ulimit -n
1024
```

可以看到，在CentOS 6.8服务器中，默认的最大文件句柄数为1024。

此时，当Nginx的连接数超过1024时，Nginx的错误日志中就会输出如下错误信息。

```bash
[alert] 13576#0: accept() failed (24: Too many open files)
```

### 1.3.解决问题

那我们该如何解决这个问题呢？其实，也很简单，继续往下看！

使用如下命令可以把打开文件句柄数设置的足够大。

```bash
ulimit -n 655350
```

同时修改 `nginx.conf `， 添加如下配置项。

```bash
worker_rlimit_nofile 655350; 
```

**注意：上述配置需要与error_log同级别。**

这样就可以解决Nginx连接过多的问题，Nginx就可以支持高并发（这里需要配置Nginx）。

另外， `ulimit -n` 还会影响到MySQL的并发连接数。把它提高，也可以提高MySQL的并发。

**注意： 用 `ulimit -n 655350` 修改只对当前的shell有效，退出后失效。**

### 1.4.永久解决问题

若要令修改ulimits的数值永久生效，则必须修改配置文件，可以给 `ulimit` 修改命令放入 `/etc/profile` 里面，这个方法实在是不方便。

还有一个方法是修改 `/etc/security/limits.conf` 配置文件，如下所示。

```ba
vim /etc/security/limits.conf
```

在文件最后添加如下配置项。

```bash
* soft nofile 655360
* hard nofile 655360
```

保存并退出vim编辑器。

其中：星号代表全局， soft为软件，hard为硬件，nofile为这里指可打开的文件句柄数。

最后，需要注意的是：要使 `limits.conf` 文件配置生效，必须要确保 `pam_limits.so` 文件被加入到启动文件中。查看 `/etc/pam.d/login` 文件中是否存在如下配置。

```bash
session required /lib64/security/pam_limits.so
```

不存在，则需要添加上述配置项。

## 2.记一次生产环境大面积404问题！

### 2.1.问题复现

得知运营的反馈后，我迅速登录服务器排查问题。首先，查看了接口服务的启动进程正常。验证接口服务的ip和端口是否正常，结果也是没啥问题。接下来，通过Nginx转发请求，此时出现了问题，无法访问接口。同时Nginx的access.log文件中输出了如下日志信息。

```bash
192.168.175.120 - - [26/Feb/2021:21:34:21 +0800] "GET /third/system/base/thirdapp/get_detail HTTP/1.1" 404 0 "http://192.168.175.100/api/index.html" "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:85.0) Gecko/20100101 Firefox/85.0"
192.168.175.120 - - [26/Feb/2021:21:34:22 +0800] "GET /third/system/base/thirdapp/get_detail HTTP/1.1" 404 0 "http://192.168.175.100/api/index.html" "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:85.0) Gecko/20100101 Firefox/85.0"
192.168.175.120 - - [26/Feb/2021:21:34:26 +0800] "GET /third/system/base/thirdapp/get_detail HTTP/1.1" 404 0 "http://192.168.175.100/api/index.html" "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:85.0) Gecko/20100101 Firefox/85.0"
```

此时，从Nginx日志中发现，输出的状态为404，未找到后端的接口服务。为了进一步定位问题，我直接在线上环境通过`curl`命令的方式来访问接口服务，结果是正常的。

经过这一系列的操作之后，我们就可以确定问题是出在Nginx上了。

### 2.2.问题分析

#### 2.2.1.Nginx开启debug模块

既然已经定位到问题了，那我们接下来就要分析下产生问题的具体原因了。既然是Nginx的问题，我第一时间想到的就是调试Nginx查找错误原因。于是我在服务器命令行输入了如下命令来查看安装Nginx时的配置情况。

```bash
nginx -V
```

注意：这里已经为Nginx配置了系统环境变量，如果没有配置系统环境变量，则需要输入nginx命令所在目录的完整路径，例如：

```bash
/usr/local/nginx/sbin/nginx -v
```

命令行输出了如下信息。

```bash
configure arguments: --prefix=/usr/local/nginx --with-http_stub_status_module --add-module=/usr/local/src/fastdfs/fastdfs-nginx-module-1.22/src --with-openssl=/usr/local/src/openssl-1.0.2s --with-pcre=/usr/local/src/pcre-8.43 --with-zlib=/usr/local/src/zlib-1.2.11 --with-http_ssl_module
```

可以看到，安装Nginx时没有配置Nginx的debug模块。

于是我在服务器上找到了Nginx的安装文件，在命令行输入如下命令重新编译Nginx。

```bash
cd /usr/local/src/nginx/  #进入Nginx的安装文件根目录
make clean                #清除编译信息
./configuration --prefix=/usr/local/nginx-1.17.8 --with-http_stub_status_module --add-module=/usr/local/src/fastdfs/fastdfs-nginx-module-1.22/src --with-openssl=/usr/local/src/openssl-1.0.2s --with-pcre=/usr/local/src/pcre-8.43 --with-zlib=/usr/local/src/zlib-1.2.11 --with-http_ssl_module --with-debug  #设置编译Nginx的配置信息
make     #编译Nginx,切记不要输入make install
```

上述命令中，切记不要输入`make install` 进行安装。

执行完 `make` 命令后，会在当前目录的objs目录下生成nginx命令，此时我们需要先停止Nginx服务，备份 `/usr/local/nginx/sbin/` 目录下的nginx命令，然后将objs目录下的nginx命令复制到 `/usr/local/nginx/sbin/` 目录下，然后启动Nginx服务。

```bash
nginx_service.sh stop   #通过脚本停止Nginx服务
mv /usr/local/nginx/sbin/nginx /usr/local/nginx/sbin/nginx.bak #备份原有nginx命令
cp ./objs/nginx /usr/local/nginx/sbin/nginx #复制nginx命令
nginx_service.sh start #通过脚本启动Nginx服务
```

注意：这里，在停止Nginx服务前，已经将此Nginx从接入层网关中移除了，所以不会影响线上环境。为了避免使用新编译的nginx命令重启Nginx出现问题，这里通过脚本先停止Nginx服务，然后复制nginx命令后，再启动Nginx服务。

#### 2.2.2.配置Nginx输出debug日志

在Nginx的nginx.conf文件中配置如下信息。

```bash
error_log  logs/error.log debug;
```

此时，开启了Nginx的debug日志功能，并将debug信息输出到 `error.log` 文件中。

#### 2.2.3.分析问题

接下来，在服务器命令行输入如下命令监听 `error.log` 文件的输出日志。

```bash
tail -F /usr/local/nginx/logs/error.log
```

然后模拟访问http接口，可以看到error.log文件中输出如下信息。

```bash
2021/02/26 21:34:26 [debug] 31486#0: *56 http request line: "GET /third/system/base/thirdapp/get_detail HTTP/1.1"
2021/02/26 21:34:26 [debug] 31486#0: *56 http uri: "/third/system/base/thirdapp/get_detail"
2021/02/26 21:34:26 [debug] 31486#0: *56 http args: ""
2021/02/26 21:34:26 [debug] 31486#0: *56 http exten: ""
2021/02/26 21:34:26 [debug] 31486#0: *56 posix_memalign: 0000000000FF6450:4096 @16
2021/02/26 21:34:26 [debug] 31486#0: *56 http process request header line
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "Host: 10.31.5.66"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:85.0) Gecko/20100101 Firefox/85.0"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "Accept: */*"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "Accept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "Accept-Encoding: gzip, deflate"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "Referer: http://192.168.175.100/api/index.html"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header: "Connection: keep-alive"
2021/02/26 21:34:26 [debug] 31486#0: *56 http header done
2021/02/26 21:34:26 [debug] 31486#0: *56 rewrite phase: 0
2021/02/26 21:34:26 [debug] 31486#0: *56 test location: "/"
2021/02/26 21:34:26 [debug] 31486#0: *56 test location: "file/"
2021/02/26 21:34:26 [debug] 31486#0: *56 test location: ~ "/base"
2021/02/26 21:34:26 [debug] 31486#0: *56 using configuration "/base"
```

从上面的输出日志中，我们可以看到：访问的接口地址为 `/third/system/base/thirdapp/get_detail` ，如下所示。

```bash
2021/02/26 21:34:26 [debug] 31486#0: *56 http uri: "/third/system/base/thirdapp/get_detail"
```

Nginx在进行转发时，分别匹配了 `/`，`file/`，`~/base`，最终将请求转发到了 `/base`，如下所示。

```bash
2021/02/26 21:34:26 [debug] 31486#0: *56 test location: "/"
2021/02/26 21:34:26 [debug] 31486#0: *56 test location: "file/"
2021/02/26 21:34:26 [debug] 31486#0: *56 test location: ~ "/base"
2021/02/26 21:34:26 [debug] 31486#0: *56 using configuration "/base"
```

我们再来看看Nginx的配置，打开 `nginx.conf` 文件，找到下面的配置。

```bash
location ~/base {
  proxy_pass                  http://base;
  proxy_set_header Host $host:$server_port;
}
location ~/third {
  proxy_pass                  http://third;
  proxy_set_header Host $host:$server_port;
}
```

**那么问题来了，访问的接口明明是 `/third/system/base/thirdapp/get_detail`，为啥会走到 `/base`下面呢？**

说到这里，相信细心的小伙伴已经发现问题了，**没错，又是运维的锅！！**

### 2.3.解决问题

看了Nginx的配置后，相信很多小伙伴应该都知道如何解决问题了，没错那就是把 `nginx.conf` 中的如下配置。

```bash
location ~/base {
  proxy_pass                  http://base;
  proxy_set_header Host $host:$server_port;
}
location ~/third {
  proxy_pass                  http://third;
  proxy_set_header Host $host:$server_port;
}
```

修改为如下所示。

```bash
location /base {
  proxy_pass                  http://base;
  proxy_set_header Host $host:$server_port;
}
location /third {
  proxy_pass                  http://third;
  proxy_set_header Host $host:$server_port;
}
```

去掉 `~` 符号即可。

接下来，再次模拟访问 `http` 接口，能够正常访问接口。

接下来，将Nginx的debug功能关闭，也就是将 `nginx.conf` 文件中的 `error_log logs/error.log debug;` 配置注释掉，如下所示。

```bash
# error_log  logs/error.log debug;
```

重新加载 `nginx.conf` 文件。

```bash
nginx_service.sh reload
```

最终，将Nginx加入到接入层网关，问题解决。

### 2.4.科普Nginx的转发规则

#### 2.4.1.Nginx的location语法

```bash
location [=|~|~*|^~] /uri/ { … }
```

- `=` 严格匹配。如果请求匹配这个location，那么将停止搜索并立即处理此请求
- `~` 区分大小写匹配(可用正则表达式)
- `~*` 不区分大小写匹配(可用正则表达式)
- `!~` 区分大小写不匹配
- `!~*` 不区分大小写不匹配
- `^~` 如果把这个前缀用于一个常规字符串,那么告诉nginx 如果路径匹配那么不测试正则表达式

**示例1：**

```bash
location  / { }
```

匹配任意请求

**示例2：**

```bash
location ~* .(gif|jpg|jpeg)$ ｛
    rewrite .(gif|jpg|jpeg)$ /logo.png;
｝
```

不区分大小写匹配任何以gif、jpg、jpeg结尾的请求，并将该请求重定向到 /logo.png请求

**示例3：**

```bash
location ~ ^.+\.txt$ {
    root /usr/local/nginx/html/;
}
```

区分大小写匹配以.txt结尾的请求，并设置此location的路径是/usr/local/nginx/html/。也就是以.txt结尾的请求将访问/usr/local/nginx/html/ 路径下的txt文件

#### 2.4.2.alias与root的区别

- root 实际访问文件路径会拼接URL中的路径
- alias 实际访问文件路径不会拼接URL中的路径

示例如下：

```bash
location ^~ /binghe/ {  
   alias /usr/local/nginx/html/binghetic/;  
}
```

- 请求：`http://test.com/binghe/binghe1.html`
- 实际访问：`/usr/local/nginx/html/binghetic/binghe1.html` 文件

```bash
location ^~ /binghe/ {  
   root /usr/local/nginx/html/;  
}
```

- 请求：`http://test.com/binghe/binghe1.html`
- 实际访问：`/usr/local/nginx/html/binghe/binghe1.html` 文件

#### 2.4.3.last 和 break关键字的区别

1. last 和 break 当出现在location 之外时，两者的作用是一致的没有任何差异

2. last 和 break 当出现在location 内部时：

   - last 使用了last 指令，rewrite 后会跳出location 作用域，重新开始再走一次刚才的行为

   - break 使用了break 指令，rewrite后不会跳出location 作用域，其整个生命周期都在当前location中。

#### 2.4.4.permanent 和 redirect关键字的区别

- `rewrite … permanent` 永久性重定向，请求日志中的状态码为301
- `rewrite … redirect` 临时重定向，请求日志中的状态码为302

#### 2.4.5.综合实例

将符合某个正则表达式的URL重定向到一个固定页面

比如：我们需要将符合“/test/(\d+)/[\w-.]+” 这个正则表达式的URL重定向到一个固定的页面。符合这个正则表达式的页面可能是：`http://test.com/test/12345/abc122.html`、`http://test.com/test/456/11111cccc.js` 等

从上面的介绍可以看出，这里可以使用rewrite重定向或者alias关键字来达到我们的目的。因此，这里可以这样做：

1. 使用rewrite关键字

    ```bash
    location ~ ^.+\.txt$ {
        root /usr/local/nginx/html/;
    }
    location ~* ^/test/(\d+)/[\w-\.]+$ {
        rewrite ^/test/(\d+)/[\w-\.]+$ /testpage.txt last;
    }
    ```

	这里将所有符合条件的URL（PS：不区分大小写）都重定向到 `/testpage.txt` 请求，也就是 `/usr/local/nginx/html/testpage.txt` 文件

2. 使用alias关键字

    ```bash
    location ~* ^/test/(\d+)/[\w-\.]+$ {
        alias /usr/local/nginx/html/binghetic/binghe1.html;
    }
    ```

	这里将所有符合条件的URL（不区分大小写）都重定向到 `/usr/local/nginx/html/binghetic/binghe1.html` 文件















