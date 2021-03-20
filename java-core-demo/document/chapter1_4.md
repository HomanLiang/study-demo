[toc]



# Java 方法

## 1. 方法的使用

### 1.1. 方法定义

方法定义语法格式：

```
[修饰符] 返回值类型 方法名([参数类型 参数名]){
    ...
    方法体
    ...
    return 返回值;
}
```

示例：

```
public static void main(String[] args) {
    System.out.println("Hello World");
}
```

方法包含一个方法头和一个方法体。下面是一个方法的所有部分：

- **修饰符** - 修饰符是可选的，它告诉编译器如何调用该方法。定义了该方法的访问类型。
- **返回值类型** - 返回值类型表示方法执行结束后，返回结果的数据类型。如果没有返回值，应设为 void。
- **方法名** - 是方法的实际名称。方法名和参数表共同构成方法签名。
- **参数类型** - 参数像是一个占位符。当方法被调用时，传递值给参数。参数列表是指方法的参数类型、顺序和参数的个数。参数是可选的，方法可以不包含任何参数。
- **方法体** - 方法体包含具体的语句，定义该方法的功能。
- **return** - 必须返回声明方法时返回值类型相同的数据类型。在 void 方法中，return 语句可有可无，如果要写 return，则只能是 `return;` 这种形式。

### 1.2. 方法的调用

当程序调用一个方法时，程序的控制权交给了被调用的方法。当被调用方法的返回语句执行或者到达方法体闭括号时候交还控制权给程序。

Java 支持两种调用方法的方式，根据方法是否有返回值来选择。

- 有返回值方法 - 有返回值方法通常被用来给一个变量赋值或代入到运算表达式中进行计算。

    ```
    int larger = max(30, 40);
    ```

- 无返回值方法 - 无返回值方法只能是一条语句。

    ```
    System.out.println("Hello World");
    ```

#### 递归调用

Java 支持方法的递归调用（即方法调用自身）。

> 🔔 注意：
>
> - 递归方法必须有明确的结束条件。
> - 尽量避免使用递归调用。因为递归调用如果处理不当，可能导致栈溢出。

斐波那契数列（一个典型的递归算法）示例：

```
public class RecursionMethodDemo {
    public static int fib(int num) {
        if (num == 1 || num == 2) {
            return 1;
        } else {
            return fib(num - 2) + fib(num - 1);
        }
    }

    public static void main(String[] args) {
        for (int i = 1; i < 10; i++) {
            System.out.print(fib(i) + "\t");
        }
    }
}
```

## 2. 方法参数

在 C/C++ 等编程语言中，方法的参数传递一般有两种形式：

- 值传递 - 值传递的参数被称为形参。值传递时，传入的参数，在方法中的修改，不会在方法外部生效。
- 引用传递 - 引用传递的参数被称为实参。引用传递时，传入的参数，在方法中的修改，会在方法外部生效。

那么，Java 中是怎样的呢？

**Java 中只有值传递。**

示例一：

```
public class MethodParamDemo {
    public static void method(int value) {
        value =  value + 1;
    }
    public static void main(String[] args) {
        int num = 0;
        method(num);
        System.out.println("num = [" + num + "]");
        method(num);
        System.out.println("num = [" + num + "]");
    }
}
// Output:
// num = [0]
// num = [0]
```

示例二：

```
public class MethodParamDemo2 {
    public static void method(StringBuilder sb) {
        sb = new StringBuilder("B");
    }

    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("A");
        System.out.println("sb = [" + sb.toString() + "]");
        method(sb);
        System.out.println("sb = [" + sb.toString() + "]");
        sb = new StringBuilder("C");
        System.out.println("sb = [" + sb.toString() + "]");
    }
}
// Output:
// sb = [A]
// sb = [A]
// sb = [C]
```

说明：

以上两个示例，无论向方法中传入的是基础数据类型，还是引用类型，在方法中修改的值，在外部都未生效。

Java 对于基本数据类型，会直接拷贝值传递到方法中；对于引用数据类型，拷贝当前对象的引用地址，然后把该地址传递过去，所以也是值传递。

