[toc]



# Java 并发核心机制

## 1. J.U.C 简介

Java 的 `java.util.concurrent` 包（简称 J.U.C）中提供了大量并发工具类，是 Java 并发能力的主要体现（注意，不是全部，有部分并发能力的支持在其他包中）。从功能上，大致可以分为：

- 原子类 - 如：`AtomicInteger`、`AtomicIntegerArray`、`AtomicReference`、`AtomicStampedReference` 等。
- 锁 - 如：`ReentrantLock`、`ReentrantReadWriteLock` 等。
- 并发容器 - 如：`ConcurrentHashMap`、`CopyOnWriteArrayList`、`CopyOnWriteArraySet` 等。
- 阻塞队列 - 如：`ArrayBlockingQueue`、`LinkedBlockingQueue` 等。
- 非阻塞队列 - 如： `ConcurrentLinkedQueue` 、`LinkedTransferQueue` 等。
- `Executor` 框架（线程池）- 如：`ThreadPoolExecutor`、`Executors` 等。

我个人理解，Java 并发框架可以分为以下层次。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e63757272656e742f6a6176612d636f6e63757272656e742d62617369632d6d656368616e69736d2e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322211030.png)

由 Java 并发框架图不难看出，J.U.C 包中的工具类是基于 `synchronized`、`volatile`、`CAS`、`ThreadLocal` 这样的并发核心机制打造的。所以，要想深入理解 J.U.C 工具类的特性、为什么具有这样那样的特性，就必须先理解这些核心机制。

## 2. synchronized

> `synchronized` 是 Java 中的关键字，是 **利用锁的机制来实现互斥同步的**。
>
> **`synchronized` 可以保证在同一个时刻，只有一个线程可以执行某个方法或者某个代码块**。
>
> 如果不需要 `Lock` 、`ReadWriteLock` 所提供的高级同步特性，应该优先考虑使用 `synchronized` ，理由如下：
>
> - Java 1.6 以后，`synchronized` 做了大量的优化，其性能已经与 `Lock` 、`ReadWriteLock` 基本上持平。从趋势来看，Java 未来仍将继续优化 `synchronized` ，而不是 `ReentrantLock` 。
> - `ReentrantLock` 是 Oracle JDK 的 API，在其他版本的 JDK 中不一定支持；而 `synchronized` 是 JVM 的内置特性，所有 JDK 版本都提供支持。

### 2.1. synchronized 的应用

`synchronized` 有 3 种应用方式：

- **同步实例方法** - 对于普通同步方法，锁是当前实例对象
- **同步静态方法** - 对于静态同步方法，锁是当前类的 `Class` 对象
- **同步代码块** - 对于同步方法块，锁是 `synchonized` 括号里配置的对象

> 说明：
>
> 类似 `Vector`、`Hashtable` 这类同步类，就是使用 `synchonized` 修饰其重要方法，来保证其线程安全。
>
> 事实上，这类同步容器也非绝对的线程安全，当执行迭代器遍历，根据条件删除元素这种场景下，就可能出现线程不安全的情况。此外，Java 1.6 针对 `synchonized` 进行优化前，由于阻塞，其性能不高。
>
> 综上，这类同步容器，在现代 Java 程序中，已经渐渐不用了。

#### 2.1.1.同步实例方法

❌ 错误示例 - 未同步的示例

```
public class NoSynchronizedDemo implements Runnable {

    public static final int MAX = 100000;

    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        NoSynchronizedDemo instance = new NoSynchronizedDemo();
        Thread t1 = new Thread(instance);
        Thread t2 = new Thread(instance);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count);
    }

    @Override
    public void run() {
        for (int i = 0; i < MAX; i++) {
            increase();
        }
    }

    public void increase() {
        count++;
    }

}
// 输出结果: 小于 200000 的随机数字
```

Java 实例方法同步是同步在拥有该方法的对象上。这样，每个实例其方法同步都同步在不同的对象上，即该方法所属的实例。只有一个线程能够在实例方法同步块中运行。如果有多个实例存在，那么一个线程一次可以在一个实例同步块中执行操作。一个实例一个线程。

```
public class SynchronizedDemo implements Runnable {

    private static final int MAX = 100000;

    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        SynchronizedDemo instance = new SynchronizedDemo();
        Thread t1 = new Thread(instance);
        Thread t2 = new Thread(instance);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count);
    }

    @Override
    public void run() {
        for (int i = 0; i < MAX; i++) {
            increase();
        }
    }

    /**
     * synchronized 修饰普通方法
     */
    public synchronized void increase() {
        count++;
    }

}
```

【示例】错误示例

```
class Account {
  private int balance;
  // 转账
  synchronized void transfer(Account target, int amt){
    if (this.balance > amt) {
      this.balance -= amt;
      target.balance += amt;
    }
  }
}
```

在这段代码中，临界区内有两个资源，分别是转出账户的余额 this.balance 和转入账户的余额 target.balance，并且用的是一把锁 this，符合我们前面提到的，多个资源可以用一把锁来保护，这看上去完全正确呀。真的是这样吗？可惜，这个方案仅仅是看似正确，为什么呢？

问题就出在 this 这把锁上，this 这把锁可以保护自己的余额 this.balance，却保护不了别人的余额 target.balance，就像你不能用自家的锁来保护别人家的资产，也不能用自己的票来保护别人的座位一样。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303730313133353235372e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322211058.png)

应该保证使用的**锁能覆盖所有受保护资源**。

【示例】正确姿势

```
class Account {
  private Object lock；
  private int balance;
  private Account();
  // 创建 Account 时传入同一个 lock 对象
  public Account(Object lock) {
    this.lock = lock;
  }
  // 转账
  void transfer(Account target, int amt){
    // 此处检查所有对象共享的锁
    synchronized(lock) {
      if (this.balance > amt) {
        this.balance -= amt;
        target.balance += amt;
      }
    }
  }
}
```

这个办法确实能解决问题，但是有点小瑕疵，它要求在创建 Account 对象的时候必须传入同一个对象，如果创建 Account 对象时，传入的 lock 不是同一个对象，那可就惨了，会出现锁自家门来保护他家资产的荒唐事。在真实的项目场景中，创建 Account 对象的代码很可能分散在多个工程中，传入共享的 lock 真的很难。

上面的方案缺乏实践的可行性，我们需要更好的方案。还真有，就是**用 Account.class 作为共享的锁**。Account.class 是所有 Account 对象共享的，而且这个对象是 Java 虚拟机在加载 Account 类的时候创建的，所以我们不用担心它的唯一性。使用 Account.class 作为共享的锁，我们就无需在创建 Account 对象时传入了，代码更简单。

【示例】正确姿势

```
class Account {
  private int balance;
  // 转账
  void transfer(Account target, int amt){
    synchronized(Account.class) {
      if (this.balance > amt) {
        this.balance -= amt;
        target.balance += amt;
      }
    }
  }
}
```

#### 2.1.2.同步静态方法

静态方法的同步是指同步在该方法所在的类对象上。因为在 JVM 中一个类只能对应一个类对象，所以同时只允许一个线程执行同一个类中的静态同步方法。

对于不同类中的静态同步方法，一个线程可以执行每个类中的静态同步方法而无需等待。不管类中的那个静态同步方法被调用，一个类只能由一个线程同时执行。

```
public class SynchronizedDemo2 implements Runnable {

    private static final int MAX = 100000;

    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        SynchronizedDemo2 instance = new SynchronizedDemo2();
        Thread t1 = new Thread(instance);
        Thread t2 = new Thread(instance);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count);
    }

    @Override
    public void run() {
        for (int i = 0; i < MAX; i++) {
            increase();
        }
    }

    /**
     * synchronized 修饰静态方法
     */
    public synchronized static void increase() {
        count++;
    }

}
```

#### 2.1.3.同步代码块

有时你不需要同步整个方法，而是同步方法中的一部分。Java 可以对方法的一部分进行同步。

