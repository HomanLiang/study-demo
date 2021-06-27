[toc]



# Java 生成随机数

## 1.Random

Random 类诞生于 JDK 1.0，它产生的随机数是伪随机数，也就是有规则的随机数。Random 使用的随机算法为 linear congruential pseudorandom number generator (LGC) 线性同余法伪随机数。在随机数生成时，随机算法的起源数字称为种子数（seed），在种子数的基础上进行一定的变换，从而产生需要的随机数字。

**Random 对象在种子数相同的情况下，相同次数生成的随机数是相同的**。比如两个种子数相同的 Random 对象，第一次生成的随机数字完全相同，第二次生成的随机数字也完全相同。**默认情况下 new Random() 使用的是当前纳秒时间作为种子数的**。

### 1.1.基础使用

使用 Random 生成一个从 0 到 10 的随机数（不包含 10），实现代码如下：

```java
// 生成 Random 对象
Random random = new Random();
for (int i = 0; i < 10; i++) {
    // 生成 0-9 随机整数
    int number = random.nextInt(10);
    System.out.println("生成随机数：" + number);
}
```

以上程序的执行结果为：
![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626193743.png)

### 1.2.优缺点分析

Random 使用 LGC 算法生成伪随机数的**优点是执行效率比较高，生成的速度比较快**。

它的**缺点是如果 Random 的随机种子一样的话，每次生成的随机数都是可预测的（都是一样的）**。如下代码所示，当我们给两个线程设置相同的种子数的时候，会发现每次产生的随机数也是相同的：

```java
 // 创建两个线程
for (int i = 0; i < 2; i++) {
    new Thread(() -> {
        // 创建 Random 对象，设置相同的种子
        Random random = new Random(1024);
        // 生成 3 次随机数
        for (int j = 0; j < 3; j++) {
            // 生成随机数
            int number = random.nextInt();
            // 打印生成的随机数
            System.out.println(Thread.currentThread().getName() + ":" +
                               number);
            // 休眠 200 ms
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("---------------------");
        }
    }).start();
}
```

以上程序的执行结果为：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626193841.png)

### 1.3.线程安全问题

当我们要使用一个类时，我们首先关心的第一个问题是：它是否为线程安全？对于 Random 来说，**Random 是线程安全的**。

> PS：线程安全指的是在多线程的场景下，程序的执行结果和预期的结果一致，就叫线程安全的，否则则为非线程安全的（也叫线程安全问题）。比如有两个线程，第一个线程执行 10 万次 ++ 操作，第二个线程执行 10 万次 -- 操作，那么最终的结果应该是没加也没减，如果程序最终的结果和预期不符，则为非线程安全的。

我们来看 Random 的实现源码：

```java
public Random() {
    this(seedUniquifier() ^ System.nanoTime());
}

public int nextInt() {
    return next(32);
}

protected int next(int bits) {
    long oldseed, nextseed;
    AtomicLong seed = this.seed;
    do {
        oldseed = seed.get();
        nextseed = (oldseed * multiplier + addend) & mask;
    } while (!seed.compareAndSet(oldseed, nextseed)); // CAS（Compare and Swap）生成随机数
    return (int)(nextseed >>> (48 - bits));
}
```

> PS：本文所有源码来自于 JDK 1.8.0_211。

从以上源码可以看出，Random 底层使用的是 CAS（Compare and Swap，比较并替换）来解决线程安全问题的，因此对于绝大数随机数生成的场景，使用 Random 不乏为一种很好的选择。
​

> PS：Java 并发机制实现原子操作有两种：一种是锁，一种是 CAS。
> ​

> CAS 是 Compare And Swap（比较并替换）的缩写，java.util.concurrent.atomic 中的很多类，如（AtomicInteger AtomicBoolean AtomicLong等）都使用了 CAS 机制来实现。

## 2.ThreadLocalRandom

ThreadLocalRandom 是 JDK 1.7 新提供的类，它属于 JUC（java.util.concurrent）下的一员，为什么有了 Random 之后还会再创建一个 ThreadLocalRandom？

