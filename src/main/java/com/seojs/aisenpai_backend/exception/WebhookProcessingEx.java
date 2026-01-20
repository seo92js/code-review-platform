package com.seojs.aisenpai_backend.exception;

public class WebhookProcessingEx extends RuntimeException {
    
    public WebhookProcessingEx() {
        super();
    }

    public WebhookProcessingEx(String message) {
        super(message);
    }

    public WebhookProcessingEx(String message, Throwable cause) {
        super(message, cause);
    }

    public WebhookProcessingEx(Throwable cause) {
        super(cause);
    }

    protected WebhookProcessingEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
