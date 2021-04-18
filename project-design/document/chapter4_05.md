[toc]



# 分布式事务 -  TCC

## 1.基于电商交易流程，图解TCC事务分段提交

### 1.1.场景案例简介

#### 1.1.1.场景描述

分布式事务在业务系统中是十分常见的，最经典的场景就是电商架构中的交易业务，如图：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210759.png)

客户端通过请求订单服务，执行下单操作，实际上从订单服务上又触发了多个服务链请求，基本步骤如下：

- 客户端请求在订单服务上创建订单；
- 订单服务调用账户服务扣款；
- 订单服务调用库存服务执行库存扣减；
- 订单通过物流服务，转化为物流运单；

这套流程在电商系统中是基本业务，在实际的开发中远比这里描述的复杂。

#### 1.1.2.服务时序图

上述1中是业务性的流程概念描述，从系统开发层面，在微服务的架构模式下，通常的时序流如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210837.png)

这样服务间的通信时序图在程序设计中十分常见，在分布式系统中，清楚的描述各个服务间的通信流程是十分关键的。

上图描述的交易流程是在最理想的状态下，各个服务都执行成功，但是程序是不能100%保证一直正常，经常出现如下情况：

- 服务间通信失败；
- 单个节点服务宕掉；
- 服务接口执行失败；

这些都是实际开发中经常出现的问题，比如订单创建成功，扣款成功，但是库存扣减失败，物流运单生成，那么这笔订单该如何处理？这就是分布式事务要解决的核心问题。

分布式事务机制要保证不同服务之间形成一个整体性的可控的事务，业务流程上的服务除非全部成功，否则任何服务的操作失败，都会导致所有服务上操作回滚，撤销已经完成的动作。

### 1.2.TCC基础概念

#### 1.2.1.分段提交协议

XA是一个分布式事务协议，大致分为两部分：事务管理器和本地资源管理器，本地资源管理器基本由数据库实现，大多数关系型数据库都实现XA接口，而事务管理器作为全局事务的调度者，负责整个事务中本地资源的提交和回滚，基本原理如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210842.png)

**阶段1：事务询问**

事务管理器向所有的参与事务的资源管理器发送确认请求，询问是否可以执行事务提交操作，并等待各参与者的响应，如果执事务操作成功，就反馈给事务管理器表示事务可以执行，如果没有成功执行事务，就反馈事务不可以执行；

**阶段2：事务提交**

XA根据第一阶段每个资源管理器是否都准备提交成功，判断是要事务整体提交还是回滚，正式执行事务提交操作，并在完成提交之后释放整个事务占用的资源；事务也会存在失败情况，导致流程取消回滚；

XA事务具有强一致性，在两阶段提交的整个过程中，一直会持有资源的锁，性能不理想的缺点很明显，特别是在交易下单链路中，往往并发量很高，XA无法满足该类高并发场景。

#### 1.2.2.TCC概念简介

Try(预处理)-Confirm(确认)-Cancel(取消)模式的简称TCC。

**Try阶段**

业务检查(一致性)及资源预留(隔离)，该阶段是一个初步操作，提交事务前的检查及预留业务资源完成；例如购票系统中的占位成功，需要在15分钟内支付；

**Confirm阶段**

确认执行业务操作，不在执行任何业务检查，基于Try阶段预留的业务资源，从理想状态下看只要Try成功，Confirm也会成功，因为资源的检查和锁定都已经成功；该阶段出现问题，需要重试机制或者手动处理；购票系统中的占位成功并且15分钟内支付完成，购票成功；

**Cancel阶段**

Cancel阶段是在业务执行错误需要回滚到状态下执行分支事务的取消，预留资源的释放；购票系统中的占位成功但是15分钟内没有支付，取消占位；

#### 1.2.3.TCC对比XA

XA事务的强一致性，导致资源层的锁定；

TCC在业务层面追求最终一致性，不会长久占用资源；

### 1.3.分段事务分析

现在回到模块一中的场景案例，在理想状态下流程全部成功是好的，但实际情况是突发情况很多，基于TCC模式分析上述电商的具体业务：

#### 1.3.1.资源预留

在TCC模式下，通常表字段的状态设计思路为：订单(支付中.已支付.取消订单)，账户(金额.冻结金额)，库存(库存.冻结库存)，物流(出库中.已出库，已撤回)，这种状态管理在开发中非常常见。

所以在TCC模式里通常会如下处理资源预留：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210848.png)

假设订单总额为：200，状态：支付中，则此时资源预留情况如下：

- tc_account账户表：tc_total=1000，tc_ice=200，总金额1000，冻结200；
- tc_inventory库存表：tc_total=100，tc_ice=20，总库存100件，冻结20件；
- tc_waybill运单表：tc_state=1，运单状态，出库中；

这样下单链路上的相关资源已检查并且预留成功；

#### 1.3.2.资源提交确认

资源预留成功之后，执行资源提交执行：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210905.png)

- tc_account账户表：tc_total=800，tc_ice=0，即订单扣款成功；
- tc_inventory库存表：tc_total=80，tc_ice=0，库存消减成功；
- tc_waybill运单表：tc_state=2，运单状态，已出库；

这样下单链路上的相关资源已全部提交处理成功，这是最理想的状态；

#### 1.3.3.失败回滚

整个过程是可能执行失败的，或者用户直接自己发起回退，则要回滚整个链路上的数据：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210900.png)

