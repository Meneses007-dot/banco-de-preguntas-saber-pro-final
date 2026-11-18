package co.edu.unicauca.saberpro.validation.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.validation.pipeline.base.QuestionFilter;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPayload;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPipeline;
import co.edu.unicauca.saberpro.validation.pipeline.filters.ContentValidationFilter;
import co.edu.unicauca.saberpro.validation.pipeline.filters.CorrectAnswerValidationFilter;
import co.edu.unicauca.saberpro.validation.pipeline.filters.JustificationValidationFilter;
import co.edu.unicauca.saberpro.validation.pipeline.filters.OptionsValidationFilter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ValidationPipeline - Pipes & Filters (HU03)")
class ValidationPipelineTest {

    private static final String VALID_STATEMENT = "What is the author's conclusion in the text?";
    private static final String VALID_JUSTIFICATION =
            "The key is supported by the central thesis of the third paragraph.";

    private ValidationPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new ValidationPipeline(List.of(
                new ContentValidationFilter(),
                new JustificationValidationFilter(),
                new OptionsValidationFilter(),
                new CorrectAnswerValidationFilter()));
    }

    @Test
    @DisplayName("The pipeline is composed of four filters")
    void hasFourFilters() {
        assertEquals(4, pipeline.size());
        assertEquals(4, pipeline.filters().stream().map(QuestionFilter::getClass).distinct().count());
    }

    @Nested
    @DisplayName("Structurally valid payload")
    class ValidPayload {

        @Test
        @DisplayName("Four options and one key produce an empty error list")
        void acceptsValidPayload() {
            assertTrue(pipeline.validate(payload(VALID_STATEMENT, VALID_JUSTIFICATION, 4, 1)).isEmpty());
        }
    }

    @Nested
    @DisplayName("Structural errors")
    class StructuralErrors {

        @Test
        @DisplayName("An empty statement reports INV-01")
        void reportsEmptyStatement() {
            List<String> errors = pipeline.validate(payload("  ", VALID_JUSTIFICATION, 4, 1));
            assertEquals(List.of("El enunciado de la pregunta no puede ser nulo ni vacío."), errors);
        }

        @Test
        @DisplayName("A fourteen-character justification reports INV-02")
        void reportsShortJustification() {
            List<String> errors = pipeline.validate(payload(VALID_STATEMENT, "12345678901234", 4, 1));
            assertEquals(List.of(
                    "La justificación debe tener al menos 15 caracteres de sustentación técnica."), errors);
        }

        @Test
        @DisplayName("Three options report INV-03")
        void reportsWrongOptionCount() {
            List<String> errors = pipeline.validate(payload(VALID_STATEMENT, VALID_JUSTIFICATION, 3, 1));
            assertEquals(List.of("La pregunta debe tener exactamente 4 opciones de respuesta."), errors);
        }

        @Test
        @DisplayName("Two correct keys report INV-04")
        void reportsWrongKeyCount() {
            List<String> errors = pipeline.validate(payload(VALID_STATEMENT, VALID_JUSTIFICATION, 4, 2));
            assertEquals(List.of("La pregunta debe tener exactamente una clave correcta."), errors);
        }

        @Test
        @DisplayName("A blank option reports INV-05")
        void reportsBlankOption() {
            List<ValidationPayload.OptionPayload> options = new ArrayList<>(options(4, 1));
            options.set(1, new ValidationPayload.OptionPayload("   ", false));

            List<String> errors = pipeline.validate(
                    new ValidationPayload(VALID_STATEMENT, VALID_JUSTIFICATION, options));

            assertEquals(List.of("El texto de la opción de respuesta no puede estar vacío."), errors);
        }

        @Test
        @DisplayName("Duplicate options are rejected regardless of case and spacing")
        void reportsDuplicatedOptions() {
            List<ValidationPayload.OptionPayload> options = new ArrayList<>(options(4, 1));
            options.set(2, new ValidationPayload.OptionPayload("  option 1 ", false));

            List<String> errors = pipeline.validate(
                    new ValidationPayload(VALID_STATEMENT, VALID_JUSTIFICATION, options));

            assertEquals(List.of("Las opciones de respuesta no pueden estar duplicadas."), errors);
        }

        @Test
        @DisplayName("A null payload does not throw: it reports form errors")
        void handlesNullPayload() {
            List<String> errors = pipeline.validate(null);
            assertTrue(errors.size() >= 3);
        }
    }

    private ValidationPayload payload(String statement, String justification, int count, int keys) {
        return new ValidationPayload(statement, justification, options(count, keys));
    }

    private List<ValidationPayload.OptionPayload> options(int count, int keys) {
        List<ValidationPayload.OptionPayload> options = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            options.add(new ValidationPayload.OptionPayload("Option " + index, index <= keys));
        }
        return options;
    }
}
