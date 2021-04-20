[toc]



# 创建者模式

## 1.模式意图

一个对象的创建十分复杂，为了区分构建过程和使用过程，因此分开。使用一个Director类进行对象的创建，Builder规定了这个创建过程。

## 2.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420212510.jpeg)

- Builder 抽象建造者接口，规范各个组成部分的构建。
- ConcreteBuilder 具体建造者角色，实现组成部分的构建，并提供示例。
- Product 产品角色，创建返回的对象。
- Director 导演者角色，负责创建以及使用。

## 3.使用场景

- 当创建对象复杂并且与其使用过程独立。
- 构造过程允许构造的对象有不同的表示。

## 4.代码结构

Builder接口

```
abstract class Builder{
    public abstract void buildPart1();
    public abstract void buildPart2();
    public abstract Product retrieveResult();
}
```

ConcreteBuilder 具体产品

```
class ConcreteBuilder extends Builder{
    public void buildPart1() {

    }
    public void buildPart2() {

    }
    public Product retrieveResult() {
        return null;
    }
}
```

Product 产品

```
class Product{
    //Anything
}
```

Director 导演者角色

```
class Director{
    private Builder builder;
    public void Constructor(){
        builder = new ConcreteBuilder();
        builder.buildPart1();
        builder.buildPart2();
        builder.retrieveResult();
    }
}
```

## 5.生活中的设计模式

“汽车人变身”也是伴随着复杂的变化过程。而汽车人的转变只在一瞬之间，看起来行云流水，这就有点创建者的味道。擎天柱的各个身体部分经过复杂的变化，最后变身成为汽车人。

```
/**
 * Builder
 */
interface Transformer{
    public void Create_Head();
    public void Create_Body();

    public Autobots transforming();
}
/**
 * ConcreteBuilder
 */
class Transformer_Captain implements Transformer{
    public void Create_Head() {
        System.out.println("变形出脑袋...");
    }
    public void Create_Body() {
        System.out.println("变形出身体...");
    }
    public Autobots transforming() {
        return new Autobots();
    }
}
/**
 * Product
 */
class Autobots{
     Autobots(){
        System.out.println("啊！...变形金刚 变身....");
    }
}
/**
 * 这个类当做Director
 */
public class TransformerBuilder {

    public static Autobots Transforming(Transformer optimusPrime){
        optimusPrime.Create_Head();
        optimusPrime.Create_Body();
        return optimusPrime.transforming();
    }

    public static void main(String[] args){
        Transformer optimusPrime = new Transformer_Captain();
        Transforming(optimusPrime);
    }

}
```

变身结果

```
变形出脑袋...
变形出身体...
啊！...变形金刚 变身....
```



