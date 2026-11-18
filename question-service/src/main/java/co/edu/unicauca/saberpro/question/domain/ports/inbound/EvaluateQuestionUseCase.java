package co.edu.unicauca.saberpro.question.domain.ports.inbound;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;

/**
 * Driving port of the evaluation use case (HTTP {@code POST /api/questions/{id}/evaluate}).
 */
public interface EvaluateQuestionUseCase {

    /**
     * Validates the transition invariants, moves the item to {@code PENDING_REVIEW},
     * persists it and notifies through the outbound port.
     */
    Question evaluate(QuestionId id);
}
