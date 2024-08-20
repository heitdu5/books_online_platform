package com.OLP.common.vo;

import com.OLP.common.pojo.Comment;
import lombok.Data;

import java.util.List;

@Data
public class CommentVo extends Comment {
    private String username;
    private String avatarUrl;
    private List<CommentVo> secondReplies;//第二级回复评论
}
