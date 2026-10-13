package com.roviacuna.fleet;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Los controllers guardan el ChatClient que construyen en su constructor. Si se mockea solo el
 * Builder, Spring lo resetea después de cada test y build() empieza a devolver un ChatClient
 * distinto al que tiene el controller. Por eso el Builder siempre devuelve el ChatClient del
 * test, declarado como @MockitoBean (Spring lo resetea, pero sigue siendo la misma instancia).
 */
@TestConfiguration(proxyBeanMethods = false)
class MockChatClientConfiguration {

    @Bean
    ChatClient.Builder chatClientBuilder(ChatClient chatClient) {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        return builder;
    }

}
