# Pipeline Handler HandlerContext 创建源码剖析

## 源码剖析目的

Netty 中的 ChannelPipeline 、 ChannelHandler 和 ChannelHandlerContext 是非常核心的组件, 我们从源码来分析 Netty 是如何设计这三个核心组件的，并分析是如何创建和协调工作的



## 源码剖析

###  ChannelPipeline 、 ChannelHandler 和 ChannelHandlerContext 介绍

1. 三者关系

   - 每当 ServerSocket 创建了一个新的连接，就会创建一个 Socket，对应的就是目标客户端
   - 每一个新创建的 Socket 都将会分配一个全新的 ChannelPipeline（以下简称 pipeline）
   - 每一个 ChannelPipeline 内部都含有多个 ChannelHandlerContext（以下简称 Context）
   - 他们一起组成了双向链表，这些 Context 用于包装我们调用 addLast 方法时添加的 ChannelHandler（以下简称 handler）

   ![image-20201220140730360](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220140730360.png)

   上图说明：

   - ChannelSocket 和 ChannelPipeline 是一对一的关联关系，而pipeline 内部的多个 Context 形成了链表，Context 只是对 Handler 的封装
   - 当一个请求进来的时候，会进入 Socket 对应的 pipeline，并经过 pipeline 所有的 handler （过滤器模式）



2. ChannelPipeline 作用及设计

   - pipeline 的接口设计

     ![image-20201220141551123](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220141551123.png)

     部分方法

     ![image-20201220141626936](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220141626936.png)

     说明：

     - 可以看出该接口继承了 inBound，outBound， Iterable 接口，表示他可以调用数据出站的方法和入站的方法，同时也能遍历内部的链表，看看他的几个代表性的方法，基本上都是针对 handler 链表的插入、追加、删除、替换操作，类似是一个 LinkedList。同时也能返回 channel（也就是 socket）

   - 在 pipeline 的接口文档上，提供了一幅画

     ![image-20201220142207434](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220142207434.png)

     对上图说明：

     - 这是一个 handler 的 list，handler 用于处理或拦截入站事件和出站事件，pipeline 实现了过滤器的高级形式，以便用户控制事件如何处理以及 handler 在 pipeline 中如何交互。

     - 上图描述了一个典型的 handler 在 Pipeline 中处理 I/O 事件的方式，I/O事件由 inBoundHandler 或者 outBoundHandler 处理，并通过调用 ChannelHandlerContext.fireChannelRead 方法转发给其最近的处理程序

     - 入站事件由入站处理程序以自下而上的方向处理，如图所示。入站处理程序通常处理由图底部的I/O线程生成入站数据。入站数据通常从如 SocketChannel.read(ByteBuffer) 获取。

     - 通常一个 pipeline 有多个 handler，例如，一个典型的服务器在每个通道的管道中都会有以下处理程序：

       1. 协议解码器：将二进制数据转换成 Java 对象
       2. 协议编码器：将 Java 对象转换为二进制数据
       3. 业务逻辑处理程序：执行实际业务逻辑（例如数据库访问）

     - 你的业务程序不能将线程阻塞，会影响 I/O 速度，进而影响整个 Netty 程序的性能。如果你的业务程序很快，就可以放在 I/O 线程中，反之你需要异步执行。或者在添加 handler 的时候添加一个线程池。

       例如：

       ```
       // 下面这个任务执行的时候，将不会阻塞 I/O 线程，执行的线程来自 group 线程池
       pipeline.addLast(group, "handler", new MyBusinessLogicHandler());
       ```

       



3. ChannelHandler 作用及设计

   - 源码

     ```
     public interface ChannelHandler {
     
         /**
          * Gets called after the {@link ChannelHandler} was added to the actual context and it's ready to handle events.
          */
         void handlerAdded(ChannelHandlerContext ctx) throws Exception;
     
         /**
          * Gets called after the {@link ChannelHandler} was removed from the actual context and it doesn't handle events
          * anymore.
          */
         void handlerRemoved(ChannelHandlerContext ctx) throws Exception;
     
         /**
          * Gets called if a {@link Throwable} was thrown.
          *
          * @deprecated is part of {@link ChannelInboundHandler}
          */
         @Deprecated
         void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception;
     }
     ```

     ChannelHandler 的作用就是处理 I/O 事件或者拦截 I/O 事件，并将其转发给下一个处理程序 ChannelHandler。Handler 处理事件时分入站和出站的，两个方向的操作都是不同，因此 Netty 定义了两个子接口继承 ChannelHandler

   - ChannelInboundHandler 入站事件接口

     ![image-20201220165044305](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220165044305.png)

     - channelActive 用于当 Channel 处于活动状态时被调用
     - channelRead 当从 Channel 读取数据时被调用
     - 程序员需要重写一些方法，当发生关注的事件，需要在方法中实现我们的业务逻辑，因为当事件发生时，Netty 会回调对应的方法

   - ChannelOutboundHandler 出站事件接口

     ![image-20201220165500887](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220165500887.png)

     - bind 方法：当请求将 Channel 绑定到本地地址时调用
     - close 方法：当请求关闭 Channel 时调用
     - 出入站操作都是一些连接和写出数据类似的方法

   - ChannelDuplexHandler 处理出站和入站事件

     ![image-20201220170300335](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220170300335.png)

     - ChannelDuplexHandler 间接实现了入站接口并直接实现了出站接口
     - 是一个通用的能够同时处理入站事件和出站事件的类



