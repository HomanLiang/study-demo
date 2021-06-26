[toc]



# Java 面向对象

## 1. 面向对象

每种编程语言，都有自己的操纵内存中元素的方式。

Java 中提供了基本数据类型，但这还不能满足编写程序时，需要抽象更加复杂数据类型的需要。因此，Java 中，允许开发者通过类（类的机制下面会讲到）创建自定义类型。

有了自定义类型，那么数据类型自然会千变万化，所以，必须要有一定的机制，使得它们仍然保持一些必要的、通用的特性。

Java 世界有一句名言：一切皆为对象。这句话，你可能第一天学 Java 时，就听过了。这不仅仅是一句口号，也体现在 Java 的设计上。

- 首先，所有 Java 类都继承自 `Object` 类（从这个名字，就可见一斑）。
- 几乎所有 Java 对象初始化时，都要使用 `new` 创建对象（基本数据类型、String、枚举特殊处理），对象存储在堆中。

```java
// 下面两
String s = "abc";
String s = new String("abc");
```

其中，`String s` 定义了一个名为 s 的引用，它指向一个 `String` 类型的对象，而实际的对象是 `"abc"` 字符串。这就像是，使用遥控器（引用）来操纵电视机（对象）。

与 C/C++ 这类语言不同，程序员只需要通过 `new` 创建一个对象，但不必负责销毁或结束一个对象。负责运行 Java 程序的 Java 虚拟机有一个垃圾回收器，它会监视 `new` 创建的对象，一旦发现对象不再被引用，则会释放对象的内存空间。

### 1.1. 封装

**封装（Encapsulation）是指一种将抽象性函式接口的实现细节部份包装、隐藏起来的方法。**

封装最主要的作用在于我们能修改自己的实现代码，而不用修改那些调用我们代码的程序片段。

适当的封装可以让程式码更容易理解与维护，也加强了程式码的安全性。

封装的优点：

- 良好的封装能够减少耦合。
- 类内部的结构可以自由修改。
- 可以对成员变量进行更精确的控制。
- 隐藏信息，实现细节。

实现封装的步骤：

1. 修改属性的可见性来限制对属性的访问（一般限制为 private）。
2. 对每个值属性提供对外的公共方法访问，也就是创建一对赋取值方法，用于对私有属性的访问。

### 1.2. 继承

继承是 java 面向对象编程技术的一块基石，因为它允许创建分等级层次的类。

继承就是子类继承父类的特征和行为，使得子类对象（实例）具有父类的实例域和方法，或子类从父类继承方法，使得子类具有父类相同的行为。

现实中的例子：

狗和鸟都是动物。如果将狗、鸟作为类，它们可以继承动物类。

![image-20210320145516855](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320145517.png)

类的继承形式：

```java
class 父类 {}

class 子类 extends 父类 {}
```

#### 1.2.1.继承类型

![image-20210320145551644](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320145551.png)

#### 1.2.2.继承的特性

- 子类拥有父类非 private 的属性、方法。
- 子类可以拥有自己的属性和方法，即子类可以对父类进行扩展。
- 子类可以用自己的方式实现父类的方法。
- Java 的继承是单继承，但是可以多重继承，单继承就是一个子类只能继承一个父类，多重继承就是，例如 A 类继承 B 类，B 类继承 C 类，所以按照关系就是 C 类是 B 类的父类，B 类是 A 类的父类，这是 Java 继承区别于 C++ 继承的一个特性。
- 提高了类之间的耦合性（继承的缺点，耦合度高就会造成代码之间的联系越紧密，代码独立性越差）。

#### 1.2.3.继承关键字

继承可以使用 extends 和 implements 这两个关键字来实现继承，而且所有的类都是继承于 `java.lang.Object`，当一个类没有继承的两个关键字，则默认继承 object（这个类在 **java.lang** 包中，所以不需要 **import**）祖先类。

### 1.3. 多态

刚开始学习面向对象编程时，容易被各种术语弄得云里雾里。所以，很多人会死记硬背书中对于术语的定义。

但是，随着应用和理解的深入，应该会渐渐有更进一步的认识，将其融汇贯通的理解。

学习类之前，先让我们思考一个问题：Java 中为什么要引入类机制，设计的初衷是什么？

Java 中提供的基本数据类型，只能表示单一的数值，这用于数值计算，还 OK。但是，如果要抽象模拟现实中更复杂的事物，则无法做到。

试想，如果要让你抽象狗的数据模型，怎么做？狗有眼耳口鼻等器官，有腿，狗有大小，毛色，这些都是它的状态，狗会跑、会叫、会吃东西，这些是它的行为。

类的引入，就是为了抽象这种相对复杂的事物。

对象是用于计算机语言对问题域中事物的描述。**对象通过方法和属性来分别描述事物所具有的行为和状态。**

**类是用于描述同一类的对象的一个抽象的概念，类中定义了这一类对象所具有的行为和状态。**

类可以看成是创建 Java 对象的模板。

