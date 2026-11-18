package co.edu.unicauca.saberpro.question.infrastructure.api;

import co.edu.unicauca.saberpro.question.domain.model.QuestionId;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.GetQuestionUseCase;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.CreateQuestionUseCase;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.EvaluateQuestionUseCase;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.OptionCommand;
import co.edu.unicauca.saberpro.question.infrastructure.api.dto.CreateQuestionRequest;
import co.edu.unicauca.saberpro.question.infrastructure.api.dto.OptionRequest;
import co.edu.unicauca.saberpro.question.infrastructure.api.dto.QuestionResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Driving REST adapter. It depends <strong>only</strong> on inbound ports: it does not know the
 * repository, the JPA entities or the AMQP broker.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionRestController {

    private final CreateQuestionUseCase createQuestionUseCase;
    private final EvaluateQuestionUseCase evaluateQuestionUseCase;
    private final GetQuestionUseCase getQuestionUseCase;

    public QuestionRestController(CreateQuestionUseCase createQuestionUseCase,
                                  EvaluateQuestionUseCase evaluateQuestionUseCase,
                                  GetQuestionUseCase getQuestionUseCase) {
        this.createQuestionUseCase = createQuestionUseCase;
        this.evaluateQuestionUseCase = evaluateQuestionUseCase;
        this.getQuestionUseCase = getQuestionUseCase;
    }

    /** E2E-01 ... E2E-05: structural validation + domain invariants -> 201 / 400. */
    @PostMapping
    public ResponseEntity<QuestionResponse> create(@RequestBody CreateQuestionRequest request) {
        CreateQuestionCommand command = new CreateQuestionCommand(
                request.statement(),
                request.justification(),
                toOptions(request.options()));
        QuestionResponse response = QuestionResponse.from(createQuestionUseCase.create(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** E2E-06: transition to PENDING_REVIEW and asynchronous notification -> 202. */
    @PostMapping({"/{id}/evaluate", "/{id}/submit-for-review"})
    public ResponseEntity<QuestionResponse> evaluate(@PathVariable("id") String id) {
        QuestionResponse response = QuestionResponse.from(evaluateQuestionUseCase.evaluate(QuestionId.of(id)));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /** E2E-08 / E2E-09: aggregate lookup -> 200 / 404. */
    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable("id") String id) {
        return QuestionResponse.from(getQuestionUseCase.get(QuestionId.of(id)));
    }

    private List<OptionCommand> toOptions(List<OptionRequest> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> new OptionCommand(option.text(), option.correct()))
                .toList();
    }
}
