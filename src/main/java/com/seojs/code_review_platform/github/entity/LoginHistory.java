package com.seojs.code_review_platform.github.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class LoginHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String loginId;

    @Column(nullable = false)
    private LocalDateTime loginAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Builder
    public LoginHistory(String loginId, String ipAddress, String userAgent) {
        this.loginId = loginId;
        this.loginAt = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
