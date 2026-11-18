package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

/**
 * Uniform error body: {@code {"error":"...","message":"..."}}.
 */
public record ApiError(String error, String message) {
}
