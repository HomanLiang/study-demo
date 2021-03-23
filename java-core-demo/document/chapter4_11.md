[toc]



# Java 并发面试题

## 一、Java 并发简介





## 二、Java 线程基础

### 1. 进程与线程的区别

### 2. 说出Java创建线程的三种方式及对比

### 3. 启动线程是start()还是run()？为什么？

### 4. 如何停止一个正在运行的线程？

### 5. 线程协作问题





## 三、Java 并发核心机制

### 1.谈谈volatile的特性

### 2. volatile的内存语义

- 当写一个 volatile 变量时，JMM 会把该线程对应的本地内存中的共享变量值刷新到主内存。
- 当读一个 volatile 变量时，JMM 会把该线程对应的本地内存置为无效。线程接下来将从主内存中读取共享变量。

### 3. 说说并发编程的3大特性

- 原子性
- 可见性
- 有序性

### 4. 什么是内存可见性，什么是指令重排序？

- 可见性就是指当一个线程修改了共享变量的值时，其他线程能够立即得知这个修改。
- 指令重排是指JVM在编译Java代码的时候，或者CPU在执行JVM字节码的时候，对现有的指令顺序进行重新排序。

### 5. volatile是如何解决java并发中可见性的问题

底层是通过内存屏障实现的哦，volatile能保证修饰的变量后，可以立即同步回主内存，每次使用前立即先从主内存刷新最新的值。

### 6. volatile如何防止指令重排

也是内存屏障哦，跟面试官讲下Java内存的保守策略：

- 在每个volatile写操作的前面插入一个StoreStore屏障。
- 在每个volatile写操作的后面插入一个StoreLoad屏障。
- 在每个volatile读操作的前面插入一个LoadLoad屏障。
- 在每个volatile读操作的后面插入一个LoadStore屏障。

再讲下volatile的语义哦，重排序时不能把内存屏障后面的指令重排序到内存屏障之前的位置

### 7. volatile可以解决原子性嘛？为什么？

不可以，可以直接举i++那个例子，原子性需要synchronzied或者lock保证

```
public class Test {
    public volatile int race = 0;
     
    public void increase() {
        race++;
    }
     
    public static void main(String[] args) {
        final Test test = new Test();
        for(int i=0;i<10;i++){
            new Thread(){
                public void run() {
                    for(int j=0;j<100;j++)
                        test.increase();
                };
            }.start();
        }
        
        //等待所有累加线程结束
        while(Thread.activeCount()>1)  
            Thread.yield();
        System.out.println(test.race);
    }
}
```

### 8. volatile底层的实现机制

可以看本文的第六小节，volatile底层原理哈，主要你要跟面试官讲述，volatile如何保证可见性和禁止指令重排，需要讲到内存屏障~

### 9. volatile和synchronized的区别？

- volatile修饰的是变量，synchronized一般修饰代码块或者方法
- volatile保证可见性、禁止指令重排，但是不保证原子性；synchronized可以保证原子性
- volatile不会造成线程阻塞，synchronized可能会造成线程的阻塞，所以后面才有锁优化那么多故事~
- 哈哈，你还有补充嘛~



## 四、Java 并发锁

1.读写锁可以用于什么应用场景？









## 五、Java 原子类





## 六、Java 并发容器





## 七、Java 线程池









## 八、Java 并发工具类



## 九、Java 内存模型

### 1.i++是线程安全的吗？

i++是一个复合操作，用volatile修饰只能保证可见性，不能保证原子性。如果想要保证其多线程下的安全性，可以使用原子变量、sychronized关键字、Lock锁实现

### 2.为什么局部变量是线程安全的？



## 十、Java Fork Join