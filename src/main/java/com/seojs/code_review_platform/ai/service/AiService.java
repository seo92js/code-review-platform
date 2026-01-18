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

    public String callAiChat(String apiKey, String systemPrompt, String userPrompt, String model, Double temperature) {
        String actualModel = (model != null) ? model : "gpt-4o-mini";
        double actualTemp = (temperature != null) ? temperature : 0.7;

        OpenAiApi userApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(actualModel)
                .temperature(actualTemp)
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

    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        try {
            // 최소 토큰으로 API 호출 시도
            callAiChat(apiKey, "Validation", "ping", "gpt-4o-mini", 0.1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
