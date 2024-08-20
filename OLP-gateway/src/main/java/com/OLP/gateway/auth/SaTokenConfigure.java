package com.OLP.gateway.auth;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * [Sa-Token 权限认证] 配置类
 */
@Configuration
public class SaTokenConfigure{


    // 注册 Sa-Token全局过滤器 
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
            // 拦截地址 
            .addInclude("/**")    /* 拦截全部path */

            // 开放地址 
            .addExclude("/users/user/info")
            // 鉴权方法：每次访问进入 
            .setAuth(obj -> {
                System.out.println("--------- 请求进入了拦截器，访问的 path 是：" + SaHolder.getRequest().getRequestPath());
                SaRouter.match("/books/**", r ->StpUtil.checkLogin());
                SaRouter.match("/oss/**", r ->StpUtil.checkLogin());
                SaRouter.match("/ws/**", r ->StpUtil.checkLogin());
                SaRouter.match("/users/**", "/users/user/login",r -> StpUtil.checkLogin());
                SaRouter.match("/author/publish/addBook", r -> StpUtil.checkPermission("author:addBook"));

                // 登录校验 -- 拦截所有路由，并排除/user/doLogin 用于开放登录 
//                SaRouter.match("/auth/**", "/auth/user/doLogin", r -> StpUtil.checkRole("admin"));
                // 权限认证 -- 不同模块, 校验不同权限
//                SaRouter.match("/oss/**", r -> StpUtil.checkLogin());
//                SaRouter.match("/subject/subject/add", r -> StpUtil.checkPermission("subject:add"));
//                SaRouter.match("/subject/**", r -> StpUtil.checkLogin());


            })

                // 异常处理函数：每次认证函数发生异常时执行此函数
                .setError(e -> {
                    return SaResult.error(e.getMessage());
                })

                // 前置函数：在每次认证函数之前执行
                .setBeforeAuth(obj -> {
                    SaHolder.getResponse()

                            // ---------- 设置跨域响应头 ----------
                            // 允许指定域访问跨域资源
                            .setHeader("Access-Control-Allow-Origin", "http://localhost:3001")
                            // 允许所有请求方式
                            .setHeader("Access-Control-Allow-Methods", "*")
                            // 允许的header参数
                            .setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept,satoken,loginId")
                            .setHeader("Access-Control-Allow-Methods","GET, POST, PUT, DELETE, OPTIONS")
                            .setHeader("Access-Control-Allow-Credentials","true")
                            // 有效时间
                            .setHeader("Access-Control-Max-Age", "3600")
                    ;

                    // 如果是预检请求，则立即返回到前端
                    SaRouter.match(SaHttpMethod.OPTIONS)
                            .free(r -> System.out.println("--------OPTIONS预检请求，不做处理"))
                            .back();
                })
                ;



    }
}
