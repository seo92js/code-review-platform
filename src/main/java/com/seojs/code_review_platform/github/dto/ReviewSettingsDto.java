package com.seojs.code_review_platform.github.dto;

import com.seojs.code_review_platform.github.entity.DetailLevel;
import com.seojs.code_review_platform.github.entity.ReviewFocus;
import com.seojs.code_review_platform.github.entity.ReviewTone;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSettingsDto {
    private ReviewTone tone;
    private ReviewFocus focus;
    private DetailLevel detailLevel;
    private String customInstructions;
}
