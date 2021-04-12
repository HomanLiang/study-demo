[toc]



# Linux 常用命令

## vi/vim
![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411152739.png)
## 关机&重启命令
`shutdown -h now` [立刻关机]

`shutdown -h  1`   "1分钟，关机."   [1分钟后，关机]

`shutdown -r  now`   [立刻重启]

`shutdown -r  2`  "2分钟后，重启"

`halt`  【立刻关机】

`reboot`  【立刻重启】

在重启和关机前，通常需要先执行，`sync`  [把内存的数据，写入磁盘]

## 注销用户
`logout`

## 用户管理
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411152848.png)
### 添加用户
`useradd  用户名`

### 指定/修改密码
`passwd    用户名`    // 如果没有带用户名，则是给当前登录的用户修改密码

### 删除用户
`userdel   用户名`

删除用户xiaoming，但是要保留家目录  `userdel 用户名` // `userdel xiaoming`（一般保留家目录）

删除用户以及用户主目录   // `userdel –r  xiaoming` 【小心使用】

### 查询用户信息指令
`id  用户名`

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411153006.png)

### 切换用户
`su  –  切换用户名`

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411153023.png)


## 用户组
### 新增组
`groupadd 组名`

### 增加用户时直接加上组
`useradd  –g 用户组 用户名`

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411153058.png)

### 删除组
`groupdel 组名`

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411153113.png)

### 修改用户的组
`usermod  –g 新的组名 用户名`

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411153124.png)

## 帮助指令
### man 获得帮助信息
`man [命令或配置文件]`（功能描述：获得帮助信息）

`man ls`

### help指令
`help 命令` （功能描述：获得shell内置命令的帮助信息）

`help cd`

## 文件和目录相关的指令
### pwd 指令
`pwd` (功能描述：显示当前工作目录的绝对路径)

### ls指令
`ls     [选项]    [目录或是文件]`

常用选项

- `-a` ：显示当前目录所有的文件和目录，包括隐藏的 (文件名以.开头就是隐藏)。
- `-l ` ：以列表的方式显示信息
- `-h`  : 显示文件大小时，以 k , m, G单位显示

### cd 指令
`cd  [参数]`     (功能描述：切换到指定目录)

### mkdir指令 [make directory]
`mkdir  [选项]  要创建的目录`

常用选项

-  `-p` ：创建多级目录

### rmdir指令 [remove directory]
`rmdir  [选项]  要删除的空目录`

常用选项

- `-r`: 表示递归删除，就是将该目录下的文件和子目录全部删除
- `-f`: 表示强制删除，就是不需询问

### touch指令
`touch 文件名称`

### cp 指令拷贝文件到指定目录
`cp [选项]  source【源】  dest【目的文件】`

常用选项

- `-r `：递归复制整个文件夹

### rm指令
`rm  [选项]  要删除的文件或目录`
常用选项

- `-r` ：递归删除整个文件夹
-  `-f` ： 强制删除不提示

### mv指令
`mv  oldNameFile newNameFile`     (功能描述：重命名)

`mv /temp/movefile /targetFolder` (功能描述：移动文件或目录)

### cat指令
`cat  [选项] 要查看的文件`

常用选项

- `-n`：显示行号

应用实例

案例1:  

/ect/profile  文件内容，并显示行号

`cat  -n  /etc/profile  |   more`

说明：如果需要一行行，输入 enter 

如果需要翻页 ，输入空格键.

如果需要退出，输入 q

### more指令
`more 要查看的文件`

**快捷键：**

|      操作      |                 功能说明                 |
| :------------: | :--------------------------------------: |
| 空白键 (space) |             代表向下翻一页；             |
|     Enter      |           代表向下翻『一行』；           |
|       q        | 代表立刻离开 more ，不再显示该文件内容。 |
|     Ctrl+F     |               向下滚动一屏               |
|     Ctrl+B     |                返回上一屏                |
|       =        |             输出当前行的行号             |
|       :f       |         输出文件名和当前行的行号         |
### less指令
less指令用来分屏查看文件内容，它的功能与more指令类似，但是比more指令更加强大，支持各种显示终端。less指令在显示文件内容时，并不是一次将整个文件加载之后才显示，而是根据显示需要加载内容，对于显示大型文件具有较高的效率

`less 要查看的文件`

### echo指令
echo输出内容到控制台

`echo  [选项]  [输出内容]`

### head指令
head用于显示文件的开头部分内容，默认情况下head指令显示文件的前10行内容

`head 文件`      (功能描述：查看文件头10行内容)

`head -n 5 文件`      (功能描述：查看文件头5行内容，5可以是任意行数)

### tail指令
tail用于输出文件中尾部的内容，默认情况下tail指令显示文件的后10行内容
```
命令格式: tail[必要参数][选择参数][文件]
-f 循环读取
-q 不显示处理信息
-v 显示详细的处理信息
-c<数目> 显示的字节数
-n<行数> 显示行数
-q, --quiet, --silent 从不输出给出文件名的首部
-s, --sleep-interval=S 与-f合用,表示在每次反复的间隔休眠S秒
```
用法如下：
```
tail  文件       （功能描述：查看文件头10行内容）
tail  -n  10   test.log   查询日志尾部最后10行的日志;
tail  -n +10   test.log   查询10行之后的所有日志;
tail  -fn 10   test.log   循环实时查看最后1000行记录(最常用的)
tail  -f  文件  （功能描述：实时追踪该文档的所有更新）
```
一般还会配合着grep用，例如 :
```
tail -fn 1000 test.log | grep '关键字'
```
如果一次性查询的数据量太大,可以进行翻页查看，例如:
```
tail -n 4700 aa.log |more -1000 可以进行多屏显示(ctrl + f 或者 空格键可以快捷键)
```

