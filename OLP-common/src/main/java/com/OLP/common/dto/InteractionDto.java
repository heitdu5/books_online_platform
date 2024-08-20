package com.OLP.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InteractionDto {
    private String username;
    private String avatarurl;
    private LocalDateTime sendTime;
    private String message;
    private Boolean isLeft;
    private Boolean isRead;
}
