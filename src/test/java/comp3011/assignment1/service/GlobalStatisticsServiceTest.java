package comp3011.assignment1.service;

//race-condition regression test for the shared global token counters.
//concurrent updates must finish with the exact expected totals.

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.model.GlobalStatsResponse;

class GlobalStatisticsServiceTest {

	@Test
	void addUsage_whenCalledConcurrently_doesNotLoseUpdates() throws Exception {

		// Use the real service because this test is specifically checking whether its token counters are thread-safe.
		GlobalStatisticsService statisticsService = new GlobalStatisticsService();

		// perform 500 updates at roughly the same time.
		int numberOfTasks = 500;

		// threads for concurrent execution
		ExecutorService executor = Executors.newFixedThreadPool(32);

		// release the worker tasks together to create contention on the counters
		CountDownLatch startLatch = new CountDownLatch(1);

		List<Future<?>> futures = new ArrayList<>();

		try {

			// Create 500 concurrent update tasks.
			for (int i = 0; i < numberOfTasks; i++) {

				Future<?> future = executor.submit(() -> {

					// Wait here until every task has been created.
					startLatch.await();

					// Each task contributes:
					// 2 input tokens
					// 3 output tokens
					statisticsService.addUsage(2, 3);

					return null;
				});

				futures.add(future);
			}

			// let the waiting updates run at roughly the same time
			startLatch.countDown();

			for (Future<?> future : futures) {
				future.get();
			}

			// Read the final token counts after all concurrent work has finished.
			GlobalStatsResponse result = statisticsService.getStats();

			// 500 tasks × 2 input tokens = 1000
			assertEquals(1000L, result.inputTokens());

			// 500 tasks × 3 output tokens = 1500
			assertEquals(1500L, result.outputTokens());

		} finally {

			// cleanup worker threads
			executor.shutdownNow();
		}
	}
}