package com.homan.netty.tcp2;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * TCP 粘包和拆包解决方案--客户端初始化器
 *
 * @author Homan
 */
public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 解码器
        pipeline.addLast(new MyMessageDecoder());
        // 编码器
        pipeline.addLast(new MyMessageEncoder());
        pipeline.addLast(new TcpServerHandler());
    }
}
