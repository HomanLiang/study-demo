[toc]



# Java 容器之 Map

## 1. Map 简介

### 1.1. Map 架构

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e7461696e65722f4d61702d6469616772616d732e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123645.png)

Map 家族主要成员功能如下：

- `Map` 是 Map 容器家族的祖先，Map 是一个用于保存键值对(key-value)的接口。**Map 中不能包含重复的键；每个键最多只能映射到一个值。**
- `AbstractMap` 继承了 `Map` 的抽象类，它实现了 `Map` 中的核心 API。其它 `Map` 的实现类可以通过继承 `AbstractMap` 来减少重复编码。
- `SortedMap` 继承了 `Map` 的接口。`SortedMap` 中的内容是排序的键值对，排序的方法是通过实现比较器(`Comparator`)完成的。
- `NavigableMap` 继承了 `SortedMap` 的接口。相比于 `SortedMap`，`NavigableMap` 有一系列的“导航”方法；如"获取大于/等于某对象的键值对"、“获取小于/等于某对象的键值对”等等。
- `HashMap` 继承了 `AbstractMap`，但没实现 `NavigableMap` 接口。`HashMap` 的主要作用是储存无序的键值对，而 `Hash` 也体现了它的查找效率很高。`HashMap` 是使用最广泛的 `Map`。
- `Hashtable` 虽然没有继承 `AbstractMap`，但它继承了 `Dictionary`（`Dictionary` 也是键值对的接口），而且也实现 `Map` 接口。因此，`Hashtable` 的主要作用是储存无序的键值对。和 HashMap 相比，`Hashtable` 在它的主要方法中使用 `synchronized` 关键字修饰，来保证线程安全。但是，由于它的锁粒度太大，非常影响读写速度，所以，现代 Java 程序几乎不会使用 `Hashtable` ，如果需要保证线程安全，一般会用 `ConcurrentHashMap` 来替代。
- `TreeMap` 继承了 `AbstractMap`，且实现了 `NavigableMap` 接口。`TreeMap` 的主要作用是储存有序的键值对，排序依据根据元素类型的 `Comparator` 而定。
- `WeakHashMap` 继承了 `AbstractMap`。`WeakHashMap` 的键是**弱引用** （即 `WeakReference`），它的主要作用是当 GC 内存不足时，会自动将 `WeakHashMap` 中的 key 回收，这避免了 `WeakHashMap` 的内存空间无限膨胀。很明显，`WeakHashMap` 适用于作为缓存。

### 1.2. Map 接口

Map 的定义如下：

```
public interface Map<K,V> { }
```

Map 是一个用于保存键值对(key-value)的接口。**Map 中不能包含重复的键；每个键最多只能映射到一个值。**

Map 接口提供三种 `Collection` 视图，允许以**键集**、**值集**或**键-值映射关系集**的形式访问数据。

Map 有些实现类，可以有序的保存元素，如 `TreeMap`；另一些实现类则不保证顺序，如 `HashMap` 类。

Map 的实现类应该提供 2 个“标准的”构造方法：

- void（无参数）构造方法，用于创建空 Map；
- 带有单个 Map 类型参数的构造方法，用于创建一个与其参数具有相同键-值映射关系的新 Map。

实际上，后一个构造方法允许用户复制任意 Map，生成所需类的一个等价 Map。尽管无法强制执行此建议（因为接口不能包含构造方法），但是 JDK 中所有通用的 Map 实现都遵从它。

### 1.3. Map.Entry 接口

`Map.Entry` 一般用于通过迭代器（`Iterator`）访问问 `Map`。

`Map.Entry` 是 Map 中内部的一个接口，`Map.Entry` 代表了 **键值对** 实体，Map 通过 `entrySet()` 获取 `Map.Entry` 集合，从而通过该集合实现对键值对的操作。

### 1.4. AbstractMap 抽象类

`AbstractMap` 的定义如下：

```
public abstract class AbstractMap<K,V> implements Map<K,V> {}
```

`AbstractMap` 抽象类提供了 `Map` 接口的核心实现，以最大限度地减少实现 `Map` 接口所需的工作。

要实现不可修改的 Map，编程人员只需扩展此类并提供 `entrySet()` 方法的实现即可，该方法将返回 `Map` 的映射关系 Set 视图。通常，返回的 set 将依次在 `AbstractSet` 上实现。此 Set 不支持 `add()` 或 `remove()` 方法，其迭代器也不支持 `remove()` 方法。

要实现可修改的 `Map`，编程人员必须另外重写此类的 `put` 方法（否则将抛出 `UnsupportedOperationException`），`entrySet().iterator()` 返回的迭代器也必须另外实现其 `remove()` 方法。

### 1.5. SortedMap 接口

`SortedMap` 的定义如下：

```
public interface SortedMap<K,V> extends Map<K,V> { }
```

`SortedMap` 继承了 `Map` ，它是一个有序的 `Map`。

`SortedMap` 的排序方式有两种：**自然排序**或者**用户指定比较器**。**插入有序 `SortedMap` 的所有元素都必须实现 `Comparable` 接口（或者被指定的比较器所接受）**。

另外，所有 `SortedMap` 实现类都应该提供 4 个“标准”构造方法：

1. `void`（无参数）构造方法，它创建一个空的有序 `Map`，按照键的自然顺序进行排序。
2. 带有一个 `Comparator` 类型参数的构造方法，它创建一个空的有序 `Map`，根据指定的比较器进行排序。
3. 带有一个 `Map` 类型参数的构造方法，它创建一个新的有序 `Map`，其键-值映射关系与参数相同，按照键的自然顺序进行排序。
4. 带有一个 `SortedMap` 类型参数的构造方法，它创建一个新的有序 `Map`，其键-值映射关系和排序方法与输入的有序 Map 相同。无法保证强制实施此建议，因为接口不能包含构造方法。

### 1.6. NavigableMap 接口

`NavigableMap` 的定义如下：

```
public interface NavigableMap<K,V> extends SortedMap<K,V> { }
```

`NavigableMap` 继承了 `SortedMap` ，它提供了丰富的查找方法。

NavigableMap 分别提供了获取“键”、“键-值对”、“键集”、“键-值对集”的相关方法。

`NavigableMap` 提供的功能可以分为 4 类：

- 获取键-值对

  - `lowerEntry`、`floorEntry`、`ceilingEntry` 和 `higherEntry` 方法，它们分别返回与小于、小于等于、大于等于、大于给定键的键关联的 Map.Entry 对象。
  - `firstEntry`、`pollFirstEntry`、`lastEntry` 和 `pollLastEntry` 方法，它们返回和/或移除最小和最大的映射关系（如果存在），否则返回 null。

- 获取键

  。这个和第 1 类比较类似。

  - `lowerKey`、`floorKey`、`ceilingKey` 和 `higherKey` 方法，它们分别返回与小于、小于等于、大于等于、大于给定键的键。

- 获取键的集合

  - `navigableKeySet`、`descendingKeySet` 分别获取正序/反序的键集。

- **获取键-值对的子集**

### 1.7. Dictionary 抽象类

`Dictionary` 的定义如下：

```
public abstract class Dictionary<K,V> {}
```

`Dictionary` 是 JDK 1.0 定义的操作键值对的抽象类，它包括了操作键值对的基本方法。

## 2. HashMap 类

`HashMap` 类是最常用的 `Map`。

### 2.1. HashMap 要点

从 `HashMap` 的命名，也可以看出：**`HashMap` 以散列方式存储键值对**。

**`HashMap` 允许使用空值和空键**。（`HashMap` 类大致等同于 `Hashtable`，除了它是不同步的并且允许为空值。）这个类不保序；特别是，它的元素顺序可能会随着时间的推移变化。

**`HashMap` 有两个影响其性能的参数：初始容量和负载因子**。

- 容量是哈希表中桶的数量，初始容量就是哈希表创建时的容量。
- 加载因子是散列表在其容量自动扩容之前被允许的最大饱和量。当哈希表中的 entry 数量超过负载因子和当前容量的乘积时，散列表就会被重新映射（即重建内部数据结构），一般散列表大约是存储桶数量的两倍。

通常，默认加载因子（0.75）在时间和空间成本之间提供了良好的平衡。较高的值会减少空间开销，但会增加查找成本（反映在大部分 `HashMap`类的操作中，包括 `get` 和 `put`）。在设置初始容量时，应考虑映射中的条目数量及其负载因子，以尽量减少重新运行操作的次数。如果初始容量大于最大入口数除以负载因子，则不会发生重新刷新操作。

如果许多映射要存储在 `HashMap` 实例中，使用足够大的容量创建映射将允许映射存储的效率高于根据需要执行自动重新散列以增长表。请注意，使用多个具有相同 `hashCode()` 的密钥是降低任何散列表性能的一个可靠方法。为了改善影响，当键是 `Comparable` 时，该类可以使用键之间的比较顺序来帮助断开关系。

`HashMap` 不是线程安全的。

### 2.2. HashMap 原理

#### HashMap 数据结构

`HashMap` 的核心字段：

- `table` - `HashMap` 使用一个 `Node<K,V>[]` 类型的数组 `table` 来储存元素。
- `size` - 初始容量。 初始为 16，每次容量不够自动扩容
- `loadFactor` - 负载因子。自动扩容之前被允许的最大饱和量，默认 0.75。

```
public class HashMap<K,V> extends AbstractMap<K,V>
    implements Map<K,V>, Cloneable, Serializable {

    // 该表在初次使用时初始化，并根据需要调整大小。分配时，长度总是2的幂。
    transient Node<K,V>[] table;
    // 保存缓存的 entrySet()。请注意，AbstractMap 字段用于 keySet() 和 values()。
    transient Set<Map.Entry<K,V>> entrySet;
    // map 中的键值对数
    transient int size;
    // 这个HashMap被结构修改的次数结构修改是那些改变HashMap中的映射数量或者修改其内部结构（例如，重新散列）的修改。
    transient int modCount;
    // 下一个调整大小的值（容量*加载因子）。
    int threshold;
    // 散列表的加载因子
    final float loadFactor;
}
```

#### HashMap 构造方法

```
public HashMap(); // 默认加载因子0.75
public HashMap(int initialCapacity); // 默认加载因子0.75；以 initialCapacity 初始化容量
public HashMap(int initialCapacity, float loadFactor); // 以 initialCapacity 初始化容量；以 loadFactor 初始化加载因子
public HashMap(Map<? extends K, ? extends V> m) // 默认加载因子0.75
```

#### put 方法的实现

put 方法大致的思路为：

