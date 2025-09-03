package com.seojs.code_review_platform.exception;

public class PullRequestNotFoundEx extends RuntimeException {
    
    public PullRequestNotFoundEx() {
        super();
    }

    public PullRequestNotFoundEx(String message) {
        super(message);
    }

    public PullRequestNotFoundEx(String message, Throwable cause) {
        super(message, cause);
    }

    public PullRequestNotFoundEx(Throwable cause) {
        super(cause);
    }

    protected PullRequestNotFoundEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
