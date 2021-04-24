[toc]



# Gradle

## 1.Gradle 简介

### 1.1.简介

gradle的最新版本是6.7，从2009年的第一个版本，到2020年的6.7，已经发展了11年了。gradle是作为android的官方构建工具引入的，除了java，它还能够支持多种语言的构建，所以用途非常广泛。

gradle是开源的构建工具，你可以使用groovy或者kotlin来编写gradle的脚本，所以说gradle是一个非常强大的，高度定制化的和非常快速的构建工具。

根据我的了解，虽然gradle非常强大，但是对于java程序员来说，一般还是都使用的maven，或者同时提供maven和gradle两种构建方式。

为什么会这样呢？个人觉得有两个原因：

- 第一个原因是gradle安装文件和依赖包的网络环境，如果单单依靠国内的网络环境的话，非常难安装完成。

- 第二个原因就是gradle中需要自己编写构建脚本，相对于纯配置的脚本来说，比较复杂。

### 1.2.安装gradle和解决gradle安装的问题

gradle需要java8的支持，所以，你首先需要安装好JDK8或者以上的版本。

```sh
❯ java -version
java version "1.8.0_151"
Java(TM) SE Runtime Environment (build 1.8.0_151-b12)
Java HotSpot(TM) 64-Bit Server VM (build 25.151-b12, mixed mode)
```

安装gradle有两种方式，一种就是最简单的从官网上面下载安装包。然后解压在某个目录，最后将PATH指向该目录下的bin即可：

```sh
❯ mkdir /opt/gradle
❯ unzip -d /opt/gradle gradle-6.7-bin.zip
❯ ls /opt/gradle/gradle-6.7
LICENSE  NOTICE  bin  README  init.d  lib  media

export PATH=$PATH:/opt/gradle/gradle-6.7/bin
```

如果你想使用包管理器，比如MAC上面的brew来进行管理的话，则可以这样安装：

```sh
brew install gradle
```

但是这样安装很有可能在下载gradle安装包的时候卡住。

```sh
==> Downloading https://services.gradle.org/distributions/gradle-6.4.1-bin.zip
##O#- #
```

怎么办呢？

这时候我们需要自行下载gradle-6.4.1-bin.zip安装包，然后将其放入http服务器中，让这个压缩包可以通过http协议来访问。

简单点的做法就是将这个zip文件拷贝到IDEA中，利用IDEA本地服务器的预览功能，获得zip的http路径，比如：http://localhost:63345/gradle/gradle-6.7-all.zip.

接下来就是最精彩的部分了，我们需要修改gradle.rb文件：

```sh
brew edit gradle
```

使用上面的命令可以修改gracle.rb文件，我们替换掉下面的一段：

```sh
  homepage "https://www.gradle.org/"
  url "https://services.gradle.org/distributions/gradle-6.7-all.zip"
  sha256 "0080de8491f0918e4f529a6db6820fa0b9e818ee2386117f4394f95feb1d5583"
```

url替换成为http://localhost:63345/gradle/gradle-6.7-all.zip，而sha256可以使用 sha256sum gradle-6.7-all.zip这个命令来获取。

替换之后，重新执行brew install gradle即可安装完成。

安装完毕之后，我们使用gradle -v命令可以验证是否安装成功：

```sh
gradle -v

Welcome to Gradle 6.7!
```

### 1.3.Gradle特性

gradle作为一种新的构建工具，因为它是依赖于groovy和kotlin脚本的，基于脚本的灵活性，我们通过自定义脚本基本上可以做任何想要的构建工作。

虽然说gradle可以做任何构建工作，但是gradle现在还是有一定的限制，那就是项目的依赖项目前只支持于maven和Ivy兼容的存储库以及文件系统。

gradle通过各种预定义的插件，可以轻松的构建通用类型的项目，并且支持自定义的插件类型。

另外一个非常重要的特性是gradle是以任务为基础的，每一个build都包含了一系列的task，这些task又有各自的依赖关系，然后这些task一起构成了一个有向无环图Directed Acyclic Graphs (DAGs)。

有了这个DAG，gradle就可以决定各个task的顺序，并执行他们。

我们看两个task DAG的例子，一个是通用的task，一个是专门的编译java的例子：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424150730.png)

task可以依赖task，我们看个例子：

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

一个task可以包含Actions，inputs和Outputs。根据需要这些类型可以自由组合。

#### 1.3.1.标准task

