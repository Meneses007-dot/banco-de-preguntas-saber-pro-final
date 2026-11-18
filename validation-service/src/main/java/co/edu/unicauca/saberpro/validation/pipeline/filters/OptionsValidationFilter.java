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
            errors.add("The question must have exactly 4 answer options.");
        }

        Set<String> seen = new HashSet<>();
        for (ValidationPayload.OptionPayload option : options) {
            if (option == null || option.text() == null || option.text().isBlank()) {
                errors.add("The text of the answer option cannot be empty.");
                continue;
            }
            String normalized = option.text().trim().toLowerCase(Locale.ROOT);
            if (!seen.add(normalized)) {
                errors.add("Answer options cannot be duplicated.");
            }
        }
        return errors;
    }
}