- tc_account账户表：tc_total=1000，tc_ice=0，取消账户冻结的200；
- tc_inventory库存表：tc_total=100，tc_ice=0，取消库存冻结的20件；
- tc_waybill运单表：tc_state=3，运单状态，已撤回；

这样下单链路上的相关数据都基于该笔订单做回退操作，恢复；

#### 1.3.4.补偿机制

整个电商交易流程，不管是成功，还是完整的回退失败，都是需要在理想状态下，要求整个服务链路和数据是绝对正常的才行。但是在实际分布式架构下是很难保证的，所以在产品的设计上会预留很多操作入口，用来手动做事务补偿或回退操作：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418210854.png)

大型复杂的业务系统中，直接修改数据库通常情况下是不允许的，一般核心流程会预留各种操作入口，用来处理突发状况，弥补数据的完整性，例如交易链路上，只要扣款成功，后续的数据无论如何都会补上，是不允许回滚的，当然如果没有扣款成功，订单有效期结束，该笔交易也就算做结束。

## 2.TCC分布式事务实现原理

### 2.1业务场景介绍

咱们先来看看业务场景，假设你现在有一个电商系统，里面有一个支付订单的场景。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211933.png)

那对一个订单支付之后，我们需要做下面的步骤：

- 更改订单的状态为“已支付”
- 扣减商品库存
- 给会员增加积分
- 创建销售出库单通知仓库发货

这是一系列比较真实的步骤，无论大家有没有做过电商系统，应该都能理解。

### 2.2.进一步思考

好，业务场景有了，现在我们要更进一步，实现一个 TCC 分布式事务的效果。

什么意思呢？也就是说，[1] 订单服务-修改订单状态，[2] 库存服务-扣减库存，[3] 积分服务-增加积分，[4] 仓储服务-创建销售出库单。

上述这几个步骤，要么一起成功，要么一起失败，必须是一个整体性的事务。

举个例子，现在订单的状态都修改为“已支付”了，结果库存服务扣减库存失败。那个商品的库存原来是 100 件，现在卖掉了 2 件，本来应该是 98 件了。

结果呢？由于库存服务操作数据库异常，导致库存数量还是 100。这不是在坑人么，当然不能允许这种情况发生了！

但是如果你不用 TCC 分布式事务方案的话，就用个 Spring Cloud 开发这么一个微服务系统，很有可能会干出这种事儿来。

我们来看看下面的这个图，直观的表达了上述的过程：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211921.png)

所以说，我们有必要使用 TCC 分布式事务机制来保证各个服务形成一个整体性的事务。

上面那几个步骤，要么全部成功，如果任何一个服务的操作失败了，就全部一起回滚，撤销已经完成的操作。

比如说库存服务要是扣减库存失败了，那么订单服务就得撤销那个修改订单状态的操作，然后得停止执行增加积分和通知出库两个操作。

说了那么多，老规矩，给大家上一张图，大伙儿顺着图来直观的感受一下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211900.png)

### 2.3.落地实现 TCC 分布式事务

那么现在到底要如何来实现一个 TCC 分布式事务，使得各个服务，要么一起成功？要么一起失败呢？

大家稍安勿躁，我们这就来一步一步的分析一下。咱们就以一个 Spring Cloud 开发系统作为背景来解释。

#### 2.3.1.TCC 实现阶段一：Try

首先，订单服务那儿，它的代码大致来说应该是这样子的：

```java
public class OrderService {

    // 库存服务
    @Autowired
    private InventoryService inventoryService;

    // 积分服务
    @Autowired
    private CreditService creditService;

    // 仓储服务
    @Autowired
    private WmsService wmsService;

    // 对这个订单完成支付
    public void pay(){
        //对本地的的订单数据库修改订单状态为"已支付"
        orderDAO.updateStatus(OrderStatus.PAYED);

        //调用库存服务扣减库存
        inventoryService.reduceStock();

        //调用积分服务增加积分
        creditService.addCredit();

        //调用仓储服务通知发货
        wmsService.saleDelivery();
    }
}
```

如果你之前看过 Spring Cloud 架构原理那篇文章，同时对 Spring Cloud 有一定的了解的话，应该是可以理解上面那段代码的。

其实就是订单服务完成本地数据库操作之后，通过 Spring Cloud 的 Feign 来调用其他的各个服务罢了。

但是光是凭借这段代码，是不足以实现 TCC 分布式事务的啊？！兄弟们，别着急，我们对这个订单服务修改点儿代码好不好。

首先，上面那个订单服务先把自己的状态修改为：OrderStatus.UPDATING。

这是啥意思呢？也就是说，在 pay() 那个方法里，你别直接把订单状态修改为已支付啊！你先把订单状态修改为 UPDATING，也就是修改中的意思。

这个状态是个没有任何含义的这么一个状态，代表有人正在修改这个状态罢了。

然后呢，库存服务直接提供的那个 reduceStock() 接口里，也别直接扣减库存啊，你可以是冻结掉库存。

举个例子，本来你的库存数量是 100，你别直接 100 - 2 = 98，扣减这个库存！

你可以把可销售的库存：100 - 2 = 98，设置为 98 没问题，然后在一个单独的冻结库存的字段里，设置一个 2。也就是说，有 2 个库存是给冻结了。

积分服务的 addCredit() 接口也是同理，别直接给用户增加会员积分。你可以先在积分表里的一个预增加积分字段加入积分。

比如：用户积分原本是 1190，现在要增加 10 个积分，别直接 1190 + 10 = 1200 个积分啊！

