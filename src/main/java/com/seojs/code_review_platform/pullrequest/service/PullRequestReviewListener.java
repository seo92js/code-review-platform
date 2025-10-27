package com.seojs.code_review_platform.pullrequest.service;

import com.seojs.code_review_platform.ai.service.AiService;
import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.pullrequest.dto.ReviewRequestDto;
import com.seojs.code_review_platform.pullrequest.entity.PullRequest.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PullRequestReviewListener {
    private final AiService aiService;
    private final ObjectMapper objectMapper;
    private final PullRequestService pullRequestService;

    @Async
    @EventListener
    public void handleReviewRequested(ReviewRequestDto dto) {
        List<ChangedFileDto> changedFiles = dto.getChangedFiles();
        String loginId = dto.getLoginId();
        String repositoryName = dto.getRepositoryName();
        Integer prNumber = dto.getPrNumber();

        String systemPrompt = """
                당신은 트렌디하고 능숙한 시니어 코드 리뷰어입니다.
                
                주어진 변경된 파일 목록을 바탕으로 코드 리뷰를 작성해 주세요.
                
                리뷰 지침
                1.  **전체 요약:** 변경 사항의 주요 목적과 영향을 간략히 요약합니다.
                2.  **주요 변경점:** 각 파일별 중요한 변경 사항을 설명합니다.
                3.  **개선 제안:** 코드 가독성, 성능, 유지보수성, 잠재적 버그, 보안 취약점 관점에서 구체적인 개선 방안을 제안합니다. (코드 스니펫 포함 권장)
                4.  **칭찬:** 잘 작성된 코드나 좋은 개선점은 칭찬합니다.
                5.  **질문:** 명확하지 않거나 의도가 불분명한 부분에 대해 질문합니다.
                6.  **결론:** 리뷰 내용을 종합하고 최종 의견을 제시합니다.
                """;

        try {
            String userPrompt = objectMapper.writeValueAsString(changedFiles);
            String review = aiService.callAiChat(systemPrompt, userPrompt);
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber, review, ReviewStatus.COMPLETED);
        } catch (Exception e) {
            pullRequestService.updateAiReview(repositoryName, loginId, prNumber, "AI review failed", ReviewStatus.FAILED);
        }
    }
}
