package co.edu.unicauca.saberpro.lifecycle.messaging;

import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycle;
import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycleRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Asynchronous consumer of {@code QuestionReviewRequested}.
 *
 * <p>It is idempotent by {@code eventId}: a redelivered event is logged and discarded. The
 * listener retries three times with backoff (1s/2s/4s, see {@code application.yml}) and, if it
 * keeps failing, the message is routed to the dead-letter queue declared by the producer.</p>
 */
@Component
public class QuestionReviewListener {

    private static final Logger log = LoggerFactory.getLogger(QuestionReviewListener.class);

    private final QuestionLifecycleRegistry registry;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public QuestionReviewListener(QuestionLifecycleRegistry registry) {
        this.registry = registry;
    }

    @RabbitListener(queues = RabbitMQListenerConfig.LIFECYCLE_QUEUE)
    public synchronized void onQuestionReviewRequested(QuestionReviewEvent event) {
        if (event == null || isBlank(event.eventId()) || isBlank(event.questionId())) {
            log.warn("Review event ignored due to incomplete contract: {}", event);
            return;
        }
        if (processedEventIds.contains(event.eventId())) {
            log.info("Event {} already processed; duplicate discarded (idempotency)",
                    event.eventId());
            return;
        }

        QuestionLifecycle lifecycle = registry.register(
                event.questionId(), event.eventId(), event.occurredAt());
        processedEventIds.add(event.eventId());

        log.info("Event {} processed: question {} registered in the lifecycle context "
                        + "(status={}, options={}, occurredAt={})",
                event.eventId(), event.questionId(), event.status(),
                event.optionCount(), event.occurredAt());
        log.debug("Local state of question {}: {}", lifecycle.getQuestionId(), lifecycle.status());
    }

    /** Visible for tests: tells whether an event id was already handled. */
    public boolean wasProcessed(String eventId) {
        return processedEventIds.contains(eventId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
