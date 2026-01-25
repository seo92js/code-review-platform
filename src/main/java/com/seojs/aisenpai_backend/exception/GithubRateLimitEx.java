package com.seojs.aisenpai_backend.exception;

public class GithubRateLimitEx extends RuntimeException {
    public GithubRateLimitEx(String message) {
        super(message);
    }
}
