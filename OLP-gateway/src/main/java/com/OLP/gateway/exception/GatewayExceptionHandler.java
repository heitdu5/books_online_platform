package com.OLP.gateway.exception;


import cn.dev33.satoken.exception.SaTokenException;
import com.OLP.gateway.entity.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局异常处理
 */
@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, Throwable throwable) {
        ServerHttpRequest request = serverWebExchange.getRequest();
        ServerHttpResponse response = serverWebExchange.getResponse();
        Integer code = 200;
        String message = "";
        if (throwable instanceof SaTokenException){
            code = 401;
            message = "用户无权限";
        } else{
            code = 500;
            message = "系统繁忙";
        }
        Result result = Result.fail(code, message);
        //Mono表示一个包含零个或一个元素的异步序列,整个过程是异步的，Mono会在生成响应数据时触发。
        //获取了response对象的DataBufferFactory，用于创建DataBuffer对象，
        // 然后将Result对象转换为字节数组，最后通过DataBufferFactory创建一个DataBuffer对象来包装这个字节数组
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.fromSupplier(() ->{
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            byte[] bytes = null;
            try {
                bytes = objectMapper.writeValueAsBytes(result);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return dataBufferFactory.wrap(bytes);
        }));
    }
}
