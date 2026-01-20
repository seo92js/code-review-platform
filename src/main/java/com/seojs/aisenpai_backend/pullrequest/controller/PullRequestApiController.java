package com.seojs.aisenpai_backend.pullrequest.controller;

import com.seojs.aisenpai_backend.github.dto.ChangedFileDto;
import com.seojs.aisenpai_backend.pullrequest.dto.PullRequestResponseDto;
import com.seojs.aisenpai_backend.pullrequest.service.PullRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PullRequestApiController {
    private final PullRequestService pullRequestService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/api/pull-requests")
    public List<PullRequestResponseDto> getPullRequestList(@RequestParam Long repositoryId) {
        return pullRequestService.getPullRequestList(repositoryId);
    }

    @GetMapping("/api/pull-request/changes")
    public List<ChangedFileDto> getPullRequestWithChanges(@AuthenticationPrincipal OAuth2User principal,
            @RequestParam Long repositoryId, @RequestParam Integer prNumber) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github",
                principal.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        return pullRequestService.getPullRequestWithChanges(repositoryId, prNumber, accessToken);
    }

    @PostMapping("/api/pull-request/review")
    public void reviewPullRequest(@AuthenticationPrincipal OAuth2User principal, @RequestParam Long repositoryId,
            @RequestParam Integer prNumber, @RequestParam(required = false) String model) {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient("github",
                principal.getName());
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        pullRequestService.review(repositoryId, prNumber, accessToken, model);
    }

    @GetMapping("/api/pull-request/review")
    public String getPullRequestReview(@RequestParam Long repositoryId, @RequestParam Integer prNumber) {
        return pullRequestService.getAiReview(repositoryId, prNumber);
    }
}