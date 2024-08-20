package com.OLP.author;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 网关服务启动器
 *
 */
@SpringBootApplication
@ComponentScan("com.OLP")
public class AuthorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorApplication.class);
    }

}
