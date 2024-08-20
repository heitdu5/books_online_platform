package com.OLP.books.service.impl;

import com.OLP.books.rpc.UserRpc;
import com.OLP.common.pojo.Bookshelf;
import com.OLP.common.util.LoginUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.OLP.books.common.BaseContext;
import com.OLP.books.common.exception.DataException;
import com.OLP.books.mapper.BookshelfMapper;
import com.OLP.books.service.BookshelfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookshelfServiceImpl extends ServiceImpl<BookshelfMapper, Bookshelf> implements BookshelfService {
    @Autowired
    private BookshelfService bookshelfService;
    @Autowired
    private UserRpc userRpc;

    @Override
    public Map<String, Long> collectMonth(String username) {
        Long id = LoginUtil.getLoginId();//我本人的id
        LocalDate now = LocalDate.now();
            //获取今年第一天
            LocalDate startDayofYear = now.with(TemporalAdjusters.firstDayOfYear());
            LocalDate afterYear = now.plusYears(1);
            //获取明年第一天
            LocalDate startDayofAfterYear = afterYear.with(TemporalAdjusters.firstDayOfYear());
            LambdaQueryWrapper<Bookshelf> bslam = new LambdaQueryWrapper<>();
            bslam.eq(Bookshelf::getAuthor,userRpc.getById(id).getUsername())
                    .ge(Bookshelf::getCreateTime,startDayofYear)
                    .le(Bookshelf::getCreateTime,startDayofAfterYear);
            List<Bookshelf> list = bookshelfService.list(bslam);
            if (list==null||list.size()==0){
                throw new DataException("今年该作者的书籍都没有被收藏");
            }
            Map<String, Long> countmap = list.stream().collect(Collectors.groupingBy(
                    item -> String.valueOf(item.getCreateTime().getMonthValue()),Collectors.counting()));
            return countmap;
    }
}
