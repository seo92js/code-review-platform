package com.seojs.code_review_platform.pullrequest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto;
import com.seojs.code_review_platform.pullrequest.dto.PullRequestResponseDto;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest;
import com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PullRequestService {

    private final PullRequestRepository pullRequestRepository;
    private final ObjectMapper objectMapper;

    /**
     * PR 웹훅 이벤트를 처리하고 데이터베이스에 저장
     */
    @Transactional
    public void processAndSaveWebhook(String payload) {
        try {
            WebhookPayloadDto webhookPayload = objectMapper.readValue(payload, WebhookPayloadDto.class);
            String action = webhookPayload.getAction();
            
            // PR 관련 액션만 처리
            if (isPrAction(action)) {
                savePullRequest(webhookPayload);
            }
        } catch (Exception e) {
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    /**
     * 특정 저장소의 PR 목록 조회
     */
    @Transactional
    public List<PullRequestResponseDto> getPullRequestList(String ownerLogin, String repositoryName) {
        return pullRequestRepository.findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName).stream()
        .map(PullRequestResponseDto::fromEntity)
        .toList();
    }

    /**
     * PR 관련 액션인지 확인
     */
    private boolean isPrAction(String action) {
        return List.of("opened", "synchronize", "reopened", "closed", "merged").contains(action);
    }

    /**
     * PR 정보를 데이터베이스에 저장
     */
    private void savePullRequest(WebhookPayloadDto webhookPayload) {
        String repoName = webhookPayload.getRepository().getName();
        String ownerLogin = webhookPayload.getRepository().getOwner().getLogin();
        Integer prNumber = webhookPayload.getPullRequest().getNumber();
        String action = webhookPayload.getAction();
        String title = webhookPayload.getPullRequest().getTitle();

        PullRequest existingPr = pullRequestRepository
                .findByRepositoryNameAndOwnerLoginAndPrNumber(repoName, ownerLogin, prNumber)
                .orElse(null);

        if (existingPr != null) {
            updateExistingPullRequest(existingPr, action);
        } else {
            createNewPullRequest(repoName, ownerLogin, prNumber, action, title);
        }
    }

    /**
     * 기존 PR 업데이트
     */
    private void updateExistingPullRequest(PullRequest existingPr, String action) {
        existingPr.updateStatus(PullRequest.ReviewStatus.PENDING);
        existingPr.updateAction(action);
        
        pullRequestRepository.save(existingPr);
    }

    /**
     * 새 PR 생성
     */
    private void createNewPullRequest(String repoName, String ownerLogin, Integer prNumber, 
                                   String action, String title) {
        PullRequest newPr = PullRequest.builder()
                .repositoryName(repoName)
                .ownerLogin(ownerLogin)
                .prNumber(prNumber)
                .action(action)
                .title(title)
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        pullRequestRepository.save(newPr);
    }
} 