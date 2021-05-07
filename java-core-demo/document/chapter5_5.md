[toc]



# Java 网络编程

## 1.Socket 和 ServerSocket

套接字（Socket）使用 TCP 提供了两台计算机之间的通信机制。 客户端程序创建一个套接字，并尝试连接服务器的套接字。

**Java 通过 Socket 和 ServerSocket 实现对 TCP 的支持**。Java 中的 Socket 通信可以简单理解为：**`java.net.Socket` 代表客户端，`java.net.ServerSocket` 代表服务端**，二者可以建立连接，然后通信。

以下为 Socket 通信中建立建立的基本流程：

- 服务器实例化一个 `ServerSocket` 对象，表示服务器绑定一个端口。
- 服务器调用 `ServerSocket` 的 `accept()` 方法，该方法将一直等待，直到客户端连接到服务器的绑定端口（即监听端口）。
- 服务器监听端口时，客户端实例化一个 `Socket` 对象，指定服务器名称和端口号来请求连接。
- `Socket` 类的构造函数试图将客户端连接到指定的服务器和端口号。如果通信被建立，则在客户端创建一个 Socket 对象能够与服务器进行通信。
- 在服务器端，`accept()` 方法返回服务器上一个新的 `Socket` 引用，该引用连接到客户端的 `Socket` 。

连接建立后，可以通过使用 IO 流进行通信。每一个 `Socket` 都有一个输出流和一个输入流。客户端的输出流连接到服务器端的输入流，而客户端的输入流连接到服务器端的输出流。

TCP 是一个双向的通信协议，因此数据可以通过两个数据流在同一时间发送，以下是一些类提供的一套完整的有用的方法来实现 sockets。

### 1.1.ServerSocket

服务器程序通过使用 `java.net.ServerSocket` 类以获取一个端口，并且监听客户端请求连接此端口的请求。

#### 1.1.1.ServerSocket 构造方法

`ServerSocket` 有多个构造方法：

| **方法**                                                   | **描述**                                                     |
| ---------------------------------------------------------- | ------------------------------------------------------------ |
| `ServerSocket()`                                           | 创建非绑定服务器套接字。                                     |
| `ServerSocket(int port)`                                   | 创建绑定到特定端口的服务器套接字。                           |
| `ServerSocket(int port, int backlog)`                      | 利用指定的 `backlog` 创建服务器套接字并将其绑定到指定的本地端口号。 |
| `ServerSocket(int port, int backlog, InetAddress address)` | 使用指定的端口、监听 `backlog` 和要绑定到的本地 IP 地址创建服务器。 |

#### 1.1.2.ServerSocket 常用方法

创建非绑定服务器套接字。 如果 `ServerSocket` 构造方法没有抛出异常，就意味着你的应用程序已经成功绑定到指定的端口，并且侦听客户端请求。

这里有一些 `ServerSocket` 类的常用方法：

| **方法**                                     | **描述**                                              |
| -------------------------------------------- | ----------------------------------------------------- |
| `int getLocalPort()`                         | 返回此套接字在其上侦听的端口。                        |
| `Socket accept()`                            | 监听并接受到此套接字的连接。                          |
| `void setSoTimeout(int timeout)`             | 通过指定超时值启用/禁用 `SO_TIMEOUT`，以毫秒为单位。  |
| `void bind(SocketAddress host, int backlog)` | 将 `ServerSocket` 绑定到特定地址（IP 地址和端口号）。 |

### 1.2.Socket

`java.net.Socket` 类代表客户端和服务器都用来互相沟通的套接字。客户端要获取一个 `Socket` 对象通过实例化 ，而 服务器获得一个 `Socket`对象则通过 `accept()` 方法 a 的返回值。

#### 1.2.1.Socket 构造方法

`Socket` 类有 5 个构造方法：

| **方法**                                                     | **描述**                                                 |
| ------------------------------------------------------------ | -------------------------------------------------------- |
| `Socket()`                                                   | 通过系统默认类型的 `SocketImpl` 创建未连接套接字         |
| `Socket(String host, int port)`                              | 创建一个流套接字并将其连接到指定主机上的指定端口号。     |
| `Socket(InetAddress host, int port)`                         | 创建一个流套接字并将其连接到指定 IP 地址的指定端口号。   |
| `Socket(String host, int port, InetAddress localAddress, int localPort)` | 创建一个套接字并将其连接到指定远程主机上的指定远程端口。 |
| `Socket(InetAddress host, int port, InetAddress localAddress, int localPort)` | 创建一个套接字并将其连接到指定远程地址上的指定远程端口。 |

