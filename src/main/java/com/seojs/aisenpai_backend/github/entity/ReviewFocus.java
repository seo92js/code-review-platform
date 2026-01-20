package com.seojs.aisenpai_backend.github.entity;

import lombok.Getter;

@Getter
public enum ReviewFocus {
    PRAISE_ONLY("잘 작성된 코드, 좋은 패턴, 개선된 점 위주로 칭찬해주세요. 문제점 지적은 최소화해주세요."),
    IMPROVEMENT_ONLY("버그, 성능 이슈, 보안 취약점, 코드 품질 개선점 위주로 피드백해주세요. 칭찬은 최소화해주세요."),
    BOTH("잘 작성된 부분은 칭찬하고, 개선이 필요한 부분은 구체적으로 제안해주세요. 균형 잡힌 리뷰를 제공해주세요.");

    private final String prompt;

    ReviewFocus(String prompt) {
        this.prompt = prompt;
    }
}
