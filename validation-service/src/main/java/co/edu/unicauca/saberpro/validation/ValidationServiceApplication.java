package co.edu.unicauca.saberpro.validation;

import co.edu.unicauca.saberpro.validation.pipeline.base.QuestionFilter;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPipeline;
import co.edu.unicauca.saberpro.validation.pipeline.filters.ContentValidationFilter;
import co.edu.unicauca.saberpro.validation.pipeline.filters.CorrectAnswerValidationFilter;
import co.edu.unicauca.saberpro.validation.pipeline.filters.JustificationValidationFilter;
import co.edu.unicauca.saberpro.validation.pipeline.filters.OptionsValidationFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Structural validation service (synchronous REST, port 8082).
 *
 * <p>It keeps the Pipes &amp; Filters pipeline from Term I (user story HU03). It validates the <em>shape</em>
 * of the item only: the business decision always belongs to the aggregate in
 * {@code question-service}.</p>
 */
@SpringBootApplication
public class ValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationServiceApplication.class, args);
    }

    /**
     * Assembles the pipe. The order is significant for the error report: content, justification,
     * option count/shape and finally the correct-key consistency.
     */
    @Bean
    public ValidationPipeline validationPipeline() {
        return new ValidationPipeline(java.util.List.of(
                new ContentValidationFilter(),
                new JustificationValidationFilter(),
                new OptionsValidationFilter(),
                new CorrectAnswerValidationFilter()));
    }
}
