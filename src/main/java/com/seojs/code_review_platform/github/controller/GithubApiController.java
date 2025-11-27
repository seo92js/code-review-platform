package com.seojs.code_review_platform.github.controller;

import com.seojs.code_review_platform.github.dto.GitRepositoryWithWebhookResponseDto;
import com.seojs.code_review_platform.github.dto.OpenAiKeyDto;
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
    public void handleWebhook(@RequestBody String payload, 
                            @RequestHeader("X-Github-Event") String event, 
                            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        if (event.equals("pull_request")) {
            pullRequestWebhookService.processAndSaveWebhook(payload, signature);
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

    @GetMapping("/api/github/prompt")
    public String getSystemPrompt(@AuthenticationPrincipal OAuth2User principal) {
        String owner = principal.getAttribute("login");
        return githubService.getSystemPrompt(owner);
    }

    @PatchMapping("/api/github/prompt")
    public Long updateSystemPrompt(@AuthenticationPrincipal OAuth2User principal,
                                   @RequestParam String prompt) {
        String owner = principal.getAttribute("login");
        return githubService.updateSystemPrompt(owner, prompt);
    }

    @GetMapping("/api/github/ignore")
    public List<String> getIgnorePatterns(@AuthenticationPrincipal OAuth2User principal) {
        String owner = principal.getAttribute("login");
        return githubService.getIgnorePatterns(owner);
    }

    @PatchMapping("/api/github/ignore")
    public Long updateIgnorePatterns(@AuthenticationPrincipal OAuth2User principal,
                                     @RequestBody List<String> patterns) {
        String owner = principal.getAttribute("login");
        return githubService.updateIgnorePatterns(owner, patterns);
    }

    @PatchMapping("/api/github/openai")
    public Long updateOpenAiKey(@AuthenticationPrincipal OAuth2User principal, @RequestBody OpenAiKeyDto dto) {
        String owner = principal.getAttribute("login");
        String key = dto.getKey();
        return githubService.updateOpenAiKey(owner, key);
    }

    @GetMapping("/api/github/openai")
    public String getOpenAiKey(@AuthenticationPrincipal OAuth2User principal) {
        String owner = principal.getAttribute("login");
        return githubService.getOpenAiKey(owner);
    }
}
