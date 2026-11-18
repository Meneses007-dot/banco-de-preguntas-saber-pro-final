package co.edu.unicauca.saberpro.question.infrastructure.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.question.domain.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import co.edu.unicauca.saberpro.question.infrastructure.api.dto.ApiError;
import co.edu.unicauca.saberpro.question.infrastructure.client.ValidationServiceUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler - error translation to HTTP")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("A violated business rule produces 400 BUSINESS_RULE")
    void mapsBusinessRuleTo400() {
        ResponseEntity<ApiError> response =
                handler.handleBusinessRule(new BusinessRuleException("The question is already under review."));

        assertEquals(400, response.getStatusCode().value());
        ApiError body = response.getBody();
        assertEquals("BUSINESS_RULE", body.error());
        assertEquals("The question is already under review.", body.message());
    }

    @Test
    @DisplayName("An unknown identifier produces 404 NOT_FOUND")
    void mapsNotFoundTo404() {
        ResponseEntity<ApiError> response =
                handler.handleNotFound(new QuestionNotFoundException("not found"));

        assertEquals(404, response.getStatusCode().value());
        assertTrue(response.getBody().message().contains("not found"));
    }

    @Test
    @DisplayName("validation-service being down produces 503 VALIDATION_UNAVAILABLE")
    void mapsUnavailableDependencyTo503() {
        ResponseEntity<ApiError> response = handler.handleValidationUnavailable(
                new ValidationServiceUnavailableException("no connection"));

        assertEquals(503, response.getStatusCode().value());
        assertEquals("VALIDATION_UNAVAILABLE", response.getBody().error());
    }
}
