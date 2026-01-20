package com.seojs.code_review_platform.github.repository;

import com.seojs.code_review_platform.github.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
}
