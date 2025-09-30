package com.seojs.code_review_platform.pullrequest.service;

import com.seojs.code_review_platform.exception.PullRequestNotFoundEx;
import com.seojs.code_review_platform.exception.WebhookProcessingEx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.service.GithubService;
import com.seojs.code_review_platform.github.service.WebhookSecurityService;
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
    private final GithubService githubService;
    private final WebhookSecurityService webhookSecurityService;
    private final ObjectMapper objectMapper;

    /**
     * PR 웹훅 이벤트를 처리하고 데이터베이스에 저장
     */
    @Transactional
    public void processAndSaveWebhook(String payload, String signature) {
        webhookSecurityService.validateWebhookSignature(payload, signature);
        processWebhookPayload(payload);
    }
    
    /**
     * 웹훅 페이로드 처리
     */
    private void processWebhookPayload(String payload) {
        try {
            WebhookPayloadDto webhookPayload = objectMapper.readValue(payload, WebhookPayloadDto.class);
            String action = webhookPayload.getAction();
            
            // PR 관련 액션만 처리
            if (isPrAction(action)) {
                savePullRequest(webhookPayload);
            }
        } catch (Exception e) {
            throw new WebhookProcessingEx("Webhook processing failed", e);
        }
    }

    /**
     * 특정 저장소의 PR 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PullRequestResponseDto> getPullRequestList(String loginId, String repositoryName) {
        return pullRequestRepository.findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName).stream()
        .map(PullRequestResponseDto::fromEntity)
        .toList();
    }

    /**
     * PR 변경된 파일 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ChangedFileDto> getPullRequestWithChanges(String loginId, String repositoryName, Integer prNumber, String accessToken) {
        findByRepositoryNameAndGithubAccountLoginIdAndPrNumberOrThrow(repositoryName, loginId, prNumber);

        List<ChangedFileDto> changedFiles = githubService.getChangedFiles(accessToken, loginId, repositoryName, prNumber);

        return changedFiles;
    }

    /**
     * 특정 저장소의 특정 PR 번호로 조회 - 존재하지 않으면 예외 발생
     */
    @Transactional(readOnly = true)
    public PullRequest findByRepositoryNameAndGithubAccountLoginIdAndPrNumberOrThrow(String repositoryName, String loginId, Integer prNumber) {
        return pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber)
            .orElseThrow(() -> new PullRequestNotFoundEx("Pull request not found for repositoryName: " + repositoryName + ", loginId: " + loginId + ", prNumber: " + prNumber));
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
        String loginId = webhookPayload.getRepository().getOwner().getLogin();
        Integer prNumber = webhookPayload.getPullRequest().getNumber();
        String action = webhookPayload.getAction();
        String title = webhookPayload.getPullRequest().getTitle();

        PullRequest existingPr = pullRequestRepository
                .findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repoName, loginId, prNumber)
                .orElse(null);

        if (existingPr != null) {
            updateExistingPullRequest(existingPr, action);
        } else {
            createNewPullRequest(repoName, loginId, prNumber, action, title);
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
    private void createNewPullRequest(String repoName, String loginId, Integer prNumber, String action, String title) {
        GithubAccount githubAccount = githubService.findByLoginIdOrThrow(loginId);

        PullRequest newPr = PullRequest.builder()
                .repositoryName(repoName)
                .githubAccount(githubAccount)
                .prNumber(prNumber)
                .action(action)
                .title(title)
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        pullRequestRepository.save(newPr);
    }
} 