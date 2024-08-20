package com.OLP.auth.controller;


import com.OLP.auth.basic.entity.AuthPermission;
import com.OLP.auth.basic.entity.AuthRolePermission;
import com.OLP.auth.basic.entity.AuthUserRole;
import com.OLP.auth.basic.mapper.AuthPermissionDao;
import com.OLP.auth.basic.mapper.AuthRolePermissionDao;
import com.OLP.auth.basic.mapper.AuthUserRoleDao;
import com.OLP.auth.basic.service.AuthPermissionService;
import com.OLP.auth.basic.service.AuthRolePermissionService;
import com.OLP.auth.basic.service.AuthRoleService;
import com.OLP.auth.basic.service.AuthUserRoleService;
import com.OLP.common.entity.R;
import com.OLP.common.entity.SystemConstants;
import com.OLP.common.redis.RedisUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthUserRoleDao authUserRoleDao;

    private final AuthRolePermissionDao authRolePermissionDao;

    private final AuthPermissionDao authPermissionDao;

    private final RedisUtil redisUtil;


    @PostMapping("/init")
    public R<String> initRBAC (Long userId){
        AuthUserRole authUserRole = new AuthUserRole();
        authUserRole.setUserId(userId);
        List<AuthUserRole> RolesList = authUserRoleDao.queryAllByLimit(authUserRole);
        if (RolesList.size()>=1) {
            //在redis中设置key
            String rolekey = redisUtil.buildKey(SystemConstants.AUTH_ROLE_PREFIX, userId.toString());
            //去重
            RolesList = RolesList.stream().distinct().collect(Collectors.toList());
            redisUtil.set(rolekey, new Gson().toJson(RolesList));
        }
        List<Long> RoleIdList = RolesList.stream().map(AuthUserRole::getRoleId).collect(Collectors.toList());
        //权限id集合
        List<Long> permissionIds = new ArrayList<>();
        for (Long item : RoleIdList) {
            AuthRolePermission authRolePermission = new AuthRolePermission();
            authRolePermission.setRoleId(item);
            List<AuthRolePermission> authRolePermissions = authRolePermissionDao.queryAllByLimit(authRolePermission);
            List<Long> perimission = authRolePermissions
                    .stream().map(AuthRolePermission::getPermissionId).collect(Collectors.toList());
            if (perimission.size() == 1 && perimission!=null){
                permissionIds.add(perimission.get(0));
            }
            if (perimission.size() >1){
                permissionIds.addAll(perimission);
            }
        }

        if (permissionIds.size()==0){
            return R.error("没有权限");
        }
        //获取permission_key
        List<AuthPermission> perimissionList = permissionIds.stream().map(item -> {
            AuthPermission authPermission = authPermissionDao.queryById(item);
            return authPermission;
        }).collect(Collectors.toList());
        //在redis中设置key
        String permissionkey = redisUtil.buildKey(SystemConstants.AUTH_PERMISSION_PREFIX, userId.toString());
        redisUtil.set(permissionkey, new Gson().toJson(perimissionList));
        return R.success("RBAC初始化成功");

    }


}
