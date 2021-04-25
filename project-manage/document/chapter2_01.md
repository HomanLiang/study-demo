[toc]



# Git

## 1.概要
### 1.1.什么是Git
Git是目前世界上最先进的分布式版本控制系统。

Git是免费、开源的

最初Git是为辅助 Linux 内核开发的，来替代 BitKeeper

作者：Linux和Git之父李纳斯·托沃兹（Linus Benedic Torvalds）1969、芬兰

优点：

- 适合分布式开发，强调个体。
- 公共服务器压力和数据量都不会太大。
- 速度快、灵活。
- 任意两个开发者之间可以很容易的解决冲突。
- 离线工作。

缺点：

- 模式上比SVN更加复杂。
- 不符合常规思维。
- 代码保密性差，一旦开发者把整个库克隆下来就可以完全公开所有代码和版本信息。 

### 1.2.相关网址
[官网](https://git-scm.com/)
[源码](https://github.com/git/git/)

### 1.3.常用术语
- 仓库（Repository）

  受版本控制的所有文件修订历史的共享数据库

- 工作空间（Workspace) 

  本地硬盘或Unix 用户帐户上编辑的文件副本

- 工作树/区（Working tree）

  工作区中包含了仓库的工作文件。您可以修改的内容和提交更改作为新的提交到仓库。

- 暂存区（Staging area）

  暂存区是工作区用来提交更改（commit）前可以暂存工作区的变化。

  ![Image](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221011.png)

- 索引（Index）

  索引是暂存区的另一种术语。

- 签入（Checkin）

  将新版本复制回仓库

- 签出（Checkout）

  从仓库中将文件的最新修订版本复制到工作空间

- 提交（Commit）

  对各自文件的工作副本做了更改，并将这些更改提交到仓库

- 冲突（Conflict）

  多人对同一文件的工作副本进行更改，并将这些更改提交到仓库

- 合并（Merge）

  将某分支上的更改联接到此主干或同为主干的另一个分支

- 分支（Branch）

  从主线上分离开的副本，默认分支叫master

- 锁（Lock）

  获得修改文件的专有权限。

- 头（HEAD）

  头是一个象征性的参考，最常用以指向当前选择的分支。

- 修订（Revision）

  表示代码的一个版本状态。Git通过用SHA1 hash算法表示的ID来标识不同的版本。

- 标记（Tags）

  标记指的是某个分支某个特定时间点的状态。通过标记，可以很方便的切换到标记时的状态。

## 2.Git安装与配置
### 2.1.搭建Git工作环境
**下载Git**

打开 git官网，下载git对应操作系统的版本。

![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221057.png)

选择版本：

![Image [3]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221110.png)

这里我选择下载64-bit Git for Windows Setup

![Image [4]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221123.png)

**安装Git**

![Image [5]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221133.png)

选择安装配置信息

![Image [6]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221225.png)

一直Next默认就好了，如果需要设置就要仔细读一下安装界面上的选项。

**启动Git**

安装成功后在开始菜单中会有Git项，菜单下有3个程序：

![Image [7]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221247.png)

**Git Bash**：Unix与Linux风格的命令行，使用最多，推荐最多

![Image [8]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221317.png)

与DOS风格的命令有些区别，不习惯可以选择Git CMD

**Git CMD**：Windows风格的命令行

![Image [9]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221334.png)

**Git GUI**：图形界面的Git，不建议初学者使用，尽量先熟悉常用命令

![Image [10]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221413.png)

点击Create New Repository可以直接创建一个新的仓库。

### 2.2.Git配置 - git config
#### 2.2.1.查看配置 - git config -l
使用 `git config -l` 可以查看现在的git环境详细配置

![Image [11]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221436.png)

查看不同级别的配置文件：
```
#查看系统config
git config --system --list
　　
#查看当前用户（global）配置
git config --global  --list
 
#查看当前仓库配置信息
git config --local  --list
```
#### 2.2.2.Git配置文件分类
在Windows系统中，Git在$HOME目录中查找.gitconfig文件（一般位于C:\Documents and Settings$USER下）

**Git相关的配置文件有三个：**
 1. `/etc/gitconfig`：包含了适用于系统所有用户和所有项目的值。(Win：`C:\Program Files\Git\mingw64\etc\gitconfig`) --system 系统级

  ![Image [12]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221553.png)

2. `~/.gitconfig`：只适用于当前登录用户的配置。(`Win：C:\Users\Administrator\.gitconfig`)  --global 全局

  ![Image [13]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225832.png)

3. 位于git项目目录中的 `.git/config`：适用于特定git项目的配置。(Win：`C:\gitProject`) --local当前项目

  注意：对于同一配置项，三个配置文件的优先级是1<2<3

  这里可以直接编辑配置文件，通过命令设置后会响应到这里。

#### 2.2.2.设置用户名与邮箱（用户标识，必要）
当你安装Git后首先要做的事情是设置你的用户名称和e-mail地址。这是非常重要的，因为每次Git提交都会使用该信息。它被永远的嵌入到了你的提交中：
```
$ git config --global user.name "zhangguo"  #名称
$ git config --global user.email zhangguo@qq.com   #邮箱
```
只需要做一次这个设置，如果你传递了 `--global` 选项，因为Git将总是会使用该信息来处理你在系统中所做的一切操作。如果你希望在一个特定的项目中使用不同的名称或e-mail地址，你可以在该项目中运行该命令而不要 `--global` 选项。 总之 `--global` 为全局配置，不加为某个项目的特定配置。

![Image [14]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221746.png)

#### 2.2.3.添加或删除配置项
##### 2.2.3.1.添加配置项 
```
git config [--local|--global|--system]  section.key value
[--local|--global|--system]  #可选的，对应本地，全局，系统不同级别的设置
section.key #区域下的键
value #对应的值
```
`--local` 项目级

`--global` 当前用户级

`--system` 系统级 

例如我们要在student区域下添加一个名称为height值为198的配置项，执行结果如下：

![Image [15]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221857.png)

##### 2.2.3.2.删除配置项 
```
git config [--local|--global|--system] --unset section.key
```
 将系统级的height配置项移除

![Image [16]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425221916.png)

##### 2.2.3.3.更多配置项
```
git config --global color.ui true   #打开所有的默认终端着色
git config --global alias.ci commit   #别名 ci 是commit的别名
[alias]  
co = checkout  
ci = commit  
st = status  
pl = pull  
ps = push  
dt = difftool  
l = log --stat  
cp = cherry-pick  
ca = commit -a  
b = branch 

user.name  #用户名
user.email  #邮箱
core.editor  #文本编辑器  
merge.tool  #差异分析工具  
core.paper "less -N"  #配置显示方式  
color.diff true  #diff颜色配置  
alias.co checkout  #设置别名
git config user.name  #获得用户名
git config core.filemode false  #忽略修改权限的文件
```
所有config命令参数
```
语法: git config [<options>]        
        
文件位置        
    --global                  #use global config file 使用全局配置文件
    --system                  #use system config file 使用系统配置文件
    --local                   #use repository config file    使用存储库配置文件
    -f, --file <file>         #use given config file    使用给定的配置文件
    --blob <blob-id>          #read config from given blob object    从给定的对象中读取配置
        
动作        
    --get                     #get value: name [value-regex]    获得值：[值]名[正则表达式]
    --get-all                 #get all values: key [value-regex]    获得所有值：[值]名[正则表达式]
    --get-regexp          #get values for regexp: name-regex [value-regex]    得到的值根据正则
    --get-urlmatch            #get value specific for the URL: section[.var] URL    为URL获取特定的值
    --replace-all             #replace all matching variables: name value [value_regex]    替换所有匹配的变量：名称值[ value_regex ]
    --add                     #add a new variable: name value    添加一个新变量：name值
    --unset                   #remove a variable: name [value-regex]    删除一个变量名[值]：正则表达式
    --unset-all               #remove all matches: name [value-regex]    删除所有匹配的正则表达式：名称[值]
    --rename-section          #rename section: old-name new-name    重命名部分：旧名称 新名称
    --remove-section          #remove a section: name    删除部分：名称
    -l, --list                #list all    列出所有
    -e, --edit            #open an editor    打开一个编辑器
    --get-color               #find the color configured: slot [default]    找到配置的颜色：插槽[默认]
    --get-colorbool           #find the color setting: slot [stdout-is-tty]    发现颜色设置：槽[ stdout是TTY ]
        
类型        
    --bool                    #value is "true" or "false"    值是“真”或“假”。
    --int                     #value is decimal number    值是十进制数。
    --bool-or-int             #value is --bool or --int    值--布尔或int
    --path                    #value is a path (file or directory name)    值是路径（文件或目录名）
        
其它        
    -z, --null                #terminate values with NUL byte    终止值与null字节
    --name-only               #show variable names only    只显示变量名
    --includes                #respect include directives on lookup    尊重包括查找指令
    --show-origin             #show origin of config (file, standard input, blob, command line)    显示配置（文件、标准输入、数据块、命令行）的来源
```

## 3.Git理论基础
### 3.1.工作区域
Git本地有三个工作区域：工作目录（Working Directory）、暂存区(Stage/Index)、资源库(Repository或Git Directory)。如果在加上远程的git仓库(Remote Directory)就可以分为四个工作区域。文件在这四个区域之间的转换关系如下：

![Image [17]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425224007.png)

- Workspace：工作区，就是你平时存放项目代码的地方
- Index / Stage：暂存区，用于临时存放你的改动，事实上它只是一个文件，保存即将提交到文件列表信息
- Repository：仓库区（或本地仓库），就是安全存放数据的位置，这里面有你提交到所有版本的数据。其中HEAD指向最新放入仓库的版本
- Remote：远程仓库，托管代码的服务器，可以简单的认为是你项目组中的一台电脑用于远程数据交换

本地的三个区域确切的说应该是git仓库中HEAD指向的版本

![Image [18]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225308.png)

- Directory：使用Git管理的一个目录，也就是一个仓库，包含我们的工作空间和Git的管理空间。
- WorkSpace：需要通过Git进行版本控制的目录和文件，这些目录和文件组成了工作空间。
- .git：存放Git管理信息的目录，初始化仓库的时候自动创建。
- Index/Stage：暂存区，或者叫待提交更新区，在提交进入repo之前，我们可以把所有的更新放在暂存区。
- Local Repo：本地仓库，一个存放在本地的版本库；HEAD会只是当前的开发分支（branch）。
- Stash：隐藏，是一个工作状态保存栈，用于保存/恢复WorkSpace中的临时状态。

### 3.2.工作流程
git的工作流程一般是这样的：

１. 在工作目录中添加、修改文件；
２. 将需要进行版本管理的文件放入暂存区域；
３. 将暂存区域的文件提交到git仓库。

因此，git管理的文件有三种状态：已修改（modified）,已暂存（staged）,已提交(committed)

![Image [19]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225323.png)

### 3.3.图解教程
个人认为Git的原理相比别的版本控制器还是复杂一些的，有一份图解教程比较直观：

![Image [20]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225339.png)

## 4.Git操作
### 4.1.创建工作目录与常用指令
工作目录（WorkSpace)一般就是你希望Git帮助你管理的文件夹，可以是你项目的目录，也可以是一个空目录，建议不要有中文。

