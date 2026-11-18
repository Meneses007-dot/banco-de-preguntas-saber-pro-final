package co.edu.unicauca.saberpro.lifecycle.state;

/**
 * State pattern contract for the life cycle of an item inside the <em>lifecycle</em> bounded
 * context (Term II traceability).
 *
 * <p>The domain aggregate {@code Question} keeps only {@code DRAFT} and
 * {@code PENDING_REVIEW}; the post-review states belong to this context (decision A18).</p>
 */
public interface LifecycleState {

    String code();

    LifecycleState startReview();

    LifecycleState approve();

    LifecycleState reject();
}
