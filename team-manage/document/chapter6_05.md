[toc]



# 面经

## 阿里淘系 Java（大三实习）

### 一面

1. 首先还是没有自我介绍，直接是项目介绍，项目使用的什么技术栈，里面的一些难点。
2. 项目中的大致流程，怎么去运行的。
3. 项目中的容器使用到 MySQL，ES，MongoDB，Redis，这些都是干嘛？为什么要用他们（他们有什么好处？解决什么额问题？），项目中怎么使用的？
4. 那么你们肯定使用到了索引对吧，详细说说 mysql 索引吧？
5. 你们建立索引有什么规则？怎么建立的？哪些地方使用到了索引？有什么好处？结合具体场景回答回答。
6. 索引是 B+树是吧？这个有什么优点呢，为什么 MySQL 要使用 B+树，不用别的呢？（这里我想问下大佬们，除了减少 io 次数，局部性原理，稳定，有序还有什么优点呢？）
7. 存储引擎了解过吗？
8. 你使用过 MVC 是吧，具体说说是什么，为什么选择这个，怎么封装的，如何方便？
9. MVC 是什么?三层架构指什么？为什么使用三层架构，具体说说怎么松耦合的,举例子？
10. MVC 请求执行流程
11. 你说说为什么松耦合，你代码中怎么实现松耦合？这样为什么就能体现松耦合，是什么和什么之间的耦合？
12. Spring 是什么?有什么优点?IOC 具体讲讲，为什么交给 Spring 容器管理就会松耦合，你来具体说一说？
13. HashMap 底层了解过吧？说说底层吧，数据结构。
14. HashMap 查询，删除的时间复杂度。
15. 保证线程安全，为什么推荐使用 ConcurrentHashMap，有什么特点。就只是使用数据结构 cas volatile 吗？除了这些没别的优点了吗？
16. cas 为什么就比 synchronized 轻量，什么原因，synchronized 怎么调用到操作系统的？具体说说，操作系统消耗什么资源呢？
17. Java 内存模型，那怎么保证可见性？加锁为什么就可以保证内存屏障？
18. 内存屏障是什么？具体说说吧？内存屏障为什么保障可见性？指令重排序？happen-before 原则了解么？
19. 线程这边在操作系统怎么体现的，解决什么问题？在单核 cpu 中线程起什么作用？为什么轻量？
20. 单核 CPU 线程解决问题，多核 CPU 中解决什么问题？
21. 怎么设置 CPU 最佳线程数？
22. 操作系统内存管理？分页？置换算法?有去深入了解吗?
23. 线程池聊聊？怎么设置线程数，什么时候最优，为什么这么设置？
24. 好的我这边已经大致了解了，你还有什么想问的吗?（我反问：部门干什么的，我表现怎么样？）

### 二面

1. 自我介绍
2. 项目介绍，技术难点
3. 看你项目使用到反射，谈谈你对于反射的理解，你在 项目中怎么使用的反射，有什么好处？
4. 说说 jvm 吧？jvm 怎么样你所了解的？（内存结构，堆，垃圾收集 算法。垃圾收集器，各个特点）
5. 类加载？双亲委派？你怎么能实现类加载机制？有什么需要考虑的吗？
6. 使用类加载机制能实现吗？考虑什么问题？
7. 说说 fullgc 你说说你的了解？jvm 触发 fullGC 老年代没有减少可能是什么原因？
8. 说说你使用的集合？底层了解多少？
9. 说说 ArrayList 和 LinkedList 底层有什么区别？说说怎么删除固定的位置的元素？有什么线程安全问题吗？为什么会产生？
10. 说说 HashMap 的底层原理？数据结构？扩容？
11. 说说 ConcurrentHashMap？
12. 说说你的 MySQL 理解？有没有写过什么复杂的 SQL？使用什么优化了吗？你平时是如何优化 SQL 的？如何查看 SQL 语句的执行速度？
13. 索引说一说？分库分表？
14. 说说 synchronized 和 volatile？
15. 说说 url 从输入到回车经历的过程？http tcp（三次握手，对应的状态）
16. 说说 ARP 协议？
17. 说说磁盘调度 算法？说说电梯 算法？说说扫描 算法？
18. 说说 Java 的线程和操作系统的线程是不是一样的？有什么区别？
19. 说说你理解的线程？说说线程池？几种方式？阿里巴巴开发手册为什么推荐使用自定义线程池？
20. 说说你的实习？
21. 说说怎么创建线程？有几种方式，为什么？有什么区别？
22. 移位操作为什么快？从底层是二进制，补码原码真值回答底层的原理。反问：部门干什么的？我表现怎么样？

