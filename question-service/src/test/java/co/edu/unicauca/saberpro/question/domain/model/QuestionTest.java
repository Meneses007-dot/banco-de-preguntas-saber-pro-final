package co.edu.unicauca.saberpro.question.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Question aggregate - INV-03/04/06/07/08")
class QuestionTest {

    private Statement statement;
    private Justification justification;

    @BeforeEach
    void setUp() {
        statement = Statement.of("What is the author's conclusion in the text?");
        justification = Justification.of("The key is supported by the central thesis of the third paragraph.");
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("T-09: a new question starts in DRAFT (INV-06)")
        void startsInDraft() {
            Question question = Question.create(statement, justification, options(4, 1));
            assertEquals(QuestionStatus.DRAFT, question.getStatus());
            assertNotNull(question.getId());
        }

        @Test
        @DisplayName("T-09: a draft may exist with three options (INV-03 is a transition invariant)")
        void draftMayBeIncomplete() {
            Question question = Question.create(statement, justification, options(3, 1));
            assertEquals(3, question.getOptions().size());
            assertEquals(QuestionStatus.DRAFT, question.getStatus());
        }
    }

    @Nested
    @DisplayName("Transition to PENDING_REVIEW")
    class Transition {

        @Test
        @DisplayName("T-10: three options block the transition and keep DRAFT")
        void rejectsThreeOptions() {
            Question question = Question.create(statement, justification, options(3, 1));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    question::submitForReview);
            assertEquals("The question must have exactly 4 answer options.",
                    exception.getMessage());
            assertEquals(QuestionStatus.DRAFT, question.getStatus());
        }

        @Test
        @DisplayName("T-11: five options block the transition and keep DRAFT")
        void rejectsFiveOptions() {
            Question question = Question.create(statement, justification, options(5, 1));

            assertThrows(BusinessRuleException.class, question::submitForReview);
            assertEquals(QuestionStatus.DRAFT, question.getStatus());
        }

        @Test
        @DisplayName("T-12: zero correct keys block the transition (INV-04)")
        void rejectsZeroKeys() {
            Question question = Question.create(statement, justification, options(4, 0));

            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    question::submitForReview);
            assertEquals("The question must have exactly one correct key.", exception.getMessage());
        }

        @Test
        @DisplayName("T-13: two correct keys block the transition (INV-04)")
        void rejectsTwoKeys() {
            Question question = Question.create(statement, justification, options(4, 2));

            assertThrows(BusinessRuleException.class, question::submitForReview);
            assertEquals(QuestionStatus.DRAFT, question.getStatus());
        }

        @Test
        @DisplayName("T-14: four options and one key lead to PENDING_REVIEW")
        void transitionsWithValidOptions() {
            Question question = Question.create(statement, justification, options(4, 1));

            question.submitForReview();

            assertEquals(QuestionStatus.PENDING_REVIEW, question.getStatus());
        }

        @Test
        @DisplayName("T-17: a question already submitted cannot be submitted again (INV-07)")
        void rejectsDoubleSubmission() {
            Question question = Question.create(statement, justification, options(4, 1));
            question.submitForReview();

            BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                    question::submitForReview);
            assertEquals("The question is already under review.", exception.getMessage());
            assertEquals(QuestionStatus.PENDING_REVIEW, question.getStatus());
        }
    }

    @Nested
    @DisplayName("Encapsulation and identity")
    class Encapsulation {

        @Test
        @DisplayName("T-15: the options list cannot be modified from outside (INV-08)")
        void optionsAreImmutable() {
            Question question = Question.create(statement, justification, options(4, 1));

            assertThrows(UnsupportedOperationException.class,
                    () -> question.getOptions().add(AnswerOption.of("Intruder", false)));
        }

        @Test
        @DisplayName("T-16: Question equality is by identity, not by content")
        void equalityByIdentity() {
            Question first = Question.create(statement, justification, options(4, 1));
            Question second = Question.rehydrate(first.getId(), statement, justification,
                    options(4, 1), QuestionStatus.DRAFT);
            Question third = Question.create(statement, justification, options(4, 1));

            assertEquals(first, second);
            assertNotEquals(first, third);
            assertEquals(first.hashCode(), second.hashCode());
        }

        @Test
        @DisplayName("GoF Builder: rebuilds the aggregate preserving identity and status")
        void builderRebuildsAggregate() {
            QuestionId id = QuestionId.generate();
            Question question = Question.builder()
                    .id(id)
                    .statement(statement)
                    .justification(justification)
                    .options(options(4, 1))
                    .status(QuestionStatus.PENDING_REVIEW)
                    .build();

            assertEquals(id, question.getId());
            assertEquals(QuestionStatus.PENDING_REVIEW, question.getStatus());
            assertEquals(4, question.getOptions().size());
        }

        @Test
        @DisplayName("QuestionId: generated in the domain and rebuilt from text")
        void questionIdFromText() {
            QuestionId generated = QuestionId.generate();
            assertEquals(generated, QuestionId.of(generated.toString()));
            assertTrue(QuestionId.of(generated.toString()).value().version() > 0);
        }
    }

    private List<AnswerOption> options(int count, int correctKeys) {
        List<AnswerOption> result = new java.util.ArrayList<>();
        for (int index = 1; index <= count; index++) {
            result.add(AnswerOption.of("Option " + index, index <= correctKeys));
        }
        return List.copyOf(result);
    }
}
