[toc]



# Java 异常

## 1. 异常框架

### 1.1. Throwable

**`Throwable` 是 Java 语言中所有错误（`Error`）和异常（`Exception`）的超类。**在 `Java` 中只有 `Throwable` 类型的实例才可以被抛出（`throw`）或者捕获（`catch`），它是异常处理机制的基本组成类型。

`Throwable` 包含了其线程创建时线程执行堆栈的快照，它提供了 `printStackTrace()` 等接口用于获取堆栈跟踪数据等信息。

主要方法：

- `fillInStackTrace` - 用当前的调用栈层次填充 `Throwable` 对象栈层次，添加到栈层次任何先前信息中。
- `getMessage` - 返回关于发生的异常的详细信息。这个消息在 `Throwable` 类的构造函数中初始化了。
- `getCause` - 返回一个 `Throwable` 对象代表异常原因。
- `getStackTrace` - 返回一个包含堆栈层次的数组。下标为 0 的元素代表栈顶，最后一个元素代表方法调用堆栈的栈底。
- `printStackTrace` - 打印 `toString()` 结果和栈层次到 `System.err`，即错误输出流。
- `toString` - 使用 `getMessage` 的结果返回代表 `Throwable` 对象的字符串。

### 1.2. Error

`Error` 是 `Throwable` 的一个子类。**`Error` 表示正常情况下，不大可能出现的严重问题**。**编译器不会检查 `Error`**。绝大部分的 `Error` 都会导致程序（比如 `JVM` 自身）处于非正常的、不可恢复状态。既然是非正常情况，所以不便于也不需要捕获，常见的比如 `OutOfMemoryError` 之类，都是 `Error` 的子类。

常见 `Error`：

- `AssertionError` - 断言错误。
- `VirtualMachineError` - 虚拟机错误。
- `UnsupportedClassVersionError` - Java 类版本错误。
- `StackOverflowError` - 栈溢出错误。
- `OutOfMemoryError` - 内存溢出错误。

### 1.3. Exception

`Exception` 是 `Throwable` 的一个子类。**`Exception` 表示合理的应用程序可能想要捕获的条件。** `Exception` 是程序正常运行中，可以预料的意外情况，可能并且应该被捕获，进行相应处理。

`Exception` 又分为可检查（`checked`）异常和不检查（`unchecked`）异常，可检查异常在源代码里必须显式地进行捕获处理，这是编译期检查的一部分。

**编译器会检查 `Exception` 异常。**此类异常，要么通过 `throws` 进行声明抛出，要么通过 `try catch` 进行捕获处理，否则不能通过编译。

常见 `Exception`：

- `ClassNotFoundException` - 应用程序试图加载类时，找不到相应的类，抛出该异常。
- `CloneNotSupportedException` - 当调用 `Object` 类中的 `clone` 方法克隆对象，但该对象的类无法实现 `Cloneable` 接口时，抛出该异常。
- `IllegalAccessException` - 拒绝访问一个类的时候，抛出该异常。
- `InstantiationException` - 当试图使用 `Class` 类中的 `newInstance` 方法创建一个类的实例，而指定的类对象因为是一个接口或是一个抽象类而无法实例化时，抛出该异常。
- `InterruptedException` - 一个线程被另一个线程中断，抛出该异常。
- `NoSuchFieldException` - 请求的变量不存在。
- `NoSuchMethodException` - 请求的方法不存在。

示例：

```
public class ExceptionDemo {
    public static void main(String[] args) {
        Method method = String.class.getMethod("toString", int.class);
    }
};
```

试图编译运行时会报错：

```
Error:(7, 47) java: 未报告的异常错误java.lang.NoSuchMethodException; 必须对其进行捕获或声明以便抛出
```

### 1.4. RuntimeException

`RuntimeException` 是 `Exception` 的一个子类。`RuntimeException` 是那些可能在 `Java` 虚拟机正常运行期间抛出的异常的超类。

**编译器不会检查 `RuntimeException` 异常。**当程序中可能出现这类异常时，倘若既没有通过 `throws` 声明抛出它，也没有用 `try catch` 语句捕获它，程序还是会编译通过。

示例：

```
public class RuntimeExceptionDemo {
    public static void main(String[] args) {
        // 此处产生了异常
        int result = 10 / 0;
        System.out.println("两个数字相除的结果：" + result);
        System.out.println("----------------------------");
    }
};
```

运行时输出：

