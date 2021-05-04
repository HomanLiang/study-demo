[toc]



# Spring 事务

## 事务类型

- 划分本地事务和分布式事务：
  - 本地事务：就是普通事务，能保证单台数据库上的操作的ACID,被限定在一台数据库上
  - 分布式事务：涉及多个数据源的事务，即跨越多台同类或异类数据库的事务（由每台数据库的本地事务组务），分布式事务旨在保证这些本地事务的所有操作的ACID，使事务可以跨越多台数据库；

- 划分JDBC事务和JTA事务：
  - JDBC事务：就是数据库事务中的本地事务。通过Connection对象的控制来管理事务
  - JTA指(Java Transaction API),是Java EE数据库事务规范，JTA只提供了事务管理接口，由应用程序服务器厂商提供实现 ，JTA事务比JDBC更强大，支持分布式事务

- 按是否通过编程实现事务:
  - 编程式事务：通过编写代码来管理事务
  - 通过注解或XML配置来管理事务



## Spring事务管理

 Spring的事务管理主要包括3个接口：

- **PlatformTransactionManager**：根据TransactionDefinition提供的事务属性配置信息，创建事务.

- **TransactionDefinition**：封状事务的隔离级别、超时时间、是否只读事务和传播规则等事务属性.

- **TransactionStatus**：封装了事务的具体运行状态，如是否是新事务，是否已经提交事务，设置当前事务为rollback-only等；

### PlatformTransactionManager

接口统一抽象处理事务操作相关的方法，是其他事务的规范,方法解析:

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329233952.png)

- `TransactionStatus getTransaction(@Nullable TransactionDefinition definition)`：根据事务定义信息从事事务环境返回一个已存在的事务，或者创建一个新的事务。

- `void commit(TransactionStatus status)`：根据事务的状态提交事务，如果事务状态已经标识为rollback-only,该方法执行回滚事务的操作

- `void rollback(TransactionStatus status)`：将事务回滚，当commit方法抛出异常时，rollback会被隐式调用 

常用的事务管理器: 

- **DataSourceTransactionManager**:支持JDBC,MyBatis等；

- **HibernateTransactionManager**:支持Hibernate

###  TransactionDefinition

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329234215.png)

- **事务隔离级别**：用来解决并发事务出现的问题
  - `ISOLATION_DEFAULT`：默认隔离级别，即使用底层数据库默认的隔离级别；
  - `ISOLATION_READ_UNCOMMITTED`：未提交读
  - `ISOLATION_READ_COMMITTED `：提交读，一般情况我们使用这个
  - `ISOLATION_REPEATABLE_READ` ：可重复读
  - `ISOLATION_SERIALIZABLE` : 序列化

注：除第一个外，后面四个都是spring通过java代码模拟出来的

- **传播规则**：在一个事务中调用其他事务方法，此时事务该如何传播，按照什么规则传播，用谁的事务，还是都不用等

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329234237.png)

  Spring共支持7种传播行为：

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329234248.png)

  **情况一：遵从当前事务**
  
  - REQUIRED:必须存在事务，如果当前存在一个事务，则加入该事务，否则将新建一个事务（缺省）
  - SUPPORTS：支持当前事务，指如果当前存在逻辑事务，就加入到该事务，如果当前没有事务，就以非事务方式执行
- MANDATORY：必须有事务，使用当前事务执行，如果当前没有事务，则抛出异常IllegalTransactionStateException
  
  **情况二：不遵从当前事务**
  
  - REQUIRES_NEW：不管当前是否存在事务，每次都创建新事务
- NOT_SUPPORTED：以非事务方式执行，如果当前存在事务，就把当前事务暂停，以非事务方式执行
  - NEVER：不支持事务，如果当前存在事务，则抛出异常：IllegalTransactionStateException
  
  **情况三：寄生事务（外部事务和寄生事务）**
  
  - NESTED：如果当前存在事务，则在内部事务内执行，如果当前不存在事务，则创建一个新的事务，嵌套事务使用数据库中的保存点来实现，即嵌套事务回滚不影响外部事务，但外部事务回滚将导致嵌套事务回滚。 



## 使用XML配置JDBC事务 

 **1.表account结构**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329234550.png)

**2.domain类**

```
@Data
public class Account {
    private Long id;
    private int balance;
}
```

**3.dao接口及实现类**

    public interface IAccountDAO {
     
        /**
         * 从指定帐户转出多少钱
         * @param outId
         * @param money
         */
        void transOut(Long outId,int money);
        
        /**
         * 从指定帐户转入多少钱
         * @param inId
         * @param money
         */
        void transIn(Long inId,int money);
    }
    
    public class AccountDAOImpl implements IAccountDAO {
     
        private JdbcTemplate jdbcTemplate;
        public void setDataSource(DataSource ds) {
            this.jdbcTemplate = new JdbcTemplate(ds);
        }
        
        @Override
        public void transOut(Long outId, int money) {
            System.out.println("outId:"+outId+",money:"+money);
            this.jdbcTemplate.update("update account set balance = balance - ? where id=?", money,outId);
     
        }
     
        @Override
        public void transIn(Long inId, int money) {
            System.out.println("inId:"+inId+",money:"+money);
            this.jdbcTemplate.update("update account set balance = balance + ? where id=?", money,inId);
     
        }
     
    }

**3.service接口及实现类**

    public interface IAccountService {
        /**
         * 从指定帐户转出另一个帐户多少钱
         * @param outId
         * @param inId
         * @param money
         */
        void trans(Long outId,Long inId,int money);
    }

```
public class AccountServiceImpl implements IAccountService {
 
    private IAccountDAO dao;
    
    public void setDao(IAccountDAO dao) {
        this.dao = dao;
    }
    
    @Override
    public void trans(Long outId, Long inId, int money) {
        //转出
        this.dao.transOut(outId, money);
        //转入
        int a = 1/0;//模拟异常
        this.dao.transIn(inId, money);
 
    }
 
}
```

