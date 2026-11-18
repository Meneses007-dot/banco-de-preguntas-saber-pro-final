package co.edu.unicauca.saberpro.question.infrastructure.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable representation of one answer option. It lives inside the {@code question_options}
 * table, ordered by the {@code position} column.
 */
@Embeddable
public class AnswerOptionRecord {

    @Column(name = "text", nullable = false, length = 500)
    private String text;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    /** Required by JPA. */
    protected AnswerOptionRecord() {
    }

    public AnswerOptionRecord(String text, boolean correct) {
        this.text = text;
        this.correct = correct;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }
}
