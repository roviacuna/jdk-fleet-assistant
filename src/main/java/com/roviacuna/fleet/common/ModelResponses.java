package com.roviacuna.fleet.common;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

public final class ModelResponses {

    private static final Set<String> TRUNCATION_REASONS = Set.of("max_tokens", "length");

    private ModelResponses() {
    }

    public static ChatResponse requireResults(ChatResponse response) {
        if (response == null || response.getResults().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "The model returned no response");
        }
        return response;
    }

    public static String text(ChatResponse response) {
        return response.getResults().stream()
                .map(g -> g.getOutput().getText())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));
    }

    public static boolean truncated(ChatResponse response) {
        return response.getResults().stream()
                .map(g -> g.getMetadata().getFinishReason())
                .anyMatch(reason -> reason != null && TRUNCATION_REASONS.contains(reason.toLowerCase()));
    }
}