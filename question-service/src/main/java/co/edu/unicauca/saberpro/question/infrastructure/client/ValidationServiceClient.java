package co.edu.unicauca.saberpro.question.infrastructure.client;

import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.OptionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.ValidationResult;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionValidationPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Synchronous REST client for {@code validation-service} (REST, HTTP 200/400).
 *
 * <p>It implements the pure outbound port {@link QuestionValidationPort}, so neither the
 * application service nor the domain knows that HTTP exists. Timeouts are injected through
 * {@code RestClientConfig} and any transport failure becomes a
 * {@link ValidationServiceUnavailableException} (HTTP 503).</p>
 */
public class ValidationServiceClient implements QuestionValidationPort {

    static final String VALIDATION_PATH = "/api/validation/questions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ValidationServiceClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidationResult validate(CreateQuestionCommand command) {
        ValidationRequest body = new ValidationRequest(
                command.statement(),
                command.justification(),
                toOptions(command.options()));
        try {
            ValidationResponse response = restClient.post()
                    .uri(VALIDATION_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .exchange((request, clientResponse) -> {
                        HttpStatusCode status = clientResponse.getStatusCode();
                        if (!status.is2xxSuccessful() && status.value() != 400) {
                            throw new ValidationServiceUnavailableException(
                                    "validation-service responded with HTTP " + status.value());
                        }
                        return objectMapper.readValue(clientResponse.getBody(), ValidationResponse.class);
                    });
            if (response == null) {
                throw new ValidationServiceUnavailableException(
                        "validation-service returned an empty response");
            }
            return response.valid()
                    ? ValidationResult.success()
                    : ValidationResult.invalid(response.errors());
        } catch (ValidationServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ValidationServiceUnavailableException(
                    "Could not reach validation-service at " + VALIDATION_PATH, exception);
        }
    }

    private List<ValidationRequest.ValidationOption> toOptions(List<OptionCommand> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> new ValidationRequest.ValidationOption(option.text(), option.correct()))
                .toList();
    }
}
