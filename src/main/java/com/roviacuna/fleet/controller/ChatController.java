package com.roviacuna.fleet.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    private static final Set<String> TRUNCATION_REASONS = Set.of("max_tokens", "length");

    public record ChatRequest(@NotBlank @Size(max = 2000) String message) {
    }

    public record ChatResponse(String reply, boolean truncated) {
    }

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        var response = chatClient.prompt()
                .user(request.message())
                .call()
                .chatResponse();

        if (log.isDebugEnabled()) {
            response.getResults().forEach(g -> log.debug("finish={} | text={}",
                    g.getMetadata().getFinishReason(), g.getOutput().getText()));
            log.debug("usage={}", response.getMetadata().getUsage());
        }

        String reply = response.getResults().stream()
                .map(g -> g.getOutput().getText())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"));

        boolean truncated = response.getResults().stream()
                .map(g -> g.getMetadata().getFinishReason())
                .anyMatch(reason -> reason != null && TRUNCATION_REASONS.contains(reason.toLowerCase()));

        return new ChatResponse(reply, truncated);
    }

}