Gradle包含了下面7种标准的task：

- clean ：用来删除build目录和里面的一切。
- check：这是一个生命周期任务，通常做一些验证工作，比如执行测试任务等。
- assemble ：这是一个生命周期任务，用来生成可分发的文件，比如jar包。
- build： 也是一个生命周期任务，用来执行测试任务和生成最后的production文件。通常我们不在build中直接做任何特定的任务操作，它一般是其他任务的组合。
- buildConfiguration： 组装configuration中指定的archives。
- uploadConfiguration： 除了执行buildConfiguration之外，还会执行上传工作。
- cleanTask： 删除特定的某个task的执行结果。

#### 1.3.2.Build phases

一个gradle的build包含了三个phases：

- Initialization： 初始化阶段。gradle支持一个或者多个project的build。在初始化阶段，gradle将会判断到底有哪些project将会执行，并且为他们分别创建一个project实例。
- Configuration： 配置阶段。gradle将会执行build脚本，然后分析出要运行的tasks。
- Execution： 执行阶段。gradle将会执行configuration阶段分析出来的tasks。

### 1.4.Gradle Wrapper

上面讲的是gradle的手动安装，如果是在多人工作的环境中使用了gradle，有没有什么办法可以不用手动安装gradle就可以自动运行gradle程序呢？

方法当然有，那就是gradle wrapper:

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424150842.png)

gradle wrapper是一个工具，通过它我们可以方便的对本地的gradle进行管理。

上图列出了gradle wrapper的工作流程，第一步是去下载gradle的安装文件，第二步是将这个安装文件解压到gradle的用户空间，第三步就是使用这个解压出来的gradle了。

我们先看下怎么创建gradle wrapper：

虽然Gradle wrapper的作用是帮我们下载和安装gradle，但是要生成gradle wrapper需要使用gradle命令才行。也就是说有了wrapper你可以按照成功gradle，有了gradle你才可以生成gradle wrapper。

假如我们已经手动按照好了gradle，那么可以执行下面的命令来生成gradle wrapper：

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

gradle/wrapper/gradle-wrapper.properties 是 wrapper的配置文件，我们看下里面的内容：

```sh
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-6.7-all.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

其中distributionUrl就是我们要下载的gradle的路径，其他的配置是gradle的安装目录。

一般来说有两种安装文件类型：bin和all。bin和all的区别在于，bin只有安装文件，而all还包含了gradle的文档和样例代码。

我们可以通过--distribution-type参数来修改安装文件的类型。此外还有 --gradle-version ，--gradle-distribution-url和--gradle-distribution-sha256-sum 这几个参数可以使用。

```sh
$ gradle wrapper --gradle-version 6.7 --distribution-type all
> Task :wrapper

BUILD SUCCESSFUL in 0s
1 actionable task: 1 executed
```

除了配置文件之外，我们还有3个文件：

- gradle-wrapper.jar： wrapper业务逻辑的实现文件。
- gradlew, gradlew.bat ：使用wrapper执行build的执行文件。也就是说我们可以使用wrapper来执行gradle的build任务。

#### 1.4.1.wrapper的使用

我们可以这样使用gradlew,来执行build：

```sh
gradlew.bat build
```

> 注意，如果你是第一次在项目中执行build命令的话，将会自动为你下载和安装gradle。

#### 1.4.2.wrapper的升级

如果我们想要升级gradle的版本，也很简单：

```sh
./gradlew wrapper --gradle-version 6.7
```

或者直接修改 gradle-wrapper.properties 也可以。

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

上面我们需要安装一个application plugin，使用的是jcenter的依赖仓库，还指定了几个具体的依赖项。最后，指明了我们应用程序的mainClass。

### 1.6.gradle使用maven仓库

build.gradle中的repositories指明的是使用的仓库选项。

默认情况下gradle有自己的本地仓库,一般在~/.gradle目录下面，如果我们之前用的是maven仓库，那么在本地的maven仓库中已经存在了很多依赖包了，如何重用呢？

我们可以这样修改repositories：

```java
    mavenLocal()
    mavenCentral()
