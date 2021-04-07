[toc]



# MyBatis 框架设计

## 功能架构设计

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210406233005.webp)

**功能架构讲解：**

我们把Mybatis的功能架构分为三层：

- API接口层：提供给外部使用的接口API，开发人员通过这些本地API来操纵数据库。接口层一接收到调用请求就会调用数据处理层来完成具体的数据处理。

- 数据处理层：负责具体的SQL查找、SQL解析、SQL执行和执行结果映射处理等。它主要的目的是根据调用的请求完成一次数据库操作。

- 基础支撑层：负责最基础的功能支撑，包括连接管理、事务管理、配置加载和缓存处理，这些都是共用的东西，将他们抽取出来作为最基础的组件。为上层的数据处理层提供最基础的支撑。



## 核心对象

![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210406234007.png)

- **BoundSql：** 表示动态生成的SQL语句以及相应的参数信息

- **MappedStatement：** MappedStatement维护了一条<select|update|delete|insert>节点的封装

- **SqlSource:** 负责根据用户传递的parameterObject，动态地生成SQL语句，将信息封装到BoundSql对象中，并返回

- **Configuration:** yBatis所有的配置信息都维持在Configuration对象之中



## 核心组件

**关于mybatis的组件**

mybatis的核心组件：

1. SqlSessionFactoryBuilder（构造器）. 它会根据代码或者配置来生成SqlSessionFactory，采用的是分布构建的builder模式。
2. SqlSessionFactory(工厂接口).  它可以生成SqlSession，采用的是工厂模式。
3. SqlSession(会话). 它可以发送SQL语句返回结果，也可以获取Mapper接口。
4. SQL Mapper(映射器).  它由一个Java接口和一个XML文件（或注解）构成，需要给出对应的SQL和映射规则，它可以发送SQL并返回结果。

下面说一下这些组件的用法。

**组件的用法**

1. **SqlSessionFactory的构建**
   
   为什么先说SqlSessionFactory，因为整个mybatis应用是以SqlSessionFactory的实例为中心的，它的唯一作用就是为了生成mybatis的核心接口对象SqlSession，所以我们一般用单例模式处理它。构建它的方法有两种：XML方式和Java代码方式。
   
   **XML方式（推荐）：**
   
   mybatis中的XML分为两类，一类是基础配置文件，用来构建上下文环境；另一类是映射器配置文件，用来配置SQL和映射规则。首先来看一下基础配置文件：
   
   ```
   <?xml version="1.0" encoding="utf-8" ?>
   <!DOCTYPE configuration
           PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
           "http://mybatis.org/dtd/mybatis-3-config.dtd">
   <configuration>
       <!--引入外部配置文件，如数据库连接的属性文件-->
       <properties resource="jdbc.properties"/>
       
       <!--为实体类配置别名-->
       <typeAliases>
           <typeAlias type="com.mybatisdemo.pojo.User" alias="user"/>
           <typeAlias type="com.mybatisdemo.pojo.Role" alias="role"/>
       </typeAliases>
       
       <!--数据库的描述信息-->
       <environments default="development">
           <environment id="development">
               <transactionManager type="JDBC"></transactionManager>
               <!--配置数据库，POOLED表示这里使用了连接池-->
               <dataSource type="POOLED">
                   <property name="driver" value="${jdbc.driver}"/>
                   <property name="url" value="${jdbc.url}"/>
                   <property name="username" value="${jdbc.username}"/>
                   <property name="password" value="${jdbc.password}"/>
               </dataSource>
           </environment>
       </environments>
       
       <!--引入映射器-->
       <mappers>
           <mapper resource="mappings/UserMapper.xml"/>
           <mapper resource="mappings/RoleMapper.xml" />
       </mappers>
   </configuration>
   ```
   
   这里列出了一些最常使用的配置，也是最基本的配置。之后就可以用一段很简短的代码生成SqlSessionFactory了：
   
   ```
   package com.mybatisdemo.test.util;
    
   import org.apache.ibatis.io.Resources;
   import org.apache.ibatis.session.SqlSessionFactory;
   import org.apache.ibatis.session.SqlSessionFactoryBuilder;
    
   import java.io.IOException;
    
   // 生成SqlSessionFactory实例，采用单例模式
   public class SqlSessionFactoryUtil {
       
       private static SqlSessionFactory sqlSessionFactory = null;
       private static String resource = "mybatis-config.xml";
       
       private SqlSessionFactoryUtil(){
           
       }
       
       public static SqlSessionFactory getSqlSessionFactory(){
           if(sqlSessionFactory == null){
               try {
                   sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream(resource)); 
               } catch (IOException e) {
                   e.printStackTrace();
               }
           }
           return sqlSessionFactory;
       }
   }
   ```
   
   流程非常简单，就是将配置文件mybatis-config.xml文件用流的方式读取，然后通过SqlSessionFactoryBuilder的builder方法去生成SqlSessionFactory就可以了。 
   
   **代码方式：**
   
   使用代码方式获取SqlSessionFactory就要繁琐很多了，但是原理是一样的。上面我们将一些配置信息写在了配置文件中然后通过读取的方式获得这些配置信息；而代码方式就是需要我们在代码中手动的去设置这些配置信息，而不是写配置文件了。哈哈，是不是感觉换汤不换药的赶脚。。这里就不贴代码了，详细的可以Google或者度娘。这种方式使用的并不多，只有在需要对数据库用户名和密码进行加密的时候才考虑使用。

