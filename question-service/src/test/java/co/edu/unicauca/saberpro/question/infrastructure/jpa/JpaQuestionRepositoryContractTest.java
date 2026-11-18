package co.edu.unicauca.saberpro.question.infrastructure.jpa;

import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import co.edu.unicauca.saberpro.question.infrastructure.contract.QuestionRepositoryPortContractTest;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DisplayName("Contrato QuestionRepositoryPort - JpaQuestionAdapter (H2)")
@DataJpaTest
@Import({QuestionDomainMapper.class, JpaQuestionAdapter.class})
class JpaQuestionRepositoryContractTest extends QuestionRepositoryPortContractTest {

    @Autowired
    private JpaQuestionAdapter adapter;

    @Override
    protected QuestionRepositoryPort repository() {
        return adapter;
    }
}
