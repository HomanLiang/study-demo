[toc]



# 访问者模式

## 1.模式意图

对于某个对象或者一组对象，不同的访问者，产生的结果不同，执行操作也不同。此时，就是访问者模式的典型应用了。

## 2.应用场景

- 不同的子类，依赖于不同的其他对象
- 需要对一组对象，进行许多不相关的操作，又不想在类中是现在这些方法
- 定义的类很少改变，但是执行的操作却经常发生改变。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420235658.jpeg)

Context 环境角色

```
class Context{
    List<Node> list = new ArrayList();
    public void add(Node node) {
        list.add(node);
    }
    public void visit(Visitor visitor) {
        for(Node node : list){
            node.accept(visitor);
        }
    }
}
```

Visitor 访问者角色

```
interface Visitor{
    public void visit(NodeA nodeA);
    public void visit(NodeB nodeB);
}
class VisitA implements Visitor{
    public void visit(NodeA nodeA){
        System.out.println("***visitA***");
        nodeA.action();
    }
    public void visit(NodeB nodeB){
        System.out.println("***visitA***");
        nodeB.action();
    }
}
class VisitB implements Visitor{
    public void visit(NodeA nodeA){
        System.out.println("***visitB***");
        nodeA.action();
    }
    public void visit(NodeB nodeB){
        System.out.println("***visitB***");
        nodeB.action();
    }
}
```

Node 被访问角色

```
interface Node{
    public void accept(Visitor visitor);
}
class NodeA implements Node{
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    public void action(){
        System.out.println("NodeA visited");
    }
}
class NodeB implements Node{
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    public void action(){
        System.out.println("NodeB visited");
    }
}
```

全部代码

```
package com.xingoo.test.design.visitor;

import java.util.ArrayList;
import java.util.List;

interface Visitor{
    public void visit(NodeA nodeA);
    public void visit(NodeB nodeB);
}
class VisitA implements Visitor{
    public void visit(NodeA nodeA){
        System.out.println("***visitA***");
        nodeA.action();
    }
    public void visit(NodeB nodeB){
        System.out.println("***visitA***");
        nodeB.action();
    }
}
class VisitB implements Visitor{
    public void visit(NodeA nodeA){
        System.out.println("***visitB***");
        nodeA.action();
    }
    public void visit(NodeB nodeB){
        System.out.println("***visitB***");
        nodeB.action();
    }
}
interface Node{
    public void accept(Visitor visitor);
}
class NodeA implements Node{
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    public void action(){
        System.out.println("NodeA visited");
    }
}
class NodeB implements Node{
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
    public void action(){
        System.out.println("NodeB visited");
    }
}
class Context{
    List<Node> list = new ArrayList();
    public void add(Node node) {
        list.add(node);
    }
    public void visit(Visitor visitor) {
        for(Node node : list){
            node.accept(visitor);
        }
    }
}
public class Client {
    private static Context ctx = new Context();
    public static void main(String[] args) {
        ctx.add(new NodeA());
        ctx.add(new NodeB());
        ctx.visit(new VisitA());
        ctx.visit(new VisitB());
    }
}
```

运行结果

```
***visitA***
NodeA visited
***visitA***
NodeB visited
***visitB***
NodeA visited
***visitB***
NodeB visited
```

