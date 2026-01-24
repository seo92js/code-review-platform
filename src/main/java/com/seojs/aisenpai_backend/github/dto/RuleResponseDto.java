package com.seojs.aisenpai_backend.github.dto;

import com.seojs.aisenpai_backend.github.entity.Rule;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResponseDto {
    private Long id;
    private String content;
    @JsonProperty("isEnabled")
    private boolean isEnabled;
    private String targetFilePattern;

    public static RuleResponseDto from(Rule rule) {
        return RuleResponseDto.builder()
                .id(rule.getId())
                .content(rule.getContent())
                .isEnabled(rule.isEnabled())
                .targetFilePattern(rule.getTargetFilePattern())
                .build();
    }
}
