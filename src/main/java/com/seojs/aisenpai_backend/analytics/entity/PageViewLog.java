package com.seojs.aisenpai_backend.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
public class PageViewLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sessionId;

    @Column(length = 500)
    private String path;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Builder
    public PageViewLog(String sessionId, String path, String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.path = path;
        this.viewedAt = LocalDateTime.now();
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }
}
