package com.homan.netty.dubbo.provider;

import com.homan.netty.dubbo.netty.DubboServer;

/**
 * ServerBootstrap 会启动一个服务提供者，就是 NettyServer
 *
 * @author Homan
 */
public class DubboServerBootstrap {
    public static void main(String[] args) {
        // 代码代填..
        DubboServer.startServer("127.0.0.1", 7000);
    }
}
