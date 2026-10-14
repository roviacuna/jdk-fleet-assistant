package com.roviacuna.fleet;

import com.roviacuna.fleet.controller.EmailController;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@Import(MockChatClientConfiguration.class)
class EmailControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @Test
    void replyReturnsGeneratedEmail() throws Exception {
        givenModelResponds(generation("Hola Ana, gracias por escribirnos.", "end_turn"));

        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "Ana", "customerMessage": "¿Java 21 es LTS?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("Hola Ana, gracias por escribirnos."))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    void replyFillsUserTemplateWithRequestFields() throws Exception {
        givenModelResponds(generation("Hola Ana", "end_turn"));

        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "Ana", "customerMessage": "¿Java 21 es LTS?"}
                                """))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(chatClient.prompt(), atLeastOnce()).user(captor.capture());

        ChatClient.PromptUserSpec userSpec = mock(ChatClient.PromptUserSpec.class, RETURNS_SELF);
        captor.getValue().accept(userSpec);

        verify(userSpec).text(any(Resource.class));
        verify(userSpec).param("customerName", "Ana");
        verify(userSpec).param("customerMessage", "¿Java 21 es LTS?");
    }

    @Test
    void replyFlagsTruncatedEmailWhenMaxTokensReached() throws Exception {
        givenModelResponds(generation("Hola Ana, la respuesta larga se cor", "max_tokens"));

        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "Ana", "customerMessage": "Explícame todo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.truncated").value(true));
    }

    @Test
    void replyReturnsBadGatewayWhenModelReturnsNoResults() throws Exception {
        givenModelResponds();

        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "Ana", "customerMessage": "¿Java 21 es LTS?"}
                                """))
                .andExpect(status().isBadGateway());
    }

    @Test
    void replyRejectsBlankCustomerName() throws Exception {
        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": " ", "customerMessage": "Hola"}
                                """))
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).prompt();
    }

    @Test
    void replyRejectsBlankCustomerMessage() throws Exception {
        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "Ana", "customerMessage": ""}
                                """))
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).prompt();
    }

    @Test
    void replyRejectsCustomerNameLongerThan100Chars() throws Exception {
        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "%s", "customerMessage": "Hola"}
                                """.formatted("a".repeat(101))))
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).prompt();
    }

    @Test
    void replyRejectsCustomerMessageLongerThan4000Chars() throws Exception {
        mockMvc.perform(post("/api/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName": "Ana", "customerMessage": "%s"}
                                """.formatted("a".repeat(4001))))
                .andExpect(status().isBadRequest());

        verify(chatClient, never()).prompt();
    }

    private void givenModelResponds(Generation... generations) {
        when(chatClient.prompt().user(any(Consumer.class)).call().chatResponse())
                .thenReturn(new ChatResponse(List.of(generations)));
    }

    private static Generation generation(String text, String finishReason) {
        return new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
    }

}