### > 指令 和 >> 指令
`ls -l >文件`           （功能描述：列表的内容写入文件a.txt中（覆盖写））

`ls -al >>文件`         （功能描述：列表的内容追加到文件aa.txt的末尾）

`cat 文件1 > 文件2` （功能描述：将文件1的内容覆盖到文件2）

`echo "内容">> 文件`

### ln 指令 (link)
软链接也成为符号链接，类似于windows里的快捷方式，主要存放了链接其他文件的路径

`ln -s [原文件或目录] [软链接名]` （功能描述：给原文件创建一个软链接）

### history指令
查看已经执行过历史命令,也可以执行历史指令

`history`   （功能描述：查看已经执行过历史命令）

## 时间日期类
### date指令-显示当前日期
`date`      （功能描述：显示当前时间）

`date +%Y`   （功能描述：显示当前年份）

`date +%m`    （功能描述：显示当前月份）

`date +%d`    （功能描述：显示当前是哪一天）

`date "+%Y-%m-%d %H:%M:%S"`（功能描述：显示年月日时分秒）

### 使用date指令设置最新时间
`date  -s`  字符串时间

### cal指令
查看日历指令

`cal [选项]`      （功能描述：不加选项，显示本月日历）

`cal  2020`

## 搜索查找类
### find指令
find指令将从指定目录向下递归地遍历其各个子目录，将满足条件的文件或者目录显示在终端

`find  [搜索范围]  [选项] `

**选项说明**

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154009.png)

**应用实例**

案例1: 按文件名：根据名称查找/home 目录下的hello.txt文件

`find  /home   -name  hello.txt `

案例2：按拥有者：查找/opt目录下，用户名称为 nobody的文件       

`find  /opt    -user  nobody`

案例3：查找整个linux系统下大于10M的文件（+n 大于  -n小于   n等于）

`find   /  -size   +10M`

### locate指令
locate指令可以快速定位文件路径。locate指令利用事先建立的系统中所有文件名称及路径的locate数据库实现快速定位给定的文件。

Locate指令无需遍历整个文件系统，查询速度较快。为了保证查询结果的准确度，管理员必须定期更新locate时刻

`locate 搜索文件`

注意：由于locate指令基于数据库进行查询，所以第一次运行前，必须使用updatedb指令创建locate数据库

**应用实例**

案例1: 请使用locate 指令快速定位 hello.txt 文件所在目录

![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154054.png)

### grep指令和 管道符号 |
grep 过滤查找 ， 管道符，“|”，表示将前一个命令的处理结果输出传递给后面的命令处理

`grep [选项] 查找内容 源文件`

grep常用命令：
```
# 基本使用
grep yoursearchkeyword f.txt     #文件查找
grep 'KeyWord otherKeyWord' f.txt cpf.txt #多文件查找, 含空格加引号
grep 'KeyWord' /home/admin -r -n #目录下查找所有符合关键字的文件
grep 'keyword' /home/admin -r -n -i # -i 忽略大小写
grep 'KeyWord' /home/admin -r -n --include *.{vm,java} #指定文件后缀
grep 'KeyWord' /home/admin -r -n --exclude *.{vm,java} #反匹配

# cat + grep
cat f.txt | grep -i keyword # 查找所有keyword且不分大小写  
cat f.txt | grep -c 'KeyWord' # 统计Keyword次数

# seq + grep
seq 10 | grep 5 -A 3    #上匹配
seq 10 | grep 5 -B 3    #下匹配
seq 10 | grep 5 -C 3    #上下匹配，平时用这个就妥了
```
Grep的参数：
```
--color=auto：显示颜色;
-i, --ignore-case：忽略字符大小写;
-o, --only-matching：只显示匹配到的部分;
-n, --line-number：显示行号;
-v, --invert-match：反向显示,显示未匹配到的行;
-E, --extended-regexp：支持使用扩展的正则表达式;
-q, --quiet, --silent：静默模式,即不输出任何信息;
-w, --word-regexp：整行匹配整个单词;
-c, --count：统计匹配到的行数; print a count of matching lines;

-B, --before-context=NUM：print NUM lines of leading context   后#行 
-A, --after-context=NUM：print NUM lines of trailing context   前#行 
-C, --context=NUM：print NUM lines of output context           前后各#行 
```




## 压缩和解压类
### gzip/gunzip 指令
gzip 用于压缩文件， gunzip 用于解压的

`gzip 文件`    （功能描述：压缩文件，只能将文件压缩为*.gz文件）

`gunzip 文件.gz`     （功能描述：解压缩文件命令）

![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154132.png)

### zip/unzip 指令
zip 用于压缩文件， unzip 用于解压的，这个在项目打包发布中很有用的

`zip      [选项] XXX.zip  需要压缩的内容`（功能描述：压缩文件和目录的命令）

**常用选项**

- `-r`：递归压缩，即压缩目录

- `unzip [选项] XXX.zip`  （功能描述：解压缩文件）

unzip的常用选项

- `-d<目录>` ：指定解压后文件的存放目录

### tar 指令
tar 指令 是打包指令，最后打包后的文件是 .tar.gz 的文件。 [可以压缩，和解压]

tar  [选项]  XXX.tar.gz  打包的内容/目录   (功能描述：打包目录，压缩后的文件格式.tar.gz)  

