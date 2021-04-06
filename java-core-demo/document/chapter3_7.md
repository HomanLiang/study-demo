[toc]



# Java 容器最佳实践

## 永远不要使用双花括号初始化实例，否则就会OOM！

### 集合初始化

```
List<String> list = new ArrayList<String>() {{
    add("www.");
    add("javastack.");
    add("cn");
}};

Map<String, String> map = new HashMap<String, String>() {{
    put("1", "www.");
    put("2", "javastack.");
    put("3", "cn");
}};
```

### 双花括号初始化分析
以我们这段代码为例：
```
Map<String, String> map = new HashMap() {{
    put("map1", "value1");
    put("map2", "value2");
    put("map3", "value3");
}};
```
这段代码其实是创建了匿名内部类，然后再进行初始化代码块。

这一点我们可以使用命令 javac 将代码编译成字节码之后发现，我们发现之前的一个类被编译成两个字节码（.class）文件，如下图所示：

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173121.png)

我们使用 Idea 打开 DoubleBracket$1.class 文件发现：

```
import java.util.HashMap;

class DoubleBracket$1 extends HashMap {
    DoubleBracket$1(DoubleBracket var1) {
        this.this$0 = var1;
        this.put("map1", "value1");
        this.put("map2", "value2");
    }
}
```
此时我们可以确认，它就是一个匿名内部类。那么问题来了，匿名内部类为什么会导致内存溢出呢？

### 匿名内部类的“锅”
在 Java 语言中非静态内部类会持有外部类的引用，从而导致 GC 无法回收这部分代码的引用，以至于造成内存溢出。
#### 思考 1：为什么要持有外部类？
这个就要从匿名内部类的设计说起了，在 Java 语言中，非静态匿名内部类的主要作用有两个。

1. 当匿名内部类只在外部类（主类）中使用时，匿名内部类可以让外部不知道它的存在，从而减少了代码的维护工作。
2. 当匿名内部类持有外部类时，它就可以直接使用外部类中的变量了，这样可以很方便的完成调用，如下代码所示：
```
public class DoubleBracket {
    private static String userName = "磊哥";
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> map = new HashMap() {{
            put("map1", "value1");
            put("map2", "value2");
            put("map3", "value3");
            put(userName, userName);
        }};
    }
}
```
从上述代码可以看出在 HashMap 的方法内部，可以直接使用外部类的变量 userName。
#### 思考 2：它是怎么持有外部类的？
关于匿名内部类是如何持久外部对象的，我们可以通过查看匿名内部类的字节码得知，我们使用 `javap -c DoubleBracket\$1.class` 命令进行查看，其中 `$1` 为以匿名类的字节码，字节码的内容如下；
```
javap -c DoubleBracket\$1.class
Compiled from "DoubleBracket.java"
class com.example.DoubleBracket$1 extends java.util.HashMap {
  final com.example.DoubleBracket this$0;

  com.example.DoubleBracket$1(com.example.DoubleBracket);
    Code:
       0: aload_0
       1: aload_1
       2: putfield      #1                  // Field this$0:Lcom/example/DoubleBracket;
       5: aload_0
       6: invokespecial #7                  // Method java/util/HashMap."<init>":()V
       9: aload_0
      10: ldc           #13                 // String map1
      12: ldc           #15                 // String value1
      14: invokevirtual #17                 // Method put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      17: pop
      18: aload_0
      19: ldc           #21                 // String map2
      21: ldc           #23                 // String value2
      23: invokevirtual #17                 // Method put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      26: pop
      27: return
}
```
其中，关键代码的在 putfield 这一行，此行表示有一个对 DoubleBracket 的引用被存入到 this$0 中，也就是说这个匿名内部类持有了外部类的引用。

如果您觉得以上字节码不够直观，没关系，我们用下面的实际的代码来证明一下：
```
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DoubleBracket {
    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        Map map = new DoubleBracket().createMap();
        // 获取一个类的所有字段
        Field field = map.getClass().getDeclaredField("this$0");
        // 设置允许方法私有的 private 修饰的变量
        field.setAccessible(true);
        System.out.println(field.get(map).getClass());
    }
    public Map createMap() {
        // 双花括号初始化
        Map map = new HashMap() {{
            put("map1", "value1");
            put("map2", "value2");
            put("map3", "value3");
        }};
        return map;
    }
}
```
当我们开启调试模式时，可以看出 map 中持有了外部对象 DoubleBracket，如下图所示：
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173133.png)
以上代码的执行结果为：

