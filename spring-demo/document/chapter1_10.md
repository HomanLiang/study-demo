[toc]



# Spring DAO

## 1.回顾对模版代码优化过程

我们来回忆一下我们怎么对模板代码进行优化的！

- 首先来看一下我们**原生的JDBC：需要手动去数据库的驱动从而拿到对应的连接**..

    ```
    try {
        String sql = "insert into t_dept(deptName) values('test');";
        Connection con = null;
        Statement stmt = null;
        Class.forName("com.mysql.jdbc.Driver");
        // 连接对象
        con = DriverManager.getConnection("jdbc:mysql:///hib_demo", "root", "root");
        // 执行命令对象
        stmt =  con.createStatement();
        // 执行
        stmt.execute(sql);

        // 关闭
        stmt.close();
        con.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
    ```

- 因为JDBC是面向接口编程的，因此数据库的驱动都是由数据库的厂商给做到好了，我们**只要加载对应的数据库驱动，便可以获取对应的数据库连接**....因此，我们**写了一个工具类，专门来获取与数据库的连接(Connection)**,当然啦，为了更加灵活，我们的**工具类是读取配置文件的方式来做的**。

```
    /*
    * 连接数据库的driver，url，username，password通过配置文件来配置，可以增加灵活性
    * 当我们需要切换数据库的时候，只需要在配置文件中改以上的信息即可
    *
    * */

    private static String  driver = null;
    private static String  url = null;
    private static String  username = null;
    private static String password = null;

    static {
        try {

            //获取配置文件的读入流
            InputStream inputStream = UtilsDemo.class.getClassLoader().getResourceAsStream("db.properties");

            Properties properties = new Properties();
            properties.load(inputStream);

            //获取配置文件的信息
            driver = properties.getProperty("driver");
            url = properties.getProperty("url");
            username = properties.getProperty("username");
            password = properties.getProperty("password");

            //加载驱动类
            Class.forName(driver);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url,username,password);
    }
    
    public static void release(Connection connection, Statement statement, ResultSet resultSet) {

        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
```

- 经过上面一层的封装，我们可以在使用的地方直接使用工具类来得到与数据库的连接...那么比原来就方便很多了！但是呢，每次还是需要使用Connection去创建一个Statement对象。并且无论是什么方法，其实就是SQL语句和传递进来的参数不同！

------

## 2.使用Spring的JDBC

上面已经回顾了一下以前我们的JDBC开发了，那么看看Spring对JDBC又是怎么优化的

首先，想要使用Spring的JDBC模块，就必须引入两个jar文件：

- 引入jar文件
  - **spring-jdbc-3.2.5.RELEASE.jar**
  - **spring-tx-3.2.5.RELEASE.jar**
- 首先还是看一下我们原生的JDBC代码：**获取Connection是可以抽取出来的，直接使用dataSource来得到Connection就行了**。

    ```
        public void save() {
            try {
                String sql = "insert into t_dept(deptName) values('test');";
                Connection con = null;
                Statement stmt = null;
                Class.forName("com.mysql.jdbc.Driver");
                // 连接对象
                con = DriverManager.getConnection("jdbc:mysql:///hib_demo", "root", "root");
                // 执行命令对象
                stmt =  con.createStatement();
                // 执行
                stmt.execute(sql);

                // 关闭
                stmt.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    ```

- 值得注意的是，**JDBC对C3P0数据库连接池是有很好的支持的。因此我们直接可以使用Spring的依赖注入，在配置文件中配置dataSource就行了**！

    ```
        <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
            <property name="driverClass" value="com.mysql.jdbc.Driver"></property>
            <property name="jdbcUrl" value="jdbc:mysql:///hib_demo"></property>
            <property name="user" value="root"></property>
            <property name="password" value="root"></property>
            <property name="initialPoolSize" value="3"></property>
            <property name="maxPoolSize" value="10"></property>
            <property name="maxStatements" value="100"></property>
            <property name="acquireIncrement" value="2"></property>
        </bean>
    ```

    ```
        // IOC容器注入
        private DataSource dataSource;
        public void setDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public void save() {
            try {
                String sql = "insert into t_dept(deptName) values('test');";
                Connection con = null;
                Statement stmt = null;
                // 连接对象
                con = dataSource.getConnection();
                // 执行命令对象
                stmt =  con.createStatement();
                // 执行
                stmt.execute(sql);

                // 关闭
                stmt.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    ```

