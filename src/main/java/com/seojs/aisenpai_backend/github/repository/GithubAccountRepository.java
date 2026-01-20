package com.seojs.aisenpai_backend.github.repository;

import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GithubAccountRepository extends JpaRepository<GithubAccount, Long> {
    Optional<GithubAccount> findByLoginId(String loginId);
}

