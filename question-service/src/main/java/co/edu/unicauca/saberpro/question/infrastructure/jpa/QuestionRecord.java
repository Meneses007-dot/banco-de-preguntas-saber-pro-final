package co.edu.unicauca.saberpro.question.infrastructure.jpa;

import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Relational representation of the aggregate. It is the only place where JPA is allowed, and it
 * is never exposed outside the {@code infrastructure.jpa} package.
 */
@Entity
@Table(name = "questions")
public class QuestionRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "statement", nullable = false, length = 2000)
    private String statement;

    @Column(name = "justification", nullable = false, length = 2000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private QuestionStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    private List<AnswerOptionRecord> options = new ArrayList<>();

    /** Required by JPA. */
    protected QuestionRecord() {
    }

    public QuestionRecord(UUID id, String statement, String justification, QuestionStatus status,
                          List<AnswerOptionRecord> options) {
        this.id = id;
        this.statement = statement;
        this.justification = justification;
        this.status = status;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
    }

    public UUID getId() {
        return id;
    }

    public String getStatement() {
        return statement;
    }

    public String getJustification() {
        return justification;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public List<AnswerOptionRecord> getOptions() {
        return options;
    }
}
