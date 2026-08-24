package comp3011.assignment1.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;

import comp3011.assignment1.model.UptimeResponse;

@Service
public class ServerLifecycleService {
	private final Instant serverStartTime;
	
	public ServerLifecycleService() {
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
	
}