注意 Java 同步块构造器用括号将对象括起来。在上例中，使用了 `this`，即为调用 add 方法的实例本身。在同步构造器中用括号括起来的对象叫做监视器对象。上述代码使用监视器对象同步，同步实例方法使用调用方法本身的实例作为监视器对象。

一次只有一个线程能够在同步于同一个监视器对象的 Java 方法内执行。

```
public class SynchronizedDemo3 implements Runnable {

    private static final int MAX = 100000;

    private static int count = 0;

    public static void main(String[] args) throws InterruptedException {
        SynchronizedDemo3 instance = new SynchronizedDemo3();
        Thread t1 = new Thread(instance);
        Thread t2 = new Thread(instance);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        System.out.println(count);
    }

    @Override
    public void run() {
        for (int i = 0; i < MAX; i++) {
            increase();
        }
    }

    /**
     * synchronized 修饰代码块
     */
    public static void increase() {
        synchronized (SynchronizedDemo3.class) {
            count++;
        }
    }

}
```

### 2.2. synchronized 的原理

**`synchronized` 代码块是由一对 `monitorenter` 和 `monitorexit` 指令实现的，`Monitor` 对象是同步的基本实现单元**。在 Java 6 之前，`Monitor`的实现完全是依靠操作系统内部的互斥锁，因为需要进行用户态到内核态的切换，所以同步操作是一个无差别的重量级操作。

如果 `synchronized` 明确制定了对象参数，那就是这个对象的引用；如果没有明确指定，那就根据 `synchronized` 修饰的是实例方法还是静态方法，去对对应的对象实例或 `Class` 对象来作为锁对象。

`synchronized` 同步块对同一线程来说是可重入的，不会出现锁死问题。

`synchronized` 同步块是互斥的，即已进入的线程执行完成前，会阻塞其他试图进入的线程。

【示例】

```
public void foo(Object lock) {
    synchronized (lock) {
      lock.hashCode();
    }
  }
  // 上面的 Java 代码将编译为下面的字节码
  public void foo(java.lang.Object);
    Code:
       0: aload_1
       1: dup
       2: astore_2
       3: monitorenter
       4: aload_1
       5: invokevirtual java/lang/Object.hashCode:()I
       8: pop
       9: aload_2
      10: monitorexit
      11: goto          19
      14: astore_3
      15: aload_2
      16: monitorexit
      17: aload_3
      18: athrow
      19: return
    Exception table:
       from    to  target type
           4    11    14   any
          14    17    14   any
```

#### 2.2.1.同步代码块

`synchronized` 在修饰同步代码块时，是由 `monitorenter` 和 `monitorexit` 指令来实现同步的。进入 `monitorenter` 指令后，线程将持有 `Monitor` 对象，退出 `monitorenter` 指令后，线程将释放该 `Monitor` 对象。

#### 2.2.2.同步方法

`synchronized` 修饰同步方法时，会设置一个 `ACC_SYNCHRONIZED` 标志。当方法调用时，调用指令将会检查该方法是否被设置 `ACC_SYNCHRONIZED`访问标志。如果设置了该标志，执行线程将先持有 `Monitor` 对象，然后再执行方法。在该方法运行期间，其它线程将无法获取到该 `Mointor` 对象，当方法执行完成后，再释放该 `Monitor` 对象。

#### 2.2.3.Monitor

每个对象实例都会有一个 `Monitor`，`Monitor` 可以和对象一起创建、销毁。`Monitor` 是由 `ObjectMonitor` 实现，而 `ObjectMonitor` 是由 C++ 的 `ObjectMonitor.hpp` 文件实现。

当多个线程同时访问一段同步代码时，多个线程会先被存放在 EntryList 集合中，处于 block 状态的线程，都会被加入到该列表。接下来当线程获取到对象的 Monitor 时，Monitor 是依靠底层操作系统的 Mutex Lock 来实现互斥的，线程申请 Mutex 成功，则持有该 Mutex，其它线程将无法获取到该 Mutex。

如果线程调用 wait() 方法，就会释放当前持有的 Mutex，并且该线程会进入 WaitSet 集合中，等待下一次被唤醒。如果当前线程顺利执行完方法，也将释放 Mutex。

### 2.3. synchronized 的优化

> **Java 1.6 以后，`synchronized` 做了大量的优化，其性能已经与 `Lock` 、`ReadWriteLock` 基本上持平**。

#### 2.3.1.Java 对象头

在 JDK1.6 JVM 中，对象实例在堆内存中被分为了三个部分：对象头、实例数据和对齐填充。其中 Java 对象头由 Mark Word、指向类的指针以及数组长度三部分组成。

Mark Word 记录了对象和锁有关的信息。Mark Word 在 64 位 JVM 中的长度是 64bit，我们可以一起看下 64 位 JVM 的存储结构是怎么样的。如下图所示：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303632393139313235302e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322211137.png)

锁升级功能主要依赖于 Mark Word 中的锁标志位和释放偏向锁标志位，`synchronized` 同步锁就是从偏向锁开始的，随着竞争越来越激烈，偏向锁升级到轻量级锁，最终升级到重量级锁。

Java 1.6 引入了偏向锁和轻量级锁，从而让 `synchronized` 拥有了四个状态：

- **无锁状态（unlocked）**
- **偏向锁状态（biasble）**
- **轻量级锁状态（lightweight locked）**
- **重量级锁状态（inflated）**

当 JVM 检测到不同的竞争状况时，会自动切换到适合的锁实现。

当没有竞争出现时，默认会使用偏向锁。JVM 会利用 CAS 操作（compare and swap），在对象头上的 Mark Word 部分设置线程 ID，以表示这个对象偏向于当前线程，所以并不涉及真正的互斥锁。这样做的假设是基于在很多应用场景中，大部分对象生命周期中最多会被一个线程锁定，使用偏斜锁可以降低无竞争开销。

如果有另外的线程试图锁定某个已经被偏斜过的对象，JVM 就需要撤销（revoke）偏向锁，并切换到轻量级锁实现。轻量级锁依赖 CAS 操作 Mark Word 来试图获取锁，如果重试成功，就使用普通的轻量级锁；否则，进一步升级为重量级锁。

#### 2.3.2.偏向锁

偏向锁的思想是偏向于**第一个获取锁对象的线程，这个线程在之后获取该锁就不再需要进行同步操作，甚至连 CAS 操作也不再需要**。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303630343130353135312e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322211159.png)

#### 2.3.3.轻量级锁

**轻量级锁**是相对于传统的重量级锁而言，它 **使用 CAS 操作来避免重量级锁使用互斥量的开销**。对于绝大部分的锁，在整个同步周期内都是不存在竞争的，因此也就不需要都使用互斥量进行同步，可以先采用 CAS 操作进行同步，如果 CAS 失败了再改用互斥量进行同步。

当尝试获取一个锁对象时，如果锁对象标记为 `0|01`，说明锁对象的锁未锁定（unlocked）状态。此时虚拟机在当前线程的虚拟机栈中创建 Lock Record，然后使用 CAS 操作将对象的 Mark Word 更新为 Lock Record 指针。如果 CAS 操作成功了，那么线程就获取了该对象上的锁，并且对象的 Mark Word 的锁标记变为 00，表示该对象处于轻量级锁状态。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303630343130353234382e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322211217.png)

#### 2.3.4.锁消除 / 锁粗化

除了锁升级优化，Java 还使用了编译器对锁进行优化。

**2.3.4.1.锁消除**

**锁消除是指对于被检测出不可能存在竞争的共享数据的锁进行消除**。

JIT 编译器在动态编译同步块的时候，借助了一种被称为逃逸分析的技术，来判断同步块使用的锁对象是否只能够被一个线程访问，而没有被发布到其它线程。

确认是的话，那么 JIT 编译器在编译这个同步块的时候不会生成 synchronized 所表示的锁的申请与释放的机器码，即消除了锁的使用。在 Java7 之后的版本就不需要手动配置了，该操作可以自动实现。

对于一些看起来没有加锁的代码，其实隐式的加了很多锁。例如下面的字符串拼接代码就隐式加了锁：

```
public static String concatString(String s1, String s2, String s3) {
    return s1 + s2 + s3;
}
```

