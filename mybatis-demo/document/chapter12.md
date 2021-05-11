[toc]



# MyBatis 增强

## 1.TkMybatis

### 1.1.基本步骤

```
1. 引入TkMybatis的Maven依赖

2. 实体类的相关配置,@Id,@Table

3. Mapper继承tkMabatis的Mapper接口

4. 启动类Application或自定义Mybatis配置类上使用@MapperScan注解扫描Mapper接口

5. 在application.properties配置文件中,配置mapper.xml文件指定的位置[可选]

6. 使用TkMybatis提供的sql执行方法

7. 如有需要,实现mapper.xml自定义sql语句
    PS : 
        1. TkMybatis默认使用继承Mapper接口中传入的实体类对象去数据库寻找对应的表,因此如果表名与实体类名不满足对应规则时,会报错,这时使用@Table为实体类指定表。(这种对应规则为驼峰命名规则)
        2. 使用TkMybatis可以无xml文件实现数据库操作,只需要继承tkMybatis的Mapper接口即可。
        3. 如果有自定义特殊的需求,可以添加mapper.xml进行自定义sql书写,但路径必须与步骤4对应。
```

### 1.2.Java 实体类

考虑到基本数据类型在Java 类中都有默认值，会导致MyBatis 在执行相关操作时很难判断当前字段是否为null，所以在MyBatis 环境下使用Java 实体类时尽量不要使用基本数据类型，都使用对应的包装类型。

```
TkMybatis默认使用继承Mapper接口中传入的实体类对象去数据库寻找对应的表,因此如果表名与实体类名不满足对应规则时,会报错,这时使用@Table为实体类指定表。(这种对应规则为驼峰命名规则)
下面以一个实体类Custoemr为例：
// @Table指定该实体类对应的表名,如表名为base_customer,类名为BaseCustomer可以不需要此注解
@Table(name = "t_base_customer")
public class Customer {

    // @Id表示该字段对应数据库表的主键id
    // @GeneratedValue中strategy表示使用数据库自带的主键生成策略.
    // @GeneratedValue中generator配置为"JDBC",在数据插入完毕之后,会自动将主键id填充到实体类中.类似普通mapper.xml中配置的selectKey标签
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY,generator = "JDBC")
    private Long id;

    private String name;

    private String code;

    private String status;

    private Date createDate;

    private Date lastUpdate;
}
```

**注** `getter` 与 `setter` 省略，可以使用 `lombok` 插件进行省略操作，可以的话，加上 `@Id` 与 `@Table` 两个注解

### 1.3.集成Mapper

```
 		<!-- https://mvnrepository.com/artifact/tk.mybatis/mapper -->
        <dependency>
            <groupId>tk.mybatis</groupId>
            <artifactId>mapper</artifactId>
            <version>4.0.3</version>
        </dependency>
        <!-- https://mvnrepository.com/artifact/tk.mybatis/mapper-spring-boot-starter -->
        <dependency>
            <groupId>tk.mybatis</groupId>
            <artifactId>mapper-spring-boot-starter</artifactId>
            <version>2.0.3</version>
        </dependency>
```

### 1.4.Mapper继承tkMabatis的Mapper接口

```
import cn.base.model.Customer;
import tk.mybatis.mapper.common.Mapper;
public interface CustomerMapper extends Mapper<Customer> {
}
```

### 1.5.启动类Application或自定义Mybatis配置类上使用@MapperScan注解扫描Mapper接口

```
@MapperScan("cn.base.mapper")
public class MiddlewareApplication extends SpringBootServletInitializer {
}
```

### 1.6.application.properties配置mapper.xml配置文件的扫描路径

```
mybatis.mapperLocations=classpath*:cn/base/mapper/*.xml
```

### 1.7.常用注解

- **@Table 注解**

  作用：建立实体类和数据库表之间的对应关系。

  默认规则：实体类类名首字母小写作为表名。Employee 类→employee 表。

  用法：在@Table 注解的name 属性中指定目标数据库表的表名

- **@Column 注解**

  作用：建立实体类字段和数据库表字段之间的对应关系。

  默认规则：

  实体类字段：驼峰式命名

  数据库表字段：使用 `_` 区分各个单词

  用法：在 `@Column` 注解的name 属性中指定目标字段的字段名

- **@Id 注解**

  通用Mapper 在执行 `xxxByPrimaryKey(key)` 方法时，有两种情况。

  情况1：没有使用 `@Id` 注解明确指定主键字段

    ```
    SELECT emp_id,emp_name,emp_salary_apple,emp_age FROM tabple_emp WHERE emp_id = ?
    AND emp_name = ? AND emp_salary_apple = ? AND emp_age = ?
    ```
  
  之所以会生成上面这样的WHERE 子句是因为通用Mapper 将实体类中的所有字段都拿来放在一起作为联合主键。
  
  情况2：使用@Id 主键明确标记和数据库表中主键字段对应的实体类字段。
  
- **@GeneratedValue 注解**

  作用：让通用Mapper 在执行insert 操作之后将数据库自动生成的主键值回写到实体类对象中。

  自增主键用法：

  序列主键用法：

- **@Transient 主键**

  用于标记不与数据库表字段对应的实体类字段。

    ```
    @Transient
    private String otherThings; //非数据库表中字段
    ```

### 1.8.常用方法

**selectOne 方法**