**4.XML配置**

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xmlns:aop="http://www.springframework.org/schema/aop"
    xmlns:tx="http://www.springframework.org/schema/tx"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/aop
        http://www.springframework.org/schema/aop/spring-aop.xsd
        http://www.springframework.org/schema/tx
        http://www.springframework.org/schema/tx/spring-tx.xsd">
 
	<!-- 从classpath的根路径去加载db.properties文件 -->
	<context:property-placeholder location="classpath:db.properties" system-properties-mode="NEVER" />
 
	<!-- 配置一个druid的连接池 -->
	<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close">
		<property name="driverClassName" value="${jdbc.driverClassName}" />
		<property name="url" value="${jdbc.url}" />
		<property name="username" value="${jdbc.username}" />
		<property name="password" value="${jdbc.password}" />
		<property name="initialSize" value="${jdbc.initialSize}" />
	</bean>
	
	<!-- DAO配置 -->
	<bean id="accountDAO" class="com.bigfong.txxml.dao.impl.AccountDAOImpl">
		<property name="dataSource" ref="dataSource"/>
	</bean>
	<!-- Service配置 -->
	<bean id="accountService" class="com.bigfong.txxml.service.impl.AccountServiceImpl">
		<property name="dao" ref="accountDAO"/>
	</bean>
	
	<!-- =============配置事务 start============= -->
	<!-- 1:WHAT 配置JDBC事务管理器 -->
	<bean id="txManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
		<property name="dataSource" ref="dataSource"/>
	</bean>
	<!-- 2:WHEN配置事务管理器增强 -->
	<tx:advice id="txAdvice" transaction-manager="txManager">
		<tx:attributes>
			<tx:method name="trans"/>
		</tx:attributes>
	</tx:advice>
	<!-- 3:WHERE 配置切面 -->
	<aop:config>
		<!-- 接口路径 -->
		<aop:pointcut expression="execution(* com.bigfong.txxml.service.*Service.*(..))" id="txPC"/>
		<aop:advisor advice-ref="txAdvice" pointcut-ref="txPC"/>
	</aop:config>
	<!-- =============配置事务 end============= -->
</beans>
```

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329234813.png)

**注意以上关联关系** 

**5.测试代码**

    @SpringJUnitConfig
    public class App {
        
        @Autowired
        private IAccountService service;
        
        @Test
        void testTrans() {
            service.trans(10002L, 10010L, 100);
        }
    }
**6.tx:method标签设置**

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329234932.png)

- name：匹配到的方法模拟，必须配置；

- read-only:如果为true,开启一个只读事务，只读事务的性能较高，但是不能只读事务中操作DML;

- isolation:代表数据库事务隔离级别（就使用默认），DEFAULT：让Spring使用数据库默认的事务隔离级别；其他：Spring模拟

- no-rollback-for:如果遇到的异常是匹配的异常类型，就不回滚事务

- rollback-for:如果遇到的异常是指定匹配的异常类型，才回滚事务；

- propagation:事务的传播方式（当一个方法已在一个开启的事务当中了，应该怎么处理自身的事务）；

**7.配置一个CRUD通用的事务配置**

```
<tx:advice id="crudAdvice" transaction-manager="txManager">
	<tx:attributes>
		<tx:method name="get*" read-only="true" propagation="REQUIRED"/>
		<tx:method name="list*" read-only="true" propagation="REQUIRED"/>
		<tx:method name="query*" read-only="true" propagation="REQUIRED"/>
		<!-- service其他方法（非查询方法） -->
		<tx:method name="*" propagation="REQUIRED"/>
	</tx:attributes>
</tx:advice>
```



## 使用注解配置JDBC事务

**1.XML配置**

```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xmlns:aop="http://www.springframework.org/schema/aop"
    xmlns:tx="http://www.springframework.org/schema/tx"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/aop
        http://www.springframework.org/schema/aop/spring-aop.xsd
        http://www.springframework.org/schema/tx
        http://www.springframework.org/schema/tx/spring-tx.xsd">
 
	<!-- 从classpath的根路径去加载db.properties文件 -->
	<context:property-placeholder location="classpath:db.properties" system-properties-mode="NEVER" />
 
	<!-- 配置一个druid的连接池 -->
	<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource" init-method="init" destroy-method="close">
		<property name="driverClassName" value="${jdbc.driverClassName}" />
		<property name="url" value="${jdbc.url}" />
		<property name="username" value="${jdbc.username}" />
		<property name="password" value="${jdbc.password}" />
		<property name="initialSize" value="${jdbc.initialSize}" />
	</bean>
	
	<!-- DI注解解析器 -->
	<context:annotation-config/>
	<!-- Ioc注解解析器 -->
	<context:component-scan base-package="com.bigfong.txcode"/>
	<!-- 配置事务管理器 -->
	<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
		<property name="dataSource" ref="dataSource"/>
	</bean>
	<!-- TX注解解析器 -->
	<tx:annotation-driven transaction-manager="transactionManager"/>
 
</beans>
```

**2.domain类似上面JDBC的方式**

**3.dao接口同上，实现类如下，添加注解:@Repository**

```
@Repository
public class AccountDAOImpl implements IAccountDAO {
 
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    public void setDataSource(DataSource ds) {
        this.jdbcTemplate = new JdbcTemplate(ds);
    }
    
    @Override
    public void transOut(Long outId, int money) {
        System.out.println("outId:"+outId+",money:"+money);
        this.jdbcTemplate.update("update account set balance = balance - ? where id=?", money,outId);
 
    }
 
    @Override
    public void transIn(Long inId, int money) {
        System.out.println("inId:"+inId+",money:"+money);
        this.jdbcTemplate.update("update account set balance = balance + ? where id=?", money,inId);
 
    }
 
}
```

**4.service接口同上，实现类如下，添加注解:@Service@Transactional**

```
@Service
@Transactional
public class AccountServiceImpl implements IAccountService {
 
