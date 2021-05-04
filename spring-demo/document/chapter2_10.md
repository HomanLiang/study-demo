[toc]



# Spring Boot 集成

## 1.Spring Boot 集成 WebSocket

在一次项目开发中，使用到了Netty 网络应用框架，以及 MQTT 进行消息数据的收发，这其中需要后台来将获取到的消息主动推送给前端，于是就使用到了MQTT，特此记录一下。

### 1.1.什么是websocket？

WebSocket 协议是基于 TCP 的一种新的网络协议。

它实现了客户端与服务器之间的全双工通信，学过计算机网络都知道，既然是全双工，就说明了**服务器可以主动发送信息给客户端**。

这与我们的推送技术或者是多人在线聊天的功能不谋而合。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103235.png)

**为什么不使用 HTTP 协议呢？**

这是因为HTTP是单工通信，通信只能由客户端发起，客户端请求一下，服务器处理一下，这就太麻烦了。

于是 websocket 应运而生。

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103239.png)

下面我们就直接开始使用 Spring Boot 开始整合。以下案例都在我自己的电脑上测试成功，你可以根据自己的功能进行修改即可。

我的项目结构如下：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103242.png)

### 1.2.使用步骤

#### 1.2.1.添加依赖

Maven 依赖：

```
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-websocket</artifactId>  
</dependency> 
```

#### 1.2.2.启用Springboot对WebSocket的支持

启用 WebSocket 的支持也是很简单，几句代码搞定。

```
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
/**
 * @ Auther: 马超伟
 * @ Date: 2020/06/16/14:35
 * @ Description: 开启WebSocket支持
 */
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
```

#### 1.2.3.核心配置：WebSocketServer

因为 Web Socket 是类似客户端服务端的形式(采用 ws 协议)，那么这里的 WebSocketServer 其实就相当于一个 ws 协议的 Controller。

@ServerEndpoint 注解这是一个类层次的注解，它的功能主要是将目前的类定义成一个 websocket 服务器端。注解的值将被用于监听用户连接的终端访问 URL 地址，客户端可以通过这个 URL 来连接到 WebSocket 服务器端

再新建一个 ConcurrentHashMap webSocketMap 用于接收当前 userId 的 WebSocket，方便传递之间对 userId 进行推送消息。

下面是具体业务代码：

```
package cc.mrbird.febs.external.webScoket;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created with IntelliJ IDEA.
 * @ Auther: 马超伟
 * @ Date: 2020/06/16/14:35
 * @ Description:
 * @ ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 */
@Component
@Slf4j
@Service
@ServerEndpoint("/api/websocket/{sid}")
public class WebSocketServer {
    //当前在线连接数
    private static int onlineCount = 0;
    //存放每个客户端对应的MyWebSocket对象
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

    private Session session;

    //接收sid
    private String sid = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        this.session = session;
        webSocketSet.add(this);     //加入set中
        this.sid = sid;
        addOnlineCount();           //在线数加1
        try {
            sendMessage("conn_success");
            log.info("有新窗口开始监听:" + sid + ",当前在线人数为:" + getOnlineCount());
        } catch (IOException e) {
            log.error("websocket IO Exception");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
       
        log.info("释放的sid为："+sid);
        
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());

    }

    /**
     * 收到客户端消息后调用的方法
     * @ Param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自窗口" + sid + "的信息:" + message);
        //群发消息
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @ Param session
     * @ Param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message, @PathParam("sid") String sid) throws IOException {
        log.info("推送消息到窗口" + sid + "，推送内容:" + message);

        for (WebSocketServer item : webSocketSet) {
            try {
                //为null则全部推送
                if (sid == null) {
//                    item.sendMessage(message);
                } else if (item.sid.equals(sid)) {
                    item.sendMessage(message);
                }
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }

    public static CopyOnWriteArraySet<WebSocketServer> getWebSocketSet() {
        return webSocketSet;
    }
}
```

#### 1.2.4.测试Controller

```
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @ Auther: 马超伟
 * @ Date: 2020/06/16/14:38
 * @ Description:
 */
@Controller("web_Scoket_system")
@RequestMapping("/api/socket")
public class SystemController {
    //页面请求
    @GetMapping("/index/{userId}")
    public ModelAndView socket(@PathVariable String userId) {
        ModelAndView mav = new ModelAndView("/socket1");
        mav.addObject("userId", userId);
        return mav;
    }

    //推送数据接口
    @ResponseBody
    @RequestMapping("/socket/push/{cid}")
    public Map pushToWeb(@PathVariable String cid, String message) {
        Map<String,Object> result = new HashMap<>();
        try {
            WebSocketServer.sendInfo(message, cid);
            result.put("code", cid);
            result.put("msg", message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
```

#### 1.2.5.测试页面index.html

```
<!DOCTYPE html>
<html>

 <head>
  <meta charset="utf-8">
  <title>Java 后端 WebSocket 的 Tomcat 实现</title>
  <script type="text/javascript" src="js/jquery.min.js"></script>
 </head>

 <body>
  <div id="main" style="width: 1200px;height:800px;"></div>
  Welcome<br/><input id="text" type="text" />
  <button onclick="send()">发送消息</button>
  <hr/>
  <button onclick="closeWebSocket()">关闭WebSocket连接</button>
  <hr/>
  <div id="message"></div>
 </body>
 <script type="text/javascript">
  var websocket = null;
  //判断当前浏览器是否支持WebSocket
  if('WebSocket' in window) {
   websocket = new WebSocket("ws://192.168.100.196:8082/api/websocket/100");
  } else {
   alert('当前浏览器 Not support websocket')
  }

  //连接发生错误回调方法
  websocket.onerror = function() {
   setMessageInnerHTML("WebSocket连接发生错误");
  };

  //连接成功建立回调方法
  websocket.onopen = function() {
   setMessageInnerHTML("WebSocket连接成功");
  }
  var U01data, Uidata, Usdata
  //接收消息回调方法
  websocket.onmessage = function(event) {
   console.log(event);
   setMessageInnerHTML(event);
   setechart()
  }

  //连接关闭回调方法
  websocket.onclose = function() {
   setMessageInnerHTML("WebSocket连接关闭");
  }

  //监听窗口关闭事件
  window.onbeforeunload = function() {
   closeWebSocket();
  }

  //将消息显示在网页上
  function setMessageInnerHTML(innerHTML) {
   document.getElementById('message').innerHTML += innerHTML + '<br/>';
  }

  //关闭WebSocket连接
  function closeWebSocket() {
   websocket.close();
  }

  //发送消息
  function send() {
   var message = document.getElementById('text').value;
   websocket.send('{"msg":"' + message + '"}');
   setMessageInnerHTML(message + "&#13;");
  }
 </script>

</html>
```

#### 1.2.6.结果展示

后台：如果有连接请求

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103258.png)

前台显示：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103301.png)

### 1.3.总结

这中间我遇到一个问题，就是说 WebSocket 启动的时候优先于 spring 容器，从而导致在 WebSocketServer 中调用业务Service会报空指针异常。

所以需要在 WebSocketServer 中将所需要用到的 service 给静态初始化一下：

如图所示：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103304.png)

还需要做如下配置：

![图片](https://homan-blog.oss-cn-beijing.aliyuncs.com/study-demo/spring-demo/20210504103307.png)











