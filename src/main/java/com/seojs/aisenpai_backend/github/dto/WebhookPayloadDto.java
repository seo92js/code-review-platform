package com.seojs.aisenpai_backend.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class WebhookPayloadDto {
    private final String action;
    @JsonProperty("pull_request")
    private final PullRequestDto pullRequest;
    private final RepositoryDto repository;

    @Getter
    @AllArgsConstructor
    public static class PullRequestDto {
        private final int number;
        private final String title;
        private final String body;
        private final String state;
        private final UserDto user;
        private final String htmlUrl;
        private final String diffUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class RepositoryDto {
        private final Long id;
        private final String name;
        private final String fullName;
        private final UserDto owner;
    }

    @Getter
    @AllArgsConstructor
    public static class UserDto {
        private final String login;
        private final int id;
        private final String htmlUrl;
    }
}
