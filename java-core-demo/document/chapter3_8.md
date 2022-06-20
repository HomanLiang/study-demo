[toc]



# Java Steam



## 1.使用技巧

### 1.1.Stream

使用这个方法创建一个 Stream 对象。

```java
new ArrayList<>().stream()
```

### 1.2.Filter

过滤器，里面传递一个函数，这个函数的返回结果如果为 true 则保留这个元素，否则的话丢弃这个元素。

```java
        stringCollection
                .stream()
                .filter((s) -> s.startsWith("a"))
                .forEach(System.out::println);
```

### 1.3.Foreach

遍历，消费。

```java
        stringCollection
                .stream()
                .filter((s) -> s.startsWith("a"))
                .forEach(System.out::println);
```

### 1.4.Map

这个功能也是遍历，但是他是有返回值的，而上面的 Foreach 是没有返回值的，仅仅是单纯的消费。而且 Foreach 不能够链式调用，因为没有返回值，但是 Map 没问题。

```java
        stringCollection
                .stream()
                .map(String::toUpperCase)
                .sorted(Comparator.reverseOrder())
                .forEach(System.out::println);
```

### 1.5.Sorted

这个方法是用来排序的，里面传递的函数就是一个比较器，也可以不传递参数，使用默认的就好。

```java
        stringCollection
                .stream()
                .sorted(( x, y)-> y.length()-x.length())
                .filter((s) -> s.startsWith("a"))
                .forEach(System.out::println);
```

### 1.6.Match

根据在给定的 stream 对象中是否含有指定内容返回 true 或者 false 。

具体的有：

- allMatch
- anyMatch
- noneMatch

```x86asm
        boolean anyStartsWithA = stringCollection
                .stream()
                .anyMatch((s) -> s.startsWith("a"));

        boolean allStartsWithA = stringCollection
                .stream()
                .allMatch((s) -> s.startsWith("a"));

        boolean noneStartsWithZ = stringCollection
                .stream()
                .noneMatch((s) -> s.startsWith("z"));
```

### 1.7.count

计算集合中的元素的个数。

```java
long startsWithB = stringCollection
        .stream()
        .filter((s) -> s.startsWith("b"))
        .count();
```

### 1.8.reduce

这个函数就是类似于斐波那契数列，每次传递的参数是上一次的结果和从集合中取出的新元素。第一次默认取出了第一个元素和第二个元素。

简单的例子就是，第一次取出 0,1 第二次取出 第一次reduce的结果作为第一个参数，取出 2 作为第二个参数，以此类推。

```java
Optional<String> reduced =
        stringCollection
                .stream()
                .sorted()
                .reduce((s1, s2) -> s1 + "#" + s2);
```

### 1.9.parallelStream

并行的 steam 流，可以进行并行处理，这样会效率更高。在使用stream.foreach时这个遍历没有线程安全问题，但是使用parallelStream就会有线程安全问题，所有在parallelStream里面使用的外部变量，比如集合一定要使用线程安全集合，不然就会引发多线程安全问题。如果说需要保证安全性需要使用 reduce 和 collect，不过这个用起来超级麻烦！！！

```java
long count = values.parallelStream().sorted().count();
```

### 1.10.IntStream.range(a,b)

可以直接生成 从 a 到 b 的整数这里还是遵循编程语言的大多数约定，那就是含头不含尾。

```java
IntStream.range(0, 10)
    .forEach(System.out::println);
```

输出的结果是

```undefined
0
1
2
3
4
5
6
7
8
9
```

### 1.11.new Random().ints()

获取一系列的随机值，这个接口出来的数据是连续不断的，所以需要用limit来限制一下。

```java
new Random().ints().limit(10).forEach(System.out::println);
```

### 1.12.Supplier

```Java
Supplier<String> stringSupplier=String::new;
stringSupplier.get();
```

该接口就一个抽象方法get方法,不用传入任何参数,直接返回一个泛型T的实例.就如同无参构造一样

### 1.13.Consumer

#### 1.13.1. accept方法

 该函数式接口的唯一的抽象方法,接收一个参数,没有返回值.

#### 1.13.2. andThen方法

 在执行完调用者方法后再执行传入参数的方法.

