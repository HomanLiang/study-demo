[toc]



# 类加载子系统

## 类加载器与类的加载过程

![image-20210102184758508](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210102184758508.png)

###  类加载器子系统作用

1. 类加载器子系统负载从文件系统或者网络中加载 class 文件，class 文件在文件开头有特定的文件标识。
2. ClassLoader 只负责 class 文件的加载，至于它是否可以运行，则由 ExecutionEngine 决定
3. 加载的类信息存放于一块称为方法区的内存空间。除了类的信息外，方法区中还会存放运行时常量池信息，可能还包括字符串常量和数字常量（这部分常量信息是 class 文件中常量池部分的内存映射）

![image-20210102191847140](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210102191847140.png)

### 类加载器 ClassLoader 角色

1. class 文件存在于本地硬盘上，可以理解为设计师画在纸上的模板，而最终这个模板在执行的时候是要加载到 JVM 当中来根据这个文件实例化出 n 个一模一样的实例
2. class 文件加载到 JVM 中，被称为 DNA 元数据模板，放在方法区
3. 在 .class 文件 --> JVM --> 最终成为元数据模板，此过程就要一个运输工具（类装载器 ClassLoader），扮演一个快递员的角色

![image-20210102201611438](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210102201611438.png)

### 类的加载过程

![image-20210102202401432](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210102202401432.png)

![image-20210102202532800](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/jvm-demo/image-20210102202532800.png)

#### 加载：

1. 通过一个类的全限定名获取定义此类的二进制字节流
2. 将这个字节流所代表的静态存储结构转化为方法区的运行时数据结构
3. 在内存中生成一个代表这个类的 java.lang.Class 对象，作为方法区这个类的各种数据访问入口

**补充：加载 .class 文件的方式**

- 从本地系统中直接加载
- 通过网络获取，典型场景：Web Applet
- 从 zip 压缩包中读取，成为日后 jar、war格式的基础
- 运行时计算生成，使用最多的是：动态代理技术
- 由其他文件生成，典型场景：JSP 应用
- 从专用数据库中提取 .class 文件，比较少见
- 从加密文件中获取，典型的防 Class 文件被反编译的保护措施

#### 链接

##### 验证（Verify）：

- 目的在于保证 Class 文件的字节流中包含符合当前虚拟机要求，保证被加载类的正确性，不会危害虚拟机自身安全
- 主要包括四种验证，文件格式验证，元数据验证，字节码验证，符号引用验证

##### 准备（Prepare）:

- 为类变量分配内存并且设置该类的默认初始化值，即零值
- 这里不包含 final 修饰的 static，因为 final 在编译的时候就会分配了，准备阶段会显式初始化
- 这里不会为实例变量分配初始化，类变量会分配在方法区中，而实例变量是会随着对象一起分配到 Java 堆中

##### 解析（Resolve）：

- 将常量池内的符号引用转换为直接引用的过程
- 实时上，解析操作往往会伴随着 JVM 在执行完初始化之后再执行
- 符号引用就是一组符号来描述所引用的目标。符号引用的字面量形式明确定义在《Java 虚拟机规范》的 Class 文件格式中，直接引用就是直接指向目标的指针、相对偏移量或一个间接定位到目标的句柄
- 解析动作主要针对类或接口、字段、类方法、接口方法、方法类型等。对应常量池中的CONSTANT_CLASS_INFO、CONSTANT_FIELDREF_INFO、CONSTANT_METHODREF_INFO等

#### 初始化

- 初始化阶段就是执行类构造方法<clinit>()的过程
- 此方法不需要定义，是 javac 编译器自动收集类中的所有类变量的赋值动作和静态代码块中的语句合并而来
- 构造器方法中指令按语句在源文件中出现得顺序执行。
- <clinit>()不同于类的构造器。（关联：构造器是虚拟机视角下的<clinit>()）
- 若该类具有父类，JVM 会保证子类的<clinit>() 执行前，父类的<clinit>()已经执行完毕
- 虚拟机必须保证一个类的 <clinit>() 方法在多线程下被同步加锁



## 类加载器的分类







## ClassLoader 的使用说明







## 双亲委派机制





## 其他





























































