[toc]



# Java 线程池

## 1. 简介

### 1.1. 什么是线程池

线程池是一种多线程处理形式，处理过程中将任务添加到队列，然后在创建线程后自动启动这些任务。

### 1.2. 为什么要用线程池

如果并发请求数量很多，但每个线程执行的时间很短，就会出现频繁的创建和销毁线程。如此一来，会大大降低系统的效率，可能频繁创建和销毁线程的时间、资源开销要大于实际工作的所需。

正是由于这个问题，所以有必要引入线程池。使用 **线程池的好处** 有以下几点：

- **降低资源消耗** - 通过重复利用已创建的线程降低线程创建和销毁造成的消耗。
- **提高响应速度** - 当任务到达时，任务可以不需要等到线程创建就能立即执行。
- **提高线程的可管理性** - 线程是稀缺资源，如果无限制的创建，不仅会消耗系统资源，还会降低系统的稳定性，使用线程池可以进行统一的分配，调优和监控。但是要做到合理的利用线程池，必须对其原理了如指掌。

### 1.3. 使用线程池的风险

虽然线程池是构建多线程应用程序的强大机制，但使用它并不是没有风险的。用线程池构建的应用程序容易遭受任何其它多线程应用程序容易遭受的所有并发风险，诸如同步错误和死锁，它还容易遭受特定于线程池的少数其它风险，诸如与池有关的**死锁、资源不足和线程泄漏。**

**1.3.1 死锁**

任何多线程应用程序都有死锁风险。当一组进程或线程中的每一个都在等待一个只有该组中另一个进程才能引起的事件时，我们就说这组进程或线程 死锁了。

> 死锁的最简单情形是：线程 A 持有对象 X 的独占锁，并且在等待对象 Y 的锁，而线程 B 持有对象 Y 的独占锁，却在等待对象 X 的锁。除非有某种方法来打破对锁的等待（Java 锁定不支持这种方法），否则死锁的线程将永远等下去。

虽然任何多线程程序中都有死锁的风险，但线程池却引入了另一种死锁可能，在那种情况下，所有池线程都在执行已阻塞的等待队列中另一任务的执行结果的任务，但这一任务却因为没有未被占用的线程而不能运行。当线程池被用来实现涉及许多交互对象的模拟，被模拟的对象可以相互发送查询，这些查询接下来作为排队的任务执行，查询对象又同步等待着响应时，会发生这种情况。

**1.3.2 资源不足**

线程池的一个优点在于：相对于其它替代调度机制（有些我们已经讨论过）而言，它们通常执行得很好。**但只有恰当地调整了线程池大小时才是这样的。**线程消耗包括内存和其它系统资源在内的大量资源。除了 `Thread` 对象所需的内存之外，每个线程都需要两个可能很大的执行调用堆栈。除此以外，`JVM` 可能会为每个 `Java` 线程创建一个本机线程，这些本机线程将消耗额外的系统资源。最后，虽然线程之间切换的调度开销很小，但如果有很多线程，环境切换也可能严重地影响程序的性能。

如果线程池太大，那么被那些线程消耗的资源可能严重地影响系统性能。在线程之间进行切换将会浪费时间，而且使用超出比您实际需要的线程可能会引起资源匮乏问题，因为池线程正在消耗一些资源，而这些资源可能会被其它任务更有效地利用。除了线程自身所使用的资源以外，服务请求时所做的工作可能需要其它资源，例如 `JDBC` 连接、套接字或文件。这些也都是有限资源，有太多的并发请求也可能引起失效，例如不能分配 `JDBC` 连接。

**1.3.3 线程泄漏**

各种类型的线程池中一个严重的风险是线程泄漏，当从池中除去一个线程以执行一项任务，而在任务完成后该线程却没有返回池时，会发生这种情况。发生线程泄漏的一种情形出现在任务抛出一个 `RuntimeException` 或一个 `Error` 时。如果池类没有捕捉到它们，那么线程只会退出而线程池的大小将会永久减少一个。当这种情况发生的次数足够多时，线程池最终就为空，而且系统将停止，因为没有可用的线程来处理任务。

有些任务可能会永远等待某些资源或来自用户的输入，而这些资源又不能保证变得可用，用户可能也已经回家了，诸如此类的任务会永久停止，而这些停止的任务也会引起和线程泄漏同样的问题。如果某个线程被这样一个任务永久地消耗着，那么它实际上就被从池除去了。对于这样的任务，应该要么只给予它们自己的线程，要么只让它们等待有限的时间。

## 2. Executor 框架

> `Executor` 框架是一个根据一组执行策略调用，调度，执行和控制的异步任务的框架，目的是提供一种将”任务提交”与”任务如何运行”分离开来的机制。

### 2.1. 核心 API 概述

`Executor` 框架核心 `API` 如下：

- `Executor` - 运行任务的简单接口。

- `ExecutorService` - 扩展了`Executor`接口。扩展能力：
- 支持有返回值的线程；
- 支持管理线程的生命周期。
- `ScheduledExecutorService` - 扩展了 `ExecutorService` 接口。扩展能力：支持定期执行任务。

- `AbstractExecutorService` - `ExecutorService` 接口的默认实现。

- `ThreadPoolExecutor` - Executor 框架最核心的类，它继承了 `AbstractExecutorService` 类。

