package com.OLP.common.entity;

/**
 * redis常数key类
 */
public class SystemConstants {

    public static final String LOGIN_PREFIX = "loginCode";//登录token

    public static final String LOGIN_VALIDATECODE = "login_validatecode: ";//登录验证码

    public static final String USER_SEND_FREQUENCY = "user_send_number: ";//用户每天发送验证码的次数

    public static final String PAGE_SELECT_NONAME = "books_select_NoName: ";//默认无名字，无日期查询

    public static final String AUTH_PERMISSION_PREFIX = "auth.permission";

    public static final String AUTH_ROLE_PREFIX = "auth.role";

}
