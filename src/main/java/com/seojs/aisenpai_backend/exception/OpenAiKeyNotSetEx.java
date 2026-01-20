package com.seojs.aisenpai_backend.exception;

public class OpenAiKeyNotSetEx extends RuntimeException {
    
    public OpenAiKeyNotSetEx() {
        super();
    }

    public OpenAiKeyNotSetEx(String message) {
        super(message);
    }

    public OpenAiKeyNotSetEx(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenAiKeyNotSetEx(Throwable cause) {
        super(cause);
    }

    protected OpenAiKeyNotSetEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
