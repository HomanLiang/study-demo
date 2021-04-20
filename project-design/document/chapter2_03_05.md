[toc]



# 单例模式

## 1.模式意图

保证类仅有一个实例，并且可以供应用程序全局使用。为了保证这一点，就需要这个类自己创建自己的对象，并且对外有公开的调用方法。

## 2.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420213241.jpeg)

Singleton 单例类，内部包含一个本身的对象。并且构造方法时私有的。

## 3.使用场景

当类只有一个实例，而且可以从一个固定的访问点访问它时。

## 4.代码结构

**【饿汉模式】**通过定义Static 变量，在类加载时，静态变量被初始化。

```
package com.xingoo.eagerSingleton;
class Singleton{
    private static final Singleton singleton = new Singleton();
    /**
     * 私有构造函数
     */
    private Singleton(){

    }
    /**
     * 获得实例
     * @return
     */
    public static Singleton getInstance(){
        return singleton;
    }
}
public class test {
    public static void main(String[] args){
        Singleton.getInstance();
    }
}
```

**【懒汉模式】**

```
package com.xingoo.lazySingleton;
class Singleton{
    private static Singleton singleton = null;

    private Singleton(){

    }
    /**
     * 同步方式，当需要实例的才去创建
     * @return
     */
    public static synchronized Singleton getInstatnce(){
        if(singleton == null){
            singleton = new Singleton();
        }
        return singleton;
    }
}
public class test {
    public static void main(String[] args){
        Singleton.getInstatnce();
    }
}
```

