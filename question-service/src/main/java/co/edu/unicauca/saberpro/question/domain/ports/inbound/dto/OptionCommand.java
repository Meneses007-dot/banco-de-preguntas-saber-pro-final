package co.edu.unicauca.saberpro.question.domain.ports.inbound.dto;

/**
 * Option data inside the creation command: the text plus the correct-key flag.
 */
public record OptionCommand(String text, boolean correct) {
}
