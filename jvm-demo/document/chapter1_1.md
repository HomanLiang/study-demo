[toc]



# JVM 与 Java 体系结构

## 1.Java 及 JVM 简介

### 1.1.Java : 跨平台的语言

![image-20201228102504083](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228102504083.png)

### 1.2.JVM ： 跨语言的平台

![image-20201228102536668](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228102536668.png)

- 随着 Java 7 的正式发布，Java 虚拟机的设计者们通过 JSR-292 规范 基本实现在 Java 虚拟机平台上运行非 Java 语言编写的程序。
- Java 虚拟机根本不关心运行在其内部的程序到底是使用何种编程语言编写的，它只关心“字节码”文件。也就是说 Java 虚拟机拥有语言无关性，并不会单纯地与 Java 语言“终身绑定”，只要其他编程语言的编译结果满足并包含 Java 虚拟机的内部指令集、符号表以及其他的辅助信息，它就是一个有效的字节码文件，就能够被虚拟机所识别并装载运行。



### 1.3.字节码

- 我们平时说的 Java 字节码，指的是用 Java 语言编译成的字节码。准确的说任何能在 JVM 平台上执行的字节码格式都是一样的。所以统称为：JVM 字节码
- 不同的编译器，可以编译出相同的字节码文件，字节码文件也可以在不同的 JVM 上运行。
- Java 虚拟机与 Java 语言并没有必然的联系，它只与特定的二进制文件格式--Class 文件格式所关联，Class 文件中包含了 Java 虚拟机指令集（或者称为字节码，Bytecodes）和符号表，还有一些其他辅助信息。



### 1.4.多语言混合编程

- Java 平台上的多语言混合编程正成为主流，通过特定领域的语言去解决特地领域的问题是当前软件开发应对日趋复杂的项目需求的一个方向。
- 试想一下，在一个项目之中，并行处理用 Clojure 语言编写，展示层使用 JRuby/Rails，中间层则是 Java，每个应用层都将使用不同的编程语言来完成，而且接口对每一层的开发者都是透明的，各种语言之间的交互不存在任何困难，就像使用自己语言的原生 API 一样方便，因为它们最终都运行在一个虚拟机之上。
- 对这些运行于 Java 虚拟机之上、Java 之外的语言，来自系统级的、底层的支持正在迅速增强，以 JSR-292 为核心的一系列项目和功能改进（如 DaCinci machine 项目、Nashorn 引擎、InvokeDynamic指令、java.lang.invoke 包等），推动 Java 虚拟机从“Java 语言的虚拟机”，向 “多语言虚拟机”的方向发展。



### 1.5.Java 发展的重大事件

![image-20201228110955645](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228110955645.png)

![image-20201228111010164](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228111010164.png)



### 1.6.Open JDK 和 Oracle JDK 

![image-20201228111052463](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228111052463.png)

在 JDK 11 之前，Oracle JDK 中还会存在一些 Open JDK 中没有的、闭源的功能。但在 JDK 11 中，我们可以认为 Open JDK 和 Oracle JDK 代码实质上已经完全一直的程度。



## 2.虚拟机与 Java 虚拟机

### 虚拟机

- 所谓虚拟机（Virtual Machine），就是一台虚拟的计算机。它是一款软件，用来执行一系列虚拟计算机指令。大体上，虚拟机可以分为系统虚拟机和程序虚拟机
  - Visual Box、VMware 就是属于系统虚拟机，它们完全是对物理计算机的仿真，提供了一个可运行完整操作系统的软件平台
  - 程序虚拟机的典型就是 Java 虚拟机，它专门为执行单个计算机程序而设计，在 Java 虚拟机中执行的指令我们称为 Java 字节码指令。
- 无论是系统虚拟机还是程序虚拟机，在上面运行的软件都被限制于虚拟机提供的资源中。



### Java 虚拟机

- Java 虚拟机是一台执行 Java 字节码的虚拟计算机，它拥有独立的运行机制，其运行的 Java 字节码也未必由 Java 语言编译而成。
- JVM 平台的各种语言可以共享 Java 虚拟机带来的跨平台、优秀的垃圾回收器、以及可靠的即时编译器。
- Java 技术的核心就是 Java 虚拟机（JVM，Java Virtual Machine），因为所有的 Java 程序都运行在 Java 虚拟机内部。

- 作用：
  - Java 虚拟机就是二进制字节码的运行环境，负责装载字节码到其内部，解释、编译为对应平台上的机器指令执行。每一条 Java 指令，Java 虚拟机规范中都有详细定义，如怎么取操作数、怎么处理操作数、处理结果放在哪里。

