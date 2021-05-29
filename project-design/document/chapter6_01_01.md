[toc]



# Maven

## 1.Maven概念

`Maven` 是一个项目管理和整合工具。`Maven` 为开发者提供了一套完整的构建生命周期框架。开发团队几乎不用花多少时间就能够自动完成工程的基础构建配置，因为 `Maven` 使用了一个标准的目录结构和一个默认的构建生命周期。

若有多个开发团队环境的情况下，`Maven` 能够在很短的时间内使得每项工作都按照标准进行。因为大部分的工程配置都非常简单且可复用，在创建报告、检查、构建和测试自动配置时，`Maven` 可以让开发者的工作变得更简单。

`Maven` 的主要目的是为开发者提供：

- 一个可复用、可维护、更易理解的工程综合模型，与这个模型交互的插件或工具
- `Maven` 工程结构和内容定义在一个 `xml` 文件中（一般是 `pom.xml`）

利用 `Maven` 构建项目过程阶段：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424105509.png)

## 2.Maven安装配置

- `JDK` 安装

  `Maven` 是基于 `Java` 的工具，所以配置 `Maven` 要做的第一件事就是安装 `JDK`

- Windows安装

  Maven下载地址：http://maven.apache.org/download.html

  下载解压后，添加环境变量即完成

- Linux安装

  - 配置yum源

    ```
    # sudo yum install -y yum-utils
    
    # yum-config-manager --add-repo http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo 
    ```

  - 安装Maven

    ```
    # yum install -y apache-maven
    ```

- 完成安装后，通过mvn --version 检验安装版本

## 3.Maven初体验

`Maven` 主要是用来打 `jar`、`war` 包以及管理 `jar` 包

### 3.1.原始的javac打包方式

假如我们有一个 `Hello.java` 文件，想要将它打成可执行 `jar` 包，一般这样做。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110250.png)

使用 `javac` 命令和 `jar` 命令打包：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110308.png)

生成的 `jar` 包文件，由两部分组成，`class` 文件和 `META-INF` 目录，如下：

此时的 `jar` 包，是不可直接运行的，需要指定入口 `main` 类

进入 `META-INF`  目录，编辑 `MANIFEST.MF` 文件，如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110322.png)

运行此 `jar` 包，得到运行结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110330.png)

### 3.2.maven打包

在 `pom` 文件内配置入口类

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110345.png)

执行 `maven` 打包命令

```
mvn package
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110400.png)

运行生成的 `jar`，结果与原始方式无区别

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110408.png)

## 4.POM结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110421.png)

## 5.Maven流程

### 5.1.maven的理想

`maven` 像一种什么设计模式？答案：模板方法模式

自动走完标准的构建流程：`清理->编译->测试->报告->打包->部署`

统一入口，所有配置在一个 `pom` 里搞定

## 6.maven的约定

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110510.png)

约定的目录（不可改）：

| `src/main/java` –存放项目的 `.java` 文件                     |
| ------------------------------------------------------------ |
| `src/main/resources` –存放项目资源文件。比方 `spring`,`hibernate`配置文件 |
| `src/test/java` –存放全部測试 `.java` 文件，比方 `JUnit` 測试类 |
| `src/test/resources` ---測试资源文件                         |
| `target` ---项目输出位置,编译完毕后的东西放到这里面          |
| `pom.xml`                                                    |

### 6.1.maven的生命周期

`maven` 的构建生命周期，只是一个抽象的规范流程。周期内的每个阶段的具体执行，是在插件里面来实现的。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110537.png)

各个生命周期预设的定义如下：

| 阶段          | 处理     | 描述                                                     |
| ------------- | -------- | -------------------------------------------------------- |
| 验证 validate | 验证项目 | 验证项目是否正确且所有必须信息是可用的                   |
| 编译 compile  | 执行编译 | 源代码编译在此阶段完成                                   |
| 测试 Test     | 测试     | 使用适当的单元测试框架（例如JUnit）运行测试。            |
| 包装 package  | 打包     | 创建JAR/WAR包如在 pom.xml 中定义提及的包                 |
| 检查 verify   | 检查     | 对集成测试的结果进行检查，以保证质量达标                 |
| 安装 install  | 安装     | 安装打包的项目到本地仓库，以供其他项目使用               |
| 部署 deploy   | 部署     | 拷贝最终的工程包到远程仓库中，以共享给其他开发人员和工程 |

运行任何一个阶段，都会从其所在生命周期的第一个阶段开始，顺序执行到指定的阶段，如：

`mvn package`（本义：执行 `default` 周期的 `package` 阶段，`maven` 会自动从 `process-resources` 阶段开始运行到 `package` 阶段结束）

### 6.2.maven的插件

插件 `plugin` 是绑定到生命周期，承担实际功能的组件。`mvn` 运行时，自动关联插件来运行

下图是 `maven` 默认的各阶段对应的插件列表：

| 生命周期 | 生命周期阶段           | 插件目标                                                | 执行任务                     |
| -------- | ---------------------- | ------------------------------------------------------- | ---------------------------- |
| clean    | pre-clean              |                                                         |                              |
|          | clean                  | maven-clean-plugin:clean                                | 删除项目的输出目录。         |
|          | post-clean             |                                                         |                              |
| site     | pre-site               |                                                         |                              |
|          | site                   | maven-site-plugin:site                                  |                              |
|          | post-site              |                                                         |                              |
|          | site-deploy            | maven-site-plugin:deploy                                |                              |
| default  | process-resources      | maven-resources-plugin:resources                        | 复制主资源文件至主输出目录   |
|          | compile                | maven-compiler-plugin:compile                           | 编译主代码至主输出目录       |
|          | process-test-resources | maven-resources-plugin:testResources                    | 复制测试资源文件至测试输出目 |
|          | test-compile           | maven-compiler-plugin:testCompile                       | 编译测试代码至测试输出目录   |
|          | test                   | maven-surefire-plugin:test                              | 执行测试用例                 |
|          | package                | maven-jar-plugin:jar（ejb:ejb jar:jar rar:rar war:war） | 创建项目jar包                |
|          | install                | maven-install-plugin:install                            | 将项目输出构件安装到本地仓库 |
|          | deploy                 | maven-deploy-plugin:deploy                              | 将项目输出构件部署到远程仓库 |

## 7.常用Maven命令

`mvn clean` 清理

`mvn compile` 编译主程序

`mvn package` 打包

`mvn install` 安装jar到本地库

使用 `maven` 命令生成项目（`idea` 和 `eclipse` 生成项目最终也是依赖 `maven` 插件生成的）：

```
mvn archetype:generate -DgroupId=enjoy -DartifactId=simple -DarchetypeArtifactId=maven-archetype-quickstart -Dversion=1.0

