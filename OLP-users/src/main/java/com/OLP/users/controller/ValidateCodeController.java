package com.OLP.users.controller;
import cn.dsna.util.images.ValidateCode;
import com.OLP.common.entity.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ValidateCodeController {
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/checkCode")
    public void ValidateCode(HttpServletResponse response, HttpServletRequest request) throws IOException {
        //声明变量存储验证码宽度，高度，验证码的字母个数，验证码的干扰线的条数
        int width=120;
        int height=40;
        int count=4;
        int lineCount=20;
        //创建验证码对象
        ValidateCode vc=new ValidateCode(width, height, count, lineCount);
        //获取验证码
        String code=vc.getCode();
        System.out.println("生成的验证码："+code);
        //将生成的验证码存到Session会话
        HttpSession session = request.getSession(true);
        session.setAttribute("checkcode", code);
        //redis缓存验证码
        redisTemplate.opsForValue().set(SystemConstants.LOGIN_VALIDATECODE,code,30, TimeUnit.MINUTES);
        //将生成的验证码返回给客户端
        vc.write(response.getOutputStream());

    }


}
