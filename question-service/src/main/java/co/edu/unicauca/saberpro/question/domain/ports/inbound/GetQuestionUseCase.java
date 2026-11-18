package co.edu.unicauca.saberpro.question.domain.ports.inbound;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;

/**
 * Driving port of the read use case (HTTP {@code GET /api/questions/{id}}).
 *
 * <p>It exists so the REST adapter depends only on inbound ports instead of reaching the
 * repository directly.</p>
 */
public interface GetQuestionUseCase {

    /** Returns the aggregate or throws {@code QuestionNotFoundException}. */
    Question get(QuestionId id);
}
