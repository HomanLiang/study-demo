[toc]



# Druid

## 1.什么是Druid？

Druid（中文译为“德鲁伊”）是阿里巴巴开源的一款 Java 语言中最好的数据库连接池。Druid 提供了强大的监控和扩展功能，当然也包含了数据库的加密功能。

Druid 开源地址：https://github.com/alibaba/druid/

## 2.Druid可以做什么？

1. Druid 可以监控数据库访问性能，Druid 内置提供了一个功能强大的 StatFilter 插件，能够详细统计 SQL 的执行性能，这对于线上分析数据库访问性能有帮助。
2. 替换数据库连接池 DBCP 和 C3P0，Druid 提供了一个高效、功能强大、可扩展性好的数据库连接池。
3. 数据库密码加密，直接把数据库密码写在配置文件中，这是不好的行为，容易导致安全问题。DruidDruiver 和 DruidDataSource 都支持 PasswordCallback。
4. SQL 执行日志，Druid 提供了不同的 LogFilter，能够支持 Common-Logging、Log4j 和 JdkLog，你可以按需要选择相应的 LogFilter，监控你应用的数据库访问情况。
5. 扩展 JDBC，如果你要对 JDBC 层有编程的需求，可以通过 Druid 提供的 Filter-Chain 机制，很方便编写 JDBC 层的扩展插件。



## X.案例

### X.1.阿里巴巴Druid，轻松实现MySQL数据库连接加密！

#### X.1.1.为什么要加密？

现在的开发习惯，无论是公司的项目还是个人的项目，都会选择将源码上传到 Git 服务器（GitHub、Gitee 或是自建服务器），但只要**将源码提交到公网服务器就会存在源码泄漏的风险**，而数据库配置信息作为源码的一部分，一旦出现源码泄漏，那么数据库中的所有数据都会公之于众，其产生的不良后果无法预期（比如某某酒店的信息）。

于是**为了避免这种问题的产生，我们至少要对数据库的密码进行加密操作**，这样即使得到了源码，也不会造成数据的泄露，也算保住了最后一块遮羞布。

#### X.1.2.如何加密？

对于 Java 项目来说，要想快速实现数据库的加密，**最简单可行的方案就是使用阿里巴巴提供的 Druid 来实现加密**。

#### X.1.3.加密执行流程

在没有进行密码加密之前，项目的交互流程是这样的：
![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210506194308.png)

在使用了密码加密之后，项目的交互流程就变成了这样：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210506194323.png)

#### X.1.4.使用Druid实现加密

本示例运行环境：

> Spring Boot 2.4.3
>
> MySQL 5.7
>
> Java 1.8
>
> Idea 2020.1.3

##### X.1.4.1.添加Druid依赖

Maven 项目：

```xml
<dependency>
   <groupId>com.alibaba</groupId>
   <artifactId>druid-spring-boot-starter</artifactId>
   <version>1.2.5</version>
</dependency>
```

Gradle 项目：

```xml
compile 'com.alibaba:druid-spring-boot-starter:1.2.5'
```

获取 Druid 最新版本：https://mvnrepository.com/artifact/com.alibaba/druid-spring-boot-starter

##### X.1.4.2.生成密文

Druid 添加完成之后就可以借助 Druid 中提供的 `ConfigTools` 类来加密密码了，实现代码如下：

```java
import com.alibaba.druid.filter.config.ConfigTools;

class MyTests {
    public static void main(String[] args) throws Exception {
        // 需要加密的明文命名
        String password = "youPassword"; // 【注意：这里要改为你自己的密码】
        // 调用 druid 生成私钥、公钥、密文
        ConfigTools.main(new String[]{password});
    }
}
```

以上代码执行的结果如下：

> privateKey:MIIBUwIBADANBgkqhkiG9w0BAQEFAASCAT0wggE5AgEAAkEApOjcMWSDzJiKVGmtcBBoQPtM9tVW2H2cnS6xZK7NrbzQXYWLQD2zefIrrx9vMvqRIHEqkmAHTuUcUXHgCxu0cwIDAQABAkAlqo5ItdWo0Jqf5zdXJlg5p2yP4HCiqCYyfKzF+2s9KEmgWZJWTctZDsgQ0iYUohORR59I+J4nabhel1x5/INpAiEA6jwSyFqMUPOh1XlrzNFek+RthOQ5n4+ALPo+vULayO0CIQC0O7JM9sIq+tg+jCGv+ypk6vbuRKY9m5W2rSRXapGm3wIgRHul3jAjIDPrF/f1HaAFL+Y0Yws7Ebyp8/yCRWF7iA0CIALbe20q8FMcHPeI4zPWCIsHCpkmb3hEkjAOOKhGIT8DAiAqiUuz92NqKeyjmOfons1ka65EzVwA3NDhZ6+IQcnuig==
> publicKey:MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKTo3DFkg8yYilRprXAQaED7TPbVVth9nJ0usWSuza280F2Fi0A9s3nyK68fbzL6kSBxKpJgB07lHFFx4AsbtHMCAwEAAQ==
> password:IMgKm27bOHok3/+5aDL4jGBoVVZkpicbbM6pIXQppi3dI7h3jngSAqhqwqYnfuYpyVJ0k++q9xWWnHtd6sAWnQ==

