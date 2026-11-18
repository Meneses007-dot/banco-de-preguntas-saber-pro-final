package co.edu.unicauca.saberpro.question.infrastructure.config;

import co.edu.unicauca.saberpro.question.infrastructure.messaging.RabbitMQPublisher;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.NotificationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * AMQP topology of the item-review flow.
 *
 * <p>Exchange {@code saberpro.questions.exchange} (topic, durable) is bound to the durable queue
 * {@code saberpro.lifecycle.question-review} with routing key {@code question.review.requested}.
 * A dead-letter fanout exchange and its queue are declared as the recommended safety net.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String LIFECYCLE_QUEUE = "saberpro.lifecycle.question-review";
    public static final String DEAD_LETTER_EXCHANGE = "saberpro.questions.dlx";
    public static final String DEAD_LETTER_QUEUE = "saberpro.lifecycle.question-review.dlq";

    @Bean
    public TopicExchange questionsExchange() {
        return new TopicExchange(RabbitMQPublisher.EXCHANGE, true, false);
    }

    @Bean
    public Queue lifecycleQuestionReviewQueue() {
        return QueueBuilder.durable(LIFECYCLE_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    public Binding lifecycleBinding(Queue lifecycleQuestionReviewQueue, TopicExchange questionsExchange) {
        return BindingBuilder.bind(lifecycleQuestionReviewQueue)
                .to(questionsExchange)
                .with(RabbitMQPublisher.ROUTING_KEY);
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
    public Binding deadLetterBinding(Queue lifecycleDeadLetterQueue,
                                     FanoutExchange questionsDeadLetterExchange) {
        return BindingBuilder.bind(lifecycleDeadLetterQueue).to(questionsDeadLetterExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public NotificationPort notificationPort(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        return new RabbitMQPublisher(rabbitTemplate, objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        template.setRetryTemplate(retryTemplate());
        return template;
    }

    /**
     * Three publish attempts with exponential backoff (1s, 2s, 4s). If all of them fail the
     * exception reaches {@link RabbitMQPublisher}, which logs it and keeps the already persisted
     * transition.
     */
    private RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(4000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
