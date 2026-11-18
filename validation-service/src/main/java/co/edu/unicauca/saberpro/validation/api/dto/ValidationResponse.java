package co.edu.unicauca.saberpro.validation.api.dto;

import java.util.List;

/**
 * Response body: {@code {"valid":true,"errors":[]}} or
 * {@code {"valid":false,"errors":["..."]}}.
 */
public record ValidationResponse(boolean valid, List<String> errors) {
}
