[toc]



# 原型模式

## 1.模式意图

由于有些时候，需要在运行时指定对象时哪个类的实例，此时用工厂模式就有些力不从心了。通过原型模式就可以通过拷贝函数clone一个原有的对象，给现在的对象使用，从而创建更多的同类型的对象。

## 2.模式结构

【**简单原型模式**】用于原型的版本不多的时候

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420212919.jpeg)

【**登记模式的原型模式**】如果原型的实现很多种版本，那么通过一个登记管理类，可以方便的实现原型的管理。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420212934.jpeg)

- Prototype 原型接口，定义原型的结构。
- ConcretePrototype 原型的具体实现。
- Client 使用类，创建一个原型，创建一个引用，可以随意指定要引用的实现类。
- PrototypeManager 原型的管理器，里面含有一个Map，用来保存原型的实例对象。

## 3.使用场景

- 当需要在运行时指定对象的实现类时。
- 当一个类的实例只能有集中状态的一种时。

## 4.代码结构

【**简单原型模式**】

```
package com.xingoo.test;

interface Prototype{
    public Object clone();
}
class ConcretePrototype1 implements Prototype{
    public Object clone() {
        Prototype prototype = new ConcretePrototype1();
        return prototype;
    }
}
class ConcretePrototype2 implements Prototype{
    public Object clone(){
        Prototype prototype = new ConcretePrototype2();
        return prototype;
    }
}
public class Client{
    public static void main(String[] args){
        Prototype p1 = new ConcretePrototype1();
        System.out.println("p1 "+p1);

        Prototype p2 = new ConcretePrototype2();
        System.out.println("p2 "+p2);

        Prototype prototype = (Prototype)p1.clone();
        System.out.println("prototype "+prototype);
        prototype = (Prototype)p2.clone();
        System.out.println("prototype "+prototype);
    }
}
```

运行结果

```
p1 com.xingoo.test.ConcretePrototype1@1fb8ee3
p2 com.xingoo.test.ConcretePrototype2@14318bb
prototype com.xingoo.test.ConcretePrototype1@ca0b6
prototype com.xingoo.test.ConcretePrototype2@10b30a7
```

【**登记模式的原型模式**】

```
package com.xingoo.test1;

import java.util.HashMap;
import java.util.Map;
/**
 * 原型的接口
 * @author xingoo
 */
interface Prototype{
    public Prototype clone();
}
/**
 * 具体的实现类1
 * @author xingoo
 *
 */
class ConcretePrototype1 implements Prototype{
    public Prototype clone() {
        Prototype prototype = new ConcretePrototype1();
        return prototype;
    }
}
/**
 * 具体的实现类2
 * @author xingoo
 *
 */
class ConcretePrototype2 implements Prototype{
    public Prototype clone(){
        Prototype prototype = new ConcretePrototype2();
        return prototype;
    }
}
/**
 * 原型的管理器
 * @author xingoo
 *
 */
class PrototypeManager{
    /**
     * 用于保存原型的实例
     */
    private static Map<String,Prototype> map = new HashMap<String,Prototype>();
    /**
     * 静态方法创建构造函数，避免外部类调用
     */
    private PrototypeManager(){
    }
    /**
     * 添加原型
     * @param protoName 原型的名字
     * @param prototype 原型的实例
     */
    public synchronized static void setPrototype(String protoName,Prototype prototype){
        map.put(protoName, prototype);
    }
    /**
     * 获得原型
     * @param protoName 原型的名字
     * @return 返回原型的实例
     * @throws Exception 如果找不到，则跑出找不到异常
     */
    public synchronized static Prototype getPrototype(String protoName) throws Exception{
        Prototype prototype = map.get(protoName);
        if(prototype == null){
            throw new Exception("no "+protoName+" in Manager");
        }
        return prototype;
    }
    /**
     * 从管理器中删除原型的实例
     * @param protoName 原型的名字
     */
    public synchronized static void removedPrototype(String protoName){
        map.remove(protoName);
    }
}
/**
 * 原型的使用者
 * @author xingoo
 *
 */
public class Client {
    public static void main(String[] args){
        try{
            /**
             * 创建一种原型的实现，放入管理器中
             */
            Prototype p1 = new ConcretePrototype1();
            System.out.println("p1 "+p1);
            PrototypeManager.setPrototype("MyPrototype", p1);

            Prototype prototype1 = PrototypeManager.getPrototype("MyPrototype").clone();
            System.out.println("prototype1 "+prototype1);
            /**
             * 切换成另一种原型的实现，修改管理器中的对象
             */
            Prototype p2 = new ConcretePrototype1();
            System.out.println("p2 "+p2);
            PrototypeManager.setPrototype("p1", p2);

            Prototype prototype2 = PrototypeManager.getPrototype("MyPrototype").clone();
            System.out.println("prototype2 "+prototype2);
            /**
             * 注销该原型实现，对象使用后，观察情况
             */
            PrototypeManager.removedPrototype("MyPrototype");

            Prototype prototype3 = PrototypeManager.getPrototype("MyPrototype").clone();
            System.out.println("prototype3 "+prototype3);

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
```

运行结果

```
p1 com.xingoo.test1.ConcretePrototype1@116ab4e
prototype1 com.xingoo.test1.ConcretePrototype1@129f3b5
p2 com.xingoo.test1.ConcretePrototype1@13f3045
prototype2 com.xingoo.test1.ConcretePrototype1@17a29a1
java.lang.Exception: no MyPrototype in Manager
    at com.xingoo.test1.PrototypeManager.getPrototype(Client.java:66)
    at com.xingoo.test1.Client.main(Client.java:109)
```















