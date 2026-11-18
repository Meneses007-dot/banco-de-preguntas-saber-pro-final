package co.edu.unicauca.saberpro.question.infrastructure.api;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("REST API with JpaQuestionAdapter (h2 profile)")
@ActiveProfiles("h2")
class QuestionRestControllerH2Test extends QuestionRestControllerContractTest {
}
