package co.edu.unicauca.saberpro.question.application;

import co.edu.unicauca.saberpro.question.domain.exception.QuestionNotFoundException;
import co.edu.unicauca.saberpro.question.domain.exception.BusinessRuleException;
import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Statement;
import co.edu.unicauca.saberpro.question.domain.model.Justification;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.GetQuestionUseCase;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.CreateQuestionUseCase;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.EvaluateQuestionUseCase;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.OptionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.NotificationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionRepositoryPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.ValidationResult;
import java.util.List;

/**
 * Application service of the hexagon. It implements the three inbound use cases and coordinates
 * the outbound ports.
 *
 * <p>It deliberately contains <strong>no business rules</strong>: it maps the command to value
 * objects, delegates the decisions to the aggregate and orchestrates persistence, notification
 * and the synchronous validation call. Constructor injection only, no framework annotations.</p>
 */
public class QuestionApplicationService
        implements CreateQuestionUseCase, EvaluateQuestionUseCase, GetQuestionUseCase {

    private final QuestionRepositoryPort repository;
    private final NotificationPort notification;
    private final QuestionValidationPort validation;

    public QuestionApplicationService(QuestionRepositoryPort repository,
                                      NotificationPort notification,
                                      QuestionValidationPort validation) {
        this.repository = repository;
        this.notification = notification;
        this.validation = validation;
    }

    @Override
    public Question create(CreateQuestionCommand command) {
        if (command == null) {
            throw new BusinessRuleException("The question data is required.");
        }
        ValidationResult result = validation.validate(command);
        if (!result.valid()) {

            throw new BusinessRuleException(String.join(" | ", result.errors()));
        }
        Statement statement = Statement.of(command.statement());
        Justification justification = Justification.of(command.justification());
        Question question = Question.create(statement, justification, toOptions(command.options()));
        return repository.save(question);
    }

    @Override
    public Question evaluate(QuestionId id) {
        Question question = repository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(
                        "No question exists with identifier: " + id));
        question.submitForReview();
        Question saved = repository.save(question);
        notification.notifyReviewRequested(saved);
        return saved;
    }

    @Override
    public Question get(QuestionId id) {
        return repository.findById(id)
                .orElseThrow(() -> new QuestionNotFoundException(
                        "No question exists with identifier: " + id));
    }

    private List<AnswerOption> toOptions(List<OptionCommand> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> AnswerOption.of(option.text(), option.correct()))
                .toList();
    }
}
