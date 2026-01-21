package com.seojs.aisenpai_backend.notification.repository;

import com.seojs.aisenpai_backend.notification.entity.Notification;
import java.util.List;

public interface NotificationRepositoryCustom {
    void markAllAsRead(String loginId);

    List<Notification> getNotifications(String loginId);
}