什么是方法？扩展阅读：[面向对象编程的弊端是什么？ - invalid s 的回答](https://www.zhihu.com/question/20275578/answer/26577791)

## 2. 类

与大多数面向对象编程语言一样，Java 使用 `class` （类）关键字来表示自定义类型。自定义类型是为了更容易抽象现实事物。

在一个类中，可以设置一静一动两种元素：属性（静）和方法（动）。

- **属性（有的人喜欢称为成员、字段）** - 属性抽象的是事物的状态。类属性可以是任何类型的对象。
- **方法（有的人喜欢称为函数）** - 方法抽象的是事物的行为。

类的形式如下：

![image-20210320145720541](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320145720.png)

## 3. 方法

### 3.1. 方法定义

```java
修饰符 返回值类型 方法名(参数类型 参数名){
    ...
    方法体
    ...
    return 返回值;
}
```

方法包含一个方法头和一个方法体。下面是一个方法的所有部分：

- **修饰符：**修饰符，这是可选的，告诉编译器如何调用该方法。定义了该方法的访问类型。
- **返回值类型 ：**方法可能有返回值。如果没有返回值，这种情况下，返回值类型应设为 void。
- **方法名：**是方法的实际名称。方法名和参数表共同构成方法签名。
- **参数类型：**参数像是一个占位符。当方法被调用时，传递值给参数。这个值被称为实参或变量。参数列表是指方法的参数类型、顺序和参数的个数。参数是可选的，方法可以不包含任何参数。
- **方法体：**方法体包含具体的语句，定义该方法的功能。

示例：

```java
public static int add(int x, int y) {
   return x + y;
}
```

### 3.2. 方法调用

Java 支持两种调用方法的方式，根据方法是否返回值来选择。

当程序调用一个方法时，程序的控制权交给了被调用的方法。当被调用方法的返回语句执行或者到达方法体闭括号时候交还控制权给程序。

当方法返回一个值的时候，方法调用通常被当做一个值。例如：

```java
int larger = max(30, 40);
```

如果方法返回值是 void，方法调用一定是一条语句。例如，方法 println 返回 void。下面的调用是个语句：

```java
System.out.println("Hello World");
```

### 3.3. 构造方法

每个类都有构造方法。如果没有显式地为类定义任何构造方法，Java 编译器将会为该类提供一个默认构造方法。

在创建一个对象的时候，至少要调用一个构造方法。构造方法的名称必须与类同名，一个类可以有多个构造方法。

```java
public class Puppy{
    public Puppy(){
    }

    public Puppy(String name){
        // 这个构造器仅有一个参数：name
    }
}
```

## 4. 变量

Java 支持的变量类型有：

- `局部变量` - 类方法中的变量。
- `实例变量（也叫成员变量）` - 类方法外的变量，不过没有 `static` 修饰。
- `类变量（也叫静态变量）` - 类方法外的变量，用 `static` 修饰。

特性对比：

| 局部变量                                                     | 实例变量（也叫成员变量）                                     | 类变量（也叫静态变量）                                       |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 局部变量声明在方法、构造方法或者语句块中。                   | 实例变量声明在方法、构造方法和语句块之外。                   | 类变量声明在方法、构造方法和语句块之外。并且以 static 修饰。 |
| 局部变量在方法、构造方法、或者语句块被执行的时候创建，当它们执行完成后，变量将会被销毁。 | 实例变量在对象创建的时候创建，在对象被销毁的时候销毁。       | 类变量在第一次被访问时创建，在程序结束时销毁。               |
| 局部变量没有默认值，所以必须经过初始化，才可以使用。         | 实例变量具有默认值。数值型变量的默认值是 0，布尔型变量的默认值是 false，引用类型变量的默认值是 null。变量的值可以在声明时指定，也可以在构造方法中指定。 | 类变量具有默认值。数值型变量的默认值是 0，布尔型变量的默认值是 false，引用类型变量的默认值是 null。变量的值可以在声明时指定，也可以在构造方法中指定。此外，静态变量还可以在静态语句块中初始化。 |
| 对于局部变量，如果是基本类型，会把值直接存储在栈；如果是引用类型，会把其对象存储在堆，而把这个对象的引用（指针）存储在栈。 | 实例变量存储在堆。                                           | 类变量存储在静态存储区。                                     |
| 访问修饰符不能用于局部变量。                                 | 访问修饰符可以用于实例变量。                                 | 访问修饰符可以用于类变量。                                   |
| 局部变量只在声明它的方法、构造方法或者语句块中可见。         | 实例变量对于类中的方法、构造方法或者语句块是可见的。一般情况下应该把实例变量设为私有。通过使用访问修饰符可以使实例变量对子类可见。 | 与实例变量具有相似的可见性。但为了对类的使用者可见，大多数静态变量声明为 public 类型。 |
|                                                              | 实例变量可以直接通过变量名访问。但在静态方法以及其他类中，就应该使用完全限定名：ObejectReference.VariableName。 | 静态变量可以通过：ClassName.VariableName 的方式访问。        |
|                                                              |                                                              | 无论一个类创建了多少个对象，类只拥有类变量的一份拷贝。       |
|                                                              |                                                              | 类变量除了被声明为常量外很少使用。                           |

### 4.1. 变量修饰符

- 访问级别修饰符 - 如果变量是实例变量或类变量，可以添加访问级别修饰符（`public`/`protected`/`private`）
- 静态修饰符 - 如果变量是类变量，需要添加 `static` 修饰
- final - 如果变量使用 fianl 修饰符，就表示这是一个常量，不能被修改。

### 4.2. 创建对象

对象是根据类创建的。在 Java 中，使用关键字 new 来创建一个新的对象。创建对象需要以下三步：

- **声明**：声明一个对象，包括对象名称和对象类型。
- **实例化**：使用关键字 new 来创建一个对象。
- **初始化**：使用 new 创建对象时，会调用构造方法初始化对象。

```java
public class Puppy{
   public Puppy(String name){
      //这个构造器仅有一个参数：name
      System.out.println("小狗的名字是 : " + name );
   }
   public static void main(String[] args){
      // 下面的语句将创建一个Puppy对象
      Puppy myPuppy = new Puppy("tommy");
   }
}
```

### 4.3. 访问实例变量和方法

```java
/* 实例化对象 */
ObjectReference = new Constructor();
/* 访问类中的变量 */
ObjectReference.variableName;
/* 访问类中的方法 */
ObjectReference.methodName();
```

## 5. 访问权限控制

### 5.1. 代码组织

**当编译一个 .java 文件时，在 .java 文件中的每个类都会输出一个与类同名的 .class 文件。**

MultiClassDemo.java 示例：

```java
class MultiClass1 {}

class MultiClass2 {}

class MultiClass3 {}

public class MultiClassDemo {}
```

执行 `javac MultiClassDemo.java` 命令，本地会生成 `MultiClass1.class`、`MultiClass2.class`、`MultiClass3.class`、`MultiClassDemo.class` 四个文件。

**Java 可运行程序是由一组 .class 文件打包并压缩成的一个 .jar 文件**。Java 解释器负责这些文件的查找、装载和解释。**Java 类库实际上是一组类文件（.java 文件）。**

- **其中每个文件允许有一个 public 类，以及任意数量的非 public 类**。
- **public 类名必须和 .java 文件名完全相同，包括大小写。**

程序一般不止一个人编写，会调用系统提供的代码、第三方库中的代码、项目中其他人写的代码等，不同的人因为不同的目的可能定义同样的类名/接口名，这就是命名冲突。

Java 中为了解决命名冲突问题，提供了包（`package`）和导入（`import`）机制。

#### 5.1.1.package

包（`package`）的原则：

- 包类似于文件夹，文件放在文件夹中，类和接口则放在包中。为了便于组织，文件夹一般是一个**有层次的树形结构**，包也类似。
- **包名以逗号 `.` 分隔，表示层次结构。**
- Java 中命名包名的一个惯例是使用域名作为前缀，因为域名是唯一的，一般按照域名的反序来定义包名，比如，域名是：`apache.org`，包名就以 `org.apache` 开头。
- **包名和文件目录结构必须完全匹配。**Java 解释器运行过程如下：
  - 找出环境变量 CLASSPATH，作为 .class 文件的根目录。
  - 从根目录开始，获取包名称，并将逗号 `.` 替换为文件分隔符（反斜杠 `/`），通过这个路径名称去查找 Java 类。

#### 5.1.2.import

同一个包下的类之间互相引用是不需要包名的，可以直接使用。但如果类不在同一个包内，则必须要知道其所在的包，使用有两种方式：

- 通过类的完全限定名
- 通过 import 将用到的类引入到当前类

通过类的完全限定名示例：

```java
public class PackageDemo {
    public static void main (String[]args){
        System.out.println(new java.util.Date());
        System.out.println(new java.util.Date());
    }
}
```

通过 `import` 导入其它包的类到当前类：

```java
import java.util.Date;

public class PackageDemo2 {
    public static void main(String[] args) {
        System.out.println(new Date());
        System.out.println(new Date());
    }
}
```

> 说明：以上两个示例比较起来，显然是 `import` 方式，代码更加整洁。

> 扩展阅读：https://www.cnblogs.com/swiftma/p/5628762.html

### 5.2. 访问权限修饰关键字

访问权限控制的等级，从最大权限到最小权限依次为：

```java
public > protected > 包访问权限（没有任何关键字）> private
```

- `public` - 表示任何类都可以访问；
- `包访问权限` - 包访问权限，没有任何关键字。它表示当前包中的所有其他类都可以访问，但是其它包的类无法访问。
- `protected` - 表示子类可以访问，此外，同一个包内的其他类也可以访问，即使这些类不是子类。
- `private` - 表示其它任何类都无法访问。

## 6. 接口

接口是对行为的抽象，它是抽象方法的集合，利用接口可以达到 API 定义和实现分离的目的。

接口，不能实例化；不能包含任何非常量成员，任何 field 都是隐含着 `public static final` 的意义；同时，没有非静态方法实现，也就是说要么是抽象方法，要么是静态方法。

Java 标准类库中，定义了非常多的接口，比如 `java.util.List`。

```java
public interface Comparable<T> {
    public int compareTo(T o);
}
```

## 7. 抽象类

抽象类是不能实例化的类，用 `abstract` 关键字修饰 `class`，其目的主要是代码重用。除了不能实例化，形式上和一般的 Java 类并没有太大区别，可以有一个或者多个抽象方法，也可以没有抽象方法。抽象类大多用于抽取相关 Java 类的共用方法实现或者是共同成员变量，然后通过继承的方式达到代码复用的目的。

Java 标准库中，比如 `collection` 框架，很多通用部分就被抽取成为抽象类，例如 `java.util.AbstractList`。

1. 抽象类不能被实例化(初学者很容易犯的错)，如果被实例化，就会报错，编译无法通过。只有抽象类的非抽象子类可以创建对象。
2. 抽象类中不一定包含抽象方法，但是有抽象方法的类必定是抽象类。
3. 抽象类中的抽象方法只是声明，不包含方法体，就是不给出方法的具体实现也就是方法的具体功能。
4. 构造方法，类方法（用 static 修饰的方法）不能声明为抽象方法。
5. 抽象类的子类必须给出抽象类中的抽象方法的具体实现，除非该子类也是抽象类。

## 8.this 关键字

### 8.1.消除字段歧义

我敢赌一毛钱，所有的读者，不管男女老少，应该都知道这种用法，毕竟写构造方法的时候经常用啊。谁要不知道，过来，我给你发一毛钱红包，只要你脸皮够厚。

```
public class Writer {
    private int age;
    private String name;

    public Writer(int age, String name) {
        this.age = age;
        this.name = name;
    }
}
```

Writer 类有两个成员变量，分别是 age 和 name，在使用有参构造函数的时候，如果参数名和成员变量的名字相同，就需要使用 this 关键字消除歧义：this.age 是指成员变量，age 是指构造方法的参数。

### 8.2.引用类的其他构造方法

当一个类的构造方法有多个，并且它们之间有交集的话，就可以使用 this 关键字来调用不同的构造方法，从而减少代码量。

比如说，在无参构造方法中调用有参构造方法：

```
public class Writer {
    private int age;
    private String name;

    public Writer(int age, String name) {
        this.age = age;
        this.name = name;
    }

    public Writer() {
        this(18, "沉默王二");
    }
}
```

也可以在有参构造方法中调用无参构造方法：

```
public class Writer {
    private int age;
    private String name;

    public Writer(int age, String name) {
        this();
        this.age = age;
        this.name = name;
    }

    public Writer() {
    }
}
```

需要注意的是，`this()` 必须是构造方法中的第一条语句，否则就会报错。

![image-20210320232056165](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320232056.png)

### 8.3.作为参数传递

在下例中，有一个无参的构造方法，里面调用了 `print()` 方法，参数只有一个 this 关键字。

```
public class ThisTest {
    public ThisTest() {
        print(this);
    }

    private void print(ThisTest thisTest) {
        System.out.println("print " +thisTest);
    }

    public static void main(String[] args) {
        ThisTest test = new ThisTest();
        System.out.println("main " + test);
    }
}
```

来打印看一下结果：

```
print com.cmower.baeldung.this1.ThisTest@573fd745
main com.cmower.baeldung.this1.ThisTest@573fd745
```

从结果中可以看得出来，this 就是我们在 `main()` 方法中使用 new 关键字创建的 ThisTest 对象。

### 8.4.链式调用

学过 JavaScript，或者 jQuery 的读者可能对链式调用比较熟悉，类似于 `a.b().c().d()`，仿佛能无穷无尽调用下去。

在 Java 中，对应的专有名词叫 Builder 模式，来看一个示例。

```
public class Writer {
    private int age;
    private String name;
    private String bookName;

    public Writer(WriterBuilder builder) {
        this.age = builder.age;
        this.name = builder.name;
        this.bookName = builder.bookName;
    }

    public static class WriterBuilder {
        public String bookName;
        private int age;
        private String name;

        public WriterBuilder(int age, String name) {
            this.age = age;
            this.name = name;
        }

        public WriterBuilder writeBook(String bookName) {
            this.bookName = bookName;
            return this;
        }

        public Writer build() {
            return new Writer(this);
        }
    }
}
```

Writer 类有三个成员变量，分别是 age、name 和 bookName，还有它们仨对应的一个构造方法，参数是一个内部静态类 WriterBuilder。

内部类 WriterBuilder 也有三个成员变量，和 Writer 类一致，不同的是，WriterBuilder 类的构造方法里面只有 age 和 name 赋值了，另外一个成员变量 bookName 通过单独的方法 `writeBook()` 来赋值，注意，该方法的返回类型是 WriterBuilder，最后使用 return 返回了 this 关键字。

最后的 `build()` 方法用来创建一个 Writer 对象，参数为 this 关键字，也就是当前的 WriterBuilder 对象。

这时候，创建 Writer 对象就可以通过链式调用的方式。

```
Writer writer = new Writer.WriterBuilder(18,"沉默王二")
                .writeBook("《Web全栈开发进阶之路》")
                .build();
```

### 8.5.在内部类中访问外部类对象

说实话，自从 Java 8 的函数式编程出现后，就很少用到 this 在内部类中访问外部类对象了。来看一个示例：

```
public class ThisInnerTest {
    private String name;

    class InnerClass {
        public InnerClass() {
            ThisInnerTest thisInnerTest = ThisInnerTest.this;
            String outerName = thisInnerTest.name;
        }
    }
}
```

在内部类 InnerClass 的构造方法中，通过外部类.this 可以获取到外部类对象，然后就可以使用外部类的成员变量了，比如说 name。

## 9.深克隆和浅克隆

### 9.1.基本概念
**浅复制(浅克隆)**

被复制对象的所有变量都含有与原来的对象相同的值，而所有的对其他对象的引用仍然指向原来的对象。换言之，浅复制仅仅复制所拷贝的对象，而不复制它所引用的对象。

**深复制(深克隆)**

被复制对象的所有变量都含有与原来的对象相同的值，除去那些引用其他对象的变量。那些引用其他对象的变量将指向被复制过的新对象，而不再是原有的那些被引用的对象。换言之，深复制把要复制的对象所引用的对象都复制了一遍。

**实现java深复制和浅复制的最关键的就是要实现Object中的clone()方法。**

### 9.2.如何使用clone()方法
首先我们来看一下Cloneable接口：
官方解释：
1. 实现此接口则可以使用java.lang.Object 的clone()方法，否则会抛出CloneNotSupportedException 异常
2. 实现此接口的类应该使用公共方法覆盖clone方法
3. 此接口并不包含clone 方法，所以实现此接口并不能克隆对象，这只是一个前提，还需覆盖上面所讲的clone方法。
```
public interface Cloneable { }
```
看看Object里面的Clone()方法：
1. clone()方法返回的是Object类型，所以必须强制转换得到克隆后的类型
2. clone()方法是一个native方法，而native的效率远远高于非native方法，
3. 可以发现clone方法被一个Protected修饰，所以可以知道必须继承Object类才能使用，而Object类是所有类的基类，也就是说所有的类都可以使用clone方法
```
protected native Object clone() throws CloneNotSupportedException;
```
小试牛刀：
```
public class Person {
    public void testClone(){
        super.clone(); // 报错了
    }
}
```
事实却是clone()方法报错了，那么肯定奇怪了，既然Object是一切类的基类，并且clone的方法是Protected的，那应该是可以通过super.clone()方法去调用的，然而事实却是会抛出CloneNotSupportedException异常, 官方解释如下：
1. 对象的类不支持Cloneable接口
2. 覆盖方法的子类也可以抛出此异常表示无法克隆实例。

所以我们更改代码如下：
```
public class Person implements Cloneable{
    public void testClone(){
        try {
            super.clone();
            System.out.println("克隆成功");
        } catch (CloneNotSupportedException e) {
            System.out.println("克隆失败");
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        Person p = new Person();
        p.testClone();
    }
}
```
要注意，必须将克隆方法写在try-catch块中，因为clone方法会把异常抛出，当然程序也要求我们try-catch。
### 9.3.java.lang.object规范中对clone方法的约定
1. 对任何的对象x，都有x.clone() !=x 因为克隆对象与原对象不是同一个对象
2. 对任何的对象x，都有x.clone().getClass()= =x.getClass()//克隆对象与原对象的类型一样
3. 如果对象x的equals()方法定义恰当，那么x.clone().equals(x)应该成立

对于以上三点要注意，这3项约定并没有强制执行，所以如果用户不遵循此约定，那么将会构造出不正确的克隆对象，所以根据effective java的建议：
> 谨慎的使用clone方法，或者尽量避免使用。

### 9.4.深复制实例
深拷贝实现的是对所有可变(没有被final修饰的引用变量)引用类型的成员变量都开辟内存空间所以一般深拷贝对于浅拷贝来说是比较耗费时间和内存开销的。
#### 9.4.1.重写clone方法实现深拷贝
学生类：
```
public class Student implements Cloneable {
    private String name;
    private int age;
    public Student(String name, int age){
        this.name = name;
        this.age = age;
    }

    @Override
    protected Object clone()  {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```
老师类：
```
public class Teacher implements Cloneable{
    private String name;
    private int age;
    private Student student;

    public Teacher(String name, int age, Student student){
        this.name = name;
        this.age = age;
        this.student = student;
    }
    // 覆盖
    @Override
    public Object clone() {
        Teacher t = null;
        try {
            t = (Teacher) super.clone();
            t.student = (Student)student.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return t;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
}
```
测试端：
```
public class test {
    public static void main(String[] args) {
        Student s = new Student("学生1", 11);
        Teacher origin = new Teacher("老师原对象", 23, s);
        System.out.println("克隆前的学生姓名：" + origin.getStudent().getName());
        Teacher clone = (Teacher) origin.clone();
        // 更改克隆后的学生信息 更改了姓名
        clone.getStudent().setName("我是克隆对象更改后的学生2");
        System.out.println("克隆后的学生姓名：" + clone.getStudent().getName());
    }
}
```
运行结果：
克隆前的学生姓名：学生1
克隆后的学生姓名：我是克隆对象更改后的学生2
#### 9.4.2.序列化实现深克隆
Teacher:
```
public class Teacher implements Serializable{
    private String name;
    private int age;
    private Student student;

    public Teacher(String name, int age, Student student){
        this.name = name;
        this.age = age;
        this.student = student;
    }
    // 深克隆
    public Object deepClone() throws IOException, ClassNotFoundException {
        // 序列化
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        // 反序列化
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bis);
        return ois.readObject();
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
```
Student:
```
public class Student implements Serializable {
    private String name;
    private int age;
    public Student(String name, int age){
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```
client：
```
public class test {
    public static void main(String[] args) {
        try {
            Student s = new Student("学生1", 11);
            Teacher origin = new Teacher("老师原对象", 23, s);
            System.out.println("克隆前的学生姓名：" + origin.getStudent().getName());
            Teacher clone = (Teacher) origin.deepClone();
            // 更改克隆后的d学生信息 更改了姓名
            clone.getStudent().setName("我是克隆对象更改后的学生2");
            System.out.println("克隆后的学生姓名：" + clone.getStudent().getName());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
```
**当然这些工作都有现成的轮子了，借助于Apache Commons可以直接实现：**

**浅克隆**：BeanUtils.cloneBean(Object obj);

**深克隆**：SerializationUtils.clone(T object);



## X.面试题

### X.1.简单聊一下关于你对`Object`的理解

在 Java 中，只有基本数据类型不是对象，比如，数值，布尔和字符类型的值都不是对象。而其余的数据类型都是继承自一个名为`Object`的类，这个类是所有类的始祖，每个类都是由`Object`类扩展而来。

如果一个类继承自`Object`类，我们可以将`extends Object`给省略掉，如果在一个类的定义中没有明确的指出哪个是它的父类，那么`Object`类就认为是这个类的父类。

![2020-08-14-123909](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320230151.jpg)



### X.2.`Object`类中有一个`registerNatives`方法，对此你了解多少？

从方法的命名上我们就可以看出，该方法是用于注册本地（native）方法，主要是为了服务于JNI的，它主要是提供了 java 类中的方法与对应 C++ 代码中的方法的**映射**，方便jvm去查找调用 C++ 中的方法。

![2020-08-15-033936](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320230220.png)



### X.3.`Object`类中有`clone`方法，聊聊你对这个方法的认识

`clone`方法是`Object`类的一个`protected`的方法，我们可以这样去应用这个方法

1. 实现`Cloneable`接口
2. 重写`clone`方法，并指定`public`修饰符。

![2020-08-16-032428](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320230235.png)



### X.4.为什么我们一定要去实现`Cloneable`接口，而不是直接去重写这个方法呢？

我们通过源码可以发现这是一个空的接口，`clone`是从`Object`类继承的。这个接口只是作为一个标记，指示类设计者了解克隆继承。对象对于克隆也很"偏执"，如果一个对象请求克隆，但没有实现这个接口，就会生成一个异常。

在 Java 中，`Cloneable`这样的接口叫做标记接口，标记接口不包括任何方法，它的唯一作用就是允许在类型查询的时候使用`instanceof`：

```
if (obj instanceof Cloneable){    
	//TODO
}
```



### X.5.说一说你对关于深克隆和浅克隆的认识

首先来说一下`Object`类是如何实现`clone`，它对这个对象一无所知，所以只能逐个域的进行拷贝。如果对象中的所有数据域都是数值或其他基本类型，拷贝这些域没有任何问题，但是如果对象中包含子对象的引用，拷贝域就会得到相同子对象的另一个引用，这样一来，原对象和克隆对象仍然会去共享一些信息。这种`Object`类默认实现的`clone`方法称为**浅拷贝**（Shallow Clone）。

这里需要注意，关于浅克隆的安全性，如果原对象和浅克隆对象共享的子对象是不可变的，那么这种共享就是安全的。如果子对象属于一个不可变的类，如`String`，就是这种情况。或者在对象的生命期中，子对象一直包含不变的常量 ，没有更改器方法会改变它，也没有方法会生成它的引用，这种情况同样是安全的。

不过子类对象通常是可变的，这时我们就需要定义**深拷贝**（Deep Clone），来克隆这个类的所有子对象。

具体实现方法如下：

```
public Test clone() throws CloneNotSupportedException{
	//拷贝该对象    
	Test cloned = (Test)super.clone();     
	//拷贝该对象中的可变域    
	cloned.time = (Date) time.clone();    
	return cloned;
}
```

这里需要提到的一点是：

虽然我们已经学习了`clone`的两种用法，但是在实际的编码中还是尽量少用这个方法，它具有天生的不稳定性，仅仅了解即可。即使是Java的标准库中也只有5%的类实现了这个方法。

我们可以使用Java的对象串行化特性来实现克隆对象，虽然效率不高，但是很安全，而且很容易实现。

![2020-08-16-032446](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320230400.png)



### X.6.关于`equals`方法，说说是什么？

`Object`类中的`equals`方法用于检测一个对象是否等于另一个对象。在`Object`类中，这个方法将判断两个对象是否具有相同的引用。如果两个对象具有相同的引用，它们一定是相等的。

![2020-08-16-032458](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320230414.png)



### X.7.有没有自己去重写过`equals`方法呢？

当然，这个我有笔记～

![2020-08-16-032530](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320230437.png)



### X.8.不限于`Object`，聊聊`hashCode`

在Java中，hash code是**由对象导出的一个整型值**，以下是几个常见哈希值的算法：

1. `Object`类的`hashCode()`。返回对象的内存地址经过处理后的结构，由于每个对象的内存地址都不一样，所以哈希码也不一样。
2. `String`类的`hashCode()`。根据`String`类包含的字符串的内容，根据一种特殊算法返回哈希码，只要字符串所在的堆空间相同，返回的哈希码也相同。
3. `Integer`类，返回的哈希码就是`Integer`对象里所包含的那个整数的数值，例如`Integer i1=new Integer(100`)，`i1.hashCode`的值就是100 。由此可见，2个一样大小的`Integer`对象，返回的哈希码也一样。



