package co.edu.unicauca.saberpro.question.domain.ports.inbound;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;

/**
 * Driving port of the authoring use case (HTTP {@code POST /api/questions}).
 */
public interface CreateQuestionUseCase {

    /** Creates an item in {@code DRAFT} status after the structural validation. */
    Question create(CreateQuestionCommand command);
}
