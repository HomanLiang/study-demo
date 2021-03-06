[toc]



# 设计模式简介





## X.案例分析

### X.1.促销活动需求

#### X.1.1.为什么要使用设计模式
因为我们的项目的需求是永远在变的，为了应对这种变化，使得我们的代码能够轻易的实现解耦和拓展。如果能够保证代码一次写好以后都不会再改变了，那可以想怎么写怎么写了。
#### X.1.2.如何判断那里需要使用设计模式
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505214429.png)

在我们实现中，有一些代码是一次写好后续基本不会改变的，或者不太需要扩展的，比如一些工具类等。有一部分是会经常变得，设计模式大多都应用在需求会变化的这一部分。分析这些代码会如何变，选择合适的设计模式来优化这部分代码。

#### X.1.3.需求
为了促进商品的销售，各大电商品台会在平时或者一些节日的时候退出一些促销活动刺激用户消费，活动的类型可能会各不相同，如下：
- 满减，满400减20
- 代金卷，玛莎拉蒂5元代金卷
- 折扣，9折，8折
- 每满减，每满200减10
- 等等

其中有些可以叠加，有些只能单独使用。
#### X.1.4.简单实现
先拿到需求的时候，也不用去想那么多，挽起袖子就是一通操作：
```
public class OrderPromotion {
    public BigDecimal promotion(Order order, int[] promotions){
        for(int promotion:promotions){
            switch (promotion){
                case 1:
                    //计算该类型折扣后的价格
                    break;
                case 2:
                    //计算该类型折扣后的价格
                    break;
                case 3:
                    //计算该类型折扣后的价格
                    break;
                //....
            }
        }
        return order.getResultPrice();
    }
}
```
单从功能实现上来说，上面的代码已经完成了基本功能了。

但是上面的代码也是致命的，虽然看起来很简单，但是那只不过是因为大多数功能都用注释代替了，换成实际代码的话一个方法可能就得上千行。

尤其是当我们需要添加新的促销活动的话就需要在switch中添加新的类型，这对于开发来说简直是灾难，并且维护这些代码也是一个麻烦。

#### X.1.5.优化一：单一职责原则
上面的代码中，promotion(…)方法直接完成了所有的工作，但是咋我们实际实现中最好让一个方法的职责单一，只完成某一个功能，所以这里我们将对折扣类型的判断和计算价格分开：
```
public class OrderPromotion {
    public BigDecimal promotion(Order order, int[] promotions){
        for(int promotion:promotions){
            switch (promotion){
                case 1:
                    calculate1(order);
                    break;
                case 2:
                    calculate2(order);
                    break;
                case 3:
                    calculate3(order);
                    break;
                //more promotion
            }
        }
        return order.getResultPrice();
    }
    public void calculate1(Order order){
        //计算使用折扣一后的价格
    }
    public void calculate2(Order order){
        //计算使用折扣二后的价格
    }
    public void calculate3(Order order){
        //计算使用折扣三后的价格
    }
    //more calculate

}
```
这里我们将折扣类型的判断和计算价格分开，使得promotion(…)方法的代码量大大降低，提升了代码的可读性。
#### X.1.6.优化二：策略模式
上面优化后的代码提升了原有代码的可读性，但是原来OrderPromotion类代码大爆炸的问题还是没有解决。

针对这个问题，我们希望能够将计算的代码和当前代码分离开，首先我们能想到的就是定义一个类，然后将计算的代码复制到这个类中，需要的时候就调用。这样到的确是分离开了，但是完全是治标不治本。在添加新的促销活动是两个类都要改。

所以我们希望能够将不同的促销活动的实现分离开，这样对每一种活动的实现都是分开的，修改也不会影响其他的，基于此我们完全可以选择策略模式来实现。

**策略模式**

策略模式的思想是针对一组算法，将每一种算法都封装到具有共同接口的独立的类中，从而是它们可以相互替换。策略模式的最大特点是使得算法可以在不影响客户端的情况下发生变化，从而改变不同的功能。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210505214459.png)

```
public class OrderPromotion {
    public BigDecimal promotion(Order order, int[] promotions){
        for(int promotion:promotions){
            switch (promotion){
                case 1:
                    new PromotionType1Calculate(order);
                    break;
                case 2:
                    new PromotionType2Calculate(order);
                    break;
                case 3:
                    new PromotionType3Calculate(order);
                    break;
                //more promotion
            }
        }
        return order.getResultPrice();
    }
}
```
上面的代码很明显已经精简很多了，到了现在如果需要添加一个促销活动的话只需定义一个促销类，实现PromotionCalculation接口然后在switch中添加即可
#### X.1.7.优化三：工厂模式
上面的代码虽然已经将促销活动的实现分离开了，但是OrderPromotion还是一直在变得，每一次添加或者下线活动都需要修改该类。
现在我们希望OrderPromotion是不变的，将PromotionCalculation的实例化剥离开来。创建类很明显是使用工厂设计模式了。

**OrderPromotion**

```
public class OrderPromotion {
    public BigDecimal promotion(Order order, int[] promotions){
        for(int promotion:promotions){
            PromotionFactory.getPromotionCalculate(promotion).calculate(order);
        }
        return order.getResultPrice();
    }
}
```
类的创建工作交给工厂来实现。

**PromotionFactory**

```
public class PromotionFactory {
    public static PromotionCalculate getPromotionCalculate(int promotion){
        switch (promotion){
            case 1:
                return new PromotionType1Calculate(order);
            break;
            case 2:
                return new PromotionType2Calculate(order);
            break;
            case 3:
                return new PromotionType3Calculate(order);
            break;
            //more promotion
        }
        return null;
    }
}
```
使用工厂模式后OrderPromotion类就不需要改了，每一次添加新的促销活动后只需要在工厂类中添加即可。
#### X.1.8.优化四：配置+反射
上面的代码还存在的问题在于每一次需要添加新的促销活动的时候还是需要修改工厂类中的代码，这里我们通过配置文件加反射的方式来解决。

**定义映射配置文件 mapping.properties**

```
1=design.order.PromotionType1Calculate
2=design.order.PromotionType2Calculate
3=design.order.PromotionType3Calculate
```
**PromotionFactory**
```
public class PromotionFactory {

    private static Map<Integer, String> mapping = new HashMap<Integer, String>();

    static {
        try {
            Properties pps = new Properties();
            pps.load(new FileInputStream("Test.properties"));
            Iterator<String> iterator = pps.stringPropertyNames().iterator();
            while(iterator.hasNext()){
                String key=iterator.next();
                mapping.put(Integer.valueOf(key), pps.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PromotionCalculate getPromotionCalculate(int promotion) throws Exception {
        if(mapping.containsKey(promotion)){
            String beanName = mapping.get(promotion);
            return Class.forName(beanName).newInstance();
        }
        return null;
    }
}
```
通过上面的代码就可以实现不改变已有代码的前提下实现对功能的灵活扩展。当然，这里的代码只是作为演示用的，实际上可以改进的地方还有不少，像最后反射效率较低，也可以通过其他的方式来实现。
#### X.1.9.小结
设计模式是我们一定要了解的东西，熟悉设计模式能让我们设计出易于扩展和维护的代码结构。但是并不是任何地方都需要上设计模式，应该结合我们的项目实际进行分析是否需要设计模式，使用哪种设计模式。