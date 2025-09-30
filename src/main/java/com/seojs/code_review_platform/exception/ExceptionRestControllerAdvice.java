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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(GithubAccountNotFoundEx.class)
    public ErrorDto githubAccountNotFound(GithubAccountNotFoundEx e) {
        String errorCode = "GITHUB_ACCOUNT_NOT_FOUND";
        String message = e.getMessage();
        log.error("{} : {}", errorCode, message);
        return new ErrorDto(errorCode, message);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PullRequestNotFoundEx.class)
    public ErrorDto pullRequestNotFound(PullRequestNotFoundEx e) {
        String errorCode = "PULL_REQUEST_NOT_FOUND";
        String message = e.getMessage();
        log.error("{} : {}", errorCode, message);
        return new ErrorDto(errorCode, message);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SecurityException.class)
    public ErrorDto securityException(SecurityException e) {
        String errorCode = "SECURITY_VIOLATION";
        String message = e.getMessage();
        log.error("{} : {}", errorCode, message);
        return new ErrorDto(errorCode, message);
    }
}
