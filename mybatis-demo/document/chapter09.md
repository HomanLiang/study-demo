[toc]



# MyBatis 缓存

## 1.一级缓存

**1.1前言**

在介绍缓存之前，先了解下mybatis的几个核心概念：

- SqlSession：代表和数据库的一次会话，向用户提供了操作数据库的方法

- MapperedStatement：代表要往数据库发送的要执行的指令，可以理解为sql的抽象表示

- Executor：用来和数据库交互的执行器，接收MapperedStatement作为参数

**1.2.一级缓存的介绍**

mybatis一级缓存有两种：一种是SESSION级别的，针对同一个会话SqlSession中，执行多次条件完全相同的同一个sql，那么会共享这一缓存，默认是SESSION级别的缓存；一种是STATEMENT级别的，缓存只针对当前执行的这一statement有效。

对于一级缓存的流程，看下图：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407001022.png)

整个流程是这样的：

- 针对某个查询的statement，生成唯一的key

- 在Local Cache 中根据key查询数据是否存在

- 如果存在，则命中，跳过数据库查询，继续往下走

- 如果没命中：

  - 去数据库中查询，得到查询结果

  - 将key和查询结果放到Local Cache中
  - 将查询结果返回

- 判断是否是STATEMENT级别缓存，如果是，则清除缓存

接下来针对一级缓存的几种情况，来进行验证。

情况1：SESSION级别缓存，同一个Mapper代理对象执行条件相同的同一个查询sql

```
SqlSession sqlSession = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper = sqlSession.getMapper(GoodsDao.class);
        goodsMapper.selectGoodsById("1");
        goodsMapper.selectGoodsById("1");
```

**结果：**

```
Setting autocommit to false on JDBC Connection [com.mysql.jdbc.JDBC4Connection@3dd44d5e]
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
```

**总结：**只向数据库进行了一次查询，第二次用了缓存

**情况2：**SESSION级别缓存，同一个Mapper代理对象执行条件不同的同一个查询sql

```
public void selectGoodsTest(){
        SqlSession sqlSession = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper = sqlSession.getMapper(GoodsDao.class);
        goodsMapper.selectGoodsById("1");
        goodsMapper.selectGoodsById("2");
    }
```

**结果：**

```
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
==>  Preparing: select * from goods where id = ? 
==> Parameters: 2(String)
<==    Columns: id, name, detail, remark
<==        Row: 2, title2, null, null
<==      Total: 1
```

**总结：**因为查询条件不同，所以是两个不同的statement，生成了两个不同key，缓存中是没有的

**情况3：**SESSION级别缓存，针对同一个Mapper接口生成两个代理对象，然后执行查询条件完全相同的同一条sql

```
public void selectGoodsTest(){
        SqlSession sqlSession = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper = sqlSession.getMapper(GoodsDao.class);
        GoodsDao goodsMapper2 = sqlSession.getMapper(GoodsDao.class);
        goodsMapper.selectGoodsById("1");
        goodsMapper2.selectGoodsById("1");
    }
```

**结果：**

```
==> Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
```

**总结：**这种情况满足：同一个SqlSession会话，查询条件完全相同的同一条sql。所以，第二次查询是从缓存中查找的。

**情况4：**SESSION级别缓存，在同一次会话中，对数据库进行了修改操作，一级缓存是否是失效。

```
// 这里对id=2的数据进行了upate操作，发现id=1的一级缓存也被清除，因为它们是在同一个SqlSession中    @Test
    public void selectGoodsTest(){
        SqlSession sqlSession = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper = sqlSession.getMapper(GoodsDao.class);
        Goods goods = new Goods();
        goods.setId("2");
        goods.setName("篮球");
        goodsMapper.selectGoodsById("1");
        goodsMapper.updateGoodsById(goods);
        goodsMapper.selectGoodsById("1");
    }
```

**结果：**

```
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
==>  Preparing: update goods set name = ? where id = ? 
==> Parameters: 篮球(String), 2(String)
<==    Updates: 1
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
```

**总结：**在同一个SqlSession会话中，如果对数据库进行了修改操作，那么该会话中的缓存都会被清除。但是，并不会影响其它会话中的缓存。

**情况5：**SESSION级别缓存，开启两个SqlSession，在SqlSession1中查询操作，在SqlSession2中执行修改操作，那么SqlSession1中的一级缓存是否仍然有效？

