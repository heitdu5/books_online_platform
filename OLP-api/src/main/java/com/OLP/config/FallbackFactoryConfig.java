package com.OLP.config;

import com.OLP.fallback.UserFeignFallbackFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FallbackFactoryConfig {

    @Bean
    public UserFeignFallbackFactory userFeignFallbackFactory(){
        return new UserFeignFallbackFactory();
    }
}
