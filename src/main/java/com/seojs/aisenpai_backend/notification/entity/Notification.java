package com.seojs.aisenpai_backend.notification.entity;

import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_account_id", nullable = false)
    private GithubAccount githubAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = true)
    private PullRequest pullRequest;

    @Column(nullable = false)
    private boolean isRead = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Notification(GithubAccount githubAccount, NotificationType type, PullRequest pullRequest) {
        this.githubAccount = githubAccount;
        this.type = type;
        this.pullRequest = pullRequest;
    }

    public void markAsRead() {
        this.isRead = true;
    }

}
