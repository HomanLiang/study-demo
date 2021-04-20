[toc]



# 享元模式

## 1.模式意图

享元模式，也叫【轻量级模式】或者【蝇量级模式】。主要目的就是为了减少细粒度资源的消耗。比如，一个编辑器用到大量的字母数字和符号，但是不需要每次都创建一个字母对象，只需要把它放到某个地方共享使用，单独记录每次创建的使用上下文就可以了。

再比如餐馆的桌子，餐具，这些都是享元模式的体现。客户是流动的，每次吃饭都是用饭店固定的那些餐具，而饭店也不需要每次新来顾客，就买新的盘子餐具。

## 2.应用场景

- 一个系统应用到了大量的对象，而且很多都是重复的。
- 由于大量对象的使用，造成了存储效率上的开销。
- 对象的状态大多是外部状态，不干扰状态本身。
- 如果剔除这些外部状态，可以用一组小规模的对象表示共享对象。

最近项目中就有这个使用场景，比如一些文件的图标，由于重复使用，完全可以采用这种模式，放入缓存中，以后每次调用直接从缓存中读取就行了。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420225051.jpeg)

**FlyweightFactorty** 提供共享对象的工厂方法，里面含有一个聚集对象，一般都是用HashMap。通常这个工厂类都通过单例模式创建。

```
class FlyweightFactory{
    private HashMap map = new HashMap();

    public FlyweightFactory() {
    }

    public Flyweight factory(int state){
        if(map.containsKey(state)){
            return (Flyweight)map.get(state);
        }else{
            map.put(state, new ConcreteFlyweight(state));
            return (Flyweight)map.get(state);
        }
    }

    public void CheckMap(){
        System.out.println("*****************************************");
        int i=0;
        for(Iterator it=map.entrySet().iterator();it.hasNext(); ){
            Map.Entry e = (Map.Entry)it.next();
            System.out.println("map.get("+(i++)+") : "+ e.getKey());
        }
        System.out.println("*****************************************");
    }
}
```

**Flyweight** 共享对象的接口，描述统一标识

```
interface Flyweight{
    public int getState();
}
```

**ConcreteFlyweight** 真正的具体实现类

```
class ConcreteFlyweight implements Flyweight{
    private int state;
    public ConcreteFlyweight(int state) {
        this.state = state;
    }
    public int getState() {
        return state;
    }
}
```

## 4.全部代码

```
package com.xingoo.Flyweight;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class FlyweightFactory{
    private HashMap map = new HashMap();
    
    public FlyweightFactory() {
    }
    
    public Flyweight factory(int state){
        if(map.containsKey(state)){
            return (Flyweight)map.get(state);
        }else{
            map.put(state, new ConcreteFlyweight(state));
            return (Flyweight)map.get(state);
        }
    }
    
    public void CheckMap(){
        System.out.println("*****************************************");
        int i=0;
        for(Iterator it=map.entrySet().iterator();it.hasNext(); ){
            Map.Entry e = (Map.Entry)it.next();
            System.out.println("map.get("+(i++)+") : "+ e.getKey());
        }
        System.out.println("*****************************************");
    }
}
interface Flyweight{
    public int getState();
}
class ConcreteFlyweight implements Flyweight{
    private int state;
    public ConcreteFlyweight(int state) {
        this.state = state;
    }
    public int getState() {
        return state;
    }
}
public class Client {
    public static void main(String[] args) {
        FlyweightFactory factory = new FlyweightFactory();
        factory.factory(4);
        factory.factory(2);
        factory.factory(2);
        factory.factory(1);
        factory.factory(1);
        factory.factory(3);
        factory.CheckMap();
    }
}
```

运行结果

```
*****************************************
map.get(0) : 1
map.get(1) : 2
map.get(2) : 3
map.get(3) : 4
*****************************************
```







