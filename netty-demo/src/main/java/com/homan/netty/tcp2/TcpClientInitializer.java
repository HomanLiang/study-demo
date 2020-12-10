package com.homan.netty.tcp2;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * TCP 粘包和拆包解决方案--服务器端初始化器
 *
 * @author Homan
 */
public class TcpClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 加入编码器
        pipeline.addLast(new MyMessageEncoder());
        // 加入解码器
        pipeline.addLast(new MyMessageDecoder());
        pipeline.addLast(new TcpClientHandler());
    }
}
