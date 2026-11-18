package co.edu.unicauca.saberpro.question.domain.ports.outbound;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import java.util.Optional;

/**
 * Driven persistence port. It speaks the domain language and exposes no Spring Data, JPA or SQL
 * type, which is what makes the two adapters interchangeable.
 */
public interface QuestionRepositoryPort {

    /** Persists or updates the aggregate and returns the stored state. */
    Question save(Question question);

    /** Looks a question up by identity; it never returns {@code null}. */
    Optional<Question> findById(QuestionId id);
}