### X.9.说说`Equals`和 `Hashcode`的关系

这两个其实确切意义上并没有什么联系，前提是我们不会在HashSet，HashMap这种本质是散列表的数据结构中使用，如果我们要在HashSet，HashMap这种本质是散列表的数据结构中使用，在重写equals方法的同时也要重写hashCode方法，以便用户将对象插入到散列表中，否则会导致数据不唯一，内存泄漏等各种问题。

1.`hashCode`是为了提高在散列结构存储中查找的效率，在线性表中没有作用。

2.`equals()`和`hashCode()`需要同时覆盖，而且定义必须一致，也就是说equals比较了哪些域，hashCode就会对哪些域进行hash值的处理。

3.若两个对象`equals()`返回true，则`hashCode()`有必要也返回相同的值。

4.若两个对象`equals()`返回false，则`hashCode()`不一定返回不同的值。

5.若两个对象`hashCode()`返回相同的值，则`equals()`不一定返回true。

6.若两个对象`hashCode()`返回不同值，则`equals()`一定返回false。

7.同一对象在执行期间若已经存储在集合中，则不能修改影响hashCode值的相关信息，否则会导致内存泄露问题。



### X.10.浅析Java中的static关键字

面试官Q1：请说说static关键字，你在项目中是怎么使用的？

