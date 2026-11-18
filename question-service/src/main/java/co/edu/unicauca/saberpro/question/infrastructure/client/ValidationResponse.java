package co.edu.unicauca.saberpro.question.infrastructure.client;

/**
 * Response returned by {@code validation-service} ({@code POST /api/validation/questions}).
 */
public record ValidationResponse(boolean valid, java.util.List<String> errors) {
}
