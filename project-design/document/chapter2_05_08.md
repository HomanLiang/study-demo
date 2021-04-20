[toc]



# 状态模式

## 1.模式意图

允许一个对象在内部改变它的状态，并根据不同的状态有不同的操作行为。

例如，水在固体、液体、气体是三种状态，但是展现在我们面前的确实不同的感觉。通过改变水的状态，就可以更改它的展现方式。

## 2.应用场景

- 当一个对象的行为，取决于它的状态时
- 当类结构中存在大量的分支，并且每个分支内部的动作抽象相同，可以当做一种状态来执行时。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420235118.jpeg)

**Context** 环境角色，里面包含状态对象

```
class Context{
    private State state;
    public void setState(State state) {
        this.state = state;
    }
    public void operation(){
        state.operation();
    }
}
```

**State** 状态的抽象接口

```
interface State{
    public void operation();
}
```

**ConcreteState** 具体的状态角色

```
class ConcreteState1 implements State{
    public void operation(){
        System.out.println("state1 operation");
    }
}
class ConcreteState2 implements State{
    public void operation(){
        System.out.println("state2 operation");
    }
}
class ConcreteState3 implements State{
    public void operation(){
        System.out.println("state3 operation");
    }
}
```

全部代码

```
package com.xingoo.test.design.state;
class Context{
    private State state;
    public void setState(State state) {
        this.state = state;
    }
    public void operation(){
        state.operation();
    }
}
interface State{
    public void operation();
}
class ConcreteState1 implements State{
    public void operation(){
        System.out.println("state1 operation");
    }
}
class ConcreteState2 implements State{
    public void operation(){
        System.out.println("state2 operation");
    }
}
class ConcreteState3 implements State{
    public void operation(){
        System.out.println("state3 operation");
    }
}
public class Client {
    public static void main(String[] args) {
        Context ctx = new Context();
        State state1 = new ConcreteState1();
        State state2 = new ConcreteState2();
        State state3 = new ConcreteState3();

        ctx.setState(state1);
        ctx.operation();

        ctx.setState(state2);
        ctx.operation();

        ctx.setState(state3);
        ctx.operation();
    }
}
```

运行结果

```
state1 operation
state2 operation
state3 operation
```

