package com.OLP.common.pojo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Bookshelf {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private String author;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