2. **获取SqlSession**

   SqlSession主要是用来发送SQL语句并返回结果的，这与下面要说到的映射器作用一样。有了SqlSessionFactory之后，获取SqlSession就非常简单了。

   ```
   sqlSession = sqlSessionFactory.openSession();
   .......
   sqlSession.commit();
   .......
   sqlSession.rollback();
   .......
   sqlSession.close();
   ```

   注意sqlSession在使用完之后要及时关闭，以免浪费连接资源。

3. 映射器

   映射器是mybatis最重要的组件，它由一个XML文件和一个Java接口构成。他有着诸多强大的特性：例如动态SQL，缓存等等。它的主要作用是将查询结果映射成一个POJO对象。它的实现也有两种方式，XML文件形式和注解形式。

   **XML文件形式（推荐）：**

   先定义一个POJO：

   ```
   package com.mybatisdemo.pojo;
    
   import com.mybatisdemo.enums.SexEnum;
    
   import java.io.InputStream;
   import java.util.List;
    
   // 用户实体类
   public class User {
       private long userId;
       private long roleId;
       private String userName;
       private String password;
       private String realName;
       private SexEnum sex;
       private InputStream userImg;   // 用户头像
    
   /**setter and getter**/
   }
   ```

   然后编写映射器接口：

   ```
   public interface UserMapper {
       // 根据id查找用户
       User getUserById(long id);
       // 根据用户名查找用户
       List<User> getUserByName(String userName);
   }
   ```

   用XML创建映射器：

   ```
   <?xml version="1.0" encoding="UTF-8" ?>
   <!DOCTYPE mapper
           PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
           "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
   <mapper namespace="com.mybatisdemo.mapper.UserMapper">
        <resultMap id="userMap" type="user">
           <id property="userId" column="user_id"/>
           <result property="userName" column="user_name"/>
           <result property="password" column="password"/>
           <result property="realName" column="real_name"/>
           <result property="sex" column="sex" typeHandler="org.apache.ibatis.type.EnumOrdinalTypeHandler"/>
           <result property="userImg" column="user_img" typeHandler="org.apache.ibatis.type.BlobInputStreamTypeHandler"/>
           <result property="roleId" column="role_id"/>
       </resultMap>
       <select id="getUserById" parameterType="long" resultMap="userMap">
           select * from t_user where user_id=#{id}
       </select>
    
       <select id="getUserByName" parameterType="string" resultMap="userMap">
           select * from t_user where user_name like concat('%',#{userName},'%')
       </select>
   </mapper>
   ```

    这样一个映射器就完成了。创建完成之后通过SqlSession对象就能获取这个映射器了，具体代码如下：

   ```
   public void test(){
       SqlSession sqlSession = SqlSessionFactoryUtil.getSqlSession();
       UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
       User user = userMapper.getUserById(1L);
       sqlSession.close();
   }
   ```

    **注解方式：**

   ```
   public interface UserMapper {
    
       // 根据id查找用户
       @Select("select * from t_user where user_id=#{id}")
       User getUserById(long id);
   }
   ```

   这种方式有一个不好的地方就是当SQL语句非常长的时候，会降低代码的可读性，而且当要使用动态SQL的时候就更复杂了，所以不推荐这种方式。










