package co.edu.unicauca.saberpro.question.infrastructure.jpa;

import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA implementation of the outbound persistence port.
 *
 * <p>It converts the aggregate into a {@link QuestionRecord} (and back) so the domain never sees
 * Hibernate, SQL or Spring Data types. Swapping this bean for {@code InMemoryQuestionAdapter} is
 * a pure configuration change.</p>
 */
public class JpaQuestionAdapter implements QuestionRepositoryPort {

    private final SpringDataQuestionRepository repository;
    private final QuestionDomainMapper mapper;

    public JpaQuestionAdapter(SpringDataQuestionRepository repository, QuestionDomainMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Question save(Question question) {
        QuestionRecord stored = repository.save(mapper.toRecord(question));
        return mapper.toDomain(stored);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(QuestionId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }
}
