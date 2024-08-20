package com.OLP.books.service;

import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.Recommend;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface RecommendService extends IService <Recommend> {
    /**
     * 用户没有进入浏览过书籍，默认推荐接口
     */
    List<Book> nullrecommendBooks();
}
