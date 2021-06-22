[toc]



# Zookeeper的API使用

`Zookeeper` 作为一个分布式框架，主要用于解决分布一致性问题，它提供了多种编程语言 `API`，以下为 `java` 客户端的 `API` 使用方式

`Zookeeper API` 共包含5个包：

- `org.apache.zookeeper`
- `org.apache.zookeeper.data`
- `org.apache.zookeeper.server`
- `org.apache.zookeeper.server.quorum`
- `org.apache.zookeeper.server.upgrade`

其中 `org.apache.zookeeper`，包含 `Zookeeper` 类，是我们最常用的类文件。如果使用 `Zookeeper` 服务，必须先创建一个 `Zookeeper` 实例，一旦客户端和 `Zookeeper` 服务端建立起来连接，`Zookeeper` 系统将会给本次会话分配一个ID指，并且客户端会周期性的向服务端发送心跳来维持会话连接。只要连接有效，客户端就可以使用 `Zookeeper API` 进行处理了。

1. **准备工作：导入依赖**

   ```
   	<dependency>
   		 <groupId>org.apache.zookeeper</groupId>
   		 <artifactId>zookeeper</artifactId>
   		 <version>3.4.14</version>
    	</dependency>
   ```

2. **建立会话**

   ```
   public class CreateSession implements Watcher {
   	 //countDownLatch这个类使⼀个线程等待,主要不让main⽅法结束
   	 private static CountDownLatch countDownLatch = new CountDownLatch(1);
   	 
   	 public static void main(String[] args) throws InterruptedException,
   IOException {
   		 /*
   		 客户端可以通过创建⼀个zk实例来连接zk服务器
   		 new Zookeeper(connectString,sesssionTimeOut,Wather)
   		 connectString: 连接地址：IP：端⼝
   		 sesssionTimeOut：会话超时时间：单位毫秒
   		 Wather：监听器(当特定事件触发监听时，zk会通过watcher通知到客户端)
   		 */
   		 ZooKeeper zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new
   	CreateSession());
   		 System.out.println(zooKeeper.getState());
   		 countDownLatch.await();
   		 //表示会话真正建⽴
   		 System.out.println("=========Client Connected to zookeeper==========");
   	 }
   	// 当前类实现了Watcher接⼝，重写了process⽅法，该⽅法负责处理来⾃Zookeeper服务端的watcher通知，在收到服务端发送过来的SyncConnected事件之后，解除主程序在CountDownLatch上的等待阻塞，⾄此，会话创建完毕
   	 public void process(WatchedEvent watchedEvent) {
   	 	//当连接创建了，服务端发送给客户端SyncConnected事件
   		 if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
   		 	countDownLatch.countDown();
   		 }
   	 }
   }
   ```

3. **创建节点**

   ```
   public class CreateNote implements Watcher {
   	 //countDownLatch这个类使⼀个线程等待,主要不让main⽅法结束
   	 private static CountDownLatch countDownLatch = new CountDownLatch(1);
   	 private static ZooKeeper zooKeeper;
   	 
   	 public static void main(String[] args) throws Exception {
   		 zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new CreateNote());
   		 countDownLatch.await();
   	 }
   	 
   	 public void process(WatchedEvent watchedEvent) {
   		 //当连接创建了，服务端发送给客户端SyncConnected事件
   		 if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
   		 	countDownLatch.countDown();
   		 }
   		 //调⽤创建节点⽅法
   		 try {
   		 	createNodeSync();
   		 } catch (Exception e) {
   		 	e.printStackTrace();
   		 }
   	 }
   	 private void createNodeSync() throws Exception {
   		 /**
   		 * path ：节点创建的路径
   		 * data[] ：节点创建要保存的数据，是个byte类型的
   		 * acl ：节点创建的权限信息(4种类型)
   		 * ANYONE_ID_UNSAFE : 表示任何⼈
   		 * AUTH_IDS ：此ID仅可⽤于设置ACL。它将被客户机验证的ID替
   		换。
   		 * OPEN_ACL_UNSAFE ：这是⼀个完全开放的ACL(常⽤)-->
   		world:anyone
   		 * CREATOR_ALL_ACL ：此ACL授予创建者身份验证ID的所有权限
   		 * createMode ：创建节点的类型(4种类型)
   		 * PERSISTENT：持久节点
   		 * PERSISTENT_SEQUENTIAL：持久顺序节点
   		 * EPHEMERAL：临时节点
   		 * EPHEMERAL_SEQUENTIAL：临时顺序节点
   		 String node = zookeeper.create(path,data,acl,createMode);
   		 */
   		 String node_PERSISTENT = zooKeeper.create("/lg_persistent", "持久节点内容".getBytes("utf-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
   		 String node_PERSISTENT_SEQUENTIAL = zooKeeper.create("/lg_persistent_sequential", "持久节点内容".getBytes("utf-8"),ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
   		 String node_EPERSISTENT = zooKeeper.create("/lg_ephemeral", "临时节点内容".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
   		 System.out.println("创建的持久节点是:"+node_PERSISTENT);
   		 System.out.println("创建的持久顺序节是:"+node_PERSISTENT_SEQUENTIAL);
   		 System.out.println("创建的临时节点是:"+node_EPERSISTENT);
   	 }
   }
   ```

