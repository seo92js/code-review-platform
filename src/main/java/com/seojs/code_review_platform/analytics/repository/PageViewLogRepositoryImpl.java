package com.seojs.code_review_platform.analytics.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import static com.seojs.code_review_platform.analytics.entity.QPageViewLog.pageViewLog;

@RequiredArgsConstructor
public class PageViewLogRepositoryImpl implements PageViewLogRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Long countByViewedAtBetween(LocalDateTime start, LocalDateTime end) {
        Long count = queryFactory.select(pageViewLog.count())
                .from(pageViewLog)
                .where(
                        pageViewLog.viewedAt.goe(start),
                        pageViewLog.viewedAt.lt(end))
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public Long countDistinctSessionIdByViewedAtBetween(LocalDateTime start, LocalDateTime end) {
        Long count = queryFactory.select(pageViewLog.sessionId.countDistinct())
                .from(pageViewLog)
                .where(
                        pageViewLog.viewedAt.goe(start),
                        pageViewLog.viewedAt.lt(end))
                .fetchOne();
        return count != null ? count : 0L;
    }
}
