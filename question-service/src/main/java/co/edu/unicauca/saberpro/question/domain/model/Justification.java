package co.edu.unicauca.saberpro.question.domain.model;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import java.util.Objects;

/**
 * Immutable value object that holds the technical justification of the correct answer.
 *
 * <p>Invariant INV-02: it requires at least 15 characters counted on the normalized text.</p>
 */
public final class Justification {

    private static final int MIN_LENGTH = 15;

    private final String text;

    private Justification(String text) {
        this.text = text;
    }

    /**
     * Factory method that validates INV-02 on the normalized (trimmed) text.
     *
     * @param text justification written by the author
     * @return an immutable justification
     * @throws BusinessRuleException when INV-02 is violated
     */
    public static Justification of(String text) {
        if (text == null) {
            throw new BusinessRuleException(
                    "The justification must contain at least 15 characters of technical support.");
        }
        String normalized = text.trim();
        if (normalized.length() < MIN_LENGTH) {
            throw new BusinessRuleException(
                    "The justification must contain at least 15 characters of technical support.");
        }
        return new Justification(normalized);
    }

    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Justification justification)) {
            return false;
        }
        return text.equals(justification.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return text;
    }
}