`String` 是一个不可变的类，编译器会对 String 的拼接自动优化。在 Java 1.5 之前，会转化为 `StringBuffer` 对象的连续 `append()` 操作：

```
public static String concatString(String s1, String s2, String s3) {
    StringBuffer sb = new StringBuffer();
    sb.append(s1);
    sb.append(s2);
    sb.append(s3);
    return sb.toString();
}
```

每个 `append()` 方法中都有一个同步块。虚拟机观察变量 sb，很快就会发现它的动态作用域被限制在 `concatString()` 方法内部。也就是说，sb 的所有引用永远不会逃逸到 `concatString()` 方法之外，其他线程无法访问到它，因此可以进行消除。

**2.3.4.2.锁粗化**

锁粗化同理，就是在 JIT 编译器动态编译时，如果发现几个相邻的同步块使用的是同一个锁实例，那么 JIT 编译器将会把这几个同步块合并为一个大的同步块，从而避免一个线程“反复申请、释放同一个锁“所带来的性能开销。

如果**一系列的连续操作都对同一个对象反复加锁和解锁**，频繁的加锁操作就会导致性能损耗。

上一节的示例代码中连续的 `append()` 方法就属于这类情况。如果**虚拟机探测到由这样的一串零碎的操作都对同一个对象加锁，将会把加锁的范围扩展（粗化）到整个操作序列的外部**。对于上一节的示例代码就是扩展到第一个 `append()` 操作之前直至最后一个 `append()` 操作之后，这样只需要加锁一次就可以了。

#### 2.3.5.自旋锁

互斥同步进入阻塞状态的开销都很大，应该尽量避免。在许多应用中，共享数据的锁定状态只会持续很短的一段时间。自旋锁的思想是让一个线程在请求一个共享数据的锁时执行忙循环（自旋）一段时间，如果在这段时间内能获得锁，就可以避免进入阻塞状态。

自旋锁虽然能避免进入阻塞状态从而减少开销，但是它需要进行忙循环操作占用 CPU 时间，它只适用于共享数据的锁定状态很短的场景。

在 Java 1.6 中引入了自适应的自旋锁。自适应意味着自旋的次数不再固定了，而是由前一次在同一个锁上的自旋次数及锁的拥有者的状态来决定。

### 2.4. synchronized 的误区

> 示例摘自：[《Java 业务开发常见错误 100 例》](https://time.geekbang.org/column/intro/100047701)

#### 2.4.1.synchronized 使用范围不当导致的错误

```
public class Interesting {

    volatile int a = 1;
    volatile int b = 1;

    public static void main(String[] args) {
        Interesting interesting = new Interesting();
        new Thread(() -> interesting.add()).start();
        new Thread(() -> interesting.compare()).start();
    }

    public synchronized void add() {
        log.info("add start");
        for (int i = 0; i < 10000; i++) {
            a++;
            b++;
        }
        log.info("add done");
    }

    public void compare() {
        log.info("compare start");
        for (int i = 0; i < 10000; i++) {
            //a始终等于b吗？
            if (a < b) {
                log.info("a:{},b:{},{}", a, b, a > b);
                //最后的a>b应该始终是false吗？
            }
        }
        log.info("compare done");
    }

}
```

【输出】

```
16:05:25.541 [Thread-0] INFO io.github.dunwu.javacore.concurrent.sync.synchronized使用范围不当 - add start
16:05:25.544 [Thread-0] INFO io.github.dunwu.javacore.concurrent.sync.synchronized使用范围不当 - add done
16:05:25.544 [Thread-1] INFO io.github.dunwu.javacore.concurrent.sync.synchronized使用范围不当 - compare start
16:05:25.544 [Thread-1] INFO io.github.dunwu.javacore.concurrent.sync.synchronized使用范围不当 - compare done
```

之所以出现这种错乱，是因为两个线程是交错执行 add 和 compare 方法中的业务逻辑，而且这些业务逻辑不是原子性的：a++ 和 b++ 操作中可以穿插在 compare 方法的比较代码中；更需要注意的是，a<b 这种比较操作在字节码层面是加载 a、加载 b 和比较三步，代码虽然是一行但也不是原子性的。

所以，正确的做法应该是，为 add 和 compare 都加上方法锁，确保 add 方法执行时，compare 无法读取 a 和 b：

```
public synchronized void add()
public synchronized void compare()
```

所以，使用锁解决问题之前一定要理清楚，我们要保护的是什么逻辑，多线程执行的情况又是怎样的。

#### 2.4.2.synchronized 保护对象不对导致的错误

加锁前要清楚锁和被保护的对象是不是一个层面的。

静态字段属于类，类级别的锁才能保护；而非静态字段属于类实例，实例级别的锁就可以保护。

```
public class synchronized错误使用示例2 {

    public static void main(String[] args) {
        synchronized错误使用示例2 demo = new synchronized错误使用示例2();
        System.out.println(demo.wrong(1000000));
        System.out.println(demo.right(1000000));
    }

    public int wrong(int count) {
        Data.reset();
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().wrong());
        return Data.getCounter();
    }

    public int right(int count) {
        Data.reset();
        IntStream.rangeClosed(1, count).parallel().forEach(i -> new Data().right());
        return Data.getCounter();
    }

    private static class Data {

        @Getter
        private static int counter = 0;
        private static Object locker = new Object();

        public static int reset() {
            counter = 0;
            return counter;
        }

        public synchronized void wrong() {
            counter++;
        }

        public void right() {
            synchronized (locker) {
                counter++;
            }
        }

    }

}
```

wrong 方法中试图对一个静态对象加对象级别的 synchronized 锁，并不能保证线程安全。

#### 2.4.3.锁粒度导致的问题

要尽可能的缩小加锁的范围，这可以提高并发吞吐。

如果精细化考虑了锁应用范围后，性能还无法满足需求的话，我们就要考虑另一个维度的粒度问题了，即：区分读写场景以及资源的访问冲突，考虑使用悲观方式的锁还是乐观方式的锁。

```
public class synchronized锁粒度不当 {

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.wrong();
        demo.right();
    }

    private static class Demo {

        private List<Integer> data = new ArrayList<>();

        private void slow() {
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
            }
        }

        public int wrong() {
            long begin = System.currentTimeMillis();
            IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
                synchronized (this) {
                    slow();
                    data.add(i);
                }
            });
            log.info("took:{}", System.currentTimeMillis() - begin);
            return data.size();
        }

        public int right() {
            long begin = System.currentTimeMillis();
            IntStream.rangeClosed(1, 1000).parallel().forEach(i -> {
                slow();
                synchronized (data) {
                    data.add(i);
                }
            });
            log.info("took:{}", System.currentTimeMillis() - begin);
            return data.size();
        }

    }

}
```

## 3. volatile

### 3.1. volatile 的要点

`volatile` 是轻量级的 `synchronized`，它在多处理器开发中保证了共享变量的“可见性”。

被 `volatile` 修饰的变量，具备以下特性：

- **线程可见性** - 保证了不同线程对这个变量进行操作时的可见性，即一个线程修改了某个共享变量，另外一个线程能读到这个修改的值。
- **禁止指令重排序**
- **不保证原子性**

我们知道，线程安全需要具备：可见性、原子性、顺序性。`volatile` 不保证原子性，所以决定了它不能彻底地保证线程安全。

### 3.2. volatile 的应用

如果 `volatile` 变量修饰符使用恰当的话，它比 `synchronized` 的使用和执行成本更低，因为它不会引起线程上下文的切换和调度。但是，**`volatile` 无法替代 `synchronized` ，因为 `volatile` 无法保证操作的原子性**。

通常来说，**使用 `volatile` 必须具备以下 2 个条件**：

- **对变量的写操作不依赖于当前值**
- **该变量没有包含在具有其他变量的表达式中**

【示例】状态标记量

```
volatile boolean flag = false;

while(!flag) {
    doSomething();
}

public void setFlag() {
    flag = true;
}
```

【示例】双重锁实现线程安全的单例模式

```
class Singleton {
    private volatile static Singleton instance = null;

    private Singleton() {}

    public static Singleton getInstance() {
        if(instance==null) {
            synchronized (Singleton.class) {
                if(instance==null)
                    instance = new Singleton();
            }
        }
        return instance;
    }
}
```

### 3.3. volatile 的原理

#### 3.3.1. 重排序

重排序是指编译器和处理器为了优化程序性能而对指令序列进行排序的一种手段。但是重排序也需要遵守一定规则：

1. 重排序操作不会对存在数据依赖关系的操作进行重排序。

   比如：`a=1;b=a; ` 这个指令序列，由于第二个操作依赖于第一个操作，所以在编译时和处理器运行时这两个操作不会被重排序。

2. 重排序是为了优化性能，但不管怎么重排序，单线程下程序的执行结果不能被改变

   比如：`a=1;b=2;c=a+b` 这三个操作，第一步（a=1)和第二步(b=2)由于不存在数据依赖关系，所以可能会发生重排序，但是c=a+b这个操作是不会被重排序的，因为需要保证最终的结果一定是 `c=a+b=3`。

