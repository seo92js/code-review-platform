package com.seojs.code_review_platform.github.service;

import com.seojs.code_review_platform.exception.GitHubApiEx;
import com.seojs.code_review_platform.exception.GithubAccountNotFoundEx;
import com.seojs.code_review_platform.exception.WebhookRegistrationEx;
import com.seojs.code_review_platform.github.dto.*;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class GithubService {

    private final RestTemplate restTemplate;
    private final GithubAccountRepository githubAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final PullRequestRepository pullRequestRepository;
    @Qualifier("githubApiExecutor")
    private final Executor githubApiExecutor;

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
                new ParameterizedTypeReference<>() {
                });

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
                new ParameterizedTypeReference<>() {
                });

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
     * 사용자의 모든 저장소와 각 저장소의 webhook 등록 상태를 조회 (병렬 처리)
     */
    public List<GitRepositoryWithWebhookResponseDto> getRepositoriesWithWebhookStatus(String accessToken) {
        List<GitRepositoryResponseDto> repositories = getRepositories(accessToken);

        List<CompletableFuture<GitRepositoryWithWebhookResponseDto>> futures = repositories.stream()
                .map(repo -> CompletableFuture.supplyAsync(() -> {
                    boolean hasWebhook = isWebhook(accessToken, repo.getOwner(), repo.getName());
                    boolean existsOpenPr = pullRequestRepository.existsOpenPrByLoginIdAndRepositoryName(repo.getOwner(),
                            repo.getName());

                    return GitRepositoryWithWebhookResponseDto.builder()
                            .repository(repo)
                            .hasWebhook(hasWebhook)
                            .existsOpenPullRequest(existsOpenPr)
                            .build();
                }, githubApiExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .sorted((r1, r2) -> {
                    // Open PR이 있는 경우 최우선
                    if (r1.isExistsOpenPullRequest() != r2.isExistsOpenPullRequest()) {
                        return r1.isExistsOpenPullRequest() ? -1 : 1;
                    }
                    // 최근 수정일 순 (내림차순)
                    String t1 = r1.getRepository().getUpdatedAt();
                    String t2 = r2.getRepository().getUpdatedAt();

                    if (t1 == null && t2 == null)
                        return 0;
                    if (t1 == null)
                        return 1;
                    if (t2 == null)
                        return -1;

                    return t2.compareTo(t1);
                })
                .collect(Collectors.toList());
    }

    /**
     * 로그인 아이디로 accessToken 조회
     */
    @Transactional(readOnly = true)
    public String findAccessTokenByLoginId(String loginId) {
        return githubAccountRepository.findByLoginId(loginId)
                .map(account -> tokenEncryptionService.decryptToken(account.getAccessToken()))
                .orElseThrow(() -> new GithubAccountNotFoundEx("No accessToken for loginId: " + loginId));
    }

    /**
     * 로그인 아이디로 GithubAccount 조회 - 존재하지 않으면 예외 발생
     */
    @Transactional(readOnly = true)
    public GithubAccount findByLoginIdOrThrow(String loginId) {
        return githubAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new GithubAccountNotFoundEx("GithubAccount not found for loginId: " + loginId));
    }

    /**
     * 시스템 프롬프트 조회
     */
    @Transactional(readOnly = true)
    public String getSystemPrompt(String loginId) {
        GithubAccount account = findByLoginIdOrThrow(loginId);
        return account.getSystemPrompt();
    }

    /**
     * 무시 패턴 조회
     */
    @Transactional(readOnly = true)
    public List<String> getIgnorePatterns(String loginId) {
        GithubAccount account = findByLoginIdOrThrow(loginId);
        String patterns = account.getIgnorePatterns();
        if (patterns == null || patterns.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(patterns.split("\\s*,\\s*"));
    }

    /**
     * openai api key 조회
     */
    @Transactional(readOnly = true)
    public String getOpenAiKey(String loginId) {
        GithubAccount account = findByLoginIdOrThrow(loginId);
        String encryptedKey = account.getOpenAiKey();
        if (encryptedKey == null || encryptedKey.isBlank()) {
            return null;
        }
        return tokenEncryptionService.decryptToken(encryptedKey);
    }

    /**
     * 마스킹된 OpenAI Key 반환 (API 응답용)
     */
    @Transactional(readOnly = true)
    public String getMaskedOpenAiKey(String loginId) {
        String decryptedKey = getOpenAiKey(loginId);
        if (decryptedKey == null || decryptedKey.length() < 10) {
            return null;
        }
        return decryptedKey.substring(0, 10) + "...****";
    }

    /**
     * 시스템 프롬프트 업데이트
     */
    @Transactional
    public Long updateSystemPrompt(String loginId, String systemPrompt) {
        GithubAccount account = findByLoginIdOrThrow(loginId);
        account.updateSystemPrompt(systemPrompt);
        return account.getId();
    }

    /**
     * 무시 패턴 업데이트
     */
    @Transactional
    public Long updateIgnorePatterns(String loginId, List<String> patterns) {
        GithubAccount account = findByLoginIdOrThrow(loginId);
        String patternsString = patterns == null ? "" : String.join(",", patterns);
        account.updateIgnorePatterns(patternsString);
        return account.getId();
    }

    /**
     * openai api key 업데이트
     */
    @Transactional
    public Long updateOpenAiKey(String loginId, String openAiKey) {
        GithubAccount account = findByLoginIdOrThrow(loginId);
        if (openAiKey == null || openAiKey.isBlank()) {
            account.updateOpenAiKey(null);
        } else {
            String encryptedKey = tokenEncryptionService.encryptToken(openAiKey);
            account.updateOpenAiKey(encryptedKey);
        }
        return account.getId();
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
                    new ParameterizedTypeReference<>() {
                    });

            List<ChangedFileDto> changedFiles = response.getBody();
            return changedFiles != null ? changedFiles : Collections.emptyList();

        } catch (Exception e) {
            throw new GitHubApiEx("Failed to get changed files", e);
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
    private WebhookCreateRequestDto createWebhookRequest(String webhookSecret) {
        Map<String, String> config = new HashMap<>();
        config.put("url", webhookUrl);
        config.put("content_type", "json");
        config.put("insecure_ssl", "0");
        config.put("secret", webhookSecret);

        List<String> events = List.of("push", "pull_request");

        return WebhookCreateRequestDto.builder()
                .name("web")
                .config(config)
                .events(events)
                .active(true)
                .build();
    }

    /**
     * 웹훅 등록
     */
    public void registerWebhook(String accessToken, String owner, String repository) {
        GithubAccount account = findByLoginIdOrThrow(owner);

        String url = String.format("https://api.github.com/repos/%s/%s/hooks", owner, repository);

        WebhookCreateRequestDto request = createWebhookRequest(account.getWebhookSecret());
        HttpHeaders headers = createAuthHeaders(accessToken);
        HttpEntity<WebhookCreateRequestDto> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new WebhookRegistrationEx("Webhook registration failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new WebhookRegistrationEx("Error occurred during webhook registration", e);
        }
    }
}
