package com.seojs.aisenpai_backend.pullrequest.service;

import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import com.seojs.aisenpai_backend.github.service.GithubService;
import com.seojs.aisenpai_backend.notification.entity.NotificationType;
import com.seojs.aisenpai_backend.notification.service.NotificationService;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest.ReviewStatus;
import com.seojs.aisenpai_backend.pullrequest.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PullRequestManager {

    private final PullRequestRepository pullRequestRepository;
    private final GithubService githubService;
    private final NotificationService notificationService;

    /**
     * 새 PR 생성 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PullRequest createNewPullRequest(Long repoId, String repoName, String loginId, Integer prNumber,
            String action,
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

        PullRequest savedPr = pullRequestRepository.save(newPr);

        notificationService.createNotification(
                githubAccount,
                NotificationType.NEW_PR,
                savedPr);

        return savedPr;
    }

    /**
     * 기존 PR 업데이트 (별도 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PullRequest updateExistingPullRequest(PullRequest existingPr, String action) {
        ReviewStatus currentStatus = existingPr.getStatus();

        // COMPLETED, FAILED 상태에서 새 변경사항이 있으면 NEW_CHANGES로 변경
        if (currentStatus == ReviewStatus.COMPLETED || currentStatus == ReviewStatus.FAILED) {
            existingPr.updateStatus(ReviewStatus.NEW_CHANGES);
        }
        // PENDING, IN_PROGRESS, NEW_CHANGES는 상태 유지

        existingPr.updateAction(action);
        return pullRequestRepository.save(existingPr);
    }
}
