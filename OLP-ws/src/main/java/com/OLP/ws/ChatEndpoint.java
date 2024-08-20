package com.OLP.ws;


import com.OLP.common.pojo.Interaction;
import com.OLP.common.pojo.Readstatus;
import com.OLP.ws.config.GetHttpSessionConfig;
import com.OLP.ws.pojo.Message;
import com.OLP.ws.service.InteractionService;
import com.OLP.ws.service.ReadstatusService;
import com.OLP.ws.util.MessageUtils;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息格式
 * 客户端-->服务端
 * {"toName":"张三","message":"你好"}
 *
 * 服务端-->客户端
 * 1.系统消息格式:{"system":true,"fromName":null,"message":["李四","王五"]}
 * 2.推送给某一个用户的消息格式:{"system":false,"fromName":"张三","message":"你好"}
 *
 */
@ServerEndpoint(value = "/Userchat",configurator = GetHttpSessionConfig.class)
@Component
@Slf4j
public class ChatEndpoint {
    //用static对于所有实例都使用同一个map，final防止重新赋值,concurrent是线程安全的map
    private static final Map<String,Session> onlineUsers = new ConcurrentHashMap<>();

    private HttpSession httpSession;

    private Long id;
    private String username;

//    @Autowired
//    UserService userServiceAuto; //注入的对象

    @Autowired
    private InteractionService interactionServiceAuto;//注入的对象

    @Autowired
    private ReadstatusService readstatusServiceAuto;//注入的对象

//    private static UserService userService;  //静态对象

    private static InteractionService interactionService;

    private static ReadstatusService readstatusService;

    @PostConstruct
    public void init(){
        //将注入的对象交给静态对象管理
        interactionService = this.interactionServiceAuto;
        readstatusService = this.readstatusServiceAuto;
    }


    /**
     * 建立websocket连接后，被调用
     * @param session
     */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        //1.将session进行保存
        this.httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
        String username = (String) this.httpSession.getAttribute("username");
        this.username = username;
        onlineUsers.put(username,session);
        //2.广播消息,需要将登录的所有用户推送给所有用户
        String message = MessageUtils.getMessage(true, null, getAllclinet());
        bradcastAllUsers(message);
    }


    public Set getAllclinet(){
        //得到所有键
        Set<String> set = onlineUsers.keySet();
        return set;
    }



    /**
     * 广播消息的内部方法
     * @param message
     */
    private void bradcastAllUsers(String message){
        try {
            //遍历map集合
            Set<Map.Entry<String, Session>> entries = onlineUsers.entrySet();
            for (Map.Entry<String, Session> entry : entries) {
                //获取所有session对象

                Session session = entry.getValue();
                //发送消息
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e){
            //记录日志
        }

    }


    /**
     * 浏览器发送消息到服务端，该方法会调用
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {

        try {

            //将消息推送给指定的用户
            Message msg = JSON.parseObject(message, Message.class);
            //获取消息接收方的用户名
            String toName = msg.getToName();
            String mess = msg.getMessage();
            //获取消息接收方用户对应的session对象
            Session session = onlineUsers.get(toName);
            //心跳机制
            if (message.equals("ping")) {
                this.sendMessage("pong", session);
            }
            String finalmessage = MessageUtils.getMessage(false, this.username, mess);

            //存入数据库的实体
            Interaction interaction = new Interaction();
            //保存历史记录
            interaction.setSponsorName(this.username);
            interaction.setAnswerName(toName);
            interaction.setContent(mess);
            interaction.setSendTime(LocalDateTime.now());
            interactionService.save(interaction);
            //设置已读未读信息
            //1.先设置自己的
            LambdaQueryWrapper<Interaction> intlam = new LambdaQueryWrapper<>();

            Readstatus readstatus = new Readstatus();
            //很神奇的现象，上面save之后interaction自动跟随数据库也增加了id
            readstatus.setMessageId(interaction.getInteractId());
            readstatus.setUsername(this.username);
            readstatus.setIsRead(true);
            readstatusService.save(readstatus);
            //2.再设置对方的
            Readstatus readstatus2 = new Readstatus();

            //发送消息，根据在线状态设置已读/未读
            if (session!=null){
                readstatus2.setIsRead(true);
                session.getBasicRemote().sendText(finalmessage);
            }

            else {
                readstatus2.setIsRead(false);//先将对方对该消息设置为未读状态
                log.error("该用户未上线,存为历史记录");
            }
            readstatus2.setMessageId(interaction.getInteractId());
            readstatus2.setUsername(toName);
            readstatusService.save(readstatus2);
        } catch (IOException e) {
            //记录日志
            e.printStackTrace();
        }

    }

    /**
     * 断开wenbsocket 连接时被调用
     * @param session
     */
    @OnClose
    public void onClose(Session session) {
        //1.从onlineUsers中剔除当前用户session对象
//        Long id = LoginUtil.getLoginId();
//        String username = userService.getById(id).getUsername();
        onlineUsers.remove(this.username);
        //2.通知其他所有用户，当前用户下线了
    }

    @OnError
    public void onError(Session s ,Throwable error){
        System.out.println("ws连接错误");
        error.printStackTrace();
    }


    /**
     * 指定发送消息
     * @param message
     */
    public void sendMessage(String message,Session session){
        log.info("服务端给客户端[{}]发送消息:{}", this.username, message);
        try {
            session.getBasicRemote().sendText(message);
        }catch (IOException e){
            log.error("{}发送消息发生异常，异常原因{}", this.username, message);
        }
    }
}
