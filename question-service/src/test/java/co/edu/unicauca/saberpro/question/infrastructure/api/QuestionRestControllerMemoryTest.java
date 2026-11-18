package co.edu.unicauca.saberpro.question.infrastructure.api;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("REST API with InMemoryQuestionAdapter (memory profile)")
@ActiveProfiles("memory")
class QuestionRestControllerMemoryTest extends QuestionRestControllerContractTest {
}
