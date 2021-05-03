[toc]



# 电商 - 促销后台

电商所谓营销，归根结底都是订单金额的变化；如果我们清楚的知道订单金额的计算流程是怎样的，那么我们只需要顺着系统的计算流程做促销，就不用担心各种促销类型之间产生重叠或者冲突的情况了。当我们知道这个关系后，就可以将营销活动区分为三种类型：改商品价格、改商品小计价格、改订单价格，因为无论什么营销归根结底都是可以描述成改价格。

购物车中任何增删查改都要重新计算促销，所以促销的计算变得尤为重要，感觉京东已经把促销做到了极致。

从模式上来讲，我们公司的促销就相当于京东自营，所以很多也都是参考京东自营的，但我们还没法做到像京东促销那样强大。

这里，将我们做的促销跟大家分享一下，只涉及后台接口逻辑部分。

接口的功能就是输入商品列表，返回加了促销分组后的商品列表。

## 1.促销类型

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211727.webp)

前面说了，促销归根结底是改价格。在我们这里其它单品促销就是改商品价格；而条件促销就相当于改小计的价格；至于赠品促销不设计改价格，可以认为是单品促销的一种类型。

## 2.主流程

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211742.webp)

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211750.webp)

“**同类型通过实体进行互斥、不同类型可以相互叠加。**”这是别人总结的设计电商促销系统的基本原则，我也比较认同。

上面接口主流程就是先应用单品促销，再应用条件促销。稍微再细化一点儿就是这样的：

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211807.webp)

先处理赠品促销，将赠品挂载到主商品（原先用户添加的购物车中的商品我称之为主商品）上，再应用单品促销。

在进行单品促销的时候，很有可能同一个商品命中多个单品促销。这个时候只能取一个促销，此处的计算逻辑是这样的：

- 优惠力度最大的优先
- 优惠力度相同时，取最新创建的那个（创建时间最新）

**例如：**

商品A命中四条促销，分别是：【促销1】直降2元，【促销2】折扣8折，【促销3】直降1元。假设A的原价时10元，那么经过计算【促销1】8元，【促销2】8元，【促销3】9元。这个时候，【促销3】应该被剔除，假设【促销2】的创建时间比【促销1】要晚，那么应该取【促销2】。即商品A最终命中【促销2】。原价10元，促销价8元。

## 3.计算商品价格流程

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211826.webp)

稍微解释一下：

- 特价：商品A原价12元，今日特价9.9元。
- 折扣：商品打几折。
- 直降：商品A原价12元，今日直降3元，所以最终9元。且当促销价低于原价的70%时恢复原价。

## 4.限购流程

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211844.webp)

这里有两点需要说明：

1. 限购的话需要查订单系统，但是刚才说了购物车中的任意增删查改都要重新计算促销，所以如果这里直接调订单的话可能订单的顶不住（技术实力还比较薄弱，无奈！！！），考虑到这里我们冗余了订单数据，每次从本地数据库去查。当然，这样肯定不准，但是我们只保证90%的情况就可以了，所以这里我们采用这种方式。
2. 拆商品行。还是用上面的例子，商品A命中了【促销2】，假设【促销2】限购每人每单1件，而现在A 的数量时3，那么我们会拆成2行，第一行商品A售价8元数量1件，第二行商品A售价10元数量2件。

## 5.条件促销分组

同一个商品可能会命中多个条件促销，而最终每个商品只能应用一个条件促销（即每个商品最终只能属于一个组）

我们说，同种类型的促销不能叠加，不同类型的促销可以叠加。在我们这里，单品促销和单品促销不能叠加，条件促销与条件促销不能叠加，单品与条件可以叠加。

程序走到这里，我们已经完成了单品促销的处理，接下来处理条件促销。在决定商品应该最终应用哪个条件促销时，我们的原则是这样的：

1. **优先考虑满足条件的促销**

   这句话的意思是，假设商品A，商品B满足【促销1】满100减20这个阶梯，同时A和B又都命中了【促销2】但是不满足【促销2】的条件，因为假设【促销2】的最小阶梯是满150减30。那么这个时候，虽然A和B都同时命中【促销1】和【促销2】，但A和B一起正好符合【促销1】满100减20的条件，所以这个时候促销A和B应该最终取【促销2】

