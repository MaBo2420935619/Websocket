# WebsocketWSS
connunica room
基于Spring-WebSocket +RocketMQ的分布式项目
利用rocketMQ广播实现消息订阅



注意，两个用户在同一个房间才可以互相聊天![在这里插入图片描述](https://img-blog.csdnimg.cn/cd9bf24b52684ddbad563c0920352d26.jpeg)

在线体验地址: http://47.103.194.1:8081/

# github下载地址
https://github.com/MaBo2420935619/Websocket
![在这里插入图片描述](https://img-blog.csdnimg.cn/b5079f2b671a4c959a55e8a465c490fd.png)

# 前言

>  HTTP 协议有一个缺陷：通信只能由客户端发起。 HTTP 协议做不到服务器主动向客户端推送信息。
> 
> 
> 这种单向请求的特点，注定了如果服务器有连续的状态变化，客户端要获知就非常麻烦。我们只能使用"轮询"：每隔一段时候，就发出一个询问，了解服务器有没有新的信息。
> 轮询的效率低，非常浪费资源（因为必须不停连接，或者 HTTP 连接始终打开）。因此，出现了 WebSocket。

## WebSocke
**WebSocket是一种在单个TCP连接上进行全双工通信的协议。WebSocket使得客户端和服务器之间的数据交换变得更加简单，允许服务端主动向客户端推送数据。
在WebSocket中，浏览器和服务器只需要完成一次握手，两者之间就直接可以创建持久性的连接，并进行双向数据传输。**
所以可通过Websocket实现网络在线聊天室的功能

# 源码解读

## 实现原理

当用户登录后，向房间1发送消息，服务器收到消息后，找到所有在房间1的用户，并且向这些用户转发这条消息。即可实现网络聊天室的功能。
## 单机
首先创建一个SpringBoot项目
## POM依赖

```java
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!--webSocket-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.83</version>
        </dependency>

        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-spring-boot-starter</artifactId>
            <version>2.1.1</version>
        </dependency>
```
## 创建configure

```java
package com.mabo.websocket;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


@Component
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }



}

```
## 编写Websocket Server

```java
package com.mabo.websocket;


import com.alibaba.fastjson.JSONObject;
import com.mabo.rockMQ.producer.WebsocketProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;
//测试网站 http://www.websocket-test.com/
// wss://la23972002.goho.co//websocket/2/2
                        //ws://127.0.0.1:8080/websocket/房间号/用户id
@Slf4j
@ServerEndpoint(value = "/websocket/{chatroom}/{userId}")
@Component
public class WebSocketServer {
    private static SimpleDateFormat sdf=new SimpleDateFormat("MM月dd日 HH:mm:ss");
    private  static  WebsocketProducer websocketProducer;

    @Autowired
    public void setWebsocketProducer(WebsocketProducer websocketProducer) {
        WebSocketServer.websocketProducer = websocketProducer;
    }

    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    public static int onlineCount = 0;
    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    public static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    public Session session;

    //接收参数中的用户ID
    public String userId;

    //接收用户中的平台类型
    public String chatroom;


    /**
     * 连接建立成功调用的方法
     * 接收url中的参数
     */
    @OnOpen
    public void onOpen(Session session,@PathParam("chatroom") String chatroom, @PathParam("userId") String userId) throws IOException {
        log.info("有新连接加入！  userId==== " + userId + "  chatroom==== " + chatroom);
        this.session = session;
        this.userId = userId;
        this.chatroom = chatroom;
        log.info("用户名  userId==== " + userId + "  chatroom==== " + chatroom+ "  session==== " + session.getId());
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("websocket IO异常");
        }

    }

    /**
     * 连接关闭调用的方法
     * 如果服务端主动关闭当前连接,客户端感知不到
     *需要调用http请求通知客户端已经下线
     */
    @OnClose
    public void onClose(Session session, @PathParam("chatroom") String chatroom, @PathParam("userId") String userId) throws IOException {
        boolean close=false;
        WebSocketServer closeUser=null;
        for (WebSocketServer item : webSocketSet) {
            try {
                if (item.userId.equals(userId)) {
                    close=true;
                    closeUser=item;
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (close){
//            sendMessage(session,userId+"用户离线");
            webSocketSet.remove(closeUser);  //从set中删除
            subOnlineCount();           //在线数减1
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        for (WebSocketServer item : webSocketSet) {
            try {
                if (item.session.equals(session)) {
                    log.info( "用户 "+item.userId+" 向房间 "+item.chatroom+" 发送消息: "+message);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("sender",item.userId);
                    jsonObject.put("msg",message);
                    jsonObject.put("date",sdf.format(new Date()));
                    WebSocketServer.sendChatroom(item.chatroom,jsonObject);//单机方式
                    websocketProducer.sendMsg(item.chatroom,item.userId,message);//分布式部署
                }
            } catch (Exception e) {
               e.printStackTrace();
            }
        }

    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误" + error);
        error.printStackTrace();
    }


    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    public void sendMessage(Session session,String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }
    /**
     * 私发
     *
     * @param message
     * @throws IOException
     */
    public static void sendInfo(Long userId, String message) throws IOException {
        for (WebSocketServer item : webSocketSet) {
            try {
                if (item.userId.equals(userId)) {
                    item.sendMessage(message);
                }
            } catch (IOException e) {
                break;
            }
        }
    }

    /**
     * 发送到聊天室
     */
    public static void sendChatroom(String chatroom, JSONObject json) throws IOException {
        for (WebSocketServer item : webSocketSet) {
            try {
                if (item.chatroom.equals(chatroom)) {
                    item.sendMessage(json.toJSONString());
                }
            } catch (IOException e) {
                continue;
            }
        }
    }
    /**
     * 群发自定义消息
     */
    public static void sendInfos(String message) throws IOException {
        log.info(message);
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
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
        log.info("有新连接加入！当前在线人数为" + getOnlineCount() );
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
        log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
    }
}

```
## 单机如何测试
到这里就可以启动websocket服务器进行测试了，
但是需要客户端进行测试
下载git前端文件
https://github.com/MaBo2420935619/Websocket/tree/main/src/main/resources/static
直接打开即可进行测试
![在这里插入图片描述](https://img-blog.csdnimg.cn/0a55c38a5b08452294f1644a7cce9565.png)


## 分布式的问题

> Websocket识别用户并且发送消息时根据用户的session来进行发送的，其他jvm中的websocket时无法获取的，所以需要依赖中间件来解决这个问题

分布式下的websocket消息无法依靠websocket实现消息发送，
**该demo使用RocketMQ的广播消息模式，对所有服务器发送消息，如果当前服务器连接了该用户则该服务器对用户发送消息，通过这种方式可以实现分布式部署情况下实现网络聊天室**

**消费者代码**

```java
package com.mabo.rockMQ.listener;

import com.alibaba.fastjson.JSONObject;
import com.mabo.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(consumerGroup = "websocket", topic = "websocket",messageModel = MessageModel.BROADCASTING)
//MessageModel 设置为广播模式BROADCASTING
public class WebsocketConsumer implements RocketMQListener<String> {
    private static SimpleDateFormat sdf=new SimpleDateFormat("MM月dd日 HH:mm:ss");
    @Override
    public void onMessage(String s) {
        JSONObject parse = (JSONObject) JSONObject.parse(s);
        //消息类型      1发送消息,2关闭客户端
        String type = parse.getString("type");
        //data   包括
        //userID  roomId
        log.info("接收到消息，开始消费..message:" + s);
        if (type.equals("1")){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sender",parse.getString("userId"));
            jsonObject.put("msg",parse.getString("msg"));
            jsonObject.put("date",sdf.format(new Date()));
            log.info( "用户 "+parse.getString("userId")+" 向房间 "+parse.getString("classRoom")+" 发送消息: "+parse.getString("msg"));
            try {
                WebSocketServer.sendChatroom(parse.getString("classRoom"),jsonObject);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}

```
**生产者代码**

```java
package com.mabo.rockMQ.producer;

import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class WebsocketProducer {
    private static final Logger log = LoggerFactory.getLogger(WebsocketProducer.class);
    @Autowired
    RocketMQTemplate rocketMQTemplate;

    public void sendMsg(String classRoom,String userId,String msg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type",1);
        jsonObject.put("classRoom",classRoom);
        jsonObject.put("userId",userId);
        jsonObject.put("msg",msg);
        rocketMQTemplate.convertAndSend("websocket", jsonObject.toJSONString());
        log.info("send message success"+jsonObject);

    }

    public void closeUser(String classRoom,String userId,String msg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type",2);
        jsonObject.put("classRoom",classRoom);
        jsonObject.put("userId",userId);
        jsonObject.put("msg",msg);
        rocketMQTemplate.convertAndSend("websocket", jsonObject.toJSONString());
        log.info("send message success"+jsonObject);
    }
}


```
**配置文件**

```java
server:
  port: 8080
rocketmq:
  nameServer: 127.0.0.1:9876
  producer:
    group: maboGroup
    topicName: websocket
```

# 需要改进的地方

> 单机下所有的用户信息都是存储在static修饰的静态变量中，每一次消息发送都需要所有服务器通过该变量轮询服务器中是否存在用户，造成了效率低下。

可以采用Redis缓存，用户的登录了哪个服务器存储到缓存中，每次发送消息只需要发送给缓存中的服务器（或者添加服务器标记），可以提高消息发送的效率。


