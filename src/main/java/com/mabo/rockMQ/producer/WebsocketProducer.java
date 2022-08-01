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

