package com.seojs.code_review_platform.github.service;

import com.seojs.code_review_platform.github.entity.GithubAccount;
import com.seojs.code_review_platform.github.repository.GithubAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final GithubAccountRepository githubAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String accessToken = userRequest.getAccessToken().getTokenValue();
        String loginId = oAuth2User.getAttribute("login");
        if (loginId != null && accessToken != null) {
            String encryptedToken = tokenEncryptionService.encryptToken(accessToken);
            githubAccountRepository.findByLoginId(loginId)
                .ifPresentOrElse(
                    account -> {
                        account.updateAccessToken(encryptedToken);},
                    () -> {
                        GithubAccount newAccount = GithubAccount.builder().loginId(loginId).accessToken(encryptedToken).build();
                        githubAccountRepository.save(newAccount);
                    }
                );
        }
        return oAuth2User;
    }
}
