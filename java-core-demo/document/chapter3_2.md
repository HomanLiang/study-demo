[toc]



# Java 容器之 List

## 1. List 简介

`List` 是一个接口，它继承于 `Collection` 的接口。它代表着有序的队列。

`AbstractList` 是一个抽象类，它继承于 `AbstractCollection`。`AbstractList` 实现了 `List` 接口中除 `size()`、`get(int location)` 之外的函数。

`AbstractSequentialList` 是一个抽象类，它继承于 `AbstractList`。`AbstractSequentialList` 实现了“链表中，根据 index 索引值操作链表的全部函数”。

### 1.1. ArrayList 和 LinkedList

`ArrayList`、`LinkedList` 是 `List` 最常用的实现。

- `ArrayList` 基于动态数组实现，存在容量限制，当元素数超过最大容量时，会自动扩容；`LinkedList` 基于双向链表实现，不存在容量限制。
- `ArrayList` 随机访问速度较快，随机插入、删除速度较慢；`LinkedList` 随机插入、删除速度较快，随机访问速度较慢。
- `ArrayList` 和 `LinkedList` 都不是线程安全的。

### 1.2. Vector 和 Stack

`Vector` 和 `Stack` 的设计目标是作为线程安全的 `List` 实现，替代 `ArrayList`。

- `Vector` - `Vector` 和 `ArrayList` 类似，也实现了 `List` 接口。但是， `Vector` 中的主要方法都是 `synchronized` 方法，即通过互斥同步方式保证操作的线程安全。
- `Stack` - `Stack` 也是一个同步容器，它的方法也用 `synchronized` 进行了同步，它实际上是继承于 `Vector` 类。

## 2. ArrayList

> ArrayList 从数据结构角度来看，可以视为支持动态扩容的线性表。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303232313134323830332e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123103.png)

### 2.1. ArrayList 要点

`ArrayList` 是一个数组队列，相当于**动态数组**。**`ArrayList` 默认初始容量大小为 `10` ，添加元素时，如果发现容量已满，会自动扩容为原始大小的 1.5 倍**。因此，应该尽量在初始化 `ArrayList` 时，为其指定合适的初始化容量大小，减少扩容操作产生的性能开销。

`ArrayList` 定义：

```
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable
```

从 ArrayList 的定义，不难看出 ArrayList 的一些基本特性：

- `ArrayList` 实现了 `List` 接口，并继承了 `AbstractList`，它支持所有 `List` 的操作。
- `ArrayList` 实现了 `RandomAccess` 接口，**支持随机访问**。`RandomAccess` 是一个标志接口，它意味着“只要实现该接口的 `List` 类，都支持快速随机访问”。在 `ArrayList` 中，我们即可以**通过元素的序号快速获取元素对象**；这就是快速随机访问。
- `ArrayList` 实现了 `Cloneable` 接口，**支持深拷贝**。
- `ArrayList` 实现了 `Serializable` 接口，**支持序列化**，能通过序列化方式传输。
- `ArrayList` 是**非线程安全**的。

### 2.2. ArrayList 原理

#### ArrayList 的数据结构

ArrayList 包含了两个重要的元素：`elementData` 和 `size`。

```
// 默认初始化容量
private static final int DEFAULT_CAPACITY = 10;
// 对象数组
transient Object[] elementData;
// 数组长度
private int size;
```

- `size` - 是动态数组的实际大小。
- `elementData` - 是一个 `Object` 数组，用于保存添加到 `ArrayList` 中的元素。

#### ArrayList 的序列化

`ArrayList` 具有动态扩容特性，因此保存元素的数组不一定都会被使用，那么就没必要全部进行序列化。为此，`ArrayList` 定制了其序列化方式。具体做法是：

- 存储元素的 `Object` 数组（即 `elementData`）使用 `transient` 修饰，使得它可以被 Java 序列化所忽略。
- `ArrayList` 重写了 `writeObject()` 和 `readObject()` 来控制序列化数组中有元素填充那部分内容。

#### ArrayList 构造方法

ArrayList 类实现了三个构造函数：

- 第一个是默认构造方法，ArrayList 会创建一个空数组；
- 第二个是创建 ArrayList 对象时，传入一个初始化值；
- 第三个是传入一个集合类型进行初始化。

当 ArrayList 新增元素时，如果所存储的元素已经超过其当前容量，它会计算容量后再进行动态扩容。数组的动态扩容会导致整个数组进行一次内存复制。因此，**初始化 ArrayList 时，指定数组初始大小，有助于减少数组的扩容次数，从而提高系统性能**。

```
public ArrayList() {
    // 创建一个空数组
	this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}

public ArrayList(int initialCapacity) {
	if (initialCapacity > 0) {
        // 根据初始化值创建数组大小
		this.elementData = new Object[initialCapacity];
	} else if (initialCapacity == 0) {
        // 初始化值为 0 时，创建一个空数组
		this.elementData = EMPTY_ELEMENTDATA;
	} else {
		throw new IllegalArgumentException("Illegal Capacity: "+
										   initialCapacity);
	}
}
```

#### ArrayList 访问元素

`ArrayList` 访问元素的实现主要基于以下关键性源码：

```
// 获取第 index 个元素
public E get(int index) {
    rangeCheck(index);
    return elementData(index);
}

E elementData(int index) {
    return (E) elementData[index];
}
```

实现非常简单，其实就是**通过数组下标访问数组元素，其时间复杂度为 O(1)**，所以很快。

#### ArrayList 添加元素

`ArrayList` 添加元素有两种方法：一种是添加元素到数组末尾，另外一种是添加元素到任意位置。

```
// 添加元素到数组末尾
public boolean add(E e) {
    ensureCapacityInternal(size + 1);  // Increments modCount!!
    elementData[size++] = e;
    return true;
}

// 添加元素到任意位置
public void add(int index, E element) {
	rangeCheckForAdd(index);

	ensureCapacityInternal(size + 1);  // Increments modCount!!
	System.arraycopy(elementData, index, elementData, index + 1,
					 size - index);
	elementData[index] = element;
	size++;
}
```

两种添加元素方法的**不同点**是：

- 添加元素到任意位置，会导致在**该位置后的所有元素都需要重新排列**；
- 而添加元素到数组末尾，在没有发生扩容的前提下，是不会有元素复制排序过程的。

两种添加元素方法的**共同点**是：添加元素时，会先检查容量大小，**如果发现容量不足，会自动扩容为原始大小的 1.5 倍**。

`ArrayList` 添加元素的实现主要基于以下关键性源码：

