[toc]





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



## Selector(选择器)

### 基本介绍

1. Java 的 NIO，用非阻塞的 IO 方式。可以用一个线程，处理多个的客户端连接，就会使用到 Selector（选择器）
2. Selector 能够检测多个注册的通道上是否有事件发生（注意：多个 Channel 以事件的方式可以注册到同一个 Selector），如果有事件发生，便获取事件然后针对每个事件进行相应的处理。这样就可以只用一个单线程去管理多个通道，也就是管理多个连接和请求。
3. 只有在连接通道真正有读写事件发生时，才会进行读写，就大大地减少了系统开销，并且不必为每个连接都创建一个线程，不用去维护多个线程
4. 避免了多线程之间的上下文切换导致的开销



### Selector 示意图和特点说明

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/12.png )

说明如下：

1. Netty 的 IO 线程 NioEventLoop 聚合了 Selector（选择器，也叫多路复用器），可以同时并发处理成百上千个客户端连接。
2. 当线程从某客户端 Socket 通道进行读写数据时，若没有数据可用时，该线程可以进行其他任务。
3. 线程通常将非阻塞 IO 的空闲时间用于在其他通道上执行 IO 操作，所以单独的线程可以管理多个输入和输出通道。
4. 由于读写操作都是非阻塞的，这就可以充分提升 IO 线程的运行效率，避免由于频繁 I/O 阻塞导致的线程挂起。
5. 一个 I/O 线程可以并发处理 N 个客户端连接和读写操作，这从根本上解决了传统同步阻塞 I/O 一连接一线程模型，架构的性能、弹性伸缩能力和可靠性都得到了极大的提升。



### Selector 类相关方法

Selector 类是一个抽象类，常用方法和说明如下：

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/13.png )



### 注意事项

1. NIO 中的 ServerSocketChannel 功能类似 ServerSocket，SocketChannel 功能类似 Socket
2. Selector 相关方法说明：
   - select()； //阻塞
   - select(1000)； // 阻塞 1000 毫秒，在 1000 毫秒后返回
   - wakeup(); // 唤醒 selector
   - selectNow();  // 不阻塞，立马返还



## NIO 原理分析图

Selector、SelectionKey、ServerSocketChannel 和 SocketChannel 关系梳理图

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/14.png )

对上图的说明：

1. 当客户端连接时，会通过 ServerSocketChannel 得到 SocketChannel
2. Selector 进行监听 select 方法，返回有事件发生的通道的个数。
3. 将 SocketChannel 注册到 Selector 上，register(Selector sel, int ops)，一个 Selector 上可以注册多个 SocketChannel
4. 注册后返回一个 SelectionKey，会和该 Selector 关联（集合）
5. 进一步得到各个 SelectionKey（有事件发生）
6. 在通过 SelectionKey 反向获取 SocketChannel，方法 channel() ，得到Channel，完成业务处理



## NIO 快速入门

案例要求：

1. 编写一个 NIO 入门案例，实现服务器端和客户端之间的数据简单通讯（非阻塞）
2. 目的：理解 NIO 非阻塞网络编程机制

代码演示：

NIOServer

```java
package com.homan.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO 服务器端
 *
 * @author Homan
 */
public class NIOServer {
    public static void main(String[] args) throws Exception{
        //创建ServerSocketChannel -> ServerSocket
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //得到一个Selecor对象
        Selector selector = Selector.open();
        //绑定一个端口6666, 在服务器端监听
        serverSocketChannel.socket().bind(new InetSocketAddress(6666));
        //设置为非阻塞
        serverSocketChannel.configureBlocking(false);
        //把 serverSocketChannel 注册到  selector 关心 事件为 OP_ACCEPT
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("注册后的selectionkey 数量=" + selector.keys().size());

        //循环等待客户端连接
        while (true) {
            //这里我们等待1秒，如果没有事件发生, 返回
            if(selector.select(1000) == 0) {
                //没有事件发生
                System.out.println("服务器等待了1秒，无连接");
                continue;
            }
            //如果返回的>0, 就获取到相关的 selectionKey集合
            //1.如果返回的>0， 表示已经获取到关注的事件
            //2. selector.selectedKeys() 返回关注事件的集合
            //   通过 selectionKeys 反向获取通道
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            System.out.println("selectionKeys 数量 = " + selectionKeys.size());
            //遍历 Set<SelectionKey>, 使用迭代器遍历
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

            while (keyIterator.hasNext()) {
                //获取到SelectionKey
                SelectionKey key = keyIterator.next();
                //根据key 对应的通道发生的事件做相应处理
                if(key.isAcceptable()) {
                    //如果是 OP_ACCEPT, 有新的客户端连接
                    //该该客户端生成一个 SocketChannel
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    System.out.println("客户端连接成功 生成了一个 socketChannel " + socketChannel.hashCode());
                    //将  SocketChannel 设置为非阻塞
                    socketChannel.configureBlocking(false);
                    //将socketChannel 注册到selector, 关注事件为 OP_READ， 同时给socketChannel
                    //关联一个Buffer
                    socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

                    System.out.println("客户端连接后 ，注册的selectionkey 数量=" + selector.keys().size());
                }
                if(key.isReadable()) {
                    //发生 OP_READ
                    //通过key 反向获取到对应channel
                    SocketChannel channel = (SocketChannel)key.channel();

                    //获取到该channel关联的buffer
                    ByteBuffer buffer = (ByteBuffer)key.attachment();
                    channel.read(buffer);
                    System.out.println("form 客户端 " + new String(buffer.array()));
                }
                //手动从集合中移动当前的selectionKey, 防止重复操作
                keyIterator.remove();
            }
        }
    }
}
```

