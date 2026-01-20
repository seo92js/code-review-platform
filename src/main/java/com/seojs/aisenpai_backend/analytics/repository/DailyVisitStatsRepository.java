package com.seojs.aisenpai_backend.analytics.repository;

import com.seojs.aisenpai_backend.analytics.entity.DailyVisitStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyVisitStatsRepository extends JpaRepository<DailyVisitStats, Long> {
    Optional<DailyVisitStats> findByStatDate(LocalDate statDate);
}
