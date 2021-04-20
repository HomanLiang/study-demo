[toc]



# 代理模式

## 1.模式意图

代理模式为其他的对象增加一个代理对象，进行访问控制。从而避免直接访问一个对象，造成效率或者安全性上的降低。

## 2.应用场景

- 远程代理，为一个远程对象，创建一个本地的代理对象。每次访问，直接访问本地代理对象即可。
- 虚代理，如果对象很大，直接访问开销很大，可以为他创建一个代理对象，只生成关键的信息即可。
- 保护代理，为某个对象增加一种保护机制，只有一定的权限才能通过这个代理，访问后面的对象。
- 智能指针，有点像C++里面的那个智能指针，为指针进行计数和销毁等操作。避免出现悬垂指针等现象。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420230919.jpeg)

**Subject** 类接口，定义代理类，实现类等的规范。

```
interface Subject{
    public void request();
}
```

**RealSubject** 真正的对象

```
class RealSubject implements Subject{
    public void request() {
        System.out.println("ConcreteSubject request()");
    }
}
```

**ProxySubject** 代理类，可以对真正的实现部分加上一种类似装饰的效果。看！多像**AOP**！

```
class ProxySubject implements Subject{
    private RealSubject subject;
    
    public void request() {
        preRequest();
        
        if(subject == null){
            subject = new RealSubject();
        }
        subject.request();
        
        postRequest();
    }
    
    public void preRequest(){
        System.out.println("ConcreteSubject preRequest()");
    }
    
    public void postRequest(){
        System.out.println("ConcreteSubject postRequest()");
    }
}
```

**全部代码**

```
package com.xingoo.Proxy;
interface Subject{
    public void request();
}
class ProxySubject implements Subject{
    private RealSubject subject;

    public void request() {
        preRequest();

        if(subject == null){
            subject = new RealSubject();
        }
        subject.request();

        postRequest();
    }

    public void preRequest(){
        System.out.println("ConcreteSubject preRequest()");
    }

    public void postRequest(){
        System.out.println("ConcreteSubject postRequest()");
    }
}
class RealSubject implements Subject{
    public void request() {
        System.out.println("ConcreteSubject request()");
    }
}
public class Client {
    public static void main(String[] args) {
        Subject subject = new ProxySubject();
        subject.request();
    }
}
```

**运行结果**

```
ConcreteSubject preRequest()
ConcreteSubject request()
ConcreteSubject postRequest()
```

