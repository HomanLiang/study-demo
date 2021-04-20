[toc]



# 策略模式

## 1.模式意图

定义一系列的算法，把他们封装起来，使得算法独立于适用对象。

比如，一个系统有很多的排序算法，但是使用哪个排序算法是客户对象的自有。因此把每一个排序当做一个策略对象，客户调用哪个对象，就使用对应的策略方法。

## 2.应用场景

- 当许多的类，仅仅是行为或者策略不同时，可以把行为或策略单独提取出来，这样主体的类就可以进行统一了。
- 需要使用不同的算法。
- 一个类定义了多种行为。

## 3.模式结构

![img](https://images0.cnblogs.com/blog/449064/201411/091214163157652.jpg)

Context 环境角色的，策略的调用者

```
class Context{
    private Strategy strategy;
    public Strategy getStrategy() {
        return strategy;
    }
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
    public void operation(){
        strategy.action();
    }
}
```

Strategy 策略的抽象，规定了统一的调用接口

```
interface Strategy{
    public void action();
}
```

ConcreteStrategy 具体的策略

```
class ConcreteStrategy1 implements Strategy{
    public void action(){
        System.out.println("strategy1 oepration");
    }
}
class ConcreteStrategy2 implements Strategy{
    public void action(){
        System.out.println("strategy2 oepration");
    }
}
```

全部代码

```
package com.xingoo.test.design.strategy;
class Context{
    private Strategy strategy;
    public Strategy getStrategy() {
        return strategy;
    }
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
    public void operation(){
        strategy.action();
    }
}
interface Strategy{
    public void action();
}
class ConcreteStrategy1 implements Strategy{
    public void action(){
        System.out.println("strategy1 oepration");
    }
}
class ConcreteStrategy2 implements Strategy{
    public void action(){
        System.out.println("strategy2 oepration");
    }
}
public class Client {
    public static void main(String[] args) {
        Context ctx = new Context();
        ctx.setStrategy(new ConcreteStrategy1());
        ctx.operation();
        ctx.setStrategy(new ConcreteStrategy2());
        ctx.operation();
    }
}
```

运行结果

```
strategy1 oepration
strategy2 oepration
```





