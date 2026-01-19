package com.seojs.code_review_platform.github.service;

import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final GithubAccountRepository githubAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String accessToken = userRequest.getAccessToken().getTokenValue();
        String loginId = oAuth2User.getAttribute("login");
        if (loginId != null && accessToken != null) {
            String encryptedToken = tokenEncryptionService.encryptToken(accessToken);
            githubAccountRepository.findByLoginId(loginId)
                    .ifPresentOrElse(
                            account -> {
                                account.updateAccessToken(encryptedToken);
                            },
                            () -> {
                                String webhookSecret = generateWebhookSecret();
                                GithubAccount newAccount = GithubAccount.builder()
                                        .loginId(loginId)
                                        .accessToken(encryptedToken)
                                        .webhookSecret(webhookSecret)
                                        .build();
                                githubAccountRepository.save(newAccount);
                                newAccount.initializeAiSettings();
                            });
        }
        return oAuth2User;
    }

    /**
     * 웹훅 시크릿 생성
     */
    private String generateWebhookSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
