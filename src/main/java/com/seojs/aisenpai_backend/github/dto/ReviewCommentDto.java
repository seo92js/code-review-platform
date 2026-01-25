package com.seojs.aisenpai_backend.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommentDto {
    @JsonAlias("file")
    private String path;

    @JsonAlias("codeSnippet")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String codeSnippet;

    private Integer line;

    private String side;

    @JsonAlias("comment")
    private String body;
}
