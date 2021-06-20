[TOC]



# Spring Boot 部署

## 1.两种运行方式

`Springboot` 应用程序有两种运行方式

- 以 `jar` 包方式运行
- 以 `war` 包方式运行

两种方式应用场景不一样，各有优缺点

### 1.1.jar包运行

通过 `maven` 插件`spring-boot-maven-plugin`，在进行打包时，会动态生成 `jar` 的启动类`org.springframework.boot.loader.JarLauncher`，借助该类对 `springboot` 应用程序进行启动。

**优点**

- 本地无需搭建 `web` 容器，方便开发和调试。
- 因为自带 `web` 容器，可以避免由于 `web` 容器的差异造成不同环境结果不一致问题。
- 一个 `jar` 包就是全部，方便应用扩展。
- 借助容器化，可以进行大规模的部署。

**缺点**

- 应用过于独立，难以统一管理。
- 数据源无法通过界面进行管理。
- 应用体积过大。
- 修改web容器相关配置较为困难，需要借助代码实现。

### 1.2.war包运行

以 `war` 包方式运行，通过 `maven` 插件`spring-boot-maven-plugin`进行相关配置后，最终生成一个可运行在 `tomcat`，`weblogic` 等`java web` 容器中的 `war` 包。

**优点**

- 可以借助 `web` 容器管理界面对应用进行管理。
- 可以管理 `JNDI` 数据源。
- `web` 容器配置较为灵活，配置和程序分离。
- 应用体积较小，甚至可以借助 `web` 容器的包管理功能(比如 `weblogic Library` )进一步减小应用大小。

**缺点**

- 本地需要搭建 `web` 容器，对本地环境要求更高点，学习成本也响应更高。
- 调试较为困难，需要借助 `web` 容器。
- 无法兼容所有 `web` 容器（比如 `spring boot2.x` 无法运行在 `weblogic 11g` 上）。
- 部署较为困难（比如和 `weblogic` 有较多的类冲突）

在实际的项目中，并没有哪一种方式是最好的，根据客户不同的需求制定不同的部署方案，比如有些客户比较看中管理功能，要求数据源和 `tomcat` 相关配置必须由管理员进行管理，那么选择 `war` 包方式，有些客户希望借助容器化进行大规模部署，那么 `jar` 方式更适合。不管选择哪种方式，在部署时都会遇到下面的问题

- 如果需要打 `war` 包，那么不仅是 `pom` 文件需要修改，应用程序也要做相应的改动，改动完后，应用程序就无法本地运行，需要打完包后将配置信息修改回来，这样不仅麻烦，还容易出错。
- 不管是 `war` 包还是 `jar` 包，如何管理不同环境的配置文件，保证不会出错，虽然 `spring boot` 有提供`spring.profiles.active`配置设置不同的环境，但一方面需要人为修改配置文件，只要是人为的就有可能出错，另一方面，客户有时出于安全考虑不会提供生产环境配置信息，那么这时候就无法指定`prifiles.active`。
- 如何将多个 `spring boot` 模块打包在一起。
- jar包需要配合容器化才能发挥出最大的优势，如果没有容器，`spring boot jar` 包就是一个`玩具`，随处运行的 `jar` 包，缺少统一管理，是达不到生产的要求，那么如果从jar包到容器也是一个问题。

早期碰到这些问题，都是人工解决，不仅效率十分低下，部署一次都需要十几分钟，而且很容易出错，一百次出错一次算是概率低了，但是生产出错一次都是重大事件，所以我们也在思考如何通过自动化解决以上问题，如何将开发和部署分离，开发人员只关心开发，开发完提交代码，打包和部署都是后台透明的完成。以下就是我们的解决方案。

## 2.打包

### 2.1.war包打包问题解决

`spring boot` 打 `war` 包的步骤如下

- 在`pom.xml`中将打包方式改为 `war`。

    ```
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        ...
        <packaging>war</packaging>
        ...
    </project>
    ```

- 设置`spring-boot-starter-tomcat`范围为`provided`

    ```
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
        <scope>provided</scope>
    </dependency>
    ```

- 修改 `spring boot` 的启动类，继承`SpringBootServletInitializer`

    ```
    public class DemoApplication extends SpringBootServletInitializer{
        @Override
        protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
            return builder.sources(DemoApplication.class);
        }
    }
    ```