```
@Test
    public void selectGoodsTest(){
        SqlSession sqlSession = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper = sqlSession.getMapper(GoodsDao.class);
        SqlSession sqlSession2 = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper2 = sqlSession2.getMapper(GoodsDao.class);
        Goods goods = new Goods();
        goods.setId("1");
        goods.setName("篮球");
        Goods goods1 = goodsMapper.selectGoodsById("1");
        System.out.println("name="+goods1.getName());
        System.out.println("******************************************************");
        goodsMapper2.updateGoodsById(goods);
        Goods goodsResult = goodsMapper.selectGoodsById("1");
        System.out.println("******************************************************");
        System.out.println("name="+goodsResult.getName());
    }
```

**结果：**

```
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
name=title1
******************************************************
Opening JDBC Connection
Created connection 644010817.
Setting autocommit to false on JDBC Connection [com.mysql.jdbc.JDBC4Connection@2662d341]
==>  Preparing: update goods set name = ? where id = ? 
==> Parameters: 篮球(String), 1(String)
<==    Updates: 1
******************************************************
name=title1
```

**总结：**在SqlSession2中对id=1的数据做了修改，但是在SqlSession1中的最后一次查询中，仍然是从一级缓存中取得数据，说明了一级缓存只在SqlSession内部共享，SqlSession对数据库的修改操作不影响其它SqlSession中的一级缓存。

**情况6**：SqlSession的缓存级别设置为STATEMENT，即在配置文件中添加如下代码：

```
<settings>
    <setting name="localCacheScope" value="STATEMENT"/>
</settings>
```

**执行代码：**

```
	@Test
    public void selectGoodsTest(){
        SqlSession sqlSession = getSqlSessionFactory().openSession();
        GoodsDao goodsMapper = sqlSession.getMapper(GoodsDao.class);
        goodsMapper.selectGoodsById("1");
        System.out.println("****************************************");
        goodsMapper.selectGoodsById("1");
    }
```

**结果：**

```
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
****************************************
==>  Preparing: select * from goods where id = ? 
==> Parameters: 1(String)
<==    Columns: id, name, detail, remark
<==        Row: 1, title1, null, null
<==      Total: 1
```

**总结：**STATEMENT级别的缓存，只针对当前执行的这一statement有效

 **1.3.一级缓存是如何被存取的？**

我们知道，当与数据库建立一次连接，就会创建一个SqlSession对象，默认是DefaultSqlSession这个实现，这个对象给用户提供了操作数据库的各种方法，与此同时，也会创建一个Executor执行器，缓存信息就是维护在Executor中，Executor有一个抽象子类BaseExecutor，这个类中有个属性PerpetualCache类，这个类就是真正用于维护一级缓存的地方。通过看源码，可以知道如何根据cacheKey，取出和存放缓存的。

在查询数据库前，先从缓存中查找，进入BaseExecutor类的query方法：

```
//这是BaseExecutor的一个属性，用于存放一级缓存protected PerpetualCache localCache; @SuppressWarnings("unchecked")
  public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
    if (closed) throw new ExecutorException("Executor was closed.");
    if (queryStack == 0 && ms.isFlushCacheRequired()) {
      clearLocalCache();
    }
    List<E> list;
    try {
      queryStack++;      // 根据CacheKey作为key，查询HashMap中的value值，也就是缓存，这就是取出缓存的过程
      list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
      if (list != null) {
        handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
      } else {        // 如果没有查询到对应的缓存，那么就从数据库中查找，进入该方法
        list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
      }
    } finally {
      queryStack--;
    }
    if (queryStack == 0) {
      for (DeferredLoad deferredLoad : deferredLoads) {
        deferredLoad.load();
      }
      deferredLoads.clear(); // issue #601
      if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
        clearLocalCache(); // issue #482
      }
    }
    return list;
  }
```

当从数据库中查询到数据后，需要把数据存放到缓存中的，然后再返回数据，这个就是存放缓存的过程，进入queryFromDatabase方法：

```
private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
    List<E> list;
    localCache.putObject(key, EXECUTION_PLACEHOLDER);
    try {      // 从数据库中查询到数据
      list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    } finally {
      localCache.removeObject(key);
    }    // 把数据放到缓存中，这就是存房缓存的动作
    localCache.putObject(key, list);
    if (ms.getStatementType() == StatementType.CALLABLE) {
      localOutputParameterCache.putObject(key, parameter);
    }
    return list;
  }
```

**1.4.CacheKey是如何确定唯一的？**

我们知道，如果两次查询完全相同，那么第二次查询就从缓存中取数据，换句话说，怎么判断两次查询是不是相同的？是否相同是根据CacheKey来判断的，那么看下CacheKey的生成过程，就知道影响CacheKey是否相同的元素有哪些了。

进入BaseExecutor类的createCacheKey方法：

