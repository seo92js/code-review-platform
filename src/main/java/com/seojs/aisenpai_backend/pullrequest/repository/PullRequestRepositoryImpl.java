package com.seojs.aisenpai_backend.pullrequest.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import static com.seojs.aisenpai_backend.github.entity.QGithubAccount.githubAccount;
import static com.seojs.aisenpai_backend.pullrequest.entity.QPullRequest.pullRequest;

@RequiredArgsConstructor
public class PullRequestRepositoryImpl implements PullRequestRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsOpenPrByLoginIdAndRepositoryName(String loginId, String repositoryName) {
        Integer result = queryFactory.selectOne()
                .from(pullRequest)
                .join(pullRequest.githubAccount, githubAccount)
                .where(
                        githubAccount.loginId.eq(loginId),
                        pullRequest.repositoryName.eq(repositoryName),
                        pullRequest.action.notIn("closed", "merged")
                ).fetchFirst();

        return result != null;
    }
}
