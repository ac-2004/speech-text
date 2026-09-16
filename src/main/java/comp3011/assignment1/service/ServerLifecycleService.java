package comp3011.assignment1.service;

//tracks server uptime and coordinates graceful application shutdown.
//shutdown state is shared safely when multiple admin requests arrive together.

import java.time.Duration;
import java.time.Instant;
//Provides a thread-safe flag for tracking whether shutdown has begun.
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
// allows spring to close the application context and shut itself down
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import comp3011.assignment1.model.UptimeResponse;

@Service
public class ServerLifecycleService {
	private final Instant serverStartTime;
	private final ConfigurableApplicationContext applicationContext;

	// Ensures only one shutdown request can be accepted, even if multiple requests arrive concurrently.
	private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

	private static final Logger logger = LoggerFactory.getLogger(ServerLifecycleService.class);

	public ServerLifecycleService(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.serverStartTime = Instant.now(); // get curr time
	}

	// provide three info needed by uptime response
	public UptimeResponse getUptime() {

		Instant now = Instant.now();

		Duration uptime = Duration.between(serverStartTime, now);

		double uptimeSeconds = uptime.toNanos() / 1_000_000_000.0;

		return new UptimeResponse(serverStartTime, now, uptimeSeconds);
	}

	// shutdown
	public boolean shutdown() {

		// Atomically claim the right to initiate shutdown.
		// If another request has already changed the flag to true,
		// this request must be rejected.
		if (!shutdownInProgress.compareAndSet(false, true)) {
			logger.warn("Shutdown request rejected because shutdown is already in progress");
			return false;
		}

		logger.info("Graceful shutdown requested");

		// Run shutdown separately so the controller has time
		// to return the HTTP 202 response to the client.
		Thread shutdownThread = new Thread(() -> {
			try {
				Thread.sleep(500);

				// Ask Spring to close the application cleanly.
				int exitCode = SpringApplication.exit(applicationContext);

				logger.info("Application shutdown initiated");

				// Terminate the JVM once Spring has completed its shutdown work.
				System.exit(exitCode);

			} catch (InterruptedException exception) {

				// Restore the interrupted status instead of silently swallowing it.
				Thread.currentThread().interrupt();

				logger.error("Shutdown thread was interrupted");
			}
		});

		// Keep this thread alive long enough to complete the shutdown process.
		shutdownThread.setDaemon(false);
		shutdownThread.start();

		// This request successfully initiated shutdown.
		return true;
	}

}