选项说明

![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154315.png)

**应用实例**

1. 案例1:  压缩多个文件，将 /home/a1.txt 和 /home/a2.txt 压缩成  a.tar.gz    

  `tar  -zcvf  a.tar.gz  a1.txt  a2.txt`  [注意，路径要写清楚]

2. 案例2:  将/home 的文件夹 压缩成 myhome.tar.gz

  `tar  -zcvf   myhome.tar.gz   /home/`  [注意，路径写清楚]

3. 案例3:   将 a.tar.gz  解压到当前目录

  `tar -zxvf  a.tar.gz` 

4. 案例4: 将myhome.tar.gz  解压到 /opt/tmp2目录下 【-C】 

  `tar -zxvf myhome.tar.gz  -C  /opt/tmp2`  [注意; /opt/tmp2 事先需要创建好]

## 权限管理
### 查看文件的所有者
指令：`ls –ahl`

![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154332.png)

### 修改文件所有者
指令：`chown 用户名 文件名`

![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154341.png)

### 修改文件所在的组
`chgrp 组名 文件名`

### 改变用户所在组
在添加用户时，可以指定将该用户添加到哪个组中，同样的用root的管理权限可以改变某个用户所在的组

改变用户所在组

- `usermod   –g   组名  用户名`
-  `usermod   –d   目录名  用户名  改变该用户登陆的初始目录`。

### 权限的基本介绍
![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154419.png)

![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154438.png)

### 修改权限-chmod
![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154515.png)

![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154657.png)

### 修改文件所有者-chown
`chown  newowner  file`  改变文件的所有者

`chown  newowner:newgroup  file`  改变用户的所有者和所有组

`-R`   如果是目录 则使其下所有子文件或目录递归生效

### 修改文件所在组-chgrp
`chgrp newgroup file`  改变文件的所有组

### 其它用户的身份来运行命令 -sudo

![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154716.png)


## 系统监控
### ps 命令

**显示系统执行的进程**

`ps -aux` // 显示所有的进程

`ps -aux | grep sshd` //查看sshd进程

![Image [19]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154740.png)

**ps详解**

指令：`ps –aux|grep xxx` ，比如我看看有没有sshd服务
### kill 和 kill all 命令
若是某个进程执行一半需要停止时，或是已消了很大的系统资源时，此时可以考虑停止该进程。使用kill命令来完成此项任务

`kill  [选项] 进程号`（功能描述：通过进程号杀死进程 -9 强制终止）

`kill all 进程名称`     （功能描述：通过进程名称杀死进程，也支持通配符，这在系统因负载过大而变得很慢时很有用）
**常用选项：**

- -9 :表示强迫进程立即停止

### pstree 命令

查看进程树

`pstree [选项]` ,可以更加直观的来看进程信息

常用选项：

- -p :显示进程的PID

- -u :显示进程的所属用户

### uptime 命令

uptime命令显示的平均负载包括了正在或准备运行在CPU上的进程和阻塞在不可中断睡眠状态(uninterruptible) I/O(通常是磁盘I/O)上的进程。

```
[root@server ~]# uptime
 16:54:53 up 29 days,  2:02,  1 user,  load average: 0.03, 0.03, 0.00
[root@server ~]# cat /proc/loadavg
0.03 0.03 0.00 3/166 16903
```

- 显示最近1分钟、5分钟、15分钟系统负载的移动平均值，它们共同展现了负载随时间变动的情况。
- 3：正在运行的进程数，166：总的进程数，16903：最近运行进程的ID。

### 服务(service)管理

服务(service) 本质就是进程，但是是运行在后台的，通常都会监听某个端口，等待其它程序的请求，比如(mysql , sshd  防火墙等)，因此我们又称为守护进程，是Linux中非常重要的知识点

**service管理指令：**

`service  服务名 [start | stop | restart | reload | status]`

在CentOS7.0后 不再使用service ,而是 systemctl

启动一个服务：`systemctl start firewalld.service`

关闭一个服务：`systemctl stop firewalld.service`

重启一个服务：`systemctl restart firewalld.service`

显示一个服务的状态：`systemctl status firewalld.service`

在开机时启用一个服务：`systemctl enable firewalld.service`

在开机时禁用一个服务：`systemctl disable firewalld.service`

查看服务是否开机启动：`systemctl s-enabled firewalld.service`

查看已启动的服务列表：`systemctl list-unit-files|grep enabled`

查看启动失败的服务列表：`systemctl --failed`

### TOP命令 - 动态监控进程
top与ps命令很相似。它们都用来显示正在执行的进程。Top与ps最大的不同之处，在于top在执行一段时间可以更新正在运行的的进程(默认每3秒变化一次)

`top [选项]`

![Image [20]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411154929.png)

**示例**

打印指定pid进程的cpu信息，间隔时间为1s，打印20次

![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411183707.png)

1. 查看进程的pid：

   ```
   ps -ef | grep systemd
   ```

   ![img](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411183734.png)

2. 循环打印

   ```
   # 打印一次
   top -p 1 -n 1 | grep systemd | awk '{print $10}'
   # 循环打印20次
   for i in {1..20};do top -p 1 -n 1 | grep systemd | awk '{print $10}';sleep 1s;done
   
   for((i=0;i<20;i++));do top -p 1 -n 1 | grep systemd | awk '{print $10}';sleep 1s;done
   ```

### dmesg | tail 命令 - 显示最新的10个系统信息

