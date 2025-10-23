package com.seojs.code_review_platform.pullrequest.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.seojs.code_review_platform.pullrequest.dto.PullRequestResponseDto;
import com.seojs.code_review_platform.pullrequest.service.PullRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PullRequestApiController {
    private final PullRequestService pullRequestService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    
    @GetMapping("/api/pull-requests")
    public List<PullRequestResponseDto> getPullRequestList(@AuthenticationPrincipal OAuth2User principal, @RequestParam String repositoryName) {
        String loginId = principal.getAttribute("login");
        return pullRequestService.getPullRequestList(loginId, repositoryName);
    }

    @GetMapping("/api/pull-request/changes")
    public List<ChangedFileDto> getPullRequestWithChanges(@AuthenticationPrincipal OAuth2User principal, @RequestParam String repository, @RequestParam Integer prNumber) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principal.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String loginId = principal.getAttribute("login");

        return pullRequestService.getPullRequestWithChanges(loginId, repository, prNumber, accessToken);
    }

    @PostMapping("/api/pull-request/review")
    public void reviewPullRequest(@AuthenticationPrincipal OAuth2User principal, @RequestParam String repository, @RequestParam Integer prNumber) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github", principal.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        String loginId = principal.getAttribute("login");

        pullRequestService.review(loginId, repository, prNumber, accessToken);
    }
}