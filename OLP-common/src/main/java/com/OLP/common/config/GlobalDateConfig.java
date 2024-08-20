package com.OLP.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class GlobalDateConfig {
    // 序列化时设置的时间格式
    private final String dateTimeFormatter = "yyyy-MM-dd HH:mm:ss";
    private final String dateFormatter = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            DateTimeFormatter localDateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormatter);
            DateTimeFormatter localDateFormatter = DateTimeFormatter.ofPattern(dateFormatter);
            
            // 返回时间数据序列化
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(localDateTimeFormatter));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(localDateFormatter));

            // 接收时间数据反序列化
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(localDateTimeFormatter));
            builder.deserializerByType(LocalDate.class, new LocalDateDeserializer(localDateFormatter));
        };
    }
}