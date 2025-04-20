package com.seojs.code_review_platform.github.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class WebhookResponseDto {
    private Long id;
    private String name;
    private boolean active;
    private Map<String, String> config;
    private List<String> events;
}
