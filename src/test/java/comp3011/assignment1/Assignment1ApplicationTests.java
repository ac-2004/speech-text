package comp3011.assignment1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Smoke test that verifies the complete Spring Boot application
// context can initialise successfully.
@SpringBootTest
class Assignment1ApplicationTests {

    @Test
    void contextLoads() {
        // No assertion is required here. If Spring cannot construct the
        // application context, the test fails before this method completes.
    }
}