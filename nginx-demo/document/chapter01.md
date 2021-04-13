[toc]



# Nginx 简介

## 1.什么是 Nginx
官方定义：nginx [engine x]是最初由Igor Sysoev编写的HTTP和反向代理服务器，邮件代理服务器和通用TCP / UDP代理服务器。
- 基本的HTTP服务器功能
- 其他HTTP服务器功能
- 邮件代理服务器功能
- TCP / UDP代理服务器功能
- 体系结构和可扩展性
- 经测试的操作系统和平台

nginx是一款自由的、开源的、高性能的HTTP服务器和反向代理服务器；

同时也是一个IMAP、POP3、SMTP代理服务器；

nginx可以作为一个HTTP服务器进行网站的发布处理，

另外nginx可以作为反向代理进行负载均衡的实现。

## 2.常用功能
### 2.1.反向代理
这是 Nginx 服务器作为 WEB 服务器的主要功能之一，客户端向服务器发送请求时，会首先经过 Nginx 服务器，由服务器将请求分发到相应的 WEB 服务器。正向代理是代理客户端，而反向代理则是代理服务器，Nginx 在提供反向代理服务方面，通过使用正则表达式进行相关配置，采取不同的转发策略，配置相当灵活，而且在配置后端转发请求时，完全不用关心网络环境如何，可以指定任意的IP地址和端口号，或其他类型的连接、请求等。
### 2.2.负载均衡
这也是 Nginx 最常用的功能之一，负载均衡，一方面是将单一的重负载分担到多个网络节点上做并行处理，每个节点处理结束后将结果汇总返回给用户，这样可以大幅度提高网络系统的处理能力；另一方面将大量的前端并发请求或数据流量分担到多个后端网络节点分别处理，这样可以有效减少前端用户等待相应的时间。而 Nginx 负载均衡都是属于后一方面，主要是对大量前端访问或流量进行分流，已保证前端用户访问效率，并可以减少后端服务器处理压力。
### 2.3.Web 缓存
在很多优秀的网站中，Nginx 可以作为前置缓存服务器，它被用于缓存前端请求，从而提高 Web服务器的性能。Nginx 会对用户已经访问过的内容在服务器本地建立副本，这样在一段时间内再次访问该数据，就不需要通过 Nginx 服务器向后端发出请求。减轻网络拥堵，减小数据传输延时，提高用户访问速度。
### 2.4.Rewrite功能
Nginx还提供了一个rewrite功能让我们在请求到达服务器时重写URI,有点类似Servlet Filter的意味，对请求进行一些预处理。

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/nginx-demo/20210414001754.png)

如果我们要实现如果判断请求为POST的话返回405，只需要更改配置为：

```
location ^~  /api/v1 {
    proxy_set_header Host $host;
    if ($request_method = POST){
      return 405;
    }
    proxy_pass http://192.168.1.9:8080/;
}
```
你可以使用Nginx提供的全局变量(如上面配置中的$request_method)或自己设置的变量作为条件，结合正则表达式和标志位（last、break、redirect、permanent）实现URI重写以及重定向。

### 2.5.配置HTTPS
之前很多同学在群里问如何在Spring Boot项目中配置HTTPS,我都推荐使用Nginx来做这个事情。 Nginx比Spring Boot中配置SSL要方便的多，而且不影响我们本地开发。Nginx中HTTPS的相关配置根据下面的改一改就能用：
```
http{
    #http节点中可以添加多个server节点
    server{
        #ssl 需要监听443端口
        listen 443;
        # CA证书对应的域名
        server_name felord.cn;
        # 开启ssl
        ssl on;
        # 服务器证书绝对路径
        ssl_certificate /etc/ssl/cert_felord.cn.crt;
        # 服务器端证书key绝对路径 
        ssl_certificate_key /etc/ssl/cert_felord.cn.key;
        ssl_session_timeout 5m;
        # 协议类型
        ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
        # ssl算法列表 
        ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;
        #  是否 服务器决定使用哪种算法  on/off   TLSv1.1 的话需要开启
        ssl_prefer_server_ciphers on;
        
        location ^~  /api/v1 {
            proxy_set_header Host $host;
            proxy_pass http://192.168.1.9:8080/;
        }
    }
    # 如果用户通过 http 访问 直接重写 跳转到 https 这个是一个很有必要的操作
    server{
        listen 80;
        server_name felord.cn;
        rewrite ^/(.*)$ https://felord.cn:443/$1 permanent;
    }

}
```
这里就用到了rewrite来提高用户体验。

### 2.6.限流
通过对Nginx的配置，我们可以实现漏桶算法和令牌桶算法，通过限制单位时间的请求数、同一时间的连接数来限制访问速度。这一块我并没有深入研究过这里就提一提，你可以查询相关的资料研究。