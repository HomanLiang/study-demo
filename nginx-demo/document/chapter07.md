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

同时修改nginx.conf ， 添加如下配置项。

```bash
worker_rlimit_nofile 655350; 
```

**注意：上述配置需要与error_log同级别。**

这样就可以解决Nginx连接过多的问题，Nginx就可以支持高并发（这里需要配置Nginx）。

另外， `ulimit -n`还会影响到MySQL的并发连接数。把它提高，也可以提高MySQL的并发。

**注意： 用 `ulimit -n 655350` 修改只对当前的shell有效，退出后失效。**

### 1.4.永久解决问题

若要令修改ulimits的数值永久生效，则必须修改配置文件，可以给ulimit修改命令放入/etc/profile里面，这个方法实在是不方便。

还有一个方法是修改/etc/security/limits.conf配置文件，如下所示。

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

最后，需要注意的是：要使 limits.conf 文件配置生效，必须要确保 pam_limits.so 文件被加入到启动文件中。查看 /etc/pam.d/login 文件中是否存在如下配置。

```bash
session required /lib64/security/pam_limits.so
```

不存在，则需要添加上述配置项。