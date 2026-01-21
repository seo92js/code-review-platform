package com.seojs.aisenpai_backend.notification.repository;

import com.seojs.aisenpai_backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
}