4. ChannelHandlerContext 作用及设计

   - ChannelHandlerContext UML 图

     ![image-20201220170648497](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220170648497.png)

     ChannelHandlerContext  继承了出站 方法调用接口和入站方法调用接口

     - ChannelOutboundInvoker 和 ChannelInboundInvoker 部分源码

       ![image-20201220170844085](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220170844085.png)

       ![image-20201220170903031](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220170903031.png)

       这两个 invoker 就是针对入站或出站方法来的，就是在入站或出站 handler 的外层再包装一层，达到在方法前拦截并做一些特定操作的目的

   - ChannelHandlerContext 部分源码

     ![image-20201220171125748](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220171125748.png)

     - ChannelHandlerContext 不仅仅是继承了他们两个的方法，同时还定义了一些自己的方法
     - 这些方法能够获取 Context 上下文环境中对应的比如：channel、executor、handler、pipeline、内存分配器、关联的 handler 是否被删除
     - Context 就是包装了 handler 相关的一切，以方便 Context 可以在 pipeline 方便的操作 handler



### ChannelPipeline 、 ChannelHandler 和 ChannelHandlerContext 创建过程

分为 3 个步骤来看创建的过程：

- 任何一个 ChannelSocket 创建的同时都会创建一个 pipeline

- 当用户或系统内部调用 pipeline 的 addXXX 方法添加 handler 时，都会创建一个包装这 handler 的 Context

- 这些 Context 在 pipeline 中组成了双向链表

  

1. Socket 创建 pipeline

   在 SocketChannel 的抽象父类 AbstractChannel 的构造方法中

   ```
       protected AbstractChannel(Channel parent) {
           this.parent = parent; // 断点测试
           id = newId();
           unsafe = newUnsafe();
           pipeline = newChannelPipeline();
       }
   ```

   Debug 一下，可以看到代码会执行到这里，然后继续追踪到

   ```
       protected DefaultChannelPipeline(Channel channel) {
           this.channel = ObjectUtil.checkNotNull(channel, "channel");
           succeededFuture = new SucceededChannelFuture(channel, null);
           voidPromise =  new VoidChannelPromise(channel, true);
   
           tail = new TailContext(this);
           head = new HeadContext(this);
   
           head.next = tail;
           tail.prev = head;
       }
   ```

   说明：

   -  将 channel 赋值给 channel 字段，用于 pipeline 操作 channel
   - 创建一个 future 和 promise， 用于异步回调使用
   - 创建一个 inbound 的 tailContext，创建一个既是 inbound 类型又是 outbound 类型的 headContext
   - 最后把两个 Context 互相连接，形成双向链表
   - TailContext 和 HeadContext 非常重要，所有 pipeline 中的事件都会流经他们



2. 在 addXXX 添加处理器的时候，创建 ContextXXX

   看下 DefaultChannelPipeline 的 addLast 方法如何创建的 Context，代码如下：

   ```
       @Override
       public final ChannelPipeline addLast(EventExecutorGroup executor, ChannelHandler... handlers) {
           if (handlers == null) {
               throw new NullPointerException("handlers");
           }
   
           for (ChannelHandler h: handlers) {
               if (h == null) {
                   break;
               }
               addLast(executor, null, h);
           }
   
           return this;
       }
   ```

   继续 Debug

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

   - pipeline 添加 handler，参数是线程池，name 是 null，handler 是我们或者系统传入的 handler。Netty 为了防止多个线程导致安全问题，同步了这段代码，步骤如下：
   - 检查这个 handler 实例是否共享的，如果不是，并且已经被别的 pipeline 使用了，则抛出异常。
   - 调用 newContext(group, filterName(name, handler), handler) 方法，创建了一个 Context。从这里可以看出来了，每次添加一个 handler 都会创建一个关联 Context。
   - 调用 addLast 方法，将 Context 追加到链表中
   - 如果这个通道还没有注册到 selector 上，就将这个 Context 添加到这个 pipeline 的待办任务中。当注册好了以后，就会调用 callHandlerAdded0 方法（默认是什么都不做，用户可以实现这个方法）
   - 到这里，针对三对象创建过程，了解的差不多了，和最初说的一样，每当创建 ChannelSocket 的时候都会创建一个绑定的 pipeline，一对一的关系，创建 pipeline 的时候也会创建 tail 节点和 head 节点，形成最初的链表。tail 是入站 inbound 类型的 handler，head 既是 inbound 也是 outbound 类型的 handler。在调用 pipeline 的 addLast 方法的时候，会根据给定的 handler 创建一个 Context，然后将这个 Context 插入到链表的尾端（tail 前面）。



## Pipeline Handler HandlerContext创建过程梳理

1. 每当创建 ChannelSocket 的时候都会创建一个绑定的 pipeline，一对一的关系，创建 pipeline 的时候也会创建 tail 节点和 head 节点，形成最初的链表。
2. 在调用 pipeline 的 addLast 方法的时候，会根据给定的 handler 创建一个 Context，然后，将这个 Context 插入到链表的尾端（tail 前面）。
3. Context 包装 handler，多个 Context 在 pipeline 中形成了双向链表
4. 入站方向叫 inbound，由 head 节点开始，出站方法叫 outbound ，由 tail 节点开始





















