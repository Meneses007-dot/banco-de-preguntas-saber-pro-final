package co.edu.unicauca.saberpro.lifecycle.api;

import co.edu.unicauca.saberpro.lifecycle.api.dto.LifecycleResponse;
import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycleRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read endpoint used as E2E-08 evidence: it proves that the asynchronous event was consumed and
 * that the state lives in the lifecycle context (not in {@code question-service}).
 */
@RestController
@RequestMapping("/api/lifecycle/questions")
public class LifecycleRestController {

    private final QuestionLifecycleRegistry registry;

    public LifecycleRestController(QuestionLifecycleRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/{id}")
    public ResponseEntity<LifecycleResponse> findByQuestionId(@PathVariable("id") String id) {
        return registry.findByQuestionId(id)
                .map(LifecycleResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