你可以保持积分为 1190 不变，在一个预增加字段里，比如说 prepare_add_credit 字段，设置一个 10，表示有 10 个积分准备增加。

仓储服务的 saleDelivery() 接口也是同理啊，你可以先创建一个销售出库单，但是这个销售出库单的状态是“UNKNOWN”。

也就是说，刚刚创建这个销售出库单，此时还不确定它的状态是什么呢！

上面这套改造接口的过程，其实就是所谓的 TCC 分布式事务中的第一个 T 字母代表的阶段，也就是 Try 阶段。

总结上述过程，如果你要实现一个 TCC 分布式事务，首先你的业务的主流程以及各个接口提供的业务含义，不是说直接完成那个业务操作，而是完成一个 Try 的操作。

这个操作，一般都是锁定某个资源，设置一个预备类的状态，冻结部分数据，等等，大概都是这类操作。

咱们来一起看看下面这张图，结合上面的文字，再来捋一捋整个过程：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211843.png)

#### 2.3.2.TCC 实现阶段二：Confirm

然后就分成两种情况了，第一种情况是比较理想的，那就是各个服务执行自己的那个 Try 操作，都执行成功了，Bingo！

这个时候，就需要依靠 TCC 分布式事务框架来推动后续的执行了。这里简单提一句，如果你要玩儿 TCC 分布式事务，必须引入一款 TCC 分布式事务框架，比如国内开源的 ByteTCC、Himly、TCC-transaction。

否则的话，感知各个阶段的执行情况以及推进执行下一个阶段的这些事情，不太可能自己手写实现，太复杂了。

如果你在各个服务里引入了一个 TCC 分布式事务的框架，订单服务里内嵌的那个 TCC 分布式事务框架可以感知到，各个服务的 Try 操作都成功了。

此时，TCC 分布式事务框架会控制进入 TCC 下一个阶段，第一个 C 阶段，也就是 Confirm 阶段。

为了实现这个阶段，你需要在各个服务里再加入一些代码。比如说，订单服务里，你可以加入一个 Confirm 的逻辑，就是正式把订单的状态设置为“已支付”了，大概是类似下面这样子：

```java
public class OrderServiceConfirm {

    public void pay(){
        orderDao.updateStatus(OrderStatus.PAYED);
    }
}
```

库存服务也是类似的，你可以有一个 InventoryServiceConfirm 类，里面提供一个 reduceStock() 接口的 Confirm 逻辑，这里就是将之前冻结库存字段的 2 个库存扣掉变为 0。

这样的话，可销售库存之前就已经变为 98 了，现在冻结的 2 个库存也没了，那就正式完成了库存的扣减。

积分服务也是类似的，可以在积分服务里提供一个 CreditServiceConfirm 类，里面有一个 addCredit() 接口的 Confirm 逻辑，就是将预增加字段的 10 个积分扣掉，然后加入实际的会员积分字段中，从 1190 变为 1120。

仓储服务也是类似，可以在仓储服务中提供一个 WmsServiceConfirm 类，提供一个 saleDelivery() 接口的 Confirm 逻辑，将销售出库单的状态正式修改为“已创建”，可以供仓储管理人员查看和使用，而不是停留在之前的中间状态“UNKNOWN”了。

好了，上面各种服务的 Confirm 的逻辑都实现好了，一旦订单服务里面的 TCC 分布式事务框架感知到各个服务的 Try 阶段都成功了以后，就会执行各个服务的 Confirm 逻辑。

订单服务内的 TCC 事务框架会负责跟其他各个服务内的 TCC 事务框架进行通信，依次调用各个服务的 Confirm 逻辑。然后，正式完成各个服务的所有业务逻辑的执行。

同样，给大家来一张图，顺着图一起来看看整个过程：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211820.png)

#### 2.3.3.TCC 实现阶段三：Cancel

好，这是比较正常的一种情况，那如果是异常的一种情况呢？

举个例子：在 Try 阶段，比如积分服务吧，它执行出错了，此时会怎么样？

那订单服务内的 TCC 事务框架是可以感知到的，然后它会决定对整个 TCC 分布式事务进行回滚。

也就是说，会执行各个服务的第二个 C 阶段，Cancel 阶段。同样，为了实现这个 Cancel 阶段，各个服务还得加一些代码。

首先订单服务，它得提供一个 OrderServiceCancel 的类，在里面有一个 pay() 接口的 Cancel 逻辑，就是可以将订单的状态设置为“CANCELED”，也就是这个订单的状态是已取消。

库存服务也是同理，可以提供 reduceStock() 的 Cancel 逻辑，就是将冻结库存扣减掉 2，加回到可销售库存里去，98 + 2 = 100。

积分服务也需要提供 addCredit() 接口的 Cancel 逻辑，将预增加积分字段的 10 个积分扣减掉。

仓储服务也需要提供一个 saleDelivery() 接口的 Cancel 逻辑，将销售出库单的状态修改为“CANCELED”设置为已取消。

然后这个时候，订单服务的 TCC 分布式事务框架只要感知到了任何一个服务的 Try 逻辑失败了，就会跟各个服务内的 TCC 分布式事务框架进行通信，然后调用各个服务的 Cancel 逻辑。

大家看看下面的图，直观的感受一下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211711.png)

#### 2.3.4.总结与思考

好了，兄弟们，聊到这儿，基本上大家应该都知道 TCC 分布式事务具体是怎么回事了！

总结一下，你要玩儿 TCC 分布式事务的话：首先需要选择某种 TCC 分布式事务框架，各个服务里就会有这个 TCC 分布式事务框架在运行。

