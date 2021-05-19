[toc]



# Java BIO 编程

## 1.I/O 模型

### 1.1.I/O 模型基本说明

1. I/O 模型简单的理解：就是用什么样的通道进行数据的发送和接收，这很大程度上决定了程序通信的性能
2. Java 共支持3种网络编程模型 I/O 模式：BIO、NIO、AIO



### 1.2.Java BIO

同步并阻塞（传统阻塞型），服务器实现模式为一个连接一个线程，即客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事情会造成不必要的线程开销。

![BIO 简单示意图]( https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/2.png )

### 1.3.Java NIO

同步非阻塞，服务器实现模式为一个线程处理多个请求（连接），即客户端发送的连接请求都会注册到多路复用器上，多路复用器轮询到连接有 I/O 请求就进行处理。

![NIO 简单示意图]( https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/3.png )

### 1.4.Java AIO

Java AIO(NIO 2.0)：异步非阻塞，AIO 引入异步通道的概念，采用了 Proactor 模式，简化了程序编写，有效的请求才启动线程，它的特点是先由操作系统完成后才通知服务器端程序启动线程去处理，一般适用于连接数较多且连接时间较长的应用



## 2.BIO、NIO、AIO 适用场景分析

1. BIO 方式适用于连接数目比较少且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，JDK 1.4以前的唯一选择，但程序简单易理解。

2. NIO 方式适用于连接数目多且连接比短（轻操作）的架构，比如聊天服务器，弹幕系统，服务器间通讯等。编程比较复杂，JDK 1.4 开始支持。
3. AIO 方式适用于连接数目多且连接比较长（重操作）的架构，比如相册服务器，充分调用OS参与并发操作，编程比较复杂，JDK 7 开始支持



## 3.Java BIO 基本介绍

1. Java BIO 就是传统的 `java.io` 编程，其相关的类和接口在 java.io
2. BIO(blocking I/O)：同步阻塞，服务器实现模式为一个连接一个线程，即客户端有连接请求时服务器端就需要启动一个线程进行处理，如果这个连接不做任何事情会造成不必要的线程开销，可以通过线程池机制改善（实现多个客户连接服务器）

3. BIO 方式适用于连接数目比较小且固定的架构，这种方式对服务器资源要求比较高，并发局限于应用中，JDK 1.4 以前的唯一选择，程序简单易理解



## 4.Java BIO 工作机制

对 BIO 编程流程的梳理

1. 服务器端启动一个 ServerSocket
2. 客户端启动 Socket 对服务器进行通讯，默认情况下服务器端需要对每个客户建立一个线程与之通讯
3. 客户端发送请求后，先咨询服务器是否有线程响应，如果没有则会等待，或者被拒绝
4. 如果有响应，客户端线程会等待请求结束后，再继续执行



## 5.Java BIO 应用实例

实例说明：

1. 使用 BIO 模型编写一个服务器端，监听 6666 端口，当有客户端连接时，就启动一个线程与之通讯。

2. 要求使用线程池机制改善，可以连接多个客户端

3. 服务器端可以接收客户端发送的数据（telnet 方式即可）

4. 代码演示：

   ```
   package com.homan.bio;
   
   import java.io.InputStream;
   import java.net.ServerSocket;
   import java.net.Socket;
   import java.util.concurrent.ExecutorService;
   import java.util.concurrent.Executors;
   
   /**
    * BIO 服务器例子
    *
    * @author Homan Liang
    * @date 2020/11/18
    */
   public class BIOServer {
       /**
        * 主方法
        * @param args
        * @throws Exception
        */
       public static void main(String[] args) throws Exception {
           // 线程池机制
           // 思路
           // 1. 创建一个线程池
           // 2. 如果有客户端连接，就创建一个线程，与之通讯(单独写一个方法)
           ExecutorService newCachedThreadPool = Executors.newCachedThreadPool();
           // 创建ServerSocket
           ServerSocket serverSocket = new ServerSocket(6666);
           System.out.println("服务器启动了");
   
           while (true) {
               System.out.println("线程信息 id =" + Thread.currentThread().getId() + " 名字=" + Thread.currentThread().getName());
               // 监听，等待客户端连接
               System.out.println("等待连接....");
               final Socket socket = serverSocket.accept();
               System.out.println("连接到一个客户端");
               // 就创建一个线程，与之通讯(单独写一个方法)
               newCachedThreadPool.execute(new Runnable() {
                   // 我们重写
                   public void run() {
                       // 可以和客户端通讯
                       handler(socket);
                   }
               });
           }
       }
   
       /**
        * 编写一个handler方法，和客户端通讯
        * @param socket
        */
       public static void handler(Socket socket) {
           try {
               System.out.println("线程信息 id =" + Thread.currentThread().getId() + " 名字=" + Thread.currentThread().getName());
               byte[] bytes = new byte[1024];
               // 通过socket 获取输入流
               InputStream inputStream = socket.getInputStream();
   
               // 循环的读取客户端发送的数据
               while (true) {
                   System.out.println("线程信息 id =" + Thread.currentThread().getId() + " 名字=" + Thread.currentThread().getName());
                   System.out.println("read....");
                   int read = inputStream.read(bytes);
                   if (read != -1) {
                       // 输出客户端发送的数据
                       System.out.println(new String(bytes, 0, read));
                   } else {
                       break;
                   }
               }
           } catch (Exception e) {
               e.printStackTrace();
           } finally {
               System.out.println("关闭和client的连接");
               try {
                   socket.close();
               } catch (Exception e) {
                   e.printStackTrace();
               }
           }
       }
   }
   
   ```

   ## 6.Java BIO 问题分析

   1. 每个请求都需要创建独立的线程，与对应的客户端进行数据 Read，业务处理，数据 Write
   2. 当并发数较大时，需要创建大量线程来处理连接，系统资源占用较大
   3. 连接建立后，如果当前线程暂时没有数据可读，则线程就阻塞在 Read 操作上，造成线程资源浪费







