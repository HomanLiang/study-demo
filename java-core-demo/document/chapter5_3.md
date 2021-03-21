[toc]



# Java 序列化

## Java 序列化简介

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f313535333232343132393438342e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321195349.png)

- **序列化（serialize）** - 序列化是将对象转换为字节流。
- **反序列化（deserialize）** - 反序列化是将字节流转换为对象。
- 序列化用途
  - 序列化可以将对象的字节序列持久化——保存在内存、文件、数据库中。
  - 在网络上传送对象的字节序列。
  - RMI(远程方法调用)

> 🔔 注意：使用 Java 对象序列化，在保存对象时，会把其状态保存为一组字节，在未来，再将这些字节组装成对象。必须注意地是，对象序列化保存的是对象的”状态”，即它的成员变量。由此可知，**对象序列化不会关注类中的静态变量**。

## 2. Java 序列化和反序列化

Java 通过对象输入输出流来实现序列化和反序列化：

- `java.io.ObjectOutputStream` 类的 `writeObject()` 方法可以实现序列化；
- `java.io.ObjectInputStream` 类的 `readObject()` 方法用于实现反序列化。

序列化和反序列化示例：

```
public class SerializeDemo01 {
    enum Sex {
        MALE,
        FEMALE
    }


    static class Person implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name = null;
        private Integer age = null;
        private Sex sex;

        public Person() { }

        public Person(String name, Integer age, Sex sex) {
            this.name = name;
            this.age = age;
            this.sex = sex;
        }

        @Override
        public String toString() {
            return "Person{" + "name='" + name + '\'' + ", age=" + age + ", sex=" + sex + '}';
        }
    }

    /**
     * 序列化
     */
    private static void serialize(String filename) throws IOException {
        File f = new File(filename); // 定义保存路径
        OutputStream out = new FileOutputStream(f); // 文件输出流
        ObjectOutputStream oos = new ObjectOutputStream(out); // 对象输出流
        oos.writeObject(new Person("Jack", 30, Sex.MALE)); // 保存对象
        oos.close();
        out.close();
    }

    /**
     * 反序列化
     */
    private static void deserialize(String filename) throws IOException, ClassNotFoundException {
        File f = new File(filename); // 定义保存路径
        InputStream in = new FileInputStream(f); // 文件输入流
        ObjectInputStream ois = new ObjectInputStream(in); // 对象输入流
        Object obj = ois.readObject(); // 读取对象
        ois.close();
        in.close();
        System.out.println(obj);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final String filename = "d:/text.dat";
        serialize(filename);
        deserialize(filename);
    }
}
// Output:
// Person{name='Jack', age=30, sex=MALE}
```

## 3. Serializable 接口

**被序列化的类必须属于 Enum、Array 和 Serializable 类型其中的任何一种，否则将抛出 `NotSerializableException` 异常**。这是因为：在序列化操作过程中会对类型进行检查，如果不满足序列化类型要求，就会抛出异常。

【示例】`NotSerializableException` 错误

```
public class UnSerializeDemo {
    static class Person { // 其他内容略 }
    // 其他内容略
}
```

输出：结果就是出现如下异常信息。

```
Exception in thread "main" java.io.NotSerializableException:
...
```

### 3.1. serialVersionUID

请注意 `serialVersionUID` 字段，你可以在 Java 世界的无数类中看到这个字段。

`serialVersionUID` 有什么作用，如何使用 `serialVersionUID`？

**`serialVersionUID` 是 Java 为每个序列化类产生的版本标识**。它可以用来保证在反序列时，发送方发送的和接受方接收的是可兼容的对象。如果接收方接收的类的 `serialVersionUID` 与发送方发送的 `serialVersionUID` 不一致，会抛出 `InvalidClassException`。

如果可序列化类没有显式声明 `serialVersionUID`，则序列化运行时将基于该类的各个方面计算该类的默认 `serialVersionUID` 值。尽管这样，还是**建议在每一个序列化的类中显式指定 `serialVersionUID` 的值**。因为不同的 jdk 编译很可能会生成不同的 `serialVersionUID` 默认值，从而导致在反序列化时抛出 `InvalidClassExceptions` 异常。

**`serialVersionUID` 字段必须是 `static final long` 类型**。

我们来举个例子：

（1）有一个可序列化类 Person

