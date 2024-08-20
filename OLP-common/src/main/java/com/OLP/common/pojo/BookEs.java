package com.OLP.common.pojo;

import com.OLP.common.dto.BookDto;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookEs {
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
    private List<String> suggestion;//自动补全


    public BookEs(Book book) {
        this.bookId = book.getBookId();
        this.bookName = book.getBookName();
        this.author = book.getAuthor();
        this.publicationDate = book.getPublicationDate();
        this.description = book.getDescription();
        this.coverUrl = book.getCoverUrl();
        this.category = book.getCategory();
        this.status = book.getStatus();
        Clicks = book.getClicks();
        this.suggestion = Arrays.asList(book.getBookName(),book.getAuthor());
    }


    public BookEs(BookDto bookDto) {
        this.bookId = bookDto.getBookId();
        this.bookName = bookDto.getBookName();
        this.author = bookDto.getAuthor();
        this.publicationDate = bookDto.getPublicationDate();
        this.description = bookDto.getDescription();
        this.coverUrl = bookDto.getCoverUrl();
        this.category = bookDto.getCategory();
        this.status = bookDto.getStatus();
        Clicks = bookDto.getClicks();
        this.suggestion = Arrays.asList(bookDto.getBookName(),bookDto.getAuthor());
    }
}
