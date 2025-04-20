package com.seojs.code_review_platform.github.service;

import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.GitRepositoryWithWebhookResponseDto;
import com.seojs.code_review_platform.github.dto.WebhookCreateRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class GithubService {

    private final RestTemplate restTemplate;

    public List<GitRepositoryResponseDto> getRepositories(String accessToken) {
        String url = "https://api.github.com/user/repos";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<GitRepositoryResponseDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {
                }
        );

        return response.getBody();
    }

    public boolean isWebhook(String accessToken, String owner, String repo, String webhookUrl) {
        String url = String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
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
                    .anyMatch(hook -> hook.getConfig().get("url").equals(webhookUrl));
        }

        return false;
    }

    public List<GitRepositoryWithWebhookResponseDto> getRepositoriesWithWebhookStatus(String accessToken) {
        String webhookUrl = "http://13.236.152.27:8080/api/github/webhook/";

        List<GitRepositoryResponseDto> repositories = getRepositories(accessToken);

        return repositories.stream()
                .map(repo -> {
                    boolean hasWebhook = isWebhook(accessToken, repo.getOwner(), repo.getName(), webhookUrl);
                    return GitRepositoryWithWebhookResponseDto.builder().repository(repo).hasWebhook(hasWebhook).build();
                })
                .toList();
    }

    public WebhookResponseDto registerWebhook(String accessToken, String owner, String repo) {
        String webhookUrl = "http://13.236.152.27:8080/api/github/webhook/";

        String url = String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo);

        //웹훅 설정
        Map<String, String> config = new HashMap<>();
        config.put("url", webhookUrl);
        config.put("content_type", "json");
        config.put("insecure_ssl", "0");

        //웹훅 이벤트
        List<String> events = Arrays.asList("push", "pull_request");

        WebhookCreateRequestDto dto = WebhookCreateRequestDto.builder()
                .name("web")
                .config(config)
                .events(events)
                .active(true)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<WebhookCreateRequestDto> entity = new HttpEntity<>(dto, headers);

        HttpEntity<WebhookResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                WebhookResponseDto.class
        );

        return response.getBody();
    }
}