二面之后，就没有下文了。这次面试确实有点受到了打击，业务场景确实我菜了。



## 携程春招 Java 后端

### 一面

1. 介绍下项目，热点数据用 Redis 的什么数据结构存储？`zset`。你这个负载均衡算法中的加权轮训算法怎么实现的？Rpc 项目中客户端调用服务的整个过程？怎么使用 Netty 进行通讯的？
2. SpringBoot 常用注解？`SpringBootApllication`由哪些注解组成？由三个注解组成，其中有一个开启自动配置的注解。
3. 说一下`volatile`? `volatile`有内存可见性和有序性，底层通过 lock 前缀的一个空指令实现。
4. `sleep`和`wait`的区别？一个释放锁，一个不会，wait 可以无限阻塞，sleep 不能。
5. 了解哪些设计模式？写一个双重检测的单例模式，为什么要加 synchronized？因为要考虑线程安全，加在类方法和普通方法有什么区别？
6. 了解注解吗？自己的项目使用到了自定义注解。注解怎么实现的？通过反射实现。反射中获取 class 对象的三种方式？通过 object 类的 getClass 方法，类的 class 属性，class 类的 forName 方法。
7. 了解哪些排序算法，写一个快排吧。



### 二面

1. 详细讲解你的项目，细节与难点。项目中使用的序列器，protobuf 为什么快？
2. Java 的 io 模型？bio，nio，aio。其中 nio 使用操作系统的 io 多路复用。io 多路复用的实现有哪些？select，poll，epoll。
3. 写一个多线程的题，线程 1 打印 a，线程 2 打印 b，线程 3 打印 c，要求顺序打印出 abcabcabc。

反问，对自己有什么建议，按校招已经到达标准，由于时间问题没法再深入了解项目，所以也没有什么建议。

整体难度不是很大，不像大厂那样死扣细节。最后求个 hr 面，不然真的要失业了。

### HR 面

1. 自我介绍
2. 为什么不读研究生了
3. 工作地点有要求吗
4. 薪资要求
5. 有过实习吗
6. 你这个项目的难点是什么
7. 比较看中公司的哪些方面
8. 有其他 offer 吗，哪些公司还在流程中
9. 过四六级吗？多少分？等下发个英语测评有时间去做下，会根据测评结果安排后续的进程



## 大厂面试走心经验分享

### 正文

一面面经：2021/1/21 下午五点

是个小姐姐，但是很严肃

1. 自我介绍
2. 介绍实习项目、难点、参与的工作
3. 项目用到的设计模式以及其他设计模式
4. syn 锁（升级过程、降级？（读写锁）一些底层原理）
5. 操作系统线程间同步机制
6. syn 和 lock 区别
7. 线程池、参数详解？你怎么设置参数（I/O 密集型、计算密集型）
8. JVM 内存区域
9. 如何排查一下线上 OOM 问题？
10. 类加载机制？
11. 双亲委派？tomcat 如何打破的？
12. 写代码 1 翻转链表（秒）2 手写快排序 介绍各个排序算法时间复杂度
13. 在看的书籍？知识？《深入理解 Java 虚拟机》说下最深刻的部分？我说了个 R 大写的 JVM 是如何区分出是引用类型还是基本类型
14. 反问 大约 47 min

二面：2020/1/22 上午十一点

HR 说的是一个高 T 二面面试我，果不其然，一个秃头大叔。

1. 自我介绍
2. 介绍京东实习项目
3. 项目分布式锁怎么用的？主从的缓存 master 节点 down 了怎么办？（Redlock）
4. 脑裂问题（配置文件）、数据倾斜（一致性哈希，虚拟节点）、数据分片
5. 缓存穿透、击穿、雪崩
6. Redis 哨兵？cluster？
7. zset 底层，为什么用压缩列表（避免内存碎片），跳表查询复杂度？log（n）（逮住 Redis 真就往死里问啊）
8. 看你博客里有微服务，讲讲微服务、分布式？
9. 项目中各个服务之间怎么调用的？我说用 JSF（JSF 是京东内部的RPC通信工具，类似于 dubbo）
10. 分布式 CAP 定理
11. 讲讲分布式事务解决方案，各自优缺点（内心。。。还好前几天看过）
12. 写题 忘记具体是啥了 反正都是属于 easy 的题目，大约 50+min

还有一些回忆不起来了，好像是 Spring 源码的东西，电脑面试没有录音，总之抠得很细，面试官果然是个大佬。。。

三面 2020/1/22 晚上八点半

