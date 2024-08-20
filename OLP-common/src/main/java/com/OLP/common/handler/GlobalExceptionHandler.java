package com.OLP.common.handler;



import com.OLP.common.entity.ErrorConstant;
import com.OLP.common.entity.R;
import com.OLP.common.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler(BaseException.class)
    public R<String> baseExceptionHandler(BaseException ex){
        ex.printStackTrace();
        log.error("异常信息：{}",ex.getMessage());
        return R.error(ex.getMessage());
    }

    /**
     * 捕获其他异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public R<String> exceptionHandler(Exception ex){
        ex.printStackTrace();
        log.error("异常信息：{}",ex.getMessage());
        return R.error(ErrorConstant.UNKOWN_ERROR);
    }
}
