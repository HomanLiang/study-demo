package com.homan.netty.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

/**
 * ByteBuf 案例2
 *
 * @author Homan
 */
public class NettyByteBuf02 {
    public static void main(String[] args) {
        // 创建ByteBuf
        ByteBuf byteBuf = Unpooled.copiedBuffer("hello,world!", Charset.forName("utf-8"));
        //使用相关的方法
        if(byteBuf.hasArray()) {
            byte[] content = byteBuf.array();
            //将 content 转成字符串
            System.out.println(new String(content, Charset.forName("utf-8")));
            System.out.println("byteBuf=" + byteBuf);
            // 0
            System.out.println(byteBuf.arrayOffset());
            // 0
            System.out.println(byteBuf.readerIndex());
            // 12
            System.out.println(byteBuf.writerIndex());
            // 36
            System.out.println(byteBuf.capacity());
            // System.out.println(byteBuf.readByte());
            // 104
            System.out.println(byteBuf.getByte(0));
            //可读的字节数  12
            int len = byteBuf.readableBytes();
            System.out.println("len=" + len);
            //使用for取出各个字节
            for(int i = 0; i < len; i++) {
                System.out.println((char) byteBuf.getByte(i));
            }
            //按照某个范围读取
            System.out.println(byteBuf.getCharSequence(0, 4, Charset.forName("utf-8")));
            System.out.println(byteBuf.getCharSequence(4, 6, Charset.forName("utf-8")));
        }
    }
}
