[toc]



# JVM 字节码

>Java 之所以可以“一次编译，到处运行”，一是因为 JVM 针对各种操作系统、平台都进行了定制，二是因为无论在什么平台，都可以编译生成固定格式的字节码（.class 文件）供 JVM 使用。

> **.class 文件是一组以 8 位字节为基础单位的二进制流**，各个数据项严格按照顺序紧凑地排列在 .class 文件中，中间没有添加任何分隔符。**整个 .class 文件本质上就是一张表**。



## 1. 字节码

### 1.1. 什么是字节码

之所以被称之为字节码，是因为字节码文件由十六进制值组成，而 JVM 以两个十六进制值为一组，即以字节为单位进行读取。在 Java 中一般是用 `javac` 命令编译源代码为字节码文件，一个.java 文件从编译到运行的示例如下图所示。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646634343933633832323f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121053.webp)

对于开发人员，了解字节码可以更准确、直观地理解 Java 语言中更深层次的东西，比如通过字节码，可以很直观地看到 Volatile 关键字如何在字节码上生效。另外，字节码增强技术在 Spring AOP、各种 ORM 框架、热部署中的应用屡见不鲜，深入理解其原理对于我们来说大有裨益。除此之外，由于 JVM 规范的存在，只要最终可以生成符合规范的字节码就可以在 JVM 上运行，因此这就给了各种运行在 JVM 上的语言（如 Scala、Groovy、Kotlin）一种契机，可以扩展 Java 所没有的特性或者实现各种语法糖。理解字节码后再学习这些语言，可以“逆流而上”，从字节码视角看它的设计思路，学习起来也“易如反掌”。

### 1.2. 字节码结构

.java 文件通过 javac 编译后将得到一个.class 文件，比如编写一个简单的 ByteCodeDemo 类，如下图 2 的左侧部分：

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646634346339663830333f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121105.webp)

编译后生成 ByteCodeDemo.class 文件，打开后是一堆十六进制数，按字节为单位进行分割后展示如图 2 右侧部分所示。上文提及过，JVM 对于字节码是有规范要求的，那么看似杂乱的十六进制符合什么结构呢？JVM 规范要求每一个字节码文件都要由十部分按照固定的顺序组成，整体结构如图 3 所示。接下来我们将一一介绍这十部分：

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646634353035633332313f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121117.webp)

**（1）魔数（Magic Number）**

每个 `.class` 文件的头 4 个字节称为 **`魔数（magic number）`**，它的唯一作用是确定这个文件是否为一个能被虚拟机接收的 `.class` 文件。魔数的固定值为：`0xCAFEBABE`。

> 有趣的是，魔数的固定值是 Java 之父 James Gosling 制定的，为 CafeBabe（咖啡宝贝），而 Java 的图标为一杯咖啡。

**（2）版本号（Version）**

版本号为魔数之后的 4 个字节，**前两个字节表示次版本号（Minor Version），后两个字节表示主版本号（Major Version）**。

举例来说，如果版本号为：“00 00 00 34”。那么，次版本号转化为十进制为 0，主版本号转化为十进制为 52，在 Oracle 官网中查询序号 52 对应的主版本号为 1.8，所以编译该文件的 Java 版本号为 1.8.0。

**（3）常量池（Constant Pool）**

紧接着主版本号之后的字节为常量池入口。

常量池主要存放两类常量：

- **字面量** - 如文本字符串、声明为 `final` 的常量值。
- 符号引用
  - 类和接口的全限定名
  - 字段的名称和描述符
  - 方法的名称和描述符

常量池整体上分为两部分：常量池计数器以及常量池数据区，如下图 4 所示。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646634363062346239643f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121125.webp)

- **常量池计数器（constant_pool_count）** - 由于常量的数量不固定，所以需要先放置两个字节来表示常量池容量计数值。图 2 中示例代码的字节码前 10 个字节如下图 5 所示，将十六进制的 24 转化为十进制值为 36，排除掉下标“0”，也就是说，这个类文件中共有 35 个常量。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646634346635366262323f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121133.webp)

- **常量池数据区** - 数据区是由（`constant_pool_count-1`）个 cp_info 结构组成，一个 cp_info 结构对应一个常量。在字节码中共有 14 种类型的 cp_info（如下图 6 所示），每种类型的结构都是固定的。

![68747470733a2f2f75706c6f61642d696d616765732e6a69616e7368752e696f2f75706c6f61645f696d616765732f313938363836382d383331393933623264633139646439302e706e673f696d6167654d6f6772322f6175746f2d6f7269656e742f7374726970](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121149.webp)

