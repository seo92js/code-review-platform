package com.seojs.aisenpai_backend.github.service;

import com.seojs.aisenpai_backend.exception.RuleNotFoundEx;

import com.seojs.aisenpai_backend.github.dto.RuleResponseDto;
import com.seojs.aisenpai_backend.github.dto.RuleSaveDto;
import com.seojs.aisenpai_backend.github.entity.AiReviewSettings;
import com.seojs.aisenpai_backend.github.entity.GithubAccount;
import com.seojs.aisenpai_backend.github.entity.Rule;
import com.seojs.aisenpai_backend.github.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final GithubService githubService;

    @Transactional(readOnly = true)
    public List<RuleResponseDto> getRules(String loginId) {
        GithubAccount account = githubService.findByLoginIdOrThrow(loginId);
        Long settingsId = account.getAiSettings().getId();
        return ruleRepository.findBySettingsId(settingsId).stream()
                .map(RuleResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public RuleResponseDto createRule(String loginId, RuleSaveDto request) {
        GithubAccount account = githubService.findByLoginIdOrThrow(loginId);
        AiReviewSettings settings = account.getAiSettings();

        Rule rule = Rule.builder()
                .settings(settings)
                .content(request.getContent())
                .isEnabled(true) // 기본값 활성화
                .targetFilePattern(request.getTargetFilePattern())
                .build();

        return RuleResponseDto.from(ruleRepository.save(rule));
    }

    @Transactional
    public RuleResponseDto updateRule(Long ruleId, RuleSaveDto request) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundEx("Rule not found with id: " + ruleId));

        rule.update(request.getContent(), rule.isEnabled(), request.getTargetFilePattern());
        return RuleResponseDto.from(rule);
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        ruleRepository.deleteById(ruleId);
    }

    @Transactional
    public RuleResponseDto toggleRule(Long ruleId) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuleNotFoundEx("Rule not found with id: " + ruleId));

        rule.toggle();
        return RuleResponseDto.from(rule);
    }
}
