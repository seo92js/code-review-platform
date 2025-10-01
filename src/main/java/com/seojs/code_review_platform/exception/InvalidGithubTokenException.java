package com.seojs.code_review_platform.exception;

public class InvalidGithubTokenException extends RuntimeException {
    public InvalidGithubTokenException(String message) {
        super(message);
    }
}
