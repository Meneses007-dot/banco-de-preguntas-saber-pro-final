package co.edu.unicauca.saberpro.question.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request body sent to {@code validation-service}. It is an infrastructure DTO: the domain never
 * sees it.
 */
public record ValidationRequest(
        @JsonProperty("enunciado") @JsonAlias("statement") String statement,
        @JsonProperty("justificacion") @JsonAlias("justification") String justification,
        @JsonProperty("opciones") @JsonAlias("options") List<ValidationOption> options) {

    public record ValidationOption(
            @JsonProperty("texto") @JsonAlias("text") String text,
            @JsonProperty("correcta") @JsonAlias("correct") boolean correct) {
    }
}
