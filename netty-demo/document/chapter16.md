# Netty 核心组件 EventLoop 源码剖析

## 源码剖析目的

Echo第一行代码就是 ：`EventLoopGroup bossGroup = new NioEventLoopGroup(1);` 下面分析其最核心的组件 EventLoop



## 源码剖析

### EventLoop 介绍

1. 首先看看 NioEventLoop 的继承图

   ![image-20201221114340898](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201221114340898.png)

   说明：

   - ScheduledExecutorService 接口表示是一个定时任务接口，EventLoop 接收定时任务
   - EventLoop 接口：Netty 接口文档说明该接口作用：一旦 Channel 注册了，就处理该 Channel 对应得所有 I/O 操作
   - SingleThreadEventExecutor 表示这是一个单个线程得线程池
   - EventLoop 是一个单例的线程池，里面含有一个死循环的线程不断的做着 3 件事：监听端口、处理端口事件、处理队列事件。每个 EventLoop都可以绑定多个 Channel，而每个 Channel 始终只能由一个 EventLoop 来处理 



### NioEventLoop 的使用 -- execute 方法

1. execute 源码剖析

   ![image-20201221115159389](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/netty-demo/image-20201221115159389.png)

   在 EventLoop 的使用，一般就是 `eventLoop.execute(task);` 看下 execute 方法的实现（在 SingleThreadEventExecutor 类中）

   ```
       @Override
       public void execute(Runnable task) {
           if (task == null) {
               throw new NullPointerException("task");
           }
   
           boolean inEventLoop = inEventLoop();
           if (inEventLoop) {
               addTask(task);
           } else {
               startThread();
               addTask(task);
               if (isShutdown() && removeTask(task)) {
                   reject();
               }
           }
   
           if (!addTaskWakesUp && wakesUpForTask(task)) {
               wakeup(inEventLoop);
           }
       }
   ```

   说明：

   - 首先判断该 EventLoop 的线程是否是当前线程，如果是，直接添加到任务队列中去，如果不是，则尝试启动线程（但由于线程是单个的，因此只能启动一次），随后再将任务添加到队列中去。
   - 如果线程已经停止，并且删除任务失败，则执行拒绝策略，默认是抛出异常
   - 如果 addTaskWakesUp 是 false，并且任务不是 NonWakeupRunnable 类型的，就尝试唤醒 selector。这个时候，阻塞在 selector 的线程就会立即返回



2. addTask 和 offerTask 方法源码

   ```
       protected void addTask(Runnable task) {
           if (task == null) {
               throw new NullPointerException("task");
           }
           if (!offerTask(task)) {
               reject(task);
           }
       }
   
       final boolean offerTask(Runnable task) {
           if (isShutdown()) {
               reject();
           }
           return taskQueue.offer(task);
       }
   ```

   



### NioEventLoop 的父类 SingleThreadEventExecutor 的 startThread 方法

