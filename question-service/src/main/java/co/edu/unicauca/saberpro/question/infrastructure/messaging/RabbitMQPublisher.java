package co.edu.unicauca.saberpro.question.infrastructure.messaging;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.NotificationPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * AMQP implementation of the notification port.
 *
 * <p>It is the proof that the domain ignores the existence of RabbitMQ: the aggregate only knows
 * {@link NotificationPort}. The event is written as plain JSON (no Java type header) so the
 * consumer can deserialize its own duplicated DTO.</p>
 */
public class RabbitMQPublisher implements NotificationPort {

    public static final String EXCHANGE = "saberpro.questions.exchange";
    public static final String ROUTING_KEY = "question.review.requested";
    public static final String LIFECYCLE_QUEUE = "saberpro.lifecycle.question-review";
    public static final String EVENT_TYPE = "QuestionReviewRequested";
    public static final int EVENT_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(RabbitMQPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitMQPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void notifyReviewRequested(Question question) {
        QuestionReviewEvent event = new QuestionReviewEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                Instant.now(),
                question.getId().toString(),
                toApiStatus(question.getStatus()),
                question.getOptions().size());
        try {
            rabbitTemplate.send(EXCHANGE, ROUTING_KEY, toMessage(event));
            log.info("Event {} published for question {} (status={})",
                    event.eventId(), event.questionId(), event.status());
        } catch (RuntimeException exception) {
            // The transition was already persisted; losing the notification must not roll it back.
            // The retry template (RabbitMQConfig) already performed 3 attempts with backoff.
            log.error("Could not publish event {} for question {}; "
                            + "check RabbitMQ and queue {}",
                    event.eventId(), event.questionId(), LIFECYCLE_QUEUE, exception);
        }
    }

    private static String toApiStatus(QuestionStatus status) {
        return switch (status) {
            case DRAFT -> "BORRADOR";
            case PENDING_REVIEW -> "PENDIENTE_REVISION";
        };
    }

    private Message toMessage(QuestionReviewEvent event) {
        try {
            byte[] body = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
            return MessageBuilder.withBody(body)
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .build();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize the review event", exception);
        }
    }
}