然后你原本的一个接口，要改造为 3 个逻辑，Try-Confirm-Cancel：

- 先是服务调用链路依次执行 Try 逻辑。
- 如果都正常的话，TCC 分布式事务框架推进执行 Confirm 逻辑，完成整个事务。
- 如果某个服务的 Try 逻辑有问题，TCC 分布式事务框架感知到之后就会推进执行各个服务的 Cancel 逻辑，撤销之前执行的各种操作。

这就是所谓的 TCC 分布式事务。TCC 分布式事务的核心思想，说白了，就是当遇到下面这些情况时：

- 某个服务的数据库宕机了。
- 某个服务自己挂了。
- 那个服务的 Redis、Elasticsearch、MQ 等基础设施故障了。
- 某些资源不足了，比如说库存不够这些。

先来 Try 一下，不要把业务逻辑完成，先试试看，看各个服务能不能基本正常运转，能不能先冻结我需要的资源。

如果 Try 都 OK，也就是说，底层的数据库、Redis、Elasticsearch、MQ 都是可以写入数据的，并且你保留好了需要使用的一些资源（比如冻结了一部分库存）。

接着，再执行各个服务的 Confirm 逻辑，基本上 Confirm 就可以很大概率保证一个分布式事务的完成了。

那如果 Try 阶段某个服务就失败了，比如说底层的数据库挂了，或者 Redis 挂了，等等。

此时就自动执行各个服务的 Cancel 逻辑，把之前的 Try 逻辑都回滚，所有服务都不要执行任何设计的业务逻辑。保证大家要么一起成功，要么一起失败。

等一等，你有没有想到一个问题？如果有一些意外的情况发生了，比如说订单服务突然挂了，然后再次重启，TCC 分布式事务框架是如何保证之前没执行完的分布式事务继续执行的呢？

所以，TCC 事务框架都是要记录一些分布式事务的活动日志的，可以在磁盘上的日志文件里记录，也可以在数据库里记录。保存下来分布式事务运行的各个阶段和状态。

问题还没完，万一某个服务的 Cancel 或者 Confirm 逻辑执行一直失败怎么办呢？

那也很简单，TCC 事务框架会通过活动日志记录各个服务的状态。举个例子，比如发现某个服务的 Cancel 或者 Confirm 一直没成功，会不停的重试调用它的 Cancel 或者 Confirm 逻辑，务必要它成功！

当然了，如果你的代码没有写什么 Bug，有充足的测试，而且 Try 阶段都基本尝试了一下，那么其实一般 Confirm、Cancel 都是可以成功的！

最后，再给大家来一张图，来看看给我们的业务，加上分布式事务之后的整个执行流程：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418211646.png)

不少大公司里，其实都是自己研发 TCC 分布式事务框架的，专门在公司内部使用，比如我们就是这样。

不过如果自己公司没有研发 TCC 分布式事务框架的话，那一般就会选用开源的框架。

这里笔者给大家推荐几个比较不错的框架，都是咱们国内自己开源出去的：ByteTCC，TCC-transaction，Himly。

大家有兴趣的可以去它们的 GitHub 地址，学习一下如何使用，以及如何跟 Spring Cloud、Dubbo 等服务框架整合使用。

只要把那些框架整合到你的系统里，很容易就可以实现上面那种奇妙的 TCC 分布式事务的效果了。

## 3.跑通分布式事务框架tcc-transaction的示例项目

### 3.1.背景

前段时间在看项目代码的时候，发现有些接口的流程比较长，在各个服务里面都有通过数据库事务保证数据的一致性，但是在上游的controller层并没有对一致性做保证。

网上查了下，还没找到基于Go开源的比较成熟的分布式事务框架。

于是，准备看看之前隔壁部门大佬写的tcc-transaction，这是一个基于tcc思想实现的分布式事务框架。

tcc分别代码Try，Confirm和Cancel。

Try: 尝试执行业务

- 完成所有业务检查（一致性）
- 预留必须业务资源（准隔离性）

Confirm: 确认执行业务

- 真正执行业务
- 不作任何业务检查
- 只使用Try阶段预留的业务资源
- Confirm操作满足幂等性

Cancel: 取消执行业务

- 释放Try阶段预留的业务资源
- Cancel操作满足幂等性

要了解其实现原理，第一步就是跑通项目自带的示例，即tcc-transaction-tutorial-sample部分的代码。

今天主要介绍在跑通tcc-transaction-tutorial-sample过程中遇到的各种坑。

### 3.2.依赖环境

- Java
- Maven
- Git
- MySQL
- Redis
- Zookeeper
- Intellij IDEA

源码地址：https://github.com/changmingxie/tcc-transaction

### 3.3.踩坑历程

**踩坑准备**

**第一步：克隆代码**

使用"git clone https://github.com/changmingxie/tcc-transaction"命令下载代码

**第二步：导入代码并执行数据库脚本**

代码导入Intellij IDEA。

执行 `tcc-transaction-http-sample/src/main/dbscripts` 下的数据库脚本。

**第三步：修改配置文件**

主要修改的是数据库配置参数。拿 `tcc-transaction-dubbo-sample` 举例，需要修改的文件有

- tcc-transaction/tcc-transaction-tutorial-sample/tcc-transaction-dubbo-sample/tcc-transaction-dubbo-capital/src/main/resources/tccjdbc.properties

- tcc-transaction/tcc-transaction-tutorial-sample/tcc-transaction-dubbo-sample/tcc-transaction-dubbo-redpacket/src/main/resources/tccjdbc.properties

