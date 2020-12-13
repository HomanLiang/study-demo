# **Netty** 核心源码剖析

## Netty 启动过程源码剖析

### 源码剖析目的

用源码分析的方式走一下 Netty （服务器）的启动过程，更好的理解Netty 的整体 设计和运行机制。

### 源码剖析

#### 说明

1. 源码需要剖析到Netty 调用doBind方法， 追踪到 NioServerSocketChannel的doBind
2. 并且要Debug 程序到 NioEventLoop类 的run代码 ，无限循环，在服务器端运行。

![](https://raw.githubusercontent.com/HomanLiang/pictures/main/study-demo/netty-demo/11_1.png)



#### 源码剖析过程

1. Demo 源码的基本理解

   EchoServer

   ```java
   package com.homan.netty.source.echo;
   
   import io.netty.bootstrap.ServerBootstrap;
   import io.netty.channel.*;
   import io.netty.channel.nio.NioEventLoopGroup;
   import io.netty.channel.socket.SocketChannel;
   import io.netty.channel.socket.nio.NioServerSocketChannel;
   import io.netty.handler.logging.LogLevel;
   import io.netty.handler.logging.LoggingHandler;
   import io.netty.handler.ssl.SslContext;
   import io.netty.handler.ssl.SslContextBuilder;
   import io.netty.handler.ssl.util.SelfSignedCertificate;
   
   /**
    * Echoes back any received data from a client.
    *
    * @author hmliang
    */
   public final class EchoServer {
       static final boolean SSL = System.getProperty("ssl") != null;
       static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
   
       public static void main(String[] args) throws Exception {
           // Configure SSL.
           final SslContext sslCtx;
           if (SSL) {
               SelfSignedCertificate ssc = new SelfSignedCertificate();
               sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
           } else {
               sslCtx = null;
           }
           // Configure the server.
           EventLoopGroup bossGroup = new NioEventLoopGroup(1);
           EventLoopGroup workerGroup = new NioEventLoopGroup();
           try {
               ServerBootstrap b = new ServerBootstrap();
               b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc()));
                        }
   //                     p.addLast(new LoggingHandler(LogLevel.INFO));
                         p.addLast(new EchoServerHandler());
                    }
                });
               // Start the server.
               ChannelFuture f = b.bind(PORT).sync();
               // Wait until the server socket is closed.
               f.channel().closeFuture().sync();
           } finally {
               // Shut down all event loops to terminate all threads.
               bossGroup.shutdownGracefully();
               workerGroup.shutdownGracefully();
           }
       }
   }
   ```

   说明：

   1. 先看启动类：main 方法中，首先创建了关于 SSL 的配置类。

   2. 重点分析创建的两个 EventLoopGroup 对象：

      ```
      EventLoopGroup bossGroup = new NioEventLoopGroup(1);
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      ```

      - 这两个对象是整个 Netty 的核心对象，可以说，整个 Netty 的运行都依赖于他们。 bossGroup 用于接收 Tcp 请求，他会将请求交给 workerGrop，workGroup 会获取到真正的连接，然后和连接进行通信，比如读写、解码、编码等操作

      - EventLoopGroup 是事件循环组（线程组）含多个 EventLoop，可以注册 channel，用于在事件循环中去进行选择（和选择器相关）

      - new NioEventLoopGroup(1); 这个1表示 bossGroup 事件组有1个线程你可以指定，如果 new NioEventLoopGroup() 会含有默认个线程 cpu核数*2，即可以充分的利用多核的优势

        ```java
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        ```

        会创建 EventExecutor 数组 children = new EventExecutor[nThreads];

        每个元素的类型几时 NIOEventLoop，NIOEventLoop 实现了 EventLoop 接口和 Executor 接口

        try 块中创建了一个 ServerBootstrap 对象，他是一个引导类，用于启动服务器和引导整个程序的初始化。它和 ServerChannel 关联，而 ServerChannel 继承了 Channel，有一些方法 remoteAddress等

        随后，变量 b 调用了 group 方法将两个 group 放入自己的字段中，用于后期引导使用

        

      - 然后添加了一个 channel，其中参数一个 Class 对象，引导类将通过这个 Class 对象反射创建 ChannelFactory。然后添加了一些 TCP 的参数。【说明：Channel 的创建在 bind 方法，`channel = channelFactory.newChannel();`】

      - 再添加了一个服务器专属的日志处理器 handler

      - 再添加一个 SocketChannel (不是 ServerSocketChannel) 的 handler

      - 然后绑定端口并阻塞至连接成功

      - 最后 mian 线程阻塞等待关闭

      - finally 块中的代码将在服务器关闭时优雅关闭所有资源

   EchoServerHandler

   ```java
   package com.homan.netty.source.echo;
   
   import io.netty.channel.ChannelHandler.Sharable;
   import io.netty.channel.ChannelHandlerContext;
   import io.netty.channel.ChannelInboundHandlerAdapter;
   
   /**
    * Handler implementation for the echo server.
    *
    * @author hmliang
    */
   @Sharable
   public class EchoServerHandler extends ChannelInboundHandlerAdapter {
   
       @Override
       public void channelRead(ChannelHandlerContext ctx, Object msg) {
           ctx.write(msg);
       }
   
       @Override
       public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
           super.handlerAdded(ctx);
       }
   
       @Override
       public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
           super.handlerRemoved(ctx);
       }
   
       @Override
       public void channelReadComplete(ChannelHandlerContext ctx) {
           ctx.flush();
       }
   
       @Override
       public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
           // Close the connection when an exception is raised.
           cause.printStackTrace();
           ctx.close();
       }
   }
   ```

   说明：

   1. 这是一个普通的处理器类，用于处理客户端发送的消息，在我这里，我们简单的解析出客户端传过来的内容然后打印，最后发送字符串给客户端

2. 分析 EventLoopGroup 的过程

   1. 构造器方法

      ```java
          public NioEventLoopGroup(int nThreads) {
              this(nThreads, (Executor) null);
          }
      ```

   2. 1调用下面构造器方法

      ```java
          public NioEventLoopGroup(int nThreads, Executor executor) {
              this(nThreads, executor, SelectorProvider.provider());
          }
      ```

   3. 2调用下面构造器方法

      ```java
          public NioEventLoopGroup(
                  int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
              this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
          }
      ```

   4. 3调用下面构造器方法

      ```java
          public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory,
              final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
              super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
          }
      }
      ```

   5. 上面的 super() 方法是父类：MultithreadEventLoopGroup

      ```java
          protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
              super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
          }
      ```

   6. 追踪到源码抽象类 MultithreadEventExecutorGroup的构造器方法 MultithreadEventExecutorGroup 才是 NioEventLoopGroup 真正的构造方法，这里可以看成是一个模板方法，使用了设计模式的模板模式。

   7. 分析  MultithreadEventExecutorGroup

      ```
          /**
           * Create a new instance.
           *
           * @param nThreads          使用的线程数，默认是 core*2
           * @param executor          执行器：如果传入null，则采用Netty默认的线程工厂和默认的执行器 ThreadPerTaskExecutor
           * @param chooserFactory    单例 new DefaultEventExecutorChooserFactory()
           * @param args              args 在创建执行器的时候传入固定参数
           */
          protected MultithreadEventExecutorGroup(int nThreads, Executor executor,
                                                  EventExecutorChooserFactory chooserFactory, Object... args) {
              if (nThreads <= 0) {
                  throw new IllegalArgumentException(String.format("nThreads: %d (expected: > 0)", nThreads));
              }
      		// 如果传入null，则采用Netty默认的线程工厂和默认的执行器 
              if (executor == null) {
                  executor = new ThreadPerTaskExecutor(newDefaultThreadFactory());
              }
      		// 创建指定线程数的执行器数组
              children = new EventExecutor[nThreads];
      		// 初始化线程数组
              for (int i = 0; i < nThreads; i ++) {
                  boolean success = false;
                  try {
                  	// 创建 NioEventLoop
                      children[i] = newChild(executor, args);
                      success = true;
                  } catch (Exception e) {
                      // TODO: Think about if this is a good exception type
                      throw new IllegalStateException("failed to create a child event loop", e);
                  } finally {
                  	// 如果创建失败，优雅关闭
                      if (!success) {
                          for (int j = 0; j < i; j ++) {
                              children[j].shutdownGracefully();
                          }
      
                          for (int j = 0; j < i; j ++) {
                              EventExecutor e = children[j];
                              try {
                                  while (!e.isTerminated()) {
                                      e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                                  }
                              } catch (InterruptedException interrupted) {
                                  // Let the caller handle the interruption.
                                  Thread.currentThread().interrupt();
                                  break;
                              }
                          }
                      }
                  }
              }
      
              chooser = chooserFactory.newChooser(children);
      
              final FutureListener<Object> terminationListener = new FutureListener<Object>() {
                  @Override
                  public void operationComplete(Future<Object> future) throws Exception {
                      if (terminatedChildren.incrementAndGet() == children.length) {
                          terminationFuture.setSuccess(null);
                      }
                  }
              };
      		// 为每一个单例线程池添加一个关闭监听器
              for (EventExecutor e: children) {
                  e.terminationFuture().addListener(terminationListener);
              }
      
              Set<EventExecutor> childrenSet = new LinkedHashSet<EventExecutor>(children.length);
              // 将所有的单例线程池添加到一个 HashSet 中
              Collections.addAll(childrenSet, children);
              readonlyChildren = Collections.unmodifiableSet(childrenSet);
          }
      ```

      说明：

      - 如果 executor 是 null，创建一个默认的 ThreadPerTaskExecutor，使用 Netty 默认的线程工厂。
      - 根据传入的线程数（CPU*2）创建一个线程池（单例线程池）数组。
      - 循环填充数组中的元素。如果异常，则关闭所有的单例线程池
      - 根据线程选择工厂创建一个线程选择器
      - 为每一个单例线程池添加一个关闭监听器
      - 将所有的单例线程池添加到一个 HashSet 中

3. 

4. 2

5. Netty 启动过程梳理

   - 创建2个 EventLoopGroup 线程池数组。数组默认大小 CPU*2，方便 chooser 选择线程池时提高性能
   - BootStrap 将 boss 设置为 group 属性，将 worker 设置为 childer 属性
   - 通过 bind 方法启动，内部重要方法为 initAndRegister 和 dobind 方法
   - initAndRegister 方法会反射创建 NioServerSocketChannel 及其相关的 NIO 的对象，pipeline，unsafe，同时也为 pipeline 初始化了 head 节点和 tail 节点
   - 在 register() 方法成功以后，调用 doBind() 方法，该方法会调用 NioServerSocketChannel 的 doBind 方法对 JDK 的 channel 和端口进行绑定，完成 Netty 服务器的所有启动，并开始监听连接事件