- 对 key 的 `hashCode()` 做 hash 计算，然后根据 hash 值再计算 Node 的存储位置;
- 如果没有哈希碰撞，直接放到桶里；如果有哈希碰撞，以链表的形式存在桶后。
- 如果哈希碰撞导致链表过长(大于等于 `TREEIFY_THRESHOLD`，数值为 8)，就把链表转换成红黑树；
- 如果节点已经存在就替换旧值
- 桶数量超过容量*负载因子（即 `load factor * current capacity`），HashMap 调用 `resize` 自动扩容一倍

具体代码的实现如下：

```
public V put(K key, V value) {
    return putVal(hash(key), key, value, false, true);
}

// hashcode 无符号位移 16 位
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}

final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    // tab 为空则创建
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    // 计算 index，并对 null 做处理
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        Node<K,V> e; K k;
        // 节点存在
        if (p.hash == hash &&
            ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        // 该链为树
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        // 该链为链表
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break;
                }
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        // 写入
        if (e != null) { // existing mapping for key
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
    }
    ++modCount;
    if (++size > threshold)
        resize();
    afterNodeInsertion(evict);
    return null;
}
```

为什么计算 hash 使用 hashcode 无符号位移 16 位。

假设要添加两个对象 a 和 b，如果数组长度是 16，这时对象 a 和 b 通过公式 (n - 1) & hash 运算，也就是 (16-1)＆a.hashCode 和 (16-1)＆b.hashCode，15 的二进制为 0000000000000000000000000001111，假设对象 A 的 hashCode 为 1000010001110001000001111000000，对象 B 的 hashCode 为 0111011100111000101000010100000，你会发现上述与运算结果都是 0。这样的哈希结果就太让人失望了，很明显不是一个好的哈希算法。

但如果我们将 hashCode 值右移 16 位（h >>> 16 代表无符号右移 16 位），也就是取 int 类型的一半，刚好可以将该二进制数对半切开，并且使用位异或运算（如果两个数对应的位置相反，则结果为 1，反之为 0），这样的话，就能避免上面的情况发生。这就是 hash() 方法的具体实现方式。**简而言之，就是尽量打乱 hashCode 真正参与运算的低 16 位。**

#### get 方法的实现

在理解了 put 之后，get 就很简单了。大致思路如下：

- 对 key 的 hashCode() 做 hash 计算，然后根据 hash 值再计算桶的 index
- 如果桶中的第一个节点命中，直接返回；
- 如果有冲突，则通过 `key.equals(k)` 去查找对应的 entry
  - 若为树，则在红黑树中通过 `key.equals(k)` 查找，O(logn)；
  - 若为链表，则在链表中通过 `key.equals(k)` 查找，O(n)。

具体代码的实现如下：

```
public V get(Object key) {
    Node<K,V> e;
    return (e = getNode(hash(key), key)) == null ? null : e.value;
}

final Node<K,V> getNode(int hash, Object key) {
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (first = tab[(n - 1) & hash]) != null) {
        // 直接命中
        if (first.hash == hash && // always check first node
            ((k = first.key) == key || (key != null && key.equals(k))))
            return first;
        // 未命中
        if ((e = first.next) != null) {
            // 在树中 get
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            // 在链表中 get
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || (key != null && key.equals(k))))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

#### hash 方法的实现

HashMap **计算桶下标（index）公式：`key.hashCode() ^ (h >>> 16)`**。

下面针对这个公式来详细讲解。

在 `get` 和 `put` 的过程中，计算下标时，先对 `hashCode` 进行 `hash` 操作，然后再通过 `hash` 值进一步计算下标，如下图所示：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e7461696e65722f486173684d61702d686173682e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123724.png)

在对 `hashCode()` 计算 hash 时具体实现是这样的：

```
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

可以看到这个方法大概的作用就是：高 16bit 不变，低 16bit 和高 16bit 做了一个异或。

在设计 hash 方法时，因为目前的 table 长度 n 为 2 的幂，而计算下标的时候，是这样实现的(使用 `&` 位操作，而非 `%` 求余)：

```
(n - 1) & hash
```

设计者认为这方法很容易发生碰撞。为什么这么说呢？不妨思考一下，在 n - 1 为 15(0x1111) 时，其实散列真正生效的只是低 4bit 的有效位，当然容易碰撞了。

因此，设计者想了一个顾全大局的方法(综合考虑了速度、作用、质量)，就是把高 16bit 和低 16bit 异或了一下。设计者还解释到因为现在大多数的 hashCode 的分布已经很不错了，就算是发生了碰撞也用 O(logn)的 tree 去做了。仅仅异或一下，既减少了系统的开销，也不会造成的因为高位没有参与下标的计算(table 长度比较小时)，从而引起的碰撞。

如果还是产生了频繁的碰撞，会发生什么问题呢？作者注释说，他们使用树来处理频繁的碰撞(we use trees to handle large sets of collisions in bins)，在 [JEP-180](http://openjdk.java.net/jeps/180) 中，描述了这个问题：

> Improve the performance of java.util.HashMap under high hash-collision conditions by using balanced trees rather than linked lists to store map entries. Implement the same improvement in the LinkedHashMap class.

之前已经提过，在获取 HashMap 的元素时，基本分两步：

1. 首先根据 hashCode()做 hash，然后确定 bucket 的 index；
2. 如果 bucket 的节点的 key 不是我们需要的，则通过 keys.equals()在链中找。

在 JDK8 之前的实现中是用链表解决冲突的，在产生碰撞的情况下，进行 get 时，两步的时间复杂度是 O(1)+O(n)。因此，当碰撞很厉害的时候 n 很大，O(n)的速度显然是影响速度的。

因此在 JDK8 中，利用红黑树替换链表，这样复杂度就变成了 O(1)+O(logn)了，这样在 n 很大的时候，能够比较理想的解决这个问题，在 JDK8：HashMap 的性能提升一文中有性能测试的结果。

#### resize 的实现

当 `put` 时，如果发现目前的 bucket 占用程度已经超过了 Load Factor 所希望的比例，那么就会发生 resize。在 resize 的过程，简单的说就是把 bucket 扩充为 2 倍，之后重新计算 index，把节点再放到新的 bucket 中。

当超过限制的时候会 resize，然而又因为我们使用的是 2 次幂的扩展(指长度扩为原来 2 倍)，所以，元素的位置要么是在原位置，要么是在原位置再移动 2 次幂的位置。

怎么理解呢？例如我们从 16 扩展为 32 时，具体的变化如下所示：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e7461696e65722f486173684d61702d726573697a652d30312e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123740.png)

因此元素在重新计算 hash 之后，因为 n 变为 2 倍，那么 n-1 的 mask 范围在高位多 1bit(红色)，因此新的 index 就会发生这样的变化：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e7461696e65722f486173684d61702d726573697a652d30322e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123749.png)

因此，我们在扩充 HashMap 的时候，不需要重新计算 hash，只需要看看原来的 hash 值新增的那个 bit 是 1 还是 0 就好了，是 0 的话索引没变，是 1 的话索引变成“原索引+oldCap”。可以看看下图为 16 扩充为 32 的 resize 示意图：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f636f6e7461696e65722f486173684d61702d726573697a652d30332e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123757.png)

这个设计确实非常的巧妙，既省去了重新计算 hash 值的时间，而且同时，由于新增的 1bit 是 0 还是 1 可以认为是随机的，因此 resize 的过程，均匀的把之前的冲突的节点分散到新的 bucket 了。

```java
final Node<K,V>[] resize() {
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    int newCap, newThr = 0;
    if (oldCap > 0) {
        // 超过最大值就不再扩充了，就只好随你碰撞去吧
        if (oldCap >= MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return oldTab;
        }
        // 没超过最大值，就扩充为原来的 2 倍
        else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
            newThr = oldThr << 1; // double threshold
    }
    else if (oldThr > 0) // initial capacity was placed in threshold
        newCap = oldThr;
    else {               // zero initial threshold signifies using defaults
        newCap = DEFAULT_INITIAL_CAPACITY;
        newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
    }

    // 计算新的 resize 上限
    if (newThr == 0) {
        float ft = (float)newCap * loadFactor;
        newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                    (int)ft : Integer.MAX_VALUE);
    }
    threshold = newThr;
    @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];
    table = newTab;
    if (oldTab != null) {
        // 把每个 bucket 都移动到新的 buckets 中
        for (int j = 0; j < oldCap; ++j) {
            Node<K,V> e;
            if ((e = oldTab[j]) != null) {
                oldTab[j] = null;
                if (e.next == null)
                    newTab[e.hash & (newCap - 1)] = e;
                else if (e instanceof TreeNode)
                    ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                else { // preserve order
                    Node<K,V> loHead = null, loTail = null;
                    Node<K,V> hiHead = null, hiTail = null;
                    Node<K,V> next;
                    do {
                        next = e.next;
                        // 原索引
                        if ((e.hash & oldCap) == 0) {
                            if (loTail == null)
                                loHead = e;
                            else
                                loTail.next = e;
                            loTail = e;
                        }
                        // 原索引+oldCap
                        else {
                            if (hiTail == null)
                                hiHead = e;
                            else
                                hiTail.next = e;
                            hiTail = e;
                        }
                    } while ((e = next) != null);
                    // 原索引放到bucket里
                    if (loTail != null) {
                        loTail.next = null;
                        newTab[j] = loHead;
                    }
                    // 原索引+oldCap放到bucket里
                    if (hiTail != null) {
                        hiTail.next = null;
                        newTab[j + oldCap] = hiHead;
                    }
                }
            }
        }
    }
    return newTab;
}
```

### 2.3 HashMap默认容量的选择，背后的思考

#### 什么是容量

在Java中，保存数据有两种比较简单的数据结构：数组和链表。数组的特点是：寻址容易，插入和删除困难；而链表的特点是：寻址困难，插入和删除容易。HashMap就是将数组和链表组合在一起，发挥了两者的优势，我们可以将其理解为链表的数组。

在HashMap中，有两个比较容易混淆的关键字段：size和capacity ，这其中capacity就是Map的容量，而size我们称之为Map中的元素个数。

简单打个比方你就更容易理解了：HashMap就是一个“桶”，那么容量（capacity）就是这个桶当前最多可以装多少元素，而元素个数（size）表示这个桶已经装了多少元素。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321163829.webp)

如以下代码：

```
Map<String, String> map = new HashMap<String, String>();

map.put("hollis", "hollischuang");

Class<?> mapType = map.getClass();

Method capacity = mapType.getDeclaredMethod("capacity");

capacity.setAccessible(true);

System.out.println("capacity : " + capacity.invoke(map));

Field size = mapType.getDeclaredField("size");

size.setAccessible(true);

System.out.println("size : " + size.get(map));
```

