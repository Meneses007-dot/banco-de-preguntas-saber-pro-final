package co.edu.unicauca.saberpro.validation.pipeline.base;

import java.util.List;

/**
 * Contract of one filter in the Pipes &amp; Filters pattern.
 *
 * <p>A filter returns the list of structural errors it detects; an empty list means "pass".
 * Returning a list instead of a boolean allows the pipeline to report every problem at once.</p>
 */
public interface QuestionFilter {

    List<String> validate(ValidationPayload payload);
}
