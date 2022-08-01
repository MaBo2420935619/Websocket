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
import java.util.concurrent.CopyOnWriteArraySet;
//测试网站 http://www.websocket-test.com/
// wss://la23972002.goho.co//websocket/2/2
                        //ws://127.0.0.1:8080/websocket/2/2
@Slf4j
@ServerEndpoint(value = "/websocket/{chatroom}/{userId}")
@Component
public class WebSocketServer {
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
//        boolean login=false;
//        WebSocketServer loginUser=null;
//        for (WebSocketServer item : webSocketSet) {
//            try {
//                if (item.userId.equals(userId)) {
//                    login=true;
//                    loginUser=item;
//                   break;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        if (!login){
//
//        }
//        else {
//            try {
//                sendMessage("当前用户已经存在是否继续登录");
//            } catch (IOException e) {
//                log.error("websocket IO异常");
//            }
//           //旧用户离线
//            sendMessage(loginUser.session,"其他用户登录,你被挤下线");
//            onClose(loginUser.session ,loginUser.chatroom,loginUser.userId);
//            webSocketSet.add(this);
//            addOnlineCount();
//            try {
//                sendMessage("连接成功");
//            } catch (IOException e) {
//                log.error("websocket IO异常");
//            }
//        }



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
//                    WebSocketServer.sendChatroom(item.chatroom,jsonObject);//单机方式
                    websocketProducer.sendMsg(item.chatroom,item.userId,message);
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
