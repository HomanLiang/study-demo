[toc]



# Gradle

## 1.Gradle 简介

### 1.1.简介

`gradle` 的最新版本是 `6.7`，从2009年的第一个版本，到2020年的 `6.7`，已经发展了11年了。`gradle` 是作为 `android` 的官方构建工具引入的，除了 `java` ，它还能够支持多种语言的构建，所以用途非常广泛。

`gradle` 是开源的构建工具，你可以使用 `groovy` 或者 `kotlin` 来编写 `gradle` 的脚本，所以说 `gradle` 是一个非常强大的，高度定制化的和非常快速的构建工具。

根据我的了解，虽然 `gradle` 非常强大，但是对于 `java` 程序员来说，一般还是都使用的 `maven`，或者同时提供 `maven` 和 `gradle` 两种构建方式。

为什么会这样呢？个人觉得有两个原因：

- 第一个原因是 `gradle` 安装文件和依赖包的网络环境，如果单单依靠国内的网络环境的话，非常难安装完成。

- 第二个原因就是 `gradle` 中需要自己编写构建脚本，相对于纯配置的脚本来说，比较复杂。

### 1.2.安装gradle和解决gradle安装的问题

`gradle` 需要 `java8` 的支持，所以，你首先需要安装好 `JDK8` 或者以上的版本。

```sh
❯ java -version
java version "1.8.0_151"
Java(TM) SE Runtime Environment (build 1.8.0_151-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.151-b12, mixed mode)
```

安装 `gradle` 有两种方式，一种就是最简单的从官网上面下载安装包。然后解压在某个目录，最后将PATH指向该目录下的bin即可：

```sh
❯ mkdir /opt/gradle
❯ unzip -d /opt/gradle gradle-6.7-bin.zip
❯ ls /opt/gradle/gradle-6.7
LICENSE  NOTICE  bin  README  init.d  lib  media

export PATH=$PATH:/opt/gradle/gradle-6.7/bin
```

如果你想使用包管理器，比如 `MAC` 上面的 `brew` 来进行管理的话，则可以这样安装：

```sh
brew install gradle
```

但是这样安装很有可能在下载 `gradle` 安装包的时候卡住。

```sh
==> Downloading https://services.gradle.org/distributions/gradle-6.4.1-bin.zip
##O#- #
```

怎么办呢？

这时候我们需要自行下载 `gradle-6.4.1-bin.zip` 安装包，然后将其放入http服务器中，让这个压缩包可以通过 `http` 协议来访问。

简单点的做法就是将这个zip文件拷贝到IDEA中，利用IDEA本地服务器的预览功能，获得zip的http路径，比如：http://localhost:63345/gradle/gradle-6.7-all.zip.

接下来就是最精彩的部分了，我们需要修改gradle.rb文件：

```sh
brew edit gradle
```

使用上面的命令可以修改 `gracle.rb` 文件，我们替换掉下面的一段：

```sh
  homepage "https://www.gradle.org/"
  url "https://services.gradle.org/distributions/gradle-6.7-all.zip"
  sha256 "0080de8491f0918e4f529a6db6820fa0b9e818ee2386117f4394f95feb1d5583"
```

url替换成为 `http://localhost:63345/gradle/gradle-6.7-all.zip`，而 `sha256` 可以使用 `sha256sum gradle-6.7-all.zip`这个命令来获取。

替换之后，重新执行 `brew install gradle` 即可安装完成。

安装完毕之后，我们使用gradle -v命令可以验证是否安装成功：

```sh
gradle -v

Welcome to Gradle 6.7!
```

### 1.3.Gradle特性

`gradle` 作为一种新的构建工具，因为它是依赖于 `groovy` 和 `kotlin` 脚本的，基于脚本的灵活性，我们通过自定义脚本基本上可以做任何想要的构建工作。

虽然说 `gradle` 可以做任何构建工作，但是 `gradle` 现在还是有一定的限制，那就是项目的依赖项目前只支持于 `maven` 和 `Ivy` 兼容的存储库以及文件系统。

`gradle` 通过各种预定义的插件，可以轻松的构建通用类型的项目，并且支持自定义的插件类型。

另外一个非常重要的特性是 `gradle` 是以任务为基础的，每一个 `build` 都包含了一系列的 `task`，这些 `task` 又有各自的依赖关系，然后这些 `task` 一起构成了一个有向无环图 `Directed Acyclic Graphs`(`DAGs`)。

有了这个 `DAG`，`gradle`就可以决定各个 `task` 的顺序，并执行他们。

我们看两个 `task DAG` 的例子，一个是通用的 `task`，一个是专门的编译 `java` 的例子：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424150730.png)

`task` 可以依赖 `task`，我们看个例子：

```java
task hello {
    doLast {
        println 'Hello world!'
    }
}
task intro {
    dependsOn hello
    doLast {
        println "I'm Gradle"
    }
}
```

一个 `task` 可以包含 `Actions`，`inputs`和 `Outputs`。根据需要这些类型可以自由组合。

#### 1.3.1.标准task

`Gradle` 包含了下面7种标准的 `task`：

- `clean` ：用来删除 `build` 目录和里面的一切。
- `check`：这是一个生命周期任务，通常做一些验证工作，比如执行测试任务等。
- `assemble` ：这是一个生命周期任务，用来生成可分发的文件，比如 `jar` 包。
- `build`：也是一个生命周期任务，用来执行测试任务和生成最后的 `production` 文件。通常我们不在 `build` 中直接做任何特定的任务操作，它一般是其他任务的组合。
- `buildConfiguration`： 组装 `configuration` 中指定的 `archives`。
- `uploadConfiguration`： 除了执行 `buildConfiguration` 之外，还会执行上传工作。
- `cleanTask`：删除特定的某个 `task` 的执行结果。

#### 1.3.2.Build phases

一个 `gradle` 的 `build` 包含了三个 `phases`：

- `Initialization`： 初始化阶段。`gradle` 支持一个或者多个 `project` 的 `build`。在初始化阶段，`gradle` 将会判断到底有哪些`project` 将会执行，并且为他们分别创建一个 `project` 实例。
- `Configuration`： 配置阶段。`gradle` 将会执行 `build` 脚本，然后分析出要运行的 `tasks`。
- `Execution`：执行阶段。`gradle` 将会执行 `configuration` 阶段分析出来的tasks。

### 1.4.Gradle Wrapper

上面讲的是 `gradle` 的手动安装，如果是在多人工作的环境中使用了 `gradle`，有没有什么办法可以不用手动安装 `gradle` 就可以自动运行 `gradle` 程序呢？

方法当然有，那就是 `gradle wrapper`:

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424150842.png)

`gradle wrapper` 是一个工具，通过它我们可以方便的对本地的 `gradle` 进行管理。

上图列出了 `gradle wrapper` 的工作流程，第一步是去下载 `gradle` 的安装文件，第二步是将这个安装文件解压到 `gradle` 的用户空间，第三步就是使用这个解压出来的 `gradle` 了。

我们先看下怎么创建 `gradle wrapper`：

虽然 `Gradle wrapper` 的作用是帮我们下载和安装 `gradle`，但是要生成 `gradle wrapper` 需要使用 `gradle` 命令才行。也就是说有了`wrapper` 你可以按照成功 `gradle`，有了 `gradle` 你才可以生成 `gradle wrapper`。

假如我们已经手动按照好了 `gradle`，那么可以执行下面的命令来生成 `gradle wrapper`：

```sh
$ gradle wrapper
> Task :wrapper

BUILD SUCCESSFUL in 0s
1 actionable task: 1 executed
```

先看下生成出来的文件结构：

```sh
.
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
└── gradlew.bat
```

`gradle/wrapper/gradle-wrapper.properties` 是  `wrapper` 的配置文件，我们看下里面的内容：

```sh
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-6.7-all.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

其中 `distributionUrl` 就是我们要下载的 `gradle` 的路径，其他的配置是 `gradle` 的安装目录。

一般来说有两种安装文件类型：`bin` 和 `all`。`bin` 和 `all` 的区别在于，`bin` 只有安装文件，而 `all` 还包含了 `gradle` 的文档和样例代码。

我们可以通过 `--distribution-type` 参数来修改安装文件的类型。此外还有 `--gradle-version` ，`--gradle-distribution-url` 和 `--gradle-distribution-sha256-sum` 这几个参数可以使用。

```sh
$ gradle wrapper --gradle-version 6.7 --distribution-type all
> Task :wrapper

BUILD SUCCESSFUL in 0s
1 actionable task: 1 executed
```

除了配置文件之外，我们还有3个文件：

- `gradle-wrapper.jar`： `wrapper` 业务逻辑的实现文件。
- `gradlew`, `gradlew.bat` ：使用 `wrapper` 执行 `build` 的执行文件。也就是说我们可以使用 `wrapper` 来执行 `gradle` 的 `build` 任务。

#### 1.4.1.wrapper的使用

我们可以这样使用 `gradlew`,来执行 `build`：

```sh
gradlew.bat build
```

> 注意，如果你是第一次在项目中执行 `build` 命令的话，将会自动为你下载和安装 `gradle`。

#### 1.4.2.wrapper的升级

如果我们想要升级 `gradle` 的版本，也很简单：

```sh
./gradlew wrapper --gradle-version 6.7
```

或者直接修改 `gradle-wrapper.properties` 也可以。

### 1.5.一个简单的build.gradle

我们看一个非常简单的gradle的例子：

```java
plugins {
    id 'application' 
}

