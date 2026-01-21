package com.seojs.aisenpai_backend.notification.controller;

import com.seojs.aisenpai_backend.notification.dto.NotificationResponseDto;
import com.seojs.aisenpai_backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponseDto> getNotifications(@AuthenticationPrincipal OAuth2User principal) {
        String loginId = principal.getAttribute("login");
        return notificationService.getNotifications(loginId);
    }

    @PutMapping("/read-all")
    public void markAllAsRead(@AuthenticationPrincipal OAuth2User principal) {
        String loginId = principal.getAttribute("login");
        notificationService.markAllAsRead(loginId);
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal) {
        String loginId = principal.getAttribute("login");
        notificationService.markAsRead(id, loginId);
    }
}