具体以 CONSTANT_utf8_info 为例，它的结构如下图 7 左侧所示。首先一个字节“tag”，它的值取自上图 6 中对应项的 Tag，由于它的类型是 utf8_info，所以值为“01”。接下来两个字节标识该字符串的长度 Length，然后 Length 个字节为这个字符串具体的值。从图 2 中的字节码摘取一个 cp_info 结构，如下图 7 右侧所示。将它翻译过来后，其含义为：该常量类型为 utf8 字符串，长度为一字节，数据为“a”。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646637333162346665653f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121212.webp)

其他类型的 cp_info 结构在本文不再赘述，整体结构大同小异，都是先通过 Tag 来标识类型，然后后续 n 个字节来描述长度和（或）数据。先知其所以然，以后可以通过 javap -verbose ByteCodeDemo 命令，查看 JVM 反编译后的完整常量池，如下图 8 所示。可以看到反编译结果将每一个 cp_info 结构的类型和值都很明确地呈现了出来。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646637333263626237383f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121225.webp)

**（4）访问标志**

紧接着的 2 个字节代表访问标志，这个标志**用于识别一些类或者接口的访问信息**，描述该 Class 是类还是接口，以及是否被 `public`、`abstract`、`final` 等修饰符修饰。

JVM 规范规定了如下图 9 的访问标志（Access_Flag）。需要注意的是，JVM 并没有穷举所有的访问标志，而是使用按位或操作来进行描述的，比如某个类的修饰符为 Public Final，则对应的访问修饰符的值为 ACC_PUBLIC | ACC_FINAL，即 0x0001 | 0x0010=0x0011。

![687474703a2f2f64756e77752e746573742e757063646e2e6e65742f736e61702f313536313437333232383831362e706e67](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121251.png)

**（5）当前类名**

访问标志后的 2 个字节，描述的是当前类的全限定名。这两个字节保存的值为常量池中的索引值，根据索引值就能在常量池中找到这个类的全限定名。

**（6）父类名称**

当前类名后的 2 个字节，描述父类的全限定名，同上，保存的也是常量池中的索引值。

**（7）接口信息**

父类名称后为 2 字节的接口计数器，描述了该类或父类实现的接口数量。紧接着的 n 个字节是所有接口名称的字符串常量的索引值。

**（8）字段表**

字段表用于描述类和接口中声明的变量，包含类级别的变量以及实例变量，但是不包含方法内部声明的局部变量。字段表也分为两部分，第一部分为两个字节，描述字段个数；第二部分是每个字段的详细信息 fields_info。字段表结构如下图所示：

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646637333337383738383f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121302.webp)

以图 2 中字节码的字段表为例，如下图 11 所示。其中字段的访问标志查图 9，0002 对应为 Private。通过索引下标在图 8 中常量池分别得到字段名为“a”，描述符为“I”（代表 int）。综上，就可以唯一确定出一个类中声明的变量 private int a。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646637333463623738323f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121311.webp)

**（9）方法表**

字段表结束后为方法表，方法表也是由两部分组成，第一部分为两个字节描述方法的个数；第二部分为每个方法的详细信息。方法的详细信息较为复杂，包括方法的访问标志、方法名、方法的描述符以及方法的属性，如下图所示：

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646637333333333538653f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121320.webp)

方法的权限修饰符依然可以通过图 9 的值查询得到，方法名和方法的描述符都是常量池中的索引值，可以通过索引值在常量池中找到。而“方法的属性”这一部分较为复杂，直接借助 javap -verbose 将其反编译为人可以读懂的信息进行解读，如图 13 所示。可以看到属性中包括以下三个部分：

- “Code 区”：源代码对应的 JVM 指令操作码，在进行字节码增强时重点操作的就是“Code 区”这一部分。
- “LineNumberTable”：行号表，将 Code 区的操作码和源代码中的行号对应，Debug 时会起到作用（源代码走一行，需要走多少个 JVM 指令操作码）。
- “LocalVariableTable”：本地变量表，包含 This 和局部变量，之所以可以在每一个方法内部都可以调用 This，是因为 JVM 将 This 作为每一个方法的第一个参数隐式进行传入。当然，这是针对非 Static 方法而言。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646639643366343432663f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121331.webp)

**（10）附加属性表**

字节码的最后一部分，该项存放了在该文件中类或接口所定义属性的基本信息。

### 1.3. 字节码操作集合

