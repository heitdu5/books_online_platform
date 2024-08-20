package com.OLP.books.service.impl;

import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.Recommend;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.OLP.books.mapper.BookMapper;
import com.OLP.books.mapper.RecommendMapper;
import com.OLP.books.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RecommendServiceImpl extends ServiceImpl<RecommendMapper, Recommend> implements RecommendService {
    @Autowired
    private BookMapper bookMapper;
    @Override
    public List<Book> nullrecommendBooks() {
        String status = "1";
      return bookMapper.nullrecommendBookList(status);
    }
}