输出结果：

```
capacity : 16、size : 1
```

上面我们定义了一个新的HashMap，并向其中put了一个元素，然后通过反射的方式打印capacity和size，其容量是16，已经存放的元素个数是1。

通过前面的例子，我们发现了，当我们创建一个HashMap的时候，如果没有指定其容量，那么会得到一个默认容量为16的Map，那么，这个容量是怎么来的呢？又为什么是这个数字呢？

#### 容量与哈希

要想讲清楚这个默认容量的缘由，我们要首先要知道这个容量有什么用？

我们知道，容量就是一个HashMap中"桶"的个数，那么，当我们想要往一个HashMap中put一个元素的时候，需要通过一定的算法计算出应该把他放到哪个桶中，这个过程就叫做哈希（hash），对应的就是HashMap中的hash方法。

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321163855.webp)

我们知道，hash方法的功能是根据Key来定位这个K-V在链表数组中的位置的。也就是hash方法的输入应该是个Object类型的Key，输出应该是个int类型的数组下标。如果让你设计这个方法，你会怎么做？

其实简单，我们只要调用Object对象的hashCode()方法，该方法会返回一个整数，然后用这个数对HashMap的容量进行取模就行了。

如果真的是这么简单的话，那HashMap的容量设置就会简单很多了，但是考虑到效率等问题，HashMap的hash方法实现还是有一定的复杂的。

#### hash的实现

接下来就介绍下HashMap中hash方法的实现原理。

具体实现上，由两个方法int hash(Object k)和int indexFor(int h, int length)来实现。

> hash ：该方法主要是将Object转换成一个整型。
>
> indexFor ：该方法主要是将hash生成的整型转换成链表数组中的下标。

为了聚焦本文的重点，我们只来看一下indexFor方法。我们先来看下Java 7（Java8中虽然没有这样一个单独的方法，但是查询下标的算法也是和Java 7一样的）中该实现细节：

```
static int indexFor(int h, int length) {

    return h & (length-1);

}
```

indexFor方法其实主要是将hashcode换成链表数组中的下标。其中的两个参数h表示元素的hashcode值，length表示HashMap的容量。那么return h & (length-1) 是什么意思呢？

其实，他就是取模。Java之所有使用位运算(&)来代替取模运算(%)，最主要的考虑就是效率。

> 位运算(&)效率要比代替取模运算(%)高很多，主要原因是位运算直接对内存数据进行操作，不需要转成十进制，因此处理速度非常快。

那么，为什么可以使用位运算(&)来实现取模运算(%)呢？这实现的原理如下：

```
X % 2^n = X & (2^n – 1)
```

假设n为3，则2^3 = 8，表示成2进制就是1000。2^3 -1 = 7 ，即0111。

此时X & (2^3 – 1) 就相当于取X的2进制的最后三位数。

从2进制角度来看，X / 8相当于 X >> 3，即把X右移3位，此时得到了X / 8的商，而被移掉的部分(后三位)，则是X % 8，也就是余数。

上面的解释不知道你有没有看懂，没看懂的话其实也没关系，你只需要记住这个技巧就可以了。或者你可以找几个例子试一下。

```
6 % 8 = 6 ，6 & 7 = 6

10 & 8 = 2 ，10 & 7 = 2
```

运算过程如下如：

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321163930.webp)

所以，return h & (length-1);只要保证length的长度是2^n 的话，就可以实现取模运算了。

所以，因为位运算直接对内存数据进行操作，不需要转成十进制，所以位运算要比取模运算的效率更高，所以HashMap在计算元素要存放在数组中的index的时候，使用位运算代替了取模运算。之所以可以做等价代替，前提是要求HashMap的容量一定要是2^n 。

那么，既然是2^n ，为啥一定要是16呢？为什么不能是4、8或者32呢？

关于这个默认容量的选择，JDK并没有给出官方解释，笔者也没有在网上找到关于这个任何有价值的资料。（如果哪位有相关的权威资料或者想法，可以留言交流）

根据作者的推断，这应该就是个经验值（Experience Value），既然一定要设置一个默认的2^n 作为初始值，那么就需要在效率和内存使用上做一个权衡。这个值既不能太小，也不能太大。

太小了就有可能频繁发生扩容，影响效率。太大了又浪费空间，不划算。

所以，16就作为一个经验值被采用了。

> 在JDK 8中，关于默认容量的定义为：static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16 ，其故意把16写成1<<4，就是提醒开发者，这个地方要是2的幂。值得玩味的是：注释中的 **aka 16** 也是1.8中新增的，

那么，接下来我们再来谈谈，HashMap是如何保证其容量一定可以是2^n 的呢？如果用户自己设置了的话又会怎么样呢？

关于这部分，HashMap在两个可能改变其容量的地方都做了兼容处理，分别是指定容量初始化时以及扩容时。

#### 指定容量初始化

当我们通过HashMap(int initialCapacity)设置初始容量的时候，HashMap并不一定会直接采用我们传入的数值，而是经过计算，得到一个新值，目的是提高hash的效率。(1->1、3->4、7->8、9->16)

> 在JDK 1.7和JDK 1.8中，HashMap初始化这个容量的时机不同。JDK 1.8中，在调用HashMap的构造函数定义HashMap的时候，就会进行容量的设定。而在JDK 1.7中，要等到第一次put操作时才进行这一操作。

看一下JDK是如何找到比传入的指定值大的第一个2的幂的：

```
int n = cap - 1;

n |= n >>> 1;

n |= n >>> 2;

n |= n >>> 4;

n |= n >>> 8;

n |= n >>> 16;

return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
```

上面的算法目的挺简单，就是：根据用户传入的容量值（代码中的cap），通过计算，得到第一个比他大的2的幂并返回。

![640 (3)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321163945.webp)

请关注上面的几个例子中，蓝色字体部分的变化情况，或许你会发现些规律。5->8、9->16、19->32、37->64都是主要经过了两个阶段。

> Step 1，5->7
>
> Step 2，7->8
>
> Step 1，9->15
>
> Step 2，15->16
>
> Step 1，19->31
>
> Step 2，31->32

对应到以上代码中，Step1：

```
n |= n >>> 1;

n |= n >>> 2;

n |= n >>> 4;

n |= n >>> 8;

n |= n >>> 16;
```

对应到以上代码中，Step2：

```
return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
```

Step 2 比较简单，就是做一下极限值的判断，然后把Step 1得到的数值+1。

Step 1 怎么理解呢？其实是对一个二进制数依次向右移位，然后与原值取或。其目的对于一个数字的二进制，从第一个不为0的位开始，把后面的所有位都设置成1。

随便拿一个二进制数，套一遍上面的公式就发现其目的了：

```
1100 1100 1100 >>>1 = 0110 0110 0110

1100 1100 1100 | 0110 0110 0110 = 1110 1110 1110

1110 1110 1110 >>>2 = 0011 1011 1011

1110 1110 1110 | 0011 1011 1011 = 1111 1111 1111

1111 1111 1111 >>>4 = 1111 1111 1111

1111 1111 1111 | 1111 1111 1111 = 1111 1111 1111
```

通过几次无符号右移和按位或运算，我们把1100 1100 1100转换成了1111 1111 1111 ，再把1111 1111 1111加1，就得到了1 0000 0000 0000，这就是大于1100 1100 1100的第一个2的幂。

好了，我们现在解释清楚了Step 1和Step 2的代码。就是可以把一个数转化成第一个比他自身大的2的幂。

但是还有一种特殊情况套用以上公式不行，这些数字就是2的幂自身。如果数字4套用公式的话。得到的会是 8，不过其实这个问题也被解决了。

总之，HashMap根据用户传入的初始化容量，利用无符号右移和按位或运算等方式计算出第一个大于该数的2的幂。

#### 扩容

除了初始化的时候会指定HashMap的容量，在进行扩容的时候，其容量也可能会改变。

HashMap有扩容机制，就是当达到扩容条件时会进行扩容。HashMap的扩容条件就是当HashMap中的元素个数（size）超过临界值（threshold）时就会自动扩容。

在HashMap中，threshold = loadFactor * capacity。

loadFactor是装载因子，表示HashMap满的程度，默认值为0.75f，设置成0.75有一个好处，那就是0.75正好是3/4，而capacity又是2的幂。所以，两个数的乘积都是整数。

对于一个默认的HashMap来说，默认情况下，当其size大于12(16*0.75)时就会触发扩容。

下面是HashMap中的扩容方法(resize)中的一段：

```
if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&

                 oldCap >= DEFAULT_INITIAL_CAPACITY)

    newThr = oldThr << 1; // double threshold

}
```

从上面代码可以看出，扩容后的table大小变为原来的两倍，这一步执行之后，就会进行扩容后table的调整，这部分非本文重点，省略。

可见，当HashMap中的元素个数（size）超过临界值（threshold）时就会自动扩容，扩容成原容量的2倍，即从16扩容到32、64、128 …

所以，通过保证初始化容量均为2的幂，并且扩容时也是扩容到之前容量的2倍，所以，保证了HashMap的容量永远都是2的幂。

#### 总结

HashMap作为一种数据结构，元素在put的过程中需要进行hash运算，目的是计算出该元素存放在hashMap中的具体位置。

hash运算的过程其实就是对目标元素的Key进行hashcode，再对Map的容量进行取模，而JDK 的工程师为了提升取模的效率，使用位运算代替了取模运算，这就要求Map的容量一定得是2的幂。

而作为默认容量，太大和太小都不合适，所以16就作为一个比较合适的经验值被采用了。

为了保证任何情况下Map的容量都是2的幂，HashMap在两个地方都做了限制。

首先是，如果用户制定了初始容量，那么HashMap会计算出比该数大的第一个2的幂作为初始容量。

另外，在扩容的时候，也是进行成倍的扩容，即4变成8，8变成16。

本文，通过分析为什么HashMap的默认容量是16，我们深入HashMap的原理，分析了下背后的原理，从代码中我们可以发现，JDK 的工程师把各种位运算运用到了极致，想尽各种办法优化效率。值得我们学习！



## 3. LinkedHashMap 类

### 3.1. LinkedHashMap 要点

**`LinkedHashMap` 通过维护一个保存所有条目（Entry）的双向链表，保证了元素迭代的顺序（即插入顺序）**。

| 关注点                | 结论                           |
| --------------------- | ------------------------------ |
| 是否允许键值对为 null | Key 和 Value 都允许 null       |
| 是否允许重复数据      | Key 重复会覆盖、Value 允许重复 |
| 是否有序              | 按照元素插入顺序存储           |
| 是否线程安全          | 非线程安全                     |