- tcc-transaction/tcc-transaction-tutorial-sample/tcc-transaction-dubbo-sample/tcc-transaction-dubbo-order/src/main/resources/tccjdbc.properties

三个文件修改后对应配置如下

```
# 根据具体的MySQL版本使用驱动名称
jdbc.driverClassName=com.mysql.cj.jdbc.Driver
# 换成你连接数据库的地址
tcc.jdbc.url=jdbc:mysql://127.0.0.1:3306/TCC?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false
# 换成你需要配置数据库的用户名
jdbc.username=root
# 换成你需要配置数据库的密码
jdbc.password=rootroot

c3p0.initialPoolSize=10
c3p0.minPoolSize=10
c3p0.maxPoolSize=30
c3p0.acquireIncrement=3
c3p0.maxIdleTime=1800
c3p0.checkoutTimeout=30000 
```

同时修改 `tcc-transaction-sample-capital`、`tcc-transaction-sample-redpacket` 和 `tcc-transaction-sample-order` 三个项目中jdbc.proerties文件的数据库连接，修改后配置如下

```
jdbc.driverClassName=com.mysql.jdbc.Driver
jdbc.url=jdbc:mysql://127.0.0.1:3306/TCC_CAP?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false
tcc.jdbc.url=jdbc:mysql://127.0.0.1:3306/TCC?useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&useSSL=false
jdbc.username=root
jdbc.password=rootroot
 
c3p0.initialPoolSize=10
c3p0.minPoolSize=10
c3p0.maxPoolSize=30
c3p0.acquireIncrement=3
c3p0.maxIdleTime=1800
c3p0.checkoutTimeout=30000
```

**第四步：启动项目**

结合项目的README.md文件以及网上的文章了解到如果要跑通示例项目，需要分别启动三个项目。

tcc-transaction提供了两个版本：

- 基于dubbo通讯的示例版本
- 基于http通讯的示例版本

这两个版本对于的三个项目分别是

- tcc-transaction-dubbo-capital（账户资产服务）、 tcc-transaction-dubbo-redpacket（红包服务）、 tcc-transaction-dubbo-order（交易订单服务）
- tcc-transaction-http-capital（账户资产服务）、 tcc-transaction-http-redpacket（红包服务）、 tcc-transaction-http-order（交易订单服务）

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214522.png)

其实这两个版本我都跑过，最终成功跑通的只有基于dubbo通讯的示例版本（http版本在最后confirm的时候最是失败，导致最终订单状态为unkown）。

**以基于dubbo通讯的示例为例**

tcc-transaction-dubbo-capital的启动配置如下

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214544.png)

tcc-transaction-dubbo-redpacket的启动配置如下

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214553.png)

tcc-transaction-dubbo-order的启动配置如下

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214600.png)

**坑1：连接不上zk**

启动tcc-transaction-dubbo-capital项目，报错信息如下

```
[sample-dubbo-capital]2019-08-31 17:48:05,312 INFO [org.apache.zookeeper.ZooKeeper] Initiating client connection, connectString=127.0.0.1:2181 sessionTimeout=30000 watcher=org.I0Itec.zkclient.ZkClient@32c7bb63
[sample-dubbo-capital]2019-08-31 17:48:05,334 INFO [org.apache.zookeeper.ClientCnxn] Opening socket connection to server 127.0.0.1/127.0.0.1:2181. Will not attempt to authenticate using SASL (unknown error)
[sample-dubbo-capital]2019-08-31 17:48:05,344 WARN [org.apache.zookeeper.ClientCnxn] Session 0x0 for server null, unexpected error, closing socket connection and attempting reconnect
java.net.ConnectException: Connection refused
at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717)
at org.apache.zookeeper.ClientCnxnSocketNIO.doTransport(ClientCnxnSocketNIO.java:361)
at org.apache.zookeeper.ClientCnxn$SendThread.run(ClientCnxn.java:1081)
[sample-dubbo-capital]2019-08-31 17:48:06,456 INFO [org.apache.zookeeper.ClientCnxn] Opening socket connection to server 127.0.0.1/127.0.0.1:2181. Will not attempt to authenticate using SASL (unknown error)
[sample-dubbo-capital]2019-08-31 17:48:06,459 WARN [org.apache.zookeeper.ClientCnxn] Session 0x0 for server null, unexpected error, closing socket connection and attempting reconnect
java.net.ConnectException: Connection refused
at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:717)
at org.apache.zookeeper.ClientCnxnSocketNIO.doTransport(ClientCnxnSocketNIO.java:361)
at org.apache.zookeeper.ClientCnxn$SendThread.run(ClientCnxn.java:1081)
[sample-dubbo-capital]2019-08-31 17:48:07,566 INFO [org.apache.zookeeper.ClientCnxn] Opening socket connection to server 127.0.0.1/127.0.0.1:2181. Will not attempt to authenticate using SASL (unknown error)
[sample-dubbo-capital]2019-08-31 17:48:07,567 WARN [org.apache.zookeeper.ClientCnxn] Session 0x0 for server null, unexpected error, closing socket connection and attempting reconnect
java.net.ConnectException: Connection refused
```

从报错信息，一眼就看出是连不上zk即zookeeper。

这个很好理解，因为本地没有安装zk，于是安装并通过"./zkServer.sh start"启动zk

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214627.png)

**坑2：redis连不上**

启动tcc-transaction-dubbo-order报错部分信息如下：

