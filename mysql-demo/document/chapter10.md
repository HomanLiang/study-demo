[toc]

# MySQL 高可用



## keepalived+MySQL实现高可用
### keepalived概述
Keepalived通过VRRP（虚拟路由冗余协议）协议实现虚拟IP的漂移。当master故障后，VIP会自动漂移到backup，这时通知下端主机刷新ARP表，如果业务是通过VIP连接到服务器的，则此时依然能够连接到正常运行的主机，<u>RedHat</u> 给出的VRRP工作原理如下图：
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307132701.png)
本来对VIP漂移有一定了解的我，看了上面的图后，越来越懵了。因此只能根据我的个人理解，来对keepalived的VIP漂移做一个解释了，假设我现在有一套这样的环境：
主机A的IP地址为：192.168.10.11
主机B的IP地址为：192.168.10.12

我们再单独定义一个keepalived使用的VIP：192.168.10.10

当2台主机安装了keepalive并正常运行时，keepalive会选择一个节点做为主节点（这里假设为主机A，IP为192.168.10.11），由于A是主节点，所以主机A上还会生成一个IP地址192.168.10.10，即虚拟IP（Virtual IP，也称VIP），此时我们使用192.168.10.10访问主机，访问到的主机是A；假如A主机上的keepalived由于某些原因(例如服务器宕机、用户主动关闭…)关闭了，keepalived备用节点会检查与主节点keepalived的通信是否正常，检测到不正常，则会提升一个备节点为主节点，相应的虚拟IP也会在对应的主机上生成，从而实现高可用的目的。
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307132802.png)
### MySQL是如何结合keepalived实现高可用的
在MySQL中，通过搭建MySQL双主复制，保持2台主机上的MySQL数据库一模一样，并在2台主机上安装keepalived软件，启用VIP，用户应用程序通过VIP访问数据库。当包含VIP的主机上的数据库发生故障时，关闭keepalived，从而将VIP漂移到另一个节点，用户依然可以正常访问数据库。 （这里需要注意，虽然MySQL架构双主复制，2个节点都可以写入数据，但是我们在使用的时候，是通过VIP访问其中一个实例，并没有2个数据库实例一起使用）。这里我简单画了一个流程图，来说明keepalive与MySQL实现高可用的过程：
![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307132803.png)

### keepalived+MySQL实现高可用过程实现
基础环境规划：
![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mysql-demo/20210307132904.png)

#### 搭建MySQL双主复制环境
1. **STEP1：安装MySQL过程**见：https://www.cnblogs.com/lijiaman/p/10743102.html

2. **STEP2：配置双主复制参数**
服务器A
   
    ```
    [mysqld]
    basedir=/usr/local/mysql
 datadir=/mysql/data
   
    server_id = 1
    binlog_format=ROW
    log_bin=/mysql/binlog/master-bin
    auto-increment-increment = 2            #字段变化增量值
    auto-increment-offset = 1               #初始字段ID为1
    slave-skip-errors = all                 #忽略所有复制产生的错误
    gtid_mode=ON
    enforce-gtid-consistency=ON
 ```
   服务器B
    ```
    [mysqld]
    basedir=/usr/local/mysql
 datadir=/mysql/data
   
    server_id = 2
    binlog_format=ROW
    log_bin=/mysql/binlog/master-bin
    auto-increment-increment = 2            #字段变化增量值
    auto-increment-offset = 2               #初始字段ID为2
    slave-skip-errors = all                 #忽略所有复制产生的错误
    gtid_mode=ON
    enforce-gtid-consistency=ON
    ```
