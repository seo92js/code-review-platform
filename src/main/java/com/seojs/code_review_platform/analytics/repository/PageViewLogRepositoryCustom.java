package com.seojs.code_review_platform.analytics.repository;

import java.time.LocalDateTime;

public interface PageViewLogRepositoryCustom {
    Long countByViewedAtBetween(LocalDateTime start, LocalDateTime end);

    Long countDistinctSessionIdByViewedAtBetween(LocalDateTime start, LocalDateTime end);
}
