package com.seojs.code_review_platform.controller;

import com.seojs.code_review_platform.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.service.GithubService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class GithubApiController {
    private final GithubService githubService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/api/github/repositories")
    public List<GitRepositoryResponseDto> getRepositories(@AuthenticationPrincipal OAuth2User principal) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                "github", principal.getName());

        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        return githubService.getRepositories(accessToken);
    }
}