```
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Integer age;
    private String address;
    // 构造方法、get、set 方法略
}
```

（2）开发过程中，对 Person 做了修改，增加了一个字段 email，如下：

```
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Integer age;
    private String address;
    private String email;
    // 构造方法、get、set 方法略
}
```

由于这个类和老版本不兼容，我们需要修改版本号：

```
private static final long serialVersionUID = 2L;
```

再次进行反序列化，则会抛出 `InvalidClassException` 异常。

综上所述，我们大概可以清楚：**`serialVersionUID` 用于控制序列化版本是否兼容**。若我们认为修改的可序列化类是向后兼容的，则不修改 `serialVersionUID`。

### 3.2. 默认序列化机制

如果仅仅只是让某个类实现 `Serializable` 接口，而没有其它任何处理的话，那么就会使用默认序列化机制。

使用默认机制，在序列化对象时，不仅会序列化当前对象本身，还会对其父类的字段以及该对象引用的其它对象也进行序列化。同样地，这些其它对象引用的另外对象也将被序列化，以此类推。所以，如果一个对象包含的成员变量是容器类对象，而这些容器所含有的元素也是容器类对象，那么这个序列化的过程就会较复杂，开销也较大。

> 🔔 注意：这里的父类和引用对象既然要进行序列化，那么它们当然也要满足序列化要求：**被序列化的类必须属于 Enum、Array 和 Serializable 类型其中的任何一种**。

### 3.3. transient

在现实应用中，有些时候不能使用默认序列化机制。比如，希望在序列化过程中忽略掉敏感数据，或者简化序列化过程。下面将介绍若干影响序列化的方法。

**当某个字段被声明为 `transient` 后，默认序列化机制就会忽略该字段的内容,该字段的内容在序列化后无法获得访问**。

我们将 SerializeDemo01 示例中的内部类 Person 的 age 字段声明为 `transient`，如下所示：

```
public class SerializeDemo02 {
    static class Person implements Serializable {
        transient private Integer age = null;
        // 其他内容略
    }
    // 其他内容略
}
// Output:
// name: Jack, age: null, sex: MALE
```

从输出结果可以看出，age 字段没有被序列化。

## 4. Externalizable 接口

无论是使用 `transient` 关键字，还是使用 `writeObject()` 和 `readObject()` 方法，其实都是基于 `Serializable` 接口的序列化。

JDK 中提供了另一个序列化接口--`Externalizable`。

**可序列化类实现 `Externalizable` 接口之后，基于 `Serializable` 接口的默认序列化机制就会失效**。

我们来基于 SerializeDemo02 再次做一些改动，代码如下：

```
public class ExternalizeDemo01 {
    static class Person implements Externalizable {
        transient private Integer age = null;
        // 其他内容略

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(age);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            age = in.readInt();
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException { }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException { }
    }
     // 其他内容略
}
// Output:
// call Person()
// name: null, age: null, sex: null
```

从该结果，一方面可以看出 Person 对象中任何一个字段都没有被序列化。另一方面，如果细心的话，还可以发现这此次序列化过程调用了 Person 类的无参构造方法。

- **`Externalizable` 继承于 `Serializable`，它增添了两个方法：`writeExternal()` 与 `readExternal()`。这两个方法在序列化和反序列化过程中会被自动调用，以便执行一些特殊操作**。当使用该接口时，序列化的细节需要由程序员去完成。如上所示的代码，由于 `writeExternal()`与 `readExternal()` 方法未作任何处理，那么该序列化行为将不会保存/读取任何一个字段。这也就是为什么输出结果中所有字段的值均为空。
- 另外，**若使用 `Externalizable` 进行序列化，当读取对象时，会调用被序列化类的无参构造方法去创建一个新的对象；然后再将被保存对象的字段的值分别填充到新对象中**。这就是为什么在此次序列化过程中 Person 类的无参构造方法会被调用。由于这个原因，实现 `Externalizable` 接口的类必须要提供一个无参的构造方法，且它的访问权限为 `public`。

对上述 Person 类作进一步的修改，使其能够对 name 与 age 字段进行序列化，但要忽略掉 gender 字段，如下代码所示：

