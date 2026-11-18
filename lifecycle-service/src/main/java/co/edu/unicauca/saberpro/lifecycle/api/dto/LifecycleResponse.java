package co.edu.unicauca.saberpro.lifecycle.api.dto;

import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycle;
import java.time.Instant;

/**
 * Read model exposed by {@code GET /api/lifecycle/questions/{id}}.
 */
public record LifecycleResponse(
        String questionId,
        String estado,
        String eventId,
        Instant occurredAt,
        Instant processedAt) {

    public static LifecycleResponse from(QuestionLifecycle lifecycle) {
        return new LifecycleResponse(
                lifecycle.getQuestionId(),
                toApiStatus(lifecycle.status()),
                lifecycle.getEventId(),
                lifecycle.getOccurredAt(),
                lifecycle.getProcessedAt());
    }

    private static String toApiStatus(String status) {
        return switch (status) {
            case "PENDING_REVIEW" -> "PENDIENTE_REVISION";
            case "IN_REVIEW" -> "EN_REVISION";
            case "APPROVED" -> "APROBADA";
            case "REJECTED" -> "RECHAZADA";
            default -> status;
        };
    }
}
