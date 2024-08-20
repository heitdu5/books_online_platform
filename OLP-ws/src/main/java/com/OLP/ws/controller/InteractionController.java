package com.OLP.ws.controller;


import com.OLP.common.dto.InteractionDto;
import com.OLP.common.entity.R;
import com.OLP.common.exception.DataException;
import com.OLP.common.pojo.Interaction;
import com.OLP.common.pojo.Readstatus;
import com.OLP.common.pojo.User;
import com.OLP.common.util.LoginUtil;
import com.OLP.ws.rpc.UserRpc;
import com.OLP.ws.service.InteractionService;
import com.OLP.ws.service.ReadstatusService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
@RequestMapping("/interact")
public class InteractionController {
    @Autowired
    private InteractionService interactionService;

    @Autowired
    private UserRpc userRpc;

    @Autowired
    private ReadstatusService readstatusService;

    /**
     * 返回好友列表中的联系人栏数据
     * @param username
     * @return
     */
    @GetMapping("/UsersSearch")
    public R<List<User>> UsersSearch(@RequestParam(name = "username",required = false)String username){
        log.info("进入ws.interact");
        return R.success(userRpc.FreindsList(username));
    }



    /**
     * 展示历史消息
     */
    @GetMapping("/getrecords")
    public R<List<InteractionDto>> getrecords(String name){
        LambdaQueryWrapper<Interaction> intlam = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        Long MyId = LoginUtil.getLoginId();
        String Myname = userRpc.getById(MyId).getUsername();
        String Myavatar = userRpc.getById(MyId).getAvatarurl();
        //得到对方的头像
        userlam.eq(User::getUsername,name);
        //名字唯一，可以直接getOne
        User counterpart = userRpc.getOneByCondition(name);
        String partAvatar = counterpart.getAvatarurl();

        // TODO: 左边消息：传递过来的name时answername,自己是sponsorname。右边消息反之
        intlam.and(i ->i.eq(Interaction::getAnswerName,name).eq(Interaction::getSponsorName,Myname))
                .or(i ->i.eq(Interaction::getAnswerName,Myname).eq(Interaction::getSponsorName,name))
                .orderByAsc(Interaction::getSendTime);//按时间升序符合聊天的顺序
        List<Interaction> List = interactionService.list(intlam);
        if (List==null||List.size()==0){
            return R.error("你与Ta还没有消息记录");
        }
        List<InteractionDto> newList = List.stream().map(item->{
            InteractionDto interactionDto = new InteractionDto();
            //我是发起者，即右边消息
            if (Myname.equals(item.getSponsorName())){
                interactionDto.setUsername(item.getSponsorName());
                interactionDto.setAvatarurl(Myavatar);
                interactionDto.setSendTime(item.getSendTime());
                interactionDto.setMessage(item.getContent());
                interactionDto.setIsLeft(false);
            }
            //对方是发起者，即左边消息
           if (name.equals(item.getSponsorName())){
               interactionDto.setUsername(item.getSponsorName());
               interactionDto.setAvatarurl(partAvatar);
               interactionDto.setSendTime(item.getSendTime());
               interactionDto.setMessage(item.getContent());
               interactionDto.setIsLeft(true);
           }
           return interactionDto;
        }).collect(Collectors.toList());

        return R.success(newList);
    }

