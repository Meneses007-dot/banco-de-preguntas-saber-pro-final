package co.edu.unicauca.saberpro.question.infrastructure.api;

import co.edu.unicauca.saberpro.question.domain.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import co.edu.unicauca.saberpro.question.infrastructure.api.dto.ApiError;
import co.edu.unicauca.saberpro.question.infrastructure.client.ValidationServiceUnavailableException;
import java.util.Arrays;
import java.util.stream.Collectors;
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
                .body(new ApiError("REGLA_NEGOCIO", translate(exception.getMessage())));
    }

    /** A missing aggregate is a 404, not a business rule violation. */
    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(QuestionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("NO_ENCONTRADO", translate(exception.getMessage())));
    }

    /** The synchronous validation dependency is down: 503 (documented in the README). */
    @ExceptionHandler(ValidationServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleValidationUnavailable(ValidationServiceUnavailableException exception) {
        log.error("validation-service unavailable: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError("VALIDACION_NO_DISPONIBLE", translate(exception.getMessage())));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("SOLICITUD_INVALIDA", "La solicitud no tiene un formato válido."));
    }

    private static String translate(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        return Arrays.stream(message.split(" \\| "))
                .map(GlobalExceptionHandler::translateSegment)
                .collect(Collectors.joining(" | "));
    }

    private static String translateSegment(String message) {
        if (message.startsWith("No question exists with identifier: ")) {
            return message.replace("No question exists with identifier: ",
                    "No existe una pregunta con el identificador: ");
        }
        if (message.startsWith("Invalid question identifier: ")) {
            return message.replace("Invalid question identifier: ",
                    "Identificador de pregunta inválido: ");
        }
        if (message.startsWith("validation-service responded with HTTP ")) {
            return message.replace("validation-service responded with HTTP ",
                    "validation-service respondió con HTTP ");
        }
        if (message.startsWith("Could not reach validation-service at ")) {
            return message.replace("Could not reach validation-service at ",
                    "No se pudo contactar a validation-service en ");
        }
        return switch (message) {
            case "The question identifier is required." ->
                    "El identificador de la pregunta es obligatorio.";
            case "The question statement cannot be null or empty." ->
                    "El enunciado de la pregunta no puede ser nulo ni vacío.";
            case "The question justification is required." ->
                    "La justificación de la pregunta es obligatoria.";
            case "The question status is required." ->
                    "El estado de la pregunta es obligatorio.";
            case "The question is already under review." ->
                    "La pregunta ya se encuentra en revisión.";
            case "The question must have exactly 4 answer options." ->
                    "La pregunta debe tener exactamente 4 opciones de respuesta.";
            case "The question must have exactly one correct key." ->
                    "La pregunta debe tener exactamente una clave correcta.";
            case "The answer options list is required." ->
                    "La lista de opciones de respuesta es obligatoria.";
            case "The answer options list cannot contain nulls." ->
                    "La lista de opciones de respuesta no puede contener nulos.";
            case "The text of the answer option cannot be empty." ->
                    "El texto de la opción de respuesta no puede estar vacío.";
            case "The justification must contain at least 15 characters of technical support." ->
                    "La justificación debe tener al menos 15 caracteres de sustentación técnica.";
            case "The question data is required." ->
                    "Los datos de la pregunta son obligatorios.";
            case "validation-service returned an empty response" ->
                    "validation-service devolvió una respuesta vacía";
            default -> message;
        };
    }
}
