package com.example.profile.controller;

import com.example.profile.dto.ChatRequestDto; // 방금 만든 DTO 임포트
import com.example.profile.service.GeminiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GeminiChatService geminiChatService;

    @PostMapping(value = "/stream", produces = "text/event-stream")
    public ResponseEntity<SseEmitter> chatStream(@RequestBody ChatRequestDto request) {

        String userMessage = request.getNewMessage();

        // 1차 방어벽: 새로운 메시지 검증
        if (userMessage == null || userMessage.trim().isEmpty() || userMessage.length() > 200) {
            throw new IllegalArgumentException("메시지는 1자 이상, 200자 이하로 입력해주세요.");
        }

        // SSE 연결 유지 시간 30초 설정
        SseEmitter emitter = new SseEmitter(30000L);

        // 서비스로 DTO 전체(과거 내역 + 새로운 질문)를 넘겨줍니다.
        geminiChatService.streamChatResponse(request)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );

        return ResponseEntity.ok(emitter);
    }
}