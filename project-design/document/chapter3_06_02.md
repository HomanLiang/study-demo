[toc]



# 实用的jar包加密方案

### 前言

jar包相信大家都很熟悉，是通过打包java工程而获得的产物，但是jar包是有一个致命的缺点的，那就是很容易被反编译，只需要使用jd-gui就可以很容易的获取到java源码。

如果你想要防止别人反编译做逆向工程，那么对jar包进行一次加密就是一项很重要的工作了。

如何对jar包进行加密呢？其实没有想象中的那么困难，有一款开源工具已经提供了较为完善的加密方案，这款开源工具的名字叫做xjar。

接下来我们就看一下使用xjar工具给jar包加密有多么的容易。

### 基础环境准备

现在假设你的项目是一个maven项目（目前不使用maven的项目已经不多了），那么加密起来特别的容易，

首先就是要在你的pom文件中增加插件仓库地址，如下：

```
    <pluginRepositories>
        <pluginRepository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </pluginRepository>
    </pluginRepositories>
```

然后在pom文件中增加如下插件：

```
<plugin>
    <groupId>com.github.core-lib</groupId>
    <artifactId>xjar-maven-plugin</artifactId>
    <version>4.0.1</version>
    <executions>
        <execution>
            <goals>
                <goal>build</goal>
            </goals>
            <phase>install</phase>

            <configuration>
                <includes>
                    <include>/com/huc/**/*.class</include>
                    <include>/mapper/**/*Mapper.xml</include>
                    <include>/*.yml</include>
                </includes>
                <excludes>
                    <exclude>/templates/**.*</exclude>
                    <exclude>/static/**.*</exclude>
                </excludes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

这样，我们的准备工作就做完了，需要注意的是，includes代表我们想要加密的内容，采用的是Ant表达式，excludes代表的是不需要加密的内容，同样使用的Ant表达式。

一般情况下我们建议这两处内容必填，如果不填写，会加密整个jar包中的所有文件，这样其实是没有必要的，而且全部加密后启动的时候也可能产生意料之外的错误。

另外要说明的是，加密后的jar包是需要通过golang环境运行的，所以我们需要提前把golang的运行环境安装好，安装过程请自行百度。

### 开始加密

现在我们就开始正式的加密工作了，加密过程非常简单，只需要使用maven的install命令即可自动打包，命令如下：

```
mvn clean install -Dxjar.password=password  -Dmaven.test.skip=true
```

这里的password可以自行指定密码，是必填项。

执行后就会得到两个文件：一个是xjar.go的go源文件，一个是你项目的xjar包，也就是加密后的jar包。

### 运行加密后的jar包

运行加密后的jar包是需要先编译xjar.go源文件生成jar包启动器的。编译方式如下：

```
go build ./xjar.go
```

编译后会生成xjar.exe启动器（王子使用的是window系统，如果是linux系统就不是exe后缀了）。

之后使用如下命令即可运行加密后的jar包：

```
./xjar.exe java -jar ./**.xjar
```

可以看出，只是在使用java -jar的前边加上启动器即可，还是很方便的。