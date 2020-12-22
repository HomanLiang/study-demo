package com.homan.netty.dubbo.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.Callable;

/**
 * 客户端处理器
 *
 * @author Homan
 */
public class DubboClientHandler extends ChannelInboundHandlerAdapter implements Callable {

    /**
     * 上下文
     */
    private ChannelHandlerContext context;
    /**
     * 返回的结果
     */
    private String result;
    /**
     * 客户端调用方法时，传入的参数
     */
    private String para;

    /**
     * 与服务器的连接创建后，就会被调用, 这个方法是第一个被调用(1)
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(" channelActive 被调用  ");
        // 因为我们在其它方法会使用到 ctx
        context = ctx;
    }

    /**
     * 收到服务器的数据后，调用方法 (4)
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public synchronized void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println(" channelRead 被调用  ");
        result = msg.toString();
        // 唤醒等待的线程
        notify();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    /**
     * 被代理对象调用, 发送数据给服务器，-> wait -> 等待被唤醒(channelRead) -> 返回结果 (3)-》5
     *
     * @return
     * @throws Exception
     */
    @Override
    public synchronized Object call() throws Exception {
        System.out.println(" call1 被调用  ");
        context.writeAndFlush(para);
        // 等待channelRead 方法获取到服务器的结果后，唤醒
        wait();
        System.out.println(" call2 被调用  ");
        // 服务方返回的结果
        return result;

    }

    /**
     * (2)
     *
     * @param para
     */
    void setPara(String para) {
        System.out.println(" setPara  ");
        this.para = para;
    }
}
