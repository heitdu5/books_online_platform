package com.OLP.books.controller;


import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.Bookshelf;
import com.OLP.common.util.LoginUtil;
import com.OLP.common.vo.BookshelfVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.OLP.books.common.BaseContext;
import com.OLP.books.common.R;
import com.OLP.books.common.exception.DataException;
import com.OLP.books.service.BookService;
import com.OLP.books.service.BookshelfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 书架
 */
@Slf4j
@RestController
@RequestMapping("/bookshelf")
public class BookshelfController {

    @Autowired
    private BookshelfService bookshelfService;

    @Autowired
    private BookService bookService;

    /**
     * 加入书架
     * @param bookId
     * @return
     */
    @GetMapping("/addshelf")
    public R<String> addshelf(Long bookId){
        if (bookId==null){
            throw new DataException("加入书架的书籍不存在！");
        }
        Long Myid = LoginUtil.getLoginId();
        String author = bookService.getById(bookId).getAuthor();
        Bookshelf bookshelf = new Bookshelf();
        bookshelf.setUserId(Myid);
        bookshelf.setBookId(bookId);
        bookshelf.setAuthor(author);
        bookshelfService.save(bookshelf);
        return R.success("加入书架成功！");
    }

    /**
     * 用来给定这个人是否加入书架的标志
     * @param bookId
     * @return
     */
    @GetMapping("/isAdd")
    public R<String> isAdd(Long bookId){
        if (bookId==null){
            throw new DataException("没有获得书籍详情的id");
        }
        Long Myid = LoginUtil.getLoginId();
        LambdaQueryWrapper<Bookshelf> bslam = new LambdaQueryWrapper<>();
        bslam.eq(Bookshelf::getUserId,Myid).eq(Bookshelf::getBookId,bookId);
        if (bookshelfService.getOne(bslam)==null){
            return R.error("还没有将该书加入书架");
        }
        return R.success("之前已经将该书加入书架");
    }

    /**
     * 删除书籍
     * @param bookId
     * @return
     */
    @DeleteMapping("/deleteshelf")
    public R<String> deleteshelf(Long bookId){
        if (bookId==null){
            throw new DataException("没有获得书籍详情的id");
        }
        Long Myid = LoginUtil.getLoginId();
        LambdaQueryWrapper<Bookshelf> bslam = new LambdaQueryWrapper<>();
        bslam.eq(Bookshelf::getUserId,Myid).eq(Bookshelf::getBookId,bookId);
        if (bookshelfService.getOne(bslam)==null){
            return R.error("你没有将该书加入书架");
        }
        bookshelfService.remove(bslam);
        return R.success("移出书架成功");
    }

    /**
     * 展示书架
     * @return
     */
    @GetMapping("/bookList")
    public R<List<BookshelfVo>> bookList(){
        Long id = LoginUtil.getLoginId();
        LambdaQueryWrapper<Bookshelf> bslam = new LambdaQueryWrapper<>();
        bslam.eq(Bookshelf::getUserId,id);

        try {
            //对集合进行处理
            List<BookshelfVo> collectBooks = bookshelfService.list(bslam)
                    .stream()
                    .map(item -> {
                        Long bookId = item.getBookId();
                        BookshelfVo bookshelfVo = new BookshelfVo();
                        bookshelfVo.setAuthor(item.getAuthor());
                        bookshelfVo.setBookId(item.getBookId());
                        Book book = bookService.getById(bookId);
                        bookshelfVo.setBookName(book.getBookName());
                        bookshelfVo.setCoverUrl(book.getCoverUrl());
                        bookshelfVo.setDescription(book.getDescription());
                        bookshelfVo.setCategory(book.getCategory());
                        bookshelfVo.setClicks(book.getClicks());
                        bookshelfVo.setCreateTime(item.getCreateTime());
                        return bookshelfVo;
                    }).collect(Collectors.toList());
                    return R.success(collectBooks);
        } catch (Exception e) {
            e.printStackTrace();
           throw new DataException("该用户的书架为空");
        }
    }
}
