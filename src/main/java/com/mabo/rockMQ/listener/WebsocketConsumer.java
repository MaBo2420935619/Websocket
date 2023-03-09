//package com.mabo.rockMQ.listener;
//
//import com.alibaba.fastjson.JSONObject;
//import com.mabo.websocket.WebSocketServer;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.spring.annotation.MessageModel;
//import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
//import org.apache.rocketmq.spring.core.RocketMQListener;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
//@Slf4j
//@Component
//@RocketMQMessageListener(consumerGroup = "websocket", topic = "websocket",messageModel = MessageModel.BROADCASTING)
////MessageModel 设置为广播模式BROADCASTING
//public class WebsocketConsumer implements RocketMQListener<String> {
//    private static SimpleDateFormat sdf=new SimpleDateFormat("MM月dd日 HH:mm:ss");
//    @Override
//    public void onMessage(String s) {
//        JSONObject parse = (JSONObject) JSONObject.parse(s);
//        //消息类型      1发送消息,2关闭客户端
//        String type = parse.getString("type");
//        //data   包括
//        //userID  roomId
//        log.info("接收到消息，开始消费..message:" + s);
//        if (type.equals("1")){
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("sender",parse.getString("userId"));
//            jsonObject.put("msg",parse.getString("msg"));
//            jsonObject.put("date",sdf.format(new Date()));
//            log.info( "用户 "+parse.getString("userId")+" 向房间 "+parse.getString("classRoom")+" 发送消息: "+parse.getString("msg"));
//            try {
//                WebSocketServer.sendChatroom(parse.getString("classRoom"),jsonObject);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//}
