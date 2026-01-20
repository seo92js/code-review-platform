package com.seojs.aisenpai_backend.exception;

public class GitHubApiEx extends RuntimeException {
    
    public GitHubApiEx() {
        super();
    }

    public GitHubApiEx(String message) {
        super(message);
    }

    public GitHubApiEx(String message, Throwable cause) {
        super(message, cause);
    }

    public GitHubApiEx(Throwable cause) {
        super(cause);
    }

    protected GitHubApiEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
