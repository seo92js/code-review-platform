package com.seojs.code_review_platform.pullrequest.dto;

import com.seojs.code_review_platform.github.dto.ChangedFileDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    private String loginId;
    private String repositoryName;
    private Integer prNumber;
    private List<ChangedFileDto> changedFiles;
    private String model;
}
