[toc]



# MySQL 常用语句

## 1.创建数据库

```undefined
db_name: 数据库名称
tb_name: 数据表名
tb_field: 数据字段
```

### 1.1 数据库登录

```shell
# 数据库服务启动 关闭 重启
service mysql start/stop/restart
# 数据库登录(通过密码登录)
mysql -u [username] -p
# 查看数据库存储引擎
show engines;
# 显示所有数据库
show databases;
# 选择需要使用的数据库
use db_name;
# 显示数据所有的表
show tables;
# 显示列属性
show columns from db_table;
```

### 1.2 编码问题

设置数据库UTF-8编码，修改数据库配置文件，或者修改数据默认编码方式

```sql
修改全局配置文件my.cnf (/etc/mysql/my.cnf)
# 文件末尾添加下述默认项配置
[client]
default-character-set=utf8

[mysql]
default-character-set=utf8

[mysqld]
collation-server = utf8_unicode_ci
init-connect='SET NAMES utf8'
character-set-server = utf8
# 数据库查询忽略大小写
lower_case_table_names = 1
```

```sql
修改数据库默认编码
# 设置数据库db_name默认为utf8:
ALTER DATABASE `db_name` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

# 设置表tb_name默认编码为utf8:
ALTER TABLE `tb_name` DEFAULT CHARACTER SET utf8 COLLATE utf8_general_ci;

# 创建表时声明编码
CREATE TABLE tb_name
(
  id CHAR(10) CHARACTER SET utf8 COLLATE utf8_unicode_ci
) CHARACTER SET latin1 COLLATE latin1_bin;
```

> **提示: **查看数据库当前编码方式，使用命令：
>  show variables like 'char%';
>  你会看到类似的输出：

![image-20210308212045581](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/image-20210308212045581.png)

### 1.3 数据库创建

```sql
#例子
CREATE TABLE tb_name(
  id int NOT NULL AUTO_INCREMENT, # 非空、自增  声明
  name VARCHAR(30) UNIQUE, #添加字段的唯一约束
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, #设置时间戳默认值为当前时间
  last_updated TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP,#设置自动添加更新时间
  size ENUM( 'small', 'medium', 'large') DEFAULT 'small', #枚举类型声明
  PRIMARY KEY (id), #主键声明
  FOREIGN KEY (id) REFERENCES db_name(tb_field) #声明外键
)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci
COMMENT = 'XXXX表';
```

> ***扩展: ***Microsoft Access、MySQL 以及 SQL Server 所使用的数据类型和范围
>  [http://www.w3school.com.cn/sql/sql_datatypes.asp](https://links.jianshu.com/go?to=http%3A%2F%2Fwww.w3school.com.cn%2Fsql%2Fsql_datatypes.asp)

### 1.4 修改密码

```
　　　格式：mysql> set password for 用户名@localhost = password(‘新密码’); 
　　　举例：mysql> set password for root@localhost = password(‘root’); 
```

### 1.5.数据库查询

- 列出全部数据库命令

  ```
  show databases;
  ```

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505094410.png)

- 切换数据库

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505094501.png)

- 列出当前数据库下全部表

  ```
  show tables;
  ```

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505094529.png)

- 查看表结构

  ```
  DESCRIBE + 表名
  ```

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505094814.png)


## 2. 常用数据类型

**int**：整型 

**double**：浮点型，例如 `double(5,2)` 表示最多5位，其中必须有2位小数，即最大值为999.99； 

**char**：固定长度字符串类型； `char(10) `

**varchar**：可变长度字符串类型；`varchar(10) `

**text**：字符串类型; 

**blob**：字节类型； 

**date**：日期类型，格式为：`yyyy-MM-dd` 

**time**：时间类型，格式为：`hh:mm:ss`

**timestamp**：时间戳类型 `yyyy-MM-dd hh:mm:ss` 会自动赋值 

**datetime**:日期时间类型 `yyyy-MM-dd hh:mm:ss`



## 3. 数据查询

```sql
# 基本查询
SELECT * FROM tb_name;
SELECT * FROM tb_name WHERE id = XXX;
# 多条查询
SELECT * FROM tb_name WHERE id in (xx, xx);
# MD5 加密，加密比较
INSERT INTO user(name,password) VALUES("admin",md5("12345"));
SELECT * FROM user WHERE name="admin" AND password=md5("12345");
# 查询最 新 的一条数据
1) SELECT * FROM tb_name ORDER BY date DESC LIMIT 1;
2) SELECT * FROM tb_name WHERE date IN (SELECT max(tb_field) FROM tb_name);
3) SELECT * FROM tb_name WHERE date = (SELECT max(tb_field) FROM tb_name);
# 查询记录总数
SELECT count(*) FROM tb_name;
# 查询前十条数据
SELECT * FROM tb_name LIMIT 10;
# 查询平均值
SELECT AVG(tb_field) FROM tb_name;
SELECT name, AVG(tb_field) FROM tb_name GROUP BY name;
```

