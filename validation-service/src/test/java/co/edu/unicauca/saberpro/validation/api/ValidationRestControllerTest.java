package co.edu.unicauca.saberpro.validation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("ValidationRestController - 200/400 contract")
class ValidationRestControllerTest {

    private static final String VALID_STATEMENT = "What is the author's conclusion in the text?";
    private static final String VALID_JUSTIFICATION =
            "The key is supported by the central thesis of the third paragraph.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("A valid question responds 200 with valid=true")
    void acceptsValidQuestion() throws Exception {
        mockMvc.perform(post("/api/validation/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options(4, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    @DisplayName("Three options respond 400 with valid=false and the INV-03 message")
    void rejectsWrongOptionCount() throws Exception {
        mockMvc.perform(post("/api/validation/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, VALID_JUSTIFICATION, options(3, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0]").value(
                        "The question must have exactly 4 answer options."));
    }

    @Test
    @DisplayName("A short justification responds 400 with valid=false")
    void rejectsShortJustification() throws Exception {
        mockMvc.perform(post("/api/validation/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(VALID_STATEMENT, "short", options(4, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @DisplayName("The container health check responds UP")
    void exposesActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private String payload(String statement, String justification,
                           List<Map<String, Object>> options) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "statement", statement,
                "justification", justification,
                "options", options));
    }

    private List<Map<String, Object>> options(int count, int keys) {
        List<Map<String, Object>> options = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            options.add(Map.of("text", "Option " + index, "correct", index <= keys));
        }
        return options;
    }
}