### 3.2. LinkedHashMap 要点

#### LinkedHashMap 数据结构

**`LinkedHashMap` 通过维护一对 `LinkedHashMap.Entry<K,V>` 类型的头尾指针，以双链表形式，保存所有数据**。

学习过数据结构的双链表，就能理解其元素存储以及访问必然是有序的。

```
public class LinkedHashMap<K,V>
    extends HashMap<K,V>
    implements Map<K,V> {

    // 双链表的头指针
    transient LinkedHashMap.Entry<K,V> head;
    // 双链表的尾指针
    transient LinkedHashMap.Entry<K,V> tail;
    // 迭代排序方法：true 表示访问顺序；false 表示插入顺序
    final boolean accessOrder;
}
```

`LinkedHashMap` 继承了 `HashMap` 的 `put` 方法，本身没有实现 `put` 方法。

## 4. TreeMap 类

### 4.1. TreeMap 要点

`TreeMap` 基于红黑树实现。

`TreeMap` 是有序的。它的排序规则是：根据 map 中的 key 的自然语义顺序或提供的比较器（`Comparator`）的自定义比较顺序。

TreeMap 不是线程安全的。

### 4.2. TreeMap 原理

#### put 方法

```java
public V put(K key, V value) {
    Entry<K,V> t = root;
    // 如果根节点为 null，插入第一个节点
    if (t == null) {
        compare(key, key); // type (and possibly null) check

        root = new Entry<>(key, value, null);
        size = 1;
        modCount++;
        return null;
    }
    int cmp;
    Entry<K,V> parent;
    // split comparator and comparable paths
    Comparator<? super K> cpr = comparator;
    // 每个节点的左孩子节点的值小于它；右孩子节点的值大于它
    // 如果有比较器，使用比较器进行比较
    if (cpr != null) {
        do {
            parent = t;
            cmp = cpr.compare(key, t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    // 没有比较器，使用 key 的自然顺序进行比较
    else {
        if (key == null)
            throw new NullPointerException();
        @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
        do {
            parent = t;
            cmp = k.compareTo(t.key);
            if (cmp < 0)
                t = t.left;
            else if (cmp > 0)
                t = t.right;
            else
                return t.setValue(value);
        } while (t != null);
    }
    // 通过上面的遍历未找到 key 值，则新插入节点
    Entry<K,V> e = new Entry<>(key, value, parent);
    if (cmp < 0)
        parent.left = e;
    else
        parent.right = e;
    // 插入后，为了维持红黑树的平衡需要调整
    fixAfterInsertion(e);
    size++;
    modCount++;
    return null;
}
```

#### get 方法

```
public V get(Object key) {
    Entry<K,V> p = getEntry(key);
    return (p==null ? null : p.value);
}

final Entry<K,V> getEntry(Object key) {
    // Offload comparator-based version for sake of performance
    if (comparator != null)
        return getEntryUsingComparator(key);
    if (key == null)
        throw new NullPointerException();
    @SuppressWarnings("unchecked")
        Comparable<? super K> k = (Comparable<? super K>) key;
    Entry<K,V> p = root;
    // 按照二叉树搜索的方式进行搜索，搜到返回
    while (p != null) {
        int cmp = k.compareTo(p.key);
        if (cmp < 0)
            p = p.left;
        else if (cmp > 0)
            p = p.right;
        else
            return p;
    }
    return null;
}
```

### 4.3. remove 方法

```
public V remove(Object key) {
    Entry<K,V> p = getEntry(key);
    if (p == null)
        return null;

    V oldValue = p.value;
    deleteEntry(p);
    return oldValue;
}
private void deleteEntry(Entry<K,V> p) {
    modCount++;
    size--;

    // 如果当前节点有左右孩子节点，使用后继节点替换要删除的节点
    // If strictly internal, copy successor's element to p and then make p
    // point to successor.
    if (p.left != null && p.right != null) {
        Entry<K,V> s = successor(p);
        p.key = s.key;
        p.value = s.value;
        p = s;
    } // p has 2 children

    // Start fixup at replacement node, if it exists.
    Entry<K,V> replacement = (p.left != null ? p.left : p.right);

    if (replacement != null) { // 要删除的节点有一个孩子节点
        // Link replacement to parent
        replacement.parent = p.parent;
        if (p.parent == null)
            root = replacement;
        else if (p == p.parent.left)
            p.parent.left  = replacement;
        else
 D:\codes\zp\java\database\docs\redis\分布式锁.md           p.parent.right = replacement;

        // Null out links so they are OK to use by fixAfterDeletion.
        p.left = p.right = p.parent = null;

        // Fix replacement
        if (p.color == BLACK)
            fixAfterDeletion(replacement);
    } else if (p.parent == null) { // return if we are the only node.
        root = null;
    } else { //  No children. Use self as phantom replacement and unlink.
        if (p.color == BLACK)
            fixAfterDeletion(p);

        if (p.parent != null) {
            if (p == p.parent.left)
                p.parent.left = null;
            else if (p == p.parent.right)
                p.parent.right = null;
            p.parent = null;
        }
    }
}
```

### 4.4. TreeMap 示例

```
public class TreeMapDemo {

    private static final String[] chars = "A B C D E F G H I J K L M N O P Q R S T U V W X Y Z".split(" ");

    public static void main(String[] args) {
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        for (int i = 0; i < chars.length; i++) {
            treeMap.put(i, chars[i]);
        }
        System.out.println(treeMap);
        Integer low = treeMap.firstKey();
        Integer high = treeMap.lastKey();
        System.out.println(low);
        System.out.println(high);
        Iterator<Integer> it = treeMap.keySet().iterator();
        for (int i = 0; i <= 6; i++) {
            if (i == 3) { low = it.next(); }
            if (i == 6) { high = it.next(); } else { it.next(); }
        }
        System.out.println(low);
        System.out.println(high);
        System.out.println(treeMap.subMap(low, high));
        System.out.println(treeMap.headMap(high));
        System.out.println(treeMap.tailMap(low));
    }
}
```

## 5. WeakHashMap

WeakHashMap 的定义如下：

```
public class WeakHashMap<K,V>
    extends AbstractMap<K,V>
    implements Map<K,V> {}
```

WeakHashMap 继承了 AbstractMap，实现了 Map 接口。

和 HashMap 一样，WeakHashMap 也是一个散列表，它存储的内容也是键值对(key-value)映射，而且键和值都可以是 null。

不过 WeakHashMap 的键是**弱键**。在 WeakHashMap 中，当某个键不再被其它对象引用，会被从 WeakHashMap 中被自动移除。更精确地说，对于一个给定的键，其映射的存在并不阻止垃圾回收器对该键的丢弃，这就使该键成为可终止的，被终止，然后被回收。某个键被终止时，它对应的键值对也就从映射中有效地移除了。

这个**弱键**的原理呢？大致上就是，通过 WeakReference 和 ReferenceQueue 实现的。

WeakHashMap 的 key 是**弱键**，即是 WeakReference 类型的；ReferenceQueue 是一个队列，它会保存被 GC 回收的**弱键**。实现步骤是：

1. 新建 WeakHashMap，将**键值对**添加到 WeakHashMap 中。实际上，WeakHashMap 是通过数组 table 保存 Entry(键值对)；每一个 Entry 实际上是一个单向链表，即 Entry 是键值对链表。
2. 当某**弱键**不再被其它对象引用，并被 GC 回收时。在 GC 回收该**弱键**时，这个**弱键**也同时会被添加到 ReferenceQueue(queue)队列中。
3. 当下一次我们需要操作 WeakHashMap 时，会先同步 table 和 queue。table 中保存了全部的键值对，而 queue 中保存被 GC 回收的键值对；同步它们，就是删除 table 中被 GC 回收的键值对。

这就是**弱键**如何被自动从 WeakHashMap 中删除的步骤了。

和 HashMap 一样，WeakHashMap 是不同步的。可以使用 Collections.synchronizedMap 方法来构造同步的 WeakHashMap。



## 6. Hashtable



## 7. ConcurrentHashMap



## 面试题

### 一、HashMap与HashTable有什么不同？

因为HashTable和HashMap很是类似，就跟我们的Vector与ArrayList的关系一样。提供了线程安全的解决方案，所有我们在这里通过区别，就相当与对HashTable进行了源码分析！

> 从存储结构和实现来讲基本上都是相同的。
>
> 它和HashMap的最大的不同是它是线程安全的，另外它不允许key和value为null。
>
> Hashtable是个过时的集合类，不建议在新代码中使用，不需要线程安全的场合可以用HashMap替换，需要线程安全的场合可以用ConcurrentHashMap替换或者Collections的synchronizedMap方法使HashMap具有线程安全的能力。

| 不同点                          | HashMap             | HashTable                                 |
| ------------------------------- | ------------------- | ----------------------------------------- |
| 数据结构                        | 数组+链表+红黑树    | 数组+链表                                 |
| 继承的类不同                    | 继承AbstractMap     | 继承Dictionary                            |
| 是否线程安全                    | 否                  | 是                                        |
| 性能高低                        | 高                  | 低                                        |
| 默认初始化容量                  | 16                  | 11                                        |
| 扩容方式不同                    | 原始容量*2          | 原始容量*2+1                              |
| 底层数组的容量为2的整数次幂     | 要求为2的整数次幂   | 不要求                                    |
| 确认key在数组中的索引的方法不同 | i = (n - 1) & hash; | index = (hash & 0x7FFFFFFF) % tab.length; |
| 遍历方式                        | Iterator(迭代器)    | Iterator(迭代器)和Enumeration(枚举器)     |
| Iterator遍历数组顺序            | 索引从小到大        | 索引从大到小                              |



### 二、为什么要将转换成树形结构的阈值设置为8呢？为什么不将转换成链表结构的阈值也设置为8呢？

1. 当初始阈值为8时，链表的长度达到8的概率变的很小，如果再大概率减小的并不明显
2. 树结构查找的时间复杂度是O(log(n))，而链表的时间复杂度是O(n)，当阈值为8时，`log8 = 3`，相比链表更快，但树结构比链表占用的空间更多，所以这是一种时间和空间的平衡

至于为什么不将转换链表的阈值也设置为8，是因为如果两个值太接近的话，就会造成频繁的转换，导致我们的时间复杂度变高。而在6是经过计算后最合适的数值



### 三、HashMap 为什么不用平衡树，而用红黑树？

这一题应该归类与数据结构了，不过这里同样给出分析

