package com.seojs.code_review_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.PullRequestDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.RepositoryDto;
import com.seojs.code_review_platform.github.dto.WebhookPayloadDto.UserDto;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.service.GithubService;
import com.seojs.code_review_platform.github.service.WebhookSecurityService;
import com.seojs.code_review_platform.pullrequest.dto.PullRequestResponseDto;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest;
import com.seojs.code_review_platform.pullrequest.repository.PullRequestRepository;
import com.seojs.code_review_platform.pullrequest.service.PullRequestService;
import com.seojs.code_review_platform.exception.PullRequestNotFoundEx;
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
    private GithubService githubService;

    @Mock
    private WebhookSecurityService webhookSecurityService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    private PullRequestService pullRequestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pullRequestService = new PullRequestService(pullRequestRepository, githubService, webhookSecurityService, objectMapper, eventPublisher);
    }

    @Test
    void getPullRequestList_성공() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        
        // 테스트용 GithubAccount 생성
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId(loginId)
                .accessToken("test-token")
                .build();

        // 테스트용 PR 엔티티들 생성
        PullRequest pr1 = PullRequest.builder()
                .prNumber(1)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("첫 번째 PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        PullRequest pr2 = PullRequest.builder()
                .prNumber(2)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("두 번째 PR")
                .action("synchronize")
                .status(PullRequest.ReviewStatus.COMPLETED)
                .build();

        List<PullRequest> pullRequests = Arrays.asList(pr1, pr2);

        // Repository mock 설정
        when(pullRequestRepository.findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName))
                .thenReturn(pullRequests);

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(loginId, repositoryName);

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
        verify(pullRequestRepository).findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName);
    }

    @Test
    void getPullRequestList_빈리스트반환() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        
        // Repository에서 빈 리스트 반환
        when(pullRequestRepository.findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName))
                .thenReturn(Arrays.asList());

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(loginId, repositoryName);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
        
        // Repository 메서드 호출 검증
        verify(pullRequestRepository).findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName);
    }

    @Test
    void getPullRequestList_다른저장소_빈리스트반환() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        
        // Repository에서 빈 리스트 반환 (다른 저장소는 조회되지 않음)
        when(pullRequestRepository.findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName))
                .thenReturn(Arrays.asList());

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(loginId, repositoryName);

        // then
        assertTrue(result.isEmpty());
        verify(pullRequestRepository).findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName);
    }

    @Test
    void getPullRequestList_다른소유자_빈리스트반환() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        
        // Repository에서 빈 리스트 반환 (다른 소유자는 조회되지 않음)
        when(pullRequestRepository.findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName))
                .thenReturn(Arrays.asList());

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(loginId, repositoryName);

        // then
        assertTrue(result.isEmpty());
        verify(pullRequestRepository).findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName);
    }

    @Test
    void getPullRequestList_정렬순서확인() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        
        // 업데이트 시간이 다른 PR들 생성
        // 테스트용 GithubAccount 생성
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId(loginId)
                .accessToken("test-token")
                .build();

        PullRequest oldPr = PullRequest.builder()
                .prNumber(1)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("오래된 PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        PullRequest newPr = PullRequest.builder()
                .prNumber(2)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("새로운 PR")
                .action("synchronize")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        PullRequest middlePr = PullRequest.builder()
                .prNumber(3)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("중간 PR")
                .action("reopened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        // Repository에서 정렬된 순서로 반환 (updatedAt 내림차순)
        List<PullRequest> pullRequests = Arrays.asList(newPr, middlePr, oldPr);

        when(pullRequestRepository.findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName))
                .thenReturn(pullRequests);

        // when
        List<PullRequestResponseDto> result = pullRequestService.getPullRequestList(loginId, repositoryName);

        // then
        assertEquals(3, result.size());
        
        // 정렬 순서 확인 (updatedAt 내림차순)
        assertEquals(2, result.get(0).getPrNumber());
        assertEquals(3, result.get(1).getPrNumber());
        assertEquals(1, result.get(2).getPrNumber());
        
        verify(pullRequestRepository).findByGithubAccountLoginIdAndRepositoryNameOrderByUpdatedAtDesc(loginId, repositoryName);
    }

    @Test
    void processAndSaveWebhook_PR액션_새PR생성() throws Exception {
        // given
        String payload = "{\"action\":\"opened\",\"pull_request\":{\"number\":1,\"title\":\"Test PR\"},\"repository\":{\"name\":\"test-repo\",\"owner\":{\"login\":\"test-owner\"}}}";

        WebhookPayloadDto webhookPayload = createWebhookPayload("opened", 1, "Test PR", "test-repo", "test-owner");

        when(objectMapper.readValue(payload, WebhookPayloadDto.class))
                .thenReturn(webhookPayload);

        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber("test-repo", "test-owner", 1))
                .thenReturn(Optional.empty());

        // GithubService mock 설정
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId("test-owner")
                .accessToken("test-token")
                .build();
        when(githubService.findByLoginIdOrThrow("test-owner"))
                .thenReturn(githubAccount);

        // when
        pullRequestService.processAndSaveWebhook(payload, "sha256=test-signature");

        // then
        ArgumentCaptor<PullRequest> prCaptor = ArgumentCaptor.forClass(PullRequest.class);
        verify(pullRequestRepository).save(prCaptor.capture());

        PullRequest savedPr = prCaptor.getValue();
        assertEquals(1, savedPr.getPrNumber());
        assertEquals("test-repo", savedPr.getRepositoryName());
        assertEquals("test-owner", savedPr.getGithubAccount().getLoginId());
        assertEquals("Test PR", savedPr.getTitle());
        assertEquals("opened", savedPr.getAction());
        assertEquals(PullRequest.ReviewStatus.PENDING, savedPr.getStatus());
    }

    @Test
    void processAndSaveWebhook_PR액션_기존PR업데이트() throws Exception {
        // given
        String payload = "{\"action\":\"synchronize\",\"pull_request\":{\"number\":1,\"title\":\"Updated PR\"},\"repository\":{\"name\":\"test-repo\",\"owner\":{\"login\":\"test-owner\"}}}";

        WebhookPayloadDto webhookPayload = createWebhookPayload("synchronize", 1, "Updated PR", "test-repo", "test-owner");

        // GithubAccount 생성
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId("test-owner")
                .accessToken("test-token")
                .build();

        PullRequest existingPr = PullRequest.builder()
                .prNumber(1)
                .repositoryName("test-repo")
                .githubAccount(githubAccount)
                .title("Old Title")
                .action("opened")
                .status(PullRequest.ReviewStatus.COMPLETED)
                .build();

        when(objectMapper.readValue(payload, WebhookPayloadDto.class))
                .thenReturn(webhookPayload);

        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber("test-repo", "test-owner", 1))
                .thenReturn(Optional.of(existingPr));

        // when
        pullRequestService.processAndSaveWebhook(payload, "sha256=test-signature");

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
        pullRequestService.processAndSaveWebhook(payload, "sha256=test-signature");

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
                () -> pullRequestService.processAndSaveWebhook(invalidPayload, "sha256=test-signature"));

        assertEquals("Webhook processing failed", exception.getMessage());
    }

    @Test
    void getPullRequestWithChanges_성공() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        Integer prNumber = 123;
        String accessToken = "test-access-token";

        // 테스트용 GithubAccount 생성
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId(loginId)
                .accessToken(accessToken)
                .build();

        // 테스트용 PR 엔티티 생성
        PullRequest pullRequest = PullRequest.builder()
                .prNumber(prNumber)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("Test PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        // 테스트용 변경된 파일 목록 생성
        ChangedFileDto changedFile1 = new ChangedFileDto(
                "src/main/java/Example.java",
                "modified",
                5,
                2,
                7,
                100,
                "abc123",
                "https://github.com/test-owner/test-repo/blob/main/src/main/java/Example.java",
                "https://raw.githubusercontent.com/test-owner/test-repo/main/src/main/java/Example.java",
                "https://api.github.com/repos/test-owner/test-repo/contents/src/main/java/Example.java",
                "@@ -1,3 +1,4 @@\n public class Example {\n+    // 새로운 주석\n     private String name;\n-    private int age;\n+    private String age;\n"
        );

        ChangedFileDto changedFile2 = new ChangedFileDto(
                "README.md",
                "added",
                10,
                0,
                10,
                10,
                "def456",
                "https://github.com/test-owner/test-repo/blob/main/README.md",
                "https://raw.githubusercontent.com/test-owner/test-repo/main/README.md",
                "https://api.github.com/repos/test-owner/test-repo/contents/README.md",
                "@@ -0,0 +1,10 @@\n+# Test Repository\n+This is a test repository.\n+\n+## Features\n+- Feature 1\n+- Feature 2\n+\n+## Usage\n+See the documentation.\n"
        );

        List<ChangedFileDto> expectedChangedFiles = Arrays.asList(changedFile1, changedFile2);

        // Repository mock 설정
        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber))
                .thenReturn(Optional.of(pullRequest));

        // GithubService mock 설정
        when(githubService.getChangedFiles(accessToken, loginId, repositoryName, prNumber))
                .thenReturn(expectedChangedFiles);

        // when
        List<ChangedFileDto> result = pullRequestService.getPullRequestWithChanges(loginId, repositoryName, prNumber, accessToken);

        // then
        assertEquals(2, result.size());
        
        // 첫 번째 파일 검증
        ChangedFileDto firstFile = result.get(0);
        assertEquals("src/main/java/Example.java", firstFile.getFilename());
        assertEquals("modified", firstFile.getStatus());
        assertEquals(5, firstFile.getAdditions());
        assertEquals(2, firstFile.getDeletions());
        assertEquals(7, firstFile.getChanges());
        assertEquals(100, firstFile.getLines());
        assertTrue(firstFile.getPatch().contains("// 새로운 주석"));
        assertTrue(firstFile.getPatch().contains("private String age"));

        // 두 번째 파일 검증
        ChangedFileDto secondFile = result.get(1);
        assertEquals("README.md", secondFile.getFilename());
        assertEquals("added", secondFile.getStatus());
        assertEquals(10, secondFile.getAdditions());
        assertEquals(0, secondFile.getDeletions());
        assertEquals(10, secondFile.getChanges());
        assertEquals(10, secondFile.getLines());
        assertTrue(secondFile.getPatch().contains("# Test Repository"));

        // Repository와 GithubService 메서드 호출 검증
        verify(pullRequestRepository).findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber);
        verify(githubService).getChangedFiles(accessToken, loginId, repositoryName, prNumber);
    }

    @Test
    void getPullRequestWithChanges_PR존재하지않음_예외발생() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        Integer prNumber = 999;
        String accessToken = "test-access-token";

        // Repository에서 PR을 찾지 못함
        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber))
                .thenReturn(Optional.empty());

        // when & then
        PullRequestNotFoundEx exception = assertThrows(PullRequestNotFoundEx.class,
                () -> pullRequestService.getPullRequestWithChanges(loginId, repositoryName, prNumber, accessToken));

        assertEquals("Pull request not found for repositoryName: test-repo, loginId: test-owner, prNumber: 999", exception.getMessage());
        
        // Repository 메서드 호출 검증
        verify(pullRequestRepository).findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber);
        // GithubService는 호출되지 않아야 함
        verify(githubService, never()).getChangedFiles(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void getPullRequestWithChanges_변경된파일없음_빈리스트반환() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        Integer prNumber = 123;
        String accessToken = "test-access-token";

        // 테스트용 GithubAccount 생성
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId(loginId)
                .accessToken(accessToken)
                .build();

        // 테스트용 PR 엔티티 생성
        PullRequest pullRequest = PullRequest.builder()
                .prNumber(prNumber)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("Test PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        // Repository mock 설정
        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber))
                .thenReturn(Optional.of(pullRequest));

        // GithubService에서 빈 리스트 반환
        when(githubService.getChangedFiles(accessToken, loginId, repositoryName, prNumber))
                .thenReturn(Arrays.asList());

        // when
        List<ChangedFileDto> result = pullRequestService.getPullRequestWithChanges(loginId, repositoryName, prNumber, accessToken);

        // then
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());

        // Repository와 GithubService 메서드 호출 검증
        verify(pullRequestRepository).findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber);
        verify(githubService).getChangedFiles(accessToken, loginId, repositoryName, prNumber);
    }

    @Test
    void getPullRequestWithChanges_GitHubAPI호출실패_예외전파() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        Integer prNumber = 123;
        String accessToken = "test-access-token";

        // 테스트용 GithubAccount 생성
        GithubAccount githubAccount = GithubAccount.builder()
                .loginId(loginId)
                .accessToken(accessToken)
                .build();

        // 테스트용 PR 엔티티 생성
        PullRequest pullRequest = PullRequest.builder()
                .prNumber(prNumber)
                .repositoryName(repositoryName)
                .githubAccount(githubAccount)
                .title("Test PR")
                .action("opened")
                .status(PullRequest.ReviewStatus.PENDING)
                .build();

        // Repository mock 설정
        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber))
                .thenReturn(Optional.of(pullRequest));

        // GithubService에서 예외 발생
        when(githubService.getChangedFiles(accessToken, loginId, repositoryName, prNumber))
                .thenThrow(new RuntimeException("GitHub API call failed"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> pullRequestService.getPullRequestWithChanges(loginId, repositoryName, prNumber, accessToken));

        assertEquals("GitHub API call failed", exception.getMessage());

        // Repository와 GithubService 메서드 호출 검증
        verify(pullRequestRepository).findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(repositoryName, loginId, prNumber);
        verify(githubService).getChangedFiles(accessToken, loginId, repositoryName, prNumber);
    }

    @Test
    void getPullRequestWithChanges_다른저장소의PR_예외발생() {
        // given
        String loginId = "test-owner";
        String repositoryName = "test-repo";
        String differentRepository = "different-repo";
        Integer prNumber = 123;
        String accessToken = "test-access-token";

        // Repository에서 PR을 찾지 못함 (다른 저장소)
        when(pullRequestRepository.findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(differentRepository, loginId, prNumber))
                .thenReturn(Optional.empty());

        // when & then
        PullRequestNotFoundEx exception = assertThrows(PullRequestNotFoundEx.class,
                () -> pullRequestService.getPullRequestWithChanges(loginId, differentRepository, prNumber, accessToken));

        assertEquals("Pull request not found for repositoryName: different-repo, loginId: test-owner, prNumber: 123", exception.getMessage());

        // Repository 메서드 호출 검증
        verify(pullRequestRepository).findByRepositoryNameAndGithubAccountLoginIdAndPrNumber(differentRepository, loginId, prNumber);
        // GithubService는 호출되지 않아야 함
        verify(githubService, never()).getChangedFiles(anyString(), anyString(), anyString(), anyInt());
    }

    private WebhookPayloadDto createWebhookPayload(String action, int prNumber, String title, String repoName, String ownerLogin) {
        UserDto owner = new UserDto(ownerLogin, 1, "http://test.com/user");
        RepositoryDto repository = new RepositoryDto(repoName, repoName + "/" + ownerLogin, owner);
        PullRequestDto pullRequest = new PullRequestDto(prNumber, title, "body", "open", owner, "http://test.com/pr", "http://test.com/diff");

        return new WebhookPayloadDto(action, pullRequest, repository);
    }
}
