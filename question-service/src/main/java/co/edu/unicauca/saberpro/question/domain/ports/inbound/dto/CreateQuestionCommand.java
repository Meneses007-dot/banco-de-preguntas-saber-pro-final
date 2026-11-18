package co.edu.unicauca.saberpro.question.domain.ports.inbound.dto;

import java.util.List;

/**
 * Framework-free command that carries the raw authoring data of a new item.
 * It lives in the domain so the domain never depends on HTTP DTOs or on the application layer.
 */
public record CreateQuestionCommand(String statement, String justification, List<OptionCommand> options) {
}
