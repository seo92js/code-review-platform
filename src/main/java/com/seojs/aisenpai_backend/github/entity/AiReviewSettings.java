package com.seojs.aisenpai_backend.github.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class AiReviewSettings {
    @Id
    private Long id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private GithubAccount githubAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewTone reviewTone = ReviewTone.NEUTRAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewFocus reviewFocus = ReviewFocus.BOTH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DetailLevel detailLevel = DetailLevel.STANDARD;

    @OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rule> rules = new ArrayList<>();

    @Column(length = 1000)
    private String ignorePatterns;

    @Column
    private String openAiKey;

    @Column(nullable = false)
    private Boolean autoReviewEnabled = false;

    @Column(nullable = false)
    private Boolean autoPostToGithub = false;

    @Column(nullable = false)
    private String openaiModel = "gpt-4o-mini";

    @Builder
    public AiReviewSettings(GithubAccount githubAccount) {
        this.githubAccount = githubAccount;
        this.reviewTone = ReviewTone.NEUTRAL;
        this.reviewFocus = ReviewFocus.BOTH;
        this.detailLevel = DetailLevel.STANDARD;
        this.autoReviewEnabled = false;
        this.autoPostToGithub = false;
        this.openaiModel = "gpt-4o-mini";
        this.ignorePatterns = "package-lock.json, yarn.lock, *.lock, .env*, *.pem, *.key, .yml, .yaml";
    }

    public void updateReviewSettings(ReviewTone tone, ReviewFocus focus, DetailLevel detailLevel,
            Boolean autoReviewEnabled, Boolean autoPostToGithub, String openaiModel) {
        this.reviewTone = tone;
        this.reviewFocus = focus;
        this.detailLevel = detailLevel;
        this.autoReviewEnabled = autoReviewEnabled != null ? autoReviewEnabled : false;
        this.autoPostToGithub = autoPostToGithub != null ? autoPostToGithub : false;
        this.openaiModel = openaiModel != null ? openaiModel : "gpt-4o-mini";
    }

    public void updateIgnorePatterns(String ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    public void updateOpenAiKey(String openAiKey) {
        this.openAiKey = openAiKey;
    }

    public List<String> getIgnorePatternsAsList() {
        if (this.ignorePatterns == null || this.ignorePatterns.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(this.ignorePatterns.split("\\s*,\\s*"));
    }

    /**
     * 설정된 옵션들을 조합하여 최종 시스템 프롬프트를 생성
     */
    public String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 시니어 코드 리뷰어입니다.\n\n");
        sb.append("주어진 변경된 파일 목록을 바탕으로 코드 리뷰를 작성해 주세요.\n\n");

        sb.append("### 리뷰 톤\n");
        sb.append(this.reviewTone.getPrompt()).append("\n\n");

        sb.append("### 리뷰 포커스\n");
        sb.append(this.reviewFocus.getPrompt()).append("\n\n");

        sb.append("### 상세 수준\n");
        sb.append(this.detailLevel.getPrompt()).append("\n\n");

        if (this.rules != null && !this.rules.isEmpty()) {
            List<String> activeRules = this.rules.stream()
                    .filter(Rule::isEnabled)
                    .map(rule -> {
                        String prefix = (rule.getTargetFilePattern() != null && !rule.getTargetFilePattern().isBlank())
                                ? "[Target: " + rule.getTargetFilePattern() + "] "
                                : "";
                        return "- " + prefix + rule.getContent();
                    })
                    .toList();

            if (!activeRules.isEmpty()) {
                sb.append("### 코드 리뷰 규칙\n");
                sb.append("다음 규칙들을 엄격하게 준수하여 리뷰해 주세요:\n");
                activeRules.forEach(rule -> sb.append(rule).append("\n"));
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}