```
public class ExternalizeDemo02 {
    static class Person implements Externalizable {
        transient private Integer age = null;
        // 其他内容略

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(age);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            age = in.readInt();
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject(name);
            out.writeInt(age);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            name = (String) in.readObject();
            age = in.readInt();
        }
    }
     // 其他内容略
}
// Output:
// call Person()
// name: Jack, age: 30, sex: null
```

### 4.1. Externalizable 接口的替代方法

实现 `Externalizable` 接口可以控制序列化和反序列化的细节。它有一个替代方法：实现 `Serializable` 接口，并添加 `writeObject(ObjectOutputStream out)` 与 `readObject(ObjectInputStream in)` 方法。序列化和反序列化过程中会自动回调这两个方法。

示例如下所示：

```
public class SerializeDemo03 {
    static class Person implements Serializable {
        transient private Integer age = null;
        // 其他内容略

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(age);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            age = in.readInt();
        }
        // 其他内容略
    }
    // 其他内容略
}
// Output:
// name: Jack, age: 30, sex: MALE
```

在 `writeObject()` 方法中会先调用 `ObjectOutputStream` 中的 `defaultWriteObject()` 方法，该方法会执行默认的序列化机制，如上节所述，此时会忽略掉 age 字段。然后再调用 writeInt() 方法显示地将 age 字段写入到 `ObjectOutputStream` 中。readObject() 的作用则是针对对象的读取，其原理与 writeObject() 方法相同。

> 🔔 注意：`writeObject()` 与 `readObject()` 都是 `private` 方法，那么它们是如何被调用的呢？毫无疑问，是使用反射。详情可见 `ObjectOutputStream` 中的 `writeSerialData` 方法，以及 `ObjectInputStream` 中的 `readSerialData` 方法。

### 4.2. readResolve() 方法

当我们使用 Singleton 模式时，应该是期望某个类的实例应该是唯一的，但如果该类是可序列化的，那么情况可能会略有不同。此时对第 2 节使用的 Person 类进行修改，使其实现 Singleton 模式，如下所示：

```
public class SerializeDemo04 {

    enum Sex {
        MALE, FEMALE
    }

    static class Person implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name = null;
        transient private Integer age = null;
        private Sex sex;
        static final Person instatnce = new Person("Tom", 31, Sex.MALE);

        private Person() {
            System.out.println("call Person()");
        }

        private Person(String name, Integer age, Sex sex) {
            this.name = name;
            this.age = age;
            this.sex = sex;
        }

        public static Person getInstance() {
            return instatnce;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            out.writeInt(age);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            age = in.readInt();
        }

        public String toString() {
            return "name: " + this.name + ", age: " + this.age + ", sex: " + this.sex;
        }
    }

    /**
     * 序列化
     */
    private static void serialize(String filename) throws IOException {
        File f = new File(filename); // 定义保存路径
        OutputStream out = new FileOutputStream(f); // 文件输出流
        ObjectOutputStream oos = new ObjectOutputStream(out); // 对象输出流
        oos.writeObject(new Person("Jack", 30, Sex.MALE)); // 保存对象
        oos.close();
        out.close();
    }

    /**
     * 反序列化
     */
    private static void deserialize(String filename) throws IOException, ClassNotFoundException {
        File f = new File(filename); // 定义保存路径
        InputStream in = new FileInputStream(f); // 文件输入流
        ObjectInputStream ois = new ObjectInputStream(in); // 对象输入流
        Object obj = ois.readObject(); // 读取对象
        ois.close();
        in.close();
        System.out.println(obj);
        System.out.println(obj == Person.getInstance());
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final String filename = "d:/text.dat";
        serialize(filename);
        deserialize(filename);
    }
}
// Output:
// name: Jack, age: null, sex: MALE
// false
```

值得注意的是，从文件中获取的 Person 对象与 Person 类中的单例对象并不相等。**为了能在单例类中仍然保持序列的特性，可以使用 `readResolve()` 方法**。在该方法中直接返回 Person 的单例对象。我们在 SerializeDemo04 示例的基础上添加一个 `readResolve` 方法， 如下所示：

```
public class SerializeDemo05 {
    // 其他内容略

    static class Person implements Serializable {

        // private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        //     in.defaultReadObject();
        //     age = in.readInt();
        // }

        // 添加此方法
        private Object readResolve() {
            return instatnce;
        }
        // 其他内容略
    }

    // 其他内容略
}
// Output:
// name: Tom, age: 31, sex: MALE
// true
```

