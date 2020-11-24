# Java NIO 编程

## Java NIO 基本介绍

1. Java NIO 全称 java non-blocking IO，是指 JDK 提供的新 API。从 JDK 1.4 开始，Java 提供了一系列改进的输入/输出的新特性，被统称为 NIO(即 New IO)，是同步非阻塞的
2. NIO 相关类都被放在 java.nio 包及子包下，并且对原 java.io 包中的很多类进行改写。
3. NIO 有三大核心部分：**Channel(通道)**，**Buffer(缓冲区)**，**Selector(选择器)**
4. NIO 是面向缓冲区，或者面向块编程的。数据读取到一个它稍后处理的缓冲区，需要时可在缓冲区中前后移动，这就增加了处理过程中的灵活性，使用它可以提供非阻塞式的高伸缩性网络
5. Java NIO 的非阻塞模式，使一个线程从某通道发送请求或者读取数据，但是它能得到目前可用的数据，如果目前没有数据可用时，就什么都不会获取，而不是保持线程阻塞，所以直至数据变得可以读取之前，该线程可以继续做其他得事情。非阻塞写也是如此，一个线程请求写入一些数据到某通道，但不需要等待它完全写入，这个线程同时可以去做别的事情。
6. 通俗理解：NIO 是可以做到一个线程处理多个操作。假设有10000个请求过来，根据实际情况，可以分配50或者100个线程来处理。不像之前的阻塞 IO 那样，非得分配10000个。
7. HTTP 2.0 使用了多路复用的技术，做到同一个连接并发处理多个请求，而且并发请求的数量比 HTTP 1.1 大了好几个数量级



## NIO 和 BIO 的比较

1. BIO 以流的方式处理数据，而 NIO 以块的方式处理数据，块 I/O 的效率比流 I/O 高很多。
2. BIO 是阻塞的，NIO 则是非阻塞的
3. BIO 基于字节流和字符流进行操作，而 NIO 基于 Channel（通道）和 Buffer（缓冲区）进行操作，数据总是从通道读取到缓冲区中，或者从缓冲区写入到通道中。 Selector（选择器）用于监听多个通道的事件（比如：连接请求，数据到达等），因此使用单个线程就可以监听多个客户端通道



## NIO 三大核心原理示意图

Selector、Channel 和 Buffer 的关系图（简单版）：

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/4.png )

关系图说明：

1. 每个 Channel 都会对应一个 Buffer
2. Selector 对应一个线程，一个线程对应多个 Channel（连接）
3. 该图反应了有三个 Channel 注册到该 Selector 程序
4. 程序切换到哪个 Channel 是由事件决定的，Event 就是一个重要的概念
5. Selector 会根据不同的事件，在各个通道上切换
6. Buffer 就是一个内存块，底层是有一个数组
7. 数据的读取写入是通过 Buffer，这个和 BIO，BIO 中要么是输入流，或者是输出流，不能双向，但是 NIO 的 Buffer 是可以读也可以写，需要 flip 方法切换。Channel 是双向的，可以返回底层操作系统的情况，比如 Linux，底层的操作系统通道就是双向的。



## 缓冲区（Buffer）

### 基本介绍

缓冲区（Buffer）：缓冲区本质上是一个可以读写数据的内存块，可以理解成是一个容器对象（含数组），该对象提供了一组方法，可以更轻松地使用内存块，缓冲区对象内置了一些机制，能够跟踪和记录缓冲区的状态变化情况。Channel 提供从文件、网络读取数据的渠道，但是读取或者写入的数据都必须经由 Buffer，如图：

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/5.png )

### Buffer 类及其子类

1. 在 NIO 中，Buffer 是一个顶层父类，它是一个抽象类，类的层级关系图：

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/6.png )

   常用 Buffer 子类一览：

   - ByteBuffer：存储字节数据到缓冲区
   - ShortBuffer：存储字符串数据到缓冲区
   - CharBuffer：存储字符数据到缓冲区
   - IntBuffer：存储整型数据到缓冲区
   - LongBuffer：存储长整型数据到缓冲区
   - DoubleBuffer：存储小数到缓冲区
   - FloatBuffer：存储小数到缓冲区