3. **STEP3：创建复制用户，2个数据库上都要创建**
```
grant replication slave on *.* to 'rep'@'%' identified by '123';
```
4. **STEP4：将hosta的数据拷贝到hostb，并应用**
```
[root@hostb ~]# mysqldump -uroot -p123456 -h 192.168.10.11 --single-transaction --all-databases --master-data=2  > hosta.sql
[root@hostb ~]# mysql -uroot -p123456 < hosta.sql
```
5. **STEP5：hostb上开启复制，以下脚本在hostb上执行**
```
-- 配置复制
mysql> CHANGE MASTER TO
     ->   master_host='192.168.10.11',
     ->   master_port=3306,
     ->   master_user='rep',
     ->   master_password='123',
     ->   MASTER_AUTO_POSITION = 1;
Query OK, 0 rows affected, 2 warnings (0.01 sec)

-- 开启复制
mysql> start slave;
 Query OK, 0 rows affected (0.00 sec)


-- 查看复制状态
mysql> show slave status \G
 *************************** 1. row ***************************
                Slave_IO_State: Waiting for master to send event
                   Master_Host: 192.168.10.11
                   Master_User: rep
                   Master_Port: 3306
                 Connect_Retry: 60
               Master_Log_File: master-bin.000001
           Read_Master_Log_Pos: 322
                Relay_Log_File: hostb-relay-bin.000002
                 Relay_Log_Pos: 417
         Relay_Master_Log_File: master-bin.000001
              Slave_IO_Running: Yes

             Slave_SQL_Running: Yes
```
6. **STEP6：hosta上开启复制，以下脚本在hosta上执行**

```
mysql> CHANGE MASTER TO
    ->    master_host='192.168.10.12',
    ->    master_port=3306,
    ->    master_user='rep',
    ->    master_password='123',
    ->    MASTER_AUTO_POSITION = 1;
Query OK, 0 rows affected, 2 warnings (0.01 sec)

mysql> start slave;
Query OK, 0 rows affected (0.01 sec)

mysql> show slave status \G;
*************************** 1. row ***************************
               Slave_IO_State: Waiting for master to send event
                  Master_Host: 192.168.10.12
                  Master_User: rep
                  Master_Port: 3306
                Connect_Retry: 60
              Master_Log_File: master-bin.000001
          Read_Master_Log_Pos: 154
               Relay_Log_File: hosta-relay-bin.000002
                Relay_Log_Pos: 369
        Relay_Master_Log_File: master-bin.000001
             Slave_IO_Running: Yes
            Slave_SQL_Running: Yes
```
7. **STEP7：测试双主复制**

在hosta上创建数据库testdb，到hostb服务器上查看数据库是否已经创建

```
-- hosta上创建数据库
create database testdb；


--hostb上查看数据库，发现已经创建
mysql> show databases;
 +--------------------+
 | Database           |
 +--------------------+
 | information_schema |
 | db1                |
 | lijiamandb         |
 | mysql              |
 | performance_schema |
 | sbtest             |
 | sys                |
 | testdb             |
 +--------------------+
 8 rows in set (0.01 sec)
```
在hostb的testdb数据库上创建表t1,并插入数据，到hosta上查看是否复制过来

```
-- 在hostb上创建表并插入数据
mysql> use testdb
Database changed
mysql> create table t1(id int,name varchar(20));
Query OK, 0 rows affected (0.01 sec)

mysql> insert into t1 values(1,'a');
Query OK, 1 row affected (0.01 sec)


-- 在hosta上查看数据，数据已经过来
mysql> select * from testdb.t1;
+------+------+
| id   | name |
+------+------+
|    1 | a    |
+------+------+
1 row in set (0.00 sec)
```
到这，双主复制已经搭建完成，接下来安装配置keepalived。



#### 安装配置keepalived

1、**keepalived的安装与管理**