- **SQL分组查询后取每组的前N条记录**

  **数据准备**

  - **数据库:** MySQL 8.0社区版

  - **表设计**

  	- 资讯分类表:

      | id   | 主键     |
      | :--- | :------- |
      | name | 分类名称 |

  	- 资讯信息记录表:

      | code         | 说明     |
      | :----------- | :------- |
      | id           | 主键     |
      | title        | 资讯名称 |
      | views        | 浏览量   |
      | info_type_id | 资讯类别 |

  - **初始化SQL语句:**

      ```javascript
      DROP TABLE IF EXISTS `info`;
      CREATE TABLE `info`  (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `title` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
        `views` int(255) DEFAULT NULL,
        `info_type_id` int(11) DEFAULT NULL,
        PRIMARY KEY (`id`) USING BTREE
      ) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

      -- ----------------------------
      -- Records of info
      -- ----------------------------
      INSERT INTO `info` VALUES (1, '中日海军演习', 10, 4);
      INSERT INTO `info` VALUES (2, '美俄军事竞赛', 22, 4);
      INSERT INTO `info` VALUES (3, '流浪地球电影大火', 188, 1);
      INSERT INTO `info` VALUES (4, '葛优瘫', 99, 2);
      INSERT INTO `info` VALUES (5, '周杰伦出轨了', 877, 2);
      INSERT INTO `info` VALUES (6, '蔡依林西安演唱会', 86, 1);
      INSERT INTO `info` VALUES (7, '中纪委调盐', 67, 3);
      INSERT INTO `info` VALUES (8, '人民大会堂', 109, 3);
      INSERT INTO `info` VALUES (9, '重庆称为网红城市', 202, 1);
      INSERT INTO `info` VALUES (10, '胡歌结婚了', 300, 2);
      INSERT INTO `info` VALUES (11, 'ipone15马上上市', 678, 2);
      INSERT INTO `info` VALUES (12, '中国探月成功', 54, 4);
      INSERT INTO `info` VALUES (13, '钓鱼岛对峙', 67, 4);

      DROP TABLE IF EXISTS `info_type`;
      CREATE TABLE `info_type`  (
        `id` int(11) NOT NULL AUTO_INCREMENT,
        `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL,
        PRIMARY KEY (`id`) USING BTREE
      ) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

      -- ----------------------------
      -- Records of info_type
      -- ----------------------------
      INSERT INTO `info_type` VALUES (1, '娱乐');
      INSERT INTO `info_type` VALUES (2, '八卦');
      INSERT INTO `info_type` VALUES (3, '政治');
      INSERT INTO `info_type` VALUES (4, '军事');
      ```

		资讯分类示例数据如下：

		![c3ux1ti6y4](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505095125.png)
	
  	资讯分类
	
  	资讯信息记录表示例数据如下：
	
  	![h92601e87p](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505095201.jpeg)

  需求 ：**取热门的资讯信息列表且每个类别只取前3条。**

  **核心思想**

  一般意义上我们在取前N条记录时候，都是根据某个业务字段进行降序排序，然后取前N条就能实现。

  形如 `select * from info order by views asc limit 0,3`，这条SQL就是取info表中的前3条记录。

  但是当你仔细阅读我们的题目要求，你会发现：**“它是让你每个类型下都要取浏览量的前3条记录”**。

  一种比较简单但是粗暴的方式就是在Java代码中循环所有的资讯类型，取出每个类型的前3条记录，最后进行汇总。虽然这种方式也能实现我们的要求，但存在很严重的弊端，有可能会发送多次（夸张的说成百上千次也是有可能）sql语句，这种程序显然是有重大缺陷的。

  但是，我们换一种思路。我们想在查询每条资讯记录时要是能查出其所在类型的排名就好了，然后根据排名字段进行过滤就好了。这时候我们就想到了子查询，而且MySQL是可以实现这样的功能子查询的。要计算出某条资讯信息的在同资讯分类下所有记录中排第几名，换成算出 **有多少条浏览量比当前记录的浏览量高，然后根据具体的多少（N）条+1就是N+1就是当前记录所在其分类下的的排名。**

  假如以本文上面的示例数据说明：就是在计算每个资讯信息记录时，多计算出一列作为其“排名”字段，然后取“排名”字段的小于等于3的记录即可。如果这里还不是很理解的话，就先看下面的SQL,然后根据SQL再回过头来理解这段话。

  **SQL实现**

  - **方法一**

    SQL语句：

      ```javascript
           SELECT t.* from (
               SELECT
                   t1.*,
                   (SELECT count(*) + 1 FROM info t2 WHERE t2.info_type_id = t1.info_type_id AND t2.views > t1.views ) top
               FROM
               info t1
           ) t where top <=3 order by t.info_type_id,top
      ```

		查询结果:

		![87uf2cb77i](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505095333.jpeg)

		说明： 分析top字段的子查询，发现其满足条件有两个：其一是info_type_id和当前记录的type_id相等；其二是info表所有记录大于 当前记录的浏览量且info_type_id相等的记录数量（假设为N），所有N+1就等于当前记录在其分类下的按照浏览量降序排名。

  - **方法二**

		SQL语句：

      ```javascript
          SELECT
              t1.*    
          FROM
          info t1
          where (SELECT count(*) + 1 FROM info t2 WHERE t2.info_type_id = t1.info_type_id AND t2.views > t1.views ) <=3
          ORDER BY t1.info_type_id
      ```
    
      查询结果
    
      ![5t9v4z0uxz](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505095432.jpeg)
    
      说明: 方法二可以看做是方法一的变体。

  - **方法三**

		SQL语句

      ```javascript
          SELECT
              t1.*    
          FROM
          info t1
          where exists (SELECT count(*) + 1 FROM info t2 WHERE t2.info_type_id = t1.info_type_id AND t2.views > t1.views having (count(*) + 1) <= 3) 
          ORDER BY t1.info_type_id
      ```

		查询结果:

		![q1u9o5p6mb](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505095502.jpeg)

- 

## 4. 数据添加

```sql
# 插入数据
INSERT INTO tb_name VALUES ('Gates', 'Bill', 'ACC', 'Beijing');
# 插入指定列的数据
INSERT INTO tb_name (tb_field1, tb_field2) VALUES ('Gates', 'Bill');
```



## 5. 数据修改

```undefined
包括数据表属性的修改，一个记录的数据的修改
```

### 5.1 数据修改

```sql
UPDATE tb_name SET tb_field1 = 'One', tb_field2 = 'Two' WHERE tb_field3 = '111';
```

### 5.2 属性修改

```sql
# 添加列
ALTER TABLE tb_name ADD COLUMN state ENUM('1','0') NOT NULL DEFAULT '1';
# 添加唯一約束
1) ALTER TABLE tb_name ADD UNIQUE (name);
2) CREATE UNIQUE INDEX [索引名称] ON tb_name(tb_field);
# 更改列属性
ALTER TABLE tb_name MODIFY COLUMN tb_field VARCHAR(50);
# 更改列名
ALTER TABLE tb_name RNAME COLUMN tb_fieldA to tb_fieldB;
ALTER TABLE tb_name CHANGE tb_fieldA tb_fieldB varchar(255);
```





## 6. 数据删除

```sql
# 删除一个记录
DELETE FROM tb_name WHERE name = 'Bill';
# 删除所有行
DELETE FROM tb_name;
# 删除表
DROP TABLE tb_name;
# 删除数据库
DROP DATABASE db_name;
# 删除索引
ALTER TABLE tb_name DROP INDEX [索引名称];
# 删除列
ALTER TABLE tb_name  DROP COLUMN column_name;
```



## 7. 其他

```sql
# 导出数据库
mysqldump -u [用户名] -p [数据库名] > [导出的文件名] 
mysqldump -u username -p db_name > db_name.sql;
# 导出数据表
mysqldump -u username -p db_name tb_name> db_name.sql;
# 导入数据库
1) mysql -u username -p db_name < [xxx.sql];
2) use db_name;
source xxx.sql;
```

- **导入sql文本**

  有时，对多条SQL语句进行操作时，我们一条一条的写入不是很方便，尤其是在有事务操作和结构化语句时，就更加困难，因此，我们可以将SQL语句事先写好，保存在文本中，然后一次性导入到数据库中，我们使用如下命令对sql文本进行导入。

  ```
  “source ”+文本文件
  
  source c:/createtable.sql
  ```

  ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210505094737.png)