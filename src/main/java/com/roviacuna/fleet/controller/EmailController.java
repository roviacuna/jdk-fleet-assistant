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
public class EmailController {

    private final ChatClient chatClient;
    private final Resource userTemplate;

    public record EmailRequest(
            @NotBlank @Size(max = 100) String customerName,
            @NotBlank @Size(max = 4000) String customerMessage) {
    }

    public record EmailResponse(String email, boolean truncated) {
    }

    public EmailController(ChatClient.Builder builder,
                           @Value("classpath:prompts/email-system.st") Resource systemPrompt,
                           @Value("classpath:prompts/email-user.st") Resource userTemplate) {
        this.chatClient = builder.defaultSystem(systemPrompt).build();
        this.userTemplate = userTemplate;
    }

    @PostMapping("/email")
    public EmailResponse reply(@Valid @RequestBody EmailRequest request) {
        var response = ModelResponses.requireResults(chatClient.prompt()
                .user(u -> u.text(userTemplate)
                        .param("customerName", request.customerName())
                        .param("customerMessage", request.customerMessage()))
                .call()
                .chatResponse());

        return new EmailResponse(ModelResponses.text(response), ModelResponses.truncated(response));
    }

}