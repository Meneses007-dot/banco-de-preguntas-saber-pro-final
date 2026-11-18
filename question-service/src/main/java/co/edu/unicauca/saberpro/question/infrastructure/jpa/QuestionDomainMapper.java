package co.edu.unicauca.saberpro.question.infrastructure.jpa;

import co.edu.unicauca.saberpro.question.domain.model.AnswerOption;
import co.edu.unicauca.saberpro.question.domain.model.Statement;
import co.edu.unicauca.saberpro.question.domain.model.Justification;
import co.edu.unicauca.saberpro.question.domain.model.Question;
import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import java.util.List;

/**
 * Anti-corruption mapper between the pure aggregate and the JPA record. It uses the GoF Builder
 * of the aggregate (Term II traceability) when rebuilding the domain object.
 */
public class QuestionDomainMapper {

    public Question toDomain(QuestionRecord record) {
        return Question.builder()
                .id(QuestionId.of(record.getId()))
                .statement(Statement.of(record.getStatement()))
                .justification(Justification.of(record.getJustification()))
                .options(toDomainOptions(record.getOptions()))
                .status(record.getStatus())
                .build();
    }

    public QuestionRecord toRecord(Question question) {
        return new QuestionRecord(
                question.getId().value(),
                question.getStatement().value(),
                question.getJustification().text(),
                question.getStatus(),
                toRecordOptions(question.getOptions()));
    }

    private List<AnswerOption> toDomainOptions(List<AnswerOptionRecord> records) {
        if (records == null) {
            return List.of();
        }
        return records.stream()
                .map(record -> AnswerOption.of(record.getText(), record.isCorrect()))
                .toList();
    }

    private List<AnswerOptionRecord> toRecordOptions(List<AnswerOption> options) {
        return options.stream()
                .map(option -> new AnswerOptionRecord(option.text(), option.isCorrect()))
                .toList();
    }
}
