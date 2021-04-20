[toc]



# 迭代模式

## 1.模式意图

提供一个方法按顺序遍历一个集合内的元素，而又不需要暴露该对象的内部表示。

## 2.应用场景

- 访问一个聚合的对象，而不需要暴露对象的内部表示
- 支持对聚合对象的多种遍历
- 对遍历不同的对象，提供统一的接口。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420231908.jpeg)

Iterator 定义访问的接口

```
/**
 * 抽象的迭代，有判断结束和下一个，获取当前元素等函数
 * @author xingoo
 *
 */
interface Iterator{
    void first();
    void next();
    boolean isDone();
    Object currentItem();
}
```

ConcreteIterator 具体的迭代器，跟踪聚合内的元素

```
/**
 * 具体的迭代类
 * @author xingoo
 *
 */
class ConcreteIterator implements Iterator{
    private ConreteAggregate agg;
    private int index = 0;
    private int size = 0;
    
    public ConcreteIterator(ConreteAggregate agg) {
        this.agg = agg;
        size = agg.size();
        index = 0;
    }

    public void first() {
        index = 0;
    }

    public void next() {
        if(index < size){
            index++;
        }
    }

    public boolean isDone() {
        return (index >= size);
    }

    public Object currentItem() {
        return agg.getElement(index);
    }
    
}
```

Aggregate 提供聚合的接口

```
/**
 * 聚合的类
 * @author xingoo
 *
 */
abstract class Aggregate{
    public Iterator createIterator(){
        return null;
    }
}
```

ConcreteAggregate 具体的聚合

```
/**
 * 具体的聚合对象，拥有大小，创建迭代子等函数
 * @author xingoo
 *
 */
class ConreteAggregate extends Aggregate{
    private Object[] obj = {"test1","test2","test3","test4"};
    public Iterator createIterator(){
        return new ConcreteIterator(this);
    }
    public Object getElement(int index){
        if(index < obj.length){
            return obj[index];
        }else{
            return null;
        }
    }
    public int size(){
        return obj.length;
    }
}
```

全部代码

```
package com.xingoo.Iterator;
/**
 * 聚合的类
 * @author xingoo
 *
 */
abstract class Aggregate{
    public Iterator createIterator(){
        return null;
    }
}
/**
 * 抽象的迭代，有判断结束和下一个，获取当前元素等函数
 * @author xingoo
 *
 */
interface Iterator{
    void first();
    void next();
    boolean isDone();
    Object currentItem();
}
/**
 * 具体的聚合对象，拥有大小，创建迭代子等函数
 * @author xingoo
 *
 */
class ConreteAggregate extends Aggregate{
    private Object[] obj = {"test1","test2","test3","test4"};
    public Iterator createIterator(){
        return new ConcreteIterator(this);
    }
    public Object getElement(int index){
        if(index < obj.length){
            return obj[index];
        }else{
            return null;
        }
    }
    public int size(){
        return obj.length;
    }
}
/**
 * 具体的迭代类
 * @author xingoo
 *
 */
class ConcreteIterator implements Iterator{
    private ConreteAggregate agg;
    private int index = 0;
    private int size = 0;

    public ConcreteIterator(ConreteAggregate agg) {
        this.agg = agg;
        size = agg.size();
        index = 0;
    }

    public void first() {
        index = 0;
    }

    public void next() {
        if(index < size){
            index++;
        }
    }

    public boolean isDone() {
        return (index >= size);
    }

    public Object currentItem() {
        return agg.getElement(index);
    }

}
/**
 * 客户端 使用方法
 * @author xingoo
 *
 */
public class Client {
    private Iterator it;
    private Aggregate agg = new ConreteAggregate();
    public void operation(){
        it = agg.createIterator();
        while(!it.isDone()){
            System.out.println(it.currentItem().toString());
            it.next();
        }
    }
    public static void main(String[] args) {
        Client client = new Client();
        client.operation();
    }
}
```

运行结果

```
test1
test2
test3
test4
```





