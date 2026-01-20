package com.seojs.code_review_platform.analytics.service;

import com.seojs.code_review_platform.analytics.entity.DailyVisitStats;
import com.seojs.code_review_platform.analytics.repository.DailyVisitStatsRepository;
import com.seojs.code_review_platform.analytics.repository.PageViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsAggregationService {

    private final PageViewLogRepository pageViewLogRepository;
    private final DailyVisitStatsRepository dailyVisitStatsRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void aggregateYesterdayStats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        aggregateStatsForDate(yesterday);
    }

    @Transactional
    public void aggregateStatsForDate(LocalDate date) {
        if (dailyVisitStatsRepository.findByStatDate(date).isPresent()) {
            log.info("Stats for {} already exists, skipping", date);
            return;
        }

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        Long pv = pageViewLogRepository.countByViewedAtBetween(start, end);
        Long uv = pageViewLogRepository.countDistinctSessionIdByViewedAtBetween(start, end);

        DailyVisitStats stats = DailyVisitStats.builder()
                .statDate(date)
                .pageViews(pv)
                .uniqueVisitors(uv)
                .build();

        dailyVisitStatsRepository.save(stats);
        log.info("Aggregated stats for {}: PV={}, UV={}", date, pv, uv);
    }
}
