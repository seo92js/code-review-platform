package com.seojs.code_review_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.PullRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.RepositoryDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.UserDto;
import com.seojs.code_review_platform.pullrequest.dto.PullRequestResponseDto;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest;
import com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository;
import com.seojs.code_review_platform.pullrequest.service.PullRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PullRequestServiceTest {

    @Mock
    private PullRequestRepository pullRequestRepository;

    @Mock
    private ObjectMapper objectMapper;

    private PullRequestService pullRequestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pullRequestService = new PullRequestService(pullRequestRepository, objectMapper);
    }

    @Test
    void getPullRequestList_성공() {
        // given
        String ownerLogin = "test-owner";
        String repositoryName = "test-repo";
        
        // 테스트용 PR 엔티티들 생성
        PullRequest pr1 = PullRequest.builder()
                .prNumber(1)
                .repositoryName(repositoryName)
                .ownerLogin(ownerLogin)
                .title("첫 번째 PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        PullRequest pr2 = PullRequest.builder()
                .prNumber(2)
                .repositoryName(repositoryName)
                .ownerLogin(ownerLogin)
                .title("두 번째 PR")
                .action("synchronize")
                .status(PullRequest.ReviewStatus.COMPLETED)
                .build();

        List<PullRequest> pullRequests = Arrays.asList(pr1, pr2);

        // Repository mock 설정
        when(pullRequestRepository.findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName))
                .thenReturn(pullRequests);

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(ownerLogin, repositoryName);

        // then
        assertEquals(2, result.size());
        
        // 첫 번째 PR 검증
        PullRequestResponseDto firstPr = result.get(0);
        assertEquals(1, firstPr.getPrNumber());
        assertEquals("첫 번째 PR", firstPr.getTitle());
        assertEquals("opened", firstPr.getAction());
        assertEquals(PullRequest.ReviewStatus.PENDING, firstPr.getStatus());
        
        // 두 번째 PR 검증
        PullRequestResponseDto secondPr = result.get(1);
        assertEquals(2, secondPr.getPrNumber());
        assertEquals("두 번째 PR", secondPr.getTitle());
        assertEquals("synchronize", secondPr.getAction());
        assertEquals(PullRequest.ReviewStatus.COMPLETED, secondPr.getStatus());
        
        // Repository 메서드 호출 검증
        verify(pullRequestRepository).findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName);
    }

    @Test
    void getPullRequestList_빈리스트반환() {
        // given
        String ownerLogin = "test-owner";
        String repositoryName = "test-repo";
        
        // Repository에서 빈 리스트 반환
        when(pullRequestRepository.findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName))
                .thenReturn(Arrays.asList());

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(ownerLogin, repositoryName);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        
        // Repository 메서드 호출 검증
        verify(pullRequestRepository).findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName);
    }

    @Test
    void getPullRequestList_다른저장소_빈리스트반환() {
        // given
        String ownerLogin = "test-owner";
        String repositoryName = "test-repo";
        
        // Repository에서 빈 리스트 반환 (다른 저장소는 조회되지 않음)
        when(pullRequestRepository.findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName))
                .thenReturn(Arrays.asList());

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(ownerLogin, repositoryName);

        // then
        assertTrue(result.isEmpty());
        verify(pullRequestRepository).findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName);
    }

    @Test
    void getPullRequestList_다른소유자_빈리스트반환() {
        // given
        String ownerLogin = "test-owner";
        String repositoryName = "test-repo";
        
        // Repository에서 빈 리스트 반환 (다른 소유자는 조회되지 않음)
        when(pullRequestRepository.findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName))
                .thenReturn(Arrays.asList());

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(ownerLogin, repositoryName);

        // then
        assertTrue(result.isEmpty());
        verify(pullRequestRepository).findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName);
    }

    @Test
    void getPullRequestList_정렬순서확인() {
        // given
        String ownerLogin = "test-owner";
        String repositoryName = "test-repo";
        
        // 업데이트 시간이 다른 PR들 생성
        PullRequest oldPr = PullRequest.builder()
                .prNumber(1)
                .repositoryName(repositoryName)
                .ownerLogin(ownerLogin)
                .title("오래된 PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        PullRequest newPr = PullRequest.builder()
                .prNumber(2)
                .repositoryName(repositoryName)
                .ownerLogin(ownerLogin)
                .title("새로운 PR")
                .action("synchronize")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        PullRequest middlePr = PullRequest.builder()
                .prNumber(3)
                .repositoryName(repositoryName)
                .ownerLogin(ownerLogin)
                .title("중간 PR")
                .action("reopened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        // Repository에서 정렬된 순서로 반환 (updatedAt 내림차순)
        List<PullRequest> pullRequests = Arrays.asList(newPr, middlePr, oldPr);

        when(pullRequestRepository.findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName))
                .thenReturn(pullRequests);

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(ownerLogin, repositoryName);

        // then
        assertEquals(3, result.size());
        
        // 정렬 순서 확인 (updatedAt 내림차순)
        assertEquals(2, result.get(0).getPrNumber());
        assertEquals(3, result.get(1).getPrNumber());
        assertEquals(1, result.get(2).getPrNumber());
        
        verify(pullRequestRepository).findByOwnerLoginAndRepositoryNameOrderByUpdatedAtDesc(ownerLogin, repositoryName);
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
        pullRequestService.processAndSaveWebhook(payload);

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
        pullRequestService.processAndSaveWebhook(payload);

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
        pullRequestService.processAndSaveWebhook(payload);

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
                () -> pullRequestService.processAndSaveWebhook(invalidPayload));

        assertEquals("Webhook processing failed", exception.getMessage());
    }

    private WebhookPayloadDto createWebhookPayload(String action, int prNumber, String title, String repoName, String ownerLogin) {
        UserDto owner = new UserDto(ownerLogin, 1, "http://test.com/user");
        RepositoryDto repository = new RepositoryDto(repoName, repoName + "/" + ownerLogin, owner);
        PullRequestDto pullRequest = new PullRequestDto(prNumber, title, "body", "open", owner, "http://test.com/pr", "http://test.com/diff");

        return new WebhookPayloadDto(action, pullRequest, repository);
    }
}
