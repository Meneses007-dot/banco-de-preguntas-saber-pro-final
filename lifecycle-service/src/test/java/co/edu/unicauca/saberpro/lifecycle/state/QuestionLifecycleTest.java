package co.edu.unicauca.saberpro.lifecycle.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("QuestionLifecycle - State pattern in the lifecycle context")
class QuestionLifecycleTest {

    private QuestionLifecycle lifecycle() {
        return new QuestionLifecycle("question-1", "event-1", Instant.parse("2026-11-13T15:04:05Z"),
                Instant.now());
    }

    @Test
    @DisplayName("The review event leaves the question in PENDING_REVIEW")
    void startsPendingReview() {
        assertEquals("PENDING_REVIEW", lifecycle().status());
    }

    @Test
    @DisplayName("PENDING_REVIEW -> IN_REVIEW -> APPROVED")
    void followsApprovalPath() {
        QuestionLifecycle lifecycle = lifecycle();

        lifecycle.startReview();
        assertEquals("IN_REVIEW", lifecycle.status());

        lifecycle.approve();
        assertEquals("APPROVED", lifecycle.status());
    }

    @Test
    @DisplayName("PENDING_REVIEW -> IN_REVIEW -> REJECTED")
    void followsRejectionPath() {
        QuestionLifecycle lifecycle = lifecycle();

        lifecycle.startReview();
        lifecycle.reject();

        assertEquals("REJECTED", lifecycle.status());
    }

    @Test
    @DisplayName("A question that has not entered review cannot be approved")
    void rejectsInvalidTransition() {
        assertThrows(IllegalStateException.class, () -> lifecycle().approve());
    }

    @Test
    @DisplayName("Terminal states do not allow new transitions")
    void terminalStatesAreFinal() {
        QuestionLifecycle approved = lifecycle();
        approved.startReview();
        approved.approve();

        assertThrows(IllegalStateException.class, approved::startReview);
        assertThrows(IllegalStateException.class, approved::reject);
    }
}
