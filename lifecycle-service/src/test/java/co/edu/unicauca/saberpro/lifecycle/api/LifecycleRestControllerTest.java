package co.edu.unicauca.saberpro.lifecycle.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unicauca.saberpro.lifecycle.state.QuestionLifecycleRegistry;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.rabbitmq.dynamic=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
@DisplayName("LifecycleRestController - status query E2E-08")
class LifecycleRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionLifecycleRegistry registry;

    @BeforeEach
    void setUp() {
        registry.clear();
    }

    @Test
    @DisplayName("A processed question responds 200 with its local status")
    void returnsRegisteredQuestion() throws Exception {
        registry.register("question-1", "event-1", Instant.parse("2026-11-13T15:04:05Z"));

        mockMvc.perform(get("/api/lifecycle/questions/{id}", "question-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value("question-1"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"))
                .andExpect(jsonPath("$.eventId").value("event-1"));
    }

    @Test
    @DisplayName("An unprocessed question responds 404")
    void returnsNotFoundForUnknownQuestion() throws Exception {
        mockMvc.perform(get("/api/lifecycle/questions/{id}", "question-404"))
                .andExpect(status().isNotFound());
    }
}
