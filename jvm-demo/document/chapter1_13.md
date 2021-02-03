[toc]



# String Table

## String 的基本特性

- String：字符串，使用一对""引起来表示。
- String 声明为 final 的，不可被继承
- String 实现了 Serializable 接口：表示字符串是支持序列化的
- String 实现了 Comparable 接口：表示 String 可以比较大小
- String 在 JDK 8 及以前内部定义了 final char[] value 用于存储字符串数据。JDK 9 时改为 byte[]
- String：代表不可变的字符序列。简称：不可变性。
  - 当对字符串重新赋值时，需要重写指定内存区域赋值，不能使用原来的 value 进行赋值。
  - 当对现有的字符串进行连接操作时，也需要重新指定内存区域赋值，不能使用原有的value进行赋值。
  - 当调用 String 的 replace() 方法修改指定字符或字符串时，也需要重新指定内存区域赋值，不能使用原有的 value 进行赋值。
- 通过字面量的方式（区别于 new）给一个字符串赋值，此时的字符串值声明在字符串常量池中。
- 字符串常量池中是不会存储相同内容的字符串的
  - String 的 String Pool 是一个固定大小的 Hashtable，默认值大小长度是1009。如果放进 String Pool 的 String 非常多，就会造成 Hash 冲突严重，从而导致链表会很长，而链表长了后直接会造成的影响就是当调用String.intern时性能会大幅下降。
  - 使用 -XX:StringTableSize 可设置 StringTable 的长度
  - 在 JDK 6 中 StringTable 是固定的，就是 1009 的长度，所以如果常量池中的字符串过多就会导致效率下降很快。StringTableSize 设置没有要求。
  - 在 JDK 7 中，StringTable 的长度默认值是 60013，StringTableSize 设置没有要求
  - JDK 8 开始，设置 StringTable 的长度的话，1009 是可设置的最小值。



### String 在 JDK9 中存储结构变更

![image-20210202000853523](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202000853523.png)

![image-20210202000920745](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202000920745.png)



## String 的内存分配

- 在 Java 语言中有 8 种基本类型和一种比较特殊的类型 String。这些类型为了使它们在运行过程中速度更快、更节省内存，都提供了一种常量池的概念。

- 常量池就类似一个 Java 系统级别提供的缓存。8 种基本数据类型的常量池都是系统协调的，String 类型的常量池比较特殊。它的主要使用方法有两种：

  - 直接使用双引号声明出来的 String 对象会直接存储在常量池中。

    比如：`String info = "atguigu.com";`

  - 如果不是用双引号声明的 String 对象，可以使用 String 提供的 intern() 方法。

- Java 6 及以前，字符串常量池存放在永久代。

- Java 7 中 Oracle 的工程师对字符串池的逻辑做了很大的改变，即将字符串常量池的位置调整到 Java 堆内。

  - 所有的字符串都保存在堆（Heap）中，和其他普通对象一样，这样可以让你在进行调优应用时仅需要调整堆大小就可以了。
  - 字符串常量池概念原本使用得比较多，但是这个改动使得我们有足够的理由让我们重新考虑在 Java 7中使用 String.intern()。

- Java 8 元空间，字符串常量在堆

![image-20210202221304976](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202221304976.png)

![image-20210202221316313](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202221316313.png)

### StringTable 为什么调整

![image-20210202221357066](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202221357066.png)



## String 的基本操作

Java 语言规范里要求完全相同的字符串字面量，应该包含同样的 Unicode 字符序列（包含同一份码点序列的常量），并且必须是指向同一个String类实例。

![image-20210202225933669](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202225933669.png)

![image-20210202230133400](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230133400.png)

![image-20210202230235838](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230235838.png)





## 字符串拼接操作

1. 常量与常量的拼接结果在常量池，原理是编译期优化
2. 常量池中不会存在相同内容的常量
3. 只要其中有一个是变量，结果就在堆中。变量拼接的原理是 StringBuilder
4. 如果拼接的结果调用 intern() 方法，则主动将常量池中还没有的字符串对象放入池中，并返回此对象地址。

![image-20210202230520108](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230520108.png)

![image-20210202230543278](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230543278.png)

![image-20210202230705622](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230705622.png)

![image-20210202230722615](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230722615.png)



## intern()的使用