从上述结果可以看出，使用 `ConfigTools` 类会生成 3 部分的内容：

1. privateKey：私钥，暂时不会用到，用于密码的加密；
2. publicKey：公钥，用于密码的解密；
3. password：加密之后的密码。

> PS：要实现数据库的加密，主要使用的是 publicKey（公钥）和 password（密文），这就把明文转换成密文了。

##### X.1.4.3.添加配置

完成了以上操作之后，只需要将上一步生成的**公钥**和**密文**添加到项目的配置文件 application.yml（或application.xml）中就实现了加密操作了，具体配置信息如下：

```xml
spring:
  # MySQL 配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      url: jdbc:mysql://127.0.0.1:3306/testdb?serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&useSSL=false
      username: root
      password: IMgKm27bOHok3/+5aDL4jGBoVVZkpicbbM6pIXQppi3dI7h3jngSAqhqwqYnfuYpyVJ0k++q9xWWnHtd6sAWnQ==
      # encrypt config
      filters: config
      connect-properties:
        config.decrypt: true
        config.decrypt.key: MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKTo3DFkg8yYilRprXAQaED7TPbVVth9nJ0usWSuza280F2Fi0A9s3nyK68fbzL6kSBxKpJgB07lHFFx4AsbtHMCAwEAAQ==
```

其中 password 对应的是上一步生成的 password（密文），而 config.decrypt.key 对应的是上一步生成的 publicKey（公钥），如下图所示：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210506194348.png)

这里提供一个原始的配置文件，以便和加密后的配置文件进行比对：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210506194438.png)

##### X.1.4.4.注意事项-插着钥匙的锁

经过前面 3 步的配置之后，我们的程序就可以正常运行了，但这远没有结束！

在第 3 步配置时，我们将密文和公钥都写入配置文件，这就会造成**当有人拿到密文和公钥之后，就可以使用 Druid 将加密的密码还原出来了**，这就好比一把插着钥匙的锁是极不安全的。

因此我们**正确的使用姿势：是将公钥找一个安全的地方保存起来，每次在项目启动时动态的将公钥设置到项目中**，这样就可以有效的保证密码的安全了。

**正确的配置文件**

接下来我们将 Spring Boot 的公钥设置为配置项，在项目运行时再替换为具体的值，最终的安全配置信息如下：

```xml
spring:
  # MySQL 配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      url: jdbc:mysql://127.0.0.1:3306/testdb?serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&useSSL=false
      username: root
      password: IMgKm27bOHok3/+5aDL4jGBoVVZkpicbbM6pIXQppi3dI7h3jngSAqhqwqYnfuYpyVJ0k++q9xWWnHtd6sAWnQ==
      # encrypt config
      filters: config
      connect-properties:
        config.decrypt: true
        config.decrypt.key: ${spring.datasource.druid.publickey}
```

可以看出公钥被修改成“${spring.datasource.druid.publickey}”了，这就相当于使用占位符先把坑给占上，等项目启动时再更换上具体的值。

> PS：“spring.datasource.druid.publickey”并非是固定不可变的 key，此 key 值用户可自行定义。

**开发环境替换公钥**

开发环境只需要在 Idea 的启动参数中配置公钥的值即可，如下图所示：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210506194447.png)

当我们输入正确的公钥值时程序可以正常运行，当输入一个错误的公钥值时就会提示解码失败，如下图所示：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210506194535.png)

**生产环境替换公钥**

生产环境在启动 jar 包时只需要动态设置公钥的值即可，参考以下命令：

> java -jar xxx.jar --spring.datasource.druid.publickey=你的公钥

#### X.1.5.Druid运行原理

经过上述步骤之后，我们就完成 MySQL 密码的加密了，这样当 Spring Boot 项目启动时，**Druid 的拦截器会使用密文和公钥将密码还原成真实的密码以供项目使用**，当然这一切都无需人工干预（无需编写任何代码），Druid 已经帮我封装好了，我们只需要通过以上配置即可。

什么？你想知道 Druid 是如何通过密文和公钥还原出真实的密码的？

没问题，满足你，其实 `ConfigTools` 类中已经提供了相应实现，代码如下：

```java
// 密文
String password = "VwH1mu2IUpqjfKTd+gSikiZgJTi+3Y5zFIFRfxYnH1UqHzm1K8TIHnMaV3TErBaGsVEaGV0e63pb0Ys3Wdm7Kg==";
// 公钥
String publicKey = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALWIEp19IM04sB+vQXnEOH9gFNFdL5TFGSEhORgHj4MnfTfBSNaOoSgCaM8BOpjiHmwuEb7LpvmXI1x/ymUvNzECAwEAAQ==";
// 还原成真实的密码
String result = ConfigTools.decrypt(publicKey, password);
System.out.println("最终结果：" + result);
```

#### X.1.6.总结

本文我们使用阿里巴巴开源的 Druid 实现了 MySQL 的密码加密，Druid 的加密过程非常方便，无需编写任何代码，只需要添加 Druid 依赖，再通过 Druid 的工具类生成密文，最后将密文配置到 application.yml 文件即可。项目在运行时会通过拦截器将密文转换成真正的密码，从而实现了 MySQL 密码的加密和解码的过程。























