package comp3011.assignment1.service;

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

		// ARRANGE:
		// Use the real service because this test is specifically checking
		// whether its token counters are thread-safe.
		GlobalStatisticsService statisticsService = new GlobalStatisticsService();

		// We will perform 500 updates at roughly the same time.
		int numberOfTasks = 500;

		// Create a pool of worker threads.
		// These threads will execute our updates concurrently.
		ExecutorService executor = Executors.newFixedThreadPool(32);

		// The latch keeps all tasks waiting until we deliberately
		// release them together. This creates more contention.
		CountDownLatch startLatch = new CountDownLatch(1);

		// Store each submitted task so we can later wait for
		// all of them to finish.
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

			// Release all waiting tasks.
			// Many threads will now call addUsage() at roughly the same time.
			startLatch.countDown();

			// Wait for every task to finish.
			// future.get() will also surface any exception thrown by a task.
			for (Future<?> future : futures) {
				future.get();
			}

			// ACT:
			// Read the final token counts after all concurrent work has finished.
			GlobalStatsResponse result = statisticsService.getStats();

			// ASSERT:
			// 500 tasks × 2 input tokens = 1000
			assertEquals(1000L, result.inputTokens());

			// 500 tasks × 3 output tokens = 1500
			assertEquals(1500L, result.outputTokens());

		} finally {

			// Always clean up the worker threads, even if the test fails.
			executor.shutdownNow();
		}
	}
}