package comp3011.assignment1.model;

//defines the server start time, current time and uptime returned by the admin api.

import java.time.Instant;

public record UptimeResponse(Instant utcServerStart, Instant utcNow, double serverUptimeSeconds) {

}
