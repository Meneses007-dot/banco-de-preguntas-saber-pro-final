package co.edu.unicauca.saberpro.validation.pipeline.base;

import java.util.List;

/**
 * Message that travels through the pipe. It carries only raw data: no domain aggregate is
 * instantiated here, because this service validates form, not business meaning.
 */
public record ValidationPayload(String statement, String justification, List<OptionPayload> options) {

    /** Option as received from the client. */
    public record OptionPayload(String text, boolean correct) {
    }

    /** Null-safe view of the options (an incoming {@code null} list means "no options"). */
    public List<OptionPayload> safeOptions() {
        return options == null ? List.of() : options;
    }
}
