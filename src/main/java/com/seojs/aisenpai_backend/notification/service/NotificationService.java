package com.seojs.aisenpai_backend.notification.service;

import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest;
import com.seojs.aisenpai_backend.notification.dto.NotificationResponseDto;

import com.seojs.aisenpai_backend.notification.entity.Notification;
import com.seojs.aisenpai_backend.notification.entity.NotificationType;
import com.seojs.aisenpai_backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /**
     * 알림 생성
     */
    @Transactional
    public void createNotification(GithubAccount githubAccount, NotificationType type, PullRequest pullRequest) {
        Notification notification = Notification.builder()
                .githubAccount(githubAccount)
                .type(type)
                .pullRequest(pullRequest)
                .build();
        notificationRepository.save(notification);
        log.info("Notification created for user: {}", githubAccount.getLoginId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(String loginId) {
        List<Notification> notifications = notificationRepository.getNotifications(loginId);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private NotificationResponseDto convertToDto(Notification notification) {
        String prTitle = null;
        Integer prNumber = null;
        String repositoryName = null;

        if (notification.getPullRequest() != null) {
            prTitle = notification.getPullRequest().getTitle();
            prNumber = notification.getPullRequest().getPrNumber();
            repositoryName = notification.getPullRequest().getRepositoryName();
        }

        return NotificationResponseDto.builder()
                .id(notification.getId())
                .loginId(notification.getGithubAccount().getLoginId())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .prTitle(prTitle)
                .prNumber(prNumber)
                .repositoryName(repositoryName)
                .build();
    }

    /**
     * 알림 모두 읽음 처리
     */
    @Transactional
    public void markAllAsRead(String loginId) {
        notificationRepository.markAllAsRead(loginId);
    }
}
