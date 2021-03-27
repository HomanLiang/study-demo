[toc]



# Java 内存管理

## 1. 内存简介

### 1.1. 物理内存和虚拟内存

所谓物理内存就是通常所说的 RAM（随机存储器）。

虚拟内存使得多个进程在同时运行时可以共享物理内存，这里的共享只是空间上共享，在逻辑上彼此仍然是隔离的。

### 1.2. 内核空间和用户空间

一个计算通常有固定大小的内存空间，但是程序并不能使用全部的空间。因为这些空间被划分为内核空间和用户空间，而程序只能使用用户空间的内存。

### 1.3. 使用内存的 Java 组件

Java 启动后，作为一个进程运行在操作系统中。

有哪些 Java 组件需要占用内存呢？

- 堆内存：Java 堆、类和类加载器
- 栈内存：线程
- 本地内存：NIO、JNI

## 2. 运行时数据区域（内存结构）

JVM 在执行 Java 程序的过程中会把它所管理的内存划分为若干个不同的数据区域。这些区域都有各自的用途，以及创建和销毁的时间，有的区域随着虚拟机进程的启动而存在，有些区域则依赖用户线程的启动和结束而建立和销毁。如下图所示：

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f6a766d2f6a766d2d6d656d6f72792d72756e74696d652d646174612d617265612e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210327110515.png)

### 2.1. 程序计数器

**`程序计数器（Program Counter Register）`** 是一块较小的内存空间，它可以看做是**当前线程所执行的字节码的行号指示器**。例如，分支、循环、跳转、异常、线程恢复等都依赖于计数器。

当执行的线程数量超过 CPU 数量时，线程之间会根据时间片轮询争夺 CPU 资源。如果一个线程的时间片用完了，或者是其它原因导致这个线程的 CPU 资源被提前抢夺，那么这个退出的线程就需要单独的一个程序计数器，来记录下一条运行的指令，从而在线程切换后能恢复到正确的执行位置。各条线程间的计数器互不影响，独立存储，我们称这类内存区域为 “线程私有” 的内存。

- 如果线程正在执行的是一个 Java 方法，这个计数器记录的是正在执行的虚拟机字节码指令的地址；
- 如果正在执行的是 Native 方法，这个计数器值则为空（Undefined）。

> 🔔 注意：此内存区域是唯一一个在 JVM 中没有规定任何 `OutOfMemoryError` 情况的区域。

### 2.2. Java 虚拟机栈

**`Java 虚拟机栈（Java Virtual Machine Stacks）`** 也**是线程私有的，它的生命周期与线程相同**。

每个 Java 方法在执行的同时都会创建一个栈帧（Stack Frame）用于存储 **局部变量表**、**操作数栈**、**常量池引用** 等信息。每一个方法从调用直至执行完成的过程，就对应着一个栈帧在 Java 虚拟机栈中入栈和出栈的过程。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f6a766d2f6a766d2d737461636b2e706e672177363430](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210327110530.png)

- **局部变量表** - 32 位变量槽，存放了编译期可知的各种基本数据类型、对象引用、`ReturnAddress` 类型。
- **操作数栈** - 基于栈的执行引擎，虚拟机把操作数栈作为它的工作区，大多数指令都要从这里弹出数据、执行运算，然后把结果压回操作数栈。
- **动态链接** - 每个栈帧都包含一个指向运行时常量池（方法区的一部分）中该栈帧所属方法的引用。持有这个引用是为了支持方法调用过程中的动态连接。Class 文件的常量池中有大量的符号引用，字节码中的方法调用指令就以常量池中指向方法的符号引用为参数。这些符号引用一部分会在类加载阶段或第一次使用的时候转化为直接引用，这种转化称为静态解析。另一部分将在每一次的运行期间转化为直接应用，这部分称为动态链接。
- **方法出口** - 返回方法被调用的位置，恢复上层方法的局部变量和操作数栈，如果无返回值，则把它压入调用者的操作数栈。

> 🔔 注意：
>
> 该区域可能抛出以下异常：
>
> - 如果线程请求的栈深度超过最大值，就会抛出 `StackOverflowError` 异常；
> - 如果虚拟机栈进行动态扩展时，无法申请到足够内存，就会抛出 `OutOfMemoryError` 异常。
>
> 💡 提示：
>
> 可以通过 `-Xss` 这个虚拟机参数来指定一个程序的 Java 虚拟机栈内存大小：
>
> ```
> java -Xss=512M HackTheJava
> ```

### 2.3. 本地方法栈

**`本地方法栈（Native Method Stack）`** 与虚拟机栈的作用相似。

二者的区别在于：**虚拟机栈为 Java 方法服务；本地方法栈为 Native 方法服务**。本地方法并不是用 Java 实现的，而是由 C 语言实现的。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f6a766d2f6a766d2d6e61746976652d6d6574686f642d737461636b2e6769662177363430](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210327110543.gif)

> 🔔 注意：本地方法栈也会抛出 `StackOverflowError` 异常和 `OutOfMemoryError` 异常。

### 2.4. Java 堆

**`Java 堆（Java Heap）` 的作用就是存放对象实例，几乎所有的对象实例都是在这里分配内存**。

Java 堆是垃圾收集的主要区域（因此也被叫做"GC 堆"）。现代的垃圾收集器基本都是采用**分代收集算法**，该算法的思想是针对不同的对象采取不同的垃圾回收算法。

因此虚拟机把 Java 堆分成以下三块：

- `新生代（Young Generation）`
  - `Eden` - Eden 和 Survivor 的比例为 8:1
  - `From Survivor`
  - `To Survivor`
- **`老年代（Old Generation）`**
- **`永久代（Permanent Generation）`**

当一个对象被创建时，它首先进入新生代，之后有可能被转移到老年代中。新生代存放着大量的生命很短的对象，因此新生代在三个区域中垃圾回收的频率最高。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f63732f6a6176612f6a617661636f72652f6a766d2f6a766d2d686561702e6769662177363430](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210327110556.gif)

> 🔔 注意：Java 堆不需要连续内存，并且可以动态扩展其内存，扩展失败会抛出 `OutOfMemoryError` 异常。
>
> 💡 提示：可以通过 `-Xms` 和 `-Xmx` 两个虚拟机参数来指定一个程序的 Java 堆内存大小，第一个参数设置初始值，第二个参数设置最大值。
>
> ```
> java -Xms=1M -Xmx=2M HackTheJava
> ```

### 2.5. 方法区

方法区（Method Area）也被称为永久代。**方法区用于存放已被加载的类信息、常量、静态变量、即时编译器编译后的代码等数据**。

对这块区域进行垃圾回收的主要目标是对常量池的回收和对类的卸载，但是一般比较难实现。

> 🔔 注意：和 Java 堆一样不需要连续的内存，并且可以动态扩展，动态扩展失败一样会抛出 `OutOfMemoryError` 异常。
>
> 💡 提示：
>
> - JDK 1.7 之前，HotSpot 虚拟机把它当成永久代来进行垃圾回收。可通过参数 `-XX:PermSize` 和 `-XX:MaxPermSize` 设置。
> - JDK 1.8 之后，取消了永久代，用 **`metaspace（元数据）`**区替代。可通过参数 `-XX:MaxMetaspaceSize` 设置。

### 2.6. 运行时常量池

