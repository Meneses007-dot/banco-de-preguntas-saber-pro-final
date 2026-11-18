package co.edu.unicauca.saberpro.question;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the hexagonal core of the Saber PRO Question Bank.
 *
 * <p>The package scan covers {@code infrastructure} only; the domain and the application layers
 * carry no Spring annotations at all.</p>
 */
@SpringBootApplication
public class QuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuestionServiceApplication.class, args);
    }
}