- static 关键字可以用来修饰：属性、方法、内部类、代码块；
- static 修饰的资源属于**类级别**，是全体对象实例共享的资源；
- 使用 static 修饰的属性，静态属性是在类的加载期间初始化的，使用**类名.属性**访问

**案例说明**

**①修饰成员变量**

```
package com.ant.param;
 public class StaticFieldDemo {
     public static void main(String[] args) {
         Foo f1 = new Foo();
         Foo f2 = new Foo();
         Foo f3 = new Foo();
         System.out.println(f1.id + " " + f2.id + " " + f3.id );
     }
 }
class Foo{
    int id;
}
运行结果如下：
0 0 0
```

上面的代码我们很熟悉，根据Foo构造出的每一个对象都是独立存在的，保存有自己独立的成员变量，相互不会影响，他们在内存中的示意如下:

![image-20210320234620765](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320234620.png)

从上图中可以看出，f1、f2和f3三个变量引用的对象分别存储在内存中堆区域的不同地址中，所以他们之间相互不会干扰。对象的成员属性都在这了，由每个对象自己保存。f1.id、f2.id、f3.id相当于“每个人一个水杯”。

我们对上面的代码，做如下修改：

```
package com.ant.param;
 public class StaticFieldDemo {
     public static void main(String[] args) {
         Foo f1 = new Foo();
         Foo f2 = new Foo();
         Foo f3 = new Foo();
         System.out.println(f1.id + " " + f2.id + " " +
         f3.id + " " + Foo.i);
     }
}
class Foo{
    int id;
    static int i=0;
    public Foo(){
        id = i++;
    }
}
运行结果如下：
0 1 2 3
```

