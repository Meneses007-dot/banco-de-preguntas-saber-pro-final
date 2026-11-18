package co.edu.unicauca.saberpro.question.domain.ports.outbound;

import java.util.List;

/**
 * Pure result of the structural validation performed by {@code validation-service}.
 * It is a JDK-only record so the domain does not depend on Jackson or on the client DTOs.
 */
public record ValidationResult(boolean valid, List<String> errors) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