repositories {
    jcenter() 
}

dependencies {
    testImplementation 'junit:junit:4.13' 

    implementation 'com.google.guava:guava:29.0-jre' 
}

application {
    mainClass = 'demo.App' 
}
```

上面我们需要安装一个 `application plugin`，使用的是 `jcenter` 的依赖仓库，还指定了几个具体的依赖项。最后，指明了我们应用程序的 `mainClass`。

### 1.6.gradle使用maven仓库

`build.gradle` 中的 `repositories` 指明的是使用的仓库选项。

默认情况下 `gradle` 有自己的本地仓库,一般在 `~/.gradle` 目录下面，如果我们之前用的是 `maven` 仓库，那么在本地的 `maven` 仓库中已经存在了很多依赖包了，如何重用呢？

我们可以这样修改 `repositories`：

```java
mavenLocal()
mavenCentral()
```

这样的话, 就会优先从 `maven` 的仓库中查找所需的 `jar` 包。

## 2.深入了解gradle和maven的区别

### 2.1.gradle和maven的比较

虽然 `gradle` 和 `maven` 都可以作为java程序的构建工具。但是两者还是有很大的不同之处的。我们可以从下面几个方面来进行分析。

#### 2.1.1.可扩展性

`Google` 选择 `gradle` 作为 `android` 的构建工具不是没有理由的，其中一个非常重要的原因就是因为 `gradle` 够灵活。一方面是因为`gradle` 使用的是 `groovy` 或者 `kotlin` 语言作为脚本的编写语言，这样极大的提高了脚本的灵活性，但是其本质上的原因是 `gradle` 的基础架构能够支持这种灵活性。

你可以使用 `gradle` 来构建 `native` 的 `C/C++` 程序，甚至扩展到任何语言的构建。

相对而言，`maven` 的灵活性就差一些，并且自定义起来也比较麻烦，但是 `maven` 的项目比较容易看懂，并且上手简单。

所以如果你的项目没有太多自定义构建需求的话还是推荐使用maven，但是如果有自定义的构建需求，那么还是投入gradle的怀抱吧。

#### 2.1.2.性能比较

虽然现在大家的机子性能都比较强劲，好像在做项目构建的时候性能的优势并不是那么的迫切，但是对于大型项目来说，一次构建可能会需要很长的时间，尤其对于自动化构建和CI的环境来说，当然希望这个构建是越快越好。

`Gradle` 和 `Maven` 都支持并行的项目构建和依赖解析。但是 `gradle` 的三个特点让 `gradle` 可以跑的比 `maven` 快上一点：

- 增量构建

  `gradle` 为了提升构建的效率，提出了增量构建的概念，为了实现增量构建，`gradle` 将每一个 `task` 都分成了三部分，分别是 `input` 输入，任务本身和 `output` 输出。下图是一个典型的 `java` 编译的 `task`。

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151623.png)

  以上图为例，`input` 就是目标 `jdk` 的版本，源代码等，`output` 就是编译出来的 `class` 文件。

  增量构建的原理就是监控 `input` 的变化，只有 `input` 发送变化了，才重新执行 `task` 任务，否则 `gradle` 认为可以重用之前的执行结果。

  所以在编写 `gradle` 的 `task` 的时候，需要指定 `task` 的输入和输出。

  并且要注意只有会对输出结果产生变化的才能被称为输入，如果你定义了对初始结果完全无关的变量作为输入，则这些变量的变化会导致 `gradle` 重新执行 `task`，导致了不必要的性能的损耗。

  还要注意不确定执行结果的任务，比如说同样的输入可能会得到不同的输出结果，那么这样的任务将不能够被配置为增量构建任务。

- 构建缓存

  `gradle` 可以重用同样 `input` 的输出作为缓存，大家可能会有疑问了，这个缓存和增量编译不是一个意思吗？

  在同一个机子上是的，但是缓存可以跨机器共享.如果你是在一个CI服务的话，`build cache` 将会非常有用。因为 `developer` 的`build` 可以直接从 `CI` 服务器上面拉取构建结果，非常的方便。

- `Gradle` 守护进程

  `gradle` 会开启一个守护进程来和各个 `build` 任务进行交互，优点就是不需要每次构建都初始化需要的组件和服务。

  同时因为守护进程是一个一直运行的进程，除了可以避免每次 `JVM` 启动的开销之外，还可以缓存项目结构，文件，`task` 和其他的信息，从而提升运行速度。

  我们可以运行 `gradle --status` 来查看正在运行的 `daemons` 进程。

  从 `Gradle 3.0` 之后，`daemons` 是默认开启的，你可以使用 `org.gradle.daemon=false` 来禁止 `daemons`。

我们可以通过下面的几个图来直观的感受一下 `gradle` 和 `maven` 的性能比较：

- 使用 `gradle` 和 `maven` 构建 `Apache Commons Lang 3` 的比较：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151736.png)

- 使用 `gradle` 和 `maven` 构建小项目（10个模块，每个模块50个源文件和50个测试文件）的比较：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151750.png)

- 使用 `gradle` 和 `maven` 构建大项目（500个模块，每个模块100个源文件和100个测试文件）的比较：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151803.png)

可以看到gradle性能的提升是非常明显的。

#### 2.1.3.依赖的区别

`gralde` 和 `maven` 都可以本地缓存依赖文件，并且都支持依赖文件的并行下载。

在 `maven` 中只可以通过版本号来覆盖一个依赖项。而 `gradle` 更加灵活，你可以自定义依赖关系和替换规则，通过这些替换规则，`gradle` 可以构建非常复杂的项目。

### 2.2.从maven迁移到gradle

因为 `maven` 出现的时间比较早，所以基本上所有的java项目都支持 `maven`，但是并不是所有的项目都支持 `gradle`。如果你有需要把`maven` 项目迁移到 `gradle` 的想法，那么就一起来看看吧。

根据我们之前的介绍，大家可以发现 `gradle` 和 `maven` 从本质上来说就是不同的，`gradle` 通过 `task` 的 `DAG` 图来组织任务，而`maven` 则是通过 `attach` 到 `phases` 的 `goals` 来执行任务。

虽然两者的构建有很大的不同，但是得益于 `gradle `和 `maven` 相识的各种约定规则，从 `maven` 移植到 `gradle` 并不是那么难。

要想从 `maven` 移植到 `gradle`，首先要了解下 `maven` 的 `build` 生命周期，`maven` 的生命周期包含了 `clean`，`compile`，`test`，`package`，`verify`，`install` 和 `deploy` 这几个 `phase`。

我们需要将 `maven` 的生命周期 `phase` 转换为 `gradle` 的生命周期 `task`。这里需要使用到 `gradle` 的 `Base Plugin`，`Java Plugin` 和 `Maven Publish Plugin`。

先看下怎么引入这三个 `plugin`：

```java
plugins {
    id 'base'
    id 'java'
    id 'maven-publish'
}
```

`clean` 会被转换成为 `clean task`，`compile`会被转换成为 `classes task`，`test` 会被转换成为 `test task`，`package` 会被转换成为 `assemble task`，`verify` 会被转换成为 `check task`，`install` 会被转换成为 `Maven Publish Plugin` 中的`publishToMavenLocal task`，`deploy` 会被转换成为 `Maven Publish Plugin` 中的 `publish task`。

有了这些 `task` 之间的对应关系，我们就可以尝试进行 `maven` 到 `gradle` 的转换了。

#### 2.2.1.自动转换

我们除了可以使用 `gradle init` 命令来创建一个 `gradle` 的架子之外，还可以使用这个命令来将 `maven` 项目转换成为 `gradle` 项目，`gradle init` 命令会去读取 `pom` 文件，并将其转换成为 `gradle` 项目。

#### 2.2.2.转换依赖

`gradle` 和 `maven` 的依赖都包含了 `group ID`, `artifact ID` 和版本号。两者本质上是一样的，只是形式不同，我们看一个转换的例子：

```xml
<dependencies>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>1.2.12</version>
    </dependency>
