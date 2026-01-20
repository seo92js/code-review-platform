package com.seojs.aisenpai_backend.exception;

public class WebhookRegistrationEx extends RuntimeException {
    
    public WebhookRegistrationEx() {
        super();
    }

    public WebhookRegistrationEx(String message) {
        super(message);
    }

    public WebhookRegistrationEx(String message, Throwable cause) {
        super(message, cause);
    }

    public WebhookRegistrationEx(Throwable cause) {
        super(cause);
    }

    protected WebhookRegistrationEx(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
