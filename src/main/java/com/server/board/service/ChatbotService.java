package com.server.board.service;

import com.server.board.domain.dto.ViewPageData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private final RestClient restClient;
    private final String apiKey;
    private final String apiUrl;
    private final MainService mainService;

    public ChatbotService(
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.url}") String apiUrl,
            MainService mainService
    ) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.mainService = mainService;
        this.restClient = RestClient.create();
    }

    private String buildSystemPrompt() {
        List<ViewPageData> pageDataList = mainService.viewPageDataAll();

        StringBuilder sb = new StringBuilder();
        sb.append("당신은 포트폴리오 사이트 안내 챗봇입니다. 아래 페이지 데이터를 기반으로 사용자 질문에 답변하세요. ");
        sb.append("데이터에 없는 내용은 '해당 내용은 확인이 어렵습니다.'라고 답변하세요.\n\n");
        sb.append("페이지 데이터:\n");
        for (ViewPageData data : pageDataList) {
            String decodedContent;
            try {
                decodedContent = new String(Base64.getDecoder().decode(data.content()), StandardCharsets.UTF_8);
            } catch (Exception e) {
                decodedContent = data.content();
            }
            sb.append("[페이지: ").append(data.pagename()).append("]\n");
            sb.append("내용: ").append(decodedContent).append("\n");
            if (data.memo() != null && !data.memo().isBlank()) {
                sb.append("메모: ").append(data.memo()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public String chat(String message)
    {
        String systemPrompt = buildSystemPrompt();

        // Gemini API 요청 바디 구성 (system_instruction + user message)
        // claude의 경우 다른 바디로 구성
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", message)
                        ))
                )
        );
        //        Claude API 형식:
        //        {
        //            "model":"claude-sonnet-4-6",
        //                "max_tokens":1024,
        //                "system":"...",
        //                "messages": [{
        //            "role":"user", "content":"user message" }]
        //        }

        // Gemini API 호출
        Map<?, ?> response = restClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        // 응답에서 텍스트 추출
        try {
            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            List<?> parts = (List<?>) content.get("parts");
            return (String) ((Map<?, ?>) parts.get(0)).get("text");
        } catch (Exception e) {
            throw new RuntimeException("Gemini 응답 파싱 실패: " + e.getMessage());
        }
    }
}
