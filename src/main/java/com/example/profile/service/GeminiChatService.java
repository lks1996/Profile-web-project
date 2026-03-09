package com.example.profile.service;

import com.example.profile.dto.ChatRequestDto;
import com.example.profile.dto.ProfileWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiChatService {

    private final FrontProfileService frontProfileService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().baseUrl("https://generativelanguage.googleapis.com").build();

    @Value("${api.gemini.api-key}")
    private String geminiApiKey;

    public Flux<String> streamChatResponse(ChatRequestDto requestDto) {
        try {
            // 1. 포트폴리오 데이터 확보.
            ProfileWrapper profile = frontProfileService.getPublicProfile();
            String portfolioJson = objectMapper.writeValueAsString(profile);

            // 2. 시스템 프롬프트 조립.
            String systemPrompt = "너는 백엔드 개발자 '이경석'의 포트폴리오 안내 챗봇이야. " +
                    "반드시 제공된 포트폴리오 JSON 데이터만을 기반으로 답변해. " +
                    "포트폴리오 내용과 관련 없는 질문은 단호하게 거절해. 모르는 내용은 지어내지 마.\n" +
                    "데이터: " + portfolioJson;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
            ));

            // 3. 대화 맥락(History) 조립.
            List<Map<String, Object>> contents = new ArrayList<>();

            // 프론트에서 넘겨준 과거 대화 데이터 적재.
            if (requestDto.getHistory() != null) {
                for (ChatRequestDto.ChatMessage msg : requestDto.getHistory()) {
                    contents.add(Map.of(
                            "role", msg.getRole(), // "user" 또는 "model"
                            "parts", List.of(Map.of("text", msg.getText()))
                    ));
                }
            }

            // 4. 새로 입력한 질문 추가.
            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", requestDto.getNewMessage()))
            ));

            requestBody.put("contents", contents);

            // 5. API 스트리밍 요청.
            return webClient.post()
//                    .uri("/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse")
                    .uri("/v1beta/models/gemini-3.1-flash-lite-preview:streamGenerateContent?alt=sse")
                    .header("x-goog-api-key", geminiApiKey) // URL 대신 헤더에 안전하게 API 키 삽입
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class);

        } catch (Exception e) {
            return Flux.just("시스템 에러: 포트폴리오 데이터를 처리하지 못했습니다.");
        }
    }
}