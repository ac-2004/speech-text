package comp3011.assignment1.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import comp3011.assignment1.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

// Centralises REST error handling so controllers stay focused on HTTP routing.
@RestControllerAdvice
public class GlobalExceptionHandler {

	// Handles the specific case where shutdown has already started.
	// This response matches the 409 contract defined in the OpenAPI YAML.
	@ExceptionHandler(ShutdownInProgressException.class)
	public ResponseEntity<ErrorResponse> handleShutdownInProgress(ShutdownInProgressException exception,
			HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.CONFLICT.value(),
				HttpStatus.CONFLICT.getReasonPhrase(), exception.getMessage(), request.getRequestURI());

		return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
	}

	// Handles unexpected exceptions using the standard YAML ErrorResponse shape.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception, HttpServletRequest request) {

		ErrorResponse response = new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
				HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), "An unexpected server error occurred.",
				request.getRequestURI());

		// Return HTTP 500 with the structured JSON error body.
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
	}
}