2. **同时满足多个条件促销时，取后创建的那个（创建时间最近）**

   还是上面的例子，假设A和B的总金额加起来是160元，那么它们都满足【促销1】和【促销2】，假设【促销2】是后创建的，所以此时它们最终命中的条件促销应该取【促销2】。并且，之后应该讲它们从【促销1】的商品组中剔除（PS：因为一个商品只能属于一个组，即只能应用一个条件促销）。京东在这里对每种促销做了计算，把最终用哪个促销的决定权交给用户去选，我们这里不搞这么复杂。

   说了这么多，可能有点晕，下面举个例子

   ![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503211919.webp)

   假设有A，B，C，D四个商品，促销1234是四个促销

   如图，【促销1】是所有商品，所有A，B，C，D四个都命中【促销1】，换句话说【促销1】的商品组中有A，B，C，D

   【促销2】的商品组中有A，C

   【促销3】的商品组中有A，B

   【促销4】的商品组中有A，B，C

   假设促销1，2，3，4是依次创建的，也就是说4是最晚创建的，1是最早创建的

   再假设，A+B+C符合【促销4】的其中一个阶梯条件，A+B符合【促销3】中的其中一个阶梯条件，A+B+C+D符合【促销1】的其中最低一级的阶梯条件

   那么，最终的促销分组应该是这样的：

   【促销4】的商品组有：A，B，C

   【促销3】的商品组为空

   【促销2】的商品组为空

   【促销1】的商品组中有：D，而且不满足最低的阶梯，因为原来A+B+C+D满足最低一级的阶梯，现在只剩下D了当然不满足最低一个的阶梯

## 6.条件促销分组计算

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503212016.webp)

在代码实现上，这里是两层循环：

- 第一层是条件促销列表
- 第二层是某个条件促销中的商品组

## 7.部分代码实现

代码可能是这样的，下面贴出条件促销部分的代码片段