```
Exception in thread "main" java.lang.ArithmeticException: / by zero
	at io.github.dunwu.javacore.exception.RumtimeExceptionDemo01.main(RumtimeExceptionDemo01.java:6)
```

常见 `RuntimeException`：

- `ArrayIndexOutOfBoundsException` - 用非法索引访问数组时抛出的异常。如果索引为负或大于等于数组大小，则该索引为非法索引。
- `ArrayStoreException` - 试图将错误类型的对象存储到一个对象数组时抛出的异常。
- `ClassCastException` - 当试图将对象强制转换为不是实例的子类时，抛出该异常。
- `IllegalArgumentException` - 抛出的异常表明向方法传递了一个不合法或不正确的参数。
- `IllegalMonitorStateException` - 抛出的异常表明某一线程已经试图等待对象的监视器，或者试图通知其他正在等待对象的监视器而本身没有指定监视器的线程。
- `IllegalStateException` - 在非法或不适当的时间调用方法时产生的信号。换句话说，即 `Java` 环境或 `Java` 应用程序没有处于请求操作所要求的适当状态下。
- `IllegalThreadStateException` - 线程没有处于请求操作所要求的适当状态时抛出的异常。
- `IndexOutOfBoundsException` - 指示某排序索引（例如对数组、字符串或向量的排序）超出范围时抛出。
- `NegativeArraySizeException` - 如果应用程序试图创建大小为负的数组，则抛出该异常。
- `NullPointerException` - 当应用程序试图在需要对象的地方使用 `null` 时，抛出该异常
- `NumberFormatException` - 当应用程序试图将字符串转换成一种数值类型，但该字符串不能转换为适当格式时，抛出该异常。
- `SecurityException` - 由安全管理器抛出的异常，指示存在安全侵犯。
- `StringIndexOutOfBoundsException` - 此异常由 `String` 方法抛出，指示索引或者为负，或者超出字符串的大小。
- `UnsupportedOperationException` - 当不支持请求的操作时，抛出该异常。

## 2. 自定义异常

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f313535333735323739353031302e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210320165157.png)

**自定义一个异常类，只需要继承 `Exception` 或 `RuntimeException` 即可。**

示例：

```
public class MyExceptionDemo {
    public static void main(String[] args) {
        throw new MyException("自定义异常");
    }

    static class MyException extends RuntimeException {
        public MyException(String message) {
            super(message);
        }
    }
}
```

输出：

```
Exception in thread "main" io.github.dunwu.javacore.exception.MyExceptionDemo$MyException: 自定义异常
	at io.github.dunwu.javacore.exception.MyExceptionDemo.main(MyExceptionDemo.java:9)
```

## 3. 抛出异常

如果想在程序中明确地抛出异常，需要用到 `throw` 和 `throws` 。

如果一个方法没有捕获一个检查性异常，那么该方法必须使用 `throws` 关键字来声明。`throws` 关键字放在方法签名的尾部。

`throw` 示例：

```
public class ThrowDemo {
    public static void f() {
        try {
            throw new RuntimeException("抛出一个异常");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        f();
    }
};
```

输出：

```
java.lang.RuntimeException: 抛出一个异常
```

也可以使用 `throw` 关键字抛出一个异常，无论它是新实例化的还是刚捕获到的。

`throws` 示例：

```
public class ThrowsDemo {
    public static void f1() throws NoSuchMethodException, NoSuchFieldException {
        Field field = Integer.class.getDeclaredField("digits");
        if (field != null) {
            System.out.println("反射获取 digits 方法成功");
        }
        Method method = String.class.getMethod("toString", int.class);
        if (method != null) {
            System.out.println("反射获取 toString 方法成功");
        }
    }

    public static void f2() {
        try {
            // 调用 f1 处，如果不用 try catch ，编译时会报错
            f1();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        f2();
    }
};
```

输出：

```
反射获取 digits 方法成功
java.lang.NoSuchMethodException: java.lang.String.toString(int)
	at java.lang.Class.getMethod(Class.java:1786)
	at io.github.dunwu.javacore.exception.ThrowsDemo.f1(ThrowsDemo.java:12)
	at io.github.dunwu.javacore.exception.ThrowsDemo.f2(ThrowsDemo.java:21)
	at io.github.dunwu.javacore.exception.ThrowsDemo.main(ThrowsDemo.java:30)
```

`throw` 和 `throws` 的区别：

- `throws` 使用在函数上，`throw` 使用在函数内。
- `throws` 后面跟异常类，可以跟多个，用逗号区别；`throw` 后面跟的是异常对象。

