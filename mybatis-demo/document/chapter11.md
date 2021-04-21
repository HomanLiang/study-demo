[toc]



# MyBatis 应用

## 1.大数据量下 MyBatis PageHelper 分页查询性能问题的解决办法

**前因**

项目一直使用的是PageHelper实现分页功能，项目前期数据量较少一直没有什么问题。随着业务扩增，数据库扩增PageHelper出现了明显的性能问题。

几十万甚至上百万的单表数据查询性能缓慢，需要几秒乃至十几秒的查询时间。故此特地研究了一下PageHelper源码，查找PageHelper分页的实现方式。

一段较为简单的查询，跟随debug开始源码探寻之旅。

```
public ResultContent select(Integer id) {
        Page<Test> blogPage = PageHelper.startPage(1,3).doSelectPage( () -> testDao.select(id));
        List<Test> test = (List<Test>)blogPage.getResult();
        return new ResultContent(0, "success", test);
    }
```

主要保存由前端传入的pageNum(页数)、pageSize(每页显示数量)和count(是否进行count(0)查询)信息。

这里是简单的创建page并保存当前线程的变量副本心里，不做深究。

```
public static <E> Page<E> startPage(int pageNum, int pageSize) {
        return startPage(pageNum, pageSize, DEFAULT_COUNT);
    }

    public static <E> Page<E> startPage(int pageNum, int pageSize, boolean count) {
        return startPage(pageNum, pageSize, count, (Boolean)null, (Boolean)null);
    }

    public static <E> Page<E> startPage(int pageNum, int pageSize, String orderBy) {
        Page<E> page = startPage(pageNum, pageSize);
        page.setOrderBy(orderBy);
        return page;
    }

    public static <E> Page<E> startPage(int pageNum, int pageSize, boolean count, Boolean reasonable, Boolean pageSizeZero) {
        Page<E> page = new Page(pageNum, pageSize, count);
        page.setReasonable(reasonable);
        page.setPageSizeZero(pageSizeZero);
        Page<E> oldPage = getLocalPage();
        if(oldPage != null && oldPage.isOrderByOnly()) {
            page.setOrderBy(oldPage.getOrderBy());
        }

        setLocalPage(page);
        return page;
    }
```

开始执行真正的select语句

```
public <E> Page<E> doSelectPage(ISelect select) {
        select.doSelect();
        return this;
    }
```

进入MapperProxy类执行invoke方法获取到方法名称及参数值

```
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (Object.class.equals(method.getDeclaringClass())) {
      try {
        return method.invoke(this, args);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    }
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    return mapperMethod.execute(sqlSession, args);
  }
```

接着是MapperMethod方法执行execute语句，判断是增、删、改、查。判断返回值是多个，进入executeForMany方法

```
public Object execute(SqlSession sqlSession, Object[] args) {
    Object result;
    if (SqlCommandType.INSERT == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.insert(command.getName(), param));
    } else if (SqlCommandType.UPDATE == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.update(command.getName(), param));
    } else if (SqlCommandType.DELETE == command.getType()) {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.delete(command.getName(), param));
    } else if (SqlCommandType.SELECT == command.getType()) {
      if (method.returnsVoid() && method.hasResultHandler()) {
        executeWithResultHandler(sqlSession, args);
        result = null;
      } else if (method.returnsMany()) {
        result = executeForMany(sqlSession, args);
      } else if (method.returnsMap()) {
        result = executeForMap(sqlSession, args);
      } else {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = sqlSession.selectOne(command.getName(), param);
      }
    } else if (SqlCommandType.FLUSH == command.getType()) {
        result = sqlSession.flushStatements();
    } else {
      throw new BindingException("Unknown execution method for: " + command.getName());
    }
    if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
      throw new BindingException("Mapper method '" + command.getName()
          + " attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
    }
    return result;
  }
```

这个方法开始调用SqlSessionTemplate、DefaultSqlSession等类获取到Mapper.xml文件的SQL语句

```
private <E> Object executeForMany(SqlSession sqlSession, Object[] args) {
    List<E> result;
    Object param = method.convertArgsToSqlCommandParam(args);
    if (method.hasRowBounds()) {
      RowBounds rowBounds = method.extractRowBounds(args);
      result = sqlSession.<E>selectList(command.getName(), param, rowBounds);
    } else {
      result = sqlSession.<E>selectList(command.getName(), param);
    }
    // issue #510 Collections & arrays support
    if (!method.getReturnType().isAssignableFrom(result.getClass())) {
      if (method.getReturnType().isArray()) {
        return convertToArray(result);
      } else {
        return convertToDeclaredCollection(sqlSession.getConfiguration(), result);
      }
    }
    return result;
  }
```