</dependencies>
```

上是一个maven的例子，我们看下 `gradle` 的例子怎写：

```java
dependencies {
    implementation 'log4j:log4j:1.2.12'  
}
```

可以看到 `gradle` 比 `maven` 写起来要简单很多。

注意这里的 `implementation` 实际上是由 ` Java Plugin` 来实现的。

我们在 `maven` 的依赖中有时候还会用到 `scope` 选项，用来表示依赖的范围，我们看下这些范围该如何进行转换：

- `compile`：

  在 `gradle` 可以有两种配置来替换 `compile`，我们可以使用 `implementation` 或者 `api`。

  前者在任何使用 `Java Plugin` 的 `gradle` 中都可以使用，而 `api` 只能在使用 `Java Library Plugin` 的项目中使用。

  当然两者是有区别的，如果你是构建应用程序或者 `webapp`，那么推荐使用 `implementation`，如果你是在构建 `Java libraries`，那么推荐使用 `api`。

- `runtime`：

  可以替换成 `runtimeOnly` 。

- `test`：

  `gradle` 中的 `test` 分为两种，一种是编译 `test` 项目的时候需要，那么可以使用 `testImplementation`，一种是运行 `test` 项目的时候需要，那么可以使用 `testRuntimeOnly`。

- `provided`：

  可以替换成为 `compileOnly`。

- `import`：

  在 `maven` 中，`import` 经常用在 `dependencyManagement` 中，通常用来从一个 `pom` 文件中导入依赖项，从而保证项目中依赖项目版本的一致性。

在 `gradle` 中，可以使用 `platform()` 或者 `enforcedPlatform()` 来导入 `pom` 文件：

```java
dependencies {
    implementation platform('org.springframework.boot:spring-boot-dependencies:1.5.8.RELEASE') 

    implementation 'com.google.code.gson:gson' 
    implementation 'dom4j:dom4j'
}
```

比如上面的例子中，我们导入了 `spring-boot-dependencies`。因为这个 `pom` 中已经定义了依赖项的版本号，所以我们在后面引入`gson` 的时候就不需要指定版本号了。

`platform` 和 `enforcedPlatform` 的区别在于，`enforcedPlatform` 会将导入的 `pom` 版本号覆盖其他导入的版本号：

```java
dependencies {
    // import a BOM. The versions used in this file will override any other version found in the graph
    implementation enforcedPlatform('org.springframework.boot:spring-boot-dependencies:1.5.8.RELEASE')

    // define dependencies without versions
    implementation 'com.google.code.gson:gson'
    implementation 'dom4j:dom4j'

    // this version will be overridden by the one found in the BOM
    implementation 'org.codehaus.groovy:groovy:1.8.6'
}
```

#### 2.2.3.转换repositories仓库

`gradle` 可以兼容使用 `maven` 或者 `lvy` 的 `repository`。`gradle`没有默认的仓库地址，所以你必须手动指定一个。

你可以在 `gradle` 使用 `maven` 的仓库：

```java
repositories {
    mavenCentral()
}
```

我们还可以直接指定 `maven` 仓库的地址：

```java
repositories {
    maven {
        url "http://repo.mycompany.com/maven2"
    }
}
```

如果你想使用 `maven` 本地的仓库，则可以这样使用：

```java
repositories {
    mavenLocal()
}
```

但是 `mavenLocal` 是不推荐使用的，为什么呢？

`mavenLocal` 只是maven在本地的一个cache，它包含的内容并不完整。比如说一个本地的 `maven repository module` 可能只包含了`jar` 包文件，并没有包含 `source` 或者 `javadoc` 文件。那么我们将不能够在 `gradle` 中查看这个 `module` 的源代码，因为 `gradle` 会首先在 `maven` 本地的路径中查找这个 `module`。

并且本地的 `repository` 是不可信任的，因为里面的内容可以轻易被修改，并没有任何的验证机制。

#### 2.2.4.控制依赖的版本

如果同一个项目中对同一个模块有不同版本的两个依赖的话，默认情况下 `Gradle` 会在解析完 `DAG` 之后，选择版本最高的那个依赖包。

但是这样做并不一定就是正确的， 所以我们需要自定义依赖版本的功能。

首先就是上面我们提到的使用 `platform()` 和 `enforcedPlatform()` 来导入 `BOM`（`packaging` 类型是 `POM` 的）文件。

如果我们项目中依赖了某个 `module`，而这个 `module` 又依赖了另外的 `module`，我们叫做传递依赖。在这种情况下，如果我们希望控制传递依赖的版本，比如说将传递依赖的版本升级为一个新的版本，那么可以使用 `dependency constraints`：

```java
dependencies {
    implementation 'org.apache.httpcomponents:httpclient'
    constraints {
        implementation('org.apache.httpcomponents:httpclient:4.5.3') {
            because 'previous versions have a bug impacting this application'
        }
        implementation('commons-codec:commons-codec:1.11') {
            because 'version 1.9 pulled from httpclient has bugs affecting this application'
        }
    }
}
```

> 注意，`dependency constraints` 只对传递依赖有效，如果上面的例子中 `commons-codec` 并不是传递依赖，那么将不会有任何影响。

> 同时 `Dependency constraints` 需要 `Gradle Module Metadata` 的支持，也就是说只有你的 `module` 是发布在 `gradle` 中才支持这个特性，如果是发布在 `maven` 或者 `ivy` 中是不支持的。

上面讲的是传递依赖的版本升级。同样是传递依赖，如果本项目也需要使用到这个传递依赖的 `module`，但是需要使用到更低的版本（因为默认 `gradle` 会使用最新的版本），就需要用到版本降级了。

```java
dependencies {
    implementation 'org.apache.httpcomponents:httpclient:4.5.4'
    implementation('commons-codec:commons-codec') {
        version {
            strictly '1.9'
        }
    }
}
```

我们可以在 `implementation` 中指定特定的 `version` 即可。

`strictly` 表示的是强制匹配特定的版本号，除了 `strictly` 之外，还有 `require`，表示需要的版本号大于等于给定的版本号。prefer，如果没有指定其他的版本号，那么就使用 `prefer` 这个。`reject`，拒绝使用这个版本。

除此之外，你还可以使用 `Java Platform Plugin` 来指定特定的 `platform`，从而限制版本号。

最后看一下如何 `exclude` 一个依赖：

```java
dependencies {
    implementation('commons-beanutils:commons-beanutils:1.9.4') {
        exclude group: 'commons-collections', module: 'commons-collections'
    }
}
```

#### 2.2.5.多模块项目

maven中可以创建多模块项目：

```xml
<modules>
    <module>simple-weather</module>
    <module>simple-webapp</module>
</modules>
```

我们可以在gradle中做同样的事情 `settings.gradle`：

```java
rootProject.name = 'simple-multi-module'  

include 'simple-weather', 'simple-webapp'  
```

#### 2.2.6.profile和属性

`maven` 中可以使用 `profile` 来区别不同的环境，在 `gradle` 中，我们可以定义好不同的 `profile` 文件，然后通过脚本来加载他们：

`build.gradle`：

```java
if (!hasProperty('buildProfile')) ext.buildProfile = 'default'  

apply from: "profile-${buildProfile}.gradle"  

task greeting {
    doLast {
        println message  
    }
}
```

`profile-default.gradle`：

```java
ext.message = 'foobar'  
```

`profile-test.gradle`：

```java
ext.message = 'testing 1 2 3'
```

我们可以这样来运行：

```java
> gradle greeting
foobar

> gradle -PbuildProfile=test greeting
testing 1 2 3
```

#### 2.2.7.资源处理

在 `maven` 中有一个 `process-resources` 阶段，可以执行 `resources:resources` 用来进行 `resource` 文件的拷贝操作。

在 `Gradle` 中的 `Java plugin` 的 `processResources task` 也可以做相同的事情。

比如我可以执行 `copy` 任务：

```java
task copyReport(type: Copy) {
    from file("$buildDir/reports/my-report.pdf")
    into file("$buildDir/toArchive")
}
```

更加复杂的拷贝：

```java
task copyPdfReportsForArchiving(type: Copy) {
    from "$buildDir/reports"
    include "*.pdf"
    into "$buildDir/toArchive"
}
```

## 3.build script详解

### 3.1.project和task

`gradle` 是一个构建工具，所谓构建工具就是通过既定的各种规则，将原代码或者原文件通过一定的 `task` 处理过后，打包生成目标文件的步骤。

所以我们在 `gradle` 中有两个非常重要的概念，分别是项目和任务。

每一个 `gradle` 的构建任务可以包含一个或者多个项目，项目可以有多种类型，比如是一个 `web` 项目或者一个 `java lib` 项目等。为了实现 `project` 要完成的目标，需要定义一个个的 `task` 来辅助完成目标。

`task` 主要用来执行特定的任务，比如编译 `class` 文件，打包成 `jar`，生成 `javadoc` 等等。

### 3.2.一个例子

接下来我们使用一个具体的例子来讲解一下，`gradle` 到底是怎么用的。

首先我们创建一个新的 `project` 目录：

```sh
$ mkdir gradle-test
$ cd gradle-test
```

`gradle` 提供了一个 `init` 方法，来方便的创建 `gradle` 项目的骨架，我们用下看：

```sh
gradle init
Starting a Gradle Daemon (subsequent builds will be faster)

Select type of project to generate:
  1: basic
  2: application
  3: library
  4: Gradle plugin
Enter selection (default: basic) [1..4] 2

Select implementation language:
  1: C++
  2: Groovy
  3: Java
  4: Kotlin
  5: Scala
  6: Swift
Enter selection (default: Java) [1..6] 3

Split functionality across multiple subprojects?:
  1: no - only one application project
  2: yes - application and library projects
Enter selection (default: no - only one application project) [1..2] 1

Select build script DSL:
  1: Groovy
  2: Kotlin