1. 当执行 execute 方法的时候，如果当前线程不是 EventLoop 所属线程，则尝试启动线程，也就是 startThread 方法，代码如下：

   ```
       private void startThread() {
           if (state == ST_NOT_STARTED) {
               if (STATE_UPDATER.compareAndSet(this, ST_NOT_STARTED, ST_STARTED)) {
                   try {
                       doStartThread();
                   } catch (Throwable cause) {
                       STATE_UPDATER.set(this, ST_NOT_STARTED);
                       PlatformDependent.throwException(cause);
                   }
               }
           }
       }
   ```

   说明：

   - 该方法首先判断是否启动过了，保证 EventLoop 只有一个线程，如果没有启动过，则尝试使用 CAS 将 state 状态改为 ST_STARTED，也就是已启动。然后调用 doStartThread 方法。如果失败，则进行回滚

   **do StartThread 方法**

   ```
       private void doStartThread() {
           assert thread == null;
           executor.execute(new Runnable() {
               @Override
               public void run() {
                   thread = Thread.currentThread();
                   if (interrupted) {
                       thread.interrupt();
                   }
   
                   boolean success = false;
                   updateLastExecutionTime();
                   try {
                       SingleThreadEventExecutor.this.run();
                       success = true;
                   } catch (Throwable t) {
                       logger.warn("Unexpected exception from an event executor: ", t);
                   } finally {
                       for (;;) {
                           int oldState = state;
                           if (oldState >= ST_SHUTTING_DOWN || STATE_UPDATER.compareAndSet(
                                   SingleThreadEventExecutor.this, oldState, ST_SHUTTING_DOWN)) {
                               break;
                           }
                       }
   
                       // Check if confirmShutdown() was called at the end of the loop.
                       if (success && gracefulShutdownStartTime == 0) {
                           logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " +
                                   SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " +
                                   "before run() implementation terminates.");
                       }
   
                       try {
                           // Run all remaining tasks and shutdown hooks.
                           for (;;) {
                               if (confirmShutdown()) {
                                   break;
                               }
                           }
                       } finally {
                           try {
                               cleanup();
                           } finally {
                               STATE_UPDATER.set(SingleThreadEventExecutor.this, ST_TERMINATED);
                               threadLock.release();
                               if (!taskQueue.isEmpty()) {
                                   logger.warn(
                                           "An event executor terminated with " +
                                                   "non-empty task queue (" + taskQueue.size() + ')');
                               }
   
                               terminationFuture.setSuccess(null);
                           }
                       }
                   }
               }
           });
       }
   ```

   说明：

   - 首先调用 executor 的 execute 方法，这个 executor 就是在创建 EventLoopGroup 的时候创建的 ThreadPerTaskExecutor 类。该 execute 方法会将 Runnable 包装成 Netty 的 FastThreadLocalThread
   - 任务中，首先判断线程中断状态，然后设置最后一次的执行时间
   - 执行当前 NioEventLoop 的 run 方法，注意：这个方法是个死循环，是整个 EventLoop 的核心
   - 在 finally 块中，使用 CAS 不断修改 state 状态，改成 ST_SHUTTING_DOWN。也就是当前线程 Loop 结束的时候， 关闭线程。最后还要死循环确认是否关闭，否则不会 break。然后执行 cleanup 操作，更新状态为 ST_TERMINATED ，并释放当前线程锁。如果任务队列不是空，则打印队列中还有多少个未完成的任务，并回调  terminationFuture 方法
   - 其实最核心的就是 EventLoop 自身的 run 方法



### EventLoop 中的 Loop 是靠 run 实现的，我们分析下 run 方法（该方法在 NioEventLoop）

```
    @Override
    protected void run() {
        for (;;) {
            try {
                switch (selectStrategy.calculateStrategy(selectNowSupplier, hasTasks())) {
                    case SelectStrategy.CONTINUE:
                        continue;
                    case SelectStrategy.SELECT:
                        select(wakenUp.getAndSet(false));

                        // 'wakenUp.compareAndSet(false, true)' is always evaluated
                        // before calling 'selector.wakeup()' to reduce the wake-up
                        // overhead. (Selector.wakeup() is an expensive operation.)
                        //
                        // However, there is a race condition in this approach.
                        // The race condition is triggered when 'wakenUp' is set to
                        // true too early.
                        //
                        // 'wakenUp' is set to true too early if:
                        // 1) Selector is waken up between 'wakenUp.set(false)' and
                        //    'selector.select(...)'. (BAD)
                        // 2) Selector is waken up between 'selector.select(...)' and
                        //    'if (wakenUp.get()) { ... }'. (OK)
                        //
                        // In the first case, 'wakenUp' is set to true and the
                        // following 'selector.select(...)' will wake up immediately.
                        // Until 'wakenUp' is set to false again in the next round,
                        // 'wakenUp.compareAndSet(false, true)' will fail, and therefore
                        // any attempt to wake up the Selector will fail, too, causing
                        // the following 'selector.select(...)' call to block
                        // unnecessarily.
                        //
                        // To fix this problem, we wake up the selector again if wakenUp
                        // is true immediately after selector.select(...).
                        // It is inefficient in that it wakes up the selector for both
                        // the first case (BAD - wake-up required) and the second case
                        // (OK - no wake-up required).

                        if (wakenUp.get()) {
                            selector.wakeup();
                        }
                        // fall through
                    default:
                }

                cancelledKeys = 0;
                needsToSelectAgain = false;
                final int ioRatio = this.ioRatio;
                if (ioRatio == 100) {
                    try {
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        runAllTasks();
                    }
                } else {
                    final long ioStartTime = System.nanoTime();
                    try {
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        final long ioTime = System.nanoTime() - ioStartTime;
                        runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
            // Always handle shutdown even if the loop processing threw an exception.
            try {
                if (isShuttingDown()) {
                    closeAll();
                    if (confirmShutdown()) {
                        return;
                    }
                }
            } catch (Throwable t) {
                handleLoopException(t);
            }
        }
    }
```

说明：

- 从上面的步骤可以看出，整个 run 方法做了 3 件事：
  - select 获取感兴趣的事件
  - processSelectedKeys 处理事件
  - runAllTasks 执行队列中的任务