    @Autowired
    private IAccountDAO dao;
 
    @Override
    public void trans(Long outId, Long inId, int money) {
        //转出
        this.dao.transOut(outId, money);
        //转入
        int a = 1/0;//模拟异常
        this.dao.transIn(inId, money);
 
    }
    
    @Transactional(readOnly=true)
    public void  listAcount() {
        
    }
 
}
```

可以在指定方法配置指定规则，如上述: @Transactional(readOnly=true)

**5.测试类同上** 



## Spring事务传播行为

Spring 在 TransactionDefinition 接口中规定了 7 种类型的事务传播行为。事务传播行为是 Spring 框架独有的事务增强特性，他不属于的事务实际提供方数据库行为。

这是 Spring 为我们提供的强大的工具箱，使用事务传播行可以为我们的开发工作提供许多便利。

但是人们对他的误解也颇多，你一定也听过“service 方法事务最好不要嵌套”的传言。

### 基础概念

#### 1. 什么是事务传播行为？

事务传播行为用来描述由某一个事务传播行为修饰的方法被嵌套进另一个方法的时事务如何传播。

用伪代码说明：

```
 public void methodA(){
    methodB();
    //doSomething
 }

 @Transaction(Propagation=XXX)
 public void methodB(){
    //doSomething
 }
