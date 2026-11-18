package co.edu.unicauca.saberpro.lifecycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Asynchronous consumer of the review flow (port 8084).
 *
 * <p>It owns its own bounded context: it never imports classes from {@code question-service} and
 * never touches its database. The integration contract is the versioned JSON event.</p>
 */
@SpringBootApplication
public class LifecycleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifecycleServiceApplication.class, args);
    }
}