```
private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }

    ensureExplicitCapacity(minCapacity);
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;

    // overflow-conscious code
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

`ArrayList` 执行添加元素动作（`add` 方法）时，调用 `ensureCapacityInternal` 方法来保证容量足够。

- 如果容量足够时，将数据作为数组中 `size+1` 位置上的元素写入，并将 `size` 自增 1。
- 如果容量不够时，需要使用 `grow` 方法进行扩容数组，新容量的大小为 `oldCapacity + (oldCapacity >> 1)`，也就是旧容量的 1.5 倍。扩容操作实际上是**调用 `Arrays.copyOf()` 把原数组拷贝为一个新数组**，因此最好在创建 `ArrayList` 对象时就指定大概的容量大小，减少扩容操作的次数。

#### ArrayList 删除元素

`ArrayList` 的删除方法和添加元素到任意位置方法有些相似。

`ArrayList` 在每一次有效的删除操作后，都要进行数组的重组，并且删除的元素位置越靠前，数组重组的开销就越大。具体来说，`ArrayList`会**调用 `System.arraycopy()` 将 `index+1` 后面的元素都复制到 `index` 位置上。

```
public E remove(int index) {
    rangeCheck(index);

    modCount++;
    E oldValue = elementData(index);

    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index, numMoved);
    elementData[--size] = null; // clear to let GC do its work

    return oldValue;
}
```

#### ArrayList 的 Fail-Fast

`ArrayList` 使用 `modCount` 来记录结构发生变化的次数。结构发生变化是指添加或者删除至少一个元素的所有操作，或者是调整内部数组的大小，仅仅只是设置元素的值不算结构发生变化。

在进行序列化或者迭代等操作时，需要比较操作前后 `modCount` 是否改变，如果发生改变，`ArrayList` 会抛出 `ConcurrentModificationException`。

```
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException{
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out size as capacity for behavioural compatibility with clone()
    s.writeInt(size);

    // Write out all elements in the proper order.
    for (int i=0; i<size; i++) {
        s.writeObject(elementData[i]);
    }

    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}
```

### 2.3 ArrayList 源码

#### 类图

![image-20210321135432489](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321135432.png)

- 实现了`RandomAccess`接口，可以随机访问
- 实现了`Cloneable`接口，可以克隆
- 实现了`Serializable`接口，可以序列化、反序列化
- 实现了`List`接口，是`List`的实现类之一
- 实现了`Collection`接口，是`Java Collections Framework`成员之一
- 实现了`Iterable`接口，可以使用`for-each`迭代

#### 属性

```Java
// 序列化版本UID
private static final long
        serialVersionUID = 8683452581122892189L;

/**
 * 默认的初始容量
 */
private static final int
        DEFAULT_CAPACITY = 10;

/**
 * 用于空实例的共享空数组实例
 * new ArrayList(0);
 */
private static final Object[]
        EMPTY_ELEMENTDATA = {};

/**
 * 用于提供默认大小的实例的共享空数组实例
 * new ArrayList();
 */
private static final Object[]
        DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

/**
 * 存储ArrayList元素的数组缓冲区
 * ArrayList的容量，是数组的长度
 * 
 * non-private to simplify nested class access
 */
transient Object[] elementData;

/**
 * ArrayList中元素的数量
 */
private int size;
```

> 小朋友，你四否有很多问号？

1. 为什么空实例默认数组有的时候是`EMPTY_ELEMENTDATA`，而又有的时候是`DEFAULTCAPACITY_EMPTY_ELEMENTDATA`
2. 为什么`elementData`要用`transient`修饰？
3. 为什么`elementData`没有被`private`修饰？难道正如注释所写的**non-private to simplify nested class access**

> 带着问题，我们继续往下看。

#### 构造方法

**带初始容量的构造方法**

```Java
/**
 * 带一个初始容量参数的构造方法
 *
 * @param  initialCapacity  初始容量
 * @throws  如果初始容量非法就抛出
 *          IllegalArgumentException
 */
public ArrayList(int initialCapacity) {
    if (initialCapacity > 0) {
        this.elementData =
                new Object[initialCapacity];
    } else if (initialCapacity == 0) {
        this.elementData = EMPTY_ELEMENTDATA;
    } else {
        throw new IllegalArgumentException(
                "Illegal Capacity: "+ initialCapacity);
    }
}
```

- 如果`initialCapacity > 0`，就创建一个新的长度是`initialCapacity`的数组
- 如果`initialCapacity == 0`，就使用EMPTY_ELEMENTDATA
- 其他情况，`initialCapacity`不合法，抛出异常

**无参构造方法**

```Java
/**
 * 无参构造方法 将elementData 赋值为
 *   DEFAULTCAPACITY_EMPTY_ELEMENTDATA
 */
public ArrayList() {
    this.elementData =
            DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
}
```

**带一个集合参数的构造方法**

```Java
/**
 * 带一个集合参数的构造方法
 *
 * @param c 集合，代表集合中的元素会被放到list中
 * @throws 如果集合为空，抛出NullPointerException
 */
public ArrayList(Collection<? extends E> c) {
    elementData = c.toArray();
    // 如果 size != 0
    if ((size = elementData.length) != 0) {
        // c.toArray 可能不正确的，不返回 Object[]
        // https://bugs.openjdk.java.net/browse/JDK-6260652
        if (elementData.getClass() != Object[].class)
            elementData = Arrays.copyOf(
                    elementData, size, Object[].class);
    } else {
        // size == 0
        // 将EMPTY_ELEMENTDATA 赋值给 elementData
        this.elementData = EMPTY_ELEMENTDATA;
    }
}
```

- 使用将集合转换为数组的方法
- 为了防止`c.toArray()`方法不正确的执行，导致没有返回`Object[]`，特殊做了处理
- 如果数组大小等于`0`，则使用 `EMPTY_ELEMENTDATA`

> 那么问题来了，什么情况下`c.toArray()`会不返回`Object[]`呢？

```Java
public static void main(String[] args) {
    List<String> list = new ArrayList<>(Arrays.asList("list"));
    // class java.util.ArrayList
    System.out.println(list.getClass());

    Object[] listArray = list.toArray();
    // class [Ljava.lang.Object;
    System.out.println(listArray.getClass());
    listArray[0] = new Object();

    System.out.println();

    List<String> asList = Arrays.asList("asList");
    // class java.util.Arrays$ArrayList
    System.out.println(asList.getClass());

    Object[] asListArray = asList.toArray();
    // class [Ljava.lang.String;
    System.out.println(asListArray.getClass());
    // java.lang.ArrayStoreException
    asListArray[0] = new Object();
}
```

我们通过这个例子可以看出来，`java.util.ArrayList.toArray()`方法会返回`Object[]`没有问题。而`java.util.Arrays`的私有内部类ArrayList的`toArray()`方法可能不返回`Object[]`。

> 为什么会这样？

我们看ArrayList的`toArray()`方法源码：

```Java
public Object[] toArray() {
    // ArrayLisy中 elementData是这样定义的
    // transient Object[] elementData;
    return Arrays.copyOf(elementData, size);
}
```

使用了`Arrays.copyOf()`方法：

```Java
public static <T> T[] copyOf(T[] original, int newLength) {
    // original.getClass() 是 class [Ljava.lang.Object
    return (T[]) copyOf(original, newLength, original.getClass());
}
```

`copyOf()`的具体实现：

```Java
public static <T,U> T[] copyOf(U[] original, 
          int newLength, Class<? extends T[]> newType) {
    @SuppressWarnings("unchecked")
    /**
     * 如果newType是Object[] copy 数组 类型就是 Object 
     * 否则就是 newType 类型
     */
    T[] copy = ((Object)newType == (Object)Object[].class)
        ? (T[]) new Object[newLength]
        : (T[]) Array.newInstance(newType.getComponentType(), newLength);
    System.arraycopy(original, 0, copy, 0,
                     Math.min(original.length, newLength));
    return copy;
}
```

我们知道ArrayList中`elementData`就是`Object[]`类型，所以ArrayList的`toArray()`方法必然会返回`Object[]`。

我们再看一下`java.util.Arrays`的内部ArrayList源码（截取的部分源码）：

```Java
private static class ArrayList<E> extends AbstractList<E>
        implements RandomAccess, java.io.Serializable {
        
    // 存储元素的数组
    private final E[] a;

    ArrayList(E[] array) {
        // 直接把接收的数组 赋值 给 a
        a = Objects.requireNonNull(array);
    }

    /**
     * obj 为空抛出异常
     * 不为空 返回 obj
     */
    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

    @Override
    public Object[] toArray() {
        // 返回 a 的克隆对象
        return a.clone();
    }

}
```

这是`Arrays.asList()`方法源码

```Java
public static <T> List<T> asList(T... a) {
    return new ArrayList<>(a);
}
```

不难看出来`java.util.Arrays`的内部ArrayList的`toArray()`方法，是构造方法接收什么类型的数组，就返回什么类型的数组。

所以，在我们上面的例子中，实际上返回的是String类型的数组，再将其中的元素赋值成`Object`类型的，自然报错。

我们还是继续看ArrayList吧...

#### 插入方法

**在列表最后添加指定元素**

```Java
/**
 * 在列表最后添加指定元素
 *
 * @param e 要添加的指定元素
 * @return true
 */