### 4.2.获得GIT仓库
创建本地仓库的方法有两种：一种是创建全新的仓库，另一种是克隆远程仓库。
#### 4.2.1.创建全新仓库
需要用GIT管理的项目的根目录执行：
```
# 在当前目录新建一个Git代码库
$ git init
```
执行：

![Image [22]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225424.png)

结果：

![Image [23]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225444.png)

执行后可以看到，仅仅在项目目录多出了一个.git目录，关于版本等的所有信息都在这个目录里面。

当然如果使用如下命令，可以把创建目录与仓库一起完成：
```
# 新建一个目录，将其初始化为Git代码库
$ git init [project-name]
```
执行命令与运行结果：

![Image [24]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225516.png)

#### 4.2.2.克隆远程仓库
另一种方式是克隆远程目录，由于是将远程服务器上的仓库完全镜像一份至本地，而不是取某一个特定版本，所以用clone而不是checkout，语法格式如下：
```
# 克隆一个项目和它的整个代码历史(版本信息)
$ git clone [url]
```
执行：
比如我们要从克隆的远程仓库托管在github上，地址为：https://github.com/zhangguo5/SuperPlus.git，这是一个公开的项目

![Image [25]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225529.png)

![Image [26]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425225543.png)

结果：

![Image [27]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230230.png)

### 4.3.GIT文件操作
版本控制就是对文件的版本控制，要对文件进行修改、提交等操作，首先要知道文件当前在什么状态，不然可能会提交了现在还不想提交的文件，或者要提交的文件没提交上。GIT不关心文件两个版本之间的具体差别，而是关心文件的整体是否有改变，若文件被改变，在添加提交时就生成文件新版本的快照，而判断文件整体是否改变的方法就是用SHA-1算法计算文件的校验和。

