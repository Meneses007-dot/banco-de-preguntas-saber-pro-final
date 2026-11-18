package co.edu.unicauca.saberpro.lifecycle.state;

/**
 * Initial state after the review event is consumed.
 */
public final class PendingReviewState implements LifecycleState {

    public static final PendingReviewState INSTANCE = new PendingReviewState();

    private PendingReviewState() {
    }

    @Override
    public String code() {
        return "PENDING_REVIEW";
    }

    @Override
    public LifecycleState startReview() {
        return InReviewState.INSTANCE;
    }

    @Override
    public LifecycleState approve() {
        throw new IllegalStateException("The question is not under review yet.");
    }

    @Override
    public LifecycleState reject() {
        throw new IllegalStateException("The question is not under review yet.");
    }
}