## 4. 捕获异常

**使用 try 和 catch 关键字可以捕获异常**。`try catch` 代码块放在异常可能发生的地方。

它的语法形式如下：

```
try {
    // 可能会发生异常的代码块
} catch (Exception e1) {
    // 捕获并处理try抛出的异常类型Exception
} catch (Exception2 e2) {
    // 捕获并处理try抛出的异常类型Exception2
} finally {
    // 无论是否发生异常，都将执行的代码块
}
```

此外，`JDK7` 以后，`catch` 多种异常时，也可以像下面这样简化代码：

```
try {
    // 可能会发生异常的代码块
} catch (Exception | Exception2 e) {
    // 捕获并处理try抛出的异常类型
} finally {
    // 无论是否发生异常，都将执行的代码块
}
```

- `try` - **`try` 语句用于监听。将要被监听的代码(可能抛出异常的代码)放在 `try` 语句块之内，当 `try` 语句块内发生异常时，异常就被抛出。**
- `catch` - `catch` 语句包含要捕获异常类型的声明。当保护代码块中发生一个异常时，`try` 后面的 `catch` 块就会被检查。
- `finally` - **`finally` 语句块总是会被执行，无论是否出现异常。**`try catch` 语句后不一定非要`finally` 语句。`finally` 常用于这样的场景：由于`finally` 语句块总是会被执行，所以那些在 `try` 代码块中打开的，并且必须回收的物理资源(如数据库连接、网络连接和文件)，一般会放在`finally` 语句块中释放资源。
- `try`、`catch`、`finally` 三个代码块中的局部变量不可共享使用。
- `catch` 块尝试捕获异常时，是按照 `catch` 块的声明顺序从上往下寻找的，一旦匹配，就不会再向下执行。因此，如果同一个 `try` 块下的多个 `catch` 异常类型有父子关系，应该将子类异常放在前面，父类异常放在后面。

示例：

```
public class TryCatchFinallyDemo {
    public static void main(String[] args) {
        try {
            // 此处产生了异常
            int temp = 10 / 0;
            System.out.println("两个数字相除的结果：" + temp);
            System.out.println("----------------------------");
        } catch (ArithmeticException e) {
            System.out.println("出现异常了：" + e);
        } finally {
            System.out.println("不管是否出现异常，都执行此代码");
        }
    }
};
```

运行时输出：

```
出现异常了：java.lang.ArithmeticException: / by zero
不管是否出现异常，都执行此代码
```

## 5. 异常链

异常链是以一个异常对象为参数构造新的异常对象，新的异常对象将包含先前异常的信息。

通过使用异常链，我们可以提高代码的可理解性、系统的可维护性和友好性。

我们有两种方式处理异常，一是 `throws` 抛出交给上级处理，二是 `try…catch` 做具体处理。`try…catch` 的 `catch` 块我们可以不需要做任何处理，仅仅只用 `throw` 这个关键字将我们封装异常信息主动抛出来。然后在通过关键字 `throws` 继续抛出该方法异常。它的上层也可以做这样的处理，以此类推就会产生一条由异常构成的异常链。

【示例】

```
public class ExceptionChainDemo {
    static class MyException1 extends Exception {
        public MyException1(String message) {
            super(message);
        }
    }

    static class MyException2 extends Exception {
        public MyException2(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void f1() throws MyException1 {
        throw new MyException1("出现 MyException1");
    }

    public static void f2() throws MyException2 {
        try {
            f1();
        } catch (MyException1 e) {
            throw new MyException2("出现 MyException2", e);
        }
    }

    public static void main(String[] args) throws MyException2 {
        f2();
    }
}
```

输出：

```
Exception in thread "main" io.github.dunwu.javacore.exception.ExceptionChainDemo$MyException2: 出现 MyException2
	at io.github.dunwu.javacore.exception.ExceptionChainDemo.f2(ExceptionChainDemo.java:29)
	at io.github.dunwu.javacore.exception.ExceptionChainDemo.main(ExceptionChainDemo.java:34)
Caused by: io.github.dunwu.javacore.exception.ExceptionChainDemo$MyException1: 出现 MyException1
	at io.github.dunwu.javacore.exception.ExceptionChainDemo.f1(ExceptionChainDemo.java:22)
	at io.github.dunwu.javacore.exception.ExceptionChainDemo.f2(ExceptionChainDemo.java:27)
	... 1 more
```

> 扩展阅读：https://juejin.im/post/5b6d61e55188251b38129f9a#heading-10
>
> 这篇文章中对于异常链讲解比较详细。

