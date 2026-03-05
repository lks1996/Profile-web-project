package com.example.profile.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ChatRequestDto {
    private List<ChatMessage> history; // 이전 대화 기록들
    private String newMessage;         // 이번에 새로 던진 질문

    @Getter
    @Setter
    public static class ChatMessage {
        private String role; // "user" 또는 "model"
        private String text; // 대화 내용
    }
}
