package com.seojs.aisenpai_backend.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleSaveDto {
    private String content;
    private String targetFilePattern;
}