- `ScheduledThreadPoolExecutor` - `ScheduledExecutorService` 接口的实现，一个可定时调度任务的线程池。

- `Executors` - 可以通过调用 `Executors` 的静态工厂方法来创建线程池并返回一个 `ExecutorService` 对象。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e63757272656e742f6578657863746f722d756d6c2e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322222237.png)

### 2.2. Executor

`Executor` 接口中只定义了一个 `execute` 方法，用于接收一个 `Runnable` 对象。

```
public interface Executor {
    void execute(Runnable command);
}
```

### 2.3. ExecutorService

`ExecutorService` 接口继承了 `Executor` 接口，它还提供了 `invokeAll`、`invokeAny`、`shutdown`、`submit` 等方法。

```
public interface ExecutorService extends Executor {

    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException;

    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException;

    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                  long timeout, TimeUnit unit)
        throws InterruptedException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException;

    <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                    long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```

从其支持的方法定义，不难看出：相比于 `Executor` 接口，`ExecutorService` 接口主要的扩展是：

- 支持有返回值的线程 - `sumbit`、`invokeAll`、`invokeAny` 方法中都支持传入`Callable` 对象。
- 支持管理线程生命周期 - `shutdown`、`shutdownNow`、`isShutdown` 等方法。

### 2.4. ScheduledExecutorService

`ScheduledExecutorService` 接口扩展了 `ExecutorService` 接口。

它除了支持前面两个接口的所有能力以外，还支持定时调度线程。

```
public interface ScheduledExecutorService extends ExecutorService {

    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay, TimeUnit unit);

    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay, TimeUnit unit);

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);

}
```

其扩展的接口提供以下能力：

- `schedule` 方法可以在指定的延时后执行一个 `Runnable` 或者 `Callable` 任务。
- `scheduleAtFixedRate` 方法和 `scheduleWithFixedDelay` 方法可以按照指定时间间隔，定期执行任务。

## 3. ThreadPoolExecutor

`java.uitl.concurrent.ThreadPoolExecutor` 类是 `Executor` 框架中最核心的类。所以，本文将着重讲述一下这个类。

### 3.1. 重要字段

`ThreadPoolExecutor` 有以下重要字段：

```
private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));
private static final int COUNT_BITS = Integer.SIZE - 3;
private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
// runState is stored in the high-order bits
private static final int RUNNING    = -1 << COUNT_BITS;
private static final int SHUTDOWN   =  0 << COUNT_BITS;
private static final int STOP       =  1 << COUNT_BITS;
private static final int TIDYING    =  2 << COUNT_BITS;
private static final int TERMINATED =  3 << COUNT_BITS;
```

参数说明：

- `ctl` - 用于控制线程池的运行状态和线程池中的有效线程数量。它包含两部分的信息：
  - 线程池的运行状态 (`runState`)
    
    - 线程池内有效线程的数量 (`workerCount`)
    
    - 可以看到，`ctl` 使用了 `Integer` 类型来保存，高 3 位保存 `runState`，低 29 位保存 `workerCount`。`COUNT_BITS` 就是 29，`CAPACITY`就是 1 左移 29 位减 1（29 个 1），这个常量表示 `workerCount` 的上限值，大约是 5 亿。
    
  - 运行状态 - 线程池一共有五种运行状态：

    - `RUNNING` - **运行状态**。接受新任务，并且也能处理阻塞队列中的任务。

    - `SHUTDOWN` - 关闭状态。不接受新任务，但可以处理阻塞队列中的任务。

      - 在线程池处于 `RUNNING` 状态时，调用 `shutdown` 方法会使线程池进入到该状态。

      - `finalize` 方法在执行过程中也会调用 `shutdown` 方法进入该状态。

    - `STOP` - **停止状态**。不接受新任务，也不处理队列中的任务。会中断正在处理任务的线程。在线程池处于 `RUNNING` 或 `SHUTDOWN` 状态时，调用 `shutdownNow` 方法会使线程池进入到该状态。

    - `TIDYING` - **整理状态**。如果所有的任务都已终止了，`workerCount` (有效线程数) 为 0，线程池进入该状态后会调用 `terminated` 方法进入 `TERMINATED` 状态。

    - `TERMINATED` - 已终止状态。在`terminated`方法执行完后进入该状态。默认`terminated`方法中什么也没有做。进入`TERMINATED`的条件如下：
      - 线程池不是 `RUNNING` 状态；
      - 线程池状态不是 `TIDYING` 状态或 `TERMINATED` 状态；
      - 如果线程池状态是 `SHUTDOWN` 并且 `workerQueue` 为空；
      - `workerCount` 为 0；
      - 设置 `TIDYING` 状态成功。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e63757272656e742f6a6176612d7468726561642d706f6f6c5f322e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322222532.png)

### 3.2. 构造方法

`ThreadPoolExecutor` 有四个构造方法，前三个都是基于第四个实现。第四个构造方法定义如下：

```
public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
```

参数说明：

