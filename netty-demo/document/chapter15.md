# Netty 心跳(heartbeat)服务源码剖析

## 源码剖析目的

Netty 作为一个网络框架，提供了诸多功能，比如编码解码等，Netty 还提供了非常重要的一个服务-----心跳机制heartbeat。通过心跳检查对方是否有效，这是 RPC 框架中是必不可少的功能。下面我们分析一下Netty内部心跳服务源码实现。



## 源码剖析

### 说明

1. Netty 提供了 IdleStateHandler ，ReadTimeoutHandler，WriteTimeoutHandler 三个Handler 检测连接的有效性，重点分析 IdleStateHandler 。如图所示：

   ![image-20201220203042293](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220203042293.png)

2. ReadTimeout 事件和 WriteTimeout 事件都会自动关闭连接，而且属于异常处理，所以这里只介绍一下，我们重点看 IdleStateHandler 



### 源码剖析

### IdleStateHandler 分析

1. 4 个属性

   ```
       // 是否考虑出站时较慢的情况。默认是false
       private final boolean observeOutput;
       // 读事件空闲时间，0 则禁用事件
       private final long readerIdleTimeNanos;
       // 读事件空闲时间，0 则禁用事件
       private final long writerIdleTimeNanos;
       // 读或写空闲时间，0 则禁用事件
       private final long allIdleTimeNanos;
   ```

   

2. handlerAdded 方法

   当该 handler 被添加到 pipeline 中时，则调用 initialize 方法

   ```
       private void initialize(ChannelHandlerContext ctx) {
           // Avoid the case where destroy() is called before scheduling timeouts.
           // See: https://github.com/netty/netty/issues/143
           switch (state) {
           case 1:
           case 2:
               return;
           }
   
           state = 1;
           initOutputChanged(ctx);
   
           lastReadTime = lastWriteTime = ticksInNanos();
           if (readerIdleTimeNanos > 0) {
               readerIdleTimeout = schedule(ctx, new ReaderIdleTimeoutTask(ctx),
                       readerIdleTimeNanos, TimeUnit.NANOSECONDS);
           }
           if (writerIdleTimeNanos > 0) {
               writerIdleTimeout = schedule(ctx, new WriterIdleTimeoutTask(ctx),
                       writerIdleTimeNanos, TimeUnit.NANOSECONDS);
           }
           if (allIdleTimeNanos > 0) {
               allIdleTimeout = schedule(ctx, new AllIdleTimeoutTask(ctx),
                       allIdleTimeNanos, TimeUnit.NANOSECONDS);
           }
       }
   ```

   只要给定的参数大于 0，就创建一个定时任务，每个事件都创建。同时，将 state 状态设置为 1，防止重复初始化。调用 initOutputChanged 方法，初始化“监控出站数据属性”。



3. 该类内部的 3 个定时任务类

   ![image-20201220210802793](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201220210802793.png)

   这 3 个定时任务分别对应读、写、读或写事件。都共有一个父类（AbstractIdleTask），这个父类提供了一个模板方法

   ```
       private abstract static class AbstractIdleTask implements Runnable {
   
           private final ChannelHandlerContext ctx;
   
           AbstractIdleTask(ChannelHandlerContext ctx) {
               this.ctx = ctx;
           }
   
           @Override
           public void run() {
               if (!ctx.channel().isOpen()) {
                   return;
               }
   
               run(ctx);
           }
   
           protected abstract void run(ChannelHandlerContext ctx);
       }
   ```

   说明：当通道关闭了，就不执行任务了。反之执行子类的 run 方法



### 读事件的 run 方法（即 ReaderIdleTimeoutTask）分析

1. 代码及其说明

   ```
           @Override
           protected void run(ChannelHandlerContext ctx) {
               long nextDelay = readerIdleTimeNanos;
               if (!reading) {
                   nextDelay -= ticksInNanos() - lastReadTime;
               }
   
               if (nextDelay <= 0) {
                   // Reader is idle - set a new timeout and notify the callback.
                   readerIdleTimeout = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
   
                   boolean first = firstReaderIdleEvent;
                   firstReaderIdleEvent = false;
   
                   try {
                       IdleStateEvent event = newIdleStateEvent(IdleState.READER_IDLE, first);
                       channelIdle(ctx, event);
                   } catch (Throwable t) {
                       ctx.fireExceptionCaught(t);
                   }
               } else {
                   // Read occurred before the timeout - set a new timeout with shorter delay.
                   readerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
               }
           }
   ```

   说明：

   - 得到用户设置的超时时间
   - 如果读取操作结束了（执行了 channelReadComplete 方法设置），就用当前时间减去给定时间和最后一次读（channelReadComplete 方法设置），如果小于 0就触发事件。反之，继续放入队列。间隔时间是新的计算时间
   - 触发的逻辑是：首先将任务再次放到队列，时间是刚开始设置的时间，返回一个 promise 对象，用于做取消操作。然后设置 first 属性为 false，表示下一次读取不再是第一次，这个属性在 channelRead 方法会被改成 true
   - 创建一个 IdleStateEvent 类型的写事件对象，将此对象传递给用户的 UserEventTriggered 方法。完成触发事件的操作
   - 总的来说，每次读取操作都会记录一个时间，定时任务时间到了，会计算当前时间和最后一次读的时间的间隔，如果间隔超过了设置的时间，就会触发 UserEventTriggered 方法。