Enter selection (default: Groovy) [1..2] 1

Select test framework:
  1: JUnit 4
  2: TestNG
  3: Spock
  4: JUnit Jupiter
Enter selection (default: JUnit 4) [1..4] 1

Project name (default: gradle-test):
Source package (default: gradle.test):

> Task :init
Get more help with your project: https://docs.gradle.org/6.7/samples/sample_building_java_applications.html

BUILD SUCCESSFUL in 45s
2 actionable tasks: 2 executed
```

按照你的需要，经过一系列的选择之后，就可以生成一个基本的gradle项目了。

我们看下生成的文件和目录：

```sh
.
├── app
│   ├── build.gradle
│   └── src
│       ├── main
│       │   ├── java
│       │   │   └── gradle
│       │   │       └── test
│       │   │           └── App.java
│       │   └── resources
│       └── test
│           ├── java
│           │   └── gradle
│           │       └── test
│           │           └── AppTest.java
│           └── resources
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
└── settings.gradle

14 directories, 8 files
```

其中 `gradle-wrapper` 是帮你自动设置和安装 `gradle` 的工具，同时它还提供了 `gradlew` 和 `gradlew.bat` 这两个执行文件，用来执行`gradle` 的任务。

我们主要看其中的两个配置文件，`settings.gradle` 和 `build.gradle`。

`settings.gradle` 中配置的是 `gradle` 中要 `build` 的项目信息：

```java
rootProject.name = 'gradle-test'
include('app')
```

上面的例子中，`rootProject.name` 指定了项目的名字，`include('app')` 表示需要引入一个叫做 `app` 的子项目，这个子项目中包含着实际的要打包的内容。

再看一下 `app` 中的 `build.gradle` 文件：

```java
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    id 'application'
}

repositories {
    // Use JCenter for resolving dependencies.
    jcenter()
}

dependencies {
    // Use JUnit test framework.
    testImplementation 'junit:junit:4.13'

    // This dependency is used by the application.
    implementation 'com.google.guava:guava:29.0-jre'
}

application {
    // Define the main class for the application.
    mainClass = 'gradle.test.App'
}
```

很简单，指定了插件，仓库地址，依赖包和应用程序的main class路径。

一切准备好之后，我们就可以进行构建和运行了。

有两种方式来运行，一种方式就是使用系统自带的 `gradle` 命令，一种方式就是使用刚刚gradle为你生成的 `gradlew`。

```java
gradle run

> Configure project :app
Repository ${repo.url} replaced by $REPOSITORY_URL .

> Task :app:run
Hello World!
gradle build

> Configure project :app
Repository ${repo.url} replaced by $REPOSITORY_URL .

BUILD SUCCESSFUL in 2s
7 actionable tasks: 6 executed, 1 up-to-date
```

你还可以带上 `--scan` 参数将 `build`上传到 `gradle scan` 中，得到更加详细的构建分析：

```java
./gradlew build --scan

BUILD SUCCESSFUL in 0s
7 actionable tasks: 7 executed

Publishing a build scan to scans.gradle.com requires accepting the Gradle Terms of Service defined at https://gradle.com/terms-of-service.
Do you accept these terms? [yes, no] yes

Gradle Terms of Service accepted.

Publishing build scan...
https://gradle.com/s/5u4w3gxeurtd2
```

### 3.3.task详细讲解

上面的例子中，我们使用的都是 `gradle` 默认的 `tasks`，并没有看到自定义 `task` 的使用，接下来我们将会探讨一下，如何在`build.gradle` 编写自己的 `task`。

这里我们使用的 `groovy` 来编写 `build.gradle`，所以我们可以像运行代码一样来运行它。

#### 3.3.1.task脚本

先创建一个非常简单的task：

```java
task hello {
    doLast {
        println 'Hello www.flydean.com!'
    }
}
```

上面定义了一个名叫 `hello` 的 `task`，并且会在执行最后输出 "Hello www.flydean.com!"。

我们这样运行：

```sh
gradle -q hello
Hello www.flydean.com!
```

`-q` 的意思是悄悄的执行，将会忽略 `gradle` 自身的 `log` 信息。我们把要执行的 `task` 名字写在 `gradle` 后面就可以了。

如果你熟悉 `ant` 命令的话，可以看到 `gradle` 的 `task` 和 `ant` 很类似，不过更加的强大。

因为是 `groovy` 脚本，所以我们可以在其中执行代码：

```java
task upper {
    doLast {
        String someString = 'www.flydean.com'
        println "Original: $someString"
        println "Upper case: ${someString.toUpperCase()}"
    }
}
```

运行结果：

```java
> gradle -q upper
Original: www.flydean.com
Upper case: WWW.FLYDEAN.COM
```

或者执行times操作：

```java
task count {
    doLast {
        4.times { print "$it " }
    }
}
> gradle -q count
0 1 2 3
```

#### 3.3.2.task依赖

`gradle` 中的一个 `task` 可以依赖其他的 `task`：

```java
task hello {
    doLast {
        println 'Hello www.flydean.com!'
    }
}
task intro {
    dependsOn hello
    doLast {
        println "I'm flydean"
    }
}
```

上面两个task的顺序是无关的，可以依赖的写在前面，被依赖的写在后面，或者反过来都成立。

#### 3.3.3.动态task

除了静态的task之外，我们还可以通过代码来动态创建task：

```java
4.times { counter ->
    task "task$counter" {
        doLast {
            println "I'm task number $counter"
        }
    }
}
> gradle -q task1
I'm task number 1
```

我们还可以将task看做成为一个对象，调用gradle的api进行操作：

```java
4.times { counter ->
    task "task$counter" {
        doLast {
            println "I'm task number $counter"
        }
    }
}
task0.dependsOn task2, task3
```

上面的例子中，我们调用API手动创建了task之间的依赖关系：

```sh
> gradle -q task0
I'm task number 2
I'm task number 3
I'm task number 0
```

还可以task之间的属性调用：

```java
task myTask {
    ext.myProperty = "www.flydean.com"
}

task printTaskProperties {
    doLast {
        println myTask.myProperty
    }
}
```

#### 3.3.4.默认task

如果不想每次都在调用 `gradle` 命令的时候手动指定某个具体的 `task` 名字，我们可以使用 `defaultTasks`：

```java
defaultTasks 'clean', 'run'

task clean {
    doLast {
        println 'Default Cleaning!'
    }
}

task run {
    doLast {
        println 'Default Running!'
    }
}

task other {
    doLast {
        println "I'm not a default task!"
    }
}
```

上面的代码执行 `gradle` 和 `gradle clean run` 是相当的。

#### 3.3.5.build script的外部依赖

既然 `build script` 可以用 `groovy` 代码来编写，那么如果我们想要在 `build script` 中使用外部的 `jar` 包怎么办呢？

这个时候，我们可以将外部依赖放到 `buildscript()` 方法中，后面的 `task` 就可以使用引入的依赖了：

```java
import org.apache.commons.codec.binary.Base64

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath group: 'commons-codec', name: 'commons-codec', version: '1.2'
    }
}

task encode {
    doLast {
        def byte[] encodedString = new Base64().encode('hello world\n'.getBytes())
        println new String(encodedString)
    }
}
```

上面的例子中，`encode` 使用了一个外部的依赖包 `Base64`，这个依赖包是在 `buildscript` 方法中引入的。

## 4.深入理解gradle中的task

### 4.1.定义task

定义一个 `task` 可以有很多种方式，比如下面的使用 `string` 作为 `task` 的名字：

```java
task('hello') {
    doLast {
        println "hello"
    }
}

task('copy', type: Copy) {
    from(file('srcDir'))
    into(buildDir)
}
```

还可以使用tasks容器来创建：

```java
tasks.create('hello') {
    doLast {
        println "hello"
    }
}

tasks.create('copy', Copy) {
    from(file('srcDir'))
    into(buildDir)
}
```

上面的例子中，我们使用 `tasks.create` 方法，将新创建的 `task` 加到 `tasks` 集合中。

我们还可以使用 `groovy` 特有的语法来定义一个 `task`：

```java
task(hello) {
    doLast {
        println "hello"
    }
}

task(copy, type: Copy) {
    from(file('srcDir'))
    into(buildDir)
}
```

### 4.2.tasks 集合类

上面我们在创建 `task` 的时候，使用了 `tasks` 集合类来创建 `task`。

实际上，`tasks` 集合类是一个非常有用的工具类，我们可以使用它来做很多事情。

直接在 `build` 文件中使用 `tasks`，实际上是引用了 `TaskContainer` 的一个实例对象。我们还可以使用 `Project.getTasks()` 来获取这个实例对象。

我们看下 `TaskContainer` 的定义：

```java
public interface TaskContainer extends TaskCollection<Task>, PolymorphicDomainObjectContainer<Task> 
```

从定义上，我们可以看出 `TaskContainer`是一个 `task` 的集合和域对象的集合。

`taskContainer` 中有四类非常重要的方法：

第一类是定位 `task` 的方法，有个分别是 `findByPath` 和 `getByPath` 。两个方法的区别就是 `findByPath` 如果没找到会返回null，而`getByPath` 没找到的话会抛出 `UnknownTaskException`。

看下怎么使用：

```java
task hello

