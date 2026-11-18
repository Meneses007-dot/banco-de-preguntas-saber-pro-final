package co.edu.unicauca.saberpro.validation.pipeline.filters;

import co.edu.unicauca.saberpro.validation.pipeline.base.QuestionFilter;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPayload;
import java.util.List;

/**
 * Filter 2 - the justification must provide at least 15 characters (INV-02).
 */
public class JustificationValidationFilter implements QuestionFilter {

    private static final int MIN_LENGTH = 15;

    @Override
    public List<String> validate(ValidationPayload payload) {
        if (payload == null || payload.justification() == null
                || payload.justification().trim().length() < MIN_LENGTH) {
            return List.of("The justification must contain at least 15 characters of technical support.");
        }
        return List.of();
    }
}
