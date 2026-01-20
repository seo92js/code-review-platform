package com.seojs.aisenpai_backend.pullrequest.repository;

import com.seojs.aisenpai_backend.pullrequest.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, Long>, PullRequestRepositoryCustom {

    /**
     * 특정 저장소의 특정 PR 번호로 조회
     */
    Optional<PullRequest> findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(String repositoryName, String loginId, Integer prNumber);

    /**
     * 특정 저장소의 모든 PR 조회
     */
    List<PullRequest> findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(String loginId, String repositoryName);
} 