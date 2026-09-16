package comp3011.assignment1.model;

//defines the standard error body returned by the rest api.

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}