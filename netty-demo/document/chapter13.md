# Pipeline Handler HandlerContext 创建源码剖析

## 源码剖析目的

Netty 中的 ChannelPipeline 、 ChannelHandler 和 ChannelHandlerContext 是非常核心的组件, 我们从源码来分析 Netty 是如何设计这三个核心组件的，并分析是如何创建和协调工作的



## 源码剖析

###  ChannelPipeline 、 ChannelHandler 和 ChannelHandlerContext 介绍

1. 三者关系



2. ChannelPipeline 作用及设计



3. ChannelHandler 作用及设计



4. ChannelHandlerContext 作用及设计



### ChannelPipeline 、 ChannelHandler 和 ChannelHandlerContext 创建过程



1. Socket 创建 pipeline



2. 在 addXXX 添加处理器的时候，创建 ContextXXX



## Pipeline Handler HandlerContext创建过程梳理

1. 每当创建 ChannelSocket 的时候都会创建一个绑定的 pipeline，一对一的关系，创建 pipeline 的时候也会创建 tail 节点和 head 节点，形成最初的链表。
2. 在调用 pipeline 的 addLast 方法的时候，会根据给定的 handler 创建一个 Context，然后，将这个 Context 插入到链表的尾端（tail 前面）。
3. Context 包装 handler，多个 Context 在 pipeline 中形成了双向链表
4. 入站方向叫 inbound，由 head 节点开始，出站方法叫 outbound ，由 tail 节点开始





















