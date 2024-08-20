package com.OLP.books.controller;


import com.OLP.common.entity.MqConstants;
import com.OLP.common.pojo.Book;
import com.OLP.common.pojo.Recommend;
import com.OLP.common.util.LoginUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.OLP.books.common.BaseContext;
import com.OLP.books.common.R;
import com.OLP.books.common.exception.DataException;
import com.OLP.books.service.BookService;
import com.OLP.books.service.RecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 兴趣推荐
 */
@Slf4j
@RestController
@RequestMapping("/recommend")
public class RecommendController {
    @Autowired
    private RecommendService recommendService;
    @Autowired
    private BookService bookService;
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 浏览开始
     * @param bookId
     * @param category 这里传入category是为了不用id查一次库
     * @return
     */
    @GetMapping("/Begin")
    public R<String> begin(Long bookId,String category) {
        LambdaQueryWrapper<Book> bookLam = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Recommend> reLam = new LambdaQueryWrapper<>();
        Long userId = LoginUtil.getLoginId();

        //先给这本书点击量加1
        rabbitTemplate.convertAndSend(MqConstants.BOOKS_EXCHANGE,MqConstants.BOOKS_CLICK_KEY,bookId);

        reLam.eq(Recommend::getUserId, userId).eq(Recommend::getBookId, bookId);
        Recommend Mayrecommend = recommendService.getOne(reLam);
        if (Mayrecommend == null) {
            //该用户还没看过该书籍
            Recommend recommend = new Recommend();
            recommend.setBookId(bookId);
            recommend.setCategory(category);
            recommend.setUserId(userId);
            recommend.setClicks(1);
            recommend.setStartTime(LocalDateTime.now());
            recommendService.save(recommend);
            return R.success("已记录用户浏览书籍...");
        }
        Mayrecommend.setStartTime(LocalDateTime.now());
        Mayrecommend.setClicks(Mayrecommend.getClicks() + 1);
        recommendService.updateById(Mayrecommend);
        return R.success("已记录用户浏览书籍...");
    }

    /**
     * 浏览结束
     * @param bookId
     * @return
     */
    @GetMapping("/End")
    public R<String> End(Long bookId) {
        LambdaQueryWrapper<Book> bookLam = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Recommend> reLam = new LambdaQueryWrapper<>();
        Long userId = LoginUtil.getLoginId();
        reLam.eq(Recommend::getUserId, userId).eq(Recommend::getBookId, bookId);
        Recommend Mayrecommend = recommendService.getOne(reLam);
        if (Mayrecommend == null) {
            throw new DataException("该用户不可能没看过该书籍");
        }
        Mayrecommend.setEndTime(LocalDateTime.now());
        //计算时间差
        Long seconds = ChronoUnit.SECONDS.between(Mayrecommend.getStartTime(),Mayrecommend.getEndTime());
        if (Mayrecommend.getTotalTime()==null||Mayrecommend.getTotalTime()==0){
            Mayrecommend.setTotalTime(Math.toIntExact(seconds));
        }
        else{
            Mayrecommend.setTotalTime(Mayrecommend.getTotalTime() + Math.toIntExact(seconds) );
        }
        recommendService.updateById(Mayrecommend);
        return R.success("已记录用户浏览书籍...");
    }

    /**
     * 推荐算法
     * 展示书籍
     * @return
     */
    @GetMapping("/recommendList")
    public R<Page> recommendList(int page,int pageSize){
        //构造分页构造器
        Page pageinfo = new Page(page, pageSize);
        LambdaQueryWrapper<Recommend> recLam = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Book> booklam = new LambdaQueryWrapper<>();
        Long id = LoginUtil.getLoginId();
        recLam.eq(Recommend::getUserId,id);
        List<Recommend> recList = recommendService.list(recLam);
        //没有进入书籍浏览，无法获取该用户喜好,展示点击量最高的八本书籍
        if (recList==null || recList.size()==0){
//            booklam.eq(Book::getStatus,"1");
//            bookService.page(pageinfo,booklam);
            List<Book> books = recommendService.nullrecommendBooks();//只返回8条数据
            pageinfo.setTotal(8);
            if (page==1){
                List<Book> pagebooks = books.subList(0,4);
                pageinfo.setRecords(pagebooks);
            }
            if (page==2){
                List<Book> pagebooks = books.subList(4,8);
                pageinfo.setRecords(pagebooks);
            }
            return R.success(pageinfo);
        }
        Integer maxClicks = 0;
        Integer maxTotalTime = 0;
        String maxClicksCategory = new String();
        String maxTimeCategory = new String();
        for (Recommend i : recList) {
            if (i.getClicks()>maxClicks) {
                maxClicks = i.getClicks();
                maxClicksCategory = i.getCategory();
            }
            if (i.getTotalTime()>maxTotalTime){
                maxTotalTime = i.getTotalTime();
                maxTimeCategory = i.getCategory();
            }
        }
        //如果是同一种类型
        if (maxClicksCategory.equals(maxTimeCategory)) {
            booklam.eq(Book::getCategory,maxClicksCategory).eq(Book::getStatus,"1");
            bookService.page(pageinfo,booklam);
            return R.success(pageinfo);
        }
        //不同类型
        String finalMaxClicksCategory = maxClicksCategory;
        String finalMaxTimeCategory = maxTimeCategory;
        booklam.eq(Book::getStatus,"1")
                .and(i -> i.eq(Book::getCategory, finalMaxClicksCategory)
                .or()
                .eq(Book::getCategory, finalMaxTimeCategory));
        List<Book> list = bookService.list(booklam);
        bookService.page(pageinfo,booklam);
        return R.success(pageinfo);
    }
}
