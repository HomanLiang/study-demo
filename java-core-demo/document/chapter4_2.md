[toc]



# Java 线程基础

## 1. 线程简介

### 1.1. 什么是进程

简言之，**进程可视为一个正在运行的程序**。它是系统运行程序的基本单位，因此进程是动态的。进程是具有一定独立功能的程序关于某个数据集合上的一次运行活动。进程是操作系统进行资源分配的基本单位。

### 1.2. 什么是线程

线程是操作系统进行调度的基本单位。线程也叫轻量级进程（Light Weight Process），在一个进程里可以创建多个线程，这些线程都拥有各自的计数器、堆栈和局部变量等属性，并且能够访问共享的内存变量。

### 1.3. 进程和线程的区别

- 一个程序至少有一个进程，一个进程至少有一个线程。
- 线程比进程划分更细，所以执行开销更小，并发性更高。
- 进程是一个实体，拥有独立的资源；而同一个进程中的多个线程共享进程的资源。

**加强理解，做个简单的比喻：进程=火车，线程=车厢**

- 线程在进程下行进（单纯的车厢无法运行）
- 一个进程可以包含多个线程（一辆火车可以有多个车厢）
- 不同进程间数据很难共享（一辆火车上的乘客很难换到另外一辆火车，比如站点换乘）
- 同一进程下不同线程间数据很易共享（A车厢换到B车厢很容易）
- 进程要比线程消耗更多的计算机资源（采用多列火车相比多个车厢更耗资源）
- 进程间不会相互影响，一个线程挂掉将导致整个进程挂掉（一列火车不会影响到另外一列火车，但是如果一列火车上中间的一节车厢着火了，将影响到所有车厢）
- 进程可以拓展到多机，进程最多适合多核（不同火车可以开在多个轨道上，同一火车的车厢不能在行进的不同的轨道上）
- 进程使用的内存地址可以上锁，即一个线程使用某些共享内存时，其他线程必须等它结束，才能使用这一块内存。（比如火车上的洗手间）－"互斥锁"
- 进程使用的内存地址可以限定使用量（比如火车上的餐厅，最多只允许多少人进入，如果满了需要在门口等，等有人出来了才能进去）－“信号量”

## 2. 创建线程

创建线程有三种方式：

- 继承 `Thread` 类
- 实现 `Runnable` 接口
- 实现 `Callable` 接口

### 2.1. Thread

通过继承 `Thread` 类创建线程的步骤：

1. 定义 `Thread` 类的子类，并覆写该类的 `run` 方法。`run` 方法的方法体就代表了线程要完成的任务，因此把 `run` 方法称为执行体。
2. 创建 `Thread` 子类的实例，即创建了线程对象。
3. 调用线程对象的 `start` 方法来启动该线程。

```
public class ThreadDemo {

    public static void main(String[] args) {
        // 实例化对象
        MyThread tA = new MyThread("Thread 线程-A");
        MyThread tB = new MyThread("Thread 线程-B");
        // 调用线程主体
        tA.start();
        tB.start();
    }

    static class MyThread extends Thread {

        private int ticket = 5;

        MyThread(String name) {
            super(name);
        }

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

### 2.2. Runnable

**实现 `Runnable` 接口优于继承 `Thread` 类**，因为：

- Java 不支持多重继承，所有的类都只允许继承一个父类，但可以实现多个接口。如果继承了 `Thread` 类就无法继承其它类，这不利于扩展。
- 类可能只要求可执行就行，继承整个 `Thread` 类开销过大。

通过实现 `Runnable` 接口创建线程的步骤：

1. 定义 `Runnable` 接口的实现类，并覆写该接口的 `run` 方法。该 `run` 方法的方法体同样是该线程的线程执行体。
2. 创建 `Runnable` 实现类的实例，并以此实例作为 `Thread` 的 target 来创建 `Thread` 对象，该 `Thread` 对象才是真正的线程对象。
3. 调用线程对象的 `start` 方法来启动该线程。

```
public class RunnableDemo {

    public static void main(String[] args) {
        // 实例化对象
        Thread tA = new Thread(new MyThread(), "Runnable 线程-A");
        Thread tB = new Thread(new MyThread(), "Runnable 线程-B");
        // 调用线程主体
        tA.start();
        tB.start();
    }

    static class MyThread implements Runnable {