在上图 13 中，Code 区的红色编号 0 ～ 17，就是.java 中的方法源代码编译后让 JVM 真正执行的操作码。为了帮助人们理解，反编译后看到的是十六进制操作码所对应的助记符，十六进制值操作码与助记符的对应关系，以及每一个操作码的用处可以查看 Oracle 官方文档进行了解，在需要用到时进行查阅即可。比如上图中第一个助记符为 iconst_2，对应到图 2 中的字节码为 0x05，用处是将 int 值 2 压入操作数栈中。以此类推，对 0~17 的助记符理解后，就是完整的 add()方法的实现。

### 1.4. 操作数栈和字节码

JVM 的指令集是基于栈而不是寄存器，基于栈可以具备很好的跨平台性（因为寄存器指令集往往和硬件挂钩），但缺点在于，要完成同样的操作，基于栈的实现需要更多指令才能完成（因为栈只是一个 FILO 结构，需要频繁压栈出栈）。另外，由于栈是在内存实现的，而寄存器是在 CPU 的高速缓存区，相较而言，基于栈的速度要慢很多，这也是为了跨平台性而做出的牺牲。

我们在上文所说的操作码或者操作集合，其实控制的就是这个 JVM 的操作数栈。为了更直观地感受操作码是如何控制操作数栈的，以及理解常量池、变量表的作用，将 add()方法的对操作数栈的操作制作为 GIF，如下图 14 所示，图中仅截取了常量池中被引用的部分，以指令 iconst_2 开始到 ireturn 结束，与图 13 中 Code 区 0~17 的指令一一对应：

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646639663065653834363f696d616765736c696d](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121340.gif)

### 1.5. 字节码工具

如果每次查看反编译后的字节码都使用 javap 命令的话，好非常繁琐。这里推荐一个 Idea 插件：[jclasslib](https://plugins.jetbrains.com/plugin/9248-jclasslib-bytecode-viewer)。使用效果如图 15 所示，代码编译后在菜单栏"View"中选择"Show Bytecode With jclasslib"，可以很直观地看到当前字节码文件的类信息、常量池、方法区等信息。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646661303862363930343f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121400.webp)

## 2. 字节码增强

### 2.1. Asm

对于需要手动操纵字节码的需求，可以使用 Asm，它可以直接生产 `.class`字节码文件，也可以在类被加载入 JVM 之前动态修改类行为（如下图 17 所示）。

Asm 的应用场景有 AOP（Cglib 就是基于 Asm）、热部署、修改其他 jar 包中的类等。当然，涉及到如此底层的步骤，实现起来也比较麻烦。

![68747470733a2f2f757365722d676f6c642d63646e2e786974752e696f2f323031392f392f31302f313664313962646661643733376664373f696d61676556696577322f302f772f313238302f682f3936302f666f726d61742f776562702f69676e6f72652d6572](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210327121410.webp)

Asm 有两类 API：核心 API 和树形 API

#### 核心 API

Asm Core API 可以类比解析 XML 文件中的 SAX 方式，不需要把这个类的整个结构读取进来，就可以用流式的方法来处理字节码文件。好处是非常节约内存，但是编程难度较大。然而出于性能考虑，一般情况下编程都使用 Core API。在 Core API 中有以下几个关键类：

- ClassReader：用于读取已经编译好的.class 文件。
- ClassWriter：用于重新构建编译后的类，如修改类名、属性以及方法，也可以生成新的类的字节码文件。
- 各种 Visitor 类：如上所述，CoreAPI 根据字节码从上到下依次处理，对于字节码文件中不同的区域有不同的 Visitor，比如用于访问方法的 MethodVisitor、用于访问类变量的 FieldVisitor、用于访问注解的 AnnotationVisitor 等。为了实现 AOP，重点要使用的是 MethodVisitor。

#### 树形 API

Asm Tree API 可以类比解析 XML 文件中的 DOM 方式，把整个类的结构读取到内存中，缺点是消耗内存多，但是编程比较简单。TreeApi 不同于 CoreAPI，TreeAPI 通过各种 Node 类来映射字节码的各个区域，类比 DOM 节点，就可以很好地理解这种编程方式。

### 2.2. Javassist

利用 Javassist 实现字节码增强时，可以无须关注字节码刻板的结构，其优点就在于编程简单。直接使用 java 编码的形式，而不需要了解虚拟机指令，就能动态改变类的结构或者动态生成类。

其中最重要的是 ClassPool、CtClass、CtMethod、CtField 这四个类：

- `CtClass（compile-time class）` - 编译时类信息，它是一个 class 文件在代码中的抽象表现形式，可以通过一个类的全限定名来获取一个 CtClass 对象，用来表示这个类文件。
- `ClassPool` - 从开发视角来看，ClassPool 是一张保存 CtClass 信息的 HashTable，key 为类名，value 为类名对应的 CtClass 对象。当我们需要对某个类进行修改时，就是通过 pool.getCtClass("className")方法从 pool 中获取到相应的 CtClass。
- `CtMethod`、`CtField` - 这两个比较好理解，对应的是类中的方法和属性。



## X.面试题

### X.1.讲一讲 `a = a ++; ` 和 `a = ++a; `

> 面试官：我看你简历上写的熟悉JVM，我给你下面一个题目，先来讲一讲a = a ++; 和a = ++a; 的运行结果各是多少？

```
public class Test1 {
    public static void main(String[] args) {
        int a = 88;
        a = a++;
//        a = ++a;
        System.out.println(a);
    }
}
```

我们先来看 `i=i++`等于8，具体他内部是怎样执行的呢，我们需要看它的指令是怎么操作的
我们可以用过 `Jclasslib`来解析他二进制码之后点到的main方法

#### X.1.1.安装 Jclasslib

首先我们需要安装 Jclasslib，安装成功如下图所示：

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230330.png)

