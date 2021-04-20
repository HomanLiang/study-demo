[toc]



# 外观模式

## 1.模式意图

外观模式主要是为了为一组接口提供一个一致的界面。从而使得复杂的子系统与用户端分离解耦。

有点类似家庭常用的一键开关，只要按一个键，台灯卧室客厅的灯都亮了。虽然他们各有各自的开关，但是对外用一个来控制。

## 2.应用场景

- 为复杂系统 提供简单的接口。
- 客户程序与抽象类的实现部分分离。
- 构建层次系统时，用作入口。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420224830.jpeg)

Facade 对外的统一接口

```
class Facade{
    public void operation(){
        subsystemClass1.operation();
        subsystemClass2.operation();
    }
}
```

subsystem Class 内部系统的实现类

```
class subsystemClass1{
    public static void operation(){
        System.out.println("subsystemClass1 operation()");
    }
}
class subsystemClass2{
    public static void operation(){
        System.out.println("subsystemClass2 operation()");
    }
}
```

## 4.代码结构

```
package com.xingoo.facade;
class Facade{
    public void operation(){
        subsystemClass1.operation();
        subsystemClass2.operation();
    }
}

class subsystemClass1{
    public static void operation(){
        System.out.println("subsystemClass1 operation()");
    }
}
class subsystemClass2{
    public static void operation(){
        System.out.println("subsystemClass2 operation()");
    }
}
public class Client {
    public static void main(String[] args) {
        Facade facade = new Facade();
        facade.operation();
    }
}
```

运行结果

```
subsystemClass1 operation()
subsystemClass2 operation()
```

