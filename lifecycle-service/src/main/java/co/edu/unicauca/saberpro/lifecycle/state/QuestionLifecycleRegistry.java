package co.edu.unicauca.saberpro.lifecycle.state;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory registry owned by the lifecycle bounded context (decision A18). It never reads the
 * database of {@code question-service}.
 */
@Component
public class QuestionLifecycleRegistry {

    private final Map<String, QuestionLifecycle> lifecycles = new ConcurrentHashMap<>();

    /**
     * Registers the item the first time its review event is processed. Repeated events for the
     * same question keep the existing entry, which makes the registration idempotent.
     */
    public QuestionLifecycle register(String questionId, String eventId, Instant occurredAt) {
        Instant processedAt = Instant.now();
        return lifecycles.computeIfAbsent(questionId,
                id -> new QuestionLifecycle(id, eventId, occurredAt, processedAt));
    }

    public Optional<QuestionLifecycle> findByQuestionId(String questionId) {
        return Optional.ofNullable(lifecycles.get(questionId));
    }

    public int size() {
        return lifecycles.size();
    }

    public void clear() {
        lifecycles.clear();
    }
}
