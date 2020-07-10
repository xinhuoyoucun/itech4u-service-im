package cn.itech4u.service.im.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author by yuanlai
 * @Date 2020/7/8 10:45 上午
 * @Description: TODO
 * @Version 1.0
 */

@Component
@ServerEndpoint("/im")
public class WebSocketServer {
    private final static Logger log = LoggerFactory.getLogger(WebSocketServer.class);

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的WebSocket对象。
     */
    private static ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 接收token
     */
    private String token = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("网络异常,连接失败");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(token)) {
            webSocketMap.remove(token);
            //从set中删除
            subOnlineCount();
        }
        log.info("用户退出:{},当前在线人数为:{}", token, getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message) throws IOException {
        log.info("收到消息,报文:{}", message);
        //消息保存到数据库、redis
        if (StringUtils.isNotBlank(message)) {
            JSONObject jsonObject = JSON.parseObject(message);
            String type = jsonObject.getString("type");
            switch (type) {
                case "system":
                    log.info("系统消息");
                    break;
                case "user":
                    log.info("用户消息");
                    jsonObject = jsonObject.getJSONObject("msg");
                    //追加发送人(防止串改)
                    String fromUserId = this.token;
                    jsonObject.put("fromUserId", fromUserId);
//                    String toUserId=jsonObject.getString("toUserId");
                    String toUserId = fromUserId;
                    //传送给对应toUserId用户的websocket
                    if (StringUtils.isNotBlank(toUserId) && webSocketMap.containsKey(toUserId)) {
                        webSocketMap.get(toUserId).sendMessage(jsonObject.toJSONString());
                    } else {
                        log.error("请求的userId:" + toUserId + "不在该服务器上");
                        //否则不在这个服务器上，发送到mysql或者redis
                    }
                    break;
                case "login":
                    log.info("登录消息");
                    String token = jsonObject.getJSONObject("msg").getString("token");
                    this.token = token;
                    if (webSocketMap.containsKey(token)) {
                        webSocketMap.remove(token);
                        webSocketMap.put(token, this);
                        //加入set中
                    } else {
                        //加入set中
                        webSocketMap.put(token, this);
                        //在线数加1
                        addOnlineCount();
                    }
                    sendMessage("登录成功");
                    log.info("用户{}登录,当前在线人数为:{}", token, getOnlineCount());

                    break;
                case "keepAlive":
                    log.debug("心跳包");

                    break;
                default:
                    log.info("没有这样的消息类型");
            }
        }


    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("{}发送错误,原因:{}", this.token, error.getMessage());
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }


    /**
     * 发送消息
     */
    public static void sendInfo(String message, @PathParam("token") String token) throws IOException {
        log.info("发送消息到:{}，报文:{}", token, message);
        if (StringUtils.isNotBlank(token) && webSocketMap.containsKey(token)) {
            webSocketMap.get(token).sendMessage(message);
        } else {
            log.error("用户{},不在线！", token);
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

}