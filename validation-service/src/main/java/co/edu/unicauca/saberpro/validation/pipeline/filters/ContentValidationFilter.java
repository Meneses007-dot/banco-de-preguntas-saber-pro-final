package co.edu.unicauca.saberpro.validation.pipeline.filters;

import co.edu.unicauca.saberpro.validation.pipeline.base.QuestionFilter;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPayload;
import java.util.List;

/**
 * Filter 1 - presence and integrity of the statement (mirrors INV-01 at the form level).
 */
public class ContentValidationFilter implements QuestionFilter {

    @Override
    public List<String> validate(ValidationPayload payload) {
        if (payload == null || payload.statement() == null || payload.statement().isBlank()) {
            return List.of("The question statement cannot be null or empty.");
        }
        return List.of();
    }
}
