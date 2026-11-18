package co.edu.unicauca.saberpro.question.domain.ports.outbound;

import co.edu.unicauca.saberpro.question.domain.model.Question;

/**
 * Driven notification port. Its only implementation is {@code RabbitMQPublisher}: the domain
 * never learns that AMQP exists.
 */
public interface NotificationPort {

    /** Notifies that an item has moved to peer review. */
    void notifyReviewRequested(Question question);
}