原因很简单，通过上面 Random 的源码我们可以看出，Random 在生成随机数时使用的 CAS 来解决线程安全问题的，然而 CAS 在线程竞争比较激烈的场景中效率是非常低的**，原因是 CAS 对比时老有其他的线程在修改原来的值，所以导致 CAS 对比失败，所以它要一直循环来尝试进行 CAS 操作。所以**在多线程竞争比较激烈的场景可以使用 ThreadLocalRandom 来解决 Random 执行效率比较低的问题。

当我们第一眼看到 ThreadLocalRandom 的时候，一定会联想到一次类 ThreadLocal，确实如此。**ThreadLocalRandom 的实现原理与 ThreadLocal 类似，它相当于给每个线程一个自己的本地种子，从而就可以避免因多个线程竞争一个种子，而带来的额外性能开销了**。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626193920.png)

### 2.1.基础使用

接下来我们使用 ThreadLocalRandom 来生成一个 0 到 10 的随机数（不包含 10），实现代码如下：

```java
// 得到 ThreadLocalRandom 对象
ThreadLocalRandom random = ThreadLocalRandom.current();
for (int i = 0; i < 10; i++) {
    // 生成 0-9 随机整数
    int number = random.nextInt(10);
    // 打印结果
    System.out.println("生成随机数：" + number);
}
```

以上程序的执行结果为：
![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626194002.png)

### 2.2.实现原理

ThreadLocalRandom 的实现原理和 ThreadLocal 类似，它是让每个线程持有自己的本地种子，该种子在生成随机数时候才会被初始化，实现源码如下：

```java
public int nextInt(int bound) {
    // 参数效验
    if (bound <= 0)
        throw new IllegalArgumentException(BadBound);
    // 根据当前线程中种子计算新种子
    int r = mix32(nextSeed());
    int m = bound - 1;
    // 根据新种子和 bound 计算随机数
    if ((bound & m) == 0) // power of two
        r &= m;
    else { // reject over-represented candidates
        for (int u = r >>> 1;
             u + m - (r = u % bound) < 0;
             u = mix32(nextSeed()) >>> 1)
            ;
    }
    return r;
}

final long nextSeed() {
    Thread t; long r; // read and update per-thread seed
    // 获取当前线程中 threadLocalRandomSeed 变量，然后在种子的基础上累加 GAMMA 值作为新种子
    // 再使用 UNSAFE.putLong 将新种子存放到当前线程的 threadLocalRandomSeed 变量中
    UNSAFE.putLong(t = Thread.currentThread(), SEED,
                   r = UNSAFE.getLong(t, SEED) + GAMMA); 
    return r;
}
```

### 2.3.优缺点分析

ThreadLocalRandom 结合了 Random 和 ThreadLocal 类，并被隔离在当前线程中。因此它通过避免竞争操作种子数，从而**在多线程运行的环境中实现了更好的性能**，而且也保证了它的**线程安全**。

另外，不同于 Random， ThreadLocalRandom 明确不支持设置随机种子。它重写了 Random 的
`setSeed(long seed)` 方法并直接抛出了 `UnsupportedOperationException` 异常，因此**降低了多个线程出现随机数重复的可能性**。

源码如下：

```java
public void setSeed(long seed) {
    // only allow call from super() constructor
    if (initialized)
        throw new UnsupportedOperationException();
}
```

只要程序中调用了 setSeed() 方法就会抛出 `UnsupportedOperationException` 异常，如下图所示：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626194027.png)

**ThreadLocalRandom 缺点分析**

虽然 ThreadLocalRandom 不支持手动设置随机种子的方法，但并不代表 ThreadLocalRandom 就是完美的，当我们查看 ThreadLocalRandom 初始化随机种子的方法 initialSeed() 源码时发现，默认情况下它的随机种子也是以当前时间有关，源码如下：

```java
private static long initialSeed() {
    // 尝试获取 JVM 的启动参数
    String sec = VM.getSavedProperty("java.util.secureRandomSeed");
    // 如果启动参数设置的值为 true，则参数一个随机 8 位的种子
    if (Boolean.parseBoolean(sec)) {
        byte[] seedBytes = java.security.SecureRandom.getSeed(8);
        long s = (long)(seedBytes[0]) & 0xffL;
        for (int i = 1; i < 8; ++i)
            s = (s << 8) | ((long)(seedBytes[i]) & 0xffL);
        return s;
    }
    // 如果没有设置启动参数，则使用当前时间有关的随机种子算法
    return (mix64(System.currentTimeMillis()) ^
            mix64(System.nanoTime()));
}
```

