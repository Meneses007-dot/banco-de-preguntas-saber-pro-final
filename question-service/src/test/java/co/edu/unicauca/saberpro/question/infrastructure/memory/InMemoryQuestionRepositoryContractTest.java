package co.edu.unicauca.saberpro.question.infrastructure.memory;

import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import co.edu.unicauca.saberpro.question.infrastructure.contract.QuestionRepositoryPortContractTest;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Contrato QuestionRepositoryPort - InMemoryQuestionAdapter")
class InMemoryQuestionRepositoryContractTest extends QuestionRepositoryPortContractTest {

    private final InMemoryQuestionAdapter adapter = new InMemoryQuestionAdapter();

    @Override
    protected QuestionRepositoryPort repository() {
        return adapter;
    }
}
