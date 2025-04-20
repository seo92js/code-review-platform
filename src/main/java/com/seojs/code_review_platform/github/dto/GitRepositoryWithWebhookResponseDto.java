package com.seojs.code_review_platform.github.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GitRepositoryWithWebhookResponseDto {
    private GitRepositoryResponseDto repository;
    private boolean hasWebhook;

    @Builder
    GitRepositoryWithWebhookResponseDto(GitRepositoryResponseDto repository, boolean hasWebhook) {
        this.repository = repository;
        this.hasWebhook = hasWebhook;
    }
}
