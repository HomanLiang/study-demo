[toc]



# 观察者模式

## 1.模式意图

观察者模式，也叫发布/订阅模式，从名字来说就能察觉到它的过程应该是，发布——其他人接受。

**这个模式定义了对象之间的一种依赖关系，当一个对象发生变化时，其他的对象收到更新，也发生变化**。

模拟我们订阅邮件这个场景，不管我们的邮箱是在登陆还是关闭，邮件都会发送到邮箱里面。只要把自己的邮箱订阅到这个邮件就可以了！这个模式也是这样一个过程。

这个模式代码相对来说比较容易理解，而且应用很广泛。

## 2.应用场景

- 当一个模型有几个展现方面，通过修改一个展现，顺便更新其他的。就好比一个网站的有web端，也有移动端，当web端的数据发生变化时，移动端的数据展现也要更新。
- 对一个对象发生改变，而不知道将有多少对象会发生改变时，利用这种模式可以有效的管理对象。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420234837.jpeg)

**Subject** 抽象的主题角色

```
interface Subject{
    public void attach(Observer observer);
    public void detach(Observer observer);
    void notifyObservers();
}
```

**ConcreteSubject** 具体的主题角色，内部有一个容易，用于存放订阅者

```
class ConcreteSubject implements Subject{
    List<Observer> list = new ArrayList();
    public void attach(Observer observer) {
        list.add(observer);
    }
    public void detach(Observer observer) {
        list.remove(observer);
    }
    public void notifyObservers() {
        for(Observer o : list){
            o.update();
        }
    }
}
```

**Observer** 抽象的订阅者角色

```
interface Observer{
    public void update();
}
```

**ConcreteObserver** 具体的订阅者

```
class ConcreteObserver1 implements Observer{
    public void update() {
        System.out.println("ConcreteObserver1 update");
    }
}
class ConcreteObserver2 implements Observer{
    public void update() {
        System.out.println("ConcreteObserver2 update");
    }
}
class ConcreteObserver3 implements Observer{
    public void update() {
        System.out.println("ConcreteObserver3 update");
    }
}
```

全部代码

```
package com.xingoo.test.design.observer;

import java.util.ArrayList;
import java.util.List;

interface Subject{
    public void attach(Observer observer);
    public void detach(Observer observer);
    void notifyObservers();
}
class ConcreteSubject implements Subject{
    List<Observer> list = new ArrayList();
    public void attach(Observer observer) {
        list.add(observer);
    }
    public void detach(Observer observer) {
        list.remove(observer);
    }
    public void notifyObservers() {
        for(Observer o : list){
            o.update();
        }
    }
}
interface Observer{
    public void update();
}
class ConcreteObserver1 implements Observer{
    public void update() {
        System.out.println("ConcreteObserver1 update");
    }
}
class ConcreteObserver2 implements Observer{
    public void update() {
        System.out.println("ConcreteObserver2 update");
    }
}
class ConcreteObserver3 implements Observer{
    public void update() {
        System.out.println("ConcreteObserver3 update");
    }
}
public class Client {
    public static void main(String[] args) {
        Subject subject = new ConcreteSubject();
        Observer o1 = new ConcreteObserver1();
        Observer o2 = new ConcreteObserver2();
        Observer o3 = new ConcreteObserver3();
        subject.attach(o1);
        subject.attach(o2);
        subject.attach(o3);
        subject.notifyObservers();

        subject.detach(o2);
        subject.notifyObservers();
    }
}
```

运行结果

```
ConcreteObserver1 update
ConcreteObserver2 update
ConcreteObserver3 update
ConcreteObserver1 update
ConcreteObserver3 update
```

