[toc]



# Zookeeper命令行操作

- **使用zkClient进入zookeeper客户端命令行**

  ```
  cd /usr/local/zookeeper-3.4.14/bin
  ./zkcli.sh 连接本地的zookeeper服务器
  ./zkCli.sh -server ip:port 连接指定服务器
  ```

  连接成功后，系统会输出Zookeeper的相关环境及配置信息。输入help后，屏幕会输出可用的zookeeper命令

  ![image-20210313142714788](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313142714.png)

- **创建节点**

  使用create命令

  ```
  create [-s][-e] path data acl
  #其中，-s或-e分别指定节点特性，顺序或临时节点，若不指定则为持久节点；
  #acl用来进行权限控制
  
  create -s /zk-test 123
  #创建一个zk-test的顺序节点，节点内容为123
  
  create -e /zk-temp 123
  #创建一个zk-temp的临时节点，节点内容为123
  
  create /zk-permanent 123
  #创建一个zk-permanent的持节节点
  ```

  退出使用`quit`命令

- **读取节点**

  与读取相关的命令有`ls`和`get`命令

  `ls`命令可以列出Zookeeper指定节点下的所有子节点，但只能查看指定节点下的第一级的所有子节点

  ```
  ls path
  #其中，path标识的是指定数据节点的节点路径
  ```

  ![image-20210313142840227](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313142840.png)

  `get`命令可以获取Zookeeper指定节点的数据内容和属性信息

  ```
  get path
  ```

  ![image-20210313142909997](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313142910.png)

- **更新节点**

  使用`set`命令，可以更新指定节点的数据内容

  ```
  set path data [version]
  ```

  其中，data就是要更新的新内容，version标识数据版本，在zookeeper中，节点数据是有版本概念的，这个参数用于指定本次操作时基于ZNode的哪一个数据版本进行的

  ```
  set /zk-permanent 456
  #将/zk-permanent数据更新为456
  ```

  ![image-20210313142954783](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/zookeeper-demo/20210313142954.png)

- **删除节点**

  使用`delete`命令可以删除Zookeeper上的指定节点

  ```
  delete path [version]
  ```

  其中version也是表示数据版本

  ```
  delete /zk-permanent
  #删除/zk-permanent节点
  ```

  **若删除节点存在子节点，那么无法删除该节点，必须先删除子节点，再删除父节点**















