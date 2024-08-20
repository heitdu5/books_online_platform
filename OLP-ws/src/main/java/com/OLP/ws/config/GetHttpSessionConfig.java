package com.OLP.ws.config;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;

public class GetHttpSessionConfig extends ServerEndpointConfig.Configurator {



    /**
     * 过滤，权限校验
     */
    @Override
    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        //获取HttpSession对象
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        Map<String, List<String>> parameterMap = request.getParameterMap();
        String username = parameterMap.get("username").get(0);
        httpSession.setAttribute("username",username);
        //将httpSession对象保存起来,方便那边拿到
        sec.getUserProperties().put(HttpSession.class.getName(),httpSession);
    }
}

