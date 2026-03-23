package com.server.board.domain.dto;

import java.util.List;

public record ChatResponse(
        boolean ok,
        String reply,
        List<Integer> matchedIds,
        String errorMessage
) {
    public static ChatResponse success(String reply, List<Integer> matchedIds) {
        return new ChatResponse(true, reply, matchedIds, null);
    }

    public static ChatResponse error(String errorMessage) {
        return new ChatResponse(false, null, null, errorMessage);
    }
}