**`运行时常量池（Runtime Constant Pool）` 是方法区的一部分**，Class 文件中除了有类的版本、字段、方法、接口等描述信息，还有一项信息是常量池（Constant Pool Table），**用于存放编译器生成的各种字面量和符号引用**，这部分内容会在类加载后被放入这个区域。

- **字面量** - 文本字符串、声明为 `final` 的常量值等。
- **符号引用** - 类和接口的完全限定名（Fully Qualified Name）、字段的名称和描述符（Descriptor）、方法的名称和描述符。

除了在编译期生成的常量，还允许动态生成，例如 `String` 类的 `intern()`。这部分常量也会被放入运行时常量池。

> 🔔 注意：当常量池无法再申请到内存时会抛出 `OutOfMemoryError` 异常。

### 2.7. 直接内存

直接内存（Direct Memory）并不是虚拟机运行时数据区的一部分，也不是 JVM 规范中定义的内存区域。

在 JDK 1.4 中新加入了 NIO 类，它可以使用 Native 函数库直接分配堆外内存，然后通过一个存储在 Java 堆里的 `DirectByteBuffer` 对象作为这块内存的引用进行操作。这样能在一些场景中显著提高性能，因为避免了在 Java 堆和 Native 堆中来回复制数据。

> 🔔 注意：直接内存这部分也被频繁的使用，且也可能导致 `OutOfMemoryError` 异常。
>
> 💡 提示：直接内存容量可通过 `-XX:MaxDirectMemorySize` 指定，如果不指定，则默认与 Java 堆最大值（`-Xmx` 指定）一样。

### 2.8. Java 内存区域对比

| 内存区域      | 内存作用范围   | 异常                                       |
| ------------- | -------------- | ------------------------------------------ |
| 程序计数器    | 线程私有       | 无                                         |
| Java 虚拟机栈 | 线程私有       | `StackOverflowError` 和 `OutOfMemoryError` |
| 本地方法栈    | 线程私有       | `StackOverflowError` 和 `OutOfMemoryError` |
| Java 堆       | 线程共享       | `OutOfMemoryError`                         |
| 方法区        | 线程共享       | `OutOfMemoryError`                         |
| 运行时常量池  | 线程共享       | `OutOfMemoryError`                         |
| 直接内存      | 非运行时数据区 | `OutOfMemoryError`                         |

## 3. JVM 运行原理

```
public class JVMCase {
	// 常量
	public final static String MAN_SEX_TYPE = "man";
	// 静态变量
	public static String WOMAN_SEX_TYPE = "woman";

	public static void main(String[] args) {

		Student stu = new Student();
		stu.setName("nick");
		stu.setSexType(MAN_SEX_TYPE);
		stu.setAge(20);

		JVMCase jvmcase = new JVMCase();

		// 调用静态方法
		print(stu);
		// 调用非静态方法
		jvmcase.sayHello(stu);
	}


	// 常规静态方法
	public static void print(Student stu) {
		System.out.println("name: " + stu.getName() + "; sex:" + stu.getSexType() + "; age:" + stu.getAge());
	}


	// 非静态方法
	public void sayHello(Student stu) {
		System.out.println(stu.getName() + "say: hello");
	}
}

class Student{
	String name;
	String sexType;
	int age;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String getSexType() {
		return sexType;
	}
	public void setSexType(String sexType) {
		this.sexType = sexType;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
}
```

运行以上代码时，JVM 处理过程如下：

（1）JVM 向操作系统申请内存，JVM 第一步就是通过配置参数或者默认配置参数向操作系统申请内存空间，根据内存大小找到具体的内存分配表，然后把内存段的起始地址和终止地址分配给 JVM，接下来 JVM 就进行内部分配。

（2）JVM 获得内存空间后，会根据配置参数分配堆、栈以及方法区的内存大小。

（3）class 文件加载、验证、准备以及解析，其中准备阶段会为类的静态变量分配内存，初始化为系统的初始值（这部分我在第 21 讲还会详细介绍）。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f32303230303633303039343235302e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210327110616.png)

（4）完成上一个步骤后，将会进行最后一个初始化阶段。在这个阶段中，JVM 首先会执行构造器 `<clinit>` 方法，编译器会在 `.java` 文件被编译成 `.class` 文件时，收集所有类的初始化代码，包括静态变量赋值语句、静态代码块、静态方法，收集在一起成为 `<clinit>()` 方法。

（5）执行方法。启动 main 线程，执行 main 方法，开始执行第一行代码。此时堆内存中会创建一个 student 对象，对象引用 student 就存放在栈中。

（6）此时再次创建一个 JVMCase 对象，调用 sayHello 非静态方法，sayHello 方法属于对象 JVMCase，此时 sayHello 方法入栈，并通过栈中的 student 引用调用堆中的 Student 对象；之后，调用静态方法 print，print 静态方法属于 JVMCase 类，是从静态方法中获取，之后放入到栈中，也是通过 student 引用调用堆中的 student 对象。

![image-20210327110736523](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210327110736.png)

## 4. OutOfMemoryError

### 4.1. 什么是 OutOfMemoryError

`OutOfMemoryError` 简称为 OOM。Java 中对 OOM 的解释是，没有空闲内存，并且垃圾收集器也无法提供更多内存。通俗的解释是：JVM 内存不足了。

在 JVM 规范中，**除了程序计数器区域外，其他运行时区域都可能发生 `OutOfMemoryError` 异常（简称 OOM）**。

下面逐一介绍 OOM 发生场景。

### 4.2. 堆空间溢出

`java.lang.OutOfMemoryError: Java heap space` 这个错误意味着：**堆空间溢出**。

更细致的说法是：Java 堆内存已经达到 `-Xmx` 设置的最大值。Java 堆用于存储对象实例，只要不断地创建对象，并且保证 GC Roots 到对象之间有可达路径来避免垃圾收集器回收这些对象，那么当堆空间到达最大容量限制后就会产生 OOM。

堆空间溢出有可能是**`内存泄漏（Memory Leak）`** 或 **`内存溢出（Memory Overflow）`** 。需要使用 jstack 和 jmap 生成 threaddump 和 heapdump，然后用内存分析工具（如：MAT）进行分析。

#### 4.2.1. Java heap space 分析步骤

1. 使用 `jmap` 或 `-XX:+HeapDumpOnOutOfMemoryError` 获取堆快照。
2. 使用内存分析工具（visualvm、mat、jProfile 等）对堆快照文件进行分析。
3. 根据分析图，重点是确认内存中的对象是否是必要的，分清究竟是是内存泄漏（Memory Leak）还是内存溢出（Memory Overflow）。

#### 4.2.2. 内存泄漏

**内存泄漏是指由于疏忽或错误造成程序未能释放已经不再使用的内存的情况**。

内存泄漏并非指内存在物理上的消失，而是应用程序分配某段内存后，由于设计错误，失去了对该段内存的控制，因而造成了内存的浪费。内存泄漏随着被执行的次数不断增加，最终会导致内存溢出。

在Java中，**内存泄漏**就是存在一些被分配的对象，这些对象有下面两个特点，**首先**，这些对象是可达的，即**在有向图中，存在通路可以与其相连**；**其次**，**这些对象是无用的，即程序以后不会再使用这些对象**。如果对象满足这两个条件，这些对象就可以判定为Java中的内存泄漏，这些对象不会被GC所回收，然而它却占用内存。