开始进入PageHelper的真正实现，Plugin通过实现InvocationHandler进行动态代理获取到相关信息

```
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      Set<Method> methods = signatureMap.get(method.getDeclaringClass());
      if (methods != null && methods.contains(method)) {
        return interceptor.intercept(new Invocation(target, method, args));
      }
      return method.invoke(target, args);
    } catch (Exception e) {
      throw ExceptionUtil.unwrapThrowable(e);
    }
  }
```

PageInterceptor 实现Mybatis的Interceptor 接口，进行拦截

```
public Object intercept(Invocation invocation) throws Throwable {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement)args[0];
            Object parameter = args[1];
            RowBounds rowBounds = (RowBounds)args[2];
            ResultHandler resultHandler = (ResultHandler)args[3];
            Executor executor = (Executor)invocation.getTarget();
            CacheKey cacheKey;
            BoundSql boundSql;
            if(args.length == 4) {
                boundSql = ms.getBoundSql(parameter);
                cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
            } else {
                cacheKey = (CacheKey)args[4];
                boundSql = (BoundSql)args[5];
            }

            this.checkDialectExists();
            List resultList;
            if(!this.dialect.skip(ms, parameter, rowBounds)) {
                if(this.dialect.beforeCount(ms, parameter, rowBounds)) {
                    Long count = this.count(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                    if(!this.dialect.afterCount(count.longValue(), parameter, rowBounds)) {
                        Object var12 = this.dialect.afterPage(new ArrayList(), parameter, rowBounds);
                        return var12;
                    }
                }

                resultList = ExecutorUtil.pageQuery(this.dialect, executor, ms, parameter, rowBounds, resultHandler, boundSql, cacheKey);
            } else {
                resultList = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            }

            Object var16 = this.dialect.afterPage(resultList, parameter, rowBounds);
            return var16;
        } finally {
            this.dialect.afterAll();
        }
    }
```

转到ExecutorUtil抽象类的pageQuery方法

```
public static <E> List<E> pageQuery(Dialect dialect, Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, CacheKey cacheKey) throws SQLException {
        if(!dialect.beforePage(ms, parameter, rowBounds)) {
            return executor.query(ms, parameter, RowBounds.DEFAULT, resultHandler, cacheKey, boundSql);
        } else {
            parameter = dialect.processParameterObject(ms, parameter, boundSql, cacheKey);
            String pageSql = dialect.getPageSql(ms, boundSql, parameter, rowBounds, cacheKey);
            BoundSql pageBoundSql = new BoundSql(ms.getConfiguration(), pageSql, boundSql.getParameterMappings(), parameter);
            Map<String, Object> additionalParameters = getAdditionalParameter(boundSql);
            Iterator var12 = additionalParameters.keySet().iterator();

            while(var12.hasNext()) {
                String key = (String)var12.next();
                pageBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
            }

            return executor.query(ms, parameter, RowBounds.DEFAULT, resultHandler, cacheKey, pageBoundSql);
        }
    }
```

在抽象类AbstractHelperDialect的getPageSql获取到对应的Page对象

```
public String getPageSql(MappedStatement ms, BoundSql boundSql, Object parameterObject, RowBounds rowBounds, CacheKey pageKey) {
        String sql = boundSql.getSql();
        Page page = this.getLocalPage();
        String orderBy = page.getOrderBy();
        if(StringUtil.isNotEmpty(orderBy)) {
            pageKey.update(orderBy);
            sql = OrderByParser.converToOrderBySql(sql, orderBy);
        }

        return page.isOrderByOnly()?sql:this.getPageSql(sql, page, pageKey);
    }
```

进入到MySqlDialect类的getPageSql方法进行SQL封装，根据page对象信息增加Limit。分页的信息就是这么拼装起来的

```
public String getPageSql(String sql, Page page, CacheKey pageKey) {
        StringBuilder sqlBuilder = new StringBuilder(sql.length() + 14);
        sqlBuilder.append(sql);
        if(page.getStartRow() == 0) {
            sqlBuilder.append(" LIMIT ? ");
        } else {
            sqlBuilder.append(" LIMIT ?, ? ");
        }

        return sqlBuilder.toString();
    }
```