程序执行过程内存图如下所示：

**第 1 步：加载类**

![image-20210320234657362](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320234657.png)

static修饰的变量在类加载期间初始化，且在方法区中分配，属于线程共享区，所有的对象实例共享一份数据。

**第 2步：继续加载类**

![image-20210320234724112](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320234724.png)

***\*第 3步：继续加载类\****

 ![image-20210320234749272](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320234749.png)

最终加载结果如上述三个步骤

 

**②修饰成员方法**

static的另一个作用，就是修饰成员方法。相比于修饰成员属性，修饰成员方法对于数据的存储上面并没有多大的变化，因为我们从上面可以看出，方法本来就是存放在类的定义当中的(方法区)。static修饰成员方法最大的作用，就是可以使用"**类名.方法名"**的方式操作方法，避免了先要new出对象的繁琐和资源消耗，我们可能会经常在帮助类中看到它的使用：

```
package com.ant.param;
  
 public class StaticFieldDemo {
     private static void print(){
         System.out.println("hello");
     }
     public static void main(String[] args) {
         StaticFieldDemo.print();
     }
}
```

**③修饰静态代码块**

静态代码块是在类加载期间运行的代码块，由于类只加载一次，所以静态代码块只执行一次！静态代码块用途很常见,一般用来在类加载以后初始化一些静态资源时候使用。如：加载配置文件等

