package co.edu.unicauca.saberpro.question.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VO AnswerOption - INV-05")
class AnswerOptionTest {

    @Test
    @DisplayName("T-07: an option's text cannot be null, empty or blank")
    void rejectsEmptyText() {
        assertThrows(BusinessRuleException.class, () -> AnswerOption.of(null, true));
        assertThrows(BusinessRuleException.class, () -> AnswerOption.of("", false));
        BusinessRuleException exception =
                assertThrows(BusinessRuleException.class, () -> AnswerOption.of("   ", false));
        assertEquals("The text of the answer option cannot be empty.", exception.getMessage());
    }

    @Test
    @DisplayName("T-08: a valid option exposes its normalized text and correct-key flag")
    void exposesTextAndKey() {
        AnswerOption key = AnswerOption.of("  The author's conclusion  ", true);
        AnswerOption distractor = AnswerOption.of("A textual quote", false);

        assertEquals("The author's conclusion", key.text());
        assertTrue(key.isCorrect());
        assertFalse(distractor.isCorrect());
    }

    @Test
    @DisplayName("T-16: option equality is by value")
    void equalsByValue() {
        assertEquals(AnswerOption.of("Option A", true), AnswerOption.of("Option A", true));
        assertNotEquals(AnswerOption.of("Option A", true), AnswerOption.of("Option A", false));
    }
}
