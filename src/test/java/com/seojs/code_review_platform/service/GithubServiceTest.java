package com.seojs.code_review_platform.service;

import com.seojs.code_review_platform.exception.GithubAccountNotFoundEx;
import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import com.seojs.code_review_platform.github.service.GithubService;
import com.seojs.code_review_platform.github.service.TokenEncryptionService;
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
    private com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository pullRequestRepository;

    private GithubService githubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        githubService = new GithubService(restTemplate, githubAccountRepository, tokenEncryptionService, pullRequestRepository);
        // 테스트용 webhook URL 설정
        ReflectionTestUtils.setField(githubService, "webhookUrl", "http://test.com/webhook");
    }

    @Test
    void getRepositories_성공() {
        // given
        String accessToken = "test-token";
        List<GitRepositoryResponseDto> repos = Arrays.asList(
                new GitRepositoryResponseDto(),
                new GitRepositoryResponseDto()
        );

        ResponseEntity<List<GitRepositoryResponseDto>> response = new ResponseEntity<>(repos, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://api.github.com/user/repos"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

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
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

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
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

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
                any(ParameterizedTypeReference.class)
        )).thenReturn(new ResponseEntity<>(repos, HttpStatus.OK));

        when(restTemplate.exchange(
                any(String.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(
                new ResponseEntity<>(repos, HttpStatus.OK),
                new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK),
                new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK)
        );

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
                eq(String.class)
        )).thenReturn(response);

        // when
        githubService.registerWebhook(accessToken, owner, repo);

        // then
        // registerWebhook는 void 메서드이므로 검증은 mock 호출로 확인
        verify(restTemplate).postForEntity(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                any(HttpEntity.class),
                eq(String.class)
        );

        // 웹훅 등록이 성공적으로 호출되었는지 확인
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
    void getSystemPrompt_성공() {
        // given
        String loginId = "test-user";
        String expectedPrompt = "당신은 시니어 개발자입니다.";

        GithubAccount account = GithubAccount.builder()
                .loginId(loginId)
                .accessToken("test-token")
                .webhookSecret("test-secret")
                .build();
        account.updateSystemPrompt(expectedPrompt);

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(account));

        // when
        String result = githubService.getSystemPrompt(loginId);

        // then
        assertEquals(expectedPrompt, result);
        verify(githubAccountRepository).findByLoginId(loginId);
    }

    @Test
    void getSystemPrompt_계정없을시_예외발생() {
        // given
        String loginId = "non-existent-user";

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        // when & then
        GithubAccountNotFoundEx exception = assertThrows(GithubAccountNotFoundEx.class,
                () -> githubService.getSystemPrompt(loginId));

        assertEquals("GithubAccount not found for loginId: " + loginId, exception.getMessage());
    }

    @Test
    void updateSystemPrompt_성공() {
        // given
        String loginId = "test-user";
        String newPrompt = "당신은 주니어 개발자를 위한 친절한 리뷰어입니다.";
        Long expectedId = 1L;

        GithubAccount account = GithubAccount.builder()
                .loginId(loginId)
                .accessToken("test-token")
                .webhookSecret("test-secret")
                .build();
        ReflectionTestUtils.setField(account, "id", expectedId);

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.of(account));

        // when
        Long result = githubService.updateSystemPrompt(loginId, newPrompt);

        // then
        assertEquals(expectedId, result);
        assertEquals(newPrompt, account.getSystemPrompt());
        verify(githubAccountRepository).findByLoginId(loginId);
    }

    @Test
    void updateSystemPrompt_계정없을시_예외발생() {
        // given
        String loginId = "non-existent-user";
        String newPrompt = "새로운 프롬프트";

        when(githubAccountRepository.findByLoginId(loginId))
                .thenReturn(Optional.empty());

        // when & then
        GithubAccountNotFoundEx exception = assertThrows(GithubAccountNotFoundEx.class,
                () -> githubService.updateSystemPrompt(loginId, newPrompt));

        assertEquals("GithubAccount not found for loginId: " + loginId, exception.getMessage());
    }
}