public boolean add(E e) {
    // 增加 modCount ！！
    ensureCapacityInternal(size + 1); 
    elementData[size++] = e;
    return true;
}
```

- 在父类`AbstractList`上，定义了`modCount` 属性，用于记录数组修改的次数。

**在指定位置添加指定元素**

```Java
/**
 * 在指定位置添加指定元素
 * 如果指定位置已经有元素，就将该元素和随后的元素移动到右面一位
 *
 * @param index 待插入元素的下标
 * @param element 待插入的元素
 * @throws 可能抛出 IndexOutOfBoundsException
 */
public void add(int index, E element) {
    rangeCheckForAdd(index);


    // 增加 modCount ！！
    ensureCapacityInternal(size + 1);
    System.arraycopy(elementData, index, elementData, index + 1,
                     size - index);
    elementData[index] = element;
    size++;
}
```

**插入方法调用的其他私有方法**

```Java
/**
 * 计算容量
 */
private static int calculateCapacity(
        Object[] elementData, int minCapacity) {

    if (elementData ==
            DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        return Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    return minCapacity;
}

private void ensureCapacityInternal(int minCapacity) {
    ensureExplicitCapacity(
            calculateCapacity(elementData, minCapacity)
    );
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;

    // overflow-conscious code
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}
```

#### 扩容方法

```Java
/**
 * 数组可以分配的最大size
 * 一些虚拟机在数组中预留一些header words
 * 如果尝试分配更大的size，可能导致OutOfMemoryError
 */
private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

/**
 * 增加容量，至少保证比minCapacity大
 * @param minCapacity 期望的最小容量
 */
private void grow(int minCapacity) {
    // 有可能溢出的代码
    int oldCapacity = elementData.length;
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    // minCapacity is usually close to size, so this is a win:
    elementData = Arrays.copyOf(elementData, newCapacity);
}

/**
 * 最大容量返回 Integer.MAX_VALUE
 */
private static int hugeCapacity(int minCapacity) {
    if (minCapacity < 0) // overflow
        throw new OutOfMemoryError();
    return (minCapacity > MAX_ARRAY_SIZE) ?
        Integer.MAX_VALUE :
        MAX_ARRAY_SIZE;
}
```

- 通常情况新容量是原来容量的1.5倍
- 如果原容量的1.5倍比`minCapacity`小，那么就扩容到`minCapacity`
- 特殊情况扩容到`Integer.MAX_VALUE`

> 看完构造方法、添加方法、扩容方法之后，上文第1个问题终于有了答案。原来，`new ArrayList()`会将`elementData` 赋值为 DEFAULTCAPACITY_EMPTY_ELEMENTDATA，`new ArrayList(0)`会将`elementData` 赋值为 EMPTY_ELEMENTDATA，EMPTY_ELEMENTDATA添加元素会扩容到容量为`1`，而DEFAULTCAPACITY_EMPTY_ELEMENTDATA扩容之后容量为`10`。

通过反射我们可以验证这一想法。如下：

```Java
public static void main(String[] args) {
    printDefaultCapacityList();
    printEmptyCapacityList();
}

public static void printDefaultCapacityList() {
    ArrayList defaultCapacity = new ArrayList();
    System.out.println(
            "default 初始化长度：" + getCapacity(defaultCapacity));

    defaultCapacity.add(1);
    System.out.println(
            "default add 之后 长度：" + getCapacity(defaultCapacity));
}

public static void printEmptyCapacityList() {
    ArrayList emptyCapacity = new ArrayList(0);
    System.out.println(
            "empty 初始化长度：" + getCapacity(emptyCapacity));

    emptyCapacity.add(1);
    System.out.println(
            "empty add 之后 长度：" + getCapacity(emptyCapacity));
}

public static int getCapacity(ArrayList<?> arrayList) {
    Class<ArrayList> arrayListClass = ArrayList.class;
    try {
        // 获取 elementData 字段
        Field field = arrayListClass.getDeclaredField("elementData");
        // 开启访问权限
        field.setAccessible(true);
        // 把示例传入get，获取实例字段elementData的值
        Object[] objects = (Object[]) field.get(arrayList);
        //返回当前ArrayList实例的容量值
        return objects.length;
    } catch (Exception e) {
        e.printStackTrace();
        return -1;
    }
}
```

#### 移除方法

**移除指定下标元素方法**

```Java
/**
 * 移除列表中指定下标位置的元素
 * 将所有的后续元素，向左移动
 *
 * @param 要移除的指定下标
 * @return 返回被移除的元素
 * @throws 下标越界会抛出IndexOutOfBoundsException
 */
public E remove(int index) {
    rangeCheck(index);

    modCount++;
    E oldValue = elementData(index);

    int numMoved = size - index - 1;
    if (numMoved > 0)
            System.arraycopy(elementData, 
                    index+1, elementData, index,  numMoved);
    // 将引用置空，让GC回收
    elementData[--size] = null;

    return oldValue;
}
```

**移除指定元素方法**

```Java
/**
 * 移除第一个在列表中出现的指定元素
 * 如果存在，移除返回true
 * 否则，返回false
 *
 * @param o 指定元素
 */
public boolean remove(Object o) {
    if (o == null) {
        for (int index = 0; index < size; index++)
            if (elementData[index] == null) {
                fastRemove(index);
                return true;
            }
    } else {
        for (int index = 0; index < size; index++)
            if (o.equals(elementData[index])) {
                fastRemove(index);
                return true;
            }
    }
    return false;
}
```

> 移除方法名字、参数的个数都一样，使用的时候要注意。

**私有移除方法**

```Java
/*
 * 私有的 移除 方法 跳过边界检查且不返回移除的元素
 */
private void fastRemove(int index) {
    modCount++;
    int numMoved = size - index - 1;
    if (numMoved > 0)
        System.arraycopy(elementData, index+1, elementData, index,
                         numMoved);
    // 将引用置空，让GC回收
    elementData[--size] = null;
}
```

#### 查找方法

**查找指定元素的所在位置**

```Java
/**
 * 返回指定元素第一次出现的下标
 * 如果不存在该元素，返回 -1
 * 如果 o ==null 会特殊处理
 */
public int indexOf(Object o) {
    if (o == null) {
        for (int i = 0; i < size; i++)
            if (elementData[i]==null)
                return i;
    } else {
        for (int i = 0; i < size; i++)
            if (o.equals(elementData[i]))
                return i;
    }
    return -1;
}
```

**查找指定位置的元素**

```Java
/**
 * 返回指定位置的元素
 *
 * @param  index 指定元素的位置 
 * @throws index越界会抛出IndexOutOfBoundsException
 */
public E get(int index) {
    rangeCheck(index);

    return elementData(index);
}
```

> 该方法直接返回`elementData`数组指定下标的元素，效率还是很高的。所以ArrayList，`for`循环遍历效率也是很高的。

#### 序列化方法

```Java
/**
 * 将ArrayLisy实例的状态保存到一个流里面
 */
private void writeObject(java.io.ObjectOutputStream s)
    throws java.io.IOException{
    // Write out element count, and any hidden stuff
    int expectedModCount = modCount;
    s.defaultWriteObject();

    // Write out size as capacity for behavioural compatibility with clone()
    s.writeInt(size);

    // 按照顺序写入所有的元素
    for (int i=0; i<size; i++) {
        s.writeObject(elementData[i]);
    }

    if (modCount != expectedModCount) {
        throw new ConcurrentModificationException();
    }
}
```

#### 反序列化方法

```Java
/**
 * 根据一个流(参数)重新生成一个ArrayList
 */
private void readObject(java.io.ObjectInputStream s)
    throws java.io.IOException, ClassNotFoundException {
    elementData = EMPTY_ELEMENTDATA;

    // Read in size, and any hidden stuff
    s.defaultReadObject();

    // Read in capacity
    s.readInt();

    if (size > 0) {
        // be like clone(), allocate array based upon size not capacity
        ensureCapacityInternal(size);

        Object[] a = elementData;
        // Read in all elements in the proper order.
        for (int i=0; i<size; i++) {
            a[i] = s.readObject();
        }
    }
}
```

> 看完序列化，反序列化方法，我们终于又能回答开篇的第二个问题了。`elementData`之所以用`transient`修饰，是因为JDK不想将整个`elementData`都序列化或者反序列化，而只是将`size`和实际存储的元素序列化或反序列化，从而节省空间和时间。

#### 创建子数组

```Java
public List<E> subList(int fromIndex, int toIndex) {
    subListRangeCheck(fromIndex, toIndex, size);
    return new SubList(this, 0, fromIndex, toIndex);
}
```

我们看一下简短版的`SubList`：

```Java
private class SubList extends AbstractList<E> implements RandomAccess {
    private final AbstractList<E> parent;
    private final int parentOffset;
    private final int offset;
    int size;

    SubList(AbstractList<E> parent,
            int offset, int fromIndex, int toIndex) {
        this.parent = parent;
        this.parentOffset = fromIndex;
        this.offset = offset + fromIndex;
        this.size = toIndex - fromIndex;
        this.modCount = ArrayList.this.modCount;
    }

    public E set(int index, E e) {
        rangeCheck(index);
        checkForComodification();
        E oldValue = ArrayList.this.elementData(offset + index);
        ArrayList.this.elementData[offset + index] = e;
        return oldValue;
    }
    
    // 省略代码...
}
```

- SubList的set()方法，**是直接修改ArrayList**中`elementData`数组的，使用中应该注意
- SubList是没有实现`Serializable`接口的，**是不能序列化的**

#### 迭代器

**创建迭代器方法**

```Java
public Iterator<E> iterator() {
    return new Itr();
}
```

**Itr属性**

```Java
// 下一个要返回的元素的下标
int cursor;
// 最后一个要返回元素的下标 没有元素返回 -1
int lastRet = -1;
// 期望的 modCount
int expectedModCount = modCount;
```

**Itr的hasNext() 方法**

```Java
public boolean hasNext() {
    return cursor != size;
}
```

**Itr的next()方法**

```Java
public E next() {
    checkForComodification();
    int i = cursor;
    if (i >= size)
        throw new NoSuchElementException();
    Object[] elementData = ArrayList.this.elementData;
    if (i >= elementData.length)
        throw new ConcurrentModificationException();
    cursor = i + 1;
    return (E) elementData[lastRet = i];
}

final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

> 在迭代的时候，会校验`modCount`是否等于`expectedModCount`，不等于就会抛出著名的`ConcurrentModificationException`异常。什么时候会抛出`ConcurrentModificationException`？

```Java
public static void main(String[] args) {
    ArrayList arrayList = new ArrayList();
    for (int i = 0; i < 10; i++) {
        arrayList.add(i);
    }
    remove(arrayList);
    System.out.println(arrayList);
}

public static void remove(ArrayList<Integer> list) {
    Iterator<Integer> iterator = list.iterator();
    while (iterator.hasNext()) {
        Integer number = iterator.next();
        if (number % 2 == 0) {
            // 抛出ConcurrentModificationException异常
            list.remove(number);
        }
    }
}
```

> 那怎么写才能不抛出`ConcurrentModificationException`？很简单，将`list.remove(number);`换成`iterator.remove();`即可。why？请看Itr的`remove()`源码...

**Itr的remove()方法**

```Java
public void remove() {
    if (lastRet < 0)
        throw new IllegalStateException();
    checkForComodification();

    try {
        ArrayList.this.remove(lastRet);
        cursor = lastRet;
        lastRet = -1;
        // 移除之后将modCount 重新赋值给 expectedModCount
        expectedModCount = modCount;
    } catch (IndexOutOfBoundsException ex) {
        throw new ConcurrentModificationException();
    }
}
```

原因就是因为Itr的`remove()`方法，移除之后将`modCount`重新赋值给 `expectedModCount`。这就是源码，不管单线程还是多线程，只要违反了规则，就会抛异常。

> 源码看的差不多了，开篇的问题却还剩一个！到底为什么`elementData`没有用`private`修饰呢？

我们知道的，`private`修饰的变量，内部类也是可以访问到的。难道注释中`non-private to simplify nested class access`的这句话有毛病？

当我们看表面看不到什么东西的时候，不妨看一下底层。

测试类代码：
![image-20210321135734108](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321135734.png)

一顿`javac`、`javap`之后（使用JDK8）：
![image-20210321135753328](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321135753.png)

再一顿`javac`、`javap`之后（使用JDK11）：
![image-20210321135813507](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321135813.png)

虽然字节码指令我还看不太懂，但是我能品出来，注释是没毛病的，`private`修饰的确会影响内部类的访问。

#### ArrayList类注释翻译

类注释还是要看的，能给我们一个整体的了解这个类。我将ArrayList的类注释大概翻译整理了一下：

- ArrayList是实现`List`接口的可自动扩容的数组。实现了所有的`List`操作，允许所有的元素，包括`null`值。
- ArrayList大致和Vector相同，除了ArrayList是非同步的。
- `size` `isEmpty` `get` `set` `iterator` 和 `listIterator` 方法时间复杂度是`O(1)`，常量时间。其他方法是`O(n)`，线性时间。
- 每一个ArrayList实例都有一个`capacity`（容量）。`capacity`是用于存储列表中元素的数组的大小。`capacity`至少和列表的大小一样大。
- 如果多个线程同时访问ArrayList的实例，并且至少一个线程会修改，必须在外部保证ArrayList的同步。修改包括添加删除扩容等操作，仅仅设置值不包括。这种场景可以用其他的一些封装好的同步的`list`。如果不存在这样的`Object`，ArrayList应该用`Collections.synchronizedList`包装起来最好在创建的时候就包装起来，来保证同步访问。
- `iterator()`和`listIterator(int)`方法是`fail-fast`的，如果在迭代器创建之后，列表进行结构化修改，迭代器会抛出`ConcurrentModificationException`。
- 面对并发修改，迭代器快速失败、清理，而不是在未知的时间不确定的情况下冒险。请注意，快速失败行为不能被保证。通常来讲，不能同步进行的并发修改几乎不可能做任何保证。因此，写依赖这个异常的程序的代码是错误的，快速失败行为应该仅仅用于防止`bug`。

#### 总结

- ArrayList底层的数据结构是数组
- ArrayList可以自动扩容，不传初始容量或者初始容量是`0`，都会初始化一个空数组，但是如果添加元素，会自动进行扩容，所以，创建ArrayList的时候，给初始容量是必要的
- `Arrays.asList()`方法返回的是的`Arrays`内部的ArrayList，用的时候需要注意
- `subList()`返回内部类，不能序列化，和ArrayList共用同一个数组
- 迭代删除要用，迭代器的`remove`方法，或者可以用倒序的`for`循环
- ArrayList重写了序列化、反序列化方法，避免序列化、反序列化全部数组，浪费时间和空间
- `elementData`不使用`private`修饰，可以简化内部类的访问

## 3. LinkedList

> LinkedList 从数据结构角度来看，可以视为双链表。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303232313134323533352e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321123305.png)

