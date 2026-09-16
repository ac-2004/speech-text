package comp3011.assignment1.controller;

//provides the admin end-points for server up-time and graceful shutdown.

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import comp3011.assignment1.exception.ShutdownInProgressException;
import comp3011.assignment1.model.ShutdownResponse;
import comp3011.assignment1.model.UptimeResponse;
import comp3011.assignment1.service.ServerLifecycleService;

// @RestController tells spring this class handles rest requests and returns its results in the http response body
@RestController
public class AdminController {
	private final ServerLifecycleService serverLifecycleService;

	public AdminController(ServerLifecycleService serverLifecycleService) {
		this.serverLifecycleService = serverLifecycleService;
	}

	// @GetMapping maps http get requests at this path to the method below
	@GetMapping("/api/v1/admin/uptime")
	public UptimeResponse getUptime() {
		return serverLifecycleService.getUptime();
	}

	// @PostMapping maps http post requests at this path to the method below
	@PostMapping("/api/v1/admin/shutdown")
	public ResponseEntity<ShutdownResponse> shutdownServer() {

		// Ask the lifecycle service to initiate shutdown, only first shutdown requested accepted.
		boolean accepted = serverLifecycleService.shutdown();

		// If another request has already initiated shutdown, delegate creation of the 409 response to the exception handler.
		if (!accepted) {
			throw new ShutdownInProgressException();
		}

		// Build the successful response required by the YAML specification.
		ShutdownResponse response = new ShutdownResponse("Graceful shutdown requested.");

		return ResponseEntity.accepted().body(response);
	}
}