        private int ticket = 5;

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

### 2.3. Callable、Future、FutureTask

**继承 Thread 类和实现 Runnable 接口这两种创建线程的方式都没有返回值**。所以，线程执行完后，无法得到执行结果。但如果期望得到执行结果该怎么做？

为了解决这个问题，Java 1.5 后，提供了 `Callable` 接口和 `Future` 接口，通过它们，可以在线程执行结束后，返回执行结果。

#### 2.3.1.Callable

Callable 接口只声明了一个方法，这个方法叫做 call()：

```
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;
}
```

那么怎么使用 Callable 呢？一般情况下是配合 ExecutorService 来使用的，在 ExecutorService 接口中声明了若干个 submit 方法的重载版本：

```
<T> Future<T> submit(Callable<T> task);
<T> Future<T> submit(Runnable task, T result);
Future<?> submit(Runnable task);
```

第一个 submit 方法里面的参数类型就是 Callable。

#### 2.3.2.Future

Future 就是对于具体的 Callable 任务的执行结果进行取消、查询是否完成、获取结果。必要时可以通过 get 方法获取执行结果，该方法会阻塞直到任务返回结果。

```
public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException;
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```

#### 2.3.3.FutureTask

FutureTask 类实现了 RunnableFuture 接口，RunnableFuture 继承了 Runnable 接口和 Future 接口。

所以，FutureTask 既可以作为 Runnable 被线程执行，又可以作为 Future 得到 Callable 的返回值。

```
public class FutureTask<V> implements RunnableFuture<V> {
    // ...
    public FutureTask(Callable<V> callable) {}
    public FutureTask(Runnable runnable, V result) {}
}

public interface RunnableFuture<V> extends Runnable, Future<V> {
    void run();
}
```

事实上，FutureTask 是 Future 接口的一个唯一实现类。

#### 2.3.4.Callable + Future + FutureTask 示例

通过实现 `Callable` 接口创建线程的步骤：

1. 创建 `Callable` 接口的实现类，并实现 `call` 方法。该 `call` 方法将作为线程执行体，并且有返回值。
2. 创建 `Callable` 实现类的实例，使用 `FutureTask` 类来包装 `Callable` 对象，该 `FutureTask` 对象封装了该 `Callable` 对象的 `call` 方法的返回值。
3. 使用 `FutureTask` 对象作为 `Thread` 对象的 target 创建并启动新线程。
4. 调用 `FutureTask` 对象的 `get` 方法来获得线程执行结束后的返回值。

```
public class CallableDemo {

    public static void main(String[] args) {
        Callable<Long> callable = new MyThread();
        FutureTask<Long> future = new FutureTask<>(callable);
        new Thread(future, "Callable 线程").start();
        try {
            System.out.println("任务耗时：" + (future.get() / 1000000) + "毫秒");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    static class MyThread implements Callable<Long> {

        private int ticket = 10000;

        @Override
        public Long call() {
            long begin = System.nanoTime();
            while (ticket > 0) {
                System.out.println(Thread.currentThread().getName() + " 卖出了第 " + ticket + " 张票");
                ticket--;
            }

            long end = System.nanoTime();
            return (end - begin);
        }

    }

}
```

### 2.4.三种创建方式比较

**2.4.1.采用实现Runnable、Callable接口的方式创建多线程时**

**优势是：**

线程类只是实现了Runnable接口或Callable接口，还可以继承其他类。

在这种方式下，多个线程可以共享同一个target对象，所以非常适合多个相同线程来处理同一份资源的情况，从而可以将CPU、代码和数据分开，形成清晰的模型，较好地体现了面向对象的思想。

**劣势是：**

编程稍微复杂，如果要访问当前线程，则必须使用 `Thread.currentThread()` 方法。

**2.4.2.使用继承Thread类的方式创建多线程时**

**优势是：**编写简单，如果需要访问当前线程，则无需使用 `Thread.currentThread()` 方法，直接使用this即可获得当前线程。

**劣势是：**线程类已经继承了Thread类，所以不能再继承其他父类。

**2.4.3.Runnable和Callable的区别**

- Callable规定（重写）的方法是call()，Runnable规定（重写）的方法是run()。

- Callable的任务执行后可返回值，而Runnable的任务是不能返回值的。

- call方法可以抛出异常，run方法不可以。

- 运行Callable任务可以拿到一个Future对象，表示异步计算的结果。它提供了检查计算是否完成的方法，以等待计算的完成，并检索计算的结果。通过Future对象可以了解任务执行情况，可取消任务的执行，还可获取执行结果。



## 3. 线程基本用法

线程（`Thread`）基本方法清单：

| 方法            | 描述                                                         |
| --------------- | ------------------------------------------------------------ |
| `run`           | 线程的执行实体。                                             |
| `start`         | 线程的启动方法。                                             |
| `currentThread` | 返回对当前正在执行的线程对象的引用。                         |
| `setName`       | 设置线程名称。                                               |
| `getName`       | 获取线程名称。                                               |
| `setPriority`   | 设置线程优先级。Java 中的线程优先级的范围是 [1,10]，一般来说，高优先级的线程在运行时会具有优先权。可以通过 `thread.setPriority(Thread.MAX_PRIORITY)` 的方式设置，默认优先级为 5。 |
| `getPriority`   | 获取线程优先级。                                             |
| `setDaemon`     | 设置线程为守护线程。                                         |
| `isDaemon`      | 判断线程是否为守护线程。                                     |
| `isAlive`       | 判断线程是否启动。                                           |
| `interrupt`     | 中断另一个线程的运行状态。                                   |
| `interrupted`   | 测试当前线程是否已被中断。通过此方法可以清除线程的中断状态。换句话说，如果要连续调用此方法两次，则第二次调用将返回 false（除非当前线程在第一次调用清除其中断状态之后且在第二次调用检查其状态之前再次中断）。 |
| `join`          | 可以使一个线程强制运行，线程强制运行期间，其他线程无法运行，必须等待此线程完成之后才可以继续执行。 |
| `Thread.sleep`  | 静态方法。将当前正在执行的线程休眠。                         |
| `Thread.yield`  | 静态方法。将当前正在执行的线程暂停，让其他线程执行。         |

### 3.1. 线程休眠

**使用 `Thread.sleep` 方法可以使得当前正在执行的线程进入休眠状态。**

使用 `Thread.sleep` 需要向其传入一个整数值，这个值表示线程将要休眠的毫秒数。

`Thread.sleep` 方法可能会抛出 `InterruptedException`，因为异常不能跨线程传播回 `main` 中，因此必须在本地进行处理。线程中抛出的其它异常也同样需要在本地进行处理。

```
public class ThreadSleepDemo {

    public static void main(String[] args) {
        new Thread(new MyThread("线程A", 500)).start();
        new Thread(new MyThread("线程B", 1000)).start();
        new Thread(new MyThread("线程C", 1500)).start();
    }

    static class MyThread implements Runnable {

        /** 线程名称 */
        private String name;

        /** 休眠时间 */
        private int time;

        private MyThread(String name, int time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public void run() {
            try {
                // 休眠指定的时间
                Thread.sleep(this.time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(this.name + "休眠" + this.time + "毫秒。");
        }

    }

}
```

### 3.2. 线程礼让

`Thread.yield` 方法的调用声明了当前线程已经完成了生命周期中最重要的部分，可以切换给其它线程来执行 。

该方法只是对线程调度器的一个建议，而且也只是建议具有相同优先级的其它线程可以运行。

```
public class ThreadYieldDemo {

    public static void main(String[] args) {
        MyThread t = new MyThread();
        new Thread(t, "线程A").start();
        new Thread(t, "线程B").start();
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + "运行，i = " + i);
                if (i == 2) {
                    System.out.print("线程礼让：");
                    Thread.yield();
                }
            }
        }
    }
}
```

### 3.3. 终止线程

> **`Thread` 中的 `stop` 方法有缺陷，已废弃**。
>
> 使用 `Thread.stop` 停止线程会导致它解锁所有已锁定的监视器（由于未经检查的 `ThreadDeath` 异常会在堆栈中传播，这是自然的结果）。 如果先前由这些监视器保护的任何对象处于不一致状态，则损坏的对象将对其他线程可见，从而可能导致任意行为。
>
> stop() 方法会真的杀死线程，不给线程喘息的机会，如果线程持有 ReentrantLock 锁，被 stop() 的线程并不会自动调用 ReentrantLock 的 unlock() 去释放锁，那其他线程就再也没机会获得 ReentrantLock 锁，这实在是太危险了。所以该方法就不建议使用了，类似的方法还有 suspend() 和 resume() 方法，这两个方法同样也都不建议使用了，所以这里也就不多介绍了。`Thread.stop` 的许多用法应由仅修改某些变量以指示目标线程应停止运行的代码代替。 目标线程应定期检查此变量，如果该变量指示要停止运行，则应按有序方式从其运行方法返回。如果目标线程等待很长时间（例如，在条件变量上），则应使用中断方法来中断等待。

当一个线程运行时，另一个线程可以直接通过 `interrupt` 方法中断其运行状态。

```
public class ThreadInterruptDemo {

    public static void main(String[] args) {
        MyThread mt = new MyThread(); // 实例化Runnable子类对象
        Thread t = new Thread(mt, "线程"); // 实例化Thread对象
        t.start(); // 启动线程
        try {
            Thread.sleep(2000); // 线程休眠2秒
        } catch (InterruptedException e) {
            System.out.println("3、休眠被终止");
        }
        t.interrupt(); // 中断线程执行
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            System.out.println("1、进入run()方法");
            try {
                Thread.sleep(10000); // 线程休眠10秒
                System.out.println("2、已经完成了休眠");
            } catch (InterruptedException e) {
                System.out.println("3、休眠被终止");
                return; // 返回调用处
            }
            System.out.println("4、run()方法正常结束");
        }
    }
}
```

如果一个线程的 `run` 方法执行一个无限循环，并且没有执行 `sleep` 等会抛出 `InterruptedException` 的操作，那么调用线程的 `interrupt` 方法就无法使线程提前结束。

但是调用 `interrupt` 方法会设置线程的中断标记，此时调用 `interrupted` 方法会返回 `true`。因此可以在循环体中使用 `interrupted` 方法来判断线程是否处于中断状态，从而提前结束线程。

安全地终止线程有两种方法：

- 定义 `volatile` 标志位，在 `run` 方法中使用标志位控制线程终止
- 使用 `interrupt` 方法和 `Thread.interrupted` 方法配合使用来控制线程终止

【示例】使用 `volatile` 标志位控制线程终止

```
public class ThreadStopDemo2 {

    public static void main(String[] args) throws Exception {
        MyTask task = new MyTask();
        Thread thread = new Thread(task, "MyTask");
        thread.start();
        TimeUnit.MILLISECONDS.sleep(50);
        task.cancel();
    }

    private static class MyTask implements Runnable {

        private volatile boolean flag = true;

        private volatile long count = 0L;

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 线程启动");
            while (flag) {
                System.out.println(count++);
            }
            System.out.println(Thread.currentThread().getName() + " 线程终止");
        }

        /**
         * 通过 volatile 标志位来控制线程终止
         */
        public void cancel() {
            flag = false;
        }

    }

}
```

【示例】使用 `interrupt` 方法和 `Thread.interrupted` 方法配合使用来控制线程终止

```
public class ThreadStopDemo3 {

    public static void main(String[] args) throws Exception {
        MyTask task = new MyTask();
        Thread thread = new Thread(task, "MyTask");
        thread.start();
        TimeUnit.MILLISECONDS.sleep(50);
        thread.interrupt();
    }

    private static class MyTask implements Runnable {

        private volatile long count = 0L;

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 线程启动");
            // 通过 Thread.interrupted 和 interrupt 配合来控制线程终止
            while (!Thread.interrupted()) {
                System.out.println(count++);
            }
            System.out.println(Thread.currentThread().getName() + " 线程终止");
        }
    }
}
```

### 3.4. 守护线程

**什么是守护线程？**

- 守护线程（Daemon Thread）是在后台执行并且不会阻止 JVM 终止的线程。当所有非守护线程结束时，程序也就终止，同时会杀死所有守护线程。
- 与守护线程（Daemon Thread）相反的，叫用户线程（User Thread），也就是非守护线程。

**为什么需要守护线程？**

- 守护线程的优先级比较低，用于为系统中的其它对象和线程提供服务。典型的应用就是垃圾回收器。

**如何使用守护线程？**

- 可以使用 `isDaemon` 方法判断线程是否为守护线程。

- 可以使用`setDaemon`方法设置线程为守护线程。

