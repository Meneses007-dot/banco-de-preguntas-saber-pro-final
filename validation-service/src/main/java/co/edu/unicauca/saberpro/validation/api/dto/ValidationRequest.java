package co.edu.unicauca.saberpro.validation.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request body of {@code POST /api/validation/questions}.
 */
public record ValidationRequest(
        @JsonProperty("enunciado") @JsonAlias("statement") String statement,
        @JsonProperty("justificacion") @JsonAlias("justification") String justification,
        @JsonProperty("opciones") @JsonAlias("options") List<OptionRequest> options) {

    public record OptionRequest(
            @JsonProperty("texto") @JsonAlias("text") String text,
            @JsonProperty("correcta") @JsonAlias("correct") boolean correct) {
    }
}
