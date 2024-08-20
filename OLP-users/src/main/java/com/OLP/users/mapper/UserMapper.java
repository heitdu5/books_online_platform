package com.OLP.users.mapper;
import com.OLP.common.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User>{
    /**
     * 根据用户名查询用户信息
     * @param username
     * @return
     */
    @Select("select * from user where username = #{username}")
    public User findByUsername(String username);

    /**
     * 上传头像
     * @param s
     */
    @Update("update user set avatarurl = #{s} where user_id = #{id}")
    void setAvatar(String s,Long id);

    @Update("update user set proofurl = #{s} where user_id = #{id}")
    void setproof(String s, Long id);
}