> class com.example.DoubleBracket
>
> 从以上程序输出结果可以看出：匿名内部类持有了外部类的引用，因此我们才可以使用 $0 正常获取到外部类，并输出相关的类信息。

### 什么情况会导致内存泄漏？
当我们把以下正常的代码：
```
public void createMap() {
    Map map = new HashMap() {{
        put("map1", "value1");
        put("map2", "value2");
        put("map3", "value3");
    }};
    // 业务处理....
}
```
改为下面这个样子时，可能会造成内存泄漏：
```
public Map createMap() {
    Map map = new HashMap() {{
        put("map1", "value1");
        put("map2", "value2");
        put("map3", "value3");
    }};
    return map;
}
```
为什么用了「可能」而不是「一定」会造成内存泄漏？

这是因为当此 map 被赋值为其他类属性时，可能会导致 GC 收集时不清理此对象，这时候才会导致内存泄漏。
### 如何保证内存不泄露？
要想保证双花扣号不泄漏，办法也很简单，只需要将 map 对象声明为 static 静态类型的就可以了，代码如下：
```
public static Map createMap() {
    Map map = new HashMap() {{
        put("map1", "value1");
        put("map2", "value2");
        put("map3", "value3");
    }};
    return map;
}
```
什么？你不相信！

没关系，我们用事实说话，使用以上代码，我们重新编译一份字节码，查看匿名类的内容如下：
```
javap -c  DoubleBracket\$1.class
Compiled from "DoubleBracket.java"
class com.example.DoubleBracket$1 extends java.util.HashMap {
  com.example.DoubleBracket$1();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/util/HashMap."<init>":()V
       4: aload_0
       5: ldc           #7                  // String map1
       7: ldc           #9                  // String value1
       9: invokevirtual #11                 // Method put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      12: pop
      13: aload_0
      14: ldc           #17                 // String map2
      16: ldc           #19                 // String value2
      18: invokevirtual #11                 // Method put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      21: pop
      22: aload_0
      23: ldc           #21                 // String map3
      25: ldc           #23                 // String value3
      27: invokevirtual #11                 // Method put:(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
      30: pop
      31: return
}
```
从这次的代码我们可以看出，已经没有 putfield 关键字这一行了，也就是说静态匿名类不会持有外部对象的引用了。

#### 为什么静态内部类不会持有外部类的引用？
原因其实很简单，因为匿名内部类是静态的之后，它所引用的对象或属性也必须是静态的了，因此就可以直接从 JVM 的 Method Area（方法区）获取到引用而无需持久外部对象了。
### 双花括号的替代方案
即使声明为静态的变量可以避免内存泄漏，但依旧不建议这样使用，为什么呢？

原因很简单，项目一般都是需要团队协作的，假如那位老兄在不知情的情况下把你的 static 给删掉呢？这就相当于设置了一个隐形的“坑”，其他不知道的人，一不小心就跳进去了，所以我们可以尝试一些其他的方案，比如 Java8 中的 Stream API 和 Java9 中的集合工厂等。

#### 替代方案 1：Stream
使用 Java8 中的 Stream API 替代，示例如下。原代码：
```
List<String> list = new ArrayList() {{
    add("Java");
    add("Redis");
}};
```
替代代码：
```
List<String> list = Stream.of("Java", "Redis").collect(Collectors.toList());
```
替代方案 2：集合工厂

使用集合工厂的 of 方法替代，示例如下。原代码：

```
Map map = new HashMap() {{
    put("map1", "value1");
    put("map2", "value2");
}};
```
替代代码：
```
Map map = Map.of("map1", "Java", "map2", "Redis");
```

### 总结
本文我们讲了双花括号初始化因为会持有外部类的引用，从而可以会导致内存泄漏的问题，还从字节码以及反射的层面演示了这个问题。

