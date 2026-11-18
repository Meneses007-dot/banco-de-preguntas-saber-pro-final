package co.edu.unicauca.saberpro.question.domain.exception;

/**
 * Raised when the requested aggregate does not exist. The infrastructure layer maps it to
 * {@code 404 Not Found}; it does not extend {@link BusinessRuleException} on purpose, because a
 * missing item is not a violated business rule but a missing resource.
 */
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(String message) {
        super(message);
    }
}
