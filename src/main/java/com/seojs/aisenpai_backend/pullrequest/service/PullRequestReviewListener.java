package com.seojs.aisenpai_backend.pullrequest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.aisenpai_backend.ai.service.AiService;
import com.seojs.aisenpai_backend.github.dto.ChangedFileDto;
import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import com.seojs.aisenpai_backend.github.service.GithubService;
import com.seojs.aisenpai_backend.pullrequest.dto.ReviewRequestDto;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
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
        String model = dto.getModel();

        GithubAccount githubAccount = githubService.findByLoginIdOrThrow(loginId);
        String systemPrompt = githubAccount.getAiSettings().buildSystemPrompt();
        String openApiKey = githubService.getOpenAiKey(loginId);

        try {
            String userPrompt = objectMapper.writeValueAsString(changedFiles);
            String review = aiService.callAiChat(openApiKey, systemPrompt, userPrompt, model, null);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber, review, ReviewStatus.COMPLETED);
        } catch (JsonProcessingException e) {
            log.error("json processing failed - loginId: {}, repo: {}, pr: {}", loginId, repositoryName, prNumber, e);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber,
                    "AI review failed: Json processing failed", ReviewStatus.FAILED);
        } catch (IllegalArgumentException e) {
            log.error("invalid api configuration - loginId: {}, repo: {}, pr: {}", loginId, repositoryName, prNumber,
                    e);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber,
                    "AI review failed: Invalid API configuration", ReviewStatus.FAILED);
        } catch (NonTransientAiException e) {
            log.error("invalid api key error - loginId: {}, repo: {}, pr: {}", loginId, repositoryName, prNumber, e);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber,
                    "AI review failed: Invalid API key", ReviewStatus.FAILED);
        } catch (Exception e) {
            log.error("unexpected error - loginId: {}, repo: {}, pr: {}", loginId, repositoryName, prNumber, e);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber,
                    "AI review failed: Unexpected error", ReviewStatus.FAILED);
        }
    }
}