**4.2.2.1. 内存泄漏常见场景：**

- 静态容器
  - 声明为静态（`static`）的 `HashMap`、`Vector` 等集合
  - 通俗来讲 A 中有 B，当前只把 B 设置为空，A 没有设置为空，回收时 B 无法回收。因为被 A 引用。
  
- 监听器
  
  - 监听器被注册后释放对象时没有删除监听器
  
- 物理连接
  
  - 各种连接池建立了连接，必须通过 `close()` 关闭链接
  
- 内部类和外部模块等的引用

  内部类的引用是比较容易遗忘的一种，而且一旦没释放可能导致一系列的后继类对象没有释放。此外程序员还要小心外部模块不经意的引用，例如程序员A 负责A 模块，调用了B 模块的一个方法如：

  ```
  public void registerMsg(Object b);
  ```

  这种调用就要非常小心了，传入了一个对象，很可能模块B就保持了对该对象的引用，这时候就需要注意模块B是否提供相应的操作去除引用。

- 单例模式

  不正确使用单例模式是引起内存泄漏的一个常见问题，单例对象在初始化后将在 **JVM** 的整个生命周期中存在（**以静态变量的方式**），如果单例对象持有外部的引用，那么这个对象将不能被 JVM 正常回收，导致内存泄漏，考虑下面的例子：

  ```
  public class A {
      public A() {
          B.getInstance().setA(this);
      }
      ...
  }
  
  //B类采用单例模式
  class B{
      private A a;
      private static B instance = new B();
      
      public B(){}
      
      public static B getInstance() {
          return instance;
      }
      
      public void setA(A a) {
          this.a = a;
      }
  
      public A getA() {
          return a;
      }
  }
  ```

**4.2.2.2.重点关注：**

- `FGC` — 从应用程序启动到采样时发生 Full GC 的次数。
- `FGCT` — 从应用程序启动到采样时 Full GC 所用的时间（单位秒）。
- `FGC` 次数越多，`FGCT` 所需时间越多，越有可能存在内存泄漏。

如果是内存泄漏，可以进一步查看泄漏对象到 GC Roots 的对象引用链。这样就能找到泄漏对象是怎样与 GC Roots 关联并导致 GC 无法回收它们的。掌握了这些原因，就可以较准确的定位出引起内存泄漏的代码。

导致内存泄漏的常见原因是使用容器，且不断向容器中添加元素，但没有清理，导致容器内存不断膨胀。

**4.2.2.3.内存泄漏的典型例子**

```
Vector v = new Vector(10);

for (int i = 0; i < 100; i++) {
    Object o = new Object();
    v.add(o);
    o = null;
}
```

在这个例子中，我们循环申请Object对象，并将所申请的对象放入一个 Vector 中，如果我们仅仅释放引用本身，那么 Vector 仍然引用该对象，所以这个对象对 GC 来说是不可回收的。因此，如果对象加入到Vector 后，还必须从 Vector 中删除，最简单的方法就是将 Vector 对象设置为 null。

**4.2.2.4.如何防止内存泄漏的发生？**

- **好的编码习惯**

  最基本的建议就是尽早释放无用对象的引用，大多数程序员在使用临时变量的时候，都是让引用变量在退出活动域后，自动设置为 **null** 。在使用这种方式时候，必须特别注意一些复杂的对象图，例如数组、列、树、图等，这些对象之间有相互引用关系较为复杂。对于这类对象，**GC** 回收它们一般效率较低。如果程序允许，尽早将不用的引用对象赋为null。另外建议几点：

  在确认一个对象无用后，将其所有引用显式的置为null；

  当类从 **Jpanel** 或 **Jdialog** 或其它容器类继承的时候，删除该对象之前不妨调用它的 **removeall()** 方法；在设一个引用变量为 **null** 值之前，应注意该引用变量指向的对象是否被监听，若有，要首先除去监听器，然后才可以赋空值；当对象是一个 **Thread** 的时候，删除该对象之前不妨调用它的
   **interrupt()** 方法；内存检测过程中不仅要关注自己编写的类对象，同时也要关注一些基本类型的对象，例如：**int[]、String、char[]** 等等；如果有数据库连接，使用 **try…finally** 结构，在 **finally** 中关闭 **Statement** 对象和连接。

- **好的测试工具**

  在开发中不能完全避免内存泄漏，关键要在发现有内存泄漏的时候能用好的测试工具迅速定位问题的所在。市场上已有几种专业检查 **Java** 内存泄漏的工具，它们的基本工作原理大同小异，都是通过监测 **Java** 程序运行时，所有对象的申请、释放等动作，将内存管理的所有信息进行统计、分析、可视化。开发人员将根据这些信息判断程序是否有内存泄漏问题。这些工具包括 **Optimizeit Profiler、JProbe Profiler、JinSight、Rational** 公司的 **Purify** 等。

- 注意像 **HashMap** 、**ArrayList** 的集合对象

  特别注意一些像 **HashMap** 、**ArrayList** 的集合对象，它们经常会引发内存泄漏。当它们被声明为 **static** 时，它们的生命周期就会和应用程序一样长。

- 注意 事件监听 和 回调函数

  特别注意 **事件监听** 和 **回调函数** 。当一个监听器在使用的时候被注册，但不再使用之后却未被反注册。

  **“如果一个类自己管理内存，那开发人员就得小心内存泄漏问题了。”** 通常一些成员变量引用其他对象，初始化的时候需要置空。

#### 4.2.3. 内存溢出

如果不存在内存泄漏，即内存中的对象确实都必须存活着，则应当检查虚拟机的堆参数（`-Xmx` 和 `-Xms`），与机器物理内存进行对比，看看是否可以调大。并从代码上检查是否存在某些对象生命周期过长、持有时间过长的情况，尝试减少程序运行期的内存消耗。

【示例】

```
/**
 * 堆溢出示例
 * <p>
 * 错误现象：java.lang.OutOfMemoryError: Java heap space
 * <p>
 * VM Args：-verbose:gc -Xms10M -Xmx10M
 *
 * @author <a href="mailto:forbreak@163.com">Zhang Peng</a>
 * @since 2019-06-25
 */
public class HeapOutOfMemoryDemo {

    public static void main(String[] args) {
        Double[] array = new Double[999999999];
        System.out.println("array length = [" + array.length + "]");
    }

}
```

执行 `java -verbose:gc -Xms10M -Xmx10M -XX:+HeapDumpOnOutOfMemoryError io.github.dunwu.javacore.jvm.memory.HeapMemoryLeakMemoryErrorDemo`

上面的例子是一个极端的例子，试图创建一个维度很大的数组，堆内存无法分配这么大的内存，从而报错：`Java heap space`。

但如果在现实中，代码并没有问题，仅仅是因为堆内存不足，可以通过 `-Xms` 和 `-Xmx` 适当调整堆内存大小。

**常见原因**

- 设置的jvm内存太小，对象所需内存太大，创建对象时分配空间，就会抛出这个异常。
- 流量/数据峰值，应用程序自身的处理存在一定的限额，比如一定数量的用户或一定数量的数据。而当用户数量或数据量突然激增并超过预期的阈值时，那么就会峰值停止前正常运行的操作将停止并触发java . lang.OutOfMemoryError:Java堆空间错误
- 存在内存泄漏的情况，不停的堆积最终会触发java . lang.OutOfMemoryError。

