package com.OLP.common.util;

import com.OLP.common.entity.LoginContextHolder;

public class LoginUtil {

    public static Long getLoginId(){
        return LoginContextHolder.getLoginId();
    }
}
