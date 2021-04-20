[toc]



# 抽象工厂模式

## 1.模式意图

提供对象的使用接口，隐藏对象的创建过程。

## 2.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420210639.jpeg)

- AbstractFactory 提供创建对象的接口。

- ConcreteFactory 提供真正创建对象的实现类，用于组合并创建不同的对象，实现一个产品族。

- AbstractProduct 提供对象的使用接口。

- ConcreteProduct 提供真正的适用对象，隐藏该对象的创建过程，是工厂创建的对象。

- Client 使用者，通过抽象工厂接口，使用不同的具体工厂方法创建对象组合，从而直接使用对象，无需关注对象的创建过程。

## 3.适合场景

1. 系统独立于它的产品创建、组合和表示。即无需关心内部对象时如何创建的，怎么创建的，什么含义。

2. 系统需要多个产品组合中的一个配置。由于对象很多，能够组合出的组合非常多，而系统只是使用某一个组合。

3. 强调的对象的组合结果，而不是他们具体的接口和实现。

## 4.代码结构

**AbstractFactory.java**

```
interface AbstractFactory {
    public AbstractProductA CreateProductA();
    public AbstractProductB CreateProductB();
}
```

**ConcreteFactory.java**

```
class ConcreteFactory1 implements AbstractFactory{

    @Override
    public AbstractProductA CreateProductA() {
        return new ConcreteProductA1();
    }

    @Override
    public AbstractProductB CreateProductB() {
        return new ConcreteProductB1();
    }

}
```

**AbstractProduct.java**

```
interface AbstractProductA {
    public void use();
}
interface AbstractProductB {
    public void use();
}
```

**ConcreteProduct.java**

```
class ConcreteProductA1 implements AbstractProductA{

    @Override
    public void use() {
        // TODO Auto-generated method stub
        System.out.println("use A1 product!");
    }

}
class ConcreteProductB1 implements AbstractProductB{

    @Override
    public void use() {
        // TODO Auto-generated method stub
        System.out.println("use B1 product!");
    }

}
```

**使用方式**

```
public static void main(String[] args){
        AbstractProductA pa;
        AbstractProductB pb;
        
        AbstractFactory fa1 = new ConcreteFactory1();
        pa = fa1.CreateProductA();
        pb = fa1.CreateProductB();
        pa.use();
        pb.use();
        
        AbstractFactory fa2 = new ConcreteFactory2();
        pa = fa2.CreateProductA();
        pb = fa2.CreateProductB();
        pa.use();
        pb.use();
        
    }
```