**解决方法**

- 如果代码没有什么问题的情况下，可以适当调整-Xms和-Xmx两个jvm参数，使用压力测试来调整这两个参数达到最优值。
- 尽量避免大的对象的申请，像文件上传，大批量从数据库中获取，这是需要避免的，尽量分块或者分批处理，有助于系统的正常稳定的执行。
- 尽量提高一次请求的执行速度，垃圾回收越早越好，否则，大量的并发来了的时候，再来新的请求就无法分配内存了，就容易造成系统的雪崩。
- 排除解决内存泄漏的问题

### 4.3. GC 开销超过限制

`java.lang.OutOfMemoryError: GC overhead limit exceeded` 这个错误，官方给出的定义是：**超过 `98%` 的时间用来做 GC 并且回收了不到 `2%` 的堆内存时会抛出此异常**。这意味着，发生在 GC 占用大量时间为释放很小空间的时候发生的，是一种保护机制。导致异常的原因：一般是因为堆太小，没有足够的内存。

【示例】

```
/**
 * GC overhead limit exceeded 示例
 * 错误现象：java.lang.OutOfMemoryError: GC overhead limit exceeded
 * 发生在GC占用大量时间为释放很小空间的时候发生的，是一种保护机制。导致异常的原因：一般是因为堆太小，没有足够的内存。
 * 官方对此的定义：超过98%的时间用来做GC并且回收了不到2%的堆内存时会抛出此异常。
 * VM Args: -Xms10M -Xmx10M
 */
public class GcOverheadLimitExceededDemo {

    public static void main(String[] args) {
        List<Double> list = new ArrayList<>();
        double d = 0.0;
        while (true) {
            list.add(d++);
        }
    }

}
```

【处理】

- 与 **Java heap space** 错误处理方法类似，先判断是否存在内存泄漏。如果有，则修正代码；如果没有，则通过 `-Xms` 和 `-Xmx` 适当调整堆内存大小。
- 要减少对象生命周期，尽量能快速的进行垃圾回收。

### 4.4. 永久代空间不足

【错误】

```
java.lang.OutOfMemoryError: PermGen space
```

【原因】

Perm （永久代）空间主要用于存放 `Class` 和 `Meta` 信息，包括类的名称和字段，带有方法字节码的方法，常量池信息，与类关联的对象数组和类型数组以及即时编译器优化。GC 在主程序运行期间不会对永久代空间进行清理，默认是 64M 大小。

根据上面的定义，可以得出 **PermGen 大小要求取决于加载的类的数量以及此类声明的大小**。因此，可以说造成该错误的主要原因是永久代中装入了太多的类或太大的类。

在 JDK8 之前的版本中，可以通过 `-XX:PermSize` 和 `-XX:MaxPermSize` 设置永久代空间大小，从而限制方法区大小，并间接限制其中常量池的容量。

#### 4.4.1. 初始化时永久代空间不足

【示例】

```
/**
 * 永久代内存空间不足示例
 * <p>
 * 错误现象：
 * <ul>
 * <li>java.lang.OutOfMemoryError: PermGen space (JDK8 以前版本)</li>
 * <li>java.lang.OutOfMemoryError: Metaspace (JDK8 及以后版本)</li>
 * </ul>
 * VM Args:
 * <ul>
 * <li>-Xmx100M -XX:MaxPermSize=16M (JDK8 以前版本)</li>
 * <li>-Xmx100M -XX:MaxMetaspaceSize=16M (JDK8 及以后版本)</li>
 * </ul>
 */
public class PermOutOfMemoryErrorDemo {

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 100_000_000; i++) {
            generate("eu.plumbr.demo.Generated" + i);
        }
    }

    public static Class generate(String name) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        return pool.makeClass(name).toClass();
    }

}
```

在此示例中，源代码遍历循环并在运行时生成类。javassist 库正在处理类生成的复杂性。

#### 4.4.2.重部署时永久代空间不足

对于更复杂，更实际的示例，让我们逐步介绍一下在应用程序重新部署期间发生的 Permgen 空间错误。重新部署应用程序时，你希望垃圾回收会摆脱引用所有先前加载的类的加载器，并被加载新类的类加载器取代。

不幸的是，许多第三方库以及对线程，JDBC 驱动程序或文件系统句柄等资源的不良处理使得无法卸载以前使用的类加载器。反过来，这意味着在每次重新部署期间，所有先前版本的类仍将驻留在 PermGen 中，从而在每次重新部署期间生成数十兆的垃圾。

让我们想象一个使用 JDBC 驱动程序连接到关系数据库的示例应用程序。启动应用程序时，初始化代码将加载 JDBC 驱动程序以连接到数据库。对应于规范，JDBC 驱动程序向 java.sql.DriverManager 进行注册。该注册包括将对驱动程序实例的引用存储在 DriverManager 的静态字段中。

现在，当从应用程序服务器取消部署应用程序时，java.sql.DriverManager 仍将保留该引用。我们最终获得了对驱动程序类的实时引用，而驱动程序类又保留了用于加载应用程序的 java.lang.Classloader 实例的引用。反过来，这意味着垃圾回收算法无法回收空间。

而且该 java.lang.ClassLoader 实例仍引用应用程序的所有类，通常在 PermGen 中占据数十兆字节。这意味着只需少量重新部署即可填充通常大小的 PermGen。

#### 4.4.3.PermGen space 解决方案

（1）解决初始化时的 `OutOfMemoryError`

在应用程序启动期间触发由于 PermGen 耗尽导致的 `OutOfMemoryError` 时，解决方案很简单。该应用程序仅需要更多空间才能将所有类加载到 PermGen 区域，因此我们只需要增加其大小即可。为此，更改你的应用程序启动配置并添加（或增加，如果存在）`-XX:MaxPermSize` 参数，类似于以下示例：

```
java -XX:MaxPermSize=512m com.yourcompany.YourClass
```

上面的配置将告诉 JVM，PermGen 可以增长到 512MB。

清理应用程序中 `WEB-INF/lib` 下的 jar，用不上的 jar 删除掉，多个应用公共的 jar 移动到 Tomcat 的 lib 目录，减少重复加载。

🔔 注意：`-XX:PermSize` 一般设为 64M

（2）解决重新部署时的 `OutOfMemoryError`

重新部署应用程序后立即发生 OutOfMemoryError 时，应用程序会遭受类加载器泄漏的困扰。在这种情况下，解决问题的最简单，继续进行堆转储分析–使用类似于以下命令的重新部署后进行堆转储：

```
jmap -dump:format=b,file=dump.hprof <process-id>
```

然后使用你最喜欢的堆转储分析器打开转储（Eclipse MAT 是一个很好的工具）。在分析器中可以查找重复的类，尤其是那些正在加载应用程序类的类。从那里，你需要进行所有类加载器的查找，以找到当前活动的类加载器。

对于非活动类加载器，你需要通过从非活动类加载器收集到 GC 根的最短路径来确定阻止它们被垃圾收集的引用。有了此信息，你将找到根本原因。如果根本原因是在第三方库中，则可以进入 Google/StackOverflow 查看是否是已知问题以获取补丁/解决方法。

（3）解决运行时 `OutOfMemoryError`