每打包一次都要修改`pom.xml`和启动类，打包完再修改回来，十分的繁琐，因为，我们提出以下整改方案

- 从`pom.xml`复制一个`pom-war.xml`文件，将`pom-war.xml`修改为 `war` 包配置
- 在根目录下(除了 `src` 目录外都可以)复制一份启动类的代码，修改为 `war` 包的配置方式。
- 编写 `shell` 脚本进行打包。

`shell` 脚本打包过程为

1. 备份当前启动类的 `java` 代码。
2. 将 `war` 包启动类的代码替换掉当前启动类的代码。
3. `maven` 指定`pom-war.xml`文件进行打包。
4. 打包结束后恢复启动类文件。

以下就是参考脚本

**app-war.sh**

```
#!/usr/bin/env bash

v1=src/main/java/com/definesys/demo/DemoApplication.java
v2=war/DemoApplication.java
v3=war/DemoApplication-bak.java

cp -rf $v2 $v1

mvn clean package -Dmaven.test.skip=true -f war-pom.xml

#recovery
cp -rf $v3 $v1
```

通过预先配置好 `pom` 文件和启动类文件，开发人员只要运行`app-war.sh`脚本无需修改任何文件即可生成 `war` 包。

**更优的方案**

以上方案 `pom` 文件和启动类文件都需要预先准备好，未实现完全的自动化，通过优化方案做到完全自动化。

- 脚本可以通过 `find` 命令搜索以`*Application.java`结尾的文件，作为启动类文件，读取文件名获取类名，通过字符串替换方式动态生成 `war` 包启动类文件。
- 在 `pom.xml` 中用注释设置好锚点，脚本通过替换锚点动态生成 `pom.xml` 文件。
- 如果不希望通过锚点实现，可以借助更高级的脚本语言，比如 `python` 对 `xml` 进行解析，再动态生成 `xml`。

### 2.2.多模块打包

这里的多模块指的是 `maven` 中的多模块，项目工程中的代码多模块，一个项目按功能划分模块后，在创建工程时一般也按照功能层面上的模块进行创建，这样避免一个模块代码过于庞大，也利于任务的分工，但打包却更麻烦了。

- 每个模块都是独立的 `spring boot `程序，整合到一个包的时候会出现多个启动类，多个配置文件冲突的问题。
- 每个模块有引用相同的依赖，依赖包版本升级后，需要每个 `pom` 文件都做修改。

通过优化项目结构解决以上问题

- 父项目的 `pom` 指定 `spring boot` 的依赖和公共的依赖。
- 创建一个 `spring boot` 的子项目，作为启动项目，我们称为`start`项目。
- 其余子项目为普通的 `java maven` 项目，`parent` 设置为第一步创建的 `spring boot` 父项目。
- `start` 项目的 `pom` 引用其他子项目的依赖。
- 本地调试可以直接运行 `start` 的启动类，`ide` 会自动编译其他模块并引用。
- 打包可以在父项目上进行`install`后再进入start项目进行打包，脚本参考如下

    ```
    mvn clean install
    cd start
    mvn clean package
    ```

	*目录结构如下*

    ```
    .
    ├── pom.xml
    ├── role
    │   ├── pom.xml
    │   └── src
    │       ├── main
    │       │   ├── java
    │       │   │   └── com
    │       │   │       └── definesys
    │       │   │           └── demo
    │       │   │               └── controller
    │       │   │                   └── RoleController.java
    │       │   └── resources
    ├── start
    │   ├── pom.xml
    │   ├── src
    │   │   ├── main
    │   │   │   ├── java
    │   │   │   │   └── com
    │   │   │   │       └── definesys
    │   │   │   │           └── demo
    │   │   │   │               └── DemoApplication.java
    │   │   │   └── resources
    │   │   │       └── application.properties
    └── user
        ├── pom.xml
        └── src
            └── main
                ├── java
                │   └── com
                │       └── definesys
                │           └── demo
                │               └── controller
                │                   └── UserController.java
                └── resources
    ```

