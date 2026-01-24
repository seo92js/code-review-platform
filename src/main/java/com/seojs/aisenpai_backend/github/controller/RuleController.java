package com.seojs.aisenpai_backend.github.controller;

import com.seojs.aisenpai_backend.github.dto.RuleResponseDto;
import com.seojs.aisenpai_backend.github.dto.RuleSaveDto;
import com.seojs.aisenpai_backend.github.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public List<RuleResponseDto> getRules(@AuthenticationPrincipal OAuth2User principal) {
        String loginId = principal.getAttribute("login");
        return ruleService.getRules(loginId);
    }

    @PostMapping
    public RuleResponseDto createRule(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody RuleSaveDto request) {
        String loginId = principal.getAttribute("login");
        return ruleService.createRule(loginId, request);
    }

    @PutMapping("/{ruleId}")
    public RuleResponseDto updateRule(
            @PathVariable Long ruleId,
            @RequestBody RuleSaveDto request) {
        return ruleService.updateRule(ruleId, request);
    }

    @DeleteMapping("/{ruleId}")
    public void deleteRule(@PathVariable Long ruleId) {
        ruleService.deleteRule(ruleId);
    }

    @PatchMapping("/{ruleId}/toggle")
    public RuleResponseDto toggleRule(@PathVariable Long ruleId) {
        return ruleService.toggleRule(ruleId);
    }
}
