package com.OLP.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.apache.tomcat.jni.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Recommend {
    @TableId(type = IdType.AUTO)
    private Long recomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalTime;
    private Integer clicks;
    private Long userId;
    private Long bookId;
    private String category;
}