#### X.1.2.查看字节码

首先我们需要 **运行main方法** ，加载其class的内容后，点击 `view -> show Bytecode With Jclasslib`

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230343.png)

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230348.png)
main方法里面记录的有两张表：

表1：LineNumberTable 记录是行号

表2：LocalVariabletable 是局部变量表，里面就是方法内部使用到的变量，第一个是 args ，第二个是a，所以局部变量表，指的就是我们当前这个方法，这个栈帧里面用到了哪些局部变量。

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230405.png)

##### X.1.2.1.`a = a++;`

接下来我们来看一下，a = a++;中间的执行过程具体是怎么样的

```java
 0 bipush 88
 2 istore_1
 3 iload_1
 4 iinc 1 by 1
 7 istore_1
 8 getstatic #2 <java/lang/System.out>
11 iload_1
12 invokevirtual #3 <java/io/PrintStream.println>
15 return
```

如果我们不理解指令具体是什么意思，我们可以点击对应指令，浏览器直接定位这条指令的详细说明

首先我们来看一下 `bipush 88 和 istore_1`，对应的是 int a = 88；iload+1 等于89，再把89赋值出来还是89，

- **bipush 88** 是指 push byte 放到栈中，88当成一个byte值，会自动扩展成Int类型，把它放到栈中，88放在局部变量表，输入结果是88。
- 第二条指令`istore_1`是把我们栈顶上的那个数出栈，放到下标值为1的局部变量表。局部变量表下标值为1的就是a的值，刚才88是放到栈顶上的，现在把88弹出来放到a里面，所以这两句话完成之后对应的int a = 88就完成了，如下图所示

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230434.png)

- **iload_1：** 的意思是 从局部变量加载int(load int from local variable) ，就是从局部变量表中 拿值，之后放到栈里面，如下图所示：

  ![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230439.png)

- **iinc 1 by 1：** 执行 a++ 操作，将局部变量表中 数值为88的进行+1 操作，所以就是 89了，

  ![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230455.png)

- **istore_1：** 执行 a = a++ 操作，原先已经执行了 a++ 操作，这个时候将 a++ 中 a 赋值给 int a ，所以会将栈中的数据赋值到 局部变量表中，所以这个时候局部变量表中的数据就是88了

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230515.png)
所以我们最后的结果就是88

##### X.1.2.2.`a = ++a;`

字节码指令：

```java
 0 bipush 88
 2 istore_1
 3 iinc 1 by 1
 6 iload_1
 7 istore_1
 8 getstatic #2 <java/lang/System.out>
11 iload_1
12 invokevirtual #3 <java/io/PrintStream.println>
15 return
```

**bipush 88和istore_1：** 这句话其实完成了 int a = 88，先将88压栈，然后在出栈赋值到局部变量表中

![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230537.png)
**iinc 1 by 1：** 进行++a 操作，所以这个时候局部变量表中的数据就变成了89
![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230548.png)

**iload_1：** 这个时候将局部变量表中的数值压到栈中，
![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230553.png)

**istore_1：** 这个时候做 a = ++a 操作，将 a的值赋值给 int a，因为在栈中的数据本身就是89，所以最后打印出来的结果就是89
![在这里插入图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230557.png)

