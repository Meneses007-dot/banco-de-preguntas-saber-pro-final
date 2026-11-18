package co.edu.unicauca.saberpro.question.infrastructure.api;

import co.edu.unicauca.saberpro.question.domain.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import co.edu.unicauca.saberpro.question.infrastructure.api.dto.ApiError;
import co.edu.unicauca.saberpro.question.infrastructure.client.ValidationServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates domain and transport failures into the HTTP contract of the service.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A violated business rule is a client error: 400 (decision A12). */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException exception) {
        log.warn("Business rule violated: {}", exception.getMessage());
        return ResponseEntity.badRequest()
                .body(new ApiError("BUSINESS_RULE", exception.getMessage()));
    }

    /** A missing aggregate is a 404, not a business rule violation. */
    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(QuestionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NOT_FOUND", exception.getMessage()));
    }

    /** The synchronous validation dependency is down: 503 (documented in the README). */
    @ExceptionHandler(ValidationServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleValidationUnavailable(ValidationServiceUnavailableException exception) {
        log.error("validation-service unavailable: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("VALIDATION_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("INVALID_REQUEST", "The request is not in a valid format."));
    }
}
