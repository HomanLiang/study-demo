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
        pipeline.addLast(new MyByteToLongDecoder());
//        pipeline.addLast(new MyByteToLongDecoder2());
        // 加入一个自定义的handler，处理业务
        pipeline.addLast(new HandlerClientHandler());
    }
}
