package co.edu.unicauca.saberpro.question.infrastructure.config;

import co.edu.unicauca.saberpro.question.application.QuestionApplicationService;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.NotificationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.infrastructure.jpa.JpaQuestionAdapter;
import co.edu.unicauca.saberpro.question.infrastructure.jpa.QuestionDomainMapper;
import co.edu.unicauca.saberpro.question.infrastructure.jpa.SpringDataQuestionRepository;
import co.edu.unicauca.saberpro.question.infrastructure.memory.InMemoryQuestionAdapter;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Composition root of the hexagon (wiring lives only here).
 *
 * <p>Exactly one {@link QuestionRepositoryPort} bean is created: the switch is
 * {@code question.repository.adapter=jpa|memory}. If both values were configured, the startup
 * would fail with an ambiguous-dependency error instead of silently choosing one.</p>
 */
@Configuration
public class PortsConfig {

    private static final Logger log = LoggerFactory.getLogger(PortsConfig.class);

    @Bean
    public QuestionDomainMapper questionDomainMapper() {
        return new QuestionDomainMapper();
    }

    @Bean
    @ConditionalOnProperty(name = "question.repository.adapter", havingValue = "jpa", matchIfMissing = true)
    public QuestionRepositoryPort jpaQuestionAdapter(SpringDataQuestionRepository repository,
                                                     QuestionDomainMapper mapper,
                                                     Environment environment) {
        log.info("Active persistence adapter: JPA({})", relationalBackend(environment));
        return new JpaQuestionAdapter(repository, mapper);
    }

    @Bean
    @ConditionalOnProperty(name = "question.repository.adapter", havingValue = "memory")
    public QuestionRepositoryPort inMemoryQuestionAdapter() {
        log.info("Active persistence adapter: MEMORY(HashMap)");
        return new InMemoryQuestionAdapter();
    }

    @Bean
    public QuestionApplicationService questionApplicationService(QuestionRepositoryPort repository,
                                                                 NotificationPort notification,
                                                                 QuestionValidationPort validation) {
        return new QuestionApplicationService(repository, notification, validation);
    }

    private String relationalBackend(Environment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("postgres") ? "postgres" : "h2";
    }
}
