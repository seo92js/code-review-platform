package com.seojs.code_review_platform.pullrequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.ai.service.AiService;
import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.service.GithubService;
import com.seojs.code_review_platform.pullrequest.dto.ReviewRequestDto;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PullRequestReviewListener {
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final PullRequestService pullRequestService;
    private final GithubService githubService;

    @Async
    @EventListener
    public void handleReviewRequested(ReviewRequestDto dto) {
        List<ChangedFileDto> changedFiles = dto.getChangedFiles();
        String loginId = dto.getLoginId();
        String repositoryName = dto.getRepositoryName();
        Integer prNumber = dto.getPrNumber();

        GithubAccount githubAccount = githubService.findByLoginIdOrThrow(loginId);
        String systemPrompt = githubAccount.getSystemPrompt();
        String openApiKey = githubAccount.getOpenAiKey();

        try {
            String userPrompt = objectMapper.writeValueAsString(changedFiles);
            String review = aiService.callAiChat(openApiKey, systemPrompt, userPrompt);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber, review, ReviewStatus.COMPLETED);
        } catch (Exception e) {
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber, "AI review failed", ReviewStatus.FAILED);
        }
    }
}