```
package com.ant.param;
  
 public class StaticBlockDemo {
     public static void main(String[] args) {
         Foo foo = new Foo();
     }
 }
  
 class Foo{
    //代码块，在创建对象的时候执行，使用很少，和构造器差不多
    {
        System.out.println("创建对象了！");
    }
    //静态代码块，在类加载期间执行，用于加载配置文件或者其他信息等
    static{
        System.out.println("类加载了！");
    }
    public Foo(){
        System.out.println("调用了构造器！");
    }
}
```

静态块用法：将多个类成员放在一起初始化，使得程序更加规整，对理解对象的初始化过程非常关键；

在我的印象中，这些问题一般初中级Java工程师会被问到，都是很常规的面试题，您会了吗？



### X.11.浅析Java中的final关键字？

面试官Q1：请谈谈你对final关键字的理解？

说起final关键字，大家想必不会陌生，我们会使用它定义一些常量、定义方法或者在匿名内部类的时候也会使用到final关键字。

我们先总结一下它的相关用法：

- 可以用来修饰类，修饰的类无法被继承，例如String类、Math类、Integer类等；
- 可以用来修饰方法，修饰的方法无法被重写；
- 可以用来修饰变量，包括成员变量和局部变量，该变量只能被赋值一次且它的值无法被改变，对于成员变量来讲，我们必须在声明时或者构造方法中对它赋值；

**修饰类**

用final修饰的类，不能被继承，其实在实际项目开发中，很少有看到使用final类的，对于开发人员来讲，如果我们写的类被继承的越多，就说明我们写的类越有价值，越成功。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320235059.webp)

**修饰方法**

final也可以声明方法。方法前面加上final关键字，代表这个方法不可以被子类的方法重写。如果你认为一个方法的功能已经足够完整了，子类中不需要改变的话，你可以声明此方法为final。final方法比非final方法要快，因为在编译的时候已经静态绑定了，不需要在运行时再动态绑定。下面是final方法的例子：

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320235112.webp)

**修饰变量**

final成员变量表示常量，只能被赋值一次，赋值后值不再改变；

final修饰一个基本数据类型时，表示该基本数据类型的值一旦在初始化后便不能发生变化；

final修饰一个引用类型时，则在对其初始化之后便不能再让其指向其他对象了，但该引用所指向的对象的内容是可以发生变化的；

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320235124.webp)

当函数的参数类型声明为final时，说明该参数是只读型的。即你可以读取使用该参数，但是无法改变该参数的值；

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320235130.png)



### X.12.说说hashCode() 和 equals() 之间的关系？

先祭一张图，可以思考一下为什么？

![java4-1572150703](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320235543.jpg)

#### X.12.1.介绍

`equals()` 的作用是用来判断两个对象是否相等。

`hashCode()` 的作用是获取哈希码，也称为散列码；它实际上是返回一个int整数。这个哈希码的作用是确定该对象在哈希表中的索引位置。

#### X.12.2.关系

我们以“类的用途”来将 `hashCode()`  和 `equals()` 的关系”分2种情况来说明。

**X.12.2.1.不会创建“类对应的散列表”**

这里所说的“不会创建类对应的散列表”是说：我们不会在HashSet, Hashtable, HashMap等等这些本质是散列表的数据结构中，用到该类。例如，不会创建该类的HashSet集合。