```
Caused by: org.springframework.beans.factory.BeanCreationException: Could not autowire field: org.mengyun.tcctransaction.sample.dubbo.order.service.PlaceOrderServiceImpl org.mengyun.tcctransaction.sample.dubbo.order.web.controller.OrderController.placeOrderService; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'placeOrderServiceImpl': Injection of autowired dependencies failed; nested exception is org.springframework.beans.factory.BeanCreationException: Could not autowire field: org.mengyun.tcctransaction.sample.dubbo.order.service.PaymentServiceImpl org.mengyun.tcctransaction.sample.dubbo.order.service.PlaceOrderServiceImpl.paymentService; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'paymentServiceImpl': Injection of autowired dependencies failed; nested exception is org.springframework.beans.factory.BeanCreationException: Could not autowire field: org.mengyun.tcctransaction.sample.dubbo.capital.api.CapitalTradeOrderService org.mengyun.tcctransaction.sample.dubbo.order.service.PaymentServiceImpl.capitalTradeOrderService; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'captialTradeOrderService': Post-processing of FactoryBean's singleton object failed; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'compensableTransactionAspect' defined in class path resource [tcc-transaction.xml]: Cannot resolve reference to bean 'transactionConfigurator' while setting bean property 'transactionConfigurator'; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'transactionConfigurator': Injection of autowired dependencies failed; nested exception is org.springframework.beans.factory.BeanCreationException: Could not autowire field: private org.mengyun.tcctransaction.TransactionRepository org.mengyun.tcctransaction.spring.support.SpringTransactionConfigurator.transactionRepository; nested exception is org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'transactionRepository' defined in file [/Users/jackie/workspace/tcc-transaction/tcc-transaction-tutorial-sample/tcc-transaction-dubbo-sample/tcc-transaction-dubbo-order/target/tcc-transaction-dubbo-order-1.2.6/WEB-INF/classes/config/spring/local/appcontext-service-tcc.xml]: Error setting property values; nested exception is org.springframework.beans.PropertyBatchUpdateException; nested PropertyAccessExceptions (1) are:
PropertyAccessException 1: org.springframework.beans.MethodInvocationException: Property 'jedisPool' threw exception; nested exception is redis.clients.jedis.exceptions.JedisConnectionException: Could not get a resource from the pool
at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor$AutowiredFieldElement.inject(AutowiredAnnotationBeanPostProcessor.java:526)
at org.springframework.beans.factory.annotation.InjectionMetadata.inject(InjectionMetadata.java:87)
at org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor.postProcessPropertyValues(AutowiredAnnotationBeanPostProcessor.java:295)
... 60 more
```

这个和上面的原因类似，本地没有安装redis，导致无法拿到redis连接。

于是安装redis，并使用"redis-server"启动redis。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214650.png)

**坑3：Cause: org.springframework.jdbc.CannotGetJdbcConnectionException: Could not get JDBC Connection**

三个项目都启动后，可以看到一个商品链接列表页，但是在点击链接后无法跳转，并且报错如下

```
Type Exception Report
Message Request processing failed; nested exception is org.mybatis.spring.MyBatisSystemException: nested exception is org.apache.ibatis.exceptions.PersistenceException:
Description The server encountered an unexpected condition that prevented it from fulfilling the request.
Exception
org.springframework.web.util.NestedServletException: Request processing failed; nested exception is org.mybatis.spring.MyBatisSystemException: nested exception is org.apache.ibatis.exceptions.PersistenceException:
### Error querying database. Cause: org.springframework.jdbc.CannotGetJdbcConnectionException: Could not get JDBC Connection; nested exception is java.sql.SQLException: An attempt by a client to checkout a Connection has timed out.
### The error may exist in URL [jar:file:/Users/jackie/workspace/tcc-transaction/tcc-transaction-tutorial-sample/tcc-transaction-http-sample/tcc-transaction-http-order/target/tcc-transaction-http-order-1.2.6/WEB-INF/lib/tcc-transaction-sample-order-1.2.6.jar!/config/sqlmap/main/sample-product.xml]
### The error may involve org.mengyun.tcctransaction.sample.order.infrastructure.dao.ProductDao.findByShopId
### The error occurred while executing a query
### Cause: org.springframework.jdbc.CannotGetJdbcConnectionException: Could not get JDBC Connection; nested exception is java.sql.SQLException: An attempt by a client to checkout a Connection has timed out.
org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:965)
org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:844)
javax.servlet.http.HttpServlet.service(HttpServlet.java:634)
org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:829)
javax.servlet.http.HttpServlet.service(HttpServlet.java:741)
org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)
org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:88)
org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:106)
 
Root Cause
org.mybatis.spring.MyBatisSystemException: nested exception is org.apache.ibatis.exceptions.PersistenceException:
### Error querying database. Cause: org.springframework.jdbc.CannotGetJdbcConnectionException: Could not get JDBC Connection; nested exception is java.sql.SQLException: An attempt by a client to checkout a Connection has timed out.
### The error may exist in URL [jar:file:/Users/jackie/workspace/tcc-transaction/tcc-transaction-tutorial-sample/tcc-transaction-http-sample/tcc-transaction-http-order/target/tcc-transaction-http-order-1.2.6/WEB-INF/lib/tcc-transaction-sample-order-1.2.6.jar!/config/sqlmap/main/sample-product.xml]
```

根据错误信息，排查是MySQL版本和数据库驱动版本不匹配。

本地的MySQL版本是"8.0.11 MySQL Community Server - GPL"，但是tcc-transaction中对应的驱动版本是