要想保证双花括号初始化不会出现内存泄漏的办法也很简单，只需要被 static 修饰即可，但这样做还是存在潜在的风险，可能会被某人不小心删除掉，于是我们另寻它道，发现了可以使用 Java8 中的 Stream 或 Java9 中的集合工厂 of 方法替代“{{”。

### 使用匿名内部类初始化集合类
```
List<String> list = new ArrayList<String>(){
            {
                add("a");
            }
        };
```
这种写法，是使用匿名内部类，继承自ArrayList，同时菱形运算符里的String不能省略，否则Eclipse会提示错误'<>' cannot be used with anonymous classes。因为这里省略String，编译器无法推测正确的类型。里层的大括号包裹的代码是实例初始化块。 

这种写法编译器会警告 `The serializable class does not declare a static final serialVersionUID field of type long`，跟 `serialVersionUID` 这个东西有关。就是说这种写法在序列化上会出现一些问题。
```
private static final List<String> list = new ArrayList<String>() {
    {
        add("a");
    }
};
```
不符合sonar java规范

sonar Java规范中上述写法为 `Noncompliant - ArrayList should be extended only to add behavior, not for initialization. `

静态对象，用标准的静态块初始化,应该如下写：

```
private static final List<String> list = new ArrayList<>();
static {
    list.add("a");
}
```





## ArrayList插入大量数据问题
**问题描述：**
```
List<String> temp = new ArrayList() ;
//获取一批数据
List<String> all = getData();
for(String str : all) {
    temp.add(str);
}
```
首先大家看看这段代码有什么问题嘛？

其实在大部分情况下这都是没啥问题，无非就是循环的往 ArrayList 中写入数据而已。

但在特殊情况下，比如这里的 getData() 返回数据非常巨大时后续 temp.add(str) 就会有问题了。

比如我们在 review 代码时发现这里返回的数据有时会高达 2000W，这时 ArrayList 写入的问题就凸显出来了。

**填坑指南**

大家都知道 ArrayList 是由数组实现，而数据的长度有限；需要在合适的时机对数组扩容。

这里以插入到尾部为例 add(E e)。

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173410.png)

```
ArrayList<String> temp = new ArrayList<>(2) ;
temp.add("1");
temp.add("2");
temp.add("3");
```
当我们初始化一个长度为 2 的 ArrayList ，并往里边写入三条数据时 ArrayList 就得扩容了，也就是将之前的数据复制一份到新的数组长度为 3 的数组中。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173422.png)

之所以是 3 ，是因为新的长度=原有长度 * 1.5

通过源码我们可以得知 ArrayList 的默认长度为 10.

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173438.png)

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173448.png)

但其实并不是在初始化的时候就创建了 DEFAULT_CAPACITY=10 的数组。

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173500.png)

而是在往里边 add 第一个数据的时候会扩容到 10.

既然知道了默认的长度为 10 ，那说明后续一旦写入到第九个元素的时候就会扩容为 10*1.5=15。这一步为数组复制，也就是要重新开辟一块新的内存空间存放这 15 个数组。

一旦我们频繁且数量巨大的进行写入时就会导致许多的数组复制，这个效率是极低的。

但如果我们提前预知了可能会写入多少条数据时就可以提前避免这个问题。

比如我们往里边写入 1000W 条数据，在初始化的时候就给定数组长度与用默认 10 的长度之间性能是差距巨大的。

这里强烈建议大家：在有大量数据写入 ArrayList 时，一定要初始化指定长度。

再一个是一定要慎用 add(intindex,E element) 向指定位置写入数据。

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173514.png)

通过源码我们可以看出，每一次写入都会将 index 后的数据往后移动一遍，其实本质也是要复制数组；

但区别于往常规的往数组尾部写入数据，它每次都会进行数组复制，效率极低。



## Arrays.asList()使用指南

### 简介
Arrays.asList()在平时开发中还是比较常见的，我们可以使用它将一个数组转换为一个List集合。
```
String[] myArray = { "Apple", "Banana", "Orange" }； 
List<String> myList = Arrays.asList(myArray);
//上面两个语句等价于下面一条语句
List<String> myList = Arrays.asList("Apple","Banana", "Orange");
```
JDK 源码对于这个方法的说明：
```
/**
 *返回由指定数组支持的固定大小的列表。此方法作为基于数组和基于集合的API之间的桥梁，与Collection.toArray()结合使用。返回的List是可序列化并实现RandomAccess接口。
 */ 
public static <T> List<T> asList(T... a) {
    return new ArrayList<>(a);
}
```
### 《阿里巴巴Java 开发手册》对其的描述
Arrays.asList()将数组转换为集合后,底层其实还是数组，《阿里巴巴Java 开发手册》对于这个方法有如下描述：
![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173532.png)

