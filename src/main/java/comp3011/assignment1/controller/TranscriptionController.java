package comp3011.assignment1.controller;

// imports
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TranscriptionController {
	@PostMapping("/api/transcriptions")
	// handles audio uploaded 
	public String receiveAudio(@RequestParam("audio") MultipartFile audio) {
		
		System.out.println(audio.getSize());
		return "received";
	}
	
}
