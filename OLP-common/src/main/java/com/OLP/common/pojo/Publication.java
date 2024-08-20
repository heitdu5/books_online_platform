package com.OLP.common.pojo;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Publication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private Long bookId;
    private Integer chapter;
    private String chTitle;
    private String status;
    private String chContent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