### 使用时的注意事项总结
#### 传递的数组必须是对象数组，而不是基本类型。
Arrays.asList()是泛型方法，传入的对象必须是对象数组。  
```
int[] myArray = { 1, 2, 3 };
List myList = Arrays.asList(myArray);
System.out.println(myList.size());//1
System.out.println(myList.get(0));//数组地址值
System.out.println(myList.get(1));//报错：ArrayIndexOutOfBoundsException
int [] array=(int[]) myList.get(0);
System.out.println(array[0]);//1
```
当传入一个原生数据类型数组时，Arrays.asList() 的真正得到的参数就不是数组中的元素，而是数组对象本身！此时List 的唯一元素就是这个数组，这也就解释了上面的代码。
我们使用包装类型数组就可以解决这个问题。
```
Integer[] myArray = { 1, 2, 3 };
```
#### 使用集合的修改方法:add()、remove()、clear()会抛出异常。
```
List myList = Arrays.asList(1, 2, 3);
myList.add(4);//运行时报错：UnsupportedOperationException
myList.remove(1);//运行时报错：UnsupportedOperationException
myList.clear();//运行时报错：UnsupportedOperationException
```
Arrays.asList() 方法返回的并不是 java.util.ArrayList ，而是 java.util.Arrays 的一个内部类,这个内部类并没有实现集合的修改方法或者说并没有重写这些方法。
```
List myList = Arrays.asList(1, 2, 3);
System.out.println(myList.getClass());//class java.util.Arrays$ArrayList
```
下图是java.util.Arrays$ArrayList的简易源码，我们可以看到这个类重写的方法有哪些。
```
  private static class ArrayList<E> extends AbstractList<E>
        implements RandomAccess, java.io.Serializable
    {
        ...

        @Override
        public E get(int index) {
          ...
        }

        @Override
        public E set(int index, E element) {
          ...
        }

        @Override
        public int indexOf(Object o) {
          ...
        }

        @Override
        public boolean contains(Object o) {
           ...
        }

        @Override
        public void forEach(Consumer<? super E> action) {
          ...
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
          ...
        }

        @Override
        public void sort(Comparator<? super E> c) {
          ...
        }
    }
```
我们再看一下java.util.AbstractList的remove()方法，这样我们就明白为啥会抛出UnsupportedOperationException。
```
public E remove(int index) {
    throw new UnsupportedOperationException();
}
```
## 如何正确的将数组转换为ArrayList?
### 自己动手实现（教育目的）
```
//JDK1.5+
static <T> List<T> arrayToList(final T[] array) {
  final List<T> l = new ArrayList<T>(array.length);

  for (final T s : array) {
    l.add(s);
  }
  return (l);
}
```
```
Integer [] myArray = { 1, 2, 3 };
System.out.println(arrayToList(myArray).getClass());//class java.util.ArrayList
```
### 最简便的方法(推荐)
```
List list = new ArrayList<>(Arrays.asList("a", "b", "c"))
```
注意：
1. 这样做生成的list，是定长的。也就是说，如果你对它做add或者remove，都会抛`UnsupportedOperationException`。
2. 如果修改数组的值，list中的对应值也会改变！
### 使用 Java8 的Stream(推荐)
```
Integer [] myArray = { 1, 2, 3 };
List myList = Arrays.stream(myArray).collect(Collectors.toList());
```
//基本类型也可以实现转换（依赖boxed的装箱操作）
```
int [] myArray2 = { 1, 2, 3 };
List myList = Arrays.stream(myArray2).boxed().collect(Collectors.toList());
```
### 使用 Guava(推荐)
对于不可变集合，你可以使用ImmutableList类及其of()与copyOf()工厂方法：（参数不能为空）
```
List<String> il = ImmutableList.of("string", "elements");  // from varargs
List<String> il = ImmutableList.copyOf(aStringArray);      // from array
```
对于可变集合，你可以使用Lists类及其newArrayList()工厂方法：
```
List<String> l1 = Lists.newArrayList(anotherListOrCollection);    // from collection
List<String> l2 = Lists.newArrayList(aStringArray);               // from array
List<String> l3 = Lists.newArrayList("or", "string", "elements"); // from varargs
```
### 使用 Apache Commons Collections
```
List<String> list = new ArrayList<String>();
CollectionUtils.addAll(list, str);
```

### 使用 keySet 迭代器迭代 Map，获取对应的 value。
反例：

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173549.png)

正解：

![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173557.png)

解惑：keySet 方式遍历 Map 的性能不如 entrySet 性能好。

![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321173606.png)

如果采用 keySet 的方式获取 Map 中 key，然后通过 key 获取 Map 对应的 value，如上图 HashMap 源码所示，每次都需要通过 key 去计算对应的 hash 值，然后再通过 hash 值获取对应的 value，效率会低不少。

建议：
- 如果想获取 Map 对应的 key 和 value，则推荐使用 entrySet。
- 如果只是单纯获取 Map 对应的 key，则推荐使用 keySet。