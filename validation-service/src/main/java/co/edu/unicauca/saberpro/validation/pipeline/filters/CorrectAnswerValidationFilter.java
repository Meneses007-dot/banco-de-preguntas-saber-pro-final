package co.edu.unicauca.saberpro.validation.pipeline.filters;

import co.edu.unicauca.saberpro.validation.pipeline.base.QuestionFilter;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPayload;
import java.util.List;

/**
 * Filter 4 - structural consistency of the key: exactly one option flagged as correct (INV-04).
 */
public class CorrectAnswerValidationFilter implements QuestionFilter {

    @Override
    public List<String> validate(ValidationPayload payload) {
        long keys = payload == null
                ? 0
                : payload.safeOptions().stream()
                        .filter(option -> option != null && option.correct())
                        .count();
        if (keys != 1) {
            return List.of("La pregunta debe tener exactamente una clave correcta.");
        }
        return List.of();
    }
}