第一步是检查是否允许 GC 从 PermGen 卸载类。在这方面，标准的 JVM 相当保守-类是天生的。因此，一旦加载，即使没有代码在使用它们，类也会保留在内存中。当应用程序动态创建许多类并且长时间不需要生成的类时，这可能会成为问题。在这种情况下，允许 JVM 卸载类定义可能会有所帮助。这可以通过在启动脚本中仅添加一个配置参数来实现：

```
-XX:+CMSClassUnloadingEnabled
```

默认情况下，此选项设置为 false，因此要启用此功能，你需要在 Java 选项中显式设置。如果启用 CMSClassUnloadingEnabled，GC 也会扫描 PermGen 并删除不再使用的类。请记住，只有同时使用 UseConcMarkSweepGC 时此选项才起作用。

```
-XX:+UseConcMarkSweepGC
```

在确保可以卸载类并且问题仍然存在之后，你应该继续进行堆转储分析–使用类似于以下命令的方法进行堆转储：

```
jmap -dump:file=dump.hprof,format=b <process-id>
```

然后，使用你最喜欢的堆转储分析器（例如 Eclipse MAT）打开转储，然后根据已加载的类数查找最昂贵的类加载器。从此类加载器中，你可以继续提取已加载的类，并按实例对此类进行排序，以使可疑对象排在首位。

然后，对于每个可疑者，就需要你手动将根本原因追溯到生成此类的应用程序代码。

### 4.5. 元数据区空间不足

【错误】

```
Exception in thread "main" java.lang.OutOfMemoryError: Metaspace
```

【原因】

Java8 以后，JVM 内存空间发生了很大的变化。取消了永久代，转而变为元数据区。

**元数据区的内存不足，即方法区和运行时常量池的空间不足**。

方法区用于存放 Class 的相关信息，如类名、访问修饰符、常量池、字段描述、方法描述等。

一个类要被垃圾收集器回收，判定条件是比较苛刻的。在经常动态生成大量 Class 的应用中，需要特别注意类的回收状况。这类常见除了 CGLib 字节码增强和动态语言以外，常见的还有：大量 JSP 或动态产生 JSP 文件的应用（JSP 第一次运行时需要编译为 Java 类）、基于 OSGi 的应用（即使是同一个类文件，被不同的加载器加载也会视为不同的类）等。

【示例】方法区出现 `OutOfMemoryError`

```
public class MethodAreaOutOfMemoryDemo {

    public static void main(String[] args) {
        while (true) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(Bean.class);
            enhancer.setUseCache(false);
            enhancer.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                    return proxy.invokeSuper(obj, args);
                }
            });
            enhancer.create();
        }
    }

    static class Bean {}

}
```

【解决】

当由于元空间而面临 `OutOfMemoryError` 时，第一个解决方案应该是显而易见的。如果应用程序耗尽了内存中的 Metaspace 区域，则应增加 Metaspace 的大小。更改应用程序启动配置并增加以下内容：

```
-XX:MaxMetaspaceSize=512m
```

上面的配置示例告诉 JVM，允许 Metaspace 增长到 512 MB。

另一种解决方案甚至更简单。你可以通过删除此参数来完全解除对 Metaspace 大小的限制，JVM 默认对 Metaspace 的大小没有限制。但是请注意以下事实：这样做可能会导致大量交换或达到本机物理内存而分配失败。

### 4.6. 无法新建本地线程

`java.lang.OutOfMemoryError: Unable to create new native thread` 这个错误意味着：**Java 应用程序已达到其可以启动线程数的限制**。

【原因】

当发起一个线程的创建时，虚拟机会在 JVM 内存创建一个 `Thread` 对象同时创建一个操作系统线程，而这个系统线程的内存用的不是 JVM 内存，而是系统中剩下的内存。

那么，究竟能创建多少线程呢？这里有一个公式：

```
线程数 = (MaxProcessMemory - JVMMemory - ReservedOsMemory) / (ThreadStackSize)
```

【参数】

- `MaxProcessMemory` - 一个进程的最大内存
- `JVMMemory` - JVM 内存
- `ReservedOsMemory` - 保留的操作系统内存
- `ThreadStackSize` - 线程栈的大小

**给 JVM 分配的内存越多，那么能用来创建系统线程的内存就会越少，越容易发生 `unable to create new native thread`**。所以，JVM 内存不是分配的越大越好。

但是，通常导致 `java.lang.OutOfMemoryError` 的情况：无法创建新的本机线程需要经历以下阶段：

1. JVM 内部运行的应用程序请求新的 Java 线程
2. JVM 本机代码代理为操作系统创建新本机线程的请求
3. 操作系统尝试创建一个新的本机线程，该线程需要将内存分配给该线程
4. 操作系统将拒绝本机内存分配，原因是 32 位 Java 进程大小已耗尽其内存地址空间（例如，已达到（2-4）GB 进程大小限制）或操作系统的虚拟内存已完全耗尽
5. 引发 `java.lang.OutOfMemoryError: Unable to create new native thread` 错误。

【示例】

```
public class UnableCreateNativeThreadErrorDemo {

    public static void main(String[] args) {
        while (true) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeUnit.MINUTES.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
```

【处理】

可以通过增加操作系统级别的限制来绕过无法创建新的本机线程问题。例如，如果限制了 JVM 可在用户空间中产生的进程数，则应检查出并可能增加该限制：

```
[root@dev ~]# ulimit -a
core file size          (blocks, -c) 0
--- cut for brevity ---
max user processes              (-u) 1800
```

通常，`OutOfMemoryError` 对新的本机线程的限制表示编程错误。当应用程序产生数千个线程时，很可能出了一些问题—很少有应用程序可以从如此大量的线程中受益。

解决问题的一种方法是开始进行线程转储以了解情况。

### 4.7. 直接内存溢出

由直接内存导致的内存溢出，一个明显的特征是在 Head Dump 文件中不会看见明显的异常，如果发现 OOM 之后 Dump 文件很小，而程序中又直接或间接使用了 NIO，就可以考虑检查一下是不是这方面的原因。

【示例】直接内存 `OutOfMemoryError`

```
/**
 * 本机直接内存溢出示例
 * 错误现象：java.lang.OutOfMemoryError
 * VM Args：-Xmx20M -XX:MaxDirectMemorySize=10M
 */
public class DirectOutOfMemoryDemo {

    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) throws IllegalAccessException {
        Field unsafeField = Unsafe.class.getDeclaredFields()[0];
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        while (true) {
            unsafe.allocateMemory(_1MB);
        }
    }

}
```

**解决办法**

- 如果经常有类似的操作，可以考虑设置参数：-XX:MaxDirectMemorySize，并及时clear内存。

### 总结

通过以上的出现内存溢出情况，大家在实际碰到问题时也就会知道怎么解决了，在实际编码中也要记得：

1. 第三方jar包要慎重引入，坚决去掉没有用的jar包，提高编译的速度和系统的占用内存。

2. 对于大的对象或者大量的内存申请，要进行优化，大的对象要分片处理，提高处理性能，减少对象生命周期。

3. 尽量固定线程的数量，保证线程占用内存可控，同时需要大量线程时，要优化好操作系统的最大可打开的连接数。

4. 对于递归调用，也要控制好递归的层级，不要太高，超过栈的深度。

5. 分配给栈的内存并不是越大越好，因为栈内存越大，线程多，留给堆的空间就不多了，容易抛出OOM。JVM的默认参数一般情况没有问题（包括递归）。

## 5. StackOverflowError

