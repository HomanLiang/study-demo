[toc]



# MyBatis 拦截器

## 1.Mybatis拦截器介绍

Mybatis拦截器设计的初衷就是为了供用户在某些时候可以实现自己的逻辑而不必去动Mybatis固有的逻辑。通过Mybatis拦截器我们可以拦截某些方法的调用，我们可以选择在这些被拦截的方法执行前后加上某些逻辑，也可以在执行这些被拦截的方法时执行自己的逻辑而不再执行被拦截的方法。所以Mybatis拦截器的使用范围是非常广泛的。

Mybatis里面的核心对象还是比较多，如下：

| Mybatis核心对象  | 解释                                                         |
| ---------------- | ------------------------------------------------------------ |
| SqlSession       | 作为MyBatis工作的主要顶层API，表示和数据库交互的会话，完成必要数据库增删改查功能 |
| Executor         | MyBatis执行器，是MyBatis 调度的核心，负责SQL语句的生成和查询缓存的维护 |
| StatementHandler | 封装了JDBC Statement操作，负责对JDBC statement 的操作，如设置参数、将Statement结果集转换成List集合 |
| ParameterHandler | 负责对用户传递的参数转换成JDBC Statement 所需要的参数        |
| ResultSetHandler | 负责将JDBC返回的ResultSet结果集对象转换成List类型的集合；    |
| TypeHandler      | 负责java数据类型和jdbc数据类型之间的映射和转换               |
| MappedStatement  | MappedStatement维护了一条 `mapper.xml` 文件里面 `select `、`update`、`delete`、`insert`节点的封装 |
| SqlSource        | 负责根据用户传递的parameterObject，动态地生成SQL语句，将信息封装到BoundSql对象中，并返回 |
| BoundSql         | 表示动态生成的SQL语句以及相应的参数信息                      |
| Configuration    | MyBatis所有的配置信息都维持在Configuration对象之中           |

Mybatis拦截器并不是每个对象里面的方法都可以被拦截的。Mybatis拦截器只能拦截Executor、ParameterHandler、StatementHandler、ResultSetHandler四个对象里面的方法。

### 1.1.Executor

Mybatis中所有的Mapper语句的执行都是通过Executor进行的。Executor是Mybatis的核心接口。从其定义的接口方法我们可以看出，对应的增删改语句是通过Executor接口的update方法进行的，查询是通过query方法进行的。Executor里面常用拦截方法如下所示。

  ```dart
  public interface Executor {

     ...

      /**
       * 执行update/insert/delete
       */
      int update(MappedStatement ms, Object parameter) throws SQLException;

      /**
       * 执行查询,先在缓存里面查找
       */
      <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

      /**
       * 执行查询
       */
      <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;

      /**
       * 执行查询，查询结果放在Cursor里面
       */
      <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

      ...

  }
  ```

### 1.2.ParameterHandler

ParameterHandler用来设置参数规则，当StatementHandler使用 `prepare()` 方法后，接下来就是使用它来设置参数。所以如果有对参数做自定义逻辑处理的时候，可以通过拦截ParameterHandler来实现。ParameterHandler里面可以拦截的方法解释如下：

  ```csharp
  public interface ParameterHandler {

   ...

   /**
    * 设置参数规则的时候调用 -- PreparedStatement
    */
   void setParameters(PreparedStatement ps) throws SQLException;

   ...

  }
  ```

### 1.3.StatementHandler

StatementHandler负责处理Mybatis与JDBC之间Statement的交互。

  ```java
  public interface StatementHandler {

      ...

      /**
       * 从连接中获取一个Statement
       */
      Statement prepare(Connection connection, Integer transactionTimeout)
              throws SQLException;

      /**
       * 设置statement执行里所需的参数
       */
      void parameterize(Statement statement)
              throws SQLException;

      /**
       * 批量
       */
      void batch(Statement statement)
              throws SQLException;

      /**
       * 更新：update/insert/delete语句
       */
      int update(Statement statement)
              throws SQLException;

      /**
       * 执行查询
       */
      <E> List<E> query(Statement statement, ResultHandler resultHandler)
              throws SQLException;

      <E> Cursor<E> queryCursor(Statement statement)
              throws SQLException;

      ...

  }
  ```

> 一般只拦截StatementHandler里面的prepare方法。

