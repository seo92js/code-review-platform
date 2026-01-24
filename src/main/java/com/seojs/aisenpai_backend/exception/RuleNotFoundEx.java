package com.seojs.aisenpai_backend.exception;

public class RuleNotFoundEx extends RuntimeException {

    public RuleNotFoundEx() {
        super();
    }

    public RuleNotFoundEx(String message) {
        super(message);
    }

    public RuleNotFoundEx(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleNotFoundEx(Throwable cause) {
        super(cause);
    }

    protected RuleNotFoundEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
