package co.edu.unicauca.saberpro.validation.pipeline.filters;

import co.edu.unicauca.saberpro.validation.pipeline.base.QuestionFilter;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Filter 3 - option list shape: exactly four options (INV-03), no blank texts (INV-05) and no
 * duplicated alternatives.
 */
public class OptionsValidationFilter implements QuestionFilter {

    private static final int REQUIRED_OPTIONS = 4;

    @Override
    public List<String> validate(ValidationPayload payload) {
        List<String> errors = new ArrayList<>();
        List<ValidationPayload.OptionPayload> options = payload == null
                ? List.of()
                : payload.safeOptions();

        if (options.size() != REQUIRED_OPTIONS) {
            errors.add("La pregunta debe tener exactamente 4 opciones de respuesta.");
        }

        Set<String> seen = new HashSet<>();
        for (ValidationPayload.OptionPayload option : options) {
            if (option == null || option.text() == null || option.text().isBlank()) {
                errors.add("El texto de la opción de respuesta no puede estar vacío.");
                continue;
            }
            String normalized = option.text().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(normalized)) {
                errors.add("Las opciones de respuesta no pueden estar duplicadas.");
            }
        }
        return errors;
    }
}
