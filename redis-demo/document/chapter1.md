[toc]

# Redis 简介

## 1.Redis 简介
Redis 是完全开源免费的，遵守 BSD 协议，是一个高性能的 key - value 数据库
Redis 与 其他 key - value 缓存产品有以下三个特点：
- Redis 支持数据持久化，可以将内存中的数据保存在磁盘中，重启的时候可以再次加载进行使用。
- Redis 不仅仅支持简单的 key - value 类型的数据，同时还提供 list，set，zset，hash 等数据结构的存储
- Redis 支持数据的备份，即 master - slave 模式的数据备份



## 2.Redis 优势

- 性能极高 – Redis 读的速度是 110000 次 /s, 写的速度是 81000 次 /s 。
- 丰富的数据类型 - Redis 支持二进制案例的 Strings, Lists, Hashes, Sets 及 Ordered Sets 数据类型操作。
- 原子性 - Redis 的所有操作都是原子性的，意思就是要么成功执行要么失败完全不执行。单个操作是原子性的。多个操作也支持事务，即原子性，通过 MULTI 和 EXEC 指令包起来。
- 其他特性 - Redis 还支持 publish/subscribe 通知，key 过期等特性。