package com.seojs.code_review_platform.controller;

import com.seojs.code_review_platform.github.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.github.controller.GithubApiController;
import com.seojs.code_review_platform.github.service.GithubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(GithubApiController.class)
@AutoConfigureMockMvc
class GithubApiControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    GithubService githubService;

    @MockitoBean
    OAuth2AuthorizedClientService authorizedClientService;

    @Test
    void getRepositories() throws Exception {
        // given
        List<GitRepositoryResponseDto> repos = Arrays.asList(
                new GitRepositoryResponseDto(),
                new GitRepositoryResponseDto()
        );

        // 사용자 정보 설정
        String userName = "seo92js";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("login", userName);
        attributes.put("name", "seo92js");
        attributes.put("email", "seo92js@gmail.com");

        // OAuth2User 객체 생성
        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "login"
        );

        // OAuth2AuthorizedClient 모킹
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("github")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("client-id")
                .clientSecret("client-secret")
                .redirectUri("http://localhost/login/oauth2/code/github")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .clientName("GitHub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-token-value",
                Instant.now(),
                Instant.now().plus(1, ChronoUnit.HOURS)
        );

        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                clientRegistration,
                userName,
                accessToken
        );

        // authorizedClientService 모킹
        when(authorizedClientService.loadAuthorizedClient("github", userName))
                .thenReturn(authorizedClient);

        // githubService 모킹
        when(githubService.getRepositories("test-token-value")).thenReturn(repos);

        // when & then
        ResultActions resultActions = mockMvc.perform(get("/api/github/repositories")
                        .with(oauth2Login()
                                .clientRegistration(clientRegistration)
                                .oauth2User(oauth2User)))
                .andExpect(status().isOk());

        System.out.printf(resultActions.andReturn().getResponse().getContentAsString());
    }
}