将最后拼装好的SQL返回给DefaultSqlSession执行查询并返回

```
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
```

至此整个查询过程完成，原来PageHelper的分页功能是通过Limit拼接SQL实现的。查询效率低的问题也找出来了，那么应该如何解决。

首先分析SQL语句，limit在数据量少或者页数比较靠前的时候查询效率是比较高的。(单表数据量百万进行测试)

```
select * from user where age = 10 limit 1,10;结果显示0.43s
```

当where条件后的结果集较大并且页数达到一个量级整个SQL的查询效率就十分低下(哪怕where的条件加上了索引也不行)。

```
select * from user where age = 10 limit 100000,10;结果显示4.73s
```

那有什么解决方案呢？mysql就不能单表数据量超百万乃至千万嘛？答案是NO，显然是可以的。

```
SELECT a.* FROM USER a
INNER JOIN
    (SELECT id FROM USER WHERE age = 10 LIMIT 100000,10) b
ON a.id = b.id;
```

结果0.53s

完美解决了查询效率问题！！！其中需要对where条件增加索引，id因为是主键自带索引。select返回减少回表可以提升查询性能,所以采用查询主键字段后进行关联大幅度提升了查询效率。

PageHelper想要优化需要在拦截器的拼接SQL部分进行重构，由于博主能力有限暂未实现。能力较强的读者可以自己进行重构



## 2.MyBatis的多数据源配置（Spring Boot 2.x）
### 2.1.添加多数据源的配置
```
spring.datasource.primary.jdbc-url=jdbc:mysql://localhost:3306/test1
spring.datasource.primary.username=root
spring.datasource.primary.password=123456
spring.datasource.primary.driver-class-name=com.mysql.cj.jdbc.Driver

spring.datasource.secondary.jdbc-url=jdbc:mysql://localhost:3306/test2
spring.datasource.secondary.username=root
spring.datasource.secondary.password=123456
spring.datasource.secondary.driver-class-name=com.mysql.cj.jdbc.Driver
```
说明与注意：

1. 多数据源配置的时候，与单数据源不同点在于spring.datasource之后多设置一个数据源名称primary和secondary来区分不同的数据源配置，这个前缀将在后续初始化数据源的时候用到。
1. 数据源连接配置2.x和1.x的配置项是有区别的：2.x使用spring.datasource.secondary.jdbc-url，而1.x版本使用spring.datasource.secondary.url。如果你在配置的时候发生了这个报错java.lang.IllegalArgumentException: jdbcUrl is required with driverClassName.，那么就是这个配置项的问题。
1. 可以看到，不论使用哪一种数据访问框架，对于数据源的配置都是一样的。

### 2.2.初始化数据源与MyBatis配置
完成多数据源的配置信息之后，就来创建个配置类来加载这些配置信息，初始化数据源，以及初始化每个数据源要用的MyBatis配置。

这里我们继续将数据源与框架配置做拆分处理：

1. 单独建一个多数据源的配置类，比如下面这样：
```
@Configuration
public class DataSourceConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

}
```
可以看到内容跟JdbcTemplate、Spring Data JPA的时候是一模一样的。通过@ConfigurationProperties可以知道这两个数据源分别加载了spring.datasource.primary.*和spring.datasource.secondary.*的配置。@Primary注解指定了主数据源，就是当我们不特别指定哪个数据源的时候，就会使用这个Bean真正差异部分在下面的JPA配置上。

2. 分别创建两个数据源的MyBatis配置。
Primary数据源的JPA配置：
```
@Configuration
@MapperScan(
        basePackages = "com.didispace.chapter39.p",
        sqlSessionFactoryRef = "sqlSessionFactoryPrimary",
        sqlSessionTemplateRef = "sqlSessionTemplatePrimary")
public class PrimaryConfig {

    private DataSource primaryDataSource;

    public PrimaryConfig(@Qualifier("primaryDataSource") DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactoryPrimary() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(primaryDataSource);
        return bean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplatePrimary() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactoryPrimary());
    }

}
```
Secondary数据源的JPA配置：
```
@Configuration
@MapperScan(
        basePackages = "com.didispace.chapter39.s",
        sqlSessionFactoryRef = "sqlSessionFactorySecondary",
        sqlSessionTemplateRef = "sqlSessionTemplateSecondary")
public class SecondaryConfig {

    private DataSource secondaryDataSource;

    public SecondaryConfig(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        this.secondaryDataSource = secondaryDataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactorySecondary() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(secondaryDataSource);
        return bean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplateSecondary() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactorySecondary());
    }

}
```
说明与注意：

