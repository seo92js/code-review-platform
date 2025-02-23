package com.seojs.code_review_platform.controller;

import com.seojs.code_review_platform.dto.GitRepositoryResponseDto;
import com.seojs.code_review_platform.service.GithubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(GithubApiController.class)
@AutoConfigureMockMvc
class GithubApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GithubService githubService;

    @Test
    @WithMockUser
    void getRepositories() throws Exception {
        //given
        List<GitRepositoryResponseDto> repos = Arrays.asList(
                new GitRepositoryResponseDto(),
                new GitRepositoryResponseDto()
        );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("access_token", "test_token");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))
                ,attributes
                ,"access_token"
        );

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                oauth2User,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                "test-client"
        );

        when(githubService.getRepositories(anyString())).thenReturn(repos);

        //when
        //then
        mockMvc.perform(get("/api/github/repositories")
                .with(SecurityMockMvcRequestPostProcessors.authentication(authenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}