### 3.1. LinkedList 要点

`LinkedList` 基于双链表结构实现。由于是双链表，所以**顺序访问会非常高效，而随机访问效率比较低。**

`LinkedList` 定义：

```
public class LinkedList<E>
    extends AbstractSequentialList<E>
    implements List<E>, Deque<E>, Cloneable, java.io.Serializable
```

从 `LinkedList` 的定义，可以得出 `LinkedList` 的一些基本特性：

- `LinkedList` 实现了 `List` 接口，并继承了 `AbstractSequentialList` ，它支持所有 `List` 的操作。
- `LinkedList` 实现了 `Deque` 接口，也可以被当作队列（`Queue`）或双端队列（`Deque`）进行操作，此外，也可以用来实现栈。
- `LinkedList` 实现了 `Cloneable` 接口，**支持深拷贝**。
- `LinkedList` 实现了 `Serializable` 接口，**支持序列化**。
- `LinkedList` 是**非线程安全**的。

### 3.2. LinkedList 原理

#### LinkedList 的数据结构

**`LinkedList` 内部维护了一个双链表**。

`LinkedList` 通过 `Node` 类型的头尾指针（`first` 和 `last`）来访问数据。

```
// 链表长度
transient int size = 0;
// 链表头节点
transient Node<E> first;
// 链表尾节点
transient Node<E> last;
```

