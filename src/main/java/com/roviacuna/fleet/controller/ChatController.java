package com.roviacuna.fleet.controller;

import com.roviacuna.fleet.common.ModelResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public record ChatRequest(@NotBlank @Size(max = 2000) String message) {
    }

    public record ChatReply(String reply, boolean truncated) {
    }

    public ChatController(ChatClient.Builder builder,
                          @Value("classpath:prompts/chat-system.st") Resource systemPrompt) {
        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .build();
    }

    @PostMapping("/chat")
    public ChatReply chat(@Valid @RequestBody ChatRequest request) {
        var response = ModelResponses.requireResults(chatClient.prompt()
                .user(request.message())
                .call()
                .chatResponse());

        return new ChatReply(ModelResponses.text(response), ModelResponses.truncated(response));
    }

}