## 5. Java 序列化问题

Java 的序列化能保证对象状态的持久保存，但是遇到一些对象结构复杂的情况还是难以处理，这里归纳一下：

- 父类是 `Serializable`，所有子类都可以被序列化。
- 子类是 `Serializable` ，父类不是，则子类可以正确序列化，但父类的属性不会被序列化（不报错，数据丢失）。
- 如果序列化的属性是对象，则这个对象也必须是 `Serializable` ，否则报错。
- 反序列化时，如果对象的属性有修改或删减，则修改的部分属性会丢失，但不会报错。
- 反序列化时，如果 `serialVersionUID` 被修改，则反序列化会失败。

## 6. Java 序列化的缺陷

- **无法跨语言**：Java 序列化目前只适用基于 Java 语言实现的框架，其它语言大部分都没有使用 Java 的序列化框架，也没有实现 Java 序列化这套协议。因此，如果是两个基于不同语言编写的应用程序相互通信，则无法实现两个应用服务之间传输对象的序列化与反序列化。
- **容易被攻击**：对象是通过在 `ObjectInputStream` 上调用 `readObject()` 方法进行反序列化的，它可以将类路径上几乎所有实现了 `Serializable` 接口的对象都实例化。这意味着，在反序列化字节流的过程中，该方法可以执行任意类型的代码，这是非常危险的。对于需要长时间进行反序列化的对象，不需要执行任何代码，也可以发起一次攻击。攻击者可以创建循环对象链，然后将序列化后的对象传输到程序中反序列化，这种情况会导致 `hashCode` 方法被调用次数呈次方爆发式增长, 从而引发栈溢出异常。例如下面这个案例就可以很好地说明。
- **序列化后的流太大**：Java 序列化中使用了 `ObjectOutputStream` 来实现对象转二进制编码，编码后的数组很大，非常影响存储和传输效率。
- **序列化性能太差**：Java 的序列化耗时比较大。序列化的速度也是体现序列化性能的重要指标，如果序列化的速度慢，就会影响网络通信的效率，从而增加系统的响应时间。
- 序列化编程限制：
  - Java 官方的序列化一定**需要实现 `Serializable` 接口**。
  - Java 官方的序列化**需要关注 `serialVersionUID`**。

## 7. 序列化技术选型

通过上一章节——Java 序列化的缺陷，我们了解到，Java 序列化方式存在许多缺陷。因此，建议使用第三方序列化工具来替代。

当然我们还有更加优秀的一些序列化和反序列化的工具，根据不同的使用场景可以自行选择！

- [thrift](https://github.com/apache/thrift)、[protobuf](https://github.com/protocolbuffers/protobuf) - 适用于**对性能敏感，对开发体验要求不高**。
- [hessian](http://hessian.caucho.com/doc/hessian-overview.xtp) - 适用于**对开发体验敏感，性能有要求**。
- [jackson](https://github.com/FasterXML/jackson)、[gson](https://github.com/google/gson)、[fastjson](https://github.com/alibaba/fastjson) - 适用于对序列化后的数据要求有**良好的可读性**（转为 json 、xml 形式）。



## 8.序列化底层

### Serializable底层

Serializable接口，只是一个空的接口，没有方法或字段，为什么这么神奇，实现了它就可以让对象序列化了？

```
public interface Serializable {
}
```

为了验证Serializable的作用，把以上demo的Student对象，去掉实现Serializable接口，看序列化过程怎样吧~

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31382f313731386362626230316130323939393f773d36303726683d35343226663d706e6726733d3436353136](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321201901.png)

序列化过程中抛出异常啦，堆栈信息如下：

```
Exception in thread "main" java.io.NotSerializableException: com.example.demo.Student
	at java.io.ObjectOutputStream.writeObject0(ObjectOutputStream.java:1184)
	at java.io.ObjectOutputStream.writeObject(ObjectOutputStream.java:348)
	at com.example.demo.Test.main(Test.java:13)
```

顺着堆栈信息看一下，原来有重大发现，如下~

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31382f313731386339383063393034633265653f773d38303226683d36333626663d706e6726733d3639353036](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321201938.png)

**原来底层是这样：**ObjectOutputStream 在序列化的时候，会判断被序列化的Object是哪一种类型，String？array？enum？还是 Serializable，如果都不是的话，抛出 NotSerializableException异常。所以呀，**Serializable真的只是一个标志，一个序列化标志**~

### writeObject（Object）

序列化的方法就是writeObject，基于以上的demo，我们来分析一波它的核心方法调用链吧~（建议大家也去debug看一下这个方法，感兴趣的话）

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31382f313731386431666261643237386638663f773d36363826683d3130303326663d706e6726733d3630393338](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321202000.png)

