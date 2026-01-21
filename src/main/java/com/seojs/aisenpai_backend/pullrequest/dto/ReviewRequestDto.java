package com.seojs.aisenpai_backend.pullrequest.dto;

import com.seojs.aisenpai_backend.github.dto.ChangedFileDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequestDto {
    private Long repositoryId;
    private Integer prNumber;
    private List<ChangedFileDto> changedFiles;
    private String model;
    private String systemPrompt;
    private String encryptedOpenAiKey;
}
