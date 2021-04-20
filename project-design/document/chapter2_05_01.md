[toc]



# 职责链模式

## 1.模式意图

避免请求的发送者，和接受者过度的耦合在一起。一个请求者只需要发送一个请求即可，它的请求具体由后面哪个对象进行响应，并不需要关心。而请求的接受者可以自己处理它，也可以把它像链条一样向后传。

因此，请求也就意味着有可能丢失，或者说没有确切的安全保障。

## 2.应用场景

- 降低耦合度
- 增强指派职责的灵活性
- 不保证被接受

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420231141.jpeg)

**Handler** 定义一个接口，实现向后传递的过程

```
abstract class Handler{
    protected Handler successor;

    public abstract void handleRequest();

    public Handler getSuccessor() {
        return successor;
    }

    public void setSuccesor(Handler successor) {
        this.successor = successor;
    }

}
```

**ConcreteHandler** 可以负责请求，也可以向后传递

````
class ConcreteHandler extends Handler{
    public void handleRequest(){
        if(getSuccessor() != null){
            System.out.println("getSuccessor !");
            getSuccessor().handleRequest();
        }else{
            System.out.println("handle in this! request()!");
        }
    }
}
````

**全部代码**

```
package com.xingoo;
abstract class Handler{
    protected Handler successor;

    public abstract void handleRequest();

    public Handler getSuccessor() {
        return successor;
    }

    public void setSuccesor(Handler successor) {
        this.successor = successor;
    }

}
class ConcreteHandler extends Handler{
    public void handleRequest(){
        if(getSuccessor() != null){
            System.out.println("getSuccessor !");
            getSuccessor().handleRequest();
        }else{
            System.out.println("handle in this! request()!");
        }
    }
}
public class Client {
    public static void main(String[] args) {
        Handler handle1,handle2,handle3;
        handle1 = new ConcreteHandler();
        handle2 = new ConcreteHandler();
        handle3 = new ConcreteHandler();
        handle1.setSuccesor(handle2);
        handle2.setSuccesor(handle3);
        handle1.handleRequest();
    }
}
```

**运行结果**

```
getSuccessor !
getSuccessor !
handle in this! request()!
```