默认显示最新的10个系统信息，可以查看导致性能问题的错误信息。

1. 显示最新的20个系统信息

   ```
   [root@centos7 ~]# dmesg | tail -20
   [   15.356358] RPC: Registered named UNIX socket transport module.
   [   15.356360] RPC: Registered udp transport module.
   [   15.356361] RPC: Registered tcp transport module.
   [   15.356362] RPC: Registered tcp NFSv4.1 backchannel transport module.
   [   15.551529] type=1305 audit(1584428235.986:4): audit_pid=1054 old=0 auid=4294967295 ses=4294967295 subj=system_u:system_r:auditd_t:s0 res=1
   [   19.223990] NET: Registered protocol family 40
   [   23.857606] ip6_tables: (C) 2000-2006 Netfilter Core Team
   [   24.130255] Ebtables v2.0 registered
   [   24.366128] Netfilter messages via NETLINK v0.30.
   [   24.418582] ip_set: protocol 7
   [   24.517273] IPv6: ADDRCONF(NETDEV_UP): ens33: link is not ready
   [   24.521156] e1000: ens33 NIC Link is Up 1000 Mbps Full Duplex, Flow Control: None
   [   24.524658] IPv6: ADDRCONF(NETDEV_UP): ens33: link is not ready
   [   24.524669] IPv6: ADDRCONF(NETDEV_CHANGE): ens33: link becomes ready
   [   24.528687] IPv6: ADDRCONF(NETDEV_UP): ens34: link is not ready
   [   24.532350] e1000: ens34 NIC Link is Up 1000 Mbps Full Duplex, Flow Control: None
   [   24.535760] IPv6: ADDRCONF(NETDEV_UP): ens34: link is not ready
   [   24.574912] IPv6: ADDRCONF(NETDEV_UP): ens34: link is not ready
   [   25.391535] nf_conntrack version 0.5.0 (16384 buckets, 65536 max)
   [   25.525351] IPv6: ADDRCONF(NETDEV_CHANGE): ens34: link becomes ready
   [root@centos7 ~]#
   ```

2. 显示开始的20个系统信息

   ```
   [root@centos7 ~]# dmesg | head -20
   [    0.000000] Initializing cgroup subsys cpuset
   [    0.000000] Initializing cgroup subsys cpu
   [    0.000000] Initializing cgroup subsys cpuacct
   [    0.000000] Linux version 3.10.0-1062.el7.x86_64 (mockbuild@kbuilder.bsys.centos.org) (gcc version 4.8.5 20150623 (Red Hat 4.8.5-36) (GCC) ) #1 SMP Wed Aug 7 18:08:02 UTC 2019
   [    0.000000] Command line: BOOT_IMAGE=/vmlinuz-3.10.0-1062.el7.x86_64 root=UUID=d7dc0c9e-a27d-4239-aba4-7c2e51d9fc93 ro crashkernel=auto spectre_v2=retpoline rhgb quiet LANG=en_US.UTF-8
   [    0.000000] Disabled fast string operations
   [    0.000000] e820: BIOS-provided physical RAM map:
   [    0.000000] BIOS-e820: [mem 0x0000000000000000-0x000000000009ebff] usable
   [    0.000000] BIOS-e820: [mem 0x000000000009ec00-0x000000000009ffff] reserved
   [    0.000000] BIOS-e820: [mem 0x00000000000dc000-0x00000000000fffff] reserved
   [    0.000000] BIOS-e820: [mem 0x0000000000100000-0x000000007fedffff] usable
   [    0.000000] BIOS-e820: [mem 0x000000007fee0000-0x000000007fefefff] ACPI data
   [    0.000000] BIOS-e820: [mem 0x000000007feff000-0x000000007fefffff] ACPI NVS
   [    0.000000] BIOS-e820: [mem 0x000000007ff00000-0x000000007fffffff] usable
   [    0.000000] BIOS-e820: [mem 0x00000000f0000000-0x00000000f7ffffff] reserved
   [    0.000000] BIOS-e820: [mem 0x00000000fec00000-0x00000000fec0ffff] reserved
   [    0.000000] BIOS-e820: [mem 0x00000000fee00000-0x00000000fee00fff] reserved
   [    0.000000] BIOS-e820: [mem 0x00000000fffe0000-0x00000000ffffffff] reserved
   [    0.000000] NX (Execute Disable) protection: active
   [    0.000000] SMBIOS 2.7 present.
   [root@centos7 ~]#
   ```

### 监控网络状态 netstat

`netstat [选项]`

选项说明 

- `-an`  按一定顺序排列输出

- `-p`  显示哪个进程在调用

请查看服务名为 sshd 的服务的信息。

`netstat –anp | grep sshd`

### free命令
free 命令能够显示系统中物理上的空闲和已用内存，还有交换内存，同时，也能显示被内核使用的缓冲和缓存
```
free [param]
```
**param可以为：**

- -b：以Byte为单位显示内存使用情况；
- -k：以KB为单位显示内存使用情况；
- -m：以MB为单位显示内存使用情况；
- -o：不显示缓冲区调节列；
- -s<间隔秒数>：持续观察内存使用状况；
- -t：显示内存总和列；
- -V：显示版本信息。

![Image [21]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155026.png)

**Mem：表示物理内存统计**

- total：表示物理内存总数(total=used+free)
- used：表示系统分配给缓存使用的数量(这里的缓存包括buffer和cache)
- free：表示未分配的物理内存总数
- shared：表示共享内存
- buffers：系统分配但未被使用的buffers 数量。
- cached：系统分配但未被使用的cache 数量。

