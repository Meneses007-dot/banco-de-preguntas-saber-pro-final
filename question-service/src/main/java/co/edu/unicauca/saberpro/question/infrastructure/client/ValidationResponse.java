package co.edu.unicauca.saberpro.question.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response returned by {@code validation-service} ({@code POST /api/validation/questions}).
 */
public record ValidationResponse(
        @JsonProperty("valido") @JsonAlias("valid") boolean valid,
        @JsonProperty("errores") @JsonAlias("errors") List<String> errors) {
}
