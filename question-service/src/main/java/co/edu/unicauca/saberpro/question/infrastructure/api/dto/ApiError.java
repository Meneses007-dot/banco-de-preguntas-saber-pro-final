package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

/**
 * Uniform error body: {@code {"error":"...","mensaje":"..."}}.
 */
public record ApiError(String error, String mensaje) {
}
