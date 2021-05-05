[toc]

# Java String 类型

## 1. String 的不可变性

我们先来看下 `String` 的定义：

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];
```

`String` 类被 `final` 关键字修饰，表示**不可继承 `String` 类**。

`String` 类的数据存储于 `char[]` 数组，这个数组被 `final` 关键字修饰，表示 **`String` 对象不可被更改**。

为什么 Java 要这样设计？

- **保证 String 对象安全性**。避免 String 被篡改。

- **保证 hash 值不会频繁变更**。

- **可以实现字符串常量池**。

通常有两种创建字符串对象的方式：

- 一种是通过字符串常量的方式创建，如 `String str="abc";` 
- 另一种是字符串变量通过 new 形式的创建，如 `String str = new String("abc")`。

使用第一种方式创建字符串对象时，JVM 首先会检查该对象是否在字符串常量池中，如果在，就返回该对象引用，否则新的字符串将在常量池中被创建。这种方式可以减少同一个值的字符串对象的重复创建，节约内存。

`String str = new String("abc")` 这种方式，首先在编译类文件时，`"abc"` 常量字符串将会放入到常量结构中，在类加载时，`"abc"` 将会在常量池中创建；其次，在调用 new 时，JVM 命令将会调用 `String` 的构造函数，同时引用常量池中的 `"abc"` 字符串，在堆内存中创建一个 `String` 对象；最后，str 将引用 `String` 对象。

## 2. String 的性能考量

### 2.1. 字符串拼接

**字符串常量的拼接，编译器会将其优化为一个常量字符串**。

【示例】字符串常量拼接

```java
public static void main(String[] args) {
    // 本行代码在 class 文件中，会被编译器直接优化为：
    // String str = "abc";
    String str = "a" + "b" + "c";
    System.out.println("str = " + str);
}
```

**字符串变量的拼接，编译器会优化成 `StringBuilder` 的方式**。

【示例】字符串变量的拼接

```java
public static void main(String[] args) {
    String str = "";
    for(int i=0; i<1000; i++) {
        // 本行代码会被编译器优化为：
        // str = (new StringBuilder(String.valueOf(str))).append(i).toString();
        str = str + i;
    }
}
```

但是，每次循环都会生成一个新的 `StringBuilder` 实例，同样也会降低系统的性能。

字符串拼接的正确方案：

- 如果需要使用**字符串拼接，应该优先考虑 `StringBuilder` 的 `append` 方法替代使用 `+` 号**。
- 如果在并发编程中，`String` 对象的拼接涉及到线程安全，可以使用 `StringBuffer`。但是要注意，由于 `StringBuffer` 是线程安全的，涉及到锁竞争，所以从性能上来说，要比 `StringBuilder` 差一些。

### 2.2. 字符串分割

**`String` 的 `split()` 方法使用正则表达式实现其强大的分割功能**。而正则表达式的性能是非常不稳定的，使用不恰当会引起回溯问题，很可能导致 CPU 居高不下。

所以，应该慎重使用 `split()` 方法，**可以考虑用 `String.indexOf()` 方法代替 `split()` 方法完成字符串的分割**。如果实在无法满足需求，你就在使用 Split() 方法时，对回溯问题加以重视就可以了。

### 2.3. String.intern

**在每次赋值的时候使用 `String` 的 `intern` 方法，如果常量池中有相同值，就会重复使用该对象，返回对象引用，这样一开始的对象就可以被回收掉**。

在字符串常量中，默认会将对象放入常量池；在字符串变量中，对象是会创建在堆内存中，同时也会在常量池中创建一个字符串对象，复制到堆内存对象中，并返回堆内存对象引用。

如果调用 `intern` 方法，会去查看字符串常量池中是否有等于该对象的字符串，如果没有，就在常量池中新增该对象，并返回该对象引用；如果有，就返回常量池中的字符串引用。堆内存中原有的对象由于没有引用指向它，将会通过垃圾回收器回收。

【示例】

```java
public class SharedLocation {
	private String city;
	private String region;
	private String countryCode;
}

SharedLocation sharedLocation = new SharedLocation();
sharedLocation.setCity(messageInfo.getCity().intern());		sharedLocation.setCountryCode(messageInfo.getRegion().intern());
sharedLocation.setRegion(messageInfo.getCountryCode().intern());
```

> 使用 `intern` 方法需要注意：一定要结合实际场景。因为常量池的实现是类似于一个 HashTable 的实现方式，HashTable 存储的数据越大，遍历的时间复杂度就会增加。如果数据过大，会增加整个字符串常量池的负担。

## 3. String、StringBuffer、StringBuilder 有什么区别

`String` 是 Java 语言非常基础和重要的类，提供了构造和管理字符串的各种基本逻辑。它是典型的 `Immutable` 类，被声明成为 `final class`，所有属性也都是 `final` 的。也由于它的不可变性，类似拼接、裁剪字符串等动作，都会产生新的 `String` 对象。由于字符串操作的普遍性，所以相关操作的效率往往对应用性能有明显影响。

`StringBuffer` 是为解决上面提到拼接产生太多中间对象的问题而提供的一个类，我们可以用 `append` 或者 `add` 方法，把字符串添加到已有序列的末尾或者指定位置。`StringBuffer` 是一个**线程安全的**可修改字符序列。`StringBuffer` 的线程安全是通过在各种修改数据的方法上用 `synchronized` 关键字修饰实现的。

`StringBuilder` 是 Java 1.5 中新增的，在能力上和 StringBuffer 没有本质区别，但是它去掉了线程安全的部分，有效减小了开销，是绝大部分情况下进行字符串拼接的首选。

`StringBuffer` 和 `StringBuilder` 底层都是利用可修改的（char，JDK 9 以后是 byte）数组，二者都继承了 `AbstractStringBuilder`，里面包含了基本操作，区别仅在于最终的方法是否加了 `synchronized`。构建时初始字符串长度加 16（这意味着，如果没有构建对象时输入最初的字符串，那么初始值就是 16）。我们如果确定拼接会发生非常多次，而且大概是可预计的，那么就可以指定合适的大小，避免很多次扩容的开销。扩容会产生多重开销，因为要抛弃原有数组，创建新的（可以简单认为是倍数）数组，还要进行 `arraycopy`。

**除非有线程安全的需要，不然一般都使用 `StringBuilder`**。

## 4. 字符串常量池

**字符串常量池的设计意图是什么？**

字符串的分配，和其他的对象分配一样，耗费高昂的时间与空间代价。

JVM为了提高性能和减少内存开销，在实例化字符串常量的时候进行了一些优化：

- 为了减少在JVM中创建的字符串的数量，字符串类维护了一个字符串池，每当代码创建字符串常量时，JVM会首先检查字符串常量池；

- 如果字符串已经存在池中，就返回池中的实例引用；

- 如果字符串不在池中，就会实例化一个字符串并放到池中。Java能够进行这样的优化是因为字符串是不可变的，可以不用担心数据冲突进行共享；

实现的基础：

- 实现该优化的基础是因为字符串是不可变的，可以不用担心数据冲突进行共享；

- 运行时实例创建的全局字符串常量池中有一个表，总是为池中每个唯一的字符串对象维护一个引用,这就意味着它们一直引用着字符串常量池中的对象，所以，在常量池中的这些字符串不会被垃圾收集器回收

我们来看下面一段代码，就是从字符串常量池中获取相应的字符串：

```
String str1 = “hello”;
String str2 = “hello”;
System.out.printl（"str1 == str2" : str1 == str2 ) //true 
```

**字符串常量池在哪里？**

在分析字符串常量池的位置时，首先得了解JVM内存模型，JVM内存区域分为线程共享区和线程独占区；线程共享区包括堆和方法区；线程独占区包括Java虚拟机栈、本地方法栈和陈程序计数器。

![image-20210320195636031](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320195636.png)

程序计数器

- 是一块比较小的内存区域，是唯一一个不会发生OutOfMemoryError的区域，可以这样理解方法进栈后，每一行代码都有一个标识，程序按着标识往下执行。

Java虚拟机栈：

- 每个方法执行，都会创建一个栈帧，方法调用进栈，方法结束出栈；
- 栈帧里面存放着局部变量表，操作数栈，动态链接以及方法出口等；
- 局部变量表里面存放着基本数据类型，引用类型等；
- 栈帧伴随着方法的开始而开始，结束而结束；
- 局部变量表所需的内存空间在编译期间就完成了分配，在运行期间是不会改变的；
- 栈很容易出现StackOverFlowError，栈内存溢出错误，常见于递归调用；

本地方法栈和Java虚拟机栈

- 其实是差不多的，但是也是有区别的Java虚拟机栈为Java方法服务，本地方法栈为native方法服务

堆

- 功能单一，就是存储对象的实例，堆其实又分新生代和老年代；
- 新生代又分Eden、Survivor01和Survivor02三个区域，垃圾收集器主要管理的区域，Eden区回收效率很高。
- 并不是所有的对象实例都会分配到堆上去，Java虚拟机栈也会分配。堆很容易出现OutOfMemoryError错误，内存溢出

方法区

- 存放加载的类信息、常量、静态变量，静态代码块等信息；
- 类信息包括类的版本、字段、方法、接口等，方法区也被称为永久代。

而我们所说的字符串常量池存在于方法区。

**如何操作字符串常量池？**

JVM实例化字符串常量池时

```
String str1 = "hello";
String str2 = "hello";
System.out.println("str1 == str2" : str1 == str2 ) //true
```

String.intern()

通过new操作符创建的字符串对象不指向字符串池中的任何对象，但是可以通过使用字符串的intern()方法来指向其中的某一个。java.lang.String.intern()返回一个保留池字符串，就是一个在全局字符串池中有了一个入口。如果以前没有在全局字符串池中，那么它就会被添加到里面。

```
String s1 = "Hello";
String s2 = new String("Hello");
String s3 = s2.intern();
System.out.println("s1 == s3? " + (s1 == s3)); // true
```





## X.面试题

### X.1.String s = new String("xyz");产生了几个对象？

面试官Q1：请问 `String s = new String("xyz");` 产生了几个对象？

对于这个Java面试题，老套路先上代码：

```
public class StringTest {
    public static void main(String[] args){
        String s1="Hello";
        String s2="Hello";
        String s3=new String("Hello");
        System.out.println("s1和s2 引用地址是否相同："+(s1 == s2));
        System.out.println("s1和s2 值是否相同："+s1.equals(s2));
        System.out.println("s1和s3 引用地址是否相同："+(s1 == s3));
        System.out.println("s1和s3 值是否相同："+s1.equals(s3));
    }
}
```

打印结果如下：

```
s1和s2 引用地址是否相同：true
s1和s2 值是否相同：true
s1和s3 引用地址是否相同：false
s1和s3 值是否相同：true
```

上面程序中的"=="是判断两个对象引用的地址是否相同，也就是判断是否为同一个对象，s1与s2 返回为true，s1与s3返回则是false。说明s1与s2 引用的同一个对象的地址，s3则与其它两个引用不是同一个对象地址。

Java为了避免产生大量的String对象，设计了一个字符串常量池。工作原理是这样的，创建一个字符串时，JVM首先为检查字符串常量池中是否有值相等的字符串，如果有，则不再创建，直接返回该字符串的引用地址，若没有，则创建，然后放到字符串常量池中，并返回新创建的字符串的引用地址。所以上面s1与s2引用地址相同。

那为什么s3与s1、s2引用的不是同一个字符串地址呢？ String s3=new String("Hello"); JVM首先是在字符串常量池中找"Hello" 字符串，如果没有创建字符串常量，然后放到常量池中，若已存在，则不需要创建；当遇到 new 时，还会在内存（不是字符串常量池中，而是在堆里面）上创建一个新的String对象，存储"Hello"，并将内存上的String对象引用地址返回，所以s3与s1、s2引用的不是同一个字符串地址。 内存结构图如下：

![image-20210320190410685](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320190410.png)

从内存图可见，s1与s2指向的都是常量池中的字符串常量，所以它们比较的是同一块内存地址，而s3指向的是堆里面的一块地址，说的具体点应该是堆里面的Eden区域，s1跟s3，s2跟s3比较都是不相等的，都不是同一块地址。

了解了String类的工作原理，回归问题本身：

在String的工作原理中，已经提到了，new一个String对象，是需要先在字符串常量中查找相同值或创建一个字符串常量，然后再在内存中创建一个String对象，所以 `String str = new String("xyz");` 会创建两个对象。

下面两道Java面试题可以放在留言区回复哟：

```
String str1 = new String("A"+"B") ; 会创建多少个对象?
String str2 = new String("ABC") + "ABC" ; 会创建多少个对象?
```



### X.2.请问StringBuffer和StringBuilder有什么区别？

这是一个老生常谈的话题，笔者前几年每次面试都会被问到，作为基础面试题，被问到的概率百分之八九十。下面我们从面试需要答到的几个知识点来总结一下两者的区别有哪些？

- 继承关系？
- 如何实现的扩容？
- 线程安全性？

**继承关系**

从源码上看看类StringBuffer和StringBuilder的继承结构：

![image-20210320191203863](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320191203.png)

从结构图上可以直到，StringBuffer和StringBuiler都继承自AbstractStringBuilder类

**如何实现扩容**

StringBuffer和StringBuiler的扩容的机制在抽象类AbstractStringBuilder中实现，当发现长度不够的时候(默认长度是16)，会自动进行扩容工作，扩展为原数组长度的2倍加2，创建一个新的数组，并将数组的数据复制到新数组。

```java
public void ensureCapacity(int minimumCapacity) {
    if (minimumCapacity > 0)
        ensureCapacityInternal(minimumCapacity);
}
 
