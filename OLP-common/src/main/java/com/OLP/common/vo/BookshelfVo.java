package com.OLP.common.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookshelfVo {
    private Long bookId;
    private String bookName;
    private String author;
    private String coverUrl;
    private LocalDateTime createTime;
    private String description;
    private String category;
    private String status;
    private Integer Clicks;
}