```
public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    if (closed) throw new ExecutorException("Executor was closed.");
    CacheKey cacheKey = new CacheKey();    // statement id
    cacheKey.update(ms.getId());    // rowBounds.offset
    cacheKey.update(rowBounds.getOffset());
    // rowBounds.limit    cacheKey.update(rowBounds.getLimit());
    // sql语句    cacheKey.update(boundSql.getSql());
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
    for (int i = 0; i < parameterMappings.size(); i++) { // mimic DefaultParameterHandler logic
      ParameterMapping parameterMapping = parameterMappings.get(i);
      if (parameterMapping.getMode() != ParameterMode.OUT) {
        Object value;
        String propertyName = parameterMapping.getProperty();
        if (boundSql.hasAdditionalParameter(propertyName)) {
          value = boundSql.getAdditionalParameter(propertyName);
        } else if (parameterObject == null) {
          value = null;
        } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
          value = parameterObject;
        } else {
          MetaObject metaObject = configuration.newMetaObject(parameterObject);
          value = metaObject.getValue(propertyName);
        }        // 传递的每一个参数
        cacheKey.update(value);
      }
    }
    return cacheKey;
  }
```

**所以影响Cachekey是否相同的因素有：statementId，offset，limit，sql语句，参数**

接下来进入cacheKey.update方法，看它如何处理以上这五个元素的：

```
private void doUpdate(Object object) {    // 获取对象的HashCode
    int baseHashCode = object == null ? 1 : object.hashCode();
    // 计数器+1
    count++;
    checksum += baseHashCode;    // baseHashCode扩大count倍
    baseHashCode *= count;
    // 对HashCode进一步做处理
    hashcode = multiplier * hashcode + baseHashCode;
    // 把以上五个元素存放到集合中
    updateList.add(object);
  }
```

CahceKey的属性和构造方法：

```
private int multiplier;private int hashcode;private long checksum; 
  private int count;
  private List<Object> updateList;
public CacheKey() {
    this.hashcode = DEFAULT_HASHCODE;
    this.multiplier = DEFAULT_MULTIPLYER;
    this.count = 0;
    this.updateList = new ArrayList<Object>();
  }
```

CacheKey中最重要的一个方法来了，如何判断两个CacheKey是否相等？

```
public boolean equals(Object object) {
    if (this == object)
      return true;
    if (!(object instanceof CacheKey))
      return false;

    final CacheKey cacheKey = (CacheKey) object;
    // 判断HashCode是否相等
    if (hashcode != cacheKey.hashcode)
      return false;    // 判断checksum是否相等
    if (checksum != cacheKey.checksum)
      return false;    // 判断count是否相等
    if (count != cacheKey.count)
      return false;
    // 逐一判断以上五个元素是否相等
    for (int i = 0; i < updateList.size(); i++) {
      Object thisObject = updateList.get(i);
      Object thatObject = cacheKey.updateList.get(i);
      if (thisObject == null) {
        if (thatObject != null)
          return false;
      } else {
        if (!thisObject.equals(thatObject))
          return false;
      }
    }
    return true;
  } // 只有以上所有的判断都相等时，两个CacheKey才相等
```

**1.5.一级缓存的生命周期是多长？**

**开始：**mybatis建立一次数据库会话时，就会生成一系列对象：SqlSession--->Executor--->PerpetualCache,也就开启了对一级缓存的维护。

**结束：** 

- 会话结束，会释放掉以上生成的一系列对象，缓存也就不可用了。
- 调用sqlSession.close方法，会释放掉PerpetualCache对象，一级缓存不可用
- 调用sqlSession.clearCache方法，会清空PerpetualCache对象中的缓存数据，该对象可用，一级缓存不可用
- 调用sqlSession的update,insert，delete方法，会清空PerpetualCache对象中的缓存数据，该对象可用，一级缓存不可用



## 2.二级缓存

### 2.1.MyBatis 二级缓存介绍

MyBatis 一级缓存最大的共享范围就是一个SqlSession内部，那么如果多个 SqlSession 需要共享缓存，则需要开启二级缓存，开启二级缓存后，会使用 CachingExecutor 装饰 Executor，进入一级缓存的查询流程前，先在CachingExecutor 进行二级缓存的查询，具体的工作流程如下所示

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407002039.png)

当二级缓存开启后，同一个命名空间(namespace) 所有的操作语句，都影响着一个**共同的 cache**，也就是二级缓存被多个 SqlSession 共享，是一个**全局的变量**。当开启缓存后，数据的查询执行的流程就是 二级缓存 -> 一级缓存 -> 数据库。

#### 2.1.1.二级缓存开启条件

二级缓存默认是不开启的，需要手动开启二级缓存，实现二级缓存的时候，MyBatis要求返回的POJO必须是可序列化的。开启二级缓存的条件也是比较简单，通过直接在 MyBatis 配置文件中通过