#### 4.3.1.文件4种状态
![Image [28]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230246.png)

- Untracked: 未跟踪, 此文件在文件夹中, 但并没有加入到git库, 不参与版本控制. 通过git add 状态变为Staged.
- Unmodify: 文件已经入库, 未修改, 即版本库中的文件快照内容与文件夹中完全一致. 这种类型的文件有两种去处, 如果它被修改, 而变为Modified. 如果使用git rm移出版本库, 则成为Untracked文件
- Modified: 文件已修改, 仅仅是修改, 并没有进行其他的操作. 这个文件也有两个去处, 通过git add可进入暂存staged状态, 使用git checkout 则丢弃修改过, 返回到unmodify状态, 这个git checkout即从库中取出文件, 覆盖当前修改
- Staged: 暂存状态. 执行git commit则将修改同步到库中, 这时库中的文件和本地文件又变为一致, 文件为Unmodify状态. 执行git reset HEAD filename取消暂存, 文件状态为Modified

![Image [29]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230302.png)

#### 4.3.2.查看文件状态
上面说文件有4种状态，通过如下命令可以查看到文件的状态：
```
#查看指定文件状态
git status [filename]

#查看所有文件状态
git status
```
命令：

![Image [30]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230328.png)

结果：

foo.htm文件的状态为untracked（未跟踪），提示通过git add可以暂存

GIT在这一点做得很好，在输出每个文件状态的同时还说明了怎么操作，像上图就有怎么暂存、怎么跟踪文件、怎么取消暂存的说明。

#### 4.3.3.添加文件与目录
![Image [31]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230345.png)

工作区（Working Directory）就是你在电脑里能看到的目录。

版本库（Repository）工作区有一个隐藏目录.git，这个不算工作区，而是Git的版本库。

Git的版本库里存了很多东西，其中最重要的就是称为stage（或者叫index）的暂存区，还有Git为我们自动创建的第一个分支master，以及指向master的一个指针叫HEAD。

将untracked状态的文件添加到暂存区，语法格式如下：
```
# 添加指定文件到暂存区
$ git add [file1] [file2] ...

# 添加指定目录到暂存区，包括子目录
$ git add [dir]

# 添加当前目录的所有文件到暂存区
$ git add .
```
执行：

![Image [32]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230356.png)

#### 4.3.4.移除文件与目录（撤销add）
![Image [33]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230412.png)

当执行如下命令时，会直接从暂存区删除文件，工作区则不做出改变

```
#直接从暂存区删除文件，工作区则不做出改变
git rm --cached <file>
```
执行命令

![Image [34]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230433.png)

通过重写目录树移除add文件：

```
#如果已经用add 命令把文件加入stage了，就先需要从stage中撤销
git reset HEAD <file>...
```
当执行 “git reset HEAD” 命令时，暂存区的目录树会被重写，被 master 分支指向的目录树所替换，但是工作区不受影响。

示例：把f1.txt文件从暂存区撤回工作区

![Image [35]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230458.png)

移除所有未跟踪文件
```
#移除所有未跟踪文件
#一般会加上参数-df，-d表示包含目录，-f表示强制清除。
git clean [options] 
```
示例：

![Image [36]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230517.png)

移除前：

![Image [37]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230530.png)

执行移除：

![Image [38]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230541.png)

移除后：

![Image [39]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230554.png)

```
#只从stage中删除，保留物理文件
git rm --cached readme.txt 

#不但从stage中删除，同时删除物理文件
git rm readme.txt 

#把a.txt改名为b.txt
git mv a.txt b.txt
```
当执行提交操作（git commit）时，暂存区的目录树写到版本库（对象库）中，master 分支会做相应的更新。即 master 指向的目录树就是提交时暂存区的目录树。

当执行 `git reset HEAD` 命令时，暂存区的目录树会被重写，被 master 分支指向的目录树所替换，但是工作区不受影响。

当执行 `git rm –cached <file>` 命令时，会直接从暂存区删除文件，工作区则不做出改变。

当执行 `git checkout .` 或者 `git checkout — <file>` 命令时，会用暂存区全部或指定的文件替换工作区的文件。这个操作很危险，会清除工作区中未添加到暂存区的改动。

当执行 `git checkout HEAD .` 或者 `git checkout HEAD <file>` 命令时，会用 HEAD 指向的 master 分支中的全部或者部分文件替换暂存区和以及工作区中的文件。这个命令也是极具危险性的，因为不但会清除工作区中未提交的改动，也会清除暂存区中未提交的改 动。

#### 4.3.5.查看文件修改后的差异
git diff用于显示WorkSpace中的文件和暂存区文件的差异

用 `git status` 只能查看对哪些文件做了改动，如果要看改动了什么，可以用：
```
#查看文件修改后的差异
git diff [files]
```
命令：

![Image [40]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230646.png)

---a表示修改之前的文件，+++b表示修改后的文件

```
#比较暂存区的文件与之前已经提交过的文件
git diff --cached
```
也可以把WorkSpace中的状态和repo中的状态进行diff，命令如下:
```
#比较repo与工作空间中的文件差异
git diff HEAD~n
```
![Image [41]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230658.png)

#### 4.3.6.签出
如果仓库中已经存在文件f4.txt，在工作区中对f4修改了，如果想撤销可以使用checkout，签出覆盖

检出命令git checkout是git最常用的命令之一，同时也是一个很危险的命令，因为这条命令会重写工作区

语法：
```
#用法一
git checkout [-q] [<commit>] [--] <paths>...
#用法二
git checkout [<branch>]
#用法三
git checkout [-m] [[-b]--orphan] <new_branch>] [<start_point>]
```
`<commit>` 是可选项，如果省略则相当于从暂存区（index）进行检出

![Image [42]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230719.png)

```
$ git checkout branch
#检出branch分支。要完成图中的三个步骤，更新HEAD以指向branch分支，以及用branch  指向的树更新暂存区和工作区。

$ git checkout
#汇总显示工作区、暂存区与HEAD的差异。

$ git checkout HEAD
#同上

$ git checkout -- filename
#用暂存区中filename文件来覆盖工作区中的filename文件。相当于取消自上次执行git add filename以来（如果执行过）的本地修改。

$ git checkout branch -- filename
#维持HEAD的指向不变。用branch所指向的提交中filename替换暂存区和工作区中相   应的文件。注意会将暂存区和工作区中的filename文件直接覆盖。

$ git checkout -- . 或写作 git checkout .
#注意git checkout 命令后的参数为一个点（“.”）。这条命令最危险！会取消所有本地的  #修改（相对于暂存区）。相当于用暂存区的所有文件直接覆盖本地文件，不给用户任何确认的机会！

$ git checkout commit_id -- file_name
#如果不加commit_id，那么git checkout -- file_name 表示恢复文件到本地版本库中最新的状态。
```
示例： 