**补充：**
当我们设置 int a = 250 的时候，下面的值会变成 sipush，是因为 250已经超过127，他已经超过byte 所能代表的最大结果，所以看到的二进制就是sipush，s 代表 short

![0 sipush 250](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429230600.png)

### X.2.从JVM底层原理分析数值交换那些事

#### X.2.1.基础数据类型交换

这个话题，需要从最最基础的一道题目说起，看题目：以下代码a和b的值会交换么：

```
    public static void main(String[] args) {
        int a = 1, b = 2;
        swapInt(a, b);
        System.out.println("a=" + a + " , b=" + b);
    }
    private static void swapInt(int a, int b) {
        int temp = a;
        a = b;
        b = temp;
    }    
```

结果估计大家都知道，a和b并没有交换：

```
integerA=1 , integerB=2
```

但是原因呢？先看这张图，先来说说Java虚拟机的结构：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231536.png)

运行时区域主要分为：

- 线程私有：
  - 程序计数器:`Program Count Register`,线程私有，没有垃圾回收
  - 虚拟机栈:`VM Stack`，线程私有，没有垃圾回收
  - 本地方法栈:`Native Method Stack`,线程私有，没有垃圾回收
- 线程共享：
  - 方法区:`Method Area`，以`HotSpot`为例，`JDK1.8`后元空间取代方法区，有垃圾回收。
  - 堆:`Heap`，垃圾回收最重要的地方。

和这个代码相关的主要是虚拟机栈，也叫方法栈，是每一个线程私有的。

生命周期和线程一样，主要是记录该线程Java方法执行的内存模型。虚拟机栈里面放着好多**栈帧**。**注意虚拟机栈，对应是Java方法，不包括本地方法。**

**一个Java方法执行会创建一个栈帧**，一个栈帧主要存储：

- 局部变量表

- 操作数栈

- 动态链接

- 方法出口
  
  每一个方法调用的时候，就相当于将一个**栈帧**放到虚拟机栈中（入栈），方法执行完成的时候，就是对应着将该栈帧从虚拟机栈中弹出（出栈）。

每一个线程有一个自己的虚拟机栈，这样就不会混起来，如果不是线程独立的话，会造成调用混乱。

大家平时说的java内存分为堆和栈，其实就是为了简便的不太严谨的说法，他们说的栈一般是指虚拟机栈，或者虚拟机栈里面的局部变量表。

局部变量表一般存放着以下数据：

- 基本数据类型（`boolean`,`byte`,`char`,`short`,`int`,`float`,`long`,`double`）
- 对象引用（reference类型，不一定是对象本身，可能是一个对象起始地址的引用指针，或者一个代表对象的句柄，或者与对象相关的位置）
- returAddress（指向了一条字节码指令的地址）

局部变量表内存大小编译期间确定，运行期间不会变化。空间衡量我们叫Slot（局部变量空间）。64位的long和double会占用2个Slot，其他的数据类型占用1个Slot。

上面的方法调用的时候，实际上栈帧是这样的，调用main()函数的时候，会往虚拟机栈里面放一个栈帧，栈帧里面我们主要关注局部变量表，传入的参数也会当成局部变量，所以第一个局部变量就是参数`args`，由于这个是`static`方法，也就是类方法，所以不会有当前对象的指针。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231559.png)

如果是普通方法，那么局部变量表里面会多出一个局部变量`this`。

如何证明这个东西真的存在呢？我们大概看看字节码，因为局部变量在编译的时候就确定了，运行期不会变化的。下面是`IDEA`插件`jclasslib`查看的:

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231631.png)

上面的图，我们在`main()`方法的局部变量表中，确实看到了三个变量：`args`,`a`，`b`。

**那在main()方法里面调用了swapInt(a, b)呢？**

那堆栈里面就会放入`swapInt(a,b)`的栈帧，**相当于把a和b局部变量复制了一份**，变成下面这样，由于里面一共有三个局部变量：

- a:参数
- b：参数
- temp：函数内临时变量

a和b交换之后，其实`swapInt(a,b)`的栈帧变了，a变为2，b变为1，但是`main()`栈帧的a和b并没有变。
![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231648.png)

那同样来从字节码看，会发现确实有3个局部变量在局部变量表内，并且他们的数值都是int类型。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231659.png)

