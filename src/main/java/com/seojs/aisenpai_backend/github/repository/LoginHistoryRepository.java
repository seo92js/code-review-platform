package com.seojs.aisenpai_backend.github.repository;

import com.seojs.aisenpai_backend.github.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
}
