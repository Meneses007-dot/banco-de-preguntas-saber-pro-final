package co.edu.unicauca.saberpro.question.domain.model;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import java.util.Objects;

/**
 * Immutable value object that holds the statement (the concrete task) of a question.
 *
 * <p>Invariant INV-01: the text cannot be null, empty or made up only of whitespace.</p>
 */
public final class Statement {

    private final String value;

    private Statement(String value) {
        this.value = value;
    }

    /**
     * Factory method that validates INV-01 and normalizes the text with {@code trim()}.
     *
     * @param value raw statement written by the author
     * @return an immutable statement
     * @throws BusinessRuleException when INV-01 is violated
     */
    public static Statement of(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("The question statement cannot be null or empty.");
        }
        return new Statement(value.trim());
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Statement statement)) {
            return false;
        }
        return value.equals(statement.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