- `size` - **表示双链表中节点的个数，初始为 0**。
- `first` 和 `last` - **分别是双链表的头节点和尾节点**。

`Node` 是 `LinkedList` 的内部类，它表示链表中的元素实例。Node 中包含三个元素：

- `prev` 是该节点的上一个节点；
- `next` 是该节点的下一个节点；
- `item` 是该节点所包含的值。

```
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
    ...
}
```

#### LinkedList 的序列化

`LinkedList` 与 `ArrayList` 一样也定制了自身的序列化方式。具体做法是：

- 将 `size` （双链表容量大小）、`first` 和`last` （双链表的头尾节点）修饰为 `transient`，使得它们可以被 Java 序列化所忽略。
- 重写了 `writeObject()` 和 `readObject()` 来控制序列化时，只处理双链表中能被头节点链式引用的节点元素。

#### LinkedList 访问元素

`LinkedList` 访问元素的实现主要基于以下关键性源码：

```
public E get(int index) {
	checkElementIndex(index);
	return node(index).item;
}

Node<E> node(int index) {
    // assert isElementIndex(index);

    if (index < (size >> 1)) {
        Node<E> x = first;
        for (int i = 0; i < index; i++)
            x = x.next;
        return x;
    } else {
        Node<E> x = last;
        for (int i = size - 1; i > index; i--)
            x = x.prev;
        return x;
    }
}
```

获取 `LinkedList` 第 index 个元素的算法是：

- 判断 index 在链表前半部分，还是后半部分。
- 如果是前半部分，从头节点开始查找；如果是后半部分，从尾结点开始查找。

`LinkedList` 这种访问元素的性能是 `O(N)` 级别的（极端情况下，扫描 N/2 个元素）；相比于 `ArrayList` 的 `O(1)`，显然要慢不少。

**推荐使用迭代器遍历 `LinkedList` ，不要使用传统的 `for` 循环**。注：foreach 语法会被编译器转换成迭代器遍历，但是它的遍历过程中不允许修改 `List` 长度，即不能进行增删操作。

#### LinkedList 添加元素

`LinkedList` 有多种添加元素方法：

- `add(E e)`：默认添加元素方法（插入尾部）
- `add(int index, E element)`：添加元素到任意位置
- `addFirst(E e)`：在头部添加元素
- `addLast(E e)`：在尾部添加元素

```
public boolean add(E e) {
	linkLast(e);
	return true;
}

public void add(int index, E element) {
	checkPositionIndex(index);

	if (index == size)
		linkLast(element);
	else
		linkBefore(element, node(index));
}

public void addFirst(E e) {
	linkFirst(e);
}

public void addLast(E e) {
	linkLast(e);
}
```

`LinkedList` 添加元素的实现主要基于以下关键性源码：

```
private void linkFirst(E e) {
	final Node<E> f = first;
	final Node<E> newNode = new Node<>(null, e, f);
	first = newNode;
	if (f == null)
		last = newNode;
	else
		f.prev = newNode;
	size++;
	modCount++;
}

void linkLast(E e) {
	final Node<E> l = last;
	final Node<E> newNode = new Node<>(l, e, null);
	last = newNode;
	if (l == null)
		first = newNode;
	else
		l.next = newNode;
	size++;
	modCount++;
}

void linkBefore(E e, Node<E> succ) {
	// assert succ != null;
	final Node<E> pred = succ.prev;
	final Node<E> newNode = new Node<>(pred, e, succ);
	succ.prev = newNode;
	if (pred == null)
		first = newNode;
	else
		pred.next = newNode;
	size++;
	modCount++;
}
```

算法如下：

- 将新添加的数据包装为 `Node`；
- 如果往头部添加元素，将头指针 `first` 指向新的 `Node`，之前的 `first` 对象的 `prev` 指向新的 `Node`。
- 如果是向尾部添加元素，则将尾指针 `last` 指向新的 `Node`，之前的 `last` 对象的 `next` 指向新的 `Node`。

#### LinkedList 删除元素

`LinkedList` 删除元素的实现主要基于以下关键性源码：