- `corePoolSize` - 核心线程数量。当有新任务通过`execute`方法提交时 ，线程池会执行以下判断：

  - 如果运行的线程数少于 `corePoolSize`，则创建新线程来处理任务，即使线程池中的其他线程是空闲的。
  - 如果线程池中的线程数量大于等于 `corePoolSize` 且小于 `maximumPoolSize`，则只有当 `workQueue` 满时才创建新的线程去处理任务；
  - 如果设置的 `corePoolSize` 和 `maximumPoolSize` 相同，则创建的线程池的大小是固定的。这时如果有新任务提交，若 `workQueue` 未满，则将请求放入 `workQueue` 中，等待有空闲的线程去从 `workQueue` 中取任务并处理；
  - 如果运行的线程数量大于等于 `maximumPoolSize`，这时如果 `workQueue` 已经满了，则使用 `handler` 所指定的策略来处理任务；
  - 所以，任务提交时，判断的顺序为 `corePoolSize` => `workQueue` => `maximumPoolSize`。

- `maximumPoolSize` - 最大线程数量。

  - 如果队列满了，并且已创建的线程数小于最大线程数，则线程池会再创建新的线程执行任务。
  - 值得注意的是：如果使用了无界的任务队列这个参数就没什么效果。

- `keepAliveTime`：线程保持活动的时间。

  - 当线程池中的线程数量大于 `corePoolSize` 的时候，如果这时没有新的任务提交，核心线程外的线程不会立即销毁，而是会等待，直到等待的时间超过了 `keepAliveTime`。
  - 所以，如果任务很多，并且每个任务执行的时间比较短，可以调大这个时间，提高线程的利用率。

- `unit` - **`keepAliveTime` 的时间单位**。有 7 种取值。可选的单位有天（`DAYS`），小时（`HOURS`），分钟（`MINUTES`），毫秒(`MILLISECONDS`)，微秒(`MICROSECONDS`, 千分之一毫秒)和毫微秒(`NANOSECONDS`, 千分之一微秒)。

- `workQueue` - 等待执行的任务队列。用于保存等待执行的任务的阻塞队列。 可以选择以下几个阻塞队列。

  - `ArrayBlockingQueue` - 有界阻塞队列。

    - 此队列是**基于数组的先进先出队列（FIFO）**。
    - 此队列创建时必须指定大小。

  - `LinkedBlockingQueue` - 无界阻塞队列。

    - 此队列是**基于链表的先进先出队列（FIFO）**。
    - 如果创建时没有指定此队列大小，则默认为 `Integer.MAX_VALUE`。
    - 吞吐量通常要高于 `ArrayBlockingQueue`。
    - 使用 `LinkedBlockingQueue` 意味着： `maximumPoolSize` 将不起作用，线程池能创建的最大线程数为 `corePoolSize`，因为任务等待队列是无界队列。
    - `Executors.newFixedThreadPool` 使用了这个队列。

  - `SynchronousQueue` - 不会保存提交的任务，而是将直接新建一个线程来执行新来的任务。

    - 每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态。
    - 吞吐量通常要高于 `LinkedBlockingQueue`。
    - `Executors.newCachedThreadPool` 使用了这个队列。

  - `PriorityBlockingQueue` - **具有优先级的无界阻塞队列**。

- `threadFactory` - **线程工厂**。可以通过线程工厂给每个创建出来的线程设置更有意义的名字。

- `handler` - 饱和策略。它是`RejectedExecutionHandler`类型的变量。当队列和线程池都满了，说明线程池处于饱和状态，那么必须采取一种策略处理提交的新任务。线程池支持以下策略：

  - `AbortPolicy` - 丢弃任务并抛出异常。这也是默认策略。
  - `DiscardPolicy` - 丢弃任务，但不抛出异常。
  - `DiscardOldestPolicy` - 丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）。
  - `CallerRunsPolicy` - 直接调用 `run` 方法并且阻塞执行。
  - 如果以上策略都不能满足需要，也可以通过实现 `RejectedExecutionHandler` 接口来定制处理策略。如记录日志或持久化不能处理的任务。

### 3.3. execute 方法

默认情况下，创建线程池之后，线程池中是没有线程的，需要提交任务之后才会创建线程。

提交任务可以使用 `execute` 方法，它是 `ThreadPoolExecutor` 的核心方法，通过这个方法可以**向线程池提交一个任务，交由线程池去执行**。

`execute` 方法工作流程如下：

1. 如果 `workerCount < corePoolSize`，则创建并启动一个线程来执行新提交的任务；
2. 如果 `workerCount >= corePoolSize`，且线程池内的阻塞队列未满，则将任务添加到该阻塞队列中；
3. 如果 `workerCount >= corePoolSize && workerCount < maximumPoolSize`，且线程池内的阻塞队列已满，则创建并启动一个线程来执行新提交的任务；
4. 如果`workerCount >= maximumPoolSize`，并且线程池内的阻塞队列已满，则根据拒绝策略来处理该任务, 默认的处理方式是直接抛异常。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e63757272656e742f6a6176612d7468726561642d706f6f6c5f312e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210322222842.png)

### 3.4. 其他重要方法

在 `ThreadPoolExecutor` 类中还有一些重要的方法：

- `submit` - 类似于 `execute`，但是针对的是有返回值的线程。`submit` 方法是在 `ExecutorService` 中声明的方法，在 `AbstractExecutorService` 就已经有了具体的实现。`ThreadPoolExecutor` 直接复用 `AbstractExecutorService` 的 `submit` 方法。

- `shutdown` - 不会立即终止线程池，而是要等所有任务缓存队列中的任务都执行完后才终止，但再也不会接受新的任务。

  - 将线程池切换到 `SHUTDOWN` 状态；
  - 并调用 `interruptIdleWorkers` 方法请求中断所有空闲的 `worker`；
  - 最后调用 `tryTerminate` 尝试结束线程池。