1. 介绍自己
2. 两个栈实现一个队列
3. 基本上就是一些非技术问题，唠家常、谈理想、问 offer
4. 可能是我比较能 bb 三面约 1 h 15min

十分钟 HR 联系微信联系说过了，让我等接下来的补笔试、测评邮件

1/26下午收到邮件做完后 HR 叫我安心等待，最迟周五给发 offer。

lz 从去年二月份开始春招找实习，（双非本科学历）到现在差不多过去了一年。大大小小的面试也经历过了不少，也相应的拿了一些公司的 offer，下面是我的一些个人见解加上别的大佬的一些参考，希望能抛砖引玉，如有瑕疵，还请多多指教！

我认为的面试 = 基础 + 能力 + 规划以及一些面试技巧

其实也就是对应着一个人的：过去（学过的知识）、现在（拥有的能力）、未来（规划）

### 基础

这个就很简单了比如

1. 最基本的数据结构、算法；
2. 以Java 为例的一些基础知识：JVM、集合、框架；
3. 计算机操作系统、计算机网络；
4. 通用的一些中间件：netty、nginx、redis、MySQL 等。

这些是基本上都要知道的，尤其是一些面试常问的必须要数量掌握，尽管有人认为这是背书，但是连背书都不背的说明态度有问题，肯定是不是被公司接受的。

当然以上说的太笼统了，具体的复习路线可以参考其他大佬的作品，如不嫌弃可以看一下我写过的一些博客 https://blog.csdn.net/weixin_44104367 本文只做一些面试经验相关的总结、概述。

### 能力

这方面主要是围绕实习、项目来展开的基本上 = 技术硬实力 + 能力软实力。

比如：有实习的

1. 在实习期间做了哪些事能够证明自己的能力
2. 如何在开发过程中优雅的书写代码？（其实一些知识譬如设计模式大家都会背，但是能够真正将这个知识落到实地的又有几个呢？
3. 项目开发过程中如何一步步提升自己的技术能力、业务水平？
4. 如何快速的理解业务？适应环境？
5. 对于实习OR项目的思考总结？

要明白一点：**工作了的人很喜欢问一些 case，尤其是一些领导特别喜欢问，哪怕这个项目技术再牛，那么它是如何落地的呢？他的场景是什么？为了解决什么问题？使用了什么方法 OR 工具？达到的效果如何？最终能满足预期吗**？

比如大家都会背一些 Spring 源码，设计模式，但是能自己将这二者结合起来吗？比如结合 Spring 源码+设计模式开发？（这篇文章就是个例子 淘系技术部的 https://mp.weixin.qq.com/s/94oe5c_7ouE1GbyiPfNg5g）

对面试官而言，他们已经听吐了这些背的东西，如果自己能讲的让面试官眼前一亮，那么即时面试问题回答的不太好 最起码也能证明自己**对于技术是有追求、有思考的，而不是一个背题机器**。

**对于一个技术而言，它在这个公司存在的意义就是为了一些变现业务服务 技术服务于业务，用业务创造价值**。

个人认为学生状态过渡到工作状态就是理论转化为实践动力的过程 有些人总觉得校招生身上有一股`书生气` 大概就是：理论的东西多而幼稚，有些不切实际的想法，能不能落地呢？

### 规划

这点其实发现面试总结里面很少有人去谈，但是看到脉脉上，十个 HR，九个都会喜欢听到候选人这方面的一些思考、总结。

#### 路线规划

举个例子比如：会不会提前去规划自己的人生路线？

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-design/20220710225412.png)

这是从网上找的一张图，可以看到有很多路线，究竟哪一条适合自己？可能很多人压根没思考过这个问题？另外自己适合哪一行呢？教育？广告？地图？还是纯技术？

隔行如隔山，因为我从去年五月份就来实习，也换过俩部门，所以对于这些事情比其他人要深刻一些。

只有清楚知道自己喜欢的才有动力去做好他 不喜欢的只是为了生活被动的产出。

不是所有人都适合做纯技术、也有的人压根就不适合搞业务，究竟那条大路通向自己心中的罗马，只有自己清楚

**而这些事很多人都这么回复的：先有了工作再说**。

看过很多人 尤其是 90 后工作半年跳槽、一年的也有。反正给我的感觉就是：`不踏实`。对于公司而言就是不忠诚：谁能保证你从上一家公司跳又能干多长时间又跳走了呢？公司培养一个人的代价又有谁去承担呢？

在一个公司呆够超过五年 最起码可以说明这个人在这个行业、公司站住脚了。

有的人入职以后才发现自己不太适合这个行业、这个方向，于是就跳槽，简历就花了。

