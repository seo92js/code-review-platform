package com.seojs.code_review_platform.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
@Service
@Slf4j
public class WebhookSecurityService {
    private final GithubService githubService;
    private final ObjectMapper objectMapper;
    
    /**
     * 웹훅 시그니처 검증 (페이로드에서 소유자 추출)
     * @param payload 웹훅 페이로드
     * @param signature X-Hub-Signature-256 헤더 값
     */
    public void validateWebhookSignature(String payload, String signature) {
        try {
            // 페이로드에서 repository 소유자 추출
            JsonNode payloadJson = objectMapper.readTree(payload);
            String repositoryFullName = payloadJson.path("repository").path("full_name").asText();
            
            if (repositoryFullName.isEmpty()) {
                throw new SecurityException("Repository information not found in webhook payload");
            }
            
            String owner = repositoryFullName.split("/")[0];
            
            GithubAccount account = githubService.findByLoginIdOrThrow(owner);

            // 사용자별 웹훅 시크릿으로 검증
            isValidWebhookSignature(payload, signature, account.getWebhookSecret());
            
        } catch (Exception e) {
            log.error("웹훅 시그니처 검증 실패: {}", e.getMessage());
            throw new SecurityException("Webhook signature validation failed: " + e.getMessage());
        }
    }

    /**
     * GitHub 웹훅 시그니처 검증 (사용자별 시크릿)
     */
    public void isValidWebhookSignature(String payload, String signature, String userWebhookSecret) {
        boolean isValid = true;
        
        if (signature == null || signature.isEmpty()) {
            isValid = false;
        }
        
        if (userWebhookSecret == null || userWebhookSecret.isEmpty()) {
            isValid = false;
        }
        
        // GitHub이 보낸 시그니처 형식: "sha256=..."
        if (!signature.startsWith("sha256=")) {
            isValid = false;
        }
        
        // 예상 시그니처 생성
        String expectedSignature = "sha256=" + 
            calculateHmacSha256(userWebhookSecret, payload);
        
        // 시그니처 비교 (타이밍 공격 방지를 위해 MessageDigest.isEqual 사용)
        isValid = MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );

        if (!isValid) {
            throw new SecurityException("Invalid webhook signature");
        }
    }
    
    
    /**
     * HMAC-SHA256 계산
     */
    private String calculateHmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            
            byte[] hmacData = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder result = new StringBuilder();
            for (byte b : hmacData) {
                result.append(String.format("%02x", b));
            }
            
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC calculation failed", e);
        }
    }
}