mvn archetype:generate -DgroupId=enjoy -DartifactId=simple-web -DarchetypeArtifactId=maven-archetype-webapp -Dversion=1.0 
```

## 8.Maven插件开发

可以自定义插件，来扩展maven的功能。插件的开发步骤如下：

- 引入 `maven api` 依赖

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110844.png)

- 编写简单Mojo类（继承AbstractMojo）

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110858.png)

- 执行插件

  ```
  mvn com.enjoy:enjoy-plugin:1.0:log
  ```

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110918.png)

- 关联插件到生命周期来执行

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110930.png)

- 构建项目对应的生命周期

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424110952.png)

## 9.Maven坐标与依赖

### 9.1.坐标

--------在数学中, 任何一个坐标可以唯一确定一个“点”

`Maven` 中坐标是 `Jar` 包的唯一标识

坐标元素包括 `groupId`、`artifactId`、`version`、`packaging`：

| 元素       | 描述                            | 说明                                                         |
| ---------- | ------------------------------- | ------------------------------------------------------------ |
| groupId    | 定义当前模块隶属的实际Maven项目 | 中小企业常常直接对应公司、组织                               |
| artifactId | 定义实际项目中的一个Maven模块   | 唯一标识一个模块                                             |
| version    | 定义当前项目所属版本            | SNAPSHOT：表示不稳定版本LATEST：指最新发布的版本，可能是个发布版，也可能是一个snapshot版本RELEASE：指最后一个发布版 |
| packaging  | 定义Maven项目打包方式           | 有jar（默认）、war、pom、maven-plugin等                      |
| dassifier  | 附属构建（如javadoc、sources）  | 须有附加插件的帮助                                           |

### 9.2.依赖

依赖即：`A->B`，`B->C`，`C->D` 这种项目间的依存关系。

在 `java` 的 `jvm` 内，依赖的最终表现是，项目A启动时，其依赖的 `jar` 包必须都对应放入其 `classpath` 路径内。

### 9.3.依赖传递

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113652.jpeg)

上述过程中，项目 `Mall` 归结起来，依赖的 `fastjson` 会有三个版本。

而我们的jvm最终肯定只能接受一个版本的jar，所以必须有所取舍。

maven默认的取舍规则是：

- 路径最短原则：`product` 和 `customer` 里的 `fastjson` 引用路径较短，路径为两步；`pay` 项目里的 `fastjson` 引用路径较长，路径为三步。因此 `pay` 中的 `fastjson` 被淘汰；

- 同路径长度下，谁先声明谁优先：`product` 和 `customer` 中的 `fastjson` 路径相同，那么就看在 `pom` 中是先声明 `product` 还是先声明 `customer`，谁先用谁的。

### 9.4.依赖冲突及解决

在依赖传递里，我们看到，`maven` 根据自己的规则为我们取舍出了一个版本的jar，但此jar版本选择可能会与我们的项目预期不符：

例如：我们最终想的版本是 `fastjson:1.2.30` 版本（但它在第一步即被淘汰掉了）

当出现此类情况时，我们项目运行可能会出错（项目中使用到了 `1.2.30` 版本的特性），此问题即是我们常遇到的jar包冲突问题。

补救方式：使用 `exclusions` 将 `product` 和 `customer` 中的 `fastjson` 包排除掉，用法如下图：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113739.png)

当发生jar冲突程序报错时，可以使用mvn命令查出项目最终依赖的jar包树，看版本是否是我们预期的：

命令：`mvn dependency:tree`

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113753.png)

### 9.5.依赖范围scope

`mvn` 在运行时，生命周期的不同阶段，会有不同的依赖范围，一般有以下依赖范围 `scope`：

`- compile`：默认范围，用于编译（依赖的 `jar` 在打包时会包含进去）    

`- provided`：类似于编译，但支持你期待 `jdk` 或者容器提供，类似于 `classpath`（依赖的 `jar` 在打包时不会包含进去）

`- runtime`：在执行时需要使用（依赖的 `jar` 在打包时会包含进去）

`- test`：用于test任务时使用（依赖的 `jar` 在打包时不会包含进去）

`- system`：需要外在提供相应的元素。通过 `systemPath` 来取得（一般禁止使用）

每个 `scope` 实际上是配置了一个不同的 `classpath`，`jvm` 根据选择不同的 `classpath` 来达到依赖不同

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113812.png)

### 9.6.项目聚合与继承

**9.6.1.聚合**

聚合是指将多个模块整合在一起，统一构建，避免一个一个的构建。聚合需要个父工程，然后使用 `<modules></modules>` 进行配置其中对应的是子工程的相对路径。例如下面的配置。

```xml
<modules>
  <module>mykit-dao</module>
  <module>mykit-service</module>
</modules>
```

**9.6.2.继承**

继承是指子工程直接继承父工程 当中的属性、依赖、插件等配置，避免重复配置。继承包括如下几种方式。

- 属性继承
- 依赖继承
- 插件继承

**注意：上面的三个配置子工程都可以进行重写，重写之后以子工程的为准。**

**9.6.3.依赖管理**

通过继承的特性，子工程是可以间接依赖父工程的依赖，但多个子工程依赖有时并不一至，这时就可以在父工程中加入`<dependencyManagement></dependencyManagement>` 声明该工程需要的JAR包，然后在子工程中引入。例如下面的配置。

```xml
<!-- 父工程中声明 junit 4.12 -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.12</version>
    </dependency>
  </dependencies>
</dependencyManagement>
<!-- 子工程中引入 -->
<dependency>
  <groupId>junit</groupId>
  <artifactId>junit</artifactId>
</dependency>
```

**9.6.4.项目属性**

通过 `<properties></properties>` 配置属性参数，可以简化配置。例如下面的配置。

```xml
<!-- 配置proName属性 -->
<properties>
  <projectName>projectName</projectName>
