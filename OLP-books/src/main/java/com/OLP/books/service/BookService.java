package com.OLP.books.service;

import com.OLP.books.common.R;
import com.OLP.common.dto.BookDto;
import com.OLP.common.pojo.Book;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Transactional
public interface BookService extends IService<Book> {
    /**
     * 分类下拉菜单展示
     */
    List<String> sortlist();

    /**
     * 书籍搜索
     */
    List<Book> search(BookDto bookDto);

    /**
     *默认展示书籍与选中种类
     */
    List<Book> commonSort(String type);



    /**
     * 待审核的书籍
     * @return
     */
    List<Book> Auditsearch(BookDto bookDto);


    R getList(int page, int pageSize);

    R getEsPage(int page, int pageSize, String name,String category, LocalDate sdate, LocalDate edate);

    void deleteDoc(Long id);

    void insertOrUpdateById(Long id);

    void UpdateclickDoc(Long id);

    void syncClicks();
}
