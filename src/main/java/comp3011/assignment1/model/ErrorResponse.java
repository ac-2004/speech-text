package comp3011.assignment1.model;

import java.time.Instant;

// Represents the standard error response defined in the OpenAPI YAML.
// A record is suitable because this object only carries response data.
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}