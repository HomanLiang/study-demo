[toc]



# 适配器模式

## 1.模式意图

如果已经有了一种类，而需要调用的接口却并不能通过这个类实现。因此，把这个现有的类，经过适配，转换成支持接口的类。

换句话说，就是把一种现有的接口编程另一种可用的接口。

## 2.模式结构

【类的适配器】

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420215127.jpeg)

- Target 目标接口
- Adaptee 现有的类
- Adapter 中间转换的类，即实现了目标接口，又继承了现有的类。

```
package com.xingoo.test1;
interface Target{
    public void operation1();
    public void operation2();
}
class Adaptee{
    public void operation1(){
        System.out.println("operation1");
    }
}

class Adapter extends Adaptee implements Target{
    public void operation2() {
        System.out.println("operation2");
    }
}

public class test {
    public static void main(String[] args){
        Target tar = new Adapter();
        tar.operation1();
        tar.operation2();
    }
}
```

【对象的适配器】

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420215208.jpeg)

与上面不同的是，这次并不是直接继承现有的类，而是把现有的类，作为一个内部的对象，进行调用。

```
package com.xingoo.test2;

interface Target{
    public void operation1();
    public void operation2();
}

class Adaptee{
    public void operation1(){
        System.out.println("operation1");
    }
}

class Adapter implements Target{
    private Adaptee adaptee;
    public Adapter(Adaptee adaptee){
        this.adaptee = adaptee;
    }
    public void operation1() {
        adaptee.operation1();
    }

    public void operation2() {
        System.out.println("operation2");
    }

}
public class test {
    public static void main(String[] args){
        Target tar = new Adapter(new Adaptee());
        tar.operation1();
        tar.operation2();
    }
}
```

## 3.使用场景

- 想使用一个已经存在的类，但是它的接口并不符合要求
- 想创建一个可以复用的类，这个类与其他的类可以协同工作
- 想使用已经存在的子类，但是不可能对每个子类都匹配他们的接口。因此对象适配器可以适配它的父类接口。

## 4.生活中的设计模式

俗话说，窈窕淑女君子好逑，最近看跑男，十分迷恋Baby。

但是，如果桃花运浅，身边只有凤姐，那么也不需要担心。

只需要简单的化妆化妆，PS一下，美女凤姐，依然无可替代！

虽然，没有AngleBaby，但是我们有凤姐，所以依然可以看到AngleBaby甜美的笑。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420215342.jpeg)

```
package com.xingoo.test3;
interface BeautifulGirl{
    public void Smiling();
}
class UglyGirl{
    public void Crying(){
        System.out.println("我在哭泣...");
    }
}
class ApplyCosmetics implements BeautifulGirl{
    private UglyGirl girl;
    public ApplyCosmetics(UglyGirl girl){
        this.girl = girl;
    }
    public void Smiling() {
        girl.Crying();
    }
}
public class test {
    public static void main(String[] args){
        BeautifulGirl girl = new ApplyCosmetics(new UglyGirl());
        girl.Smiling();
    }
}
```

运行结果

```
我在哭泣...
```