```
public boolean remove(Object o) {
    if (o == null) {
        // 遍历找到要删除的元素节点
        for (Node<E> x = first; x != null; x = x.next) {
            if (x.item == null) {
                unlink(x);
                return true;
            }
        }
    } else {
        // 遍历找到要删除的元素节点
        for (Node<E> x = first; x != null; x = x.next) {
            if (o.equals(x.item)) {
                unlink(x);
                return true;
            }
        }
    }
    return false;
}

E unlink(Node<E> x) {
    // assert x != null;
    final E element = x.item;
    final Node<E> next = x.next;
    final Node<E> prev = x.prev;

    if (prev == null) {
        first = next;
    } else {
        prev.next = next;
        x.prev = null;
    }

    if (next == null) {
        last = prev;
    } else {
        next.prev = prev;
        x.next = null;
    }

    x.item = null;
    size--;
    modCount++;
    return element;
}
```

算法说明：

- 遍历找到要删除的元素节点，然后调用 `unlink` 方法删除节点；

- `unlink` 删除节点的方法：

  - 如果当前节点有前驱节点，则让前驱节点指向当前节点的下一个节点；否则，让双链表头指针指向下一个节点。
  - 如果当前节点有后继节点，则让后继节点指向当前节点的前一个节点；否则，让双链表尾指针指向上一个节点。

### 3.3 LinkedList 源码

#### 准备

**LinkedList是基于双向链表数据结构实现的Java集合**(jdk1.8以前基于双向循环链表)，在阅读源码之前，有必要简单了解一下链表。

先了解一下链表的概念：链表是由一系列非连续的节点组成的存储结构，简单分下类的话，链表又分为单向链表和双向链表，而单向/双向链表又可以分为循环链表和非循环链表。

- 单向链表：单向链表就是通过每个结点的指针指向下一个结点从而链接起来的结构，最后一个节点的next指向null。
- 单向循环链表：单向循环链表和单向列表的不同是，最后一个节点的next不是指向null，而是指向head节点，形成一个“环”。
- 双向链表：向链表是包含两个指针的，pre指向前一个节点，next指向后一个节点，但是第一个节点head的pre指向null，最后一个节点的tail指向null。

![image-20210321140213866](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140214.png)

- 双向循环链表：向循环链表和双向链表的不同在于，第一个节点的pre指向最后一个节点，最后一个节点的next指向第一个节点，也形成一个“环”。

![image-20210321140234338](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140234.png)

#### LinkedList继承体系

![image-20210321140303687](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140303.png)

通过类图可以看到，LinkedList不仅实现了List接口，而且实现了现了Queue和Deque接口，所以它既能作为List使用，也能作为双端队列使用，也可以作为栈使用。

#### 源码分析

**节点类**

LinkedList有一个静态内部类，我们看到在双链表中每个节点有前趋、后继、数据域，节点类实现了这个结构。
![image-20210321140329412](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140329.png)

```java
    private static class Node<E> {
       //数据域
        E item;
        //后继
        Node<E> next;
        //后继
        Node<E> prev;

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
```

**属性**

看一下LinkedList的主要属性。first和last对应了双链表的头结点和尾结点。

![image-20210321140352716](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140352.png)

```java
//元素个数
transient int size = 0;
//头结点
transient Node<E> first;
//尾结点
transient Node<E> last;
```

**构造函数**

```java
//无参
public LinkedList() {
}

//从其它集合中构造
public LinkedList(Collection<? extends E> c) {
    this();
    addAll(c);
}
```

**获取元素**

双向链表的灵活处就是**链表中的一个元素结构就可以向左或者向右开始遍历查找需要的元素结构**。因此对于一个有序链表，查询的效率比单链表高一些。因为，我们可以记录上次查找的位置 p，每次查询时，根据要查找的值与 p 的大小关系，决定是往前还是往后查找，所以平均只需要查找一半的数据。

链表查询示意图如下：
![20200819213217886](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140426.gif)

```java
   //根据索引获取数据
    public E get(int index) {
        //越界判断
        checkElementIndex(index);
        //根据index获取节点
        return node(index).item;
    }

   //根据索引获取节点
    Node<E> node(int index) {
        // 因为是双链表
       // 所以根据index是在前半段还是后半段决定从前遍历还是从后遍历
        // 这样index在后半段的时候可以少遍历一半的元素
        if (index < (size >> 1)) {
            // 如果是在前半段
            // 就从后往前遍历
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
           //如果是在前半段
           //就从前往后遍历
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
```

**添加元素**

- 头插法

```java
    private void linkFirst(E e) {
       // 首节点
        final Node<E> f = first;
        // 创建新节点，新节点的next是首节点
        final Node<E> newNode = new Node<>(null, e, f);
        first = newNode;
       // 判断链表是不是为空
       // 如果是就把last也置为新节点
       // 否则把原首节点的prev指针置为新节点
        if (f == null)
            last = newNode;
        else
            f.prev = newNode;
        //元素个数加1    
        size++;
         // 修改次数 +1，用于 fail-fast 处理
        modCount++;
    }
    

   public void addFirst(E e) {
    linkFirst(e);
   }
```

- 尾插法

```java
    void linkLast(E e) {
        //尾结点
        final Node<E> l = last;
        //新节点
        final Node<E> newNode = new Node<>(l, e, null);
        //尾结点置为新节点
        last = newNode;
        //如果链表为空，头结点指向尾结点
        if (l == null)
            first = newNode;
        else
            l.next = newNode;
        //元素个数加1    
        size++;
        // 修改次数 +1，用于 fail-fast 处理
        modCount++;
    }

  public void addLast(E e) {
    linkLast(e);
  }

  public boolean add(E e) {
        linkLast(e);
        return true;
    }
```

在链表头部和尾部插入时间复杂度都是O(1)，头插法和尾插法的示意图如下：

![image-20210321140504427](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140504.png)

- 中间插入法：中间插入需要找到插入位置节点，改变该节点的前趋和该节点前趋节点的后继

![image-20210321140521059](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140521.png)

```java
     //根据索引插入节点
     public void add(int index, E element) {
        //判断是否越界
        checkPositionIndex(index);
        //未插入
        if (index == size)
            linkLast(element);
        else
            //找到索引位置节点，在该节点前插入新节点
            linkBefore(element, node(index));
    }

   

   // 在节点succ之前添加元素
    void linkBefore(E e, Node<E> succ) {
        //节点succ的前趋节点
        final Node<E> pred = succ.prev;
        //新节点
        final Node<E> newNode = new Node<>(pred, e, succ);
        //改变节点succ的前趋指向
        succ.prev = newNode;
         // 判断前置节点是否为空
        // 如果为空，说明是第一个添加的元素，头结点重新赋值
       // 否则修改前置节点的next为新节点
        if (pred == null)
            first = newNode;
        else
            pred.next = newNode;
        //元素个数加1     
        size++;
        // 修改次数 +1，用于 fail-fast 处理
        modCount++;
    }
```

在中间添加元素效率低一些，首先要先找到插入位置的节点，再修改前后节点的指针，时间复杂度为O(n)。

**删除元素**

双链表中删除元素只需要改变前趋和后继的指向。

- 删除头节点

```java
   //删除头节点
    public E removeFirst() {
        final Node<E> f = first;
        //如果链表为空，抛出异常
        if (f == null)
            throw new NoSuchElementException();
         // 删除首节点   
        return unlinkFirst(f);
    }
    
    // 删除头节点
     private E unlinkFirst(Node<E> f) {
        // 头结点
        final E element = f.item;
        //头结点后继节点
        final Node<E> next = f.next;
        //头结点数据域后继置空，帮助GC
        f.item = null;
        f.next = null; 
        //头结点置为后继节点
        first = next;
       // 如果只有一个元素，删除了，把last也置为空
       // 否则把next的前趋置为空
        if (next == null)
            last = null;
        else
            next.prev = null;
        size--;
        modCount++;
        //返回删除的节点
        return element;
    }
```

