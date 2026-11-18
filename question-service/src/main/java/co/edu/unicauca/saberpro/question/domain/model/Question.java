package co.edu.unicauca.saberpro.question.domain.model;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregate root of the Saber PRO item.
 *
 * <p>It encapsulates the statement, the justification, the answer options and the status.
 * It is the only gateway to the business rules: no external service changes its status or
 * validates its invariants. Its equality is by identity.</p>
 *
 * <p>Invariants handled here:</p>
 * <ul>
 *   <li>INV-03: exactly 4 answer options before {@code PENDING_REVIEW}.</li>
 *   <li>INV-04: exactly 1 correct key before {@code PENDING_REVIEW}.</li>
 *   <li>INV-06: a new item always starts in {@code DRAFT}.</li>
 *   <li>INV-07: an item already under review cannot be sent again.</li>
 *   <li>INV-08: the options list is read-only from the outside.</li>
 * </ul>
 */
public class Question {

    private static final int REQUIRED_OPTIONS = 4;

    private final QuestionId id;
    private final Statement statement;
    private final Justification justification;
    private final List<AnswerOption> options;
    private QuestionStatus status;

    private Question(QuestionId id, Statement statement, Justification justification,
                     List<AnswerOption> options, QuestionStatus status) {
        if (id == null) {
            throw new BusinessRuleException("The question identifier is required.");
        }
        if (statement == null) {
            throw new BusinessRuleException("The question statement cannot be null or empty.");
        }
        if (justification == null) {
            throw new BusinessRuleException("The question justification is required.");
        }
        if (status == null) {
            throw new BusinessRuleException("The question status is required.");
        }
        this.id = id;
        this.statement = statement;
        this.justification = justification;
        this.options = immutableCopy(options);
        this.status = status;
    }

    /**
     * Creates an item in {@code DRAFT} status (INV-06).
     *
     * <p>INV-03 and INV-04 are transition invariants: an incomplete draft may exist, but it can
     * never reach {@code PENDING_REVIEW}.</p>
     */
    public static Question create(Statement statement, Justification justification,
                                 List<AnswerOption> options) {
        return new Question(QuestionId.generate(), statement, justification, options,
                QuestionStatus.DRAFT);
    }

    /**
     * Rebuilds the aggregate from a persistence source without re-triggering creation rules.
     * It is used by the outbound adapters and by the contract tests.
     */
    public static Question rehydrate(QuestionId id, Statement statement, Justification justification,
                                      List<AnswerOption> options, QuestionStatus status) {
        return new Question(id, statement, justification, options, status);
    }

    /** GoF Builder kept for traceability with Term II. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Transitions {@code DRAFT -> PENDING_REVIEW} only when the item is publishable:
     * exactly four options (INV-03) and exactly one correct key (INV-04).
     *
     * @throws BusinessRuleException when INV-03, INV-04 or INV-07 is violated
     */
    public void submitForReview() {
        if (status == QuestionStatus.PENDING_REVIEW) {
            throw new BusinessRuleException("The question is already under review.");
        }
        validatePublishableOptions();
        this.status = QuestionStatus.PENDING_REVIEW;
    }

    private void validatePublishableOptions() {
        if (options.size() != REQUIRED_OPTIONS) {
            throw new BusinessRuleException("The question must have exactly 4 answer options.");
        }
        long keys = options.stream().filter(AnswerOption::isCorrect).count();
        if (keys != 1) {
            throw new BusinessRuleException("The question must have exactly one correct key.");
        }
    }

    private static List<AnswerOption> immutableCopy(List<AnswerOption> options) {
        if (options == null) {
            throw new BusinessRuleException("The answer options list is required.");
        }
        if (options.stream().anyMatch(Objects::isNull)) {
            throw new BusinessRuleException("The answer options list cannot contain nulls.");
        }
        // List.copyOf returns an unmodifiable list: INV-08.
        return List.copyOf(options);
    }

    public QuestionId getId() {
        return id;
    }

    public Statement getStatement() {
        return statement;
    }

    public Justification getJustification() {
        return justification;
    }

    /** Read-only view of the options; any attempt to mutate it throws (INV-08). */
    public List<AnswerOption> getOptions() {
        return options;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Question question)) {
            return false;
        }
        return id.equals(question.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Question{" + id + ", status=" + status + ", options="
                + options.stream().map(AnswerOption::toString).collect(Collectors.joining(" | ")) + '}';
    }

    /**
     * Fluent builder for the aggregate (GoF Builder, Term II traceability).
     * When no id is supplied a new one is generated by the domain.
     */
    public static final class Builder {

        private QuestionId id;
        private Statement statement;
        private Justification justification;
        private List<AnswerOption> options;
        private QuestionStatus status;

        private Builder() {
        }

        public Builder id(QuestionId id) {
            this.id = id;
            return this;
        }

        public Builder statement(Statement statement) {
            this.statement = statement;
            return this;
        }

        public Builder justification(Justification justification) {
            this.justification = justification;
            return this;
        }

        public Builder options(List<AnswerOption> options) {
            this.options = options;
            return this;
        }

        public Builder status(QuestionStatus status) {
            this.status = status;
            return this;
        }

        public Question build() {
            return new Question(id == null ? QuestionId.generate() : id, statement, justification,
                    options, status == null ? QuestionStatus.DRAFT : status);
        }
    }
}