重排序在单线程模式下是一定会保证最终结果的正确性，但是在多线程环境下，问题就不能保证了。

#### 3.3.2. 如何保证可见性和禁止指令重排序的？

假设flag变量的初始值false，现在有两条线程t1和t2要访问它，就可以简化为以下图：

![7ddbe230c8dc4501a77ffbe0587b5ba6_tplv-k3u1fbpfcp-zoom-1.image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324001343.jpg)

如果线程t1执行以下代码语句，并且flag没有volatile修饰的话；t1刚修改完flag的值，还没来得及刷新到主内存，t2又跑过来读取了，很容易就数据flag不一致了，如下：

```
flag=true;
```

![cc065cf75803496aa1efafd6d68ba968_tplv-k3u1fbpfcp-zoom-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324001429.jpg)

如果flag变量是由volatile修饰的话，就不一样了，如果线程t1修改了flag值，volatile能保证修饰的flag变量后，可以**「立即同步回主内存」**。如图：

![27e9e195810a4a71bdeb38dd128b27e4_tplv-k3u1fbpfcp-zoom-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324001559.jpg)

细心的朋友会发现，线程t2不还是flag旧的值吗，这不还有问题嘛？其实volatile还有一个保证，就是**「每次使用前立即先从主内存刷新最新的值」**，线程t1修改完后，线程t2的变量副本会过期了，如图：

![7e67dcdfe9d9412dab89961bf92b5b53_tplv-k3u1fbpfcp-zoom-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324001630.jpg)

显然，这里还不是底层，实际上volatile保证可见性和禁止指令重排都跟**「内存屏障」**有关，我们编译volatile相关代码看看~

**DCL单例模式（volatile）&编译对比**

DCL单例模式（Double Check Lock，双重检查锁）比较常用，它是需要volatile修饰的，所以就拿这段代码编译吧

```
public class Singleton {  
    private volatile static Singleton instance;  
    private Singleton (){}  
    public static Singleton getInstance() {  
    if (instance == null) {  
        synchronized (Singleton.class) {  
        if (instance == null) {  
            instance = new Singleton();  
        }  
        }  
    }  
    return instance;  
    }  
}  
```

编译这段代码后，观察有volatile关键字和没有volatile关键字时的instance所生成的汇编代码发现，有volatile关键字修饰时，会多出一个lock addl $0x0,(%esp)，即多出一个lock前缀指令

```
0x01a3de0f: mov    $0x3375cdb0,%esi   ;...beb0cd75 33  
                                        ;   {oop('Singleton')}  
0x01a3de14: mov    %eax,0x150(%esi)   ;...89865001 0000  
0x01a3de1a: shr    $0x9,%esi          ;...c1ee09  
0x01a3de1d: movb   $0x0,0x1104800(%esi)  ;...c6860048 100100  
0x01a3de24: lock addl $0x0,(%esp)     ;...f0830424 00  
                                        ;*putstatic instance  
                                        ; - Singleton::getInstance@24 
```

ock指令相当于一个**「内存屏障」**，它保证以下这几点：

- 1.重排序时不能把后面的指令重排序到内存屏障之前的位置
- 2.将本处理器的缓存写入内存
- 3.如果是写入动作，会导致其他处理器中对应的缓存无效。

显然，第2、3点不就是volatile保证可见性的体现嘛，第1点就是禁止指令重排列的体现。

**内存屏障**

内存屏障四大分类：（Load 代表读取指令，Store代表写入指令）

| 内存屏障类型   | 抽象场景                   | 描述                                                         |
| :------------- | :------------------------- | :----------------------------------------------------------- |
| LoadLoad屏障   | Load1; LoadLoad; Load2     | 在Load2要读取的数据被访问前，保证Load1要读取的数据被读取完毕。 |
| StoreStore屏障 | Store1; StoreStore; Store2 | 在Store2写入执行前，保证Store1的写入操作对其它处理器可见     |
| LoadStore屏障  | Load1; LoadStore; Store2   | 在Store2被写入前，保证Load1要读取的数据被读取完毕。          |
| StoreLoad屏障  | Store1; StoreLoad; Load2   | 在Load2读取操作执行前，保证Store1的写入对所有处理器可见。    |

为了实现volatile的内存语义，Java内存模型采取以下的保守策略

- 在每个volatile写操作的前面插入一个StoreStore屏障。
- 在每个volatile写操作的后面插入一个StoreLoad屏障。
- 在每个volatile读操作的前面插入一个LoadLoad屏障。
- 在每个volatile读操作的后面插入一个LoadStore屏障。

有些小伙伴，可能对这个还是有点疑惑，内存屏障这玩意太抽象了。我们照着代码看下吧：

![a3097b7467304540b6a552d897d46997_tplv-k3u1fbpfcp-zoom-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324001903.jpg)

内存屏障保证前面的指令先执行，所以这就保证了禁止了指令重排啦，同时内存屏障保证缓存写入内存和其他处理器缓存失效，这也就保证了可见性

### 3.4. volatile 的问题

`volatile` 的要点中，已经提到，**`volatile` 不保证原子性，所以 volatile 并不能保证线程安全**。

那么，如何做到线程安全呢？有两种方案：

- `volatile` + `synchronized` - 可以参考：【示例】双重锁实现线程安全的单例模式
- 使用原子类替代 `volatile`



## 4. CAS

### 4.1. CAS 的要点

互斥同步是最常见的并发正确性保障手段。

**互斥同步最主要的问题是线程阻塞和唤醒所带来的性能问题**，因此互斥同步也被称为阻塞同步。互斥同步属于一种悲观的并发策略，总是认为只要不去做正确的同步措施，那就肯定会出现问题。无论共享数据是否真的会出现竞争，它都要进行加锁（这里讨论的是概念模型，实际上虚拟机会优化掉很大一部分不必要的加锁）、用户态核心态转换、维护锁计数器和检查是否有被阻塞的线程需要唤醒等操作。

随着硬件指令集的发展，我们可以使用基于冲突检测的乐观并发策略：先进行操作，如果没有其它线程争用共享数据，那操作就成功了，否则采取补偿措施（不断地重试，直到成功为止）。这种乐观的并发策略的许多实现都不需要将线程阻塞，因此这种同步操作称为非阻塞同步。

为什么说乐观锁需要 **硬件指令集的发展** 才能进行？因为需要操作和冲突检测这两个步骤具备原子性。而这点是由硬件来完成，如果再使用互斥同步来保证就失去意义了。硬件支持的原子性操作最典型的是：CAS。

**CAS（Compare and Swap），字面意思为比较并交换。CAS 有 3 个操作数，分别是：内存值 M，期望值 E，更新值 U。当且仅当内存值 M 和期望值 E 相等时，将内存值 M 修改为 U，否则什么都不做**。

### 4.2. CAS 的应用

**CAS 只适用于线程冲突较少的情况**。

CAS 的典型应用场景是：

- 原子类
- 自旋锁

#### 4.2.1.原子类

> 原子类是 CAS 在 Java 中最典型的应用。

