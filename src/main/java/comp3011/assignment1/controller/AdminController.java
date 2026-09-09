package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;

import comp3011.assignment1.model.ShutdownResponse;

import comp3011.assignment1.service.ServerLifecycleService;
import comp3011.assignment1.model.UptimeResponse;

import comp3011.assignment1.exception.ShutdownInProgressException;

@RestController
public class AdminController {
	private final ServerLifecycleService serverLifecycleService;
	
	public AdminController(ServerLifecycleService serverLifecycleService) {
		this.serverLifecycleService = serverLifecycleService;
	}
	
	@GetMapping("/api/v1/admin/uptime")
	public UptimeResponse getUptime() {
	    return serverLifecycleService.getUptime();
	}
	
	@PostMapping("/api/v1/admin/shutdown")
	public ResponseEntity<ShutdownResponse> shutdownServer() {

	    // Ask the lifecycle service to initiate shutdown.
	    boolean accepted = serverLifecycleService.shutdown();

	    // If another request has already initiated shutdown,
	    // delegate creation of the 409 response to the exception handler.
	    if (!accepted) {
	        throw new ShutdownInProgressException();
	    }

	    // Build the successful response required by the YAML specification.
	    ShutdownResponse response =
	            new ShutdownResponse("Graceful shutdown requested.");

	    return ResponseEntity
	            .accepted()
	            .body(response);
	}
}
