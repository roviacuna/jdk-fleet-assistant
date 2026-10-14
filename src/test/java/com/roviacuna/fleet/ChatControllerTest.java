package com.roviacuna.fleet;

import com.roviacuna.fleet.controller.ChatController;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(MockChatClientConfiguration.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @Test
    void chatReturnsModelReply() throws Exception {
        givenModelResponds(generation("Hello from the model", "end_turn"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Hi"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello from the model"))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    void chatJoinsGenerationsAndSkipsBlankOnes() throws Exception {
        givenModelResponds(
                generation("First part", "end_turn"),
                generation("   ", "end_turn"),
                generation("Second part", "end_turn"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Hi"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("First part\nSecond part"));
    }

    @Test
    void chatFlagsTruncatedReplyWhenMaxTokensReached() throws Exception {
        givenModelResponds(generation("A very long answer that got cut", "max_tokens"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Explain everything"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truncated").value(true));
    }

    @Test
    void chatReturnsBadGatewayWhenModelReturnsNoResults() throws Exception {
        givenModelResponds();

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Hi"}
                                """))
                .andExpect(status().isBadGateway());
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": ""}
                                """))
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).prompt();
    }

    @Test
    void chatRejectsMessageLongerThan2000Chars() throws Exception {
        String tooLong = "a".repeat(2001);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "%s"}
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).prompt();
    }

    private void givenModelResponds(Generation... generations) {
        when(chatClient.prompt().user(anyString()).call().chatResponse())
                .thenReturn(new ChatResponse(List.of(generations)));
    }

    private static Generation generation(String text, String finishReason) {
        return new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
    }

}
