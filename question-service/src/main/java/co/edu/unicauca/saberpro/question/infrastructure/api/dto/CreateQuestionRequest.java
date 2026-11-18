package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

import java.util.List;

/**
 * HTTP payload for {@code POST /api/questions}. It is a pure Jackson record, kept out of the
 * domain on purpose.
 */
public record CreateQuestionRequest(String statement, String justification, List<OptionRequest> options) {
}