当 Socket 构造方法返回，并没有简单的实例化了一个 Socket 对象，它实际上会尝试连接到指定的服务器和端口。

#### 1.2.2.Socket 常用方法

下面列出了一些感兴趣的方法，注意客户端和服务器端都有一个 Socket 对象，所以无论客户端还是服务端都能够调用这些方法。

| **方法**                                        | **描述**                                              |
| ----------------------------------------------- | ----------------------------------------------------- |
| `void connect(SocketAddress host, int timeout)` | 将此套接字连接到服务器，并指定一个超时值。            |
| `InetAddress getInetAddress()`                  | 返回套接字连接的地址。                                |
| `int getPort()`                                 | 返回此套接字连接到的远程端口。                        |
| `int getLocalPort()`                            | 返回此套接字绑定到的本地端口。                        |
| `SocketAddress getRemoteSocketAddress()`        | 返回此套接字连接的端点的地址，如果未连接则返回 null。 |
| `InputStream getInputStream()`                  | 返回此套接字的输入流。                                |
| `OutputStream getOutputStream()`                | 返回此套接字的输出流。                                |
| `void close()`                                  | 关闭此套接字。                                        |

### 1.3.Socket 通信示例

服务端示例：

```
public class HelloServer {

    public static void main(String[] args) throws Exception {
        // Socket 服务端
        // 服务器在8888端口上监听
        ServerSocket server = new ServerSocket(8888);
        System.out.println("服务器运行中，等待客户端连接。");
        // 得到连接，程序进入到阻塞状态
        Socket client = server.accept();
        // 打印流输出最方便
        PrintStream out = new PrintStream(client.getOutputStream());
        // 向客户端输出信息
        out.println("hello world");
        client.close();
        server.close();
        System.out.println("服务器已向客户端发送消息，退出。");
    }

}
```

客户端示例：

```
public class HelloClient {

    public static void main(String[] args) throws Exception {
        // Socket 客户端
        Socket client = new Socket("localhost", 8888);
        InputStreamReader inputStreamReader = new InputStreamReader(client.getInputStream());
        // 一次性接收完成
        BufferedReader buf = new BufferedReader(inputStreamReader);
        String str = buf.readLine();
        buf.close();
        client.close();
        System.out.println("客户端接收到服务器消息：" + str + "，退出");
    }

}
```

## 2.DatagramSocket 和 DatagramPacket

Java 通过 `DatagramSocket` 和 `DatagramPacket` 实现对 UDP 协议的支持。

- `DatagramPacket`：数据包类
- `DatagramSocket`：通信类

UDP 服务端示例：

```
public class UDPServer {

    public static void main(String[] args) throws Exception { // 所有异常抛出
        String str = "hello World!!!";
        DatagramSocket ds = new DatagramSocket(3000); // 服务端在3000端口上等待服务器发送信息
        DatagramPacket dp =
            new DatagramPacket(str.getBytes(), str.length(), InetAddress.getByName("localhost"), 9000); // 所有的信息使用buf保存
        System.out.println("发送信息。");
        ds.send(dp); // 发送信息出去
        ds.close();
    }

}
```

UDP 客户端示例：

```
public class UDPClient {

    public static void main(String[] args) throws Exception { // 所有异常抛出
        byte[] buf = new byte[1024]; // 开辟空间，以接收数据
        DatagramSocket ds = new DatagramSocket(9000); // 客户端在9000端口上等待服务器发送信息
        DatagramPacket dp = new DatagramPacket(buf, 1024); // 所有的信息使用buf保存
        ds.receive(dp); // 接收数据
        String str = new String(dp.getData(), 0, dp.getLength()) + "from " + dp.getAddress().getHostAddress() + "："
            + dp.getPort();
        System.out.println(str); // 输出内容
    }

}
```

## 3.InetAddress

`InetAddress` 类表示互联网协议(IP)地址。

没有公有的构造函数，只能通过静态方法来创建实例。

```
InetAddress.getByName(String host);
InetAddress.getByAddress(byte[] address);
```

## 4.URL

可以直接从 URL 中读取字节流数据。

```
public static void main(String[] args) throws IOException {

    URL url = new URL("http://www.baidu.com");

    /* 字节流 */
    InputStream is = url.openStream();

    /* 字符流 */
    InputStreamReader isr = new InputStreamReader(is, "utf-8");

    /* 提供缓存功能 */
    BufferedReader br = new BufferedReader(isr);

    String line;
    while ((line = br.readLine()) != null) {
        System.out.println(line);
    }

    br.close();
}
```



