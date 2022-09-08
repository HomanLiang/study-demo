[toc]



# 避免线上故障的10条建议

## 1、谨慎大切面

**引发问题示例：**

之前有次故障，因为一个大切面里包含了一个无界队列，直接out of memory了。

**原理和最佳实践：**

1>自定义切面时，要注意切面所占用的资源和耗时。如果切面包含外部调用等操作，前后一定要打日志。

2>业务逻辑代码不建议catch(Throwable)，因为针对系统的Error，人工处理的原则很难把控，但是切面是例外。切面建议catch(Throwable)，不要因为切面问题影响正常的业务逻辑。

## 2、合理的超时和重试

**引发问题示例：**

上面提到的故障，它的上游服务看到下游服务的现象就是超时。超时会造成线程池长时间不释放。结果上游服务线程池被打满。级联故障，整个服务链路雪崩。

**原理和最佳实践：**

超时和重试实践设置好需要压测，如果下游全部超时故障，也要做到自身不能挂。

## 3、事务里不能包含外部调用和大查询

**引发问题示例：**

有个事故是在事务包含了一个发送MQ的操作，当时的技术选型选用的rabbitMQ，积压敏感。MQ同步阻塞写入，积压时写入不了，造成事务无法提交。DB行锁升级为表锁。整个DB挂了。

**原理和最佳实践：**

1>在Java里，事务也一般用切面来控制，建议使用注解，而不是直接配置扫描包或者类的方式，避免操作被忽略。

2>事务中的查询建议使用id或者唯一索引作为查询条件。最好的方法是不用事务，比如可以用CAS原理先查询一般符合预期再进行更新这样的轻量级操作，这样替代事务操作不成功则回滚的重量级操作。

## 4、代码即注释

现在讲究代码即注释。单纯的注释会因为疏于维护，不能表达代码真正的含义，反而造成困扰。但是可以把真正要表达的意思写到日志里。担心日志太多影响性能，可以使用debug级别日志，线上实际打印是info级别日志的话，编译时日志就被过滤掉了，并不影响性能。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20220823170625.png)图片

## 5、不要做计划外的优化和重构（避免随手优化）

之前有个故障是一个代码洁癖的开发人员看到一个都表达账户的字段，一个用了accNO，另外一个用了accontNO。于是他对代码进行了重构。代码分层也会把对象分成DO、DTO、BO。这些对象有时候会用BeanUtils.copy来进行转换。结果这次重构没有改全，对象拷贝的时候一个值没有拷贝过来，导致线上所有创建账户操作都失败。

## 6、及时处理废弃逻辑和临时代码（设计包含扩展，开发只留有用）

一些同事喜欢把现在用不到，但是将来有可能用的代码留下来。我的建议是涉及时把怎么扩展都想好是对的，但实际代码里只留用到的，其他可以真正要的时候再写。针对一些拷贝过来可能会用到的代码可以进行一次提交再删除，这样就可以借助git版本管理等工具在需要的时候方便的找回来。前提是提交的注释一定要写好。

因为随着人员更替，线上一直在用的代码还好，改动起来心里反而有底气。找不到谁在用的代码，改起来心里更加忐忑。

## 7、不要吝啬回滚

好不容易一个服务上线了，听说整个链路里发生了故障，可能听起来和自己这边关系不大。绝大多数情况可能真的没有关系。但是我觉得一个比较好的实践是听到出现了故障，那咱就先回滚吧。起码让人家排查故障的时候也安心，肯定和咱们这边的发布没有关系。

之前有个故障是服务周二上线后当时没有什么问题，到了周四在高峰时段爆发了。开发人员只回滚了前一天晚上的服务。回滚后故障没有恢复，后来领导下令将所有服务版本回滚到了周一，包含了那个服务，才恢复了现场。

## 8、不要掩盖错误

有时候代码有个逻辑错误，为了避免挨一顿小骂，自己偷偷修改上线。其他人不知道。而修改的逻辑没有经过设计评审、代码走查。从而引起更大的问题，出了故障了大家还不知道这块有修改，现场恢复也慢。从而造成大故障。

## 9、组件等参数修改需要配合压测、避免想当然

有个故障是因为修改了MQ参数引起的，开发人员修改了消费端连接MQ的参数，这个参数调整实际上是把BIO阻塞方式进行消费消息改成了非阻塞。以为这个参数的调整可以提高性能，没有压测，造成了大故障

## 10、原理要理解透彻

上面的故障中为什么把BIO阻塞方式进行消费消息改成了非阻塞反而在高峰期高并发场景下造成故障呢？下面是个极简版的原理图：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20220823170745.png)