- `shutdownNow` - 立即终止线程池，并尝试打断正在执行的任务，并且清空任务缓存队列，返回尚未执行的任务。与`shutdown`方法类似，不同的地方在于：

  - 设置状态为 `STOP`；
  - 中断所有工作线程，无论是否是空闲的；
  - 取出阻塞队列中没有被执行的任务并返回。

- `isShutdown` - 调用了 `shutdown` 或 `shutdownNow` 方法后，`isShutdown` 方法就会返回 true。

- `isTerminaed` - 当所有的任务都已关闭后，才表示线程池关闭成功，这时调用 `isTerminaed` 方法会返回 true。

- `setCorePoolSize` - 设置核心线程数大小。

- `setMaximumPoolSize` - 设置最大线程数大小。

- `getTaskCount` - 线程池已经执行的和未执行的任务总数；

- `getCompletedTaskCount` - 线程池已完成的任务数量，该值小于等于 `taskCount`；

- `getLargestPoolSize` - 线程池曾经创建过的最大线程数量。通过这个数据可以知道线程池是否满过，也就是达到了 `maximumPoolSize`；

- `getPoolSize` - 线程池当前的线程数量；

- `getActiveCount` - 当前线程池中正在执行任务的线程数量。

### 3.5. 使用示例

```
public class ThreadPoolExecutorDemo {

    public static void main(String[] args) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(5, 10, 500, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

        for (int i = 0; i < 100; i++) {
            threadPoolExecutor.execute(new MyThread());
            String info = String.format("线程池中线程数目：%s，队列中等待执行的任务数目：%s，已执行玩别的任务数目：%s",
                threadPoolExecutor.getPoolSize(),
                threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getCompletedTaskCount());
            System.out.println(info);
        }
        threadPoolExecutor.shutdown();
    }

    static class MyThread implements Runnable {

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " 执行");
        }

    }

}
```

### 3.6 源码分析

**1、先看一下线程池的executor方法**

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323231250.webp)

- 判断当前活跃线程数是否小于 `corePoolSize`，如果小于，则调用 `addWorker` 创建线程执行任务
- 如果不小于 `corePoolSize`，则将任务添加到 `workQueue` 队列。
- 如果放入 `workQueue` 失败，则创建线程执行任务，如果这时创建线程失败(当前线程数不小于 `maximumPoolSize` 时)，就会调用`reject` (内部调用 `handler` )拒绝接受任务。

**2、再看下addWorker的方法实现**

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323231312.jpg)

这块代码是在创建非核心线程时，即 `core` 等于 `false`。判断当前线程数是否大于等于 `maximumPoolSize`，如果大于等于则返回`false`，即上边说到的③中创建线程失败的情况。

`addWorker` 方法的下半部分：

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323231324.webp)

- 创建 `Worker` 对象，同时也会实例化一个 `Thread` 对象。
- 启动这个线程

**3、再到Worker里看看其实现**

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323231332.png)

可以看到在创建 `Worker` 时会调用 `threadFactory` 来创建一个线程。上边的②中启动一个线程就会触发 `Worker` 的 `run` 方法被线程调用。

**4、接下来咱们看看runWorker方法的逻辑**

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323231342.jpg)

线程调用 `runWoker`，会 `while` 循环调用 `getTask` 方法从 `workerQueue` 里读取任务，然后执行任务。只要 `getTask` 方法不返回 `null`，此线程就不会退出。

**5、最后在看看getTask方法实现**

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323231358.webp)

- 咱们先不管 `allowCoreThreadTimeOut`，这个变量默认值是 `false`。`wc>corePoolSize` 则是判断当前线程数是否大于`corePoolSize`。
- 如果当前线程数大于 `corePoolSize`，则会调用 `workQueue` 的 `poll` 方法获取任务，超时时间是 `keepAliveTime`。如果超过`keepAliveTime` 时长，`poll` 返回了 `null`，上边提到的 `while` 循序就会退出，线程也就执行完了。

- 如果当前线程数小于 `corePoolSize`，则会调用 `workQueue` 的 `take` 方法阻塞在当前。

## 4. Executors

`JDK` 的 `Executors` 类中提供了几种具有代表性的线程池，这些线程池 **都是基于 `ThreadPoolExecutor` 的定制化实现**。

在实际使用线程池的场景中，我们往往不是直接使用 `ThreadPoolExecutor` ，而是使用 `JDK` 中提供的具有代表性的线程池实例。

### 4.1. newSingleThreadExecutor

**创建一个单线程的线程池**。

只会创建唯一的工作线程来执行任务，保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行。 **如果这个唯一的线程因为异常结束，那么会有一个新的线程来替代它** 。

单工作线程最大的特点是：**可保证顺序地执行各个任务**。

示例：

```
public class SingleThreadExecutorDemo {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + " 执行");
                }
            });
        }
        executorService.shutdown();
    }

}
```

### 4.2. newFixedThreadPool

**创建一个固定大小的线程池**。

**每次提交一个任务就会新创建一个工作线程，如果工作线程数量达到线程池最大线程数，则将提交的任务存入到阻塞队列中**。