```xml
<settings>
	<setting name = "cacheEnabled" value = "true" />
</settings>
```

来开启二级缓存，还需要在 Mapper 的xml 配置文件中加入 `<cache>`标签

**设置 cache 标签的属性**

cache 标签有多个属性，一起来看一些这些属性分别代表什么意义

- `eviction`: 缓存回收策略，有这几种回收策略
- LRU - 最近最少回收，移除最长时间不被使用的对象
  - FIFO - 先进先出，按照缓存进入的顺序来移除它们
  - SOFT - 软引用，移除基于垃圾回收器状态和软引用规则的对象
  - WEAK - 弱引用，更积极的移除基于垃圾收集器和弱引用规则的对象

> 默认是 LRU 最近最少回收策略

- `flushinterval` 缓存刷新间隔，缓存多长时间刷新一次，默认不清空，设置一个毫秒值
- `readOnly`: 是否只读；**true 只读**，MyBatis 认为所有从缓存中获取数据的操作都是只读操作，不会修改数据。MyBatis 为了加快获取数据，直接就会将数据在缓存中的引用交给用户。不安全，速度快。**读写(默认)**：MyBatis 觉得数据可能会被修改
- `size` : 缓存存放多少个元素
- `type`: 指定自定义缓存的全类名(实现Cache 接口即可)
- `blocking`： 若缓存中找不到对应的key，是否会一直blocking，直到有对应的数据进入缓存。

#### 2.1.2.探究二级缓存

我们继续以 MyBatis 一级缓存文章中的例子为基础，搭建一个满足二级缓存的例子，来对二级缓存进行探究，例子如下(对 一级缓存的例子部分源码进行修改)：

Dept.java

```java
//存放在共享缓存中数据进行序列化操作和反序列化操作
//因此数据对应实体类必须实现【序列化接口】
public class Dept implements Serializable {

    private Integer deptNo;
    private String  dname;
    private String  loc;

    public Dept() {}
    public Dept(Integer deptNo, String dname, String loc) {
        this.deptNo = deptNo;
        this.dname = dname;
        this.loc = loc;
    }

   get and set...
    @Override
    public String toString() {
        return "Dept{" +
                "deptNo=" + deptNo +
                ", dname='" + dname + '\'' +
                ", loc='" + loc + '\'' +
                '}';
    }
}
```

myBatis-config.xml

在myBatis-config 中添加开启二级缓存的条件

```xml
<!-- 通知 MyBatis 框架开启二级缓存 -->
<settings>
  <setting name="cacheEnabled" value="true"/>
</settings>
```

DeptDao.xml

还需要在 Mapper 对应的xml中添加 cache 标签，表示对哪个mapper 开启缓存

```xml
<!-- 表示DEPT表查询结果保存到二级缓存(共享缓存) -->
<cache/>
```

对应的二级缓存测试类如下：

```java
public class MyBatisSecondCacheTest {

    private SqlSession sqlSession;
    SqlSessionFactory factory;
    @Before
    public void start() throws IOException {
        InputStream is = Resources.getResourceAsStream("myBatis-config.xml");
        SqlSessionFactoryBuilder builderObj = new SqlSessionFactoryBuilder();
        factory = builderObj.build(is);
        sqlSession = factory.openSession();
    }
    @After
    public void destory(){
        if(sqlSession!=null){
            sqlSession.close();
        }
    }

    @Test
    public void testSecondCache(){
        //会话过程中第一次发送请求，从数据库中得到结果
        //得到结果之后，mybatis自动将这个查询结果放入到当前用户的一级缓存
        DeptDao dao =  sqlSession.getMapper(DeptDao.class);
        Dept dept = dao.findByDeptNo(1);
        System.out.println("第一次查询得到部门对象 = "+dept);
        //触发MyBatis框架从当前一级缓存中将Dept对象保存到二级缓存

        sqlSession.commit();
      	// 改成 sqlSession.close(); 效果相同

        SqlSession session2 = factory.openSession();
        DeptDao dao2 = session2.getMapper(DeptDao.class);
        Dept dept2 = dao2.findByDeptNo(1);
        System.out.println("第二次查询得到部门对象 = "+dept2);
    }
}
```

> 测试二级缓存效果，提交事务，`sqlSession`查询完数据后，`sqlSession2`相同的查询是否会从缓存中获取数据。

通过结果可以得知，首次执行的SQL语句是从数据库中查询得到的结果，然后第一个 SqlSession 执行提交，第二个 SqlSession 执行相同的查询后是从缓存中查取的。

用一下这幅图能够比较直观的反映两次 SqlSession 的缓存命中

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407002228.png)

