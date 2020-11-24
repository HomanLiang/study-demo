package com.homan.nio.channel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 使用一个 Buffer 完成文件读取、写入
 *
 * @author Homan
 */
public class FileChannel03 {
    public static void main(String[] args) throws Exception {
        FileInputStream fileInputStream = new FileInputStream("d:\\file01.txt");
        FileChannel fileChannel01 = fileInputStream.getChannel();

        FileOutputStream fileOutputStream = new FileOutputStream("d:\\file02.txt");
        FileChannel fileChannel02 = fileOutputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        // 循环读取
        while (true) {
            // 这里有一个重要的操作，一定不要忘了
            /*
             public final Buffer clear() {
                position = 0;
                limit = capacity;
                mark = -1;
                return this;
            }
             */
            // 清空buffer
            byteBuffer.clear();
            int read = fileChannel01.read(byteBuffer);
            System.out.println("read =" + read);
            // 表示读完
            if(read == -1) {
                break;
            }
            // 将buffer 中的数据写入到 fileChannel02 -- file02.txt
            byteBuffer.flip();
            fileChannel02.write(byteBuffer);
        }
        // 关闭相关的流
        fileInputStream.close();
        fileOutputStream.close();
    }
}
