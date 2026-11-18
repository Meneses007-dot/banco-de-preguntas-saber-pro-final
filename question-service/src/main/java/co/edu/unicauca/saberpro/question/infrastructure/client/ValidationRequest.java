package co.edu.unicauca.saberpro.question.infrastructure.client;

import java.util.List;

/**
 * Request body sent to {@code validation-service}. It is an infrastructure DTO: the domain never
 * sees it.
 */
public record ValidationRequest(String statement, String justification, List<ValidationOption> options) {

    public record ValidationOption(String text, boolean correct) {
    }
}
