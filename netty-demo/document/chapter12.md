[toc]



# Netty 核心源码剖析（二）

## 1.Netty 接收请求过程源码剖析 

### 1.1.源码剖析目的

服务器启动后肯定是要接受客户端请求并返回客户端想要的信息的，下面源码分析 Netty 在启动之后是如何接受客户端请求的



### 1.2.源码剖析

#### 1.2.1.说明

1. 从之前服务器启动的源码中，我们得知，服务器最终注册了一个 Accept 事件等待客户端的连接。我们也知道，NioServerSocketChannel 将自己注册到了 boss 单例线程池（reactor 线程）上，也就是 EventLoop 。

2. 先简单说下EventLoop的逻辑(后面我们详细讲解EventLoop)，EventLoop 的作用是一个死循环，而这个循环中做3件事情：
   - 有条件的等待 Nio 事件。
   - 处理 Nio 事件。
   - 处理消息队列中的任务。
3. 仍用前面的项目来分析：进入到 NioEventLoop 源码中后，在 `private void processSelectedKey(SelectionKey k, AbstractNioChannel ch) ` 方法开始调试

4. 最终我们要分析到 AbstractNioChannel 的 doBeginRead 方法， 当到这个方法时，针对于这个客户端的连接就完成了，接下来就可以监听读事件了



#### 1.2.2.源码分析过程

1. 断点位置 NioEventLoop 的如下方法 processSelectedKey

    ```
                // Also check for readOps of 0 to workaround possible JDK bug which may otherwise lead
                // to a spin loop
                if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                    unsafe.read();
                }
    ```



2. 浏览器输入 `http//localhost:8007/`，客户端发送请求

3. 从断点我们可以看到，readyOps 是16，也就是 Accept 事件。说明浏览器的请求已经进来了。

4. 这个 unsafe 是 boss 线程中 `NioServerSocketChannel` 的 `AbstractNioMessageChannel$NioMessageUnsafe` 对象。我们进入到 `AbstractNioMessageChannel$NioMessageUnsafe` 的 read 方法中

5. read 方法代码

    ```
           @Override
            public void read() {
                assert eventLoop().inEventLoop();
                final ChannelConfig config = config();
                final ChannelPipeline pipeline = pipeline();
                final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
                allocHandle.reset(config);
    
                boolean closed = false;
                Throwable exception = null;
                try {
                    try {
                        do {
                            int localRead = doReadMessages(readBuf);
                            if (localRead == 0) {
                                break;
                            }
                            if (localRead < 0) {
                                closed = true;
                                break;
                            }
    
                            allocHandle.incMessagesRead(localRead);
                        } while (allocHandle.continueReading());
                    } catch (Throwable t) {
                        exception = t;
                    }
    
                    int size = readBuf.size();
                    for (int i = 0; i < size; i ++) {
                        readPending = false;
                        pipeline.fireChannelRead(readBuf.get(i));
                    }
                    readBuf.clear();
                    allocHandle.readComplete();
                    pipeline.fireChannelReadComplete();
    
                    if (exception != null) {
                        closed = closeOnReadError(exception);
    
                        pipeline.fireExceptionCaught(exception);
                    }
    
                    if (closed) {
                        inputShutdown = true;
                        if (isOpen()) {
                            close(voidPromise());
                        }
                    }
                } finally {
                    // Check if there is a readPending which was not processed yet.
                    // This could be for two reasons:
                    // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                    // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                    //
                    // See https://github.com/netty/netty/issues/2254
                    if (!readPending && !config.isAutoRead()) {
                        removeReadOp();
                    }
                }
            }
    ```

    说明：
    - 检查该 eventLoop 线程是否是当前线程。`assert eventLoop().inEventLoop();`
    - 执行 doReadMessages 方法，并传入一个 readBuf 变量，这个变量是个一个 List 容器。
    - 循环容器，执行 `pipeline.fireChannelRead(readBuf.get(i));`
    - doReadMessages 是读取 boss 线程中的 NioServerSocketChannel 接收到的请求，并把这些请求放进容器。
    - 循环遍历容器中的所有请求，调用 pipeline 的 fireChannelRead 方法，用于处理这些接收的请求或者其他事件，在 read 方法中，循环调用 ServerSocket 的 pipeline 的 fireChannelRead 方法，开始执行管道中的 handler 的 ChannelRead 方法



6. 追踪一下 doReadMessages 方法，就可以看得更清晰

    ```
        @Override
        protected int doReadMessages(List<Object> buf) throws Exception {
            SocketChannel ch = SocketUtils.accept(javaChannel());
    
            try {
                if (ch != null) {
                    buf.add(new NioSocketChannel(this, ch));
                    return 1;
                }
            } catch (Throwable t) {
                logger.warn("Failed to create a new channel from an accepted socket.", t);
    
                try {
                    ch.close();
                } catch (Throwable t2) {
                    logger.warn("Failed to close a socket.", t2);
                }
            }
    
            return 0;
        }
    ```

    说明：
    - 通过工具类，调用 NioServerSocketChannel 内部封装的 serverSocketChannel 的 accept 方法，这就是 NIO 做法
    - 获取到一个 JDK 的 SocketChannel，然后使用 NioSocketChannel 进行封装，最后添加到容器中
    - 这样容器 buf 中就有了 NioSocketChannel



