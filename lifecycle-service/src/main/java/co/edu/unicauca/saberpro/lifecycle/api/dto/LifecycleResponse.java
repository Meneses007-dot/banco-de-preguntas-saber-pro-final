package co.edu.unicauca.saberpro.lifecycle.api.dto;

import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycle;
import java.time.Instant;

/**
 * Read model exposed by {@code GET /api/lifecycle/questions/{id}}.
 */
public record LifecycleResponse(
        String questionId,
        String status,
        String eventId,
        Instant occurredAt,
        Instant processedAt) {

    public static LifecycleResponse from(QuestionLifecycle lifecycle) {
        return new LifecycleResponse(
                lifecycle.getQuestionId(),
                lifecycle.status(),
                lifecycle.getEventId(),
                lifecycle.getOccurredAt(),
                lifecycle.getProcessedAt());
    }
}
