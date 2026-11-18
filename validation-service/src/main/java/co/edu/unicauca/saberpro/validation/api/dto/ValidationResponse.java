package co.edu.unicauca.saberpro.validation.api.dto;

import java.util.List;

/**
 * Response body: {@code {"valido":true,"errores":[]}} or
 * {@code {"valido":false,"errores":["..."]}}.
 */
public record ValidationResponse(boolean valido, List<String> errores) {
}
