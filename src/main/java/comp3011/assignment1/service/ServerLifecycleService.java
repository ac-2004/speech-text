package comp3011.assignment1.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import comp3011.assignment1.model.UptimeResponse;
import org.springframework.boot.SpringApplication;
// allows spring to close the application context and shut itself down
import org.springframework.context.ConfigurableApplicationContext;

@Service
public class ServerLifecycleService {
	private final Instant serverStartTime;
	private final ConfigurableApplicationContext applicationContext;
	
	
	public ServerLifecycleService(ConfigurableApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		this.serverStartTime = Instant.now(); // get curr time
	}
	
	// provide three info needed by uptime response
	public UptimeResponse getUptime() {

        Instant now = Instant.now();

        Duration uptime =
                Duration.between(serverStartTime, now);

        double uptimeSeconds =
                uptime.toNanos() / 1_000_000_000.0;

        return new UptimeResponse(
                serverStartTime,
                now,
                uptimeSeconds
        );
    }
	
	// shutdown
	public void shutdown() {
		Thread shutdownThread = new Thread(() -> {
	        try {
	            Thread.sleep(500);

	            int exitCode = SpringApplication.exit(applicationContext);

	            System.exit(exitCode);

	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }
	    });

	    shutdownThread.setDaemon(false);
	    shutdownThread.start();
	}
	
}
