package co.edu.unicauca.saberpro.question.infrastructure.client;

/**
 * Raised when {@code validation-service} is unreachable, times out or answers with an unexpected
 * payload. The global exception handler maps it to {@code 503 Service Unavailable}.
 */
public class ValidationServiceUnavailableException extends RuntimeException {

    public ValidationServiceUnavailableException(String message) {
        super(message);
    }

    public ValidationServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