## 5.HttpClient

### 5.1.HttpClient的介绍

> The most essential function of HttpClient is to execute HTTP methods. Execution of an HTTP method involves one or several HTTP request / HTTP response exchanges, usually handled internally by HttpClient. The user is expected to provide a request object to execute and HttpClient is expected to transmit the request to the target server return a corresponding response object, or throw an exception if execution was unsuccessful.

HttpClient最基本的功能就是执行http方法，执行http方法包括了一次或者几次HTTP请求和相应的变化，通常也是通过HttpClient来处理的。只要用户提供一个request的对象，HttpClient就会将用户的请求发送到目标服务器上，并且返回一个respone对象，如果没有执行成功将抛出一个异常。

通过文档的介绍我们可以知道，发送HTTP请求一般可以分为以下步骤

- 取得HttpClient对象
- 封装http请求
- 执行http请求
- 处理结果

其中可以发送的请求类型有GET, HEAD, POST, PUT, DELETE, TRACE 和 OPTIONS

> HttpClient supports out of the box all HTTP methods defined in the HTTP/1.1 specification: GET, HEAD, POST, PUT, DELETE, TRACE and OPTIONS.

官方文档中的示例

```
//1.获得一个httpclient对象
CloseableHttpClient httpclient = HttpClients.createDefault();
//2.生成一个get请求
HttpGet httpget = new HttpGet("http://localhost/");
//3.执行get请求并返回结果
CloseableHttpResponse response = httpclient.execute(httpget);
try {
    //4.处理结果
} finally {
    response.close();
}
```


介绍一下最常用的HttpGet和HttpPost。

RESTful提倡，通过HTTP请求对应的POST、GET、PUT、DELETE来完成对应的CRUD操作。

所以本文介绍一下通过GET获取数据和POST提交数据的实现方法。

