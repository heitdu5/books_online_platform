package com.OLP.ws.service.impl;

import com.OLP.common.pojo.Interaction;
import com.OLP.ws.mapper.InteractionMapper;
import com.OLP.ws.service.InteractionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InteractionServiceImpl extends ServiceImpl<InteractionMapper, Interaction> implements InteractionService {

}