在Mybatis里面 `RoutingStatementHandler` 是 `SimpleStatementHandler(对应Statement)`、`PreparedStatementHandler(对应PreparedStatement)`、`CallableStatementHandler(对应CallableStatement)` 的路由类，所有需要拦截StatementHandler里面的方法的时候，对RoutingStatementHandler做拦截处理就可以了，如下的写法可以过滤掉一些不必要的拦截类。

  ```java
  @Intercepts({
          @Signature(
                  type = StatementHandler.class,
                  method = "prepare",
                  args = {Connection.class, Integer.class}
          )
  })
  public class TableShardInterceptor implements Interceptor {
  
      @Override
      public Object intercept(Invocation invocation) throws Throwable {
          if (invocation.getTarget() instanceof RoutingStatementHandler) {
              // TODO: 做自己的逻辑
          }
          return invocation.proceed();
      }
  
      @Override
      public Object plugin(Object target) {
          // 当目标类是StatementHandler类型时，才包装目标类，否者直接返回目标本身,减少目标被代理的次数
          return (target instanceof RoutingStatementHandler) ? Plugin.wrap(target, this) : target;
      }
  
      @Override
      public void setProperties(Properties properties) {
  
      }
  }
  ```

> 关于Statement、PreparedStatement和CallableStatement的一些区别。以及Statement和PreparedStatement相比PreparedStatement的优势在哪里。强烈建议大家去百度下。

### 1.4.ResultSetHandler

ResultSetHandler用于对查询到的结果做处理。所以如果你有需求需要对返回结果做特殊处理的情况下可以去拦截ResultSetHandler的处理。ResultSetHandler里面常用拦截方法如下：

  ```java
  public interface ResultSetHandler {

      /**
       * 将Statement执行后产生的结果集（可能有多个结果集）映射为结果列表
       */
      <E> List<E> handleResultSets(Statement stmt) throws SQLException;
      <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

      /**
       * 处理存储过程执行后的输出参数
       */
      void handleOutputParameters(CallableStatement cs) throws SQLException;

  }
  ```

## 2.Mybatis拦截器的使用

Mybatis拦截器的使用，分两步：自定义拦截器类、注册拦截器类。

### 2.1 自定义拦截器类

自定义的拦截器需要实现 `Interceptor` 接口，并且需要在自定义拦截器类上添加 `@Intercepts` 注解。

#### 2.1.1 Interceptor接口

Interceptor接口里面就三个方法。如下所示：

```dart
public interface Interceptor {

    /**
     * 代理对象每次调用的方法，就是要进行拦截的时候要执行的方法。在这个方法里面做我们自定义的逻辑处理
     */
    Object intercept(Invocation invocation) throws Throwable;

    /**
     * plugin方法是拦截器用于封装目标对象的，通过该方法我们可以返回目标对象本身，也可以返回一个它的代理
     *
     * 当返回的是代理的时候我们可以对其中的方法进行拦截来调用intercept方法 -- Plugin.wrap(target, this)
     * 当返回的是当前对象的时候 就不会调用intercept方法，相当于当前拦截器无效
     */
    Object plugin(Object target);

    /**
     * 用于在Mybatis配置文件中指定一些属性的，注册当前拦截器的时候可以设置一些属性
     */
    void setProperties(Properties properties);

}
```

#### 2.1.2 @Intercepts注解

Intercepts注解需要一个 `Signature(拦截点)` 参数数组。通过Signature来指定拦截哪个对象里面的哪个方法。`@Intercepts` 注解定义如下:

```kotlin
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Intercepts {
    /**
     * 定义拦截点
     * 只有符合拦截点的条件才会进入到拦截器
     */
    Signature[] value();
}
```

Signature来指定咱们需要拦截那个类对象的哪个方法。定义如下：

```tsx
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Signature {
    /**
     * 定义拦截的类 Executor、ParameterHandler、StatementHandler、ResultSetHandler当中的一个
     */
    Class<?> type();

    /**
     * 在定义拦截类的基础之上，在定义拦截的方法
     */
    String method();

    /**
     * 在定义拦截方法的基础之上在定义拦截的方法对应的参数，
     * JAVA里面方法可能重载，不指定参数，不晓得是那个方法
     */
    Class<?>[] args();
}
```

