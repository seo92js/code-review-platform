package com.seojs.code_review_platform.github.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor
@Entity
public class GithubAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String accessToken;

    @Column(nullable = false)
    private String webhookSecret;

    @OneToOne(mappedBy = "githubAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private AiReviewSettings aiSettings;

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * AiReviewSettings 설정
     */
    public void initializeAiSettings() {
        if (this.aiSettings == null) {
            this.aiSettings = AiReviewSettings.builder()
                    .githubAccount(this)
                    .build();
        }
    }

    /**
     * AiReviewSettings 안전하게 가져오기
     * 설정이 없으면 새로 생성하고 초기화
     */
    public AiReviewSettings getAiSettings() {
        if (this.aiSettings == null) {
            initializeAiSettings();
        }
        return this.aiSettings;
    }

    @Builder
    public GithubAccount(String loginId, String accessToken, String webhookSecret) {
        this.loginId = loginId;
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
    }
}
