package com.seojs.aisenpai_backend.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seojs.aisenpai_backend.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.seojs.aisenpai_backend.notification.entity.QNotification.notification;

@RequiredArgsConstructor
@Repository
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public void markAllAsRead(String loginId) {
        queryFactory.update(notification)
                .set(notification.isRead, true)
                .where(notification.githubAccount.loginId.eq(loginId)
                        .and(notification.isRead.isFalse()))
                .execute();
    }

    @Override
    public List<Notification> getNotifications(String loginId) {
        return queryFactory.selectFrom(notification)
                .join(notification.githubAccount).fetchJoin()
                .leftJoin(notification.pullRequest).fetchJoin()
                .where(notification.githubAccount.loginId.eq(loginId))
                .orderBy(notification.createdAt.desc())
                .fetch();
    }
}
