package comp3011.assignment1.controller;

//mvc regression tests for global statistics responses and error handling.

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import comp3011.assignment1.model.GlobalStatsResponse;
import comp3011.assignment1.service.GlobalStatisticsService;

@WebMvcTest(StatisticsController.class)
public class StatisticsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GlobalStatisticsService globalStatisticsService;

	@Test
	void getGlobalStats_returnsCorrectStatistics() throws Exception {

		// Create predictable statistics for the mocked service to return.
		GlobalStatsResponse response = new GlobalStatsResponse(120, 35);

		when(globalStatisticsService.getStats()).thenReturn(response);

		// Send a simulated request through Spring MVC and verify both the HTTP status and the JSON response contract.
		mockMvc.perform(get("/api/v1/global/stats")).andExpect(status().isOk())

				// Confirm that the returned token counts are correct.
				.andExpect(jsonPath("$.inputTokens").value(120))
				.andExpect(jsonPath("$.outputTokens").value(35));
	}

	// forced failure
	@Test
	void getGlobalStats_whenServiceFails_returnsStandard500Error() throws Exception {

		when(globalStatisticsService.getStats()).thenThrow(new RuntimeException("simulated failure"));

		mockMvc.perform(get("/api/v1/global/stats"))

				// Unexpected service failures must become HTTP 500 responses.
				.andExpect(status().isInternalServerError())

				// Verify the standard ErrorResponse fields required by the YAML.
				.andExpect(jsonPath("$.status").value(500))

				.andExpect(jsonPath("$.error").value("Internal Server Error"))

				.andExpect(jsonPath("$.message").value("An unexpected server error occurred."))

				.andExpect(jsonPath("$.path").value("/api/v1/global/stats"))

				// The timestamp is generated dynamically, so we only verify that it exists.
				.andExpect(jsonPath("$.timestamp").exists());
	}
}