- `start` 项目包含包含启动类和配置文件，`pom` 文件引用其余子项目。

	**start pom.xml**

    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <parent>
            <artifactId>blog0915</artifactId>
            <groupId>com.definesys.demo</groupId>
            <version>1.0-SNAPSHOT</version>
        </parent>
        <modelVersion>4.0.0</modelVersion>
        <artifactId>start</artifactId>
        <dependencies>
            <dependency>
                <groupId>com.definesys.demo</groupId>
                <artifactId>user</artifactId>
                <version>1.0.0</version>
            </dependency>
            <dependency>
                <groupId>com.definesys.demo</groupId>
                <artifactId>role</artifactId>
                <version>1.0.0</version>
            </dependency>
        </dependencies>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </project>
    ```

- 父项目 `parent` 为 `spring boot`，引用 `spring boot` 相关依赖和各个子项目公共的依赖

    **父项目 pom.xml**

    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <packaging>pom</packaging>
        <modules>
            <module>user</module>
            <module>role</module>
            <module>start</module>
        </modules>

        <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>2.1.2.RELEASE</version>
            <relativePath/>
        </parent>

        <groupId>com.definesys.demo</groupId>
        <artifactId>blog0915</artifactId>
        <version>1.0-SNAPSHOT</version>

        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
        </dependencies>

    </project>
    ```

- 所有非 `start` 的子项目需要指定版本号并且父项目都设为根目录项目。

	**子项目 pom.xml**

    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <parent>
            <artifactId>blog0915</artifactId>
            <groupId>com.definesys.demo</groupId>
            <version>1.0-SNAPSHOT</version>
        </parent>
        <modelVersion>4.0.0</modelVersion>
        <artifactId>role</artifactId>
        <version>1.0.0</version>
    </project>
    ```

- 所有子项目的包路径前缀必须一样，并且以start项目作为基本路径。

### 2.3.配置文件问题

`spring boot` 提供`spring.profiles.active`指定配置文件，但生产环境有时候客户出于安全考虑不提供配置信息给开发人员，而是预先将配置文件上传到服务器指定路径，程序需要在运行时去引用该配置文件，如果运行环境是`kubernetes`，则会提供一个 `config map`作为配置文件，这时候就要求 `spring boot` 程序读取外部配置文件。

> 这里讨论的是线上环境配置文件方案，本地调试参考子模块打包相关内容，可以将配置文件统一写在start项目中。

#### 2.3.1.jar包外部配置文件读取

`jar` 运行可以通过指定参数`spring.config.location`引用外部文件，命令参考如下：

```
java -jar start-1.0-SNAPSHOT.jar --spring.config.location=/Users/asan/workspace/config
```

`config`目录存放 `properties` 配置文件

可以通过配合`spring.profiles.active`参数可以指定目录下配置文件，如：

```
java -jar start-1.0-SNAPSHOT.jar --spring.profiles.active=prod --spring.config.location=/Users/asan/workspace/config
```

则会读取`/Users/asan/workspace/config/appliction-prod.properties`文件作为配置文件。

#### 2.3.2.war包外部配置文件读取

以`tomcat`为例，需要在tomcat启动时指定`-Dspring.config.location`参数，可以设置服务器环境变量`CATALINA_OPTS`达到目的。可以编辑用户 prifile文件

```
export CATALINA_OPTS=/Users/asan/workspace/config
```

同样，也可以通过`-Dspring.profiles.active`指定配置文件名称。

### 2.4.容器化

`spring boot` 借助容器化，可以如虎添翼，发挥出更大的威力，也只有通过容器化，才能体会到 `spring boot` 开发的高效。通过以上的介绍，你可以很顺利的打好一个 `jar` 包或者 `war` 包，那么可以通过编写 `dockerfile` 文件进行镜像的构建。`spring boot` 在构建镜像时有两个地方需要考虑

- 时区问题，基础镜像的时区默认是`UTC`，比北京时间早8小时，需要指定镜像时区。
- 配置文件问题，需要指定外部配置文件（根据项目具体情况选择）。

**app-jar-dockerfile.Dockerfile**

```
FROM openjdk:8-jdk-alpine

MAINTAINER definesys.com

VOLUME /tmp
ADD start-1.0-SNAPSHOT.jar app.jar
RUN echo "Asia/Shanghai" > /etc/timezone
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","--spring.config.location=/Users/asan/workspace/config","/app.jar"]
```

**app-war.dockerfile.Dockerfile**

```
FROM tomcat

MAINTAINER definesys.com