核心 select 方法 解析

```
    private void select(boolean oldWakenUp) throws IOException {
        Selector selector = this.selector;
        try {
            int selectCnt = 0;
            long currentTimeNanos = System.nanoTime();
            long selectDeadLineNanos = currentTimeNanos + delayNanos(currentTimeNanos);
            for (;;) {
                long timeoutMillis = (selectDeadLineNanos - currentTimeNanos + 500000L) / 1000000L;
                if (timeoutMillis <= 0) {
                    if (selectCnt == 0) {
                        selector.selectNow();
                        selectCnt = 1;
                    }
                    break;
                }

                // If a task was submitted when wakenUp value was true, the task didn't get a chance to call
                // Selector#wakeup. So we need to check task queue again before executing select operation.
                // If we don't, the task might be pended until select operation was timed out.
                // It might be pended until idle timeout if IdleStateHandler existed in pipeline.
                if (hasTasks() && wakenUp.compareAndSet(false, true)) {
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                int selectedKeys = selector.select(timeoutMillis);
                selectCnt ++;
				
				// 如果 1 秒后返回，有返回值|| select 被用户唤醒 || 任务队列有任务 || 有定时任务即将被执行；则跳出循环
                if (selectedKeys != 0 || oldWakenUp || wakenUp.get() || hasTasks() || hasScheduledTasks()) {
                    // - Selected something,
                    // - waken up by user, or
                    // - the task queue has a pending task.
                    // - a scheduled task is ready for processing
                    break;
                }
                if (Thread.interrupted()) {
                    // Thread was interrupted so reset selected keys and break so we not run into a busy loop.
                    // As this is most likely a bug in the handler of the user or it's client library we will
                    // also log it.
                    //
                    // See https://github.com/netty/netty/issues/2426
                    if (logger.isDebugEnabled()) {
                        logger.debug("Selector.select() returned prematurely because " +
                                "Thread.currentThread().interrupt() was called. Use " +
                                "NioEventLoop.shutdownGracefully() to shutdown the NioEventLoop.");
                    }
                    selectCnt = 1;
                    break;
                }

                long time = System.nanoTime();
                if (time - TimeUnit.MILLISECONDS.toNanos(timeoutMillis) >= currentTimeNanos) {
                    // timeoutMillis elapsed without anything selected.
                    selectCnt = 1;
                } else if (SELECTOR_AUTO_REBUILD_THRESHOLD > 0 &&
                        selectCnt >= SELECTOR_AUTO_REBUILD_THRESHOLD) {
                    // The selector returned prematurely many times in a row.
                    // Rebuild the selector to work around the problem.
                    logger.warn(
                            "Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                            selectCnt, selector);

                    rebuildSelector();
                    selector = this.selector;

                    // Select again to populate selectedKeys.
                    selector.selectNow();
                    selectCnt = 1;
                    break;
                }

                currentTimeNanos = time;
            }

            if (selectCnt > MIN_PREMATURE_SELECTOR_RETURNS) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Selector.select() returned prematurely {} times in a row for Selector {}.",
                            selectCnt - 1, selector);
                }
            }
        } catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector {} - JDK bug?",
                        selector, e);
            }
            // Harmless exception - log anyway
        }
    }
```

说明：

- 调用 selector 的 select 方法，默认阻塞一秒钟，如果有定时任务，则在定时任务剩余时间的基础上再加上 0.5   秒进行阻塞。当执行 execute 方法的时候，也就是添加任务的时候，唤醒 selector，防止 selector 阻塞时间过长



## EventLoop 作为 Netty 的核心的运行机制小结

每次执行 execute 方法都是向队列添加任务。当第一次添加时就启动线程，执行 run 方法，而 run 方法是整个 EventLoop 的核心，就像 EventLoop 的名字一样， 不停 Loop。Loop 做了下面 3 件事：

- 调用 selector 的 select 方法，默认阻塞一秒钟，如果有定时任务，则在定时任务剩余时间的基础上再加上0.5秒进行阻塞。当执行 execute 方法的时候，也就是添加任务的时候，唤醒 selector，防止 selector 阻塞时间过长
- 调用 selector 返回的时候，回调用 processSelectedKeys 方法对 selectKey 进行处理
- 当 processSelectedKeys 方法执行结束后，则按照 ioRatio 的比例执行 runAllTasks 方法，默认是 IO任务时间和非 IO 任务时间相同的，可以根据应用特点进行调优。比如非 IO 任务比较多，那么你就将 ioRatio 调小一点，这样非 IO 任务就能执行得长一点，防止队列积攒过多的任务



