writeObject直接调用的就是writeObject0（）方法，

```
public final void writeObject(Object obj) throws IOException {
    ......
    writeObject0(obj, false);
    ......
}
```

writeObject0 主要实现是对象的不同类型，调用不同的方法写入序列化数据，这里面如果对象实现了Serializable接口，就调用writeOrdinaryObject()方法~

```
private void writeObject0(Object obj, boolean unshared)
        throws IOException
    {
    ......
   //String类型
    if (obj instanceof String) {
        writeString((String) obj, unshared);
   //数组类型
    } else if (cl.isArray()) {
        writeArray(obj, desc, unshared);
   //枚举类型
    } else if (obj instanceof Enum) {
        writeEnum((Enum<?>) obj, desc, unshared);
   //Serializable实现序列化接口
    } else if (obj instanceof Serializable) {
        writeOrdinaryObject(obj, desc, unshared);
    } else{
        //其他情况会抛异常~
        if (extendedDebugInfo) {
            throw new NotSerializableException(
                cl.getName() + "\n" + debugInfoStack.toString());
        } else {
            throw new NotSerializableException(cl.getName());
        }
    }
    ......
```

writeOrdinaryObject()会先调用writeClassDesc(desc)，写入该类的生成信息，然后调用writeSerialData方法,写入序列化数据

```
    private void writeOrdinaryObject(Object obj,
                                     ObjectStreamClass desc,
                                     boolean unshared)
        throws IOException
    {
            ......
            //调用ObjectStreamClass的写入方法
            writeClassDesc(desc, false);
            // 判断是否实现了Externalizable接口
            if (desc.isExternalizable() && !desc.isProxy()) {
                writeExternalData((Externalizable) obj);
            } else {
                //写入序列化数据
                writeSerialData(obj, desc);
            }
            .....
    }
```

writeSerialData（）实现的就是写入被序列化对象的字段数据

```
  private void writeSerialData(Object obj, ObjectStreamClass desc)
        throws IOException
    {
        for (int i = 0; i < slots.length; i++) {
            if (slotDesc.hasWriteObjectMethod()) {
                   //如果被序列化的对象自定义实现了writeObject()方法，则执行这个代码块
                    slotDesc.invokeWriteObject(obj, this);
            } else {
                // 调用默认的方法写入实例数据
                defaultWriteFields(obj, slotDesc);
            }
        }
    }
```

defaultWriteFields（）方法，获取类的基本数据类型数据，直接写入底层字节容器；获取类的obj类型数据，循环递归调用writeObject0()方法，写入数据~

```
   private void defaultWriteFields(Object obj, ObjectStreamClass desc)
        throws IOException
    {   
        // 获取类的基本数据类型数据，保存到primVals字节数组
        desc.getPrimFieldValues(obj, primVals);
        //primVals的基本类型数据写到底层字节容器
        bout.write(primVals, 0, primDataSize, false);

        // 获取对应类的所有字段对象
        ObjectStreamField[] fields = desc.getFields(false);
        Object[] objVals = new Object[desc.getNumObjFields()];
        int numPrimFields = fields.length - objVals.length;
        // 获取类的obj类型数据，保存到objVals字节数组
        desc.getObjFieldValues(obj, objVals);
        //对所有Object类型的字段,循环
        for (int i = 0; i < objVals.length; i++) {
            ......
              //递归调用writeObject0()方法，写入对应的数据
            writeObject0(objVals[i],
                             fields[numPrimFields + i].isUnshared());
            ......
        }
    }
```



## 9.日常开发序列化的一些注意点

- static静态变量和transient 修饰的字段是不会被序列化的
- serialVersionUID问题
- 如果某个序列化类的成员变量是对象类型，则该对象类型的类必须实现序列化
- 子类实现了序列化，父类没有实现序列化，父类中的字段丢失问题