NIOClient

```java
package com.homan.nio;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * NIO 客户端
 *
 * @author Homan
 */
public class NIOClient {
    public static void main(String[] args) throws Exception{
        // 得到一个网络通道
        SocketChannel socketChannel = SocketChannel.open();
        // 设置非阻塞
        socketChannel.configureBlocking(false);
        // 提供服务器端的ip 和 端口
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 6666);
        // 连接服务器
        if (!socketChannel.connect(inetSocketAddress)) {
            while (!socketChannel.finishConnect()) {
                System.out.println("因为连接需要时间，客户端不会阻塞，可以做其它工作..");
            }
        }
        // ...如果连接成功，就发送数据
        String str = "hello Homan";
        //Wraps a byte array into a buffer
        ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());
        // 发送数据，将 buffer 数据写入 channel
        socketChannel.write(buffer);

        System.in.read();
    }
}
```



## SelectionKey

1. SelectionKey，表示 Selector 和网络通道的注册关系，共四种：

   - OP_ACCEPT: 有新的网络连接可以 accept，值为 16
   - OP_CONNECT: 代表连接已经建立，值为 8
   - OP_READ：代表读操作，值为 1
   - OP_WRITE:  代表写操作，值为 4

2. Selection 相关方法

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/15.png )



## ServerSocketChannel

1. ServerSocketChannel 在服务器端监听新的客户端 Socket 连接

2. 相关方法如下：

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/16.png )



## SocketChannel

1. SocketChannel，网络 IO 通道，具体负责进行读写操作。NIO 把缓冲区的数据写入通道，或者把通道里的数据读到缓冲区。

2. 相关方法如下：

   ![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/17.png )



## NIO 实例 -- 群聊系统

实例要求：

1. 编写一个 NIO 群聊系统，实现服务器端和客户端之间的数据简单通讯（非阻塞）
2. 实现多人群聊
3. 服务器端：可以监测用户上线，离线，并实现消息转发功能
4. 客户端：通过 channel 可以无阻塞发送消息给其它所有用户，同时可以接受其它用户发送的消息（由服务器转发得到）
5. 目的：进一步理解 NIO 非阻塞网络编程机制

实例代码：

GroupChatServer

```

package com.homan.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * 群聊系统 -- 服务器端
 *
 * @author Homan
 */
public class GroupChatServer {
    private Selector selector;
    private ServerSocketChannel listenChannel;
    private static final int PORT = 6667;

    /**
     * 初始化工作
     */
    public GroupChatServer() {
        try {
            // 得到选择器
            selector = Selector.open();
            //ServerSocketChannel
            listenChannel =  ServerSocketChannel.open();
            // 绑定端口
            listenChannel.socket().bind(new InetSocketAddress(PORT));
            // 设置非阻塞模式
            listenChannel.configureBlocking(false);
            // 将该listenChannel 注册到selector
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听
     */
    public void listen() {
        System.out.println("监听线程: " + Thread.currentThread().getName());
        try {
            // 循环处理
            while (true) {
                int count = selector.select();
                if(count > 0) {
                    // 有事件处理
                    // 遍历得到selectionKey 集合
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        // 取出selectionkey
                        SelectionKey key = iterator.next();
                        //监听到accept
                        if(key.isAcceptable()) {
                            SocketChannel sc = listenChannel.accept();
                            sc.configureBlocking(false);
                            //将该 sc 注册到seletor
                            sc.register(selector, SelectionKey.OP_READ);
                            //提示
                            System.out.println(sc.getRemoteAddress() + " 上线 ");
                        }
                        // 通道发送read事件，即通道是可读的状态
                        if(key.isReadable()) {
                            //处理读 (专门写方法..)
                            readData(key);
                        }
                        //当前的key 删除，防止重复处理
                        iterator.remove();
                    }
                } else {
                    System.out.println("等待....");
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            //发生异常处理....
        }
    }

    /**
     * 读取客户端消息
     * @param key
     */
    private void readData(SelectionKey key) {
        // 取到关联的channle
        SocketChannel channel = null;

        try {
            // 得到channel
            channel = (SocketChannel) key.channel();
            // 创建buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            int count = channel.read(buffer);
            // 根据count的值做处理
            if(count > 0) {
                // 把缓存区的数据转成字符串
                String msg = new String(buffer.array());
                // 输出该消息
                System.out.println("form 客户端: " + msg);
                // 向其它的客户端转发消息(去掉自己), 专门写一个方法来处理
                sendInfoToOtherClients(msg, channel);
            }
        }catch (IOException e) {
            try {
                System.out.println(channel.getRemoteAddress() + " 离线了..");
                //取消注册
                key.cancel();
                //关闭通道
                channel.close();
            }catch (IOException e2) {
                e2.printStackTrace();;
            }
        }
    }

    /**
     * 转发消息给其它客户(通道)
     * @param msg
     * @param self
     * @throws IOException
     */
    private void sendInfoToOtherClients(String msg, SocketChannel self) throws  IOException{
        System.out.println("服务器转发消息中...");
        System.out.println("服务器转发数据给客户端线程: " + Thread.currentThread().getName());
        // 遍历 所有注册到selector 上的 SocketChannel,并排除 self
        for(SelectionKey key: selector.keys()) {
            // 通过 key  取出对应的 SocketChannel
            Channel targetChannel = key.channel();
            // 排除自己
            if(targetChannel instanceof  SocketChannel && targetChannel != self) {
                // 转型
                SocketChannel dest = (SocketChannel)targetChannel;
                // 将msg 存储到buffer
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                // 将buffer 的数据写入 通道
                dest.write(buffer);
            }
        }
    }

    public static void main(String[] args) {
        // 创建服务器对象
        GroupChatServer groupChatServer = new GroupChatServer();
        groupChatServer.listen();
    }
}
```