- **Spring来提供了JdbcTemplate这么一个类给我们使用！它封装了DataSource，也就是说我们可以在Dao中使用JdbcTemplate就行了。**
- 创建dataSource，创建jdbcTemplate对象

    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:context="http://www.springframework.org/schema/context"
           xmlns:c="http://www.springframework.org/schema/c"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

        <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
            <property name="driverClass" value="com.mysql.jdbc.Driver"></property>
            <property name="jdbcUrl" value="jdbc:mysql:///zhongfucheng"></property>
            <property name="user" value="root"></property>
            <property name="password" value="root"></property>
            <property name="initialPoolSize" value="3"></property>
            <property name="maxPoolSize" value="10"></property>
            <property name="maxStatements" value="100"></property>
            <property name="acquireIncrement" value="2"></property>
        </bean>

        <!--扫描注解-->
        <context:component-scan base-package="bb"/>

        <!-- 2. 创建JdbcTemplate对象 -->
        <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
            <property name="dataSource" ref="dataSource"></property>
        </bean>

    </beans>
    ```

- userDao

    ```
    package bb;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.jdbc.core.JdbcTemplate;
    import org.springframework.stereotype.Component;

    /**
     * Created by ozc on 2017/5/10.
     */
    @Component
    public class UserDao implements IUser {

        //使用Spring的自动装配
        @Autowired
        private JdbcTemplate template;

        @Override
        public void save() {
            String sql = "insert into user(name,password) values('zhoggucheng','123')";
            template.update(sql);
        }

    }
    ```

- 测试：

    ```
        @Test
        public void test33() {
            ApplicationContext ac = new ClassPathXmlApplicationContext("bb/bean.xml");

            UserDao userDao = (UserDao) ac.getBean("userDao");
            userDao.save();
        }
    ```
    
    ![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330211826.webp)

------

### 2.1.JdbcTemplate查询

我们要是使用JdbcTemplate查询会发现**有很多重载了query()方法**

![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330211844.webp)

一般地，**如果我们使用queryForMap()，那么只能封装一行的数据，如果封装多行的数据、那么就会报错**！并且，Spring是不知道我们想把一行数据封装成是什么样的，因此返回值是Map集合...我们得到Map集合的话还需要我们自己去转换成自己需要的类型。

------

我们一般使用下面这个方法：

![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330211857.webp)

我们可以**实现RowMapper，告诉Spriing我们将每行记录封装成怎么样的**。

```
    public void query(String id) {
        String sql = "select * from USER where password=?";

        List<User> query = template.query(sql, new RowMapper<User>() {


            //将每行记录封装成User对象
            @Override
            public User mapRow(ResultSet resultSet, int i) throws SQLException {
                User user = new User();
                user.setName(resultSet.getString("name"));
                user.setPassword(resultSet.getString("password"));

                return user;
            }

        },id);


        System.out.println(query);
    }
```

![è¿éåå¾çæè¿°](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330211944.webp)

------

当然了，一般我们都是**将每行记录封装成一个JavaBean对象的，因此直接实现RowMapper，在使用的时候创建就好了**。

```
	class MyResult implements RowMapper<Dept>{

		// 如何封装一行记录
		@Override
		public Dept mapRow(ResultSet rs, int index) throws SQLException {
			Dept dept = new Dept();
			dept.setDeptId(rs.getInt("deptId"));
			dept.setDeptName(rs.getString("deptName"));
			return dept;
		}
		
	}