### 2.2.二级缓存失效的条件

与一级缓存一样，二级缓存也会存在失效的条件的，下面我们就来探究一下哪些情况会造成二级缓存失效

#### 2.2.1.第一次SqlSession 未提交

SqlSession 在未提交的时候，SQL 语句产生的查询结果还没有放入二级缓存中，这个时候 SqlSession2 在查询的时候是感受不到二级缓存的存在的，修改对应的测试类，结果如下：

```java
@Test
public void testSqlSessionUnCommit(){
  //会话过程中第一次发送请求，从数据库中得到结果
  //得到结果之后，mybatis自动将这个查询结果放入到当前用户的一级缓存
  DeptDao dao =  sqlSession.getMapper(DeptDao.class);
  Dept dept = dao.findByDeptNo(1);
  System.out.println("第一次查询得到部门对象 = "+dept);
  //触发MyBatis框架从当前一级缓存中将Dept对象保存到二级缓存

  SqlSession session2 = factory.openSession();
  DeptDao dao2 = session2.getMapper(DeptDao.class);
  Dept dept2 = dao2.findByDeptNo(1);
  System.out.println("第二次查询得到部门对象 = "+dept2);
}
```

产生的输出结果：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407002244.png)

#### 2.2.2.更新对二级缓存影响

与一级缓存一样，更新操作很可能对二级缓存造成影响，下面用三个 SqlSession来进行模拟，第一个 SqlSession 只是单纯的提交，第二个 SqlSession 用于检验二级缓存所产生的影响，第三个 SqlSession 用于执行更新操作，测试如下：

```java
@Test
public void testSqlSessionUpdate(){
  SqlSession sqlSession = factory.openSession();
  SqlSession sqlSession2 = factory.openSession();
  SqlSession sqlSession3 = factory.openSession();

  // 第一个 SqlSession 执行更新操作
  DeptDao deptDao = sqlSession.getMapper(DeptDao.class);
  Dept dept = deptDao.findByDeptNo(1);
  System.out.println("dept = " + dept);
  sqlSession.commit();

  // 判断第二个 SqlSession 是否从缓存中读取
  DeptDao deptDao2 = sqlSession2.getMapper(DeptDao.class);
  Dept dept2 = deptDao2.findByDeptNo(1);
  System.out.println("dept2 = " + dept2);

  // 第三个 SqlSession 执行更新操作
  DeptDao deptDao3 = sqlSession3.getMapper(DeptDao.class);
  deptDao3.updateDept(new Dept(1,"ali","hz"));
  sqlSession3.commit();

  // 判断第二个 SqlSession 是否从缓存中读取
  dept2 = deptDao2.findByDeptNo(1);
  System.out.println("dept2 = " + dept2);
}
```

对应的输出结果如下

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407002252.png)

#### 2.2.3.探究多表操作对二级缓存的影响

现有这样一个场景，有两个表，部门表dept（deptNo,dname,loc）和 部门数量表deptNum（id,name,num），其中部门表的名称和部门数量表的名称相同，通过名称能够联查两个表可以知道其坐标(loc)和数量(num)，现在我要对部门数量表的 num 进行更新，然后我再次关联dept 和 deptNum 进行查询，你认为这个 SQL 语句能够查询到的 num 的数量是多少？来看一下代码探究一下

DeptNum.java

```java
public class DeptNum {

    private int id;
    private String name;
    private int num;

    get and set...
}
```

DeptVo.java

```java
public class DeptVo {

    private Integer deptNo;
    private String  dname;
    private String  loc;
    private Integer num;

    public DeptVo(Integer deptNo, String dname, String loc, Integer num) {
        this.deptNo = deptNo;
        this.dname = dname;
        this.loc = loc;
        this.num = num;
    }

    public DeptVo(String dname, Integer num) {
        this.dname = dname;
        this.num = num;
    }

    get and set

    @Override
    public String toString() {
        return "DeptVo{" +
                "deptNo=" + deptNo +
                ", dname='" + dname + '\'' +
                ", loc='" + loc + '\'' +
                ", num=" + num +
                '}';
    }
}
```

DeptDao.java

```java
public interface DeptDao {

    ...

    DeptVo selectByDeptVo(String name);

    DeptVo selectByDeptVoName(String name);

    int updateDeptVoNum(DeptVo deptVo);
}
```

DeptDao.xml

```xml
<select id="selectByDeptVo" resultType="com.mybatis.beans.DeptVo">
  select d.deptno,d.dname,d.loc,dn.num from dept d,deptNum dn where dn.name = d.dname
  and d.dname = #{name}
</select>

<select id="selectByDeptVoName" resultType="com.mybatis.beans.DeptVo">
  select * from deptNum where name = #{name}
</select>

<update id="updateDeptVoNum" parameterType="com.mybatis.beans.DeptVo">
  update deptNum set num = #{num} where name = #{dname}
</update>
```