/**
* 确保value字符数组不会越界.重新new一个数组,引用指向value
*/   
private void ensureCapacityInternal(int minimumCapacity) {
    // overflow-conscious code
    if (minimumCapacity - value.length > 0) {
        value = Arrays.copyOf(value,
                newCapacity(minimumCapacity));
    }
}
 
/**
* 扩容:将长度扩展到之前大小的2倍+2
*/   
private int newCapacity(int minCapacity) {
    // overflow-conscious code   扩大2倍+2
    //这里可能会溢出,溢出后是负数哈,注意
    int newCapacity = (value.length << 1) + 2;
    if (newCapacity - minCapacity < 0) {
        newCapacity = minCapacity;
    }
    //MAX_ARRAY_SIZE的值是Integer.MAX_VALUE - 8,先判断一下预期容量(newCapacity)是否在0<x<MAX_ARRAY_SIZE之间,在这区间内就直接将数值返回,不在这区间就去判断一下是否溢出
    return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0)
        ? hugeCapacity(minCapacity)
        : newCapacity;
}
 
/**
* 判断大小，是否溢出
*/
private int hugeCapacity(int minCapacity) {
    if (Integer.MAX_VALUE - minCapacity < 0) { // overflow
        throw new OutOfMemoryError();
    }
    return (minCapacity > MAX_ARRAY_SIZE)
        ? minCapacity : MAX_ARRAY_SIZE;
}
```

**线程安全性**

我们先来看看StringBuffer的相关方法：

```java
@Override
public synchronized StringBuffer append(long lng) {
    toStringCache = null;
    super.append(lng);
    return this;
}
 
/**
 * @throws StringIndexOutOfBoundsException {@inheritDoc}
 * @since      1.2
 */
@Override
public synchronized StringBuffer replace(int start, int end, String str) {
    toStringCache = null;
    super.replace(start, end, str);
    return this;
}
 
/**
 * @throws StringIndexOutOfBoundsException {@inheritDoc}
 * @since      1.2
 */
@Override
public synchronized String substring(int start) {
    return substring(start, count);
}
 
@Override
public synchronized String toString() {
    if (toStringCache == null) {
        toStringCache = Arrays.copyOfRange(value, 0, count);
    }
    return new String(toStringCache, true);
}
```

从上面的源码中我们看到几乎都是所有方法都加了synchronized,几乎都是调用的父类的方法.，用synchronized关键字修饰意味着什么？加锁，资源同步串行化处理，所以是线程安全的。

 

我们再来看看StringBuilder的相关源码：

```java
@Override
public StringBuilder append(double d) {
    super.append(d);
    return this;
}
 
/**
 * @since 1.5
 */
@Override
public StringBuilder appendCodePoint(int codePoint) {
    super.appendCodePoint(codePoint);
    return this;
}
 
/**
 * @throws StringIndexOutOfBoundsException {@inheritDoc}
 */
@Override
public StringBuilder delete(int start, int end) {
    super.delete(start, end);
    return this;
}
```

StringBuilder的源码里面，基本上所有方法都没有用synchronized关键字修饰，当多线程访问时，就会出现线程安全性问题。

为了证明StringBuffer线程安全，StringBuilder线程不安全，我们通过一段代码进行验证：

**测试思想**

- 分别用1000个线程写StringBuffer和StringBuilder，
- 使用CountDownLatch保证在各自1000个线程执行完之后才打印StringBuffer和StringBuilder长度，
- 观察结果。

**测试代码**

```java
import java.util.concurrent.CountDownLatch;
 