复制代码
```

## 3.事务控制

### 3.1.事务控制概述

下面主要讲解Spring的事务控制，如何使用Spring来对程序进行事务控制....

- **Spring的事务控制是属于Spring Dao模块的**。

一般地，我们**事务控制都是在service层做的**。。为什么是在service层而不是在dao层呢？？有没有这样的疑问...

service层是业务逻辑层，service的方法一旦执行成功，那么说明该功能没有出错。

一个**service方法可能要调用dao层的多个方法**...如果在dao层做事务控制的话，一个dao方法出错了，仅仅把事务回滚到当前dao的功能，这样是不合适的[因为我们的业务由多个dao方法组成]。如果没有出错，调用完dao方法就commit了事务，这也是不合适的[导致太多的commit操作]。

事务控制分为两种：

- **编程式事务控制**
- **声明式事务控制**

### 3.2.编程式事务控制

**自己手动控制事务，就叫做编程式事务控制。**

- Jdbc代码：

    ```
    Conn.setAutoCommite(false);  // 设置手动控制事务
    ```

- Hibernate代码：

    ```
    Session.beginTransaction();    // 开启一个事务
    ```

- **【细粒度的事务控制： 可以对指定的方法、指定的方法的某几行添加事务控制】**

- **(比较灵活，但开发起来比较繁琐： 每次都要开启、提交、回滚.)**

### 3.3.声明式事务控制

**Spring提供对事务的控制管理就叫做声明式事务控制**

Spring提供了对事务控制的实现。

- 如果用户想要使用Spring的事务控制，**只需要配置就行了**。
- 当不用Spring事务的时候，直接移除就行了。
- Spring的事务控制是**基于AOP实现的**。因此它的**耦合度是非常低**的。
- 【粗粒度的事务控制： 只能给整个方法应用事务，不可以对方法的某几行应用事务。】
  - (因为aop拦截的是方法。)

**Spring给我们提供了事务的管理器类**，事务管理器类又分为两种，因为**JDBC的事务和Hibernate的事务是不一样的**。

- Spring声明式事务管理器类：

  - ```
    Jdbc技术：DataSourceTransactionManager
    ```

  - ```
    Hibernate技术：HibernateTransactionManager
    ```

#### 3.3.1.声明式事务控制示例

我们基于Spring的JDBC来做例子吧

引入相关jar包

- **AOP相关的jar包【因为Spring的声明式事务控制是基于AOP的，那么就需要引入AOP的jar包。】**
- **引入tx名称空间**
- **引入AOP名称空间**
- **引入jdbcjar包【jdbc.jar包和tx.jar包】**

------

##### 3.3.1.1.搭建配置环境

- 编写一个接口

    ```
    public interface IUser {
        void save();
    }
    ```

- **UserDao实现类，使用JdbcTemplate对数据库进行操作！**

    ```
    @Repository
    public class UserDao implements IUser {

        //使用Spring的自动装配
        @Autowired
        private JdbcTemplate template;

        @Override
        public void save() {
            String sql = "insert into user(name,password) values('zhong','222')";
            template.update(sql);
        }

    }
    ```

- userService

    ```
    @Service
    public class UserService {

        @Autowired
        private UserDao userDao;
        public void save() {

            userDao.save();
        }
    }
    ```

- bean.xml配置：配置数据库连接池、jdbcTemplate对象、扫描注解

    ```
    <?xml version="1.0" encoding="UTF-8"?>
    <beans xmlns="http://www.springframework.org/schema/beans"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:context="http://www.springframework.org/schema/context"
           xmlns:c="http://www.springframework.org/schema/c"
           xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

        <!--数据连接池配置-->
        <bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource">
            <property name="driverClass" value="com.mysql.jdbc.Driver"></property>
            <property name="jdbcUrl" value="jdbc:mysql:///zhongfucheng"></property>
            <property name="user" value="root"></property>
            <property name="password" value="root"></property>
            <property name="initialPoolSize" value="3"></property>
            <property name="maxPoolSize" value="10"></property>
            <property name="maxStatements" value="100"></property>
            <property name="acquireIncrement" value="2"></property>
        </bean>

        <!--扫描注解-->
        <context:component-scan base-package="bb"/>

        <!-- 2. 创建JdbcTemplate对象 -->
        <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
            <property name="dataSource" ref="dataSource"></property>
        </bean>

    </beans>
    ```
    
    前面搭建环境的的时候，是没有任何的事务控制的。
    
    也就是说，**当我在service中调用两次userDao.save()，即时在中途中有异常抛出，还是可以在数据库插入一条记录的**。
    
- Service代码：

    ```
    @Service
    public class UserService {

        @Autowired
        private UserDao userDao;
        public void save() {

            userDao.save();

            int i = 1 / 0;
            userDao.save();
        }
    }
    ```

- 测试代码：

    ```
    public class Test2 {

        @Test
        public void test33() {
            ApplicationContext ac = new ClassPathXmlApplicationContext("bb/bean.xml");

            UserService userService = (UserService) ac.getBean("userService");
            userService.save();
        }
    }
    ```
    
    ![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330212056.webp)

------

##### 3.3.1.2.XML方式实现声明式事务控制

**首先，我们要配置事务的管理器类：因为JDBC和Hibernate的事务控制是不同的。**

```
    <!--1.配置事务的管理器类:JDBC-->
    <bean id="txManage" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">

        <!--引用数据库连接池-->
        <property name="dataSource" ref="dataSource"/>
    </bean>
