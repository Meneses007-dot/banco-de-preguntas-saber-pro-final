package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

/**
 * Answer option inside the creation payload.
 */
public record OptionRequest(String text, boolean correct) {
}
