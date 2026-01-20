package com.seojs.aisenpai_backend.analytics.repository;

import com.seojs.aisenpai_backend.analytics.entity.PageViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageViewLogRepository extends JpaRepository<PageViewLog, Long>, PageViewLogRepositoryCustom {
}
