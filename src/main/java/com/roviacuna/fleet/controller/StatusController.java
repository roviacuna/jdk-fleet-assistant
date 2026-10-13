package com.roviacuna.fleet.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class StatusController {

    private final ChatClient chatClient;

    public StatusController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("service", "jdk-fleet-assistant", "phase", 1);
    }

    // Prueba de punta a punta: confirma que la API key y el modelo responden.
    @GetMapping("/ai/ping")
    public Map<String, String> aiPing() {
        String reply = chatClient.prompt().user("Reply with exactly one word: OK").call().content();
        return Map.of("model_reply", reply);
    }

}