- 红黑树也是一种平衡树，但不是严格平衡，平衡树是左右子树高度差不超过1，红黑树可以是2倍
- 红黑树在插入、删除的时候旋转的概率比平衡树低很多，效率比平衡树高

查找时间复杂度都维持在O(logN)，具体的还望查看红黑树的特性，上面最开始也给了一篇关于红黑树的介绍。



### 四、HashMap在并发下会产生什么问题？有什么替代方案?

HashMap并发下产生问题：由于在发生hash冲突，插入链表的时候，多线程会造成环链，再get的时候变成死循环，Map.size()不准确，数据丢失。

替代方案：

- HashTable: 通过synchronized来修饰，效率低，多线程put的时候，只能有一个线程成功，其他线程都处于阻塞状态

- ConcurrentHashMap：
  
  1.7 采用锁分段技术提高并发访问率
  
  1.8 数据依旧是分段存储，但锁采用了synchronized



### 五、HashMap中的key可以是任何对象或数据类型吗？

- 可以是null，但不能是可变对象，如果是可变对象，对象中的属性改变，则对象的HashCode也相应改变，导致下次无法查找到已存在Map中的数据
- 如果要可变对象当着键，必须保证其HashCode在成员属性改变的时候保持不变



### 六、为什么HashMap是线程不安全的吗？

我们都知道HashMap是线程不安全的，在多线程环境中不建议使用，但是其线程不安全主要体现在什么地方呢，本文将对该问题进行解密。

#### 1.jdk1.7中的HashMap

在jdk1.8中对HashMap做了很多优化，这里先分析在jdk1.7中的问题，相信大家都知道在jdk1.7多线程环境下HashMap容易出现死循环，这里我们先用代码来模拟出现死循环的情况：

```
public class HashMapTest {

    public static void main(String[] args) {
        HashMapThread thread0 = new HashMapThread();
        HashMapThread thread1 = new HashMapThread();
        HashMapThread thread2 = new HashMapThread();
        HashMapThread thread3 = new HashMapThread();
        HashMapThread thread4 = new HashMapThread();
        thread0.start();
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
    }
}

class HashMapThread extends Thread {
    private static AtomicInteger ai = new AtomicInteger();
    private static Map<Integer, Integer> map = new HashMap<>();

    @Override
    public void run() {
        while (ai.get() < 1000000) {
            map.put(ai.get(), ai.get());
            ai.incrementAndGet();
        }
    }
}
```

上述代码比较简单，就是开多个线程不断进行put操作，并且HashMap与AtomicInteger都是全局共享的。在多运行几次该代码后，出现如下死循环情形：

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162150.png)

其中有几次还会出现数组越界的情况：

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162203.png)

这里我们着重分析为什么会出现死循环的情况，通过jps和jstack命名查看死循环情况，结果如下：

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162211.webp)

从堆栈信息中可以看到出现死循环的位置，通过该信息可明确知道死循环发生在HashMap的扩容函数中，根源在transfer函数中，jdk1.7中HashMap的transfer函数如下：

```
void transfer(Entry[] newTable, boolean rehash) {
        int newCapacity = newTable.length;
        for (Entry<K,V> e : table) {
            while(null != e) {
                Entry<K,V> next = e.next;
                if (rehash) {
                    e.hash = null == e.key ? 0 : hash(e.key);
                }
                int i = indexFor(e.hash, newCapacity);
                e.next = newTable[i];
                newTable[i] = e;
                e = next;
            }
        }
    }
```

总结下该函数的主要作用：

**在对table进行扩容到newTable后，需要将原来数据转移到newTable中，注意10-12行代码，这里可以看出在转移元素的过程中，使用的是头插法，也就是链表的顺序会翻转，这里也是形成死循环的关键点。**

下面进行详细分析。

##### 1.1 扩容造成死循环分析过程

前提条件：

这里假设

> 1. hash算法为简单的用key mod链表的大小。
> 2. 最开始hash表size=2，key=3,7,5，则都在table[1]中。
> 3. 然后进行resize，使size变成4。

未resize前的数据结构如下：

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162225.webp)

如果在单线程环境下，最后的结果如下：

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162234.webp)

这里的转移过程，不再进行详述，只要理解transfer函数在做什么，其转移过程以及如何对链表进行反转应该不难。

然后在多线程环境下，假设有两个线程A和B都在进行put操作。线程A在执行到transfer函数中第11行代码处挂起，因为该函数在这里分析的地位非常重要，因此再次贴出来。

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162245.png)

此时线程A中运行结果如下：

![640 (3)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162304.webp)

线程A挂起后，此时线程B正常执行，并完成resize操作，结果如下：

![640 (4)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162312.webp)

这里需要特别注意的点：由于线程B已经执行完毕，根据Java内存模型，现在newTable和table中的Entry都是主存中最新值：`7.next=3，3.next=null`。

此时切换到线程A上，在线程A挂起时内存中值如下：e=3，next=7，newTable[3]=null，代码执行过程如下：

```
newTable[3]=e ----> newTable[3]=3
e=next ----> e=7
```

此时结果如下：

![640 (5)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162348.webp)

继续循环：

```
e=7
next=e.next ----> next=3【从主存中取值】
e.next=newTable[3] ----> e.next=3【从主存中取值】
newTable[3]=e ----> newTable[3]=7
e=next ----> e=3
```

结果如下：

![640 (6)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162359.webp)

再次进行循环：

```
e=3
next=e.next ----> next=null
e.next=newTable[3] ----> e.next=7 即：3.next=7
newTable[3]=e ----> newTable[3]=3
e=next ----> e=null
```

注意此次循环：e.next=7，而在上次循环中7.next=3，出现环形链表，并且此时e=null循环结束。

结果如下：

![640 (7)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162409.webp)

在后续操作中只要涉及轮询hashmap的数据结构，就会在这里发生死循环，造成悲剧。

##### 1.2 扩容造成数据丢失分析过程

遵照上述分析过程，初始时：

![640 (8)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162419.webp)

线程A和线程B进行put操作，同样线程A挂起：

![640 (3)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162433.png)

此时线程A的运行结果如下：

![640 (4)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162444.png)

此时线程B已获得CPU时间片，并完成resize操作：

![640 (9)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162453.webp)

同样注意由于线程B执行完成，newTable和table都为最新值：5.next=null。

此时切换到线程A，在线程A挂起时：e=7，next=5，newTable[3]=null。

执行newtable[i]=e，就将7放在了table[3]的位置，此时next=5。接着进行下一次循环：

```
e=5
next=e.next ----> next=null，从主存中取值
e.next=newTable[1] ----> e.next=5，从主存中取值
newTable[1]=e ----> newTable[1]=5
e=next ----> e=null
```

将5放置在table[1]位置，此时e=null循环结束，3元素丢失，并形成环形链表。并在后续操作hashmap时造成死循环。

![640 (10)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321162507.webp)

#### 2.jdk1.8中HashMap

在jdk1.8中对HashMap进行了优化，在发生hash碰撞，不再采用头插法方式，而是直接插入链表尾部，因此不会出现环形链表的情况，但是在多线程的情况下仍然不安全，这里我们看jdk1.8中HashMap的put操作源码：

```
final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null) // 如果没有hash碰撞则直接插入元素
            tab[i] = newNode(hash, key, value, null);
        else {
            Node<K,V> e; K k;
            if (p.hash == hash &&
                ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            else if (p instanceof TreeNode)
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }
```

这是jdk1.8中HashMap中put操作的主函数， 注意第6行代码，如果没有hash碰撞则会直接插入元素。如果线程A和线程B同时进行put操作，刚好这两条不同的数据hash值一样，并且该位置数据为null，所以这线程A、B都会进入第6行代码中。

假设一种情况，线程A进入后还未进行数据插入时挂起，而线程B正常执行，从而正常插入数据，然后线程A获取CPU时间片，此时线程A不用再进行hash判断了，问题出现：线程A会把线程B插入的数据给覆盖，发生线程不安全。

#### 总结

首先HashMap是线程不安全的，其主要体现：

> 1. 在jdk1.7中，在多线程环境下，扩容时会造成环形链或数据丢失。
> 2. 在jdk1.8中，在多线程环境下，会发生数据覆盖的情况。



### 七、HashMap怎样解决hash冲突？

在Java编程语言中，最基本的结构就是两种，一种是数组，一种是模拟指针(引用)，所有的数据结构都可以用这两个基本结构构造，HashMap也一样。

当程序试图**将多个 key-value 放入 HashMap 中时**，以如下代码片段为例：

```
HashMap<String,Object> m=new HashMap<String,Object>(); 
m.put("a", "rrr1"); 
m.put("b", "tt9"); 
m.put("c", "tt8"); 
m.put("d", "g7"); 
m.put("e", "d6"); 
m.put("f", "d4"); 
m.put("g", "d4"); 
m.put("h", "d3"); 
m.put("i", "d2"); 
m.put("j", "d1"); 
m.put("k", "1"); 
m.put("o", "2"); 
m.put("p", "3"); 
m.put("q", "4"); 
m.put("r", "5"); 
m.put("s", "6"); 
m.put("t", "7"); 
m.put("u", "8"); 
m.put("v", "9");
```

HashMap 采用一种所谓的“Hash 算法”来决定每个元素的存储位置。当程序执行 map.put(String,Obect)方法 时，系统将调用String的 hashCode() 方法得到其 hashCode 值——每个 Java 对象都有 hashCode() 方法，都可通过该方法获得它的 hashCode 值。得到这个对象的 hashCode 值之后，系统会根据该 hashCode 值来决定该元素的存储位置。

源码如下:

```
public V put(K key, V value) {  
        if (key == null)  
            return putForNullKey(value);  
        int hash = hash(key.hashCode());  
        int i = indexFor(hash, table.length);  
        for (Entry<K,V> e = table[i]; e != null; e = e.next) {  
            Object k;  
            //判断当前确定的索引位置是否存在相同hashcode和相同key的元素，如果存在相同的hashcode和相同的key的元素，那么新值覆盖原来的旧值，并返回旧值。  
            //如果存在相同的hashcode，那么他们确定的索引位置就相同，这时判断他们的key是否相同，如果不相同，这时就是产生了hash冲突。  
            //Hash冲突后，那么HashMap的单个bucket里存储的不是一个 Entry，而是一个 Entry 链。  
            //系统只能必须按顺序遍历每个 Entry，直到找到想搜索的 Entry 为止——如果恰好要搜索的 Entry 位于该 Entry 链的最末端（该 Entry 是最早放入该 bucket 中），  
            //那系统必须循环到最后才能找到该元素。  
            if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {  
                V oldValue = e.value;  
                e.value = value;  
                return oldValue;  
            }  
        }  
        modCount++;  
        addEntry(hash, key, value, i);  
        return null;  
    }  
```

