[toc]



# 中介者模式

## 1.模式意图

使用一个中介的对象，封装一组对象之间的交互，这样这些对象就可以不用彼此耦合。

这个中介者常常起着**中间桥梁**的作用，使其他的对象可以利用中介者完成某些行为活动，因此它必须对所有的参与活动的对象了如指掌！

## 2.应用场景

- 当一组对象要进行沟通或者业务上的交互，但是其关系却又很复杂混乱时，可以采用此模式。
- 当一个对象与其他的对象要进行紧密的交互，但又想服用该对象而不依赖其他的对象时。
- 想创造一个运行于多个类之间的对象，又不想生成新的子类时。

## 3.模式结构

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210420233018.jpeg)

**Mediator** 抽象的中介者，定义中介的规范

```
interface Mediator{
    public void colleagueChanged(Colleague c);
}
```

**ConcreteMediator** 具体的中介者，通常内部依赖于多个业务对象

```
class ConcreteMediator implements Mediator{
    private Colleague1 col1;
    private Colleague2 col2;
    public void colleagueChanged(Colleague c) {
        col1.action();
        col2.action();
    }
    public void createConcreteMediator() {
        col1 = new Colleague1(this);
        col2 = new Colleague2(this);
    }
    private Colleague1 getCol1() {
        return col1;
    }
    public Colleague2 getCol2() {
        return col2;
    }
}
```

**Colleague** 抽象的业务角色

```
abstract class Colleague{
    private Mediator mediator;
    public Colleague(Mediator mediator){
        this.mediator = mediator;
    }
    public Mediator getMediator() {
        return mediator;
    }
    public abstract void action();
    public void change(){
        mediator.colleagueChanged(this);
    }
}
```

**Colleague1 Colleague2** 具体的业务角色

```
class Colleague1 extends Colleague{
    public Colleague1(Mediator m){
        super(m);
    }
    public void action(){
        System.out.println("this is an action from Colleague1");
    }
}
class Colleague2 extends Colleague{
    public Colleague2(Mediator m){
        super(m);
    }
    public void action(){
        System.out.println("this is an action from Colleague2");
    }
}
```

全部代码

```
package com.xingoo.test.design.mediator;
abstract class Colleague{
    private Mediator mediator;

    public Colleague(Mediator mediator){
        this.mediator = mediator;
    }

    public Mediator getMediator() {
        return mediator;
    }

    public abstract void action();

    public void change(){
        mediator.colleagueChanged(this);
    }
}
class Colleague1 extends Colleague{
    public Colleague1(Mediator m){
        super(m);
    }
    public void action(){
        System.out.println("this is an action from Colleague1");
    }
}
class Colleague2 extends Colleague{
    public Colleague2(Mediator m){
        super(m);
    }
    public void action(){
        System.out.println("this is an action from Colleague2");
    }
}
interface Mediator{
    public void colleagueChanged(Colleague c);
}
class ConcreteMediator implements Mediator{
    private Colleague1 col1;
    private Colleague2 col2;

    public void colleagueChanged(Colleague c) {
        col1.action();
        col2.action();
    }

    public void createConcreteMediator() {
        col1 = new Colleague1(this);
        col2 = new Colleague2(this);
    }

    private Colleague1 getCol1() {
        return col1;
    }

    public Colleague2 getCol2() {
        return col2;
    }

}

public class Client {
    public static void main(String[] args) {
        ConcreteMediator mediator = new ConcreteMediator();
        mediator.createConcreteMediator();
        Colleague1 col1 = new Colleague1(mediator);
//        Colleague2 col2 = new Colleague2(mediator);
        mediator.colleagueChanged(col1);
    }
}
```

运行结果

```
this is an action from Colleague1
this is an action from Colleague2
```

## 4.生活中的设计模式

毕业的同学们，第一个要解决的问题就是租房子，当白富美高富帅出没社会后，穷屌丝没了生存之地。但是只要勤劳，一样有饭吃有房住！

这里房屋中介好比是一个中介者，它知道每个租客的身份信息，当有房屋出租后，它会发送给每一个租客消息。

这样，**租客们中有一个变化活动时，都会利用房屋中介，发送消息到其他的租客**。下面就是模仿的一个过程。

房屋中介代码如下：

```
interface StateMediator{
    public void sell(Tenant tenant);
}
class RealEstateAgents implements StateMediator{
    private TenantA teA;
    private TenantB teB;
    private TenantC teC;

    public void sell(Tenant tenant) {
        System.out.println("海景洋房 已经租出去了！");
        if(tenant instanceof TenantA){
            teB.crying();
            teC.crying();
        }else if(tenant instanceof TenantB){
            teA.crying();
            teC.crying();
        }else if(tenant instanceof TenantC){
            teB.crying();
            teA.crying();
        }
    }

    public void createAgents(){
        teA = new TenantA(this);
        teB = new TenantB(this);
        teC = new TenantC(this);
    }
}
```

租客的代码如下：

```
abstract class Tenant{
    private RealEstateAgents agent;
    public Tenant(RealEstateAgents agent) {
        this.agent = agent;
    }
    public abstract void crying();
    public void renting(){
        agent.sell(this);
    }
}
class TenantA extends Tenant{
    public TenantA(RealEstateAgents agent) {
        super(agent);
    }
    public void crying() {
        System.out.println("我是高富帅 TenantA！哎呀我想要！");
    }
}
class TenantB extends Tenant{
    public TenantB(RealEstateAgents agent) {
        super(agent);
    }
    public void crying() {
        System.out.println("我是白富美 TenantB！哎呀我想要！");
    }
}
class TenantC extends Tenant{
    public TenantC(RealEstateAgents agent) {
        super(agent);
    }
    public void crying() {
        System.out.println("我是穷屌丝 TenantC！哎呀我想要！");
    }
}
```

产生的业务活动如下：

```
public class ClientTest {
    public static void main(String[] args) {
        RealEstateAgents agent = new RealEstateAgents();
        agent.createAgents();

        System.out.println("TeA 抢到了房子了！");
        agent.sell(new TenantA(agent));

        System.out.println("过了两个月 TeB 抢到了房子了！");
        agent.sell(new TenantB(agent));
    }
}
```

运行结果

```
TeA 抢到了房子了！
海景洋房 已经租出去了！
我是白富美 TenantB！哎呀我想要！
我是穷屌丝 TenantC！哎呀我想要！
过了两个月 TeB 抢到了房子了！
海景洋房 已经租出去了！
我是高富帅 TenantA！哎呀我想要！
我是穷屌丝 TenantC！哎呀我想要！
```