DeptNum 数据库初始值：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407002303.png)

测试类对应如下：

```java
/**
     * 探究多表操作对二级缓存的影响
     */
@Test
public void testOtherMapper(){

  // 第一个mapper 先执行联查操作
  SqlSession sqlSession = factory.openSession();
  DeptDao deptDao = sqlSession.getMapper(DeptDao.class);
  DeptVo deptVo = deptDao.selectByDeptVo("ali");
  System.out.println("deptVo = " + deptVo);
  // 第二个mapper 执行更新操作 并提交
  SqlSession sqlSession2 = factory.openSession();
  DeptDao deptDao2 = sqlSession2.getMapper(DeptDao.class);
  deptDao2.updateDeptVoNum(new DeptVo("ali",1000));
  sqlSession2.commit();
  sqlSession2.close();
  // 第一个mapper 再次进行查询,观察查询结果
  deptVo = deptDao.selectByDeptVo("ali");
  System.out.println("deptVo = " + deptVo);
}
```

测试结果如下：

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407002308.png)

> 在对DeptNum 表执行了一次更新后，再次进行联查，发现数据库中查询出的还是 num 为 1050 的值，也就是说，实际上 1050 -> 1000 ，最后一次联查实际上查询的是第一次查询结果的缓存，而不是从数据库中查询得到的值，这样就读到了脏数据。

**解决办法**

如果是两个mapper命名空间的话，可以使用 `<cache-ref>`来把一个命名空间指向另外一个命名空间，从而消除上述的影响，再次执行，就可以查询到正确的数据

### 2.3.二级缓存源码解析

源码模块主要分为两个部分：二级缓存的创建和二级缓存的使用，首先先对二级缓存的创建进行分析：

#### 2.3.1.二级缓存的创建

二级缓存的创建是使用 Resource 读取 XML 配置文件开始的

```java
InputStream is = Resources.getResourceAsStream("myBatis-config.xml");
SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
factory = builder.build(is);
```

读取配置文件后，需要对XML创建 Configuration并初始化

```java
XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
return build(parser.parse());
```

调用 `parser.parse()` 解析根目录 /configuration 下面的标签，依次进行解析

```java
public Configuration parse() {
  if (parsed) {
    throw new BuilderException("Each XMLConfigBuilder can only be used once.");
  }
  parsed = true;
  parseConfiguration(parser.evalNode("/configuration"));
  return configuration;
}
private void parseConfiguration(XNode root) {
  try {
    //issue #117 read properties first
    propertiesElement(root.evalNode("properties"));
    Properties settings = settingsAsProperties(root.evalNode("settings"));
    loadCustomVfs(settings);
    typeAliasesElement(root.evalNode("typeAliases"));
    pluginElement(root.evalNode("plugins"));
    objectFactoryElement(root.evalNode("objectFactory"));
    objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
    reflectorFactoryElement(root.evalNode("reflectorFactory"));
    settingsElement(settings);
    // read it after objectFactory and objectWrapperFactory issue #631
    environmentsElement(root.evalNode("environments"));
    databaseIdProviderElement(root.evalNode("databaseIdProvider"));
    typeHandlerElement(root.evalNode("typeHandlers"));
    mapperElement(root.evalNode("mappers"));
  } catch (Exception e) {
    throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
  }
}
```

其中有一个二级缓存的解析就是

```java
mapperElement(root.evalNode("mappers"));
```

然后进去 mapperElement 方法中

```java
XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
```

继续跟 mapperParser.parse() 方法

```java
public void parse() {
  if (!configuration.isResourceLoaded(resource)) {
    configurationElement(parser.evalNode("/mapper"));
    configuration.addLoadedResource(resource);
    bindMapperForNamespace();
  }

  parsePendingResultMaps();
  parsePendingCacheRefs();
  parsePendingStatements();
}
```

这其中有一个 configurationElement 方法，它是对二级缓存进行创建，如下

```java
private void configurationElement(XNode context) {
  try {
    String namespace = context.getStringAttribute("namespace");
    if (namespace == null || namespace.equals("")) {
      throw new BuilderException("Mapper's namespace cannot be empty");
    }
    builderAssistant.setCurrentNamespace(namespace);
    cacheRefElement(context.evalNode("cache-ref"));
    cacheElement(context.evalNode("cache"));
    parameterMapElement(context.evalNodes("/mapper/parameterMap"));
    resultMapElements(context.evalNodes("/mapper/resultMap"));
    sqlElement(context.evalNodes("/mapper/sql"));
    buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
  } catch (Exception e) {
    throw new BuilderException("Error parsing Mapper XML. Cause: " + e, e);
  }
}
```

