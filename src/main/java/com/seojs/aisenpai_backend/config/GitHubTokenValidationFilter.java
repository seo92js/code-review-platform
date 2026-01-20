package com.seojs.aisenpai_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class GitHubTokenValidationFilter extends OncePerRequestFilter {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GitHubTokenValidationFilter(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/h2-console") ||
                path.startsWith("/api/github/webhook") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/static/") ||
                path.startsWith("/public/") ||
                path.equals("/favicon.ico") ||
                path.equals("/robots.txt");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null &&
                authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof OAuth2User) {

            if (!isGitHubTokenValid()) {
                log.warn("GitHub token invalid for authenticated user. Clearing security context.");
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * GitHub 토큰 유효성 검증
     */
    private boolean isGitHubTokenValid() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
                return false;
            }

            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", oAuth2User.getName());

            return authorizedClient != null && authorizedClient.getAccessToken() != null;
        } catch (Exception e) {
            log.error("GitHub token validation failed", e);
            return false;
        }
    }
}
