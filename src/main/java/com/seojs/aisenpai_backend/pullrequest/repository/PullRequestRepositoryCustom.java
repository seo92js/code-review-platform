package com.seojs.aisenpai_backend.pullrequest.repository;

public interface PullRequestRepositoryCustom {
    /**
     * closed 나 merged 가 아닌 pr이 있는지 조회
     */
    boolean existsOpenPrByLoginIdAndRepositoryName(String loginId, String repositoryName);
}
