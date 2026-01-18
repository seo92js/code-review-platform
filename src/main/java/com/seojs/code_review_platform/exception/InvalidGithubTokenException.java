package com.seojs.code_review_platform.exception;

public class InvalidGithubTokenException extends RuntimeException {
    public InvalidGithubTokenException() {
        super();
    }

    public InvalidGithubTokenException(String message) {
        super(message);
    }

    public InvalidGithubTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidGithubTokenException(Throwable cause) {
        super(cause);
    }

    protected InvalidGithubTokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
