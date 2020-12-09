# Netty编解码器和handler的调用机制

## 基本说明

1. Netty 的组件设计：Netty 的主要组件有 Channel 、EventLoop、ChannelFuture、ChannelHandler、ChannelPipe 等
2. ChannelHandler 充当了处理入站和出站数据的应用程序逻辑的容器。例如，实现 ChannelInboundHandler 接口（或 ChannelInboundHandlerAdapter ），你就可以接收入站事件和数据，这些数据会被业务逻辑处理。当要给客户端发送响应时，也可以从 ChannelInboundHandler 冲刷数据。业务逻辑通常写在一个或者多个 ChannelInboundHandler 中。ChannelOutboundHandler 原理一样，只不过它是用来处理出站数据的
3. ChannelPipeline 提供了 ChannelHandler 链的容器。以客户端应用程序为例，如果事件的运动方向是从客户端到服务端的，那么我们称这些事件为出站的，即客户端发送给服务端的数据会通过 pipeline 中的一系列ChannelOutboundHandler，并被这些 Handler 处理，反之则称为入站的

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_1.png )



## 编码解码器

1. 当 Netty 发送或者接收一个消息的时候，就将会发生一次数据转换。入站消息会被解码：从字节转换为另一种格式（比如 Java 对象）；如果是出站消息，它会被编码成字节
2. Netty 提供一系列实用的编解码器，他们都实现了 ChannelInboundHandler 或者 ChannelOutboundHandler 接口。在这些类中，channelRead 方法已经被重写了。以入站为例，对于每个从入站 Channel 读取的消息，这个方法会被调用。随后，它将调用由解码器所提供的 decode() 方法进行解码，并将已经解码的字节转发给 ChannelPipeline 中的下一个 ChannelInboundHandler



## 解码器-ByteToMessageDecoder

1. 关系继承图

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_2.png )

2. 由于不可能知道远程节点是否会一次性发送一个完整的信息，tcp有可能出现粘包拆包的问题，这个类会对入站数据进行缓冲，直到它准备好被处理

3. 一个关于ByteToMessageDecoder实例分析

```java
public class ToIntegerDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() >= 4) {
            out.add(in.readInt());
        }
    }
}
```

说明：

- 这个例子，每次入站从 ByteBuf 中读取4字节，将其解码为一个 int ，然后将它添加到下一个 List 中。当没有更多元素可以被添加到该 List 中时，它的内容将会被发送给下一个 ChannelInboundHandler 。int 在被添加到 List 中时，会被自动装箱为 Integer 。在调用 readInt() 方法前必须验证所输入的 ByteBuf 是否具有足够的数据
- decode 执行分析图 [示意图]

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_3.png )



## Netty 的 handler 链的调用机制

1. 实例要求:  
   - 使用自定义的编码器和解码器来说明 Netty 的 handler  调用机制
   - 客户端发送long -> 服务器
   - 服务端发送long -> 客户端

2. 案例演示

   MyByteToLongDecoder

   ```java
   package com.homan.netty.handler;
   
   import io.netty.buffer.ByteBuf;
   import io.netty.channel.ChannelHandlerContext;
   import io.netty.handler.codec.ByteToMessageDecoder;
   
   import java.util.List;
   
   /**
    * 字节解码成长整型
    *
    * @author Homan
    */
   public class MyByteToLongDecoder extends ByteToMessageDecoder {
       /**
        *
        * decode 会根据接收的数据，被调用多次, 直到确定没有新的元素被添加到list
        * , 或者是ByteBuf 没有更多的可读字节为止
        * 如果list out 不为空，就会将list的内容传递给下一个 channelinboundhandler处理, 该处理器的方法也会被调用多次
        *
        * @param ctx 上下文对象
        * @param in 入站的 ByteBuf
        * @param out List 集合，将解码后的数据传给下一个handler
        * @throws Exception
        */
       @Override
       protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
           System.out.println("MyByteToLongDecoder 被调用");
           // 因为 long 8个字节, 需要判断有8个字节，才能读取一个long
           if(in.readableBytes() >= 8) {
               out.add(in.readLong());
           }
       }
   }
   ```

   MyLongToByteEncoder

   ```java
   package com.homan.netty.handler;
   
   import io.netty.buffer.ByteBuf;
   import io.netty.channel.ChannelHandlerContext;
   import io.netty.handler.codec.MessageToByteEncoder;
   
   /**
    * 长整型编码成字节
    *
    * @author Homan
    */
   public class MyLongToByteEncoder extends MessageToByteEncoder<Long> {
       /**
        * 编码方法
        * @param ctx
        * @param msg
        * @param out
        * @throws Exception
        */
       @Override
       protected void encode(ChannelHandlerContext ctx, Long msg, ByteBuf out) throws Exception {
           System.out.println("MyLongToByteEncoder encode 被调用");
           System.out.println("msg=" + msg);
           out.writeLong(msg);
       }
   }
   ```

   HandlerServer

```java
package com.homan.netty.handler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * handler链的调用机制 -- 服务器端
 *
 * @author Homan
 */
public class HandlerServer {
    public static void main(String[] args) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // 自定义一个初始化类
                    .childHandler(new HandlerServerInitializer());
            ChannelFuture channelFuture = serverBootstrap.bind(7000).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

```

HandlerServerInitializer

```java
package com.homan.netty.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * handler链的调用机制 -- 服务器端 -- 初始化器
 *
 * @author Homan
 */
public class HandlerServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 入站的handler进行解码 MyByteToLongDecoder
        //pipeline.addLast(new MyByteToLongDecoder());
        pipeline.addLast(new MyByteToLongDecoder2());
        // 出站的handler进行编码
        pipeline.addLast(new MyLongToByteEncoder());
        // 自定义的handler 处理业务逻辑
        pipeline.addLast(new HandlerServerHandler());
        System.out.println("initChannel");
    }
}
```