![Image [43]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230736.png)

#### 4.3.7.忽略文件
有些时候我们不想把某些文件纳入版本控制中，比如数据库文件，临时文件，设计文件等

在主目录下建立".gitignore"文件，此文件有如下规则：
1. 忽略文件中的空行或以井号（#）开始的行将会被忽略。
1. 可以使用Linux通配符。例如：星号（*）代表任意多个字符，问号（？）代表一个字符，方括号（[abc]）代表可选字符范围，大括号（{string1,string2,...}）代表可选的字符串等。
1. 如果名称的最前面有一个感叹号（!），表示例外规则，将不被忽略。
1. 如果名称的最前面是一个路径分隔符（/），表示要忽略的文件在此目录下，而子目录中的文件不忽略。
1. 如果名称的最后面是一个路径分隔符（/），表示要忽略的是此目录下该名称的子目录，而非文件（默认文件或目录都忽略）。

如：
```
#为注释
*.txt #忽略所有 .txt结尾的文件
!lib.txt #但lib.txt除外
/temp #仅忽略项目根目录下的TODO文件,不包括其它目录temp
build/ #忽略build/目录下的所有文件
doc/*.txt #会忽略 doc/notes.txt 但不包括 doc/server/arch.txt
```
示例：
创建一个.gitignore文件忽视所有的日志文件

![Image [44]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230747.png)

查看状态：

![Image [45]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230758.png)

从上图中可以看出2个日志文件并没有添加到暂存区，直接被忽视了。

通用的java忽视文件：
```
# Compiled class file
*.class

# Log file
*.log

# BlueJ files
*.ctxt

# Mobile Tools for Java (J2ME)
.mtj.tmp/

# Package Files #
*.jar
*.war
*.ear
*.zip
*.tar.gz
*.rar

# virtual machine crash logs, see http://www.java.com/en/download/help/error_hotspot.xml
hs_err_pid*
```
idea忽视文件：
```
.idea/
*.iml
out/
gen/
idea-gitignore.jar
resources/templates.list
resources/gitignore/*
build/
build.properties
junit*.properties
IgnoreLexer.java~
.gradle

/verification
```
#### 4.3.8.提交
通过add只是将文件或目录添加到了index暂存区，使用commit可以实现将暂存区的文件提交到本地仓库。
```
# 提交暂存区到仓库区
$ git commit -m [message]

# 提交暂存区的指定文件到仓库区
$ git commit [file1] [file2] ... -m [message]

# 提交工作区自上次commit之后的变化，直接到仓库区，跳过了add,对新文件无效
$ git commit -a

# 提交时显示所有diff信息
$ git commit -v

# 使用一次新的commit，替代上一次提交
# 如果代码没有任何新变化，则用来改写上一次commit的提交信息
$ git commit --amend -m [message]

# 重做上一次commit，并包括指定文件的新变化
$ git commit --amend [file1] [file2] ...
```
示例：
提交前的状态

![Image [46]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230812.png)

提交：

![Image [47]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230823.png)

提交后的状态：

![Image [48]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230830.png)

从上图中可以看出暂存区中没有了bar.htm

##### 4.3.8.1.修订提交
如果我们提交过后发现有个文件改错了，或者只是想修改提交说明，这时可以对相应文件做出修改，将修改过的文件通过"git add"添加到暂存区，然后执行以下命令：
```
#修订提交
git commit --amend
```
##### 4.3.8.2.撤销提交（commit）
原理就是放弃工作区和index的改动，同时HEAD指针指向前一个commit对象
```
#撤销上一次的提交
git reset --hard HEAD~1
```
要通过git log查看提交日志，也可直接指定提交编号或序号

示例：

![Image [49]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230850.png)

撤销提交

```
git revert <commit-id>
```
这条命令会把指定的提交的所有修改回滚，并同时生成一个新的提交。

#### 4.3.9.日志与历史
查看提交日志可以使用git log指令，语法格式如下：
```
#查看提交日志
git log [<options>] [<revision range>] [[\--] <path>…?]
```
示例：

![Image [50]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230929.png)

`git log --graph` 以图形化的方式显示提交历史的关系，这就可以方便地查看提交历史的分支信息，当然是控制台用字符画出来的图形。

`git log -1` 则表示显示1行。

使用history可以查看您在bash下输入过的指令：

![Image [51]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230949.png)

几乎所有输入过的都被记录下来的，不愧是做版本控制的。

##### 4.3.9.1.查看所有分支日志
"git reflog"中会记录这个仓库中所有的分支的所有更新记录，包括已经撤销的更新。

![Image [52]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425230956.png)

#### 4.3.10.查看文件列表
使用git ls-files指令可以查看指定状态的文件列表，格式如下：
```
#查看指定状态的文件
git ls-files [-z] [-t] [-v] (--[cached|deleted|others|ignored|stage|unmerged|killed|modified])* (-[c|d|o|i|s|u|k|m])*
```
示例：

![Image [53]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231005.png)

#### 4.3.11.撤销更新
##### 4.3.11.1.撤销暂存区更新
使用 `git add` 把更新提交到了暂存区。这时"git status"的输出中提示我们可以通过 `git reset HEAD <file>...` 把暂存区的更新移出到WorkSpace中

示例：f6已经提交，工作区修改，暂存区修改，撤销

![Image [54]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231018.png)

##### 4.3.11.2.撤销本地仓库更新
使用git log查看提交日志

![Image [55]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231039.png)

撤销提交有两种方式：使用HEAD指针和使用commit id

在Git中，有一个HEAD指针指向当前分支中最新的提交。当前版本，我们使用 `HEAD^` ，那么再钱一个版本可以使用 `HEAD^^`，如果想回退到更早的提交，可以使用 `HEAD~n`。（也就是，`HEAD^=HEAD~1`，`HEAD^^=HEAD~2`）
```
git reset --hard HEAD^
git reset --hard HEAD~1
git reset --59cf9334cf957535cb328f22a1579b84db0911e5
```

示例：回退到添加f6

回退前：

![Image [56]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231113.png)

