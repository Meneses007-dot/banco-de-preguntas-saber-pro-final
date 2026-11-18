package co.edu.unicauca.saberpro.question.infrastructure.api.dto;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionStatus;
import java.util.List;

/**
 * HTTP representation of the aggregate. It never exposes JPA records or persistence types.
 */
public record QuestionResponse(
        String id,
        String enunciado,
        String justificacion,
        List<OptionResponse> opciones,
        String estado) {

    public record OptionResponse(String texto, boolean correcta) {
    }

    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
                question.getId().toString(),
                question.getStatement().value(),
                question.getJustification().text(),
                question.getOptions().stream()
                        .map(option -> new OptionResponse(option.text(), option.isCorrect()))
                        .toList(),
                toApiStatus(question.getStatus()));
    }

    private static String toApiStatus(QuestionStatus status) {
        return switch (status) {
            case DRAFT -> "BORRADOR";
            case PENDING_REVIEW -> "PENDIENTE_REVISION";
        };
    }
}