```

再而，**配置事务管理器类如何管理事务**

```
    <!--2.配置如何管理事务-->
    <tx:advice id="txAdvice" transaction-manager="txManage">
        
        <!--配置事务的属性-->
        <tx:attributes>
            <!--所有的方法，并不是只读-->
            <tx:method name="*" read-only="false"/>
        </tx:attributes>
    </tx:advice>
```

最后，**配置拦截哪些方法，**

```
    <!--3.配置拦截哪些方法+事务的属性-->
    <aop:config>
        <aop:pointcut id="pt" expression="execution(* bb.UserService.*(..) )"/>
        <aop:advisor advice-ref="txAdvice" pointcut-ref="pt"></aop:advisor>
    </aop:config>
```

配置完成之后，service中的方法都应该被Spring的声明式事务控制了。因此我们再次测试一下：

```
    @Test
    public void test33() {
        ApplicationContext ac = new ClassPathXmlApplicationContext("bb/bean.xml");

        UserService userService = (UserService) ac.getBean("userService");
        userService.save();
    }
```

![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330212121.webp)

------

##### 3.3.1.3.使用注解的方法实现事务控制

当然了，有的人可能觉得到XML文件上配置太多东西了。**Spring也提供了使用注解的方式来实现对事务控制**

第一步和XML的是一样的，**必须配置事务管理器类：**

```
    <!--1.配置事务的管理器类:JDBC-->
    <bean id="txManage" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">

        <!--引用数据库连接池-->
        <property name="dataSource" ref="dataSource"/>
    </bean>
```

第二步：开启以注解的方式来实现事务控制

```
    <!--开启以注解的方式实现事务控制-->
    <tx:annotation-driven transaction-manager="txManage"/>
```

最后，**想要控制哪个方法事务，在其前面添加@Transactional这个注解就行了！**如果想要控制整个类的事务，那么在类上面添加就行了。

```
    @Transactional
    public void save() {

        userDao.save();

        int i = 1 / 0;
        userDao.save();
    }
```

![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330212142.webp)

------

##### 3.3.1.4.事务属性

其实我们**在XML配置管理器类如何管理事务，就是在指定事务的属性！**我们来看一下事务的属性有什么：

![这里写图片描述](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210330212152.webp)

**事务传播行为:**

看了上面的事务属性，没有接触过的其实就这么一个：`propagation = Propagation.REQUIRED`事务的传播行为。

事务传播行为的属性有以下这么多个，常用的就只有两个：

- Propagation.REQUIRED【如果当前方法已经有事务了，**加入当前方法事务**】
- Propagation.REQUIRED_NEW【如果当前方法有事务了，当前方法事务会挂起。**始终开启一个新的事务**，直到新的事务执行完、当前方法的事务才开始】

![这里写图片描述](https://user-gold-cdn.xitu.io/2018/3/15/16227fcfdd37c9e0?imageView2/0/w/1280/h/960/format/webp/ignore-error/1)

**当事务传播行为是Propagation.REQUIRED**

- 现在有一个日志类，它的事务传播行为是Propagation.REQUIRED

    ```
        Class Log{
                Propagation.REQUIRED  
                insertLog();  
        }
    ```

- 现在，我要在保存之前记录日志

    ```
        Propagation.REQUIRED
        Void  saveDept(){
            insertLog();   
            saveDept();
        }
    ```
    
    saveDept()本身就存在着一个事务，当调用insertLog()的时候，insertLog()的事务会加入到saveDept()事务中
    
    也就是说，saveDept()方法内始终是一个事务，如果在途中出现了异常，那么insertLog()的数据是会被回滚的【因为在同一事务内】

    ```
        Void  saveDept(){
            insertLog();    // 加入当前事务
            .. 异常, 会回滚
            saveDept();
        }
    ```

------

**当事务传播行为是Propagation.REQUIRED_NEW**

- 现在有一个日志类，它的事务传播行为是Propagation.REQUIRED_NEW

    ```
        Class Log{
                Propagation.REQUIRED  
                insertLog();  
        }
    ```

- 现在，我要在保存之前记录日志

    ```
        Propagation.REQUIRED
        Void  saveDept(){
            insertLog();   
            saveDept();
        }
    ```

	当执行到saveDept()中的insertLog()方法时，insertLog()方法发现 saveDept()已经存在事务了，insertLog()会独自新开一个事务，直到事务关闭之后，再执行下面的方法

	如果在中途中抛出了异常，insertLog()是不会回滚的，因为它的事务是自己的，已经提交了

    ```
        Void  saveDept(){
            insertLog();    // 始终开启事务
            .. 异常, 日志不会回滚
            saveDept();
        }
    ```