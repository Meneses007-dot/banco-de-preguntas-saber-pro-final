package co.edu.unicauca.saberpro.lifecycle.state;

/**
 * State while a reviewer is working on the item.
 */
public final class InReviewState implements LifecycleState {

    public static final InReviewState INSTANCE = new InReviewState();

    private InReviewState() {
    }

    @Override
    public String code() {
        return "IN_REVIEW";
    }

    @Override
    public LifecycleState startReview() {
        return this;
    }

    @Override
    public LifecycleState approve() {
        return ApprovedState.INSTANCE;
    }

    @Override
    public LifecycleState reject() {
        return RejectedState.INSTANCE;
    }
}