我们先来看一个常见的代码片段。

```
if(a==b) {
    a++;
}
```

如果 `a++` 执行前， a 的值被修改了怎么办？还能得到预期值吗？出现该问题的原因是在并发环境下，以上代码片段不是原子操作，随时可能被其他线程所篡改。

解决这种问题的最经典方式是应用原子类的 `incrementAndGet` 方法。

```
public class AtomicIntegerDemo {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        final AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    count.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(3, TimeUnit.SECONDS);
        System.out.println("Final Count is : " + count.get());
    }

}
```

J.U.C 包中提供了 `AtomicBoolean`、`AtomicInteger`、`AtomicLong` 分别针对 `Boolean`、`Integer`、`Long` 执行原子操作，操作和上面的示例大体相似，不做赘述。

#### 4.2.2.自旋锁

利用原子类（本质上是 CAS），可以实现自旋锁。

所谓自旋锁，是指线程反复检查锁变量是否可用，直到成功为止。由于线程在这一过程中保持执行，因此是一种忙等待。一旦获取了自旋锁，线程会一直保持该锁，直至显式释放自旋锁。

示例：非线程安全示例

```
public class AtomicReferenceDemo {

    private static int ticket = 10;

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 5; i++) {
            executorService.execute(new MyThread());
        }
        executorService.shutdown();
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            while (ticket > 0) {
                System.out.println(Thread.currentThread().getName() + " 卖出了第 " + ticket + " 张票");
                ticket--;
            }
        }

    }

}
```

输出结果：

```
pool-1-thread-2 卖出了第 10 张票
pool-1-thread-1 卖出了第 10 张票
pool-1-thread-3 卖出了第 10 张票
pool-1-thread-1 卖出了第 8 张票
pool-1-thread-2 卖出了第 9 张票
pool-1-thread-1 卖出了第 6 张票
pool-1-thread-3 卖出了第 7 张票
pool-1-thread-1 卖出了第 4 张票
pool-1-thread-2 卖出了第 5 张票
pool-1-thread-1 卖出了第 2 张票
pool-1-thread-3 卖出了第 3 张票
pool-1-thread-2 卖出了第 1 张票
```

很明显，出现了重复售票的情况。

【示例】使用自旋锁来保证线程安全

可以通过自旋锁这种非阻塞同步来保证线程安全，下面使用 `AtomicReference` 来实现一个自旋锁。

```
public class AtomicReferenceDemo2 {

    private static int ticket = 10;

    public static void main(String[] args) {
        threadSafeDemo();
    }

    private static void threadSafeDemo() {
        SpinLock lock = new SpinLock();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 5; i++) {
            executorService.execute(new MyThread(lock));
        }
        executorService.shutdown();
    }

    static class SpinLock {

        private AtomicReference<Thread> atomicReference = new AtomicReference<>();

        public void lock() {
            Thread current = Thread.currentThread();
            while (!atomicReference.compareAndSet(null, current)) {}
        }

        public void unlock() {
            Thread current = Thread.currentThread();
            atomicReference.compareAndSet(current, null);
        }

    }

    static class MyThread implements Runnable {

        private SpinLock lock;

        public MyThread(SpinLock lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            while (ticket > 0) {
                lock.lock();
                if (ticket > 0) {
                    System.out.println(Thread.currentThread().getName() + " 卖出了第 " + ticket + " 张票");
                    ticket--;
                }
                lock.unlock();
            }
        }

    }

}
```

输出结果：

```
pool-1-thread-2 卖出了第 10 张票
pool-1-thread-1 卖出了第 9 张票
pool-1-thread-3 卖出了第 8 张票
pool-1-thread-2 卖出了第 7 张票
pool-1-thread-3 卖出了第 6 张票
pool-1-thread-1 卖出了第 5 张票
pool-1-thread-2 卖出了第 4 张票
pool-1-thread-1 卖出了第 3 张票
pool-1-thread-3 卖出了第 2 张票
pool-1-thread-1 卖出了第 1 张票
```

### 4.3. CAS 的原理

Java 主要利用 `Unsafe` 这个类提供的 CAS 操作。`Unsafe` 的 CAS 依赖的是 JVM 针对不同的操作系统实现的硬件指令 **`Atomic::cmpxchg`**。`Atomic::cmpxchg` 的实现使用了汇编的 CAS 操作，并使用 CPU 提供的 `lock` 信号保证其原子性。

### 4.4. CAS 的问题

一般情况下，CAS 比锁性能更高。因为 CAS 是一种非阻塞算法，所以其避免了线程阻塞和唤醒的等待时间。

但是，事物总会有利有弊，CAS 也存在三大问题：

- ABA 问题
- 循环时间长开销大
- 只能保证一个共享变量的原子性

#### 4.4.1.ABA 问题

**如果一个变量初次读取的时候是 A 值，它的值被改成了 B，后来又被改回为 A，那 CAS 操作就会误认为它从来没有被改变过**。

J.U.C 包提供了一个带有标记的**原子引用类 `AtomicStampedReference` 来解决这个问题**，它可以通过控制变量值的版本来保证 CAS 的正确性。大部分情况下 ABA 问题不会影响程序并发的正确性，如果需要解决 ABA 问题，改用**传统的互斥同步可能会比原子类更高效**。

#### 4.4.2.循环时间长开销大

**自旋 CAS （不断尝试，直到成功为止）如果长时间不成功，会给 CPU 带来非常大的执行开销**。

如果 JVM 能支持处理器提供的 `pause` 指令那么效率会有一定的提升，`pause` 指令有两个作用：

- 它可以延迟流水线执行指令（de-pipeline）,使 CPU 不会消耗过多的执行资源，延迟的时间取决于具体实现的版本，在一些处理器上延迟时间是零。
- 它可以避免在退出循环的时候因内存顺序冲突（memory order violation）而引起 CPU 流水线被清空（CPU pipeline flush），从而提高 CPU 的执行效率。

比较花费 CPU 资源，即使没有任何用也会做一些无用功。

#### 4.4.3.只能保证一个共享变量的原子性

当对一个共享变量执行操作时，我们可以使用循环 CAS 的方式来保证原子操作，但是对多个共享变量操作时，循环 CAS 就无法保证操作的原子性，这个时候就可以用锁。

或者有一个取巧的办法，就是把多个共享变量合并成一个共享变量来操作。比如有两个共享变量 `i ＝ 2, j = a`，合并一下 `ij=2a`，然后用 CAS 来操作 `ij`。从 Java 1.5 开始 JDK 提供了 `AtomicReference` 类来保证引用对象之间的原子性，你可以把多个变量放在一个对象里来进行 CAS 操作。

## 5. ThreadLocal

> **`ThreadLocal` 是一个存储线程本地副本的工具类**。
>
> 要保证线程安全，不一定非要进行同步。同步只是保证共享数据争用时的正确性，如果一个方法本来就不涉及共享数据，那么自然无须同步。
>
> Java 中的 **无同步方案** 有：
>
> - **可重入代码** - 也叫纯代码。如果一个方法，它的 **返回结果是可以预测的**，即只要输入了相同的数据，就能返回相同的结果，那它就满足可重入性，当然也是线程安全的。
> - **线程本地存储** - 使用 **`ThreadLocal` 为共享变量在每个线程中都创建了一个本地副本**，这个副本只能被当前线程访问，其他线程无法访问，那么自然是线程安全的。

**理解误区**

> ThreadLocal为解决多线程程序的并发问题提供了一种新的思路；
>
> ThreadLocal的目的是为了解决多线程访问资源时的共享问题。

结论：ThreadLocal 并不是像上面所说为了解决多线程 **共享** 变量的问题。

**正确理解**

ThreadLoal 变量，它的基本原理是，同一个 ThreadLocal 所包含的对象（对 `ThreadLocal< StringBuilder >` 而言即为 StringBuilder 类型变量），在不同的 Thread 中有不同的副本（实际上是不同的实例）:

- 因为每个 Thread 内有自己的实例副本，且该副本只能由当前 Thread 使用；
- 既然其它 Thread 不可访问，那就不存在多线程间共享的问题。

