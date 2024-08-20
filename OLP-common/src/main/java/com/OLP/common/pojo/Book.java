package com.OLP.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class Book implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long bookId;
    private String bookName;
    private String author;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate publicationDate;
    private String description;
    private String coverUrl;//封面路径
    private String category;
    private String status;
    private Integer Clicks;
}
