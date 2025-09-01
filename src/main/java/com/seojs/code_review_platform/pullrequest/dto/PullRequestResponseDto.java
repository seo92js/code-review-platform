package com.seojs.code_review_platform.pullrequest.dto;

import java.time.LocalDateTime;

import com.seojs.code_review_platform.pullrequest.entity.PullRequest;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PullRequestResponseDto {
    private Integer prNumber;
    private String title;
    private String action;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PullRequestResponseDto fromEntity(PullRequest pullRequest) {
        return new PullRequestResponseDto(
            pullRequest.getPrNumber(),
            pullRequest.getTitle(),
            pullRequest.getAction(),
            pullRequest.getStatus(),
            pullRequest.getCreatedAt(),
            pullRequest.getUpdatedAt()
        );
    }
}