> 扩展阅读：
>
> [图解 Java 中的参数传递](https://zhuanlan.zhihu.com/p/24556934?refer=dreawer)

## 3. 方法修饰符

前面提到了，Java 方法的修饰符是可选的，它告诉编译器如何调用该方法。定义了该方法的访问类型。

Java 方法有好几个修饰符，让我们一一来认识一下：

### 3.1. 访问控制修饰符

访问权限控制的等级，从最大权限到最小权限依次为：

```
public > protected > 包访问权限（没有任何关键字）> private
```

- `public` - 表示任何类都可以访问；
- `包访问权限` - 包访问权限，没有任何关键字。它表示当前包中的所有其他类都可以访问，但是其它包的类无法访问。
- `protected` - 表示子类可以访问，此外，同一个包内的其他类也可以访问，即使这些类不是子类。
- `private` - 表示其它任何类都无法访问。

### 3.2. static

**被 `static` 修饰的方法被称为静态方法。**

静态方法相比于普通的实例方法，主要有以下区别：

- 在外部调用静态方法时，可以使用 `类名.方法名` 的方式，也可以使用 `对象名.方法名` 的方式。而实例方法只有后面这种方式。也就是说，**调用静态方法可以无需创建对象**。
- **静态方法在访问本类的成员时，只允许访问静态成员**（即静态成员变量和静态方法），而不允许访问实例成员变量和实例方法；实例方法则无此限制。

静态方法常被用于各种工具类、工厂方法类。

### 3.3. final

被 `final` 修饰的方法不能被子类覆写（Override）。

final 方法示例：

```
public class FinalMethodDemo {
    static class Father {
        protected final void print() {
            System.out.println("call Father print()");
        };
    }

    static class Son extends Father {
        @Override
        protected void print() {
            System.out.println("call print()");
        }
    }

    public static void main(String[] args) {
        Father demo = new Son();
        demo.print();
    }
}
// 编译时会报错
```

> 说明：
>
> 上面示例中，父类 Father 中定义了一个 `final` 方法 `print()`，则其子类不能 Override 这个 final 方法，否则会编译报错。

### 3.4. default

JDK8 开始，支持在接口 `Interface` 中定义 `default` 方法。**`default` 方法只能出现在接口 `Interface` 中**。

**接口中被 `default` 修饰的方法被称为默认方法，实现此接口的类如果没 Override 此方法，则直接继承这个方法，不再强制必须实现此方法。**

default 方法语法的出现，是为了既有的成千上万的 Java 类库的类增加新的功能， 且不必对这些类重新进行设计。 举例来说，JDK8 中 `Collection` 类中有一个非常方便的 `stream()` 方法，就是被修饰为 `default`，Collection 的一大堆 List、Set 子类就直接继承了这个方法 I，不必再为每个子类都注意添加这个方法。

`default` 方法示例：

```
public class DefaultMethodDemo {
    interface MyInterface {
        default void print() {
            System.out.println("Hello World");
        }
    }


    static class MyClass implements MyInterface {}

    public static void main(String[] args) {
        MyInterface obj = new MyClass();
        obj.print();
    }
}
// Output:
// Hello World
```

### 3.5. abstract

**被 `abstract` 修饰的方法被称为抽象方法，方法不能有实体。抽象方法只能出现抽象类中。**

抽象方法示例：

```
public class AbstractMethodDemo {
    static abstract class AbstractClass {
        abstract void print();
    }

    static class ConcreteClass extends AbstractClass {
        @Override
        void print() {
            System.out.println("call print()");
        }
    }

    public static void main(String[] args) {
        AbstractClass demo = new ConcreteClass();
        demo.print();
    }

}
// Outpu:
// call print()
```

### 3.6. synchronized

`synchronized` 用于并发编程。**被 `synchronized` 修饰的方法在一个时刻，只允许一个线程执行。**

在 Java 的同步容器（Vector、Stack、HashTable）中，你会见到大量的 synchronized 方法。不过，请记住：在 Java 并发编程中，synchronized 方法并不是一个好的选择，大多数情况下，我们会选择更加轻量级的锁 。

## 4. 特殊方法

Java 中，有一些较为特殊的方法，分别使用于特殊的场景。

### 4.1. main 方法

Java 中的 main 方法是一种特殊的静态方法，因为所有的 Java 程序都是由 `public static void main(String[] args)` 方法开始执行。

有很多新手虽然一直用 main 方法，却不知道 main 方法中的 args 有什么用。实际上，这是用来接收接收命令行输入参数的。

示例：

```
public class MainMethodDemo {
    public static void main(String[] args) {
        for (String arg : args) {
            System.out.println("arg = [" + arg + "]");
        }
    }
}
```

依次执行

```
javac MainMethodDemo.java
java MainMethodDemo A B C
```

控制台会打印输出参数：

```
arg = [A]
arg = [B]
arg = [C]
```

### 4.2. 构造方法

任何类都有构造方法，构造方法的作用就是在初始化类实例时，设置实例的状态。

每个类都有构造方法。如果没有显式地为类定义任何构造方法，Java 编译器将会为该类提供一个默认构造方法。

在创建一个对象的时候，至少要调用一个构造方法。构造方法的名称必须与类同名，一个类可以有多个构造方法。

```
public class ConstructorMethodDemo {

    static class Person {
        private String name;

        public Person(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static void main(String[] args) {
        Person person = new Person("jack");
        System.out.println("person name is " + person.getName());
    }
}
```

注意，构造方法除了使用 public，也可以使用 private 修饰，这种情况下，类无法调用此构造方法去实例化对象，这常常用于设计模式中的单例模式。

### 4.3. 变参方法

JDK5 开始，Java 支持传递同类型的可变参数给一个方法。在方法声明中，在指定参数类型后加一个省略号 `...`。一个方法中只能指定一个可变参数，它必须是方法的最后一个参数。任何普通的参数必须在它之前声明。

变参方法示例：

```
public class VarargsDemo {
    public static void method(String... params) {
        System.out.println("params.length = " + params.length);
        for (String param : params) {
            System.out.println("params = [" + param + "]");
        }
    }

    public static void main(String[] args) {
        method("red");
        method("red", "yellow");
        method("red", "yellow", "blue");
    }
}
// Output:
// params.length = 1
// params = [red]
// params.length = 2
// params = [red]
// params = [yellow]
// params.length = 3
// params = [red]
// params = [yellow]
// params = [blue]
```

### 4.4. finalize() 方法

`finalize` 在对象被垃圾收集器析构(回收)之前调用，用来清除回收对象。

`finalize` 是在 `java.lang.Object` 里定义的，也就是说每一个对象都有这么个方法。这个方法在 GC 启动，该对象被回收的时候被调用。

finalizer() 通常是不可预测的，也是很危险的，一般情况下是不必要的。使用终结方法会导致行为不稳定、降低性能，以及可移植性问题。

**请记住：应该尽量避免使用 `finalizer()`**。千万不要把它当成是 C/C++ 中的析构函数来用。原因是：**Finalizer 线程会和我们的主线程进行竞争，不过由于它的优先级较低，获取到的 CPU 时间较少，因此它永远也赶不上主线程的步伐。所以最后可能会发生 OutOfMemoryError 异常。**

> 扩展阅读：
>
> 下面两篇文章比较详细的讲述了 finalizer() 可能会造成的问题及原因。
>
> - [Java 的 Finalizer 引发的内存溢出](http://www.cnblogs.com/benwu/articles/5812903.html)
> - [重载 Finalize 引发的内存泄露](https://zhuanlan.zhihu.com/p/27850176)

## 5. 覆写和重载

**覆写（Override）是指子类定义了与父类中同名的方法，但是在方法覆写时必须考虑到访问权限，子类覆写的方法不能拥有比父类更加严格的访问权限。**

子类要覆写的方法如果要访问父类的方法，可以使用 `super` 关键字。

覆写示例：

```
public class MethodOverrideDemo {
    static class Animal {
        public void move() {
            System.out.println("会动");
        }
    }
    static class Dog extends Animal {
        @Override
        public void move() {
            super.move();
            System.out.println("会跑");
        }
    }

    public static void main(String[] args) {
        Animal dog = new Dog();
        dog.move();
    }
}
// Output:
// 会动
// 会跑
```

**方法的重载（Overload）是指方法名称相同，但参数的类型或参数的个数不同。通过传递参数的个数及类型的不同可以完成不同功能的方法调用。**

> 🔔 注意：
>
> 重载一定是方法的参数不完全相同。如果方法的参数完全相同，仅仅是返回值不同，Java 是无法编译通过的。

重载示例：

```
public class MethodOverloadDemo {
    public static void add(int x, int y) {
        System.out.println("x + y = " + (x + y));
    }

    public static void add(double x, double y) {
        System.out.println("x + y = " + (x + y));
    }

    public static void main(String[] args) {
        add(10, 20);
        add(1.0, 2.0);
    }
}
// Output:
// x + y = 30
// x + y = 3.0
```



## 面试题

### 一、说说Java到底是值传递还是引用传递

将参数传递给方法有两种常见的方式，一种是“值传递”，一种是“引用传递”。C 语言本身只支持值传递，它的衍生品 C++ 既支持值传递，也支持引用传递，而 Java 只支持值传递。

#### 值传递 VS 引用传递

首先，我们必须要搞清楚，到底什么是值传递，什么是引用传递，否则，讨论 Java 到底是值传递还是引用传递就显得毫无意义。

当一个参数按照值的方式在两个方法之间传递时，调用者和被调用者其实是用的两个不同的变量——被调用者中的变量（原始值）是调用者中变量的一份拷贝，对它们当中的任何一个变量修改都不会影响到另外一个变量。

而当一个参数按照引用传递的方式在两个方法之间传递时，调用者和被调用者其实用的是同一个变量，当该变量被修改时，双方都是可见的。

Java 程序员之所以容易搞混值传递和引用传递，主要是因为 Java 有两种数据类型，一种是基本类型，比如说 int，另外一种是引用类型，比如说 String。

基本类型的变量存储的都是实际的值，而引用类型的变量存储的是对象的引用——指向了对象在内存中的地址。值和引用存储在 stack（栈）中，而对象存储在 heap（堆）中。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002659.webp)

之所以有这个区别，是因为：

- 栈的优势是，存取速度比堆要快，仅次于直接位于 CPU 中的寄存器。但缺点是，栈中的数据大小与生存周期必须是确定的。
- 堆的优势是可以动态地分配内存大小，生存周期也不必事先告诉编译器，Java 的垃圾回收器会自动收走那些不再使用的数据。但由于要在运行时动态分配内存，存取速度较慢。

#### 基本类型的参数传递

众所周知，Java 有 8 种基本数据类型，分别是 int、long、byte、short、float、double 、char 和 boolean。它们的值直接存储在栈中，每当作为参数传递时，都会将原始值（实参）复制一份新的出来，给形参用。形参将会在被调用方法结束时从栈中清除。

来看下面这段代码：

```
public class PrimitiveTypeDemo {
    public static void main(String[] args) {
        int age = 18;
        modify(age);
        System.out.println(age);
    }

    private static void modify(int age1) {
        age1 = 30;
    }
}
```

1）main 方法中的 age 是基本类型，所以它的值 18 直接存储在栈中。

2）调用 `modify()` 方法的时候，将为实参 age 创建一个副本（形参 age1），它的值也为 18，不过是在栈中的其他位置。

3）对形参 age 的任何修改都只会影响它自身而不会影响实参。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002712.png)

#### 引用类型的参数传递

来看一段创建引用类型变量的代码：

```
Writer writer = new Writer(18, "沉默王二");
```

writer 是对象吗？还是对象的引用？为了搞清楚这个问题，我们可以把上面的代码拆分为两行代码：

```
Writer writer;
writer = new Writer(18, "沉默王二");
```

假如 writer 是对象的话，就不需要通过 new 关键字创建对象了，对吧？那也就是说，writer 并不是对象，在“=”操作符执行之前，它仅仅是一个变量。那谁是对象呢？`new Writer(18, "沉默王二")`，它是对象，存储于堆中；然后，“=”操作符将对象的引用赋值给了 writer 变量，于是 writer 此时应该叫对象引用，它存储在栈中，保存了对象在堆中的地址。

每当引用类型作为参数传递时，都会创建一个对象引用（实参）的副本（形参），该形参保存的地址和实参一样。

来看下面这段代码：

```
public class ReferenceTypeDemo {
    public static void main(String[] args) {
        Writer a = new Writer(18);
        Writer b = new Writer(18);
        modify(a, b);

        System.out.println(a.getAge());
        System.out.println(b.getAge());
    }

    private static void modify(Writer a1, Writer b1) {
        a1.setAge(30);

        b1 = new Writer(18);
        b1.setAge(30);
    }
}
```

1）在调用 `modify()` 方法之前，实参 a 和 b 指向的对象是不一样的，尽管 age 都为 18。

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002725.webp)

2）在调用 `modify()` 方法时，实参 a 和 b 都在栈中创建了一个新的副本，分别是 a1 和 b1，但指向的对象是一致的（a 和 a1 指向对象 a，b 和 b1 指向对象 b）。

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002732.png)

3）在 `modify()` 方法中，修改了形参 a1 的 age 为 30，意味着对象 a 的 age 从 18 变成了 30，而实参 a 指向的也是对象 a，所以 a 的 age 也变成了 30；形参 b1 指向了一个新的对象，随后 b1 的 age 被修改为 30。

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002742.png)

修改 a1 的 age，意味着同时修改了 a 的 age，因为它们指向的对象是一个；修改 b1 的 age，对 b 却没有影响，因为它们指向的对象是两个。

程序输出的结果如下所示：

```
30
18
```

果然和我们的分析是吻合的。







