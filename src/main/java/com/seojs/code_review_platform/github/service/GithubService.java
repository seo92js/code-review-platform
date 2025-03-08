package com.seojs.code_review_platform.github.service;

import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.GitRepositoryWithWebhookResponseDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
        String webhookUrl = "temp";

        List<GitRepositoryResponseDto> repositories = getRepositories(accessToken);

        return repositories.stream()
                .map(repo -> {
                    boolean hasWebhook = isWebhook(accessToken, repo.getOwner(), repo.getName(), webhookUrl);
                    return GitRepositoryWithWebhookResponseDto.builder().repository(repo).hasWebhook(hasWebhook).build();
                })
                .toList();
    }
}