- 删除尾结点

```java
   //删除尾结点
    public E removeLast() {
        //尾结点
        final Node<E> l = last;
        //链表为空，抛出异常
        if (l == null)
            throw new NoSuchElementException();
            
        return unlinkLast(l);
    }

   //删除尾结点
   private E unlinkLast(Node<E> l) {
        // 尾结点元素
        final E element = l.item;
        //尾结点前趋节点
        final Node<E> prev = l.prev;
        //尾结点数据、前趋置为null，帮助GC
        l.item = null;
        l.prev = null; 
        //尾结点置为前趋节点
        last = prev;
        // 如果只有一个元素，删除了把first置为空
       // 否则把前置节点的next置为空
        if (prev == null)
            first = null;
        else
            prev.next = null;
        size--;
        modCount++;
        //返回删除的节点
        return element;
    }
```

**注意：**
不管是上一节的头插入和未插入，还是这一节的删除头节点和删除尾结点，都没有在List中定义。前面提到，LinkedList实现了Deque接口，所以这是作为双向队列的LinkedList插入和删除元素的方式。还有获取头结点和尾结点的方法getFirst()和getLast()，同样都是双向队列的实现。

删除指定位置的节点

![image-20210321140558782](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321140558.png)

```
   //删除指定位置的节点
    public E remove(int index) {
       //检查越界情况
        checkElementIndex(index);
        //根据索引找到节点，删除
        return unlink(node(index));
    }

     //删除指定节点
       E unlink(Node<E> x) {
        // 删除节点的值
        final E element = x.item;
        //被删除节点的后继节点
        final Node<E> next = x.next;
        //被删除节点的前趋节点
        final Node<E> prev = x.prev;
        // 如果前趋节点为空
        // 说明是首节点，让first指向x的后继节点
       // 否则修改前置节点的next为x的后继节点
        if (prev == null) {
            first = next;
        } else {
            prev.next = next;
            x.prev = null;
        }
        // 如果后继节点为空
        // 说明是尾节点，让last指向x的前趋节点
       // 否则修改后置节点的prev为x的前趋节点
        if (next == null) {
            last = prev;
        } else {
            next.prev = prev;
            x.next = null;
        }
         // 清空x的元素值，协助GC
        x.item = null;
        // 元素个数减1
        size--;
        // 修改次数加1，fail-fast
        modCount++;
        //返回删除的元素
        return element;
    }

```

删除头尾节点，时间复杂度为O(1)。

在中间删除元素，首先要找到删除位置的节点，再修改前后指针，时间复杂度为O(n)。
**前面还提到，LinkedList可以作为栈使用，栈的特点是先进后出，LinkedList同样有作为栈的方法实现。**

- push

  入栈：插入头节点

  ```
      public void push(E e) {
          addFirst(e);
      }
  ```

- pop

  出栈：删除头结点

  ```
      public E pop() {
          return removeFirst();
      }
  ```

#### 与ArrayList

LinkedList、ArrayList基本操作时间效率对比如下(粗略对比)：

| 操作                      | ArrayList                       | LinkedList         |
| ------------------------- | ------------------------------- | ------------------ |
| get(int index)            | O(1)                            | O(n)，平均 n / 4步 |
| add(E element)            | 最坏情况（扩容）O(n) ，平均O(1) | O(1)               |
| add(int index, E element) | O(n) ,平均n / 2步               | O(n)，平均 n / 4步 |
| remove(int index)         | O(n) 平均n /2步                 | O(n)，平均 n / 4步 |

简而言之，需要频繁读取集合中的元素时，使用ArrayList效率较高，而在插入和删除操作较多时，使用LinkedList效率较高。





## 4. List 常见问题

### 4.1. Arrays.asList 问题点

在业务开发中，我们常常会把原始的数组转换为 `List` 类数据结构，来继续展开各种 `Stream` 操作。通常，我们会使用 `Arrays.asList` 方法可以把数组一键转换为 `List`。

【示例】Arrays.asList 转换基本类型数组

```
int[] arr = { 1, 2, 3 };
List list = Arrays.asList(arr);
log.info("list:{} size:{} class:{}", list, list.size(), list.get(0).getClass());
```

【输出】

```
11:26:33.214 [main] INFO io.github.dunwu.javacore.container.list.AsList示例 - list:[[I@ae45eb6] size:1 class:class [I
```

数组元素个数为 3，但转换后的列表个数为 1。

由此可知， `Arrays.asList` 第一个问题点：**不能直接使用 `Arrays.asList` 来转换基本类型数组**。

其原因是：`Arrays.asList` 方法传入的是一个泛型 T 类型可变参数，最终 `int` 数组整体作为了一个对象成为了泛型类型 T：

```
public static <T> List<T> asList(T... a) {
    return new ArrayList<>(a);
}
```

直接遍历这样的 `List` 必然会出现 Bug，修复方式有两种，如果使用 Java8 以上版本可以使用 `Arrays.stream` 方法来转换，否则可以把 `int` 数组声明为包装类型 `Integer` 数组：

【示例】转换整型数组为 List 的正确方式

```
int[] arr1 = { 1, 2, 3 };
List list1 = Arrays.stream(arr1).boxed().collect(Collectors.toList());
log.info("list:{} size:{} class:{}", list1, list1.size(), list1.get(0).getClass());

Integer[] arr2 = { 1, 2, 3 };
List list2 = Arrays.asList(arr2);
log.info("list:{} size:{} class:{}", list2, list2.size(), list2.get(0).getClass());
```

【示例】Arrays.asList 转换引用类型数组

```
String[] arr = { "1", "2", "3" };
List list = Arrays.asList(arr);
arr[1] = "4";
try {
    list.add("5");
} catch (Exception ex) {
    ex.printStackTrace();
}
log.info("arr:{} list:{}", Arrays.toString(arr), list);
```

抛出 `java.lang.UnsupportedOperationException`。

抛出异常的原因在于 `Arrays.asList` 第二个问题点：**`Arrays.asList` 返回的 `List` 不支持增删操作**。`Arrays.asList` 返回的 List 并不是我们期望的 `java.util.ArrayList`，而是 `Arrays` 的内部类 `ArrayList`。

查看源码，我们可以发现 `Arrays.asList` 返回的 `ArrayList` 继承了 `AbstractList`，但是并没有覆写 `add` 和 `remove` 方法。

```
private static class ArrayList<E> extends AbstractList<E>
    implements RandomAccess, java.io.Serializable
{
    private static final long serialVersionUID = -2764017481108945198L;
    private final E[] a;

    ArrayList(E[] array) {
        a = Objects.requireNonNull(array);
    }

    // ...

    @Override
    public E set(int index, E element) {
        E oldValue = a[index];
        a[index] = element;
        return oldValue;
    }

}

public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E> {
    public void add(int index, E element) {
        throw new UnsupportedOperationException();
    }

    public E remove(int index) {
        throw new UnsupportedOperationException();
    }
}
```

