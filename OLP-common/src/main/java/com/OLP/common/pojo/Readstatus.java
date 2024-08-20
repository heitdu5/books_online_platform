package com.OLP.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Readstatus {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private Long messageId;
    private Boolean isRead;
}
