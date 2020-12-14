[toc]



# Netty 核心源码剖析

## Netty 启动过程源码剖析

### 源码剖析目的

用源码分析的方式走一下 Netty（服务器）的启动过程，更好的理解 Netty 的整体 设计和运行机制。

### 源码剖析

#### 说明

- 源码需要剖析到 Netty 调用`doBind`方法， 追踪到 `NioServerSocketChannel`的`doBind`
- 并且要 Debug 程序到 `NioEventLoop类` 的`run` 代码 ，无限循环，在服务器端运行。

![](https://raw.githubusercontent.com/HomanLiang/pictures/main/study-demo/netty-demo/11_1.png)



#### 源码剖析过程

1. Demo 源码的基本理解

   **EchoServer**

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

   1. 先看启动类：`main` 方法中，首先创建了关于 `SSL` 的配置类。

   2. 重点分析创建的两个 `EventLoopGroup` 对象：

      ```
      EventLoopGroup bossGroup = new NioEventLoopGroup(1);
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      ```

      - 这两个对象是整个 `Netty` 的核心对象，可以说，整个`Netty` 的运行都依赖于他们。 `bossGroup` 用于接收 `Tcp` 请求，他会将请求交给 `workerGrop`，`workGroup` 会获取到真正的连接，然后和连接进行通信，比如读写、解码、编码等操作

      - `EventLoopGroup` 是事件循环组（线程组）含多个 `EventLoop`，可以注册 `channel`，用于在事件循环中去进行选择（和选择器相关）

      - `new NioEventLoopGroup(1);` 这个1表示 `bossGroup` 事件组有1个线程你可以指定，如果 `new NioEventLoopGroup()` 会含有默认个线程 `cpu核数*2`，即可以充分的利用多核的优势

        ```
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));
        ```

      - 会通过 `children = new EventExecutor[nThreads];`创建 `EventExecutor` 数组，每个元素的类型是 `NIOEventLoop`，`NIOEventLoop` 实现了 `EventLoop` 接口和 `Executor` 接口

      - try 块中创建了一个 `ServerBootstrap` 对象，他是一个引导类，用于启动服务器和引导整个程序的初始化。它和 `ServerChannel` 关联，而 `ServerChannel` 继承了 `Channel`，有一些方法 `remoteAddress`等

      - 随后，变量 `b` 调用了 `group` 方法将两个 `group` 放入自己的字段中，用于后期引导使用

      - 然后添加了一个 `channel`，其中参数一个 `Class` 对象，引导类将通过这个 `Class` 对象反射创建 `ChannelFactory`。然后添加了一些 `TCP` 的参数。【说明：`Channel` 的创建在 `bind` 方法，`channel = channelFactory.newChannel();`】

      - 再添加了一个服务器专属的日志处理器 `handler`

      - 再添加一个 `SocketChannel` (不是 `ServerSocketChannel`) 的 `handler`

      - 然后绑定端口并阻塞至连接成功

      - 最后 `mian` 线程阻塞等待关闭

      - `finally` 块中的代码将在服务器关闭时优雅关闭所有资源

   

   **EchoServerHandler**

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

   这是一个普通的处理器类，用于处理客户端发送的消息，在我这里，我们简单的解析出客户端传过来的内容然后打印，最后发送字符串给客户端

   

2. 分析 EventLoopGroup 的过程

   1. 构造器方法

      ```
          public NioEventLoopGroup(int nThreads) {
              this(nThreads, (Executor) null);
          }
      ```

   2. 1调用下面构造器方法

      ```
          public NioEventLoopGroup(int nThreads, Executor executor) {
              this(nThreads, executor, SelectorProvider.provider());
          }
      ```

   3. 2调用下面构造器方法

      ```
          public NioEventLoopGroup(
                  int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
              this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
          }
      ```

   4. 3调用下面构造器方法

      ```
          public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory,
              final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
              super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
          }
      }
      ```

   5. 上面的 super() 方法是父类：`MultithreadEventLoopGroup`

      ```
          protected MultithreadEventLoopGroup(int nThreads, ThreadFactory threadFactory, Object... args) {
              super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, threadFactory, args);
          }
      ```

   6. 追踪到源码抽象类 `MultithreadEventExecutorGroup` 的构造器方法 `MultithreadEventExecutorGroup` 才是 `NioEventLoopGroup` 真正的构造方法，这里可以看成是一个模板方法，使用了设计模式的模板模式。

   7. 分析  `MultithreadEventExecutorGroup`

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

      - 如果 `executor` 是 null，创建一个默认的 `ThreadPerTaskExecutor`，使用 `Netty` 默认的线程工厂。
      - 根据传入的线程数（`CPU*2`）创建一个线程池（单例线程池）数组
      - 循环填充数组中的元素。如果异常，则关闭所有的单例线程池
      - 根据线程选择工厂创建一个线程选择器
      - 为每一个单例线程池添加一个关闭监听器
      - 将所有的单例线程池添加到一个 `HashSet` 中

3. `ServerBootstrap` 创建和构造过程

   1. `ServerBootstrap` 是个空构造，但有默认的成员变量

      ```
          private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap<ChannelOption<?>, Object>();
          private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap<AttributeKey<?>, Object>();
      	// config 对象，会在后面起很大作用
          private final ServerBootstrapConfig config = new ServerBootstrapConfig(this);
          private volatile EventLoopGroup childGroup;
          private volatile ChannelHandler childHandler;
      ```

      

   2. 分析一下 ServerBootstrap 基本使用情况

      ```
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
      ```

      说明：

      - 链式调用：`group` 方法，将 `boss` 和 `worker` 传入，`boss` 赋值给 `parentGroup` 属性，`worker` 赋值给 `childGroup` 属性
      - `channel` 方法传入 `NioServerSocketChannel.class`，会根据这个` class `创建 `channel` 对象
      - `option` 方法传入 `TCP` 参数，放在一个 `LinkedHashMap` 中
      - `handler` 方法传入一个 `handler` ，这个 `handler` 只专属于 `ServerSocketChannel` 而不是 `SocketChannel`
      - `childHandler` 传入一个 `handler`，这个 handler 将会在每个客户端连接的时候调用，供 `SocketChannel` 使用

      

4. 绑定端口的分析

   1. 服务器就是在这个 `bind` 方法里启动完成的

   2. `bind` 方法代码，追踪到创建了一个端口对象，并做了一些空判断，核心代码 `doBind`

      ```
          public ChannelFuture bind(SocketAddress localAddress) {
              validate();
              if (localAddress == null) {
                  throw new NullPointerException("localAddress");
              }
              return doBind(localAddress);
          }
      ```

   3. `doBind` 源码剖析，核心是两个方法 `initAndRegister` 和 `doBind0`

      ```
          private ChannelFuture doBind(final SocketAddress localAddress) {
              final ChannelFuture regFuture = initAndRegister();
              final Channel channel = regFuture.channel();
              if (regFuture.cause() != null) {
                  return regFuture;
              }
      
              if (regFuture.isDone()) {
                  // At this point we know that the registration was complete and successful.
                  ChannelPromise promise = channel.newPromise();
                  // 完成对端口的绑定
                  doBind0(regFuture, channel, localAddress, promise);
                  return promise;
              } else {
                  // Registration future is almost always fulfilled already, but just in case it's not.
                  final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
                  regFuture.addListener(new ChannelFutureListener() {
                      @Override
                      public void operationComplete(ChannelFuture future) throws Exception {
                          Throwable cause = future.cause();
                          if (cause != null) {
                              // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                              // IllegalStateException once we try to access the EventLoop of the Channel.
                              promise.setFailure(cause);
                          } else {
                              // Registration was successful, so set the correct executor to use.
                              // See https://github.com/netty/netty/issues/2586
                              promise.registered();
      
                              doBind0(regFuture, channel, localAddress, promise);
                          }
                      }
                  });
                  return promise;
              }
          }
      ```

      

   4. 分析说 `initAndRegister`

      ```
          final ChannelFuture initAndRegister() {
              Channel channel = null;
              try {
                  // 通过 ServerBootstrap 的通道工厂反射创建一个 NioServerSocketChannel：
                  // 1. 通过 NIO 的 SelectorProvider 的 openServerSocketChannel 方法得到 JDK 的channel。目的是让 Netty 包装 JDK 的 channel
                  // 2. 创建了一个唯一的 ChannelId，创建了一个 NioMessageUnsafe，用于操作消息，创建了一个 DefaultChannelPipeline 管道，是个双向链表结构，用于过滤所有的进出消息
                  // 3. 创建了一个 NioServerChannelConfig 对象，用于对外展示一些配置
                  channel = channelFactory.newChannel();
                  // 初始化 NioServerSocketChannel：
                  // 1. init 方法，这是个抽象方法（AbstractBootstrap类的），由 ServerBootstrap 实现（setChannelOptions(channel, options, logger);）
                  // 2. 设置 NioServerSocketChannel 的 TCP 属性
                  // 3. 由于 LinkedHashMap 是非线程安全的，使用同步进行处理
                  // 4. 对 NioServerSocketChannel 的 ChannelPipleline 添加 ChannelInitializer 处理器
                  // 5. 可以看出，init 方法的核心作用和 ChannelPipeline 相关
                  // 6. 从 NioServerSocketChannel 的初始化过程中，我们知道，pipeline 是一个双向链表，并且它本身就是初始化了 head 和 tail，这里调用了他的 addLast 方法，也就是将整个 handler 插入到 tail 的前面，因为 tail 永远会在后面，需要做一些系统的固定工作 
                  init(channel);
              } catch (Throwable t) {
                  if (channel != null) {
                      // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                      channel.unsafe().closeForcibly();
                      // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                      return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
                  }
                  // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                  return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
              }
      
              ChannelFuture regFuture = config().group().register(channel);
              if (regFuture.cause() != null) {
                  if (channel.isRegistered()) {
                      channel.close();
                  } else {
                      channel.unsafe().closeForcibly();
                  }
              }
      
              // If we are here and the promise is not failed, it's one of the following cases:
              // 1) If we attempted registration from the event loop, the registration has been completed at this point.
              //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
              // 2) If we attempted registration from the other thread, the registration request has been successfully
              //    added to the event loop's task queue for later execution.
              //    i.e. It's safe to attempt bind() or connect() now:
              //         because bind() or connect() will be executed *after* the scheduled registration task is executed
              //         because register(), bind(), and connect() are all bound to the same thread.
      
              return regFuture;
          }
      ```

      说明：

      - `initAndRegister()` 初始化 `NioServerSocketChannel` 通道并注册各个 `handler`，返回一个 `future`
      - 通过 `ServerBootstrap` 的通道工厂反射创建一个 `NioServerSocketChannel`
      - `init` 初始化 `NioServerSocketChannel`
      - `config().group().register(channel)` 通过 `serverBootstrap` 的 `bossGroup` 注册 `NioServerSocketChannel`
      - 最后返回这个异步执行的占位符即 `regFuture`

   5. init 方法会调用 addLast，现在进入到 addLast 方法内查看

      ```
          @Override
          public final ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
              final AbstractChannelHandlerContext newCtx;
              synchronized (this) {
                  checkMultiplicity(handler);
      
                  newCtx = newContext(group, filterName(name, handler), handler);
      
                  addLast0(newCtx);
      
                  // If the registered is false it means that the channel was not registered on an eventloop yet.
                  // In this case we add the context to the pipeline and add a task that will call
                  // ChannelHandler.handlerAdded(...) once the channel is registered.
                  if (!registered) {
                      newCtx.setAddPending();
                      callHandlerCallbackLater(newCtx, true);
                      return this;
                  }
      
                  EventExecutor executor = newCtx.executor();
                  if (!executor.inEventLoop()) {
                      newCtx.setAddPending();
                      executor.execute(new Runnable() {
                          @Override
                          public void run() {
                              callHandlerAdded0(newCtx);
                          }
                      });
                      return this;
                  }
              }
              callHandlerAdded0(newCtx);
              return this;
          }
      ```

      说明：

      - `addLast` 方法，在 `DefaultChannelPipeline` 类中
      - `addLast` 方法就是 `pipeline` 方法的核心
      - 检查该 `handler` 是否符合标准
      - 创建一个 `AbstractChannelHandlerContext` 对象，这里说一下，`ChannelHandlerContext` 对象是 `ChannelHandler` 和 `ChannelPipeline` 之间的关联，每当有 `ChannelHandler` 添加到 `pipeline` 中，都会创建 `Context`。`Context` 的主要功能是管理它所关联的 `Handler` 和同一个 `Pipeline` 中的其他 `Handler` 之间的交互
      - 将 `Context` 添加到链表中，也就是追加到 `tail` 节点的前面
      - 最后同步或者异步或者晚点异步的调用 `callHandlerAdded0` 方法

   6. 前面说了 `dobind` 方法有2个重要的步骤，`initAndRegister` 说完，接下来看 `doBind0`方法

      1. `doBind0` 方法

      ```
          private static void doBind0(
                  final ChannelFuture regFuture, final Channel channel,
                  final SocketAddress localAddress, final ChannelPromise promise) {
      
              // This method is invoked before channelRegistered() is triggered.  Give user handlers a chance to set up
              // the pipeline in its channelRegistered() implementation.
              channel.eventLoop().execute(new Runnable() {
                  @Override
                  public void run() {
                      if (regFuture.isSuccess()) {
                      	// 往下看
                          channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                      } else {
                          promise.setFailure(regFuture.cause());
                      }
                  }
              });
          }
      ```

      说明：
      - 该方法的参数为 `initAndRegister` 的 `future`，`NioServerSocketChannel`的端口地址，`NioServerSocketChannel` 的 `promise`

      2. 往下看，将调用 `LoggingHandler` 的 `invokeBind` 方法，最后会追到

      ```
              @Override
              public void bind(
                      ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise)
                      throws Exception {
                  unsafe.bind(localAddress, promise);
              }
      ```

      3. 继续追踪 `AbstractChannel` 的 `bind` 方法

         ```
                 @Override
                 public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
                     assertEventLoop();
         
                     if (!promise.setUncancellable() || !ensureOpen(promise)) {
                         return;
                     }
         
                     // See: https://github.com/netty/netty/issues/576
                     if (Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST)) &&
                         localAddress instanceof InetSocketAddress &&
                         !((InetSocketAddress) localAddress).getAddress().isAnyLocalAddress() &&
                         !PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser()) {
                         // Warn a user about the fact that a non-root user can't receive a
                         // broadcast packet on *nix if the socket is bound on non-wildcard address.
                         logger.warn(
                                 "A non-root user can't receive a broadcast packet if the socket " +
                                 "is not bound to a wildcard address; binding to a non-wildcard " +
                                 "address (" + localAddress + ") anyway as requested.");
                     }
         
                     boolean wasActive = isActive();
                     try {
                         // 这里是最终的doBind 方法，执行成功后，执行通道的 fireChannelActive 方法，告诉所有的 handler，已经成功绑定
                         doBind(localAddress);
                     } catch (Throwable t) {
                         safeSetFailure(promise, t);
                         closeIfClosed();
                         return;
                     }
         
                     if (!wasActive && isActive()) {
                         invokeLater(new Runnable() {
                             @Override
                             public void run() {
                                 pipeline.fireChannelActive();
                             }
                         });
                     }
         
                     safeSetSuccess(promise);
                 }
         ```

         

      4. 最终 `doBind` 就会追踪到 `NioServerSocketChannel` 的 `doBind`，说明 `Netty` 底层使用的是 `Nio`

         ```
             @Override
             protected void doBind(SocketAddress localAddress) throws Exception {
                 if (PlatformDependent.javaVersion() >= 7) {
                     javaChannel().bind(localAddress, config.getBacklog());
                 } else {
                     javaChannel().socket().bind(localAddress, config.getBacklog());
                 }
             }
         ```

   7. 回到 `bind` 方法，最后一步：`safeSetSuccess(promise);`，告诉 `promise` 任务成功了。其可以执行监听器的方法了。到此整个启动过程已经结束了。

5. 继续往下执行，服务器就会进入到（`NioEventLoop` 类）一个循环代码，进行监听

   ```
       @Override
       protected void run() {
           for (;;) {
               try {
                   ...
               }
           }
       }
   ```

   

#### Netty 启动过程梳理

1. 创建 2 个 `EventLoopGroup` 线程池数组。数组默认大小 `CPU*2`，方便 `chooser` 选择线程池时提高性能
2. `BootStrap` 将 `boss` 设置为 `group` 属性，将 `worker` 设置为 `childer` 属性
3. 通过 `bind` 方法启动，内部重要方法为 `initAndRegister` 和 `dobind` 方法
4. `initAndRegister` 方法会反射创建 `NioServerSocketChannel` 及其相关的 `NIO` 的对象、`pipeline`、`unsafe`，同时也为 `pipeline` 初始化了 `head` 节点和 `tail` 节点
5. 在 `register()` 方法成功以后，调用 `doBind() `方法，该方法会调用 `NioServerSocketChannel` 的 `doBind` 方法对 `JDK` 的 `channel` 和端口进行绑定，完成 Netty 服务器的所有启动，并开始监听连接事件































