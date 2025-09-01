package com.seojs.code_review_platform.pullrequest.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class PullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer prNumber;

    @Column(nullable = false)
    private String repositoryName;

    @Column(nullable = false)
    private String ownerLogin;

    private String title;

    @Column(nullable = false)
    private String action;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    private Integer changedFilesCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ReviewStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReviewStatus {
        PENDING,    // 리뷰 대기 중
        COMPLETED,   // 리뷰 완료
    }

    public void updateStatus(ReviewStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateChangedFilesCount(int count) {
        this.changedFilesCount = count;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAction(String action) {
        this.action = action;
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public PullRequest(Integer prNumber, String repositoryName, String ownerLogin, String title, String action, ReviewStatus status, Integer changedFilesCount) {
        this.prNumber = prNumber;
        this.repositoryName = repositoryName;
        this.ownerLogin = ownerLogin;
        this.title = title;
        this.action = action;
        this.status = status;
        this.changedFilesCount = changedFilesCount != null ? changedFilesCount : 0;
    }
} 