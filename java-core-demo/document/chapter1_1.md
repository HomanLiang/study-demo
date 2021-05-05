[toc]



# Java 基本数据类型

## 1. 数据类型分类

Java 中的数据类型有两类：

- 值类型（又叫内置数据类型，基本数据类型）
- 引用类型（除值类型以外，都是引用类型，包括 `String`、数组）

### 1.1. 值类型

Java 语言提供了 **8** 种基本类型，大致分为 **4** 类

| 基本数据类型 | 分类       | 比特数 | 默认值     | 取值范围                      | 说明                              |
| ------------ | ---------- | ------ | ---------- | ----------------------------- | --------------------------------- |
| `boolean`    | **布尔型** | 8 位   | `false`    | {false, true}                 |                                   |
| `char`       | **字符型** | 16 位  | `'\u0000'` | [0, $2^{16} - 1$]             | 存储 Unicode 码，用单引号赋值     |
| `byte`       | **整数型** | 8 位   | `0`        | [-$2^7$, $2^7 - 1$]           |                                   |
| `short`      | **整数型** | 16 位  | `0`        | [-$2^{15}$, $2^{15} - 1$]     |                                   |
| `int`        | **整数型** | 32 位  | `0`        | [-$2^{31}$, $2^{31} - 1$]     |                                   |
| `long`       | **整数型** | 64 位  | `0L`       | [-$2^{63}$, $2^{63} - 1$]     | 赋值时一般在数字后加上 `l` 或 `L` |
| `float`      | **浮点型** | 32 位  | `+0.0F`    | [$2^{-149}$, $2^{128} - 1$]   | 赋值时必须在数字后加上 `f` 或 `F` |
| `double`     | **浮点型** | 64 位  | `+0.0D`    | [$2^{-1074}$, $2^{1024} - 1$] | 赋值时一般在数字后加 `d` 或 `D`   |

尽管各种数据类型的默认值看起来不一样，但在内存中都是 0。

在这些基本类型中，`boolean` 和 `char` 是唯二的无符号类型。

### 1.2. 值类型和引用类型的区别

- 从概念方面来说
  - 基本类型：变量名指向具体的数值。
  - 引用类型：变量名指向存数据对象的内存地址。
- 从内存方面来说
  - 基本类型：变量在声明之后，Java 就会立刻分配给他内存空间。
  - 引用类型：它以特殊的方式（类似 C 指针）向对象实体（具体的值），这类变量声明时不会分配内存，只是存储了一个内存地址。
- 从使用方面来说
  - 基本类型：使用时需要赋具体值,判断时使用 `==` 号。
  - 引用类型：使用时可以赋 null，判断时使用 `equals` 方法。