  - 正在运行的用户线程无法设置为守护线程，所以 `setDaemon` 必须在 `thread.start` 方法之前设置，否则会抛出 `llegalThreadStateException` 异常；
  - 一个守护线程创建的子线程依然是守护线程。
  - 不要认为所有的应用都可以分配给守护线程来进行服务，比如读写操作或者计算逻辑。

```
public class ThreadDaemonDemo {

    public static void main(String[] args) {
        Thread t = new Thread(new MyThread(), "线程");
        t.setDaemon(true); // 此线程在后台运行
        System.out.println("线程 t 是否是守护进程：" + t.isDaemon());
        t.start(); // 启动线程
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            while (true) {
                System.out.println(Thread.currentThread().getName() + "在运行。");
            }
        }
    }
}
```

> 参考阅读：[Java 中守护线程的总结](https://blog.csdn.net/shimiso/article/details/8964414)

## 4. 线程通信

> 当多个线程可以一起工作去解决某个问题时，如果某些部分必须在其它部分之前完成，那么就需要对线程进行协调。

### 4.1. wait/notify/notifyAll

- `wait` - `wait` 会自动释放当前线程占有的对象锁，并请求操作系统挂起当前线程，**让线程从 `Running` 状态转入 `Waiting` 状态**，等待 `notify` / `notifyAll` 来唤醒。如果没有释放锁，那么其它线程就无法进入对象的同步方法或者同步控制块中，那么就无法执行 `notify` 或者 `notifyAll` 来唤醒挂起的线程，造成死锁。
- `notify` - 唤醒一个正在 `Waiting` 状态的线程，并让它拿到对象锁，具体唤醒哪一个线程由 JVM 控制 。
- `notifyAll` - 唤醒所有正在 `Waiting` 状态的线程，接下来它们需要竞争对象锁。

> 注意：
>
> - **`wait`、`notify`、`notifyAll` 都是 `Object` 类中的方法**，而非 `Thread`。
> - **`wait`、`notify`、`notifyAll` 只能用在 `synchronized` 方法或者 `synchronized` 代码块中使用，否则会在运行时抛出 `IllegalMonitorStateException`**。
>
> 为什么 `wait`、`notify`、`notifyAll` 不定义在 `Thread` 中？为什么 `wait`、`notify`、`notifyAll` 要配合 `synchronized` 使用？
>
> 首先，需要了解几个基本知识点：
>
> - 每一个 Java 对象都有一个与之对应的 **监视器（monitor）**
> - 每一个监视器里面都有一个 **对象锁** 、一个 **等待队列**、一个 **同步队列**
>
> 了解了以上概念，我们回过头来理解前面两个问题。
>
> 为什么这几个方法不定义在 `Thread` 中？
>
> 由于每个对象都拥有对象锁，让当前线程等待某个对象锁，自然应该基于这个对象（`Object`）来操作，而非使用当前线程（`Thread`）来操作。因为当前线程可能会等待多个线程的锁，如果基于线程（`Thread`）来操作，就非常复杂了。
>
> 为什么 `wait`、`notify`、`notifyAll` 要配合 `synchronized` 使用？
>
> 如果调用某个对象的 `wait` 方法，当前线程必须拥有这个对象的对象锁，因此调用 `wait` 方法必须在 `synchronized` 方法和 `synchronized`代码块中。

生产者、消费者模式是 `wait`、`notify`、`notifyAll` 的一个经典使用案例：

```
public class ThreadWaitNotifyDemo02 {

    private static final int QUEUE_SIZE = 10;
    private static final PriorityQueue<Integer> queue = new PriorityQueue<>(QUEUE_SIZE);

    public static void main(String[] args) {
        new Producer("生产者A").start();
        new Producer("生产者B").start();
        new Consumer("消费者A").start();
        new Consumer("消费者B").start();
    }

    static class Consumer extends Thread {

        Consumer(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    while (queue.size() == 0) {
                        try {
                            System.out.println("队列空，等待数据");
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            queue.notifyAll();
                        }
                    }
                    queue.poll(); // 每次移走队首元素
                    queue.notifyAll();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() + " 从队列取走一个元素，队列当前有：" + queue.size() + "个元素");
                }
            }
        }
    }

    static class Producer extends Thread {

        Producer(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (true) {
                synchronized (queue) {
                    while (queue.size() == QUEUE_SIZE) {
                        try {
                            System.out.println("队列满，等待有空余空间");
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            queue.notifyAll();
                        }
                    }
                    queue.offer(1); // 每次插入一个元素
                    queue.notifyAll();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() + " 向队列取中插入一个元素，队列当前有：" + queue.size() + "个元素");
                }
            }
        }
    }
}
```

### 4.2. join

在线程操作中，可以使用 `join` 方法让一个线程强制运行，线程强制运行期间，其他线程无法运行，必须等待此线程完成之后才可以继续执行。

```
public class ThreadJoinDemo {

    public static void main(String[] args) {
        MyThread mt = new MyThread(); // 实例化Runnable子类对象
        Thread t = new Thread(mt, "mythread"); // 实例化Thread对象
        t.start(); // 启动线程
        for (int i = 0; i < 50; i++) {
            if (i > 10) {
                try {
                    t.join(); // 线程强制运行
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Main 线程运行 --> " + i);
        }
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 50; i++) {
                System.out.println(Thread.currentThread().getName() + " 运行，i = " + i); // 取得当前线程的名字
            }
        }
    }
}
```

### 4.3. 管道

管道输入/输出流和普通的文件输入/输出流或者网络输入/输出流不同之处在于，它主要用于线程之间的数据传输，而传输的媒介为内存。 管道输入/输出流主要包括了如下 4 种具体实现：`PipedOutputStream`、`PipedInputStream`、`PipedReader` 和 `PipedWriter`，前两种面向字节，而后两种面向字符。

```
public class Piped {

    public static void main(String[] args) throws Exception {
        PipedWriter out = new PipedWriter();
        PipedReader in = new PipedReader();
        // 将输出流和输入流进行连接，否则在使用时会抛出IOException
        out.connect(in);
        Thread printThread = new Thread(new Print(in), "PrintThread");
        printThread.start();
        int receive = 0;
        try {
            while ((receive = System.in.read()) != -1) {
                out.write(receive);
            }
        } finally {
            out.close();
        }
    }

    static class Print implements Runnable {

        private PipedReader in;

        Print(PipedReader in) {
            this.in = in;
        }

        public void run() {
            int receive = 0;
            try {
                while ((receive = in.read()) != -1) {
                    System.out.print((char) receive);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 4.4 sleep

sleep方法的作用是让当前线程暂停指定的时间（毫秒），sleep方法是最简单的方法。唯一需要注意的是其与wait方法的区别。最简单的区别是，wait方法依赖于同步，而sleep方法可以直接调用。而更深层次的区别在于sleep方法只是暂时让出CPU的执行权，并不释放锁。而wait方法则需要释放锁。

**示例**

```java
public class SleepTest {
    public synchronized void sleepMethod(){
        System.out.println("Sleep start-----");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Sleep end-----");
    }

    public synchronized void waitMethod(){
        System.out.println("Wait start-----");
        synchronized (this){
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Wait end-----");
    }

    public static void main(String[] args) {
        final SleepTest test1 = new SleepTest();

        for(int i = 0;i<3;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    test1.sleepMethod();
                }
            }).start();
        }


        try {
            Thread.sleep(10000);//暂停十秒，等上面程序执行完成
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("-----分割线-----");

        final SleepTest test2 = new SleepTest();

        for(int i = 0;i<3;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    test2.waitMethod();
                }
            }).start();
        }
    }
}
```

**执行结果**：

```
Sleep start-----
Sleep end-----
Sleep start-----
Sleep end-----
Sleep start-----
Sleep end-----
-----分割线-----
Wait start-----
Wait start-----
Wait start-----
Wait end-----
Wait end-----
Wait end-----
```

这个结果的区别很明显，通过sleep方法实现的暂停，程序是顺序进入同步块的，只有当上一个线程执行完成的时候，下一个线程才能进入同步方法，sleep暂停期间一直持有monitor对象锁，其他线程是不能进入的。而wait方法则不同，当调用wait方法后，当前线程会释放持有的monitor对象锁，因此，其他线程还可以进入到同步方法，线程被唤醒后，需要竞争锁，获取到锁之后再继续执行。

### 4.5 yield方法

yield方法的作用是暂停当前线程，以便其他线程有机会执行，不过不能指定暂停的时间，并且也不能保证当前线程马上停止。yield方法只是将Running状态转变为Runnable状态。我们还是通过一个例子来演示其使用：

```java
public class YieldTest implements Runnable {
    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0;i<5;i++){
            System.out.println(Thread.currentThread().getName() + ": " + i);
            Thread.yield();
        }
    }

    public static void main(String[] args) {	
        YieldTest runn = new YieldTest();
        Thread t1 = new Thread(runn,"FirstThread");
        Thread t2 = new Thread(runn,"SecondThread");

        t1.start();
        t2.start();

    }
}
```

运行结果如下：

```
FirstThread: 0
SecondThread: 0
FirstThread: 1
SecondThread: 1
FirstThread: 2
SecondThread: 2
FirstThread: 3
SecondThread: 3
FirstThread: 4
SecondThread: 4
```

这个例子就是通过yield方法来实现两个线程的交替执行。不过请注意：这种交替并不一定能得到保证：

- 调度器可能会忽略该方法。
- 使用的时候要仔细分析和测试，确保能达到预期的效果。
- 很少有场景要用到该方法，主要使用的地方是调试和测试。

### 4.6 Condition

Condition是在java 1.5中出现的，它用来替代传统的Object的 `wait()/notify()` 实现线程间的协作，它的使用依赖于 Lock，Condition、Lock 和 Thread 三者之间的关系如下图所示。相比使用Object的 `wait()/notify()`，使用Condition的 `await()/signal()` 这种方式能够更加安全和高效地实现线程间协作。Condition是个接口，基本的方法就是 `await()` 和 `signal()` 方法。Condition依赖于Lock接口，生成一个Condition的基本代码是 `lock.newCondition() `。 必须要注意的是，Condition 的 `await()/signal()` 使用都必须在lock保护之内，也就是说，必须在lock.lock()和lock.unlock之间才可以使用。事实上，Conditon的 `await()/signal()` 与 Object的 `wait()/notify()` 有着天然的对应关系：

- Conditon中的 `await()` 对应Object的 `wait()`；
- Condition中的 `signal()` 对应Object的 `notify()`；
- Condition中的 `signalAll()` 对应Object的 `notifyAll()`。

![20170519154927256](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323220507.png)

使用Condition往往比使用传统的通知等待机制(Object的 `wait()/notify())` 要更灵活、高效，例如，我们可以使用多个Condition实现通知部分线程：

```
// 线程 A
class ThreadA extends Thread {
	private MyService service;
	public ThreadA(MyService service) {
		super();
		this.service = service;
	}
	@Override
	public void run() {
		service.awaitA();
	}
}
// 线程 B
class ThreadB extends Thread {
	public ThreadB(MyService service) {
		super();
		this.service = service;
	}
	@Override
	public void run() {
		service.awaitB();
	}
}

