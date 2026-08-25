package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.model.GlobalStatsResponse;
import comp3011.assignment1.service.GlobalStatisticsService;

@RestController
public class StatisticsController {
	private final GlobalStatisticsService globalStatisticsService;

    public StatisticsController(
            GlobalStatisticsService globalStatisticsService) {
        this.globalStatisticsService = globalStatisticsService;
    }

    @GetMapping("/api/v1/global/stats")
    public GlobalStatsResponse getGlobalStats() {
        return globalStatisticsService.getStats();
    }
}
