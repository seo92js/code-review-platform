package com.seojs.code_review_platform.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Getter
@Service
@NoArgsConstructor
public class GitRepositoryResponseDto {
    private Long id;

    private String name;

    private boolean isPrivate;

    private String description;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("private")
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