```

这样的话, 就会优先从maven的仓库中查找所需的jar包。

## 2.深入了解gradle和maven的区别

### 2.1.gradle和maven的比较

虽然gradle和maven都可以作为java程序的构建工具。但是两者还是有很大的不同之处的。我们可以从下面几个方面来进行分析。

#### 2.1.1.可扩展性

Google选择gradle作为android的构建工具不是没有理由的，其中一个非常重要的原因就是因为gradle够灵活。一方面是因为gradle使用的是groovy或者kotlin语言作为脚本的编写语言，这样极大的提高了脚本的灵活性，但是其本质上的原因是gradle的基础架构能够支持这种灵活性。

你可以使用gradle来构建native的C/C++程序，甚至扩展到任何语言的构建。

相对而言，maven的灵活性就差一些，并且自定义起来也比较麻烦，但是maven的项目比较容易看懂，并且上手简单。

所以如果你的项目没有太多自定义构建需求的话还是推荐使用maven，但是如果有自定义的构建需求，那么还是投入gradle的怀抱吧。

#### 2.1.2.性能比较

虽然现在大家的机子性能都比较强劲，好像在做项目构建的时候性能的优势并不是那么的迫切，但是对于大型项目来说，一次构建可能会需要很长的时间，尤其对于自动化构建和CI的环境来说，当然希望这个构建是越快越好。

Gradle和Maven都支持并行的项目构建和依赖解析。但是gradle的三个特点让gradle可以跑的比maven快上一点：

- 增量构建

  gradle为了提升构建的效率，提出了增量构建的概念，为了实现增量构建，gradle将每一个task都分成了三部分，分别是input输入，任务本身和output输出。下图是一个典型的java编译的task。

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151623.png)

  以上图为例，input就是目标jdk的版本，源代码等，output就是编译出来的class文件。

  增量构建的原理就是监控input的变化，只有input发送变化了，才重新执行task任务，否则gradle认为可以重用之前的执行结果。

  所以在编写gradle的task的时候，需要指定task的输入和输出。

  并且要注意只有会对输出结果产生变化的才能被称为输入，如果你定义了对初始结果完全无关的变量作为输入，则这些变量的变化会导致gradle重新执行task，导致了不必要的性能的损耗。

  还要注意不确定执行结果的任务，比如说同样的输入可能会得到不同的输出结果，那么这样的任务将不能够被配置为增量构建任务。

- 构建缓存

  gradle可以重用同样input的输出作为缓存，大家可能会有疑问了，这个缓存和增量编译不是一个意思吗？

  在同一个机子上是的，但是缓存可以跨机器共享.如果你是在一个CI服务的话，build cache将会非常有用。因为developer的build可以直接从CI服务器上面拉取构建结果，非常的方便。

- Gradle守护进程

  gradle会开启一个守护进程来和各个build任务进行交互，优点就是不需要每次构建都初始化需要的组件和服务。

  同时因为守护进程是一个一直运行的进程，除了可以避免每次JVM启动的开销之外，还可以缓存项目结构，文件，task和其他的信息，从而提升运行速度。

  我们可以运行 gradle --status 来查看正在运行的daemons进程。

  从Gradle 3.0之后，daemons是默认开启的，你可以使用 org.gradle.daemon=false 来禁止daemons。

我们可以通过下面的几个图来直观的感受一下gradle和maven的性能比较：

- 使用gradle和maven构建 Apache Commons Lang 3的比较：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151736.png)

- 使用gradle和maven构建小项目（10个模块，每个模块50个源文件和50个测试文件）的比较：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151750.png)

- 使用gradle和maven构建大项目（500个模块，每个模块100个源文件和100个测试文件）的比较：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210424151803.png)

可以看到gradle性能的提升是非常明显的。

#### 2.1.3.依赖的区别

gralde和maven都可以本地缓存依赖文件，并且都支持依赖文件的并行下载。

在maven中只可以通过版本号来覆盖一个依赖项。而gradle更加灵活，你可以自定义依赖关系和替换规则，通过这些替换规则，gradle可以构建非常复杂的项目。

### 2.2.从maven迁移到gradle

因为maven出现的时间比较早，所以基本上所有的java项目都支持maven，但是并不是所有的项目都支持gradle。如果你有需要把maven项目迁移到gradle的想法，那么就一起来看看吧。

根据我们之前的介绍，大家可以发现gradle和maven从本质上来说就是不同的，gradle通过task的DAG图来组织任务，而maven则是通过attach到phases的goals来执行任务。

虽然两者的构建有很大的不同，但是得益于gradle和maven相识的各种约定规则，从maven移植到gradle并不是那么难。

要想从maven移植到gradle，首先要了解下maven的build生命周期，maven的生命周期包含了clean，compile，test，package，verify，install和deploy这几个phase。

我们需要将maven的生命周期phase转换为gradle的生命周期task。这里需要使用到gradle的Base Plugin，Java Plugin和Maven Publish Plugin。

先看下怎么引入这三个plugin：

```java
plugins {
    id 'base'
    id 'java'
    id 'maven-publish'
}
```

clean会被转换成为clean task，compile会被转换成为classes task，test会被转换成为test task，package会被转换成为assemble task，verify 会被转换成为check task，install会被转换成为 Maven Publish Plugin 中的publishToMavenLocal task，deploy 会被转换成为Maven Publish Plugin 中的publish task。

有了这些task之间的对应关系，我们就可以尝试进行maven到gradle的转换了。

#### 2.2.1.自动转换

我们除了可以使用 gradle init 命令来创建一个gradle的架子之外，还可以使用这个命令来将maven项目转换成为gradle项目，gradle init命令会去读取pom文件，并将其转换成为gradle项目。

#### 2.2.2.转换依赖

gradle和maven的依赖都包含了group ID, artifact ID 和版本号。两者本质上是一样的，只是形式不同，我们看一个转换的例子：

```xml
<dependencies>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>1.2.12</version>
    </dependency>