7. 回到 read 方法，继续分析。循环执行 `pipeline.fireChannelRead` 方法

   - 前面分析 doReadMessages 方法的作用是通过 ServerSocket 的 accept 方法获取到 TCP 连接，然后封装成 Netty 的 NioSocketChannel 对象，最后添加到容器中

   - 在 read 方法中，循环调用 ServerSocket 的 pipeline 的 fireChannelRead 方法，开始执行管道中的 handler 的 ChannelRead 方法

   - 经过 debug (多次)，可以看到会反复执行多个 handler 的 ChannelRead，我们知道，pipeline 里面有 4 个 handler，分别是 Head、LoggingHandler、ServerBootstrapAcceptor、Tail

   - 我们重点看看 ServerBootstrapAcceptor，debug 之后，断点会进入到 ServerBootstrapAcceptor 中。我们来看看 ServerBootstrapAcceptor 的 channelRead 方法（要多次 debug 才可以）

   - channelRead 方法

     ```
             @Override
             @SuppressWarnings("unchecked")
             public void channelRead(ChannelHandlerContext ctx, Object msg) {
                 final Channel child = (Channel) msg;
     
                 child.pipeline().addLast(childHandler);
     
                 setChannelOptions(child, childOptions, logger);
     
                 for (Entry<AttributeKey<?>, Object> e: childAttrs) {
                     child.attr((AttributeKey<Object>) e.getKey()).set(e.getValue());
                 }
     
                 try {
                 	// 将客户端连接注册到 worker 线程池
                     childGroup.register(child).addListener(new ChannelFutureListener() {
                         @Override
                         public void operationComplete(ChannelFuture future) throws Exception {
                             if (!future.isSuccess()) {
                                 forceClose(child, future.cause());
                             }
                         }
                     });
                 } catch (Throwable t) {
                     forceClose(child, t);
                 }
             }
     ```

     说明：
     
     - msg 强转成 Channel，实际上就是 NioSocketChannel
     - 添加 NioSocketChannel 的 pipeline 的 handler，就是我们 main 方法里面设置的 childHandler 方法里的
     - 设置 NioSocketChannel 的各种属性。
     - 将该 NioSocketChannel 注册到 childGroup 中的一个 EventLoop 上，并添加一个监听器
     - 这个 childGroup 就是我们 main 方法创建的数组 workerGroup



8. 进入 register 方法查看

   ```
           @Override
           public final void register(EventLoop eventLoop, final ChannelPromise promise) {
               if (eventLoop == null) {
                   throw new NullPointerException("eventLoop");
               }
               if (isRegistered()) {
                   promise.setFailure(new IllegalStateException("registered to an event loop already"));
                   return;
               }
               if (!isCompatible(eventLoop)) {
                   promise.setFailure(
                           new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                   return;
               }
   
               AbstractChannel.this.eventLoop = eventLoop;
   
               if (eventLoop.inEventLoop()) {
                   register0(promise);
               } else {
                   try {
                       eventLoop.execute(new Runnable() {
                           @Override
                           public void run() {
                           	// 进入到这里
                               register0(promise);
                           }
                       });
                   } catch (Throwable t) {
                       logger.warn(
                               "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                               AbstractChannel.this, t);
                       closeForcibly();
                       closeFuture.setClosed();
                       safeSetFailure(promise, t);
                   }
               }
           }
   ```

   继续进入下面方法，执行管道中可能存在的任务，这里我们就不追了



9. 最终会调用 doBeginRead 方法，也就是 AbstractNioChannel 类的方法

   ```
       @Override
       protected void doBeginRead() throws Exception {
           // Channel.read() or ChannelHandlerContext.read() was called
           // 断点
           final SelectionKey selectionKey = this.selectionKey;
           if (!selectionKey.isValid()) {
               return;
           }
   
           readPending = true;
   
           final int interestOps = selectionKey.interestOps();
           if ((interestOps & readInterestOp) == 0) {
               selectionKey.interestOps(interestOps | readInterestOp);
           }
       }
   ```

   

10. 这个地方调试时，请把前面的断点都去掉，然后启动服务器就会停止在 doBeginRead (需要先放过该断点，然后浏览器请求，才能看到效果)

    

11. 执行到这里时，针对这个客户端的连接就完成了，接下来就可以监听读事件了



#### 1.2.3.Netty接受请求过程梳理

总体流程：接收连接 => 创建一个新的 NioSocketChannel => 注册到一个 worker EventLoop 上 =>  注册 selector Read 事件

1. 服务器轮询 Accept 事件，获取事件后调用 unsafe 的 read 方法，这个 unsafe 是 ServerSocket 的内部类，该方法内部由 2 部分组成
2. doReadMessages 用于创建 NioSocketChannel 对象，该对象包装 JDK 的 Nio Channel 客户端。该方法会像创建 ServerSocketChanel 类似创建相关的 pipeline， unsafe，config
3. 随后执行 pipeline.fireChannelRead 方法，并将自己绑定到一个 chooser 选择器选择的 workerGroup 中的一个 EventLoop，并且注册









































