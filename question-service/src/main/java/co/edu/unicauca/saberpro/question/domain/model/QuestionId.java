package co.edu.unicauca.saberpro.question.domain.model;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object that identifies the question.
 *
 * <p>The identity is always generated in the domain ({@link #generate()}), never by the database,
 * and its equality is by value.</p>
 */
public final class QuestionId {

    private final UUID value;

    private QuestionId(UUID value) {
        this.value = value;
    }

    /** Generates a brand new identity for a freshly created item. */
    public static QuestionId generate() {
        return new QuestionId(UUID.randomUUID());
    }

    /** Rebuilds an identity from an existing {@link UUID}. */
    public static QuestionId of(UUID value) {
        if (value == null) {
            throw new BusinessRuleException("The question identifier is required.");
        }
        return new QuestionId(value);
    }

    /** Rebuilds an identity from its textual representation. */
    public static QuestionId of(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException("The question identifier is required.");
        }
        try {
            return new QuestionId(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessRuleException("Invalid question identifier: " + value);
        }
    }

    public UUID value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QuestionId questionId)) {
            return false;
        }
        return value.equals(questionId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