`FixedThreadPool` 是一个典型且优秀的线程池，它具有线程池提高程序效率和节省创建线程时所耗的开销的优点。但是，在线程池空闲时，即线程池中没有可运行任务时，它不会释放工作线程，还会占用一定的系统资源。

示例：

```
public class FixedThreadPoolDemo {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + " 执行");
                }
            });
        }
        executorService.shutdown();
    }

}
```

### 4.3. newCachedThreadPool

**创建一个可缓存的线程池**。

- 如果线程池大小超过处理任务所需要的线程数，就会回收部分空闲的线程；
- 如果长时间没有往线程池中提交任务，即如果工作线程空闲了指定的时间（默认为 1 分钟），则该工作线程将自动终止。终止后，如果你又提交了新的任务，则线程池重新创建一个工作线程。
- 此线程池不会对线程池大小做限制，线程池大小完全依赖于操作系统（或者说 `JVM`）能够创建的最大线程大小。 因此，使用 `CachedThreadPool` 时，一定要注意控制任务的数量，否则，由于大量线程同时运行，很有会造成系统瘫痪。

示例：

```
public class CachedThreadPoolDemo {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < 100; i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + " 执行");
                }
            });
        }
        executorService.shutdown();
    }

}
```

### 4.4. newScheduleThreadPool

创建一个大小无限的线程池。此线程池支持定时以及周期性执行任务的需求。

```
public class ScheduledThreadPoolDemo {

    public static void main(String[] args) {
        schedule();
        scheduleAtFixedRate();
    }

    private static void schedule() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
        for (int i = 0; i < 100; i++) {
            executorService.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + " 执行");
                }
            }, 1, TimeUnit.SECONDS);
        }
        executorService.shutdown();
    }

    private static void scheduleAtFixedRate() {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
        for (int i = 0; i < 100; i++) {
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + " 执行");
                }
            }, 1, 1, TimeUnit.SECONDS);
        }
        executorService.shutdown();
    }

}
```

### 4.5. newWorkStealingPool

Java 8 才引入。

