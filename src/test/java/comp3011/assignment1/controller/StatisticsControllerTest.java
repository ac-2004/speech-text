package comp3011.assignment1.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import comp3011.assignment1.model.GlobalStatsResponse;
import comp3011.assignment1.service.GlobalStatisticsService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Loads only the Spring MVC layer needed to test StatisticsController.
// This keeps the test focused and avoids starting the whole application.
@WebMvcTest(StatisticsController.class)
public class StatisticsControllerTest {

    // MockMvc lets us simulate HTTP requests without launching a real web server on port 8080.
    @Autowired
    private MockMvc mockMvc;

    // Replaces the real GlobalStatisticsService with a mock.
    // This gives us complete control over the values returned in the test.
    @MockitoBean
    private GlobalStatisticsService globalStatisticsService;

    @Test
    void getGlobalStats_returnsCorrectStatistics() throws Exception {

        // ARRANGE:
        // Create predictable statistics for the mocked service to return.
        GlobalStatsResponse response =
                new GlobalStatsResponse(120, 35);

        // Tell Mockito what the fake service should return
        // when the controller requests the current global statistics.
        when(globalStatisticsService.getStats())
                .thenReturn(response);

        // ACT + ASSERT:
        // Send a simulated request through Spring MVC and verify
        // both the HTTP status and the JSON response contract.
        mockMvc.perform(
                get("/api/v1/global/stats")
        )
        .andExpect(status().isOk())

        // Confirm that the returned input-token count is correct.
        .andExpect(jsonPath("$.inputTokens")
                .value(120))

        // Confirm that the returned output-token count is correct.
        .andExpect(jsonPath("$.outputTokens")
                .value(35));
    }
    
    @Test
    void getGlobalStats_whenServiceFails_returnsStandard500Error() throws Exception {

        // ARRANGE:
        // Force the mocked statistics service to fail so we can verify that the global exception handler produces the standard YAML error shape.
        when(globalStatisticsService.getStats())
                .thenThrow(new RuntimeException("simulated failure"));

        // ACT + ASSERT:
        // Send the request through the real StatisticsController.
        mockMvc.perform(
                get("/api/v1/global/stats")
        )

        // Unexpected service failures must become HTTP 500 responses.
        .andExpect(status().isInternalServerError())

        // Verify the standard ErrorResponse fields required by the YAML.
        .andExpect(jsonPath("$.status")
                .value(500))

        .andExpect(jsonPath("$.error")
                .value("Internal Server Error"))

        .andExpect(jsonPath("$.message")
                .value("An unexpected server error occurred."))

        .andExpect(jsonPath("$.path")
                .value("/api/v1/global/stats"))

        // The timestamp is generated dynamically, so we only verify that it exists.
        .andExpect(jsonPath("$.timestamp").exists());
    }
}