回退后：

![Image [57]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231130.png)

现在又想恢复被撤销的提交可用"git reflog"查看仓库中所有的分支的所有更新记录，包括已经撤销的更新，撤销方法与前面一样。

```
git reset --hard HEAD@{7}
git reset --hard e0e79d7
```
`--hard`：撤销并删除相应的更新

`--soft`：撤销相应的更新，把这些更新的内容放到Stage中

#### 4.3.12.删除文件
##### 4.3.12.1.删除未跟踪文件
如果文件还是未跟踪状态，直接删除文件就可了，bash中使用rm可以删除文件，示例如下：

![Image [58]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231150.png)

##### 4.3.12.2.删除已提交文件
![Image [59]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231157.png)

`-f` 强制删除，物理删除了，同时删除工作区和暂存区中的文件

撤销删除：
```
#to discard changes in working directory
git checkout -- <file>...
```
![Image [60]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231214.png)

##### 4.3.12.3.删除暂存区的文件，不删除工作区的文件
![Image [61]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231227.png)

使用 `git reset HEAD <file>...`同样可以实现上面的功能

#### 4.3.13.文件操作小结
![Image [62]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231341.png)

Git很强大，很灵活，这是毋庸置疑的。但也正因为它的强大造成了它的复杂，因此会有很多奇奇怪怪的问题出现，多用就好了。

### 4.4.GIT分支
分支在GIT中相对较难

分支就是科幻电影里面的平行宇宙，当你正在电脑前努力学习Git的时候，另一个你正在另一个平行宇宙里努力学习SVN。

如果两个平行宇宙互不干扰，那对现在的你也没啥影响。不过，在某个时间点，两个平行宇宙合并了，结果，你既学会了Git又学会了SVN！

![Image [63]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231351.png)

分支在实际中有什么用呢？假设你准备开发一个新功能，但是需要两周才能完成，第一周你写了50%的代码，如果立刻提交，由于代码还没写完，不完整的代码库会导致别人不能干活了。如果等代码全部写完再一次提交，又存在丢失每天进度的巨大风险。

现在有了分支，就不用怕了。你创建了一个属于你自己的分支，别人看不到，还继续在原来的分支上正常工作，而你在自己的分支上干活，想提交就提交，直到开发完毕后，再一次性合并到原来的分支上，这样，既安全，又不影响别人工作。

Git分支的速度非常快。

截止到目前，只有一条时间线，在Git里，这个分支叫主分支，即master分支。HEAD严格来说不是指向提交，而是指向master，master才是指向提交的，所以，HEAD指向的就是当前分支。

![Image [64]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231407.png)

![Image [65]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231417.png)

![Image [66]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231512.png)

git分支中常用指令：

```
# 列出所有本地分支
$ git branch

# 列出所有远程分支
$ git branch -r

# 列出所有本地分支和远程分支
$ git branch -a

# 新建一个分支，但依然停留在当前分支
$ git branch [branch-name]

# 新建一个分支，并切换到该分支
$ git checkout -b [branch]

# 新建一个分支，指向指定commit
$ git branch [branch] [commit]

# 新建一个分支，与指定的远程分支建立追踪关系
$ git branch --track [branch] [remote-branch]

# 切换到指定分支，并更新工作区
$ git checkout [branch-name]

# 切换到上一个分支
$ git checkout -

# 建立追踪关系，在现有分支与指定的远程分支之间
$ git branch --set-upstream [branch] [remote-branch]

# 合并指定分支到当前分支
$ git merge [branch]

# 选择一个commit，合并进当前分支
$ git cherry-pick [commit]

# 删除分支
$ git branch -d [branch-name]

# 删除远程分支
$ git push origin --delete [branch-name]
$ git branch -dr [remote/branch]
```
#### 4.4.1.新建分支与切换分支
每次提交，Git都把它们串成一条时间线，这条时间线就是一个分支。截止到目前，只有一条时间线，在Git里，这个分支叫主分支，即master分支。HEAD严格来说不是指向提交，而是指向master，master才是指向提交的，所以，HEAD指向的就是当前分支。

一开始的时候，master分支是一条线，Git用master指向最新的提交，再用HEAD指向master，就能确定当前分支，以及当前分支的提交点：

![Image [67]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231521.png)

每次提交，master分支都会向前移动一步，这样，随着你不断提交，master分支的线也越来越长：

![63651-20180920210256578-751843766](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231528.gif)

默认分支是这样的，master是主分支

![Image [68]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231534.png)

1. 新建一个分支，但依然停留在当前分支，使用：$ git branch [branch-name]

  ![Image [69]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231544.png)

  切换分支到dev1后的结果：

  ![Image [70]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231600.png)

  当我们创建新的分支，例如dev时，Git新建了一个指针叫dev，指向master相同的提交，再把HEAD指向dev，就表示当前分支在dev上：

  ![Image [71]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231633.png)

  你看，Git创建一个分支很快，因为除了增加一个dev指针，改改HEAD的指向，工作区的文件都没有任何变化！

  不过，从现在开始，对工作区的修改和提交就是针对dev分支了，比如新提交一次后，dev指针往前移动一步，而master指针不变：

  ![Image [72]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231702.png)

  假如我们在dev上的工作完成了，就可以把dev合并到master上。Git怎么合并呢？最简单的方法，就是直接把master指向dev的当前提交，就完成了合并：

  ![Image [73]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231708.png)

  所以Git合并分支也很快！就改改指针，工作区内容也不变！

  合并完分支后，甚至可以删除dev分支。删除dev分支就是把dev指针给删掉，删掉后，我们就剩下了一条master分支：

  ![Image [74]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231714.png)

  动画演示：

  ![63651-20180920210908347-486995158](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231719.gif)

2. 切换分支，`git branch <name>`，如果name为-则为上一个分支

  ![Image [75]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231743.png)

  切换为上一个分支

  ![Image [76]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231749.png)

3. 新建一个分支，并切换到该分支，`$ git checkout -b [branch]`

  ![Image [77]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231804.png)

  ![Image [78]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231832.png)

4. 新建一个分支，指向指定commit使用命令：`$ git branch [branch] [commit]` 

  ![Image [79]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231848.png)

  上面创建了dev3分支且指向了master中首次提交的位置，切换到dev3查看日志如下：

  ![Image [80]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231907.png)

  master上本来有两个提交记录的，此时的dev3指向的是第1次提交的位置

  ![Image [81]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231916.png)

