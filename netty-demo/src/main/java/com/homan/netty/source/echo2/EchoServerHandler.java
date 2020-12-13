package com.homan.netty.source.echo2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Handler implementation for the echo server.
 *
 * @author hmliang
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * group 就是充当业务线程池，可以将任务提交到该线程池，这里我们创建了16个线程
     */
    static final EventExecutorGroup group = new DefaultEventExecutorGroup(16);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("EchoServer Handler 的线程是=" + Thread.currentThread().getName());

        //按照原来的方法处理耗时任务
        /*
        //解决方案1 用户程序自定义的普通任务
        ctx.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(5 * 1000);
                    //输出线程名
                    System.out.println("EchoServerHandler execute 线程是=" + Thread.currentThread().getName());
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));

                } catch (Exception ex) {
                    System.out.println("发生异常" + ex.getMessage());
                }
            }
        });

        ctx.channel().eventLoop().execute(new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(5 * 1000);
                    //输出线程名
                    System.out.println("EchoServerHandler execute 线程2是=" + Thread.currentThread().getName());
                    ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));

                } catch (Exception ex) {
                    System.out.println("发生异常" + ex.getMessage());
                }
            }
        });*/

        /*
        //将任务提交到 group线程池
        group.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                //接收客户端信息
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String body = new String(bytes, "UTF-8");
                //休眠10秒
                Thread.sleep(10 * 1000);
                System.out.println("group.submit 的  call 线程是=" + Thread.currentThread().getName());
                ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));
                return null;

            }
        });

        //将任务提交到 group线程池
        group.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                //接收客户端信息
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String body = new String(bytes, "UTF-8");
                //休眠10秒
                Thread.sleep(10 * 1000);
                System.out.println("group.submit 的  call 线程是=" + Thread.currentThread().getName());
                ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));
                return null;

            }
        });


        //将任务提交到 group线程池
        group.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {

                //接收客户端信息
                ByteBuf buf = (ByteBuf) msg;
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String body = new String(bytes, "UTF-8");
                //休眠10秒
                Thread.sleep(10 * 1000);
                System.out.println("group.submit 的  call 线程是=" + Thread.currentThread().getName());
                ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));
                return null;

            }
        });*/

        // 普通方式
        // 接收客户端信息
        ByteBuf buf = (ByteBuf) msg;
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String body = new String(bytes, "UTF-8");
        //休眠10秒
        Thread.sleep(10 * 1000);
        System.out.println("普通调用方式的 线程是=" + Thread.currentThread().getName());
        ctx.writeAndFlush(Unpooled.copiedBuffer("hello, 客户端~(>^ω^<)喵2", CharsetUtil.UTF_8));

        System.out.println("go on ");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        ctx.close();
    }
}
