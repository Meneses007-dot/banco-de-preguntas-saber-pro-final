package co.edu.unicauca.saberpro.question.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VO Statement - INV-01")
class StatementTest {

    @Nested
    @DisplayName("Construction validation")
    class Construction {

        @Test
        @DisplayName("T-01: a null statement throws BusinessRuleException")
        void rejectsNull() {
            assertThrows(BusinessRuleException.class, () -> Statement.of(null));
        }

        @Test
        @DisplayName("T-01: an empty statement throws BusinessRuleException")
        void rejectsEmpty() {
            assertThrows(BusinessRuleException.class, () -> Statement.of(""));
        }

        @Test
        @DisplayName("T-02: a blank statement throws BusinessRuleException")
        void rejectsBlank() {
            BusinessRuleException exception =
                    assertThrows(BusinessRuleException.class, () -> Statement.of("    "));
            assertEquals("The question statement cannot be null or empty.", exception.getMessage());
        }

        @Test
        @DisplayName("T-03: a valid statement is normalized and keeps its text")
        void acceptsValidValue() {
            Statement statement = Statement.of("  What is the conclusion of the text?  ");
            assertEquals("What is the conclusion of the text?", statement.value());
        }
    }

    @Nested
    @DisplayName("Value equality")
    class ValueEquality {

        @Test
        @DisplayName("T-16: two statements with the same text are equal")
        void equalsByValue() {
            assertEquals(Statement.of("Same text"), Statement.of("Same text"));
            assertEquals(Statement.of("Same text").hashCode(), Statement.of("Same text").hashCode());
        }

        @Test
        @DisplayName("T-16: two statements with different text are not equal")
        void notEqualsWhenTextDiffers() {
            assertNotEquals(Statement.of("Text A"), Statement.of("Text B"));
        }
    }
}
