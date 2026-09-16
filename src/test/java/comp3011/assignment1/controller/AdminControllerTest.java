package comp3011.assignment1.controller;

//mvc regression tests for the uptime and graceful shutdown admin endpoints. Test remains focused and avoids starting the whole application.


import static org.mockito.Mockito.times;
//Lets Mockito verify that the controller actually asked
//the lifecycle service to begin shutdown.
import static org.mockito.Mockito.verify;
//Lets Mockito define what our fake ServerLifecycleService should return.
import static org.mockito.Mockito.when;
//Creates a simulated HTTP GET request.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//Lets MockMvc simulate an HTTP POST request.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//Lets us check the HTTP status and JSON returned by the controller.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Used to create fixed timestamps for our predictable test response.
import java.time.Instant;

//Marks a method as a JUnit test.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import comp3011.assignment1.model.UptimeResponse;
import comp3011.assignment1.service.ServerLifecycleService;

@WebMvcTest(AdminController.class)
public class AdminControllerTest {

	// MockMvc lets us simulate HTTP requests to our controller without launching a real server on port 8080.
	@Autowired
	private MockMvc mockMvc;

	// Replace real ServerLifecycleService with a controlled mock.
	@MockitoBean
	private ServerLifecycleService serverLifecycleService;

	@Test
	void getUptime_returnsCorrectUptimeResponse() throws Exception {

		// Arrange: use fixed values so the test is deterministic.
		Instant start = Instant.parse("2026-09-09T00:00:00Z");
		Instant now = Instant.parse("2026-09-09T00:01:30Z");

		UptimeResponse response = new UptimeResponse(start, now, 90.0);
		// keep the service predictable so the controller response can be checked

		when(serverLifecycleService.getUptime()).thenReturn(response);

		// Act + Assert: verify the endpoint contract.
		mockMvc.perform(get("/api/v1/admin/uptime")).andExpect(status().isOk())
				.andExpect(jsonPath("$.utcServerStart").value("2026-09-09T00:00:00Z"))
				.andExpect(jsonPath("$.utcNow").value("2026-09-09T00:01:30Z"))
				.andExpect(jsonPath("$.serverUptimeSeconds").value(90.0));
	}

	@Test
	void getUptime_whenServiceFails_returnsStandard500Error() throws Exception {

		// Arrange: Force mocked service to fail so we can verify that the global exception handler converts the failure into the YAML error format.
		when(serverLifecycleService.getUptime()).thenThrow(new RuntimeException("simulated failure"));

		// Send the request through the real controller.
		mockMvc.perform(get("/api/v1/admin/uptime"))

				// The API contract requires an unexpected failure to return HTTP 500.
				.andExpect(status().isInternalServerError())

				// Verify the fixed fields required by ErrorResponse.
				.andExpect(jsonPath("$.status").value(500))

				.andExpect(jsonPath("$.error").value("Internal Server Error"))

				.andExpect(jsonPath("$.message").value("An unexpected server error occurred."))

				.andExpect(jsonPath("$.path").value("/api/v1/admin/uptime"))

				// The timestamp is generated at runtime, so we only verify that it exists rather than comparing it to a hard-coded value.
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void shutdownServer_whenAccepted_returns202() throws Exception {
		// Arrange: shutdown() now returns true when this request successfully claims the right to initiate the graceful shutdown.
		when(serverLifecycleService.shutdown()).thenReturn(true);
		// Send a simulated POST request to the shutdown endpoint.
		mockMvc.perform(post("/api/v1/admin/shutdown"))

				// The YAML requires a successful shutdown request to return 202 Accepted.
				.andExpect(status().isAccepted())

				// Verify the response body matches the ShutdownResponse contract.
				.andExpect(jsonPath("$.message").value("Graceful shutdown requested."));

		
		// Confirm the controller delegated the actual shutdown responsibility to ServerLifecycleService exactly once.
		verify(serverLifecycleService, times(1)).shutdown();
	}

	@Test
	void shutdownServer_whenAlreadyInProgress_returns409() throws Exception {
		// A false return value means another request has already initiated the graceful shutdown process.
		when(serverLifecycleService.shutdown()).thenReturn(false);

		// act and assert:
		mockMvc.perform(post("/api/v1/admin/shutdown"))

				// The OpenAPI YAML requires HTTP 409 Conflict.
				.andExpect(status().isConflict())

				// Verify the standard ErrorResponse fields.
				.andExpect(jsonPath("$.status").value(409))

				.andExpect(jsonPath("$.error").value("Conflict"))

				.andExpect(jsonPath("$.message").value("Graceful shutdown is already in progress."))

				.andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"))

				// Generated dynamically, so only check that it exists.
				.andExpect(jsonPath("$.timestamp").exists());

		// Confirm the controller checked the lifecycle service exactly once.
		verify(serverLifecycleService, times(1)).shutdown();
	}

	@Test
	void shutdownServer_whenServiceFails_returnsStandard500Error() throws Exception {

		// Simulate an unexpected internal failure while attempting to initiate the graceful shutdown process.
		when(serverLifecycleService.shutdown()).thenThrow(new RuntimeException("simulated shutdown failure"));

		// a+a
		mockMvc.perform(post("/api/v1/admin/shutdown"))

				// An unexpected failure must be converted into HTTP 500.
				.andExpect(status().isInternalServerError())

				// Verify the response follows the ErrorResponse structure defined by the OpenAPI specification.
				.andExpect(jsonPath("$.status").value(500))

				.andExpect(jsonPath("$.error").value("Internal Server Error"))

				.andExpect(jsonPath("$.message").value("An unexpected server error occurred."))

				.andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"))

				// The timestamp is generated when the exception is handled, so its exact value should not be hard-coded.
				.andExpect(jsonPath("$.timestamp").exists());

		// Confirm that the controller attempted the shutdown operation.
		verify(serverLifecycleService, times(1)).shutdown();
	}
}
