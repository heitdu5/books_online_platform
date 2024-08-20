package com.OLP.users.service;
import com.OLP.common.pojo.User;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Transactional
public interface UserService extends IService<User> {

    /**
     * 用户登录
     * @param formData
     * @return
     */
    User login(Map<String, String> formData);

    /**
     * 上传头像
     * @param s
     */
    void setAvatar(String s);

}