</dependencies>
```

上是一个maven的例子，我们看下gradle的例子怎写：

```java
dependencies {
    implementation 'log4j:log4j:1.2.12'  
}
```

可以看到gradle比maven写起来要简单很多。

注意这里的implementation实际上是由 Java Plugin 来实现的。

我们在maven的依赖中有时候还会用到scope选项，用来表示依赖的范围，我们看下这些范围该如何进行转换：

- compile：

  在gradle可以有两种配置来替换compile，我们可以使用implementation或者api。

  前者在任何使用Java Plugin的gradle中都可以使用，而api只能在使用Java Library Plugin的项目中使用。

  当然两者是有区别的，如果你是构建应用程序或者webapp，那么推荐使用implementation，如果你是在构建Java libraries，那么推荐使用api。

- runtime：

  可以替换成 runtimeOnly 。

- test：

  gradle中的test分为两种，一种是编译test项目的时候需要，那么可以使用testImplementation，一种是运行test项目的时候需要，那么可以使用testRuntimeOnly。

- provided：

  可以替换成为compileOnly。

- import：

  在maven中，import经常用在dependencyManagement中，通常用来从一个pom文件中导入依赖项，从而保证项目中依赖项目版本的一致性。

在gradle中，可以使用 platform() 或者 enforcedPlatform() 来导入pom文件：

```java
dependencies {
    implementation platform('org.springframework.boot:spring-boot-dependencies:1.5.8.RELEASE') 

    implementation 'com.google.code.gson:gson' 
    implementation 'dom4j:dom4j'
}
```

比如上面的例子中，我们导入了spring-boot-dependencies。因为这个pom中已经定义了依赖项的版本号，所以我们在后面引入gson的时候就不需要指定版本号了。

platform和enforcedPlatform的区别在于，enforcedPlatform会将导入的pom版本号覆盖其他导入的版本号：

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

gradle可以兼容使用maven或者lvy的repository。gradle没有默认的仓库地址，所以你必须手动指定一个。

你可以在gradle使用maven的仓库：

```java
repositories {
    mavenCentral()
}
```

我们还可以直接指定maven仓库的地址：

```java
repositories {
    maven {
        url "http://repo.mycompany.com/maven2"
    }
}
```

如果你想使用maven本地的仓库，则可以这样使用：

```java
repositories {
    mavenLocal()
}
```

但是mavenLocal是不推荐使用的，为什么呢？

mavenLocal只是maven在本地的一个cache，它包含的内容并不完整。比如说一个本地的maven repository module可能只包含了jar包文件，并没有包含source或者javadoc文件。那么我们将不能够在gradle中查看这个module的源代码，因为gradle会首先在maven本地的路径中查找这个module。

并且本地的repository是不可信任的，因为里面的内容可以轻易被修改，并没有任何的验证机制。

#### 2.2.4.控制依赖的版本

如果同一个项目中对同一个模块有不同版本的两个依赖的话，默认情况下Gradle会在解析完DAG之后，选择版本最高的那个依赖包。

但是这样做并不一定就是正确的， 所以我们需要自定义依赖版本的功能。

首先就是上面我们提到的使用platform()和enforcedPlatform() 来导入BOM（packaging类型是POM的）文件。

如果我们项目中依赖了某个module，而这个module又依赖了另外的module，我们叫做传递依赖。在这种情况下，如果我们希望控制传递依赖的版本，比如说将传递依赖的版本升级为一个新的版本，那么可以使用dependency constraints：

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

> 注意，dependency constraints只对传递依赖有效，如果上面的例子中commons-codec并不是传递依赖，那么将不会有任何影响。

> 同时 Dependency constraints需要Gradle Module Metadata的支持，也就是说只有你的module是发布在gradle中才支持这个特性，如果是发布在maven或者ivy中是不支持的。

上面讲的是传递依赖的版本升级。同样是传递依赖，如果本项目也需要使用到这个传递依赖的module，但是需要使用到更低的版本（因为默认gradle会使用最新的版本），就需要用到版本降级了。

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

我们可以在implementation中指定特定的version即可。

strictly表示的是强制匹配特定的版本号，除了strictly之外，还有require，表示需要的版本号大于等于给定的版本号。prefer，如果没有指定其他的版本号，那么就使用prefer这个。reject，拒绝使用这个版本。

除此之外，你还可以使用Java Platform Plugin来指定特定的platform，从而限制版本号。

最后看一下如何exclude一个依赖：

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

我们可以在gradle中做同样的事情settings.gradle：

```java
rootProject.name = 'simple-multi-module'  

