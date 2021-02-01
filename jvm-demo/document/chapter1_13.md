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



## String 的基本操作



## 字符串拼接操作



## intern()的使用



## StringTable的垃圾回收



## G1 中的 String 去重操作





