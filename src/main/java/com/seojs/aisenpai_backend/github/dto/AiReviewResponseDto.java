package com.seojs.aisenpai_backend.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponseDto {
    private String generalReview;
    private List<ReviewCommentDto> comments;
}