**5.1.1.发送HttpGet**
先介绍发送HttpGet请求

	/**
	 * 发送HttpGet请求
	 * @param url
	 * @return
	 */
	public static String sendGet(String url) {
		//1.获得一个httpclient对象
		CloseableHttpClient httpclient = HttpClients.createDefault();
		//2.生成一个get请求
		HttpGet httpget = new HttpGet(url);
		CloseableHttpResponse response = null;
		try {
			//3.执行get请求并返回结果
			response = httpclient.execute(httpget);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		String result = null;
		try {
			//4.处理结果，这里将结果返回为字符串
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				result = EntityUtils.toString(entity);
			}
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
**5.1.2.发送HttpPost**

发送HttpPost的方法和发送HttpGet很类似，只是将请求类型给位HttpPost即可。

代码如下

	/**
	 * 发送不带参数的HttpPost请求
	 * @param url
	 * @return
	 */
	public static String sendPost(String url) {
		//1.获得一个httpclient对象
		CloseableHttpClient httpclient = HttpClients.createDefault();
		//2.生成一个post请求
		HttpPost httppost = new HttpPost(url);
		CloseableHttpResponse response = null;
		try {
			//3.执行get请求并返回结果
			response = httpclient.execute(httppost);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//4.处理结果，这里将结果返回为字符串
		HttpEntity entity = response.getEntity();
		String result = null;
		try {
			result = EntityUtils.toString(entity);
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return result;
	}
**5.1.3.带参数的HttpPost**

发送带参数的HttpPost

> Many applications need to simulate the process of submitting an HTML form, for instance, in order to log in to a web application or submit input data. HttpClient provides the entity class UrlEncodedFormEntity to facilitate the process.

HttpClient通过UrlEncodedFormEntity，来提交带参数的请求

将需要提交的参数放在map里

代码如下

	/**
	 * 发送HttpPost请求，参数为map
	 * @param url
	 * @param map
	 * @return
	 */
	public static String sendPost(String url, Map<String, String> map) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		for (Map.Entry<String, String> entry : map.entrySet()) {
			//给参数赋值
			formparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, Consts.UTF_8);
		HttpPost httppost = new HttpPost(url);
		httppost.setEntity(entity);
		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(httppost);
		} catch (IOException e) {
			e.printStackTrace();
		}
		HttpEntity entity1 = response.getEntity();
		String result = null;
		try {
			result = EntityUtils.toString(entity1);
		} catch (ParseException | IOException e) {
			e.printStackTrace();
		}
		return result;
	}

### 5.2.HttpClient忽略SSL证书

今天公司项目请求一个接口地址是ip格式的，如：https://120.20.xx.xxx/xx/xx，报一个SSL的错：

由于之前请求的接口地址都是域名地址，如：https://www.xxx.com/xxx/xxx，

使用HttpClient工具，忽略SSL认证代码如下：

```
package com.allchips.common.util;

import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @description:
 * @author: Homan Liang
 * @time: 2021/3/30 16:43
 */
public class HttpTest {

    public static void main(String[] args) throws Exception {
//        SapHttpUtil.httpGet("https://www.cnblogs.com/spll/p/11856610.html", null);
//
//        String httpGet = RestTemplateUtil.httpGet("https://www.cnblogs.com/spll/p/11856610.html", null);


        testPostNoSSL("https://test.allchips.com/", "", "");
    }

    public static String testPostNoSSL(String postUrl, String paramJson,String token) {
        String resultStr = "";  //返回结果
        try {

//            1、创建httpClient
            CloseableHttpClient buildSSLCloseableHttpClient = buildSSLCloseableHttpClient();

            System.setProperty("jsse.enableSNIExtension", "false");
            HttpPost httpPost = new HttpPost(postUrl);


            httpPost.setHeader("Authorization", "Bearer " +token);



            // 设置请求和传输超时时间
            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(60*1000)
                    .setConnectTimeout(60*1000).build();

            httpPost.setConfig(requestConfig);

            httpPost.setHeader("Content-Type","application/json;charset=UTF-8");

            //放入请求参数
            StringEntity data = new StringEntity(paramJson, Charset.forName("UTF-8"));
            httpPost.setEntity(data);
            //发送请求，接收结果
            CloseableHttpResponse response = buildSSLCloseableHttpClient.execute(httpPost);

            //4.获取响应对象中的响应码
            StatusLine statusLine = response.getStatusLine();//获取请求对象中的响应行对象
            int responseCode = statusLine.getStatusCode();//从状态行中获取状态码

            System.out.println(responseCode);

            if (responseCode == 200) {
                // 打印响应内容
                resultStr = EntityUtils.toString(response.getEntity(), "UTF-8");

                //5.  可以接收和发送消息
                org.apache.http.HttpEntity entity = response.getEntity();
                //6.从消息载体对象中获取操作的读取流对象
                InputStream input = entity.getContent();

            } else {
                System.out.println("响应失败! : " + response.toString());
            }
            buildSSLCloseableHttpClient.close();


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return resultStr;
    }

    /**
     * ============忽略证书
     */
    private static CloseableHttpClient buildSSLCloseableHttpClient()
            throws Exception {
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null,
                new TrustStrategy() {
                    // 信任所有
                    @Override
                    public boolean isTrusted(X509Certificate[] chain,
                                             String authType) throws CertificateException {
                        return true;
                    }
                }).build();
        // ALLOW_ALL_HOSTNAME_VERIFIER:这个主机名验证器基本上是关闭主机名验证的,实现的是一个空操作，并且不会抛出javax.net.ssl.SSLException异常。
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext, new String[] { "TLSv1" }, null,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        return HttpClients.custom().setSSLSocketFactory(sslsf).build();
    }


}
```



## 6.Java RMI

### 6.1.Java RMI是什么

Java RMI（Java Remote Method Invocation），即Java远程方法调用。是Java编程语言里，一种用于实现远程过程调用的应用程序**编程接口**。

> 注：很多文章或博客把RMI说成是一种消息协议，官方定义是java 编程接口。

RMI 使用 JRMP（Java Remote Message Protocol，Java远程消息交换协议）实现，使得客户端运行的程序可以调用远程服务器上的对象。是实现RPC的一种方式。

### 6.2.RMI 的使用

1、server端：创建远程对象，并注册远程对象

```java
//定义远程对象的接口
public interface HelloService extends Remote {
    String say() throws RemoteException;
}

//接口的实现
public class HelloServiceImpl extends UnicastRemoteObject implements HelloService {

    public HelloServiceImpl() throws RemoteException{
        super();
    }

    @Override
    public String say() throws RemoteException {
        return "Hello";
    }
}

//注册远程对象
public class Service {
    public static void main(String[] args) throws RemoteException, AlreadyBoundException, MalformedURLException {
        HelloServiceImpl helloService = new HelloServiceImpl();
        LocateRegistry.createRegistry(1099);
        Naming.bind("rmi://127.0.0.1/hello",helloService);

    }
}
```

2、client端：查找远程对象，调用远程方法

```java
public class Client {
    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        HelloService helloService = (HelloService) Naming.lookup("rmi://127.0.0.1/hello");
        System.out.println(helloService.say());

    }
}
```

### 6.3.RMI 的原理

RMI本质是TCP网络通信，内部封装了序列化和通信过程，使用代理实现接口调用。下一篇文章带大家手写一个RPC框架，会更加清晰的明白RMI原理。

![3972450-7c38625c0b60b1b1](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321233824.webp)

**服务端**

```java
//调用UnicastRemoteObject构造函数，发布对象
  protected UnicastRemoteObject(int port) throws RemoteException
    {
        this.port = port;
        exportObject((Remote) this, port);
    }

//创建UnicastServerRef对象，对象内有引用LiveRef(tcp通信)
    public static Remote exportObject(Remote obj, int port)
        throws RemoteException
    {
        return exportObject(obj, new UnicastServerRef(port));
    }


   public Remote exportObject(Remote var1, Object var2, boolean var3) throws RemoteException {
        Class var4 = var1.getClass();

        Remote var5;
        try {
//创建远程代理类，getClientRef提供的InvocationHandler提供了TCP连接
            var5 = Util.createProxy(var4, this.getClientRef(), this.forceStubUse);
        } catch (IllegalArgumentException var7) {
            throw new ExportException("remote object implements illegal remote interface", var7);
        }

        if (var5 instanceof RemoteStub) {
            this.setSkeleton(var1);
        }
//包装实际对象，并将其暴露在TCP端口上，等待客户端调用
        Target var6 = new Target(var1, this, var5, this.ref.getObjID(), var3);
        this.ref.exportObject(var6);
        this.hashToMethod_Map = (Map)hashToMethod_Maps.get(var4);
        return var5;
    }
```

![3972450-c06d14d2c4486299](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/java-core-demo/20210321233909.webp)

**客户端**

```java
//客户端通过LocateRegistry的getRegistry方法创建RegistryImpl_Stub代理，调用RegistryImpl_Stub的newCall方法建立与服务端Skeleton的映射
public static Registry getRegistry(String host, int port,
                                       RMIClientSocketFactory csf)
        throws RemoteException
    {
        Registry registry = null;

        if (port <= 0)
            port = Registry.REGISTRY_PORT;

        if (host == null || host.length() == 0) {
            // If host is blank (as returned by "file:" URL in 1.0.2 used in
            // java.rmi.Naming), try to convert to real local host name so
            // that the RegistryImpl's checkAccess will not fail.
            try {
                host = java.net.InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                // If that failed, at least try "" (localhost) anyway...
                host = "";
            }
        }

//调用RegistryImpl_Stub的lookup方法时，看似本地调用，实则通过tcp连接发送消息到服务端
 public Remote lookup(String var1) throws AccessException, NotBoundException, RemoteException {
        try {
            RemoteCall var2 = super.ref.newCall(this, operations, 2, 4905912898345647071L);

            try {
                ObjectOutput var3 = var2.getOutputStream();
                var3.writeObject(var1);
            } catch (IOException var18) {
                throw new MarshalException("error marshalling arguments", var18);
            }

            super.ref.invoke(var2);

            Remote var23;
            try {
                ObjectInput var6 = var2.getInputStream();
                var23 = (Remote)var6.readObject();
            } catch (IOException var15) {
                throw new UnmarshalException("error unmarshalling return", var15);
            } catch (ClassNotFoundException var16) {
                throw new UnmarshalException("error unmarshalling return", var16);
            } finally {
                super.ref.done(var2);
            }

            return var23;
        } catch (RuntimeException var19) {
            throw var19;
        } catch (RemoteException var20) {
            throw var20;
        } catch (NotBoundException var21) {
            throw var21;
        } catch (Exception var22) {
            throw new UnexpectedException("undeclared checked exception", var22);
        }
    }
```

### 6.4.RMI 的优劣

1. 优势
   
    给分布计算的系统设计、编程都带来了极大的方便。只要按照RMI规则设计程序，可以不必再过问在RMI之下的网络细节了，如：TCP和Socket等等。任意两台计算机之间的通讯完全由RMI负责。调用远程计算机上的对象就像本地对象一样方便。
    
2. 劣势

    RMI对服务器的IP地址和端口依赖很紧密，但是在开发的时候不知道将来的服务器IP和端口如何，而客户端程序又依赖这个IP和端口。即客户端如何维护服务端地址和动态感知服务端地址变化的问题。
    
    另一局限性是，RMI是Java语言的远程调用，两端的程序语言必须是Java实现。



