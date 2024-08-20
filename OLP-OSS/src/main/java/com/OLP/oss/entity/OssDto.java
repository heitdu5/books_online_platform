package com.OLP.oss.entity;


import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class OssDto {
    private MultipartFile uploadFile;

    private String bucket;

    private String objectName;
}
