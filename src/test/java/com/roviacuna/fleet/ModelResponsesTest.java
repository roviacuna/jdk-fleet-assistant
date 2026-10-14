package com.roviacuna.fleet;

import com.roviacuna.fleet.common.ModelResponses;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelResponsesTest {

    @Test
    void requireResultsReturnsResponseWithResults() {
        ChatResponse response = response(generation("Hi", "end_turn"));

        assertThat(ModelResponses.requireResults(response)).isSameAs(response);
    }

    @Test
    void requireResultsRejectsNullResponse() {
        assertThatThrownBy(() -> ModelResponses.requireResults(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void requireResultsRejectsResponseWithoutResults() {
        assertThatThrownBy(() -> ModelResponses.requireResults(response()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void textJoinsGenerationsAndSkipsBlankOnes() {
        ChatResponse response = response(
                generation("First part", "end_turn"),
                generation("   ", "end_turn"),
                generation(null, "end_turn"),
                generation("Second part", "end_turn"));

        assertThat(ModelResponses.text(response)).isEqualTo("First part\nSecond part");
    }

    @Test
    void textIsEmptyWhenAllGenerationsAreBlank() {
        assertThat(ModelResponses.text(response(generation("", "end_turn")))).isEmpty();
    }

    @Test
    void truncatedDetectsAnthropicAndOpenAiReasonsIgnoringCase() {
        assertThat(ModelResponses.truncated(response(generation("cut", "max_tokens")))).isTrue();
        assertThat(ModelResponses.truncated(response(generation("cut", "LENGTH")))).isTrue();
    }

    @Test
    void truncatedIsTrueWhenAnyGenerationWasCut() {
        ChatResponse response = response(
                generation("complete", "end_turn"),
                generation("cut", "max_tokens"));

        assertThat(ModelResponses.truncated(response)).isTrue();
    }

    @Test
    void truncatedIsFalseForNormalOrMissingFinishReason() {
        assertThat(ModelResponses.truncated(response(generation("done", "end_turn")))).isFalse();
        assertThat(ModelResponses.truncated(response(generation("done", "stop")))).isFalse();
        assertThat(ModelResponses.truncated(response(new Generation(new AssistantMessage("done"))))).isFalse();
    }

    private static ChatResponse response(Generation... generations) {
        return new ChatResponse(List.of(generations));
    }

    private static Generation generation(String text, String finishReason) {
        return new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
    }

}
