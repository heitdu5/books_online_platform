package com.OLP.api;


import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("yh-olp-books")
public interface BookFeignService {
}
