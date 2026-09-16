package comp3011.assignment1.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
// imports
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import comp3011.assignment1.model.AudioData;
import comp3011.assignment1.service.TranscriptionService;
import reactor.core.publisher.Mono;

@RestController
public class TranscriptionController {
	// field
	private final TranscriptionService transcriptionService;

	// constructor
	public TranscriptionController(TranscriptionService transcriptionService) {
		this.transcriptionService = transcriptionService;
	}

	// handles audio uploaded (endpoint method)
	@PostMapping("/api/transcriptions")
	public Mono<String> receiveAudio(@RequestParam("audio") MultipartFile audio) {
		try {
			AudioData audioData = new AudioData(audio.getBytes(), audio.getOriginalFilename(), audio.getContentType());

			Mono<String> result = transcriptionService.transcribe(audioData);

			return result;

		} catch (IOException error) {
			// handle failure
			return Mono.just("error reading audio");
		}
	}
}
