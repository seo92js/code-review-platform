package com.seojs.code_review_platform.pullrequest.repository;

public interface PullRequestRepositoryCustom {
    /**
     * closed 나 merged 가 아닌 pr이 있는지 조회
     */
    boolean existsOpenPrByLoginIdAndRepositoryName(String loginId, String repositoryName);
}