**在这种情况下，该类的“hashCode() 和 equals() ”没有半毛钱关系的！**equals() 用来比较该类的两个对象是否相等。而hashCode() 则根本没有任何作用。

下面，我们通过示例查看类的两个对象相等 以及 不等时hashCode()的取值。

```
import java.util.*;
import java.lang.Comparable;

/**
 * @desc 比较equals() 返回true 以及 返回false时， hashCode()的值。
 *
 */
public class NormalHashCodeTest{

    public static void main(String[] args) {
        // 新建2个相同内容的Person对象，
        // 再用equals比较它们是否相等
        Person p1 = new Person("eee", 100);
        Person p2 = new Person("eee", 100);
        Person p3 = new Person("aaa", 200);
        System.out.printf("p1.equals(p2) : %s; p1(%d) p2(%d)n", p1.equals(p2), p1.hashCode(), p2.hashCode());
        System.out.printf("p1.equals(p3) : %s; p1(%d) p3(%d)n", p1.equals(p3), p1.hashCode(), p3.hashCode());
    }

    /**
     * @desc Person类。
     */
    private static class Person {
        int age;
        String name;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String toString() {
            return name + " - " +age;
        }

        /** 
         * @desc 覆盖equals方法 
         */  
        public boolean equals(Object obj){  
            if(obj == null){  
                return false;  
            }  

            //如果是同一个对象返回true，反之返回false  
            if(this == obj){  
                return true;  
            }  

            //判断是否类型相同  
            if(this.getClass() != obj.getClass()){  
                return false;  
            }  

            Person person = (Person)obj;  
            return name.equals(person.name) && age==person.age;  
        } 
    }
}
```

运行结果：

```
p1.equals(p2) : true; p1(1169863946) p2(1901116749)
p1.equals(p3) : false; p1(1169863946) p3(2131949076)
```

从结果也可以看出：p1和p2相等的情况下，hashCode()也不一定相等。

**X.12.2.2.会创建“类对应的散列表”**

这里所说的“会创建类对应的散列表”是说：我们会在HashSet, Hashtable, HashMap等等这些本质是散列表的数据结构中，用到该类。例如，会创建该类的HashSet集合。

在这种情况下，该类的“hashCode() 和 equals() ”是有关系的：

- **如果两个对象相等，那么它们的hashCode()值一定相同。**这里的相等是指，通过equals()比较两个对象时返回true。
- **如果两个对象hashCode()相等，它们并不一定相等**。因为在散列表中，hashCode()相等，即两个键值对的哈希值相等。然而哈希值相等，并不一定能得出键值对相等。补充说一句：“两个不同的键值对，哈希值相等”，这就是哈希冲突。

此外，在这种情况下。若要判断两个对象是否相等，除了要覆盖equals()之外，也要覆盖hashCode()函数。否则，equals()无效。

举例，创建Person类的HashSet集合，必须同时覆盖Person类的equals() 和 hashCode()方法。 

如果单单只是覆盖equals()方法。我们会发现，equals()方法没有达到我们想要的效果。

```
import java.util.*;
import java.lang.Comparable;

/**
 * @desc 比较equals() 返回true 以及 返回false时， hashCode()的值。
 *
 */
public class ConflictHashCodeTest1{

    public static void main(String[] args) {
        // 新建Person对象，
        Person p1 = new Person("eee", 100);
        Person p2 = new Person("eee", 100);
        Person p3 = new Person("aaa", 200);

        // 新建HashSet对象 
        HashSet set = new HashSet();
        set.add(p1);
        set.add(p2);
        set.add(p3);

        // 比较p1 和 p2， 并打印它们的hashCode()
        System.out.printf("p1.equals(p2) : %s; p1(%d) p2(%d)n", p1.equals(p2), p1.hashCode(), p2.hashCode());
        // 打印set
        System.out.printf("set:%sn", set);
    }

    /**
     * @desc Person类。
     */
    private static class Person {
        int age;
        String name;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String toString() {
            return "("+name + ", " +age+")";
        }

        /** 
         * @desc 覆盖equals方法 
         */  
        @Override
        public boolean equals(Object obj){  
            if(obj == null){  
                return false;  
            }  

            //如果是同一个对象返回true，反之返回false  
            if(this == obj){  
                return true;  
            }  

            //判断是否类型相同  
            if(this.getClass() != obj.getClass()){  
                return false;  
            }  

            Person person = (Person)obj;  
            return name.equals(person.name) && age==person.age;  
        } 
    }
}
```

运行结果：

```
p1.equals(p2) : true; p1(1169863946) p2(1690552137)
set:[(eee, 100), (eee, 100), (aaa, 200)]
```

结果分析：

我们重写了Person的equals()。但是，很奇怪的发现：HashSet中仍然有重复元素：p1 和 p2。为什么会出现这种情况呢？

**这是因为虽然p1 和 p2的内容相等，但是它们的hashCode()不等；所以，HashSet在添加p1和p2的时候，认为它们不相等。**

那同时覆盖equals() 和 hashCode()方法呢？

```
import java.util.*;
import java.lang.Comparable;

/**
 * @desc 比较equals() 返回true 以及 返回false时， hashCode()的值。
 *
 */
public class ConflictHashCodeTest2{

    public static void main(String[] args) {
        // 新建Person对象，
        Person p1 = new Person("eee", 100);
        Person p2 = new Person("eee", 100);
        Person p3 = new Person("aaa", 200);
        Person p4 = new Person("EEE", 100);

        // 新建HashSet对象 
        HashSet set = new HashSet();
        set.add(p1);
        set.add(p2);
        set.add(p3);

        // 比较p1 和 p2， 并打印它们的hashCode()
        System.out.printf("p1.equals(p2) : %s; p1(%d) p2(%d)n", p1.equals(p2), p1.hashCode(), p2.hashCode());
        // 比较p1 和 p4， 并打印它们的hashCode()
        System.out.printf("p1.equals(p4) : %s; p1(%d) p4(%d)n", p1.equals(p4), p1.hashCode(), p4.hashCode());
        // 打印set
        System.out.printf("set:%sn", set);
    }

    /**
     * @desc Person类。
     */
    private static class Person {
        int age;
        String name;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String toString() {
            return name + " - " +age;
        }

        /** 
         * @desc重写hashCode 
         */  
        @Override
        public int hashCode(){  
            int nameHash =  name.toUpperCase().hashCode();
            return nameHash ^ age;
        }

        /** 
         * @desc 覆盖equals方法 
         */  
        @Override
        public boolean equals(Object obj){  
            if(obj == null){  
                return false;  
            }  

            //如果是同一个对象返回true，反之返回false  
            if(this == obj){  
                return true;  
            }  

            //判断是否类型相同  
            if(this.getClass() != obj.getClass()){  
                return false;  
            }  

            Person person = (Person)obj;  
            return name.equals(person.name) && age==person.age;  
        } 
    }
}
```