## 6. 异常注意事项

### 6.1. finally 覆盖异常

`Java` 异常处理中 `finally` 中的 `return` 会覆盖 `catch` 代码块中的 `return` 语句和 `throw` 语句，所以 `Java` **不建议在 `finally` 中使用 `return`语句**。

此外 `finally` 中的 `throw` 语句也会覆盖 `catch` 代码块中的 `return` 语句和 `throw` 语句。

示例：

```
public class FinallyOverrideExceptionDemo {
    static void f() throws Exception {
        try {
            throw new Exception("A");
        } catch (Exception e) {
            throw new Exception("B");
        } finally {
            throw new Exception("C");
        }
    }

    public static void main(String[] args) {
        try {
            f();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
```

输出：C

### 6.2. 覆盖抛出异常的方法

当子类重写父类带有 `throws` 声明的函数时，其 `throws` 声明的异常必须在父类异常的可控范围内——用于处理父类的 `throws` 方法的异常处理器，必须也适用于子类的这个带 `throws` 方法 。这是为了支持多态。

示例：

```
public class ExceptionOverrideDemo {
    static class Father {
        public void start() throws IOException {
            throw new IOException();
        }
    }

    static class Son extends Father {
        @Override
        public void start() throws SQLException {
            throw new SQLException();
        }
    }

    public static void main(String[] args) {
        Father obj1 = new Father();
        Father obj2 = new Son();
        try {
            obj1.start();
            obj2.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

> 上面的示例编译时会报错，原因在于：
>
> 因为 `Son` 类抛出异常的实质是 `SQLException`，而 `IOException` 无法处理它。那么这里的 `try catch` 就不能处理 `Son` 中的异常了。多态就不能实现了。

### 6.3. 异常和线程

如果 `Java` 程序只有一个线程，那么没有被任何代码处理的异常会导致程序终止。如果 `Java` 程序是多线程的，那么没有被任何代码处理的异常仅仅会导致异常所在的线程结束。

## 7. 最佳实践

- 对可恢复的情况使用检查性异常（`Exception`），对编程错误使用运行时异常（`RuntimeException`）。
- 优先使用 `Java` 标准的异常。
- 抛出与抽象相对应的异常。
- 在细节消息中包含能捕获失败的信息。
- 尽可能减少 `try` 代码块的大小。
- 尽量缩小异常范围。例如，如果明知尝试捕获的是一个 `ArithmeticException`，就应该 `catch` `ArithmeticException`，而不是 `catch` 范围较大的 `RuntimeException`，甚至是 `Exception`。
- 尽量不要在 `finally` 块抛出异常或者返回值。
- 不要忽略异常，一旦捕获异常，就应该处理，而非丢弃。
- 异常处理效率很低，所以不要用异常进行业务逻辑处理。
- 各类异常必须要有单独的日志记录，将异常分级，分类管理，因为有的时候仅仅想给第三方运维看到逻辑异常，而不是更细节的信息。
- 如何对异常进行分类：
  - 逻辑异常，这类异常用于描述业务无法按照预期的情况处理下去，属于用户制造的意外。
  - 代码错误，这类异常用于描述开发的代码错误，例如 `NPE`，`ILLARG`，都属于程序员制造的 `BUG`。
  - 专有异常，多用于特定业务场景，用于描述指定作业出现意外情况无法预先处理。

> 扩展阅读：
>
> - [Effective java 中文版 之 第九章 异常](https://book.douban.com/subject/3360807/)
> - [优雅的处理你的 Java 异常](https://my.oschina.net/c5ms/blog/1827907)



## X.面试题

### X.1.ClassNotFoundException 和 NoClassDefFoundError 有什么区别

在写 `Java` 程序的时候，当一个类找不到的时候，`JVM` 有时候会抛出 `ClassNotFoundException` 异常，而有时候又会抛出 `NoClassDefFoundError` 。看两个异常的字面意思，好像都是类找不到，但是 `JVM` 为什么要用两个异常去区分类找不到的情况呢？这个两个异常有什么不同的地方呢？

#### X.1.1.ClassNotFoundException

`ClassNotFoundException` 是一个运行时异常。从类继承层次上来看，`ClassNotFoundException` 是从 `Exception` 继承的，所以 `ClassNotFoundException` 是一个检查异常。

![640](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002035.webp)

当应用程序运行的过程中尝试使用类加载器去加载 `Class` 文件的时候，如果没有在 `classpath` 中查找到指定的类，就会抛出`ClassNotFoundException`。一般情况下，当我们使用 `Class.forName()` 或者 `ClassLoader.loadClass` 以及使用 `ClassLoader.findSystemClass()` 在运行时加载类的时候，如果类没有被找到，那么就会导致JVM抛出 `ClassNotFoundException`。

最简单的，当我们使用 `JDBC` 去连接数据库的时候，我们一般会使用 `Class.forName()` 的方式去加载JDBC的驱动，如果我们没有将驱动放到应用的 `classpath` 下，那么会导致运行时找不到类，所以运行 `Class.forName()` 会抛出 `ClassNotFoundException`。

```
public class MainClass {
    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
```

输出：

```
java.lang.ClassNotFoundException: oracle.jdbc.driver.OracleDriver
    at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
    at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:331)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
    at java.lang.Class.forName0(Native Method)
    at java.lang.Class.forName(Class.java:264)
    at MainClass.main(MainClass.java:7)
