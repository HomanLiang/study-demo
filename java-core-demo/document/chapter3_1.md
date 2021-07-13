[toc]



# Java 容器简介

## 1. 容器简介

### 1.1. 数组与容器

`Java` 中常用的存储容器就是数组和容器，二者有以下区别：

- 存储大小是否固定
  - 数组的**长度固定**；
  - 容器的**长度可变**。
- 数据类型
  - **数组可以存储基本数据类型，也可以存储引用数据类型**；
  - **容器只能存储引用数据类型**，基本数据类型的变量要转换成对应的包装类才能放入容器类中。

### 1.2. 容器框架

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e7461696e65722f6a6176612d636f6e7461696e65722d7374727563747572652e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321122356.png)

Java 容器框架主要分为 `Collection` 和 `Map` 两种。其中，`Collection` 又分为 `List`、`Set` 以及 `Queue`。

- `Collection`- 一个独立元素的序列，这些元素都服从一条或者多条规则。
- `List` - 必须按照插入的顺序保存元素。
  - `Set` - 不能有重复的元素。
- `Queue` - 按照排队规则来确定对象产生的顺序（通常与它们被插入的顺序相同）。
  
- `Map` - 一组成对的“键值对”对象，允许你使用键来查找值。

## 2. 容器的基本机制

> Java 的容器具有一定的共性，它们或全部或部分依赖以下技术。所以，学习以下技术点，对于理解 Java 容器的特性和原理有很大的帮助。

### 2.1. 泛型

`Java 1.5` 引入了泛型技术。

`Java` **容器通过泛型技术来保证其数据的类型安全**。什么是类型安全呢？

举例来说：如果有一个 `List<Object>` 容器，`Java` **编译器在编译时不会对原始类型进行类型安全检查**，却会对带参数的类型进行检查，通过使用 `Object` 作为类型，可以告知编译器该方法可以接受任何类型的对象，比如 `String` 或 `Integer`。

```
List<Object> list = new ArrayList<Object>();
list.add("123");
list.add(123);
```

如果没有泛型技术，如示例中的代码那样，容器中就可能存储任意数据类型，这是很危险的行为。

```
List<String> list = new ArrayList<String>();
list.add("123");
list.add(123);
```

### 2.2. Iterable 和 Iterator

> Iterable 和 Iterator 目的在于遍历访问容器中的元素。

`Iterator` 接口定义：

```
public interface Iterator<E> {

    boolean hasNext();

    E next();

    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    default void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        while (hasNext())
            action.accept(next());
    }
}
```

`Iterable` 接口定义：

```
public interface Iterable<T> {

    Iterator<T> iterator();

    default void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (T t : this) {
            action.accept(t);
        }
    }

    default Spliterator<T> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), 0);
    }
}
```

`Collection` 接口扩展了 `Iterable` 接口。

迭代其实我们可以简单地理解为遍历，是一个标准化遍历各类容器里面的所有对象的接口。它是一个经典的设计模式——迭代器模式（`Iterator`）。

**迭代器模式** - **提供一种方法顺序访问一个聚合对象中各个元素，而又无须暴露该对象的内部表示**。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6f6f702f64657369676e2d7061747465726e732f6974657261746f722d7061747465726e2e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321122415.png)

示例：迭代器遍历

```
public class IteratorDemo {

    public static void main(String[] args) {
        List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        Iterator it = list.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

}
```

### 2.3. Comparable 和 Comparator

`Comparable` 是排序接口。若一个类实现了 `Comparable` 接口，表示该类的实例可以比较，也就意味着支持排序。实现了 `Comparable` 接口的类的对象的列表或数组可以通过 `Collections.sort` 或 `Arrays.sort` 进行自动排序。

`Comparable` 接口定义：

```
public interface Comparable<T> {
    public int compareTo(T o);
}
```

`Comparator` 是比较接口，我们如果需要控制某个类的次序，而该类本身不支持排序(即没有实现 `Comparable` 接口)，那么我们就可以建立一个“该类的比较器”来进行排序，这个“比较器”只需要实现 `Comparator` 接口即可。也就是说，我们可以通过实现 `Comparator` 来新建一个比较器，然后通过这个比较器对类进行排序。

`Comparator` 接口定义：

```
@FunctionalInterface
public interface Comparator<T> {

    int compare(T o1, T o2);

    boolean equals(Object obj);

    // 反转
    default Comparator<T> reversed() {
        return Collections.reverseOrder(this);
    }

    default Comparator<T> thenComparing(Comparator<? super T> other) {
        Objects.requireNonNull(other);
        return (Comparator<T> & Serializable) (c1, c2) -> {
            int res = compare(c1, c2);
            return (res != 0) ? res : other.compare(c1, c2);
        };
    }

    // thenComparingXXX 方法略

    // 静态方法略
}
```

在 `Java` 容器中，一些可以排序的容器，如 `TreeMap`、`TreeSet`，都可以通过传入 `Comparator`，来定义内部元素的排序规则。

### 2.4. Cloneable

