package com.OLP.api;

import com.OLP.common.entity.R;
import com.OLP.common.pojo.User;
import com.OLP.fallback.UserFeignFallbackFactory;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@FeignClient(value = "yh-olp-users",fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignService {


    @GetMapping(value = "/user/getFreindsList",consumes = "application/json")
    public R<List<User>> FreindsList(@RequestParam(name = "username",required = false) String username);

    @GetMapping("/user/getById")
    public User getById(@RequestParam("id") Long id);

    @GetMapping("/user/getListByCondition")
    public List<User> getListByCondition(@RequestParam("reactUsers")  List<String> reactUsers);

    @GetMapping("/user/getOneByCondition")
    public User getOneByCondition(@RequestParam("name")  String name);

    @GetMapping("setproof")
    public void setproof(@RequestParam("s") String s);




}