有两个二级缓存的关键点

```java
cacheRefElement(context.evalNode("cache-ref"));
cacheElement(context.evalNode("cache"));
```

也就是说，mybatis 首先进行解析的是 `cache-ref` 标签，其次进行解析的是 `cache` 标签。

**根据上面我们的 — 多表操作对二级缓存的影响 一节中提到的解决办法，采用 cache-ref 来进行命名空间的依赖能够避免二级缓存**，但是总不能每次写一个 XML 配置都会采用这种方式吧，最有效的方式还是避免多表操作使用二级缓存

然后我们再来看一下cacheElement(context.evalNode("cache")) 这个方法

```java
private void cacheElement(XNode context) throws Exception {
  if (context != null) {
    String type = context.getStringAttribute("type", "PERPETUAL");
    Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
    String eviction = context.getStringAttribute("eviction", "LRU");
    Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
    Long flushInterval = context.getLongAttribute("flushInterval");
    Integer size = context.getIntAttribute("size");
    boolean readWrite = !context.getBooleanAttribute("readOnly", false);
    boolean blocking = context.getBooleanAttribute("blocking", false);
    Properties props = context.getChildrenAsProperties();
    builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
  }
}
```

认真看一下其中的属性的解析，是不是感觉很熟悉？这不就是对 cache 标签属性的解析吗？！！！

上述最后一句代码

```java
builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
public Cache useNewCache(Class<? extends Cache> typeClass,
      Class<? extends Cache> evictionClass,
      Long flushInterval,
      Integer size,
      boolean readWrite,
      boolean blocking,
      Properties props) {
    Cache cache = new CacheBuilder(currentNamespace)
        .implementation(valueOrDefault(typeClass, PerpetualCache.class))
        .addDecorator(valueOrDefault(evictionClass, LruCache.class))
        .clearInterval(flushInterval)
        .size(size)
        .readWrite(readWrite)
        .blocking(blocking)
        .properties(props)
        .build();
    configuration.addCache(cache);
    currentCache = cache;
    return cache;
  }
```

这段代码使用了构建器模式，一步一步构建Cache 标签的所有属性，最终把 cache 返回。

#### 2.3.2.二级缓存的使用

在 mybatis 中，使用 Cache 的地方在 `CachingExecutor`中，来看一下 CachingExecutor 中缓存做了什么工作，我们以查询为例

```java
@Override
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
  throws SQLException {
  // 得到缓存
  Cache cache = ms.getCache();
  if (cache != null) {
    // 如果需要的话刷新缓存
    flushCacheIfRequired(ms);
    if (ms.isUseCache() && resultHandler == null) {
      ensureNoOutParams(ms, parameterObject, boundSql);
      @SuppressWarnings("unchecked")
      List<E> list = (List<E>) tcm.getObject(cache, key);
      if (list == null) {
        list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
        tcm.putObject(cache, key, list); // issue #578 and #116
      }
      return list;
    }
  }
  // 委托模式，交给SimpleExecutor等实现类去实现方法。
  return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
```

其中，先从 MapperStatement 取出缓存。只有通过`<cache/>,<cache-ref/>`或`@CacheNamespace,@CacheNamespaceRef`标记使用缓存的Mapper.xml或Mapper接口（同一个namespace，不能同时使用）才会有二级缓存。

