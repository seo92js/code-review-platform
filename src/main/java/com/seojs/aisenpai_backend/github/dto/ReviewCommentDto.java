package com.seojs.aisenpai_backend.github.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    private Integer line;

    @JsonAlias("comment")
    private String body;
}
