package com.OLP.ws.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
public class WebsocketConfig {

    //Bean注解将方法返回值交给spring管理
    //注入ServerEndpointExcepter，用来自动注册使用@ServerEndpoint注解
    @Bean
    public ServerEndpointExporter serverEndpointExporter(){
        return new ServerEndpointExporter();
    }

}
