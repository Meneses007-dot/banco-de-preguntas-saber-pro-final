package co.edu.unicauca.saberpro.question.infrastructure.messaging;

import java.time.Instant;

/**
 * Integration event published when an item is submitted to review.
 *
 * <p>This DTO is duplicated in {@code lifecycle-service} on purpose: the official structure has
 * no shared module, so each service owns its own copy of the versioned JSON contract.</p>
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
