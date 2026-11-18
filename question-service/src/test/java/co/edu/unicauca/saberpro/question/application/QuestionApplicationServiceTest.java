package co.edu.unicauca.saberpro.question.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.question.domain.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.OptionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.NotificationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.ValidationResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("QuestionApplicationService - orchestration without business rules")
class QuestionApplicationServiceTest {

    private static final String VALID_STATEMENT = "What is the author's conclusion in the text?";
    private static final String VALID_JUSTIFICATION =
            "The key is supported by the central thesis of the third paragraph.";

    private RecordingRepository repository;
    private RecordingNotification notification;
    private StubValidation validation;
    private QuestionApplicationService service;

    @BeforeEach
    void setUp() {
        repository = new RecordingRepository();
        notification = new RecordingNotification();
        validation = new StubValidation();
        service = new QuestionApplicationService(repository, notification, validation);
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Delegates to the validation port, stores the aggregate in DRAFT and does not notify")
        void createsAndPersists() {
            Question created = service.create(command(4, 1));

            assertEquals(1, validation.calls);
            assertEquals(1, repository.saveCalls);
            assertEquals(DRAFT_STATUS, created.getStatus().name());
            assertTrue(notification.notified.isEmpty());
        }

        @Test
        @DisplayName("When validation-service rejects, it propagates BusinessRuleException without saving")
        void propagatesStructuralValidationError() {
            validation.next = ValidationResult.invalid(
                    List.of("The question must have exactly 4 answer options."));

            BusinessRuleException exception =
                    assertThrows(BusinessRuleException.class, () -> service.create(command(4, 1)));

            assertTrue(exception.getMessage().contains("exactly 4"));
            assertEquals(0, repository.saveCalls);
        }

        @Test
        @DisplayName("Even when structural validation passes, the VOs revalidate INV-01/INV-02")
        void domainStillRevalidatesText() {
            CreateQuestionCommand blankStatement =
                    new CreateQuestionCommand("   ", VALID_JUSTIFICATION, options(4, 1));

            assertThrows(BusinessRuleException.class, () -> service.create(blankStatement));
            assertEquals(0, repository.saveCalls);
        }
    }

    @Nested
    @DisplayName("Evaluation")
    class Evaluation {

        @Test
        @DisplayName("Transitions, persists and notifies the review event")
        void evaluatesAndNotifies() {
            Question created = service.create(command(4, 1));

            Question evaluated = service.evaluate(created.getId());

            assertEquals("PENDING_REVIEW", evaluated.getStatus().name());
            assertEquals(2, repository.saveCalls);
            assertEquals(1, notification.notified.size());
            assertEquals(created.getId(), notification.notified.get(0).getId());
        }

        @Test
        @DisplayName("An unknown identifier raises QuestionNotFoundException")
        void failsWhenQuestionDoesNotExist() {
            assertThrows(QuestionNotFoundException.class,
                    () -> service.evaluate(QuestionId.generate()));
            assertEquals(0, notification.notified.size());
        }

        @Test
        @DisplayName("Transition invariants are propagated and no notification is sent")
        void propagatesTransitionInvariants() {
            Question incomplete = service.create(command(3, 1));

            assertThrows(BusinessRuleException.class, () -> service.evaluate(incomplete.getId()));
            assertTrue(notification.notified.isEmpty());
            assertEquals(DRAFT_STATUS, repository.findById(incomplete.getId()).orElseThrow()
                    .getStatus().name());
        }
    }

    @Nested
    @DisplayName("Query")
    class Query {

        @Test
        @DisplayName("Returns the existing aggregate and fails with QuestionNotFoundException otherwise")
        void queriesById() {
            Question created = service.create(command(4, 1));

            assertEquals(created.getId(), service.get(created.getId()).getId());
            assertThrows(QuestionNotFoundException.class,
                    () -> service.get(QuestionId.generate()));
        }
    }

    private static final String DRAFT_STATUS = "DRAFT";

    private CreateQuestionCommand command(int optionCount, int correctKeys) {
        return new CreateQuestionCommand(VALID_STATEMENT, VALID_JUSTIFICATION,
                options(optionCount, correctKeys));
    }

    private List<OptionCommand> options(int count, int correctKeys) {
        List<OptionCommand> options = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            options.add(new OptionCommand("Option " + index, index <= correctKeys));
        }
        return List.copyOf(options);
    }

    private static final class RecordingRepository implements QuestionRepositoryPort {

        private final Map<QuestionId, Question> store = new HashMap<>();
        private int saveCalls;

        @Override
        public Question save(Question question) {
            saveCalls++;
            store.put(question.getId(), question);
            return question;
        }

        @Override
        public Optional<Question> findById(QuestionId id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    private static final class RecordingNotification implements NotificationPort {

        private final List<Question> notified = new ArrayList<>();

        @Override
        public void notifyReviewRequested(Question question) {
            notified.add(question);
        }
    }

    private static final class StubValidation implements QuestionValidationPort {

        private ValidationResult next = ValidationResult.success();
        private int calls;

        @Override
        public ValidationResult validate(CreateQuestionCommand command) {
            calls++;
            return next;
        }
    }
}