官方文档是这样描述的：

![16f0756eb974cc8f](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324210853.jpg)

我看完之后，得出这样的结论

> ThreadLocal 提供了线程本地的实例。它与普通变量的区别在于，每个使用该变量的线程都会初始化一个完全独立的实例副本。ThreadLocal 变量通常被`private static`修饰。当一个线程结束时，它所使用的所有 ThreadLocal 相对的实例副本都会被回收。

因此**ThreadLocal 非常适用于这样的场景：每个线程需要自己独立的实例且该实例需要在多个方法中使用**。当然，使用其它方式也可以实现同样的效果，但是看完这篇文章，你会发现 ThreadLocal 会让实现更简洁、更优雅！



### 5.1. ThreadLocal 的应用

`ThreadLocal` 的方法：

```
public class ThreadLocal<T> {
    public T get() {}
    public void set(T value) {}
    public void remove() {}
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {}
}
```

> 说明：
>
> - `get` - 用于获取 `ThreadLocal` 在当前线程中保存的变量副本。
> - `set` - 用于设置当前线程中变量的副本。
> - `remove` - 用于删除当前线程中变量的副本。如果此线程局部变量随后被当前线程读取，则其值将通过调用其 `initialValue` 方法重新初始化，除非其值由中间线程中的当前线程设置。 这可能会导致当前线程中多次调用 `initialValue` 方法。
> - `initialValue` - 为 ThreadLocal 设置默认的 `get` 初始值，需要重写 `initialValue` 方法 。

`ThreadLocal` 常用于防止对可变的单例（Singleton）变量或全局变量进行共享。典型应用场景有：管理数据库连接、Session。

【示例】数据库连接

```
private static ThreadLocal<Connection> connectionHolder = new ThreadLocal<Connection>() {
    @Override
    public Connection initialValue() {
        return DriverManager.getConnection(DB_URL);
    }
};

public static Connection getConnection() {
    return connectionHolder.get();
}
```

【示例】Session 管理

```
private static final ThreadLocal<Session> sessionHolder = new ThreadLocal<>();

public static Session getSession() {
    Session session = (Session) sessionHolder.get();
    try {
        if (session == null) {
            session = createSession();
            sessionHolder.set(session);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return session;
}
```

【示例】完整使用 `ThreadLocal` 示例

```
public class ThreadLocalDemo {

    private static ThreadLocal<Integer> threadLocal = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executorService.execute(new MyThread());
        }
        executorService.shutdown();
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            int count = threadLocal.get();
            for (int i = 0; i < 10; i++) {
                try {
                    count++;
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            threadLocal.set(count);
            threadLocal.remove();
            System.out.println(Thread.currentThread().getName() + " : " + count);
        }

    }

}
```

全部输出 count = 10

### 5.2. ThreadLocal 的原理

#### 5.2.1 存储结构
**方案一**

我们大胆猜想一下，既然每个访问 ThreadLocal 变量的线程都有自己的一个“本地”实例副本。一个可能的方案是 ThreadLocal 维护一个 Map，Key 是当前线程，Value是ThreadLocal在当前线程内的实例。这样，线程通过该 ThreadLocal 的 get() 方案获取实例时，只需要以线程为键，从 Map 中找出对应的实例即可。该方案如下图所示

![VarMap](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210406174404.webp)

这个方案可以满足上文提到的每个线程内部都有一个ThreadLocal 实例备份的要求。每个新线程访问该 ThreadLocal 时，都会向 Map 中添加一个新的映射，而当每个线程结束时再清除该线程对应的映射。But，这样就存在两个问题：

- 开启线程与结束线程时我们都需要及时更新 Map，因此必需保证 Map 的线程安全。
- 当线程结束时，需要保证它所访问的所有 ThreadLocal 中对应的映射均删除，否则可能会引起内存泄漏。

线程安全问题是JDK 未采用该方案的一个主要原因。

**方案二**

上面这个方案，存在多线程访问同一个 Map时可能会出现的同步问题。如果该 Map 由 Thread 维护，从而使得每个 Thread 只访问自己的 Map，就不存在这个问题。该方案如下图所示。

![ThreadMap](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210406174433.webp)

该方案虽然没有锁的问题，但是由于每个线程在访问ThreadLocal 变量后，都会在自己的 Map 内维护该 ThreadLocal 变量与具体实例的映射，如果不删除这些引用（映射），就有可能会造成内存泄漏的问题。我们一起来看一下Jdk8是如何解决这个问题的。

**`Thread` 类中维护着一个 `ThreadLocal.ThreadLocalMap` 类型的成员** `threadLocals`。这个成员就是用来存储当前线程独占的变量副本。

`ThreadLocalMap` 是 `ThreadLocal` 的内部类，它维护着一个 `Entry` 数组，**`Entry` 继承了 `WeakReference`** ，所以是弱引用。 `Entry` 用于保存键值对，其中：

- `key` 是 `ThreadLocal` 对象；
- `value` 是传递进来的对象（变量副本）。

```
public class Thread implements Runnable {
    // ...
    ThreadLocal.ThreadLocalMap threadLocals = null;
    // ...
}

static class ThreadLocalMap {
    // ...
    static class Entry extends WeakReference<ThreadLocal<?>> {
        /** The value associated with this ThreadLocal. */
        Object value;

        Entry(ThreadLocal<?> k, Object v) {
            super(k);
            value = v;
        }
    }
    // ...
}
```

#### 5.2.2 如何解决 Hash 冲突

`ThreadLocalMap` 虽然是类似 `Map` 结构的数据结构，但它并没有实现 `Map` 接口。它不支持 `Map` 接口中的 `next` 方法，这意味着 `ThreadLocalMap` 中解决 Hash 冲突的方式并非 **拉链表** 方式。

实际上，**`ThreadLocalMap` 采用线性探测的方式来解决 Hash 冲突**。所谓线性探测，就是根据初始 key 的 hashcode 值确定元素在 table 数组中的位置，如果发现这个位置上已经被其他的 key 值占用，则利用固定的算法寻找一定步长的下个位置，依次判断，直至找到能够存放的位置。

#### 5.2.3 ThreadLocal 在 JDK 8 中的实现

##### 5.2.3.1 ThreadLocalMap与内存泄漏

在该方案中，Map 由 ThreadLocal 类的静态内部类 ThreadLocalMap 提供。该类的实例维护某个 ThreadLocal 与具体实例的映射。与 HashMap 不同的是，ThreadLocalMap 的每个 **Entry** 都是一个对 **Key** 的弱引用，这一点我们可以从`super(k)`可看出。另外，每个 Entry 中都包含了一个对 **Value** 的强引用。

```
static class Entry extends WeakReference<ThreadLocal<?>> {
  /** The value associated with this ThreadLocal. */
  Object value;

  Entry(ThreadLocal<?> k, Object v) {
    super(k);
    value = v;
  }
}
```

之所以使用弱引用，是因为当没有强引用指向 ThreadLocal 变量时，这个变量就可以被回收，就避免ThreadLocal 因为不能被回收而造成的内存泄漏的问题。

但是，这里又可能出现另外一种内存泄漏的问题。ThreadLocalMap 维护 ThreadLocal 变量与具体实例的映射，当 ThreadLocal 变量被回收后，该映射的键变为 null，该 Entry 无法被移除。从而使得实例被该 Entry 引用而无法被回收造成内存泄漏。

![1824337-20200829092808143-824603771](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324212220.png)

**注意：**Entry是对 ThreadLocal 类型的弱引用，并不是具体实例的弱引用，因此还存在具体实例相关的内存泄漏的问题。

##### 5.2.3.2 读取实例

我们来看一下ThreadLocal获取实例的方法

```
public T get() {
  Thread t = Thread.currentThread();
  ThreadLocalMap map = getMap(t);
  if (map != null) {
    ThreadLocalMap.Entry e = map.getEntry(this);
    if (e != null) {
      @SuppressWarnings("unchecked")
      T result = (T)e.value;
      return result;
    }
  }
  return setInitialValue();
}
```

