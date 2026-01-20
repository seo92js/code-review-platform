package com.seojs.code_review_platform.analytics.repository;

import com.seojs.code_review_platform.analytics.entity.PageViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageViewLogRepository extends JpaRepository<PageViewLog, Long>, PageViewLogRepositoryCustom {
}
