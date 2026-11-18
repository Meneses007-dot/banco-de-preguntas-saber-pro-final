package co.edu.unicauca.saberpro.question.infrastructure.memory;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of the outbound persistence port (decision A16: {@link HashMap}).
 *
 * <p>It stores defensive copies of the aggregate so callers cannot mutate the stored state, which
 * mimics the behaviour of a relational adapter. Only the configuration changes to activate it.</p>
 */
public class InMemoryQuestionAdapter implements QuestionRepositoryPort {

    private final Map<QuestionId, Question> store = new HashMap<>();

    @Override
    public synchronized Question save(Question question) {
        Question copy = copyOf(question);
        store.put(copy.getId(), copy);
        return copyOf(copy);
    }

    @Override
    public synchronized Optional<Question> findById(QuestionId id) {
        return Optional.ofNullable(store.get(id)).map(InMemoryQuestionAdapter::copyOf);
    }

    private static Question copyOf(Question question) {
        return Question.rehydrate(question.getId(), question.getStatement(),
                question.getJustification(), question.getOptions(), question.getStatus());
    }
}
