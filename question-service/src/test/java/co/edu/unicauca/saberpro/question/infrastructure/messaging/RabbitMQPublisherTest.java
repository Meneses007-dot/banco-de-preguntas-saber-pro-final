package co.edu.unicauca.saberpro.question.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Statement;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import co.edu.unicauca.saberpro.question.domain.model.Justification;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("RabbitMQPublisher - QuestionReviewRequested event contract")
class RabbitMQPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Publishes versioned JSON with the required minimum fields")
    void publishesVersionedJsonEvent() throws Exception {
        CapturingRabbitTemplate template = new CapturingRabbitTemplate();
        RabbitMQPublisher publisher = new RabbitMQPublisher(template, objectMapper);

        publisher.notifyReviewRequested(pendingReviewQuestion());

        assertThat(template.exchange).isEqualTo(RabbitMQPublisher.EXCHANGE);
        assertThat(template.routingKey).isEqualTo(RabbitMQPublisher.ROUTING_KEY);

        JsonNode event = objectMapper.readTree(template.message.getBody());
        assertThat(event.get("eventId").asText()).isNotBlank();
        assertThat(event.get("eventType").asText()).isEqualTo("QuestionReviewRequested");
        assertThat(event.get("version").asInt()).isEqualTo(1);
        assertThat(event.get("occurredAt").asText()).isNotBlank();
        assertThat(event.get("questionId").asText()).isNotBlank();
        assertThat(event.get("estado").asText()).isEqualTo("PENDIENTE_REVISION");
        assertThat(event.get("numeroOpciones").asInt()).isEqualTo(4);
        assertThat(template.message.getMessageProperties().getContentType())
                .isEqualTo("application/json");
    }

    @Test
    @DisplayName("If the broker does not respond, it logs the error and does not propagate the exception")
    void keepsThePersistedTransitionWhenBrokerIsDown() {
        CapturingRabbitTemplate template = new CapturingRabbitTemplate();
        template.failure = new AmqpException("broker down");
        RabbitMQPublisher publisher = new RabbitMQPublisher(template, objectMapper);

        assertThatCode(() -> publisher.notifyReviewRequested(pendingReviewQuestion()))
                .doesNotThrowAnyException();
    }

    private Question pendingReviewQuestion() {
        return Question.builder()
                .statement(Statement.of("What is the author's conclusion in the text?"))
                .justification(Justification.of("The key is supported by the central thesis of the paragraph."))
                .options(List.of(
                        AnswerOption.of("Option 1", true),
                        AnswerOption.of("Option 2", false),
                        AnswerOption.of("Option 3", false),
                        AnswerOption.of("Option 4", false)))
                .status(QuestionStatus.PENDING_REVIEW)
                .build();
    }

    /** Test double that records the outgoing message without touching a real broker. */
    private static final class CapturingRabbitTemplate extends RabbitTemplate {

        private String exchange;
        private String routingKey;
        private Message message;
        private RuntimeException failure;

        @Override
        public void send(String exchange, String routingKey, Message message) {
            if (failure != null) {
                throw failure;
            }
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.message = message;
        }
    }
}
