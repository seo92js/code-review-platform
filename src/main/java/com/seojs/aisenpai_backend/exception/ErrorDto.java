package com.seojs.aisenpai_backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ErrorDto {
    private final String code;
    private final String message;
}
