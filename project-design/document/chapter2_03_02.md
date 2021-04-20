[toc]



# 工厂方法模式

## 1. 模式意图

工厂方法在MVC中应用的很广泛。

工厂方法意在分离产品与创建的两个层次，使用户在一个工厂池中可以选择自己想要使用的产品，而忽略其创建过程。

简单来说，就像一个大型的工厂，对于消费者来说，只需要知道都有什么工厂的产品生产出来，而不需要关心工厂是如何生产产品的。对于工厂来说，必须知道所有的产品的制造方法。

## 2.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420211216.jpeg)

- Creator 创建工厂的接口

- ConcreteCreator 具体的产品创建者

- Product 产品的接口

- ConcreteProduct 具体的产品

## 3. 适合场景

- 当一个类不知道它所必须创建的对象的类的时候。
- 当一个类希望由他的子类来制定它所创建的对象的时候
- 当类创建对象的职责委托给多个帮助子类中的某一个，并且希望进行一些信息的局部初始化的时候。

## 4.代码结构

工厂方法需要一个工厂接口，提供了基本的类模式，和一个产品接口，提供基本的方法。

```
interface Creator{
    public Product factory();
}
interface Product{
    public void Say();
}
```

接下来就是具体的工厂方法，可以创建不同的产品。

```
class ConcreteCreator1 implements Creator{
    public Product factory() {
        return new ConcreteProduct1();
    }
}
class ConcreteCreator2 implements Creator{
    public Product factory() {
        return new ConcreteProduct2();
    }
}
```

其次需要不同的产品。

```
class ConcreteProduct1 implements Product{
    public void Say() {
        System.out.println("ConcreteProduct1");
    }
}
class ConcreteProduct2 implements Product{
    public void Say() {
        System.out.println("ConcreteProduct2");
    }
}
```

使用方法，大致如下

```
public class test {
    public static Creator creator1,creator2;
    public static Product product1,product2;
    public static void main(String[] args){
        creator1 = new ConcreteCreator1();
        creator2 = new ConcreteCreator2();

        product1 = creator1.factory();
        product2 = creator2.factory();

        product1.Say();
        product2.Say();
    }
}
```

## 5.生活中的设计模式

目前的生活，是一种快餐的生活。有时候不想做饭，去饭店也等不及，肯德基、麦当劳、德克士就成为了一种很方便的方式。

我们去肯德基，通常会吃点汉堡，点点可乐，不需要知道这些东西是怎么做出来的，直接点，拿到吃，简单方便，这就是生活中的快餐工厂。

通过一个MakeChoice方法，可以统一的进行选择。

```
interface KFC {
    public Client working();
}

interface Client {
    public void eating();
}

class Bread_Menu implements KFC {
    public Client working() {
        return new Client_Bread();
    }
}

class Cola_Menu implements KFC {
    public Client working() {
        return new Client_Cola();
    }
}

class Client_Bread implements Client {
    public void eating() {
        System.out.println("顾客 吃面包");
    }
}

class Client_Cola implements Client {
    public void eating() {
        System.out.println("顾客 喝可乐");
    }
}

public class FactoryMethodTest {
    public static KFC waiter;
    public static Client client1,client2;
    
    public static KFC MakeChoice(KFC maker){
        if(maker instanceof Bread_Menu){
            return new Bread_Menu(); 
        }else{
            return new Cola_Menu();
        }
    }
    
    public static void main(String[] args){
        
        System.out.println("-------------想吃面包-----------------");
        waiter = MakeChoice(new Bread_Menu());
        client1= waiter.working();
        client1.eating();
        
        System.out.println("-------------想喝可乐-----------------");
        waiter = MakeChoice(new Cola_Menu());
        client2 = waiter.working();
        client2.eating();
    }
}
```

执行如下

```
-------------想吃面包-----------------
顾客 吃面包
-------------想喝可乐-----------------
顾客 喝可乐
```