我们举一个例子来说明，比如我们自定义一个MybatisInterceptor类，来拦截Executor类里面的两个query。自定义拦截类MybatisInterceptor

```java
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        ),
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
public class MybatisInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        // TODO: 自定义拦截逻辑

    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this); // 返回代理类
    }

    @Override
    public void setProperties(Properties properties) {

    }
}
```

#### 2.2 注册拦截器

注册拦截器就是去告诉Mybatis去使用我们的拦截器。注册拦截器类非常的简单，在@Configuration注解的类里面，@Bean我们自定义的拦截器类。比如我们需要注册自定义的MybatisInterceptor拦截器。

```cpp
/**
 * mybatis配置
 */
@Configuration
public class MybatisConfiguration {

    /**
     * 注册拦截器
     */
    @Bean
    public MybatisInterceptor mybatisInterceptor() {
        MybatisInterceptor interceptor = new MybatisInterceptor();
        Properties properties = new Properties();
        // 可以调用properties.setProperty方法来给拦截器设置一些自定义参数
        interceptor.setProperties(properties);
        return interceptor;
    }
    

}
```

## 3.Mybatis拦截器实例-自定义拦截器

上面讲了一大堆，最终的目的都是要使用上拦截器，接下来。我们通过几个简单的自定义拦截器来加深对Mybatis拦截器的理解。实例代码在链接地址：[https://github.com/tuacy/microservice-framework](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2Ftuacy%2Fmicroservice-framework) 的 mybatis-interceptor module里面。

### 3.1 日志打印

自定义LogInterceptor拦截器，打印出我们每次sq执行对应sql语句。

### 3.2 分页

模仿pagehelper，咱们也来实现一个分页的拦截器PageInterceptor，该拦截器也支持自定义count查询。

### 3.3 分表

自定义拦截器TableShardInterceptor实现水平分表的功能。

### 3.4 对查询结果的某个字段加密

自定义拦截器EncryptResultFieldInterceptor对查询回来的结果中的某个字段进行加密处理。

> 上面拦截器的实现，在github  [https://github.com/tuacy/microservice-framework](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2Ftuacy%2Fmicroservice-framework) 的 mybatis-interceptor module里面都能找到具体的实现。

------

发现想把Mybatis拦截器的使用讲清楚还是比较难的，因为里面设计的到的东西太多了，用代码才是最好说话的，所以我在实例里面都尽可能的把注解写的很详细。希望能对大家有点帮助。

## X.常见问题

### X.1.分离代理对象的目标类 

```
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class}), 
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})}) 
@Log4j 
public class PageHelper implements Interceptor {
    public Object intercept(Invocation invocation) throws Throwable { 
        if (localPage.get() == null) { 
            return invocation.proceed(); 
        } 
        if (invocation.getTarget() instanceof StatementHandler) { 
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget(); 
            MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler); 
            // 分离代理对象链(由于目标类可能被多个插件拦截，从而形成多次代理，通过下面的两次循环 
            // 可以分离出最原始的的目标类) 
            while (metaStatementHandler.hasGetter("h")) { 
                Object object = metaStatementHandler.getValue("h"); 
                metaStatementHandler = SystemMetaObject.forObject(object); 
            } 
            // 分离最后一个代理对象的目标类 
            while (metaStatementHandler.hasGetter("target")) { 
                Object object = metaStatementHandler.getValue("target"); 
                metaStatementHandler = SystemMetaObject.forObject(object); 
            } 
            MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement"); 
            //分页信息if (localPage.get() != null) { 
            Page page = localPage.get(); 
            BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql"); 
            // 分页参数作为参数对象parameterObject的一个属性 
            String sql = boundSql.getSql(); 
            // 重写sql 
            String pageSql = buildPageSql(sql, page); 
            //重写分页sql 
            metaStatementHandler.setValue("delegate.boundSql.sql", pageSql); 
            Connection connection = (Connection) invocation.getArgs()[0]; 
            // 重设分页参数里的总页数等 
            setPageParameter(sql, connection, mappedStatement, boundSql, page); 
            // 将执行权交给下一个插件 
            return invocation.proceed(); 
        } else if (invocation.getTarget() instanceof ResultSetHandler) { 
            Object result = invocation.proceed(); 
            Page page = localPage.get(); 
            page.setResult((List) result); 
            return result; 
        } 
    	return null; 
    } 
}
```













