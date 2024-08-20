package com.OLP.common.dto;

import com.OLP.common.pojo.Book;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class BookDto extends Book implements Serializable {
    private String type;//筛选书籍种类
    private String date;//搜索出版时间
    private MultipartFile coverImg;//前端传递过来的封面文件
    private String bucket;
    private String objectName;

}