当线程获取实例时，首先会通过`getMap(t)`方法获取自身的 ThreadLocalMap。从如下该方法的定义可见，该 ThreadLocalMap 的实例是 Thread 类的一个字段，即由 Thread 维护 ThreadLocal 对象与具体实例的映射，这一点与上文分析一致。

```
ThreadLocalMap getMap(Thread t) {
  return t.threadLocals;
}
```

获取到 ThreadLocalMap 后，通过`map.getEntry(this)`方法获取该 ThreadLocal 在当前线程的 ThreadLocalMap 中对应的 Entry。该方法中的 this 即当前访问的 ThreadLocal 对象。

如果获取到的 Entry 不为 null，从 Entry 中取出值即为所需访问的本线程对应的实例。如果获取到的 Entry 为 null，则通过`setInitialValue()`方法设置该 ThreadLocal 变量在该线程中对应的具体实例的初始值。

##### 5.2.3.3 设置初始值

设置初始值方法如下

```
private T setInitialValue() {
  T value = initialValue();
  Thread t = Thread.currentThread();
  ThreadLocalMap map = getMap(t);
  if (map != null)
    map.set(this, value);
  else
    createMap(t, value);
  return value;
}
```

该方法为 private 方法，无法被重载。

首先，通过`initialValue()`方法获取初始值。该方法为 public 方法，且默认返回 null。所以典型用法中常常重载该方法。上例中即在内部匿名类中将其重载。

然后拿到该线程对应的 ThreadLocalMap 对象，若该对象不为 null，则直接将该 ThreadLocal 对象与对应实例初始值的映射添加进该线程的 ThreadLocalMap中。若为 null，则先创建该 ThreadLocalMap 对象再将映射添加其中。

这里并不需要考虑 ThreadLocalMap 的线程安全问题。因为每个线程有且只有一个 ThreadLocalMap 对象，并且只有该线程自己可以访问它，其它线程不会访问该 ThreadLocalMap，也即该对象不会在多个线程中共享，也就不存在线程安全的问题。

##### 5.2.3.4 设置实例

除了通过`initialValue()`方法设置实例的初始值，还可通过 set 方法设置线程内实例的值，如下所示。

```
public void set(T value) {
  Thread t = Thread.currentThread();
  ThreadLocalMap map = getMap(t);
  if (map != null)
    map.set(this, value);
  else
    createMap(t, value);
}
```

该方法先获取该线程的 ThreadLocalMap 对象，然后直接将 ThreadLocal 对象（即代码中的 this）与目标实例的映射添加进 ThreadLocalMap 中。当然，如果映射已经存在，就直接覆盖。另外，如果获取到的 ThreadLocalMap 为 null，则先创建该 ThreadLocalMap 对象。

##### 5.2.3.5 防止内存泄漏

对于已经不再被使用且已被回收的 ThreadLocal 对象，它在每个线程内对应的实例由于被线程的 ThreadLocalMap 的 Entry 强引用，无法被回收，可能会造成内存泄漏。

针对该问题，ThreadLocalMap 的 set 方法中，通过 replaceStaleEntry 方法将所有键为 null 的 Entry 的值设置为 null，从而使得该值可被回收。另外，会在 rehash 方法中通过 expungeStaleEntry 方法将键和值为 null 的 Entry 设置为 null 从而使得该 Entry 可被回收。

```
private void set(ThreadLocal<?> key, Object value) {
  Entry[] tab = table;
  int len = tab.length;
  int i = key.threadLocalHashCode & (len-1);

  for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
    ThreadLocal<?> k = e.get();
    if (k == key) {
      e.value = value;
      return;
    }
    if (k == null) {
      replaceStaleEntry(key, value, i);
      return;
    }
  }
  tab[i] = new Entry(key, value);
  int sz = ++size;
  if (!cleanSomeSlots(i, sz) && sz >= threshold)
    rehash();
}
```

##### 5.2.3.6 ThreadLocal正确的使用方法

- 每次使用完ThreadLocal都调用它的remove()方法清除数据
- 将ThreadLocal变量定义成private static，这样就一直存在ThreadLocal的强引用，也就能保证任何时候都能通过ThreadLocal的弱引用访问到Entry的value值，进而清除掉 。



### 5.3. ThreadLocal 的误区

> 示例摘自：[《Java 业务开发常见错误 100 例》](https://time.geekbang.org/column/intro/100047701)

ThreadLocal 适用于变量在线程间隔离，而在方法或类间共享的场景。

前文提到，ThreadLocal 是线程隔离的，那么是不是使用 ThreadLocal 就一定高枕无忧呢？

#### 5.3.1.ThreadLocal 错误案例

使用 Spring Boot 创建一个 Web 应用程序，使用 ThreadLocal 存放一个 Integer 的值，来暂且代表需要在线程中保存的用户信息，这个值初始是 null。

```
    private ThreadLocal<Integer> currentUser = ThreadLocal.withInitial(() -> null);

    @GetMapping("wrong")
    public Map<String, String> wrong(@RequestParam("id") Integer userId) {
        //设置用户信息之前先查询一次ThreadLocal中的用户信息
        String before = Thread.currentThread().getName() + ":" + currentUser.get();
        //设置用户信息到ThreadLocal
        currentUser.set(userId);
        //设置用户信息之后再查询一次ThreadLocal中的用户信息
        String after = Thread.currentThread().getName() + ":" + currentUser.get();
        //汇总输出两次查询结果
        Map<String, String> result = new HashMap<>();
        result.put("before", before);
        result.put("after", after);
        return result;
    }
```

【预期】从代码逻辑来看，我们预期第一次获取的值始终应该是 null。

【实际】

为了方便复现，将 Tomcat 工作线程设为 1：

```
server.tomcat.max-threads=1
```

当访问 id = 1 时，符合预期

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303733313131313835342e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322211301.png)

当访问 id = 2 时，before 的应答不是 null，而是 1，不符合预期。

【分析】实际情况和预期存在偏差。Spring Boot 程序运行在 Tomcat 中，执行程序的线程是 Tomcat 的工作线程，而 Tomcat 的工作线程是基于线程池的。**线程池会重用固定的几个线程，一旦线程重用，那么很可能首次从** **ThreadLocal 获取的值是之前其他用户的请求遗留的值。这时，ThreadLocal 中的用户信息就是其他用户的信息。并不能认为没有显式开启多线程就不会有线程安全问题**。使用类似 ThreadLocal 工具来存放一些数据时，需要特别注意在代码运行完后，显式地去清空设置的数据。

#### 5.3.2.ThreadLocal 错误案例修正

```
    @GetMapping("right")
    public Map<String, String> right(@RequestParam("id") Integer userId) {
        String before = Thread.currentThread().getName() + ":" + currentUser.get();
        currentUser.set(userId);
        try {
            String after = Thread.currentThread().getName() + ":" + currentUser.get();
            Map<String, String> result = new HashMap<>();
            result.put("before", before);
            result.put("after", after);
            return result;
        } finally {
            //在finally代码块中删除ThreadLocal中的数据，确保数据不串
            currentUser.remove();
        }
    }
```

### 5.4. InheritableThreadLocal

`InheritableThreadLocal` 类是 `ThreadLocal` 类的子类。

`ThreadLocal` 中每个线程拥有它自己独占的数据。与 `ThreadLocal` 不同的是，`InheritableThreadLocal` 允许一个线程以及该线程创建的所有子线程都可以访问它保存的数据。