```java
public class ConsumerTest {
 public static void main(String[] args) {
 Consumer<Integer> consumer = (x) -> {
 int num = x * 2;
 System.out.println(num);
        };
 Consumer<Integer> consumer1 = (x) -> {
 int num = x * 3;
 System.out.println(num);
        };
 consumer.andThen(consumer1).accept(10);
    }
```

先执行了 consumer.accept(10) 然后执行了 consumer1.accept(10)

### 1.14.ifPresent

针对一个optional 如果有值的话就执行否则不执行。

```java
IntStream
    .builder()
    .add(1)
    .add(3)
    .add(5)
    .add(7)
    .add(11)
    .build()
    .average()
    .ifPresent(System.out::println);
```

average 执行结果就是一个 optional

### 1.15.Collect

他有两种调用方式

```java
  <R> R collect(Supplier<R> supplier,
 BiConsumer<R, ? super T> accumulator,
 BiConsumer<R, R> combiner);

 <R, A> R collect(Collector<? super T, A, R> collector);
```

下面主要介绍一下这两种方式的使用方法：

#### 1.15.1. 函数

第一种调用方式的接口如下

```java
  <R> R collect(Supplier<R> supplier,
 BiConsumer<R, ? super T> accumulator,
 BiConsumer<R, R> combiner);
```

- supplier 这个参数就是提供一个容器，可以看到最后 collect 操作的结果是一个 R 类型变量，而 supplier 接口最后需要返回的也是一个 R 类型的变量，所以说这里返回的是收集元素的容器。
- accumulator 参数，看到这个函数的定义是传入一个 R 容器，后面则是 T 类型的元素，需要将这个 T 放到 R 容器中，即这一步是用来将元素添加到容器中的操作。
- conbiner 这个参数是两个容器，即当出现多个容器的时候容器如何进行聚合。

一个简单的例子：

```java
String concat = stringStream.collect(StringBuilder::new, StringBuilder::append,StringBuilder::append).toString();
//等价于上面,这样看起来应该更加清晰
String concat = stringStream.collect(() -> new StringBuilder(),(l, x) -> l.append(x), (r1, r2) -> r1.append(r2)).toString();
```

#### 1.15.2. Collector 接口

第二种方案是更高级的用法采用了 Collector 接口：

```java
 <R, A> R collect(Collector<? super T, A, R> collector);
```

可以看到他返回的还是一个 R 类型的变量，也就是容器。

`Collector`接口是使得`collect`操作强大的终极武器,对于绝大部分操作可以分解为旗下主要步骤,**提供初始容器->加入元素到容器->并发下多容器聚合->对聚合后结果进行操作**

```java
static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
 private final Supplier<A> supplier;
 private final BiConsumer<A, T> accumulator;
 private final BinaryOperator<A> combiner;
 private final Function<A, R> finisher;
 private final Set<Characteristics> characteristics;

 CollectorImpl(Supplier<A> supplier,
 BiConsumer<A, T> accumulator,
 BinaryOperator<A> combiner,
 Function<A,R> finisher,
 Set<Characteristics> characteristics) {
 this.supplier = supplier;
 this.accumulator = accumulator;
 this.combiner = combiner;
 this.finisher = finisher;
 this.characteristics = characteristics;
        }

 CollectorImpl(Supplier<A> supplier,
 BiConsumer<A, T> accumulator,
 BinaryOperator<A> combiner,
 Set<Characteristics> characteristics) {
 this(supplier, accumulator, combiner, castingIdentity(), characteristics);
        }

        @Override
 public BiConsumer<A, T> accumulator() {
 return accumulator;
        }

        @Override
 public Supplier<A> supplier() {
 return supplier;
        }

        @Override
 public BinaryOperator<A> combiner() {
 return combiner;
        }

        @Override
 public Function<A, R> finisher() {
 return finisher;
        }

        @Override
 public Set<Characteristics> characteristics() {
 return characteristics;
        }
    }
```

可以看到我们可以直接 `new CollectorImpl` 然后将这些函数传入，另外还有一种简单的方式就是 使用 `Collector.of()`依然可以直接传入函数。和 `new CollectorImpl` 是等价的。

#### 1.15.3. 工具函数

##### 1.15.3.1. toList()

容器: `ArrayList::new`
加入容器操作: `List::add`
多容器合并: `left.addAll(right); return left;`