```
//  处理条件促销
//  算小计
for (PromotionProductDTO promotionProductDTO : promotionProductDTOList) {
    promotionProductDTO.setSubtotal(promotionProductDTO.getPromotionPrice().multiply(new BigDecimal(promotionProductDTO.getQuantity())));
}
List<PromotionInfoDTO> conditionPromotionInfoDTOList = promotionInfoMap.get(PromotionTypeEnum.TIAOJIAN.getType());
//  限购
List<PromotionInfoDTO> validConditionPromotionInfoDTOList = new ArrayList<>();
for (PromotionInfoDTO promotionInfoDTO : conditionPromotionInfoDTOList) {
    if (isMaxConditionPromotionLimit(promotionInfoDTO, userId)) {
        continue;
    }
    validConditionPromotionInfoDTOList.add(promotionInfoDTO);
}
conditionPromotionInfoDTOList = validConditionPromotionInfoDTOList;

//  按范围初步将商品归到各个条件促销下（撒网）
for (PromotionInfoDTO promotionInfoDTO : conditionPromotionInfoDTOList) {
    List<PromotionProductDTO> matchedPromotionProductDTOList = new ArrayList<>();

    List<PromotionProductEntity> promotionProductEntityList = promotionInfoDTO.getDefinitiveProductEntityList();
    for (PromotionProductDTO promotionProductDTO : promotionProductDTOList) {
        //  商品匹配到的促销
        if (promotionInfoDTO.getProductRange() == PromotionPruductRangeEnum.ALL.getValue()) {
            matchedPromotionProductDTOList.add(promotionProductDTO);
        }else if (promotionInfoDTO.getProductRange() == PromotionPruductRangeEnum.CATEGORY.getValue()) {
            Set<String> secondCategorySet = promotionProductEntityList.stream().map(PromotionProductEntity::getProCategorySecond).collect(Collectors.toSet());
            if (secondCategorySet.contains(promotionProductDTO.getCategoryCode())) {
                matchedPromotionProductDTOList.add(promotionProductDTO);
            }
        }else if (promotionInfoDTO.getProductRange() == PromotionPruductRangeEnum.SPECIFIED.getValue()) {
            Set<Long> specialProductIdSet = promotionProductEntityList.stream().map(PromotionProductEntity::getProductId).collect(Collectors.toSet());
            if (specialProductIdSet.contains(promotionProductDTO.getId())) {
                matchedPromotionProductDTOList.add(promotionProductDTO);
            }
        }
    }

    //  促销匹配到的商品
    promotionInfoDTO.setMatchedProductDTOList(matchedPromotionProductDTOList);

    //  判断促销匹配的这些商品是否满足条件
    BigDecimal totalAmount = BigDecimal.ZERO;
    for (PromotionProductDTO promotionProductDTO : matchedPromotionProductDTOList) {
        totalAmount = totalAmount.add(promotionProductDTO.getSubtotal());
    }
    PromotionStairEntity promotionStairEntity = matchStair(promotionInfoDTO.getDefinitiveStairEntityList(), totalAmount);
    if (null != promotionStairEntity) {
        promotionInfoDTO.setPromotionStairEntity(promotionStairEntity);
    }
}

//  按满足条件与否以及促销创建的先后顺序进一步归档商品（即分组）
//  挑选出满足条件的促销，并按照创建时间降序排序
List<PromotionInfoDTO> matchedConditionPromotionInfoDTOList = conditionPromotionInfoDTOList.stream()
        .filter(x->null != x.getPromotionStairEntity())
        .sorted(Comparator.comparing(PromotionInfoDTO::getCreateTime).reversed())
        .collect(Collectors.toList());

//  去重，以保证每个组中的商品之间无交集
int len = matchedConditionPromotionInfoDTOList.size();
for (int i = 0; i < len - 1; i++) {
    PromotionInfoDTO majorPromotionInfoDTO = matchedConditionPromotionInfoDTOList.get(i);
    for (int j = i + 1; j < len; j++) {
        PromotionInfoDTO minorPromotionInfoDTO = matchedConditionPromotionInfoDTOList.get(j);
        for (PromotionProductDTO majorMatchedPromotionProductDTO : majorPromotionInfoDTO.getMatchedProductDTOList()) {
            minorPromotionInfoDTO.setMatchedProductDTOList(minorPromotionInfoDTO.getMatchedProductDTOList()
                    .stream()
                    .filter(x -> !x.getId().equals(majorMatchedPromotionProductDTO.getId()))
                    .collect(Collectors.toList()));
        }
    }
}

//  最终命中的促销
List<PromotionInfoDTO> ultimatePromotionInfoDTOList = new ArrayList<>();
//  重新计算各组匹配的阶梯规则
for (PromotionInfoDTO promotionInfoDTO : matchedConditionPromotionInfoDTOList) {
    List<PromotionProductDTO> promotionProductDTOS = promotionInfoDTO.getMatchedProductDTOList();
    //  过滤掉空的促销
    if (null == promotionProductDTOS || promotionProductDTOS.size() < 1) {
        continue;
    }
    ultimatePromotionInfoDTOList.add(promotionInfoDTO);
    BigDecimal totalAmount = BigDecimal.ZERO;
    for (PromotionProductDTO promotionProductDTO : promotionProductDTOS) {
        totalAmount = totalAmount.add(promotionProductDTO.getSubtotal());
    }

    //  查询该组商品满足的最高阶梯
    PromotionStairEntity promotionStairEntity = matchStair(promotionInfoDTO.getDefinitiveStairEntityList(), totalAmount);
    if (null != promotionStairEntity) {
        //  设置这组商品命中的促销的哪一个阶梯
        promotionInfoDTO.setPromotionStairEntity(promotionStairEntity);
        //  设置每个商品最终命中的唯一的条件促销
        for (PromotionProductDTO promotionProductDTO : promotionProductDTOS) {
            promotionProductDTO.setConditionpromotionInfoDTO(promotionInfoDTO);
        }
    }else {
        //  计算还差多少钱满足最低阶梯
        List<PromotionStairEntity> promotionStairList = promotionInfoDTO.getDefinitiveStairEntityList().stream().sorted(Comparator.comparing(PromotionStairEntity::getMinimumCharge)).collect(Collectors.toList());
        PromotionStairEntity promotionStairEntity2 = promotionStairList.get(0);
        BigDecimal minimumCharge = promotionStairEntity2.getMinimumCharge();
        BigDecimal balance = minimumCharge.subtract(totalAmount);
        promotionInfoDTO.setBalance(balance);
    }
}
```

## 8.返回的数据接口

最终返回的应该是一个列表，列表中的每一个元素代表一个条件促销（即分组）

接口看起来可能是这样的：

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503212051.webp)

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503212058.webp)

![å¾ç](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20210503212103.webp)



