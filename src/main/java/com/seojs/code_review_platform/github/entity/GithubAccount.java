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

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Builder
    public GithubAccount(String loginId, String accessToken, String webhookSecret) {
        this.loginId = loginId;
        this.accessToken = accessToken;
        this.webhookSecret = webhookSecret;
    }
}

