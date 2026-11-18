package co.edu.unicauca.saberpro.question.domain.model;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import java.util.Objects;

/**
 * Immutable value object that represents one answer option of the item: its text plus the flag
 * that tells whether it is the correct key.
 *
 * <p>Invariant INV-05: the text cannot be null, empty or blank.</p>
 */
public final class AnswerOption {

    private final String text;
    private final boolean correct;

    private AnswerOption(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    /**
     * Factory method that validates INV-05 and trims the outer whitespace of the text.
     *
     * @param text    option text
     * @param correct {@code true} only for the single key of the item
     * @return an immutable answer option
     * @throws BusinessRuleException when INV-05 is violated
     */
    public static AnswerOption of(String text, boolean correct) {
        if (text == null || text.isBlank()) {
            throw new BusinessRuleException("The text of the answer option cannot be empty.");
        }
        return new AnswerOption(text.trim(), correct);
    }

    public String text() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnswerOption option)) {
            return false;
        }
        return correct == option.correct && text.equals(option.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, correct);
    }

    @Override
    public String toString() {
        return (correct ? "[key] " : "[distractor] ") + text;
    }
}
