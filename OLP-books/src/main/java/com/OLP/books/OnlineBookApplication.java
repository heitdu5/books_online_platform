package com.OLP.books;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;


@Slf4j
@SpringBootApplication
@ComponentScan("com.OLP")
@EnableFeignClients(basePackages = "com.OLP")
public class OnlineBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnlineBookApplication.class,args);
        log.info("项目启动成功...");
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(){
        return new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.232.112:9200")
        ));
    }
}
