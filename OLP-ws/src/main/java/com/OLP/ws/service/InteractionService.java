package com.OLP.ws.service;

import com.OLP.common.pojo.Interaction;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface InteractionService extends IService<Interaction> {
}
