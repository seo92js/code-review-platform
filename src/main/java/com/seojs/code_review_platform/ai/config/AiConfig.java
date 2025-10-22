package com.seojs.code_review_platform.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel model) {
        return ChatClient.create(model);
    }
}
