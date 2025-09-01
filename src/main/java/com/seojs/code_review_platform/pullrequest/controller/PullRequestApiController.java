package com.seojs.code_review_platform.pullrequest.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.seojs.code_review_platform.pullrequest.dto.PullRequestResponseDto;
import com.seojs.code_review_platform.pullrequest.service.PullRequestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PullRequestApiController {
    private final PullRequestService pullRequestService;

    @GetMapping("/api/pull-requests")
    public List<PullRequestResponseDto> getPullRequestList(@AuthenticationPrincipal OAuth2User principal, @RequestParam String repositoryName) {
        String ownerLogin = principal.getAttribute("login");
        return pullRequestService.getPullRequestList(ownerLogin, repositoryName);
    }
}