package com.OLP.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionVo {
    private String username;
    private String avatarurl;
    private LocalDateTime sendTime;
    private  String message;
    private Boolean isLeft;
}
