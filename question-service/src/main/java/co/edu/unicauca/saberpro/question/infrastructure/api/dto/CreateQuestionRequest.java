package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * HTTP payload for {@code POST /api/questions}. It is a pure Jackson record, kept out of the
 * domain on purpose.
 */
public record CreateQuestionRequest(
        @JsonProperty("enunciado") @JsonAlias("statement") String statement,
        @JsonProperty("justificacion") @JsonAlias("justification") String justification,
        @JsonProperty("opciones") @JsonAlias("options") List<OptionRequest> options) {
}