简历一花，找工作就更不好找。别的公司不知道，JD 这边有的部门五二原则卡的很严格（五年内只在两家公司工作过，也就是说平均一家公司至少工作两年时间 https://www.jianshu.com/p/ac8f28f58e11）

这还是次要，更重要的是自己能确保这个新的方向就自己适合或者喜欢吗？于是又开始跳、跳、跳。

人生往往是一步错、步步错。

别人已经在自己合适的方向上工作很久了 而你还在思考自己要做什么。

男怕入错行，女怕嫁错郎，说的大概就是这个意思。

#### 时间规划

大家都知道程序猿 35 是一道坎，到时候要么转管理，要么成为 CTO，自己对于自己的成长路线是一个什么规划呢？

管理路线：三年能够处理日常开发当中的任何问题；五年能够在技术上达到自己一个比较理想的状态；七年能够成为小组 leader；十年能够成为部门 leader

技术路线：三年够在技术上能够有较大提升；五年成为架构师；七年成为资深架构；十年成为总监等。

能够表达出这些，最起码能够说明自己**比较踏实，是一个有规划、有思想的人**。

### 面试经验

对于面试经验这块真的就只能实战找感觉了，每个人都有自己的一个表达方式，不过套路都是差不太多：

#### 扬长避短

像楼主本人学校不好但是一直在 JD 实习，那么自我介绍的时候可以说自己实习时间比较长等。

学校比较好但是导师不放实习的可以着重说下自己的学校经历，paper、竞赛情况，都没有的可以说能够凸显自己能力的地方

#### 适当的往自己会的方向引导面试官

有的面试官会自己电脑前放一个题库，但是大部分不会，会根据简历上，自己脑海中搜索问题。

比如问你 MySQL 调优，自己知道那些就说哪些，比如你知道索引这块哪些自己知道原理就说那些：（is null 判断可能会导致放弃索引、尽量避免使用判断等）再往下往往会问原理，因为你知道最左前缀原则、MySQL 优化器的索引代价分析、选择过程，你就可以轻松回答上来。

但是你不太懂索引相关的原理就不要胡言乱语，瞎往自己不擅长的领域引导面试官，可以从设计规范方面谈起（使用 varchar 而不是 char 等等）因为面试官一般会问：为什么？自己在学习知识的过程中也要经常问自己一句：为什么？比如都知道函数表达式操作会导致索引失效，那么原理呢？

#### 面试充满了不确定性

**你又不是RMB 不会所有人都喜欢你**。

这个恐怕很多人是深有体会：我面试面的挺好的啊，问题都回答上来了，怎么还是挂了？放宽心态，该佛系的时候佛系一点啦。

面试充满了不确定性，能和面试官聊得来，即便问题回答的不怎么样，面试官也会放你一马。

有的人跟面试官聊不来，甚至面试过程中发生了争执，那肯定就是不给过了呗。

#### 面试是一个双向选择的过程

你被面试官面试的时候其实你也在考量这个部门、这个面试官技术水平、人品以及是否愿意引导新人等。

一般面试自己的都会和自己入职后的工作关系是在一起的，一般是自己的同事、直属领导。

如果面试官为人和善、愿意引导你解答出问题来，那么入职之后你的成长速度也会更快的。

如果面试官技术问题问的很模糊其辞，不够专业，那么面试多了你也会感受出来。

**你不是非我不要，我也不是非你们部门、公司不选，面试就是一个双向选择的过程**。

#### 烂大街的项目尽量不要写

> 秒杀系统 、商城系统。

原因自己体会 懂得都懂

#### 学历差的自信一点

牛客上认识了很多盆友，大家的学历大多数都比我这个双非渣本菜鸡学历高得多，这也不是意味着学历差就一定不行，我这不是也上岸了百度、京东了么。

衷心劝诫 22 届及以后毕业学历较差的盆友，学历差不代表一切。

但正是因为学历的问题，就需要我们比别人付出更多，别人不会的可以通过学校、paper 补过来，但是我们一无所有只能冲。

确实像 bat 这大厂对于学历的要求不是特别高（某东除外，很多部门明确要求学历 211 及以上）

#### PS

都在问啥项目比较好，我从个人角度谈一下：有实习的话肯定是公司的项目比较好 无论是体量还是专业程度

没实习的话可以做个：仿制 dubbo、netty、tomcat、简单的 ioc 容器啦 或者一些实实在在能落地的项目，因为一个项目落没落地面试官一问就知道。

前者会更好的走完一个底层的流程，从原理搞懂这些中间件，会比直接背书强。

真正走完一个流程的项目远远优于那些网上的项目 没有实际的背景 那些开发中的情况面试官一问就破（以上为个人观点）