上面程序中用到了一个重要的内部接口：Map.Entry，每个 Map.Entry 其实就是一个 key-value 对。从上面程序中可以看出：当系统决定存储 HashMap 中的 key-value 对时，完全没有考虑 Entry 中的 value，仅仅只是根据 key 来计算并决定每个 Entry 的存储位置。

这也说明了前面的结论：我们完全可以**把 Map 集合中的 value 当成 key 的附属，当系统决定了 key 的存储位置之后，value 随之保存在那里即可。**HashMap程序经过我改造，我故意的构造出了hash冲突现象，因为HashMap的初始大小16，但是我在hashmap里面放了超过16个元素，并且我屏蔽了它的resize()方法。不让它去扩容。

这时HashMap的底层数组Entry[] table结构如下:

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321163245.webp)

Hashmap里面的bucket出现了单链表的形式，散列表要解决的一个问题就是散列值的冲突问题，通常是两种方法：**链表法和开放地址法**。

- 链表法就是将相同hash值的对象组织成一个链表放在hash值对应的槽位；
- 开放地址法是通过一个探测算法，当某个槽位已经被占据的情况下继续查找下一个可以使用的槽位。

java.util.HashMap采用的链表法的方式，链表是单向链表。形成单链表的核心代码如下：

```
void addEntry(int hash, K key, V value, int bucketIndex) {  
    Entry<K,V> e = table[bucketIndex];  
    table[bucketIndex] = new Entry<K,V>(hash, key, value, e);  
    if (size++ >= threshold)  
        resize(2 * table.length);  
bsp;  
```

上面方法的代码很简单，但其中包含了一个设计：系统总是将新添加的 Entry 对象放入 table 数组的 bucketIndex 索引处——如果 bucketIndex 索引处已经有了一个 Entry 对象，那新添加的 Entry 对象指向原有的 Entry 对象（产生一个 Entry 链），如果 bucketIndex 索引处没有 Entry 对象，也就是上面程序代码的 e 变量是 null，也就是新放入的 Entry 对象指向 null，也就是没有产生 Entry 链。

HashMap里面没有出现hash冲突时，没有形成单链表时，hashmap查找元素很快,get()方法能够直接定位到元素，但是出现单链表后，单个bucket 里存储的不是一个 Entry，而是一个 Entry 链，系统只能必须按顺序遍历每个 Entry，直到找到想搜索的 Entry 为止——如果恰好要搜索的 Entry 位于该 Entry 链的最末端（该 Entry 是最早放入该 bucket 中），那系统必须循环到最后才能找到该元素。

当创建 HashMap 时，有一个默认的负载因子（load factor），其默认值为 0.75，这是时间和空间成本上一种折衷：增大负载因子可以减少 Hash 表（就是那个 Entry 数组）所占用的内存空间，但会增加查询数据的时间开销，而查询是最频繁的的操作（HashMap 的 get() 与 put() 方法都要用到查询）；减小负载因子会提高数据查询的性能，但会增加 Hash 表所占用的内存空间。

#### 1、HashMap概述

HashMap基于哈希表的 Map 接口的实现。此实现提供所有可选的映射操作，并允许使用 null 值和 null 键。（除了不同步和允许使用 null 之外，HashMap 类与 Hashtable 大致相同。）此类不保证映射的顺序，特别是它不保证该顺序恒久不变。

> 值得注意的是HashMap不是线程安全的，如果想要线程安全的HashMap，可以通过Collections类的静态方法synchronizedMap获得线程安全的HashMap。

```
 Map map = Collections.synchronizedMap(new HashMap());
```

#### 2、HashMap的数据结构

HashMap的底层主要是基于数组和链表来实现的，它之所以有相当快的查询速度主要是因为它是通过计算散列码来决定存储的位置。**HashMap中主要是通过key的hashCode来计算hash值的，只要hashCode相同，计算出来的hash值就一样。如果存储的对象对多了，就有可能不同的对象所算出来的hash值是相同的，这就出现了所谓的hash冲突。**学过数据结构的同学都知道，解决hash冲突的方法有很多，HashMap底层是**通过链表来解决hash冲突**的。

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321163459.webp)

图中，紫色部分即代表哈希表，也称为哈希数组，数组的每个元素都是一个单链表的头节点，链表是用来解决冲突的，如果不同的key映射到了数组的同一位置处，就将其放入单链表中。

我们看看HashMap中Entry类的代码：

```
/** Entry是单向链表。    
     * 它是 “HashMap链式存储法”对应的链表。    
     *它实现了Map.Entry 接口，即实现getKey(), getValue(), setValue(V value), equals(Object o), hashCode()这些函数  
    **/  
    static class Entry<K,V> implements Map.Entry<K,V> {    
        final K key;    
        V value;    
        // 指向下一个节点    
        Entry<K,V> next;    
        final int hash;    

        // 构造函数。    
        // 输入参数包括"哈希值(h)", "键(k)", "值(v)", "下一节点(n)"    
        Entry(int h, K k, V v, Entry<K,V> n) {    
            value = v;    
            next = n;    
            key = k;    
            hash = h;    
        }    

        public final K getKey() {    
            return key;    
        }    

        public final V getValue() {    
            return value;    
        }    

        public final V setValue(V newValue) {    
            V oldValue = value;    
            value = newValue;    
            return oldValue;    
        }    

        // 判断两个Entry是否相等    
        // 若两个Entry的“key”和“value”都相等，则返回true。    
        // 否则，返回false    
        public final boolean equals(Object o) {    
            if (!(o instanceof Map.Entry))    
                return false;    
            Map.Entry e = (Map.Entry)o;    
            Object k1 = getKey();    
            Object k2 = e.getKey();    
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {    
                Object v1 = getValue();    
                Object v2 = e.getValue();    
                if (v1 == v2 || (v1 != null && v1.equals(v2)))    
                    return true;    
            }    
            return false;    
        }    

        // 实现hashCode()    
        public final int hashCode() {    
            return (key==null   ? 0 : key.hashCode()) ^    
                   (value==null ? 0 : value.hashCode());    
        }    

        public final String toString() {    
            return getKey() + "=" + getValue();    
        }    

        // 当向HashMap中添加元素时，绘调用recordAccess()。    
        // 这里不做任何处理    
        void recordAccess(HashMap<K,V> m) {    
        }    

        // 当从HashMap中删除元素时，绘调用recordRemoval()。    
        // 这里不做任何处理    
        void recordRemoval(HashMap<K,V> m) {    
        }    
    }
```

HashMap其实就是一个Entry数组，Entry对象中包含了键和值，其中next也是一个Entry对象，它就是用来处理hash冲突的，形成一个链表。

#### 3、HashMap源码分析

##### 3.1、关键属性

先看看HashMap类中的一些关键属性：

```
transient Entry[] table;//存储元素的实体数组

transient int size;//存放元素的个数

int threshold; //临界值   当实际大小超过临界值时，会进行扩容threshold = 加载因子*容量

final float loadFactor; //加载因子

transient int modCount;//被修改的次数
```

其中loadFactor加载因子是表示Hash表中元素的填满的程度.

若：加载因子越大,填满的元素越多。好处是：空间利用率高了。但：冲突的机会加大了.链表长度会越来越长,查找效率降低。

反之，加载因子越小,填满的元素越少。好处是：冲突的机会减小了,但:空间浪费多了。表中的数据将过于稀疏（很多空间还没用，就开始扩容了）

冲突的机会越大,则查找的成本越高.

因此,**必须在 "冲突的机会"与"空间利用率"之间寻找一种平衡与折衷**. 这种平衡与折衷本质上是数据结构中有名的"时-空"矛盾的平衡与折衷.

如果机器内存足够，并且想要提高查询速度的话可以将加载因子设置小一点；相反如果机器内存紧张，并且对查询速度没有什么要求的话可以将加载因子设置大一点。不过一般我们都不用去设置它，让它取默认值0.75就好了。

##### 3.2、构造方法

下面看看HashMap的几个构造方法：

```
public HashMap(int initialCapacity, float loadFactor) {
        //确保数字合法
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                              initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                              loadFactor);

        // Find a power of 2 >= initialCapacity
        int capacity = 1;   //初始容量
        while (capacity < initialCapacity)   //确保容量为2的n次幂，使capacity为大于initialCapacity的最小的2的n次幂
            capacity <<= 1;

        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        table = new Entry[capacity];
       init();
   }

    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
   }

    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
       init();
    }
```

我们可以看到在构造HashMap的时候如果我们指定了加载因子和初始容量的话就调用第一个构造方法，否则的话就是用默认的。默认初始容量为16，默认加载因子为0.75。我们可以看到上面代码中13-15行，这段代码的作用是确保容量为2的n次幂，使capacity为大于initialCapacity的最小的2的n次幂，至于为什么要把容量设置为2的n次幂，我们等下再看。

重点分析下HashMap中用的最多的两个方法put和get

##### 3.3、存储数据

下面看看HashMap存储数据的过程是怎样的，首先看看HashMap的put方法：

```
public V put(K key, V value) {
     // 若“key为null”，则将该键值对添加到table[0]中。
         if (key == null) 
            return putForNullKey(value);
     // 若“key不为null”，则计算该key的哈希值，然后将其添加到该哈希值对应的链表中。
         int hash = hash(key.hashCode());
     //搜索指定hash值在对应table中的索引
         int i = indexFor(hash, table.length);
     // 循环遍历Entry数组,若“该key”对应的键值对已经存在，则用新的value取代旧的value。然后退出！
         for (Entry<K,V> e = table[i]; e != null; e = e.next) { 
             Object k;
              if (e.hash == hash && ((k = e.key) == key || key.equals(k))) { //如果key相同则覆盖并返回旧值
                  V oldValue = e.value;
                 e.value = value;
                 e.recordAccess(this);
                 return oldValue;
              }
         }
     //修改次数+1
         modCount++;
     //将key-value添加到table[i]处
     addEntry(hash, key, value, i);
     return null;
}
```

上面程序中用到了一个重要的内部接口：Map.Entry，每个 Map.Entry 其实就是一个 key-value 对。从上面程序中可以看出：当系统决定存储 HashMap 中的 key-value 对时，完全没有考虑 Entry 中的 value，仅仅只是根据 key 来计算并决定每个 Entry 的存储位置。

**这也说明了前面的结论：我们完全可以把 Map 集合中的 value 当成 key 的附属，当系统决定了 key 的存储位置之后，value 随之保存在那里即可。**

