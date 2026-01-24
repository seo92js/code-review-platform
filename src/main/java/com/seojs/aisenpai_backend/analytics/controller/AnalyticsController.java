package com.seojs.aisenpai_backend.analytics.controller;

import com.seojs.aisenpai_backend.analytics.service.PageViewService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private static final String VISITOR_COOKIE_NAME = "_vid";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24;

    private final PageViewService pageViewService;

    @PostMapping("/pageview")
    public void recordPageView(
            @RequestBody PageViewRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String sessionId = getOrCreateSessionId(httpRequest, httpResponse);
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        pageViewService.recordPageView(sessionId, request.path(), ipAddress, userAgent);
    }

    private String getOrCreateSessionId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (VISITOR_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String newSessionId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(VISITOR_COOKIE_NAME, newSessionId);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return newSessionId;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record PageViewRequest(String path) {
    }
}
