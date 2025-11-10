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
    private boolean existsOpenPullRequest;

    @Builder
    GitRepositoryWithWebhookResponseDto(GitRepositoryResponseDto repository, boolean hasWebhook, boolean existsOpenPullRequest) {
        this.repository = repository;
        this.hasWebhook = hasWebhook;
        this.existsOpenPullRequest = existsOpenPullRequest;
    }
}
