package co.edu.unicauca.saberpro.lifecycle.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the same durable queue (and dead-letter infrastructure) used by the producer. The
 * arguments are identical to {@code question-service} so RabbitMQ never raises a
 * PRECONDITION_FAILED error for a conflicting declaration.
 */
@Configuration
public class RabbitMQListenerConfig {

    public static final String LIFECYCLE_QUEUE = "saberpro.lifecycle.question-review";
    public static final String DEAD_LETTER_EXCHANGE = "saberpro.questions.dlx";
    public static final String DEAD_LETTER_QUEUE = "saberpro.lifecycle.question-review.dlq";

    @Bean
    public Queue lifecycleQuestionReviewQueue() {
        return QueueBuilder.durable(LIFECYCLE_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    public FanoutExchange questionsDeadLetterExchange() {
        return new FanoutExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue lifecycleDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
