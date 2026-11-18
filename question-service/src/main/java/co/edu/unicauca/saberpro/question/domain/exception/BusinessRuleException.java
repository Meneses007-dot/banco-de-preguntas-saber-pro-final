package co.edu.unicauca.saberpro.question.domain.exception;

/**
 * Unchecked exception raised whenever a business rule (INV-01 ... INV-08) is violated.
 *
 * <p>It is deliberately a JDK type: the domain does not know that an HTTP status code exists.
 * The infrastructure layer maps it to {@code 400 Bad Request}.</p>
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