```

代码中`methodA()`方法嵌套调用了`methodB()`方法，`methodB()`的事务传播行为由`@Transaction(Propagation=XXX)`设置决定。这里需要注意的是`methodA()`并没有开启事务，某一个事务传播行为修饰的方法并不是必须要在开启事务的外围方法中调用。

#### 2. Spring 中七种事务传播行为

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330001632.png)

定义非常简单，也很好理解，下面我们就进入代码测试部分，验证我们的理解是否正确。

### 代码验证

文中代码以传统三层结构中两层呈现，即 Service 和 Dao 层，由 Spring 负责依赖注入和注解式事务管理，DAO 层由 Mybatis 实现，你也可以使用任何喜欢的方式，例如，Hibernate,JPA,JDBCTemplate 等。数据库使用的是 MySQL 数据库，你也可以使用任何支持事务的数据库，并不会影响验证结果。

首先我们在数据库中创建两张表：

**user1**

```
CREATE TABLE `user1` (
  `id` INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL DEFAULT '',
  PRIMARY KEY(`id`)
)
ENGINE = InnoDB;
```

**user2**

```
CREATE TABLE `user2` (
  `id` INTEGER UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL DEFAULT '',
  PRIMARY KEY(`id`)
)
ENGINE = InnoDB;
```

然后编写相应的 Bean 和 DAO 层代码：

**User1**

```
public class User1 {
    private Integer id;
    private String name;
   //get和set方法省略...
}
```

**User2**

```
public class User2 {
    private Integer id;
    private String name;
   //get和set方法省略...
}
```

**User1Mapper**

```
public interface User1Mapper {
    int insert(User1 record);
    User1 selectByPrimaryKey(Integer id);
    //其他方法省略...
}
```

**User2Mapper**

```
public interface User2Mapper {
    int insert(User2 record);
    User2 selectByPrimaryKey(Integer id);
    //其他方法省略...
}
```

最后也是具体验证的代码由 service 层实现，下面我们分情况列举。

#### 1.PROPAGATION_REQUIRED

我们为 User1Service 和 User2Service 相应方法加上`Propagation.REQUIRED`属性。

**User1Service 方法：**

```
@Service
public class User1ServiceImpl implements User1Service {
    //省略其他...
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void addRequired(User1 user){
        user1Mapper.insert(user);
    }
}
```

**User2Service 方法：**

```
@Service
public class User2ServiceImpl implements User2Service {
    //省略其他...
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void addRequired(User2 user){
        user2Mapper.insert(user);
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void addRequiredException(User2 user){
        user2Mapper.insert(user);
        throw new RuntimeException();
    }

}
```

##### 1.1 场景一

此场景外围方法没有开启事务。

**验证方法 1：**

```
    @Override
    public void notransaction_exception_required_required(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequired(user2);

        throw new RuntimeException();
    }
```

**验证方法 2：**

```
    @Override
    public void notransaction_required_required_exception(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiredException(user2);
    }
```

分别执行验证方法，结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330001719.webp)

**结论：通过这两个方法我们证明了在外围方法未开启事务的情况下`Propagation.REQUIRED`修饰的内部方法会新开启自己的事务，且开启的事务相互独立，互不干扰。**

##### 1.2 场景二

外围方法开启事务，这个是使用率比较高的场景。

**验证方法 1：**

```
   @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void transaction_exception_required_required(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequired(user2);

        throw new RuntimeException();
    }
```

**验证方法 2：**

```
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void transaction_required_required_exception(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiredException(user2);
    }
```

**验证方法 3：**

```
    @Transactional
    @Override
    public void transaction_required_required_exception_try(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        try {
            user2Service.addRequiredException(user2);
        } catch (Exception e) {
            System.out.println("方法回滚");
        }
    }
```

分别执行验证方法，结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330001949.webp)

**结论：以上试验结果我们证明在外围方法开启事务的情况下`Propagation.REQUIRED`修饰的内部方法会加入到外围方法的事务中，所有`Propagation.REQUIRED`修饰的内部方法和外围方法均属于同一事务，只要一个方法回滚，整个事务均回滚。**

#### 2.PROPAGATION_REQUIRES_NEW

我们为 User1Service 和 User2Service 相应方法加上`Propagation.REQUIRES_NEW`属性。

**User1Service 方法：**

```
@Service
public class User1ServiceImpl implements User1Service {
    //省略其他...
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addRequiresNew(User1 user){
        user1Mapper.insert(user);
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void addRequired(User1 user){
        user1Mapper.insert(user);
    }
}
```

**User2Service 方法：**

```
@Service
public class User2ServiceImpl implements User2Service {
    //省略其他...
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addRequiresNew(User2 user){
        user2Mapper.insert(user);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addRequiresNewException(User2 user){
        user2Mapper.insert(user);
        throw new RuntimeException();
    }
}
```

##### 2.1 场景一

外围方法没有开启事务。

**验证方法 1：**

```
    @Override
    public void notransaction_exception_requiresNew_requiresNew(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequiresNew(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiresNew(user2);
        throw new RuntimeException();

    }
```

**验证方法 2：**

```
    @Override
    public void notransaction_requiresNew_requiresNew_exception(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequiresNew(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiresNewException(user2);
    }
```

分别执行验证方法，结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330001942.webp)

**结论：通过这两个方法我们证明了在外围方法未开启事务的情况下`Propagation.REQUIRES_NEW`修饰的内部方法会新开启自己的事务，且开启的事务相互独立，互不干扰。**

##### 2.2 场景二

外围方法开启事务。

**验证方法 1：**

```
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void transaction_exception_required_requiresNew_requiresNew(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiresNew(user2);

        User2 user3=new User2();
        user3.setName("王五");
        user2Service.addRequiresNew(user3);
        throw new RuntimeException();
    }
```

**验证方法 2：**

```
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void transaction_required_requiresNew_requiresNew_exception(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiresNew(user2);

        User2 user3=new User2();
        user3.setName("王五");
        user2Service.addRequiresNewException(user3);
    }
```

**验证方法 3：**

```
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void transaction_required_requiresNew_requiresNew_exception_try(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addRequired(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addRequiresNew(user2);
        User2 user3=new User2();
        user3.setName("王五");
        try {
            user2Service.addRequiresNewException(user3);
        } catch (Exception e) {
            System.out.println("回滚");
        }
    }
```

分别执行验证方法，结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330002015.png)

**结论：在外围方法开启事务的情况下`Propagation.REQUIRES_NEW`修饰的内部方法依然会单独开启独立事务，且与外部方法事务也独立，内部方法之间、内部方法和外部方法事务均相互独立，互不干扰。**

#### 3.PROPAGATION_NESTED

我们为 User1Service 和 User2Service 相应方法加上`Propagation.NESTED`属性。**User1Service 方法：**

```
@Service
public class User1ServiceImpl implements User1Service {
    //省略其他...
    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void addNested(User1 user){
        user1Mapper.insert(user);
    }
}
```

**User2Service 方法：**

```
@Service
public class User2ServiceImpl implements User2Service {
    //省略其他...
    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void addNested(User2 user){
        user2Mapper.insert(user);
    }

    @Override
    @Transactional(propagation = Propagation.NESTED)
    public void addNestedException(User2 user){
        user2Mapper.insert(user);
        throw new RuntimeException();
    }
}
```

##### 3.1 场景一

此场景外围方法没有开启事务。

**验证方法 1：**

```
    @Override
    public void notransaction_exception_nested_nested(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addNested(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addNested(user2);
        throw new RuntimeException();
    }
```

**验证方法 2：**

```
    @Override
    public void notransaction_nested_nested_exception(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addNested(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addNestedException(user2);
    }
```

分别执行验证方法，结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330002030.png)

**结论：通过这两个方法我们证明了在外围方法未开启事务的情况下`Propagation.NESTED`和`Propagation.REQUIRED`作用相同，修饰的内部方法都会新开启自己的事务，且开启的事务相互独立，互不干扰。**

##### 3.2 场景二

外围方法开启事务。

**验证方法 1：**

```
    @Transactional
    @Override
    public void transaction_exception_nested_nested(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addNested(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addNested(user2);
        throw new RuntimeException();
    }
```

**验证方法 2：**

```
    @Transactional
    @Override
    public void transaction_nested_nested_exception(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addNested(user1);

        User2 user2=new User2();
        user2.setName("李四");
        user2Service.addNestedException(user2);
    }
```

**验证方法 3：**

```
    @Transactional
    @Override
    public void transaction_nested_nested_exception_try(){
        User1 user1=new User1();
        user1.setName("张三");
        user1Service.addNested(user1);

        User2 user2=new User2();
        user2.setName("李四");
        try {
            user2Service.addNestedException(user2);
        } catch (Exception e) {
            System.out.println("方法回滚");
        }
    }
```

分别执行验证方法，结果：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330002040.png)

**结论：以上试验结果我们证明在外围方法开启事务的情况下`Propagation.NESTED`修饰的内部方法属于外部事务的子事务，外围主事务回滚，子事务一定回滚，而内部子事务可以单独回滚而不影响外围主事务和其他子事务**

#### 4. REQUIRED,REQUIRES_NEW,NESTED 异同

由“1.2 场景二”和“3.2 场景二”对比，我们可知：**NESTED 和 REQUIRED 修饰的内部方法都属于外围方法事务，如果外围方法抛出异常，这两种方法的事务都会被回滚。但是 REQUIRED 是加入外围方法事务，所以和外围事务同属于一个事务，一旦 REQUIRED 事务抛出异常被回滚，外围方法事务也将被回滚。而 NESTED 是外围方法的子事务，有单独的保存点，所以 NESTED 方法抛出异常被回滚，不会影响到外围方法的事务。**

由“2.2 场景二”和“3.2 场景二”对比，我们可知：**NESTED 和 REQUIRES_NEW 都可以做到内部方法事务回滚而不影响外围方法事务。但是因为 NESTED 是嵌套事务，所以外围方法回滚之后，作为外围方法事务的子事务也会被回滚。而 REQUIRES_NEW 是通过开启新的事务实现的，内部事务和外围事务是两个事务，外围事务回滚不会影响内部事务。**

#### 5. 其他事务传播行为

鉴于文章篇幅问题，其他事务传播行为的测试就不在此一一描述了，感兴趣的读者可以去源码中自己寻找相应测试代码和结果解释。传送门：**https://github.com/TmTse/transaction-test**

### 模拟用例

介绍了这么多事务传播行为，我们在实际工作中如何应用呢？下面我来举一个示例：

假设我们有一个注册的方法，方法中调用添加积分的方法，如果我们希望添加积分不会影响注册流程（即添加积分执行失败回滚不能使注册方法也回滚），我们会这样写：

```
   @Service
   public class UserServiceImpl implements UserService {

        @Transactional
        public void register(User user){

            try {
                membershipPointService.addPoint(Point point);
            } catch (Exception e) {
               //省略...
            }
            //省略...
        }
        //省略...
   }
```

我们还规定注册失败要影响`addPoint()`方法（注册方法回滚添加积分方法也需要回滚），那么`addPoint()`方法就需要这样实现：

```
   @Service
   public class MembershipPointServiceImpl implements MembershipPointService{

        @Transactional(propagation = Propagation.NESTED)
        public void addPoint(Point point){

            try {
                recordService.addRecord(Record record);
            } catch (Exception e) {
               //省略...
            }
            //省略...
        }
        //省略...
   }
```

我们注意到了在`addPoint()`中还调用了`addRecord()`方法，这个方法用来记录日志。他的实现如下：

```
   @Service
   public class RecordServiceImpl implements RecordService{

        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        public void addRecord(Record record){


            //省略...
        }
        //省略...
   }
```

我们注意到`addRecord()`方法中`propagation = Propagation.NOT_SUPPORTED`，因为对于日志无所谓精确，可以多一条也可以少一条，所以`addRecord()`方法本身和外围`addPoint()`方法抛出异常都不会使`addRecord()`方法回滚，并且`addRecord()`方法抛出异常也不会影响外围`addPoint()`方法的执行。

通过这个例子相信大家对事务传播行为的使用有了更加直观的认识，通过各种属性的组合确实能让我们的业务实现更加灵活多样。





## X.常见问题

### X.1.事务失效

事务失效我们一般要从两个方面排查问题

#### X.1.1.数据库层面

数据库层面，数据库使用的存储引擎是否支持事务？默认情况下MySQL数据库使用的是Innodb存储引擎（5.5版本之后），它是支持事务的，但是如果你的表特地修改了存储引擎，例如，你通过下面的语句修改了表使用的存储引擎为`MyISAM`，而`MyISAM`又是不支持事务的

```sql
alter table table_name engine=myisam;
```

这样就会出现“事务失效”的问题了

**解决方案**：修改存储引擎为`Innodb`。

#### X.1.2.业务代码层面

业务层面的代码是否有问题，这就有很多种可能了

**X.1.2.1.**我们要使用Spring的申明式事务，那么需要执行事务的Bean是否已经交由了Spring管理？在代码中的体现就是类上是否有`@Service`、`Component`等一系列注解

**解决方案：**将Bean交由Spring进行管理（添加`@Service`注解）

**X.1.2.2.**`@Transactional`注解是否被放在了合适的位置。在上篇文章中我们对Spring中事务失效的原理做了详细的分析，其中也分析了Spring内部是如何解析`@Transactional`注解的，我们稍微回顾下代码：

![image-20200818152357704](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000020.png)

> 代码位于：`AbstractFallbackTransactionAttributeSource#computeTransactionAttribute`中

也就是说，默认情况下你无法使用`@Transactional`对一个非public的方法进行事务管理

**解决方案：**修改需要事务管理的方法为`public`。

**X.1.2.3.**出现了自调用。什么是自调用呢？我们看个例子

```java
@Service
public class DmzService {
	
	public void saveAB(A a, B b) {
		saveA(a);
		saveB(b);
	}

	@Transactional
	public void saveA(A a) {
		dao.saveA(a);
	}
	
	@Transactional
	public void saveB(B b){
		dao.saveB(a);
	}
}
```

上面三个方法都在同一个类`DmzService`中，其中`saveAB`方法中调用了本类中的`saveA`跟`saveB`方法，这就是自调用。在上面的例子中`saveA`跟`saveB`上的事务会失效

那么自调用为什么会导致事务失效呢？我们知道Spring中事务的实现是依赖于`AOP`的，当容器在创建`dmzService`这个Bean时，发现这个类中存在了被`@Transactional`标注的方法（修饰符为public）那么就需要为这个类创建一个代理对象并放入到容器中，创建的代理对象等价于下面这个类

```java
public class DmzServiceProxy {

    private DmzService dmzService;

    public DmzServiceProxy(DmzService dmzService) {
        this.dmzService = dmzService;
    }

    public void saveAB(A a, B b) {
        dmzService.saveAB(a, b);
    }

    public void saveA(A a) {
        try {
            // 开启事务
            startTransaction();
            dmzService.saveA(a);
        } catch (Exception e) {
            // 出现异常回滚事务
            rollbackTransaction();
        }
        // 提交事务
        commitTransaction();
    }

    public void saveB(B b) {
        try {
            // 开启事务
            startTransaction();
            dmzService.saveB(b);
        } catch (Exception e) {
            // 出现异常回滚事务
            rollbackTransaction();
        }
        // 提交事务
        commitTransaction();
    }
}
```

上面是一段伪代码，通过`startTransaction`、`rollbackTransaction`、`commitTransaction`这三个方法模拟代理类实现的逻辑。因为目标类`DmzService`中的`saveA`跟`saveB`方法上存在`@Transactional`注解，所以会对这两个方法进行拦截并嵌入事务管理的逻辑，同时`saveAB`方法上没有`@Transactional`，相当于代理类直接调用了目标类中的方法。

我们会发现当通过代理类调用`saveAB`时整个方法的调用链如下：

![äºå¡å¤±æ](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000152.png)

实际上我们在调用`saveA`跟`saveB`时调用的是目标类中的方法，这种清空下，事务当然会失效。

常见的自调用导致的事务失效还有一个例子，如下：

```java
@Service
public class DmzService {
	@Transactional
	public void save(A a, B b) {
		saveB(b);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveB(B b){
		dao.saveB(a);
	}
}
```

当我们调用`save`方法时，我们预期的执行流程是这样的

![äºå¡å¤±æï¼èªè°ç¨requires_newï¼](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000207.png)

也就是说两个事务之间互不干扰，每个事务都有自己的开启、回滚、提交操作。

但根据之前的分析我们知道，实际上在调用saveB方法时，是直接调用的目标类中的saveB方法，在saveB方法前后并不会有事务的开启或者提交、回滚等操作，实际的流程是下面这样的

![事务失效（自调用requires_new）执行流程](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210329235521.png)

由于saveB方法实际上是由dmzService也就是目标类自己调用的，所以在saveB方法的前后并不会执行事务的相关操作。这也是自调用带来问题的根本原因：**自调用时，调用的是目标类中的方法而不是代理类中的方法**

**解决方案**：

1. 自己注入自己，然后显示的调用，例如：

   ```java
   @Service
   public class DmzService {
   	// 自己注入自己
   	@Autowired
   	DmzService dmzService;
   	
   	@Transactional
   	public void save(A a, B b) {
   		dmzService.saveB(b);
   	}
   
   	@Transactional(propagation = Propagation.REQUIRES_NEW)
   	public void saveB(B b){
   		dao.saveB(a);
   	}
   }
   ```

   这种方案看起来不是很优雅

2. 利用`AopContext`，如下：

   ```java
   @Service
   public class DmzService {
   
   	@Transactional
   	public void save(A a, B b) {
   		((DmzService) AopContext.currentProxy()).saveB(b);
   	}
   
   	@Transactional(propagation = Propagation.REQUIRES_NEW)
   	public void saveB(B b){
   		dao.saveB(a);
   	}
   }
   ```

   > 使用上面这种解决方案需要注意的是，需要在配置类上新增一个配置
   >
   > ```java
   > // exposeProxy=true代表将代理类放入到线程上下文中，默认是false
   > @EnableAspectJAutoProxy(exposeProxy = true)
   > ```

   个人比较喜欢的是第二种方式

这里我们做个来做个小总结

#### X.1.3.总结

![äºå¡å¤±æçåå ](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000240.png)

### X.2.事务回滚相关问题

回滚相关的问题可以被总结为两句话

1. 想回滚的时候事务确提交了
2. 想提交的时候被标记成只能回滚了（rollback only）

先看第一种情况：**想回滚的时候事务确提交了**。这种情况往往是程序员对Spring中事务的`rollbackFor`属性不够了解导致的。

> Spring默认抛出了未检查`unchecked`异常（继承自 `RuntimeException` 的异常）或者 `Error`才回滚事务；其他异常不会触发回滚事务，已经执行的SQL会提交掉。如果在事务中抛出其他类型的异常，但却期望 Spring 能够回滚事务，就需要指定`rollbackFor`属性。

对应代码其实我们上篇文章也分析过了，如下：

![image-20200818195112983](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000301.png)

> 以上代码位于：`TransactionAspectSupport#completeTransactionAfterThrowing`方法中

默认情况下，只有出现`RuntimeException`或者`Error`才会回滚

```java
public boolean rollbackOn(Throwable ex) {
    return (ex instanceof RuntimeException || ex instanceof Error);
}
```

所以，如果你想在出现了非`RuntimeException`或者`Error`时也回滚，请指定回滚时的异常，例如：

```java
@Transactional(rollbackFor = Exception.class)
```

第二种情况：**想提交的时候被标记成只能回滚了（rollback only）**。

对应的异常信息如下：

```java
Transaction rolled back because it has been marked as rollback-only
```

我们先来看个例子吧

```java
@Service
public class DmzService {

	@Autowired
	IndexService indexService;

	@Transactional
	public void testRollbackOnly() {
		try {
			indexService.a();
		} catch (ClassNotFoundException e) {
			System.out.println("catch");
		}
	}
}

@Service
public class IndexService {
	@Transactional(rollbackFor = Exception.class)
	public void a() throws ClassNotFoundException{
		// ......
		throw new ClassNotFoundException();
	}
}
```

在上面这个例子中，`DmzService`的`testRollbackOnly`方法跟`IndexService`的`a`方法都开启了事务，并且事务的传播级别为`required`，所以当我们在`testRollbackOnly`中调用`IndexService`的`a`方法时这两个方法应当是共用的一个事务。按照这种思路，虽然`IndexService`的`a`方法抛出了异常，但是我们在`testRollbackOnly`将异常捕获了，那么这个事务应该是可以正常提交的，为什么会抛出异常呢？

如果你看过我之前的源码分析的文章应该知道，在处理回滚时有这么一段代码

![rollBackOnly设置](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000428.png)

在提交时又做了下面这个判断（*这个方法我删掉了一些不重要的代码*）

![commit_rollbackOnly](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000435.png)

可以看到当提交时发现事务已经被标记为rollbackOnly后会进入回滚处理中，并且unexpected传入的为true。在处理回滚时又有下面这段代码

![抛出异常](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000443.png)

最后在这里抛出了这个异常。

> 以上代码均位于`AbstractPlatformTransactionManager`中

总结起来，**主要的原因就是因为内部事务回滚时将整个大事务做了一个rollbackOnly的标记**，所以即使我们在外部事务中catch了抛出的异常，整个事务仍然无法正常提交，并且如果你希望正常提交，Spring还会抛出一个异常。

**解决方案**:

这个解决方案要依赖业务而定，你要明确你想要的结果是什么

1.内部事务发生异常，外部事务catch异常后，内部事务自行回滚，不影响外部事务

> 将内部事务的传播级别设置为nested/requires_new均可。在我们的例子中就是做如下修改：
>
> ```java
> // @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRES_NEW)
> @Transactional(rollbackFor = Exception.class,propagation = Propagation.NESTED)
> public void a() throws ClassNotFoundException{
> // ......
> throw new ClassNotFoundException();
> }
> ```

虽然这两者都能得到上面的结果，但是它们之间还是有不同的。当传播级别为`requires_new`时，两个事务完全没有联系，各自都有自己的事务管理机制（开启事务、关闭事务、回滚事务）。但是传播级别为`nested`时，实际上只存在一个事务，只是在调用a方法时设置了一个保存点，当a方法回滚时，实际上是回滚到保存点上，并且当外部事务提交时，内部事务才会提交，外部事务如果回滚，内部事务会跟着回滚。

2.内部事务发生异常时，外部事务catch异常后，内外两个事务都回滚，但是方法不抛出异常

> ```java
> @Transactional
> public void testRollbackOnly() {
> try {
>    indexService.a();
> } catch (ClassNotFoundException e) {
>    // 加上这句代码
>    TransactionInterceptor.currentTransactionStatus().setRollbackOnly();
> }
> }
> ```

通过显示的设置事务的状态为`RollbackOnly`。这样当提交事务时会进入下面这段代码

![显示回滚](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330000453.png)

最大的区别在于处理回滚时第二个参数传入的是false,这意味着回滚是回滚是预期之中的，所以在处理完回滚后并不会抛出异常。

### X.3.读写分离跟事务结合使用时的问题

读写分离一般有两种实现方式

1. 配置多数据源
2. 依赖中间件，如`MyCat`

如果是配置了多数据源的方式实现了读写分离，那么需要注意的是：**如果开启了一个读写事务，那么必须使用写节点**，**如果是一个只读事务，那么可以使用读节点**

如果是依赖于`MyCat`等中间件那么需要注意：**只要开启了事务，事务内的SQL都会使用写节点（依赖于具体中间件的实现，也有可能会允许使用读节点，具体策略需要自行跟DB团队确认）**

基于上面的结论，我们在使用事务时应该更加谨慎，在没有必要开启事务时尽量不要开启。

> 一般我们会在配置文件配置某些约定的方法名字前缀开启不同的事务（或者不开启），但现在随着注解事务的流行，好多开发人员（或者架构师）搭建框架的时候在service类上加上了@Transactional注解，导致整个类都是开启事务的，这样严重影响数据库执行的效率，更重要的是开发人员不重视、或者不知道在查询类的方法上面自己加上@Transactional（propagation=Propagation.NOT_SUPPORTED）就会导致，所有的查询方法实际并没有走从库，导致主库压力过大。

其次，关于如果没有对只读事务做优化的话（优化意味着将只读事务路由到读节点），那么`@Transactional`注解中的`readOnly`属性就应该要慎用。我们使用`readOnly`的原本目的是为了将事务标记为只读，这样当MySQL服务端检测到是一个只读事务后就可以做优化，少分配一些资源（例如：只读事务不需要回滚，所以不需要分配undo log段）。但是当配置了读写分离后，可能会可能会导致只读事务内所有的SQL都被路由到了主库，读写分离也就失去了意义。



### X.4.大事务引发的问题

#### X.4.1.大事务引发的问题

在分享解决办法之前，先看看系统中如果出现大事务可能会引发哪些问题

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210331213423.png)

从上图可以看出如果系统中出现大事务时，问题还不小，所以我们在实际项目开发中应该尽量避免大事务的情况。如果我们已有系统中存在大事务问题，该如何解决呢？

#### X.4.2.解决办法

##### X.4.2.1.少用@Transactional注解

大家在实际项目开发中，我们在业务方法加上@Transactional注解开启事务功能，这是非常普遍的做法，它被称为声明式事务。

部分代码如下：

```java
  @Transactional(rollbackFor=Exception.class)
   public void save(User user) {
         doSameThing...
   }
```

然而，我要说的第一条是：少用@Transactional注解。

**为什么？**

我们知道 @Transactional 注解是通过spring的aop起作用的，但是如果使用不当，事务功能可能会失效。如果恰巧你经验不足，这种问题不太好排查。

@Transactional注解一般加在某个业务方法上，会导致整个业务方法都在同一个事务中，粒度太粗，不好控制事务范围，是出现大事务问题的最常见的原因。

**那我们该怎么办呢？**

可以使用编程式事务，在spring项目中使用TransactionTemplate类的对象，手动执行事务。

部分代码如下：

```java
   @Autowired
   private TransactionTemplate transactionTemplate;
   
   ...
   
   public void save(final User user) {
         transactionTemplate.execute((status) => {
            doSameThing...
            return Boolean.TRUE;
         })
   }
```

从上面的代码中可以看出，使用TransactionTemplate的编程式事务功能自己灵活控制事务的范围，是避免大事务问题的首选办法。

当然，我说少使用@Transactional注解开启事务，并不是说一定不能用它，如果项目中有些业务逻辑比较简单，而且不经常变动，使用@Transactional注解开启事务开启事务也无妨，因为它更简单，开发效率更高，但是千万要小心事务失效的问题。

##### X.4.2.2.将查询(select)方法放到事务外

如果出现大事务，可以将查询(select)方法放到事务外，也是比较常用的做法，因为一般情况下这类方法是不需要事务的。

比如出现如下代码：

```java
  @Transactional(rollbackFor=Exception.class)
   public void save(User user) {
         queryData1();
         queryData2();
         addData1();
         updateData2();
   }
```

可以将queryData1和queryData2两个查询方法放在事务外执行，将真正需要事务执行的代码才放到事务中，比如：addData1和updateData2方法，这样就能有效的减少事务的粒度。

如果使用TransactionTemplate的编程式事务这里就非常好修改。

```java
   @Autowired
   private TransactionTemplate transactionTemplate;
   
   ...
   
   public void save(final User user) {
         queryData1();
         queryData2();
         transactionTemplate.execute((status) => {
            addData1();
            updateData2();
            return Boolean.TRUE;
         })
   }
```

但是如果你实在还是想用@Transactional注解，该怎么拆分呢？

```java
   public void save(User user) {
         queryData1();
         queryData2();
         doSave();
    }
   
    @Transactional(rollbackFor=Exception.class)
    public void doSave(User user) {
       addData1();
       updateData2();
    }
```

这个例子是非常经典的错误，这种直接方法调用的做法事务不会生效，给正在坑中的朋友提个醒。因为@Transactional注解的声明式事务是通过spring aop起作用的，而spring aop需要生成代理对象，直接方法调用使用的还是原始对象，所以事务不会生效。

有没有办法解决这个问题呢？

**1.新加一个Service方法**

这个方法非常简单，只需要新加一个Service方法，把@Transactional注解加到新Service方法上，把需要事务执行的代码移到新方法中。具体代码如下：

```java
  @Servcie
  publicclass ServiceA {
     @Autowired
     prvate ServiceB serviceB;
  
     public void save(User user) {
           queryData1();
           queryData2();
           serviceB.doSave(user);
     }
   }
   
   @Servcie
   publicclass ServiceB {
   
      @Transactional(rollbackFor=Exception.class)
      public void doSave(User user) {
         addData1();
         updateData2();
      }
   
   }
```

2.在该Service类中注入自己

如果不想再新加一个Service类，在该Service类中注入自己也是一种选择。具体代码如下：

```java
  @Servcie
  publicclass ServiceA {
     @Autowired
     prvate ServiceA serviceA;
  
     public void save(User user) {
           queryData1();
           queryData2();
           serviceA.doSave(user);
     }
     
     @Transactional(rollbackFor=Exception.class)
     public void doSave(User user) {
         addData1();
         updateData2();
      }
   }
```

可能有些人可能会有这样的疑问：这种做法会不会出现循环依赖问题？

其实spring ioc内部的三级缓存保证了它，不会出现循环依赖问题。如果你想进一步了解循环依赖问题，可以看看我之前文章《spring解决循环依赖为什么要用三级缓存？》。

3.在该Service类中使用AopContext.currentProxy()获取代理对象

上面的方法2确实可以解决问题，但是代码看起来并不直观，还可以通过在该Service类中使用AOPProxy获取代理对象，实现相同的功能。具体代码如下：

```java
  @Servcie
  publicclass ServiceA {
  
     public void save(User user) {
           queryData1();
           queryData2();
           ((ServiceA)AopContext.currentProxy()).doSave(user);
     }
     
     @Transactional(rollbackFor=Exception.class)
     public void doSave(User user) {
         addData1();
         updateData2();
      }
   }
```

##### X.4.2.3.事务中避免远程调用

我们在接口中调用其他系统的接口是不能避免的，由于网络不稳定，这种远程调的响应时间可能比较长，如果远程调用的代码放在某个事物中，这个事物就可能是大事务。当然，远程调用不仅仅是指调用接口，还有包括：发MQ消息，或者连接redis、mongodb保存数据等。

```java
  @Transactional(rollbackFor=Exception.class)
   public void save(User user) {
         callRemoteApi();
         addData1();
   }
```

远程调用的代码可能耗时较长，切记一定要放在事务之外。

```java
   @Autowired
   private TransactionTemplate transactionTemplate;
   
   ...
   
   public void save(final User user) {
         callRemoteApi();
         transactionTemplate.execute((status) => {
            addData1();
            return Boolean.TRUE;
         })
   }
```

有些朋友可能会问，远程调用的代码不放在事务中如何保证数据一致性呢？这就需要建立：重试+补偿机制，达到数据最终一致性了。

##### X.4.2.4.事务中避免一次性处理太多数据

如果一个事务中需要处理的数据太多，也会造成大事务问题。比如为了操作方便，你可能会一次批量更新1000条数据，这样会导致大量数据锁等待，特别在高并发的系统中问题尤为明显。

解决办法是分页处理，1000条数据，分50页，一次只处理20条数据，这样可以大大减少大事务的出现。

##### X.4.2.5.非事务执行

在使用事务之前，我们都应该思考一下，是不是所有的数据库操作都需要在事务中执行？

```java
@Autowired
private TransactionTemplate transactionTemplate;

...

public void save(final User user) {
      transactionTemplate.execute((status) => {
         addData();
         addLog();
         updateCount();
         return Boolean.TRUE;
      })
}
```

上面的例子中，其实addLog增加操作日志方法 和 updateCount更新统计数量方法，是可以不在事务中执行的，因为操作日志和统计数量这种业务允许少量数据不一致的情况。

```java
@Autowired
private TransactionTemplate transactionTemplate;

...

public void save(final User user) {
      transactionTemplate.execute((status) => {
         addData();           
         return Boolean.TRUE;
      })
      addLog();
      updateCount();
}
```

当然大事务中要鉴别出哪些方法可以非事务执行，其实没那么容易，需要对整个业务梳理一遍，才能找出最合理的答案。

##### X.4.2.6.异步处理

还有一点也非常重要，是不是事务中的所有方法都需要同步执行？我们都知道，方法同步执行需要等待方法返回，如果一个事务中同步执行的方法太多了，势必会造成等待时间过长，出现大事务问题。

看看下面这个列子：

```java
   @Autowired
   private TransactionTemplate transactionTemplate;
   
   ...
   
   public void save(final User user) {
         transactionTemplate.execute((status) => {
            order();
            delivery();
            return Boolean.TRUE;
         })
   }
```

order方法用于下单，delivery方法用于发货，是不是下单后就一定要马上发货呢？

答案是否定的。

这里发货功能其实可以走mq异步处理逻辑。

```java
   @Autowired
   private TransactionTemplate transactionTemplate;
   
   ...
   
   public void save(final User user) {
         transactionTemplate.execute((status) => {
            order();
            return Boolean.TRUE;
         })
         sendMq();
   }
```

#### X.4.3.总结

本人从网友的一个问题出发，结合自己实际的工作经验分享了处理大事务的6种办法：

- 少用@Transactional注解
- 将查询(select)方法放到事务外
- 事务中避免远程调用
- 事务中避免一次性处理太多数据
- 非事务执行
- 异步处理