从上述源码可以看出，当我们设置了启动参数“-Djava.util.secureRandomSeed=true”时，ThreadLocalRandom 会产生一个随机种子，一定程度上能缓解随机种子相同所带来随机数可预测的问题，然而**默认情况下如果不设置此参数，那么在多线程中就可以因为启动时间相同，而导致多个线程在每一步操作中都会生成相同的随机数**。

## 3.SecureRandom

SecureRandom 继承自 Random，该类提供加密强随机数生成器。**SecureRandom 不同于 Random，它收集了一些随机事件，比如鼠标点击，键盘点击等，SecureRandom 使用这些随机事件作为种子。这意味着，种子是不可预测的**，而不像 Random 默认使用系统当前时间的毫秒数作为种子，从而避免了生成相同随机数的可能性。

### 3.1.基础使用

```java
// 创建 SecureRandom 对象，并设置加密算法
SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
for (int i = 0; i < 10; i++) {
    // 生成 0-9 随机整数
    int number = random.nextInt(10);
    // 打印结果
    System.out.println("生成随机数：" + number);
}
```

以上程序的执行结果为：

![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626194057.png)
SecureRandom 默认支持两种加密算法：

1. SHA1PRNG 算法，提供者 sun.security.provider.SecureRandom；
2. NativePRNG 算法，提供者 sun.security.provider.NativePRNG。

当然除了上述的操作方式之外，你还可以选择使用 `new SecureRandom()` 来创建 SecureRandom 对象，实现代码如下：

```java
SecureRandom secureRandom = new SecureRandom();
```

通过 new 初始化 SecureRandom，默认会使用 NativePRNG 算法来生成随机数，但是也可以配置 JVM 启动参数“-Djava.security”参数来修改生成随机数的算法，或选择使用 `getInstance("算法名称")` 的方式来指定生成随机数的算法。

## 4.Math

Math 类诞生于 JDK 1.0，它里面包含了用于执行基本数学运算的属性和方法，如初等指数、对数、平方根和三角函数，当然它里面也包含了生成随机数的静态方法 `Math.random()` ，**此方法会产生一个 0 到 1 的 double 值**，如下代码所示。

### 4.1.基础使用

```java
for (int i = 0; i < 10; i++) {
    // 产生随机数
    double number = Math.random();
    System.out.println("生成随机数：" + number);
}
```

以上程序的执行结果为：
![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626194212.png)

### 4.2.扩展

当然如果你想**用它来生成一个一定范围的 int 值**也是可以的，你可以这样写：

```java
for (int i = 0; i < 10; i++) {
    // 生成一个从 0-99 的整数
    int number = (int) (Math.random() * 100);
    System.out.println("生成随机数：" + number);
}
```



以上程序的执行结果为：
![image.png](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210626194237.png)

### 4.3.实现原理

通过分析 `Math` 的源码我们可以得知：当第一次调用 `Math.random()` 方法时，自动创建了一个伪随机数生成器，**实际上用的是 **`new java.util.Random()`，当下一次继续调用 `Math.random()` 方法时，就会使用这个新的伪随机数生成器。

源码如下：

```java
public static double random() {
    return RandomNumberGeneratorHolder.randomNumberGenerator.nextDouble();
}

private static final class RandomNumberGeneratorHolder {
    static final Random randomNumberGenerator = new Random();
}
```

## 5.总结

本文我们介绍了 4 种生成随机数的方法，其中 Math 是对 Random 的封装，所以二者比较类似。Random 生成的是伪随机数，是以当前纳秒时间作为种子数的，并且在多线程竞争比较激烈的情况下因为要进行 CAS 操作，所以存在一定的性能问题，但**对于绝大数应用场景来说，使用 Random 已经足够了。当在竞争比较激烈的场景下可以使用 ThreadLocalRandom 来替代 Random，但如果对安全性要求比较高的情况下，可以使用 SecureRandom 来生成随机数**，因为 SecureRandom 会收集一些随机事件来作为随机种子，所以 SecureRandom 可以看作是生成真正随机数的一个工具类。













