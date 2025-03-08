package com.seojs.code_review_platform.github.dto;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Getter
@Service
public class WebhookResponseDto {
    private Long id;
    private String name;
    private boolean active;
    private Map<String, String> config;
    private List<String> events;
}