而`swap(a,b)`执行结束之后，该方法的堆栈会被弹出虚拟机栈，此时虚拟机栈又剩下`main()`方法的栈帧，由于基础数据类型的数值相当于存在局部变量中，`swap(a,b)`栈帧中的局部变量不会影响`main()`方法的栈帧中的局部变量，所以，就算你在`swap(a,b)`中交换了，也不会变。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231712.png)

#### X.2.2.基础包装数据类型交换

将上面的数据类型换成包装类型，也就是`Integer`对象,结果会如何呢？

```
    public static void main(String[] args) {
        Integer a = 1, b = 2;
        swapInteger(a, b);
        System.out.println("a=" + a + " , b=" + b);
    }
    private static void swapInteger(Integer a, Integer b) {
        Integer temp = a;
        a = b;
        b = temp;
    }
```

结果还是一样，交换无效：

```
a=1 , b=2
```

这个怎么解释呢？

对象类型已经不是基础数据类型了，局部变量表里面的变量存的不是数值，而是对象的引用了。先用`jclasslib`查看一下字节码里面的局部变量表，发现其实和上面差不多，只是描述符变了，从`int`变成`Integer`。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231727.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231736.png)

但是和基础数据类型不同的是，局部变量里面存在的其实是堆里面真实的对象的引用地址，通过这个地址可以找到对象，比如，执行`main()`函数的时候，虚拟机栈如下：

假设 a 里面记录的是 1001 ，去堆里面找地址为 1001 的对象，对象里面存了数值1。b 里面记录的是 1002 ，去堆里面找地址为 1002 的对象，对象里面存了数值2。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231752.png)

而执行`swapInteger(a,b)`的时候，但是还没有交换的时候，相当于把 局部变量复制了一份：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231809.png)

而两者交换之后，其实是`SwapInteger(a,b)`栈帧中的a里面存的地址引用变了，指向了b，但是b里面的，指向了a。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231820.png)

而`swapInteger()`执行结束之后，其实`swapInteger(a,b)`的栈帧会退出虚拟机栈，只留下`main()`的栈帧。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231834.png)

这时候，a其实还是指向1，b还是指向2，因此，交换是没有起效果的。

#### X.2.3.String，StringBuffer，自定义对象交换

一开始，我以为`String`不会变是因为`final`修饰的，但是实际上，不变是对的，但是不是这个原因。原因和上面的差不多。

`String`是不可变的，只是说堆/常量池内的数据本身不可变。但是引用还是一样的，和上面分析的`Integer`一样。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/20210429231846.png)

其实`StringBuffer`和自定义对象都一样，局部变量表内存在的都是引用，所以交换是不会变化的，因为`swap()`函数内的栈帧不会影响调用它的函数的栈帧。

不行我们来测试一下，用事实说话：

```
   public static void main(String[] args) {
        String a = new String("1"), b = new String("2");
        swapString(a, b);
        System.out.println("a=" + a + " , b=" + b);

        StringBuffer stringBuffer1 = new StringBuffer("1"), stringBuffer2 = new StringBuffer("2");
        swapStringBuffer(stringBuffer1, stringBuffer2);
        System.out.println("stringBuffer1=" + stringBuffer1 + " , stringBuffer2=" + stringBuffer2);

        Person person1 = new Person("person1");
        Person person2 = new Person("person2");
        swapObject(person1,person2);
        System.out.println("person1=" + person1 + " , person2=" + person2);
    }

    private static void swapString(String s1,String s2){
        String temp = s1;
        s1 = s2;
        s2 = temp;
    }

    private static void swapStringBuffer(StringBuffer s1,StringBuffer s2){
        StringBuffer temp = s1;
        s1 = s2;
        s2 = temp;
    }

    private static void swapObject(Person p1,Person p2){
        Person temp = p1;
        p1 = p2;
        p2 = temp;
    }


class Person{
    String name;

    public Person(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                '}';
    }
}
```

执行结果,证明交换确实没有起效果。

```
a=1 , b=2
stringBuffer1=1 , stringBuffer2=2
person1=Person{name='person1'} , person2=Person{name='person2'}
```

#### X.2.4.总结

基础数据类型交换，栈帧里面存的是局部变量的数值，交换的时候，两个栈帧不会干扰，`swap(a,b)`执行完成退出栈帧后，`main()`的局部变量表还是以前的，所以不会变。

对象类型交换，栈帧里面存的是对象的地址引用，交换的时候，只是`swap(a,b)`的局部变量表的局部变量里面存的引用地址变化了，同样`swap(a,b)`执行完成退出栈帧后，`main()`的局部变量表还是以前的，所以不会变。

所以不管怎么交换都是不会变的。



