ENV CATALINA_OPTS -Dspring.config.location=file:/middleware/config/
ADD start-1.0-SNAPSHOT.war /usr/local/tomcat/webapps/app.jar
RUN echo "Asia/Shanghai" > /etc/timezone
EXPOSE 8080
EOF
```

## 3.部署

早期我们采用的是以下部署过程

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401233601.jpeg)

- 首先构建测试环境的镜像，上传到镜像仓库，应用重新部署。
- 接着构建UAT环境的镜像，上传到镜像仓库，应用重新部署。
- 最后构建生产环境的镜像，上传到镜像仓库，应用重新部署。

每一次发布都是一个新的镜像，但这种方式有个问题就是如何保证前一个环境验证没问题，后一个环境就一定没问题，因为两个镜像是不一样的，虽然可能两次构建都是基于同一版本代码，但因为是重新构建，中间可能因为各种原因，如 `maven` 包版本更新等，无法保证两次构建就是完全一样的镜像。因此我们优化了构建的流程，如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401233544.jpeg)

所有的环境都是用同一个镜像，环境之间只有配置文件不同，文件通过 `configmap` 或者外部配置文件方式进行挂载，这样保证了配置文件没问题的前提下，每个环境的程序一定是一样的。

## 4.jenkins自动打包部署

打包和部署在本地进行也是有问题的，本地 `jdk` 版本取决于个人电脑，甚至有黑客污染 `jdk` 导致编译的 `class` 文件自带后门，个人电脑环境也是随着用户不同操作可能改变，构建出来的包不能保证是稳定的包。因此需要一个远程服务器用于打包和部署，能够实现从源码到镜像过程。`jenkins` 是一个基于 `java` 开发的持续集成工具，通过配置插件和编写脚本实现程序从代码到制品再到线上运行的过程。`jenkins` 在 `spring boot` 开发中主要完成了以下工作。

- 通过 `gitlab` 插件实现源代码的获取。
- 基于以上介绍的脚本，实现从源码到制品的过程。
- 通过 `docker` 工具实现从制品到镜像的过程。
- 通过 `kubectl` 工具，实现从镜像到上云的过程。

`jenkins` 在构建镜像时需要借助 `docker` 工具，但 `jenkins` 本身也是有 `docker` 版本的，所以就面临着`docker in docker`的问题，这里选择的方案是用二进制文件安装 `jenkin` 而非镜像方式，虽然丧失了 `docker` 的便利性，但可以简化 `docker` 方案，降低集成的复杂度。



## 5.Spring Boot项目打包瘦身

> 默认情况下，**Spring Boot** 项目发布时会将项目代码和项目的所有依赖文件一起打成一个可执行的 **jar** 包。但如果项目的依赖包很多，那么这个文件就会非常大。这样每次即使只改动一点东西，就需要将整个项目重新打包部署，我们将依赖 **lib** 从项目分离出来，这样每次部署只需要发布项目源码即可。

### 5.1.瘦身打包配置

pringboot默认使用**spring-boot-maven-plugin** 来打包，这个插件会将项目所有的依赖打入项目**jar** 包里面，将打包插件替换为 **maven-jar-plugin**，并拷贝依赖到 **jar** 到外面的 **lib** 目录。

```
<build>
    <plugins>
        <!-- 指定启动类，将依赖打成外部jar包 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <configuration>
                <archive>
                    <!-- 生成的jar中，不要包含pom.xml和pom.properties这两个文件 -->
                    <addMavenDescriptor>false</addMavenDescriptor>
                    <manifest>
                        <!-- 是否要把第三方jar加入到类构建路径 -->
                        <addClasspath>true</addClasspath>
                        <!-- 外部依赖jar包的最终位置 -->
                        <classpathPrefix>lib/</classpathPrefix>
                        <!-- 项目启动类 -->                       <mainClass>vip.codehome.springboot.tutorials.SpringbootTutorialsApplication</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
        <!--拷贝依赖到jar外面的lib目录-->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <executions>
                <execution>
                    <id>copy-lib</id>
                    <phase>package</phase>
                    <goals>
                        <goal>copy-dependencies</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>target/lib</outputDirectory>
                        <excludeTransitive>false</excludeTransitive>
                        <stripVersion>false</stripVersion>
                        <includeScope>runtime</includeScope>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

项目打包时会在target目录生成lib依赖包跟项目jar包，部署时将项目 **jar** 包以及 **lib** 文件夹上传到服务器上，使用java -jar 命令启动即可。如果后续仅仅修改了项目代码，只需上传替换项目 **jar** 包。

![image-20201003072915631](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210401234445.png)