如果缓存不为空，说明是存在缓存。如果cache存在，那么会根据sql配置(`<insert>,<select>,<update>,<delete>`的`flushCache`属性来确定是否清空缓存。

```java
flushCacheIfRequired(ms);
```

然后根据xml配置的属性`useCache`来判断是否使用缓存(resultHandler一般使用的默认值，很少会null)。

```java
if (ms.isUseCache() && resultHandler == null)
```

确保方法没有Out类型的参数，mybatis不支持存储过程的缓存，所以如果是存储过程，这里就会报错。

```java
private void ensureNoOutParams(MappedStatement ms, Object parameter, BoundSql boundSql) {
  if (ms.getStatementType() == StatementType.CALLABLE) {
    for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
      if (parameterMapping.getMode() != ParameterMode.IN) {
        throw new ExecutorException("Caching stored procedures with OUT params is not supported.  Please configure useCache=false in " + ms.getId() + " statement.");
      }
    }
  }
}
```

然后根据在 `TransactionalCacheManager` 中根据 key 取出缓存，如果没有缓存，就会执行查询，并且将查询结果放到缓存中并返回取出结果，否则就执行真正的查询方法。

```java
List<E> list = (List<E>) tcm.getObject(cache, key);
if (list == null) {
  list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  tcm.putObject(cache, key, list); // issue #578 and #116
}
return list;
```

### 2.4.是否应该使用二级缓存？

那么究竟应该不应该使用二级缓存呢？先来看一下二级缓存的注意事项：

1. 缓存是以`namespace`为单位的，不同`namespace`下的操作互不影响。
2. insert,update,delete操作会清空所在`namespace`下的全部缓存。
3. 通常使用MyBatis Generator生成的代码中，都是各个表独立的，每个表都有自己的`namespace`。
4. 多表操作一定不要使用二级缓存，因为多表操作进行更新操作，一定会产生脏数据。

如果你遵守二级缓存的注意事项，那么你就可以使用二级缓存。

但是，如果不能使用多表操作，二级缓存不就可以用一级缓存来替换掉吗？而且二级缓存是表级缓存，开销大，没有一级缓存直接使用 HashMap 来存储的效率更高，所以**二级缓存并不推荐使用**。



## 3.自定义缓存

mybatis的一级缓存和二级缓存都实现了cache接口,所以要实现自定义缓存而不使用mybatis默认的缓存，那么就要定义一个类让其实现cache接口，并在mapper.xml文件中指明缓存的类型。

**cache接口**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407003804.png)

cache接口默认的实现类=========》PerpetualCache.java

cache属性作为存储数据的变量，以下方法基本都是对cache的操作

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407003825.png)

**过程**

实现cache接口的类

```
package com.atguigu.cache;
import org.apache.ibatis.cache.Cache;
import java.util.concurrent.locks.ReadWriteLock;
public class RedisCache2 implements Cache {
    private String id;
    public RedisCache2(String id){
        this.id = id;
        System.out.println("当前加入缓存的namespace" + id); //com.atguigu.dao.UserDao
    }
    @Override
    public String getId() {
        return id;
    }
    //放入缓存
    @Override
    public void putObject(Object key, Object value) {
        System.out.println("key的值为: " + key );
        System.out.println("value的值为: " + value );
    }
    //在缓存中获取
    @Override
    public Object getObject(Object key) {
        return null;
    }
    //删除缓存中数据
    @Override
    public Object removeObject(Object key) {
        return null;
    }
    //清空缓存
    @Override
    public void clear() {
    }
    //缓存命中率计算
    @Override
    public int getSize() {
        return 0;
    }
    //读写锁
    @Override
    public ReadWriteLock getReadWriteLock() {
        //ReadWriteLock=====>接口
        return null;
    }
}
```

mapper.xml开启自定义缓存使用

```
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.atguigu.dao.UserDao">
 
    <!--开启二级缓存 mybatis二级缓存默认全局开启 cacheEnabled true 默认值-->
   <!--type:指定自定义cache全限定类名-->
    <!--开启自定义缓存-->
    <cache type="com.atguigu.cache.RedisCache2"></cache>
    <select id="queryById" parameterType="int" resultType="com.atguigu.entity.UserEntity">
        select * from user
        <where>
            <if test="_parameter!=null">
                id=#{userId}
            </if>
        </where>
    </select>
</mapper>
```

service.java

```
package com.atguigu.service.impl;
import com.atguigu.dao.UserDao;
import com.atguigu.entity.UserEntity;
import com.atguigu.service.UserService;
import org.apache.ibatis.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
@Transactional    //加入事务注解
public class UserServiceImpl implements UserService  {
    @Autowired
    private UserDao userDao;
    @Override
    public UserEntity queryById(Integer id) {
       UserEntity user = this.userDao.queryById(id);
       UserEntity user2 = this.userDao.queryById(id);
       UserEntity user3 = this.userDao.queryById(id);
        return user3;
    }
}
```

测试类

```
import com.atguigu.UserApplication;
import com.atguigu.dao.UserDao;
import com.atguigu.entity.UserEntity;
import com.atguigu.service.UserService;
import org.apache.ibatis.cache.Cache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
 
@SpringBootTest(classes = UserApplication.class)
@RunWith(SpringRunner.class)
public class UserServiceTest {
    @Autowired
    private UserService userService;
    @Test
    public void testFindAll(){
        userService.queryById(1);
        System.out.println();
        System.out.println("==============");
        UserEntity userEntity = userService.queryById(1);//执行相同的sql语句，进行相同数据的查询
        System.out.println(userEntity);
    }
```

结果

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407004019.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407004001.png)

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407004030.png)

所以在mybatis默认的缓存中，缓存的id值是namespace命名空间，数据类型是map类型的，key为namespace+sql语句等等，value为SQL语句查询的数据 

