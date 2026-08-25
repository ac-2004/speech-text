package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;

import comp3011.assignment1.model.ShutdownResponse;

import comp3011.assignment1.service.ServerLifecycleService;
import comp3011.assignment1.model.UptimeResponse;

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
		ShutdownResponse response = new ShutdownResponse("Graceful shutdown requested.");
		
		serverLifecycleService.shutdown();
		
		return ResponseEntity.accepted().body(response);
		
	}
}
