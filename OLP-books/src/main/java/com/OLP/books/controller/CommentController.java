package com.OLP.books.controller;


import com.OLP.books.rpc.UserRpc;
import com.OLP.common.pojo.Comment;
import com.OLP.common.pojo.User;
import com.OLP.common.util.LoginUtil;
import com.OLP.common.vo.CommentVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.OLP.books.common.BaseContext;
import com.OLP.books.common.R;
import com.OLP.books.common.exception.DataException;
import com.OLP.books.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论功能
 */
@Slf4j
@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private CommentService commentService;
    @Autowired
    private UserRpc userRpc;

    @PostMapping("/issuedMycomment")
    public R<String> issuedMycomment(@RequestBody Comment comment){
        log.info("{}",comment);
        Long id = LoginUtil.getLoginId();
        comment.setUserId(id);
        commentService.save(comment);
        return R.success("发表评论成功!");
    }

    /**
     * 回复其他评论
     * @param comment
     * @return
     */
    @PostMapping("/issueOthercomment")
    public R<String> issueOthercomment(@RequestBody Comment comment){
        Long id = LoginUtil.getLoginId();
        comment.setUserId(id);
        commentService.save(comment);
        //将被回复的评论的hasReplies改变状态
        Comment answeredComment = commentService.getById(comment.getAnswerId());
        answeredComment.setHasReplies(true);
        commentService.updateById(answeredComment);
        return R.success("回复评论成功!");
    }

    @GetMapping("/CommentList")
    public R<List<CommentVo>> CommentList(Long bookId){
        log.info("{}",bookId);
        LambdaQueryWrapper<Comment> comLam = new LambdaQueryWrapper<>();
        //这里采用先获取第一层级的评论的方法，再把集合每一个评论实体里装填它的回复评论集合
        comLam.eq(Comment::getBookId,bookId).eq(Comment::getAnswerId,0);
        List<Comment> list = commentService.list(comLam);
        if (list == null ||list.size()==0){
            return R.error("该书籍还没有人评论");
        }
        List<CommentVo> newlist = list.stream().map(item ->{
            CommentVo commentVo = new CommentVo();
            BeanUtils.copyProperties(item,commentVo);
            User user = userRpc.getById(item.getUserId());
            commentVo.setUsername(user.getUsername());
            commentVo.setAvatarUrl(user.getAvatarurl());
            //这是设置的是这个评论的回复评论集合，先通过hasReplies判断是否有回复
            if (commentVo.getHasReplies()){
                LambdaQueryWrapper<Comment> newcomlam = new LambdaQueryWrapper<>();
                newcomlam.eq(Comment::getAnswerId,commentVo.getCommentId());
                List<Comment> answerlist = commentService.list(newcomlam);
                List<CommentVo> newAnswerlist = answerlist.stream().map(answeritem ->{
                    CommentVo answercommentVo = new CommentVo();
                    BeanUtils.copyProperties(answeritem,answercommentVo);
                    User answeruser = userRpc.getById(answeritem.getUserId());
                    answercommentVo.setUsername(answeruser.getUsername());
                    answercommentVo.setAvatarUrl(answeruser.getAvatarurl());
                    return answercommentVo;
                }).collect(Collectors.toList());
                commentVo.setSecondReplies(newAnswerlist);
            }
            return commentVo;
        }).collect(Collectors.toList());
        return R.success(newlist);
    }

}