![image-20210202230744014](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230744014.png)

- 如果不是用双引号声明的 String 对象，可以使用 String 提供的 intern 方法：intern 方法会从字符串常量池中查询当前字符串是否存在，若不存在就会当前字符串放入常量池中。
  - 比如：`String myInfo = new String("I love atguigu").intern();`
- 也就是说，如果再任意字符串上调用 String.intern 方法，那么其返回结果所指向的那个类实例，必须和直接以常量形式出现得字符串实例完全相同。因此，下列表达式的值必定是 true：`("a" + "b" + "c").intern() == "abc"`
- 通俗点讲，Interned String 就是确保字符串在内存里只有一份拷贝，这样可以节约内存空间，加快字符串操作任务的执行速度。注意，这个值会被存放在字符串内部池（String Intern Pool）。

![image-20210202230800068](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202230800068.png)

![image-20210202231856711](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202231856711.png)

### intern() 的使用：JDK 6 VS JDK 7/8

![image-20210202232032380](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202232032380.png)

![image-20210202232049758](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202232049758.png)

总结String 的 intern() 使用

- JDK 1.6 中，将这个字符串对象尝试放入串池。
  - 如果串池中有，则并不会放入。返回已有的串池中的对象的地址。
  - 如果没有，会把对象复制一份，放入串池，并返回串池中的对象地址
- JDK 1.7 起，将这个字符串对象尝试放入串池。
  - 如果串池中有，则并不会放入。返回已有的串池中的对象的地址。
  - 如果没有，则会把对象的引用地址复制一份，放入串池，并返回串池中的引用地址



### intern() 的使用：练习1

![image-20210202233601525](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202233601525.png)

![image-20210202233653563](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202233653563.png)

![image-20210202233711226](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202233711226.png)



### 面试题

- new String("ab") 会创建几个对象？
- new String("a") + new String("b") 呢？

![image-20210202233857010](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202233857010.png)



### intern() 的效率测试：空间角度

![image-20210202233949757](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210202233949757.png)

大的网站平台，需要内存中存储大量的字符串。比如社交网站，很多人都存储：北京市、海定区等信息。这时候如果字符串都调用intern()方法，就会明显降低内存的大小。



## StringTable的垃圾回收

![image-20210203213844074](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210203213844074.png)





## G1 中的 String 去重操作

![image-20210203213931228](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210203213931228.png)

- 背景：对许多 Java 应用（有大的也有小的）做的测试得出以下结果：
  - 堆存活数据集合里面 String 对象占了 25%
  - 堆存活数据集合里面重复的 String 对象有 13.5%
  - String 对象的平均长度是 45
- 许多大规模的 Java 应用的瓶颈在于内存，测试表明，在这些类型的应用里面，Java 堆中存活的数据集合差不多 25% 是String 对象。更进一步，这里面差不多一半 String 对象是重复，重复的意思是说：`string1.equals(string2)=true`。堆上存在重复的 String 对象必然是一种内存的浪费。这个项目将在 G1 垃圾收集器中实现自动持续对重复的 String 对象进行去重，这样就能避免浪费内存。

- 实现
  - 当垃圾收集器工作的时候，会访问堆上存活的对象。对每一个访问的对象都会检查是否是候选的要去重的 String 对象。
  - 如果是，把这个对象的一个引用插入到队列中等待后续的处理。一个去重的线程在后台运行，处理这个队列。处理队列的一个元素意味着从队列删除这个元素，然后尝试去重它引用的     String 对象。
  - 使用一个 Hashtable 来记录所有的被 String 对象使用的不重复的 char 数组。当去重的时候，会查这个 Hashtable,来看堆上是否已经存在一个一模一样的 char 数组。
  - 如果存在，String 对象会被调整引用那个数组，释放对原来的数组的引用，最终会被垃圾收集器回收掉。
  - 如果查找失败，char 数组会被插入到 Hashtable，这样以后的时候就可以共享这个数组了。

- 命令行选项
  - UseStringDeduplication(bool): 开启 String 去重，默认是不开启的，需要手动开启。
  - PrintStringDeduplicationStatistics(bool):打印详细的去重统计信息
  - StringDeduplicationAgeThreshold(uintx): 达到这个年龄的String对象被认为是去重的候选对象













