package co.edu.unicauca.saberpro.lifecycle.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycleRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuestionReviewListener - async consumption and idempotency")
class QuestionReviewListenerTest {

    private QuestionLifecycleRegistry registry;
    private QuestionReviewListener listener;

    @BeforeEach
    void setUp() {
        registry = new QuestionLifecycleRegistry();
        listener = new QuestionReviewListener(registry);
    }

    @Test
    @DisplayName("E2E-07/08: the event registers the question in the lifecycle context")
    void registersQuestionOnEvent() {
        listener.onQuestionReviewRequested(event("event-1", "question-1"));

        assertTrue(registry.findByQuestionId("question-1").isPresent());
        assertEquals("PENDING_REVIEW",
                registry.findByQuestionId("question-1").orElseThrow().status());
        assertTrue(listener.wasProcessed("event-1"));
    }

    @Test
    @DisplayName("E2E-10: redelivery of the same eventId is processed only once")
    void ignoresDuplicatedEvent() {
        QuestionReviewEvent event = event("event-1", "question-1");

        listener.onQuestionReviewRequested(event);
        listener.onQuestionReviewRequested(event);
        listener.onQuestionReviewRequested(event);

        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("Two different events for the same question do not duplicate the record")
    void keepsSingleLifecyclePerQuestion() {
        listener.onQuestionReviewRequested(event("event-1", "question-1"));
        listener.onQuestionReviewRequested(event("event-2", "question-1"));

        assertEquals(1, registry.size());
        assertEquals("event-1",
                registry.findByQuestionId("question-1").orElseThrow().getEventId());
    }

    @Test
    @DisplayName("An incomplete event is discarded without side effects")
    void ignoresIncompleteEvent() {
        listener.onQuestionReviewRequested(new QuestionReviewEvent(
                null, "QuestionReviewRequested", 1, Instant.now(), "question-1", "PENDING_REVIEW", 4));

        assertEquals(0, registry.size());
    }

    private QuestionReviewEvent event(String eventId, String questionId) {
        return new QuestionReviewEvent(eventId, "QuestionReviewRequested", 1,
                Instant.parse("2026-11-13T15:04:05Z"), questionId, "PENDING_REVIEW", 4);
    }
}
