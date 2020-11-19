# Java BIO 编程

## I/O 模型

### I/O 模型基本说明

1. I/O 模型简单的理解：就是用什么样的通道进行数据的发送和接收，这很大程度上决定了程序通信的性能
2. Java 共支持3种网络编程模型 I/O 模式：BIO、NIO、AIO



### Java BIO

同步并阻塞（传统阻塞型），服务器实现模式为一个连接一个线程，即客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事情会造成不必要的线程开销。

![BIO 简单示意图]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/2.png )

### Java NIO

同步非阻塞，服务器实现模式为一个线程处理多个请求（连接），即客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接有 I/O 请求就进行处理。

![NIO 简单示意图]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/3.png )

### Java AIO

Java AIO(NIO 2.0)：异步非阻塞，AIO 引入异步通道的概念，采用了 Proactor 模式，简化了程序编写，有效的请求才启动线程，它的特点是先由操作系统完成后才通知服务器端程序启动线程去处理，一般适用于连接数较多且连接时间较长的应用



## BIO、NIO、AIO 适用场景分析

1. BIO 方式适用于连接数目比较少且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，JDK 1.4以前的唯一选择，但程序简单易理解。

2. NIO 方式适用于连接数目多且连接比短（轻操作）的架构，比如聊天服务器，弹幕系统，服务器间通讯等。编程比较复杂，JDK 1.4 开始支持。
3. AIO 方式适用于连接数目多且连接比较长（重操作）的架构，比如相册服务器，充分调用OS参与并发操作，编程比较复杂，JDK 7 开始支持



## Java BIO 基本介绍