    /**
     * 展示与我聊天过的消息记录
     * 需求：将最晚消息展示在联系人名字下方
     * @return
     */
    @GetMapping("/onesHistory")
    public R<List<InteractionDto>> onesHistory(){
        LambdaQueryWrapper<User> userlam = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Interaction> intlam = new LambdaQueryWrapper<>();
        Long MyId = LoginUtil.getLoginId();
        String Myname = userRpc.getById(MyId).getUsername();
        intlam.eq(Interaction::getSponsorName,Myname).or().eq(Interaction::getAnswerName,Myname);
        //TODO  1.筛选出与我有过消息的联系人的数据
        List<Interaction> relativeList = interactionService.list(intlam);
        if (relativeList==null||relativeList.size()==0){
            throw new DataException("没有与我有过消息的联系人");
        }
        //这里把联系人的名字放到一个集合里面，不重复
        List<String> reactUsers = new ArrayList<>();

        for (Interaction i : relativeList) {
            String sponsorName = i.getSponsorName();
            String answerName = i.getAnswerName();
            //条件：名字不重复,且不是我本人
            if (!reactUsers.contains(sponsorName) &&!Myname.equals(sponsorName)){
                        reactUsers.add(sponsorName);
            }
            if (!reactUsers.contains(answerName)&&!Myname.equals(answerName)){
                        reactUsers.add(answerName);
            }
        }
        //获得了和我有关的联系人的数据信息
        List<User> userList = userRpc.getListByCondition(reactUsers);
        //TODO 2.现在只获取每个和我有关的联系人和我最晚的那条信息,放入dto存为集合
        List<InteractionDto> dtoList = new ArrayList<>();
        LambdaQueryWrapper<Interaction> newinterLam = new LambdaQueryWrapper<>();
        for (User user : userList) {
            InteractionDto interactionDto = new InteractionDto();
            String username = user.getUsername();
            String avatarurl = user.getAvatarurl();
            newinterLam.and(i ->i.eq(Interaction::getAnswerName,username).eq(Interaction::getSponsorName,Myname))
                    .or(i ->i.eq(Interaction::getAnswerName,Myname).eq(Interaction::getSponsorName,username))
                    .orderByDesc(Interaction::getSendTime)//匹配到我和这个人互相发的所有信息
                    .last("limit 1");//时间降序排列再限制一条数据，就得到最晚发送的那条信息数据
            Interaction interaction = interactionService.getOne(newinterLam);
            String content = interaction.getContent();
            interactionDto.setUsername(username);
            interactionDto.setAvatarurl(avatarurl);
            interactionDto.setMessage(content);
            //根据messageId和username获得已读状态,这里肯定是针对"我"读了没，因为是显示"我"读没读
            LambdaQueryWrapper<Readstatus> readLam = new LambdaQueryWrapper<>();
            readLam.eq(Readstatus::getMessageId,interaction.getInteractId()).eq(Readstatus::getUsername,Myname);
            if (readstatusService.getOne(readLam)!=null){
                interactionDto.setIsRead(readstatusService.getOne(readLam).getIsRead());
            }
            dtoList.add(interactionDto);
            //清空构造器，方便循环利用
            newinterLam.clear();
            readLam.clear();
        }

        return R.success(dtoList);
    }

    /**
     * 将未读状态改为已读
     * @param username
     * @return
     */
    @GetMapping("/hasRead")
    public R<String> updateRead(String username){
        //TODO 传来的是点击的用户，将我和他之间的最新消息状态(对"我")修改为已读

        LambdaQueryWrapper<Interaction> intlam = new LambdaQueryWrapper<>();
        //得到我的名字
        Long MyId = LoginUtil.getLoginId();
        String Myname = userRpc.getById(MyId).getUsername();

        try {
        //TODO 1.获得我和他之间所有的消息数据
        intlam.and(i ->i.eq(Interaction::getAnswerName,username).eq(Interaction::getSponsorName,Myname))
                .or(i ->i.eq(Interaction::getAnswerName,Myname).eq(Interaction::getSponsorName,username))
                .orderByDesc(Interaction::getSendTime)
                .last("limit 1");//时间降序排列再限制一条数据，就得到最晚发送的那条信息数据
        Interaction latest = interactionService.getOne(intlam);
        if (latest==null){
            throw new DataException("你和他还没有消息");
        }
        //TODO 2.修改状态
            LambdaQueryWrapper<Readstatus> readlam = new LambdaQueryWrapper<>();
            readlam.eq(Readstatus::getUsername,Myname).eq(Readstatus::getMessageId,latest.getInteractId());
            Readstatus readstatus = readstatusService.getOne(readlam);
            readstatus.setIsRead(true);
            readstatusService.updateById(readstatus);
        } catch (DataException e) {
            e.printStackTrace();
            throw new DataException("该条信息还没有已读/未读的设置");
        }
        return R.success("修改已读成功");
    }

}





