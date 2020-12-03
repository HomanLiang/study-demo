package com.homan.netty.simple;

import io.netty.util.NettyRuntime;

/**
 * 有效处理器数量
 * @author Homan
 */
public class Test {
    public static void main(String[] args) {
        System.out.println(NettyRuntime.availableProcessors());
    }
}
