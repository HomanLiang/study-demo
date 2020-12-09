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