### static静态变量和transient 修饰的字段是不会被序列化的

static静态变量和transient 修饰的字段是不会被序列化的,我们来看例子分析一波~ Student类加了一个类变量gender和一个transient修饰的字段specialty

```
public class Student implements Serializable {

    private Integer age;
    private String name;

    public static String gender = "男";
    transient  String specialty = "计算机专业";

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    @Override
    public String toString() {
        return "Student{" +"age=" + age + ", name='" + name + '\'' + ", gender='" + gender + '\'' + ", specialty='" + specialty + '\'' +
                '}';
    }
    ......
```

打印学生对象，序列化到文件，接着修改静态变量的值，再反序列化，输出反序列化后的对象~ 

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31392f313731386530626364366364636266653f773d3132343026683d35323726663d706e6726733d3833323036](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321202035.png)

运行结果：

```
序列化前Student{age=25, name='jayWei', gender='男', specialty='计算机专业'}
序列化后Student{age=25, name='jayWei', gender='女', specialty='null'}
```

对比结果可以发现：

- 1）序列化前的静态变量性别明明是‘男’，序列化后再在程序中修改，反序列化后却变成‘女’了，**what**？显然这个静态属性并没有进行序列化。其实，**静态（static）成员变量是属于类级别的，而序列化是针对对象的~所以不能序列化哦**。
- 2）经过序列化和反序列化过程后，specialty字段变量值由'计算机专业'变为空了，为什么呢？其实是因为transient关键字，**它可以阻止修饰的字段被序列化到文件中**，在被反序列化后，transient 字段的值被设为初始值，比如int型的值会被设置为 0，对象型初始值会被设置为null。

### serialVersionUID问题

serialVersionUID 表面意思就是**序列化版本号ID**，其实每一个实现Serializable接口的类，都有一个表示序列化版本标识符的静态变量，或者默认等于1L，或者等于对象的哈希码。

```
private static final long serialVersionUID = -6384871967268653799L;
```

**serialVersionUID有什么用？**

JAVA序列化的机制是通过判断类的serialVersionUID来验证版本是否一致的。在进行反序列化时，JVM会把传来的字节流中的serialVersionUID和本地相应实体类的serialVersionUID进行比较，如果相同，反序列化成功，如果不相同，就抛出InvalidClassException异常。

接下来，我们来验证一下吧，修改一下Student类，再反序列化操作

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31392f313731386638656234633031323734663f773d35393326683d33313526663d706e6726733d3239303032](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321202131.png)

```
Exception in thread "main" java.io.InvalidClassException: com.example.demo.Student;
local class incompatible: stream classdesc serialVersionUID = 3096644667492403394,
local class serialVersionUID = 4429793331949928814
	at java.io.ObjectStreamClass.initNonProxy(ObjectStreamClass.java:687)
	at java.io.ObjectInputStream.readNonProxyDesc(ObjectInputStream.java:1876)
	at java.io.ObjectInputStream.readClassDesc(ObjectInputStream.java:1745)
	at java.io.ObjectInputStream.readOrdinaryObject(ObjectInputStream.java:2033)
	at java.io.ObjectInputStream.readObject0(ObjectInputStream.java:1567)
	at java.io.ObjectInputStream.readObject(ObjectInputStream.java:427)
	at com.example.demo.Test.main(Test.java:20)
```

从日志堆栈异常信息可以看到，文件流中的class和当前类路径中的class不同了，它们的serialVersionUID不相同，所以反序列化抛出InvalidClassException异常。那么，如果确实需要修改Student类，又想反序列化成功，怎么办呢？可以手动指定serialVersionUID的值，一般可以设置为1L或者，或者让我们的编辑器IDE生成

```
private static final long serialVersionUID = -6564022808907262054L;
```

实际上，阿里开发手册，强制要求序列化类新增属性时，不能修改serialVersionUID字段~ 

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31392f313731386637386162303961313763643f773d39323526683d31343026663d706e6726733d3339383237](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321202154.png)

### 如果某个序列化类的成员变量是对象类型，则该对象类型的类必须实现序列化

给Student类添加一个Teacher类型的成员变量，其中Teacher是没有实现序列化接口的

```
public class Student implements Serializable {
    
    private Integer age;
    private String name;
    private Teacher teacher;
    ...
}
//Teacher 没有实现
public class Teacher  {
......
}
```

