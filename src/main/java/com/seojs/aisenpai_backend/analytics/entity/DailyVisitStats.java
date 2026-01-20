package com.seojs.aisenpai_backend.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
public class DailyVisitStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate statDate;

    @Column(nullable = false)
    private Long pageViews;

    @Column(nullable = false)
    private Long uniqueVisitors;

    @Builder
    public DailyVisitStats(LocalDate statDate, Long pageViews, Long uniqueVisitors) {
        this.statDate = statDate;
        this.pageViews = pageViews;
        this.uniqueVisitors = uniqueVisitors;
    }
}
