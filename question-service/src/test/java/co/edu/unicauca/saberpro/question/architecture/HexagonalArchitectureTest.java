package co.edu.unicauca.saberpro.question.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Executable architecture rules. They are the automated evidence of the 35% criterion: the domain
 * must stay pure and the application layer must never depend on the infrastructure.
 *
 * <p>The rules are evaluated with the plain JUnit 5 engine instead of the ArchUnit engine: on the
 * JDK 27 of the lab machine the ArchUnit engine reports {@code Tests run: 0} under Surefire even
 * though the rules are executed, which hides the result from the evidence. Evaluating the same
 * {@link ArchRule} objects inside regular {@code @Test} methods keeps the rule semantics and makes
 * the gate auditable (decision A30 in {@code AGENTS.md}).</p>
 */
@DisplayName("Hexagonal architecture of question-service")
class HexagonalArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("co.edu.unicauca.saberpro.question");

    private static final ArchRule PURE_DOMAIN = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "javax.persistence..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..",
                    "java.sql..",
                    "..infrastructure..",
                    "..application..");

    private static final ArchRule APPLICATION_WITHOUT_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    private static final ArchRule PORTS_ARE_INTERFACES = classes()
            .that().haveSimpleNameEndingWith("Port")
            .or().haveSimpleNameEndingWith("UseCase")
            .should().beInterfaces();

    private static final ArchRule ADAPTERS_LIVE_IN_INFRASTRUCTURE = classes()
            .that().haveSimpleNameEndingWith("Adapter")
            .or().haveSimpleNameEndingWith("Publisher")
            .or().haveSimpleNameEndingWith("Client")
            .should().resideInAnyPackage("..infrastructure..");

    @Nested
    @DisplayName("Layer purity")
    class LayerPurity {

        @Test
        @DisplayName("The domain does not depend on Spring, JPA, Jackson, AMQP, java.sql or outer layers")
        void pureDomain() {
            assertDoesNotThrow(() -> PURE_DOMAIN.check(CLASSES));
        }

        @Test
        @DisplayName("The application layer does not depend on the infrastructure")
        void applicationWithoutInfrastructure() {
            assertDoesNotThrow(() -> APPLICATION_WITHOUT_INFRASTRUCTURE.check(CLASSES));
        }
    }

    @Nested
    @DisplayName("Contracts and adapters")
    class ContractsAndAdapters {

        @Test
        @DisplayName("Every port and every use case is an interface")
        void portsAreInterfaces() {
            assertDoesNotThrow(() -> PORTS_ARE_INTERFACES.check(CLASSES));
        }

        @Test
        @DisplayName("Every adapter, publisher and client lives in infrastructure")
        void adaptersInInfrastructure() {
            assertDoesNotThrow(() -> ADAPTERS_LIVE_IN_INFRASTRUCTURE.check(CLASSES));
        }
    }
}