`Java` 中 一个类要实现 `clone` 功能 必须实现 `Cloneable` 接口，否则在调用 `clone()` 时会报 `CloneNotSupportedException` 异常。

`Java` 中所有类都默认继承 `java.lang.Object` 类，在 `java.lang.Object` 类中有一个方法 `clone()`，这个方法将返回 `Object` 对象的一个拷贝。`Object` 类里的 `clone()` 方法仅仅用于浅拷贝（拷贝基本成员属性，对于引用类型仅返回指向改地址的引用）。

如果 `Java` 类需要深拷贝，需要覆写 `clone()` 方法。

### 2.5. fail-fast

#### 2.5.1.fail-fast 的要点

`Java` 容器（如：`ArrayList`、`HashMap`、`TreeSet` 等待）的 `javadoc` 中常常提到类似的描述：

> 注意，迭代器的快速失败行为无法得到保证，因为一般来说，不可能对是否出现不同步并发修改做出任何硬性保证。快速失败（fail-fast）迭代器会尽最大努力抛出 `ConcurrentModificationException`。因此，为提高这类迭代器的正确性而编写一个依赖于此异常的程序是错误的做法：迭代器的快速失败行为应该仅用于检测 bug。

那么，我们不禁要问，什么是 `fail-fast`，为什么要有 `fail-fast` 机制？

**fail-fast 是 Java 容器的一种错误检测机制**。当多个线程对容器进行结构上的改变的操作时，就可能触发 `fail-fast` 机制。记住是有可能，而不是一定。

例如：假设存在两个线程（线程 1、线程 2），线程 1 通过 `Iterator` 在遍历容器 A 中的元素，在某个时候线程 2 修改了容器 A 的结构（是结构上面的修改，而不是简单的修改容器元素的内容），那么这个时候程序就会抛出 `ConcurrentModificationException` 异常，从而产生 fail-fast 机制。

**容器在迭代操作中改变元素个数（添加、删除元素）都可能会导致 fail-fast**。

示例：`fail-fast` 示例

```
public class FailFastDemo {

    private static int MAX = 100;

    private static List<Integer> list = new ArrayList<>();

    public static void main(String[] args) {
        for (int i = 0; i < MAX; i++) {
            list.add(i);
        }
        new Thread(new MyThreadA()).start();
        new Thread(new MyThreadB()).start();
    }

    /** 迭代遍历容器所有元素 */
    static class MyThreadA implements Runnable {

        @Override
        public void run() {
            Iterator<Integer> iterator = list.iterator();
            while (iterator.hasNext()) {
                int i = iterator.next();
                System.out.println("MyThreadA 访问元素:" + i);
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /** 遍历删除指定范围内的所有偶数 */
    static class MyThreadB implements Runnable {

        @Override
        public void run() {
            int i = 0;
            while (i < MAX) {
                if (i % 2 == 0) {
                    System.out.println("MyThreadB 删除元素" + i);
                    list.remove(i);
                }
                i++;
            }
        }

    }

}
```

执行后，会抛出 `java.util.ConcurrentModificationException` 异常。

#### 2.5.2.解决 fail-fast

`fail-fast` 有两种解决方案：

- 在遍历过程中所有涉及到改变容器个数的地方全部加上 `synchronized` 或者直接使用 `Collections.synchronizedXXX` 容器，这样就可以解决。但是不推荐，因为增删造成的同步锁可能会阻塞遍历操作，影响吞吐。
- 使用并发容器，如：`CopyOnWriterArrayList`。

## 3. 容器和线程安全

为了在并发环境下安全地使用容器，`Java` 提供了同步容器和并发容器。



## X.面试题

### X.1.同步容器（如Vector）的所有操作一定是线程安全的吗？

为了方便编写出线程安全的程序，`Java` 里面提供了一些线程安全类和并发工具，比如：同步容器、并发容器、阻塞队列等。

最常见的同步容器就是 `Vector` 和 `Hashtable` 了，那么，同步容器的所有操作都是线程安全的吗？

这个问题不知道你有没有想过，本文就来深入分析一下这个问题，一个很容易被忽略的问题。

**同步容器**

在 `Java` 中，同步容器主要包括2类：

- `Vector`、`Stack`、`HashTable`
- `Collections` 类中提供的静态工厂方法创建的类

本文拿相对简单的 `Vecotr` 来举例，我们先来看下 `Vector` 中几个重要方法的源码：

```
public synchronized boolean add(E e) {
    modCount++;
    ensureCapacityHelper(elementCount + 1);
    elementData[elementCount++] = e;
    return true;
}

public synchronized E remove(int index) {
    modCount++;
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);
    E oldValue = elementData(index);

    int numMoved = elementCount - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    elementData[--elementCount] = null; // Let gc do its work

    return oldValue;
}

public synchronized E get(int index) {
    if (index >= elementCount)
        throw new ArrayIndexOutOfBoundsException(index);

    return elementData(index);
}
```

可以看到，`Vector` 这样的同步容器的所有公有方法全都是 `synchronized` 的，也就是说，我们可以在多线程场景中放心的使用单独这些方法，因为这些方法本身的确是线程安全的。