</properties>
```

我们可以在 `pom.xml` 文件中使用下面的形式来引入配置的参数。

```xml
${projectName}
```

接下来，我们再来看几个Maven的默认属性，如下所示。

- `${basedir}` 项目根目录
- `${version}` 表示项目版本;
- `${project.basedir}` 同 `${basedir}`;
- `${project.version}` 表示项目版本,与 `${version}` 相同;
- `${project.build.directory}` 构建目录，缺省为 `target`
- `${project.build.sourceEncoding}` 表示主源码的编码格式;
- `${project.build.sourceDirectory}` 表示主源码路径;
- `${project.build.finalName}` 表示输出文件名称;
- `${project.build.outputDirectory}` 构建过程输出目录，缺省为 `target/classes`

### 9.7.项目构建配置

**9.7.1.构建资源配置**

基本配置示例：

```xml
<defaultGoal>package</defaultGoal>
<directory>${basedir}/target2</directory>
<finalName>${artifactId}-${version}</finalName>
```

说明：

- `defaultGoal`：执行构建时默认的 `goal` 或 `phase`，如 `jar:jar` 或者 `package` 等
- `directory`：构建的结果所在的路径，默认为 `${basedir}/target` 目录
- `finalName`：构建的最终结果的名字，该名字可能在其他 `plugin` 中被改变

**9.7.2.resources 配置示例**

```xml
<resources>
  <resource>
   <directory>src/main/java</directory>
   <includes>
     <include>**/*.MF</include>
     <include>**/*.xml</include>
   </includes>
   <filtering>true</filtering>
  </resource>
  <resource>
   <directory>src/main/resources</directory>
   <includes>
     <include>**/*</include>
     <include>*</include>
   </includes>
   <filtering>true</filtering>
  </resource>
 </resources>
```

说明：

- `resources`：`build` 过程中涉及的资源文件
- `targetPath`：资源文件的目标路径
- `directory`：资源文件的路径，默认位于 `${basedir}/src/main/resources/` 目录下
- `includes`：一组文件名的匹配模式，被匹配的资源文件将被构建过程处理
- `excludes`：一组文件名的匹配模式，被匹配的资源文件将被构建过程忽略。同时被 `includes` 和 `excludes` 匹配的资源文件，将被忽略。
- `filtering`：默认 `false` ，`true` 表示 通过参数 对 资源文件中的 `${key}` 在编译时进行动态变更。替换源 `-Dkey` 和 `pom` 中的 值 或 中指定的 `properties` 文件。

## 10.环境激活-profiles使用

在 `springmvc` 项目中，开发/测试/线上三个不同环境，配置文件往往也不同。

打包时需要对配置文件做出选择（`maven` 提供了 `profiles` 机制供我们使用）。

### 10.1.profiles的场景

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113848.png)

这个选择，实际发生在 `default` 生命周期的 `resource` 阶段（`maven-resources-plugin` 执行过程里）

### 10.2.定义profiles

为了指导插件将对应的 `resource` 文件打入 `classpath` 里，先定出 `profiles`

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113937.png)

此定义即指，当 `mvn` 命令执行时，我们需要通过 `-P dev` 或者 `-P test` 方式传入我们的意图：

`dev/test` 选择，会导致 `properties` 里的变量值含义不同，我们主要关注 `package.environment` 变量

### 10.3.资源插件的配置指定

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424113956.png)

配置 `maven-resources-plugin` 插件执行时，要复制的目录资源

### 10.4.mvn约定的资源中需要过滤掉环境目录

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114018.png)

需要将 `mvn` 约定的资源目录里，过滤掉环境目录

### 10.5.小属性更轻便的用法

对于简单的属性，我们可以选择更轻便的用法

**10.5.1.直接在环境中定义属性值**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114054.png)

**10.5.2.项目属性文件配置**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114104.png)

**10.5.3.约定的资源启用替换过滤**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114116.png)

最后有对 `pom.xml` 里面各标签有疑惑的小伙伴，下面附上 `pom.xml` 文件标签的详细注释解释，可以花时间好好去看一下对应的标签的作用是什么。

```
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd ">
 
    <!-- 父项目的坐标。如果项目中没有规定某个元素的值，那么父项目中的对应值即为项目的默认值。
         坐标包括group ID，artifact ID和 version。 -->
    <parent>
        <!-- 被继承的父项目的构件标识符 -->
        <artifactId>xxx</artifactId>
 
        <!-- 被继承的父项目的全球唯一标识符 -->
        <groupId>xxx</groupId>
 
        <!-- 被继承的父项目的版本 -->
        <version>xxx</version>
 
        <!-- 父项目的pom.xml文件的相对路径。相对路径允许你选择一个不同的路径。默认值是../pom.xml。
             Maven首先在构建当前项目的地方寻找父项目的pom，其次在文件系统的这个位置（relativePath位置），
             然后在本地仓库，最后在远程仓库寻找父项目的pom。 -->
        <relativePath>xxx</relativePath>
    </parent>
 
    <!-- 声明项目描述符遵循哪一个POM模型版本。模型本身的版本很少改变，虽然如此，但它仍然是必不可少的，
         这是为了当Maven引入了新的特性或者其他模型变更的时候，确保稳定性。 -->
    <modelVersion> 4.0.0 </modelVersion>
 
    <!-- 项目的全球唯一标识符，通常使用全限定的包名区分该项目和其他项目。并且构建时生成的路径也是由此生成，
         如com.mycompany.app生成的相对路径为：/com/mycompany/app -->
    <groupId>xxx</groupId>
 
    <!-- 构件的标识符，它和group ID一起唯一标识一个构件。换句话说，你不能有两个不同的项目拥有同样的artifact ID
         和groupID；在某个特定的group ID下，artifact ID也必须是唯一的。构件是项目产生的或使用的一个东西，Maven
         为项目产生的构件包括：JARs，源码，二进制发布和WARs等。 -->
    <artifactId>xxx</artifactId>
 
    <!-- 项目产生的构件类型，例如jar、war、ear、pom。插件可以创建他们自己的构件类型，所以前面列的不是全部构件类型 -->
    <packaging> jar </packaging>
 
    <!-- 项目当前版本，格式为:主版本.次版本.增量版本-限定版本号 -->
    <version> 1.0-SNAPSHOT </version>
 
    <!-- 项目的名称, Maven产生的文档用 -->
    <name> xxx-maven </name>
 
    <!-- 项目主页的URL, Maven产生的文档用 -->
    <url> http://maven.apache.org </url>
 
    <!-- 项目的详细描述, Maven 产生的文档用。 当这个元素能够用HTML格式描述时（例如，CDATA中的文本会被解析器忽略，
         就可以包含HTML标签）， 不鼓励使用纯文本描述。如果你需要修改产生的web站点的索引页面，你应该修改你自己的
         索引页文件，而不是调整这里的文档。 -->
    <description> A maven project to study maven. </description>
 
    <!-- 描述了这个项目构建环境中的前提条件。 -->
    <prerequisites>
        <!-- 构建该项目或使用该插件所需要的Maven的最低版本 -->
        <maven></maven>
    </prerequisites>
 
    <!-- 项目的问题管理系统(Bugzilla, Jira, Scarab,或任何你喜欢的问题管理系统)的名称和URL，本例为 jira -->
    <issueManagement>
        <!-- 问题管理系统（例如jira）的名字， -->
        <system> jira </system>
 
        <!-- 该项目使用的问题管理系统的URL -->
        <url> http://jira.baidu.com/banseon </url>
    </issueManagement>
 
    <!-- 项目持续集成信息 -->
    <ciManagement>
        <!-- 持续集成系统的名字，例如continuum -->
        <system></system>
 
        <!-- 该项目使用的持续集成系统的URL（如果持续集成系统有web接口的话）。 -->
        <url></url>
 
        <!-- 构建完成时，需要通知的开发者/用户的配置项。包括被通知者信息和通知条件（错误，失败，成功，警告） -->
        <notifiers>
            <!-- 配置一种方式，当构建中断时，以该方式通知用户/开发者 -->
            <notifier>
                <!-- 传送通知的途径 -->
                <type></type>
 
                <!-- 发生错误时是否通知 -->
                <sendOnError></sendOnError>
 
                <!-- 构建失败时是否通知 -->
                <sendOnFailure></sendOnFailure>
 
                <!-- 构建成功时是否通知 -->
                <sendOnSuccess></sendOnSuccess>
 
                <!-- 发生警告时是否通知 -->
                <sendOnWarning></sendOnWarning>
 
                <!-- 不赞成使用。通知发送到哪里 -->
                <address></address>
 
                <!-- 扩展配置项 -->
                <configuration></configuration>
            </notifier>
        </notifiers>
    </ciManagement>
 
    <!-- 项目创建年份，4位数字。当产生版权信息时需要使用这个值。 -->
    <inceptionYear />
 
    <!-- 项目相关邮件列表信息 -->
    <mailingLists>
        <!-- 该元素描述了项目相关的所有邮件列表。自动产生的网站引用这些信息。 -->
        <mailingList>
            <!-- 邮件的名称 -->
            <name> Demo </name>
 
            <!-- 发送邮件的地址或链接，如果是邮件地址，创建文档时，mailto: 链接会被自动创建 -->
            <post> banseon@126.com </post>
 
            <!-- 订阅邮件的地址或链接，如果是邮件地址，创建文档时，mailto: 链接会被自动创建 -->
            <subscribe> banseon@126.com </subscribe>
 
            <!-- 取消订阅邮件的地址或链接，如果是邮件地址，创建文档时，mailto: 链接会被自动创建 -->
            <unsubscribe> banseon@126.com </unsubscribe>
 
            <!-- 你可以浏览邮件信息的URL -->
            <archive> http:/hi.baidu.com/banseon/demo/dev/ </archive>
        </mailingList>
    </mailingLists>
 
    <!-- 项目开发者列表 -->
    <developers>
        <!-- 某个项目开发者的信息 -->
        <developer>
            <!-- SCM里项目开发者的唯一标识符 -->
            <id> HELLO WORLD </id>
 
            <!-- 项目开发者的全名 -->
            <name> banseon </name>
 
            <!-- 项目开发者的email -->
            <email> banseon@126.com </email>
 
            <!-- 项目开发者的主页的URL -->
            <url></url>
 
            <!-- 项目开发者在项目中扮演的角色，角色元素描述了各种角色 -->
            <roles>
                <role> Project Manager </role>
                <role> Architect </role>
            </roles>
 
            <!-- 项目开发者所属组织 -->
            <organization> demo </organization>
 
            <!-- 项目开发者所属组织的URL -->
            <organizationUrl> http://hi.baidu.com/xxx </organizationUrl>
 
            <!-- 项目开发者属性，如即时消息如何处理等 -->
            <properties>
                <dept> No </dept>
            </properties>
 
            <!-- 项目开发者所在时区， -11到12范围内的整数。 -->
            <timezone> -5 </timezone>
        </developer>
    </developers>
 
    <!-- 项目的其他贡献者列表 -->
    <contributors>
        <!-- 项目的其他贡献者。参见developers/developer元素 -->
        <contributor>
            <!-- 项目贡献者的全名 -->
            <name></name>
 
            <!-- 项目贡献者的email -->
            <email></email>
 
            <!-- 项目贡献者的主页的URL -->
            <url></url>
 
            <!-- 项目贡献者所属组织 -->
            <organization></organization>
 
            <!-- 项目贡献者所属组织的URL -->
            <organizationUrl></organizationUrl>
 
            <!-- 项目贡献者在项目中扮演的角色，角色元素描述了各种角色 -->
            <roles>
                <role> Project Manager </role>
                <role> Architect </role>
            </roles>
 
            <!-- 项目贡献者所在时区， -11到12范围内的整数。 -->
            <timezone></timezone>
 
            <!-- 项目贡献者属性，如即时消息如何处理等 -->
            <properties>
                <dept> No </dept>
            </properties>
        </contributor>
    </contributors>
 
    <!-- 该元素描述了项目所有License列表。 应该只列出该项目的license列表，不要列出依赖项目的 license列表。
         如果列出多个license，用户可以选择它们中的一个而不是接受所有license。 -->
    <licenses>
        <!-- 描述了项目的license，用于生成项目的web站点的license页面，其他一些报表和validation也会用到该元素。 -->
        <license>
            <!-- license用于法律上的名称 -->
            <name> Apache 2 </name>
 
            <!-- 官方的license正文页面的URL -->
            <url> http://www.baidu.com/banseon/LICENSE-2.0.txt </url>
 
            <!-- 项目分发的主要方式：
                    repo，可以从Maven库下载
                    manual， 用户必须手动下载和安装依赖 -->
            <distribution> repo </distribution>
 
            <!-- 关于license的补充信息 -->
            <comments> A business-friendly OSS license </comments>
        </license>
    </licenses>
 
    <!-- SCM(Source Control Management)标签允许你配置你的代码库，供Maven web站点和其它插件使用。 -->
    <scm>
        <!-- SCM的URL,该URL描述了版本库和如何连接到版本库。欲知详情，请看SCMs提供的URL格式和列表。该连接只读。 -->
        <connection>
            scm:svn:http://svn.baidu.com/banseon/maven/banseon/banseon-maven2-trunk(dao-trunk)
        </connection>
 
        <!-- 给开发者使用的，类似connection元素。即该连接不仅仅只读 -->
        <developerConnection>
            scm:svn:http://svn.baidu.com/banseon/maven/banseon/dao-trunk
        </developerConnection>
 
        <!-- 当前代码的标签，在开发阶段默认为HEAD -->
        <tag></tag>
 
        <!-- 指向项目的可浏览SCM库（例如ViewVC或者Fisheye）的URL。 -->
        <url> http://svn.baidu.com/banseon </url>
    </scm>
 
    <!-- 描述项目所属组织的各种属性。Maven产生的文档用 -->
    <organization>
        <!-- 组织的全名 -->
        <name> demo </name>
 
        <!-- 组织主页的URL -->
        <url> http://www.baidu.com/banseon </url>
    </organization>
 
    <!-- 构建项目需要的信息 -->
    <build>
        <!-- 该元素设置了项目源码目录，当构建项目的时候，构建系统会编译目录里的源码。该路径是相对
             于pom.xml的相对路径。 -->
        <sourceDirectory></sourceDirectory>
 
        <!-- 该元素设置了项目脚本源码目录，该目录和源码目录不同：绝大多数情况下，该目录下的内容会
             被拷贝到输出目录(因为脚本是被解释的，而不是被编译的)。 -->
        <scriptSourceDirectory></scriptSourceDirectory>
 
        <!-- 该元素设置了项目单元测试使用的源码目录，当测试项目的时候，构建系统会编译目录里的源码。
             该路径是相对于pom.xml的相对路径。 -->
        <testSourceDirectory></testSourceDirectory>
 
        <!-- 被编译过的应用程序class文件存放的目录。 -->
        <outputDirectory></outputDirectory>
 
        <!-- 被编译过的测试class文件存放的目录。 -->
        <testOutputDirectory></testOutputDirectory>
 
        <!-- 使用来自该项目的一系列构建扩展 -->
        <extensions>
            <!-- 描述使用到的构建扩展。 -->
            <extension>
                <!-- 构建扩展的groupId -->
                <groupId></groupId>
 
                <!-- 构建扩展的artifactId -->
                <artifactId></artifactId>
 
                <!-- 构建扩展的版本 -->
                <version></version>
            </extension>
        </extensions>
 
        <!-- 当项目没有规定目标（Maven2 叫做阶段）时的默认值 -->
        <defaultGoal></defaultGoal>
 
        <!-- 这个元素描述了项目相关的所有资源路径列表，例如和项目相关的属性文件，这些资源被包含在
             最终的打包文件里。 -->
        <resources>
            <!-- 这个元素描述了项目相关或测试相关的所有资源路径 -->
            <resource>
                <!-- 描述了资源的目标路径。该路径相对target/classes目录（例如${project.build.outputDirectory}）。
                     举个例子，如果你想资源在特定的包里(org.apache.maven.messages)，你就必须该元素设置为
                    org/apache/maven/messages。然而，如果你只是想把资源放到源码目录结构里，就不需要该配置。 -->
                <targetPath></targetPath>
 
                <!-- 是否使用参数值代替参数名。参数值取自properties元素或者文件里配置的属性，文件在filters元素
                     里列出。 -->
                <filtering></filtering>
 
                <!-- 描述存放资源的目录，该路径相对POM路径 -->
                <directory></directory>
 
                <!-- 包含的模式列表，例如**/*.xml. -->
                <includes>
                    <include></include>
                </includes>
 
                <!-- 排除的模式列表，例如**/*.xml -->
                <excludes>
                    <exclude></exclude>
                </excludes>
            </resource>
        </resources>
 
        <!-- 这个元素描述了单元测试相关的所有资源路径，例如和单元测试相关的属性文件。 -->
        <testResources>
            <!-- 这个元素描述了测试相关的所有资源路径，参见build/resources/resource元素的说明 -->
            <testResource>
                <!-- 描述了测试相关的资源的目标路径。该路径相对target/classes目录（例如${project.build.outputDirectory}）。
                     举个例子，如果你想资源在特定的包里(org.apache.maven.messages)，你就必须该元素设置为
                    org/apache/maven/messages。然而，如果你只是想把资源放到源码目录结构里，就不需要该配置。 -->
                <targetPath></targetPath>
 
                <!-- 是否使用参数值代替参数名。参数值取自properties元素或者文件里配置的属性，文件在filters元素
                     里列出。 -->
                <filtering></filtering>
 
                <!-- 描述存放测试相关的资源的目录，该路径相对POM路径 -->
                <directory></directory>
 
                <!-- 包含的模式列表，例如**/*.xml. -->
                <includes>
                    <include></include>
                </includes>
 
                <!-- 排除的模式列表，例如**/*.xml -->
                <excludes>
                    <exclude></exclude>
                </excludes>
            </testResource>
        </testResources>
 
        <!-- 构建产生的所有文件存放的目录 -->
        <directory></directory>
 
        <!-- 产生的构件的文件名，默认值是${artifactId}-${version}。 -->
        <finalName></finalName>
 
        <!-- 当filtering开关打开时，使用到的过滤器属性文件列表 -->
        <filters></filters>
 
        <!-- 子项目可以引用的默认插件信息。该插件配置项直到被引用时才会被解析或绑定到生命周期。给定插件的任何本
             地配置都会覆盖这里的配置 -->
        <pluginManagement>
            <!-- 使用的插件列表 。 -->
            <plugins>
                <!-- plugin元素包含描述插件所需要的信息。 -->
                <plugin>
                    <!-- 插件在仓库里的group ID -->
                    <groupId></groupId>
 
                    <!-- 插件在仓库里的artifact ID -->
                    <artifactId></artifactId>
 
                    <!-- 被使用的插件的版本（或版本范围） -->
                    <version></version>
 
                    <!-- 是否从该插件下载Maven扩展（例如打包和类型处理器），由于性能原因，只有在真需要下载时，该
                         元素才被设置成enabled。 -->
                    <extensions>true/false</extensions>
 
                    <!-- 在构建生命周期中执行一组目标的配置。每个目标可能有不同的配置。 -->
                    <executions>
                        <!-- execution元素包含了插件执行需要的信息 -->
                        <execution>
                            <!-- 执行目标的标识符，用于标识构建过程中的目标，或者匹配继承过程中需要合并的执行目标 -->
                            <id></id>
 
                            <!-- 绑定了目标的构建生命周期阶段，如果省略，目标会被绑定到源数据里配置的默认阶段 -->
                            <phase></phase>
 
                            <!-- 配置的执行目标 -->
                            <goals></goals>
 
                            <!-- 配置是否被传播到子POM -->
                            <inherited>true/false</inherited>
 
                            <!-- 作为DOM对象的配置 -->
                            <configuration></configuration>
                        </execution>
                    </executions>
 
                    <!-- 项目引入插件所需要的额外依赖 -->
                    <dependencies>
                        <!-- 参见dependencies/dependency元素 -->
                        <dependency>
                        </dependency>
                    </dependencies>
 
                    <!-- 任何配置是否被传播到子项目 -->
                    <inherited>true/false</inherited>
 
                    <!-- 作为DOM对象的配置 -->
                    <configuration></configuration>
                </plugin>
            </plugins>
        </pluginManagement>
 
        <!-- 该项目使用的插件列表 。 -->
        <plugins>
            <!-- plugin元素包含描述插件所需要的信息。 -->
            <plugin>
                <!-- 插件在仓库里的group ID -->
                <groupId></groupId>
 
                <!-- 插件在仓库里的artifact ID -->
                <artifactId></artifactId>
 
                <!-- 被使用的插件的版本（或版本范围） -->
                <version></version>
 
                <!-- 是否从该插件下载Maven扩展（例如打包和类型处理器），由于性能原因，只有在真需要下载时，该
                     元素才被设置成enabled。 -->
                <extensions>true/false</extensions>
 
                <!-- 在构建生命周期中执行一组目标的配置。每个目标可能有不同的配置。 -->
                <executions>
                    <!-- execution元素包含了插件执行需要的信息 -->
                    <execution>
                        <!-- 执行目标的标识符，用于标识构建过程中的目标，或者匹配继承过程中需要合并的执行目标 -->
                        <id></id>
 
                        <!-- 绑定了目标的构建生命周期阶段，如果省略，目标会被绑定到源数据里配置的默认阶段 -->
                        <phase></phase>
 
                        <!-- 配置的执行目标 -->
                        <goals></goals>
 
                        <!-- 配置是否被传播到子POM -->
                        <inherited>true/false</inherited>
 
                        <!-- 作为DOM对象的配置 -->
                        <configuration></configuration>
                    </execution>
                </executions>
 
                <!-- 项目引入插件所需要的额外依赖 -->
                <dependencies>
                    <!-- 参见dependencies/dependency元素 -->
                    <dependency>
                    </dependency>
                </dependencies>
 
                <!-- 任何配置是否被传播到子项目 -->
                <inherited>true/false</inherited>
 
                <!-- 作为DOM对象的配置 -->
                <configuration></configuration>
            </plugin>
        </plugins>
    </build>
 
    <!-- 在列的项目构建profile，如果被激活，会修改构建处理 -->
    <profiles>
        <!-- 根据环境参数或命令行参数激活某个构建处理 -->
        <profile>
            <!-- 构建配置的唯一标识符。即用于命令行激活，也用于在继承时合并具有相同标识符的profile。 -->
            <id></id>
 
            <!-- 自动触发profile的条件逻辑。Activation是profile的开启钥匙。profile的力量来自于它能够
                 在某些特定的环境中自动使用某些特定的值；这些环境通过activation元素指定。activation元
                 素并不是激活profile的唯一方式。 -->
            <activation>
                <!-- profile默认是否激活的标志 -->
                <activeByDefault>true/false</activeByDefault>
 
                <!-- 当匹配的jdk被检测到，profile被激活。例如，1.4激活JDK1.4，1.4.0_2，而!1.4激活所有版本
                     不是以1.4开头的JDK。 -->
                <jdk>jdk版本，如:1.7</jdk>
 
                <!-- 当匹配的操作系统属性被检测到，profile被激活。os元素可以定义一些操作系统相关的属性。 -->
                <os>
                    <!-- 激活profile的操作系统的名字 -->
                    <name> Windows XP </name>
 
                    <!-- 激活profile的操作系统所属家族(如 'windows') -->
                    <family> Windows </family>
 
                    <!-- 激活profile的操作系统体系结构 -->
                    <arch> x86 </arch>
 
                    <!-- 激活profile的操作系统版本 -->
                    <version> 5.1.2600 </version>
                </os>
 
                <!-- 如果Maven检测到某一个属性（其值可以在POM中通过${名称}引用），其拥有对应的名称和值，Profile
                     就会被激活。如果值字段是空的，那么存在属性名称字段就会激活profile，否则按区分大小写方式匹
                     配属性值字段 -->
                <property>
                    <!-- 激活profile的属性的名称 -->
                    <name> mavenVersion </name>
 
                    <!-- 激活profile的属性的值 -->
                    <value> 2.0.3 </value>
                </property>
 
                <!-- 提供一个文件名，通过检测该文件的存在或不存在来激活profile。missing检查文件是否存在，如果不存在则激活
                     profile。另一方面，exists则会检查文件是否存在，如果存在则激活profile。 -->
                <file>
                    <!-- 如果指定的文件存在，则激活profile。 -->
                    <exists> /usr/local/hudson/hudson-home/jobs/maven-guide-zh-to-production/workspace/ </exists>
 
                    <!-- 如果指定的文件不存在，则激活profile。 -->
                    <missing> /usr/local/hudson/hudson-home/jobs/maven-guide-zh-to-production/workspace/ </missing>
                </file>
            </activation>
 
            <!-- 构建项目所需要的信息。参见build元素 -->
            <build>
                <defaultGoal />
                <resources>
                    <resource>
                        <targetPath></targetPath>
                        <filtering></filtering>
                        <directory></directory>
                        <includes>
                            <include></include>
                        </includes>
                        <excludes>
                            <exclude></exclude>
                        </excludes>
                    </resource>
                </resources>
                <testResources>
                    <testResource>
                        <targetPath></targetPath>
                        <filtering></filtering>
                        <directory></directory>
                        <includes>
                            <include></include>
                        </includes>
                        <excludes>
                            <exclude></exclude>
                        </excludes>
                    </testResource>
                </testResources>
                <directory></directory>
                <finalName></finalName>
                <filters></filters>
                <pluginManagement>
                    <plugins>
                        <!-- 参见build/pluginManagement/plugins/plugin元素 -->
                        <plugin>
                            <groupId></groupId>
                            <artifactId></artifactId>
                            <version></version>
                            <extensions>true/false</extensions>
                            <executions>
                                <execution>
                                    <id></id>
                                    <phase></phase>
                                    <goals></goals>
                                    <inherited>true/false</inherited>
                                    <configuration></configuration>
                                </execution>
                            </executions>
                            <dependencies>
                                <!-- 参见dependencies/dependency元素 -->
                                <dependency>
                                </dependency>
                            </dependencies>
                            <goals></goals>
                            <inherited>true/false</inherited>
                            <configuration></configuration>
                        </plugin>
                    </plugins>
                </pluginManagement>
                <plugins>
                    <!-- 参见build/pluginManagement/plugins/plugin元素 -->
                    <plugin>
                        <groupId></groupId>
                        <artifactId></artifactId>
                        <version></version>
                        <extensions>true/false</extensions>
                        <executions>
                            <execution>
                                <id></id>
                                <phase></phase>
                                <goals></goals>
                                <inherited>true/false</inherited>
                                <configuration></configuration>
                            </execution>
                        </executions>
                        <dependencies>
                            <!-- 参见dependencies/dependency元素 -->
                            <dependency>
                            </dependency>
                        </dependencies>
                        <goals></goals>
                        <inherited>true/false</inherited>
                        <configuration></configuration>
                    </plugin>
                </plugins>
            </build>
 
            <!-- 模块（有时称作子项目） 被构建成项目的一部分。列出的每个模块元素是指向该模块的目录的
                 相对路径 -->
            <modules>
                <!--子项目相对路径-->
                <module></module>
            </modules>
 
            <!-- 发现依赖和扩展的远程仓库列表。 -->
            <repositories>
                <!-- 参见repositories/repository元素 -->
                <repository>
                    <releases>
                        <enabled><enabled>
                        <updatePolicy></updatePolicy>
                        <checksumPolicy></checksumPolicy>
                    </releases>
                    <snapshots>
                        <enabled><enabled>
                        <updatePolicy></updatePolicy>
                        <checksumPolicy></checksumPolicy>
                    </snapshots>
                    <id></id>
                    <name></name>
                    <url></url>
                    <layout></layout>
                </repository>
            </repositories>
 
            <!-- 发现插件的远程仓库列表，这些插件用于构建和报表 -->
            <pluginRepositories>
                <!-- 包含需要连接到远程插件仓库的信息.参见repositories/repository元素 -->
                <pluginRepository>
                    <releases>
                        <enabled><enabled>
                        <updatePolicy></updatePolicy>
                        <checksumPolicy></checksumPolicy>
                    </releases>
                    <snapshots>
                        <enabled><enabled>
                        <updatePolicy></updatePolicy>
                        <checksumPolicy></checksumPolicy>
                    </snapshots>
                    <id></id>
                    <name></name>
                    <url></url>
                    <layout></layout>
                </pluginRepository>
            </pluginRepositories>
 
            <!-- 该元素描述了项目相关的所有依赖。 这些依赖组成了项目构建过程中的一个个环节。它们自动从项目定义的
                 仓库中下载。要获取更多信息，请看项目依赖机制。 -->
            <dependencies>
                <!-- 参见dependencies/dependency元素 -->
                <dependency>
                </dependency>
            </dependencies>
 
            <!-- 不赞成使用. 现在Maven忽略该元素. -->
            <reports></reports>
 
            <!-- 该元素包括使用报表插件产生报表的规范。当用户执行“mvn site”，这些报表就会运行。 在页面导航栏能看
                 到所有报表的链接。参见reporting元素 -->
            <reporting></reporting>
 
            <!-- 参见dependencyManagement元素 -->
            <dependencyManagement>
                <dependencies>
                    <!-- 参见dependencies/dependency元素 -->
                    <dependency>
                    </dependency>
                </dependencies>
            </dependencyManagement>
 
            <!-- 参见distributionManagement元素 -->
            <distributionManagement>
            </distributionManagement>
 
            <!-- 参见properties元素 -->
            <properties />
        </profile>
    </profiles>
 
    <!-- 模块（有时称作子项目） 被构建成项目的一部分。列出的每个模块元素是指向该模块的目录的相对路径 -->
    <modules>
        <!--子项目相对路径-->
        <module></module>
    </modules>
 
    <!-- 发现依赖和扩展的远程仓库列表。 -->
    <repositories>
        <!-- 包含需要连接到远程仓库的信息 -->
        <repository>
            <!-- 如何处理远程仓库里发布版本的下载 -->
            <releases>
                <!-- true或者false表示该仓库是否为下载某种类型构件（发布版，快照版）开启。 -->
                <enabled><enabled>
 
                <!-- 该元素指定更新发生的频率。Maven会比较本地POM和远程POM的时间戳。这里的选项是：always（一直），
                     daily（默认，每日），interval：X（这里X是以分钟为单位的时间间隔），或者never（从不）。 -->
                <updatePolicy></updatePolicy>
 
                <!-- 当Maven验证构件校验文件失败时该怎么做：ignore（忽略），fail（失败），或者warn（警告）。 -->
                <checksumPolicy></checksumPolicy>
            </releases>
 
            <!-- 如何处理远程仓库里快照版本的下载。有了releases和snapshots这两组配置，POM就可以在每个单独的仓库中，
                 为每种类型的构件采取不同的策略。例如，可能有人会决定只为开发目的开启对快照版本下载的支持。参见repositories/repository/releases元素 -->
            <snapshots>
                <enabled><enabled>
                <updatePolicy></updatePolicy>
                <checksumPolicy></checksumPolicy>
            </snapshots>
 
            <!-- 远程仓库唯一标识符。可以用来匹配在settings.xml文件里配置的远程仓库 -->
            <id> banseon-repository-proxy </id>
 
            <!-- 远程仓库名称 -->
            <name> banseon-repository-proxy </name>
 
            <!-- 远程仓库URL，按protocol://hostname/path形式 -->
            <url> http://192.168.1.169:9999/repository/ </url>
 
            <!-- 用于定位和排序构件的仓库布局类型-可以是default（默认）或者legacy（遗留）。Maven 2为其仓库提供了一个默认
                 的布局；然而，Maven 1.x有一种不同的布局。我们可以使用该元素指定布局是default（默认）还是legacy（遗留）。 -->
            <layout> default </layout>
        </repository>
    </repositories>
 
    <!-- 发现插件的远程仓库列表，这些插件用于构建和报表 -->
    <pluginRepositories>
        <!-- 包含需要连接到远程插件仓库的信息.参见repositories/repository元素 -->
        <pluginRepository>
        </pluginRepository>
    </pluginRepositories>
 
    <!-- 该元素描述了项目相关的所有依赖。 这些依赖组成了项目构建过程中的一个个环节。它们自动从项目定义的仓库中下载。
         要获取更多信息，请看项目依赖机制。 -->
    <dependencies>
        <dependency>
            <!-- 依赖的group ID -->
            <groupId> org.apache.maven </groupId>
 
            <!-- 依赖的artifact ID -->
            <artifactId> maven-artifact </artifactId>
 
            <!-- 依赖的版本号。 在Maven 2里, 也可以配置成版本号的范围。 -->
            <version> 3.8.1 </version>
 
            <!-- 依赖类型，默认类型是jar。它通常表示依赖的文件的扩展名，但也有例外。一个类型可以被映射成另外一个扩展
                 名或分类器。类型经常和使用的打包方式对应，尽管这也有例外。一些类型的例子：jar，war，ejb-client和test-jar。
                 如果设置extensions为 true，就可以在plugin里定义新的类型。所以前面的类型的例子不完整。 -->
            <type> jar </type>
 
            <!-- 依赖的分类器。分类器可以区分属于同一个POM，但不同构建方式的构件。分类器名被附加到文件名的版本号后面。例如，
                 如果你想要构建两个单独的构件成JAR，一个使用Java 1.4编译器，另一个使用Java 6编译器，你就可以使用分类器来生
                 成两个单独的JAR构件。 -->
            <classifier></classifier>
 
            <!-- 依赖范围。在项目发布过程中，帮助决定哪些构件被包括进来。欲知详情请参考依赖机制。
                - compile ：默认范围，用于编译
                - provided：类似于编译，但支持你期待jdk或者容器提供，类似于classpath
                - runtime: 在执行时需要使用
                - test: 用于test任务时使用
                - system: 需要外在提供相应的元素。通过systemPath来取得
                - systemPath: 仅用于范围为system。提供相应的路径
                - optional: 当项目自身被依赖时，标注依赖是否传递。用于连续依赖时使用 -->
            <scope> test </scope>
 
            <!-- 仅供system范围使用。注意，不鼓励使用这个元素，并且在新的版本中该元素可能被覆盖掉。该元素为依赖规定了文件
                 系统上的路径。需要绝对路径而不是相对路径。推荐使用属性匹配绝对路径，例如${java.home}。 -->
            <systemPath></systemPath>
 
            <!-- 当计算传递依赖时， 从依赖构件列表里，列出被排除的依赖构件集。即告诉maven你只依赖指定的项目，不依赖项目的
                 依赖。此元素主要用于解决版本冲突问题 -->
            <exclusions>
                <exclusion>
                    <artifactId> spring-core </artifactId>
                    <groupId> org.springframework </groupId>
                </exclusion>
            </exclusions>
 
            <!-- 可选依赖，如果你在项目B中把C依赖声明为可选，你就需要在依赖于B的项目（例如项目A）中显式的引用对C的依赖。
                 可选依赖阻断依赖的传递性。 -->
            <optional> true </optional>
        </dependency>
    </dependencies>
 
    <!-- 不赞成使用. 现在Maven忽略该元素. -->
    <reports></reports>
 
    <!-- 该元素描述使用报表插件产生报表的规范。当用户执行“mvn site”，这些报表就会运行。 在页面导航栏能看到所有报表的链接。 -->
    <reporting>
        <!-- true，则，网站不包括默认的报表。这包括“项目信息”菜单中的报表。 -->
        <excludeDefaults />
 
        <!-- 所有产生的报表存放到哪里。默认值是${project.build.directory}/site。 -->
        <outputDirectory />
 
        <!-- 使用的报表插件和他们的配置。 -->
        <plugins>
            <!-- plugin元素包含描述报表插件需要的信息 -->
            <plugin>
                <!-- 报表插件在仓库里的group ID -->
                <groupId></groupId>
                <!-- 报表插件在仓库里的artifact ID -->
                <artifactId></artifactId>
 
                <!-- 被使用的报表插件的版本（或版本范围） -->
                <version></version>
 
                <!-- 任何配置是否被传播到子项目 -->
                <inherited>true/false</inherited>
 
                <!-- 报表插件的配置 -->
                <configuration></configuration>
 
                <!-- 一组报表的多重规范，每个规范可能有不同的配置。一个规范（报表集）对应一个执行目标 。例如，
                     有1，2，3，4，5，6，7，8，9个报表。1，2，5构成A报表集，对应一个执行目标。2，5，8构成B报
                     表集，对应另一个执行目标 -->
                <reportSets>
                    <!-- 表示报表的一个集合，以及产生该集合的配置 -->
                    <reportSet>
                        <!-- 报表集合的唯一标识符，POM继承时用到 -->
                        <id></id>
 
                        <!-- 产生报表集合时，被使用的报表的配置 -->
                        <configuration></configuration>
 
                        <!-- 配置是否被继承到子POMs -->
                        <inherited>true/false</inherited>
 
                        <!-- 这个集合里使用到哪些报表 -->
                        <reports></reports>
                    </reportSet>
                </reportSets>
            </plugin>
        </plugins>
    </reporting>
 
    <!-- 继承自该项目的所有子项目的默认依赖信息。这部分的依赖信息不会被立即解析,而是当子项目声明一个依赖
        （必须描述group ID和artifact ID信息），如果group ID和artifact ID以外的一些信息没有描述，则通过
            group ID和artifact ID匹配到这里的依赖，并使用这里的依赖信息。 -->
    <dependencyManagement>
        <dependencies>
            <!-- 参见dependencies/dependency元素 -->
            <dependency>
            </dependency>
        </dependencies>
    </dependencyManagement>
 
    <!-- 项目分发信息，在执行mvn deploy后表示要发布的位置。有了这些信息就可以把网站部署到远程服务器或者
         把构件部署到远程仓库。 -->
    <distributionManagement>
        <!-- 部署项目产生的构件到远程仓库需要的信息 -->
        <repository>
            <!-- 是分配给快照一个唯一的版本号（由时间戳和构建流水号）？还是每次都使用相同的版本号？参见
                 repositories/repository元素 -->
            <uniqueVersion />
            <id> banseon-maven2 </id>
            <name> banseon maven2 </name>
            <url> file://${basedir}/target/deploy </url>
            <layout></layout>
        </repository>
 
        <!-- 构件的快照部署到哪里？如果没有配置该元素，默认部署到repository元素配置的仓库，参见
             distributionManagement/repository元素 -->
        <snapshotRepository>
            <uniqueVersion />
            <id> banseon-maven2 </id>
            <name> Banseon-maven2 Snapshot Repository </name>
            <url> scp://svn.baidu.com/banseon:/usr/local/maven-snapshot </url>
            <layout></layout>
        </snapshotRepository>
 
        <!-- 部署项目的网站需要的信息 -->
        <site>
            <!-- 部署位置的唯一标识符，用来匹配站点和settings.xml文件里的配置 -->
            <id> banseon-site </id>
 
            <!-- 部署位置的名称 -->
            <name> business api website </name>
 
            <!-- 部署位置的URL，按protocol://hostname/path形式 -->
            <url>
                scp://svn.baidu.com/banseon:/var/www/localhost/banseon-web
            </url>
        </site>
 
        <!-- 项目下载页面的URL。如果没有该元素，用户应该参考主页。使用该元素的原因是：帮助定位
             那些不在仓库里的构件（由于license限制）。 -->
        <downloadUrl />
 
        <!-- 如果构件有了新的group ID和artifact ID（构件移到了新的位置），这里列出构件的重定位信息。 -->
        <relocation>
            <!-- 构件新的group ID -->
            <groupId></groupId>
 
            <!-- 构件新的artifact ID -->
            <artifactId></artifactId>
 
            <!-- 构件新的版本号 -->
            <version></version>
 
            <!-- 显示给用户的，关于移动的额外信息，例如原因。 -->
            <message></message>
        </relocation>
 
        <!-- 给出该构件在远程仓库的状态。不得在本地项目中设置该元素，因为这是工具自动更新的。有效的值
             有：none（默认），converted（仓库管理员从Maven 1 POM转换过来），partner（直接从伙伴Maven
             2仓库同步过来），deployed（从Maven 2实例部署），verified（被核实时正确的和最终的）。 -->
        <status></status>
    </distributionManagement>
 
    <!-- 以值替代名称，Properties可以在整个POM中使用，也可以作为触发条件（见settings.xml配置文件里
         activation元素的说明）。格式是<name>value</name>。 -->
    <properties>
        <name>value</name>
    </properties>
</project>
```



## 11.搭建Maven私服

### 11.1.环境说明

环境：`CentOS 6.8`、 `JDK8`、 `Sonatype Nexus`、 `Maven`
IP：`192.168.50.131`
root 用户操作

### 11.2.安装Nexus

前提： 已安装 JDK8 并配置好了环境变量，小伙伴们自行搭建JDK8环境，这里我就不再赘述了。相信小伙伴们都能够正确搭建JDK8环境。

**11.2.1.下载Nexus**

下载Nexus（这里，我使用的是： `nexus-2.11.2-03-bundle.tar.gz`） ,下载地址：http://www.sonatype.org/nexus/go/ ，我们也可以在服务器的命令行输入如下命令下载 `nexus-2.11.2-03-bundle.tar.gz` 安装文件。

```bash
# wget https://sonatype-download.global.ssl.fastly.net/nexus/oss/nexus-2.11.2-03-bundle.tar.gz
```

也可以到链接：https://download.csdn.net/download/l1028386804/12523592 下载

**11.2.2.解压Nexus**

```bash
# mkdir nexus
# tar -zxvf nexus-2.11.2-03-bundle.tar.gz -C nexus
# cd nexus
# ls
nexus-2.11.2-03 sonatype-work
(一个 nexus 服务，一个私有库目录)
```

**11.2.3.编辑 Nexus**

编辑 Nexus 的 nexus.properties 文件，配置端口和 work 目录信息（保留默认）

```bash
# cd nexus-2.11.2-03
# ls
bin conf lib LICENSE.txt logs nexus NOTICE.txt tmp
```

查看目录结构， jetty 运行

```bash
# cd conf
# vi nexus.properties
# Jetty section
application-port=8081
application-host=0.0.0.0
nexus-webapp=${bundleBasedir}/nexus
nexus-webapp-context-path=/nexus
# Nexus section
nexus-work=${bundleBasedir}/../sonatype-work/nexus
runtime=${bundleBasedir}/nexus/WEB-INF
```

**11.2.4.编辑 nexus 脚本，配置 RUN_AS_USER 参数**

```bash
# vi /usr/local/nexus/nexus-2.11.2-03/bin/nexus
#RUN_AS_USER=
```

改为：

```bash
RUN_AS_USER=root
```

**11.2.5.防火墙中打开 8081 端口**

```bash
# vi /etc/sysconfig/iptables
```

添加：

```bash
-A INPUT -m state --state NEW -m tcp -p tcp --dport 8081 -j ACCEPT
```

保存后重启防火墙

```bash
# service iptables restart
```

**11.2.6.启动 nexus**

```bash
# /usr/local/nexus/nexus-2.11.2-03/bin/nexus start
****************************************
WARNING - NOT RECOMMENDED TO RUN AS usr/local
****************************************
Starting Nexus OSS...
Started Nexus OSS.
```

**11.2.7.访问nexus**

浏览器中打开： http://192.168.50.131:8081/nexus/

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114510.png)

**11.2.8.登录nexus**

默认用户名admin,默认密码admin123。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114604.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114614.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114627.png)

到此， Nexus 已安装完成， 接下来是 Nexus 的配置

### 11.3.Nexus 配置（登录后）

**设置管理员邮箱**

菜单 Administration/Server 配置邮箱服务地址(如果忘记密码，可以通过该邮箱找回密码)
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114659.png)

**11.3.1.设置用户邮箱**

给用户配置邮箱地址，方便忘记密码时找回：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114715.png)

**11.3.2.用户修改密码**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114732.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114736.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114740.png)

**11.3.3.仓库类型**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114757.png)

- group 仓库组： Nexus通过仓库组的概念统一管理多个仓库，这样我们在项目中直接请求仓库组即可请求到仓库组管理的多个仓库；
- hosted 宿主仓库： 主要用于发布内部项目构件或第三方的项目构件 （如购买商业的构件）以及无法从公共仓库获取的构件（如 oracle 的 JDBC 驱动）proxy 代理仓库： 代理公共的远程仓库；
- virtual 虚拟仓库： 用于适配 Maven 1；

一般用到的仓库种类是 hosted、 proxy。

Hosted 仓库常用类型说明：

- releases 内部的模块中 release 模块的发布仓库
- snapshots 发布内部的 SNAPSHOT 模块的仓库
- 3rd party 第三方依赖的仓库，这个数据通常是由内部人员自行下载之后发布上去

如果构建的 Maven 项目本地仓库没有对应的依赖包，那么就会去 Nexus 私服去下载，如果Nexus私服也没有此依赖包，就回去远程中央仓库下载依赖，这些中央仓库就是 proxy。Nexus 私服下载成功后再下载至本地 Maven 库供项目引用。

**11.3.4.设置 proxy 代理仓库**

设置 proxy 代理仓库(Apache Snapshots/Central/Codehaus Snapshots)准许远程下载，如下所示。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114817.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114829.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114836.png)

### 11.4.Maven 本地库的安装与配置

**11.4.1.下载Maven**

到链接http://maven.apache.org/download.cgi 下载Maven

**11.4.2.配置Maven环境变量**

```bash
vim /etc/profile

MAVEN_HOME=/usr/local/maven
JAVA_HOME=/usr/local/jdk
CLASS_PATH=$JAVA_HOME/lib
PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
export JAVA_HOME MAVEN_HOME CLASS_PATH PATH

source /etc/profile
```

**11.4.3.配置本地Maven**

拷贝Maven的conf目录下的配置文件settings.xml，重命名为settings-lyz.xml，修改配置文件后的内容如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
	<localRepository>D:/Maven_Repository/.m2/repository</localRepository>
	<interactiveMode>true</interactiveMode>
    <offline>false</offline>
    <pluginGroups>
        <pluginGroup>org.mortbay.jetty</pluginGroup>
        <pluginGroup>org.jenkins-ci.tools</pluginGroup>
    </pluginGroups>
	
	<!--配置权限,使用默认用户-->
	<servers>
		<server>
			<id>nexus-releases</id>
			<username>deployment</username>
			<password>deployment123</password>
		</server>
		<server> 
			<id>nexus-snapshots</id>
			<username>deployment</username>
			<password>deployment123</password>
		</server>
	</servers>
 
    <mirrors>
 
    </mirrors>
 
	<profiles>
		<profile>
		   <id>lyz</id>
			    <activation>
                    <activeByDefault>false</activeByDefault>
                    <jdk>1.8</jdk>
                </activation>
			    <repositories>
					<!-- 私有库地址-->
				    <repository>
						<id>nexus</id>
						<url>http://192.168.50.131:8081/nexus/content/groups/public/</url>
						<releases>
							<enabled>true</enabled>
						</releases>
						<snapshots>
							<enabled>true</enabled>
						</snapshots>
					</repository>
				</repositories>      
				<pluginRepositories>
					<!--插件库地址-->
					<pluginRepository>
						<id>nexus</id>
						<url>http://192.168.50.131:8081/nexus/content/groups/public/</url>
						<releases>
							<enabled>true</enabled>
						</releases>
						<snapshots>
							<enabled>true</enabled>
					   </snapshots>
					</pluginRepository>
				</pluginRepositories>
			</profile>
	</profiles>
	
	<!--激活profile-->
	<activeProfiles>
		<activeProfile>lyz</activeProfile>
	</activeProfiles>

</settings>
```

其中，配置文件中的

```xml
<localRepository>D:/Maven_Repository/.m2/repository</localRepository>
```

说明本地仓库位于D:/Maven_Repository/.m2/repository目录下。

配置文件中的如下配置项。

```xml
<url>http://192.168.50.131:8081/nexus/content/groups/public/</url>
```

与下图中的链接一致：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114906.png)

**11.4.4.配置Eclipse Maven**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114935.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424114942.png)

**11.4.5.项目的构建与发布**

首先我们要在项目的pom.xml文件中加入如下内容，将项目构建成的Jar发布到Maven私有仓库

```xml
<distributionManagement>
    <repository>
        <id>nexus-releases</id>
        <name>Nexus Release Repository</name>
        <url>http://192.168.50.131:8081/nexus/content/repositories/releases/</url>
    </repository>
    <snapshotRepository>
        <id>nexus-snapshots</id>
        <name>Nexus Snapshot Repository</name>
        <url>http://192.168.50.131:8081/nexus/content/repositories/snapshots/</url>
    </snapshotRepository>
</distributionManagement>
```

配置说明

项目中的 `pom.xml` 文件中，如果版本配置如下：

```xml
<version>0.0.1-SNAPSHOT</version>
```

则发布到Maven私有仓库后对应的目录如下：
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115003.png)

如果版本配置如下：

```xml
<version>0.0.1-RELEASE</version>
```

则发布到Maven私有仓库后对应的目录如下。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115016.png)

完整pom.xml文件的配置如下所示。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>common-utils-maven</groupId>
    <artifactId>com.chwl.common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <distributionManagement>
        <repository>
            <id>nexus-releases</id>
            <name>Nexus Release Repository</name>
            <url>http://192.168.50.131:8081/nexus/content/repositories/releases/</url>
        </repository>
        <snapshotRepository>
            <id>nexus-snapshots</id>
            <name>Nexus Snapshot Repository</name>
            <url>http://192.168.50.131:8081/nexus/content/repositories/snapshots/</url>
        </snapshotRepository>
    </distributionManagement>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jdk.version>1.8</jdk.version>
    </properties>
    <dependencies>
       此处省略....
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${jdk.version}</source>
                    <target>${jdk.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>2.1.2</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

具体发布步骤如下：

右键 `pom.xml->Run as->Maven build->`
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115034.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115044.png)

上图中的私有库为空，我们右键 `pom.xml->Run as->Maven build`(此时 `pom.xml` 文件的 `version` 为 `0.0.1-SNAPSHOT`)。

构建完毕后

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115054.png)

说明已经将项目构建并发布到了我们的Maven私有仓库。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115103.png)

此时，上图中的 `Release` 目录为空，此时，我们修改 `pom.xml` 的 `version` 为 `0.0.1-RELEASE`，再次右键 `pom.xml->Run as->Maven build`，构建项目，此时发布的目录如下图：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115112.png)

说明已经将项目构建并发布到了我们的 `Maven` 私有仓库。

最后，我们添加第三方的Jar依赖到我们的 `Maven` 私有仓库，具体操作如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115126.png)

如上图，第三方依赖私有仓库为空,我们按照以下步骤上传第三方依赖到我们的Maven私有仓库。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115134.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115142.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115151.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424115156.png)

如上图，第三方依赖已经上传到我们的Maven私有仓库。

## X.常见问题

### X.1.解决Maven Jar包冲突
#### X.1.1.定位冲突
IDEA提供了一个maven依赖分析神器：Maven Helper

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424121820.png)

用这个插件能很好的显示出项目中所有的依赖树和冲突

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424121854.png)

这里面红色高亮的部分，就表明这个Jar包有了冲突。选中这个 `jar` 包，可以看到这2个版本的冲突的来源。

上图的例子，表明 `cruator-client`这个 `Jar` 包，有2个传递依赖，分别为 `2.5.0` 版本和 `4.0.1` 版本。冲突的描述为：
```
omitted for conflict with 2.5.0. 由于与2.5.0版本冲突而被省略
```
具体的层级在右边也一目了然了，所以 `maven` 最终根据最短路径优先原则选择了 `2.5.0` 版本，`4.0.1` 版本被忽略。

这时候有同学会问：本地环境我可以利用 `Maven Helper` 来定位，那么预生产或者生产环境呢。又没有IDEA，如何定位冲突的细节？

可以利用 `mvn` 命令来解决：
```
mvn dependency:tree -Dverbose

此处一定不要省略-Dverbose参数，要不然是不会显示被忽略的包的
```
![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424121925.png)
其实mvn命令行一样好用。非常清晰明确。

#### X.1.2.解决Jar包冲突的几个实用技巧
**X.1.2.1.排除法**

还是上面的那个例子，现在生效的是2.5.0，如果想生效4.0.1。只需要在2.5.0上面点exclude就行了。

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424121650.png)

**X.1.2.2.版本锁定法**

如果很多个依赖都传递了Jar包A，涉及了很多个版本，但是你只想指定一个版本。用排除法一个个去exclude太麻烦，而且exclude在pom文件中也会体现，太多的话，也影响代码整洁和阅读感受。

这时候需要用到版本锁定法

何谓版本锁定法？公司的项目一般都会有父级pom，你想指定哪个版本只需要在你项目的父POM中(当然在本工程内也可以)定义如下：（还是举上个例子，指定4.0.1版本）
```
<dependencyManagement>
    <dependency>
        <groupId>org.apache.curator</groupId>
        <artifactId>curator-client</artifactId>
        <version>4.0.1</version>
    </dependency>
</dependencyManagement>
```
锁定版本法可以打破2个依赖传递的原则，优先级为最高

锁定版本后，依赖树为：

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424121615.png)

都统一变成4.0.1，锁定版本有一个好处：版本锁定并不排除Jar包，而且显示的把所有版本不一致的Jar包变成统一一个版本，这样在阅读代码时比较友好。也不用忍受一大堆的exclude标签。

**X.1.2.3.如何写一个干净依赖关系的POM文件**

我本人是有些轻度代码洁癖的人，所以即便是pom文件的依赖关系也想干净而整洁。如何写好干净的POM呢，作者认为有几点技巧要注意：
- 尽量在父POM中定义 `<dependencyManagement>`，来进行本项目一些依赖版本的管理，这样可以从很大程度上解决一定的冲突
- 如果是提供给别人依赖的Jar包，尽可能不要传递依赖不必要的Jar包
- 使用 `mvn dependency:analyze-only` 命令用于检测那些声明了但是没被使用的依赖，如有有一些是你自己声明的，那尽量去掉
- 使用 `mvn dependency:analyze-duplicate` 命令用来分析重复定义的依赖，清理那些重复定义的依赖

**X.1.2.4.最后**

其实庞大的项目依赖传递也一定多。但是不管多复杂的依赖关系，看到不要害怕。就这么几条原则，细心的去分析，所有的依赖都有迹可循。

这些传递依赖如果管理的好，能让你的维护成本大大降低。如果管不好，这群野孩子每一个都可能是引发下一个NoSuchMethodError的导火索。


### X.2.项目版本号升级快速修改版本号
如图所示分布式项目修改版本号40多处需要修改，手动改太累了

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424121523.png)

解决方法：在父项目上两条命令即可

1. `versions:set -DnewVersion=1.3.2-SNAPSHOT`

2. `versions:commit`，这样版本号就全部自动变为1.3.2了