2. Buffer 类定义了所有的缓冲区都具有的四个属性来提供关于其所有包含的数据元素的信息：

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/7.png )

3. Buffer 类相关方法一览：

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/8.png )

4. Buffer 简单示例：

   ```java
   package com.homan.nio.buffer;
   
   import java.nio.IntBuffer;
   
   /**
    * Buffer 例子
    *
    * @author Homan
    */
   public class BasicBuffer {
       public static void main(String[] args) {
           // 举例说明Buffer 的使用 (简单说明)
           // 创建一个Buffer, 大小为 5, 即可以存放5个int
           IntBuffer intBuffer = IntBuffer.allocate(5);
           //向buffer 存放数据
           for(int i = 0; i < intBuffer.capacity(); i++) {
               intBuffer.put( i * 2);
           }
           // 如何从buffer读取数据
           // 将buffer转换，读写切换(!!!)
           /*
           public final Buffer flip() {
               limit = position; //读数据不能超过5
               position = 0;
               mark = -1;
               return this;
           }
            */
           intBuffer.flip();
           // 1,2
           intBuffer.position(1);
           System.out.println(intBuffer.get());
           intBuffer.limit(3);
           while (intBuffer.hasRemaining()) {
               System.out.println(intBuffer.get());
           }
       }
   }
   
   ```

   

### ByteBuffer

从前面可以看出对于 Java 中的基本数据类型（Boolean 除外），都有一个 Buffer 类型与之相对应，最常用的自然是 ByteBuffer 类（二进制数据），该类的主要方法

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/9.png )



## 通道（Channel）

### 基本介绍

1. NIO 的通道类似于流，但有些区别如下：
   - 通道可以同时进行读写，而流只能读或者只能写
   - 通道可以实现异步读写数据
   - 通道可以从缓冲读数据，也可以写数据到缓冲
2. Channel 在 NIO 中是一个接口 `public interface Channel extends Closeable{}`

3. 常用的 Channel 类有：

   - FileChannel -- 用于文件的数据读写
   - DatagramChannel -- 用于 UDP 的数据读写
   - ServerSocketChannel -- 类似 ServerSocket -- 用于 TCP 的数据读写
   - SocketChannel -- 类似 Socket -- 用于 TCP 的数据读写

4. 示意图：

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/10.png )



### FileChannel 类

FileChannel 主要用来对本地文件进行 IO 操作，常见方法有：

- `public int read(ByteBuffer dst)`，从通道读取数据并放到缓冲区中
- `public int write(ByteBuffer src)`，把缓冲区的数据写到通道中
- `public long transferFrom(ReadableByteChannel src, long count)`，从目标通道中复制数据到当前通道
- `public long transferTo(long position, long count, WritableByteChannel targe)`，把数据从当前通道复制给目标通道



### 应用实例1 -- 本地文件写入数据

实例要求：

1. 使用前面学习后的 ByteBuffer(缓冲) 和 FileChannel(通道)，将 ”Hello Homan“ 写入文件中
2. 文件不存在就创建

代码示例：

```java
package com.homan.nio.channel;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 本地文件写入数据
 *
 * @author Homan
 */
public class FileChannel01 {
    public static void main(String[] args) throws Exception{
        String str = "Hello Homan";
        //创建一个输出流->channel
        FileOutputStream fileOutputStream = new FileOutputStream("d:\\file01.txt");
        //通过 fileOutputStream 获取 对应的 FileChannel
        //这个 fileChannel 真实 类型是  FileChannelImpl
        FileChannel fileChannel = fileOutputStream.getChannel();
        //创建一个缓冲区 ByteBuffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        //将 str 放入 byteBuffer
        byteBuffer.put(str.getBytes());
        //对byteBuffer 进行flip
        byteBuffer.flip();
        //将byteBuffer 数据写入到 fileChannel
        fileChannel.write(byteBuffer);
        fileOutputStream.close();
    }
}
```



### 应用实例2 -- 本地文件读数据

实例要求：

1. 使用前面学习的 ByteBuffer(缓冲) 和 FileChannel(通道)，将文件中的数据读入到程序，并显示控制台显示
2. 假定文件已经存在

