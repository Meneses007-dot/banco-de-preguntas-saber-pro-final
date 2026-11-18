package co.edu.unicauca.saberpro.lifecycle.state;

import java.time.Instant;

/**
 * Context of the State pattern. It wraps the state of one item inside the lifecycle context and
 * delegates every transition to the current {@link LifecycleState}.
 */
public final class QuestionLifecycle {

    private final String questionId;
    private final String eventId;
    private final Instant occurredAt;
    private final Instant processedAt;
    private LifecycleState state;

    public QuestionLifecycle(String questionId, String eventId, Instant occurredAt, Instant processedAt) {
        this.questionId = questionId;
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.processedAt = processedAt;
        this.state = PendingReviewState.INSTANCE;
    }

    public synchronized void startReview() {
        state = state.startReview();
    }

    public synchronized void approve() {
        state = state.approve();
    }

    public synchronized void reject() {
        state = state.reject();
    }

    public synchronized String status() {
        return state.code();
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