### 写事件的 run 方法（即 WriterIdleTimeoutTask）分析

1. 代码及其说明

   ```
           protected void run(ChannelHandlerContext ctx) {
   
               long lastWriteTime = IdleStateHandler.this.lastWriteTime;
               long nextDelay = writerIdleTimeNanos - (ticksInNanos() - lastWriteTime);
               if (nextDelay <= 0) {
                   // Writer is idle - set a new timeout and notify the callback.
                   writerIdleTimeout = schedule(ctx, this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
   
                   boolean first = firstWriterIdleEvent;
                   firstWriterIdleEvent = false;
   
                   try {
                       if (hasOutputChanged(ctx, first)) {
                           return;
                       }
   
                       IdleStateEvent event = newIdleStateEvent(IdleState.WRITER_IDLE, first);
                       channelIdle(ctx, event);
                   } catch (Throwable t) {
                       ctx.fireExceptionCaught(t);
                   }
               } else {
                   // Write occurred before the timeout - set a new timeout with shorter delay.
                   writerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
               }
           }
   ```

   说明：

   - 写任务的 run 代码逻辑基本和读任务的逻辑一样，唯一不同的就是有一个针对出站较慢数据的判断 hasOutputChanged



### 所有事件的 run 方法（即 AllIdleTimeoutTask）分析

1. 代码及其说明

   ```
           @Override
           protected void run(ChannelHandlerContext ctx) {
   
               long nextDelay = allIdleTimeNanos;
               if (!reading) {
                   nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
               }
               if (nextDelay <= 0) {
                   // Both reader and writer are idle - set a new timeout and
                   // notify the callback.
                   allIdleTimeout = schedule(ctx, this, allIdleTimeNanos, TimeUnit.NANOSECONDS);
   
                   boolean first = firstAllIdleEvent;
                   firstAllIdleEvent = false;
   
                   try {
                       if (hasOutputChanged(ctx, first)) {
                           return;
                       }
   
                       IdleStateEvent event = newIdleStateEvent(IdleState.ALL_IDLE, first);
                       channelIdle(ctx, event);
                   } catch (Throwable t) {
                       ctx.fireExceptionCaught(t);
                   }
               } else {
                   // Either read or write occurred before the timeout - set a new
                   // timeout with shorter delay.
                   allIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
               }
           }
   ```

   说明：

   - 表示这个监听着所有的事件。当读写事件发生时，都会记录。代码逻辑和写事件的基本一致

   - 需要大家注意的地方是

     ```
                 long nextDelay = allIdleTimeNanos;
                 if (!reading) {
                 	// 当前时间减去最后一次写或读的时间，若大于0，说明超时了
                     nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
                 }
     ```

     

   - 这里的时间计算是取读写事件中的最大值来的，然后像写事件一样，判断是否发生了写的慢的情况



## Netty 的心跳机制小结

1. IdleStateHandler 可以实现心跳功能，当服务器和客户端没有任何读写交互时，并超过了给定的时间，则会触发用户 handler 的 userEventTriggered 方法。用户可以在这个方法中尝试向对方发送消息，如果发送失败，则关闭连接。
2. IdleStateHandler 的实现基于 EventLoop 的定时任务，每次读写都会记录一个值，在定时任务运行的时候，通过计算当时时间和设置时间和上次事件发生时间的结果来判断是否空闲
3. 内部有 3 个定时任务，分别对应读事件、写事件、读写事件。通常用户监听读写事件就足够了。
4. 同时 IdleStateHandler 内部也考虑了一些极端情况：客户端接收缓慢，一次接收数据的速度超过了设置的空闲时间。Netty 通过构造方法中的 observeOutput 属性来决定是否对出站缓冲区的情况进行判断。
5. 如果出站缓慢，Netty 不认为这是空闲，也不触发空闲事件。但第一次无论如何也是要触发的。因为第一次无法判断是出站缓慢还是空闲。当然，出站缓慢的话，可能造成 OOM，OOM 比空闲的问题更大。
6. 所以，当你的应用出现了内存溢出，OOM 之类，并且写空闲极少发生（使用了 observeOutput 为 true），那么就需要注意是不是数据出站速度过慢
7. 还有一个注意的地方：就是 ReadTimeoutHandler，它继承自 IdleStateHandler，当触发读空闲事件的时候，就触发 ctx.fireExceptionCaught 方法，并传入一个 ReadTimeoutException，然后关闭 Socket。
8. 而 WriteTimeoutHandler 的实现不是基于 IdleStateHandler 的，他的原理是，当调用 write 方法的时候，会创建一个定时任务，任务内容是根据传入的 promise 的完成情况来判断是否超过了写的时间。当定时任务根据指定时间开始运行，发现 promise 的 isDone 方法返回 false，表明还没有写完，说明超时了，则抛出异常。当 write 方法完成后，会打断定时任务。





