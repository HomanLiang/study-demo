[toc]



# Dubbo URL和协议

## URL简介
URL也就是Uniform Resource Locator，中文叫统一资源定位符。Dubbo中无论是服务消费方，或者服务提供方，或者注册中心。都是通过URL进行定位资源的。所以今天来聊聊Dubbo中的统一URL资源模型是怎么样的。
## Dubbo中的URL
标准的URL格式如下：
```
protocol://username:password@host:port/path?key=value&key=value
```
在Dubbo中URL也是主要由上面的参数组成。
```
public URL(String protocol, String username, String password, String host, int port, String path, Map<String, String> parameters) {
   if ((username == null || username.length() == 0) 
         && password != null && password.length() > 0) {
      throw new IllegalArgumentException("Invalid url, password without username!");
   }
   this.protocol = protocol;
   this.username = username;
   this.password = password;
   this.host = host;
   this.port = (port < 0 ? 0 : port);
   this.path = path;
   // trim the beginning "/"
   while(path != null && path.startsWith("/")) {
       path = path.substring(1);
   }
   if (parameters == null) {
       parameters = new HashMap<String, String>();
   } else {
       parameters = new HashMap<String, String>(parameters);
   }
   this.parameters = Collections.unmodifiableMap(parameters);
}
```
可以从上面源码看出：
- protocol：一般是 dubbo 中的各种协议 如：dubbo、thrift、http、zk
- username/password：用户名/密码
- host/port：主机IP地址/端口号
- path：接口名称
- parameter：参数键值对

大致样子如下：
```
dubbo://192.168.1.6:20880/moe.cnkirito.sample.HelloService?timeout=3000
描述一个 dubbo 协议的服务

zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=demo-consumer&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=1214&qos.port=33333&timestamp=1545721981946
描述一个 zookeeper 注册中心

consumer://30.5.120.217/org.apache.dubbo.demo.DemoService?application=demo-consumer&category=consumers&check=false&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=1209&qos.port=33333&side=consumer&timestamp=1545721827784
描述一个消费者
```

## Dubbo中有关URL的服务
### 解析服务
- Spring在遇到dubbo名称空间时，会回调DubboNamespaceHandler。这个类也是Dubbo基于spring扩展点编写的解析xml文件的类。
- 解析的xml标签使用DubboBeanDefinitionParser将其转化为bean对象。
- 服务提供方在ServiceConfig.export()初始化时将bean对象转化为URL格式，所有Bean属性转换成URL参数。这时候的URL就会传给协议扩展点。根据URL中protocol的值通过扩展点自适应机制进行不同协议的服务暴露或引用。
- 而服务消费方则是ReferenceConfig.export()方法。
### 直接暴露服务端口
- 在没有注册中心时，ServiceConfig解析出的URL格式为：dubbo://service-host/com.foo.FooService?version=1.0.0
- 基于扩展点自适应机制。通过URL的dubbo://协议头识别，这时候就调用DubboProtocol中的export方法进行暴露服务端口
### 向注册中心暴露服务端口
- 有注册中心时。ServiceConfig解析出的URL格式就类似：registry://registry-host/org.apache.dubbo.registry.RegistryService?export=URL.encode("dubbo://service-host/com.foo.FooService?version=1.0.0")
- 基于扩展点自适应机制，识别到URL以registry://开头，就会调用RegistryProtocol中的export方法先将该URL注册到注册中心里
- 再传给Protocol扩展点进行暴露，这时候就只剩下dubbo://service-host/com.foo.FooService?version=1.0.0。同样的基于dubbo://协议头识别，通过DubboProtocol的export方法打开服务端口
### 直接引用服务
- 在没有注册中心，ReferenceConfig解析出的URL格式就为dubbo://service-host/com.foo.FooService?version=1.0.0
- 基于扩展点自适应机制，通过 URL 的dubbo://协议头识别，直接调用DubboProtocol的refer方法，返回提供者引用
### 从注册中心引用服务
- 有注册中心时，ReferenceCofig解析出来的URL格式为：registry://registry-host/org.apache.dubbo.registry.RegistryService?refer=URL.encode("consumer://consumer-host/com.foo.FooService?version=1.0.0")
- 同样先识别URL的协议头，调用RegistryProtocol中的refer方法
- 通过refer参数中的条件查询到提供者的URL。如dubbo://service-host/com.foo.FooService?version=1.0.0。此时就会调用DubboProtocol中的refer方法得到提供者引用
- 最后若是存在集群Cluster扩展点，需要伪装成单个提供者引用返回

## Dubbo协议
### 协议简介
聊完了Dubbo中的URL模型就来聊聊Dubbo中的协议。协议是双方确定的交流语义，协议在双方传输数据中起到的了交换作用，没有协议就无法完成数据交换。在dubbo中就是Codec2
```
@SPI
public interface Codec2 {

    @Adaptive({Constants.CODEC_KEY})
    void encode(Channel channel, ChannelBuffer buffer, Object message) throws IOException;

    @Adaptive({Constants.CODEC_KEY})
    Object decode(Channel channel, ChannelBuffer buffer) throws IOException;


    enum DecodeResult {
        NEED_MORE_INPUT, SKIP_SOME_INPUT
    }

}
```
encode是将通信对象编码到ByteBufferWrapper，decode是将从网络上读取的ChannelBuffer解码为Object。
## 协议图解
![Image [2]](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/mybatis-demo/20210408003822.png)

具体的解释如下：

- Magic High&Magic Low（16bits）：标识协议的版本号，Dubbo协议：0xdabb

- Req/Res（1bit）：1代表请求，0代表响应

- 2Way（1bit）仅在Req/Res为1时才有用（也就是请求），标识是否期望从服务器返回值。如果需要则为1.

- Event（1bit）标识是否是事件消息。比如：如果是心跳时间则设置为1

- Serialization ID（5bits）：标识序列化类型。

	Status（8bits）：仅在Req/Res为0时才有用（即响应）。主要用于标识响应状态
	
	- 20：OK，响应正确
	- 30：CLIENT_TIMEOUT，客户端连接超时
	- 31：SERVER_TIMEOUT，服务端连接超时
	- 40：BAD_REQUEST，错误请求
	- 50：BAD_RESPONSE，错误响应
	- 60：SERVICE_NOT_FOUND，服务未找到
	- 70：SERVICE_ERROR，服务出错
	- 80：SERVER_ERROR，服务端出错
	- 90：CLIENT_ERROR，客户端出错
	- 100：SERVER_THREADPOOL_EXHAUSTED_ERROR，服务端线程池已满，无法创建新线程
	
- Request ID（64bits）：标识唯一请求，long类型

- Data Length（32bits）：序列化后的内容长度（可变的），int类型。这也是为什么实体类需要实现序列化接口。因为Dubbo协议底层是传输序列化后的内容

- Variable Part：被特定的序列化类型序列化后，每个部分都是一个byte[]或byte
	- 如果是请求包，则每个部分依次为：Dubbo Version、Service name、Service version、Method name、Method Parameter types、Method arguments、Attachments
	- 如果是响应包，则每个部分依次为：返回值类型（0表示异常，1是正常响应，2是返回空值）、返回值。