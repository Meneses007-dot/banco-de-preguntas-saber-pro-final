package co.edu.unicauca.saberpro.question.infrastructure.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.CreateQuestionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.inbound.dto.OptionCommand;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.NotificationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.domain.ports.outbound.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract of the HTTP adapter, executed against both persistence adapters (subclasses use the
 * {@code memory} and {@code h2} profiles). It is the evidence that the API behaves identically
 * regardless of the active {@code QuestionRepositoryPort}.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.dynamic=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
@Import(QuestionRestControllerContractTest.StubPortsConfig.class)
abstract class QuestionRestControllerContractTest {

    protected static final String VALID_STATEMENT = "What is the author's conclusion in the text?";
    protected static final String VALID_JUSTIFICATION =
            "The key is supported by the central thesis of the third paragraph.";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    @DisplayName("E2E-01: a valid creation responds 201 and status BORRADOR")
    void createsValidQuestion() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options(4, 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.opciones.length()").value(4))
                .andExpect(jsonPath("$.justificacion").value(VALID_JUSTIFICATION));
    }

    @Test
    @DisplayName("E2E-02: an empty statement responds 400 with INV-01")
    void rejectsEmptyStatement() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload("", VALID_JUSTIFICATION, options(4, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(
                        "El enunciado de la pregunta no puede ser nulo ni vacío."));
    }

    @Test
    @DisplayName("E2E-03: a 14-character justification responds 400 with INV-02")
    void rejectsShortJustification() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, "12345678901234", options(4, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(
                        "La justificación debe tener al menos 15 caracteres de sustentación técnica."));
    }

    @Test
    @DisplayName("E2E-04: three options respond 400 with INV-03")
    void rejectsThreeOptions() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options(3, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(
                        "La pregunta debe tener exactamente 4 opciones de respuesta."));
    }

    @Test
    @DisplayName("E2E-05: two correct keys respond 400 with INV-04")
    void rejectsTwoCorrectKeys() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options(4, 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"))
                .andExpect(jsonPath("$.mensaje").value(
                        "La pregunta debe tener exactamente una clave correcta."));
    }

    @Test
    @DisplayName("E2E-05b: an empty option responds 400 with INV-05")
    void rejectsEmptyOption() throws Exception {
        List<Map<String, Object>> options = new ArrayList<>(options(4, 1));
        options.set(2, Map.of("texto", "   ", "correcta", false));

        mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "El texto de la opción de respuesta no puede estar vacío."));
    }

    @Test
    @DisplayName("E2E-06: a valid evaluation responds 202 and PENDIENTE_REVISION")
    void evaluatesValidQuestion() throws Exception {
        mockMvc.perform(post("/api/questions/{id}/evaluar", createQuestionId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    @Test
    @DisplayName("E2E-06b: the enviar-a-revision alias responds 202")
    void supportsDocumentedAlias() throws Exception {
        mockMvc.perform(post("/api/questions/{id}/enviar-a-revision", createQuestionId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_REVISION"));
    }

    @Test
    @DisplayName("INV-07: a second evaluation responds 400")
    void rejectsDoubleEvaluation() throws Exception {
        String id = createQuestionId();
        mockMvc.perform(post("/api/questions/{id}/evaluar", id)).andExpect(status().isAccepted());

        mockMvc.perform(post("/api/questions/{id}/evaluar", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La pregunta ya se encuentra en revisión."));
    }

    @Test
    @DisplayName("GET /api/questions/{id} responds 200 with the aggregate")
    void getsQuestionById() throws Exception {
        String id = createQuestionId();

        mockMvc.perform(get("/api/questions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    @Test
    @DisplayName("E2E-09: an unknown identifier responds 404")
    void returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/questions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NO_ENCONTRADO"));
    }

    @Test
    @DisplayName("A malformed identifier responds 400")
    void returnsBadRequestForMalformedId() throws Exception {
        mockMvc.perform(get("/api/questions/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REGLA_NEGOCIO"));
    }

    protected String createQuestionId() throws Exception {
        String body = mockMvc.perform(post("/api/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options(4, 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    protected String payload(String statement, String justification,
                             List<Map<String, Object>> options) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "enunciado", statement,
                "justificacion", justification,
                "opciones", options));
    }

    protected List<Map<String, Object>> options(int count, int correctKeys) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            options.add(Map.of("texto", "Option " + index, "correcta", index <= correctKeys));
        }
        return options;
    }

    /**
     * Test doubles for the two non-deterministic dependencies. The structural stub emulates the
     * contract of {@code validation-service} (exactly four options and exactly one key), so the
     * HTTP tests do not need a broker or a second JVM.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class StubPortsConfig {

        @Bean
        @Primary
        NotificationPort notificationPortStub() {
            return question -> {
                // no-op: the asynchronous path is covered by RabbitMQPublisherTest
            };
        }

        @Bean
        @Primary
        QuestionValidationPort questionValidationPortStub() {
            return QuestionRestControllerContractTest::validateStructurally;
        }
    }

    private static ValidationResult validateStructurally(CreateQuestionCommand command) {
        List<OptionCommand> options = command.options() == null ? List.of() : command.options();
        List<String> errors = new ArrayList<>();
        if (options.size() != 4) {
            errors.add("The question must have exactly 4 answer options.");
        }
        if (options.stream().filter(OptionCommand::correct).count() != 1) {
            errors.add("The question must have exactly one correct key.");
        }
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.invalid(errors);
    }
}
