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

> ***提示: ***查看数据库当前编码方式，使用命令：
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



## 2. 常用数据类型

**int**：整型 
**double**：浮点型，例如double(5,2)表示最多5位，其中必须有2位小数，即最大值为999.99； 
**char**：固定长度字符串类型； char(10) ‘abc ’ 
**varchar**：可变长度字符串类型；varchar(10) ‘abc’ 
**text**：字符串类型; 
**blob**：字节类型； 
**date**：日期类型，格式为：yyyy-MM-dd； 
**time**：时间类型，格式为：hh:mm:ss 
**timestamp**：时间戳类型 yyyy-MM-dd hh:mm:ss 会自动赋值 
**datetime**:日期时间类型 yyyy-MM-dd hh:mm:ss



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