println tasks.getByPath('hello').path
println tasks.getByPath(':hello').path
```

输出：

```java
:hello
:hello
```

第二类是创建task的方法create，create方法有多种实现，你可以直接通过名字来创建一个task：

```java
task('hello') {
    doLast {
        println "hello"
    }
}
```

也可以创建特定类型的task：

```java
task('copy', type: Copy) {
    from(file('srcDir'))
    into(buildDir)
}
```

还可以创建带参数的构造函数的task：

```java
class CustomTask extends DefaultTask {
    final String message
    final int number

    @Inject
    CustomTask(String message, int number) {
        this.message = message
        this.number = number
    }
}
```

上面我们为CustomTask创建了一个带参数的构造函数，注意，这里需要带上@javax.inject.Inject注解，表示我们后面可以传递参数给这个构造函数。

我们可以这样使用：

```java
tasks.create('myTask', CustomTask, 'hello', 42)
```

也可以这样使用：

```java
task myTask(type: CustomTask, constructorArgs: ['hello', 42])
```

第三类是register，register也是用来创建新的task的，不过register执行的是延迟创建。也就是说只有当task被需要使用的时候才会被创建。

我们先看一个register方法的定义：

```java
TaskProvider<Task> register(String name,
                            Action<? super Task> configurationAction)
                     throws InvalidUserDataException 
```

可以看到register返回了一个TaskProvider，有点像java多线程中的callable,当我们调用Provider.get()获取task值的时候，才会去创建这个task。

或者我们调用TaskCollection.getByName(java.lang.String)的时候也会创建对应的task。

最后一类是replace方法：

```java
Task replace(String name)
<T extends Task> T replace(String name,
                           Class<T> type)
```

replace的作用就是创建一个新的task，并且替换掉同样名字的老的task。

### 4.3.Task 之间的依赖

task之间的依赖关系是通过task name来决定的。我们可以在同一个项目中做task之间的依赖：

```java
task hello {
    doLast {
        println 'Hello www.flydean.com!'
    }
}
task intro {
    dependsOn hello
    doLast {
        println "I'm flydean"
    }
}
```

也可以跨项目进行task的依赖，如果是跨项目的task依赖的话，需要制定task的路径：

```java
project('project-a') {
    task taskX {
        dependsOn ':project-b:taskY'
        doLast {
            println 'taskX'
        }
    }
}

project('project-b') {
    task taskY {
        doLast {
            println 'taskY'
        }
    }
}
```

或者我们可以在定义好task之后，再处理task之间的依赖关系：

```java
task taskX {
    doLast {
        println 'taskX'
    }
}

task taskY {
    doLast {
        println 'taskY'
    }
}
```

还可以动态添加依赖关系：

```java
task taskX {
    doLast {
        println 'taskX'
    }
}

// Using a Groovy Closure
taskX.dependsOn {
    tasks.findAll { task -> task.name.startsWith('lib') }
}

task lib1 {
    doLast {
        println 'lib1'
    }
}

task lib2 {
    doLast {
        println 'lib2'
    }
}

task notALib {
    doLast {
        println 'notALib'
    }
}
```

### 4.4.定义task之间的顺序

有时候我们的task之间是有执行顺序的，我们称之为对task的排序ordering。

先看一下ordering和dependency有什么区别。dependency表示的是一种强依赖关系，如果taskA依赖于taskB，那么执行taskA的时候一定要先执行taskB。

而ordering则是一种并不太强列的顺序关系。表示taskA需要在taskB之后执行，但是taskB不执行也可以。

在gradle中有两种order：分别是must run after和should run after。

taskA.mustRunAfter(taskB)表示必须遵守的顺序关系，而taskA.shouldRunAfter(taskB)则不是必须的，在下面两种情况下可以忽略这样的顺序关系：

第一种情况是如果shouldRunAfter引入了order循环的时候。

第二种情况是如果在并行执行的情况下，task所有的依赖关系都已经满足了，那么也会忽略这个顺序。

我们看下怎么使用：

```java
task taskX {
    doLast {
        println 'flydean.com'
    }
}
task taskY {
    doLast {
        println 'hello'
    }
}
taskY.mustRunAfter taskX
//taskY.shouldRunAfter taskX
```

### 4.5.给task一些描述

我们可以给task一些描述信息，这样我们在执行gradle tasks的时候，就可以查看到：

```java
task copy(type: Copy) {
   description 'Copies the resource directory to the target directory.'
   from 'resources'
   into 'target'
   include('**/*.txt', '**/*.xml', '**/*.properties')
}
```

### 4.6.task的条件执行

有时候我们需要根据build文件中的某些属性来判断是否执行特定的task，我们可以使用onlyIf ：

```java
task hello {
    doLast {
        println 'www.flydean.com'
    }
}

hello.onlyIf { !project.hasProperty('skipHello') }
```

或者我们可以抛出StopExecutionException异常，如果遇到这个异常，那么task后面的任务将不会被执行：

```java
task compile {
    doLast {
        println 'We are doing the compile.'
    }
}

compile.doFirst {
    if (true) { throw new StopExecutionException() }
}
task myTask {
    dependsOn('compile')
    doLast {
        println 'I am not affected'
    }
}
```

我们还可以启动和禁用task：

```java
myTask.enabled = false
```

最后我们还可以让task超时，当超时的时候，执行task的线程将会被中断，并且task将会被标记为failed。

如果我们想继续执行，那么可以使用 --continue。

> 注意， 只有能够响应中断的task，timeout才有用。

```java
task hangingTask() {
    doLast {
        Thread.sleep(100000)
    }
    timeout = Duration.ofMillis(500)
}
```

### 4.7.task rule

如果我们想要给某些task定义一些规则，那么可以使用tasks.addRule：

```java
tasks.addRule("Pattern: ping<ID>") { String taskName ->
    if (taskName.startsWith("ping")) {
        task(taskName) {
            doLast {
                println "Pinging: " + (taskName - 'ping')
            }
        }
    }
}
```

上我们定义了一个rule，如果taskName是以ping开头的话，那么将会输出对应的内容。

看下运行结果：

```java
> gradle -q pingServer1
Pinging: Server1
```

我还可以将这些rules作为依赖项引入：

```java
task groupPing {
    dependsOn pingServer1, pingServer2
}
```

### 4.8.Finalizer tasks

和java中的finally一样，task也可以指定对应的finalize task：

```java
task taskX {
    doLast {
        println 'taskX'
    }
}
task taskY {
    doLast {
        println 'taskY'
    }
}

taskX.finalizedBy taskY

> gradle -q taskX
taskX
taskY
```

finalize task是一定会被执行的，即使上面的taskX中抛出了异常。

## 5.gradle中的增量构建

### 5.1.增量构建

gradle为了提升构建的效率，提出了增量构建的概念，为了实现增量构建，gradle将每一个task都分成了三部分，分别是input输入，任务本身和output输出。下图是一个典型的java编译的task。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424154541.png)

以上图为例，input就是目标jdk的版本，源代码等，output就是编译出来的class文件。

增量构建的原理就是监控input的变化，只有input发送变化了，才重新执行task任务，否则gradle认为可以重用之前的执行结果。

所以在编写gradle的task的时候，需要指定task的输入和输出。

并且要注意只有会对输出结果产生变化的才能被称为输入，如果你定义了对初始结果完全无关的变量作为输入，则这些变量的变化会导致gradle重新执行task，导致了不必要的性能的损耗。

还要注意不确定执行结果的任务，比如说同样的输入可能会得到不同的输出结果，那么这样的任务将不能够被配置为增量构建任务。

### 5.2.自定义inputs和outputs

既然task中的input和output在增量编译中这么重要，本章将会给大家讲解一下怎么才能够在task中定义input和output。

如果我们自定义一个task类型，那么满足下面两点就可以使用上增量构建了：

第一点，需要为task中的inputs和outputs添加必要的getter方法。

第二点，为getter方法添加对应的注解。

gradle支持三种主要的inputs和outputs类型：

1. 简单类型：简单类型就是所有实现了Serializable接口的类型，比如说string和数字。
2. 文件类型：文件类型就是 File 或者 FileCollection 的衍生类型，或者其他可以作为参数传递给 `Project.file(java.lang.Object)` 和 `Project.files(java.lang.Object...)` 的类型。
3. 嵌套类型：有些自定义类型，本身不属于前面的1，2两种类型，但是它内部含有嵌套的inputs和outputs属性，这样的类型叫做嵌套类型。

接下来，我们来举个例子，假如我们有一个类似于FreeMarker和Velocity这样的模板引擎，负责将模板源文件，要传递的数据最后生成对应的填充文件，我们考虑一下他的输入和输出是什么。

输入：模板源文件，模型数据和模板引擎。

输出：要输出的文件。

如果我们要编写一个适用于模板转换的task，我们可以这样写：

```java
import java.io.File;
import java.util.HashMap;
import org.gradle.api.*;
import org.gradle.api.file.*;
import org.gradle.api.tasks.*;

