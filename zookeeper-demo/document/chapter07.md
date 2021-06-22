[toc]



# Zookeeper开源客户端–zkClient

`ZkClient` 是 `GitHub` 上的一个开源 `zookeeper` 客户端，再 `Zookeeper` 原生 `API` 接口上进行了包装，同时再内部还实现了 `Session` 超时重连、`Watcher` 反复注册等功能

- **添加依赖**

  ```
  	<dependency>
  		 <groupId>com.101tec</groupId>
  		 <artifactId>zkclient</artifactId>
  		 <version>0.2</version>
  	</dependency>
  ```

- **创建会话**

  ```
  import java.io.IOException;
  import org.I0Itec.zkclient.ZkClient;
  public class CreateSession {
  	 /*
  	 创建⼀个zkClient实例来进⾏连接
  	 注意：zkClient通过对zookeeperAPI内部包装，将这个异步的会话创建过程同步化了
  	 */
  	 public static void main(String[] args) {
  		 ZkClient zkClient = new ZkClient("127.0.0.1:2181");
  		 System.out.println("ZooKeeper session established.");
  	 }
  }
  ```

- **创建节点**

  ```
  import org.I0Itec.zkclient.ZkClient;
  public class Create_Node_Sample {
  	 public static void main(String[] args) {
  		 ZkClient zkClient = new ZkClient("127.0.0.1:2181");
  		 System.out.println("ZooKeeper session established.");
  		 //createParents的值设置为true，可以递归创建节点
  		 zkClient.createPersistent("/lg-zkClient/lg-c1",true);
  		 System.out.println("success create znode.");
  	 }
  }
  ```

- **删除节点**

  `ZkClient` 提供了递归删除节点的接口，即帮助开发者先删除所有子节点（如果存在），再删除父节点

  ```
  import org.I0Itec.zkclient.ZkClient;
  public class Del_Data_Sample {
  	 public static void main(String[] args) throws Exception {
  		 String path = "/lg-zkClient/lg-c1";
  		 ZkClient zkClient = new ZkClient("127.0.0.1:2181", 5000);
  		 zkClient.deleteRecursive(path);
  		 System.out.println("success delete znode.");
  	 }
  }
  ```

- **获取子节点**

  ```
  import java.util.List;
  import org.I0Itec.zkclient.IZkChildListener;
  import org.I0Itec.zkclient.ZkClient;
  public class Get_Children_Sample {
  	 public static void main(String[] args) throws Exception {
  		 ZkClient zkClient = new ZkClient("127.0.0.1:2181", 5000);
  		 List<String> children = zkClient.getChildren("/lg-zkClient");
  		 System.out.println(children);
  		
  		 //注册监听事件
  		 zkClient.subscribeChildChanges(path, new IZkChildListener() {
  		 	public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
  				 System.out.println(parentPath + " 's child changed,
  				currentChilds:" + currentChilds);
  			 }
  		 });
  		 zkClient.createPersistent("/lg-zkClient");
  		 Thread.sleep(1000);
  		 zkClient.createPersistent("/lg-zkClient/c1");
  		 Thread.sleep(1000);
  		 zkClient.delete("/lg-zkClient/c1");
  		 Thread.sleep(1000);
  		 zkClient.delete(path);
  		 Thread.sleep(Integer.MAX_VALUE);
  	 }
  }
  ```

- **获取数据（节点是否存在、更新、删除）**

  ```
  public class Get_Data_Sample {
  	 public static void main(String[] args) throws InterruptedException {
  		 String path = "/lg-zkClient-Ep";
  		 ZkClient zkClient = new ZkClient("127.0.0.1:2181");
  		 //判断节点是否存在
  		 boolean exists = zkClient.exists(path);
  		 if (!exists){
  		 	zkClient.createEphemeral(path, "123");
  		 }
  		
  		 //注册监听
  		 zkClient.subscribeDataChanges(path, new IZkDataListener() {
  			 public void handleDataChange(String path, Object data) throws Exception {
  			 	System.out.println(path+"该节点内容被更新，更新后的内容"+data);
  			 }
  			 public void handleDataDeleted(String s) throws Exception {
  			 	System.out.println(s+" 该节点被删除");
  			 }
  		 });
  		 //获取节点内容
  		 Object o = zkClient.readData(path);
  		 System.out.println(o);
  		 //更新
  		 zkClient.writeData(path,"4567");
  		 Thread.sleep(1000);
  		 //删除
  		 zkClient.delete(path);
  		 Thread.sleep(1000);
  	 }
  }
  ```

  