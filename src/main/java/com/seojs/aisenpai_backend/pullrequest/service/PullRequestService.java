package com.seojs.aisenpai_backend.pullrequest.service;

import com.seojs.aisenpai_backend.exception.OpenAiKeyNotSetEx;
import com.seojs.aisenpai_backend.exception.PullRequestNotFoundEx;
import com.seojs.aisenpai_backend.exception.WebhookProcessingEx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.aisenpai_backend.github.dto.AiReviewResponseDto;
import com.seojs.aisenpai_backend.github.dto.ChangedFileDto;
import com.seojs.aisenpai_backend.github.dto.GithubReviewRequestDto;
import com.seojs.aisenpai_backend.github.dto.WebhookPayloadDto;
import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import com.seojs.aisenpai_backend.github.service.GithubService;
import com.seojs.aisenpai_backend.github.service.TokenEncryptionService;
import com.seojs.aisenpai_backend.github.service.WebhookSecurityService;
import com.seojs.aisenpai_backend.notification.entity.NotificationType;
import com.seojs.aisenpai_backend.notification.service.NotificationService;
import com.seojs.aisenpai_backend.pullrequest.dto.PullRequestResponseDto;
import com.seojs.aisenpai_backend.pullrequest.dto.ReviewRequestDto;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest.ReviewStatus;
import com.seojs.aisenpai_backend.pullrequest.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PullRequestService {
    private final PullRequestRepository pullRequestRepository;
    private final GithubService githubService;
    private final WebhookSecurityService webhookSecurityService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TokenEncryptionService tokenEncryptionService;
    private final NotificationService notificationService;

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
     * 특정 저장소의 PR 목록 조회 (owner/repo 기준 - GitHub API로 repositoryId 조회 후 사용)
     */
    @Transactional(readOnly = true)
    public List<PullRequestResponseDto> getPullRequestList(String owner, String repo, String accessToken) {
        Long repositoryId = githubService.getRepositoryId(accessToken, owner, repo);
        return pullRequestRepository
                .findByRepositoryIdOrderByUpdatedAtDesc(repositoryId).stream()
                .map(PullRequestResponseDto::fromEntity)
                .toList();
    }

    /**
     * PR 변경된 파일 목록 조회 (owner/repo 기준 - GitHub API로 repositoryId 조회 후 사용)
     */
    @Transactional(readOnly = true)
    public List<ChangedFileDto> getPullRequestWithChanges(String owner, String repo, Integer prNumber,
            String accessToken) {
        Long repositoryId = githubService.getRepositoryId(accessToken, owner, repo);
        PullRequest pr = findByRepositoryIdAndPrNumberOrThrow(repositoryId, prNumber);
        return githubService.getChangedFiles(accessToken, owner, repo, prNumber);
    }

    /**
     * 특정 저장소의 특정 PR 번호로 조회 - 존재하지 않으면 예외 발생
     */
    private PullRequest findByRepositoryIdAndPrNumberOrThrow(Long repositoryId, Integer prNumber) {
        return pullRequestRepository
                .findByRepositoryIdAndPrNumber(repositoryId, prNumber)
                .orElseThrow(() -> new PullRequestNotFoundEx("Pull request not found for repositoryId: "
                        + repositoryId + ", prNumber: " + prNumber));
    }

    /**
     * ai 리뷰 시작
     */
    @Transactional
    public void review(String owner, String repo, Integer prNumber, String accessToken, String model) {
        Long repositoryId = githubService.getRepositoryId(accessToken, owner, repo);
        PullRequest pr = findByRepositoryIdAndPrNumberOrThrow(repositoryId, prNumber);

        List<ChangedFileDto> changedFiles = githubService.getChangedFiles(accessToken, owner, repo, prNumber);

        GithubAccount githubAccount = pr.getGithubAccount();

        if (githubAccount.getAiSettings().getOpenAiKey() == null
                || githubAccount.getAiSettings().getOpenAiKey().isEmpty()) {
            throw new OpenAiKeyNotSetEx("OpenAI API key is not set. Please set it in the settings.");
        }

        List<String> ignorePatterns = githubAccount.getAiSettings().getIgnorePatternsAsList();
        List<ChangedFileDto> filteredFiles = changedFiles;

        if (!ignorePatterns.isEmpty()) {
            List<PathMatcher> matchers = ignorePatterns.stream()
                    .map(this::convertUserPatternToGlob)
                    .map(pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                    .toList();

            filteredFiles = changedFiles.stream()
                    .filter(file -> matchers.stream()
                            .noneMatch(matcher -> matcher.matches(Paths.get(file.getFilename()))))
                    .toList();
        }

        pr.updateStatus(ReviewStatus.IN_PROGRESS);

        // LLM 호출은 이벤트 리스너에서 수행
        String systemPrompt = githubAccount.getAiSettings().buildSystemPrompt();
        String encryptedOpenAiKey = githubAccount.getAiSettings().getOpenAiKey();
        eventPublisher.publishEvent(
                new ReviewRequestDto(repositoryId, prNumber, filteredFiles, model, systemPrompt, encryptedOpenAiKey));
    }

    /**
     * ai 리뷰 결과 업데이트
     */
    @Transactional
    public void updateAiReview(Long repositoryId, Integer prNumber, String aiReview,
            ReviewStatus status) {
        PullRequest pr = findByRepositoryIdAndPrNumberOrThrow(repositoryId, prNumber);
        pr.updateAiReview(aiReview);
        pr.updateStatus(status);

        GithubAccount account = pr.getGithubAccount();

        if (status == ReviewStatus.COMPLETED) {
            notificationService.createNotification(
                    account,
                    NotificationType.REVIEW_COMPLETE,
                    pr);

            // GitHub PR에 댓글 자동 게시
            if (Boolean.TRUE.equals(account.getAiSettings().getAutoPostToGithub())) {
                try {
                    String accessToken = tokenEncryptionService.decryptToken(account.getAccessToken());

                    try {
                        // Markdown 코드 블록 제거 (본문의 코드 블록은 유지하고 감싸고 있는 태그만 제거)
                        String sanitizedAiReview = aiReview.trim();
                        if (sanitizedAiReview.startsWith("```json")) {
                            sanitizedAiReview = sanitizedAiReview.substring(7);
                        } else if (sanitizedAiReview.startsWith("```")) {
                            sanitizedAiReview = sanitizedAiReview.substring(3);
                        }
                        if (sanitizedAiReview.endsWith("```")) {
                            sanitizedAiReview = sanitizedAiReview.substring(0, sanitizedAiReview.length() - 3);
                        }
                        sanitizedAiReview = sanitizedAiReview.trim();

                        AiReviewResponseDto aiResponse = objectMapper.readValue(
                                sanitizedAiReview, AiReviewResponseDto.class);

                        // 코멘트가 있을 경우
                        if (aiResponse.getComments() != null && !aiResponse.getComments().isEmpty()) {
                            GithubReviewRequestDto reviewRequest = GithubReviewRequestDto
                                    .builder()
                                    .body(aiResponse.getGeneralReview()) // 총평
                                    .event("COMMENT") // 기본값 COMMENT
                                    .comments(aiResponse.getComments()) // 인라인 코멘트 리스트
                                    .build();

                            try {
                                githubService.postPRReview(accessToken, account.getLoginId(), pr.getRepositoryName(),
                                        prNumber, reviewRequest);
                            } catch (Exception e) {
                                log.warn("Failed to post inline review: {}. Falling back to general comment.",
                                        e.getMessage());
                                // 인라인 코멘트 실패 시 (예: 라인 번호 불일치) 일반 코멘트로 Fallback
                                String fallbackBody = aiResponse.getGeneralReview() + "\n\n### 상세 코멘트 (전환됨)\n";
                                for (var comment : aiResponse.getComments()) {
                                    fallbackBody += String.format("- **%s (Line %d)**: %s\n",
                                            comment.getPath(), comment.getLine(), comment.getBody());
                                }
                                String formattedReview = formatReviewForGithub(fallbackBody);
                                githubService.postPRComment(accessToken, account.getLoginId(), pr.getRepositoryName(),
                                        prNumber, formattedReview);
                            }
                        } else {
                            String body = aiResponse.getGeneralReview() != null ? aiResponse.getGeneralReview()
                                    : aiReview;
                            String formattedReview = formatReviewForGithub(body);
                            githubService.postPRComment(accessToken, account.getLoginId(), pr.getRepositoryName(),
                                    prNumber, formattedReview);
                        }

                    } catch (Exception e) {
                        log.warn("Failed to parse AI review as JSON, falling back to comment. Error: {}",
                                e.getMessage());
                        String formattedReview = formatReviewForGithub(aiReview);
                        githubService.postPRComment(accessToken, account.getLoginId(), pr.getRepositoryName(), prNumber,
                                formattedReview);
                    }
                } catch (Exception e) {
                    log.warn("Failed to post review to GitHub PR #{}: {}", prNumber, e.getMessage());
                }
            }
        } else if (status == ReviewStatus.FAILED) {
            notificationService.createNotification(
                    account,
                    NotificationType.REVIEW_FAILED,
                    pr);
        }
    }

    /**
     * GitHub 댓글용 리뷰 포맷팅
     */
    private String formatReviewForGithub(String aiReview) {
        return "## 🤖 AI Code Review by AISenpai\n\n" + aiReview
                + "\n\n---\n*Powered by [AISenpai](https://aisenpai.dev)*";
    }

    /**
     * ai 리뷰 결과 조회
     */
    @Transactional(readOnly = true)
    public String getAiReview(String owner, String repo, Integer prNumber, String accessToken) {
        Long repositoryId = githubService.getRepositoryId(accessToken, owner, repo);
        PullRequest pr = findByRepositoryIdAndPrNumberOrThrow(repositoryId, prNumber);
        return pr.getAiReview();
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
        Long repoId = webhookPayload.getRepository().getId();
        String repoName = webhookPayload.getRepository().getName();
        String loginId = webhookPayload.getRepository().getOwner().getLogin();
        Integer prNumber = webhookPayload.getPullRequest().getNumber();
        String action = webhookPayload.getAction();
        String title = webhookPayload.getPullRequest().getTitle();

        PullRequest existingPr = pullRequestRepository
                .findByRepositoryIdAndPrNumber(repoId, prNumber)
                .orElse(null);

        if (existingPr != null) {
            updateExistingPullRequest(existingPr, action);
        } else {
            createNewPullRequest(repoId, repoName, loginId, prNumber, action, title);
        }
    }

    /**
     * 기존 PR 업데이트
     */
    private void updateExistingPullRequest(PullRequest existingPr, String action) {
        ReviewStatus currentStatus = existingPr.getStatus();

        // COMPLETED, FAILED 상태에서 새 변경사항이 있으면 NEW_CHANGES로 변경
        if (currentStatus == ReviewStatus.COMPLETED || currentStatus == ReviewStatus.FAILED) {
            existingPr.updateStatus(ReviewStatus.NEW_CHANGES);
        }
        // PENDING, IN_PROGRESS, NEW_CHANGES는 상태 유지

        existingPr.updateAction(action);
        pullRequestRepository.save(existingPr);
    }

    /**
     * 사용자 입력 패턴을 Glob 패턴으로 변환 (gitignore 스타일 지원)
     */
    private String convertUserPatternToGlob(String pattern) {
        pattern = pattern.trim();
        boolean isDirectory = pattern.endsWith("/");
        if (isDirectory) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }

        boolean isRooted = pattern.startsWith("/");
        if (isRooted) {
            pattern = pattern.substring(1);
        }

        boolean hasSlash = pattern.contains("/");

        StringBuilder glob = new StringBuilder();

        if (!isRooted && !hasSlash) {
            glob.append("{**/,}");
        }

        glob.append(pattern);

        if (isDirectory) {
            glob.append("/**");
        } else {
            glob.append("{,/**}");
        }

        return glob.toString();
    }

    /**
     * 새 PR 생성
     */
    private void createNewPullRequest(Long repoId, String repoName, String loginId, Integer prNumber, String action,
            String title) {
        GithubAccount githubAccount = githubService.findByLoginIdOrThrow(loginId);

        PullRequest newPr = PullRequest.builder()
                .repositoryId(repoId)
                .repositoryName(repoName)
                .githubAccount(githubAccount)
                .prNumber(prNumber)
                .action(action)
                .title(title)
                .status(ReviewStatus.PENDING)
                .build();

        pullRequestRepository.save(newPr);

        notificationService.createNotification(
                githubAccount,
                NotificationType.NEW_PR,
                newPr);

        if (Boolean.TRUE.equals(githubAccount.getAiSettings().getAutoReviewEnabled())) {
            try {
                String accessToken = tokenEncryptionService.decryptToken(githubAccount.getAccessToken());
                String model = githubAccount.getAiSettings().getOpenaiModel();
                review(loginId, repoName, prNumber, accessToken, model);
                log.info("Auto review triggered for PR #{} in {}/{}", prNumber, loginId, repoName);
            } catch (Exception e) {
                log.warn("Auto review failed for PR #{} in {}/{}: {}", prNumber, loginId, repoName, e.getMessage());
            }
        }
    }
}