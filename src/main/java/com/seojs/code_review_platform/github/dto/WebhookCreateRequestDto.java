package com.seojs.code_review_platform.github.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class WebhookCreateRequestDto {
    private String name;
    private Map<String, String> config;
    private List<String> events;
    private boolean active;
}