运行结果：

```
p1.equals(p2) : true; p1(68545) p2(68545)
p1.equals(p4) : false; p1(68545) p4(68545)
set:[aaa - 200, eee - 100]
```

结果分析：

这下，equals()生效了，HashSet中没有重复元素。

比较p1和p2，我们发现：它们的hashCode()相等，通过equals()比较它们也返回true。所以，p1和p2被视为相等。

比较p1和p4，我们发现：虽然它们的hashCode()相等；但是，通过equals()比较它们返回false。所以，p1和p4被视为不相等。

#### X.12.3.原则

**X.12.3.1.同一个对象（没有发生过修改）无论何时调用hashCode()得到的返回值必须一样。**
如果一个key对象在put的时候调用hashCode()决定了存放的位置，而在get的时候调用hashCode()得到了不一样的返回值，这个值映射到了一个和原来不一样的地方，那么肯定就找不到原来那个键值对了。

**X.12.3.2.hashCode()的返回值相等的对象不一定相等，通过hashCode()和equals()必须能唯一确定一个对象。**不相等的对象的hashCode()的结果可以相等。hashCode()在注意关注碰撞问题的时候，也要关注生成速度问题，完美hash不现实。

**X.12.3.3.一旦重写了equals()函数（重写equals的时候还要注意要满足自反性、对称性、传递性、一致性），就必须重写hashCode()函数。**而且hashCode()的生成哈希值的依据应该是equals()中用来比较是否相等的字段。

如果两个由equals()规定相等的对象生成的hashCode不等，对于hashMap来说，他们很可能分别映射到不同位置，没有调用equals()比较是否相等的机会，两个实际上相等的对象可能被插入不同位置，出现错误。其他一些基于哈希方法的集合类可能也会有这个问题



### X.13.抽象类和接口的区别有哪些？

- 抽象类中可以没有抽象方法；接口中的方法必须是抽象方法；
- 抽象类中可以有普通的成员变量；接口中的变量必须是 static final 类型的，必须被初始化,接口中只有常量，没有变量
- 抽象类只能单继承，接口可以继承多个父接口；
- Java 8 中接口中会有 default 方法，即方法可以被实现。

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005130.png)



### X.14.创建对象初始化顺序

创建顺序如下：

1. 父类静态成员（包括方法和变量，按顺序初始化）
1. 子类静态成员（包括方法和变量，按顺序初始化）
1. 父类成员变量（包括非静态代码块）
1. 父类构造方法
1. 子类成员变量（包括非静态代码块）
1. 子类构造方法

验证代码：
```
// 主类，用来创建子类对象，验证我们的结果
public class Main {
    public static void main(String[] args) {
        new Son();
    }
}

// 书类，用于测试对象成员变量
class Book{
    public Book(String user){
        System.out.println(user + "成员变量");
    }
}

// 子类
class Son extends Fa{
    static Book book= new Book("子类静态");
    static{
        System.out.println("子类静态代码块");
    }
    
    Book sBook = new Book("子类");
    {
        System.out.println("子类非静态代码块");
    }
    
    public Son(){
        System.out.println("子类构造方法");
    }
}

// 父类
class Fa{
    static Book book= new Book("父类静态");
    static{
        System.out.println("父类静态代码块");
    }
    
    Book fBook = new Book("父类");
    {
        System.out.println("父类非静态代码块");
    }

    public Fa(){
        System.out.println("父类构造方法");
    }
}
```
输出结果：
```
父类静态成员变量
父类静态代码块
子类静态成员变量
子类静态代码块
父类成员变量
父类非静态代码块
父类构造方法
子类成员变量
子类非静态代码块
子类构造方法
```

### X.15.Objects.equals

#### X.15.1.值是null的情况

1. `a.equals(b)`, a 是null, 抛出NullPointException异常。

2. `a.equals(b)`, a不是null, b是null,  返回false

3. `Objects.equals(a, b)`比较时， 若a 和 b 都是null, 则返回 true, 如果a 和 b 其中一个是null, 另一个不是null, 则返回false。注意：不会抛出空指针异常。

```
null.equals("abc")    →   抛出 NullPointerException 异常  
"abc".equals(null)    →   返回 false  
null.equals(null)     →   抛出 NullPointerException 异常  
Objects.equals(null, "abc")    →   返回 false  
Objects.equals("abc",null)     →   返回 false  
Objects.equals(null, null)     →   返回 true  
```

#### X.15.2.值是空字符串的情况

1. a 和 b 如果都是空值字符串："", 则 `a.equals(b)`, 返回的值是true, 如果a和b其中有一个不是空值字符串，则返回false;

2. 这种情况下 `Objects.equals` 与情况1 行为一致。

```
"abc".equals("")    →   返回 false  
"".equals("abc")    →   返回 false  
"".equals("")       →   返回 true  
Objects.equals("abc", "")    →   返回 false  
Objects.equals("","abc")     →   返回 false  
Objects.equals("","")        →   返回 true  
```

#### X.15.3.源码分析

##### X.15.3.1.源码

```
public final class Objects {  
    private Objects() {  
        throw new AssertionError("No java.util.Objects instances for you!");  
    }  
   
    /**  
     * Returns {@code true} if the arguments are equal to each other  
     * and {@code false} otherwise.  
     * Consequently, if both arguments are {@code null}, {@code true}  
     * is returned and if exactly one argument is {@code null}, {@code  
     * false} is returned.  Otherwise, equality is determined by using  
     * the {@link Object#equals equals} method of the first  
     * argument.  
     *  
     * @param a an object  
     * @param b an object to be compared with {@code a} for equality  
     * @return {@code true} if the arguments are equal to each other  
     * and {@code false} otherwise  
     * @see Object#equals(Object)  
     */  
    public static boolean equals(Object a, Object b) {  
        return (a == b) || (a != null && a.equals(b));  
    }  
```

##### X.15.3.2.说明

首先，进行了对象地址的判断，如果是真，则不再继续判断。

如果不相等，后面的表达式的意思是，先判断a不为空，然后根据上面的知识点，就不会再出现空指针。

所以，如果都是null，在第一个判断上就为true了。如果不为空，地址不同，就重要的是判断a.equals(b)。





