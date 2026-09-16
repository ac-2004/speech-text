package comp3011.assignment1.service;

//tracks global input and output token usage across successful transcription requests.

// use atomicLong so overlapping transcription requests do not lose updates
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import comp3011.assignment1.model.GlobalStatsResponse;

// @Service tells spring this class contains service logic and makes it available for dependency injection
@Service
public class GlobalStatisticsService {
	private final AtomicLong inputTokens = new AtomicLong(0);
	private final AtomicLong outputTokens = new AtomicLong(0);

	public GlobalStatsResponse getStats() {
		return new GlobalStatsResponse(inputTokens.get(), outputTokens.get());
	}

	public void addUsage(long input, long output) {
		inputTokens.addAndGet(input);
		outputTokens.addAndGet(output);
	}
}
