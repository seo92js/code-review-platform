package com.seojs.code_review_platform.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.GitRepositoryWithWebhookResponseDto;
import com.seojs.code_review_platform.github.dto.WebhookCreateRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
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
    private final ObjectMapper objectMapper;
    
    @Value("${github.webhook.url}")
    private String webhookUrl;

    /**
     * 사용자의 GitHub 저장소 목록을 조회
     */
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

    /**
     * 특정 저장소에 지정된 webhook이 등록되어 있는지 확인
     */
    public boolean isWebhook(String accessToken, String owner, String repo) {
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
                    return GitRepositoryWithWebhookResponseDto.builder().repository(repo).hasWebhook(hasWebhook).build();
                })
                .toList();
    }

    /**
     * 저장소에 webhook 등록
     */
    public WebhookResponseDto registerWebhook(String accessToken, String owner, String repo) {
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

    /**
     * Pull Request webhook 이벤트 처리
     */
    public void processPullRequestEvent(String payload, String accessToken) {
        try {
            WebhookPayloadDto webhookPayload = objectMapper.readValue(payload, WebhookPayloadDto.class);
            
            String action = webhookPayload.getAction();
            int prNumber = webhookPayload.getPullRequest().getNumber();
            String repoName = webhookPayload.getRepository().getName();
            String owner = webhookPayload.getRepository().getOwner().getLogin();
            
            // PR이 열렸거나 수정된 경우에만 리뷰 수행
            if ("opened".equals(action) || "synchronize".equals(action)) {
                
                // 1. 변경된 파일 목록 가져오기
                List<ChangedFileDto> changedFiles = getChangedFiles(accessToken, owner, repoName, prNumber);
                
                if (changedFiles != null && !changedFiles.isEmpty()) {
                    // TODO: 각 파일의 변경 내용 분석
                    // TODO: AI 서비스 호출하여 코드 리뷰
                    // TODO: 리뷰 결과를 PR에 코멘트로 추가
                }
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Webhook processing failed", e);
        }
    }
    
    /**
     * Push webhook 이벤트를 처리
     */
    public void processPushEvent(String payload) {

    }

    /**
     * 변경된 파일 목록 가져오기
     */
    private List<ChangedFileDto> getChangedFiles(String accessToken, String owner, String repo, int prNumber) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/pulls/%d/files", owner, repo, prNumber);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<ChangedFileDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
            );
            
            List<ChangedFileDto> changedFiles = response.getBody();
            if (changedFiles == null) {
                return Collections.emptyList();
            }
            
            return changedFiles;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to get changed files", e);
        }
    }
}
