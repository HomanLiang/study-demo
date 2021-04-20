[toc]



# 模板方法

## 1.模式意图

定义一个类的框架，当它有不同的类时，再具体实现。

比如，我们设计一个跨系统的客户端软件，Windows需要一套展现类，Linux需要一套，mac还需要一套。这样，只需要抽取他们的共同操作编程一个框架类，具体使用到哪个系统时，再使用对应的类，有点像C++里面的模板。

## 2.应用场景

- 一次性实现一个类的不变部分，其他的部分留到子类实现。
- 各个子类提取公共部分成为超类
- 控制子类的扩展。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420235513.jpeg)

AbstractClass 抽象类框架

```
abstract class AbstractClass{
    public void action(){
        step1();
        step2();
        newMethod();
    }
    abstract protected void step1();
    abstract protected void step2();
    abstract protected void newMethod();
}
```

Class 具体的子类，进行扩展

```
class Class1 extends AbstractClass{
    protected void newMethod() {
        System.out.println("class1 newMethod");
    }
    protected void step1() {
        System.out.println("class1 step1");
    }
    protected void step2() {
        System.out.println("class1 step2");
    }
}
class Class2 extends AbstractClass{
    protected void newMethod() {
        System.out.println("class2 newMethod");
    }
    protected void step1() {
        System.out.println("class2 step1");
    }
    protected void step2() {
        System.out.println("class2 step2");
    }
}
```

全部代码

```
package com.xingoo.test.design.template;
abstract class AbstractClass{
    public void action(){
        step1();
        step2();
        newMethod();
    }
    abstract protected void step1();
    abstract protected void step2();
    abstract protected void newMethod();
}
class Class1 extends AbstractClass{
    protected void newMethod() {
        System.out.println("class1 newMethod");
    }
    protected void step1() {
        System.out.println("class1 step1");
    }
    protected void step2() {
        System.out.println("class1 step2");
    }
}
class Class2 extends AbstractClass{
    protected void newMethod() {
        System.out.println("class2 newMethod");
    }
    protected void step1() {
        System.out.println("class2 step1");
    }
    protected void step2() {
        System.out.println("class2 step2");
    }
}
public class Client {
    private static AbstractClass class1 = new Class1();
    private static AbstractClass class2 = new Class2();
    public static void main(String[] args) {
        class1.action();
        class2.action();
    }
}
```

运行结果

```
class1 step1
class1 step2
class1 newMethod
class2 step1
class2 step2
class2 newMethod
```



