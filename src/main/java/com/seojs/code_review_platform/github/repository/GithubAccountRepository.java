package com.seojs.code_review_platform.github.repository;

import com.seojs.code_review_platform.github.entity.GithubAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GithubAccountRepository extends JpaRepository<GithubAccount, Long> {
    Optional<GithubAccount> findByLoginId(String loginId);
}