public class ProcessTemplates extends DefaultTask {
    private TemplateEngineType templateEngine;
    private FileCollection sourceFiles;
    private TemplateData templateData;
    private File outputDir;

    @Input
    public TemplateEngineType getTemplateEngine() {
        return this.templateEngine;
    }

    @InputFiles
    public FileCollection getSourceFiles() {
        return this.sourceFiles;
    }

    @Nested
    public TemplateData getTemplateData() {
        return this.templateData;
    }

    @OutputDirectory
    public File getOutputDir() { return this.outputDir; }

    // 上面四个属性的setter方法

    @TaskAction
    public void processTemplates() {
        // ...
    }
}
```

上面的例子中，我们定义了4个属性，分别是 `TemplateEngineType`，`FileCollection`，`TemplateData` 和 `File`。前面三个属性是输入，后面一个属性是输出。

除了getter和setter方法之外，我们还需要在getter方法中添加相应的注释： `@Input , @InputFiles ,@Nested 和 @OutputDirectory`, 除此之外，我们还定义了一个 `@TaskAction` 表示这个task要做的工作。

TemplateEngineType表示的是模板引擎的类型，比如FreeMarker或者Velocity等。我们也可以用String来表示模板引擎的名字。但是为了安全起见，这里我们自定义了一个枚举类型，在枚举类型内部我们可以安全的定义各种支持的模板引擎类型。

因为enum默认是实现Serializable的，所以这里可以作为@Input使用。

sourceFiles使用的是FileCollection，表示的是一系列文件的集合，所以可以使用@InputFiles。

为什么TemplateData是@Nested类型的呢？TemplateData表示的是我们要填充的数据，我们看下它的实现：

```java
import java.util.HashMap;
import java.util.Map;
import org.gradle.api.tasks.Input;

public class TemplateData {
    private String name;
    private Map<String, String> variables;

    public TemplateData(String name, Map<String, String> variables) {
        this.name = name;
        this.variables = new HashMap<>(variables);
    }

    @Input
    public String getName() { return this.name; }