```
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.33</version>
</dependency>
```

改为

```
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.11</version>
</dependency>
```

同时针对高版本，需要在连接的jdbc-url后面加上useSSL=false

**坑4：Loading class `com.mysql.jdbc.Driver'. This is deprecated**

启动tcc-transaction-dubbo-redpacket时，在日志中看到一个警告"Loading class `com.mysql.jdbc.Driver'. This is deprecated"。

通过搜索，发现是因为数据库驱动com.mysql.jdbc.Driver'已经被弃用了，需要使用com.mysql.cj.jdbc.Driver，于是修改jdbc.proerties的配置（具体配置见上面），启动正常。

踩完上面的坑后，启动三个项目，完整走完流程，实现了一个基于分布式事务的商品购买行为，具体过程如下图所示

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418214744.gif) 

### 3.4.总结

运行示例项目的过程不算太顺利，主要有一下几个原因吧

- 本地环境配置和项目提供的不一致，导致走了很多弯路，比如MySQL的版本。
- 缺少详细的跑示例项目的文档说明。
- 网上提供的资料比较粗略，也比较陈旧，文中能跑起来的步骤说明已经不适用现在的代码了。

## 4.分布式系统「补偿」机制

### 4.1.「补偿」机制的意义？

以电商的购物场景为例：

客户端 ---->购物车微服务 ---->订单微服务 ----> 支付微服务。

这种调用链非常普遍。

那么为什么需要考虑补偿机制呢？

正如之前几篇文章所说，一次跨机器的通信可能会经过 DNS 服务，网卡、交换机、路由器、负载均衡等设备，这些设备都不一定是一直稳定的，在数据传输的整个过程中，只要任意一个环节出错，都会导致问题的产生。

而在分布式场景中，一个完整的业务又是由多次跨机器通信组成的，所以产生问题的概率成倍数增加。

但是，这些问题并不完全代表真正的系统无法处理请求，所以我们应当尽可能的自动消化掉这些异常。

可能你会问，之前也看到过「补偿」和「事务补偿」或者「重试」，它们之间的关系是什么？

你其实可以不用太纠结这些名字，从目的来说都是一样的。**就是一旦某个操作发生了异常，如何通过内部机制将这个异常产生的「不一致」状态消除掉**。

**题外话：**在 Z 哥看来，不管用什么方式，只要通过额外的方式解决了问题都可以理解为是「补偿」，所以「事务补偿」和「重试」都是「补偿」的子集。前者是一个逆向操作，而后者则是一个正向操作。

只是从结果来看，两者的意义不同。「事务补偿」意味着“放弃”，当前操作必然会失败。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418235426.jpeg)

**▲事务补偿**

「重试」则还有处理成功的机会。这两种方式分别适用于不同的场景。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418235437.jpeg)

**▲重试**

因为「补偿」已经是一个额外流程了，既然能够走这个额外流程，说明时效性并不是第一考虑的因素，所以**做补偿的核心要点是：宁可慢，不可错**。

因此，不要草率的就确定了补偿的实施方案，需要谨慎的评估。虽说错误无法 100%避免，但是抱着这样的一个心态或多或少可以减少一些错误的发生。

### 4.2.「补偿」该怎么做？

做「补偿」的主流方式就前面提到的「事务补偿」和「重试」，以下会被称作「回滚」和「重试」。

我们先来聊聊「回滚」。相比「重试」，它逻辑上更简单一些。

#### 4.2.1.「回滚」

Z 哥将回滚分为 2 种模式，一种叫「显式回滚」（调用逆向接口），一种叫「隐式回滚」（无需调用逆向接口）。

最常见的就是「显式回滚」。这个方案无非就是做 2 个事情：

首先要确定失败的步骤和状态，从而确定需要回滚的范围。一个业务的流程，往往在设计之初就制定好了，所以确定回滚的范围比较容易。但这里唯一需要注意的一点就是：**如果在一个业务处理中涉及到的服务并不是都提供了「回滚接口」，那么在编排服务时应该把提供「回滚接口」的服务放在前面，这样当后面的工作服务错误时还有机会「回滚」**。

其次要能提供「回滚」操作使用到的业务数据。**「回滚」时提供的数据越多，越有益于程序的健壮性**。因为程序可以在收到「回滚」操作的时候可以做业务的检查，比如检查账户是否相等，金额是否一致等等。

由于这个中间状态的数据结构和数据大小并不固定，所以 Z 哥建议你在实现这点的时候可以将相关的数据序列化成一个 json，然后存放到一个 nosql 类型的存储中。

「隐式回滚」相对来说运用场景比较少。它意味着这个回滚动作你不需要进行额外处理，下游服务内部有类似“预占”并且“超时失效”的机制的。例如：

电商场景中，会将订单中的商品先预占库存，等待用户在 15 分钟内支付。如果没有收到用户的支付，则释放库存。

下面聊聊可以有很多玩法，也更容易陷入坑里的「重试」。

#### 4.2.2.「重试」

**「重试」最大的好处在于，业务系统可以不需要提供「逆向接口」**，这是一个对长期开发成本特别大的利好，毕竟业务是天天在变的。所以，**在可能的情况下，应该优先考虑使用「重试」。**

不过，相比「回滚」来说「重试」的适用场景更少一些，所以我们第一步首先要判断，当前场景是否适合「重试」。比如：

- 下游系统返回「请求超时」、「被限流中」等临时状态的时候，我们**可以考虑重试**
- 而如果是返回“余额不足”、“无权限”等明确无法继续的业务性错误的时候就**不需要重试**了
- 一些中间件或者 rpc 框架中返回 Http503、404 等没有何时恢复的预期的时候，也**不需要重试**

