[toc]



# 命令模式

## 1.模式意图

将一个请求封装成一个对象，从而对这个命令执行撤销、重做等操作。

典型的Eclipse开发中，编辑器的操作就需要用到这个模式，比如Undo、Redo等等。

另外这个模式使得一个命令的触发与接收解耦，这样我们就可以演变成把感兴趣的对象接收这个命令，当命令触发时，这些对象就会执行操作。这个机制也是java事件的处理方式。

## 2.应用场景

- 命令抽象成对象
- 在不同的时刻，指定或者排队命令
- 支持 Undo或者Redo等操作
- 修改日志，当系统崩溃时，利用修改日志执行撤销
- 原语操作上构造一个高层系统

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420231355.jpeg)

**Invoker** 命令的触发者，触发一个命令的执行。

```
/**
 * 命令的触发者，发送命令
 * @author xingoo
 *
 */
class Invoker{
    private Commond commond;
    
    public Invoker(Commond commond) {
        this.commond = commond;
    }
    
    public void action(){
        commond.excute();
    }
}
```

**Receiver** 命令的接受者，针对命令，执行一定的操作。

```
/**
 * 命令的接受者，负责接收命令，进行处理
 * @author xingoo
 *
 */
class Receiver{
    
    public Receiver() {
        
    }
    
    public void action(){
        System.out.println("Action of receiver!");
    }
}
```

**Commond** 命令的抽象接口

```
/**
 * 命令接口，定义命令的统一接口
 * @author xingoo
 *
 */
interface Commond{
    public void excute();
}
```

**ConcreteCommond** 具体的命令，关联一个接收者对象，当命令执行时，执行这个接收者对应的操作。

```
/**
 * 具体的命令
 * @author xingoo
 *
 */
class ConcreteCommond implements Commond{
    
    private Receiver receiver;
    
    public ConcreteCommond(Receiver receiver) {
        this.receiver = receiver;
    }
    
    public void excute() {
        receiver.action();
    }
    
}
```

全部代码

```
package com.xingoo.Commond;
/**
 * 命令的触发者，发送命令
 * @author xingoo
 *
 */
class Invoker{
    private Commond commond;

    public Invoker(Commond commond) {
        this.commond = commond;
    }

    public void action(){
        commond.excute();
    }
}
/**
 * 命令的接受者，负责接收命令，进行处理
 * @author xingoo
 *
 */
class Receiver{

    public Receiver() {

    }

    public void action(){
        System.out.println("Action of receiver!");
    }
}
/**
 * 命令接口，定义命令的统一接口
 * @author xingoo
 *
 */
interface Commond{
    public void excute();
}
/**
 * 具体的命令
 * @author xingoo
 *
 */
class ConcreteCommond implements Commond{

    private Receiver receiver;

    public ConcreteCommond(Receiver receiver) {
        this.receiver = receiver;
    }

    public void excute() {
        receiver.action();
    }

}
/**
 * 客户端调用者
 * @author xingoo
 *
 */
public class Client {
    public static void main(String[] args) {
        Receiver receiver = new Receiver();
        Commond commond = new ConcreteCommond(receiver);
        System.out.println("Commond register in here!");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.out.println("Commond excute in here!");
        Invoker invoker = new Invoker(commond);
        invoker.action();
    }
}
```

运行结果

```
Commond register in here!
Commond excute in here!
Action of receiver!
```





