package com.seojs.code_review_platform.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionRestControllerAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public ErrorDto runtime(RuntimeException e) {
        String errorCode = "RUNTIME_EX";
        String message = e.getMessage();
        log.error("{} : {}", errorCode, message);
        return new ErrorDto(errorCode, message);
    }
}