对于 HotSpot 虚拟机来说，栈容量只由 `-Xss` 参数来决定如果线程请求的栈深度大于虚拟机所允许的最大深度，将抛出 `StackOverflowError` 异常。

从实战来说，栈溢出的常见原因：

- **递归函数调用层数太深**
- **大量循环或死循环**

【示例】递归函数调用层数太深导致 `StackOverflowError`

```
public class StackOverflowDemo {

    private int stackLength = 1;

    public void recursion() {
        stackLength++;
        recursion();
    }

    public static void main(String[] args) {
        StackOverflowDemo obj = new StackOverflowDemo();
        try {
            obj.recursion();
        } catch (Throwable e) {
            System.out.println("栈深度：" + obj.stackLength);
            e.printStackTrace();
        }
    }
}
```

## 6.内存设置参数

在通过一张图来了解如何通过参数来控制各区域的内存大小

![331425-20160623115841781-223449019](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327164710.png)

参数说明

- `-Xms` 设置堆的最小空间大小。
- `-Xmx` 设置堆的最大空间大小。
- `-XX:NewSize` 设置新生代最小空间大小。
- `-XX:MaxNewSize` 设置新生代最大空间大小。
- `-XX:PermSize` 设置永久代最小空间大小。
- `-XX:MaxPermSize` 设置永久代最大空间大小。
- `-Xss` 设置每个线程的堆栈大小。

没有直接设置老年代的参数，但是可以设置堆空间大小和新生代空间大小两个参数来间接控制。

**老年代空间大小=堆空间大小-年轻代大空间大小**

## 7.对象的内存布局和底层机制

### 7.1.对象的内存布局

对象在JVM中是由一个Oop进行描述的。回顾一下，Oop由**对象头(_mark、_metadata)\**以及\**实例数据区**组成，而对象头中存在一个_metadata，其内部存在一个指针，指向类的元数据信息，就是下面这张图:

![java6-1606098465](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327190549.png)

而今天要说的对象的内存布局，其底层实际上就是来自于这张图。

了解过对象组成的同学应该明白，对象由三部分构成，分别是：**对象头**、**实例数据**、**对齐填充**组成，而对象头和示例数据，对应的就是Oop对象中的两大部分，而对齐填充实际上是一个只在逻辑中存在的部分。

#### 7.1.1.对象头

我们可以对这三个部分分别进行更深入的了解，首先是**对象头**：

对象头分为**MarkWord**和**类型指针**，MarkWord就是Oop对象中的_mark，其内部用于存储**对象自身运行时的数据**，例如：**HashCode、GC分代年龄、锁状态标志、持有锁的线程、偏向线程Id、偏向时间戳**等。

这是笔者在网上找的关于**对象头的内存布局**(64位操作系统，无指针压缩)：

![java3-1606098465](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327190646.png)

对象头占用128位，也就是16字节，其中**MarkWord**占8字节，**Klass Point(类型指针)\**占8字节，MarkWord中所存储的信息，是这个对象最基本的一些信息，例如\**GC分代年龄**，可以让JVM判断当前对象是否应该进入老年代，**锁状态标志**，在处理并发的过程中，可以判断当前要以什么级别的手段来保证线程安全，从而优化同步操作的性能，其他的相信大家都比较了解，这里就暂时先不一一列举了。当然，对象头在之后的并发专题依旧会有所提及。

而对象头的另外8字节，是KlassPoint，类型指针，在上一篇文章的Oop模型中，提到类型指针指向Klass对象，用于在运行时获取对象所属的类的元信息。

#### 7.1.2.实例数据

何为实例数据，顾名思义，就是对象中的字段，用更严谨一点的话来说，**类的非静态属性，在生成对象后，就是实例数据**，而实例数据这部分的大小，就是实实在在的多个属性所占的空间的和，例如有下面这样一个类：

```
public class Test{
    private int a;
    private double b;
    private boolean c;
}
```

那么在`new Test()`操作之后，这个对象的实例数据区所占的空间就是4+8+1 = 13字节，以此类推。

而在Java中，基本数据类型都有其大小：

> boolean  --- 1B
>
> byte  --- 1B
>
> short  --- 2B
>
> char  ---  2B
>
> int --- 4B
>
> float --- 4B
>
> double  --- 8B
>
> long ---  8B

除了上述的八个基本数据类型以外，类中还可以包含**引用类型**对象，那么这部分如何计算呢？

这里需要分情况讨论，由于还没有说到指针压缩，那么大家就先记下好了：

> 如果是32位机器，那么引用类型占**4字节**。
>
> 如果是64位机器，那么引用类型占**8字节**。
>
> 如果是64位机器，且开启了指针压缩，那么引用类型占**4字节**。

如果对象的实例数据区，存在别的引用类型对象，实际上只是保存了这个对象的地址，理解了这个概念，就可以对这三种情况进行理解性记忆了。

**为什么32位机器的引用类型占4个字节，而64位机器引用类型占8字节**？

这里就要提到一个寻址的概念，既然保存了内存地址，那就是为了日后方便寻址，而32位机器的含义就是，其地址是由32个Bit位组成的，所以要记录其内存地址，需要使用4字节，64位同理，需要8字节。

#### 7.1.3.对齐填充

我们提到对象是由三部分构成，但是上文只涉及了两部分，还有一部分就是**对齐填充**，这个是比较特殊的一个部分，只存在于逻辑中，这里需要科普一下，JVM中的对象都有一个特性，那就是**8字节对齐**，什么叫8字节对齐呢，就是一个对象的大小，只能是8的整数倍，如果一个对象不满8的整数倍，则会对其进行填充。

看到这里可能有同学就会心存疑惑，那假设一个对象的内容只占20字节，那么根据8字节对齐特性，这个对象不就会变成24字节吗？那岂不是浪费空间了？根据8字节对其的逻辑，这个问题的答案是肯定的，假设一个对象只有20字节，那么就会**填充**变成24字节，而多出的这四个字节，就是我们所说的**对齐填充**，笔者在这里画一张图来描述一下：

![java5-1606098465](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327190754.png)

对象头在不考虑指针压缩的情况下，占用16个字节，实例数据区，我们假设是一个int类型的数据，占用4个字节，那么这里一共是20字节，那么由于8字节对齐特性，对象就会填充到24字节。

那么**为什么要这么去设计呢？**，刚开始笔者也有这样的疑惑，这样设计会有很多白白浪费掉的空间，毕竟填充进来的数据，在逻辑上是没有任何意义的，但是如果站在一个设计者的角度上看，这样的设计在日后的维护中是最为方便的。假设对象没有8字节对齐，而是随机大小分布在内存中，由于这种不规律，会造成设计者的代码逻辑变得异常复杂，因为设计者根本不知道你这个对象到底有多大，从而没有办法完整地取出一整个对象，还有可能在这种不确定中，取到其它对象的数据，造成系统混乱。

当然，有些同学觉得设计上的问题总能克服，这点原因还不足以让我们浪费内存，这就是我理解的第二点原因，这么设计还会有一种好处，就是**提升性能**，假设对象是不等长的，那么为了获取一个完整的对象，就必须一个字节一个字节地去读，直到读到结束符，但是如果8字节对齐后，获取对象就可以以8个字节为单位进行读取，快速获取到一个对象，也不失为一种以空间换时间的设计方案。

