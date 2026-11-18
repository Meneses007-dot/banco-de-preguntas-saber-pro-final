package co.edu.unicauca.saberpro.validation.pipeline.base;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipe that executes the filters sequentially and accumulates their errors.
 *
 * <p>Although every filter runs so the client receives the complete report, the ordering still
 * matters: the first error corresponds to the most specific rule.</p>
 */
public class ValidationPipeline {

    private final List<QuestionFilter> filters;

    public ValidationPipeline(List<QuestionFilter> filters) {
        this.filters = List.copyOf(filters);
    }

    /**
     * Executes every filter and returns the aggregated errors.
     *
     * @return immutable list of messages; empty when the payload is structurally valid
     */
    public List<String> validate(ValidationPayload payload) {
        List<String> errors = new ArrayList<>();
        for (QuestionFilter filter : filters) {
            errors.addAll(filter.validate(payload));
        }
        return List.copyOf(errors);
    }

    public int size() {
        return filters.size();
    }

    public List<QuestionFilter> filters() {
        return filters;
    }
}
