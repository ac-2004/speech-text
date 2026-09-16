package comp3011.assignment1;

//basic regression test that checks the spring application context can start.

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Assignment1ApplicationTests {

	@Test
	void contextLoads() {
		// No assertion is required here. If Spring cannot construct the application context, the test fails before this method completes.
	}
}