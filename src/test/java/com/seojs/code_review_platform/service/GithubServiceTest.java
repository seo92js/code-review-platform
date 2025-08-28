package com.seojs.code_review_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.dto.WebhookCreateRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookResponseDto;
import com.seojs.code_review_platform.github.service.GithubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    private ObjectMapper objectMapper;

    private GithubService githubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        githubService = new GithubService(restTemplate, objectMapper);
        // 테스트용 webhook URL 설정
        ReflectionTestUtils.setField(githubService, "webhookUrl", "http://test.com/webhook");
    }

    @Test
    void getRepositories() {
        //given
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

        //when
        List<GitRepositoryResponseDto> result = githubService.getRepositories(accessToken);

        //then
        assertEquals(repos, result);
    }

    @Test
    void isWebhook_returnsTrueWhenWebhookExists() {
        //given
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

        //when
        boolean result = githubService.isWebhook(accessToken, owner, repo);

        //then
        assertTrue(result);
    }

    @Test
    void isWebhook_returnsFalseWhenWebhookDoesNotExist() {
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
    void createWebhook() {
        // given
        String accessToken = "test-token";
        String owner = "test-owner";
        String repo = "test-repo";

        WebhookResponseDto expectedResponse = new WebhookResponseDto();
        expectedResponse.setId(123L);
        Map<String, String> config = new HashMap<>();
        ResponseEntity<WebhookResponseDto> response = new ResponseEntity<>(expectedResponse, HttpStatus.CREATED);

        when(restTemplate.exchange(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(WebhookResponseDto.class)
        )).thenReturn(response);

        // when
        WebhookResponseDto result = githubService.registerWebhook(accessToken, owner, repo);

        // then
        assertEquals(expectedResponse, result);

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq(String.format("https://api.github.com/repos/%s/%s/hooks", owner, repo)),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(WebhookResponseDto.class)
        );

        HttpEntity<WebhookCreateRequestDto> capturedEntity = entityCaptor.getValue();
        WebhookCreateRequestDto requestDto = capturedEntity.getBody();

        assertEquals("web", requestDto.getName());
        assertTrue(requestDto.isActive());
        assertEquals("json", requestDto.getConfig().get("content_type"));
        assertTrue(requestDto.getEvents().contains("push"));
        assertTrue(requestDto.getEvents().contains("pull_request"));
    }
}