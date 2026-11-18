package co.edu.unicauca.saberpro.question.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VO Justification - INV-02")
class JustificationTest {

    private static final String FOURTEEN_CHARACTERS = "12345678901234";
    private static final String FIFTEEN_CHARACTERS = "123456789012345";

    @Nested
    @DisplayName("15-character boundary")
    class Boundary {

        @Test
        @DisplayName("T-04: fourteen characters throw BusinessRuleException")
        void rejectsFourteenCharacters() {
            assertThrows(BusinessRuleException.class, () -> Justification.of(FOURTEEN_CHARACTERS));
        }

        @Test
        @DisplayName("T-05: exactly fifteen characters are accepted")
        void acceptsFifteenCharacters() {
            assertEquals(FIFTEEN_CHARACTERS, Justification.of(FIFTEEN_CHARACTERS).text());
        }

        @Test
        @DisplayName("T-06: a null justification throws BusinessRuleException")
        void rejectsNull() {
            BusinessRuleException exception =
                    assertThrows(BusinessRuleException.class, () -> Justification.of(null));
            assertEquals("The justification must contain at least 15 characters of technical support.",
                    exception.getMessage());
        }

        @Test
        @DisplayName("T-06: leading and trailing spaces do not count toward the minimum")
        void trimsBeforeCounting() {
            assertThrows(BusinessRuleException.class,
                    () -> Justification.of("   " + FOURTEEN_CHARACTERS + "   "));
        }
    }
}