    @Input
    public Map<String, String> getVariables() {
        return this.variables;
    }
}
```

可以看到，虽然TemplateData本身不是File或者简单类型，但是它内部的属性是简单类型的，所以TemplateData本身可以看做是@Nested的。

outputDir表示的是一个输出文件目录，所以使用的是@OutputDirectory。

使用了这些注解之后，gradle在构建的时候就会检测和上一次构建相比，这些属性有没有发送变化，如果没有发送变化，那么gradle将会直接使用上一次构建生成的缓存。

> 注意，上面的例子中我们使用了FileCollection作为输入的文件集合，考虑一种情况，假如只有文件集合中的某一个文件发送变化，那么gradle是会重新构建所有的文件，还是只重构这个被修改的文件呢？
> 留给大家讨论

除了上讲到的4个注解之外，gradle还提供了其他的几个有用的注解：

- `@InputFile`： 相当于File，表示单个input文件。
- `@InputDirectory`： 相当于File，表示单个input目录。
- `@Classpath`： 相当于Iterable，表示的是类路径上的文件，对于类路径上的文件需要考虑文件的顺序。如果类路径上的文件是jar的话，jar中的文件创建时间戳的修改，并不会影响input。
- `@CompileClasspath`：相当于Iterable，表示的是类路径上的java文件，会忽略类路径上的非java文件。
- `@OutputFile`： 相当于File，表示输出文件。
- `@OutputFiles`： 相当于Map<String, File> 或者 Iterable，表示输出文件。
- `@OutputDirectories`： 相当于Map<String, File> 或者 Iterable，表示输出文件。
- `@Destroys`： 相当于File 或者 Iterable，表示这个task将会删除的文件。
- `@LocalState`： 相当于File 或者 Iterable，表示task的本地状态。
- `@Console`： 表示属性不是input也不是output，但是会影响console的输出。
- `@Internal`： 内部属性，不是input也不是output。
- `@ReplacedBy`： 属性被其他的属性替换了，不能算在input和output中。
- `@SkipWhenEmpty`： 和@InputFiles 跟 @InputDirectory一起使用，如果相应的文件或者目录为空的话，将会跳过task的执行。
- `@Incremental`： 和@InputFiles 跟 @InputDirectory一起使用，用来跟踪文件的变化。
- `@Optional`： 忽略属性的验证。
- `@PathSensitive`： 表示需要考虑paths中的哪一部分作为增量的依据。

### 5.3.运行时API

自定义task当然是一个非常好的办法来使用增量构建。但是自定义task类型需要我们编写新的class文件。有没有什么办法可以不用修改task的源代码，就可以使用增量构建呢？

答案是使用Runtime API。

gradle提供了三个API，用来对input，output和Destroyables进行获取：

- Task.getInputs() of type TaskInputs
- Task.getOutputs() of type TaskOutputs
- Task.getDestroyables() of type TaskDestroyables

获取到input和output之后，我们就是可以其进行操作了，我们看下怎么用runtime API来实现之前的自定义task：

```java
task processTemplatesAdHoc {
    inputs.property("engine", TemplateEngineType.FREEMARKER)
    inputs.files(fileTree("src/templates"))
        .withPropertyName("sourceFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("templateData.name", "docs")
    inputs.property("templateData.variables", [year: 2013])
    outputs.dir("$buildDir/genOutput2")
        .withPropertyName("outputDir")

    doLast {
        // Process the templates here
    }
}
```

上面例子中，inputs.property() 相当于 @Input ，而outputs.dir() 相当于@OutputDirectory。

Runtime API还可以和自定义类型一起使用：

```java
task processTemplatesWithExtraInputs(type: ProcessTemplates) {
    // ...

    inputs.file("src/headers/headers.txt")
        .withPropertyName("headers")
        .withPathSensitivity(PathSensitivity.NONE)
}
```

上面的例子为ProcessTemplates添加了一个input。

### 5.4.隐式依赖

除了直接使用dependsOn之外，我们还可以使用隐式依赖：

```java
task packageFiles(type: Zip) {
    from processTemplates.outputs
}
```

上面的例子中，packageFiles 使用了from，隐式依赖了processTemplates的outputs。

gradle足够智能，可以检测到这种依赖关系。

上面的例子还可以简写为：

```java
task packageFiles2(type: Zip) {
    from processTemplates
}
```

我们看一个错误的隐式依赖的例子：

```java
plugins {
    id 'java'
}

task badInstrumentClasses(type: Instrument) {
    classFiles = fileTree(compileJava.destinationDir)
    destinationDir = file("$buildDir/instrumented")
}
```

这个例子的本意是执行compileJava任务，然后将其输出的destinationDir作为classFiles的值。

但是因为fileTree本身并不包含依赖关系，所以上面的执行的结果并不会执行compileJava任务。

我们可以这样改写：

```java
task instrumentClasses(type: Instrument) {
    classFiles = compileJava.outputs.files
    destinationDir = file("$buildDir/instrumented")
}
```

或者使用layout：

```java
task instrumentClasses2(type: Instrument) {
    classFiles = layout.files(compileJava)
    destinationDir = file("$buildDir/instrumented")
}
```

或者使用buildBy：

```java
task instrumentClassesBuiltBy(type: Instrument) {
    classFiles = fileTree(compileJava.destinationDir) {
        builtBy compileJava
    }
    destinationDir = file("$buildDir/instrumented")
}
```

### 5.5.输入校验

gradle会默认对@InputFile ，@InputDirectory 和 @OutputDirectory 进行参数校验。

如果你觉得这些参数是可选的，那么可以使用@Optional。

### 5.6.自定义缓存方法

上面的例子中，我们使用from来进行增量构建，但是from并没有添加@InputFiles， 那么它的增量缓存是怎么实现的呢？

我们看一个例子：

```java
public class ProcessTemplates extends DefaultTask {
    // ...
    private FileCollection sourceFiles = getProject().getLayout().files();

    @SkipWhenEmpty
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public FileCollection getSourceFiles() {
        return this.sourceFiles;
    }

    public void sources(FileCollection sourceFiles) {
        this.sourceFiles = this.sourceFiles.plus(sourceFiles);
    }

    // ...
}
```

上面的例子中，我们将sourceFiles定义为可缓存的input，然后又定义了一个sources方法，可以将新的文件加入到sourceFiles中，从而改变sourceFile input，也就达到了自定义修改input缓存的目的。

我们看下怎么使用：

```java
task processTemplates(type: ProcessTemplates) {
    templateEngine = TemplateEngineType.FREEMARKER
    templateData = new TemplateData("test", [year: 2012])
    outputDir = file("$buildDir/genOutput")

    sources fileTree("src/templates")
}
```

我们还可以使用project.layout.files()将一个task的输出作为输入，可以这样做：

```java
    public void sources(Task inputTask) {
        this.sourceFiles = this.sourceFiles.plus(getProject().getLayout().files(inputTask));
    }
```

这个方法传入一个task，然后使用project.layout.files()将task的输出作为输入。

看下怎么使用：

```java
task copyTemplates(type: Copy) {
    into "$buildDir/tmp"
    from "src/templates"
}

task processTemplates2(type: ProcessTemplates) {
    // ...
    sources copyTemplates
}
```

非常的方便。

如果你不想使用gradle的缓存功能，那么可以使用upToDateWhen()来手动控制：

```java
task alwaysInstrumentClasses(type: Instrument) {
    classFiles = layout.files(compileJava)
    destinationDir = file("$buildDir/instrumented")
    outputs.upToDateWhen { false }
}
```

上面使用false，表示alwaysInstrumentClasses这个task将会一直被执行，并不会使用到缓存。

### 5.7.输入归一化

要想比较gradle的输入是否是一样的，gradle需要对input进行归一化处理，然后才进行比较。

我们可以自定义gradle的runtime classpath 。

```java
normalization {
    runtimeClasspath {
        ignore 'build-info.properties'
    }
}
```

上面的例子中，我们忽略了classpath中的一个文件。

我们还可以忽略META-INF中的manifest文件的属性：

```java
normalization {
    runtimeClasspath {
        metaInf {
            ignoreAttribute("Implementation-Version")
        }
    }
}
```

忽略META-INF/MANIFEST.MF ：

```java
normalization {
    runtimeClasspath {
        metaInf {
            ignoreManifest()
        }
    }
}
```

忽略META-INF中所有的文件和目录：

```java
normalization {
    runtimeClasspath {
        metaInf {
            ignoreCompletely()
        }
    }
}
```

### 5.8.其他使用技巧

如果你的gradle因为某种原因暂停了，你可以送 --continuous 或者 -t 参数，来重用之前的缓存，继续构建gradle项目。

你还可以使用 --parallel 来并行执行task。

## 6.在gradle中构建java项目

### 6.1.构建java项目的两大插件

安装java项目的目的不同，构建java项目有两大插件，一个是application，表示构建的是java应用程序；一个是java-library，表示构建的是java库，供别的项目使用。

不管是构建应用程序还是java库，我们都可以很方便的使用gradle init来创新一个新的gradle项目：

```java
$ gradle init

Select type of project to generate:
  1: basic
  2: application
  3: library
  4: Gradle plugin
Enter selection (default: basic) [1..4] 2

Select implementation language:
  1: C++
  2: Groovy
  3: Java
  4: Kotlin
  5: Scala
  6: Swift
Enter selection (default: Java) [1..6] 3

Select build script DSL:
  1: Groovy
  2: Kotlin
Enter selection (default: Groovy) [1..2] 1

Select test framework:
  1: JUnit 4
  2: TestNG
  3: Spock
  4: JUnit Jupiter
Enter selection (default: JUnit 4) [1..4]

Project name (default: demo):
Source package (default: demo):


BUILD SUCCESSFUL
2 actionable tasks: 2 executed
```

application和library的不同之处在于第二步选择的不同。

两者在build.gradle中的不同在于plugins的不同，application的plugin是：

```java
plugins {
    id 'application' 
}
```

而library的plugin是：

```java
plugins {
    id 'java-library' 
}
```

还有一个不同之处是依赖的不同，先看一个application的依赖：

```java
dependencies {
    testImplementation 'junit:junit:4.13' 

    implementation 'com.google.guava:guava:29.0-jre' 
}
```

再看一个library的依赖：

```java
dependencies {
    testImplementation 'junit:junit:4.13' 

    api 'org.apache.commons:commons-math3:3.6.1' 

    implementation 'com.google.guava:guava:29.0-jre' 
}
```

因为library是需要给第三方应用程序使用的，所以这里多了一个api的使用，api表示是第三方应用程序也需要依赖这个包，而implementation表示的是该包只是在这个项目内部被依赖。

在构建libary的时候，还可以自定义manifest的信息：

```java
tasks.named('jar') {
    manifest {
        attributes('Implementation-Title': project.name,
                   'Implementation-Version': project.version)
    }
}
```

上面的例子将会在META-INF/MANIFEST.MF生成：

```java
Manifest-Version: 1.0
Implementation-Title: lib
Implementation-Version: 0.1.0
```

我们还可以指定编译的java版本号和lib的版本：

```java
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

version = '1.2.1'
```

### 6.2.管理依赖

java的依赖一般都是jar包组成的library。和maven一样，我们在gradle中指定依赖需要指定依赖的名字和版本号，依赖的范围：是运行时依赖还是编译时依赖，还有一个重要的就是在哪里可以找到这个library。

前面两个属性我们可以在dependencies中找到，后面一个我们可以在repositories中找到，看一个例子：

```java
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.hibernate:hibernate-core:3.6.7.Final'
}
```

还可以使用这种形式的maven：

```java
repositories {
    maven {
        url "http://repo.mycompany.com/maven2"
    }
}
```

或者Ivy：

```java
repositories {
    ivy {
        url "http://repo.mycompany.com/repo"
    }
}
```

甚至可以使用本地的local dir：

```java
repositories {
    flatDir {
        dirs 'lib'
    }
    flatDir {
        dirs 'lib1', 'lib2'
    }
}
```

上面定义了一个mavenCentral的仓库，我们可以在这个仓库中去查找hibernate-core这个依赖的jar包。

在dependencies这一块，我们可以定义依赖包的工作范围：

- compileOnly： 表示依赖包只被用来编译代码，并不用在程序的运行。
- implementation：表示依赖包被用在编译和运行时。
- runtimeOnly： 只在运行时使用。
- testCompileOnly： 仅在test的编译时使用。
- testImplementation：在test的编译和运行时使用。
- testRuntimeOnly： 在test的运行时使用。

我们还可以添加动态的依赖：

```java
dependencies {
    implementation 'org.springframework:spring-web:5.+'
}
```

使用项目作为依赖：

```java
dependencies {
    implementation project(':shared')
}
```

### 6.3.编译代码

一般情况下你的源代码需要放在src/main/java 目录下，测试代码需要放在src/test/java下面。然后添加compileOnly 或者 implementation依赖，如果需要测试的话，添加testCompileOnly或者testImplementation依赖。

然后就可以运行compileJava和compileTestJava来编译代码了。

当然，如果你有自定义的源文件目录，也可以这样手动指定：

```java
sourceSets {
    main {
         java {
            srcDirs = ['src']
         }
    }

    test {
        java {
            srcDirs = ['test']
        }
    }
}
```

上面的代码中我们给srcDirs重新赋值了。如果我们只是想要在现有的代码路径上再添加一个新的路径，那么可以使用srcDir：

```java
sourceSets {
    main {
        java {
            srcDir 'thirdParty/src/main/java'
        }
    }
}
```

除了源代码的路径，我们还可以配置编译的参数，并指定编译的JDK版本号：

```java
compileJava {
    options.incremental = true
    options.fork = true
    options.failOnError = false
    options.release = 7
}
```

> 注意，gradle必须要在JDK8以上才能运行，但是我们可以指定gradle去使用Java 6 或者 Java 7去编译源代码。

我们还可以指定预览版本的特性：

```java
tasks.withType(JavaCompile) {
    options.compilerArgs += "--enable-preview"
}
tasks.withType(Test) {
    jvmArgs += "--enable-preview"
}
tasks.withType(JavaExec) {
    jvmArgs += "--enable-preview"
}
```

### 6.4.管理resource

java除了源代码文件之外，还有一些resource文件，比如配置文件，图片文件，语言文件等等。我们需要将这些配置文件拷贝到特定的目标目录中。

默认情况下，gradle会拷贝src/[sourceSet]/resources 中的文件到目标文件夹中。

我们看一个复杂的拷贝动作：

```java
task copyDocs(type: Copy) {
    from 'src/main/doc'
    into 'build/target/doc'
}

//for Ant filter
import org.apache.tools.ant.filters.ReplaceTokens

//for including in the copy task
def dataContent = copySpec {
    from 'src/data'
    include '*.data'
}

task initConfig(type: Copy) {
    from('src/main/config') {
        include '**/*.properties'
        include '**/*.xml'
        filter(ReplaceTokens, tokens: [version: '2.3.1'])
    }
    from('src/main/config') {
        exclude '**/*.properties', '**/*.xml'
    }
    from('src/main/languages') {
        rename 'EN_US_(.*)', '$1'
    }
    into 'build/target/config'
    exclude '**/*.bak'

    includeEmptyDirs = false

    with dataContent
}
```

### 6.5.打包和发布

我们可以根据不同的构建类型来打包对应的文件。比如对应java lib来说，我们可以同时上传源代码和java doc文件：

```java
java {
    withJavadocJar()
    withSourcesJar()
}
```

比如说我们还可以打包成一个fat jar包：

```java
plugins {
    id 'java'
}

version = '1.0.0'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'commons-io:commons-io:2.6'
}

task uberJar(type: Jar) {
    archiveClassifier = 'uber'

    from sourceSets.main.output

    dependsOn configurations.runtimeClasspath
    from {
        configurations.runtimeClasspath.findAll { it.name.endsWith('jar') }.collect { zipTree(it) }
    }
}
```

### 6.6.生成javadoc

gradle的java library插件有一个javadoc task，可以为java项目生成文档。它支持标准的javadoc，也支持其他类型的文档，比如说Asciidoc，我们看一个生成Asciidoc的例子：

```java
configurations {
    asciidoclet
}

