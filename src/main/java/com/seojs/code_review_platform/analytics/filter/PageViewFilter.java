package com.seojs.code_review_platform.analytics.filter;

import com.seojs.code_review_platform.analytics.service.PageViewService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PageViewFilter extends OncePerRequestFilter {

    private static final String VISITOR_COOKIE_NAME = "_vid";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24;

    private final PageViewService pageViewService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = getOrCreateSessionId(request, response);
        String path = request.getRequestURI();
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        pageViewService.recordPageView(sessionId, path, ipAddress, userAgent);

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (!"GET".equalsIgnoreCase(method)) {
            return true;
        }

        if (path.startsWith("/api/")) {
            return true;
        }

        if (path.matches(".*\\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$")) {
            return true;
        }

        return false;
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
}