`Arrays.asList` 第三个问题点：**对原始数组的修改会影响到我们获得的那个 `List`**。`ArrayList` 其实是直接使用了原始的数组。

解决方法很简单，重新 `new` 一个 `ArrayList` 初始化 `Arrays.asList` 返回的 `List` 即可：

```
String[] arr = { "1", "2", "3" };
List list = new ArrayList(Arrays.asList(arr));
arr[1] = "4";
try {
    list.add("5");
} catch (Exception ex) {
    ex.printStackTrace();
}
log.info("arr:{} list:{}", Arrays.toString(arr), list);
```

### 4.2. List.subList 问题点

List.subList 直接引用了原始的 List，也可以认为是共享“存储”，而且对原始 List 直接进行结构性修改会导致 SubList 出现异常。

```
private static List<List<Integer>> data = new ArrayList<>();

private static void oom() {
    for (int i = 0; i < 1000; i++) {
        List<Integer> rawList = IntStream.rangeClosed(1, 100000).boxed().collect(Collectors.toList());
        data.add(rawList.subList(0, 1));
    }
}
```

出现 OOM 的原因是，循环中的 1000 个具有 10 万个元素的 List 始终得不到回收，因为它始终被 subList 方法返回的 List 强引用。

解决方法是：

```
private static void oomfix() {
    for (int i = 0; i < 1000; i++) {
        List<Integer> rawList = IntStream.rangeClosed(1, 100000).boxed().collect(Collectors.toList());
        data.add(new ArrayList<>(rawList.subList(0, 1)));
    }
}
```

【示例】子 List 强引用原始的 List

```
private static void wrong() {
    List<Integer> list = IntStream.rangeClosed(1, 10).boxed().collect(Collectors.toList());
    List<Integer> subList = list.subList(1, 4);
    System.out.println(subList);
    subList.remove(1);
    System.out.println(list);
    list.add(0);
    try {
        subList.forEach(System.out::println);
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}
```

抛出 `java.util.ConcurrentModificationException`。

解决方法：

一种是，不直接使用 subList 方法返回的 SubList，而是重新使用 new ArrayList，在构造方法传入 SubList，来构建一个独立的 ArrayList；

另一种是，对于 Java 8 使用 Stream 的 skip 和 limit API 来跳过流中的元素，以及限制流中元素的个数，同样可以达到 SubList 切片的目的。

```
//方式一：
List<Integer> subList = new ArrayList<>(list.subList(1, 4));
//方式二：
List<Integer> subList = list.stream().skip(1).limit(3).collect(Collectors.toList());
```



## 5.Stack

### Stack 原理

首先看段源码：

```ruby
public class Stack<E> extends Vector<E> {
```

由此知道 Stack 继承自 Vector，Vector是个什么鬼，接触不多，但我们大概知道它跟 ArrayList 似乎有那么点关系(因为面试的时候会涉及到)，具体啥关系，不清楚，那么接着看源码：

```java
public class Vector<E>extends AbstractList<E>
    implements List<E>, RandomAccess, Cloneable, java.io.Serializable{
```

这里，我们知道了两点：

1. Vector 是 AbstractList 子类
2. Vector 实现了 List 接口

 ok，让我们再稍微追溯下 ArrayList 源码：

```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable{
```

ArrayList 是 List 的一个实现类，这大家都清楚，关键是这里我们知道了一个信息：ArrayList 是 AbstractList 子类

那么，ArrayList 和 Vector 关系总算清楚了，他们是哥们关系，并且都继承自 AbstractList

从上面所有的分析，我们知道了Stack 本质也是一个 List。其具备 List 所有方法。

然后，我们需要了解的是，Stack 栈是一个 "先进后出"的原理。

那么基于以上，我们需要记住的是：

- ##### Stack 栈是一个 "先进后出"的原理

- ##### Stack 本质是一个List，其具备 List 所有方法

### Stack 的使用

**1、初始化**

```cpp
Stack stack=new Stack();
```

**2、判断Stack是否为空**

```undefined
isEmpty()
```

**3、添加元素**

```undefined
push(E item)
```

我们知道 Stack 也是一个List，而List的添加是 add(E e),那么Stack的 push 和 add 方法有啥不同呢?
 下面先看 源码中Stack 的add方法：

```java
    public synchronized boolean add(E e) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = e;
        return true;
    }
```

Stack 中push方法的源码:

```cpp
public E push(E item) {
        addElement(item);

        return item;
    }
```

追溯 addElement(item) 方法：

```java
    public synchronized void addElement(E obj) {
        modCount++;
        ensureCapacityHelper(elementCount + 1);
        elementData[elementCount++] = obj;
    }
```

发现了没？原来stack的push方法最后调用的和stack的add方法是同一个方法，即push调用的其实还是 add方法。

**4、获取栈顶值,元素不出栈(栈为空时抛异常)**

```undefined
peek()
```

这里需要注意的是，stack调用peek后，item还是在栈中的，并未被移除，然后在调用peek时要判断stack中是否有元素，否则会引发异常

**5、是否存在obj**

```dart
search(Object obj)
```

返回值为int，若存在，返回值为obj距离栈顶的位置，若不存在，返回 -1

**6、移除栈顶**

```undefined
pop()
```

此方法是移除栈顶的元素，并且返回值是移除的item

**7、其他方法**

stack 作为 list，具备 list 常用方法，如：

```csharp
//获取stack长度
size()
//下标处添加
add(int index, E element)
//添加集合
addAll(Collection<? extends E> c)
//移除对象
remove(Object obj)
//根据下标移除对象
remove(int index)
//清空
clear()
```

其他关于 list 的方法，这里就不多讲了。

**8、Stack的通常操作**

- push 入栈
- pop 栈顶元素出栈，并返回
- peek 获取栈顶元素，并不删除

### Stack 和 ArrayList 的区别

stack 和 ArrayList 的最大区别是 stack 是线程安全的，而 ArrayList 不是线程安全的。所以，当涉及到多线程问题的时候，优先考虑使用 stack

### 使用示例及结果

调用代码如下：

```cpp
    public static void main(String []args){
        System.out.println("我是主函数");

        Stack stack=new Stack();
//        stack.isEmpty();//栈是否为空
//        stack.push("A");//进栈
//        stack.peek();//获取栈顶值,元素不出栈
//        stack.pop();//栈顶元素出栈
//        stack.search("pp");

        System.out.println("====stack是否为空====="+stack.isEmpty());
        //添加
        stack.push("A");//进栈
        stack.push("b");//进栈
        stack.push("c");//进栈
        //长度
        System.out.println("====stack==size==1==="+stack.size());
        //获取栈顶值,元素不出栈(栈为空时抛异常)
        if(stack!=null&&!stack.isEmpty()) {
            System.out.println("====stack==topValue=====" + stack.peek());
        }
        //长度
        System.out.println("====stack==size==2==="+stack.size());
        //找下标
        System.out.println("====stack===========index==="+stack.search("A"));
        //移除栈顶
        Object obj=stack.pop();
        System.out.println("====stack==pop==obj="+obj);
        //长度
        System.out.println("====stack==size==3==="+stack.size());
    }
```

返回结果：

```cpp
我是主函数
====stack是否为空=====true
====stack==size==1===3
====stack==topValue=====c
====stack==size==2===3
====stack===========index===3
====stack==pop==obj=c
====stack==size==3===2
```



## 面试题

