> 👉 扩展阅读：[Java 基本数据类型和引用类型](https://juejin.im/post/59cd71835188255d3448faf6)
>
> 这篇文章对于基本数据类型和引用类型的内存存储讲述比较生动。

## 2. 数据转换

Java 中，数据类型转换有两种方式：

- 自动转换
- 强制转换

### 2.1. 自动转换

一般情况下，定义了某数据类型的变量，就不能再随意转换。但是 JAVA 允许用户对基本类型做**有限度**的类型转换。

如果符合以下条件，则 JAVA 将会自动做类型转换：

- **由小数据转换为大数据**

  显而易见的是，“小”数据类型的数值表示范围小于“大”数据类型的数值表示范围，即精度小于“大”数据类型。

  所以，如果“大”数据向“小”数据转换，会丢失数据精度。比如：long 转为 int，则超出 int 表示范围的数据将会丢失，导致结果的不确定性。

  反之，“小”数据向“大”数据转换，则不会存在数据丢失情况。由于这个原因，这种类型转换也称为**扩大转换**。

  这些类型由“小”到“大”分别为：(byte，short，char) < int < long < float < double。

  这里我们所说的“大”与“小”，并不是指占用字节的多少，而是指表示值的范围的大小。

- **转换前后的数据类型要兼容**

  由于 boolean 类型只能存放 true 或 false，这与整数或字符是不兼容的，因此不可以做类型转换。

- **整型类型和浮点型进行计算后，结果会转为浮点类型**

  示例：

    ```java
    long x = 30;
    float y = 14.3f;
    System.out.println("x/y = " + x/y);
    ```

	输出：

    ```java
    x/y = 1.9607843
    ```

	可见 long 虽然精度大于 float 类型，但是结果为浮点数类型。

### 2.2. 强制转换

在不符合自动转换条件时或者根据用户的需要，可以对数据类型做强制的转换。

**强制转换使用括号 `()` 。**

引用类型也可以使用强制转换。

示例：

```java
float f = 25.5f;
int x = (int)f;
System.out.println("x = " + x);
```

## 3. 装箱和拆箱

### 3.1. 包装类、装箱、拆箱

Java 中为每一种基本数据类型提供了相应的包装类，如下：

```java
Byte <-> byte
Short <-> short
Integer <-> int
Long <-> long
Float <-> float
Double <-> double
Character <-> char
Boolean <-> boolean
```

**引入包装类的目的**就是：提供一种机制，使得**基本数据类型可以与引用类型互相转换**。

基本数据类型与包装类的转换被称为`装箱`和`拆箱`。

- `装箱`（boxing）是将值类型转换为引用类型。例如：`int` 转 `Integer`
  - 装箱过程是通过调用包装类的 `valueOf` 方法实现的。

- `拆箱`（unboxing）是将引用类型转换为值类型。例如：`Integer` 转 `int`
  - 拆箱过程是通过调用包装类的 `xxxValue` 方法实现的。（xxx 代表对应的基本数据类型）。

### 3.2. 自动装箱、自动拆箱

基本数据（Primitive）型的自动装箱（boxing）拆箱（unboxing）自 JDK 5 开始提供的功能。

自动装箱与拆箱的机制可以让我们在 Java 的变量赋值或者是方法调用等情况下使用原始类型或者对象类型更加简单直接。 因为自动装箱会隐式地创建对象，如果在一个循环体中，会创建无用的中间对象，这样会增加 GC 压力，拉低程序的性能。所以在写循环时一定要注意代码，避免引入不必要的自动装箱操作。

JDK 5 之前的形式：

```java
Integer i1 = new Integer(10); // 非自动装箱
```

JDK 5 之后：

```java
Integer i2 = 10; // 自动装箱
```

Java 对于自动装箱和拆箱的设计，依赖于一种叫做享元模式的设计模式（有兴趣的朋友可以去了解一下源码，这里不对设计模式展开详述）。

> 👉 扩展阅读：[深入剖析 Java 中的装箱和拆箱](https://www.cnblogs.com/dolphin0520/p/3780005.html)
>
> 结合示例，一步步阐述装箱和拆箱原理。

### 3.3. 装箱、拆箱的应用和注意点

#### 装箱、拆箱应用场景

- 一种最普通的场景是：调用一个**含类型为 `Object` 参数的方法**，该 `Object` 可支持任意类型（因为 `Object` 是所有类的父类），以便通用。当你需要将一个值类型（如 int）传入时，需要使用 `Integer` 装箱。
- 另一种用法是：一个**非泛型的容器**，同样是为了保证通用，而将元素类型定义为 `Object`。于是，要将值类型数据加入容器时，需要装箱。
- 当 `==` 运算符的两个操作，一个操作数是包装类，另一个操作数是表达式（即包含算术运算）则比较的是数值（即会触发自动拆箱的过程）。

【示例】装箱、拆箱示例

```java
Integer i1 = 10; // 自动装箱
Integer i2 = new Integer(10); // 非自动装箱
Integer i3 = Integer.valueOf(10); // 非自动装箱
int i4 = new Integer(10); // 自动拆箱
int i5 = i2.intValue(); // 非自动拆箱
System.out.println("i1 = [" + i1 + "]");
System.out.println("i2 = [" + i2 + "]");
System.out.println("i3 = [" + i3 + "]");
System.out.println("i4 = [" + i4 + "]");
System.out.println("i5 = [" + i5 + "]");
System.out.println("i1 == i2 is [" + (i1 == i2) + "]");
System.out.println("i1 == i4 is [" + (i1 == i4) + "]"); // 自动拆箱
// Output:
// i1 = [10]
// i2 = [10]
// i3 = [10]
// i4 = [10]
// i5 = [10]
// i1 == i2 is [false]
// i1 == i4 is [true]
```

【说明】

上面的例子，虽然简单，但却隐藏了自动装箱、拆箱和非自动装箱、拆箱的应用。从例子中可以看到，明明所有变量都初始化为数值 10 了，但为何会出现 `i1 == i2 is [false]` 而 `i1 == i4 is [true]` ？

原因在于：

- i1、i2 都是包装类，使用 `==` 时，Java 将它们当做两个对象，而非两个 int 值来比较，所以两个对象自然是不相等的。正确的比较操作应该使用 `equals` 方法。
- i1 是包装类，i4 是基础数据类型，使用 `==` 时，Java 会将两个 i1 这个包装类对象自动拆箱为一个 `int` 值，再代入到 `==` 运算表达式中计算；最终，相当于两个 `int` 进行比较，由于值相同，所以结果相等。

【示例】包装类判等问题

```java
Integer a = 127; //Integer.valueOf(127)
Integer b = 127; //Integer.valueOf(127)
log.info("\nInteger a = 127;\nInteger b = 127;\na == b ? {}", a == b);    // true

Integer c = 128; //Integer.valueOf(128)
Integer d = 128; //Integer.valueOf(128)
log.info("\nInteger c = 128;\nInteger d = 128;\nc == d ? {}", c == d);   //false
//设置-XX:AutoBoxCacheMax=1000再试试

Integer e = 127; //Integer.valueOf(127)
Integer f = new Integer(127); //new instance
log.info("\nInteger e = 127;\nInteger f = new Integer(127);\ne == f ? {}", e == f);   //false

Integer g = new Integer(127); //new instance
Integer h = new Integer(127); //new instance
log.info("\nInteger g = new Integer(127);\nInteger h = new Integer(127);\ng == h ? {}", g == h);  //false

Integer i = 128; //unbox
int j = 128;
log.info("\nInteger i = 128;\nint j = 128;\ni == j ? {}", i == j); //true
```

通过运行结果可以看到，虽然看起来永远是在对 127 和 127、128 和 128 判等，但 == 却并非总是返回 true。

#### 装箱、拆箱应用注意点

1. 装箱操作会创建对象，频繁的装箱操作会造成不必要的内存消耗，影响性能。所以**应该尽量避免装箱。**
2. 基础数据类型的比较操作使用 `==`，包装类的比较操作使用 `equals` 方法。

## 4. 判等问题

Java 中，通常使用 `equals` 或 `==` 进行判等操作。`equals` 是方法而 `==` 是操作符。此外，二者使用也是有区别的：

- 对**基本类型**，比如 `int`、`long`，进行判等，**只能使用 `==`，比较的是字面值**。因为基本类型的值就是其数值。
- 对**引用类型**，比如 `Integer`、`Long` 和 `String`，进行判等，**需要使用 `equals` 进行内容判等**。因为引用类型的直接值是指针，使用 `==` 的话，比较的是指针，也就是两个对象在内存中的地址，即比较它们是不是同一个对象，而不是比较对象的内容。

### 4.1. 包装类的判等

我们通过一个示例来深入研究一下判等问题。

【示例】包装类的判等

```java
Integer a = 127; //Integer.valueOf(127)
Integer b = 127; //Integer.valueOf(127)
log.info("\nInteger a = 127;\nInteger b = 127;\na == b ? {}", a == b);    // true

Integer c = 128; //Integer.valueOf(128)
Integer d = 128; //Integer.valueOf(128)
log.info("\nInteger c = 128;\nInteger d = 128;\nc == d ? {}", c == d);   //false
//设置-XX:AutoBoxCacheMax=1000再试试

Integer e = 127; //Integer.valueOf(127)
Integer f = new Integer(127); //new instance
log.info("\nInteger e = 127;\nInteger f = new Integer(127);\ne == f ? {}", e == f);   //false

Integer g = new Integer(127); //new instance
Integer h = new Integer(127); //new instance
log.info("\nInteger g = new Integer(127);\nInteger h = new Integer(127);\ng == h ? {}", g == h);  //false

Integer i = 128; //unbox
int j = 128;
log.info("\nInteger i = 128;\nint j = 128;\ni == j ? {}", i == j); //true
```

第一个案例中，编译器会把 Integer a = 127 转换为 Integer.valueOf(127)。查看源码可以发现，这个转换在内部其实做了缓存，使得两个 Integer 指向同一个对象，所以 == 返回 true。

```java
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}
```

第二个案例中，之所以同样的代码 128 就返回 false 的原因是，默认情况下会缓存[-128,127]的数值，而 128 处于这个区间之外。设置 JVM 参数加上 -XX:AutoBoxCacheMax=1000 再试试，是不是就返回 true 了呢？

```java
private static class IntegerCache {
    static final int low = -128;
    static final int high;
    static final Integer cache[];

    static {
        // high value may be configured by property
        int h = 127;
        String integerCacheHighPropValue =
            sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
        if (integerCacheHighPropValue != null) {
            try {
                int i = parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                // Maximum array size is Integer.MAX_VALUE
                h = Math.min(i, Integer.MAX_VALUE - (-low) -1);
            } catch( NumberFormatException nfe) {
                // If the property cannot be parsed into an int, ignore it.
            }
        }
        high = h;

        cache = new Integer[(high - low) + 1];
        int j = low;
        for(int k = 0; k < cache.length; k++)
            cache[k] = new Integer(j++);

        // range [-128, 127] must be interned (JLS7 5.1.7)
        assert IntegerCache.high >= 127;
    }

    private IntegerCache() {}
}
```

第三和第四个案例中，New 出来的 Integer 始终是不走缓存的新对象。比较两个新对象，或者比较一个新对象和一个来自缓存的对象，结果肯定不是相同的对象，因此返回 false。

第五个案例中，我们把装箱的 Integer 和基本类型 int 比较，前者会先拆箱再比较，比较的肯定是数值而不是引用，因此返回 true。

> 【总结】综上，我们可以得出结论：**包装类需要使用 `equals` 进行内容判等，而不能使用 `==`**。

### 4.2. String 的判等

```java
String a = "1";
String b = "1";
log.info("\nString a = \"1\";\nString b = \"1\";\na == b ? {}", a == b); //true

String c = new String("2");
String d = new String("2");
log.info("\nString c = new String(\"2\");\nString d = new String(\"2\");\nc == d ? {}", c == d); //false

String e = new String("3").intern();
String f = new String("3").intern();
log.info("\nString e = new String(\"3\").intern();\nString f = new String(\"3\").intern();\ne == f ? {}", e == f); //true

String g = new String("4");
String h = new String("4");
log.info("\nString g = new String(\"4\");\nString h = new String(\"4\");\ng == h ? {}", g.equals(h)); //true
```

在 JVM 中，当代码中出现双引号形式创建字符串对象时，JVM 会先对这个字符串进行检查，如果字符串常量池中存在相同内容的字符串对象的引用，则将这个引用返回；否则，创建新的字符串对象，然后将这个引用放入字符串常量池，并返回该引用。这种机制，就是字符串驻留或池化。

第一个案例返回 true，因为 Java 的字符串驻留机制，直接使用双引号声明出来的两个 String 对象指向常量池中的相同字符串。

第二个案例，new 出来的两个 String 是不同对象，引用当然不同，所以得到 false 的结果。

第三个案例，使用 String 提供的 intern 方法也会走常量池机制，所以同样能得到 true。

第四个案例，通过 equals 对值内容判等，是正确的处理方式，当然会得到 true。

虽然使用 new 声明的字符串调用 intern 方法，也可以让字符串进行驻留，但在业务代码中滥用 intern，可能会产生性能问题。

【示例】String#intern 性能测试

```
//-XX:+PrintStringTableStatistics
//-XX:StringTableSize=10000000
List<String> list = new ArrayList<>();
long begin = System.currentTimeMillis();
list = IntStream.rangeClosed(1, 10000000)
    .mapToObj(i -> String.valueOf(i).intern())
    .collect(Collectors.toList());
System.out.println("size:" + list.size());
System.out.println("time:" + (System.currentTimeMillis() - begin));
```

上面的示例执行时间会比较长。原因在于：字符串常量池是一个固定容量的 Map。如果容量太小（Number of buckets=60013）、字符串太多（1000 万个字符串），那么每一个桶中的字符串数量会非常多，所以搜索起来就很慢。输出结果中的 Average bucket size=167，代表了 Map 中桶的平均长度是 167。

解决方法是：设置 JVM 参数 -XX:StringTableSize=10000000，指定更多的桶。

为了方便观察，可以在启动程序时设置 JVM 参数 -XX:+PrintStringTableStatistic，程序退出时可以打印出字符串常量表的统计信息。

执行结果比不设置 -XX:StringTableSize 要快很多。

> 【总结】**没事别轻易用 intern，如果要用一定要注意控制驻留的字符串的数量，并留意常量表的各项指标**。

### 4.3. 实现 equals

如果看过 Object 类源码，你可能就知道，equals 的实现其实是比较对象引用

```java
public boolean equals(Object obj) {
    return (this == obj);
}
```

之所以 Integer 或 String 能通过 equals 实现内容判等，是因为它们都覆写了这个方法。

对于自定义类型，如果不覆写 equals 的话，默认就是使用 Object 基类的按引用的比较方式。

实现一个更好的 equals 应该注意的点：

- 考虑到性能，可以先进行指针判等，如果对象是同一个那么直接返回 true；
- 需要对另一方进行判空，空对象和自身进行比较，结果一定是 fasle；
- 需要判断两个对象的类型，如果类型都不同，那么直接返回 false；
- 确保类型相同的情况下再进行类型强制转换，然后逐一判断所有字段。

【示例】自定义 equals 示例

自定义类：

```java
class Point {
    private final int x;
    private final int y;
    private final String desc;
}
```

自定义 equals：

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Point that = (Point) o;
    return x == that.x && y == that.y;
}
```

### 4.4. hashCode 和 equals 要配对实现

```java
Point p1 = new Point(1, 2, "a");
Point p2 = new Point(1, 2, "b");

HashSet<PointWrong> points = new HashSet<>();
points.add(p1);
log.info("points.contains(p2) ? {}", points.contains(p2));
```

按照改进后的 equals 方法，这 2 个对象可以认为是同一个，Set 中已经存在了 p1 就应该包含 p2，但结果却是 false。

出现这个 Bug 的原因是，散列表需要使用 hashCode 来定位元素放到哪个桶。如果自定义对象没有实现自定义的 hashCode 方法，就会使用 Object 超类的默认实现，得到的两个 hashCode 是不同的，导致无法满足需求。

要自定义 hashCode，我们可以直接使用 Objects.hash 方法来实现。

```java
@Override
public int hashCode() {
    return Objects.hash(x, y);
}
```

### 4.5. compareTo 和 equals 的逻辑一致性

【示例】自定义 compareTo 出错示例

```java
@Data
@AllArgsConstructor
static class Student implements Comparable<Student> {

    private int id;
    private String name;

    @Override
    public int compareTo(Student other) {
        int result = Integer.compare(other.id, id);
        if (result == 0) { log.info("this {} == other {}", this, other); }
        return result;
    }
}
```

调用：

```java
List<Student> list = new ArrayList<>();
list.add(new Student(1, "zhang"));
list.add(new Student(2, "wang"));
Student student = new Student(2, "li");

log.info("ArrayList.indexOf");
int index1 = list.indexOf(student);
Collections.sort(list);
log.info("Collections.binarySearch");
int index2 = Collections.binarySearch(list, student);

log.info("index1 = " + index1);
log.info("index2 = " + index2);
```

binarySearch 方法内部调用了元素的 compareTo 方法进行比较；

- indexOf 的结果没问题，列表中搜索不到 id 为 2、name 是 li 的学生；
- binarySearch 返回了索引 1，代表搜索到的结果是 id 为 2，name 是 wang 的学生。

修复方式很简单，确保 compareTo 的比较逻辑和 equals 的实现一致即可。

```java
@Data
@AllArgsConstructor
static class StudentRight implements Comparable<StudentRight> {

    private int id;
    private String name;

    @Override
    public int compareTo(StudentRight other) {
        return Comparator.comparing(StudentRight::getName)
            .thenComparingInt(StudentRight::getId)
            .compare(this, other);
    }

}
```

### 4.6. 小心 Lombok 生成代码的“坑”

Lombok 的 @Data 注解会帮我们实现 equals 和 hashcode 方法，但是有继承关系时， Lombok 自动生成的方法可能就不是我们期望的了。

`@EqualsAndHashCode` 默认实现没有使用父类属性。为解决这个问题，我们可以手动设置 `callSuper` 开关为 true，来覆盖这种默认行为。

## 5. 数值计算

### 5.1. 浮点数计算问题

计算机是把数值保存在了变量中，不同类型的数值变量能保存的数值范围不同，当数值超过类型能表达的数值上限则会发生溢出问题。

```java
System.out.println(0.1 + 0.2); // 0.30000000000000004
System.out.println(1.0 - 0.8); // 0.19999999999999996
System.out.println(4.015 * 100); // 401.49999999999994
System.out.println(123.3 / 100); // 1.2329999999999999
double amount1 = 2.15;
double amount2 = 1.10;
System.out.println(amount1 - amount2); // 1.0499999999999998
```

上面的几个示例，输出结果和我们预期的很不一样。为什么会是这样呢？

出现这种问题的主要原因是，计算机是以二进制存储数值的，浮点数也不例外。Java 采用了 IEEE 754 标准实现浮点数的表达和运算，你可以通过这里查看数值转化为二进制的结果。

比如，0.1 的二进制表示为 0.0 0011 0011 0011… （0011 无限循环)，再转换为十进制就是 0.1000000000000000055511151231257827021181583404541015625。对于计算机而言，0.1 无法精确表达，这是浮点数计算造成精度损失的根源。

**浮点数无法精确表达和运算的场景，一定要使用 BigDecimal 类型**。

使用 BigDecimal 时，有个细节要格外注意。让我们来看一段代码：

```java
System.out.println(new BigDecimal(0.1).add(new BigDecimal(0.2)));
// Output: 0.3000000000000000166533453693773481063544750213623046875

System.out.println(new BigDecimal(1.0).subtract(new BigDecimal(0.8)));
// Output: 0.1999999999999999555910790149937383830547332763671875

System.out.println(new BigDecimal(4.015).multiply(new BigDecimal(100)));
// Output: 401.49999999999996802557689079549163579940795898437500

System.out.println(new BigDecimal(123.3).divide(new BigDecimal(100)));
// Output: 1.232999999999999971578290569595992565155029296875
```

为什么输出结果仍然不符合预期呢？

**使用 BigDecimal 表示和计算浮点数，且务必使用字符串的构造方法来初始化 BigDecimal**。

### 5.2. 浮点数精度和格式化

**浮点数的字符串格式化也要通过 BigDecimal 进行**。

```java
private static void wrong1() {
    double num1 = 3.35;
    float num2 = 3.35f;
    System.out.println(String.format("%.1f", num1)); // 3.4
    System.out.println(String.format("%.1f", num2)); // 3.3
}

private static void wrong2() {
    double num1 = 3.35;
    float num2 = 3.35f;
    DecimalFormat format = new DecimalFormat("#.##");
    format.setRoundingMode(RoundingMode.DOWN);
    System.out.println(format.format(num1)); // 3.35
    format.setRoundingMode(RoundingMode.DOWN);
    System.out.println(format.format(num2)); // 3.34
}

private static void right() {
    BigDecimal num1 = new BigDecimal("3.35");
    BigDecimal num2 = num1.setScale(1, BigDecimal.ROUND_DOWN);
    System.out.println(num2); // 3.3
    BigDecimal num3 = num1.setScale(1, BigDecimal.ROUND_HALF_UP);
    System.out.println(num3); // 3.4
}
```

### 5.3. BigDecimal 判等问题

```java
private static void wrong() {
    System.out.println(new BigDecimal("1.0").equals(new BigDecimal("1")));
}

private static void right() {
    System.out.println(new BigDecimal("1.0").compareTo(new BigDecimal("1")) == 0);
}
```

BigDecimal 的 equals 方法的注释中说明了原因，equals 比较的是 BigDecimal 的 value 和 scale，1.0 的 scale 是 1，1 的 scale 是 0，所以结果一定是 false。

**如果我们希望只比较 BigDecimal 的 value，可以使用 compareTo 方法**。

BigDecimal 的 equals 和 hashCode 方法会同时考虑 value 和 scale，如果结合 HashSet 或 HashMap 使用的话就可能会出现麻烦。比如，我们把值为 1.0 的 BigDecimal 加入 HashSet，然后判断其是否存在值为 1 的 BigDecimal，得到的结果是 false。

```java
Set<BigDecimal> hashSet1 = new HashSet<>();
hashSet1.add(new BigDecimal("1.0"));
System.out.println(hashSet1.contains(new BigDecimal("1")));//返回false
```

解决办法有两个：

第一个方法是，使用 TreeSet 替换 HashSet。TreeSet 不使用 hashCode 方法，也不使用 equals 比较元素，而是使用 compareTo 方法，所以不会有问题。

第二个方法是，把 BigDecimal 存入 HashSet 或 HashMap 前，先使用 stripTrailingZeros 方法去掉尾部的零，比较的时候也去掉尾部的 0，确保 value 相同的 BigDecimal，scale 也是一致的。

```java
Set<BigDecimal> hashSet2 = new HashSet<>();
hashSet2.add(new BigDecimal("1.0").stripTrailingZeros());
System.out.println(hashSet2.contains(new BigDecimal("1.000").stripTrailingZeros()));//返回true

Set<BigDecimal> treeSet = new TreeSet<>();
treeSet.add(new BigDecimal("1.0"));
System.out.println(treeSet.contains(new BigDecimal("1")));//返回true
```

### 5.4. 数值溢出

数值计算还有一个要小心的点是溢出，不管是 int 还是 long，所有的基本数值类型都有超出表达范围的可能性。

```java
long l = Long.MAX_VALUE;
System.out.println(l + 1); // -9223372036854775808
System.out.println(l + 1 == Long.MIN_VALUE); // true
```

**显然这是发生了溢出，而且是默默的溢出，并没有任何异常**。这类问题非常容易被忽略，改进方式有下面 2 种。

方法一是，考虑使用 Math 类的 addExact、subtractExact 等 xxExact 方法进行数值运算，这些方法可以在数值溢出时主动抛出异常。

```java
try {
    long l = Long.MAX_VALUE;
    System.out.println(Math.addExact(l, 1));
} catch (Exception ex) {
    ex.printStackTrace();
}
```

方法二是，使用大数类 BigInteger。BigDecimal 是处理浮点数的专家，而 BigInteger 则是对大数进行科学计算的专家。

```java
BigInteger i = new BigInteger(String.valueOf(Long.MAX_VALUE));
System.out.println(i.add(BigInteger.ONE).toString());

try {
    long l = i.add(BigInteger.ONE).longValueExact();
} catch (Exception ex) {
    ex.printStackTrace();
}
```

## 6.double转string

- double转string方式一：Double.toString(d)

  ```
  public class DoubleConvertToString {
  
      public static void doubleToString(double d){
          String s = Double.toString(d);
          System.out.println(s);
          if(s.equals("2016010")){
              System.out.println("yes!");
          }else {
              System.out.println("no!");
          }
      }
  
      public static void main(String[] args) {
          doubleToString(2016010);
      }
  }
  //整数情况下，低于8位输出结果是
  //2016010.0
  //no!
  //大于等于8位数时，显示成科学计数法的形式
  //2.0160101E7
  //no!
  ```

  toString()方式使用时存在此坑，尽量不要使用；

- double转string方式二：BigDecimal(d);

  ```
  public static void doubleToString2(double d){
          BigDecimal bd = new BigDecimal(d);
          String s = bd.toString();
          System.out.println(s);
      }
  //这个方法数据是整数时，都能正常转换成字符串，但是当数据为小数时，精度会更加准确，后面会展示更多小数位数，如：
  //doubleToString2(20160.00333);
  //20160.0033299999995506368577480316162109375
  ```

- double转string方式三：NumberFormat.format(d);

  ```
  public static void doubleToString3(double d){
          NumberFormat nf = NumberFormat.getInstance();
          nf.setGroupingUsed(false);
          String format = nf.format(d);
          System.out.println(format);
      }
  //此方法的输出格式和输入的格式一样
  ```

- double转string方法四：DecimalFormat().format(d);

  ```
  public static void doubleToString4(double d){
          DecimalFormat df = new DecimalFormat();
          df.setGroupingUsed(false);
          String format = df.format(d);
          System.out.println(format);
      }
  //DecimalFormat是NumberFormat的子类
  ```

  









## X.面试题

### X.1.说说基本类型和包装类型的区别

Java 的每个基本类型都对应了一个包装类型，比如说 int 的包装类型为 Integer，double 的包装类型为 Double。基本类型和包装类型的区别主要有以下 4 点。

#### X.1.1.包装类型可以为 null，而基本类型不可以

别小看这一点区别，它使得包装类型可以应用于 POJO 中，而基本类型则不行。

POJO 是什么呢？这里稍微说明一下。

POJO 的英文全称是 `Plain Ordinary Java Object`，翻译一下就是，简单无规则的 Java 对象，只有属性字段以及 setter 和 getter 方法，示例如下。

```
class Writer {
    private Integer age;
    private String name;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

和 POJO 类似的，还有数据传输对象 DTO（Data Transfer Object，泛指用于展示层与服务层之间的数据传输对象）、视图对象 VO（View Object，把某个页面的数据封装起来）、持久化对象 PO（Persistant Object，可以看成是与数据库中的表映射的 Java 对象）。

那为什么 POJO 的属性必须要用包装类型呢？

《阿里巴巴 Java 开发手册》上有详细的说明，我们来大声朗读一下（预备，起）。

> 数据库的查询结果可能是 null，如果使用基本类型的话，因为要自动拆箱（将包装类型转为基本类型，比如说把 Integer 对象转换成 int 值），就会抛出 `NullPointerException` 的异常。

#### X.1.2.包装类型可用于泛型，而基本类型不可以

泛型不能使用基本类型，因为使用基本类型时会编译出错。

```
List<int> list = new ArrayList<>(); // 提示 Syntax error, insert "Dimensions" to complete ReferenceType
List<Integer> list = new ArrayList<>();
```

为什么呢？因为泛型在编译时会进行类型擦除，最后只保留原始类型，而原始类型只能是 Object 类及其子类——基本类型是个特例。

#### X.1.3.基本类型比包装类型更高效

基本类型在栈中直接存储的具体数值，而包装类型则存储的是堆中的引用。

![图片](https://mmbiz.qpic.cn/mmbiz_png/z40lCFUAHpmDKLIZbsNufzxCDu3rjNseViaiaz7DwD79KYfZUSmtLeT5oMiasUEzgGzicswibdMmznR7mqmceicKF2WQ/640?wx_fmt=png&tp=webp&wxfrom=5&wx_lazy=1&wx_co=1)

很显然，相比较于基本类型而言，包装类型需要占用更多的内存空间。假如没有基本类型的话，对于数值这类经常使用到的数据来说，每次都要通过 new 一个包装类型就显得非常笨重。

#### X.1.4.两个包装类型的值可以相同，但却不相等

两个包装类型的值可以相同，但却不相等——这句话怎么理解呢？来看一段代码就明明白白了。

```
Integer chenmo = new Integer(10);
Integer wanger = new Integer(10);

System.out.println(chenmo == wanger); // false
System.out.println(chenmo.equals(wanger )); // true
```

两个包装类型在使用“==”进行判断的时候，判断的是其指向的地址是否相等。chenmo 和 wanger 两个变量使用了 new 关键字，导致它们在“==”的时候输出了 false。

而 `chenmo.equals(wanger)` 的输出结果为 true，是因为 equals 方法内部比较的是两个 int 值是否相等。源码如下。

```
private final int value;

public int intValue() {
    return value;
}
public boolean equals(Object obj) {
    if (obj instanceof Integer) {
        return value == ((Integer)obj).intValue();
    }
    return false;
}
```

瞧，虽然 chenmo 和 wanger 的值都是 10，但他们并不相等。换句话说就是：**将“==”操作符应用于包装类型比较的时候，其结果很可能会和预期的不符**。



### X.2.BigDecimal一定不会丢失精度吗？

我们基本已经形成了常识，需要用到金钱的地方要用BigDecimal而不是其他，而我们也都知道浮点型变量在进行计算的时候会出现丢失精度的问题。

那么，你知道其实BigDecimal也会丢失精度吗？而使用BigDecimal的背后又有什么值得去探究的地方吗？今天，告诉你，知其然，也知其所以然。

如下一段代码：

```
System.out.println(0.05 + 0.01);  
System.out.println(1.0 - 0.42);  
System.out.println(4.015 * 100);  
System.out.println(123.3 / 100);  
```

输出：
0.060000000000000005
0.5800000000000001
401.49999999999994
1.2329999999999999

可以看到在Java中进行浮点数运算的时候，会出现丢失精度的问题。那么我们如果在进行商品价格计算的时候，就会出现问题。

很有可能造成我们手中有0.06元，却无法购买一个0.05元和一个0.01元的商品。

因为如上所示，他们两个的总和为0.060000000000000005。

这无疑是一个很严重的问题，尤其是当电商网站的并发量上去的时候，出现的问题将是巨大的。可能会导致无法下单，或者对账出现问题。所以接下来我们就可以使用Java中的BigDecimal类来解决这类问题。

**普及一下：**

Java中float的精度为6-7位有效数字。double的精度为15-16位。

**X.2.1.API**

构造器：

```
构造器                   描述
BigDecimal(int)       创建一个具有参数所指定整数值的对象。
BigDecimal(double)    创建一个具有参数所指定双精度值的对象。
BigDecimal(long)      创建一个具有参数所指定长整数值的对象。
BigDecimal(String)    创建一个具有参数所指定以字符串表示的数值的对象。
```

函数：

```
方法                    描述
add(BigDecimal)       BigDecimal对象中的值相加，然后返回这个对象。
subtract(BigDecimal)  BigDecimal对象中的值相减，然后返回这个对象。
multiply(BigDecimal)  BigDecimal对象中的值相乘，然后返回这个对象。
divide(BigDecimal)    BigDecimal对象中的值相除，然后返回这个对象。
toString()            将BigDecimal对象的数值转换成字符串。
doubleValue()         将BigDecimal对象中的值以双精度数返回。
floatValue()          将BigDecimal对象中的值以单精度数返回。
longValue()           将BigDecimal对象中的值以长整数返回。
intValue()            将BigDecimal对象中的值以整数返回。
```

由于一般的数值类型，例如double不能准确的表示16位以上的数字。

**X.2.2.BigDecimal精度也丢失**

我们在使用BigDecimal时，使用它的BigDecimal(String)构造器创建对象才有意义。其他的如BigDecimal b = new BigDecimal(1)这种，还是会发生精度丢失的问题。如下代码：

```
BigDecimal a = new BigDecimal(1.01);
BigDecimal b = new BigDecimal(1.02);
BigDecimal c = new BigDecimal("1.01");
BigDecimal d = new BigDecimal("1.02");
System.out.println(a.add(b));
System.out.println(c.add(d));
```

输出：
2.0300000000000000266453525910037569701671600341796875
2.03

可见论丢失精度BigDecimal显的更为过分。但是使用Bigdecimal的BigDecimal(String)构造器的变量在进行运算的时候却没有出现这种问题。

究其原因计算机组成原理里面都有，它们的编码决定了这样的结果。

long可以准确存储19位数字，而double只能准备存储16位数字。

double由于有exp位，可以存16位以上的数字，但是需要以低位的不精确作为代价。如果需要高于19位数字的精确存储，则必须用BigInteger来保存，当然会牺牲一些性能。

所以我们一般使用BigDecimal来解决商业运算上丢失精度的问题的时候，声明BigDecimal对象的时候一定要使用它构造参数为String的类型的构造器。

同时这个原则Effective Java和MySQL 必知必会中也都有提及。float和double只能用来做科学计算和工程计算。商业运算中我们要使用BigDecimal。

而且我们从源码的注释中官方也给出了说明，如下是BigDecimal类的double类型参数的构造器上的一部分注释说明：

```
* The results of this constructor can be somewhat unpredictable.  
     * One might assume that writing {@codenew BigDecimal(0.1)} in  
     * Java creates a {@code BigDecimal} which is exactly equal to  
     * 0.1 (an unscaled value of 1, with a scale of 1), but it is  
     * actually equal to  
     * 0.1000000000000000055511151231257827021181583404541015625.  
     * This is because 0.1 cannot be represented exactly as a  
     * {@codedouble} (or, for that matter, as a binary fraction of  
     * any finite length).  Thus, the value that is being passed  
     * <i>in</i> to the constructor is not exactly equal to 0.1,  
     * appearances notwithstanding.  
       ……  
        * When a {@codedouble} must be used as a source for a  
     * {@code BigDecimal}, note that this constructor provides an  
     * exact conversion; it does not give the same result as  
     * converting the {@codedouble} to a {@code String} using the  
     * {@link Double#toString(double)} method and then using the  
     * {@link #BigDecimal(String)} constructor.  To get that result,  
     * use the {@codestatic} {@link #valueOf(double)} method.  
     * </ol>  
public BigDecimal(double val) {  
    this(val,MathContext.UNLIMITED);  
}  
```

第一段也说的很清楚它只能计算的无限接近这个数，但是无法精确到这个数。

第二段则说，如果要想准确计算这个值，那么需要把double类型的参数转化为String类型的。并且使用BigDecimal(String)这个构造方法进行构造。去获取结果。

**X.2.3.正确运用BigDecimal**

另外，BigDecimal所创建的是对象，我们不能使用传统的+、-、*、/等算术运算符直接对其对象进行数学运算，而必须调用其相对应的方法。方法中的参数也必须是BigDecimal的对象，由刚才我们所罗列的API也可看出。

在一般开发过程中，我们数据库中存储的数据都是float和double类型的。在进行拿来拿去运算的时候还需要不断的转化，这样十分的不方便。这里我写了一个工具类：

```
/**  
 * @author: Ji YongGuang.  
 * @date: 19:50 2017/12/14.  
 */  
publicclass BigDecimalUtil {  

    private BigDecimalUtil() {  

    }  

    public static BigDecimal add(double v1, double v2) {// v1 + v2  
        BigDecimal b1 = new BigDecimal(Double.toString(v1));  
        BigDecimal b2 = new BigDecimal(Double.toString(v2));  
        return b1.add(b2);  
    }  

    public static BigDecimal sub(double v1, double v2) {  
        BigDecimal b1 = new BigDecimal(Double.toString(v1));  
        BigDecimal b2 = new BigDecimal(Double.toString(v2));  
        return b1.subtract(b2);  
    }  

    public static BigDecimal mul(double v1, double v2) {  
        BigDecimal b1 = new BigDecimal(Double.toString(v1));  
        BigDecimal b2 = new BigDecimal(Double.toString(v2));  
        return b1.multiply(b2);  
    }  

    public static BigDecimal div(double v1, double v2) {  
        BigDecimal b1 = new BigDecimal(Double.toString(v1));  
        BigDecimal b2 = new BigDecimal(Double.toString(v2));  
        // 2 = 保留小数点后两位   ROUND_HALF_UP = 四舍五入  
        return b1.divide(b2, 2, BigDecimal.ROUND_HALF_UP);// 应对除不尽的情况  
    }  
}  
```

该工具类提供了double类型的基本的加减乘除运算。直接调用即可。



### X.3.商业计算怎样才能保证精度不丢失

#### X.3.1.前言

很多系统都有「处理金额」的需求，比如电商系统、财务系统、收银系统，等等。只要和钱扯上关系，就不得不打起十二万分精神来对待，一分一毫都不能出错，否则对系统和用户来说都是灾难。

保证金额的准确性主要有两个方面：**溢出**和**精度**。溢出是指存储数据的空间得充足，不能金额较大就存储不下了。精度是指计算金额时不能有偏差，多一点少一点都不行。

溢出问题大家都知道如何解决，选择位数长的数值类型即可，即不用 `float` 用 `double` 。而精度问题，`double` 就无法解决了，因为浮点数会导致精度丢失。

我们来直观感受一下精度丢失：

```java
double money = 1.0 - 0.9;
```

这个运算结果谁都知道该为 `0.1`，然而实际结果却是 `0.09999999999999998`。出现这个现象是因为计算机底层是二进制运算，而二进制并不能精准表示十进制小数。所以在商业计算等精确计算中要使用其他数据类型来保证精度不丢失，一定不要使用浮点数。

#### X.3.2.解决方案

有两种数据类型可以满足商业计算的需求，第一个自然是专为商业计算而设计的 **Decimal** 类型，第二个则是**定长整数**。

##### X.3.2.1.Decimal

关于数据类型的选择，一要考虑数据库，二要考虑编程语言。即数据库中用什么类型来**存储数据**，代码中用什么类型来**处理数据**。

数据库层面自然是用 `decimal` 类型，因为该类型不存在精度损失的情况，用它来进行商业计算再合适不过。

将字段定义为 `decimal` 的语法为 `decimal(M,N)`，`M` 代表存储多少位，`N` 代表小数存储多少位。假设 `decimal(20,2)`，则代表一共存储 20 位数值，其中小数占 2 位。

我们新建一张用户表，字段很简单就两个，主键和余额：

![image-20210320185027003](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320185027.png)

这里小数位置保留 2 点，代表金额只存储到**分**，实际项目中存储到什么单位得根据业务需求来定，都是可以的。

数据库层面搞定了咱们来看代码层面，在 Java 中对应数据库 `decimal` 的是 `java.math.BigDecimal`类型，它自然也能保证精度完全准确。

要创建`BigDecimal`主要有三种方法：

```java
BigDecimal d1 = new BigDecimal(0.1); // BigDecimal(double val)
BigDecimal d2 = new BigDecimal("0.1"); // BigDecimal(String val)
BigDecimal d3 = BigDecimal.valueOf(0.1); // static BigDecimal valueOf(double val)
```

前面两个是构造函数，后面一个是静态方法。这三种方法都非常方便，但第一种方法禁止使用！看一下这三个对象各自的打印结果就知道为什么了：

```
d1: 0.1000000000000000055511151231257827021181583404541015625
d2: 0.1
d3: 0.1
```

第一种方法通过构造函数传入 `double` 类型的参数并不能精确地获取到值，若想正确的创建 `BigDecimal`，要么将 `double` 转换为字符串然后调用构造方法，要么直接调用静态方法。事实上，静态方法内部也是将 `double` 转换为字符串然后调用的构造方法：

![image-20210320185056344](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320185056.png)

如果是从数据库中查询出小数值，或者前端传递过来小数值，数据会准确映射成 `BigDecimal` 对象，这一点我们不用操心。

说完创建，接下来就要说最重要的数值运算。运算无非就是加减乘除，这些 `BigDecimal` 都提供了对应的方法：

```java
BigDecimal add(BigDecimal); // 加
BigDecimal subtract(BigDecimal); // 减
BigDecimal multiply(BigDecimal); // 乘
BigDecimal divide(BigDecimal); // 除
```

`BigDecimal` 是不可变对象，意思就是这些操作都不会改变原有对象的值，方法执行完毕只会返回一个新的对象。若要运算后更新原有值，只能重新赋值：

```java
d1 = d1.subtract(d2);
```

口说无凭，我们来验证一下精度是否会丢失 ：

```java
BigDecimal d1 = new BigDecimal("1.0");
BigDecimal d2 = new BigDecimal("0.9");
System.out.println(d1.subtract(d2));
```

输出结果毫无疑问为 `0.1`。

代码方面已经能保证精度不会丢失，但数学方面**除法**可能会出现除不尽的情况。比如我们运算 `10` 除以 `3`，会抛出如下异常：

![image-20210320185137935](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320185138.png)

为了解决除不尽后导致的无穷小数问题，我们需要人为去控制小数的精度。除法运算还有一个方法就是用来控制精度的：

```java
BigDecimal divide(BigDecimal divisor, int scale, int roundingMode)
```

`scale` 参数表示运算后保留几位小数，`roundingMode` 参数表示计算小数的方式。

```java
BigDecimal d1 = new BigDecimal("1.0");
BigDecimal d2 = new BigDecimal("3");
System.out.println(d1.divide(d2, 2, RoundingMode.DOWN)); // 小数精度为2，多余小数直接舍去。输出结果为0.33
```

用 `RoundingMode` 枚举能够方便地指定小数运算方式，除了直接舍去，还有四舍五入、向上取整等多种方式，根据具体业务需求指定即可。

> 注意，小数精度尽量在代码中控制，不要通过数据库来控制。数据库中默认采用四舍五入的方式保留小数精度。
>
> 比如数据库中设置的小数精度为2，我存入 `0.335`，那么最终存储的值就会变为 `0.34`。

我们已经知道如何创建和运算 `BigDecimal` 对象，只剩下最后一个操作：比较。因为其不是基本数据类型，用双等号 `==` 肯定是不行的，那我们来试试用 `equals`比较：

```java
BigDecimal d1 = new BigDecimal("0.33");
BigDecimal d2 = new BigDecimal("0.3300");
System.out.println(d1.equals(d2)); // false
```

输出结果为 `false`，因为 `BigDecimal` 的 `equals` 方法不光会比较值，还会比较精度，就算值一样但精度不一样结果也是 `false`。若想判断值是否一样，需要使用`int compareTo(BigDecimal val)`方法：

```java
BigDecimal d1 = new BigDecimal("0.33");
BigDecimal d2 = new BigDecimal("0.3300");
System.out.println(d1.compareTo(d2) == 0); // true
```

`d1` 大于 `d2`，返回 `1`；

`d1` 小于 `d2`，返回 `-1`；

两值相等，返回 `0`。

`BigDecimal` 的用法就介绍到这，我们接下来看第二种解决方案。

##### X.3.2.2.定长整数

定长整数，顾名思义就是固定（小数）长度的整数。它只是一个概念，并不是新的数据类型，我们使用的还是普通的整数。

金额好像理所应当有小数，但稍加思考便会发觉小数并非是必须的。之前我们演示的金额单位是**元**，`1.55` 就是一元五角五分。那如果我们单位是**角**，一元五角五分的值就会变成 `15.5`。如果再将单位缩小到**分**，值就为 `155`。没错，只要达到最小单位，小数完全可以省略！这个最小单位根据业务需求来定，比如系统要求精确到**厘**，那么值就是`1550`。当然，一般精确到分就可以了，咱们接下来演示单位都是分。

咱们现在新建一个字段，类型为 `bigint`，单位为分：

![image-20210320185157094](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320185157.png)

代码中对应的数据类型自然是 `Long`。基本类型的数值运算我们是再熟悉不过的了，直接使用运算操作符即可：

```java
long d1 = 10000L; // 100元
d1 += 500L; // 加五元
d1 -= 500L; // 减五元
```

加和减没什么好说的，乘和除可能会出现小数的情况，比如某个商品打八折，运算就是乘以 `0.8`：

```java
long d1 = 2366L; // 23.66元
double result = d1 * 0.8; // 打八折，运算后结果为1892.8
d1 = (long)result; // 转换为整数，舍去所有小数，值为1892。即18.92元
```

进行小数运算，类型自然而然就会变为浮点数，所以我们还要将浮点数转换为整数。

强转会将所有小数舍去，**这个舍去并不代表精度丢失**。业务要求最小单位是什么，就只保留什么，低于分的单位我们压根没必要保存。这一点和 `BigDecimal` 是一致的，如果系统中只需要到分，那小数精度就为 `2`， 剩余的小数都舍去。

不过有些业务计算可能要求四舍五入等其他操作，这一点我们可以通过 `Math`类来完成：

```java
long d1 = 2366L; // 23.66元
double result = d1 * 0.8; // 运算后结果为1892.8
d1 = (long)result; // 强转舍去所有小数，值为1892
d1 = (long)Math.ceil(result); // 向上取整，值为1893
d1 = (long)Math.round(result); // 四舍五入，值为1893
...
```

再来看除法运算。当整数除以整数时，会自动舍去所有小数：

```java
long d1 = 2366L;
long result = d1 / 3; // 正确的值本应该为788.6666666666666，舍去所有小数，最终值为788
```

如果要进行四舍五入等其他小数操作，则运算时先进行浮点数运算，然后再转换成整数：

```java
long d1 = 2366L;
double result = d1 / 3.0; // 注意，这里除以不是 3，而是 3.0 浮点数
d1 = (long)Math.round(result); // 四射勿入，最终值为789，即7.89元
```

虽说数据库存储和代码运算都是整数，但前端显示时若还是以**分**为单位就对用户不太友好了。所以后端将值传递给前端后，前端需要自行将值除以 `100`，以**元**为单位展示给用户。然后前端传值给后端时，还是以约定好的整数传递。

![image-20210320185227233](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320185227.png)