keepalived可以使用源码安装，也可以使用yum在线安装，这里直接使用yum在线安装:
```
[root@hosta data]# yum install -y keepalived
```
使用如下命令查看安装路径:
```
[root@hosta data]# rpm -ql keepalived
/etc/keepalived
/etc/keepalived/keepalived.conf
/etc/sysconfig/keepalived
/usr/bin/genhash
/usr/lib/systemd/system/keepalived.service
/usr/libexec/keepalived
/usr/sbin/keepalived
/usr/share/doc/keepalived-1.3.5… 略
```
使用如下命令管理keepalived
```
# 开启keepalived
systemctl start keepalived 或者 service keepalived start 

# 关闭keepalived
systemctl stop keepalived 或者 service keepalived stop 

# 查看keepalived运行状态
systemctl status keepalived 或者 service keepalived status

# 重新启动keepalived
systemctl restart keepalived 或者 service keepalived restart
```
2、**keepalived的配置**

keepalived的配置文件为：/etc/keepalived/keepalived.conf，我的配置文件如下：

【hosta主机的配置文件】
```
[root@hosta keepalived]# cat keepalived.conf
! Configuration File for keepalived
       
global_defs {
notification_email {
ops@wangshibo.cn
tech@wangshibo.cn
}
       
notification_email_from ops@wangshibo.cn
smtp_server 127.0.0.1 
smtp_connect_timeout 30
router_id MASTER-HA
}
       
vrrp_script chk_mysql_port {       #检测mysql服务是否在运行。有很多方式，比如进程，用脚本检测等等
    script "/mysql/chk_mysql.sh"   #这里通过脚本监测
    interval 2                     #脚本执行间隔，每2s检测一次
    weight –5                      #脚本结果导致的优先级变更，检测失败（脚本返回非0）则优先级 -5
    fall 2                         #检测连续2次失败才算确定是真失败。会用weight减少优先级（1-255之间）
    rise 1                         #检测1次成功就算成功。但不修改优先级
}
       
vrrp_instance VI_1 {
    state BACKUP                  #这里所有节点都定义为BACKUP
    interface ens34               #指定虚拟ip的网卡接口
    mcast_src_ip 192.168.10.11    #本地IP 
    virtual_router_id 51          #路由器标识，MASTER和BACKUP必须是一致的
    priority 101                  #定义优先级，数字越大，优先级越高，在同一个vrrp_instance下，MASTER的优先级必须大于BACKUP的优先级。 
    advert_int 1
    nopreempt                     #不抢占模式，在优先级高的机器上设置即可，优先级低的机器可不设置         
    authentication {   
        auth_type PASS 
        auth_pass 1111     
    }
    virtual_ipaddress {    
        192.168.10.10             #虚拟IP 
    }
      
track_script {               
   chk_mysql_port             
}
}
```
【hostb主机的配置文件】
```
[root@hostb keepalived]# cat keepalived.conf
! Configuration File for keepalived
       
global_defs {
notification_email {
ops@wangshibo.cn
tech@wangshibo.cn
}
       
notification_email_from ops@wangshibo.cn
smtp_server 127.0.0.1 
smtp_connect_timeout 30
router_id MASTER-HA
}
       
vrrp_script chk_mysql_port {
    script "/mysql/chk_mysql.sh"
    interval 2            
    weight -5                 
    fall 2                 
    rise 1               
}
       
vrrp_instance VI_1 {
    state BACKUP
    interface ens34 
    mcast_src_ip 192.168.10.12
    virtual_router_id 51    
    priority 99          
    advert_int 1         
    authentication {   
        auth_type PASS 
        auth_pass 1111     
    }
    virtual_ipaddress {    
        192.168.10.10
    }
      
track_script {               
   chk_mysql_port             
}
}
```
需要特别注意：nopreempt这个参数只能用于state为BACKUP的情况，所以在配置的时候要把master和backup的state都设置成BACKUP，这样才会实现keepalived的非抢占模式！

在配置完成之后，启动MySQL数据库和keepalive，需要注意，先启动MySQL，再启动keepalive，因为keepalive启动后会检测MySQL的运行状态，如果MySQL运行异常，keepalive会自动关闭。

文章：[keepalived+MySQL实现高可用](https://www.cnblogs.com/lijiaman/p/13430668.html)