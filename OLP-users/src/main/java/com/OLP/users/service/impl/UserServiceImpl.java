package com.OLP.users.service.impl;
import com.OLP.common.pojo.User;
import com.OLP.common.redis.RedisUtil;
import com.OLP.common.exception.DataException;
import com.OLP.common.exception.ServiceException;
import com.OLP.users.mapper.UserMapper;
import com.OLP.users.service.UserService;
import com.OLP.common.util.LoginUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;



@Service
@Slf4j
@AllArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserMapper userMapper;

    private final RedisUtil redisUtil;

    @Override
    public User login(Map<String, String> formData) {
        //1.先查询该员工信息
        String username = formData.get("account");
        User user = userMapper.findByUsername(username);
        //2.判断该员工是否存在，不存在
        if (user==null){
            log.info("查询到的用户信息为空..");
            throw new DataException("用户不存在");
        }
        //3.校验密码的正确性
        String password = formData.get("password");
        if (!password.equals(user.getPassword())){
            log.info("密码错误");
            throw new ServiceException("密码比对错误");
        }
        //4.判断用户是否被禁用
        if (user.getStatus()==false){
            log.info("该账号已被禁用");
            throw new ServiceException("该账号已被禁用");
        }
        return user;
    }

    @Override
    public void setAvatar(String s) {
        Long id = LoginUtil.getLoginId();
        //处理路径格式
        String pictureUrl = s.replace("\\","/").replaceFirst(":",":/");
        userMapper.setAvatar(pictureUrl,id);
    }

}