dependencies {
    asciidoclet 'org.asciidoctor:asciidoclet:1.+'
}

task configureJavadoc {
    doLast {
        javadoc {
            options.doclet = 'org.asciidoctor.Asciidoclet'
            options.docletpath = configurations.asciidoclet.files.toList()
        }
    }
}

javadoc {
    dependsOn configureJavadoc
}
```

## 7.使用gradle插件发布项目到nexus中央仓库

### 7.1.Gradle Nexus Publish Plugin历史

今天要给大家介绍的gradle插件名字叫做Gradle Nexus Publish Plugin，最近才发布了1.0.0版本，有小伙伴可能要问了，gradle出来这么久了，最近才有这样的插件吗？

其实不然，我们来讲一下gradle Nexus发布插件的历史。

2015年，Marcin Zajączkowski创建了gradle-nexus-staging-plugin，该插件可在Nexus存储库管理器中关闭和释放staging存储库。使用这个插件就可以直接从代码中将Gradle项目发布到Maven Central仓库。多年来，它已经在全球各地被多个项目所采用。

但是这个插件存在一个小问题: 由于Gradle发布过程中的技术限制，因此需要使用启发式技术来跟踪隐式创建的staging存储库，对于给定状态的多个存储库，通常会发布失败。尤其是在持续集成服务Travis CI在2019年末更改其网络架构之后，这个插件问题就更多了。

基于这个问题，马克·菲利普（Marc Philipp）创建了另外一个插件Nexus Publish Plugin，该插件丰富了Gradle中的发布机制，可以显式创建staging存储库并直接向其发布（上传）组件。

通常我们需要将这两个插件一起使用，但是，一个功能需要使用到两个插件还是会让用户感到困惑。所以Gradle Nexus Publish Plugin在2020/2021年应运而生了，它的目的就是合并上面两个插件的功能。

### 7.2.插件的使用

在gradle中使用该插件很简单，首先需要引入这个插件：

```
plugins {
    id("io.github.gradle-nexus.publish-plugin") version "«version»"
}
```

> 注意，这个插件必须在 Gradle 5.0 或者之后的版本使用，并且在根项目中引入。

接下来，我们需要定义要发布的仓库，如果是通过Sonatype's OSSRH Nexus发布到Maven的中央仓库，那么需要添加sonatype()，如下所示：

```
nexusPublishing {
    repositories {
        sonatype()
    }
}
```

在sonatype()中，实际上定义了 `nexusUrl` 和 `snapshotRepositoryUrl`。

发布到中央仓库是需要用户名密码的，我们需要设置 `sonatypeUsername` 和 `sonatypePassword` 这两个项目的属性。一种方法是在`~/.gradle/gradle.properties` 中进行配置，或者设置 `ORG_GRADLE_PROJECT_sonatypeUsername` 和 `ORG_GRADLE_PROJECT_sonatypePassword` 这两个环境变量。

或者，可以直接在sonatype 中进行定义：

```
nexusPublishing {
    repositories {
        sonatype {
            username = "your-username"
            password = "your-password"
        }
    }
}
```

最后，调用publishToSonatype和 closeAndReleaseSonatypeStagingRepository就可以分别发布到Sonatype和关闭并发布到中央仓库了。

> 注意，上面的closeAndReleaseSonatypeStagingRepository实际上是包含了两步操作：close和release。我们也可以仅仅调用closeSonatypeStagingRepository，然后手动登录Nexus UI，进行release操作。

下面是两个分别使用groovy和Kotlin的具体的例子：

#### 7.2.1.Groovy DSL

```
plugins {
    id "java-library"
    id "maven-publish"
    id "io.github.gradle-nexus.publish-plugin" version "«version»"
}

publishing {
    publications {
        mavenJava(MavenPublication) {
            from(components.java)
        }
    }
}

nexusPublishing {
    repositories {
        myNexus {
            nexusUrl = uri("https://your-server.com/staging")
            snapshotRepositoryUrl = uri("https://your-server.com/snapshots")
            username = "your-username" // defaults to project.properties["myNexusUsername"]
            password = "your-password" // defaults to project.properties["myNexusPassword"]
        }
    }
}
```

#### 7.2.2.Kotlin DSL

```
plugins {
    `java-library`
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "«version»"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

nexusPublishing {
    repositories {
        create("myNexus") {
            nexusUrl.set(uri("https://your-server.com/staging"))
            snapshotRepositoryUrl.set(uri("https://your-server.com/snapshots"))
            username.set("your-username") // defaults to project.properties["myNexusUsername"]
            password.set("your-password") // defaults to project.properties["myNexusPassword"]
        }
    }
}
```

默认情况下nexusPublishing中的connectTimeout和clientTimeout是5分钟，可以根据自己的需要进行调整。

### 7.3.插件背后的故事

我们来看一下这个插件背后是怎么工作的。

首先定义的`nexusPublishing { repositories { ... } }` 会拦截所有子项目的 `maven-publish` 插件，用来修改发布地址。

如果项目的版本号不是以`-SNAPSHOT`结尾，这说明是发布版本，那么会创建一个`initialize${repository.name.capitalize()}StagingRepository` 任务，开启一个新的staging仓库，并且设置好对应的URL。在多项目构建中，所有拥有相同nexusUrl 的子项目，将会使用同样的staging仓库。

`initialize${repository.name.capitalize()}StagingRepository`为每个配置好的仓库地址，生成发布任务。

为每个发布任务生成一个 `publishTo${repository.name.capitalize()}` 生命周期task。

在发布任务之后分别创建 `close${repository.name.capitalize()}StagingRepository` 和 `release${repository.name.capitalize()}StagingRepository` 任务。



## X.常见问题

Gradle 3.4 引入了新的依赖配置，新增了 `api` 和 `implementation` 来代替 `compile` 依赖配置。其中 `api` 和以前的 `compile` 依赖配置是一样的。使用 `implementation` 依赖配置，会显著提升构建时间。

接下来，我们举例说明 `api` 和 `implementation` 的区别。

假如我们一个名 MyLibrary 的 module 类库和一个名为 InternalLibrary 的 module 类库。里面的代码类似这样：

```cpp
//internal library module
public class InternalLibrary {
    public static String giveMeAString(){
        return "hello";
    }
}

//my library module
public class MyLibrary {
    public String myString(){
        return InternalLibrary.giveMeAString();
    }
}
```

MyLibrary 中 build.gradle 对 InternalLibrary 的依赖如下：

```bash
dependencies {
    api project(':InternalLibrary')
}
```

然后在主 module 的 build.gradle 添加对 MyLibrary 的依赖：

```bash
dependencies {
    api project(':MyLibrary')
}
```

在主 module 中，使用 `api` 依赖配置 MyLibrary 和 InternalLibrary 都可以访问：

```csharp
//so you can access the library (as it should)
MyLibrary myLib = new MyLibrary();
System.out.println(myLib.myString());

//but you can access the internal library too (and you shouldn't)
System.out.println(InternalLibrary.giveMeAString());
```

使用这种方法，会泄露一些不应该被使用的实现。

为了阻止这种情况，Gradle 新增了 `implementation` 配置。如果我们在 MyLibrary 中使用 `implementation` 配置：

```bash
dependencies {
    implementation project(':InternalLibrary')
}
```

然后在主 module 的 build.gradle 文件中使用 `implementation` 添加对 MyLibrary 的依赖：

```bash
dependencies {
    implementation project(':MyLibrary')
}
```

使用这个 `implementation` 依赖配置在应用中无法调用 `InternalLibrary.giveMeAString()`。如果 MyLibrary 使用 `api` 依赖 InternalLibrary，无论主 module 使用 `api` 还是 `implementation` 依赖配置，主 module 中都可以访问 `InternalLibrary.giveMeAString()`。

使用这种封箱策略，如果你只修改了 InternalLibrary 中的代码，Gradle 只会重新编译 MyLibrary，它不会触发重新编译整个应用，因为你无法访问 InternalLibrary。当你有大量的嵌套依赖时，这个机制会显著提升构建速度。

其它配置说明如下表所示。

| 新配置      | 废弃配置 | 说明                                                         |
| ----------- | -------- | ------------------------------------------------------------ |
| compileOnly | provided | gradle 添加依赖到编译路径，编译时使用。（不会打包到APK）     |
| runtimeOnly | apk      | gradle 添加依赖只打包到 APK，运行时使用。（不会添加到编译路径） |

**总结**

- 当你切换到新的 Android gradle plugin 3.x.x，你应用使用 `implementation` 替换所有的 `compile` 依赖配置。然后尝试编译和测试你的应用。如果没问题那样最好，如果有问题说明你的依赖或使用的代码现在是私有的或不可访问——来自 Android Gradle plugin engineer Jerome Dochez 的建议。
- 如果你是一个lib库的维护者，对于所有需要公开的 API 你应该使用 `api` 依赖配置，测试依赖或不让最终用户使用的依赖使用 `implementation` 依赖配置。









































