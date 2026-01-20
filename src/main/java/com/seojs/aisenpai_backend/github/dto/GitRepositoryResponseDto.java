package com.seojs.aisenpai_backend.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Getter
@Service
@NoArgsConstructor
public class GitRepositoryResponseDto {
    private Long id;

    private String name;

    private boolean isPrivate;

    private String description;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("updated_at")
    private String updatedAt;

    private String owner;

    @JsonProperty("private")
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    @JsonProperty("owner")
    public void setOwner(Map<String, Object> owner) {
        this.owner = owner.get("login").toString();
    }
}
