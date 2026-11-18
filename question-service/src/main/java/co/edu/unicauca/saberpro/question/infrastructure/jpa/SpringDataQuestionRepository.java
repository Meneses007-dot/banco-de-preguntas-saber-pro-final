package co.edu.unicauca.saberpro.question.infrastructure.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Spring Data repository used by the JPA adapter. It is a technical detail of the adapter and is
 * never injected outside {@code infrastructure.jpa}/{@code infrastructure.config}.
 */
public interface SpringDataQuestionRepository extends JpaRepository<QuestionRecord, UUID> {
}
