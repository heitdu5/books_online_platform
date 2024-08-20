package com.OLP.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "yh-olp-oss")
public interface OssFeignService {
    /**
     * 上传文件
     * 要返回一个地址是多少的信息
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("uploadFile") MultipartFile uploadFile,
                         @RequestParam("bucket") String bucket,
                         @RequestParam("objectName") String objectName);



}