我们慢慢的来分析这个函数，第2和3行的作用就是处理key值为null的情况，我们看看putForNullKey(value)方法：

```
private V putForNullKey(V value) {
        for (Entry<K,V> e = table[0]; e != null; e = e.next) {
            if (e.key == null) {   //如果有key为null的对象存在，则覆盖掉
                V oldValue = e.value;
                e.value = value;
                e.recordAccess(this);
                return oldValue;
           }
       }
        modCount++;
        addEntry(0, null, value, 0); //如果键为null的话，则hash值为0
        return null;
    }
```

注意：如果key为null的话，hash值为0，对象存储在数组中索引为0的位置。即table[0]

我们再回去看看put方法中第4行，它是通过key的hashCode值计算hash码，下面是计算hash码的函数：

```
//计算hash值的方法 通过键的hashCode来计算
    static int hash(int h) {
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
```

得到hash码之后就会通过hash码去计算出应该存储在数组中的索引，计算索引的函数如下：

```
static int indexFor(int h, int length) { //根据hash值和数组长度算出索引值
         return h & (length-1);  //这里不能随便算取，用hash&(length-1)是有原因的，这样可以确保算出来的索引是在数组大小范围内，不会超出
}
```

这个我们要重点说下，我们一般对哈希表的散列很自然地会想到用hash值对length取模（即除法散列法），Hashtable中也是这样实现的，这种方法基本能保证元素在哈希表中散列的比较均匀，但取模会用到除法运算，效率很低，HashMap中则通过h&(length-1)的方法来代替取模，同样实现了均匀的散列，但效率要高很多，这也是HashMap对Hashtable的一个改进。

接下来，我们分析下为什么哈希表的容量一定要是2的整数次幂。首先，length为2的整数次幂的话，h&(length-1)就相当于对length取模，这样便保证了散列的均匀，同时也提升了效率；其次，length为2的整数次幂的话，为偶数，这样length-1为奇数，奇数的最后一位是1，这样便保证了h&(length-1)的最后一位可能为0，也可能为1（这取决于h的值），即与后的结果可能为偶数，也可能为奇数，这样便可以保证散列的均匀性，而如果length为奇数的话，很明显length-1为偶数，它的最后一位是0，这样h&(length-1)的最后一位肯定为0，即只能为偶数，这样任何hash值都只会被散列到数组的偶数下标位置上，这便浪费了近一半的空间，因此，length取2的整数次幂，是为了使不同hash值发生碰撞的概率较小，这样就能使元素在哈希表中均匀地散列。

这看上去很简单，其实比较有玄机的，我们举个例子来说明：

假设数组长度分别为15和16，优化后的hash码分别为8和9，那么&运算后的结果如下：

```
       h & (table.length-1)                     hash                             table.length-1
       8 & (15-1)：                                 0100                   &              1110                   =                0100
       9 & (15-1)：                                 0101                   &              1110                   =                0100
       -----------------------------------------------------------------------------------------------------------------------
       8 & (16-1)：                                 0100                   &              1111                   =                0100
       9 & (16-1)：                                 0101                   &              1111                   =                0101
```

从上面的例子中可以看出：当它们和15-1（1110）“与”的时候，产生了相同的结果，也就是说它们会定位到数组中的同一个位置上去，这就产生了碰撞，8和9会被放到数组中的同一个位置上形成链表，那么查询的时候就需要遍历这个链 表，得到8或者9，这样就降低了查询的效率。同时，我们也可以发现，当数组长度为15的时候，hash值会与15-1（1110）进行“与”，那么 最后一位永远是0，而0001，0011，0101，1001，1011，0111，1101这几个位置永远都不能存放元素了，空间浪费相当大，更糟的是这种情况中，数组可以使用的位置比数组长度小了很多，这意味着进一步增加了碰撞的几率，减慢了查询的效率！而当数组长度为16时，即为2的n次方时，2n-1得到的二进制数的每个位上的值都为1，这使得在低位上&时，得到的和原hash的低位相同，加之hash(int h)方法对key的hashCode的进一步优化，加入了高位计算，就使得只有相同的hash值的两个值才会被放到数组中的同一个位置上形成链表。

所以说，当数组长度为2的n次幂的时候，不同的key算得得index相同的几率较小，那么数据在数组上分布就比较均匀，也就是说碰撞的几率小，相对的，查询的时候就不用遍历某个位置上的链表，这样查询效率也就较高了。

根据上面 put 方法的源代码可以看出，当程序试图将一个key-value对放入HashMap中时，程序首先根据该 key 的 hashCode() 返回值决定该 Entry 的存储位置：如果两个 Entry 的 key 的 hashCode() 返回值相同，那它们的存储位置相同。如果这两个 Entry 的 key 通过 equals 比较返回 true，新添加 Entry 的 value 将覆盖集合中原有 Entry 的 value，但key不会覆盖。如果这两个 Entry 的 key 通过 equals 比较返回 false，新添加的 Entry 将与集合中原有 Entry 形成 Entry 链，而且新添加的 Entry 位于 Entry 链的头部——具体说明继续看 addEntry() 方法的说明。

```
void addEntry(int hash, K key, V value, int bucketIndex) {
        Entry<K,V> e = table[bucketIndex]; //如果要加入的位置有值，将该位置原先的值设置为新entry的next,也就是新entry链表的下一个节点
       table[bucketIndex] = new Entry<>(hash, key, value, e);
        if (size++ >= threshold) //如果大于临界值就扩容
            resize(2 * table.length); //以2的倍数扩容
    }
```

参数bucketIndex就是indexFor函数计算出来的索引值，第2行代码是取得数组中索引为bucketIndex的Entry对象，第3行就是用hash、key、value构建一个新的Entry对象放到索引为bucketIndex的位置，并且将该位置原先的对象设置为新对象的next构成链表。

第4行和第5行就是判断put后size是否达到了临界值threshold，如果达到了临界值就要进行扩容，HashMap扩容是扩为原来的两倍。

##### 3.4、调整大小

resize()方法如下：

重新调整HashMap的大小，newCapacity是调整后的单位

```
void resize(int newCapacity) {
        Entry[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
       }

        Entry[] newTable = new Entry[newCapacity];
        transfer(newTable);//用来将原先table的元素全部移到newTable里面
        table = newTable;  //再将newTable赋值给table
        threshold = (int)(newCapacity * loadFactor);//重新计算临界值
    }
```

新建了一个HashMap的底层数组，上面代码中第10行为调用transfer方法，将HashMap的全部元素添加到新的HashMap中,并重新计算元素在新的数组中的索引位置

当HashMap中的元素越来越多的时候，hash冲突的几率也就越来越高，因为数组的长度是固定的。所以为了提高查询的效率，就要对HashMap的数组进行扩容，数组扩容这个操作也会出现在ArrayList中，这是一个常用的操作，而在HashMap数组扩容之后，最消耗性能的点就出现了：原数组中的数据必须重新计算其在新数组中的位置，并放进去，这就是resize。

那么HashMap什么时候进行扩容呢？**当HashMap中的元素个数超过数组大小\*loadFactor时，就会进行数组扩容，loadFactor的默认值为0.75，这是一个折中的取值。**也就是说，默认情况下，数组大小为16，那么当HashMap中元素个数超过 `16*0.75=12` 的时候，就把数组的大小扩展为  `2*16=32`，即扩大一倍，然后重新计算每个元素在数组中的位置，扩容是需要进行数组复制的，复制数组是非常消耗性能的操作，所以如果我们已经预知HashMap中元素的个数，那么预设元素的个数能够有效的提高HashMap的性能。

##### 3.5、数据读取

```
public V get(Object key) {   
    if (key == null)   
        return getForNullKey();   
    int hash = hash(key.hashCode());   
    for (Entry<K,V> e = table[indexFor(hash, table.length)];   
        e != null;   
        e = e.next) {   
        Object k;   
        if (e.hash == hash && ((k = e.key) == key || key.equals(k)))   
            return e.value;   
    }   
    return null;   
}
```

有了上面存储时的hash算法作为基础，理解起来这段代码就很容易了。从上面的源代码中可以看出：从HashMap中get元素时，首先计算key的hashCode，找到数组中对应位置的某一元素，然后通过key的equals方法在对应位置的链表中找到需要的元素。

##### 3.6、HashMap的性能参数：

HashMap 包含如下几个构造器：

- **HashMap()：**构建一个初始容量为 16，负载因子为 0.75 的 HashMap。
- **HashMap(int initialCapacity)：**构建一个初始容量为 initialCapacity，负载因子为 0.75 的 HashMap。
- **HashMap(int initialCapacity, float loadFactor)：**以指定初始容量、指定的负载因子创建一个 HashMap。
  HashMap的基础构造器HashMap(int initialCapacity, float loadFactor)带有两个参数，它们是初始容量initialCapacity和加载因子loadFactor。
- **initialCapacity：**HashMap的最大容量，即为底层数组的长度。
- **loadFactor：**负载因子loadFactor定义为：散列表的实际元素数目(n)/ 散列表的容量(m)。

负载因子衡量的是一个散列表的空间的使用程度，负载因子越大表示散列表的装填程度越高，反之愈小。对于使用链表法的散列表来说，查找一个元素的平均时间是O(1+a)，因此如果负载因子越大，对空间的利用更充分，然而后果是查找效率的降低；如果负载因子太小，那么散列表的数据将过于稀疏，对空间造成严重浪费。

HashMap的实现中，通过threshold字段来判断HashMap的最大容量：

```
threshold = (int)(capacity * loadFactor);  
```

结合负载因子的定义公式可知，threshold就是在此loadFactor和capacity对应下允许的最大元素数目，超过这个数目就重新resize，以降低实际的负载因子。默认的的负载因子0.75是对空间和时间效率的一个平衡选择。当容量超出此最大容量时，resize后的HashMap容量是容量的两倍。



### 八、谈谈ConcurrentHashMap是如何保证线程安全的？

#### ConcurrentHashMap的简介

> 我想有基础的同学知道在jdk1.7中是采用Segment + HashEntry + ReentrantLock的方式进行实现的，而1.8中放弃了Segment臃肿的设计，取而代之的是采用Node + CAS + Synchronized来保证并发安全进行实现。

- JDK1.8的实现降低锁的粒度，JDK1.7版本锁的粒度是基于Segment的，包含多个HashEntry，而JDK1.8锁的粒度就是HashEntry（首节点）
- JDK1.8版本的数据结构变得更加简单，使得操作也更加清晰流畅，因为已经使用synchronized来进行同步，所以不需要分段锁的概念，也就不需要Segment这种数据结构了，由于粒度的降低，实现的复杂度也增加了
- JDK1.8使用红黑树来优化链表，基于长度很长的链表的遍历是一个很漫长的过程，而红黑树的遍历效率是很快的，代替一定阈值的链表，这样形成一个最佳拍档

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321170949.png)

