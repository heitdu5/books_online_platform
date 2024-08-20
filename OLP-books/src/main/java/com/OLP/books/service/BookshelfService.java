package com.OLP.books.service;


import com.OLP.common.pojo.Bookshelf;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface BookshelfService extends IService<Bookshelf> {

    /**
     * 统计作者的书籍的各月份收藏量
     * @param username
     * @return
     */
    Map<String, Long> collectMonth(String username);

}
