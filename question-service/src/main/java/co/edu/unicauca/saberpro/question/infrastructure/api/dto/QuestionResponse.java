package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import java.util.List;

/**
 * HTTP representation of the aggregate. It never exposes JPA records or persistence types.
 */
public record QuestionResponse(
        String id,
        String statement,
        String justification,
        List<OptionResponse> options,
        String status) {

    public record OptionResponse(String text, boolean correct) {
    }

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId().toString(),
                question.getStatement().value(),
                question.getJustification().text(),
                question.getOptions().stream()
                        .map(option -> new OptionResponse(option.text(), option.isCorrect()))
                        .toList(),
                question.getStatus().name());
    }
}
