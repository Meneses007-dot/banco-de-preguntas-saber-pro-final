package co.edu.unicauca.saberpro.question.infrastructure.config;

import co.edu.unicauca.saberpro.question.domain.ports.outbound.QuestionValidationPort;
import co.edu.unicauca.saberpro.question.infrastructure.client.ValidationServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the synchronous REST client used against {@code validation-service}.
 * Base URL and timeouts are injected from the environment (never hardcoded).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient validationRestClient(RestClient.Builder builder,
                                           @Value("${validation.service.url}") String baseUrl,
                                           @Value("${validation.service.connect-timeout-ms:2000}") int connectTimeoutMs,
                                           @Value("${validation.service.read-timeout-ms:3000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Bean
    public QuestionValidationPort questionValidationPort(RestClient validationRestClient,
                                                         ObjectMapper objectMapper) {
        return new ValidationServiceClient(validationRestClient, objectMapper);
    }
}
