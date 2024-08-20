package com.OLP.books.service.impl;

import com.OLP.common.pojo.Comment;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.OLP.books.mapper.CommentMapper;
import com.OLP.books.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Autowired
    private CommentService commentService;

}
