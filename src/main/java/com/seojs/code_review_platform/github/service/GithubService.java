package com.seojs.code_review_platform.github.service;

import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.GitRepositoryWithWebhookResponseDto;
import com.seojs.code_review_platform.github.dto.WebhookCreateRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class GithubService {

    private final RestTemplate restTemplate;
    private final GithubAccountRepository githubAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;

    @Value("${github.webhook.url}")
    private String webhookUrl;

    /**
     * 사용자의 GitHub 저장소 목록을 조회
     */
    public List<GitRepositoryResponseDto> getRepositories(String accessToken) {
        String url = "https://api.github.com/user/repos";

        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<GitRepositoryResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        return response.getBody();
    }

    /**
     * 특정 저장소에 지정된 webhook이 등록되어 있는지 확인
     */
    public boolean isWebhook(String accessToken, String owner, String repo) {
        String url = String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo);

        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<WebhookResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        List<WebhookResponseDto> webhooks = response.getBody();

        if (webhooks != null) {
            return webhooks.stream()
                    .anyMatch(hook -> {
                        Object configUrl = hook.getConfig().get("url");
                        return configUrl != null && configUrl.toString().equals(webhookUrl);
                    });
        }

        return false;
    }

    /**
     * 사용자의 모든 저장소와 각 저장소의 webhook 등록 상태를 조회
     */
    public List<GitRepositoryWithWebhookResponseDto> getRepositoriesWithWebhookStatus(String accessToken) {
        List<GitRepositoryResponseDto> repositories = getRepositories(accessToken);

        return repositories.stream()
                .map(repo -> {
                    boolean hasWebhook = isWebhook(accessToken, repo.getOwner(), repo.getName());
                    return GitRepositoryWithWebhookResponseDto.builder()
                            .repository(repo)
                            .hasWebhook(hasWebhook)
                            .build();
                })
                .toList();
    }

    /**
     * 저장소에 webhook 등록
     */
    public WebhookResponseDto registerWebhook(String accessToken, String owner, String repo) {
        String url = String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo);

        WebhookCreateRequestDto dto = createWebhookRequest();
        HttpHeaders headers = createAuthHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<WebhookCreateRequestDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<WebhookResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                WebhookResponseDto.class
        );

        return response.getBody();
    }

    /**
     * owner(login)으로 accessToken 조회
     */
    public String findAccessTokenByOwner(String owner) {
        return githubAccountRepository.findByLoginId(owner)
                .map(account -> tokenEncryptionService.decryptToken(account.getAccessToken()))
                .orElseThrow(() -> new RuntimeException("No accessToken for owner: " + owner));
    }
    
    /**
     * PR의 변경된 파일 목록 조회
     */
    public List<ChangedFileDto> getChangedFiles(String accessToken, String owner, String repo, int prNumber) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/files", owner, repo, prNumber);
            
            HttpHeaders headers = createAuthHeaders(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<ChangedFileDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            List<ChangedFileDto> changedFiles = response.getBody();
            return changedFiles != null ? changedFiles : Collections.emptyList();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get changed files", e);
        }
    }

    /**
     * GitHub API 인증 헤더 생성
     */
    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }

    /**
     * Webhook 생성 요청 DTO 생성
     */
    private WebhookCreateRequestDto createWebhookRequest() {
        Map<String, String> config = new HashMap<>();
        config.put("url", webhookUrl);
        config.put("content_type", "json");
        config.put("insecure_ssl", "0");

        List<String> events = Arrays.asList("push", "pull_request");

        return WebhookCreateRequestDto.builder()
                .name("web")
                .config(config)
                .events(events)
                .active(true)
                .build();
    }
}