```java
 public static <T>
 Collector<T, ?, List<T>> toList() {
 return new CollectorImpl<>((Supplier<List<T>>) ArrayList::new, List::add,
                                   (left, right) -> { left.addAll(right); return left; },
                                   CH_ID);
    }
```

##### 1.15.3.2.joining()

容器: `StringBuilder::new`
加入容器操作: `StringBuilder::append`
多容器合并: `r1.append(r2); return r1;`
聚合后的结果操作: `StringBuilder::toString`

```java
 public static Collector<CharSequence, ?, String> joining() {
 return new CollectorImpl<CharSequence, StringBuilder, String>(
                StringBuilder::new, StringBuilder::append,
                (r1, r2) -> { r1.append(r2); return r1; },
                StringBuilder::toString, CH_NOID);
    }
```

##### 1.15.3.3.groupingBy()

`roupingBy`是`toMap`的一种高级方式,弥补了`toMap`对值无法提供多元化的收集操作,比如对于返回`Map<T,List<E>>`这样的形式`toMap`就不是那么顺手,那么`groupingBy`的重点就是对Key和Value值的处理封装.分析如下代码,其中`classifier`是对key值的处理,`mapFactory`则是指定Map的容器具体类型,`downstream`为对Value的收集操作.

```java
 public static <T, K, D, A, M extends Map<K, D>>
 Collector<T, ?, M> groupingBy(Function<? super T, ? extends K> classifier,
 Supplier<M> mapFactory,
 Collector<? super T, A, D> downstream) {
       .......
    }
```

一个简单的例子

```java
//原生形式
   Lists.<Person>newArrayList().stream()
        .collect(() -> new HashMap<Integer,List<Person>>(),
            (h, x) -> {
 List<Person> value = h.getOrDefault(x.getType(), Lists.newArrayList());
 value.add(x);
 h.put(x.getType(), value);
            },
            HashMap::putAll
        );
//groupBy形式
Lists.<Person>newArrayList().stream()
        .collect(Collectors.groupingBy(Person::getType, HashMap::new, Collectors.toList()));
//因为对值有了操作,因此我可以更加灵活的对值进行转换
Lists.<Person>newArrayList().stream()
        .collect(Collectors.groupingBy(Person::getType, HashMap::new, Collectors.mapping(Person::getName,Collectors.toSet())));
// 还有一种比较简单的使用方式 只需要传递一个参数按照key来划分
Map<Integer, List<Person>> personsByAge = persons
            .stream()
    .collect(Collectors.groupingBy(p -> p.age));
```

##### 1.15.3.4.reducing()

`reducing`是针对单个值的收集,其返回结果不是集合家族的类型,而是单一的实体类T

容器: `boxSupplier(identity)`,这里包裹用的是一个长度为1的Object[]数组,至于原因自然是不可变类型的锅

加入容器操作: `a[0] = op.apply(a[0], t)`

多容器合并: `a[0] = op.apply(a[0], b[0]); return a;`

聚合后的结果操作: 结果自然是Object[0]所包裹的数据`a -> a[0]`

优化操作状态字段: `CH_NOID`

```
 public static <T> Collector<T, ?, T>
 reducing(T identity, BinaryOperator<T> op) {
 return new CollectorImpl<>(
 boxSupplier(identity),
                (a, t) -> { a[0] = op.apply(a[0], t); },
                (a, b) -> { a[0] = op.apply(a[0], b[0]); return a; },
                a -> a[0],
                CH_NOID);
    }
```

简单来说这个地方做的事情和 reduce 是一样的，第一个 id 传入的就是 reduce 的初始值，只是他把它包装成一个 长度为1的数组了。

```java
//原生操作
final Integer[] integers = Lists.newArrayList(1, 2, 3, 4, 5)
        .stream()
        .collect(() -> new Integer[]{0}, (a, x) -> a[0] += x, (a1, a2) -> a1[0] += a2[0]);
//reducing操作
final Integer collect = Lists.newArrayList(1, 2, 3, 4, 5)
        .stream()
        .collect(Collectors.reducing(0, Integer::sum));    
//当然Stream也提供了reduce操作
final Integer collect = Lists.newArrayList(1, 2, 3, 4, 5)
        .stream().reduce(0, Integer::sum)
```