但是，请注意上面这句话中，有一个比较关键的词：单独

因为，虽然同步容器的所有方法都加了锁，但是对这些容器的复合操作无法保证其线程安全性。需要客户端通过主动加锁来保证。

简单举一个例子，我们定义如下删除 `Vector` 中最后一个元素方法：

```
public Object deleteLast(Vector v){
    int lastIndex  = v.size()-1;
    v.remove(lastIndex);
}
```

上面这个方法是一个复合方法，包括 `size(）`和 `remove()`，乍一看上去好像并没有什么问题，无论是 `size()` 方法还是 `remove()` 方法都是线程安全的，那么整个 `deleteLast` 方法应该也是线程安全的。

但是时，如果多线程调用该方法的过程中，`remove` 方法有可能抛出 `ArrayIndexOutOfBoundsException`。

```
Exception in thread "Thread-1" java.lang.ArrayIndexOutOfBoundsException: Array index out of range: 879
    at java.util.Vector.remove(Vector.java:834)
    at com.hollis.Test.deleteLast(EncodeTest.java:40)
    at com.hollis.Test$2.run(EncodeTest.java:28)
    at java.lang.Thread.run(Thread.java:748)
```

我们上面贴了 `remove` 的源码，我们可以分析得出：当 `index >= elementCount` 时，会抛出 `ArrayIndexOutOfBoundsException` ，也就是说，当当前索引值不再有效的时候，将会抛出这个异常。

因为 `removeLast` 方法，有可能被多个线程同时执行，当线程2通过 `index()` 获得索引值为10，在尝试通过 `remove()` 删除该索引位置的元素之前，线程1把该索引位置的值删除掉了，这时线程一在执行时便会抛出异常。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321172235.webp)

为了避免出现类似问题，可以尝试加锁：

```
public void deleteLast() {
    synchronized (v) {
        int index = v.size() - 1;
        v.remove(index);
    }
}
```

如上，我们在 `deleteLast` 中，对 `v` 进行加锁，即可保证同一时刻，不会有其他线程删除掉 `v` 中的元素。

另外，如果以下代码会被多线程执行时，也要特别注意：

```
for (int i = 0; i < v.size(); i++) {
    v.remove(i);
}
```

由于，不同线程在同一时间操作同一个 `Vector`，其中包括删除操作，那么就同样有可能发生线程安全问题。所以，在使用同步容器的时候，如果涉及到多个线程同时执行删除操作，就要考虑下是否需要加锁。

**同步容器的问题**

前面说过了，同步容器直接保证单个操作的线程安全性，但是无法保证复合操作的线程安全，遇到这种情况时，必须要通过主动加锁的方式来实现。

而且，除此之外，同步容易由于对其所有方法都加了锁，这就导致多个线程访问同一个容器的时候，只能进行顺序访问，即使是不同的操作，也要排队，如 `get` 和 `add` 要排队执行。这就大大的降低了容器的并发能力。

**并发容器**

针对前文提到的同步容器存在的并发度低问题，从 `Java5` 开始，`java.util.concurent` 包下，提供了大量支持高效并发的访问的集合类，我们称之为并发容器。

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321172252.webp)

针对前文提到的同步容器的复合操作的问题，一般在Map中发生的比较多，所以在 `ConcurrentHashMap` 中增加了对常用复合操作的支持，比如 `putIfAbsent()`、`replace()`，这2个操作都是原子操作，可以保证线程安全。

另外，并发包中的 `CopyOnWriteArrayList` 和 `CopyOnWriteArraySet` 是 `Copy-On-Write` 的两种实现。

`Copy-On-Write` 容器即写时复制的容器。通俗的理解是当我们往一个容器添加元素的时候，不直接往当前容器添加，而是先将当前容器进行 `Copy`，复制出一个新的容器，然后新的容器里添加元素，添加完元素之后，再将原容器的引用指向新的容器。

`CopyOnWriteArrayList` 中 `add/remove` 等写方法是需要加锁的，而读方法是没有加锁的。

这样做的好处是我们可以对 `CopyOnWrite` 容器进行并发的读，当然，这里读到的数据可能不是最新的。因为写时复制的思想是通过延时更新的策略来实现数据的最终一致性的，并非强一致性。

但是，作为代替 `Vector` 的 `CopyOnWriteArrayList` 并没有解决同步容器的复合操作的线程安全性问题。

**总结**

本文介绍了同步容器和并发容器。

同步容器是通过加锁实现线程安全的，并且只能保证单独的操作是线程安全的，无法保证复合操作的线程安全性。并且同步容器的读和写操作之间会互相阻塞。

并发容器是 `Java 5` 中提供的，主要用来代替同步容器。有更好的并发能力。而且其中的 `ConcurrentHashMap` 定义了线程安全的复合操作。

在多线程场景中，如果使用并发容器，一定要注意复合操作的线程安全问题。必要时候要主动加锁。

在并发场景中，建议直接使用 `java.util.concurent` 包中提供的容器类，如果需要复合操作时，建议使用有些容器自身提供的复合方法。