4. **获取节点数据**

   ```
   public class GetNoteData implements Watcher {
   	 //countDownLatch这个类使⼀个线程等待,主要不让main⽅法结束
   	 private static CountDownLatch countDownLatch = new CountDownLatch(1);
   	 private static ZooKeeper zooKeeper;
   	 
   	 public static void main(String[] args) throws Exception {
            zooKeeper = new ZooKeeper("127.0.0.1:2181", 10000, new GetNoteDate());
            Thread.sleep(Integer.MAX_VALUE);
   	 }
   	 public void process(WatchedEvent watchedEvent) {
   		 //⼦节点列表发⽣变化时，服务器会发出NodeChildrenChanged通知，但不会把变化情况告诉给客户端
   		 // 需要客户端⾃⾏获取，且通知是⼀次性的，需反复注册监听
   		 if(watchedEvent.getType() ==Event.EventType.NodeChildrenChanged){
   			 //再次获取节点数据
   			 try {
   				 List<String> children =
   				zooKeeper.getChildren(watchedEvent.getPath(), true);
   			 	System.out.println(children);
   			 } catch (KeeperException e) {
   			 	e.printStackTrace();
   			 } catch (InterruptedException e) {
   			 	e.printStackTrace();
   			 }
   		 }
   		 //当连接创建了，服务端发送给客户端SyncConnected事件
   		 if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
   			 try {
   				 //调⽤获取单个节点数据⽅法
   				 getNoteDate();
   				 getChildrens();
   			 } catch (KeeperException e) {
   			 	e.printStackTrace();
   			 } catch (InterruptedException e) {
   			 	e.printStackTrace();
   			 }
   		 }
   	 }
   	 
   	 private static void getNoteData() throws Exception {
   		 /**
   		 * path : 获取数据的路径
   		 * watch : 是否开启监听
   		 * stat : 节点状态信息
   		 * null: 表示获取最新版本的数据
   		 * zk.getData(path, watch, stat);
   		 */
   		 byte[] data = zooKeeper.getData("/lg_persistent/lg-children", true,null);
   		 System.out.println(new String(data,"utf-8"));
   	 }
   	 
   	 private static void getChildrens() throws KeeperException,InterruptedException {
   		 /*
   		 path:路径
   		 watch:是否要启动监听，当⼦节点列表发⽣变化，会触发监听
   		 zooKeeper.getChildren(path, watch);
   		 */
   		 List<String> children = zooKeeper.getChildren("/lg_persistent", true);
   	 	 System.out.println(children);
   	 }
   }
   ```
   
5. **修改节点**

   ```
   public class updateNote implements Watcher {
   	
   	 private static ZooKeeper zooKeeper;
   	 
   	 public static void main(String[] args) throws Exception {
   		 zooKeeper = new ZooKeeper("127.0.0.1:2181", 5000, new updateNote());
   		 Thread.sleep(Integer.MAX_VALUE);
   	 }
   	 public void process(WatchedEvent watchedEvent) {
   		 //当连接创建了，服务端发送给客户端SyncConnected事件
   		 try {
   		 	updateNodeSync();
   		 } catch (Exception e) {
   		 	e.printStackTrace();
   		 }
   	 }
   	 private void updateNodeSync() throws Exception {
   	
   		 /*
   		 path:路径
   		 data:要修改的内容 byte[]
   		 version:为-1，表示对最新版本的数据进⾏修改
   		 zooKeeper.setData(path, data,version);
   		 */
   		 byte[] data = zooKeeper.getData("/lg_persistent", false, null);
   		 System.out.println("修改前的值:"+new String(data));
   		 //修改 stat:状态信息对象 -1:最新版本
   		 Stat stat = zooKeeper.setData("/lg_persistent", "客户端修改内容".getBytes(), -1);
   		 byte[] data2 = zooKeeper.getData("/lg_persistent", false, null);
   		 System.out.println("修改后的值:"+new String(data2));
   	 }
   }
   ```
   
6. **删除节点数据**

   ```
   public class DeleteNote implements Watcher {
   	 private static ZooKeeper zooKeeper;
   	 public static void main(String[] args) throws Exception {
   		 zooKeeper = new ZooKeeper("10.211.55.4:2181", 5000, new DeleteNote());
   		 Thread.sleep(Integer.MAX_VALUE);
   	 }
   	 public void process(WatchedEvent watchedEvent) {
   		 //当连接创建了，服务端发送给客户端SyncConnected事件
   		 try {
   		 	deleteNodeSync();
   		 } catch (Exception e) {
   		 	e.printStackTrace();
   		 }
   	 }
   	 private void deleteNodeSync() throws KeeperException,InterruptedException{
   		 /*
   		 zooKeeper.exists(path,watch) :判断节点是否存在
   		 zookeeper.delete(path,version) : 删除节点
   		 */
   		 Stat exists = zooKeeper.exists("/lg_persistent/lg-children", false);
   		 System.out.println(exists == null ? "该节点不存在":"该节点存在");
   		 zooKeeper.delete("/lg_persistent/lg-children",-1);
   		 Stat exists2 = zooKeeper.exists("/lg_persistent/lg-children", false);
   		 System.out.println(exists2 == null ? "该节点不存在":"该节点存在");
   	 }
   }
   ```

   

