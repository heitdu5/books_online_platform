package com.OLP.books.service;

import com.OLP.common.pojo.Comment;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface CommentService extends IService<Comment> {

}