class MyService {
	private Lock lock = new ReentrantLock();
	// 使用多个Condition实现通知部分线程
	public Condition conditionA = lock.newCondition();
	public Condition conditionB = lock.newCondition();

	public void awaitA() {
	    lock.lock();
	    try {
		    System.out.println("begin awaitA时间为" + System.currentTimeMillis()
			    	+ " ThreadName=" + Thread.currentThread().getName());
		    conditionA.await();
		    System.out.println("  end awaitA时间为" + System.currentTimeMillis()
			    	+ " ThreadName=" + Thread.currentThread().getName());
	    } catch (InterruptedException e) {
		    e.printStackTrace();
	    } finally {
		    lock.unlock();
	    }
    }

	public void awaitB() {
	    lock.lock();
		try {
			System.out.println("begin awaitB时间为" + System.currentTimeMillis()
					+ " ThreadName=" + Thread.currentThread().getName());
			conditionB.await();
			System.out.println("  end awaitB时间为" + System.currentTimeMillis()
					+ " ThreadName=" + Thread.currentThread().getName());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			lock.unlock();
		}
	}

	public void signalAll_A() {
		try {
			lock.lock();
			System.out.println("  signalAll_A时间为" + System.currentTimeMillis()
					+ " ThreadName=" + Thread.currentThread().getName());
			conditionA.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void signalAll_B() {
		try {
			lock.lock();
			System.out.println("  signalAll_B时间为" + System.currentTimeMillis()
					+ " ThreadName=" + Thread.currentThread().getName());
			conditionB.signalAll();
		} finally {
			lock.unlock();
		}
	}
}

// 测试
public class Run {
	public static void main(String[] args) throws InterruptedException {
	    MyService service = new MyService();

	    ThreadA a = new ThreadA(service);
	    a.setName("A");
	    a.start();

    	ThreadB b = new ThreadB(service);
	    b.setName("B");
    	b.start();

	    Thread.sleep(3000);
	    service.signalAll_A();
    }
}
```

输出结果如下图所示，我们可以看到只有线程A被唤醒，线程B仍然阻塞。

![多个Condition通知部分线程](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323220605.png)

实际上，**Condition 实现了一种分组机制，将所有对临界资源进行访问的线程进行分组，以便实现线程间更精细化的协作，例如通知部分线程。**我们可以从上面例子的输出结果看出，只有conditionA范围内的线程A被唤醒，而conditionB范围内的线程B仍然阻塞。



## 5. 线程生命周期

![20210102103928](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322210003.png)

`java.lang.Thread.State` 中定义了 **6** 种不同的线程状态，在给定的一个时刻，线程只能处于其中的一个状态。

以下是各状态的说明，以及状态间的联系：

- **新建（New）** - 尚未调用 `start` 方法的线程处于此状态。此状态意味着：**创建的线程尚未启动**。

- **就绪（Runnable）** - 已经调用了 `start` 方法的线程处于此状态。此状态意味着：**线程已经在 JVM 中运行**。但是在操作系统层面，它可能处于运行状态，也可能等待资源调度（例如处理器资源），资源调度完成就进入运行状态。所以该状态的可运行是指可以被运行，具体有没有运行要看底层操作系统的资源调度。

- **阻塞（Blocked）** - 此状态意味着：**线程处于被阻塞状态**。表示线程在等待 `synchronized` 的隐式锁（Monitor lock）。`synchronized` 修饰的方法、代码块同一时刻只允许一个线程执行，其他线程只能等待，即处于阻塞状态。当占用 `synchronized` 隐式锁的线程释放锁，并且等待的线程获得 `synchronized` 隐式锁时，就又会从 `BLOCKED` 转换到 `RUNNABLE` 状态。

- **等待（Waiting）** - 此状态意味着：**线程无限期等待，直到被其他线程显式地唤醒**。 阻塞和等待的区别在于，阻塞是被动的，它是在等待获取 `synchronized` 的隐式锁。而等待是主动的，通过调用 `Object.wait` 等方法进入。

  | 进入方法                                                     | 退出方法                             |
  | ------------------------------------------------------------ | ------------------------------------ |
  | 没有设置 Timeout 参数的 `Object.wait` 方法                   | `Object.notify` / `Object.notifyAll` |
  | 没有设置 Timeout 参数的 `Thread.join` 方法                   | 被调用的线程执行完毕                 |
  | `LockSupport.park` 方法（Java 并发包中的锁，都是基于它实现的） | `LockSupport.unpark`                 |

- **定时等待（Timed waiting）** - 此状态意味着：**无需等待其它线程显式地唤醒，在一定时间之后会被系统自动唤醒**。

  | 进入方法                                                     | 退出方法                                        |
  | ------------------------------------------------------------ | ----------------------------------------------- |
  | `Thread.sleep` 方法                                          | 时间结束                                        |
  | 获得 `synchronized` 隐式锁的线程，调用设置了 Timeout 参数的 `Object.wait` 方法 | 时间结束 / `Object.notify` / `Object.notifyAll` |
  | 设置了 Timeout 参数的 `Thread.join` 方法                     | 时间结束 / 被调用的线程执行完毕                 |
  | `LockSupport.parkNanos` 方法                                 | `LockSupport.unpark`                            |
  | `LockSupport.parkUntil` 方法                                 | `LockSupport.unpark`                            |

- **终止(Terminated)** - 线程执行完 `run` 方法，或者因异常退出了 `run` 方法。此状态意味着：线程结束了生命周期。

## 6. 线程常见问题

### 6.1. sleep、yield、join 方法有什么区别

- `yield`方法

  - `yield` 方法会 **让线程从 `Running` 状态转入 `Runnable` 状态**。
  - 当调用了 `yield` 方法后，只有**与当前线程相同或更高优先级的`Runnable` 状态线程才会获得执行的机会**。

- `sleep`方法

  - `sleep` 方法会 **让线程从 `Running` 状态转入 `Waiting` 状态**。
  - `sleep` 方法需要指定等待的时间，**超过等待时间后，JVM 会将线程从 `Waiting` 状态转入 `Runnable` 状态**。
  - 当调用了 `sleep` 方法后，**无论什么优先级的线程都可以得到执行机会**。
  - `sleep` 方法不会释放“锁标志”，也就是说如果有 `synchronized` 同步块，其他线程仍然不能访问共享数据。

- `join` 方法
  - `join` 方法会 **让线程从 `Running` 状态转入 `Waiting` 状态**。
  - 当调用了 `join` 方法后，**当前线程必须等待调用 `join` 方法的线程结束后才能继续执行**。

### 6.2. 为什么 sleep 和 yield 方法是静态的

`Thread` 类的 `sleep` 和 `yield` 方法将处理 `Running` 状态的线程。

所以在其他处于非 `Running` 状态的线程上执行这两个方法是没有意义的。这就是为什么这些方法是静态的。它们可以在当前正在执行的线程中工作，并避免程序员错误的认为可以在其他非运行线程调用这些方法。

### 6.3. Java 线程是否按照线程优先级严格执行

即使设置了线程的优先级，也**无法保证高优先级的线程一定先执行**。

原因在于线程优先级依赖于操作系统的支持，然而，不同的操作系统支持的线程优先级并不相同，不能很好的和 Java 中线程优先级一一对应。

### 6.4. 一个线程两次调用 start()方法会怎样

Java 的线程是不允许启动两次的，第二次调用必然会抛出 IllegalThreadStateException，这是一种运行时异常，多次调用 start 被认为是编程错误。

### 6.5. `start` 和 `run` 方法有什么区别

**6.5.1 start 方法和 run 方法的比较**

**代码演示:**

```java
/**
 * <p>
 * start() 和 run() 的比较
 * </p>
 *
 * @author 踏雪彡寻梅
 * @version 1.0
 * @date 2020/9/20 - 16:15
 * @since JDK1.8
 */
public class StartAndRunMethod {
    public static void main(String[] args) {
        // run 方法演示
        // 输出: name: main
        // 说明由主线程去执行的, 不符合新建一个线程的本意
        Runnable runnable = () -> {
            System.out.println("name: " + Thread.currentThread().getName());
        };
        runnable.run();

        // start 方法演示
        // 输出: name: Thread-0
        // 说明新建了一个线程, 符合本意
        new Thread(runnable).start();
    }
}
```

从以上示例可以分析出以下两点:

- 直接使用 `run` 方法不会启动一个新线程。(错误方式)
- `start` 方法会启动一个新线程。(正确方式)

**6.5.2 start 方法分析**

**start 方法的含义以及注意事项**

- `start` 方法可以启动一个新线程。

  - 线程对象在初始化之后调用了 `start` 方法之后, 当前线程(通常是主线程)会请求 JVM 虚拟机如果有空闲的话来启动一下这边的这个新线程。
  - 也就是说, 启动一个新线程的本质就是请求 JVM 来运行这个线程。
  - 至于这个线程何时能够运行，并不是简单的由我们能够决定的，而是由线程调度器去决定的。
  - 如果它很忙，即使我们运行了 `start` 方法，也不一定能够立刻的启动线程。
  - 所以说 `start` 方法调用之后，并不意味这个方法已经开始运行了。它可能稍后才会运行，也很有可能很长时间都不会运行，比如说遇到了饥饿的情况。
  - 这也就印证了有些情况下，线程 1 先掉用了 `start` 方法，而线程 2 后调用了 `start` 方法，却发现线程 2 先执行线程 1 后执行的情况。
  - 总结: 调用 `start` 方法的顺序并不能决定真正线程执行的顺序。
  - **注意事项**
    - `start` 方法会牵扯到两个线程。
    - 第一个就是主线程，因为我们必须要有一个主线程或者是其他的线程(哪怕不是主线程)来执行这个 `start` 方法，第二个才是新的线程。
    - 很多情况下会忽略掉为我们创建线程的这个主线程，不要误以为调用了 `start` 就已经是子线程去执行了，这个语句其实是主线程或者说是父线程来执行的，被执行之后才去创建新线程。

- `start` 方法创建新线程的准备工作

  - 首先，它会让自己处于就绪状态。
    - 就绪状态指已经获取到除了 CPU 以外的其他资源, 如已经设置了上下文、栈、线程状态以及 PC(PC 是一个寄存器，PC 指向程序运行的位置) 等。
  - 做完这些准备工作之后，就万事俱备只欠东风了，东风就是 CPU 资源。
  - 做完准备工作之后，线程才能被 JVM 或操作系统进一步去调度到执行状态等待获取 CPU 资源，然后才会真正地进入到运行状态执行 `run` 方法中的代码。

- **需要注意: 不能重复的执行 start 方法**

  - 代码示例

    ```java
    /**
    * <p>
    * 演示不能重复的执行 start 方法(两次及以上), 否则会报错
    * </p>
    *
    * @author 踏雪彡寻梅
    * @version 1.0
    * @date 2020/9/20 - 16:47
    * @since JDK1.8
    */
    public class CantStartTwice {
        public static void main(String[] args) {
            Runnable runnable = () -> {
                System.out.println("name: " + Thread.currentThread().getName());
            };
            Thread thread = new Thread(runnable);
            // 输出: name: Thread-0
            thread.start();
            // 输出: 抛出 java.lang.IllegalThreadStateException
            // 即非法线程状态异常(线程状态不符合规定)
            thread.start();
        }
    }
    ```

  - 报错的原因

    - `start` 一旦开始执行，线程状态就从最开始的 New 状态进入到后续的状态，比如说 Runnable，然后一旦线程执行完毕，线程就会变成终止状态，而终止状态永远不可能再返回回去，所以会抛出以上异常，也就是说不能回到初始状态了。这里描述的还不够清晰，让我们来看看源码能了解的更透彻。

**start 方法源码分析**

#### 6.5.1.源码

```java
public synchronized void start() {
    /**
     * This method is not invoked for the main method thread or "system"
     * group threads created/set up by the VM. Any new functionality added
     * to this method in the future may have to also be added to the VM.
     *
     * A zero status value corresponds to state "NEW".
     */
    // 第一步, 检查线程状态是否为初始状态, 这里也就是上面抛出异常的原因
    if (threadStatus != 0)
        throw new IllegalThreadStateException();

    /* Notify the group that this thread is about to be started
     * so that it can be added to the group's list of threads
     * and the group's unstarted count can be decremented. */
    // 第二步, 加入线程组
    group.add(this);

    boolean started = false;
    try {
        // 第三步, 调用 start0 方法
        start0();
        started = true;
    } finally {
        try {
            if (!started) {
                group.threadStartFailed(this);
            }
        } catch (Throwable ignore) {
            /* do nothing. If start0 threw a Throwable then
              it will be passed up the call stack */
        }
    }
}
```

**源码中的流程**

**第一步：**
启动新线程时会首先检查线程状态是否为初始状态, 这也是以上抛出异常的原因。即以下代码:

```java
if (threadStatus != 0)
	throw new IllegalThreadStateException();
```

其中 `threadStatus` 这个变量的注释如下，也就是说 Java 的线程状态最初始(还没有启动)的时候表示为 0:

```java
/* Java thread status for tools,
 * initialized to indicate thread 'not yet started'
 */
private volatile int threadStatus = 0;
```

**第二步:**
将其加入线程组。即以下代码:

```java
group.add(this);
```

**第三步:**
最后调用 `start0()` 这个 native 方法(native 代表它的代码不是由 Java 实现的，而是由 C/C++ 实现的，具体实现可以在 JDK 里面看到，了解即可), 即以下代码:

```java
boolean started = false;
try {
    // 第三步, 调用 start0 方法
    start0();
    started = true;
} finally {
    try {
        if (!started) {
            group.threadStartFailed(this);
        }
    } catch (Throwable ignore) {
        /* do nothing. If start0 threw a Throwable then
          it will be passed up the call stack */
    }
}
```

**6.5.3 run 方法分析**

**run 方法源码分析**

```java
@Override
public void run() {
    // 传入了 target 对象(即 Runnable 接口的实现), 执行传入的 target 对象的 run 方法
    if (target != null) {
        target.run();
    }
}
```

**对于 run 方法的两种情况**

- 第一种: 重写了 `Thread` 类的 `run` 方法，`Thread` 的 `run` 方法会失效, 将会执行重写的 `run` 方法。
- 第二种: 传入了 `target` 对象(即 `Runnable` 接口的实现)，执行 `Thread` 的原有 `run` 方法然后接着执行 `target`对象的 `run` 方法。
- 总结:
  - `run` 方法就是一个普通的方法, 上文中直接去执行 `run` 方法也就是相当于我们执行自己写的普通方法一样，所以它的执行线程就是我们的主线程。
  - 所以要想真正的启动线程，不能直接调用 `run` 方法，而是要调用 `start` 方法，其中可以间接的调用 `run` 方法。

### 6.6. 可以直接调用 `Thread` 类的 `run` 方法么

- 可以。但是如果直接调用 `Thread` 的 `run` 方法，它的行为就会和普通的方法一样。
- 为了在新的线程中执行我们的代码，必须使用 `Thread` 的 `start` 方法。



### 6.7. 如何停止一个正在运行的线程？

停止一个线程意味着在任务处理完任务之前停掉正在做的操作，也就是放弃当前的操作。停止一个线程可以用Thread.stop()方法，但最好不要用它。虽然它确实可以停止一个正在运行的线程，但是这个方法是不安全的，而且是已被废弃的方法。

在java中有以下3种方法可以终止正在运行的线程：

- 使用退出标志，使线程正常退出，也就是当run方法完成后线程终止。
- 使用stop方法强行终止，但是不推荐这个方法，因为stop和suspend及resume一样都是过期作废的方法。
- 使用interrupt方法中断线程。

**6.7.1. 停止不了的线程**

`interrupt()` 方法的使用效果并不像 `for+break` 语句那样，马上就停止循环。调用 `interrupt` 方法是在当前线程中打了一个停止标志，并不是真的停止线程。

```
public class MyThread extends Thread {
    public void run(){
        super.run();
        for(int i=0; i<500000; i++){
            System.out.println("i="+(i+1));
        }
    }
}

public class Run {
    public static void main(String args[]){
        Thread thread = new MyThread();
        thread.start();
        try {
            Thread.sleep(2000);
            thread.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

输出结果：

```
...
i=499994
i=499995
i=499996
i=499997
i=499998
i=499999
i=500000
```

**6.7.2. 判断线程是否停止状态**

Thread.java类中提供了两种方法：

- `this.interrupted()`: 测试当前线程是否已经中断；
- `this.isInterrupted()`: 测试线程是否已经中断；

**那么这两个方法有什么图区别呢？**

我们先来看看 `this.interrupted()` 方法的解释：测试当前线程是否已经中断，当前线程是指运行`this.interrupted()` 方法的线程。

```
public class MyThread extends Thread {
    public void run(){
        super.run();
        for(int i=0; i<500000; i++){
            i++;
//            System.out.println("i="+(i+1));
        }
    }
}

public class Run {
    public static void main(String args[]){
        Thread thread = new MyThread();
        thread.start();
        try {
            Thread.sleep(2000);
            thread.interrupt();

            System.out.println("stop 1??" + thread.interrupted());
            System.out.println("stop 2??" + thread.interrupted());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

运行结果：

```
stop 1??false
stop 2??false
```

类Run.java中虽然是在thread对象上调用以下代码：thread.interrupt(), 后面又使用

```
System.out.println("stop 1??" + thread.interrupted());
System.out.println("stop 2??" + thread.interrupted());  
```

来判断thread对象所代表的线程是否停止，但从控制台打印的结果来看，线程并未停止，这也证明了interrupted()方法的解释，测试当前线程是否已经中断。这个当前线程是main，它从未中断过，所以打印的结果是两个false.

如何使main线程产生中断效果呢？

```
public class Run2 {
    public static void main(String args[]){
        Thread.currentThread().interrupt();
        System.out.println("stop 1??" + Thread.interrupted());
        System.out.println("stop 2??" + Thread.interrupted());

        System.out.println("End");
    }
}    
```

运行效果为：

```
stop 1??true
stop 2??false
End
```

方法interrupted()的确判断出当前线程是否是停止状态。但为什么第2个布尔值是false呢？官方帮助文档中对interrupted方法的解释：

测试当前线程是否已经中断。线程的中断状态由该方法清除。换句话说，如果连续两次调用该方法，则第二次调用返回false。

下面来看一下inInterrupted()方法。

```
public class Run3 {
    public static void main(String args[]){
        Thread thread = new MyThread();
        thread.start();
        thread.interrupt();
        System.out.println("stop 1??" + thread.isInterrupted());
        System.out.println("stop 2??" + thread.isInterrupted());
    }
}
```

运行结果：

```
stop 1??true
stop 2??true
```

isInterrupted()并为清除状态，所以打印了两个true。

**6.7.3. 能停止的线程--异常法**

有了前面学习过的知识点，就可以在线程中用for语句来判断一下线程是否是停止状态，如果是停止状态，则后面的代码不再运行即可：

```
public class MyThread extends Thread {
    public void run(){
        super.run();
        for(int i=0; i<500000; i++){
            if(this.interrupted()) {
                System.out.println("线程已经终止， for循环不再执行");
                break;
            }
            System.out.println("i="+(i+1));
        }
    }
}

public class Run {
    public static void main(String args[]){
        Thread thread = new MyThread();
        thread.start();
        try {
            Thread.sleep(2000);
            thread.interrupt();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

运行结果：

```
...
i=202053
i=202054
i=202055
i=202056
```

线程已经终止， for循环不再执行

上面的示例虽然停止了线程，但如果for语句下面还有语句，还是会继续运行的。看下面的例子：

```
public class MyThread extends Thread {
    public void run(){
        super.run();
        for(int i=0; i<500000; i++){
            if(this.interrupted()) {
                System.out.println("线程已经终止， for循环不再执行");
                break;
            }
            System.out.println("i="+(i+1));
        }

        System.out.println("这是for循环外面的语句，也会被执行");
    }
}
```

使用Run.java执行的结果是：

```
...
i=180136
i=180137
i=180138
i=180139
```

线程已经终止， for循环不再执行

这是for循环外面的语句，也会被执行

如何解决语句继续运行的问题呢？看一下更新后的代码：

```
public class MyThread extends Thread {
    public void run(){
        super.run();
        try {
            for(int i=0; i<500000; i++){
                if(this.interrupted()) {
                    System.out.println("线程已经终止， for循环不再执行");
                        throw new InterruptedException();
                }
                System.out.println("i="+(i+1));
            }

            System.out.println("这是for循环外面的语句，也会被执行");
        } catch (InterruptedException e) {
            System.out.println("进入MyThread.java类中的catch了。。。");
            e.printStackTrace();
        }
    }
}
```

使用Run.java运行的结果如下：

```
...
i=203798
i=203799
i=203800
线程已经终止， for循环不再执行
进入MyThread.java类中的catch了。。。
java.lang.InterruptedException
    at thread.MyThread.run(MyThread.java:13)
```

**6.7.4. 在沉睡中停止**

如果线程在 `sleep()` 状态下停止线程，会是什么效果呢？

```
public class MyThread extends Thread {
    public void run(){
        super.run();

        try {
            System.out.println("线程开始。。。");
            Thread.sleep(200000);
            System.out.println("线程结束。");
        } catch (InterruptedException e) {
            System.out.println("在沉睡中被停止, 进入catch， 调用isInterrupted()方法的结果是：" + this.isInterrupted());
            e.printStackTrace();
        }

    }
}
```

使用Run.java运行的结果是：

```
线程开始。。。
在沉睡中被停止, 进入catch， 调用isInterrupted()方法的结果是：false
java.lang.InterruptedException: sleep interrupted
    at java.lang.Thread.sleep(Native Method)
    at thread.MyThread.run(MyThread.java:12)
```

从打印的结果来看， 如果在sleep状态下停止某一线程，会进入catch语句，并且清除停止状态值，使之变为false。

前一个实验是先sleep然后再用interrupt()停止，与之相反的操作在学习过程中也要注意：

```
public class MyThread extends Thread {
    public void run(){
        super.run();
        try {
            System.out.println("线程开始。。。");
            for(int i=0; i<10000; i++){
                System.out.println("i=" + i);
            }
            Thread.sleep(200000);
            System.out.println("线程结束。");
        } catch (InterruptedException e) {
             System.out.println("先停止，再遇到sleep，进入catch异常");
            e.printStackTrace();
        }

    }
}

public class Run {
    public static void main(String args[]){
        Thread thread = new MyThread();
        thread.start();
        thread.interrupt();
    }
}
```

运行结果：

```
i=9998
i=9999
先停止，再遇到sleep，进入catch异常
java.lang.InterruptedException: sleep interrupted
    at java.lang.Thread.sleep(Native Method)
    at thread.MyThread.run(MyThread.java:15)
```

**6.7.5. 能停止的线程---暴力停止**

使用stop()方法停止线程则是非常暴力的。

```
public class MyThread extends Thread {
    private int i = 0;
    public void run(){
        super.run();
        try {
            while (true){
                System.out.println("i=" + i);
                i++;
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class Run {
    public static void main(String args[]) throws InterruptedException {
        Thread thread = new MyThread();
        thread.start();
        Thread.sleep(2000);
        thread.stop();
    }
}
```

运行结果：

```
i=0
i=1
i=2
i=3
i=4
i=5
i=6
i=7
i=8
i=9

Process finished with exit code 0
```

**6.7.6.方法 `stop()` 与 `java.lang.ThreadDeath` 异常**

调用 `stop()` 方法时会抛出 `java.lang.ThreadDeath` 异常，但是通常情况下，此异常不需要显示地捕捉。

```
public class MyThread extends Thread {
    private int i = 0;
    public void run(){
        super.run();
        try {
            this.stop();
        } catch (ThreadDeath e) {
            System.out.println("进入异常catch");
            e.printStackTrace();
        }
    }
}

public class Run {
    public static void main(String args[]) throws InterruptedException {
        Thread thread = new MyThread();
        thread.start();
    }
}
```

`stop()` 方法以及作废，因为如果强制让线程停止有可能使一些清理性的工作得不到完成。另外一个情况就是对锁定的对象进行了解锁，导致数据得不到同步的处理，出现数据不一致的问题。

**6.7.7. 释放锁的不良后果**

使用 `stop()` 释放锁将会给数据造成不一致性的结果。如果出现这样的情况，程序处理的数据就有可能遭到破坏，最终导致程序执行的流程错误，一定要特别注意：

```
public class SynchronizedObject {
    private String name = "a";
    private String password = "aa";

    public synchronized void printString(String name, String password){
        try {
            this.name = name;
            Thread.sleep(100000);
            this.password = password;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

public class MyThread extends Thread {
    private SynchronizedObject synchronizedObject;
    public MyThread(SynchronizedObject synchronizedObject){
        this.synchronizedObject = synchronizedObject;
    }

    public void run(){
        synchronizedObject.printString("b", "bb");
    }
}

public class Run {
    public static void main(String args[]) throws InterruptedException {
        SynchronizedObject synchronizedObject = new SynchronizedObject();
        Thread thread = new MyThread(synchronizedObject);
        thread.start();
        Thread.sleep(500);
        thread.stop();
        System.out.println(synchronizedObject.getName() + "  " + synchronizedObject.getPassword());
    }
}
```

输出结果：

```
b  aa
```

由于 `stop()` 方法以及在JDK中被标明为“过期/作废”的方法，显然它在功能上具有缺陷，所以不建议在程序张使用`stop()` 方法。

**6.7.8. 使用return停止线程**

将方法 `interrupt()` 与 return 结合使用也能实现停止线程的效果：

```
public class MyThread extends Thread {
    public void run(){
        while (true){
            if(this.isInterrupted()){
                System.out.println("线程被停止了！");
                return;
            }
            System.out.println("Time: " + System.currentTimeMillis());
        }
    }
}

public class Run {
    public static void main(String args[]) throws InterruptedException {
        Thread thread = new MyThread();
        thread.start();
        Thread.sleep(2000);
        thread.interrupt();
    }
}
```

输出结果：

```
...
Time: 1467072288503
Time: 1467072288503
Time: 1467072288503
线程被停止了！
```

**不过还是建议使用“抛异常”的方法来实现线程的停止，因为在catch块中还可以将异常向上抛，使线程停止事件得以传播。**



### 6.8 线程协作问题

最近在网上看到一个面试题目，感觉挺有意思的，大意如下：

![519126-20200926203742890-403133210](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323220915.png)

ok，大家看到这个题，可以先理解下，这里启动了两个线程，a和b，但是虽然说a在b之前start，不一定就可以保证线程a的逻辑，可以先于线程b执行，所以，这里的意思是，线程a和b，执行顺序互不干扰，我们不应该假定其中一个线程可以先于另外一个执行。

另外，既然是面试题，那常规做法自然是不用上了，比如让b先sleep几秒钟之类的，如果真这么答，那可能面试就结束了吧。

ok，我们下面开始分析解法。

#### 6.8.1.可见性保证

程序里定义了一个全局变量，var = 1；线程a会修改这个变量为2，线程b则在变量为2时，执行自己的业务逻辑。

那么，这里首先，我们要做的是，先讲var使用volatile修饰，保证多线程操作时的可见性。

```
public static volatile int var = 1;
```

#### 6.8.2.解法分析

经过前面的可见性保证的分析，我们知道，要想达到目的，其实就是要保证：**a中的对var+1的操作，需要先于b执行。**

但是，现在的问题是，两个线程同时启动，不知道谁先谁后，怎么保证a先执行，b后执行呢？

让线程b先不执行，大概有两种思路，一种是阻塞该线程，一种是不阻塞该线程，阻塞的话，我们可以想想，怎么阻塞一个线程。

大概有：

- `synchronized`，取不到锁时，阻塞
- `java.util.concurrent.locks.ReentrantLock#lock`，取不到锁时，阻塞
- `object.wait`，取到synchronized了，但是因为一些条件不满足，执行不下去，调用wait，将释放锁，并进入等待队列，线程暂停运行
- `java.util.concurrent.locks.Condition.await`，和 `object.wait` 类似，只不过 `object.wait` 在jvm层面，使用c++实现，`Condition.await` 在jdk层面使用java语言实现
- `threadA.join()`，等待对应的线程threadA执行完成后，本线程再继续运行；threadA没结束，则当前线程阻塞；
- `CountDownLatch#await` ，在对应的state不为0时，阻塞
- `Semaphore#acquire()`，在state为0时（即剩余令牌为0时），阻塞
- 其他阻塞队列、FutureTask等等

如果不让线程进入阻塞，则一般可以让线程进入一个while循环，循环的退出条件，可以由线程a来修改，线程a修改后，线程b跳出循环。

比如：

```
volatile boolean stop = false;
while (!stop){
    ...
}
```

上面也说了这么多了，我们实际上手写一写吧。

**错误解法1--基于wait**

下面的思路是基于wait、notify；线程b直接wait，线程a在修改了变量后，进行notify。

```java
public class Global1 {
    public static volatile int var = 1;
    public static final Object monitor = new Object();

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            // 1
            Global1.var++;
            // 2
            synchronized (monitor) {
                monitor.notify();
            }
        });
        Thread b = new Thread(() -> {
            // 3
            synchronized (monitor) {
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 4
            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        a.start();
        b.start();
    }
}
```

大家觉得这个代码能行吗？实际是不行的。因为实际的顺序可能是：

```
线程a--1
线程a--2
线程b--1
线程b--2
```

在线程a-2时，线程a去notify，但是此时线程b还没开始wait，所以此时的notify是没有任何效果的：没人在等，notify个锤子。

怎么修改，本方案才行得通呢？

那就是，修改线程a的代码，不要急着notify，先等等。

```
        Thread a = new Thread(() -> {
            Global1.var++;
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (monitor) {
                monitor.notify();
            }
        });
```

但是这样的话，明显不合适，有作弊嫌疑，也不优雅。

**错误解法2--基于condition的signal**

```
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Global1 {
    public static volatile int var = 1;
    public static final ReentrantLock reentrantLock = new ReentrantLock();
    public static final Condition condition = reentrantLock.newCondition();

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            final ReentrantLock lock = reentrantLock;
            lock.lock();
            try {
                condition.signal();
            } finally {
                lock.unlock();
            }
        });
        Thread b = new Thread(() -> {
            final ReentrantLock lock = reentrantLock;
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        a.start();
        b.start();
    }
}
```

这个方案使用了Condition对象来实现object的notify、wait效果。当然，这个也有同样的问题。

**正确解法1--基于错误解法2进行改进**

我们看看，前面问题的根源在于，我们线程a，在去通知线程b的时候，有可能线程b还没开始wait，所以此时通知失效。

那么，我们是不是可以先等等，等线程b开始wait了，再去通知呢？

```
        Thread a = new Thread(() -> {
            Global1.var++;
            final ReentrantLock lock = reentrantLock;
            lock.lock();
            try {
                // 1
                while (!reentrantLock.hasWaiters(condition)) {
                    Thread.yield();
                }
                condition.signal();
            } finally {
                lock.unlock();
            }
        });
```

1处代码，就是这个思想，在signal之前，判断当前condition上是否有waiter线程，如果没有，就死循环；如果有，才去执行signal。

这个方法实测是可行的。

**正确解法2**

对正确解法1，换一个api，就变成了正确解法2.

```
Thread a = new Thread(() -> {
    Global1.var++;
    final ReentrantLock lock = reentrantLock;
    lock.lock();
    try {
        // 1
        while (reentrantLock.getWaitQueueLength(condition) == 0) {
            Thread.yield();
        }
        condition.signal();
    } finally {
        lock.unlock();
    }
});
```

1这里，获取condition上等待队列的长度，如果为0，说明没有等待者，则死循环。

**正确解法3--基于Semaphore**

刚开始，我们初始化一个信号量，state为0. 线程b去获取信号量的时候，就会阻塞。

然后我们线程a再去释放一个信号量，此时线程b就可以继续执行。

```
public class Global1 {
    public static volatile int var = 1;
    public static final Semaphore semaphore = new Semaphore(0);

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            semaphore.release();
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**正确解法4--基于CountDownLatch**

```
public class Global1 {
    public static volatile int var = 1;
    public static final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            countDownLatch.countDown();
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**正确解法5--基于BlockingQueue**

这里使用了ArrayBlockingQueue，其他的阻塞队列也是可以的。

```
import countdown.CountdownTest;


public class Global1 {
    public static volatile int var = 1;
    public static final ArrayBlockingQueue arrayBlockingQueue = new ArrayBlockingQueue<Object>(1);

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            arrayBlockingQueue.offer(new Object());
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            try {
                arrayBlockingQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**正确解法6--基于FutureTask**

我们也可以让线程b等待一个task的执行结果；而线程a在执行完修改var为2后，执行该任务，任务执行完成后，线程b就会被通知继续执行。

```
public class Global1 {
    public static volatile int var = 1;
    public static final FutureTask futureTask = new FutureTask<Object>(new Callable<Object>() {
        @Override
        public Object call() throws Exception {
            System.out.println("callable task ");
            return null;
        }
    });

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            futureTask.run();
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            try {
                futureTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**正确解法7--基于join**

这个可能是最简洁直观的，哈哈。也是群里同学们提供的解法，真的有才！

```
public class Global1 {
    public static volatile int var = 1;

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            try {
                a.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**正确解法8--基于CompletableFuture**

这个和第6种类似。都是基于future。

```
public class Global1 {
    public static volatile int var = 1;
    public static final CompletableFuture<Object> completableFuture =
            new CompletableFuture<Object>();

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            completableFuture.complete(new Object());
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            try {
                completableFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**非阻塞--正确解法9--忙等待**

这种代码量也少，只要线程b在变量为1时，死循环就行了。

```
public class Global1 {
    public static volatile int var = 1;

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            while (var == 1) {
                Thread.yield();
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```

**非阻塞--正确解法10--忙等待**

忙等待的方案很多，反正就是某个条件不满足时，不阻塞自己，阻塞了会释放cpu，我们就是不希望释放cpu的。

比如像下面这样也可以。

```
public class Global1 {
    public static volatile int var = 1;
    public static final AtomicInteger atomicInteger =
            new AtomicInteger(1);

    public static void main(String[] args) {
        Thread a = new Thread(() -> {
            Global1.var++;
            atomicInteger.set(2);
        });
        a.setName("thread a");
        Thread b = new Thread(() -> {
            while (true) {
                boolean success = atomicInteger.compareAndSet(2, 1);
                if (success) {
                    break;
                } else {
                    Thread.yield();
                }
            }

            if (Global1.var == 2) {
                //do something;
                System.out.println(Thread.currentThread().getName() + " good job");
            }
        });
        b.setName("thread b");
        a.start();
        b.start();
    }
}
```









