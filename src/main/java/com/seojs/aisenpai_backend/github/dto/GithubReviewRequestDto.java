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
public class GithubReviewRequestDto {
    private String body;
    private String event;
    private List<ReviewCommentDto> comments;
}
