package com.hui.myteam.utils;

import cn.hutool.json.JSONUtil;
import com.hui.myteam.model.dto.SocketMsg;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * WebSocket 连接测试
 */
@Component
@ServerEndpoint("/web-socket/{userName}")
public class WebSocketUtil {

    private String userName;

    private Session session;

    /** 固定前缀  */
    private static final String USER_NAME_PREFIX = "user_name_";

    /**
     * 用来存放每个客户端对应的MyWebSocket对象。
     **/
    private static CopyOnWriteArraySet<WebSocketUtil> webSocketSet = new CopyOnWriteArraySet<>();

    /**
     * 存放Session集合，方便推送消息 （javax.websocket.Session）
     */
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 私聊：向指定客户端推送消息
     */
    public synchronized static void privateMessage(SocketMsg socketMsg) {
        //接收消息的用户
        Session receiveUser = sessionMap.get(USER_NAME_PREFIX + socketMsg.getReceiveUser());
        //发送给接收者
        if(receiveUser != null){
            //发送给接收者
            System.out.println(socketMsg.getSendOutUser()+" 向 "+socketMsg.getReceiveUser()+" 发送了一条消息："+socketMsg.getMsg());
            receiveUser.getAsyncRemote().sendText(socketMsg.getSendOutUser()+"："+socketMsg.getMsg());
        }else{
            //发送消息的用户
            System.out.println(socketMsg.getSendOutUser()+" 私聊的用户 "+socketMsg.getReceiveUser()+" 不在线或者输入的用户名不对");
            Session sendOutUser = sessionMap.get(USER_NAME_PREFIX + socketMsg.getSendOutUser());
            //将系统提示推送给发送者
            sendOutUser.getAsyncRemote().sendText("系统消息：对方不在线或者您输入的用户名不对");
        }
    }

    /**
     * 群聊：公开聊天记录
     * @param userName 发送者的用户名称（当前用户）
     * @param message 发送的消息
     * @param flag 用来标识 是否要将消息推送给 当前用户
     */
    public synchronized static void publicMessage(String userName,String message,boolean flag) {
        for (WebSocketUtil item : webSocketSet) {
            Session session = item.session;
            if (flag){
                session.getAsyncRemote().sendText(message);
            }else {
                //获取发送这条消息的用户
                Session currentUser = sessionMap.get(USER_NAME_PREFIX + userName);
                //消息不用推送到发送者的客户端
                if (!session.getId().equals(currentUser.getId())){
                    session.getAsyncRemote().sendText(message);
                }
            }
        }
        System.out.println("公共频道接收了一条消息："+message);
    }

    /**
     * 监听：连接成功
     * @param session
     * @param userName 连接的用户名
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userName") String userName) {
        this.userName = userName;
        this.session = session;
        sessionMap.put(USER_NAME_PREFIX + userName, session);
        webSocketSet.add(this);
        //在线数加1
        String tips = userName+" 加入聊天室。当前聊天室人数为" + webSocketSet.size();
        System.out.println(tips);
        publicMessage(userName,tips,true);
    }

    /**
     * 监听：收到客户端发送的消息
     * @param message 发送的信息（json格式，里面是 SocketMsg 的信息）
     */
    @OnMessage
    public void onMessage(String message) {
        if (JSONUtil.isTypeJSONObject(message)) {
            SocketMsg socketMsg = JSONUtil.toBean(message, SocketMsg.class);
            if(socketMsg.getType() == 1){
                //单聊，需要找到发送者和接受者
                privateMessage(socketMsg);
            }else{
                //群发消息
                publicMessage(socketMsg.getSendOutUser(),socketMsg.getSendOutUser()+": "+socketMsg.getMsg(),false);
            }
        }
    }

    /**
     * 监听: 连接关闭
     */
    @OnClose
    public void onClose() {
        if (sessionMap.containsKey(USER_NAME_PREFIX + userName)) {
            //连接关闭后，将此websocket从set中删除
            sessionMap.remove(USER_NAME_PREFIX + userName);
            webSocketSet.remove(this);
        }
        String tips = userName+" 退出聊天室。当前聊天室人数为" + webSocketSet.size();
        System.out.println(tips);
        publicMessage(userName,tips,true);
    }

    /**
     * 监听：发生异常
     * @param error
     */
    @OnError
    public void onError(Throwable error) {
        System.out.println("userName为：" + userName + "，发生错误：" + error.getMessage());
        error.printStackTrace();
    }

}