- 特点：
  - 一次编译，到处运行
  - 自动内存管理
  - 自动垃圾回收功能



### JVM 的位置

![image-20201228134338261](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228134338261.png)

![image-20201228134410021](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228134410021.png)



## 3.JVM 的整体结构

- HotSpot VM 是目前市面上高性能虚拟机的代表作之一。
- 它采用解释器与即时编译器并存的架构
- 在今天，Java 程序的运行性能早已脱胎换骨，已经达到了可以和 C/C++ 程序一较高下的地步。

![image-20201228134500435](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228134500435.png)



## 4.Java 代码执行流程

![image-20201228134838539](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201228134838539.png)



## 5.JVM的架构模型

Java 编译器输入的指令流基本上是一种基于栈的指令集架构，另外一种指令集架构则是基于寄存器的指令集架构。

具体来说，这两种架构之间的区别：

- 基于栈式架构的特点：
  - 设计和实现更简单，适用于资源受限的系统
  - 避开了寄存器的分配难题：使用零地址指令方式分配
  - 指令流中的指令大部分是零地址指令，其执行过程依赖于操作栈。指令集更小，编译器容易实现
  - 不需要硬件支持，可移植性更好，更好实现跨平台
- 基于寄存器架构的特点
  - 典型的应用是 x86 的二进制指令集：比如传统的 PC 以及 Android 的 Davlik 虚拟机。
  - 指令集架构则完全依赖硬件，可移植性差
  - 性能优秀和执行更高效
  - 花费更少的指令去完成一项操作
  - 在大部分情况下，基于寄存器架构的指令集往往都以一地址指令、二地址指令和三地址指令为主，而基于栈式架构的指令集却是以零地址指令为主。



**举例1：**

同样执行`2 + 3`这种逻辑操作，其指令分别如下：

**基于栈的计算流程（以 Java 虚拟机为例）：**

![image-20201229192919904](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201229192919904.png)

**而基于寄存器的计算流程：**

![image-20201229192949796](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201229192949796.png)



**举例2：**

![image-20201230153126721](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230153126721.png)

![image-20201230153141476](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230153141476.png)

**总结**

- 由于跨平台性的设计，Java 的指令都是根据栈来设计的。不同平台 CPU 架构不同，所以不能设计为基于寄存器的。优点是跨平台，指令集少，编译器容易实现，缺点是性能下降，实现同样的功能需要更多的指令。
- 时至今日，尽管嵌入式平台已经不是 Java 程序的主流运行平台了（准确来说应该是 HotSpot VM 的宿主环境已经不局限于嵌入式平台了），那么为什么不将架构更换为寄存器的架构呢？



## 6.JVM 的生命周期

### 6.1.虚拟机的启动

Java 虚拟机的启动是通过引导类加载器（bootstrap class loader）创建一个初始化（initial class）来完成，这个类是由虚拟机的具体实现指定的。

### 6.2.虚拟机的执行

- 一个运行中的 Java 虚拟机有着一个清晰的任务：执行 Java 程序。
- 程序开始执行时他才运行，程序结束时他就停止。
- 执行一个所谓的 Java 程序的时候，真真正正在执行的时一个叫做 Java 虚拟机的进程

### 6.3.虚拟机的退出

有如下的几种情况：

- 程序正常执行结束
- 程序在执行过程中遇到了异常或错误而异常终止
- 由于操作系统出现错误而导致 Java 虚拟机进程终止
- 某线程调用 Runtime 类或 System 类的 exit 方法，或 Runtime 类的 halt 方法，并且 Java 安全管理器也允许这次 exit 或 halt 操作。
- 除此之外，JNI（Java Native Interface）规范描述了用 JNI Invocation API 来加载或卸载 Java 虚拟机时，Java 虚拟机的退出情况。



## 7.JVM 发展历程

### 7.1.Sun Classic VM

![image-20201230172028963](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172028963.png)

### 7.2.Exact VM

![image-20201230172044578](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172044578.png)

### 7.3.HotSpot VM

![image-20201230172100844](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172100844.png)

### 7.4.JRockit

![image-20201230172115025](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172115025.png)

### 7.5.J9

![image-20201230172131287](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172131287.png)

### 7.6.KVM 和 CDC/CLDC Hotspot

![image-20201230172144440](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172144440.png)

### 7.7.Azul VM

![image-20201230172156042](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172156042.png)

### 7.8.Liquid VM

![image-20201230172205826](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172205826.png)

### 7.9.Harmony

![image-20201230172216071](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172216071.png)

### 7.10.Microsoft JVM

![image-20201230172226753](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172226753.png)

### 7.11.Taobao JVM

![image-20201230172235476](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20201230172235476.png)





































































