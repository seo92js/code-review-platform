package com.seojs.code_review_platform.dto;

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
    private String fullName;
    private boolean isPrivate;
    private String description;
    private String url;

    @JsonProperty("private")
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
