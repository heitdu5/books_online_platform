package com.OLP.author.service;

import com.OLP.common.pojo.Publication;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface PublicationService  extends IService<Publication> {
}