代码演示：

```java
package com.homan.nio.channel;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 本地文件读数据
 *
 * @author Homan
 */
public class FileChannel02 {
    public static void main(String[] args) throws Exception {
        //创建文件的输入流
        File file = new File("d:\\file01.txt");
        FileInputStream fileInputStream = new FileInputStream(file);
        //通过fileInputStream 获取对应的FileChannel -> 实际类型  FileChannelImpl
        FileChannel fileChannel = fileInputStream.getChannel();
        //创建缓冲区
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) file.length());
        //将 通道的数据读入到Buffer
        fileChannel.read(byteBuffer);
        //将byteBuffer 的 字节数据 转成String
        System.out.println(new String(byteBuffer.array()));
        fileInputStream.close();
    }
}
```



### 应用实例3 -- 使用一个 Buffer 完成文件读取、写入

实例要求：

1. 使用 FileChannel(通道) 和方法 read, write，完成文件的拷贝
2. 拷贝一个文本文件

代码演示：

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/11.png )

```java
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
```



### 应用实例4 -- 拷贝文件 transferFrom 方法

实例要求：

1. 使用 FileChannel(通道) 和方法 transferFrom， 完成文件的拷贝
2. 拷贝一张图片

代码演示：

```java
package com.homan.nio.channel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * 拷贝文件 transferFrom 方法
 *
 * @author Homan
 */
public class FileChannel04 {
    public static void main(String[] args)  throws Exception {
        // 创建相关流
        FileInputStream fileInputStream = new FileInputStream("d:\\a.png");
        FileOutputStream fileOutputStream = new FileOutputStream("d:\\a2.png");
        // 获取各个流对应的 FileChannel
        FileChannel sourceCh = fileInputStream.getChannel();
        FileChannel destCh = fileOutputStream.getChannel();
        // 使用transferForm完成拷贝
        destCh.transferFrom(sourceCh,0,sourceCh.size());
        // 关闭相关通道和流
        sourceCh.close();
        destCh.close();
        fileInputStream.close();
        fileOutputStream.close();
    }
}
```



### 关于 Buffer 和 Channel 的注意事项和细节

1. ByteBuffer 支持类型化的 put 和 get，put 放入的是什么类型，get 就应该使用相应的数据类型来取出，否则可能有 BufferUnderflowExceptin 异常。

   ```java
   package com.homan.nio.buffer;
   
   import java.nio.ByteBuffer;
   
   /**
    * ByteBuffer 支持类型化的 put 和 get
    *
    * @author Homan
    */
   public class ByteBufferDemo {
       public static void main(String[] args) {
           //创建一个Buffer
           ByteBuffer buffer = ByteBuffer.allocate(64);
           //类型化方式放入数据
           buffer.putInt(100);
           buffer.putLong(9);
           buffer.putChar('尚');
           buffer.putShort((short) 4);
           //取出
           buffer.flip();
   
           System.out.println();
           System.out.println(buffer.getInt());
           System.out.println(buffer.getLong());
           System.out.println(buffer.getChar());
           System.out.println(buffer.getShort());
       }
   }
   ```

   

2. 可以将一个普通 Buffer 转成只读 Buffer

   ```java
   package com.homan.nio.buffer;
   
   import java.nio.ByteBuffer;
   
   /**
    * 将一个普通 Buffer 转成只读 Buffer
    *
    * @author Homan
    */
   public class ReadOnlyBuffer {
       public static void main(String[] args) {
           // 创建一个buffer
           ByteBuffer buffer = ByteBuffer.allocate(64);
           for(int i = 0; i < 64; i++) {
               buffer.put((byte)i);
           }
           // 读取
           buffer.flip();
           // 得到一个只读的Buffer
           ByteBuffer readOnlyBuffer = buffer.asReadOnlyBuffer();
           System.out.println(readOnlyBuffer.getClass());
           // 读取
           while (readOnlyBuffer.hasRemaining()) {
               System.out.println(readOnlyBuffer.get());
           }
           // ReadOnlyBufferException
           readOnlyBuffer.put((byte)100);
       }
   }
   ```

   