序列化运行，就报NotSerializableException异常啦

```
Exception in thread "main" java.io.NotSerializableException: com.example.demo.Teacher
	at java.io.ObjectOutputStream.writeObject0(ObjectOutputStream.java:1184)
	at java.io.ObjectOutputStream.defaultWriteFields(ObjectOutputStream.java:1548)
	at java.io.ObjectOutputStream.writeSerialData(ObjectOutputStream.java:1509)
	at java.io.ObjectOutputStream.writeOrdinaryObject(ObjectOutputStream.java:1432)
	at java.io.ObjectOutputStream.writeObject0(ObjectOutputStream.java:1178)
	at java.io.ObjectOutputStream.writeObject(ObjectOutputStream.java:348)
	at com.example.demo.Test.main(Test.java:16)
```

其实这个可以在上小节的底层源码分析找到答案，一个对象序列化过程，会循环调用它的Object类型字段，递归调用序列化的，也就是说，序列化Student类的时候，会对Teacher类进行序列化，但是对Teacher没有实现序列化接口，因此抛出NotSerializableException异常。所以如果某个实例化类的成员变量是对象类型，则该对象类型的类必须实现序列化 

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323032302f342f31392f313731386661393238613666663137383f773d38353226683d34313526663d706e6726733d3532313535](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321202210.png)

### 子类实现了Serializable，父类没有实现Serializable接口的话，父类不会被序列化。

子类Student实现了Serializable接口，父类User没有实现Serializable接口

```
//父类实现了Serializable接口
public class Student  extends User implements Serializable {

    private Integer age;
    private String name;
}
//父类没有实现Serializable接口
public class User {
    String userId;
}

Student student = new Student();
student.setAge(25);
student.setName("jayWei");
student.setUserId("1");

ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("D:\\text.out"));
objectOutputStream.writeObject(student);

objectOutputStream.flush();
objectOutputStream.close();

//反序列化结果
ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("D:\\text.out"));
Student student1 = (Student) objectInputStream.readObject();
System.out.println(student1.getUserId());
//output
/** 
 * null
 */
```

从反序列化结果，可以发现，父类属性值丢失了。因此子类实现了Serializable接口，父类没有实现Serializable接口的话，父类不会被序列化。



## 面试题

### 1.序列化的底层是怎么实现的？

本文第六小节可以回答这个问题，如回答Serializable关键字作用，序列化标志啦，源码中，它的作用啦~~还有，可以回答writeObject几个核心方法，如直接写入基本类型，获取obj类型数据，循环递归写入，哈哈~~

### 2.序列化时，如何让某些成员不要序列化？

可以用transient关键字修饰，它可以阻止修饰的字段被序列化到文件中，在被反序列化后，transient 字段的值被设为初始值，比如int型的值会被设置为 0，对象型初始值会被设置为null。

### 3.在 Java 中,Serializable 和 Externalizable 有什么区别

Externalizable继承了Serializable，给我们提供 writeExternal() 和 readExternal() 方法, 让我们可以控制 Java的序列化机制, 不依赖于Java的默认序列化。正确实现 Externalizable 接口可以显著提高应用程序的性能。

### 4.serialVersionUID有什么用？

JAVA序列化的机制是通过判断类的serialVersionUID来验证版本是否一致的。在进行反序列化时，JVM会把传来的字节流中的serialVersionUID和本地相应实体类的serialVersionUID进行比较，如果相同，反序列化成功，如果不相同，就抛出InvalidClassException异常。

### 5.是否可以自定义序列化过程, 或者是否可以覆盖 Java 中的默认序列化过程？

可以的。我们都知道,对于序列化一个对象需调用 ObjectOutputStream.writeObject(saveThisObject), 并用 ObjectInputStream.readObject() 读取对象, 但 Java 虚拟机为你提供的还有一件事, 是定义这两个方法。如果在类中定义这两种方法, 则 JVM 将调用这两种方法, 而不是应用默认序列化机制。同时，可以声明这些方法为私有方法，以避免被继承、重写或重载。

### 6.在 Java 序列化期间,哪些变量未序列化？

static静态变量和transient 修饰的字段是不会被序列化的。静态（static）成员变量是属于类级别的，而序列化是针对对象的。transient关键字修字段饰，可以阻止该字段被序列化到文件中。











