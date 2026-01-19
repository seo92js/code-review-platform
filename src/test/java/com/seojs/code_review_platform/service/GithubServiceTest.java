package com.seojs.code_review_platform.service;

import com.seojs.code_review_platform.ai.service.AiService;
import com.seojs.code_review_platform.exception.GithubAccountNotFoundEx;
import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.ReviewSettingsDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
import com.seojs.code_review_platform.github.entity.DetailLevel;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.entity.ReviewFocus;
import com.seojs.code_review_platform.github.entity.ReviewTone;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import com.seojs.code_review_platform.github.service.GithubService;
import com.seojs.code_review_platform.github.service.TokenEncryptionService;
import com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class GithubServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GithubAccountRepository githubAccountRepository;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private AiService aiService;

    private Executor githubApiExecutor = Runnable::run;

    private GithubService githubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        githubService = new GithubService(restTemplate, githubAccountRepository, tokenEncryptionService,
                pullRequestRepository, aiService, githubApiExecutor);
        ReflectionTestUtils.setField(githubService, "webhookUrl", "http://test.com/webhook");
    }

    @Test
    void getRepositories_성공() {
        // given
        String accessToken = "test-token";
        List<GitRepositoryResponseDto> repos = Arrays.asList(
                new GitRepositoryResponseDto(),
                new GitRepositoryResponseDto());

        ResponseEntity<List<GitRepositoryResponseDto>> response = new ResponseEntity<>(repos, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://api.github.com/user/repos"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // when
        List<GitRepositoryResponseDto> result = githubService.getRepositories(accessToken);

        // then
        assertEquals(repos, result);
    }

    @Test
    void isWebhook_웹훅존재시_True반환() {
        // given
        String accessToken = "test-token";
        String owner = "test-owner";
        String repo = "test-repo";
        String webhookUrl = "http://test.com/webhook";

        Map<String, String> config = new HashMap<>();
        config.put("url", webhookUrl);

        WebhookResponseDto dto = new WebhookResponseDto();
        dto.setConfig(config);

        List<WebhookResponseDto> webhooks = Collections.singletonList(dto);
        ResponseEntity<List<WebhookResponseDto>> response = new ResponseEntity<>(webhooks, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // when
        boolean result = githubService.isWebhook(accessToken, owner, repo);

        // then
        assertTrue(result);
    }

    @Test
    void isWebhook_웹훅없을시_False반환() {
        // given
        String accessToken = "test-token";
        String owner = "test-owner";
        String repo = "test-repo";
        String differentUrl = "http://different.com/webhook";

        Map<String, String> config = new HashMap<>();
        config.put("url", differentUrl);

        WebhookResponseDto webhookDto = new WebhookResponseDto();
        webhookDto.setConfig(config);

        List<WebhookResponseDto> webhooks = Collections.singletonList(webhookDto);
        ResponseEntity<List<WebhookResponseDto>> response = new ResponseEntity<>(webhooks, HttpStatus.OK);

        when(restTemplate.exchange(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(response);

        // when
        boolean result = githubService.isWebhook(accessToken, owner, repo);

        // then
        assertFalse(result);
    }

    @Test
    void getRepositoriesWithWebhookStatus_성공() {
        // given
        String accessToken = "test-token";

        GitRepositoryResponseDto repo1 = new GitRepositoryResponseDto();
        GitRepositoryResponseDto repo2 = new GitRepositoryResponseDto();

        List<GitRepositoryResponseDto> repos = Arrays.asList(repo1, repo2);

        when(restTemplate.exchange(
                eq("https://api.github.com/user/repos"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(repos, HttpStatus.OK));

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(
                new ResponseEntity<>(repos, HttpStatus.OK),
                new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK),
                new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        // when
        var result = githubService.getRepositoriesWithWebhookStatus(accessToken);

        // then
        assertEquals(2, result.size());
        assertFalse(result.get(0).isHasWebhook());
        assertFalse(result.get(1).isHasWebhook());
    }

    @Test
    void registerWebhook_성공() {
        // given
        String accessToken = "test-token";
        String owner = "test-owner";
        String repo = "test-repo";
        String webhookSecret = "test-webhook-secret";

        GithubAccount mockAccount = GithubAccount.builder()
                .loginId(owner)
                .accessToken("encrypted-token")
                .webhookSecret(webhookSecret)
                .build();

        when(githubAccountRepository.findByLoginId(owner)).thenReturn(Optional.of(mockAccount));

        WebhookResponseDto expectedResponse = new WebhookResponseDto();
        expectedResponse.setId(123L);
        ResponseEntity<String> response = new ResponseEntity<>("{\"id\":123}", HttpStatus.CREATED);

        when(restTemplate.postForEntity(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(response);

        // when
        githubService.registerWebhook(accessToken, owner, repo);

        // then
        verify(restTemplate).postForEntity(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                any(HttpEntity.class),
                eq(String.class));

        verify(githubAccountRepository).findByLoginId(owner);
    }

    @Test
    void findAccessTokenByLoginId_성공() {
        // given
        String loginId = "test-owner";
        String encryptedToken = "encrypted-token";
        String decryptedToken = "test-access-token";

        GithubAccount account = GithubAccount.builder()
                .loginId(loginId)
                .accessToken(encryptedToken)
                .build();

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(account));
        when(tokenEncryptionService.decryptToken(encryptedToken))
                .thenReturn(decryptedToken);

        // when
        String result = githubService.findAccessTokenByLoginId(loginId);

        // then
        assertEquals(decryptedToken, result);
    }

    @Test
    void findAccessTokenByLoginId_계정없을시_예외발생() {
        // given
        String loginId = "non-existent-owner";

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        // when & then
        GithubAccountNotFoundEx exception = assertThrows(GithubAccountNotFoundEx.class,
                () -> githubService.findAccessTokenByLoginId(loginId));

        assertEquals("No accessToken for loginId: " + loginId, exception.getMessage());
    }

    @Test
    void getReviewSettings_성공() {
        // given
        String loginId = "test-user";

        GithubAccount account = GithubAccount.builder()
                .loginId(loginId)
                .accessToken("test-token")
                .webhookSecret("test-secret")
                .build();

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(account));

        // when
        var result = githubService.getReviewSettings(loginId);

        // then
        assertEquals(ReviewTone.NEUTRAL, result.getTone());
        assertEquals(ReviewFocus.BOTH, result.getFocus());
        assertEquals(DetailLevel.STANDARD, result.getDetailLevel());
        assertNull(result.getCustomInstructions());
        verify(githubAccountRepository).findByLoginId(loginId);
    }

    @Test
    void getReviewSettings_계정없을시_예외발생() {
        // given
        String loginId = "non-existent-user";

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        // when & then
        GithubAccountNotFoundEx exception = assertThrows(GithubAccountNotFoundEx.class,
                () -> githubService.getReviewSettings(loginId));

        assertEquals("GithubAccount not found for loginId: " + loginId, exception.getMessage());
    }

    @Test
    void updateReviewSettings_성공() {
        // given
        String loginId = "test-user";
        Long expectedId = 1L;

        GithubAccount account = GithubAccount.builder()
                .loginId(loginId)
                .accessToken("test-token")
                .webhookSecret("test-secret")
                .build();
        ReflectionTestUtils.setField(account, "id", expectedId);

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(account));

        ReviewSettingsDto dto = new ReviewSettingsDto(
                ReviewTone.FRIENDLY,
                ReviewFocus.PRAISE_ONLY,
                DetailLevel.DETAILED,
                "보안에 집중해주세요");

        // when
        Long result = githubService.updateReviewSettings(loginId, dto);

        // then
        assertEquals(expectedId, result);
        assertEquals(ReviewTone.FRIENDLY, account.getAiSettings().getReviewTone());
        assertEquals(ReviewFocus.PRAISE_ONLY, account.getAiSettings().getReviewFocus());
        assertEquals(DetailLevel.DETAILED, account.getAiSettings().getDetailLevel());
        assertEquals("보안에 집중해주세요", account.getAiSettings().getCustomInstructions());
        verify(githubAccountRepository).findByLoginId(loginId);
    }

    @Test
    void updateReviewSettings_계정없을시_예외발생() {
        // given
        String loginId = "non-existent-user";
        ReviewSettingsDto dto = new ReviewSettingsDto(
                ReviewTone.STRICT, ReviewFocus.IMPROVEMENT_ONLY, DetailLevel.CONCISE, null);

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        // when & then
        GithubAccountNotFoundEx exception = assertThrows(GithubAccountNotFoundEx.class,
                () -> githubService.updateReviewSettings(loginId, dto));

        assertEquals("GithubAccount not found for loginId: " + loginId, exception.getMessage());
    }

    @Test
    void getRepositoriesWithWebhookStatus_정렬검증() {
        // given
        String accessToken = "test-token";

        GitRepositoryResponseDto repo1 = new GitRepositoryResponseDto();
        ReflectionTestUtils.setField(repo1, "name", "repo1");
        ReflectionTestUtils.setField(repo1, "owner", "owner");
        ReflectionTestUtils.setField(repo1, "updatedAt", "2024-01-01T00:00:00Z");

        GitRepositoryResponseDto repo2 = new GitRepositoryResponseDto();
        ReflectionTestUtils.setField(repo2, "name", "repo2");
        ReflectionTestUtils.setField(repo2, "owner", "owner");
        ReflectionTestUtils.setField(repo2, "updatedAt", "2024-01-02T00:00:00Z");

        GitRepositoryResponseDto repo3 = new GitRepositoryResponseDto();
        ReflectionTestUtils.setField(repo3, "name", "repo3");
        ReflectionTestUtils.setField(repo3, "owner", "owner");
        ReflectionTestUtils.setField(repo3, "updatedAt", "2024-01-03T00:00:00Z");

        List<GitRepositoryResponseDto> repos = Arrays.asList(repo1, repo2, repo3);

        when(restTemplate.exchange(
                eq("https://api.github.com/user/repos"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(repos, HttpStatus.OK));

        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.contains("hooks"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK));

        when(pullRequestRepository.existsOpenPrByLoginIdAndRepositoryName("owner", "repo1")).thenReturn(false);
        when(pullRequestRepository.existsOpenPrByLoginIdAndRepositoryName("owner", "repo2")).thenReturn(true);
        when(pullRequestRepository.existsOpenPrByLoginIdAndRepositoryName("owner", "repo3")).thenReturn(true);

        // when
        var result = githubService.getRepositoriesWithWebhookStatus(accessToken);

        // then
        assertEquals(3, result.size());
        assertEquals("repo3", result.get(0).getRepository().getName());
        assertEquals("repo2", result.get(1).getRepository().getName());
        assertEquals("repo1", result.get(2).getRepository().getName());
    }

    @Test
    void validateOpenAiKey_성공() {
        // given
        String validKey = "sk-valid-key";
        when(aiService.validateApiKey(validKey)).thenReturn(true);

        // when
        boolean result = githubService.validateOpenAiKey(validKey);

        // then
        assertTrue(result);
        verify(aiService).validateApiKey(validKey);
    }

    @Test
    void validateOpenAiKey_실패() {
        // given
        String invalidKey = "invalid-key";
        when(aiService.validateApiKey(invalidKey)).thenReturn(false);

        // when
        boolean result = githubService.validateOpenAiKey(invalidKey);

        // then
        assertFalse(result);
        verify(aiService).validateApiKey(invalidKey);
    }
}