### get操作源码

- 首先计算hash值，定位到该table索引位置，如果是首节点符合就返回
- 如果遇到扩容的时候，会调用标志正在扩容节点ForwardingNode的find方法，查找该节点，匹配就返回
- 以上都不符合的话，就往下遍历节点，匹配就返回，否则最后就返回null

```
//会发现源码中没有一处加了锁
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode()); //计算hash
    if ((tab = table) != null && (n = tab.length) > 0 &&
      (e = tabAt(tab, (n - 1) & h)) != null) {//读取首节点的Node元素
        if ((eh = e.hash) == h) { //如果该节点就是首节点就返回
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        //hash值为负值表示正在扩容，这个时候查的是ForwardingNode的find方法来定位到nextTable来
        //eh=-1，说明该节点是一个ForwardingNode，正在迁移，此时调用ForwardingNode的find方法去nextTable里找。
        //eh=-2，说明该节点是一个TreeBin，此时调用TreeBin的find方法遍历红黑树，由于红黑树有可能正在旋转变色，所以find里会有读写锁。
        //eh>=0，说明该节点下挂的是一个链表，直接遍历该链表即可。
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {//既不是首节点也不是ForwardingNode，那就往下遍历
            if (e.hash == h &&
             ((ek = e.key) == key || (ek != null && key.equals(ek))))
                 return e.val;
        }
    }
    return null;
}
```

> get没有加锁的话，ConcurrentHashMap是如何保证读到的数据不是脏数据的呢？

#### volatile登场

对于可见性，Java提供了volatile关键字来保证可见性、有序性。但不保证原子性。

普通的共享变量不能保证可见性，因为普通共享变量被修改之后，什么时候被写入主存是不确定的，当其他线程去读取时，此时内存中可能还是原来的旧值，因此无法保证可见性。

- volatile关键字对于基本类型的修改可以在随后对多个线程的读保持一致，但是对于引用类型如数组，实体bean，仅仅保证引用的可见性，但并不保证引用内容的可见性。。
- 禁止进行指令重排序。

背景：为了提高处理速度，处理器不直接和内存进行通信，而是先将系统内存的数据读到内部缓存（L1，L2或其他）后再进行操作，但操作完不知道何时会写到内存。

- 如果对声明了volatile的变量进行写操作，JVM就会向处理器发送一条指令，将这个变量所在缓存行的数据写回到系统内存。但是，就算写回到内存，如果其他处理器缓存的值还是旧的，再执行计算操作就会有问题。
- 在多处理器下，为了保证各个处理器的缓存是一致的，就会实现缓存一致性协议，当某个CPU在写数据时，如果发现操作的变量是共享变量，则会通知其他CPU告知该变量的缓存行是无效的，因此其他CPU在读取该变量时，发现其无效会重新从主存中加载数据。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321171016.jpg)

**总结下来：**

- 第一：使用volatile关键字会强制将修改的值立即写入主存；

- 第二：使用volatile关键字的话，当线程2进行修改时，会导致线程1的工作内存中缓存变量的缓存行无效（反映到硬件层的话，就是CPU的L1或者L2缓存中对应的缓存行无效）；

- 第三：由于线程1的工作内存中缓存变量的缓存行无效，所以线程1再次读取变量的值时会去主存读取。

#### 是加在数组上的volatile吗?

```
    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     */
    transient volatile Node<K,V>[] table;
```

我们知道volatile可以修饰数组的，只是意思和它表面上看起来的样子不同。举个栗子，volatile int array[10]是指array的地址是volatile的而不是数组元素的值是volatile的.

#### 用volatile修饰的Node

get操作可以无锁是由于Node的元素val和指针next是用volatile修饰的，在多线程环境下线程A修改结点的val或者新增节点的时候是对线程B可见的。

```
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    //可以看到这些都用了volatile修饰
    volatile V val;
    volatile Node<K,V> next;

    Node(int hash, K key, V val, Node<K,V> next) {
        this.hash = hash;
        this.key = key;
        this.val = val;
        this.next = next;
    }

    public final K getKey() { return key; }
    public final V getValue() { return val; }
    public final int hashCode() { return key.hashCode() ^ val.hashCode(); }
    public final String toString(){ return key + "=" + val; }
    public final V setValue(V value) {
        throw new UnsupportedOperationException();
    }

    public final boolean equals(Object o) {
        Object k, v, u; Map.Entry<?,?> e;
        return ((o instanceof Map.Entry) &&
          (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
          (v = e.getValue()) != null &&
          (k == key || k.equals(key)) &&
          (v == (u = val) || v.equals(u))); 
    }

    /**
    * Virtualized support for map.get(); overridden in subclasses.
    */
    Node<K,V> find(int h, Object k) {
        Node<K,V> e = this;
        if (k != null) {
            do {
                K ek;
                if (e.hash == h &&
                 ((ek = e.key) == k || (ek != null && k.equals(ek))))
                   return e;
            } while ((e = e.next) != null);
        }
        return null;
    }
}
```

> 既然volatile修饰数组对get操作没有效果那加在数组上的volatile的目的是什么呢？

其实就是为了使得Node数组在扩容的时候对其他线程具有可见性而加的volatile

#### 总结

- 在1.8中ConcurrentHashMap的get操作全程不需要加锁，这也是它比其他并发集合比如hashtable、用Collections.synchronizedMap()包装的hashmap;安全效率高的原因之一。
- get操作全程不需要加锁是因为Node的成员val是用volatile修饰的和数组用volatile修饰没有关系。
- 数组用volatile修饰主要是保证在数组扩容的时候保证可见性。



### 九、如何决定使用 HashMap 还是 TreeMap？

**介绍**

`TreeMap<K,V>`的Key值是要求实现`java.lang.Comparable`，所以迭代的时候TreeMap默认是按照Key值升序排序的；TreeMap的实现是基于红黑树结构。适用于按自然顺序或自定义顺序遍历键（key）。

`HashMap<K,V>`的Key值实现散列`hashCode()`，分布是散列的、均匀的，不支持排序；数据结构主要是桶(数组)，链表或红黑树。适用于在Map中插入、删除和定位元素。

**结论**

如果你需要得到一个有序的结果时就应该使用TreeMap（因为HashMap中元素的排列顺序是不固定的）。除此之外，由于HashMap有更好的性能，所以大多不需要排序的时候我们会使用HashMap。

**拓展**

**1、HashMap 和 TreeMap 的实现**

**HashMap：** 基于哈希表实现。使用HashMap要求添加的键类明确定义了`hashCode()`和`equals()`[可以重写`hashCode()`和`equals()`]，为了优化HashMap空间的使用，您可以调优初始容量和负载因子。

- HashMap(): 构建一个空的哈希映像
- HashMap(Map m): 构建一个哈希映像，并且添加映像m的所有映射
- HashMap(int initialCapacity): 构建一个拥有特定容量的空的哈希映像
- HashMap(int initialCapacity, float loadFactor): 构建一个拥有特定容量和加载因子的空的哈希映像

**TreeMap：** 基于红黑树实现。TreeMap没有调优选项，因为该树总处于平衡状态。

- TreeMap()：构建一个空的映像树
- TreeMap(Map m): 构建一个映像树，并且添加映像m中所有元素
- TreeMap(Comparator c): 构建一个映像树，并且使用特定的比较器对关键字进行排序
- TreeMap(SortedMap s): 构建一个映像树，添加映像树s中所有映射，并且使用与有序映像s相同的比较器排序

**2、HashMap 和 TreeMap 都是非线程安全**

HashMap继承AbstractMap抽象类，TreeMap继承自SortedMap接口。

**AbstractMap抽象类：** 覆盖了equals()和hashCode()方法以确保两个相等映射返回相同的哈希码。如果两个映射大小相等、包含同样的键且每个键在这两个映射中对应的值都相同，则这两个映射相等。映射的哈希码是映射元素哈希码的总和，其中每个元素是Map.Entry接口的一个实现。因此，不论映射内部顺序如何，两个相等映射会报告相同的哈希码。

**SortedMap接口：** 它用来保持键的有序顺序。SortedMap接口为映像的视图(子集)，包括两个端点提供了访问方法。除了排序是作用于映射的键以外，处理SortedMap和处理SortedSet一样。添加到SortedMap实现类的元素必须实现Comparable接口，否则您必须给它的构造函数提供一个Comparator接口的实现。TreeMap类是它的唯一一个实现。

**3、TreeMap中默认是按照升序进行排序的，如何让他降序**

通过自定义的比较器来实现

定义一个比较器类，实现Comparator接口，重写compare方法，有两个参数，这两个参数通过调用compareTo进行比较，而compareTo默认规则是：

> - 如果参数字符串等于此字符串，则返回 0 值；
> - 如果此字符串小于字符串参数，则返回一个小于 0 的值；
> - 如果此字符串大于字符串参数，则返回一个大于 0 的值。

自定义比较器时，在返回时多添加了个负号，就将比较的结果以相反的形式返回，代码如下：

```
static class MyComparator implements Comparator{
    @Override
    public int compare(Object o1, Object o2) {
        // TODO Auto-generated method stub
        String param1 = (String)o1;
        String param2 = (String)o2;
        return -param1.compareTo(param2);
    }
}
```

之后，通过MyComparator类初始化一个比较器实例，将其作为参数传进TreeMap的构造方法中：

```
MyComparator comparator = new MyComparator();

Map<String,String> map = new TreeMap<String,String>(comparator);
```

这样，我们就可以使用自定义的比较器实现降序了

```
public class MapTest {

    public static void main(String[] args) {
        //初始化自定义比较器
        MyComparator comparator = new MyComparator();
        //初始化一个map集合
        Map<String,String> map = new TreeMap<String,String>(comparator);
        //存入数据
        map.put("a", "a");
        map.put("b", "b");
        map.put("f", "f");
        map.put("d", "d");
        map.put("c", "c");
        map.put("g", "g");
        //遍历输出
        Iterator iterator = map.keySet().iterator();
        while(iterator.hasNext()){
            String key = (String)iterator.next();
            System.out.println(map.get(key));
        }
    }

    static class MyComparator implements Comparator{

        @Override
        public int compare(Object o1, Object o2) {
            // TODO Auto-generated method stub
            String param1 = (String)o1;
            String param2 = (String)o2;
            return -param1.compareTo(param2);
        }
    }
}
```

