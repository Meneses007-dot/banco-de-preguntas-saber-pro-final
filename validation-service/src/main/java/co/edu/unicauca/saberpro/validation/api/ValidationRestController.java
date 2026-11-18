package co.edu.unicauca.saberpro.validation.api;

import co.edu.unicauca.saberpro.validation.api.dto.ValidationRequest;
import co.edu.unicauca.saberpro.validation.api.dto.ValidationResponse;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPayload;
import co.edu.unicauca.saberpro.validation.pipeline.base.ValidationPipeline;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synchronous validation endpoint consumed by {@code question-service}.
 *
 * <ul>
 *   <li>{@code 200 OK} + {@code {"valid":true,"errors":[]}}</li>
 *   <li>{@code 400 Bad Request} + {@code {"valid":false,"errors":[...]}}</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/validation/questions")
public class ValidationRestController {

    private static final Logger log = LoggerFactory.getLogger(ValidationRestController.class);

    private final ValidationPipeline pipeline;

    public ValidationRestController(ValidationPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping
    public ResponseEntity<ValidationResponse> validate(@RequestBody ValidationRequest request) {
        ValidationPayload payload = new ValidationPayload(
                request.statement(),
                request.justification(),
                toPayloadOptions(request.options()));

        List<String> errors = pipeline.validate(payload);
        if (errors.isEmpty()) {
            log.debug("Question is structurally valid");
            return ResponseEntity.ok(new ValidationResponse(true, List.of()));
        }
        log.info("Question rejected by structural validation: {}", errors);
        return ResponseEntity.badRequest().body(new ValidationResponse(false, errors));
    }

    private List<ValidationPayload.OptionPayload> toPayloadOptions(
            List<ValidationRequest.OptionRequest> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> new ValidationPayload.OptionPayload(option.text(), option.correct()))
                .toList();
    }
}
