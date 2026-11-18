package co.edu.unicauca.saberpro.lifecycle.state;

/**
 * Terminal state: the item was rejected.
 */
public final class RejectedState implements LifecycleState {

    public static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {
    }

    @Override
    public String code() {
        return "REJECTED";
    }

    @Override
    public LifecycleState startReview() {
        throw new IllegalStateException("A rejected question cannot return to review.");
    }

    @Override
    public LifecycleState approve() {
        throw new IllegalStateException("A rejected question cannot be approved.");
    }

    @Override
    public LifecycleState reject() {
        throw new IllegalStateException("A rejected question cannot be rejected again.");
    }
}
