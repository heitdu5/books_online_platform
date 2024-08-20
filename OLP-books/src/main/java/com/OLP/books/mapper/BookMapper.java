package com.OLP.books.mapper;

import com.OLP.common.pojo.Book;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
    @Select("select * from book where status = #{status} ")
    List<Book> displayAll(String status);

    @Select("select * from book where category = #{type} and status = #{status} ")
    List<Book> displaysort(String type,String status);

    @Select("select * from book where status = #{status} order by Clicks desc limit 8")
    List<Book> nullrecommendBookList(String status);
}
