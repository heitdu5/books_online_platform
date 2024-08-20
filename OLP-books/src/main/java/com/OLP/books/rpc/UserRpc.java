package com.OLP.books.rpc;


import com.OLP.api.UserFeignService;
import com.OLP.common.entity.R;
import com.OLP.common.pojo.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;

@Component
public class UserRpc {

    @Resource
    private  UserFeignService userFeignService;


    public List<User> FreindsList(@RequestParam(name = "username",required = false) String userName){
        R<List<User>> userList = userFeignService.FreindsList(userName);
        List<User> data = userList.getData();
        return data;
    }

    public User getById(@RequestParam("id") Long id){
        return userFeignService.getById(id);
    }



    public List<User> getListByCondition(@RequestParam("reactUsers")  List<String> reactUsers){
        return userFeignService.getListByCondition(reactUsers);
    }


    public User getOneByCondition(@RequestParam("name")  String name){
        return userFeignService.getOneByCondition(name);
    }

    @GetMapping("setproof")
    public void setproof(@RequestParam("s") String s){
        userFeignService.setproof(s);
    }

}
