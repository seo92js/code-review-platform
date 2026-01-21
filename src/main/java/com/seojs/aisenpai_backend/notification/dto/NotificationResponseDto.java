package com.seojs.aisenpai_backend.notification.dto;

import com.seojs.aisenpai_backend.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    private Long id;
    private String loginId;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String prTitle;
    private Integer prNumber;
    private String repositoryName;
}
