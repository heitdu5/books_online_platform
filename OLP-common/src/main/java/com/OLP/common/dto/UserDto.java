package com.OLP.common.dto;


import com.OLP.common.pojo.User;
import lombok.Data;

@Data
public class UserDto extends User {
    private Boolean banOrRe;//true为注册为作者，false为封禁
}