- + buffers/cache：表示物理内存的缓存统计

- (-buffers/cache) 内存数： (指的第一部分Mem行中的used – buffers – cached)
- (+buffers/cache) 内存数: (指的第一部分Mem行中的free + buffers + cached)
(-buffers/cache)表示真正使用的内存数， (+buffers/cache) 表示真正未使用的内存数

**Swap**：表示硬盘上交换分区的使用情况

### vmstat 命令

全称 virtual memory stat，逐行输出虚拟内存状态统计信息

```
[root@centos7 ~]# vmstat
procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
1  0      0 1424832   2084 195100    0    0    47     4   45   55  0  0 99  1  0
```

每隔一秒打印一次

```
[root@centos7 ~]#
[root@centos7 ~]# vmstat 1   #1s打印一个
procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
1  0      0 1424472   2084 195120    0    0    28     2   30   37  0  0 99  1  0
0  0      0 1424456   2084 195120    0    0     0     0   38   53  0  0 100  0  0
0  0      0 1424456   2084 
```

参数解释：

- r: 运行队列中进程数量
- b: 等待IO的进程数量
- swpd：使用的虚拟内存
- free：可用内存
- buff：用作缓冲的内存大小
- cache：用作缓存的内存大小
- us：用户进程执行时间(user time)
- sy：系统进程执行时间(system time
- id：空闲时间(包括IO等待时间)，中央处理器的空闲时间
- wa：等待IO时间

### ulimit命令

ulimit用于显示系统资源限制的信息

语法：`ulimit [param]`

param参数可以为：

- -a 　显示目前资源限制的设定。
- -c <core文件上限> 　设定core文件的最大值，单位为区块。
- -d <数据节区大小> 　程序数据节区的最大值，单位为KB。
- -f <文件大小> 　shell所能建立的最大文件，单位为区块。
- -H 　设定资源的硬性限制，也就是管理员所设下的限制。
- -m <内存大小> 　指定可使用内存的上限，单位为KB。
- -n <文件数目> 　指定同一时间最多可开启的文件数。
- -p <缓冲区大小> 　指定管道缓冲区的大小，单位512字节。
- -s <堆叠大小> 　指定堆叠的上限，单位为KB。
- -S 　设定资源的弹性限制。
- -t <CPU时间> 　指定CPU使用时间的上限，单位为秒。
- -u <程序数目> 　用户最多可开启的程序数目。
- -v <虚拟内存大小> 　指定可使用的虚拟内存上限，单位为KB

### mpstat 命令

mpstat是Multiprocessor Statistics的缩写，实时监控CPU性能。
`mpstat -P ALL 1 2`：间隔1s打印报告，共打印2个

- -P ALL：监控所有CPU
- 1：间隔时间1s
- 2：打印次数2次

```
[root@centos7 ~]# mpstat
Linux 3.10.0-1062.el7.x86_64 (centos7)  03/18/2020      _x86_64_        (4 CPU)

04:41:47 AM  CPU    %usr   %nice    %sys %iowait    %irq   %soft  %steal  %guest  %gnice   %idle
04:41:47 AM  all    0.66    0.00    1.39    2.65    0.00    0.01    0.00    0.00    0.00   95.28
[root@centos7 ~]#
[root@centos7 ~]# mpstat -P ALL 1
Linux 3.10.0-1062.el7.x86_64 (centos7)  03/18/2020      _x86_64_        (4 CPU)

04:44:11 AM  CPU    %usr   %nice    %sys %iowait    %irq   %soft  %steal  %guest  %gnice   %idle
04:44:11 AM  all    0.39    0.00    0.82    1.54    0.00    0.01    0.00    0.00    0.00   97.24
04:44:11 
[root@centos7 ~]#
```

- %usr：间隔时间段内，用户态的CPU时间（%），不包含 nice值为负进程
- %nice：nice值为负进程的CPU时间（%）
- %sys：核心时间（%）
- %iowait：硬盘IO等待时间（%）
- %irq：硬中断时间（%）
- %soft：软中断时间（%）
- %steal：虚拟机管理器在服务另一个虚拟处理器时虚拟CPU处在非自愿等待下花费时间的百分比
- %guest：运行虚拟处理器时CPU花费时间的百分比
- %idle：CPU的空闲时间（%）

### pidstat 命令

pidstat用于监控全部或指定进程的资源占用情况，和top命令类似，但不覆盖输出，有利于观察数据随时间的变动情况，top会覆盖之前的输出

- `pidstat -p 1 1`：-p 指定进程号，间隔1s打印pid为1的进程

```
[root@centos7 ~]# pidstat
Linux 3.10.0-1062.el7.x86_64 (centos7)  03/18/2020      _x86_64_        (4 CPU)


04:52:29 AM   UID       PID    %usr %system  %guest    %CPU   CPU  Command
04:52:29 AM     0         1    0.05    0.19    0.00    0.24     0  systemd
04:52:29 AM     0         2    0.00    0.00    0.00    0.00     3  kthreadd
04:52:29 AM     0         6    0.00    0.00    0.00    0.00     0  ksoftirqd/0
04:52:29 
```

- PID：进程ID
- %usr：进程在用户空间占用cpu的百分比
- %system：进程在内核空间占用cpu的百分比
- %guest：进程在虚拟机占用cpu的百分比
- %CPU：进程占用cpu的百分比，各个CPU上的使用量的总和
- CPU：处理进程的cpu编号
- Command：当前进程对应的命令

### iostat 命令

iostat用于显示CPU和块设备（磁盘I/O）相关的统计信息

```
[root@centos7 ~]# iostat 1
Linux 3.10.0-1062.el7.x86_64 (centos7)  03/18/2020      _x86_64_        (4 CPU)

avg-cpu:  %user   %nice %system %iowait  %steal   %idle
           0.15    0.00    0.34    0.60    0.00   98.92


Device:            tps    kB_read/s    kB_wrtn/s    kB_read    kB_wrtn
sda               9.46       158.59        15.05     142895      13561
scd0              0.02         1.14         0.00       1028          0
```

avg-cpu：总体cpu使用情况统计信息

linux各种设备文件在/dev目录下可以看到

- tps：每秒进程向磁盘设备下发的IO读、写请求数量
- kB_read/s：每秒从驱动器读入的数据量
- kB_wrtn/s：每秒从驱动器写入的数据量
- kB read：读入数据总量
- kB wrtn：写入数据总量

### sar 命令

sar（System ActivityReporter）：系统活动情况报告，是Linux系统性能分析工具。可以用来分析磁盘I/O、CPU效率、内存使用等，下面介绍它的分析网络性能用法。

`sar -n DEV 1`  检查网络流量的工作负载，可用来检查网络流量是否已经达到限额。

```
[root@centos7 dev]# sar -n DEV 1
Linux 4.18.0-147.5.1.el8_1.x86_64 (iZ8vb54310gt89j8qct198Z)     12/19/2020      _x86_64_        (1 CPU)

08:08:37 PM     IFACE   rxpck/s   txpck/s    rxkB/s    txkB/s   rxcmp/s   txcmp/s  rxmcst/s   %ifutil
08:08:38 PM      eth0      4.00      2.00      0.23      0.27      0.00      0.00      0.00      0.00
08:08:38 PM        lo      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
08:08:38 PM   docker0      0.00      0.00      0.00      0.00      0.00      0.00      0.00      0.00
```

`sar -n TCP 1`  显示TCP连接情况，可用来描述系统负载

```
[root@centos7 dev]# sar -n TCP,ETCP 1
Linux 4.18.0-147.5.1.el8_1.x86_64 (iZ8vb54310gt89j8qct198Z)     12/19/2020      _x86_64_        (1 CPU)

08:15:48 PM  active/s passive/s    iseg/s    oseg/s
08:15:49 PM      0.00      0.00      1.00      1.00

08:15:48 PM  atmptf/s  estres/s retrans/s isegerr/s   orsts/s
08:15:49 PM      0.00      0.00      0.00      0.00      0.00
```

- active/s：主动连接数，本地每秒创建的TCP连接数
- passive/s：被动连接数，远程每秒创建的TCP连接数
- retrans/s：每秒TCP重传次数

### df命令

`df -h` 查看磁盘使用情况

`df -i` 查看inode使用情况


### whoami
查看当前用户
```
[alvin@VM_0_16_centos ~]$ whoami
alvin
```
### who
目前都有谁登录到系统里？
```
[alvin@VM_0_16_centos ~]$ who
alvin    pts/0        2018-12-09 07:25 (116.199.***.***)
root     pts/1        2018-12-09 11:05 (116.199.***.***)
alvin    pts/2        2018-12-09 11:05 (116.199.***.***)
harry    pts/3        2018-12-09 11:06 (116.199.***.***)
kate     pts/4        2018-12-09 11:08 (116.199.***.***)
alvin    pts/5        2018-12-09 11:53 (116.199.***.***)
```

### users
```
[alvin@VM_0_16_centos ~]$ users
alvin alvin alvin harry kate root
```
### w
知道了谁登录到系统里，我们就可以进一步调查他们在做什么。w 命令用于显示已经登录系统的用户的名称，以及他们正在做的事。该命令所使用的信息来源于/var/run/utmp文件。
```
[alvin@VM_0_16_centos ~]$ w
 16:25:54 up 29 days,  6:05,  6 users,  load average: 0.00, 0.01, 0.05
USER     TTY      FROM             LOGIN@   IDLE   JCPU   PCPU WHAT
alvin    pts/0    116.199.***.**   07:25    2.00s  0.11s  0.00s w
root     pts/1    116.199.***.**   11:05    5:20m  0.02s  0.02s -bash
alvin    pts/2    116.199.***.**   11:05    5:20m  0.04s  0.05s sshd: alvin [priv]
harry    pts/3    116.199.***.**   11:06    4:33m 18.08s 18.06s watch date
kate     pts/4    116.199.***.**   11:08    4:33m 10.51s 10.48s top
alvin    pts/5    116.199.***.**   11:53    4:32m  0.02s  0.02s -bash
```
### last
last命令可用于显示特定用户登录系统的历史记录。如果没有指定任何参数，则显示所有用户的历史信息。在默认情况下，这些信息（所显示的信息）将来源于/var/log/wtmp文件。该命令的输出结果包含以下几列信息：
- 用户名称
- tty设备号
- 历史登录时间日期
- 登出时间日期
- 总工作时间
```
alvin    pts/5        116.199.***.**   Sun Dec  9 11:53   still logged in
kate     pts/4        116.199.***.**   Sun Dec  9 11:08   still logged in
harry    pts/3        116.199.***.**   Sun Dec  9 11:06   still logged in
alvin    pts/2        116.199.***.**   Sun Dec  9 11:05   still logged in
root     pts/1        116.199.***.**   Sun Dec  9 11:05   still logged in
alvin    pts/0        116.199.***.**   Sun Dec  9 07:25   still logged in
alvin    pts/0        116.199.***.**   Sat Dec  8 20:42 - 23:10  (02:28)
alvin    pts/0        119.33.***.**    Mon Dec  3 20:50 - 23:51 (1+03:01)
alvin    pts/0        119.33.***.**    Thu Nov 29 20:20 - 22:45  (02:24)
alvin    pts/0        223.104.***.**   Thu Nov 29 06:46 - 07:00  (00:14)
alvin    pts/0        223.104.***.**   Wed Nov 28 20:45 - 22:27  (01:42)
alvin    pts/1        14.25.***.***    Sun Nov 25 19:50 - 21:09  (01:18)
alvin    pts/0        119.33.***.**    Sun Nov 25 16:32 - 21:40  (05:07)
```
如果我们只想看某个人的历史记录，则在last后跟上对应的用户名即可：
```
[alvin@VM_0_16_centos ~]$ last alvin
alvin    pts/5        116.199.***.**   Sun Dec  9 11:53   still logged in
alvin    pts/2        116.199.***.**   Sun Dec  9 11:05   still logged in
alvin    pts/0        116.199.***.**   Sun Dec  9 07:25   still logged in
alvin    pts/0        116.199.***.**   Sat Dec  8 20:42 - 23:10  (02:28)
alvin    pts/0        119.33.***.**    Mon Dec  3 20:50 - 23:51 (1+03:01)
alvin    pts/0        119.33.***.**    Thu Nov 29 20:20 - 22:45  (02:24)
alvin    pts/0        223.104.***.**   Thu Nov 29 06:46 - 07:00  (00:14)
alvin    pts/0        223.104.***.**   Wed Nov 28 20:45 - 22:27  (01:42)
```
### pkill
踢除使坏人员
```
pkill -u alvin
```
```
[alvin@VM_0_16_centos ~]$ sudo pkill -kill -t pts/3
#harry用户已经被踢除了
[alvin@VM_0_16_centos ~]$ w
 17:04:37 up 29 days,  6:44,  5 users,  load average: 0.00, 0.01, 0.05
USER     TTY      FROM             LOGIN@   IDLE   JCPU   PCPU WHAT
alvin    pts/0    116.199.102.65   07:25    5.00s  0.12s  0.00s w
root     pts/1    116.199.102.65   11:05    5:59m  0.02s  0.02s -bash
alvin    pts/2    116.199.102.65   11:05    5:59m  0.04s  0.05s sshd: alvin [priv]
kate     pts/4    116.199.102.65   11:08    5:12m 11.94s 11.91s top
alvin    pts/5    116.199.102.65   11:53    5:10m  0.02s  0.02s -bash
```

## rpm 和 yum软件安装
### rpm包
一种用于互联网下载包的打包及安装工具，它包含在某些Linux分发版中。它生成具有.RPM扩展名的文件。RPM是RedHat Package Manager（RedHat软件包管理工具）的缩写，类似windows的setup.exe，这一文件格式名称虽然打上了RedHat的标志，但理念是通用的。

Linux的分发版本都有采用（suse,redhat, centos 等等），可以算是公认的行业标准了

### rpm 常用的指令
![Image [22]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155223.png)
### 卸载rpm包
rpm -e RPM包的名称
### 安装rpm包
rpm  -ivh  RPM包全路径名称
**参数说明**
    i=install 安装
    v=verbose 提示
    h=hash  进度条
### yum的使用
![Image [23]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155236.png)

## 下载文件
### WGET
`wget 【-P 目录】 网址`

## 网络和进程
### 查看所有网络接口的属性
#### ifconfig
```
[root@pdai.tech ~]# ifconfig
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 172.31.165.194  netmask 255.255.240.0  broadcast 172.31.175.255
        ether 00:16:3e:08:c1:ea  txqueuelen 1000  (Ethernet)
        RX packets 21213152  bytes 2812084823 (2.6 GiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 25264438  bytes 46566724676 (43.3 GiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536
        inet 127.0.0.1  netmask 255.0.0.0
        loop  txqueuelen 1000  (Local Loopback)
        RX packets 502  bytes 86350 (84.3 KiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 502  bytes 86350 (84.3 KiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```
#### 重启网络
设置了linux网络，需要重启网络，可以用命令：
```
service network restart 
```

### 防火墙
#### firewalld的基本使用
启动： `systemctl start firewalld`

查看状态： `systemctl status firewalld `

停止：`systemctl disable firewalld`

禁用： `systemctl stop firewalld`

#### 配置firewalld-cmd
查看版本： `firewall-cmd --version`
查看帮助： `firewall-cmd --help`
显示状态： `firewall-cmd --state`
查看所有打开的端口： `firewall-cmd--zone=public --list-ports`
更新防火墙规则： `firewall-cmd --reload`
查看区域信息:  `firewall-cmd--get-active-zones`
查看指定接口所属区域：` firewall-cmd--get-zone-of-interface=eth0`
拒绝所有包：`firewall-cmd --panic-on`
取消拒绝状态： `firewall-cmd --panic-off`
查看是否拒绝： `firewall-cmd --query-panic`

#### 开启一个端口
添加: `firewall-cmd --zone=public --add-port=80/tcp --permanent`  （--permanent永久生效，没有此参数重启后失效）

重新载入: `firewall-cmd --reload`

查看: `firewall-cmd --zone=public --query-port=80/tcp`

删除: `firewall-cmd --zone=public --remove-port=80/tcp --permanent`

#### 查看firewall是否运行
下面两个命令都可以

`systemctl status firewalld.service`

`firewall-cmd --state`

#### 查看当前开了哪些端口
其实一个服务对应一个端口，每个服务对应`/usr/lib/firewalld/services`下面一个xml文件。

`firewall-cmd --list-services`

### Linux命令发送Http GET/POST请求
#### Get请求
##### curl命令模拟Get请求：
1. 使用curl命令：
    ```
    curl "http://www.baidu.com"  如果这里的URL指向的是一个文件或者一幅图都可以直接下载到本地
    curl -i "http://www.baidu.com"  显示全部信息
    curl -I "http://www.baidu.com"  只显示头部信息
    curl -v "http://www.baidu.com"   显示get请求全过程解析
    ```
2. 使用wget命令：
    ```
    wget  "http://www.baidu.com"
    ```
##### curl命令模拟Get请求携带参数（linux）：
```
curl -v http://127.0.0.1:80/xcloud/test?version=1&client_version=1.1.0&seq=1001&host=aaa.com
```
上述命令在linux系统，get请求携带的参数只到version=1，”&”符号在linux系统中为后台运行的操作符，此处需要使用反斜杠”\”转义，即：
```
curl -v http://127.0.0.1:80/xcloud/test?version=1\&client_version=1.1.0\&seq=1001\&host=aaa.com
```
或者
```
curl -v "http://127.0.0.1:80/xcloud/test?version=1&client_version=1.1.0&seq=1001&host=aaa.com"
```
#### Post请求
1. 使用curl命令，通过-d参数，把访问参数放在里面，如果没有参数，则不需要-d
```
curl -d "username=user1&password=123" "www.test.com/login"
```
2. 使用wget命令
```
wget –post-data 'username=user1&password=123' http://www.baidu.com
```
3. 发送格式化json请求
```
curl -i -k  -H "Content-type: application/json" -X POST -d '{"version":"6.6.0", "from":"mu", "product_version":"1.1.1.0"}' https://10.10.10.10:80/test
```
#### curl和wget区别
curl模拟的访问请求一般直接在控制台显示，而wget则把结果保存为一个文件。如果结果内容比较少，需要直接看到结果可以考虑使用curl进行模拟请求，如果返回结果比较多，则可考虑wget进行模拟请求。



### 查看 Linux 系统服务的 5 大方法
#### Centos/RHEL 7.X 的 systemd 系统服务查看
CentOS 7.x开始，CentOS开始使用 systemd 服务来代替 daemon ，原来管理系统启动和管理系统服务的相关命令全部由 systemctl 命令来代替。
```
systemctl list-unit-files
```
命令的输出结果如下：

![Image [24]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155525.png)

查看所有运行着的 systemd 服务可以运行以下命令：

```
systemctl | more
```
命令的输出结果如下：

![Image [25]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155539.png)

除此之外，你还可以使用以下命令：

```
systemctl list-units --type service
```
命令的输出结果如下：

![Image [26]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155614.png)

如果你想要在结果里搜索某个特定的服务，可以使用管道及 grep 命令。

```
systemctl | grep "apache2"
```
命令的输出结果如下：

![Image [27]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155624.png)

#### 使用 netstat 命令查看系统服务
Netstat 命令是用来检查活动的网络连接、接口统计分析，以及路由表状态。这个命令在所有的 Linux 发行版都可用，我们接下来就用它来查看系统服务。

查看服务及它们所监听的端口：
```
netstat -pnltu
```
命令的输出结果如下：

![Image [28]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155635.png)

#### 通过系统服务配置文件查看系统服务
服务的配置文件是 /etc/services 是一个 ASCII 文件，它包含了一系列的用户程序可能用到的服务。在这个文件里，包括了服务名称，端口号，所使用的协议，以及一些别名。

对于这个文件，我们可以使用任意的文本工具查看，比如 vim ：
```
vim /etc/services
```
命令的输出结果如下：

![Image [29]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155649.png)

#### 查看 systemd 服务状态
在一些新版的 Linux 系统，已经有些用 systemd 来取代 init 进程了。在这种系统里，如何去查看系统服务呢？我们可以使用以下语法：
```
systemctl status service_name
```
比如说，查看你系统上的 OpenSSH 是否在运行，可以运行：
```
systemctl status sshd
```
命令的输出结果如下：

![Image [30]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155706.png)

或者，你也可以使用以下命令格式去查看某个服务是否正在运行：

```
systemctl is-active service_name
```
如果使用这条命令的话，实现上面那个例子对应的命令为：
```
systemctl is-active sshd
```
命令的输出结果如下：

![Image [31]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155717.png)

同时，你也可以查看一个服务是否已经被使能了，可以使用以下命令：

```
systemctl is-enabled service_name
```
比如，检查 OpenSSH 服务是否已经使能，可能输入以下命令：
```
systemctl is-enabled sshd
```
命令的输出结果如下：

![Image [32]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155731.png)

#### 早期版本的服务状态查看
其实也不能说早期，现在依然还有很多这样的系统，上面跑着 SysV init 进程。对于这种系统，查看服务状态的命令为：
```
service service_name status
```
还是查看 OpenSSH 状态的例子，对应的命令为：
```
service sshd status
```
命令的输出结果如下：

![Image [33]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155745.png)

你也可以使用以下命令来查看所有的服务状态：

```
chkconfig --list
```
命令的输出结果如下：
![Image [34]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/linux-demo/20210411155754.png)

