package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Answer option inside the creation payload.
 */
public record OptionRequest(
        @JsonProperty("texto") @JsonAlias("text") String text,
        @JsonProperty("correcta") @JsonAlias("correct") boolean correct) {
}
