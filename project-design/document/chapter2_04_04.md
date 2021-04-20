[toc]



# 装饰模式

## 1.模式意图

在不改变原来类的情况下，进行扩展。

动态的给对象增加一个业务功能，就功能来说，比生成子类更方便。

## 2.应用场景

- 在不生成子类的情况下，为对象动态的添加某些操作。
- 处理一些可以撤销的职责。
- 当不能使用生成子类来扩充时。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420224605.jpeg)

**Component** 外部接口，用于定义外部调用的形式。提供默认的处理方法。

```
interface Component{
     public void operation();
 }
```

**ConcreteComponent** 具体的处理类，用于实现operation方法。

```
class ConcreteComponent implements Component{

    @Override
    public void operation() {
        // TODO Auto-generated method stub
        System.out.println("ConcreteComponent operation()");
    }
    
}
```

**Decorator** 装饰类，内部关联一个component对象，调用其operation方法，并添加自己的业务操作。

```
class Decorator implements Component{
    private Component component;
    @Override
    public void operation() {
        // TODO Auto-generated method stub
        System.out.println("before decorator!");
        component.operation();
        System.out.println("after decorator!");
    }
    public Decorator() {
        // TODO Auto-generated constructor stub
    }
    public Decorator(Component component){
        this.component = component;
    }
    
}
```

## 4.全部代码

```
package com.xingoo.decorator;
interface Component{
    public void operation();
}
class ConcreteComponent implements Component{

    @Override
    public void operation() {
        // TODO Auto-generated method stub
        System.out.println("ConcreteComponent operation()");
    }

}
class Decorator implements Component{
    private Component component;
    @Override
    public void operation() {
        // TODO Auto-generated method stub
        System.out.println("before decorator!");
        component.operation();
        System.out.println("after decorator!");
    }
    public Decorator() {
        // TODO Auto-generated constructor stub
    }
    public Decorator(Component component){
        this.component = component;
    }

}


public class test {
    public static void main(String[] args) {
        Component component = new Decorator(new ConcreteComponent());
        component.operation();
    }
}
```

**运行结果**

```
before decorator!
ConcreteComponent operation()
after decorator!
```