1. 配置类上使用@MapperScan注解来指定当前数据源下定义的Entity和Mapper的包路径；另外需要指定sqlSessionFactory和sqlSessionTemplate，这两个具体实现在该配置类中类中初始化。
1. 配置类的构造函数中，通过@Qualifier注解来指定具体要用哪个数据源，其名字对应在DataSourceConfiguration配置类中的数据源定义的函数名。
1. 配置类中定义SqlSessionFactory和SqlSessionTemplate的实现，注意具体使用的数据源正确（如果使用这里的演示代码，只要第二步没问题就不需要修改）。

上一篇介绍JPA的时候，因为之前介绍JPA的使用时候，说过实体和Repository定义的方法，所以省略了 User 和 Repository的定义代码，但是还是有读者问怎么没有这个，其实都有说明，仓库代码里也都是有的。未避免再问这样的问题，所以这里就贴一下吧。

根据上面Primary数据源的定义，在com.didispace.chapter39.p包下，定义Primary数据源要用的实体和数据访问对象，比如下面这样：
```
@Data
@NoArgsConstructor
public class UserPrimary {

    private Long id;

    private String name;
    private Integer age;

    public UserPrimary(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}

public interface UserMapperPrimary {

    @Select("SELECT * FROM USER WHERE NAME = #{name}")
    UserPrimary findByName(@Param("name") String name);

    @Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
    int insert(@Param("name") String name, @Param("age") Integer age);

    @Delete("DELETE FROM USER")
    int deleteAll();

}
```
根据上面Secondary数据源的定义，在com.didispace.chapter39.s包下，定义Secondary数据源要用的实体和数据访问对象，比如下面这样：
```
@Data
@NoArgsConstructor
public class UserSecondary {

    private Long id;

    private String name;
    private Integer age;

    public UserSecondary(String name, Integer age) {
        this.name = name;
        this.age = age;
    }
}

public interface UserMapperSecondary {

    @Select("SELECT * FROM USER WHERE NAME = #{name}")
    UserSecondary findByName(@Param("name") String name);

    @Insert("INSERT INTO USER(NAME, AGE) VALUES(#{name}, #{age})")
    int insert(@Param("name") String name, @Param("age") Integer age);

    @Delete("DELETE FROM USER")
    int deleteAll();
}
```
### 2.3.测试验证
完成了上面之后，我们就可以写个测试类来尝试一下上面的多数据源配置是否正确了，先来设计一下验证思路：

1. 往Primary数据源插入一条数据
1. 从Primary数据源查询刚才插入的数据，配置正确就可以查询到
1. 从Secondary数据源查询刚才插入的数据，配置正确应该是查询不到的
1. 往Secondary数据源插入一条数据
1. 从Primary数据源查询刚才插入的数据，配置正确应该是查询不到的
1. 从Secondary数据源查询刚才插入的数据，配置正确就可以查询到

具体实现如下：
```
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class Chapter39ApplicationTests {

    @Autowired
    private UserMapperPrimary userMapperPrimary;
    @Autowired
    private UserMapperSecondary userMapperSecondary;

    @Before
    public void setUp() {
        // 清空测试表，保证每次结果一样
        userMapperPrimary.deleteAll();
        userMapperSecondary.deleteAll();
    }

    @Test
    public void test() throws Exception {
        // 往Primary数据源插入一条数据
        userMapperPrimary.insert("AAA", 20);

        // 从Primary数据源查询刚才插入的数据，配置正确就可以查询到
        UserPrimary userPrimary = userMapperPrimary.findByName("AAA");
        Assert.assertEquals(20, userPrimary.getAge().intValue());

        // 从Secondary数据源查询刚才插入的数据，配置正确应该是查询不到的
        UserSecondary userSecondary = userMapperSecondary.findByName("AAA");
        Assert.assertNull(userSecondary);

        // 往Secondary数据源插入一条数据
        userMapperSecondary.insert("BBB", 20);

        // 从Primary数据源查询刚才插入的数据，配置正确应该是查询不到的
        userPrimary = userMapperPrimary.findByName("BBB");
        Assert.assertNull(userPrimary);

        // 从Secondary数据源查询刚才插入的数据，配置正确就可以查询到
        userSecondary = userMapperSecondary.findByName("BBB");
        Assert.assertEquals(20, userSecondary.getAge().intValue());
    }

}
```