3. NIO 还提供了 MappedByteBuffer，可以让文件直接在内存（堆外的内存）中进行修改，而如何同步到文件由 NIO 来完成

   ```java
   package com.homan.nio.buffer;
   
   import java.io.RandomAccessFile;
   import java.nio.MappedByteBuffer;
   import java.nio.channels.FileChannel;
   
   /**
    * MappedByteBuffer 可让文件直接在内存(堆外内存)修改, 操作系统不需要拷贝一次
    *
    * @author Homan
    */
   public class MappedByteBufferDemo {
       public static void main(String[] args) throws Exception {
           RandomAccessFile randomAccessFile = new RandomAccessFile("C:\\Users\\Homan\\Desktop\\1.txt", "rw");
           //获取对应的通道
           FileChannel channel = randomAccessFile.getChannel();
           /**
            * 参数1: FileChannel.MapMode.READ_WRITE 使用的读写模式
            * 参数2： 0 ： 可以直接修改的起始位置
            * 参数3:  5: 是映射到内存的大小(不是索引位置) ,即将 1.txt 的多少个字节映射到内存
            * 可以直接修改的范围就是 0-5
            * 实际类型 DirectByteBuffer
            */
           MappedByteBuffer mappedByteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 5);
   
           mappedByteBuffer.put(0, (byte) 'H');
           mappedByteBuffer.put(3, (byte) '9');
           //IndexOutOfBoundsException
           mappedByteBuffer.put(5, (byte) 'Y');
   
           randomAccessFile.close();
           System.out.println("修改成功~~");
       }
   }
   ```

   

4. NIO 还支持通过多个 Buffer（即 Buffer 数组）完成读写操作，即Scattering 和 Gathering

   ```java
   package com.homan.nio.buffer;
   
   import java.net.InetSocketAddress;
   import java.nio.ByteBuffer;
   import java.nio.channels.ServerSocketChannel;
   import java.nio.channels.SocketChannel;
   import java.util.Arrays;
   
   /**
    * Scattering：将数据写入到buffer时，可以采用buffer数组，依次写入  [分散]
    * Gathering: 从buffer读取数据时，可以采用buffer数组，依次读
    *
    * @author Homan
    */
   public class ScatteringAndGatheringBuffer {
       public static void main(String[] args) throws Exception {
           //使用 ServerSocketChannel 和 SocketChannel 网络
           ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
           InetSocketAddress inetSocketAddress = new InetSocketAddress(7000);
           //绑定端口到socket ，并启动
           serverSocketChannel.socket().bind(inetSocketAddress);
           //创建buffer数组
           ByteBuffer[] byteBuffers = new ByteBuffer[2];
           byteBuffers[0] = ByteBuffer.allocate(5);
           byteBuffers[1] = ByteBuffer.allocate(3);
           //等客户端连接(telnet)
           SocketChannel socketChannel = serverSocketChannel.accept();
           //假定从客户端接收8个字节
           int messageLength = 8;
           //循环的读取
           while (true) {
               int byteRead = 0;
   
               while (byteRead < messageLength ) {
                   long l = socketChannel.read(byteBuffers);
                   //累计读取的字节数
                   byteRead += l;
                   System.out.println("byteRead=" + byteRead);
                   //使用流打印, 看看当前的这个buffer的position 和 limit
                   Arrays.asList(byteBuffers).stream().map(buffer -> "position=" + buffer.position() + ", limit=" + buffer.limit()).forEach(System.out::println);
               }
               //将所有的buffer进行flip
               Arrays.asList(byteBuffers).forEach(buffer -> buffer.flip());
               //将数据读出显示到客户端
               long byteWrite = 0;
               while (byteWrite < messageLength) {
                   long l = socketChannel.write(byteBuffers);
                   byteWrite += l;
               }
               //将所有的buffer 进行clear
               Arrays.asList(byteBuffers).forEach(buffer-> {
                   buffer.clear();
               });
   
               System.out.println("byteRead:=" + byteRead + " byteWrite=" + byteWrite + ", messageLength" + messageLength);
           }
       }
   }
   ```

   