如果确定要进行「重试」，我们还需要选定一个合适的「重试策略」。主流的「重试策略」主要是以下几种。

**策略 1.立即重试**。有时故障是候暂时性，可能是因网络数据包冲突或硬件组件流量高峰等事件造成的。在此情况下，适合立即重试操作。不过，立即重试次数不应超过一次，如果立即重试失败，应改用其它的策略。

**策略 2.固定间隔**。应用程序每次尝试的间隔时间相同。 这个好理解，例如，固定每 3 秒重试操作。（以下所有示例代码中的具体的数字仅供参考。）

策略 1 和策略 2 多用于前端系统的交互式操作中。

**策略 3.增量间隔**。每一次的重试间隔时间增量递增。比如，第一次 0 秒、第二次 3 秒、第三次 6 秒，9、12、15 这样。

```
return (retryCount - 1) * incrementInterval;
```

使得失败次数越多的重试请求优先级排到越后面，给新进入的重试请求让道。

**策略 4.指数间隔**。每一次的重试间隔呈指数级增加。和增量间隔“殊途同归”，都是想让失败次数越多的重试请求优先级排到越后面，只不过这个方案的增长幅度更大一些。

```
return 2 ^ retryCount;
```

**策略 5.全抖动**。在递增的基础上，增加随机性（可以把其中的指数增长部分替换成增量增长。）。适用于将某一时刻集中产生的大量重试请求进行压力分散的场景。

```
return random(0 , 2 ^ retryCount);
```

**策略 6.等抖动**。在「指数间隔」和「全抖动」之间寻求一个中庸的方案，降低随机性的作用。适用场景和「全抖动」一样。

```
var baseNum = 2 ^ retryCount;return baseNum + random(0 , baseNum);
```

3、4、5、6 策略的表现情况大致是这样。(x 轴为重试次数)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418235535.png)

**为什么说「重试」有坑呢？**

正如前面聊到的那样，出于对开发成本考虑，你在做「重试」的时候可能是复用的常规调用的接口。那么此时就不得不提一个「幂等性」问题。

如果实现「重试」选用的技术方案不能 100%确保不会重复发起重试，那么「幂等性」问题是一个必须要考虑的问题。哪怕技术方案可以确保 100%不会重复发起重试，出于对意外情况的考量，尽量也考虑一下「幂等性」问题。

**幂等性：**不管对程序发起几次重复调用，程序表现的状态（所有相关的数据变化）与调用一次的结果是一致的话，就是保证了幂等性。

这意味着可以根据需要重复或重试操作，而不会导致意外的影响。对于非幂等操作，算法可能必须跟踪操作是否已经执行。

所以，**一旦某个功能支持「重试」，那么整个链路上的接口都需要考虑幂等性问题**，**不能因为服务的多次调用而导致业务数据的累计增加或减少。**

满足「幂等性」其实就是需要想办法识别重复的请求，并且将其过滤掉。思路就是：

1. 给每个请求定义一个唯一标识。
2. 在进行「重试」的时候判断这个请求是否已经被执行或者正在被执行，如果是则抛弃该请求。

**第 1 点，**我们可以使用一个全局唯一 id 生成器或者生成服务。 或者简单粗暴一些，使用官方类库自带的 Guid、uuid 之类的也行。

然后通过 rpc 框架在发起调用的客户端中，对每个请求增加一个唯一标识的字段进行赋值。

**第 2 点，**我们可以在服务端通过 Aop 的方式切入到实际的处理逻辑代码之前和之后，一起配合做验证。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210418235608.png)

大致的代码思路如下。

```
【方法执行前】if(isExistLog(requestId)){  //1.判断请求是否已被接收过。  对应序号3
    var lastResult = getLastResult();  //2.获取用于判断之前的请求是否已经处理完成。  对应序号4
    if(lastResult == null){  
        var result = waitResult();  //挂起等待处理完成
        return result;
    }
    else{
        return lastResult;
    }  
}
else{
    log(requestId);  //3.记录该请求已接收
}

//do something..【方法执行后】

logResult(requestId, result);  //4.将结果也更新一下。
```

如果「补偿」这个工作是通过 MQ 来进行的话，这事就可以直接在对接 MQ 所封装的 SDK 中做。在生产端赋值全局唯一标识，在消费端通过唯一标识消重。

### 4.3.「重试」的最佳实践

再聊一些 Z 哥积累的最佳实践吧（划重点：）），都是针对「重试」的，的确这也是工作中最常用的方案。

「重试」特别适合在高负载情况下被「降级」，当然也应当受到「限流」和「熔断」机制的影响。当「重试」的“矛”与「限流」和「熔断」的“盾”搭配使用，效果才是最好。

需要衡量增加补偿机制的投入产出比。一些不是很重要的问题时，应该「快速失败」而不是「重试」。

过度积极的重试策略（例如间隔太短或重试次数过多）会对下游服务造成不利影响，这点一定要注意。

**一定要给「重试」制定一个终止策略。**

当回滚的过程很困难或代价很大的情况下，可以接受很长的间隔及大量的重试次数，DDD 中经常被提到的「saga」模式其实也是这样的思路。不过，前提是不会因为保留或锁定稀缺资源而阻止其他操作（比如 1、2、3、4、5 几个串行操作。由于 2 一直没处理完成导致 3、4、5 没法继续进行）。







