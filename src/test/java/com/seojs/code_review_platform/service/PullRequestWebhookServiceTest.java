package com.seojs.code_review_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.PullRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.RepositoryDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.UserDto;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest;
import com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository;
import com.seojs.code_review_platform.pullrequest.service.PullRequestWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PullRequestWebhookServiceTest {

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private ObjectMapper objectMapper;

    private PullRequestWebhookService pullRequestWebhookService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pullRequestWebhookService = new PullRequestWebhookService(pullRequestRepository, objectMapper);
    }

    @Test
    void processAndSaveWebhook_PR액션_새PR생성() throws Exception {
        // given
        String payload = "{\"action\":\"opened\",\"pull_request\":{\"number\":1,\"title\":\"Test PR\"},\"repository\":{\"name\":\"test-repo\",\"owner\":{\"login\":\"test-owner\"}}}";
        
        WebhookPayloadDto webhookPayload = createWebhookPayload("opened", 1, "Test PR", "test-repo", "test-owner");
        
        when(objectMapper.readValue(payload, WebhookPayloadDto.class))
                .thenReturn(webhookPayload);
        
        when(pullRequestRepository.findByRepositoryNameAndOwnerLoginAndPrNumber("test-repo", "test-owner", 1))
                .thenReturn(Optional.empty());

        // when
        pullRequestWebhookService.processAndSaveWebhook(payload);

        // then
        ArgumentCaptor<PullRequest> prCaptor = ArgumentCaptor.forClass(PullRequest.class);
        verify(pullRequestRepository).save(prCaptor.capture());
        
        PullRequest savedPr = prCaptor.getValue();
        assertEquals(1, savedPr.getPrNumber());
        assertEquals("test-repo", savedPr.getRepositoryName());
        assertEquals("test-owner", savedPr.getOwnerLogin());
        assertEquals("Test PR", savedPr.getTitle());
        assertEquals("opened", savedPr.getAction());
        assertEquals(PullRequest.ReviewStatus.PENDING, savedPr.getStatus());
    }

    @Test
    void processAndSaveWebhook_PR액션_기존PR업데이트() throws Exception {
        // given
        String payload = "{\"action\":\"synchronize\",\"pull_request\":{\"number\":1,\"title\":\"Updated PR\"},\"repository\":{\"name\":\"test-repo\",\"owner\":{\"login\":\"test-owner\"}}}";
        
        WebhookPayloadDto webhookPayload = createWebhookPayload("synchronize", 1, "Updated PR", "test-repo", "test-owner");
        
        PullRequest existingPr = PullRequest.builder()
                .prNumber(1)
                .repositoryName("test-repo")
                .ownerLogin("test-owner")
                .title("Old Title")
                .action("opened")
                .status(PullRequest.ReviewStatus.COMPLETED)
                .build();
        
        when(objectMapper.readValue(payload, WebhookPayloadDto.class))
                .thenReturn(webhookPayload);
        
        when(pullRequestRepository.findByRepositoryNameAndOwnerLoginAndPrNumber("test-repo", "test-owner", 1))
                .thenReturn(Optional.of(existingPr));

        // when
        pullRequestWebhookService.processAndSaveWebhook(payload);

        // then
        verify(pullRequestRepository).save(existingPr);
        assertEquals(PullRequest.ReviewStatus.PENDING, existingPr.getStatus());
        assertEquals("synchronize", existingPr.getAction());
    }

    @Test
    void processAndSaveWebhook_지원하지않는액션_저장안함() throws Exception {
        // given
        String payload = "{\"action\":\"assigned\",\"pull_request\":{\"number\":1,\"title\":\"Test PR\"},\"repository\":{\"name\":\"test-repo\",\"owner\":{\"login\":\"test-owner\"}}}";
        
        WebhookPayloadDto webhookPayload = createWebhookPayload("assigned", 1, "Test PR", "test-repo", "test-owner");
        
        when(objectMapper.readValue(payload, WebhookPayloadDto.class))
                .thenReturn(webhookPayload);

        // when
        pullRequestWebhookService.processAndSaveWebhook(payload);

        // then
        verify(pullRequestRepository, never()).save(any());
    }

    @Test
    void processAndSaveWebhook_JSON파싱실패시_예외발생() throws Exception {
        // given
        String invalidPayload = "invalid json";
        
        when(objectMapper.readValue(invalidPayload, WebhookPayloadDto.class))
                .thenThrow(new RuntimeException("JSON parsing failed"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> pullRequestWebhookService.processAndSaveWebhook(invalidPayload));
        
        assertEquals("Webhook processing failed", exception.getMessage());
    }

    private WebhookPayloadDto createWebhookPayload(String action, int prNumber, String title, String repoName, String ownerLogin) {
        UserDto owner = new UserDto(ownerLogin, 1, "http://test.com/user");
        RepositoryDto repository = new RepositoryDto(repoName, repoName + "/" + ownerLogin, owner);
        PullRequestDto pullRequest = new PullRequestDto(prNumber, title, "body", "open", owner, "http://test.com/pr", "http://test.com/diff");
        
        return new WebhookPayloadDto(action, pullRequest, repository);
    }
}
