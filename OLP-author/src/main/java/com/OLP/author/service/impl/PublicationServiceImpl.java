package com.OLP.author.service.impl;

import com.OLP.author.mapper.PublicationMapper;
import com.OLP.author.service.PublicationService;
import com.OLP.common.pojo.Publication;
import com.OLP.common.util.LoginUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PublicationServiceImpl extends ServiceImpl<PublicationMapper, Publication> implements PublicationService {
    @Autowired
    private PublicationService publicationService;

}