public class TestStringBuilderAndStringBuffer {
    public static void main(String[] args) {
        //证明StringBuffer线程安全，StringBuilder线程不安全
        StringBuffer stringBuffer = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        CountDownLatch latch1 = new CountDownLatch(1000);
        CountDownLatch latch2 = new CountDownLatch(1000);
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        stringBuilder.append(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch1.countDown();
                    }
                }
            }).start();
        }
        for (int i = 0; i < 1000; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        stringBuffer.append(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch2.countDown();
                    }
 
                }
            }).start();
        }
        try {
            latch1.await();
            System.out.println(stringBuilder.length());
            latch2.await();
            System.out.println(stringBuffer.length());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

**测试结果**

- StringBuffer不论运行多少次都是1000长度。
- StringBuilder绝大多数情况长度都会小于1000。
- StringBuffer线程安全，StringBuilder线程不安全得到证明。

**总结一下**

- StringBuffer和StringBuilder都继承自抽象类AbstractStringBuilder。
- 存储数据的字符数组也没有被final修饰，说明值可以改变，且构造出来的字符串还有空余位置拼接字符串，但是拼接下去肯定也有不够用的时候，这时候它们内部都提供了一个自动扩容机制，当发现长度不够的时候(默认长度是16)，会自动进行扩容工作，扩展为原数组长度的2倍加2，创建一个新的数组，并将数组的数据复制到新数组，所以对于拼接字符串效率要比String要高。自动扩容机制是在抽象类中实现的。
- 线程安全性：StringBuffer效率低，线程安全，因为StringBuffer中很多方法都被 synchronized 修饰了，多线程访问时，线程安全，但是效率低下，因为它有加锁和释放锁的过程。StringBuilder效率高，但是线程是不安全的。



### X.3.请问 equals() 和 "==" 有什么区别？

**面试官：请问 equals() 和 "==" 有什么区别？**

**应聘者：**

- equals()方法用来比较的是两个对象的内容是否相等，由于所有的类都是继承自java.lang.Object类的，所以适用于所有对象，如果没有对该方法进行覆盖的话，调用的仍然是Object类中的方法，而Object中的equals方法返回的却是==的判断；
- "==" 比较的是变量(栈)内存中存放的对象的(堆)内存地址，用来判断两个对象的地址是否相同，即是否是指相同一个对象。

**1、equals()作用**

equals() 的作用是用来判断两个对象是否相等。 

equals() 定义在JDK的Object.java中。通过判断两个对象的地址是否相等(即，是否是同一个对象)来区分它们是否相等。源码如下：

```
public boolean equals(Object obj) {
    return (this == obj);
}
```

既然Object.java中定义了equals()方法，这就意味着所有的Java类都实现了equals()方法，所有的类都可以通过equals()去比较两个对象是否相等。但是，我们已经说过，使用默认的“equals()”方法，等价于“==”方法。因此，我们通常会重写equals()方法：若两个对象的内容相等，则equals()方法返回true；否则，返回fasle。

下面根据"类是否覆盖equals()方法"，将它分为2类。

- 若某个类没有覆盖equals()方法，当它的通过equals()比较两个对象时，实际上是比较两个对象是不是同一个对象。这时，等价于通过“==”去比较这两个对象。
- 我们可以覆盖类的equals()方法，来让equals()通过其它方式比较两个对象是否相等。通常的做法是：若两个对象的内容相等，则equals()方法返回true；否则，返回fasle。

下面，举例对上面的2种情况进行说明：

**1.1、没有覆盖equals()方法的情况**

```
public class EqualsTest {
    public static void main(String[] args) {
        // 新建2个相同内容的Person对象，
        // 再用equals比较它们是否相等
        User user1 = new User("James", 100);
        User user2 = new User("James", 100);
        System.out.printf("比较结果：" + user1.equals(user2));
    }
 
    /**
     * @desc User类。
     */
    static class User {
        int age;
        String name;
 
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
 
        public String toString() {
            return name + " - " + age;
        }
    }
}
```

运行结果：

```
比较结果：false
```

结果分析：我们通过 user1.equals(user2) 来“比较user1和user2是否相等时”。实际上，调用的Object.java的equals()方法，即调用的 (user1==user2) 。它是比较“p1和p2是否是同一个对象”。而由 user1 和 user2 的定义可知，它们虽然内容相同；但它们是两个不同的对象，因此，返回结果是false。

**1.2、覆盖equals()方法的情况**

修改上面的EqualsTest，覆盖equals()方法：

```
public class EqualsTest {
    public static void main(String[] args) {
        // 新建2个相同内容的Person对象，
        // 再用equals比较它们是否相等
        User user1 = new User("James", 100);
        User user2 = new User("James", 100);
        System.out.printf("比较结果：" + user1.equals(user2));
    }
 
    /**
     * @desc User类。
     */
    static class User {
        int age;
        String name;
 
        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }
 
        public String toString() {
            return name + " - " + age;
        }
 
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            User other = (User) obj;
            if (age != other.age)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
    }
}
```

运行结果：

```
比较结果：true
```

结果分析：我们在EqualsTest.java 中重写了User的equals()函数：当两个User对象的 name 和 age 都相等，则返回true。因此，运行结果返回true。

**2、== 的作用**

“==”：它的作用是判断两个对象的地址是不是相等。即判断引用对象是不是指向的堆中的同一个对象，我们知道，凡是new出来的对象都在堆中。而对象的引用都存放在栈中，具体来讲就是放在栈帧中，我们来看下面一段代码：

```
public static void main(String[] args) {
        User user1 = new User("James", 100);
        User user2 = new User("James", 100);
        System.out.println("user1.equals(user2)：" + user1.equals(user2));
        System.out.println("user1==user2：" + (user1==user2));
}
```

输出结果：

```
user1.equals(user2)：true
user1==user2：false
```

用内存图表示如下：

![image-20210320194000052](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320194000.png)

指向的是堆中两块不同的区域，所以用 "==" 比较时返回的是false。



### X.4.String为什么是不可变的？

**什么是不可变对象？**

如果一个对象，在它创建完成之后，不能再改变它的状态，那么这个对象就是不可变的。不能改变状态的意思是，不能改变对象内的成员变量，包括基本数据类型的值不能改变，引用类型的变量不能指向其他的对象，引用类型指向的对象的状态也不能改变。

我们来看下面一段代码：

```java
public class Demo {
    String str = "ABC";
    System.out.println("s = " + str);

    str = "123";
    System.out.println("s = " + str);
}
```

打印结果为：

```java
s = ABC
s = 123
```

对于上述代码，我们简单的分析一下：首先创建一个String对象str，然后让str的值为“ABC”，然后又让str的值为“123”。从打印结果可以看出，str的值确实改变了。

**那还说String对象是不可变的呢？** 

这里存在一个误区：str只是一个String对象的引用，并不是对象本身。对象在内存中是一块内存区，放在堆中，成员变量越多，这块内存区占的空间越大。引用只是一个4字节的数据，里面存放了它所指向的对象的地址，通过这个地址可以访问对象，而这个引用存放在Java虚拟机栈栈帧的局部变量表中。也就是说，str只是一个引用，它指向了一个具体的对象，当str=“123”; 这句代码执行过之后，又创建了一个新的对象“123”， 而引用str重新指向了这个新的对象，原来的对象“ABC”还在内存中存在，并没有改变。

我们用一张内存结构图来看看整个变化过程：

![image-20210320200345715](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320200345.png)

其实上面的"ABC","123"是字符串常量，按照JVM规范应该是存放在方法区的常量池里面。但是Java1.7之后HotSpot虚拟机并没有区分方法区和堆，所以，这里统一就当做是放在堆里面的吧。

**String源码构成**

```java
public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence {
    /** The value is used for character storage. */
    private final char value[];

    /** Cache the hash code for the string */
    private int hash; // Default to 0
```

通过源码我们可以知道String底层是由char数组构成，我们创建一个字符串对象的时候，其实是将字符串保存在char数组中，因为数组是引用对象，为了防止数组可变，JDK加了final修饰，但是加了final修饰的数组只是代表了引用不可变，不代表数组内容不可变，因此JDK为了真正防止不可变，又加了private修饰符。

**String对象是真的不可变吗？**

从上文可知String的成员变量是private final 的，也就是初始化之后不可改变。那么在这几个成员中，value比较特殊，因为他是一个引用变量，而不是真正的对象。value是final修饰的，也就是说final不能再指向其他数组对象，那么我能改变value指向的数组吗？我们来看下面的代码：

```java
final int[] value={1,2,3}
int[] another={4,5,6};
value=another;    //编译器报错，final不可变
```

value用final修饰，编译器不允许我把value指向堆区另一个地址。但如果我直接对数组元素动手，分分钟搞定

```java
final int[] value={1,2,3};
value[2]=100;  //这时候数组里已经是{1,2,100}
```

所以String是不可变，关键是因为设计源代码的工程师，在后面所有String的方法里很小心的没有去动Array里的元素，没有暴露内部成员字段。`private final char value[]` 这一句里，private的私有访问权限的作用都比final大。而且设计师还很小心地把整个String设成final禁止继承，避免被其他人继承后破坏。**所以String是不可变的关键都在底层的实现，而不是一个final。**

**不可变有什么好处？**

**1、多线程下安全性**

最简单地原因，就是为了安全。因为String是不可变的，因此多线程操作下，它是安全的，我们来看下面一段代码：

```java
public String get(String str){
    str += "aaa";
    return str;
}
```

试想一下，如果String是可变的，那么get方法内部改变了str的值，方法外部str也会随之改变。

**2、类加载中体现的安全性**

类加载器要用到字符串，不可变提供了安全性，以便正确的类被加载，例如你想加载java.sql.Connection类，而这个值被改成了xxx.Connection，那么会对你的数据库造成不可知的破坏。

**3、使用常量池可以节省空间**

像下面这样字符串one和two都用字面量"something"赋值。它们其实都指向同一个内存地址

```java
String one = "someString";
String two = "someString";
```

这样在大量使用字符串的情况下，可以节省内存空间，提高效率。但之所以能实现这个特性，String的不可变性是最基本的一个必要条件。要是内存里字符串内容能改来改去，这么做就完全没有意义了。



### X.5.String拼接字符串效率低，你知道原因吗？

**面试官Q1：请问为什么String用"+"拼接字符串效率低下，最好能从JVM角度谈谈吗？**

对于这个问题，我们先来看看如下代码：

```java
public class StringTest {
     public static void main(String[] args) {
         String a = "abc";
         String b = "def";
         String c = a + b;
         String d = "abc" + "def";
         System.out.Println(c);
         System.out.Println(d);
     }
}
```

打印结果：

```
abcdef
abcdef
```

从上面代码示例中，我们看到两种方式拼接的字符串打印的结果是一样的。但这只是表面上的，实际内部运行不一样。

**两者究竟有什么不一样？**

为了看到两者的不同，对代码做如下调整：

```java
public class StringTest {
     public static void main(String[] args) {
         String a = "abc";
         String b = "def";
         String c = a + b;
         System.out.Println(c);
    }
}
```

我们看看编译完成后它是什么样子：

```
C:\Users\GRACE\Documents>javac StringTest.java
C:\Users\GRACE\Documents>javap -verbose StringTest
Classfile /C:/Users/GRACE/Documents/StringTest.class
  Last modified 2018-7-21; size 607 bytes
  MD5 checksum a2729f11e22d7e1153a209e5ac968b98
  Compiled from "StringTest.java"
public class StringTest
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #11.#20        // java/lang/Object."<init>":()V
   #2 = String             #21            // abc
   #3 = String             #22            // def
   #4 = Class              #23            // java/lang/StringBuilder
   #5 = Methodref          #4.#20         // java/lang/StringBuilder."<init>":()V
   #6 = Methodref          #4.#24         // java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
   #7 = Methodref          #4.#25         // java/lang/StringBuilder.toString:()Ljava/lang/String;
   #8 = Fieldref           #26.#27        // java/lang/System.out:Ljava/io/PrintStream;
   #9 = Methodref          #28.#29        // java/io/PrintStream.println:(Ljava/lang/String;)V
  #10 = Class              #30            // StringTest
  #11 = Class              #31            // java/lang/Object
  #12 = Utf8               <init>
  #13 = Utf8               ()V
  #14 = Utf8               Code
  #15 = Utf8               LineNumberTable
  #16 = Utf8               main
  #17 = Utf8               ([Ljava/lang/String;)V
  #18 = Utf8               SourceFile
  #19 = Utf8               StringTest.java
  #20 = NameAndType        #12:#13        // "<init>":()V
  #21 = Utf8               abc
  #22 = Utf8               def
  #23 = Utf8               java/lang/StringBuilder
  #24 = NameAndType        #32:#33        // append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
  #25 = NameAndType        #34:#35        // toString:()Ljava/lang/String;
  #26 = Class              #36            // java/lang/System
  #27 = NameAndType        #37:#38        // out:Ljava/io/PrintStream;
  #28 = Class              #39            // java/io/PrintStream
  #29 = NameAndType        #40:#41        // println:(Ljava/lang/String;)V
  #30 = Utf8               StringTest
  #31 = Utf8               java/lang/Object
  #32 = Utf8               append
  #33 = Utf8               (Ljava/lang/String;)Ljava/lang/StringBuilder;
  #34 = Utf8               toString
  #35 = Utf8               ()Ljava/lang/String;
  #36 = Utf8               java/lang/System
  #37 = Utf8               out
  #38 = Utf8               Ljava/io/PrintStream;
  #39 = Utf8               java/io/PrintStream
  #40 = Utf8               println
  #41 = Utf8               (Ljava/lang/String;)V
{
  public StringTest();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=4, args_size=1
         0: ldc           #2                  // String abc
         2: astore_1
         3: ldc           #3                  // String def
         5: astore_2
         6: new           #4                  // class java/lang/StringBuilder
         9: dup
        10: invokespecial #5                  // Method java/lang/StringBuilder."<init>":()V
        13: aload_1
        14: invokevirtual #6                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        17: aload_2
        18: invokevirtual #6                  // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
        21: invokevirtual #7                  // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
        24: astore_3
        25: getstatic     #8                  // Field java/lang/System.out:Ljava/io/PrintStream;
        28: aload_3
        29: invokevirtual #9                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        32: return
      LineNumberTable:
        line 3: 0
        line 4: 3
        line 5: 6
        line 6: 25
        line 7: 32
}
SourceFile: "StringTest.java"
```

首先看到使用了一个指针指向一个常量池中的对象内容为“abc”，而另一个指针指向“def”，此时通过new申请了一个StringBuilder，然后调用这个StringBuilder的初始化方法；然后分别做了两次append操作，然后最后做一个toString()操作；可见String的+在编译后会被编译为StringBuilder来运行，我们知道这里做了一个new StringBuilder的操作，并且做了一个toString的操作，如果你对JVM有所了解，凡是new出来的对象绝对不会放在常量池中，toString会发生一次内容拷贝，但是也不会在常量池中，所以在这里常量池String+常量池String放在了堆中。

**我们再来看看另外一种情况，用同样的方式来看看结果是什么：**

代码如下：

```java
public class StringTest {
     public static void main(String[] args) {
         String c = "abc" + "def";
         System.out.println(c);
    }
}
```

我们也来看看它编译完成后是什么样子：

```
C:\Users\GRACE\Documents>javac StringTest.java

C:\Users\GRACE\Documents>javap -verbose StringTest
Classfile /C:/Users/GRACE/Documents/StringTest.class
  Last modified 2018-7-21; size 426 bytes
  MD5 checksum c659d48ff8aeb45a3338dea5d129f593
  Compiled from "StringTest.java"
public class StringTest
  minor version: 0
  major version: 52
  flags: ACC_PUBLIC, ACC_SUPER
Constant pool:
   #1 = Methodref          #6.#15         // java/lang/Object."<init>":()V
   #2 = String             #16            // abcdef
   #3 = Fieldref           #17.#18        // java/lang/System.out:Ljava/io/PrintStream;
   #4 = Methodref          #19.#20        // java/io/PrintStream.println:(Ljava/lang/String;)V
   #5 = Class              #21            // StringTest
   #6 = Class              #22            // java/lang/Object
   #7 = Utf8               <init>
   #8 = Utf8               ()V
   #9 = Utf8               Code
  #10 = Utf8               LineNumberTable
  #11 = Utf8               main
  #12 = Utf8               ([Ljava/lang/String;)V
  #13 = Utf8               SourceFile
  #14 = Utf8               StringTest.java
  #15 = NameAndType        #7:#8          // "<init>":()V
  #16 = Utf8               abcdef
  #17 = Class              #23            // java/lang/System
  #18 = NameAndType        #24:#25        // out:Ljava/io/PrintStream;
  #19 = Class              #26            // java/io/PrintStream
  #20 = NameAndType        #27:#28        // println:(Ljava/lang/String;)V
  #21 = Utf8               StringTest
  #22 = Utf8               java/lang/Object
  #23 = Utf8               java/lang/System
  #24 = Utf8               out
  #25 = Utf8               Ljava/io/PrintStream;
  #26 = Utf8               java/io/PrintStream
  #27 = Utf8               println
  #28 = Utf8               (Ljava/lang/String;)V
{
  public StringTest();
    descriptor: ()V
    flags: ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 1: 0

  public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=2, args_size=1
         0: ldc           #2                  // String abcdef
         2: astore_1
         3: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
         6: aload_1
         7: invokevirtual #4                  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
        10: return
      LineNumberTable:
        line 3: 0
        line 4: 3
        line 5: 10
}
SourceFile: "StringTest.java"
```

这一次编译完后的代码比前面少了很多，而且，仔细看，你会发现14行处，编译的过程中直接变成了"abcdef"，这是为什么呢？因为当发生“abc” + “def”在同一行发生时，JVM在编译时就认为这个加号是没有用处的，编译的时候就直接变成

```
String d = "abcdef";
```

同理如果出现：String a =“a” + 1，编译时候就会变成：String a = “a1″;

再补充一个例子：

```
final String a = "a";
final String b = "ab";
String c = a + b;
```

在编译时候，c部分会被编译为：String c = “aab”;但是如果a或b有任意一个不是final的，都会new一个新的对象出来；其次再补充下，如果a和b，是某个方法返回回来的，不论方法中是final类型的还是常量什么的，都不会被在编译时将数据编译到常量池，因为编译器并不会跟踪到方法体里面去看你做了什么，其次**只要是变量就是可变的，即使你认为你看到的代码是不可变的**，但是运行时是可以被切入的。

**那么效率问题从何说起？**

那说了这么多，也没看到有说效率方面的问题呀？

其实上面两个例子，连接字符串行表达式很简单，那么"+"和StringBuilder基本是一样的，但如果结构比较复杂，如使用循环来连接字符串，那么产生的Java Byte Code就会有很大的区别。我们再来看看下面一段代码：

```java
import java.util.*;
public class StringTest {
     public static void main(String[] args){
          String s = "";
          Random rand = new Random();
          for (int i = 0; i < 10; i++){
              s = s + rand.nextInt(1000) + " ";
          }
          System.out.println(s);
      }
}
```

上面代码反编译后的结果如下：

```
C:\Java\jdk1.8.0_171\bin>javap -c E:\StringTest.class
Picked up _JAVA_OPTIONS: -Xmx512M
Compiled from "StringTest.java"
public class StringTest {
  public StringTest();
    Code:
       0: aload_0
       1: invokespecial #8                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       //String s = "";
       0: ldc           #16                 // String
       2: astore_1
       //Random rand = new Random();
       3: new           #18                 // class java/util/Random
       6: dup
       7: invokespecial #20                 // Method java/util/Random."<init>":()V
      10: astore_2
      //StringBuilder result = new StringBuilder();
      11: iconst_0
      12: istore_3
      13: goto          49
      //s = (new StringBuilder(String.valueOf(s))).append(rand.nextInt(1000)).append(" ").toString();
      16: new           #21                 // class java/lang/StringBuilder
      19: dup
      20: aload_1
      21: invokestatic  #23                 // Method java/lang/String.valueOf:(Ljava/lang/Object;)Ljava/lang/String;
      24: invokespecial #29                 // Method java/lang/StringBuilder."<init>":(Ljava/lang/String;)V
      27: aload_2
      28: sipush        1000
      31: invokevirtual #32                 // Method java/util/Random.nextInt:(I)I
      34: invokevirtual #36                 // Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;
      37: ldc           #40                 // String
      39: invokevirtual #42                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      42: invokevirtual #45                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
      45: astore_1
      46: iinc          3, 1
      49: iload_3
      50: bipush        10
      52: if_icmplt     16
      //System.out.println(s);
      55: getstatic     #49                 // Field java/lang/System.out:Ljava/io/PrintStream;
      58: aload_1
      59: invokevirtual #55                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      62: return
}
```

我们可以看到，虽然编译器将"+"转换成了StringBuilder，但创建StringBuilder对象的位置却在for语句内部。这就意味着每执行一次循环，就会创建一个StringBuilder对象（对于本例来说，是创建了10个StringBuilder对象），虽然Java有垃圾回收器，但这个回收器的工作时间是不定的。如果不断产生这样的垃圾，那么仍然会占用大量的资源。解决这个问题的方法就是在程序中直接使用StringBuilder来连接字符串，代码如下：

```java
import java.util.Random;
public class StringTest {
    public static void main(String[] args) {
        Random rand = new Random();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            result.append(rand.nextInt(1000));
            result.append(" ");
        }
        System.out.println(result.toString());
    }
}
```

上面代码反编译后的结果如下：

```
C:\Java\jdk1.8.0_171\bin>javap -c E:\Dubbo\Demo\bin\StringTest.class
Picked up _JAVA_OPTIONS: -Xmx512M
Compiled from "StringTest.java"
public class StringTest {
  public StringTest();
    Code:
       0: aload_0
       1: invokespecial #8                  // Method java/lang/Object."<init>":()V
       4: return

  public static void main(java.lang.String[]);
    Code:
       //Random rand = new Random();
       0: new           #16                 // class java/util/Random
       3: dup
       4: invokespecial #18                 // Method java/util/Random."<init>":()V
       7: astore_1
       //StringBuilder result = new StringBuilder();
       8: new           #19                 // class java/lang/StringBuilder
      11: dup
      12: invokespecial #21                 // Method java/lang/StringBuilder."<init>":()V
      15: astore_2
      //for(int i = 0; i < 10; i++)
      16: iconst_0
      17: istore_3
      18: goto          43
      //result.append(rand.nextInt(1000));
      21: aload_2
      22: aload_1
      23: sipush        1000
      26: invokevirtual #22                 // Method java/util/Random.nextInt:(I)I
      29: invokevirtual #26                 // Method java/lang/StringBuilder.append:(I)Ljava/lang/StringBuilder;
      32: pop
      //result.append(" ");
      33: aload_2
      34: ldc           #30                 // String
      36: invokevirtual #32                 // Method java/lang/StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
      39: pop
      40: iinc          3, 1
      43: iload_3
      44: bipush        10
      46: if_icmplt     21
      //System.out.println(result.toString());
      49: getstatic     #35                 // Field java/lang/System.out:Ljava/io/PrintStream;
      52: aload_2
      53: invokevirtual #41                 // Method java/lang/StringBuilder.toString:()Ljava/lang/String;
      56: invokevirtual #45                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      59: return
}
```

从上面的反编译结果可以看出，创建StringBuilder的代码被放在了for语句外。虽然这样处理在源程序中看起来复杂，但却换来了更高的效率，同时消耗的资源也更少了。

所以，从上述几个例子中我们得出的结论是：String采用连接运算符(+)效率低下，都是上述循环、大批量数据情况造成的，每做一次"+"就产生个StringBuilder对象，然后append后就扔掉。下次循环再到达时重新产生个StringBuilder对象，然后append字符串，如此循环直至结束。如果我们直接采用StringBuilder对象进行append的话，我们可以节省创建和销毁对象的时间。如果只是简单的字面量拼接或者很少的字符串拼接，性能都是差不多的。



### X.6.你真的了解String的常见API吗？

String是我们开发中使用频率最高的类，它有哪些方法，大家一定不会陌生，例如：

- `length();`//计算字符串的长度
- `charAt();`//截取一个字符
- `getChars();`//截取多个字符
- `equals();`//比较两个字符串
- `equalsIgnoreCase();`//比较两个字符串,忽略大小写
- `startsWith();`//startsWith()方法决定是否以特定字符串开始
- `endWith();`//方法决定是否以特定字符串结束
- `indexOf();`//查找字符或者子串第一次出现的地方。
- `lastIndexOf();`//查找字符或者子串是后一次出现的地方。
- `substring();`//截取字符串
- `concat();`//连接两个字符串
- `replace();`//替换
- `trim();`//去掉起始和结尾的空格
- `valueOf();`//转换为字符串
- `toLowerCase();`//转换为小写
- `toUpperCase();`// 转换为大写

但是像replace()，substring()，toLowerCase()这三个方法需要注意一下，我们看下下面一段代码：

```
import java.util.*;
public class StringTest {
     public static void main(String[] args){
      String ss = "123456";
      System.out.println("ss = " + ss);
      ss.replace('1', '0');
      System.out.println("ss = " + ss);
	}
}
```

打印结果：

```
ss = 123456
ss = 123456
```

如果你不了解replace方法的源码，可能会认为最后的打印结果为 "ss = 023456"，但是实际上方法内部创建了一个新的String对象，并将这个新的String对象返回。对ss是没有做任何操作的，我们也知道String是不可变的嘛。源码如下：

```
public String replace(char oldChar, char newChar) {
    // 判断替换字符和被替换字符是否相同
    if (oldChar != newChar) {
        int len = value.length;
        int i = -1;
        // 将源字符串转换为字符数组
        char[] val = value; /* avoid getfield opcode */
        while (++i < len) {
            // 判断第一次被替换字符串出现的位置
            if (val[i] == oldChar) {
                break;
            }
        }
        // 从出现被替换字符位置没有大于源字符串长度
        if (i < len) {
            char buf[] = new char[len];
            for (int j = 0; j < i; j++) {
                // 将源字符串，从出现被替换字符位置前的字符将其存放到字符串数组中
                buf[j] = val[j];
            }
            while (i < len) {
                char c = val[i];
                // 开始进行比较；如果相同的字符串替换，如果不相同按原字符串
                buf[i] = (c == oldChar) ? newChar : c;
                i++;
            }
            // 使用String的构造方法进行重新创建String
            return new String(buf, true);
        }
    }
    return this;
}
```

方法内部最后重新创建新的String对象，并且返回这个新的对象，原来的对象是不会被改变的。substring()，toLowerCase()方法也是如此。

还有诸如contact()方法，源码如下：

```
public String concat(String str) {
    int otherLen = str.length();
    if (otherLen == 0) {
        return this;
    }
    int len = value.length;
    char buf[] = Arrays.copyOf(value, len + otherLen);
    str.getChars(buf, len);
    return new String(buf, true);
}
```

从上可知参数str不能为null，否则就会包空指针异常。用contact()拼接字符串速度也很快，因为直接Arrays.copyOf，直接内存复制。



### X.7.Java中的substring真的会引起内存泄露么？

在Java中开发，String是我们开发程序可以说必须要使用的类型，String有一个substring方法用来截取字符串，我们想必也常常使用。但是你知道么，关于Java 6中的substring是否会引起内存泄露，在国外的论坛和社区有着一些讨论，以至于Java官方已经将其标记成bug，并且为此Java 7 还重新进行了实现。读到这里可能你的问题就来了，substring怎么会引起内存泄露呢？那么我们就带着问题，走进小黑屋，看看substring有没有内存泄露，又是怎么导致所谓的内存泄露。

**基本介绍**

substring方法提供两种重载，第一种为只接受开始截取位置一个参数的方法

```
public String substring(int beginIndex)
```

比如我们使用上面的方法

```
public String substring(int beginIndex, int endIndex)
```

使用这个方法，"smiles".substring(1, 5) 返回结果 "mile"，"unhappy".substring(2) 返回结果 "happy" 另一种重载就是接受一个开始截取位置和一个结束截取位置的参数的方法

通过这个介绍我们基本了解了substring的作用，这样便于我们理解下面的内容。

**准备工作**

因为这个问题出现的情况在Java 6，如果你的Java版本号不是Java 6 需要调整一下。

**终端调整（适用于Mac系统）**

查看java版本号

```
13:03 $ java -version
java version "1.8.0_25"
Java(TM) SE Runtime Environment (build 1.8.0_25-b17)
Java HotSpot(TM) 64-Bit Server VM (build 25.25-b02, mixed mode)
```

切换到1.6

```
export JAVA_HOME=$(/usr/libexec/java_home -v 1.6)
```

Ubuntu使用alternatives --config java，Fedora上面使用alternatives --config java。

如果你使用Eclipse，可以选择工程，右击，选择Properties（属性）— Java Compiler（Java编译器）进行特殊指定。

**问题重现**

这里贴一下java官方bug里用到的重现问题的代码。

```
public class TestGC {
    private String largeString = new String(new byte[100000]);

    String getString() {
        return this.largeString.substring(0,2);
    }

    public static void main(String[] args) {
        java.util.ArrayList list = new java.util.ArrayList();
        for (int i = 0; i < 1000000; i++) {
            TestGC gc = new TestGC();
            list.add(gc.getString());
        }
    }
}
```

执行上面的方法，并不会导致OOM异常，因为我们持有的时1000000个ab字符串对象，而TestGC对象（包括其中的largeString）会在java的垃圾回收中释放掉。所以这里不会存在内存溢出。

那么究竟是什么导致的内存泄露呢？要研究这个问题，我们需要看一下方法的实现，即可。

**深入Java 6实现**

在String类中存在这样三个属性

- value 字符数组，存储字符串实际的内容
- offset 该字符串在字符数组value中的起始位置
- count 字符串包含的字符的长度

Java6中substring的实现

```
public String substring(int beginIndex, int endIndex) {
  if (beginIndex < 0) {
      throw new StringIndexOutOfBoundsException(beginIndex);
  }
  if (endIndex > count) {
      throw new StringIndexOutOfBoundsException(endIndex);
  }
  if (beginIndex > endIndex) {
      throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
  }
  return ((beginIndex == 0) && (endIndex == count)) ? this :
      new String(offset + beginIndex, endIndex - beginIndex, value);
}
```

上述方法调用的构造方法

```
//Package private constructor which shares value array for speed.
String(int offset, int count, char value[]) {
  this.value = value;
  this.offset = offset;
  this.count = count;
}
```

当我们读完上述的代码，我们应该会豁然开朗，原来是这个样子啊！

当我们调用字符串a的substring得到字符串b，其实这个操作，无非就是调整了一下b的offset和count，用到的内容还是a之前的value字符数组，并没有重新创建新的专属于b的内容字符数组。

举个和上面重现代码相关的例子，比如我们有一个1G的字符串a，我们使用substring(0,2)得到了一个只有两个字符的字符串b，如果b的生命周期要长于a或者手动设置a为null，当垃圾回收进行后，a被回收掉，b没有回收掉，那么这1G的内存占用依旧存在，因为b持有这1G大小的字符数组的引用。

看到这里，大家应该可以明白上面的代码为什么出现内存溢出了。

**共享内容字符数组**

其实substring中生成的字符串与原字符串共享内容数组是一个很棒的设计，这样避免了每次进行substring重新进行字符数组复制。正如其文档说明的,共享内容字符数组为了就是速度。但是对于本例中的问题，共享内容字符数组显得有点蹩脚。

**如何解决**

对于之前比较不常见的1G字符串只截取2个字符的情况可以使用下面的代码，这样的话，就不会持有1G字符串的内容数组引用了。

```
String littleString = new String(largeString.substring(0,2));
```

下面的这个构造方法，在源字符串内容数组长度大于字符串长度时，进行数组复制，新的字符串会创建一个只包含源字符串内容的字符数组。

```
public String(String original) {
  int size = original.count;
  char[] originalValue = original.value;
  char[] v;
  if (originalValue.length > size) {
      // The array representing the String is bigger than the new
      // String itself.  Perhaps this constructor is being called
      // in order to trim the baggage, so make a copy of the array.
      int off = original.offset;
      v = Arrays.copyOfRange(originalValue, off, off+size);
  } else {
      // The array representing the String is the same
      // size as the String, so no point in making a copy.
      v = originalValue;
  }
  this.offset = 0;
  this.count = size;
  this.value = v;
}
```

**Java7 实现**

在Java 7 中substring的实现抛弃了之前的内容字符数组共享的机制，对于子字符串（自身除外）采用了数组复制实现单个字符串持有自己的应该拥有的内容。

```
public String substring(int beginIndex, int endIndex) {
    if (beginIndex < 0) {
      throw new StringIndexOutOfBoundsException(beginIndex);
    }
    if (endIndex > value.length) {
      throw new StringIndexOutOfBoundsException(endIndex);
    }
    int subLen = endIndex - beginIndex;
    if (subLen < 0) {
      throw new StringIndexOutOfBoundsException(subLen);
    }
    return ((beginIndex == 0) && (endIndex == value.length)) ? this
                : new String(value, beginIndex, subLen);
}
```

substring方法中调用的构造方法，进行内容字符数组复制

```
public String(char value[], int offset, int count) {
    if (offset < 0) {
          throw new StringIndexOutOfBoundsException(offset);
    }
    if (count < 0) {
      throw new StringIndexOutOfBoundsException(count);
    }
    // Note: offset or count might be near -1>>>1.
    if (offset > value.length - count) {
      throw new StringIndexOutOfBoundsException(offset + count);
    }
    this.value = Arrays.copyOfRange(value, offset, offset+count);
}
```

**真的是内存泄露么?**

我们知道了substring某些情况下可能引起内存问题，但是这个叫做内存泄露么？

其实个人认为这个不应该算为内存泄露，使用substring生成的字符串b固然会持有原有字符串a的内容数组引用，但是当a和b都被回收之后，该字符数组的内容也是可以被垃圾回收掉的。

**哪个版本实现的好?**

关于Java7对substring做的修改，收到了褒贬不一的反馈。

个人更加倾向于Java 6的实现，当进行substring时，使用共享内容字符数组，速度会更快，不用重新申请内存。虽然有可能出现本文中的内存性能问题，但也是有方法可以解决的。

Java 7的实现不需要程序员特殊操作避免了本文中问题，但是进行每次substring的操作性能总会比java 6 的实现要差一些。这种实现显得有点“糟糕”。

**问题的价值**

虽然这个问题出现在Java 6并且Java 7中已经修复，但并不代表我们就不需要了解，况且Java 7的重新实现被喷的很厉害。

其实这个问题的价值，还是比较宝贵的，尤其是内容字符数组共享这个优化的实现。希望可以为大家以后的设计实现提供帮助和一些想法。

**受影响的方法**

trim和subSequence都存在调用substring的操作。Java 6和Java 7 substring实现的更改也间接影响到了这些方法



### X.8.你真的了解String类的intern()方法吗

#### X.8.0.引言

什么都先不说，先看下面这个引入的例子：

```
String str1 = new String("SEU")+ new String("Calvin");    
System.out.println(str1.intern() == str1); 
System.out.println(str1 == "SEUCalvin");
```

本人JDK版本1.8，输出结果为：

```
true
true
```

再将上面的例子加上一行代码：

```
String str2 = "SEUCalvin";//新加的一行代码，其余不变
String str1 = new String("SEU")+ new String("Calvin");    
System.out.println(str1.intern() == str1); 
System.out.println(str1 == "SEUCalvin"); 
```

再运行，结果为：

```
false
false
```

是不是感觉莫名其妙，新定义的str2好像和str1没有半毛钱的关系，怎么会影响到有关str1的输出结果呢？其实这都是intern()方法搞的鬼！看完这篇文章，你就会明白。o(∩_∩)o 

#### X.8.1.为什么要介绍intern()方法

intern()方法设计的初衷，就是重用String对象，以节省内存消耗。这么说可能有点抽象，那么就用例子来证明。

```
static final int MAX = 100000;

static final String[] arr = new String[MAX];

public static void main(String[] args) throws Exception {
	//为长度为10的Integer数组随机赋值
	Integer[] sample = new Integer[10];

	Random random = new Random(1000);

	for (int i = 0; i < sample.length; i++) {
	    sample[i] = random.nextInt();
	}
	//记录程序开始时间
	long t = System.currentTimeMillis();
	//使用/不使用intern方法为10万个String赋值，值来自于Integer数组的10个数
    for (int i = 0; i < MAX; i++) {
        arr[i] = new String(String.valueOf(sample[i % sample.length]));
        //arr[i] = new String(String.valueOf(sample[i % sample.length])).intern();
    }
    System.out.println((System.currentTimeMillis() - t) + "ms");
    System.gc();
}
```


这个例子也比较简单，就是为了证明使用intern()比不使用intern()消耗的内存更少。

先定义一个长度为10的Integer数组，并随机为其赋值，在通过for循环为长度为10万的String对象依次赋值，这些值都来自于Integer数组。两种情况分别运行，可通过 `Window ---> Preferences --> Java --> Installed JREs` 设置JVM启动参数为 `-agentlib:hprof=heap=dump,format=b` ，将程序运行完后的hprof置于工程目录下。再通过MAT插件查看该hprof文件。

两次实验结果如下：

![20160823152027600](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320215000.jpg)

![20160823152041988](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320215005.jpg)

从运行结果来看，不使用 `intern()` 的情况下，程序生成了101762个String对象，而使用了 `intern()` 方法时，程序仅生成了1772个String对象。自然也证明了 `intern()` 节省内存的结论。

细心的同学会发现使用了 `intern()` 方法后程序运行时间有所增加。这是因为程序中每次都是用了new String后又进行 `intern()` 操作的耗时时间，但是不使用 `intern()` 占用内存空间导致GC的时间是要远远大于这点时间的。 

#### X.8.2.深入认识intern()方法

JDK1.7后，常量池被放入到堆空间中，这导致 `intern()` 函数的功能不同，具体怎么个不同法，且看看下面代码，这个例子是网上流传较广的一个例子，分析图也是直接粘贴过来的，这里我会用自己的理解去解释这个例子：

```
String s = new String("1");
s.intern();
String s2 = "1";
System.out.println(s == s2);

String s3 = new String("1") + new String("1");
s3.intern();
String s4 = "11";
System.out.println(s3 == s4);
```

输出结果为：

```
JDK1.6以及以下：false false
JDK1.7以及以上：false true
```

再分别调整上面代码2.3行、7.8行的顺序：

```
String s = new String("1");
String s2 = "1";
s.intern();
System.out.println(s == s2);

String s3 = new String("1") + new String("1");
String s4 = "11";
s3.intern();
System.out.println(s3 == s4);
```

输出结果为：

```
JDK1.6以及以下：false false
JDK1.7以及以上：false false
```


下面依据上面代码对intern()方法进行分析：

**X.8.2.1 JDK1.6**

![image-20210320215551906](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320215552.png)

在JDK1.6中所有的输出结果都是 false，因为JDK1.6以及以前版本中，常量池是放在 Perm 区（属于方法区）中的，熟悉JVM的话应该知道这是和堆区完全分开的。

使用引号声明的字符串都是会直接在字符串常量池中生成的，而 new 出来的 String 对象是放在堆空间中的。所以两者的内存地址肯定是不相同的，即使调用了intern()方法也是不影响的。如果不清楚String类的“==”和equals()的区别可以查看我的这篇博文Java面试——从Java堆、栈角度比较equals和==的区别。

intern()方法在JDK1.6中的作用是：比如`String s = new String("SEU_Calvin")`，再调用`s.intern()`，此时返回值还是字符串"SEU_Calvin"，表面上看起来好像这个方法没什么用处。但实际上，在JDK1.6中它做了个小动作：检查字符串池里是否存在"SEU_Calvin"这么一个字符串，如果存在，就返回池里的字符串；如果不存在，该方法会把"SEU_Calvin"添加到字符串池中，然后再返回它的引用。然而在JDK1.7中却不是这样的，后面会讨论。

**X.8.2.2 JDK1.7**

针对JDK1.7以及以上的版本，我们将上面两段代码分开讨论。先看第一段代码的情况：

![image-20210320215624841](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320215624.png)

再把第一段代码贴一下便于查看：

```
String s = new String("1");
s.intern();
String s2 = "1";
System.out.println(s == s2);

String s3 = new String("1") + new String("1");
s3.intern();
String s4 = "11";
System.out.println(s3 == s4);
```

`String s = newString("1")`，生成了常量池中的“1” 和堆空间中的字符串对象。

`s.intern()`，这一行的作用是s对象去常量池中寻找后发现"1"已经存在于常量池中了。

`String s2 = "1"`，这行代码是生成一个s2的引用指向常量池中的“1”对象。

结果就是 s 和 s2 的引用地址明显不同。因此返回了false。

`String s3 = new String("1") + newString("1")`，这行代码在字符串常量池中生成“1” ，并在堆空间中生成s3引用指向的对象（内容为"11"）。注意此时常量池中是没有 “11”对象的。

`s3.intern()`，这一行代码，是将 s3中的“11”字符串放入 String 常量池中，此时常量池中不存在“11”字符串，JDK1.6的做法是直接在常量池中生成一个 "11" 的对象。

但是在JDK1.7中，常量池中不需要再存储一份对象了，可以直接存储堆中的引用。这份引用直接指向 s3 引用的对象，也就是说`s3.intern() ==s3`会返回true。

String s4 = "11"， 这一行代码会直接去常量池中创建，但是发现已经有这个对象了，此时也就是指向 s3 引用对象的一个引用。因此`s3 == s4`返回了true。

下面继续分析第二段代码：

![image-20210320215805637](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320215805.png)

再把第二段代码贴一下便于查看：

```
String s = new String("1");
String s2 = "1";
s.intern();
System.out.println(s == s2);

String s3 = new String("1") + new String("1");
String s4 = "11";
s3.intern();
System.out.println(s3 == s4);
```

`String s = newString("1")`，生成了常量池中的“1” 和堆空间中的字符串对象。

`String s2 = "1"`，这行代码是生成一个s2的引用指向常量池中的“1”对象，但是发现已经存在了，那么就直接指向了它。

`s.intern()`，这一行在这里就没什么实际作用了。因为"1"已经存在了。

结果就是 s 和 s2 的引用地址明显不同。因此返回了false。



`String s3 = new String("1") + newString("1")`，这行代码在字符串常量池中生成“1” ，并在堆空间中生成s3引用指向的对象（内容为"11"）。注意此时常量池中是没有 “11”对象的。

`String s4 = "11"`， 这一行代码会直接去生成常量池中的"11"。

`s3.intern()`，这一行在这里就没什么实际作用了。因为"11"已经存在了。

结果就是 s3 和 s4 的引用地址明显不同。因此返回了false。

#### X.8.3.总结

终于要做Ending了。现在再来看一下开篇给的引入例子，是不是就很清晰了呢。

```
String str1 = new String("SEU") + new String("Calvin");      
System.out.println(str1.intern() == str1);   
System.out.println(str1 == "SEUCalvin");  
```

`str1.intern() == str1`就是上面例子中的情况，`str1.intern()`发现常量池中不存在“SEUCalvin”，因此指向了str1。 "SEUCalvin"在常量池中创建时，也就直接指向了str1了。两个都返回true就理所当然啦。

那么第二段代码呢：

```
String str2 = "SEUCalvin";//新加的一行代码，其余不变
String str1 = new String("SEU")+ new String("Calvin");    
System.out.println(str1.intern() == str1); 
System.out.println(str1 == "SEUCalvin"); 
```


也很简单啦，str2先在常量池中创建了“SEUCalvin”，那么str1.intern()当然就直接指向了str2，你可以去验证它们两个是返回的true。后面的"SEUCalvin"也一样指向str2。所以谁都不搭理在堆空间中的str1了，所以都返回了false。



### X.9.String字符串拼接问题，到底什么时候会走StringBuilder？

#### X.9.1.前言

> 最近在突然想到了String字符串拼接问题，于是做了一个demo测试了一下，到底String类型的字符串在拼接的时候，哪种情况下会走会走StringBulider进行字符串拼接，而哪种情况编译器会对代码进行优化？话不多说，先看demo

#### X.9.2.问题

**案例1**

![14534869-dd266493a5f1e16a](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320221314.webp)

可以发现，str == str2的结果为false，那么我们在看看下一个例子。

**案例2**

![14534869-a762dfb13e0457a2](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320221325.webp)

这时候，两个字符串对比的结果为true。

#### X.9.3.探究问题

这时候，疑问就来了，为什么结果会不一致呢？利用在cmd窗口输入`javap -c TestDemo.class`命令，对字节码文件进行反编译，发现了问题所在？

![14534869-bf48d260d37c2d58](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320221416.webp)

可以看到在案例1中，java代码底层走了StringBuilder，进行字符串拼接，然后调用了StringBuilder的toString方法。

![14534869-2468eb0566d532be](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320221425.webp)

而案例2中，对class文件进行反编译，发现代码出现了一点变化，并没有走StringBuilder进行字符串拼接。

#### X.9.4.总结

1. **案例1中**，通过变量和字符串拼接，java是需要先到内存找变量对应的值，才能进行完成字符串拼接的工作，这种方式java编译器没法优化，只能走`StringBuilder`进行拼接字符串，然后调用toString方法，当然返回的结果和常量池中的`111`这个字符串的内存地址是**不一样**的，因此结果为false。
2. **案例2中**，直接在表达式里写值，java不用根据变量去内存里找对应的值，可以在编译的时候直接对这个表达式进行优化，优化后的表达式从 `"111" + ""` 直接变成了 `"111"` ，两个String类型的变量都指向了常量池的111字符串，因此结果为true;



### X.10.Java中的String有没有长度限制

**String的长度限制**

想要搞清楚这个问题，首先我们需要翻阅一下String的源码，看下其中是否有关于长度的限制或者定义。

String类中有很多重载的构造函数，其中有几个是支持用户传入length来执行长度的：

```
public String(byte bytes[], int offset, int length) 
```

可以看到，这里面的参数length是使用int类型定义的，那么也就是说，String定义的时候，最大支持的长度就是int的最大范围值。

根据Integer类的定义，`java.lang.Integer#MAX_VALUE`的最大值是2^31 - 1;

那么，我们是不是就可以认为String能支持的最大长度就是这个值了呢？

其实并不是，这个值只是在运行期，我们构造String的时候可以支持的一个最大长度，而实际上，在运行期，定义字符串的时候也是有长度限制的。

如以下代码：

```
String s = "11111...1111";//其中有10万个字符"1"
```

当我们使用如上形式定义一个字符串的时候，当我们执行javac编译时，是会抛出异常的，提示如下：

```
错误: 常量字符串过长
```

那么，明明String的构造函数指定的长度是可以支持2147483647(2^31 - 1)的，为什么像以上形式定义的时候无法编译呢？

其实，形如`String s = "xxx";`定义String的时候，xxx被我们称之为字面量，这种字面量在编译之后会以常量的形式进入到Class常量池。

那么问题就来了，因为要进入常量池，就要遵守常量池的有关规定。

**常量池限制**

我们知道，javac是将Java文件编译成class文件的一个命令，那么在Class文件生成过程中，就需要遵守一定的格式。

根据《Java虚拟机规范》中第4.4章节常量池的定义，CONSTANT_String_info 用于表示 java.lang.String 类型的常量对象，格式如下：

```
CONSTANT_String_info {
    u1 tag;
    u2 string_index;
}
```

其中，string_index 项的值必须是对常量池的有效索引， 常量池在该索引处的项必须是 CONSTANT_Utf8_info 结构，表示一组 Unicode 码点序列，这组 Unicode 码点序列最终会被初始化为一个 String 对象。

CONSTANT_Utf8_info 结构用于表示字符串常量的值：

```
CONSTANT_Utf8_info {
    u1 tag;
    u2 length;
    u1 bytes[length];
}
```

其中，length则指明了 bytes[]数组的长度，其类型为u2，

通过翻阅《规范》，我们可以获悉。u2表示两个字节的无符号数，那么1个字节有8位，2个字节就有16位。

16位无符号数可表示的最大值位 `2^16 - 1 = 65535`。

也就是说，Class文件中常量池的格式规定了，其字符串常量的长度不能超过65535。

那么，我们尝试使用以下方式定义字符串：

```
String s = "11111...1111";//其中有65535万个字符"1"
```

尝试使用javac编译，同样会得到"错误: 常量字符串过长"，那么原因是什么呢？

其实，这个原因在javac的代码中是可以找到的，在Gen类中有如下代码：

```
private void checkStringConstant(DiagnosticPosition var1, Object var2) {
    if (this.nerrs == 0 && var2 != null && var2 instanceof String
    	&& ((String)var2).length() >= 65535) {
        this.log.error(var1, "limit.string", new Object[0]);
        ++this.nerrs;
    }
}
```

代码中可以看出，当参数类型为String，并且长度大于等于65535的时候，就会导致编译失败。

这个地方大家可以尝试着debug一下javac的编译过程（视频中有对java的编译过程进行debug的方法），也可以发现这个地方会报错。

如果我们尝试以65534个字符定义字符串，则会发现可以正常编译。

其实，关于这个值，在《Java虚拟机规范》也有过说明：

> if the Java Virtual Machine code for a method is exactly 65535 bytes long and ends with an instruction that is 1 byte long, then that instruction cannot be protected by an exception handler. A compiler writer can work around this bug by limiting the maximum size of the generated Java Virtual Machine code for any method, instance initialization method, or static initializer (the size of any code array) to 65534 bytes

**运行期限制**

上面提到的这种String长度的限制是编译期的限制，也就是使用String s= “”;这种字面值方式定义的时候才会有的限制。

那么。String在运行期有没有限制呢，答案是有的，就是我们前文提到的那个Integer.MAX_VALUE ，这个值约等于4G，在运行期，如果String的长度超过这个范围，就可能会抛出异常。(在jdk 1.9之前）

int 是一个 32 位变量类型，取正数部分来算的话，他们最长可以有

```
2^31-1 =2147483647 个 16-bit Unicodecharacter

2147483647 * 16 = 34359738352 位

34359738352 / 8 = 4294967294 (Byte)

4294967294 / 1024 = 4194303.998046875 (KB)

4194303.998046875 / 1024 = 4095.9999980926513671875 (MB)

4095.9999980926513671875 / 1024 = 3.99999999813735485076904296875 (GB)
```

有近 4G 的容量。

很多人会有疑惑，编译的时候最大长度都要求小于65535了，运行期怎么会出现大于65535的情况呢。这其实很常见，如以下代码：

```
String s = "";
for (int i = 0; i <100000 ; i++) {
    s+="i";
}
```

得到的字符串长度就有10万，另外我之前在实际应用中遇到过这个问题。

之前一次系统对接，需要传输高清图片，约定的传输方式是对方将图片转成BASE6编码，我们接收到之后再转成图片。

在将BASE64编码后的内容赋值给字符串的时候就抛了异常。

**总结**

字符串有长度限制，在编译期，要求字符串常量池中的常量不能超过65535，并且在javac执行过程中控制了最大值为65534。

在运行期，长度不能超过Int的范围，否则会抛异常。



### X.11.Java中的String到底占用多大的内存空间？

#### X.11.1.Java对象的结构

首先，我们来下Java对象在虚拟机中的结构，这里，以HotSpot虚拟机为例。

![20201021013353498](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320224934.png)

从上面的这张图里面可以看出，对象在内存中的结构主要包含以下几个部分：

- Mark Word(标记字段)：对象的Mark Word部分占4个字节，其内容是一系列的标记位，比如轻量级锁的标记位，偏向锁标记位等等。
- Klass Pointer（Class对象指针）：Class对象指针的大小也是4个字节，其指向的位置是对象对应的Class对象（其对应的元数据对象）的内存地址
- 对象实际数据：这里面包括了对象的所有成员变量，其大小由各个成员变量的大小决定，比如：byte和boolean是1个字节，short和char是2个字节，int和float是4个字节，long和double是8个字节，reference是4个字节
- 对齐：最后一部分是对齐填充的字节，按8个字节填充。

换种说法就是：

- 对象头（object header）：8 个字节（保存对象的 class 信息、ID、在虚拟机中的状态）
- Java 原始类型数据：如 int, float, char 等类型的数据
- 引用（reference）：4 个字节
- 填充符（padding）

#### X.11.2.Java中的String类型

**空String占用的空间**

这里，我们以Java8为例进行说明。首先，我们来看看String类中的成员变量。

```java
/** The value is used for character storage. */
private final char value[];
 
/** Cache the hash code for the string */
private int hash; // Default to 0
 
/** use serialVersionUID from JDK 1.0.2 for interoperability */
private static final long serialVersionUID = -6849794470754667710L;
```

在 Java 里数组也是对象，因此数组也有对象头。所以，一个数组所占的空间为对象头所占的空间加上数组长度加上数组的引用，即 8 + 4 + 4= 16 字节 。

所以，我们可以得出一个空String对象所占用的内存空间，如下所示。

```java
对象头（8 字节）+ 引用 (4 字节 )  + char 数组（16 字节）+ 1个 int（4字节）+ 1个long（8字节）= 40 字节
```

**所以，小伙伴们，你们的回答正确吗？**

**非空String占用的空间**

如果String字符串的长度大于0的话，我们也可以得出String占用内存的计算公式，如下所示。

```java
40 + 2 * n
```

其中，n为字符串的长度。

这里，可能有小伙伴会问，为什么是 `40 + 2 * n` 呢？这是因为40是空字符串占用的内存空间，这个我们上面已经说过了，String类实际上是把数据存储到char[]这个成员变量数组中的，而char[]数组中的一个char类型的数据占用2个字节的空间，所以，只是String中的数据就会占用 2 * n（n为字符串的长度）个字节的空间，再加上空字符串所占用的40个字节空间，最终得出一个字符串所占用的存储空间为： `40 + 2 * n` （n为字符串长度）。

注：`40 + 2 * n` 这个公式我们可以看成是计算String对象占用多大内存空间的通用公式。

因此在代码中大量使用String对象时，应考虑内存的实际占用情况。

#### X.11.3.验证结论

接下来，我们就一起来验证下我们上面的结论。首先，创建一个UUIDUtils类用来生成32位的UUID，如下所示。

```java
package io.mykit.binghe.string.test;

import java.util.UUID;

/**
 * @author binghe
 * @version 1.0.0
 * @description 生成没有-的UUID
 */
public class UUIDUtils {
	public static String getUUID(){
		String uuid = UUID.randomUUID().toString();
		return uuid.replace("-", "");
	}
}
```

接下来，创建一个TestString类，在main()方法中创建一个长度为4000000的数组，然后在数组中放满UUID字符串，如下所示。

```java
package io.mykit.binghe.string.test;

import java.util.UUID;

/**
 * @author binghe
 * @version 1.0.0
 * @description 测试String占用的内存空间
 */
public class TestString{
    public static void main(String[] args){
         String[] strContainer = new String[4000000];
        for(int i = 0; i < 4000000; i++){
            strContainer[i] = UUIDUtils.getUUID();
            System.out.println(i);
        }
        //防止程序退出
        while(true){

        }
    }
}
```

这里，4000000个字符串，每个字符串的长度为32，所以保存字符串数据所占用的内存空间为：(40 + 32 * 2) * 4000000 = 416000000字节，约等于416MB。

我们使用Jprofiler内存分析工具进行分析：

![20201021013440615](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320224944.png)

可以看到，使用Jprofiler内存分析工具的结果为：321MB + 96632KB，约等于417MB。之所以使用Jprofiler内存分析工具得出的结果比我们计算的大些，是因为在程序实际运行的过程中，程序内部也会生成一些字符串，这些字符串也会占用内存空间！！





