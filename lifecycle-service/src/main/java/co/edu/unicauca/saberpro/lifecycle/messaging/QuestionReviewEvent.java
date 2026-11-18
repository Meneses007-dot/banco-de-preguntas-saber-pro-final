package co.edu.unicauca.saberpro.lifecycle.messaging;

import java.time.Instant;

/**
 * Local copy of the event contract published by {@code question-service}.
 *
 * <p>Duplicated on purpose (decision A17): the official structure has no shared module, so each
 * bounded context owns its own view of the JSON schema, version {@code 1}.</p>
 */
public record QuestionReviewEvent(
        String eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String questionId,
        String status,
        int optionCount) {
}