通用Mapper 替我们自动生成的SQL 语句情况，实体类封装查询条件生成WHERE 子句的规则，使用非空的值生成WHERE 子句，在条件表达式中使用“=”进行比较，要求必须返回一个实体类结果，如果有多个，则会抛出异常。

**xxxByPrimaryKey 方法**

需要使用@Id 主键明确标记和数据库表主键字段对应的实体类字段，否则通用 Mapper 会将所有实体类字段作为联合主键。

**xxxSelective 方法**

非主键字段如果为null 值，则不加入到SQL 语句中。

**QBC 查询**

Query By Criteria

Criteria 是Criterion 的复数形式。意思是：规则、标准、准则。在SQL 语句中相当于查询条件。

QBC 查询是将查询条件通过Java 对象进行模块化封装。

**示例代码**

```
//目标：WHERE (emp_salary>? AND emp_age<?) OR (emp_salary<? AND emp_age>?)
//1.创建Example 对象
Example example = new Example(Employee.class);
//***********************
//i.设置排序信息
example.orderBy("empSalary").asc().orderBy("empAge").desc();
//ii.设置“去重”
example.setDistinct(true);
//iii.设置select 字段
example.selectProperties("empName","empSalary");
//***********************
//2.通过Example 对象创建Criteria 对象
Criteria criteria01 = example.createCriteria();
Criteria criteria02 = example.createCriteria();
//3.在两个Criteria 对象中分别设置查询条件
//property 参数：实体类的属性名
//value 参数：实体类的属性值
criteria01.andGreaterThan("empSalary", 3000)
.andLessThan("empAge", 25);
criteria02.andLessThan("empSalary", 5000)
.andGreaterThan("empAge", 30);
//4.使用OR 关键词组装两个Criteria 对象
example.or(criteria02);
//5.执行查询
List<Employee> empList = employeeService.getEmpListByExample(example);
for (Employee employee : empList) {
	System.out.println(employee);
}
```



## 2.MyBatis-Plus

[MyBatis-Plus](https://baomidou.com/guide/)



### 2.1.【mybatis-plus】什么是乐观锁？如何实现“乐观锁”

乐观锁的实现，通过增加一个字段，比如version，来记录每次的更新。

查询数据的时候带出version的值，执行更新的时候，会再去比较version，如果不一致，就更新失败。

还是用之前的user表，增加了新的字段`version`。

**1.在实体类里增加对于的字段，并且加上自动填充（你也可以每次手动填充）**

```
@Data
public class User {
    @TableId(type = IdType.ID_WORKER)
    private Long id;
    private String name;
    private Integer age;
    private String email;

    @TableField(fill = FieldFill.INSERT)        // 新增的时候填充数据
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) // 新增或修改的时候填充数据
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    @Version
    private Integer version; // 版本号
}
@Component //此注解表示 将其交给spring去管理
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
        this.setFieldValByName("version", 0, metaObject); //新增就设置版本值为0
    }


    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }
}
```

#### 2. 配置插件

为了便于管理，可以见一个包，用于存放各种配置类，顺便把配置在启动类里的mapper扫描也换到这里来。

```
package com.pingguo.mpdemo.config;

import com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 配置扫描mapper的路径
@MapperScan("com.pingguo.mpdemo.mapper")
public class MpConfig {

    // 乐观锁插件
    @Bean
    public OptimisticLockerInterceptor optimisticLockerInterceptor() {
        return new OptimisticLockerInterceptor();
    }
}
```

#### 3.测试乐观锁

先新增一条测试数据：

```
    //    新增
    @Test
    void addUser() {
        User user = new User();
        user.setName("大周");
        user.setAge(22);
        user.setEmail("laowang@123.com");
        userMapper.insert(user);
    }
```

新增成功，可以看到version值是0。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407222352.png)

再来试一下正常的修改：

```
	//      测试乐观锁
    @Test
    void testOptimisticLocker() {
        User user = userMapper.selectById(1342502561945915393L);
        user.setName("大周2");
        userMapper.updateById(user);
    }
```

修改成功，可以看到version 变成了1。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407222410.png)

最后，模拟下并发更新，乐观锁更新失败的情况：

```
    //  测试乐观锁-失败
    @Test
    void testOptimisticLockerFailed() {
        User user = userMapper.selectById(1342502561945915393L);
        user.setName("大周3");

        User user2 = userMapper.selectById(1342502561945915393L);
        user2.setName("大周4");

        userMapper.updateById(user2); // 这里user2插队到user前面,先去更新
        userMapper.updateById(user); // 这里由于user2先做了更新后，版本号不对，所以更新失败

    }
```

按照乐观锁的原理，user2是可以更新成功的，也就是name会修改为“大周4”，version会加1。user因为前后拿到的版本号不对，更新失败。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407222417.png)

结果符合预期，我们也可以看下mybatis的日志，进一步了解一下：

可以看到上面首先是2个查询，查询到的version都是1。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407222424.png)

接着，第一个执行update语句的时候，where条件中version=1，可以找到数据，于是更新成功，切更新version=2。

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210407222432.png)

ps：这里图丢了一个我重新补的一个数据，说明下意思，忽略ID与上面的不一致。

而第二个再执行update的时候，where条件 `version=1`，已经找不到了，因为version已经被上面的更新成了2，所以更新失败。



## 3.TkMybatis VS MyBatis-Plus

[tkmybatis VS mybatisplus](https://www.cnblogs.com/jpfss/p/12073767.html)