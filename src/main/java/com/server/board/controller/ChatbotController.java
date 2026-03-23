package com.server.board.controller;

import com.server.board.domain.dto.ChatRequest;
import com.server.board.domain.dto.ChatResponse;
import com.server.board.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/aichat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            System.out.println("메시지를 입력해 주세요.");
            return ResponseEntity.badRequest().body(ChatResponse.error("메시지를 입력해 주세요."));
        }
        System.out.println("Pass!");
        System.out.println(request.message());


        try {
            String reply = chatbotService.chat(request.message());
            return ResponseEntity.ok(ChatResponse.success(reply, null));
        } catch (Exception e) {
            System.out.println("chatbotService.chat exception Error : "+e);
            return ResponseEntity.internalServerError().body(ChatResponse.error("챗봇 오류: " + e.getMessage()));
        }
    }
}
