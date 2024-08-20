package com.OLP.books.mq.config;

import com.OLP.common.entity.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    @Bean
    public TopicExchange topicExchange(){
                                                                    //是否持久化和交换机是否自动删除
        return new TopicExchange(MqConstants.BOOKS_EXCHANGE,true,false);
    }
    @Bean
    public Queue insertQueue(){
        return new Queue(MqConstants.BOOKS_INSERTORUPDATE_QUEUE,true);
    }

    @Bean
    public Queue deleteQueue(){
        return new Queue(MqConstants.BOOKS_DELETE_QUEUE,true);
    }

    @Bean
    public Queue clickQueue(){
        return new Queue(MqConstants.BOOKS_CLICK_QUEUE,true);
    }

    @Bean
    public Binding clickQueueBinding(){
        return BindingBuilder.bind(clickQueue()).to(topicExchange()).with(MqConstants.BOOKS_CLICK_KEY);
    }

    @Bean
    public Binding insertQueueBinding(){
        return BindingBuilder.bind(insertQueue()).to(topicExchange()).with(MqConstants.BOOKS_INSERTORUPDATE_KEY);
    }

    @Bean
    public Binding deleteQueueBinding(){
        return BindingBuilder.bind(deleteQueue()).to(topicExchange()).with(MqConstants.BOOKS_DELETE_KEY);
    }

}
