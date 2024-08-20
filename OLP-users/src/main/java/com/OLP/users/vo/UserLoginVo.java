package com.OLP.users.vo;

import lombok.Data;

@Data
public class UserLoginVo {
    private Long userId;

    private String username;

    private String token;//令牌

    private String avatarurl;

    private Boolean isAuthor;

    private Boolean status;

    private String rights;

    public UserLoginVo(Long userId, String username, String token,String avatarurl,Boolean isAuthor,Boolean status,String rights ) {
        this.userId = userId;
        this.username = username;
        this.token = token;
        this.avatarurl = avatarurl;
        this.isAuthor = isAuthor;
        this.status = status;
        this.rights = rights;
    }
}