其内部会构建 `ForkJoinPool`，利用 [Work-Stealing](https://en.wikipedia.org/wiki/Work_stealing) 算法，并行地处理任务，不保证处理顺序。

## 5. 线程池最佳实践

### 5.1. 计算线程数量

一般多线程执行的任务类型可以分为 `CPU` 密集型和 `I/O` 密集型，根据不同的任务类型，我们计算线程数的方法也不一样。

**CPU 密集型任务：**这种任务消耗的主要是 `CPU` 资源，可以将线程数设置为 N（`CPU` 核心数）+1，比 `CPU` 核心数多出来的一个线程是为了防止线程偶发的缺页中断，或者其它原因导致的任务暂停而带来的影响。一旦任务暂停，`CPU` 就会处于空闲状态，而在这种情况下多出来的一个线程就可以充分利用 `CPU` 的空闲时间。

**I/O 密集型任务：**这种任务应用起来，系统会用大部分的时间来处理 `I/O` 交互，而线程在处理 `I/O` 的时间段内不会占用 `CPU` 来处理，这时就可以将 `CPU` 交出给其它线程使用。因此在 `I/O` 密集型任务的应用中，我们可以多配置一些线程，具体的计算方法是 `2N`。

### 5.2. 建议使用有界阻塞队列

不建议使用 `Executors` 的最重要的原因是：`Executors` 提供的很多方法默认使用的都是无界的 `LinkedBlockingQueue`，高负载情境下，无界队列很容易导致 `OOM`，而 `OOM` 会导致所有请求都无法处理，这是致命问题。所以**强烈建议使用有界队列**。

《阿里巴巴 Java 开发手册》中提到，禁止使用这些方法来创建线程池，而应该手动 `new ThreadPoolExecutor` 来创建线程池。制订这条规则是因为容易导致生产事故，最典型的就是 `newFixedThreadPool` 和 `newCachedThreadPool`，可能因为资源耗尽导致 `OOM` 问题。

【示例】`newFixedThreadPool` OOM

```
ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
printStats(threadPool);
for (int i = 0; i < 100000000; i++) {
	threadPool.execute(() -> {
		String payload = IntStream.rangeClosed(1, 1000000)
			.mapToObj(__ -> "a")
			.collect(Collectors.joining("")) + UUID.randomUUID().toString();
		try {
			TimeUnit.HOURS.sleep(1);
		} catch (InterruptedException e) {
		}
		log.info(payload);
	});
}

threadPool.shutdown();
threadPool.awaitTermination(1, TimeUnit.HOURS);
```

`newFixedThreadPool` 使用的工作队列是 `LinkedBlockingQueue` ，而默认构造方法的 `LinkedBlockingQueue` 是一个 `Integer.MAX_VALUE` 长度的队列，可以认为是无界的。如果任务较多并且执行较慢的话，队列可能会快速积压，撑爆内存导致 OOM。

【示例】`newCachedThreadPool` OOM

```
ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();
printStats(threadPool);
for (int i = 0; i < 100000000; i++) {
	threadPool.execute(() -> {
		String payload = UUID.randomUUID().toString();
		try {
			TimeUnit.HOURS.sleep(1);
		} catch (InterruptedException e) {
		}
		log.info(payload);
	});
}
threadPool.shutdown();
threadPool.awaitTermination(1, TimeUnit.HOURS);
```

`newCachedThreadPool` 的最大线程数是 `Integer.MAX_VALUE`，可以认为是没有上限的，而其工作队列 `SynchronousQueue` 是一个没有存储空间的阻塞队列。这意味着，只要有请求到来，就必须找到一条工作线程来处理，如果当前没有空闲的线程就再创建一条新的。

如果大量的任务进来后会创建大量的线程。我们知道线程是需要分配一定的内存空间作为线程栈的，比如 1MB，因此无限制创建线程必然会导致 OOM。

### 5.3. 重要任务应该自定义拒绝策略

使用有界队列，当任务过多时，线程池会触发执行拒绝策略，线程池默认的拒绝策略会 `throw RejectedExecutionException` 这是个运行时异常，对于运行时异常编译器并不强制 `catch` 它，所以开发人员很容易忽略。因此**默认拒绝策略要慎重使用**。如果线程池处理的任务非常重要，建议自定义自己的拒绝策略；并且在实际工作中，自定义的拒绝策略往往和降级策略配合使用。

### 5.4. 不要对那些同步等待其它任务结果的任务排队

这可能会导致上面所描述的那种形式的死锁，在那种死锁中，所有线程都被一些任务所占用，这些任务依次等待排队任务的结果，而这些任务又无法执行，因为所有的线程都很忙。

### 5.5. 在为时间可能很长的操作使用合用的线程时要小心

如果程序必须等待诸如 `I/O` 完成这样的某个资源，那么请指定最长的等待时间，以及随后是失效还是将任务重新排队以便稍后执行。这样做保证了：通过将某个线程释放给某个可能成功完成的任务，从而将最终取得某些进展。

### 5.6. 理解任务

要有效地调整线程池大小，您需要理解正在排队的任务以及它们正在做什么。它们是 `CPU` 限制的（`CPU-bound`）吗？它们是 `I/O` 限制的（`I/O-bound`）吗？您的答案将影响您如何调整应用程序。如果您有不同的任务类，这些类有着截然不同的特征，那么为不同任务类设置多个工作队列可能会有意义，这样可以相应地调整每个池。



## 6.线程池自定义异常处理方法

**深入探究线程池的异常处理**

工作上的问题到这里就找到原因了，之后的解决过程也十分简单，这里就不提了。

但是疑问又来了，为什么使用线程池的时候，线程因异常被中断却没有抛出任何信息呢？还有平时如果是在 `main` 函数里面的异常也会被抛出来，而不是像线程池这样被吞掉。

如果子线程抛出了异常，线程池会如何进行处理呢？

> 我提交任务到线程池的方式是: `threadPoolExecutor.submit(Runnbale task);` ，后面了解到使用 `execute()` 方式提交任务会把异常日志给打出来，这里研究一下为什么使用 `submit` 提交任务，在任务中的异常会被“吞掉”。

对于 `submit()` 形式提交的任务，我们直接看源码：

```
public Future<?> submit(Runnable task) {
    if (task == null) throw new NullPointerException();
    // 被包装成 RunnableFuture 对象，然后准备添加到工作队列
    RunnableFuture<Void> ftask = newTaskFor(task, null);
    execute(ftask);
    return ftask;
}
```

它会被线程池包装成 `RunnableFuture` 对象，而最终它其实是一个 `FutureTask` 对象，在被添加到线程池的工作队列，然后调用 `start()` 方法后，`FutureTask` 对象的 `run()` 方法开始运行，即本任务开始执行。

```
public void run() {
    if (state != NEW || !UNSAFE.compareAndSwapObject(this,runnerOffset,null, Thread.currentThread()))
        return;
    try {
        Callable<V> c = callable;
        if (c != null && state == NEW) {
            V result;
            boolean ran;
            try {
                result = c.call();
                ran = true;
            } catch (Throwable ex) {
                // 捕获子任务中的异常
                result = null;
                ran = false;
                setException(ex);
            }
            if (ran)
                set(result);
        }
    } finally {
        runner = null;
        int s = state;
        if (s >= INTERRUPTING)
            handlePossibleCancellationInterrupt(s);
    }
}
```

在 `FutureTask` 对象的 `run()` 方法中，该任务抛出的异常被捕获，然后在 `setException(ex); ` 方法中，抛出的异常会被放到 outcome 对象中，这个对象就是 `submit() ` 方法会返回的 `FutureTask` 对象执行 `get()`  方法得到的结果。

但是在线程池中，并没有获取执行子线程的结果，所以异常也就没有被抛出来，即被“吞掉”了。

这就是线程池的 `submit()` 方法提交任务没有异常抛出的原因。

**线程池自定义异常处理方法**

在定义 `ThreadFactory` 的时候调用`setUncaughtExceptionHandler`方法，自定义异常处理方法。例如：

```
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("judge-pool-%d")
                .setUncaughtExceptionHandler((thread, throwable)-> logger.error("ThreadPool {} got exception", thread,throwable))
                .build();
```

这样，对于线程池中每条线程抛出的异常都会打下 error 日志，就不会看不到了。

## 7.CompletionService

### 7.1.ExecutorService VS CompletionService

假设我们有 4 个任务(A, B, C, D)用来执行复杂的计算，每个任务的执行时间随着输入参数的不同而不同，如果将任务提交到 `ExecutorService`， 相信你已经可以“信手拈来”

```java
ExecutorService executorService = Executors.newFixedThreadPool(4);
List<Future> futures = new ArrayList<Future<Integer>>();
futures.add(executorService.submit(A));
futures.add(executorService.submit(B));
futures.add(executorService.submit(C));
futures.add(executorService.submit(D));

// 遍历 Future list，通过 get() 方法获取每个 future 结果
for (Future future:futures) {
    Integer result = future.get();
    // 其他业务逻辑
}
```

先直入主题，用 `CompletionService` 实现同样的场景

```java
ExecutorService executorService = Executors.newFixedThreadPool(4);

// ExecutorCompletionService 是 CompletionService 唯一实现类
CompletionService executorCompletionService= new ExecutorCompletionService<>(executorService );

List<Future> futures = new ArrayList<Future<Integer>>();
futures.add(executorCompletionService.submit(A));
futures.add(executorCompletionService.submit(B));
futures.add(executorCompletionService.submit(C));
futures.add(executorCompletionService.submit(D));

// 遍历 Future list，通过 get() 方法获取每个 future 结果
for (int i=0; i<futures.size(); i++) {
    Integer result = executorCompletionService.take().get();
    // 其他业务逻辑
}
```

两种方式在代码实现上几乎一毛一样，我们曾经说过 `JDK` 中不会重复造轮子，如果要造一个新轮子，必定是原有的轮子在某些场景的使用上有致命缺陷

既然新轮子出来了，二者到底有啥不同呢？ 但是 `Future get()` 方法的致命缺陷:

> 如果 Future 结果没有完成，调用 get() 方法，程序会**阻塞**在那里，直至获取返回结果

先来看第一种实现方式，假设任务 A 由于参数原因，执行时间相对任务 B,C,D 都要长很多，但是按照程序的执行顺序，程序在 get() 任务 A 的执行结果会阻塞在那里，导致任务 B,C,D 的后续任务没办法执行。又因为每个任务执行时间是不固定的，**所以无论怎样调整将任务放到 List 的顺序，都不合适，这就是致命弊端**

新轮子自然要解决这个问题，它的设计理念就是哪个任务先执行完成，`get()` 方法就会获取到相应的任务结果，这么做的好处是什么呢？来看个图你就瞬间理解了

![1583165-20200812090855367-1588324368](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235146.png)

![1583165-20200812090857332-985732104](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235158.png)

两张图一对比，执行时长高下立判了，在当今高并发的时代，这点时间差，在吞吐量上起到的效果可能不是一点半点了

> 那 CompletionService 是怎么做到获取最先执行完的任务结果的呢？

### 7.2.远看CompletionService 轮廓

如果你使用过消息队列，你应该秒懂我要说什么了，`CompletionService` 实现原理很简单

> 就是一个将异步任务的生产和任务完成结果的消费解耦的服务

用人话解释一下上面的抽象概念我只能再画一张图了

![1583165-20200812090858396-1312267284](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235209.png)

说白了，哪个任务执行的完，就直接将执行结果放到队列中，这样消费者拿到的结果自然就是最早拿到的那个了

从上图中看到，有**任务**，有**结果队列**，那 `CompletionService` 自然也要围绕着几个关键字做文章了

- 既然是异步任务，那自然可能用到 Runnable 或 Callable
- 既然能获取到结果，自然也会用到 Future 了

带着这些线索，我们走进 **CompletionService** 源码看一看

### 7.3.近看 CompletionService 源码

`CompletionService` 是一个接口，它简单的只有 5 个方法：

```java
Future<V> submit(Callable<V> task);
Future<V> submit(Runnable task, V result);
Future<V> take() throws InterruptedException;
Future<V> poll();
Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
```

![1583165-20200812090859234-208312168](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235233.png)

另外 3 个方法都是从阻塞队列中获取并移除阻塞队列第一个元素，只不过他们的功能略有不同

- Take: 如果**队列为空**，那么调用 **take()** 方法的线程会**被阻塞**
- Poll: 如果**队列为空**，那么调用 **poll()** 方法的线程会**返回 null**
- Poll-timeout: 以**超时的方式**获取并移除阻塞队列中的第一个元素，如果超时时间到，队列还是空，那么该方法会返回 null

所以说，按大类划分上面5个方法，其实就是两个功能

- 提交异步任务 （submit）
- 从队列中拿取并移除第一个元素 (take/poll)

`CompletionService` 只是接口，`ExecutorCompletionService` 是该接口的唯一实现类

#### 7.3.1.ExecutorCompletionService 源码分析

先来看一下类结构, 实现类里面并没有多少内容

![1583165-20200812090859766-766495257](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235246.png)

`ExecutorCompletionService` 有两种构造函数：

```java
private final Executor executor;
private final AbstractExecutorService aes;
private final BlockingQueue<Future<V>> completionQueue;

public ExecutorCompletionService(Executor executor) {
    if (executor == null)
        throw new NullPointerException();
    this.executor = executor;
    this.aes = (executor instanceof AbstractExecutorService) ?
        (AbstractExecutorService) executor : null;
    this.completionQueue = new LinkedBlockingQueue<Future<V>>();
}
public ExecutorCompletionService(Executor executor,
                                 BlockingQueue<Future<V>> completionQueue) {
    if (executor == null || completionQueue == null)
        throw new NullPointerException();
    this.executor = executor;
    this.aes = (executor instanceof AbstractExecutorService) ?
        (AbstractExecutorService) executor : null;
    this.completionQueue = completionQueue;
}
```

两个构造函数都需要传入一个 Executor 线程池，**因为是处理异步任务的，我们是不被允许手动创建线程的**，所以这里要使用线程池也就很好理解了

另外一个参数是 BlockingQueue，如果不传该参数，就会默认队列为 `LinkedBlockingQueue`，任务执行结果就是加入到这个阻塞队列中的

所以要彻底理解 `ExecutorCompletionService` ，我们只需要知道一个问题的答案就可以了：

> 它是如何将异步任务结果放到这个阻塞队列中的？

想知道这个问题的答案，那只需要看它提交任务之后都做了些什么？

```java
public Future<V> submit(Callable<V> task) {
    if (task == null) throw new NullPointerException();
    RunnableFuture<V> f = newTaskFor(task);
    executor.execute(new QueueingFuture(f));
    return f;
}
```

我们前面也分析过，execute 是提交 Runnable 类型的任务，本身得不到返回值，但又可以将执行结果放到阻塞队列里面，所以肯定是在 QueueingFuture 里面做了文章

![1583165-20200812090900125-888416735](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235258.png)

从上图中看一看出，QueueingFuture 实现的接口非常多，所以说也就具备了相应的接口能力。

重中之重是，它继承了 FutureTask ，FutureTask 重写了 Runnable 的 run() 方法，无论是set() 正常结果，还是setException() 结果，都会调用 `finishCompletion()` 方法:

```java
private void finishCompletion() {
    // assert state > COMPLETING;
    for (WaitNode q; (q = waiters) != null;) {
        if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
            for (;;) {
                Thread t = q.thread;
                if (t != null) {
                    q.thread = null;
                    LockSupport.unpark(t);
                }
                WaitNode next = q.next;
                if (next == null)
                    break;
                q.next = null; // unlink to help gc
                q = next;
            }
            break;
        }
    }

  	// 重点 重点 重点
    done();

    callable = null;        // to reduce footprint
}
```

上述方法会执行 done() 方法，而 QueueingFuture 恰巧重写了 FutureTask 的 done() 方法：

![1583165-20200812090900299-142620864](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210323235342.png)

方法实现很简单，就是将 task 放到阻塞队列中

```java
protected void done() { 
  completionQueue.add(task); 
}
```

执行到此的 task 已经是前序步骤 set 过结果的 task，所以就可以通过消费阻塞队列获取相应的结果了

相信到这里，CompletionService 在你面前应该没什么秘密可言了

### 7.4.CompletionService 的主要用途

在 JDK docs 上明确给了两个例子来说明 CompletionService 的用途：

> 假设你有一组针对某个问题的solvers，每个都返回一个类型为Result的值，并且想要并发地运行它们，处理每个返回一个非空值的结果，在某些方法使用(Result r)

其实就是文中开头的使用方式

```java
 void solve(Executor e,
            Collection<Callable<Result>> solvers)
     throws InterruptedException, ExecutionException {
     CompletionService<Result> ecs
         = new ExecutorCompletionService<Result>(e);
     for (Callable<Result> s : solvers)
         ecs.submit(s);
     int n = solvers.size();
     for (int i = 0; i < n; ++i) {
         Result r = ecs.take().get();
         if (r != null)
             use(r);
     }
 }
```

> 假设你想使用任务集的第一个非空结果，忽略任何遇到异常的任务，并在第一个任务准备好时取消所有其他任务

```java
void solve(Executor e,
            Collection<Callable<Result>> solvers)
     throws InterruptedException {
     CompletionService<Result> ecs
         = new ExecutorCompletionService<Result>(e);
     int n = solvers.size();
     List<Future<Result>> futures
         = new ArrayList<Future<Result>>(n);
     Result result = null;
     try {
         for (Callable<Result> s : solvers)
             futures.add(ecs.submit(s));
         for (int i = 0; i < n; ++i) {
             try {
                 Result r = ecs.take().get();
                 if (r != null) {
                     result = r;
                     break;
                 }
             } catch (ExecutionException ignore) {}
         }
     }
     finally {
         for (Future<Result> f : futures)
           	// 注意这里的参数给的是 true，详解同样在前序 Future 源码分析文章中
             f.cancel(true);
     }

     if (result != null)
         use(result);
 }
```

这两种方式都是非常经典的 `CompletionService` 使用 **范式** ，请大家仔细品味每一行代码的用意

范式没有说明 `Executor` 的使用，使用 `ExecutorCompletionService`，需要自己创建线程池，看上去虽然有些麻烦，但好处是你可以让多个 `ExecutorCompletionService` 的线程池隔离，这种隔离性能避免几个特别耗时的任务拖垮整个应用的风险 （这也是我们反复说过多次的，**不要所有业务共用一个线程池**）

### 7.5.总结

`CompletionService` 的应用场景还是非常多的，比如

- `Dubbo` 中的 `Forking Cluster`
- 多仓库文件/镜像下载（从最近的服务中心下载后终止其他下载过程）
- 多服务调用（天气预报服务，最先获取到的结果）

`CompletionService` 不但能满足获取最快结果，还能起到一定 `load balancer` 作用，获取可用服务的结果，使用也非常简单， 只需要遵循范式即可



## X.技术文章

[Java线程池实现原理及其在美团业务中的实践](https://tech.meituan.com/2020/04/02/java-pooling-pratice-in-meituan.html)