GroupChatClient

```java
package com.homan.nio.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

/**
 * 群聊系统 客户端
 *
 * @author Homan
 */
public class GroupChatClient {
    /**
     * 服务器的ip
     */
    private final String HOST = "127.0.0.1";
    /**
     * 服务器端口
     */
    private final int PORT = 6667;
    private Selector selector;
    private SocketChannel socketChannel;
    private String username;

    /**
     * 构造器, 完成初始化工作
     * @throws IOException
     */
    public GroupChatClient() throws IOException {
        selector = Selector.open();
        // 连接服务器
        socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", PORT));
        // 设置非阻塞
        socketChannel.configureBlocking(false);
        // 将channel 注册到selector
        socketChannel.register(selector, SelectionKey.OP_READ);
        // 得到username
        username = socketChannel.getLocalAddress().toString().substring(1);
        System.out.println(username + " is ok...");

    }

    /**
     * 向服务器发送消息
     * @param info
     */
    public void sendInfo(String info) {
        info = username + " 说：" + info;
        try {
            socketChannel.write(ByteBuffer.wrap(info.getBytes()));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取从服务器端回复的消息
     */
    public void readInfo() {
        try {
            int readChannels = selector.select();
            if(readChannels > 0) {
                //有可以用的通道
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if(key.isReadable()) {
                        //得到相关的通道
                       SocketChannel sc = (SocketChannel) key.channel();
                       //得到一个Buffer
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        //读取
                        sc.read(buffer);
                        //把读到的缓冲区的数据转成字符串
                        String msg = new String(buffer.array());
                        System.out.println(msg.trim());
                    }
                }
                //删除当前的selectionKey, 防止重复操作
                iterator.remove();
            } else {
                //System.out.println("没有可以用的通道...");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        //启动我们客户端
        GroupChatClient chatClient = new GroupChatClient();
        //启动一个线程, 每个3秒，读取从服务器发送数据
        new Thread() {
            @Override
            public void run() {

                while (true) {
                    chatClient.readInfo();
                    try {
                        Thread.sleep(3000);
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        //发送数据给服务器端
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String s = scanner.nextLine();
            chatClient.sendInfo(s);
        }
    }
}
```

运行结果：

服务器端：

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/18.png )

客户端1（发送”111“）：

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/19.png )

客户端2（收到服务器端转发的”111“）

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/20.png )

## Java AIO 基本介绍

- JDK 7 引入了 Asynchronous I/O，即 AIO。在进行 I/O 编程中，常用到两种模式：Reactor和 Proactor。Java 的 NIO 就是 Reactor，当有事件触发时，服务器端得到通知，进行相应的处理

- AIO 即 NIO2.0，叫做异步不阻塞的 IO。AIO 引入异步通道的概念，采用了 Proactor 模式，简化了程序编写，有效的请求才启动线程，它的特点是先由操作系统完成后才通知服务端程序启动线程去处理，一般适用于连接数较多且连接时间较长的应用

- 目前 AIO 还没有广泛应用，Netty 也是基于NIO, 而不是AIO， 因此我们就不详解AIO了，有兴趣的同学可以参考 <<Java新一代网络编程模型AIO原理及Linux系统AIO介绍>>  http://www.52im.net/thread-306-1-1.html  



## BIO、NIO、AIO对比表

![]( https://raw.githubusercontent.com/HomanLiang/study-demo/main/netty-demo/document/pic/32.png )

举例说明 

- 同步阻塞：到理发店理发，就一直等理发师，直到轮到自己理发。
- 同步非阻塞：到理发店理发，发现前面有其它人理发，给理发师说下，先干其他事情，一会过来看是否轮到自己.
- 异步非阻塞：给理发师打电话，让理发师上门服务，自己干其它事情，理发师自己来家给你理发

























