package com.seojs.aisenpai_backend.github.dto;

import com.seojs.aisenpai_backend.github.entity.DetailLevel;
import com.seojs.aisenpai_backend.github.entity.ReviewFocus;
import com.seojs.aisenpai_backend.github.entity.ReviewTone;
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
    private Boolean autoReviewEnabled;
    private String openaiModel;
}