HandlerServerHandler

```java
package com.homan.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * handler链的调用机制 -- 服务器端 Handler
 *
 * @author Homan
 */
public class HandlerServerHandler extends SimpleChannelInboundHandler<Long> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Long msg) throws Exception {
        System.out.println("从客户端" + ctx.channel().remoteAddress() + " 读取到long " + msg);
        // 给客户端发送一个long
        ctx.writeAndFlush(98765L);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```

HandlerClient

```java
package com.homan.netty.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * handler链的调用机制 -- 客户端
 *
 * @author Homan
 */
public class HandlerClient {
    public static void main(String[] args)  throws  Exception{
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    // 自定义一个初始化类
                    .handler(new HandlerClientInitializer());

            ChannelFuture channelFuture = bootstrap.connect("localhost", 7000).sync();
            channelFuture.channel().closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }
}
```

HandlerClientInitializer

```java
package com.homan.netty.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * handler链的调用机制 -- 客户端 -- 初始化器
 *
 * @author Homan
 */
public class HandlerClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 加入一个出站的handler 对数据进行一个编码
        pipeline.addLast(new MyLongToByteEncoder());
        // 这时一个入站的解码器(入站handler )
        //pipeline.addLast(new MyByteToLongDecoder());
        pipeline.addLast(new MyByteToLongDecoder2());
        // 加入一个自定义的handler，处理业务
        pipeline.addLast(new HandlerClientHandler());
    }
}
```

HandlerClientHandler

```java
package com.homan.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * handler链的调用机制 -- 客户端 Handler
 *
 * @author Homan
 */
public class HandlerClientHandler extends SimpleChannelInboundHandler<Long> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Long msg) throws Exception {
        System.out.println("服务器的ip=" + ctx.channel().remoteAddress());
        System.out.println("收到服务器消息=" + msg);
    }

    /**
     * 重写channelActive 发送数据
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("MyClientHandler 发送数据");
        //ctx.writeAndFlush(Unpooled.copiedBuffer(""))
        // 发送的是一个long
        ctx.writeAndFlush(123456L);

        //分析
        //1. "abcdabcdabcdabcd" 是 16个字节
        //2. 该处理器的前一个handler 是  MyLongToByteEncoder
        //3. MyLongToByteEncoder 父类  MessageToByteEncoder
        //4. 父类  MessageToByteEncoder
        /*
         public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = null;
        try {
            if (acceptOutboundMessage(msg)) { //判断当前msg 是不是应该处理的类型，如果是就处理，不是就跳过encode
                @SuppressWarnings("unchecked")
                I cast = (I) msg;
                buf = allocateBuffer(ctx, cast, preferDirect);
                try {
                    encode(ctx, cast, buf);
                } finally {
                    ReferenceCountUtil.release(cast);
                }

                if (buf.isReadable()) {
                    ctx.write(buf, promise);
                } else {
                    buf.release();
                    ctx.write(Unpooled.EMPTY_BUFFER, promise);
                }
                buf = null;
            } else {
                ctx.write(msg, promise);
            }
        }
        4. 因此我们编写 Encoder 是要注意传入的数据类型和处理的数据类型一致
        */
       // ctx.writeAndFlush(Unpooled.copiedBuffer("abcdabcdabcdabcd",CharsetUtil.UTF_8));
    }
}
```

**运行结果图**

客户端

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_6.png )

服务器端

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_7.png )



3. 结论

- 不论解码器 handler  还是 编码器 handler 即接收的消息类型必须与待处理的消息类型一致，否则该 handler 不会被执行

- 在解码器进行数据解码时，需要判断缓存区(ByteBuf)的数据是否足够 ，否则接收到的结果会期望结果可能不一致



## 解码器-ReplayingDecoder

1. `public abstract class ReplayingDecoder<S> extends ByteToMessageDecoder`
2. `ReplayingDecoder`扩展了`ByteToMessageDecoder`类，使用这个类，我们不必调用`readableBytes()`方法。参数`S`指定了用户状态管理的类型，其中`Void`代表不需要状态管理
3. 应用实例：使用`ReplayingDecoder` 编写解码器，对前面的案例进行简化

MyByteToLongDecoder

```java
package com.homan.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

/**
 * 字节解码成长整型
 *
 * @author Homan
 */
public class MyByteToLongDecoder2 extends ReplayingDecoder<Void> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        System.out.println("MyByteToLongDecoder2 被调用");
        // 在 ReplayingDecoder 不需要判断数据是否足够读取，内部会进行处理判断
        out.add(in.readLong());
    }
}
```

**运行结果图**

客户端

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_4.png )

服务器端

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_5.png )



4. `ReplayingDecoder`使用方便，但它也有一些局限性：
   - 并不是所有的 `ByteBuf` 操作都被支持，如果调用了一个不被支持的方法，将会抛出一个 `UnsupportedOperationException`。
   - `ReplayingDecoder` 在某些情况下可能稍慢于 `ByteToMessageDecoder`，例如网络缓慢并且消息格式复杂时，消息会被拆成了多个碎片，速度变慢



## 其它编解码器

1. `LineBasedFrameDecoder`：这个类在Netty内部也有使用，它使用行尾控制字符（`\n`或者`\r\n`）作为分隔符来解析数据。
2. `DelimiterBasedFrameDecoder`：使用自定义的特殊字符作为消息的分隔符。
3. `HttpObjectDecoder`：一个HTTP数据的解码器
4. `LengthFieldBasedFrameDecoder`：通过指定长度来标识整包消息，这样就可以自动的处理黏包和半包消息。

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9_8.png )















