package com.seojs.aisenpai_backend.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
@Service
public class AiService {

    public String callAiChat(String apiKey, String systemPrompt, String userPrompt, String model, Double temperature) {
        String actualModel = (model != null) ? model : "gpt-4o-mini";
        double actualTemp = (temperature != null) ? temperature : 0.7;

        log.info("AI review started with model: {}", actualModel);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(120 * 1000);
        requestFactory.setConnectTimeout(60 * 1000);

        RestClient.Builder restClientBuilder = RestClient
                .builder()
                .requestFactory(requestFactory);

        OpenAiApi userApi = OpenAiApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
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

        String result = customClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        log.info("AI review completed");
        return result;
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