5. 新建一个分支，与指定的远程分支建立追踪关系使用命令：`$ git branch --track [branch] [remote-branch]`

  ![Image [82]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231928.png)

#### 4.4.2.查看分支
1. 列出所有本地分支使用 `$ git branch`

  ![Image [83]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425231953.png)

2. 列表所有远程分支使用 `$ git branch -r`

  ![Image [84]](C:\Users\hmliang\Desktop\temp\Image [84].png)

3. 列出所有本地分支和远程分支使用 `$ git branch -a`

  ![Image [85]](C:\Users\hmliang\Desktop\temp\Image [85].png)

#### 4.4.3.分支合并
合并指定分支到当前分支使用指令 `$ git merge [branch]`

这里的合并分支就是对分支的指针操作，我们先创建一个分支再合并到主分支：

![Image [86]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232032.png)

这里的file11.txt主分支与dev6的内容现在是不同的，因为在dev6中已被修改过，我们可以使用指令查看：

![Image [87]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232045.png)

现在我们将dev6合并到主分支中去，从下图中可以看出dev6中有一次提交，而master并没有

![Image [88]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232129.png)

![Image [89]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232134.png)

合并后在master上查看file11.txt文件内容与dev6上的内容就一样了，合并后dev6中多出的提交在master也拥有了。

![Image [90]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232143.png)

#### 4.4.4.解决冲突
如果同一个文件在合并分支时都被修改了则会引起冲突，如下所示：

提交前两个分支的状态

![Image [91]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232155.png)

在dev6分支中同样修改file11.txt

![Image [92]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232204.png)

dev6与master分支中file11.txt文件都被修改且提交了，现在合并分支

![Image [93]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232211.png)

提示冲突，现在我们看看file11.txt在master分支中的状态

![Image [94]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232223.png)

Git用<<<<<<<，=======，>>>>>>>标记出不同分支的内容，其中<<<HEAD是指主分支修改的内容，>>>>>dev6 是指dev6上修改的内容解决的办法是我们可以修改冲突文件后重新提交，请注意当前的状态产master | MERGING：

![Image [95]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232234.png)

重新提交后冲突解决：

![Image [96]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232241.png)

手动解决完冲突后就可以把此文件添 加到索引(index)中去，用git commit命令来提交，就像平时修改了一个文件 一样。

用 `git log --graph` 命令可以看到分支合并图。

![Image [97]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232249.png)

##### 4.4.4.1.分支策略
master主分支应该非常稳定，用来发布新版本，一般情况下不允许在上面工作，工作一般情况下在新建的dev分支上工作，工作完后，比如上要发布，或者说dev分支代码稳定后可以合并到主分支master上来。

### 4.5.删除分支
删除本地分支可以使用命令：$ git branch -d [branch-name]，-D（大写）强制删除

![Image [98]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232303.png)

删除远程分支可以使用如下指令：
```
$ git push origin --delete [branch-name] 
$ git branch -dr [remote/branch]
```
-d表示删除分支。分支必须完全合并在其上游分支，或者在HEAD上没有设置上游

-r表示远程的意思remotes，如果-dr则表示删除远程分支

![Image [99]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232309.png)


## 5.常用操作
### 5.1.暂存
`git stash`暂存（存储在本地，并将项目本次操作还原）
`git stash pop` 使用上一次暂存，并将这个暂存删除，使用该命令后，如果有冲突，终端会显示，如果有冲突需要先解决冲突（这就避免了冲突提交服务器，将冲突留在本地，然后解决）
`git stash list` 查看所有的暂存
`git stash clear` 清空所有的暂存
`git stash drop [-q|--quiet] [<stash>]` 删除某一个暂存，在中括号里面放置需要删除的暂存ID
`git stash apply` 使用某个暂存，但是不会删除这个暂存
#### 5.1.1.暂存不小心清空，结果里面有需要的代码，也是有找回方法的
`git fsck --lost-found` 命令找出刚才删除的分支里面的提交对象。
然后使用 `git show` 命令查看是否正确，
如果正确使用`git merge`命令找回


### 5.2.删除本地和远程分支
1. 切换到要操作的项目文件夹

  命令行 : `$ cd <ProjectPath>`

  ![Image [100]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232318.png)

2. 查看项目的分支们(包括本地和远程)

  命令行 : `$ git branch -a`

  ![Image [101]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232352.png)

3. 删除本地分支 

  命令行 : `$ git branch -d <BranchName>`

  ![Image [102]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232358.png)

4. 删除远程分支

  命令行 : `$ git push origin --delete <BranchName>`

  ![Image [103]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232403.png)

5. 查看删除后分支们

  命令行 : `$ git branch -a`

  ![Image [104]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232412.png)
### 5.2.撤销commit
写完代码后，我们一般这样
```
git add . //添加所有文件
git commit -m "本功能全部完成"
```
执行完commit后，想撤回commit，怎么办？

这样凉拌：
```
git reset --soft HEAD^
```
这样就成功的撤销了你的commit

注意，仅仅是撤回commit操作，您写的代码仍然保留。

说一下个人理解：

HEAD^的意思是上一个版本，也可以写成HEAD~1

如果你进行了2次commit，想都撤回，可以使用HEAD~2

至于这几个参数：

- `--mixed`

  意思是：不删除工作空间改动代码，撤销commit，并且撤销git add . 操作

  这个为默认参数,git reset --mixed HEAD^ 和 git reset HEAD^ 效果是一样的。

- `--soft`

  不删除工作空间改动代码，撤销commit，不撤销git add . 

- `--hard`

  删除工作空间改动代码，撤销commit，撤销git add . 

  注意完成这个操作后，就恢复到了上一次的commit状态。

顺便说一下，如果commit注释写错了，只是想改一下注释，只需要：

`git commit --amend`

此时会进入默认vim编辑器，修改注释完毕后保存就好了。

## 6.git-flow
`git-flow` 是一个 git 扩展集，按 Vincent Driessen 的分支模型提供高层次的库操作

### 6.1.基础建议
- Git flow 提供了极出色的命令帮忙以及输出提示。请仔细阅读并观察发生了什么事情...
- macOS 程序 Sourcetree 是一个极出色的 git 界面客户端，已经提供了 git-flow 的支持。
- Git-flow 是一个基于归并的解决方案，它并没有提供重置(rebase)特性分支的能力。
### 6.2.安装
Linux
```
$ apt-get install git-flow
```
安装 git-flow, 你需要 wget 和 util-linux。
### 6.3.开始
#### 6.3.1.初始化
使用 git-flow，从初始化一个现有的 git 库内开始:
```
git flow init
```
你必须回答几个关于分支的命名约定的问题。建议使用默认值。
### 6.4.特性
- 为即将发布的版本开发新功能特性。
- 这通常只存在开发者的库中。