> 原理参考：[Java 多线程：InheritableThreadLocal 实现原理](https://blog.csdn.net/ni357103403/article/details/51970748)

### 5.5.总结

- ThreadLocal 并不解决线程间共享数据的问题
- ThreadLocal 通过隐式的在不同线程内创建独立实例副本避免了实例线程安全的问题
- 每个线程持有一个 Map 并维护了 ThreadLocal 对象与具体实例的映射，该 Map 由于只被持有它的线程访问，故不存在线程安全以及锁的问题
- ThreadLocalMap 的 Entry 对 ThreadLocal 的引用为弱引用，避免了 ThreadLocal 对象无法被回收的问题
- ThreadLocalMap 的 set 方法通过调用 replaceStaleEntry 方法回收键为 null 的 Entry 对象的值（即为具体实例）以及 Entry 对象本身从而防止内存泄漏
- ThreadLocal 适用于变量在线程间隔离且在方法间共享的场景

## 6.TimeUnit枚举

TimeUnit是java.util.concurrent包下面的一个枚举类，TimeUnit提供了可读性更好的线程暂停操作。

在JDK5之前，一般我们暂停线程是这样写的：

```
Thread.sleep（2400000）//可读性差
```

可读性相当的差，一眼看去，不知道睡了多久；

在JDK5之后，我们可以这样写：

```
 TimeUnit.SECONDS.sleep(4);
 TimeUnit.MINUTES.sleep(4);
 TimeUnit.HOURS.sleep(1);
 TimeUnit.DAYS.sleep(1);
```

清晰明了；

另外，TimeUnit还提供了便捷方法用于把时间转换成不同单位，例如，如果你想把秒转换成毫秒，你可以使用下面代码

```
TimeUnit.SECONDS.toMillis(44);// 44,000
```




## 面试题

### Java中提供了synchronized，为什么还要提供Lock呢？

**再造轮子？**

既然JVM中提供了synchronized关键字来保证只有一个线程能够访问同步代码块，为何还要提供Lock接口呢？这是在重复造轮子吗？Java的设计者们为何要这样做呢？让我们一起带着疑问往下看。

**为何提供Lock接口？**

很多小伙伴可能会听说过，在Java 1.5版本中，synchronized的性能不如Lock，但在Java 1.6版本之后，synchronized做了很多优化，性能提升了不少。那既然synchronized关键字的性能已经提升了，那为何还要使用Lock呢？

如果我们向更深层次思考的话，就不难想到了：我们使用synchronized加锁是无法主动释放锁的，这就会涉及到死锁的问题。

**死锁问题**

如果要发生死锁，则必须存在以下四个必要条件，四者缺一不可。

![20200916002614352](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324214505.jpg)

- **互斥条件**

  在一段时间内某资源仅为一个线程所占有。此时若有其他线程请求该资源，则请求线程只能等待。

- **不可剥夺条件**

  线程所获得的资源在未使用完毕之前，不能被其他线程强行夺走，即只能由获得该资源的线程自己来释放（只能是主动释放)。

- **请求与保持条件**

  线程已经保持了至少一个资源，但又提出了新的资源请求，而该资源已被其他线程占有，此时请求线程被阻塞，但对自己已获得的资源保持不放。

- **循环等待条件**

  在发生死锁时必然存在一个进程等待队列{P1,P2,…,Pn},其中P1等待P2占有的资源，P2等待P3占有的资源，…，Pn等待P1占有的资源，形成一个进程等待环路，环路中每一个进程所占有的资源同时被另一个申请，也就是前一个进程占有后一个进程所深情地资源。

**synchronized的局限性**

如果我们的程序使用synchronized关键字发生了死锁时，synchronized关键是是无法破坏“不可剥夺”这个死锁的条件的。这是因为synchronized申请资源的时候， 如果申请不到， 线程直接进入阻塞状态了， 而线程进入阻塞状态， 啥都干不了， 也释放不了线程已经占有的资源。

然而，在大部分场景下，我们都是希望“不可剥夺”这个条件能够被破坏。也就是说对于“不可剥夺”这个条件，占用部分资源的线程进一步申请其他资源时， 如果申请不到， 可以主动释放它占有的资源， 这样不可剥夺这个条件就破坏掉了。

如果我们自己重新设计锁来解决synchronized的问题，我们该如何设计呢？

**解决问题**

了解了synchronized的局限性之后，如果是让我们自己实现一把同步锁，我们该如何设计呢？也就是说，我们在设计锁的时候，要如何解决synchronized的局限性问题呢？这里，我觉得可以从三个方面来思考这个问题。

![20200916002629808](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210324214538.jpg)

1. **能够响应中断**

   synchronized的问题是， 持有锁A后， 如果尝试获取锁B失败， 那么线程就进入阻塞状态， 一旦发生死锁， 就没有任何机会来唤醒阻塞的线程。 但如果阻塞状态的线程能够响应中断信号， 也就是说当我们给阻塞的线程发送中断信号的时候， 能够唤醒它， 那它就有机会释放曾经持有的锁A。 这样就破坏了不可剥夺条件了。

2. **支持超时**

    如果线程在一段时间之内没有获取到锁， 不是进入阻塞状态， 而是返回一个错误， 那这个线程也有机会释放曾经持有的锁。 这样也能破坏不可剥夺条件。

3. **非阻塞地获取锁**

    如果尝试获取锁失败， 并不进入阻塞状态， 而是直接返回， 那这个线程也有机会释放曾经持有的锁。 这样也能破坏不可剥夺条件。

体现在Lock接口上，就是Lock接口提供的三个方法，如下所示。

```java
// 支持中断的API
void lockInterruptibly() throws InterruptedException;
// 支持超时的API
boolean tryLock(long time, TimeUnit unit) throws InterruptedException;
// 支持非阻塞获取锁的API
boolean tryLock();
```

- `lockInterruptibly()`

  支持中断。

- `tryLock()`

  tryLock()方法是有返回值的，它表示用来尝试获取锁，如果获取成功，则返回true，如果获取失败（即锁已被其他线程获取），则返回false，也就说这个方法无论如何都会立即返回。在拿不到锁时不会一直在那等待。

- `tryLock(long time, TimeUnit unit)`

  tryLock(long time, TimeUnit unit)方法和tryLock()方法是类似的，只不过区别在于这个方法在拿不到锁时会等待一定的时间，在时间期限之内如果还拿不到锁，就返回false。如果一开始拿到锁或者在等待期间内拿到了锁，则返回true。

也就是说，对于死锁问题，Lock能够破坏不可剥夺的条件，例如，我们下面的程序代码就破坏了死锁的不可剥夺的条件。

```java
public class TansferAccount{
    private Lock thisLock = new ReentrantLock();
    private Lock targetLock = new ReentrantLock();
    //账户的余额
    private Integer balance;
    //转账操作
    public void transfer(TansferAccount target, Integer transferMoney){
        boolean isThisLock = thisLock.tryLock();
        if(isThisLock){
            try{
                boolean isTargetLock = targetLock.tryLock();
                if(isTargetLock){
                    try{
                         if(this.balance >= transferMoney){
                            this.balance -= transferMoney;
                            target.balance += transferMoney;
                        }   
                    }finally{
                        targetLock.unlock
                    }
                }
            }finally{
                thisLock.unlock();
            }
        }
    }
}
```

例外，Lock下面有一个ReentrantLock，而ReentrantLock支持公平锁和非公平锁。

在使用ReentrantLock的时候， ReentrantLock中有两个构造函数， 一个是无参构造函数， 一个是传入fair参数的构造函数。 fair参数代表的是锁的公平策略， 如果传入true就表示需要构造一个公平锁， 反之则表示要构造一个非公平锁。如下代码片段所示。

```java
//无参构造函数： 默认非公平锁
public ReentrantLock() {
	sync = new NonfairSync();
} 
//根据公平策略参数创建锁
public ReentrantLock(boolean fair){
	sync = fair ? new FairSync() : new NonfairSync();
}
```

锁的实现在本质上都对应着一个入口等待队列， 如果一个线程没有获得锁， 就会进入等待队列， 当有线程释放锁的时候， 就需要从等待队列中唤醒一个等待的线程。 如果是公平锁， 唤醒的策略就是谁等待的时间长， 就唤醒谁， 很公平； 如果是非公平锁， 则不提供这个公平保证， 有可能等待时间短的线程反而先被唤醒。 而Lock是支持公平锁的，synchronized不支持公平锁。

最后，值得注意的是，在使用Lock加锁时，一定要在finally{}代码块中释放锁，例如，下面的代码片段所示。

```java
try{
    lock.lock();
}finally{
    lock.unlock();
}
```









