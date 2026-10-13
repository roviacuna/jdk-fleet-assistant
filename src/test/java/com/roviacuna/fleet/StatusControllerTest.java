package com.roviacuna.fleet;

import com.roviacuna.fleet.controller.StatusController;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusController.class)
@Import(MockChatClientConfiguration.class)
class StatusControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @Test
    void statusReturnsServiceNameAndPhase() throws Exception {
        mockMvc.perform(get("/api/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("jdk-fleet-assistant"))
                .andExpect(jsonPath("$.phase").value(1));
    }

    @Test
    void aiPingReturnsModelReply() throws Exception {
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("OK");

        mockMvc.perform(get("/api/ai/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model_reply").value("OK"));
    }

}
