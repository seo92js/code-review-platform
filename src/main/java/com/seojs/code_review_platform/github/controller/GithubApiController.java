package com.seojs.code_review_platform.github.controller;

import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
public class GithubApiController {
    private final GithubService githubService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/api/github/status")
    public boolean getLoginStatus(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return false;
        }

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principal.getName());

        return authorizedClient != null && authorizedClient.getAccessToken() != null;
    }

    @GetMapping("/api/github/username")
    public String getUsername(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttribute("login");
    }

    @GetMapping("/api/github/repositories")
    public List<GitRepositoryResponseDto> getRepositories(@AuthenticationPrincipal OAuth2User principal) throws IOException {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principal.getName());

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        return githubService.getRepositories(accessToken);
    }
}
