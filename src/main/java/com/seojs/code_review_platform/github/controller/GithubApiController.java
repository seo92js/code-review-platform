package com.seojs.code_review_platform.github.controller;

import com.seojs.code_review_platform.github.dto.GitRepositoryWithWebhookResponseDto;
import com.seojs.code_review_platform.github.service.GithubService;
import com.seojs.code_review_platform.pullrequest.service.PullRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
public class GithubApiController {
    private final GithubService githubService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final PullRequestService pullRequestWebhookService;

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
    public List<GitRepositoryWithWebhookResponseDto> getRepositories(@AuthenticationPrincipal OAuth2User principal) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principal.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        return githubService.getRepositoriesWithWebhookStatus(accessToken);
    }

    @PostMapping("/api/github/webhook/")
    public void handleWebhook(@RequestBody String payload, @RequestHeader("X-Github-Event") String event) {
        if (event.equals("pull_request")) {
            pullRequestWebhookService.processAndSaveWebhook(payload);
        }
    }

    @PostMapping("/api/github/register")
    public void registerWebhook(@AuthenticationPrincipal OAuth2User principal,
                               @RequestParam String repository) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principal.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String owner = principal.getAttribute("login");

        githubService.registerWebhook(accessToken, owner, repository);
    }
}
