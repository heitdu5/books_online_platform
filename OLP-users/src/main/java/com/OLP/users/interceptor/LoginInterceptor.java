package com.OLP.users.interceptor;


import com.OLP.common.entity.LoginContextHolder;
import com.OLP.users.properties.JwtProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 *
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Resource
    private JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String jwt = request.getHeader("loginId");
        log.info("LoginInterceptor.preHandle:{}",request.getRequestURI());
        //1.判断令牌是否存在，如果不存在，则报401
        if (StringUtils.isBlank(jwt)){
            log.info("令牌为空");
            response.setStatus(401);
            return false;
        }
        //2.校验令牌
        try {
//                Claims claims = JwtUtil.parseJWT("mysecret", jwt);
//                Long id = claims.get("id", Long.class);
                LoginContextHolder.set("loginId", jwt);
                return true;
        }catch (Exception e){
            e.printStackTrace();
            log.info("令牌非法");
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        LoginContextHolder.remove();
    }

}
