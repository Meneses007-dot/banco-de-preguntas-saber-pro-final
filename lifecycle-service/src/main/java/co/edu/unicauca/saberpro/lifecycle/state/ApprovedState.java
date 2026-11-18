package co.edu.unicauca.saberpro.lifecycle.state;

/**
 * Terminal state: the item was approved.
 */
public final class ApprovedState implements LifecycleState {

    public static final ApprovedState INSTANCE = new ApprovedState();

    private ApprovedState() {
    }

    @Override
    public String code() {
        return "APPROVED";
    }

    @Override
    public LifecycleState startReview() {
        throw new IllegalStateException("An approved question cannot return to review.");
    }

    @Override
    public LifecycleState approve() {
        throw new IllegalStateException("An approved question cannot be approved again.");
    }

    @Override
    public LifecycleState reject() {
        throw new IllegalStateException("An approved question cannot be rejected.");
    }
}
