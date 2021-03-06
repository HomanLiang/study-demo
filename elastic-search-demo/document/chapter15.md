[toc]



# ElasticSearch 集群安全

在安装完 ES 后，ES 默认是没有任何安全防护的。

ES 的[安全管理](https://www.elastic.co/guide/en/elasticsearch/reference/current/secure-cluster.html)主要包括以下内容：

- [身份认证](https://www.elastic.co/guide/en/elasticsearch/reference/current/setting-up-authentication.html)：鉴定访问用户是否合法。
- [用户鉴权](https://www.elastic.co/guide/en/elasticsearch/reference/current/authorization.html)：设置用户有哪些访问权限。
- [传输加密](https://www.elastic.co/guide/en/elasticsearch/reference/current/encrypting-communications.html)：数据在传输的过程中，要加密。
- [日志审计](https://www.elastic.co/guide/en/elasticsearch/reference/current/auditing.html)：记录集群操作。
- 等

这里有一些免费的安全方案：

- 设置 Nginx 方向代理。
- 安装免费的安全插件，比如：
  - [Search Guard](https://search-guard.com/)：一个安全和报警的 ES 插件，分收费版和免费版。
  - [Readonly REST](https://github.com/sscarduzio/elasticsearch-readonlyrest-plugin)
- X-Pack 的 Basic 版：可参考[这里](https://www.elastic.co/what-is/elastic-stack-security)。



## 身份认证

ES 中提供的认证叫做 [Realms](https://www.elastic.co/guide/en/elasticsearch/reference/current/realms.html)，有以下几种方式，可分为两类：

- 内部的：不需要与 ES 外部方通信。
  - [file](https://www.elastic.co/guide/en/elasticsearch/reference/current/file-realm.html)（免费）：用户名和密码保存在 ES 索引中。
  - [native](https://www.elastic.co/guide/en/elasticsearch/reference/current/native-realm.html)（免费）：用户名和密码保存在 ES 索引中。
- 外部的：需要与 ES 外部组件通信。
  - [ldap](https://www.elastic.co/guide/en/elasticsearch/reference/current/ldap-realm.html)（收费）
  - [active_directory](https://www.elastic.co/guide/en/elasticsearch/reference/current/active-directory-realm.html)（收费）
  - [pki](https://www.elastic.co/guide/en/elasticsearch/reference/current/pki-realm.html)（收费）
  - [saml](https://www.elastic.co/guide/en/elasticsearch/reference/current/saml-realm.html)（收费）
  - [kerberos](https://www.elastic.co/guide/en/elasticsearch/reference/current/kerberos-realm.html)（收费）



## 用户鉴权

用户鉴权通过定义一个角色，并分配一组权限；然后将角色分配给用户，使得用户拥有这些权限。

ES 中的[权限](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-privileges.html)有不同的级别，包括集群级别（30 多种）和索引级别（不到 20 种）。

ES 中提供了很多[内置角色](https://www.elastic.co/guide/en/elasticsearch/reference/current/built-in-roles.html)（不到 30 种）可供使用。

ES 中提供了很多关于用户与角色的 API：

- 关于用户：
  - [Change passwords](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-change-password.html)：修改密码。
  - [Create or update users](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-put-user.html)：创建更新用户。
  - [Delete users](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-delete-user.html)：删除用户。
  - [Enable users](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-enable-user.html)：打开用户。
  - [Disable users](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-disable-user.html)：禁止用户。
  - [Get users](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-get-user.html)：查看用户信息。
- 关于角色：
  - [Create or update roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-put-role.html)：创建更新角色。
  - [Delete roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-delete-role.html)：删除角色。
  - [Get roles](https://www.elastic.co/guide/en/elasticsearch/reference/current/security-api-get-role.html)：查看角色信息。



## 启动 ES 安全功能

下面演示如何使用 ES 的安全功能。

启动 ES 并通过 [xpack.security.enabled](https://www.elastic.co/guide/en/kibana/current/security-settings-kb.html) 参数打开安全功能：

```shell
bin\elasticsearch -E node.name=node0 -E cluster.name=mycluster -E path.data=node0_data -E http.port=9200 -E xpack.security.enabled=true
```

使用 [elasticsearch-setup-passwords](https://www.elastic.co/guide/en/elasticsearch/reference/current/setup-passwords.html) 命令启用 ES内置用户及初始 6 位密码（需要手动输入，比如是 `111111`）：

```shell
bin\elasticsearch-setup-passwords interactive
```

该命令会启用下面这些用户：

- **elastic**：超级用户。
- **kibana**：用于 ES 与 Kibana 之间的通信。
- **kibana_system**：用于 ES 与 Kibana 之间的通信。
- **apm_system**
- **logstash_system**
- **beats_system**
- **remote_monitoring_user**

启用 ES 的安全功能后，访问 ES 就需要输入用户名和密码：

![image-20210306170435857](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306170435857.png)

也可以通过 **curl** 命令（并指定用户）来访问 ES：

```shell
curl -u elastic 'localhost:9200'
```

更多内容可参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/7.10/configuring-security.html)。



## 启动 Kibana 安全功能

打开 Kibana 的配置文件 `kibana.yml`，写入下面内容：

```shell
elasticsearch.username: "kibana_system"  # 用户名
elasticsearch.password: "111111"         # 密码
```

然后使用 `bin\kibana` 命令启动 Kibana。

访问 Kibana 也需要用户和密码（这里使用的是超级用户）：

![image-20210306170459839](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306170459839.png)



## 使用 Kibana 创建角色和用户

下面演示如何使用 Kibana 创建角色和用户。登录 Kibana 之后进行如下操作：

![20210125182334697](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210125182334697.png)

点击 `Stack Management` 后进入下面页面：

![20210125182437975](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210125182437975.png)

### 1，创建角色

点击 `Create role` 创建角色：

![image-20210306170641791](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306170641791.png)

创建角色需要填写如下内容：

- 角色名称
- 角色对哪些索引有权限及索引的权限级别
- 添加一个 Kibana 权限
- 最后创建角色

![image-20210306170703567](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306170703567.png)

经过上面的操作，创建的角色名为 `test_role`，该角色对 `test_index` 索引有**只读权限**；如果进行超越范围的操作，将发生错误。

### 2，创建用户

进入到创建用户的界面，点击 `Create user` 创建用户：

![image-20210306170726008](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306170726008.png)

填写用户名和密码，并将角色 `test_role` 赋予该用户。

![20210125213636132](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/20210125213636132.png)

### 3，使用用户

使用新创建的用户登录 Kibana：

![image-20210306170817282](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/elastic-search-demo/image-20210306170817282.png)

该用户只对 `test_index` 索引有**只读权限**；如果进行超越范围的操作，将发生错误。



## 传输加密

传输加密指的是在数据的传输过程中，对数据进行加密（可防止数据被抓包）。

传输加密分为**集群内**加密和**集群间**加密：

- 集群内加密指的是 ES 集群内部各节点之间的数据传输时的加密。
  - 通过 TLS 协议完成。
- 集群间加密指的是外部客户访问 ES 时，数据传输的加密。
  - 通过 HTTPS 协议完成。

更多的内容可参考[这里](https://www.elastic.co/guide/en/elasticsearch/reference/current/configuring-tls.html)。

### 1，集群内部传输加密

在 ES 中可以使用 TLS 协议对数据进行加密，需要进行以下步骤：

- 创建 CA
- 为 ES 节点创建证书和私钥
- 配置证书

**1.1，创建 CA 证书**

使用如下命令创建 CA：

```shell
bin\elasticsearch-certutil ca
```

成功后，可以看到当前文件夹下多了一个文件：

```shell
elastic-stack-ca.p12
```

**1.2，生成证书和私钥**

使用如下命令为 ES 中的节点生成证书和私钥

```shell
bin\elasticsearch-certutil cert --ca elastic-stack-ca.p12
```

成功后，可以看到当前文件夹下多了一个文件：

```shell
elastic-certificates.p12
```

**1.3，配置证书**

将创建好的证书 `elastic-certificates.p12` 放在 `config/certs` 目录下。

**1.4，启动集群**

```shell
# 启动第一个节点
bin\elasticsearch 
-E node.name=node0 
-E cluster.name=mycluster
-E path.data=node0_data 
-E http.port=9200 
-E xpack.security.enabled=true 
-E xpack.security.transport.ssl.enabled=true 
-E xpack.security.transport.ssl.verification_mode=certificate 
-E xpack.security.transport.ssl.keystore.path=certs\elastic-certificates.p12 
-E xpack.security.transport.ssl.truststore.path=certs\elastic-certificates.p12

# 启动第二个节点
bin\elasticsearch 
-E node.name=node1 
-E cluster.name=mycluster 
-E path.data=node1_data 
-E http.port=9201 
-E xpack.security.enabled=true 
-E xpack.security.transport.ssl.enabled=true 
-E xpack.security.transport.ssl.verification_mode=certificate 
-E xpack.security.transport.ssl.keystore.path=certs\elastic-certificates.p12 
-E xpack.security.transport.ssl.truststore.path=certs\elastic-certificates.p12
```

不提供证书的节点将无法加入集群：

```shell
bin\elasticsearch 
-E node.name=node2 
-E cluster.name=mycluster 
-E path.data=node2_data 
-E http.port=9202 
-E xpack.security.enabled=true 
-E xpack.security.transport.ssl.enabled=true 
-E xpack.security.transport.ssl.verification_mode=certificate
# 加入失败
```

也可以将配置写在配置文件 `elasticsearch.yml` 中，如下：

```shell
xpack.security.transport.ssl.enabled: true
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.keystore.path: certs/elastic-certificates.p12
xpack.security.transport.ssl.truststore.path: certs/elastic-certificates.p12
```

### 2，集群外部传输加密

通过配置如下三个参数，使得 ES 支持 HTTPS：

```shell
xpack.security.http.ssl.enabled: true
xpack.security.http.ssl.keystore.path: certs/elastic-certificates.p12
xpack.security.http.ssl.truststore.path: certs/elastic-certificates.p12
```

在命令行启动：

```shell
bin\elasticsearch 
-E node.name=node0 
-E cluster.name=mycluster
-E path.data=node0_data 
-E http.port=9200 
-E xpack.security.enabled=true 
-E xpack.security.transport.ssl.enabled=true 
-E xpack.security.transport.ssl.verification_mode=certificate 
-E xpack.security.transport.ssl.keystore.path=certs\elastic-certificates.p12 
-E xpack.security.transport.ssl.truststore.path=certs\elastic-certificates.p12
-E xpack.security.http.ssl.enabled=true 
-E xpack.security.http.ssl.keystore.path=certs\elastic-certificates.p12 
-E xpack.security.http.ssl.truststore.path=certs\elastic-certificates.p12
```

启动成功后，可以通过 HTTPS 协议访问 ES：

```shell
https://localhost:5601/
```

### 3，配置 Kibana 链接 ES HTTPS

**3.1，为 Kibana 生成 pem 文件**

首先用 `openssl` 为 kibana 生成 pem：

```shell
openssl pkcs12 -in elastic-certificates.p12 -cacerts -nokeys -out elastic-ca.pem
```

成功后会生成如下文件：

```shell
elastic-ca.pem
```

将该文件放在 `config\certs` 目录下。

**3.2，配置 kibana.yml**

在 Kibana 的配置文件 `kibana.yml` 中配置如下参数：

```shell
elasticsearch.hosts: ["https://localhost:9200"]
elasticsearch.ssl.certificateAuthorities: ["C:\\elasticsearch-7.10.1\\config\\certs\\elastic-ca.pem"]
elasticsearch.ssl.verificationMode: certificate
```

**3.3，运行 Kibana**

```shell
bin\kibana
```

### 4，配置 Kibana 支持 HTTPS

**4.1，为 Kibana 生成 pem**

```shell
bin/elasticsearch-certutil ca --pem
```

上面命令执行成功后会生成如下 **zip** 文件：

```shell
elastic-stack-ca.zip
```

将该文件解压，会有两个文件：

```shell
ca.crt
ca.key
```

将这两个文件放到 Kibana 的配置文件目录 `config\certs`。

**4.2，配置 kibana.yml**

在 Kibana 的配置文件 `kibana.yml` 中配置如下参数：

```shell
server.ssl.enabled: true
server.ssl.certificate: config\\certs\\ca.crt
server.ssl.key: config\\certs\\ca.key
```

**4.3，运行 Kibana**

```shell
bin\kibana
```

启动成功后，可以通过 HTTPS 协议访问 Kibana：

```shell
https://localhost:5601/
```













参考文档：

[ElasticSearch 集群安全](https://www.cnblogs.com/codeshell/p/14467692.html)