include 'simple-weather', 'simple-webapp'  
```

#### 2.2.6.profile和属性

maven中可以使用profile来区别不同的环境，在gradle中，我们可以定义好不同的profile文件，然后通过脚本来加载他们：

build.gradle：

```java
if (!hasProperty('buildProfile')) ext.buildProfile = 'default'  

apply from: "profile-${buildProfile}.gradle"  

task greeting {
    doLast {
        println message  
    }
}
```

profile-default.gradle：

```java
ext.message = 'foobar'  
```

profile-test.gradle：

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

在maven中有一个process-resources阶段，可以执行resources:resources用来进行resource文件的拷贝操作。

在Gradle中的Java plugin的processResources task也可以做相同的事情。

比如我可以执行copy任务：

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

gradle是一个构建工具，所谓构建工具就是通过既定的各种规则，将原代码或者原文件通过一定的task处理过后，打包生成目标文件的步骤。

所以我们在gradle中有两个非常重要的概念，分别是项目和任务。

每一个gradle的构建任务可以包含一个或者多个项目，项目可以有多种类型，比如是一个web项目或者一个java lib项目等。为了实现project要完成的目标，需要定义一个个的task来辅助完成目标。

task主要用来执行特定的任务，比如编译class文件，打包成jar，生成javadoc等等。

### 3.2.一个例子

接下来我们使用一个具体的例子来讲解一下，gradle到底是怎么用的。

首先我们创建一个新的project目录：

```sh
$ mkdir gradle-test
$ cd gradle-test
```

gradle提供了一个init方法，来方便的创建gradle项目的骨架，我们用下看：

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

其中gradle-wrapper是帮你自动设置和安装gradle的工具，同时它还提供了gradlew和gradlew.bat这两个执行文件，用来执行gradle的任务。

我们主要看其中的两个配置文件，settings.gradle和build.gradle。

settings.gradle中配置的是gradle中要build的项目信息：

```java
rootProject.name = 'gradle-test'
include('app')
```

上面的例子中，rootProject.name指定了项目的名字，include('app')表示需要引入一个叫做app的子项目，这个子项目中包含着实际的要打包的内容。

再看一下app中的build.gradle文件：

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

有两种方式来运行，一种方式就是使用系统自带的gradle命令，一种方式就是使用刚刚gradle为你生成的gradlew。

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

你还可以带上 --scan 参数将build上传到gradle scan中，得到更加详细的构建分析：

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

上面的例子中，我们使用的都是gradle默认的tasks，并没有看到自定义task的使用，接下来我们将会探讨一下，如何在build.gradle编写自己的task。

这里我们使用的groovy来编写build.gradle，所以我们可以像运行代码一样来运行它。

#### 3.3.1.task脚本

先创建一个非常简单的task：

```java
task hello {
    doLast {
        println 'Hello www.flydean.com!'
    }
}
```

上面定义了一个名叫hello的task，并且会在执行最后输出 "Hello www.flydean.com!"。

我们这样运行：

```sh
gradle -q hello
Hello www.flydean.com!
```

-q的意思是悄悄的执行，将会忽略gradle自身的log信息。我们把要执行的task名字写在gradle后面就可以了。

如果你熟悉ant命令的话，可以看到gradle的task和ant很类似，不过更加的强大。

因为是groovy脚本，所以我们可以在其中执行代码：

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

gradle中的一个task可以依赖其他的task：

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

如果不想每次都在调用gradle命令的时候手动指定某个具体的task名字，我们可以使用defaultTasks：

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

上面的代码执行gradle和gradle clean run是相当的。

#### 3.3.5.build script的外部依赖

既然build script可以用groovy代码来编写，那么如果我们想要在build script中使用外部的jar包怎么办呢？

这个时候，我们可以将外部依赖放到buildscript()方法中，后面的task就可以使用引入的依赖了：

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

上面的例子中，encode使用了一个外部的依赖包Base64，这个依赖包是在buildscript方法中引入的。

## 4.深入理解gradle中的task

### 4.1.定义task

定义一个task可以有很多种方式，比如下面的使用string作为task的名字：

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

上面的例子中，我们使用tasks.create方法，将新创建的task加到tasks集合中。

我们还可以使用groovy特有的语法来定义一个task：

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

上面我们在创建task的时候，使用了tasks集合类来创建task。

实际上，tasks集合类是一个非常有用的工具类，我们可以使用它来做很多事情。

直接在build文件中使用tasks，实际上是引用了TaskContainer的一个实例对象。我们还可以使用 `Project.getTasks()` 来获取这个实例对象。

我们看下TaskContainer的定义：

```java
public interface TaskContainer extends TaskCollection<Task>, PolymorphicDomainObjectContainer<Task> 
```

从定义上，我们可以看出TaskContainer是一个task的集合和域对象的集合。

taskContainer中有四类非常重要的方法：

第一类是定位task的方法，有个分别是findByPath和getByPath。两个方法的区别就是findByPath如果没找到会返回null，而getByPath没找到的话会抛出UnknownTaskException。

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
2. 文件类型：文件类型就是 File 或者 FileCollection 的衍生类型，或者其他可以作为参数传递给 Project.file(java.lang.Object) 和 Project.files(java.lang.Object...) 的类型。
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

上面的例子中，我们定义了4个属性，分别是TemplateEngineType，FileCollection，TemplateData和File。前面三个属性是输入，后面一个属性是输出。

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

- @InputFile： 相当于File，表示单个input文件。
- @InputDirectory： 相当于File，表示单个input目录。
- @Classpath： 相当于Iterable，表示的是类路径上的文件，对于类路径上的文件需要考虑文件的顺序。如果类路径上的文件是jar的话，jar中的文件创建时间戳的修改，并不会影响input。
- @CompileClasspath：相当于Iterable，表示的是类路径上的java文件，会忽略类路径上的非java文件。
- @OutputFile： 相当于File，表示输出文件。
- @OutputFiles： 相当于Map<String, File> 或者 Iterable，表示输出文件。
- @OutputDirectories： 相当于Map<String, File> 或者 Iterable，表示输出文件。
- @Destroys： 相当于File 或者 Iterable，表示这个task将会删除的文件。
- @LocalState： 相当于File 或者 Iterable，表示task的本地状态。
- @Console： 表示属性不是input也不是output，但是会影响console的输出。
- @Internal： 内部属性，不是input也不是output。
- @ReplacedBy： 属性被其他的属性替换了，不能算在input和output中。
- @SkipWhenEmpty： 和@InputFiles 跟 @InputDirectory一起使用，如果相应的文件或者目录为空的话，将会跳过task的执行。
- @Incremental： 和@InputFiles 跟 @InputDirectory一起使用，用来跟踪文件的变化。
- @Optional： 忽略属性的验证。
- @PathSensitive： 表示需要考虑paths中的哪一部分作为增量的依据。

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

在sonatype()中，实际上定义了nexusUrl 和 snapshotRepositoryUrl。

发布到中央仓库是需要用户名密码的，我们需要设置sonatypeUsername 和 sonatypePassword 这两个项目的属性。一种方法是在~/.gradle/gradle.properties 中进行配置，或者设置 ORG_GRADLE_PROJECT_sonatypeUsername 和 ORG_GRADLE_PROJECT_sonatypePassword 这两个环境变量。

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







































