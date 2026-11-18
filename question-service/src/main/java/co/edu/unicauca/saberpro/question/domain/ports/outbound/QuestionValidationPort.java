package co.edu.unicauca.saberpro.question.domain.ports.outbound;

import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;

/**
 * Driven port for the synchronous structural validation delegated to {@code validation-service}
 * (decision A11: the REST client lives in infrastructure and the domain keeps a pure interface).
 */
public interface QuestionValidationPort {

    /** Validates the shape of the item before the aggregate is built. */
    ValidationResult validate(CreateQuestionCommand command);
}
