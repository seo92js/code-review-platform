package com.seojs.code_review_platform.ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AiService {

    public String callAiChat(String apiKey, String systemPrompt, String userPrompt) {
        OpenAiApi userApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-4o-mini") // 임시
                .temperature(0.7) // 임시
                .build();

        OpenAiChatModel customModel = OpenAiChatModel.builder()
                .openAiApi(userApi)
                .defaultOptions(options)
                .build();

        ChatClient customClient = ChatClient.builder(customModel).build();

        return customClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
