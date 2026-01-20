package com.seojs.aisenpai_backend.exception;

public class GithubAccountNotFoundEx extends RuntimeException {
    
    public GithubAccountNotFoundEx() {
        super();
    }

    public GithubAccountNotFoundEx(String message) {
        super(message);
    }

    public GithubAccountNotFoundEx(String message, Throwable cause) {
        super(message, cause);
    }

    public GithubAccountNotFoundEx(Throwable cause) {
        super(cause);
    }

    protected GithubAccountNotFoundEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
