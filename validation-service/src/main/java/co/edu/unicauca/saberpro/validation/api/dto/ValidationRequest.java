package co.edu.unicauca.saberpro.validation.api.dto;

import java.util.List;

/**
 * Request body of {@code POST /api/validation/questions}.
 */
public record ValidationRequest(String statement, String justification, List<OptionRequest> options) {

    public record OptionRequest(String text, boolean correct) {
    }
}