#### 6.4.1.增加新特性
新特性的开发是基于 'develop' 分支的。

通过下面的命令开始开发新特性：
```
git flow feature start MYFEATURE
```
这个操作创建了一个基于'develop'的特性分支，并切换到这个分支之下。

#### 6.4.2.完成新特性
完成开发新特性。这个动作执行下面的操作.
- 合并 MYFEATURE 分支到 'develop'
- 删除这个新特性分支
- 切换回 'develop' 分支
```
git flow feature finish MYFEATURE
```
#### 6.4.3.发布新特性
你是否合作开发一项新特性？发布新特性分支到远程服务器，所以，其它用户也可以使用这分支。
```
git flow feature publish MYFEATURE
```
#### 6.4.4.取得一个发布的新特性分支
取得其它用户发布的新特性分支，并签出远程的变更。
```
git flow feature pull origin MYFEATURE
```
你可以使用
```
git flow feature track MYFEATURE
```
跟踪在origin上的特性分支

### 6.5.作一个release版本
- 支持一个新的用于生产环境的发布版本。
- 允许修正小问题，并为发布版本准备元数据。
#### 6.5.1.开始准备release版本
- 开始准备release版本，使用 git flow release 命令.
- 它从 'develop' 分支开始创建一个 release 分支。
```
git flow release start RELEASE [BASE]
```
你可以选择提供一个 [BASE]参数，即提交记录的 sha-1 hash 值，来开启动 release 分支. 这个提交记录的 sha-1 hash 值必须是'develop' 分支下的。

创建 release 分支之后立即发布允许其它用户向这个 release 分支提交内容是个明智的做法。命令十分类似发布新特性：
```
git flow release publish RELEASE
```
(你可以通过` git flow release track RELEASE` 命令签出 release 版本的远程变更)

#### 6.5.2.完成 release 版本
完成 release 版本是一个大 git 分支操作。它执行下面几个动作：

- 归并 release 分支到 'master' 分支
- 用 release 分支名打 Tag
- 归并 release 分支到 'develop'
- 移除 release 分支
```
git flow release finish RELEASE
```

### 6.6.紧急修复
- 紧急修复来自这样的需求：生产环境的版本处于一个不预期状态，需要立即修正。
- 有可能是需要修正 master 分支上某个 TAG 标记的生产版本。

#### 6.6.1.开始 git flow 紧急修复
像其它 git flow 命令一样, 紧急修复分支开始自：
```
git flow hotfix start VERSION [BASENAME]
```
VERSION 参数标记着修正版本。你可以从 [BASENAME]开始，[BASENAME]为finish release时填写的版本号
#### 6.6.2.完成紧急修复
当完成紧急修复分支，代码归并回 develop 和 master 分支。相应地，master 分支打上修正版本的 TAG。
```
git flow hotfix finish VERSION
```

### 6.7.命令
![Image [105]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232603.png)


## 7.Git 工作流
### 7.1.基本的 Git 工作流
最基本的 Git 工作流是只有一个分支 - master 分支的模式。开发人员直接提交 master 分支并使用它来部署到预发布和生产环境。

![Image [106]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232643.png)

上图为基本的 Git 工作流，所有提交都直接添加到 master 分支。

通常不建议使用此工作流，除非你正在开发一个 side 项目并且希望快速开始。

由于只有一个分支，因此这里实际上没有任何流程。这样一来，你就可以轻松开始使用 Git。但是，使用此工作流时需要记住它的一些缺点：
1. 在代码上进行协作将导致多种冲突。
1. 生产环境出现 bug 的概率会大增。
1. 维护干净的代码将更加困难。

### 7.2.Git 功能分支工作流
当你有多个开发人员在同一个代码库上工作时，Git 功能分支工作流将成为必选项。

假设你有一个正在开发一项新功能的开发人员。另一个开发人员正在开发第二个功能。现在，如果两个开发人员都向同一个分支提交代码，这将使代码库陷入混乱，并产生大量冲突。

![Image [107]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232654.png)

上图为具有功能分支的 Git 工作流模型。

为避免这种情况，两个开发人员可以分别从 master 分支创建两个单独的分支，并分别开发其负责的功能。完成功能后，他们可以将各自的分支合并到 master 分支，然后进行部署，而不必等待对方的功能开发完成。

使用此工作流的优点是，Git 功能分支工作流使你可以在代码上进行协作，而不必担心代码冲突。

### 7.3.带有 Develop 分支的 Git 功能分支工作流
此工作流是开发团队中比较流行的工作流之一。它与 Git 功能分支工作流相似，但它的 develop 分支与 master 分支并行存在。

在此工作流中，master 分支始终代表生产环境的状态。每当团队想要部署代码到生产环境时，他们都会部署 master 分支。

Develop 分支代表针对下一版本的最新交付的代码。开发人员从 develop 分支创建新分支，并开发新功能。功能开发完毕后，将对其进行测试，与 develop 分支合并，在合并了其他功能分支的情况下使用 develop 分支的代码进行测试，然后与 master 分支合并。

![Image [108]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232704.png)

上图为具有 develop 分支的 Git 功能分支工作流模型。

此工作流的优点是，它使团队能够一致地合并所有新功能，在预发布阶段对其进行测试并部署到生产环境中。尽管这种工作流让代码维护变得更加容易，但是对于某些团队来说，这样做可能会感到有些疲倦，因为频繁的 Git 操作可能会让你感到乏味。

### 7.4.Gitflow 工作流
Gitflow 工作流与我们之前讨论的工作流非常相似，我们将它们与其他两个分支（ release 分支和 hot-fix 分支）结合使用。

#### 7.4.1.Hot-Fix 分支
Hot-fix 分支是唯一一个从 master 分支创建的分支，并且直接合并到 master 分支而不是 develop 分支。仅在必须快速修复生产环境问题时使用。该分支的一个优点是，它使你可以快速修复并部署生产环境的问题，而无需中断其他人的工作流，也不必等待下一个发布周期。

将修复合并到 master 分支并进行部署后，应将其合并到 develop 和当前的 release 分支中。这样做是为了确保任何从 develop 分支创建新功能分支的人都具有最新代码。

#### 7.4.2.Release 分支
在将所有准备发布的功能的代码成功合并到 develop 分支之后，就可以从 develop 分支创建 release 分支了。

