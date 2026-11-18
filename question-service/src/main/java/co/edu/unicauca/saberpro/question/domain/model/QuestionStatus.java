package co.edu.unicauca.saberpro.question.domain.model;

/**
 * Life-cycle states covered by the domain of the item.
 *
 * <p>Only two states exist in this delivery:</p>
 * <pre>
 * [DRAFT] --submitForReview()--> [PENDING_REVIEW]
 * </pre>
 */
public enum QuestionStatus {
    DRAFT,
    PENDING_REVIEW
}
