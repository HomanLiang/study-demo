[toc]



# 桥接模式

## 1.模式意图

这个模式使用的并不多，但是思想确实很普遍。就是要分离抽象部分与实现部分。

实现弱关联，即在运行时才产生依赖关系。

降低代码之间的耦合。

## 2.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420224010.jpeg)

- Abstraction 抽象部分的基类，定义抽象部分的基础内容。
- RefinedAbstraction 抽象部分的扩充，用于对基类的内容补充，添加特定场景的业务操作。
- Implementor 实现部分的基类，定义实现部分的基本内容。
- ConcreteImplementor 具体的实现类。

## 3.应用场景

- 不希望在抽象和它的实现部分之间有一个固定的绑定关系
- 抽象部分以及实现部分都想通过子类生成一定的扩充内容
- 对一个抽象的实现部分的修改对客户不产生影响

## 4.代码结构

```
package com.xingoo.test;
/**
 * 抽象类基类
 * @author xingoo
 */
abstract class Abstraction{
    abstract public void operation(Implementor imp);
}
/**
 * 实现类 基类
 * @author xingoo
 */
abstract class Implementor{
    abstract public void operation();
}
/**
 * 重新定义的抽象类
 * @author xingoo
 */
class RefinedAbstraction extends Abstraction{
    public void operation(Implementor imp){
        imp.operation();
        System.out.println("RefinedAbstraction");
    }
}
/**
 * 具体的实现类
 * @author xingoo
 */
class ConcreteImplementorA extends Implementor{
    public void operation() {
        System.out.println("ConcreteImplementorA");
    }
}
/**
 * 具体的实现类
 * @author xingoo
 */
class ConcreteImplementorB extends Implementor{
    public void operation() {
        System.out.println("ConcreteImplementorB");
    }
}
public class test {
    public static void main(String[] args){
        RefinedAbstraction abstraction = new RefinedAbstraction();
        abstraction.operation(new ConcreteImplementorA());

        abstraction.operation(new ConcreteImplementorB());
    }
}
```

**运行结果**

```
ConcreteImplementorA
RefinedAbstraction
ConcreteImplementorB
RefinedAbstraction
```



















