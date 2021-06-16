

[toc]



# Redis 缓存穿透、缓存击穿、缓存雪崩



## 1.缓存穿透
key对应的数据在数据源并不存在，每次针对此key的请求从缓存获取不到，请求都会到数据源，从而可能压垮数据源。比如用一个不存在的用户id获取用户信息，不论缓存还是数据库都没有，若黑客利用此漏洞进行攻击可能压垮数据库。
![image-20210303220733389](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210303220733389.png)

### 1.1.解决缓存穿透也有两种方案：
- 由于请求的参数是不合法的(每次都请求不存在的参数)，于是我们可以使用布隆过滤器(BloomFilter)或者压缩filter提前拦截，不合法就不让这个请求到数据库层！
- 当我们从数据库找不到的时候，我们也将这个空对象设置到缓存里边去。下次再请求的时候，就可以从缓存里边获取了。

	这种情况我们一般会将空对象设置一个较短的过期时间。
	
    ```
    //伪代码
    public object GetProductListNew() {
        int cacheTime = 30;
        String cacheKey = "product_list";

        String cacheValue = CacheHelper.Get(cacheKey);
        if (cacheValue != null) {
            return cacheValue;
        }

        cacheValue = CacheHelper.Get(cacheKey);
        if (cacheValue != null) {
            return cacheValue;
        } else {
            //数据库查询不到，为空
            cacheValue = GetProductListFromDB();
            if (cacheValue == null) {
                //如果发现为空，设置个默认值，也缓存起来
                cacheValue = string.Empty;
            }
            CacheHelper.Add(cacheKey, cacheValue, cacheTime);
            return cacheValue;
        }
    }
    ```



## 2.缓存击穿

key对应的数据存在，但在redis中过期，此时若有大量并发请求过来，这些请求发现缓存过期一般都会从后端DB加载数据并回设到缓存，这个时候大并发的请求可能会瞬间把后端DB压垮。
### 2.1.缓存击穿解决方案
key可能会在某些时间点被超高并发地访问，是一种非常“热点”的数据。这个时候，需要考虑一个问题：缓存被“击穿”的问题。

- 若缓存的数据是基本不会发生更新的，则可尝试将该热点数据设置为永不过期。

- 若缓存的数据更新不频繁，且缓存刷新的整个流程耗时较少的情况下，则可以采用基于 redis、zookeeper 等分布式中间件的分布式互斥锁，或者本地互斥锁以保证仅少量的请求能请求数据库并重新构建缓存，其余线程则在锁释放后能访问到新缓存。
  

**例如：使用互斥锁(mutex key)**

业界比较常用的做法，是使用mutex。简单地来说，就是在缓存失效的时候（判断拿出来的值为空），不是立即去load db，而是先使用缓存工具的某些带成功操作返回值的操作（比如Redis的SETNX或者Memcache的ADD）去set一个mutex key，当操作返回成功时，再进行load db的操作并回设缓存；否则，就重试整个get缓存的方法。SETNX，是「SET if Not eXists」的缩写，也就是只有不存在的时候才设置，可以利用它来实现锁的效果。

  ```
    public String get(key) {
          String value = redis.get(key);
          if (value == null) { // 代表缓存值过期
              // 设置3min的超时，防止del操作失败的时候，下次缓存过期一直不能load db
          	if (redis.setnx(key_mutex, 1, 3 * 60) == 1) {  // 代表设置成功
                   value = db.get(key);
                   redis.set(key, value, expire_secs);
                   redis.del(key_mutex);
              } else {  // 这个时候代表同时候的其他线程已经load db并回设到缓存了，这时候重试获取缓存值即可
                   sleep(50);
                   get(key);  // 重试
              }
          } else {
              return value;      
          }
     }
  ```

- 若缓存的数据更新频繁或者缓存刷新的流程耗时较长的情况下，可以利用定时线程在缓存过期前主动的重新构建缓存或者延后缓存的过期时间，以保证所有的请求能一直访问到对应的缓存。



## 3.缓存雪崩

当缓存服务器重启或者大量缓存集中在某一个时间段失效，这样在失效的时候，也会给后端系统(比如DB)带来很大压力。
原因：
- Redis挂掉了，请求全部走数据库。
- 对缓存数据设置相同的过期时间，导致某段时间内缓存失效，请求全部走数据库。

![image-20210303221540613](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/redis-demo/image-20210303221540613.png)



### 3.1.解决方案

- 对于“对缓存数据设置相同的过期时间，导致某段时间内缓存失效，请求全部走数据库。”

  这种情况，非常好解决，解决方法：

  - 在缓存的时候给过期时间加上一个随机值，这样就会大幅度的减少缓存在同一时间过期。

  - 加锁排队，伪代码如下：

    ```
    //伪代码
    public object GetProductListNew() {
        int cacheTime = 30;
        String cacheKey = "product_list";
        String lockKey = cacheKey;
    
        String cacheValue = CacheHelper.get(cacheKey);
        if (cacheValue != null) {
            return cacheValue;
        } else {
            synchronized(lockKey) {
                cacheValue = CacheHelper.get(cacheKey);
                if (cacheValue != null) {
                    return cacheValue;
                } else {
                  //这里一般是sql查询数据
                    cacheValue = GetProductListFromDB(); 
                    CacheHelper.Add(cacheKey, cacheValue, cacheTime);
                }
            }
            return cacheValue;
        }
    }
    ```
    加锁排队只是为了减轻数据库的压力，并没有提高系统吞吐量。假设在高并发下，缓存重建期间key是锁着的，这是过来1000个请求999个都在阻塞的。同样会导致用户等待超时，这是个治标不治本的方法！
    注意：加锁排队的解决方式分布式环境的并发问题，有可能还要解决分布式锁的问题；线程还会被阻塞，用户体验很差！因此，在真正的高并发场景下很少使用！

    

- 对于“Redis挂掉了，请求全部走数据库”这种情况，我们可以有以下的思路：

	- 事发前：实现Redis的高可用(主从架构+Sentinel 或者Redis Cluster)，尽量避免Redis挂掉这种情况发生。

	- 事发中：万一Redis真的挂了，我们可以设置本地缓存(ehcache)+限流(hystrix)，尽量避免我们的数据库被干掉(起码能保证我们的服务还是能正常工作的)

	- 事发后：redis持久化，重启后自动从磁盘上加载数据，快速恢复缓存数据。























