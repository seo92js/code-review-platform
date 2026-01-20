package com.seojs.aisenpai_backend.exception;

public class TokenEncryptionEx extends RuntimeException {
    
    public TokenEncryptionEx() {
        super();
    }

    public TokenEncryptionEx(String message) {
        super(message);
    }

    public TokenEncryptionEx(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenEncryptionEx(Throwable cause) {
        super(cause);
    }

    protected TokenEncryptionEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