Release 分支不包含新功能相关的代码。仅将与发布相关的代码添加到 release 分支。例如，与此版本相关的文档，错误修复和其他关联任务才能添加到此分支。

一旦将此分支与 master 分支合并并部署到生产环境后，它也将被合并回 develop 分支中，以便之后从 develop 分支创建新功能分支时，新的分支能够具有最新代码。

![Image [109]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232716.png)

上图为具有 hot-fix 和 release 分支的 Gitflow 工作流模型

此工作流由 Vincent Driessen 首次发布并广受欢迎，已被具有预定发布周期的组织广泛使用。

由于 git-flow 是对 Git 的包装，因此你可以为当前代码库安装 git-flow。git-flow 非常简单，除了为你创建分支外，它不会更改代码库中的任何内容。

要在 Mac 机器上安装 ，请在终端中执行 brew install git-flow 。

要在 Windows 机器上安装，你需要 下载并安装 git-flow 。安装完成后，运行 git flow init 命令，就可以在项目中使用它了。

### 7.5.Git Fork 工作流
Fork 工作流在使用开源软件的团队中很流行。

该流程通常如下所示：
1. 开发人员 fork 开源软件的官方代码库。在他们的帐户中创建此代码库的副本。
1. 然后，开发人员将代码库从其帐户克隆到本地系统。
1. 官方代码库的远端源已添加到克隆到本地系统的代码库中。
1. 开发人员创建一个新的功能分支，该分支将在其本地系统中创建，进行更改并提交。
1. 这些更改以及分支将被推送到其帐户上开发人员的代码库副本。
1. 从该新功能分支创建一个 pull request，提交到官方代码库。
1. 官方代码库的维护者检查 pull request 中的修改并批准将这些修改合并到官方代码库中。



## 8.问题集
### 8.1.修改.gitignore后生效
在使用git的时候我们有时候需要忽略一些文件或者文件夹。我们一般在仓库的根目录创建.gitignore文件
在提交之前，修改.gitignore文件，添加需要忽略的文件。然后再做add  commit push 等
但是有时在使用过称中，需要对.gitignore文件进行再次的修改。这次我们需要清除一下缓存cache，才能是.gitignore 生效。
具体做法：
```
git rm -r --cached .  #清除缓存
git add . #重新trace file
git commit -m "update .gitignore" #提交和注释
git push origin master #可选，如果需要同步到remote上的话
```
这样就能够使修改后的.gitignore生效。

### 8.2.Git 多平台换行符问题(LF or CRLF)
目前，在开发中，使用 Git 作为版本管理工具还是比较流行的，大量的开源项目都在往 Github 迁移。Windows 上有 Git bash 客户端，基于 MinGW，有很多 GNU 工具可用，体验还不错。

在做完工作后，我尝试 git add .，想着这块工作可以告一段落了，而事实是：
```
$ git add .
fatal: CRLF would be replaced by LF ...
```
一脸懵逼，Google 一下吧，看看是什么原因。发现，这已经是一个非常经典的问题了：

![Image [110]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/project-manage/20210425232733.png)

早就听说过这个问题，总算是亲自踩到这个坑里了。

文本文件所使用的换行符，在不同的系统平台上是不一样的。UNIX/Linux 使用的是 0x0A（LF），早期的 Mac OS 使用的是 0x0D（CR），后来的 OS X 在更换内核后与 UNIX 保持一致了。但 DOS/Windows 一直使用 0x0D0A（CRLF） 作为换行符。

跨平台协作开发是常有的，不统一的换行符确实对跨平台的文件交换带来了麻烦。最大的问题是，在不同平台上，换行符发生改变时，Git 会认为整个文件被修改，这就造成我们没法 diff，不能正确反映本次的修改。还好 Git 在设计时就考虑了这一点，其提供了一个 autocrlf 的配置项，用于在提交和检出时自动转换换行符，该配置有三个可选项：
- true: 提交时转换为 LF，检出时转换为 CRLF
- false: 提交检出均不转换
- input: 提交时转换为LF，检出时不转换

用如下命令即可完成配置：
```
# 提交时转换为LF，检出时转换为CRLF
git config --global core.autocrlf true

# 提交时转换为LF，检出时不转换
git config --global core.autocrlf input

# 提交检出均不转换
git config --global core.autocrlf false
```

如果把 autocrlf 设置为 false 时，那另一个配置项 safecrlf 最好设置为 ture。该选项用于检查文件是否包含混合换行符，其有三个可选项：
```
- true: 拒绝提交包含混合换行符的文件
- false: 允许提交包含混合换行符的文件
- warn: 提交包含混合换行符的文件时给出警告
```
配置方法：
```
# 拒绝提交包含混合换行符的文件
git config --global core.safecrlf true

# 允许提交包含混合换行符的文件
git config --global core.safecrlf false

# 提交包含混合换行符的文件时给出警告
git config --global core.safecrlf warn
```

到此，还并未解决我遇到的问题。实际上，我们有两种办法解决。

一种是将配置项改为如下的形式：
```
$ git config --global core.autocrlf false
$ git config --global core.safecrlf false
```
这种方式是不推荐的，虽然代码能被提交，但是项目中的文件可能会包含两种格式的换行符。而且会有如上提到的问题，文件被视为整个被修改，无法 diff，之所以使用版本控制工具，最重要的原因之一就是其 diff 功能。

另一种办法是，手动将文件的换行符转化为 LF，这可以通过编辑器来完成，大部分编辑器都可以将文件的换行符风格设置为 unix 的形式。也可以使用 dos2unix 转换工具来完成，Windows 上 Git bash 客户端自带了该工具。其他系统上也可以安装该工具，例如 Ubuntu 上安装：
```
sudo apt-get install dos2unix
```
有了该工具，可以批量的把项目中的文件都转化一遍：
```
find . -type fxargs dos2unix
```
或者
```
find . -type f -exec dos2unix {} +
```

如果涉及到在多个系统平台上工作，推荐将 git 做如下配置：
```
$ git config --global core.autocrlf input
$ git config --global core.safecrlf true
```
也就是让代码仓库使用统一的换行符(LF)，如果代码中包含 CRLF 类型的文件时将无法提交，需要用 dos2unix 或者其他工具手动转换文件类型。当然，可以根据自己的需要进行更为合适的配置！

### 8.3.git每次提交都输入密码
> .gitconfig 文件中添加
```
[credential]    
    helper = store
```
> 或者在git bash 中执行
```
git config --global credential.helper store
```