那么又有同学要问了，那既然8字节可以提升性能，那为什么不**16字节对齐**呢，这样岂不是性能更高吗？答案是：没有必要，有两个原因，第一，我们对象头最大是16字节，而实例数据区最大的数据类型是8个字节，所以如果选择16字节对齐，假设有一个18字节的对象，那么我们需要将其填充成为一个32字节的对象，而选择8字节填充则只需要填充到24字节即可，这样不会造成更大的空间浪费。第二个原因，允许我在这里卖一下关子，在之后的指针压缩中，我们再详细进行说明。

#### 7.1.4.关于对象内存布局的证明方式

证明方式有两种，一种是使用代码的方式，还有一种就是使用上一篇文章中我们提到的，使用HSDB，可以直接了当地查看对象的组成，由于HSDB在上一篇文章中已经说过了，所以这里只说第一种方式。

首先，我们需要引入一个maven依赖：

```
<!-- https://mvnrepository.com/artifact/org.openjdk.jol/jol-core -->
<dependency>
    <groupId>org.openjdk.jol</groupId>
    <artifactId>jol-core</artifactId>
    <version>0.10</version>
</dependency>
```

引入这个依赖之后，我们就可以在控制台中查看对象的内存布局了，代码如下：

```
public class Blog {
    public static void main(String[] args) {
        Blog blog = new Blog();
        System.out.println(ClassLayout.parseInstance(blog).toPrintable());
    }
}
```

首先是关闭指针压缩的情况，对齐填充为0字节，对象大小为16字节：

![java6-1606098465-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327190908.png)

然后是开启指针压缩的情况，对齐填充为4字节，对象大小依旧为16字节：

![java0-1606098465](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327190924.png)

解释一下为什么两种情况都是16字节：

> 开启指针压缩，对象大小（16字节） = MarkWord（8字节）+ KlassPointer（4字节）+ 数组长度（0字节） + 实例数据（0字节）+ 对齐填充（4字节） 关闭指针压缩，对象大小（16字节）= MarkWord（8字节）+ KlassPointer（8字节）+ 数组长度（0字节）+ 实例数据（0字节） + 对齐填充（0字节）

### 7.2.如何计算对象的内存占用

在第一节中我们已经详细阐述了对象在内存中的布局，主要分为三部分，**对象头**、**实例数据**、**对齐填充**，并且进行了证明。这一节中来带大家计算对象的内存占用。

实际上在刚才对内存布局的阐述中，应该有很多同学都对如何计算对象内存占用有了初步的了解，其实这也并不难，无非就是把三个区域的占用求和，但是上文中我们只是说了几种简单的情况，所以这里主要来说说我们上文中没有考虑到的，我们将分情况进行讨论并证明。

#### 7.2.1.对象中只存在基本数据类型

```
public class Blog {
    private int a = 10;
    private long b = 20;
    private double c = 0.0;
    private float d = 0.0f;

    public static void main(String[] args) {
        Blog blog = new Blog();
        System.out.println(ClassLayout.parseInstance(blog).toPrintable());
    }
}
```

这种情况是除了空对象以外的最简单的一种情况，假设对象中存在的属性全都是Java八种基本类型中的某一种或某几种类型，对象的大小如何计算？

不妨先来看看结果：

![java2-1606098466](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191237.png)

对于这种情况，我们只需要简单地将**对象头+示例数据+对齐填充**即可，由于我们在对象中存在四个属性，分别为int(4字节)+long(8字节)+double(8字节)+float(4字节)，可以得出实例数据为24字节，而对象头为12字节（指针压缩开启），那么一共就是36字节，但是由于Java中的对象必须得是**8字节对齐**，所以对齐填充会为其补上4字节，所以整个对象就是：

> 对象头(12字节)+实例数据(24字节)+对齐填充(4字节) = 40字节

#### 7.2.2.对象中存在引用类型（关闭指针压缩）

那么对象中存在引用类型，该如何计算？这里涉及到**开启指针压缩**和**关闭指针压缩**两种情况，我们先来看看关闭指针压缩的情况，究竟有何不同。

```
public class Blog {
    Map<String,Object> objMap = new HashMap<>(16);

    public static void main(String[] args) {
        Blog blog = new Blog();
        System.out.println(ClassLayout.parseInstance(blog).toPrintable());
    }
}
```

同样，先看结果：

![java0-1606098466](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191332.png)

可以看到，对象的实例数据区存在一个引用类型属性，就像第一节中说的，只是保存了指向这个属性的指针，这个指针在关闭指针压缩的情况下，占用8字节，不妨也计算一下它的大小：

> 对象头(关闭指针压缩，占用16字节)+实例数据（1个对象指针8字节）+ 对齐填充(无需进行填充)=24字节

#### 7.2.3.对象中存在引用类型（开启指针压缩）

那么如果是开启指针压缩的情况呢？

![java0-1606098466-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191416.png)

如果是开启指针压缩的情况，**类型指针**和**实例数据区的指针**都仅占用**4字节**，所以其内存大小为：

> MarkWord(8B)+KlassPointer(4B)+实例数据区(4B)+对齐填充(0B) = 16B

#### 7.2.4.数组类型（关闭指针压缩）

如果是数组类型的对象呢？由于在上文中已经形成的定向思维，大家可能已经开始使用原先的套路开始计算数组对象的大小了，但是这里的情况就相对比普通对象要复杂很多，出现的一些现象可能要让大家大跌眼镜了。

我们这里枚举三种情况：

```
public class Blog {

    private int a = 10;
    private int b = 10;


    public static void main(String[] args) {
        //对象中无属性的数组
        Object[] objArray = new Object[3];
        //对象中存在两个int型属性的数组
        Blog[] blogArray = new Blog[3];
        //基本类型数组
        int[] intArray = new int[1];
        System.out.println(ClassLayout.parseInstance(blogArray).toPrintable());
        System.out.println(ClassLayout.parseInstance(objArray).toPrintable());
        System.out.println(ClassLayout.parseInstance(intArray).toPrintable());
    }
}
```

依旧是先看结果：

**首先是第一种情况：对象中无属性的数组**：

![java9-1606098466](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191507.png)

同样的一个打印对象操作，除了MarkWord、KlassPointer、实例数据对齐填充以外，多了一篇空间，我们可以发现，原先在普通对象的算法，已经不适用于数组的算法了，因为在数组中出现了一个很诡异而我们从没有提到过的东西，那就是**对象头的第三部分**——**数组长度**。

**数组长度究竟为何物？**

如果对象是一个数组，它的内部除了我们刚才说的那些以外，还会存在一个数组长度属性，用于记录这个数组的大小，数组长度为32个Bit，也就是4个字节，这里也可以关联上一个基础知识，就是**Java中数组最大可以设置为多大？\**跟计算内存地址的表示方式类似，由于其占4个字节，所以数组的长度最大为\**2^32**。

我们再来看看实例数据区的情况，由于其存放了三个对象，而我们在**对象中存在引用类型**这个情况中阐述过，即使存在对象，我们也只是保存了指向其内存地址的指针，这里由于关闭了指针压缩，所以每个指针占用8个字节，一共24字节。

再回到图上，在前几个案例中，对齐填充都在实例数据区之后，但是这里对齐填充是处于**对象头的第四部分**。在实例数据区之前，也就是在数组对象中，出现了第二段的对齐填充，那么数组对象的内存布局就应该变成下图这样：