```

#### X.1.2.NoClassDefFoundError

`NoClassDefFoundError` 异常，看命名后缀是一个 `Error`。从类继承层次上看，`NoClassDefFoundError` 是从 `Error` 继承的。和`ClassNotFoundException` 相比，明显的一个区别是，`NoClassDefFoundError` 并不需要应用程序去关心 `catch` 的问题。

![640 (1)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002048.webp)

当JVM在加载一个类的时候，如果这个类在编译时是可用的，但是在运行时找不到这个类的定义的时候，`JVM` 就会抛出一个`NoClassDefFoundError` 错误。比如当我们在 `new` 一个类的实例的时候，如果在运行是类找不到，则会抛出一个`NoClassDefFoundError` 的错误。

```
public class TempClass {
}

public class MainClass {
    public static void main(String[] args) {
        TempClass t = new TempClass();
    }
}
```

首先这里我们先创建一个 `TempClass`，然后编译以后，将 `TempClass` 生产的 `TempClass.class` 文件删除，然后执行程序，输出：

```
Exception in thread "main" java.lang.NoClassDefFoundError: TempClass
    at MainClass.main(MainClass.java:6)
Caused by: java.lang.ClassNotFoundException: TempClass
    at java.net.URLClassLoader.findClass(URLClassLoader.java:381)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
    at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:331)
    at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
    ... 1 more
```

#### X.1.3.总结

![640 (2)](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321002059.webp)



### X.2.try catch finally中放入return

大概考察了几种情况：

一下 i 初始值都是 0
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005326.png)

1. **catch中有return，finally中的代码会执行吗？**
   
   ![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005335.png)
   

结果很明显会执行：

   ![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005348.png)

2. **catch中有return，finally也有return，怎么执行？**

   ![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005357.png)

   结果显示 `finally` 中的 `return` 会比 `catch` 中 `return` 先执行

   ![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005406.png)

3. **catch中return变量，finally对变量做计算，返回结果是啥？**

   ![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005413.png)

   按照往常的逻辑，输入 `0` ，经过 `++i`，返回 `1` 没毛病呀！ 

   ![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005420.png)

   `i` 赋初值为 `0`，捕获到异常后进入 `catch`，`catch` 里有 `return`，但是后边还有 `finally`，先执行 `finally` 里的，对 `i+1=1`，然后 `return`。那 `return` 的是 `1`？事实上不是 `1`，而是 `0.`为何？

   ![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005437.png)

   可以清楚的看到 `i=1`；但是返回的确是 `0` ？那说明此 `i` 非彼 `i`，就是返回的i不是这个 `i`。

   因为这里是值传递，在执行 `return` 前，保留了一个 `i` 的副本，值为 `0`，然后再去执行 `finally`，`finally` 完后，到 `return` 的时候，返回的并不是当前的 `i`，而是保留的那个副本，也就是 `0`.所以返回结果是 `0`.

4. **catch、finally同时return变量，返回结果又该是啥？**

   ![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005444.png)

   如果 `finally` 中有 `return` 不会走 `catch`

   ![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321005453.png)

总结：

- 在 `catch` 中有 `return` 的情况下, `finally` 中的内容还是会执行，并且是先执行 `finally` 再 `return`。

- 需要注意的是，如果返回的是一个基本数据类型，则 `finally` 中的内容对返回的值没有影响。因为返回的是 `finally` 执行之前生成的一个副本。

- 当 `catch` 和 `finally` 都有 `return` 时，`return` 的是 `finally` 的值。