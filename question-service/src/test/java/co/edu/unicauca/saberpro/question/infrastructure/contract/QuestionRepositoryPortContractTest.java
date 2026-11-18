package co.edu.unicauca.saberpro.question.infrastructure.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Statement;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import co.edu.unicauca.saberpro.question.domain.model.Justification;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Black-box contract of {@link QuestionRepositoryPort}. The same suite must pass with the in-memory
 * adapter and with the JPA adapter, which is the evidence of interchangeability (Step 3).
 */
public abstract class QuestionRepositoryPortContractTest {

    protected abstract QuestionRepositoryPort repository();

    @Test
    @DisplayName("save -> findById preserves identity, texts and status")
    void savesAndRetrievesById() {
        Question question = draft();

        Question saved = repository().save(question);
        Question found = repository().findById(question.getId()).orElseThrow();

        assertEquals(question.getId(), saved.getId());
        assertEquals(question.getId(), found.getId());
        assertEquals(QuestionStatus.DRAFT, found.getStatus());
        assertEquals(question.getStatement(), found.getStatement());
        assertEquals(question.getJustification(), found.getJustification());
    }

    @Test
    @DisplayName("findById returns Optional.empty for an unknown identifier")
    void returnsEmptyForUnknownId() {
        assertTrue(repository().findById(QuestionId.generate()).isEmpty());
    }

    @Test
    @DisplayName("save updates the status and preserves identity")
    void updatesStatusPreservingIdentity() {
        Question question = draft();
        repository().save(question);

        question.submitForReview();
        repository().save(question);

        Question updated = repository().findById(question.getId()).orElseThrow();
        assertEquals(QuestionStatus.PENDING_REVIEW, updated.getStatus());
        assertEquals(question.getId(), updated.getId());
    }

    @Test
    @DisplayName("Preserves the four options, their order and the correct key")
    void preservesOptionsAndKey() {
        Question question = draft();
        repository().save(question);

        List<AnswerOption> options = repository().findById(question.getId()).orElseThrow().getOptions();
        assertEquals(4, options.size());
        assertEquals("Option 1", options.get(0).text());
        assertEquals("Option 4", options.get(3).text());
        assertEquals(1, options.stream().filter(AnswerOption::isCorrect).count());
        assertTrue(options.get(0).isCorrect());
    }

    @Test
    @DisplayName("Two consecutive saves do not duplicate the question")
    void savingTwiceKeepsSingleAggregate() {
        Question question = draft();
        repository().save(question);
        repository().save(question);

        assertEquals(question.getId(),
                repository().findById(question.getId()).orElseThrow().getId());
    }

    private static Question draft() {
        return Question.create(
                Statement.of("What is the author's conclusion in the text?"),
                Justification.of("The key is supported by the central thesis of the third paragraph."),
                List.of(
                        AnswerOption.of("Option 1", true),
                        AnswerOption.of("Option 2", false),
                        AnswerOption.of("Option 3", false),
                        AnswerOption.of("Option 4", false)));
    }
}