![java4-1606098466](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191528.png)

我们可以在另外两种情况中验证这个想法：

**对象中存在两个int型属性的数组**：

![java10-1606098466](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191547.png)

**基本数据类型数组**：

![java5-1606098466](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191605.png)

我们可以看到，即使对象中存在两个int类型的数组，依旧保存其内存地址指针，所以依旧是4字节，而在基本类型的数组中，其保存的是实例数据的大小，也就是int类型的长度**4字节**，如果数组长度是3，这里的实例数据就是**12字节**，以此类推，而这种情况下，同样出现了两段填充的现象，由于我们代码中的数组长度设置为1，所以这里的对象大小为:

> MarkWord(8B)+KlassPointer(8B)+数组长度(4B)+第一段对齐填充(4B)+实例数据区(4B)+第二段对齐填充(4B) = 32B

#### 7.2.5.数组类型（开启指针压缩）

那么如果开启指针压缩又会是什么样的状况呢？有了上面的基础，大家可以先考虑一下，我这里就直接上图了。

**长度为1的基本类型数组**：

![java5-1606098466-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191635.png)

在**对象中存在引用类型（开启指针压缩）中我说过只要开启了指针压缩，我们的类型指针就是占用4个字节，由于是数组，对象头中依旧多了一个存放对象的指针，但是对象头中的对齐填充消失了**，所以其大小为：

> MarkWord(8B)+KlassPointer(4B)+数组长度(4B)+实例数据区(4B)+对齐填充(4B) = 24B

#### 7.2.6.仅存在静态变量

最后一种情况，假设类中仅存在一个静态变量(开启指针压缩)：

```
public class Blog {
    private static Map<String,Object> mapObj = new HashMap<>(16);


    public static void main(String[] args) {
        Blog blog = new Blog();
        int[] intArray = new int[1];
        System.out.println(ClassLayout.parseInstance(blog).toPrintable());
    }
}
```

![java10-1606098466-1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327191741.png)

可以看到其内部并没有实例数据区，原因很简单，我们也说过，大家要记住，只有**类的非静态属性，在生成对象后，才是实例数据**，而静态变量不在其列。

#### 7.2.7.总结

关于如何对象的大小，其实很简单，我们首先关注是否是开启了指针压缩，然后关注其是普通对象还是数组对象，这里做个总结。

如果是**普通对象**，那么只需要计算：**MarkWord+KlassPointer（8B）+实例数据+对齐填充**。

如果是**数组对象**，则需要分两种情况，如果是**开启指针压缩**的情况，那么分为五段：**MarkWord+KlassPointer（4B）+第一段对齐填充+实例数据+第二段对齐填充**。

如果对象中存在**引用类型数据**，则保存的只是指向这个数据的指针，在开启指针压缩的情况下，为4字节，关闭指针压缩为8字节。

如果对象中存在**基本数据类型**，那么保存的就是其实体，这就需要按照8中基本数据类型的大小来灵活计算了。

### 7.3.指针压缩

在本篇文章中我们和**指针压缩**打过多次交道，那么究竟是什么指针压缩？

简单来说，指针压缩就是一种**节约内存**的技术，并且可以**增强内存寻址的效率**，由于在64位系统中，对象中的指针占用8字节,也就是64Bit，我们再来回顾一下，8字节指针可以表示的内存大小是多少？

> **2^64 = 18446744073709552000Bit = 2147483648GB**

很显然，站在**内存的角度**，首先，在当前的硬件条件下，我们几乎不可能达到这种内存级别。其次，64位对象引用需要占用更多的对空间，留给其他数据的空间将会减少，从而加快GC的发生。站在**CPU的角度**，对象引用变大了，CPU能缓存的对象也就少了，每次使用时都需要去内存中取，降低了CPU的效率。所以，在设计时，就引入了指针压缩的概念。

#### 7.3.1.指针压缩原理

我们都知道，指针压缩会将原先的8字节指针，压缩到4字节，那么4字节能表示的内存大小是多少？

> 2^32 = 4GB

这个内存级别，在当前64位机器的大环境下，在大多数的生产环境下已经是不够用了，需要更大的寻址范围，但是刚才我们看到，指针压缩之后，对象指针的大小就是4个字节，那么我们需要了解的就是，**JVM是如何在指针压缩的条件下，提升寻址范围的呢？**

需要注意的一点是：**由于32位操作系统，能够识别的最大内存地址就是4GB，所以指针压缩后也依旧够用，所以32位操作系统不在这个讨论范畴内，这里只针对64位操作系统进行讨论。**

首先我们来看看，指针压缩之后，对象的内存地址存在何种规律：

> 假设这里有三个对象，分别是对象A 8字节，对象B 16字节，对象C 24字节。
>
> 那么其内存地址(假设从00000000)开始，就是:
>
> A：00000000 00000000 00000000 00000000     0x00000000
>
> B：00000000 00000000 00000000 00001000     0x00000008
>
> C：00000000 00000000 00000000 00010000     0x00000010

由于Java中对象存在**8字节对齐**的特性，所以**所有对象的内存地址，后三位永远是0**。那么这里就是JVM在设计上解决这个问题的精妙之处。

首先，在**存储**的时候，JVM会将**对象内存地址的后三位的0抹去（右移3位）**，在**使用**的时候，将**对象的内存地址后三位补0（左移3位）**，这样做有什么好处呢。

按照这种逻辑，在存储的时候，假设有一个对象，所在的内存地址已经达到了8GB，超出了4GB，那么其内存地址就是:**00000010 00000000 00000000 00000000 00000000 **

很显然，这已经超出了32位（4字节）能表示的最大范围，那么依照上文中的逻辑，在存储的时候，JVM将对象地址右移三位，变成**01000000 00000000 00000000 00000000**，而在使用的时候，在后三位补0（左移3位），这样就又回到了最开始的样子：**00000010 00000000 00000000 00000000 00000000 **，就又可以在内存中找到对象，并加载到寄存器中进行使用了。

由于**8字节对齐，内存地址后三位永远是0**这一特殊的规律，JVM使用这一巧妙地设计，将仅占有32位的对象指针，变成实际上可以使用35位，也就是最大可以表示**32GB**的内存地址，这一精妙绝伦的设计，笔者叹为观止。

当然，这里只是说JVM在开启指针压缩下的寻址能力，而实际上64位操作系统的寻址能力是很强大的，如果JVM被分配的内存大于32GB，那么会自动关闭指针压缩，使用8字节的指针进行寻址。

#### 7.3.2.解答遗留问题：为什么不使用16字节对齐

第一节的遗留问题，为什么不用16字节对齐的第二个原因，其实学习完指针压缩之后，答案已经很明了了，我们在使用8字节对齐时并开启指针压缩的情况下，最大的内存表示范围已经达到了32GB，如果大于32GB，关闭指针压缩，就可以获取到非常强大的寻址能力。

当然，如果假设JVM中没有指针压缩，而是开始就设定了对象指针只有8字节，那么此时如果需要又超过32GB的内存寻址能力，那么就需要使用16字节对齐，原理和上面说的相同，如果是16字节对齐，那么对象的内存地址**后4位一定为0**，那么我们在存储和读取的时候分别左移右移4位，就可以仅用32位的指针，获取到36位的寻址能力，寻址能力也就可以达到64GB了。



## 面试题

