[toc]

# 面试题

## 容器简介

1.同步容器（如Vector）的所有操作一定是线程安全的吗？



## List

### 1. 当面试官问我ArrayList和LinkedList哪个更占空间时，我这么答让他眼前一亮

一般情况下，LinkedList的占用空间更大，因为每个节点要维护指向前后地址的两个节点，但也不是绝对，如果刚好数据量超过ArrayList默认的临时值时，ArrayList占用的空间也是不小的，因为扩容的原因会浪费将近原来数组一半的容量，不过，因为ArrayList的数组变量是用transient关键字修饰的，如果集合本身需要做序列化操作的话，ArrayList这部分多余的空间不会被序列化。



## Set

### 1.HashSet 如何保证元素不重复？



## Map

### 1、HashMap与HashTable有什么不同？

### 2、为什么要将转换成树形结构的阈值设置为8呢？为什么不将转换成链表结构的阈值也设置为8呢？

### 3、HashMap 为什么不用平衡树，而用红黑树？

### 4、HashMap在并发下会产生什么问题？有什么替代方案?

### 5、HashMap中的key可以是任何对象或数据类型吗？

### 6、为什么HashMap是线程不安全的吗？

### 7、HashMap怎样解决hash冲突？

### 8、谈谈ConcurrentHashMap是如何保证线程安全的？

### 9、如何决定使用 HashMap 还是 TreeMap？







