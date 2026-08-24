package comp3011.assignment1.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

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
}
